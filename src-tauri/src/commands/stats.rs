use tauri::State;
use crate::adapters::sqlite_store::SqliteStore;
use crate::error::AppError;

#[tauri::command]
pub fn get_monthly_trend(store: State<'_, SqliteStore>, ledger_id: String, start_ts: i64, end_ts: i64) -> Result<Vec<crate::core::stats_engine::TrendDataPoint>, AppError> {
    let engine = crate::core::stats_engine::StatsEngine::new(store.get_conn());
    engine.get_monthly_trend(&ledger_id, start_ts, end_ts)
}

#[tauri::command]
pub fn get_category_breakdown(store: State<'_, SqliteStore>, ledger_id: String, start_ts: i64, end_ts: i64, tx_type: String) -> Result<Vec<crate::core::stats_engine::CategoryBreakdown>, AppError> {
    let engine = crate::core::stats_engine::StatsEngine::new(store.get_conn());
    engine.get_category_breakdown(&ledger_id, start_ts, end_ts, &tx_type)
}

#[tauri::command]
pub fn get_assets_overview(store: State<'_, SqliteStore>) -> Result<Vec<crate::core::stats_engine::AssetData>, AppError> {
    let engine = crate::core::stats_engine::StatsEngine::new(store.get_conn());
    engine.get_assets_overview()
}
