package com.mycompany.tutorhub_enterprise.client.services;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2RuntimeHandoffMeta;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Formatter;

public class V2RuntimeHandoffReader {

    private static final Gson gson = new Gson();

    public static V2RuntimeHandoffMeta readMeta(Path metaPath) throws Exception {
        if (!Files.exists(metaPath)) {
            throw new Exception("ERROR_META_FILE_NOT_FOUND");
        }
        String content = new String(Files.readAllBytes(metaPath), StandardCharsets.UTF_8);
        return gson.fromJson(content, V2RuntimeHandoffMeta.class);
    }

    public static void verifyEncryptedFileHash(Path encPath, V2RuntimeHandoffMeta meta) throws Exception {
        if (!Files.exists(encPath)) {
            throw new Exception("ERROR_ENC_FILE_NOT_FOUND");
        }
        String encryptedData = new String(Files.readAllBytes(encPath), StandardCharsets.UTF_8);
        String actualHash = hashString(encryptedData);
        if (meta.encryptedFileSha256 == null || !meta.encryptedFileSha256.equals(actualHash)) {
            throw new Exception("ERROR_HASH_MISMATCH");
        }
    }

    public static String decryptRuntimeHandoff(Path encPath, String base64Key) throws Exception {
        if (!Files.exists(encPath)) {
            throw new Exception("ERROR_ENC_FILE_NOT_FOUND");
        }
        String encryptedData = new String(Files.readAllBytes(encPath), StandardCharsets.UTF_8);
        try {
            return CryptoUtils.decryptWrapper(encryptedData, base64Key);
        } catch (Exception e) {
            throw new Exception("ERROR_DECRYPT_FAILED");
        }
    }

    public static V2ExamHandoffBundle parseBundle(String decryptedJson) throws Exception {
        if (decryptedJson == null || decryptedJson.isEmpty()) {
            throw new Exception("ERROR_EMPTY_DECRYPTED_JSON");
        }

        // Security check
        if (decryptedJson.contains("isCorrect") || 
            decryptedJson.contains("answerKey") || 
            decryptedJson.contains("correctOption") ||
            decryptedJson.contains("grading_config") ||
            decryptedJson.contains("password") ||
            decryptedJson.contains("passwordHash")) {
            throw new Exception("ERROR_SECURITY_VIOLATION_ANSWER_FIELDS");
        }

        try {
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(decryptedJson).getAsJsonObject();
            V2ExamHandoffBundle bundle = gson.fromJson(jsonObject, V2ExamHandoffBundle.class);
            if (jsonObject.has("sessionToken")) {
                bundle.sessionToken = jsonObject.get("sessionToken").getAsString();
            }
            return bundle;
        } catch (Exception e) {
            throw new Exception("ERROR_PARSE_FAILED");
        }
    }

    public static void validateBundleForChildPrototype(V2ExamHandoffBundle bundle, boolean isDebugMode) throws Exception {
        if (bundle == null) {
            throw new Exception("ERROR_NULL_BUNDLE");
        }
        if (bundle.examId <= 0) {
            throw new Exception("ERROR_INVALID_EXAM_ID");
        }
        if (bundle.paperId <= 0) {
            throw new Exception("ERROR_INVALID_PAPER_ID");
        }
        if (bundle.packageHash == null || bundle.packageHash.isEmpty()) {
            throw new Exception("ERROR_MISSING_PACKAGE_HASH");
        }
        if (bundle.questions == null || bundle.questions.isEmpty()) {
            throw new Exception("ERROR_MISSING_QUESTIONS");
        }
        if (!isDebugMode) {
            if (bundle.attemptId == null || bundle.attemptId.isEmpty()) {
                throw new Exception("ERROR_MISSING_ATTEMPT_ID");
            }
        }
    }

    private static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            String result = formatter.toString();
            formatter.close();
            return result;
        } catch (Exception e) {
            return "hash_error";
        }
    }
}
