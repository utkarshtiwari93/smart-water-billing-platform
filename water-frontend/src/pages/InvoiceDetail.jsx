import { Link, useParams } from 'react-router-dom';
import { get } from '../api.js';
import { useAuth } from '../auth.jsx';
import { dateStr, dateTimeStr, litres, money, number } from '../fmt.js';
import { Badge, ErrorBox, PageHeader, Spinner, useAsync } from '../ui.jsx';

const TYPE_LABEL = {
  WATER_TIER1: 'Water (tier 1)',
  WATER_TIER2: 'Water (tier 2)',
  SHARED_AREA: 'Shared area',
  ADJUSTMENT: 'Adjustment',
};

export default function InvoiceDetail() {
  const { id } = useParams();
  const { isAdmin } = useAuth();
  const { data: inv, loading, error } = useAsync(() => get(`/api/v1/invoices/${id}`), [id]);

  if (loading) return <Spinner />;
  if (error) return <ErrorBox error={error} />;

  const back = isAdmin ? `/admin/cycles/${inv.cycleId}` : '/my-invoices';

  return (
    <>
      <div className="no-print">
        <PageHeader
          title={`Invoice ${inv.invoiceNumber}`}
          subtitle={`Flat ${inv.flatNumber}`}
          actions={(
            <>
              <Link className="btn btn-ghost" to={back}>Back</Link>
              <button className="btn btn-primary" onClick={() => window.print()}>Print / Save as PDF</button>
            </>
          )}
        />
      </div>

      <div className="invoice">
        <div className="invoice-head">
          <div>
            <div className="brand-name">Water Billing</div>
            <div className="muted small">Smart Water Usage Monitoring and Billing</div>
          </div>
          <div className="right">
            <div className="invoice-no">{inv.invoiceNumber}</div>
            <div className="muted small">Issued {dateTimeStr(inv.issuedAt)}</div>
          </div>
        </div>

        <div className="grid-3 invoice-meta">
          <div><div className="muted small">Flat</div><strong>{inv.flatNumber}</strong></div>
          <div><div className="muted small">Billing period</div><strong>{dateStr(inv.periodStart)} to {dateStr(inv.periodEnd)}</strong></div>
          <div><div className="muted small">Water used</div><strong>{litres(inv.consumptionLitres)}</strong></div>
        </div>

        <table className="table">
          <thead>
            <tr><th>Item</th><th>Description</th><th className="num">Qty (kL)</th><th className="num">Rate</th><th className="num">Amount</th></tr>
          </thead>
          <tbody>
            {inv.items.map((it, i) => (
              <tr key={i}>
                <td><Badge tone={it.type === 'ADJUSTMENT' ? 'amber' : 'blue'}>{TYPE_LABEL[it.type] || it.type}</Badge></td>
                <td>{it.description}</td>
                <td className="num">{it.quantity === null ? '-' : number(it.quantity)}</td>
                <td className="num">{it.unitRate === null ? '-' : money(it.unitRate)}</td>
                <td className="num">{money(it.amount)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr><td colSpan="4" className="right"><strong>Total payable</strong></td><td className="num total">{money(inv.totalAmount)}</td></tr>
          </tfoot>
        </table>
        <p className="muted small">This is a computer generated invoice.</p>
      </div>
    </>
  );
}
