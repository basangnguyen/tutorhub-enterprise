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

public class ExamPaperQuestionListDialog extends JDialog {

    public static ExamPaperQuestionListDialog currentActiveDialog = null;

    private int paperId;
    private String paperTitle;
    private NetworkManager networkManager;
    private ExamPaperTab parentTab;

    private JTable questionTable;
    private DefaultTableModel tableModel;

    public ExamPaperQuestionListDialog(Frame owner, int paperId, String paperTitle, NetworkManager networkManager, ExamPaperTab parentTab) {
        super(owner, "Danh sách câu hỏi - " + paperTitle, true);
        this.paperId = paperId;
        this.paperTitle = paperTitle;
        this.networkManager = networkManager;
        this.parentTab = parentTab;

        currentActiveDialog = this;

        setSize(800, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel titleLabel = new JLabel("Đề thi: " + paperTitle);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        String[] columns = {"ID", "Nội dung (Preview)", "Loại", "Điểm", "Thứ tự"};
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
        JButton btnRemove = new JButton("Gỡ câu hỏi khỏi đề");
        JButton btnClose = new JButton("Đóng");

        btnRemove.addActionListener(e -> removeSelectedQuestion());
        btnClose.addActionListener(e -> dispose());

        actionPanel.add(btnRemove);
        actionPanel.add(btnClose);
        add(actionPanel, BorderLayout.SOUTH);

        loadQuestions();
    }

    private void loadQuestions() {
        if (networkManager != null) {
            try {
                networkManager.sendPacket(new Packet("EXAM_PAPER_LIST_QUESTIONS", String.valueOf(paperId)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateList(Object data) {
        tableModel.setRowCount(0);
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            for (Object obj : list) {
                if (obj instanceof LinkedTreeMap) {
                    LinkedTreeMap<?, ?> map = (LinkedTreeMap<?, ?>) obj;
                    Object qObj = map.get("question");
                    if (qObj instanceof LinkedTreeMap) {
                        LinkedTreeMap<?, ?> questionObj = (LinkedTreeMap<?, ?>) qObj;
                        
                        Object idObj = questionObj.get("id");
                        int id = idObj instanceof Number ? ((Number) idObj).intValue() : 0;
                        
                        String contentRaw = (String) questionObj.get("contentRaw");
                        if (contentRaw == null) contentRaw = "";
                        String preview = contentRaw.length() > 50 ? contentRaw.substring(0, 50) + "..." : contentRaw;
                        
                        String type = (String) questionObj.get("type");
                        
                        Object scoreObj = map.get("score");
                        float score = scoreObj instanceof Number ? ((Number) scoreObj).floatValue() : 0f;
                        
                        Object orderObj = map.get("orderIndex");
                        int orderIndex = orderObj instanceof Number ? ((Number) orderObj).intValue() : 0;

                        tableModel.addRow(new Object[]{id, preview, type, score, orderIndex});
                    }
                }
            }
        }
    }

    private void removeSelectedQuestion() {
        int row = questionTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một câu hỏi để gỡ.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int questionId = (int) tableModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn gỡ câu hỏi này khỏi đề thi?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Map<String, Object> req = new HashMap<>();
                req.put("paperId", paperId);
                req.put("questionId", questionId);
                networkManager.sendPacket(new Packet("EXAM_PAPER_REMOVE_QUESTION", new Gson().toJson(req)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void onQuestionRemoved() {
        JOptionPane.showMessageDialog(this, "Gỡ câu hỏi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        loadQuestions();
        if (parentTab != null) {
            parentTab.reloadPapers();
        }
    }

    @Override
    public void dispose() {
        currentActiveDialog = null;
        super.dispose();
    }
}
