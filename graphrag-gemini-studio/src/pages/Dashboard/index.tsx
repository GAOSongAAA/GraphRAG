import React, { useState, useRef, useEffect } from 'react';
import { Input, Card, Spin, Alert, Row, Col, App, Empty } from 'antd';
import { graphRagApi } from '@/api/graphRagApi';
import { GraphRagRequest, GraphRagResponse } from '@/api/types';
import ResultDisplay from '@/components/ResultDisplay';
import FileUploader from '@/components/FileUploader';

const DashboardPage: React.FC = () => {
  const [result, setResult] = useState<GraphRagResponse | null>(null);
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const eventRef = useRef<EventSource | null>(null);

  useEffect(() => {
    return () => {
      eventRef.current?.close();
    };
  }, []);

  const handleSearch = (value: string) => {
    if (!value.trim()) {
      message.warning('请输入问题');
      return;
    }
    setResult(null);
    setError(null);
    setLoading(true);
    const request: GraphRagRequest = {
      question: value,
      retrievalMode: 'hybrid',
    };
    eventRef.current?.close();
    eventRef.current = graphRagApi.queryStream(
      request,
      (res) => {
        setLoading(false);
        if (res.success) {
          setResult(res.data);
        } else {
          setError(res.message);
        }
        eventRef.current?.close();
      },
      () => {
        setLoading(false);
        setError('查询失败');
      }
    );
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
              loading={loading}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={16}>
          <Card title="问答结果" style={{ minHeight: '400px' }}>
            {loading && (
              <div className="flex justify-center items-center h-full">
                <Spin size="large" tip="正在思考中..." />
              </div>
            )}
            {error && (
              <Alert
                message="查询失败"
                description={error}
                type="error"
                showIcon
              />
            )}
            {result && <ResultDisplay data={result} />}
            {!loading && !result && !error && (
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
