import { useState } from 'react';
import { api, inr, msg } from '../../api';
import { Badge, Empty, Img, Input, Modal, Select, Spinner, StatusBadge, useFetch, useToast } from '../../components/ui';
import { BarChart, DonutChart, LineChart, Stat } from '../../components/charts';

const Table = ({ head, rows, empty = 'Nothing here yet' }) => rows.length ? <div className="table-wrap"><table><thead><tr>{head.map((h) => <th key={h}>{h}</th>)}</tr></thead><tbody>{rows.map((r, i) => <tr key={i}>{r.map((c, j) => <td key={j}>{c}</td>)}</tr>)}</tbody></table></div> : <Empty title={empty} />;
const useAct = (reload) => { const toast = useToast(); return async (fn, ok = 'Done') => { try { await fn(); toast(ok); reload(); } catch (e) { toast(msg(e), 'err'); } }; };

export function AdminHome() {
  const { data: d, loading } = useFetch(() => api.get('/admin/analytics')); if (loading) return <Spinner />;
  const reg = Object.keys(d.monthlyRegistrations), cat = Object.keys(d.popularCategories);
  return <div className="space-y-5"><h1>Platform overview</h1><div className="grid grid-cols-2 gap-3 md:grid-cols-4"><Stat label="Farmers" value={d.totalFarmers} /><Stat label="Customers" value={d.totalCustomers} /><Stat label="Delivery partners" value={d.totalDeliveryPartners} /><Stat label="Products" value={d.totalProducts} />
    <Stat label="Total orders" value={d.totalOrders} /><Stat label="Completed" value={d.completedOrders} /><Stat label="Pending" value={d.pendingOrders} /><Stat label="Cancelled" value={d.cancelledOrders} />
    <Stat label="Orders in last 7 days" value={d.ordersLast7Days} /><Stat label="Pending verifications" value={d.pendingVerifications} tone={d.pendingVerifications ? 'warn' : ''} /><Stat label="Open complaints" value={d.openComplaints} tone={d.openComplaints ? 'warn' : ''} /></div>
    <div className="grid gap-4 lg:grid-cols-2"><section className="card p-3"><h2 className="mb-2">Monthly registrations</h2><LineChart labels={reg} values={reg.map((m) => d.monthlyRegistrations[m])} label="Registrations" /></section>
      <section className="card p-3"><h2 className="mb-2">Popular categories (units sold)</h2>{cat.length ? <DonutChart labels={cat} values={cat.map((c) => Number(d.popularCategories[c]))} /> : <p className="p-4 text-sm text-stone-500">No orders yet.</p>}</section></div></div>;
}

export function Users({ role, title }) {
  const [q, setQ] = useState(''); const { data, loading, reload } = useFetch(() => api.get('/admin/users', { params: { role, q } }), [role, q]); const act = useAct(reload);
  return <div className="space-y-3"><h1>{title}</h1><Input label="Search name, email or mobile" value={q} onChange={(e) => setQ(e.target.value)} />
    {loading ? <Spinner /> : <Table head={['Name', 'Contact', 'Location', 'Status', 'Joined', '']} rows={data.map((u) => [<b key="n">{u.fullName}</b>, <span key="c">{u.email}<br />{u.mobile}</span>, [u.district, u.state].filter(Boolean).join(', '), <span key="s"><StatusBadge s={u.status} />{u.verification && <> <StatusBadge s={u.verification} /></>}</span>, new Date(u.createdAt).toLocaleDateString(),
      <button key="b" className="underline" onClick={() => act(() => api.patch(`/admin/users/${u.id}/status`, { status: u.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED' }), 'Account updated')}>{u.status === 'SUSPENDED' ? 'Reactivate' : 'Suspend'}</button>])} />}</div>;
}

export function AdminProducts() {
  const { data, loading, reload } = useFetch(() => api.get('/admin/products')); const act = useAct(reload); if (loading) return <Spinner />;
  return <div className="space-y-3"><h1>Products</h1><Table head={['Product', 'Farm', 'Category', 'Price', 'Stock', 'Listed', '']} rows={data.map((p) => [<span key="p" className="flex items-center gap-2"><Img src={p.imageUrls[0]} alt={p.name} className="h-8 w-8 rounded" />{p.name}</span>, p.farmName, p.categoryName, inr(p.price), Number(p.available), p.enabled ? <Badge key="l">Yes</Badge> : <Badge key="l" tone="gray">No</Badge>, <button key="t" className="underline" onClick={() => act(() => api.patch(`/admin/products/${p.id}/toggle`))}>{p.enabled ? 'Hide' : 'Show'}</button>])} /></div>;
}

export function Categories() {
  const { data, loading, reload } = useFetch(() => api.get('/admin/categories')); const act = useAct(reload); const [name, setName] = useState('');
  return <div className="max-w-2xl space-y-3"><h1>Product categories</h1><form className="flex items-end gap-2" onSubmit={(e) => { e.preventDefault(); act(() => api.post('/admin/categories', { name }).then(() => setName('')), 'Category added'); }}><Input label="New category" required value={name} onChange={(e) => setName(e.target.value)} className="flex-1" /><button className="btn-primary">Add</button></form>
    {loading ? <Spinner /> : <Table head={['Name', 'Status', '']} rows={data.map((c) => [c.name, c.active ? <Badge key="a">Active</Badge> : <Badge key="a" tone="gray">Hidden</Badge>, <button key="b" className="underline" onClick={() => act(() => api.put(`/admin/categories/${c.id}`, { name: c.name, description: c.description, active: !c.active }))}>{c.active ? 'Hide' : 'Show'}</button>])} />}</div>;
}

export function AdminOrders() {
  const { data, loading } = useFetch(() => api.get('/admin/orders')); if (loading) return <Spinner />;
  return <div className="space-y-3"><h1>Orders</h1><Table head={['Order', 'Date', 'Total', 'Payment', 'Farms and status']} rows={data.map((o) => [<b key="n">{o.orderNumber}</b>, new Date(o.createdAt).toLocaleDateString(), inr(o.total), `${o.paymentMethod} · ${o.paymentStatus.toLowerCase().replaceAll('_', ' ')}`, <div key="s" className="space-y-1">{o.subOrders.map((s) => <div key={s.id}>{s.farmName} <StatusBadge s={s.status} /></div>)}</div>])} /></div>;
}

export function Verification() {
  const { data, loading, reload } = useFetch(() => api.get('/admin/verifications')); const act = useAct(reload); const [note, setNote] = useState({});
  if (loading) return <Spinner />;
  return <div className="space-y-3"><h1>Farmer verification</h1>{data.length ? data.map((f) => <article key={f.farmerId} className="card p-3"><div className="flex flex-wrap items-center justify-between gap-2"><p><b>{f.farmName}</b> · {f.farmerName}</p><StatusBadge s={f.status} /></div>
    <ul className="my-2 text-sm">{f.documents.length ? f.documents.map((d) => <li key={d.id}>{d.documentType}: <a className="underline" target="_blank" rel="noreferrer" href={d.documentUrl}>open document</a> <StatusBadge s={d.status} /></li>) : <li className="text-stone-500">No documents submitted.</li>}</ul>
    <div className="flex flex-wrap items-end gap-2"><Input label="Note to farmer" className="min-w-[12rem] flex-1" value={note[f.farmerId] || ''} onChange={(e) => setNote({ ...note, [f.farmerId]: e.target.value })} /><button className="btn-primary" onClick={() => act(() => api.post(`/admin/verifications/${f.farmerId}`, { approve: true, note: note[f.farmerId] }), 'Approved')}>Approve</button><button className="btn-danger" onClick={() => act(() => api.post(`/admin/verifications/${f.farmerId}`, { approve: false, note: note[f.farmerId] }), 'Rejected')}>Reject</button></div></article>) : <Empty title="No farmers yet" />}</div>;
}

export function Deliveries() {
  const wait = useFetch(() => api.get('/admin/awaiting-assignment')); const all = useFetch(() => api.get('/admin/deliveries')); const partners = useFetch(() => api.get('/admin/delivery-partners')); const [sel, setSel] = useState(null); const [pid, setPid] = useState('');
  const reload = () => { wait.reload(); all.reload(); }; const act = useAct(reload);
  return <div className="space-y-5"><h1>Delivery management</h1><section><h2 className="mb-2">Awaiting a delivery partner</h2>
    {wait.data?.length ? <Table head={['Order', 'Farm', 'Status', '']} rows={wait.data.map((s) => [s.orderNumber, s.farmName, <StatusBadge key="s" s={s.status} />, <button key="b" className="btn-primary" onClick={() => setSel(s)}>Assign</button>])} /> : <Empty title="No orders waiting" />}</section>
    <section><h2 className="mb-2">All deliveries</h2><Table head={['Order', 'Farm', 'Partner', 'Status', 'Live GPS']} rows={(all.data || []).map((d) => [d.orderNumber, d.farmName, d.partner || '—', <StatusBadge key="s" s={d.status} />, d.trackingActive ? 'Active' : '—'])} /></section>
    {sel && <Modal title={`Assign ${sel.orderNumber}`} onClose={() => setSel(null)}><Select label="Delivery partner" value={pid} onChange={(e) => setPid(e.target.value)}><option value="">Select…</option>{partners.data?.map((p) => <option key={p.id} value={p.id}>{p.name} {p.vehicle ? `(${p.vehicle})` : ''}</option>)}</Select><button className="btn-primary mt-3 w-full" disabled={!pid} onClick={() => { act(() => api.post('/admin/deliveries/assign', { subOrderId: sel.id, partnerId: Number(pid) }), 'Partner assigned'); setSel(null); }}>Assign</button></Modal>}</div>;
}

export function Reports() {
  const download = async () => { const r = await api.get('/admin/reports/orders.csv', { responseType: 'blob' }); const a = document.createElement('a'); a.href = URL.createObjectURL(r.data); a.download = 'orders.csv'; a.click(); URL.revokeObjectURL(a.href); };
  const { data } = useFetch(() => api.get('/admin/analytics'));
  return <div className="space-y-4"><h1>Reports</h1><button className="btn-primary" onClick={download}>Download all orders (CSV)</button>{data && <section className="card p-3"><h2 className="mb-2">Registrations by month</h2><BarChart labels={Object.keys(data.monthlyRegistrations)} values={Object.values(data.monthlyRegistrations)} label="Registrations" /></section>}</div>;
}

export function Complaints() {
  const { data, loading, reload } = useFetch(() => api.get('/admin/complaints')); const act = useAct(reload); const [res, setRes] = useState(null); const [text, setText] = useState('');
  if (loading) return <Spinner />;
  return <div className="space-y-3"><h1>Complaints and messages</h1><Table head={['From', 'Subject', 'Details', 'Status', '']} rows={data.map((c) => [c.userName || '—', c.subject, <span key="d" className="max-w-xs whitespace-pre-line">{c.description}{c.orderId ? ` (order #${c.orderId})` : ''}</span>, <StatusBadge key="s" s={c.status === 'OPEN' ? 'PENDING' : 'APPROVED'} />, c.status === 'OPEN' ? <button key="b" className="underline" onClick={() => setRes(c)}>Resolve</button> : c.resolution])} />
    {res && <Modal title="Resolve complaint" onClose={() => setRes(null)}><Input label="Resolution note" value={text} onChange={(e) => setText(e.target.value)} /><button className="btn-primary mt-3 w-full" disabled={!text.trim()} onClick={() => { act(() => api.post(`/admin/complaints/${res.id}/resolve`, { resolution: text }), 'Resolved'); setRes(null); setText(''); }}>Mark resolved</button></Modal>}</div>;
}

export function Refunds() {
  const { data, loading, reload } = useFetch(() => api.get('/admin/refunds')); const act = useAct(reload); if (loading) return <Spinner />;
  return <div className="space-y-3"><h1>Refunds</h1><Table head={['Order', 'Amount', 'Reason', 'Status', '']} rows={data.map((r) => [r.orderNumber, inr(r.amount), r.reason, r.status.replaceAll('_', ' ').toLowerCase(), r.status.startsWith('FAILED') ? <button key="b" className="underline" onClick={() => act(() => api.post(`/admin/refunds/${r.id}/retry`), 'Refund retried')}>Retry</button> : null])} empty="No refunds" /></div>;
}

export function AdminReviews() {
  const { data, loading, reload } = useFetch(() => api.get('/admin/reviews')); const act = useAct(reload); if (loading) return <Spinner />;
  return <div className="space-y-3"><h1>Reviews</h1><Table head={['Customer', 'Rating', 'Comment', '']} rows={data.map((r) => [r.customerName, '★'.repeat(r.rating), r.comment, <button key="d" className="text-red-700 underline" onClick={() => confirm('Remove this review?') && act(() => api.delete(`/admin/reviews/${r.id}`), 'Removed')}>Remove</button>])} empty="No reviews" /></div>;
}

export function Settings() {
  const { data, loading, reload } = useFetch(() => api.get('/admin/settings')); const coupons = useFetch(() => api.get('/admin/coupons')); const act = useAct(reload); const [f, setF] = useState({}); const [c, setC] = useState({ code: '', percentOff: 10, minOrder: 0 });
  if (loading) return <Spinner />; const val = (k, d) => f[k] ?? data[k] ?? d;
  const SETTINGS = [['delivery.base', 'Delivery fee per farm (₹)', '40'], ['delivery.freeAbove', 'Free delivery above (₹ per farm)', '500'], ['cod.enabled', 'Cash on delivery (1 = on, 0 = off)', '1']];
  return <div className="grid gap-6 lg:grid-cols-2"><section className="space-y-3"><h1>Platform settings</h1>{SETTINGS.map(([k, l, d]) => <div key={k} className="flex items-end gap-2"><Input label={l} className="flex-1" value={val(k, d)} onChange={(e) => setF({ ...f, [k]: e.target.value })} /><button className="btn-primary" onClick={() => act(() => api.put('/admin/settings', { key: k, value: String(val(k, d)) }), 'Saved')}>Save</button></div>)}<p className="text-xs text-stone-500">Defaults are used until a value is saved.</p></section>
    <section className="space-y-3"><h2>Promotions</h2><form className="grid grid-cols-3 items-end gap-2" onSubmit={(e) => { e.preventDefault(); act(() => api.post('/admin/coupons', { code: c.code, percentOff: Number(c.percentOff), minOrder: Number(c.minOrder) }).then(() => { setC({ code: '', percentOff: 10, minOrder: 0 }); coupons.reload(); }), 'Coupon created'); }}><Input label="Code" required value={c.code} onChange={(e) => setC({ ...c, code: e.target.value })} /><Input label="% off" type="number" min="1" max="90" value={c.percentOff} onChange={(e) => setC({ ...c, percentOff: e.target.value })} /><Input label="Min ₹" type="number" min="0" value={c.minOrder} onChange={(e) => setC({ ...c, minOrder: e.target.value })} /><button className="btn-primary col-span-3">Create coupon</button></form>
      <Table head={['Code', '% off', 'Min order', '']} rows={(coupons.data || []).map((x) => [x.code, x.percentOff, inr(x.minOrder), <button key="t" className="underline" onClick={() => act(() => api.patch(`/admin/coupons/${x.id}/toggle`).then(coupons.reload))}>{x.active ? 'Disable' : 'Enable'}</button>])} empty="No coupons" /></section></div>;
}
