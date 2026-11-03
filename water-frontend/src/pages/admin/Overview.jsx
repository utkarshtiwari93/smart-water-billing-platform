import { Link } from 'react-router-dom';
import { get } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { useAuth } from '../../auth.jsx';
import { dateStr } from '../../fmt.js';
import { ErrorBox, Spinner, StatusBadge, useAsync } from '../../ui.jsx';

function OverviewIcon({ name }) {
  const paths = {
    households: <><path d="M3 10.5 12 3l9 7.5" /><path d="M5.5 9.5V21h13V9.5M9 21v-7h6v7" /></>,
    residents: <><circle cx="9" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M3 20v-1a6 6 0 0 1 12 0v1zm13-5a5 5 0 0 1 5 5h-3" /></>,
    tariffs: <><path d="M4 7h16M4 12h10M4 17h7" /><circle cx="19" cy="17" r="2" /></>,
    cycles: <><rect x="3.5" y="5" width="17" height="16" rx="2" /><path d="M7.5 3v4m9-4v4M3.5 9.5h17M8 13h3m-3 4h3m3-4h2m-2 4h2" /></>,
    meter: <><circle cx="12" cy="12" r="9" /><path d="M12 12 16 8m-4 4v5m-5 0h10" /></>,
    plus: <><path d="M12 5v14m-7-7h14" /></>,
    arrow: <><path d="M5 12h14m-6-6 6 6-6 6" /></>,
    check: <><path d="m5 12 4 4L19 6" /></>,
  };

  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name]}
    </svg>
  );
}

function StatCard({ icon, label, value, note, tone }) {
  return (
    <article className={`overview-stat overview-stat-${tone}`}>
      <div className="overview-stat-top">
        <span className="overview-stat-label">{label}</span>
        <span className="overview-stat-icon"><OverviewIcon name={icon} /></span>
      </div>
      <strong className="overview-stat-value">{value}</strong>
      <span className="overview-stat-note">{note}</span>
    </article>
  );
}

function ProgressRow({ label, detail, done, to }) {
  return (
    <Link className={`overview-setup-row ${done ? 'is-complete' : ''}`} to={to}>
      <span className="overview-setup-check">
        <OverviewIcon name={done ? 'check' : 'arrow'} />
      </span>
      <span className="overview-setup-copy">
        <strong>{label}</strong>
        <small>{detail}</small>
      </span>
      <span className={`overview-setup-state ${done ? 'is-complete' : ''}`}>{done ? 'Complete' : 'Get started'}</span>
    </Link>
  );
}

function ProgressMetric({ icon, label, value, detail, percent, tone }) {
  return (
    <div className="overview-progress-metric">
      <div className={`overview-metric-icon overview-metric-icon-${tone}`}><OverviewIcon name={icon} /></div>
      <div className="overview-progress-content">
        <div className="overview-progress-heading">
          <strong>{label}</strong>
          <span>{value}</span>
        </div>
        <div className="overview-progress-track" role="progressbar" aria-label={label} aria-valuemin={0} aria-valuemax={100} aria-valuenow={percent ?? 0}>
          <span className={`overview-progress-fill overview-progress-fill-${tone}`} style={{ width: `${percent ?? 0}%` }} />
        </div>
        <small>{detail}</small>
      </div>
    </div>
  );
}

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

  const residents = data.users.filter((item) => item.role === 'RESIDENT');
  const linkedResidents = residents.filter((item) => item.householdId).length;
  const meteredHouseholds = data.households.filter((item) => item.meterNumber).length;
  const activeTariffs = data.tariffs.filter((item) => item.active).length;
  const openCycles = data.cycles.filter((item) => item.status === 'OPEN');
  const finalizedCycles = data.cycles.filter((item) => item.status === 'FINALIZED');
  const archivedCycles = data.cycles.filter((item) => item.status === 'ARCHIVED');
  const latestCycle = data.cycles[0];
  const todayLabel = new Intl.DateTimeFormat('en-IN', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date());
  const meterCoverage = data.households.length
    ? Math.round((meteredHouseholds / data.households.length) * 100)
    : null;
  const residentLinking = residents.length
    ? Math.round((linkedResidents / residents.length) * 100)
    : null;
  const setupSteps = [
    { done: data.households.length > 0, label: 'Add households', detail: `${data.households.length} household${data.households.length === 1 ? '' : 's'} created`, to: '/admin/households' },
    { done: residents.length > 0 && linkedResidents === residents.length, label: 'Link residents', detail: `${linkedResidents} of ${residents.length} residents linked`, to: '/admin/residents' },
    { done: activeTariffs > 0, label: 'Create a tariff plan', detail: `${activeTariffs} active plan${activeTariffs === 1 ? '' : 's'}`, to: '/admin/tariffs' },
    { done: openCycles.length > 0, label: 'Open a billing cycle', detail: `${openCycles.length} open billing cycle${openCycles.length === 1 ? '' : 's'}`, to: '/admin/cycles' },
  ];
  const completedSteps = setupSteps.filter((step) => step.done).length;
  const address = apartment ? [apartment.address, apartment.city].filter(Boolean).join(', ') : '';
  const adminName = user?.name?.trim().split(/\s+/)[0] || 'there';

  return (
    <div className="admin-overview">
      <section className="overview-welcome">
        <div className="overview-welcome-copy">
          <span className="overview-eyebrow">APARTMENT OVERVIEW</span>
          <h1>Welcome back, {adminName}</h1>
          <p>Here’s what’s happening with {apartment?.name || 'your community'}.</p>
          {address && <span className="overview-address"><OverviewIcon name="households" />{address}</span>}
        </div>
        <div className="overview-welcome-aside">
          <span className="overview-date-label">TODAY</span>
          <strong>{todayLabel}</strong>
          <span className="overview-apartment-tag">Apartment #{apartment?.id ?? apartmentId}</span>
        </div>
      </section>

      <section className="overview-quick-actions" aria-label="Quick actions">
        <span className="overview-quick-label">QUICK ACTIONS</span>
        <Link to="/admin/households"><span><OverviewIcon name="plus" /></span>Add household<OverviewIcon name="arrow" /></Link>
        <Link to="/admin/residents"><span><OverviewIcon name="residents" /></span>Add resident<OverviewIcon name="arrow" /></Link>
        <Link to="/admin/tariffs"><span><OverviewIcon name="tariffs" /></span>Configure tariff<OverviewIcon name="arrow" /></Link>
        <Link to="/admin/cycles"><span><OverviewIcon name="cycles" /></span>Billing cycles<OverviewIcon name="arrow" /></Link>
      </section>

      <section className="overview-stats" aria-label="Apartment statistics">
        <StatCard icon="households" label="Total households" value={data.households.length} note={`${meteredHouseholds} with meter details`} tone="teal" />
        <StatCard icon="residents" label="Total residents" value={residents.length} note={`${linkedResidents} linked to a household`} tone="blue" />
        <StatCard icon="tariffs" label="Active tariff plans" value={activeTariffs} note={`${data.tariffs.length} plans configured`} tone="gold" />
        <StatCard icon="cycles" label="Open billing cycles" value={openCycles.length} note={`${data.cycles.length} cycles in total`} tone="purple" />
      </section>

      <div className="overview-main-grid">
        <section className="overview-panel overview-setup-panel">
          <div className="overview-panel-heading">
            <div>
              <span className="overview-section-kicker">GETTING STARTED</span>
              <h2>Setup progress</h2>
            </div>
            <span className="overview-progress-count">{completedSteps} of {setupSteps.length} complete</span>
          </div>
          <div className="overview-setup-progress" aria-label={`${completedSteps} of ${setupSteps.length} setup steps complete`}>
            <span style={{ width: `${(completedSteps / setupSteps.length) * 100}%` }} />
          </div>
          <div className="overview-setup-list">
            {setupSteps.map((step) => <ProgressRow key={step.label} {...step} />)}
          </div>
        </section>

        <section className="overview-panel overview-cycle-panel">
          <div className="overview-panel-heading">
            <div>
              <span className="overview-section-kicker">BILLING</span>
              <h2>Latest billing cycle</h2>
            </div>
            <Link className="overview-panel-link" to="/admin/cycles">All cycles <OverviewIcon name="arrow" /></Link>
          </div>
          {latestCycle ? (
            <div className="overview-latest-cycle">
              <div className="overview-cycle-icon"><OverviewIcon name="cycles" /></div>
              <span className="overview-cycle-period-label">BILLING PERIOD</span>
              <strong className="overview-cycle-period">{dateStr(latestCycle.startDate)} <span>to</span> {dateStr(latestCycle.endDate)}</strong>
              <div className="overview-cycle-meta">
                <span><small>TARIFF PLAN</small><strong>{latestCycle.tariffPlanName || 'Not specified'}</strong></span>
                <span><small>STATUS</small><StatusBadge status={latestCycle.status} /></span>
              </div>
              <Link className="btn btn-primary overview-cycle-button" to={`/admin/cycles/${latestCycle.id}`}>
                View cycle details <OverviewIcon name="arrow" />
              </Link>
            </div>
          ) : (
            <div className="overview-cycle-empty">
              <span className="overview-cycle-icon"><OverviewIcon name="cycles" /></span>
              <strong>No billing cycles yet</strong>
              <p>Create a cycle to start organizing your community billing.</p>
              <Link className="btn btn-primary btn-sm" to="/admin/cycles">Go to billing cycles</Link>
            </div>
          )}
        </section>
      </div>

      <section className="overview-panel overview-health-panel">
        <div className="overview-panel-heading">
          <div>
            <span className="overview-section-kicker">COMMUNITY READINESS</span>
            <h2>Apartment setup at a glance</h2>
          </div>
        </div>
        <div className="overview-health-grid">
          <ProgressMetric
            icon="meter"
            label="Meter coverage"
            value={meterCoverage === null ? '—' : `${meterCoverage}%`}
            detail={data.households.length ? `${meteredHouseholds} of ${data.households.length} households have a meter number` : 'Add households to calculate meter coverage'}
            percent={meterCoverage}
            tone="teal"
          />
          <ProgressMetric
            icon="residents"
            label="Resident linking"
            value={residentLinking === null ? '—' : `${residentLinking}%`}
            detail={residents.length ? `${linkedResidents} of ${residents.length} residents linked to a household` : 'No residents registered in this apartment yet'}
            percent={residentLinking}
            tone="blue"
          />
          <div className="overview-cycle-breakdown">
            <div className="overview-metric-icon overview-metric-icon-gold"><OverviewIcon name="cycles" /></div>
            <div className="overview-cycle-breakdown-copy">
              <strong>Billing cycle status</strong>
              <small>{data.cycles.length} total cycle{data.cycles.length === 1 ? '' : 's'}</small>
            </div>
            <div className="overview-cycle-statuses">
              <span><i className="overview-status-dot status-open" />{openCycles.length} open</span>
              <span><i className="overview-status-dot status-finalized" />{finalizedCycles.length} finalized</span>
              <span><i className="overview-status-dot status-archived" />{archivedCycles.length} archived</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
