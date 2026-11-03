import { Link, useParams } from 'react-router-dom';
import { get } from '../api.js';
import { useAuth } from '../auth.jsx';
import { dateStr, dateTimeStr, litres, money, number } from '../fmt.js';
import { Badge, ErrorBox, PageHeader, Spinner, useAsync } from '../ui.jsx';

const TYPE_LABEL = {
  WATER_TIER1: 'Water consumption (tier 1)',
  WATER_TIER2: 'Water consumption (tier 2)',
  SHARED_AREA: 'Shared common area utility',
  ADJUSTMENT: 'Adjustment / Surcharge',
};

export default function InvoiceDetail() {
  const { id } = useParams();
  const { isAdmin } = useAuth();
  const { data: inv, loading, error } = useAsync(() => get(`/api/v1/invoices/${id}`), [id]);

  if (loading) return <Spinner />;
  if (error) return <ErrorBox error={error} />;

  const back = isAdmin ? `/admin/cycles/${inv.cycleId}` : '/my-invoices';

  return (
    <div className="invoice-page">
      <div className="no-print">
        <PageHeader
          title={`Invoice #${inv.invoiceNumber}`}
          subtitle={`Billing statement for Flat ${inv.flatNumber}`}
          actions={(
            <>
              <Link className="btn btn-ghost" to={back}>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" style={{ marginRight: 4 }}>
                  <line x1="19" y1="12" x2="5" y2="12" />
                  <polyline points="12 19 5 12 12 5" />
                </svg>
                Back
              </Link>
              <button className="btn btn-primary" onClick={() => window.print()}>
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" style={{ marginRight: 4 }}>
                  <polyline points="6 9 6 2 18 2 18 9" />
                  <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
                  <rect x="6" y="14" width="12" height="8" />
                </svg>
                Print / Save as PDF
              </button>
            </>
          )}
        />
      </div>

      <div className="invoice">
        <div className="invoice-head">
          <div>
            <div className="row gap" style={{ marginBottom: 6 }}>
              <div className="brand-mark" style={{ width: 34, height: 34, borderRadius: 8 }}>
                <svg viewBox="0 0 32 32" fill="none" width="20" height="20">
                  <path d="M16 3.5C12.2 9.1 6.8 14.2 6.8 20a9.2 9.2 0 0 0 18.4 0C25.2 14.2 19.8 9.1 16 3.5Z" fill="currentColor" />
                  <path d="M11.5 20.5a4.7 4.7 0 0 0 4.7 4.7" stroke="#D8F7F1" strokeWidth="1.8" strokeLinecap="round" />
                </svg>
              </div>
              <strong style={{ fontSize: 18, color: 'var(--navy-900)' }}>Smart Water Platform</strong>
            </div>
            <div className="muted small">Intelligent Water Usage Monitoring &amp; Automated Billing</div>
          </div>
          <div className="right">
            <div className="invoice-no">{inv.invoiceNumber}</div>
            <div className="muted small">Issued on {dateTimeStr(inv.issuedAt)}</div>
          </div>
        </div>

        <div className="grid-3 invoice-meta" style={{ padding: '16px 0', borderBottom: '1px solid var(--line)', marginBottom: 20 }}>
          <div>
            <div className="muted small" style={{ fontWeight: 600, textTransform: 'uppercase' }}>Household / Flat</div>
            <strong style={{ fontSize: 16 }}>{inv.flatNumber}</strong>
          </div>
          <div>
            <div className="muted small" style={{ fontWeight: 600, textTransform: 'uppercase' }}>Billing Period</div>
            <strong style={{ fontSize: 14 }}>{dateStr(inv.periodStart)} to {dateStr(inv.periodEnd)}</strong>
          </div>
          <div>
            <div className="muted small" style={{ fontWeight: 600, textTransform: 'uppercase' }}>Total Consumption</div>
            <strong style={{ fontSize: 16, color: 'var(--brand)' }}>{litres(inv.consumptionLitres)}</strong>
          </div>
        </div>

        <table className="table" style={{ marginTop: 12 }}>
          <thead>
            <tr>
              <th>Item Type</th>
              <th>Description</th>
              <th className="num">Quantity (kL)</th>
              <th className="num">Unit Rate</th>
              <th className="num">Amount</th>
            </tr>
          </thead>
          <tbody>
            {inv.items.map((it, i) => (
              <tr key={i}>
                <td><Badge tone={it.type === 'ADJUSTMENT' ? 'amber' : 'blue'}>{TYPE_LABEL[it.type] || it.type}</Badge></td>
                <td>{it.description}</td>
                <td className="num">{it.quantity === null ? '-' : number(it.quantity)}</td>
                <td className="num">{it.unitRate === null ? '-' : money(it.unitRate)}</td>
                <td className="num"><strong>{money(it.amount)}</strong></td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td colSpan="4" className="right" style={{ fontSize: 15, fontWeight: 700 }}>Total Payable Amount:</td>
              <td className="num total" style={{ fontSize: 20, color: 'var(--navy-900)' }}>{money(inv.totalAmount)}</td>
            </tr>
          </tfoot>
        </table>

        <div style={{ marginTop: 32, padding: 14, background: 'var(--bg-subtle)', borderRadius: 8, fontSize: 12, color: 'var(--ink-muted)' }}>
          This is an official computer-generated utility statement. All consumption data reflects recorded household meter intervals and approved society tariff formulas.
        </div>
      </div>
    </div>
  );
}
