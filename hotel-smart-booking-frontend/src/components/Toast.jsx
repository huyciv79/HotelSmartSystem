/* eslint-disable react-refresh/only-export-components */
import { useState, useCallback } from 'react';

/**
 * Lightweight toast notification hook.
 * Usage:
 *   const { toasts, showToast } = useToast();
 *   showToast('Nội dung thông báo', 'error');
 *   <ToastContainer toasts={toasts} />
 */
export function useToast() {
  const [toasts, setToasts] = useState([]);

  const showToast = useCallback((message, type = 'info', duration = 4000) => {
    setToasts((prev) => {
      // Prevent duplicate toast messages
      if (prev.some((t) => t.message === message)) {
        return prev;
      }
      const id = Date.now() + Math.random(); // Ensure unique ID even if triggered simultaneously
      setTimeout(() => {
        setToasts((active) => active.filter((t) => t.id !== id));
      }, duration);
      return [...prev, { id, message, type }];
    });
  }, []);

  const dismissToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return { toasts, showToast, dismissToast };
}

const TOAST_CONFIG = {
  info: {
    icon: 'info',
    label: 'Thông tin',
    role: 'status',
    live: 'polite',
    className: 'border-sky-300 bg-sky-600 text-white shadow-sky-900/20',
  },
  warning: {
    icon: 'warning',
    label: 'Cảnh báo',
    role: 'alert',
    live: 'assertive',
    className: 'border-amber-300 bg-amber-500 text-slate-950 shadow-amber-900/20',
  },
  error: {
    icon: 'error',
    label: 'Lỗi',
    role: 'alert',
    live: 'assertive',
    className: 'border-red-300 bg-red-600 text-white shadow-red-900/20',
  },
  success: {
    icon: 'check_circle',
    label: 'Thành công',
    role: 'status',
    live: 'polite',
    className: 'border-emerald-300 bg-emerald-600 text-white shadow-emerald-900/20',
  },
};

function Toast({ id, message, type, onDismiss }) {
  const config = TOAST_CONFIG[type] || TOAST_CONFIG.info;

  return (
    <div
      role={config.role}
      aria-live={config.live}
      aria-atomic="true"
      className={`
        flex items-start gap-3 border-l-4 rounded-lg px-4 py-3 shadow-lg min-w-0 w-[calc(100vw-2rem)] sm:min-w-[280px] sm:w-auto max-w-sm
        animate-[slideInRight_0.3s_ease-out]
        ${config.className}
      `}
    >
      <span className="material-symbols-outlined text-xl mt-0.5 shrink-0" aria-hidden="true">
        {config.icon}
      </span>
      <p className="text-sm font-semibold leading-snug flex-1">
        <span className="sr-only">{config.label}: </span>
        {message}
      </p>
      <button
        onClick={() => onDismiss(id)}
        aria-label="Đóng thông báo"
        className="ml-1 size-7 -mr-1 -mt-1 inline-flex items-center justify-center rounded-md opacity-75 hover:opacity-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-current transition-opacity bg-transparent border-none cursor-pointer text-inherit"
      >
        <span className="material-symbols-outlined text-base" aria-hidden="true">close</span>
      </button>
    </div>
  );
}

export function ToastContainer({ toasts, onDismiss }) {
  return (
    <div
      aria-label="Thông báo"
      className="fixed bottom-4 left-4 right-4 sm:left-auto sm:bottom-6 sm:right-6 z-[9999] flex flex-col items-stretch sm:items-end gap-3 pointer-events-none"
    >
      {toasts.map((toast) => (
        <div key={toast.id} className="pointer-events-auto">
          <Toast {...toast} onDismiss={onDismiss} />
        </div>
      ))}
    </div>
  );
}
