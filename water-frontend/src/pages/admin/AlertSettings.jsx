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
      setSaved('Settings saved');
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
    <>
      <PageHeader title="Alert settings" subtitle={apartment ? `Leak and overuse alerts for ${apartment.name}` : ''} />
      <ErrorBox error={settings.error} />

      <div className="grid-2">
        <Card title="Rules">
          <form className="form" onSubmit={save}>
            <label className="switch-row">
              <input type="checkbox" checked={leak} onChange={(e) => setLeak(e.target.checked)} />
              <span>
                <strong>Leak detection</strong>
                <span className="field-hint" style={{ display: 'block' }}>
                  {settings.data ? settings.data.leakRule : ''}
                </span>
              </span>
            </label>

            <Field label="Usage limit per reading (litres)" hint="Optional. An alert is raised when one reading is above this limit. Leave empty for no limit.">
              <input type="number" min="1" step="0.01" value={threshold} onChange={(e) => setThreshold(e.target.value)} />
            </Field>

            <label className="switch-row">
              <input type="checkbox" checked={email} onChange={(e) => setEmail(e.target.checked)} />
              <span>
                <strong>Also send email</strong>
                <span className="field-hint" style={{ display: 'block' }}>
                  Works only if the server has SMTP configured. Otherwise alerts still appear inside the app.
                </span>
              </span>
            </label>

            <Alert type="success">{saved}</Alert>
            <ErrorBox error={saveError} />
            <div><button className="btn btn-primary">Save settings</button></div>
          </form>
        </Card>

        <Card title="Check now">
          <p className="muted">
            The server checks new meter readings automatically every 15 minutes. Use this button to run the check
            right now, for example after uploading readings.
          </p>
          <button className="btn btn-primary" disabled={running} onClick={runNow}>
            {running ? 'Checking...' : 'Run alert check'}
          </button>
          <ErrorBox error={runError} />
          {result && (
            <div className="stats stats-3" style={{ marginTop: 16 }}>
              <Stat label="Readings checked" value={result.readingsChecked} />
              <Stat label="Alerts raised" value={result.alertsCreated} tone={result.alertsCreated ? 'bad' : undefined} />
              <Stat label="Emails sent" value={result.emailsSent} />
            </div>
          )}
        </Card>
      </div>
    </>
  );
}
