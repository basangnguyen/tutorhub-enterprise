package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.models.Packet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignPaperToExamDialog extends JDialog {

    public static AssignPaperToExamDialog currentActiveDialog = null;

    private int examId;
    private String examTitle;
    private NetworkManager networkManager;

    private JLabel lblCurrentPaper;
    private JComboBox<PaperItem> cbPapers;

    public AssignPaperToExamDialog(Frame owner, int examId, String examTitle, NetworkManager networkManager) {
        super(owner, "Gán Đề thi cho Kỳ thi - " + examTitle, true);
        this.examId = examId;
        this.examTitle = examTitle;
        this.networkManager = networkManager;

        currentActiveDialog = this;

        setSize(500, 300);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        mainPanel.add(new JLabel("Kỳ thi: " + examTitle));

        lblCurrentPaper = new JLabel("Đề thi đang gán: Đang tải...");
        lblCurrentPaper.setForeground(Color.BLUE);
        mainPanel.add(lblCurrentPaper);

        JPanel selectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectPanel.add(new JLabel("Chọn đề thi: "));
        cbPapers = new JComboBox<>();
        cbPapers.setPreferredSize(new Dimension(300, 30));
        selectPanel.add(cbPapers);
        mainPanel.add(selectPanel);

        add(mainPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAssign = new JButton("Gán đề thi");
        JButton btnUnassign = new JButton("Hủy gán");
        JButton btnClose = new JButton("Đóng");

        btnAssign.addActionListener(e -> assignPaper());
        btnUnassign.addActionListener(e -> unassignPaper());
        btnClose.addActionListener(e -> dispose());

        actionPanel.add(btnAssign);
        actionPanel.add(btnUnassign);
        actionPanel.add(btnClose);
        add(actionPanel, BorderLayout.SOUTH);

        loadCurrentAssignedPaper();
        loadAllPapers();
    }

    private void loadCurrentAssignedPaper() {
        if (networkManager != null) {
            try {
                networkManager.sendPacket(new Packet("EXAM_GET_ASSIGNED_PAPER", String.valueOf(examId)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateCurrentAssignedPaper(Object data) {
        if (data instanceof LinkedTreeMap) {
            LinkedTreeMap<?, ?> map = (LinkedTreeMap<?, ?>) data;
            Object idObj = map.get("id");
            String title = (String) map.get("title");
            if (idObj != null && title != null) {
                lblCurrentPaper.setText("Đề thi đang gán: " + title + " (ID: " + ((Number)idObj).intValue() + ")");
                return;
            }
        }
        lblCurrentPaper.setText("Đề thi đang gán: (Chưa có)");
    }

    private void loadAllPapers() {
        if (networkManager != null) {
            try {
                networkManager.sendPacket(new Packet("EXAM_PAPER_LIST", "{}"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updatePaperList(Object data) {
        cbPapers.removeAllItems();
        if (data instanceof List) {
            List<?> list = (List<?>) data;
            for (Object obj : list) {
                if (obj instanceof LinkedTreeMap) {
                    LinkedTreeMap<?, ?> map = (LinkedTreeMap<?, ?>) obj;
                    Object idObj = map.get("id");
                    int id = idObj instanceof Number ? ((Number) idObj).intValue() : 0;
                    String title = (String) map.get("title");
                    String status = (String) map.get("status");
                    
                    // Chỉ hiển thị đề thi chưa bị ARCHIVED
                    if (!"ARCHIVED".equalsIgnoreCase(status)) {
                        cbPapers.addItem(new PaperItem(id, title + " (" + status + ")"));
                    }
                }
            }
        }
    }

    private void assignPaper() {
        PaperItem selected = (PaperItem) cbPapers.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đề thi.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Map<String, Object> req = new HashMap<>();
            req.put("examId", examId);
            req.put("paperId", selected.id);
            networkManager.sendPacket(new Packet("EXAM_ASSIGN_PAPER", new Gson().toJson(req)));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void unassignPaper() {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn hủy gán đề thi khỏi kỳ thi này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                networkManager.sendPacket(new Packet("EXAM_UNASSIGN_PAPER", String.valueOf(examId)));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void onAssigned() {
        JOptionPane.showMessageDialog(this, "Thao tác gán đề thi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        loadCurrentAssignedPaper();
    }

    @Override
    public void dispose() {
        currentActiveDialog = null;
        super.dispose();
    }

    private static class PaperItem {
        int id;
        String title;
        public PaperItem(int id, String title) {
            this.id = id;
            this.title = title;
        }
        @Override
        public String toString() {
            return title;
        }
    }
}
