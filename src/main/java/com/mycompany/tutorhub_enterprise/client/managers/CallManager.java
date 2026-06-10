package com.mycompany.tutorhub_enterprise.client.managers;

import com.mycompany.tutorhub_enterprise.client.ChatTab;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.sound.sampled.*;

public class CallManager {
    private static CallManager instance;
    private ChatTab chatTab;
    
    private JDialog currentCallDialog;
    private String currentCallConversationId;
    private Clip ringtoneClip;
    private boolean isCallActive = false;

    private String activeRoomName;
    private String activeMyName;

    private CallManager() {}

    public static synchronized CallManager getInstance() {
        if (instance == null) {
            instance = new CallManager();
        }
        return instance;
    }

    public void init(ChatTab chatTab) {
        this.chatTab = chatTab;
    }

    private void playRingtone() {
        try {
            // Cố gắng phát file ringtone.wav nếu có, nếu không thì beep
            File f = new File(System.getProperty("user.dir"), "ringtone.wav");
            if (f.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(f);
                ringtoneClip = AudioSystem.getClip();
                ringtoneClip.open(audioIn);
                ringtoneClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                new Thread(() -> {
                    while (currentCallDialog != null && currentCallDialog.isVisible()) {
                        Toolkit.getDefaultToolkit().beep();
                        try { Thread.sleep(2000); } catch (Exception e) {}
                    }
                }).start();
            }
        } catch (Exception e) {}
    }

    private void stopRingtone() {
        if (ringtoneClip != null) {
            ringtoneClip.stop();
            ringtoneClip.close();
            ringtoneClip = null;
        }
    }

    private void closeDialog() {
        if (currentCallDialog != null) {
            currentCallDialog.dispose();
            currentCallDialog = null;
        }
        stopRingtone();
        currentCallConversationId = null;
    }

    // ==========================================
    // 1. GỌI ĐI
    // ==========================================
    public void initCall(String conversationId, String roomName, String myName, String partnerName, boolean isVideo) {
        if (isCallActive) {
            JOptionPane.showMessageDialog(chatTab, "Bạn đang trong một cuộc gọi khác!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        this.currentCallConversationId = conversationId;
        this.activeRoomName = roomName;
        this.activeMyName = myName;

        currentCallDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(chatTab), "Đang gọi...", false);
        currentCallDialog.setSize(300, 400);
        currentCallDialog.setLocationRelativeTo(chatTab);
        currentCallDialog.setLayout(new BorderLayout());
        currentCallDialog.getContentPane().setBackground(Color.decode("#1E293B"));

        JLabel lblName = new JLabel("Đang gọi " + partnerName + "...", SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblName.setForeground(Color.WHITE);
        currentCallDialog.add(lblName, BorderLayout.CENTER);

        JButton btnCancel = new JButton("Kết thúc");
        btnCancel.setBackground(Color.decode("#EF4444"));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> {
            try {
                NetworkManager.getInstance().sendPacket(new Packet("CALL_CANCEL", conversationId));
            } catch (Exception ex) {}
            closeDialog();
        });
        
        currentCallDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                btnCancel.doClick();
            }
        });

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(btnCancel);
        currentCallDialog.add(bottom, BorderLayout.SOUTH);

        try {
            NetworkManager.getInstance().sendPacket(new Packet("CALL_INIT", conversationId + "|" + (isVideo ? "VIDEO" : "AUDIO")));
            playRingtone();
            currentCallDialog.setVisible(true);
        } catch (Exception e) {
            closeDialog();
            JOptionPane.showMessageDialog(chatTab, "Lỗi mạng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // 2. NHẬN CUỘC GỌI TỚI
    // ==========================================
    public void handleIncomingCall(String conversationId, String senderName, String type) {
        if (isCallActive || currentCallDialog != null) {
            // Đang bận, tự động reject
            try {
                NetworkManager.getInstance().sendPacket(new Packet("CALL_REJECT", conversationId));
            } catch (Exception e) {}
            return;
        }

        this.currentCallConversationId = conversationId;
        
        currentCallDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(chatTab), "Cuộc gọi đến", false);
        currentCallDialog.setSize(300, 400);
        currentCallDialog.setLocationRelativeTo(chatTab);
        currentCallDialog.setLayout(new BorderLayout());
        currentCallDialog.getContentPane().setBackground(Color.decode("#1E293B"));

        JLabel lblName = new JLabel("<html><center>" + senderName + "<br>đang gọi " + (type.equals("VIDEO") ? "Video" : "Thoại") + "</center></html>", SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblName.setForeground(Color.WHITE);
        currentCallDialog.add(lblName, BorderLayout.CENTER);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        pnlBtns.setOpaque(false);

        JButton btnAccept = new JButton("Nghe");
        btnAccept.setBackground(Color.decode("#22C55E"));
        btnAccept.setForeground(Color.WHITE);
        btnAccept.addActionListener(e -> {
            try {
                NetworkManager.getInstance().sendPacket(new Packet("CALL_ACCEPT", conversationId));
                closeDialog();
                launchLiveKit(conversationId, type.equals("VIDEO"), false);
            } catch (Exception ex) {}
        });

        JButton btnReject = new JButton("Từ chối");
        btnReject.setBackground(Color.decode("#EF4444"));
        btnReject.setForeground(Color.WHITE);
        btnReject.addActionListener(e -> {
            try {
                NetworkManager.getInstance().sendPacket(new Packet("CALL_REJECT", conversationId));
                closeDialog();
            } catch (Exception ex) {}
        });

        pnlBtns.add(btnAccept);
        pnlBtns.add(btnReject);
        currentCallDialog.add(pnlBtns, BorderLayout.SOUTH);

        playRingtone();
        currentCallDialog.setVisible(true);
    }

    // ==========================================
    // 3. XỬ LÝ SỰ KIỆN TỪ ĐỐI TÁC
    // ==========================================
    public void handleCallAccept(String conversationId) {
        if (currentCallDialog != null && conversationId.equals(currentCallConversationId)) {
            // Xác định xem mình đang gọi Video hay Audio
            boolean isVideo = ((JLabel)currentCallDialog.getContentPane().getComponent(0)).getText().contains("video") || 
                              ((JLabel)currentCallDialog.getContentPane().getComponent(0)).getText().contains("Video");
            closeDialog();
            launchLiveKit(conversationId, isVideo, true);
        }
    }

    public void handleCallReject(String conversationId) {
        if (currentCallDialog != null && conversationId.equals(currentCallConversationId)) {
            closeDialog();
            JOptionPane.showMessageDialog(chatTab, "Người dùng đang bận hoặc đã từ chối cuộc gọi.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void handleCallCancel(String conversationId) {
        if (currentCallDialog != null && conversationId.equals(currentCallConversationId)) {
            closeDialog();
        }
    }

    // ==========================================
    // 4. KÍCH HOẠT ENGINE
    // ==========================================
    private void launchLiveKit(String conversationId, boolean isVideo, boolean isCaller) {
        isCallActive = true;
        
        new Thread(() -> {
            try {
                String appPath = System.getProperty("user.dir"); 
                String exePath = appPath + java.io.File.separator + "VideoEngine" + java.io.File.separator + "TutorHubVideo.exe";
                String roomName = "ChatRoom_Private_" + conversationId;
                
                String myName = this.activeMyName != null ? this.activeMyName : "User"; 
                
                ProcessBuilder pb = new ProcessBuilder(exePath, roomName, myName);
                Process process = pb.start(); 
                
                if (isCaller) {
                    String notifyText = isVideo ? "Đã bắt đầu cuộc gọi video" : "Đã bắt đầu cuộc gọi thoại";
                    NetworkManager.getInstance().sendPacket(new Packet("SEND_CHAT", conversationId + "|" + (isVideo ? "VIDEO_CALL" : "AUDIO_CALL") + "|" + notifyText));
                    chatTab.appendLocalMessage(isVideo ? "VIDEO_CALL" : "AUDIO_CALL", notifyText);
                }
                
                process.waitFor(); 
                
                if (isCaller) {
                    String endedText = "Cuộc gọi đã kết thúc";
                    NetworkManager.getInstance().sendPacket(new Packet("SEND_CHAT", conversationId + "|" + (isVideo ? "VIDEO_CALL" : "AUDIO_CALL") + "|" + endedText));
                    chatTab.appendLocalMessage(isVideo ? "VIDEO_CALL" : "AUDIO_CALL", endedText);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                isCallActive = false;
            }
        }).start();
    }
}

