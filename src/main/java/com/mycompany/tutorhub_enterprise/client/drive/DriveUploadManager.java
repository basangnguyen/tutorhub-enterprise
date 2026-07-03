package com.mycompany.tutorhub_enterprise.client.drive;

import com.mycompany.tutorhub_enterprise.server.CloudStorageService;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import javafx.beans.property.DoubleProperty;
import javafx.application.Platform;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Drive Upload Manager - Xử lý hàng đợi đa luồng và Chunked Upload cho file lớn.
 */
public class DriveUploadManager {

    private static DriveUploadManager instance;
    private final ExecutorService executor;
    private static final int CHUNK_SIZE = 15 * 1024 * 1024; // 15MB
    private static final int MAX_RETRIES = 3;

    private DriveUploadManager() {
        // Sử dụng ThreadPool kích thước 4 để không làm nghẽn băng thông
        this.executor = Executors.newFixedThreadPool(4);
    }

    public static synchronized DriveUploadManager getInstance() {
        if (instance == null) {
            instance = new DriveUploadManager();
        }
        return instance;
    }

    /**
     * Upload một file lên Cloud (tự động quyết định dùng nguyên khối hay đa mảnh).
     * @param file File cần upload
     * @param progressProperty DoubleProperty để binding với giao diện ProgressBar (từ 0.0 đến 1.0)
     * @return URL của file sau khi hoàn tất (hoặc null nếu lỗi)
     */
    public CompletableFuture<String> uploadFileAsync(File file, DoubleProperty progressProperty) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                CloudStorageService cloudService = CloudStorageService.getInstance();
                if (!cloudService.isAvailable()) {
                    System.err.println("[UPLOAD MANAGER] Cloud Storage không khả dụng!");
                    return null;
                }

                long fileSize = file.length();
                if (fileSize <= CHUNK_SIZE) {
                    // Upload nguyên khối
                    updateProgress(progressProperty, 0.5); // Báo đang up
                    String url = cloudService.uploadFile(file);
                    updateProgress(progressProperty, 1.0);
                    return url;
                } else {
                    // Multipart Upload
                    return doMultipartUpload(file, cloudService, progressProperty);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }); // Không truyền executor vào supplyAsync để nó chạy trên ForkJoinPool chung, chỉ phần part chạy trên executor.
    }

    private String doMultipartUpload(File file, CloudStorageService cloudService, DoubleProperty progressProperty) throws Exception {
        String originalName = file.getName();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
        }

        String cloudFileName = UUID.randomUUID().toString() + extension;
        String contentType = URLConnection.guessContentTypeFromName(originalName);

        System.out.println("[UPLOAD MANAGER] Bắt đầu Multipart Upload: " + cloudFileName + " (" + file.length() / 1024 / 1024 + " MB)");

        String uploadId = cloudService.initiateMultipartUpload(cloudFileName, contentType);
        if (uploadId == null) {
            throw new Exception("Không thể khởi tạo phiên Multipart.");
        }

        long fileSize = file.length();
        int totalParts = (int) Math.ceil((double) fileSize / CHUNK_SIZE);
        AtomicInteger completedPartsCounter = new AtomicInteger(0);

        List<CompletableFuture<CompletedPart>> partFutures = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            for (int i = 1; i <= totalParts; i++) {
                final int partNumber = i;
                long offset = (long) (partNumber - 1) * CHUNK_SIZE;
                int partSize = (int) Math.min(CHUNK_SIZE, fileSize - offset);
                
                byte[] chunkData = new byte[partSize];
                raf.seek(offset);
                raf.readFully(chunkData);

                // Tạo task upload cho part này (có Retry)
                CompletableFuture<CompletedPart> partFuture = CompletableFuture.supplyAsync(() -> {
                    return uploadPartWithRetry(cloudService, cloudFileName, uploadId, partNumber, chunkData);
                }, executor).thenApply(completedPart -> {
                    if (completedPart != null) {
                        int done = completedPartsCounter.incrementAndGet();
                        double progress = (double) done / totalParts;
                        updateProgress(progressProperty, progress);
                    }
                    return completedPart;
                });

                partFutures.add(partFuture);
            }

            // Chờ tất cả các parts tải xong
            CompletableFuture<Void> allOf = CompletableFuture.allOf(partFutures.toArray(new CompletableFuture[0]));
            allOf.join(); // Block đến khi xong (chạy trong luồng async cha nên không block UI)

            List<CompletedPart> completedParts = new ArrayList<>();
            for (CompletableFuture<CompletedPart> future : partFutures) {
                CompletedPart part = future.get();
                if (part == null) {
                    throw new Exception("Có part bị lỗi không thể phục hồi. Hủy toàn bộ Upload.");
                }
                completedParts.add(part);
            }

            // Chốt file
            return cloudService.completeMultipartUpload(cloudFileName, uploadId, completedParts);

        } catch (Exception e) {
            System.err.println("[UPLOAD MANAGER] Lỗi tải đa mảnh, đang tiến hành hủy (Abort)...: " + e.getMessage());
            cloudService.abortMultipartUpload(cloudFileName, uploadId);
            return null;
        }
    }

    private CompletedPart uploadPartWithRetry(CloudStorageService cloudService, String cloudFileName, String uploadId, int partNumber, byte[] chunkData) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                attempt++;
                CompletedPart part = cloudService.uploadPart(cloudFileName, uploadId, partNumber, chunkData);
                if (part != null) {
                    return part;
                }
                System.err.println("[UPLOAD MANAGER] Part " + partNumber + " trả về null, retry lần " + attempt);
            } catch (Exception e) {
                System.err.println("[UPLOAD MANAGER] Lỗi mạng khi tải Part " + partNumber + ", retry lần " + attempt);
            }
            // Tạm nghỉ 2 giây trước khi thử lại
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
        }
        return null;
    }

    private void updateProgress(DoubleProperty progressProperty, double progress) {
        if (progressProperty != null) {
            Platform.runLater(() -> progressProperty.set(progress));
        }
    }
}
