package com.mycompany.tutorhub_enterprise.client.managers;

import com.mycompany.tutorhub_enterprise.models.exam.ExamSession;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class LockdownManager {
    private static final String EXE_NAME = "TutorHub_LockdownCore.exe";
    private static final String RESOURCE_PATH = "/tools/" + EXE_NAME;
    private static final String SHA256_RESOURCE_PATH = RESOURCE_PATH + ".sha256";

    private Path extractedExePath;
    private Process rustProcess;
    
    private RustIPCClient ipcClient;
    private Thread pingThread;
    private volatile boolean isPinging = false;
    private String currentSessionId;
    private volatile boolean emergencyUnlockTriggered = false;

    public LockdownManager() {
        this.ipcClient = new RustIPCClient();
        this.ipcClient.setDisconnectListener(() -> {
            System.err.println("IPC Client disconnected unexpectedly!");
            emergencyUnlock();
        });
    }

    public Path extractRustExe() throws Exception {
        InputStream shaStream = getClass().getResourceAsStream(SHA256_RESOURCE_PATH);
        if (shaStream == null) {
            throw new Exception("Missing SHA256 file in resources: " + SHA256_RESOURCE_PATH);
        }
        String expectedSha256;
        try (Scanner scanner = new Scanner(shaStream, "UTF-8")) {
            expectedSha256 = scanner.useDelimiter("\\A").next().trim();
        }

        InputStream exeStream = getClass().getResourceAsStream(RESOURCE_PATH);
        if (exeStream == null) {
            throw new Exception("Missing executable in resources: " + RESOURCE_PATH);
        }

        Path tempDir = Files.createTempDirectory("tutorhub_lockdown");
        Path exePath = tempDir.resolve(EXE_NAME);
        
        Files.copy(exeStream, exePath, StandardCopyOption.REPLACE_EXISTING);

        String actualSha256 = calculateSha256(exePath);
        if (!expectedSha256.equalsIgnoreCase(actualSha256)) {
            Files.deleteIfExists(exePath);
            throw new Exception("SHA256 verification failed for LockdownCore.exe!");
        }

        this.extractedExePath = exePath;
        return exePath;
    }

    private String calculateSha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file);
        byte[] hashBytes = digest.digest(fileBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String buildLockConfig(ExamSession session) {
        return buildLockConfig(session, true);
    }

    public String buildLockConfig(ExamSession session, boolean enableVmDetection) {
        return buildLockConfig(session, enableVmDetection, false, false);
    }

    public String buildLockConfig(ExamSession session, boolean enableVmDetection, boolean testProcessAlert) {
        return buildLockConfig(session, enableVmDetection, testProcessAlert, false);
    }

    public String buildLockConfig(ExamSession session, boolean enableVmDetection, boolean testProcessAlert, boolean testObsScreenProtection) {
        long pid = ProcessHandle.current().pid();
        String sessionId = "sess_" + session.id;
        
        String bannedProcs = "\"teamviewer.exe\", \"anydesk.exe\"";
        if (!testObsScreenProtection) {
            bannedProcs += ", \"obs64.exe\", \"obs32.exe\"";
        }
        if (testProcessAlert) {
            bannedProcs += ", \"notepad.exe\"";
        }

        String json = "{" +
            "\"session_id\":\"" + sessionId + "\"," +
            "\"exam_id\":" + session.examId + "," +
            "\"exam_key\":\"" + (session.tekHash != null ? session.tekHash : "") + "\"," +
            "\"java_pid\":" + pid + "," +
            "\"enable_vm_detection\":" + enableVmDetection + "," +
            "\"enable_keyboard_hook\":true," +
            "\"enable_screen_protection\":true," +
            "\"banned_process_names\":[" + bannedProcs + "]," +
            "\"banned_process_hashes\":[]," +
            "\"process_scan_interval_secs\":3," +
            "\"heartbeat_timeout_secs\":10" +
        "}";

        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    public void spawnRustProcess(Path exePath, String configB64, String sessionId) throws Exception {
        this.currentSessionId = sessionId;
        ProcessBuilder pb = new ProcessBuilder(
            exePath.toAbsolutePath().toString(),
            "--session-id", sessionId,
            "--config", configB64
        );
        pb.inheritIO();
        this.rustProcess = pb.start();
    }

    public Process spawnChildOnSecureDesktop(Path exePath, String javaExePath, String jarPath, String contextPath, String outputPath, String keyB64) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            exePath.toAbsolutePath().toString(),
            "--spawn-child",
            "--java-exe", javaExePath,
            "--jar", jarPath,
            "--context", contextPath,
            "--output", outputPath,
            "--key", keyB64
        );
        pb.redirectErrorStream(true);
        this.rustProcess = pb.start();
        return this.rustProcess;
    }

    public long getRustProcessPid() {
        return (rustProcess != null) ? rustProcess.pid() : -1;
    }

    public int getRustProcessExitCode() {
        if (rustProcess != null && !rustProcess.isAlive()) {
            return rustProcess.exitValue();
        }
        return -1; // Still alive or not started
    }

    public void waitForPipeReady(String sessionId) throws Exception {
        int retries = 10;
        boolean connected = false;
        for (int i = 0; i < retries; i++) {
            if (ipcClient.connect(sessionId)) {
                connected = true;
                break;
            }
            Thread.sleep(500);
        }
        
        if (!connected) {
            throw new Exception("Failed to connect to Rust IPC pipe after 10 retries.");
        }
        
        this.currentSessionId = sessionId;
        // Do NOT start listener here. Wait for LOCK handshake to complete first.
    }

    public void sendLockCommand(String sessionId) throws Exception {
        sendLockCommand(sessionId, true);
    }

    public void sendLockCommand(String sessionId, boolean startListenerAsync) throws Exception {
        if (ipcClient == null) throw new Exception("IPC Client not initialized");
        
        System.out.println("Before send LOCK");
        ipcClient.sendCommand("LOCK");
        System.out.println("After send LOCK");
        
        System.out.println("Before read LOCK response");
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<String> future = executor.submit(() -> ipcClient.readResponse());
        String response;
        try {
            response = future.get(15, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            System.err.println("LOCK response timeout");
            emergencyUnlock();
            throw new Exception("LOCK response timeout");
        } finally {
            executor.shutdownNow();
        }
        
        System.out.println("Received LOCK response: " + response);

        if (response == null || response.trim().startsWith("LOCK_FAILED")) {
            emergencyUnlock();
            throw new Exception("Lockdown failed: " + response);
        }

        if (!response.trim().equals("LOCKED")) {
            emergencyUnlock();
            throw new Exception("Unexpected response during LOCK handshake: " + response);
        }

        if (startListenerAsync) {
            // Handshake successful, start the async listener for PROCESS_ALERT, PONG, UNLOCKED
            ipcClient.startListener();
            startPingThread();
        }
    }

    public String sendPingSync(int pingNumber) throws Exception {
        System.out.println("Sync PING " + pingNumber);
        try {
            ipcClient.sendPing();
        } catch (Exception e) {
            System.err.println("PING send failed: " + e.getMessage());
            emergencyUnlock();
            throw e;
        }
        
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<String> future = executor.submit(() -> ipcClient.readResponse());
        String response;
        try {
            response = future.get(10, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            System.err.println("PING response timeout (10s). Rust process might be stuck.");
            emergencyUnlock();
            throw new Exception("PING response timeout");
        } catch (Exception e) {
            System.err.println("PING read failed: " + e.getMessage());
            emergencyUnlock();
            throw e;
        } finally {
            executor.shutdownNow();
        }

        if (response != null && response.trim().equals("PONG")) {
            System.out.println("Received PING response: PONG");
        } else if (response == null) {
            System.err.println("IPC pipe EOF. Rust process disconnected.");
            emergencyUnlock();
            throw new Exception("IPC disconnected");
        } else {
            System.out.println("Received response: " + response);
        }
        return response;
    }

    public void startPingThread() {
        if (isPinging) return;
        System.out.println("Starting ping thread");
        isPinging = true;
        pingThread = new Thread(() -> {
            while (isPinging) {
                try {
                    ipcClient.sendPing();
                    Thread.sleep(2000);
                } catch (Exception e) {
                    System.err.println("Ping failed, Rust process might be dead: " + e.getMessage());
                    emergencyUnlock();
                    break;
                }
            }
        });
        pingThread.setDaemon(true);
        pingThread.start();
    }

    public synchronized void emergencyUnlock() {
        if (emergencyUnlockTriggered) return;
        emergencyUnlockTriggered = true;
        
        System.err.println("Emergency unlock triggered!");
        isPinging = false;
        if (pingThread != null) {
            pingThread.interrupt();
        }
        
        if (ipcClient != null) {
            try {
                ipcClient.sendUnlockCommand();
            } catch (Exception e) {
                // Ignore errors during emergency
            }
            ipcClient.close();
        }
        
        if (rustProcess != null && rustProcess.isAlive()) {
            rustProcess.destroyForcibly();
        }

        System.err.println("Starting Rust emergency reset process");
        if (extractedExePath != null && java.nio.file.Files.exists(extractedExePath)) {
            try {
                ProcessBuilder pb = new ProcessBuilder(extractedExePath.toAbsolutePath().toString(), "--emergency-reset");
                pb.inheritIO();
                Process resetProc = pb.start();
                if (resetProc.waitFor(5, TimeUnit.SECONDS)) {
                    System.out.println("Emergency reset process exited with code " + resetProc.exitValue());
                } else {
                    System.err.println("Emergency reset process timed out.");
                    resetProc.destroyForcibly();
                }
            } catch (Exception e) {
                System.err.println("Failed to run emergency reset: " + e.getMessage());
            }
        } else {
            System.err.println("Extracted EXE path is null or does not exist, cannot run emergency reset.");
        }
    }

    public void shutdownSync() {
        if (ipcClient != null) {
            try {
                System.out.println("Before send UNLOCK");
                ipcClient.sendUnlockCommand();
                
                long startTime = System.currentTimeMillis();
                boolean unlocked = false;
                
                while (System.currentTimeMillis() - startTime < 10000) {
                    java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
                    java.util.concurrent.Future<String> future = executor.submit(() -> ipcClient.readResponse());
                    String response;
                    try {
                        long remaining = 10000 - (System.currentTimeMillis() - startTime);
                        if (remaining <= 0) break;
                        response = future.get(remaining, java.util.concurrent.TimeUnit.MILLISECONDS);
                    } catch (java.util.concurrent.TimeoutException e) {
                        future.cancel(true);
                        System.err.println("Timeout waiting for UNLOCKED. Calling emergencyUnlock.");
                        emergencyUnlock();
                        return;
                    } finally {
                        executor.shutdownNow();
                    }

                    if (response != null) {
                        String r = response.trim();
                        if (r.equals("UNLOCKED")) {
                            System.out.println("Received UNLOCK response: UNLOCKED");
                            unlocked = true;
                            break;
                        } else if (r.equals("PONG")) {
                            System.out.println("Ignoring stale PONG while waiting for UNLOCKED");
                        } else if (r.startsWith("PROCESS_ALERT:")) {
                            System.out.println("Ignoring alert while waiting for UNLOCKED");
                        } else {
                            System.out.println("Ignoring unexpected response while waiting for UNLOCKED: " + response);
                        }
                    } else {
                        break;
                    }
                }
                
                if (!unlocked) {
                    System.err.println("Unexpected UNLOCK response or timeout.");
                }
            } catch (Exception e) {
                System.err.println("Error during graceful shutdown: " + e.getMessage());
                emergencyUnlock();
                return;
            } finally {
                ipcClient.close();
            }
        }

        if (rustProcess != null && rustProcess.isAlive()) {
            rustProcess.destroy();
            try {
                if (!rustProcess.waitFor(3, TimeUnit.SECONDS)) {
                    rustProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                rustProcess.destroyForcibly();
            }
        }
    }

    public void shutdown() {
        if (ipcClient != null) {
            try {
                CountDownLatch latch = new CountDownLatch(1);
                ipcClient.setGenericResponseListener(response -> {
                    if (response.equals("UNLOCKED")) {
                        latch.countDown();
                    }
                });
                System.out.println("Before send UNLOCK");
                ipcClient.sendUnlockCommand();
                System.out.println("After send UNLOCK");
                
                // Wait for UNLOCKED acknowledgment up to 10 seconds
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    System.err.println("Timeout waiting for UNLOCKED. Calling emergencyUnlock.");
                    emergencyUnlock();
                    return;
                }
            } catch (Exception e) {
                System.err.println("Error during graceful shutdown: " + e.getMessage());
                emergencyUnlock();
                return;
            } finally {
                isPinging = false;
                if (pingThread != null) {
                    pingThread.interrupt();
                }
                ipcClient.close();
            }
        }

        if (rustProcess != null && rustProcess.isAlive()) {
            rustProcess.destroy();
            try {
                if (!rustProcess.waitFor(3, TimeUnit.SECONDS)) {
                    rustProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                rustProcess.destroyForcibly();
            }
        }
    }
}
