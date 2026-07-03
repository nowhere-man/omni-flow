use crate::error::AppError;
use serde::{Deserialize, Serialize};
use std::sync::Mutex;
use rusqlite::{params, Connection};

#[derive(Debug, Serialize, Deserialize)]
pub struct TrendDataPoint {
    pub date: String,
    pub income: f64,
    pub expense: f64,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct CategoryBreakdown {
    pub category_name: String,
    pub amount: f64,
    pub percent: f64,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct AssetData {
    pub account_type: String,
    pub balance: f64,
}

pub struct StatsEngine<'a> {
    conn: &'a Mutex<Connection>,
}

impl<'a> StatsEngine<'a> {
    pub fn new(conn: &'a Mutex<Connection>) -> Self {
        Self { conn }
    }

    /// Monthly trend
    pub fn get_monthly_trend(&self, ledger_id: &str, start_ts: i64, end_ts: i64) -> Result<Vec<TrendDataPoint>, AppError> {
        let conn = self.conn.lock().unwrap();
        // Uses strftime over unixepoch
        let query = "
            SELECT 
                strftime('%Y-%m', transaction_date, 'unixepoch') as month,
                SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END) as income,
                SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END) as expense
            FROM transactions
            WHERE ledger_id = ?1 AND is_excluded = 0 AND deleted_at IS NULL AND transaction_date BETWEEN ?2 AND ?3
            GROUP BY month
            ORDER BY month ASC
        ";
        let mut stmt = conn.prepare(query)?;
        let rows = stmt.query_map(params![ledger_id, start_ts, end_ts], |row| {
            Ok(TrendDataPoint {
                date: row.get(0)?,
                income: row.get(1)?,
                expense: row.get(2)?,
            })
        })?.collect::<Result<Vec<_>, _>>()?;
        
        Ok(rows)
    }

    /// Category breakdown
    pub fn get_category_breakdown(&self, ledger_id: &str, start_ts: i64, end_ts: i64, tx_type: &str) -> Result<Vec<CategoryBreakdown>, AppError> {
        let conn = self.conn.lock().unwrap();
        let query = "
            SELECT 
                COALESCE(c.name, '未分类') as category_name,
                SUM(t.amount) as amount
            FROM transactions t
            LEFT JOIN categories c ON t.category_id = c.id
            WHERE t.ledger_id = ?1 AND t.type = ?2 AND t.is_excluded = 0 AND t.deleted_at IS NULL AND t.transaction_date BETWEEN ?3 AND ?4
            GROUP BY category_name
            ORDER BY amount DESC
        ";
        let mut stmt = conn.prepare(query)?;
        let mut total = 0.0;
        let mut results = Vec::new();
        
        let rows = stmt.query_map(params![ledger_id, tx_type, start_ts, end_ts], |row| {
            let amount: f64 = row.get(1)?;
            Ok((row.get::<_, String>(0)?, amount))
        })?;

        for row in rows {
            let (name, amount) = row?;
            total += amount;
            results.push(CategoryBreakdown {
                category_name: name,
                amount,
                percent: 0.0,
            });
        }

        if total > 0.0 {
            for r in &mut results {
                r.percent = (r.amount / total * 100.0).round();
            }
        }

        Ok(results)
    }

    /// Assets overview
    pub fn get_assets_overview(&self) -> Result<Vec<AssetData>, AppError> {
        let conn = self.conn.lock().unwrap();
        let query = "
            SELECT account_type, SUM(balance) as balance
            FROM accounts
            WHERE deleted_at IS NULL
            GROUP BY account_type
            ORDER BY balance DESC
        ";
        let mut stmt = conn.prepare(query)?;
        let rows = stmt.query_map([], |row| {
            Ok(AssetData {
                account_type: row.get(0)?,
                balance: row.get(1)?,
            })
        })?.collect::<Result<Vec<_>, _>>()?;
        
        Ok(rows)
    }
}
