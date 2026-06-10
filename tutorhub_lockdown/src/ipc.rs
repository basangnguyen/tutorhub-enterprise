use crate::error::LockdownError;
use serde::{Deserialize, Serialize};
use windows::core::{HRESULT, HSTRING, PCWSTR};
use windows::Win32::Foundation::{CloseHandle, ERROR_PIPE_CONNECTED, HANDLE, INVALID_HANDLE_VALUE};
use windows::Win32::Storage::FileSystem::{ReadFile, WriteFile, FILE_FLAGS_AND_ATTRIBUTES};
use windows::Win32::System::Pipes::{
    ConnectNamedPipe, CreateNamedPipeW, DisconnectNamedPipe, NAMED_PIPE_MODE,
};

#[derive(Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum IpcCommand {
    Lock,
    Unlock,
    Ping,
    Status,
    Unknown(String),
}

#[derive(Debug, PartialEq, Eq, Serialize, Deserialize)]
pub enum IpcResponse {
    Locked,
    Unlocked,
    LockFailed(String),
    Pong,
    StatusResponse(String),
    ProcessAlert(String),
}

impl IpcCommand {
    pub fn parse(line: &str) -> Self {
        let line = line.trim();
        match line {
            "LOCK" => IpcCommand::Lock,
            "UNLOCK" => IpcCommand::Unlock,
            "PING" => IpcCommand::Ping,
            "STATUS" => IpcCommand::Status,
            _ => IpcCommand::Unknown(line.to_string()),
        }
    }
}

impl IpcResponse {
    pub fn serialize(&self) -> String {
        match self {
            IpcResponse::Locked => "LOCKED\n".to_string(),
            IpcResponse::Unlocked => "UNLOCKED\n".to_string(),
            IpcResponse::LockFailed(msg) => format!("LOCK_FAILED:{}\n", msg),
            IpcResponse::Pong => "PONG\n".to_string(),
            IpcResponse::StatusResponse(msg) => format!("STATUS:{}\n", msg),
            IpcResponse::ProcessAlert(msg) => format!("PROCESS_ALERT:{}\n", msg),
        }
    }
}

pub struct IpcServer {
    pipe_handle: HANDLE,
}

impl IpcServer {
    pub fn create_pipe(session_id: &str) -> Result<Self, LockdownError> {
        let pipe_name = format!(r#"\\.\pipe\TutorHubExam_{}"#, session_id);
        let pipe_name_hstring = HSTRING::from(pipe_name.clone());

        log::info!("CreateNamedPipeW params: name={}, access=DUPLEX, mode=BYTE", pipe_name);
        let pipe_handle = unsafe {
            CreateNamedPipeW(
                PCWSTR(pipe_name_hstring.as_ptr()),
                FILE_FLAGS_AND_ATTRIBUTES(3), // PIPE_ACCESS_DUPLEX
                NAMED_PIPE_MODE(0), // PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT
                255,                // PIPE_UNLIMITED_INSTANCES
                4096,
                4096,
                0,
                None,
            )
        };

        if pipe_handle == INVALID_HANDLE_VALUE {
            let err = std::io::Error::last_os_error();
            log::error!("CreateNamedPipeW failed: {}", err);
            return Err(LockdownError::PipeCreateFailed(
                "INVALID_HANDLE_VALUE returned".to_string(),
            ));
        }

        Ok(IpcServer { pipe_handle })
    }

    pub fn accept_connection(&self) -> Result<(), LockdownError> {
        let result = unsafe { ConnectNamedPipe(self.pipe_handle, None) };
        if let Err(err) = result {
            if err.code() != HRESULT::from_win32(ERROR_PIPE_CONNECTED.0) {
                log::error!("ConnectNamedPipe failed: {:?}", err);
                return Err(LockdownError::PipeConnectFailed(format!(
                    "Error code: {:?}",
                    err
                )));
            }
        }
        log::info!("ConnectNamedPipe success.");
        Ok(())
    }

    pub fn read_command(&self) -> Result<IpcCommand, LockdownError> {
        let mut buffer = [0u8; 4096];
        let mut bytes_read = 0;

        log::info!("Before ReadFile command...");
        let success = unsafe {
            ReadFile(
                self.pipe_handle,
                Some(&mut buffer),
                Some(&mut bytes_read),
                None,
            )
        };

        if success.is_err() || bytes_read == 0 {
            let err = std::io::Error::last_os_error();
            log::error!("ReadFile timeout/no data. err: {}, bytes_read: {}", err, bytes_read);
            return Err(LockdownError::IoError(err));
        }

        log::info!("After ReadFile bytes_read={}", bytes_read);

        let s = String::from_utf8_lossy(&buffer[..bytes_read as usize]);
        log::info!("Raw command bytes/string: {:?}", s.to_string());
        Ok(IpcCommand::parse(&s))
    }

    pub fn send_response(&self, resp: &IpcResponse) -> Result<(), LockdownError> {
        let mut bytes_written = 0;
        let data = resp.serialize();

        let success = unsafe {
            WriteFile(
                self.pipe_handle,
                Some(data.as_bytes()),
                Some(&mut bytes_written),
                None,
            )
        };

        if success.is_err() || bytes_written == 0 {
            return Err(LockdownError::IoError(std::io::Error::last_os_error()));
        }

        Ok(())
    }

    pub fn close(&mut self) {
        if self.pipe_handle != INVALID_HANDLE_VALUE {
            unsafe {
                let _ = DisconnectNamedPipe(self.pipe_handle);
                let _ = CloseHandle(self.pipe_handle);
            }
            self.pipe_handle = INVALID_HANDLE_VALUE;
        }
    }
}

impl Drop for IpcServer {
    fn drop(&mut self) {
        self.close();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_command() {
        assert_eq!(IpcCommand::parse("LOCK\n"), IpcCommand::Lock);
        assert_eq!(IpcCommand::parse("UNLOCK\r\n"), IpcCommand::Unlock);
        assert_eq!(IpcCommand::parse("PING "), IpcCommand::Ping);
        assert_eq!(IpcCommand::parse("STATUS"), IpcCommand::Status);
        assert_eq!(
            IpcCommand::parse("UNKNOWN_STUFF"),
            IpcCommand::Unknown("UNKNOWN_STUFF".to_string())
        );
    }

    #[test]
    fn test_serialize_response() {
        assert_eq!(IpcResponse::Locked.serialize(), "LOCKED\n");
        assert_eq!(IpcResponse::Unlocked.serialize(), "UNLOCKED\n");
        assert_eq!(
            IpcResponse::LockFailed("error".to_string()).serialize(),
            "LOCK_FAILED:error\n"
        );
        assert_eq!(IpcResponse::Pong.serialize(), "PONG\n");
        assert_eq!(
            IpcResponse::StatusResponse("ok".to_string()).serialize(),
            "STATUS:ok\n"
        );
        assert_eq!(
            IpcResponse::ProcessAlert("obs.exe".to_string()).serialize(),
            "PROCESS_ALERT:obs.exe\n"
        );
    }
}
