package com.graphrag.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger 配置
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Graph RAG API")
                        .version("1.0.0")
                        .description("基于 Java、LangChain4j 和 Neo4j 的大规模图 RAG 工业级解决方案")
                        .contact(new Contact()
                                .name("Graph RAG Team")
                                .email("support@graphrag.com")
                                .url("https://github.com/graphrag/graph-rag-solution"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}

