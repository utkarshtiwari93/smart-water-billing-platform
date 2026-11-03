import { useEffect, useRef, useState } from 'react';

const CLIENT_ID = (import.meta.env.VITE_GOOGLE_CLIENT_ID || '').trim();

// Renders Google's official "Sign in with Google" button. It returns an ID token (credential),
// which the backend verifies at POST /api/v1/auth/google.
export default function GoogleButton({ onCredential, onUnavailable }) {
  const ref = useRef(null);
  const callback = useRef(onCredential);
  const [loadError, setLoadError] = useState('');
  callback.current = onCredential;

  useEffect(() => {
    if (!CLIENT_ID) return undefined;

    let initialized = false;
    const render = () => {
      if (!window.google?.accounts?.id || !ref.current) return;
      if (!initialized) {
        window.google.accounts.id.initialize({
          client_id: CLIENT_ID,
          callback: (res) => callback.current(res.credential),
        });
        initialized = true;
      }
      ref.current.replaceChildren();
      window.google.accounts.id.renderButton(ref.current, {
        theme: 'outline',
        size: 'large',
        width: Math.min(400, Math.floor(ref.current.getBoundingClientRect().width)),
        text: 'continue_with',
      });
    };

    const handleLoad = () => {
      if (!window.google?.accounts?.id) {
        setLoadError('Google sign-in could not be loaded. Check your connection and try again.');
        return;
      }
      setLoadError('');
      render();
    };
    const handleError = () => setLoadError('Google sign-in could not be loaded. Check your connection and try again.');

    let observer;
    if (window.google?.accounts?.id) {
      render();
    } else {
      let script = document.querySelector('script[data-gsi]');
      if (!script) {
        script = document.createElement('script');
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        script.dataset.gsi = '1';
        document.head.appendChild(script);
      }
      script.addEventListener('load', handleLoad);
      script.addEventListener('error', handleError);
    }

    if (window.ResizeObserver && ref.current) {
      observer = new ResizeObserver(render);
      observer.observe(ref.current);
    }

    const script = document.querySelector('script[data-gsi]');
    return () => {
      observer?.disconnect();
      script?.removeEventListener('load', handleLoad);
      script?.removeEventListener('error', handleError);
    };
  }, []);

  if (!CLIENT_ID) {
    return (
      <div className="google-fallback-wrap">
        <button type="button" className="google-fallback" onClick={onUnavailable}>
          <svg className="google-logo" viewBox="0 0 48 48" aria-hidden="true">
            <path fill="#4285F4" d="M43.6 24.5c0-1.6-.1-3.1-.4-4.5H24v8.5h11c-.5 2.7-2 5-4.4 6.5v5.3h7.1c4.1-3.8 6.4-9.3 6.4-15.8z" />
            <path fill="#34A853" d="M24 44c5.9 0 10.8-2 14.4-5.5l-7.1-5.3c-2 1.3-4.4 2.1-7.3 2.1-5.6 0-10.3-3.8-12-8.9H4.7v5.5C8.3 39.1 15.6 44 24 44z" />
            <path fill="#FBBC05" d="M12 26.4a12 12 0 0 1 0-4.8v-5.5H4.7a20 20 0 0 0 0 15.8z" />
            <path fill="#EA4335" d="M24 12.7c3.2 0 6 1.1 8.2 3.2l6.2-6.2C34.7 6.1 29.9 4 24 4 15.6 4 8.3 8.9 4.7 16.1l7.3 5.5c1.7-5.1 6.4-8.9 12-8.9z" />
          </svg>
          <span>Continue with Google</span>
        </button>
        <p className="muted small center">Google sign-in needs to be configured by your administrator.</p>
      </div>
    );
  }
  return (
    <div>
      <div ref={ref} className="google-btn" />
      {loadError && <p className="google-error" role="alert">{loadError}</p>}
    </div>
  );
}
