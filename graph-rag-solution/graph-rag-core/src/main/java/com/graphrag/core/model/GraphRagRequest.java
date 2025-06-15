package com.graphrag.core.model;

import java.util.List;
import java.util.Map;

/**
 * 图 RAG 请求模型
 */
public class GraphRagRequest {

    private String question;
    private Integer maxDocuments = 5;
    private Integer maxEntities = 10;
    private Double similarityThreshold = 0.7;
    private String retrievalMode = "hybrid"; // vector, graph, hybrid
    private Map<String, Object> parameters;

    public GraphRagRequest() {}

    public GraphRagRequest(String question) {
        this.question = question;
    }

    // Getters and Setters
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public Integer getMaxDocuments() { return maxDocuments; }
    public void setMaxDocuments(Integer maxDocuments) { this.maxDocuments = maxDocuments; }

    public Integer getMaxEntities() { return maxEntities; }
    public void setMaxEntities(Integer maxEntities) { this.maxEntities = maxEntities; }

    public Double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(Double similarityThreshold) { this.similarityThreshold = similarityThreshold; }

    public String getRetrievalMode() { return retrievalMode; }
    public void setRetrievalMode(String retrievalMode) { this.retrievalMode = retrievalMode; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
}

