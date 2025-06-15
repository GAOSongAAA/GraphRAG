package com.graphrag.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Spring Security 配置
 * 
 * 配置說明：
 * 1. 禁用默認登錄頁面
 * 2. 允許所有 API 請求無需認證
 * 3. 配置 CORS 和安全頭
 * 4. 禁用 CSRF（API 服務不需要）
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF 保護（API 服務通常不需要）
            .csrf(csrf -> csrf.disable())
            
            // 配置授權規則
            .authorizeHttpRequests(authz -> authz
                // 允許健康檢查端點
                .requestMatchers("/api/v1/graph-rag/health").permitAll()
                
                // 允許 Swagger 文檔訪問
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**", "/api-docs/**").permitAll()
                
                // 允許 Actuator 監控端點
                .requestMatchers("/actuator/**").permitAll()
                
                // 允許所有 API 請求
                .requestMatchers("/api/**").permitAll()
                
                // 允許靜態資源
                .requestMatchers("/static/**", "/public/**").permitAll()
                
                // 其他請求需要認證（如果有的話）
                .anyRequest().permitAll()
            )
            
            // 配置會話管理（無狀態）
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 禁用默認登錄頁面
            .formLogin(form -> form.disable())
            
            // 禁用 HTTP Basic 認證
            .httpBasic(basic -> basic.disable())
            
            // 配置安全頭
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