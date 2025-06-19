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
