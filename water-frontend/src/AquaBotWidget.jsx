import { useEffect, useRef, useState } from 'react';
import { post } from './api.js';
import { useAuth } from './auth.jsx';

/**
 * AquaBot chat widget
 * ----------------------------------------------------------------------------
 * UI-only refresh. The behaviour is unchanged:
 *  - Same imports (./api.js, ./auth.jsx) and same default export (AquaBotWidget).
 *  - Still renders nothing unless VITE_AQUABOT_ENABLED === 'true'.
 *  - Same endpoints: /api/v1/assistant/admin/chat for admins and
 *    /api/v1/assistant/chat for residents, with the same { message } body.
 *  - Same response check and the same error-code to message mapping.
 *
 * Added on the client only: suggested questions, Enter-to-send, a "New chat"
 * button, auto-scroll, Escape to close, a character counter and short help text
 * about what AquaBot can and cannot do. No new requests are made.
 *
 * Styling is injected by this file and every selector starts with .abw-root,
 * so no other part of the app is affected. The old global ".aquabot*" classes
 * are no longer used. Colours and type match the public home page.
 */

const enabled = import.meta.env.VITE_AQUABOT_ENABLED === 'true';
const MAX_LENGTH = 2000;

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
  if (ERROR_MESSAGES[error?.code]) return ERROR_MESSAGES[error.code];
  if (error?.status === 401) return 'Your session has expired. Please sign in again.';
  if (error?.status === 403) return 'You are not allowed to use AquaBot.';
  if (error?.status === 404) return 'AquaBot is disabled on the server. Please contact the administrator.';
  if (error?.status === 429) return 'AquaBot is receiving too many requests. Please try again shortly.';
  if (error?.status === 502) return 'AquaBot received an invalid response. Please try again later.';
  if (error?.status === 503) return 'AquaBot is temporarily unavailable. Please try again later.';
  if (error?.status === 504) return 'AquaBot took too long to respond. Please try again.';
  if (error?.status === 0) return 'Cannot reach the server. Please try again when it is available.';
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

/* ------------------------------------------------------------------ */
/*  Content                                                            */
/* ------------------------------------------------------------------ */

const RESIDENT_PROMPTS = [
  'How much water did I use last month?',
  'Why did my last bill go up?',
  'Do I have any open usage alerts?',
  'Give me tips to reduce my water use',
];

const ADMIN_PROMPTS = [
  'Which households used the most water this cycle?',
  'What is the apartment average for this cycle?',
  'How were shared costs allocated?',
  'Summarize the current alerts',
];

const RESIDENT_SCOPE = 'your water usage, finalized bills, billing history, alerts and practical water-saving advice';
const ADMIN_SCOPE = 'apartment usage, household rankings and averages, billing cycles, invoices, alerts, water purchases and shared-cost allocation';

function Icon({ name }) {
  const paths = {
    sparkle: <><path d="m12 3 2.2 6.8L21 12l-6.8 2.2L12 21l-2.2-6.8L3 12l6.8-2.2L12 3Z" /><path d="m19 2 .8 2.2L22 5l-2.2.8L19 8l-.8-2.2L16 5l2.2-.8L19 2Z" /></>,
    close: <path d="M18 6 6 18M6 6l12 12" />,
    send: <><path d="M22 2 11 13" /><path d="m22 2-7 20-4-9-9-4 20-7Z" /></>,
    plus: <path d="M12 5v14M5 12h14" />,
    shield: <><path d="M12 3 20 6v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6l8-3Z" /><path d="m8.5 12 2.3 2.3 4.8-5" /></>,
  };
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name] || null}
    </svg>
  );
}

/* ------------------------------------------------------------------ */
/*  Widget                                                             */
/* ------------------------------------------------------------------ */

export default function AquaBotWidget() {
  const { isAdmin } = useAuth();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);
  const listRef = useRef(null);
  const inputRef = useRef(null);
  const launcherRef = useRef(null);

  // All hooks stay above the early return below.
  useEffect(() => {
    if (open && inputRef.current) inputRef.current.focus();
  }, [open]);

  useEffect(() => {
    const list = listRef.current;
    if (list) list.scrollTop = list.scrollHeight;
  }, [messages, sending, error, open]);

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

  const onKeyDown = (event) => {
    // Enter sends, Shift+Enter adds a new line.
    if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent?.isComposing) {
      send(event);
    }
  };

  const closePanel = () => {
    setOpen(false);
    if (launcherRef.current) launcherRef.current.focus();
  };

  const startNewChat = () => {
    if (sending) return;
    setMessages([]);
    setMessage('');
    setError('');
    if (inputRef.current) inputRef.current.focus();
  };

  const usePrompt = (prompt) => {
    setMessage(prompt);
    setError('');
    if (inputRef.current) inputRef.current.focus();
  };

  const prompts = isAdmin ? ADMIN_PROMPTS : RESIDENT_PROMPTS;

  return (
    <div className="abw-root">
      <style>{STYLES}</style>

      {open && (
        <section
          className="abw-panel"
          aria-label="AquaBot chat"
          onKeyDown={(event) => { if (event.key === 'Escape') closePanel(); }}
        >
          <header className="abw-header">
            <span className="abw-avatar"><Icon name="sparkle" /></span>
            <div className="abw-title">
              <strong>AquaBot</strong>
              <span>Water intelligence assistant · {isAdmin ? 'Administrator view' : 'Resident view'}</span>
            </div>
            {messages.length > 0 && (
              <button type="button" className="abw-icon-btn" onClick={startNewChat} disabled={sending} aria-label="Start a new chat" title="New chat">
                <Icon name="plus" />
              </button>
            )}
            <button type="button" className="abw-icon-btn" onClick={closePanel} aria-label="Close AquaBot" title="Close">
              <Icon name="close" />
            </button>
          </header>

          <div className="abw-messages" ref={listRef} aria-live="polite">
            {messages.length === 0 && (
              <div className="abw-welcome">
                <h2>How can I help?</h2>
                <p>
                  {isAdmin
                    ? 'Ask about apartment consumption, billing cycles, invoices, alerts, procurement, or shared costs.'
                    : 'Ask about your water usage, finalized bills, billing history, or alerts. AquaBot uses only records linked to your resident account.'}
                </p>

                <h3>Try asking</h3>
                <ul className="abw-prompts">
                  {prompts.map((prompt) => (
                    <li key={prompt}>
                      <button type="button" onClick={() => usePrompt(prompt)}>{prompt}</button>
                    </li>
                  ))}
                </ul>

                <details className="abw-details">
                  <summary>What can AquaBot answer?</summary>
                  <p>It can answer questions about {isAdmin ? ADMIN_SCOPE : RESIDENT_SCOPE}.</p>
                </details>
                <details className="abw-details">
                  <summary>Privacy and limits</summary>
                  <ul>
                    <li><Icon name="shield" />Read-only: it cannot change any record.</li>
                    <li><Icon name="shield" />It answers only from records your role can access.</li>
                    <li><Icon name="shield" />It never shows another apartment’s data.</li>
                    <li><Icon name="shield" />Answers can be wrong. Check important figures against your invoices.</li>
                  </ul>
                </details>
              </div>
            )}

            {messages.map((item, index) => (
              <p key={`${item.role}-${index}`} className={`abw-msg abw-msg--${item.role}`}>
                <span className="abw-sr">{item.role === 'assistant' ? 'AquaBot said: ' : 'You said: '}</span>
                {item.role === 'assistant' ? renderAnswer(item.text) : item.text}
              </p>
            ))}

            {sending && (
              <p className="abw-msg abw-msg--assistant abw-typing">
                AquaBot is analyzing records
                <span className="abw-dots" aria-hidden="true"><i /><i /><i /></span>
              </p>
            )}

            {error && <p className="abw-error" role="alert">{error}</p>}
          </div>

          <form className="abw-form" onSubmit={send}>
            <label className="abw-sr" htmlFor="aquabot-message">Message AquaBot</label>
            <div className="abw-compose">
              <textarea
                id="aquabot-message"
                ref={inputRef}
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                onKeyDown={onKeyDown}
                placeholder="Ask about water usage, bills, or alerts..."
                maxLength={MAX_LENGTH}
                rows={2}
                required
              />
              <button type="submit" className="abw-send" disabled={sending || !message.trim()} aria-label="Send message">
                {sending ? '…' : <Icon name="send" />}
              </button>
            </div>
            <div className="abw-foot">
              <span>Enter to send, Shift+Enter for a new line</span>
              {message.length > 1500 && <span className="abw-count">{message.length}/{MAX_LENGTH}</span>}
            </div>
          </form>
        </section>
      )}

      <button
        type="button"
        ref={launcherRef}
        className="abw-launcher"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        aria-label={open ? 'Close AquaBot chat' : 'Open AquaBot chat'}
      >
        <Icon name={open ? 'close' : 'sparkle'} />
        <span>{open ? 'Close' : 'Ask AquaBot'}</span>
      </button>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Styles: every selector starts with .abw-root                       */
/* ------------------------------------------------------------------ */

const STYLES = `
.abw-root {
  --abw-ink: #0b2540;
  --abw-text: #35506a;
  --abw-muted: #5b7186;
  --abw-tide: #0a6ea8;
  --abw-deep: #07496f;
  --abw-foam: #e8f3f9;
  --abw-paper: #f5f9fb;
  --abw-line: #d8e5ed;
  --abw-brass: #a9681d;
  --abw-red: #b42318;
  --abw-red-soft: #fdecea;
  --abw-display: "Iowan Old Style", "Palatino Linotype", Palatino, "Book Antiqua", Georgia, serif;
  position: fixed;
  right: 20px;
  bottom: 20px;
  bottom: max(20px, env(safe-area-inset-bottom));
  z-index: 900;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  font-family: inherit;
  color: var(--abw-ink);
  line-height: 1.5;
}
.abw-root *, .abw-root *::before, .abw-root *::after { box-sizing: border-box; }
.abw-root :where(h2, h3, p, ul) { margin: 0; }
.abw-root :where(ul) { padding: 0; list-style: none; }
.abw-root svg { display: block; flex: none; }
.abw-root button:focus-visible, .abw-root summary:focus-visible, .abw-root textarea:focus-visible { outline: 3px solid rgba(10, 110, 168, .5); outline-offset: 2px; }
.abw-root .abw-sr { position: absolute; width: 1px; height: 1px; margin: -1px; padding: 0; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; border: 0; }

/* Launcher */
.abw-root .abw-launcher { display: inline-flex; align-items: center; gap: 9px; min-height: 52px; padding: 0 20px 0 16px; border: 0; border-radius: 999px; background: var(--abw-tide); color: #fff; font: inherit; font-size: 15px; font-weight: 700; cursor: pointer; box-shadow: 0 14px 30px -10px rgba(7, 73, 111, .55); transition: background .2s, box-shadow .2s; }
.abw-root .abw-launcher:hover { background: var(--abw-deep); }
.abw-root .abw-launcher svg { width: 22px; height: 22px; }

/* Panel */
.abw-root .abw-panel { width: 400px; max-width: calc(100vw - 24px); height: 600px; max-height: calc(100vh - 110px); max-height: calc(100dvh - 110px); display: flex; flex-direction: column; overflow: hidden; border: 1px solid var(--abw-line); border-radius: 18px; background: #fff; box-shadow: 0 30px 70px -24px rgba(11, 37, 64, .5); }
.abw-root .abw-header { display: flex; align-items: center; gap: 10px; padding: 14px 14px 14px 16px; background: var(--abw-ink); color: #fff; }
.abw-root .abw-avatar { width: 38px; height: 38px; display: grid; place-items: center; border-radius: 10px; background: rgba(255, 255, 255, .12); color: #9bdcf5; }
.abw-root .abw-avatar svg { width: 21px; height: 21px; }
.abw-root .abw-title { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.abw-root .abw-title strong { font-family: var(--abw-display); font-size: 19px; font-weight: 600; line-height: 1.2; }
.abw-root .abw-title span { color: rgba(255, 255, 255, .72); font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.abw-root .abw-icon-btn { width: 34px; height: 34px; display: grid; place-items: center; padding: 0; border: 0; border-radius: 8px; background: transparent; color: #fff; cursor: pointer; }
.abw-root .abw-icon-btn:hover:not(:disabled) { background: rgba(255, 255, 255, .14); }
.abw-root .abw-icon-btn:disabled { opacity: .5; cursor: not-allowed; }
.abw-root .abw-icon-btn svg { width: 18px; height: 18px; }

/* Messages */
.abw-root .abw-messages { flex: 1; min-height: 0; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 10px; background: var(--abw-paper); overscroll-behavior: contain; }
.abw-root .abw-welcome h2 { font-family: var(--abw-display); font-size: 24px; font-weight: 600; letter-spacing: -.01em; color: var(--abw-ink); }
.abw-root .abw-welcome > p { margin-top: 6px; color: var(--abw-text); font-size: 14px; line-height: 1.65; }
.abw-root .abw-welcome h3 { margin-top: 18px; color: var(--abw-ink); font-size: 13px; font-weight: 700; }
.abw-root .abw-prompts { margin-top: 8px; display: grid; gap: 8px; }
.abw-root .abw-prompts button { width: 100%; padding: 10px 12px; border: 1.5px solid var(--abw-line); border-radius: 10px; background: #fff; color: var(--abw-ink); font: inherit; font-size: 13.5px; text-align: left; cursor: pointer; transition: border-color .2s, background .2s; }
.abw-root .abw-prompts button:hover { border-color: var(--abw-tide); background: var(--abw-foam); }
.abw-root .abw-details { margin-top: 12px; border-top: 1px solid var(--abw-line); }
.abw-root .abw-details + .abw-details { margin-top: 0; border-bottom: 1px solid var(--abw-line); }
.abw-root .abw-details summary { position: relative; padding: 11px 26px 11px 0; color: var(--abw-ink); font-size: 13.5px; font-weight: 700; cursor: pointer; list-style: none; }
.abw-root .abw-details summary::-webkit-details-marker { display: none; }
.abw-root .abw-details summary::after { content: "+"; position: absolute; right: 2px; top: 50%; transform: translateY(-50%); color: var(--abw-tide); font-size: 20px; font-weight: 400; line-height: 1; }
.abw-root .abw-details[open] summary::after { content: "\\2212"; }
.abw-root .abw-details p { padding: 0 0 12px; color: var(--abw-text); font-size: 13.5px; line-height: 1.65; }
.abw-root .abw-details ul { padding-bottom: 12px; display: grid; gap: 8px; }
.abw-root .abw-details li { display: flex; align-items: flex-start; gap: 8px; color: var(--abw-text); font-size: 13.5px; line-height: 1.55; }
.abw-root .abw-details li svg { width: 16px; height: 16px; margin-top: 2px; color: #17795c; }
.abw-root .abw-msg { max-width: 88%; padding: 10px 14px; border-radius: 14px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; overflow-wrap: anywhere; }
.abw-root .abw-msg--user { align-self: flex-end; border-bottom-right-radius: 4px; background: var(--abw-tide); color: #fff; }
.abw-root .abw-msg--assistant { align-self: flex-start; border-bottom-left-radius: 4px; border: 1px solid var(--abw-line); background: #fff; color: var(--abw-text); }
.abw-root .abw-msg--assistant strong { color: var(--abw-ink); }
.abw-root .abw-typing { color: var(--abw-muted); font-style: italic; }
.abw-root .abw-dots { display: inline-flex; gap: 3px; margin-left: 4px; }
.abw-root .abw-dots i { width: 4px; height: 4px; border-radius: 50%; background: currentColor; animation: abw-blink 1.2s infinite ease-in-out; }
.abw-root .abw-dots i:nth-child(2) { animation-delay: .2s; }
.abw-root .abw-dots i:nth-child(3) { animation-delay: .4s; }
@keyframes abw-blink { 0%, 80%, 100% { opacity: .25; } 40% { opacity: 1; } }
.abw-root .abw-error { padding: 10px 12px; border: 1px solid #f3c1bb; border-radius: 10px; background: var(--abw-red-soft); color: var(--abw-red); font-size: 13.5px; line-height: 1.55; }

/* Composer */
.abw-root .abw-form { padding: 12px 14px 10px; border-top: 1px solid var(--abw-line); background: #fff; }
.abw-root .abw-compose { display: flex; align-items: flex-end; gap: 8px; }
.abw-root .abw-compose textarea { flex: 1; min-width: 0; max-height: 120px; padding: 10px 12px; border: 1.5px solid var(--abw-line); border-radius: 10px; background: #fff; color: var(--abw-ink); font: inherit; font-size: 14px; line-height: 1.5; resize: none; outline: none; transition: border-color .2s, box-shadow .2s; }
.abw-root .abw-compose textarea::placeholder { color: #8aa0b5; }
.abw-root .abw-compose textarea:focus { border-color: var(--abw-tide); box-shadow: 0 0 0 3px rgba(10, 110, 168, .14); }
.abw-root .abw-send { width: 44px; height: 44px; display: grid; place-items: center; padding: 0; border: 0; border-radius: 10px; background: var(--abw-tide); color: #fff; font: inherit; font-size: 18px; cursor: pointer; transition: background .2s, opacity .2s; }
.abw-root .abw-send:hover:not(:disabled) { background: var(--abw-deep); }
.abw-root .abw-send:disabled { opacity: .5; cursor: not-allowed; }
.abw-root .abw-send svg { width: 19px; height: 19px; }
.abw-root .abw-foot { display: flex; justify-content: space-between; gap: 8px; margin-top: 7px; color: var(--abw-muted); font-size: 11.5px; }
.abw-root .abw-count { color: var(--abw-brass); font-weight: 700; }

/* Small screens: the panel becomes a bottom sheet */
@media (max-width: 520px) {
  .abw-root { right: 12px; left: 12px; bottom: 12px; bottom: max(12px, env(safe-area-inset-bottom)); }
  .abw-root .abw-panel { width: 100%; max-width: none; height: 640px; max-height: calc(100vh - 88px); max-height: calc(100dvh - 88px); }
  .abw-root .abw-launcher { min-height: 48px; padding: 0 16px 0 14px; }
  .abw-root .abw-msg { max-width: 92%; }
  .abw-root .abw-compose textarea { font-size: 16px; } /* avoids iOS zoom on focus */
}
@media (prefers-reduced-motion: reduce) {
  .abw-root *, .abw-root *::before, .abw-root *::after { animation: none !important; transition: none !important; }
}
`;