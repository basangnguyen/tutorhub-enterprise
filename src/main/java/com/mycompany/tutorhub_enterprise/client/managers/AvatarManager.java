package com.mycompany.tutorhub_enterprise.client.managers;

import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.mycompany.tutorhub_enterprise.client.AvatarCache;
import com.mycompany.tutorhub_enterprise.client.auth.ClientSessionManager;
import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.utils.B2Helper;

public class AvatarManager {
    private static AvatarManager instance;

    public interface AvatarListener {
        void onAvatarUpdated(int userId, Image newAvatar);
    }

    private final Map<Integer, Image> inMemoryCache = new ConcurrentHashMap<>();
    private final List<AvatarListener> listeners = new CopyOnWriteArrayList<>();

    private AvatarManager() {}

    public static synchronized AvatarManager getInstance() {
        if (instance == null) {
            instance = new AvatarManager();
        }
        return instance;
    }

    public void addListener(AvatarListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            for (Map.Entry<Integer, Image> entry : inMemoryCache.entrySet()) {
                try {
                    listener.onAvatarUpdated(entry.getKey(), entry.getValue());
                } catch (Exception ignored) {}
            }
        }
    }

    public void removeListener(AvatarListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(int userId, Image avatar) {
        for (AvatarListener listener : listeners) {
            try {
                listener.onAvatarUpdated(userId, avatar);
            } catch (Exception ignored) {}
        }
    }

    public void setAvatar(int userId, Image avatar, byte[] rawBytes) {
        if (avatar != null) {
            inMemoryCache.put(userId, avatar);
            if (rawBytes != null) {
                AvatarCache.saveAvatar(String.valueOf(userId), rawBytes);
            }
            notifyListeners(userId, avatar);
        }
    }

    public Image getAvatar(int userId) {
        // 1. Return from in-memory cache if available
        if (inMemoryCache.containsKey(userId)) {
            return inMemoryCache.get(userId);
        }

        // 2. Try loading from disk asynchronously
        new Thread(() -> {
            byte[] cached = AvatarCache.loadAvatar(String.valueOf(userId));
            if (cached != null) {
                try {
                    Image rawImg = new ImageIcon(cached).getImage();
                    setAvatar(userId, rawImg, null); // don't re-save to disk
                    return;
                } catch (Exception ignored) {}
            }
            
            // 3. Request from server if missing on disk
            try {
                NetworkManager.getInstance().sendPacket(new Packet("GET_USER_AVATAR", String.valueOf(userId)));
            } catch (Exception ignored) {}
        }).start();

        return null;
    }

    public void handlePacket(Packet packet) {
        Integer currentUserId = ClientSessionManager.getCurrentUserId();
        if (currentUserId == null) return;

        new Thread(() -> {
            try {
                if ("LOAD_AVATAR".equals(packet.action) || "UPDATE_AVATAR_SUCCESS".equals(packet.action)) {
                    byte[] imageBytes = Base64.getDecoder().decode(packet.payload);
                    Image rawImg = new ImageIcon(imageBytes).getImage();
                    setAvatar(currentUserId, rawImg, imageBytes);
                } else if ("LOAD_AVATAR_URL".equals(packet.action) || "UPDATE_AVATAR_URL_SUCCESS".equals(packet.action)) {
                    loadAvatarFromUrl(currentUserId, packet.payload);
                } else if ("LOAD_TUTOR_AVATAR".equals(packet.action)) {
                    // Cần server gửi kèm userId, tạm thời bỏ qua nếu server chỉ gửi Base64
                }
            } catch (Exception ignored) {}
        }, "avatar-packet-handler").start();
    }

    private void loadAvatarFromUrl(int userId, String urlStr) {
        if (urlStr == null || urlStr.trim().isEmpty()) return;
        try {
            Image avatarImg = ImageIO.read(new URL(urlStr.trim()));
            if (avatarImg != null) {
                java.awt.image.BufferedImage bImg = toBufferedImage(avatarImg);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bImg, "jpg", baos);
                byte[] avatarBytes = baos.toByteArray();
                
                setAvatar(userId, avatarImg, avatarBytes);
            }
        } catch (Exception ignored) {}
    }

    public void uploadAvatar(byte[] imageBytes, String email) {
        Integer currentUserId = ClientSessionManager.getCurrentUserId();
        if (currentUserId == null || imageBytes == null) return;

        // Apply immediately for snappy UI
        try {
            Image rawImg = new ImageIcon(imageBytes).getImage();
            setAvatar(currentUserId, rawImg, imageBytes);
            if (email != null) {
                AvatarCache.saveAvatar(email, imageBytes); // backward compatibility
            }
        } catch (Exception ignored) {}

        // Upload in background
        new Thread(() -> {
            try {
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                String avatarUrl = B2Helper.uploadBase64Image(base64Image, ".jpg");
                
                if (avatarUrl != null) {
                    NetworkManager.getInstance().sendPacket(new Packet("UPDATE_AVATAR_URL", avatarUrl));
                } else {
                    NetworkManager.getInstance().sendPacket(new Packet("UPDATE_AVATAR", base64Image));
                }
            } catch (Exception ignored) {}
        }, "avatar-uploader").start();
    }

    private java.awt.image.BufferedImage toBufferedImage(Image img) {
        if (img instanceof java.awt.image.BufferedImage) return (java.awt.image.BufferedImage) img;
        int w = img.getWidth(null) > 0 ? img.getWidth(null) : 200;
        int h = img.getHeight(null) > 0 ? img.getHeight(null) : 200;
        java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        bImg.createGraphics().drawImage(img, 0, 0, null);
        return bImg;
    }
}
