package com.graphrag.core.service;

import com.graphrag.core.model.GraphRagRequest;
import com.graphrag.core.model.GraphRagResponse;
import com.graphrag.data.entity.DocumentNode;
import com.graphrag.data.entity.EntityNode;
import com.graphrag.data.service.DocumentService;
import com.graphrag.data.service.EntityService;
import com.graphrag.data.service.GraphService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 圖 RAG 檢索服務
 */
@Service
public class GraphRagRetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(GraphRagRetrievalService.class);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private EntityService entityService;

    @Autowired
    private GraphService graphService;

    // RAG 生成的提示模板
    private static final PromptTemplate RAG_TEMPLATE = PromptTemplate.from("""
            基於以下上下文資訊回答用戶問題。
            
            上下文資訊：
            {{context}}
            
            用戶問題：
            {{question}}
            
            請根據上下文資訊提供準確、詳細的回答。如果上下文資訊不足以回答問題，請說明這一點。
            
            回答：
            """);

    /**
     * 執行圖 RAG 檢索和生成
     */
    public GraphRagResponse retrieve(GraphRagRequest request) {
        logger.info("開始執行圖 RAG 檢索，問題: {}", request.getQuestion());

        try {
            // 1. 生成查詢嵌入向量
            List<Double> queryEmbedding = embeddingService.embedText(request.getQuestion());

            // 2. 向量檢索相關文檔
            List<DocumentNode> similarDocuments = documentService.findSimilarDocuments(
                    queryEmbedding, 0.7, 5);

            // 3. 向量檢索相關實體
            List<EntityNode> similarEntities = entityService.findSimilarEntities(
                    queryEmbedding, 0.7, 10);

            // 4. 圖遍歷獲取相關子圖
            List<Map<String, Object>> graphContext = retrieveGraphContext(request.getQuestion(), similarEntities);

            // 5. 整合上下文資訊
            String context = buildContext(similarDocuments, similarEntities, graphContext);

            // 6. 生成回答
            String answer = generateAnswer(request.getQuestion(), context);

            // 7. 構建響應
            GraphRagResponse response = new GraphRagResponse();
            response.setQuestion(request.getQuestion());
            response.setAnswer(answer);
            response.setRelevantDocuments(similarDocuments.stream()
                    .map(doc -> {
                        Map<String, Object> docMap = new HashMap<>();
                        docMap.put("id", doc.getId());
                        docMap.put("title", doc.getTitle());
                        docMap.put("source", doc.getSource());
                        return docMap;
                    })
                    .collect(Collectors.toList()));
            response.setRelevantEntities(similarEntities.stream()
                    .map(entity -> {
                        Map<String, Object> entityMap = new HashMap<>();
                        entityMap.put("id", entity.getId());
                        entityMap.put("name", entity.getName());
                        entityMap.put("type", entity.getType());
                        return entityMap;
                    })
                    .collect(Collectors.toList()));

            logger.info("圖 RAG 檢索完成，找到 {} 個相關文檔，{} 個相關實體", 
                    similarDocuments.size(), similarEntities.size());

            return response;

        } catch (Exception e) {
            logger.error("圖 RAG 檢索失敗", e);
            throw new RuntimeException("檢索失敗", e);
        }
    }

    /**
     * 檢索圖上下文
     */
    private List<Map<String, Object>> retrieveGraphContext(String question, List<EntityNode> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }

        // 構建圖查詢，獲取實體間的關係
        String cypher = """
                MATCH (e1:Entity)-[r]-(e2:Entity)
                WHERE e1.name IN $entityNames
                RETURN e1.name AS entity1, type(r) AS relationship, e2.name AS entity2, r.description AS description
                LIMIT 50
                """;

        List<String> entityNames = entities.stream()
                .map(EntityNode::getName)
                .collect(Collectors.toList());

        Map<String, Object> parameters = Map.of("entityNames", entityNames);

        return graphService.executeCypher(cypher, parameters);
    }

    /**
     * 構建上下文資訊
     */
    private String buildContext(List<DocumentNode> documents, List<EntityNode> entities, 
                               List<Map<String, Object>> graphContext) {
        StringBuilder context = new StringBuilder();

        // 添加文檔資訊
        if (!documents.isEmpty()) {
            context.append("相關文檔：\n");
            for (DocumentNode doc : documents) {
                context.append("- ").append(doc.getTitle()).append(": ")
                        .append(doc.getContent().substring(0, Math.min(200, doc.getContent().length())))
                        .append("...\n");
            }
            context.append("\n");
        }

        // 添加實體資訊
        if (!entities.isEmpty()) {
            context.append("相關實體：\n");
            for (EntityNode entity : entities) {
                context.append("- ").append(entity.getName()).append(" (").append(entity.getType()).append(")");
                if (entity.getDescription() != null) {
                    context.append(": ").append(entity.getDescription());
                }
                context.append("\n");
            }
            context.append("\n");
        }

        // 添加關係資訊
        if (!graphContext.isEmpty()) {
            context.append("實體關係：\n");
            for (Map<String, Object> relation : graphContext) {
                context.append("- ").append(relation.get("entity1"))
                        .append(" ").append(relation.get("relationship"))
                        .append(" ").append(relation.get("entity2"));
                if (relation.get("description") != null) {
                    context.append(" (").append(relation.get("description")).append(")");
                }
                context.append("\n");
            }
        }

        return context.toString();
    }

    /**
     * 生成回答
     */
    private String generateAnswer(String question, String context) {
        Prompt prompt = RAG_TEMPLATE.apply(Map.of(
                "question", question,
                "context", context
        ));

        return chatLanguageModel.generate(prompt.text());
    }

    /**
     * 混合檢索（向量 + 圖遍歷）
     */
    public GraphRagResponse hybridRetrieve(GraphRagRequest request) {
        logger.info("開始執行混合檢索，問題: {}", request.getQuestion());

        try {
            // 1. 向量檢索
            List<Double> queryEmbedding = embeddingService.embedText(request.getQuestion());
            List<DocumentNode> vectorDocuments = documentService.findSimilarDocuments(queryEmbedding, 0.6, 3);
            List<EntityNode> vectorEntities = entityService.findSimilarEntities(queryEmbedding, 0.6, 5);

            // 2. 關鍵詞檢索
            List<DocumentNode> keywordDocuments = documentService.searchByContent(request.getQuestion());
            
            // 3. 圖遍歷檢索
            List<EntityNode> graphEntities = findEntitiesByGraphTraversal(request.getQuestion());

            // 4. 合併結果
            List<DocumentNode> allDocuments = mergeDocuments(vectorDocuments, keywordDocuments);
            List<EntityNode> allEntities = mergeEntities(vectorEntities, graphEntities);

            // 5. 獲取圖上下文
            List<Map<String, Object>> graphContext = retrieveGraphContext(request.getQuestion(), allEntities);

            // 6. 構建上下文並生成回答
            String context = buildContext(allDocuments, allEntities, graphContext);
            String answer = generateAnswer(request.getQuestion(), context);

            // 7. 構建響應
            GraphRagResponse response = new GraphRagResponse();
            response.setQuestion(request.getQuestion());
            response.setAnswer(answer);
            response.setRelevantDocuments(allDocuments.stream()
                    .map(doc -> {
                        Map<String, Object> docMap = new HashMap<>();
                        docMap.put("id", doc.getId());
                        docMap.put("title", doc.getTitle());
                        docMap.put("source", doc.getSource());
                        return docMap;
                    })
                    .collect(Collectors.toList()));
            response.setRelevantEntities(allEntities.stream()
                    .map(entity -> {
                        Map<String, Object> entityMap = new HashMap<>();
                        entityMap.put("id", entity.getId());
                        entityMap.put("name", entity.getName());
                        entityMap.put("type", entity.getType());
                        return entityMap;
                    })
                    .collect(Collectors.toList()));

            logger.info("混合檢索完成，找到 {} 個相關文檔，{} 個相關實體", 
                    allDocuments.size(), allEntities.size());

            return response;

        } catch (Exception e) {
            logger.error("混合檢索失敗", e);
            throw new RuntimeException("混合檢索失敗", e);
        }
    }

    /**
     * 通過圖遍歷查找實體
     */
    private List<EntityNode> findEntitiesByGraphTraversal(String question) {
        // 簡單的關鍵詞匹配，實際應用中可以使用更複雜的 NLP 技術
        String[] keywords = question.toLowerCase().split("\\s+");
        
        List<EntityNode> entities = entityService.findAll();
        return entities.stream()
                .filter(entity -> {
                    String entityText = (entity.getName() + " " + entity.getDescription()).toLowerCase();
                    for (String keyword : keywords) {
                        if (entityText.contains(keyword)) {
                            return true;
                        }
                    }
                    return false;
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 合併文檔列表
     */
    private List<DocumentNode> mergeDocuments(List<DocumentNode>... documentLists) {
        return java.util.Arrays.stream(documentLists)
                .flatMap(List::stream)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 合併實體列表
     */
    private List<EntityNode> mergeEntities(List<EntityNode>... entityLists) {
        return java.util.Arrays.stream(entityLists)
                .flatMap(List::stream)
                .distinct()
                .limit(15)
                .collect(Collectors.toList());
    }
}
