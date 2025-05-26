package com.socialcipher.auth.controller;

import com.socialcipher.auth.model.Post;
import com.socialcipher.auth.model.User;
import com.socialcipher.auth.repository.PostRepository;
import com.socialcipher.auth.repository.UserRepository;
import com.socialcipher.auth.util.Ciphers;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Controller
public class AuthController {
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Value("${admin.service.url}")
    private String adminServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthController(UserRepository userRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        if (session.getAttribute("username") != null) {
            model.addAttribute("username", session.getAttribute("username"));
            return "index";
        }
        return "redirect:/login";
    }
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Неверный логин или пароль");
        }
        return "login";
    }
    @PostMapping("/login")
    public String login(String username, String password, HttpSession session) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent() && passwordEncoder.matches(password,userOptional.get().getPasswordHash())) {
            session.setAttribute("username", userOptional.get().getUsername());
            session.setAttribute("userId", userOptional.get().getId());
            session.setAttribute("role", userOptional.get().getRole());
            return "redirect:/";
        }
        return "redirect:/login?error"; // Or add error message
    }
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
    @PostMapping("/register")
    public String register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            return "register"; // Or add error message
        }
        User user = new User();
        user.setUsername(username);
        user.setRole("USER");
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

        @GetMapping("/my_profile")
    public String myProfile(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", session.getAttribute("username"));
        return "my_profile";
    }

    @GetMapping("/users")
    public String browseUsers(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        model.addAttribute("users", userRepository.findAll());
        return "users";
    }

    @GetMapping("/user/{userId}")
    public String viewUserProfile(@PathVariable Long userId, HttpSession session, Model model, HttpServletResponse response) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        if(session.getAttribute("userId").equals(userId)) {
            return "redirect:/my_profile";
        }

        String adminProfileUrl = adminServiceUrl + "/api/v1/profile/" + userId;
        try {
            ResponseEntity<User> adminResponse = restTemplate.exchange(
                    adminProfileUrl,
                    HttpMethod.GET,
                    null,
                    User.class
            );

            model.addAttribute("userId", userId);
            model.addAttribute("username", adminResponse.getBody().getUsername());

            response.addHeader("Request-URL", adminResponse.getHeaders().getFirst("Request-URL"));

            return "profile";

        } catch (HttpClientErrorException e) {
            // If the admin service returns 4xx error (e.g., user not found)
            model.addAttribute("error", "User not found or access denied: " + e.getResponseBodyAsString());
            return "error_page"; // A simple error page
        } catch (Exception e) {
            model.addAttribute("error", "Error connecting to admin service or invalid user ID: " + e.getMessage());
            return "error_page";
        }
    }
    @GetMapping("/my_posts")
    public String myPosts(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        String adminPostsUrl = adminServiceUrl + "/api/v1/posts/" + session.getAttribute("userId");
        try {
            ResponseEntity<List<Post>> postsResponse = restTemplate.exchange(
                    adminPostsUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders(){{
                        set("Authorization", "Bearer " + generateToken(session));
                    }}),
                    new ParameterizedTypeReference<>() {}
            );
            List<Post> posts = postsResponse.getBody();
            for (Post post : posts) {
                String content = post.getContent();
                content = Ciphers.caesarDecipher(content,5);
                content = Ciphers.saltyLanguageDecipher(content);
                post.setContent(content);
            }

            model.addAttribute("posts", posts);
            model.addAttribute("username", session.getAttribute("username"));
            return "my_posts";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "No posts found for user or access denied.");
            return "error_page";
        } catch (Exception e) {
            model.addAttribute("error", "Error fetching posts: " + e.getMessage());
            return "error_page";
        }
    }
    @PostMapping("/add_post")
    public String addPost(@RequestParam String title,
                          @RequestParam String content,
                          HttpSession session)
    {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        postRepository.save(post);

        return "redirect:/my_posts";
    }

    @GetMapping("/user/{userId}/posts")
    public String viewUserPosts(@PathVariable Long userId, HttpSession session, Model model, HttpServletResponse response) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        String adminPostsUrl = adminServiceUrl + "/api/v1/posts/" + userId;
        try {
            ResponseEntity<List<Post>> postsResponse = restTemplate.exchange(
                    adminPostsUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders(){{
                        set("Authorization", "Bearer " + generateToken(session));
                    }}),
                    new ParameterizedTypeReference<>() {}
            );

            List<Post> posts = postsResponse.getBody();
            for (Post post : posts) {
                String content = post.getContent();
                content = Ciphers.caesarDecipher(content,5);
                content = Ciphers.saltyLanguageDecipher(content);
                post.setContent(content);
            }

            model.addAttribute("posts", posts);
            model.addAttribute("userId", userId);
            model.addAttribute("username", session.getAttribute("username"));

            response.addHeader("Request-URL", postsResponse.getHeaders().getFirst("Request-URL"));
            response.addHeader("X-Flag-One", postsResponse.getHeaders().getFirst("X-Flag-One"));

            return "profile_posts"; // A new template for posts
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "No posts found for user or access denied.");
            return "error_page";
        } catch (Exception e) {
            model.addAttribute("error", "Error fetching posts: " + e.getMessage());
            return "error_page";
        }
    }
    private static final String SECRET = "SECRET_KEY1SECRET_KEY1SECRET_KEY1SECRET_KEY1"; // 32+ символа
    public static String generateToken(HttpSession session) {
        return Jwts.builder()
                .claim("userId", session.getAttribute("userId"))
                .claim("username", session.getAttribute("username"))
                .claim("role", session.getAttribute("role"))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
}