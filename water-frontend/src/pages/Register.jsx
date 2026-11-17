import { useId, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { portalPathFor } from '../navigation.js';
import { errText, post } from '../api.js';
import { Alert } from '../ui.jsx';
import './Register.css';

/**
 * AquaFlow resident registration page
 * ----------------------------------------------------------------------------
 * Registration requires a verified email before an account is created:
 *  - Same imports, same default export (Register) and same CSS file name.
 *  - Send a code to the supplied email, then create the account with that code.
 *  - Same field limits (name max 100, password 8 to 72, IDs are numbers >= 1).
 *  - Same redirects: signed-in users go to portalPathFor(user); after a
 *    successful sign-up the user goes to /login with { registered: true }.
 *
 * All styles live in Register.css and are scoped under .af-register, so no
 * other page is affected. Colours and type match the rest of the app.
 */

const BENEFITS = [
  'Daily and monthly usage trends',
  'Itemized invoices and PDF downloads',
  'Alerts for high usage and possible leaks',
  'Comparison with your apartment’s average',
];

const BEFORE_YOU_START = [
  { title: 'Apartment ID (required)', text: 'A number from your society administrator that identifies your apartment.' },
  { title: 'Household ID (optional)', text: 'Identifies your flat. If you don’t have it yet, leave it blank and ask your administrator to link you later.' },
  { title: 'Email and password', text: 'A valid email address and a password of at least 8 characters.' },
];

const NEXT_STEPS = [
  { title: 'Create your account', text: 'Fill in the form on this page.' },
  { title: 'Sign in', text: 'You will be sent to the sign-in page with a confirmation message.' },
  { title: 'Get linked to your household', text: 'If you didn’t enter a Household ID, your administrator links your account.' },
  { title: 'Open your portal', text: 'See readings, trends, invoices and alerts for your home.' },
];

const HELP_ITEMS = [
  { q: 'Where do I find my Apartment ID?', a: 'Ask your society administrator. It is the number that identifies your apartment in AquaFlow.' },
  { q: 'I don’t know my Household ID', a: 'That’s fine. Leave it empty, finish registering, and ask your administrator to link your account to your household. Your readings and invoices appear once that is done.' },
  { q: 'I already have an account', a: 'Use the Sign in link below the form. You can request a password reset code from the sign-in page.' },
  { q: 'Is this for administrators too?', a: 'This page creates resident accounts. Administrator access is set up by your society.' },
];

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

function Icon({ name }) {
  const paths = {
    user: <><circle cx="12" cy="8" r="4" /><path d="M4 21v-2a8 8 0 0 1 16 0v2" /></>,
    mail: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" /></>,
    lock: <><rect x="4" y="10" width="16" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></>,
    check: <path d="m5 12 4 4L19 6" />,
    home: <><path d="M3 10.5 12 3l9 7.5" /><path d="M5.5 9.5V21h13V9.5" /></>,
  };
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name] || null}
    </svg>
  );
}

export default function Register() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '', apartmentId: '', householdId: '' });
  const [verificationCode, setVerificationCode] = useState('');
  const [verificationStep, setVerificationStep] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  if (user) return <Navigate to={portalPathFor(user)} replace />;

  // Keep the original controlled-field update behavior.
  const set = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }));

  const submit = async (event) => {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      if (!verificationStep) {
        await post('/api/v1/auth/register/send-code', { email: form.email.trim() });
        setNotice('If this email can be registered, a six-digit verification code has been sent.');
        setVerificationStep(true);
        setBusy(false);
        return;
      }

      const body = {
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        apartmentId: Number(form.apartmentId),
        verificationCode,
      };
      if (form.householdId) body.householdId = Number(form.householdId);
      await post('/api/v1/auth/register', body);
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (err) {
      setError(errText(err));
      setBusy(false);
    }
  };

  const resendCode = async () => {
    if (busy) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await post('/api/v1/auth/register/send-code', { email: form.email.trim() });
      setNotice('If this email can be registered, a verification code has been sent.');
    } catch (err) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  };

  const passwordMet = form.password.length >= 8;

  return (
    <div className="af-register">
      {/* Information panel */}
      <section className="af-register__intro" aria-label="About AquaFlow">
        <Brand />

        <div className="af-register__intro-content">
          <h2 className="af-register__headline">Your home’s water, in one place.</h2>
          <p className="af-register__intro-description">
            Join AquaFlow to keep household water usage, bills and community updates connected.
          </p>

          <div className="af-register__meter" aria-hidden="true">
            <span>Meter reading, kL</span>
            <div className="af-register__odometer">
              {['0', '0', '4', '2', '7'].map((digit, index) => <b key={index}>{digit}</b>)}
              <b className="is-decimal">1</b>
            </div>
          </div>

          <h3 className="af-register__section-title">What you’ll get</h3>
          <ul className="af-register__benefits">
            {BENEFITS.map((item) => <li key={item}><Icon name="check" />{item}</li>)}
          </ul>

          <h3 className="af-register__section-title">Before you start</h3>
          <dl className="af-register__checklist">
            {BEFORE_YOU_START.map((item) => (
              <div key={item.title}><dt>{item.title}</dt><dd>{item.text}</dd></div>
            ))}
          </dl>

          <h3 className="af-register__section-title">What happens next</h3>
          <ol className="af-register__steps">
            {NEXT_STEPS.map((step) => (
              <li key={step.title}><strong>{step.title}</strong><span>{step.text}</span></li>
            ))}
          </ol>
        </div>

        <div className="af-register__intro-footer">
          <span>Smart water management for apartment communities.</span>
          <span>© {new Date().getFullYear()} AquaFlow</span>
        </div>
      </section>

      {/* Form panel */}
      <section className="af-register__panel">
        <div className="af-register__panel-inner">
          <Link className="af-register__back" to="/"><Icon name="home" />Back to home</Link>

          <div className="af-register__card">
            <Brand mobile />

            <div className="af-register__heading">
              <h1>{verificationStep ? 'Verify your email' : 'Create your account'}</h1>
              <p>{verificationStep
                ? `Enter the six-digit code sent to ${form.email.trim()}.`
                : 'Set up your AquaFlow household access. It takes about a minute.'}</p>
            </div>

            {error && <div className="af-register__alert" role="alert"><Alert>{error}</Alert></div>}
            {notice && <div className="af-register__alert" role="status"><Alert type="success">{notice}</Alert></div>}

            <form className="af-register__form" onSubmit={submit}>
              {!verificationStep ? (
                <>
              <label className="af-register__field">
                <span>Full name</span>
                <div className="af-register__input-wrap">
                  <Icon name="user" />
                  <input type="text" name="name" required maxLength={100} autoComplete="name" placeholder="Enter your full name" value={form.name} onChange={set('name')} />
                </div>
              </label>

              <label className="af-register__field">
                <span>Email address</span>
                <div className="af-register__input-wrap">
                  <Icon name="mail" />
                  <input type="email" name="email" required autoComplete="email" placeholder="you@example.com" value={form.email} onChange={set('email')} />
                </div>
              </label>

              <label className="af-register__field">
                <span>Password</span>
                <div className="af-register__input-wrap">
                  <Icon name="lock" />
                  <input type={showPassword ? 'text' : 'password'} name="password" required minLength={8} maxLength={72} autoComplete="new-password" placeholder="Create a password" value={form.password} onChange={set('password')} />
                  <button type="button" className="af-register__password-toggle" onClick={() => setShowPassword((visible) => !visible)} aria-pressed={showPassword} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? 'Hide' : 'Show'}</button>
                </div>
                <small className={`af-register__hint${passwordMet ? ' is-met' : ''}`}>
                  {passwordMet ? 'Long enough. Use at least 8 characters.' : 'Use at least 8 characters.'}
                </small>
              </label>

              <div className="af-register__community-heading">
                <strong>Your community</strong>
                <span>Get these IDs from your society administrator.</span>
              </div>

              <div className="af-register__id-grid">
                <label className="af-register__field">
                  <span>Apartment ID <b aria-hidden="true">*</b></span>
                  <div className="af-register__input-wrap af-register__input-wrap--plain">
                    <input type="number" name="apartmentId" min="1" required inputMode="numeric" placeholder="e.g. 12" value={form.apartmentId} onChange={set('apartmentId')} />
                  </div>
                </label>
                <label className="af-register__field">
                  <span>Household ID <em>Optional</em></span>
                  <div className="af-register__input-wrap af-register__input-wrap--plain">
                    <input type="number" name="householdId" min="1" inputMode="numeric" placeholder="Add later if unknown" value={form.householdId} onChange={set('householdId')} />
                  </div>
                </label>
              </div>

              <p className="af-register__linking-note">You can ask your administrator to link your household after registration.</p>

                </>
              ) : (
                <>
                  <label className="af-register__field">
                    <span>Six-digit verification code</span>
                    <div className="af-register__input-wrap af-register__input-wrap--plain">
                      <input
                        type="text"
                        name="verificationCode"
                        required
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        pattern="[0-9]{6}"
                        maxLength={6}
                        placeholder="Enter code"
                        value={verificationCode}
                        onChange={(event) => setVerificationCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                      />
                    </div>
                    <small className="af-register__hint">The code expires after 10 minutes.</small>
                  </label>
                  <button className="af-register__submit" type="submit" disabled={busy} aria-busy={busy}>
                    {busy ? 'Creating your account…' : 'Verify and create account'}
                  </button>
                  <div className="af-register__verification-actions">
                    <button type="button" className="af-register__text-button" onClick={resendCode} disabled={busy}>
                      Resend code
                    </button>
                    <button type="button" className="af-register__text-button" onClick={() => {
                      setVerificationStep(false);
                      setVerificationCode('');
                      setNotice('');
                      setError('');
                    }} disabled={busy}>
                      Edit registration details
                    </button>
                  </div>
                </>
              )}
              {!verificationStep && (
                <button className="af-register__submit" type="submit" disabled={busy} aria-busy={busy}>
                  {busy ? 'Sending code…' : 'Verify email and continue'}
                </button>
              )}
            </form>

            <p className="af-register__signin">Already have an account? <Link to="/login">Sign in</Link></p>
          </div>

          <div className="af-register__help">
            <h2>Need help registering?</h2>
            {HELP_ITEMS.map((item) => (
              <details key={item.q}>
                <summary>{item.q}</summary>
                <p>{item.a}</p>
              </details>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}