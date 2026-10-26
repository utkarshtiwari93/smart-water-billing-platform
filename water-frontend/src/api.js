const BASE = import.meta.env.VITE_API_BASE || '';
const TOKEN_KEY = 'wb_token';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export class ApiError extends Error {
  constructor(status, message, fieldErrors) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors || null;
  }
}

// Turns an ApiError into one readable line (includes field-level validation errors).
export function errText(e) {
  if (!e) return '';
  if (e.fieldErrors && Object.keys(e.fieldErrors).length) {
    return Object.entries(e.fieldErrors).map(([k, v]) => `${k}: ${v}`).join(' | ');
  }
  return e.message || 'Something went wrong';
}

let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

export async function api(path, { method = 'GET', body, form, auth = true } = {}) {
  const headers = {};
  const token = tokenStore.get();
  if (auth && token) headers.Authorization = `Bearer ${token}`;

  let payload;
  if (form) {
    payload = form;                       // browser sets the multipart boundary
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch(BASE + path, { method, headers, body: payload });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the backend running on port 8080?');
  }

  if (res.status === 204) return null;

  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }

  if (!res.ok) {
    if (res.status === 401 && auth && token) onUnauthorized();
    const fallback = res.status === 401 ? 'Please sign in again' :
      res.status === 403 ? 'You do not have permission to do this' : `Request failed (${res.status})`;
    throw new ApiError(res.status, (data && data.message) || fallback, data && data.fieldErrors);
  }
  return data;
}

export const get = (p) => api(p);
export const post = (p, body) => api(p, { method: 'POST', body });
export const put = (p, body) => api(p, { method: 'PUT', body });
export const del = (p) => api(p, { method: 'DELETE' });
