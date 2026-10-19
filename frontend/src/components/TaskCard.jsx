import { Link } from 'react-router-dom';
import { Pencil, Trash2, CalendarClock } from 'lucide-react';
import StatusBadge from './StatusBadge';
import PriorityBadge from './PriorityBadge';
import { formatDate } from '../utils/format';

/**
 * Task summary card.
 *
 * @param {object} task
 * @param {(task: object) => void} onEdit
 * @param {(task: object) => void} onDelete
 */
export default function TaskCard({ task, onEdit, onDelete }) {
  const overdue = task.overdue;

  return (
    <article className={`task-card priority-${task.priority?.toLowerCase()}${overdue ? ' overdue' : ''}`}>
      <div className="row-between gap-sm">
        <Link to={`/tasks/${task.id}`} className="task-card-title truncate grow">
          {task.title}
        </Link>
        <PriorityBadge priority={task.priority} />
      </div>

      {task.description ? (
        <p className="task-card-desc">{task.description}</p>
      ) : (
        <p className="task-card-desc muted">No description</p>
      )}

      <div className="task-card-meta">
        <span className={`row gap-sm${overdue ? ' badge-overdue' : ''}`}>
          <CalendarClock size={14} aria-hidden="true" />
          {formatDate(task.dueDate)}
          {overdue && <strong>· Overdue</strong>}
        </span>
        <StatusBadge status={task.status} />
      </div>

      <div className="task-card-footer">
        <span className="muted" style={{ fontSize: 12 }}>
          Updated {formatDate(task.updatedAt)}
        </span>
        <div className="task-card-actions">
          <button
            type="button"
            className="btn-icon"
            aria-label={`Edit ${task.title}`}
            onClick={() => onEdit(task)}
          >
            <Pencil size={16} />
          </button>
          <button
            type="button"
            className="btn-icon"
            aria-label={`Delete ${task.title}`}
            onClick={() => onDelete(task)}
          >
            <Trash2 size={16} />
          </button>
        </div>
      </div>
    </article>
  );
}
