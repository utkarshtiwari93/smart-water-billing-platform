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
  const act = useAction(list.reload);

  return (
    <>
      <PageHeader
        title="Households"
        subtitle={apartment ? `Flats in ${apartment.name}` : ''}
        actions={<button className="btn btn-primary" onClick={() => setModal({})}>Add household</button>}
      />
      <ErrorBox error={list.error || act.error} />
      <Card>
        {list.loading ? <Spinner /> : !list.data || list.data.length === 0 ? (
          <Empty>No households yet. Add the first flat.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>ID</th><th>Flat</th><th className="num">Area (sq ft)</th><th className="num">Occupancy</th><th>Meter</th><th /></tr></thead>
              <tbody>
                {list.data.map((h) => (
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
    </>
  );
}
