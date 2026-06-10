package com.mycompany.tutorhub_enterprise.client.exam;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.client.JcefManager;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Map;

public class ExamTakingPanel extends JPanel {
    private int examId;
    private String title;
    private int duration;
    private int currentUserId;
    private NetworkManager networkManager;
    private CefBrowser browser;
    private Timer autoSaveTimer;

    public ExamTakingPanel(int examId, String title, int duration, int currentUserId, NetworkManager networkManager) {
        this.examId = examId;
        this.title = title;
        this.duration = duration;
        this.currentUserId = currentUserId;
        this.networkManager = networkManager;
        
        setLayout(new BorderLayout());
        
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("Kỳ thi: " + title + " | Thời gian: " + duration + " phút"), BorderLayout.WEST);
        
        JButton submitBtn = new JButton("Nộp Bài");
        submitBtn.addActionListener(e -> submitExam());
        header.add(submitBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        
        // JCEF Browser for Exam Content
        try {
            String html = "<html><body style='font-family:sans-serif; padding:20px;'>" +
                          "<h2>" + title + "</h2>" +
                          "<p>Màn hình làm bài thi. Câu hỏi sẽ hiển thị ở đây.</p>" +
                          "<div id='questions'></div>" +
                          "<script>" +
                          "  function saveAnswer(qId, ans) {" +
                          "      window.cefQuery({ request: 'SAVE_ANSWER:' + qId + ':' + ans });" +
                          "  }" +
                          "</script>" +
                          "</body></html>";
                          
            File tempHtml = File.createTempFile("exam_" + examId, ".html");
            tempHtml.deleteOnExit();
            java.nio.file.Files.write(tempHtml.toPath(), html.getBytes("UTF-8"));

            browser = JcefManager.getClient().createBrowser(tempHtml.toURI().toString(), false, false);
            
            CefMessageRouter msgRouter = CefMessageRouter.create();
            msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
                @Override
                public boolean onQuery(CefBrowser browser, org.cef.browser.CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
                    if (request.startsWith("SAVE_ANSWER:")) {
                        String[] parts = request.split(":");
                        if (parts.length >= 3) {
                            int qId = Integer.parseInt(parts[1]);
                            String ans = parts[2];
                            OfflineExamCache.saveDraftAnswer(examId, currentUserId, qId, ans);
                            callback.success("OK");
                            return true;
                        }
                    }
                    return false;
                }
            }, true);
            JcefManager.getClient().addMessageRouter(msgRouter);
            
            add(browser.getUIComponent(), BorderLayout.CENTER);
            
        } catch (Exception e) {
            e.printStackTrace();
            add(new JLabel("Lỗi tải giao diện làm bài thi!"), BorderLayout.CENTER);
        }
        
        autoSaveTimer = new Timer(30000, e -> {
            System.out.println("[Offline Cache] Auto-saving drafts to local DB...");
        });
        autoSaveTimer.start();
    }
    
    public void requestClose() {
        if (autoSaveTimer != null) autoSaveTimer.stop();
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        if (parentWindow != null) {
            parentWindow.setVisible(false); // Hide immediately for good UX
            
            new Thread(() -> {
                if (browser != null) {
                    browser.close(true); // Close CEF asynchronously
                }
                SwingUtilities.invokeLater(() -> {
                    Timer t = new Timer(500, evt -> {
                        parentWindow.dispose(); // Dispose safely after CEF native window is destroyed
                    });
                    t.setRepeats(false);
                    t.start();
                });
            }).start();
        }
    }

    private void submitExam() {
        if (autoSaveTimer != null) autoSaveTimer.stop();
        Map<Integer, String> drafts = OfflineExamCache.getDraftAnswers(examId, currentUserId);
        
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String answersJson = gson.toJson(drafts);
        
        try { networkManager.sendPacket(new Packet("SUBMIT_EXAM", examId + "|" + answersJson)); } catch (Exception e) { e.printStackTrace(); }
        
        OfflineExamCache.clearDraft(examId, currentUserId);
        
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        if (parentWindow != null) {
            parentWindow.setVisible(false); // Hide immediately
            
            new Thread(() -> {
                if (browser != null) {
                    browser.close(true);
                }
                SwingUtilities.invokeLater(() -> {
                    Timer t = new Timer(500, evt -> {
                        parentWindow.dispose();
                    });
                    t.setRepeats(false);
                    t.start();
                });
            }).start();
        }
    }
}
