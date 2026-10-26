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
  const act = useAction(list.reload);

  return (
    <>
      <PageHeader
        title="Tariff plans"
        subtitle={apartment ? `Rate slabs for ${apartment.name}` : ''}
        actions={<button className="btn btn-primary" onClick={() => setModal({})}>New plan</button>}
      />
      <Alert type="info">
        Bill per household = (usage up to the limit x base rate) + (usage above the limit x higher rate).
        Rates are locked once a billing cycle using the plan is finalized.
      </Alert>
      <ErrorBox error={list.error || act.error} />
      <Card>
        {list.loading ? <Spinner /> : !list.data || list.data.length === 0 ? (
          <Empty>No tariff plan yet. Create one before opening a billing cycle.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Name</th><th className="num">Limit (kL)</th><th className="num">Base rate</th><th className="num">Higher rate</th><th>Effective from</th><th>Status</th><th /></tr></thead>
              <tbody>
                {list.data.map((t) => (
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
    </>
  );
}
