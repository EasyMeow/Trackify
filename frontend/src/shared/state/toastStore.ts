import { create } from 'zustand';

export type ToastKind = 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  kind: ToastKind;
  message: string;
}

interface ToastState {
  toasts: Toast[];
  addToast: (kind: ToastKind, message: string) => void;
  dismissToast: (id: string) => void;
}

let _nextId = 1;

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],

  addToast(kind, message) {
    const id = String(_nextId++);
    set((s) => ({ toasts: [...s.toasts, { id, kind, message }] }));
    // auto-dismiss after 5 s
    setTimeout(() => {
      set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }));
    }, 5000);
  },

  dismissToast(id) {
    set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }));
  },
}));

/** Convenience helper — call outside React (e.g., in mutation onError callbacks). */
export function addErrorToast(message: string): void {
  useToastStore.getState().addToast('error', message);
}
