import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { LANGUAGES } from '../i18n';
import { api } from '../api';
import { HOME, useAuth } from '../auth';
import { useStomp } from './useStomp';

function Bell() {
  const [items, setItems] = useState([]); const [open, setOpen] = useState(false);
  const load = () => api.get('/notifications').then((r) => setItems(r.data)).catch(() => {});
  useEffect(() => { load(); }, []);
  useStomp('/user/queue/notifications', load);
  const unread = items.filter((n) => !n.read).length;
  const toggle = async () => { setOpen(!open); if (!open && unread) { await api.post('/notifications/read-all').catch(() => {}); setTimeout(load, 400); } };
  return (
    <div className="relative">
      <button className="btn-outline relative !px-3" onClick={toggle} aria-label={`Notifications, ${unread} unread`}>🔔{unread > 0 && <span className="absolute -right-1 -top-1 rounded-full bg-red-600 px-1.5 text-xs text-white">{unread}</span>}</button>
      {open && <div className="absolute right-0 z-40 mt-2 max-h-96 w-[85vw] max-w-sm overflow-y-auto rounded-xl border border-stone-200 bg-white p-2 shadow-xl">
        {items.length === 0 ? <p className="p-4 text-sm text-stone-500">No notifications yet.</p> : items.map((n) => <div key={n.id} className="border-b border-stone-100 p-2 last:border-0"><p className="text-sm font-semibold">{n.title}</p><p className="text-xs text-stone-600">{n.message}</p></div>)}
      </div>}
    </div>
  );
}

const linkCls = ({ isActive }) => `rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-field-100 text-field-900' : 'text-stone-700 hover:bg-field-50'}`;

export function Navbar() {
  const { t, i18n } = useTranslation(); const { user, logout } = useAuth(); const nav = useNavigate();
  const [open, setOpen] = useState(false); const [count, setCount] = useState(0);
  const loadCount = () => user?.role === 'CUSTOMER' && api.get('/customer/cart').then((r) => setCount(r.data.items.length)).catch(() => {});
  useEffect(() => { loadCount(); window.addEventListener('fd:cart', loadCount); return () => window.removeEventListener('fd:cart', loadCount); }, [user]); // eslint-disable-line
  const links = [['/', t('home')], ['/products', t('products')], ['/farmers', t('farmers')], ['/about', t('about')], ['/contact', t('contact')]];
  const out = async () => { await logout(); nav('/'); setOpen(false); };
  return (
    <header className="sticky top-0 z-30 border-b border-stone-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center gap-2 px-3 py-2 md:px-6">
        <Link to="/" className="mr-2 font-display text-xl font-bold text-field-800">FarmDirect</Link>
        <nav className="hidden flex-1 items-center gap-1 md:flex" aria-label="Main">{links.map(([to, l]) => <NavLink key={to} to={to} end={to === '/'} className={linkCls}>{l}</NavLink>)}</nav>
        <div className="ml-auto flex items-center gap-2">
          <select aria-label={t('language')} className="input !min-h-[40px] !w-auto !py-1 text-sm" value={i18n.language} onChange={(e) => i18n.changeLanguage(e.target.value)}>{LANGUAGES.map(([c, n]) => <option key={c} value={c}>{n}</option>)}</select>
          {user?.role === 'CUSTOMER' && <Link to="/customer/cart" className="btn-outline relative !px-3" aria-label={t('cart')}>🛒{count > 0 && <span className="absolute -right-1 -top-1 rounded-full bg-field-700 px-1.5 text-xs text-white">{count}</span>}</Link>}
          {user && <Bell />}
          {user ? <><Link to={HOME[user.role]} className="btn-primary hidden md:inline-flex">{t('dashboard')}</Link><button className="btn-outline hidden md:inline-flex" onClick={out}>{t('logout')}</button></>
            : <><Link to="/login" className="btn-outline hidden sm:inline-flex">{t('login')}</Link><Link to="/register" className="btn-primary hidden sm:inline-flex">{t('register')}</Link></>}
          <button className="btn-outline md:hidden" aria-expanded={open} aria-label="Menu" onClick={() => setOpen(!open)}>{open ? '✕' : '☰'}</button>
        </div>
      </div>
      {open && <nav className="flex flex-col gap-1 border-t border-stone-200 p-3 md:hidden" aria-label="Mobile" onClick={() => setOpen(false)}>
        {links.map(([to, l]) => <NavLink key={to} to={to} end={to === '/'} className={linkCls}>{l}</NavLink>)}
        {user ? <><NavLink to={HOME[user.role]} className={linkCls}>{t('dashboard')}</NavLink><button className="rounded-lg px-3 py-2 text-left text-sm" onClick={out}>{t('logout')}</button></>
          : <><NavLink to="/login" className={linkCls}>{t('login')}</NavLink><NavLink to="/register" className={linkCls}>{t('register')}</NavLink></>}
      </nav>}
    </header>
  );
}

const BOTTOM = {
  CUSTOMER: [['/', '🏠', 'Home'], ['/products', '🥬', 'Shop'], ['/customer/cart', '🛒', 'Cart'], ['/customer/orders', '📦', 'Orders'], ['/customer/profile', '👤', 'Me']],
  FARMER: [['/farmer', '📊', 'Home'], ['/farmer/products/new', '➕', 'Add'], ['/farmer/products', '📋', 'Stock'], ['/farmer/orders', '📦', 'Orders'], ['/farmer/earnings', '💰', 'Sales']],
  DELIVERY_PARTNER: [['/delivery', '🚚', 'Active'], ['/delivery/history', '🕑', 'History']],
  ADMIN: [],
};
function BottomNav() {
  const { user } = useAuth(); const items = user ? BOTTOM[user.role] : [];
  if (!items.length) return null;
  return <nav aria-label="Quick" className="fixed inset-x-0 bottom-0 z-30 grid border-t border-stone-200 bg-white pb-[env(safe-area-inset-bottom)] md:hidden" style={{ gridTemplateColumns: `repeat(${items.length}, 1fr)` }}>
    {items.map(([to, ic, l]) => <NavLink key={to} to={to} end className={({ isActive }) => `flex min-h-[56px] flex-col items-center justify-center text-xs ${isActive ? 'font-bold text-field-800' : 'text-stone-600'}`}><span aria-hidden className="text-lg">{ic}</span>{l}</NavLink>)}
  </nav>;
}

export function Footer() {
  return <footer className="mt-12 border-t border-stone-200 bg-field-900 px-4 py-8 pb-24 text-sm text-field-100 md:pb-8"><div className="mx-auto flex max-w-7xl flex-col justify-between gap-2 md:flex-row"><p className="font-display text-lg text-white">FarmDirect</p><p>From the farmer's field to your home. Fresh products, fair prices, direct connections.</p></div></footer>;
}

export default function Layout() {
  return <div className="flex min-h-screen flex-col"><Navbar /><main className="mx-auto w-full max-w-7xl flex-1 px-3 py-4 pb-24 md:px-6 md:pb-8"><Outlet /></main><Footer /><BottomNav /></div>;
}

/** Dashboard frame: sidebar on laptops and up, scrolling tab strip below that. */
export function Shell({ title, links }) {
  return (
    <div className="lg:grid lg:grid-cols-[14rem_1fr] lg:gap-6">
      <aside className="mb-4 lg:mb-0">
        <p className="mb-2 hidden font-display text-lg text-field-900 lg:block">{title}</p>
        <nav aria-label={title} className="-mx-3 flex gap-1 overflow-x-auto px-3 pb-1 lg:mx-0 lg:flex-col lg:overflow-visible lg:px-0">
          {links.map(([to, l, end]) => <NavLink key={to} to={to} end={end} className={({ isActive }) => `shrink-0 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-field-700 text-white' : 'bg-field-50 text-field-900 hover:bg-field-100'}`}>{l}</NavLink>)}
        </nav>
      </aside>
      <section className="min-w-0"><Outlet /></section>
    </div>
  );
}
