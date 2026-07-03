use super::{BillParser, RawTransaction};
use crate::error::AppError;
use crate::models::TransactionType;
use chrono::NaiveDateTime;
use std::fs;
use std::io::Cursor;

pub struct JdParser;

impl JdParser {
    pub fn new() -> Self {
        Self
    }
}

impl BillParser for JdParser {
    fn source_name(&self) -> &'static str {
        "jd"
    }

    fn probe(&self, file_path: &str) -> Result<bool, AppError> {
        let content = fs::read_to_string(file_path).unwrap_or_default();
        Ok(content.contains("京东") && content.contains("交易单号") && content.contains("商户名称"))
    }

    fn parse(&self, file_path: &str) -> Result<Vec<RawTransaction>, AppError> {
        let content = fs::read_to_string(file_path).map_err(|e| AppError::IoError(e.to_string()))?;
        // Remove UTF-8 BOM if present
        let content = content.strip_prefix('\u{feff}').unwrap_or(&content);

        let mut lines = content.lines().peekable();
        
        while let Some(&line) = lines.peek() {
            if line.contains("交易时间") && line.contains("商户名称") && line.contains("金额") {
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
            
            // Expected JD Columns:
            // 0: 交易时间
            // 1: 商户名称
            // 2: 交易说明
            // 3: 交易单号
            // 4: 支付方式
            // 5: 收/支
            // 6: 交易状态
            // 7: 金额
            // 8: 交易分类 (might be column 6/7/8 depending on export version, let's look at PRD details: 交易时间、商户名称、交易说明、金额、交易状态、收/支、交易分类、交易订单号)

            if record.len() < 8 {
                continue;
            }

            let date_str = record.get(0).unwrap_or("").trim();
            if !date_str.starts_with("20") {
                continue;
            }

            let dt = NaiveDateTime::parse_from_str(date_str, "%Y-%m-%d %H:%M:%S")
                .map_err(|e| AppError::ParseError(format!("Invalid date format: {}", e)))?;
            let transaction_date = dt.and_utc().timestamp();

            let merchant = record.get(1).map(|s| s.trim().to_string());
            let notes = record.get(2).map(|s| s.trim().to_string());
            let external_id = record.get(3).map(|s| s.trim().to_string());
            let direction_str = record.get(5).unwrap_or("").trim();
            let amount_str = record.get(7).unwrap_or("0").trim().replace(",", "");
            let amount = amount_str.parse::<f64>().unwrap_or(0.0);
            
            // Try to find category in later columns
            let category_hint = (0..record.len())
                .map(|i| record.get(i).unwrap_or(""))
                .find(|s| *s == "食品酒饮" || *s == "生活服务" || *s == "百货")
                .map(|s| s.to_string());

            let mut is_excluded = false;
            let transaction_type = match direction_str {
                "收入" => TransactionType::Income,
                "支出" => TransactionType::Expense,
                "不计收支" => {
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
                merchant,
                notes,
                is_excluded,
                external_id,
                category_hint,
            });
        }

        Ok(transactions)
    }
}
