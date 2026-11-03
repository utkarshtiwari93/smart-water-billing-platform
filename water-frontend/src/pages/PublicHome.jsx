import { useId } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { portalPathFor } from '../navigation.js';
import './PublicHome.css';

/**
 * AquaFlow public landing page
 * ----------------------------
 * UI-only refresh of the existing public home page.
 * - Keeps the existing authentication hook and portal redirect logic.
 * - Keeps the existing /login and /register routes.
 * - Uses local SVG icons; no icon package or new dependency is required.
 * - All page styles are scoped under .af-home in PublicHome.css.
 */

function Watermark({ className = '' }) {
  // Unique SVG gradient ID prevents collisions when the header and footer both render the logo.
  const gradientId = useId();
  return (
    <svg className={className} viewBox="0 0 40 48" fill="none" aria-hidden="true">
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

function Brand({ footer = false }) {
  return (
    <Link className={`af-home__brand${footer ? ' af-home__brand--footer' : ''}`} to="/" aria-label="AquaFlow home">
      <span className="af-home__brand-mark"><Watermark /></span>
      <span className="af-home__brand-copy">
        <strong>AquaFlow<sup>™</sup></strong>
        <small>Smart Water Management</small>
      </span>
    </Link>
  );
}

/** Consistent inline SVG icon set; every icon uses the same 24×24 viewBox. */
function Icon({ name, className = '' }) {
  const common = {
    viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor',
    strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round',
    'aria-hidden': true,
  };
  const icons = {
    usage: <><path d="M4 20V12h4v8M10 20V7h4v13M16 20V3h4v17" /><path d="M2 20h20" /></>,
    billing: <><path d="M6 3h9l5 5v13H6z" /><path d="M15 3v5h5M9 12h8M9 16h8" /></>,
    leak: <><path d="M12 3 2.8 20h18.4L12 3Z" /><path d="M12 9v5m0 3h.01" /></>,
    residents: <><circle cx="9" cy="8" r="3" /><path d="M3 20v-1a6 6 0 0 1 12 0v1" /><path d="M16 5.5a3 3 0 0 1 0 6M18 15a5 5 0 0 1 3 4.5" /></>,
    layers: <><path d="m12 3 9 5-9 5-9-5 9-5Z" /><path d="m3 12 9 5 9-5M3 16l9 5 9-5" /></>,
    sparkle: <><path d="m12 3 2.2 6.8L21 12l-6.8 2.2L12 21l-2.2-6.8L3 12l6.8-2.2L12 3Z" /><path d="m19 2 .8 2.2L22 5l-2.2.8L19 8l-.8-2.2L16 5l2.2-.8L19 2Z" /></>,
    arrow: <><path d="M5 12h14M13 6l6 6-6 6" /></>,
    check: <path d="m5 12 4 4L19 6" />,
    menu: <><path d="M4 7h16M4 12h16M4 17h16" /></>,
    shield: <><path d="M12 3 20 6v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6l8-3Z" /><path d="m8.5 12 2.3 2.3 4.8-5" /></>,
  };
  return <svg {...common} className={className}>{icons[name] || icons.usage}</svg>;
}

function SmartApartmentIllustration() {
  return (
    <svg className="af-home__building-art" viewBox="0 0 620 570" role="img" aria-label="Illustration of an apartment connected to a smart water monitoring network">
      <defs>
        <linearGradient id="af-building-front" x1="145" y1="115" x2="430" y2="500" gradientUnits="userSpaceOnUse">
          <stop stopColor="#F8FDFF" /><stop offset="1" stopColor="#8FC8E8" />
        </linearGradient>
        <linearGradient id="af-building-side" x1="390" y1="130" x2="530" y2="490" gradientUnits="userSpaceOnUse">
          <stop stopColor="#BFEAFF" /><stop offset="1" stopColor="#5D9FCB" />
        </linearGradient>
        <linearGradient id="af-glass" x1="0" y1="0" x2="1" y2="1">
          <stop stopColor="#064C83" /><stop offset="1" stopColor="#20BFE8" />
        </linearGradient>
        <linearGradient id="af-water" x1="0" y1="0" x2="1" y2="1">
          <stop stopColor="#28F5FF" /><stop offset="1" stopColor="#008FE5" />
        </linearGradient>
        <filter id="af-pipe-glow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="5" result="blur" /><feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
        <filter id="af-building-shadow" x="-30%" y="-30%" width="160%" height="170%">
          <feDropShadow dx="0" dy="18" stdDeviation="15" floodColor="#07518A" floodOpacity=".22" />
        </filter>
      </defs>

      {/* Apartment silhouette and roof */}
      <g filter="url(#af-building-shadow)">
        <path d="m108 145 254-92 165 60-254 96-165-64Z" fill="#DDF5FF" stroke="#fff" strokeWidth="5" />
        <path d="m108 145 165 64v285l-165-62V145Z" fill="url(#af-building-front)" stroke="#EAFBFF" strokeWidth="3" />
        <path d="m273 209 254-96v290l-254 91V209Z" fill="url(#af-building-side)" stroke="#EAFBFF" strokeWidth="3" />
        <path d="m108 145 254-92v27l-254 96v-31Z" fill="#A9DDF5" />
        {/* Floor slabs */}
        {[0, 1, 2, 3].map((floor) => {
          const y = 205 + floor * 66;
          return <g key={floor}>
            <path d={`m108 ${y} 165 63 254-95v-15l-254 95-165-63v15Z`} fill="#F5FDFF" stroke="#B5E8FC" strokeWidth="2" />
            <path d={`m128 ${y + 9} 125 48v42l-125-47v-43Z`} fill="url(#af-glass)" stroke="#E9FBFF" strokeWidth="3" />
            <path d={`m293 ${y + 55} 208-78v42l-208 78v-42Z`} fill="url(#af-glass)" stroke="#E9FBFF" strokeWidth="3" />
            <path d={`m190 ${y + 32}v43m-27-54v43m202-83v43m49-61v43`} stroke="#9BEAFF" strokeWidth="3" opacity=".8" />
            <path d={`m137 ${y + 12} 105 40m69 7 184-69`} stroke="#D9F8FF" strokeWidth="2" opacity=".65" />
          </g>;
        })}
        {/* Ground floor and entrance */}
        <path d="m273 473 254-91v57l-254 92v-58Z" fill="#C3E9FA" stroke="#F5FDFF" strokeWidth="3" />
        <path d="m335 450 83-30v77l-83 30v-77Z" fill="#073F70" stroke="#DFF8FF" strokeWidth="3" />
        <path d="m349 455 56-20v55l-56 20v-55Z" fill="#E7F8FF" opacity=".35" />
        <path d="m94 432 179 67 254-91v32l-254 96-179-68v-36Z" fill="#EAFBFF" stroke="#fff" strokeWidth="3" />
      </g>

      {/* Roof water tank */}
      <g>
        <ellipse cx="366" cy="64" rx="51" ry="15" fill="#22BFEA" stroke="#C7F8FF" strokeWidth="3" />
        <path d="M315 64v-32c0-9 23-17 51-17s51 8 51 17v32" fill="#087FC5" stroke="#B9F5FF" strokeWidth="3" />
        <ellipse cx="366" cy="32" rx="51" ry="15" fill="#119BD8" stroke="#B9F5FF" strokeWidth="3" />
        <path d="M334 27v31m64-31v31M353 15v-8h26v8" stroke="#83EFFF" strokeWidth="3" strokeLinecap="round" />
      </g>

      {/* Glowing connected water pipes */}
      <g filter="url(#af-pipe-glow)" stroke="url(#af-water)" strokeWidth="7" strokeLinecap="round" strokeLinejoin="round" fill="none">
        <path d="M366 79v104h-93v71h-67v80h67v77h-73v54" />
        <path d="M273 254h180v62" />
        <path d="M206 334h-49v72" />
      </g>
      <g fill="#05BCEB" stroke="#E8FFFF" strokeWidth="4">
        {[[273, 254], [453, 316], [157, 406], [200, 411], [273, 334]].map(([x, y]) => <circle key={`${x}-${y}`} cx={x} cy={y} r="12" />)}
      </g>
      <g fill="white">
        {[[273, 254], [453, 316], [157, 406], [200, 411], [273, 334]].map(([x, y]) => <circle key={`inner-${x}-${y}`} cx={x} cy={y} r="4" />)}
      </g>
      {/* Water drop on the building */}
      <path d="M366 127c-10 15-18 25-18 37a18 18 0 0 0 36 0c0-12-8-22-18-37Z" fill="#0CD9F4" stroke="white" strokeWidth="3" />
      <path d="M356 165a10 10 0 0 0 10 10" stroke="white" strokeWidth="3" strokeLinecap="round" />

      {/* Landscaping and base */}
      <path d="m65 494 208 78 292-105-208-77-292 104Z" fill="#D7F6FF" stroke="white" strokeWidth="4" />
      <path d="m65 494 208 78v17L65 511v-17Zm208 78 292-105v17L273 589v-17Z" fill="#4BA8D4" />
      {[85, 112, 493, 520].map((x) => <g key={x}>
        <path d={`M${x} 480v-28`} stroke="#278A62" strokeWidth="5" />
        <circle cx={x} cy="447" r="15" fill="#238C68" /><circle cx={x - 8} cy="455" r="10" fill="#2BA579" />
      </g>)}
      {[145, 470].map((x) => <g key={`light-${x}`}><path d={`M${x} 500v-19`} stroke="#EAFBFF" strokeWidth="3" /><circle cx={x} cy="477" r="5" fill="#FFF2A8" /></g>)}
    </svg>
  );
}

const FEATURES = [
  { icon: 'usage', title: 'Real-time Monitoring', text: 'Follow household water readings and consumption history in one clear view.' },
  { icon: 'billing', title: 'Automated Billing', text: 'Review billing cycles, household invoices, and itemized charges.' },
  { icon: 'leak', title: 'Leak & Anomaly Detection', text: 'Identify unusual usage patterns and bring important alerts into view.' },
  { icon: 'layers', title: 'Resident Benchmarking', text: 'Compare household consumption to understand usage patterns.' },
  { icon: 'sparkle', title: 'AI Assistant (AquaBot)', text: 'A helpful assistant experience for water-management information.' },
];

export default function PublicHome() {
  const { user } = useAuth();
  const portalPath = user ? portalPathFor(user) : '/login';
  const isAdmin = user?.role === 'ADMIN';

  return (
    <div className="af-home">
      {/* Main navigation: existing authentication-aware destinations are preserved. */}
      <header className="af-home__header">
        <div className="af-home__header-inner">
          <Brand />
          <nav className="af-home__nav" aria-label="Main navigation">
            <a className="af-home__nav-link af-home__nav-link--active" href="#home">Home</a>
            <a className="af-home__nav-link" href="#features">Features</a>
            <a className="af-home__nav-link" href="#for-residents">For Residents</a>
            <a className="af-home__nav-link" href="#for-admins">For Admins</a>
            <a className="af-home__nav-link" href="#about">About</a>
          </nav>
          <div className="af-home__header-actions">
            {user ? (
              <Link className="af-home__login-link" to={portalPath}>My Portal</Link>
            ) : (
              <Link className="af-home__login-link" to="/login">Log In</Link>
            )}
            <Link className="af-home__button af-home__button--primary af-home__button--nav" to={user ? portalPath : '/register'}>
              {user ? 'Open Portal' : 'Get Started'} <Icon name="arrow" />
            </Link>
          </div>
        </div>
      </header>

      <main>
        {/* Hero: headline, smart-apartment illustration, and supporting dashboard cards. */}
        <section className="af-home__hero" id="home">
          <div className="af-home__hero-inner">
            <div className="af-home__hero-copy">
              <span className="af-home__eyebrow">SMART WATER MANAGEMENT</span>
              <h1>Monitor,<br />Manage &amp;<br /><span>Save Water.</span></h1>
              <p className="af-home__hero-description">
                A smart platform to track water usage, automate billing, detect leaks, and manage your apartment’s water resources efficiently.
              </p>
              <div className="af-home__hero-actions">
                <Link className="af-home__button af-home__button--primary af-home__button--large" to={user ? portalPath : '/register'}>
                  {user ? 'Open Your Portal' : 'Get Started'} <Icon name="arrow" />
                </Link>
                <Link className="af-home__button af-home__button--secondary af-home__button--large" to={user ? portalPath : '/register'}>
                  Register Now <Icon name="arrow" />
                </Link>
              </div>
              <div className="af-home__hero-note"><Icon name="shield" /> Built for residents and apartment administrators</div>
            </div>

            <div className="af-home__visual" aria-label="Smart apartment water network illustration">
              <div className="af-home__visual-orbit af-home__visual-orbit--one" />
              <div className="af-home__visual-orbit af-home__visual-orbit--two" />
              <div className="af-home__building-stage"><SmartApartmentIllustration /></div>

              {/* Informational cards are illustrative UI labels, not live metrics. */}
              <div className="af-home__float-card af-home__float-card--usage">
                <span className="af-home__float-icon"><Icon name="usage" /></span>
                <span><small>WATER USAGE</small><strong>Usage Insights</strong><em>Explore reading history</em></span>
              </div>
              <div className="af-home__float-card af-home__float-card--billing">
                <span className="af-home__float-icon"><Icon name="billing" /></span>
                <span><small>SMART BILLING</small><strong>Automated Bills</strong><em>Clear household charges</em></span>
              </div>
              <div className="af-home__float-card af-home__float-card--households">
                <span className="af-home__float-icon"><Icon name="residents" /></span>
                <span><small>COMMUNITY</small><strong>Household Records</strong><em>Resident information</em></span>
              </div>
              <div className="af-home__float-card af-home__float-card--leaks">
                <span className="af-home__float-icon"><Icon name="leak" /></span>
                <span><small>SMART ALERTS</small><strong>Leak Monitoring</strong><em>Review unusual usage</em></span>
              </div>
            </div>
          </div>

          {/* Wave decoration is CSS-only and does not rely on an external image. */}
          <div className="af-home__wave af-home__wave--back" aria-hidden="true" />
          <div className="af-home__wave af-home__wave--front" aria-hidden="true" />
        </section>

        {/* Compact feature ribbon matching the supplied visual reference. */}
        <section className="af-home__feature-ribbon" aria-label="AquaFlow platform highlights">
          <div className="af-home__feature-ribbon-inner">
            {FEATURES.map((feature) => (
              <a className="af-home__ribbon-item" href="#features" key={feature.title}>
                <span className="af-home__ribbon-icon"><Icon name={feature.icon} /></span>
                <strong>{feature.title}</strong>
              </a>
            ))}
          </div>
        </section>

        {/* Detailed feature cards. */}
        <section className="af-home__section af-home__features" id="features">
          <div className="af-home__section-heading">
            <span className="af-home__eyebrow">ONE CONNECTED PLATFORM</span>
            <h2>Everything you need to manage water better.</h2>
            <p>Useful tools that bring household usage, apartment billing, and water-management operations together.</p>
          </div>
          <div className="af-home__feature-grid">
            {FEATURES.map((feature, index) => (
              <article className="af-home__feature-card" key={feature.title}>
                <span className={`af-home__feature-icon af-home__feature-icon--${index + 1}`}><Icon name={feature.icon} /></span>
                <h3>{feature.title}</h3>
                <p>{feature.text}</p>
              </article>
            ))}
          </div>
        </section>

        {/* Explain the current product workflow without claiming unsupported integrations. */}
        <section className="af-home__workflow" id="about">
          <div className="af-home__section af-home__workflow-inner">
            <div className="af-home__workflow-copy">
              <span className="af-home__eyebrow">HOW AQUAFLOW WORKS</span>
              <h2>From meter reading to a clearer picture.</h2>
              <p>Keep apartment setup, water readings, household usage, and billing information connected in one organized workspace.</p>
              <Link className="af-home__text-link" to={user ? portalPath : '/register'}>
                {user ? 'Go to your portal' : 'Register Now'} <Icon name="arrow" />
              </Link>
            </div>
            <div className="af-home__steps">
              <article className="af-home__step"><span>01</span><div><h3>Set up the community</h3><p>Administrators organize apartment details, households, meters, and tariff plans.</p></div></article>
              <article className="af-home__step"><span>02</span><div><h3>Record water readings</h3><p>Keep household meter readings up to date and maintain a useful reading history.</p></div></article>
              <article className="af-home__step"><span>03</span><div><h3>Review usage and bills</h3><p>Residents review household usage and invoices while administrators manage billing cycles.</p></div></article>
            </div>
          </div>
        </section>

        {/* Role-specific sections retain the shared login/portal routing model. */}
        <section className="af-home__section af-home__audience">
          <div className="af-home__section-heading">
            <span className="af-home__eyebrow">ONE PLATFORM, TWO CLEAR VIEWS</span>
            <h2>Designed for the whole community.</h2>
          </div>
          <div className="af-home__audience-grid">
            <article className="af-home__audience-card" id="for-residents">
              <span className="af-home__audience-label">FOR RESIDENTS</span>
              <span className="af-home__audience-icon"><Icon name="residents" /></span>
              <h3>Your household, at a glance.</h3>
              <p>Check water usage, follow reading history, review invoices, and see important household alerts.</p>
              <Link className="af-home__text-link" to={user ? portalPath : '/login'}>
                {user ? 'Open your portal' : 'Resident Log In'} <Icon name="arrow" />
              </Link>
            </article>
            <article className="af-home__audience-card af-home__audience-card--admin" id="for-admins">
              <span className="af-home__audience-label">FOR ADMINISTRATORS</span>
              <span className="af-home__audience-icon"><Icon name="layers" /></span>
              <h3>Community operations, organized.</h3>
              <p>Manage households, meter readings, tariff plans, billing cycles, and apartment-wide alerts.</p>
              <Link className="af-home__text-link" to={user ? portalPath : '/login'}>
                {isAdmin ? 'Open admin portal' : 'Administrator Log In'} <Icon name="arrow" />
              </Link>
            </article>
          </div>
        </section>

        {/* Final conversion section. */}
        <section className="af-home__cta-section">
          <div className="af-home__cta">
            <div>
              <span className="af-home__eyebrow">A CLEARER VIEW STARTS HERE</span>
              <h2>Make every reading count.</h2>
              <p>Sign in to your portal or create a resident account to get started.</p>
            </div>
            <div className="af-home__cta-actions">
              <Link className="af-home__button af-home__button--white" to={user ? portalPath : '/login'}>{user ? 'Open Your Portal' : 'Log In'}</Link>
              {!user && <Link className="af-home__button af-home__button--outline" to="/register">Register Now <Icon name="arrow" /></Link>}
            </div>
          </div>
        </section>
      </main>

      <footer className="af-home__footer">
        <div className="af-home__footer-main">
          <Brand footer />
          <p>Smarter water usage and transparent billing for apartment communities.</p>
          <span>© {new Date().getFullYear()} AquaFlow. All rights reserved.</span>
        </div>
      </footer>
    </div>
  );
}
