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
  const invoices = useAsync(() => (user.householdId ? get('/api/v1/invoices/my') : Promise.resolve([])), [user.householdId]);

  if (!user.householdId) {
    return (
      <>
        <PageHeader title={`Hello, ${user.name}`} subtitle="Your water usage at a glance" />
        <Alert type="info">
          You are not linked to a flat yet. Ask your society admin to assign your account to your household,
          then refresh this page.
        </Alert>
      </>
    );
  }

  const rows = usage.data || [];
  const total = rows.reduce((s, r) => s + Number(r.consumptionLitres), 0);
  const peak = rows.reduce((m, r) => (Number(r.consumptionLitres) > Number(m?.consumptionLitres ?? -1) ? r : m), null);
  const last = rows[rows.length - 1];
  const latestInvoice = (invoices.data || [])[0];

  const chart = rows.map((r) => ({ date: dateStr(r.readingDate), litres: Number(r.consumptionLitres) }));

  return (
    <>
      <PageHeader
        title={`Hello, ${user.name}`}
        subtitle={`Household #${user.householdId}`}
        actions={(
          <select value={days} onChange={(e) => setDays(Number(e.target.value))}>
            <option value={7}>Last 7 days</option>
            <option value={30}>Last 30 days</option>
            <option value={90}>Last 90 days</option>
          </select>
        )}
      />

      <ErrorBox error={usage.error} />

      <div className="stats">
        <Stat label={`Water used (${days} days)`} value={litres(total)} hint={`${rows.length} readings`} />
        <Stat label="Average per reading" value={rows.length ? litres(total / rows.length) : '-'} />
        <Stat label="Highest reading" value={peak ? litres(peak.consumptionLitres) : '-'} hint={peak ? dateStr(peak.readingDate) : ''} />
        <Stat label="Latest meter reading" value={last ? number(last.meterReading) : '-'} hint={last ? dateStr(last.readingDate) : ''} />
      </div>

      <Card title="Water consumption">
        {usage.loading ? <Spinner /> : rows.length === 0 ? (
          <Empty>No readings in this period yet.</Empty>
        ) : (
          <div className="chart">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={chart}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="date" fontSize={12} />
                <YAxis fontSize={12} />
                <Tooltip formatter={(v) => [`${number(v)} L`, 'Consumption']} />
                <Bar dataKey="litres" fill="#0f8f8f" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </Card>

      <Card title="Latest bill" actions={<Link className="btn btn-ghost" to="/my-invoices">All invoices</Link>}>
        {invoices.loading ? <Spinner /> : !latestInvoice ? (
          <Empty>No invoice yet. Your bill appears here after the admin closes the billing cycle.</Empty>
        ) : (
          <div className="row between">
            <div>
              <div className="muted small">{latestInvoice.invoiceNumber}</div>
              <div>{dateStr(latestInvoice.periodStart)} to {dateStr(latestInvoice.periodEnd)}</div>
            </div>
            <div className="big-amount">{money(latestInvoice.totalAmount)}</div>
            <Link className="btn btn-primary" to={`/invoices/${latestInvoice.id}`}>View invoice</Link>
          </div>
        )}
      </Card>
    </>
  );
}
