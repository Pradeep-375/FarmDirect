export const passwordChecks = (p) => [
  ['At least 12 characters', p.length >= 12], ['An uppercase letter', /[A-Z]/.test(p)], ['A lowercase letter', /[a-z]/.test(p)],
  ['A number', /\d/.test(p)], ['A special character', /[^A-Za-z0-9]/.test(p)],
];
export const passwordValid = (p) => passwordChecks(p).every(([, ok]) => ok);

export default function PasswordMeter({ value }) {
  const checks = passwordChecks(value), score = checks.filter(([, ok]) => ok).length;
  const label = ['Too weak', 'Too weak', 'Weak', 'Fair', 'Good', 'Strong'][score], color = score >= 5 ? 'bg-field-600' : score >= 3 ? 'bg-wheat-500' : 'bg-red-600';
  return (
    <div aria-live="polite">
      <div className="mt-2 h-1.5 w-full rounded bg-stone-200"><div className={`h-1.5 rounded transition-all ${color}`} style={{ width: `${(score / 5) * 100}%` }} /></div>
      <p className="mt-1 text-xs font-medium">{value ? label : ''}</p>
      <ul className="mt-1 grid grid-cols-1 gap-x-4 text-xs sm:grid-cols-2">{checks.map(([t, ok]) => <li key={t} className={ok ? 'text-field-700' : 'text-stone-500'}>{ok ? '✓' : '○'} {t}</li>)}</ul>
    </div>
  );
}
