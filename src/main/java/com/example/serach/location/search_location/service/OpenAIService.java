package com.example.serach.location.search_location.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;


import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OpenAIService {
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String GPT_MODEL = "gpt-4.1-nano";

    public static JsonNode callWithFunction(List<Map<String, Object>> history) throws IOException, ParseException {
        ObjectMapper mapper = new ObjectMapper();
        String toolName = "get_nearby_places";

        // STEP 1 — Initial OpenAI call (tool_choice: auto)
        Map<String, Object> toolFunction = Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "get_nearby_places",  // Renamed from get_food_places
                        "description", "Get nearby places of a specific type (restaurants, hotels, parks, etc.) in a given location",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "location", Map.of(
                                                "type", "string",
                                                "description", "The city/area to search in, e.g., 'New York'"
                                        ),
                                        "place_type", Map.of(
                                                "type", "string",
                                                "description", "Type of place (restaurant, hotel, park, zoo, etc.)",
                                                "enum", List.of(
                                                        "restaurant", "hotel", "park",
                                                        "zoo", "museum", "cafe",
                                                        "shopping_mall", "apartment"
                                                )
                                        ),
                                        "keyword", Map.of(
                                                "type", "string",
                                                "description", "Optional filter (e.g., 'Italian' for Italian restaurants, 'luxury' for hotels)"
                                        ),
                                        "radius", Map.of(
                                                "type", "integer",
                                                "description", "Search radius in meters (default: 3000)",
                                                "default", 3000
                                        )
                                ),
                                "required", List.of("location", "place_type")  // Only location and place_type are mandatory
                        )
                )
        );

        // Prepare the first request
        Map<String, Object> request = Map.of(
                "model", GPT_MODEL,
                "messages", history,
                "tools", List.of(toolFunction),
                "tool_choice", "auto"
        );

        String json = mapper.writeValueAsString(request);

        HttpPost post = new HttpPost("https://api.openai.com/v1/chat/completions");
        post.setHeader("Authorization", "Bearer " + OPENAI_API_KEY);
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(json));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            String body = EntityUtils.toString(response.getEntity());
//            System.out.println("🔍 OpenAI Response 1: " + body);
            JsonNode root = mapper.readTree(body);

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("OpenAI API did not return valid choices: " + root.toPrettyString());
            }

            JsonNode message = choices.get(0).path("message");
            history.add(mapper.convertValue(message, new TypeReference<>() {}));

            // STEP 2 — If tool_calls present, handle the tool
            if (message.has("tool_calls")) {
                JsonNode toolCall = message.path("tool_calls").get(0);
                String functionName = toolCall.path("function").path("name").asText();
                String argsJson = toolCall.path("function").path("arguments").asText();

                Map<String, Object> args = mapper.readValue(argsJson, new TypeReference<>() {});

                List<Map<String, Object>> toolOutput = switch (functionName) {
                    case "get_nearby_places" -> GooglePlacesService.getNearbyPlaces(
                            args.get("location").toString(),
                            args.get("place_type").toString(),  // Changed from "cuisine" to "place_type"
                            args.getOrDefault("keyword", "").toString(),  // New optional keyword parameter
                            (Integer) args.getOrDefault("radius", 3000)
                    );

                    default -> throw new RuntimeException("Unsupported function: " + functionName);
                };

                // Add tool message ONLY after tool_call
                history.add(Map.of(
                        "role", "tool",
                        "tool_call_id", toolCall.path("id").asText(),
                        "name", functionName,
                        "content", mapper.writeValueAsString(toolOutput)
                ));

                // STEP 3 — Final GPT call with tool result
                Map<String, Object> secondRequest = Map.of(
                        "model", GPT_MODEL,
                        "messages", history
                );

                String secondJson = mapper.writeValueAsString(secondRequest);

                HttpPost secondPost = new HttpPost("https://api.openai.com/v1/chat/completions");
                secondPost.setHeader("Authorization", "Bearer " + OPENAI_API_KEY);
                secondPost.setHeader("Content-Type", "application/json");
                secondPost.setEntity(new StringEntity(secondJson));

                try (CloseableHttpClient client2 = HttpClients.createDefault();
                     CloseableHttpResponse response2 = client2.execute(secondPost)) {

                    String secondBody = EntityUtils.toString(response2.getEntity());
//                    System.out.println("✅ OpenAI Response 2: " + secondBody);
                    JsonNode root2 = mapper.readTree(secondBody);

                    JsonNode finalMessage = root2.path("choices").get(0).path("message");
                    String finalContent = finalMessage.path("content").asText();

                    history.add(Map.of(
                            "role", "assistant",
                            "content", finalContent
                    ));

//                    System.out.println("api response {}",);
                    return mapper.valueToTree(toolOutput);
                }
            } else {
                // No tool_call, just return the message
                return message.path("content");
            }
        }
    }


}
