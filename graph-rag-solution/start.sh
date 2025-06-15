#!/bin/bash

# Graph RAG 項目快速啟動腳本
# 使用方法：./start.sh

set -e

echo "🚀 Graph RAG 項目啟動腳本"
echo "=========================="

# 檢查 Java 版本
echo "📋 檢查 Java 版本..."
if ! command -v java &> /dev/null; then
    echo "❌ 錯誤：未找到 Java，請安裝 Java 17 或更高版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ 錯誤：需要 Java 17 或更高版本，當前版本：$JAVA_VERSION"
    exit 1
fi
echo "✅ Java 版本檢查通過"

# 檢查 Maven
echo "📋 檢查 Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ 錯誤：未找到 Maven，請安裝 Maven"
    exit 1
fi
echo "✅ Maven 檢查通過"

# 檢查 Docker
echo "📋 檢查 Docker..."
if ! command -v docker &> /dev/null; then
    echo "❌ 錯誤：未找到 Docker，請安裝 Docker"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "❌ 錯誤：Docker 未運行，請啟動 Docker"
    exit 1
fi
echo "✅ Docker 檢查通過"

# 設置環境變量
echo "🔧 設置環境變量..."
export OPENAI_API_KEY="${OPENAI_API_KEY:-sk-test-key}"
export NEO4J_PASSWORD="${NEO4J_PASSWORD:-password123}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-redis123}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

if [ "$OPENAI_API_KEY" = "sk-test-key" ]; then
    echo "⚠️  警告：使用測試 OpenAI API Key，請設置真實的 API Key"
    echo "   export OPENAI_API_KEY='your-real-api-key'"
fi

# 啟動基礎設施
echo "🐳 啟動基礎設施（Neo4j 和 Redis）..."
docker-compose up -d neo4j redis

# 等待 Neo4j 啟動
echo "⏳ 等待 Neo4j 啟動..."
for i in {1..30}; do
    if docker-compose exec -T neo4j cypher-shell -u neo4j -p $NEO4J_PASSWORD "RETURN 1" &> /dev/null; then
        echo "✅ Neo4j 已啟動"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Neo4j 啟動超時"
        exit 1
    fi
    sleep 2
done

# 等待 Redis 啟動
echo "⏳ 等待 Redis 啟動..."
for i in {1..10}; do
    if docker-compose exec -T redis redis-cli ping &> /dev/null; then
        echo "✅ Redis 已啟動"
        break
    fi
    if [ $i -eq 10 ]; then
        echo "❌ Redis 啟動超時"
        exit 1
    fi
    sleep 1
done

# 編譯項目
echo "🔨 編譯項目..."
mvn clean compile -DskipTests -q

# 啟動應用
echo "🚀 啟動 Graph RAG API 服務..."
echo "   服務將在 http://localhost:8080 啟動"
echo "   Swagger 文檔：http://localhost:8080/swagger-ui.html"
echo "   健康檢查：http://localhost:8080/api/v1/graph-rag/health"
echo ""
echo "按 Ctrl+C 停止服務"
echo "=========================="

# 啟動應用（前台運行）
mvn spring-boot:run -pl graph-rag-api -q

