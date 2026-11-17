import { useState } from 'react';
import { api, del, get, post, put } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { dateStr, money, today } from '../../fmt.js';
import { Alert, Badge, Card, Empty, ErrorBox, Field, FormModal, PageHeader, Spinner, useAction, useAsync } from '../../ui.jsx';

function TariffForm({ apartmentId, initial, onClose }) {
  const defaultSlabs = initial?.slabs?.length
    ? initial.slabs.map((slab) => ({ upToKl: slab.upToKl ?? '', rate: slab.rate }))
    : initial
      ? [
        { upToKl: initial.tier1LimitKl, rate: initial.baseRate },
        { upToKl: '', rate: initial.higherRate },
      ]
      : [{ upToKl: 10, rate: '' }, { upToKl: '', rate: '' }];
  const [f, setF] = useState(initial
    ? { name: initial.name, slabs: defaultSlabs, effectiveFrom: initial.effectiveFrom, active: initial.active }
    : { name: '', slabs: defaultSlabs, effectiveFrom: today(), active: true });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const updateSlab = (index, key, value) => setF({
    ...f,
    slabs: f.slabs.map((slab, i) => (i === index ? { ...slab, [key]: value } : slab)),
  });

  const body = () => {
    const slabs = f.slabs.map((slab, index) => ({
      upToKl: index === f.slabs.length - 1 ? null : Number(slab.upToKl),
      rate: Number(slab.rate),
    }));
    return {
      name: f.name.trim(),
      tier1LimitKl: slabs[0].upToKl ?? 0,
      baseRate: slabs[0].rate,
      higherRate: slabs[1]?.rate ?? slabs[0].rate,
      effectiveFrom: f.effectiveFrom,
      active: f.active,
      slabs,
    };
  };

  return (
    <FormModal
      title={initial ? 'Edit tariff plan' : 'New tariff plan'}
      onClose={onClose}
      onSubmit={() => (initial ? put(`/api/v1/tariffs/${initial.id}`, body()) : post(`/api/v1/apartments/${apartmentId}/tariffs`, body()))}
    >
      <Field label="Plan name"><input required maxLength={100} value={f.name} onChange={set('name')} /></Field>
      <div className="field">
        <span className="field-label">Progressive price slabs</span>
        <div className="stack-sm">
          {f.slabs.map((slab, index) => {
            const last = index === f.slabs.length - 1;
            const from = index === 0 ? 0 : f.slabs[index - 1].upToKl;
            return (
              <div className="grid-2" key={index}>
                <Field label={`Slab ${index + 1} (${from || 0} kL and above)`}>
                  {last ? <input value="No upper limit" readOnly aria-label={`Slab ${index + 1} upper limit`} />
                    : <input type="number" required min="0.01" step="0.01" value={slab.upToKl}
                      aria-label={`Slab ${index + 1} upper limit in kL`}
                      onChange={(event) => updateSlab(index, 'upToKl', event.target.value)} />}
                </Field>
                <Field label="Rate per kL">
                  <div className="row gap">
                    <input type="number" required min="0" step="0.01" value={slab.rate}
                      aria-label={`Slab ${index + 1} rate per kL`}
                      onChange={(event) => updateSlab(index, 'rate', event.target.value)} />
                    {f.slabs.length > 1 && <button type="button" className="btn btn-ghost btn-sm"
                      onClick={() => setF({ ...f, slabs: f.slabs.filter((_, i) => i !== index) })}>Remove</button>}
                  </div>
                </Field>
              </div>
            );
          })}
          <button type="button" className="btn btn-ghost btn-sm"
            onClick={() => setF({
              ...f,
              slabs: [...f.slabs.slice(0, -1), { upToKl: '', rate: '' }, f.slabs[f.slabs.length - 1]],
            })}>
            Add slab
          </button>
        </div>
        <span className="field-hint">Each limit is cumulative in kL. The final slab has no upper limit; rates cannot decrease.</span>
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
  const [activationId, setActivationId] = useState(null);
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
        Each rate applies only to consumption within its slab. Plans used by finalized or archived cycles cannot have their slabs changed.
      </Alert>
      <ErrorBox error={list.error || act.error} />
      {act.message && <Alert type="success">{act.message}</Alert>}
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
              <thead><tr><th>Name</th><th>Progressive slabs</th><th>Effective from</th><th>Status</th><th /></tr></thead>
              <tbody>
                {filteredTariffs.map((t) => (
                  <tr key={t.id}>
                    <td><strong>{t.name}</strong></td>
                    <td>{(t.slabs || []).map((slab) => (
                      `${slab.fromKl}-${slab.upToKl == null ? '∞' : slab.upToKl} kL @ ${money(slab.rate)}`
                    )).join(' · ')}</td>
                    <td>{dateStr(t.effectiveFrom)}</td>
                    <td><Badge tone={t.active ? 'green' : 'grey'}>{t.active ? 'Active' : 'Inactive'}</Badge></td>
                    <td className="right">
                      <div className="row gap end">
                        <button className="btn btn-ghost btn-sm" onClick={() => setModal({ item: t })}>Edit</button>
                        <button
                          className="btn btn-ghost btn-sm"
                          disabled={activationId !== null}
                          onClick={async () => {
                            setActivationId(t.id);
                            await act.run(
                              () => api(`/api/v1/tariffs/${t.id}/activation?active=${!t.active}`, { method: 'PATCH' }),
                              { success: `Tariff plan "${t.name}" ${t.active ? 'deactivated' : 'activated'}.` }
                            );
                            setActivationId(null);
                          }}
                        >{activationId === t.id ? 'Updating...' : t.active ? 'Deactivate' : 'Activate'}</button>
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
