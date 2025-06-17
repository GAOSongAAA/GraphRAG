package com.graphrag.core.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Enhanced Knowledge Graph Construction Service
 * Supports smarter entity extraction and relationship recognition
 */
@Service
public class EnhancedKnowledgeGraphService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedKnowledgeGraphService.class);

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

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Improved entity and relationship extraction prompt template - using JSON format output
    private static final PromptTemplate ENHANCED_EXTRACTION_TEMPLATE = PromptTemplate.from("""
            You are a professional knowledge graph construction expert. Please extract entities and relationships from the following text and output in JSON format.
            
            Text:
            {{text}}
            
            Please output in the following JSON format:
            {
              "entities": [
                {
                  "name": "Entity Name",
                  "type": "Entity Type",
                  "description": "Entity Description",
                  "importance": 0.8,
                  "aliases": ["Alias1", "Alias2"]
                }
              ],
              "relationships": [
                {
                  "source": "Source Entity Name",
                  "target": "Target Entity Name",
                  "relation_type": "Relation Type",
                  "description": "Relation Description",
                  "confidence": 0.9,
                  "direction": "directed"
                }
              ]
            }
            
            Entity types include but are not limited to:
            - PERSON
            - ORGANIZATION
            - LOCATION
            - CONCEPT
            - TECHNOLOGY
            - PRODUCT
            - EVENT
            - TIME
            - QUANTITY
            
            Relation types include but are not limited to:
            - WORKS_AT
            - FOUNDED
            - LOCATED_IN
            - PART_OF
            - DEVELOPS
            - USES
            - RELATED_TO
            - CAUSES
            - FOLLOWS
            
            Notes:
            1. Only extract important and clear entities and relationships
            2. importance and confidence values range from 0.0-1.0
            3. Ensure entity name consistency
            4. Relationship direction: directed or undirected
            """);

    // Entity type validation and standardization prompt
    private static final PromptTemplate ENTITY_VALIDATION_TEMPLATE = PromptTemplate.from("""
            Please validate and standardize the following entity information:
            
            Entity Name: {{entity_name}}
            Current Type: {{current_type}}
            Description: {{description}}
            
            Please output standardized JSON format:
            {
              "standardized_name": "Standardized Name",
              "verified_type": "Verified Type",
              "enhanced_description": "Enhanced Description",
              "confidence": 0.9
            }
            
            If entity information has issues, set confidence < 0.5
            """);

    /**
     * Build enhanced knowledge graph from document
     */
    public CompletableFuture<DocumentNode> buildEnhancedKnowledgeGraph(Document document) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Starting to build enhanced knowledge graph, source: {}", document.metadata().get("source"));

            try {
                // 1. Save document to database
                DocumentNode documentNode = saveDocumentToDatabase(document);

                // 2. Split document
                List<TextSegment> segments = textSplitterService.splitDocumentOptimized(document);
                logger.info("Document splitting completed, generated {} segments", segments.size());

                // 3. Process all segments in parallel
                List<CompletableFuture<ExtractionResult>> futures = segments.stream()
                    .map(segment -> extractEntitiesAndRelationsAsync(segment, documentNode))
                    .collect(Collectors.toList());

                // 4. Collect all results
                List<ExtractionResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

                // 5. Merge and deduplicate entities
                mergeAndDeduplicateEntities(results, documentNode);

                // 6. Generate document embedding
                generateDocumentEmbedding(documentNode);

                logger.info("Enhanced knowledge graph construction completed, document: {}", documentNode.getTitle());
                return documentNode;

            } catch (Exception e) {
                logger.error("Failed to build enhanced knowledge graph", e);
                throw new RuntimeException("Enhanced knowledge graph construction failed", e);
            }
        }, executorService);
    }

    /**
     * Extract entities and relationships asynchronously
     */
    private CompletableFuture<ExtractionResult> extractEntitiesAndRelationsAsync(
            TextSegment segment, DocumentNode documentNode) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use enhanced LLM extraction
                Prompt prompt = ENHANCED_EXTRACTION_TEMPLATE.apply(Map.of("text", segment.text()));
                String response = chatLanguageModel.generate(prompt.text());

                // Parse JSON response
                ExtractionResult result = parseEnhancedExtractionResponse(response);

                // Validate and standardize entities
                List<ValidatedEntity> validatedEntities = validateEntities(result.entities);

                // Process entities
                List<EntityNode> processedEntities = processEntities(validatedEntities, documentNode);

                // Process relationships
                processRelationships(result.relationships, processedEntities);

                logger.debug("Segment processing completed - Entities: {}, Relationships: {}", 
                    processedEntities.size(), result.relationships.size());

                return new ExtractionResult(
                    result.entities, 
                    result.relationships, 
                    processedEntities
                );

            } catch (Exception e) {
                logger.error("Failed to extract entities and relationships asynchronously", e);
                return new ExtractionResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            }
        }, executorService);
    }

    /**
     * Parse enhanced extraction response (JSON format)
     */
    private ExtractionResult parseEnhancedExtractionResponse(String response) {
        try {
            // Clean response text, extract JSON part
            String jsonText = extractJsonFromResponse(response);
            
            ExtractionResponse extractionResponse = objectMapper.readValue(jsonText, ExtractionResponse.class);
            
            List<EnhancedEntityInfo> entities = extractionResponse.entities != null ? 
                extractionResponse.entities : Collections.emptyList();
            List<EnhancedRelationInfo> relationships = extractionResponse.relationships != null ? 
                extractionResponse.relationships : Collections.emptyList();

            return new ExtractionResult(entities, relationships, Collections.emptyList());

        } catch (JsonProcessingException e) {
            logger.warn("JSON parsing failed, falling back to text parsing: {}", e.getMessage());
            return parseTextExtractionResponse(response);
        }
    }

    /**
     * Extract JSON part from response
     */
    private String extractJsonFromResponse(String response) {
        // Find JSON start and end positions
        int startIndex = response.indexOf("{");
        int endIndex = response.lastIndexOf("}");
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }
        
        throw new RuntimeException("Cannot extract valid JSON from response");
    }

    /**
     * Text format parsing (fallback)
     */
    private ExtractionResult parseTextExtractionResponse(String response) {
        // Use original text parsing logic as fallback
        logger.info("Using text parsing as fallback");
        return new ExtractionResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Validate entity information
     */
    private List<ValidatedEntity> validateEntities(List<EnhancedEntityInfo> entities) {
        return entities.stream()
            .filter(entity -> entity.importance > 0.5) // Only keep high importance entities
            .map(this::validateSingleEntity)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Validate single entity
     */
    private ValidatedEntity validateSingleEntity(EnhancedEntityInfo entity) {
        try {
            Prompt prompt = ENTITY_VALIDATION_TEMPLATE.apply(Map.of(
                "entity_name", entity.name,
                "current_type", entity.type,
                "description", entity.description != null ? entity.description : ""
            ));

            String response = chatLanguageModel.generate(prompt.text());
            String jsonText = extractJsonFromResponse(response);
            
            ValidationResponse validation = objectMapper.readValue(jsonText, ValidationResponse.class);
            
            if (validation.confidence > 0.5) {
                return new ValidatedEntity(
                    validation.standardizedName,
                    validation.verifiedType,
                    validation.enhancedDescription,
                    validation.confidence,
                    entity.aliases
                );
            }
            
        } catch (Exception e) {
            logger.warn("Entity validation failed: {}", entity.name, e);
        }
        
        return null;
    }

    /**
     * Process entities
     */
    private List<EntityNode> processEntities(List<ValidatedEntity> validatedEntities, DocumentNode documentNode) {
        List<EntityNode> processedEntities = new ArrayList<>();

        for (ValidatedEntity validatedEntity : validatedEntities) {
            try {
                // Find or create entity
                EntityNode entity = entityService.findOrCreateEntity(
                    validatedEntity.name, 
                    validatedEntity.type
                );
                
                // Update entity information
                entity.setDescription(validatedEntity.description);
                
                // Generate entity embedding
                String embeddingText = String.format("%s %s %s", 
                    validatedEntity.name, 
                    validatedEntity.type, 
                    validatedEntity.description);
                
                List<Double> embedding = embeddingService.embedText(embeddingText);
                entity.setEmbedding(embedding);
                
                // Save entity
                EntityNode savedEntity = entityService.saveEntity(entity);
                processedEntities.add(savedEntity);
                
                // Create document-entity relationship
                graphService.createDocumentEntityRelationship(
                    documentNode.getId(), 
                    savedEntity.getName(), 
                    savedEntity.getType()
                );
                
            } catch (Exception e) {
                logger.error("Failed to process entity: {}", validatedEntity.name, e);
            }
        }

        return processedEntities;
    }

    /**
     * Process relationships
     */
    private void processRelationships(List<EnhancedRelationInfo> relationships, List<EntityNode> entities) {
        Map<String, String> entityTypeMap = entities.stream()
            .collect(Collectors.toMap(EntityNode::getName, EntityNode::getType, (a, b) -> a));

        for (EnhancedRelationInfo relation : relationships) {
            try {
                if (relation.confidence > 0.7) { // Only keep high confidence relationships
                    String sourceType = entityTypeMap.getOrDefault(relation.source, "UNKNOWN");
                    String targetType = entityTypeMap.getOrDefault(relation.target, "UNKNOWN");
                    
                    graphService.createRelationship(
                        relation.source, sourceType,
                        relation.target, targetType,
                        relation.relationType, 
                        relation.description, 
                        relation.confidence
                    );
                }
            } catch (Exception e) {
                logger.error("Failed to process relationship: {} -> {}", relation.source, relation.target, e);
            }
        }
    }

    /**
     * Merge and deduplicate entities
     */
    private void mergeAndDeduplicateEntities(List<ExtractionResult> results, DocumentNode documentNode) {
        Map<String, EntityNode> entityMap = new HashMap<>();

        for (ExtractionResult result : results) {
            // Merge entities
            for (EntityNode entity : result.processedEntities) {
                String key = entity.getName() + ":" + entity.getType();
                if (!entityMap.containsKey(key)) {
                    entityMap.put(key, entity);
                }
            }
        }

        logger.info("Number of entities after deduplication: {}", entityMap.size());
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
        String[] lines = text.split("\n");
        if (lines.length > 0 && lines[0].length() < 100) {
            return lines[0].trim();
        }
        return text.substring(0, Math.min(50, text.length())).trim() + "...";
    }

    /**
     * Generate document embedding
     */
    private void generateDocumentEmbedding(DocumentNode documentNode) {
        try {
            String textForEmbedding = documentNode.getTitle() + "\n" + 
                    documentNode.getContent().substring(0, Math.min(1000, documentNode.getContent().length()));
            
            List<Double> embedding = embeddingService.embedText(textForEmbedding);
            documentService.updateEmbedding(documentNode.getId(), embedding);
            
            logger.debug("Document embedding generation completed, document ID: {}", documentNode.getId());
        } catch (Exception e) {
            logger.error("Failed to generate document embedding", e);
        }
    }

    // JSON response data classes
    static class ExtractionResponse {
        @JsonProperty("entities")
        public List<EnhancedEntityInfo> entities;
        
        @JsonProperty("relationships")
        public List<EnhancedRelationInfo> relationships;
    }

    static class EnhancedEntityInfo {
        @JsonProperty("name")
        public String name;
        
        @JsonProperty("type")
        public String type;
        
        @JsonProperty("description")
        public String description;
        
        @JsonProperty("importance")
        public double importance = 0.5;
        
        @JsonProperty("aliases")
        public List<String> aliases = new ArrayList<>();
    }

    static class EnhancedRelationInfo {
        @JsonProperty("source")
        public String source;
        
        @JsonProperty("target")
        public String target;
        
        @JsonProperty("relation_type")
        public String relationType;
        
        @JsonProperty("description")
        public String description;
        
        @JsonProperty("confidence")
        public double confidence = 0.5;
        
        @JsonProperty("direction")
        public String direction = "directed";
    }

    static class ValidationResponse {
        @JsonProperty("standardized_name")
        public String standardizedName;
        
        @JsonProperty("verified_type")
        public String verifiedType;
        
        @JsonProperty("enhanced_description")
        public String enhancedDescription;
        
        @JsonProperty("confidence")
        public double confidence;
    }

    static class ValidatedEntity {
        final String name;
        final String type;
        final String description;
        final double confidence;
        final List<String> aliases;

        ValidatedEntity(String name, String type, String description, double confidence, List<String> aliases) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.confidence = confidence;
            this.aliases = aliases != null ? aliases : new ArrayList<>();
        }
    }

    static class ExtractionResult {
        final List<EnhancedEntityInfo> entities;
        final List<EnhancedRelationInfo> relationships;
        final List<EntityNode> processedEntities;

        ExtractionResult(List<EnhancedEntityInfo> entities, List<EnhancedRelationInfo> relationships, List<EntityNode> processedEntities) {
            this.entities = entities != null ? entities : Collections.emptyList();
            this.relationships = relationships != null ? relationships : Collections.emptyList();
            this.processedEntities = processedEntities != null ? processedEntities : Collections.emptyList();
        }
    }

    /**
     * Shutdown thread pool
     */
    public void shutdown() {
        executorService.shutdown();
    }
}