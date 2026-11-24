import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { api } from '../api';
import ProductCard from '../components/ProductCard';
import { Empty, Spinner, useFetch } from '../components/ui';

export default function Home() {
  const { t } = useTranslation();
  const { data, loading } = useFetch(() => api.get('/public/products', { params: { size: 8 } }));
  const cats = useFetch(() => api.get('/public/categories'));
  return (
    <div className="space-y-10">
      <section className="rounded-2xl bg-field-50 p-5 md:p-10">
        <h1 className="max-w-3xl">{t('tagline')}</h1>
        <p className="mt-3 max-w-xl text-stone-700">{t('sub')}</p>
        <div className="mt-5 flex flex-wrap gap-3"><Link to="/products" className="btn-primary">{t('shop')}</Link><Link to="/register" className="btn-outline">{t('sell')}</Link></div>
      </section>
      {cats.data && <section aria-label="Categories" className="flex flex-wrap gap-2">{cats.data.map((c) => <Link key={c.id} to={`/products?categoryId=${c.id}`} className="rounded-full border border-field-200 px-4 py-2 text-sm hover:bg-field-50">{c.name}</Link>)}</section>}
      <section>
        <div className="mb-3 flex items-baseline justify-between"><h2>Newly listed by farmers</h2><Link to="/products" className="text-sm font-semibold text-field-700">{t('viewAll')}</Link></div>
        {loading ? <Spinner /> : data?.content?.length ? <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-4 lg:gap-4">{data.content.map((p) => <ProductCard key={p.id} p={p} />)}</div>
          : <Empty title="No products listed yet">Farmers can register and list their harvest from their dashboard.</Empty>}
      </section>
      <section className="grid gap-4 md:grid-cols-3">
        {[['Direct from farms', 'Every product shows who grew it, where, and when it was harvested.'], ['Prices set by farmers', 'No middlemen. The farmer decides the price and keeps the sale.'], ['Track to your door', 'Follow your order from the farm to delivery, live when GPS sharing is on.']].map(([h, b]) => <div key={h} className="card p-4"><h3 className="text-lg">{h}</h3><p className="mt-1 text-sm text-stone-600">{b}</p></div>)}
      </section>
    </div>
  );
}
