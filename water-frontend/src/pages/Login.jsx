import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { errText } from '../api.js';
import { Alert } from '../ui.jsx';
import GoogleButton from '../GoogleButton.jsx';

export default function Login() {
  const { user, login, loginWithGoogle } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [pendingCredential, setPendingCredential] = useState(null);
  const [apartmentId, setApartmentId] = useState('');

  if (user) return <Navigate to="/" replace />;

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(email.trim(), password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(errText(err));
      setBusy(false);
    }
  };

  const google = async (credential, apt) => {
    setError('');
    try {
      await loginWithGoogle(credential, apt);
      navigate('/', { replace: true });
    } catch (err) {
      // First-time Google users must say which apartment they live in.
      if (err.status === 400 && /apartmentId/i.test(err.message)) setPendingCredential(credential);
      else setError(errText(err));
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="brand brand-large">
          <span className="brand-mark">W</span>
          <div>
            <div className="brand-name">Smart Water Billing</div>
            <div className="brand-sub">Monitor usage. Pay a fair bill.</div>
          </div>
        </div>

        <h2>Sign in</h2>
        {location.state?.registered && <Alert type="success">Account created. Please sign in.</Alert>}
        <Alert>{error}</Alert>

        {pendingCredential ? (
          <form className="form" onSubmit={(e) => { e.preventDefault(); google(pendingCredential, apartmentId); }}>
            <Alert type="info">First time with Google? Enter your Apartment ID (ask your society admin).</Alert>
            <label className="field">
              <span className="field-label">Apartment ID</span>
              <input type="number" min="1" required value={apartmentId} onChange={(e) => setApartmentId(e.target.value)} />
            </label>
            <button className="btn btn-primary btn-block">Continue</button>
            <button type="button" className="btn btn-ghost btn-block" onClick={() => setPendingCredential(null)}>Cancel</button>
          </form>
        ) : (
          <>
            <form className="form" onSubmit={submit}>
              <label className="field">
                <span className="field-label">Email</span>
                <input type="email" required autoFocus value={email} onChange={(e) => setEmail(e.target.value)} />
              </label>
              <label className="field">
                <span className="field-label">Password</span>
                <input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} />
              </label>
              <button className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Signing in...' : 'Sign in'}</button>
            </form>
            <div className="divider"><span>or</span></div>
            <GoogleButton onCredential={(c) => google(c)} />
          </>
        )}

        <p className="muted center">New resident? <Link to="/register">Create an account</Link></p>
      </div>
    </div>
  );
}
