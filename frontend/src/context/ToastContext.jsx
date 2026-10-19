import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';

const ToastContext = createContext(null);

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used inside a <ToastProvider>');
  }
  return context;
}

/**
 * Tiny toast queue. Each toast auto-dismisses after a few seconds.
 * Usage: const { toast } = useToast(); toast.success('Saved!');
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const nextId = useRef(0);

  const remove = useCallback((id) => {
    setToasts((current) => current.filter((item) => item.id !== id));
  }, []);

  const push = useCallback(
    (type, message, duration = 4000) => {
      const id = (nextId.current += 1);
      setToasts((current) => [...current, { id, type, message }]);
      if (duration > 0) {
        window.setTimeout(() => remove(id), duration);
      }
      return id;
    },
    [remove],
  );

  const value = useMemo(
    () => ({
      toasts,
      remove,
      toast: {
        success: (message) => push('success', message),
        error: (message) => push('error', message, 6000),
        info: (message) => push('info', message),
      },
    }),
    [toasts, remove, push],
  );

  return <ToastContext.Provider value={value}>{children}</ToastContext.Provider>;
}
