import { useId, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { portalPathFor } from '../navigation.js';
import { errText, post } from '../api.js';
import { Alert } from '../ui.jsx';
import './Register.css';

/**
 * AquaFlow resident registration page
 * -----------------------------------
 * UI refresh only: preserves the existing registration endpoint, request body,
 * authentication redirect, field constraints, and post-registration route.
 * All styles are scoped under .af-register to avoid changing other pages.
 */
function AquaDrop({ className = '' }) {
  const gradientId = useId();
  return (
    <svg className={className} viewBox="0 0 40 48" fill="none" aria-hidden="true">
      <path d="M20 2C15 10 5 21 5 30a15 15 0 0 0 30 0C35 21 25 10 20 2Z" fill={`url(#${gradientId})`} />
      <path d="M12 30a8 8 0 0 0 8 8" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
      <defs>
        <linearGradient id={gradientId} x1="7" y1="4" x2="34" y2="43" gradientUnits="userSpaceOnUse">
          <stop stopColor="#19E5FA" />
          <stop offset="1" stopColor="#0875CE" />
        </linearGradient>
      </defs>
    </svg>
  );
}

function Brand({ mobile = false }) {
  return (
    <Link to="/" className={`af-register__brand${mobile ? ' af-register__brand--mobile' : ''}`} aria-label="AquaFlow home">
      <span className="af-register__brand-mark"><AquaDrop /></span>
      <span className="af-register__brand-copy">
        <strong>AquaFlow<sup>™</sup></strong>
        <small>Smart Water Management</small>
      </span>
    </Link>
  );
}

function FeatureIcon({ name }) {
  const icons = {
    usage: <><path d="M4 20V12h4v8M10 20V7h4v13M16 20V3h4v17" /><path d="M2 20h20" /></>,
    billing: <><path d="M6 3h9l5 5v13H6z" /><path d="M15 3v5h5M9 12h8M9 16h8" /></>,
    shield: <><path d="M12 3 20 6v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6l8-3Z" /><path d="m8.5 12 2.3 2.3 4.8-5" /></>,
  };
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {icons[name]}
    </svg>
  );
}

function ApartmentIllustration() {
  return (
    <svg className="af-register__building" viewBox="0 0 520 430" fill="none" role="img" aria-label="Smart apartment connected to a water monitoring network">
      <defs>
        <linearGradient id="reg-building-front" x1="100" y1="80" x2="330" y2="390" gradientUnits="userSpaceOnUse"><stop stopColor="#F7FDFF"/><stop offset="1" stopColor="#8BC8E9"/></linearGradient>
        <linearGradient id="reg-building-side" x1="320" y1="80" x2="460" y2="370" gradientUnits="userSpaceOnUse"><stop stopColor="#C7EDFF"/><stop offset="1" stopColor="#579BC8"/></linearGradient>
        <linearGradient id="reg-glass" x1="0" y1="0" x2="1" y2="1"><stop stopColor="#064B80"/><stop offset="1" stopColor="#20C4E9"/></linearGradient>
        <filter id="reg-glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="5" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>
      </defs>
      <ellipse cx="260" cy="395" rx="210" ry="22" fill="#04D8F3" opacity=".22"/>
      <path d="m82 105 205-76 157 58-204 79-158-61Z" fill="#E6F8FF" stroke="white" strokeWidth="4"/>
      <path d="m82 105 158 61v210L82 318V105Z" fill="url(#reg-building-front)" stroke="#E9FAFF" strokeWidth="3"/>
      <path d="m240 166 204-79v210l-204 79V166Z" fill="url(#reg-building-side)" stroke="#E9FAFF" strokeWidth="3"/>
      {[0,1,2].map((floor) => {
        const y = 165 + floor * 67;
        return <g key={floor}>
          <path d={`M82 ${y+2} 240 ${y+63} 444 ${y-16}`} stroke="white" strokeWidth="7"/>
          <path d={`M99 ${y+10}v42l57 22v-42z`} fill="url(#reg-glass)" stroke="#F2FDFF" strokeWidth="2"/>
          <path d={`M171 ${y+38}v42l49 19v-42z`} fill="url(#reg-glass)" stroke="#F2FDFF" strokeWidth="2"/>
          <path d={`M261 ${y+44}v-39l67-26v39z`} fill="url(#reg-glass)" stroke="#F2FDFF" strokeWidth="2"/>
          <path d={`M349 ${y+10}v-39l77-30v39z`} fill="url(#reg-glass)" stroke="#F2FDFF" strokeWidth="2"/>
        </g>;
      })}
      <path d="M270 30V12c0-13 76-13 76 0v33" fill="#087FC5" stroke="#A8F3FF" strokeWidth="3"/>
      <ellipse cx="308" cy="12" rx="38" ry="11" fill="#1B9EDB" stroke="#C4F8FF" strokeWidth="3"/>
      <path d="M308 46v91h-39v62h-57v58h82v68h48" stroke="#13E9FF" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" filter="url(#reg-glow)"/>
      {[[269,199],[212,257],[294,257],[342,325]].map(([x,y],i)=><g key={i}><circle cx={x} cy={y} r="12" fill="#079ED5" stroke="#DFFFFF" strokeWidth="3"/><path d={`M${x} ${y-5}v10m-5-5h10`} stroke="white" strokeWidth="2" strokeLinecap="round"/></g>)}
      <path d="M308 75c-7 10-12 16-12 24a12 12 0 0 0 24 0c0-8-5-14-12-24Z" fill="#12DDF5" stroke="white" strokeWidth="2"/>
      <path d="M54 376h420l20 18H34z" fill="#D5F5FF" stroke="white" strokeWidth="3"/><path d="M34 394h460v13H34z" fill="#318FC1"/>
    </svg>
  );
}

export default function Register() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '', apartmentId: '', householdId: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  if (user) return <Navigate to={portalPathFor(user)} replace />;

  // Keep the original controlled-field update behavior.
  const set = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }));

  // Keep the existing API contract and registration flow unchanged.
  const submit = async (event) => {
    event.preventDefault();
    if (busy) return;
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
    <div className="af-register">
      <section className="af-register__intro">
        <Brand />

        <div className="af-register__intro-content">
          <span className="af-register__eyebrow">SMART WATER MANAGEMENT</span>
          <h1>Your community.<br/>Your water.<br/><span>One smart place.</span></h1>
          <p className="af-register__intro-description">
            Join AquaFlow to keep household water usage, bills and community updates connected.
          </p>

          <div className="af-register__showcase">
            <div className="af-register__building-wrap">
              <ApartmentIllustration />
              <div className="af-register__building-caption"><AquaDrop/> <span><strong>AquaFlow Smart Apartment</strong><small>Connected water network</small></span></div>
            </div>
            <div className="af-register__benefits">
              <div className="af-register__benefit"><span><FeatureIcon name="usage"/></span><div><strong>Track usage</strong><small>Understand household readings</small></div></div>
              <div className="af-register__benefit"><span><FeatureIcon name="billing"/></span><div><strong>Clear billing</strong><small>Keep invoices close</small></div></div>
              <div className="af-register__benefit"><span><FeatureIcon name="shield"/></span><div><strong>Stay informed</strong><small>Get important alerts</small></div></div>
            </div>
          </div>
        </div>

        <div className="af-register__intro-footer">
          <span>Every drop counts. Every household matters.</span>
          <span>© {new Date().getFullYear()} AquaFlow</span>
        </div>
        <div className="af-register__wave" aria-hidden="true" />
      </section>

      <section className="af-register__panel">
        <div className="af-register__card">
          <Link className="af-register__back" to="/">← Back to home</Link>
          <Brand mobile />

          <div className="af-register__heading">
            <span>RESIDENT REGISTRATION</span>
            <h2>Create your account</h2>
            <p>Set up your AquaFlow household access in a few steps.</p>
          </div>

          {error && <div className="af-register__alert"><Alert>{error}</Alert></div>}

          <form className="af-register__form" onSubmit={submit}>
            <label className="af-register__field">
              <span>Full name</span>
              <div className="af-register__input-wrap">
                <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/></svg>
                <input type="text" required maxLength={100} autoComplete="name" placeholder="Enter your full name" value={form.name} onChange={set('name')}/>
              </div>
            </label>

            <label className="af-register__field">
              <span>Email address</span>
              <div className="af-register__input-wrap">
                <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg>
                <input type="email" required autoComplete="email" placeholder="you@example.com" value={form.email} onChange={set('email')}/>
              </div>
            </label>

            <label className="af-register__field">
              <span>Password</span>
              <div className="af-register__input-wrap">
                <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>
                <input type={showPassword ? 'text' : 'password'} required minLength={8} maxLength={72} autoComplete="new-password" placeholder="Create a password" value={form.password} onChange={set('password')}/>
                <button type="button" className="af-register__password-toggle" onClick={() => setShowPassword((visible) => !visible)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? 'Hide' : 'Show'}</button>
              </div>
              <small className="af-register__hint">Use at least 8 characters.</small>
            </label>

            <div className="af-register__community-heading">
              <strong>YOUR COMMUNITY</strong>
              <span>Get these IDs from your society administrator.</span>
            </div>

            <div className="af-register__id-grid">
              <label className="af-register__field">
                <span>Apartment ID <b>*</b></span>
                <div className="af-register__input-wrap af-register__input-wrap--plain">
                  <input type="number" min="1" required inputMode="numeric" placeholder="e.g. 12" value={form.apartmentId} onChange={set('apartmentId')}/>
                </div>
              </label>
              <label className="af-register__field">
                <span>Household ID <em>Optional</em></span>
                <div className="af-register__input-wrap af-register__input-wrap--plain">
                  <input type="number" min="1" inputMode="numeric" placeholder="Add later if unknown" value={form.householdId} onChange={set('householdId')}/>
                </div>
              </label>
            </div>

            <p className="af-register__linking-note"><span>i</span>You can ask your administrator to link your household after registration.</p>

            <button className="af-register__submit" type="submit" disabled={busy}>
              {busy ? 'Creating your account...' : 'Create resident account'}
              {!busy && <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14m-6-6 6 6-6 6"/></svg>}
            </button>
          </form>

          <p className="af-register__signin">Already have an account? <Link to="/login">Sign in</Link></p>
          <p className="af-register__secure"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 20 6v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6l8-3Z"/><path d="m8.5 12 2.3 2.3 4.8-5"/></svg>Your account details are protected and secure.</p>
        </div>
      </section>
    </div>
  );
}
