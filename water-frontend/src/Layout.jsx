import { useEffect, useId, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { get } from './api.js';
import { useAuth } from './auth.jsx';
import { AdminProvider, useAdmin } from './adminContext.jsx';
import AquaBotWidget from './AquaBotWidget.jsx';

/**
 * App shell (sidebar, top bar and page outlet)
 * ----------------------------------------------------------------------------
 * UI-only refresh. Nothing in the wiring has changed:
 *  - Same imports, same default export (Layout), same AdminProvider wrapping.
 *  - Same routes, same <Outlet />, same sign-out flow, same unread-count request
 *    (GET /api/v1/notifications/unread-count, refreshed every 30 s and on the
 *    'notifications-changed' event).
 *  - All existing class names are kept (shell, sidebar, nav, nav-link, topbar,
 *    main, content, bell, ...), so your global CSS and every page keep working.
 *
 * What this file adds is a thin visual layer. Every added rule starts with
 * .lyt-shell, and it only changes colours, type and small spacing. It does not
 * change the widths or positions your global CSS sets for the desktop layout.
 *
 * New: grouped sidebar sections, a skip-to-content link, and a slide-in menu on
 * small screens. If the slide-in menu ever clashes with your global responsive
 * CSS, set ENABLE_MOBILE_DRAWER to false and the original behaviour returns.
 */

const BRAND_NAME = 'AquaFlow';
const ENABLE_MOBILE_DRAWER = true;

const ADMIN_NAV = [
  { to: '/admin', label: 'Overview', end: true, group: 'Manage' },
  { to: '/admin/apartments', label: 'Apartments', group: 'Manage' },
  { to: '/admin/households', label: 'Households', group: 'Manage' },
  { to: '/admin/residents', label: 'Residents', group: 'Manage' },
  { to: '/admin/usage', label: 'Water usage', group: 'Water and billing' },
  { to: '/admin/tariffs', label: 'Tariff plans', group: 'Water and billing' },
  { to: '/admin/cycles', label: 'Billing cycles', group: 'Water and billing' },
  { to: '/admin/alerts', label: 'Alert settings', group: 'Alerts' },
  { to: '/notifications', label: 'Alerts', group: 'Alerts' },
  { to: '/profile', label: 'My profile', group: 'Account' },
];

const RESIDENT_NAV = [
  { to: '/dashboard', label: 'My usage', end: true, group: 'Your home' },
  { to: '/my-invoices', label: 'My invoices', group: 'Your home' },
  { to: '/notifications', label: 'Alerts', group: 'Your home' },
  { to: '/profile', label: 'My profile', group: 'Account' },
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

function BrandDrop() {
  // Unique gradient id, so it can never clash with another SVG on the page.
  const gradientId = useId();
  return (
    <svg viewBox="0 0 40 48" fill="none" aria-hidden="true">
      <path d="M20 2C15 10 5 21 5 30a15 15 0 0 0 30 0C35 21 25 10 20 2Z" fill={`url(#${gradientId})`} />
      <path d="M12 30a8 8 0 0 0 8 8" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
      <defs>
        <linearGradient id={gradientId} x1="7" y1="4" x2="34" y2="43" gradientUnits="userSpaceOnUse">
          <stop stopColor="#19E5FA" />
          <stop offset="1" stopColor="#0875CE" />
        </linearGradient>
      </defs>
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
  const [menuOpen, setMenuOpen] = useState(false);
  const nav = isAdmin ? ADMIN_NAV : RESIDENT_NAV;
  const activePage = nav.find((item) => item.end ? location.pathname === item.to : location.pathname.startsWith(item.to));

  // Group the links for the sidebar while keeping their original order.
  const groups = [];
  nav.forEach((item) => {
    const last = groups[groups.length - 1];
    if (last && last.label === item.group) last.items.push(item);
    else groups.push({ label: item.group, items: [item] });
  });

  // Close the mobile menu after navigating, and when Escape is pressed.
  useEffect(() => { setMenuOpen(false); }, [location.pathname]);
  useEffect(() => {
    if (!menuOpen) return undefined;
    const onKey = (event) => { if (event.key === 'Escape') setMenuOpen(false); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [menuOpen]);

  const signOut = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const initial = (user?.name || '?').charAt(0).toUpperCase();

  return (
    <div className={`shell lyt-shell${ENABLE_MOBILE_DRAWER ? ' lyt-drawer' : ''}${menuOpen ? ' is-menu-open' : ''}`}>
      <style>{STYLES}</style>
      <a className="lyt-skip" href="#lyt-main">Skip to main content</a>

      <aside className="sidebar" id="lyt-sidebar">
        <Link className="brand" to={isAdmin ? '/admin' : '/dashboard'}>
          <span className="brand-mark" aria-hidden="true"><BrandDrop /></span>
          <div>
            <div className="brand-name">{BRAND_NAME}</div>
            <div className="brand-sub">{isAdmin ? 'ADMIN CONSOLE' : 'RESIDENT PORTAL'}</div>
          </div>
        </Link>
        {isAdmin && (
          <div className="sidebar-apartment">
            <span className="sidebar-apartment-kicker">Current apartment</span>
            <strong>{admin?.apartment?.name || 'Select apartment'}</strong>
          </div>
        )}
        <nav className="nav" aria-label="Sidebar navigation">
          {groups.map((group) => (
            <div className="lyt-group" key={group.label}>
              {groups.length > 1 && <div className="lyt-group-label">{group.label}</div>}
              {group.items.map((n) => (
                <NavLink key={n.to} to={n.to} end={n.end} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                  <NavIcon name={n.label} />
                  <span>{n.label}</span>
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="user-chip">
            <div className="avatar">{initial}</div>
            <div className="user-meta">
              <div className="user-name" title={user?.name}>{user?.name}</div>
              <div className="muted small">{user?.role}</div>
            </div>
          </div>
          <button className="btn btn-ghost btn-block" onClick={signOut}>Sign out</button>
        </div>
      </aside>

      {ENABLE_MOBILE_DRAWER && menuOpen && (
        <button type="button" className="lyt-backdrop" aria-label="Close menu" onClick={() => setMenuOpen(false)} />
      )}

      <div className="main">
        <header className="topbar">
          <div className="lyt-topbar-left">
            {ENABLE_MOBILE_DRAWER && (
              <button
                type="button"
                className="lyt-menu-btn"
                aria-label={menuOpen ? 'Close menu' : 'Open menu'}
                aria-expanded={menuOpen}
                aria-controls="lyt-sidebar"
                onClick={() => setMenuOpen((open) => !open)}
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                  {menuOpen ? <path d="M6 6l12 12M18 6 6 18" /> : <path d="M4 7h16M4 12h16M4 17h16" />}
                </svg>
              </button>
            )}
            <div className="topbar-context">
              <span>Workspace</span>
              <span className="topbar-context-separator">/</span>
              <strong>{activePage?.label || (isAdmin ? 'Admin Console' : 'Resident Portal')}</strong>
            </div>
          </div>
          <div className="row gap">
            <ApartmentSelect />
            <Bell />
            <Link className="topbar-user" to="/profile" aria-label={`View profile for ${user?.name}`}>
              <span className="topbar-avatar">{initial}</span>
              <span>
                <strong>{user?.name}</strong>
                <small>{isAdmin ? 'Administrator' : 'Resident'}</small>
              </span>
            </Link>
          </div>
        </header>
        <main className="content" id="lyt-main" tabIndex={-1}>
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

/* ------------------------------------------------------------------ */
/*  Styles: every selector starts with .lyt-shell                      */
/*  Colours and type only. Layout metrics stay in your global CSS.     */
/* ------------------------------------------------------------------ */

const STYLES = `
.lyt-shell {
  --lyt-ink: #0b2540;
  --lyt-tide: #0a6ea8;
  --lyt-cyan: #7fd6f0;
  --lyt-line: #d8e5ed;
  --lyt-muted: #5b7186;
  --lyt-brass: #a9681d;
}
.lyt-shell .lyt-skip { position: absolute; left: 12px; top: -60px; z-index: 2000; padding: 10px 16px; border-radius: 8px; background: #fff; color: var(--lyt-ink); font-weight: 700; text-decoration: none; box-shadow: 0 8px 24px rgba(11, 37, 64, .3); transition: top .15s; }
.lyt-shell .lyt-skip:focus { top: 12px; }
.lyt-shell #lyt-main:focus { outline: none; }
.lyt-shell a:focus-visible, .lyt-shell button:focus-visible, .lyt-shell select:focus-visible { outline: 3px solid rgba(10, 110, 168, .5); outline-offset: 2px; }

/* Sidebar */
.lyt-shell .sidebar { background: var(--lyt-ink); color: #fff; }
.lyt-shell .sidebar .brand { color: #fff; text-decoration: none; }
.lyt-shell .sidebar .brand-mark { width: 32px; height: 39px; padding: 0; display: grid; place-items: center; border-radius: 0; background: none; box-shadow: none; }
.lyt-shell .sidebar .brand-mark svg { width: 100%; height: 100%; }
.lyt-shell .sidebar .brand-name { color: #fff; font-size: 20px; font-weight: 800; letter-spacing: -.5px; line-height: 1.1; }
.lyt-shell .sidebar .brand-sub { margin-top: 3px; color: rgba(255, 255, 255, .62); font-size: 10.5px; font-weight: 600; letter-spacing: .08em; }
.lyt-shell .sidebar .sidebar-apartment { border: 1px solid rgba(255, 255, 255, .14); background: rgba(255, 255, 255, .06); border-radius: 10px; color: #fff; }
.lyt-shell .sidebar .sidebar-apartment-kicker { display: block; margin-bottom: 2px; color: rgba(255, 255, 255, .62); font-size: 12px; font-weight: 600; letter-spacing: 0; text-transform: none; }
.lyt-shell .lyt-group + .lyt-group { margin-top: 14px; }
.lyt-shell .lyt-group-label { padding: 0 12px 6px; color: rgba(255, 255, 255, .5); font-size: 12px; font-weight: 700; }
.lyt-shell .sidebar .nav-link { color: rgba(255, 255, 255, .8); border-radius: 8px; transition: background .15s, color .15s; }
.lyt-shell .sidebar .nav-link:hover { background: rgba(255, 255, 255, .08); color: #fff; }
.lyt-shell .sidebar .nav-link.active { background: rgba(255, 255, 255, .13); color: #fff; box-shadow: inset 3px 0 0 var(--lyt-cyan); }
.lyt-shell .sidebar .nav-link svg { width: 19px; height: 19px; flex: none; }
.lyt-shell .sidebar .sidebar-foot { border-top: 1px solid rgba(255, 255, 255, .12); }
.lyt-shell .sidebar .user-name { color: #fff; font-weight: 700; }
.lyt-shell .sidebar .muted { color: rgba(255, 255, 255, .62); }
.lyt-shell .sidebar .avatar { background: rgba(255, 255, 255, .16); color: #fff; }
.lyt-shell .sidebar .btn-ghost { border-color: rgba(255, 255, 255, .3); background: transparent; color: #fff; }
.lyt-shell .sidebar .btn-ghost:hover { background: rgba(255, 255, 255, .12); }

/* Top bar */
.lyt-shell .topbar { background: #fff; border-bottom: 1px solid var(--lyt-line); }
.lyt-shell .lyt-topbar-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.lyt-shell .lyt-menu-btn { display: none; width: 40px; height: 40px; padding: 0; align-items: center; justify-content: center; border: 1px solid var(--lyt-line); border-radius: 10px; background: #fff; color: var(--lyt-ink); cursor: pointer; }
.lyt-shell .lyt-menu-btn svg { width: 22px; height: 22px; }
.lyt-shell .topbar .topbar-context strong { color: var(--lyt-ink); }
.lyt-shell .topbar .bell-badge { background: var(--lyt-brass); color: #fff; }
.lyt-shell .topbar .topbar-avatar { background: var(--lyt-ink); color: #fff; }
.lyt-shell .lyt-backdrop { display: none; }

/* Slide-in menu on small screens (set ENABLE_MOBILE_DRAWER to false to turn off) */
@media (max-width: 900px) {
  .lyt-shell.lyt-drawer { display: block; }
  .lyt-shell.lyt-drawer .sidebar { position: fixed; top: 0; left: 0; bottom: 0; z-index: 1100; width: min(290px, 86vw); height: 100vh; height: 100dvh; max-height: none; display: flex; flex-direction: column; overflow-y: auto; transform: translateX(-102%); transition: transform .25s ease; box-shadow: none; }
  .lyt-shell.lyt-drawer.is-menu-open .sidebar { transform: translateX(0); box-shadow: 0 0 60px rgba(0, 0, 0, .45); }
  .lyt-shell.lyt-drawer .nav { display: block; flex: 1; overflow-y: auto; }
  .lyt-shell.lyt-drawer .main { width: 100%; margin-left: 0; min-width: 0; }
  .lyt-shell.lyt-drawer .lyt-menu-btn { display: inline-flex; }
  .lyt-shell.lyt-drawer .lyt-backdrop { display: block; position: fixed; inset: 0; z-index: 1050; border: 0; padding: 0; background: rgba(11, 37, 64, .5); cursor: pointer; }
  .lyt-shell.lyt-drawer .topbar-user > span:last-child { display: none; }
}
@media (max-width: 560px) {
  .lyt-shell.lyt-drawer .topbar-context span:not(.topbar-context-separator):first-child, .lyt-shell.lyt-drawer .topbar-context-separator { display: none; }
  .lyt-shell.lyt-drawer .apt-select .muted { display: none; }
}
@media (prefers-reduced-motion: reduce) {
  .lyt-shell *, .lyt-shell *::before, .lyt-shell *::after { transition: none !important; animation: none !important; }
}
`;