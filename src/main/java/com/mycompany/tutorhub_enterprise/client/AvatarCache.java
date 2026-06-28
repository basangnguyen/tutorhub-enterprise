package com.mycompany.tutorhub_enterprise.client;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class AvatarCache {
    private static final String CACHE_DIR = System.getProperty("user.home") + File.separator + ".tutorhub_cache" + File.separator + "avatars";

    public static void saveAvatar(String userId, byte[] imageBytes) {
        if (userId == null || imageBytes == null) return;
        try {
            File dir = new File(CACHE_DIR);
            if (!dir.exists()) dir.mkdirs();
            
            String safeId = userId.replaceAll("[^a-zA-Z0-9.-]", "_");
            File cacheFile = new File(dir, "avatar_" + safeId + ".jpg");
            Files.write(cacheFile.toPath(), imageBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] loadAvatar(String userId) {
        if (userId == null) return null;
        try {
            String safeId = userId.replaceAll("[^a-zA-Z0-9.-]", "_");
            File cacheFile = new File(CACHE_DIR, "avatar_" + safeId + ".jpg");
            if (cacheFile.exists()) {
                return Files.readAllBytes(cacheFile.toPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
