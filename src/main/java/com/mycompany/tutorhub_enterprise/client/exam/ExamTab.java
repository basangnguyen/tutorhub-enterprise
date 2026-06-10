package com.mycompany.tutorhub_enterprise.client.exam;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

public class ExamTab extends JPanel {
    private JTable examTable;
    private DefaultTableModel tableModel;
    private int currentUserId;
    private String currentUserRole;
    private NetworkManager networkManager;

    public ExamTab(int userId, String role, NetworkManager networkManager) {
        this.currentUserId = userId;
        this.currentUserRole = role;
        this.networkManager = networkManager;
        
        setLayout(new BorderLayout());
        
        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Làm mới");
        refreshBtn.addActionListener(e -> loadExams());
        toolbar.add(refreshBtn);
        
        if ("TUTOR".equalsIgnoreCase(role)) {
            JButton createBtn = new JButton("Tạo Kỳ Thi");
            createBtn.addActionListener(e -> openCreateExamDialog());
            toolbar.add(createBtn);
        }
        
        add(toolbar, BorderLayout.NORTH);
        
        // Table
        String[] columns = {"ID", "Tên Kỳ Thi", "Thời gian (phút)", "Trạng thái", "Hành động"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        examTable = new JTable(tableModel);
        add(new JScrollPane(examTable), BorderLayout.CENTER);
        
        // Action Panel at the bottom
        JPanel actionPanel = new JPanel();
        JButton startBtn = new JButton("Tham gia kỳ thi");
        startBtn.addActionListener(e -> startSelectedExam(startBtn));
        actionPanel.add(startBtn);
        add(actionPanel, BorderLayout.SOUTH);
        
        loadExams();
    }
    
    private void loadExams() {
        // Send request to server to fetch exams
        if (networkManager != null) {
            try { networkManager.sendPacket(new Packet("GET_EXAMS", "")); } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    public void updateExamList(java.util.List<com.mycompany.tutorhub_enterprise.models.exam.Exam> exams) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (com.mycompany.tutorhub_enterprise.models.exam.Exam exam : exams) {
                Object[] row = {
                    exam.id,
                    exam.title,
                    exam.durationMins,
                    exam.status,
                    "Chi tiết"
                };
                tableModel.addRow(row);
            }
        });
    }
    
    private void openCreateExamDialog() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent, "Tạo Kỳ Thi Mới", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.add(new ExamCreatorPanel(currentUserId, networkManager, dialog));
        dialog.setVisible(true);
    }
    
    private void startSelectedExam(JButton startBtn) {
        int selectedRow = examTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một kỳ thi trước khi tham gia.");
            return;
        }
        
        String status = (String) tableModel.getValueAt(selectedRow, 3);
        
        if ("DRAFT".equalsIgnoreCase(status) || "Nháp".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Kỳ thi này đang là bản nháp, chưa thể tham gia.");
            return;
        }
        
        if ("CLOSED".equalsIgnoreCase(status) || "ARCHIVED".equalsIgnoreCase(status) || "Đã đóng".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Kỳ thi này đã đóng hoặc kết thúc.");
            return;
        }

        if (!"ACTIVE".equalsIgnoreCase(status) && !"Đang diễn ra".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Kỳ thi này không trong trạng thái mở để tham gia.");
            return;
        }

        try {
            int examId = (int) tableModel.getValueAt(selectedRow, 0);
            com.mycompany.tutorhub_enterprise.client.exam.integration.SecureExamLauncherBridge.launchExam(examId);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Chưa tìm thấy TutorHub Secure Exam. Vui lòng cài đặt ứng dụng thi bảo mật trước.");
        }
    }
}
