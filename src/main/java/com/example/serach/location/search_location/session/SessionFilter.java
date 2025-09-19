package com.example.serach.location.search_location.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SessionFilter extends OncePerRequestFilter {
    @Autowired
    private SessionStore sessionStore;

    private static final List<String> EXCLUDE_URLS = List.of(
            "/auth/browser-handshake" // handshake endpoint doesn't need session
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // <-- Add this line here
        System.out.println("SessionFilter path=" + request.getRequestURI() + ", method=" + request.getMethod());


        String path = request.getRequestURI();
        if (EXCLUDE_URLS.contains(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get SESSION_ID cookie
        Cookie[] cookies = request.getCookies();
        String sessionId = null;
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("SESSION_ID".equals(c.getName())) {
                    sessionId = c.getValue();
                    break;
                }
            }
        }

        if (sessionId == null || !sessionStore.isValid(sessionId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"invalid-session\"}");
            return;
        }

        // proceed
        filterChain.doFilter(request, response);
    }
}
