import { BrowserRouter as Router, Routes, Route, Link } from "react-router-dom";
import { LayoutDashboard, ReceiptText, ArrowDownToLine, PieChart, Search, Settings, CreditCard } from "lucide-react";
import Dashboard from "./features/dashboard/Dashboard";
import TransactionList from "./features/transactions/TransactionList";
import ImportView from "./features/import/ImportView";
import ChartsView from "./features/charts/ChartsView";
import SearchView from "./features/search/SearchView";
import SettingsView from "./features/settings/SettingsView";

function Sidebar() {
  const navItems = [
    { icon: LayoutDashboard, label: "仪表盘", path: "/" },
    { icon: ReceiptText, label: "交易记录", path: "/transactions" },
    { icon: ArrowDownToLine, label: "导入账单", path: "/import" },
    { icon: PieChart, label: "图表分析", path: "/charts" },
    { icon: Search, label: "高级搜索", path: "/search" },
    { icon: CreditCard, label: "负债管理", path: "/credit" },
    { icon: Settings, label: "设置", path: "/settings" },
  ];

  return (
    <div className="w-64 h-screen bg-surface border-r border-border flex flex-col p-4">
      <div className="text-xl font-bold mb-8 pl-2 flex items-center gap-2">
        <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-white">O</div>
        <span>OmniFlow</span>
      </div>
      <nav className="flex-1 flex flex-col gap-2">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <Link
              key={item.path}
              to={item.path}
              className="flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-background text-surface-foreground transition-colors"
            >
              <Icon size={20} className="text-primary/80" />
              <span className="font-medium text-sm">{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </div>
  );
}

import { useEffect } from "react";
import { useSettingsStore, applyTheme } from "./store/useSettingsStore";

export default function App() {
  const theme = useSettingsStore((state) => state.theme);

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  return (
    <Router>
      <div className="flex w-screen h-screen bg-background overflow-hidden text-foreground">
        <Sidebar />
        <main className="flex-1 overflow-y-auto p-8">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/transactions" element={<TransactionList />} />
            <Route path="/import" element={<ImportView />} />
            <Route path="/charts" element={<ChartsView />} />
            <Route path="/search" element={<SearchView />} />
            <Route path="/settings/*" element={<SettingsView />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}
