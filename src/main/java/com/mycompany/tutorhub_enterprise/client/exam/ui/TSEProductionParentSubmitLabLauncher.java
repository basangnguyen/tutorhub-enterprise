package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSEExamConfig;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSEExamContext;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSELoginResult;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSEStartExamResult;
import com.mycompany.tutorhub_enterprise.client.exam.models.TSESubmitResult;
import com.mycompany.tutorhub_enterprise.client.exam.services.NetworkTSEExamService;
import com.mycompany.tutorhub_enterprise.client.managers.LockdownManager;
import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;
import com.mycompany.tutorhub_enterprise.client.exam.utils.RecoveryKeyStore;
import com.mycompany.tutorhub_enterprise.client.exam.utils.WindowsDpapiRecoveryKeyStore;

import javax.swing.*;
import java.awt.*;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TSEProductionParentSubmitLabLauncher {
    
    private static Path currentSessionTempDir = null;
    private static Path currentPayloadFile = null;
    private static Path currentContextFile = null;
    private static String currentSessionId = null;
    private static int currentExamId = -1;
    private static String currentKeyB64 = null;
    private static boolean startInProgress = false;
    private static boolean autoStart = false;
    
    public static void main(String[] args) {
        int requestedExamId = -1;
        for (int i = 0; i < args.length; i++) {
            if ("--exam-id".equals(args[i]) && i + 1 < args.length) {
                try {
                    requestedExamId = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException ignored) {}
            }
            if ("--auto-start".equals(args[i])) {
                autoStart = true;
            }
        }
        System.out.println("==================================================");
        System.out.println("TSE_BUILD_VERSION=2I.9.5-3STEP-UX");
        System.out.println("java.class.path=" + System.getProperty("java.class.path"));
        System.out.println("user.dir=" + System.getProperty("user.dir"));
        System.out.println("tutorhub.app.root=" + System.getProperty("tutorhub.app.root"));
        System.out.println("tutorhub.app.jar=" + System.getProperty("tutorhub.app.jar"));
        System.out.print("args received=");
        for(String arg : args) System.out.print(arg + " ");
        System.out.println("\n==================================================");
        
        final int finalRequestedExamId = requestedExamId;

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("TutorHub Secure Exam — Production Launcher [2I.9.5]");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Fullscreen like other kiosk UI
            frame.setUndecorated(true);
            frame.setLocationRelativeTo(null);
            
            CardLayout cardLayout = new CardLayout();
            JPanel rootPanel = new JPanel(cardLayout);
            frame.setContentPane(rootPanel);
            
            // --- LOG PANEL (Card 3: LOGS) ---
            JPanel logPanel = new JPanel(new BorderLayout());
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            final JLabel statusLabel = new JLabel("Status: Idle");
            statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            topPanel.add(statusLabel, BorderLayout.CENTER);
            logPanel.add(topPanel, BorderLayout.NORTH);
            
            JTextArea logArea = new JTextArea();
            logArea.setEditable(false);
            logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
            
            if (finalRequestedExamId > 0) {
                logArea.append("Requested examId from main TutorHub app: " + finalRequestedExamId + "\n");
                currentExamId = finalRequestedExamId;
            }
            
            JPanel btnPanel = new JPanel(new FlowLayout());
            JButton startBtn = new JButton("Start Secure Exam");
            JButton retryBtn = new JButton("Retry Submit");
            JButton recoverBtn = new JButton("Recover Pending Submission");
            JButton exitBtn = new JButton("Exit");
            retryBtn.setVisible(false);
            
            logPanel.add(btnPanel, BorderLayout.SOUTH);
            
            exitBtn.addActionListener(e -> {
                frame.dispose(); // Don't use System.exit()
            });
            
            NetworkTSEExamService service = new NetworkTSEExamService();
            
            // --- UI Transitions ---
            Runnable exitAction = () -> frame.dispose();
            
            Runnable submitLogic = () -> {
                try {
                    setStatus(statusLabel, "Submitting to server...");
                    log(logArea, "Submitting to server for Session ID: " + currentSessionId);
                    String payloadEncrypted = new String(Files.readAllBytes(currentPayloadFile), "UTF-8");
                    String payloadContent = CryptoUtils.decryptWrapper(payloadEncrypted, currentKeyB64);
                    log(logArea, "Decrypted payload size: " + payloadContent.length());
                    
                    TSESubmitResult submitRes = service.submitExam(currentSessionId, currentExamId, payloadContent)
                            .get(30, java.util.concurrent.TimeUnit.SECONDS);
                    
                    if (submitRes.success) {
                        setStatus(statusLabel, "Submit successful");
                        log(logArea, "Submit SUCCESS: " + submitRes.message);
                        
                        log(logArea, "Cleaning up temp folder: " + currentSessionTempDir.toAbsolutePath());
                        try {
                            Files.walk(currentSessionTempDir)
                                .map(Path::toFile)
                                .forEach(File::delete);
                            log(logArea, "Cleanup complete.");
                        } catch (Exception ioEx) {
                            log(logArea, "Failed to cleanup temp folder: " + ioEx.getMessage());
                        }
                        
                        startInProgress = false;
                        SwingUtilities.invokeLater(() -> startBtn.setEnabled(true));
                    } else {
                        setStatus(statusLabel, "Submit Failed");
                        log(logArea, "Submit FAILED: " + submitRes.message);
                        log(logArea, "Please click Retry Submit or contact Admin. Payload remains in: " + currentSessionTempDir.toAbsolutePath());
                        SwingUtilities.invokeLater(() -> retryBtn.setVisible(true));
                    }
                } catch (java.util.concurrent.TimeoutException tex) {
                    setStatus(statusLabel, "Submit Timeout");
                    log(logArea, "[TSE_SUBMIT] Server submit timeout after 30 seconds. Please retry.");
                    SwingUtilities.invokeLater(() -> retryBtn.setVisible(true));
                } catch (Exception ex) {
                    setStatus(statusLabel, "Submit Error");
                    log(logArea, "Submit Exception: " + ex.getMessage());
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> retryBtn.setVisible(true));
                }
            };
            
            java.util.function.Consumer<TSEExamContext> onLoginSuccess = (ctx) -> {
                TSEConfigListPanel pnlConfig = new TSEConfigListPanel(service, ctx, finalRequestedExamId, (examCfg) -> {
                    // Do NOT switch to LOGS card. Stay on CONFIG card but show a wait cursor or overlay.
                    rootPanel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                    log(logArea, "[TSE_UI] User confirmed start examId: " + examCfg.examId);
                    log(logArea, "Selected Exam: " + examCfg.examTitle);
                    
                    // Wire the startBtn to start this specific exam!
                    // Clear previous listeners
                    for(java.awt.event.ActionListener al : startBtn.getActionListeners()) {
                        startBtn.removeActionListener(al);
                    }
                    
                    Runnable startSecureExamFlow = () -> {
                        startFlowLogic(service, logArea, statusLabel, startBtn, retryBtn, frame, ctx.userId, examCfg, submitLogic);
                    };
                    
                    startBtn.addActionListener(e -> {
                        startSecureExamFlow.run();
                    });
                    
                    // We automatically start the flow right after user confirmed in Config panel
                    SwingUtilities.invokeLater(startSecureExamFlow);
                    
                    if (autoStart && finalRequestedExamId > 0) {
                        log(logArea, "Auto-start flag detected. (Handled by startSecureExamFlow directly)");
                    }
                }, exitAction);
                
                // Add debug label to CONFIG
                JPanel configWrapper = new JPanel(new BorderLayout());
                JLabel lblDebugConfig = new JLabel("TSE Configuration Files");
                lblDebugConfig.setForeground(Color.RED);
                configWrapper.add(lblDebugConfig, BorderLayout.NORTH);
                configWrapper.add(pnlConfig, BorderLayout.CENTER);
                
                rootPanel.add(configWrapper, "CONFIG");
                log(logArea, "[TSE_UI] Login success, showing CONFIG card");
                cardLayout.show(rootPanel, "CONFIG");
            };
            
            TSELoginPanel pnlLogin = new TSELoginPanel(service, onLoginSuccess, exitAction);
            JPanel loginWrapper = new JPanel(new BorderLayout());
            JLabel lblDebugLogin = new JLabel("Đăng nhập hệ thống");
            lblDebugLogin.setForeground(Color.RED);
            loginWrapper.add(lblDebugLogin, BorderLayout.NORTH);
            loginWrapper.add(pnlLogin, BorderLayout.CENTER);
            
            // Add debug label to LOGS
            JLabel lblDebugLogs = new JLabel("Khởi chạy môi trường thi bảo mật");
            lblDebugLogs.setForeground(Color.RED);
            logPanel.add(lblDebugLogs, BorderLayout.WEST);
            
            rootPanel.add(loginWrapper, "LOGIN");
            rootPanel.add(logPanel, "LOGS");
            
            Runnable connectToServer = () -> {
                pnlLogin.setConnectionStatus("Đang kết nối tới Server...", false, false);
                new Thread(() -> {
                    try {
                        log(logArea, "[TSE_PORTAL] Connecting to server localhost:7860...");
                        if (!NetworkManager.getInstance().isConnected()) {
                            NetworkManager.getInstance().connect("localhost", 7860);
                        }
                        log(logArea, "[TSE_PORTAL] Server connected.");
                        pnlLogin.setConnectionStatus("Đã kết nối Server", true, false);
                        
                        if ("true".equals(System.getProperty("dev.autoLogin"))) {
                            log(logArea, "dev.autoLogin=true. Simulating login success...");
                            TSEExamContext mockCtx = new TSEExamContext();
                            mockCtx.userId = 1;
                            mockCtx.token = "mock_token";
                            TSELoginResult mockRes = new TSELoginResult(true, "Mock", mockCtx);
                            onLoginSuccess.accept(mockRes.context);
                        }
                    } catch (Exception ex) {
                        log(logArea, "[TSE_PORTAL] Server connect failed: " + ex.getMessage());
                        pnlLogin.setConnectionStatus("Không kết nối được Server", false, true);
                    }
                }).start();
            };
            
            pnlLogin.setOnRetryConnect(connectToServer);
            connectToServer.run();

            
            retryBtn.addActionListener(e -> {
                retryBtn.setEnabled(false);
                new Thread(() -> {
                    submitLogic.run();
                    SwingUtilities.invokeLater(() -> retryBtn.setEnabled(true));
                }).start();
            });
            
            recoverBtn.addActionListener(e -> {
                recoverBtn.setEnabled(false);
                startBtn.setEnabled(false);
                new Thread(() -> {
                    try {
                        String tempDirStr = System.getProperty("java.io.tmpdir");
                        File[] tempFiles = new File(tempDirStr).listFiles((dir, name) -> name.startsWith("tse_exam_"));
                        if (tempFiles == null || tempFiles.length == 0) {
                            log(logArea, "No pending submission folders found in %TEMP%");
                            setStatus(statusLabel, "Idle");
                            return;
                        }
                        
                        log(logArea, "Found " + tempFiles.length + " pending folder(s). Checking...");
                        
                        File selectedFolder = null;
                        if (tempFiles.length > 1) {
                            String[] options = new String[tempFiles.length];
                            for (int i = 0; i < tempFiles.length; i++) {
                                options[i] = tempFiles[i].getName();
                            }
                            String selected = (String) JOptionPane.showInputDialog(
                                    frame, "Select a pending session to recover:", "Recover Submission",
                                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                            if (selected == null) {
                                log(logArea, "Recovery cancelled.");
                                setStatus(statusLabel, "Idle");
                                return;
                            }
                            selectedFolder = new File(tempDirStr, selected);
                        } else {
                            selectedFolder = tempFiles[0];
                        }
                        
                        File[] targetFolders = new File[]{selectedFolder};
                        
                        for (File folder : targetFolders) {
                            Path sessionDir = folder.toPath();
                            Path recoveryKeyFile = sessionDir.resolve("recovery_key.enc");
                            Path submitFile = sessionDir.resolve("submit_payload.enc");
                            Path autosaveFile = sessionDir.resolve("autosave_payload.enc");
                            
                            if (Files.exists(recoveryKeyFile)) {
                                log(logArea, "Found recovery_key.enc in " + folder.getName());
                                RecoveryKeyStore keyStore = new WindowsDpapiRecoveryKeyStore();
                                byte[] encKeyBytes = Files.readAllBytes(recoveryKeyFile);
                                try {
                                    String recoveredKey = keyStore.unprotectKey(encKeyBytes);
                                    log(logArea, "Successfully unprotected AES session key via DPAPI.");
                                    
                                    Path targetPayload = null;
                                    if (Files.exists(submitFile)) {
                                        targetPayload = submitFile;
                                        log(logArea, "Found submit_payload.enc for recovery.");
                                    } else if (Files.exists(autosaveFile)) {
                                        targetPayload = autosaveFile;
                                        log(logArea, "Found autosave_payload.enc for recovery.");
                                    }
                                    
                                    if (targetPayload != null) {
                                        currentSessionTempDir = sessionDir;
                                        currentPayloadFile = targetPayload;
                                        currentKeyB64 = recoveredKey;
                                        
                                        String payloadEncrypted = new String(Files.readAllBytes(targetPayload), "UTF-8");
                                        String payloadContent = CryptoUtils.decryptWrapper(payloadEncrypted, currentKeyB64);
                                        
                                        JsonObject payloadObj = new Gson().fromJson(payloadContent, JsonObject.class);
                                        if (payloadObj.has("sessionId")) {
                                            currentSessionId = payloadObj.get("sessionId").getAsString();
                                            currentExamId = payloadObj.get("examId").getAsInt();
                                            
                                            log(logArea, "Connecting to server (localhost:7860)...");
                                            NetworkManager.getInstance().connect("localhost", 7860);
                                            Thread.sleep(1000);
                                            
                                            submitLogic.run();
                                        } else {
                                            log(logArea, "Decrypted payload does not contain sessionId. Skipping.");
                                        }
                                    } else {
                                        log(logArea, "No payload files found in this folder. Skipping.");
                                    }
                                } catch (Exception kex) {
                                    log(logArea, "Failed to unprotect key: " + kex.getMessage());
                                }
                            }
                        }
                    } catch (Exception ex) {
                        log(logArea, "Recovery Exception: " + ex.getMessage());
                        ex.printStackTrace();
                    } finally {
                        SwingUtilities.invokeLater(() -> {
                            recoverBtn.setEnabled(true);
                            startBtn.setEnabled(true);
                        });
                    }
                }).start();
            });
            // The submit logic and recover logic are fine above.
            frame.setVisible(true);
            log(logArea, "[TSE_UI] Showing LOGIN card");
            cardLayout.show(rootPanel, "LOGIN");
        });
    }

    private static void startFlowLogic(NetworkTSEExamService service, JTextArea logArea, JLabel statusLabel, JButton startBtn, JButton retryBtn, JFrame frame, int userId, TSEExamConfig examCfg, Runnable submitLogic) {
        if (startInProgress) {
            log(logArea, "Start ignored because secure exam flow is already in progress.");
            return;
        }
        startInProgress = true;
        startBtn.setEnabled(false);
        retryBtn.setVisible(false);
        
        // Run network operations in a background thread to prevent blocking EDT!
        new Thread(() -> {
            try {
                if (!com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().isConnected()) {
                    log(logArea, "[TSE_PORTAL] Reconnecting to server (localhost:7860)...");
                    try {
                        com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().connect("localhost", 7860);
                        Thread.sleep(1000); // give it a sec to connect
                    } catch (Exception connEx) {
                        setStatus(statusLabel, "Không kết nối được máy chủ thi ở localhost:7860.");
                        log(logArea, "[TSE_PORTAL] Lỗi: Không kết nối được máy chủ thi ở localhost:7860.");
                        startInProgress = false;
                        SwingUtilities.invokeLater(() -> {
                            startBtn.setEnabled(true);
                            frame.getContentPane().setCursor(java.awt.Cursor.getDefaultCursor());
                            javax.swing.JOptionPane.showMessageDialog(frame, "Lỗi kết nối máy chủ", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        });
                        return;
                    }
                }
                
                log(logArea, "Selected Exam: " + examCfg.examTitle + " (ID: " + examCfg.examId + ")");
                log(logArea, "[TSE_FLOW] Starting secure exam flow for examId: " + examCfg.examId);
                log(logArea, "[TSE_FLOW] Sending EXAM_START_REQUEST...");
                
                // Call the API synchronously inside the background thread
                TSEStartExamResult startRes = service.verifyPasswordAndStart(userId, examCfg.examId, examCfg.requiresPassword ? "..." : "").join();
                        if (!startRes.success) {
                            log(logArea, "Start failed: " + startRes.message);
                            startInProgress = false;
                            SwingUtilities.invokeLater(() -> {
                                startBtn.setEnabled(true);
                                frame.getContentPane().setCursor(java.awt.Cursor.getDefaultCursor());
                                javax.swing.JOptionPane.showMessageDialog(frame, "Start failed: " + startRes.message, "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                            });
                            return;
                        }
                        log(logArea, "[TSE_FLOW] Received EXAM_START_RESPONSE...");
                        
                        // Zero-question check
                        if (startRes.questionCount <= 0) {
                            log(logArea, "[TSE_FLOW] Exam has zero questions. Secure Desktop will not start.");
                            startInProgress = false;
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(frame, "Kỳ thi này chưa có câu hỏi nào. Không thể tham gia.", "Lỗi Kỳ Thi", JOptionPane.ERROR_MESSAGE);
                                startBtn.setEnabled(true);
                            });
                            return;
                        }
                        currentSessionId = startRes.sessionId;
                        currentExamId = examCfg.examId;
                        setStatus(statusLabel, "Exam session created");
                        log(logArea, "Exam started. Session ID: " + currentSessionId);
                        boolean inputTestEnabled = Boolean.getBoolean("tutorhub.tse.inputTest");
                        log(logArea, "[TSE_INPUT_TEST] Parent input test enabled=" + inputTestEnabled);
                        
                        // 6. Create temp folder
                        String tempDirStr = System.getProperty("java.io.tmpdir");
                        currentSessionTempDir = Paths.get(tempDirStr, "tse_exam_" + currentSessionId);
                        Files.createDirectories(currentSessionTempDir);
                        
                        currentContextFile = currentSessionTempDir.resolve("exam_context.enc");
                        currentPayloadFile = currentSessionTempDir.resolve("submit_payload.enc");
                        
                        currentKeyB64 = CryptoUtils.generateAESKey();
                        log(logArea, "Temp keys generated.");
                        
                        RecoveryKeyStore keyStore = new WindowsDpapiRecoveryKeyStore();
                        byte[] encKey = keyStore.protectKey(currentKeyB64);
                        Files.write(currentSessionTempDir.resolve("recovery_key.enc"), encKey);
                        log(logArea, "Protected AES Session Key with DPAPI and wrote to recovery_key.enc.");
                        
                        // 7. Write context file
                        JsonObject contextObj = new JsonObject();
                        contextObj.addProperty("sessionId", currentSessionId);
                        contextObj.addProperty("examId", examCfg.examId);
                        contextObj.addProperty("examTitle", examCfg.examTitle);
                        contextObj.addProperty("durationMinutes", examCfg.durationMinutes);
                        contextObj.addProperty("htmlContent", startRes.htmlContent);
                        contextObj.addProperty("inputTestEnabled", inputTestEnabled);
                        String currentMode = TSEInputModeManager.getInstance().getMode();
                        contextObj.addProperty("inputMode", currentMode);
                        log(logArea, "[TSE_INPUT_TEST] Added inputTestEnabled=" + inputTestEnabled + " to exam context, mode=" + currentMode);
                        
                        String plainContext = new Gson().toJson(contextObj);
                        String encContext = CryptoUtils.encryptWrapper(plainContext, currentKeyB64);
                        Files.write(currentContextFile, encContext.getBytes("UTF-8"));
                        log(logArea, "Wrote encrypted exam_context.enc to " + currentContextFile.toString());
                        
                        // 8. Find absolute paths
                        String javaHome = System.getProperty("java.home");
                        Path javaExePath = Paths.get(javaHome, "bin", "java.exe");
                        if (!Files.exists(javaExePath)) {
                            javaExePath = Paths.get(javaHome, "bin", "java"); // fallback
                        }
                        
                        // Resolve JAR Path
                        Path jarPath = null;
                        String workingDirectory = System.getProperty("user.dir");
                        String sysPropJar = System.getProperty("tutorhub.app.jar");
                        Path appJar = Paths.get(workingDirectory, "app", "TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar");
                        Path targetJar = Paths.get(workingDirectory, "target", "TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar");
                        
                        if (sysPropJar != null && Files.exists(Paths.get(sysPropJar))) {
                            jarPath = Paths.get(sysPropJar);
                        } else if (Files.exists(appJar)) {
                            jarPath = appJar;
                        } else if (Files.exists(targetJar)) {
                            jarPath = targetJar;
                        } else {
                            throw new RuntimeException("Fat JAR not found. Checked: " + appJar + " and " + targetJar);
                        }
                        
                        String appRoot = System.getProperty("tutorhub.app.root", workingDirectory);
                        
                        log(logArea, "=== Absolute Paths ===");
                        log(logArea, "appRoot: " + appRoot);
                        log(logArea, "resolvedJarPath: " + jarPath.toAbsolutePath());
                        log(logArea, "javaExePath: " + javaExePath.toAbsolutePath());
                        log(logArea, "workingDirectory: " + workingDirectory);
                        log(logArea, "contextPath: " + currentContextFile.toAbsolutePath());
                        log(logArea, "outputPath: " + currentPayloadFile.toAbsolutePath());
                        
                        // 9. Spawn Rust
                        setStatus(statusLabel, "Secure Desktop running");
                        log(logArea, "[TSE_FLOW] Spawning Rust LockdownCore...");
                        log(logArea, "[TSE_PORTAL] Passing context via args and key via --key");
                        LockdownManager lockdownManager = new LockdownManager();
                        Path rustExe = lockdownManager.extractRustExe();
                        log(logArea, "rustExePath: " + rustExe.toAbsolutePath());
                        
                        String fullCmdStr = rustExe.toAbsolutePath() + " --spawn-child --java-exe \"" + javaExePath.toAbsolutePath() + "\" --jar \"" + jarPath.toAbsolutePath() + "\" --context \"" + currentContextFile.toAbsolutePath() + "\" --output \"" + currentPayloadFile.toAbsolutePath() + "\" --key \"[HIDDEN]\"";
                        log(logArea, "FULL Command: " + fullCmdStr);
                        
                        SwingUtilities.invokeLater(() -> {
                            frame.setExtendedState(JFrame.ICONIFIED);
                            log(logArea, "Parent minimized.");
                        });
                        
                        Process rustProc = lockdownManager.spawnChildOnSecureDesktop(
                            rustExe,
                            javaExePath.toAbsolutePath().toString(),
                            jarPath.toAbsolutePath().toString(),
                            currentContextFile.toAbsolutePath().toString(),
                            currentPayloadFile.toAbsolutePath().toString(),
                            currentKeyB64
                        );
                        
                        // Drain the output stream continuously to avoid deadlock
                        new Thread(() -> {
                            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(rustProc.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    System.out.println("[RUST_PROC] " + line);
                                }
                            } catch (Exception e) {}
                        }).start();

                        new Thread(() -> {
                            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(rustProc.getErrorStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    System.err.println("[RUST_PROC_ERR] " + line);
                                }
                            } catch (Exception e) {}
                        }).start();
                        
                        // 10. Wait for Rust — dynamic timeout based on exam duration
                        long waitTimeMinutes = (examCfg.durationMinutes > 0)
                                ? (long) examCfg.durationMinutes + 15
                                : 240L; // fallback: 240 min if duration unknown
                        setStatus(statusLabel, "Waiting for exam client...");
                        log(logArea, "[TSE_CLEANUP] Waiting for Rust process (timeout: " + waitTimeMinutes + " minutes)...");
                        
                        boolean exited = rustProc.waitFor(waitTimeMinutes, java.util.concurrent.TimeUnit.MINUTES);
                        if (!exited) {
                            log(logArea, "[TSE_CLEANUP] WARNING: Rust still alive after " + waitTimeMinutes + " minutes. Force terminating...");
                            rustProc.destroyForcibly();
                        }
                        int rustExitCode = exited ? rustProc.exitValue() : -1;
                        log(logArea, "[TSE_CLEANUP] Rust process exited code: " + rustExitCode);
                        
                        SwingUtilities.invokeLater(() -> {
                            frame.setExtendedState(JFrame.NORMAL);
                            frame.toFront();
                            frame.getContentPane().setCursor(java.awt.Cursor.getDefaultCursor());
                            log(logArea, "Parent restored.");
                        });
                        
                        // Force-kill Rust process tree by PID (cleans up any orphaned JCEF renderer subprocesses)
                        long rustPid = rustProc.pid();
                        log(logArea, "[TSE_CLEANUP] Checking remaining TutorHub_LockdownCore processes...");
                        try {
                            if (rustPid > 0) {
                                Runtime.getRuntime().exec(new String[]{"taskkill", "/F", "/T", "/PID", String.valueOf(rustPid)});
                                log(logArea, "[TSE_CLEANUP] Force-killed process tree for PID " + rustPid + " (cleans up JCEF renderers).");
                            }
                            Process checkProc = Runtime.getRuntime().exec("tasklist /fi \"imagename eq TutorHub_LockdownCore.exe\"");
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(checkProc.getInputStream()))) {
                                String line;
                                boolean found = false;
                                while ((line = reader.readLine()) != null) {
                                    if (line.contains("TutorHub_LockdownCore.exe")) {
                                        found = true;
                                    }
                                }
                                if (found) {
                                    log(logArea, "[TSE_CLEANUP] WARNING: TutorHub_LockdownCore.exe is STILL RUNNING in the background!");
                                    log(logArea, "[TSE_CLEANUP] Force terminating remaining process by image name...");
                                    Runtime.getRuntime().exec("taskkill /F /IM TutorHub_LockdownCore.exe");
                                } else {
                                    log(logArea, "[TSE_CLEANUP] No TutorHub_LockdownCore process remains.");
                                }
                            }
                        } catch (Exception processEx) {
                            log(logArea, "[TSE_CLEANUP] Cleanup check error: " + processEx.getMessage());
                        }
                        
                        Path autosaveFile = currentSessionTempDir.resolve("autosave_payload.enc");
                        
                        if (rustExitCode != 0) {
                            log(logArea, "WARNING: Rust/Child exited with code " + rustExitCode + ". Checking for payloads...");
                        }
                        
                        // 11. Rust returned to default desktop. Prefer FINAL payload, fallback to autosave.
                        if (Files.exists(currentPayloadFile)) {
                            setStatus(statusLabel, "Payload received");
                            log(logArea, "[TSE_PARENT] Found submit_payload.enc. Using FINAL submit payload.");
                            submitLogic.run();
                        } else if (Files.exists(autosaveFile)) {
                            setStatus(statusLabel, "Recovery available");
                            log(logArea, "[TSE_PARENT] WARNING: submit_payload.enc not found. Falling back to autosave_payload.enc.");
                            log(logArea, "Đang dùng bản lưu tạm để nộp bài...");
                            currentPayloadFile = autosaveFile;
                            submitLogic.run();
                        } else {
                            setStatus(statusLabel, "Error / Retry required");
                            log(logArea, "ERROR: No payload found (no submit_payload.enc or autosave_payload.enc)!");
                            log(logArea, "Temp folder NOT deleted: " + currentSessionTempDir.toAbsolutePath());
                            startInProgress = false;
                            SwingUtilities.invokeLater(() -> {
                                startBtn.setEnabled(true);
                                retryBtn.setVisible(false);
                            });
                            return;
                        }
                        
                    } catch (Exception ex) {
                        log(logArea, "Exception: " + ex.getMessage());
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            frame.setExtendedState(JFrame.NORMAL);
                            frame.toFront();
                        });
                    } finally {
                        startInProgress = false;
                        SwingUtilities.invokeLater(() -> startBtn.setEnabled(true));
                    }
        }).start();
    }
    
    private static void log(JTextArea area, String msg) {
        SwingUtilities.invokeLater(() -> {
            area.append(msg + "\n");
            area.setCaretPosition(area.getDocument().getLength());
            System.out.println("[ParentLab] " + msg);
        });
    }
    
    private static void setStatus(JLabel label, String status) {
        SwingUtilities.invokeLater(() -> {
            label.setText("Status: " + status);
        });
    }
}
