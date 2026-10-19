/**
 * Summary tile used on the dashboard.
 * @param {import('react').ReactNode} icon
 * @param {'indigo'|'amber'|'green'|'red'|'blue'} tone
 */
export default function StatCard({ icon, value, label, tone = 'indigo' }) {
  return (
    <div className="stat-card">
      <div className={`stat-icon ${tone}`} aria-hidden="true">
        {icon}
      </div>
      <div className="grow">
        <div className="stat-value">{value}</div>
        <div className="stat-label">{label}</div>
      </div>
    </div>
  );
}
