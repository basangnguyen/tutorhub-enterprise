package com.mycompany.tutorhub_enterprise.client.home;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

public class LocketPostUploadDialog extends JDialog {

    private static final Color BG          = new Color(0xFAFAFC);
    private static final Color WHITE       = Color.WHITE;
    private static final Color PURPLE_MAIN = new Color(0x8B5CF6);
    private static final Color PURPLE_DARK = new Color(0x7C3AED);
    private static final Color PURPLE_ZONE = new Color(0xF5F3FF);
    private static final Color TEXT_MAIN   = new Color(0x111827);
    private static final Color TEXT_MUTED  = new Color(0x6B7280);
    private static final Color BORDER      = new Color(0xE5E7EB);

    private File selectedFile;
    private boolean isDragOver = false;
    private boolean isLoading = false;

    private JLabel lblDropInstruction;
    private JLabel lblPreviewInfo;
    private JTextPane txtCaption;
    private JLabel lblCharCount;
    private JPanel dropZone;
    private JLabel lblImagePreview;
    
    private JButton btnCancel;
    private JButton btnPost;

    private PostListener postListener;

    public interface PostListener {
        void onPostClicked(File file, String caption);
    }

    public LocketPostUploadDialog(Frame parent) {
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

        JPanel overlayWrapper = new JPanel(new GridBagLayout());
        overlayWrapper.setOpaque(false);

        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
        card.setPreferredSize(new Dimension(500, 680));

        card.add(buildHeader(), BorderLayout.NORTH);
        card.add(buildBody(), BorderLayout.CENTER);
        card.add(buildFooter(), BorderLayout.SOUTH);

        overlayWrapper.add(card);
        setContentPane(overlayWrapper);

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

    public void setPostListener(PostListener listener) {
        this.postListener = listener;
    }

    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.setBorder(new EmptyBorder(8, 20, 8, 20));

        JPanel titlePill = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePill.setOpaque(false);

        JLabel icoLocket = new JLabel();
        try {
            java.net.URL url = getClass().getResource("/images/icon/icon_locket.png");
            if (url != null) {
                Image img = new ImageIcon(url).getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                icoLocket.setIcon(new ImageIcon(img));
            }
        } catch (Exception ignored) {}

        JLabel lblTitle = new JLabel("Đăng ảnh Locket");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(PURPLE_DARK);

        titlePill.add(icoLocket);
        titlePill.add(lblTitle);
        hdr.add(titlePill, BorderLayout.CENTER);

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
        btnClose.addActionListener(e -> {
            if (!isLoading) dispose();
        });
        hdr.add(btnClose, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(hdr, BorderLayout.CENTER);
        wrap.add(makeSep(), BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(16, 28, 8, 28));

        body.add(buildDropZone());
        body.add(Box.createVerticalStrut(18));

        JLabel lblCaptionTitle = new JLabel("Mô tả nội dung (Tùy chọn)");
        lblCaptionTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCaptionTitle.setForeground(TEXT_MAIN);
        lblCaptionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(lblCaptionTitle);
        body.add(Box.createVerticalStrut(8));
        body.add(buildCaptionBox());

        return body;
    }

    private JPanel buildDropZone() {
        dropZone = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isDragOver ? new Color(139, 92, 246, 18) : PURPLE_ZONE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                if (selectedFile == null) {
                    g2.setColor(new Color(0xC4B5FD));
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8f, 5f}, 0));
                    g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 20, 20);
                }
                g2.dispose();
            }
        };
        dropZone.setLayout(new BorderLayout());
        dropZone.setOpaque(false);
        dropZone.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dropZone.setAlignmentX(Component.LEFT_ALIGNMENT);
        dropZone.setPreferredSize(new Dimension(420, 320));
        dropZone.setMaximumSize(new Dimension(420, 320));

        JPanel emptyStatePanel = new JPanel();
        emptyStatePanel.setLayout(new BoxLayout(emptyStatePanel, BoxLayout.Y_AXIS));
        emptyStatePanel.setOpaque(false);
        emptyStatePanel.setBorder(new EmptyBorder(80, 24, 28, 24));
        
        JLabel icoUpload = new JLabel();
        try {
            java.net.URL url = getClass().getResource("/images/icon/video_posting.png");
            if (url != null) {
                Image img = new ImageIcon(url).getImage().getScaledInstance(56, 56, Image.SCALE_SMOOTH);
                icoUpload.setIcon(new ImageIcon(img));
            }
        } catch (Exception ignored) {}
        icoUpload.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyStatePanel.add(icoUpload);
        emptyStatePanel.add(Box.createVerticalStrut(12));

        lblDropInstruction = new JLabel("Kéo & thả hoặc click để chọn tệp");
        lblDropInstruction.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblDropInstruction.setForeground(PURPLE_DARK);
        lblDropInstruction.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyStatePanel.add(lblDropInstruction);

        lblPreviewInfo = new JLabel("Ảnh: JPG, PNG, WEBP (Tối đa 8MB)");
        lblPreviewInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPreviewInfo.setForeground(TEXT_MUTED);
        lblPreviewInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyStatePanel.add(Box.createVerticalStrut(6));
        emptyStatePanel.add(lblPreviewInfo);

        lblImagePreview = new JLabel();
        lblImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagePreview.setVisible(false);

        dropZone.add(emptyStatePanel, BorderLayout.CENTER);
        dropZone.add(lblImagePreview, BorderLayout.NORTH);

        try {
            DropTarget dt = new DropTarget();
            dt.addDropTargetListener(new DropTargetAdapter() {
                @Override public void dragEnter(DropTargetDragEvent e) {
                    if (isLoading) return;
                    isDragOver = true; lblDropInstruction.setText("Thả để tải lên!"); dropZone.repaint();
                }
                @Override public void dragExit(DropTargetEvent e) {
                    if (isLoading) return;
                    isDragOver = false; lblDropInstruction.setText("Kéo & thả hoặc click để chọn tệp"); dropZone.repaint();
                }
                @Override public void drop(DropTargetDropEvent e) {
                    if (isLoading) return;
                    isDragOver = false;
                    try {
                        e.acceptDrop(DnDConstants.ACTION_COPY);
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) handleFileSelected(files.get(0));
                    } catch (Exception ex) { ex.printStackTrace(); }
                    dropZone.repaint();
                }
            });
            dropZone.setDropTarget(dt);
        } catch (java.util.TooManyListenersException ignored) {}

        dropZone.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { 
                if (!isLoading) openFileChooser(); 
            }
        });

        return dropZone;
    }

    private JPanel buildCaptionBox() {
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        txtCaption = new JTextPane() {
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
        txtCaption.setText("Nhập mô tả ngắn...");
        txtCaption.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtCaption.setForeground(TEXT_MUTED);
        txtCaption.setOpaque(false);
        txtCaption.setBorder(new EmptyBorder(10, 14, 10, 14));
        txtCaption.setPreferredSize(new Dimension(100, 60));

        txtCaption.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (txtCaption.getText().equals("Nhập mô tả ngắn...")) {
                    txtCaption.setText("");
                    txtCaption.setForeground(TEXT_MAIN);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (txtCaption.getText().trim().isEmpty()) {
                    txtCaption.setText("Nhập mô tả ngắn...");
                    txtCaption.setForeground(TEXT_MUTED);
                }
            }
        });

        lblCharCount = new JLabel("0/150");
        lblCharCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCharCount.setForeground(TEXT_MUTED);
        lblCharCount.setHorizontalAlignment(SwingConstants.RIGHT);

        txtCaption.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                int len = txtCaption.getText().length();
                if (txtCaption.getText().equals("Nhập mô tả ngắn...")) len = 0;
                lblCharCount.setText(len + "/150");
                if (len > 150) txtCaption.setForeground(new Color(0xEF4444));
                else txtCaption.setForeground(TEXT_MAIN);
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
        });

        wrap.add(txtCaption, BorderLayout.CENTER);
        wrap.add(lblCharCount, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(makeSepBorder(), new EmptyBorder(0, 20, 4, 20)));

        btnCancel = new JButton("Hủy") {
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
        btnCancel.addActionListener(e -> {
            if (!isLoading) dispose();
        });

        btnPost = new JButton("  🚀  Đăng ngay") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isLoading) {
                    g2.setColor(Color.LIGHT_GRAY);
                } else {
                    GradientPaint gp = new GradientPaint(0, 0, new Color(0xA78BFA), getWidth(), 0, PURPLE_DARK);
                    g2.setPaint(gp);
                }
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
            if (isLoading) return;
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn ảnh!", "Chưa chọn file", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (getCaption().length() > 150) {
                JOptionPane.showMessageDialog(this, "Mô tả không được vượt quá 150 ký tự.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (postListener != null) {
                postListener.onPostClicked(selectedFile, getCaption());
            }
        });

        footer.add(btnCancel);
        footer.add(btnPost);
        return footer;
    }

    private void openFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn ảnh Locket");
        chooser.setFileFilter(new FileNameExtensionFilter("Ảnh (JPG, PNG, WEBP)", "jpg", "jpeg", "png", "webp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            handleFileSelected(chooser.getSelectedFile());
        }
    }

    private void handleFileSelected(File file) {
        if (!file.exists() || !file.isFile()) {
            showError("File không tồn tại hoặc không hợp lệ.");
            return;
        }

        long sizeInBytes = file.length();
        if (sizeInBytes > 8 * 1024 * 1024) {
            showError("File quá lớn. Tối đa 8MB.");
            return;
        }

        String name = file.getName().toLowerCase();
        if (!name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".png") && !name.endsWith(".webp")) {
            showError("Vui lòng chọn ảnh định dạng JPG, PNG hoặc WEBP.");
            return;
        }

        try {
            Image img = ImageIO.read(file);
            if (img == null) {
                if (name.endsWith(".webp")) {
                    showError("Định dạng WebP chưa được hỗ trợ trên máy này, vui lòng chọn JPG/PNG.");
                } else {
                    showError("File không đúng định dạng ảnh hoặc bị hỏng.");
                }
                return;
            }
            
            selectedFile = file;
            
            int maxWidth = 400;
            int maxHeight = 300;
            
            int originalWidth = img.getWidth(null);
            int originalHeight = img.getHeight(null);
            
            int newWidth = originalWidth;
            int newHeight = originalHeight;
            
            if (originalWidth > maxWidth || originalHeight > maxHeight) {
                double widthRatio = (double) maxWidth / originalWidth;
                double heightRatio = (double) maxHeight / originalHeight;
                double ratio = Math.min(widthRatio, heightRatio);
                
                newWidth = (int) (originalWidth * ratio);
                newHeight = (int) (originalHeight * ratio);
            }
            
            Image scaledImg = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            lblImagePreview.setIcon(new ImageIcon(scaledImg));
            lblImagePreview.setPreferredSize(new Dimension(newWidth, newHeight));
            lblImagePreview.setVisible(true);
            
            // Hide the empty state instruction components
            for (Component c : dropZone.getComponents()) {
                if (c instanceof JPanel) {
                    c.setVisible(false);
                }
            }
            
            dropZone.revalidate();
            dropZone.repaint();

        } catch (Exception e) {
            showError("Không thể đọc file ảnh: " + e.getMessage());
        }
    }

    private String getCaption() {
        String t = txtCaption.getText();
        if (t.equals("Nhập mô tả ngắn...")) return "";
        return t.trim();
    }

    public void setLoading(boolean loading) {
        this.isLoading = loading;
        if (loading) {
            btnPost.setText("Đang upload...");
            btnPost.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            txtCaption.setEnabled(false);
        } else {
            btnPost.setText("  🚀  Đăng ngay");
            btnPost.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            txtCaption.setEnabled(true);
        }
        btnPost.repaint();
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
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
}
