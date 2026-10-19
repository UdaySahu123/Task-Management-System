/* Formatting helpers shared across the UI. */

const dateFormatter = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
});

const dateTimeFormatter = new Intl.DateTimeFormat('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});

/** "2026-10-19" -> "19 Oct 2026" */
export const formatDate = (value) => {
  if (!value) return '—';
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : dateFormatter.format(date);
};

/** ISO timestamp -> "19 Oct 2026, 09:30" */
export const formatDateTime = (value) => {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : dateTimeFormatter.format(date);
};

/** Today's date as a yyyy-MM-dd string (for <input type="date"> defaults). */
export const todayIso = () => new Date().toISOString().slice(0, 10);

/**
 * Friendly due-date text, relative to today.
 * Returns { text, overdue }.
 */
export const describeDueDate = (value, isCompleted = false) => {
  if (!value) return { text: 'No due date', overdue: false };

  const due = new Date(value);
  const today = new Date();
  due.setHours(0, 0, 0, 0);
  today.setHours(0, 0, 0, 0);

  const diffDays = Math.round((due - today) / (1000 * 60 * 60 * 24));

  if (due.getTime() === today.getTime()) return { text: 'Due today', overdue: false };
  if (diffDays === 1) return { text: 'Due tomorrow', overdue: false };
  if (due < today) {
    const days = Math.abs(diffDays);
    return {
      text: `Overdue by ${days} day${days === 1 ? '' : 's'}`,
      overdue: !isCompleted,
    };
  }
  return { text: `Due in ${diffDays} days`, overdue: false };
};

/* Enum labels for display. Values match the backend enums exactly. */
export const STATUS_LABELS = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
};

export const PRIORITY_LABELS = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
};

export const TASK_STATUSES = ['TODO', 'IN_PROGRESS', 'COMPLETED'];
export const TASK_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];

export const statusLabel = (status) => STATUS_LABELS[status] || status;
export const priorityLabel = (priority) => PRIORITY_LABELS[priority] || priority;

/** Initials for the avatar, e.g. "Alice Smith" -> "AS". */
export const initials = (name = '') =>
  name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() || '')
    .join('') || '?';
