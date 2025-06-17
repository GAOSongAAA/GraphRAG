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
 * Vector retrieval optimization algorithm
 */
@Component
public class VectorRetrievalAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(VectorRetrievalAlgorithm.class);

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * Multi-query vector retrieval
     */
    public List<ScoredResult<DocumentNode>> multiQueryVectorRetrieval(
            List<String> queries, List<DocumentNode> candidates, int topK) {
        
        logger.debug("Multi-query vector retrieval, query count: {}, candidate count: {}", queries.size(), candidates.size());

        // Generate query vectors
        List<List<Double>> queryVectors = embeddingService.embedTexts(queries);

        // Calculate similarity between each candidate document and all queries
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

        // Sort and return top K results
        return documentScores.entrySet().stream()
                .sorted(Map.Entry.<DocumentNode, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> new ScoredResult<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Reranking algorithm
     */
    public List<ScoredResult<DocumentNode>> rerank(List<DocumentNode> candidates, String query, 
                                                  List<String> contextQueries) {
        logger.debug("Reranking, candidate count: {}, context query count: {}", candidates.size(), contextQueries.size());

        List<Double> queryVector = embeddingService.embedText(query);
        List<List<Double>> contextVectors = embeddingService.embedTexts(contextQueries);

        List<ScoredResult<DocumentNode>> results = new ArrayList<>();

        for (DocumentNode doc : candidates) {
            if (doc.getEmbedding() == null) {
                continue;
            }

            // Calculate similarity with main query
            double mainSimilarity = embeddingService.cosineSimilarity(queryVector, doc.getEmbedding());

            // Calculate average similarity with context queries
            double contextSimilarity = 0.0;
            if (!contextVectors.isEmpty()) {
                for (List<Double> contextVector : contextVectors) {
                    contextSimilarity += embeddingService.cosineSimilarity(contextVector, doc.getEmbedding());
                }
                contextSimilarity /= contextVectors.size();
            }

            // Final score (main query weight 0.7, context weight 0.3)
            double finalScore = 0.7 * mainSimilarity + 0.3 * contextSimilarity;
            results.add(new ScoredResult<>(doc, finalScore));
        }

        return results.stream()
                .sorted(Comparator.comparing(ScoredResult<DocumentNode>::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Diversity-aware retrieval
     */
    public List<ScoredResult<DocumentNode>> diversityRetrieval(List<DocumentNode> candidates, 
                                                              String query, int topK, double diversityWeight) {
        logger.debug("Diversity retrieval, candidate count: {}, topK: {}, diversity weight: {}", 
                    candidates.size(), topK, diversityWeight);

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

                // Calculate similarity with query
                double relevanceScore = embeddingService.cosineSimilarity(queryVector, doc.getEmbedding());

                // Calculate maximum similarity with selected documents (diversity penalty)
                double maxSimilarityToSelected = 0.0;
                for (ScoredResult<DocumentNode> selectedResult : selected) {
                    double similarity = embeddingService.cosineSimilarity(
                            doc.getEmbedding(), selectedResult.getItem().getEmbedding());
                    maxSimilarityToSelected = Math.max(maxSimilarityToSelected, similarity);
                }

                // Final score: relevance - diversity penalty
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
     * Hierarchical retrieval
     */
    public List<ScoredResult<DocumentNode>> hierarchicalRetrieval(List<DocumentNode> candidates, 
                                                                 String query, List<String> hierarchyLevels) {
        logger.debug("Hierarchical retrieval, candidate count: {}, hierarchy levels: {}", 
                    candidates.size(), hierarchyLevels);

        List<Double> queryVector = embeddingService.embedText(query);
        Map<String, List<DocumentNode>> levelGroups = new HashMap<>();

        // Group by hierarchy level
        for (DocumentNode doc : candidates) {
            String level = extractHierarchyLevel(doc, hierarchyLevels);
            levelGroups.computeIfAbsent(level, k -> new ArrayList<>()).add(doc);
        }

        List<ScoredResult<DocumentNode>> results = new ArrayList<>();

        // Calculate similarities for each hierarchy level
        for (String level : hierarchyLevels) {
            List<DocumentNode> levelDocs = levelGroups.getOrDefault(level, List.of());
            
            for (DocumentNode doc : levelDocs) {
                if (doc.getEmbedding() == null) {
                    continue;
                }

                double similarity = embeddingService.cosineSimilarity(queryVector, doc.getEmbedding());
                
                // Adjust weight based on hierarchy level
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
     * Extract document hierarchy level
     */
    private String extractHierarchyLevel(DocumentNode doc, List<String> hierarchyLevels) {
        // Simple hierarchy level extraction based on document source or metadata
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
     * Get hierarchy level weight
     */
    private double getLevelWeight(String level, List<String> hierarchyLevels) {
        int index = hierarchyLevels.indexOf(level);
        if (index == -1) {
            return 1.0;
        }
        // Higher weights for earlier hierarchy levels
        return 1.0 - (index * 0.1);
    }

    /**
     * Adaptive threshold retrieval
     */
    public List<ScoredResult<DocumentNode>> adaptiveThresholdRetrieval(List<DocumentNode> candidates, 
                                                                      String query, double baseThreshold) {
        logger.debug("Adaptive threshold retrieval, candidate count: {}, base threshold: {}", 
                    candidates.size(), baseThreshold);

        List<Double> queryVector = embeddingService.embedText(query);
        List<ScoredResult<DocumentNode>> allResults = new ArrayList<>();

        // Calculate all similarities
        for (DocumentNode doc : candidates) {
            if (doc.getEmbedding() == null) {
                continue;
            }

            double similarity = embeddingService.cosineSimilarity(queryVector, doc.getEmbedding());
            allResults.add(new ScoredResult<>(doc, similarity));
        }

        // Sort results
        allResults.sort(Comparator.comparing(ScoredResult<DocumentNode>::getScore).reversed());

        if (allResults.isEmpty()) {
            return allResults;
        }

        // Calculate adaptive threshold
        double maxScore = allResults.get(0).getScore();
        double adaptiveThreshold = Math.max(baseThreshold, maxScore * 0.8);

        // Filter results
        return allResults.stream()
                .filter(result -> result.getScore() >= adaptiveThreshold)
                .collect(Collectors.toList());
    }

    /**
     * Scored result class
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
