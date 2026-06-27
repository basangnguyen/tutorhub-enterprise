use crate::ipc_server::DesktopDemoResult;
use std::time::Instant;

#[cfg(windows)]
use windows_sys::Win32::System::StationsAndDesktops::{
    CreateDesktopW, SwitchDesktop, CloseDesktop, GetThreadDesktop,
};

#[cfg(windows)]
use windows_sys::Win32::System::Threading::GetCurrentThreadId;

pub fn run_demo() -> DesktopDemoResult {
    let start_time = Instant::now();

    #[cfg(windows)]
    unsafe {
        let current_thread_id = GetCurrentThreadId();
        let original_desktop = GetThreadDesktop(current_thread_id);

        if original_desktop == 0 {
            return DesktopDemoResult {
                success: false,
                message: "NOT_SAFE_TO_SWITCH: Could not get original desktop.".to_string(),
                auto_returned: false,
                duration_ms: start_time.elapsed().as_millis() as u64,
            };
        }

        eprintln!("[WARNING] Preparing to switch to Safe Demo Desktop in VM.");
        let desktop_name: Vec<u16> = "TSE_SAFE_DEMO_DESKTOP\0".encode_utf16().collect();
        let desktop_all_access = 0x000F01FF;
        
        let new_desktop = CreateDesktopW(
            desktop_name.as_ptr(),
            std::ptr::null(),
            std::ptr::null_mut(),
            0,
            desktop_all_access,
            std::ptr::null(),
        );

        if new_desktop == 0 {
            return DesktopDemoResult {
                success: false,
                message: "Failed to create demo desktop.".to_string(),
                auto_returned: false,
                duration_ms: start_time.elapsed().as_millis() as u64,
            };
        }

        eprintln!("[WARNING] Switching desktop now. Auto-return in 5 seconds...");
        
        let switch_result = SwitchDesktop(new_desktop);
        if switch_result == 0 {
            CloseDesktop(new_desktop);
            return DesktopDemoResult {
                success: false,
                message: "NOT_SAFE_TO_SWITCH: SwitchDesktop failed.".to_string(),
                auto_returned: false,
                duration_ms: start_time.elapsed().as_millis() as u64,
            };
        }

        // Sleep 5 seconds safely
        std::thread::sleep(std::time::Duration::from_secs(5));

        // Switch back
        let return_switch = SwitchDesktop(original_desktop);
        CloseDesktop(new_desktop);

        if return_switch == 0 {
            return DesktopDemoResult {
                success: false,
                message: "FATAL: Failed to return to original desktop!".to_string(),
                auto_returned: false,
                duration_ms: start_time.elapsed().as_millis() as u64,
            };
        }

        DesktopDemoResult {
            success: true,
            message: "Demo completed and returned successfully.".to_string(),
            auto_returned: true,
            duration_ms: start_time.elapsed().as_millis() as u64,
        }
    }

    #[cfg(not(windows))]
    {
        DesktopDemoResult {
            success: false,
            message: "NOT_SAFE_TO_SWITCH: OS is not Windows.".to_string(),
            auto_returned: false,
            duration_ms: start_time.elapsed().as_millis() as u64,
        }
    }
}
