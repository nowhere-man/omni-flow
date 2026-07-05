import { useEffect, useState } from "react";
import { Play, Plus, Trash2 } from "lucide-react";
import { Account, Ledger, PeriodicBill, TransactionAPI } from "../../../tauri-adapter/transactions";
import { useAppStore } from "../../../stores/appStore";

const ts = () => Math.floor(Date.now() / 1000);

export default function ManagementSettings() {
  const { fetchInitialData } = useAppStore();
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [bills, setBills] = useState<PeriodicBill[]>([]);
  const [message, setMessage] = useState("");

  async function load() {
    const [nextLedgers, nextAccounts, nextBills] = await Promise.all([
      TransactionAPI.listLedgers(),
      TransactionAPI.listAccounts(),
      TransactionAPI.listPeriodicBills(),
    ]);
    setLedgers(nextLedgers);
    setAccounts(nextAccounts);
    setBills(nextBills);
  }

  useEffect(() => {
    load();
  }, []);

  async function addLedger() {
    await TransactionAPI.createLedger({ id: crypto.randomUUID(), name: "新账本", budget: 0, created_at: ts(), updated_at: ts(), deleted_at: null });
    await load();
    await fetchInitialData();
  }

  async function addAccount() {
    await TransactionAPI.createAccount({
      id: crypto.randomUUID(),
      name: "新账户",
      account_type: "cash",
      balance: 0,
      credit_limit: 0,
      bill_day: null,
      repay_day: null,
      created_at: ts(),
      updated_at: ts(),
      deleted_at: null,
    });
    await load();
    await fetchInitialData();
  }

  async function addPeriodicBill() {
    const account = accounts[0];
    if (!account) {
      setMessage("请先创建账户");
      return;
    }
    await TransactionAPI.createPeriodicBill({
      id: crypto.randomUUID(),
      name: "新周期账单",
      amount: 0,
      bill_type: "expense",
      category_id: null,
      account_id: account.id,
      cron_expression: "monthly",
      next_date: ts(),
      created_at: ts(),
      updated_at: ts(),
      deleted_at: null,
    });
    await load();
  }

  async function generatePending() {
    const created = await TransactionAPI.generatePendingConfirmations(ts());
    setMessage(`已生成 ${created.length} 张待确认卡片`);
  }

  return (
    <div className="settings-stack wide">
      {message && <div className="panel panel-pad">{message}</div>}
      <Section title="账本" onAdd={addLedger}>
        {ledgers.map((ledger) => (
          <Row key={ledger.id} title={ledger.name} meta={`预算 ${ledger.budget}`} onDelete={async () => { await TransactionAPI.deleteLedger(ledger.id); await load(); }} />
        ))}
      </Section>

      <Section title="账户" onAdd={addAccount}>
        {accounts.map((account) => (
          <Row key={account.id} title={account.name} meta={`${account.account_type} · 余额 ${account.balance}`} onDelete={async () => { await TransactionAPI.deleteAccount(account.id); await load(); }} />
        ))}
      </Section>

      <Section title="周期账单" onAdd={addPeriodicBill} action={<button className="ghost-button" onClick={generatePending}><Play size={16} />检测到期</button>}>
        {bills.map((bill) => (
          <Row key={bill.id} title={bill.name} meta={`${bill.bill_type} · ${bill.amount}`} onDelete={async () => { await TransactionAPI.deletePeriodicBill(bill.id); await load(); }} />
        ))}
      </Section>
    </div>
  );
}

function Section({ title, children, onAdd, action }: { title: string; children: React.ReactNode; onAdd: () => void; action?: React.ReactNode }) {
  return (
    <section className="panel panel-pad">
      <div className="settings-head-row">
        <h2 className="section-title">{title}</h2>
        <div className="toolbar">
          {action}
          <button className="primary-button" onClick={onAdd}><Plus size={16} />新增</button>
        </div>
      </div>
      <div className="line-list">{children}</div>
    </section>
  );
}

function Row({ title, meta, onDelete, onRun }: { title: string; meta: string; onDelete: () => void; onRun?: () => void }) {
  return (
    <div className="line-item">
      <div>
        <div className="line-title">{title}</div>
        <div className="line-meta">{meta}</div>
      </div>
      <div className="toolbar">
        {onRun && <button className="icon-button" onClick={onRun} aria-label="测试"><Play size={15} /></button>}
        <button className="icon-button" onClick={onDelete} aria-label="删除"><Trash2 size={15} /></button>
      </div>
    </div>
  );
}
