import { useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { save } from "@tauri-apps/plugin-dialog";
import { Database, Download, AlertTriangle, Loader2 } from "lucide-react";

export default function DataSettings() {
  const [isExporting, setIsExporting] = useState(false);
  const [isClearing, setIsClearing] = useState(false);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const handleExport = async () => {
    try {
      const filePath = await save({
        filters: [{
          name: 'SQLite Database',
          extensions: ['db']
        }],
        defaultPath: 'omniflow_backup.db'
      });

      if (!filePath) return;

      setIsExporting(true);
      setMessage(null);
      await invoke('export_database', { targetPath: filePath });
      setMessage({ type: 'success', text: `数据库已成功导出到: ${filePath}` });
    } catch (error: any) {
      setMessage({ type: 'error', text: `导出失败: ${error}` });
    } finally {
      setIsExporting(false);
    }
  };

  const handleClearData = async () => {
    const confirmed = window.confirm(
      "【警告】此操作将永久删除本地所有的账本、交易记录和分类数据！\n强烈建议在操作前先导出数据库备份。\n\n你确定要清空所有数据吗？"
    );
    if (!confirmed) return;

    setIsClearing(true);
    setMessage(null);
    try {
      await invoke('clear_all_data');
      setMessage({ type: 'success', text: '所有数据已成功清空。建议重启应用程序。' });
      // Reload window to reset all state
      setTimeout(() => window.location.reload(), 2000);
    } catch (error: any) {
      setMessage({ type: 'error', text: `清空失败: ${error}` });
    } finally {
      setIsClearing(false);
    }
  };

  return (
    <div className="space-y-8 max-w-2xl">
      {/* 导出数据 */}
      <div>
        <h2 className="text-lg font-medium text-foreground mb-4 flex items-center gap-2">
          <Database size={20} className="text-primary" />
          数据备份
        </h2>
        <p className="text-sm text-surface-foreground/80 mb-4">
          OmniFlow 是一个本地优先的应用程序，所有的财务数据都存储在你设备本地的 SQLite 数据库中。你可以随时将其导出备份。
        </p>
        <button
          onClick={handleExport}
          disabled={isExporting || isClearing}
          className="flex items-center gap-2 bg-surface border border-border text-foreground px-4 py-2 rounded-lg hover:bg-background transition-colors disabled:opacity-50"
        >
          {isExporting ? <Loader2 size={18} className="animate-spin" /> : <Download size={18} />}
          导出数据库 (.db)
        </button>
      </div>

      <div className="h-px bg-border w-full"></div>

      {/* 危险操作区 */}
      <div>
        <h2 className="text-lg font-medium text-red-500 mb-4 flex items-center gap-2">
          <AlertTriangle size={20} />
          危险区域
        </h2>
        <p className="text-sm text-surface-foreground/80 mb-4">
          清空数据将删除你所有的账单、分类、账户和账本信息。此操作不可逆！
        </p>
        <button
          onClick={handleClearData}
          disabled={isClearing || isExporting}
          className="flex items-center gap-2 bg-red-500/10 text-red-600 border border-red-200 dark:border-red-500/30 px-4 py-2 rounded-lg hover:bg-red-500/20 transition-colors disabled:opacity-50"
        >
          {isClearing ? <Loader2 size={18} className="animate-spin" /> : <AlertTriangle size={18} />}
          彻底清空本地数据
        </button>
      </div>

      {message && (
        <div className={`p-4 rounded-lg text-sm font-medium ${message.type === 'success' ? 'bg-green-500/10 text-green-600' : 'bg-red-500/10 text-red-600'}`}>
          {message.text}
        </div>
      )}
    </div>
  );
}
