package com.recipegen.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmartRecipeService {

    private final TranslationService translationService;
    private final RestTemplate restTemplate = new RestTemplate();

    public SmartRecipeService(TranslationService translationService) {
        this.translationService = translationService;
    }

    public Map<String, Object> handleSmartChat(String message, String area, Map<String, Boolean> filters) {
        Map<String, Object> response = new HashMap<>();

        // Step 1: Translate to English
        String english = translationService.translateToEnglish(message);

        // Step 2: Tokenize and match known ingredients
        List<String> knownIngredients = fetchAllIngredients();
        List<String> words = Arrays.stream(english.toLowerCase().split("[ ,.!?]+"))
                .collect(Collectors.toList());

        List<String> matched = new ArrayList<>();
        for (String word : words) {
            for (String ingredient : knownIngredients) {
                if (ingredient.toLowerCase().contains(word)) {
                    matched.add(ingredient);
                    break;
                }
            }
        }

        // Step 3: Reply if nothing matched
        if (matched.isEmpty()) {
            response.put("reply", "😥 Sorry, I couldn't recognize any ingredients in your message.");
            response.put("recipes", Collections.emptyList());
            return response;
        }

        // Step 4: Fetch full match recipes
        List<Map<String, String>> meals = fetchRecipes(String.join(",", matched), area);

        StringBuilder reply = new StringBuilder();
        if (meals != null && !meals.isEmpty()) {
            meals = applyFilters(meals, filters);
            reply.append("🍽️ Here are recipes based on your ingredients:\n\n");
        } else {
            // Step 5: If no full match, fallback to partial match
            reply.append("🔍 No full matches found. Showing partial results:\n\n");
            Set<String> seen = new HashSet<>();
            meals = new ArrayList<>();

            for (String ing : matched) {
                List<Map<String, String>> partial = fetchRecipes(ing, area);
                if (partial != null) {
                    for (Map<String, String> meal : partial) {
                        if (seen.add(meal.get("strMeal"))) {
                            meals.add(meal);
                            if (meals.size() >= 5) break;
                        }
                    }
                }
                if (meals.size() >= 5) break;
            }
        }

        // Step 6: Add meal names to reply
        for (int i = 0; i < Math.min(5, meals.size()); i++) {
            String name = meals.get(i).get("strMeal");
            reply.append("• ").append(addIngredientEmojis(name)).append("\n");
        }

        // Final response
        response.put("reply", reply.toString());
        response.put("recipes", meals);
        return response;
    }

    public List<String> suggestIngredients(String partial) {
        List<String> knownIngredients = fetchAllIngredients();
        return knownIngredients.stream()
                .filter(ing -> ing.toLowerCase().startsWith(partial.toLowerCase()))
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<Map<String, String>> fetchRecipes(String ingredients, String area) {
        try {
            String url = "https://www.themealdb.com/api/json/v1/1/filter.php?i=" + ingredients;
            ResponseEntity<Map> res = restTemplate.getForEntity(url, Map.class);
            List<Map<String, String>> meals = (List<Map<String, String>>) res.getBody().get("meals");

            if (area != null && !area.isEmpty() && meals != null) {
                meals = meals.stream()
                        .filter(m -> area.equalsIgnoreCase(m.get("strArea")))
                        .collect(Collectors.toList());
            }

            return meals != null ? meals : Collections.emptyList();
        } catch (Exception e) {
            System.err.println("⚠️ Error fetching recipes: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> fetchAllIngredients() {
        String url = "https://www.themealdb.com/api/json/v1/1/list.php?i=list";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            List<Map<String, String>> ingredients = (List<Map<String, String>>) response.getBody().get("meals");
            return ingredients.stream()
                    .map(i -> i.get("strIngredient"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("⚠️ Error fetching ingredients: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Map<String, String>> applyFilters(List<Map<String, String>> meals, Map<String, Boolean> filters) {
        if (filters == null || filters.isEmpty()) return meals;

        return meals.stream()
                .filter(meal -> {
                    String name = meal.getOrDefault("strMeal", "").toLowerCase();
                    if (Boolean.TRUE.equals(filters.get("vegetarian")) && !name.contains("vegetarian")) return false;
                    if (Boolean.TRUE.equals(filters.get("lowFat")) && name.contains("fat")) return false;
                    if (Boolean.TRUE.equals(filters.get("under30")) && name.length() > 120) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    private String addIngredientEmojis(String text) {
        return text
                .replaceAll("(?i)chicken", "🍗 chicken")
                .replaceAll("(?i)beef", "🥩 beef")
                .replaceAll("(?i)pork", "🥓 pork")
                .replaceAll("(?i)egg", "🥚 egg")
                .replaceAll("(?i)garlic", "🧄 garlic")
                .replaceAll("(?i)onion", "🧅 onion")
                .replaceAll("(?i)cheese", "🧀 cheese")
                .replaceAll("(?i)carrot", "🥕 carrot")
                .replaceAll("(?i)fish", "🐟 fish")
                .replaceAll("(?i)salmon", "🐟 salmon")
                .replaceAll("(?i)shrimp|prawn", "🦐 shrimp")
                .replaceAll("(?i)crab", "🦀 crab")
                .replaceAll("(?i)tomato", "🍅 tomato")
                .replaceAll("(?i)potato", "🥔 potato")
                .replaceAll("(?i)rice", "🍚 rice")
                .replaceAll("(?i)noodle|pasta", "🍝 pasta")
                .replaceAll("(?i)milk", "🥛 milk")
                .replaceAll("(?i)butter", "🧈 butter")
                .replaceAll("(?i)bread", "🍞 bread")
                .replaceAll("(?i)apple", "🍎 apple")
                .replaceAll("(?i)banana", "🍌 banana")
                .replaceAll("(?i)lemon", "🍋 lemon")
                .replaceAll("(?i)orange", "🍊 orange");
    }
}
