import { useState } from 'react';
import { del, get, put } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { Alert, Badge, Card, Empty, ErrorBox, PageHeader, Spinner, useAction, useAsync } from '../../ui.jsx';

function AssignCell({ user, households, act, reload }) {
  const [pick, setPick] = useState(user.householdId || '');
  return (
    <div className="row gap end">
      <select value={pick} onChange={(e) => setPick(e.target.value)}>
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
  const list = useAsync(async () => {
    const [users, households] = await Promise.all([
      get(`/api/v1/users?apartmentId=${apartmentId}`),
      get(`/api/v1/apartments/${apartmentId}/households`),
    ]);
    return { users, households };
  }, [apartmentId]);
  const act = useAction(list.reload);

  const flat = (id) => list.data?.households.find((h) => h.id === id)?.flatNumber;

  return (
    <>
      <PageHeader title="Residents" subtitle={apartment ? `People registered in ${apartment.name}` : ''} />
      <Alert type="info">
        Residents register themselves with the Apartment ID. Link each one to their flat here so they can see their usage and bills.
      </Alert>
      <ErrorBox error={list.error || act.error} />
      <Alert type="success">{act.message}</Alert>
      <Card>
        {list.loading ? <Spinner /> : list.data.users.length === 0 ? (
          <Empty>No users in this apartment yet.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Flat</th><th className="right">Link to flat</th></tr></thead>
              <tbody>
                {list.data.users.map((u) => (
                  <tr key={u.id}>
                    <td><strong>{u.name}</strong></td>
                    <td>{u.email}</td>
                    <td><Badge tone={u.role === 'ADMIN' ? 'blue' : 'grey'}>{u.role}</Badge></td>
                    <td>{u.householdId ? flat(u.householdId) || `#${u.householdId}` : <span className="muted">-</span>}</td>
                    <td className="right">
                      {u.role === 'RESIDENT'
                        ? <AssignCell key={`${u.id}-${u.householdId}`} user={u} households={list.data.households} act={act} reload={list.reload} />
                        : <span className="muted small">Admins are not linked to a flat</span>}
                    </td>
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
