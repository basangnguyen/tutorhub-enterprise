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

public class QuestionBankTab extends JPanel {

    private int userId;
    private String role;
    private NetworkManager networkManager;
    private JTable bankTable;
    private DefaultTableModel tableModel;

    public QuestionBankTab(int userId, String role, NetworkManager networkManager) {
        this.userId = userId;
        this.role = role;
        this.networkManager = networkManager;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(240, 240, 245));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Ngân Hàng Câu Hỏi");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Tên Ngân Hàng", "Mô tả", "Người tạo"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bankTable = new JTable(tableModel);
        bankTable.setRowHeight(30);
        bankTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        bankTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(bankTable);
        add(scrollPane, BorderLayout.CENTER);

        // Action Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(Color.WHITE);

        JButton reloadBtn = new JButton("Tải lại");
        reloadBtn.addActionListener(e -> reloadBanks());

        if ("TUTOR".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            JButton createBankBtn = new JButton("Tạo ngân hàng");
            createBankBtn.addActionListener(e -> showCreateBankDialog());
            actionPanel.add(createBankBtn);
            
            JButton importHtmlBtn = new JButton("Import đề từ HTML");
            importHtmlBtn.addActionListener(e -> showImportHtmlDialog());
            actionPanel.add(importHtmlBtn);
            
            JButton viewQuestionsBtn = new JButton("Xem câu hỏi");
            viewQuestionsBtn.addActionListener(e -> viewSelectedBankQuestions());
            actionPanel.add(viewQuestionsBtn);
        }

        actionPanel.add(reloadBtn);
        add(actionPanel, BorderLayout.SOUTH);
    }

    public void reloadBanks() {
        if (networkManager != null) {
            try {
                networkManager.sendPacket(new Packet("QUESTION_BANK_LIST", "{}"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateBankList(Object data) {
        tableModel.setRowCount(0);
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            for (Object obj : list) {
                if (obj instanceof LinkedTreeMap) {
                    LinkedTreeMap<?, ?> map = (LinkedTreeMap<?, ?>) obj;
                    Object idObj = map.get("id");
                    int id = idObj instanceof Number ? ((Number) idObj).intValue() : 0;
                    String title = (String) map.get("name");
                    String desc = (String) map.get("description");
                    Object cIdObj = map.get("creatorId");
                    int cId = cIdObj instanceof Number ? ((Number) cIdObj).intValue() : 0;
                    
                    tableModel.addRow(new Object[]{id, title != null ? title : "Bank " + id, desc, cId});
                }
            }
        }
    }

    public void onBankCreated() {
        JOptionPane.showMessageDialog(this, "Tạo ngân hàng câu hỏi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        reloadBanks();
    }

    private void showCreateBankDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tạo Ngân Hàng Câu Hỏi", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        panel.add(new JLabel("Tên ngân hàng (*):"));
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
                JOptionPane.showMessageDialog(dialog, "Tên ngân hàng không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("description", desc);
            payload.put("creatorId", userId);
            
            try {
                networkManager.sendPacket(new Packet("QUESTION_BANK_CREATE", new Gson().toJson(payload)));
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

    private void showImportHtmlDialog() {
        HtmlQuizImportDialog dialog = new HtmlQuizImportDialog((Frame) SwingUtilities.getWindowAncestor(this), networkManager, this);
        dialog.setVisible(true);
    }

    private void viewSelectedBankQuestions() {
        int row = bankTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một ngân hàng câu hỏi.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bankId = (int) tableModel.getValueAt(row, 0);
        String bankTitle = (String) tableModel.getValueAt(row, 1);

        QuestionListDialog qDialog = new QuestionListDialog((Frame) SwingUtilities.getWindowAncestor(this), bankId, bankTitle, networkManager, this);
        qDialog.setVisible(true);
    }
}
