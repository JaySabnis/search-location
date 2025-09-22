package com.example.serach.location.search_location.config;

import com.example.serach.location.search_location.session.SessionFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class FilterConfig {
        @Bean
        public FilterRegistrationBean<SessionFilter> sessionFilterRegistration(SessionFilter sessionFilter) {
            FilterRegistrationBean<SessionFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(sessionFilter);
            registration.addUrlPatterns("/*"); // Apply to all URLs
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // Make sure it runs first
            return registration;
        }
}
