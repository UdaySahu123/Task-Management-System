/**
 * Placeholder shown when a list has no data.
 * @param {import('react').ReactNode} icon
 * @param {import('react').ReactNode} action - optional call-to-action button
 */
export default function EmptyState({ icon, title, message, action }) {
  return (
    <div className="empty-state">
      {icon && <div className="empty-icon">{icon}</div>}
      <h3 className="task-card-title">{title}</h3>
      {message && <p className="muted mt-sm">{message}</p>}
      {action && <div className="mt">{action}</div>}
    </div>
  );
}
