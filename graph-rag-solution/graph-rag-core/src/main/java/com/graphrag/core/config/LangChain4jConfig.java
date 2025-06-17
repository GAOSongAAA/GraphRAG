package com.graphrag.core.config;

import com.graphrag.common.config.GraphRagProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import java.time.Duration;


/**
 * LangChain4j 配置
 * 支持 OpenAI 和 Azure OpenAI
 */
@Configuration
@Order(1)
public class LangChain4jConfig {

    @Autowired
    private GraphRagProperties properties;

    private static final Logger logger = LoggerFactory.getLogger(LangChain4jConfig.class);
    /**
     * 配置聊天語言模型
     * 自動檢測是否為 Azure OpenAI 或標準 OpenAI
     */
    @Bean
    @Primary
    @Order(1)
    public ChatLanguageModel chatLanguageModel() {
        String baseUrl = properties.getLlm().getBaseUrl();
        String apiKey = properties.getLlm().getApiKey();
        
        // 檢測是否為 Azure OpenAI
        if (isAzureOpenAI(baseUrl)) {
            return createAzureChatModel(baseUrl, apiKey);
        } else {
            return createStandardChatModel(baseUrl, apiKey);
        }
    }

    /**
     * 配置嵌入模型
     * 自動檢測是否為 Azure OpenAI 或標準 OpenAI
     */
    @Bean
    @Order(1)
    public EmbeddingModel embeddingModel() {
        String baseUrl = properties.getLlm().getBaseUrl();
        String apiKey = properties.getLlm().getApiKey();
        
        // 檢測是否為 Azure OpenAI
        if (isAzureOpenAI(baseUrl)) {
            return createAzureEmbeddingModel(baseUrl, apiKey);
        } else {
            return createStandardEmbeddingModel(baseUrl, apiKey);
        }
    }

    /**
     * 檢測是否為 Azure OpenAI
     */
    private boolean isAzureOpenAI(String baseUrl) {
        return baseUrl != null && baseUrl.contains("openai.azure.com");
    }

    /**
     * 創建 Azure OpenAI 聊天模型
     */
    private ChatLanguageModel createAzureChatModel(String baseUrl, String apiKey) {
        // 從環境變量獲取 API 版本，默認為 2023-05-15
        String apiVersion = System.getenv("AZURE_OPENAI_API_VERSION");
        if (apiVersion == null || apiVersion.isEmpty()) {
            apiVersion = "2023-05-15";
        }
        
        // 確保 baseUrl 以 / 結尾
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        
        logger.info("創建 Azure OpenAI 聊天模型 - Endpoint: {}, Deployment: {}, API Version: {}", 
                baseUrl, properties.getLlm().getModel(), apiVersion);
        
        return AzureOpenAiChatModel.builder()
                .endpoint(baseUrl)
                .deploymentName(properties.getLlm().getModel())
                .serviceVersion(apiVersion)
                .apiKey(apiKey)
                .temperature(properties.getLlm().getTemperature())
                .maxTokens(properties.getLlm().getMaxTokens())
                .timeout(Duration.ofMinutes(2))
                .maxRetries(3)
                .build();
    }

    /**
     * 創建標準 OpenAI 聊天模型
     */
    private ChatLanguageModel createStandardChatModel(String baseUrl, String apiKey) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(properties.getLlm().getModel())
                .temperature(properties.getLlm().getTemperature())
                .maxTokens(properties.getLlm().getMaxTokens())
                .timeout(Duration.ofMinutes(2))
                .maxRetries(3)
                .build();
    }

    /**
     * 創建 Azure OpenAI 嵌入模型
     */
    private EmbeddingModel createAzureEmbeddingModel(String baseUrl, String apiKey) {
        // 從環境變量獲取 API 版本，默認為 2023-05-15
        String apiVersion = System.getenv("AZURE_OPENAI_API_VERSION");
        if (apiVersion == null || apiVersion.isEmpty()) {
            apiVersion = "2023-05-15";
        }
        
        // 確保 baseUrl 以 / 結尾
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        
        logger.info("創建 Azure OpenAI 嵌入模型 - Endpoint: {}, Deployment: {}, API Version: {}", 
                baseUrl, properties.getEmbedding().getModel(), apiVersion);
        
        return AzureOpenAiEmbeddingModel.builder()
                .endpoint(baseUrl)
                .deploymentName(properties.getEmbedding().getModel())
                .serviceVersion(apiVersion)
                .apiKey(apiKey)
                .timeout(Duration.ofMinutes(1))
                .maxRetries(3)
                .build();
    }

    /**
     * 創建標準 OpenAI 嵌入模型
     */
    private EmbeddingModel createStandardEmbeddingModel(String baseUrl, String apiKey) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(properties.getEmbedding().getModel())
                .timeout(Duration.ofMinutes(1))
                .maxRetries(3)
                .build();
    }
}