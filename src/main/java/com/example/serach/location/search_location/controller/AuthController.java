package com.example.serach.location.search_location.controller;

import com.example.serach.location.search_location.session.SessionStore;
import com.example.serach.location.search_location.util.FingerprintUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
public class AuthController {

    @Autowired
    private SessionStore sessionStore;

    @Autowired
    private FingerprintUtil fingerprintUtil;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${recaptcha.secret}")
    private String recaptchaSecret;

    @PostMapping("/browser-handshake")
    public ResponseEntity<?> handshake(@RequestBody Map<String,String> body, HttpServletRequest request, HttpServletResponse response) {
        String token = body.get("token");
        System.out.println("Received reCAPTCHA token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "NULL"));

        if (token == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "token_required"));
        }

        // Verify reCAPTCHA
        String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", recaptchaSecret);
        params.add("response", token);

        System.out.println("Sending reCAPTCHA verification to Google...");

        try {
            Map<?, ?> result = restTemplate.postForObject(verifyUrl, params, Map.class);
            System.out.println("Google reCAPTCHA response: " + result);

            boolean success = result != null && Boolean.TRUE.equals(result.get("success"));
            if (!success) {
                System.out.println("reCAPTCHA verification failed!");
                if (result != null) {
                    System.out.println("Error codes: " + result.get("error-codes"));
                }
                return ResponseEntity.status(403).body(Map.of("error", "bot_detected", "details", result));
            }

            // Create session with consistent fingerprint
            String sessionId = UUID.randomUUID().toString();
            String userAgent = request.getHeader("User-Agent");
            String clientIp = fingerprintUtil.getClientIp(request);
            String fingerprint = fingerprintUtil.createConsistentFingerprint(request);

            sessionStore.create(sessionId, fingerprint, userAgent, clientIp, true);

            ResponseCookie cookie = ResponseCookie.from("SESSION_ID", sessionId)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(Duration.ofMinutes(15))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "sessionId", sessionId
            ));

        } catch (Exception e) {
            System.out.println("Error during reCAPTCHA verification: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "recaptcha_verification_failed"));
        }
    }
}