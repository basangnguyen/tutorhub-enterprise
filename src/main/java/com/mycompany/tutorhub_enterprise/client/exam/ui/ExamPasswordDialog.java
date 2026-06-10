package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * ExamPasswordDialog – Password prompt.
 *
 * Matches 04_seb_password_ref.png:
 *   - Custom title bar with X button
 *   - Body: grey key icon (Graphics2D) + label + password field
 *   - Footer: Confirm (left-primary) + Cancel (right)
 *
 * No JOptionPane. Uses instance field to share pwdField between body and footer.
 */
public class ExamPasswordDialog extends JDialog {

    private static final Color TITLE_BG   = Color.decode("#F0F0F0");
    private static final Color BODY_BG    = Color.WHITE;
    private static final Color FOOTER_BG  = Color.decode("#F5F5F5");
    private static final Color BORDER_CLR = Color.decode("#BBBBBB");

    // Instance field – shared between buildBody() and buildFooter()
    private final JPasswordField pwdField = new JPasswordField();

    public ExamPasswordDialog(JFrame parent, Consumer<String> onConfirm) {
        super(parent, true);
        setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BODY_BG);
        root.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));

        root.add(buildTitleBar(),        BorderLayout.NORTH);
        root.add(buildBody(),            BorderLayout.CENTER);
        root.add(buildFooter(onConfirm), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(460, 180));
        setLocationRelativeTo(parent);
    }

    // ── title bar ────────────────────────────────────────────────
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new MigLayout(
                "insets 6 10, fillx, aligny center", "[left]push[right]"));
        bar.setBackground(TITLE_BG);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_CLR));

        JLabel lblTitle = new JLabel("Password Required");
        lblTitle.setIcon(new FlatSVGIcon("images/exam/icons/lock.svg", 16, 16));
        lblTitle.setIconTextGap(8);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitle.setForeground(Color.decode("#222222"));

        JButton btnClose = flatCloseBtn();
        btnClose.addActionListener(e -> dispose());

        bar.add(lblTitle, "cell 0 0");
        bar.add(btnClose, "cell 1 0");
        return bar;
    }

    // ── body ─────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new MigLayout(
                "insets 20 16 16 16, fillx",
                "[64!][grow]",
                "[top]"));
        body.setBackground(BODY_BG);

        // Drawn key icon replaced with SVG
        JLabel keyIcon = new JLabel(ExamLoginMockPanel.loadSVG("images/exam/icons/key-round.svg", 48, Color.decode("#9E9E9E")));

        // Right side: label + password field
        JPanel right = new JPanel(new MigLayout("wrap 1, insets 0, gapy 8", "[grow]"));
        right.setOpaque(false);

        JLabel lblText = new JLabel("Please enter the exam password:");
        lblText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblText.setForeground(Color.decode("#222222"));

        pwdField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        right.add(lblText,   "growx");
        right.add(pwdField,  "growx, h 32!");

        body.add(keyIcon, "cell 0 0, aligny center");
        body.add(right,   "cell 1 0, grow");
        return body;
    }

    // ── footer: Confirm + Cancel ──────────────────────────────────
    private JPanel buildFooter(Consumer<String> onConfirm) {
        JPanel footer = new JPanel(new MigLayout(
                "insets 8 10, fillx, aligny center", "push[][right][right]"));
        footer.setBackground(FOOTER_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_CLR));

        JButton btnConfirm = new JButton("Confirm");
        stylePrimaryBtn(btnConfirm);
        btnConfirm.addActionListener(e -> {
            String pwd = new String(pwdField.getPassword());
            dispose();
            if (onConfirm != null) onConfirm.accept(pwd);
        });

        JButton btnCancel = new JButton("Cancel");
        styleSecondaryBtn(btnCancel);
        btnCancel.addActionListener(e -> dispose());

        footer.add(btnConfirm, "cell 1 0, w 90!, h 28!");
        footer.add(btnCancel,  "cell 2 0, w 80!, h 28!");
        return footer;
    }

    // ── helpers ───────────────────────────────────────────────────
    private JButton flatCloseBtn() {
        // Uses ExamIconFactory Graphics2D X icon, no unicode glyph
        return ExamIconFactory.closeButton();
    }

    private void stylePrimaryBtn(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setBackground(Color.decode("#E8E8E8"));
        btn.setForeground(Color.decode("#222222"));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.decode("#AAAAAA"), 1));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSecondaryBtn(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setBackground(Color.decode("#E8E8E8"));
        btn.setForeground(Color.decode("#222222"));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(Color.decode("#AAAAAA"), 1));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
