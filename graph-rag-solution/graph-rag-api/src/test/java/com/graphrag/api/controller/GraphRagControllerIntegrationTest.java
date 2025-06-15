package com.graphrag.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphrag.core.model.GraphRagRequest;
import com.graphrag.core.model.GraphRagResponse;
import com.graphrag.core.service.GraphRagRetrievalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 图 RAG 控制器集成测试
 */
@WebMvcTest(GraphRagController.class)
class GraphRagControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GraphRagRetrievalService retrievalService;

    @Test
    void testQueryEndpoint_Success() throws Exception {
        // 准备测试数据
        GraphRagRequest request = new GraphRagRequest("什么是人工智能？");
        
        GraphRagResponse mockResponse = new GraphRagResponse();
        mockResponse.setQuestion("什么是人工智能？");
        mockResponse.setAnswer("人工智能是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。");
        mockResponse.setRelevantDocuments(Arrays.asList(
                Map.of("id", 1L, "title", "AI概述", "source", "test")
        ));
        mockResponse.setRelevantEntities(Arrays.asList(
                Map.of("id", 1L, "name", "人工智能", "type", "概念")
        ));
        mockResponse.setProcessingTimeMs(500L);
        
        // 配置 Mock 行为
        when(retrievalService.retrieve(any(GraphRagRequest.class))).thenReturn(mockResponse);
        
        // 执行测试
        mockMvc.perform(post("/api/v1/graph-rag/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.question").value("什么是人工智能？"))
                .andExpect(jsonPath("$.data.answer").exists())
                .andExpect(jsonPath("$.data.relevantDocuments").isArray())
                .andExpect(jsonPath("$.data.relevantEntities").isArray())
                .andExpect(jsonPath("$.data.processingTimeMs").exists());
    }

    @Test
    void testQueryEndpoint_InvalidRequest() throws Exception {
        // 测试无效请求
        GraphRagRequest invalidRequest = new GraphRagRequest("");
        
        mockMvc.perform(post("/api/v1/graph-rag/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testAnalyzeEndpoint_Success() throws Exception {
        mockMvc.perform(post("/api/v1/graph-rag/analyze")
                .param("query", "什么是机器学习？"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testHealthEndpoint_Success() throws Exception {
        mockMvc.perform(get("/api/v1/graph-rag/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testStatsEndpoint_Success() throws Exception {
        mockMvc.perform(get("/api/v1/graph-rag/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testAsyncQueryEndpoint_Success() throws Exception {
        GraphRagRequest request = new GraphRagRequest("异步查询测试");
        
        mockMvc.perform(post("/api/v1/graph-rag/query/async")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists()); // 应该返回任务ID
    }
}

