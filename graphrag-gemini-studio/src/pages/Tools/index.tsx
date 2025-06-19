import React, { useState } from 'react';
import { Card, Input, Button, App, Space, Typography } from 'antd';
import ResultDisplay from '@/components/ResultDisplay';
import { graphRagApi } from '@/api/graphRagApi';
import { GraphRagResponse, QueryAnalysis } from '@/api/types';

const { Paragraph } = Typography;

const ToolsPage: React.FC = () => {
  const { message } = App.useApp();

  const [analysisInput, setAnalysisInput] = useState('');
  const [analysis, setAnalysis] = useState<QueryAnalysis | null>(null);
  const [analysisLoading, setAnalysisLoading] = useState(false);

  const [asyncQuestion, setAsyncQuestion] = useState('');
  const [taskId, setTaskId] = useState<string | null>(null);
  const [asyncResult, setAsyncResult] = useState<GraphRagResponse | null>(null);
  const [asyncStatus, setAsyncStatus] = useState('');
  const [asyncLoading, setAsyncLoading] = useState(false);

  const handleAnalyze = () => {
    if (!analysisInput.trim()) {
      message.warning('请输入查询');
      return;
    }
    setAnalysis(null);
    setAnalysisLoading(true);
    graphRagApi
      .analyzeQuery(analysisInput)
      .then((res) => {
        if (res.success) {
          setAnalysis(res.data);
        } else {
          message.error(res.message);
        }
      })
      .catch((err) => message.error(err.message))
      .finally(() => setAnalysisLoading(false));
  };

  const handleSubmitAsync = () => {
    if (!asyncQuestion.trim()) {
      message.warning('请输入问题');
      return;
    }
    setAsyncLoading(true);
    setAsyncResult(null);
    setAsyncStatus('');
    graphRagApi
      .submitAsyncQuery({ question: asyncQuestion })
      .then((res) => {
        if (res.success) {
          setTaskId(res.data);
          message.success('任务已提交');
        } else {
          message.error(res.message);
        }
      })
      .catch((err) => message.error(err.message))
      .finally(() => setAsyncLoading(false));
  };

  const handleGetResult = () => {
    if (!taskId) return;
    setAsyncLoading(true);
    graphRagApi
      .getAsyncResult(taskId)
      .then((res) => {
        if (!res.success) {
          message.error(res.message);
          return;
        }
        if (res.data === 'running' || res.data === 'task not found') {
          setAsyncStatus(res.data);
        } else {
          setAsyncResult(res.data);
          setAsyncStatus('completed');
        }
      })
      .catch((err) => message.error(err.message))
      .finally(() => setAsyncLoading(false));
  };

  const handleHealth = () => {
    graphRagApi
      .checkHealth()
      .then((res) => {
        if (res.success) {
          message.success(res.data);
        } else {
          message.error(res.message);
        }
      })
      .catch((err) => message.error(err.message));
  };

  const handleClear = () => {
    graphRagApi
      .clearGraph()
      .then((res) => {
        if (res.success) {
          message.success(res.data);
        } else {
          message.error(res.message);
        }
      })
      .catch((err) => message.error(err.message));
  };

  return (
    <Space direction="vertical" size="large" className="w-full">
      <Card title="查询分析">
        <Input.Search
          placeholder="输入问题以分析其类型"
          enterButton="分析"
          value={analysisInput}
          onChange={(e) => setAnalysisInput(e.target.value)}
          onSearch={handleAnalyze}
          loading={analysisLoading}
        />
        {analysis && (
          <Paragraph className="mt-md">
            {JSON.stringify(analysis, null, 2)}
          </Paragraph>
        )}
      </Card>

      <Card title="异步问答">
        <Space direction="vertical" className="w-full">
          <Input.Search
            placeholder="输入问题提交异步任务"
            enterButton="提交"
            value={asyncQuestion}
            onChange={(e) => setAsyncQuestion(e.target.value)}
            onSearch={handleSubmitAsync}
            loading={asyncLoading}
          />
          {taskId && (
            <Space>
              <span>任务ID: {taskId}</span>
              <Button onClick={handleGetResult} loading={asyncLoading}>
                获取结果
              </Button>
              {asyncStatus && <span>状态: {asyncStatus}</span>}
            </Space>
          )}
          {asyncResult && <ResultDisplay data={asyncResult} />}
        </Space>
      </Card>

      <Card title="系统运维">
        <Space>
          <Button onClick={handleHealth}>健康检查</Button>
          <Button danger onClick={handleClear}>
            清空知识图谱
          </Button>
        </Space>
      </Card>
    </Space>
  );
};

export default ToolsPage;
