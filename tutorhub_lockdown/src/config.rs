use crate::error::LockdownError;
use base64::{engine::general_purpose::STANDARD, Engine as _};
use serde::{Deserialize, Serialize};

#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct ExamConfig {
    pub session_id: String,
    pub exam_id: i64,
    pub exam_key: String,
    pub java_pid: u32,
    pub enable_vm_detection: bool,
    pub enable_keyboard_hook: bool,
    pub enable_screen_protection: bool,
    pub enable_secure_desktop: Option<bool>,
    pub banned_process_names: Vec<String>,
    pub banned_process_hashes: Vec<String>,
    pub process_scan_interval_secs: u64,
    pub heartbeat_timeout_secs: u64,
}

impl ExamConfig {
    pub fn from_base64_json(b64: &str) -> Result<Self, LockdownError> {
        let bytes = STANDARD
            .decode(b64)
            .map_err(|e| LockdownError::ConfigParseFailed(format!("Base64 decode error: {}", e)))?;

        let json_str = String::from_utf8(bytes)
            .map_err(|e| LockdownError::ConfigParseFailed(format!("UTF-8 error: {}", e)))?;

        let config: ExamConfig = serde_json::from_str(&json_str)
            .map_err(|e| LockdownError::ConfigParseFailed(format!("JSON parse error: {}", e)))?;

        Ok(config)
    }

    pub fn validate(&self) -> Result<(), LockdownError> {
        if self.session_id.is_empty() {
            return Err(LockdownError::ConfigParseFailed(
                "session_id is empty".to_string(),
            ));
        }
        if self.heartbeat_timeout_secs < 5 {
            return Err(LockdownError::ConfigParseFailed(
                "heartbeat_timeout_secs must be >= 5".to_string(),
            ));
        }
        if self.process_scan_interval_secs < 1 {
            return Err(LockdownError::ConfigParseFailed(
                "process_scan_interval_secs must be >= 1".to_string(),
            ));
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_valid_config() {
        let config = ExamConfig {
            session_id: "test1234".to_string(),
            exam_id: 1,
            exam_key: "abc".to_string(),
            java_pid: 1234,
            enable_vm_detection: true,
            enable_keyboard_hook: true,
            enable_screen_protection: true,
            banned_process_names: vec![],
            banned_process_hashes: vec![],
            process_scan_interval_secs: 3,
            heartbeat_timeout_secs: 10,
        };
        let json = serde_json::to_string(&config).unwrap();
        let b64 = STANDARD.encode(json);

        let parsed = ExamConfig::from_base64_json(&b64).unwrap();
        assert_eq!(parsed.session_id, "test1234");
        assert!(parsed.validate().is_ok());
    }

    #[test]
    fn test_validate_invalid_config() {
        let mut config = ExamConfig {
            session_id: "test1234".to_string(),
            exam_id: 1,
            exam_key: "abc".to_string(),
            java_pid: 1234,
            enable_vm_detection: true,
            enable_keyboard_hook: true,
            enable_screen_protection: true,
            banned_process_names: vec![],
            banned_process_hashes: vec![],
            process_scan_interval_secs: 3,
            heartbeat_timeout_secs: 4, // invalid
        };
        assert!(config.validate().is_err());

        config.heartbeat_timeout_secs = 10;
        config.session_id = "".to_string(); // invalid
        assert!(config.validate().is_err());
    }
}
