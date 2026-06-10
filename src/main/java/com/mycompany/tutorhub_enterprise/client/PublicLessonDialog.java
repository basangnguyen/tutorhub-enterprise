package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PublicLessonDialog extends JDialog {
    public interface PostListener {
        void onPost(Request request);
    }

    public static class Request {
        private final String lessonName;
        private final String organizationName;
        private final long startMillis;
        private final int durationMinutes;
        private final String stageLayout;
        private final boolean lobbyEnabled;
        private final boolean allowStudentDraw;
        private final boolean recordingEnabled;
        private final String coTeachers;

        public Request(String lessonName, String organizationName, long startMillis, int durationMinutes,
                       String stageLayout, boolean lobbyEnabled, boolean allowStudentDraw,
                       boolean recordingEnabled, String coTeachers) {
            this.lessonName = lessonName;
            this.organizationName = organizationName;
            this.startMillis = startMillis;
            this.durationMinutes = durationMinutes;
            this.stageLayout = stageLayout;
            this.lobbyEnabled = lobbyEnabled;
            this.allowStudentDraw = allowStudentDraw;
            this.recordingEnabled = recordingEnabled;
            this.coTeachers = coTeachers;
        }

        public String getLessonName() { return lessonName; }
        public String getOrganizationName() { return organizationName; }
        public long getStartMillis() { return startMillis; }
        public int getDurationMinutes() { return durationMinutes; }
        public String getStageLayout() { return stageLayout; }
        public boolean isLobbyEnabled() { return lobbyEnabled; }
        public boolean isAllowStudentDraw() { return allowStudentDraw; }
        public boolean isRecordingEnabled() { return recordingEnabled; }
        public String getCoTeachers() { return coTeachers; }
    }

    private static final int MAX_LESSON_NAME_LENGTH = 50;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final String organizationName;
    private final String teacherName;
    private final PostListener postListener;
    private final HintTextField lessonNameField;
    private final JTextField dateField;
    private final JTextField timeField;
    private final HintTextField coTeachersField;
    private final JComboBox<String> durationCombo;
    private final JComboBox<String> stageLayoutCombo;
    private final JCheckBox lobbyCheck;
    private final JCheckBox allowDrawCheck;
    private final JCheckBox recordingCheck;
    private final JLabel validationLabel;
    private final JButton postButton;

    public PublicLessonDialog(Window owner, String organizationName, String teacherName, PostListener postListener) {
        super(owner, "New Public Lesson", ModalityType.APPLICATION_MODAL);
        this.organizationName = organizationName == null || organizationName.trim().isEmpty() ? "My Account" : organizationName.trim();
        this.teacherName = teacherName == null || teacherName.trim().isEmpty() ? "Teacher" : teacherName.trim();
        this.postListener = postListener;

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        this.lessonNameField = new HintTextField("Enter public lesson name");
        this.lessonNameField.setText(this.teacherName + "'s public lesson");
        this.dateField = new JTextField(now.toLocalDate().format(DATE_FORMAT));
        this.timeField = new JTextField(now.toLocalTime().format(TIME_FORMAT));
        this.coTeachersField = new HintTextField("Add co-teachers by name or email");
        this.durationCombo = new JComboBox<>(new String[]{"40 min", "60 min", "90 min", "120 min"});
        this.stageLayoutCombo = new JComboBox<>(new String[]{"1V6", "1V1", "1V4", "1V12"});
        this.lobbyCheck = new JCheckBox("Waiting room / lobby");
        this.allowDrawCheck = new JCheckBox("Allow students to draw");
        this.recordingCheck = new JCheckBox("Record classroom");
        this.validationLabel = new JLabel(" ");
        this.postButton = new JButton("Post");

        lobbyCheck.setSelected(true);

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setContentPane(createContent());
        setSize(720, 760);
        setMinimumSize(new Dimension(680, 680));
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(postButton);
        updatePostState();
    }

    private JComponent createContent() {
        RoundedPanel root = new RoundedPanel(12, Color.WHITE);
        root.setLayout(new BorderLayout());

        root.add(createHeader(), BorderLayout.NORTH);
        root.add(createScrollableForm(), BorderLayout.CENTER);
        root.add(createFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(20, 28, 18, 28));

        JLabel title = new JLabel("New Public Lesson");
        title.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        title.setForeground(Color.decode("#202124"));

        JButton close = new JButton("x");
        close.setPreferredSize(new Dimension(34, 34));
        close.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        close.setForeground(Color.decode("#202124"));
        close.setBackground(Color.WHITE);
        close.setBorder(BorderFactory.createEmptyBorder());
        close.setFocusPainted(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dispose());

        header.add(title, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);
        return header;
    }

    private JComponent createScrollableForm() {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(12, 30, 20, 30));

        form.add(createLabel("Organization"));
        form.add(Box.createVerticalStrut(8));
        form.add(createReadOnlyRow(organizationName, "v"));
        form.add(Box.createVerticalStrut(24));

        form.add(createLabel("Public lesson name"));
        form.add(Box.createVerticalStrut(8));
        form.add(createInputBox(lessonNameField, 54));
        form.add(Box.createVerticalStrut(6));

        validationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        validationLabel.setForeground(Color.decode("#9CA3AF"));
        validationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(validationLabel);
        form.add(Box.createVerticalStrut(18));

        form.add(createLabel("Start time and duration"));
        form.add(Box.createVerticalStrut(10));
        form.add(createUpgradeNotice());
        form.add(Box.createVerticalStrut(10));
        form.add(createTimeDurationRow());
        form.add(Box.createVerticalStrut(24));

        form.add(createLabel("Post to"));
        form.add(Box.createVerticalStrut(10));
        form.add(createReadOnlyRow("Teacher", teacherName));
        form.add(Box.createVerticalStrut(10));
        form.add(createInputBox(coTeachersField, 48));
        form.add(Box.createVerticalStrut(24));

        form.add(createSectionHeader("Classroom", "Expanded"));
        form.add(Box.createVerticalStrut(10));
        form.add(createStageRow());
        form.add(Box.createVerticalStrut(10));
        form.add(createSettingsPanel());

        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updatePostState(); }
            public void removeUpdate(DocumentEvent e) { updatePostState(); }
            public void changedUpdate(DocumentEvent e) { updatePostState(); }
        };
        lessonNameField.getDocument().addDocumentListener(listener);
        dateField.getDocument().addDocumentListener(listener);
        timeField.getDocument().addDocumentListener(listener);
        coTeachersField.getDocument().addDocumentListener(listener);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.decode("#EEF0F2")));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        return scroll;
    }

    private JComponent createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 20));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 28, 0, 28));

        JButton cancel = new JButton("Cancel");
        cancel.setPreferredSize(new Dimension(130, 54));
        cancel.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        cancel.setBackground(Color.decode("#F1F3F4"));
        cancel.setForeground(Color.decode("#202124"));
        cancel.setBorder(BorderFactory.createEmptyBorder());
        cancel.setFocusPainted(false);
        cancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancel.addActionListener(e -> dispose());

        postButton.setPreferredSize(new Dimension(130, 54));
        postButton.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        postButton.setBorder(BorderFactory.createEmptyBorder());
        postButton.setFocusPainted(false);
        postButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        postButton.addActionListener(e -> submit());

        footer.add(cancel);
        footer.add(postButton);
        return footer;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        label.setForeground(Color.decode("#202124"));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JComponent createReadOnlyRow(String leftText, String rightText) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(Color.decode("#F7F8FA"));
        row.setBorder(new EmptyBorder(0, 16, 0, 16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row.setPreferredSize(new Dimension(640, 58));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel left = new JLabel(leftText);
        left.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        left.setForeground(Color.decode("#202124"));

        JLabel right = new JLabel(rightText);
        right.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        right.setForeground(Color.decode("#8A8D91"));

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JComponent createInputBox(JTextField field, int height) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(Color.decode("#F7F8FA"));
        panel.setBorder(new EmptyBorder(0, 16, 0, 16));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setPreferredSize(new Dimension(640, height));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setOpaque(false);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createUpgradeNotice() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(Color.decode("#EAF7F2"));
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        panel.setPreferredSize(new Dimension(640, 72));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel text = new JLabel("<html>Free plan offers <b>40 min</b> for every lesson. Upgrade for longer sessions and recordings</html>");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        text.setForeground(Color.decode("#52616B"));

        JLabel action = new JLabel("Upgrade >");
        action.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        action.setForeground(Color.decode("#00A86B"));

        panel.add(text, BorderLayout.CENTER);
        panel.add(action, BorderLayout.EAST);
        return panel;
    }

    private JComponent createTimeDurationRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        row.setPreferredSize(new Dimension(640, 54));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(createInputBox(dateField, 54));
        row.add(createInputBox(timeField, 54));
        row.add(createComboBox(durationCombo, 54));
        return row;
    }

    private JComponent createStageRow() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setBackground(Color.decode("#F7F8FA"));
        row.setBorder(new EmptyBorder(0, 16, 0, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        row.setPreferredSize(new Dimension(640, 58));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel left = new JLabel("People On Stage");
        left.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        left.setForeground(Color.decode("#202124"));
        row.add(left, BorderLayout.WEST);
        row.add(stageLayoutCombo, BorderLayout.EAST);
        return row;
    }

    private JComponent createComboBox(JComboBox<String> comboBox, int height) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(Color.decode("#F7F8FA"));
        panel.setBorder(new EmptyBorder(0, 12, 0, 12));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setPreferredSize(new Dimension(200, height));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        comboBox.setBackground(Color.decode("#F7F8FA"));
        comboBox.setBorder(BorderFactory.createEmptyBorder());
        panel.add(comboBox, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createSectionHeader(String title, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel left = createLabel(title);
        JLabel right = new JLabel(value + " v");
        right.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        right.setForeground(Color.decode("#8A8D91"));

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JComponent createSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 14, 0));
        panel.setOpaque(true);
        panel.setBackground(Color.decode("#F7F8FA"));
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        panel.setPreferredSize(new Dimension(640, 74));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(styleCheckBox(lobbyCheck));
        panel.add(styleCheckBox(allowDrawCheck));
        panel.add(styleCheckBox(recordingCheck));
        return panel;
    }

    private JCheckBox styleCheckBox(JCheckBox box) {
        box.setOpaque(false);
        box.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        box.setForeground(Color.decode("#202124"));
        box.setFocusPainted(false);
        return box;
    }

    private void updatePostState() {
        boolean valid = isValidForm();
        postButton.setEnabled(valid);
        postButton.setBackground(valid ? Color.decode("#2EDB68") : Color.decode("#E5E7EB"));
        postButton.setForeground(valid ? Color.BLACK : Color.WHITE);
    }

    private boolean isValidForm() {
        String name = lessonNameField.getText().trim();
        String coTeachers = coTeachersField.getText().trim();
        if (name.isEmpty()) {
            validationLabel.setText(" ");
            return false;
        }
        if (name.length() > MAX_LESSON_NAME_LENGTH) {
            validationLabel.setText(name.length() + "/" + MAX_LESSON_NAME_LENGTH + " characters");
            validationLabel.setForeground(Color.decode("#EF4444"));
            return false;
        }
        if (name.contains("|") || coTeachers.contains("|")) {
            validationLabel.setText("Text cannot contain the | character.");
            validationLabel.setForeground(Color.decode("#EF4444"));
            return false;
        }
        if (parseStartMillis() < 0) {
            validationLabel.setText("Use date yyyy-MM-dd and time HH:mm.");
            validationLabel.setForeground(Color.decode("#EF4444"));
            return false;
        }
        validationLabel.setText(name.length() + "/" + MAX_LESSON_NAME_LENGTH + " characters");
        validationLabel.setForeground(Color.decode("#9CA3AF"));
        return true;
    }

    private void submit() {
        if (!isValidForm()) {
            updatePostState();
            return;
        }

        Request request = new Request(
                lessonNameField.getText().trim(),
                organizationName,
                parseStartMillis(),
                parseDurationMinutes(),
                (String) stageLayoutCombo.getSelectedItem(),
                lobbyCheck.isSelected(),
                allowDrawCheck.isSelected(),
                recordingCheck.isSelected(),
                coTeachersField.getText().trim()
        );

        dispose();
        if (postListener != null) {
            postListener.onPost(request);
        }
    }

    private long parseStartMillis() {
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim(), DATE_FORMAT);
            LocalTime time = LocalTime.parse(timeField.getText().trim(), TIME_FORMAT);
            return LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return -1;
        }
    }

    private int parseDurationMinutes() {
        String selected = String.valueOf(durationCombo.getSelectedItem());
        String digits = selected.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return 40;
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
