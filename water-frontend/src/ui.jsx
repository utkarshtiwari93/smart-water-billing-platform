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
  return <div className={`alert alert-${type}`}>{children}</div>;
}

export function ErrorBox({ error }) {
  return error ? <Alert>{errText(error)}</Alert> : null;
}

export function Spinner({ text = 'Loading...' }) {
  return <div className="spinner">{text}</div>;
}

export function Empty({ children }) {
  return <div className="empty">{children}</div>;
}

export function Card({ title, actions, children, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <div className="card-head">
          <h3>{title}</h3>
          <div className="row gap">{actions}</div>
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
      <div className="row gap">{actions}</div>
    </div>
  );
}

export function Stat({ label, value, hint, tone }) {
  return (
    <div className={`stat ${tone ? `stat-${tone}` : ''}`}>
      <div className="stat-label">{label}</div>
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
          <button className="icon-btn" onClick={onClose} aria-label="Close">x</button>
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

// Runs an async action (usually a DELETE or POST), confirming first.
export function useAction(reload) {
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);
  const run = async (fn, { confirm, success } = {}) => {
    if (confirm && !window.confirm(confirm)) return;
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
  return { run, error, message, clear: () => { setError(null); setMessage(null); } };
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
