package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

import com.mycompany.tutorhub_enterprise.client.exam.models.*;
import com.mycompany.tutorhub_enterprise.client.exam.services.*;

/**
 * TSEExamShellFrame – Entry point for UI mockup preview.
 */
public class TSEExamShellFrame extends JFrame {

    private final CardLayout cardLayout    = new CardLayout();
    private final JPanel     mainContainer = new JPanel(cardLayout);
    private TSEExamShellPanel pnlExam;
    
    private final TSEExamService examService;
    private TSEExamContext context;
    private TSEStartExamResult currentExamSession;

    public TSEExamShellFrame(TSEExamService examService) {
        this.examService = examService;
        setTitle("TutorHub Kiosk – UI Preview");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                exitPreview();
            }
        });

        // ── Build screens ──────────────────────────────────
        TSELoginPanel pnlLogin = new TSELoginPanel(examService, this::onLoginSuccess, this::exitPreview);
        
        mainContainer.add(pnlLogin,  "LOGIN");
        add(mainContainer);
        cardLayout.show(mainContainer, "LOGIN");
    }

    // ── Navigation ─────────────────────────────────────────
    private void onLoginSuccess(TSEExamContext ctx) {
        this.context = ctx;
        TSEConfigListPanel pnlConfig = new TSEConfigListPanel(examService, context, 0, this::promptPassword, this::exitPreview);
        mainContainer.add(pnlConfig, "CONFIG");
        cardLayout.show(mainContainer, "CONFIG");
    }

    private void promptPassword(TSEExamConfig config) {
        if (!config.requiresPassword) {
            verifyPasswordAndStart(config, "");
            return;
        }
        
        ExamPasswordDialog dlg = new ExamPasswordDialog(this, pwd -> {
            verifyPasswordAndStart(config, pwd);
        });
        dlg.setVisible(true);
    }
    
    private void verifyPasswordAndStart(TSEExamConfig config, String pwd) {
        examService.verifyPasswordAndStart(context.userId, config.examId, pwd).thenAccept(res -> {
            SwingUtilities.invokeLater(() -> {
                if (res.success) {
                    currentExamSession = res;
                    pnlExam = new TSEExamShellPanel(examService, currentExamSession, this::promptSubmit, this::exitPreview);
                    mainContainer.add(pnlExam, "EXAM");
                    cardLayout.show(mainContainer, "EXAM");
                } else {
                    JOptionPane.showMessageDialog(this, res.message, "Lỗi xác thực", JOptionPane.ERROR_MESSAGE);
                }
            });
        });
    }

    private void promptSubmit() {
        SubmitConfirmDialog dlg = new SubmitConfirmDialog(this, () -> {
            if (pnlExam != null) {
                // Đặt callback chờ dữ liệu JS
                TSEJcefLifecycleManager.setSubmitCallback((payload, cb) -> {
                    if (currentExamSession != null) {
                        examService.submitExam(currentExamSession.sessionId, 0, payload).thenAccept(res -> {
                            SwingUtilities.invokeLater(() -> {
                                if (res.success) {
                                    ExamSuccessDialog success = new ExamSuccessDialog(this, this::exitPreview);
                                    success.setVisible(true);
                                } else {
                                    JOptionPane.showMessageDialog(this, "Lỗi nộp bài: " + res.message, "Lỗi", JOptionPane.ERROR_MESSAGE);
                                }
                            });
                        });
                    }
                    cb.success("OK");
                });
                
                // Kích hoạt thu thập trả lời từ trang web
                pnlExam.getBrowserPanel().executeJavaScript("if (typeof collectTSEAnswers === 'function') { collectTSEAnswers(); } else { window.cefQuery && window.cefQuery({request: 'SUBMIT_PAYLOAD:{\"error\":\"no_script\"}'}); }");
                
                // Reset callback nếu sau X giây không phản hồi? Tạm thời để đơn giản
            }
        });
        dlg.setVisible(true);
    }

    private void exitPreview() {
        if (pnlExam != null) {
            pnlExam.cleanup();
        }
        com.mycompany.tutorhub_enterprise.client.NetworkManager.getInstance().disconnect();
        TSEJcefLifecycleManager.cleanup();
        dispose(); // Never System.exit()
    }
}
