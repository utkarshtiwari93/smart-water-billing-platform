const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
const num = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 });

export const money = (v) => (v === null || v === undefined || v === '' ? '-' : inr.format(Number(v)));
export const number = (v) => (v === null || v === undefined || v === '' ? '-' : num.format(Number(v)));
export const litres = (v) => `${number(v)} L`;
export const kl = (v) => `${num.format(Number(v))} kL`;

export const dateStr = (d) => {
  if (!d) return '-';
  const x = new Date(d.length === 10 ? `${d}T00:00:00` : d);
  return x.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
};

export const dateTimeStr = (d) => (d ? new Date(d).toLocaleString('en-IN') : '-');

export const isoDate = (d) => {
  const x = d instanceof Date ? d : new Date(d);
  const p = (n) => String(n).padStart(2, '0');
  return `${x.getFullYear()}-${p(x.getMonth() + 1)}-${p(x.getDate())}`;
};

export const daysAgo = (n) => {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return isoDate(d);
};

export const today = () => isoDate(new Date());
