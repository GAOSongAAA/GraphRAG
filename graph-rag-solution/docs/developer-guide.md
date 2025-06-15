# Graph RAG 开发者指南

## 开发环境搭建

### 本地开发环境

1. **安装必要软件**
```bash
# 安装 Java 17
sudo apt update
sudo apt install openjdk-17-jdk

# 安装 Maven
sudo apt install maven

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# 安装 Node.js（用于前端开发）
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
```

2. **启动依赖服务**
```bash
# 启动 Neo4j
docker run -d --name neo4j-dev \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/dev123 \
  neo4j:5.15.0

# 启动 Redis
docker run -d --name redis-dev \
  -p 6379:6379 \
  redis:7.2-alpine
```

3. **克隆和构建项目**
```bash
git clone https://github.com/your-repo/graph-rag-solution.git
cd graph-rag-solution
mvn clean install
```

4. **配置开发环境变量**
```bash
export OPENAI_API_KEY="your-dev-api-key"
export NEO4J_URI="bolt://localhost:7687"
export NEO4J_USERNAME="neo4j"
export NEO4J_PASSWORD="dev123"
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
```

5. **启动应用**
```bash
cd graph-rag-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### IDE 配置

**IntelliJ IDEA 配置：**

1. 导入项目：File → Open → 选择项目根目录
2. 配置 JDK：File → Project Structure → Project → Project SDK
3. 配置 Maven：File → Settings → Build → Build Tools → Maven
4. 安装推荐插件：
   - Spring Boot
   - Neo4j
   - Docker
   - Swagger

**VS Code 配置：**

1. 安装扩展：
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Docker
   - REST Client

2. 配置 settings.json：
```json
{
  "java.home": "/usr/lib/jvm/java-17-openjdk-amd64",
  "maven.executable.path": "/usr/bin/mvn",
  "spring-boot.ls.java.home": "/usr/lib/jvm/java-17-openjdk-amd64"
}
```

## 项目结构详解

```
graph-rag-solution/
├── graph-rag-common/          # 通用组件模块
│   ├── src/main/java/
│   │   └── com/graphrag/common/
│   │       ├── config/        # 配置类
│   │       ├── exception/     # 异常定义
│   │       ├── model/         # 通用模型
│   │       └── util/          # 工具类
│   └── pom.xml
├── graph-rag-data/            # 数据访问模块
│   ├── src/main/java/
│   │   └── com/graphrag/data/
│   │       ├── config/        # 数据库配置
│   │       ├── entity/        # 实体类
│   │       ├── repository/    # 数据访问层
│   │       └── service/       # 数据服务层
│   └── pom.xml
├── graph-rag-core/            # 核心算法模块
│   ├── src/main/java/
│   │   └── com/graphrag/core/
│   │       ├── algorithm/     # 核心算法
│   │       ├── model/         # 核心模型
│   │       └── service/       # 核心服务
│   └── pom.xml
├── graph-rag-api/             # API 服务模块
│   ├── src/main/java/
│   │   └── com/graphrag/api/
│   │       ├── config/        # API 配置
│   │       ├── controller/    # 控制器
│   │       ├── interceptor/   # 拦截器
│   │       └── service/       # API 服务
│   └── pom.xml
├── docs/                      # 文档目录
├── docker-compose.yml         # Docker 编排文件
├── Dockerfile                 # Docker 镜像构建文件
└── pom.xml                    # 父级 POM 文件
```

## 核心开发流程

### 添加新的实体类型

1. **定义实体类**
```java
@Node("CustomEntity")
public class CustomEntityNode {
    @Id
    @GeneratedValue
    private Long id;
    
    @Property("name")
    private String name;
    
    @Property("type")
    private String type;
    
    @Property("customProperty")
    private String customProperty;
    
    // 构造函数、getter、setter
}
```

2. **创建仓库接口**
```java
@Repository
public interface CustomEntityRepository extends Neo4jRepository<CustomEntityNode, Long> {
    
    @Query("MATCH (e:CustomEntity) WHERE e.name CONTAINS $name RETURN e")
    List<CustomEntityNode> findByNameContaining(@Param("name") String name);
    
    @Query("MATCH (e:CustomEntity) WHERE e.type = $type RETURN e")
    List<CustomEntityNode> findByType(@Param("type") String type);
}
```

3. **实现服务类**
```java
@Service
public class CustomEntityService {
    
    @Autowired
    private CustomEntityRepository repository;
    
    public CustomEntityNode save(CustomEntityNode entity) {
        return repository.save(entity);
    }
    
    public List<CustomEntityNode> findByType(String type) {
        return repository.findByType(type);
    }
}
```

### 添加新的检索算法

1. **实现算法类**
```java
@Component
public class CustomRetrievalAlgorithm {
    
    @Autowired
    private Neo4jTemplate neo4jTemplate;
    
    public List<ScoredResult<DocumentNode>> customRetrieve(String query, int maxResults) {
        // 实现自定义检索逻辑
        String cypher = "MATCH (d:Document) WHERE d.content CONTAINS $query " +
                       "RETURN d, score ORDER BY score DESC LIMIT $limit";
        
        Map<String, Object> params = Map.of(
            "query", query,
            "limit", maxResults
        );
        
        return neo4jTemplate.findAll(cypher, params, DocumentNode.class)
                .stream()
                .map(doc -> new ScoredResult<>(doc, calculateScore(doc, query)))
                .collect(Collectors.toList());
    }
    
    private double calculateScore(DocumentNode doc, String query) {
        // 实现评分逻辑
        return 0.5;
    }
}
```

2. **集成到检索服务**
```java
@Service
public class GraphRagRetrievalService {
    
    @Autowired
    private CustomRetrievalAlgorithm customAlgorithm;
    
    public GraphRagResponse customRetrieve(GraphRagRequest request) {
        List<ScoredResult<DocumentNode>> results = 
            customAlgorithm.customRetrieve(request.getQuestion(), request.getMaxDocuments());
        
        // 处理结果并生成响应
        return buildResponse(request, results);
    }
}
```

### 添加新的 API 接口

1. **定义请求/响应模型**
```java
public class CustomQueryRequest {
    private String query;
    private String customParameter;
    
    // 构造函数、getter、setter
}

public class CustomQueryResponse {
    private String result;
    private List<String> suggestions;
    
    // 构造函数、getter、setter
}
```

2. **实现控制器**
```java
@RestController
@RequestMapping("/api/v1/custom")
public class CustomController {
    
    @Autowired
    private CustomService customService;
    
    @PostMapping("/query")
    @Operation(summary = "自定义查询", description = "执行自定义查询逻辑")
    public ResponseEntity<ApiResponse<CustomQueryResponse>> customQuery(
            @RequestBody CustomQueryRequest request) {
        
        try {
            CustomQueryResponse response = customService.processCustomQuery(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
}
```

## 测试开发

### 单元测试

```java
@ExtendWith(MockitoExtension.class)
class CustomServiceTest {
    
    @Mock
    private CustomEntityRepository repository;
    
    @InjectMocks
    private CustomEntityService service;
    
    @Test
    void testFindByType() {
        // 准备测试数据
        CustomEntityNode entity = new CustomEntityNode("test", "type1");
        when(repository.findByType("type1")).thenReturn(Arrays.asList(entity));
        
        // 执行测试
        List<CustomEntityNode> results = service.findByType("type1");
        
        // 验证结果
        assertEquals(1, results.size());
        assertEquals("test", results.get(0).getName());
        
        // 验证 Mock 调用
        verify(repository).findByType("type1");
    }
}
```

### 集成测试

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class CustomControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testCustomQuery() {
        CustomQueryRequest request = new CustomQueryRequest("test query", "param");
        
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/custom/query", request, ApiResponse.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
    }
}
```

### 性能测试

```java
@Test
void testQueryPerformance() {
    int iterations = 1000;
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < iterations; i++) {
        service.performQuery("test query " + i);
    }
    
    long endTime = System.currentTimeMillis();
    long averageTime = (endTime - startTime) / iterations;
    
    assertTrue(averageTime < 100, "平均查询时间应小于100ms");
}
```

## 代码规范

### Java 编码规范

1. **命名规范**
   - 类名：PascalCase（如 `GraphRagService`）
   - 方法名：camelCase（如 `retrieveDocuments`）
   - 常量：UPPER_SNAKE_CASE（如 `MAX_RETRY_COUNT`）
   - 包名：小写，用点分隔（如 `com.graphrag.core`）

2. **注释规范**
```java
/**
 * 图 RAG 检索服务
 * 
 * 提供基于知识图谱的检索增强生成功能，支持多种检索模式
 * 和智能答案生成。
 * 
 * @author Manus AI
 * @version 1.0.0
 * @since 2025-06-15
 */
@Service
public class GraphRagRetrievalService {
    
    /**
     * 执行混合检索
     * 
     * @param request 查询请求，包含查询文本和参数
     * @return 查询响应，包含答案和相关文档
     * @throws GraphRagException 当检索失败时抛出
     */
    public GraphRagResponse hybridRetrieve(GraphRagRequest request) {
        // 方法实现
    }
}
```

3. **异常处理**
```java
public class GraphRagService {
    
    public void processDocument(String content) {
        try {
            // 处理逻辑
            documentProcessor.process(content);
        } catch (ProcessingException e) {
            logger.error("文档处理失败: {}", content, e);
            throw new GraphRagException("文档处理失败", e);
        } catch (Exception e) {
            logger.error("未知错误: {}", content, e);
            throw new GraphRagException("系统内部错误", e);
        }
    }
}
```

### Git 工作流

1. **分支策略**
   - `main`: 主分支，用于生产环境
   - `develop`: 开发分支，用于集成测试
   - `feature/*`: 功能分支，用于新功能开发
   - `hotfix/*`: 热修复分支，用于紧急修复

2. **提交规范**
```bash
# 提交格式
<type>(<scope>): <subject>

<body>

<footer>

# 示例
feat(core): 添加新的图遍历算法

实现了基于 A* 算法的最短路径查找功能，支持权重边
和双向搜索优化。

Closes #123
```

3. **提交类型**
   - `feat`: 新功能
   - `fix`: 修复 bug
   - `docs`: 文档更新
   - `style`: 代码格式调整
   - `refactor`: 代码重构
   - `test`: 测试相关
   - `chore`: 构建或工具相关

## 调试技巧

### 日志调试

```java
@Slf4j
@Service
public class GraphRagService {
    
    public void processQuery(String query) {
        log.debug("开始处理查询: {}", query);
        
        try {
            // 处理逻辑
            log.info("查询处理成功: {}", query);
        } catch (Exception e) {
            log.error("查询处理失败: {}", query, e);
            throw e;
        }
    }
}
```

### 性能分析

```java
@Component
public class PerformanceMonitor {
    
    @Around("@annotation(Monitored)")
    public Object monitor(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            
            log.info("方法 {} 执行时间: {}ms", 
                joinPoint.getSignature().getName(), 
                endTime - startTime);
            
            return result;
        } catch (Exception e) {
            log.error("方法 {} 执行失败", joinPoint.getSignature().getName(), e);
            throw e;
        }
    }
}
```

### 数据库调试

```java
// 启用 Neo4j 查询日志
logging:
  level:
    org.neo4j.driver: DEBUG
    org.springframework.data.neo4j: DEBUG

// 查看慢查询
@Query("PROFILE MATCH (n:Document) WHERE n.content CONTAINS $query RETURN n")
List<DocumentNode> findDocumentsWithProfile(@Param("query") String query);
```

## 扩展开发

### 自定义嵌入模型

```java
@Component
public class CustomEmbeddingModel implements EmbeddingModel {
    
    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        // 实现自定义嵌入逻辑
        List<Embedding> embeddings = textSegments.stream()
            .map(this::embed)
            .collect(Collectors.toList());
        
        return Response.from(embeddings);
    }
    
    private Embedding embed(TextSegment textSegment) {
        // 调用自定义嵌入服务
        float[] vector = customEmbeddingService.embed(textSegment.text());
        return Embedding.from(vector);
    }
}
```

### 自定义图算法

```java
@Component
public class CustomGraphAlgorithm {
    
    @Autowired
    private Driver neo4jDriver;
    
    public List<String> customCommunityDetection(String startNode) {
        try (Session session = neo4jDriver.session()) {
            String cypher = """
                MATCH (start:Entity {name: $startNode})
                CALL gds.louvain.stream('myGraph')
                YIELD nodeId, communityId
                MATCH (n) WHERE id(n) = nodeId
                RETURN n.name as name, communityId
                """;
            
            return session.run(cypher, Map.of("startNode", startNode))
                .list(record -> record.get("name").asString());
        }
    }
}
```

### 插件开发

```java
@Component
public class CustomPlugin implements GraphRagPlugin {
    
    @Override
    public String getName() {
        return "custom-plugin";
    }
    
    @Override
    public void initialize(PluginContext context) {
        // 插件初始化逻辑
    }
    
    @Override
    public PluginResult process(PluginRequest request) {
        // 插件处理逻辑
        return new PluginResult(processedData);
    }
}

