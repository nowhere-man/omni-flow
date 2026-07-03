import { useState } from 'react';
import { useAppStore } from '../../stores/appStore';
import { Search, Filter, ArrowUpRight, ArrowDownRight, Tag } from 'lucide-react';
import { format } from 'date-fns';

export default function TransactionList() {
  const { transactions, isLoading } = useAppStore();
  const [searchTerm, setSearchTerm] = useState('');

  const filteredTransactions = transactions.filter(t => 
    t.merchant?.includes(searchTerm) || t.notes?.includes(searchTerm)
  );

  if (isLoading) {
    return <div className="flex h-full items-center justify-center">Loading transactions...</div>;
  }

  return (
    <div className="space-y-6 flex flex-col h-full">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">交易记录</h1>
      </div>

      <div className="bg-white p-4 rounded-2xl shadow-sm border border-border flex gap-4 items-center">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
          <input
            type="text"
            placeholder="搜索商家、备注..."
            className="w-full pl-10 pr-4 py-2 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-blue-500 outline-none"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <button className="flex items-center gap-2 px-4 py-2 bg-gray-50 hover:bg-gray-100 rounded-xl font-medium transition-colors">
          <Filter size={20} />
          筛选
        </button>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-border flex-1 overflow-hidden flex flex-col">
        <div className="overflow-x-auto flex-1">
          <table className="w-full text-left border-collapse">
            <thead className="bg-gray-50/80 sticky top-0 backdrop-blur-sm z-10">
              <tr>
                <th className="py-4 px-6 font-medium text-gray-500 border-b">时间</th>
                <th className="py-4 px-6 font-medium text-gray-500 border-b">交易类型</th>
                <th className="py-4 px-6 font-medium text-gray-500 border-b">商家 & 备注</th>
                <th className="py-4 px-6 font-medium text-gray-500 border-b">金额</th>
                <th className="py-4 px-6 font-medium text-gray-500 border-b">标签</th>
                <th className="py-4 px-6 font-medium text-gray-500 border-b">来源</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredTransactions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-gray-400">
                    暂无交易记录，请导入账单
                  </td>
                </tr>
              ) : (
                filteredTransactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="py-4 px-6 text-sm text-gray-600">
                      {format(new Date(tx.transaction_date * 1000), 'yyyy-MM-dd HH:mm')}
                    </td>
                    <td className="py-4 px-6">
                      {tx.transaction_type === 'expense' ? (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-red-50 text-red-600">
                          <ArrowUpRight size={14} /> 支出
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-green-50 text-green-600">
                          <ArrowDownRight size={14} /> 收入
                        </span>
                      )}
                    </td>
                    <td className="py-4 px-6">
                      <div className="font-medium text-gray-900">{tx.merchant || '未知商家'}</div>
                      <div className="text-sm text-gray-500 truncate max-w-[200px]">{tx.notes}</div>
                    </td>
                    <td className="py-4 px-6">
                      <span className={`font-semibold ${tx.transaction_type === 'expense' ? 'text-gray-900' : 'text-green-600'}`}>
                        {tx.transaction_type === 'expense' ? '-' : '+'}¥{tx.amount.toFixed(2)}
                      </span>
                    </td>
                    <td className="py-4 px-6">
                      <div className="flex gap-1 flex-wrap">
                        {tx.tags.length > 0 ? (
                          tx.tags.map((tag, idx) => (
                            <span key={idx} className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-gray-100 text-gray-600 text-xs">
                              <Tag size={12} /> {tag}
                            </span>
                          ))
                        ) : (
                          <span className="text-gray-400 text-xs">-</span>
                        )}
                      </div>
                    </td>
                    <td className="py-4 px-6 text-sm text-gray-500 uppercase tracking-wider">
                      {tx.external_source || '-'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
