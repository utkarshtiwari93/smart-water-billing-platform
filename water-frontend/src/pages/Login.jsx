import { useId, useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { portalPathFor } from '../navigation.js';
import { errText, post } from '../api.js';
import { Alert } from '../ui.jsx';
import GoogleButton from '../GoogleButton.jsx';

/**
 * AquaFlow sign-in page
 * ----------------------------------------------------------------------------
 * UI-only refresh. The authentication logic is unchanged:
 *  - Same imports and same default export (Login), so routing is unaffected.
 *  - Same auth calls: login(email, password) and loginWithGoogle(credential, apartmentId).
 *  - Same Google first-time flow: on a 400 "apartmentId" error the page asks for
 *    the Apartment ID and retries with it.
 *  - Same redirects: signed-in users go to portalPathFor(user).
 *  - Same shared classes (form, field, field-label), same <Alert>, same <GoogleButton>.
 *
 * Styling is injected by this file and every selector starts with .afl-page,
 * so nothing here can leak into other pages. Colours and type match the public
 * home page: ink navy, tide blue, pale foam and a brass accent from water meters.
 *
 * Layout: an information panel (what the portal offers, how sign-in works) next
 * to the sign-in card. On tablets and phones the card comes first and the
 * information panel follows below it.
 */

/* ------------------------------------------------------------------ */
/*  Content                                                            */
/* ------------------------------------------------------------------ */

const RESIDENT_ITEMS = [
  'Daily and monthly usage trends',
  'Itemized invoices and PDF downloads',
  'Overuse and possible leak alerts',
  'AquaBot, for questions about your own records',
];

const ADMIN_ITEMS = [
  'Meter readings and CSV import',
  'Tariff plans and billing cycles',
  'Shared costs and water purchases',
  'Household comparisons and alerts',
];

const SIGN_IN_STEPS = [
  { title: 'Sign in', text: 'Use your email and password, or continue with Google.' },
  { title: 'Connect once with Google', text: 'The first time you use Google, enter your Apartment ID so we can link the account to your apartment.' },
  { title: 'Land in the right place', text: 'Your portal opens automatically for your role, resident or administrator.' },
];

const HELP_ITEMS = [
  {
    q: 'I can’t sign in',
    a: 'Check that your email address is spelled correctly and that Caps Lock is off. If you forgot your password, use the reset link to receive a code by email. For other sign-in issues, contact your society administrator.',
  },
  {
    q: 'What is an Apartment ID?',
    a: 'It is the number that identifies your apartment in AquaFlow. Google sign-in asks for it the first time you connect your account. Your society administrator can share it with you.',
  },
  {
    q: 'I don’t have an account yet',
    a: 'Residents can register from the Register page. Your administrator then links your account to your household so your readings and invoices appear in your portal.',
  },
  {
    q: 'Who can see my data?',
    a: 'Residents see their own household’s readings, bills and alerts. Administrators work within their own apartment’s records.',
  },
];

/* ------------------------------------------------------------------ */
/*  Small components                                                   */
/* ------------------------------------------------------------------ */

function WaterDrop() {
  // Unique gradient id, because the logo is rendered more than once on this page.
  const gradientId = useId();
  return (
    <svg viewBox="0 0 40 48" fill="none" aria-hidden="true">
      <path d="M20 2C15 10 5 21 5 30a15 15 0 0 0 30 0C35 21 25 10 20 2Z" fill={`url(#${gradientId})`} />
      <path d="M12 30c0 4.4 3.6 8 8 8" stroke="white" strokeWidth="2.5" strokeLinecap="round" />
      <defs>
        <linearGradient id={gradientId} x1="8" y1="4" x2="34" y2="42" gradientUnits="userSpaceOnUse">
          <stop stopColor="#22E5FF" />
          <stop offset="1" stopColor="#0875D1" />
        </linearGradient>
      </defs>
    </svg>
  );
}

function Icon({ name }) {
  const paths = {
    mail: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="m3 7 9 6 9-6" /></>,
    lock: <><rect x="4" y="10" width="16" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" /></>,
    check: <path d="m5 12 4 4L19 6" />,
    home: <><path d="M3 10.5 12 3l9 7.5" /><path d="M5.5 9.5V21h13V9.5" /></>,
    hash: <path d="M5 9h14M5 15h14M10 4 8 20M16 4l-2 16" />,
  };
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name] || null}
    </svg>
  );
}

function AquaFlowBrand({ mobile = false }) {
  return (
    <Link to="/" className={`afl-brand${mobile ? ' afl-brand--mobile' : ''}`} aria-label="AquaFlow home">
      <span className="afl-brand-drop"><WaterDrop /></span>
      <span className="afl-brand-copy">
        <strong>AquaFlow<sup>™</sup></strong>
        <small>Smart Water Management</small>
      </span>
    </Link>
  );
}

/* ------------------------------------------------------------------ */
/*  Page                                                               */
/* ------------------------------------------------------------------ */

export default function Login() {
  const { user, login, loginWithGoogle } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [pendingCredential, setPendingCredential] = useState(null);
  const [apartmentId, setApartmentId] = useState('');
  const [resetStep, setResetStep] = useState(0);
  const [resetCode, setResetCode] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [resetNotice, setResetNotice] = useState('');
  const uid = useId(); // all hooks stay above the early return below
  const forgotMode = location.pathname === '/forgot-password';

  if (user) {
    return <Navigate to={portalPathFor(user)} replace />;
  }

  const submit = async (e) => {
    e.preventDefault();
    if (busy) return;

    setBusy(true);
    setError('');

    try {
      const authenticatedUser = await login(email.trim(), password);
      navigate(portalPathFor(authenticatedUser), { replace: true });
    } catch (err) {
      setError(errText(err));
      setBusy(false);
    }
  };

  const google = async (credential, apt) => {
    if (busy) return;

    setError('');
    setBusy(true);

    try {
      const authenticatedUser = await loginWithGoogle(credential, apt);
      navigate(portalPathFor(authenticatedUser), { replace: true });
    } catch (err) {
      if (err.status === 400 && /apartmentId/i.test(err.message)) {
        setPendingCredential(credential);
      } else {
        setError(errText(err));
      }
    } finally {
      setBusy(false);
    }
  };

  const requestPasswordReset = async (e) => {
    e?.preventDefault?.();
    if (busy) return;
    setBusy(true);
    setError('');
    setResetNotice('');
    try {
      await post('/api/v1/auth/password/forgot', { email: email.trim() });
      setResetNotice('If an account exists for this email, a six-digit reset code has been sent.');
      setResetStep(1);
    } catch (err) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  };

  const verifyResetCode = async (e) => {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      const result = await post('/api/v1/auth/password/verify-code', {
        email: email.trim(),
        code: resetCode,
      });
      setResetToken(result.resetToken);
      setResetNotice('');
      setResetStep(2);
    } catch (err) {
      setError(errText(err));
    } finally {
      setBusy(false);
    }
  };

  const submitNewPassword = async (e) => {
    e.preventDefault();
    if (busy) return;
    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      await post('/api/v1/auth/password/reset', {
        email: email.trim(),
        resetToken,
        newPassword,
      });
      navigate('/login', { replace: true, state: { passwordReset: true } });
    } catch (err) {
      setError(errText(err));
      setBusy(false);
    }
  };

  const year = new Date().getFullYear();

  return (
    <div className="afl-page">
      <style>{STYLES}</style>

      {/* Information panel */}
      <section className="afl-info" aria-label="About AquaFlow">
        <AquaFlowBrand />

        <div className="afl-info-body">
          <h2 className="afl-headline">Your apartment’s water, made clear.</h2>
          <p className="afl-lede">
            Sign in to follow meter readings, understand every charge and keep your community’s water use in view.
          </p>

          <div className="afl-meter" aria-hidden="true">
            <span className="afl-meter-label">Meter reading, kL</span>
            <div className="afl-odometer">
              {['0', '0', '4', '2', '7'].map((digit, index) => <b key={index}>{digit}</b>)}
              <b className="is-decimal">1</b>
            </div>
          </div>

          <h3 className="afl-section-title">What’s inside your portal</h3>
          <div className="afl-roles">
            <div>
              <h4>Residents</h4>
              <ul>{RESIDENT_ITEMS.map((item) => <li key={item}><Icon name="check" />{item}</li>)}</ul>
            </div>
            <div>
              <h4>Administrators</h4>
              <ul>{ADMIN_ITEMS.map((item) => <li key={item}><Icon name="check" />{item}</li>)}</ul>
            </div>
          </div>

          <h3 className="afl-section-title">How sign-in works</h3>
          <ol className="afl-steps">
            {SIGN_IN_STEPS.map((step) => (
              <li key={step.title}>
                <strong>{step.title}</strong>
                <span>{step.text}</span>
              </li>
            ))}
          </ol>
        </div>

        <div className="afl-info-footer">
          <span>Smart water management for apartment communities.</span>
          <span>© {year} AquaFlow</span>
        </div>
      </section>

      {/* Sign-in column */}
      <section className="afl-main">
        <div className="afl-main-inner">
          <Link to="/" className="afl-back"><Icon name="home" />Back to home</Link>

          <div className="afl-card">
            <AquaFlowBrand mobile />

            <div className="afl-heading">
              <h1>{pendingCredential ? 'One last step' : forgotMode ? 'Reset your password' : 'Sign in'}</h1>
              <p>
                {pendingCredential
                  ? 'Connect your Google account to your apartment.'
                  : forgotMode
                    ? resetStep === 0
                      ? 'We’ll email a reset code if an account exists for this address.'
                      : resetStep === 1
                        ? 'Enter the six-digit code from your email.'
                        : 'Choose a new password for your AquaFlow account.'
                    : 'Use your AquaFlow account to open your portal.'}
              </p>
            </div>

            {location.state?.registered && (
              <div className="afl-alert" role="status">
                <Alert type="success">Account created. Please sign in.</Alert>
              </div>
            )}
            {location.state?.passwordReset && (
              <div className="afl-alert" role="status">
                <Alert type="success">Password reset. You can now sign in with your new password.</Alert>
              </div>
            )}
            {forgotMode && resetNotice && (
              <div className="afl-alert" role="status">
                <Alert type="success">{resetNotice}</Alert>
              </div>
            )}

            {error && (
              <div className="afl-alert" role="alert">
                <Alert>{error}</Alert>
              </div>
            )}

            {pendingCredential ? (
              <form
                className="form"
                onSubmit={(e) => {
                  e.preventDefault();
                  google(pendingCredential, apartmentId);
                }}
              >
                <div className="afl-note">
                  First time signing in with Google? Enter your Apartment ID to connect your account.
                  You can get this ID from your society administrator.
                </div>

                <label className="field">
                  <span className="field-label">Apartment ID</span>
                  <div className="afl-input-wrap">
                    <span className="afl-input-icon"><Icon name="hash" /></span>
                    <input
                      type="number"
                      min="1"
                      inputMode="numeric"
                      required
                      autoFocus
                      aria-describedby={`${uid}-apt-hint`}
                      placeholder="Enter your apartment ID"
                      value={apartmentId}
                      onChange={(e) => setApartmentId(e.target.value)}
                    />
                  </div>
                  <span className="afl-hint" id={`${uid}-apt-hint`}>You only need to do this once for this Google account.</span>
                </label>

                <div className="afl-actions">
                  <button className="afl-submit" type="submit" disabled={busy}>
                    {busy ? 'Connecting…' : 'Continue'}
                  </button>
                  <button
                    type="button"
                    className="afl-cancel"
                    disabled={busy}
                    onClick={() => {
                      setPendingCredential(null);
                      setApartmentId('');
                      setError('');
                    }}
                  >
                    Cancel
                  </button>
                </div>
              </form>
            ) : forgotMode ? (
              <form className="form" onSubmit={resetStep === 0 ? requestPasswordReset : resetStep === 1 ? verifyResetCode : submitNewPassword}>
                <label className="field">
                  <span className="field-label">Email address</span>
                  <div className="afl-input-wrap">
                    <span className="afl-input-icon"><Icon name="mail" /></span>
                    <input
                      type="email"
                      required
                      autoComplete="email"
                      readOnly={resetStep > 0}
                      placeholder="you@example.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                    />
                  </div>
                </label>

                {resetStep === 1 && (
                  <label className="field">
                    <span className="field-label">Six-digit code</span>
                    <div className="afl-input-wrap">
                      <span className="afl-input-icon"><Icon name="hash" /></span>
                      <input
                        type="text"
                        required
                        autoFocus
                        inputMode="numeric"
                        autoComplete="one-time-code"
                        pattern="[0-9]{6}"
                        maxLength={6}
                        placeholder="Enter code"
                        value={resetCode}
                        onChange={(e) => setResetCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      />
                    </div>
                    <span className="afl-hint">The code expires after 10 minutes. Google-only accounts use Google sign-in instead.</span>
                  </label>
                )}

                {resetStep === 2 && (
                  <>
                    <label className="field">
                      <span className="field-label">New password</span>
                      <div className="afl-input-wrap">
                        <span className="afl-input-icon"><Icon name="lock" /></span>
                        <input
                          type="password"
                          required
                          autoFocus
                          minLength={8}
                          maxLength={72}
                          autoComplete="new-password"
                          placeholder="At least 8 characters"
                          value={newPassword}
                          onChange={(e) => setNewPassword(e.target.value)}
                        />
                      </div>
                    </label>
                    <label className="field">
                      <span className="field-label">Confirm new password</span>
                      <div className="afl-input-wrap">
                        <span className="afl-input-icon"><Icon name="lock" /></span>
                        <input
                          type="password"
                          required
                          minLength={8}
                          maxLength={72}
                          autoComplete="new-password"
                          placeholder="Re-enter new password"
                          value={confirmPassword}
                          onChange={(e) => setConfirmPassword(e.target.value)}
                        />
                      </div>
                    </label>
                  </>
                )}

                <button className="afl-submit" type="submit" disabled={busy} aria-busy={busy}>
                  {busy ? 'Please wait…' : resetStep === 0 ? 'Send reset code' : resetStep === 1 ? 'Verify code' : 'Reset password'}
                </button>
                {resetStep === 1 && (
                  <div className="afl-forgot">
                    <button className="afl-link-button" type="button" disabled={busy} onClick={requestPasswordReset}>Resend code</button>
                  </div>
                )}
                <p className="afl-register"><Link to="/login" className="afl-link">Back to sign in</Link></p>
              </form>
            ) : (
              <>
                <form className="form" onSubmit={submit}>
                  <label className="field">
                    <span className="field-label">Email address</span>
                    <div className="afl-input-wrap">
                      <span className="afl-input-icon"><Icon name="mail" /></span>
                      <input
                        type="email"
                        name="email"
                        required
                        autoFocus
                        autoComplete="email"
                        placeholder="you@example.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                      />
                    </div>
                  </label>

                  <label className="field">
                    <span className="field-label">Password</span>
                    <div className="afl-input-wrap">
                      <span className="afl-input-icon"><Icon name="lock" /></span>
                      <input
                        type={showPassword ? 'text' : 'password'}
                        name="password"
                        required
                        autoComplete="current-password"
                        placeholder="Enter your password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                      />
                      <button
                        type="button"
                        className="afl-toggle"
                        onClick={() => setShowPassword((visible) => !visible)}
                        aria-pressed={showPassword}
                        aria-label={showPassword ? 'Hide password' : 'Show password'}
                      >
                        {showPassword ? 'Hide' : 'Show'}
                      </button>
                    </div>
                  </label>

                  <div className="afl-forgot">
                    <Link to="/forgot-password" className="afl-link">Forgot password?</Link>
                  </div>

                  <button className="afl-submit" type="submit" disabled={busy} aria-busy={busy}>
                    {busy ? 'Signing in…' : 'Log in'}
                  </button>
                </form>

                <div className="afl-divider"><span>or</span></div>

                <div className="afl-google">
                  <GoogleButton
                    onCredential={(c) => google(c)}
                    onUnavailable={() =>
                      setError(
                        'Google sign-in is not configured yet. Add VITE_GOOGLE_CLIENT_ID to water-frontend/.env and restart the app.'
                      )
                    }
                  />
                </div>
              </>
            )}

            {!pendingCredential && !forgotMode && (
              <p className="afl-register">
                Don’t have an account? <Link to="/register" className="afl-link">Register now</Link>
              </p>
            )}

            <p className="afl-secure">
              <Icon name="check" />
              Residents and administrators sign in here. Your portal opens for your role.
            </p>
          </div>

          <div className="afl-help">
            <h2>Need help signing in?</h2>
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

/* ------------------------------------------------------------------ */
/*  Styles: every selector starts with .afl-page                       */
/* ------------------------------------------------------------------ */

const STYLES = `
.afl-page {
  --afl-ink: #0b2540;
  --afl-text: #35506a;
  --afl-muted: #5b7186;
  --afl-tide: #0a6ea8;
  --afl-deep: #07496f;
  --afl-foam: #e8f3f9;
  --afl-paper: #f5f9fb;
  --afl-line: #d8e5ed;
  --afl-brass: #a9681d;
  --afl-brass-soft: #fbf0de;
  --afl-display: "Iowan Old Style", "Palatino Linotype", Palatino, "Book Antiqua", Georgia, serif;
  --afl-mono: ui-monospace, "SF Mono", Menlo, Consolas, monospace;
  min-height: 100vh;
  min-height: 100svh;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(460px, .95fr);
  color: var(--afl-ink);
  background: var(--afl-paper);
  font-family: inherit;
  line-height: 1.55;
}
.afl-page *, .afl-page *::before, .afl-page *::after { box-sizing: border-box; }
.afl-page :where(h1, h2, h3, h4, p, ul, ol) { margin: 0; }
.afl-page :where(ul, ol) { padding: 0; list-style: none; }
.afl-page svg { display: block; flex: none; }
.afl-page a:focus-visible, .afl-page button:focus-visible, .afl-page summary:focus-visible, .afl-page input:focus-visible {
  outline: 3px solid rgba(10, 110, 168, .5);
  outline-offset: 2px;
}

/* Information panel */
.afl-page .afl-info {
  position: relative;
  min-height: 100vh;
  padding: 32px clamp(28px, 5vw, 72px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 36px;
  overflow: hidden;
  color: #fff;
  background: var(--afl-deep) repeating-radial-gradient(circle at 92% 6%, transparent 0 44px, rgba(255, 255, 255, .07) 44px 45px);
}
.afl-page .afl-info-body { width: 100%; max-width: 560px; margin: 0 auto; }
.afl-page .afl-headline { font-family: var(--afl-display); font-weight: 600; font-size: clamp(34px, 3.8vw, 52px); line-height: 1.08; letter-spacing: -.02em; color: #fff; }
.afl-page .afl-lede { max-width: 470px; margin-top: 16px; color: rgba(255, 255, 255, .84); font-size: 16px; line-height: 1.7; }
.afl-page .afl-meter { display: inline-block; margin-top: 26px; padding: 12px 14px; border-radius: 12px; background: rgba(255, 255, 255, .1); border: 1px solid rgba(255, 255, 255, .18); }
.afl-page .afl-meter-label { display: block; margin-bottom: 8px; color: rgba(255, 255, 255, .78); font-size: 12px; font-weight: 600; }
.afl-page .afl-odometer { display: flex; gap: 4px; }
.afl-page .afl-odometer b { width: 32px; height: 42px; display: grid; place-items: center; border-radius: 6px; background: var(--afl-ink); color: #fff; font: 700 23px/1 var(--afl-mono); box-shadow: inset 0 -10px 12px -8px rgba(255, 255, 255, .18); }
.afl-page .afl-odometer b.is-decimal { margin-left: 4px; background: var(--afl-brass); }
.afl-page .afl-section-title { margin-top: 36px; padding-top: 22px; border-top: 1px solid rgba(255, 255, 255, .18); font-family: var(--afl-display); font-weight: 600; font-size: 20px; color: #fff; }
.afl-page .afl-roles { margin-top: 16px; display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
.afl-page .afl-roles h4 { margin: 0 0 10px; color: #9bdcf5; font-size: 14px; font-weight: 700; }
.afl-page .afl-roles ul { display: grid; gap: 9px; }
.afl-page .afl-roles li { display: flex; align-items: flex-start; gap: 8px; color: rgba(255, 255, 255, .9); font-size: 14px; line-height: 1.5; }
.afl-page .afl-roles li svg { width: 16px; height: 16px; margin-top: 3px; color: #9bdcf5; }
.afl-page .afl-steps { margin-top: 16px; display: grid; gap: 14px; counter-reset: step; }
.afl-page .afl-steps li { position: relative; padding-left: 42px; display: flex; flex-direction: column; counter-increment: step; }
.afl-page .afl-steps li::before { content: counter(step); position: absolute; left: 0; top: 0; width: 28px; height: 28px; display: grid; place-items: center; border-radius: 50%; border: 1.5px solid rgba(255, 255, 255, .45); font-size: 13px; font-weight: 700; }
.afl-page .afl-steps strong { font-size: 14.5px; }
.afl-page .afl-steps span { margin-top: 2px; color: rgba(255, 255, 255, .78); font-size: 13.5px; line-height: 1.55; }
.afl-page .afl-info-footer { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px 24px; color: rgba(255, 255, 255, .68); font-size: 12.5px; }

/* Brand */
.afl-page .afl-brand { display: inline-flex; align-items: center; gap: 10px; width: fit-content; color: #fff; text-decoration: none; }
.afl-page .afl-brand-drop { width: 36px; height: 43px; display: grid; place-items: center; }
.afl-page .afl-brand-drop svg { width: 100%; height: 100%; }
.afl-page .afl-brand-copy { display: flex; flex-direction: column; }
.afl-page .afl-brand-copy strong { font-size: 24px; font-weight: 800; letter-spacing: -.9px; line-height: 1.05; }
.afl-page .afl-brand-copy sup { margin-left: 2px; font-size: 9px; vertical-align: top; }
.afl-page .afl-brand-copy small { margin-top: 4px; font-size: 11px; opacity: .85; }
.afl-page .afl-brand--mobile { display: none; margin: 0 auto 22px; color: var(--afl-ink); }
.afl-page .afl-brand--mobile .afl-brand-copy small { color: var(--afl-muted); opacity: 1; }

/* Sign-in column */
.afl-page .afl-main { display: flex; align-items: center; justify-content: center; padding: 36px clamp(20px, 4vw, 56px); background: var(--afl-paper); }
.afl-page .afl-main-inner { width: 100%; max-width: 480px; }
.afl-page .afl-back { display: inline-flex; align-items: center; gap: 8px; margin-bottom: 16px; color: var(--afl-text); font-size: 13.5px; font-weight: 600; text-decoration: none; }
.afl-page .afl-back svg { width: 17px; height: 17px; }
.afl-page .afl-back:hover { color: var(--afl-tide); }
.afl-page .afl-card { padding: 36px 36px 28px; border: 1px solid var(--afl-line); border-radius: 18px; background: #fff; box-shadow: 0 30px 60px -36px rgba(11, 37, 64, .35); }
.afl-page .afl-heading { margin-bottom: 24px; }
.afl-page .afl-heading h1 { font-family: var(--afl-display); font-weight: 600; font-size: clamp(30px, 3.2vw, 38px); line-height: 1.1; letter-spacing: -.02em; color: var(--afl-ink); }
.afl-page .afl-heading p { margin-top: 8px; color: var(--afl-muted); font-size: 14.5px; }
.afl-page .afl-alert { margin-bottom: 14px; }

/* Form (overrides keep the shared .field / .field-label classes consistent) */
.afl-page .field { display: block; margin-bottom: 16px; }
.afl-page .field-label { display: block; margin-bottom: 7px; color: var(--afl-ink); font-size: 13px; font-weight: 700; }
.afl-page .afl-input-wrap { position: relative; }
.afl-page .afl-input-icon { position: absolute; top: 50%; left: 14px; transform: translateY(-50%); width: 18px; height: 18px; color: var(--afl-muted); pointer-events: none; }
.afl-page .afl-input-icon svg { width: 100%; height: 100%; }
.afl-page .afl-input-wrap input { width: 100%; height: 50px; padding: 0 56px 0 44px; border: 1.5px solid var(--afl-line); border-radius: 10px; outline: none; background: #fff; color: var(--afl-ink); font: inherit; font-size: 14.5px; transition: border-color .2s, box-shadow .2s; }
.afl-page .afl-input-wrap input::placeholder { color: #8aa0b5; }
.afl-page .afl-input-wrap input:focus { border-color: var(--afl-tide); box-shadow: 0 0 0 4px rgba(10, 110, 168, .14); }
.afl-page .afl-toggle { position: absolute; top: 50%; right: 8px; transform: translateY(-50%); padding: 6px 8px; border: 0; border-radius: 6px; background: transparent; color: var(--afl-tide); font: inherit; font-size: 13px; font-weight: 700; cursor: pointer; }
.afl-page .afl-toggle:hover { background: var(--afl-foam); }
.afl-page .afl-hint { display: block; margin-top: 7px; color: var(--afl-muted); font-size: 12.5px; }
.afl-page .afl-forgot { margin: -4px 0 20px; color: var(--afl-muted); font-size: 12.5px; text-align: right; }
.afl-page .afl-link-button { padding: 0; border: 0; background: transparent; color: var(--afl-tide); font: inherit; font-weight: 700; cursor: pointer; }
.afl-page .afl-link-button:hover:not(:disabled) { text-decoration: underline; }
.afl-page .afl-link-button:disabled { opacity: .6; cursor: not-allowed; }
.afl-page .afl-link { color: var(--afl-tide); font-weight: 700; text-decoration: none; }
.afl-page .afl-link:hover { text-decoration: underline; }
.afl-page .afl-submit { width: 100%; min-height: 50px; display: flex; align-items: center; justify-content: center; border: 0; border-radius: 10px; background: var(--afl-tide); color: #fff; font: inherit; font-size: 15px; font-weight: 700; cursor: pointer; transition: background .2s, box-shadow .2s, opacity .2s; }
.afl-page .afl-submit:hover:not(:disabled) { background: var(--afl-deep); box-shadow: 0 8px 20px rgba(7, 73, 111, .25); }
.afl-page .afl-submit:disabled { opacity: .65; cursor: not-allowed; }
.afl-page .afl-divider { display: flex; align-items: center; gap: 14px; margin: 22px 0 18px; color: var(--afl-muted); font-size: 12.5px; }
.afl-page .afl-divider::before, .afl-page .afl-divider::after { content: ""; flex: 1; height: 1px; background: var(--afl-line); }
.afl-page .afl-google { width: 100%; display: flex; justify-content: center; min-height: 44px; }
.afl-page .afl-register { margin-top: 24px; text-align: center; color: var(--afl-text); font-size: 14px; }
.afl-page .afl-secure { margin-top: 22px; padding-top: 16px; border-top: 1px solid var(--afl-line); display: flex; align-items: flex-start; justify-content: center; gap: 8px; color: var(--afl-muted); font-size: 12.5px; line-height: 1.5; text-align: left; }
.afl-page .afl-secure svg { width: 15px; height: 15px; margin-top: 2px; color: #17795c; }
.afl-page .afl-note { margin-bottom: 18px; padding: 14px; border: 1px solid #efd8b2; border-radius: 10px; background: var(--afl-brass-soft); color: #6d4413; font-size: 13.5px; line-height: 1.6; }
.afl-page .afl-actions { display: grid; gap: 10px; margin-top: 20px; }
.afl-page .afl-cancel { min-height: 46px; border: 1.5px solid var(--afl-line); border-radius: 10px; background: #fff; color: var(--afl-text); font: inherit; font-size: 14px; font-weight: 700; cursor: pointer; }
.afl-page .afl-cancel:hover:not(:disabled) { border-color: var(--afl-ink); color: var(--afl-ink); }
.afl-page .afl-cancel:disabled { opacity: .6; cursor: not-allowed; }

/* Help accordion */
.afl-page .afl-help { margin-top: 28px; }
.afl-page .afl-help h2 { margin-bottom: 8px; font-family: var(--afl-display); font-weight: 600; font-size: 20px; color: var(--afl-ink); }
.afl-page .afl-help details { border-bottom: 1px solid var(--afl-line); }
.afl-page .afl-help details:first-of-type { border-top: 1px solid var(--afl-line); }
.afl-page .afl-help summary { position: relative; padding: 14px 32px 14px 0; font-size: 14.5px; font-weight: 700; cursor: pointer; list-style: none; }
.afl-page .afl-help summary::-webkit-details-marker { display: none; }
.afl-page .afl-help summary::after { content: "+"; position: absolute; right: 4px; top: 50%; transform: translateY(-50%); color: var(--afl-tide); font-size: 22px; font-weight: 400; line-height: 1; }
.afl-page .afl-help details[open] summary::after { content: "\\2212"; }
.afl-page .afl-help details p { padding: 0 32px 16px 0; color: var(--afl-text); font-size: 14px; line-height: 1.7; }

/* Tablet and mobile: sign-in card first, information below */
@media (max-width: 1000px) {
  .afl-page { grid-template-columns: minmax(0, 1fr); }
  .afl-page .afl-main { order: -1; min-height: 100vh; min-height: 100svh; padding: 28px 18px 40px; }
  .afl-page .afl-info { min-height: 0; padding: 48px 24px 28px; }
  .afl-page .afl-info > .afl-brand { display: none; }
  .afl-page .afl-brand--mobile { display: inline-flex; }
}
@media (max-width: 560px) {
  .afl-page .afl-card { padding: 26px 20px 22px; border-radius: 16px; }
  .afl-page .afl-roles { grid-template-columns: minmax(0, 1fr); gap: 20px; }
  .afl-page .afl-odometer b { width: 28px; height: 38px; font-size: 20px; }
  .afl-page .afl-info { padding: 40px 18px 24px; }
}
@media (prefers-reduced-motion: reduce) {
  .afl-page *, .afl-page *::before, .afl-page *::after { animation: none !important; transition: none !important; }
}
`;