import { useId, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { portalPathFor } from '../navigation.js';
import './PublicHome.css';

/**
 * AquaFlow public landing page
 * ----------------------------------------------------------------------------
 * UI-only refresh. Nothing in the application's wiring has changed:
 *  - Same imports, same default export, same CSS file name (PublicHome.css).
 *  - Same auth hook (useAuth) and portal redirect (portalPathFor).
 *  - Same routes: "/", "/login", "/register" and the user's portal path.
 *  - No new dependencies. Icons are local inline SVG.
 *
 * Page structure (top to bottom)
 *  1. Header ............ sticky, mobile menu, auth-aware buttons
 *  2. Hero .............. promise + illustrative resident dashboard (sample data)
 *  3. Capability strip .. five core capabilities at a glance
 *  4. How it works ...... the four-step billing cycle, from both roles' view
 *  5. Features .......... grouped by audience (#for-residents, #for-admins)
 *  6. Billing explained . how a bill is calculated, with a worked example
 *  7. AquaBot ........... what the assistant can and cannot do
 *  8. About ............. principles the platform is built around
 *  9. Get started ....... checklists for residents and administrators
 * 10. FAQ ............... native <details> accordion, no JavaScript needed
 * 11. Closing CTA + footer
 *
 * Content note: every number in the hero preview, the billing example and the
 * AquaBot conversation is illustrative sample data and is labelled as such.
 * Real rates and allocation rules are configured per apartment.
 */

/* ------------------------------------------------------------------ */
/*  Brand                                                              */
/* ------------------------------------------------------------------ */

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
    check: <path d="m5 12 4 4L19 6" />,
    menu: <path d="M4 7h16M4 12h16M4 17h16" />,
    close: <path d="M6 6l12 12M18 6 6 18" />,
    shield: <><path d="M12 3 20 6v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6l8-3Z" /><path d="m8.5 12 2.3 2.3 4.8-5" /></>,
    trend: <><path d="M3 17 9 11l4 4 8-9" /><path d="M15 6h6v6M3 21h18" /></>,
    upload: <><path d="M12 16V4m-5 5 5-5 5 5" /><path d="M4 16v4h16v-4" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a7.5 7.5 0 0 0 0-6l1.3-1-2-3.4-1.6.7a7.5 7.5 0 0 0-5.2-1.5L11.5 2h-4l-.4 1.8a7.5 7.5 0 0 0-3.4 3l-1.8-.2v4l1.6.7a7.5 7.5 0 0 0 1.5 5.2l-.7 1.6 3.4 2 .9-1.4a7.5 7.5 0 0 0 5.2 0l1 1.3 3.4-2-.7-1.6Z" /></>,
    water: <><path d="M12 3c-3 4.3-7 8.3-7 12a7 7 0 0 0 14 0c0-3.7-4-7.7-7-12Z" /><path d="M9 16a3 3 0 0 0 3 3" /></>,
    invoice: <><path d="M6 3h9l5 5v13H6z" /><path d="M15 3v5h5M9 12h8M9 16h8" /><path d="m9 20 1.5-1.5L12 20l1.5-1.5L15 20" /></>,
    bell: <><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></>,
    purchase: <><path d="M3 7h18l-2 13H5L3 7Z" /><path d="M8 7a4 4 0 0 1 8 0M8 12h8m-8 4h5" /></>,
  };
  return <svg {...common} className={className}>{icons[name] || icons.usage}</svg>;
}

/* ------------------------------------------------------------------ */
/*  Content (kept as data so copy can be edited without touching JSX)  */
/* ------------------------------------------------------------------ */

const NAV_LINKS = [
  { href: '#features', label: 'Features' },
  { href: '#how-it-works', label: 'How it works' },
  { href: '#for-residents', label: 'For residents' },
  { href: '#for-admins', label: 'For admins' },
  { href: '#about', label: 'About' },
  { href: '#faq', label: 'FAQ' },
];

const CAPABILITIES = [
  { icon: 'upload', title: 'Meter readings', text: 'Manual entry or CSV import' },
  { icon: 'settings', title: 'Tiered tariffs', text: 'Rates by usage slab' },
  { icon: 'invoice', title: 'Itemized invoices', text: 'Downloadable PDFs' },
  { icon: 'bell', title: 'Usage alerts', text: 'Overuse and possible leaks' },
  { icon: 'sparkle', title: 'AquaBot', text: 'Read-only, role-aware assistant' },
];

// Illustrative sample data for the hero preview (kilolitres per month).
const USAGE_HISTORY = [
  { month: 'May', value: 11.8 }, { month: 'Jun', value: 12.4 }, { month: 'Jul', value: 10.9 },
  { month: 'Aug', value: 13.6 }, { month: 'Sep', value: 11.2 }, { month: 'Oct', value: 14.5 },
];
const CHART_MAX = 16;
const APARTMENT_AVERAGE = 12;

const STEPS = [
  {
    title: 'Set up the apartment',
    text: 'Add households, link residents, enter meter details and configure a tariff plan.',
    admin: 'Creates households, links residents and sets tariff tiers.',
    resident: 'Registers and is linked to their household.',
  },
  {
    title: 'Record readings',
    text: 'Add readings one at a time or import a CSV batch. Each reading is validated and kept in history.',
    admin: 'Enters readings or uploads the CSV template and reviews row-by-row results.',
    resident: 'Sees recent readings and usage summaries.',
  },
  {
    title: 'Review usage and costs',
    text: 'Compare consumption across households and preview how costs will be split before anything is final.',
    admin: 'Compares households, previews cost distribution and records water purchases.',
    resident: 'Explores trends and compares with apartment averages.',
  },
  {
    title: 'Finalize the cycle',
    text: 'Finalize itemized household bills. The tariff plan is locked into each one, and the cycle can then be archived.',
    admin: 'Finalizes and archives the billing cycle.',
    resident: 'Reads the itemized invoice and downloads the PDF.',
  },
];

const FEATURE_GROUPS = [
  {
    id: 'for-residents',
    title: 'For residents',
    intro: 'A personal view of your home’s water use, without the administration tools.',
    items: [
      { icon: 'trend', title: 'Usage dashboard and trends', text: 'See household consumption and meter history in one clear view.', details: ['Daily and monthly trend charts', 'Recent meter readings and usage summaries'] },
      { icon: 'layers', title: 'Peer benchmarking', text: 'Put your usage in context with apartment and similar-size household averages.', details: ['Apartment-wide average comparison', 'Similar homes grouped by registered floor area'] },
      { icon: 'water', title: 'Water-saving guidance', text: 'Practical conservation tips and overuse alerts help you spot where to save.', details: ['Actionable everyday water-saving tips', 'Usage alerts with reduction suggestions'] },
    ],
  },
  {
    id: 'for-admins',
    title: 'For administrators',
    intro: 'A dedicated, apartment-scoped workspace for water operations and billing.',
    items: [
      { icon: 'upload', title: 'Meter readings and CSV import', text: 'Record readings individually or import a batch while keeping a traceable history.', details: ['Manual entry with reading validation', 'CSV template, row results and error feedback'] },
      { icon: 'residents', title: 'Apartment and household records', text: 'Organize apartments, flats, residents, occupancy, area and meter details.', details: ['Link residents to households', 'Keep household and meter information together'] },
      { icon: 'billing', title: 'Billing cycle controls', text: 'Create and manage billing periods, review costs, then finalize and archive cycles.', details: ['Cycle status and period management', 'Preview household cost distribution before finalizing'] },
      { icon: 'settings', title: 'Flexible tariff plans', text: 'Configure effective dates and progressive water rates for each apartment.', details: ['Tier limits and per-unit rates', 'Rate plans lock into finalized bills'] },
      { icon: 'purchase', title: 'Shared costs and water purchases', text: 'Record bulk water purchases and see how common costs are allocated to homes.', details: ['Track source, volume, supplier and cost', 'View allocation basis and household shares'] },
    ],
  },
  {
    id: 'for-everyone',
    title: 'For both roles',
    intro: 'Shared tools that keep residents and administrators looking at the same facts.',
    items: [
      { icon: 'invoice', title: 'Itemized invoices and PDF downloads', text: 'Review household charges with a clear breakdown of water, shared costs and adjustments.', details: ['Invoice history and consumption breakdown', 'Downloadable PDF with payment instructions'] },
      { icon: 'bell', title: 'Alerts and notifications', text: 'Bring unusual readings and important account updates into one place.', details: ['Overuse and possible leak detection', 'Configurable email alerts and in-app notifications'] },
    ],
  },
];

// Worked example. Illustrative only: 14.5 kL = 10.0 kL in slab 1 + 4.5 kL in slab 2.
const BILL_PARTS = [
  { title: 'Metered usage', text: 'The difference between the current and previous meter reading for each household.' },
  { title: 'Tiered rates', text: 'Usage is split into slabs, and each slab has its own rate per unit.' },
  { title: 'Shared costs', text: 'Bulk water purchases and other common costs are allocated across households, with the allocation basis and each home’s share visible.' },
  { title: 'Adjustments', text: 'Adjustments appear as separate lines, so the total is easy to check.' },
];

const BILL_EXAMPLE = {
  rows: [
    { slab: 'First 10 kL', used: '10.0 kL', rate: '₹8.00', amount: '₹80.00' },
    { slab: 'Above 10 kL', used: '4.5 kL', rate: '₹12.00', amount: '₹54.00' },
  ],
  shared: '₹46.00',
  total: '₹180.00',
};

const AQUABOT = {
  resident: ['How much water did I use last month?', 'Why did my last bill go up?', 'Do I have any open usage alerts?'],
  admin: ['Which households used the most water this cycle?', 'What is the apartment average for this cycle?', 'How were shared costs allocated?'],
  limits: [
    'Read-only: it cannot change any record.',
    'Answers come only from records your role can access.',
    'It never reveals another apartment’s data.',
    'It appears when enabled and configured for your apartment.',
  ],
};

const PRINCIPLES = [
  { icon: 'invoice', title: 'Transparent by default', text: 'Every charge is itemized and every reading stays in the history, so a bill can always be traced back to its source.' },
  { icon: 'residents', title: 'Role-based views', text: 'Residents and administrators each get a workspace shaped around their tasks.' },
  { icon: 'shield', title: 'Apartment-scoped data', text: 'Records are organized per apartment, and AquaBot follows the same boundaries.' },
];

const RESIDENT_START = [
  'Register for a resident account.',
  'Ask your administrator to link you to your household if that has not been done yet.',
  'Open your portal to see readings, trends and invoices.',
  'Adjust your alert preferences to suit you.',
];

const ADMIN_START = [
  'Log in to the admin portal.',
  'Add households and link residents.',
  'Create a tariff plan with tier limits and rates.',
  'Enter or import meter readings.',
  'Open a billing cycle, preview costs, then finalize.',
];

const FAQS = [
  { q: 'Who can see my household’s data?', a: 'Residents see their own household’s readings, bills and alerts. Administrators work within their own apartment’s records. AquaBot follows the same rules and never shows another apartment’s data.' },
  { q: 'How do I get an account?', a: 'Residents can register from the Register page. Your administrator then links your account to your household so your readings and invoices appear in your portal.' },
  { q: 'Can I download my invoice?', a: 'Yes. Invoices include an itemized breakdown and can be downloaded as a PDF with payment instructions.' },
  { q: 'How are shared costs split?', a: 'Administrators record bulk water purchases with their source, volume, supplier and cost. The allocation basis and each household’s share can be reviewed, so a common cost can be traced to the bill it reached.' },
  { q: 'What happens if a CSV import has errors?', a: 'The import reports results row by row with error feedback, so you can see which rows need attention. A CSV template is provided to keep the format consistent.' },
  { q: 'What changes when a billing cycle is finalized?', a: 'Finalizing locks the tariff plan into the household bills. Once a cycle is complete it can be archived.' },
  { q: 'Is AquaBot available to everyone?', a: 'AquaBot appears when it is enabled and configured for your apartment. It is read-only and answers only from records your role is allowed to see.' },
];

/* ------------------------------------------------------------------ */
/*  Hero preview (illustrative sample data, clearly labelled)          */
/* ------------------------------------------------------------------ */

function DashboardPreview() {
  const digits = ['0', '0', '4', '2', '7'];
  return (
    <div className="af-home__visual" role="img" aria-label="Illustrative preview of a resident water dashboard using sample data">
      <div className="af-home__preview">
        <div className="af-home__preview-head">
          <div>
            <strong>Flat B-204</strong>
            <span>October billing cycle</span>
          </div>
          <em className="af-home__sample-tag">Sample data</em>
        </div>

        <div className="af-home__meter">
          <span className="af-home__meter-label">Meter reading, kL</span>
          <div className="af-home__odometer">
            {digits.map((digit, index) => <b key={index}>{digit}</b>)}
            <b className="is-decimal">1</b>
          </div>
        </div>

        <div className="af-home__chart">
          <div className="af-home__chart-title">
            <span>Monthly usage, kL</span>
            <strong>14.5 this cycle</strong>
          </div>
          <div className="af-home__chart-plot">
            <span className="af-home__chart-avg" style={{ bottom: `${(APARTMENT_AVERAGE / CHART_MAX) * 100}%` }}>
              <i>Apartment avg</i>
            </span>
            {USAGE_HISTORY.map((item, index) => (
              <span
                key={item.month}
                className={`af-home__bar${index === USAGE_HISTORY.length - 1 ? ' is-current' : ''}`}
                style={{ height: `${(item.value / CHART_MAX) * 100}%` }}
              />
            ))}
          </div>
          <div className="af-home__chart-axis">
            {USAGE_HISTORY.map((item) => <span key={item.month}>{item.month}</span>)}
          </div>
        </div>

        <dl className="af-home__bill">
          <div><dt>Water charges</dt><dd>₹134.00</dd></div>
          <div><dt>Shared costs</dt><dd>{BILL_EXAMPLE.shared}</dd></div>
          <div className="is-total"><dt>Total due</dt><dd>{BILL_EXAMPLE.total}</dd></div>
        </dl>
      </div>

      <div className="af-home__chip">
        <span className="af-home__chip-icon"><Icon name="bell" /></span>
        <span><strong>Usage alert</strong><small>21% above your recent average</small></span>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Page                                                               */
/* ------------------------------------------------------------------ */

export default function PublicHome() {
  const { user } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const portalPath = user ? portalPathFor(user) : '/login';
  const isAdmin = user?.role === 'ADMIN';
  const primaryTarget = user ? portalPath : '/register';
  const year = new Date().getFullYear();

  return (
    <div className="af-home">
      {/* Header: auth-aware destinations are preserved. */}
      <header className="af-home__header">
        <div className="af-home__wrap af-home__header-inner">
          <Brand />
          <nav
            id="af-home-nav"
            className={`af-home__nav${menuOpen ? ' is-open' : ''}`}
            aria-label="Main navigation"
            onClick={() => setMenuOpen(false)}
          >
            {NAV_LINKS.map((link) => (
              <a className="af-home__nav-link" href={link.href} key={link.href}>{link.label}</a>
            ))}
            <Link className="af-home__nav-link af-home__nav-link--account" to={portalPath}>
              {user ? 'My portal' : 'Log in'}
            </Link>
          </nav>
          <div className="af-home__header-actions">
            <Link className="af-home__login-link" to={portalPath}>{user ? 'My portal' : 'Log in'}</Link>
            <Link className="af-home__button af-home__button--primary af-home__button--nav" to={primaryTarget}>
              {user ? 'Open portal' : 'Get started'}
            </Link>
            <button
              type="button"
              className="af-home__menu-toggle"
              aria-label={menuOpen ? 'Close menu' : 'Open menu'}
              aria-expanded={menuOpen}
              aria-controls="af-home-nav"
              onClick={() => setMenuOpen((open) => !open)}
            >
              <Icon name={menuOpen ? 'close' : 'menu'} />
            </button>
          </div>
        </div>
      </header>

      <main>
        {/* 1. Hero */}
        <section className="af-home__hero" id="home">
          <div className="af-home__wrap af-home__hero-inner">
            <div className="af-home__hero-copy">
              <h1>Every litre measured. Every bill explained.</h1>
              <p className="af-home__hero-description">
                AquaFlow connects meter readings, household records and tariff plans, so apartment communities can understand their water use and bill it fairly.
              </p>
              <div className="af-home__hero-actions">
                <Link className="af-home__button af-home__button--primary af-home__button--large" to={primaryTarget}>
                  {user ? 'Open your portal' : 'Get started'}
                </Link>
                <a className="af-home__button af-home__button--secondary af-home__button--large" href="#how-it-works">
                  See how it works
                </a>
              </div>
              <p className="af-home__hero-note"><Icon name="shield" /> Separate views for residents and apartment administrators.</p>
            </div>
            <DashboardPreview />
          </div>
        </section>

        {/* 2. Capability strip */}
        <section className="af-home__strip" aria-label="Platform capabilities">
          <ul className="af-home__wrap af-home__strip-list">
            {CAPABILITIES.map((item) => (
              <li key={item.title}>
                <span className="af-home__strip-icon"><Icon name={item.icon} /></span>
                <span><strong>{item.title}</strong><small>{item.text}</small></span>
              </li>
            ))}
          </ul>
        </section>

        {/* 3. How it works */}
        <section className="af-home__section af-home__how" id="how-it-works">
          <div className="af-home__wrap">
            <div className="af-home__section-heading">
              <h2>From meter reading to household bill in four steps.</h2>
              <p>Each billing cycle follows the same path. Here is what happens at every stage, and what each role sees.</p>
            </div>
            <ol className="af-home__steps">
              {STEPS.map((step, index) => (
                <li className="af-home__step" key={step.title}>
                  <span className="af-home__step-number">{index + 1}</span>
                  <h3>{step.title}</h3>
                  <p>{step.text}</p>
                  <dl>
                    <div><dt>Administrator</dt><dd>{step.admin}</dd></div>
                    <div><dt>Resident</dt><dd>{step.resident}</dd></div>
                  </dl>
                </li>
              ))}
            </ol>
          </div>
        </section>

        {/* 4. Features, grouped by audience */}
        <section className="af-home__section af-home__features" id="features">
          <div className="af-home__wrap">
            <div className="af-home__section-heading">
              <h2>Everything you need to manage water better.</h2>
              <p>From meter entry to household comparisons and downloadable bills, AquaFlow gives residents and apartment teams one transparent view of water use.</p>
            </div>
            {FEATURE_GROUPS.map((group) => (
              <div className="af-home__group" id={group.id} key={group.id}>
                <div className="af-home__group-intro">
                  <h3>{group.title}</h3>
                  <p>{group.intro}</p>
                </div>
                <ul className="af-home__group-list">
                  {group.items.map((item) => (
                    <li className="af-home__feature" key={item.title}>
                      <span className="af-home__feature-icon"><Icon name={item.icon} /></span>
                      <div>
                        <h4>{item.title}</h4>
                        <p>{item.text}</p>
                        <ul>
                          {item.details.map((detail) => <li key={detail}><Icon name="check" />{detail}</li>)}
                        </ul>
                      </div>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </section>

        {/* 5. How a bill is calculated */}
        <section className="af-home__section af-home__billing" id="billing">
          <div className="af-home__wrap af-home__billing-inner">
            <div className="af-home__billing-copy">
              <h2>How a bill is calculated.</h2>
              <p>Every invoice is built from the same four parts, so residents and administrators can check the arithmetic together.</p>
              <dl className="af-home__parts">
                {BILL_PARTS.map((part) => (
                  <div key={part.title}><dt>{part.title}</dt><dd>{part.text}</dd></div>
                ))}
              </dl>
            </div>
            <figure className="af-home__example">
              <figcaption>
                <strong>Worked example</strong>
                <span>14.5 kL used in one cycle</span>
              </figcaption>
              <div className="af-home__table-scroll">
                <table className="af-home__table">
                  <thead>
                    <tr><th scope="col">Slab</th><th scope="col">Used</th><th scope="col">Rate / kL</th><th scope="col">Amount</th></tr>
                  </thead>
                  <tbody>
                    {BILL_EXAMPLE.rows.map((row) => (
                      <tr key={row.slab}><th scope="row">{row.slab}</th><td>{row.used}</td><td>{row.rate}</td><td>{row.amount}</td></tr>
                    ))}
                    <tr><th scope="row" colSpan={3}>Shared costs</th><td>{BILL_EXAMPLE.shared}</td></tr>
                  </tbody>
                  <tfoot>
                    <tr><th scope="row" colSpan={3}>Total due</th><td>{BILL_EXAMPLE.total}</td></tr>
                  </tfoot>
                </table>
              </div>
              <p className="af-home__note">
                Illustrative example only. 1 kL equals 1,000 litres. Your apartment’s rates and shared-cost allocation are set by its administrator.
              </p>
            </figure>
          </div>
        </section>

        {/* 6. AquaBot */}
        <section className="af-home__section af-home__bot" id="aquabot">
          <div className="af-home__wrap af-home__bot-inner">
            <div className="af-home__bot-copy">
              <h2>Ask AquaBot in plain language.</h2>
              <p>AquaBot is a water intelligence assistant that answers questions from the records your role is allowed to see.</p>
              <div className="af-home__asks">
                <div>
                  <h3>Residents can ask</h3>
                  <ul>{AQUABOT.resident.map((item) => <li key={item}>{item}</li>)}</ul>
                </div>
                <div>
                  <h3>Administrators can ask</h3>
                  <ul>{AQUABOT.admin.map((item) => <li key={item}>{item}</li>)}</ul>
                </div>
              </div>
              <ul className="af-home__limits">
                {AQUABOT.limits.map((item) => <li key={item}><Icon name="shield" />{item}</li>)}
              </ul>
            </div>
            <div className="af-home__chat" role="img" aria-label="Example AquaBot conversation about why a bill increased">
              <div className="af-home__chat-head"><Icon name="sparkle" /><strong>AquaBot</strong><em className="af-home__sample-tag">Example conversation</em></div>
              <p className="af-home__bubble af-home__bubble--user">Why is my bill higher this month?</p>
              <p className="af-home__bubble af-home__bubble--bot">
                You used 14.5 kL this cycle, up from 11.2 kL last cycle. The extra 3.3 kL fell into the higher-rate slab, which is the main reason for the increase.
              </p>
              <p className="af-home__bubble af-home__bubble--user">Any tips to bring it down?</p>
              <p className="af-home__bubble af-home__bubble--bot">
                Check taps and flush tanks for slow leaks, and compare your daily readings to spot unusual days. I can show your last six months if that helps.
              </p>
            </div>
          </div>
        </section>

        {/* 7. About */}
        <section className="af-home__section af-home__about" id="about">
          <div className="af-home__wrap">
            <div className="af-home__section-heading">
              <h2>One connected view of water in your community.</h2>
              <p>AquaFlow brings household records, meter readings, consumption insights, cost allocation and invoices into one role-based apartment platform.</p>
            </div>
            <ul className="af-home__principles">
              {PRINCIPLES.map((item) => (
                <li key={item.title}>
                  <span className="af-home__feature-icon"><Icon name={item.icon} /></span>
                  <h3>{item.title}</h3>
                  <p>{item.text}</p>
                </li>
              ))}
            </ul>
          </div>
        </section>

        {/* 8. Get started */}
        <section className="af-home__section af-home__start" id="get-started">
          <div className="af-home__wrap">
            <div className="af-home__section-heading">
              <h2>Getting started is straightforward.</h2>
              <p>Pick your role to see what to do first.</p>
            </div>
            <div className="af-home__start-grid">
              <article>
                <h3>Residents</h3>
                <ol>{RESIDENT_START.map((item) => <li key={item}>{item}</li>)}</ol>
                <Link className="af-home__text-link" to={user ? portalPath : '/register'}>{user ? 'Open your portal' : 'Create a resident account'}</Link>
              </article>
              <article>
                <h3>Administrators</h3>
                <ol>{ADMIN_START.map((item) => <li key={item}>{item}</li>)}</ol>
                <Link className="af-home__text-link" to={portalPath}>{isAdmin ? 'Open the admin portal' : user ? 'Open your portal' : 'Administrator log in'}</Link>
              </article>
            </div>
          </div>
        </section>

        {/* 9. FAQ */}
        <section className="af-home__section af-home__faq" id="faq">
          <div className="af-home__wrap af-home__faq-inner">
            <div className="af-home__faq-intro">
              <h2>Frequently asked questions</h2>
              <p>Short answers about accounts, privacy, billing and AquaBot.</p>
            </div>
            <div className="af-home__faq-list">
              {FAQS.map((item) => (
                <details key={item.q}>
                  <summary>{item.q}</summary>
                  <p>{item.a}</p>
                </details>
              ))}
            </div>
          </div>
        </section>

        {/* 10. Closing call to action */}
        <section className="af-home__cta-section">
          <div className="af-home__wrap">
            <div className="af-home__cta">
              <div>
                <h2>Make every reading count.</h2>
                <p>Explore the features, then sign in or create a resident account to get started.</p>
              </div>
              <div className="af-home__cta-actions">
                <Link className="af-home__button af-home__button--white" to={portalPath}>{user ? 'Open your portal' : 'Log in'}</Link>
                {!user && <Link className="af-home__button af-home__button--outline" to="/register">Register now</Link>}
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="af-home__footer">
        <div className="af-home__wrap af-home__footer-main">
          <div className="af-home__footer-brand">
            <Brand footer />
            <p>Smarter water usage and transparent billing for apartment communities.</p>
          </div>
          <nav className="af-home__footer-col" aria-label="Platform links">
            <h2>Platform</h2>
            <a href="#features">Features</a>
            <a href="#how-it-works">How it works</a>
            <a href="#billing">Billing explained</a>
            <a href="#aquabot">AquaBot</a>
          </nav>
          <nav className="af-home__footer-col" aria-label="Account links">
            <h2>Account</h2>
            <Link to={portalPath}>{user ? 'My portal' : 'Log in'}</Link>
            {!user && <Link to="/register">Register</Link>}
            <a href="#get-started">Getting started</a>
            <a href="#faq">FAQ</a>
          </nav>
        </div>
        <div className="af-home__wrap af-home__footer-base">
          <span>© {year} AquaFlow. All rights reserved.</span>
        </div>
      </footer>
    </div>
  );
}