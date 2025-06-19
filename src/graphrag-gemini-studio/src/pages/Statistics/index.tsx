import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, Col, Row, Statistic, Spin, Alert, App, Button } from 'antd';
import { DatabaseOutlined, ApartmentOutlined } from '@ant-design/icons';
import EChartsReact from 'echarts-for-react';
import { graphRagApi } from '@/api/graphRagApi';

const StatisticsPage: React.FC = () => {
  const { message } = App.useApp();

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['graphStats'],
    queryFn: () => graphRagApi.getStats(),
    select: (response) => {
      if (response.code === 0) {
        return response.data;
      }
      throw new Error(response.message);
    },
    onError: (err) => {
      message.error(`获取统计数据失败: ${err.message}`);
    },
  });

  const getPieChartOptions = () => {
    if (!data || !data.labels) return {};
    return {
      title: {
        text: '节点标签分布',
        left: 'center',
      },
      tooltip: {
        trigger: 'item',
      },
      legend: {
        orient: 'vertical',
        left: 'left',
      },
      series: [
        {
          name: '标签数量',
          type: 'pie',
          radius: '50%',
          data: Object.entries(data.labels).map(([name, value]) => ({
            name,
            value,
          })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
        },
      ],
    };
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-full">
        <Spin size="large" tip="正在加载统计数据..." />
      </div>
    );
  }

  if (isError) {
    return (
      <Alert
        message="加载失败"
        description={error.message}
        type="error"
        showIcon
        action={
          <Button size="small" type="primary" onClick={() => refetch()}>
            重试
          </Button>
        }
      />
    );
  }

  return (
    <div className="space-y-lg">
      <Row gutter={16}>
        <Col span={12}>
          <Card>
            <Statistic
              title="总节点数"
              value={data?.nodeCount || 0}
              prefix={<ApartmentOutlined />}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            <Statistic
              title="总关系数"
              value={data?.edgeCount || 0}
              prefix={<DatabaseOutlined />}
            />
          </Card>
        </Col>
      </Row>
      <Row>
        <Col span={24}>
          <Card>
            <EChartsReact option={getPieChartOptions()} style={{ height: 400 }} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default StatisticsPage;
