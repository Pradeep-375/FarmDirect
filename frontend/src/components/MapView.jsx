import { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

const dot = (c, label) => L.divIcon({ className: '', html: `<div style="background:${c};color:#fff;width:28px;height:28px;border-radius:50%;border:3px solid #fff;box-shadow:0 1px 4px #0006;display:flex;align-items:center;justify-content:center;font:700 12px sans-serif">${label}</div>`, iconSize: [28, 28], iconAnchor: [14, 14] });
const ICONS = { pickup: dot('#1f5b36', 'F'), drop: dot('#a97a14', 'C'), partner: dot('#1d4ed8', '🚚') };

function Fit({ points }) {
  const map = useMap();
  useEffect(() => { if (points.length > 1) map.fitBounds(points, { padding: [40, 40] }); else if (points.length === 1) map.setView(points[0], 14); }, [JSON.stringify(points)]); // eslint-disable-line
  return null;
}

/** Shows only coordinates that were actually supplied. A route line is drawn only when VITE_OSRM_URL points to a routing service. */
export default function MapView({ pickup, drop, partner, height = 'h-72 lg:h-[28rem]' }) {
  const ok = (p) => p && p.lat != null && p.lng != null;
  const pts = [pickup, drop, partner].filter(ok).map((p) => [p.lat, p.lng]);
  const [route, setRoute] = useState(null);
  const osrm = import.meta.env.VITE_OSRM_URL;
  useEffect(() => {
    setRoute(null);
    if (!osrm || !ok(pickup) || !ok(drop)) return;
    const from = ok(partner) ? partner : pickup;
    fetch(`${osrm}/route/v1/driving/${from.lng},${from.lat};${drop.lng},${drop.lat}?overview=full&geometries=geojson`)
      .then((r) => r.json()).then((j) => j.routes?.[0] && setRoute(j.routes[0].geometry.coordinates.map(([x, y]) => [y, x]))).catch(() => {});
  }, [osrm, pickup?.lat, pickup?.lng, drop?.lat, drop?.lng, partner?.lat, partner?.lng]); // eslint-disable-line
  if (!pts.length) return <div className={`flex ${height} items-center justify-center rounded-xl bg-stone-100 p-4 text-center text-sm text-stone-600`}>Map locations are not available for this order. Use the status timeline for progress.</div>;
  return (
    <MapContainer center={pts[0]} zoom={13} scrollWheelZoom={false} className={`${height} w-full rounded-xl`}>
      <TileLayer attribution='&copy; OpenStreetMap contributors' url="https://tile.openstreetmap.org/{z}/{x}/{y}.png" />
      {ok(pickup) && <Marker position={[pickup.lat, pickup.lng]} icon={ICONS.pickup}><Popup>Farm pickup</Popup></Marker>}
      {ok(drop) && <Marker position={[drop.lat, drop.lng]} icon={ICONS.drop}><Popup>Delivery address</Popup></Marker>}
      {ok(partner) && <Marker position={[partner.lat, partner.lng]} icon={ICONS.partner}><Popup>Delivery partner</Popup></Marker>}
      {route && <Polyline positions={route} pathOptions={{ color: '#1f5b36', weight: 4 }} />}
      <Fit points={pts} />
    </MapContainer>
  );
}
