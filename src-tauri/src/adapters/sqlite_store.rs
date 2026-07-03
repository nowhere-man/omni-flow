use std::sync::Mutex;
use rusqlite::{params, Connection, OptionalExtension};

use crate::error::AppError;
use crate::models::{Account, Category, Ledger, Transaction, AccountType, TransactionType};
use crate::ports::storage::LedgerStore;
use std::str::FromStr;

pub struct SqliteStore {
    conn: Mutex<Connection>,
}

impl SqliteStore {
    pub fn new(db_path: &str) -> Result<Self, AppError> {
        let conn = Connection::open(db_path)?;
        conn.execute("PRAGMA foreign_keys = ON;", [])?;
        Ok(Self {
            conn: Mutex::new(conn),
        })
    }

    pub fn get_conn(&self) -> &Mutex<Connection> {
        &self.conn
    }

    pub fn run_migrations(&self) -> Result<(), AppError> {
        let conn = self.conn.lock().unwrap();
        let init_sql = include_str!("../../migrations/001_init.sql");
        conn.execute_batch(init_sql)?;
        Ok(())
    }

    pub fn init_defaults(&self) -> Result<(), AppError> {
        use ulid::Ulid;
        let now = chrono::Utc::now().timestamp();
        
        let default_ledger = Ledger {
            id: Ulid::new().to_string(),
            name: "默认账本".to_string(),
            budget: 0.0,
            created_at: now,
            updated_at: now,
            deleted_at: None,
        };
        
        let cash_account = Account {
            id: Ulid::new().to_string(),
            name: "现金".to_string(),
            account_type: AccountType::Cash,
            balance: 0.0,
            credit_limit: 0.0,
            bill_day: None,
            repay_day: None,
            created_at: now,
            updated_at: now,
            deleted_at: None,
        };

        self.create_ledger(&default_ledger)?;
        self.create_account(&cash_account)?;

        // Default Expense Categories
        let expenses = vec![
            "餐饮美食", "交通出行", "日用百货", "居住服务", 
            "休闲娱乐", "医疗健康", "教育培训", "充值缴费", "其他支出"
        ];
        for name in expenses {
            self.create_category(&Category {
                id: Ulid::new().to_string(),
                name: name.to_string(),
                category_type: TransactionType::Expense,
                parent_id: None,
                created_at: now,
                updated_at: now,
                deleted_at: None,
            })?;
        }

        // Default Income Categories
        let incomes = vec!["工资", "投资理财", "兼职副业", "奖金红包", "其他收入"];
        for name in incomes {
            self.create_category(&Category {
                id: Ulid::new().to_string(),
                name: name.to_string(),
                category_type: TransactionType::Income,
                parent_id: None,
                created_at: now,
                updated_at: now,
                deleted_at: None,
            })?;
        }

        Ok(())
    }
}

impl LedgerStore for SqliteStore {
    fn create_ledger(&self, ledger: &Ledger) -> Result<(), AppError> {
        let conn = self.conn.lock().unwrap();
        conn.execute(
            "INSERT INTO ledgers (id, name, budget, created_at, updated_at, deleted_at) VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
            params![ledger.id, ledger.name, ledger.budget, ledger.created_at, ledger.updated_at, ledger.deleted_at],
        )?;
        Ok(())
    }

    fn list_ledgers(&self) -> Result<Vec<Ledger>, AppError> {
        let conn = self.conn.lock().unwrap();
        let mut stmt = conn.prepare("SELECT id, name, budget, created_at, updated_at, deleted_at FROM ledgers WHERE deleted_at IS NULL")?;
        let ledgers = stmt.query_map([], |row| {
            Ok(Ledger {
                id: row.get(0)?,
                name: row.get(1)?,
                budget: row.get(2)?,
                created_at: row.get(3)?,
                updated_at: row.get(4)?,
                deleted_at: row.get(5)?,
            })
        })?.collect::<Result<Vec<_>, _>>()?;
        Ok(ledgers)
    }

    fn update_ledger(&self, ledger: &Ledger) -> Result<(), AppError> {
        let conn = self.conn.lock().unwrap();
        conn.execute(
            "UPDATE ledgers SET name = ?1, budget = ?2, updated_at = ?3, deleted_at = ?4 WHERE id = ?5",
            params![ledger.name, ledger.budget, ledger.updated_at, ledger.deleted_at, ledger.id],
        )?;
        Ok(())
    }

    fn create_account(&self, account: &Account) -> Result<(), AppError> {
        let conn = self.conn.lock().unwrap();
        conn.execute(
            "INSERT INTO accounts (id, name, account_type, balance, credit_limit, bill_day, repay_day, created_at, updated_at, deleted_at)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)",
            params![
                account.id,
                account.name,
                account.account_type.to_string(),
                account.balance,
                account.credit_limit,
                account.bill_day,
                account.repay_day,
                account.created_at,
                account.updated_at,
                account.deleted_at
            ],
        )?;
        Ok(())
    }

    fn list_accounts(&self) -> Result<Vec<Account>, AppError> {
        let conn = self.conn.lock().unwrap();
        let mut stmt = conn.prepare("SELECT id, name, account_type, balance, credit_limit, bill_day, repay_day, created_at, updated_at, deleted_at FROM accounts WHERE deleted_at IS NULL")?;
        let accounts = stmt.query_map([], |row| {
            let type_str: String = row.get(2)?;
            let account_type = AccountType::from_str(&type_str).unwrap_or(AccountType::Other);
            Ok(Account {
                id: row.get(0)?,
                name: row.get(1)?,
                account_type,
                balance: row.get(3)?,
                credit_limit: row.get(4)?,
                bill_day: row.get(5)?,
                repay_day: row.get(6)?,
                created_at: row.get(7)?,
                updated_at: row.get(8)?,
                deleted_at: row.get(9)?,
            })
        })?.collect::<Result<Vec<_>, _>>()?;
        Ok(accounts)
    }

    fn update_account(&self, account: &Account) -> Result<(), AppError> {
        let conn = self.conn.lock().unwrap();
        conn.execute(
            "UPDATE accounts SET name = ?1, account_type = ?2, balance = ?3, credit_limit = ?4, bill_day = ?5, repay_day = ?6, updated_at = ?7, deleted_at = ?8 WHERE id = ?9",
            params![
                account.name,
                account.account_type.to_string(),
                account.balance,
                account.credit_limit,
                account.bill_day,
                account.repay_day,
                account.updated_at,
                account.deleted_at,
                account.id
            ],
        )?;
        Ok(())
    }

    fn create_category(&self, category: &Category) -> Result<(), AppError> {
        let conn = self.conn.lock().unwrap();
        conn.execute(
            "INSERT INTO categories (id, name, type, parent_id, created_at, updated_at, deleted_at) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
            params![
                category.id,
                category.name,
                category.category_type.to_string(),
                category.parent_id,
                category.created_at,
                category.updated_at,
                category.deleted_at
            ],
        )?;
        Ok(())
    }

    fn list_categories(&self) -> Result<Vec<Category>, AppError> {
        let conn = self.conn.lock().unwrap();
        let mut stmt = conn.prepare("SELECT id, name, type, parent_id, created_at, updated_at, deleted_at FROM categories WHERE deleted_at IS NULL")?;
        let categories = stmt.query_map([], |row| {
            let type_str: String = row.get(2)?;
            let category_type = TransactionType::from_str(&type_str).unwrap_or(TransactionType::Expense);
            Ok(Category {
                id: row.get(0)?,
                name: row.get(1)?,
                category_type,
                parent_id: row.get(3)?,
                created_at: row.get(4)?,
                updated_at: row.get(5)?,
                deleted_at: row.get(6)?,
            })
        })?.collect::<Result<Vec<_>, _>>()?;
        Ok(categories)
    }

    fn update_category(&self, category: &Category) -> Result<(), AppError> {
        let conn = self.conn.lock().unwrap();
        conn.execute(
            "UPDATE categories SET name = ?1, type = ?2, parent_id = ?3, updated_at = ?4, deleted_at = ?5 WHERE id = ?6",
            params![
                category.name,
                category.category_type.to_string(),
                category.parent_id,
                category.updated_at,
                category.deleted_at,
                category.id
            ],
        )?;
        Ok(())
    }

    fn insert_transaction(&self, tx: &Transaction) -> Result<(), AppError> {
        let conn = self.conn.lock().unwrap();
        let tags_json = serde_json::to_string(&tx.tags).unwrap_or_else(|_| "[]".to_string());
        conn.execute(
            "INSERT INTO transactions (id, ledger_id, account_id, category_id, transaction_date, amount, type, merchant, notes, tags, is_excluded, external_source, external_id, created_at, updated_at, deleted_at)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16)",
            params![
                tx.id,
                tx.ledger_id,
                tx.account_id,
                tx.category_id,
                tx.transaction_date,
                tx.amount,
                tx.transaction_type.to_string(),
                tx.merchant,
                tx.notes,
                tags_json,
                tx.is_excluded,
                tx.external_source,
                tx.external_id,
                tx.created_at,
                tx.updated_at,
                tx.deleted_at
            ],
        )?;
        Ok(())
    }

    fn insert_transactions(&self, transactions: &[Transaction]) -> Result<(), AppError> {
        let mut conn = self.conn.lock().unwrap();
        let tx = conn.transaction()?;
        for t in transactions {
            let tags_json = serde_json::to_string(&t.tags).unwrap_or_else(|_| "[]".to_string());
            tx.execute(
                "INSERT INTO transactions (id, ledger_id, account_id, category_id, transaction_date, amount, type, merchant, notes, tags, is_excluded, external_source, external_id, created_at, updated_at, deleted_at)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16)",
                params![
                    t.id, t.ledger_id, t.account_id, t.category_id, t.transaction_date, t.amount,
                    t.transaction_type.to_string(), t.merchant, t.notes, tags_json, t.is_excluded,
                    t.external_source, t.external_id, t.created_at, t.updated_at, t.deleted_at
                ],
            )?;
        }
        tx.commit()?;
        Ok(())
    }

    fn get_transaction(&self, id: &str) -> Result<Option<Transaction>, AppError> {
        let conn = self.conn.lock().unwrap();
        let mut stmt = conn.prepare("SELECT id, ledger_id, account_id, category_id, transaction_date, amount, type, merchant, notes, tags, is_excluded, external_source, external_id, created_at, updated_at, deleted_at FROM transactions WHERE id = ?1 AND deleted_at IS NULL")?;
        let result = stmt.query_row(params![id], |row| {
            let type_str: String = row.get(6)?;
            let transaction_type = TransactionType::from_str(&type_str).unwrap_or(TransactionType::Expense);
            let tags_str: String = row.get(9)?;
            let tags: Vec<String> = serde_json::from_str(&tags_str).unwrap_or_default();
            
            Ok(Transaction {
                id: row.get(0)?,
                ledger_id: row.get(1)?,
                account_id: row.get(2)?,
                category_id: row.get(3)?,
                transaction_date: row.get(4)?,
                amount: row.get(5)?,
                transaction_type,
                merchant: row.get(7)?,
                notes: row.get(8)?,
                tags,
                is_excluded: row.get(10)?,
                external_source: row.get(11)?,
                external_id: row.get(12)?,
                created_at: row.get(13)?,
                updated_at: row.get(14)?,
                deleted_at: row.get(15)?,
            })
        }).optional()?;
        Ok(result)
    }

    fn list_transactions(&self, ledger_id: &str) -> Result<Vec<Transaction>, AppError> {
        let conn = self.conn.lock().unwrap();
        let mut stmt = conn.prepare("SELECT id, ledger_id, account_id, category_id, transaction_date, amount, type, merchant, notes, tags, is_excluded, external_source, external_id, created_at, updated_at, deleted_at FROM transactions WHERE ledger_id = ?1 AND deleted_at IS NULL ORDER BY transaction_date DESC")?;
        let transactions = stmt.query_map(params![ledger_id], |row| {
            let type_str: String = row.get(6)?;
            let transaction_type = TransactionType::from_str(&type_str).unwrap_or(TransactionType::Expense);
            let tags_str: String = row.get(9)?;
            let tags: Vec<String> = serde_json::from_str(&tags_str).unwrap_or_default();
            
            Ok(Transaction {
                id: row.get(0)?,
                ledger_id: row.get(1)?,
                account_id: row.get(2)?,
                category_id: row.get(3)?,
                transaction_date: row.get(4)?,
                amount: row.get(5)?,
                transaction_type,
                merchant: row.get(7)?,
                notes: row.get(8)?,
                tags,
                is_excluded: row.get(10)?,
                external_source: row.get(11)?,
                external_id: row.get(12)?,
                created_at: row.get(13)?,
                updated_at: row.get(14)?,
                deleted_at: row.get(15)?,
            })
        })?.collect::<Result<Vec<_>, _>>()?;
        Ok(transactions)
    }

    fn update_transaction(&self, tx: &Transaction) -> Result<(), AppError> {
        let conn = self.conn.lock().unwrap();
        let tags_json = serde_json::to_string(&tx.tags).unwrap_or_else(|_| "[]".to_string());
        conn.execute(
            "UPDATE transactions SET account_id = ?1, category_id = ?2, transaction_date = ?3, amount = ?4, type = ?5, merchant = ?6, notes = ?7, tags = ?8, is_excluded = ?9, updated_at = ?10, deleted_at = ?11 WHERE id = ?12",
            params![
                tx.account_id,
                tx.category_id,
                tx.transaction_date,
                tx.amount,
                tx.transaction_type.to_string(),
                tx.merchant,
                tx.notes,
                tags_json,
                tx.is_excluded,
                tx.updated_at,
                tx.deleted_at,
                tx.id
            ],
        )?;
        Ok(())
    }

    fn delete_transaction(&self, id: &str) -> Result<(), AppError> {
        let conn = self.conn.lock().unwrap();
        // Soft delete
        let now = chrono::Utc::now().timestamp();
        conn.execute("UPDATE transactions SET deleted_at = ?1 WHERE id = ?2", params![now, id])?;
        Ok(())
    }
}
