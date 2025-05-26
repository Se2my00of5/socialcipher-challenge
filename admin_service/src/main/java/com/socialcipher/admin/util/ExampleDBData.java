package com.socialcipher.admin.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialcipher.admin.model.Post;
import com.socialcipher.admin.model.User;
import com.socialcipher.admin.repository.PostRepository;
import com.socialcipher.admin.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ExampleDBData implements CommandLineRunner {
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private FlagManager flagManager;

    @Override
    public void run(String... args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(getClass().getResourceAsStream("/data.json"));

        // 1. Сохраняем пользователей и запоминаем их по username
        Map<String, User> userMap = new HashMap<>();
        for (JsonNode userNode : root.get("users")) {
            User user = userRepository.findByUsername(userNode.get("username").asText()).orElse(new User());
            user.setUsername(userNode.get("username").asText());
            user.setRole(userNode.get("role").asText());
            user.setPasswordHash(passwordEncoder.encode(userNode.get("password").asText()));
            user = userRepository.save(user);
            userMap.put(user.getUsername(), user);
        }

        // 2. Сохраняем посты
        for (JsonNode postNode : root.get("posts")) {
            String username = postNode.get("userUsername").asText();
            User user = userMap.get(username);
            if (user != null) {
                Post post = postRepository.findByTitle(postNode.get("title").asText()).orElse(new Post());
                post.setUserId(user.getId());
                post.setTitle(postNode.get("title").asText());

                String content = postNode.get("content").asText();
                if (postNode.get("title").asText().contains("My secret")) {
                   content += flagManager.getFlagTwo();
                }
                content = Ciphers.saltyLanguage(content);
                content = Ciphers.caesarCipher(content, 5);

                post.setContent(content);
                post.setContent(content);
                postRepository.save(post);
            }
        }
    }

}
