package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

/**
 * ExamKioskPreviewFrame – Entry point for UI mockup preview.
 *
 * Architecture (Option B – Hybrid):
 *   - Swing Shell (this frame): undecorated, maximised
 *   - Login screen  : KioskBgPanel (Swing)
 *   - Config screen : KioskBgPanel (Swing)
 *   - Exam screen   : BorderLayout
 *       NORTH  = ExamHeaderBar   (Swing – always on top of JCEF)
 *       CENTER = ExamTakingMockPanel (Swing placeholder → real JCEF in Phase 3)
 *       SOUTH  = ExamFooterStatusBar (Swing)
 *
 * No Rust, no LockdownManager, no real JCEF, no System.exit() calls.
 */
public class ExamKioskPreviewFrame extends JFrame {

    private final CardLayout cardLayout    = new CardLayout();
    private final JPanel     mainContainer = new JPanel(cardLayout);

    public ExamKioskPreviewFrame() {
        setTitle("TutorHub Kiosk – UI Preview");
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // ── Build screens ──────────────────────────────────
        ExamLoginMockPanel      pnlLogin  = new ExamLoginMockPanel(this::showConfig, this::exitPreview);
        ExamConfigListMockPanel pnlConfig = new ExamConfigListMockPanel(this::promptPassword, this::exitPreview);

        // Exam taking: Swing shell wrapping a JCEF placeholder
        JPanel pnlExam = new JPanel(new BorderLayout(0, 0));
        pnlExam.setBackground(Color.WHITE);
        pnlExam.add(new ExamHeaderBar("Bài thi Học kỳ 1 – KTHP", this::promptSubmit), BorderLayout.NORTH);
        pnlExam.add(new ExamTakingMockPanel(),                                          BorderLayout.CENTER);
        pnlExam.add(new ExamFooterStatusBar(this::exitPreview),                         BorderLayout.SOUTH);

        mainContainer.add(pnlLogin,  "LOGIN");
        mainContainer.add(pnlConfig, "CONFIG");
        mainContainer.add(pnlExam,   "EXAM");

        add(mainContainer);
        cardLayout.show(mainContainer, "LOGIN");
    }

    // ── Navigation ─────────────────────────────────────────
    private void showConfig() {
        cardLayout.show(mainContainer, "CONFIG");
    }

    private void promptPassword(String configName) {
        ExamPasswordDialog dlg = new ExamPasswordDialog(this,
            pwd -> cardLayout.show(mainContainer, "EXAM"));
        dlg.setVisible(true);
    }

    private void promptSubmit() {
        SubmitConfirmDialog dlg = new SubmitConfirmDialog(this, () -> {
            ExamSuccessDialog success = new ExamSuccessDialog(this, this::exitPreview);
            success.setVisible(true);
        });
        dlg.setVisible(true);
    }

    private void exitPreview() {
        dispose(); // Never System.exit()
    }

    // ── Entry point ────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                UIManager.put("defaultFont",      new Font("Segoe UI", Font.PLAIN, 13));
                UIManager.put("Button.arc",        8);
                UIManager.put("Component.arc",     8);
                UIManager.put("TextComponent.arc", 6);
            } catch (Exception ignored) {}
            new ExamKioskPreviewFrame().setVisible(true);
        });
    }
}
