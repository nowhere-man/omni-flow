import { useEffect } from 'react';
import { useAppStore } from '../../stores/appStore';
import { Wallet, ArrowDownCircle, ArrowUpCircle } from 'lucide-react';
import ReactECharts from 'echarts-for-react';

export default function Dashboard() {
  const { ledgers, currentLedgerId, fetchInitialData, isLoading } = useAppStore();

  useEffect(() => {
    fetchInitialData();
  }, []);

  if (isLoading) {
    return <div className="flex h-full items-center justify-center">Loading...</div>;
  }

  const currentLedger = ledgers.find(l => l.id === currentLedgerId);

  // Mock data for initial UI before full StatsEngine integration
  const option = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'] },
    yAxis: { type: 'value' },
    series: [
      {
        data: [120, 200, 150, 80, 70, 110, 130],
        type: 'bar',
        itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] }
      }
    ]
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">仪表盘</h1>
        <div className="bg-white border px-4 py-2 rounded-lg shadow-sm text-sm font-medium">
          当前账本: {currentLedger?.name || '无'}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-border">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-blue-50 text-blue-500 rounded-xl"><Wallet size={24} /></div>
            <div>
              <div className="text-sm text-gray-500 font-medium">净资产</div>
              <div className="text-2xl font-bold mt-1">¥ 204,707.30</div>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-border">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-green-50 text-green-500 rounded-xl"><ArrowDownCircle size={24} /></div>
            <div>
              <div className="text-sm text-gray-500 font-medium">本月收入</div>
              <div className="text-2xl font-bold mt-1">¥ 12,500.00</div>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-2xl shadow-sm border border-border">
          <div className="flex items-center gap-4">
            <div className="p-3 bg-red-50 text-red-500 rounded-xl"><ArrowUpCircle size={24} /></div>
            <div>
              <div className="text-sm text-gray-500 font-medium">本月支出</div>
              <div className="text-2xl font-bold mt-1">¥ 4,320.50</div>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white p-6 rounded-2xl shadow-sm border border-border">
        <h2 className="text-lg font-semibold mb-4">本周收支概览</h2>
        <ReactECharts option={option} style={{ height: 300 }} />
      </div>
    </div>
  );
}
