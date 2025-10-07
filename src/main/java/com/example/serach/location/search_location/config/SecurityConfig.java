package com.example.serach.location.search_location.config;

import com.example.serach.location.search_location.session.SessionFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final SessionFilter sessionFilter;

    public SecurityConfig(SessionFilter sessionFilter) {
        this.sessionFilter = sessionFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // enable CORS handling
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/browser-handshake").permitAll()
                        .requestMatchers("/v1/search/**").permitAll()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
