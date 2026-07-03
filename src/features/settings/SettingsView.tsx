import { useState } from "react";
import { Link, Routes, Route, useLocation, Navigate } from "react-router-dom";
import { Palette, Database, CloudSync, Tags } from "lucide-react";
import BasicSettings from "./components/BasicSettings";
import SyncSettings from "./components/SyncSettings";
import DataSettings from "./components/DataSettings";
import CategorySettings from "./components/CategorySettings";

export default function SettingsView() {
  const location = useLocation();
  const currentTab = location.pathname.split('/').pop() || 'basic';

  const tabs = [
    { id: 'basic', label: '外观与基础', icon: Palette, path: '/settings/basic' },
    { id: 'categories', label: '分类管理', icon: Tags, path: '/settings/categories' },
    { id: 'data', label: '数据安全', icon: Database, path: '/settings/data' },
    { id: 'sync', label: '高级同步', icon: CloudSync, path: '/settings/sync' },
  ];

  return (
    <div className="max-w-5xl mx-auto flex gap-8">
      {/* Settings Sidebar */}
      <div className="w-56 shrink-0">
        <h1 className="text-2xl font-bold mb-6 text-foreground">设置</h1>
        <nav className="flex flex-col gap-1">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = location.pathname.includes(tab.id);
            return (
              <Link
                key={tab.id}
                to={tab.path}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-primary/10 text-primary'
                    : 'text-surface-foreground hover:bg-surface hover:text-foreground'
                }`}
              >
                <Icon size={18} />
                {tab.label}
              </Link>
            );
          })}
        </nav>
      </div>

      {/* Settings Content Area */}
      <div className="flex-1 bg-surface border border-border rounded-2xl p-8 min-h-[600px]">
        <Routes>
          <Route path="/" element={<Navigate to="basic" replace />} />
          <Route path="basic" element={<BasicSettings />} />
          <Route path="categories" element={<CategorySettings />} />
          <Route path="data" element={<DataSettings />} />
          <Route path="sync" element={<SyncSettings />} />
        </Routes>
      </div>
    </div>
  );
}
