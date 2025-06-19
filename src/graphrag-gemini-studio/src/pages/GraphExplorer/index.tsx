import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, Input, App, Empty, Button } from 'antd';
import { graphRagApi } from '@/api/graphRagApi';
import GraphVisualization from '@/components/GraphVisualization';

const GraphExplorerPage: React.FC = () => {
  const [entityName, setEntityName] = useState('');
  const [searchTrigger, setSearchTrigger] = useState('');
  const { message } = App.useApp();

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['relatedEntities', searchTrigger],
    queryFn: () => graphRagApi.getRelatedEntities(searchTrigger),
    enabled: !!searchTrigger,
    select: (response) => {
      if (response.success) {
        return response.data;
      }
      throw new Error(response.message);
    },
    onError: (err) => {
      message.error(`获取图数据失败: ${err.message}`);
    },
  });

  const handleSearch = () => {
    if (!entityName.trim()) {
      message.warning('请输入实体名称');
      return;
    }
    setSearchTrigger(entityName);
  };

  return (
    <div className="space-y-lg">
      <Card title="图谱探索">
        <Input.Search
          placeholder="输入一个实体名称开始探索，例如 'Vector Database'"
          enterButton="探索"
          size="large"
          value={entityName}
          onChange={(e) => setEntityName(e.target.value)}
          onSearch={handleSearch}
          loading={isLoading}
        />
      </Card>
      <Card title="可视化结果">
        {isError && (
          <Empty
            description={
              <span>
                加载失败: {error?.message} <br />
                <Button type="primary" onClick={handleSearch} className="mt-md">
                  重试
                </Button>
              </span>
            }
          />
        )}
        {!isError && (!data || data.length === 0) && !isLoading && (
          <Empty description="未找到相关实体或尚未开始探索" />
        )}
        <GraphVisualization data={data || null} isLoading={isLoading} />
      </Card>
    </div>
  );
};

export default GraphExplorerPage;
