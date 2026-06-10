use std::fmt;

#[derive(Debug)]
pub enum LockdownError {
    ConfigParseFailed(String),
    PipeCreateFailed(String),
    PipeConnectFailed(String),
    DesktopCreateFailed(String),
    DesktopSwitchFailed(String),
    HookInstallFailed(String),
    ProcessSpawnFailed(String),
    VmDetected(String),
    HeartbeatTimeout,
    UnexpectedCommand,
    IoError(std::io::Error),
    Win32Error(windows::core::Error),
}

impl fmt::Display for LockdownError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            LockdownError::ConfigParseFailed(msg) => write!(f, "Config parse failed: {}", msg),
            LockdownError::PipeCreateFailed(msg) => write!(f, "Pipe create failed: {}", msg),
            LockdownError::PipeConnectFailed(msg) => write!(f, "Pipe connect failed: {}", msg),
            LockdownError::DesktopCreateFailed(msg) => write!(f, "Desktop create failed: {}", msg),
            LockdownError::DesktopSwitchFailed(msg) => write!(f, "Desktop switch failed: {}", msg),
            LockdownError::HookInstallFailed(msg) => write!(f, "Hook install failed: {}", msg),
            LockdownError::ProcessSpawnFailed(msg) => write!(f, "Process spawn failed: {}", msg),
            LockdownError::VmDetected(msg) => write!(f, "VM detected: {}", msg),
            LockdownError::HeartbeatTimeout => write!(f, "Heartbeat timeout"),
            LockdownError::UnexpectedCommand => write!(f, "Unexpected command"),
            LockdownError::IoError(err) => write!(f, "IO error: {}", err),
            LockdownError::Win32Error(err) => write!(f, "Win32 error: {}", err),
        }
    }
}

impl std::error::Error for LockdownError {}

impl From<std::io::Error> for LockdownError {
    fn from(err: std::io::Error) -> Self {
        LockdownError::IoError(err)
    }
}

impl From<windows::core::Error> for LockdownError {
    fn from(err: windows::core::Error) -> Self {
        LockdownError::Win32Error(err)
    }
}
