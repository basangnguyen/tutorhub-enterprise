package com.mycompany.tutorhub_enterprise.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;

public class FacebookLoginDialog extends JDialog {

    public interface FacebookLoginListener {
        void onSuccess(String fbName, String fbEmail);
    }

    public FacebookLoginDialog(JFrame parent, FacebookLoginListener listener) {
        super(parent, "Facebook Login", true);
        setSize(400, 500);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Header Facebook
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
        header.setBackground(Color.decode("#1877F2")); // Màu xanh Facebook chuẩn
        JLabel lblFb = new JLabel("facebook");
        lblFb.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblFb.setForeground(Color.WHITE);
        header.add(lblFb);
        add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel lblTitle = new JLabel("TutorHub Enterprise");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDesc = new JLabel("<html><div style='text-align: center; color: #606770;'>đang yêu cầu quyền truy cập vào tên và địa chỉ email của bạn.</div></html>");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Mock Profile Info
        JPanel profile = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        profile.setOpaque(false);
        profile.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        JLabel lblAvatar = new JLabel();
        setNetworkIcon(lblAvatar, "https://img.icons8.com/color/96/circled-user-male-skin-type-4--v1.png", 64, 64);
        
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        
        // Dùng tên của bạn cho sinh động
        JLabel lblName = new JLabel("Nguyễn Bá Sáng");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JLabel lblEmail = new JLabel("basang.fb@gmail.com");
        lblEmail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblEmail.setForeground(Color.GRAY);
        info.add(lblName); info.add(lblEmail);
        
        profile.add(lblAvatar); profile.add(info);

        body.add(lblTitle);
        body.add(Box.createVerticalStrut(10));
        body.add(lblDesc);
        body.add(profile);

        add(body, BorderLayout.CENTER);

        // Footer Buttons
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(0, 30, 30, 30));

        JButton btnContinue = new JButton("Tiếp tục dưới tên Sáng");
        btnContinue.setBackground(Color.decode("#1877F2"));
        btnContinue.setForeground(Color.WHITE);
        btnContinue.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnContinue.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnContinue.putClientProperty("JComponent.arc", 10);
        btnContinue.setFocusPainted(false);
        btnContinue.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnCancel = new JButton("Hủy");
        btnCancel.setBackground(Color.decode("#E4E6EB"));
        btnCancel.setForeground(Color.decode("#4B4F56"));
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnCancel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnCancel.putClientProperty("JComponent.arc", 10);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        footer.add(btnContinue);
        footer.add(Box.createVerticalStrut(10));
        footer.add(btnCancel);

        add(footer, BorderLayout.SOUTH);

        // Events
        btnCancel.addActionListener(e -> dispose());
        btnContinue.addActionListener(e -> {
            dispose();
            listener.onSuccess("Nguyễn Bá Sáng", "basang.fb@gmail.com"); // Trả dữ liệu ảo về cho LoginFrame
        });
    }

    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) {
        new Thread(() -> { 
            try { 
                ImageIcon raw = new ImageIcon(new URL(urlStr)); 
                Image img = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); 
                SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(img))); 
            } catch (Exception e) {} 
        }).start();
    }
}