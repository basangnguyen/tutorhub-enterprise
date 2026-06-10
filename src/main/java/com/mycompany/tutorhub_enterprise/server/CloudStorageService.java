package com.mycompany.tutorhub_enterprise.server;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.UUID;

/**
 * Enterprise Cloud Storage Service (S3 Compatible)
 * Đóng vai trò lớp lõi xử lý File cho toàn bộ nền tảng EdTech TutorHub
 * Hỗ trợ: MinIO (local), AWS S3 (production)
 */
public class CloudStorageService {

    private static CloudStorageService instance;
    private S3Client s3Client;
    private boolean available = false;

    // Cấu hình kết nối hạ tầng vật lý (MinIO local)
    private static final String ENDPOINT = ServerConfig.get("TUTORHUB_STORAGE_ENDPOINT", "tutorhub.storage.endpoint", "http://localhost:9000");
    private static final String BUCKET_NAME = ServerConfig.get("TUTORHUB_STORAGE_BUCKET", "tutorhub.storage.bucket", "tutorhub-resources");
    private static final String ACCESS_KEY = ServerConfig.get("TUTORHUB_STORAGE_ACCESS_KEY", "tutorhub.storage.accessKey", "");
    private static final String SECRET_KEY = ServerConfig.get("TUTORHUB_STORAGE_SECRET_KEY", "tutorhub.storage.secretKey", "");

    private CloudStorageService() {
        try {
            if (ServerConfig.isBlank(ACCESS_KEY) || ServerConfig.isBlank(SECRET_KEY)) {
                System.err.println("[STORAGE] Missing storage credentials. Set TUTORHUB_STORAGE_ACCESS_KEY and TUTORHUB_STORAGE_SECRET_KEY.");
                return;
            }

            AwsBasicCredentials credentials = AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);
            this.s3Client = S3Client.builder()
                    .endpointOverride(URI.create(ENDPOINT))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.US_EAST_1)
                    .forcePathStyle(true)
                    .build();

            // Kiểm tra kết nối bằng cách list buckets
            s3Client.listBuckets();
            this.available = true;
            System.out.println("[STORAGE] ✅ Kết nối MinIO thành công tại " + ENDPOINT);

            // Tạo bucket nếu chưa tồn tại
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(BUCKET_NAME).build());
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
                System.out.println("[STORAGE] ✅ Đã tạo bucket: " + BUCKET_NAME);
            }
        } catch (Exception e) {
            this.available = false;
            System.err.println("[STORAGE] ⚠️ MinIO không khả dụng. Sử dụng Local Storage. Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy thực thể duy nhất của Storage Service (Thread-safe Singleton)
     */
    public static synchronized CloudStorageService getInstance() {
        if (instance == null) {
            instance = new CloudStorageService();
        }
        return instance;
    }

    /**
     * Kiểm tra MinIO/S3 có khả dụng không
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Tải file lên hệ thống lưu trữ và trả về URL Public tuyệt đối
     */
    public String uploadFile(File localFile) {
        if (localFile == null || !localFile.exists()) {
            System.err.println("[STORAGE ERROR] File local không tồn tại hoặc rỗng!");
            return null;
        }

        String originalName = localFile.getName();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
        }

        String cloudFileName = UUID.randomUUID().toString() + extension;

        try {
            String contentType = URLConnection.guessContentTypeFromName(originalName);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(cloudFileName)
                    .contentType(contentType)
                    .build();

            System.out.println("[STORAGE] ⬆️ Đang đẩy dữ liệu file lên đám mây: " + originalName);
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(localFile));

            String publicUrl = ENDPOINT + "/" + BUCKET_NAME + "/" + cloudFileName;
            System.out.println("[STORAGE] ✅ Tải file lên thành công! URL: " + publicUrl);
            
            return publicUrl;

        } catch (S3Exception e) {
            System.err.println("[STORAGE ERROR] S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            System.err.println("[STORAGE ERROR] Upload: " + e.getMessage());
        }
        return null;
    }

    /**
     * Tải file xuống từ Cloud → trả về InputStream
     */
    public InputStream downloadFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return null;
        
        String fileKey = extractKey(fileUrl);
        if (fileKey == null) return null;

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileKey)
                    .build();
            
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getRequest);
            System.out.println("[STORAGE] ⬇️ Tải file từ Cloud: " + fileKey);
            return response;

        } catch (S3Exception e) {
            System.err.println("[STORAGE ERROR] Download: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            System.err.println("[STORAGE ERROR] Download: " + e.getMessage());
        }
        return null;
    }

    /**
     * Xóa file khỏi hệ thống lưu trữ đám mây
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return false;

        String fileKey = extractKey(fileUrl);
        if (fileKey == null) return false;

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            System.out.println("[STORAGE] ✅ Đã xóa file trên Cloud: " + fileKey);
            return true;

        } catch (S3Exception e) {
            System.err.println("[STORAGE ERROR] Xóa: " + e.awsErrorDetails().errorMessage());
            return false;
        }
    }

    /**
     * Trích xuất object key từ URL đầy đủ
     */
    private String extractKey(String fileUrl) {
        String prefixUrl = ENDPOINT + "/" + BUCKET_NAME + "/";
        if (fileUrl.startsWith(prefixUrl)) {
            return fileUrl.replace(prefixUrl, "");
        }
        // Nếu URL không phải cloud URL (file local), trả về null
        return null;
    }

    /**
     * Lấy endpoint hiện tại
     */
    public String getEndpoint() {
        return ENDPOINT;
    }

    /**
     * Lấy tên bucket
     */
    public String getBucketName() {
        return BUCKET_NAME;
    }
}
