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
 * Knowledge Graph Construction Service
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

    // Prompt template for entity and relation extraction
    private static final PromptTemplate ENTITY_EXTRACTION_TEMPLATE = PromptTemplate.from("""
            Please extract entities and their relationships from the following text.
            
            Text:
            {{text}}
            
            Please output in the following format:
            
            Entities:
            - Entity Name | Entity Type | Description
            
            Relations:
            - Entity1 | Relation Type | Entity2 | Relation Description
            
            Notes:
            1. Entity types can be: Person, Location, Organization, Concept, Technology, Product, etc.
            2. Relation types can be: Contains, BelongsTo, LocatedIn, Creates, Uses, RelatedTo, etc.
            3. Only extract important entities and relations
            4. Ensure entity names are accurate and consistent
            """);

    /**
     * Build knowledge graph from document
     */
    public void buildKnowledgeGraphFromDocument(Document document) {
        logger.info("Starting to build knowledge graph from document, source: {}", document.metadata().get("source"));

        try {
            // 1. Save document to database
            DocumentNode documentNode = saveDocumentToDatabase(document);

            // 2. Split document
            List<TextSegment> segments = textSplitterService.splitDocument(document);
            logger.info("Document splitting completed, generated {} segments", segments.size());

            // 3. Extract entities and relations for each segment
            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                logger.debug("Processing segment {}/{}", i + 1, segments.size());
                
                try {
                    extractEntitiesAndRelations(segment, documentNode);
                } catch (Exception e) {
                    logger.error("Failed to process segment: {}", segment.text().substring(0, Math.min(100, segment.text().length())), e);
                }
            }

            // 4. Generate document embedding
            generateDocumentEmbedding(documentNode);

            logger.info("Knowledge graph construction completed, document: {}", documentNode.getTitle());

        } catch (Exception e) {
            logger.error("Failed to build knowledge graph", e);
            throw new RuntimeException("Knowledge graph construction failed", e);
        }
    }

    /**
     * Save document to database
     */
    private DocumentNode saveDocumentToDatabase(Document document) {
        String title = extractTitle(document.text());
        String source = (String) document.metadata().get("source");
        
        DocumentNode documentNode = new DocumentNode(title, document.text(), source);
        documentNode.setMetadata(document.metadata().asMap().toString());
        
        return documentService.findOrCreateDocument(documentNode);
    }

    /**
     * Extract document title
     */
    private String extractTitle(String text) {
        // Simple title extraction logic, take first line or first 50 characters
        String[] lines = text.split("\n");
        if (lines.length > 0 && lines[0].length() < 100) {
            return lines[0].trim();
        }
        
        return text.substring(0, Math.min(50, text.length())).trim() + "...";
    }

    /**
     * Extract entities and relations from text segment
     */
    private void extractEntitiesAndRelations(TextSegment segment, DocumentNode documentNode) {
        try {
            // Use LLM to extract entities and relations
            Prompt prompt = ENTITY_EXTRACTION_TEMPLATE.apply(Map.of("text", segment.text()));
            String response = chatLanguageModel.generate(prompt.text());

            // Parse LLM response
            ExtractionResult result = parseExtractionResponse(response);

            // Save entities
            List<EntityNode> entities = new ArrayList<>();
            for (EntityInfo entityInfo : result.entities) {
                EntityNode entity = entityService.findOrCreateEntity(entityInfo.name, entityInfo.type);
                entity.setDescription(entityInfo.description);
                
                // Generate entity embedding
                List<Double> embedding = embeddingService.embedText(entityInfo.name + " " + entityInfo.description);
                entity.setEmbedding(embedding);
                
                entities.add(entityService.saveEntity(entity));
                
                // Create relationship between document and entity
                graphService.createDocumentEntityRelationship(documentNode.getId(), entity.getName(), entity.getType());
            }

            // Save relations
            for (RelationInfo relationInfo : result.relations) {
                graphService.createRelationship(
                    relationInfo.entity1, getEntityType(relationInfo.entity1, entities),
                    relationInfo.entity2, getEntityType(relationInfo.entity2, entities),
                    relationInfo.relationType, relationInfo.description, 1.0
                );
            }

            logger.debug("Extraction completed - Entities: {}, Relations: {}", result.entities.size(), result.relations.size());

        } catch (Exception e) {
            logger.error("Failed to extract entities and relations", e);
        }
    }

    /**
     * Get entity type
     */
    private String getEntityType(String entityName, List<EntityNode> entities) {
        return entities.stream()
                .filter(e -> e.getName().equals(entityName))
                .findFirst()
                .map(EntityNode::getType)
                .orElse("Unknown");
    }

    /**
     * Parse LLM extraction response
     */
    private ExtractionResult parseExtractionResponse(String response) {
        List<EntityInfo> entities = new ArrayList<>();
        List<RelationInfo> relations = new ArrayList<>();

        String[] sections = response.split("Relations:");
        
        // Parse entities section
        if (sections.length > 0) {
            String entitySection = sections[0].replace("Entities:", "").trim();
            entities = parseEntities(entitySection);
        }

        // Parse relations section
        if (sections.length > 1) {
            String relationSection = sections[1].trim();
            relations = parseRelations(relationSection);
        }

        return new ExtractionResult(entities, relations);
    }

    /**
     * Parse entities
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
     * Parse relations
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
     * Generate document embedding
     */
    private void generateDocumentEmbedding(DocumentNode documentNode) {
        try {
            // Generate embedding using document title and first 500 characters of content
            String textForEmbedding = documentNode.getTitle() + "\n" + 
                    documentNode.getContent().substring(0, Math.min(500, documentNode.getContent().length()));
            
            List<Double> embedding = embeddingService.embedText(textForEmbedding);
            documentService.updateEmbedding(documentNode.getId(), embedding);
            
            logger.debug("Document embedding generation completed, document ID: {}", documentNode.getId());
        } catch (Exception e) {
            logger.error("Failed to generate document embedding", e);
        }
    }

    // Inner data classes
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
