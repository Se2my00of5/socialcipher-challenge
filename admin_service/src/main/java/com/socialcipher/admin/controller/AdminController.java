package com.socialcipher.admin.controller;

import com.socialcipher.admin.model.Post;
import com.socialcipher.admin.model.User;
import com.socialcipher.admin.repository.PostRepository;
import com.socialcipher.admin.repository.UserRepository;
import com.socialcipher.admin.util.FlagManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Key;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Admin Service API", description = "Endpoints for user profiles and posts in Admin Service")
public class AdminController {
    private static final String SECRET = "SECRET_KEY1SECRET_KEY1SECRET_KEY1SECRET_KEY1"; // 32+ символа
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FlagManager flagManager;

    public AdminController(UserRepository userRepository, PostRepository postRepository, FlagManager flagManager) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.flagManager = flagManager;
    }

    @Operation(summary = "Get Authorization Token By User")
    @GetMapping("token")
    public String getToken(User user){
        return generateToken(user);
    }

    @Operation(summary = "Get user profile by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile data"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getUserProfile(
            HttpServletRequest request,
            @Parameter(description = "ID of the user") @PathVariable Long userId
    ) {
        Optional<User> user = userRepository.findById(userId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Request-URL", request.getRequestURL().toString());
        return user
                .map(u -> ResponseEntity.ok().headers(headers).body(u))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get posts by user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of posts for the user"),
            @ApiResponse(responseCode = "401", description = "Required request header 'Authorization' for method parameter type String is not present"),
            @ApiResponse(responseCode = "404", description = "No posts found for user")
    })
    @GetMapping("/posts/{userId}")
    public ResponseEntity<List<?>> getUserPosts(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "ID of the user") @PathVariable Long userId) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(401)
                    .body(List.of("Required request header 'Authorization' for method parameter type String is not present"));
        }

        String token = authHeader.replace("Bearer ", "");
        Claims claims = parseToken(token);

        List<Post> posts = null;
        if (claims.get("role").toString().equals("USER")) {
            System.out.println(claims.get("role"));
            Optional<User> user = userRepository.findById(userId);
            if (user.get().getRole().equals("USER"))
                posts = postRepository.findByUserId(userId);

        } else {
            posts = postRepository.findByUserId(userId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Request-URL", request.getRequestURL().toString()); // или твой IP
        headers.add("X-Flag-One", flagManager.getFlagOne());

        if (posts == null) {
            return ResponseEntity.ok().headers(headers).body(List.of()); // Return empty list instead of 404
        }
        return ResponseEntity.ok().headers(headers).body(posts);
    }

    public static Claims parseToken(String token) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public static String generateToken(User user) {
        return Jwts.builder()
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
}