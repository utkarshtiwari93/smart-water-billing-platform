import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { get } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { dateStr, daysAgo, litres, money, number, today } from '../../fmt.js';
import { Alert, Card, Empty, ErrorBox, PageHeader, Spinner, Stat, useAsync } from '../../ui.jsx';

/**
 * Resident dashboard
 * ----------------------------------------------------------------------------
 * UI-only refresh. The data logic is unchanged:
 *  - Same imports and same default export (Dashboard).
 *  - Same requests: usage, usage/trend, usage/compare, notifications (unread),
 *    invoices/my, resident/current-cycle and usage/benchmark, with the same
 *    parameters and the same dependency lists.
 *  - Same derived values (total, peak, previous-period comparison, chart data).
 *  - Same shared components (PageHeader, Card, Stat, Alert, Empty, ErrorBox,
 *    Spinner) and the same global classes (resident-page, stats, grid-2, ...).
 *
 * Added on the client only: a plain-language summary line, one consistent icon
 * set, a clearer "not linked yet" screen, and a "How to read this dashboard"
 * section. No new requests are made.
 *
 * Small fix: the "highest day" hint used point.date, which the trend points do
 * not have (they use point.period), so it showed nothing. It now shows the
 * formatted period.
 *
 * Styling is injected by this file and every selector starts with .rdb-page, so
 * no other page is affected.
 */

const TIPS = [
  { title: 'Check for hidden leaks', detail: 'Turn off taps and appliances, then check whether the meter still moves.' },
  { title: 'Use water-efficient habits', detail: 'Shorter showers and turning off the tap while brushing can reduce daily use.' },
  { title: 'Run full loads', detail: 'Wait until your washing machine or dishwasher is full before starting a cycle.' },
  { title: 'Reuse where you can', detail: 'Water plants with the leftover water from rinsing vegetables or filling a kettle.' },
];

const GUIDE = [
  { q: 'How is my usage calculated?', a: 'Consumption comes from the difference between meter readings. Daily values are estimates: each meter-reading interval is spread evenly across the calendar days it covers. Days without a covering interval are left blank on the chart.' },
  { q: 'What are covered days?', a: 'Days that fall inside an interval between two valid meter readings. A second valid reading is needed before consumption can be calculated.' },
  { q: 'How does the apartment comparison work?', a: 'Your usage is compared with the average of households that have valid readings. Similar homes have floor areas within 20%, and where both homes have recorded occupancy it must also be within one person.' },
  { q: 'Why is the charge an estimate?', a: 'The estimated cycle charge is projected from covered-day usage and excludes shared and other charges. Your final bill appears after the admin closes the billing cycle.' },
];

const COLORS = { tide: '#0a6ea8', brass: '#a9681d', grid: '#e3ebf1', axis: '#5b7186' };

function StatIcon({ name }) {
  const paths = {
    drop: <path d="M12 2.7 17.7 8.4a8 8 0 1 1-11.4 0z" />,
    pulse: <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />,
    compare: <><path d="M7 4v16M7 20l-3-3m3 3 3-3" /><path d="M17 20V4m0 0-3 3m3-3 3 3" /></>,
    peak: <><path d="M3 20h18" /><path d="m5 20 5-9 3 5 3-6 3 10" /></>,
    clock: <><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></>,
    home: <><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" /><polyline points="9 22 9 12 15 12 15 22" /></>,
  };
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: name === 'peak' || name === 'compare' ? COLORS.brass : COLORS.tide }} aria-hidden="true">
      {paths[name]}
    </svg>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  const [days, setDays] = useState(30);
  const [trend, setTrend] = useState('daily');
  const periodDays = trend === 'monthly' ? 365 : days;
  const periodStart = daysAgo(periodDays - 1);

  const usage = useAsync(
    () => (user.householdId
      ? get(`/api/v1/usage?householdId=${user.householdId}&from=${periodStart}&to=${today()}`)
      : Promise.resolve([])),
    [user.householdId, periodDays]
  );
  const trendData = useAsync(
    () => (user.householdId
      ? get(`/api/v1/usage/trend?householdId=${user.householdId}&from=${periodStart}&to=${today()}&granularity=${trend === 'monthly' ? 'MONTH' : 'DAY'}`)
      : Promise.resolve(null)),
    [user.householdId, periodDays, trend]
  );
  const comparison = useAsync(
    () => (user.householdId
      ? get(`/api/v1/usage/compare?householdId=${user.householdId}&from=${periodStart}&to=${today()}`)
      : Promise.resolve(null)),
    [user.householdId, periodDays]
  );
  const alerts = useAsync(() => get('/api/v1/notifications?unreadOnly=true'), []);
  const invoices = useAsync(() => (user.householdId ? get('/api/v1/invoices/my') : Promise.resolve([])), [user.householdId]);
  const cycle = useAsync(() => (user.householdId ? get('/api/v1/resident/current-cycle') : Promise.resolve(null)), [user.householdId]);
  const benchmark = useAsync(
    () => (user.householdId
      ? get(`/api/v1/usage/benchmark?from=${daysAgo(29)}&to=${today()}`)
      : Promise.resolve(null)),
    [user.householdId]
  );

  if (!user.householdId) {
    return (
      <div className="resident-page rdb-page">
        <style>{STYLES}</style>
        <PageHeader title={`Hello, ${user.name}`} subtitle="Your water usage at a glance" />
        <section className="rdb-notice">
          <span className="rdb-notice-icon"><StatIcon name="home" /></span>
          <div>
            <strong>Your household portal is almost ready</strong>
            <p>You are not linked to a flat yet. Ask your society admin to assign your account to your household, then refresh this page.</p>
            <ol className="rdb-notice-steps">
              <li>Share your registered email address with your society administrator.</li>
              <li>Ask them to link your account to your household.</li>
              <li>Refresh this page. Your readings, invoices and alerts will appear.</li>
            </ol>
          </div>
          <Link className="btn btn-ghost" to="/profile">View account</Link>
        </section>
      </div>
    );
  }

  const rows = usage.data || [];
  const points = trendData.data?.points || [];
  const total = Number(trendData.data?.totalConsumptionLitres ?? 0);
  const peak = points.reduce((m, point) => (
    point.consumptionLitres != null && Number(point.consumptionLitres) > Number(m?.consumptionLitres ?? -1)
      ? point
      : m
  ), null);
  const last = rows[rows.length - 1];
  const latestInvoice = (invoices.data || [])[0];

  const periodLabel = (period) => {
    try {
      return trend === 'monthly'
        ? new Intl.DateTimeFormat('en-IN', { month: 'short', year: '2-digit' }).format(new Date(`${period}-01T00:00:00`))
        : dateStr(period);
    } catch (e) {
      return String(period);
    }
  };
  const chart = points.map((point) => ({
    date: periodLabel(point.period),
    litres: point.consumptionLitres == null ? null : Number(point.consumptionLitres),
  }));
  const previousTotal = comparison.data?.previousConsumptionLitres;
  const previousPercent = comparison.data?.percentageDifference == null
    ? null
    : Number(comparison.data.percentageDifference);
  const periodComparison = previousPercent == null ? 'Not enough prior data'
    : `${previousPercent > 0 ? '+' : ''}${previousPercent.toFixed(1)}%`;

  // Plain-language summary built from the values above.
  const coveredDays = trendData.data?.coveredDays ?? 0;
  let insight = null;
  if (!trendData.loading && !trendData.error && trendData.data) {
    if (coveredDays === 0) {
      insight = 'We need at least two valid meter readings before we can show your usage.';
    } else {
      insight = `You used ${litres(total)} over the last ${trend === 'monthly' ? '12 months' : `${periodDays} days`}.`;
      if (previousPercent != null && previousPercent !== 0) {
        insight += ` That is ${Math.abs(previousPercent).toFixed(1)}% ${previousPercent > 0 ? 'more' : 'less'} than the previous comparable period.`;
      }
      if (peak) {
        insight += ` Your highest ${trend === 'monthly' ? 'month' : 'estimated day'} was ${litres(peak.consumptionLitres)} (${periodLabel(peak.period)}).`;
      }
    }
  }

  return (
    <div className="resident-page rdb-page">
      <style>{STYLES}</style>
      <PageHeader
        title={`Hello, ${user.name}`}
        subtitle={`Your water usage overview · Household #${user.householdId}`}
        actions={(
          <div className="row gap">
            <label className="sr-only" htmlFor="usage-trend">Trend period</label>
            <select id="usage-trend" value={trend} onChange={(e) => setTrend(e.target.value)}>
              <option value="daily">Daily</option>
              <option value="monthly">Monthly</option>
            </select>
            {trend === 'daily' && (
              <select value={days} onChange={(e) => setDays(Number(e.target.value))} aria-label="Select timeframe">
                <option value={7}>Last 7 days</option>
                <option value={30}>Last 30 days</option>
                <option value={90}>Last 90 days</option>
              </select>
            )}
          </div>
        )}
      />

      <ErrorBox error={usage.error} />
      <ErrorBox error={trendData.error} />
      <ErrorBox error={comparison.error} />
      <ErrorBox error={alerts.error} />
      <ErrorBox error={invoices.error} />
      {(alerts.data || []).slice(0, 2).map((a) => (
        <Alert key={a.id} type="error">
          <strong>{a.type === 'LEAK_SUSPECTED' ? 'Possible leak detected. ' : 'High water usage. '}</strong>
          {a.message}{' '}
          <Link to="/notifications" style={{ textDecoration: 'underline', marginLeft: 6 }}>See all alerts</Link>
        </Alert>
      ))}

      {insight && <p className="rdb-insight">{insight}</p>}

      <div className="stats">
        <Stat
          label={`Water used (${periodDays} days)`}
          value={litres(total)}
          hint={`${trendData.data?.coveredDays ?? 0} covered days · ${rows.length} meter readings`}
          icon={<StatIcon name="drop" />}
        />
        <Stat
          label="Average per covered day"
          value={trendData.data?.coveredDays ? litres(total / trendData.data.coveredDays) : '-'}
          hint="Daily values are estimated between meter reads"
          icon={<StatIcon name="pulse" />}
        />
        <Stat
          label="Previous comparable period"
          value={periodComparison}
          hint={previousPercent == null
            ? comparison.data?.currentCoveredDays === 0
              ? 'No current interval coverage'
              : 'Insufficient prior interval coverage or previous usage was zero'
            : `${litres(comparison.data.currentConsumptionLitres)} now vs ${litres(previousTotal)} previously`}
          tone={previousPercent > 0 ? 'bad' : previousPercent < 0 ? 'good' : undefined}
          icon={<StatIcon name="compare" />}
        />
        <Stat
          label={trend === 'monthly' ? 'Highest month' : 'Highest estimated day'}
          value={peak ? litres(peak.consumptionLitres) : '-'}
          hint={peak ? periodLabel(peak.period) : 'No covered intervals'}
          tone={peak && Number(peak.consumptionLitres) > 1000 ? 'bad' : undefined}
          icon={<StatIcon name="peak" />}
        />
      </div>
      <div className="stats stats-3">
        <Stat
          label="Latest meter reading"
          value={last ? number(last.meterReading) : '-'}
          hint={last ? dateStr(last.readingDate) : 'No readings yet'}
          icon={<StatIcon name="clock" />}
        />
      </div>

      <Card title={trend === 'monthly' ? 'Monthly consumption trend' : 'Daily consumption trend'}>
        {trendData.loading ? <Spinner /> : trendData.data?.coveredDays === 0 ? (
          <Empty>No meter-reading intervals cover this period yet. A second valid meter reading is needed to calculate consumption.</Empty>
        ) : (
          <>
            <p className="muted small">Daily estimates spread each meter-reading interval evenly across calendar days. Gaps without a covering interval are left blank.</p>
            <div className="chart" role="img" aria-label={insight || 'Consumption trend chart'}>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={chart} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid stroke={COLORS.grid} strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="date" fontSize={11} stroke={COLORS.axis} tickLine={false} axisLine={{ stroke: COLORS.grid }} />
                  <YAxis fontSize={11} stroke={COLORS.axis} tickLine={false} axisLine={false} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#ffffff',
                      border: `1px solid ${COLORS.grid}`,
                      borderRadius: '8px',
                      boxShadow: '0 8px 20px rgba(11, 37, 64, 0.12)',
                      fontSize: '12px',
                    }}
                    formatter={(v) => [v == null ? 'No covered readings' : `${number(v)} L`, 'Consumption']}
                  />
                  <Line dataKey="litres" name="Consumption" type="monotone" stroke={COLORS.tide} strokeWidth={3}
                    connectNulls={false} dot={{ r: 3, fill: COLORS.tide }} activeDot={{ r: 5 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </>
        )}
      </Card>

      <div className="grid-2">
        <Card title="Current billing cycle">
          <ErrorBox error={cycle.error} />
          {cycle.loading ? <Spinner /> : cycle.error ? null : !cycle.data ? (
            <Empty>No billing cycle is active today.</Empty>
          ) : (
            <div className="stack">
              <div className="row between">
                <strong>{dateStr(cycle.data.startDate)} to {dateStr(cycle.data.endDate)}</strong>
                <span className="badge">{cycle.data.status}</span>
              </div>
              <Stat label="Your consumption this cycle" value={litres(cycle.data.consumptionLitres)} />
              <p className="muted small">
                {cycle.data.coveredDays > 0
                  ? `Based on ${cycle.data.coveredDays} covered day(s); latest reading ${cycle.data.latestReadingDate ? dateStr(cycle.data.latestReadingDate) : 'unavailable'}.`
                  : 'No valid reading interval covers this cycle yet.'}
              </p>
              {cycle.data.estimatedWaterCharge != null && (
                <Stat label="Estimated full-cycle water charges" value={money(cycle.data.estimatedWaterCharge)}
                  hint="Projected from covered-day usage; excludes shared and other charges" />
              )}
              <Link className="btn btn-ghost" to="/my-invoices">View invoice history</Link>
            </div>
          )}
        </Card>
        <Card title="Apartment usage comparison" actions={<span className="management-result-count">Last 30 days</span>}>
          <ErrorBox error={benchmark.error} />
          {benchmark.loading ? <Spinner /> : benchmark.error ? null : !benchmark.data ? (
            <Empty>Usage comparison is not available yet.</Empty>
          ) : (
            <div className="stack">
              <div className="chart">
                <ResponsiveContainer width="100%" height={210}>
                  <BarChart data={[
                    ...(benchmark.data.householdConsumptionLitres == null ? [] : [
                      { group: 'Your home', litres: Number(benchmark.data.householdConsumptionLitres) },
                    ]),
                    ...(benchmark.data.apartmentAverageLitres == null ? [] : [
                      { group: 'Apartment avg.', litres: Number(benchmark.data.apartmentAverageLitres) },
                    ]),
                    ...(benchmark.data.similarGroupAvailable
                      ? [{ group: 'Similar homes', litres: Number(benchmark.data.similarHouseholdAverageLitres) }]
                      : []),
                  ]} margin={{ top: 8, right: 12, left: -14, bottom: 0 }}>
                    <CartesianGrid stroke={COLORS.grid} strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="group" fontSize={10} stroke={COLORS.axis} tickLine={false} />
                    <YAxis fontSize={10} stroke={COLORS.axis} tickLine={false} axisLine={false} />
                    <Tooltip formatter={(value) => [`${number(value)} L`, 'Consumption']} />
                    <Bar dataKey="litres" name="Consumption" fill={COLORS.tide} radius={[5, 5, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
              {benchmark.data.householdConsumptionLitres == null && (
                <p className="muted small">A household comparison will appear after at least two valid readings establish a consumption interval.</p>
              )}
              {benchmark.data.householdVsApartmentPercent != null && (
                <p className="muted small">
                  Your usage is {Math.abs(Number(benchmark.data.householdVsApartmentPercent)).toFixed(1)}% {Number(benchmark.data.householdVsApartmentPercent) > 0 ? 'above' : 'below'} the average of households with valid readings.
                </p>
              )}
              {benchmark.data.householdPerOccupantLitres != null && benchmark.data.apartmentAveragePerOccupantLitres != null && (
                <p className="muted small">
                  Per recorded occupant: {litres(benchmark.data.householdPerOccupantLitres)} for your home vs {litres(benchmark.data.apartmentAveragePerOccupantLitres)} apartment average.
                </p>
              )}
              <p className="muted small">
                Similar homes have floor areas within 20%; where both households have recorded occupancy, occupancy must also be within one person.
                {benchmark.data.similarGroupAvailable
                  ? ` Compared with ${benchmark.data.similarHouseholdCount} similar household(s).`
                  : ' No similar-sized households are available for comparison yet.'}
              </p>
            </div>
          )}
        </Card>
      </div>

      <Card title="Water-saving tips">
        <ul className="rdb-tips">
          {TIPS.map((tip) => (
            <li key={tip.title}>
              <strong>{tip.title}</strong>
              <p>{tip.detail}</p>
            </li>
          ))}
        </ul>
      </Card>

      <Card title="Recent meter readings" actions={<span className="management-result-count">{rows.length} recorded</span>}>
        {usage.loading ? <Spinner /> : rows.length === 0 ? (
          <Empty>No meter readings are available for this period.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Reading date</th>
                  <th className="num">Meter reading</th>
                  <th className="num">Water used</th>
                </tr>
              </thead>
              <tbody>
                {rows.slice(-5).reverse().map((row) => (
                  <tr key={row.id}>
                    <td>{dateStr(row.readingDate)}</td>
                    <td className="num">{number(row.meterReading)}</td>
                    <td className="num"><strong>{litres(row.consumptionLitres)}</strong></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card title="Latest bill" actions={<Link className="btn btn-ghost" to="/my-invoices">All invoices</Link>}>
        {invoices.loading ? <Spinner /> : !latestInvoice ? (
          <Empty>No invoice yet. Your bill appears here after the admin closes the billing cycle.</Empty>
        ) : (
          <div className="row between">
            <div>
              <div className="muted small">Invoice #{latestInvoice.invoiceNumber}</div>
              <div style={{ fontWeight: 600, color: 'var(--ink-800)', marginTop: 2 }}>
                {dateStr(latestInvoice.periodStart)} to {dateStr(latestInvoice.periodEnd)}
              </div>
            </div>
            <div className="big-amount">{money(latestInvoice.totalAmount)}</div>
            <Link className="btn btn-primary" to={`/invoices/${latestInvoice.id}`}>View invoice</Link>
          </div>
        )}
      </Card>

      <section className="rdb-guide" aria-labelledby="rdb-guide-title">
        <h2 id="rdb-guide-title">How to read this dashboard</h2>
        {GUIDE.map((item) => (
          <details key={item.q}>
            <summary>{item.q}</summary>
            <p>{item.a}</p>
          </details>
        ))}
      </section>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Styles: every selector starts with .rdb-page                       */
/* ------------------------------------------------------------------ */

const STYLES = `
.rdb-page {
  --rdb-ink: #0b2540;
  --rdb-text: #35506a;
  --rdb-muted: #5b7186;
  --rdb-tide: #0a6ea8;
  --rdb-deep: #07496f;
  --rdb-foam: #e8f3f9;
  --rdb-paper: #f5f9fb;
  --rdb-line: #d8e5ed;
  --rdb-display: "Iowan Old Style", "Palatino Linotype", Palatino, "Book Antiqua", Georgia, serif;
}
.rdb-page *, .rdb-page *::before, .rdb-page *::after { box-sizing: border-box; }
.rdb-page summary:focus-visible { outline: 3px solid rgba(10, 110, 168, .5); outline-offset: 2px; border-radius: 6px; }

/* Plain-language summary */
.rdb-page .rdb-insight { margin: 0 0 16px; padding: 14px 18px; border-left: 4px solid var(--rdb-tide); border-radius: 0 10px 10px 0; background: var(--rdb-foam); color: var(--rdb-ink); font-size: 15.5px; line-height: 1.65; }

/* Not linked yet */
.rdb-page .rdb-notice { display: flex; align-items: flex-start; gap: 16px; padding: 22px; border: 1px solid var(--rdb-line); border-radius: 14px; background: #fff; }
.rdb-page .rdb-notice-icon { width: 44px; height: 44px; flex: none; display: grid; place-items: center; border-radius: 12px; background: var(--rdb-foam); }
.rdb-page .rdb-notice > div { flex: 1; min-width: 0; }
.rdb-page .rdb-notice strong { color: var(--rdb-ink); font-family: var(--rdb-display); font-size: 20px; font-weight: 600; }
.rdb-page .rdb-notice p { margin: 6px 0 0; color: var(--rdb-text); font-size: 14.5px; line-height: 1.65; }
.rdb-page .rdb-notice-steps { margin: 14px 0 0; padding: 0; display: grid; gap: 10px; list-style: none; counter-reset: rdb; }
.rdb-page .rdb-notice-steps li { position: relative; padding-left: 36px; counter-increment: rdb; color: var(--rdb-text); font-size: 14px; line-height: 1.6; }
.rdb-page .rdb-notice-steps li::before { content: counter(rdb); position: absolute; left: 0; top: -1px; width: 24px; height: 24px; display: grid; place-items: center; border-radius: 50%; background: var(--rdb-foam); color: var(--rdb-deep); font-size: 12.5px; font-weight: 800; }

/* Tips */
.rdb-page .rdb-tips { margin: 0; padding: 0; list-style: none; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 32px; }
.rdb-page .rdb-tips li { padding: 14px 0; border-top: 1px solid var(--rdb-line); }
.rdb-page .rdb-tips strong { color: var(--rdb-ink); font-size: 15px; }
.rdb-page .rdb-tips p { margin: 4px 0 0; color: var(--rdb-text); font-size: 14px; line-height: 1.65; }

/* Guide */
.rdb-page .rdb-guide { margin-top: 24px; padding: 26px 28px; border: 1px solid var(--rdb-line); border-radius: 14px; background: #fff; }
.rdb-page .rdb-guide h2 { margin: 0 0 8px; color: var(--rdb-ink); font-family: var(--rdb-display); font-size: 24px; font-weight: 600; letter-spacing: -.01em; }
.rdb-page .rdb-guide details { border-bottom: 1px solid var(--rdb-line); }
.rdb-page .rdb-guide details:first-of-type { border-top: 1px solid var(--rdb-line); }
.rdb-page .rdb-guide summary { position: relative; padding: 14px 32px 14px 0; color: var(--rdb-ink); font-size: 14.5px; font-weight: 700; cursor: pointer; list-style: none; }
.rdb-page .rdb-guide summary::-webkit-details-marker { display: none; }
.rdb-page .rdb-guide summary::after { content: "+"; position: absolute; right: 4px; top: 50%; transform: translateY(-50%); color: var(--rdb-tide); font-size: 22px; font-weight: 400; line-height: 1; }
.rdb-page .rdb-guide details[open] summary::after { content: "\\2212"; }
.rdb-page .rdb-guide details p { margin: 0; padding: 0 32px 16px 0; color: var(--rdb-text); font-size: 14px; line-height: 1.7; }

@media (max-width: 720px) {
  .rdb-page .rdb-notice { flex-direction: column; }
  .rdb-page .rdb-tips { grid-template-columns: minmax(0, 1fr); }
  .rdb-page .rdb-guide { padding: 20px 16px; }
  .rdb-page .rdb-insight { font-size: 14.5px; }
}
@media (prefers-reduced-motion: reduce) {
  .rdb-page *, .rdb-page *::before, .rdb-page *::after { animation: none !important; transition: none !important; }
}
`;