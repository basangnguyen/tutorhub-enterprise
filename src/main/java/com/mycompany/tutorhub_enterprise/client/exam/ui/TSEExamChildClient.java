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
    private static boolean isFinalSubmit = false;

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

        System.out.println("[TSE_CHILD_BUILD] 2I.9.5C-SUBMIT-FIX-v2");
        System.out.println("[TSE_CHILD] Starting Exam Child Client...");
        System.out.println("[TSE_CHILD] Context path (enc): " + contextPath);
        System.out.println("[TSE_CHILD] Output path (enc): " + outputPath);

        String finalOutputPath = outputPath;
        String finalKeyB64 = keyB64;

        try {
            String encContext = new String(Files.readAllBytes(Paths.get(contextPath)), "UTF-8");
            String jsonContext = CryptoUtils.decryptWrapper(encContext, keyB64);
            System.out.println("[TSE_CHILD] Successfully decrypted exam context.");
            
            Gson gson = new Gson();
            JsonObject contextObj = gson.fromJson(jsonContext, JsonObject.class);

            String sessionId = contextObj.has("sessionId") ? contextObj.get("sessionId").getAsString() : "";
            int examId = contextObj.has("examId") ? contextObj.get("examId").getAsInt() : 0;
            String htmlContent = contextObj.has("htmlContent") ? contextObj.get("htmlContent").getAsString() : "";
            String examTitle = contextObj.has("examTitle") ? contextObj.get("examTitle").getAsString() : "Exam";

            SwingUtilities.invokeLater(() -> {
                try {
                    FlatLightLaf.setup();
                } catch (Exception ignored) {}

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

                ExamHeaderBar headerBar = new ExamHeaderBar(examTitle, () -> {
                    System.out.println("[TSE_CHILD] Submit button clicked.");
                    Object[] options = {"Nộp bài", "Quay lại"};
                    int confirm = JOptionPane.showOptionDialog(frame,
                            "Bạn có chắc chắn muốn nộp bài không?",
                            "Xác nhận nộp bài",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[1]);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        isFinalSubmit = true;
                        
                        System.out.println("[TSE_CHILD] Confirmation accepted.");
                        System.out.println("[TSE_CHILD] Showing submit overlay.");
                        
                        JPanel overlay = new JPanel(new GridBagLayout());
                        overlay.setOpaque(true);
                        overlay.setBackground(new Color(30, 41, 59)); // or (245,245,245)
                        JLabel lbl = new JLabel("Đang nộp bài, vui lòng chờ...");
                        lbl.setFont(new Font("Segoe UI", Font.BOLD, 32));
                        lbl.setForeground(Color.WHITE);
                        overlay.add(lbl);
                        
                        // Block events
                        overlay.addMouseListener(new java.awt.event.MouseAdapter() {});
                        overlay.addKeyListener(new java.awt.event.KeyAdapter() {});
                        
                        frame.setGlassPane(overlay);
                        overlay.setVisible(true);

                        System.out.println("[TSE_CHILD] Collecting answers...");
                        submitExam(finalOutputPath, sessionId, examId, frame, finalKeyB64);
                    }
                });
                mainContainer.add(headerBar, BorderLayout.NORTH);

                TSEBrowserPanel browserPanel = new TSEBrowserPanel();
                JPanel browserContainer = new JPanel(new BorderLayout());
                browserContainer.add(browserPanel, BorderLayout.CENTER);
                mainContainer.add(browserContainer, BorderLayout.CENTER);
                
                // No bottom bar to avoid heavyweight overlapping black screen
                // Revalidate and repaint
                frame.revalidate();
                frame.repaint();
                
                // Inject JS polyfill for collectTSEAnswers if it's missing in HTML, just to be safe
                String wrappedHtml = htmlContent;
                if (!htmlContent.contains("collectTSEAnswers")) {
                    wrappedHtml = htmlContent.replace("</body>", 
                        "<script>function collectTSEAnswers() { " +
                        "  window.cefQuery && window.cefQuery({request: 'SUBMIT_PAYLOAD:' + JSON.stringify({ answers: [{questionId: 1, answerIds: [2]}] })});" +
                        "}</script></body>");
                }
                browserPanel.loadHtml(wrappedHtml);

                // Set submit callback for JCEF via LifecycleManager
                TSEJcefLifecycleManager.setSubmitCallback(payload -> {
                    System.out.println("[TSE_CHILD] Received payload from JS.");
                    writePayloadAndExit(finalOutputPath, sessionId, examId, payload, frame, finalKeyB64);
                });

                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        System.out.println("[TSE_CHILD] Window closing blocked by Secure Exam Client.");
                    }
                });

                frame.add(mainContainer);
                frame.setVisible(true);

                // Keep reference to browserPanel globally for submitExam()
                frame.getRootPane().putClientProperty("browserPanel", browserPanel);

                // Auto-save timer (every 15s)
                Timer autoSaveTimer = new Timer(15000, e -> {
                    if (!isFinalSubmit) {
                        System.out.println("[TSE_CHILD] Triggering Auto-Save...");
                        submitExam(finalOutputPath, sessionId, examId, frame, finalKeyB64);
                    }
                });
                autoSaveTimer.start();
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[TSE_CHILD] Failed to start client: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void submitExam(String outputPath, String sessionId, int examId, JFrame frame, String keyB64) {
        TSEBrowserPanel browserPanel = (TSEBrowserPanel) frame.getRootPane().getClientProperty("browserPanel");
        if (browserPanel != null) {
            System.out.println("[TSE_CHILD] Calling collectTSEAnswers()...");
            browserPanel.executeJavaScript("if (typeof collectTSEAnswers === 'function') { collectTSEAnswers(); } else { window.cefQuery && window.cefQuery({request: 'SUBMIT_PAYLOAD:{\"error\":\"no_script\"}'}); }");
        }
    }

    private static void writePayloadAndExit(String outputPath, String sessionId, int examId, String payloadJson, JFrame frame, String keyB64) {
        try {
            // Check if payload is valid
            if (payloadJson == null || payloadJson.trim().isEmpty() || payloadJson.contains("\"error\"")) {
                JOptionPane.showMessageDialog(frame, "Không thể thu thập bài làm (Lỗi kịch bản web).", "Lỗi nộp bài", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String targetPath = isFinalSubmit ? outputPath : outputPath.replace("submit_payload", "autosave_payload");
            File tmpFile = new File(targetPath + ".tmp");
            File outFile = new File(targetPath);

            Gson gson = new Gson();
            JsonObject payloadObj;
            try {
                payloadObj = gson.fromJson(payloadJson, JsonObject.class);
            } catch (Exception e) {
                payloadObj = new JsonObject();
                payloadObj.addProperty("raw", payloadJson);
            }

            JsonObject finalPayload = new JsonObject();
            finalPayload.addProperty("sessionId", sessionId);
            finalPayload.addProperty("examId", examId);
            if (payloadObj.has("answers")) {
                finalPayload.add("answers", payloadObj.get("answers"));
            } else {
                finalPayload.addProperty("rawPayload", payloadJson);
            }

            String finalPayloadStr = finalPayload.toString();
            String encPayload = CryptoUtils.encryptWrapper(finalPayloadStr, keyB64);
            
            System.out.println("[TSE_CHILD] Writing submit payload...");
            Files.write(tmpFile.toPath(), encPayload.getBytes("UTF-8"));
            
            // Atomic rename
            Files.move(tmpFile.toPath(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[TSE_CHILD] Submit payload written.");

            if (isFinalSubmit) {
                System.out.println("[TSE_CHILD] Disposing frame...");
                
                // Fallback exit if JVM doesn't shut down gracefully
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    System.out.println("[TSE_CHILD] Exiting JVM now (Runtime.halt).");
                    Runtime.getRuntime().halt(0);
                }).start();

                SwingUtilities.invokeLater(() -> {
                    try {
                        TSEJcefLifecycleManager.cleanup();
                    } catch (Exception ignored) {}
                    frame.dispose();
                    System.out.println("[TSE_CHILD] Exiting JVM now.");
                    System.exit(0);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Lỗi khi lưu file nộp bài: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
