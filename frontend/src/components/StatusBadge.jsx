import { statusLabel } from '../utils/format';

const CLASSES = {
  TODO: 'badge-todo',
  IN_PROGRESS: 'badge-in_progress',
  COMPLETED: 'badge-completed',
};

export default function StatusBadge({ status }) {
  return <span className={`badge ${CLASSES[status] || 'badge-todo'}`}>{statusLabel(status)}</span>;
}
