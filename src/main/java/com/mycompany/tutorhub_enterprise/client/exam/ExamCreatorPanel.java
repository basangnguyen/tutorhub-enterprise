package com.mycompany.tutorhub_enterprise.client.exam;

import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;

public class ExamCreatorPanel extends JPanel {
    private JTextField titleField;
    private JTextField durationField;
    private NetworkManager networkManager;
    private int currentUserId;
    private JDialog parentDialog;

    public ExamCreatorPanel(int userId, NetworkManager networkManager, JDialog parentDialog) {
        this.currentUserId = userId;
        this.networkManager = networkManager;
        this.parentDialog = parentDialog;
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        
        formPanel.add(new JLabel("Tên Kỳ Thi:"));
        titleField = new JTextField();
        formPanel.add(titleField);
        
        formPanel.add(new JLabel("Thời gian (phút):"));
        durationField = new JTextField();
        formPanel.add(durationField);
        
        add(formPanel, BorderLayout.NORTH);
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Hủy");
        cancelBtn.addActionListener(e -> parentDialog.dispose());
        
        JButton createBtn = new JButton("Tạo Kỳ Thi");
        createBtn.addActionListener(e -> submitExam());
        
        actionPanel.add(cancelBtn);
        actionPanel.add(createBtn);
        
        add(actionPanel, BorderLayout.SOUTH);
    }
    
    private void submitExam() {
        String title = titleField.getText().trim();
        String durationStr = durationField.getText().trim();
        
        if (title.isEmpty() || durationStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        
        try {
            int duration = Integer.parseInt(durationStr);
            
            Map<String, Object> examData = new HashMap<>();
            examData.put("title", title);
            examData.put("durationMins", duration);
            
            String payload = new Gson().toJson(examData);
            networkManager.sendPacket(new Packet("CREATE_EXAM", payload));
            
            JOptionPane.showMessageDialog(this, "Yêu cầu tạo kỳ thi đã được gửi.");
            parentDialog.dispose();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Thời gian phải là số nguyên!");
        }
    }
}
