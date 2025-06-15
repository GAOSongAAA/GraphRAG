package com.graphrag.core.algorithm;

import com.graphrag.core.service.EmbeddingService;
import com.graphrag.data.entity.DocumentNode;
import com.graphrag.data.entity.EntityNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量检索优化算法
 */
@Component
public class VectorRetrievalAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(VectorRetrievalAlgorithm.class);

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * 多查询向量检索
     */
    public List<ScoredResult<DocumentNode>> multiQueryVectorRetrieval(
            List<String> queries, List<DocumentNode> candidates, int topK) {
        
        logger.debug("多查询向量检索，查询数量: {}, 候选文档数量: {}", queries.size(), candidates.size());

        // 生成查询向量
        List<List<Double>> queryVectors = embeddingService.embedTexts(queries);

        // 计算每个候选文档与所有查询的相似度
        Map<DocumentNode, Double> documentScores = new HashMap<>();

        for (DocumentNode doc : candidates) {
            if (doc.getEmbedding() == null) {
                continue;
            }

            double maxSimilarity = 0.0;
            for (List<Double> queryVector : queryVectors) {
                double similarity = embeddingService.cosineSimilarity(queryVector, doc.getEmbedding());
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }
            documentScores.put(doc, maxSimilarity);
        }

        // 排序并返回前 K 个结果
        return documentScores.entrySet().stream()
                .sorted(Map.Entry.<DocumentNode, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> new ScoredResult<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 重排序算法
     */
    public List<ScoredResult<DocumentNode>> rerank(List<DocumentNode> candidates, String query, 
                                                  List<String> contextQueries) {
        logger.debug("重排序，候选数量: {}, 上下文查询数量: {}", candidates.size(), contextQueries.size());

        List<Double> queryVector = embeddingService.embedText(query);
        List<List<Double>> contextVectors = embeddingService.embedTexts(contextQueries);

        List<ScoredResult<DocumentNode>> results = new ArrayList<>();

        for (DocumentNode doc : candidates) {
            if (doc.getEmbedding() == null) {
                continue;
            }

            // 计算与主查询的相似度
            double mainSimilarity = embeddingService.cosineSimilarity(queryVector, doc.getEmbedding());

            // 计算与上下文查询的平均相似度
            double contextSimilarity = 0.0;
            if (!contextVectors.isEmpty()) {
                for (List<Double> contextVector : contextVectors) {
                    contextSimilarity += embeddingService.cosineSimilarity(contextVector, doc.getEmbedding());
                }
                contextSimilarity /= contextVectors.size();
            }

            // 综合评分（主查询权重 0.7，上下文权重 0.3）
            double finalScore = 0.7 * mainSimilarity + 0.3 * contextSimilarity;
            results.add(new ScoredResult<>(doc, finalScore));
        }

        return results.stream()
                .sorted(Comparator.comparing(ScoredResult<DocumentNode>::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 多样性检索
     */
    public List<ScoredResult<DocumentNode>> diversityRetrieval(List<DocumentNode> candidates, 
                                                              String query, int topK, double diversityWeight) {
        logger.debug("多样性检索，候选数量: {}, topK: {}, 多样性权重: {}", candidates.size(), topK, diversityWeight);

        List<Double> queryVector = embeddingService.embedText(query);
        List<ScoredResult<DocumentNode>> selected = new ArrayList<>();
        List<DocumentNode> remaining = new ArrayList<>(candidates);

        while (selected.size() < topK && !remaining.isEmpty()) {
            DocumentNode bestDoc = null;
            double bestScore = -1.0;

            for (DocumentNode doc : remaining) {
                if (doc.getEmbedding() == null) {
                    continue;
                }

                // 计算与查询的相似度
                double relevanceScore = embeddingService.cosineSimilarity(queryVector, doc.getEmbedding());

                // 计算与已选择文档的最大相似度（多样性惩罚）
                double maxSimilarityToSelected = 0.0;
                for (ScoredResult<DocumentNode> selectedResult : selected) {
                    double similarity = embeddingService.cosineSimilarity(
                            doc.getEmbedding(), selectedResult.getItem().getEmbedding());
                    maxSimilarityToSelected = Math.max(maxSimilarityToSelected, similarity);
                }

                // 综合评分：相关性 - 多样性惩罚
                double finalScore = relevanceScore - diversityWeight * maxSimilarityToSelected;

                if (finalScore > bestScore) {
                    bestScore = finalScore;
                    bestDoc = doc;
                }
            }

            if (bestDoc != null) {
                selected.add(new ScoredResult<>(bestDoc, bestScore));
                remaining.remove(bestDoc);
            } else {
                break;
            }
        }

        return selected;
    }

    /**
     * 层次化检索
     */
    public List<ScoredResult<DocumentNode>> hierarchicalRetrieval(List<DocumentNode> candidates, 
                                                                 String query, List<String> hierarchyLevels) {
        logger.debug("层次化检索，候选数量: {}, 层次级别: {}", candidates.size(), hierarchyLevels);

        List<Double> queryVector = embeddingService.embedText(query);
        Map<String, List<DocumentNode>> levelGroups = new HashMap<>();

        // 按层次级别分组
        for (DocumentNode doc : candidates) {
            String level = extractHierarchyLevel(doc, hierarchyLevels);
            levelGroups.computeIfAbsent(level, k -> new ArrayList<>()).add(doc);
        }

        List<ScoredResult<DocumentNode>> results = new ArrayList<>();

        // 为每个层次级别计算相似度
        for (String level : hierarchyLevels) {
            List<DocumentNode> levelDocs = levelGroups.getOrDefault(level, List.of());
            
            for (DocumentNode doc : levelDocs) {
                if (doc.getEmbedding() == null) {
                    continue;
                }

                double similarity = embeddingService.cosineSimilarity(queryVector, doc.getEmbedding());
                
                // 根据层次级别调整权重
                double levelWeight = getLevelWeight(level, hierarchyLevels);
                double adjustedScore = similarity * levelWeight;
                
                results.add(new ScoredResult<>(doc, adjustedScore));
            }
        }

        return results.stream()
                .sorted(Comparator.comparing(ScoredResult<DocumentNode>::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 提取文档的层次级别
     */
    private String extractHierarchyLevel(DocumentNode doc, List<String> hierarchyLevels) {
        // 简单的层次级别提取逻辑，基于文档来源或元数据
        String source = doc.getSource();
        if (source != null) {
            for (String level : hierarchyLevels) {
                if (source.toLowerCase().contains(level.toLowerCase())) {
                    return level;
                }
            }
        }
        return hierarchyLevels.isEmpty() ? "default" : hierarchyLevels.get(0);
    }

    /**
     * 获取层次级别权重
     */
    private double getLevelWeight(String level, List<String> hierarchyLevels) {
        int index = hierarchyLevels.indexOf(level);
        if (index == -1) {
            return 1.0;
        }
        // 越靠前的层次级别权重越高
        return 1.0 - (index * 0.1);
    }

    /**
     * 自适应阈值检索
     */
    public List<ScoredResult<DocumentNode>> adaptiveThresholdRetrieval(List<DocumentNode> candidates, 
                                                                      String query, double baseThreshold) {
        logger.debug("自适应阈值检索，候选数量: {}, 基础阈值: {}", candidates.size(), baseThreshold);

        List<Double> queryVector = embeddingService.embedText(query);
        List<ScoredResult<DocumentNode>> allResults = new ArrayList<>();

        // 计算所有相似度
        for (DocumentNode doc : candidates) {
            if (doc.getEmbedding() == null) {
                continue;
            }

            double similarity = embeddingService.cosineSimilarity(queryVector, doc.getEmbedding());
            allResults.add(new ScoredResult<>(doc, similarity));
        }

        // 排序
        allResults.sort(Comparator.comparing(ScoredResult<DocumentNode>::getScore).reversed());

        if (allResults.isEmpty()) {
            return allResults;
        }

        // 计算自适应阈值
        double maxScore = allResults.get(0).getScore();
        double adaptiveThreshold = Math.max(baseThreshold, maxScore * 0.8);

        // 过滤结果
        return allResults.stream()
                .filter(result -> result.getScore() >= adaptiveThreshold)
                .collect(Collectors.toList());
    }

    /**
     * 评分结果类
     */
    public static class ScoredResult<T> {
        private final T item;
        private final double score;

        public ScoredResult(T item, double score) {
            this.item = item;
            this.score = score;
        }

        public T getItem() { return item; }
        public double getScore() { return score; }

        @Override
        public String toString() {
            return String.format("ScoredResult{item=%s, score=%.4f}", item, score);
        }
    }
}

