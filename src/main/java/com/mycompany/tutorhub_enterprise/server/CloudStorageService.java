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
import java.time.Duration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Enterprise Cloud Storage Service (S3 Compatible)
 * Đóng vai trò lớp lõi xử lý File cho toàn bộ nền tảng EdTech TutorHub
 * Hỗ trợ: MinIO (local), AWS S3 (production)
 */
public class CloudStorageService {

    private static CloudStorageService instance;
    private S3Client s3Client;
    private boolean available = false;

    // Cấu hình kết nối hạ tầng vật lý (MinIO / B2)
    private static final String ENDPOINT = ServerConfig.get("TUTORHUB_B2_ENDPOINT", "tutorhub.b2.endpoint", "http://localhost:9000");
    private static final String BUCKET_NAME = ServerConfig.get("TUTORHUB_B2_BUCKET", "tutorhub.b2.bucket", "tutorhub-resources");
    private static final String ACCESS_KEY = ServerConfig.get("TUTORHUB_B2_ACCESS_KEY", "tutorhub.b2.accessKey", "");
    private static final String SECRET_KEY = ServerConfig.get("TUTORHUB_B2_SECRET_KEY", "tutorhub.b2.secretKey", "");

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

            this.available = true;
            System.out.println("[STORAGE] ✅ Kết nối Cloud Storage thành công tại " + ENDPOINT);

            // Bỏ qua listBuckets/createBucket tự động để tránh lỗi 403 với các Key giới hạn quyền hạn

        } catch (Exception e) {
            this.available = false;
            System.err.println("[STORAGE] ⚠️ Cloud Storage không khả dụng. Lỗi: " + e.getMessage());
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
            String contentType = null;
            String ext = "";
            if (originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
            }
            switch (ext) {
                case "docx": contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; break;
                case "xlsx": contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; break;
                case "pptx": contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation"; break;
                case "pdf": contentType = "application/pdf"; break;
                case "doc": contentType = "application/msword"; break;
                case "xls": contentType = "application/vnd.ms-excel"; break;
                case "ppt": contentType = "application/vnd.ms-powerpoint"; break;
                default: 
                    contentType = URLConnection.guessContentTypeFromName(originalName);
                    if (contentType == null) contentType = "application/octet-stream";
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
     * Tạo Pre-signed URL (Link có chữ ký bảo mật S3) cho các Bucket Private.
     * @param fileUrl URL vật lý của S3/B2
     * @param expirationMinutes Thời gian hết hạn của link (Phút)
     * @return Link đã được ký bí mật
     */
    public String generatePresignedUrl(String fileUrl, int expirationMinutes) {
        if (fileUrl == null || fileUrl.isEmpty()) return null;
        String fileKey = extractKey(fileUrl);
        if (fileKey == null) return null;

        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY);
            S3Configuration s3Config = S3Configuration.builder().pathStyleAccessEnabled(true).build();
            S3Presigner presigner = S3Presigner.builder()
                    .endpointOverride(URI.create(ENDPOINT))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.US_EAST_1)
                    .serviceConfiguration(s3Config)
                    .build();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
            String theUrl = presignedGetObjectRequest.url().toString();
            presigner.close();
            
            System.out.println("[STORAGE] 🔐 Đã tạo Pre-signed URL (Sống trong " + expirationMinutes + " phút)");
            return theUrl;
        } catch (Exception e) {
            System.err.println("[STORAGE ERROR] Presign URL thất bại: " + e.getMessage());
            return null;
        }
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

    // ==============================================================
    // S3 MULTIPART UPLOAD API (Hỗ trợ chia nhỏ tệp & tải đa luồng)
    // ==============================================================

    /**
     * Khởi tạo quá trình Multipart Upload, trả về Upload ID.
     */
    public String initiateMultipartUpload(String fileName, String contentType) {
        try {
            CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileName)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build();
            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(request);
            return response.uploadId();
        } catch (Exception e) {
            System.err.println("[STORAGE ERROR] Không thể khởi tạo Multipart Upload: " + e.getMessage());
            return null;
        }
    }

    /**
     * Tải lên một phần (Part) của tệp.
     */
    public CompletedPart uploadPart(String fileName, String uploadId, int partNumber, byte[] data) {
        try {
            UploadPartRequest uploadRequest = UploadPartRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileName)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .contentLength((long) data.length)
                    .build();

            UploadPartResponse response = s3Client.uploadPart(uploadRequest, RequestBody.fromBytes(data));
            return CompletedPart.builder()
                    .partNumber(partNumber)
                    .eTag(response.eTag())
                    .build();
        } catch (Exception e) {
            System.err.println("[STORAGE ERROR] Lỗi tải lên Part " + partNumber + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Hoàn tất quá trình Multipart Upload, yêu cầu server nối các mảnh lại.
     */
    public String completeMultipartUpload(String fileName, String uploadId, java.util.List<CompletedPart> completedParts) {
        try {
            completedParts.sort(java.util.Comparator.comparingInt(CompletedPart::partNumber));

            CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();

            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileName)
                    .uploadId(uploadId)
                    .multipartUpload(completedMultipartUpload)
                    .build();

            s3Client.completeMultipartUpload(completeRequest);

            String publicUrl = ENDPOINT + "/" + BUCKET_NAME + "/" + fileName;
            System.out.println("[STORAGE] ✅ Hoàn thành Multipart Upload! URL: " + publicUrl);
            return publicUrl;
        } catch (Exception e) {
            System.err.println("[STORAGE ERROR] Lỗi chốt Multipart Upload: " + e.getMessage());
            return null;
        }
    }

    /**
     * Hủy bỏ Multipart Upload.
     */
    public void abortMultipartUpload(String fileName, String uploadId) {
        try {
            AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileName)
                    .uploadId(uploadId)
                    .build();
            s3Client.abortMultipartUpload(abortRequest);
            System.out.println("[STORAGE] 🛑 Đã hủy Multipart Upload cho tệp: " + fileName);
        } catch (Exception e) {
            System.err.println("[STORAGE ERROR] Lỗi hủy Multipart Upload: " + e.getMessage());
        }
    }
}
