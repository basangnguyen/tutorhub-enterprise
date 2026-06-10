package com.mycompany.tutorhub_enterprise.client;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomLessonModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomMemberModel;
import com.mycompany.tutorhub_enterprise.models.Packet;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassManagerTab extends JPanel {
    private static final String FILTER_ALL = "ALL";
    private static final String FILTER_TEACHER = "TEACHER";
    private static final String FILTER_STUDENT = "STUDENT";
    private static final String FILTER_PENDING = "PENDING";
    private static final String FILTER_ENDED = "ENDED";
    private static final int CONTENT_MAX_WIDTH = 1560;
    private static final int CLASS_CARD_WIDTH = 220;
    private static final int CLASS_CARD_HEIGHT = 270;
    private static final int CLASS_COVER_HEIGHT = 94;
    private static final int CLASS_COVER_INSET = 8;

    private final Color background = TutorHubTheme.BACKGROUND;
    private final Color surface = TutorHubTheme.SURFACE;
    private final Color primary = TutorHubTheme.PRIMARY;
    private final Color primaryBlue = TutorHubTheme.PRIMARY_BLUE;
    private final Color textDark = TutorHubTheme.TEXT_DARK;
    private final Color textMuted = TutorHubTheme.TEXT_MUTED;
    private final Color border = TutorHubTheme.BORDER;
    private final Color green = TutorHubTheme.SUCCESS;
    private final Color hoverBorder = TutorHubTheme.HOVER_BORDER;

    private final MainDashboard dashboard;
    private final List<ClassroomGroupModel> classrooms = new ArrayList<>();
    private final List<ClassroomLessonModel> lessons = new ArrayList<>();
    private final Map<String, Image> coverImageCache = new HashMap<>();

    private JPanel filterTabs;
    private JPanel cardsGrid;
    private JTextField searchField;
    private String activeFilter = FILTER_ALL;
    private boolean dataLoading = true;
    private boolean classroomsLoaded;
    private boolean lessonsLoaded;
    private Timer loadingFallbackTimer;

    public ClassManagerTab(MainDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout());
        setBackground(background);
        setBorder(new EmptyBorder(0, 0, 0, 0));
        initUI();
        requestData();
    }

    private void initUI() {
        JPanel content = new ScrollableContentPanel();
        content.setLayout(new MigLayout("insets 14 34 32 34, fillx, wrap 1", "[grow,fill]", "[]16[]18[]14[]"));
        content.setOpaque(false);

        content.add(createTitleHeader(), "growx, wmax " + CONTENT_MAX_WIDTH);
        content.add(createActionCards(), "growx, wmax " + CONTENT_MAX_WIDTH);
        content.add(createFilterHeader(), "growx, wmax " + CONTENT_MAX_WIDTH);

        cardsGrid = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        cardsGrid.setOpaque(false);
        cardsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(cardsGrid, "growx, wmax " + CONTENT_MAX_WIDTH);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        renderClassCards();
    }

    private JPanel createTitleHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Lớp học của tôi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(textDark);

        JLabel subtitle = new JLabel("Tạo, tham gia và quản lý các lớp học trực tuyến");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(textMuted);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        JPanel refresh = createSmallCommand("Làm mới", () -> requestData());
        header.add(refresh, BorderLayout.EAST);
        return header;
    }

    private JPanel createActionCards() {
        JPanel row = new JPanel(new MigLayout(
                "insets 0, fillx, gap 12 0",
                "[grow,fill][grow,fill][grow,fill]",
                "[" + TutorHubTheme.ACTION_CARD_HEIGHT + "!]"
        ));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, TutorHubTheme.ACTION_CARD_HEIGHT + 4));

        row.add(createActionCard(
                "Tạo lớp học mới",
                "Tạo phòng học live cho giáo viên",
                "Tạo lớp học",
                "+",
                Color.decode("#6657F2"),
                Color.decode("#2F63D7"),
                Color.WHITE,
                true,
                this::promptCreateClassThroughServer
        ), "grow, h " + TutorHubTheme.ACTION_CARD_HEIGHT + "!, wmin 0");

        row.add(createActionCard(
                "Public Lesson",
                "Lên lịch và quản lý buổi học công khai",
                "Tạo public lesson",
                "PL",
                Color.decode("#FFFFFF"),
                Color.decode("#FBFBFD"),
                textDark,
                false,
                this::promptCreatePublicLesson
        ), "grow, h " + TutorHubTheme.ACTION_CARD_HEIGHT + "!, wmin 0");

        row.add(createActionCard(
                "Tham gia lớp học",
                "Nhập mã lớp hoặc mã buổi học",
                "Nhập mã lớp",
                "IN",
                Color.decode("#FFFFFF"),
                Color.decode("#FBFBFD"),
                textDark,
                false,
                this::promptJoinPublicLesson
        ), "grow, h " + TutorHubTheme.ACTION_CARD_HEIGHT + "!, wmin 0");

        return row;
    }

    private JPanel createActionCard(String title, String subtitle, String cta, String mark,
                                    Color start, Color end, Color foreground, boolean filled, Runnable action) {
        Color accent = "IN".equals(mark) ? green : primary;
        JPanel card = new JPanel(new MigLayout(
                "insets 8 14 9 14, fill, gap 12 0",
                "[50!][grow,fill][" + TutorHubTheme.ACTION_VISUAL_WIDTH + "!]",
                "[grow,center]"
        )) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = Boolean.TRUE.equals(getClientProperty("hover"));
                int arc = TutorHubTheme.RADIUS_ACTION_CARD;
                g2.setColor(filled
                        ? new Color(43, 91, 191, hover ? 28 : 20)
                        : new Color(31, 41, 55, hover ? 12 : 7));
                g2.fillRoundRect(1, 4, getWidth() - 2, getHeight() - 5, arc, arc);
                if (filled) {
                    GradientPaint paint = new GradientPaint(0, 0, start, getWidth(), getHeight(), end);
                    g2.setPaint(paint);
                } else {
                    g2.setColor(Color.decode("#F1F2F5"));
                }
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, arc, arc);
                if (filled) {
                    g2.setColor(new Color(255, 255, 255, hover ? 46 : 30));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 5, arc - 2, arc - 2);
                    g2.setColor(new Color(255, 255, 255, 13));
                    g2.fillOval(getWidth() - 132, -52, 170, 170);
                } else {
                    g2.setColor(hover ? hoverBorder : border);
                    g2.setStroke(new BasicStroke(1.1f));
                    g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 4, arc, arc);
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hover ? 9 : 0));
                    g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 5, arc - 1, arc - 1);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel markBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = Boolean.TRUE.equals(getClientProperty("hover"));
                Color markBg = filled
                        ? new Color(255, 255, 255, hover ? 76 : 58)
                        : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hover ? 32 : 20);
                g2.setColor(markBg);
                int box = TutorHubTheme.ACTION_ICON_BOX;
                int boxX = (getWidth() - box) / 2;
                int boxY = (getHeight() - box) / 2;
                g2.fillRoundRect(boxX, boxY, box, box, 14, 14);
                g2.setColor(filled
                        ? new Color(255, 255, 255, 44)
                        : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 24));
                g2.drawRoundRect(boxX, boxY, box - 1, box - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        markBox.setOpaque(false);
        markBox.setPreferredSize(new Dimension(50, 52));
        markBox.add(createActionIconLabel(mark, filled, accent), BorderLayout.CENTER);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new MigLayout("insets 0, fillx, wrap 1, gap 0 0", "[grow,fill]", "[]3[]4[]"));
        copy.setMinimumSize(new Dimension(0, 58));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TutorHubTheme.font(Font.BOLD, 16));
        titleLabel.setForeground(foreground);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(TutorHubTheme.font(Font.PLAIN, 12));
        subtitleLabel.setForeground(filled ? new Color(255, 255, 255, 222) : textMuted);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ctaLabel = new JLabel(cta + "  \u2192");
        ctaLabel.setFont(TutorHubTheme.font(Font.BOLD, 12));
        ctaLabel.setForeground(filled ? Color.WHITE : accent);
        ctaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        copy.add(titleLabel, "growx");
        copy.add(subtitleLabel, "growx");
        copy.add(ctaLabel, "growx");

        JPanel illustration = new ActionCardVisualPanel(mark, accent);

        card.add(markBox, "cell 0 0, align center");
        card.add(copy, "cell 1 0, growx, aligny center");
        card.add(illustration, "cell 2 0, alignx right, aligny center");
        addHover(card, markBox, illustration);
        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        };
        addMouseListenerRecursively(card, clickHandler);
        return card;
    }

    private JLabel createActionIconLabel(String mark, boolean filled, Color accent) {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        Icon icon = loadActionIcon(mark);
        if (icon != null) {
            label.setIcon(icon);
            return label;
        }

        label.setText("IN".equals(mark) ? "IN" : "+");
        label.setFont(TutorHubTheme.font(Font.BOLD, "IN".equals(mark) ? 14 : 27));
        label.setForeground(filled ? Color.WHITE : accent);
        return label;
    }

    private Icon loadActionIcon(String mark) {
        if ("+".equals(mark)) {
            return new FlatSVGIcon("images/icon/create.svg", TutorHubTheme.ACTION_ICON_SIZE, TutorHubTheme.ACTION_ICON_SIZE);
        }
        if ("PL".equals(mark)) {
            return new FlatSVGIcon("images/icon/public.svg", TutorHubTheme.ACTION_ICON_SIZE, TutorHubTheme.ACTION_ICON_SIZE);
        }
        if ("IN".equals(mark)) {
            URL url = getClass().getResource("/images/icon/enter.png");
            if (url != null) {
                ImageIcon source = new ImageIcon(url);
                Image scaled = source.getImage().getScaledInstance(TutorHubTheme.ACTION_ICON_SIZE, TutorHubTheme.ACTION_ICON_SIZE, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        }
        return null;
    }

    private JPanel createFilterHeader() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel(new MigLayout(
                "insets 0 0 6 0, fillx, gap 12 0",
                "[grow,fill][276!]",
                "[36!]"
        ));
        top.setOpaque(false);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        filterTabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        filterTabs.setOpaque(false);
        renderFilterTabs();
        top.add(filterTabs, "cell 0 0, growx, aligny center");

        JPanel searchBox = createSearchBox();
        top.add(searchBox, "cell 1 0, w 276!, h 36!, alignx right, aligny center");

        JPanel rule = new JPanel();
        rule.setOpaque(true);
        rule.setBackground(Color.decode("#ECEEF5"));
        rule.setPreferredSize(new Dimension(1, 1));
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        wrapper.add(top);
        wrapper.add(rule);
        return wrapper;
    }

    private JPanel createSearchBox() {
        JPanel box = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused = Boolean.TRUE.equals(getClientProperty("focused"));
                if (focused) {
                    g2.setColor(TutorHubTheme.alpha(primary, 16));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                g2.setColor(surface);
                g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 12, 12);
                g2.setColor(focused ? hoverBorder : border);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        box.setOpaque(false);
        box.setPreferredSize(new Dimension(276, 36));
        box.setBorder(new EmptyBorder(0, 13, 2, 13));
        JLabel icon = new JLabel();
        FlatSVGIcon searchIcon = new FlatSVGIcon("images/icon_svg/search.svg", 15, 15);
        searchIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> textMuted));
        icon.setIcon(searchIcon);
        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createEmptyBorder());
        searchField.setOpaque(false);
        searchField.setFont(TutorHubTheme.font(Font.PLAIN, 12));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm lớp học...");
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                box.putClientProperty("focused", true);
                box.repaint();
            }

            @Override public void focusLost(java.awt.event.FocusEvent e) {
                box.putClientProperty("focused", false);
                box.repaint();
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { renderClassCards(); }
            @Override public void removeUpdate(DocumentEvent e) { renderClassCards(); }
            @Override public void changedUpdate(DocumentEvent e) { renderClassCards(); }
        });
        box.add(icon, BorderLayout.WEST);
        box.add(searchField, BorderLayout.CENTER);
        return box;
    }

    private void renderFilterTabs() {
        if (filterTabs == null) return;
        filterTabs.removeAll();
        filterTabs.add(createFilterTab("Tất cả lớp học", FILTER_ALL));
        filterTabs.add(createFilterTab("Là giáo viên", FILTER_TEACHER));
        filterTabs.add(createFilterTab("Là học viên", FILTER_STUDENT));
        filterTabs.add(createFilterTab("Chờ duyệt", FILTER_PENDING));
        filterTabs.add(createFilterTab("Đã kết thúc", FILTER_ENDED));
        filterTabs.revalidate();
        filterTabs.repaint();
    }

    private JPanel createFilterTab(String label, String key) {
        boolean active = activeFilter.equals(key);
        JPanel tab = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (!active && Boolean.TRUE.equals(getClientProperty("hover"))) {
                    g2.setColor(new Color(17, 24, 39, 5));
                    g2.fillRoundRect(1, 4, getWidth() - 2, getHeight() - 9, 10, 10);
                }
                if (active) {
                    g2.setColor(TutorHubTheme.alpha(primary, 8));
                    g2.fillRoundRect(1, 4, getWidth() - 2, getHeight() - 9, 10, 10);
                    g2.setColor(primary);
                    g2.fillRoundRect(8, getHeight() - 3, Math.max(14, getWidth() - 16), 3, 4, 4);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(7, 8, 9, 8));
        tab.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel text = new JLabel(label);
        text.setFont(TutorHubTheme.font(active ? Font.BOLD : Font.PLAIN, 13));
        text.setForeground(active ? textDark : textMuted);
        tab.add(text, BorderLayout.CENTER);
        tab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                tab.putClientProperty("hover", true);
                tab.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                tab.putClientProperty("hover", false);
                tab.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                activeFilter = key;
                renderFilterTabs();
                renderClassCards();
            }
        });
        return tab;
    }

    private void requestData() {
        dataLoading = true;
        classroomsLoaded = false;
        lessonsLoaded = false;
        renderClassCards();
        startLoadingFallback();
        try {
            NetworkManager.getInstance().sendPacket(new Packet("GET_CLASSROOMS", ""));
            NetworkManager.getInstance().sendPacket(new Packet("GET_CLASSROOM_LESSONS", ""));
        } catch (Exception e) {
            dataLoading = false;
            stopLoadingFallback();
            renderClassCards();
            // The dashboard-level network listener will show connection errors.
        }
    }

    public void loadClassrooms(List<ClassroomGroupModel> list) {
        classrooms.clear();
        if (list != null) {
            classrooms.addAll(list);
        }
        classroomsLoaded = true;
        updateLoadingState();
        renderClassCards();
    }

    public void syncLessonsFromServer(List<ClassroomLessonModel> list) {
        lessons.clear();
        if (list != null) {
            lessons.addAll(list);
        }
        lessonsLoaded = true;
        updateLoadingState();
        renderClassCards();
    }

    private void updateLoadingState() {
        if (classroomsLoaded && lessonsLoaded) {
            dataLoading = false;
            stopLoadingFallback();
        }
    }

    private void startLoadingFallback() {
        stopLoadingFallback();
        loadingFallbackTimer = new Timer(4500, e -> {
            dataLoading = false;
            stopLoadingFallback();
            renderClassCards();
        });
        loadingFallbackTimer.setRepeats(false);
        loadingFallbackTimer.start();
    }

    private void stopLoadingFallback() {
        if (loadingFallbackTimer != null) {
            loadingFallbackTimer.stop();
            loadingFallbackTimer = null;
        }
    }

    private void renderClassCards() {
        if (cardsGrid == null) return;
        cardsGrid.removeAll();

        boolean hasCards = false;
        List<ClassroomLessonModel> filteredLessons = filterLessons();
        if (!filteredLessons.isEmpty()) {
            for (ClassroomLessonModel lesson : filteredLessons) {
                cardsGrid.add(createLessonCard(lesson));
                hasCards = true;
            }
        }

        List<ClassroomGroupModel> filteredClassrooms = filterClassrooms();
        if (!filteredClassrooms.isEmpty()) {
            for (ClassroomGroupModel classroom : filteredClassrooms) {
                cardsGrid.add(createClassroomCard(classroom));
                hasCards = true;
            }
        }

        if (!hasCards) {
            cardsGrid.add(createListStatePanel());
        }

        cardsGrid.revalidate();
        cardsGrid.repaint();
    }

    private JPanel createListStatePanel() {
        boolean hasAnyData = !lessons.isEmpty() || !classrooms.isEmpty();
        String query = currentSearchQuery();
        if (dataLoading && !hasAnyData) {
            return createLoadingState();
        }
        if (!hasAnyData) {
            return createEmptyState(
                    "Chưa có lớp học",
                    "Tạo lớp học mới, đăng public lesson hoặc tham gia lớp bằng mã để bắt đầu.",
                    "Tạo lớp học",
                    this::promptCreateClassThroughServer
            );
        }
        if (!query.isEmpty()) {
            return createEmptyState(
                    "Không tìm thấy lớp phù hợp",
                    "Thử từ khóa khác hoặc xóa nội dung tìm kiếm để xem lại tất cả lớp học.",
                    "Xóa tìm kiếm",
                    () -> {
                        searchField.setText("");
                        renderClassCards();
                    }
            );
        }
        return createEmptyState(
                "Không có lớp trong bộ lọc này",
                "Chọn một tab khác hoặc làm mới danh sách lớp học để kiểm tra dữ liệu mới.",
                "Làm mới",
                this::requestData
        );
    }

    private String currentSearchQuery() {
        return searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
    }

    private List<ClassroomLessonModel> filterLessons() {
        String query = currentSearchQuery();
        List<ClassroomLessonModel> result = new ArrayList<>();
        for (ClassroomLessonModel lesson : lessons) {
            String title = valueOrDefault(lesson.getTitle(), "");
            String org = valueOrDefault(lesson.getOrganizationName(), "");
            String code = valueOrDefault(lesson.getJoinCode(), "");
            boolean matchesQuery = query.isEmpty()
                    || title.toLowerCase().contains(query)
                    || org.toLowerCase().contains(query)
                    || code.toLowerCase().contains(query);
            if (!matchesQuery || !matchesFilter(lesson)) continue;
            result.add(lesson);
        }
        return result;
    }

    private List<ClassroomGroupModel> filterClassrooms() {
        String query = currentSearchQuery();
        List<ClassroomGroupModel> result = new ArrayList<>();
        for (ClassroomGroupModel classroom : classrooms) {
            String name = valueOrDefault(classroom.getName(), "");
            String org = valueOrDefault(classroom.getOrganizationName(), "");
            String code = valueOrDefault(classroom.getJoinCode(), "");
            boolean matchesQuery = query.isEmpty()
                    || name.toLowerCase().contains(query)
                    || org.toLowerCase().contains(query)
                    || code.toLowerCase().contains(query);
            if (!matchesQuery) continue;
            if (FILTER_TEACHER.equals(activeFilter) && !isCurrentUserOwner(classroom)) continue;
            if (FILTER_STUDENT.equals(activeFilter) && isCurrentUserOwner(classroom)) continue;
            if (FILTER_PENDING.equals(activeFilter)) continue;
            if (FILTER_ENDED.equals(activeFilter) && !"ENDED".equalsIgnoreCase(valueOrDefault(classroom.getStatus(), ""))) continue;
            result.add(classroom);
        }
        return result;
    }

    private boolean matchesFilter(ClassroomLessonModel lesson) {
        if (FILTER_ALL.equals(activeFilter)) return true;
        if (FILTER_TEACHER.equals(activeFilter)) return isCurrentUserTeacher(lesson);
        if (FILTER_STUDENT.equals(activeFilter)) return !isCurrentUserTeacher(lesson) && !isWaitingForApproval(lesson);
        if (FILTER_PENDING.equals(activeFilter)) return isWaitingForApproval(lesson);
        if (FILTER_ENDED.equals(activeFilter)) return isEnded(lesson);
        return true;
    }

    private JPanel createLessonCard(ClassroomLessonModel lesson) {
        boolean teacher = isCurrentUserTeacher(lesson);
        boolean waiting = isWaitingForApproval(lesson);
        boolean ended = isEnded(lesson);
        boolean publicLesson = isPublicLesson(lesson);

        ElevatedPanel card = new ElevatedPanel(15, surface);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.setPreferredSize(new Dimension(CLASS_CARD_WIDTH, CLASS_CARD_HEIGHT));
        card.setMaximumSize(new Dimension(CLASS_CARD_WIDTH, CLASS_CARD_HEIGHT));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel cover = createCoverPanel(lesson);
        JPanel coverTop = new JPanel(new BorderLayout());
        coverTop.setOpaque(false);
        coverTop.add(createStatusPill(statusText(lesson), statusBackground(lesson), statusForeground(lesson)), BorderLayout.WEST);
        JLabel more = createMoreTrigger();
        setupLessonMenu(more, lesson);
        coverTop.add(more, BorderLayout.EAST);
        cover.add(coverTop, BorderLayout.NORTH);
        card.add(createCoverShell(cover), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 13, 12, 13));

        JLabel title = new JLabel(shortText(friendlyLessonTitle(lesson), 23));
        title.setFont(TutorHubTheme.font(Font.BOLD, 14));
        title.setForeground(textDark);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel org = new JLabel(shortText(friendlyOrganizationName(valueOrDefault(lesson.getOrganizationName(), "TutorHub Enterprise")), 25));
        org.setFont(TutorHubTheme.font(Font.PLAIN, 11));
        org.setForeground(textMuted);
        org.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel schedule = createInfoLine("clock", formatSchedule(lesson), textDark);
        JPanel meta = createInfoLine("user", (teacher ? "Giáo viên" : "Học viên") + " Â· " + (publicLesson ? "Public lesson" : "Live classroom"), textMuted);
        JPanel capacity = createInfoLine("book-open", Math.max(lesson.getSeatCount(), 1) + " học viên", textMuted);

        JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));

        JButton enter = createRoundedButton(waiting ? "Chờ duyệt" : ended ? "Xem chi tiết" : "Vào lớp",
                waiting ? Color.decode("#FEF3C7") : primaryBlue,
                waiting ? Color.decode("#92400E") : Color.WHITE);
        enter.setFont(TutorHubTheme.font(Font.BOLD, 11));
        enter.setBorder(BorderFactory.createEmptyBorder(6, 11, 6, 11));
        int enterWidth = ended ? 90 : waiting ? 82 : 72;
        enter.setPreferredSize(new Dimension(enterWidth, 30));
        enter.setEnabled(!waiting);
        enter.addActionListener(e -> {
            if (ended) {
                showLessonDetail(lesson);
            } else {
                enterLesson(lesson);
            }
        });
        actions.add(enter, BorderLayout.WEST);

        if (publicLesson && teacher && lesson.isLobbyEnabled()) {
            JButton lobby = createRoundedButton("Lobby", Color.decode("#F3F0FF"), primary);
            lobby.setFont(TutorHubTheme.font(Font.BOLD, 11));
            lobby.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            lobby.setPreferredSize(new Dimension(70, 30));
            lobby.addActionListener(e -> requestWaitingRoom(lesson));
            actions.add(lobby, BorderLayout.EAST);
        }

        body.add(title);
        body.add(Box.createVerticalStrut(3));
        body.add(org);
        body.add(Box.createVerticalStrut(7));
        body.add(schedule);
        body.add(Box.createVerticalStrut(3));
        body.add(meta);
        body.add(Box.createVerticalStrut(2));
        body.add(capacity);
        body.add(Box.createVerticalGlue());
        body.add(actions);

        card.add(body, BorderLayout.CENTER);
        addHover(card, cover);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !waiting) {
                    enterLesson(lesson);
                }
            }
        });
        return card;
    }

    private JPanel createClassroomCard(ClassroomGroupModel classroom) {
        ElevatedPanel card = new ElevatedPanel(15, surface);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.setPreferredSize(new Dimension(CLASS_CARD_WIDTH, CLASS_CARD_HEIGHT));
        card.setMaximumSize(new Dimension(CLASS_CARD_WIDTH, CLASS_CARD_HEIGHT));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel cover = createCoverPanel(valueOrDefault(classroom.getName(), "TH"));
        JPanel coverTop = new JPanel(new BorderLayout());
        coverTop.setOpaque(false);
        coverTop.add(createStatusPill(valueOrDefault(classroom.getStatus(), "ACTIVE"), Color.decode("#EAFBF0"), Color.decode("#087443")), BorderLayout.WEST);
        JLabel more = createMoreTrigger();
        setupClassroomMenu(more, classroom);
        coverTop.add(more, BorderLayout.EAST);
        cover.add(coverTop, BorderLayout.NORTH);
        card.add(createCoverShell(cover), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 13, 12, 13));

        JLabel title = new JLabel(shortText(friendlyClassroomTitle(classroom), 23));
        title.setFont(TutorHubTheme.font(Font.BOLD, 14));
        title.setForeground(textDark);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel org = new JLabel(shortText(friendlyOrganizationName(valueOrDefault(classroom.getOrganizationName(), "TutorHub Enterprise")), 25));
        org.setFont(TutorHubTheme.font(Font.PLAIN, 11));
        org.setForeground(textMuted);
        org.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel schedule = createInfoLine("clock", "Lịch học: chưa có lịch", textDark);
        JPanel role = createInfoLine("user", "Vai trò: " + (isCurrentUserOwner(classroom) ? "Giáo viên" : "Học viên"), textMuted);
        JPanel type = createInfoLine("book-open", "Private class", textMuted);

        JButton enter = createRoundedButton("Vào lớp", primaryBlue, Color.WHITE);
        enter.setFont(TutorHubTheme.font(Font.BOLD, 11));
        enter.setBorder(BorderFactory.createEmptyBorder(6, 11, 6, 11));
        enter.setPreferredSize(new Dimension(72, 30));
        enter.setMaximumSize(new Dimension(72, 30));
        enter.setAlignmentX(Component.LEFT_ALIGNMENT);
                enter.addActionListener(e -> {
            if (dashboard != null) {
                String joinRole = isCurrentUserOwner(classroom) ? "teacher" : "student";
                java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
                PreJoinDialog preJoin = new PreJoinDialog(owner, valueOrDefault(classroom.getName(), "Lớp học"), joinRole, () -> {
                    dashboard.openLiveClassroom(
                            String.valueOf(classroom.getId()),
                            valueOrDefault(classroom.getName(), "Lớp học"),
                            joinRole
                    );
                });
                preJoin.setVisible(true);
            }
        });

        body.add(title);
        body.add(Box.createVerticalStrut(3));
        body.add(org);
        body.add(Box.createVerticalStrut(7));
        body.add(schedule);
        body.add(Box.createVerticalStrut(3));
        body.add(role);
        body.add(Box.createVerticalStrut(2));
        body.add(type);
        body.add(Box.createVerticalGlue());
        body.add(enter);

        card.add(body, BorderLayout.CENTER);
        addHover(card, cover);
        return card;
    }

    private JPanel createCoverPanel(ClassroomLessonModel lesson) {
        String seed = valueOrDefault(lesson.getTitle(), "TH");
        return createCoverPanel(seed);
    }

    private Image loadCoverImage(String seed) {
        String resource = coverResourceFor(seed);
        if (coverImageCache.containsKey(resource)) {
            return coverImageCache.get(resource);
        }
        try {
            URL url = getClass().getResource(resource);
            if (url == null) {
                return null;
            }
            Image image = ImageIO.read(url);
            coverImageCache.put(resource, image);
            return image;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String coverResourceFor(String seed) {
        String clean = valueOrDefault(seed, "").toLowerCase(Locale.ROOT);
        if (clean.contains("toán") || clean.contains("math") || clean.contains("ielts")) {
            return "/images/math/math1.jpg";
        }
        if (clean.contains("anh") || clean.contains("english")) {
            return "/images/english/english1.jpg";
        }
        if (clean.contains("code") || clean.contains("java") || clean.contains("it") || clean.contains("lập trình")) {
            return "/images/it/it1.jpg";
        }
        if (clean.contains("lý") || clean.contains("physics")) {
            return "/images/physics/physics1.jpg";
        }
        if (clean.contains("hóa") || clean.contains("chemistry")) {
            return "/images/chemistry/chemistry1.jpg";
        }
        if (clean.contains("văn") || clean.contains("literature")) {
            return "/images/literature/literature1.jpg";
        }
        return "/images/general/general1.png";
    }

    private JPanel createCoverPanel(String seed) {
        Image coverImage = loadCoverImage(seed);
        RoundedImagePanel cover = new RoundedImagePanel(
                CLASS_CARD_WIDTH - CLASS_COVER_INSET * 2,
                CLASS_COVER_HEIGHT,
                TutorHubTheme.RADIUS_CARD,
                RoundedImagePanel.Fit.COVER
        );
        cover.setImage(coverImage);
        cover.setOverlayColor(coverImage == null ? null : new Color(12, 18, 32, 18));
        cover.setBorderColor(new Color(255, 255, 255, 58));
        cover.setFallbackText(initials(seed), primary);
        cover.setFallbackFont(TutorHubTheme.font(Font.BOLD, 24));
        cover.setBorder(new EmptyBorder(8, 8, 8, 8));
        return cover;
    }

    private JPanel createCoverShell(JPanel cover) {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setOpaque(false);
        shell.setBorder(new EmptyBorder(CLASS_COVER_INSET, CLASS_COVER_INSET, 0, CLASS_COVER_INSET));
        shell.add(cover, BorderLayout.CENTER);
        return shell;
    }

    private JLabel createStatusPill(String text, Color bg, Color fg) {
        JLabel label = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setOpaque(false);
        label.setFont(TutorHubTheme.font(Font.BOLD, 10));
        label.setForeground(fg);
        label.setBorder(new EmptyBorder(2, 8, 3, 8));
        label.setPreferredSize(new Dimension(Math.min(92, Math.max(68, label.getPreferredSize().width)), 23));
        return label;
    }

    private JLabel createMoreTrigger() {
        JLabel label = new JLabel("...", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = Boolean.TRUE.equals(getClientProperty("hover"));
                g2.setColor(new Color(255, 255, 255, hover ? 232 : 210));
                g2.fillRoundRect(5, 3, getWidth() - 10, getHeight() - 7, 11, 11);
                g2.setColor(new Color(17, 24, 39, hover ? 30 : 18));
                g2.drawRoundRect(5, 3, getWidth() - 11, getHeight() - 8, 11, 11);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setOpaque(false);
        label.setFont(TutorHubTheme.font(Font.BOLD, 14));
        label.setForeground(textDark);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setPreferredSize(new Dimension(34, 27));
        addHover(label);
        return label;
    }

    private JPanel createLoadingState() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(CLASS_CARD_WIDTH * 3 + 32, CLASS_CARD_HEIGHT));
        for (int i = 0; i < 3; i++) {
            row.add(createSkeletonCard(i));
        }
        return row;
    }

    private JPanel createSkeletonCard(int index) {
        ElevatedPanel card = new ElevatedPanel(15, surface);
        card.setLayout(new MigLayout("insets 0, fillx, wrap 1", "[grow,fill]", "[]10[]6[]6[]push[]"));
        card.setPreferredSize(new Dimension(CLASS_CARD_WIDTH, CLASS_CARD_HEIGHT));
        card.add(createSkeletonBlock(CLASS_CARD_WIDTH, CLASS_COVER_HEIGHT, 15, 20 + index * 8), "h " + CLASS_COVER_HEIGHT + "!");
        card.add(createSkeletonBlock(142, 14, 7, 24 + index * 8), "gapleft 13, w 142!, h 14!");
        card.add(createSkeletonBlock(94, 11, 6, 18 + index * 8), "gapleft 13, w 94!, h 11!");
        card.add(createSkeletonBlock(166, 10, 5, 16 + index * 8), "gapleft 13, w 166!, h 10!");
        card.add(createSkeletonBlock(72, 30, 9, 22 + index * 8), "gapleft 13, gapbottom 12, w 72!, h 30!");
        return card;
    }

    private JComponent createSkeletonBlock(int width, int height, int radius, int alpha) {
        JComponent block = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(128, 141, 164, alpha));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                g2.dispose();
            }
        };
        block.setOpaque(false);
        block.setPreferredSize(new Dimension(width, height));
        return block;
    }

    private JPanel createEmptyState(String titleText, String descText, String actionText, Runnable action) {
        JPanel empty = new ElevatedPanel(18, surface);
        empty.setPreferredSize(new Dimension(760, 176));
        empty.setLayout(new MigLayout("insets 28 30 28 30, fill", "[60!][grow][]", "[grow,center]"));

        JPanel glyph = createStateGlyph(actionText);

        JLabel title = new JLabel(titleText);
        title.setFont(TutorHubTheme.font(Font.BOLD, 18));
        title.setForeground(textDark);

        JLabel desc = new JLabel(descText);
        desc.setFont(TutorHubTheme.font(Font.PLAIN, 13));
        desc.setForeground(textMuted);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(title);
        copy.add(Box.createVerticalStrut(7));
        copy.add(desc);

        JButton button = createRoundedButton(actionText, primary, Color.WHITE);
        button.setFont(TutorHubTheme.font(Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        button.addActionListener(e -> action.run());

        empty.add(glyph, "cell 0 0, align center");
        empty.add(copy, "cell 1 0, growx");
        empty.add(button, "cell 2 0, w 118!, h 38!, alignx right");
        return empty;
    }

    private JPanel createStateGlyph(String actionText) {
        JPanel glyph = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TutorHubTheme.alpha(primary, 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(TutorHubTheme.alpha(primary, 28));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        glyph.setOpaque(false);
        glyph.setPreferredSize(new Dimension(52, 52));
        JLabel mark = new JLabel(glyphText(actionText), SwingConstants.CENTER);
        mark.setFont(TutorHubTheme.font(Font.BOLD, 20));
        mark.setForeground(primary);
        glyph.add(mark, BorderLayout.CENTER);
        return glyph;
    }

    private String glyphText(String actionText) {
        if (actionText.contains("Xóa")) return "x";
        if (actionText.contains("Làm")) return "R";
        return "+";
    }

    private JPanel createEmptyState() {
        JPanel empty = new ElevatedPanel(18, surface);
        empty.setPreferredSize(new Dimension(760, 190));
        empty.setLayout(new MigLayout("insets 30 32 30 32, fill", "[grow][]", "[grow]"));

        JLabel title = new JLabel("Chưa có lớp học phù hợp");
        title.setFont(new Font("Segoe UI", Font.BOLD, 19));
        title.setForeground(textDark);

        JLabel desc = new JLabel("Tạo lớp học mới, đăng public lesson hoặc tham gia lớp bằng mã để bắt đầu.");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        desc.setForeground(textMuted);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(title);
        copy.add(Box.createVerticalStrut(8));
        copy.add(desc);

        JButton create = createRoundedButton("Tạo lớp học", primary, Color.WHITE);
        create.setFont(new Font("Segoe UI", Font.BOLD, 13));
        create.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        create.addActionListener(e -> promptCreateClassThroughServer());

        empty.add(copy, "grow");
        empty.add(create, "w 128!, h 40!");
        return empty;
    }

    private JPanel createSmallCommand(String text, Runnable action) {
        JPanel button = new ElevatedPanel(12, surface);
        button.setLayout(new BorderLayout());
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.setPreferredSize(new Dimension(108, 42));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(primary);
        button.add(label, BorderLayout.CENTER);
        addHover(button);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return button;
    }

    private JPanel createInfoLine(String iconName, String text, Color color) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

        JLabel icon = new JLabel();
        try {
            FlatSVGIcon svg = new FlatSVGIcon("images/icon_svg/" + iconName + ".svg", 11, 11);
            svg.setColorFilter(new FlatSVGIcon.ColorFilter(c -> textMuted));
            icon.setIcon(svg);
        } catch (Exception ignored) {
            icon.setText("-");
            icon.setForeground(textMuted);
        }

        JLabel label = new JLabel(shortText(text, 25));
        label.setFont(TutorHubTheme.font(Font.PLAIN, 11));
        label.setForeground(color);
        row.add(icon);
        row.add(label);
        return row;
    }

    private JButton createRoundedButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = Boolean.TRUE.equals(getClientProperty("hover"));
                Color fill = isEnabled() ? (hover ? brighten(background, 1.05f) : background) : Color.decode("#E9EBF2");
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 9, 9);
                if (!isEnabled()) {
                    g2.setColor(Color.decode("#D7DAE3"));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 9, 9);
                }
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public void updateUI() {
                super.updateUI();
                setContentAreaFilled(false);
                setBorderPainted(false);
                setOpaque(false);
            }
        };
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                button.putClientProperty("hover", true);
                button.repaint();
            }

            @Override public void mouseExited(MouseEvent e) {
                button.putClientProperty("hover", false);
                button.repaint();
            }
        });
        return button;
    }

    private Color brighten(Color color, float factor) {
        return new Color(
                Math.min(255, Math.round(color.getRed() * factor)),
                Math.min(255, Math.round(color.getGreen() * factor)),
                Math.min(255, Math.round(color.getBlue() * factor)),
                color.getAlpha()
        );
    }

    private void promptCreateClassThroughServer() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        NewClassDialog dialog = new NewClassDialog(owner, "My Account", this::sendCreateClassRequest);
        dialog.setVisible(true);
    }

    private void sendCreateClassRequest(String className, String organizationName) {
        String payload = className + "|" + organizationName;
        try {
            NetworkManager.getInstance().sendPacket(new Packet("CREATE_CLASSROOM_AND_ENTER", payload));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể gửi yêu cầu tạo lớp: " + e.getMessage());
        }
    }

    private void promptCreatePublicLesson() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        String teacherName = dashboard != null ? dashboard.getUserName() : "Teacher";
        PublicLessonDialog dialog = new PublicLessonDialog(owner, "My Account", teacherName, this::sendCreatePublicLessonRequest);
        dialog.setVisible(true);
    }

    private void sendCreatePublicLessonRequest(PublicLessonDialog.Request request) {
        String payload = request.getLessonName() + "|" +
                request.getOrganizationName() + "|" +
                request.getStartMillis() + "|" +
                request.getDurationMinutes() + "|" +
                request.getStageLayout() + "|" +
                request.isLobbyEnabled() + "|" +
                request.isAllowStudentDraw() + "|" +
                request.isRecordingEnabled() + "|" +
                request.getCoTeachers();
        try {
            NetworkManager.getInstance().sendPacket(new Packet("CREATE_PUBLIC_LESSON", payload));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể gửi public lesson: " + e.getMessage());
        }
    }

    private void promptJoinPublicLesson() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JoinPublicLessonDialog dialog = new JoinPublicLessonDialog(owner, this::sendJoinPublicLessonRequest);
        dialog.setVisible(true);
    }

    private void sendJoinPublicLessonRequest(String joinCodeOrLink) {
        try {
            NetworkManager.getInstance().sendPacket(new Packet("JOIN_PUBLIC_LESSON", joinCodeOrLink));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể tham gia lớp: " + e.getMessage());
        }
    }

        private void enterLesson(ClassroomLessonModel lesson) {
        if (dashboard == null) return;
        String role = isCurrentUserTeacher(lesson) ? "teacher" : "student";
        
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        PreJoinDialog preJoin = new PreJoinDialog(owner, valueOrDefault(lesson.getTitle(), "Lớp học"), role, () -> {
            dashboard.openLiveLesson(
                    String.valueOf(lesson.getClassroomId()),
                    String.valueOf(lesson.getId()),
                    valueOrDefault(lesson.getBoardId(), "LESSON_" + lesson.getId()),
                    valueOrDefault(lesson.getTitle(), "Lớp học"),
                    role
            );
        });
        preJoin.setVisible(true);
    }

    private void setupLessonMenu(JLabel trigger, ClassroomLessonModel lesson) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(border));
        addMenuItem(popup, "Vào lớp", () -> enterLesson(lesson), !isWaitingForApproval(lesson));
        addMenuItem(popup, "Xem chi tiết", () -> showLessonDetail(lesson), true);
        if (isPublicLesson(lesson) && isCurrentUserTeacher(lesson)) {
            popup.addSeparator();
            addMenuItem(popup, "Lobby", () -> requestWaitingRoom(lesson), lesson.isLobbyEnabled());
            addMenuItem(popup, "Copy Code", () -> copyPublicLessonCode(lesson.getJoinCode()), hasText(lesson.getJoinCode()));
            addMenuItem(popup, "Copy Link", () -> copyPublicLessonLink(lesson.getJoinCode()), hasText(lesson.getJoinCode()));
        }
        trigger.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                popup.show(trigger, -130, trigger.getHeight());
            }
        });
    }

    private void setupClassroomMenu(JLabel trigger, ClassroomGroupModel classroom) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(border));
        addMenuItem(popup, "Vào lớp", () -> {
            if (dashboard != null) {
                dashboard.openLiveClassroom(
                        String.valueOf(classroom.getId()),
                        valueOrDefault(classroom.getName(), "Lớp học"),
                        isCurrentUserOwner(classroom) ? "teacher" : "student"
                );
            }
        }, true);
        addMenuItem(popup, "Xem chi tiết", () -> showClassroomDetail(classroom), true);
        trigger.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                popup.show(trigger, -130, trigger.getHeight());
            }
        });
    }

    private void addMenuItem(JPopupMenu popup, String label, Runnable action, boolean enabled) {
        JMenuItem item = new JMenuItem("  " + label + "  ");
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        item.setPreferredSize(new Dimension(150, 32));
        item.setEnabled(enabled);
        item.addActionListener(e -> action.run());
        popup.add(item);
    }

        private void showLessonDetail(ClassroomLessonModel lesson) {
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        ClassroomDetailDialog dialog = new ClassroomDetailDialog(owner, lesson);
        dialog.setVisible(true);
    }

        private void showClassroomDetail(ClassroomGroupModel classroom) {
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        ClassroomDetailDialog dialog = new ClassroomDetailDialog(owner, classroom);
        dialog.setVisible(true);
    }

    private void requestWaitingRoom(ClassroomLessonModel lesson) {
        try {
            NetworkManager.getInstance().sendPacket(new Packet("GET_PUBLIC_LESSON_WAITING_ROOM", String.valueOf(lesson.getId())));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể tải lobby: " + e.getMessage());
        }
    }

    public void showWaitingRoom(List<ClassroomMemberModel> members) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        WaitingRoomDialog dialog = new WaitingRoomDialog(owner, members, this::approveWaitingMember);
        dialog.setVisible(true);
    }

    private void approveWaitingMember(ClassroomMemberModel member) {
        String payload = member.getLessonId() + "|" + member.getUserId();
        try {
            NetworkManager.getInstance().sendPacket(new Packet("APPROVE_PUBLIC_LESSON_STUDENT", payload));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể duyệt học viên: " + e.getMessage());
        }
    }

    private void copyPublicLessonCode(String joinCode) {
        copyToClipboard(joinCode);
        JOptionPane.showMessageDialog(this, "Đã sao chép mã public lesson.");
    }

    private void copyPublicLessonLink(String joinCode) {
        copyToClipboard(InviteLinkDialog.buildInviteLink(joinCode));
        JOptionPane.showMessageDialog(this, "Đã sao chép link mời public lesson.");
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(valueOrDefault(text, "")), null);
    }

    private boolean isCurrentUserTeacher(ClassroomLessonModel lesson) {
        return dashboard != null && dashboard.getCurrentUserId() == lesson.getCreatedBy();
    }

    private boolean isCurrentUserOwner(ClassroomGroupModel classroom) {
        return dashboard != null && dashboard.getCurrentUserId() == classroom.getOwnerId();
    }

    private boolean isPublicLesson(ClassroomLessonModel lesson) {
        return "PUBLIC".equalsIgnoreCase(valueOrDefault(lesson.getLessonType(), ""));
    }

    private boolean isWaitingForApproval(ClassroomLessonModel lesson) {
        return "WAITING".equalsIgnoreCase(valueOrDefault(lesson.getMemberStatus(), ""));
    }

    private boolean isEnded(ClassroomLessonModel lesson) {
        String status = valueOrDefault(lesson.getStatus(), "");
        return "ENDED".equalsIgnoreCase(status) || "FINISHED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);
    }

    private String statusText(ClassroomLessonModel lesson) {
        if (isWaitingForApproval(lesson)) return "Chờ duyệt";
        if (isEnded(lesson)) return "Đã kết thúc";
        if (isPublicLesson(lesson)) return "Public";
        return valueOrDefault(lesson.getStatus(), "Live");
    }

    private Color statusBackground(ClassroomLessonModel lesson) {
        if (isWaitingForApproval(lesson)) return Color.decode("#FEF3C7");
        if (isEnded(lesson)) return Color.decode("#F3F4F6");
        if (isPublicLesson(lesson)) return Color.decode("#EEF2FF");
        return Color.decode("#EAFBF0");
    }

    private Color statusForeground(ClassroomLessonModel lesson) {
        if (isWaitingForApproval(lesson)) return Color.decode("#92400E");
        if (isEnded(lesson)) return Color.decode("#4B5563");
        if (isPublicLesson(lesson)) return primary;
        return Color.decode("#087443");
    }

    private String formatSchedule(ClassroomLessonModel lesson) {
        String duration = lesson.getDurationMinutes() > 0 ? lesson.getDurationMinutes() + " phút" : "40 phút";
        if (lesson.getStartTime() == null) {
            return "Bắt đầu ngay · " + duration;
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(lesson.getStartTime()) + " Â· " + duration;
    }

    private String friendlyLessonTitle(ClassroomLessonModel lesson) {
        String raw = valueOrDefault(lesson.getTitle(), "Lớp học không tên");
        String clean = raw.replace("'s public lesson", "")
                .replace("â€™s public lesson", "")
                .replace(" public lesson", "")
                .trim();
        if (looksLikeEmail(clean)) {
            return "Lớp của " + nameFromEmail(clean);
        }
        if (raw.toLowerCase(Locale.ROOT).contains("public lesson") && !clean.equals(raw)) {
            return clean.isEmpty() ? "Public Lesson" : "Public lesson của " + clean;
        }
        if (looksLikeEmail(raw)) {
            return "Lớp của " + nameFromEmail(raw);
        }
        return raw;
    }

    private String friendlyClassroomTitle(ClassroomGroupModel classroom) {
        String raw = valueOrDefault(classroom.getName(), "Lớp học không tên");
        if (looksLikeEmail(raw)) {
            return "Lớp của " + nameFromEmail(raw);
        }
        return raw;
    }

    private String friendlyOrganizationName(String value) {
        String clean = valueOrDefault(value, "TutorHub Enterprise");
        return looksLikeEmail(clean) ? nameFromEmail(clean) : clean;
    }

    private boolean looksLikeEmail(String value) {
        String clean = valueOrDefault(value, "");
        return clean.contains("@") && clean.indexOf("@") > 0;
    }

    private String nameFromEmail(String value) {
        String clean = valueOrDefault(value, "");
        int at = clean.indexOf("@");
        if (at > 0) {
            clean = clean.substring(0, at);
        }
        clean = clean.replace(".", " ").replace("_", " ").replace("-", " ").trim();
        return clean.isEmpty() ? "TutorHub" : clean;
    }

    private String initials(String text) {
        String clean = valueOrDefault(text, "TH").trim();
        String[] parts = clean.split("\\s+");
        if (parts.length == 1) {
            return clean.substring(0, Math.min(clean.length(), 2)).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String shortText(String text, int max) {
        String clean = valueOrDefault(text, "");
        return clean.length() <= max ? clean : clean.substring(0, max - 3) + "...";
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void addHover(JComponent component, JComponent... linkedComponents) {
        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                setHoverState(component, true, linkedComponents);
            }

            @Override public void mouseExited(MouseEvent e) {
                setHoverState(component, false, linkedComponents);
            }
        };
        component.addMouseListener(adapter);
        for (Component child : component.getComponents()) {
            if (child instanceof JComponent) {
                child.addMouseListener(adapter);
            }
        }
    }

    private void addMouseListenerRecursively(Component component, MouseAdapter adapter) {
        component.addMouseListener(adapter);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                addMouseListenerRecursively(child, adapter);
            }
        }
    }

    private void setHoverState(JComponent component, boolean hover, JComponent... linkedComponents) {
        component.putClientProperty("hover", hover);
        component.repaint();
        for (JComponent linked : linkedComponents) {
            linked.putClientProperty("hover", hover);
            linked.repaint();
        }
    }

    private class ScrollableContentPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 80, 120);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private class ElevatedPanel extends JPanel {
        private final int radius;
        private final Color fill;

        ElevatedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean hover = Boolean.TRUE.equals(getClientProperty("hover"));
                g2.setColor(new Color(31, 41, 55, hover ? TutorHubTheme.SHADOW_ALPHA_HOVER : TutorHubTheme.SHADOW_ALPHA));
            g2.fillRoundRect(1, 3, getWidth() - 2, getHeight() - 4, radius, radius);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 2, radius, radius);
            g2.setColor(hover ? hoverBorder : border);
            g2.setStroke(new BasicStroke(1.1f));
            g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 3, radius, radius);
            if (hover) {
                g2.setColor(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 8));
                g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 4, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

}



