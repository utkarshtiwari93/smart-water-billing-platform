import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { get } from './api.js';
import { useAuth } from './auth.jsx';
import { AdminProvider, useAdmin } from './adminContext.jsx';

const ADMIN_NAV = [
  { to: '/admin', label: 'Overview', end: true },
  { to: '/admin/apartments', label: 'Apartments' },
  { to: '/admin/households', label: 'Households' },
  { to: '/admin/residents', label: 'Residents' },
  { to: '/admin/usage', label: 'Water usage' },
  { to: '/admin/tariffs', label: 'Tariff plans' },
  { to: '/admin/cycles', label: 'Billing cycles' },
  { to: '/admin/alerts', label: 'Alert settings' },
  { to: '/notifications', label: 'Alerts' },
  { to: '/profile', label: 'My profile' },
];

const RESIDENT_NAV = [
  { to: '/dashboard', label: 'My usage', end: true },
  { to: '/my-invoices', label: 'My invoices' },
  { to: '/notifications', label: 'Alerts' },
  { to: '/profile', label: 'My profile' },
];

// Shows the number of unread alerts. Refreshes every 30 seconds and whenever the alerts page changes something.
function Bell() {
  const [count, setCount] = useState(0);
  useEffect(() => {
    let alive = true;
    const load = () => get('/api/v1/notifications/unread-count')
      .then((r) => alive && setCount(r.unread))
      .catch(() => {});
    load();
    const timer = setInterval(load, 30000);
    window.addEventListener('notifications-changed', load);
    return () => {
      alive = false;
      clearInterval(timer);
      window.removeEventListener('notifications-changed', load);
    };
  }, []);
  return (
    <Link to="/notifications" className="bell" title="Alerts">
      Alerts {count > 0 && <span className="bell-badge">{count}</span>}
    </Link>
  );
}

function ApartmentSelect() {
  const admin = useAdmin();
  if (!admin) return null;
  return (
    <div className="apt-select">
      <span className="muted small">Apartment</span>
      <select value={admin.apartmentId || ''} onChange={(e) => admin.setApartmentId(e.target.value)}>
        {admin.apartments.map((a) => (
          <option key={a.id} value={a.id}>{a.name} (#{a.id})</option>
        ))}
      </select>
    </div>
  );
}

function Shell() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const nav = isAdmin ? ADMIN_NAV : RESIDENT_NAV;

  const signOut = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">W</span>
          <div>
            <div className="brand-name">Water Billing</div>
            <div className="brand-sub">{isAdmin ? 'Admin console' : 'Resident portal'}</div>
          </div>
        </div>
        <nav className="nav">
          {nav.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              {n.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="user-chip">
            <div className="avatar">{(user.name || '?').charAt(0).toUpperCase()}</div>
            <div className="user-meta">
              <div className="user-name" title={user.name}>{user.name}</div>
              <div className="muted small">{user.role}</div>
            </div>
          </div>
          <button className="btn btn-ghost btn-block" onClick={signOut}>Sign out</button>
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <ApartmentSelect />
          <Bell />
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default function Layout() {
  const { isAdmin } = useAuth();
  return isAdmin ? (
    <AdminProvider>
      <Shell />
    </AdminProvider>
  ) : (
    <Shell />
  );
}
