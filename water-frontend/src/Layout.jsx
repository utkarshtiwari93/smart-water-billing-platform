import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { get } from './api.js';
import { useAuth } from './auth.jsx';
import { AdminProvider, useAdmin } from './adminContext.jsx';
import AquaBotWidget from './AquaBotWidget.jsx';

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

const NAV_ICONS = {
  Overview: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="5" rx="1.5" /><rect x="14" y="12" width="7" height="9" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /></>,
  Apartments: <><path d="M3 21h18M5 21V5l7-2v18m0-14h7v14M8 7h1m-1 4h1m-1 4h1m7-3h1m-1 4h1" /></>,
  Households: <><path d="m3 10 9-7 9 7" /><path d="M5 9v12h14V9M9 21v-7h6v7" /></>,
  Residents: <><circle cx="9" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M3 20v-1a6 6 0 0 1 12 0v1zm13-5a5 5 0 0 1 5 5h-3" /></>,
  'Water usage': <><circle cx="12" cy="12" r="9" /><path d="M12 12 16 8m-4 4v5m-5 0h10" /></>,
  'Tariff plans': <><path d="M4 7h16M4 12h10M4 17h7" /><circle cx="19" cy="17" r="2" /></>,
  'Billing cycles': <><rect x="3.5" y="5" width="17" height="16" rx="2" /><path d="M7.5 3v4m9-4v4M3.5 9.5h17M8 13h3m-3 4h3m3-4h2" /></>,
  'Alert settings': <><path d="M18 10a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 22h4" /></>,
  Alerts: <><path d="M18 10a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 22h4" /></>,
  'My usage': <><path d="M4 19V5m0 14h17" /><path d="m7 15 4-4 3 2 6-7" /></>,
  'My invoices': <><path d="M7 3h8l4 4v14H7z" /><path d="M15 3v5h5M10 12h6m-6 4h6" /></>,
  'My profile': <><circle cx="12" cy="8" r="3.5" /><path d="M4 21a8 8 0 0 1 16 0" /></>,
};

function NavIcon({ name }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {NAV_ICONS[name] || <circle cx="12" cy="12" r="9" />}
    </svg>
  );
}

// Shows the number of unread alerts with reactive refresh.
function Bell() {
  const [count, setCount] = useState(0);
  const [error, setError] = useState(null);

  useEffect(() => {
    let alive = true;
    const load = () => get('/api/v1/notifications/unread-count')
      .then((r) => {
        if (alive) {
          setCount(r.unread);
          setError(null);
        }
      })
      .catch((e) => alive && setError(e));
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
    <Link
      to="/notifications"
      className="bell"
      title={error ? `Alert count unavailable: ${error.message}` : 'Alerts'}
      aria-label={error ? 'Alerts, unread count unavailable' : `Alerts${count > 0 ? `, ${count} unread` : ''}`}
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M18 10a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
        <path d="M13.73 21a2 2 0 0 1-3.46 0" />
      </svg>
      {count > 0 && <span className="bell-badge">{count}</span>}
      {error && <span className="bell-error" aria-hidden="true">!</span>}
    </Link>
  );
}

function ApartmentSelect() {
  const admin = useAdmin();
  if (!admin) return null;
  return (
    <div className="apt-select">
      <span className="muted small">Apartment</span>
      <select value={admin.apartmentId || ''} onChange={(e) => admin.setApartmentId(e.target.value)} aria-label="Select active apartment">
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
  const location = useLocation();
  const admin = useAdmin();
  const nav = isAdmin ? ADMIN_NAV : RESIDENT_NAV;
  const activePage = nav.find((item) => item.end ? location.pathname === item.to : location.pathname.startsWith(item.to));

  const signOut = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="shell">
      <aside className="sidebar">
        <Link className="brand" to={isAdmin ? '/admin' : '/dashboard'}>
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" fill="none">
              <path d="M16 3.5C12.2 9.1 6.8 14.2 6.8 20a9.2 9.2 0 0 0 18.4 0C25.2 14.2 19.8 9.1 16 3.5Z" fill="currentColor" />
              <path d="M11.5 20.5a4.7 4.7 0 0 0 4.7 4.7" stroke="#D8F7F1" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
          </span>
          <div>
            <div className="brand-name">Smart Water</div>
            <div className="brand-sub">{isAdmin ? 'ADMIN CONSOLE' : 'RESIDENT PORTAL'}</div>
          </div>
        </Link>
        {isAdmin && (
          <div className="sidebar-apartment">
            <span className="sidebar-apartment-kicker">CURRENT APARTMENT</span>
            <strong>{admin?.apartment?.name || 'Select apartment'}</strong>
          </div>
        )}
        <nav className="nav" aria-label="Sidebar navigation">
          {nav.map((n) => (
            <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              <NavIcon name={n.label} />
              <span>{n.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="user-chip">
            <div className="avatar">{(user?.name || '?').charAt(0).toUpperCase()}</div>
            <div className="user-meta">
              <div className="user-name" title={user?.name}>{user?.name}</div>
              <div className="muted small">{user?.role}</div>
            </div>
          </div>
          <button className="btn btn-ghost btn-block" onClick={signOut}>Sign out</button>
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <div className="topbar-context">
            <span>Workspace</span>
            <span className="topbar-context-separator">/</span>
            <strong>{activePage?.label || (isAdmin ? 'Admin Console' : 'Resident Portal')}</strong>
          </div>
          <div className="row gap">
            <ApartmentSelect />
            <Bell />
            <Link className="topbar-user" to="/profile" aria-label={`View profile for ${user?.name}`}>
              <span className="topbar-avatar">{(user?.name || '?').charAt(0).toUpperCase()}</span>
              <span>
                <strong>{user?.name}</strong>
                <small>{isAdmin ? 'Administrator' : 'Resident'}</small>
              </span>
            </Link>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
      <AquaBotWidget />
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
