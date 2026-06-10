package com.mycompany.tutorhub_enterprise.client.managers;

import com.mycompany.tutorhub_enterprise.models.exam.ExamSession;

import java.nio.file.Path;
import java.util.Scanner;

public class LockdownIntegrationTestHarness {

    public static void main(String[] args) {
        if (args.length < 2 || !args[0].equals("--test")) {
            printUsage();
            return;
        }

        String testType = args[1];
        int minutes = 1;

        if (args.length >= 4 && args[2].equals("--minutes")) {
            try {
                minutes = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid minutes parameter.");
                return;
            }
        }

        System.out.println("Starting VM Integration Test Harness...");
        System.out.println("Test Type: " + testType);
        
        LockdownManager manager = new LockdownManager();
        
        try {
            ExamSession session = new ExamSession();
            session.id = 9999;
            session.examId = 1234;
            
            Path exePath = manager.extractRustExe();
            System.out.println("Extracted to: " + exePath);

            boolean enableVmDetection = testType.equals("vm-detection");
            boolean testProcessAlert = testType.equals("process-alert");
            boolean testObsScreenProtection = testType.equals("obs-screen-protection");
            String configB64 = manager.buildLockConfig(session, enableVmDetection, testProcessAlert, testObsScreenProtection);
            String sessionId = "sess_" + session.id;

            System.out.println("Spawning Rust process (enableVmDetection=" + enableVmDetection + ")...");
            manager.spawnRustProcess(exePath, configB64, sessionId);
            
            if (enableVmDetection) {
                System.out.println("VM Detection test: Waiting briefly for Rust process to exit due to VM detection...");
                Thread.sleep(2000);
                System.out.println("VM Detection test finished. Check if Rust process exited with VmDetected log/error.");
                manager.emergencyUnlock(); // Don't call graceful shutdown, it prints pipe errors if not connected
                return;
            }

            System.out.println("Waiting for IPC Pipe to become ready...");
            manager.waitForPipeReady(sessionId);
            
            if (testType.equals("kill-java") || testType.equals("kill-rust")) {
                long javaPid = ProcessHandle.current().pid();
                long rustPid = manager.getRustProcessPid();
                System.out.println("==================================================");
                System.out.println("PREPARATION PHASE: " + testType);
                System.out.println("JAVA PID: " + javaPid);
                System.out.println("RUST PID: " + rustPid);
                if (testType.equals("kill-java")) {
                    System.out.println("Command to kill Java: taskkill /F /PID " + javaPid);
                } else {
                    System.out.println("Command to kill Rust: taskkill /F /PID " + rustPid);
                }
                System.out.println("Please prepare your command prompt now.");
                System.out.println("Waiting 10 seconds BEFORE entering Kiosk Mode...");
                System.out.println("==================================================");
                for (int w = 10; w > 0; w--) {
                    System.out.println(w + "...");
                    Thread.sleep(1000);
                }
            }

            System.out.println("Pipe connected. Sending LOCK command...");
            boolean isSyncTest = testType.equals("lock-unlock") || testType.equals("ping-loop") || testType.equals("process-alert") || testType.equals("kill-java") || testType.equals("kill-rust") || testType.equals("dev-panic-key") || testType.equals("auto-kill") || testType.equals("obs-screen-protection");
            manager.sendLockCommand(sessionId, !isSyncTest);
            System.out.println("System is now LOCKED (Kiosk Mode).");

            switch (testType) {
                case "obs-screen-protection":
                    System.out.println("Starting OBS Screen Protection test...");
                    System.out.println("OBS is allowed to run. Open OBS and check if the recording/preview is BLACK.");
                    System.out.println("Waiting for up to 60 seconds. You can check the screen now.");
                    for (int i = 1; i <= 30; i++) {
                        manager.sendPingSync(i);
                        Thread.sleep(2000);
                    }
                    System.out.println("OBS Screen Protection test time is up. Initiating unlock...");
                    manager.shutdownSync();
                    System.out.println("Test obs-screen-protection completed.");
                    break;
                case "lock-unlock":
                    manager.sendPingSync(1);
                    Thread.sleep(2000);
                    manager.sendPingSync(2);
                    Thread.sleep(2000);
                    System.out.println("Sleeping before unlock");
                    manager.shutdownSync();
                    System.out.println("Test lock-unlock completed");
                    break;
                    
                case "ping-loop":
                    System.out.println("Starting sync PING loop for " + minutes + " minutes...");
                    int totalPings = (minutes * 60) / 2;
                    for (int i = 1; i <= totalPings; i++) {
                        manager.sendPingSync(i);
                        Thread.sleep(2000);
                    }
                    System.out.println("Ping loop finished. Initiating unlock...");
                    manager.shutdownSync();
                    System.out.println("Test ping-loop completed");
                    break;
                    
                case "process-alert":
                    System.out.println("Starting sync PROCESS_ALERT test for max 60 seconds...");
                    System.out.println("Open notepad.exe now to trigger PROCESS_ALERT.");
                    boolean alertReceived = false;
                    for (int i = 1; i <= 30; i++) {
                        String resp = manager.sendPingSync(i);
                        if (resp != null && resp.startsWith("PROCESS_ALERT:")) {
                            System.out.println("SUCCESS: Received expected alert: " + resp);
                            alertReceived = true;
                            break;
                        }
                        Thread.sleep(2000);
                    }
                    if (!alertReceived) {
                        System.err.println("FAILED: No PROCESS_ALERT received within 60 seconds.");
                    }
                    System.out.println("Initiating unlock...");
                    manager.shutdownSync();
                    System.out.println("Test process-alert completed");
                    break;

                case "kill-java":
                    System.out.println("Starting kill-java test...");
                    System.out.println("System is LOCKED. Please run the taskkill command now.");
                    System.out.println("Waiting up to 60 seconds for you to kill Java...");
                    for (int i = 1; i <= 30; i++) {
                        manager.sendPingSync(i);
                        Thread.sleep(2000);
                    }
                    System.out.println("If you see this, Java was not killed. Test failed.");
                    manager.shutdownSync();
                    break;

                case "kill-rust":
                    System.out.println("Starting kill-rust test...");
                    System.out.println("System is LOCKED. Please run the taskkill command on Rust now.");
                    System.out.println("Waiting up to 60 seconds for you to kill Rust...");
                    for (int i = 1; i <= 30; i++) {
                        try {
                            manager.sendPingSync(i);
                        } catch (Exception e) {
                            System.out.println("SUCCESS: Rust process was killed, PING failed as expected. Emergency unlock was triggered.");
                            System.out.println("Test kill-rust completed.");
                            return;
                        }
                        Thread.sleep(2000);
                    }
                    System.out.println("If you see this, Rust was not killed. Test failed.");
                    manager.shutdownSync();
                    break;


                case "auto-kill":
                    System.out.println("Starting auto-kill test...");
                    System.out.println("System is LOCKED. Do not press any panic key.");
                    System.out.println("Waiting up to 130 seconds for Auto-kill timer (120s) to expire...");
                    for (int i = 1; i <= 65; i++) {
                        try {
                            manager.sendPingSync(i);
                        } catch (Exception e) {
                            System.out.println("Rust process disconnected. Checking exit code...");
                            Thread.sleep(1000); // Wait briefly for process to fully terminate
                            int exitCode = manager.getRustProcessExitCode();
                            if (exitCode == 98) {
                                System.out.println("SUCCESS: Rust process exited due to auto-kill timer (exit code 98).");
                                System.out.println("Test auto-kill completed.");
                            } else if (exitCode == 99) {
                                System.err.println("FAILED: Rust process exited due to panic key (exit code 99). You pressed the panic key instead of waiting.");
                            } else {
                                System.err.println("FAILED: Rust process exited with code " + exitCode + ".");
                            }
                            return;
                        }
                        Thread.sleep(2000);
                    }
                    System.err.println("FAILED: Auto-kill timer did not trigger within 130 seconds.");
                    manager.shutdownSync();
                    break;

                case "dev-panic-key":
                    System.out.println("Starting dev-panic-key test...");
                    System.out.println("System is LOCKED.");
                    System.out.println("Press Ctrl+Shift+P (easiest in VMware), or Ctrl+Shift+F12, or Ctrl+Shift+Alt+F12 NOW.");
                    System.out.println("Waiting up to 120 seconds for you to press the key...");
                    for (int i = 1; i <= 60; i++) {
                        try {
                            manager.sendPingSync(i);
                        } catch (Exception e) {
                            System.out.println("Rust process disconnected. Checking exit code...");
                            Thread.sleep(1000); // Wait briefly for process to fully terminate
                            int exitCode = manager.getRustProcessExitCode();
                            if (exitCode == 99) {
                                System.out.println("SUCCESS: Rust process exited due to panic key (exit code 99). Emergency unlock triggered.");
                                System.out.println("Test dev-panic-key completed.");
                            } else if (exitCode == 98) {
                                System.err.println("FAILED: Rust process exited due to auto-kill timer (exit code 98). You did not press the panic key in time.");
                            } else {
                                System.err.println("FAILED: Rust process exited with code " + exitCode + ".");
                            }
                            return;
                        }
                        Thread.sleep(2000);
                    }
                    System.out.println("If you see this, Dev panic key was not pressed or didn't work. Test failed.");
                    manager.shutdownSync();
                    break;

                default:
                    System.err.println("Unknown test type: " + testType);
                    manager.shutdown();
            }
            
        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            System.err.println("Calling emergency unlock...");
            manager.emergencyUnlock();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -cp <classpath> com.mycompany.tutorhub_enterprise.client.managers.LockdownIntegrationTestHarness --test <type> [--minutes <min>]");
        System.out.println("Available types:");
        System.out.println("  lock-unlock");
        System.out.println("  ping-loop");
        System.out.println("  process-alert");
        System.out.println("  vm-detection");
        System.out.println("  kill-java");
        System.out.println("  kill-rust");
        System.out.println("  auto-kill");
        System.out.println("  dev-panic-key");
        System.out.println("  obs-screen-protection");
    }
}
