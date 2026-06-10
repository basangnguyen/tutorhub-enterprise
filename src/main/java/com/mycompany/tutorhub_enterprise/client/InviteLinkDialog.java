package com.mycompany.tutorhub_enterprise.client;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

public class InviteLinkDialog extends JDialog {
    private static final String INVITE_PREFIX = "tutorhub://public-lesson?code=";

    private final JLabel statusLabel = new JLabel(" ");
    private final String joinCode;
    private final String inviteLink;

    public InviteLinkDialog(Window owner, String lessonTitle, String joinCode) {
        super(owner, "Public Lesson Invite", ModalityType.APPLICATION_MODAL);
        this.joinCode = clean(joinCode);
        this.inviteLink = buildInviteLink(this.joinCode);

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setContentPane(createContent(cleanTitle(lessonTitle)));
        setSize(680, 430);
        setMinimumSize(new Dimension(640, 400));
        setLocationRelativeTo(owner);
    }

    public static String buildInviteLink(String joinCode) {
        return INVITE_PREFIX + clean(joinCode);
    }

    public static void copyToClipboard(String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(value.trim()), null);
    }

    private JComponent createContent(String lessonTitle) {
        RoundedPanel root = new RoundedPanel(16, Color.WHITE);
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Public Lesson Created");
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        title.setForeground(Color.decode("#202124"));

        JButton close = createIconButton("x");
        close.addActionListener(e -> dispose());

        header.add(title, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(24, 0));
        body.setOpaque(false);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel lessonLabel = new JLabel(lessonTitle);
        lessonLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lessonLabel.setForeground(Color.decode("#5F6368"));
        lessonLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel codeLabel = new JLabel(joinCode);
        codeLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        codeLabel.setForeground(Color.decode("#111827"));
        codeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField linkField = new JTextField(inviteLink);
        linkField.setEditable(false);
        linkField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        linkField.setForeground(Color.decode("#202124"));
        linkField.setBackground(Color.decode("#F7F8FA"));
        linkField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.decode("#E5E7EB")),
                new EmptyBorder(0, 12, 0, 12)
        ));
        linkField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        linkField.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.decode("#0F9D58"));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        details.add(Box.createVerticalStrut(18));
        details.add(lessonLabel);
        details.add(Box.createVerticalStrut(18));
        details.add(newLabel("Invite code"));
        details.add(Box.createVerticalStrut(6));
        details.add(codeLabel);
        details.add(Box.createVerticalStrut(16));
        details.add(newLabel("Invite link"));
        details.add(Box.createVerticalStrut(8));
        details.add(linkField);
        details.add(Box.createVerticalStrut(8));
        details.add(statusLabel);

        body.add(details, BorderLayout.CENTER);
        body.add(createQrCard(), BorderLayout.EAST);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        footer.setOpaque(false);

        JButton copyCode = createActionButton("Copy Code", Color.decode("#F1F3F4"), Color.decode("#202124"));
        copyCode.addActionListener(e -> copy(joinCode, "Code copied."));

        JButton copyLink = createActionButton("Copy Link", Color.decode("#2EDB68"), Color.BLACK);
        copyLink.addActionListener(e -> copy(inviteLink, "Invite link copied."));

        JButton done = createActionButton("Done", Color.decode("#111827"), Color.WHITE);
        done.addActionListener(e -> dispose());

        footer.add(copyCode);
        footer.add(copyLink);
        footer.add(done);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JLabel newLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(Color.decode("#6B7280"));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JComponent createQrCard() {
        RoundedPanel card = new RoundedPanel(14, Color.decode("#F7F8FA"));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setPreferredSize(new Dimension(210, 250));

        JLabel title = new JLabel("Scan to join");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(Color.decode("#202124"));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel qrLabel = new JLabel();
        qrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        Image qrImage = createQrImage(inviteLink, 170);
        if (qrImage != null) {
            qrLabel.setIcon(new ImageIcon(qrImage));
        } else {
            qrLabel.setText("QR unavailable");
            qrLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            qrLabel.setForeground(Color.decode("#9AA0A6"));
        }

        JLabel caption = new JLabel("Share this with students");
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        caption.setForeground(Color.decode("#6B7280"));
        caption.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(title);
        card.add(Box.createVerticalStrut(12));
        card.add(qrLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(caption);
        return card;
    }

    private Image createQrImage(String value, int size) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            int white = Color.WHITE.getRGB();
            int black = Color.decode("#111827").getRGB();
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? black : white);
                }
            }
            return image;
        } catch (WriterException e) {
            return null;
        }
    }

    private JButton createIconButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(32, 32));
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(Color.decode("#8A8D91"));
        button.setBackground(Color.decode("#F1F3F4"));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createActionButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(112, 42));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void copy(String value, String message) {
        copyToClipboard(value);
        statusLabel.setText(message);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String cleanTitle(String value) {
        String text = clean(value);
        return text.isEmpty() ? "Public Lesson" : text;
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
}
