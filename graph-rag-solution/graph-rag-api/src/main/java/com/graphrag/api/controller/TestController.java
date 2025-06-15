package com.graphrag.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 測試控制器
 * 用於驗證 Spring Boot 應用是否正常工作
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of(
            "message", "Hello from Graph RAG API!",
            "timestamp", LocalDateTime.now(),
            "status", "OK"
        );
    }

    @GetMapping("/health")
    public Map<String, Object> simpleHealth() {
        return Map.of(
            "status", "UP",
            "service", "Graph RAG API",
            "timestamp", LocalDateTime.now()
        );
    }
} 