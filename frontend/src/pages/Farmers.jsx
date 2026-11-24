import { Link, useParams } from 'react-router-dom';
import { useState } from 'react';
import { api } from '../api';
import ProductCard from '../components/ProductCard';
import { Badge, Empty, Img, Input, Spinner, useFetch } from '../components/ui';

export function Farmers() {
  const [state, setState] = useState(''); const [district, setDistrict] = useState(''); const [pincode, setPin] = useState('');
  const { data, loading } = useFetch(() => api.get('/public/farmers', { params: { state, district, pincode } }), [state, district, pincode]);
  return <div><h1 className="mb-3">Meet the farmers</h1>
    <div className="mb-4 grid grid-cols-1 gap-2 sm:grid-cols-3"><Input label="State" value={state} onChange={(e) => setState(e.target.value)} /><Input label="District" value={district} onChange={(e) => setDistrict(e.target.value)} /><Input label="PIN code (nearby)" inputMode="numeric" maxLength={6} value={pincode} onChange={(e) => setPin(e.target.value)} /></div>
    {loading ? <Spinner /> : data?.length ? <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{data.map((f) => <Link key={f.id} to={`/farmers/${f.id}`} className="card flex gap-3 p-3 hover:border-field-400"><Img src={f.profileImageUrl} alt={f.farmName} className="h-16 w-16 shrink-0 rounded-full" /><div><p className="font-semibold">{f.farmName}</p><p className="text-sm text-stone-600">{f.farmerName} · {[f.district, f.state].filter(Boolean).join(', ')}</p><div className="mt-1 flex gap-1">{f.verificationStatus === 'APPROVED' ? <Badge>Verified</Badge> : <Badge tone="gray">Unverified</Badge>}{f.ratingCount > 0 && <Badge tone="amber">★ {f.ratingAverage} ({f.ratingCount})</Badge>}</div></div></Link>)}</div> : <Empty title="No farmers found" />}</div>;
}

export function FarmerPublic() {
  const { id } = useParams();
  const f = useFetch(() => api.get(`/public/farmers/${id}`), [id]);
  const prods = useFetch(() => api.get('/public/products', { params: { farmerId: id, size: 48 } }), [id]);
  const rev = useFetch(() => api.get(`/public/farmers/${id}/reviews`), [id]);
  if (f.loading) return <Spinner />; if (!f.data) return <Empty title="Farmer not found" />;
  const d = f.data;
  return <div className="space-y-6">
    <header className="card flex flex-col gap-4 p-4 md:flex-row"><Img src={d.profileImageUrl} alt={d.farmName} className="h-28 w-28 shrink-0 rounded-xl" />
      <div><h1>{d.farmName}</h1><p>{d.farmerName} · {d.farmLocation || [d.city, d.district, d.state].filter(Boolean).join(', ')}</p><div className="my-2 flex flex-wrap gap-2">{d.verificationStatus === 'APPROVED' ? <Badge>Verified by FarmDirect</Badge> : <Badge tone="gray">Verification pending</Badge>}<Badge tone="gray">{d.farmingCategory}</Badge>{d.ratingCount > 0 && <Badge tone="amber">★ {d.ratingAverage} from {d.ratingCount} reviews</Badge>}</div><p className="text-sm text-stone-700">{d.farmDetails}</p></div></header>
    {d.farmImages?.length > 0 && <div className="flex gap-2 overflow-x-auto">{d.farmImages.map((u) => <Img key={u} src={u} alt="Farm" className="h-32 w-48 shrink-0 rounded-lg" />)}</div>}
    <section><h2 className="mb-2">Products</h2>{prods.data?.content?.length ? <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-4">{prods.data.content.map((p) => <ProductCard key={p.id} p={p} />)}</div> : <Empty title="No products listed right now" />}</section>
    <section><h2 className="mb-2">Reviews</h2>{rev.data?.length ? rev.data.map((r) => <div key={r.id} className="border-b border-stone-100 py-2"><p className="text-sm font-semibold">{r.customerName} <span className="text-wheat-700">{'★'.repeat(r.rating)}</span></p><p className="text-sm text-stone-600">{r.comment}</p></div>) : <p className="text-sm text-stone-500">No reviews yet.</p>}</section>
  </div>;
}
