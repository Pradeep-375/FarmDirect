import { useEffect, useRef, useState } from 'react';
import { api, inr, msg } from '../../api';
import MapView from '../../components/MapView';
import { Empty, Spinner, StatusBadge, useFetch, useToast } from '../../components/ui';

const ACTION = { READY_FOR_PICKUP: ['pickup', 'Confirm pickup'], PICKED_UP: ['out-for-delivery', 'Start delivery'], OUT_FOR_DELIVERY: ['delivered', 'Confirm delivered'] };

/** Shares GPS only while the delivery is active and the partner has granted location permission. */
function useGps(active, deliveryId) {
  const [state, setState] = useState('idle'); const last = useRef(0);
  useEffect(() => {
    if (!active) { setState('idle'); return undefined; }
    if (!navigator.geolocation) { setState('unsupported'); return undefined; }
    const id = navigator.geolocation.watchPosition((p) => {
      setState('sharing'); const now = Date.now(); if (now - last.current < 8000) return; last.current = now;
      api.post(`/delivery/${deliveryId}/location`, { latitude: p.coords.latitude, longitude: p.coords.longitude }).catch(() => {});
    }, (e) => setState(e.code === 1 ? 'denied' : 'error'), { enableHighAccuracy: true, maximumAge: 5000 });
    return () => navigator.geolocation.clearWatch(id);
  }, [active, deliveryId]);
  return state;
}

function Card({ a, reload }) {
  const toast = useToast(); const act = ACTION[a.status]; const gps = useGps(a.trackingActive, a.deliveryId);
  const go = async () => { try { await api.post(`/delivery/${a.deliveryId}/${act[0]}`); toast('Updated'); reload(); } catch (e) { toast(msg(e), 'err'); } };
  const nav = (lat, lng, addr) => lat != null ? `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}` : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(addr)}`;
  return <article className="card space-y-2 p-3 md:p-4"><div className="flex flex-wrap items-center justify-between gap-2"><b>{a.orderNumber}</b><StatusBadge s={a.status} /></div>
    <p className="text-sm"><b>Pickup:</b> {a.farmName}, {a.pickupAddress}</p><p className="text-sm"><b>Deliver to:</b> {a.customerName} · {a.customerMobile}<br />{a.dropAddress}</p>
    {a.codAmount && <p className="rounded bg-wheat-100 p-2 text-sm font-semibold">Collect cash: {inr(a.codAmount)}</p>}
    <MapView pickup={a.pickupLat != null ? { lat: a.pickupLat, lng: a.pickupLng } : null} drop={a.dropLat != null ? { lat: a.dropLat, lng: a.dropLng } : null} height="h-48" />
    {a.trackingActive && <p className="text-xs">{{ sharing: '📡 Sharing your live location with the customer', denied: 'Location permission denied. Customer sees milestones only.', unsupported: 'This device cannot share location.', error: 'Could not read your location.', idle: 'Waiting for GPS…' }[gps]}</p>}
    <div className="flex flex-wrap gap-2"><a className="btn-outline" target="_blank" rel="noreferrer" href={nav(a.pickupLat, a.pickupLng, a.pickupAddress)}>Navigate to farm</a><a className="btn-outline" target="_blank" rel="noreferrer" href={nav(a.dropLat, a.dropLng, a.dropAddress)}>Navigate to customer</a><a className="btn-outline" href={`tel:+91${a.customerMobile}`}>Call customer</a>{act && <button className="btn-primary" onClick={go}>{act[1]}</button>}</div>
    {a.status === 'FARMER_ACCEPTED' || a.status === 'PROCESSING' ? <p className="text-xs text-stone-500">The farmer is still preparing this order. Pickup opens when it is ready.</p> : null}</article>;
}

const DONE = ['DELIVERED', 'CANCELLED', 'REFUNDED', 'RETURN_REQUESTED'];
export function DeliveryHome() {
  const { data, loading, reload } = useFetch(() => api.get('/delivery/assignments')); useEffect(() => { const i = setInterval(reload, 30000); return () => clearInterval(i); }, []); // eslint-disable-line
  if (loading && !data) return <Spinner />; const active = (data || []).filter((a) => !DONE.includes(a.status));
  return <div className="space-y-3"><h1>Assigned deliveries</h1>{active.length ? active.map((a) => <Card key={a.deliveryId} a={a} reload={reload} />) : <Empty title="No active deliveries">New assignments from the admin appear here.</Empty>}</div>;
}
export function DeliveryHistory() {
  const { data, loading } = useFetch(() => api.get('/delivery/assignments')); if (loading) return <Spinner />; const done = data.filter((a) => DONE.includes(a.status));
  return <div className="space-y-3"><h1>Delivery history</h1>{done.length ? <div className="table-wrap"><table><thead><tr><th>Order</th><th>Farm</th><th>Customer</th><th>Status</th></tr></thead><tbody>{done.map((a) => <tr key={a.deliveryId}><td>{a.orderNumber}</td><td>{a.farmName}</td><td>{a.customerName}</td><td><StatusBadge s={a.status} /></td></tr>)}</tbody></table></div> : <Empty title="No completed deliveries yet" />}</div>;
}
