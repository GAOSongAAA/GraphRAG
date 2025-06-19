import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Input, Card, Spin, Alert, Row, Col, App, Empty } from 'antd';
import { graphRagApi } from '@/api/graphRagApi';
import { GraphRagRequest, GraphRagResponse } from '@/api/types';
import ResultDisplay from '@/components/ResultDisplay';
import FileUploader from '@/components/FileUploader';

const DashboardPage: React.FC = () => {
  const [result, setResult] = useState<GraphRagResponse | null>(null);
  const { message } = App.useApp();

  const queryMutation = useMutation({
    mutationFn: (request: GraphRagRequest) => graphRagApi.query(request),
    onSuccess: (response) => {
      if (response.code === 0) {
        setResult(response.data);
      } else {
        message.error(response.message);
      }
    },
    onError: (error) => {
      message.error(error.message);
      setResult(null);
    },
  });

  const handleSearch = (value: string) => {
    if (!value.trim()) {
      message.warning('请输入问题');
      return;
    }
    setResult(null);
    const request: GraphRagRequest = {
      question: value,
      retrievalMode: 'hybrid',
    };
    queryMutation.mutate(request);
  };

  return (
    <div className="space-y-lg">
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card title="智能问答">
            <Input.Search
              placeholder="请输入您的问题，例如：'Explain vector databases in simple terms'"
              enterButton="提问"
              size="large"
              onSearch={handleSearch}
              loading={queryMutation.isPending}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={16}>
          <Card title="问答结果" style={{ minHeight: '400px' }}>
            {queryMutation.isPending && (
              <div className="flex justify-center items-center h-full">
                <Spin size="large" tip="正在思考中..." />
              </div>
            )}
            {queryMutation.isError && (
              <Alert
                message="查询失败"
                description={queryMutation.error.message}
                type="error"
                showIcon
              />
            )}
            {result && <ResultDisplay data={result} />}
            {!queryMutation.isPending && !result && !queryMutation.isError && (
              <div className="flex justify-center items-center h-full">
                <Empty description="请输入问题以开始查询" />
              </div>
            )}
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="知识库上传" style={{ minHeight: '400px' }}>
            <FileUploader />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DashboardPage;
