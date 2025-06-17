package com.graphrag.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Spring Security Configuration
 *
 * Configuration details:
 * 1. Disable default login page
 * 2. Allow all API requests without authentication
 * 3. Configure CORS and security headers
 * 4. Disable CSRF (not needed for API services)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF protection (typically not needed for API services)
            .csrf(csrf -> csrf.disable())
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Allow health check endpoint
                .requestMatchers("/api/v1/graph-rag/health").permitAll()
                
                // Allow Swagger documentation access
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**", "/api-docs/**").permitAll()
                
                // Allow Actuator monitoring endpoints
                .requestMatchers("/actuator/**").permitAll()
                
                // Allow all API requests
                .requestMatchers("/api/**").permitAll()
                
                // Allow static resources
                .requestMatchers("/static/**", "/public/**").permitAll()
                
                // Other requests require authentication (if any)
                .anyRequest().permitAll()
            )
            
            // Configure session management (stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Disable default login page
            .formLogin(form -> form.disable())
            
            // Disable HTTP Basic authentication
            .httpBasic(basic -> basic.disable())
            
            // Configure security headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            );

        return http.build();
    }
}