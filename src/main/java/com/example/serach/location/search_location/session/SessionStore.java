package com.example.serach.location.search_location.session;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class SessionStore {

        public static class SessionInfo {
            public Instant expiry;
            public String fingerprint;
            public Instant lastActivity;
            public String userAgent;
            public String ipAddress;
            public int requestCount;
            public boolean recaptchaVerified;
            public Instant createdAt;
            public String secretKey;
            public Set<String> usedSignatures = ConcurrentHashMap.newKeySet();

            public SessionInfo(Instant expiry, String fingerprint, String userAgent, String ipAddress, boolean recaptchaVerified) {
                this.expiry = expiry;
                this.fingerprint = fingerprint;
                this.userAgent = userAgent;
                this.ipAddress = ipAddress;
                this.recaptchaVerified = recaptchaVerified;
                this.lastActivity = Instant.now();
                this.createdAt = Instant.now();
                this.requestCount = 0;
                this.secretKey = UUID.randomUUID().toString();
            }
        }

        private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
        private final Duration SESSION_DURATION = Duration.ofMinutes(15);
        private final int MAX_REQUESTS_PER_MINUTE = 60;

        public void create(String sessionId, String fingerprint, String userAgent, String ipAddress, boolean recaptchaVerified) {
            sessions.put(sessionId, new SessionInfo(
                    Instant.now().plus(SESSION_DURATION),
                    fingerprint,
                    userAgent,
                    ipAddress,
                    recaptchaVerified
            ));
            System.out.println("Session created: " + sessionId + " | reCAPTCHA: " + recaptchaVerified);
        }

        public boolean isValid(String sessionId, String fingerprint, String userAgent, String ipAddress) {
            SessionInfo info = sessions.get(sessionId);

            if (info == null) {
                System.out.println("Session not found: " + sessionId);
                return false;
            }

            if (Instant.now().isAfter(info.expiry)) {
                System.out.println("Session expired: " + sessionId);
                sessions.remove(sessionId);
                return false;
            }

            if (!info.recaptchaVerified) {
                System.out.println("Session without reCAPTCHA verification: " + sessionId);
                return false;
            }

            // Allow grace period for new sessions (2 seconds)
            if (Duration.between(info.createdAt, Instant.now()).toMillis() < 2000) {
                System.out.println("Allowing grace period for new session: " + sessionId);
                info.lastActivity = Instant.now();
                info.requestCount++;
                return true;
            }

            if (Duration.between(info.lastActivity, Instant.now()).toMillis() < 100) {
                System.out.println("Request too fast - possible replay attack: " + sessionId);
                return false;
            }

            if (info.requestCount > MAX_REQUESTS_PER_MINUTE) {
                System.out.println("Rate limit exceeded: " + sessionId);
                return false;
            }

            if (!info.fingerprint.equals(fingerprint)) {
                System.out.println("Fingerprint mismatch for session: " + sessionId);
                System.out.println("Stored: " + info.fingerprint);
                System.out.println("Current: " + fingerprint);
                return false;
            }

            if (!info.userAgent.equals(userAgent)) {
                System.out.println("User-Agent mismatch for session: " + sessionId);
                return false;
            }

            if (!info.ipAddress.equals(ipAddress)) {
                System.out.println("IP address mismatch for session: " + sessionId);
                return false;
            }

            info.lastActivity = Instant.now();
            info.requestCount++;
            info.expiry = Instant.now().plus(SESSION_DURATION);

            return true;
        }

        public void remove(String sessionId) {
            sessions.remove(sessionId);
        }

        public SessionInfo getSessionInfo(String sessionId) {
            return sessions.get(sessionId);
        }

        public void cleanupExpiredSessions() {
            Instant now = Instant.now();
            sessions.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiry));
        }

        // Add these methods to SessionStore class
        public String generateRequestSignature(String sessionId, String path, String method, String timestamp) {
            SessionInfo info = sessions.get(sessionId);
            if (info == null) return null;

            try {
                String data = sessionId + "|" + path + "|" + method + "|" + timestamp;
                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKeySpec = new SecretKeySpec(info.secretKey.getBytes(), "HmacSHA256");
                mac.init(secretKeySpec);
                byte[] signatureBytes = mac.doFinal(data.getBytes());
                return Base64.getEncoder().encodeToString(signatureBytes);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                return null;
            }
        }

        public boolean validateRequestSignature(String sessionId, String signature, String path, String method, String timestamp) {
            SessionInfo info = sessions.get(sessionId);
            if (info == null) return false;

            String expectedSignature = generateRequestSignature(sessionId, path, method, timestamp);
            return expectedSignature != null && expectedSignature.equals(signature);
        }

    public boolean isSignatureReplayed(String sessionId, String signature) {
        SessionInfo info = sessions.get(sessionId);
        if (info == null) return true;

        // Check if signature was already used
        if (info.usedSignatures.contains(signature)) {
            return true;
        }

        // Add to used signatures (with cleanup for memory management)
        info.usedSignatures.add(signature);

        // Clean up old signatures if set gets too large (prevent memory issues)
        if (info.usedSignatures.size() > 1000) {
            // Keep only the most recent 500 signatures
            List<String> recentSignatures = info.usedSignatures.stream()
                    .skip(info.usedSignatures.size() - 500)
                    .toList();
            info.usedSignatures.clear();
            info.usedSignatures.addAll(recentSignatures);
        }

        return false;
    }
}
