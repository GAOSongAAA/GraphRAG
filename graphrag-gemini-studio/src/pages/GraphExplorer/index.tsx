import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card, Input, App, Empty, Button } from 'antd';
import { graphRagApi } from '@/api/graphRagApi';
import GraphVisualization from '@/components/GraphVisualization';
import { RelatedEntityInfo } from '@/api/types';

const GraphExplorerPage: React.FC = () => {
  const [entityName, setEntityName] = useState('');
  const [searchTrigger, setSearchTrigger] = useState('');
  const { message } = App.useApp();

  const { data, isLoading, isError, error } = useQuery<RelatedEntityInfo[], Error>({
    queryKey: ['relatedEntities', searchTrigger],
    queryFn: () => graphRagApi.getRelatedEntities(searchTrigger).then(response => {
      if (response.code === 0) {
        return response.data;
      }
      throw new Error(response.message);
    }),
    enabled: !!searchTrigger,
  });
  // 使用 useEffect 來處理錯誤訊息
  React.useEffect(() => {
    if (isError && error) {
      message.error(`獲取圖數據失敗: ${error.message}`);
    }
  }, [isError, error, message]);

  const handleSearch = () => {
    if (!entityName.trim()) {
      message.warning('請輸入實體名稱');
      return;
    }
    setSearchTrigger(entityName);
  };

  return (
    <div className="space-y-lg">
      <Card title="圖譜探索">
        <Input.Search
          placeholder="輸入一個實體名稱開始探索，例如 'Vector Database'"
          enterButton="探索"
          size="large"
          value={entityName}
          onChange={(e) => setEntityName(e.target.value)}
          onSearch={handleSearch}
          loading={isLoading}
        />
      </Card>
      <Card title="可視化結果">
        {isError && (
          <Empty
            description={
              <span>
                載入失敗: {error?.message} <br />
                <Button type="primary" onClick={handleSearch} className="mt-md">
                  重試
                </Button>
              </span>
            }
          />
        )}
        {!isError && (!data || data.length === 0) && !isLoading && (
          <Empty description="未找到相關實體或尚未開始探索" />
        )}
        <GraphVisualization data={data ?? null} isLoading={isLoading} />
      </Card>
    </div>
  );
};

export default GraphExplorerPage;
