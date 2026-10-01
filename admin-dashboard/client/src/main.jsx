import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import './styles.css';
import { AuthProvider, useAuth } from './lib/auth.jsx';
import { ToastProvider, Spinner } from './components/ui.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Overview from './pages/Overview.jsx';
import Traders from './pages/Traders.jsx';
import TraderDetail from './pages/TraderDetail.jsx';
import RiskCenter from './pages/RiskCenter.jsx';
import AutoPilot from './pages/AutoPilot.jsx';
import Positions from './pages/Positions.jsx';
import Trades from './pages/Trades.jsx';
import Funds from './pages/Funds.jsx';
import AiCouncil from './pages/AiCouncil.jsx';
import Copilot from './pages/Copilot.jsx';
import Markets from './pages/Markets.jsx';
import Strategies from './pages/Strategies.jsx';
import Backtests from './pages/Backtests.jsx';
import Brokers from './pages/Brokers.jsx';
import DailyReport from './pages/DailyReport.jsx';
import AuditLog from './pages/AuditLog.jsx';
import SettingsPage from './pages/Settings.jsx';

try {
  document.documentElement.dataset.theme = localStorage.getItem('tp_theme') || 'dark';
} catch { /* storage blocked */ }

function Protected({ children }) {
  const { admin, ready } = useAuth();
  if (!ready) return <Spinner />;
  return admin ? children : <Navigate to="/login" replace />;
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route element={<Protected><Layout /></Protected>}>
              <Route index element={<Overview />} />
              <Route path="traders" element={<Traders />} />
              <Route path="traders/:id" element={<TraderDetail />} />
              <Route path="risk" element={<RiskCenter />} />
              <Route path="autopilot" element={<AutoPilot />} />
              <Route path="positions" element={<Positions />} />
              <Route path="trades" element={<Trades />} />
              <Route path="funds" element={<Funds />} />
              <Route path="ai-council" element={<AiCouncil />} />
              <Route path="copilot" element={<Copilot />} />
              <Route path="markets" element={<Markets />} />
              <Route path="strategies" element={<Strategies />} />
              <Route path="backtests" element={<Backtests />} />
              <Route path="brokers" element={<Brokers />} />
              <Route path="reports" element={<DailyReport />} />
              <Route path="audit" element={<AuditLog />} />
              <Route path="settings" element={<SettingsPage />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
