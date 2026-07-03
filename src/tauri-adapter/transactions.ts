import { invoke } from "@tauri-apps/api/core";

export interface Ledger {
  id: string;
  name: string;
  budget: number;
  created_at: number;
  updated_at: number;
  deleted_at: number | null;
}

export interface Account {
  id: string;
  name: string;
  account_type: "cash" | "debit" | "credit" | "wallet" | "other";
  balance: number;
  credit_limit: number;
  bill_day: number | null;
  repay_day: number | null;
  created_at: number;
  updated_at: number;
  deleted_at: number | null;
}

export interface Transaction {
  id: string;
  ledger_id: string;
  account_id: string;
  category_id: string | null;
  transaction_date: number;
  amount: number;
  transaction_type: "expense" | "income";
  merchant: string | null;
  notes: string | null;
  tags: string[];
  is_excluded: boolean;
  external_source: string | null;
  external_id: string | null;
  created_at: number;
  updated_at: number;
  deleted_at: number | null;
}

export const TransactionAPI = {
  listLedgers: async (): Promise<Ledger[]> => {
    return invoke("list_ledgers");
  },
  
  listAccounts: async (): Promise<Account[]> => {
    return invoke("list_accounts");
  },
  
  listTransactions: async (ledgerId: string): Promise<Transaction[]> => {
    return invoke("list_transactions", { ledgerId });
  },
  
  createTransaction: async (transaction: Transaction): Promise<void> => {
    return invoke("create_transaction", { transaction });
  },
  
  importBill: async (filePath: string, ledgerId: string, accountId: string): Promise<number> => {
    return invoke("import_bill", { filePath, ledgerId, accountId });
  }
};
