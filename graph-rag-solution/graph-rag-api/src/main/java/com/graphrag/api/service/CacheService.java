package com.graphrag.api.service;

import com.graphrag.core.model.GraphRagRequest;
import com.graphrag.core.model.GraphRagResponse;
import com.graphrag.core.service.GraphRagRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 缓存服务
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private GraphRagRetrievalService retrievalService;

    /**
     * 缓存查询结果
     */
    @Cacheable(value = "queryResults", key = "#request.question + '_' + #request.retrievalMode")
    public GraphRagResponse getCachedQueryResult(GraphRagRequest request) {
        logger.debug("缓存未命中，执行查询: {}", request.getQuestion());
        
        if ("hybrid".equals(request.getRetrievalMode())) {
            return retrievalService.hybridRetrieve(request);
        } else {
            return retrievalService.retrieve(request);
        }
    }

    /**
     * 异步预热缓存
     */
    @Async("graphRagExecutor")
    public CompletableFuture<Void> preWarmCache(String[] commonQueries) {
        logger.info("开始预热缓存，查询数量: {}", commonQueries.length);
        
        for (String query : commonQueries) {
            try {
                GraphRagRequest request = new GraphRagRequest(query);
                getCachedQueryResult(request);
                logger.debug("预热缓存完成: {}", query);
            } catch (Exception e) {
                logger.error("预热缓存失败: {}", query, e);
            }
        }
        
        logger.info("缓存预热完成");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 缓存文档嵌入向量
     */
    @Cacheable(value = "documentEmbeddings", key = "#documentId")
    public java.util.List<Double> getCachedDocumentEmbedding(Long documentId) {
        // 这里应该调用实际的嵌入生成服务
        logger.debug("生成文档嵌入向量: {}", documentId);
        return null; // 实际实现中应该返回嵌入向量
    }

    /**
     * 缓存实体嵌入向量
     */
    @Cacheable(value = "entityEmbeddings", key = "#entityId")
    public java.util.List<Double> getCachedEntityEmbedding(Long entityId) {
        // 这里应该调用实际的嵌入生成服务
        logger.debug("生成实体嵌入向量: {}", entityId);
        return null; // 实际实现中应该返回嵌入向量
    }

    /**
     * 缓存图遍历结果
     */
    @Cacheable(value = "graphTraversalResults", key = "#entityName + '_' + #maxHops")
    public java.util.List<java.util.Map<String, Object>> getCachedGraphTraversal(String entityName, int maxHops) {
        // 这里应该调用实际的图遍历服务
        logger.debug("执行图遍历: {}, 最大跳数: {}", entityName, maxHops);
        return null; // 实际实现中应该返回遍历结果
    }
}

