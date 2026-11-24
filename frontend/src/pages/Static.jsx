import { useState } from 'react';
import { api, msg } from '../api';
import { ErrorBox, Input, Field } from '../components/ui';

export const About = () => (
  <div className="max-w-3xl space-y-4"><h1>About FarmDirect</h1>
    <p>FarmDirect connects farmers across India directly with customers. Farmers list what they grow, set their own prices and manage their own stock. Customers see who produced their food and follow each order until it arrives.</p>
    <p>The platform is built for farmer-produced goods. Anything not grown or made on the farm is labelled, and we never mark a product organic or certified without documentation.</p></div>
);

export function Contact() {
  const [f, setF] = useState({ name: '', email: '', subject: '', message: '' }); const [done, setDone] = useState(false); const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => { e.preventDefault(); setErr(''); try { await api.post('/public/contact', f); setDone(true); } catch (x) { setErr(msg(x)); } };
  if (done) return <p className="rounded-xl bg-field-50 p-6">Thanks. We have your message and will reply by email.</p>;
  return <form onSubmit={submit} className="max-w-xl space-y-3"><h1>Contact us</h1><ErrorBox error={err} />
    <Input label="Your name" required value={f.name} onChange={set('name')} /><Input label="Email" type="email" required value={f.email} onChange={set('email')} />
    <Input label="Subject" required value={f.subject} onChange={set('subject')} />
    <Field label="Message"><textarea className="input" rows={5} required maxLength={2000} value={f.message} onChange={set('message')} /></Field>
    <button className="btn-primary">Send message</button></form>;
}
