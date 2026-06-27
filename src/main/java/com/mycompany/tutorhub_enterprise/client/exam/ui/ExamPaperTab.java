package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.models.Packet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamPaperTab extends JPanel {

    private int userId;
    private String role;
    private NetworkManager networkManager;
    private JTable paperTable;
    private DefaultTableModel tableModel;

    public ExamPaperTab(int userId, String role, NetworkManager networkManager) {
        this.userId = userId;
        this.role = role;
        this.networkManager = networkManager;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(240, 240, 245));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Quản Lý Đề Thi");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Tên Đề Thi", "Trạng thái", "Tổng điểm"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        paperTable = new JTable(tableModel);
        paperTable.setRowHeight(30);
        paperTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        paperTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(paperTable);
        add(scrollPane, BorderLayout.CENTER);

        // Action Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(Color.WHITE);

        JButton reloadBtn = new JButton("Làm mới");
        reloadBtn.addActionListener(e -> reloadPapers());

        if ("TUTOR".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            JButton createBtn = new JButton("Tạo đề thi mới");
            createBtn.addActionListener(e -> showCreatePaperDialog());
            actionPanel.add(createBtn);
            
            JButton viewBtn = new JButton("Xem câu hỏi");
            viewBtn.addActionListener(e -> viewQuestions());
            actionPanel.add(viewBtn);

            JButton addQuestionBtn = new JButton("Thêm câu hỏi");
            addQuestionBtn.addActionListener(e -> showAddQuestionDialog());
            actionPanel.add(addQuestionBtn);

            JButton previewBtn = new JButton("Xem trước đề");
            previewBtn.addActionListener(e -> previewPaper());
            actionPanel.add(previewBtn);
        }

        actionPanel.add(reloadBtn);
        add(actionPanel, BorderLayout.SOUTH);
        
        reloadPapers();
    }

    public void reloadPapers() {
        if (networkManager != null) {
            try {
                networkManager.sendPacket(new Packet("EXAM_PAPER_LIST", ""));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updatePaperList(Object data) {
        tableModel.setRowCount(0);
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            for (Object obj : list) {
                if (obj instanceof LinkedTreeMap) {
                    LinkedTreeMap<?, ?> map = (LinkedTreeMap<?, ?>) obj;
                    Object idObj = map.get("id");
                    int id = idObj instanceof Number ? ((Number) idObj).intValue() : 0;
                    String title = (String) map.get("title");
                    String status = (String) map.get("status");
                    Object scoreObj = map.get("totalScore");
                    float totalScore = scoreObj instanceof Number ? ((Number) scoreObj).floatValue() : 0f;
                    
                    tableModel.addRow(new Object[]{id, title, status, totalScore});
                }
            }
        }
    }

    public void onPaperCreated() {
        JOptionPane.showMessageDialog(this, "Tạo đề thi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        reloadPapers();
    }

    private void showCreatePaperDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tạo Đề Thi Mới", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        panel.add(new JLabel("Tên đề thi (*):"));
        JTextField txtTitle = new JTextField();
        panel.add(txtTitle);

        panel.add(new JLabel("Mô tả:"));
        JTextArea txtDesc = new JTextArea();
        txtDesc.setLineWrap(true);
        panel.add(new JScrollPane(txtDesc));

        JButton btnSave = new JButton("Lưu");
        JButton btnCancel = new JButton("Hủy");

        btnSave.addActionListener(e -> {
            String title = txtTitle.getText().trim();
            String desc = txtDesc.getText().trim();
            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Tên đề thi không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("description", desc);
            
            try {
                networkManager.sendPacket(new Packet("EXAM_PAPER_CREATE", new Gson().toJson(payload)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            dialog.dispose();
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void viewQuestions() {
        int row = paperTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đề thi.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int paperId = (int) tableModel.getValueAt(row, 0);
        String paperTitle = (String) tableModel.getValueAt(row, 1);

        ExamPaperQuestionListDialog qDialog = new ExamPaperQuestionListDialog((Frame) SwingUtilities.getWindowAncestor(this), paperId, paperTitle, networkManager, this);
        qDialog.setVisible(true);
    }
    
    private void showAddQuestionDialog() {
        int row = paperTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đề thi để thêm câu hỏi.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int paperId = (int) tableModel.getValueAt(row, 0);
        String paperTitle = (String) tableModel.getValueAt(row, 1);
        String status = (String) tableModel.getValueAt(row, 2);
        
        if (!"DRAFT".equalsIgnoreCase(status) && !"Nháp".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Không thể thêm câu hỏi vào đề thi đã xuất bản hoặc đóng.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AddQuestionToPaperDialog dialog = new AddQuestionToPaperDialog((Frame) SwingUtilities.getWindowAncestor(this), paperId, paperTitle, networkManager);
        dialog.setVisible(true);
    }
    
    private void previewPaper() {
        int row = paperTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đề thi để xem trước.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int paperId = (int) tableModel.getValueAt(row, 0);

        try {
            Map<String, Object> req = new HashMap<>();
            req.put("paperId", paperId);
            networkManager.sendPacket(new Packet("EXAM_PACKAGE_PREVIEW_BY_PAPER", new Gson().toJson(req)));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi gửi yêu cầu xem trước.");
        }
    }
}
