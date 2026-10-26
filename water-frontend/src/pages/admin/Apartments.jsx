import { useState } from 'react';
import { del, post, put } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { Card, Empty, ErrorBox, Field, FormModal, PageHeader, useAction } from '../../ui.jsx';

function ApartmentForm({ initial, onClose }) {
  const [f, setF] = useState(initial || { name: '', address: '', city: '' });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const body = () => ({ name: f.name.trim(), address: f.address.trim(), city: f.city.trim() });
  return (
    <FormModal
      title={initial ? 'Edit apartment' : 'Add apartment'}
      onClose={onClose}
      onSubmit={() => (initial ? put(`/api/v1/apartments/${initial.id}`, body()) : post('/api/v1/apartments', body()))}
    >
      <Field label="Name"><input required maxLength={150} value={f.name} onChange={set('name')} /></Field>
      <Field label="Address"><input maxLength={255} value={f.address || ''} onChange={set('address')} /></Field>
      <Field label="City"><input maxLength={100} value={f.city || ''} onChange={set('city')} /></Field>
    </FormModal>
  );
}

export default function Apartments() {
  const { apartments, reloadApartments, apartmentId, setApartmentId } = useAdmin();
  const [modal, setModal] = useState(null);
  const act = useAction(reloadApartments);

  return (
    <>
      <PageHeader
        title="Apartments"
        subtitle="Societies managed in this system"
        actions={<button className="btn btn-primary" onClick={() => setModal({})}>Add apartment</button>}
      />
      <ErrorBox error={act.error} />
      <Card>
        {apartments.length === 0 ? <Empty>No apartments yet.</Empty> : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>ID</th><th>Name</th><th>Address</th><th>City</th><th /></tr></thead>
              <tbody>
                {apartments.map((a) => (
                  <tr key={a.id}>
                    <td>#{a.id}</td>
                    <td><strong>{a.name}</strong> {a.id === apartmentId && <span className="badge badge-green">selected</span>}</td>
                    <td>{a.address || '-'}</td>
                    <td>{a.city || '-'}</td>
                    <td className="right">
                      <div className="row gap end">
                        {a.id !== apartmentId && (
                          <button className="btn btn-ghost btn-sm" onClick={() => setApartmentId(a.id)}>Select</button>
                        )}
                        <button className="btn btn-ghost btn-sm" onClick={() => setModal({ item: a })}>Edit</button>
                        <button
                          className="btn btn-danger btn-sm"
                          onClick={() => act.run(() => del(`/api/v1/apartments/${a.id}`), {
                            confirm: `Delete "${a.name}" and all its households? This cannot be undone.`,
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
        <ApartmentForm initial={modal.item} onClose={(saved) => { setModal(null); if (saved) reloadApartments(); }} />
      )}
    </>
  );
}
