use crate::core::parsers::RawTransaction;
use crate::models::Rule;
use regex::Regex;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RuleCondition {
    pub match_type: String, // "merchant_keyword", "notes_keyword", "regex", "amount_range"
    pub value: String, // Value or JSON serialized params
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RuleAction {
    pub action_type: String, // "set_category", "set_account", "add_tag", "exclude"
    pub value: String,
}

pub struct RuleEngine {
    rules: Vec<Rule>,
}

impl RuleEngine {
    pub fn new(mut rules: Vec<Rule>) -> Self {
        rules.sort_by_key(|r| -r.priority); // Highest priority first
        Self { rules }
    }

    pub fn apply_to_raw(&self, tx: &mut RawTransaction) -> bool {
        let mut matched_any = false;

        for rule in &self.rules {
            let conditions: Vec<RuleCondition> = serde_json::from_str(&rule.match_condition).unwrap_or_default();
            let actions: Vec<RuleAction> = serde_json::from_str(&rule.action).unwrap_or_default();

            if self.evaluate_conditions(&conditions, tx) {
                matched_any = true;
                self.execute_actions(&actions, tx);
                break; // Short circuit on first matched rule
            }
        }
        
        matched_any
    }

    fn evaluate_conditions(&self, conditions: &[RuleCondition], tx: &RawTransaction) -> bool {
        if conditions.is_empty() {
            return false;
        }

        for cond in conditions {
            let matches = match cond.match_type.as_str() {
                "merchant_keyword" => {
                    tx.merchant.as_ref().map_or(false, |m| m.contains(&cond.value))
                }
                "notes_keyword" => {
                    tx.notes.as_ref().map_or(false, |n| n.contains(&cond.value))
                }
                "regex" => {
                    let target = format!("{} {}", tx.merchant.as_deref().unwrap_or(""), tx.notes.as_deref().unwrap_or(""));
                    if let Ok(re) = Regex::new(&cond.value) {
                        re.is_match(&target)
                    } else {
                        false
                    }
                }
                _ => false,
            };

            if !matches {
                return false; // All conditions must match (AND logic by default)
            }
        }
        true
    }

    fn execute_actions(&self, actions: &[RuleAction], tx: &mut RawTransaction) {
        for action in actions {
            match action.action_type.as_str() {
                "set_category" => {
                    // In a real flow, this maps to a category ID. We temporarily store it in category_hint for the preview.
                    tx.category_hint = Some(action.value.clone());
                }
                "exclude" => {
                    tx.is_excluded = action.value == "true";
                }
                _ => {}
            }
        }
    }
}
