package com.graphrag.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置属性
 */
@Component
@ConfigurationProperties(prefix = "graph-rag")
public class GraphRagProperties {

    private Neo4j neo4j = new Neo4j();
    private Llm llm = new Llm();
    private Embedding embedding = new Embedding();

    public static class Neo4j {
        private String uri = "bolt://localhost:7687";
        private String username = "neo4j";
        private String password = "password";
        private String database = "neo4j";

        // Getters and Setters
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
    }

    public static class Llm {
        private String provider = "openai";
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-3.5-turbo";
        private Double temperature = 0.7;
        private Integer maxTokens = 1000;

        // Getters and Setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    }

    public static class Embedding {
        private String provider = "openai";
        private String model = "text-embedding-ada-002";
        private Integer dimensions = 1536;

        // Getters and Setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        
        public Integer getDimensions() { return dimensions; }
        public void setDimensions(Integer dimensions) { this.dimensions = dimensions; }
    }

    // Main class getters and setters
    public Neo4j getNeo4j() { return neo4j; }
    public void setNeo4j(Neo4j neo4j) { this.neo4j = neo4j; }
    
    public Llm getLlm() { return llm; }
    public void setLlm(Llm llm) { this.llm = llm; }
    
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }
}

