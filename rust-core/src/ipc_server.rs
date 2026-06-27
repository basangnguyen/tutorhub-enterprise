use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct ProbeResult {
    pub success: bool,
    pub ready: bool,
    pub os_windows: bool,
    pub vm_likely: bool,
    pub input_desktop_detected: bool,
    pub can_open_input_desktop: bool,
    pub can_create_desktop: bool,
    pub error_code: Option<String>,
    pub warnings: Vec<String>,
    pub checked_at: String,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "camelCase")]
pub struct DesktopDemoResult {
    pub success: bool,
    pub message: String,
    pub auto_returned: bool,
    pub duration_ms: u64,
}

pub fn format_json<T: Serialize>(data: &T) -> String {
    serde_json::to_string(data).unwrap_or_else(|_| "{\"success\":false,\"errorCode\":\"JSON_ERROR\"}".to_string())
}
