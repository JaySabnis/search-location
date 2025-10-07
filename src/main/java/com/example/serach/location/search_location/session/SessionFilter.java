package com.example.serach.location.search_location.session;

import com.example.serach.location.search_location.util.FingerprintUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class SessionFilter extends OncePerRequestFilter {

    @Autowired
    private SessionStore sessionStore;

    @Autowired
    private FingerprintUtil fingerprintUtil;

    // Define patterns that should be excluded
    private static final List<String> EXCLUDE_PATTERNS = Arrays.asList(
            "/auth/browser-handshake",
            "/health",
            "/error",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        System.out.println("SessionFilter checking: " + path);

        // Check if this path should be excluded
        boolean shouldExclude = EXCLUDE_PATTERNS.stream().anyMatch(path::startsWith);

        if (shouldExclude || "OPTIONS".equalsIgnoreCase(method)) {
            System.out.println("Skipping filter for: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        // Protect all other routes including /v1/search/**
        String sessionId = extractSessionId(request);
        String fingerprint = fingerprintUtil.createConsistentFingerprint(request);
        String userAgent = request.getHeader("User-Agent");
        String clientIp = fingerprintUtil.getClientIp(request);

        logRequestDetails(request, sessionId, fingerprint);

        if (sessionId == null || !sessionStore.isValid(sessionId, fingerprint, userAgent, clientIp)) {
            logBlockedRequest(request, sessionId, fingerprint, userAgent, clientIp);

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"invalid_session\",\"message\":\"Session validation failed. Please complete reCAPTCHA.\"}");
            return;
        }


        String requestSignature = request.getHeader("X-Request-Signature");
        String timestamp = request.getHeader("X-Request-Timestamp");

        if (requestSignature == null || timestamp == null ||
                !sessionStore.validateRequestSignature(sessionId, requestSignature,
                        request.getRequestURI(), request.getMethod(), timestamp)) {

            System.out.println("Request signature validation failed for session: " + sessionId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"invalid_signature\",\"message\":\"Request signature validation failed.\"}");
            return;
        }

// Also add timestamp validation to prevent replay attacks
        long requestTime = Long.parseLong(timestamp);
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - requestTime) > 30000) { // 30 seconds window
            System.out.println("Request timestamp expired for session: " + sessionId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"expired_timestamp\",\"message\":\"Request timestamp expired.\"}");
            return;
        }

        if (sessionStore.isSignatureReplayed(sessionId, requestSignature)) {
            System.out.println("Replay attack detected for session: " + sessionId);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"replay_attack\",\"message\":\"Request signature already used.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SESSION_ID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void logRequestDetails(HttpServletRequest request, String sessionId, String fingerprint) {
        System.out.println("Request: " + request.getMethod() + " " + request.getRequestURI());
        System.out.println("Session ID: " + sessionId);
        System.out.println("User-Agent: " + request.getHeader("User-Agent"));
        System.out.println("IP: " + fingerprintUtil.getClientIp(request));
        System.out.println("Fingerprint: " + fingerprint);
        System.out.println("----------------------------------------");
    }

    private void logBlockedRequest(HttpServletRequest request, String sessionId, String fingerprint,
                                   String userAgent, String clientIp) {
        System.out.println("🚫 BLOCKED REQUEST 🚫");
        System.out.println("URL: " + request.getMethod() + " " + request.getRequestURI());
        System.out.println("Session ID: " + sessionId);
        System.out.println("User-Agent: " + userAgent);
        System.out.println("Client IP: " + clientIp);
        System.out.println("Fingerprint: " + fingerprint);
        System.out.println("========================================");
    }
}