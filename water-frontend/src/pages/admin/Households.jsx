import { useState } from 'react';
import { del, get, post, put } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { number } from '../../fmt.js';
import { Badge, Card, Empty, ErrorBox, Field, FormModal, PageHeader, Spinner, useAction, useAsync } from '../../ui.jsx';

function HouseholdForm({ apartmentId, initial, onClose }) {
  const [f, setF] = useState(initial
    ? { flatNumber: initial.flatNumber, areaSqft: initial.areaSqft ?? '', occupancy: initial.occupancy ?? '', meterNumber: initial.meterNumber ?? '' }
    : { flatNumber: '', areaSqft: '', occupancy: '', meterNumber: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const body = () => ({
    flatNumber: f.flatNumber.trim(),
    areaSqft: f.areaSqft === '' ? null : Number(f.areaSqft),
    occupancy: f.occupancy === '' ? null : Number(f.occupancy),
    meterNumber: f.meterNumber.trim(),
  });

  return (
    <FormModal
      title={initial ? `Edit flat ${initial.flatNumber}` : 'Add household'}
      onClose={onClose}
      onSubmit={() => (initial
        ? put(`/api/v1/households/${initial.id}`, body())
        : post(`/api/v1/apartments/${apartmentId}/households`, body()))}
    >
      <Field label="Flat number"><input required maxLength={20} value={f.flatNumber} onChange={set('flatNumber')} /></Field>
      <div className="grid-2">
        <Field label="Area (sq ft)"><input type="number" min="1" step="0.01" value={f.areaSqft} onChange={set('areaSqft')} /></Field>
        <Field label="Occupancy"><input type="number" min="1" step="1" value={f.occupancy} onChange={set('occupancy')} /></Field>
      </div>
      <Field label="Water meter number" hint="Leave empty if the flat has no meter. Its share is then estimated from flat area.">
        <input maxLength={50} value={f.meterNumber} onChange={set('meterNumber')} />
      </Field>
    </FormModal>
  );
}

export default function Households() {
  const { apartmentId, apartment } = useAdmin();
  const list = useAsync(() => get(`/api/v1/apartments/${apartmentId}/households`), [apartmentId]);
  const [modal, setModal] = useState(null);
  const [query, setQuery] = useState('');
  const [meterFilter, setMeterFilter] = useState('all');
  const act = useAction(list.reload);
  const households = list.data || [];
  const meteredCount = households.filter((household) => household.meterNumber).length;
  const filteredHouseholds = households.filter((household) => {
    const matchesQuery = [household.flatNumber, household.meterNumber, String(household.id)]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(query.trim().toLowerCase()));
    const matchesMeter = meterFilter === 'all'
      || (meterFilter === 'metered' ? Boolean(household.meterNumber) : !household.meterNumber);
    return matchesQuery && matchesMeter;
  });

  return (
    <div className="management-page">
      <PageHeader
        title="Households"
        subtitle={apartment ? `Manage flats and meter details for ${apartment.name}.` : 'Manage apartment flats and meter details.'}
        actions={<button className="btn btn-primary" onClick={() => setModal({})}><span aria-hidden="true">＋</span> Add household</button>}
      />
      <ErrorBox error={list.error || act.error} />
      {act.dialog}
      <div className="management-summary-row">
        <div className="management-summary-card">
          <span className="management-summary-icon">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
          </span>
          <span><small>TOTAL HOUSEHOLDS</small><strong>{list.loading ? '—' : households.length}</strong></span>
        </div>
        <div className="management-summary-card">
          <span className="management-summary-icon management-summary-icon-blue">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
          </span>
          <span><small>METERED</small><strong>{list.loading ? '—' : meteredCount}</strong></span>
        </div>
        <div className="management-summary-card">
          <span className="management-summary-icon management-summary-icon-gold">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
            </svg>
          </span>
          <span><small>WITHOUT METER</small><strong>{list.loading ? '—' : households.length - meteredCount}</strong></span>
        </div>
      </div>
      <Card title="Household directory" actions={<span className="management-result-count">{filteredHouseholds.length} {filteredHouseholds.length === 1 ? 'result' : 'results'}</span>}>
        <div className="management-toolbar">
          <label className="management-search">
            <span aria-hidden="true">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </span>
            <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search flat or meter number" aria-label="Search households" />
          </label>
          <label className="management-filter">
            <span>Status</span>
            <select value={meterFilter} onChange={(event) => setMeterFilter(event.target.value)} aria-label="Filter by meter status">
              <option value="all">All households</option>
              <option value="metered">Metered</option>
              <option value="unmetered">No meter</option>
            </select>
          </label>
        </div>
        {list.loading ? <Spinner /> : households.length === 0 ? (
          <Empty>No households yet. Add the first flat.</Empty>
        ) : filteredHouseholds.length === 0 ? <Empty>No households match these filters.</Empty> : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>ID</th><th>Flat</th><th className="num">Area (sq ft)</th><th className="num">Occupancy</th><th>Meter</th><th /></tr></thead>
              <tbody>
                {filteredHouseholds.map((h) => (
                  <tr key={h.id}>
                    <td>#{h.id}</td>
                    <td><strong>{h.flatNumber}</strong></td>
                    <td className="num">{h.areaSqft === null ? '-' : number(h.areaSqft)}</td>
                    <td className="num">{h.occupancy ?? '-'}</td>
                    <td>{h.meterNumber ? <Badge tone="blue">{h.meterNumber}</Badge> : <Badge>no meter</Badge>}</td>
                    <td className="right">
                      <div className="row gap end">
                        <button className="btn btn-ghost btn-sm" onClick={() => setModal({ item: h })}>Edit</button>
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => act.run(() => del(`/api/v1/households/${h.id}`), {
                            confirm: `Delete flat ${h.flatNumber}?`,
                          })}
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
        <HouseholdForm apartmentId={apartmentId} initial={modal.item}
          onClose={(saved) => { setModal(null); if (saved) list.reload(); }} />
      )}
    </div>
  );
}
