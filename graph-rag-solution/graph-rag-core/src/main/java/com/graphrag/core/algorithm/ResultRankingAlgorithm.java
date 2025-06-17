package com.graphrag.core.algorithm;

import com.graphrag.core.algorithm.VectorRetrievalAlgorithm.ScoredResult;
import com.graphrag.data.entity.DocumentNode;
import com.graphrag.data.entity.EntityNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Result ranking and filtering algorithm
 */
@Component
public class ResultRankingAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(ResultRankingAlgorithm.class);

    /**
     * Multi-factor ranking
     */
    public <T> List<ScoredResult<T>> multiFactorRanking(List<ScoredResult<T>> results, 
                                                       Map<String, Double> factorWeights) {
        logger.debug("Multi-factor ranking, result count: {}, factor count: {}", results.size(), factorWeights.size());

        return results.stream()
                .map(result -> {
                    double adjustedScore = calculateAdjustedScore(result, factorWeights);
                    return new ScoredResult<>(result.getItem(), adjustedScore);
                })
                .sorted(Comparator.comparing(ScoredResult<T>::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Calculate adjusted score
     */
    private <T> double calculateAdjustedScore(ScoredResult<T> result, Map<String, Double> factorWeights) {
        double baseScore = result.getScore();
        double adjustedScore = baseScore;

        // Adjust score based on different factors
        for (Map.Entry<String, Double> factor : factorWeights.entrySet()) {
            String factorName = factor.getKey();
            double weight = factor.getValue();

            switch (factorName) {
                case "recency":
                    adjustedScore += weight * calculateRecencyScore(result.getItem());
                    break;
                case "authority":
                    adjustedScore += weight * calculateAuthorityScore(result.getItem());
                    break;
                case "completeness":
                    adjustedScore += weight * calculateCompletenessScore(result.getItem());
                    break;
                case "popularity":
                    adjustedScore += weight * calculatePopularityScore(result.getItem());
                    break;
            }
        }

        return Math.max(0.0, Math.min(1.0, adjustedScore));
    }

    /**
     * Calculate recency score
     */
    private <T> double calculateRecencyScore(T item) {
        // Simplified recency scoring logic
        if (item instanceof DocumentNode) {
            DocumentNode doc = (DocumentNode) item;
            if (doc.getCreatedAt() != null) {
                long daysSinceCreation = java.time.Duration.between(
                        doc.getCreatedAt(), java.time.LocalDateTime.now()).toDays();
                return Math.max(0.0, 1.0 - (daysSinceCreation / 365.0)); // Higher score for documents within a year
            }
        }
        return 0.5; // Default medium score
    }

    /**
     * Calculate authority score
     */
    private <T> double calculateAuthorityScore(T item) {
        // Simplified authority scoring logic
        if (item instanceof DocumentNode) {
            DocumentNode doc = (DocumentNode) item;
            String source = doc.getSource();
            if (source != null) {
                // Judge authority based on source
                if (source.contains("official") || source.contains("authoritative")) {
                    return 0.9;
                } else if (source.contains("academic") || source.contains("research")) {
                    return 0.8;
                } else if (source.contains("news") || source.contains("media")) {
                    return 0.6;
                }
            }
        }
        return 0.5;
    }

    /**
     * Calculate completeness score
     */
    private <T> double calculateCompletenessScore(T item) {
        if (item instanceof DocumentNode) {
            DocumentNode doc = (DocumentNode) item;
            int contentLength = doc.getContent() != null ? doc.getContent().length() : 0;
            // Longer content means higher completeness score (with upper limit)
            return Math.min(1.0, contentLength / 5000.0);
        } else if (item instanceof EntityNode) {
            EntityNode entity = (EntityNode) item;
            boolean hasDescription = entity.getDescription() != null && !entity.getDescription().trim().isEmpty();
            boolean hasEmbedding = entity.getEmbedding() != null;
            return (hasDescription ? 0.5 : 0.0) + (hasEmbedding ? 0.5 : 0.0);
        }
        return 0.5;
    }

    /**
     * Calculate popularity score
     */
    private <T> double calculatePopularityScore(T item) {
        // Simplified popularity scoring logic
        // In practice, can be based on view count, citation count, etc.
        return 0.5; // Default medium score
    }

    /**
     * Filter by query relevance
     */
    public <T> List<ScoredResult<T>> filterByRelevance(List<ScoredResult<T>> results, 
                                                      double minRelevanceThreshold) {
        logger.debug("Filtering by relevance, threshold: {}", minRelevanceThreshold);

        return results.stream()
                .filter(result -> result.getScore() >= minRelevanceThreshold)
                .collect(Collectors.toList());
    }

    /**
     * Diversity filtering
     */
    public List<ScoredResult<DocumentNode>> diversityFilter(List<ScoredResult<DocumentNode>> results, 
                                                           double diversityThreshold, int maxResults) {
        logger.debug("Diversity filtering, threshold: {}, max results: {}", diversityThreshold, maxResults);

        List<ScoredResult<DocumentNode>> filtered = new ArrayList<>();
        
        for (ScoredResult<DocumentNode> candidate : results) {
            if (filtered.size() >= maxResults) {
                break;
            }

            boolean isDiverse = true;
            for (ScoredResult<DocumentNode> selected : filtered) {
                double similarity = calculateContentSimilarity(
                        candidate.getItem(), selected.getItem());
                if (similarity > diversityThreshold) {
                    isDiverse = false;
                    break;
                }
            }

            if (isDiverse) {
                filtered.add(candidate);
            }
        }

        return filtered;
    }

    /**
     * Calculate content similarity
     */
    private double calculateContentSimilarity(DocumentNode doc1, DocumentNode doc2) {
        // Simplified content similarity calculation
        if (doc1.getEmbedding() != null && doc2.getEmbedding() != null) {
            return cosineSimilarity(doc1.getEmbedding(), doc2.getEmbedding());
        }
        
        // Simple similarity based on title and source
        String title1 = doc1.getTitle() != null ? doc1.getTitle().toLowerCase() : "";
        String title2 = doc2.getTitle() != null ? doc2.getTitle().toLowerCase() : "";
        String source1 = doc1.getSource() != null ? doc1.getSource().toLowerCase() : "";
        String source2 = doc2.getSource() != null ? doc2.getSource().toLowerCase() : "";

        double titleSimilarity = calculateStringSimilarity(title1, title2);
        double sourceSimilarity = source1.equals(source2) ? 1.0 : 0.0;

        return 0.7 * titleSimilarity + 0.3 * sourceSimilarity;
    }

    /**
     * Calculate string similarity
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Calculate cosine similarity
     */
    private double cosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += Math.pow(vector1.get(i), 2);
            norm2 += Math.pow(vector2.get(i), 2);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Time window filtering
     */
    public List<ScoredResult<DocumentNode>> timeWindowFilter(List<ScoredResult<DocumentNode>> results, 
                                                            java.time.LocalDateTime startTime, 
                                                            java.time.LocalDateTime endTime) {
        logger.debug("Time window filtering, start time: {}, end time: {}", startTime, endTime);

        return results.stream()
                .filter(result -> {
                    DocumentNode doc = result.getItem();
                    if (doc.getCreatedAt() == null) {
                        return true; // Keep documents without time information
                    }
                    return !doc.getCreatedAt().isBefore(startTime) && 
                           !doc.getCreatedAt().isAfter(endTime);
                })
                .collect(Collectors.toList());
    }

    /**
     * Source filtering
     */
    public List<ScoredResult<DocumentNode>> sourceFilter(List<ScoredResult<DocumentNode>> results, 
                                                        Set<String> allowedSources, 
                                                        Set<String> blockedSources) {
        logger.debug("Source filtering, allowed sources: {}, blocked sources: {}", allowedSources.size(), blockedSources.size());

        return results.stream()
                .filter(result -> {
                    DocumentNode doc = result.getItem();
                    String source = doc.getSource();
                    
                    if (source == null) {
                        return allowedSources.isEmpty(); // Keep if no source restrictions
                    }
                    
                    // Check if in blocked list
                    if (blockedSources.contains(source)) {
                        return false;
                    }
                    
                    // Check if in allowed list (if restricted)
                    if (!allowedSources.isEmpty()) {
                        return allowedSources.contains(source);
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Comprehensive ranking and filtering
     */
    public List<ScoredResult<DocumentNode>> comprehensiveRankingAndFiltering(
            List<ScoredResult<DocumentNode>> results,
            RankingConfig config) {
        
        logger.debug("Comprehensive ranking and filtering, config: {}", config);

        List<ScoredResult<DocumentNode>> processed = new ArrayList<>(results);

        // 1. Relevance filtering
        if (config.getMinRelevanceThreshold() > 0) {
            processed = filterByRelevance(processed, config.getMinRelevanceThreshold());
        }

        // 2. Time window filtering
        if (config.getStartTime() != null && config.getEndTime() != null) {
            processed = timeWindowFilter(processed, config.getStartTime(), config.getEndTime());
        }

        // 3. Source filtering
        if (!config.getAllowedSources().isEmpty() || !config.getBlockedSources().isEmpty()) {
            processed = sourceFilter(processed, config.getAllowedSources(), config.getBlockedSources());
        }

        // 4. Multi-factor ranking
        if (!config.getFactorWeights().isEmpty()) {
            processed = multiFactorRanking(processed, config.getFactorWeights());
        }

        // 5. Diversity filtering
        if (config.getDiversityThreshold() > 0) {
            processed = diversityFilter(processed, config.getDiversityThreshold(), config.getMaxResults());
        } else if (config.getMaxResults() > 0) {
            processed = processed.stream().limit(config.getMaxResults()).collect(Collectors.toList());
        }

        logger.info("Ranking and filtering completed, final result count: {}", processed.size());
        return processed;
    }

    /**
     * Ranking configuration class
     */
    public static class RankingConfig {
        private double minRelevanceThreshold = 0.0;
        private java.time.LocalDateTime startTime;
        private java.time.LocalDateTime endTime;
        private Set<String> allowedSources = new HashSet<>();
        private Set<String> blockedSources = new HashSet<>();
        private Map<String, Double> factorWeights = new HashMap<>();
        private double diversityThreshold = 0.0;
        private int maxResults = 0;

        // Getters and Setters
        public double getMinRelevanceThreshold() { return minRelevanceThreshold; }
        public void setMinRelevanceThreshold(double minRelevanceThreshold) { this.minRelevanceThreshold = minRelevanceThreshold; }

        public java.time.LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(java.time.LocalDateTime startTime) { this.startTime = startTime; }

        public java.time.LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(java.time.LocalDateTime endTime) { this.endTime = endTime; }

        public Set<String> getAllowedSources() { return allowedSources; }
        public void setAllowedSources(Set<String> allowedSources) { this.allowedSources = allowedSources; }

        public Set<String> getBlockedSources() { return blockedSources; }
        public void setBlockedSources(Set<String> blockedSources) { this.blockedSources = blockedSources; }

        public Map<String, Double> getFactorWeights() { return factorWeights; }
        public void setFactorWeights(Map<String, Double> factorWeights) { this.factorWeights = factorWeights; }

        public double getDiversityThreshold() { return diversityThreshold; }
        public void setDiversityThreshold(double diversityThreshold) { this.diversityThreshold = diversityThreshold; }

        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }

        @Override
        public String toString() {
            return String.format("RankingConfig{relevanceThreshold=%.2f, maxResults=%d, diversityThreshold=%.2f}", 
                    minRelevanceThreshold, maxResults, diversityThreshold);
        }
    }
}
