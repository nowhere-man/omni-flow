use super::{BillParser, RawTransaction};
use crate::error::AppError;
use std::fs;

pub struct QingziParser;

impl QingziParser {
    pub fn new() -> Self {
        Self
    }
}

impl BillParser for QingziParser {
    fn source_name(&self) -> &'static str {
        "qingzi"
    }

    fn probe(&self, file_path: &str) -> Result<bool, AppError> {
        let content = fs::read_to_string(file_path).unwrap_or_default();
        Ok(content.contains("青子记账") || content.contains("Qingzi")) // Adjust according to actual format
    }

    fn parse(&self, _file_path: &str) -> Result<Vec<RawTransaction>, AppError> {
        // Implement parsing logic based on examples/青子记账.json
        // Returning empty for now as placeholder since format details are not fully in memory
        Ok(vec![])
    }
}
