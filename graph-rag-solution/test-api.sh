#!/bin/bash

# Graph RAG API 測試腳本
# 測試所有主要 API 端點

set -e

BASE_URL="http://localhost:8081"
echo "🧪 Graph RAG API 測試開始"
echo "基礎 URL: $BASE_URL"
echo "=========================="

# 測試函數
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo "📋 測試: $description"
    echo "   $method $endpoint"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    fi
    
    # 分離響應體和狀態碼
    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
        echo "   ✅ 成功 (HTTP $http_code)"
        echo "   響應: $(echo "$response_body" | head -c 100)..."
    else
        echo "   ❌ 失敗 (HTTP $http_code)"
        echo "   錯誤: $(echo "$response_body" | head -c 200)..."
    fi
    echo ""
}

# 1. 測試健康檢查
test_endpoint "GET" "/api/v1/graph-rag/health" "" "健康檢查"

# 2. 測試統計信息
test_endpoint "GET" "/api/v1/graph-rag/stats" "" "統計信息"

# 3. 測試基本查詢
query_data='{
    "question": "什麼是人工智能？",
    "retrievalMode": "vector",
    "maxDocuments": 5,
    "maxEntities": 10,
    "similarityThreshold": 0.7
}'
test_endpoint "POST" "/api/v1/graph-rag/query" "$query_data" "基本查詢"

# 4. 測試混合查詢
hybrid_query_data='{
    "question": "機器學習的應用領域有哪些？",
    "retrievalMode": "hybrid",
    "maxDocuments": 10,
    "maxEntities": 15,
    "similarityThreshold": 0.8
}'
test_endpoint "POST" "/api/v1/graph-rag/query" "$hybrid_query_data" "混合查詢"

# 5. 測試查詢分析
test_endpoint "POST" "/api/v1/graph-rag/analyze?query=深度學習" "" "查詢分析"

# 6. 測試異步查詢
async_query_data='{
    "question": "神經網絡的發展歷史",
    "retrievalMode": "graph",
    "maxDocuments": 8
}'
test_endpoint "POST" "/api/v1/graph-rag/query/async" "$async_query_data" "異步查詢"

# 7. 測試實體搜索
test_endpoint "GET" "/api/v1/graph-rag/entities/search?query=人工智能&limit=5" "" "實體搜索"

# 8. 測試相關實體
test_endpoint "GET" "/api/v1/graph-rag/entities/人工智能/related?maxHops=2&maxResults=10" "" "相關實體查詢"

# 9. 測試文檔上傳（模擬）
upload_data='{
    "title": "測試文檔",
    "content": "這是一個關於人工智能的測試文檔。人工智能是計算機科學的一個分支。",
    "metadata": {
        "source": "test",
        "category": "AI"
    }
}'
test_endpoint "POST" "/api/v1/graph-rag/upload" "$upload_data" "文檔上傳"

# 10. 測試 Swagger 文檔
echo "📋 測試: Swagger 文檔訪問"
swagger_response=$(curl -s -w "%{http_code}" "$BASE_URL/swagger-ui.html" -o /dev/null)
if [ "$swagger_response" -eq 200 ]; then
    echo "   ✅ Swagger UI 可訪問"
else
    echo "   ❌ Swagger UI 不可訪問 (HTTP $swagger_response)"
    # 嘗試新版本路徑
    swagger_response2=$(curl -s -w "%{http_code}" "$BASE_URL/swagger-ui/index.html" -o /dev/null)
    if [ "$swagger_response2" -eq 200 ]; then
        echo "   ✅ Swagger UI 可訪問 (新路徑)"
    else
        echo "   ❌ Swagger UI 完全不可訪問"
    fi
fi
echo ""

# 11. 測試 Actuator 端點
echo "📋 測試: Spring Boot Actuator 端點"
actuator_endpoints=("/actuator/health" "/actuator/info" "/actuator/metrics")

for endpoint in "${actuator_endpoints[@]}"; do
    response_code=$(curl -s -w "%{http_code}" "$BASE_URL$endpoint" -o /dev/null)
    if [ "$response_code" -eq 200 ]; then
        echo "   ✅ $endpoint 可訪問"
    else
        echo "   ❌ $endpoint 不可訪問 (HTTP $response_code)"
    fi
done
echo ""

# 12. 性能測試
echo "📋 測試: 基本性能測試"
start_time=$(date +%s%N)
for i in {1..5}; do
    curl -s "$BASE_URL/api/v1/graph-rag/health" > /dev/null
done
end_time=$(date +%s%N)
duration=$(( (end_time - start_time) / 1000000 ))
avg_time=$(( duration / 5 ))
echo "   平均響應時間: ${avg_time}ms (5次請求)"
echo ""

# 總結
echo "🎯 測試完成總結"
echo "=========================="
echo "✅ 基礎功能測試完成"
echo "✅ API 端點連通性測試完成"
echo "✅ 性能基準測試完成"
echo ""
echo "📝 注意事項："
echo "- 某些端點可能因為缺少數據而返回空結果"
echo "- Neo4j 和 Redis 連接狀態會影響部分功能"
echo "- OpenAI API Key 配置會影響 LLM 相關功能"
echo ""
echo "🔗 有用的鏈接："
echo "- API 文檔: $BASE_URL/swagger-ui.html"
echo "- 健康檢查: $BASE_URL/api/v1/graph-rag/health"
echo "- 監控指標: $BASE_URL/actuator/metrics" 