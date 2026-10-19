import { useToast } from '../context/ToastContext';

const ICONS = {
  success: '✓',
  error: '!',
  info: 'i',
};

/** Fixed-position stack of dismissible notifications. */
export default function ToastContainer() {
  const { toasts, remove } = useToast();

  if (!toasts.length) return null;

  return (
    <div className="toast-container" role="status" aria-live="polite">
      {toasts.map((item) => (
        <div key={item.id} className={`toast ${item.type}`}>
          <span className={`toast-icon ${item.type}`} aria-hidden="true">
            {ICONS[item.type]}
          </span>
          <span className="grow">{item.message}</span>
          <button
            type="button"
            className="toast-close"
            aria-label="Dismiss notification"
            onClick={() => remove(item.id)}
          >
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
