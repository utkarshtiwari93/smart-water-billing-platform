import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { del, get, post, put } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { dateStr, litres, money, number } from '../../fmt.js';
import {
  Alert, Badge, Card, Empty, ErrorBox, Field, FormModal, PageHeader, Spinner, Stat, StatusBadge,
  Tabs, useAction, useAsync,
} from '../../ui.jsx';

const SOURCES = ['TANKER', 'MUNICIPAL', 'BOREWELL', 'OTHER'];

/* ---------- Overview ---------- */

function CycleEditForm({ cycle, onClose }) {
  const tariffs = useAsync(() => get(`/api/v1/apartments/${cycle.apartmentId}/tariffs`), [cycle.apartmentId]);
  const [f, setF] = useState({ startDate: cycle.startDate, endDate: cycle.endDate, tariffPlanId: String(cycle.tariffPlanId) });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  return (
    <FormModal
      title="Edit billing cycle"
      onClose={onClose}
      onSubmit={() => put(`/api/v1/billing-cycles/${cycle.id}`, {
        startDate: f.startDate, endDate: f.endDate, tariffPlanId: Number(f.tariffPlanId),
      })}
    >
      <div className="grid-2">
        <Field label="Start date"><input type="date" required value={f.startDate} onChange={set('startDate')} /></Field>
        <Field label="End date"><input type="date" required min={f.startDate} value={f.endDate} onChange={set('endDate')} /></Field>
      </div>
      <Field label="Tariff plan">
        <select value={f.tariffPlanId} onChange={set('tariffPlanId')}>
          {(tariffs.data || []).filter((t) => t.active || t.id === cycle.tariffPlanId)
            .map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select>
      </Field>
    </FormModal>
  );
}

function OverviewTab({ cycle, reload }) {
  const [editing, setEditing] = useState(false);
  const open = cycle.status === 'OPEN';
  const steps = [
    ['OPEN', 'Collect readings, record bulk purchases and adjustments'],
    ['FINALIZED', 'Invoices are generated. Readings inputs, rates and purchases are locked'],
    ['ARCHIVED', 'Closed cycle kept for records'],
  ];
  return (
    <div className="grid-2">
      <Card
        title="Cycle details"
        actions={open && <button className="btn btn-ghost btn-sm" onClick={() => setEditing(true)}>Edit</button>}
      >
        <dl className="kv">
          <dt>Period</dt><dd>{dateStr(cycle.startDate)} to {dateStr(cycle.endDate)}</dd>
          <dt>Tariff plan</dt><dd>{cycle.tariffPlanName}</dd>
          <dt>Status</dt><dd><StatusBadge status={cycle.status} /></dd>
          <dt>Apartment</dt><dd>#{cycle.apartmentId}</dd>
        </dl>
      </Card>
      <Card title="How a cycle works">
        <ol className="steps">
          {steps.map(([s, text]) => (
            <li key={s} className={cycle.status === s ? 'current' : ''}>
              <StatusBadge status={s} /> <span>{text}</span>
            </li>
          ))}
        </ol>
      </Card>
      {editing && (
        <CycleEditForm cycle={cycle} onClose={(saved) => { setEditing(false); if (saved) reload(); }} />
      )}
    </div>
  );
}

/* ---------- Purchases ---------- */

function PurchaseForm({ cycle, initial, onClose }) {
  const [f, setF] = useState(initial
    ? { purchaseDate: initial.purchaseDate, source: initial.source, supplier: initial.supplier || '', volumeKl: initial.volumeKl, unitCost: initial.unitCost, notes: initial.notes || '' }
    : { purchaseDate: cycle.startDate, source: 'TANKER', supplier: '', volumeKl: '', unitCost: '', notes: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const total = Number(f.volumeKl) * Number(f.unitCost);

  const body = () => ({
    purchaseDate: f.purchaseDate,
    source: f.source,
    supplier: f.supplier.trim(),
    volumeKl: Number(f.volumeKl),
    unitCost: Number(f.unitCost),
    notes: f.notes.trim(),
  });

  return (
    <FormModal
      title={initial ? 'Edit purchase' : 'Add bulk purchase'}
      onClose={onClose}
      onSubmit={() => (initial ? put(`/api/v1/purchases/${initial.id}`, body()) : post(`/api/v1/billing-cycles/${cycle.id}/purchases`, body()))}
    >
      <div className="grid-2">
        <Field label="Purchase date" hint={`Within ${dateStr(cycle.startDate)} to ${dateStr(cycle.endDate)}`}>
          <input type="date" required min={cycle.startDate} max={cycle.endDate} value={f.purchaseDate} onChange={set('purchaseDate')} />
        </Field>
        <Field label="Source">
          <select value={f.source} onChange={set('source')}>{SOURCES.map((s) => <option key={s}>{s}</option>)}</select>
        </Field>
      </div>
      <Field label="Supplier"><input maxLength={150} value={f.supplier} onChange={set('supplier')} /></Field>
      <div className="grid-2">
        <Field label="Volume (kL)"><input type="number" required min="0.01" step="0.01" value={f.volumeKl} onChange={set('volumeKl')} /></Field>
        <Field label="Unit cost (per kL)"><input type="number" required min="0" step="0.01" value={f.unitCost} onChange={set('unitCost')} /></Field>
      </div>
      <p className="muted">Total cost: <strong>{Number.isFinite(total) ? money(total) : '-'}</strong></p>
      <Field label="Notes"><input maxLength={255} value={f.notes} onChange={set('notes')} /></Field>
    </FormModal>
  );
}

function PurchasesTab({ cycle }) {
  const list = useAsync(() => get(`/api/v1/billing-cycles/${cycle.id}/purchases`), [cycle.id]);
  const summary = useAsync(() => get(`/api/v1/billing-cycles/${cycle.id}/purchases/summary`), [cycle.id]);
  const [modal, setModal] = useState(null);
  const open = cycle.status === 'OPEN';
  const reload = () => { list.reload(); summary.reload(); };
  const act = useAction(reload);
  const s = summary.data;

  return (
    <>
      <ErrorBox error={list.error || act.error} />
      <div className="stats">
        <Stat label="Purchases" value={s ? s.purchaseCount : '-'} />
        <Stat label="Total volume" value={s ? `${number(s.totalVolumeKl)} kL` : '-'} />
        <Stat label="Total cost" value={s ? money(s.totalCost) : '-'} />
        <Stat label="Average cost per kL" value={s ? money(s.averageUnitCost) : '-'} hint="Weighted by volume" />
      </div>
      <Card
        title="Bulk water purchases"
        actions={open && <button className="btn btn-primary btn-sm" onClick={() => setModal({})}>Add purchase</button>}
      >
        {list.loading ? <Spinner /> : !list.data || list.data.length === 0 ? (
          <Empty>No purchases recorded for this cycle.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Date</th><th>Source</th><th>Supplier</th><th className="num">Volume (kL)</th><th className="num">Unit cost</th><th className="num">Total</th><th /></tr></thead>
              <tbody>
                {list.data.map((p) => (
                  <tr key={p.id}>
                    <td>{dateStr(p.purchaseDate)}</td>
                    <td><Badge tone="blue">{p.source}</Badge></td>
                    <td>{p.supplier || '-'}</td>
                    <td className="num">{number(p.volumeKl)}</td>
                    <td className="num">{money(p.unitCost)}</td>
                    <td className="num"><strong>{money(p.totalCost)}</strong></td>
                    <td className="right">
                      {open && (
                        <div className="row gap end">
                          <button className="btn btn-ghost btn-sm" onClick={() => setModal({ item: p })}>Edit</button>
                          <button className="btn btn-danger btn-sm"
                            onClick={() => act.run(() => del(`/api/v1/purchases/${p.id}`), { confirm: 'Delete this purchase?' })}>Delete</button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {!open && <p className="muted small">Purchases are locked because this cycle is {cycle.status.toLowerCase()}.</p>}
      </Card>
      {modal && <PurchaseForm cycle={cycle} initial={modal.item} onClose={(saved) => { setModal(null); if (saved) reload(); }} />}
    </>
  );
}

/* ---------- Adjustments ---------- */

function AdjustmentForm({ cycle, households, onClose }) {
  const [f, setF] = useState({ householdId: households[0] ? String(households[0].id) : '', amount: '', reason: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  return (
    <FormModal
      title="Add adjustment"
      onClose={onClose}
      onSubmit={() => post(`/api/v1/billing-cycles/${cycle.id}/adjustments`, {
        householdId: Number(f.householdId), amount: Number(f.amount), reason: f.reason.trim(),
      })}
    >
      <Field label="Household">
        <select required value={f.householdId} onChange={set('householdId')}>
          {households.map((h) => <option key={h.id} value={h.id}>{h.flatNumber}</option>)}
        </select>
      </Field>
      <Field label="Amount" hint="Positive adds a charge (late fee). Negative gives a credit (refund). Cannot be zero.">
        <input type="number" required step="0.01" value={f.amount} onChange={set('amount')} />
      </Field>
      <Field label="Reason"><input required maxLength={255} value={f.reason} onChange={set('reason')} /></Field>
    </FormModal>
  );
}

function AdjustmentsTab({ cycle }) {
  const list = useAsync(() => get(`/api/v1/billing-cycles/${cycle.id}/adjustments`), [cycle.id]);
  const households = useAsync(() => get(`/api/v1/apartments/${cycle.apartmentId}/households`), [cycle.apartmentId]);
  const [adding, setAdding] = useState(false);
  const act = useAction(list.reload);
  const open = cycle.status === 'OPEN';

  return (
    <>
      <ErrorBox error={list.error || act.error} />
      <Card
        title="Adjustments"
        actions={open && <button className="btn btn-primary btn-sm" disabled={!households.data?.length} onClick={() => setAdding(true)}>Add adjustment</button>}
      >
        <p className="muted small">Manual charges or credits that appear as a separate line on the household's invoice.</p>
        {list.loading ? <Spinner /> : !list.data || list.data.length === 0 ? (
          <Empty>No adjustments.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Flat</th><th>Reason</th><th className="num">Amount</th><th>Added by</th><th /></tr></thead>
              <tbody>
                {list.data.map((a) => (
                  <tr key={a.id}>
                    <td><strong>{a.flatNumber}</strong></td>
                    <td>{a.reason}</td>
                    <td className={`num ${Number(a.amount) < 0 ? 'neg' : ''}`}>{money(a.amount)}</td>
                    <td className="muted small">{a.createdBy || '-'}</td>
                    <td className="right">
                      {open && (
                        <button className="btn btn-danger btn-sm"
                          onClick={() => act.run(() => del(`/api/v1/adjustments/${a.id}`), { confirm: 'Delete this adjustment?' })}>Delete</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
      {adding && households.data && (
        <AdjustmentForm cycle={cycle} households={households.data}
          onClose={(saved) => { setAdding(false); if (saved) list.reload(); }} />
      )}
    </>
  );
}

/* ---------- Distribution ---------- */

const BASIS = {
  CONSUMPTION: 'By consumption',
  AREA_ESTIMATE: 'Estimated from flat area (no meter)',
  AREA: 'By flat area',
  EQUAL: 'Equal split',
};

function DistributionTab({ cycle }) {
  const { data, loading, error } = useAsync(() => get(`/api/v1/billing-cycles/${cycle.id}/distribution`), [cycle.id, cycle.status]);
  if (loading) return <Spinner />;
  if (error) return <ErrorBox error={error} />;

  const chart = data.households.map((h) => ({
    flat: h.flatNumber, Water: Number(h.waterCharge), Shared: Number(h.sharedAllocation),
  }));

  return (
    <>
      <p className="muted small">
        Preview of how the purchase cost is shared. Water charge comes from the tariff slabs. The remaining cost
        (common areas and losses) is split by consumption, or by flat area for flats without a meter.
      </p>
      <Alert type="info">{data.note}</Alert>
      <div className="stats">
        <Stat label="Purchase cost" value={money(data.totalPurchaseCost)} />
        <Stat label="Water charges (tariff)" value={money(data.totalWaterCharges)} />
        <Stat label="Shared cost" value={money(data.sharedCost)} />
        <Stat label="Total billed" value={money(data.totalBilled)} hint={Number(data.surplus) > 0 ? `Surplus ${money(data.surplus)}` : ''} />
      </div>

      <Card title="Charge per household">
        {data.households.length === 0 ? <Empty>No households.</Empty> : (
          <>
            <div className="chart">
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={chart}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="flat" fontSize={12} />
                  <YAxis fontSize={12} />
                  <Tooltip formatter={(v) => money(v)} />
                  <Legend />
                  <Bar dataKey="Water" stackId="a" fill="#0f8f8f" isAnimationActive={false} />
                  <Bar dataKey="Shared" stackId="a" fill="#f29e4c" radius={[4, 4, 0, 0]} isAnimationActive={false} />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Flat</th><th className="num">Used</th><th className="num">Water charge</th><th>Shared basis</th><th className="num">Shared</th><th className="num">Total</th></tr></thead>
                <tbody>
                  {data.households.map((h) => (
                    <tr key={h.householdId}>
                      <td><strong>{h.flatNumber}</strong> {!h.hasMeter && <Badge>no meter</Badge>}</td>
                      <td className="num">{litres(h.consumptionLitres)}</td>
                      <td className="num">{money(h.waterCharge)}</td>
                      <td className="muted small">{BASIS[h.allocationBasis] || h.allocationBasis}</td>
                      <td className="num">{money(h.sharedAllocation)}</td>
                      <td className="num"><strong>{money(h.totalCharge)}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </Card>
    </>
  );
}

/* ---------- Invoices ---------- */

function InvoicesTab({ cycle }) {
  const open = cycle.status === 'OPEN';
  const { data, loading, error } = useAsync(
    () => (open ? Promise.resolve([]) : get(`/api/v1/billing-cycles/${cycle.id}/invoices`)),
    [cycle.id, cycle.status]
  );
  if (open) return <Alert type="info">Invoices are created when you finalize this cycle.</Alert>;
  if (loading) return <Spinner />;
  const total = (data || []).reduce((s, i) => s + Number(i.totalAmount), 0);

  return (
    <>
      <ErrorBox error={error} />
      <Card title={`Invoices (${(data || []).length})`} actions={<strong>{money(total)}</strong>}>
        {!data || data.length === 0 ? <Empty>No invoices.</Empty> : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Invoice</th><th>Flat</th><th className="num">Water used</th><th className="num">Total</th><th /></tr></thead>
              <tbody>
                {data.map((i) => (
                  <tr key={i.id}>
                    <td>{i.invoiceNumber}</td>
                    <td><strong>{i.flatNumber}</strong></td>
                    <td className="num">{litres(i.consumptionLitres)}</td>
                    <td className="num"><strong>{money(i.totalAmount)}</strong></td>
                    <td className="right"><Link className="btn btn-ghost btn-sm" to={`/invoices/${i.id}`}>View</Link></td>
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

/* ---------- Page ---------- */

export default function CycleDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  useAdmin();
  const cycle = useAsync(() => get(`/api/v1/billing-cycles/${id}`), [id]);
  const [tab, setTab] = useState('overview');
  const act = useAction(cycle.reload);

  if (cycle.loading && !cycle.data) return <Spinner />;
  if (cycle.error) return <ErrorBox error={cycle.error} />;
  const c = cycle.data;

  const finalize = () => act.run(async () => {
    const r = await post(`/api/v1/billing-cycles/${c.id}/finalize`);
    setTab('invoices');
    return r;
  }, {
    confirm: 'Finalize this cycle? Invoices will be generated for every household and the cycle becomes read-only.',
    success: 'Cycle finalized and invoices generated.',
  });

  const archive = () => act.run(() => post(`/api/v1/billing-cycles/${c.id}/archive`), {
    confirm: 'Archive this cycle?', success: 'Cycle archived.',
  });

  const remove = () => act.run(async () => {
    await del(`/api/v1/billing-cycles/${c.id}`);
    navigate('/admin/cycles', { replace: true });
  }, { confirm: 'Delete this cycle and its purchases and adjustments?' });

  return (
    <>
      <PageHeader
        title={`${dateStr(c.startDate)} to ${dateStr(c.endDate)}`}
        subtitle={`Billing cycle #${c.id} | ${c.tariffPlanName}`}
        actions={(
          <>
            <StatusBadge status={c.status} />
            <Link className="btn btn-ghost" to="/admin/cycles">Back</Link>
            {c.status === 'OPEN' && <button className="btn btn-danger" onClick={remove}>Delete</button>}
            {c.status === 'OPEN' && <button className="btn btn-primary" onClick={finalize}>Finalize</button>}
            {c.status === 'FINALIZED' && <button className="btn btn-primary" onClick={archive}>Archive</button>}
          </>
        )}
      />
      <ErrorBox error={act.error} />
      <Alert type="success">{act.message}</Alert>

      <Tabs
        active={tab}
        onChange={setTab}
        tabs={[
          { key: 'overview', label: 'Overview' },
          { key: 'purchases', label: 'Bulk purchases' },
          { key: 'adjustments', label: 'Adjustments' },
          { key: 'distribution', label: 'Cost distribution' },
          { key: 'invoices', label: 'Invoices' },
        ]}
      />

      {tab === 'overview' && <OverviewTab cycle={c} reload={cycle.reload} />}
      {tab === 'purchases' && <PurchasesTab cycle={c} />}
      {tab === 'adjustments' && <AdjustmentsTab cycle={c} />}
      {tab === 'distribution' && <DistributionTab cycle={c} />}
      {tab === 'invoices' && <InvoicesTab cycle={c} />}
    </>
  );
}
