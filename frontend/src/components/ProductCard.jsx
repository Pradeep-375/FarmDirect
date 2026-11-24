import { Link } from 'react-router-dom';
import { useState } from 'react';
import { api, inr, msg, UNIT } from '../api';
import { useAuth } from '../auth';
import { Badge, Img, useToast } from './ui';
import { useTranslation } from 'react-i18next';

export default function ProductCard({ p }) {
  const { user } = useAuth(); const toast = useToast(); const { t } = useTranslation(); const [busy, setBusy] = useState(false);
  const soldOut = Number(p.available) < Number(p.minOrderQuantity);
  const add = async () => {
    if (!user) return toast('Please log in as a customer to shop', 'err');
    if (user.role !== 'CUSTOMER') return toast('Only customer accounts can shop', 'err');
    setBusy(true);
    try { await api.post('/customer/cart/items', { productId: p.id, quantity: p.minOrderQuantity }); toast('Added to cart'); window.dispatchEvent(new Event('fd:cart')); }
    catch (e) { toast(msg(e), 'err'); } finally { setBusy(false); }
  };
  return (
    <article className="card flex flex-col overflow-hidden">
      <Link to={`/products/${p.id}`} className="block"><Img src={p.imageUrls?.[0]} alt={p.name} className="aspect-[4/3] w-full" /></Link>
      <div className="flex flex-1 flex-col gap-1 p-3">
        <Link to={`/products/${p.id}`} className="font-semibold leading-snug hover:text-field-700">{p.name}</Link>
        <p className="text-lg font-bold text-field-800">{inr(p.price)} <span className="text-sm font-normal text-stone-500">/ {UNIT[p.unit]}</span></p>
        <p className="text-xs text-stone-600">{t('by')} <Link className="underline" to={`/farmers/${p.farmerId}`}>{p.farmerName}</Link> · {p.farmName}</p>
        <p className="text-xs text-stone-500">{p.farmLocation || [p.district, p.state].filter(Boolean).join(', ')}</p>
        <div className="flex flex-wrap items-center gap-1 text-xs">
          {soldOut ? <Badge tone="red">Out of stock</Badge> : <Badge>In stock</Badge>}
          {p.harvestDate && <span className="text-stone-500">{t('harvested')} {p.harvestDate}</span>}
          {!p.farmerProduced && <Badge tone="amber">Not farm-produced</Badge>}
        </div>
        <button className="btn-primary mt-auto" disabled={soldOut || busy} onClick={add}>{t('addToCart')}</button>
      </div>
    </article>
  );
}
