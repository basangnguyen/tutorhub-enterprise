use crate::error::LockdownError;
use windows::core::{HSTRING, PCWSTR};
use windows::Win32::System::StationsAndDesktops::{
    CloseDesktop, CreateDesktopW, GetThreadDesktop, SetThreadDesktop, SwitchDesktop,
    DESKTOP_CONTROL_FLAGS, HDESK,
};
use windows::Win32::System::Threading::{
    GetCurrentThreadId, CreateProcessW, STARTUPINFOW, PROCESS_INFORMATION, PROCESS_CREATION_FLAGS
};

pub struct ExamDesktop {
    handle: HDESK,
    original_desktop: HDESK,
    name: String,
}

unsafe impl Send for ExamDesktop {}
unsafe impl Sync for ExamDesktop {}

impl ExamDesktop {
    pub fn create(name: &str) -> Result<Self, LockdownError> {
        // Save original desktop
        let original_desktop = unsafe { GetThreadDesktop(GetCurrentThreadId()) }.map_err(|e| {
            LockdownError::DesktopCreateFailed(format!("Failed to get original desktop: {}", e))
        })?;

        let name_hstring = HSTRING::from(name);

        // Define all access permissions needed for the new desktop
        // Using explicit bits to avoid windows-rs enum incompatibilities
        let desired_access = 0x01FFu32; // GENERIC_ALL for desktop is usually represented, or 0x01FF

        let handle_result = unsafe {
            CreateDesktopW(
                PCWSTR(name_hstring.as_ptr()),
                PCWSTR::null(),
                None,
                DESKTOP_CONTROL_FLAGS(0),
                desired_access,
                None,
            )
        };

        match handle_result {
            Ok(handle) => Ok(ExamDesktop {
                handle,
                original_desktop,
                name: name.to_string(),
            }),
            Err(e) => Err(LockdownError::DesktopCreateFailed(format!(
                "CreateDesktopW failed: {}",
                e
            ))),
        }
    }

    pub fn switch_to(&self) -> Result<(), LockdownError> {
        let switch_result = unsafe { SwitchDesktop(self.handle) };
        if let Err(e) = switch_result {
            return Err(LockdownError::DesktopSwitchFailed(format!(
                "SwitchDesktop failed: {}",
                e
            )));
        }

        let thread_result = unsafe { SetThreadDesktop(self.handle) };
        if let Err(e) = thread_result {
            return Err(LockdownError::DesktopSwitchFailed(format!(
                "SetThreadDesktop failed: {}",
                e
            )));
        }

        Ok(())
    }

    pub fn switch_to_default(&self) -> Result<(), LockdownError> {
        if !self.original_desktop.is_invalid() {
            let switch_result = unsafe { SwitchDesktop(self.original_desktop) };
            if let Err(e) = switch_result {
                return Err(LockdownError::DesktopSwitchFailed(format!(
                    "SwitchDesktop back failed: {}",
                    e
                )));
            }

            let thread_result = unsafe { SetThreadDesktop(self.original_desktop) };
            if let Err(e) = thread_result {
                return Err(LockdownError::DesktopSwitchFailed(format!(
                    "SetThreadDesktop back failed: {}",
                    e
                )));
            }
        }
        Ok(())
    }

    pub fn handle_as_u64(&self) -> u64 {
        self.handle.0 as u64
    }

    pub fn get_raw_handle(&self) -> HDESK {
        self.handle
    }

    pub fn spawn_process(&self, exe_path: &str) -> Result<u32, LockdownError> {
        let mut si = STARTUPINFOW::default();
        si.cb = std::mem::size_of::<STARTUPINFOW>() as u32;
        
        let desktop_hstring = HSTRING::from(self.name.clone());
        si.lpDesktop = windows::core::PWSTR(desktop_hstring.as_ptr() as *mut u16);
        
        let mut pi = PROCESS_INFORMATION::default();
        
        let exe_hstring = HSTRING::from(exe_path);
        let mut cmd_line: Vec<u16> = exe_hstring.as_wide().to_vec();
        cmd_line.push(0);

        let success = unsafe {
            CreateProcessW(
                PCWSTR::null(),
                windows::core::PWSTR(cmd_line.as_mut_ptr()),
                None,
                None,
                false,
                PROCESS_CREATION_FLAGS(0),
                None,
                PCWSTR::null(),
                &si,
                &mut pi,
            )
        };

        if success.is_ok() {
            unsafe {
                let _ = windows::Win32::Foundation::CloseHandle(pi.hProcess);
                let _ = windows::Win32::Foundation::CloseHandle(pi.hThread);
            }
            Ok(pi.dwProcessId)
        } else {
            let err = unsafe { windows::Win32::Foundation::GetLastError() };
            Err(LockdownError::ProcessSpawnFailed(format!("Failed to spawn process: {:?}", err)))
        }
    }
}

impl Drop for ExamDesktop {
    fn drop(&mut self) {
        let _ = self.switch_to_default();
        if !self.handle.is_invalid() {
            unsafe {
                let _ = CloseDesktop(self.handle);
            }
        }
    }
}
