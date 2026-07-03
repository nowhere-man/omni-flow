import { useState } from 'react';
import { useAppStore } from '../../stores/appStore';
import { TransactionAPI } from '../../tauri-adapter/transactions';
import { UploadCloud, CheckCircle2, AlertCircle, FileText } from 'lucide-react';
import { open } from '@tauri-apps/plugin-dialog';

export default function ImportView() {
  const { currentLedgerId, accounts, fetchTransactions } = useAppStore();
  const [selectedAccount, setSelectedAccount] = useState<string>('');
  const [isImporting, setIsImporting] = useState(false);
  const [importResult, setImportResult] = useState<{ success: boolean; message: string } | null>(null);

  const handleSelectFile = async () => {
    if (!currentLedgerId) return;
    
    // Choose default account (Cash) if none selected
    const targetAccountId = selectedAccount || (accounts.find(a => a.account_type === 'cash')?.id ?? accounts[0]?.id);
    
    if (!targetAccountId) {
      setImportResult({ success: false, message: '没有可用的账户进行导入' });
      return;
    }

    try {
      const selected = await open({
        multiple: false,
        filters: [{
          name: 'Bills',
          extensions: ['csv', 'xls', 'xlsx']
        }]
      });

      if (!selected) return;

      setIsImporting(true);
      setImportResult(null);

      const filePath = Array.isArray(selected) ? selected[0] : selected;
      
      const count = await TransactionAPI.importBill(filePath as string, currentLedgerId, targetAccountId);
      
      setImportResult({ success: true, message: `成功导入 ${count} 条交易记录` });
      fetchTransactions(); // Refresh the list
      
    } catch (err: any) {
      setImportResult({ success: false, message: `导入失败: ${err.toString()}` });
    } finally {
      setIsImporting(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">导入账单</h1>
      </div>

      <div className="bg-white p-8 rounded-3xl shadow-sm border border-border">
        <div className="max-w-xl mx-auto space-y-8">
          <div className="space-y-4">
            <h2 className="text-xl font-semibold">1. 选择导入目标账户</h2>
            <select 
              className="w-full p-3 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-blue-500 outline-none"
              value={selectedAccount}
              onChange={(e) => setSelectedAccount(e.target.value)}
            >
              <option value="">默认现金账户 (自动匹配)</option>
              {accounts.map(acc => (
                <option key={acc.id} value={acc.id}>{acc.name} ({acc.account_type})</option>
              ))}
            </select>
          </div>

          <div className="space-y-4">
            <h2 className="text-xl font-semibold">2. 上传账单文件</h2>
            <div 
              onClick={!isImporting ? handleSelectFile : undefined}
              className={`border-2 border-dashed rounded-3xl p-12 text-center transition-all cursor-pointer flex flex-col items-center justify-center gap-4
                ${isImporting ? 'bg-gray-50 border-gray-200 cursor-not-allowed' : 'hover:bg-blue-50/50 hover:border-blue-400 border-gray-300'}`}
            >
              <div className={`p-4 rounded-full ${isImporting ? 'bg-gray-100 text-gray-400' : 'bg-blue-100 text-blue-500'}`}>
                <UploadCloud size={40} />
              </div>
              <div>
                <p className="text-lg font-medium">点击选择对账单文件</p>
                <p className="text-sm text-gray-500 mt-1">支持 支付宝 (CSV), 微信 (XLSX), 建设银行 (XLS), 京东/美团 等格式</p>
              </div>
            </div>
          </div>

          {isImporting && (
            <div className="flex items-center justify-center gap-3 text-blue-600 font-medium p-4 bg-blue-50 rounded-xl">
              <div className="animate-spin h-5 w-5 border-2 border-current border-t-transparent rounded-full" />
              正在解析与应用规则，请稍候...
            </div>
          )}

          {importResult && (
            <div className={`p-4 rounded-xl flex items-start gap-3 ${importResult.success ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}`}>
              {importResult.success ? <CheckCircle2 className="shrink-0" /> : <AlertCircle className="shrink-0" />}
              <div>
                <h3 className="font-medium">{importResult.success ? '导入成功' : '导入失败'}</h3>
                <p className="text-sm mt-1 opacity-90">{importResult.message}</p>
              </div>
            </div>
          )}

          <div className="bg-gray-50 p-6 rounded-2xl">
            <h3 className="flex items-center gap-2 font-medium text-gray-700 mb-3">
              <FileText size={18} /> 支持的账单导出说明
            </h3>
            <ul className="text-sm text-gray-600 space-y-2 list-disc pl-5">
              <li><strong>支付宝</strong>: 我的 - 账单 - 右上角 - 开具交易流水证明</li>
              <li><strong>微信支付</strong>: 钱包 - 账单 - 常见问题 - 下载账单</li>
              <li><strong>建设银行</strong>: 手机银行导出明细 (仅支持 xls 格式)</li>
              <li>导入时会自动进行重复数据检测与清洗，无需担心重复导入</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
