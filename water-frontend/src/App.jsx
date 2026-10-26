import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Layout from './Layout.jsx';
import { Spinner } from './ui.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Profile from './pages/Profile.jsx';
import InvoiceDetail from './pages/InvoiceDetail.jsx';
import Dashboard from './pages/resident/Dashboard.jsx';
import MyInvoices from './pages/resident/MyInvoices.jsx';
import Overview from './pages/admin/Overview.jsx';
import Apartments from './pages/admin/Apartments.jsx';
import Households from './pages/admin/Households.jsx';
import Residents from './pages/admin/Residents.jsx';
import Usage from './pages/admin/Usage.jsx';
import Tariffs from './pages/admin/Tariffs.jsx';
import Cycles from './pages/admin/Cycles.jsx';
import CycleDetail from './pages/admin/CycleDetail.jsx';

function Protected({ role, children }) {
  const { user, loading } = useAuth();
  if (loading) return <Spinner text="Signing you in..." />;
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return children;
}

function Home() {
  const { user, loading } = useAuth();
  if (loading) return <Spinner text="Signing you in..." />;
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/" element={<Home />} />

      <Route element={<Protected><Layout /></Protected>}>
        <Route path="/profile" element={<Profile />} />
        <Route path="/invoices/:id" element={<InvoiceDetail />} />

        <Route path="/dashboard" element={<Protected role="RESIDENT"><Dashboard /></Protected>} />
        <Route path="/my-invoices" element={<Protected role="RESIDENT"><MyInvoices /></Protected>} />

        <Route path="/admin" element={<Protected role="ADMIN"><Overview /></Protected>} />
        <Route path="/admin/apartments" element={<Protected role="ADMIN"><Apartments /></Protected>} />
        <Route path="/admin/households" element={<Protected role="ADMIN"><Households /></Protected>} />
        <Route path="/admin/residents" element={<Protected role="ADMIN"><Residents /></Protected>} />
        <Route path="/admin/usage" element={<Protected role="ADMIN"><Usage /></Protected>} />
        <Route path="/admin/tariffs" element={<Protected role="ADMIN"><Tariffs /></Protected>} />
        <Route path="/admin/cycles" element={<Protected role="ADMIN"><Cycles /></Protected>} />
        <Route path="/admin/cycles/:id" element={<Protected role="ADMIN"><CycleDetail /></Protected>} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
