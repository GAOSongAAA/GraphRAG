package com.graphrag.core.algorithm;

import com.graphrag.core.service.EmbeddingService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Query understanding and intent recognition algorithm
 */
@Component
public class QueryUnderstandingAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(QueryUnderstandingAlgorithm.class);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private EmbeddingService embeddingService;

    // Query analysis prompt template
    private static final PromptTemplate QUERY_ANALYSIS_TEMPLATE = PromptTemplate.from("""
            Please analyze the following user query and extract key information:
            
            Query: {{query}}
            
            Please output in the following format:
            
            Query Type: [Factual Query/Concept Explanation/Comparative Analysis/Reasoning Q&A/List Query/Others]
            Key Entities: [Entity1, Entity2, ...]
            Query Intent: [Brief description of user's query intent]
            Related Concepts: [Concept1, Concept2, ...]
            Query Complexity: [Simple/Medium/Complex]
            Expected Answer Type: [Brief Answer/Detailed Explanation/List/Comparison Table/Others]
            
            Note: Please ensure the extracted information is accurate and useful.
            """);

    /**
     * Analyze query
     */
    public QueryAnalysis analyzeQuery(String query) {
        logger.debug("Start analyzing query: {}", query);

        try {
            // 1. Use LLM for query analysis
            Prompt prompt = QUERY_ANALYSIS_TEMPLATE.apply(Map.of("query", query));
            String response = chatLanguageModel.generate(prompt.text());

            // 2. Parse LLM response
            QueryAnalysis analysis = parseQueryAnalysis(response, query);

            // 3. Enhance analysis information
            enhanceAnalysis(analysis, query);

            logger.info("Query analysis completed, type: {}, complexity: {}", analysis.getQueryType(), analysis.getComplexity());
            return analysis;

        } catch (Exception e) {
            logger.error("Query analysis failed", e);
            return createFallbackAnalysis(query);
        }
    }

    /**
     * Parse query analysis response
     */
    private QueryAnalysis parseQueryAnalysis(String response, String originalQuery) {
        QueryAnalysis analysis = new QueryAnalysis(originalQuery);

        // Extract query type
        String queryType = extractField(response, "Query Type");
        analysis.setQueryType(queryType != null ? queryType : "Others");

        // Extract key entities
        String entitiesStr = extractField(response, "Key Entities");
        if (entitiesStr != null) {
            List<String> entities = parseList(entitiesStr);
            analysis.setKeyEntities(entities);
        }

        // Extract query intent
        String intent = extractField(response, "Query Intent");
        analysis.setIntent(intent != null ? intent : "Unknown Intent");

        // Extract related concepts
        String conceptsStr = extractField(response, "Related Concepts");
        if (conceptsStr != null) {
            List<String> concepts = parseList(conceptsStr);
            analysis.setRelatedConcepts(concepts);
        }

        // Extract query complexity
        String complexity = extractField(response, "Query Complexity");
        analysis.setComplexity(complexity != null ? complexity : "Medium");

        // Extract expected answer type
        String answerType = extractField(response, "Expected Answer Type");
        analysis.setExpectedAnswerType(answerType != null ? answerType : "Detailed Explanation");

        return analysis;
    }

    /**
     * Extract field value
     */
    private String extractField(String text, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*\\[?([^\\]\\n]+)\\]?");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Parse list
     */
    private List<String> parseList(String listStr) {
        if (listStr == null || listStr.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(listStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Enhance analysis information
     */
    private void enhanceAnalysis(QueryAnalysis analysis, String query) {
        // 1. Detect query patterns
        analysis.setQueryPatterns(detectQueryPatterns(query));

        // 2. Extract temporal information
        analysis.setTemporalInfo(extractTemporalInfo(query));

        // 3. Detect comparative intent
        analysis.setComparative(detectComparative(query));

        // 4. Calculate query vector
        List<Double> queryVector = embeddingService.embedText(query);
        analysis.setQueryVector(queryVector);

        // 5. Generate expanded queries
        analysis.setExpandedQueries(generateExpandedQueries(analysis));
    }

    /**
     * Detect query patterns
     */
    private List<String> detectQueryPatterns(String query) {
        List<String> patterns = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        // Question word pattern
        if (lowerQuery.matches(".*\\b(what|what is|how|why|which|who|when|where)\\b.*")) {
            patterns.add("Question Word Query");
        }

        // Definition pattern
        if (lowerQuery.matches(".*\\b(is|define|meaning|concept)\\b.*")) {
            patterns.add("Definition Query");
        }

        // Comparison pattern
        if (lowerQuery.matches(".*\\b(compare|contrast|difference|similarity|different|same)\\b.*")) {
            patterns.add("Comparison Query");
        }

        // List pattern
        if (lowerQuery.matches(".*\\b(list|enumerate|what are|include|types)\\b.*")) {
            patterns.add("List Query");
        }

        // Causal pattern
        if (lowerQuery.matches(".*\\b(cause|lead to|impact|result|effect)\\b.*")) {
            patterns.add("Causal Query");
        }

        // Process pattern
        if (lowerQuery.matches(".*\\b(steps|process|procedure|method|how to)\\b.*")) {
            patterns.add("Process Query");
        }

        return patterns;
    }

    /**
     * Extract temporal information
     */
    private Map<String, String> extractTemporalInfo(String query) {
        Map<String, String> temporalInfo = new HashMap<>();

        // Detect time expressions
        Pattern timePattern = Pattern.compile("\\b(\\d{4})\\s*year|\\b(\\d{1,2})\\s*month|\\b(\\d{1,2})\\s*day|\\b(today|yesterday|tomorrow|recent|now|current|past|future)\\b");
        Matcher matcher = timePattern.matcher(query);

        while (matcher.find()) {
            String timeExpr = matcher.group();
            if (timeExpr.contains("year")) {
                temporalInfo.put("year", timeExpr.replace("year", "").trim());
            } else if (timeExpr.contains("month")) {
                temporalInfo.put("month", timeExpr.replace("month", "").trim());
            } else if (timeExpr.contains("day")) {
                temporalInfo.put("day", timeExpr.replace("day", "").trim());
            } else {
                temporalInfo.put("relative", timeExpr);
            }
        }

        return temporalInfo;
    }

    /**
     * Detect comparative intent
     */
    private boolean detectComparative(String query) {
        String lowerQuery = query.toLowerCase();
        String[] comparativeWords = {"compare", "contrast", "difference", "similarity", "same", "different", "pros and cons", "vs", "and", "with"};
        
        return Arrays.stream(comparativeWords)
                .anyMatch(lowerQuery::contains);
    }

    /**
     * Generate expanded queries
     */
    private List<String> generateExpandedQueries(QueryAnalysis analysis) {
        List<String> expandedQueries = new ArrayList<>();
        String originalQuery = analysis.getOriginalQuery();

        // Generate expanded queries based on key entities
        for (String entity : analysis.getKeyEntities()) {
            expandedQueries.add("definition of " + entity);
            expandedQueries.add("characteristics of " + entity);
            expandedQueries.add("applications of " + entity);
        }

        // Generate expanded queries based on related concepts
        for (String concept : analysis.getRelatedConcepts()) {
            expandedQueries.add("relationship between " + concept + " and " + String.join("", analysis.getKeyEntities()));
        }

        // Generate expanded queries based on query type
        switch (analysis.getQueryType()) {
            case "Concept Explanation":
                expandedQueries.add("detailed explanation of " + originalQuery);
                expandedQueries.add("examples of " + originalQuery);
                break;
            case "Comparative Analysis":
                expandedQueries.add("advantages and disadvantages of " + originalQuery);
                expandedQueries.add("similarities in " + originalQuery);
                break;
            case "Reasoning Q&A":
                expandedQueries.add("reasons for " + originalQuery);
                expandedQueries.add("impacts of " + originalQuery);
                break;
        }

        return expandedQueries.stream()
                .distinct()
                .limit(10)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Create fallback analysis
     */
    private QueryAnalysis createFallbackAnalysis(String query) {
        QueryAnalysis analysis = new QueryAnalysis(query);
        analysis.setQueryType("Others");
        analysis.setIntent("General Query");
        analysis.setComplexity("Medium");
        analysis.setExpectedAnswerType("Detailed Explanation");
        analysis.setKeyEntities(extractSimpleEntities(query));
        analysis.setQueryPatterns(List.of("General Query"));
        
        return analysis;
    }

    /**
     * Simple entity extraction
     */
    private List<String> extractSimpleEntities(String query) {
        // Simple entity extraction logic based on common patterns
        List<String> entities = new ArrayList<>();
        
        // Extract potential entities (capitalized word groups)
        Pattern entityPattern = Pattern.compile("\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b");
        Matcher matcher = entityPattern.matcher(query);
        
        while (matcher.find()) {
            entities.add(matcher.group());
        }
        
        return entities;
    }

    /**
     * Query analysis result class
     */
    public static class QueryAnalysis {
        private final String originalQuery;
        private String queryType;
        private List<String> keyEntities = new ArrayList<>();
        private String intent;
        private List<String> relatedConcepts = new ArrayList<>();
        private String complexity;
        private String expectedAnswerType;
        private List<String> queryPatterns = new ArrayList<>();
        private Map<String, String> temporalInfo = new HashMap<>();
        private boolean comparative;
        private List<Double> queryVector;
        private List<String> expandedQueries = new ArrayList<>();

        public QueryAnalysis(String originalQuery) {
            this.originalQuery = originalQuery;
        }

        // Getters and Setters
        public String getOriginalQuery() { return originalQuery; }
        
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        
        public List<String> getKeyEntities() { return keyEntities; }
        public void setKeyEntities(List<String> keyEntities) { this.keyEntities = keyEntities; }
        
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        
        public List<String> getRelatedConcepts() { return relatedConcepts; }
        public void setRelatedConcepts(List<String> relatedConcepts) { this.relatedConcepts = relatedConcepts; }
        
        public String getComplexity() { return complexity; }
        public void setComplexity(String complexity) { this.complexity = complexity; }
        
        public String getExpectedAnswerType() { return expectedAnswerType; }
        public void setExpectedAnswerType(String expectedAnswerType) { this.expectedAnswerType = expectedAnswerType; }
        
        public List<String> getQueryPatterns() { return queryPatterns; }
        public void setQueryPatterns(List<String> queryPatterns) { this.queryPatterns = queryPatterns; }
        
        public Map<String, String> getTemporalInfo() { return temporalInfo; }
        public void setTemporalInfo(Map<String, String> temporalInfo) { this.temporalInfo = temporalInfo; }
        
        public boolean isComparative() { return comparative; }
        public void setComparative(boolean comparative) { this.comparative = comparative; }
        
        public List<Double> getQueryVector() { return queryVector; }
        public void setQueryVector(List<Double> queryVector) { this.queryVector = queryVector; }
        
        public List<String> getExpandedQueries() { return expandedQueries; }
        public void setExpandedQueries(List<String> expandedQueries) { this.expandedQueries = expandedQueries; }

        @Override
        public String toString() {
            return String.format("QueryAnalysis{query='%s', type='%s', intent='%s', complexity='%s'}", 
                    originalQuery, queryType, intent, complexity);
        }
    }
}
