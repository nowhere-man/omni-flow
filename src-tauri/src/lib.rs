pub mod core;
pub mod error;
pub mod models;
pub mod ports;
pub mod adapters;
pub mod commands;

use std::fs;
use std::path::PathBuf;
use tauri::Manager;
use adapters::sqlite_store::SqliteStore;
use ports::storage::LedgerStore;

pub fn init_db(app_dir: PathBuf) -> Result<SqliteStore, error::AppError> {
    fs::create_dir_all(&app_dir).map_err(|e| error::AppError::IoError(e.to_string()))?;
    let db_path = app_dir.join("ominiflow.db");
    
    let store = SqliteStore::new(db_path.to_str().unwrap())?;
    
    // Run migrations
    store.run_migrations()?;
    
    // Initialize default data if ledgers are empty
    let ledgers = store.list_ledgers()?;
    if ledgers.is_empty() {
        store.init_defaults()?;
    }
    
    Ok(store)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_dialog::init())
        .setup(|app| {
            let app_dir = app.path().app_data_dir().unwrap_or_else(|_| PathBuf::from("."));
            let store = init_db(app_dir).expect("Failed to initialize database");
            app.manage(store);
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            commands::list_ledgers,
            commands::list_accounts,
            commands::list_transactions,
            commands::create_transaction,
            commands::import_bill,
            commands::stats::get_monthly_trend,
            commands::stats::get_category_breakdown,
            commands::stats::get_assets_overview,
            commands::category::list_categories,
            commands::category::create_category,
            commands::category::update_category,
            commands::category::delete_category,
            commands::data::export_database,
            commands::data::clear_all_data,
            commands::sync::sync_to_webdav,
            commands::sync::restore_from_webdav
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
