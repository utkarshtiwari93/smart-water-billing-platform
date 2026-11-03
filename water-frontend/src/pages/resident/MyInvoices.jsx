import { useState } from 'react';
import { Link } from 'react-router-dom';
import { get } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { dateStr, dateTimeStr, litres, money, number } from '../../fmt.js';
import { Alert, Card, Empty, ErrorBox, PageHeader, Spinner, Stat, useAsync } from '../../ui.jsx';

export default function MyInvoices() {
  const { user } = useAuth();
  const [query, setQuery] = useState('');
  const { data, loading, error } = useAsync(
    () => (user.householdId ? get('/api/v1/invoices/my') : Promise.resolve([])),
    [user.householdId]
  );
  const invoices = data || [];
  const billedTotal = invoices.reduce((sum, invoice) => sum + Number(invoice.totalAmount), 0);
  const filteredInvoices = invoices.filter((invoice) => (
    [invoice.invoiceNumber, invoice.flatNumber, dateStr(invoice.periodStart), dateStr(invoice.periodEnd)]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(query.trim().toLowerCase()))
  ));

  return (
    <div className="resident-page">
      <PageHeader title="My invoices" subtitle="Billing statements and itemized charges for your household" />
      {!user.householdId && <Alert type="info">You are not linked to a flat yet. Ask your society administrator to assign your account.</Alert>}
      <ErrorBox error={error} />
      {user.householdId && !loading && !error && (
        <div className="stats stats-3 invoice-summary-stats">
          <Stat
            label="Total invoices"
            value={number(invoices.length)}
            hint="Billing cycles finalized"
            icon={(
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: 'var(--brand)' }}>
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
              </svg>
            )}
          />
          <Stat
            label="Total billed"
            value={money(billedTotal)}
            hint="Cumulative water charges"
            icon={(
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: 'var(--info)' }}>
                <line x1="12" y1="1" x2="12" y2="23" />
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
              </svg>
            )}
          />
          <Stat
            label="Latest invoice"
            value={invoices[0]?.invoiceNumber || '—'}
            hint={invoices[0] ? `${dateStr(invoices[0].periodStart)} to ${dateStr(invoices[0].periodEnd)}` : 'No invoices yet'}
            icon={(
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: 'var(--warning)' }}>
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
            )}
          />
        </div>
      )}
      <Card title="Invoice history" actions={!loading && !error && <span className="management-result-count">{filteredInvoices.length} {filteredInvoices.length === 1 ? 'statement' : 'statements'}</span>}>
        {invoices.length > 0 && (
          <div className="management-toolbar">
            <label className="management-search">
              <span aria-hidden="true">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
              </span>
              <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search invoice or billing period" aria-label="Search invoices" />
            </label>
          </div>
        )}
        {loading ? <Spinner /> : !data || data.length === 0 ? (
          <Empty>No invoices issued yet. You will see finalized statements here when billing cycles close.</Empty>
        ) : filteredInvoices.length === 0 ? <Empty>No invoices match your search.</Empty> : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Invoice #</th>
                  <th>Billing period</th>
                  <th>Flat</th>
                  <th className="num">Water consumption</th>
                  <th className="num">Total payable</th>
                  <th>Issued on</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {filteredInvoices.map((inv) => (
                  <tr key={inv.id}>
                    <td><strong>{inv.invoiceNumber}</strong></td>
                    <td>{dateStr(inv.periodStart)} to {dateStr(inv.periodEnd)}</td>
                    <td>{inv.flatNumber}</td>
                    <td className="num">{litres(inv.consumptionLitres)}</td>
                    <td className="num"><strong>{money(inv.totalAmount)}</strong></td>
                    <td>{dateTimeStr(inv.issuedAt)}</td>
                    <td className="right"><Link className="btn btn-ghost btn-sm" to={`/invoices/${inv.id}`}>View Invoice</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
