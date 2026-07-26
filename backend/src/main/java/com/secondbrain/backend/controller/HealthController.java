package com.secondbrain.backend.controller;

import com.secondbrain.backend.repository.NoteRepository;
import com.secondbrain.backend.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping({"/api/health", "/health", "/"})
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-3.1-flash-lite}")
    private String geminiModel;

    @Value("${spring.neo4j.uri:}")
    private String neo4jUri;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private AIService aiService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("version", "1.0.0");
        health.put("timestamp", Instant.now().toString());

        // Check Neo4j Database status
        Map<String, Object> neo4jStatus = new HashMap<>();
        try {
            long count = noteRepository.count();
            neo4jStatus.put("status", "ONLINE");
            neo4jStatus.put("connected", true);
            neo4jStatus.put("connectionType", (neo4jUri != null && (neo4jUri.contains("neo4j+s") || neo4jUri.contains("aura"))) ? "AuraDB" : "Neo4j Direct");
            neo4jStatus.put("nodeCount", count);
        } catch (Exception e) {
            logger.warn("Neo4j health check failed: {}", e.getMessage());
            neo4jStatus.put("status", "OFFLINE");
            neo4jStatus.put("connected", false);
            neo4jStatus.put("connectionType", (neo4jUri != null && (neo4jUri.contains("neo4j+s") || neo4jUri.contains("aura"))) ? "AuraDB" : "Neo4j Direct");
            neo4jStatus.put("error", "Neo4j database service is offline at " + (neo4jUri != null && !neo4jUri.isEmpty() ? neo4jUri : "unconfigured endpoint"));
        }
        health.put("neo4j", neo4jStatus);

        // Check Gemini AI status
        Map<String, Object> geminiStatus = new HashMap<>();
        boolean apiKeyPresent = geminiApiKey != null && !geminiApiKey.trim().isEmpty();
        geminiStatus.put("status", apiKeyPresent ? "CONFIGURED" : "NOT_CONFIGURED");
        geminiStatus.put("apiKeyLoaded", apiKeyPresent);
        geminiStatus.put("model", geminiModel);
        geminiStatus.put("lastSuccessfulAiRequestTime", aiService.getLastSuccessfulAiRequestTime());
        geminiStatus.put("lastAiError", aiService.getLastAiError());
        if (!apiKeyPresent) {
            geminiStatus.put("message", "Set GEMINI_API_KEY environment variable to enable Gemini AI synthesis");
        }
        health.put("gemini", geminiStatus);

        return ResponseEntity.ok(health);
    }
}
