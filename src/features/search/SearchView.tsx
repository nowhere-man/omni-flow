import { useState } from 'react';
import { useAppStore } from '../../stores/appStore';
import { Search, Calendar, Tag } from 'lucide-react';
import { format } from 'date-fns';

export default function SearchView() {
  const { transactions } = useAppStore();
  const [searchTerm, setSearchTerm] = useState('');
  
  const searchResults = searchTerm.length > 0 
    ? transactions.filter(t => 
        t.merchant?.includes(searchTerm) || 
        t.notes?.includes(searchTerm) ||
        t.tags.some(tag => tag.includes(searchTerm)) ||
        t.amount.toString().includes(searchTerm)
      )
    : [];

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <h1 className="text-3xl font-bold tracking-tight">全能搜索</h1>
      
      <div className="relative">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-blue-500" size={24} />
        <input
          type="text"
          placeholder="搜索金额、商户、备注、标签..."
          className="w-full pl-14 pr-6 py-4 bg-white border border-gray-200 rounded-2xl shadow-sm text-lg focus:ring-4 focus:ring-blue-500/20 focus:border-blue-500 outline-none transition-all"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          autoFocus
        />
      </div>

      <div className="space-y-4">
        <p className="text-gray-500 font-medium">
          {searchTerm.length > 0 ? `找到 ${searchResults.length} 条结果` : '输入任意内容开始搜索'}
        </p>

        {searchResults.map(tx => (
          <div key={tx.id} className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm flex justify-between items-center hover:border-blue-200 transition-colors cursor-pointer group">
            <div className="flex gap-4 items-center">
              <div className={`p-3 rounded-xl ${tx.transaction_type === 'expense' ? 'bg-red-50 text-red-500' : 'bg-green-50 text-green-500'}`}>
                <Calendar size={20} />
              </div>
              <div>
                <div className="font-semibold text-gray-900 group-hover:text-blue-600 transition-colors">
                  {tx.merchant || '未知商户'}
                </div>
                <div className="text-sm text-gray-500 mt-1 flex gap-3">
                  <span>{format(new Date(tx.transaction_date * 1000), 'yyyy-MM-dd')}</span>
                  {tx.notes && <span>• {tx.notes}</span>}
                </div>
                {tx.tags.length > 0 && (
                  <div className="flex gap-1 mt-2">
                    {tx.tags.map((tag, i) => (
                      <span key={i} className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-md flex items-center gap-1">
                        <Tag size={10} /> {tag}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </div>
            <div className={`text-xl font-bold ${tx.transaction_type === 'expense' ? 'text-gray-900' : 'text-green-600'}`}>
              {tx.transaction_type === 'expense' ? '-' : '+'}¥{tx.amount.toFixed(2)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
