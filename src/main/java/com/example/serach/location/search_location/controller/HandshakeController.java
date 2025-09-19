package com.example.serach.location.search_location.controller;

import com.example.serach.location.search_location.session.SessionStore;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/auth")
public class HandshakeController {

    @Autowired
    private SessionStore sessionStore;

    @Value("${recaptcha.secret}")
    private String recaptchaSecret;

    private final RestTemplate rest = new RestTemplate();

    @PostMapping("/browser-handshake")
    public ResponseEntity<?> handshake(@RequestBody Map<String,String> body, HttpServletResponse response) {
        String token = body.get("token");
        if (token == null) return ResponseEntity.badRequest().build();

        // Verify with Google
        String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", recaptchaSecret);
        params.add("response", token);
        Map result = rest.postForObject(verifyUrl, params, Map.class);

        boolean success = result != null && Boolean.TRUE.equals(result.get("success"));
        if (!success) {
            System.out.println("Inside the block to block");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","bot-detected"));
        }

        // Create session
        String sessionId = UUID.randomUUID().toString();
        // Persist in SessionStore
        sessionStore.create(sessionId);

        // Dev vs Prod cookie setup
        boolean isDev = false;
        ResponseCookie cookie = ResponseCookie.from("SESSION_ID", sessionId)
                .httpOnly(true)
                .secure(!isDev)                   // secure=true in prod
                .sameSite(isDev ? "Lax" : "None") // Lax in dev, None in prod
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("status","ok"));
    }


}

