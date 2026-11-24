import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { api, msg } from '../api';
import { HOME, useAuth } from '../auth';
import PasswordMeter, { passwordValid } from '../components/PasswordMeter';
import { ErrorBox, Input, Select, useToast } from '../components/ui';

const Box = ({ title, sub, children }) => <div className="mx-auto w-full max-w-md py-4"><h1 className="mb-1 text-3xl">{title}</h1>{sub && <p className="mb-4 text-sm text-stone-600">{sub}</p>}<div className="space-y-3">{children}</div></div>;

export function Login() {
  const { user, login } = useAuth(); const nav = useNavigate();
  const [identifier, setId] = useState(''); const [password, setPw] = useState(''); const [show, setShow] = useState(false); const [err, setErr] = useState(''); const [busy, setBusy] = useState(false);
  if (user) return <Navigate to={HOME[user.role]} replace />;
  const submit = async (e) => { e.preventDefault(); setErr(''); setBusy(true);
    try { const { data } = await api.post('/auth/login', { identifier, password }); login(data); nav(HOME[data.role], { replace: true }); } catch (x) { setErr(msg(x)); } finally { setBusy(false); } };
  return <Box title="Log in" sub="Use your registered email address or mobile number.">
    <form onSubmit={submit} className="space-y-3"><ErrorBox error={err} />
      <Input label="Email or mobile number" required autoComplete="username" value={identifier} onChange={(e) => setId(e.target.value)} />
      <div><Input label="Password" required type={show ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={(e) => setPw(e.target.value)} />
        <label className="mt-1 flex items-center gap-2 text-xs"><input type="checkbox" checked={show} onChange={(e) => setShow(e.target.checked)} /> Show password</label></div>
      <button className="btn-primary w-full" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button></form>
    <p className="text-sm">New here? <Link className="font-semibold text-field-700 underline" to="/register">Create an account</Link></p></Box>;
}

const STATES = ['Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh', 'Goa', 'Gujarat', 'Haryana', 'Himachal Pradesh', 'Jharkhand', 'Karnataka', 'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya', 'Mizoram', 'Nagaland', 'Odisha', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu', 'Telangana', 'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal', 'Andaman and Nicobar Islands', 'Chandigarh', 'Dadra and Nagar Haveli and Daman and Diu', 'Delhi', 'Jammu and Kashmir', 'Ladakh', 'Lakshadweep', 'Puducherry'];

export function Register() {
  const nav = useNavigate(); const { login } = useAuth(); const toast = useToast(); const [f, setF] = useState({ role: 'CUSTOMER' }); const [err, setErr] = useState(''); const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value }); const farmer = f.role === 'FARMER', partner = f.role === 'DELIVERY_PARTNER';
  const submit = async (e) => { e.preventDefault(); setErr('');
    if (!passwordValid(f.password || '')) return setErr('Password does not meet all the rules below');
    if (f.password !== f.confirmPassword) return setErr('Passwords do not match');
    setBusy(true);
    try { const { data } = await api.post('/auth/register', f); login(data); toast('Welcome to FarmDirect!'); nav(HOME[data.role], { replace: true }); } catch (x) { setErr(msg(x)); } finally { setBusy(false); } };
  return <Box title="Create your account">
    <form onSubmit={submit} className="space-y-3"><ErrorBox error={err} />
      <Select label="I am a" value={f.role} onChange={set('role')}><option value="CUSTOMER">Customer</option><option value="FARMER">Farmer</option><option value="DELIVERY_PARTNER">Delivery partner</option></Select>
      <Input label="Full name" required autoComplete="name" onChange={set('fullName')} /><Input label="Email" type="email" required autoComplete="email" onChange={set('email')} />
      <Input label="Mobile number" type="tel" inputMode="numeric" required maxLength={13} hint="10-digit Indian mobile number" onChange={set('mobile')} />
      <div><Input label="Password" type="password" required autoComplete="new-password" onChange={set('password')} /><PasswordMeter value={f.password || ''} /></div>
      <Input label="Confirm password" type="password" required autoComplete="new-password" onChange={set('confirmPassword')} error={f.confirmPassword && f.confirmPassword !== f.password ? 'Passwords do not match' : ''} />
      <Select label="State" required defaultValue="" onChange={set('state')}><option value="" disabled>Select state</option>{STATES.map((s) => <option key={s}>{s}</option>)}</Select>
      <div className="grid grid-cols-2 gap-2"><Input label="District" required onChange={set('district')} /><Input label="Village or city" required onChange={set('city')} /></div>
      <Input label="Address" required onChange={set('address')} /><Input label="PIN code" required inputMode="numeric" maxLength={6} onChange={set('pincode')} />
      {farmer && <fieldset className="space-y-3 rounded-xl border border-field-200 p-3"><legend className="px-1 text-sm font-semibold">Farm details</legend>
        <Input label="Farm name" required onChange={set('farmName')} /><Input label="Farm address" required onChange={set('farmAddress')} /><Input label="Farm location (village, landmark)" onChange={set('farmLocation')} />
        <Select label="Farming category" required defaultValue="" onChange={set('farmingCategory')}><option value="" disabled>Select</option>{['Vegetables', 'Fruits', 'Dairy', 'Grains and cereals', 'Pulses', 'Spices and plantation', 'Mixed farming'].map((s) => <option key={s}>{s}</option>)}</Select>
        <Input label="About your farm" onChange={set('farmDetails')} /></fieldset>}
      {partner && <div className="grid grid-cols-2 gap-2"><Input label="Vehicle type" onChange={set('vehicleType')} /><Input label="Vehicle number" onChange={set('vehicleNumber')} /></div>}
      <button className="btn-primary w-full" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</button></form>
    <p className="text-sm">Already registered? <Link className="font-semibold text-field-700 underline" to="/login">Log in</Link></p></Box>;
}

