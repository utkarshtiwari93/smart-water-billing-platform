import { useState } from 'react';
import { del, get, put } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { Alert, Badge, Card, Empty, ErrorBox, PageHeader, Spinner, useAction, useAsync } from '../../ui.jsx';

function AssignCell({ user, households, act }) {
  const [pick, setPick] = useState(user.householdId || '');
  return (
    <div className="row gap end resident-assignment">
      <select value={pick} onChange={(e) => setPick(e.target.value)} aria-label={`Household for ${user.name}`}>
        <option value="">Select flat...</option>
        {households.map((h) => <option key={h.id} value={h.id}>{h.flatNumber}</option>)}
      </select>
      <button
        className="btn btn-primary btn-sm"
        disabled={!pick || Number(pick) === user.householdId}
        onClick={() => act.run(() => put(`/api/v1/users/${user.id}/household`, { householdId: Number(pick) }), { success: `${user.name} linked` })}
      >Assign</button>
      {user.householdId && (
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => act.run(() => del(`/api/v1/users/${user.id}/household`), { confirm: `Remove ${user.name} from the flat?` })}
        >Unlink</button>
      )}
    </div>
  );
}

export default function Residents() {
  const { apartmentId, apartment } = useAdmin();
  const [query, setQuery] = useState('');
  const [linkFilter, setLinkFilter] = useState('all');
  const list = useAsync(async () => {
    const [users, households] = await Promise.all([
      get(`/api/v1/users?apartmentId=${apartmentId}`),
      get(`/api/v1/apartments/${apartmentId}/households`),
    ]);
    return { users, households };
  }, [apartmentId]);
  const act = useAction(list.reload);

  const flat = (id) => list.data?.households.find((h) => h.id === id)?.flatNumber;
  const users = list.data?.users || [];
  const households = list.data?.households || [];
  const residents = users.filter((item) => item.role === 'RESIDENT');
  const linkedResidents = residents.filter((item) => item.householdId).length;
  const filteredUsers = users.filter((item) => {
    const matchesQuery = [item.name, item.email, item.role, item.householdId ? flat(item.householdId) : '']
      .filter(Boolean)
      .some((value) => value.toLowerCase().includes(query.trim().toLowerCase()));
    const matchesLink = item.role !== 'RESIDENT' || linkFilter === 'all'
      || (linkFilter === 'linked' ? Boolean(item.householdId) : !item.householdId);
    return matchesQuery && matchesLink;
  });

  return (
    <div className="management-page">
      <PageHeader
        title="Residents"
        subtitle={apartment ? `Manage resident accounts and household assignments in ${apartment.name}.` : 'Manage resident accounts and household assignments.'}
      />
      <Alert type="info">
        Residents register themselves with the Apartment ID. Link each one to their flat here so they can see their usage and bills.
      </Alert>
      <ErrorBox error={list.error || act.error} />
      {act.message && <Alert type="success">{act.message}</Alert>}
      {act.dialog}
      <div className="management-summary-row">
        <div className="management-summary-card">
          <span className="management-summary-icon management-summary-icon-blue">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </span>
          <span><small>TOTAL RESIDENTS</small><strong>{list.loading ? '—' : residents.length}</strong></span>
        </div>
        <div className="management-summary-card">
          <span className="management-summary-icon">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </span>
          <span><small>LINKED TO A HOUSEHOLD</small><strong>{list.loading ? '—' : linkedResidents}</strong></span>
        </div>
        <div className="management-summary-card">
          <span className="management-summary-icon management-summary-icon-gold">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          </span>
          <span><small>NEEDS ASSIGNMENT</small><strong>{list.loading ? '—' : residents.length - linkedResidents}</strong></span>
        </div>
      </div>
      <Card title="Resident directory" actions={<span className="management-result-count">{filteredUsers.length} {filteredUsers.length === 1 ? 'result' : 'results'}</span>}>
        <div className="management-toolbar">
          <label className="management-search">
            <span aria-hidden="true">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </span>
            <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search name, email or flat" aria-label="Search residents" />
          </label>
          <label className="management-filter">
            <span>Household link</span>
            <select value={linkFilter} onChange={(event) => setLinkFilter(event.target.value)} aria-label="Filter residents by household assignment">
              <option value="all">All residents</option>
              <option value="linked">Linked</option>
              <option value="unlinked">Needs assignment</option>
            </select>
          </label>
        </div>
        {list.loading ? <Spinner /> : users.length === 0 ? (
          <Empty>No users in this apartment yet.</Empty>
        ) : filteredUsers.length === 0 ? <Empty>No residents match these filters.</Empty> : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Flat</th><th className="right">Link to flat</th></tr></thead>
              <tbody>
                {filteredUsers.map((item) => (
                  <tr key={item.id}>
                    <td><strong>{item.name}</strong></td>
                    <td>{item.email}</td>
                    <td><Badge tone={item.role === 'ADMIN' ? 'blue' : 'grey'}>{item.role}</Badge></td>
                    <td>{item.householdId ? flat(item.householdId) || `#${item.householdId}` : <span className="muted">-</span>}</td>
                    <td className="right">
                      {item.role === 'RESIDENT'
                        ? <AssignCell key={`${item.id}-${item.householdId}`} user={item} households={households} act={act} />
                        : <span className="muted small">Admins are not linked to a flat</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
