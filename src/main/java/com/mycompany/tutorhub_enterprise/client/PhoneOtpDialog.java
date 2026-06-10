package com.mycompany.tutorhub_enterprise.client;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public class PhoneOtpDialog extends JDialog {

    private final Color primary = Color.decode("#10B981");
    private final Color textMain = Color.decode("#111827");
    private final Color textMuted = Color.decode("#6B7280");
    private final Color error = Color.decode("#DC2626");
    private final Consumer<String> onVerify;
    private final Runnable onResend;

    private JTextField txtOtp;
    private JLabel lblStatus;
    private JButton btnVerify;
    private JButton btnResend;
    private Timer resendTimer;
    private int countdown = 60;

    public PhoneOtpDialog(Window owner, String phone, Consumer<String> onVerify, Runnable onResend) {
        super(owner, "Phone verification");
        this.onVerify = onVerify;
        this.onResend = onResend;

        setModalityType(ModalityType.MODELESS);
        setSize(430, 360);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(null);
        getContentPane().setBackground(Color.WHITE);

        JLabel title = new JLabel("Xác minh số điện thoại", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        title.setForeground(textMain);
        title.setBounds(0, 28, 430, 32);
        add(title);

        JLabel subtitle = new JLabel("Nhập mã 6 số đã gửi tới " + phone, SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(textMuted);
        subtitle.setBounds(20, 64, 390, 24);
        add(subtitle);

        txtOtp = new JTextField();
        txtOtp.setBounds(86, 116, 258, 48);
        txtOtp.setFont(new Font("Segoe UI", Font.BOLD, 20));
        txtOtp.setHorizontalAlignment(JTextField.CENTER);
        txtOtp.putClientProperty("JTextField.placeholderText", "123456");
        txtOtp.putClientProperty("JComponent.arc", 16);
        InputFilters.installOtpFilter(txtOtp);
        add(txtOtp);

        lblStatus = new JLabel("Mã OTP có hiệu lực trong thời gian ngắn.", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(textMuted);
        lblStatus.setBounds(42, 174, 346, 24);
        add(lblStatus);

        btnVerify = new JButton("Xác minh");
        btnVerify.setBounds(42, 222, 166, 44);
        btnVerify.setBackground(primary);
        btnVerify.setForeground(Color.WHITE);
        btnVerify.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnVerify.putClientProperty("JComponent.arc", 999);
        btnVerify.setFocusPainted(false);
        btnVerify.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btnVerify);

        btnResend = new JButton("Gửi lại mã");
        btnResend.setBounds(222, 222, 166, 44);
        btnResend.setBackground(Color.decode("#F3F4F6"));
        btnResend.setForeground(textMain);
        btnResend.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnResend.putClientProperty("JComponent.arc", 999);
        btnResend.setFocusPainted(false);
        btnResend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btnResend);

        JButton cancel = new JButton("Đóng");
        cancel.setBounds(165, 282, 100, 28);
        cancel.setContentAreaFilled(false);
        cancel.setBorderPainted(false);
        cancel.setForeground(textMuted);
        cancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(cancel);

        btnVerify.addActionListener(e -> submitOtp());
        btnResend.addActionListener(e -> resendOtp());
        cancel.addActionListener(e -> dispose());
    }

    public void markSent(String message) {
        txtOtp.setText("");
        txtOtp.requestFocus();
        setStatus(message == null || message.isBlank() ? "Mã OTP đã được gửi." : message, primary);
        startCooldown();
        if (!isVisible()) {
            setVisible(true);
        }
    }

    public void markSendFailed(String message) {
        setStatus(message == null || message.isBlank() ? "Không thể gửi OTP. Vui lòng thử lại." : message, error);
        stopCooldown("Gửi lại mã");
        btnVerify.setEnabled(true);
        if (!isVisible()) {
            setVisible(true);
        }
    }

    public void markVerifying() {
        btnVerify.setEnabled(false);
        btnVerify.setText("Đang xác minh...");
        setStatus("Đang kiểm tra mã OTP...", textMuted);
    }

    public void handleResult(boolean success, String message) {
        btnVerify.setEnabled(!success);
        btnVerify.setText("Xác minh");
        setStatus(message, success ? primary : error);
        if (success) {
            Timer closeTimer = new Timer(750, e -> dispose());
            closeTimer.setRepeats(false);
            closeTimer.start();
        }
    }

    private void submitOtp() {
        String otp = txtOtp.getText().trim();
        if (!InputFilters.isValidOtp(otp)) {
            setStatus("Mã OTP phải gồm đúng 6 chữ số.", error);
            txtOtp.requestFocus();
            return;
        }
        markVerifying();
        onVerify.accept(otp);
    }

    private void resendOtp() {
        setStatus("Đang gửi lại mã OTP...", textMuted);
        startCooldown();
        onResend.run();
    }

    private void startCooldown() {
        if (resendTimer != null) {
            resendTimer.stop();
        }
        btnResend.setEnabled(false);
        countdown = 60;
        btnResend.setText(countdown + "s");
        resendTimer = new Timer(1000, e -> {
            countdown--;
            btnResend.setText(countdown + "s");
            if (countdown <= 0) {
                stopCooldown("Gửi lại mã");
            }
        });
        resendTimer.start();
    }

    private void stopCooldown(String text) {
        if (resendTimer != null) {
            resendTimer.stop();
        }
        btnResend.setEnabled(true);
        btnResend.setText(text);
    }

    private void setStatus(String message, Color color) {
        lblStatus.setText(message == null || message.isBlank() ? " " : message);
        lblStatus.setForeground(color);
    }
}
