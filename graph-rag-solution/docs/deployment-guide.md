# Graph RAG 部署运维手册

## 快速开始

### 环境要求

- **操作系统：** Linux (Ubuntu 20.04+) / macOS / Windows 10+
- **Java：** OpenJDK 17 或更高版本
- **内存：** 最小 8GB，推荐 16GB+
- **存储：** 最小 50GB 可用空间
- **网络：** 稳定的互联网连接（用于访问 LLM 服务）

### 一键部署脚本

```bash
#!/bin/bash
# Graph RAG 一键部署脚本

echo "开始部署 Graph RAG 系统..."

# 检查 Docker 环境
if ! command -v docker &> /dev/null; then
    echo "安装 Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
fi

if ! command -v docker-compose &> /dev/null; then
    echo "安装 Docker Compose..."
    curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
fi

# 设置环境变量
export OPENAI_API_KEY="your-openai-api-key"
export OPENAI_BASE_URL="https://api.openai.com/v1"

# 启动服务
docker-compose up -d

echo "等待服务启动..."
sleep 60

# 健康检查
curl -f http://localhost:8080/api/v1/graph-rag/health

echo "Graph RAG 系统部署完成！"
echo "访问地址: http://localhost:8080"
echo "API 文档: http://localhost:8080/swagger-ui.html"
```

### Docker Compose 部署

1. **下载项目文件**
```bash
wget https://github.com/your-repo/graph-rag-solution/archive/main.zip
unzip main.zip
cd graph-rag-solution
```

2. **配置环境变量**
```bash
cp .env.example .env
# 编辑 .env 文件，设置必要的环境变量
vim .env
```

3. **启动服务**
```bash
docker-compose up -d
```

4. **验证部署**
```bash
# 检查服务状态
docker-compose ps

# 查看日志
docker-compose logs -f graph-rag-app

# 健康检查
curl http://localhost:8080/api/v1/graph-rag/health
```

## 配置说明

### 应用配置

主要配置文件位于 `application.yml`：

```yaml
# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /

# Spring 配置
spring:
  profiles:
    active: prod
  
  # Neo4j 配置
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: ${NEO4J_PASSWORD:password123}
    
  # Redis 配置
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5

# LangChain4j 配置
langchain4j:
  openai:
    api-key: ${OPENAI_API_KEY}
    base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
    chat-model:
      model-name: gpt-3.5-turbo
      temperature: 0.7
      max-tokens: 2000
    embedding-model:
      model-name: text-embedding-ada-002

# 系统配置
graph-rag:
  # 向量检索配置
  vector:
    similarity-threshold: 0.7
    max-results: 10
    
  # 图遍历配置
  graph:
    max-hops: 3
    max-nodes: 100
    
  # 缓存配置
  cache:
    ttl: 3600
    max-size: 10000
```

### 环境变量

```bash
# 必需的环境变量
OPENAI_API_KEY=your-openai-api-key
NEO4J_PASSWORD=your-neo4j-password
REDIS_PASSWORD=your-redis-password

# 可选的环境变量
OPENAI_BASE_URL=https://api.openai.com/v1
JAVA_OPTS=-Xmx4g -Xms2g
SPRING_PROFILES_ACTIVE=prod
```

## 监控和日志

### Prometheus 监控

系统集成了 Prometheus 监控，主要指标包括：

- **查询指标：** 查询次数、成功率、响应时间
- **系统指标：** CPU 使用率、内存使用率、磁盘使用率
- **数据库指标：** 连接数、查询性能、存储使用
- **缓存指标：** 命中率、内存使用、连接数

访问 Prometheus：http://localhost:9090

### Grafana 可视化

预配置的 Grafana 仪表板包括：

- **系统概览：** 整体系统状态和关键指标
- **查询分析：** 查询性能和用户行为分析
- **资源监控：** 系统资源使用情况
- **错误分析：** 错误统计和趋势分析

访问 Grafana：http://localhost:3000
默认账号：admin/admin123

### 日志管理

系统使用结构化日志，支持多种日志级别：

```yaml
# 日志配置
logging:
  level:
    com.graphrag: INFO
    org.neo4j: WARN
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/graph-rag.log
    max-size: 100MB
    max-history: 30
```

## 性能优化

### JVM 优化

```bash
# 生产环境 JVM 参数
JAVA_OPTS="-Xmx8g -Xms4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Djava.awt.headless=true \
  -Dfile.encoding=UTF-8"
```

### Neo4j 优化

```conf
# neo4j.conf 关键配置
dbms.memory.heap.initial_size=2G
dbms.memory.heap.max_size=4G
dbms.memory.pagecache.size=2G

# 查询优化
cypher.default_language_version=5
cypher.hints_error=true
cypher.lenient_create_relationship=false

# 并发配置
dbms.threads.worker_count=8
dbms.connector.bolt.thread_pool_min_size=5
dbms.connector.bolt.thread_pool_max_size=400
```

### Redis 优化

```conf
# redis.conf 关键配置
maxmemory 2gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000

# 网络优化
tcp-keepalive 300
timeout 0
tcp-backlog 511
```

## 故障排查

### 常见问题

**1. 服务启动失败**
```bash
# 检查端口占用
netstat -tlnp | grep 8080

# 检查 Java 版本
java -version

# 查看启动日志
docker-compose logs graph-rag-app
```

**2. 数据库连接失败**
```bash
# 检查 Neo4j 状态
docker-compose logs neo4j

# 测试连接
cypher-shell -a bolt://localhost:7687 -u neo4j -p password123
```

**3. 查询性能慢**
```bash
# 检查系统资源
top
free -h
df -h

# 查看慢查询日志
grep "slow query" logs/graph-rag.log
```

### 诊断工具

**健康检查脚本：**
```bash
#!/bin/bash
echo "=== Graph RAG 系统诊断 ==="

# 检查服务状态
echo "1. 检查服务状态..."
curl -s http://localhost:8080/api/v1/graph-rag/health | jq .

# 检查数据库
echo "2. 检查数据库..."
docker exec graph-rag-neo4j cypher-shell -u neo4j -p password123 "MATCH (n) RETURN count(n) as nodeCount"

# 检查缓存
echo "3. 检查缓存..."
docker exec graph-rag-redis redis-cli info memory

# 检查磁盘空间
echo "4. 检查磁盘空间..."
df -h

echo "=== 诊断完成 ==="
```

## 备份和恢复

### 数据备份

```bash
#!/bin/bash
# 备份脚本
BACKUP_DIR="/backup/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

# 备份 Neo4j 数据
docker exec graph-rag-neo4j neo4j-admin dump --database=neo4j --to=/tmp/neo4j-backup.dump
docker cp graph-rag-neo4j:/tmp/neo4j-backup.dump $BACKUP_DIR/

# 备份 Redis 数据
docker exec graph-rag-redis redis-cli BGSAVE
docker cp graph-rag-redis:/data/dump.rdb $BACKUP_DIR/

# 备份配置文件
cp -r config/ $BACKUP_DIR/

echo "备份完成: $BACKUP_DIR"
```

### 数据恢复

```bash
#!/bin/bash
# 恢复脚本
BACKUP_DIR=$1

if [ -z "$BACKUP_DIR" ]; then
    echo "请指定备份目录"
    exit 1
fi

# 停止服务
docker-compose stop

# 恢复 Neo4j 数据
docker cp $BACKUP_DIR/neo4j-backup.dump graph-rag-neo4j:/tmp/
docker exec graph-rag-neo4j neo4j-admin load --database=neo4j --from=/tmp/neo4j-backup.dump --force

# 恢复 Redis 数据
docker cp $BACKUP_DIR/dump.rdb graph-rag-redis:/data/

# 启动服务
docker-compose start

echo "恢复完成"
```

## 安全配置

### SSL/TLS 配置

```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: graph-rag
  port: 8443
```

### 防火墙配置

```bash
# UFW 防火墙规则
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw allow 8080/tcp  # 应用端口（内网）
ufw deny 7474/tcp   # Neo4j HTTP（禁止外网访问）
ufw deny 7687/tcp   # Neo4j Bolt（禁止外网访问）
ufw deny 6379/tcp   # Redis（禁止外网访问）
ufw enable
```

### 访问控制

```yaml
# 安全配置
security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400
  rate-limit:
    enabled: true
    requests-per-minute: 100
  cors:
    allowed-origins: 
      - "https://your-domain.com"
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
```

