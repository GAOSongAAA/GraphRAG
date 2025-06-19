#!/bin/bash

# Exit script on any error
set -e

# 1. Create Project Directory
PROJECT_NAME="graphrag-gemini-studio"
echo "Creating project directory: ${PROJECT_NAME}"
mkdir -p "${PROJECT_NAME}"
cd "${PROJECT_NAME}"

# 2. Create Subdirectories
echo "Creating subdirectories..."
mkdir -p src/api src/assets src/components/common src/components/GraphVisualization src/config src/hooks src/layouts src/pages/Dashboard src/pages/GraphExplorer src/pages/Statistics src/store src/styles src/types src/utils

# 3. Create Files
echo "Creating project files..."

# --- Root Files ---

echo "-> Creating package.json"
cat <<'EOF' > package.json
{
  "name": "graphrag-gemini-studio",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "preview": "vite preview",
    "prepare": "husky install"
  },
  "dependencies": {
    "@ant-design/icons": "^5.3.7",
    "@reduxjs/toolkit": "^2.2.5",
    "@tanstack/react-query": "^5.45.1",
    "@tanstack/react-query-devtools": "^5.45.1",
    "antd": "^5.18.0",
    "axios": "^1.7.2",
    "cytoscape": "^3.29.2",
    "echarts": "^5.5.0",
    "echarts-for-react": "^3.0.2",
    "fira-code": "^6.2.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-redux": "^9.1.2",
    "react-router-dom": "^6.23.1"
  },
  "devDependencies": {
    "@types/cytoscape": "^3.21.0",
    "@types/react": "^18.2.66",
    "@types/react-dom": "^18.2.22",
    "@typescript-eslint/eslint-plugin": "^7.2.0",
    "@typescript-eslint/parser": "^7.2.0",
    "@vitejs/plugin-react": "^4.2.1",
    "autoprefixer": "^10.4.19",
    "eslint": "^8.57.0",
    "eslint-config-prettier": "^9.1.0",
    "eslint-plugin-react-hooks": "^4.6.0",
    "eslint-plugin-react-refresh": "^0.4.6",
    "husky": "^8.0.0",
    "postcss": "^8.4.38",
    "prettier": "^3.3.2",
    "tailwindcss": "^3.4.4",
    "typescript": "^5.2.2",
    "vite": "^5.2.0"
  }
}
EOF

echo "-> Creating vite.config.ts"
cat <<'EOF' > vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
})
EOF

echo "-> Creating tailwind.config.js"
cat <<'EOF' > tailwind.config.js
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#1890ff',
        success: '#52c41a',
        warning: '#faad14',
        error: '#f5222d',
        'text-primary': '#333333',
        'bg-main': '#f0f2f5',
      },
      spacing: {
        xs: '4px',
        sm: '8px',
        md: '16px',
        lg: '24px',
        xl: '32px',
      },
      fontFamily: {
        sans: ['PingFang SC', 'system-ui', 'sans-serif'],
        code: ['Fira Code', 'monospace'],
      },
    },
  },
  plugins: [],
  corePlugins: {
    preflight: false,
  },
}
EOF

echo "-> Creating postcss.config.js"
cat <<'EOF' > postcss.config.js
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
}
EOF

echo "-> Creating tsconfig.json"
cat <<'EOF' > tsconfig.json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
EOF

echo "-> Creating tsconfig.node.json"
cat <<'EOF' > tsconfig.node.json
{
  "compilerOptions": {
    "composite": true,
    "skipLibCheck": true,
    "module": "ESNext",
    "moduleResolution": "bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
EOF

echo "-> Creating .eslintrc.cjs"
cat <<'EOF' > .eslintrc.cjs
module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
    'prettier',
  ],
  ignorePatterns: ['dist', '.eslintrc.cjs'],
  parser: '@typescript-eslint/parser',
  plugins: ['react-refresh'],
  rules: {
    'react-refresh/only-export-components': [
      'warn',
      { allowConstantExport: true },
    ],
    '@typescript-eslint/no-explicit-any': 'off',
  },
}
EOF

echo "-> Creating .prettierrc"
cat <<'EOF' > .prettierrc
{
  "semi": true,
  "singleQuote": true,
  "trailingComma": "es5",
  "printWidth": 80
}
EOF

echo "-> Creating index.html"
cat <<'EOF' > index.html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>GraphRAG Gemini Studio</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
EOF

echo "-> Creating .env.development"
cat <<'EOF' > .env.development
# Development API Endpoint - Replace with your actual local backend URL
VITE_API_BASE_URL=http://localhost:8080/api/v1/graph-rag
EOF

# --- src/ Directory ---

echo "-> Creating src/main.tsx"
cat <<'EOF' > src/main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ConfigProvider, App as AntApp } from 'antd';
import { store } from './store';
import App from './App';
import './styles/global.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <ConfigProvider
          theme={{
            token: {
              colorPrimary: '#1890ff',
              colorSuccess: '#52c41a',
              colorWarning: '#faad14',
              colorError: '#f5222d',
              colorTextBase: '#333333',
            },
          }}
        >
          <BrowserRouter>
            <AntApp>
              <App />
            </AntApp>
          </BrowserRouter>
        </ConfigProvider>
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </Provider>
  </React.StrictMode>
);
EOF

echo "-> Creating src/App.tsx"
cat <<'EOF' > src/App.tsx
import { Navigate, Route, Routes } from 'react-router-dom';
import PageLayout from './layouts/PageLayout';
import DashboardPage from './pages/Dashboard';
import GraphExplorerPage from './pages/GraphExplorer';
import StatisticsPage from './pages/Statistics';

function App() {
  return (
    <Routes>
      <Route path="/" element={<PageLayout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="explorer" element={<GraphExplorerPage />} />
        <Route path="statistics" element={<StatisticsPage />} />
      </Route>
    </Routes>
  );
}

export default App;
EOF

echo "-> Creating src/styles/global.css"
cat <<'EOF' > src/styles/global.css
@import url('fira-code/distr/fira_code.css');

@tailwind base;
@tailwind components;
@tailwind utilities;

body {
  background-color: #f0f2f5;
}
EOF

echo "-> Creating src/api/types.ts"
cat <<'EOF' > src/api/types.ts
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export type RetrievalMode = 'vector' | 'graph' | 'hybrid';

export interface GraphRagRequest {
  question: string;
  maxDocuments?: number;
  maxEntities?: number;
  similarityThreshold?: number;
  retrievalMode?: RetrievalMode;
  parameters?: Record<string, any>;
}

export interface Segment {
  content: string;
  score: number;
  type: 'document' | 'entity';
  source?: string;
}

export interface GraphRagResponse {
  answer: string;
  segments: Segment[];
  processingTimeMs: number;
}

export interface QueryAnalysis {
  queryType: 'factual' | 'comparative' | 'summary' | 'unknown';
  expectedAnswerType: 'short' | 'list' | 'paragraph' | 'table';
  complexity: 'simple' | 'medium' | 'complex';
}

export interface GraphStats {
  nodeCount: number;
  edgeCount: number;
  labels: Record<string, number>;
  [key: string]: any;
}

export interface RelatedEntity {
  properties: Record<string, any>;
  labels: string[];
}
export interface Relationship {
  type: string;
  properties: Record<string, any>;
}
export interface PathSegment {
  start: RelatedEntity;
  relationship: Relationship;
  end: RelatedEntity;
}

export interface RelatedEntityInfo {
  entity: RelatedEntity;
  path: PathSegment[];
  hops: number;
}
EOF

echo "-> Creating src/api/axiosClient.ts"
cat <<'EOF' > src/api/axiosClient.ts
import axios from 'axios';
import { API_BASE_URL } from '../config';

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosClient.interceptors.response.use(
  (response) => {
    // Return the full response data, letting React Query handle parsing
    return response.data;
  },
  (error) => {
    const message =
      error.response?.data?.message ||
      error.message ||
      'A network error occurred';
    return Promise.reject(new Error(message));
  }
);

export default axiosClient;
EOF

echo "-> Creating src/api/graphRagApi.ts"
cat <<'EOF' > src/api/graphRagApi.ts
import axiosClient from './axiosClient';
import {
  GraphRagRequest,
  GraphRagResponse,
  QueryAnalysis,
  GraphStats,
  RelatedEntityInfo,
  ApiResponse,
} from './types';

export const graphRagApi = {
  query: (
    request: GraphRagRequest
  ): Promise<ApiResponse<GraphRagResponse>> =>
    axiosClient.post('/query', request),

  submitAsyncQuery: (
    request: GraphRagRequest
  ): Promise<ApiResponse<string>> =>
    axiosClient.post('/query/async', request),

  getAsyncResult: (
    taskId: string
  ): Promise<ApiResponse<'running' | 'task not found' | GraphRagResponse>> =>
    axiosClient.get(`/query/async/${taskId}`),

  analyzeQuery: (query: string): Promise<ApiResponse<QueryAnalysis>> =>
    axiosClient.post('/analyze', null, { params: { query } }),

  uploadDocument: (
    file: File,
    source?: string
  ): Promise<ApiResponse<string>> => {
    const formData = new FormData();
    formData.append('file', file);
    if (source) formData.append('source', source);
    return axiosClient.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getStats: (): Promise<ApiResponse<GraphStats>> => axiosClient.get('/stats'),

  checkHealth: (): Promise<ApiResponse<string>> => axiosClient.get('/health'),

  clearGraph: (): Promise<ApiResponse<string>> => axiosClient.delete('/clear'),

  getRelatedEntities: (
    entityName: string,
    maxHops = 2,
    maxResults = 20
  ): Promise<ApiResponse<RelatedEntityInfo[]>> =>
    axiosClient.get(`/entities/${entityName}/related`, {
      params: { maxHops, maxResults },
    }),
};
EOF

echo "-> Creating src/config/index.ts"
cat <<'EOF' > src/config/index.ts
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
EOF

echo "-> Creating src/layouts/PageLayout.tsx"
cat <<'EOF' > src/layouts/PageLayout.tsx
import React from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { Layout, Menu, Typography, Avatar } from 'antd';
import {
  DashboardOutlined,
  ApartmentOutlined,
  LineChartOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '@/store';
import { toggleSider } from '@/store/uiSlice';

const { Header, Content, Footer, Sider } = Layout;
const { Title } = Typography;

const navigationItems = [
  {
    key: '/dashboard',
    icon: <DashboardOutlined />,
    label: '智能问答',
  },
  {
    key: '/explorer',
    icon: <ApartmentOutlined />,
    label: '图谱探索',
  },
  {
    key: '/statistics',
    icon: <LineChartOutlined />,
    label: '系统状态',
  },
];

const PageLayout: React.FC = () => {
  const location = useLocation();
  const dispatch = useDispatch();
  const isSiderCollapsed = useSelector(
    (state: RootState) => state.ui.isSiderCollapsed
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={isSiderCollapsed}
        onCollapse={(value) => dispatch(toggleSider(value))}
      >
        <div className="h-16 flex items-center justify-center">
          <Title
            level={4}
            className="text-white !mb-0 transition-opacity duration-300"
            style={{ opacity: isSiderCollapsed ? 0 : 1 }}
          >
            GraphRAG
          </Title>
        </div>
        <Menu
          theme="dark"
          selectedKeys={[location.pathname]}
          mode="inline"
        >
          {navigationItems.map((item) => (
            <Menu.Item key={item.key} icon={item.icon}>
              <Link to={item.key}>{item.label}</Link>
            </Menu.Item>
          ))}
        </Menu>
      </Sider>
      <Layout>
        <Header className="bg-white p-0 px-lg flex justify-between items-center">
          <Title level={3} className="!m-0">
            GraphRAG Gemini Studio
          </Title>
          <div>
            <Avatar icon={<UserOutlined />} />
            <span className="ml-sm">Admin</span>
          </div>
        </Header>
        <Content className="m-lg">
          <div className="p-lg bg-white min-h-full rounded-md">
            <Outlet />
          </div>
        </Content>
        <Footer style={{ textAlign: 'center' }}>
          GraphRAG Gemini Studio ©{new Date().getFullYear()} | Version 1.0.0
        </Footer>
      </Layout>
    </Layout>
  );
};

export default PageLayout;
EOF

echo "-> Creating src/store/index.ts"
cat <<'EOF' > src/store/index.ts
import { configureStore } from '@reduxjs/toolkit';
import uiReducer from './uiSlice';

export const store = configureStore({
  reducer: {
    ui: uiReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
EOF

echo "-> Creating src/store/uiSlice.ts"
cat <<'EOF' > src/store/uiSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UiState {
  isSiderCollapsed: boolean;
}

const initialState: UiState = {
  isSiderCollapsed: false,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSider(state, action: PayloadAction<boolean>) {
      state.isSiderCollapsed = action.payload;
    },
  },
});

export const { toggleSider } = uiSlice.actions;
export default uiSlice.reducer;
EOF

echo "-> Creating src/components/FileUploader.tsx"
cat <<'EOF' > src/components/FileUploader.tsx
import React from 'react';
import { InboxOutlined } from '@ant-design/icons';
import { App, UploadProps } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { graphRagApi } from '@/api/graphRagApi';
import Dragger from 'antd/es/upload/Dragger';

const FileUploader: React.FC = () => {
  const { message } = App.useApp();

  const uploadMutation = useMutation({
    mutationFn: (file: File) => graphRagApi.uploadDocument(file),
    onSuccess: (response) => {
      if (response.code === 0) {
        message.success(response.data || '文件上传成功!');
      } else {
        message.error(response.message);
      }
    },
    onError: (error) => {
      message.error(`上传失败: ${error.message}`);
    },
  });

  const props: UploadProps = {
    name: 'file',
    multiple: false,
    customRequest: ({ file, onSuccess, onError }) => {
      if (typeof file === 'string') {
        onError?.(new Error('Invalid file type'));
        return;
      }
      uploadMutation
        .mutateAsync(file)
        .then(() => {
          onSuccess?.('ok');
        })
        .catch((err) => {
          onError?.(err);
        });
    },
    maxCount: 1,
  };

  return (
    <Dragger {...props} disabled={uploadMutation.isPending}>
      <p className="ant-upload-drag-icon">
        <InboxOutlined />
      </p>
      <p className="ant-upload-text">点击或拖拽文件到此处上传</p>
      <p className="ant-upload-hint">
        支持单文件上传。后台将自动解析并构建知识图谱。
      </p>
    </Dragger>
  );
};

export default FileUploader;
EOF

echo "-> Creating src/components/ResultDisplay.tsx"
cat <<'EOF' > src/components/ResultDisplay.tsx
import React from 'react';
import { Card, Typography, Descriptions, Tag, Collapse } from 'antd';
import { GraphRagResponse } from '@/api/types';
import { CheckCircleOutlined, FileTextOutlined } from '@ant-design/icons';

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
          <Descriptions.Item label="耗时">
            {data.processingTimeMs} ms
          </Descriptions.Item>
        </Descriptions>
        <Paragraph className="text-base">{data.answer}</Paragraph>
      </Card>

      <Collapse accordion>
        <Panel
          header={`推理依据 (${data.segments.length}个片段)`}
          key="1"
        >
          <div className="space-y-md max-h-96 overflow-y-auto pr-sm">
            {data.segments.map((seg, index) => (
              <Card key={index} size="small">
                <div className="flex justify-between items-start">
                  <FileTextOutlined className="mr-sm mt-1" />
                  <Paragraph className="flex-1 !mb-0">{seg.content}</Paragraph>
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
EOF

echo "-> Creating src/components/GraphVisualization/index.tsx"
cat <<'EOF' > src/components/GraphVisualization/index.tsx
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
            'text-margin-y': '5px',
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
EOF

echo "-> Creating src/pages/Dashboard/index.tsx"
cat <<'EOF' > src/pages/Dashboard/index.tsx
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
EOF

echo "-> Creating src/pages/GraphExplorer/index.tsx"
cat <<'EOF' > src/pages/GraphExplorer/index.tsx
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
      if (response.code === 0) {
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
EOF

echo "-> Creating src/pages/Statistics/index.tsx"
cat <<'EOF' > src/pages/Statistics/index.tsx
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
EOF

# 4. Final Instructions
echo ""
echo "✅ Project '${PROJECT_NAME}' created successfully!"
echo ""
echo "Next steps:"
echo "1. cd ${PROJECT_NAME}"
echo "2. npm install"
echo "3. npm run dev"
echo ""