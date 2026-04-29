import { create } from 'zustand';

interface AvatarState {
  version: number;
  bumpVersion: () => void;
}

export const useAvatarStore = create<AvatarState>((set) => ({
  version: 0,
  bumpVersion: () => set((s) => ({ version: s.version + 1 })),
}));
