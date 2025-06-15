package com.graphrag.core.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 图 RAG 响应模型
 */
public class GraphRagResponse {

    private String question;
    private String answer;
    private List<Map<String, Object>> relevantDocuments;
    private List<Map<String, Object>> relevantEntities;
    private List<Map<String, Object>> graphContext;
    private Double confidence;
    private Long processingTimeMs;
    private LocalDateTime timestamp;

    public GraphRagResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<Map<String, Object>> getRelevantDocuments() { return relevantDocuments; }
    public void setRelevantDocuments(List<Map<String, Object>> relevantDocuments) { this.relevantDocuments = relevantDocuments; }

    public List<Map<String, Object>> getRelevantEntities() { return relevantEntities; }
    public void setRelevantEntities(List<Map<String, Object>> relevantEntities) { this.relevantEntities = relevantEntities; }

    public List<Map<String, Object>> getGraphContext() { return graphContext; }
    public void setGraphContext(List<Map<String, Object>> graphContext) { this.graphContext = graphContext; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

