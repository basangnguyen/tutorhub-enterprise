package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.ClassroomMemberModel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.List;

public class WaitingRoomDialog extends JDialog {
    public interface ApproveListener {
        void onApprove(ClassroomMemberModel member);
    }

    private final List<ClassroomMemberModel> members;
    private final ApproveListener approveListener;

    public WaitingRoomDialog(Window owner, List<ClassroomMemberModel> members, ApproveListener approveListener) {
        super(owner, "Waiting Room", ModalityType.APPLICATION_MODAL);
        this.members = members;
        this.approveListener = approveListener;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setContentPane(createContent());
        setSize(560, 420);
        setMinimumSize(new Dimension(520, 360));
        setLocationRelativeTo(owner);
    }

    private JComponent createContent() {
        RoundedPanel root = new RoundedPanel(16, Color.WHITE);
        root.setLayout(new BorderLayout());
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Waiting Room");
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        title.setForeground(Color.decode("#202124"));

        JButton close = createTextButton("Close", Color.decode("#F1F3F4"), Color.decode("#202124"), 90);
        close.addActionListener(e -> dispose());

        header.add(title, BorderLayout.WEST);
        header.add(close, BorderLayout.EAST);

        JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        if (members == null || members.isEmpty()) {
            listPanel.add(createEmptyState());
        } else {
            for (int i = 0; i < members.size(); i++) {
                listPanel.add(createMemberRow(members.get(i)));
                if (i < members.size() - 1) {
                    listPanel.add(Box.createVerticalStrut(10));
                }
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setBorder(new EmptyBorder(18, 0, 0, 0));

        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        return root;
    }

    private JComponent createEmptyState() {
        JPanel empty = new JPanel(new BorderLayout());
        empty.setOpaque(true);
        empty.setBackground(Color.decode("#F7F8FA"));
        empty.setBorder(new EmptyBorder(28, 20, 28, 20));
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel text = new JLabel("No students are waiting right now.", SwingConstants.CENTER);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        text.setForeground(Color.decode("#6B7280"));
        empty.add(text, BorderLayout.CENTER);
        return empty;
    }

    private JComponent createMemberRow(ClassroomMemberModel member) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(true);
        row.setBackground(Color.decode("#F7F8FA"));
        row.setBorder(new EmptyBorder(14, 14, 14, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

        JLabel avatar = new JLabel(initials(member.getFullName()), SwingConstants.CENTER);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        avatar.setForeground(Color.decode("#087443"));
        avatar.setOpaque(true);
        avatar.setBackground(Color.decode("#DFF7E9"));
        avatar.setPreferredSize(new Dimension(42, 42));

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(clean(member.getFullName(), "Student"));
        name.setFont(new Font("Segoe UI", Font.BOLD, 14));
        name.setForeground(Color.decode("#202124"));

        JLabel meta = new JLabel(clean(member.getEmail(), "No email") + "  |  " + formatTime(member));
        meta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        meta.setForeground(Color.decode("#6B7280"));

        textBox.add(name);
        textBox.add(Box.createVerticalStrut(4));
        textBox.add(meta);

        JButton approve = createTextButton("Approve", Color.decode("#2EDB68"), Color.BLACK, 104);
        approve.addActionListener(e -> {
            approve.setEnabled(false);
            approve.setText("Approved");
            if (approveListener != null) {
                approveListener.onApprove(member);
            }
            dispose();
        });

        row.add(avatar, BorderLayout.WEST);
        row.add(textBox, BorderLayout.CENTER);
        row.add(approve, BorderLayout.EAST);
        return row;
    }

    private JButton createTextButton(String text, Color background, Color foreground, int width) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(width, 40));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private String initials(String value) {
        String text = clean(value, "S").trim();
        if (text.isEmpty()) {
            return "S";
        }
        String[] parts = text.split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    private String formatTime(ClassroomMemberModel member) {
        if (member.getJoinedAt() == null) {
            return "waiting";
        }
        return new SimpleDateFormat("HH:mm").format(member.getJoinedAt());
    }

    private String clean(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
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
