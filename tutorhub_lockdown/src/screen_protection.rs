use crate::error::LockdownError;
use windows::Win32::Foundation::{BOOL, HWND, LPARAM};
use windows::Win32::UI::WindowsAndMessaging::{
    EnumWindows, GetWindowThreadProcessId, SetWindowDisplayAffinity, WINDOW_DISPLAY_AFFINITY,
};
use std::sync::atomic::{AtomicU32, Ordering};

static JAVA_PID: AtomicU32 = AtomicU32::new(0);

// WDA_EXCLUDEFROMCAPTURE (17) is available in Windows 10 2004 and later.
// It hides the window from capture software (like OBS) entirely, rendering it invisible in recordings.
// WDA_MONITOR (1) is the fallback for older versions, which blacks out the window.
const WDA_EXCLUDEFROMCAPTURE: WINDOW_DISPLAY_AFFINITY = WINDOW_DISPLAY_AFFINITY(17);
const WDA_NONE: WINDOW_DISPLAY_AFFINITY = WINDOW_DISPLAY_AFFINITY(0);

struct CallbackData {
    java_pid: u32,
    enable: bool,
    error_count: u32,
}

pub fn apply_to_all_java_windows(java_pid: u32, enable: bool) -> Result<(), LockdownError> {
    let mut data = CallbackData {
        java_pid,
        enable,
        error_count: 0,
    };

    let lparam = LPARAM(&mut data as *mut _ as isize);

    let result = unsafe { EnumWindows(Some(enum_windows_callback), lparam) };
    if let Err(e) = result {
        return Err(LockdownError::Win32Error(e));
    }

    if data.error_count > 0 {
        // We log or return error if some windows failed.
        // For now, we return an error indicating failure for at least one window.
        // Using Win32Error with a generic error code or custom message
        return Err(LockdownError::Win32Error(
            windows::core::Error::from_hresult(windows::core::HRESULT(-1)),
        ));
    }

    Ok(())
}

pub fn enable(java_pid: u32) -> Result<(), LockdownError> {
    JAVA_PID.store(java_pid, Ordering::SeqCst);
    apply_to_all_java_windows(java_pid, true)
}

pub fn get_java_pid() -> u32 {
    JAVA_PID.load(Ordering::SeqCst)
}

pub fn disable(java_pid: u32) -> Result<(), LockdownError> {
    apply_to_all_java_windows(java_pid, false)
}

unsafe extern "system" fn enum_windows_callback(hwnd: HWND, lparam: LPARAM) -> BOOL {
    let data_ptr = lparam.0 as *mut CallbackData;
    if data_ptr.is_null() {
        return BOOL::from(true);
    }

    let data = &mut *data_ptr;
    let mut process_id = 0u32;
    GetWindowThreadProcessId(hwnd, Some(&mut process_id));

    if process_id == data.java_pid {
        let affinity = if data.enable {
            WDA_EXCLUDEFROMCAPTURE
        } else {
            WDA_NONE
        };

        // Note: SetWindowDisplayAffinity may fail on windows that are not top-level,
        // or if the window is already set to WDA_MONITOR and we try to upgrade to EXCLUDEFROMCAPTURE.
        // In a real robust implementation, we might want to check the window style.
        let result = SetWindowDisplayAffinity(hwnd, affinity);
        if result.is_err() {
            // As a fallback, we could try WDA_MONITOR here if WDA_EXCLUDEFROMCAPTURE fails
            let fallback_result = SetWindowDisplayAffinity(hwnd, WINDOW_DISPLAY_AFFINITY(1));
            if fallback_result.is_err() {
                data.error_count += 1;
            }
        }
    }

    BOOL::from(true) // Continue enumeration
}
