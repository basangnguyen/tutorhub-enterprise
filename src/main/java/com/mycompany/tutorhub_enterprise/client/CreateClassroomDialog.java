package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CreateClassroomDialog extends JDialog {
    private JTextField txtName;
    private JTextArea txtDesc;
    private MainDashboard dashboard;

    public CreateClassroomDialog(MainDashboard parent) {
        super(parent, "Tạo lớp học mới", true);
        this.dashboard = parent;
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);

        JLabel lblName = new JLabel("Tên lớp học:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtName = new JTextField();
        txtName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        JLabel lblDesc = new JLabel("Mô tả:");
        lblDesc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtDesc = new JTextArea(4, 20);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDesc);

        formPanel.add(lblName);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(txtName);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(lblDesc);
        formPanel.add(Box.createVerticalStrut(5));
        formPanel.add(scrollDesc);

        JButton btnCreate = new JButton("Tạo lớp");
        btnCreate.setBackground(new Color(59, 130, 246));
        btnCreate.setForeground(Color.WHITE);
        btnCreate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCreate.setFocusPainted(false);
        btnCreate.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCreate.addActionListener(e -> createClassroom());

        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(btnCreate, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void createClassroom() {
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên lớp học!");
            return;
        }
        
        String payload = name + "|" + desc + "|";
        try {
            NetworkManager.getInstance().sendPacket(new Packet("CREATE_CLASSROOM", payload));
        } catch (Exception e) {}
        
        dispose();
    }
}
