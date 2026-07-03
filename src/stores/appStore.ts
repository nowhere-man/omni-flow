import { create } from 'zustand';
import { TransactionAPI, Ledger, Account, Transaction } from '../tauri-adapter/transactions';

interface AppState {
  ledgers: Ledger[];
  accounts: Account[];
  currentLedgerId: string | null;
  transactions: Transaction[];
  isLoading: boolean;
  error: string | null;
  
  fetchInitialData: () => Promise<void>;
  fetchTransactions: () => Promise<void>;
  setCurrentLedgerId: (id: string) => void;
}

export const useAppStore = create<AppState>((set, get) => ({
  ledgers: [],
  accounts: [],
  currentLedgerId: null,
  transactions: [],
  isLoading: false,
  error: null,

  fetchInitialData: async () => {
    set({ isLoading: true, error: null });
    try {
      const [ledgers, accounts] = await Promise.all([
        TransactionAPI.listLedgers(),
        TransactionAPI.listAccounts()
      ]);
      
      const currentLedgerId = ledgers.length > 0 ? ledgers[0].id : null;
      
      set({ 
        ledgers, 
        accounts, 
        currentLedgerId,
        isLoading: false 
      });
      
      if (currentLedgerId) {
        get().fetchTransactions();
      }
    } catch (err: any) {
      set({ error: err.toString(), isLoading: false });
    }
  },

  fetchTransactions: async () => {
    const { currentLedgerId } = get();
    if (!currentLedgerId) return;
    
    set({ isLoading: true, error: null });
    try {
      const transactions = await TransactionAPI.listTransactions(currentLedgerId);
      set({ transactions, isLoading: false });
    } catch (err: any) {
      set({ error: err.toString(), isLoading: false });
    }
  },
  
  setCurrentLedgerId: (id: string) => {
    set({ currentLedgerId: id });
    get().fetchTransactions();
  }
}));
