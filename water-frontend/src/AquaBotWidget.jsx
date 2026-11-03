import { useState } from 'react';
import { post } from './api.js';
import { useAuth } from './auth.jsx';

const enabled = import.meta.env.VITE_AQUABOT_ENABLED === 'true';

const ERROR_MESSAGES = {
  PROVIDER_NOT_CONFIGURED: 'AquaBot is not enabled on the server. Please contact the administrator.',
  PROVIDER_CONFIGURATION_MISSING: 'AquaBot is not fully configured. Please contact the administrator.',
  PROVIDER_AUTHENTICATION_FAILED: 'AquaBot could not authenticate with its AI provider. Please contact the administrator.',
  PROVIDER_MODEL_UNAVAILABLE: "AquaBot's configured model is unavailable. Please contact the administrator.",
  PROVIDER_RATE_LIMITED: 'AquaBot is receiving too many requests. Please try again shortly.',
  PROVIDER_UPSTREAM_UNAVAILABLE: 'The AquaBot AI provider is temporarily unavailable. Please try again later.',
  PROVIDER_CONNECTION_FAILED: 'AquaBot could not connect to its AI provider. Please try again later.',
  PROVIDER_TIMEOUT: 'AquaBot took too long to respond. Please try again.',
  PROVIDER_INVALID_RESPONSE: 'AquaBot received an invalid response. Please try again.',
  PROVIDER_REQUEST_REJECTED: 'The AI provider could not process this request. Please try again.',
  INTERNAL_ERROR: 'AquaBot encountered an internal error. Please try again later.',
};

function getErrorMessage(error) {
  if (ERROR_MESSAGES[error.code]) return ERROR_MESSAGES[error.code];
  if (error.status === 401) return 'Your session has expired. Please sign in again.';
  if (error.status === 403) return 'You are not allowed to use AquaBot.';
  if (error.status === 404) return 'AquaBot is disabled on the server. Please contact the administrator.';
  if (error.status === 429) return 'AquaBot is receiving too many requests. Please try again shortly.';
  if (error.status === 502) return 'AquaBot received an invalid response. Please try again later.';
  if (error.status === 503) return 'AquaBot is temporarily unavailable. Please try again later.';
  if (error.status === 504) return 'AquaBot took too long to respond. Please try again.';
  if (error.status === 0) return 'Cannot reach the server. Please try again when it is available.';
  return 'AquaBot could not answer right now. Please try again later.';
}

function renderAnswer(text) {
  return text.split(/(\*\*[^*\n]+\*\*)/g).map((part, index) => {
    const bold = part.match(/^\*\*(.+)\*\*$/);
    return bold
      ? <strong key={index}>{bold[1]}</strong>
      : part;
  });
}

export default function AquaBotWidget() {
  const { isAdmin } = useAuth();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);

  if (!enabled) return null;

  const send = async (event) => {
    event.preventDefault();
    const text = message.trim();
    if (!text || sending) return;

    setSending(true);
    setError('');
    try {
      const endpoint = isAdmin
        ? '/api/v1/assistant/admin/chat'
        : '/api/v1/assistant/chat';
      const response = await post(endpoint, { message: text });
      if (typeof response?.answer !== 'string' || !response.answer.trim()) {
        throw new Error('AquaBot returned an invalid response.');
      }
      setMessages((current) => [
        ...current,
        { role: 'user', text },
        { role: 'assistant', text: response.answer },
      ]);
      setMessage('');
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="aquabot">
      {open && (
        <section className="aquabot-panel" aria-label="AquaBot chat">
          <header className="aquabot-header">
            <div>
              <strong>AquaBot AI</strong>
              <span>Water Intelligence Assistant</span>
            </div>
            <button type="button" className="aquabot-close" onClick={() => setOpen(false)} aria-label="Close AquaBot">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M18 6 6 18M6 6l12 12" />
              </svg>
            </button>
          </header>
          <div className="aquabot-messages" aria-live="polite">
            {messages.length === 0 && (
              <p className="aquabot-intro">
                {isAdmin
                  ? 'Ask about apartment consumption, billing cycles, invoices, alerts, procurement, or shared costs.'
                  : 'Ask about your water usage, finalized bills, billing history, or alerts. AquaBot uses only records linked to your resident account.'}
              </p>
            )}
            {messages.map((item, index) => (
              <p key={`${item.role}-${index}`} className={`aquabot-message aquabot-message-${item.role}`}>
                {item.role === 'assistant' ? renderAnswer(item.text) : item.text}
              </p>
            ))}
            {sending && (
              <p className="aquabot-message aquabot-message-assistant" style={{ fontStyle: 'italic', opacity: 0.8 }}>
                AquaBot is analyzing records...
              </p>
            )}
            {error && <p className="aquabot-error" role="alert">{error}</p>}
          </div>
          <form className="aquabot-form" onSubmit={send}>
            <label className="aquabot-sr-only" htmlFor="aquabot-message">Message AquaBot</label>
            <textarea
              id="aquabot-message"
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              placeholder="Ask about water usage, bills, or alerts..."
              maxLength={2000}
              rows={2}
              required
            />
            <button type="submit" disabled={sending || !message.trim()} aria-label="Send message">
              {sending ? '...' : (
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M22 2L11 13" />
                  <polygon points="22 2 15 22 11 13 2 9 22 2" />
                </svg>
              )}
            </button>
          </form>
        </section>
      )}
      <button
        type="button"
        className="aquabot-launcher"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        aria-label={open ? 'Close AquaBot chat' : 'Open AquaBot chat'}
      >
        {open ? (
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M18 6 6 18M6 6l12 12" />
          </svg>
        ) : (
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 2C6.48 2 2 6.48 2 12a9.93 9.93 0 0 0 1.46 5.17L2 22l4.83-1.46A9.93 9.93 0 0 0 12 22c5.52 0 10-4.48 10-10S17.52 2 12 2z" />
            <path d="M8 12h.01M12 12h.01M16 12h.01" strokeWidth="3" />
          </svg>
        )}
      </button>
    </div>
  );
}
