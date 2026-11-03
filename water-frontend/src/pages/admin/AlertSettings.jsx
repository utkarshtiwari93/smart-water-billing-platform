import { useEffect, useState } from 'react';
import { get, post, put } from '../../api.js';
import { useAdmin } from '../../adminContext.jsx';
import { Alert, Card, ErrorBox, Field, PageHeader, Spinner, Stat, useAsync } from '../../ui.jsx';

export default function AlertSettings() {
  const { apartmentId, apartment } = useAdmin();
  const settings = useAsync(() => get(`/api/v1/apartments/${apartmentId}/alert-settings`), [apartmentId]);

  const [threshold, setThreshold] = useState('');
  const [leak, setLeak] = useState(true);
  const [email, setEmail] = useState(true);
  const [saved, setSaved] = useState('');
  const [saveError, setSaveError] = useState(null);

  const [running, setRunning] = useState(false);
  const [result, setResult] = useState(null);
  const [runError, setRunError] = useState(null);

  useEffect(() => {
    const s = settings.data;
    if (!s) return;
    setThreshold(s.usageThresholdLitres === null ? '' : String(s.usageThresholdLitres));
    setLeak(s.leakDetectionEnabled);
    setEmail(s.emailEnabled);
  }, [settings.data]);

  const save = async (e) => {
    e.preventDefault();
    setSaved('');
    setSaveError(null);
    try {
      await put(`/api/v1/apartments/${apartmentId}/alert-settings`, {
        usageThresholdLitres: threshold === '' ? null : Number(threshold),
        leakDetectionEnabled: leak,
        emailEnabled: email,
      });
      setSaved('Settings saved successfully');
      settings.reload();
    } catch (err) {
      setSaveError(err);
    }
  };

  const runNow = async () => {
    setRunning(true);
    setRunError(null);
    setResult(null);
    try {
      setResult(await post('/api/v1/alerts/run'));
      window.dispatchEvent(new Event('notifications-changed'));
    } catch (err) {
      setRunError(err);
    } finally {
      setRunning(false);
    }
  };

  if (settings.loading && !settings.data) return <Spinner />;

  return (
    <div className="settings-page">
      <PageHeader title="Alert settings" subtitle={apartment ? `Leak and overuse rules for ${apartment.name}` : 'Configure threshold rules and notification preferences.'} />
      <ErrorBox error={settings.error} />
      {settings.data && (
        <div className="management-summary-row">
          <div className="management-summary-card">
            <span className="management-summary-icon">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" />
              </svg>
            </span>
            <span>
              <small>LEAK DETECTION</small>
              <strong>{settings.data.leakDetectionEnabled ? 'Enabled' : 'Disabled'}</strong>
            </span>
          </div>
          <div className="management-summary-card">
            <span className="management-summary-icon management-summary-icon-blue">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                <polyline points="22,6 12,13 2,6" />
              </svg>
            </span>
            <span>
              <small>EMAIL ALERTS</small>
              <strong>{settings.data.emailEnabled ? 'Enabled' : 'Disabled'}</strong>
            </span>
          </div>
          <div className="management-summary-card">
            <span className="management-summary-icon management-summary-icon-gold">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
            </span>
            <span>
              <small>USAGE THRESHOLD</small>
              <strong>{settings.data.usageThresholdLitres === null || settings.data.usageThresholdLitres === undefined ? 'Not set' : `${Number(settings.data.usageThresholdLitres).toLocaleString('en-IN')} L`}</strong>
            </span>
          </div>
        </div>
      )}

      <div className="grid-2">
        <Card title="Alert rules">
          <form className="form" onSubmit={save}>
            <label className="switch-row check-row">
              <input type="checkbox" checked={leak} onChange={(e) => setLeak(e.target.checked)} />
              <span>
                <strong>Automated leak detection</strong>
                <span className="field-hint" style={{ display: 'block', marginTop: 4 }}>
                  {settings.data ? settings.data.leakRule : 'Flags sudden anomalous spikes and continuous flow patterns.'}
                </span>
              </span>
            </label>

            <Field label="Usage limit per reading (litres)" hint="Optional. An alert is raised when any single meter reading exceeds this volume.">
              <input type="number" min="1" step="0.01" value={threshold} onChange={(e) => setThreshold(e.target.value)} placeholder="e.g. 5000" />
            </Field>

            <label className="switch-row check-row">
              <input type="checkbox" checked={email} onChange={(e) => setEmail(e.target.checked)} />
              <span>
                <strong>Email notifications</strong>
                <span className="field-hint" style={{ display: 'block', marginTop: 4 }}>
                  Dispatches an automated email notification when high usage or leaks are triggered.
                </span>
              </span>
            </label>

            <Alert type="success">{saved}</Alert>
            <ErrorBox error={saveError} />
            <div><button className="btn btn-primary">Save settings</button></div>
          </form>
        </Card>

        <Card title="On-demand verification">
          <p className="muted" style={{ lineHeight: 1.6 }}>
            The background engine audits incoming meter readings every 15 minutes automatically.
            Trigger an immediate manual check after uploading fresh batch readings.
          </p>
          <button className="btn btn-primary" disabled={running} onClick={runNow}>
            {running ? 'Checking...' : 'Run alert check now'}
          </button>
          <ErrorBox error={runError} />
          {result && (
            <div className="stats stats-3" style={{ marginTop: 20 }}>
              <Stat label="Readings checked" value={result.readingsChecked} />
              <Stat label="Alerts raised" value={result.alertsCreated} tone={result.alertsCreated ? 'bad' : undefined} />
              <Stat label="Emails sent" value={result.emailsSent} />
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
