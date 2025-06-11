package com.recipegen.controller;

import com.recipegen.service.ChatService;
import com.recipegen.service.SmartRecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class SmartRecipeController {

    private final SmartRecipeService smartRecipeService;
    private final ChatService chatService;

    public SmartRecipeController(SmartRecipeService smartRecipeService, ChatService chatService) {
        this.smartRecipeService = smartRecipeService;
        this.chatService = chatService;
    }

    /**
     * Chat endpoint for smart recipe generation and AI conversation.
     * Accepts user message, area (nationality), and optional filters.
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        Object msgObj = request.get("message");
        if (!(msgObj instanceof String message) || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing message"));
        }

        String area = (String) request.getOrDefault("area", "");

        Map<String, Boolean> filters = null;
        Object filtersObj = request.get("filters");
        if (filtersObj instanceof Map<?, ?> map) {
            try {
                filters = map.entrySet().stream()
                        .filter(e -> e.getKey() instanceof String && e.getValue() instanceof Boolean)
                        .collect(Collectors.toMap(
                                e -> (String) e.getKey(),
                                e -> (Boolean) e.getValue()
                        ));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid filters format"));
            }
        }

        // ✅ Call smartRecipeService, which already includes AI and recipe logic
        Map<String, Object> recipeResponse = smartRecipeService.handleSmartChat(message, area, filters);

        return ResponseEntity.ok(recipeResponse);
    }

    /**
     * Suggest endpoint for ingredient auto-complete
     */
    @GetMapping("/suggestions")
    public List<String> suggest(@RequestParam String input) {
        return smartRecipeService.suggestIngredients(input);
    }
}
