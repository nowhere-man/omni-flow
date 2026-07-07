import { useCallback, useEffect, useMemo, useState } from "react";
import { invoke } from "@tauri-apps/api/core";
import { ChevronLeft, ChevronRight, Plus } from "lucide-react";
import { AnimatePresence } from "framer-motion";
import { format, startOfMonth, endOfMonth, addMonths, subMonths, eachDayOfInterval, startOfWeek, endOfWeek, isSameMonth, isToday } from "date-fns";

import { useAppStore } from "../../stores/appStore";
import { Transaction, TransactionAPI } from "../../tauri-adapter/transactions";
import { Category } from "../../models";
import { YearMonthPicker } from "../../components/ui/DatePicker";
import { TransactionEditor } from "../transactions/TransactionEditor";
import { yuan } from "../../lib/format";

const now = () => Math.floor(Date.now() / 1000);

export default function Dashboard() {
  const { accounts, currentLedgerId, fetchInitialData } = useAppStore();
  const [selectedMonth, setSelectedMonth] = useState(() => startOfMonth(new Date()));
  const [showMonthPicker, setShowMonthPicker] = useState(false);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [draft, setDraft] = useState<Transaction | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);

  useEffect(() => {
    fetchInitialData();
    invoke<Category[]>("list_categories").then(setCategories).catch(() => setCategories([]));
  }, [fetchInitialData]);

  const loadMonthData = useCallback(async () => {
    if (!currentLedgerId) return;
    setIsLoading(true);
    const start = Math.floor(startOfMonth(selectedMonth).getTime() / 1000);
    const end = Math.floor(endOfMonth(selectedMonth).getTime() / 1000);
    try {
      const res = await TransactionAPI.searchTransactions(currentLedgerId, { start_date: start, end_date: end });
      setTransactions(res.transactions);
    } finally {
      setIsLoading(false);
    }
  }, [currentLedgerId, selectedMonth]);

  useEffect(() => {
    loadMonthData();
  }, [loadMonthData]);

  const handlePrevMonth = () => setSelectedMonth((m) => subMonths(m, 1));
  const handleNextMonth = () => setSelectedMonth((m) => addMonths(m, 1));

  const calendarDays = useMemo(() => {
    const monthStart = startOfMonth(selectedMonth);
    const monthEnd = endOfMonth(selectedMonth);
    const startDate = startOfWeek(monthStart, { weekStartsOn: 1 });
    const endDate = endOfWeek(monthEnd, { weekStartsOn: 1 });

    const days = eachDayOfInterval({ start: startDate, end: endDate });

    const dailyNet = new Map<string, number>();
    for (const t of transactions) {
      if (t.is_excluded) continue;
      const dateStr = format(new Date(t.transaction_date * 1000), "yyyy-MM-dd");
      const current = dailyNet.get(dateStr) || 0;
      const delta = t.transaction_type === "income" ? t.amount : -t.amount;
      dailyNet.set(dateStr, current + delta);
    }

    return days.map(day => {
      const dateStr = format(day, "yyyy-MM-dd");
      return {
        date: day,
        isCurrentMonth: isSameMonth(day, selectedMonth),
        isToday: isToday(day),
        netIncome: dailyNet.get(dateStr),
      };
    });
  }, [selectedMonth, transactions]);

  const weekDays = ["一", "二", "三", "四", "五", "六", "日"];

  function openQuickEntry() {
    if (!currentLedgerId || accounts.length === 0) return;
    const defaultAccount = accounts.find((a) => a.account_type === "cash") || accounts[0];
    const timestamp = now();
    setDraft({
      id: crypto.randomUUID(),
      ledger_id: currentLedgerId,
      account_id: defaultAccount.id,
      category_id: null,
      transaction_date: timestamp,
      amount: 0,
      transaction_type: "expense",
      merchant: null,
      notes: null,
      tags: [],
      is_excluded: false,
      external_source: "manual",
      external_id: null,
      created_at: timestamp,
      updated_at: timestamp,
      deleted_at: null,
    });
  }

  async function saveDraft() {
    if (!draft) return;
    await TransactionAPI.createTransaction(draft);
    setDraft(null);
    await loadMonthData();
  }

  return (
    <div className="money-flow-page">
      <header className="dashboard-header">
        <div style={{ width: "80px" }}></div>
        <div className="dashboard-month-selector">
          <button className="icon-button" onClick={handlePrevMonth} aria-label="上个月">
            <ChevronLeft size={20} />
          </button>
          <div style={{ position: "relative" }}>
            <button 
              className="dashboard-title-trigger"
              onClick={() => setShowMonthPicker(!showMonthPicker)}
            >
              <h2>{format(selectedMonth, "yyyy年 MM月")}</h2>
            </button>
            <AnimatePresence>
              {showMonthPicker && (
                <YearMonthPicker 
                  value={selectedMonth} 
                  onChange={setSelectedMonth} 
                  onClose={() => setShowMonthPicker(false)} 
                />
              )}
            </AnimatePresence>
          </div>
          <button className="icon-button" onClick={handleNextMonth} aria-label="下个月">
            <ChevronRight size={20} />
          </button>
        </div>
        <div style={{ width: "80px" }}></div>
      </header>

      <div className="calendar-panel">
        <div className="calendar-weekdays">
          {weekDays.map(d => (
            <div key={d} className="calendar-weekday">周{d}</div>
          ))}
        </div>
        {isLoading ? (
          <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", color: "var(--muted)" }}>正在载入日历...</div>
        ) : (
          <div className="calendar-grid">
            {calendarDays.map((item, idx) => (
              <div 
                key={idx} 
                className={`calendar-day-card ${item.isCurrentMonth ? "current-month" : "other-month"} ${item.isToday ? "today" : ""}`}
              >
                <div className="calendar-day-num">
                  {format(item.date, "d")}
                </div>
                <div className="calendar-day-net-container" style={{ marginTop: "auto" }}>
                  {item.netIncome !== undefined && item.netIncome !== 0 && (
                    <div className={`calendar-day-net ${item.netIncome > 0 ? "positive" : "negative"}`}>
                      {item.netIncome > 0 ? "+" : "-"}{yuan(Math.abs(item.netIncome))}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <button 
        className="quick-add-fab" 
        onClick={openQuickEntry}
        aria-label="记一笔"
      >
        <Plus size={24} />
      </button>

      <AnimatePresence>
        {draft && (
          <TransactionEditor
            transaction={draft}
            onChange={setDraft}
            onSave={saveDraft}
            onClose={() => setDraft(null)}
            accounts={accounts}
            categories={categories}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
