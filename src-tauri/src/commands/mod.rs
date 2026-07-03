pub mod stats;
pub mod category;
pub mod data;
pub mod sync;
use tauri::State;
use crate::adapters::sqlite_store::SqliteStore;
use crate::ports::storage::LedgerStore;
use crate::models::{Account, Ledger, Transaction};
use crate::error::AppError;
use crate::core::parsers::{BillParser, alipay::AlipayParser, wechat::WechatParser, jd::JdParser, meituan::MeituanParser, ccb::CcbParser};
use crate::core::rule_engine::RuleEngine;
use crate::core::dedup_engine::DedupEngine;
use ulid::Ulid;

#[tauri::command]
pub fn list_ledgers(store: State<'_, SqliteStore>) -> Result<Vec<Ledger>, AppError> {
    store.list_ledgers()
}

#[tauri::command]
pub fn list_accounts(store: State<'_, SqliteStore>) -> Result<Vec<Account>, AppError> {
    store.list_accounts()
}

#[tauri::command]
pub fn list_transactions(store: State<'_, SqliteStore>, ledger_id: String) -> Result<Vec<Transaction>, AppError> {
    store.list_transactions(&ledger_id)
}

#[tauri::command]
pub fn create_transaction(store: State<'_, SqliteStore>, transaction: Transaction) -> Result<(), AppError> {
    store.insert_transaction(&transaction)
}

#[tauri::command]
pub async fn import_bill(store: State<'_, SqliteStore>, file_path: String, ledger_id: String, account_id: String) -> Result<usize, AppError> {
    let parsers: Vec<Box<dyn BillParser>> = vec![
        Box::new(AlipayParser::new()),
        Box::new(WechatParser::new()),
        Box::new(JdParser::new()),
        Box::new(MeituanParser::new()),
        Box::new(CcbParser::new()),
    ];

    let mut matched_parser = None;
    for parser in parsers {
        if parser.probe(&file_path).unwrap_or(false) {
            matched_parser = Some(parser);
            break;
        }
    }

    let parser = matched_parser.ok_or_else(|| AppError::ParseError("未匹配到支持的账单格式".into()))?;
    let raw_txs = parser.parse(&file_path)?;

    // Ideally, we fetch rules from DB. Here we use an empty RuleEngine for now.
    let rule_engine = RuleEngine::new(vec![]);
    let existing_txs = store.list_transactions(&ledger_id)?;
    let mut to_insert = Vec::new();
    let source_name = parser.source_name();
    let now = chrono::Utc::now().timestamp();

    for mut raw in raw_txs {
        rule_engine.apply_to_raw(&mut raw);
        
        if DedupEngine::is_absolute_duplicate(&raw, &existing_txs, source_name) {
            continue;
        }
        
        if DedupEngine::is_fuzzy_duplicate(&raw, &existing_txs, source_name).is_some() {
            // Can be flagged for manual review, for now we skip or insert with tag.
            // Let's insert but add a fuzzy duplicate tag.
            // raw.tags.push("疑似重复".to_string());
        }

        let id = Ulid::new().to_string();
        let tx = Transaction {
            id,
            ledger_id: ledger_id.clone(),
            account_id: account_id.clone(),
            category_id: None, // Apply from rule engine in a real flow
            transaction_date: raw.transaction_date,
            amount: raw.amount,
            transaction_type: raw.transaction_type,
            merchant: raw.merchant,
            notes: raw.notes,
            tags: vec![],
            is_excluded: raw.is_excluded,
            external_source: Some(source_name.to_string()),
            external_id: raw.external_id,
            created_at: now,
            updated_at: now,
            deleted_at: None,
        };
        
        to_insert.push(tx);
    }

    let count = to_insert.len();
    store.insert_transactions(&to_insert)?;
    
    Ok(count)
}
