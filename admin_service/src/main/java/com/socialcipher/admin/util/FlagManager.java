package com.socialcipher.admin.util;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class FlagManager {

    private Map<String, String> flags = new HashMap<>();
    private static final String FLAGS_FILE_PATH = "flags.txt"; // Путь к файлу флагов внутри контейнера

    @PostConstruct
    public void init() {
        loadFlags();
    }

    private void loadFlags() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FLAGS_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        flags.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
            System.out.println("Flags loaded successfully: " + flags.keySet());
        } catch (IOException e) {
            System.err.println("Error loading flags from " + FLAGS_FILE_PATH + ": " + e.getMessage());
            // Fallback to default flags if file not found/readable
            flags.put("FLAG_ONE", "HITS{DEFAULT_FLAG_ONE}");
            flags.put("FLAG_TWO", "HITS{DEFAULT_FLAG_TWO}");
        }
    }

    public String getFlagOne() {
        return flags.getOrDefault("FLAG_ONE", "HITS{FLAG_ONE_NOT_FOUND}");
    }

    public String getFlagTwo() {
        return flags.getOrDefault("FLAG_TWO", "HITS{FLAG_TWO_NOT_FOUND}");
    }
}