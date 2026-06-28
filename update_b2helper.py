import re

file_path = "d:/Ban_sao_du_an/src/main/java/com/mycompany/tutorhub_enterprise/utils/B2Helper.java"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add methods to B2Helper
methods_to_add = """
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
"""

if "uploadJsonData" not in content:
    # insert before the last brace
    idx = content.rfind("}")
    if idx != -1:
        content = content[:idx] + methods_to_add + "\n" + content[idx:]
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print("Methods added successfully")
    else:
        print("Could not find closing brace")
else:
    print("Methods already exist")
