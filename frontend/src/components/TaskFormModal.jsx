import { useEffect, useState } from 'react';
import { X } from 'lucide-react';
import { createTask, updateTask } from '../api/tasks';
import { getErrorMessage, getFieldErrors } from '../api/client';
import { useToast } from '../context/ToastContext';
import { TASK_PRIORITIES, TASK_STATUSES, PRIORITY_LABELS, STATUS_LABELS, todayIso } from '../utils/format';

const emptyForm = () => ({
  title: '',
  description: '',
  dueDate: todayIso(),
  status: 'TODO',
  priority: 'MEDIUM',
});

/** Create/edit dialog for a task. Handles its own submit + validation. */
export default function TaskFormModal({ open, task, onClose, onSaved }) {
  const { toast } = useToast();
  const [form, setForm] = useState(emptyForm);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const isEdit = Boolean(task);

  // (Re)initialise the form whenever the dialog opens or the task changes.
  useEffect(() => {
    if (!open) return;
    setError('');
    setFieldErrors({});
    setForm(
      task
        ? {
            title: task.title || '',
            description: task.description || '',
            dueDate: task.dueDate || todayIso(),
            status: task.status || 'TODO',
            priority: task.priority || 'MEDIUM',
          }
        : emptyForm(),
    );
  }, [open, task]);

  if (!open) return null;

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }));
    setFieldErrors((current) => ({ ...current, [field]: undefined }));
  };

  const validate = () => {
    const errors = {};
    if (!form.title.trim()) errors.title = 'Title is required';
    else if (form.title.trim().length > 200) errors.title = 'Title must be at most 200 characters';
    if (form.description.length > 2000) errors.description = 'Description must be at most 2000 characters';
    if (!form.dueDate) errors.dueDate = 'Due date is required';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    if (!validate()) return;

    const payload = {
      title: form.title.trim(),
      description: form.description.trim() || null,
      dueDate: form.dueDate,
      status: form.status,
      priority: form.priority,
    };

    setSaving(true);
    try {
      const saved = isEdit ? await updateTask(task.id, payload) : await createTask(payload);
      toast.success(isEdit ? 'Task updated' : 'Task created');
      onSaved?.(saved);
      onClose();
    } catch (err) {
      setFieldErrors(getFieldErrors(err));
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onMouseDown={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="task-form-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <form onSubmit={handleSubmit}>
          <div className="modal-header">
            <h2 className="modal-title" id="task-form-title">
              {isEdit ? 'Edit task' : 'New task'}
            </h2>
            <button type="button" className="btn-icon" aria-label="Close" onClick={onClose}>
              <X size={18} />
            </button>
          </div>

          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}

            <div className="form-group">
              <label className="form-label" htmlFor="task-title">
                Title
              </label>
              <input
                id="task-title"
                className={`form-input${fieldErrors.title ? ' is-invalid' : ''}`}
                value={form.title}
                onChange={update('title')}
                placeholder="e.g. Prepare sprint report"
                maxLength={200}
              />
              {fieldErrors.title && <p className="form-error">{fieldErrors.title}</p>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="task-description">
                Description <span className="muted">(optional)</span>
              </label>
              <textarea
                id="task-description"
                className={`form-textarea${fieldErrors.description ? ' is-invalid' : ''}`}
                value={form.description}
                onChange={update('description')}
                placeholder="Add any details, acceptance criteria or links…"
              />
              {fieldErrors.description && <div className="form-error">{fieldErrors.description}</div>}
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="task-due">
                  Due date
                </label>
                <input
                  id="task-due"
                  type="date"
                  className={`form-input${fieldErrors.dueDate ? ' is-invalid' : ''}`}
                  value={form.dueDate}
                  onChange={update('dueDate')}
                />
                {fieldErrors.dueDate && <p className="form-error">{fieldErrors.dueDate}</p>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="task-priority">
                  Priority
                </label>
                <select id="task-priority" className="form-select" value={form.priority} onChange={update('priority')}>
                  {TASK_PRIORITIES.map((value) => (
                    <option key={value} value={value}>
                      {PRIORITY_LABELS[value]}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="task-status">
                Status
              </label>
              <select id="task-status" className="form-select" value={form.status} onChange={update('status')}>
                {TASK_STATUSES.map((value) => (
                  <option key={value} value={value}>
                    {STATUS_LABELS[value]}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose} disabled={saving}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving && <span className="spinner" />}
              {isEdit ? 'Save changes' : 'Create task'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
