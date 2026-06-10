package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/**
 * Dialog upload ảnh / video ngắn lên Locket Class.
 * Phong cách thiết kế giống UploadReelDialog nhưng đơn giản hơn (1 bước).
 */
public class UploadLocketDialog extends JDialog {

    // ── Design System ──────────────────────────────────────────────────────────
    private static final Color BG          = new Color(0xFAFAFC);
    private static final Color WHITE       = Color.WHITE;
    private static final Color PURPLE_MAIN = new Color(0x8B5CF6);
    private static final Color PURPLE_DARK = new Color(0x7C3AED);
    private static final Color PURPLE_ZONE = new Color(0xF5F3FF);
    private static final Color TEXT_MAIN   = new Color(0x111827);
    private static final Color TEXT_MUTED  = new Color(0x6B7280);
    private static final Color BORDER      = new Color(0xE5E7EB);

    // ── State ──────────────────────────────────────────────────────────────────
    private File selectedFile;
    private String mediaType = "image"; // "image" or "video"
    private boolean approved = false;
    private boolean isDragOver = false;

    // ── Widgets ────────────────────────────────────────────────────────────────
    private JLabel lblDropInstruction;
    private JLabel lblFileName;
    private JTextPane txtTitle;
    private JLabel lblCharCount;
    private JLabel lblPreviewInfo;

    public UploadLocketDialog(Frame parent) {
        super(parent, "", true);
        setUndecorated(true);
        if (parent != null) {
            setSize(parent.getSize());
            setLocation(parent.getLocation());
        } else {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = ge.getMaximumWindowBounds();
            setSize(bounds.width, bounds.height);
            setLocationRelativeTo(null);
        }
        setBackground(new Color(0, 0, 0, 160));

        // Overlay wrapper
        JPanel overlayWrapper = new JPanel(new GridBagLayout());
        overlayWrapper.setOpaque(false);

        // Card nổi trung tâm
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // shadow
                for (int i = 0; i < 8; i++) {
                    g2.setColor(new Color(0, 0, 0, 4 + i));
                    g2.fillRoundRect(i, i + 3, getWidth() - i * 2, getHeight() - i * 2 - 3, 28, 28);
                }
                g2.setColor(WHITE);
                g2.fillRoundRect(8, 8, getWidth() - 16, getHeight() - 16, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));
        card.setPreferredSize(new Dimension(640, 560));

        card.add(buildHeader(), BorderLayout.NORTH);
        card.add(buildBody(), BorderLayout.CENTER);
        card.add(buildFooter(), BorderLayout.SOUTH);

        overlayWrapper.add(card);
        setContentPane(overlayWrapper);

        // Fade-in animation
        setOpacity(0f);
        Timer fadeIn = new Timer(16, null);
        final float[] op = {0f};
        fadeIn.addActionListener(e -> {
            op[0] += 0.08f;
            if (op[0] >= 1f) { op[0] = 1f; fadeIn.stop(); }
            setOpacity(op[0]);
        });
        fadeIn.start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Header
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.setBorder(new EmptyBorder(8, 20, 8, 20));

        // Gradient title pill
        JPanel titlePill = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePill.setOpaque(false);

        // Locket icon
        JLabel icoLocket = new JLabel();
        try {
            java.net.URL url = getClass().getResource("/images/icon/icon_locket.png");
            if (url != null) {
                Image img = new ImageIcon(url).getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                icoLocket.setIcon(new ImageIcon(img));
            }
        } catch (Exception ignored) {}

        JLabel lblTitle = new JLabel("Tạo Locket Class");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(PURPLE_DARK);

        titlePill.add(icoLocket);
        titlePill.add(lblTitle);
        hdr.add(titlePill, BorderLayout.CENTER);

        // Close button
        JButton btnClose = new JButton("✕") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(0xFEE2E2) : new Color(0xF3F4F6));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnClose.setForeground(TEXT_MUTED);
        btnClose.setContentAreaFilled(false);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setPreferredSize(new Dimension(34, 34));
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());
        hdr.add(btnClose, BorderLayout.EAST);

        // Separator
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(hdr, BorderLayout.CENTER);
        wrap.add(makeSep(), BorderLayout.SOUTH);
        return wrap;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Body
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(16, 28, 8, 28));

        // Drop zone
        body.add(buildDropZone());
        body.add(Box.createVerticalStrut(18));

        // Tên file đã chọn
        lblFileName = new JLabel("  Chưa chọn tệp");
        lblFileName.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblFileName.setForeground(TEXT_MUTED);
        lblFileName.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(lblFileName);
        body.add(Box.createVerticalStrut(18));

        // Mô tả ngắn
        JLabel lblCaption = new JLabel("Mô tả nội dung");
        lblCaption.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCaption.setForeground(TEXT_MAIN);
        lblCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(lblCaption);
        body.add(Box.createVerticalStrut(8));
        body.add(buildCaptionBox());

        return body;
    }

    // (Removed buildMediaTypeToggle)

    private JPanel buildDropZone() {
        JPanel zone = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isDragOver ? new Color(139, 92, 246, 18) : PURPLE_ZONE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(0xC4B5FD));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        0, new float[]{8f, 5f}, 0));
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 20, 20);
                g2.dispose();
            }
        };
        zone.setLayout(new BoxLayout(zone, BoxLayout.Y_AXIS));
        zone.setOpaque(false);
        zone.setBorder(new EmptyBorder(28, 24, 28, 24));
        zone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        zone.setAlignmentX(Component.LEFT_ALIGNMENT);
        zone.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        // Upload icon
        JLabel icoUpload = new JLabel();
        try {
            java.net.URL url = getClass().getResource("/images/icon/video_posting.png");
            if (url != null) {
                Image img = new ImageIcon(url).getImage().getScaledInstance(56, 56, Image.SCALE_SMOOTH);
                icoUpload.setIcon(new ImageIcon(img));
            }
        } catch (Exception ignored) {}
        icoUpload.setAlignmentX(Component.CENTER_ALIGNMENT);
        zone.add(icoUpload);
        zone.add(Box.createVerticalStrut(12));

        lblDropInstruction = new JLabel("Kéo & thả hoặc click để chọn tệp");
        lblDropInstruction.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblDropInstruction.setForeground(PURPLE_DARK);
        lblDropInstruction.setAlignmentX(Component.CENTER_ALIGNMENT);
        zone.add(lblDropInstruction);

        lblPreviewInfo = new JLabel("Ảnh: JPG, PNG, WEBP");
        lblPreviewInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPreviewInfo.setForeground(TEXT_MUTED);
        lblPreviewInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        zone.add(Box.createVerticalStrut(6));
        zone.add(lblPreviewInfo);

        // Drag-and-drop
        try {
            DropTarget dt = new DropTarget();
            dt.addDropTargetListener(new DropTargetAdapter() {
                @Override public void dragEnter(DropTargetDragEvent e) {
                    isDragOver = true; lblDropInstruction.setText("Thả để tải lên!"); zone.repaint();
                }
                @Override public void dragExit(DropTargetEvent e) {
                    isDragOver = false; lblDropInstruction.setText("Kéo & thả hoặc click để chọn tệp"); zone.repaint();
                }
                @Override public void drop(DropTargetDropEvent e) {
                    isDragOver = false;
                    try {
                        e.acceptDrop(DnDConstants.ACTION_COPY);
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) handleFileSelected(files.get(0));
                    } catch (Exception ex) { ex.printStackTrace(); }
                    zone.repaint();
                }
            });
            zone.setDropTarget(dt);
        } catch (java.util.TooManyListenersException ignored) {}

        // Click to open file chooser
        zone.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openFileChooser(); }
        });

        return zone;
    }

    private JPanel buildCaptionBox() {
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        txtTitle = new JTextPane() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        txtTitle.setText("Nhập mô tả ngắn...");
        txtTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtTitle.setForeground(TEXT_MUTED);
        txtTitle.setOpaque(false);
        txtTitle.setBorder(new EmptyBorder(10, 14, 10, 14));
        txtTitle.setPreferredSize(new Dimension(100, 60));

        txtTitle.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (txtTitle.getText().equals("Nhập mô tả ngắn...")) {
                    txtTitle.setText("");
                    txtTitle.setForeground(TEXT_MAIN);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (txtTitle.getText().trim().isEmpty()) {
                    txtTitle.setText("Nhập mô tả ngắn...");
                    txtTitle.setForeground(TEXT_MUTED);
                }
            }
        });

        lblCharCount = new JLabel("0/150");
        lblCharCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCharCount.setForeground(TEXT_MUTED);
        lblCharCount.setHorizontalAlignment(SwingConstants.RIGHT);

        txtTitle.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                int len = txtTitle.getText().length();
                lblCharCount.setText(len + "/150");
                if (len > 150) txtTitle.setForeground(new Color(0xEF4444));
                else txtTitle.setForeground(TEXT_MAIN);
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        wrap.add(txtTitle, BorderLayout.CENTER);
        wrap.add(lblCharCount, BorderLayout.SOUTH);
        return wrap;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Footer
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                makeSepBorder(), new EmptyBorder(0, 20, 4, 20)));

        JButton btnCancel = new JButton("Hủy") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(0xE5E7EB) : new Color(0xF3F4F6));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancel.setForeground(TEXT_MUTED);
        btnCancel.setContentAreaFilled(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setFocusPainted(false);
        btnCancel.setPreferredSize(new Dimension(100, 40));
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancel.addActionListener(e -> dispose());

        JButton btnPost = new JButton("  🚀  Đăng ngay") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0xA78BFA),
                        getWidth(), 0, PURPLE_DARK);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnPost.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPost.setForeground(WHITE);
        btnPost.setContentAreaFilled(false);
        btnPost.setBorderPainted(false);
        btnPost.setFocusPainted(false);
        btnPost.setPreferredSize(new Dimension(160, 40));
        btnPost.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPost.addActionListener(e -> {
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn ảnh!", "Chưa chọn file", JOptionPane.WARNING_MESSAGE);
                return;
            }
            approved = true;
            dispose();
        });

        footer.add(btnCancel);
        footer.add(btnPost);
        return footer;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════
    private void openFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn ảnh");
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (JPG, PNG)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            handleFileSelected(chooser.getSelectedFile());
        }
    }

    private void handleFileSelected(File file) {
        mediaType = "image";
        selectedFile = file;
        String size = String.format("%.1f MB", file.length() / 1_048_576.0);
        lblFileName.setText("  ✔  " + file.getName() + "  (" + size + ")");
        lblFileName.setForeground(PURPLE_DARK);
        lblDropInstruction.setText("✅  Đã chọn: " + file.getName());
    }

    private void updateDropZoneHint() {
        if (selectedFile == null) {
            lblDropInstruction.setText("Kéo & thả hoặc click để chọn tệp");
        }
        lblPreviewInfo.setText("Ảnh: JPG, PNG, WEBP");
    }

    private JPanel makeSep() {
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(BORDER);
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        sep.setOpaque(false);
        sep.setPreferredSize(new Dimension(1, 1));
        return sep;
    }

    private Border makeSepBorder() {
        return BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Public API
    // ══════════════════════════════════════════════════════════════════════════
    public boolean isApproved() { return approved; }

    public File getSelectedFile() { return selectedFile; }

    @Override
    public String getTitle() {
        String t = txtTitle.getText().trim();
        return (t.isEmpty() || t.equals("Nhập mô tả ngắn...")) ? "" : t;
    }

    public String getMediaType() { return mediaType; }
}
