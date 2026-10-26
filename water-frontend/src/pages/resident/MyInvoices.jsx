import { Link } from 'react-router-dom';
import { get } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { dateStr, dateTimeStr, litres, money } from '../../fmt.js';
import { Alert, Card, Empty, ErrorBox, PageHeader, Spinner, useAsync } from '../../ui.jsx';

export default function MyInvoices() {
  const { user } = useAuth();
  const { data, loading, error } = useAsync(
    () => (user.householdId ? get('/api/v1/invoices/my') : Promise.resolve([])),
    [user.householdId]
  );

  return (
    <>
      <PageHeader title="My invoices" subtitle="Bills for your household" />
      {!user.householdId && <Alert type="info">You are not linked to a flat yet. Ask your admin to assign you.</Alert>}
      <ErrorBox error={error} />
      <Card>
        {loading ? <Spinner /> : !data || data.length === 0 ? (
          <Empty>No invoices yet.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Invoice</th><th>Period</th><th>Flat</th><th className="num">Water used</th><th className="num">Total</th><th>Issued</th><th /></tr>
              </thead>
              <tbody>
                {data.map((inv) => (
                  <tr key={inv.id}>
                    <td>{inv.invoiceNumber}</td>
                    <td>{dateStr(inv.periodStart)} to {dateStr(inv.periodEnd)}</td>
                    <td>{inv.flatNumber}</td>
                    <td className="num">{litres(inv.consumptionLitres)}</td>
                    <td className="num"><strong>{money(inv.totalAmount)}</strong></td>
                    <td>{dateTimeStr(inv.issuedAt)}</td>
                    <td className="right"><Link className="btn btn-ghost btn-sm" to={`/invoices/${inv.id}`}>View</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}
