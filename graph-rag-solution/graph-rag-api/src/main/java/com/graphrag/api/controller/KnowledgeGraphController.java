package com.graphrag.api.controller;

import com.graphrag.core.service.EnhancedKnowledgeGraphService;
import com.graphrag.core.service.KnowledgeGraphService;
import com.graphrag.data.entity.DocumentNode;
import com.graphrag.data.service.DocumentService;
import com.graphrag.data.service.GraphService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Knowledge Graph Construction Controller
 * Provides LLM-based intelligent entity extraction and relationship recognition
 */
@RestController
@RequestMapping("/api/knowledge-graph")
@Tag(name = "Knowledge Graph", description = "Intelligent Knowledge Graph Construction API")
public class KnowledgeGraphController {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphController.class);

    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    @Autowired
    private EnhancedKnowledgeGraphService enhancedKnowledgeGraphService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private GraphService graphService;

    /**
     * Build knowledge graph from text (Enhanced version)
     */
    @PostMapping("/build-enhanced")
    @Operation(summary = "Build Enhanced Knowledge Graph from Text",
               description = "Use LLM to intelligently extract entities and relationships to build high-quality knowledge graph")
    public ResponseEntity<Map<String, Object>> buildEnhancedKnowledgeGraph(
            @RequestBody @Parameter(description = "Build Request") BuildKnowledgeGraphRequest request) {
        
        logger.info("Received enhanced knowledge graph build request, source: {}", request.getSource());
        
        try {
            // Create document object
            Metadata metadata = Metadata.from("source", request.getSource());
            if (request.getMetadata() != null) {
                request.getMetadata().forEach(metadata::add);
            }
            Document document = Document.from(request.getText(), metadata);

            // Build knowledge graph asynchronously
            CompletableFuture<DocumentNode> future = enhancedKnowledgeGraphService.buildEnhancedKnowledgeGraph(document);
            DocumentNode documentNode = future.get(); // Can be changed to async processing

            // Get build statistics
            Map<String, Object> stats = graphService.getDatabaseStats();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Enhanced knowledge graph build completed");
            response.put("document_id", documentNode.getId());
            response.put("document_title", documentNode.getTitle());
            response.put("stats", stats);
            response.put("enhanced_features", Map.of(
                "entity_validation", "enabled",
                "relationship_confidence", "enabled",
                "parallel_processing", "enabled",
                "vector_embedding", "enabled"
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Enhanced knowledge graph build failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Enhanced knowledge graph build failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Build knowledge graph from text (Standard version)
     */
    @PostMapping("/build")
    @Operation(summary = "Build Standard Knowledge Graph from Text",
               description = "Build knowledge graph using standard methods")
    public ResponseEntity<Map<String, Object>> buildKnowledgeGraph(
            @RequestBody @Parameter(description = "Build Request") BuildKnowledgeGraphRequest request) {
        
        logger.info("Received standard knowledge graph build request, source: {}", request.getSource());
        
        try {
            // Create document object
            Metadata metadata = Metadata.from("source", request.getSource());
            if (request.getMetadata() != null) {
                request.getMetadata().forEach(metadata::add);
            }
            Document document = Document.from(request.getText(), metadata);

            // Build knowledge graph
            knowledgeGraphService.buildKnowledgeGraphFromDocument(document);

            // Get build statistics
            Map<String, Object> stats = graphService.getDatabaseStats();

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Standard knowledge graph build completed");
            response.put("stats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Standard knowledge graph build failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Standard knowledge graph build failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get knowledge graph statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get Knowledge Graph Statistics",
               description = "View current node and relationship statistics in knowledge graph")
    public ResponseEntity<Map<String, Object>> getKnowledgeGraphStats() {
        try {
            Map<String, Object> stats = graphService.getDatabaseStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get statistics", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to get statistics: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Initialize knowledge graph database
     */
    @PostMapping("/initialize")
    @Operation(summary = "Initialize Knowledge Graph Database",
               description = "Create necessary constraints and indexes")
    public ResponseEntity<Map<String, Object>> initializeKnowledgeGraph() {
        try {
            graphService.initializeDatabase();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Knowledge graph database initialization completed");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Database initialization failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Database initialization failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Test entity extraction functionality
     */
    @PostMapping("/test-extraction")
    @Operation(summary = "Test Entity Extraction",
               description = "Test LLM entity extraction effectiveness")
    public ResponseEntity<Map<String, Object>> testEntityExtraction(
            @RequestBody @Parameter(description = "Test Text") TestExtractionRequest request) {
        
        logger.info("Received entity extraction test request");
        
        try {
            // Create test document
            Metadata metadata = Metadata.from("source", "test");
            Document document = Document.from(request.getText(), metadata);

            // Execute extraction test
            CompletableFuture<DocumentNode> future = enhancedKnowledgeGraphService.buildEnhancedKnowledgeGraph(document);
            DocumentNode documentNode = future.get();

            // Build test results
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Entity extraction test completed");
            response.put("document_id", documentNode.getId());
            response.put("extraction_method", "Enhanced LLM-based");
            response.put("features_used", Map.of(
                "json_parsing", "enabled",
                "entity_validation", "enabled",
                "confidence_filtering", "enabled",
                "vector_embedding", "enabled"
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Entity extraction test failed", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Entity extraction test failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Build Knowledge Graph Request Class
     */
    public static class BuildKnowledgeGraphRequest {
        private String text;
        private String source;
        private Map<String, String> metadata;

        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }

    /**
     * Test Extraction Request Class
     */
    public static class TestExtractionRequest {
        private String text;

        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}