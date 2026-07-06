package com.mycompany.tutorhub_enterprise.client.search;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

/**
 * Google Lens-style split panel for analyzing images and documents.
 * Layout:
 *  - Left panel: Image/file preview with crop area
 *  - Right panel: AI streaming response + follow-up chat input
 *
 * Uses the same API endpoints as LavieChatWidget:
 *  - Vision: https://hocbatrolai293-tutorhub-ai.hf.space/api/chat/vision
 *  - Document: https://hocbatrolai293-tutorhub-ai.hf.space/api/chat/document
 */
public class LensResultPanel extends JDialog {

    // ─── Design Tokens ───────────────────────────────────────
    private static final Color BG_DARK       = new Color(0x202124);
    private static final Color BG_LEFT       = new Color(0x303134);
    private static final Color BG_RIGHT      = new Color(0xFFFFFF);
    private static final Color TEXT_WHITE     = new Color(0xE8EAED);
    private static final Color TEXT_PRIMARY   = new Color(0x202124);
    private static final Color TEXT_MUTED     = new Color(0x5F6368);
    private static final Color ACCENT         = new Color(0x1A73E8);
    private static final Color INPUT_BG       = new Color(0xF1F3F4);
    private static final Color DIVIDER        = new Color(0xE8EAED);

    // ─── State ───────────────────────────────────────────────
    private File currentFile;
    private String currentUrl;
    private JTextPane resultPane;
    private JTextField followUpInput;
    private JPanel leftPanel;
    private JLabel imageLabel;
    private JLabel statusLabel;
    private StringBuilder fullResponse = new StringBuilder();
    private boolean isAnalyzing = false;

    public LensResultPanel(JFrame parent) {
        super(parent, "TutorHub Lens", false);
        setUndecorated(true);
        setSize(880, 560);
        setLocationRelativeTo(parent);
        setBackground(new Color(0, 0, 0, 0));

        JPanel rootPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow
                for (int i = 0; i < 8; i++) {
                    g2.setColor(new Color(0, 0, 0, Math.max(0, 18 - i * 2)));
                    g2.fillRoundRect(6 - i, 6 - i + 2, getWidth() - 12 + i * 2, getHeight() - 12 + i * 2, 20 + i, 20 + i);
                }
                // Main bg
                g2.setColor(BG_RIGHT);
                g2.fillRoundRect(6, 6, getWidth() - 12, getHeight() - 12, 16, 16);
                g2.dispose();
            }
        };
        rootPanel.setOpaque(false);

        // ── Title bar ──
        JPanel titleBar = buildTitleBar();
        rootPanel.add(titleBar, BorderLayout.NORTH);

        // ── Content: left preview + right results ──
        JPanel content = new JPanel(new GridLayout(1, 2, 0, 0));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 6, 6, 6));

        leftPanel = buildLeftPanel();
        JPanel rightPanel = buildRightPanel();

        content.add(leftPanel);
        content.add(rightPanel);

        rootPanel.add(content, BorderLayout.CENTER);
        setContentPane(rootPanel);

        // Drag to move
        MouseAdapter dragAdapter = new MouseAdapter() {
            int px, py;
            @Override public void mousePressed(MouseEvent e) { px = e.getX(); py = e.getY(); }
            @Override public void mouseDragged(MouseEvent e) {
                setLocation(getLocation().x + e.getX() - px, getLocation().y + e.getY() - py);
            }
        };
        titleBar.addMouseListener(dragAdapter);
        titleBar.addMouseMotionListener(dragAdapter);
    }

    // ─── Public API ──────────────────────────────────────────

    public void analyzeFile(File file) {
        this.currentFile = file;
        this.currentUrl = null;
        fullResponse.setLength(0);
        resultPane.setText("");

        loadPreview(file);
        setVisible(true);
        toFront();
        startAnalysis();
    }

    public void analyzeUrl(String imageUrl) {
        this.currentUrl = imageUrl;
        this.currentFile = null;
        fullResponse.setLength(0);
        resultPane.setText("");

        loadPreviewFromUrl(imageUrl);
        setVisible(true);
        toFront();
        startAnalysisFromUrl(imageUrl);
    }

    // ─── UI Builders ─────────────────────────────────────────

    private JPanel buildTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_DARK);
                g2.fillRoundRect(6, 0, getWidth() - 12, getHeight() + 10, 16, 16);
                g2.fillRect(6, getHeight() - 10, getWidth() - 12, 10);
                g2.dispose();
            }
        };
        titleBar.setOpaque(false);
        titleBar.setPreferredSize(new Dimension(0, 44));
        titleBar.setBorder(new EmptyBorder(8, 16, 8, 12));

        // Back button + title
        JPanel leftPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPart.setOpaque(false);

        JButton btnBack = createTitleButton("←");
        btnBack.addActionListener(e -> dispose());
        leftPart.add(btnBack);

        JLabel title = new JLabel("TutorHub Lens");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TEXT_WHITE);
        leftPart.add(title);

        // Close button
        JButton btnClose = createTitleButton("✕");
        btnClose.addActionListener(e -> dispose());

        titleBar.add(leftPart, BorderLayout.WEST);
        titleBar.add(btnClose, BorderLayout.EAST);

        return titleBar;
    }

    private JButton createTitleButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 25));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.setColor(TEXT_WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setPreferredSize(new Dimension(32, 28));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        return btn;
    }

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_LEFT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                if (getIcon() != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    super.paintComponent(g);
                    g2.dispose();
                } else {
                    super.paintComponent(g);
                }
            }
        };
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setForeground(TEXT_WHITE);
        imageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        imageLabel.setText("📁");

        panel.add(imageLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(BG_RIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 16, 12, 16));

        // ── Header ──
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel resultTitle = new JLabel("📌 Kết quả phân tích");
        resultTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        resultTitle.setForeground(TEXT_PRIMARY);

        statusLabel = new JLabel("Đang chuẩn bị...");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(TEXT_MUTED);

        headerRow.add(resultTitle, BorderLayout.WEST);
        headerRow.add(statusLabel, BorderLayout.EAST);
        panel.add(headerRow, BorderLayout.NORTH);

        // ── Result content ──
        resultPane = new JTextPane();
        resultPane.setContentType("text/html");
        resultPane.setEditable(false);
        resultPane.setOpaque(false);
        resultPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        resultPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        resultPane.setBorder(new EmptyBorder(10, 0, 10, 0));
        resultPane.setText("<html><body style='font-family: Segoe UI; font-size: 13px; color: #5F6368;'>Chọn ảnh hoặc tệp để bắt đầu phân tích...</body></html>");

        JScrollPane scrollPane = new JScrollPane(resultPane);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, BorderLayout.CENTER);

        // ── Follow-up input ──
        JPanel inputRow = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(INPUT_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(DIVIDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        inputRow.setOpaque(false);
        inputRow.setBorder(new EmptyBorder(6, 14, 6, 6));
        inputRow.setPreferredSize(new Dimension(0, 40));

        followUpInput = new JTextField();
        followUpInput.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        followUpInput.setForeground(TEXT_PRIMARY);
        followUpInput.setOpaque(false);
        followUpInput.setBorder(null);
        followUpInput.putClientProperty("JTextField.placeholderText", "Hỏi thêm về ảnh/tệp này...");
        followUpInput.addActionListener(e -> sendFollowUp());

        JButton btnSend = new JButton("▶") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(0x174EA6));
                } else {
                    g2.setColor(ACCENT);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btnSend.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSend.setPreferredSize(new Dimension(36, 28));
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.setContentAreaFilled(false);
        btnSend.setBorderPainted(false);
        btnSend.setFocusPainted(false);
        btnSend.addActionListener(e -> sendFollowUp());

        inputRow.add(followUpInput, BorderLayout.CENTER);
        inputRow.add(btnSend, BorderLayout.EAST);

        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setOpaque(false);
        bottomWrapper.setBorder(new EmptyBorder(8, 0, 0, 0));
        bottomWrapper.add(inputRow, BorderLayout.CENTER);
        panel.add(bottomWrapper, BorderLayout.SOUTH);

        return panel;
    }

    // ─── Preview Loading ─────────────────────────────────────

    private void loadPreview(File file) {
        String name = file.getName().toLowerCase();
        boolean isImage = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".bmp");

        if (isImage) {
            new Thread(() -> {
                try {
                    Image img = javax.imageio.ImageIO.read(file);
                    if (img != null) {
                        int pw = leftPanel.getWidth() - 40;
                        int ph = leftPanel.getHeight() - 40;
                        if (pw <= 0) pw = 380;
                        if (ph <= 0) ph = 400;
                        Image scaled = scaleToFit(img, pw, ph);
                        SwingUtilities.invokeLater(() -> {
                            imageLabel.setText(null);
                            imageLabel.setIcon(new ImageIcon(scaled));
                        });
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setText("🖼️");
                    });
                }
            }).start();
        } else {
            // Document icon
            String emoji = "📄";
            if (name.endsWith(".docx") || name.endsWith(".doc")) emoji = "📝";
            else if (name.endsWith(".txt")) emoji = "📋";
            imageLabel.setText(emoji);
            imageLabel.setIcon(null);

            // Also show filename
            JLabel fileNameLabel = new JLabel(file.getName());
            fileNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            fileNameLabel.setForeground(TEXT_WHITE);
            fileNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
            leftPanel.add(fileNameLabel, BorderLayout.SOUTH);
        }
    }

    private void loadPreviewFromUrl(String urlStr) {
        SwingUtilities.invokeLater(() -> {
            imageLabel.setText("⏳");
            imageLabel.setIcon(null);
            statusLabel.setText("Đang tải ảnh...");
        });

        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                Image img = javax.imageio.ImageIO.read(url);
                if (img != null) {
                    int pw = leftPanel.getWidth() - 40;
                    int ph = leftPanel.getHeight() - 40;
                    if (pw <= 0) pw = 380;
                    if (ph <= 0) ph = 400;
                    Image scaled = scaleToFit(img, pw, ph);

                    // Save to temp file for API upload
                    BufferedImage bimg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = bimg.createGraphics();
                    g2d.drawImage(img, 0, 0, null);
                    g2d.dispose();
                    File tmpFile = File.createTempFile("lens_url_", ".png");
                    javax.imageio.ImageIO.write(bimg, "png", tmpFile);
                    this.currentFile = tmpFile;

                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setText(null);
                        imageLabel.setIcon(new ImageIcon(scaled));
                    });

                    startAnalysis();
                } else {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Không thể tải ảnh từ URL");
                        imageLabel.setText("❌");
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Lỗi: " + ex.getMessage());
                    imageLabel.setText("❌");
                });
            }
        }).start();
    }

    private Image scaleToFit(Image img, int maxW, int maxH) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        if (w <= 0 || h <= 0) return img;

        double scaleW = (double) maxW / w;
        double scaleH = (double) maxH / h;
        double scale = Math.min(scaleW, scaleH);
        if (scale >= 1.0) return img;

        int nw = (int)(w * scale);
        int nh = (int)(h * scale);
        return img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
    }

    // ─── AI Analysis ─────────────────────────────────────────

    private void startAnalysis() {
        if (currentFile == null || isAnalyzing) return;
        isAnalyzing = true;

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Đang phân tích...");
            resultPane.setText("<html><body style='font-family: Segoe UI; font-size: 13px; color: #5F6368;'><i>AI đang đọc và phân tích nội dung...</i></body></html>");
        });

        String name = currentFile.getName().toLowerCase();
        boolean isDocument = name.endsWith(".pdf") || name.endsWith(".docx") || name.endsWith(".doc") || name.endsWith(".txt");

        String apiUrl = isDocument
                ? "https://hocbatrolai293-tutorhub-ai.hf.space/api/chat/document"
                : "https://hocbatrolai293-tutorhub-ai.hf.space/api/chat/vision";
        String fileField = "image";
        String fileType = isDocument ? "application/octet-stream" : "image/png";
        String message = isDocument
                ? "Hãy phân tích chi tiết nội dung tài liệu này. Trích xuất các chủ đề chính, tóm tắt nội dung, và cung cấp thông tin hữu ích."
                : "Hãy phân tích chi tiết hình ảnh này. Mô tả nội dung, nhận diện các đối tượng, trích xuất văn bản nếu có, và cung cấp thông tin liên quan.";

        new Thread(() -> {
            try {
                sendMultipartAndStream(apiUrl, currentFile, fileField, fileType, message);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Lỗi phân tích");
                    appendToResult("❌ Lỗi kết nối: " + ex.getMessage());
                });
            } finally {
                isAnalyzing = false;
            }
        }).start();
    }

    private void startAnalysisFromUrl(String imageUrl) {
        // loadPreviewFromUrl downloads and saves to currentFile, then calls startAnalysis
        // So this is handled automatically
    }

    private void sendFollowUp() {
        String text = followUpInput.getText().trim();
        if (text.isEmpty() || currentFile == null) return;
        followUpInput.setText("");
        followUpInput.setEnabled(false);

        fullResponse.setLength(0);
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Đang xử lý...");
            resultPane.setText("<html><body style='font-family: Segoe UI; font-size: 13px; color: #5F6368;'><b>Câu hỏi:</b> " + text + "<br><br><i>Đang phân tích...</i></body></html>");
        });

        String name = currentFile.getName().toLowerCase();
        boolean isDocument = name.endsWith(".pdf") || name.endsWith(".docx") || name.endsWith(".doc") || name.endsWith(".txt");

        String apiUrl = isDocument
                ? "https://hocbatrolai293-tutorhub-ai.hf.space/api/chat/document"
                : "https://hocbatrolai293-tutorhub-ai.hf.space/api/chat/vision";
        String fileField = "image";
        String fileType = isDocument ? "application/octet-stream" : "image/png";

        new Thread(() -> {
            try {
                sendMultipartAndStream(apiUrl, currentFile, fileField, fileType, text);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Lỗi");
                    appendToResult("❌ Lỗi: " + ex.getMessage());
                });
            } finally {
                SwingUtilities.invokeLater(() -> followUpInput.setEnabled(true));
            }
        }).start();
    }

    // ─── HTTP Multipart & Streaming ──────────────────────────

    private void sendMultipartAndStream(String requestURL, File uploadFile, String fileField, String fileType, String extraMessage) throws Exception {
        String boundary = "===LensBoundary" + System.currentTimeMillis() + "===";
        HttpURLConnection conn = (HttpURLConnection) new URL(requestURL).openConnection();
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream outputStream = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true)) {

            // File part
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"").append(fileField).append("\"; filename=\"").append(uploadFile.getName()).append("\"\r\n");
            writer.append("Content-Type: ").append(fileType).append("\r\n");
            writer.append("\r\n").flush();
            Files.copy(uploadFile.toPath(), outputStream);
            outputStream.flush();
            writer.append("\r\n").flush();

            // user_id
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"user_id\"\r\n\r\n");
            writer.append("java_lens_user").append("\r\n").flush();

            // message
            if (extraMessage != null && !extraMessage.isEmpty()) {
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"message\"\r\n\r\n");
                writer.append(extraMessage).append("\r\n").flush();
            }

            writer.append("--").append(boundary).append("--\r\n").flush();
        }

        // Read SSE stream
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Server returned HTTP " + responseCode);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
        String line;
        fullResponse.setLength(0);

        while ((line = br.readLine()) != null) {
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                if (data.equals("[DONE]")) break;
                if (data.isEmpty()) continue;

                String chunk = extractJsonValue(data, "chunk");
                if (chunk == null) chunk = extractJsonValue(data, "content");
                if (chunk == null) chunk = extractJsonValue(data, "response");

                if (chunk != null) {
                    chunk = chunk.replace("\\n", "\n").replace("\\\"", "\"");
                    fullResponse.append(chunk);

                    final String display = formatAsHtml(fullResponse.toString());
                    SwingUtilities.invokeLater(() -> {
                        resultPane.setText(display);
                        statusLabel.setText("Đang nhận kết quả...");
                        // Auto scroll
                        resultPane.setCaretPosition(resultPane.getDocument().getLength());
                    });
                }
            }
        }
        br.close();

        // Final
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Hoàn tất ✓");
            if (fullResponse.length() == 0) {
                // Try to read non-streaming response
                try {
                    conn.getInputStream().close();
                } catch (Exception ignored) {}
            }
        });
    }

    // ─── Utilities ───────────────────────────────────────────

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) return null;

        int start = colonIndex + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;

        if (start >= json.length()) return null;

        if (json.charAt(start) == '"') {
            int end = start + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                end++;
            }
            return json.substring(start + 1, end);
        }
        return null;
    }

    private void appendToResult(String text) {
        fullResponse.append(text);
        resultPane.setText(formatAsHtml(fullResponse.toString()));
    }

    private String formatAsHtml(String text) {
        if (text == null || text.isEmpty()) return "";

        String html = text;
        html = html.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        html = html.replace("\n", "<br>");
        // Bold
        html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
        // Italic
        html = html.replaceAll("\\*(.*?)\\*", "<i>$1</i>");
        // Inline code
        html = html.replaceAll("`(.*?)`", "<code style='background:#F1F3F4; padding:2px 4px; border-radius:3px; color:#D93025;'>$1</code>");

        return "<html><body style='font-family: Segoe UI, sans-serif; font-size: 13px; color: #202124; line-height: 1.6;'>"
                + html + "</body></html>";
    }
}
