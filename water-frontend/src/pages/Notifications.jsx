import { useState } from 'react';
import { get, put } from '../api.js';
import { dateTimeStr } from '../fmt.js';
import { Badge, Card, Empty, ErrorBox, PageHeader, Spinner, useAction, useAsync } from '../ui.jsx';

const TYPE = {
  LEAK_SUSPECTED: { label: 'Possible leak', tone: 'red' },
  OVERUSE: { label: 'High usage', tone: 'amber' },
};

export default function Notifications() {
  const [unreadOnly, setUnreadOnly] = useState(false);
  const list = useAsync(() => get(`/api/v1/notifications?unreadOnly=${unreadOnly}`), [unreadOnly]);
  const act = useAction(() => {
    list.reload();
    window.dispatchEvent(new Event('notifications-changed'));
  });
  const items = list.data || [];
  const unread = items.filter((n) => !n.read).length;

  return (
    <>
      <PageHeader
        title="Alerts"
        subtitle="Leak warnings and high-usage alerts for your water usage"
        actions={(
          <>
            <label className="check-row">
              <input type="checkbox" checked={unreadOnly} onChange={(e) => setUnreadOnly(e.target.checked)} />
              <span>Unread only</span>
            </label>
            <button className="btn btn-ghost" disabled={!items.some((n) => !n.read)}
              onClick={() => act.run(() => put('/api/v1/notifications/read-all'))}>
              Mark all as read
            </button>
          </>
        )}
      />
      <ErrorBox error={list.error || act.error} />
      <Card title={unreadOnly ? 'Unread alerts' : `All alerts${unread ? ` (${unread} unread)` : ''}`}>
        {list.loading ? <Spinner /> : items.length === 0 ? (
          <Empty>No alerts. You will see a message here if the system notices unusual water usage.</Empty>
        ) : (
          items.map((n) => {
            const t = TYPE[n.type] || { label: n.type, tone: 'grey' };
            return (
              <div key={n.id} className={`notif ${n.read ? '' : 'notif-unread'}`}>
                <div>
                  <div className="row gap">
                    <Badge tone={t.tone}>{t.label}</Badge>
                    {n.flatNumber && <span className="muted small">Flat {n.flatNumber}</span>}
                    <span className="muted small">{dateTimeStr(n.createdAt)}</span>
                  </div>
                  <div className="notif-msg">{n.message}</div>
                </div>
                {!n.read && (
                  <button className="btn btn-ghost btn-sm"
                    onClick={() => act.run(() => put(`/api/v1/notifications/${n.id}/read`))}>
                    Mark read
                  </button>
                )}
              </div>
            );
          })
        )}
      </Card>
    </>
  );
}
