import { useEffect, useState } from 'react';
import { useSearchParams, useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { api, inr, msg, UNIT } from '../api';
import { useAuth } from '../auth';
import ProductCard from '../components/ProductCard';
import { Badge, Empty, Img, Pager, Select, Input, Spinner, useFetch, useToast } from '../components/ui';

export function Products() {
  const { t } = useTranslation(); const [sp, setSp] = useSearchParams();
  const [q, setQ] = useState(sp.get('q') || ''); const [showFilters, setShow] = useState(false);
  const params = Object.fromEntries(sp.entries()); const page = Number(params.page || 0);
  const cats = useFetch(() => api.get('/public/categories'));
  const { data, loading } = useFetch(() => api.get('/public/products', { params: { ...params, page, size: 12 } }), [sp.toString()]);
  const setParam = (k, v) => { const n = new URLSearchParams(sp); v ? n.set(k, v) : n.delete(k); if (k !== 'page') n.delete('page'); setSp(n); };
  return (
    <div className="lg:grid lg:grid-cols-[15rem_1fr] lg:gap-6">
      <form onSubmit={(e) => { e.preventDefault(); setParam('q', q); }} className="mb-3 flex gap-2 lg:col-span-2"><input aria-label={t('search')} className="input" placeholder={`${t('search')}: tomatoes, milk, a farm…`} value={q} onChange={(e) => setQ(e.target.value)} /><button className="btn-primary">{t('search')}</button>
        <button type="button" className="btn-outline lg:hidden" onClick={() => setShow(!showFilters)}>Filters</button></form>
      <aside className={`${showFilters ? 'block' : 'hidden'} mb-4 space-y-3 lg:mb-0 lg:block`} aria-label="Filters">
        <Select label="Category" value={params.categoryId || ''} onChange={(e) => setParam('categoryId', e.target.value)}><option value="">All categories</option>{cats.data?.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</Select>
        <div className="grid grid-cols-2 gap-2"><Input label="Min ₹" type="number" min="0" value={params.minPrice || ''} onChange={(e) => setParam('minPrice', e.target.value)} /><Input label="Max ₹" type="number" min="0" value={params.maxPrice || ''} onChange={(e) => setParam('maxPrice', e.target.value)} /></div>
        <Input label="State" value={params.state || ''} onChange={(e) => setParam('state', e.target.value)} /><Input label="District" value={params.district || ''} onChange={(e) => setParam('district', e.target.value)} />
        <Input label="PIN code (nearby farms)" inputMode="numeric" maxLength={6} value={params.pincode || ''} onChange={(e) => setParam('pincode', e.target.value)} />
        <Select label="Sort by" value={params.sort || 'newest'} onChange={(e) => setParam('sort', e.target.value)}><option value="newest">Newest</option><option value="priceAsc">Price: low to high</option><option value="priceDesc">Price: high to low</option></Select>
        <button className="btn-outline w-full" onClick={() => setSp({})}>Clear filters</button>
      </aside>
      <div>{loading ? <Spinner /> : data?.content?.length ? <><div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-4">{data.content.map((p) => <ProductCard key={p.id} p={p} />)}</div><Pager page={page} totalPages={data.totalPages} onChange={(n) => { setParam('page', n); window.scrollTo(0, 0); }} /></> : <Empty title="No products match">Try removing a filter.</Empty>}</div>
    </div>
  );
}

export function ProductDetail() {
  const { id } = useParams(); const { user } = useAuth(); const toast = useToast();
  const { data: p, loading, error } = useFetch(() => api.get(`/public/products/${id}`), [id]);
  const reviews = useFetch(() => api.get(`/public/products/${id}/reviews`), [id]);
  const [qty, setQty] = useState(''); const [img, setImg] = useState(0);
  useEffect(() => { if (p) setQty(p.minOrderQuantity); }, [p]);
  if (loading) return <Spinner />; if (error || !p) return <Empty title="Product not found" />;
  const add = async () => {
    if (user?.role !== 'CUSTOMER') return toast('Log in with a customer account to shop', 'err');
    try { await api.post('/customer/cart/items', { productId: p.id, quantity: qty }); toast('Added to cart'); window.dispatchEvent(new Event('fd:cart')); } catch (e) { toast(msg(e), 'err'); }
  };
  const wish = async () => { try { await api.post(`/customer/wishlist/${p.id}`); toast('Saved to wishlist'); } catch (e) { toast(user ? msg(e) : 'Log in to save products', 'err'); } };
  const out = Number(p.available) < Number(p.minOrderQuantity);
  return (
    <div className="grid gap-6 md:grid-cols-2">
      <div><Img src={p.imageUrls?.[img]} alt={p.name} className="aspect-square w-full rounded-xl" />{p.imageUrls?.length > 1 && <div className="mt-2 flex gap-2 overflow-x-auto">{p.imageUrls.map((u, i) => <button key={u} onClick={() => setImg(i)} aria-label={`Image ${i + 1}`}><Img src={u} alt="" className={`h-16 w-16 rounded-lg ${i === img ? 'ring-2 ring-field-600' : ''}`} /></button>)}</div>}</div>
      <div className="space-y-3">
        <h1>{p.name}</h1>
        <p className="text-2xl font-bold text-field-800">{inr(p.price)} <span className="text-base font-normal text-stone-500">per {UNIT[p.unit]}</span></p>
        <div className="flex flex-wrap gap-2">{out ? <Badge tone="red">Out of stock</Badge> : <Badge>{Number(p.available)} {UNIT[p.unit]} available</Badge>}<Badge tone="gray">{p.categoryName}</Badge>{p.farmerProduced ? <Badge>Produced by the farmer</Badge> : <Badge tone="amber">Not farm-produced</Badge>}</div>
        {p.sourceNote && <p className="text-sm text-stone-600">Source: {p.sourceNote}</p>}
        {p.harvestDate && <p className="text-sm">Harvested on {p.harvestDate}</p>}
        <p className="whitespace-pre-line text-stone-700">{p.description}</p>
        <div className="card p-3"><p className="text-sm text-stone-500">Grown by</p><Link className="font-semibold text-field-800 underline" to={`/farmers/${p.farmerId}`}>{p.farmName}</Link><p className="text-sm">{p.farmerName} · {p.farmLocation || [p.district, p.state].filter(Boolean).join(', ')}</p><div className="mt-1 flex gap-2">{p.farmerVerified ? <Badge>Verified farm</Badge> : <Badge tone="gray">Verification pending</Badge>}{p.farmerRating > 0 && <Badge tone="amber">★ {p.farmerRating}</Badge>}</div></div>
        <div className="flex flex-wrap items-end gap-2"><Input label={`Quantity (min ${p.minOrderQuantity})`} type="number" min={p.minOrderQuantity} step="any" value={qty} onChange={(e) => setQty(e.target.value)} className="w-40" /><button className="btn-primary" disabled={out} onClick={add}>Add to cart</button><button className="btn-outline" onClick={wish}>♡ Wishlist</button></div>
      </div>
      <section className="md:col-span-2"><h2 className="mb-2">Customer reviews</h2>{reviews.data?.length ? reviews.data.map((r) => <div key={r.id} className="border-b border-stone-100 py-2"><p className="text-sm font-semibold">{r.customerName} <span className="text-wheat-700">{'★'.repeat(r.rating)}</span></p><p className="text-sm text-stone-600">{r.comment}</p></div>) : <p className="text-sm text-stone-500">No reviews yet.</p>}</section>
    </div>
  );
}
