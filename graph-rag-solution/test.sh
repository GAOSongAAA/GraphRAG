#!/usr/bin/env bash
set -euo pipefail

# === 全局配置 ================================================================
BASE_URL="http://localhost:8080/api/v1/graph-rag"
TOKEN=""                      # <- 如果后端需要 JWT/Bearer，请在此写入
HDR_AUTH=()
[[ -n "$TOKEN" ]] && HDR_AUTH=(-H "Authorization: Bearer $TOKEN")

# === 1. 同步查询 /query ======================================================
echo -e "\n[1] 同步查询 (POST /query)"
curl -s "${BASE_URL}/query" \
  -X POST \
  -H "Content-Type: application/json" \
  "${HDR_AUTH[@]}" \
  -d '{
        "question": "Explain quantum entanglement",
        "topK": 5,
        "retrievalMode": "hybrid"
      }' | jq .

# === 2. 异步查询 /query/async ===============================================
echo -e "\n[2] 提交异步查询 (POST /query/async)"
TASK_ID=$(curl -s "${BASE_URL}/query/async" \
  -X POST \
  -H "Content-Type: application/json" \
  "${HDR_AUTH[@]}" \
  -d '{
        "question": "What is superconductivity?",
        "retrievalMode": "standard"
      }' | jq -r '.data')
echo "任务 ID: $TASK_ID"

echo -e "\n[2] 轮询异步结果 (GET /query/async/{id})"
while true; do
  STATUS=$(curl -s "${BASE_URL}/query/async/$TASK_ID" "${HDR_AUTH[@]}" | jq -r '.data')
  echo "当前状态: $STATUS"
  [[ "$STATUS" != "running" ]] && break
  sleep 2
done | cat

# === 3. 查询意图分析 /analyze ===============================================
echo -e "\n[3] 查询意图分析 (POST /analyze)"
curl -s "${BASE_URL}/analyze" \
  -X POST \
  --data-urlencode "query=Describe the impact of AI on jobs" \
  "${HDR_AUTH[@]}" | jq .

# === 4. 单文件上传 /documents/upload =========================================
echo -e "\n[4] 单文件上传 (POST /documents/upload)"
curl -s "${BASE_URL}/documents/upload" \
  -X POST \
  -F "file=@/path/to/your/doc.pdf" \
  -F "source=research-paper" \
  "${HDR_AUTH[@]}" | jq .

# === 5. 批量上传 /documents/batch-upload =====================================
echo -e "\n[5] 批量上传 (POST /documents/batch-upload)"
curl -s "${BASE_URL}/documents/batch-upload" \
  -X POST \
  -F "files=@/path/to/file1.md" \
  -F "files=@/path/to/file2.md" \
  -F "source=manual" \
  "${HDR_AUTH[@]}" | jq .

# === 6. 统计信息 /stats =======================================================
echo -e "\n[6] 图谱统计 (GET /stats)"
curl -s "${BASE_URL}/stats" "${HDR_AUTH[@]}" | jq .

# === 7. 健康检查 /health ======================================================
echo -e "\n[7] 服务健康 (GET /health)"
curl -s "${BASE_URL}/health" "${HDR_AUTH[@]}" | jq .

# === 8. 清空知识图谱 /clear ===================================================
echo -e "\n[8] 清空知识图谱 (DELETE /clear)  ⚠️危险操作"
# curl -s -X D
