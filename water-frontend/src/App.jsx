import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import { portalPathFor } from './navigation.js';
import Layout from './Layout.jsx';
import { Spinner } from './ui.jsx';

const PublicHome = lazy(() => import('./pages/PublicHome.jsx'));
const Login = lazy(() => import('./pages/Login.jsx'));
const Register = lazy(() => import('./pages/Register.jsx'));
const Profile = lazy(() => import('./pages/Profile.jsx'));
const Notifications = lazy(() => import('./pages/Notifications.jsx'));
const InvoiceDetail = lazy(() => import('./pages/InvoiceDetail.jsx'));
const Dashboard = lazy(() => import('./pages/resident/Dashboard.jsx'));
const MyInvoices = lazy(() => import('./pages/resident/MyInvoices.jsx'));
const Overview = lazy(() => import('./pages/admin/Overview.jsx'));
const Apartments = lazy(() => import('./pages/admin/Apartments.jsx'));
const Households = lazy(() => import('./pages/admin/Households.jsx'));
const Residents = lazy(() => import('./pages/admin/Residents.jsx'));
const Usage = lazy(() => import('./pages/admin/Usage.jsx'));
const Tariffs = lazy(() => import('./pages/admin/Tariffs.jsx'));
const Cycles = lazy(() => import('./pages/admin/Cycles.jsx'));
const CycleDetail = lazy(() => import('./pages/admin/CycleDetail.jsx'));
const AlertSettings = lazy(() => import('./pages/admin/AlertSettings.jsx'));

function Protected({ role, children }) {
  const { user, loading } = useAuth();
  if (loading) return <Spinner text="Signing you in..." />;
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to={portalPathFor(user)} replace />;
  return children;
}

export default function App() {
  return (
    <Suspense fallback={<Spinner text="Loading page..." />}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/" element={<PublicHome />} />

        <Route element={<Protected><Layout /></Protected>}>
          <Route path="/profile" element={<Profile />} />
          <Route path="/notifications" element={<Notifications />} />
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
          <Route path="/admin/alerts" element={<Protected role="ADMIN"><AlertSettings /></Protected>} />
          <Route path="/admin/cycles/:id" element={<Protected role="ADMIN"><CycleDetail /></Protected>} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
