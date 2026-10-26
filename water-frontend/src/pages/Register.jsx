import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { errText, post } from '../api.js';
import { Alert } from '../ui.jsx';

export default function Register() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '', apartmentId: '', householdId: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  if (user) return <Navigate to="/" replace />;

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const body = {
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        apartmentId: Number(form.apartmentId),
      };
      if (form.householdId) body.householdId = Number(form.householdId);
      await post('/api/v1/auth/register', body);
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (err) {
      setError(errText(err));
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="brand brand-large">
          <span className="brand-mark">W</span>
          <div>
            <div className="brand-name">Smart Water Billing</div>
            <div className="brand-sub">Resident registration</div>
          </div>
        </div>
        <h2>Create account</h2>
        <Alert>{error}</Alert>
        <form className="form" onSubmit={submit}>
          <label className="field"><span className="field-label">Full name</span>
            <input required maxLength={100} value={form.name} onChange={set('name')} /></label>
          <label className="field"><span className="field-label">Email</span>
            <input type="email" required value={form.email} onChange={set('email')} /></label>
          <label className="field"><span className="field-label">Password</span>
            <input type="password" required minLength={8} maxLength={72} value={form.password} onChange={set('password')} />
            <span className="field-hint">At least 8 characters</span></label>
          <div className="grid-2">
            <label className="field"><span className="field-label">Apartment ID</span>
              <input type="number" min="1" required value={form.apartmentId} onChange={set('apartmentId')} /></label>
            <label className="field"><span className="field-label">Household ID (optional)</span>
              <input type="number" min="1" value={form.householdId} onChange={set('householdId')} /></label>
          </div>
          <p className="muted small">Your society admin can give you the Apartment and Household IDs. They can also link your flat later.</p>
          <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Creating...' : 'Create account'}</button>
        </form>
        <p className="muted center">Already registered? <Link to="/login">Sign in</Link></p>
      </div>
    </div>
  );
}
