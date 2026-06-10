package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class JoinPublicLessonDialog extends JDialog {
    public interface JoinListener {
        void onJoin(String joinCodeOrLink);
    }

    private final JoinListener joinListener;
    private final HintTextField codeField;
    private final JButton joinButton;

    public JoinPublicLessonDialog(Window owner, JoinListener joinListener) {
        super(owner, "Join Public Lesson", ModalityType.APPLICATION_MODAL);
        this.joinListener = joinListener;
        this.codeField = new HintTextField("Enter public lesson code or link");
        this.joinButton = new JButton("Join Public Lesson");

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setContentPane(createContent());
        setSize(480, 260);
        setMinimumSize(new Dimension(480, 260));
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(joinButton);
        updateJoinState();
    }

    private JComponent createContent() {
        RoundedPanel root = new RoundedPanel(16, Color.WHITE);
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(22, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Join Public Lesson");
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        title.setForeground(Color.decode("#202124"));

        JButton close = new JButton("x");
        close.setPreferredSize(new Dimension(32, 32));
        close.setFont(new Font("Segoe UI", Font.BOLD, 16));
        close.setForeground(Color.decode("#8A8D91"));
        close.setBackground(Color.decode("#F1F3F4"));
        close.setBorder(BorderFactory.createEmptyBorder());
        close.setFocusPainted(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dispose());

        header.add(title, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Public lesson code");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        label.setForeground(Color.decode("#202124"));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        body.add(label);
        body.add(Box.createVerticalStrut(10));
        body.add(createInputBox());
        body.add(Box.createVerticalStrut(8));
        body.add(createHintLabel("Paste CLxxxx or tutorhub://public-lesson?code=CLxxxx"));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        joinButton.setPreferredSize(new Dimension(190, 50));
        joinButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        joinButton.setBorder(BorderFactory.createEmptyBorder());
        joinButton.setFocusPainted(false);
        joinButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        joinButton.addActionListener(e -> submit());
        footer.add(joinButton);

        codeField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateJoinState(); }
            public void removeUpdate(DocumentEvent e) { updateJoinState(); }
            public void changedUpdate(DocumentEvent e) { updateJoinState(); }
        });

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JComponent createInputBox() {
        RoundedPanel input = new RoundedPanel(8, Color.WHITE);
        input.setLayout(new BorderLayout());
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#DADCE0")),
                new EmptyBorder(0, 14, 0, 14)
        ));
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        input.setPreferredSize(new Dimension(420, 56));
        input.setAlignmentX(Component.LEFT_ALIGNMENT);

        codeField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        codeField.setBorder(BorderFactory.createEmptyBorder());
        codeField.setOpaque(false);
        input.add(codeField, BorderLayout.CENTER);
        return input;
    }

    private JLabel createHintLabel(String text) {
        JLabel hint = new JLabel(text);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(Color.decode("#9AA0A6"));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        return hint;
    }

    private void updateJoinState() {
        boolean enabled = !codeField.getText().trim().isEmpty();
        joinButton.setEnabled(enabled);
        joinButton.setBackground(enabled ? Color.decode("#2EDB68") : Color.decode("#E5E7EB"));
        joinButton.setForeground(enabled ? Color.BLACK : Color.WHITE);
    }

    private void submit() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            updateJoinState();
            return;
        }
        dispose();
        if (joinListener != null) {
            joinListener.onJoin(code);
        }
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;

        private RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class HintTextField extends JTextField {
        private final String hint;

        private HintTextField(String hint) {
            this.hint = hint;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Color.decode("#9AA0A6"));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, 0, y);
                g2.dispose();
            }
        }
    }
}
