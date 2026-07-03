import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type ThemeMode = 'light' | 'dark' | 'system';

interface SyncConfig {
  url: string;
  user: string;
  pass: string;
  encryptKey: string;
}

interface SettingsState {
  theme: ThemeMode;
  setTheme: (theme: ThemeMode) => void;
  syncConfig: SyncConfig;
  setSyncConfig: (config: Partial<SyncConfig>) => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      theme: 'system',
      setTheme: (theme) => {
        set({ theme });
        applyTheme(theme);
      },
      syncConfig: {
        url: '',
        user: '',
        pass: '',
        encryptKey: '',
      },
      setSyncConfig: (config) =>
        set((state) => ({
          syncConfig: { ...state.syncConfig, ...config },
        })),
    }),
    {
      name: 'omniflow-settings',
    }
  )
);

export function applyTheme(theme: ThemeMode) {
  const root = window.document.documentElement;
  root.classList.remove('light', 'dark');

  if (theme === 'system') {
    const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
    root.classList.add(systemTheme);
  } else {
    root.classList.add(theme);
  }
}
