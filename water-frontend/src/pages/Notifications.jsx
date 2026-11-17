import { useState } from 'react';
import { get, put } from '../api.js';
import { dateTimeStr } from '../fmt.js';
import { Badge, Card, Empty, ErrorBox, PageHeader, Spinner, useAction, useAsync } from '../ui.jsx';

/**
 * Alerts & Notifications
 * ----------------------------------------------------------------------------
 * UI-only refresh. The data logic is unchanged:
 *  - Same imports and same default export (Notifications).
 *  - Same API calls: GET /api/v1/notifications?unreadOnly=..., PUT .../read-all
 *    and PUT .../{id}/read.
 *  - Same 'notifications-changed' event after every change, so the header
 *    notification count keeps updating.
 *  - Same shared components: PageHeader, Card, Badge, Empty, ErrorBox, Spinner.
 *
 * Added on the client only (no new API requests):
 *  - Summary strip with counts for what is currently shown.
 *  - Filter by alert type, and alerts grouped by day.
 *  - A short guide explaining alert types, read/unread and what to do next.
 *
 * Styling is injected by this file, and every selector starts with .ntf-page,
 * so no other page is affected. Colours and type match the public home page.
 */

const TYPE = {
  LEAK_SUSPECTED: { label: 'Possible leak', tone: 'red', icon: 'leak' },
  OVERUSE: { label: 'High usage', tone: 'amber', icon: 'usage' },
};

const FILTERS = [
  { key: 'ALL', label: 'All types' },
  { key: 'LEAK_SUSPECTED', label: 'Possible leak' },
  { key: 'OVERUSE', label: 'High usage' },
];

const DEFINITIONS = [
  { term: 'Possible leak', text: 'Readings suggest water may be escaping, for example from a dripping fixture or a running toilet. It is a prompt to check, not a confirmed fault.' },
  { term: 'High usage', text: 'Consumption went above the expected level for the period, which may be a one-off spike or a change in routine.' },
  { term: 'Unread and read', text: 'Unread alerts still need attention. Marking an alert as read removes it from your unread count, and it stays in your list.' },
];

const NEXT_STEPS = [
  'Open the alert and note the flat number and the time it was raised.',
  'Check visible taps, flush tanks and hoses for drips or running water.',
  'Compare the alert with recent readings in your usage view.',
  'Mark the alert as read once you have looked into it.',
  'If usage stays high, contact your society administrator.',
];

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */

function dayLabel(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Older';
  const startOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const diff = Math.round((startOfDay(new Date()) - startOfDay(date)) / 86400000);
  if (diff <= 0) return 'Today';
  if (diff === 1) return 'Yesterday';
  if (diff < 7) return 'Earlier this week';
  return 'Older';
}

function groupByDay(list) {
  const groups = [];
  const index = new Map();
  list.forEach((item) => {
    const label = dayLabel(item.createdAt);
    if (!index.has(label)) {
      index.set(label, groups.length);
      groups.push({ label, items: [] });
    }
    groups[index.get(label)].items.push(item);
  });
  return groups;
}

function Icon({ name }) {
  const paths = {
    leak: <><path d="M12 3 2.8 20h18.4L12 3Z" /><path d="M12 9v5m0 3h.01" /></>,
    usage: <><path d="M4 20V12h4v8M10 20V7h4v13M16 20V3h4v17" /><path d="M2 20h20" /></>,
    bell: <><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" /><path d="M10 21h4" /></>,
    check: <path d="m5 12 4 4L19 6" />,
  };
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name] || paths.bell}
    </svg>
  );
}

/* ------------------------------------------------------------------ */
/*  Page                                                               */
/* ------------------------------------------------------------------ */

export default function Notifications() {
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [typeFilter, setTypeFilter] = useState('ALL');
  const list = useAsync(() => get(`/api/v1/notifications?unreadOnly=${unreadOnly}`), [unreadOnly]);
  const act = useAction(() => {
    list.reload();
    window.dispatchEvent(new Event('notifications-changed'));
  });

  const items = Array.isArray(list.data) ? list.data : [];
  const unread = items.filter((n) => !n.read).length;
  const leaks = items.filter((n) => n.type === 'LEAK_SUSPECTED').length;
  const highUsage = items.filter((n) => n.type === 'OVERUSE').length;
  const visible = typeFilter === 'ALL' ? items : items.filter((n) => n.type === typeFilter);
  const groups = groupByDay(visible);
  const countFor = (key) => (key === 'ALL' ? items.length : items.filter((n) => n.type === key).length);

  return (
    <div className="notifications-page ntf-page">
      <style>{STYLES}</style>

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
              title="Marks every alert as read, including any hidden by the type filter"
              onClick={() => act.run(() => put('/api/v1/notifications/read-all'))}
            >
              Mark all as read
            </button>
          </>
        )}
      />
      <ErrorBox error={list.error || act.error} />

      {!list.loading && !list.error && (
        <>
          <dl className="ntf-stats" aria-label="Alert summary">
            <div><dt>{unreadOnly ? 'Unread alerts' : 'Alerts shown'}</dt><dd>{items.length}</dd></div>
            <div className={unread ? 'is-attention' : ''}><dt>Pending attention</dt><dd>{unread}</dd></div>
            <div><dt>Possible leaks</dt><dd>{leaks}</dd></div>
            <div><dt>High usage</dt><dd>{highUsage}</dd></div>
          </dl>
          <p className="ntf-note">
            {unreadOnly
              ? 'Showing unread notifications only. Mark items as read once investigated.'
              : 'Alerts indicate potential leaks or unusual consumption spikes beyond thresholds.'}
          </p>
        </>
      )}

      <Card title={unreadOnly ? 'Unread alerts' : `All alerts${unread ? ` (${unread} unread)` : ''}`}>
        {list.loading ? <Spinner /> : list.error ? (
          <Empty>
            We couldn’t load your alerts.{' '}
            <button type="button" className="ntf-linkbtn" onClick={() => list.reload()}>Try again</button>
          </Empty>
        ) : items.length === 0 ? (
          <Empty>No alerts found. You will see a notification here when unusual water usage or leak conditions are detected.</Empty>
        ) : (
          <>
            <div className="ntf-filters" role="group" aria-label="Filter alerts by type">
              {FILTERS.map((filter) => (
                <button
                  key={filter.key}
                  type="button"
                  className={`ntf-chip${typeFilter === filter.key ? ' is-active' : ''}`}
                  aria-pressed={typeFilter === filter.key}
                  onClick={() => setTypeFilter(filter.key)}
                >
                  {filter.label}<span>{countFor(filter.key)}</span>
                </button>
              ))}
            </div>

            {visible.length === 0 ? (
              <Empty>
                No alerts match this filter.{' '}
                <button type="button" className="ntf-linkbtn" onClick={() => setTypeFilter('ALL')}>Show all types</button>
              </Empty>
            ) : (
              groups.map((group) => (
                <section className="ntf-group" key={group.label}>
                  <h3>{group.label}</h3>
                  <ul>
                    {group.items.map((n) => {
                      const t = TYPE[n.type] || { label: n.type || 'Notification', tone: 'grey', icon: 'bell' };
                      return (
                        <li key={n.id} className={`ntf-item ntf-item--${t.tone}${n.read ? '' : ' is-unread'}`}>
                          <span className="ntf-item-icon"><Icon name={t.icon} /></span>
                          <div className="ntf-item-body">
                            <div className="ntf-item-top">
                              <Badge tone={t.tone}>{t.label}</Badge>
                              <Badge tone={n.read ? 'grey' : 'green'}>{n.read ? 'Read' : 'Unread'}</Badge>
                              {n.flatNumber && <span className="ntf-meta">Flat {n.flatNumber}</span>}
                              <span className="ntf-meta">{dateTimeStr(n.createdAt)}</span>
                            </div>
                            <p className="ntf-message">{n.message}</p>
                          </div>
                          {!n.read && (
                            <button
                              type="button"
                              className="btn btn-ghost btn-sm ntf-item-action"
                              aria-label={`Mark ${t.label} alert as read`}
                              onClick={() => act.run(() => put(`/api/v1/notifications/${n.id}/read`))}
                            >
                              Mark read
                            </button>
                          )}
                        </li>
                      );
                    })}
                  </ul>
                </section>
              ))
            )}
          </>
        )}
      </Card>

      {/* Documentation: what the alerts mean and what to do about them */}
      <section className="ntf-guide" aria-labelledby="ntf-guide-title">
        <h2 id="ntf-guide-title">Understanding your alerts</h2>
        <div className="ntf-guide-grid">
          <div>
            <h3>What each alert means</h3>
            <dl className="ntf-defs">
              {DEFINITIONS.map((item) => (
                <div key={item.term}><dt>{item.term}</dt><dd>{item.text}</dd></div>
              ))}
            </dl>
          </div>
          <div>
            <h3>What to do when you get one</h3>
            <ol className="ntf-steps">
              {NEXT_STEPS.map((step) => <li key={step}>{step}</li>)}
            </ol>
          </div>
        </div>
        <details className="ntf-details">
          <summary>How alerts are delivered</summary>
          <p>Alerts appear on this page as in-app notifications, and the unread count updates whenever you mark alerts as read. Email alerts are configurable, so you can choose how you want to be told about unusual usage.</p>
        </details>
        <details className="ntf-details">
          <summary>Why did I get an alert when nothing seems wrong?</summary>
          <p>Alerts are raised when readings go beyond the expected thresholds. A guest staying over, a washing-machine cycle or a filled tank can all cause a short spike. If you have checked and everything looks normal, you can mark the alert as read.</p>
        </details>
      </section>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Styles: every selector starts with .ntf-page                       */
/* ------------------------------------------------------------------ */

const STYLES = `
.ntf-page {
  --ntf-ink: #0b2540;
  --ntf-text: #35506a;
  --ntf-muted: #5b7186;
  --ntf-tide: #0a6ea8;
  --ntf-deep: #07496f;
  --ntf-foam: #e8f3f9;
  --ntf-paper: #f5f9fb;
  --ntf-line: #d8e5ed;
  --ntf-brass: #a9681d;
  --ntf-brass-soft: #fbf0de;
  --ntf-red: #b42318;
  --ntf-red-soft: #fdecea;
  --ntf-display: "Iowan Old Style", "Palatino Linotype", Palatino, "Book Antiqua", Georgia, serif;
}
.ntf-page *, .ntf-page *::before, .ntf-page *::after { box-sizing: border-box; }
.ntf-page :where(h2, h3, p, ul, ol, dl, dd) { margin: 0; }
.ntf-page :where(ul, ol) { padding: 0; list-style: none; }
.ntf-page svg { display: block; flex: none; }
.ntf-page button:focus-visible, .ntf-page summary:focus-visible { outline: 3px solid rgba(10, 110, 168, .5); outline-offset: 2px; border-radius: 6px; }

/* Summary strip */
.ntf-page .ntf-stats { margin: 0 0 10px; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); border: 1px solid var(--ntf-line); border-radius: 14px; background: #fff; overflow: hidden; }
.ntf-page .ntf-stats > div { padding: 16px 20px; border-right: 1px solid var(--ntf-line); display: flex; flex-direction: column; gap: 4px; }
.ntf-page .ntf-stats > div:last-child { border-right: 0; }
.ntf-page .ntf-stats dt { color: var(--ntf-muted); font-size: 13px; font-weight: 600; }
.ntf-page .ntf-stats dd { color: var(--ntf-ink); font-family: var(--ntf-display); font-size: 30px; font-weight: 600; line-height: 1.1; font-variant-numeric: tabular-nums; }
.ntf-page .ntf-stats .is-attention dd { color: var(--ntf-brass); }
.ntf-page .ntf-note { margin: 0 0 16px; color: var(--ntf-muted); font-size: 13.5px; line-height: 1.6; }

/* Filters */
.ntf-page .ntf-filters { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 8px; }
.ntf-page .ntf-chip { display: inline-flex; align-items: center; gap: 8px; min-height: 36px; padding: 0 14px; border: 1.5px solid var(--ntf-line); border-radius: 999px; background: #fff; color: var(--ntf-text); font: inherit; font-size: 13.5px; font-weight: 600; cursor: pointer; transition: border-color .2s, background .2s, color .2s; }
.ntf-page .ntf-chip span { min-width: 22px; padding: 1px 7px; border-radius: 999px; background: var(--ntf-foam); color: var(--ntf-deep); font-size: 12px; font-weight: 700; text-align: center; }
.ntf-page .ntf-chip:hover { border-color: var(--ntf-tide); }
.ntf-page .ntf-chip.is-active { border-color: var(--ntf-ink); background: var(--ntf-ink); color: #fff; }
.ntf-page .ntf-chip.is-active span { background: rgba(255, 255, 255, .18); color: #fff; }
.ntf-page .ntf-linkbtn { padding: 0; border: 0; background: transparent; color: var(--ntf-tide); font: inherit; font-weight: 700; text-decoration: underline; cursor: pointer; }

/* Alert list */
.ntf-page .ntf-group { margin-top: 18px; }
.ntf-page .ntf-group h3 { margin-bottom: 8px; color: var(--ntf-ink); font-family: var(--ntf-display); font-size: 18px; font-weight: 600; }
.ntf-page .ntf-group ul { display: grid; gap: 10px; }
.ntf-page .ntf-item { display: grid; grid-template-columns: 40px minmax(0, 1fr) auto; align-items: start; gap: 14px; padding: 14px 16px; border: 1px solid var(--ntf-line); border-left-width: 4px; border-radius: 12px; background: #fff; }
.ntf-page .ntf-item--red { border-left-color: var(--ntf-red); }
.ntf-page .ntf-item--amber { border-left-color: var(--ntf-brass); }
.ntf-page .ntf-item--grey { border-left-color: #9fb2c2; }
.ntf-page .ntf-item.is-unread { background: var(--ntf-paper); }
.ntf-page .ntf-item:not(.is-unread) .ntf-message { color: var(--ntf-muted); }
.ntf-page .ntf-item-icon { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 10px; background: var(--ntf-foam); color: var(--ntf-tide); }
.ntf-page .ntf-item--red .ntf-item-icon { background: var(--ntf-red-soft); color: var(--ntf-red); }
.ntf-page .ntf-item--amber .ntf-item-icon { background: var(--ntf-brass-soft); color: var(--ntf-brass); }
.ntf-page .ntf-item-icon svg { width: 21px; height: 21px; }
.ntf-page .ntf-item-body { min-width: 0; }
.ntf-page .ntf-item-top { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 10px; margin-bottom: 6px; }
.ntf-page .ntf-meta { color: var(--ntf-muted); font-size: 12.5px; }
.ntf-page .ntf-message { color: var(--ntf-ink); font-size: 14.5px; line-height: 1.6; overflow-wrap: anywhere; }
.ntf-page .ntf-item-action { align-self: center; white-space: nowrap; }

/* Guide */
.ntf-page .ntf-guide { margin-top: 24px; padding: 28px; border: 1px solid var(--ntf-line); border-radius: 16px; background: #fff; }
.ntf-page .ntf-guide h2 { color: var(--ntf-ink); font-family: var(--ntf-display); font-size: 26px; font-weight: 600; letter-spacing: -.01em; }
.ntf-page .ntf-guide-grid { margin-top: 20px; display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(0, .9fr); gap: 40px; }
.ntf-page .ntf-guide h3 { color: var(--ntf-ink); font-size: 16px; font-weight: 700; }
.ntf-page .ntf-defs { margin-top: 10px; display: grid; }
.ntf-page .ntf-defs > div { padding: 12px 0; border-top: 1px solid var(--ntf-line); }
.ntf-page .ntf-defs dt { color: var(--ntf-ink); font-size: 14px; font-weight: 700; }
.ntf-page .ntf-defs dd { margin-top: 3px; color: var(--ntf-text); font-size: 14px; line-height: 1.65; }
.ntf-page .ntf-steps { margin-top: 12px; display: grid; gap: 12px; counter-reset: ntf; }
.ntf-page .ntf-steps li { position: relative; padding-left: 38px; counter-increment: ntf; color: var(--ntf-text); font-size: 14px; line-height: 1.6; }
.ntf-page .ntf-steps li::before { content: counter(ntf); position: absolute; left: 0; top: -1px; width: 25px; height: 25px; display: grid; place-items: center; border-radius: 50%; background: var(--ntf-foam); color: var(--ntf-deep); font-size: 12.5px; font-weight: 800; }
.ntf-page .ntf-details { margin-top: 0; border-bottom: 1px solid var(--ntf-line); }
.ntf-page .ntf-guide-grid + .ntf-details { margin-top: 24px; border-top: 1px solid var(--ntf-line); }
.ntf-page .ntf-details summary { position: relative; padding: 14px 32px 14px 0; color: var(--ntf-ink); font-size: 14.5px; font-weight: 700; cursor: pointer; list-style: none; }
.ntf-page .ntf-details summary::-webkit-details-marker { display: none; }
.ntf-page .ntf-details summary::after { content: "+"; position: absolute; right: 4px; top: 50%; transform: translateY(-50%); color: var(--ntf-tide); font-size: 22px; font-weight: 400; line-height: 1; }
.ntf-page .ntf-details[open] summary::after { content: "\\2212"; }
.ntf-page .ntf-details p { padding: 0 32px 16px 0; color: var(--ntf-text); font-size: 14px; line-height: 1.7; }

/* Responsive */
@media (max-width: 860px) {
  .ntf-page .ntf-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .ntf-page .ntf-stats > div:nth-child(2) { border-right: 0; }
  .ntf-page .ntf-stats > div:nth-child(-n+2) { border-bottom: 1px solid var(--ntf-line); }
  .ntf-page .ntf-guide-grid { grid-template-columns: minmax(0, 1fr); gap: 28px; }
}
@media (max-width: 560px) {
  .ntf-page .ntf-stats > div { padding: 14px 16px; }
  .ntf-page .ntf-stats dd { font-size: 26px; }
  .ntf-page .ntf-item { grid-template-columns: 36px minmax(0, 1fr); gap: 12px; padding: 12px; }
  .ntf-page .ntf-item-icon { width: 36px; height: 36px; }
  .ntf-page .ntf-item-action { grid-column: 1 / -1; justify-self: stretch; }
  .ntf-page .ntf-guide { padding: 20px 16px; }
  .ntf-page .ntf-guide h2 { font-size: 22px; }
}
@media (prefers-reduced-motion: reduce) {
  .ntf-page *, .ntf-page *::before, .ntf-page *::after { animation: none !important; transition: none !important; }
}
`;