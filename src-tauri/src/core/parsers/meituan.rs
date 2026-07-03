use super::{BillParser, RawTransaction};
use crate::error::AppError;
use crate::models::TransactionType;
use chrono::NaiveDateTime;
use std::fs;
use std::io::Cursor;

pub struct MeituanParser;

impl MeituanParser {
    pub fn new() -> Self {
        Self
    }
}

impl BillParser for MeituanParser {
    fn source_name(&self) -> &'static str {
        "meituan"
    }

    fn probe(&self, file_path: &str) -> Result<bool, AppError> {
        let content = fs::read_to_string(file_path).unwrap_or_default();
        Ok(content.contains("美团") && content.contains("交易单号"))
    }

    fn parse(&self, file_path: &str) -> Result<Vec<RawTransaction>, AppError> {
        let content = fs::read_to_string(file_path).map_err(|e| AppError::IoError(e.to_string()))?;
        let content = content.strip_prefix('\u{feff}').unwrap_or(&content);

        let mut lines = content.lines().peekable();
        
        while let Some(&line) = lines.peek() {
            if line.contains("交易时间") && line.contains("收/支") {
                break;
            }
            lines.next();
        }

        let csv_content = lines.collect::<Vec<_>>().join("\n");
        let mut reader = csv::ReaderBuilder::new()
            .has_headers(true)
            .trim(csv::Trim::All)
            .from_reader(Cursor::new(csv_content));

        let mut transactions = Vec::new();

        for result in reader.records() {
            let record = match result {
                Ok(r) => r,
                Err(_) => continue,
            };
            
            // Expected Meituan Columns
            // Often: 0: 交易时间, 1: 交易类型, 2: 交易对方, 3: 收/支, 4: 金额, 5: 支付方式, 6: 状态, 7: 交易单号
            // Actual columns from PRD: 交易成功时间、交易类型、订单标题、收/支、订单金额、实付金额、交易单号

            if record.len() < 7 {
                continue;
            }

            let date_str = record.get(0).unwrap_or("").trim();
            if !date_str.starts_with("20") {
                continue;
            }

            let dt = NaiveDateTime::parse_from_str(date_str, "%Y-%m-%d %H:%M:%S")
                .unwrap_or_else(|_| chrono::DateTime::from_timestamp(0, 0).unwrap().naive_utc());
            let transaction_date = dt.and_utc().timestamp();

            let notes = record.get(2).map(|s| s.trim().to_string());
            let direction_str = record.get(3).unwrap_or("").trim();
            
            // Use actual paid amount if available, else use order amount
            let amount_str = record.get(5).unwrap_or("0").trim().replace("¥", "").replace(",", "");
            let amount = amount_str.parse::<f64>().unwrap_or(0.0);
            
            let external_id = record.get(6).map(|s| s.trim().to_string()); // fallback
            
            // Meituan may not have category
            let category_hint = None;

            let mut is_excluded = false;
            let transaction_type = match direction_str {
                "收入" => TransactionType::Income,
                "支出" => TransactionType::Expense,
                "不计收支" | "退款" => {
                    is_excluded = true;
                    if amount >= 0.0 { TransactionType::Income } else { TransactionType::Expense }
                },
                _ => {
                    if amount >= 0.0 { TransactionType::Income } else { TransactionType::Expense }
                }
            };

            transactions.push(RawTransaction {
                transaction_date,
                transaction_type,
                amount: amount.abs(),
                merchant: Some("美团".to_string()),
                notes,
                is_excluded,
                external_id,
                category_hint,
            });
        }

        Ok(transactions)
    }
}
