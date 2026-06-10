use crate::error::LockdownError;
use sha2::{Digest, Sha256};
use std::collections::{HashMap, HashSet};
use std::fs::File;
use std::io::Read;
use std::sync::Mutex;
use windows::core::PWSTR;
use windows::Win32::Foundation::CloseHandle;
use windows::Win32::System::Diagnostics::ToolHelp::{
    CreateToolhelp32Snapshot, Process32FirstW, Process32NextW, PROCESSENTRY32W, TH32CS_SNAPPROCESS,
};
use windows::Win32::System::Threading::{
    OpenProcess, QueryFullProcessImageNameW, PROCESS_NAME_FORMAT, PROCESS_QUERY_LIMITED_INFORMATION,
};

pub struct ProcessScanner {
    banned_names: HashSet<String>,
    banned_hashes: HashSet<String>,
    hash_cache: Mutex<HashMap<String, String>>, // path -> hash
}

static SCANNER: Mutex<Option<ProcessScanner>> = Mutex::new(None);

pub fn init(banned_names: Vec<String>, banned_hashes: Vec<String>) -> Result<(), LockdownError> {
    let mut scanner = SCANNER
        .lock()
        .map_err(|_| LockdownError::UnexpectedCommand)?;

    *scanner = Some(ProcessScanner {
        banned_names: banned_names.into_iter().map(|s| s.to_lowercase()).collect(),
        banned_hashes: banned_hashes
            .into_iter()
            .map(|s| s.to_lowercase())
            .collect(),
        hash_cache: Mutex::new(HashMap::new()),
    });
    Ok(())
}

pub fn scan_once() -> Result<Vec<String>, LockdownError> {
    let scanner_guard = SCANNER
        .lock()
        .map_err(|_| LockdownError::UnexpectedCommand)?;

    let scanner = match &*scanner_guard {
        Some(s) => s,
        None => return Ok(Vec::new()),
    };

    let mut alerts = Vec::new();

    let snapshot = unsafe { CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0) };
    let snapshot = snapshot.map_err(|e| LockdownError::Win32Error(e))?;

    let mut entry = PROCESSENTRY32W::default();
    entry.dwSize = std::mem::size_of::<PROCESSENTRY32W>() as u32;

    let first = unsafe { Process32FirstW(snapshot, &mut entry) };
    if first.is_ok() {
        loop {
            let exe_name = String::from_utf16_lossy(
                &entry.szExeFile[..entry.szExeFile.iter().position(|&c| c == 0).unwrap_or(0)],
            )
            .to_lowercase();

            if scanner.banned_names.contains(&exe_name) {
                alerts.push(exe_name.clone());
            } else {
                // Try getting path and hash. We silently ignore errors because we might not have permissions.
                if let Ok(path) = get_process_path(entry.th32ProcessID) {
                    if let Ok(hash) = get_file_hash(&path, &scanner.hash_cache) {
                        if scanner.banned_hashes.contains(&hash) {
                            alerts.push(exe_name.clone());
                        }
                    }
                }
            }

            let next = unsafe { Process32NextW(snapshot, &mut entry) };
            if next.is_err() {
                break;
            }
        }
    }

    unsafe {
        let _ = CloseHandle(snapshot);
    }

    Ok(alerts)
}

fn get_process_path(pid: u32) -> Result<String, LockdownError> {
    let handle = unsafe { OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, false, pid) };
    let handle = handle.map_err(|e| LockdownError::Win32Error(e))?;

    let mut buffer = [0u16; 1024];
    let mut size = buffer.len() as u32;

    let result = unsafe {
        QueryFullProcessImageNameW(
            handle,
            PROCESS_NAME_FORMAT(0),
            PWSTR(buffer.as_mut_ptr()),
            &mut size,
        )
    };

    unsafe {
        let _ = CloseHandle(handle);
    }

    if result.is_ok() {
        Ok(String::from_utf16_lossy(&buffer[..size as usize]))
    } else {
        Err(LockdownError::UnexpectedCommand) // Or some other generic error
    }
}

fn get_file_hash(
    path: &str,
    cache: &Mutex<HashMap<String, String>>,
) -> Result<String, LockdownError> {
    {
        let cache_guard = cache.lock().map_err(|_| LockdownError::UnexpectedCommand)?;
        if let Some(hash) = cache_guard.get(path) {
            return Ok(hash.clone());
        }
    }

    let mut file = File::open(path)?;
    let mut hasher = Sha256::new();
    let mut buffer = [0u8; 8192];

    loop {
        let bytes_read = file.read(&mut buffer)?;
        if bytes_read == 0 {
            break;
        }
        hasher.update(&buffer[..bytes_read]);
    }

    let hash_result = format!("{:x}", hasher.finalize());

    {
        let mut cache_guard = cache.lock().map_err(|_| LockdownError::UnexpectedCommand)?;
        cache_guard.insert(path.to_string(), hash_result.clone());
    }

    Ok(hash_result)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;

    #[test]
    fn test_file_hash() {
        let path = "test_hash.txt";
        let mut file = std::fs::File::create(path).unwrap();
        file.write_all(b"tutorhub").unwrap();

        let cache = Mutex::new(HashMap::new());
        let hash = get_file_hash(path, &cache).unwrap();

        assert_eq!(
            hash,
            "2006d8a31981dc01e7b8211d4d9624d333e50e12d3e35b078ba16dd7a7b5e8eb"
        );

        std::fs::remove_file(path).unwrap();
    }

    #[test]
    fn test_init_and_scan_logic() {
        let names = vec!["bad_process.exe".to_string()];
        let hashes = vec!["dummy_hash".to_string()];

        init(names, hashes).unwrap();

        let scanner_guard = SCANNER.lock().unwrap();
        let scanner = scanner_guard.as_ref().unwrap();
        assert!(scanner.banned_names.contains("bad_process.exe"));
        assert!(scanner.banned_hashes.contains("dummy_hash"));
    }
}
