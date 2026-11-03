import { useState } from 'react';
import { del, get, post, put } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { dateStr, money, number, today } from '../../fmt.js';
import { Alert, Badge, Card, Empty, ErrorBox, Field, FormModal, PageHeader, Spinner, useAction, useAsync } from '../../ui.jsx';

function TariffForm({ apartmentId, initial, onClose }) {
  const [f, setF] = useState(initial
    ? { name: initial.name, tier1LimitKl: initial.tier1LimitKl, baseRate: initial.baseRate, higherRate: initial.higherRate, effectiveFrom: initial.effectiveFrom, active: initial.active }
    : { name: '', tier1LimitKl: 10, baseRate: '', higherRate: '', effectiveFrom: today(), active: true });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const body = () => ({
    name: f.name.trim(),
    tier1LimitKl: Number(f.tier1LimitKl),
    baseRate: Number(f.baseRate),
    higherRate: Number(f.higherRate),
    effectiveFrom: f.effectiveFrom,
    active: f.active,
  });

  return (
    <FormModal
      title={initial ? 'Edit tariff plan' : 'New tariff plan'}
      onClose={onClose}
      onSubmit={() => (initial ? put(`/api/v1/tariffs/${initial.id}`, body()) : post(`/api/v1/apartments/${apartmentId}/tariffs`, body()))}
    >
      <Field label="Plan name"><input required maxLength={100} value={f.name} onChange={set('name')} /></Field>
      <Field label="Tier 1 limit (kL per cycle)" hint="Usage up to this limit is charged at the base rate.">
        <input type="number" required min="0.01" step="0.01" value={f.tier1LimitKl} onChange={set('tier1LimitKl')} />
      </Field>
      <div className="grid-2">
        <Field label="Base rate (per kL)"><input type="number" required min="0" step="0.01" value={f.baseRate} onChange={set('baseRate')} /></Field>
        <Field label="Higher rate (per kL)" hint="Usage above the limit. Must not be below the base rate.">
          <input type="number" required min="0" step="0.01" value={f.higherRate} onChange={set('higherRate')} />
        </Field>
      </div>
      <Field label="Effective from"><input type="date" required value={f.effectiveFrom} onChange={set('effectiveFrom')} /></Field>
      <label className="check-row">
        <input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} />
        <span>Active (can be used for new billing cycles)</span>
      </label>
    </FormModal>
  );
}

export default function Tariffs() {
  const { apartmentId, apartment } = useAdmin();
  const list = useAsync(() => get(`/api/v1/apartments/${apartmentId}/tariffs`), [apartmentId]);
  const [modal, setModal] = useState(null);
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const act = useAction(list.reload);
  const tariffs = list.data || [];
  const activeCount = tariffs.filter((tariff) => tariff.active).length;
  const filteredTariffs = tariffs.filter((tariff) => (
    tariff.name.toLowerCase().includes(query.trim().toLowerCase())
    && (statusFilter === 'all' || (statusFilter === 'active' ? tariff.active : !tariff.active))
  ));

  return (
    <div className="management-page">
      <PageHeader
        title="Tariff plans"
        subtitle={apartment ? `Configure progressive water rates for ${apartment.name}.` : 'Configure progressive water rates for the apartment.'}
        actions={<button className="btn btn-primary" onClick={() => setModal({})}><span aria-hidden="true">＋</span> New plan</button>}
      />
      <Alert type="info">
        Bill per household = (usage up to the limit x base rate) + (usage above the limit x higher rate).
        Rates are locked once a billing cycle using the plan is finalized.
      </Alert>
      <ErrorBox error={list.error || act.error} />
      {act.dialog}
      <div className="management-summary-row">
        <div className="management-summary-card">
          <span className="management-summary-icon">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M4 7h16M4 12h10M4 17h7" />
              <circle cx="19" cy="17" r="2" />
            </svg>
          </span>
          <span><small>TOTAL PLANS</small><strong>{list.loading ? '—' : tariffs.length}</strong></span>
        </div>
        <div className="management-summary-card">
          <span className="management-summary-icon management-summary-icon-blue">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </span>
          <span><small>ACTIVE PLANS</small><strong>{list.loading ? '—' : activeCount}</strong></span>
        </div>
        <div className="management-summary-card">
          <span className="management-summary-icon management-summary-icon-gold">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="8" y1="12" x2="16" y2="12" />
            </svg>
          </span>
          <span><small>INACTIVE PLANS</small><strong>{list.loading ? '—' : tariffs.length - activeCount}</strong></span>
        </div>
      </div>
      <Card title="Rate plans" actions={<span className="management-result-count">{filteredTariffs.length} {filteredTariffs.length === 1 ? 'plan' : 'plans'}</span>}>
        <div className="management-toolbar">
          <label className="management-search">
            <span aria-hidden="true">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </span>
            <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search tariff plans" aria-label="Search tariff plans" />
          </label>
          <label className="management-filter">
            <span>Status</span>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} aria-label="Filter tariff plans">
              <option value="all">All plans</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </label>
        </div>
        {list.loading ? <Spinner /> : tariffs.length === 0 ? (
          <Empty>No tariff plan yet. Create one before opening a billing cycle.</Empty>
        ) : filteredTariffs.length === 0 ? <Empty>No plans match your search or filter.</Empty> : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Name</th><th className="num">Limit (kL)</th><th className="num">Base rate</th><th className="num">Higher rate</th><th>Effective from</th><th>Status</th><th /></tr></thead>
              <tbody>
                {filteredTariffs.map((t) => (
                  <tr key={t.id}>
                    <td><strong>{t.name}</strong></td>
                    <td className="num">{number(t.tier1LimitKl)}</td>
                    <td className="num">{money(t.baseRate)}</td>
                    <td className="num">{money(t.higherRate)}</td>
                    <td>{dateStr(t.effectiveFrom)}</td>
                    <td><Badge tone={t.active ? 'green' : 'grey'}>{t.active ? 'Active' : 'Inactive'}</Badge></td>
                    <td className="right">
                      <div className="row gap end">
                        <button className="btn btn-ghost btn-sm" onClick={() => setModal({ item: t })}>Edit</button>
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => act.run(() => del(`/api/v1/tariffs/${t.id}`), { confirm: `Delete plan "${t.name}"?` })}
                        >Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      {modal && (
        <TariffForm apartmentId={apartmentId} initial={modal.item}
          onClose={(saved) => { setModal(null); if (saved) list.reload(); }} />
      )}
    </div>
  );
}
