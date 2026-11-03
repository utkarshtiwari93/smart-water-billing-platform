import { useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, get, post } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { dateStr, daysAgo, litres, number, today } from '../../fmt.js';
import { Alert, Badge, Card, Empty, ErrorBox, Field, FormModal, PageHeader, Spinner, Stat, useAsync } from '../../ui.jsx';

function ReadingForm({ householdId, lastReading, onClose }) {
  const [f, setF] = useState({ readingDate: today(), meterReading: '' });
  return (
    <FormModal
      title="Add meter reading"
      onClose={onClose}
      onSubmit={() => post('/api/v1/usage', {
        householdId: Number(householdId),
        readingDate: f.readingDate,
        meterReading: Number(f.meterReading),
      })}
    >
      <Field label="Reading date" hint="Must be after the household's latest reading, and not in the future.">
        <input type="date" required max={today()} value={f.readingDate} onChange={(e) => setF({ ...f, readingDate: e.target.value })} />
      </Field>
      <Field
        label="Meter reading (litres)"
        hint={lastReading ? `Last reading: ${number(lastReading.meterReading)} on ${dateStr(lastReading.readingDate)}. Meter values are monotonically increasing.` : 'The first reading serves as the initial baseline.'}
      >
        <input type="number" required min="0" step="0.01" value={f.meterReading} onChange={(e) => setF({ ...f, meterReading: e.target.value })} />
      </Field>
    </FormModal>
  );
}

function CsvUpload({ households, onDone }) {
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const upload = async (e) => {
    e.preventDefault();
    if (!file) return;
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      const form = new FormData();
      form.append('file', file);
      setResult(await api('/api/v1/usage/upload', { method: 'POST', form }));
      onDone();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  const sample = () => {
    const meter = households.find((h) => h.meterNumber)?.meterNumber || 'METER-NUMBER';
    const csv = `meterNumber,readingDate,meterReading\n${meter},${today()},1000\n`;
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'usage-sample.csv';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <Card title="Bulk import readings (CSV)" actions={<button className="btn btn-ghost btn-sm" onClick={sample}>Download CSV template</button>}>
      <p className="muted small" style={{ marginBottom: 14 }}>
        Required columns: <code>meterNumber,readingDate,meterReading</code> (format date as YYYY-MM-DD). Valid rows are recorded immediately.
      </p>
      <form className="row gap" onSubmit={upload}>
        <input type="file" accept=".csv,text/csv" onChange={(e) => setFile(e.target.files[0] || null)} style={{ maxWidth: 360 }} />
        <button className="btn btn-primary" disabled={!file || busy}>{busy ? 'Uploading...' : 'Upload & Process'}</button>
      </form>
      <ErrorBox error={error} />
      {result && (
        <div className="stack">
          <div className="stats stats-3">
            <Stat label="Total rows read" value={result.totalRows} />
            <Stat label="Successfully imported" value={result.imported} tone="good" />
            <Stat label="Failed rows" value={result.failed} tone={result.failed ? 'bad' : undefined} />
          </div>
          {result.errors.length > 0 && (
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Row #</th><th>Validation error</th></tr></thead>
                <tbody>{result.errors.map((er, i) => <tr key={i}><td>{er.rowNumber}</td><td>{er.message}</td></tr>)}</tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </Card>
  );
}

export default function Usage() {
  const { apartmentId } = useAdmin();
  const households = useAsync(() => get(`/api/v1/apartments/${apartmentId}/households`), [apartmentId]);
  const [hid, setHid] = useState('');
  const [from, setFrom] = useState(daysAgo(30));
  const [to, setTo] = useState(today());
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    const list = households.data;
    if (list && list.length && !list.some((h) => String(h.id) === String(hid))) setHid(String(list[0].id));
    if (list && list.length === 0) setHid('');
  }, [households.data, hid]);

  const usage = useAsync(
    () => (hid ? get(`/api/v1/usage?householdId=${hid}&from=${from}&to=${to}`) : Promise.resolve([])),
    [hid, from, to]
  );

  if (households.loading) return <Spinner />;
  const list = households.data || [];
  const rows = usage.data || [];
  const total = rows.reduce((s, r) => s + Number(r.consumptionLitres), 0);
  const chart = rows.map((r) => ({ date: dateStr(r.readingDate), litres: Number(r.consumptionLitres) }));
  const current = list.find((h) => String(h.id) === String(hid));

  return (
    <div className="management-page usage-page">
      <PageHeader
        title="Water usage"
        subtitle="Review household meter logs, track volume over time, and import reading batches."
        actions={<button className="btn btn-primary" disabled={!hid} onClick={() => setAdding(true)}><span aria-hidden="true">＋</span> Add reading</button>}
      />
      <ErrorBox error={households.error || usage.error} />

      {list.length === 0 ? (
        <Alert type="info">Add households first to record water meter readings.</Alert>
      ) : (
        <>
          <Card>
            <div className="filters">
              <Field label="Household">
                <select value={hid} onChange={(e) => setHid(e.target.value)} aria-label="Select household">
                  {list.map((h) => <option key={h.id} value={h.id}>{h.flatNumber}{h.meterNumber ? ` (${h.meterNumber})` : ' (no meter)'}</option>)}
                </select>
              </Field>
              <Field label="From date"><input type="date" value={from} onChange={(e) => setFrom(e.target.value)} /></Field>
              <Field label="To date"><input type="date" value={to} onChange={(e) => setTo(e.target.value)} /></Field>
            </div>
            {current && !current.meterNumber && (
              <Alert type="info">This flat has no meter number. Its share of shared cost is estimated from flat area.</Alert>
            )}
          </Card>

          <div className="stats stats-3">
            <Stat
              label="Total volume in range"
              value={litres(total)}
              hint={current ? `Flat ${current.flatNumber}` : ''}
              icon={(
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: 'var(--brand)' }}>
                  <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />
                </svg>
              )}
            />
            <Stat
              label="Meter readings recorded"
              value={rows.length}
              hint={`${dateStr(from)} – ${dateStr(to)}`}
              icon={(
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: 'var(--info)' }}>
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
              )}
            />
            <Stat
              label="Average consumption"
              value={rows.length ? litres(total / rows.length) : '-'}
              hint="Per meter reading interval"
              icon={(
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" style={{ color: 'var(--warning)' }}>
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                </svg>
              )}
            />
          </div>

          <Card title="Consumption profile">
            {usage.loading ? <Spinner /> : rows.length === 0 ? <Empty>No readings in this date range.</Empty> : (
              <>
                <div className="chart">
                  <ResponsiveContainer width="100%" height={280}>
                    <BarChart data={chart} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                      <CartesianGrid stroke="#f1f5f9" strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="date" fontSize={11} stroke="#94a3b8" tickLine={false} axisLine={{ stroke: '#e2e8f0' }} />
                      <YAxis fontSize={11} stroke="#94a3b8" tickLine={false} axisLine={false} />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: '#ffffff',
                          border: '1px solid #e2e8f0',
                          borderRadius: '8px',
                          boxShadow: '0 4px 12px rgba(15, 23, 42, 0.08)',
                          fontSize: '12px',
                        }}
                        formatter={(v) => [`${number(v)} L`, 'Consumption']}
                      />
                      <Bar dataKey="litres" fill="#0d9488" radius={[5, 5, 0, 0]} maxBarSize={48} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>Reading date</th><th className="num">Meter reading</th><th className="num">Consumption</th><th>Source</th></tr></thead>
                    <tbody>
                      {[...rows].reverse().map((r) => (
                        <tr key={r.id}>
                          <td>{dateStr(r.readingDate)}</td>
                          <td className="num">{number(r.meterReading)}</td>
                          <td className="num"><strong>{litres(r.consumptionLitres)}</strong></td>
                          <td><Badge tone={r.source === 'CSV' ? 'blue' : 'grey'}>{r.source}</Badge></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </Card>

          <CsvUpload households={list} onDone={usage.reload} />
        </>
      )}

      {adding && (
        <ReadingForm
          householdId={hid}
          lastReading={rows[rows.length - 1]}
          onClose={(saved) => { setAdding(false); if (saved) usage.reload(); }}
        />
      )}
    </div>
  );
}
