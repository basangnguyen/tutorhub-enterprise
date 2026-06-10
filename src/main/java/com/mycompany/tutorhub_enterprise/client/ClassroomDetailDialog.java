package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomLessonModel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;

public class ClassroomDetailDialog extends JDialog {

    private final Color background = TutorHubTheme.BACKGROUND;
    private final Color surface = TutorHubTheme.SURFACE;
    private final Color primaryBlue = TutorHubTheme.PRIMARY_BLUE;
    private final Color textDark = TutorHubTheme.TEXT_DARK;
    private final Color textMuted = TutorHubTheme.TEXT_MUTED;
    private final Color border = TutorHubTheme.BORDER;

    public ClassroomDetailDialog(Window owner, ClassroomLessonModel lesson) {
        super(owner, "Chi tiết buổi học", ModalityType.APPLICATION_MODAL);
        initUI(
            lesson.getTitle() != null ? lesson.getTitle() : "Buổi học",
            lesson.getOrganizationName() != null ? lesson.getOrganizationName() : "TutorHub Enterprise",
            "Mã tham gia: " + (lesson.getJoinCode() != null ? lesson.getJoinCode() : "Không có"),
            formatSchedule(lesson),
            "Học viên: " + Math.max(lesson.getSeatCount(), 1)
        );
    }
    
    public ClassroomDetailDialog(Window owner, ClassroomGroupModel classroom) {
        super(owner, "Chi tiết lớp học", ModalityType.APPLICATION_MODAL);
        initUI(
            classroom.getName() != null ? classroom.getName() : "Lớp học",
            classroom.getOrganizationName() != null ? classroom.getOrganizationName() : "TutorHub Enterprise",
            "Mã tham gia: " + (classroom.getJoinCode() != null ? classroom.getJoinCode() : "Không có"),
            "Trạng thái: " + (classroom.getStatus() != null ? classroom.getStatus() : "ACTIVE"),
            "Chưa có thông tin số lượng"
        );
    }

    private void initUI(String title, String org, String code, String schedule, String members) {
        setSize(400, 300);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(background);
        
        JPanel container = new JPanel(new MigLayout("insets 24, fillx, wrap 1", "[grow,fill]", "[]16[]24[]"));
        container.setBackground(background);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(textDark);
        
        JPanel infoBox = new ElevatedPanel(12, surface);
        infoBox.setLayout(new MigLayout("insets 16, fillx, wrap 1", "[grow,fill]", "[]8[]8[]8[]"));
        infoBox.setBorder(BorderFactory.createLineBorder(border));
        
        infoBox.add(createLabel(org, Font.BOLD, 14, textDark));
        infoBox.add(createLabel(code, Font.PLAIN, 13, textDark));
        infoBox.add(createLabel(schedule, Font.PLAIN, 13, textMuted));
        infoBox.add(createLabel(members, Font.PLAIN, 13, textMuted));
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionPanel.setOpaque(false);
        
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.addActionListener(e -> dispose());
        actionPanel.add(btnClose);
        
        container.add(titleLabel);
        container.add(infoBox);
        container.add(actionPanel);
        
        add(container);
    }
    
    private JLabel createLabel(String text, int style, int size, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", style, size));
        lbl.setForeground(color);
        return lbl;
    }
    
    private String formatSchedule(ClassroomLessonModel lesson) {
        if (lesson.getStartTime() == null) return "Chưa có lịch";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy");
        return "Bắt đầu: " + sdf.format(lesson.getStartTime());
    }
}
