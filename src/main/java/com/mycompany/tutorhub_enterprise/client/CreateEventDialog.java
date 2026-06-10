package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.server.dao.TutorScheduleDAO;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class CreateEventDialog extends JDialog {

    private boolean isSaved = false;

    public CreateEventDialog(Frame parent, LocalDate clickedDate, LocalTime clickedTime, int tutorId, TutorScheduleDAO dao) {
        super(parent, "Tạo Lịch Dạy Mới", true); // Modal dialog
        setLayout(new BorderLayout());
        setSize(350, 250);
        setLocationRelativeTo(parent);
        
        // --- FORM NHẬP LIỆU ---
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        formPanel.add(new JLabel("Tên lớp/Sự kiện:"));
        JTextField txtTitle = new JTextField();
        formPanel.add(txtTitle);

        formPanel.add(new JLabel("Phòng học / Link Meet:"));
        JTextField txtLocation = new JTextField("Phòng Online");
        formPanel.add(txtLocation);

        formPanel.add(new JLabel("Loại lịch:"));
        JComboBox<String> cbCategory = new JComboBox<>(new String[]{"CLASS", "EVENT"});
        formPanel.add(cbCategory);
        
        // Hiển thị giờ (Chỉ xem)
        formPanel.add(new JLabel("Giờ bắt đầu:"));
        JLabel lblTime = new JLabel(clickedTime.toString() + " (Thứ " + getDBDayOfWeek(clickedDate) + ")");
        lblTime.setForeground(Color.BLUE);
        formPanel.add(lblTime);

        add(formPanel, BorderLayout.CENTER);

        // --- BUTTON LƯU ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Lưu Lịch Học");
        btnSave.setBackground(Color.decode("#4F46E5"));
        btnSave.setForeground(Color.WHITE);
        
        btnSave.addActionListener(e -> {
            String title = txtTitle.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên lớp!");
                return;
            }

            int dayOfWeek = getDBDayOfWeek(clickedDate);
            java.sql.Time startTime = java.sql.Time.valueOf(clickedTime);
            java.sql.Time endTime = java.sql.Time.valueOf(clickedTime.plusHours(1)); // Mặc định tạo lớp 1 tiếng
            String color = cbCategory.getSelectedItem().equals("CLASS") ? "#10B981" : "#EA4335";

            // Gọi DAO lưu xuống Neon DB
            boolean success = dao.insertSchedule(tutorId, title, dayOfWeek, startTime, endTime, txtLocation.getText(), cbCategory.getSelectedItem().toString(), color);
            
            if (success) {
                isSaved = true;
                dispose(); // Đóng form
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu vào Database!");
            }
        });
        
        btnPanel.add(btnSave);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public boolean isSaved() {
        return isSaved;
    }

    // Chuyển LocalDate sang Thứ của Database (CN=1, T2=2...)
    private int getDBDayOfWeek(LocalDate date) {
        int javaDay = date.getDayOfWeek().getValue();
        return (javaDay == 7) ? 1 : javaDay + 1;
    }
}