import React, { useEffect, useRef } from 'react';
import cytoscape from 'cytoscape';
import { RelatedEntityInfo } from '@/api/types';
import { Spin } from 'antd';

interface GraphVisualizationProps {
  data: RelatedEntityInfo[] | null;
  isLoading: boolean;
  style?: React.CSSProperties;
}

const GraphVisualization: React.FC<GraphVisualizationProps> = ({
  data,
  isLoading,
  style = { height: '600px' },
}) => {
  const cyRef = useRef<HTMLDivElement>(null);
  const cyInstanceRef = useRef<cytoscape.Core | null>(null);

  useEffect(() => {
    if (!cyRef.current || !data) return;

    if (cyInstanceRef.current) {
      cyInstanceRef.current.destroy();
    }

    const elements = data.flatMap((info) => {
      const pathElements: cytoscape.ElementDefinition[] = [];
      const mainEntityId = info.entity.properties.id || info.entity.properties.name;

      pathElements.push({
        data: { id: mainEntityId, label: info.entity.properties.name || mainEntityId },
        classes: 'main-entity'
      });
      
      info.path.forEach((segment, i) => {
        const startId = segment.start.properties.id || segment.start.properties.name;
        const endId = segment.end.properties.id || segment.end.properties.name;
        
        pathElements.push({
          data: { id: startId, label: segment.start.properties.name || startId },
        });
        pathElements.push({
          data: { id: endId, label: segment.end.properties.name || endId },
        });
        pathElements.push({
          data: {
            id: `e${i}-${startId}-${endId}`,
            source: startId,
            target: endId,
            label: segment.relationship.type,
          },
        });
      });
      return pathElements;
    });

    const uniqueElements = Array.from(
      new Map(elements.map((item) => [item.data.id, item])).values()
    );

    cyInstanceRef.current = cytoscape({
      container: cyRef.current,
      elements: uniqueElements,
      style: [
        {
          selector: 'node',
          style: {
            'background-color': '#1890ff',
            label: 'data(label)',
            'font-size': '12px',
            'text-valign': 'bottom',
            'text-halign': 'center',
            'text-margin-y': 5,
            color: '#333',
            width: '30px',
            height: '30px',
          },
        },
        {
            selector: '.main-entity',
            style: {
                'background-color': '#faad14',
                width: '40px',
                height: '40px',
            }
        },
        {
          selector: 'edge',
          style: {
            width: 2,
            'line-color': '#ccc',
            'target-arrow-color': '#ccc',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
            label: 'data(label)',
            'font-size': '10px',
            color: '#666',
          },
        },
      ],
      layout: {
        name: 'cose',
        fit: true,
        padding: 30,
        animate: true,
        animationDuration: 500,
        nodeRepulsion: () => 10000,
      },
    });
  }, [data]);

  return (
    <div className="w-full h-full relative border rounded">
      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 z-10">
          <Spin size="large" />
        </div>
      )}
      <div ref={cyRef} style={style} />
    </div>
  );
};

export default GraphVisualization;
