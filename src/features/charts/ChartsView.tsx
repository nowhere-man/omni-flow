import { useEffect, useState } from 'react';
import { useAppStore } from '../../stores/appStore';
import { StatsAPI, TrendDataPoint, CategoryBreakdown } from '../../tauri-adapter/stats';
import ReactECharts from 'echarts-for-react';
import { startOfMonth, endOfMonth, subMonths } from 'date-fns';

export default function ChartsView() {
  const { currentLedgerId } = useAppStore();
  const [trendData, setTrendData] = useState<TrendDataPoint[]>([]);
  const [expenseBreakdown, setExpenseBreakdown] = useState<CategoryBreakdown[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    async function loadStats() {
      if (!currentLedgerId) return;
      setIsLoading(true);
      
      // Fetch last 12 months for trend
      const end = endOfMonth(new Date());
      const start = startOfMonth(subMonths(end, 11));
      
      try {
        const trend = await StatsAPI.getMonthlyTrend(
          currentLedgerId, 
          Math.floor(start.getTime() / 1000), 
          Math.floor(end.getTime() / 1000)
        );
        setTrendData(trend);

        // Fetch this month's breakdown
        const monthStart = startOfMonth(new Date());
        const breakdown = await StatsAPI.getCategoryBreakdown(
          currentLedgerId,
          Math.floor(monthStart.getTime() / 1000),
          Math.floor(end.getTime() / 1000),
          "expense"
        );
        setExpenseBreakdown(breakdown);
      } catch (e) {
        console.error("Failed to load stats", e);
      } finally {
        setIsLoading(false);
      }
    }

    loadStats();
  }, [currentLedgerId]);

  const trendOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['收入', '支出'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trendData.map(d => d.date)
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '收入',
        type: 'bar',
        data: trendData.map(d => d.income),
        itemStyle: { color: '#10b981', borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '支出',
        type: 'bar',
        data: trendData.map(d => d.expense),
        itemStyle: { color: '#ef4444', borderRadius: [4, 4, 0, 0] }
      }
    ]
  };

  const pieOption = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '本月支出',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: '20', fontWeight: 'bold' }
        },
        labelLine: { show: false },
        data: expenseBreakdown.map(b => ({
          value: b.amount,
          name: b.category_name
        }))
      }
    ]
  };

  if (isLoading) {
    return <div className="flex h-full items-center justify-center">Loading charts...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">图表分析</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-border">
          <h2 className="text-lg font-semibold mb-4">近一年收支趋势</h2>
          <ReactECharts option={trendOption} style={{ height: 400 }} />
        </div>
        
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-border">
          <h2 className="text-lg font-semibold mb-4">本月支出构成</h2>
          {expenseBreakdown.length > 0 ? (
            <ReactECharts option={pieOption} style={{ height: 400 }} />
          ) : (
            <div className="h-[400px] flex items-center justify-center text-gray-400">
              暂无数据
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
