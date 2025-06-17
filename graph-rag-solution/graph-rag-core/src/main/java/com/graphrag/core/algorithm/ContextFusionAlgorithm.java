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
 * Context Fusion Algorithm
 */
@Component
public class ContextFusionAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(ContextFusionAlgorithm.class);

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * Multi-source context fusion
     */
    public FusedContext fuseMultiSourceContext(List<DocumentNode> documents, 
                                              List<EntityNode> entities, 
                                              List<Map<String, Object>> graphRelations,
                                              String query) {
        logger.debug("Fusing multi-source context, documents: {}, entities: {}, relations: {}", 
                documents.size(), entities.size(), graphRelations.size());

        // 1. Process document context
        List<ContextSegment> documentSegments = processDocumentContext(documents, query);

        // 2. Process entity context
        List<ContextSegment> entitySegments = processEntityContext(entities, query);

        // 3. Process relation context
        List<ContextSegment> relationSegments = processRelationContext(graphRelations, query);

        // 4. Deduplicate and rank context
        List<ContextSegment> allSegments = new ArrayList<>();
        allSegments.addAll(documentSegments);
        allSegments.addAll(entitySegments);
        allSegments.addAll(relationSegments);

        List<ContextSegment> deduplicatedSegments = deduplicateSegments(allSegments);
        List<ContextSegment> rankedSegments = rankSegments(deduplicatedSegments, query);

        // 5. Build fused context
        return buildFusedContext(rankedSegments, query);
    }

    /**
     * Process document context
     */
    private List<ContextSegment> processDocumentContext(List<DocumentNode> documents, String query) {
        List<ContextSegment> segments = new ArrayList<>();
        List<Double> queryVector = embeddingService.embedText(query);

        for (DocumentNode doc : documents) {
            if (doc.getEmbedding() == null) {
                continue;
            }

            double relevanceScore = embeddingService.cosineSimilarity(queryVector, doc.getEmbedding());
            
            // Extract key paragraphs
            List<String> keyParagraphs = extractKeyParagraphs(doc.getContent(), query, 3);
            
            for (String paragraph : keyParagraphs) {
                ContextSegment segment = new ContextSegment(
                        paragraph,
                        "document",
                        relevanceScore,
                        Map.of(
                                "documentId", doc.getId(),
                                "title", doc.getTitle(),
                                "source", doc.getSource()
                        )
                );
                segments.add(segment);
            }
        }

        return segments;
    }

    /**
     * Process entity context
     */
    private List<ContextSegment> processEntityContext(List<EntityNode> entities, String query) {
        List<ContextSegment> segments = new ArrayList<>();
        List<Double> queryVector = embeddingService.embedText(query);

        for (EntityNode entity : entities) {
            if (entity.getEmbedding() == null) {
                continue;
            }

            double relevanceScore = embeddingService.cosineSimilarity(queryVector, entity.getEmbedding());
            
            String contextText = buildEntityContextText(entity);
            
            ContextSegment segment = new ContextSegment(
                    contextText,
                    "entity",
                    relevanceScore,
                    Map.of(
                            "entityId", entity.getId(),
                            "name", entity.getName(),
                            "type", entity.getType()
                    )
            );
            segments.add(segment);
        }

        return segments;
    }

    /**
     * Process relation context
     */
    private List<ContextSegment> processRelationContext(List<Map<String, Object>> relations, String query) {
        List<ContextSegment> segments = new ArrayList<>();

        for (Map<String, Object> relation : relations) {
            String contextText = buildRelationContextText(relation);
            
            // Simple keyword matching score
            double relevanceScore = calculateKeywordRelevance(contextText, query);
            
            ContextSegment segment = new ContextSegment(
                    contextText,
                    "relation",
                    relevanceScore,
                    relation
            );
            segments.add(segment);
        }

        return segments;
    }

    /**
     * Extract key paragraphs
     */
    private List<String> extractKeyParagraphs(String content, String query, int maxParagraphs) {
        String[] paragraphs = content.split("\n\n");
        List<String> queryKeywords = Arrays.asList(query.toLowerCase().split("\\s+"));

        return Arrays.stream(paragraphs)
                .filter(p -> p.trim().length() > 50) // Filter too short paragraphs
                .sorted((p1, p2) -> {
                    int score1 = calculateParagraphScore(p1, queryKeywords);
                    int score2 = calculateParagraphScore(p2, queryKeywords);
                    return Integer.compare(score2, score1);
                })
                .limit(maxParagraphs)
                .collect(Collectors.toList());
    }

    /**
     * Calculate paragraph score
     */
    private int calculateParagraphScore(String paragraph, List<String> keywords) {
        String lowerParagraph = paragraph.toLowerCase();
        return (int) keywords.stream()
                .mapToLong(keyword -> lowerParagraph.split(keyword, -1).length - 1)
                .sum();
    }

    /**
     * Build entity context text
     */
    private String buildEntityContextText(EntityNode entity) {
        StringBuilder text = new StringBuilder();
        text.append(entity.getName()).append(" (").append(entity.getType()).append(")");
        
        if (entity.getDescription() != null && !entity.getDescription().trim().isEmpty()) {
            text.append(": ").append(entity.getDescription());
        }
        
        return text.toString();
    }

    /**
     * Build relation context text
     */
    private String buildRelationContextText(Map<String, Object> relation) {
        String entity1 = (String) relation.get("entity1");
        String entity2 = (String) relation.get("entity2");
        String relationshipType = (String) relation.get("relationship");
        String description = (String) relation.get("description");

        StringBuilder text = new StringBuilder();
        text.append(entity1).append(" ").append(relationshipType).append(" ").append(entity2);
        
        if (description != null && !description.trim().isEmpty()) {
            text.append(" (").append(description).append(")");
        }
        
        return text.toString();
    }

    /**
     * Calculate keyword relevance
     */
    private double calculateKeywordRelevance(String text, String query) {
        String[] queryWords = query.toLowerCase().split("\\s+");
        String lowerText = text.toLowerCase();
        
        long matchCount = Arrays.stream(queryWords)
                .mapToLong(word -> lowerText.split(word, -1).length - 1)
                .sum();
        
        return Math.min(1.0, matchCount / (double) queryWords.length);
    }

    /**
     * Deduplicate segments
     */
    private List<ContextSegment> deduplicateSegments(List<ContextSegment> segments) {
        Map<String, ContextSegment> uniqueSegments = new LinkedHashMap<>();
        
        for (ContextSegment segment : segments) {
            String key = segment.getContent().trim().toLowerCase();
            
            // Keep segment with higher score if similar content exists
            if (uniqueSegments.containsKey(key)) {
                ContextSegment existing = uniqueSegments.get(key);
                if (segment.getRelevanceScore() > existing.getRelevanceScore()) {
                    uniqueSegments.put(key, segment);
                }
            } else {
                uniqueSegments.put(key, segment);
            }
        }
        
        return new ArrayList<>(uniqueSegments.values());
    }

    /**
     * Rank segments
     */
    private List<ContextSegment> rankSegments(List<ContextSegment> segments, String query) {
        return segments.stream()
                .sorted(Comparator.comparing(ContextSegment::getRelevanceScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Build fused context
     */
    private FusedContext buildFusedContext(List<ContextSegment> segments, String query) {
        // Group by type
        Map<String, List<ContextSegment>> segmentsByType = segments.stream()
                .collect(Collectors.groupingBy(ContextSegment::getType));

        // Build structured context
        StringBuilder contextText = new StringBuilder();
        
        // Add document context
        List<ContextSegment> documentSegments = segmentsByType.getOrDefault("document", List.of());
        if (!documentSegments.isEmpty()) {
            contextText.append("Related Document Content:\n");
            for (ContextSegment segment : documentSegments.stream().limit(5).collect(Collectors.toList())) {
                contextText.append("- ").append(segment.getContent()).append("\n");
            }
            contextText.append("\n");
        }

        // Add entity context
        List<ContextSegment> entitySegments = segmentsByType.getOrDefault("entity", List.of());
        if (!entitySegments.isEmpty()) {
            contextText.append("Related Entities:\n");
            for (ContextSegment segment : entitySegments.stream().limit(10).collect(Collectors.toList())) {
                contextText.append("- ").append(segment.getContent()).append("\n");
            }
            contextText.append("\n");
        }

        // Add relation context
        List<ContextSegment> relationSegments = segmentsByType.getOrDefault("relation", List.of());
        if (!relationSegments.isEmpty()) {
            contextText.append("Related Relations:\n");
            for (ContextSegment segment : relationSegments.stream().limit(15).collect(Collectors.toList())) {
                contextText.append("- ").append(segment.getContent()).append("\n");
            }
        }

        return new FusedContext(
                contextText.toString(),
                segments,
                calculateOverallRelevance(segments),
                segmentsByType
        );
    }

    /**
     * Calculate overall relevance
     */
    private double calculateOverallRelevance(List<ContextSegment> segments) {
        if (segments.isEmpty()) {
            return 0.0;
        }

        return segments.stream()
                .mapToDouble(ContextSegment::getRelevanceScore)
                .average()
                .orElse(0.0);
    }

    /**
     * Context segment class
     */
    public static class ContextSegment {
        private final String content;
        private final String type;
        private final double relevanceScore;
        private final Map<String, Object> metadata;

        public ContextSegment(String content, String type, double relevanceScore, Map<String, Object> metadata) {
            this.content = content;
            this.type = type;
            this.relevanceScore = relevanceScore;
            this.metadata = metadata;
        }

        // Getters
        public String getContent() { return content; }
        public String getType() { return type; }
        public double getRelevanceScore() { return relevanceScore; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    /**
     * Fused context class
     */
    public static class FusedContext {
        private final String contextText;
        private final List<ContextSegment> segments;
        private final double overallRelevance;
        private final Map<String, List<ContextSegment>> segmentsByType;

        public FusedContext(String contextText, List<ContextSegment> segments, 
                           double overallRelevance, Map<String, List<ContextSegment>> segmentsByType) {
            this.contextText = contextText;
            this.segments = segments;
            this.overallRelevance = overallRelevance;
            this.segmentsByType = segmentsByType;
        }

        // Getters
        public String getContextText() { return contextText; }
        public List<ContextSegment> getSegments() { return segments; }
        public double getOverallRelevance() { return overallRelevance; }
        public Map<String, List<ContextSegment>> getSegmentsByType() { return segmentsByType; }
    }
}
