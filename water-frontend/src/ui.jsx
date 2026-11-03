import { useEffect, useState } from 'react';
import { errText } from './api.js';

export function useAsync(fn, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null });
  const [tick, setTick] = useState(0);
  useEffect(() => {
    let alive = true;
    setState((s) => ({ ...s, loading: true, error: null }));
    Promise.resolve()
      .then(fn)
      .then((data) => alive && setState({ data, loading: false, error: null }))
      .catch((error) => alive && setState({ data: null, loading: false, error }));
    return () => { alive = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);
  return { ...state, reload: () => setTick((t) => t + 1) };
}

export function Alert({ type = 'error', children }) {
  if (!children) return null;
  return (
    <div className={`alert alert-${type}`} role={type === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}

export function ErrorBox({ error }) {
  return error ? <Alert type="error">{errText(error)}</Alert> : null;
}

export function Spinner({ text = 'Loading...' }) {
  return (
    <div className="spinner" role="status" aria-live="polite">
      <span className="spinner-ring" aria-hidden="true" />
      <span className="spinner-text">{text}</span>
    </div>
  );
}

export function Empty({ children }) {
  return (
    <div className="empty" role="status">
      <span className="empty-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
          <path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7m16 0v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-5m16 0h-4a2 2 0 0 0-2 2 2 2 0 0 1-2 2 2 2 0 0 1-2-2 2 2 0 0 0-2-2H4" />
        </svg>
      </span>
      <span>{children}</span>
    </div>
  );
}

export function Card({ title, actions, children, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <div className="card-head">
          <h3>{title}</h3>
          {actions && <div className="row gap">{actions}</div>}
        </div>
      )}
      {children}
    </section>
  );
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-head">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {actions && <div className="row gap page-head-actions">{actions}</div>}
    </div>
  );
}

export function Stat({ label, value, hint, tone, icon }) {
  return (
    <div className={`stat ${tone ? `stat-${tone}` : ''}`}>
      <div className="row between">
        <div className="stat-label">{label}</div>
        {icon && <span className="stat-icon" aria-hidden="true">{icon}</span>}
      </div>
      <div className="stat-value">{value}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  );
}

export function Badge({ children, tone = 'grey' }) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}

const STATUS_TONE = { OPEN: 'green', FINALIZED: 'blue', ARCHIVED: 'grey' };
export const StatusBadge = ({ status }) => <Badge tone={STATUS_TONE[status] || 'grey'}>{status}</Badge>;

export function Modal({ title, onClose, children, footer }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <div className="modal" onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="icon-btn" onClick={onClose} aria-label="Close">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  );
}

export function Field({ label, hint, children }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {hint && <span className="field-hint">{hint}</span>}
    </label>
  );
}

// Modal with a form: handles submit, busy state and server errors.
export function FormModal({ title, onClose, onSubmit, submitLabel = 'Save', children }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onSubmit();
      onClose(true);
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  return (
    <Modal title={title} onClose={() => onClose(false)}>
      <form onSubmit={submit} className="form">
        {children}
        <ErrorBox error={error} />
        <div className="row gap end">
          <button type="button" className="btn btn-ghost" onClick={() => onClose(false)}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving...' : submitLabel}</button>
        </div>
      </form>
    </Modal>
  );
}

export function ConfirmDialog({ message, onConfirm, onCancel }) {
  return (
    <div className="modal-backdrop" onMouseDown={onCancel}>
      <div className="modal confirm-dialog" onMouseDown={(e) => e.stopPropagation()} style={{ maxWidth: 420 }}>
        <div className="modal-head">
          <h3>Confirm action</h3>
          <button className="icon-btn" onClick={onCancel} aria-label="Close">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.2">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="modal-body">
          <p style={{ margin: 0, color: 'var(--ink-800)', lineHeight: 1.6 }}>{message}</p>
          <div className="row gap end" style={{ marginTop: 22 }}>
            <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>
            <button type="button" className="btn btn-danger" onClick={onConfirm}>Confirm</button>
          </div>
        </div>
      </div>
    </div>
  );
}

// Runs an async action (usually a DELETE or POST), confirming first.
export function useAction(reload) {
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const [pending, setPending] = useState(null);

  const execute = async (fn, success) => {
    setError(null);
    setMessage(null);
    try {
      await fn();
      if (success) setMessage(success);
      if (reload) reload();
    } catch (e) {
      setError(e);
    }
  };

  const run = async (fn, { confirm: confirmMsg, success } = {}) => {
    if (confirmMsg) {
      if (typeof window !== 'undefined' && window.confirm) {
        if (!window.confirm(confirmMsg)) return;
        return execute(fn, success);
      }
      setPending({ fn, success, confirmMsg });
      return;
    }
    return execute(fn, success);
  };

  const dialog = pending ? (
    <ConfirmDialog
      message={pending.confirmMsg}
      onConfirm={async () => {
        const item = pending;
        setPending(null);
        await execute(item.fn, item.success);
      }}
      onCancel={() => setPending(null)}
    />
  ) : null;

  return {
    run,
    error,
    message,
    clear: () => { setError(null); setMessage(null); },
    dialog,
  };
}

export function Tabs({ tabs, active, onChange }) {
  return (
    <div className="tabs">
      {tabs.map((t) => (
        <button key={t.key} className={`tab ${active === t.key ? 'tab-active' : ''}`} onClick={() => onChange(t.key)}>
          {t.label}
        </button>
      ))}
    </div>
  );
}
