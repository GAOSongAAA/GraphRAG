import React from 'react';
import { Card, Typography, Descriptions, Collapse } from 'antd';
import { GraphRagResponse } from '@/api/types';

const { Paragraph, Text } = Typography;
const { Panel } = Collapse;

interface Props {
  data: GraphRagResponse;
}

const ResultDisplay: React.FC<Props> = ({ data }) => {
  return (
    <div className="space-y-md">
      <Card bordered={false}>
        <Descriptions title="核心答案" column={1}>
          {data.processingTimeMs !== undefined && (
            <Descriptions.Item label="耗时">
              {data.processingTimeMs} ms
            </Descriptions.Item>
          )}
          {data.confidence !== undefined && (
            <Descriptions.Item label="置信度">
              {(data.confidence * 100).toFixed(2)}%
            </Descriptions.Item>
          )}
        </Descriptions>
        <Paragraph className="text-base">{data.answer}</Paragraph>
      </Card>

      <Collapse accordion>
        {data.relevantDocuments && data.relevantDocuments.length > 0 && (
          <Panel
            header={`相关文档 (${data.relevantDocuments.length})`}
            key="docs"
          >
            <div className="space-y-md max-h-96 overflow-y-auto pr-sm">
              {data.relevantDocuments.map((doc, index) => (
                <Card key={index} size="small">
                  <Paragraph className="!mb-0">
                    {doc.title || doc.name || `文档${index + 1}`}
                  </Paragraph>
                  {doc.source && (
                    <Text type="secondary">来源: {doc.source}</Text>
                  )}
                </Card>
              ))}
            </div>
          </Panel>
        )}
        {data.relevantEntities && data.relevantEntities.length > 0 && (
          <Panel
            header={`相关实体 (${data.relevantEntities.length})`}
            key="entities"
          >
            <div className="space-y-md max-h-96 overflow-y-auto pr-sm">
              {data.relevantEntities.map((ent, index) => (
                <Card key={index} size="small">
                  <Paragraph className="!mb-0">
                    {ent.name || ent.id}
                  </Paragraph>
                  {ent.type && (
                    <Text type="secondary">类型: {ent.type}</Text>
                  )}
                </Card>
              ))}
            </div>
          </Panel>
        )}
      </Collapse>
    </div>
  );
};

export default ResultDisplay;
