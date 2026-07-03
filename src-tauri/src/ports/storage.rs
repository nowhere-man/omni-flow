use crate::error::AppError;
use crate::models::{Account, Category, Ledger, Transaction};

pub trait LedgerStore: Send + Sync {
    // ---- Ledgers ----
    fn create_ledger(&self, ledger: &Ledger) -> Result<(), AppError>;
    fn list_ledgers(&self) -> Result<Vec<Ledger>, AppError>;
    fn update_ledger(&self, ledger: &Ledger) -> Result<(), AppError>;

    // ---- Accounts ----
    fn create_account(&self, account: &Account) -> Result<(), AppError>;
    fn list_accounts(&self) -> Result<Vec<Account>, AppError>;
    fn update_account(&self, account: &Account) -> Result<(), AppError>;

    // ---- Categories ----
    fn create_category(&self, category: &Category) -> Result<(), AppError>;
    fn list_categories(&self) -> Result<Vec<Category>, AppError>;
    fn update_category(&self, category: &Category) -> Result<(), AppError>;

    // ---- Transactions ----
    fn insert_transaction(&self, transaction: &Transaction) -> Result<(), AppError>;
    fn insert_transactions(&self, transactions: &[Transaction]) -> Result<(), AppError>;
    fn get_transaction(&self, id: &str) -> Result<Option<Transaction>, AppError>;
    fn list_transactions(&self, ledger_id: &str) -> Result<Vec<Transaction>, AppError>;
    fn update_transaction(&self, transaction: &Transaction) -> Result<(), AppError>;
    fn delete_transaction(&self, id: &str) -> Result<(), AppError>;
}
