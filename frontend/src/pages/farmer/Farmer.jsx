import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, assetUrl, inr, msg, UNIT } from '../../api';
import { Badge, Empty, ErrorBox, Img, Input, Modal, Select, Spinner, StatusBadge, useFetch, useToast } from '../../components/ui';
import { BarChart, LineChart, Stat } from '../../components/charts';
import { TrackOrder } from '../customer/Customer';

export function FarmerHome() {
  const a = useFetch(() => api.get('/farmer/analytics')); const p = useFetch(() => api.get('/farmer/profile'));
  if (a.loading) return <Spinner />; const d = a.data;
  return <div className="space-y-5"><div className="flex flex-wrap items-center justify-between gap-2"><h1>{p.data?.farmName || 'Your farm'}</h1>{p.data && (p.data.verificationStatus === 'APPROVED' ? <Badge>Verified</Badge> : <Link to="/farmer/profile"><Badge tone="amber">Verification {p.data.verificationStatus.toLowerCase()} · submit documents</Badge></Link>)}</div>
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">{[['/farmer/products/new', '➕ Add product'], ['/farmer/products', '📋 Manage stock'], ['/farmer/orders', `📦 New orders (${d.pendingOrders})`], ['/farmer/earnings', '💰 Sales overview']].map(([to, l]) => <Link key={to} to={to} className="card p-4 text-center font-semibold hover:bg-field-50">{l}</Link>)}</div>
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-6"><Stat label="Products" value={d.totalProducts} /><Stat label="Total orders" value={d.totalOrders} /><Stat label="Pending" value={d.pendingOrders} tone={d.pendingOrders ? 'warn' : ''} /><Stat label="In progress" value={d.acceptedOrders} /><Stat label="Completed" value={d.completedOrders} /><Stat label="Total sales" value={inr(d.totalSales)} /></div>
    <section><h2 className="mb-2">Low stock</h2>{d.lowStock.length ? <ul className="card divide-y divide-stone-100">{d.lowStock.map((l) => <li key={l.id} className="flex justify-between p-3 text-sm"><span>{l.name}</span><b className="text-red-700">{Number(l.available)} left</b></li>)}</ul> : <p className="text-sm text-stone-500">All products are well stocked.</p>}</section></div>;
}

const DOCS = ['Aadhaar (masked)', 'Land record / pattadar passbook', 'Farmer ID card', 'FSSAI licence', 'Organic certificate'];
export function FarmerProfile() {
  const toast = useToast(); const { data, loading } = useFetch(() => api.get('/farmer/profile')); const docs = useFetch(() => api.get('/farmer/verification')); const [f, setF] = useState(null); const [docType, setDocType] = useState(DOCS[0]); const [err, setErr] = useState('');
  useEffect(() => { if (data) setF({ ...data, farmImages: data.farmImages || [] }); }, [data]);
  if (loading || !f) return <Spinner />; const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const upload = async (file, doc) => { const fd = new FormData(); fd.append('file', file); fd.append('document', doc ? 'true' : 'false'); const r = await api.post('/uploads', fd); return r.data.url; };
  const save = async (e) => { e.preventDefault(); setErr(''); try { await api.put('/farmer/profile', f); toast('Profile saved'); } catch (x) { setErr(msg(x)); } };
  const geo = () => navigator.geolocation?.getCurrentPosition((p) => setF({ ...f, latitude: p.coords.latitude, longitude: p.coords.longitude }), () => toast('Location permission denied', 'err'));
  return <div className="grid gap-6 lg:grid-cols-2"><form onSubmit={save} className="space-y-3"><h1>Farm profile</h1><ErrorBox error={err} />
      <div className="flex items-center gap-3"><Img src={f.profileImageUrl} alt={f.farmName} className="h-20 w-20 rounded-full" /><label className="btn-outline cursor-pointer">Upload photo<input hidden type="file" accept="image/*" onChange={async (e) => { try { setF({ ...f, profileImageUrl: await upload(e.target.files[0]) }); } catch (x) { toast(msg(x), 'err'); } }} /></label></div>
      <Input label="Farm name" required value={f.farmName} onChange={set('farmName')} /><Input label="Farm address" required value={f.farmAddress} onChange={set('farmAddress')} /><Input label="Farm location" value={f.farmLocation || ''} onChange={set('farmLocation')} />
      <button type="button" className="btn-outline" onClick={geo}>{f.latitude ? '📍 Farm GPS saved' : '📍 Set farm GPS location (for pickup maps)'}</button>
      <div className="grid grid-cols-2 gap-2"><Input label="State" value={f.state || ''} onChange={set('state')} /><Input label="District" value={f.district || ''} onChange={set('district')} /><Input label="Village / city" value={f.city || ''} onChange={set('city')} /><Input label="PIN" value={f.pincode || ''} onChange={set('pincode')} /></div>
      <Input label="Farming category" required value={f.farmingCategory || ''} onChange={set('farmingCategory')} /><label className="block text-sm font-medium">Farming details<textarea className="input mt-1" rows={4} value={f.farmDetails || ''} onChange={set('farmDetails')} /></label>
      <div><p className="mb-1 text-sm font-medium">Farm photos</p><div className="flex flex-wrap gap-2">{f.farmImages.map((u) => <Img key={u} src={u} alt="Farm" className="h-16 w-24 rounded-lg" />)}<label className="btn-outline cursor-pointer">+ Add<input hidden type="file" accept="image/*" onChange={async (e) => { try { setF({ ...f, farmImages: [...f.farmImages, await upload(e.target.files[0])] }); } catch (x) { toast(msg(x), 'err'); } }} /></label></div></div>
      <button className="btn-primary w-full">Save profile</button></form>
    <section className="space-y-3"><h2>Verification</h2><p>Status: <StatusBadge s={f.verificationStatus} /></p><p className="text-sm text-stone-600">Upload a document so the FarmDirect team can verify your farm. Customers see a Verified badge once approved.</p>
      <Select label="Document type" value={docType} onChange={(e) => setDocType(e.target.value)}>{DOCS.map((d) => <option key={d}>{d}</option>)}</Select>
      <label className="btn-primary cursor-pointer">Upload and submit<input hidden type="file" accept="image/*,application/pdf" onChange={async (e) => { try { const url = await upload(e.target.files[0], true); await api.post('/farmer/verification', { documentType: docType, documentUrl: url }); toast('Submitted for review'); docs.reload(); } catch (x) { toast(msg(x), 'err'); } }} /></label>
      {docs.data?.map((d) => <div key={d.id} className="card flex justify-between p-3 text-sm"><span>{d.documentType}</span><StatusBadge s={d.status} /></div>)}</section></div>;
}

const empty = { name: '', description: '', price: '', unit: 'KILOGRAM', categoryId: '', stock: '', minOrderQuantity: 1, harvestDate: '', farmerProduced: true, sourceNote: '', imageUrls: [] };
export function ProductForm() {
  const { id } = useParams(); const nav = useNavigate(); const toast = useToast(); const cats = useFetch(() => api.get('/public/categories')); const mine = useFetch(() => api.get('/farmer/products'));
  const [f, setF] = useState(empty); const [err, setErr] = useState('');
  useEffect(() => { if (id && mine.data) { const p = mine.data.find((x) => String(x.id) === id); if (p) setF({ ...p, stock: p.available, imageUrls: p.imageUrls, harvestDate: p.harvestDate || '', sourceNote: p.sourceNote || '' }); } }, [id, mine.data]);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const addImages = async (files) => { try { const urls = []; for (const file of files) { const fd = new FormData(); fd.append('file', file); urls.push((await api.post('/uploads', fd)).data.url); } setF((x) => ({ ...x, imageUrls: [...x.imageUrls, ...urls] })); } catch (x) { setErr(msg(x)); } };
  const save = async (e) => { e.preventDefault(); setErr('');
    const body = { name: f.name, description: f.description, price: Number(f.price), unit: f.unit, categoryId: Number(f.categoryId), stock: Number(f.stock), minOrderQuantity: Number(f.minOrderQuantity), harvestDate: f.harvestDate || null, farmerProduced: f.farmerProduced, sourceNote: f.sourceNote, imageUrls: f.imageUrls };
    try { id ? await api.put(`/farmer/products/${id}`, body) : await api.post('/farmer/products', body); toast('Product saved'); nav('/farmer/products'); } catch (x) { setErr(msg(x)); } };
  return <form onSubmit={save} className="max-w-2xl space-y-3"><h1>{id ? 'Edit product' : 'Add product'}</h1><ErrorBox error={err} />
    <Input label="Product name" required value={f.name} onChange={set('name')} />
    <Select label="Category" required value={f.categoryId} onChange={set('categoryId')}><option value="">Select category</option>{cats.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</Select>
    <label className="block text-sm font-medium">Description<textarea className="input mt-1" rows={3} maxLength={2000} value={f.description || ''} onChange={set('description')} /></label>
    <div className="grid grid-cols-2 gap-2 md:grid-cols-4"><Input label="Price (₹)" type="number" min="0.01" step="0.01" required value={f.price} onChange={set('price')} /><Select label="Unit" value={f.unit} onChange={set('unit')}>{Object.entries(UNIT).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</Select><Input label="Stock available" type="number" min="0" step="any" required value={f.stock} onChange={set('stock')} /><Input label="Minimum order" type="number" min="0.001" step="any" value={f.minOrderQuantity} onChange={set('minOrderQuantity')} /></div>
    <Input label="Harvest / production date" type="date" value={f.harvestDate} onChange={set('harvestDate')} />
    <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={f.farmerProduced} onChange={(e) => setF({ ...f, farmerProduced: e.target.checked })} /> I grew or produced this on my farm</label>
    {!f.farmerProduced && <Input label="Where does this product come from?" required value={f.sourceNote} onChange={set('sourceNote')} />}
    <div><p className="mb-1 text-sm font-medium">Images</p><div className="flex flex-wrap gap-2">{f.imageUrls.map((u) => <div key={u} className="relative"><Img src={u} alt="" className="h-20 w-20 rounded-lg" /><button type="button" aria-label="Remove image" className="absolute -right-1 -top-1 rounded-full bg-red-700 px-1.5 text-xs text-white" onClick={() => setF({ ...f, imageUrls: f.imageUrls.filter((x) => x !== u) })}>✕</button></div>)}<label className="btn-outline h-20 w-20 cursor-pointer">+<input hidden multiple type="file" accept="image/*" onChange={(e) => addImages([...e.target.files])} /></label></div></div>
    <button className="btn-primary w-full md:w-auto">Save product</button></form>;
}

export function ManageProducts() {
  const toast = useToast(); const { data, loading, reload } = useFetch(() => api.get('/farmer/products')); const [stock, setStock] = useState({});
  const act = async (fn, ok) => { try { await fn(); toast(ok); reload(); } catch (e) { toast(msg(e), 'err'); } };
  if (loading) return <Spinner />;
  return <div className="space-y-3"><div className="flex items-center justify-between"><h1>Products and stock</h1><Link className="btn-primary" to="/farmer/products/new">Add product</Link></div>
    {data.length ? <div className="table-wrap"><table><thead><tr><th>Product</th><th>Price</th><th>Stock</th><th>Status</th><th>Actions</th></tr></thead><tbody>{data.map((p) => <tr key={p.id}><td><div className="flex items-center gap-2"><Img src={p.imageUrls[0]} alt={p.name} className="h-10 w-10 rounded" />{p.name}</div></td><td>{inr(p.price)}/{UNIT[p.unit]}</td>
      <td><div className="flex items-center gap-1"><input aria-label={`Stock for ${p.name}`} className="input !min-h-[36px] !w-20" type="number" min="0" step="any" value={stock[p.id] ?? Number(p.available)} onChange={(e) => setStock({ ...stock, [p.id]: e.target.value })} />{stock[p.id] !== undefined && <button className="btn-primary !min-h-[36px]" onClick={() => act(() => api.patch(`/farmer/products/${p.id}/stock`, { stock: Number(stock[p.id]) }).then(() => setStock((s) => { const n = { ...s }; delete n[p.id]; return n; })), 'Stock updated')}>Save</button>}</div></td>
      <td>{p.enabled ? <Badge>Listed</Badge> : <Badge tone="gray">Hidden</Badge>}</td><td className="space-x-2 whitespace-nowrap"><Link className="underline" to={`/farmer/products/${p.id}/edit`}>Edit</Link><button className="underline" onClick={() => act(() => api.patch(`/farmer/products/${p.id}/toggle`), 'Updated')}>{p.enabled ? 'Disable' : 'Enable'}</button><button className="text-red-700 underline" onClick={() => confirm('Delete this product?') && act(() => api.delete(`/farmer/products/${p.id}`), 'Deleted')}>Delete</button></td></tr>)}</tbody></table></div> : <Empty title="No products yet">Add your first product to start selling.</Empty>}</div>;
}

export function FarmerOrders() {
  const toast = useToast(); const [filter, setFilter] = useState(''); const { data, loading, reload } = useFetch(() => api.get('/farmer/orders', { params: filter ? { status: filter } : {} }), [filter]); const [rej, setRej] = useState(null); const [reason, setReason] = useState('');
  const act = async (fn, ok) => { try { await fn(); toast(ok); reload(); } catch (e) { toast(msg(e), 'err'); } };
  const next = (s) => ({ FARMER_ACCEPTED: ['PROCESSING', 'Start preparing'], PROCESSING: ['READY_FOR_PICKUP', 'Mark ready for pickup'] }[s.status] || (s.fulfilmentMode === 'FARMER_DELIVERY' ? { READY_FOR_PICKUP: ['PICKED_UP', 'Picked up'], PICKED_UP: ['OUT_FOR_DELIVERY', 'Out for delivery'], OUT_FOR_DELIVERY: ['DELIVERED', 'Delivered'] }[s.status] : s.fulfilmentMode === 'CUSTOMER_PICKUP' && s.status === 'READY_FOR_PICKUP' ? ['DELIVERED', 'Handed to customer'] : null));
  return <div className="space-y-3"><div className="flex flex-wrap items-center justify-between gap-2"><h1>Orders</h1><Select aria-label="Filter" label="" value={filter} onChange={(e) => setFilter(e.target.value)}><option value="">All</option>{['ORDER_PLACED', 'FARMER_ACCEPTED', 'PROCESSING', 'READY_FOR_PICKUP', 'PICKED_UP', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'FARMER_REJECTED'].map((s) => <option key={s} value={s}>{s.replaceAll('_', ' ').toLowerCase()}</option>)}</Select></div>
    {loading ? <Spinner /> : !data.length ? <Empty title="No orders" /> : data.map((s) => { const n = next(s); return <article key={s.id} className="card p-3 md:p-4"><div className="flex flex-wrap items-center justify-between gap-2"><p><b>{s.orderNumber}</b> <span className="text-xs text-stone-500">{new Date(s.createdAt).toLocaleString()}</span></p><StatusBadge s={s.status} /></div>
      <ul className="mt-2 text-sm">{s.items.map((i) => <li key={i.productId}>{i.name} × {Number(i.quantity)} — {inr(i.lineTotal)}</li>)}</ul><p className="mt-1 text-sm font-semibold">Your earnings: {inr(s.subtotal)} · {s.fulfilmentMode.replaceAll('_', ' ').toLowerCase()} · {s.paymentMethod === 'COD' ? 'cash on delivery' : 'paid online'}</p>
      {s.customerMobile && <p className="mt-1 text-sm text-stone-600">{s.customerName} · {s.customerMobile}<br />{s.deliveryAddress}</p>}
      <div className="mt-2 flex flex-wrap gap-2">{s.status === 'ORDER_PLACED' && <><button className="btn-primary" onClick={() => act(() => api.post(`/farmer/orders/${s.id}/accept`, {}), 'Order accepted')}>Accept</button><button className="btn-outline" onClick={() => setRej(s)}>Reject</button></>}
        {n && <button className="btn-primary" onClick={() => act(() => api.post(`/farmer/orders/${s.id}/status`, { status: n[0] }), 'Status updated')}>{n[1]}</button>}<Link className="btn-outline" to={`/farmer/orders/${s.id}/tracking`}>Tracking</Link></div></article>; })}
    {rej && <Modal title={`Reject ${rej.orderNumber}`} onClose={() => setRej(null)}><Input label="Reason (shown to the customer)" value={reason} onChange={(e) => setReason(e.target.value)} /><button className="btn-danger mt-3 w-full" disabled={!reason.trim()} onClick={() => { act(() => api.post(`/farmer/orders/${rej.id}/reject`, { reason }), 'Order rejected'); setRej(null); setReason(''); }}>Reject order</button></Modal>}</div>;
}
export const FarmerTrack = () => <TrackOrder role="farmer" />;

export function Earnings() {
  const { data, loading } = useFetch(() => api.get('/farmer/analytics')); const reviews = useFetch(() => api.get('/farmer/reviews'));
  if (loading) return <Spinner />; const months = Object.keys(data.monthlyEarnings);
  return <div className="space-y-5"><h1>Sales and earnings</h1><div className="grid grid-cols-2 gap-3 md:grid-cols-4"><Stat label="Total sales" value={inr(data.totalSales)} /><Stat label="Completed orders" value={data.completedOrders} /><Stat label="Cancelled / rejected" value={data.cancelledOrders} /><Stat label="Rating" value={data.ratingCount ? `★ ${data.ratingAverage} (${data.ratingCount})` : 'No ratings yet'} /></div>
    <section className="card p-3"><h2 className="mb-2">Monthly earnings</h2>{months.length ? <LineChart labels={months} values={months.map((m) => data.monthlyEarnings[m])} label="₹" /> : <p className="p-4 text-sm text-stone-500">Earnings appear after your first delivered order.</p>}</section>
    <section className="card p-3"><h2 className="mb-2">Best-selling products</h2>{data.bestSelling.length ? <BarChart labels={data.bestSelling.map((b) => b.name)} values={data.bestSelling.map((b) => Number(b.quantity))} label="Quantity sold" /> : <p className="p-4 text-sm text-stone-500">No sales yet.</p>}</section>
    <section><h2 className="mb-2">Customer reviews</h2>{reviews.data?.length ? reviews.data.map((r) => <div key={r.id} className="border-b border-stone-100 py-2 text-sm"><b>{r.customerName}</b> <span className="text-wheat-700">{'★'.repeat(r.rating)}</span><p className="text-stone-600">{r.comment}</p></div>) : <p className="text-sm text-stone-500">No reviews yet.</p>}</section></div>;
}
