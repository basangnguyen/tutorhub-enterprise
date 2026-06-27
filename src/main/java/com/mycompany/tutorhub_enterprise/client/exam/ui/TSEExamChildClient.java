package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.File;

public class TSEExamChildClient {

    // --- Class-level state fields (volatile for thread visibility) ---
    private static volatile boolean finalSubmitInProgress = false;
    private static volatile boolean allowProgrammaticExit = false;
    private static Timer autoSaveTimer = null; // Exposed at class level so submit handler can stop() it

    private static String loadClasspathTextResource(String resourcePath) {
        try (java.io.InputStream in = TSEExamChildClient.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.out.println("[TSE_TRAY_DOM] Resource not found on classpath: " + resourcePath);
                return "";
            }
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("[TSE_TRAY_DOM] Failed to load classpath resource " + resourcePath + ": " + e.getMessage());
            return "";
        }
    }

    public static void main(String[] args) {
        System.setProperty("tse.process.type", "child");
        
        TSEChildLaunchArgs parsedArgs = TSEChildLaunchArgsParser.parse(args);
        
        if (parsedArgs.getMode() == TSEChildLaunchArgs.Mode.INVALID) {
            System.err.println(parsedArgs.getErrorMessage());
            System.exit(1);
        }
        
        if (parsedArgs.getMode() == TSEChildLaunchArgs.Mode.V2_DEBUG) {
            System.out.println("[TSE_CHILD] V2 Boot Recognized. Entering Debug Skeleton Mode.");
            launchV2DebugSkeleton(parsedArgs);
            return;
        }

        // --- LEGACY MODE ---
        String contextPath = parsedArgs.getLegacyContextPath();
        String outputPath = parsedArgs.getLegacyOutputPath();
        String keyB64 = parsedArgs.getLegacyKeyBase64();

        System.out.println("[TSE_CHILD_BUILD] 2I.9.5D-SUBMIT-CLEANUP-FIX");
        System.out.println("[TSE_CHILD] Starting Exam Child Client (Legacy Mode)...");
        System.out.println("[TSE_CHILD] Context path (enc): " + contextPath);
        System.out.println("[TSE_CHILD] Output path (enc): " + outputPath);

        final String finalOutputPath = outputPath;
        final String finalKeyB64 = keyB64;

        try {
            String encContext = new String(Files.readAllBytes(Paths.get(contextPath)), "UTF-8");
            String jsonContext = CryptoUtils.decryptWrapper(encContext, keyB64);
            System.out.println("[TSE_CHILD] Successfully decrypted exam context.");

            Gson gson = new Gson();
            JsonObject contextObj = gson.fromJson(jsonContext, JsonObject.class);

            final String sessionId   = contextObj.has("sessionId")  ? contextObj.get("sessionId").getAsString()  : "";
            final int    examId      = contextObj.has("examId")      ? contextObj.get("examId").getAsInt()        : 0;
            final String htmlContent = contextObj.has("htmlContent") ? contextObj.get("htmlContent").getAsString() : "";
            final String examTitle   = contextObj.has("examTitle")   ? contextObj.get("examTitle").getAsString()  : "Exam";
            final String userId      = contextObj.has("userId")      ? contextObj.get("userId").getAsString()     : "";
            final String serverStatus = contextObj.has("serverStatus") ? contextObj.get("serverStatus").getAsString() : "";
            final boolean inputTestEnabled = readBoolean(contextObj, "inputTestEnabled", false);
            final String inputModeFromParent = contextObj.has("inputMode") ? contextObj.get("inputMode").getAsString() : "en";
            final String buildVersion = resolveBuildVersion();
            System.out.println("[TSE_INPUT_TEST] Child input test enabled=" + inputTestEnabled);
            if (inputTestEnabled) {
                System.out.println("[TSE_INPUT_TEST] Enabled from exam context.");
            } else {
                System.out.println("[TSE_INPUT_TEST] Disabled.");
            }

            SwingUtilities.invokeLater(() -> {
                try { FlatLightLaf.setup(); } catch (Exception ignored) {}

                JFrame frame = new JFrame("TutorHub Secure Exam Client - " + examTitle);
                frame.setUndecorated(true);
                frame.setResizable(false);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setAlwaysOnTop(true);

                GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                try {
                    gd.setFullScreenWindow(frame);
                } catch (Exception e) {
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }

                JPanel mainContainer = new JPanel(new BorderLayout());
                TSELanguageManager languageManager = new TSELanguageManager();
                TSEInputModeManager inputModeManager = TSEInputModeManager.getInstance();
                inputModeManager.setMode(inputModeFromParent);

                // --- Submit button handler (runs on EDT) ---
                ExamHeaderBar headerBar = new ExamHeaderBar(examTitle, () -> {
                    System.out.println("[TSE_CHILD] Submit button clicked.");

                    // Guard: prevent double-submit
                    if (finalSubmitInProgress) {
                        System.out.println("[TSE_CHILD] Submit already in progress. Ignored.");
                        return;
                    }

                    System.out.println("[TSE_CHILD] Showing custom submit confirmation overlay.");
                    
                    TSEBrowserPanel browserPanel = (TSEBrowserPanel) frame.getRootPane().getClientProperty("browserPanel");
                    TSEControlPanelOverlay.showSubmitConfirm(browserPanel, languageManager);
                }, () -> {
                    System.out.println("[TSE_CONTROL] About clicked.");
                    TSEControlPanelOverlay.showAbout(
                            getBrowserPanel(frame),
                            languageManager,
                            buildVersion,
                            String.valueOf(examId),
                            sessionId,
                            userId,
                            serverStatus);
                }, () -> {
                    System.out.println("[TSE_CONTROL] Refresh clicked.");
                    if (finalSubmitInProgress) {
                        System.out.println("[TSE_REFRESH] Refresh blocked: final submit in progress.");
                        return;
                    }
                    TSEBrowserPanel browserPanel = getBrowserPanel(frame);
                    if (browserPanel != null) {
                        System.out.println("[TSE_REFRESH] Timer is managed by Java/Server. Not affected by JCEF reload.");
                        browserPanel.executeJavaScript("if (window.TSESafeRefresh) { window.TSESafeRefresh.triggerSnapshotAndReload(); } else { console.error('[TSE_REFRESH] TSESafeRefresh not found!'); }");
                    }
                });
                headerBar.applyLanguage(languageManager);
                mainContainer.add(headerBar, BorderLayout.NORTH);

                // --- Browser panel ---
                TSEBrowserPanel browserPanel = new TSEBrowserPanel();
                JPanel browserContainer = new JPanel(new BorderLayout());
                browserContainer.add(browserPanel, BorderLayout.CENTER);
                mainContainer.add(browserContainer, BorderLayout.CENTER);

                // --- Footer panel ---
                final ExamFooterStatusBar[] footerRef = new ExamFooterStatusBar[1];
                ExamFooterStatusBar footerBar = new ExamFooterStatusBar(inputModeManager.getFooterLabel(), languageAnchor -> {
                    System.out.println("[TSE_INPUT] Language flyout requested.");
                    TSELanguageSwitcherManager.showLanguageSwitcherDOM(languageAnchor, getBrowserPanel(frame), inputModeManager.getMode());
                }, (source, quickSettingsAnchor) -> {
                    System.out.println("[TSE_FOOTER] QuickSettings click handler attached=true");
                    System.out.println("[TSE_QUICK_SETTINGS] Flyout requested from " + source);
                    TSEQuickSettingsManager.showQuickSettingsDOM(quickSettingsAnchor, getBrowserPanel(frame), inputTestEnabled);
                }, () -> {
                    System.out.println("[TSE_CONTROL] Exit clicked.");
                    System.out.println("[TSE_CONTROL] Exit requested: blocked");
                    TSEControlPanelOverlay.showExitBlocked(getBrowserPanel(frame), languageManager);
                });
                footerRef[0] = footerBar;
                footerBar.applyInputMode(inputModeManager, languageManager);
                mainContainer.add(footerBar, BorderLayout.SOUTH);

                frame.revalidate();
                frame.repaint();

                // Inject JS polyfill for collectTSEAnswers if missing in HTML
                String wrappedHtml = htmlContent;
                if (!htmlContent.contains("collectTSEAnswers")) {
                    wrappedHtml = htmlContent.replace("</body>",
                            "<script>function collectTSEAnswers() { " +
                            "  window.cefQuery && window.cefQuery({request: 'SUBMIT_PAYLOAD:' + " +
                            "  JSON.stringify({ answers: [{questionId: 1, answerIds: [2]}] })});" +
                            "}</script></body>");
                }
                
                try {
                    String trayScript = loadClasspathTextResource("tse/tse-tray-flyout.js");
                    if (!trayScript.isEmpty()) {
                        System.out.println("[TSE_TRAY_DOM] Loaded tse-tray-flyout.js from classpath, length=" + trayScript.length());
                        wrappedHtml = wrappedHtml.replace("</body>", "<script>\n" + trayScript + "\n</script>\n</body>");
                        System.out.println("[TSE_TRAY_DOM] Injected tray flyout JS into exam HTML.");
                    }
                } catch (Exception e) {
                    System.err.println("[TSE_TRAY_DOM] Failed to load tse-tray-flyout.js: " + e.getMessage());
                }

                try {
                    String refreshScript = loadClasspathTextResource("tse/tse-safe-refresh.js");
                    if (!refreshScript.isEmpty()) {
                        System.out.println("[TSE_REFRESH] Loaded tse-safe-refresh.js from classpath, length=" + refreshScript.length());
                        wrappedHtml = wrappedHtml.replace("</body>", "<script>\n" + refreshScript + "\n</script>\n</body>");
                        System.out.println("[TSE_REFRESH] Injected safe refresh JS into exam HTML.");
                    }
                } catch (Exception e) {
                    System.err.println("[TSE_REFRESH] Failed to load tse-safe-refresh.js: " + e.getMessage());
                }

                if (inputTestEnabled) {
                    System.out.println("[TSE_INPUT_TEST] Injecting input test panel into exam HTML");
                }
                wrappedHtml = inputModeManager.injectInputTestPanelIfEnabled(wrappedHtml, inputTestEnabled);
                wrappedHtml = inputModeManager.injectEngineIntoHtml(wrappedHtml);
                browserPanel.loadHtml(wrappedHtml);

                // JCEF submit callback — routes to FINAL or AUTOSAVE based on state flag
                TSEJcefLifecycleManager.setSubmitCallback((payload, cb) -> {
                    if ("CANCEL_FINAL_SUBMIT".equals(payload)) {
                        System.out.println("[TSE_CHILD] Final submit cancelled by user.");
                        cb.success("OK");
                        return;
                    }
                    if ("CONFIRM_FINAL_SUBMIT".equals(payload)) {
                        finalSubmitInProgress = true;
                        allowProgrammaticExit = true;
                        System.out.println("[TSE_CHILD] Final submit mode enabled.");

                        if (autoSaveTimer != null) {
                            autoSaveTimer.stop();
                            System.out.println("[TSE_CHILD] Auto-save timer stopped for final submit.");
                        }

                        System.out.println("[TSE_CHILD] Showing submit overlay.");
                        System.out.println("[TSE_CHILD] Calling collectTSEAnswers() for FINAL...");
                        collectAnswersForFinalSubmit(frame);
                        cb.success("OK");
                        return;
                    }
                    
                    if (payload != null && payload.startsWith("TSE_LANG_SELECT:")) {
                        String modeId = payload.substring("TSE_LANG_SELECT:".length());
                        inputModeManager.setMode(modeId);
                        System.out.println("[TSE_INPUT] Language changed to: " + modeId);
                        System.out.println("[TSE_INPUT] Java mode updated: " + inputModeManager.getMode());
                        
                        inputModeManager.applyMode(getBrowserPanel(frame));
                        if (footerRef[0] != null) {
                            footerRef[0].applyInputMode(inputModeManager, languageManager);
                        }
                        System.out.println("[TSE_INPUT] Footer label updated: " + inputModeManager.getFooterLabel());
                        cb.success("OK");
                        return;
                    }
                    if (payload != null && payload.equals("TSE_WIFI_REFRESH")) {
                        TSEQuickSettingsManager.refreshWifi(getBrowserPanel(frame)); 
                        cb.success("OK");
                        return;
                    }

                    System.out.println("[TSE_CHILD] Received payload from JS. finalSubmitInProgress=" + finalSubmitInProgress);
                    if (finalSubmitInProgress) {
                        writeFinalPayloadAndExit(finalOutputPath, sessionId, examId, payload, frame, finalKeyB64, languageManager, footerRef);
                    } else {
                        writeAutosavePayload(finalOutputPath, sessionId, examId, payload, finalKeyB64);
                    }
                    cb.success("OK");
                });

                // WindowClosing: block user close; only allow if programmatic exit
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        if (!allowProgrammaticExit) {
                            System.out.println("[TSE_CHILD] Window closing blocked by Secure Exam Client.");
                        }
                        // If allowProgrammaticExit=true, frame.dispose() was already called by fast-exit thread
                    }
                });

                frame.add(mainContainer);
                frame.setVisible(true);

                // Store browserPanel for access by static collect methods
                frame.getRootPane().putClientProperty("browserPanel", browserPanel);

                // Auto-save timer (every 15s) — class-level field so submit handler can stop() it
                autoSaveTimer = new Timer(15000, e -> {
                    if (finalSubmitInProgress) {
                        // Guard: do not run autosave during or after final submit
                        System.out.println("[TSE_CHILD] AutoSave skipped: final submit already in progress.");
                        return;
                    }
                    System.out.println("[TSE_CHILD] Triggering Auto-Save...");
                    collectAnswersForAutosave(frame);
                });
                autoSaveTimer.setInitialDelay(15000);
                autoSaveTimer.start();
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[TSE_CHILD] Failed to start client: " + e.getMessage());
            System.exit(1);
        }
    }

    // Trigger JS answer collection for FINAL submit (routes to writeFinalPayloadAndExit via callback)
    private static void collectAnswersForFinalSubmit(JFrame frame) {
        TSEBrowserPanel browserPanel = (TSEBrowserPanel) frame.getRootPane().getClientProperty("browserPanel");
        if (browserPanel != null) {
            System.out.println("[TSE_CHILD] Calling collectTSEAnswers() for FINAL...");
            browserPanel.executeJavaScript(
                    "if (typeof collectTSEAnswers === 'function') { collectTSEAnswers(); } " +
                    "else { window.cefQuery && window.cefQuery(" +
                    "  {request: 'SUBMIT_PAYLOAD:{\"error\":\"no_script\"}'}); }");
        }
    }

    // Trigger JS answer collection for AUTOSAVE (routes to writeAutosavePayload via callback)
    private static void collectAnswersForAutosave(JFrame frame) {
        TSEBrowserPanel browserPanel = (TSEBrowserPanel) frame.getRootPane().getClientProperty("browserPanel");
        if (browserPanel != null) {
            System.out.println("[TSE_CHILD] Calling collectTSEAnswers() for AUTOSAVE...");
            browserPanel.executeJavaScript(
                    "if (typeof collectTSEAnswers === 'function') { collectTSEAnswers(); } " +
                    "else { window.cefQuery && window.cefQuery(" +
                    "  {request: 'SUBMIT_PAYLOAD:{\"error\":\"no_script\"}'}); }");
        }
    }

    /**
     * FINAL SUBMIT path:
     * Writes submit_payload.enc atomically (tmp → rename), then exits JVM fast via a single
     * background thread. Does NOT call System.exit() to avoid JCEF shutdown hook blocking EDT.
     */
    private static void writeFinalPayloadAndExit(String outputPath, String sessionId, int examId,
                                                  String payloadJson, JFrame frame, String keyB64,
                                                  TSELanguageManager languageManager,
                                                  ExamFooterStatusBar[] footerRef) {
        try {
            if (payloadJson == null || payloadJson.trim().isEmpty() || payloadJson.contains("\"error\"")) {
                System.err.println("[TSE_CHILD] FINAL submit: invalid payload from JS: " + payloadJson);
                TSEControlPanelOverlay.showError(
                        getBrowserPanel(frame),
                        languageManager.text("error.submit.title"),
                        languageManager.text("error.submit.script"),
                        languageManager.text("close"));
                // Allow retry: reset state and re-start timer
                finalSubmitInProgress = false;
                if (autoSaveTimer != null) autoSaveTimer.start();
                return;
            }

            JsonObject finalPayload = buildPayloadObject(payloadJson, sessionId, examId);
            String encPayload = CryptoUtils.encryptWrapper(finalPayload.toString(), keyB64);

            // Atomic write: .tmp → submit_payload.enc
            File tmpFile = new File(outputPath + ".tmp");
            File outFile = new File(outputPath); // submit_payload.enc

            System.out.println("[TSE_CHILD] Writing FINAL submit payload...");
            Files.write(tmpFile.toPath(), encPayload.getBytes("UTF-8"));
            Files.move(tmpFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[TSE_CHILD] FINAL submit payload written to submit_payload.enc.");

            // Allow programmatic frame close
            allowProgrammaticExit = true;

            // --- Single fast-exit background thread ---
            // JCEF cleanup runs here (off EDT) to avoid blocking the Swing thread.
            // Runtime.halt(0) is the sole JVM exit call — no race with System.exit().
            new Thread(() -> {
                try {
                    Thread.sleep(500); // brief pause so EDT can dispose frame
                    System.out.println("[TSE_EXIT] Final payload written. Preparing child exit.");
                    
                    Thread.getAllStackTraces().keySet().stream()
                        .filter(t -> t.isAlive() && !t.isDaemon())
                        .forEach(t -> System.out.println("[TSE_EXIT] Non-daemon thread alive: " + t.getName() + " state=" + t.getState()));
                    
                    try {
                        System.out.println("[TSE_EXIT] Calling quickSettingsManager.shutdownNowNoBlock().");
                        TSEQuickSettingsManager.shutdownNowNoBlock();
                        if (footerRef[0] != null) {
                            footerRef[0].stopStatusPolling();
                        }
                        System.out.println("[TSE_EXIT] quickSettingsManager.shutdownNowNoBlock() returned.");
                    } catch (Exception ex) {
                        System.out.println("[TSE_EXIT] QuickSettings shutdown failed/ignored: " + ex.getMessage());
                    }
                    try {
                        System.out.println("[TSE_EXIT] Calling JCEF shutdown.");
                        Thread jcefThread = new Thread(() -> TSEJcefLifecycleManager.cleanup());
                        jcefThread.start();
                        jcefThread.join(1000); // Wait max 1s
                        System.out.println("[TSE_EXIT] JCEF shutdown returned/timeout.");
                    } catch (Exception ex) {
                        System.out.println("[TSE_EXIT] JCEF cleanup failed/ignored: " + ex.getMessage());
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    System.out.println("[TSE_EXIT] Calling Runtime.halt(0) NOW.");
                    System.out.println("Child process exited naturally with code 0.");
                    Runtime.getRuntime().halt(0);
                }
            }, "tse-child-fast-exit").start();

            // EDT: dispose frame only — no JCEF work, no System.exit here
            SwingUtilities.invokeLater(() -> {
                try { frame.dispose(); } catch (Exception ignored) {}
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[TSE_CHILD] FINAL submit exception: " + e.getMessage());
            TSEControlPanelOverlay.showError(
                    getBrowserPanel(frame),
                    languageManager.text("error.submit.title"),
                    languageManager.text("error.submit.save") + ": " + e.getMessage(),
                    languageManager.text("close"));
            // Allow retry
            finalSubmitInProgress = false;
        }
    }

    /**
     * AUTOSAVE path:
     * Writes autosave_payload.enc atomically (tmp → rename). Does NOT exit.
     */
    private static void writeAutosavePayload(String outputPath, String sessionId, int examId,
                                              String payloadJson, String keyB64) {
        try {
            if (payloadJson == null || payloadJson.trim().isEmpty() || payloadJson.contains("\"error\"")) {
                System.out.println("[TSE_CHILD] AUTOSAVE: skipping invalid payload.");
                return;
            }

            JsonObject savePayload = buildPayloadObject(payloadJson, sessionId, examId);
            // autosave_payload.enc is sibling of submit_payload.enc
            String autosavePath = outputPath.replace("submit_payload", "autosave_payload");
            String encPayload = CryptoUtils.encryptWrapper(savePayload.toString(), keyB64);

            File tmpFile = new File(autosavePath + ".tmp");
            File outFile = new File(autosavePath); // autosave_payload.enc

            System.out.println("[TSE_CHILD] Writing AUTOSAVE payload...");
            Files.write(tmpFile.toPath(), encPayload.getBytes("UTF-8"));
            Files.move(tmpFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[TSE_CHILD] AUTOSAVE payload written to autosave_payload.enc.");

        } catch (Exception e) {
            System.err.println("[TSE_CHILD] AUTOSAVE exception: " + e.getMessage());
        }
    }

    private static void launchV2DebugSkeleton(TSEChildLaunchArgs args) {
        SwingUtilities.invokeLater(() -> {
            try { FlatLightLaf.setup(); } catch (Exception ignored) {}

            JFrame frame = new JFrame("TutorHub Secure Exam Client - V2 DEBUG");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            TSEV2ChildDebugLoadResult result = TSEV2ChildDebugLoader.load(args);
            
            if (result.isSuccess() && result.getRenderModel() != null) {
                V2DebugDraftContext draftContext = createV2DebugDraftContext(result.getRenderModel());
                JTabbedPane tabbedPane = new JTabbedPane();
                tabbedPane.addTab("Safe Summary", new TSEV2ChildDebugSummaryPanel(result));
                tabbedPane.addTab("Selection Prototype", new TSEV2ReadOnlyExamPanel(
                        result.getRenderModel(),
                        new TSEV2AnswerSelectionState(result.getRenderModel().getQuestionCount()),
                        draftContext.autosaveHandler,
                        draftContext.restoreHandler
                ));
                frame.add(tabbedPane);
            } else {
                frame.add(new TSEV2ChildDebugSummaryPanel(result));
            }
            
            frame.setVisible(true);
        });
    }

    private static V2DebugDraftContext createV2DebugDraftContext(
            com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel renderModel
    ) {
        try {
            TSEV2AnswerDraftSnapshotService snapshotService = new TSEV2AnswerDraftSnapshotService();
            TSEV2LocalEncryptedDraftAutosaveService autosaveService =
                    new TSEV2LocalEncryptedDraftAutosaveService(snapshotService, new java.security.SecureRandom());
            javax.crypto.SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
            Path autosaveDir = autosaveService.resolveDefaultAutosaveDir(renderModel);

            TSEV2ReadOnlyExamPanel.DraftAutosaveHandler autosaveHandler = (model, state) -> {
                TSEV2AnswerDraftSnapshot snapshot = snapshotService.createSnapshot(model, state);
                autosaveService.saveEncryptedDraft(snapshot, draftKey, autosaveDir);
            };

            TSEV2ReadOnlyExamPanel.DraftRestoreHandler restoreHandler = (model, state) -> {
                java.util.Optional<TSEV2AnswerDraftSnapshot> restored =
                        autosaveService.tryLoadEncryptedDraft(
                                autosaveDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.ENC_FILE_NAME),
                                autosaveDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.META_FILE_NAME),
                                draftKey
                        );
                if (restored.isEmpty()) {
                    return TSEV2ReadOnlyExamPanel.DraftRestoreResult.notFound();
                }
                autosaveService.applySnapshotToSelectionState(restored.get(), model, state);
                return TSEV2ReadOnlyExamPanel.DraftRestoreResult.restored();
            };

            return new V2DebugDraftContext(autosaveHandler, restoreHandler);
        } catch (Exception ex) {
            System.out.println("[TSE_V2_DRAFT] Debug encrypted draft disabled: " + ex.getClass().getSimpleName());
            return V2DebugDraftContext.disabled();
        }
    }

    private static final class V2DebugDraftContext {
        private final TSEV2ReadOnlyExamPanel.DraftAutosaveHandler autosaveHandler;
        private final TSEV2ReadOnlyExamPanel.DraftRestoreHandler restoreHandler;

        private V2DebugDraftContext(
                TSEV2ReadOnlyExamPanel.DraftAutosaveHandler autosaveHandler,
                TSEV2ReadOnlyExamPanel.DraftRestoreHandler restoreHandler
        ) {
            this.autosaveHandler = autosaveHandler;
            this.restoreHandler = restoreHandler;
        }

        private static V2DebugDraftContext disabled() {
            return new V2DebugDraftContext(null, null);
        }
    }

    /**
     * Build the standard payload JSON object from raw JS payload string.
     */
    private static JsonObject buildPayloadObject(String payloadJson, String sessionId, int examId) {
        Gson gson = new Gson();
        JsonObject payloadObj;
        try {
            payloadObj = gson.fromJson(payloadJson, JsonObject.class);
        } catch (Exception e) {
            payloadObj = new JsonObject();
            payloadObj.addProperty("raw", payloadJson);
        }

        JsonObject result = new JsonObject();
        result.addProperty("sessionId", sessionId);
        result.addProperty("examId", examId);
        if (payloadObj.has("answers")) {
            result.add("answers", payloadObj.get("answers"));
        } else {
            result.addProperty("rawPayload", payloadJson);
        }
        return result;
    }

    private static TSEBrowserPanel getBrowserPanel(JFrame frame) {
        if (frame == null || frame.getRootPane() == null) {
            return null;
        }
        Object value = frame.getRootPane().getClientProperty("browserPanel");
        return value instanceof TSEBrowserPanel ? (TSEBrowserPanel) value : null;
    }

    private static String resolveBuildVersion() {
        String version = TSEExamChildClient.class.getPackage().getImplementationVersion();
        if (version == null || version.trim().isEmpty()) {
            version = System.getProperty("tutorhub.build.version", "dev-local");
        }
        return version;
    }

    private static boolean readBoolean(JsonObject obj, String key, boolean defaultValue) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
