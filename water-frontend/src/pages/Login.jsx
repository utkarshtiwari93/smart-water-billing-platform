
import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { portalPathFor } from '../navigation.js';
import { errText } from '../api.js';
import { Alert } from '../ui.jsx';
import GoogleButton from '../GoogleButton.jsx';

const WaterDrop = () => (
  <svg viewBox="0 0 40 48" fill="none" aria-hidden="true">
    <path
      d="M20 2C15 10 5 21 5 30a15 15 0 0 0 30 0C35 21 25 10 20 2Z"
      fill="url(#aquaDrop)"
    />
    <path
      d="M12 30c0 4.4 3.6 8 8 8"
      stroke="white"
      strokeWidth="2.5"
      strokeLinecap="round"
    />
    <defs>
      <linearGradient id="aquaDrop" x1="8" y1="4" x2="34" y2="42">
        <stop stopColor="#22E5FF" />
        <stop offset="1" stopColor="#0875D1" />
      </linearGradient>
    </defs>
  </svg>
);

const FeatureIcon = ({ type }) => {
  const paths = {
    usage: (
      <>
        <path d="M4 19V12h4v7M10 19V7h4v12M16 19V3h4v16" />
      </>
    ),
    billing: (
      <>
        <path d="M6 3h9l5 5v13H6z" />
        <path d="M15 3v5h5M9 12h8M9 16h8" />
      </>
    ),
    security: (
      <>
        <path d="m12 2 9 4v6c0 5-4 9-9 11-5-2-9-6-9-11V6z" />
        <path d="m8 12 3 3 6-6" />
      </>
    ),
    ai: (
      <>
        <path d="m12 2 2.5 7.5L22 12l-7.5 2.5L12 22l-2.5-7.5L2 12l7.5-2.5z" />
        <path d="m19 2 1 3 3 1-3 1-1 3-1-3-3-1 3-1z" />
      </>
    ),
  };

  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      {paths[type]}
    </svg>
  );
};

function AquaFlowBrand({ mobile = false }) {
  return (
    <Link
      to="/login"
      className={`af-brand ${mobile ? 'af-brand-mobile' : ''}`}
      aria-label="AquaFlow home"
    >
      <span className="af-brand-drop">
        <WaterDrop />
      </span>
      <span className="af-brand-copy">
        <strong>AquaFlow<sup>™</sup></strong>
        <small>Smart Water Management</small>
      </span>
    </Link>
  );
}

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

  return (
    <div className="af-login-page">
      <style>{`
        .af-login-page {
          --af-primary: #007fc2;
          --af-dark: #063c70;
          --af-cyan: #08d6ed;
          --af-text: #102f53;
          --af-muted: #607c99;
          min-height: 100vh;
          min-height: 100svh;
          display: grid;
          grid-template-columns: minmax(0, 1.15fr) minmax(440px, .85fr);
          overflow: hidden;
          color: var(--af-text);
          background: #d9f4ff;
          font-family: inherit;
        }

        .af-login-page *,
        .af-login-page *::before,
        .af-login-page *::after {
          box-sizing: border-box;
        }

        .af-left {
          position: relative;
          min-height: 100vh;
          padding: 30px clamp(30px, 5vw, 76px);
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          overflow: hidden;
          color: white;
          background:
            radial-gradient(ellipse at 72% 15%, rgba(106,224,255,.62), transparent 35%),
            radial-gradient(ellipse at 5% 80%, rgba(0,208,231,.3), transparent 40%),
            linear-gradient(125deg, #064b83 0%, #087fc0 48%, #28bde2 100%);
        }

        .af-left::before,
        .af-left::after {
          content: '';
          position: absolute;
          border: 1px solid rgba(255,255,255,.14);
          border-radius: 50%;
          pointer-events: none;
        }

        .af-left::before {
          width: 560px;
          height: 560px;
          top: 16%;
          left: -290px;
        }

        .af-left::after {
          width: 740px;
          height: 740px;
          top: 5%;
          right: -510px;
        }

        .af-brand {
          display: inline-flex;
          align-items: center;
          gap: 11px;
          color: white;
          text-decoration: none;
          position: relative;
          z-index: 2;
          width: fit-content;
        }

        .af-brand-drop {
          width: 43px;
          height: 49px;
          display: grid;
          place-items: center;
        }

        .af-brand-drop svg {
          width: 100%;
          height: 100%;
        }

        .af-brand-copy {
          display: flex;
          flex-direction: column;
          gap: 2px;
        }

        .af-brand-copy strong {
          font-size: 27px;
          font-weight: 800;
          letter-spacing: -1.2px;
          line-height: 1.1;
        }

        .af-brand-copy sup {
          font-size: 10px;
          vertical-align: top;
          margin-left: 2px;
        }

        .af-brand-copy small {
          font-size: 12px;
          letter-spacing: .25px;
          opacity: .92;
        }

        .af-hero {
          position: relative;
          z-index: 1;
          max-width: 720px;
          width: 100%;
          margin: 35px auto;
        }

        .af-kicker {
          display: flex;
          align-items: center;
          gap: 12px;
          font-size: 11px;
          font-weight: 800;
          letter-spacing: 3px;
          color: #c7f8ff;
          text-transform: uppercase;
        }

        .af-kicker::after {
          content: '';
          width: 55px;
          height: 1px;
          background: rgba(255,255,255,.65);
        }

        .af-hero h1 {
          margin: 24px 0 14px;
          font-size: clamp(42px, 4.3vw, 66px);
          line-height: 1.05;
          letter-spacing: -2.5px;
          font-weight: 800;
          color: white;
        }

        .af-hero h1 span {
          color: #38edff;
        }

        .af-hero-description {
          max-width: 390px;
          color: rgba(255,255,255,.85);
          font-size: 15px;
          line-height: 1.75;
          margin: 0;
        }

        .af-showcase {
          display: grid;
          grid-template-columns: minmax(0, 1fr) minmax(180px, .62fr);
          align-items: center;
          gap: 22px;
          margin-top: 30px;
        }

        .af-building {
          min-height: 285px;
          position: relative;
          display: flex;
          align-items: center;
          justify-content: center;
          overflow: hidden;
          border-radius: 25px;
          background:
            linear-gradient(145deg, rgba(255,255,255,.17), rgba(255,255,255,.035));
          border: 1px solid rgba(255,255,255,.2);
        }

        .af-building-art {
          width: 100%;
          max-width: 340px;
          filter: drop-shadow(0 18px 25px rgba(0,29,67,.25));
          animation: af-float 5s ease-in-out infinite;
        }

        @keyframes af-float {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-8px); }
        }

        .af-building-label {
          position: absolute;
          left: 15px;
          bottom: 14px;
          padding: 8px 12px;
          display: flex;
          align-items: center;
          gap: 8px;
          background: rgba(2,45,85,.65);
          border: 1px solid rgba(255,255,255,.2);
          border-radius: 10px;
          font-size: 11px;
          font-weight: 700;
          backdrop-filter: blur(10px);
        }

        .af-building-label svg {
          width: 17px;
          height: 20px;
        }

        .af-features {
          display: flex;
          flex-direction: column;
          gap: 15px;
        }

        .af-feature {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .af-feature-icon {
          width: 43px;
          height: 43px;
          flex-shrink: 0;
          display: grid;
          place-items: center;
          border-radius: 12px;
          color: white;
          background: rgba(255,255,255,.16);
          border: 1px solid rgba(255,255,255,.12);
        }

        .af-feature-icon svg {
          width: 22px;
          height: 22px;
        }

        .af-feature strong {
          display: block;
          font-size: 13px;
          font-weight: 700;
        }

        .af-feature small {
          display: block;
          margin-top: 3px;
          font-size: 10px;
          color: rgba(255,255,255,.75);
          line-height: 1.4;
        }

        .af-left-footer {
          position: relative;
          z-index: 1;
          display: flex;
          justify-content: space-between;
          flex-wrap: wrap;
          gap: 10px;
          color: rgba(255,255,255,.75);
          font-size: 11px;
        }

        .af-water-wave {
          position: absolute;
          height: 125px;
          width: 150%;
          bottom: -65px;
          left: -20%;
          border-radius: 45%;
          transform: rotate(-4deg);
          background: rgba(30,225,250,.15);
          border-top: 2px solid rgba(144,248,255,.5);
        }

        .af-right {
          min-height: 100vh;
          padding: 36px clamp(24px, 4vw, 65px);
          display: flex;
          justify-content: center;
          align-items: center;
          position: relative;
          background:
            radial-gradient(ellipse at 90% 5%, rgba(119,226,255,.35), transparent 38%),
            linear-gradient(145deg, #d7f5ff, #f6fcff 55%, #c7efff);
        }

        .af-right::before {
          content: '';
          position: absolute;
          width: 300px;
          height: 300px;
          right: -180px;
          bottom: -140px;
          border: 35px solid rgba(0,174,225,.07);
          border-radius: 50%;
        }

        .af-card {
          position: relative;
          z-index: 1;
          width: 100%;
          max-width: 475px;
          padding: 37px 38px 30px;
          border: 1px solid rgba(255,255,255,.85);
          border-radius: 25px;
          background: rgba(255,255,255,.69);
          box-shadow:
            0 24px 65px rgba(15,105,153,.12),
            inset 0 1px 0 rgba(255,255,255,.9);
          backdrop-filter: blur(22px);
        }

        .af-brand-mobile {
          display: none;
          color: var(--af-dark);
          margin: 0 auto 28px;
        }

        .af-brand-mobile .af-brand-copy small {
          color: var(--af-muted);
        }

        .af-heading {
          text-align: center;
          margin-bottom: 27px;
        }

        .af-heading h2 {
          margin: 0;
          font-size: clamp(27px, 3vw, 34px);
          letter-spacing: -1.1px;
          line-height: 1.2;
          font-weight: 800;
          color: #0b3159;
        }

        .af-heading p {
          margin: 9px 0 0;
          color: #617b98;
          font-size: 13px;
        }

        .af-heading-eyebrow {
          display: block;
          margin-bottom: 10px;
          color: #0789c7;
          font-size: 10px;
          font-weight: 800;
          letter-spacing: 2px;
        }

        .af-login-page .af-alert-space {
          margin-bottom: 13px;
        }

        .af-login-page .field {
          display: block;
          margin-bottom: 17px;
        }

        .af-login-page .field-label {
          display: block;
          margin-bottom: 8px;
          color: #254967;
          font-size: 12px;
          font-weight: 700;
        }

        .af-login-page .af-input-wrap {
          position: relative;
        }

        .af-login-page .af-input-icon {
          position: absolute;
          top: 50%;
          left: 15px;
          transform: translateY(-50%);
          width: 18px;
          height: 18px;
          color: #5b83a4;
          pointer-events: none;
        }

        .af-login-page .af-input-wrap input {
          width: 100%;
          height: 53px;
          padding: 0 47px;
          border: 1px solid #dcebf4;
          border-radius: 12px;
          outline: none;
          background: rgba(255,255,255,.82);
          color: #123b61;
          font: inherit;
          font-size: 13px;
          box-shadow: 0 4px 12px rgba(30,119,166,.045);
          transition: border-color .2s, box-shadow .2s, background .2s;
        }

        .af-login-page .af-input-wrap input::placeholder {
          color: #8aa0b5;
        }

        .af-login-page .af-input-wrap input:focus {
          background: white;
          border-color: #11a9db;
          box-shadow: 0 0 0 4px rgba(17,169,219,.11);
        }

        .af-password-toggle {
          position: absolute;
          top: 50%;
          right: 14px;
          transform: translateY(-50%);
          border: 0;
          background: transparent;
          color: #0b83bd;
          font-size: 11px;
          font-weight: 700;
          cursor: pointer;
        }

        .af-forgot-row {
          display: flex;
          justify-content: flex-end;
          margin: -5px 0 20px;
        }

        .af-login-page .af-link {
          color: #0077ce;
          font-size: 12px;
          font-weight: 700;
          text-decoration: none;
        }

        .af-login-page .af-link:hover {
          text-decoration: underline;
        }

        .af-submit {
          width: 100%;
          min-height: 53px;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 11px;
          border: 0;
          border-radius: 12px;
          color: white;
          font-size: 14px;
          font-weight: 800;
          cursor: pointer;
          background: linear-gradient(105deg, #05afd9, #0875bc 65%, #07508d);
          box-shadow: 0 8px 18px rgba(0,119,186,.22);
          transition: transform .2s, box-shadow .2s, opacity .2s;
        }

        .af-submit:hover:not(:disabled) {
          transform: translateY(-2px);
          box-shadow: 0 12px 24px rgba(0,119,186,.3);
        }

        .af-submit:disabled {
          opacity: .65;
          cursor: not-allowed;
        }

        .af-submit svg {
          width: 19px;
          height: 19px;
        }

        .af-divider {
          display: flex;
          align-items: center;
          gap: 13px;
          margin: 24px 0 19px;
          color: #718ba4;
          font-size: 11px;
          white-space: nowrap;
        }

        .af-divider::before,
        .af-divider::after {
          content: '';
          height: 1px;
          flex: 1;
          background: #cbdfe9;
        }

        .af-google {
          width: 100%;
        }

        .af-register {
          margin: 26px 0 0;
          text-align: center;
          color: #637e98;
          font-size: 12px;
        }

        .af-register a {
          margin-left: 5px;
        }

        .af-secure {
          margin: 23px 0 0;
          padding-top: 16px;
          border-top: 1px solid rgba(118,171,198,.2);
          display: flex;
          justify-content: center;
          align-items: center;
          gap: 7px;
          color: #8098aa;
          font-size: 10px;
        }

        .af-secure svg {
          width: 14px;
          height: 14px;
          color: #12a5b7;
        }

        .af-apartment-note {
          padding: 14px;
          margin-bottom: 19px;
          border: 1px solid #b9e9f5;
          border-radius: 12px;
          background: #e9faff;
          color: #23617f;
          font-size: 12px;
          line-height: 1.6;
        }

        .af-apartment-actions {
          display: grid;
          gap: 10px;
        }

        .af-cancel {
          min-height: 45px;
          border: 1px solid #d3e7f0;
          border-radius: 11px;
          color: #41617b;
          background: white;
          font-weight: 700;
          cursor: pointer;
        }

        @media (max-width: 1050px) {
          .af-login-page {
            grid-template-columns: minmax(0, 1fr) minmax(390px, .85fr);
          }

          .af-showcase {
            grid-template-columns: 1fr;
          }

          .af-building {
            min-height: 230px;
          }

          .af-features {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
          }

          .af-feature small {
            display: none;
          }
        }

        @media (max-width: 760px) {
          .af-login-page {
            display: block;
            min-height: 100vh;
            background: linear-gradient(150deg, #d6f5ff, #f8fdff);
          }

          .af-left {
            display: none;
          }

          .af-right {
            min-height: 100vh;
            padding: 28px 18px;
            background: transparent;
          }

          .af-card {
            max-width: 460px;
            padding: 30px 25px 25px;
            border-radius: 22px;
            box-shadow: 0 15px 45px rgba(15,105,153,.12);
          }

          .af-brand-mobile {
            display: inline-flex;
          }

          .af-heading h2 {
            font-size: 29px;
          }
        }

        @media (max-width: 380px) {
          .af-card {
            padding: 25px 18px;
          }

          .af-heading h2 {
            font-size: 26px;
          }
        }

        @media (prefers-reduced-motion: reduce) {
          .af-login-page *,
          .af-login-page *::before,
          .af-login-page *::after {
            animation: none !important;
            transition: none !important;
          }
        }
      `}</style>

      <section className="af-left">
        <AquaFlowBrand />

        <div className="af-hero">
          <div className="af-kicker">SMART WATER MANAGEMENT</div>

          <h1>
            Monitor,<br />
            Manage &amp;<br />
            <span>Save Water.</span>
          </h1>

          <p className="af-hero-description">
            A smarter way to manage apartment water consumption,
            automated billing and sustainable community living.
          </p>

          <div className="af-showcase">
            <div className="af-building">
              <svg
                className="af-building-art"
                viewBox="0 0 360 320"
                fill="none"
                role="img"
                aria-label="Illustration of a smart apartment connected to a water monitoring network"
              >
                <defs>
                  <linearGradient id="afBuilding" x1="65" y1="40" x2="280" y2="290">
                    <stop stopColor="#F5FDFF" />
                    <stop offset="1" stopColor="#8CC8E9" />
                  </linearGradient>
                  <linearGradient id="afWindow" x1="0" y1="0" x2="1" y2="1">
                    <stop stopColor="#073C6E" />
                    <stop offset="1" stopColor="#20C6E8" />
                  </linearGradient>
                  <filter id="afGlow">
                    <feGaussianBlur stdDeviation="4" result="blur" />
                    <feMerge>
                      <feMergeNode in="blur" />
                      <feMergeNode in="SourceGraphic" />
                    </feMerge>
                  </filter>
                </defs>

                <ellipse cx="175" cy="292" rx="150" ry="17" fill="#00DDF2" opacity=".2" />

                {/* Apartment building */}
                <path d="M68 72 196 24 310 65 310 266 68 266Z"
                  fill="url(#afBuilding)" stroke="#DDF7FF" strokeWidth="2" />
                <path d="M68 72 196 24 310 65 184 112Z"
                  fill="#C8EDFF" stroke="#F1FDFF" strokeWidth="2" />
                <path d="M68 72 184 112 184 266 68 266Z"
                  fill="#74AFCF" opacity=".85" />
                <path d="M184 112 310 65 310 266 184 266Z"
                  fill="#D8F4FF" />

                {/* Floors */}
                {[112, 164, 216].map((y) => (
                  <g key={y}>
                    <path d={`M68 ${y} 184 ${y + 31} 310 ${y - 16}`}
                      stroke="#FFFFFF" strokeWidth="5" />
                    <path d={`M83 ${y + 8} 83 ${y + 45} 168 ${y + 67} 168 ${y + 30}`}
                      fill="url(#afWindow)" stroke="#E8FBFF" strokeWidth="2" />
                    <path d={`M202 ${y + 29} 202 ${y - 4} 294 ${y - 39} 294 ${y - 5}`}
                      fill="url(#afWindow)" stroke="#E8FBFF" strokeWidth="2" />
                  </g>
                ))}

                {/* Water tank */}
                <ellipse cx="212" cy="28" rx="31" ry="9" fill="#22BFF0" />
                <path d="M181 28V8Q212 -5 243 8V28" fill="#087FC5" stroke="#9AF3FF" strokeWidth="2" />
                <ellipse cx="212" cy="8" rx="31" ry="9" fill="#1B9EDB" stroke="#B2F5FF" strokeWidth="2" />
                <path d="M192 12V23M232 12V23" stroke="#80EFFF" strokeWidth="2" />

                {/* Smart water network */}
                <g stroke="#12E9FF" strokeWidth="4" strokeLinecap="round"
                  strokeLinejoin="round" filter="url(#afGlow)">
                  <path d="M212 36V102H177V149H130V201H177V249H218" />
                  <path d="M177 149H273V179" />
                  <path d="M177 201H102V231" />
                </g>

                {/* Sensor points */}
                {[[177, 149], [273, 179], [102, 231], [218, 249]].map(([x, y]) => (
                  <g key={`${x}-${y}`}>
                    <circle cx={x} cy={y} r="11" fill="#03A8DC" stroke="#D9FFFF" strokeWidth="2" />
                    <path d={`M${x} ${y - 5}v10m-4-5h8`}
                      stroke="white" strokeWidth="2" strokeLinecap="round" />
                  </g>
                ))}

                {/* Water drop icon */}
                <path d="M210 72c-5 8-9 12-9 18a9 9 0 0 0 18 0c0-6-4-10-9-18Z"
                  fill="#16D9F6" stroke="white" strokeWidth="1.5" />
                <path d="M206 92a4 4 0 0 0 4 4" stroke="white" strokeWidth="1.5" />

                {/* Base */}
                <path d="M47 267H325L345 286H25Z"
                  fill="#D2F5FF" stroke="white" strokeWidth="2" />
                <path d="M26 286H345V298H26Z" fill="#3D9BC9" />
                <path d="M43 299H327" stroke="#B6F7FF" strokeWidth="3" />
              </svg>

              <div className="af-building-label">
                <WaterDrop />
                <span>AquaFlow Smart Apartment</span>
              </div>
            </div>

            <div className="af-features">
              <div className="af-feature">
                <span className="af-feature-icon"><FeatureIcon type="usage" /></span>
                <span>
                  <strong>Real-time Usage</strong>
                  <small>Track water consumption</small>
                </span>
              </div>

              <div className="af-feature">
                <span className="af-feature-icon"><FeatureIcon type="billing" /></span>
                <span>
                  <strong>Automated Billing</strong>
                  <small>Transparent household bills</small>
                </span>
              </div>

              <div className="af-feature">
                <span className="af-feature-icon"><FeatureIcon type="security" /></span>
                <span>
                  <strong>Leak Detection</strong>
                  <small>Monitor unusual usage</small>
                </span>
              </div>

              <div className="af-feature">
                <span className="af-feature-icon"><FeatureIcon type="ai" /></span>
                <span>
                  <strong>AI Assistant</strong>
                  <small>Your smart water companion</small>
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="af-left-footer">
          <span>Every drop counts. Every drop matters.</span>
          <span>© {new Date().getFullYear()} AquaFlow</span>
        </div>

        <div className="af-water-wave" aria-hidden="true" />
      </section>

      <section className="af-right">
        <div className="af-card">
          <AquaFlowBrand mobile />

          <div className="af-heading">
            <span className="af-heading-eyebrow">AQUAFLOW PORTAL</span>
            <h2>
              {pendingCredential ? 'One Last Step' : 'Welcome Back'}
            </h2>
            <p>
              {pendingCredential
                ? 'Connect your account to your apartment.'
                : 'Sign in to your AquaFlow account'}
            </p>
          </div>

          {location.state?.registered && (
            <div className="af-alert-space">
              <Alert type="success">
                Account created. Please sign in.
              </Alert>
            </div>
          )}

          {error && (
            <div className="af-alert-space">
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
              <div className="af-apartment-note">
                First time signing in with Google? Enter your Apartment ID
                to connect your account. You can get this ID from your
                society administrator.
              </div>

              <label className="field">
                <span className="field-label">Apartment ID</span>
                <div className="af-input-wrap">
                  <input
                    type="number"
                    min="1"
                    required
                    placeholder="Enter your apartment ID"
                    value={apartmentId}
                    onChange={(e) => setApartmentId(e.target.value)}
                  />
                </div>
              </label>

              <div className="af-apartment-actions">
                <button className="af-submit" disabled={busy}>
                  {busy ? 'Connecting...' : 'Continue'}
                </button>
                <button
                  type="button"
                  className="af-cancel"
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
          ) : (
            <>
              <form className="form" onSubmit={submit}>
                <label className="field">
                  <span className="field-label">Email Address</span>
                  <div className="af-input-wrap">
                    <svg className="af-input-icon" viewBox="0 0 24 24"
                      fill="none" stroke="currentColor" strokeWidth="1.8">
                      <rect x="3" y="5" width="18" height="14" rx="2" />
                      <path d="m3 7 9 6 9-6" />
                    </svg>
                    <input
                      type="email"
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
                  <div className="af-input-wrap">
                    <svg className="af-input-icon" viewBox="0 0 24 24"
                      fill="none" stroke="currentColor" strokeWidth="1.8">
                      <rect x="4" y="10" width="16" height="11" rx="2" />
                      <path d="M8 10V7a4 4 0 0 1 8 0v3" />
                      <path d="M12 14v3" />
                    </svg>
                    <input
                      type={showPassword ? 'text' : 'password'}
                      required
                      autoComplete="current-password"
                      placeholder="Enter your password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                    />
                    <button
                      type="button"
                      className="af-password-toggle"
                      onClick={() => setShowPassword((visible) => !visible)}
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                    >
                      {showPassword ? 'Hide' : 'Show'}
                    </button>
                  </div>
                </label>

                <div className="af-forgot-row">
                  <Link
                    to="/forgot-password"
                    className="af-link"
                    onClick={(e) => {
                      // Prevent navigation if the existing route is not configured.
                      // Remove this handler if your app already has this route.
                    }}
                  >
                    Forgot Password?
                  </Link>
                </div>

                <button
                  className="af-submit"
                  type="submit"
                  disabled={busy}
                >
                  {busy ? 'Signing in...' : 'Log In'}
                  {!busy && (
                    <svg viewBox="0 0 24 24" fill="none"
                      stroke="currentColor" strokeWidth="2"
                      strokeLinecap="round" strokeLinejoin="round">
                      <path d="M5 12h14M13 6l6 6-6 6" />
                    </svg>
                  )}
                </button>
              </form>

              <div className="af-divider">Or continue with</div>

              <div className="af-google">
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

          {!pendingCredential && (
            <p className="af-register">
              Don't have an account?
              <Link to="/register" className="af-link">
                Register Now
              </Link>
            </p>
          )}

          <p className="af-secure">
            <svg viewBox="0 0 24 24" fill="none"
              stroke="currentColor" strokeWidth="1.8">
              <path d="M12 3 20 6v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6z" />
              <path d="m9 12 2 2 4-4" />
            </svg>
            Your account details are protected and secure.
          </p>
        </div>
      </section>
    </div>
  );
}
