import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, Col, Row, Statistic, Spin, Alert, App, Button } from 'antd';
import { DatabaseOutlined, ApartmentOutlined } from '@ant-design/icons';
import EChartsReact from 'echarts-for-react';
import { graphRagApi } from '@/api/graphRagApi';
import { ApiResponse, GraphStats } from '@/api/types';

const StatisticsPage: React.FC = () => {
  const { message } = App.useApp();

  const { data, isLoading, isError, error, refetch } = useQuery<ApiResponse<GraphStats>, Error, GraphStats>({
    queryKey: ['graphStats'],
    queryFn: () => graphRagApi.getStats(),
    select: (response) => {
      if (response.success) {
        return response.data;
      }
      throw new Error(response.message);
    },
  });
  // 使用 useEffect 來處理錯誤訊息
  React.useEffect(() => {
    if (isError && error) {
      message.error(`獲取統計數據失敗: ${error.message}`);
    }
  }, [isError, error, message]);

  const getPieChartOptions = () => {
    if (!data || !data.labels) return {};
    return {
      title: {
        text: '節點標籤分佈',
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
          name: '標籤數量',
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
        <Spin size="large" tip="正在載入統計數據..." />
      </div>
    );
  }

  if (isError) {
    return (
      <Alert
        message="載入失敗"
        description={error?.message}
        type="error"
        showIcon
        action={
          <Button size="small" type="primary" onClick={() => refetch()}>
            重試
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
              title="總節點數"
              value={data?.nodeCount || 0}
              prefix={<ApartmentOutlined />}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            <Statistic
              title="總關係數"
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
