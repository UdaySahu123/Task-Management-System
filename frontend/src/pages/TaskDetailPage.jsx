import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Pencil, Trash2 } from 'lucide-react';
import { deleteTask, getTask, updateTaskPriority, updateTaskStatus } from '../api/tasks';
import { getErrorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import TaskFormModal from '../components/TaskFormModal';
import ConfirmDialog from '../components/ConfirmDialog';
import Spinner from '../components/Spinner';
import { TASK_PRIORITIES, TASK_STATUSES, PRIORITY_LABELS, STATUS_LABELS, formatDateTime } from '../utils/format';

export default function TaskDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [task, setTask] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setTask(await getTask(id));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  const changeStatus = async (status) => {
    try {
      setTask(await updateTaskStatus(id, status));
      toast.success('Status updated');
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const changePriority = async (priority) => {
    try {
      setTask(await updateTaskPriority(id, priority));
      toast.success('Priority updated');
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const handleDelete = async () => {
    try {
      await deleteTask(id);
      toast.success('Task deleted');
      navigate('/tasks', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  if (loading) {
    return (
      <div className="page-loader">
        <Spinner variant="dark" size={26} />
      </div>
    );
  }

  if (error || !task) {
    return (
      <div>
        <Link to="/tasks" className="row gap-sm" style={{ marginBottom: 16 }}>
          <ArrowLeft size={16} />
          Back to tasks
        </Link>
        <div className="alert alert-error">{error || 'Task not found'}</div>
      </div>
    );
  }

  return (
    <div>
      <Link to="/tasks" className="row gap-sm muted" style={{ marginBottom: 16 }}>
        <ArrowLeft size={16} />
        Back to tasks
      </Link>

      <div className="row-between wrap gap" style={{ marginBottom: 20 }}>
        <h2 className="page-title">{task.title}</h2>
        <div className="row gap-sm">
          <button type="button" className="btn btn-secondary" onClick={() => setEditing(true)}>
            <Pencil size={15} />
            Edit
          </button>
          <button type="button" className="btn btn-danger" onClick={() => setConfirming(true)}>
            <Trash2 size={15} />
            Delete
          </button>
        </div>
      </div>

      <div className="detail-grid">
        <section className="card">
          <div className="modal-header">
            <h3 className="modal-title">Description</h3>
          </div>
          <div className="modal-body">
            <p className="muted" style={{ whiteSpace: 'pre-wrap' }}>
              {task.description || 'No description provided.'}
            </p>
          </div>
        </section>

        <section className="card">
          <div className="modal-header">
            <h3 className="modal-title">Details</h3>
          </div>
          <div className="modal-body">
            <div className="form-group">
              <label className="form-label" htmlFor="detail-status">
                Status
              </label>
              <select
                id="detail-status"
                className="form-select"
                value={task.status}
                onChange={(event) => changeStatus(event.target.value)}
              >
                {TASK_STATUSES.map((value) => (
                  <option key={value} value={value}>
                    {STATUS_LABELS[value]}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="detail-priority">
                Priority
              </label>
              <select
                id="detail-priority"
                className="form-select"
                value={task.priority}
                onChange={(event) => changePriority(event.target.value)}
              >
                {TASK_PRIORITIES.map((value) => (
                  <option key={value} value={value}>
                    {PRIORITY_LABELS[value]}
                  </option>
                ))}
              </select>
            </div>

            <dl className="detail-list">
              <dt>Due date</dt>
              <dd>
                {task.dueDate || '—'}
                {task.overdue ? ' · overdue' : ''}
              </dd>
              <dt>Created</dt>
              <dd>{formatDateTime(task.createdAt)}</dd>
              <dt>Updated</dt>
              <dd>{formatDateTime(task.updatedAt)}</dd>
            </dl>
          </div>
        </section>
      </div>

      <TaskFormModal
        open={editing}
        task={task}
        onClose={() => setEditing(false)}
        onSaved={(saved) => setTask(saved)}
      />

      <ConfirmDialog
        open={confirming}
        title="Delete task"
        message={`"${task.title}" will be permanently deleted.`}
        confirmLabel="Delete"
        onConfirm={handleDelete}
        onCancel={() => setConfirming(false)}
      />
    </div>
  );
}
