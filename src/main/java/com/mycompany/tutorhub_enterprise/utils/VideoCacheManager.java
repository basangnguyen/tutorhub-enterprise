package com.mycompany.tutorhub_enterprise.utils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class VideoCacheManager {
    private static final String CACHE_DIR = System.getenv("LOCALAPPDATA") + File.separator + "TutorHub" + File.separator + "VideoCache";
    private static final long MAX_CACHE_SIZE_BYTES = 500L * 1024L * 1024L; // 500 MB
    private static final ExecutorService executor = Executors.newFixedThreadPool(2); // Tải tối đa 2 video cùng lúc
    private static final java.util.Set<String> activeDownloads = java.util.concurrent.ConcurrentHashMap.newKeySet();
    
    static {
        File dir = new File(CACHE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static String getFileNameFromUrl(String url) {
        String key = url;
        int lastSlash = key.lastIndexOf('/');
        if (lastSlash != -1) {
            key = key.substring(lastSlash + 1);
        }
        // Xóa các query parameter nếu có (ví dụ: ?X-Amz-Algorithm=...)
        int questionMark = key.indexOf('?');
        if (questionMark != -1) {
            key = key.substring(0, questionMark);
        }
        return key.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    public static File getCachedFile(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return null;
        String fileName = getFileNameFromUrl(rawUrl);
        return new File(CACHE_DIR, fileName);
    }

    public static String getAvailableVideoUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return "";
        File cachedFile = getCachedFile(rawUrl);
        if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
            // Đã tải xong
            try {
                // JavaFX Media yêu cầu format URI chuẩn
                return cachedFile.toURI().toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Chưa tải xong hoặc chưa tải, trả về Presigned URL phát từ mạng
        return B2Helper.getPresignedUrl(rawUrl);
    }

    public static void cacheVideo(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return;
        File cachedFile = getCachedFile(rawUrl);
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return; // Đã cache
        }
        
        String fileName = cachedFile.getName();
        if (!activeDownloads.add(fileName)) {
            return; // File này đang được tải bởi một luồng khác rồi
        }
        
        executor.submit(() -> {
            try {
                File tempFile = new File(cachedFile.getAbsolutePath() + ".tmp");
                try {
                    // Xóa file rác nếu tải lỗi trước đó
                    if (tempFile.exists()) tempFile.delete();
                    
                    String presignedUrl = B2Helper.getPresignedUrl(rawUrl);
                    URL url = new URL(presignedUrl);
                    URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(10000);
                    
                    try (InputStream in = conn.getInputStream();
                         FileOutputStream out = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    // Tải xong, đổi tên file tmp thành file chính thức
                    if (tempFile.exists() && tempFile.length() > 0) {
                        Files.move(tempFile.toPath(), cachedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        cleanupCache(); // Kiểm tra dọn dẹp dung lượng
                    }
                } catch (Exception e) {
                    if (tempFile.exists()) tempFile.delete();
                    System.out.println("[VideoCacheManager] Lỗi tải cache: " + e.getMessage());
                }
            } finally {
                activeDownloads.remove(fileName); // Giải phóng cờ trạng thái
            }
        });
    }

    private static synchronized void cleanupCache() {
        try {
            Path cachePath = Paths.get(CACHE_DIR);
            long totalSize = 0;
            
            try (Stream<Path> paths = Files.list(cachePath)) {
                totalSize = paths
                    .map(p -> p.toFile())
                    .filter(f -> f.isFile() && !f.getName().endsWith(".tmp"))
                    .mapToLong(File::length)
                    .sum();
            }
            
            if (totalSize > MAX_CACHE_SIZE_BYTES) {
                // Sắp xếp file theo thời gian sửa đổi cũ nhất (LRU cơ bản)
                try (Stream<Path> paths = Files.list(cachePath)) {
                    paths.map(Path::toFile)
                         .filter(f -> f.isFile() && !f.getName().endsWith(".tmp"))
                         .sorted(Comparator.comparingLong(File::lastModified))
                         .forEach(file -> {
                             // Xóa cho đến khi dưới mức 80% của Max size
                             if (getCacheSize() > MAX_CACHE_SIZE_BYTES * 0.8) {
                                 file.delete();
                             }
                         });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static long getCacheSize() {
        try (Stream<Path> paths = Files.list(Paths.get(CACHE_DIR))) {
            return paths
                .map(Path::toFile)
                .filter(f -> f.isFile() && !f.getName().endsWith(".tmp"))
                .mapToLong(File::length)
                .sum();
        } catch (Exception e) {
            return 0;
        }
    }
}
