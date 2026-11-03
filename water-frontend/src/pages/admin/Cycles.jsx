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
  const [query, setQuery] = useState('');
  const list = useAsync(() => get(`/api/v1/apartments/${apartmentId}/billing-cycles`), [apartmentId]);
  const [creating, setCreating] = useState(false);
  const cycles = list.data || [];
  const filteredCycles = cycles.filter((cycle) => {
    const matchesStatus = !status || cycle.status === status;
    const period = `${dateStr(cycle.startDate)} ${dateStr(cycle.endDate)}`.toLowerCase();
    const matchesQuery = [String(cycle.id), cycle.tariffPlanName || '', period]
      .some((value) => value.toLowerCase().includes(query.trim().toLowerCase()));
    return matchesStatus && matchesQuery;
  });

  return (
    <div className="management-page">
      <PageHeader
        title="Billing cycles"
        subtitle={apartment ? `Review billing periods and cycle status for ${apartment.name}.` : 'Review billing periods and cycle status.'}
        actions={<button className="btn btn-primary" onClick={() => setCreating(true)}><span aria-hidden="true">＋</span> New cycle</button>}
      />
      <ErrorBox error={list.error} />
      <div className="management-summary-row">
        <div className="management-summary-card">
          <span className="management-summary-icon">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
          </span>
          <span><small>TOTAL CYCLES</small><strong>{list.loading ? '—' : cycles.length}</strong></span>
        </div>
        <div className="management-summary-card">
          <span className="management-summary-icon">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          </span>
          <span><small>OPEN</small><strong>{list.loading ? '—' : cycles.filter((cycle) => cycle.status === 'OPEN').length}</strong></span>
        </div>
        <div className="management-summary-card">
          <span className="management-summary-icon management-summary-icon-blue">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </span>
          <span><small>FINALIZED</small><strong>{list.loading ? '—' : cycles.filter((cycle) => cycle.status === 'FINALIZED').length}</strong></span>
        </div>
        <div className="management-summary-card">
          <span className="management-summary-icon management-summary-icon-gold">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="21 8 21 21 3 21 3 8" />
              <rect x="1" y="3" width="22" height="5" />
              <line x1="10" y1="12" x2="14" y2="12" />
            </svg>
          </span>
          <span><small>ARCHIVED</small><strong>{list.loading ? '—' : cycles.filter((cycle) => cycle.status === 'ARCHIVED').length}</strong></span>
        </div>
      </div>
      <Card title="Billing history" actions={<span className="management-result-count">{filteredCycles.length} {filteredCycles.length === 1 ? 'cycle' : 'cycles'}</span>}>
        <div className="management-toolbar">
          <label className="management-search">
            <span aria-hidden="true">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </span>
            <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search cycle, dates or tariff" aria-label="Search billing cycles" />
          </label>
          <label className="management-filter">
            <span>Status</span>
            <select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Filter billing cycles by status">
              <option value="">All statuses</option>
              <option value="OPEN">Open</option>
              <option value="FINALIZED">Finalized</option>
              <option value="ARCHIVED">Archived</option>
            </select>
          </label>
        </div>
        {list.loading ? <Spinner /> : cycles.length === 0 ? (
          <Empty>No billing cycles found.</Empty>
        ) : filteredCycles.length === 0 ? <Empty>No cycles match your search or filter.</Empty> : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>ID</th><th>Period</th><th>Tariff plan</th><th>Status</th><th /></tr></thead>
              <tbody>
                {filteredCycles.map((c) => (
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
    </div>
  );
}
