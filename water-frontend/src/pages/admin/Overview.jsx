import { Link } from 'react-router-dom';
import { get } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { useAuth } from '../../auth.jsx';
import { dateStr } from '../../fmt.js';
import { ErrorBox, Spinner, StatusBadge, useAsync } from '../../ui.jsx';

/* ------------------------------------------------------------------ */
/*  Styles (scoped with the "wo-" prefix so nothing else is affected)  */
/* ------------------------------------------------------------------ */
const STYLES = `
.wo-root {
  --wo-ink: #12263a;
  --wo-muted: #5b6b7c;
  --wo-faint: #8a98a8;
  --wo-line: #e3e9ef;
  --wo-surface: #ffffff;
  --wo-canvas: #f4f8fb;
  --wo-brand: #0b6e8a;
  --wo-brand-dark: #084c61;
  --wo-brand-soft: #e3f3f7;
  --wo-good: #1f8a5b;
  --wo-good-soft: #e4f5ec;
  --wo-warn: #b7791f;
  --wo-warn-soft: #fdf3dc;
  --wo-info: #3b5bdb;
  --wo-info-soft: #e8edff;
  --wo-violet: #6d4bc3;
  --wo-violet-soft: #efe9fb;
  color: var(--wo-ink);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 4px 0 32px;
  box-sizing: border-box;
}
.wo-root *, .wo-root *::before, .wo-root *::after { box-sizing: border-box; }
.wo-root h1, .wo-root h2, .wo-root h3, .wo-root p { margin: 0; }
.wo-root a { color: inherit; text-decoration: none; }
.wo-root svg { width: 20px; height: 20px; flex: none; }
.wo-root a:focus-visible { outline: 3px solid rgba(11, 110, 138, 0.45); outline-offset: 2px; border-radius: 10px; }

/* Hero */
.wo-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 30px 32px;
  border-radius: 20px;
  color: #fff;
  background: linear-gradient(120deg, #084c61 0%, #0b6e8a 55%, #1b8fa6 100%);
}
.wo-hero::after {
  content: "";
  position: absolute;
  right: -60px;
  top: -80px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  pointer-events: none;
}
.wo-hero::before {
  content: "";
  position: absolute;
  right: 120px;
  bottom: -110px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  pointer-events: none;
}
.wo-hero-copy { position: relative; z-index: 1; min-width: 0; flex: 1 1 320px; }
.wo-hero h1 { font-size: 28px; line-height: 1.2; font-weight: 700; letter-spacing: -0.01em; }
.wo-hero p { margin-top: 8px; font-size: 15px; line-height: 1.5; color: rgba(255, 255, 255, 0.86); max-width: 520px; }
.wo-address {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 13px;
  max-width: 100%;
}
.wo-address svg { width: 16px; height: 16px; }
.wo-address span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wo-hero-aside {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 18px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  min-width: 200px;
}
.wo-hero-aside small { font-size: 12px; color: rgba(255, 255, 255, 0.75); }
.wo-hero-aside strong { font-size: 15px; font-weight: 600; }

/* Quick actions */
.wo-actions { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.wo-action {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  background: var(--wo-surface);
  border: 1px solid var(--wo-line);
  font-weight: 600;
  font-size: 14px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}
.wo-action:hover { border-color: var(--wo-brand); box-shadow: 0 6px 18px rgba(11, 110, 138, 0.12); transform: translateY(-1px); }
.wo-action-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--wo-brand-soft);
  color: var(--wo-brand);
}
.wo-action-text { display: flex; flex-direction: column; min-width: 0; }
.wo-action-text small { font-weight: 400; font-size: 12px; color: var(--wo-muted); margin-top: 2px; }
.wo-action-arrow { margin-left: auto; color: var(--wo-faint); }

/* Stats */
.wo-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.wo-stat {
  padding: 18px;
  border-radius: 16px;
  background: var(--wo-surface);
  border: 1px solid var(--wo-line);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.wo-stat-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; color: var(--wo-muted); font-size: 13px; font-weight: 600; }
.wo-stat-icon { display: grid; place-items: center; width: 36px; height: 36px; border-radius: 10px; }
.wo-tone-teal .wo-stat-icon, .wo-tone-teal.wo-metric-icon { background: var(--wo-brand-soft); color: var(--wo-brand); }
.wo-tone-blue .wo-stat-icon, .wo-tone-blue.wo-metric-icon { background: var(--wo-info-soft); color: var(--wo-info); }
.wo-tone-gold .wo-stat-icon, .wo-tone-gold.wo-metric-icon { background: var(--wo-warn-soft); color: var(--wo-warn); }
.wo-tone-purple .wo-stat-icon, .wo-tone-purple.wo-metric-icon { background: var(--wo-violet-soft); color: var(--wo-violet); }
.wo-stat-value { font-size: 32px; line-height: 1.1; font-weight: 700; letter-spacing: -0.02em; }
.wo-stat-note { font-size: 13px; color: var(--wo-muted); }

/* Panels */
.wo-grid { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.wo-panel { padding: 22px; border-radius: 16px; background: var(--wo-surface); border: 1px solid var(--wo-line); }
.wo-panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.wo-panel-head h2 { font-size: 18px; font-weight: 700; letter-spacing: -0.01em; }
.wo-panel-head p { margin-top: 4px; font-size: 13px; color: var(--wo-muted); line-height: 1.45; }
.wo-link { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: var(--wo-brand); }
.wo-link svg { width: 16px; height: 16px; }
.wo-link:hover { color: var(--wo-brand-dark); }
.wo-pill { padding: 4px 10px; border-radius: 999px; background: var(--wo-brand-soft); color: var(--wo-brand-dark); font-size: 12px; font-weight: 600; white-space: nowrap; }
.wo-pill-done { background: var(--wo-good-soft); color: var(--wo-good); }

/* Setup checklist */
.wo-bar { height: 8px; border-radius: 999px; background: #e9eef3; overflow: hidden; margin-bottom: 14px; }
.wo-bar > span { display: block; height: 100%; border-radius: 999px; background: linear-gradient(90deg, #1b8fa6, #1f8a5b); transition: width 0.5s ease; }
.wo-steps { display: flex; flex-direction: column; gap: 10px; }
.wo-step {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid var(--wo-line);
  background: var(--wo-canvas);
  transition: border-color 0.15s ease, background 0.15s ease;
}
.wo-step:hover { border-color: var(--wo-brand); background: #fff; }
.wo-step-mark { display: grid; place-items: center; width: 32px; height: 32px; border-radius: 50%; background: #fff; border: 1px solid var(--wo-line); color: var(--wo-faint); }
.wo-step-mark svg { width: 16px; height: 16px; }
.wo-step.is-done .wo-step-mark { background: var(--wo-good); border-color: var(--wo-good); color: #fff; }
.wo-step-copy { display: flex; flex-direction: column; min-width: 0; flex: 1; }
.wo-step-copy strong { font-size: 14px; }
.wo-step-copy small { font-size: 12.5px; color: var(--wo-muted); margin-top: 2px; }
.wo-step-state { font-size: 12px; font-weight: 600; color: var(--wo-brand); white-space: nowrap; }
.wo-step.is-done .wo-step-state { color: var(--wo-good); }
.wo-celebrate {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--wo-good-soft);
  color: #166845;
  font-size: 13.5px;
  line-height: 1.45;
}

/* Latest cycle */
.wo-cycle { display: flex; flex-direction: column; gap: 14px; }
.wo-cycle-range { padding: 16px; border-radius: 12px; background: var(--wo-canvas); border: 1px solid var(--wo-line); }
.wo-cycle-range small { display: block; font-size: 12px; color: var(--wo-muted); margin-bottom: 6px; }
.wo-cycle-range strong { font-size: 17px; line-height: 1.4; font-weight: 700; }
.wo-cycle-range strong em { font-style: normal; font-weight: 400; color: var(--wo-muted); padding: 0 4px; }
.wo-cycle-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.wo-cycle-meta > div { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.wo-cycle-meta small { font-size: 12px; color: var(--wo-muted); }
.wo-cycle-meta strong { font-size: 14px; overflow-wrap: anywhere; }
.wo-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 11px 16px;
  border-radius: 10px;
  background: var(--wo-brand);
  color: #fff !important;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.15s ease;
}
.wo-btn:hover { background: var(--wo-brand-dark); }
.wo-btn svg { width: 16px; height: 16px; }
.wo-empty { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; padding: 8px 0; }
.wo-empty-icon { display: grid; place-items: center; width: 44px; height: 44px; border-radius: 12px; background: var(--wo-brand-soft); color: var(--wo-brand); }
.wo-empty strong { font-size: 15px; }
.wo-empty p { font-size: 13.5px; color: var(--wo-muted); line-height: 1.5; }

/* Readiness */
.wo-health { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.wo-metric { display: flex; gap: 14px; padding: 16px; border-radius: 14px; border: 1px solid var(--wo-line); background: var(--wo-canvas); min-width: 0; }
.wo-metric-icon { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 10px; flex: none; }
.wo-metric-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 8px; }
.wo-metric-head { display: flex; justify-content: space-between; gap: 8px; font-size: 14px; }
.wo-metric-head span { font-weight: 700; }
.wo-metric small { font-size: 12.5px; color: var(--wo-muted); line-height: 1.45; }
.wo-track { height: 8px; border-radius: 999px; background: #e1e8ee; overflow: hidden; }
.wo-fill { display: block; height: 100%; border-radius: 999px; transition: width 0.5s ease; }
.wo-fill-teal { background: var(--wo-brand); }
.wo-fill-blue { background: var(--wo-info); }
.wo-stack { display: flex; height: 8px; border-radius: 999px; overflow: hidden; background: #e1e8ee; }
.wo-stack span { display: block; height: 100%; }
.wo-seg-open { background: var(--wo-good); }
.wo-seg-final { background: var(--wo-info); }
.wo-seg-arch { background: var(--wo-faint); }
.wo-legend { display: flex; flex-wrap: wrap; gap: 6px 14px; font-size: 12.5px; color: var(--wo-muted); }
.wo-legend i { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px; }

/* Attention list */
.wo-attention { display: flex; flex-direction: column; gap: 10px; }
.wo-attention-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--wo-warn-soft);
  border: 1px solid #f1deac;
  font-size: 13.5px;
  line-height: 1.45;
}
.wo-attention-item span { flex: 1; min-width: 0; }
.wo-attention-item a { font-weight: 600; color: var(--wo-warn); white-space: nowrap; }
.wo-allgood { padding: 12px 14px; border-radius: 12px; background: var(--wo-good-soft); color: #166845; font-size: 13.5px; }

/* Responsive */
@media (max-width: 1020px) {
  .wo-actions, .wo-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .wo-grid { grid-template-columns: minmax(0, 1fr); }
  .wo-health { grid-template-columns: minmax(0, 1fr); }
}
@media (max-width: 560px) {
  .wo-hero { padding: 22px 20px; }
  .wo-hero h1 { font-size: 23px; }
  .wo-hero-aside { width: 100%; }
  .wo-actions, .wo-stats { grid-template-columns: minmax(0, 1fr); }
  .wo-panel { padding: 18px; }
  .wo-cycle-meta { grid-template-columns: minmax(0, 1fr); }
  .wo-attention-item { flex-direction: column; align-items: flex-start; }
}
@media (prefers-reduced-motion: reduce) {
  .wo-root * { transition: none !important; }
}
`;

/* ------------------------------------------------------------------ */
/*  Small helpers                                                      */
/* ------------------------------------------------------------------ */
const asArray = (value) => (Array.isArray(value) ? value : []);
const plural = (count, word) => `${count} ${word}${count === 1 ? '' : 's'}`;

function greeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}

function Icon({ name }) {
  const paths = {
    households: <><path d="M3 10.5 12 3l9 7.5" /><path d="M5.5 9.5V21h13V9.5M9 21v-7h6v7" /></>,
    residents: <><circle cx="9" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M3 20v-1a6 6 0 0 1 12 0v1zm13-5a5 5 0 0 1 5 5h-3" /></>,
    tariffs: <><path d="M4 7h16M4 12h10M4 17h7" /><circle cx="19" cy="17" r="2" /></>,
    cycles: <><rect x="3.5" y="5" width="17" height="16" rx="2" /><path d="M7.5 3v4m9-4v4M3.5 9.5h17M8 13h3m-3 4h3m3-4h2m-2 4h2" /></>,
    meter: <><circle cx="12" cy="12" r="9" /><path d="M12 12 16 8m-4 4v5m-5 0h10" /></>,
    plus: <><path d="M12 5v14m-7-7h14" /></>,
    arrow: <><path d="M5 12h14m-6-6 6 6-6 6" /></>,
    check: <><path d="m5 12 4 4L19 6" /></>,
    pin: <><path d="M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21z" /><circle cx="12" cy="9.5" r="2.5" /></>,
  };
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name] || null}
    </svg>
  );
}

function QuickAction({ to, icon, title, hint }) {
  return (
    <Link className="wo-action" to={to}>
      <span className="wo-action-icon"><Icon name={icon} /></span>
      <span className="wo-action-text">
        {title}
        <small>{hint}</small>
      </span>
      <span className="wo-action-arrow"><Icon name="arrow" /></span>
    </Link>
  );
}

function StatCard({ icon, label, value, note, tone }) {
  return (
    <article className={`wo-stat wo-tone-${tone}`}>
      <div className="wo-stat-top">
        <span>{label}</span>
        <span className="wo-stat-icon"><Icon name={icon} /></span>
      </div>
      <strong className="wo-stat-value">{value}</strong>
      <span className="wo-stat-note">{note}</span>
    </article>
  );
}

function SetupStep({ label, detail, done, to }) {
  return (
    <Link className={`wo-step ${done ? 'is-done' : ''}`} to={to}>
      <span className="wo-step-mark"><Icon name={done ? 'check' : 'arrow'} /></span>
      <span className="wo-step-copy">
        <strong>{label}</strong>
        <small>{detail}</small>
      </span>
      <span className="wo-step-state">{done ? 'Done' : 'Set up'}</span>
    </Link>
  );
}

function Metric({ icon, label, value, detail, percent, tone, fill }) {
  const safePercent = Math.max(0, Math.min(100, Number.isFinite(percent) ? percent : 0));
  return (
    <div className="wo-metric">
      <div className={`wo-metric-icon wo-tone-${tone}`}><Icon name={icon} /></div>
      <div className="wo-metric-body">
        <div className="wo-metric-head">
          <strong>{label}</strong>
          <span>{value}</span>
        </div>
        <div className="wo-track" role="progressbar" aria-label={label} aria-valuemin={0} aria-valuemax={100} aria-valuenow={safePercent}>
          <span className={`wo-fill wo-fill-${fill}`} style={{ width: `${safePercent}%` }} />
        </div>
        <small>{detail}</small>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Page                                                               */
/* ------------------------------------------------------------------ */
export default function Overview() {
  const { apartment, apartmentId } = useAdmin();
  const { user } = useAuth();

  const { data, loading, error } = useAsync(async () => {
    const [households, users, cycles, tariffs] = await Promise.all([
      get(`/api/v1/apartments/${apartmentId}/households`),
      get(`/api/v1/users?apartmentId=${apartmentId}`),
      get(`/api/v1/apartments/${apartmentId}/billing-cycles`),
      get(`/api/v1/apartments/${apartmentId}/tariffs`),
    ]);
    return { households, users, cycles, tariffs };
  }, [apartmentId]);

  if (loading) return <Spinner />;
  if (error) return <ErrorBox error={error} />;

  const households = asArray(data?.households);
  const users = asArray(data?.users);
  const cycles = asArray(data?.cycles);
  const tariffs = asArray(data?.tariffs);

  const residents = users.filter((item) => item && item.role === 'RESIDENT');
  const linkedResidents = residents.filter((item) => item.householdId).length;
  const unlinkedResidents = residents.length - linkedResidents;
  const meteredHouseholds = households.filter((item) => item && item.meterNumber).length;
  const missingMeters = households.length - meteredHouseholds;
  const activeTariffs = tariffs.filter((item) => item && item.active).length;
  const openCycles = cycles.filter((item) => item && item.status === 'OPEN');
  const finalizedCycles = cycles.filter((item) => item && item.status === 'FINALIZED');
  const archivedCycles = cycles.filter((item) => item && item.status === 'ARCHIVED');

  // Latest cycle = the one with the most recent start date (falls back to the first item).
  const latestCycle = cycles.reduce((best, item) => {
    if (!item) return best;
    if (!best) return item;
    const a = Date.parse(item.startDate);
    const b = Date.parse(best.startDate);
    return Number.isFinite(a) && Number.isFinite(b) && a > b ? item : best;
  }, null);

  const todayLabel = new Intl.DateTimeFormat('en-IN', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date());

  const meterCoverage = households.length ? Math.round((meteredHouseholds / households.length) * 100) : null;
  const residentLinking = residents.length ? Math.round((linkedResidents / residents.length) * 100) : null;

  const setupSteps = [
    { done: households.length > 0, label: 'Add households', detail: `${plural(households.length, 'household')} added`, to: '/admin/households' },
    { done: residents.length > 0 && linkedResidents === residents.length, label: 'Link residents to households', detail: `${linkedResidents} of ${residents.length} residents linked`, to: '/admin/residents' },
    { done: activeTariffs > 0, label: 'Create a tariff plan', detail: `${plural(activeTariffs, 'active plan')}`, to: '/admin/tariffs' },
    { done: openCycles.length > 0, label: 'Open a billing cycle', detail: `${plural(openCycles.length, 'open cycle')}`, to: '/admin/cycles' },
  ];
  const completedSteps = setupSteps.filter((step) => step.done).length;
  const allDone = completedSteps === setupSteps.length;

  const attention = [];
  if (households.length === 0) attention.push({ text: 'No households have been added yet.', to: '/admin/households', cta: 'Add households' });
  if (missingMeters > 0) attention.push({ text: `${plural(missingMeters, 'household')} without a meter number. Bills cannot be calculated for them.`, to: '/admin/households', cta: 'Add meter numbers' });
  if (unlinkedResidents > 0) attention.push({ text: `${plural(unlinkedResidents, 'resident')} not linked to a household yet.`, to: '/admin/residents', cta: 'Link residents' });
  if (activeTariffs === 0) attention.push({ text: 'There is no active tariff plan. Create one before billing.', to: '/admin/tariffs', cta: 'Create tariff' });

  const address = apartment ? [apartment.address, apartment.city].filter(Boolean).join(', ') : '';
  const adminName = (user && typeof user.name === 'string' && user.name.trim().split(/\s+/)[0]) || 'there';
  const totalCycles = cycles.length;
  const pct = (count) => (totalCycles ? (count / totalCycles) * 100 : 0);

  return (
    <div className="wo-root">
      <style>{STYLES}</style>

      <section className="wo-hero">
        <div className="wo-hero-copy">
          <h1>{greeting()}, {adminName}</h1>
          <p>Here is a quick look at {apartment?.name || 'your community'} and what needs your attention today.</p>
          {address && (
            <span className="wo-address"><Icon name="pin" /><span>{address}</span></span>
          )}
        </div>
        <div className="wo-hero-aside">
          <small>Today</small>
          <strong>{todayLabel}</strong>
          <small>Apartment #{apartment?.id ?? apartmentId}</small>
        </div>
      </section>

      <section className="wo-actions" aria-label="Quick actions">
        <QuickAction to="/admin/households" icon="plus" title="Add household" hint="Register a new unit" />
        <QuickAction to="/admin/residents" icon="residents" title="Add resident" hint="Invite or link a resident" />
        <QuickAction to="/admin/tariffs" icon="tariffs" title="Set up tariff" hint="Define water rates" />
        <QuickAction to="/admin/cycles" icon="cycles" title="Billing cycles" hint="Open or review cycles" />
      </section>

      <section className="wo-stats" aria-label="Apartment statistics">
        <StatCard icon="households" tone="teal" label="Households" value={households.length} note={`${meteredHouseholds} with a meter number`} />
        <StatCard icon="residents" tone="blue" label="Residents" value={residents.length} note={`${linkedResidents} linked to a household`} />
        <StatCard icon="tariffs" tone="gold" label="Active tariff plans" value={activeTariffs} note={`${tariffs.length} configured in total`} />
        <StatCard icon="cycles" tone="purple" label="Open billing cycles" value={openCycles.length} note={`${totalCycles} cycles in total`} />
      </section>

      <div className="wo-grid">
        <section className="wo-panel" aria-labelledby="wo-setup-title">
          <div className="wo-panel-head">
            <div>
              <h2 id="wo-setup-title">Setup progress</h2>
              <p>Finish these steps to start billing your residents.</p>
            </div>
            <span className={`wo-pill ${allDone ? 'wo-pill-done' : ''}`}>{completedSteps} of {setupSteps.length} complete</span>
          </div>
          <div className="wo-bar" aria-hidden="true">
            <span style={{ width: `${(completedSteps / setupSteps.length) * 100}%` }} />
          </div>
          <div className="wo-steps">
            {setupSteps.map((step) => <SetupStep key={step.label} {...step} />)}
          </div>
          {allDone && (
            <div className="wo-celebrate">Your apartment is fully set up. You can now manage billing from the Billing cycles page.</div>
          )}
        </section>

        <section className="wo-panel" aria-labelledby="wo-cycle-title">
          <div className="wo-panel-head">
            <div>
              <h2 id="wo-cycle-title">Latest billing cycle</h2>
              <p>The most recent billing period for this apartment.</p>
            </div>
            <Link className="wo-link" to="/admin/cycles">View all <Icon name="arrow" /></Link>
          </div>
          {latestCycle ? (
            <div className="wo-cycle">
              <div className="wo-cycle-range">
                <small>Billing period</small>
                <strong>{dateStr(latestCycle.startDate)}<em>to</em>{dateStr(latestCycle.endDate)}</strong>
              </div>
              <div className="wo-cycle-meta">
                <div>
                  <small>Tariff plan</small>
                  <strong>{latestCycle.tariffPlanName || 'Not specified'}</strong>
                </div>
                <div>
                  <small>Status</small>
                  <StatusBadge status={latestCycle.status} />
                </div>
              </div>
              <Link className="wo-btn" to={`/admin/cycles/${latestCycle.id}`}>
                View cycle details <Icon name="arrow" />
              </Link>
            </div>
          ) : (
            <div className="wo-empty">
              <span className="wo-empty-icon"><Icon name="cycles" /></span>
              <strong>No billing cycles yet</strong>
              <p>Create your first billing cycle to start generating bills for residents.</p>
              <Link className="wo-btn" to="/admin/cycles">Create a billing cycle</Link>
            </div>
          )}
        </section>
      </div>

      <section className="wo-panel" aria-labelledby="wo-attention-title">
        <div className="wo-panel-head">
          <div>
            <h2 id="wo-attention-title">Needs your attention</h2>
            <p>Fix these items to keep billing accurate.</p>
          </div>
        </div>
        {attention.length > 0 ? (
          <div className="wo-attention">
            {attention.map((item) => (
              <div className="wo-attention-item" key={item.text}>
                <span>{item.text}</span>
                <Link to={item.to}>{item.cta}</Link>
              </div>
            ))}
          </div>
        ) : (
          <div className="wo-allgood">Everything looks good. There is nothing that needs your attention right now.</div>
        )}
      </section>

      <section className="wo-panel" aria-labelledby="wo-health-title">
        <div className="wo-panel-head">
          <div>
            <h2 id="wo-health-title">Apartment setup at a glance</h2>
            <p>How complete your meter, resident and billing data is.</p>
          </div>
        </div>
        <div className="wo-health">
          <Metric
            icon="meter"
            tone="teal"
            fill="teal"
            label="Meter coverage"
            value={meterCoverage === null ? '—' : `${meterCoverage}%`}
            percent={meterCoverage}
            detail={households.length ? `${meteredHouseholds} of ${households.length} households have a meter number` : 'Add households to see meter coverage'}
          />
          <Metric
            icon="residents"
            tone="blue"
            fill="blue"
            label="Resident linking"
            value={residentLinking === null ? '—' : `${residentLinking}%`}
            percent={residentLinking}
            detail={residents.length ? `${linkedResidents} of ${residents.length} residents are linked to a household` : 'No residents registered in this apartment yet'}
          />
          <div className="wo-metric">
            <div className="wo-metric-icon wo-tone-gold"><Icon name="cycles" /></div>
            <div className="wo-metric-body">
              <div className="wo-metric-head">
                <strong>Billing cycle status</strong>
                <span>{totalCycles}</span>
              </div>
              <div className="wo-stack" role="img" aria-label={`${openCycles.length} open, ${finalizedCycles.length} finalized, ${archivedCycles.length} archived`}>
                <span className="wo-seg-open" style={{ width: `${pct(openCycles.length)}%` }} />
                <span className="wo-seg-final" style={{ width: `${pct(finalizedCycles.length)}%` }} />
                <span className="wo-seg-arch" style={{ width: `${pct(archivedCycles.length)}%` }} />
              </div>
              <div className="wo-legend">
                <span><i className="wo-seg-open" />{openCycles.length} open</span>
                <span><i className="wo-seg-final" />{finalizedCycles.length} finalized</span>
                <span><i className="wo-seg-arch" />{archivedCycles.length} archived</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}