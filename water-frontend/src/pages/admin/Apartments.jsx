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
  const [query, setQuery] = useState('');
  const act = useAction(reloadApartments);
  const filteredApartments = apartments.filter((apartment) => (
    [apartment.name, apartment.address, apartment.city, String(apartment.id)]
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(query.trim().toLowerCase()))
  ));

  return (
    <div className="management-page">
      <PageHeader
        title="Apartments"
        subtitle="Manage apartment communities and choose which one to administer."
        actions={<button className="btn btn-primary" onClick={() => setModal({})}><span aria-hidden="true">＋</span> Add apartment</button>}
      />
      <ErrorBox error={act.error} />
      {act.dialog}
      <div className="management-summary-row">
        <div className="management-summary-card">
          <span className="management-summary-icon">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M3 21h18M5 21V5l7-2v18m0-14h7v14M8 7h1m-1 4h1m-1 4h1m7-3h1m-1 4h1" />
            </svg>
          </span>
          <span><small>REGISTERED APARTMENTS</small><strong>{apartments.length}</strong></span>
        </div>
        <div className="management-summary-card">
          <span className="management-summary-icon management-summary-icon-blue">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </span>
          <span><small>ACTIVE WORKSPACE</small><strong>{apartments.find((item) => item.id === apartmentId)?.name || 'Not selected'}</strong></span>
        </div>
      </div>
      <Card title="Apartment directory" actions={<span className="management-result-count">{filteredApartments.length} {filteredApartments.length === 1 ? 'result' : 'results'}</span>}>
        <div className="management-toolbar">
          <label className="management-search">
            <span aria-hidden="true">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </span>
            <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search name, address, city or ID" aria-label="Search apartments" />
          </label>
        </div>
        {apartments.length === 0 ? <Empty>No apartments yet. Add your first community to get started.</Empty> : filteredApartments.length === 0 ? <Empty>No apartments match your search.</Empty> : (
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
    </div>
  );
}
