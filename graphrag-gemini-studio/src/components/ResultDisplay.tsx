import React from 'react';
import { Card, Typography, Descriptions, Tag, Collapse } from 'antd';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { GraphRagResponse } from '@/api/types';
import { CheckCircleOutlined, FileTextOutlined } from '@ant-design/icons';

const { Text } = Typography;
const { Panel } = Collapse;

interface Props {
  data: GraphRagResponse;
}

const ResultDisplay: React.FC<Props> = ({ data }) => {
  return (
    <div className="space-y-md">
      <Card bordered={false}>
        <Descriptions title="核心答案" column={1}>
          <Descriptions.Item label="耗时">
            {data.processingTimeMs} ms
          </Descriptions.Item>
        </Descriptions>
        <ReactMarkdown remarkPlugins={[remarkGfm as any]} className="whitespace-pre-wrap">
          {data.answer}
        </ReactMarkdown>
      </Card>

      <Collapse accordion>
        <Panel
          header={`推理依据 (${data.segments?.length ?? 0}个片段)`}
          key="1"
        >
          <div className="space-y-md max-h-96 overflow-y-auto pr-sm">
            {data.segments?.map((seg, index) => (
              <Card key={index} size="small">
                <div className="flex justify-between items-start">
                  <FileTextOutlined className="mr-sm mt-1" />
                  <div className="flex-1">
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm as any]}
                      className="whitespace-pre-wrap !mb-0"
                    >
                      {seg.content}
                    </ReactMarkdown>
                  </div>
                  <Tag
                    icon={<CheckCircleOutlined />}
                    color="success"
                    className="ml-sm"
                  >
                    {seg.score.toFixed(2)}
                  </Tag>
                </div>
                {seg.source && <Text type="secondary">来源: {seg.source}</Text>}
              </Card>
            ))}
          </div>
        </Panel>
      </Collapse>
    </div>
  );
};

export default ResultDisplay;
