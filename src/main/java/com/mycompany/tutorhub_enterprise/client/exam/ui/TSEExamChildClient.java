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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.File;

public class TSEExamChildClient {

    // --- Class-level state fields (volatile for thread visibility) ---
    private static volatile boolean finalSubmitInProgress = false;
    private static volatile boolean allowProgrammaticExit = false;
    private static Timer autoSaveTimer = null; // Exposed at class level so submit handler can stop() it

    public static void main(String[] args) {
        String contextPath = null;
        String outputPath = null;
        String keyB64 = null;

        for (int i = 0; i < args.length; i++) {
            if ("--context".equals(args[i]) && i + 1 < args.length) {
                contextPath = args[i + 1];
            } else if ("--output".equals(args[i]) && i + 1 < args.length) {
                outputPath = args[i + 1];
            } else if ("--key".equals(args[i]) && i + 1 < args.length) {
                keyB64 = args[i + 1];
            }
        }

        if (contextPath == null || outputPath == null || keyB64 == null) {
            System.err.println("Usage: --context <path> --output <path> --key <base64_key>");
            System.exit(1);
        }

        System.out.println("[TSE_CHILD_BUILD] 2I.9.5D-SUBMIT-CLEANUP-FIX");
        System.out.println("[TSE_CHILD] Starting Exam Child Client...");
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
                    if (browserPanel != null) {
                        String jsOverlay = 
                            "var existing = document.getElementById('tse-submit-confirm-overlay'); " +
                            "if (existing) existing.remove(); " +
                            "var overlay = document.createElement('div'); " +
                            "overlay.id = 'tse-submit-confirm-overlay'; " +
                            "overlay.style.position = 'fixed'; " +
                            "overlay.style.inset = '0'; " +
                            "overlay.style.zIndex = '2147483647'; " +
                            "overlay.style.background = 'rgba(0,0,0,0.55)'; " +
                            "overlay.style.display = 'flex'; " +
                            "overlay.style.alignItems = 'center'; " +
                            "overlay.style.justifyContent = 'center'; " +
                            "overlay.style.fontFamily = 'sans-serif'; " +
                            "overlay.innerHTML = '<div id=\"tse-submit-confirm-card-content\" style=\"background: white; padding: 30px; border-radius: 8px; max-width: 400px; text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1);\">' + " +
                            "'<h2 style=\"margin-top: 0; color: #1e293b; font-size: 20px;\">Xác nhận nộp bài</h2>' + " +
                            "'<p style=\"color: #475569; margin-bottom: 24px; line-height: 1.5;\">Bạn có chắc chắn muốn nộp bài? Sau khi nộp, bạn không thể chỉnh sửa câu trả lời.</p>' + " +
                            "'<div style=\"display: flex; justify-content: center; gap: 16px;\">' + " +
                            "'<button id=\"tse-cancel-submit\" style=\"padding: 10px 20px; border: none; border-radius: 4px; background: #e2e8f0; color: #0f172a; font-weight: bold; cursor: pointer;\">Hủy</button>' + " +
                            "'<button id=\"tse-confirm-submit\" style=\"padding: 10px 20px; border: none; border-radius: 4px; background: #dc2626; color: white; font-weight: bold; cursor: pointer;\">Nộp bài</button>' + " +
                            "'</div></div>'; " +
                            "document.body.appendChild(overlay); " +
                            "document.getElementById('tse-cancel-submit').onclick = function() { " +
                            "  if(overlay.parentNode) overlay.parentNode.removeChild(overlay); " +
                            "  window.cefQuery && window.cefQuery({request: 'SUBMIT_PAYLOAD:CANCEL_FINAL_SUBMIT'}); " +
                            "}; " +
                            "document.getElementById('tse-confirm-submit').onclick = function() { " +
                            "  document.getElementById('tse-submit-confirm-card-content').innerHTML = '<h2 style=\"margin-top:0; color:#1e293b; font-size:20px;\">Đang nộp bài...</h2><p style=\"color:#475569;\">Vui lòng chờ trong giây lát...</p>'; " +
                            "  window.cefQuery && window.cefQuery({request: 'SUBMIT_PAYLOAD:CONFIRM_FINAL_SUBMIT'}); " +
                            "}; ";
                        browserPanel.executeJavaScript(jsOverlay);
                    }
                });
                mainContainer.add(headerBar, BorderLayout.NORTH);

                // --- Browser panel ---
                TSEBrowserPanel browserPanel = new TSEBrowserPanel();
                JPanel browserContainer = new JPanel(new BorderLayout());
                browserContainer.add(browserPanel, BorderLayout.CENTER);
                mainContainer.add(browserContainer, BorderLayout.CENTER);

                // --- Footer panel ---
                ExamFooterStatusBar footerBar = new ExamFooterStatusBar(null); // null disables power button exit
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
                browserPanel.loadHtml(wrappedHtml);

                // JCEF submit callback — routes to FINAL or AUTOSAVE based on state flag
                TSEJcefLifecycleManager.setSubmitCallback(payload -> {
                    if ("CANCEL_FINAL_SUBMIT".equals(payload)) {
                        System.out.println("[TSE_CHILD] Final submit cancelled by user.");
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
                        return;
                    }

                    System.out.println("[TSE_CHILD] Received payload from JS. finalSubmitInProgress=" + finalSubmitInProgress);
                    if (finalSubmitInProgress) {
                        writeFinalPayloadAndExit(finalOutputPath, sessionId, examId, payload, frame, finalKeyB64);
                    } else {
                        writeAutosavePayload(finalOutputPath, sessionId, examId, payload, finalKeyB64);
                    }
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
                                                  String payloadJson, JFrame frame, String keyB64) {
        try {
            if (payloadJson == null || payloadJson.trim().isEmpty() || payloadJson.contains("\"error\"")) {
                System.err.println("[TSE_CHILD] FINAL submit: invalid payload from JS: " + payloadJson);
                JOptionPane.showMessageDialog(frame,
                        "Không thể thu thập bài làm (Lỗi kịch bản web).",
                        "Lỗi nộp bài", JOptionPane.ERROR_MESSAGE);
                // Allow retry: reset state and re-start timer
                finalSubmitInProgress = false;
                if (autoSaveTimer != null) autoSaveTimer.start();
                SwingUtilities.invokeLater(() -> {
                    if (frame.getGlassPane() != null) frame.getGlassPane().setVisible(false);
                });
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
                    try {
                        TSEJcefLifecycleManager.cleanup();
                    } catch (Exception ex) {
                        System.out.println("[TSE_CHILD] JCEF cleanup failed/ignored: " + ex.getMessage());
                    }
                    Thread.sleep(2500); // allow JCEF subprocesses to terminate
                } catch (InterruptedException ignored) {
                }
                System.out.println("[TSE_CHILD] Final submit complete. Exiting child process.");
                Runtime.getRuntime().halt(0);
            }, "tse-child-fast-exit").start();

            // EDT: dispose frame only — no JCEF work, no System.exit here
            SwingUtilities.invokeLater(() -> {
                try { frame.dispose(); } catch (Exception ignored) {}
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[TSE_CHILD] FINAL submit exception: " + e.getMessage());
            JOptionPane.showMessageDialog(frame,
                    "Lỗi khi lưu file nộp bài: " + e.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            // Allow retry
            finalSubmitInProgress = false;
            SwingUtilities.invokeLater(() -> {
                if (frame.getGlassPane() != null) frame.getGlassPane().setVisible(false);
            });
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
}
