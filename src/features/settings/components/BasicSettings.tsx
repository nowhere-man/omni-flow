import { useSettingsStore, ThemeMode } from "../../../store/useSettingsStore";
import { Monitor, Moon, Sun } from "lucide-react";

export default function BasicSettings() {
  const { theme, setTheme } = useSettingsStore();

  const themeOptions: { value: ThemeMode; label: string; icon: React.ElementType }[] = [
    { value: 'light', label: '浅色模式', icon: Sun },
    { value: 'dark', label: '深色模式', icon: Moon },
    { value: 'system', label: '跟随系统', icon: Monitor },
  ];

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h2 className="text-lg font-medium text-foreground mb-4">外观与主题</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {themeOptions.map((option) => {
            const Icon = option.icon;
            const isActive = theme === option.value;
            return (
              <button
                key={option.value}
                onClick={() => setTheme(option.value)}
                className={`flex flex-col items-center justify-center p-4 rounded-xl border-2 transition-all ${
                  isActive
                    ? 'border-primary bg-primary/5 text-primary'
                    : 'border-border bg-surface text-surface-foreground hover:border-primary/50'
                }`}
              >
                <Icon className="w-8 h-8 mb-2" />
                <span className="font-medium">{option.label}</span>
              </button>
            );
          })}
        </div>
      </div>
      <p className="text-sm text-surface-foreground/70">
        主题切换会立即生效，跟随系统模式会根据您操作系统的当前外观自动匹配深浅色。
      </p>
    </div>
  );
}
