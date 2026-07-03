import { invoke } from "@tauri-apps/api/core";

export interface TrendDataPoint {
  date: string;
  income: number;
  expense: number;
}

export interface CategoryBreakdown {
  category_name: string;
  amount: number;
  percent: number;
}

export interface AssetData {
  account_type: string;
  balance: number;
}

export const StatsAPI = {
  getMonthlyTrend: async (ledgerId: string, startTs: number, endTs: number): Promise<TrendDataPoint[]> => {
    return invoke("get_monthly_trend", { ledgerId, startTs, endTs });
  },
  
  getCategoryBreakdown: async (ledgerId: string, startTs: number, endTs: number, txType: "expense" | "income"): Promise<CategoryBreakdown[]> => {
    return invoke("get_category_breakdown", { ledgerId, startTs, endTs, txType });
  },
  
  getAssetsOverview: async (): Promise<AssetData[]> => {
    return invoke("get_assets_overview");
  }
};
