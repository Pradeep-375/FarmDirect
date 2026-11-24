import axios from 'axios';

const BASE = import.meta.env.VITE_API_URL || '';
export const api = axios.create({ baseURL: BASE + '/api' });
export const assetUrl = (u) => (u && u.startsWith('/uploads') ? BASE + u : u);

const store = {
  get: () => { try { return JSON.parse(localStorage.getItem('fd_tokens')) || null; } catch { return null; } },
  set: (t) => (t ? localStorage.setItem('fd_tokens', JSON.stringify(t)) : localStorage.removeItem('fd_tokens')),
};
export const tokens = store;

api.interceptors.request.use((cfg) => {
  const t = store.get();
  if (t?.accessToken) cfg.headers.Authorization = `Bearer ${t.accessToken}`;
  return cfg;
});

let refreshing = null; // single-flight: concurrent 401s share one refresh call (the server rotates refresh tokens)
api.interceptors.response.use((r) => r, async (err) => {
  const orig = err.config;
  const t = store.get();
  if (err.response?.status === 401 && t?.refreshToken && !orig._retry && !orig.url.startsWith('/auth/')) {
    orig._retry = true;
    try {
      refreshing = refreshing || axios.post(`${BASE}/api/auth/refresh`, { refreshToken: t.refreshToken }).finally(() => { refreshing = null; });
      const { data } = await refreshing;
      store.set(data);
      orig.headers.Authorization = `Bearer ${data.accessToken}`;
      return api(orig);
    } catch (e) {
      store.set(null);
      window.dispatchEvent(new Event('fd:logout'));
    }
  }
  return Promise.reject(err);
});

export const msg = (e) => e?.response?.data?.details
  ? Object.values(e.response.data.details).join('. ')
  : e?.response?.data?.message || 'Something went wrong. Please try again.';
export const inr = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
export const UNIT = { KILOGRAM: 'kg', GRAM: 'g', LITRE: 'L', MILLILITRE: 'mL', PIECE: 'pc', DOZEN: 'dozen', PACKET: 'packet' };
