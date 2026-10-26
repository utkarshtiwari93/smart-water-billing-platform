import { useEffect, useRef } from 'react';

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

// Renders Google's official "Sign in with Google" button. It returns an ID token (credential),
// which the backend verifies at POST /api/v1/auth/google.
export default function GoogleButton({ onCredential }) {
  const ref = useRef(null);
  const callback = useRef(onCredential);
  callback.current = onCredential;

  useEffect(() => {
    if (!CLIENT_ID) return undefined;

    const init = () => {
      if (!window.google?.accounts?.id || !ref.current) return;
      window.google.accounts.id.initialize({
        client_id: CLIENT_ID,
        callback: (res) => callback.current(res.credential),
      });
      window.google.accounts.id.renderButton(ref.current, {
        theme: 'outline', size: 'large', width: 320, text: 'continue_with',
      });
    };

    if (window.google?.accounts?.id) {
      init();
      return undefined;
    }

    let script = document.querySelector('script[data-gsi]');
    if (!script) {
      script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.dataset.gsi = '1';
      document.head.appendChild(script);
    }
    script.addEventListener('load', init);
    return () => script.removeEventListener('load', init);
  }, []);

  if (!CLIENT_ID) {
    return <p className="muted small">Google sign-in is off. Set VITE_GOOGLE_CLIENT_ID in .env to enable it.</p>;
  }
  return <div ref={ref} className="google-btn" />;
}
