import { useState } from 'react';
import { Link } from 'react-router-dom';
import { get, post } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { dateStr } from '../../fmt.js';
import { Card, Empty, ErrorBox, Field, FormModal, PageHeader, Spinner, StatusBadge, useAsync } from '../../ui.jsx';

function CycleForm({ apartmentId, onClose }) {
  const tariffs = useAsync(() => get(`/api/v1/apartments/${apartmentId}/tariffs`), [apartmentId]);
  const [f, setF] = useState({ startDate: '', endDate: '', tariffPlanId: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  return (
    <FormModal
      title="New billing cycle"
      onClose={onClose}
      onSubmit={() => post(`/api/v1/apartments/${apartmentId}/billing-cycles`, {
        startDate: f.startDate,
        endDate: f.endDate,
        tariffPlanId: f.tariffPlanId ? Number(f.tariffPlanId) : null,
      })}
    >
      <div className="grid-2">
        <Field label="Start date"><input type="date" required value={f.startDate} onChange={set('startDate')} /></Field>
        <Field label="End date"><input type="date" required min={f.startDate} value={f.endDate} onChange={set('endDate')} /></Field>
      </div>
      <Field label="Tariff plan" hint="Automatic uses the latest active plan effective on the start date.">
        <select value={f.tariffPlanId} onChange={set('tariffPlanId')}>
          <option value="">Automatic</option>
          {(tariffs.data || []).filter((t) => t.active).map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select>
      </Field>
      <p className="muted small">Cycles of the same apartment cannot overlap.</p>
    </FormModal>
  );
}

export default function Cycles() {
  const { apartmentId, apartment } = useAdmin();
  const [status, setStatus] = useState('');
  const list = useAsync(
    () => get(`/api/v1/apartments/${apartmentId}/billing-cycles${status ? `?status=${status}` : ''}`),
    [apartmentId, status]
  );
  const [creating, setCreating] = useState(false);

  return (
    <>
      <PageHeader
        title="Billing cycles"
        subtitle={apartment ? `Periods billed for ${apartment.name}` : ''}
        actions={(
          <>
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">All statuses</option>
              <option value="OPEN">Open</option>
              <option value="FINALIZED">Finalized</option>
              <option value="ARCHIVED">Archived</option>
            </select>
            <button className="btn btn-primary" onClick={() => setCreating(true)}>New cycle</button>
          </>
        )}
      />
      <ErrorBox error={list.error} />
      <Card>
        {list.loading ? <Spinner /> : !list.data || list.data.length === 0 ? (
          <Empty>No billing cycles found.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>ID</th><th>Period</th><th>Tariff plan</th><th>Status</th><th /></tr></thead>
              <tbody>
                {list.data.map((c) => (
                  <tr key={c.id}>
                    <td>#{c.id}</td>
                    <td><strong>{dateStr(c.startDate)} to {dateStr(c.endDate)}</strong></td>
                    <td>{c.tariffPlanName}</td>
                    <td><StatusBadge status={c.status} /></td>
                    <td className="right"><Link className="btn btn-primary btn-sm" to={`/admin/cycles/${c.id}`}>Open</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      {creating && (
        <CycleForm apartmentId={apartmentId} onClose={(saved) => { setCreating(false); if (saved) list.reload(); }} />
      )}
    </>
  );
}
