package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.ClassroomMemberModel;
import com.mycompany.tutorhub_enterprise.models.Packet;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class PermissionPanel extends JPanel {
    private final String lessonId;
    private final JPanel listPanel;
    
    private final Color background = TutorHubTheme.BACKGROUND;
    private final Color surface = TutorHubTheme.SURFACE;
    private final Color textDark = TutorHubTheme.TEXT_DARK;
    private final Color textMuted = TutorHubTheme.TEXT_MUTED;

    public PermissionPanel(String lessonId) {
        this.lessonId = lessonId;
        setLayout(new BorderLayout());
        setBackground(background);
        setPreferredSize(new Dimension(280, 0));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, TutorHubTheme.BORDER));

        JLabel lblTitle = new JLabel("Thành viên");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(textDark);
        lblTitle.setBorder(new EmptyBorder(16, 16, 8, 16));
        add(lblTitle, BorderLayout.NORTH);

        listPanel = new JPanel(new MigLayout("insets 8, fillx, wrap 1", "[grow,fill]", "[]4[]"));
        listPanel.setBackground(background);
        
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        
        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnRefresh.addActionListener(e -> fetchMembers());
        
        JButton btnBoards = new JButton("Kho bảng vẽ");
        btnBoards.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBoards.addActionListener(e -> {
            sendPacketSafely(new Packet("GET_BOARDS_FOR_PICKER", ""));
        });
        
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(background);
        footer.add(btnRefresh);
        footer.add(btnBoards);
        add(footer, BorderLayout.SOUTH);
        
        fetchMembers();
    }
    
    public void fetchMembers() {
        sendPacketSafely(new Packet("GET_LESSON_MEMBERS", lessonId));
    }
    
    public void updateMembers(List<ClassroomMemberModel> members) {
        SwingUtilities.invokeLater(() -> {
            listPanel.removeAll();
            if (members.isEmpty()) {
                JLabel empty = new JLabel("Chưa có học viên");
                empty.setForeground(textMuted);
                listPanel.add(empty);
            } else {
                for (ClassroomMemberModel member : members) {
                    listPanel.add(createMemberItem(member));
                }
            }
            listPanel.revalidate();
            listPanel.repaint();
        });
    }
    
    private JPanel createMemberItem(ClassroomMemberModel member) {
        JPanel item = new JPanel();
        item.setBackground(surface);
        item.setLayout(new MigLayout("insets 12, fillx", "[grow][][][]", "[]"));
        item.setBorder(BorderFactory.createLineBorder(TutorHubTheme.BORDER));
        
        String name = member.getFullName() != null ? member.getFullName() : member.getEmail();
        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblName.setForeground(textDark);
        
        // Perm buttons (Draw, Mic, Cam)
        JToggleButton btnDraw = createTogglePermButton("Draw", "Draw");
        JToggleButton btnMic = createTogglePermButton("Mic", "Mic");
        JToggleButton btnCam = createTogglePermButton("Cam", "Cam");
        
        btnDraw.addActionListener(e -> updatePerm(member.getUserId(), "DRAW", btnDraw.isSelected()));
        btnMic.addActionListener(e -> updatePerm(member.getUserId(), "MIC", btnMic.isSelected()));
        btnCam.addActionListener(e -> updatePerm(member.getUserId(), "CAM", btnCam.isSelected()));
        
        item.add(lblName, "growx, pushx");
        item.add(btnDraw);
        item.add(btnMic);
        item.add(btnCam);
        return item;
    }
    
    private JToggleButton createTogglePermButton(String iconText, String tooltip) {
        JToggleButton btn = new JToggleButton(iconText);
        btn.setToolTipText(tooltip);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setMargin(new Insets(2, 4, 2, 4));
        btn.setFocusPainted(false);
        // By default, toggles are unchecked (no permission)
        return btn;
    }
    
    private void updatePerm(int targetUserId, String type, boolean isEnabled) {
        String payload = lessonId + "|" + targetUserId + "|" + type + "|" + isEnabled;
        sendPacketSafely(new Packet("UPDATE_STUDENT_PERMISSION", payload));
    }

    private void sendPacketSafely(Packet packet) {
        try {
            NetworkManager.getInstance().sendPacket(packet);
        } catch (Exception ex) {
            System.err.println("PermissionPanel network error: " + ex.getMessage());
        }
    }
}
