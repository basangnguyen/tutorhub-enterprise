package com.mycompany.tutorhub_enterprise.client;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TodoListTab extends JPanel {

    private static final String ICON_ROOT = "images/tasks/heroicons/";

    private final Color bgMain = Color.decode("#F6F8FB");
    private final Color surface = Color.WHITE;
    private final Color surfaceSoft = Color.decode("#F8FAFC");
    private final Color textMain = Color.decode("#111827");
    private final Color textMuted = Color.decode("#64748B");
    private final Color textSoft = Color.decode("#94A3B8");
    private final Color primary = Color.decode("#2563EB");
    private final Color border = Color.decode("#E2E8F0");
    private final Color success = Color.decode("#059669");
    private final Color warning = Color.decode("#D97706");
    private final Color danger = Color.decode("#DC2626");

    private final List<Task> tasks = new ArrayList<>();
    private final List<FilterChip> filterChips = new ArrayList<>();
    private JPanel listPanel;
    private JLabel totalValue;
    private JLabel pendingValue;
    private JLabel doneValue;
    private String currentFilter = "ALL";
    private String statusFilter = "ALL_STATUS";
    private String sortMode = "NEWEST";
    private Runnable onBackListener;

    public TodoListTab() {
        setLayout(new BorderLayout());
        setBackground(bgMain);
        setBorder(new EmptyBorder(0, 50, 0, 50));

        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
        add(createPagination(), BorderLayout.SOUTH);

        renderTaskList();
    }

    public void setOnBackListener(Runnable listener) {
        this.onBackListener = listener;
    }

    public void updateTasksFromServer(String payload) {
        SwingUtilities.invokeLater(() -> {
            tasks.clear();

            if (payload != null && !payload.trim().isEmpty()) {
                String[] rows = payload.split(";;");
                for (String row : rows) {
                    String[] cols = row.split("\\|", -1);
                    if (cols.length >= 6) {
                        String id = cleanServerText(cols[0]);
                        String category = cleanServerText(cols[1]).toUpperCase();
                        String title = cleanServerText(cols[2]);
                        String time = normalizeMeta(cleanServerText(cols[3]), "Thời gian đang cập nhật");
                        String location = normalizeMeta(cleanServerText(cols[4]), "Địa điểm đang cập nhật");
                        boolean completed = Boolean.parseBoolean(cols[5]);

                        tasks.add(createTask(id, category, title, time, location, completed));
                    }
                }
            }

            renderTaskList();
        });
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(42, 0, 26, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        left.setOpaque(false);
        left.add(createBackButton());

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));

        JLabel eyebrow = new JLabel("Trung tâm công việc");
        eyebrow.setFont(uiFont(Font.BOLD, 12));
        eyebrow.setForeground(primary);

        JLabel title = new JLabel("Việc cần làm");
        title.setFont(uiFont(Font.BOLD, 29));
        title.setForeground(textMain);

        JLabel sub = new JLabel("Theo dõi lớp dạy, bài tập, tài liệu và thông báo cần xử lý.");
        sub.setFont(uiFont(Font.PLAIN, 14));
        sub.setForeground(textMuted);

        titleStack.add(eyebrow);
        titleStack.add(Box.createVerticalStrut(4));
        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(5));
        titleStack.add(sub);

        left.add(titleStack);
        header.add(left, BorderLayout.WEST);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        stats.setOpaque(false);
        totalValue = new JLabel("0");
        pendingValue = new JLabel("0");
        doneValue = new JLabel("0");
        stats.add(createStatCard("Tổng", totalValue, "inbox-stack", primary));
        stats.add(createStatCard("Đang chờ", pendingValue, "clock", warning));
        stats.add(createStatCard("Đã xong", doneValue, "check-circle", success));
        header.add(stats, BorderLayout.EAST);

        return header;
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);

        body.add(createFilterSection(), BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = Color.decode("#CBD5E1");
                trackColor = bgMain;
            }

            @Override protected JButton createDecreaseButton(int orientation) {
                return invisibleScrollButton();
            }

            @Override protected JButton createIncreaseButton(int orientation) {
                return invisibleScrollButton();
            }
        });

        body.add(scroll, BorderLayout.CENTER);
        return body;
    }

    private JPanel createFilterSection() {
        JPanel filters = new JPanel(new BorderLayout(18, 0));
        filters.setOpaque(false);
        filters.setBorder(new EmptyBorder(0, 0, 22, 0));

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tabs.setOpaque(false);
        tabs.add(createFilterTab("Tất cả", "ALL", "sparkles"));
        tabs.add(createFilterTab("Lớp dạy", "TEACH", "book-open"));
        tabs.add(createFilterTab("Nộp tài liệu", "DOCS", "document-text"));
        tabs.add(createFilterTab("Bài tập", "HW", "clipboard-document-check"));
        tabs.add(createFilterTab("Thông báo", "NOTIFY", "bell-alert"));
        filters.add(tabs, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);

        JComboBox<String> statusCombo = createCombo(new String[]{"Tất cả trạng thái", "Đang chờ", "Sắp đến hạn", "Mới", "Đã hoàn thành"});
        statusCombo.addActionListener(e -> {
            int idx = statusCombo.getSelectedIndex();
            statusFilter = switch (idx) {
                case 1 -> "PENDING";
                case 2 -> "DUE";
                case 3 -> "NEW";
                case 4 -> "DONE";
                default -> "ALL_STATUS";
            };
            renderTaskList();
        });

        JComboBox<String> sortCombo = createCombo(new String[]{"Mới nhất", "Ưu tiên trước", "Hoàn thành sau"});
        sortCombo.addActionListener(e -> {
            sortMode = switch (sortCombo.getSelectedIndex()) {
                case 1 -> "PRIORITY";
                case 2 -> "DONE_LAST";
                default -> "NEWEST";
            };
            renderTaskList();
        });

        actions.add(statusCombo);
        actions.add(sortCombo);
        filters.add(actions, BorderLayout.EAST);

        return filters;
    }

    private FilterChip createFilterTab(String title, String type, String iconName) {
        FilterChip chip = new FilterChip(title, type, iconName);
        filterChips.add(chip);
        return chip;
    }

    private JPanel createBackButton() {
        JPanel button = new JPanel(new BorderLayout()) {
            private boolean hover;

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = createGraphics(g);
                g2.setColor(hover ? Color.decode("#EAF1FF") : surface);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(hover ? Color.decode("#BFD4FF") : border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }

            void setHover(boolean hover) {
                this.hover = hover;
                repaint();
            }
        };
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(48, 48));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(svgIcon("arrow-left", 20, textMain), SwingConstants.CENTER);
        button.add(icon, BorderLayout.CENTER);

        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                invokeSetHover(button, true);
            }

            @Override public void mouseExited(MouseEvent e) {
                invokeSetHover(button, false);
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (onBackListener != null) {
                    onBackListener.run();
                }
            }
        });

        return button;
    }

    private JPanel createStatCard(String label, JLabel valueLabel, String icon, Color accent) {
        JPanel card = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = createGraphics(g);
                g2.setColor(new Color(15, 23, 42, 8));
                g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 4, 18, 18);
                g2.setColor(surface);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 4, 18, 18);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 5, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 13, 12, 14));
        card.setPreferredSize(new Dimension(128, 62));

        JLabel iconLabel = new JLabel(svgIcon(icon, 20, accent), SwingConstants.CENTER);
        JPanel iconWrap = iconTile(accent, new Dimension(34, 34), 11);
        iconWrap.add(iconLabel, BorderLayout.CENTER);
        card.add(iconWrap, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        valueLabel.setFont(uiFont(Font.BOLD, 18));
        valueLabel.setForeground(textMain);
        JLabel labelText = new JLabel(label);
        labelText.setFont(uiFont(Font.PLAIN, 11));
        labelText.setForeground(textMuted);
        text.add(valueLabel);
        text.add(labelText);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JComboBox<String> createCombo(String[] values) {
        JComboBox<String> combo = new JComboBox<>(values);
        combo.setPreferredSize(new Dimension(170, 42));
        combo.setFont(uiFont(Font.PLAIN, 13));
        combo.setForeground(textMain);
        combo.setBackground(surface);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                new EmptyBorder(0, 10, 0, 10)
        ));
        return combo;
    }

    private void renderTaskList() {
        if (listPanel == null) {
            return;
        }

        updateStats();
        refreshFilterChips();
        listPanel.removeAll();

        List<Task> visible = filteredTasks();
        if (visible.isEmpty()) {
            listPanel.add(createEmptyState());
        } else {
            for (Task task : visible) {
                listPanel.add(createTaskCard(task));
                listPanel.add(Box.createVerticalStrut(12));
            }
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private List<Task> filteredTasks() {
        List<Task> visible = new ArrayList<>();
        for (Task task : tasks) {
            boolean categoryOk = currentFilter.equals("ALL") || currentFilter.equals(task.categoryType);
            boolean statusOk = switch (statusFilter) {
                case "PENDING" -> !task.completed;
                case "DUE" -> "DOCS".equals(task.categoryType) && !task.completed;
                case "NEW" -> "NOTIFY".equals(task.categoryType) && !task.completed;
                case "DONE" -> task.completed;
                default -> true;
            };
            if (categoryOk && statusOk) {
                visible.add(task);
            }
        }

        if ("PRIORITY".equals(sortMode)) {
            visible.sort(Comparator.comparingInt(Task::priority).reversed());
        } else if ("DONE_LAST".equals(sortMode)) {
            visible.sort(Comparator.comparing(Task::isCompleted));
        }
        return visible;
    }

    private JPanel createEmptyState() {
        JPanel empty = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = createGraphics(g);
                g2.setColor(surface);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 24, 24);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 24, 24);
                g2.dispose();
            }
        };
        empty.setOpaque(false);
        empty.setBorder(new EmptyBorder(56, 20, 56, 20));
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        JPanel iconWrap = iconTile(primary, new Dimension(58, 58), 18);
        iconWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconWrap.add(new JLabel(svgIcon("inbox-stack", 28, primary), SwingConstants.CENTER), BorderLayout.CENTER);

        JLabel title = new JLabel("Không có nhiệm vụ phù hợp");
        title.setFont(uiFont(Font.BOLD, 18));
        title.setForeground(textMain);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Thử đổi bộ lọc hoặc chờ dữ liệu mới từ hệ thống.");
        sub.setFont(uiFont(Font.PLAIN, 13));
        sub.setForeground(textMuted);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        stack.add(iconWrap);
        stack.add(Box.createVerticalStrut(14));
        stack.add(title);
        stack.add(Box.createVerticalStrut(6));
        stack.add(sub);
        empty.add(stack);
        return empty;
    }

    private JPanel createTaskCard(Task task) {
        JPanel card = new TaskCard(task);
        card.setLayout(new BorderLayout(18, 0));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 22, 18, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
        card.setPreferredSize(new Dimension(900, 104));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel iconWrap = iconTile(task.accent, new Dimension(54, 54), 15);
        iconWrap.add(new JLabel(svgIcon(task.iconName, 26, task.accent), SwingConstants.CENTER), BorderLayout.CENTER);
        card.add(iconWrap, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(task.title);
        title.setFont(uiFont(Font.BOLD, 16));
        title.setForeground(textMain);
        titleRow.add(title);
        titleRow.add(createCategoryBadge(task));

        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        meta.setOpaque(false);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        meta.add(createIconTextRow("calendar-days", task.time));
        if (task.location != null && !task.location.isBlank()) {
            meta.add(createIconTextRow("map-pin", task.location));
        }

        center.add(Box.createVerticalGlue());
        center.add(titleRow);
        center.add(Box.createVerticalStrut(9));
        center.add(meta);
        center.add(Box.createVerticalGlue());
        card.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        right.setOpaque(false);

        JPanel status = new JPanel();
        status.setOpaque(false);
        status.setLayout(new BoxLayout(status, BoxLayout.Y_AXIS));

        JLabel statusLabel = new JLabel(task.statusText);
        statusLabel.setFont(uiFont(Font.BOLD, 12));
        statusLabel.setForeground(task.statusColor);
        statusLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel timeLeft = new JLabel(task.timeLeft);
        timeLeft.setFont(uiFont(Font.PLAIN, 12));
        timeLeft.setForeground(textMuted);
        timeLeft.setAlignmentX(Component.RIGHT_ALIGNMENT);

        status.add(statusLabel);
        status.add(Box.createVerticalStrut(5));
        status.add(timeLeft);

        JLabel arrow = new JLabel(svgIcon("chevron-right", 20, textSoft));
        right.add(status);
        right.add(arrow);
        card.add(right, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                ((TaskCard) card).setHover(true);
            }

            @Override public void mouseExited(MouseEvent e) {
                ((TaskCard) card).setHover(false);
            }

            @Override public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(card,
                        task.title + "\n" + task.time + "\n" + task.location,
                        "Chi tiết nhiệm vụ",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        return card;
    }

    private JPanel createIconTextRow(String iconName, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        row.setOpaque(false);
        JLabel icon = new JLabel(svgIcon(iconName, 15, textSoft));
        JLabel label = new JLabel(text);
        label.setFont(uiFont(Font.PLAIN, 13));
        label.setForeground(textMuted);
        row.add(icon);
        row.add(label);
        return row;
    }

    private JPanel createCategoryBadge(Task task) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = createGraphics(g);
                g2.setColor(task.badgeBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));
        JLabel dot = new JLabel(svgIcon(task.iconName, 11, task.accent));
        JLabel label = new JLabel(task.categoryLabel);
        label.setFont(uiFont(Font.BOLD, 11));
        label.setForeground(task.accent);
        badge.add(dot);
        badge.add(label);
        return badge;
    }

    private JPanel createPagination() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(18, 0, 24, 0));

        JLabel hint = new JLabel("Hiển thị theo dữ liệu đồng bộ mới nhất từ máy chủ");
        hint.setFont(uiFont(Font.PLAIN, 12));
        hint.setForeground(textSoft);
        footer.add(hint, BorderLayout.WEST);

        JPanel pages = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pages.setOpaque(false);
        pages.add(createPageButton("‹", false));
        pages.add(createPageButton("1", true));
        pages.add(createPageButton("2", false));
        pages.add(createPageButton("3", false));
        JLabel dots = new JLabel("...");
        dots.setForeground(textMuted);
        pages.add(dots);
        pages.add(createPageButton("›", false));
        footer.add(pages, BorderLayout.EAST);
        return footer;
    }

    private JPanel createPageButton(String text, boolean active) {
        JPanel button = new JPanel(new BorderLayout()) {
            private boolean hover;

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = createGraphics(g);
                if (active) {
                    g2.setColor(primary);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                } else {
                    g2.setColor(hover ? Color.decode("#EEF4FF") : surface);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setColor(hover ? Color.decode("#C8D8FF") : border);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
                g2.dispose();
            }

            void setHover(boolean hover) {
                this.hover = hover;
                repaint();
            }
        };
        button.setOpaque(false);
        button.setPreferredSize(new Dimension(40, 40));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(uiFont(Font.BOLD, 13));
        label.setForeground(active ? Color.WHITE : textMain);
        button.add(label, BorderLayout.CENTER);
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                invokeSetHover(button, true);
            }

            @Override public void mouseExited(MouseEvent e) {
                invokeSetHover(button, false);
            }
        });
        return button;
    }

    private Task createTask(String id, String category, String title, String time, String location, boolean completed) {
        String normalized = switch (category) {
            case "DOCS", "DOCUMENT", "FILE" -> "DOCS";
            case "HW", "HOMEWORK", "ASSIGNMENT" -> "HW";
            case "NOTIFY", "NOTICE", "ANNOUNCEMENT" -> "NOTIFY";
            default -> "TEACH";
        };

        TaskVisual visual = visualFor(normalized, completed);
        return new Task(id, normalized, title, time, location, completed, visual);
    }

    private TaskVisual visualFor(String category, boolean completed) {
        if (completed) {
            return new TaskVisual("Đã hoàn thành", "Xong", "Đã xong", success, Color.decode("#ECFDF5"), "check-circle", success);
        }
        return switch (category) {
            case "DOCS" -> new TaskVisual("Tài liệu", "Sắp đến hạn", "Cần nộp", warning, Color.decode("#FFF7ED"), "document-text", Color.decode("#EA580C"));
            case "HW" -> new TaskVisual("Bài tập", "Cần làm", "Đang chờ", Color.decode("#7C3AED"), Color.decode("#F5F3FF"), "clipboard-document-check", Color.decode("#7C3AED"));
            case "NOTIFY" -> new TaskVisual("Thông báo", "Mới", "Cần xem", danger, Color.decode("#FEF2F2"), "bell-alert", danger);
            default -> new TaskVisual("Lớp dạy", "Sắp diễn ra", "Đang chờ", primary, Color.decode("#EFF6FF"), "book-open", primary);
        };
    }

    private JPanel iconTile(Color accent, Dimension size, int radius) {
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = createGraphics(g);
                g2.setColor(tint(accent, 0.10f));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.setPreferredSize(size);
        wrap.setMinimumSize(size);
        wrap.setMaximumSize(size);
        return wrap;
    }

    private Icon svgIcon(String name, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon(ICON_ROOT + name + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        return icon;
    }

    private void updateStats() {
        if (totalValue == null) {
            return;
        }
        int total = tasks.size();
        long done = tasks.stream().filter(Task::isCompleted).count();
        totalValue.setText(String.valueOf(total));
        pendingValue.setText(String.valueOf(total - done));
        doneValue.setText(String.valueOf(done));
    }

    private void refreshFilterChips() {
        for (FilterChip chip : filterChips) {
            chip.refresh();
        }
    }

    private String normalizeMeta(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String cleanServerText(String value) {
        if (value == null) {
            return "";
        }
        String current = value.trim();
        for (int i = 0; i < 3 && looksMojibake(current); i++) {
            try {
                String decoded = new String(current.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                if (decoded.equals(current)) {
                    break;
                }
                current = decoded;
            } catch (Exception ignored) {
                break;
            }
        }
        return current.replace('\u00A0', ' ').trim();
    }

    private boolean looksMojibake(String value) {
        return value.contains("Ã") || value.contains("Â") || value.contains("áº") || value.contains("Ä");
    }

    private Font uiFont(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    private Graphics2D createGraphics(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g2;
    }

    private Color tint(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * alpha));
    }

    private JButton invisibleScrollButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    private void invokeSetHover(JPanel panel, boolean hover) {
        try {
            var method = panel.getClass().getDeclaredMethod("setHover", boolean.class);
            method.setAccessible(true);
            method.invoke(panel, hover);
        } catch (Exception ignored) {
            panel.repaint();
        }
    }

    private final class FilterChip extends JPanel {
        private final String title;
        private final String type;
        private final String iconName;
        private final JLabel label;
        private final JLabel icon;
        private boolean hover;

        private FilterChip(String title, String type, String iconName) {
            super(new FlowLayout(FlowLayout.LEFT, 8, 0));
            this.title = title;
            this.type = type;
            this.iconName = iconName;
            setOpaque(false);
            setBorder(new EmptyBorder(10, 16, 10, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            icon = new JLabel();
            label = new JLabel(title);
            add(icon);
            add(label);
            refresh();

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override public void mouseClicked(MouseEvent e) {
                    currentFilter = type;
                    refreshFilterChips();
                    renderTaskList();
                }
            });
        }

        private void refresh() {
            boolean active = currentFilter.equals(type);
            label.setText(title);
            label.setFont(uiFont(active ? Font.BOLD : Font.PLAIN, 13));
            label.setForeground(active ? primary : textMain);
            icon.setIcon(svgIcon(iconName, 15, active ? primary : textMuted));
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            boolean active = currentFilter.equals(type);
            if (active) {
                g2.setColor(surface);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(primary);
                g2.setStroke(new BasicStroke(1.7f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            } else {
                g2.setColor(hover ? Color.decode("#EEF4FF") : surfaceSoft);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(border);
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private final class TaskCard extends JPanel {
        private final Task task;
        private boolean hover;

        private TaskCard(Task task) {
            this.task = task;
        }

        private void setHover(boolean hover) {
            this.hover = hover;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = createGraphics(g);
            if (hover) {
                g2.setColor(tint(task.accent, 0.11f));
                g2.fillRoundRect(4, 8, getWidth() - 8, getHeight() - 10, 22, 22);
            }
            g2.setColor(surface);
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 6, 22, 22);
            g2.setColor(hover ? tint(task.accent, 0.48f) : border);
            g2.setStroke(new BasicStroke(hover ? 1.6f : 1f));
            g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 7, 22, 22);
            g2.setColor(task.accent);
            g2.fillRoundRect(0, 18, 4, getHeight() - 42, 4, 4);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class TaskVisual {
        private final String categoryLabel;
        private final String statusText;
        private final String timeLeft;
        private final Color statusColor;
        private final Color badgeBg;
        private final String iconName;
        private final Color accent;

        private TaskVisual(String categoryLabel, String statusText, String timeLeft,
                           Color statusColor, Color badgeBg, String iconName, Color accent) {
            this.categoryLabel = categoryLabel;
            this.statusText = statusText;
            this.timeLeft = timeLeft;
            this.statusColor = statusColor;
            this.badgeBg = badgeBg;
            this.iconName = iconName;
            this.accent = accent;
        }
    }

    private static final class Task {
        private final String id;
        private final String categoryType;
        private final String title;
        private final String time;
        private final String location;
        private final boolean completed;
        private final String categoryLabel;
        private final String statusText;
        private final String timeLeft;
        private final Color statusColor;
        private final Color badgeBg;
        private final String iconName;
        private final Color accent;

        private Task(String id, String categoryType, String title, String time, String location,
                     boolean completed, TaskVisual visual) {
            this.id = id;
            this.categoryType = categoryType;
            this.title = title == null || title.isBlank() ? "Nhiệm vụ chưa có tiêu đề" : title;
            this.time = time;
            this.location = location;
            this.completed = completed;
            this.categoryLabel = visual.categoryLabel;
            this.statusText = visual.statusText;
            this.timeLeft = visual.timeLeft;
            this.statusColor = visual.statusColor;
            this.badgeBg = visual.badgeBg;
            this.iconName = visual.iconName;
            this.accent = visual.accent;
        }

        private boolean isCompleted() {
            return completed;
        }

        private int priority() {
            if (completed) {
                return 0;
            }
            return switch (categoryType) {
                case "NOTIFY" -> 4;
                case "HW" -> 3;
                case "DOCS" -> 2;
                default -> 1;
            };
        }
    }
}
