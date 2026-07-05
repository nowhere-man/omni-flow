import { useCallback, useEffect, useMemo, useState, useRef } from "react";
import { ArrowDown, ArrowUp, ChevronLeft, ChevronRight, Save, Trash2, X, LayoutGrid, List } from "lucide-react";
import { invoke } from "@tauri-apps/api/core";
import { startOfMonth, endOfMonth, addMonths, subMonths, format } from "date-fns";
import { zhCN } from "date-fns/locale";
import { AnimatePresence, motion } from "framer-motion";
import { useAppStore } from "../../stores/appStore";
import { Transaction, TransactionAPI } from "../../tauri-adapter/transactions";
import { yuan } from "../../lib/format";
import { Category } from "../../models";
import { Select } from "../../components/ui/Select";
import { DatePicker, ScrollColumn } from "../../components/ui/DatePicker";

function YearMonthPicker({ value, onChange, onClose }: { value: Date, onChange: (date: Date) => void, onClose: () => void }) {
  const containerRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        onClose();
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [onClose]);

  const years = Array.from({ length: 21 }, (_, i) => (2020 + i).toString());
  const months = Array.from({ length: 12 }, (_, i) => (i + 1).toString().padStart(2, "0"));

  return (
    <motion.div 
      initial={{ opacity: 0, y: -10, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -10, scale: 0.95 }}
      transition={{ duration: 0.15 }}
      ref={containerRef}
      style={{ position: "absolute", top: "calc(100% + 8px)", left: "50%", transform: "translateX(-50%)", background: "var(--background)", border: "1px solid var(--border)", borderRadius: "12px", boxShadow: "0 10px 30px rgba(0,0,0,0.2)", zIndex: 100, display: "flex", width: "200px", height: "240px", overflow: "hidden" }}
    >
      <div style={{ flex: 1, borderRight: "1px solid var(--border)", display: "flex", flexDirection: "column" }}>
        <div style={{ padding: "8px", textAlign: "center", fontSize: "12px", color: "var(--muted)", fontWeight: 600, background: "color-mix(in srgb, var(--surface) 50%, transparent)" }}>年份</div>
        <ScrollColumn options={years} value={format(value, "yyyy")} onChange={(y) => onChange(new Date(`${y}-${format(value, "MM")}-01`))} />
      </div>
      <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
        <div style={{ padding: "8px", textAlign: "center", fontSize: "12px", color: "var(--muted)", fontWeight: 600, background: "color-mix(in srgb, var(--surface) 50%, transparent)" }}>月份</div>
        <ScrollColumn options={months} value={format(value, "MM")} onChange={(m) => onChange(new Date(`${format(value, "yyyy")}-${m}-01`))} />
      </div>
    </motion.div>
  );
}

const now = () => Math.floor(Date.now() / 1000);

export default function TransactionList() {
  const { accounts, currentLedgerId, fetchInitialData } = useAppStore();
  
  const [selectedMonth, setSelectedMonth] = useState(() => startOfMonth(new Date()));
  const [showMonthPicker, setShowMonthPicker] = useState(false);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [metrics, setMetrics] = useState({ income: 0, expense: 0 });
  const [isLoading, setIsLoading] = useState(false);
  
  const [layoutMode, setLayoutMode] = useState<"card" | "list">("card");
  const [editing, setEditing] = useState<Transaction | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [categories, setCategories] = useState<Category[]>([]);

  useEffect(() => {
    fetchInitialData();
    invoke<Category[]>("list_categories").then(setCategories).catch(() => setCategories([]));
  }, [fetchInitialData]);

  const loadMonthData = useCallback(async () => {
    if (!currentLedgerId) return;
    setIsLoading(true);
    const start = Math.floor(selectedMonth.getTime() / 1000);
    const end = Math.floor(endOfMonth(selectedMonth).getTime() / 1000);
    try {
      const res = await TransactionAPI.searchTransactions(currentLedgerId, { start_date: start, end_date: end });
      setTransactions(res.transactions);
      setMetrics({ income: res.total_income, expense: res.total_expense });
    } finally {
      setIsLoading(false);
    }
  }, [currentLedgerId, selectedMonth]);

  useEffect(() => {
    loadMonthData();
  }, [loadMonthData]);

  const categoryLabelById = useMemo(() => new Map(categoryOptions(categories).map((category) => [category.id, category.label])), [categories]);

  const groupedTransactions = useMemo(() => {
    const groups = new Map<string, Transaction[]>();
    for (const t of transactions) {
      const dateStr = format(new Date(t.transaction_date * 1000), "yyyy-MM-dd");
      if (!groups.has(dateStr)) groups.set(dateStr, []);
      groups.get(dateStr)!.push(t);
    }
    const sortedKeys = Array.from(groups.keys()).sort((a, b) => b.localeCompare(a));
    return sortedKeys.map(k => {
      const dateObj = new Date(k);
      return {
        dateString: k,
        label: format(dateObj, "MM月dd日 EEEE", { locale: zhCN }),
        transactions: groups.get(k)!.sort((a, b) => b.transaction_date - a.transaction_date)
      };
    });
  }, [transactions]);

  async function save() {
    if (!editing) return;
    const payload = { ...editing, updated_at: now() };
    const exists = transactions.some((transaction) => transaction.id === payload.id);
    if (exists) {
      await TransactionAPI.updateTransaction(payload);
    } else {
      await TransactionAPI.createTransaction(payload);
    }
    setEditing(null);
    setConfirmDelete(false);
    await loadMonthData();
  }

  async function remove() {
    if (!editing) return;
    await TransactionAPI.deleteTransaction(editing.id);
    setEditing(null);
    setConfirmDelete(false);
    await loadMonthData();
  }

  const handlePrevMonth = () => setSelectedMonth((m) => subMonths(m, 1));
  const handleNextMonth = () => setSelectedMonth((m) => addMonths(m, 1));
  
  const netIncome = metrics.income - metrics.expense;
  const netColor = netIncome > 0 ? "var(--success)" : netIncome < 0 ? "var(--danger)" : "inherit";

  return (
    <div className="money-flow-page" style={{ padding: "24px", maxWidth: "1000px", margin: "0 auto" }}>
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "32px", position: "relative" }}>
        <div style={{ width: "80px" }}></div>
        <div style={{ display: "flex", alignItems: "center", gap: "16px", position: "relative" }}>
          <button className="icon-button" onClick={handlePrevMonth}><ChevronLeft size={24} /></button>
          <div style={{ position: "relative" }}>
            <div 
              style={{ cursor: "pointer", padding: "8px 16px", borderRadius: "8px", background: showMonthPicker ? "var(--surface)" : "color-mix(in srgb, var(--surface) 50%, transparent)", transition: "background 0.2s" }} 
              onClick={() => setShowMonthPicker(!showMonthPicker)}
            >
              <h2 style={{ margin: 0, fontSize: "22px", fontWeight: 600 }}>{format(selectedMonth, "yyyy年 MM月")}</h2>
            </div>
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
          <button className="icon-button" onClick={handleNextMonth}><ChevronRight size={24} /></button>
        </div>
        
        <div style={{ width: "80px", display: "flex", justifyContent: "flex-end", gap: "4px" }}>
          <button className={`icon-button ${layoutMode === "card" ? "active" : ""}`} onClick={() => setLayoutMode("card")} style={{ background: layoutMode === "card" ? "var(--surface)" : "transparent" }}><LayoutGrid size={18} /></button>
          <button className={`icon-button ${layoutMode === "list" ? "active" : ""}`} onClick={() => setLayoutMode("list")} style={{ background: layoutMode === "list" ? "var(--surface)" : "transparent" }}><List size={18} /></button>
        </div>
      </header>

      <section style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "16px", marginBottom: "32px" }}>
        <div className="panel panel-pad" style={{ textAlign: "center", padding: "24px" }}>
          <div style={{ color: "var(--muted)", fontSize: "14px", marginBottom: "8px" }}>总收入</div>
          <div style={{ fontSize: "28px", fontWeight: 700, color: "var(--success)" }}>{yuan(metrics.income)}</div>
        </div>
        <div className="panel panel-pad" style={{ textAlign: "center", padding: "24px" }}>
          <div style={{ color: "var(--muted)", fontSize: "14px", marginBottom: "8px" }}>总支出</div>
          <div style={{ fontSize: "28px", fontWeight: 700, color: "var(--danger)" }}>{yuan(metrics.expense)}</div>
        </div>
        <div className="panel panel-pad" style={{ textAlign: "center", padding: "24px" }}>
          <div style={{ color: "var(--muted)", fontSize: "14px", marginBottom: "8px" }}>净收入</div>
          <div style={{ fontSize: "28px", fontWeight: 700, color: netColor }}>{yuan(netIncome)}</div>
        </div>
      </section>

      <section className="timeline-panel hide-scrollbar" style={{ flex: 1, overflowY: "auto", paddingBottom: "100px", padding: "0 4px" }}>
        {isLoading ? (
          <div className="empty-line">正在载入数据</div>
        ) : groupedTransactions.length === 0 ? (
          <div className="empty-line">本月无记录</div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            {groupedTransactions.map((group) => (
              <div key={group.dateString}>
                <div style={{ fontSize: "14px", fontWeight: 600, color: "var(--muted)", marginBottom: "12px", paddingLeft: "4px" }}>
                  {group.label}
                </div>
                
                {layoutMode === "list" ? (
                  <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                    {group.transactions.map((transaction) => (
                      <article key={transaction.id} className="transaction-card" onClick={() => setEditing(transaction)} style={{ cursor: "pointer", gridTemplateColumns: "auto 1fr" }}>
                        <div className={transaction.transaction_type === "expense" ? "tx-symbol expense" : "tx-symbol income"}>
                          {transaction.transaction_type === "expense" ? <ArrowUp size={16} /> : <ArrowDown size={16} />}
                        </div>
                        <div className="tx-body">
                          <div className="tx-mainline">
                            <strong>{transaction.merchant || transaction.notes || "未命名交易"}</strong>
                            <b style={{ color: transaction.transaction_type === "expense" ? "var(--danger)" : "var(--success)" }}>
                              {transaction.transaction_type === "expense" ? "-" : "+"}{yuan(transaction.amount)}
                            </b>
                          </div>
                          <div className="tx-meta">
                            <span>{format(new Date(transaction.transaction_date * 1000), "HH:mm")}</span>
                            {transaction.category_id && <span>{categoryLabelById.get(transaction.category_id) || "未分类"}</span>}
                            <span>{transaction.external_source || "manual"}</span>
                            {transaction.is_excluded && <span>不计收支</span>}
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                ) : (
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "12px" }}>
                    {group.transactions.map((transaction) => (
                      <div 
                        key={transaction.id} 
                        onClick={() => setEditing(transaction)}
                        className="panel hover-transform"
                        style={{ padding: "16px", cursor: "pointer", display: "flex", flexDirection: "column", gap: "12px", transition: "transform 0.15s, box-shadow 0.15s" }}
                      >
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                           <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                             <div className={transaction.transaction_type === "expense" ? "tx-symbol expense" : "tx-symbol income"} style={{ width: "32px", height: "32px", flexShrink: 0 }}>
                               {transaction.transaction_type === "expense" ? <ArrowUp size={14} /> : <ArrowDown size={14} />}
                             </div>
                             <div>
                               <div style={{ fontWeight: 600, fontSize: "15px" }}>{transaction.merchant || transaction.notes || "未命名交易"}</div>
                               <div style={{ fontSize: "12px", color: "var(--muted)", marginTop: "2px" }}>
                                 {transaction.category_id ? categoryLabelById.get(transaction.category_id) : "未分类"}
                               </div>
                             </div>
                           </div>
                           <b style={{ fontSize: "16px", color: transaction.transaction_type === "expense" ? "var(--danger)" : "var(--success)" }}>
                             {transaction.transaction_type === "expense" ? "-" : "+"}{yuan(transaction.amount)}
                           </b>
                        </div>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", fontSize: "12px", color: "var(--muted)" }}>
                          <span>{format(new Date(transaction.transaction_date * 1000), "HH:mm")}</span>
                          {transaction.is_excluded && <span style={{ background: "color-mix(in srgb, var(--muted) 20%, transparent)", padding: "2px 6px", borderRadius: "4px" }}>不计收支</span>}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      <AnimatePresence>
        {editing && (
          <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "rgba(0,0,0,0.5)", zIndex: 1000, display: "flex", alignItems: "center", justifyContent: "center" }} onClick={() => { setEditing(null); setConfirmDelete(false); }}>
            <motion.div 
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              transition={{ duration: 0.2 }}
              onClick={(e) => e.stopPropagation()}
              className="panel"
              style={{ width: "420px", padding: "24px", display: "flex", flexDirection: "column", gap: "16px", background: "var(--background)", border: "1px solid var(--border)", borderRadius: "12px", boxShadow: "0 20px 40px rgba(0,0,0,0.3)" }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                <h2 style={{ margin: 0, fontSize: "18px" }}>编辑明细</h2>
                <button className="icon-button" onClick={() => { setEditing(null); setConfirmDelete(false); }} aria-label="关闭"><X size={18} /></button>
              </div>

              <div className="segmented">
                <button className={editing.transaction_type === "expense" ? "active" : ""} onClick={() => setEditing({ ...editing, transaction_type: "expense" })}>支出</button>
                <button className={editing.transaction_type === "income" ? "active" : ""} onClick={() => setEditing({ ...editing, transaction_type: "income" })}>收入</button>
              </div>
              
              <input className="money-input" type="number" min="0" step="0.01" value={editing.amount} onChange={(event) => setEditing({ ...editing, amount: Number(event.target.value) })} style={{ fontSize: "32px", textAlign: "center", padding: "12px", border: "none", background: "transparent", borderBottom: "2px solid var(--border)", outline: "none", color: "var(--foreground)" }} />
              
              <input className="field" value={editing.merchant || ""} placeholder="商户" onChange={(event) => setEditing({ ...editing, merchant: event.target.value })} />
              <input className="field" value={editing.notes || ""} placeholder="备注" onChange={(event) => setEditing({ ...editing, notes: event.target.value })} />
              
              <DatePicker 
                value={editing.transaction_date}
                showTime={true}
                onChange={(val) => {
                  if (val) setEditing({ ...editing, transaction_date: val });
                }}
              />
              
              <Select
                value={editing.account_id}
                onChange={(val) => setEditing({ ...editing, account_id: val })}
                options={accounts.map((account) => ({ value: account.id, label: account.name }))}
              />
              
              <Select
                value={editing.category_id || ""}
                onChange={(val) => setEditing({ ...editing, category_id: val || null })}
                options={[
                  { value: "", label: "未分类" },
                  ...categoryOptions(categories).map((opt) => ({ value: opt.id, label: opt.label }))
                ]}
              />

              <label style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "4px", cursor: "pointer" }}>
                <input type="checkbox" checked={editing.is_excluded} onChange={(event) => setEditing({ ...editing, is_excluded: event.target.checked })} />
                <span>不计入各项收支统计</span>
              </label>
              
              <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginTop: "8px" }}>
                 <button className="primary-button" onClick={save} style={{ padding: "12px", fontSize: "15px" }}><Save size={17} />保存更改</button>
                 
                 {confirmDelete ? (
                   <div style={{ display: "flex", gap: "8px" }}>
                     <button className="primary-button" onClick={remove} style={{ flex: 1, padding: "12px", fontSize: "15px", background: "var(--danger)" }}>确认删除</button>
                     <button className="ghost-button" onClick={() => setConfirmDelete(false)} style={{ flex: 1, padding: "12px", fontSize: "15px", border: "1px solid var(--border)" }}>取消</button>
                   </div>
                 ) : (
                   <button className="ghost-button" onClick={() => setConfirmDelete(true)} style={{ padding: "12px", fontSize: "15px", color: "var(--danger)", border: "1px solid color-mix(in srgb, var(--danger) 20%, transparent)" }}><Trash2 size={17} />删除明细</button>
                 )}
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}

function categoryOptions(categories: Category[]) {
  const parents = categories.filter((category) => !category.parent_id);
  const childrenByParent = new Map<string, Category[]>();
  for (const category of categories) {
    if (!category.parent_id) continue;
    childrenByParent.set(category.parent_id, [...(childrenByParent.get(category.parent_id) || []), category]);
  }
  return parents.flatMap((parent) => [
    { id: parent.id, label: parent.name },
    ...(childrenByParent.get(parent.id) || []).map((child) => ({ id: child.id, label: `${parent.name} / ${child.name}` })),
  ]);
}
