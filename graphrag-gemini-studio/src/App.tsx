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
