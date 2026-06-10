package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class BlackboardManagerTab extends JPanel {
    private final Color bg = Color.WHITE;
    private final Color primary = Color.decode("#7C3AED");
    private final Color primaryBlue = Color.decode("#2563EB");
    private final Color textDark = Color.decode("#111827");
    private final Color textMuted = Color.decode("#6B7280");
    private final Color border = Color.decode("#E5E7EB");

    private final MainDashboard dashboard;
    private final List<BoardMeta> originalBoards = new ArrayList<>();
    private final List<BoardMeta> currentBoards = new ArrayList<>();

    private JPanel listPanel;
    private JPanel paginationPanel;
    private JPanel tabsContainer;
    private JTextField searchField;
    private String activeTab = "Tất cả";

    public static class BoardMeta {
        public String boardId, title, lastModified, sizeMB, thumbnailUrl;
        public int totalPages;
        public boolean isCurrent;

        public BoardMeta(String id, String title, String lastModified, String sizeMB, int totalPages, boolean isCurrent, String thumbnailUrl) {
            this.boardId = id;
            this.title = title;
            this.lastModified = lastModified;
            this.sizeMB = sizeMB;
            this.totalPages = totalPages;
            this.isCurrent = isCurrent;
            this.thumbnailUrl = thumbnailUrl;
        }
    }

    public BlackboardManagerTab(MainDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout());
        setBackground(bg);
        setBorder(new EmptyBorder(10, 30, 20, 30));
        initUI();
    }

    private void initUI() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setOpaque(false);

        main.add(Box.createVerticalStrut(10));
        main.add(createHeader());
        main.add(Box.createVerticalStrut(18));
        main.add(createBoardActionStrip());
        main.add(Box.createVerticalStrut(22));
        main.add(createFilterBar());
        main.add(Box.createVerticalStrut(15));
        main.add(createListHeader());
        main.add(Box.createVerticalStrut(10));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scrollPane);

        paginationPanel = createPaginationFooter();
        main.add(paginationPanel);

        add(main, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel title = new JLabel("Bảng vẽ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(textDark);
        JLabel subtitle = new JLabel("Tạo, mở lại và quản lý các bảng vẽ đã lưu");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(textMuted);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);

        JButton create = new JButton("+ Tạo bảng vẽ");
        create.setFont(new Font("Segoe UI", Font.BOLD, 13));
        create.setForeground(Color.WHITE);
        create.setBackground(primary);
        create.setFocusPainted(false);
        create.setBorder(new EmptyBorder(9, 16, 9, 16));
        create.setCursor(new Cursor(Cursor.HAND_CURSOR));
        create.addActionListener(e -> {
            if (dashboard != null) dashboard.openNewBlackboard();
        });

        header.add(titleBox, BorderLayout.WEST);
        header.add(create, BorderLayout.EAST);
        return header;
    }

    private JPanel createBoardActionStrip() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint paint = new GradientPaint(0, 0, Color.decode("#F3F0FF"), getWidth(), getHeight(), Color.decode("#EEF2FF"));
                g2.setPaint(paint);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                if (Boolean.TRUE.equals(getClientProperty("hover"))) {
                    g2.setColor(new Color(255, 255, 255, 80));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Tạo bảng vẽ mới");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(textDark);
        JLabel desc = new JLabel("Mở một bảng vẽ độc lập để ghi chú, soạn bài hoặc chia sẻ ý tưởng.");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(textMuted);
        copy.add(title);
        copy.add(Box.createVerticalStrut(4));
        copy.add(desc);

        JLabel action = new JLabel("+  Bắt đầu", SwingConstants.CENTER);
        action.setFont(new Font("Segoe UI", Font.BOLD, 14));
        action.setForeground(primary);
        action.setPreferredSize(new Dimension(110, 36));

        card.add(copy, BorderLayout.CENTER);
        card.add(action, BorderLayout.EAST);
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { card.putClientProperty("hover", true); card.repaint(); }
            @Override public void mouseExited(MouseEvent e) { card.putClientProperty("hover", false); card.repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                if (dashboard != null) dashboard.openNewBlackboard();
            }
        });
        return card;
    }

    public void syncBoardsFromServer(String payload) {
        SwingUtilities.invokeLater(() -> {
            originalBoards.clear();
            if (payload != null && !payload.trim().isEmpty() && !"EMPTY".equals(payload)) {
                String[] rows = payload.split(";;");
                for (String row : rows) {
                    if (row.trim().isEmpty()) continue;
                    String[] cols = row.split("\\|");
                    if (cols.length >= 8) {
                        originalBoards.add(new BoardMeta(cols[0], cols[1], cols[3], cols[4], parseInt(cols[5], 1), Boolean.parseBoolean(cols[6]), cols[7]));
                    }
                }
            }
            applyFilters();
        });
    }

    public void showSaveSuccessBanner() {
        JOptionPane.showMessageDialog(this, "Lưu bảng vẽ thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel createFilterBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, border));

        tabsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 0));
        tabsContainer.setOpaque(false);
        renderTabs();

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        right.setOpaque(false);
        right.add(createSearchBox());
        right.add(createSortButton());

        bar.add(tabsContainer, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createSearchBox() {
        JPanel box = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        box.setOpaque(false);
        box.setBorder(new EmptyBorder(5, 10, 5, 10));
        box.setPreferredSize(new Dimension(250, 36));
        JLabel icon = new JLabel("⌕");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 17));
        icon.setForeground(textMuted);

        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createEmptyBorder());
        searchField.setOpaque(false);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm bảng vẽ...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        box.add(icon, BorderLayout.WEST);
        box.add(searchField, BorderLayout.CENTER);
        return box;
    }

    private JPanel createSortButton() {
        JPanel button = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel label = new JLabel("Sắp xếp ˅");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(textDark);
        button.add(label);
        button.addMouseListener(new MouseAdapter() {
            private boolean ascending;
            @Override
            public void mouseClicked(MouseEvent e) {
                ascending = !ascending;
                final boolean asc = ascending;
                originalBoards.sort((a, b) -> asc ? a.lastModified.compareTo(b.lastModified) : b.lastModified.compareTo(a.lastModified));
                label.setText(asc ? "Cũ nhất ↑" : "Mới nhất ↓");
                applyFilters();
            }
        });
        return button;
    }

    private void renderTabs() {
        tabsContainer.removeAll();
        for (String tab : new String[]{"Tất cả", "Gần đây", "Của tôi", "Đã chia sẻ"}) {
            boolean active = activeTab.equals(tab);
            JPanel item = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    if (active) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(primaryBlue);
                        g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            item.setOpaque(false);
            item.setBorder(new EmptyBorder(10, 5, 12, 5));
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            JLabel label = new JLabel(tab);
            label.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 14));
            label.setForeground(active ? primaryBlue : textMuted);
            item.add(label, BorderLayout.CENTER);
            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    activeTab = tab;
                    renderTabs();
                    applyFilters();
                }
            });
            tabsContainer.add(item);
        }
        tabsContainer.revalidate();
        tabsContainer.repaint();
    }

    private void applyFilters() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        currentBoards.clear();

        long now = System.currentTimeMillis();
        long oneDay = 24L * 60 * 60 * 1000;
        for (BoardMeta board : originalBoards) {
            boolean searchMatch = query.isEmpty() || safe(board.title).toLowerCase().contains(query);
            boolean tabMatch = true;
            if ("Gần đây".equals(activeTab)) {
                tabMatch = false;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(board.lastModified);
                    tabMatch = now - date.getTime() <= oneDay;
                } catch (Exception ignored) {}
            } else if ("Đã chia sẻ".equals(activeTab)) {
                tabMatch = false;
            }
            if (searchMatch && tabMatch) {
                currentBoards.add(board);
            }
        }
        renderBoardList();
    }

    private JPanel createListHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 20, 0, 20));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        header.add(createHeaderLabel("Tên bảng vẽ", 420));
        header.add(createHeaderLabel("Sửa lần cuối", 200));
        header.add(createHeaderLabel("Kích thước", 150));
        header.add(createHeaderLabel("Thao tác", 150));
        return header;
    }

    private JLabel createHeaderLabel(String text, int width) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(textDark);
        setFixedSize(label, width, 20);
        return label;
    }

    private void renderBoardList() {
        listPanel.removeAll();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        if (currentBoards.isEmpty()) {
            showEmptyState();
            paginationPanel.setVisible(false);
        } else {
            for (BoardMeta board : currentBoards) {
                listPanel.add(createBoardRow(board));
                listPanel.add(Box.createVerticalStrut(12));
            }
            paginationPanel.setVisible(true);
            updatePaginationText();
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void showEmptyState() {
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setBorder(new EmptyBorder(52, 20, 20, 20));
        JLabel title = new JLabel(originalBoards.isEmpty() ? "Bạn chưa có bảng vẽ nào." : "Không tìm thấy bảng vẽ phù hợp.");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(textDark);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel desc = new JLabel("Dùng nút Tạo bảng vẽ mới để bắt đầu một board độc lập.");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(textMuted);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        empty.add(title);
        empty.add(Box.createVerticalStrut(8));
        empty.add(desc);
        listPanel.add(empty);
    }

    private JPanel createBoardRow(BoardMeta board) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel row = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel col1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        col1.setOpaque(false);
        setFixedSize(col1, 420, 70);
        col1.add(createThumbnail(board));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(safe(board.title));
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(textDark);
        JLabel pages = new JLabel(board.totalPages + " trang" + (board.isCurrent ? "  ·  Hiện tại" : ""));
        pages.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pages.setForeground(textMuted);
        titleBox.add(Box.createVerticalStrut(8));
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(8));
        titleBox.add(pages);
        col1.add(titleBox);

        JPanel col2 = new JPanel();
        col2.setOpaque(false);
        col2.setLayout(new BoxLayout(col2, BoxLayout.Y_AXIS));
        setFixedSize(col2, 200, 70);
        String[] dateTime = splitDateTime(board.lastModified);
        col2.add(Box.createVerticalStrut(8));
        col2.add(createPlainLabel(dateTime[0], textDark));
        col2.add(Box.createVerticalStrut(8));
        col2.add(createPlainLabel(dateTime[1], textMuted));

        JPanel col3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 22));
        col3.setOpaque(false);
        setFixedSize(col3, 150, 70);
        col3.add(createPlainLabel(safe(board.sizeMB) + " MB", textDark));

        JPanel col4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 12));
        col4.setOpaque(false);
        setFixedSize(col4, 150, 70);
        JButton open = new JButton("Mở lại");
        open.setFont(new Font("Segoe UI", Font.BOLD, 12));
        open.setForeground(primaryBlue);
        open.setBackground(Color.WHITE);
        open.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.decode("#BFDBFE")), new EmptyBorder(6, 18, 6, 18)));
        open.setFocusPainted(false);
        open.setCursor(new Cursor(Cursor.HAND_CURSOR));
        open.addActionListener(e -> {
            if (dashboard != null) dashboard.openExistingBlackboard(board);
        });
        JLabel more = new JLabel("...", SwingConstants.CENTER);
        more.setFont(new Font("Segoe UI", Font.BOLD, 18));
        more.setForeground(textMuted);
        more.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setupRowMenu(more, board);
        col4.add(open);
        col4.add(more);

        row.add(col1);
        row.add(col2);
        row.add(col3);
        row.add(col4);
        wrapper.add(row, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createThumbnail(BoardMeta board) {
        Image image = parseBase64Image(board.thumbnailUrl);
        JPanel thumb = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                if (image != null) {
                    g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                } else {
                    GradientPaint paint = new GradientPaint(0, 0, Color.decode("#111827"), getWidth(), getHeight(), Color.decode("#3730A3"));
                    g2.setPaint(paint);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(new Color(255, 255, 255, 180));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    g2.drawString("TH", 34, 38);
                }
                g2.dispose();
            }
        };
        thumb.setPreferredSize(new Dimension(100, 60));
        thumb.setOpaque(false);
        return thumb;
    }

    private void setupRowMenu(JLabel trigger, BoardMeta board) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(border));
        addMenuItem(popup, "Mở lại", () -> {
            if (dashboard != null) dashboard.openExistingBlackboard(board);
        }, false);
        addMenuItem(popup, "Đổi tên", () -> renameBoard(board), false);
        addMenuItem(popup, "Chia sẻ qua Zalo", () -> shareBoard(board), false);
        addMenuItem(popup, "Xóa bảng", () -> deleteBoard(board), true);
        trigger.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                popup.show(trigger, -130, trigger.getHeight());
            }
        });
    }

    private void addMenuItem(JPopupMenu popup, String label, Runnable action, boolean danger) {
        JMenuItem item = new JMenuItem("  " + label + "  ");
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        item.setPreferredSize(new Dimension(160, 32));
        if (danger) item.setForeground(Color.decode("#DC2626"));
        item.addActionListener(e -> action.run());
        popup.add(item);
    }

    private void renameBoard(BoardMeta board) {
        String newName = JOptionPane.showInputDialog(this, "Nhập tên mới cho bảng:", board.title);
        if (newName == null || newName.trim().isEmpty()) return;
        try {
            NetworkManager.getInstance().sendPacket(new Packet("RENAME_BOARD", board.boardId + "|" + newName.trim()));
        } catch (Exception ignored) {}
        board.title = newName.trim();
        applyFilters();
        JOptionPane.showMessageDialog(this, "Cập nhật tên thành công!");
    }

    private void shareBoard(BoardMeta board) {
        StringSelection selection = new StringSelection("Mời bạn xem bảng vẽ: " + board.title + "\nhttps://tutorhub.vn/b/" + board.boardId);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        try {
            Desktop.getDesktop().browse(new URI("https://chat.zalo.me/"));
        } catch (Exception ignored) {}
        JOptionPane.showMessageDialog(this, "Đã sao chép link bảng vẽ. Hãy dán vào Zalo.");
    }

    private void deleteBoard(BoardMeta board) {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa vĩnh viễn bảng vẽ này?", "Xóa bảng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            NetworkManager.getInstance().sendPacket(new Packet("DELETE_BOARD", board.boardId));
        } catch (Exception ignored) {}
        originalBoards.removeIf(item -> item.boardId.equals(board.boardId));
        applyFilters();
    }

    private JPanel createPaginationFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(20, 0, 0, 0));
        JLabel info = new JLabel("");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        info.setForeground(textMuted);
        footer.add(info, BorderLayout.WEST);

        JPanel pages = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        pages.setOpaque(false);
        pages.add(createPageBtn("<", false, false));
        pages.add(createPageBtn("1", true, false));
        pages.add(createPageBtn("2", false, false));
        pages.add(createPageBtn("3", false, false));
        pages.add(createPageBtn(">", false, false));
        footer.add(pages, BorderLayout.EAST);
        return footer;
    }

    private void updatePaginationText() {
        if (paginationPanel != null && paginationPanel.getComponentCount() > 0) {
            JLabel info = (JLabel) paginationPanel.getComponent(0);
            info.setText("Hiển thị 1-" + currentBoards.size() + " trong tổng số " + currentBoards.size() + " bảng vẽ");
        }
    }

    private JPanel createPageBtn(String text, boolean active, boolean disabled) {
        JPanel button = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? Color.decode("#EFF6FF") : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(active ? primaryBlue : border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(32, 32));
        if (!disabled) button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(active ? primaryBlue : disabled ? Color.decode("#D1D5DB") : textDark);
        button.add(label, BorderLayout.CENTER);
        return button;
    }

    private JLabel createPlainLabel(String value, Color color) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(color);
        return label;
    }

    private String[] splitDateTime(String value) {
        if (value == null || value.trim().isEmpty()) return new String[]{"-", "-"};
        String[] parts = value.split(" ");
        if (parts.length >= 2) return new String[]{parts[0], parts[1]};
        return new String[]{value, "-"};
    }

    private Image parseBase64Image(String base64) {
        if (base64 == null || base64.equals("null") || base64.isEmpty()) return null;
        String value = base64.contains("###") ? base64.split("###")[0] : base64;
        try {
            byte[] bytes = Base64.getDecoder().decode(value);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Untitled" : value.trim();
    }

    private void setFixedSize(JComponent component, int width, int height) {
        Dimension size = new Dimension(width, height);
        component.setPreferredSize(size);
        component.setMaximumSize(size);
        component.setMinimumSize(size);
    }
}
