package com.example.serach.location.search_location.handler;


import com.example.serach.location.search_location.service.OpenAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class FoodBotWebSocketHandler extends TextWebSocketHandler {

    private List<Map<String, Object>> messagesHistory = new ArrayList<>();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException, ParseException {
        String userQuery = message.getPayload();
        messagesHistory.add(Map.of("role", "user", "content", userQuery));

        // Step 1: Call OpenAI
        JsonNode openAIResponse = OpenAIService.callWithFunction(messagesHistory);

        ObjectMapper mapper = new ObjectMapper();
        session.sendMessage(new TextMessage(mapper.writeValueAsString(openAIResponse)));
    }


}
