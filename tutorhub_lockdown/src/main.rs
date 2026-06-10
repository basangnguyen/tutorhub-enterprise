pub mod config;
pub mod desktop;
pub mod error;
pub mod ipc;
pub mod keyboard_hook;
pub mod logger;
pub mod process_scanner;
pub mod screen_protection;
pub mod vm_detection;
pub mod watchdog;

use config::ExamConfig;
use error::LockdownError;
use ipc::{IpcCommand, IpcResponse, IpcServer};
use std::env;
use std::sync::{mpsc, Arc};
use std::thread;
use std::time::Duration;
use watchdog::Watchdog;
use windows::Win32::System::StationsAndDesktops::{OpenDesktopW, SwitchDesktop, CloseDesktop, DESKTOP_CONTROL_FLAGS};
use windows::core::w;

fn handle_emergency_reset() -> Result<(), LockdownError> {
    log::info!("Emergency reset mode started");
    log::info!("Opening Default desktop");
    unsafe {
        let desk = OpenDesktopW(
            w!("Default"),
            DESKTOP_CONTROL_FLAGS(0),
            false,
            0x01FF,
        ).map_err(|e| LockdownError::DesktopSwitchFailed(format!("OpenDesktopW failed: {}", e)))?;
        
        log::info!("Switching to Default desktop");
        SwitchDesktop(desk).map_err(|e| LockdownError::DesktopSwitchFailed(format!("SwitchDesktop failed: {}", e)))?;
        let _ = CloseDesktop(desk);
    }
    log::info!("Emergency reset completed");
    Ok(())
}

fn handle_lab_test() -> Result<(), LockdownError> {
    log::info!("Starting Step 2I.5 Secure Desktop Lab Test");
    
    log::info!("CreateDesktop success starting...");
    let desk = desktop::ExamDesktop::create("TutorHubSecureDesktopLab")?;
    log::info!("CreateDesktop success");

    log::info!("SwitchDesktop starting...");
    desk.switch_to()?;
    log::info!("SwitchDesktop success");

    log::info!("CreateProcessW starting...");
    let exe_path = "C:\\Windows\\System32\\notepad.exe";
    match desk.spawn_process(exe_path) {
        Ok(pid) => {
            log::info!("CreateProcessW success");
            log::info!("Child PID: {}", pid);
            
            log::info!("Waiting 10 seconds on Secure Desktop...");
            thread::sleep(Duration::from_secs(10));
            
            log::info!("Cleanup child process starting...");
            let kill_status = std::process::Command::new("taskkill")
                .args(["/F", "/PID", &pid.to_string()])
                .output();
            match kill_status {
                Ok(output) if output.status.success() => {
                    log::info!("Cleanup child process success");
                }
                Ok(output) => {
                    log::error!("Failed to kill child process: {:?}", String::from_utf8_lossy(&output.stderr));
                }
                Err(e) => {
                    log::error!("Failed to execute taskkill: {:?}", e);
                }
            }
        }
        Err(e) => {
            log::error!("CreateProcessW failed: {:?}", e);
        }
    }

    log::info!("Switch back Default starting...");
    desk.switch_to_default()?;
    log::info!("Switch back Default success");

    drop(desk);
    log::info!("Desktop handle closed");
    
    Ok(())
}

fn handle_lab_jcef_test() -> Result<(), LockdownError> {
    log::info!("Starting Step 2I.6 Secure Desktop JCEF Lab Test");
    
    log::info!("CreateDesktop success starting...");
    let desk = desktop::ExamDesktop::create("TutorHubSecureDesktopLab")?;
    log::info!("CreateDesktop success");

    log::info!("SwitchDesktop starting...");
    desk.switch_to()?;
    log::info!("SwitchDesktop success");

    log::info!("CreateProcessW starting Java sandbox...");
    
    // Command line to launch the Java sandbox using the shaded jar
    // We assume the jar is built and present at target/TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar
    // We can also pass JCEF args if needed: --disable-gpu etc.
    let current_dir = std::env::current_dir().unwrap_or_default();
    // Since we are running from tutorhub_lockdown, the jar is in ../target/
    let exe_path = "java.exe";
    let args = "java.exe -cp \"..\\target\\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar\" com.mycompany.tutorhub_enterprise.client.exam.ui.TSEJCEFDesktopSandboxTest";

    match desk.spawn_process(args) {
        Ok(pid) => {
            log::info!("CreateProcessW success");
            log::info!("Child PID: {}", pid);
            
            log::info!("Waiting 20 seconds on Secure Desktop for JCEF render test...");
            thread::sleep(Duration::from_secs(20));
            
            log::info!("Cleanup child process starting...");
            let kill_status = std::process::Command::new("taskkill")
                .args(["/F", "/PID", &pid.to_string(), "/T"]) // Use /T to kill child processes (like JCEF renderers)
                .output();
            match kill_status {
                Ok(output) if output.status.success() => {
                    log::info!("Cleanup child process success");
                }
                Ok(output) => {
                    log::error!("Failed to kill child process: {:?}", String::from_utf8_lossy(&output.stderr));
                }
                Err(e) => {
                    log::error!("Failed to execute taskkill: {:?}", e);
                }
            }
        }
        Err(e) => {
            log::error!("CreateProcessW failed: {:?}", e);
        }
    }

    log::info!("Switch back Default starting...");
    desk.switch_to_default()?;
    log::info!("Switch back Default success");

    drop(desk);
    log::info!("Desktop handle closed");
    
    Ok(())
}

fn handle_lab_tse_child_test() -> Result<(), LockdownError> {
    log::info!("Starting Step 2I.7.2 Secure Desktop TSE Child Lab Test");
    
    log::info!("CreateDesktop success starting...");
    let desk = desktop::ExamDesktop::create("TutorHubSecureDesktopLab")?;
    log::info!("CreateDesktop success");

    log::info!("SwitchDesktop starting...");
    desk.switch_to()?;
    log::info!("SwitchDesktop success");

    log::info!("CreateProcessW starting Java TSEExamChildClient...");
    
    let args = "java.exe -cp \"..\\target\\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar\" com.mycompany.tutorhub_enterprise.client.exam.ui.TSEExamChildClient --context \"..\\src\\test\\resources\\exam_context_mock.json\" --output \"..\\target\\secure_desktop_submit_payload.json\"";

    match desk.spawn_process(args) {
        Ok(pid) => {
            log::info!("CreateProcessW success");
            log::info!("Child PID: {}", pid);
            
            // In a real scenario we use WaitForSingleObject or a loop. Here we just sleep in chunks.
            log::info!("Waiting for child process to exit (timeout 180s)...");
            
            unsafe {
                use windows::Win32::System::Threading::{OpenProcess, PROCESS_ALL_ACCESS, WaitForSingleObject};
                use windows::Win32::Foundation::{CloseHandle, WAIT_TIMEOUT, WAIT_OBJECT_0};
                
                let handle = OpenProcess(PROCESS_ALL_ACCESS, false, pid);
                match handle {
                    Ok(h) => {
                        let result = WaitForSingleObject(h, 180000); // Wait up to 3 mins
                        if result == WAIT_OBJECT_0 {
                            log::info!("Child process exited naturally.");
                        } else if result == WAIT_TIMEOUT {
                            log::error!("Child process timeout! Forcing kill...");
                            let _ = std::process::Command::new("taskkill").args(["/F", "/PID", &pid.to_string(), "/T"]).output();
                        }
                        let _ = CloseHandle(h);
                    }
                    Err(e) => {
                        log::error!("Failed to wait for child process: {:?}", e);
                        thread::sleep(Duration::from_secs(60));
                    }
                }
            }
        }
        Err(e) => {
            log::error!("CreateProcessW failed: {:?}", e);
        }
    }

    log::info!("Switch back Default starting...");
    desk.switch_to_default()?;
    log::info!("Switch back Default success");

    drop(desk);
    log::info!("Desktop handle closed");
    
    Ok(())
}

fn handle_spawn_child(java_exe: &str, jar_path: &str, context_path: &str, output_path: &str, key_b64: &str) -> Result<(), LockdownError> {
    log::info!("Starting Step 2I.7.5 Secure Desktop TSE Child Spawn");
    
    // Check file existence
    if !std::path::Path::new(java_exe).exists() {
        log::error!("java_exe does not exist: {}", java_exe);
        return Err(LockdownError::ConfigParseFailed("java_exe missing".to_string()));
    }
    if !std::path::Path::new(jar_path).exists() {
        log::error!("jar_path does not exist: {}", jar_path);
        return Err(LockdownError::ConfigParseFailed("jar_path missing".to_string()));
    }
    if !std::path::Path::new(context_path).exists() {
        log::error!("context_path does not exist: {}", context_path);
        return Err(LockdownError::ConfigParseFailed("context_path missing".to_string()));
    }
    
    let out_path = std::path::Path::new(output_path);
    if let Some(parent) = out_path.parent() {
        if !parent.exists() {
            log::error!("output_path parent directory does not exist: {}", parent.display());
            return Err(LockdownError::ConfigParseFailed("output_path parent missing".to_string()));
        }
    }

    log::info!("CreateDesktop success starting...");
    let desk = desktop::ExamDesktop::create("TutorHubSecureDesktopLab")?;
    log::info!("CreateDesktop success");

    log::info!("SwitchDesktop starting...");
    desk.switch_to()?;
    log::info!("SwitchDesktop success");

    log::info!("CreateProcessW starting Java TSEExamChildClient...");
    
    let mut args = format!("\"{}\" -cp \"{}\" com.mycompany.tutorhub_enterprise.client.exam.ui.TSEExamChildClient --context \"{}\" --output \"{}\"", java_exe, jar_path, context_path, output_path);
    if !key_b64.is_empty() {
        args.push_str(&format!(" --key \"{}\"", key_b64));
    }
    
    log::info!("Child command constructed (key length: {})", key_b64.len());

    let mut final_exit_code = 0;

    match desk.spawn_process(&args) {
        Ok(pid) => {
            log::info!("CreateProcessW success");
            log::info!("Child PID: {}", pid);
            
            log::info!("Waiting for child process to exit (timeout 180s)...");
            unsafe {
                use windows::Win32::System::Threading::{OpenProcess, PROCESS_ALL_ACCESS, WaitForSingleObject, GetExitCodeProcess};
                use windows::Win32::Foundation::{CloseHandle, WAIT_TIMEOUT, WAIT_OBJECT_0};
                
                let handle = OpenProcess(PROCESS_ALL_ACCESS, false, pid);
                match handle {
                    Ok(h) => {
                        log::info!("Waiting for child process to exit (INFINITE)...");
                        let result = WaitForSingleObject(h, windows::Win32::System::Threading::INFINITE); // Wait infinitely
                        if result == WAIT_OBJECT_0 {
                            let mut exit_code: u32 = 0;
                            if GetExitCodeProcess(h, &mut exit_code).is_ok() {
                                log::info!("Child process exited naturally with code {}.", exit_code);
                                final_exit_code = exit_code;
                            } else {
                                log::info!("Child process exited naturally but failed to get exit code.");
                            }
                        } else if result == WAIT_TIMEOUT {
                            log::error!("Child process timeout! Forcing kill...");
                            let _ = std::process::Command::new("taskkill").args(["/F", "/PID", &pid.to_string(), "/T"]).output();
                            final_exit_code = 1;
                        } else {
                            log::error!("Child process wait failed with result: {:?}", result);
                            final_exit_code = 1;
                        }
                        let _ = CloseHandle(h);
                    }
                    Err(e) => {
                        log::error!("Failed to wait for child process: {:?}", e);
                        thread::sleep(Duration::from_secs(60));
                        final_exit_code = 1;
                    }
                }
            }
        }
        Err(e) => {
            log::error!("CreateProcessW failed: {:?}", e);
            final_exit_code = 1;
        }
    }

    log::info!("[RUST_CLEANUP] Child exited.");
    log::info!("[RUST_CLEANUP] Switching back Default Desktop now.");
    desk.switch_to_default()?;
    log::info!("[RUST_CLEANUP] Default Desktop restored.");

    drop(desk);
    log::info!("Desktop handle closed");
    log::info!("[RUST_CLEANUP] Rust exiting code 0.");
    
    if final_exit_code != 0 {
        return Err(LockdownError::ConfigParseFailed(format!("Child exited with code {}", final_exit_code)));
    }
    
    Ok(())
}

fn main() -> Result<(), LockdownError> {
    let args: Vec<String> = env::args().collect();
    let mut session_id = String::new();
    let mut config_b64 = String::new();

    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "--session-id" => {
                if i + 1 < args.len() {
                    session_id = args[i + 1].clone();
                    i += 1;
                }
            }
            "--config" => {
                if i + 1 < args.len() {
                    config_b64 = args[i + 1].clone();
                    i += 1;
                }
            }
            _ => {}
        }
        i += 1;
    }

    if args.iter().any(|a| a == "--emergency-reset") {
        logger::init("emergency_reset")?;
        return handle_emergency_reset();
    }

    if args.iter().any(|a| a == "--lab-test") {
        logger::init("lab_test")?;
        return handle_lab_test();
    }

    if args.iter().any(|a| a == "--lab-jcef-test") {
        logger::init("lab_jcef_test")?;
        return handle_lab_jcef_test();
    }

    if args.iter().any(|a| a == "--lab-tse-child-test") {
        logger::init("lab_tse_child_test")?;
        return handle_lab_tse_child_test();
    }

    if let Some(_idx) = args.iter().position(|a| a == "--spawn-child") {
        logger::init("lab_spawn_child")?;
        let mut java_exe = String::new();
        let mut jar_path = String::new();
        let mut context_path = String::new();
        let mut output_path = String::new();
        let mut key_b64 = String::new();
        
        if let Some(c_idx) = args.iter().position(|a| a == "--java-exe") {
            if c_idx + 1 < args.len() { java_exe = args[c_idx + 1].clone(); }
        }
        if let Some(c_idx) = args.iter().position(|a| a == "--jar") {
            if c_idx + 1 < args.len() { jar_path = args[c_idx + 1].clone(); }
        }
        if let Some(c_idx) = args.iter().position(|a| a == "--context") {
            if c_idx + 1 < args.len() { context_path = args[c_idx + 1].clone(); }
        }
        if let Some(o_idx) = args.iter().position(|a| a == "--output") {
            if o_idx + 1 < args.len() { output_path = args[o_idx + 1].clone(); }
        }
        if let Some(k_idx) = args.iter().position(|a| a == "--key") {
            if k_idx + 1 < args.len() { key_b64 = args[k_idx + 1].clone(); }
        }
        
        if java_exe.is_empty() || jar_path.is_empty() || context_path.is_empty() || output_path.is_empty() {
            log::error!("Missing arguments for --spawn-child. Need --java-exe, --jar, --context, --output");
            return Err(LockdownError::ConfigParseFailed("Missing paths".to_string()));
        }
        
        return handle_spawn_child(&java_exe, &jar_path, &context_path, &output_path, &key_b64);
    }

    if session_id.is_empty() || config_b64.is_empty() {
        return Err(LockdownError::ConfigParseFailed("Missing args".to_string()));
    }

    logger::init(&session_id)?;
    log::info!("Starting TutorHub Lockdown Core");

    #[cfg(feature = "debug_mode")]
    {
        log::info!("Debug mode enabled. Spawning 120s auto-kill timer.");
        thread::spawn(|| {
            thread::sleep(Duration::from_secs(120));
            log::warn!("Auto-kill timer expired. Exiting.");
            std::process::exit(98);
        });
    }

    let config = ExamConfig::from_base64_json(&config_b64)?;
    config.validate()?;

    if config.enable_vm_detection {
        log::info!("Running VM detection...");
        if let Err(e) = vm_detection::check() {
            log::error!("VM detected: {:?}", e);
            return Err(e);
        }
    }

    log::info!("Starting IPC server on session {}...", session_id);
    let server = IpcServer::create_pipe(&session_id)?;
    log::info!("Waiting for connection...");
    server.accept_connection()?;

    log::info!("Client connected. Waiting for LOCK command...");

    loop {
        match server.read_command() {
            Ok(IpcCommand::Lock) => {
                log::info!("Received LOCK command");
                break;
            }
            Ok(cmd) => {
                log::warn!("Expected LOCK, got {:?}", cmd);
                let _ = server.send_response(&IpcResponse::LockFailed("Expected LOCK".to_string()));
            }
            Err(e) => {
                log::error!("Error reading IPC: {:?}", e);
                return Err(e);
            }
        }
    }

    log::info!("Initiating lockdown sequence...");

    let use_secure_desktop = config.enable_secure_desktop.unwrap_or(true);
    let mut exam_desktop_opt: Option<Arc<desktop::ExamDesktop>> = None;

    if use_secure_desktop {
        log::info!("Before create desktop");
        let desk = desktop::ExamDesktop::create("TutorHubExamDesktop").map_err(|e| {
            let _ = server.send_response(&IpcResponse::LockFailed(format!("Desktop err: {:?}", e)));
            e
        })?;
        log::info!("After create desktop");

        log::info!("Before switch desktop");
        desk.switch_to().map_err(|e| {
            let _ = server.send_response(&IpcResponse::LockFailed(format!(
                "Desktop switch err: {:?}",
                e
            )));
            e
        })?;
        log::info!("After switch desktop");
        exam_desktop_opt = Some(Arc::new(desk));
    } else {
        log::info!("Soft Lock Mode enabled: skipping CreateDesktop/SwitchDesktop");
    }

    if config.enable_keyboard_hook {
        log::info!("Before install keyboard hook");
        keyboard_hook::install().map_err(|e| {
            let _ = server.send_response(&IpcResponse::LockFailed(format!("Hook err: {:?}", e)));
            e
        })?;
        log::info!("After install keyboard hook");
    }

    if config.enable_screen_protection {
        log::info!("Before apply screen protection");
        screen_protection::enable(config.java_pid).map_err(|e| {
            let _ = server.send_response(&IpcResponse::LockFailed(format!(
                "Screen protect err: {:?}",
                e
            )));
            e
        })?;
        log::info!("After apply screen protection");
    }

    log::info!("Before start process scanner");
    process_scanner::init(
        config.banned_process_names.clone(),
        config.banned_process_hashes.clone(),
    ).map_err(|e| {
        let _ = server.send_response(&IpcResponse::LockFailed(format!("Scanner init err: {:?}", e)));
        e
    })?;
    log::info!("After start process scanner");

    let (alert_tx, alert_rx) = mpsc::channel();
    let scan_interval = config.process_scan_interval_secs;

    let _scanner_thread = thread::spawn(move || loop {
        thread::sleep(Duration::from_secs(scan_interval));
        if let Ok(alerts) = process_scanner::scan_once() {
            for alert in alerts {
                if alert_tx.send(alert).is_err() {
                    break;
                }
            }
        }
    });

    log::info!("Before start watchdog");
    let mut wd = Watchdog::new(config.heartbeat_timeout_secs);
    let exam_desktop_opt_clone = exam_desktop_opt.clone();
    let java_pid = config.java_pid;
    let enable_keyboard_hook_flag = config.enable_keyboard_hook;
    let enable_screen_protection_flag = config.enable_screen_protection;

    wd.start(move || {
        log::error!("Watchdog timeout detected");
        log::info!("Before emergency cleanup");
        if enable_keyboard_hook_flag {
            keyboard_hook::uninstall();
        }
        if enable_screen_protection_flag {
            let _ = screen_protection::disable(java_pid);
        }
        if let Some(ref desk) = exam_desktop_opt_clone {
            let _ = desk.switch_to_default();
            log::info!("After switch_to_default");
        }
        log::info!("After emergency cleanup");
        std::process::exit(1);
    }).map_err(|e| {
        let _ = server.send_response(&IpcResponse::LockFailed(format!("Watchdog start err: {:?}", e)));
        e
    })?;
    log::info!("After start watchdog");

    log::info!("Before sending LOCKED response");
    let _ = server.send_response(&IpcResponse::Locked);
    log::info!("After sending LOCKED response");
    log::info!("Lockdown complete. Entering main loop.");

    // Debug-only: spawn a polling thread that checks GetAsyncKeyState for Ctrl+Shift+P.
    // WH_KEYBOARD_LL hooks do NOT receive events on alternate desktops in VMware,
    // so we use direct hardware key state polling as a reliable fallback.
    // CRITICAL: The polling thread MUST call SetThreadDesktop to associate with
    // the exam desktop, otherwise GetAsyncKeyState returns 0 for all keys.
    #[cfg(feature = "debug_mode")]
    {
        let exam_desktop_for_panic = exam_desktop_opt.clone();
        let kb_hook_enabled = config.enable_keyboard_hook;
        let sp_enabled = config.enable_screen_protection;
        let j_pid = config.java_pid;
        
        let exam_desk_handle_raw = if let Some(ref desk) = exam_desktop_opt {
            desk.get_raw_handle().0 as usize
        } else {
            0
        };

        thread::spawn(move || {
            use windows::Win32::UI::Input::KeyboardAndMouse::GetAsyncKeyState;
            use windows::Win32::System::StationsAndDesktops::{SetThreadDesktop, HDESK};

            if exam_desk_handle_raw != 0 {
                let exam_desk_handle = HDESK(exam_desk_handle_raw as *mut _);
                unsafe {
                    match SetThreadDesktop(exam_desk_handle) {
                        Ok(_) => log::info!("Polling thread: SetThreadDesktop to exam desktop OK"),
                        Err(e) => log::error!("Polling thread: SetThreadDesktop FAILED: {}", e),
                    }
                }
            }

            log::info!("Debug panic key polling thread started (GetAsyncKeyState mode)");

            loop {
                thread::sleep(Duration::from_millis(200));

                unsafe {
                    let ctrl = (GetAsyncKeyState(0x11) as i16) < 0;  // VK_CONTROL
                    let shift = (GetAsyncKeyState(0x10) as i16) < 0; // VK_SHIFT
                    let p_key = (GetAsyncKeyState(0x50) as i16) < 0; // 'P' key

                    if ctrl && shift && p_key {
                        log::info!("Dev panic key detected: POLLING_CTRL_SHIFT_P");
                        log::info!("Before panic cleanup");

                        if kb_hook_enabled {
                            keyboard_hook::uninstall();
                        }
                        if sp_enabled {
                            let _ = screen_protection::disable(j_pid);
                        }
                        if let Some(ref desk) = exam_desktop_for_panic {
                            let _ = desk.switch_to_default();
                            log::info!("After switch_to_default");
                        }
                        log::info!("After panic cleanup");
                        std::process::exit(99);
                    }
                }
            }
        });
    }

    loop {
        match server.read_command() {
            Ok(IpcCommand::Ping) => {
                let _ = wd.record_ping();
                let _ = server.send_response(&IpcResponse::Pong);
            }
            Ok(IpcCommand::Unlock) => {
                log::info!("UNLOCK received. Exiting.");
                let _ = server.send_response(&IpcResponse::Unlocked);
                break;
            }
            Ok(IpcCommand::Status) => {
                let _ = server.send_response(&IpcResponse::StatusResponse("LOCKED".to_string()));
            }
            Ok(cmd) => {
                log::warn!("Unknown command in locked state: {:?}", cmd);
            }
            Err(e) => {
                log::error!("IPC read error: {:?}", e);
                let err_str = format!("{:?}", e);
                if err_str.contains("BrokenPipe") || err_str.contains("109") {
                    log::error!("IPC broken pipe detected");
                    log::error!("Java process disconnected");
                    log::info!("Before cleanup due to IPC disconnect");
                    
                    if config.enable_keyboard_hook {
                        keyboard_hook::uninstall();
                    }
                    if config.enable_screen_protection {
                        let _ = screen_protection::disable(config.java_pid);
                    }
                    if let Some(ref desk) = exam_desktop_opt {
                        let _ = desk.switch_to_default();
                        log::info!("After switch_to_default");
                    }
                    
                    // We use std::process::exit(1) to avoid duplicate cleanup at the end of main
                    // But first we must drop exam_desktop to call CloseDesktop
                    drop(exam_desktop_opt);
                    
                    log::info!("After cleanup due to IPC disconnect");
                    std::process::exit(1);
                }
                break;
            }
        }

        while let Ok(alert) = alert_rx.try_recv() {
            let _ = server.send_response(&IpcResponse::ProcessAlert(alert));
        }
    }

    if config.enable_keyboard_hook {
        keyboard_hook::uninstall();
    }
    if config.enable_screen_protection {
        let _ = screen_protection::disable(config.java_pid);
    }

    drop(exam_desktop_opt);

    Ok(())
}
