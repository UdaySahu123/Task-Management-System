import { priorityLabel } from '../utils/format';

const CLASSES = {
  LOW: 'badge-low',
  MEDIUM: 'badge-medium',
  HIGH: 'badge-high',
};

export default function PriorityBadge({ priority }) {
  return (
    <span className={`badge ${CLASSES[priority] || 'badge-low'}`}>
      {priorityLabel(priority)} priority
    </span>
  );
}
