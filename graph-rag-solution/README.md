# Graph RAG 工业级解决方案

**基于 Java、LangChain4j 和 Neo4j 的大规模图 RAG 系统**

## 🚀 快速开始

### 一键部署

```bash
# 下载并解压项目
wget https://github.com/your-repo/graph-rag-solution/archive/main.zip
unzip main.zip
cd graph-rag-solution

# 一键启动（自动安装依赖并启动服务）
./start.sh
```

### 手动部署

```bash
# 1. 配置环境变量
cp .env.example .env
vim .env  # 设置 OPENAI_API_KEY

# 2. 启动服务
docker-compose up -d

# 3. 验证部署
curl http://localhost:8080/api/v1/graph-rag/health
```

## 📋 系统要求

- **操作系统：** Linux (Ubuntu 20.04+) / macOS / Windows 10+
- **内存：** 最小 8GB，推荐 16GB+
- **存储：** 最小 50GB 可用空间
- **网络：** 稳定的互联网连接

## 🏗️ 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   前端应用      │    │   API 网关      │    │   负载均衡      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Graph RAG API  │    │  知识图谱构建   │    │   向量化服务    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Neo4j 图库    │    │   Redis 缓存    │    │   监控告警      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔧 核心功能

### 智能查询
- 🧠 多模态检索（向量 + 图遍历 + 全文搜索）
- 🔍 智能查询理解和意图识别
- 💡 多跳推理和关系分析
- 📝 高质量答案生成

### 知识图谱
- 📚 自动文档解析和实体抽取
- 🕸️ 智能关系识别和图构建
- 🔗 实体链接和消歧
- 📊 图算法和分析

### 企业级特性
- 🚀 高性能和可扩展性
- 🔒 安全认证和权限控制
- 📈 全面监控和告警
- 🐳 容器化部署

## 📊 性能指标

| 指标 | 数值 |
|------|------|
| 查询响应时间 | < 2秒 |
| 并发查询数 | 1000+ QPS |
| 知识图谱规模 | 百万级节点 |
| 系统可用性 | 99.9% |

## 🛠️ 技术栈

- **后端框架：** Spring Boot 3.2
- **AI 框架：** LangChain4j 0.25.0
- **图数据库：** Neo4j 5.15.0
- **缓存：** Redis 7.2
- **监控：** Prometheus + Grafana
- **容器化：** Docker + Kubernetes

## 📖 文档目录

- [技术架构文档](docs/technical-documentation.md)
- [部署运维手册](docs/deployment-guide.md)
- [开发者指南](docs/developer-guide.md)
- [API 接口文档](http://localhost:8080/swagger-ui.html)

## 🌟 使用示例

### 基础查询
```bash
curl -X POST http://localhost:8080/api/v1/graph-rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "什么是人工智能？",
    "retrievalMode": "hybrid",
    "maxDocuments": 5
  }'
```

### 文档上传
```bash
curl -X POST http://localhost:8080/api/v1/graph-rag/documents/upload \
  -F "file=@document.pdf" \
  -F "source=学术论文"
```

### 实体查询
```bash
curl "http://localhost:8080/api/v1/graph-rag/entities/人工智能/related?maxHops=2"
```

## 🔍 监控面板

访问 Grafana 监控面板：http://localhost:3000

- **系统概览：** 整体性能指标
- **查询分析：** 查询性能和用户行为
- **资源监控：** CPU、内存、存储使用
- **错误分析：** 错误统计和趋势

## 🚨 故障排查

### 常见问题

**服务启动失败**
```bash
# 检查日志
docker-compose logs graph-rag-app

# 检查端口占用
netstat -tlnp | grep 8080
```

**查询响应慢**
```bash
# 检查系统资源
docker stats

# 查看慢查询日志
grep "slow query" logs/graph-rag.log
```

**数据库连接失败**
```bash
# 测试 Neo4j 连接
docker exec graph-rag-neo4j cypher-shell -u neo4j -p password123
```

## 🔐 安全配置

- **API 认证：** JWT Token / API Key
- **数据加密：** AES-256 存储加密
- **网络安全：** HTTPS + 防火墙
- **访问控制：** RBAC 权限管理

## 📈 扩展部署

### Kubernetes 部署
```bash
# 部署到 K8s 集群
kubectl apply -f k8s-deployment.yaml

# 检查部署状态
kubectl get pods -n graph-rag
```

### 集群扩展
```bash
# 扩展应用实例
kubectl scale deployment graph-rag-app --replicas=5

# 扩展 Neo4j 集群
helm install neo4j-cluster neo4j/neo4j-cluster
```
