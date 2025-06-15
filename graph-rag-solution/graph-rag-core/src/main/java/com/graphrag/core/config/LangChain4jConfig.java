package com.graphrag.core.config;

import com.graphrag.common.config.GraphRagProperties;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 配置
 */
@Configuration
public class LangChain4jConfig {

    @Autowired
    private GraphRagProperties properties;

    /**
     * 配置聊天语言模型
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(properties.getLlm().getApiKey())
                .baseUrl(properties.getLlm().getBaseUrl())
                .modelName(properties.getLlm().getModel())
                .temperature(properties.getLlm().getTemperature())
                .maxTokens(properties.getLlm().getMaxTokens())
                .timeout(Duration.ofMinutes(2))
                .maxRetries(3)
                .build();
    }

    /**
     * 配置嵌入模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(properties.getLlm().getApiKey())
                .baseUrl(properties.getLlm().getBaseUrl())
                .modelName(properties.getEmbedding().getModel())
                .timeout(Duration.ofMinutes(1))
                .maxRetries(3)
                .build();
    }
}

