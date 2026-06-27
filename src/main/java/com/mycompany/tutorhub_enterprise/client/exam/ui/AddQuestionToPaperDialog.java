package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.models.Packet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddQuestionToPaperDialog extends JDialog {

    public static AddQuestionToPaperDialog currentActiveDialog = null;

    private int paperId;
    private String paperTitle;
    private NetworkManager networkManager;

    private JComboBox<BankItem> cbBanks;
    private JTable questionTable;
    private DefaultTableModel tableModel;

    public AddQuestionToPaperDialog(Frame owner, int paperId, String paperTitle, NetworkManager networkManager) {
        super(owner, "Thêm câu hỏi vào Đề thi - " + paperTitle, true);
        this.paperId = paperId;
        this.paperTitle = paperTitle;
        this.networkManager = networkManager;

        currentActiveDialog = this;

        setSize(800, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        topPanel.add(new JLabel("Chọn Ngân hàng câu hỏi: "));
        cbBanks = new JComboBox<>();
        cbBanks.setPreferredSize(new Dimension(300, 30));
        cbBanks.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                BankItem item = (BankItem) e.getItem();
                if (item.id != -1) {
                    loadQuestionsFromBank(item.id);
                } else {
                    tableModel.setRowCount(0);
                }
            }
        });
        topPanel.add(cbBanks);
        
        JButton btnReloadBanks = new JButton("Tải lại danh sách NH");
        btnReloadBanks.addActionListener(e -> loadBanks());
        topPanel.add(btnReloadBanks);
        
        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"ID", "Nội dung", "Loại", "Mức độ"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        questionTable = new JTable(tableModel);
        questionTable.setRowHeight(30);
        questionTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        questionTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(questionTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAdd = new JButton("Thêm vào đề");
        JButton btnClose = new JButton("Đóng");

        btnAdd.addActionListener(e -> addSelectedQuestion());
        btnClose.addActionListener(e -> dispose());

        actionPanel.add(btnAdd);
        actionPanel.add(btnClose);
        add(actionPanel, BorderLayout.SOUTH);

        loadBanks();
    }

    private void loadBanks() {
        if (networkManager != null) {
            // Note: Since QUESTION_BANK_LIST_SUCCESS is only handled by QuestionBankTab in MainDashboard, 
            // we will need to temporarily fetch it or add handling. 
            // Actually, we can just send the request, but wait, MainDashboard handles QUESTION_BANK_LIST_SUCCESS 
            // only for questionBankTab. Let's add handling in MainDashboard or just reuse a static list if possible.
            // Oh, we didn't add AddQuestionToPaperDialog handling for QUESTION_BANK_LIST_SUCCESS.
            // Let me use a workaround. I will send QUESTION_BANK_LIST.
            try {
                networkManager.sendPacket(new Packet("QUESTION_BANK_LIST", "{}"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // This will be called from MainDashboard
    public void updateBankList(Object data) {
        cbBanks.removeAllItems();
        cbBanks.addItem(new BankItem(-1, "-- Chọn Ngân hàng --"));
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            for (Object obj : list) {
                if (obj instanceof LinkedTreeMap) {
                    LinkedTreeMap<?, ?> map = (LinkedTreeMap<?, ?>) obj;
                    Object idObj = map.get("id");
                    int id = idObj instanceof Number ? ((Number) idObj).intValue() : 0;
                    String title = (String) map.get("name");
                    if (title == null || title.isEmpty()) {
                        title = "Bank ID: " + id;
                    }
                    cbBanks.addItem(new BankItem(id, title));
                }
            }
        }
    }

    private void loadQuestionsFromBank(int bankId) {
        if (networkManager != null) {
            try {
                networkManager.sendPacket(new Packet("QUESTION_LIST_BY_BANK", String.valueOf(bankId)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateQuestionList(Object data) {
        tableModel.setRowCount(0);
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            for (Object obj : list) {
                if (obj instanceof LinkedTreeMap) {
                    LinkedTreeMap<?, ?> map = (LinkedTreeMap<?, ?>) obj;
                    Object idObj = map.get("id");
                    int id = idObj instanceof Number ? ((Number) idObj).intValue() : 0;
                    
                    String contentRaw = (String) map.get("contentRaw");
                    if (contentRaw == null) contentRaw = "";
                    String preview = contentRaw.length() > 50 ? contentRaw.substring(0, 50) + "..." : contentRaw;
                    
                    String type = (String) map.get("type");
                    String diff = (String) map.get("difficulty");

                    tableModel.addRow(new Object[]{id, preview, type, diff});
                }
            }
        }
    }

    private void addSelectedQuestion() {
        int row = questionTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một câu hỏi để thêm.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int questionId = (int) tableModel.getValueAt(row, 0);

        String scoreStr = JOptionPane.showInputDialog(this, "Nhập điểm cho câu hỏi này (mặc định 1.0):", "1.0");
        if (scoreStr == null) return;
        
        float score = 1.0f;
        try {
            score = Float.parseFloat(scoreStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Điểm không hợp lệ, dùng 1.0", "Lỗi", JOptionPane.ERROR_MESSAGE);
            score = 1.0f;
        }

        try {
            Map<String, Object> req = new HashMap<>();
            req.put("paperId", paperId);
            req.put("questionId", questionId);
            req.put("score", score);
            req.put("orderIndex", 0);
            req.put("required", true);
            networkManager.sendPacket(new Packet("EXAM_PAPER_ADD_QUESTION", new Gson().toJson(req)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onQuestionAdded() {
        JOptionPane.showMessageDialog(this, "Thêm câu hỏi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        // Do not close so they can add more.
    }

    @Override
    public void dispose() {
        currentActiveDialog = null;
        super.dispose();
    }
    
    private static class BankItem {
        int id;
        String title;
        public BankItem(int id, String title) {
            this.id = id;
            this.title = title;
        }
        @Override
        public String toString() {
            return title;
        }
    }
}
