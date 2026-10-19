import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  CalendarClock,
  CheckCircle2,
  ClipboardList,
  Loader,
  Plus,
  RefreshCw,
} from 'lucide-react';
import { getDashboardStats } from '../api/dashboard';
import { getErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import StatCard from '../components/StatCard';
import ProgressBar from '../components/ProgressBar';
import StatusBadge from '../components/StatusBadge';
import EmptyState from '../components/EmptyState';
import TaskFormModal from '../components/TaskFormModal';
import { StatSkeletonRow } from '../components/Skeleton';
import { formatDate } from '../utils/format';

export default function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [creating, setCreating] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setStats(await getDashboardStats());
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const firstName = user?.name?.split(' ')[0] || 'there';

  return (
    <div>
      <div className="row-between wrap gap mt-sm" style={{ marginBottom: 22 }}>
        <div>
          <h2 className="page-title">Hi {firstName}, here's your overview</h2>
          <p className="page-subtitle">A snapshot of everything on your plate.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}>
          <Plus size={16} />
          New task
        </button>
      </div>

      {error && (
        <div className="alert alert-error">
          <span className="grow">{error}</span>
          <button type="button" className="btn btn-sm btn-secondary" onClick={load}>
            <RefreshCw size={14} />
            Retry
          </button>
        </div>
      )}

      {loading && !stats ? (
        <StatSkeletonRow count={4} />
      ) : (
        stats && (
          <>
            <div className="task-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
              <StatCard tone="indigo" icon={<ClipboardList size={20} />} value={stats.totalTasks} label="Total tasks" />
              <StatCard tone="blue" icon={<Loader size={20} />} value={stats.inProgressTasks} label="In progress" />
              <StatCard tone="green" icon={<CheckCircle2 size={20} />} value={stats.completedTasks} label="Completed" />
              <StatCard tone="red" icon={<AlertTriangle size={20} />} value={stats.overdueTasks} label="Overdue" />
            </div>

            <div className="card mt">
              <div className="modal-body">
                <ProgressBar
                  value={stats.completionPercentage}
                  label={`Completion — ${stats.completedTasks} of ${stats.totalTasks} done`}
                />
              </div>
            </div>

            <div className="detail-grid mt-lg">
              <section className="card">
                <div className="modal-header">
                  <h3 className="modal-title">Recent tasks</h3>
                  <Link to="/tasks" className="btn btn-sm btn-secondary">
                    View all
                  </Link>
                </div>
                <div className="modal-body">
                  {stats.recentTasks?.length ? (
                    stats.recentTasks.map((task) => (
                      <div key={task.id} className="list-row">
                        <Link to={`/tasks/${task.id}`} className="grow truncate">
                          {task.title}
                        </Link>
                        <StatusBadge status={task.status} />
                      </div>
                    ))
                  ) : (
                    <p className="muted">No tasks yet. Create your first one!</p>
                  )}
                </div>
              </section>

              <section className="card">
                <div className="modal-header">
                  <h3 className="modal-title">Upcoming deadlines</h3>
                  <CalendarClock size={18} className="muted" />
                </div>
                <div className="modal-body">
                  {stats.upcomingDeadlines?.length ? (
                    stats.upcomingDeadlines.map((task) => (
                      <div key={task.id} className="list-row">
                        <Link to={`/tasks/${task.id}`} className="grow truncate">
                          {task.title}
                        </Link>
                        <span className="muted" style={{ fontSize: 13 }}>
                          {formatDate(task.dueDate)}
                        </span>
                      </div>
                    ))
                  ) : (
                    <p className="muted">Nothing due soon. Nice work!</p>
                  )}
                </div>
              </section>
            </div>
          </>
        )
      )}

      {!loading && !stats && !error && (
        <EmptyState
          icon={<ClipboardList size={24} />}
          title="No data yet"
          message="Create a task to see your dashboard come to life."
        />
      )}

      <TaskFormModal
        open={creating}
        task={null}
        onClose={() => setCreating(false)}
        onSaved={load}
      />
    </div>
  );
}
