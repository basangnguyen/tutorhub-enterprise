package com.mycompany.tutorhub_enterprise.client.exam.ui;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

/**
 * ExitConfirmDialog – Confirm dialog before exiting the app.
 *
 * Custom title bar, body, footer.
 * All icons use ExamIconFactory (no unicode/emoji).
 * No JOptionPane. No System.exit().
 */
public class ExitConfirmDialog extends JDialog {

    private static final Color TITLE_BG   = Color.decode("#F0F0F0");
    private static final Color BODY_BG    = Color.WHITE;
    private static final Color FOOTER_BG  = Color.decode("#F5F5F5");
    private static final Color BORDER_CLR = Color.decode("#BBBBBB");
    private static final Color BTN_RED    = Color.decode("#EF4444");

    public ExitConfirmDialog(JFrame parent, Runnable onConfirm) {
        super(parent, true);
        setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BODY_BG);
        root.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildBody(),     BorderLayout.CENTER);
        root.add(buildFooter(onConfirm), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(400, 170));
        setLocationRelativeTo(parent);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new MigLayout("insets 6 10, fillx, aligny center", "[left]push[right]"));
        bar.setBackground(TITLE_BG);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR));

        JLabel lblTitle = ExamIconFactory.warningLabel("Xác nhận thoát");
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitle.setForeground(Color.decode("#222222"));

        JButton btnClose = ExamIconFactory.closeButton();
        btnClose.addActionListener(e -> dispose());

        bar.add(lblTitle, "cell 0 0");
        bar.add(btnClose, "cell 1 0");
        return bar;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new MigLayout(
            "insets 20 20 16 20, fillx",
            "[grow]",
            "[][]"
        ));
        body.setBackground(BODY_BG);

        JLabel lbl1 = new JLabel("Bạn có chắc chắn muốn thoát khỏi nền tảng thi?");
        lbl1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl1.setForeground(Color.decode("#222222"));

        JLabel lbl2 = new JLabel("Hành động này sẽ đóng hoàn toàn ứng dụng TutorHub Secure Exam.");
        lbl2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl2.setForeground(Color.decode("#888888"));

        body.add(lbl1, "cell 0 0, growx");
        body.add(lbl2, "cell 0 1, growx, gaptop 4");
        return body;
    }

    private JPanel buildFooter(Runnable onConfirm) {
        JPanel footer = new JPanel(new MigLayout("insets 8 10, fillx", "push[][right][right]"));
        footer.setBackground(FOOTER_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_CLR));

        JButton btnConfirm = new JButton("Thoát");
        btnConfirm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnConfirm.setBackground(BTN_RED);
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFocusPainted(false);
        btnConfirm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConfirm.setBorderPainted(false);
        btnConfirm.addActionListener(e -> {
            dispose();
            if (onConfirm != null) onConfirm.run();
        });

        JButton btnCancel = new JButton("Quay lại");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.setBackground(Color.decode("#E8E8E8"));
        btnCancel.setForeground(Color.decode("#222222"));
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancel.setBorder(BorderFactory.createLineBorder(Color.decode("#AAAAAA"), 1));
        btnCancel.addActionListener(e -> dispose());

        footer.add(btnConfirm, "cell 1 0, w 90!, h 28!");
        footer.add(btnCancel,  "cell 2 0, w 90!, h 28!");
        return footer;
    }
}
