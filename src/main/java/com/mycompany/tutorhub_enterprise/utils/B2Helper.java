package com.mycompany.tutorhub_enterprise.utils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import com.mycompany.tutorhub_enterprise.server.ServerConfig;

import java.net.URI;
import java.time.Duration;

public class B2Helper {
    private static S3Presigner presigner = null;
    private static final String BUCKET_NAME = ServerConfig.get("TUTORHUB_B2_BUCKET", "tutorhub.b2.bucket", null, "tutorhub-videos-123");
    private static final String ENDPOINT = ServerConfig.get("TUTORHUB_B2_ENDPOINT", "tutorhub.b2.endpoint", null, "https://s3.us-west-004.backblazeb2.com");
    private static final String REGION = ServerConfig.get("TUTORHUB_B2_REGION", "tutorhub.b2.region", null, "us-west-004");
    private static final String PUBLIC_BASE_URL = ServerConfig.get("TUTORHUB_B2_PUBLIC_BASE_URL", "tutorhub.b2.publicBaseUrl", null, "https://tutorhub-videos-123.s3.us-west-004.backblazeb2.com");
    private static final String ACCESS_KEY = ServerConfig.get("TUTORHUB_B2_ACCESS_KEY", "tutorhub.b2.accessKey", null, "");
    private static final String SECRET_KEY = ServerConfig.get("TUTORHUB_B2_SECRET_KEY", "tutorhub.b2.secretKey", null, "");

    static {
        System.out.println("[B2_CONFIG] Audit Config Loaded:");
        System.out.println("[B2_CONFIG] - TUTORHUB_B2_BUCKET present: " + !isBlank(BUCKET_NAME));
        System.out.println("[B2_CONFIG] - TUTORHUB_B2_ENDPOINT present: " + !isBlank(ENDPOINT));
        System.out.println("[B2_CONFIG] - TUTORHUB_B2_REGION present: " + !isBlank(REGION));
        System.out.println("[B2_CONFIG] - TUTORHUB_B2_ACCESS_KEY present: " + !isBlank(ACCESS_KEY));
        System.out.println("[B2_CONFIG] - TUTORHUB_B2_SECRET_KEY present: " + !isBlank(SECRET_KEY));
        System.out.println("[B2_CONFIG] Config source priority: 1. ENV -> 2. System Property -> 3. application.properties");
    }

    public static synchronized S3Presigner getPresigner() {
        ensureConfigured();
        if (presigner == null) {
            presigner = S3Presigner.builder()
                .endpointOverride(URI.create(ENDPOINT))
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)
                ))
                .build();
        }
        return presigner;
    }

    private static software.amazon.awssdk.services.s3.S3Client s3Client = null;
    public static synchronized software.amazon.awssdk.services.s3.S3Client getS3Client() {
        ensureConfigured();
        if (s3Client == null) {
            s3Client = createS3Client();
        }
        return s3Client;
    }

    public static software.amazon.awssdk.services.s3.S3Client createS3Client() {
        ensureConfigured();
        return software.amazon.awssdk.services.s3.S3Client.builder()
            .endpointOverride(URI.create(ENDPOINT))
            .region(Region.of(REGION))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)
            ))
            .build();
    }

    public static boolean isConfigured() {
        return !isBlank(ACCESS_KEY) && !isBlank(SECRET_KEY);
    }

    public static String getBucketName() {
        return BUCKET_NAME;
    }

    public static String getPublicBaseUrl() {
        return PUBLIC_BASE_URL.replaceAll("/+$", "");
    }


    public static String uploadBase64Image(String base64Data, String extension) {
        try {
            if (base64Data.contains(",")) {
                base64Data = base64Data.split(",")[1];
            }
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
            String fileName = java.util.UUID.randomUUID().toString() + (extension.startsWith(".") ? extension : "." + extension);
            
            software.amazon.awssdk.services.s3.model.PutObjectRequest putObjectRequest = software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(fileName)
                .contentType("image/" + (extension.replace(".", "")))
                .build();
                
            getS3Client().putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(imageBytes));
            return getPublicBaseUrl() + "/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getPresignedUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isEmpty()) return originalUrl;
        
        String key = null;
        String b2Prefix1 = "https://tutorhub-videos-123.s3.us-west-004.backblazeb2.com/";
        String b2Prefix2 = "https://f004.backblazeb2.com/file/tutorhub-videos-123/";
        
        if (originalUrl.startsWith(b2Prefix1)) {
            key = originalUrl.substring(b2Prefix1.length());
        } else if (originalUrl.startsWith(b2Prefix2)) {
            key = originalUrl.substring(b2Prefix2.length());
        }
        
        if (key == null) {
            return originalUrl;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(7)) // Link có thời hạn 7 ngày
                .getObjectRequest(getObjectRequest)
                .build();

            PresignedGetObjectRequest presignedGetObjectRequest = getPresigner().presignGetObject(getObjectPresignRequest);
            String presignedUrl = presignedGetObjectRequest.url().toString();
            
            // Tạm thời tắt thay thế CDN vì làm thay đổi Host của URL, dẫn đến sai chữ ký AWS S3 Signature gây lỗi 403
            return presignedUrl;
        } catch (Exception e) {
            e.printStackTrace();
            return originalUrl;
        }
    }

    private static void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("Missing Backblaze B2 credentials. Set TUTORHUB_B2_ACCESS_KEY and TUTORHUB_B2_SECRET_KEY.");
        }
    }


    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean uploadJsonData(String jsonData, String objectKey) {
        try {
            software.amazon.awssdk.services.s3.model.PutObjectRequest putObjectRequest = software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(objectKey)
                .contentType("application/json")
                .build();
            getS3Client().putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromString(jsonData));
            return true;
        } catch (Exception e) {
            System.err.println("[B2Helper] Upload JSON failed: " + e.getMessage());
            return false;
        }
    }

    public static String downloadJsonData(String objectKey) {
        try {
            software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(objectKey)
                .build();
            software.amazon.awssdk.core.ResponseBytes<software.amazon.awssdk.services.s3.model.GetObjectResponse> objectBytes = 
                getS3Client().getObjectAsBytes(getObjectRequest);
            return objectBytes.asUtf8String();
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return null; // Not found
        } catch (Exception e) {
            System.err.println("[B2Helper] Download JSON failed: " + e.getMessage());
            return null;
        }
    }

    public static boolean deleteObject(String objectKey) {
        try {
            software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteObjectRequest = software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(objectKey)
                .build();
            getS3Client().deleteObject(deleteObjectRequest);
            return true;
        } catch (Exception e) {
            System.err.println("[B2Helper] Delete Object failed: " + e.getMessage());
            return false;
        }
    }

}
