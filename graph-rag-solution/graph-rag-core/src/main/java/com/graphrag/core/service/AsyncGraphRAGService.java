package com.graphrag.core.service;

import com.graphrag.core.algorithm.*;
import com.graphrag.core.algorithm.QueryUnderstandingAlgorithm.QueryAnalysis;
import com.graphrag.core.algorithm.ContextFusionAlgorithm.FusedContext;
import com.graphrag.core.algorithm.VectorRetrievalAlgorithm.ScoredResult;
import com.graphrag.data.entity.DocumentNode;
import com.graphrag.data.entity.EntityNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AsyncGraphRAGService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncGraphRAGService.class);

    @Autowired
    private QueryUnderstandingAlgorithm queryUnderstandingAlgorithm;

    @Autowired
    private ContextFusionAlgorithm contextFusionAlgorithm;

    @Autowired
    private VectorRetrievalAlgorithm vectorRetrievalAlgorithm;

    @Autowired
    private ResultRankingAlgorithm resultRankingAlgorithm;

    @Autowired
    private GraphTraversalAlgorithm graphTraversalAlgorithm;

    @Autowired
    private AnswerGenerationAlgorithm answerGenerationAlgorithm;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 異步處理查詢並生成答案
     */
    public CompletableFuture<AnswerGenerationAlgorithm.StructuredAnswer> processQueryAsync(
            String query,
            List<DocumentNode> documents,
            List<EntityNode> entities,
            List<Map<String, Object>> graphRelations) {

        // 1. 異步查詢理解
        CompletableFuture<QueryAnalysis> queryAnalysisFuture = CompletableFuture
                .supplyAsync(() -> queryUnderstandingAlgorithm.analyzeQuery(query), executorService);

        // 2. 異步向量檢索
        CompletableFuture<List<ScoredResult<DocumentNode>>> vectorRetrievalFuture = queryAnalysisFuture
                .thenApplyAsync(analysis -> {
                    List<String> expandedQueries = analysis.getExpandedQueries();
                    return vectorRetrievalAlgorithm.multiQueryVectorRetrieval(expandedQueries, documents, 10);
                }, executorService);

        // 3. 異步圖遍歷
        CompletableFuture<List<Map<String, Object>>> graphTraversalFuture = queryAnalysisFuture
                .thenApplyAsync(analysis -> {
                    if (!analysis.getKeyEntities().isEmpty()) {
                        String startEntity = analysis.getKeyEntities().get(0);
                        return graphTraversalAlgorithm.multiHopEntityRetrieval(startEntity, 3, 10);
                    }
                    return List.of();
                }, executorService);

        // 4. 異步結果排序
        CompletableFuture<List<ScoredResult<DocumentNode>>> rankedResultsFuture = vectorRetrievalFuture
                .thenApplyAsync(results -> {
                    ResultRankingAlgorithm.RankingConfig config = new ResultRankingAlgorithm.RankingConfig();
                    config.setMinRelevanceThreshold(0.5);
                    config.setDiversityThreshold(0.3);
                    config.setMaxResults(5);
                    return resultRankingAlgorithm.comprehensiveRankingAndFiltering(results, config);
                }, executorService);

        // 5. 異步上下文融合
        CompletableFuture<FusedContext> fusedContextFuture = CompletableFuture.allOf(
                rankedResultsFuture,
                graphTraversalFuture
        ).thenApplyAsync(v -> {
            List<ScoredResult<DocumentNode>> rankedDocs = rankedResultsFuture.join();
            List<Map<String, Object>> graphResults = graphTraversalFuture.join();
            
            List<DocumentNode> relevantDocs = rankedDocs.stream()
                    .map(ScoredResult::getItem)
                    .toList();

            return contextFusionAlgorithm.fuseMultiSourceContext(
                    relevantDocs,
                    entities,
                    graphResults,
                    query
            );
        }, executorService);

        // 6. 異步生成答案
        return CompletableFuture.allOf(
                queryAnalysisFuture,
                fusedContextFuture
        ).thenApplyAsync(v -> {
            QueryAnalysis analysis = queryAnalysisFuture.join();
            FusedContext context = fusedContextFuture.join();
            return answerGenerationAlgorithm.generateStructuredAnswer(query, context, analysis);
        }, executorService);
    }

    /**
     * 異步多跳實體檢索
     */
    @Async
    public CompletableFuture<List<Map<String, Object>>> multiHopEntityRetrievalAsync(
            String startEntity,
            int maxHops,
            int maxResults) {
        return CompletableFuture.supplyAsync(() ->
                graphTraversalAlgorithm.multiHopEntityRetrieval(startEntity, maxHops, maxResults),
                executorService);
    }

    /**
     * 異步社區檢測
     */
    @Async
    public CompletableFuture<List<Map<String, Object>>> detectCommunitiesAsync(
            List<String> entityNames,
            double threshold) {
        return CompletableFuture.supplyAsync(() ->
                graphTraversalAlgorithm.detectCommunities(entityNames, threshold),
                executorService);
    }

    /**
     * 異步相似實體聚類
     */
    @Async
    public CompletableFuture<List<List<String>>> clusterSimilarEntitiesAsync(
            List<String> entityNames,
            double similarityThreshold) {
        return CompletableFuture.supplyAsync(() ->
                graphTraversalAlgorithm.clusterSimilarEntities(entityNames, similarityThreshold),
                executorService);
    }

    /**
     * 異步多維度排序
     */
    @Async
    public <T> CompletableFuture<List<ScoredResult<T>>> multiFactorRankingAsync(
            List<ScoredResult<T>> results,
            Map<String, Double> factorWeights) {
        return CompletableFuture.supplyAsync(() ->
                resultRankingAlgorithm.multiFactorRanking(results, factorWeights),
                executorService);
    }

    /**
     * 異步多查詢向量檢索
     */
    @Async
    public CompletableFuture<List<ScoredResult<DocumentNode>>> multiQueryVectorRetrievalAsync(
            List<String> queries,
            List<DocumentNode> candidates,
            int topK) {
        return CompletableFuture.supplyAsync(() ->
                vectorRetrievalAlgorithm.multiQueryVectorRetrieval(queries, candidates, topK),
                executorService);
    }
} 