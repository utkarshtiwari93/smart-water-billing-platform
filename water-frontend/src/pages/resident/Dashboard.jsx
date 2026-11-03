import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { get } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { dateStr, daysAgo, litres, money, number, today } from '../../fmt.js';
import { Alert, Card, Empty, ErrorBox, PageHeader, Spinner, Stat, useAsync } from '../../ui.jsx';

export default function Dashboard() {
  const { user } = useAuth();
  const [days, setDays] = useState(30);

  const usage = useAsync(
    () => (user.householdId
      ? get(`/api/v1/usage?householdId=${user.householdId}&from=${daysAgo(days)}&to=${today()}`)
      : Promise.resolve([])),
    [user.householdId, days]
  );
  const alerts = useAsync(() => get('/api/v1/notifications?unreadOnly=true'), []);
  const invoices = useAsync(() => (user.householdId ? get('/api/v1/invoices/my') : Promise.resolve([])), [user.householdId]);

  if (!user.householdId) {
    return (
      <div className="resident-page">
        <PageHeader title={`Hello, ${user.name}`} subtitle="Your water usage at a glance" />
        <section className="resident-link-notice">
          <span className="resident-link-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
          </span>
          <div>
            <strong>Your household portal is almost ready</strong>
            <p>You are not linked to a flat yet. Ask your society admin to assign your account to your household, then refresh this page.</p>
          </div>
          <Link className="btn btn-ghost" to="/profile">View account</Link>
        </section>
      </div>
    );
  }

  const rows = usage.data || [];
  const total = rows.reduce((s, r) => s + Number(r.consumptionLitres), 0);
  const peak = rows.reduce((m, r) => (Number(r.consumptionLitres) > Number(m?.consumptionLitres ?? -1) ? r : m), null);
  const last = rows[rows.length - 1];
  const latestInvoice = (invoices.data || [])[0];

  const chart = rows.map((r) => ({ date: dateStr(r.readingDate), litres: Number(r.consumptionLitres) }));

  return (
    <div className="resident-page">
      <PageHeader
        title={`Hello, ${user.name}`}
        subtitle={`Your water usage overview · Household #${user.householdId}`}
        actions={(
          <select value={days} onChange={(e) => setDays(Number(e.target.value))} aria-label="Select timeframe">
            <option value={7}>Last 7 days</option>
            <option value={30}>Last 30 days</option>
            <option value={90}>Last 90 days</option>
          </select>
        )}
      />

      <ErrorBox error={usage.error} />
      <ErrorBox error={alerts.error} />
      <ErrorBox error={invoices.error} />
      {(alerts.data || []).slice(0, 2).map((a) => (
        <Alert key={a.id} type="error">
          <strong>{a.type === 'LEAK_SUSPECTED' ? 'Possible leak detected. ' : 'High water usage. '}</strong>
          {a.message}{' '}
          <Link to="/notifications" style={{ textDecoration: 'underline', marginLeft: 6 }}>See all alerts</Link>
        </Alert>
      ))}

      <div className="stats">
        <Stat
          label={`Water used (${days} days)`}
          value={litres(total)}
          hint={`${rows.length} readings recorded`}
          icon={(
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: 'var(--brand)' }}>
              <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />
            </svg>
          )}
        />
        <Stat
          label="Average per reading"
          value={rows.length ? litres(total / rows.length) : '-'}
          hint="Calculated across timeframe"
          icon={(
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: 'var(--info)' }}>
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
            </svg>
          )}
        />
        <Stat
          label="Highest reading"
          value={peak ? litres(peak.consumptionLitres) : '-'}
          hint={peak ? dateStr(peak.readingDate) : 'No peak recorded'}
          tone={peak && Number(peak.consumptionLitres) > 1000 ? 'bad' : undefined}
          icon={(
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: 'var(--warning)' }}>
              <line x1="12" y1="19" x2="12" y2="5" />
              <polyline points="5 12 12 5 19 12" />
            </svg>
          )}
        />
        <Stat
          label="Latest meter reading"
          value={last ? number(last.meterReading) : '-'}
          hint={last ? dateStr(last.readingDate) : 'No readings yet'}
          icon={(
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: 'var(--ink-muted)' }}>
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          )}
        />
      </div>

      <Card title="Water consumption">
        {usage.loading ? <Spinner /> : rows.length === 0 ? (
          <Empty>No readings in this period yet.</Empty>
        ) : (
          <div className="chart">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={chart} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid stroke="#f1f5f9" strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="date" fontSize={11} stroke="#94a3b8" tickLine={false} axisLine={{ stroke: '#e2e8f0' }} />
                <YAxis fontSize={11} stroke="#94a3b8" tickLine={false} axisLine={false} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#ffffff',
                    border: '1px solid #e2e8f0',
                    borderRadius: '8px',
                    boxShadow: '0 4px 12px rgba(15, 23, 42, 0.08)',
                    fontSize: '12px',
                  }}
                  formatter={(v) => [`${number(v)} L`, 'Consumption']}
                />
                <Bar dataKey="litres" fill="#0d9488" radius={[6, 6, 0, 0]} maxBarSize={48} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
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
    </div>
  );
}
