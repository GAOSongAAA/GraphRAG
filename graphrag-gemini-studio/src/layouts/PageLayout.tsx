import React from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { Layout, Menu, Typography, Avatar } from 'antd';
import {
  DashboardOutlined,
  ApartmentOutlined,
  LineChartOutlined,
  ToolOutlined,
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
  {
    key: '/tools',
    icon: <ToolOutlined />,
    label: '工具',
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
