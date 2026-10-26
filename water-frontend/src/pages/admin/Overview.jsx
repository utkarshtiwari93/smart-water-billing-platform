import { Link } from 'react-router-dom';
import { get } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { dateStr } from '../../fmt.js';
import { Card, ErrorBox, PageHeader, Spinner, Stat, StatusBadge, useAsync } from '../../ui.jsx';

export default function Overview() {
  const { apartment, apartmentId } = useAdmin();

  const { data, loading, error } = useAsync(async () => {
    const [households, users, cycles, tariffs] = await Promise.all([
      get(`/api/v1/apartments/${apartmentId}/households`),
      get(`/api/v1/users?apartmentId=${apartmentId}`),
      get(`/api/v1/apartments/${apartmentId}/billing-cycles`),
      get(`/api/v1/apartments/${apartmentId}/tariffs`),
    ]);
    return { households, users, cycles, tariffs };
  }, [apartmentId]);

  if (loading) return <Spinner />;
  if (error) return <ErrorBox error={error} />;

  const residents = data.users.filter((u) => u.role === 'RESIDENT');
  const linked = residents.filter((u) => u.householdId).length;
  const metered = data.households.filter((h) => h.meterNumber).length;
  const openCycles = data.cycles.filter((c) => c.status === 'OPEN');
  const latest = data.cycles[0];

  const steps = [
    { done: data.households.length > 0, label: 'Add households (flats) with meter numbers', to: '/admin/households' },
    { done: linked > 0, label: 'Link residents to their households', to: '/admin/residents' },
    { done: data.tariffs.length > 0, label: 'Create a tariff plan (rate slabs)', to: '/admin/tariffs' },
    { done: data.cycles.length > 0, label: 'Open a billing cycle', to: '/admin/cycles' },
  ];

  return (
    <>
      <PageHeader
        title={apartment ? apartment.name : 'Overview'}
        subtitle={apartment ? [apartment.address, apartment.city].filter(Boolean).join(', ') || `Apartment #${apartment.id}` : ''}
      />

      <div className="stats">
        <Stat label="Households" value={data.households.length} hint={`${metered} with meters`} />
        <Stat label="Residents" value={residents.length} hint={`${linked} linked to a flat`} />
        <Stat label="Tariff plans" value={data.tariffs.length} hint={`${data.tariffs.filter((t) => t.active).length} active`} />
        <Stat label="Open billing cycles" value={openCycles.length} hint={`${data.cycles.length} in total`} />
      </div>

      <div className="grid-2">
        <Card title="Setup checklist">
          <ul className="checklist">
            {steps.map((s) => (
              <li key={s.label} className={s.done ? 'done' : ''}>
                <span className="check">{s.done ? 'v' : ''}</span>
                <Link to={s.to}>{s.label}</Link>
              </li>
            ))}
          </ul>
        </Card>

        <Card title="Latest billing cycle" actions={<Link className="btn btn-ghost btn-sm" to="/admin/cycles">All cycles</Link>}>
          {!latest ? (
            <p className="muted">No billing cycle yet.</p>
          ) : (
            <div className="row between">
              <div>
                <div><strong>{dateStr(latest.startDate)} to {dateStr(latest.endDate)}</strong></div>
                <div className="muted small">Tariff: {latest.tariffPlanName}</div>
              </div>
              <div className="row gap">
                <StatusBadge status={latest.status} />
                <Link className="btn btn-primary btn-sm" to={`/admin/cycles/${latest.id}`}>Open</Link>
              </div>
            </div>
          )}
        </Card>
      </div>
    </>
  );
}
