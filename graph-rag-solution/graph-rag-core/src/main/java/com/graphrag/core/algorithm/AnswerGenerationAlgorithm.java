package com.graphrag.core.algorithm;

import com.graphrag.core.algorithm.ContextFusionAlgorithm.FusedContext;
import com.graphrag.core.algorithm.QueryUnderstandingAlgorithm.QueryAnalysis;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Answer Generation Strategy Algorithm
 */
@Component
public class AnswerGenerationAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(AnswerGenerationAlgorithm.class);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    // Prompt templates for different query types
    private static final PromptTemplate FACTUAL_TEMPLATE = PromptTemplate.from("""
            Based on the following context, answer the user's factual question. Please provide accurate and concise answers.
            
            Context:
            {{context}}
            
            User Question: {{question}}
            
            Please answer directly, and if the context is insufficient, please clearly state so.
            
            Answer:
            """);

    private static final PromptTemplate CONCEPTUAL_TEMPLATE = PromptTemplate.from("""
            Based on the following context, explain the relevant concepts in detail. Please provide comprehensive and easy-to-understand explanations.
            
            Context:
            {{context}}
            
            User Question: {{question}}
            
            Please provide detailed concept explanations, including definition, characteristics, applications, etc.
            
            Answer:
            """);

    private static final PromptTemplate COMPARATIVE_TEMPLATE = PromptTemplate.from("""
            Based on the following context, conduct a comparative analysis. Please compare from multiple dimensions.
            
            Context:
            {{context}}
            
            User Question: {{question}}
            
            Please provide structured comparative analysis, including similarities, differences, pros and cons.
            
            Answer:
            """);

    private static final PromptTemplate REASONING_TEMPLATE = PromptTemplate.from("""
            Based on the following context, conduct reasoning analysis. Please provide clear logical reasoning process.
            
            Context:
            {{context}}
            
            User Question: {{question}}
            
            Please provide reasoning process and conclusions with rigorous logic.
            
            Answer:
            """);

    private static final PromptTemplate LIST_TEMPLATE = PromptTemplate.from("""
            Based on the following context, provide answer in list format.
            
            Context:
            {{context}}
            
            User Question: {{question}}
            
            Please organize the answer in a clear list format.
            
            Answer:
            """);

    /**
     * Generate answer
     */
    public String generateAnswer(String question, FusedContext context, QueryAnalysis queryAnalysis) {
        logger.debug("Start generating answer, query type: {}", queryAnalysis.getQueryType());

        try {
            // Select appropriate template based on query type
            PromptTemplate template = selectTemplate(queryAnalysis.getQueryType());
            
            // Build prompt
            Prompt prompt = template.apply(Map.of(
                    "question", question,
                    "context", context.getContextText()
            ));

            // Generate answer
            String answer = chatLanguageModel.generate(prompt.text());
            
            // Post-process answer
            answer = postProcessAnswer(answer, queryAnalysis);
            
            logger.info("Answer generation completed, length: {}", answer.length());
            return answer;

        } catch (Exception e) {
            logger.error("Answer generation failed", e);
            return generateFallbackAnswer(question, context);
        }
    }

    /**
     * Select prompt template
     */
    private PromptTemplate selectTemplate(String queryType) {
        switch (queryType) {
            case "factual":
                return FACTUAL_TEMPLATE;
            case "conceptual":
                return CONCEPTUAL_TEMPLATE;
            case "comparative":
                return COMPARATIVE_TEMPLATE;
            case "reasoning":
                return REASONING_TEMPLATE;
            case "list":
                return LIST_TEMPLATE;
            default:
                return CONCEPTUAL_TEMPLATE; // Default to conceptual template
        }
    }

    /**
     * Post-process answer
     */
    private String postProcessAnswer(String answer, QueryAnalysis queryAnalysis) {
        // 1. Clean formatting
        answer = answer.trim();
        
        // 2. Adjust format based on expected answer type
        switch (queryAnalysis.getExpectedAnswerType()) {
            case "short":
                answer = extractShortAnswer(answer);
                break;
            case "list":
                answer = formatAsList(answer);
                break;
            case "comparison":
                answer = formatAsComparison(answer);
                break;
        }

        // 3. Add confidence information (if relevance is low)
        if (queryAnalysis.getQueryVector() != null) {
            // Confidence assessment logic can be added here
        }

        return answer;
    }

    /**
     * Extract short answer
     */
    private String extractShortAnswer(String answer) {
        // Extract first sentence as short answer
        String[] sentences = answer.split("[.!?]");
        if (sentences.length > 0) {
            return sentences[0].trim() + ".";
        }
        return answer.length() > 100 ? answer.substring(0, 100) + "..." : answer;
    }

    /**
     * Format as list
     */
    private String formatAsList(String answer) {
        if (answer.contains("1.") || answer.contains("•") || answer.contains("-")) {
            return answer; // Already in list format
        }
        
        // Try to convert paragraphs to list
        String[] paragraphs = answer.split("\n\n");
        if (paragraphs.length > 1) {
            StringBuilder listAnswer = new StringBuilder();
            for (int i = 0; i < paragraphs.length; i++) {
                listAnswer.append((i + 1)).append(". ").append(paragraphs[i].trim()).append("\n");
            }
            return listAnswer.toString();
        }
        
        return answer;
    }

    /**
     * Format as comparison
     */
    private String formatAsComparison(String answer) {
        // Simple comparison formatting
        if (answer.contains("Similarities") || answer.contains("Differences") || answer.contains("Comparison")) {
            return answer; // Already in comparison format
        }
        
        return "Comparative Analysis:\n" + answer;
    }

    /**
     * Generate fallback answer
     */
    private String generateFallbackAnswer(String question, FusedContext context) {
        if (context.getContextText().trim().isEmpty()) {
            return "Sorry, I couldn't find enough relevant information to answer your question. Please try rephrasing your question or provide more context.";
        }
        
        return "Based on available information, I'll try to answer your question:\n\n" + 
               context.getContextText().substring(0, Math.min(500, context.getContextText().length())) + 
               "\n\nPlease note that this answer may be incomplete. It's recommended to consult additional sources.";
    }

    /**
     * Generate conversational answer
     */
    public String generateConversationalAnswer(String question, FusedContext context, 
                                             QueryAnalysis queryAnalysis, String conversationHistory) {
        logger.debug("Generating conversational answer");

        PromptTemplate conversationalTemplate = PromptTemplate.from("""
                Based on the following conversation history and context, answer the user's question. Please maintain conversation coherence.
                
                Conversation History:
                {{history}}
                
                Context:
                {{context}}
                
                Current Question: {{question}}
                
                Please provide a coherent and relevant answer.
                
                Answer:
                """);

        try {
            Prompt prompt = conversationalTemplate.apply(Map.of(
                    "question", question,
                    "context", context.getContextText(),
                    "history", conversationHistory != null ? conversationHistory : "None"
            ));

            return chatLanguageModel.generate(prompt.text());

        } catch (Exception e) {
            logger.error("Conversational answer generation failed", e);
            return generateAnswer(question, context, queryAnalysis);
        }
    }

    /**
     * Generate explanatory answer
     */
    public String generateExplanatoryAnswer(String question, FusedContext context, 
                                          QueryAnalysis queryAnalysis) {
        logger.debug("Generating explanatory answer");

        PromptTemplate explanatoryTemplate = PromptTemplate.from("""
                Please explain the following question in detail, providing comprehensive background information and in-depth analysis.
                
                Context:
                {{context}}
                
                Question: {{question}}
                
                Please provide:
                1. Basic concept explanation
                2. Related background information
                3. Detailed analysis
                4. Practical applications or examples
                
                Answer:
                """);

        try {
            Prompt prompt = explanatoryTemplate.apply(Map.of(
                    "question", question,
                    "context", context.getContextText()
            ));

            return chatLanguageModel.generate(prompt.text());

        } catch (Exception e) {
            logger.error("Explanatory answer generation failed", e);
            return generateAnswer(question, context, queryAnalysis);
        }
    }

    /**
     * Generate structured answer
     */
    public StructuredAnswer generateStructuredAnswer(String question, FusedContext context, 
                                                   QueryAnalysis queryAnalysis) {
        logger.debug("Generating structured answer");

        try {
            String mainAnswer = generateAnswer(question, context, queryAnalysis);
            
            StructuredAnswer structuredAnswer = new StructuredAnswer();
            structuredAnswer.setMainAnswer(mainAnswer);
            structuredAnswer.setConfidence(calculateConfidence(context, queryAnalysis));
            structuredAnswer.setSourceCount(context.getSegments().size());
            structuredAnswer.setAnswerType(queryAnalysis.getExpectedAnswerType());
            structuredAnswer.setKeyPoints(extractKeyPoints(mainAnswer));
            
            return structuredAnswer;

        } catch (Exception e) {
            logger.error("Structured answer generation failed", e);
            return createFallbackStructuredAnswer(question);
        }
    }

    /**
     * Calculate confidence
     */
    private double calculateConfidence(FusedContext context, QueryAnalysis queryAnalysis) {
        // Calculate confidence based on context relevance and query complexity
        double contextRelevance = context.getOverallRelevance();
        double complexityFactor = getComplexityFactor(queryAnalysis.getComplexity());
        
        return Math.min(1.0, contextRelevance * complexityFactor);
    }

    /**
     * Get complexity factor
     */
    private double getComplexityFactor(String complexity) {
        switch (complexity) {
            case "simple": return 1.0;
            case "medium": return 0.8;
            case "complex": return 0.6;
            default: return 0.8;
        }
    }

    /**
     * Extract key points
     */
    private String[] extractKeyPoints(String answer) {
        // Simple key points extraction
        String[] sentences = answer.split("[.!?]");
        return java.util.Arrays.stream(sentences)
                .filter(s -> s.trim().length() > 10)
                .limit(3)
                .map(String::trim)
                .toArray(String[]::new);
    }

    /**
     * Create fallback structured answer
     */
    private StructuredAnswer createFallbackStructuredAnswer(String question) {
        StructuredAnswer answer = new StructuredAnswer();
        answer.setMainAnswer("Sorry, unable to generate a satisfactory answer.");
        answer.setConfidence(0.1);
        answer.setSourceCount(0);
        answer.setAnswerType("error");
        answer.setKeyPoints(new String[]{"Insufficient information"});
        return answer;
    }

    /**
     * Structured Answer class
     */
    public static class StructuredAnswer {
        private String mainAnswer;
        private double confidence;
        private int sourceCount;
        private String answerType;
        private String[] keyPoints;

        // Getters and Setters
        public String getMainAnswer() { return mainAnswer; }
        public void setMainAnswer(String mainAnswer) { this.mainAnswer = mainAnswer; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public int getSourceCount() { return sourceCount; }
        public void setSourceCount(int sourceCount) { this.sourceCount = sourceCount; }

        public String getAnswerType() { return answerType; }
        public void setAnswerType(String answerType) { this.answerType = answerType; }

        public String[] getKeyPoints() { return keyPoints; }
        public void setKeyPoints(String[] keyPoints) { this.keyPoints = keyPoints; }
    }
}
