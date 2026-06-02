import { useState, useEffect, useCallback } from 'react';

/**
 * Lightweight toast notification hook.
 * Usage:
 *   const { toasts, showToast } = useToast();
 *   showToast('Nội dung thông báo', 'error');
 *   <ToastContainer toasts={toasts} />
 */
export function useToast() {
  const [toasts, setToasts] = useState([]);

  const showToast = useCallback((message, type = 'error', duration = 4000) => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, duration);
  }, []);

  const dismissToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return { toasts, showToast, dismissToast };
}

const ICON_MAP = {
  error: 'error',
  success: 'check_circle',
  warning: 'warning',
  info: 'info',
};

const COLOR_MAP = {
  error: 'bg-error text-on-error',
  success: 'bg-primary text-on-primary',
  warning: 'bg-[#b45309] text-white',
  info: 'bg-[#1d4ed8] text-white',
};

function Toast({ id, message, type, onDismiss }) {
  return (
    <div
      role="alert"
      aria-live="assertive"
      className={`
        flex items-start gap-3 px-4 py-3 shadow-lg min-w-[280px] max-w-sm
        animate-[slideInRight_0.3s_ease-out]
        ${COLOR_MAP[type] || COLOR_MAP.info}
      `}
    >
      <span className="material-symbols-outlined text-xl mt-0.5 shrink-0">
        {ICON_MAP[type] || ICON_MAP.info}
      </span>
      <p className="text-sm font-semibold leading-snug flex-1">{message}</p>
      <button
        onClick={() => onDismiss(id)}
        aria-label="Đóng thông báo"
        className="ml-1 opacity-70 hover:opacity-100 transition-opacity bg-transparent border-none cursor-pointer text-inherit"
      >
        <span className="material-symbols-outlined text-base">close</span>
      </button>
    </div>
  );
}

export function ToastContainer({ toasts, onDismiss }) {
  return (
    <div
      aria-label="Thông báo"
      className="fixed bottom-6 right-6 z-[9999] flex flex-col gap-3 pointer-events-none"
    >
      {toasts.map((toast) => (
        <div key={toast.id} className="pointer-events-auto">
          <Toast {...toast} onDismiss={onDismiss} />
        </div>
      ))}
    </div>
  );
}
