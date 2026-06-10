package com.mycompany.tutorhub_enterprise.client.exam.ui;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

/**
 * ExamSuccessDialog – Displayed after exam is submitted successfully.
 *
 * All icons use ExamIconFactory (no unicode/emoji).
 * No JOptionPane. No System.exit().
 */
public class ExamSuccessDialog extends JDialog {

    private static final Color TITLE_BG   = Color.decode("#F0F0F0");
    private static final Color BODY_BG    = Color.WHITE;
    private static final Color FOOTER_BG  = Color.decode("#F5F5F5");
    private static final Color BORDER_CLR = Color.decode("#BBBBBB");

    public ExamSuccessDialog(JFrame parent, Runnable onClose) {
        super(parent, true);
        setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BODY_BG);
        root.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildBody(),     BorderLayout.CENTER);
        root.add(buildFooter(onClose), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(380, 160));
        setLocationRelativeTo(parent);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new MigLayout("insets 6 10, fillx, aligny center", "[left]push[right]"));
        bar.setBackground(TITLE_BG);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR));

        // Check icon (Graphics2D green circle) + text — no ✔ unicode
        JLabel lblTitle = new JLabel("Nộp bài thành công");
        lblTitle.setIcon(ExamIconFactory.checkIcon(14));
        lblTitle.setIconTextGap(6);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitle.setForeground(Color.decode("#1B5E20")); // dark green

        bar.add(lblTitle, "cell 0 0");
        return bar;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new MigLayout(
            "insets 24 20 16 20, fillx, align center center",
            "[grow]",
            "[][]"
        ));
        body.setBackground(BODY_BG);

        JLabel lbl1 = new JLabel("Bài thi đã được nộp thành công.", SwingConstants.CENTER);
        lbl1.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl1.setForeground(Color.decode("#222222"));

        JLabel lbl2 = new JLabel("Hệ thống sẽ thoát khỏi chế độ Kiosk.", SwingConstants.CENTER);
        lbl2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl2.setForeground(Color.decode("#888888"));

        body.add(lbl1, "cell 0 0, growx");
        body.add(lbl2, "cell 0 1, growx, gaptop 4");
        return body;
    }

    private JPanel buildFooter(Runnable onClose) {
        JPanel footer = new JPanel(new MigLayout("insets 8 10, fillx", "push[right]"));
        footer.setBackground(FOOTER_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_CLR));

        JButton btnOk = new JButton("OK");
        btnOk.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnOk.setBackground(Color.decode("#1B5E8B"));
        btnOk.setForeground(Color.WHITE);
        btnOk.setFocusPainted(false);
        btnOk.setBorderPainted(false);
        btnOk.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnOk.addActionListener(e -> {
            dispose();
            if (onClose != null) onClose.run();
        });

        footer.add(btnOk, "cell 0 0, w 80!, h 28!");
        return footer;
    }
}
