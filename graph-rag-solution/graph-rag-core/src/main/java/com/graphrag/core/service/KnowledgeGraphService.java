package com.graphrag.core.service;

import com.graphrag.data.entity.DocumentNode;
import com.graphrag.data.entity.EntityNode;
import com.graphrag.data.service.DocumentService;
import com.graphrag.data.service.EntityService;
import com.graphrag.data.service.GraphService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识图谱构建服务
 */
@Service
public class KnowledgeGraphService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphService.class);

    @Autowired
    private ChatLanguageModel chatLanguageModel;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private TextSplitterService textSplitterService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private EntityService entityService;

    @Autowired
    private GraphService graphService;

    // 实体和关系提取的提示模板
    private static final PromptTemplate ENTITY_EXTRACTION_TEMPLATE = PromptTemplate.from("""
            请从以下文本中提取实体和它们之间的关系。
            
            文本：
            {{text}}
            
            请按照以下格式输出：
            
            实体：
            - 实体名称 | 实体类型 | 描述
            
            关系：
            - 实体1 | 关系类型 | 实体2 | 关系描述
            
            注意：
            1. 实体类型可以是：人物、地点、组织、概念、技术、产品等
            2. 关系类型可以是：包含、属于、位于、创建、使用、相关等
            3. 只提取重要的实体和关系
            4. 确保实体名称准确且一致
            """);

    /**
     * 从文档构建知识图谱
     */
    public void buildKnowledgeGraphFromDocument(Document document) {
        logger.info("开始从文档构建知识图谱，来源: {}", document.metadata().get("source"));

        try {
            // 1. 保存文档到数据库
            DocumentNode documentNode = saveDocumentToDatabase(document);

            // 2. 分割文档
            List<TextSegment> segments = textSplitterService.splitDocument(document);
            logger.info("文档分割完成，生成 {} 个片段", segments.size());

            // 3. 为每个片段提取实体和关系
            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                logger.debug("处理片段 {}/{}", i + 1, segments.size());
                
                try {
                    extractEntitiesAndRelations(segment, documentNode);
                } catch (Exception e) {
                    logger.error("处理片段失败: {}", segment.text().substring(0, Math.min(100, segment.text().length())), e);
                }
            }

            // 4. 生成文档嵌入向量
            generateDocumentEmbedding(documentNode);

            logger.info("知识图谱构建完成，文档: {}", documentNode.getTitle());

        } catch (Exception e) {
            logger.error("构建知识图谱失败", e);
            throw new RuntimeException("知识图谱构建失败", e);
        }
    }

    /**
     * 保存文档到数据库
     */
    private DocumentNode saveDocumentToDatabase(Document document) {
        String title = extractTitle(document.text());
        String source = (String) document.metadata().get("source");
        
        DocumentNode documentNode = new DocumentNode(title, document.text(), source);
        documentNode.setMetadata(document.metadata().asMap().toString());
        
        return documentService.saveDocument(documentNode);
    }

    /**
     * 提取文档标题
     */
    private String extractTitle(String text) {
        // 简单的标题提取逻辑，取第一行或前50个字符
        String[] lines = text.split("\n");
        if (lines.length > 0 && lines[0].length() < 100) {
            return lines[0].trim();
        }
        
        return text.substring(0, Math.min(50, text.length())).trim() + "...";
    }

    /**
     * 从文本片段提取实体和关系
     */
    private void extractEntitiesAndRelations(TextSegment segment, DocumentNode documentNode) {
        try {
            // 使用 LLM 提取实体和关系
            Prompt prompt = ENTITY_EXTRACTION_TEMPLATE.apply(Map.of("text", segment.text()));
            String response = chatLanguageModel.generate(prompt.text());

            // 解析 LLM 响应
            ExtractionResult result = parseExtractionResponse(response);

            // 保存实体
            List<EntityNode> entities = new ArrayList<>();
            for (EntityInfo entityInfo : result.entities) {
                EntityNode entity = entityService.findOrCreateEntity(entityInfo.name, entityInfo.type);
                entity.setDescription(entityInfo.description);
                
                // 生成实体嵌入向量
                List<Double> embedding = embeddingService.embedText(entityInfo.name + " " + entityInfo.description);
                entity.setEmbedding(embedding);
                
                entities.add(entityService.saveEntity(entity));
                
                // 创建文档与实体的关系
                graphService.createDocumentEntityRelationship(documentNode.getId(), entity.getName(), entity.getType());
            }

            // 保存关系
            for (RelationInfo relationInfo : result.relations) {
                graphService.createRelationship(
                    relationInfo.entity1, getEntityType(relationInfo.entity1, entities),
                    relationInfo.entity2, getEntityType(relationInfo.entity2, entities),
                    relationInfo.relationType, relationInfo.description, 1.0
                );
            }

            logger.debug("提取完成 - 实体: {}, 关系: {}", result.entities.size(), result.relations.size());

        } catch (Exception e) {
            logger.error("实体和关系提取失败", e);
        }
    }

    /**
     * 获取实体类型
     */
    private String getEntityType(String entityName, List<EntityNode> entities) {
        return entities.stream()
                .filter(e -> e.getName().equals(entityName))
                .findFirst()
                .map(EntityNode::getType)
                .orElse("未知");
    }

    /**
     * 解析 LLM 提取响应
     */
    private ExtractionResult parseExtractionResponse(String response) {
        List<EntityInfo> entities = new ArrayList<>();
        List<RelationInfo> relations = new ArrayList<>();

        String[] sections = response.split("关系：");
        
        // 解析实体部分
        if (sections.length > 0) {
            String entitySection = sections[0].replace("实体：", "").trim();
            entities = parseEntities(entitySection);
        }

        // 解析关系部分
        if (sections.length > 1) {
            String relationSection = sections[1].trim();
            relations = parseRelations(relationSection);
        }

        return new ExtractionResult(entities, relations);
    }

    /**
     * 解析实体
     */
    private List<EntityInfo> parseEntities(String entitySection) {
        List<EntityInfo> entities = new ArrayList<>();
        Pattern pattern = Pattern.compile("- (.+?) \\| (.+?) \\| (.+?)(?=\n|$)");
        Matcher matcher = pattern.matcher(entitySection);

        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String type = matcher.group(2).trim();
            String description = matcher.group(3).trim();
            entities.add(new EntityInfo(name, type, description));
        }

        return entities;
    }

    /**
     * 解析关系
     */
    private List<RelationInfo> parseRelations(String relationSection) {
        List<RelationInfo> relations = new ArrayList<>();
        Pattern pattern = Pattern.compile("- (.+?) \\| (.+?) \\| (.+?) \\| (.+?)(?=\n|$)");
        Matcher matcher = pattern.matcher(relationSection);

        while (matcher.find()) {
            String entity1 = matcher.group(1).trim();
            String relationType = matcher.group(2).trim();
            String entity2 = matcher.group(3).trim();
            String description = matcher.group(4).trim();
            relations.add(new RelationInfo(entity1, entity2, relationType, description));
        }

        return relations;
    }

    /**
     * 生成文档嵌入向量
     */
    private void generateDocumentEmbedding(DocumentNode documentNode) {
        try {
            // 使用文档标题和内容的前500字符生成嵌入
            String textForEmbedding = documentNode.getTitle() + "\n" + 
                    documentNode.getContent().substring(0, Math.min(500, documentNode.getContent().length()));
            
            List<Double> embedding = embeddingService.embedText(textForEmbedding);
            documentService.updateEmbedding(documentNode.getId(), embedding);
            
            logger.debug("文档嵌入向量生成完成，文档ID: {}", documentNode.getId());
        } catch (Exception e) {
            logger.error("生成文档嵌入向量失败", e);
        }
    }

    // 内部数据类
    private static class ExtractionResult {
        final List<EntityInfo> entities;
        final List<RelationInfo> relations;

        ExtractionResult(List<EntityInfo> entities, List<RelationInfo> relations) {
            this.entities = entities;
            this.relations = relations;
        }
    }

    private static class EntityInfo {
        final String name;
        final String type;
        final String description;

        EntityInfo(String name, String type, String description) {
            this.name = name;
            this.type = type;
            this.description = description;
        }
    }

    private static class RelationInfo {
        final String entity1;
        final String entity2;
        final String relationType;
        final String description;

        RelationInfo(String entity1, String entity2, String relationType, String description) {
            this.entity1 = entity1;
            this.entity2 = entity2;
            this.relationType = relationType;
            this.description = description;
        }
    }
}

