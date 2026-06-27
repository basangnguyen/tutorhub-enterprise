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
import java.util.ArrayList;

public class QuestionListDialog extends JDialog {

    private int bankId;
    private NetworkManager networkManager;
    private JTable questionTable;
    private DefaultTableModel tableModel;
    private QuestionBankTab parentTab;

    public QuestionListDialog(Frame parent, int bankId, String bankTitle, NetworkManager networkManager, QuestionBankTab parentTab) {
        super(parent, "Ngân hàng: " + bankTitle, false);
        this.bankId = bankId;
        this.networkManager = networkManager;
        this.parentTab = parentTab;

        setSize(700, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        String[] columns = {"ID", "Loại", "Nội dung", "Điểm", "Độ khó"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        questionTable = new JTable(tableModel);
        questionTable.setRowHeight(25);

        add(new JScrollPane(questionTable), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addBtn = new JButton("Thêm câu hỏi");
        addBtn.addActionListener(e -> showCreateQuestionDialog());
        
        JButton reloadBtn = new JButton("Tải lại");
        reloadBtn.addActionListener(e -> loadQuestions());
        
        JButton closeBtn = new JButton("Đóng");
        closeBtn.addActionListener(e -> dispose());

        actionPanel.add(addBtn);
        actionPanel.add(reloadBtn);
        actionPanel.add(closeBtn);
        add(actionPanel, BorderLayout.SOUTH);

        // Đăng ký tham chiếu tạm thời để nhận callback từ MainDashboard
        // (Trong kiến trúc tốt hơn nên dùng EventBus, nhưng ở đây ta dùng 1 static reference hoặc pass data)
        QuestionListDialog.currentActiveDialog = this;

        loadQuestions();
    }

    public static QuestionListDialog currentActiveDialog = null;

    public void loadQuestions() {
        Map<String, Object> req = new HashMap<>();
        req.put("bankId", bankId);
        try {
            networkManager.sendPacket(new Packet("QUESTION_LIST_BY_BANK", new Gson().toJson(req)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void updateList(Object data) {
        tableModel.setRowCount(0);
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            for (Object obj : list) {
                if (obj instanceof LinkedTreeMap) {
                    LinkedTreeMap<?, ?> map = (LinkedTreeMap<?, ?>) obj;
                    Object idObj = map.get("id");
                    int id = idObj instanceof Number ? ((Number) idObj).intValue() : 0;
                    String type = (String) map.get("type");
                    String content = (String) map.get("content");
                    if (content != null && content.length() > 50) {
                        content = content.substring(0, 50) + "...";
                    }
                    Object scoreObj = map.get("score");
                    double score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0;
                    String difficulty = (String) map.get("difficulty");
                    
                    tableModel.addRow(new Object[]{id, type, content, score, difficulty});
                }
            }
        }
    }

    public void onQuestionCreated() {
        JOptionPane.showMessageDialog(this, "Thêm câu hỏi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        loadQuestions();
    }

    private void showCreateQuestionDialog() {
        JDialog dialog = new JDialog(this, "Tạo Câu Hỏi Mới", true);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Loại câu hỏi
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typePanel.add(new JLabel("Loại:"));
        JComboBox<String> cbType = new JComboBox<>(new String[]{"SINGLE_CHOICE", "TRUE_FALSE"});
        typePanel.add(cbType);
        panel.add(typePanel);

        // Nội dung
        panel.add(new JLabel("Nội dung:"));
        JTextArea txtContent = new JTextArea(4, 40);
        txtContent.setLineWrap(true);
        panel.add(new JScrollPane(txtContent));

        // Điểm & Độ khó
        JPanel propPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        propPanel.add(new JLabel("Điểm:"));
        JTextField txtScore = new JTextField("1.0", 5);
        propPanel.add(txtScore);
        propPanel.add(new JLabel("Độ khó:"));
        JComboBox<String> cbDiff = new JComboBox<>(new String[]{"EASY", "MEDIUM", "HARD"});
        propPanel.add(cbDiff);
        panel.add(propPanel);

        // Options Panel
        JPanel optionsWrapper = new JPanel(new BorderLayout());
        optionsWrapper.setBorder(BorderFactory.createTitledBorder("Đáp án"));
        
        JPanel singleChoicePanel = new JPanel(new GridLayout(4, 1, 5, 5));
        JTextField[] scTexts = new JTextField[4];
        JRadioButton[] scRadios = new JRadioButton[4];
        ButtonGroup scGroup = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            JPanel p = new JPanel(new BorderLayout(5, 0));
            scRadios[i] = new JRadioButton((char)('A' + i) + ".");
            scGroup.add(scRadios[i]);
            scTexts[i] = new JTextField();
            p.add(scRadios[i], BorderLayout.WEST);
            p.add(scTexts[i], BorderLayout.CENTER);
            singleChoicePanel.add(p);
        }
        
        JPanel trueFalsePanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JRadioButton[] tfRadios = new JRadioButton[2];
        ButtonGroup tfGroup = new ButtonGroup();
        for (int i = 0; i < 2; i++) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
            tfRadios[i] = new JRadioButton(i == 0 ? "Đúng (True)" : "Sai (False)");
            tfGroup.add(tfRadios[i]);
            p.add(tfRadios[i]);
            trueFalsePanel.add(p);
        }
        
        optionsWrapper.add(singleChoicePanel, BorderLayout.CENTER);
        panel.add(optionsWrapper);

        cbType.addActionListener(e -> {
            optionsWrapper.removeAll();
            if ("SINGLE_CHOICE".equals(cbType.getSelectedItem())) {
                optionsWrapper.add(singleChoicePanel, BorderLayout.CENTER);
            } else {
                optionsWrapper.add(trueFalsePanel, BorderLayout.CENTER);
            }
            optionsWrapper.revalidate();
            optionsWrapper.repaint();
        });

        // Giải thích
        panel.add(new JLabel("Giải thích (không bắt buộc):"));
        JTextArea txtExplanation = new JTextArea(2, 40);
        panel.add(new JScrollPane(txtExplanation));

        // Footer
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Lưu");
        JButton btnCancel = new JButton("Hủy");
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        
        btnCancel.addActionListener(e -> dialog.dispose());
        
        btnSave.addActionListener(e -> {
            String type = (String) cbType.getSelectedItem();
            String content = txtContent.getText().trim();
            if (content.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Nội dung câu hỏi không được rỗng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            double score;
            try {
                score = Double.parseDouble(txtScore.getText().trim());
                if (score <= 0) throw new Exception();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Điểm phải là số lớn hơn 0.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            List<Map<String, Object>> optionsList = new ArrayList<>();
            if ("SINGLE_CHOICE".equals(type)) {
                int selectedIdx = -1;
                for (int i = 0; i < 4; i++) {
                    if (scRadios[i].isSelected()) selectedIdx = i;
                }
                if (selectedIdx == -1) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng chọn 1 đáp án đúng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                int validCount = 0;
                for (int i = 0; i < 4; i++) {
                    String optText = scTexts[i].getText().trim();
                    if (!optText.isEmpty()) validCount++;
                    Map<String, Object> opt = new HashMap<>();
                    opt.put("optionLabel", String.valueOf((char)('A' + i)));
                    opt.put("content", optText);
                    opt.put("isCorrect", i == selectedIdx);
                    // Server rule: don't care about empty ones if validCount >= 2? We'll just send all 4, server handles it, 
                    // but we validate at least 2 non-empty
                    optionsList.add(opt);
                }
                if (validCount < 2) {
                    JOptionPane.showMessageDialog(dialog, "Cần ít nhất 2 đáp án không rỗng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                int selectedIdx = -1;
                for (int i = 0; i < 2; i++) {
                    if (tfRadios[i].isSelected()) selectedIdx = i;
                }
                if (selectedIdx == -1) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng chọn Đúng hoặc Sai là đáp án.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Map<String, Object> optTrue = new HashMap<>();
                optTrue.put("optionLabel", "A");
                optTrue.put("content", "True");
                optTrue.put("isCorrect", 0 == selectedIdx);
                optionsList.add(optTrue);
                
                Map<String, Object> optFalse = new HashMap<>();
                optFalse.put("optionLabel", "B");
                optFalse.put("content", "False");
                optFalse.put("isCorrect", 1 == selectedIdx);
                optionsList.add(optFalse);
            }
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("bankId", bankId);
            payload.put("type", type);
            payload.put("content", content);
            payload.put("score", score);
            payload.put("difficulty", cbDiff.getSelectedItem());
            payload.put("explanation", txtExplanation.getText().trim());
            payload.put("options", optionsList);
            
            // LƯU Ý: Tuyệt đối không System.out.println(payload) để tránh lộ answer key ra console theo quy định bảo mật.
            try {
                networkManager.sendPacket(new Packet("QUESTION_CREATE", new Gson().toJson(payload)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            dialog.dispose();
        });

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    @Override
    public void dispose() {
        if (currentActiveDialog == this) {
            currentActiveDialog = null;
        }
        super.dispose();
    }
}
