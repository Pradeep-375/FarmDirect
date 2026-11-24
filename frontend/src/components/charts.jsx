import { ArcElement, BarElement, CategoryScale, Chart as ChartJS, Legend, LinearScale, LineElement, PointElement, Tooltip } from 'chart.js';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
ChartJS.register(ArcElement, BarElement, CategoryScale, Legend, LinearScale, LineElement, PointElement, Tooltip);

const GREENS = ['#1f5b36', '#5aab72', '#e0aa2b', '#a97a14', '#8cc89a', '#5a3e2b', '#b9dfc0'];
const opts = { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } };
const Wrap = ({ children, h = 'h-56 md:h-72' }) => <div className={`${h} w-full`}>{children}</div>;
export const BarChart = ({ labels, values, label }) => <Wrap><Bar options={opts} data={{ labels, datasets: [{ label, data: values, backgroundColor: '#277241', borderRadius: 4 }] }} /></Wrap>;
export const LineChart = ({ labels, values, label }) => <Wrap><Line options={opts} data={{ labels, datasets: [{ label, data: values, borderColor: '#1f5b36', backgroundColor: '#b9dfc0', fill: true, tension: 0.3 }] }} /></Wrap>;
export const DonutChart = ({ labels, values }) => <Wrap><Doughnut options={{ ...opts, plugins: { legend: { display: true, position: 'bottom' } } }} data={{ labels, datasets: [{ data: values, backgroundColor: GREENS }] }} /></Wrap>;
export const Stat = ({ label, value, tone }) => <div className={`card p-3 md:p-4 ${tone === 'warn' ? 'border-wheat-300 bg-wheat-100' : ''}`}><p className="text-xs text-stone-600">{label}</p><p className="font-display text-2xl text-field-900">{value}</p></div>;
