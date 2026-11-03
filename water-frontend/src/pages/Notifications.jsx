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
    <div className="notifications-page">
      <PageHeader
        title="Alerts & Notifications"
        subtitle="Stay informed about anomaly detections, high water usage, and community activity."
        actions={(
          <>
            <label className="check-row">
              <input type="checkbox" checked={unreadOnly} onChange={(e) => setUnreadOnly(e.target.checked)} />
              <span>Unread only</span>
            </label>
            <button
              className="btn btn-ghost"
              disabled={!items.some((n) => !n.read)}
              onClick={() => act.run(() => put('/api/v1/notifications/read-all'))}
            >
              Mark all as read
            </button>
          </>
        )}
      />
      <ErrorBox error={list.error || act.error} />
      {!list.loading && !list.error && (
        <div className="notification-summary">
          <div>
            <span className="notification-summary-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M18 10a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
            </span>
            <span>
              <small>{unreadOnly ? 'UNREAD ALERTS' : 'ALERTS SHOWN'}</small>
              <strong>{items.length}</strong>
            </span>
          </div>
          <div>
            <span className="notification-summary-icon notification-summary-icon-warn">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                <line x1="12" y1="9" x2="12" y2="13" />
                <line x1="12" y1="17" x2="12.01" y2="17" />
              </svg>
            </span>
            <span>
              <small>PENDING ATTENTION</small>
              <strong>{unread}</strong>
            </span>
          </div>
          <div className="notification-summary-note">
            {unreadOnly
              ? 'Showing unread notifications only. Mark items as read once investigated.'
              : 'Alerts indicate potential leaks or unusual consumption spikes beyond thresholds.'}
          </div>
        </div>
      )}
      <Card title={unreadOnly ? 'Unread alerts' : `All alerts${unread ? ` (${unread} unread)` : ''}`}>
        {list.loading ? <Spinner /> : items.length === 0 ? (
          <Empty>No alerts found. You will see a notification here when unusual water usage or leak conditions are detected.</Empty>
        ) : (
          items.map((n) => {
            const t = TYPE[n.type] || { label: n.type, tone: 'grey' };
            return (
              <div key={n.id} className={`notif ${n.read ? '' : 'notif-unread'}`}>
                <div>
                  <div className="row gap" style={{ marginBottom: 6 }}>
                    <Badge tone={t.tone}>{t.label}</Badge>
                    <Badge tone={n.read ? 'grey' : 'green'}>{n.read ? 'Read' : 'Unread'}</Badge>
                    {n.flatNumber && <span className="muted small">Flat {n.flatNumber}</span>}
                    <span className="muted small">{dateTimeStr(n.createdAt)}</span>
                  </div>
                  <div className="notif-msg">{n.message}</div>
                </div>
                {!n.read && (
                  <button
                    className="btn btn-ghost btn-sm"
                    onClick={() => act.run(() => put(`/api/v1/notifications/${n.id}/read`))}
                  >
                    Mark read
                  </button>
                )}
              </div>
            );
          })
        )}
      </Card>
    </div>
  );
}
