import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, inr, msg, UNIT } from '../../api';
import { Badge, Empty, ErrorBox, Img, Input, Modal, Select, Spinner, StatusBadge, useFetch, useToast } from '../../components/ui';
import ProductCard from '../../components/ProductCard';
import MapView from '../../components/MapView';
import { useStomp } from '../../components/useStomp';
import { useAuth } from '../../auth';

export function CustomerHome() {
  const { user } = useAuth(); const orders = useFetch(() => api.get('/customer/orders'));
  const live = (orders.data || []).flatMap((o) => o.subOrders).filter((s) => !['DELIVERED', 'CANCELLED', 'FARMER_REJECTED', 'REFUNDED'].includes(s.status));
  return <div className="space-y-5"><h1>Hello, {user.fullName.split(' ')[0]}</h1>
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">{[['/products', 'Shop produce'], ['/customer/cart', 'My cart'], ['/customer/orders', 'My orders'], ['/customer/wishlist', 'Wishlist']].map(([to, l]) => <Link key={to} to={to} className="card p-4 text-center font-semibold hover:bg-field-50">{l}</Link>)}</div>
    <section><h2 className="mb-2">Orders on the way</h2>{orders.loading ? <Spinner /> : live.length ? live.map((s) => <Link key={s.id} to={`/customer/track/${s.id}`} className="card mb-2 flex items-center justify-between p-3"><span><b>{s.orderNumber}</b> · {s.farmName}</span><StatusBadge s={s.status} /></Link>) : <Empty title="No active orders" />}</section></div>;
}

export function Cart() {
  const nav = useNavigate(); const toast = useToast(); const { data, loading, reload } = useFetch(() => api.get('/customer/cart'));
  const upd = async (l, q) => { try { await api.put('/customer/cart/items', { productId: l.productId, quantity: q }); reload(); window.dispatchEvent(new Event('fd:cart')); } catch (e) { toast(msg(e), 'err'); } };
  const del = async (l) => { await api.delete(`/customer/cart/items/${l.itemId}`); reload(); window.dispatchEvent(new Event('fd:cart')); };
  if (loading) return <Spinner />;
  if (!data.items.length) return <Empty title="Your cart is empty"><Link className="underline" to="/products">Browse fresh produce</Link></Empty>;
  const step = (l) => (['KILOGRAM', 'LITRE'].includes(l.unit) ? 0.5 : 1);
  return <div className="lg:grid lg:grid-cols-[1fr_20rem] lg:gap-6"><div><h1 className="mb-3">Your cart</h1>{data.items.map((l) => <div key={l.itemId} className="card mb-2 flex gap-3 p-3"><Img src={l.imageUrl} alt={l.name} className="h-20 w-20 shrink-0 rounded-lg" />
      <div className="min-w-0 flex-1"><p className="font-semibold">{l.name}</p><p className="text-xs text-stone-500">{l.farmerName} · {inr(l.price)}/{UNIT[l.unit]}</p>
        <div className="mt-2 flex items-center gap-2"><button className="btn-outline !min-h-[40px] !px-3" aria-label="Decrease" disabled={Number(l.quantity) - step(l) < Number(l.minOrderQuantity)} onClick={() => upd(l, Number(l.quantity) - step(l))}>−</button><span className="w-16 text-center">{Number(l.quantity)} {UNIT[l.unit]}</span><button className="btn-outline !min-h-[40px] !px-3" aria-label="Increase" disabled={Number(l.quantity) + step(l) > Number(l.available)} onClick={() => upd(l, Number(l.quantity) + step(l))}>+</button><button className="ml-auto text-sm text-red-700 underline" onClick={() => del(l)}>Remove</button></div></div><p className="font-bold">{inr(l.lineTotal)}</p></div>)}</div>
    <aside className="card h-fit p-4"><h2>Summary</h2><p className="mt-2 flex justify-between"><span>Subtotal</span><b>{inr(data.subtotal)}</b></p><p className="mt-1 text-xs text-stone-500">Delivery and discounts are calculated at checkout.</p><button className="btn-primary mt-3 w-full" onClick={() => nav('/customer/checkout')}>Checkout</button></aside></div>;
}

export function Wishlist() {
  const { data, loading, reload } = useFetch(() => api.get('/customer/wishlist'));
  if (loading) return <Spinner />;
  return <div><h1 className="mb-3">Wishlist</h1>{data.length ? <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-4">{data.map((p) => <div key={p.id}><ProductCard p={p} /><button className="mt-1 w-full text-sm text-red-700 underline" onClick={async () => { await api.delete(`/customer/wishlist/${p.id}`); reload(); }}>Remove</button></div>)}</div> : <Empty title="Nothing saved yet" />}</div>;
}

const emptyAddr = { label: 'Home', line1: '', city: '', district: '', state: '', pincode: '', defaultAddress: false };
function AddressForm({ onSaved }) {
  const [a, setA] = useState(emptyAddr); const [err, setErr] = useState(''); const set = (k) => (e) => setA({ ...a, [k]: e.target.value });
  const locate = () => navigator.geolocation?.getCurrentPosition((p) => setA((x) => ({ ...x, latitude: p.coords.latitude, longitude: p.coords.longitude })), () => setErr('Location permission was not granted'));
  const save = async (e) => { e.preventDefault(); setErr(''); try { await api.post('/customer/addresses', a); setA(emptyAddr); onSaved(); } catch (x) { setErr(msg(x)); } };
  return <form onSubmit={save} className="space-y-2"><ErrorBox error={err} /><Input label="Label" value={a.label} onChange={set('label')} /><Input label="Address" required value={a.line1} onChange={set('line1')} />
    <div className="grid grid-cols-2 gap-2"><Input label="City / village" required value={a.city} onChange={set('city')} /><Input label="District" required value={a.district} onChange={set('district')} /><Input label="State" required value={a.state} onChange={set('state')} /><Input label="PIN code" required maxLength={6} value={a.pincode} onChange={set('pincode')} /></div>
    <button type="button" className="btn-outline" onClick={locate}>{a.latitude ? '📍 Location saved' : '📍 Use my current location (for map tracking)'}</button>
    <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={a.defaultAddress} onChange={(e) => setA({ ...a, defaultAddress: e.target.checked })} /> Make default</label><button className="btn-primary w-full">Save address</button></form>;
}

export function Checkout() {
  const nav = useNavigate(); const toast = useToast();
  const cart = useFetch(() => api.get('/customer/cart')); const addrs = useFetch(() => api.get('/customer/addresses'));
  const [addressId, setAddr] = useState(''); const [method, setMethod] = useState('UPI'); const [coupon, setCoupon] = useState(''); const [mode, setMode] = useState('PLATFORM_PARTNER'); const [busy, setBusy] = useState(false); const [err, setErr] = useState('');
  useEffect(() => { if (addrs.data?.length && !addressId) setAddr(String((addrs.data.find((a) => a.defaultAddress) || addrs.data[0]).id)); }, [addrs.data]); // eslint-disable-line
  const loadRzp = () => new Promise((res, rej) => { if (window.Razorpay) return res(); const s = document.createElement('script'); s.src = 'https://checkout.razorpay.com/v1/checkout.js'; s.onload = res; s.onerror = () => rej(new Error('Could not load the payment window')); document.body.appendChild(s); });
  const place = async () => { setErr(''); setBusy(true);
    try {
      const { data: o } = await api.post('/customer/orders/checkout', { addressId: Number(addressId), paymentMethod: method, couponCode: coupon || null, fulfilmentMode: mode });
      window.dispatchEvent(new Event('fd:cart'));
      if (method === 'COD') { toast('Order placed'); return nav('/customer/orders'); }
      await loadRzp();
      const rz = new window.Razorpay({ key: o.razorpayKeyId, order_id: o.razorpayOrderId, amount: o.amountPaise, currency: 'INR', name: 'FarmDirect', description: `Order ${o.orderNumber}`,
        handler: async (r) => { try { await api.post('/customer/payments/verify', { razorpayOrderId: r.razorpay_order_id, razorpayPaymentId: r.razorpay_payment_id, razorpaySignature: r.razorpay_signature }); toast('Payment confirmed'); nav('/customer/orders'); } catch (x) { setErr(msg(x)); } },
        modal: { ondismiss: () => { setErr('Payment was not completed. Your order is held for 30 minutes; open My orders to review it.'); nav('/customer/orders'); } }, theme: { color: '#1f5b36' } });
      rz.on('payment.failed', () => setErr('Payment failed. You can try again from a new checkout.')); rz.open();
    } catch (x) { setErr(msg(x)); } finally { setBusy(false); } };
  if (cart.loading || addrs.loading) return <Spinner />;
  if (!cart.data?.items.length) return <Empty title="Your cart is empty" />;
  return <div className="grid gap-6 lg:grid-cols-[1fr_20rem]"><div className="space-y-5"><h1>Checkout</h1><ErrorBox error={err} />
    <section><h2 className="mb-2">Delivery address</h2>{addrs.data.map((a) => <label key={a.id} className="card mb-2 flex cursor-pointer gap-2 p-3"><input type="radio" name="addr" checked={String(a.id) === addressId} onChange={() => setAddr(String(a.id))} /><span className="text-sm"><b>{a.label}</b><br />{a.line1}, {a.city}, {a.district}, {a.state} {a.pincode}</span></label>)}
      <details className="card p-3" open={!addrs.data.length}><summary className="cursor-pointer font-semibold">Add a new address</summary><div className="mt-3"><AddressForm onSaved={addrs.reload} /></div></details></section>
    <section className="grid gap-3 sm:grid-cols-2"><Select label="Delivery option" value={mode} onChange={(e) => setMode(e.target.value)}><option value="PLATFORM_PARTNER">Delivery partner</option><option value="FARMER_DELIVERY">Farmer delivers</option><option value="CUSTOMER_PICKUP">I will pick up from the farm</option></Select>
      <Select label="Payment method" value={method} onChange={(e) => setMethod(e.target.value)}><option value="UPI">UPI</option><option value="DEBIT_CARD">Debit card</option><option value="CREDIT_CARD">Credit card</option><option value="NET_BANKING">Net banking</option><option value="COD">Cash on delivery</option></Select>
      <Input label="Coupon code" value={coupon} onChange={(e) => setCoupon(e.target.value)} /></section></div>
    <aside className="card h-fit p-4"><h2>Order</h2>{cart.data.items.map((l) => <p key={l.itemId} className="mt-1 flex justify-between text-sm"><span>{l.name} × {Number(l.quantity)}</span><span>{inr(l.lineTotal)}</span></p>)}<hr className="my-2" /><p className="flex justify-between font-bold"><span>Items</span><span>{inr(cart.data.subtotal)}</span></p><p className="text-xs text-stone-500">Delivery fee per farm and discounts are applied by the server when you place the order.</p>
      <button className="btn-primary mt-3 w-full" disabled={busy || !addressId} onClick={place}>{busy ? 'Placing order…' : method === 'COD' ? 'Place order' : 'Pay securely'}</button></aside></div>;
}

export function Orders() {
  const { data, loading, reload } = useFetch(() => api.get('/customer/orders')); const toast = useToast(); const [rev, setRev] = useState(null);
  const cancel = async (s) => { if (!confirm('Cancel this order?')) return; try { await api.post(`/customer/suborders/${s.id}/cancel`); toast('Order cancelled'); reload(); } catch (e) { toast(msg(e), 'err'); } };
  if (loading) return <Spinner />; if (!data.length) return <Empty title="No orders yet"><Link className="underline" to="/products">Start shopping</Link></Empty>;
  return <div className="space-y-4"><h1>My orders</h1>{data.map((o) => <article key={o.id} className="card p-3 md:p-4"><div className="flex flex-wrap items-center justify-between gap-2"><div><b>{o.orderNumber}</b><p className="text-xs text-stone-500">{new Date(o.createdAt).toLocaleString()} · {o.paymentMethod} · {o.paymentStatus.replaceAll('_', ' ').toLowerCase()}</p></div><div className="text-right"><b>{inr(o.total)}</b><div><a className="text-xs underline" href={`/api/customer/orders/${o.id}/invoice`} onClick={async (e) => { e.preventDefault(); const r = await api.get(`/customer/orders/${o.id}/invoice`, { responseType: 'text' }); const w = window.open('', '_blank'); w.document.write(r.data); w.document.close(); }}>Invoice</a></div></div></div>
    {o.subOrders.map((s) => <div key={s.id} className="mt-3 rounded-lg bg-field-50 p-3"><div className="flex flex-wrap items-center justify-between gap-2"><p className="text-sm font-semibold">{s.farmName}</p><StatusBadge s={s.status} /></div><ul className="mt-1 text-sm text-stone-700">{s.items.map((i) => <li key={i.productId}>{i.name} × {Number(i.quantity)} — {inr(i.lineTotal)}</li>)}</ul>{s.rejectionReason && <p className="mt-1 text-sm text-red-700">Reason: {s.rejectionReason}</p>}
      <div className="mt-2 flex flex-wrap gap-2"><Link className="btn-outline" to={`/customer/track/${s.id}`}>Track order</Link>{['ORDER_PLACED', 'FARMER_ACCEPTED'].includes(s.status) && <button className="btn-outline" onClick={() => cancel(s)}>Cancel</button>}{s.status === 'DELIVERED' && <button className="btn-outline" onClick={() => setRev(s)}>Rate farmer</button>}</div></div>)}</article>)}
    {rev && <ReviewModal s={rev} onClose={() => setRev(null)} />}</div>;
}
function ReviewModal({ s, onClose }) {
  const toast = useToast(); const [rating, setRating] = useState(5); const [comment, setComment] = useState(''); const [productId, setP] = useState('');
  const send = async () => { try { await api.post('/customer/reviews', { subOrderId: s.id, productId: productId ? Number(productId) : null, rating, comment }); toast('Thanks for your review'); onClose(); } catch (e) { toast(msg(e), 'err'); } };
  return <Modal title={`Review ${s.farmName}`} onClose={onClose}><div className="space-y-3"><Select label="About" value={productId} onChange={(e) => setP(e.target.value)}><option value="">The farmer overall</option>{s.items.map((i) => <option key={i.productId} value={i.productId}>{i.name}</option>)}</Select>
    <Select label="Rating" value={rating} onChange={(e) => setRating(Number(e.target.value))}>{[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{'★'.repeat(n)}</option>)}</Select>
    <label className="block text-sm font-medium">Comment<textarea className="input mt-1" rows={3} maxLength={1000} value={comment} onChange={(e) => setComment(e.target.value)} /></label><button className="btn-primary w-full" onClick={send}>Submit review</button></div></Modal>;
}

const STEPS = [['ORDER_PLACED', 'Order placed'], ['FARMER_ACCEPTED', 'Farmer accepted'], ['PROCESSING', 'Preparing your products'], ['READY_FOR_PICKUP', 'Ready for pickup'], ['PICKED_UP', 'Picked up'], ['OUT_FOR_DELIVERY', 'Out for delivery'], ['DELIVERED', 'Delivered']];
export function TrackOrder({ role = 'customer' }) {
  const { id } = useParams(); const { data, loading, error, reload } = useFetch(() => api.get(`/${role}/${role === 'customer' ? 'suborders' : 'orders'}/${id}/tracking`), [id]);
  const [live, setLive] = useState(null);
  useStomp(`/topic/tracking/${id}`, (m) => { if (m.type === 'LOCATION') setLive({ lat: m.latitude, lng: m.longitude, at: m.at }); else reload(); });
  if (loading) return <Spinner />; if (error || !data) return <Empty title="Order not found" />;
  const o = data.order, hist = Object.fromEntries(data.history.map((h) => [h.status, h])), idx = STEPS.findIndex(([s]) => s === o.status), ended = ['CANCELLED', 'FARMER_REJECTED', 'RETURN_REQUESTED', 'REFUNDED'].includes(o.status);
  const partnerPos = live || (data.partnerLat != null ? { lat: data.partnerLat, lng: data.partnerLng } : null);
  return <div className="space-y-4"><div className="flex flex-wrap items-center justify-between gap-2"><h1>Track order {o.orderNumber}</h1><StatusBadge s={o.status} /></div>
    <div className="grid gap-4 lg:grid-cols-[22rem_1fr]">
      <div className="space-y-4"><section className="card p-4"><p className="font-semibold">{o.farmName}</p><p className="text-sm text-stone-600">{o.farmerName}</p><ul className="mt-2 text-sm">{o.items.map((i) => <li key={i.productId}>{i.name} × {Number(i.quantity)}</li>)}</ul><p className="mt-2 font-bold">{inr(Number(o.subtotal) + Number(o.deliveryCharge))}</p></section>
        <ol className="card p-4" aria-label="Order timeline">{STEPS.map(([s, label], i) => { const h = hist[s], done = !!h && !ended || (i <= idx && !ended), cur = i === idx && !ended; return <li key={s} className="relative flex gap-3 pb-4 last:pb-0"><span className={`z-10 mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-xs ${done ? 'bg-field-700 text-white' : 'border-2 border-stone-300 bg-white'}`}>{done ? '✓' : ''}</span>{i < STEPS.length - 1 && <span className={`absolute left-[9px] top-6 h-full w-0.5 ${i < idx && !ended ? 'bg-field-700' : 'bg-stone-200'}`} />}
          <div><p className={`text-sm ${cur ? 'font-bold' : done ? 'font-medium' : 'text-stone-400'}`}>{label}</p>{h && <><p className="text-xs text-stone-500">{new Date(h.at).toLocaleString()}</p><p className="text-xs text-stone-600">{h.description}</p></>}</div></li>; })}
          {ended && <li className="mt-2 text-sm font-semibold text-red-700">{o.status.replaceAll('_', ' ')}{o.rejectionReason ? `: ${o.rejectionReason}` : ''}</li>}</ol>
        {data.partner && <section className="card p-4 text-sm"><p className="font-semibold">Delivery partner</p><p>{data.partner.name}{data.partner.vehicle ? ` · ${data.partner.vehicle}` : ''}</p>{data.partner.mobile && <a className="btn-outline mt-2" href={`tel:+91${data.partner.mobile}`}>📞 Call {data.partner.name.split(' ')[0]}</a>}</section>}</div>
      <section><MapView pickup={data.pickupLat != null ? { lat: data.pickupLat, lng: data.pickupLng } : null} drop={data.dropLat != null ? { lat: data.dropLat, lng: data.dropLng } : null} partner={data.trackingActive ? partnerPos : null} />
        <p className="mt-2 text-xs text-stone-500">{data.trackingActive ? (partnerPos ? `Live location updated ${new Date(partnerPos.at || data.partnerLocationAt || Date.now()).toLocaleTimeString()}` : 'Waiting for the delivery partner to share a location.') : 'Live location appears here only while your order is out with a delivery partner who is sharing GPS. Arrival times are not estimated.'}</p></section></div></div>;
}

export function CustomerProfile() {
  const toast = useToast(); const me = useFetch(() => api.get('/me')); const addrs = useFetch(() => api.get('/customer/addresses')); const [f, setF] = useState(null); const [adding, setAdding] = useState(false);
  useEffect(() => { if (me.data) setF(me.data); }, [me.data]);
  if (!f) return <Spinner />; const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const save = async (e) => { e.preventDefault(); try { await api.put('/me', f); toast('Profile saved'); } catch (x) { toast(msg(x), 'err'); } };
  return <div className="grid gap-6 lg:grid-cols-2"><form onSubmit={save} className="space-y-3"><h1>My profile</h1><Input label="Full name" value={f.fullName} onChange={set('fullName')} /><Input label="Email" value={f.email} disabled /><Input label="Mobile" value={f.mobile} disabled />
      <div className="grid grid-cols-2 gap-2"><Input label="State" value={f.state || ''} onChange={set('state')} /><Input label="District" value={f.district || ''} onChange={set('district')} /><Input label="City" value={f.city || ''} onChange={set('city')} /><Input label="PIN code" value={f.pincode || ''} onChange={set('pincode')} /></div><button className="btn-primary">Save profile</button></form>
    <section><h2 className="mb-2">Delivery addresses</h2>{addrs.data?.map((a) => <div key={a.id} className="card mb-2 flex items-start justify-between gap-2 p-3 text-sm"><span><b>{a.label}</b>{a.defaultAddress && <Badge> default</Badge>}<br />{a.line1}, {a.city}, {a.district}, {a.state} {a.pincode}</span><button className="text-red-700 underline" onClick={async () => { await api.delete(`/customer/addresses/${a.id}`); addrs.reload(); }}>Delete</button></div>)}
      <button className="btn-outline" onClick={() => setAdding(!adding)}>{adding ? 'Close' : 'Add address'}</button>{adding && <div className="mt-3"><AddressForm onSaved={() => { addrs.reload(); setAdding(false); }} /></div>}</section></div>;
}
