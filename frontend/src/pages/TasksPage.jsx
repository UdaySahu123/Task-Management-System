import { useCallback, useEffect, useState } from 'react';
import { FilterX, Plus, Search } from 'lucide-react';
import { deleteTask, listTasks } from '../api/tasks';
import { getErrorMessage } from '../api/client';
import { useToast } from '../context/ToastContext';
import TaskFormModal from '../components/TaskFormModal';
import ConfirmDialog from '../components/ConfirmDialog';
import TaskCard from '../components/TaskCard';
import Pagination from '../components/Pagination';
import EmptyState from '../components/EmptyState';
import { TaskSkeletonGrid } from '../components/Skeleton';
import { TASK_PRIORITIES, TASK_STATUSES, PRIORITY_LABELS, STATUS_LABELS } from '../utils/format';

const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'createdAt,asc', label: 'Oldest first' },
  { value: 'dueDate,asc', label: 'Due soonest' },
  { value: 'dueDate,desc', label: 'Due latest' },
  { value: 'priority,desc', label: 'Priority: high to low' },
  { value: 'priority,asc', label: 'Priority: low to high' },
  { value: 'title,asc', label: 'Title A–Z' },
];

const PAGE_SIZE = 9;

const initialFilters = {
  search: '',
  status: '',
  priority: '',
  overdue: false,
  sort: 'createdAt,desc',
  page: 0,
};

export default function TasksPage() {
  const { toast } = useToast();

  const [filters, setFilters] = useState(initialFilters);
  const [searchInput, setSearchInput] = useState('');

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [deletingBusy, setDeletingBusy] = useState(false);

  // Debounce the search box so we do not fire a request per keystroke.
  useEffect(() => {
    const handle = window.setTimeout(() => {
      setFilters((previous) =>
        previous.search === searchInput ? previous : { ...previous, search: searchInput, page: 0 },
      );
    }, 350);
    return () => window.clearTimeout(handle);
  }, [searchInput]);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');

    const params = { page: filters.page, size: PAGE_SIZE, sort: filters.sort };
    if (filters.search.trim()) params.search = filters.search.trim();
    if (filters.status) params.status = filters.status;
    if (filters.priority) params.priority = filters.priority;
    if (filters.overdue) params.overdue = true;

    try {
      setData(await listTasks(params));
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    load();
  }, [load]);

  const setFilter = (key, value) => {
    setFilters((previous) => ({ ...previous, [key]: value, page: 0 }));
  };

  const isFiltered =
    Boolean(filters.search.trim()) ||
    Boolean(filters.status) ||
    Boolean(filters.priority) ||
    filters.overdue;

  const resetFilters = () => {
    setSearchInput('');
    setFilters(initialFilters);
  };

  const handleDelete = async () => {
    if (!deleting) return;
    setDeletingBusy(true);
    try {
      await deleteTask(deleting.id);
      toast.success('Task deleted');
      const wasLastOnPage = data?.content?.length === 1 && filters.page > 0;
      setDeleting(null);
      if (wasLastOnPage) {
        setFilters((previous) => ({ ...previous, page: previous.page - 1 }));
      } else {
        load();
      }
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setDeletingBusy(false);
    }
  };

  const tasks = data?.content ?? [];

  return (
    <div>
      <div className="row-between wrap gap mt-sm" style={{ marginBottom: 20 }}>
        <div>
          <h2 className="page-title">Your tasks</h2>
          <p className="page-subtitle">
            {data ? `${data.totalElements} task${data.totalElements === 1 ? '' : 's'}` : 'Loading…'}
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}>
          <Plus size={16} />
          New task
        </button>
      </div>

      <div className="toolbar">
        <div className="search-box">
          <Search className="input-icon" size={16} />
          <input
            className="form-input"
            type="search"
            placeholder="Search by title or description"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            aria-label="Search tasks"
          />
        </div>

        <select
          className="form-select"
          value={filters.status}
          onChange={(event) => setFilter('status', event.target.value)}
          aria-label="Filter by status"
        >
          <option value="">All statuses</option>
          {TASK_STATUSES.map((value) => (
            <option key={value} value={value}>
              {STATUS_LABELS[value]}
            </option>
          ))}
        </select>

        <select
          className="form-select"
          value={filters.priority}
          onChange={(event) => setFilter('priority', event.target.value)}
          aria-label="Filter by priority"
        >
          <option value="">All priorities</option>
          {TASK_PRIORITIES.map((value) => (
            <option key={value} value={value}>
              {PRIORITY_LABELS[value]}
            </option>
          ))}
        </select>

        <button
          type="button"
          className={`btn btn-sm ${filters.overdue ? 'btn-primary' : 'btn-secondary'}`}
          aria-pressed={filters.overdue}
          onClick={() => setFilter('overdue', !filters.overdue)}
        >
          Overdue only
        </button>

        <select
          className="form-select"
          value={filters.sort}
          onChange={(event) => setFilter('sort', event.target.value)}
          aria-label="Sort tasks"
        >
          {SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>

        {isFiltered && (
          <button type="button" className="btn btn-ghost btn-sm" onClick={resetFilters}>
            <FilterX size={14} />
            Clear
          </button>
        )}
      </div>

      {error && (
        <div className="alert alert-error">
          <span className="grow">{error}</span>
          <button type="button" className="btn btn-sm btn-secondary" onClick={load}>
            Retry
          </button>
        </div>
      )}

      {loading && !data ? (
        <TaskSkeletonGrid count={PAGE_SIZE} />
      ) : tasks.length ? (
        <>
          <div className="task-grid">
            {tasks.map((task) => (
              <TaskCard key={task.id} task={task} onEdit={setEditing} onDelete={setDeleting} />
            ))}
          </div>

          <Pagination
            page={data.page + 1}
            totalPages={data.totalPages}
            onChange={(nextPage) => setFilters((previous) => ({ ...previous, page: nextPage - 1 }))}
          />
        </>
      ) : (
        !loading && (
          <EmptyState
            icon={<Search size={24} />}
            title={isFiltered ? 'No tasks match your filters' : 'No tasks yet'}
            message={
              isFiltered
                ? 'Try adjusting or clearing your filters.'
                : 'Create your first task to get started.'
            }
            action={
              isFiltered ? (
                <button type="button" className="btn btn-secondary" onClick={resetFilters}>
                  Clear filters
                </button>
              ) : (
                <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}>
                  <Plus size={16} />
                  New task
                </button>
              )
            }
          />
        )
      )}

      <TaskFormModal
        open={creating || Boolean(editing)}
        task={editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        onSaved={load}
      />

      <ConfirmDialog
        open={Boolean(deleting)}
        title="Delete task"
        message={deleting ? `"${deleting.title}" will be permanently deleted.` : ''}
        confirmLabel="Delete"
        busy={deletingBusy}
        onConfirm={handleDelete}
        onCancel={() => setDeleting(null)}
      />
    </div>
  );
}
