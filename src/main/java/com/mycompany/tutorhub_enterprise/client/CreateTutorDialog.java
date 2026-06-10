package com.mycompany.tutorhub_enterprise.client;

import com.mycompany.tutorhub_enterprise.models.Packet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class CreateTutorDialog extends JDialog {

    private final Color PRIMARY_BLUE = Color.decode("#2563EB");
    private final Color TEXT_DARK = Color.decode("#111827");
    private final Color TEXT_MUTED = Color.decode("#6B7280");
    private final Color BORDER_COLOR = Color.decode("#E5E7EB");
    private final Color BG_WHITE = Color.WHITE;

    private JTextField txtFullName, txtEmail, txtPhone, txtSubject, txtLocation;
    private JPasswordField txtPassword;
    private Runnable onSuccessCallback;

    public CreateTutorDialog(Frame owner, Runnable onSuccessCallback) {
        super(owner, "", true);
        this.onSuccessCallback = onSuccessCallback;
        setUndecorated(true);
        setSize(800, 750); 
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        setBackground(new Color(0, 0, 0, 0));

        // Bo góc cho JDialog
        JPanel mainWrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        mainWrapper.setOpaque(false);
        add(mainWrapper, BorderLayout.CENTER);

        // ==========================================
        // HEADER
        // ==========================================
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(25, 40, 20, 40));

        JPanel titleWrap = new JPanel();
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setOpaque(false);
        
        JLabel lblTitle = new JLabel("Thêm Gia sư mới");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(TEXT_DARK);
        
        JLabel lblSub = new JLabel("Khởi tạo tài khoản và hồ sơ cho gia sư gia nhập trung tâm.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblSub.setForeground(TEXT_MUTED);
        
        titleWrap.add(lblTitle);
        titleWrap.add(Box.createVerticalStrut(5));
        titleWrap.add(lblSub);
        header.add(titleWrap, BorderLayout.WEST);

        JLabel btnClose = new JLabel("✕");
        btnClose.setFont(new Font("Arial", Font.PLAIN, 24));
        btnClose.setForeground(Color.decode("#9CA3AF"));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dispose(); }
            @Override public void mouseEntered(MouseEvent e) { btnClose.setForeground(Color.RED); }
            @Override public void mouseExited(MouseEvent e) { btnClose.setForeground(Color.decode("#9CA3AF")); }
        });
        header.add(btnClose, BorderLayout.EAST);
        
        mainWrapper.add(header, BorderLayout.NORTH);

        // ==========================================
        // FORM NHẬP LIỆU
        // ==========================================
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);
        formContainer.setBorder(new EmptyBorder(10, 40, 20, 40));

        // Layout 2 cột cho các trường thông tin
        JPanel row1 = new JPanel(new GridLayout(1, 2, 30, 0)); row1.setOpaque(false);
        txtFullName = createInputBox("Họ và tên *", "Ví dụ: Nguyễn Văn A", "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/user.png");
        txtPhone = createInputBox("Số điện thoại", "Ví dụ: 0987654321", "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/phone.png");
        row1.add(createFormRow(txtFullName)); row1.add(createFormRow(txtPhone));
        formContainer.add(row1); formContainer.add(Box.createVerticalStrut(20));

        JPanel row2 = new JPanel(new GridLayout(1, 2, 30, 0)); row2.setOpaque(false);
        txtEmail = createInputBox("Email đăng nhập *", "Ví dụ: gia_su_a@gmail.com", "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/mail.png");
        txtPassword = createPasswordBox("Mật khẩu khởi tạo *", "Nhập mật khẩu mặc định");
        row2.add(createFormRow(txtEmail)); row2.add(createFormRow(txtPassword));
        formContainer.add(row2); formContainer.add(Box.createVerticalStrut(20));

        txtSubject = createInputBox("Chuyên môn giảng dạy *", "Toán học, Tiếng Anh IELTS...", "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/open-book.png");
        formContainer.add(createFormRow(txtSubject)); formContainer.add(Box.createVerticalStrut(20));

        txtLocation = createInputBox("Khu vực / Tỉnh thành", "Hà Nội, TP.HCM...", "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/marker.png");
        formContainer.add(createFormRow(txtLocation)); formContainer.add(Box.createVerticalStrut(20));

        mainWrapper.add(new JScrollPane(formContainer) {{ setBorder(null); setOpaque(false); getViewport().setOpaque(false); }}, BorderLayout.CENTER);

        // ==========================================
        // FOOTER (NÚT LƯU)
        // ==========================================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 20));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        JButton btnCancel = createCustomButton("Hủy bỏ", false);
        btnCancel.addActionListener(e -> dispose());
        
        JButton btnSubmit = createCustomButton("Thêm Gia sư", true);
        btnSubmit.addActionListener(e -> executeAddTutor(btnSubmit));

        footer.add(btnCancel);
        footer.add(btnSubmit);
        mainWrapper.add(footer, BorderLayout.SOUTH);
    }

    private void executeAddTutor(JButton btnSubmit) {
        String name = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();
        String phone = txtPhone.getText().trim();
        String subj = txtSubject.getText().trim();
        String loc = txtLocation.getText().trim();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || subj.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ các trường bắt buộc (*)", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnSubmit.setText("Đang xử lý...");
        btnSubmit.setEnabled(false);

        new Thread(() -> {
            try {
                NetworkManager net = NetworkManager.getInstance();
                if (!net.isConnected()) net.connect("localhost", 8888);
                
                // Gửi lệnh tạo trực tiếp từ Admin (Bỏ qua OTP)
                // Payload: email | password | name | phone | subject | location
                String payload = email + "|" + pass + "|" + name + "|" + phone + "|" + subj + "|" + loc;
                net.sendPacket(new Packet("ADD_TUTOR_BY_ADMIN", payload));
                
                Packet res = net.receivePacket();
                
                SwingUtilities.invokeLater(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Thêm Gia sư");
                    if (res.success) {
                        JOptionPane.showMessageDialog(this, "🎉 Đã tạo tài khoản Gia sư thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                        if (onSuccessCallback != null) onSuccessCallback.run();
                    } else {
                        JOptionPane.showMessageDialog(this, res.message, "Lỗi tạo tài khoản", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Thêm Gia sư");
                    JOptionPane.showMessageDialog(this, "Mất kết nối máy chủ!", "Lỗi mạng", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    // --- CÁC HÀM TIỆN ÍCH VẼ UI ---
    private JPanel createFormRow(JComponent inputComp) {
        JPanel p = new JPanel(new BorderLayout()); p.setOpaque(false);
        p.add(inputComp, BorderLayout.CENTER);
        return p;
    }

    private JTextField createInputBox(String labelStr, String placeholder, String iconUrl) {
        JPanel wrapper = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12); g2.dispose();
            }
        };
        wrapper.setOpaque(false); wrapper.setBorder(new EmptyBorder(0, 15, 0, 15)); wrapper.setPreferredSize(new Dimension(0, 48));

        JTextField field = new JTextField(); field.setBorder(null); field.setOpaque(false);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15)); field.putClientProperty("JTextField.placeholderText", placeholder);
        
        JLabel lblIcon = new JLabel(); setNetworkIcon(lblIcon, iconUrl, 20, 20);
        
        wrapper.add(lblIcon, BorderLayout.WEST); wrapper.add(field, BorderLayout.CENTER);
        
        JPanel container = new JPanel(new BorderLayout()); container.setOpaque(false);
        JLabel lbl = new JLabel(labelStr); lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(TEXT_DARK); lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        container.add(lbl, BorderLayout.NORTH); container.add(wrapper, BorderLayout.CENTER);
        return field;
    }

    private JPasswordField createPasswordBox(String labelStr, String placeholder) {
        JPanel wrapper = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER_COLOR); g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12); g2.dispose();
            }
        };
        wrapper.setOpaque(false); wrapper.setBorder(new EmptyBorder(0, 15, 0, 15)); wrapper.setPreferredSize(new Dimension(0, 48));

        JPasswordField field = new JPasswordField(); field.setBorder(null); field.setOpaque(false);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15)); field.putClientProperty("JTextField.placeholderText", placeholder);
        
        JLabel lblIcon = new JLabel(); setNetworkIcon(lblIcon, "https://img.icons8.com/fluency-systems-regular/48/9CA3AF/lock.png", 20, 20);
        
        wrapper.add(lblIcon, BorderLayout.WEST); wrapper.add(field, BorderLayout.CENTER);
        
        JPanel container = new JPanel(new BorderLayout()); container.setOpaque(false);
        JLabel lbl = new JLabel(labelStr); lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(TEXT_DARK); lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        container.add(lbl, BorderLayout.NORTH); container.add(wrapper, BorderLayout.CENTER);
        return field;
    }

    private JButton createCustomButton(String text, boolean isPrimary) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isPrimary) { g2.setColor(PRIMARY_BLUE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); } 
                else { g2.setColor(Color.WHITE); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); g2.setColor(BORDER_COLOR); g2.setStroke(new BasicStroke(1.2f)); g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 12, 12); }
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", isPrimary ? Font.BOLD : Font.PLAIN, 15)); btn.setForeground(isPrimary ? Color.WHITE : TEXT_DARK); btn.setContentAreaFilled(false); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setPreferredSize(new Dimension(140, 44)); return btn;
    }

    private void setNetworkIcon(JLabel label, String urlStr, int width, int height) {
        new Thread(() -> { try { ImageIcon icon = new ImageIcon(new URL(urlStr)); Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH); SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(img))); } catch (Exception ignored) {} }).start();
    }
}