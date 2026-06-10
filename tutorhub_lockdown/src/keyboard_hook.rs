use crate::error::LockdownError;
use std::sync::atomic::{AtomicPtr, Ordering};
use std::thread;
use windows::Win32::Foundation::{HMODULE, HWND, LPARAM, LRESULT, WPARAM};
#[allow(unused_imports)]
use windows::Win32::UI::Input::KeyboardAndMouse::{
    GetAsyncKeyState, VK_CONTROL, VK_ESCAPE, VK_F12, VK_F4, VK_LWIN, VK_MENU, VK_P, VK_RWIN,
    VK_SHIFT, VK_SNAPSHOT, VK_TAB,
};
use windows::Win32::UI::WindowsAndMessaging::{
    CallNextHookEx, DispatchMessageW, GetMessageW, SetWindowsHookExW, TranslateMessage,
    UnhookWindowsHookEx, HHOOK, KBDLLHOOKSTRUCT, MSG, WH_KEYBOARD_LL, WM_KEYDOWN, WM_SYSKEYDOWN,
};

static HOOK_HANDLE: AtomicPtr<std::ffi::c_void> = AtomicPtr::new(std::ptr::null_mut());

/// Performs full panic cleanup: uninstall hook, disable screen protection,
/// switch to Default desktop, then exit with code 99.
#[cfg(feature = "debug_mode")]
fn perform_panic_cleanup(label: &str) -> ! {
    log::info!("Dev panic key detected: {}", label);
    log::info!("Before panic cleanup");

    crate::keyboard_hook::uninstall();

    let pid = crate::screen_protection::get_java_pid();
    if pid > 0 {
        let _ = crate::screen_protection::disable(pid);
    }

    unsafe {
        let desk = windows::Win32::System::StationsAndDesktops::OpenDesktopW(
            windows::core::w!("Default"),
            windows::Win32::System::StationsAndDesktops::DESKTOP_CONTROL_FLAGS(0),
            false,
            0x01FF,
        )
        .unwrap_or_default();
        if !desk.is_invalid() {
            let _ = windows::Win32::System::StationsAndDesktops::SwitchDesktop(desk);
            let _ = windows::Win32::System::StationsAndDesktops::CloseDesktop(desk);
        }
    }

    log::info!("After switch_to_default");
    log::info!("After panic cleanup");
    std::process::exit(99);
}

pub fn install() -> Result<(), LockdownError> {
    let (tx, rx) = std::sync::mpsc::channel();

    thread::spawn(move || {
        log::info!("Keyboard hook thread started (thread id: {:?})", thread::current().id());
        let hook_result =
            unsafe { SetWindowsHookExW(WH_KEYBOARD_LL, Some(hook_proc), HMODULE::default(), 0) };

        match hook_result {
            Ok(hook_handle) => {
                log::info!("WH_KEYBOARD_LL hook installed successfully, handle={:?}", hook_handle.0);
                // Store handle
                HOOK_HANDLE.store(hook_handle.0 as *mut _, Ordering::SeqCst);
                let _ = tx.send(Ok(()));
            }
            Err(e) => {
                log::error!("WH_KEYBOARD_LL hook install FAILED: {}", e);
                let _ = tx.send(Err(LockdownError::HookInstallFailed(format!(
                    "SetWindowsHookExW failed: {}",
                    e
                ))));
                return;
            }
        }

        // Message loop required for LL hooks
        log::info!("Keyboard hook message loop started");
        let mut msg = MSG::default();
        unsafe {
            while GetMessageW(&mut msg, HWND::default(), 0, 0).0 > 0 {
                let _ = TranslateMessage(&msg);
                let _ = DispatchMessageW(&msg);
            }
        }
        log::info!("Keyboard hook message loop stopped");
    });

    match rx.recv() {
        Ok(res) => res,
        Err(_) => Err(LockdownError::HookInstallFailed(
            "Hook thread died before responding".to_string(),
        )),
    }
}

pub fn uninstall() {
    let ptr = HOOK_HANDLE.swap(std::ptr::null_mut(), Ordering::SeqCst);
    if !ptr.is_null() {
        let hhook = HHOOK(ptr as _);
        unsafe {
            let _ = UnhookWindowsHookEx(hhook);
        }
    }
}

unsafe extern "system" fn hook_proc(ncode: i32, wparam: WPARAM, lparam: LPARAM) -> LRESULT {
    if ncode >= 0 {
        let kbd_struct = &*(lparam.0 as *const KBDLLHOOKSTRUCT);
        let vk_code = kbd_struct.vkCode as u16;
        let msg = wparam.0 as u32;

        if msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN {
            let alt_down = (GetAsyncKeyState(VK_MENU.0 as i32) as i16) < 0;
            #[allow(unused_variables)]
            let ctrl_down = (GetAsyncKeyState(VK_CONTROL.0 as i32) as i16) < 0;
            #[allow(unused_variables)]
            let shift_down = (GetAsyncKeyState(VK_SHIFT.0 as i32) as i16) < 0;

            // Debug log: only log F12 key events to diagnose VMware key passthrough
            #[cfg(feature = "debug_mode")]
            if vk_code == VK_F12.0 {
                log::info!(
                    "F12 key event detected ctrl_down={} shift_down={} alt_down={}",
                    ctrl_down, shift_down, alt_down
                );
            }

            // Panic key 1: Ctrl+Shift+Alt+F12 (Primary)
            #[cfg(feature = "debug_mode")]
            if vk_code == VK_F12.0 && ctrl_down && shift_down && alt_down {
                perform_panic_cleanup("PRIMARY");
            }

            // Panic key 2: Ctrl+Shift+F12 (Fallback for VM — no Alt needed)
            #[cfg(feature = "debug_mode")]
            if vk_code == VK_F12.0 && ctrl_down && shift_down && !alt_down {
                perform_panic_cleanup("VM_TEST_FALLBACK");
            }

            // Panic key 3: Ctrl+Shift+P (Easy fallback for VM where F12 is eaten)
            #[cfg(feature = "debug_mode")]
            if vk_code == VK_P.0 && ctrl_down && shift_down && !alt_down {
                log::info!(
                    "Ctrl+Shift+P key event detected ctrl_down={} shift_down={} alt_down={}",
                    ctrl_down, shift_down, alt_down
                );
                perform_panic_cleanup("VM_TEST_FALLBACK_CTRL_SHIFT_P");
            }

            // Block Windows key
            if vk_code == VK_LWIN.0 || vk_code == VK_RWIN.0 {
                return LRESULT(1);
            }

            // Block Alt+Tab and Alt+Esc
            if alt_down && (vk_code == VK_TAB.0 || vk_code == VK_ESCAPE.0) {
                return LRESULT(1);
            }

            // Block Alt+F4
            if alt_down && vk_code == VK_F4.0 {
                return LRESULT(1);
            }

            // Block PrintScreen
            if vk_code == VK_SNAPSHOT.0 {
                return LRESULT(1);
            }
        }
    }

    let ptr = HOOK_HANDLE.load(Ordering::SeqCst);
    let hhook = if ptr.is_null() {
        HHOOK::default()
    } else {
        HHOOK(ptr as _)
    };

    CallNextHookEx(hhook, ncode, wparam, lparam)
}
