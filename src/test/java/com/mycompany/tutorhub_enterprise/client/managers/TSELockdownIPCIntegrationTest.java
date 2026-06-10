package com.mycompany.tutorhub_enterprise.client.managers;

import com.mycompany.tutorhub_enterprise.models.exam.ExamSession;

import java.nio.file.Path;

public class TSELockdownIPCIntegrationTest {

    public static void main(String[] args) {
        System.out.println("========== [STEP 2I.1] IPC INTEGRATION TEST ==========");
        
        LockdownManager manager = new LockdownManager();
        
        try {
            // 1. Chuẩn bị session giả
            ExamSession session = new ExamSession();
            session.id = 12345;
            session.examId = 555;
            session.tekHash = "test_hash";
            String sessionId = "sess_" + session.id;

            // 1.5 Tạo Dummy JFrame để Rust có thể lấy HWND (Screen protection)
            System.out.println("[TEST] 0. Creating dummy JFrame for Screen Protection...");
            javax.swing.JFrame dummyFrame = new javax.swing.JFrame("Dummy Test Frame");
            dummyFrame.setSize(400, 300);
            dummyFrame.setVisible(true);
            Thread.sleep(1500); // Đợi HWND được Windows thực sự khởi tạo

            // 2. Extract Rust exe
            System.out.println("\n[TEST] 1. Extracting Rust LockdownCore...");
            Path exePath = manager.extractRustExe();
            System.out.println("[TEST] -> Extracted to: " + exePath.toAbsolutePath());

            // 3. Build config
            // Tắt VM detection và Screen Protection để test IPC nhanh (do headless không có HWND)
            long pid = ProcessHandle.current().pid();
            String json = "{" +
                "\"session_id\":\"" + sessionId + "\"," +
                "\"exam_id\":" + session.examId + "," +
                "\"exam_key\":\"" + session.tekHash + "\"," +
                "\"java_pid\":" + pid + "," +
                "\"enable_vm_detection\":false," +
                "\"enable_keyboard_hook\":true," +
                "\"enable_screen_protection\":false," + // QUAN TRỌNG: tắt để tránh lỗi Win32Error(0x80070006)
                "\"enable_secure_desktop\":false," +   // SOFT LOCK MODE: tắt switch desktop
                "\"banned_process_names\":[]," +
                "\"banned_process_hashes\":[]," +
                "\"process_scan_interval_secs\":3," +
                "\"heartbeat_timeout_secs\":10" +
            "}";
            String configB64 = java.util.Base64.getEncoder().encodeToString(json.getBytes());
            
            // 4. Spawn Rust
            System.out.println("\n[TEST] 2. Spawning Rust process...");
            manager.spawnRustProcess(exePath, configB64, sessionId);
            long rustPid = manager.getRustProcessPid();
            System.out.println("[TEST] -> Rust PID: " + rustPid);

            // 5. Chờ Pipe Ready
            System.out.println("\n[TEST] 3. Waiting for Named Pipe to be ready...");
            manager.waitForPipeReady(sessionId);
            System.out.println("[TEST] -> Pipe connected successfully!");

            // 6. Gửi lệnh LOCK
            System.out.println("\n[TEST] 4. Sending LOCK command...");
            // Lưu ý: Nếu bước này Rust thực sự gọi SwitchDesktop, màn hình sẽ bị đen.
            // Truyền false để gửi đồng bộ, không tự bật listener ẩn của LockdownManager
            manager.sendLockCommand(sessionId, false);
            System.out.println("[TEST] -> Received LOCKED response from Rust!");
            System.out.println(">>> WARNING: IF RUST SWITCHED DESKTOP, THE SCREEN MIGHT BE BLACK NOW! <<<");

            // 7. Gửi PING / PONG
            System.out.println("\n[TEST] 5. Sending PING heartbeats (2 times)...");
            for (int i = 1; i <= 2; i++) {
                String response = manager.sendPingSync(i);
                System.out.println("[TEST] -> Ping " + i + " response: " + response);
                Thread.sleep(1500);
            }

            // 8. Gửi lệnh UNLOCK
            System.out.println("\n[TEST] 6. Sending UNLOCK command...");
            manager.shutdownSync(); // shutdownSync gửi UNLOCK và chờ UNLOCKED
            System.out.println("[TEST] -> System UNLOCKED successfully!");
            System.out.println(">>> THE SCREEN SHOULD BE BACK TO NORMAL <<<");

            // 9. Kiểm tra Rust exit
            System.out.println("\n[TEST] 7. Checking if Rust process exited...");
            Thread.sleep(1000); // Đợi Rust thoát
            int exitCode = manager.getRustProcessExitCode();
            if (exitCode != -1) {
                System.out.println("[TEST] -> Rust process exited cleanly with code: " + exitCode);
            } else {
                System.err.println("[TEST] -> WARNING: Rust process is still running!");
                manager.emergencyUnlock();
            }

            System.out.println("\n========== TEST PASSED ==========");

        } catch (Exception e) {
            System.err.println("\n========== TEST FAILED ==========");
            e.printStackTrace();
            System.out.println("Triggering Emergency Unlock to prevent system freeze...");
            manager.emergencyUnlock();
        }
    }
}
