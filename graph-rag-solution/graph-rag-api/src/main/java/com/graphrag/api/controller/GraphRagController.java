package com.graphrag.api.controller;

import com.graphrag.common.model.ApiResponse;
import com.graphrag.core.algorithm.*;
import com.graphrag.core.model.GraphRagRequest;
import com.graphrag.core.model.GraphRagResponse;
import com.graphrag.core.service.GraphRagRetrievalService;
import com.graphrag.core.service.KnowledgeGraphService;
import com.graphrag.data.service.DocumentService;
import com.graphrag.data.service.EntityService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 圖 RAG API 控制器
 */
@RestController
@RequestMapping("/api/v1/graph-rag")
@Tag(name = "Graph RAG API", description = "圖 RAG 檢索和生成介面")
@CrossOrigin(origins = "*")
public class GraphRagController {

    private static final Logger logger = LoggerFactory.getLogger(GraphRagController.class);

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

    /**
     * 圖 RAG 查詢介面
     */
    @PostMapping("/query")
    @Operation(summary = "圖 RAG 查詢", description = "基於知識圖譜的檢索增強生成")
    public ResponseEntity<ApiResponse<GraphRagResponse>> query(
            @RequestBody @Parameter(description = "查詢請求") GraphRagRequest request) {
        
        logger.info("收到圖 RAG 查詢請求: {}", request.getQuestion());
        
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
            
            logger.info("圖 RAG 查詢完成，耗時: {}ms", processingTime);
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            logger.error("圖 RAG 查詢失敗", e);
            return ResponseEntity.ok(ApiResponse.error("查詢失敗: " + e.getMessage()));
        }
    }

    /**
     * 異步圖 RAG 查詢介面
     */
    @PostMapping("/query/async")
    @Operation(summary = "異步圖 RAG 查詢", description = "異步執行圖 RAG 查詢")
    public ResponseEntity<ApiResponse<String>> queryAsync(
            @RequestBody @Parameter(description = "查詢請求") GraphRagRequest request) {
        
        logger.info("收到異步圖 RAG 查詢請求: {}", request.getQuestion());
        
        try {
            CompletableFuture<GraphRagResponse> future = CompletableFuture.supplyAsync(() -> {
                if ("hybrid".equals(request.getRetrievalMode())) {
                    return retrievalService.hybridRetrieve(request);
                } else {
                    return retrievalService.retrieve(request);
                }
            });
            
            // 這裡可以返回任務ID，客戶端可以通過任務ID查詢結果
            String taskId = "task_" + System.currentTimeMillis();
            
            return ResponseEntity.ok(ApiResponse.success(taskId));
            
        } catch (Exception e) {
            logger.error("異步圖 RAG 查詢失敗", e);
            return ResponseEntity.ok(ApiResponse.error("異步查詢失敗: " + e.getMessage()));
        }
    }

    /**
     * 查詢分析介面
     */
    @PostMapping("/analyze")
    @Operation(summary = "查詢分析", description = "分析查詢意圖和複雜度")
    public ResponseEntity<ApiResponse<QueryUnderstandingAlgorithm.QueryAnalysis>> analyzeQuery(
            @RequestParam @Parameter(description = "查詢文本") String query) {
        
        logger.info("收到查詢分析請求: {}", query);
        
        try {
            QueryUnderstandingAlgorithm.QueryAnalysis analysis = queryUnderstanding.analyzeQuery(query);
            return ResponseEntity.ok(ApiResponse.success(analysis));
            
        } catch (Exception e) {
            logger.error("查詢分析失敗", e);
            return ResponseEntity.ok(ApiResponse.error("查詢分析失敗: " + e.getMessage()));
        }
    }

    /**
     * 文檔上傳和知識圖譜構建介面
     */
    @PostMapping("/documents/upload")
    @Operation(summary = "上傳文檔", description = "上傳文檔並構建知識圖譜")
    public ResponseEntity<ApiResponse<String>> uploadDocument(
            @RequestParam("file") @Parameter(description = "文檔文件") MultipartFile file,
            @RequestParam(value = "source", required = false) @Parameter(description = "文檔來源") String source) {
        
        logger.info("收到文檔上傳請求，文件名: {}", file.getOriginalFilename());
        
        try {
            // 讀取文件內容
            String content = new String(file.getBytes(), "UTF-8");
            String documentSource = source != null ? source : file.getOriginalFilename();
            
            // 創建文檔並構建知識圖譜
            Metadata metadata = new Metadata();
            metadata.add("source", documentSource);
            Document document = Document.from(content, metadata);
            knowledgeGraphService.buildKnowledgeGraphFromDocument(document);
            
            return ResponseEntity.ok(ApiResponse.success("文檔上傳和知識圖譜構建成功"));
            
        } catch (Exception e) {
            logger.error("文檔上傳失敗", e);
            return ResponseEntity.ok(ApiResponse.error("文檔上傳失敗: " + e.getMessage()));
        }
    }

    /**
     * 批量文檔上傳介面
     */
    @PostMapping("/documents/batch-upload")
    @Operation(summary = "批量上傳文檔", description = "批量上傳文檔並構建知識圖譜")
    public ResponseEntity<ApiResponse<String>> batchUploadDocuments(
            @RequestParam("files") @Parameter(description = "文檔文件列表") MultipartFile[] files,
            @RequestParam(value = "source", required = false) @Parameter(description = "文檔來源") String source) {
        
        logger.info("收到批量文檔上傳請求，文件數量: {}", files.length);
        
        try {
            int successCount = 0;
            int failCount = 0;
            
            for (MultipartFile file : files) {
                try {
                    String content = new String(file.getBytes(), "UTF-8");
                    String documentSource = source != null ? source : file.getOriginalFilename();
                    
                    Metadata metadata = new Metadata();
                    metadata.add("source", documentSource);
                    Document document = Document.from(content, metadata);
                    knowledgeGraphService.buildKnowledgeGraphFromDocument(document);
                    
                    successCount++;
                } catch (Exception e) {
                    logger.error("處理文件失敗: {}", file.getOriginalFilename(), e);
                    failCount++;
                }
            }
            
            String message = String.format("批量上傳完成，成功: %d, 失敗: %d", successCount, failCount);
            return ResponseEntity.ok(ApiResponse.success(message));
            
        } catch (Exception e) {
            logger.error("批量文檔上傳失敗", e);
            return ResponseEntity.ok(ApiResponse.error("批量上傳失敗: " + e.getMessage()));
        }
    }

    /**
     * 獲取數據庫統計信息
     */
    @GetMapping("/stats")
    @Operation(summary = "獲取統計信息", description = "獲取知識圖譜統計信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        logger.info("收到統計信息查詢請求");
        
        try {
            Map<String, Object> stats = graphService.getDatabaseStats();
            return ResponseEntity.ok(ApiResponse.success(stats));
            
        } catch (Exception e) {
            logger.error("獲取統計信息失敗", e);
            return ResponseEntity.ok(ApiResponse.error("獲取統計信息失敗: " + e.getMessage()));
        }
    }

    /**
     * 健康檢查介面
     */
    @GetMapping("/health")
    @Operation(summary = "健康檢查", description = "檢查服務健康狀態")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        try {
            // 簡單的健康檢查
            long documentCount = documentService.findAll().size();
            long entityCount = entityService.findAll().size();
            
            String status = String.format("服務正常，文檔數量: %d, 實體數量: %d", documentCount, entityCount);
            return ResponseEntity.ok(ApiResponse.success(status));
            
        } catch (Exception e) {
            logger.error("健康檢查失敗", e);
            return ResponseEntity.ok(ApiResponse.error("服務異常: " + e.getMessage()));
        }
    }

    /**
     * 清空知識圖譜
     */
    @DeleteMapping("/clear")
    @Operation(summary = "清空知識圖譜", description = "清空所有知識圖譜數據")
    public ResponseEntity<ApiResponse<String>> clearKnowledgeGraph() {
        logger.warn("收到清空知識圖譜請求");
        
        try {
            String cypher = "MATCH (n) DETACH DELETE n";
            graphService.executeCypherWrite(cypher, Map.of());
            
            return ResponseEntity.ok(ApiResponse.success("知識圖譜已清空"));
            
        } catch (Exception e) {
            logger.error("清空知識圖譜失敗", e);
            return ResponseEntity.ok(ApiResponse.error("清空失敗: " + e.getMessage()));
        }
    }

    /**
     * 獲取相關實體
     */
    @GetMapping("/entities/{entityName}/related")
    @Operation(summary = "獲取相關實體", description = "獲取與指定實體相關的實體")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRelatedEntities(
            @PathVariable @Parameter(description = "實體名稱") String entityName,
            @RequestParam(value = "maxHops", defaultValue = "2") @Parameter(description = "最大跳數") int maxHops,
            @RequestParam(value = "maxResults", defaultValue = "20") @Parameter(description = "最大結果數") int maxResults) {
        
        logger.info("收到相關實體查詢請求，實體: {}", entityName);
        
        try {
            GraphTraversalAlgorithm traversal = new GraphTraversalAlgorithm();
            List<Map<String, Object>> relatedEntities = traversal.multiHopEntityRetrieval(
                    entityName, maxHops, maxResults);
            
            return ResponseEntity.ok(ApiResponse.success(relatedEntities));
            
        } catch (Exception e) {
            logger.error("獲取相關實體失敗", e);
            return ResponseEntity.ok(ApiResponse.error("獲取相關實體失敗: " + e.getMessage()));
        }
    }
}
