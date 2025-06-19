package com.graphrag.api.controller;

import com.graphrag.api.controller.registry.AsyncTaskRegistry;
import com.graphrag.common.model.ApiResponse;
import com.graphrag.core.algorithm.*;
import com.graphrag.core.model.GraphRagRequest;
import com.graphrag.core.model.GraphRagResponse;
import com.graphrag.core.service.GraphRagRetrievalService;
import com.graphrag.core.service.KnowledgeGraphService;
import com.graphrag.data.service.DocumentService;
import com.graphrag.data.service.EntityService;
import com.graphrag.data.service.GraphService;
import com.graphrag.core.service.DocumentLoaderService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;


/**
 * Graph RAG API Controller
 */
@RestController
@RequestMapping("/")
@Tag(name = "Graph RAG API", description = "Graph RAG Retrieval and Generation Interface")
@CrossOrigin(origins = "*")
public class GraphRagController {

    private static final Logger logger = LoggerFactory.getLogger(GraphRagController.class);

    private final AsyncTaskRegistry taskRegistry;

    @Autowired
    private GraphRagRetrievalService retrievalService;

    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    @Autowired
    private QueryUnderstandingAlgorithm queryUnderstanding;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private EntityService entityService;

    @Autowired
    private GraphService graphService;

    @Autowired
    private DocumentLoaderService documentLoaderService;

    @Autowired
    private GraphTraversalAlgorithm traversal;

    @Autowired
    public GraphRagController(AsyncTaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
    }

    /**
     * Graph RAG Query Interface
     */
    @PostMapping("/query")
    @Operation(summary = "Graph RAG Query", description = "Knowledge Graph Based Retrieval Augmented Generation")
    public ResponseEntity<ApiResponse<GraphRagResponse>> query(
            @RequestBody @Parameter(description = "Query Request") GraphRagRequest request) {

        logger.info("Received Graph RAG query request: {}", request.getQuestion());

        try {
            long startTime = System.currentTimeMillis();

            GraphRagResponse response;
            if ("hybrid".equals(request.getRetrievalMode())) {
                response = retrievalService.hybridRetrieve(request);
            } else {
                response = retrievalService.retrieve(request);
            }

            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTimeMs(processingTime);

            logger.info("Graph RAG query completed, time taken: {}ms", processingTime);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            logger.error("Graph RAG query failed", e);
            return ResponseEntity.ok(ApiResponse.error("Query failed: " + e.getMessage()));
        }
    }

    /**
     * Async Graph RAG Query Interface
     */
    @PostMapping("/query/async")
    @Operation(summary = "提交异步查询")
    public ResponseEntity<ApiResponse<String>> submit(@RequestBody GraphRagRequest req) {
        logger.info("Async query request: {}", req.getQuestion());

        String id = taskRegistry.submit(() -> {
            if ("hybrid".equalsIgnoreCase(req.getRetrievalMode())) {
                return retrievalService.hybridRetrieve(req);
            }
            return retrievalService.retrieve(req);
        });
        // 202 + 任务查询 URL
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                                                  .path("/{id}")
                                                  .build(id);
        return ResponseEntity
                .accepted()
                .location(location)
                .body(ApiResponse.success(id));
    }

    @GetMapping("/query/async/{id}")
    @Operation(summary = "查询异步结果")
    public ResponseEntity<ApiResponse<?>> result(@PathVariable("id") String id) {
        if (taskRegistry.isRunning(id)) {
            return ResponseEntity.ok(ApiResponse.success("running"));
        }
        return taskRegistry.get(id)
                .<ResponseEntity<ApiResponse<?>>>map(res -> ResponseEntity.ok(ApiResponse.success(res)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.error("task not found")));
    }

    /**
     * Query Analysis Interface
     */
    @PostMapping("/analyze")
    @Operation(summary = "Query Analysis", description = "Analyze Query Intent and Complexity")
    public ResponseEntity<ApiResponse<QueryUnderstandingAlgorithm.QueryAnalysis>> analyzeQuery(
        @RequestParam("query") @Parameter(description = "Query Text") String query) {

        logger.info("Received query analysis request: {}", query);

        try {
            QueryUnderstandingAlgorithm.QueryAnalysis analysis = queryUnderstanding.analyzeQuery(query);
            return ResponseEntity.ok(ApiResponse.success(analysis));

        } catch (Exception e) {
            logger.error("Query analysis failed", e);
            return ResponseEntity.ok(ApiResponse.error("Analysis failed: " + e.getMessage()));
        }
    }

    /**
     * Document Upload and Knowledge Graph Construction Interface
     */
    @PostMapping("/documents/upload")
    @Operation(summary = "Upload Document", description = "Upload Document and Build Knowledge Graph")
    public ResponseEntity<ApiResponse<String>> uploadDocument(
            @RequestParam("file") @Parameter(description = "Document File") MultipartFile file,
            @RequestParam(value = "source", required = false) @Parameter(description = "Document Source") String source) {

        logger.info("Received document upload request, filename: {}", file.getOriginalFilename());

        try {
            Document document = documentLoaderService.loadFromMultipartFile(file);
            if (source != null) {
                document.metadata().add("source", source);
            }

            knowledgeGraphService.buildKnowledgeGraphFromDocument(document);

            return ResponseEntity
                    .ok(ApiResponse.success("Document upload and knowledge graph construction successful"));

        } catch (Exception e) {
            logger.error("Document upload failed", e);
            return ResponseEntity.ok(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }

    /**
     * Batch Document Upload Interface
     */
    @PostMapping("/documents/batch-upload")
    @Operation(summary = "Batch Upload Documents", description = "Batch Upload Documents and Build Knowledge Graph")
    public ResponseEntity<ApiResponse<String>> batchUploadDocuments(
            @RequestParam("files") @Parameter(description = "Document Files List") MultipartFile[] files,
            @RequestParam(value = "source", required = false) @Parameter(description = "Document Source") String source) {

        logger.info("Received batch document upload request, file count: {}", files.length);

        try {
            int successCount = 0;
            int failCount = 0;

            for (MultipartFile file : files) {
                try {
                    Document document = documentLoaderService.loadFromMultipartFile(file);
                    if (source != null) {
                        document.metadata().add("source", source);
                    }
                    knowledgeGraphService.buildKnowledgeGraphFromDocument(document);

                    successCount++;
                } catch (Exception e) {
                    logger.error("Failed to process file: {}", file.getOriginalFilename(), e);
                    failCount++;
                }
            }

            String message = String.format("Batch upload completed, success: %d, failed: %d", successCount, failCount);
            return ResponseEntity.ok(ApiResponse.success(message));

        } catch (Exception e) {
            logger.error("Batch document upload failed", e);
            return ResponseEntity.ok(ApiResponse.error("Batch upload failed: " + e.getMessage()));
        }
    }

    /**
     * Get Database Statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get Statistics", description = "Get Knowledge Graph Statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        logger.info("Received statistics query request");

        try {
            Map<String, Object> stats = graphService.getDatabaseStats();
            return ResponseEntity.ok(ApiResponse.success(stats));

        } catch (Exception e) {
            logger.error("Failed to get statistics", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to get statistics: " + e.getMessage()));
        }
    }

    /**
     * Health Check Interface
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check Service Health Status")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        try {
            // Simple health check
            long documentCount = documentService.findAll().size();
            long entityCount = entityService.findAll().size();

            String status = String.format("Service healthy, document count: %d, entity count: %d", documentCount,
                    entityCount);
            return ResponseEntity.ok(ApiResponse.success(status));

        } catch (Exception e) {
            logger.error("Health check failed", e);
            return ResponseEntity.ok(ApiResponse.error("Service unhealthy: " + e.getMessage()));
        }
    }

    /**
     * Clear Knowledge Graph
     */
    @DeleteMapping("/clear")
    @Operation(summary = "Clear Knowledge Graph", description = "Clear All Knowledge Graph Data")
    public ResponseEntity<ApiResponse<String>> clearKnowledgeGraph() {
        logger.warn("Received clear knowledge graph request");

        try {
            String cypher = "MATCH (n) DETACH DELETE n";
            graphService.executeCypherWrite(cypher, Map.of());

            return ResponseEntity.ok(ApiResponse.success("Knowledge graph cleared"));

        } catch (Exception e) {
            logger.error("Failed to clear knowledge graph", e);
            return ResponseEntity.ok(ApiResponse.error("Clear failed: " + e.getMessage()));
        }
    }

    /**
     * Get Related Entities
     */
    @GetMapping("/entities/{entityName}/related")
    @Operation(summary = "Get Related Entities", description = "Get Entities Related to Specified Entity")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRelatedEntities(
        @PathVariable("entityName") @Parameter(description = "Entity Name") String entityName,
        @RequestParam("maxHops") @Parameter(description = "Maximum Hops") int maxHops,
        @RequestParam("maxResults") @Parameter(description = "Maximum Results") int maxResults) {

        logger.info("Received related entities query request, entity: {}", entityName);

        try {
            List<Map<String, Object>> relatedEntities = traversal.multiHopEntityRetrieval(
                    entityName, maxHops, maxResults);

            return ResponseEntity.ok(ApiResponse.success(relatedEntities));

        } catch (Exception e) {
            logger.error("Failed to get related entities", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to get related entities: " + e.getMessage()));
        }
    }
}
