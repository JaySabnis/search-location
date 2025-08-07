package com.example.serach.location.search_location.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenAIService {
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String GPT_MODEL = "gpt-4.1-nano";
    private static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode callWithFunction(List<Map<String, Object>> inputHistory) throws IOException {
        // Create fresh history for this request
        List<Map<String, Object>> history = new ArrayList<>(inputHistory);
        logHistory("Initial history", history);

        // Define the tool function
        Map<String, Object> toolFunction = createToolFunction();

        // First API call
        Map<String, Object> request = Map.of(
                "model", GPT_MODEL,
                "messages", history,
                "tools", List.of(toolFunction),
                "tool_choice", "auto"
        );

        String requestJson = mapper.writeValueAsString(request);
        logDebug("Initial request", requestJson);

        HttpPost post = createPostRequest(requestJson);
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            String body = EntityUtils.toString(response.getEntity());
            logDebug("Initial response", body);

            JsonNode root = mapper.readTree(body);

            if (root.has("error")) {
                String error = root.path("error").toString();
                logError("OpenAI API error", error);
                throw new RuntimeException("OpenAI API error: " + error);
            }

            JsonNode message = root.path("choices").get(0).path("message");
            Map<String, Object> assistantMessage = mapper.convertValue(message, new TypeReference<>() {});
            history.add(assistantMessage);
            logHistory("After adding assistant message", history);

            if (message.has("tool_calls")) {
                JsonNode toolCall = message.path("tool_calls").get(0);
                String callId = toolCall.path("id").asText();
                String functionName = toolCall.path("function").path("name").asText();
                String argsJson = toolCall.path("function").path("arguments").asText();
                logDebug("Tool call arguments", argsJson);

                // Process the tool call
                List<Map<String, Object>> toolOutput = processToolCallSafely(functionName, argsJson);
                logDebug("Tool output", mapper.writeValueAsString(toolOutput));

                // Add tool response to history
                Map<String, Object> toolResponse = Map.of(
                        "role", "tool",
                        "tool_call_id", callId,
                        "name", functionName,
                        "content", mapper.writeValueAsString(toolOutput)
                );
                history.add(toolResponse);
                logHistory("After adding tool response", history);

                // Make final API call
                return makeFinalCall(history, toolOutput);
            } else {
                logDebug("No tool calls in response", "");
                return message.path("content");
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> createToolFunction() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "get_nearby_places",
                        "description", "Get nearby places of a specific type",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "location", Map.of("type", "string", "description", "The city/area to search in"),
                                        "place_type", Map.of(
                                                "type", "string",
                                                "description", "Type of place",
                                                "enum", List.of("restaurant", "hotel", "park", "zoo", "museum", "cafe", "shopping_mall", "apartment")
                                        ),
                                        "keyword", Map.of("type", "string", "description", "Optional filter"),
                                        "radius", Map.of("type", "integer", "description", "Search radius in meters", "default", 3000)
                                ),
                                "required", List.of("location", "place_type")
                        )
                )
        );
    }

    private static List<Map<String, Object>> processToolCallSafely(String functionName, String argsJson) {
        try {
            Map<String, Object> args = mapper.readValue(argsJson, new TypeReference<>() {});
            if ("get_nearby_places".equals(functionName)) {
                return GooglePlacesService.getNearbyPlaces(
                        args.get("location").toString(),
                        args.get("place_type").toString(),
                        args.getOrDefault("keyword", "").toString(),
                        (Integer) args.getOrDefault("radius", 3000)
                );
            }
            throw new RuntimeException("Unsupported function: " + functionName);
        } catch (Exception e) {
            logError("Tool processing failed", e.getMessage());
            return List.of(); // Return empty list on failure
        }
    }

    private static JsonNode makeFinalCall(List<Map<String, Object>> history, List<Map<String, Object>> toolOutput)
            throws IOException {
        Map<String, Object> request = Map.of(
                "model", GPT_MODEL,
                "messages", history
        );

        String requestJson = mapper.writeValueAsString(request);
        logDebug("Final request", requestJson);

        HttpPost post = createPostRequest(requestJson);
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            String body = EntityUtils.toString(response.getEntity());
            logDebug("Final response", body);

            JsonNode root = mapper.readTree(body);

            if (root.has("error")) {
                String error = root.path("error").toString();
                logError("Final API call error", error);
                throw new RuntimeException("OpenAI API error: " + error);
            }

            JsonNode finalMessage = root.path("choices").get(0).path("message");
            history.add(mapper.convertValue(finalMessage, new TypeReference<>() {}));
            logHistory("After final assistant message", history);

            return mapper.valueToTree(toolOutput);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpPost createPostRequest(String jsonBody) {
        HttpPost post = new HttpPost("https://api.openai.com/v1/chat/completions");
        post.setHeader("Authorization", "Bearer " + OPENAI_API_KEY);
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        return post;
    }

    // Logging methods
    private static void logHistory(String stage, List<Map<String, Object>> history) {
        try {
            System.out.println("[" + stage + "] History: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(history));
        } catch (Exception e) {
            System.out.println("[" + stage + "] Could not log history: " + e.getMessage());
        }
    }

    private static void logDebug(String label, String message) {
        System.out.println("[DEBUG] " + label + ": " + message);
    }

    private static void logError(String context, String error) {
        System.err.println("[ERROR] " + context + ": " + error);
    }
}
