package com.graphrag.core.service;

import com.graphrag.core.algorithm.QueryUnderstandingAlgorithm;
import com.graphrag.core.model.GraphRagRequest;
import com.graphrag.core.model.GraphRagResponse;
import com.graphrag.data.entity.DocumentNode;
import com.graphrag.data.entity.EntityNode;
import com.graphrag.data.service.DocumentService;
import com.graphrag.data.service.EntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 图 RAG 检索服务测试
 */
@ExtendWith(MockitoExtension.class)
class GraphRagRetrievalServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentService documentService;

    @Mock
    private EntityService entityService;

    @Mock
    private QueryUnderstandingAlgorithm queryUnderstanding;

    @InjectMocks
    private GraphRagRetrievalService retrievalService;

    private GraphRagRequest testRequest;
    private List<DocumentNode> mockDocuments;
    private List<EntityNode> mockEntities;

    @BeforeEach
    void setUp() {
        testRequest = new GraphRagRequest("什么是人工智能？");
        
        // 创建模拟文档
        DocumentNode doc1 = new DocumentNode("AI概述", "人工智能是计算机科学的一个分支", "test");
        doc1.setId(1L);
        doc1.setEmbedding(Arrays.asList(0.1, 0.2, 0.3));
        
        DocumentNode doc2 = new DocumentNode("机器学习", "机器学习是AI的重要组成部分", "test");
        doc2.setId(2L);
        doc2.setEmbedding(Arrays.asList(0.2, 0.3, 0.4));
        
        mockDocuments = Arrays.asList(doc1, doc2);
        
        // 创建模拟实体
        EntityNode entity1 = new EntityNode("人工智能", "概念");
        entity1.setId(1L);
        entity1.setEmbedding(Arrays.asList(0.1, 0.2, 0.3));
        
        EntityNode entity2 = new EntityNode("机器学习", "技术");
        entity2.setId(2L);
        entity2.setEmbedding(Arrays.asList(0.2, 0.3, 0.4));
        
        mockEntities = Arrays.asList(entity1, entity2);
    }

    @Test
    void testRetrieve_Success() {
        // 准备测试数据
        List<Double> queryEmbedding = Arrays.asList(0.1, 0.2, 0.3);
        
        // 配置 Mock 行为
        when(embeddingService.embedText(anyString())).thenReturn(queryEmbedding);
        when(documentService.findSimilarDocuments(any(), anyDouble(), anyInt())).thenReturn(mockDocuments);
        when(entityService.findSimilarEntities(any(), anyDouble(), anyInt())).thenReturn(mockEntities);
        
        // 执行测试
        GraphRagResponse response = retrievalService.retrieve(testRequest);
        
        // 验证结果
        assertNotNull(response);
        assertEquals(testRequest.getQuestion(), response.getQuestion());
        assertNotNull(response.getAnswer());
        assertNotNull(response.getRelevantDocuments());
        assertNotNull(response.getRelevantEntities());
        
        // 验证 Mock 调用
        verify(embeddingService).embedText(testRequest.getQuestion());
        verify(documentService).findSimilarDocuments(any(), eq(0.7), eq(5));
        verify(entityService).findSimilarEntities(any(), eq(0.7), eq(10));
    }

    @Test
    void testRetrieve_EmptyResults() {
        // 配置 Mock 返回空结果
        when(embeddingService.embedText(anyString())).thenReturn(Arrays.asList(0.1, 0.2, 0.3));
        when(documentService.findSimilarDocuments(any(), anyDouble(), anyInt())).thenReturn(Arrays.asList());
        when(entityService.findSimilarEntities(any(), anyDouble(), anyInt())).thenReturn(Arrays.asList());
        
        // 执行测试
        GraphRagResponse response = retrievalService.retrieve(testRequest);
        
        // 验证结果
        assertNotNull(response);
        assertTrue(response.getRelevantDocuments().isEmpty());
        assertTrue(response.getRelevantEntities().isEmpty());
    }

    @Test
    void testHybridRetrieve_Success() {
        // 准备测试数据
        List<Double> queryEmbedding = Arrays.asList(0.1, 0.2, 0.3);
        
        // 配置 Mock 行为
        when(embeddingService.embedText(anyString())).thenReturn(queryEmbedding);
        when(documentService.findSimilarDocuments(any(), anyDouble(), anyInt())).thenReturn(mockDocuments);
        when(documentService.searchByContent(anyString())).thenReturn(mockDocuments);
        when(entityService.findSimilarEntities(any(), anyDouble(), anyInt())).thenReturn(mockEntities);
        
        // 执行测试
        GraphRagResponse response = retrievalService.hybridRetrieve(testRequest);
        
        // 验证结果
        assertNotNull(response);
        assertEquals(testRequest.getQuestion(), response.getQuestion());
        assertNotNull(response.getAnswer());
        
        // 验证混合检索调用了多种方法
        verify(documentService).findSimilarDocuments(any(), anyDouble(), anyInt());
        verify(documentService).searchByContent(testRequest.getQuestion());
    }

    @Test
    void testRetrieve_WithCustomParameters() {
        // 创建自定义参数的请求
        GraphRagRequest customRequest = new GraphRagRequest("测试查询");
        customRequest.setMaxDocuments(3);
        customRequest.setMaxEntities(5);
        customRequest.setSimilarityThreshold(0.8);
        
        List<Double> queryEmbedding = Arrays.asList(0.1, 0.2, 0.3);
        
        // 配置 Mock 行为
        when(embeddingService.embedText(anyString())).thenReturn(queryEmbedding);
        when(documentService.findSimilarDocuments(any(), anyDouble(), anyInt())).thenReturn(mockDocuments);
        when(entityService.findSimilarEntities(any(), anyDouble(), anyInt())).thenReturn(mockEntities);
        
        // 执行测试
        GraphRagResponse response = retrievalService.retrieve(customRequest);
        
        // 验证使用了自定义参数
        verify(documentService).findSimilarDocuments(any(), eq(0.8), eq(3));
        verify(entityService).findSimilarEntities(any(), eq(0.8), eq(5));
    }
}

