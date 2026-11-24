import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { assetUrl } from '../api';

export function Spinner({ label = 'Loading' }) {
  return <div role="status" className="flex items-center justify-center gap-2 p-8 text-field-700"><span className="h-5 w-5 animate-spin rounded-full border-2 border-field-200 border-t-field-700" />{label}…</div>;
}
export const Empty = ({ title, children }) => (
  <div className="rounded-xl border border-dashed border-field-200 bg-field-50 p-8 text-center"><p className="font-display text-lg text-field-900">{title}</p>{children && <div className="mt-2 text-sm text-stone-600">{children}</div>}</div>
);
export const ErrorBox = ({ error }) => error ? <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null;
export const Badge = ({ tone = 'green', children }) => {
  const t = { green: 'bg-field-100 text-field-800', amber: 'bg-wheat-100 text-wheat-700', red: 'bg-red-100 text-red-800', gray: 'bg-stone-100 text-stone-700', blue: 'bg-sky-100 text-sky-800' }[tone];
  return <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${t}`}>{children}</span>;
};
export const statusTone = (s) => ({ DELIVERED: 'green', FARMER_ACCEPTED: 'blue', PROCESSING: 'blue', READY_FOR_PICKUP: 'amber', PICKED_UP: 'amber', OUT_FOR_DELIVERY: 'amber', ORDER_PLACED: 'gray', CANCELLED: 'red', FARMER_REJECTED: 'red', REFUNDED: 'gray', RETURN_REQUESTED: 'amber', APPROVED: 'green', PENDING: 'amber', REJECTED: 'red', ACTIVE: 'green', SUSPENDED: 'red', PENDING_VERIFICATION: 'amber' }[s] || 'gray');
export const StatusBadge = ({ s }) => <Badge tone={statusTone(s)}>{String(s).replaceAll('_', ' ').toLowerCase()}</Badge>;

export function Field({ label, error, hint, children, className = '' }) {
  return <label className={`block ${className}`}><span className="mb-1 block text-sm font-medium text-field-900">{label}</span>{children}{hint && <span className="mt-1 block text-xs text-stone-500">{hint}</span>}{error && <span role="alert" className="mt-1 block text-xs text-red-700">{error}</span>}</label>;
}
export const Input = ({ label, error, hint, className, ...p }) => <Field label={label} error={error} hint={hint} className={className}><input className="input" {...p} /></Field>;
export const Select = ({ label, error, children, className, ...p }) => <Field label={label} error={error} className={className}><select className="input" {...p}>{children}</select></Field>;

export const Img = ({ src, alt, className = '' }) => src
  ? <img src={assetUrl(src)} alt={alt} loading="lazy" className={`object-cover ${className}`} />
  : <div aria-hidden className={`flex items-center justify-center bg-field-100 font-display text-3xl text-field-400 ${className}`}>{(alt || '?')[0]}</div>;

export function Pager({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return <div className="mt-6 flex items-center justify-center gap-3"><button className="btn-outline" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</button><span className="text-sm">Page {page + 1} of {totalPages}</span><button className="btn-outline" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button></div>;
}

export function Modal({ title, onClose, children }) {
  useEffect(() => { const h = (e) => e.key === 'Escape' && onClose(); window.addEventListener('keydown', h); return () => window.removeEventListener('keydown', h); }, [onClose]);
  return <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-0 md:items-center md:p-4" onClick={onClose}>
    <div role="dialog" aria-modal="true" aria-label={title} className="max-h-[90vh] w-full overflow-y-auto rounded-t-2xl bg-white p-5 md:max-w-lg md:rounded-2xl" onClick={(e) => e.stopPropagation()}>
      <div className="mb-3 flex items-center justify-between"><h2>{title}</h2><button className="btn-outline" onClick={onClose} aria-label="Close">✕</button></div>{children}</div></div>;
}

const ToastCtx = createContext(() => {});
export const useToast = () => useContext(ToastCtx);
export function ToastProvider({ children }) {
  const [t, setT] = useState(null);
  const show = useCallback((text, tone = 'ok') => { setT({ text, tone }); setTimeout(() => setT(null), 3500); }, []);
  return <ToastCtx.Provider value={show}>{children}{t && <div role="status" className={`fixed bottom-20 left-1/2 z-50 -translate-x-1/2 rounded-lg px-4 py-2 text-sm text-white shadow-lg md:bottom-6 ${t.tone === 'err' ? 'bg-red-700' : 'bg-field-800'}`}>{t.text}</div>}</ToastCtx.Provider>;
}

export function useFetch(fn, deps = []) {
  const [state, set] = useState({ data: null, loading: true, error: null });
  const [tick, setTick] = useState(0);
  useEffect(() => {
    let live = true; set((s) => ({ ...s, loading: true }));
    fn().then((r) => live && set({ data: r.data ?? r, loading: false, error: null }))
      .catch((e) => live && set({ data: null, loading: false, error: e?.response?.data?.message || 'Could not load data' }));
    return () => { live = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);
  return { ...state, reload: () => setTick((x) => x + 1) };
}
