package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class NewClassDialog extends JDialog {
    public interface CreateListener {
        void onCreate(String className, String organizationName);
    }

    private static final int MAX_CLASS_NAME_LENGTH = 90;

    private final String organizationName;
    private final CreateListener createListener;
    private final HintTextField classNameField;
    private final JLabel validationLabel;
    private final JButton createButton;

    public NewClassDialog(Window owner, String organizationName, CreateListener createListener) {
        super(owner, "New Class", ModalityType.APPLICATION_MODAL);
        this.organizationName = organizationName == null || organizationName.trim().isEmpty()
                ? "My Account"
                : organizationName.trim();
        this.createListener = createListener;
        this.classNameField = new HintTextField("Enter class name (within 90 characters)");
        this.validationLabel = new JLabel(" ");
        this.createButton = new JButton("Create and Enter Class");

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setContentPane(createContent());
        setSize(500, 330);
        setMinimumSize(new Dimension(500, 330));
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(createButton);
        updateCreateState();
    }

    private JComponent createContent() {
        RoundedPanel root = new RoundedPanel(18, Color.WHITE);
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(22, 30, 26, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("New Class");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.decode("#202124"));

        JButton closeButton = createCloseButton();
        header.add(title, BorderLayout.WEST);
        header.add(closeButton, BorderLayout.EAST);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(createOrganizationRow());
        form.add(Box.createVerticalStrut(28));
        form.add(createInputBox());
        form.add(Box.createVerticalStrut(6));

        validationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        validationLabel.setForeground(Color.decode("#9CA3AF"));
        validationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(validationLabel);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        footer.add(createButton);

        createButton.setPreferredSize(new Dimension(230, 54));
        createButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        createButton.setBorder(BorderFactory.createEmptyBorder());
        createButton.setFocusPainted(false);
        createButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createButton.addActionListener(e -> submit());

        classNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateCreateState(); }
            public void removeUpdate(DocumentEvent e) { updateCreateState(); }
            public void changedUpdate(DocumentEvent e) { updateCreateState(); }
        });

        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JPanel createOrganizationRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel left = new JLabel("Select Organization");
        left.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        left.setForeground(Color.decode("#202124"));

        JLabel right = new JLabel(organizationName + "   >");
        right.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        right.setForeground(Color.decode("#8A8D91"));

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JComponent createInputBox() {
        RoundedPanel inputWrapper = new RoundedPanel(8, Color.WHITE);
        inputWrapper.setLayout(new BorderLayout());
        inputWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#DADCE0")),
                new EmptyBorder(0, 14, 0, 14)
        ));
        inputWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        inputWrapper.setPreferredSize(new Dimension(440, 56));

        classNameField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        classNameField.setBorder(BorderFactory.createEmptyBorder());
        classNameField.setOpaque(false);
        inputWrapper.add(classNameField, BorderLayout.CENTER);
        return inputWrapper;
    }

    private JButton createCloseButton() {
        JButton button = new JButton("x");
        button.setPreferredSize(new Dimension(32, 32));
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.decode("#8A8D91"));
        button.setBackground(Color.decode("#F1F3F4"));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> dispose());
        return button;
    }

    private void updateCreateState() {
        String value = classNameField.getText().trim();
        boolean hasName = !value.isEmpty();
        boolean validLength = value.length() <= MAX_CLASS_NAME_LENGTH;
        boolean validSeparator = !value.contains("|");
        boolean enabled = hasName && validLength && validSeparator;

        createButton.setEnabled(enabled);
        createButton.setBackground(enabled ? Color.decode("#3B82F6") : Color.decode("#E5E7EB"));
        createButton.setForeground(enabled ? Color.WHITE : Color.decode("#FFFFFF"));

        if (!hasName) {
            validationLabel.setText(" ");
            validationLabel.setForeground(Color.decode("#9CA3AF"));
        } else if (!validLength) {
            validationLabel.setText(value.length() + "/" + MAX_CLASS_NAME_LENGTH + " characters");
            validationLabel.setForeground(Color.decode("#EF4444"));
        } else if (!validSeparator) {
            validationLabel.setText("Class name cannot contain the | character.");
            validationLabel.setForeground(Color.decode("#EF4444"));
        } else {
            validationLabel.setText(value.length() + "/" + MAX_CLASS_NAME_LENGTH + " characters");
            validationLabel.setForeground(Color.decode("#9CA3AF"));
        }
    }

    private void submit() {
        String className = classNameField.getText().trim();
        if (!createButton.isEnabled()) {
            updateCreateState();
            return;
        }
        dispose();
        if (createListener != null) {
            createListener.onCreate(className, organizationName);
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
