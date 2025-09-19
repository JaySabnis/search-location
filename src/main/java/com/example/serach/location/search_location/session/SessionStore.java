package com.example.serach.location.search_location.session;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore {

    private final Map<String, Instant> sessions = new ConcurrentHashMap<>();// use redis to store, better than in memery data which can cause issue on restart.
    private final Duration SESSION_DURATION = Duration.ofMinutes(30); // change as needed

    // Create a new session
    public void create(String sessionId) {
        sessions.put(sessionId, Instant.now().plus(SESSION_DURATION));
    }

    // Validate session and apply rolling expiry
    public boolean isValid(String sessionId) {
        Instant expiry = sessions.get(sessionId);
        if (expiry == null || Instant.now().isAfter(expiry)) {
            sessions.remove(sessionId);
            return false;
        }
        // Rolling expiry: extend session
        sessions.put(sessionId, Instant.now().plus(SESSION_DURATION));
        return true;
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}
