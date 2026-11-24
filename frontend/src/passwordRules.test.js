import { describe, expect, it } from 'vitest';
import { passwordChecks } from './components/PasswordMeter.jsx';

describe('password rules', () => {
  const ok = (p) => passwordChecks(p).every(([, v]) => v);
  it('accepts a strong password', () => expect(ok('Farm#Direct2026!')).toBe(true));
  it('rejects short and incomplete passwords', () => { expect(ok('Ab1!short')).toBe(false); expect(ok('alllowercase1234!')).toBe(false); expect(ok('NoSpecials12345Ab')).toBe(false); });
});
