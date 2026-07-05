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
    <div className="settings-stack">
      <section className="settings-section">
        <h2>外观与主题</h2>
        <div className="theme-choice-grid">
          {themeOptions.map((option) => {
            const Icon = option.icon;
            const isActive = theme === option.value;
            return (
              <button
                key={option.value}
                onClick={() => setTheme(option.value)}
                className={`theme-choice ${isActive ? 'active' : ''}`}
              >
                <Icon size={28} />
                <span>{option.label}</span>
              </button>
            );
          })}
        </div>
      </section>
      <p className="muted-note">
        主题切换会立即生效，跟随系统模式会根据您操作系统的当前外观自动匹配深浅色。
      </p>
    </div>
  );
}
