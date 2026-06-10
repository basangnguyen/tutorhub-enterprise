package com.mycompany.tutorhub_enterprise.client.exam.services;

import com.mycompany.tutorhub_enterprise.client.exam.models.*;
import com.mycompany.tutorhub_enterprise.client.managers.LockdownManager;
import com.mycompany.tutorhub_enterprise.models.exam.ExamSession;

import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SecureNetworkTSEExamService implements TSEExamService {

    private enum LockdownState {
        UNLOCKED,
        LOCKED,
        UNLOCKING,
        CLEANED_UP
    }

    private final TSEExamService coreService;
    private final LockdownManager lockdownManager;
    private LockdownState state = LockdownState.UNLOCKED;
    private Thread heartbeatThread;

    public SecureNetworkTSEExamService(TSEExamService coreService) {
        this.coreService = coreService;
        this.lockdownManager = new LockdownManager();
        
        // Add shutdown hook to ensure cleanup if JVM closes
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (state == LockdownState.LOCKED) {
                System.out.println("[TSE_SECURE] JVM shutting down, cleaning up Rust process...");
                state = LockdownState.UNLOCKING;
                stopHeartbeat();
                lockdownManager.emergencyUnlock();
                state = LockdownState.CLEANED_UP;
            }
        }));
    }

    @Override
    public CompletableFuture<TSELoginResult> login(String username, String password) {
        // Forward thẳng
        return coreService.login(username, password);
    }

    @Override
    public CompletableFuture<List<TSEExamConfig>> getConfigList(int userId) {
        // Forward thẳng
        return coreService.getConfigList(userId);
    }

    @Override
    public CompletableFuture<TSEStartExamResult> verifyPasswordAndStart(int userId, int examId, String password) {
        System.out.println("[TSE_SECURE] Calling core verifyPasswordAndStart...");
        return coreService.verifyPasswordAndStart(userId, examId, password)
            .thenCompose(result -> {
                if (!result.success) {
                    System.out.println("[TSE_SECURE] Core start failed, skipping Lockdown.");
                    return CompletableFuture.completedFuture(result);
                }

                // Nếu start exam success, gọi Rust Lockdown
                return startLockdownAsync(result).thenApply(lockSuccess -> {
                    if (lockSuccess) {
                        return result;
                    } else {
                        // Trả về fail nếu không lock được
                        return new TSEStartExamResult(false, "Failed to activate Secure Mode");
                    }
                });
            });
    }

    private CompletableFuture<Boolean> startLockdownAsync(TSEStartExamResult examResult) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("[TSE_SECURE] Extracting Rust exe...");
                Path exePath = lockdownManager.extractRustExe();

                // Tạo dummy session do TSEStartExamResult chưa chứa đầy đủ ExamSession
                ExamSession session = new ExamSession();
                try {
                    session.id = Integer.parseInt(examResult.sessionId);
                } catch (Exception e) {
                    session.id = 1; // Fallback
                }
                session.examId = 0; // Không có thông tin examId chi tiết ở bước này nhưng đủ để chạy
                session.tekHash = "test_hash";
                String sessionIdStr = "sess_" + session.id;

                System.out.println("[TSE_SECURE] Building Soft Lock config...");
                // SOFT LOCK MODE
                long pid = ProcessHandle.current().pid();
                String json = "{" +
                    "\"session_id\":\"" + sessionIdStr + "\"," +
                    "\"exam_id\":" + session.examId + "," +
                    "\"exam_key\":\"" + session.tekHash + "\"," +
                    "\"java_pid\":" + pid + "," +
                    "\"enable_vm_detection\":false," +
                    "\"enable_keyboard_hook\":true," +
                    "\"enable_screen_protection\":false," + 
                    "\"enable_secure_desktop\":false," +   // SOFT LOCK MODE: tắt switch desktop
                    "\"banned_process_names\":[]," +
                    "\"banned_process_hashes\":[]," +
                    "\"process_scan_interval_secs\":3," +
                    "\"heartbeat_timeout_secs\":10" +
                "}";
                String configB64 = Base64.getEncoder().encodeToString(json.getBytes());

                System.out.println("[TSE_SECURE] Spawning Rust Process...");
                lockdownManager.spawnRustProcess(exePath, configB64, sessionIdStr);

                System.out.println("[TSE_SECURE] Waiting for Named Pipe...");
                lockdownManager.waitForPipeReady(sessionIdStr);

                System.out.println("[TSE_SECURE] Sending LOCK command...");
                lockdownManager.sendLockCommand(sessionIdStr, true);
                
                System.out.println("[TSE_SECURE] System LOCKED successfully.");
                state = LockdownState.LOCKED;
                startHeartbeat();
                return true;
            } catch (Exception e) {
                System.err.println("[TSE_SECURE] Rust returned LOCK failure: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    private void startHeartbeat() {
        heartbeatThread = new Thread(() -> {
            int pingCounter = 1;
            while (state == LockdownState.LOCKED && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(2000); // 2s
                    if (state == LockdownState.LOCKED) {
                        lockdownManager.sendPingSync(pingCounter++);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("[TSE_SECURE] Heartbeat error: " + e.getMessage());
                    break;
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private void stopHeartbeat() {
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
            heartbeatThread = null;
        }
    }

    @Override
    public CompletableFuture<TSESubmitResult> submitExam(String sessionIdStr, int examId, String payload) {
        System.out.println("[TSE_SECURE] Submit started");
        System.out.println("[TSE_SECURE] Calling core submitExam...");
        return coreService.submitExam(sessionIdStr, examId, payload)
            .thenApply(result -> {
                System.out.println("[TSE_SECURE] Core submit result success=" + result.success);
                if (result.success) {
                    if (state == LockdownState.LOCKED) {
                        state = LockdownState.UNLOCKING;
                        System.out.println("[TSE_SECURE] Unlock started after successful submit");
                        stopHeartbeat();
                        try {
                            lockdownManager.shutdownSync();
                            System.out.println("[TSE_SECURE] Cleanup completed");
                            state = LockdownState.CLEANED_UP;
                        } catch (Exception e) {
                            String msg = e.getMessage() != null ? e.getMessage() : "";
                            if (msg.contains("Pipe is not connected") || msg.contains("No process is on the other end of the pipe")) {
                                System.out.println("[TSE_SECURE] Warning during cleanup: Pipe already disconnected. " + msg);
                            } else {
                                System.err.println("[TSE_SECURE] Error unlocking: " + msg);
                                lockdownManager.emergencyUnlock();
                            }
                            state = LockdownState.CLEANED_UP;
                        }
                    } else {
                        System.out.println("[TSE_SECURE] Skip unlock. State is not LOCKED: " + state);
                    }
                } else {
                    System.out.println("[TSE_SECURE] Submit failed. Keeping lockdown active. Error from server: " + result.message);
                }
                return result;
            });
    }

    /**
     * Phương thức dùng để an toàn đóng Lockdown nếu form bị tắt (VD: nút X)
     */
    public void cleanupOnClose() {
        if (state == LockdownState.LOCKED) {
            System.out.println("[TSE_SECURE] Force cleanup on close...");
            state = LockdownState.UNLOCKING;
            stopHeartbeat();
            try {
                lockdownManager.shutdownSync();
            } catch (Exception e) {
                lockdownManager.emergencyUnlock();
            } finally {
                state = LockdownState.CLEANED_UP;
            }
        }
    }
}
