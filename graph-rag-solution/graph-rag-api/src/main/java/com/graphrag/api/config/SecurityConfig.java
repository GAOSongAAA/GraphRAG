package com.graphrag.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Spring Security 設定
 *
 * 設定說明：
 * 1. 停用預設登入頁
 * 2. 所有 API 請求皆允許匿名存取
 * 3. 設定 CORS 與安全標頭
 * 4. 停用 CSRF（API 服務通常不需要）
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 停用 CSRF 保護（API 服務通常不需要）
            .csrf(csrf -> csrf.disable())

            // 設定授權規則
            .authorizeHttpRequests(authz -> authz
                // 健康檢查端點允許匿名
                .requestMatchers("/api/v1/graph-rag/health").permitAll()
                // Swagger 文件允許匿名
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**", "/api-docs/**").permitAll()
                // Actuator 監控端點允許匿名
                .requestMatchers("/actuator/**").permitAll()
                // 所有 API 請求允許匿名
                .requestMatchers("/api/**").permitAll()
                // 靜態資源允許匿名
                .requestMatchers("/static/**", "/public/**").permitAll()
                // 其他請求也允許匿名
                .anyRequest().permitAll()
            )

            // 設定 Session 管理為無狀態
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 停用預設登入頁
            .formLogin(form -> form.disable())

            // 停用 HTTP Basic 認證
            .httpBasic(basic -> basic.disable())

            // 設定安全標頭
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