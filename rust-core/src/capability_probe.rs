use crate::ipc_server::ProbeResult;
use std::time::{SystemTime, UNIX_EPOCH};

#[cfg(windows)]
use windows_sys::Win32::System::StationsAndDesktops::{
    CreateDesktopW, OpenInputDesktop, CloseDesktop,
};

pub fn run_probe() -> ProbeResult {
    let mut warnings = vec![];
    let os_windows = cfg!(windows);
    
    // Simplistic VM heuristic (just a placeholder for PoC)
    // Real implementation would check CPUID, MAC address, etc. safely.
    let vm_likely = true; 
    warnings.push("vm_likely is mocked to true for safe PoC".to_string());

    let mut can_open_input_desktop = false;
    let mut can_create_desktop = false;

    #[cfg(windows)]
    unsafe {
        let desktop_all_access = 0x000F01FF; // DESKTOP_ALL_ACCESS
        
        let input_desk = OpenInputDesktop(0, 0, desktop_all_access);
        if input_desk != 0 {
            can_open_input_desktop = true;
            CloseDesktop(input_desk);
        }

        let desktop_name: Vec<u16> = "TSE_PROBE_DESKTOP\0".encode_utf16().collect();
        let test_desk = CreateDesktopW(
            desktop_name.as_ptr(),
            std::ptr::null(),
            std::ptr::null_mut(),
            0,
            desktop_all_access,
            std::ptr::null(),
        );

        if test_desk != 0 {
            can_create_desktop = true;
            CloseDesktop(test_desk);
        }
    }

    let checked_at = match SystemTime::now().duration_since(UNIX_EPOCH) {
        Ok(n) => n.as_millis().to_string(),
        Err(_) => "0".to_string(),
    };

    ProbeResult {
        success: true,
        ready: true,
        os_windows,
        vm_likely,
        input_desktop_detected: can_open_input_desktop,
        can_open_input_desktop,
        can_create_desktop,
        error_code: None,
        warnings,
        checked_at,
    }
}
