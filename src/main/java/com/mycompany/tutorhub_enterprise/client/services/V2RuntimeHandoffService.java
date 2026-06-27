package com.mycompany.tutorhub_enterprise.client.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Formatter;

public class V2RuntimeHandoffService {

    public static final String RUNTIME_DIR = "runtime";
    public static final String ENC_FILE_NAME = "v2_handoff_runtime.enc";
    public static final String META_FILE_NAME = "v2_handoff_runtime.meta.json";

    public static String createEncryptedRuntimeHandoff(V2ExamHandoffBundle bundle, String base64Key, boolean isDebugMode, String handoffId, String nonce, String parentIpcHost, int parentIpcPort) throws Exception {
        validateSafeBeforeEncrypt(bundle, isDebugMode);

        Gson gson = new Gson();
        
        // Build payload
        JsonObject payload = gson.toJsonTree(bundle).getAsJsonObject();
        
        // Inject real session token for runtime handoff
        if (bundle.sessionToken != null && !bundle.sessionToken.isEmpty()) {
            payload.addProperty("sessionToken", bundle.sessionToken);
        } else if (!isDebugMode) {
            throw new Exception("sessionToken missing in non-debug mode");
        }

        String jsonStr = gson.toJson(payload);

        // Security check on jsonStr before encrypt
        if (jsonStr.contains("isCorrect") || jsonStr.contains("answerKey") || jsonStr.contains("correctOption")) {
            throw new Exception("SECURITY VIOLATION: Handoff payload contains answer fields");
        }
        
        // Encrypt
        String encryptedData = CryptoUtils.encryptWrapper(jsonStr, base64Key);

        // Compute sha256 of encrypted data
        String encryptedSha256 = hashString(encryptedData);

        // Ensure runtime dir exists
        File dir = new File(System.getProperty("user.dir"), RUNTIME_DIR);
        if (!dir.exists()) dir.mkdirs();

        File encFile = new File(dir, ENC_FILE_NAME);
        File metaFile = new File(dir, META_FILE_NAME);

        // Write ENC file
        Files.write(Paths.get(encFile.getAbsolutePath()), encryptedData.getBytes(StandardCharsets.UTF_8));

        // Create meta file content
        JsonObject meta = new JsonObject();
        if (handoffId != null) meta.addProperty("handoffId", handoffId);
        meta.addProperty("handoffVersion", bundle.handoffVersion);
        meta.addProperty("flow", bundle.flow);
        meta.addProperty("examId", bundle.examId);
        meta.addProperty("paperId", bundle.paperId);
        if (bundle.attemptId != null) meta.addProperty("attemptId", bundle.attemptId);
        if (bundle.deadlineAt != null) meta.addProperty("deadlineAt", bundle.deadlineAt);
        meta.addProperty("packageHash", bundle.packageHash);
        meta.addProperty("questionCount", bundle.questionCount);
        meta.addProperty("totalScore", bundle.totalScore);
        meta.addProperty("encryptedFileName", ENC_FILE_NAME);
        meta.addProperty("encryptedFileSha256", encryptedSha256);
        if (bundle.createdAt != null) meta.addProperty("createdAt", bundle.createdAt);
        meta.addProperty("isDebugMode", isDebugMode);
        
        if (nonce != null) meta.addProperty("nonce", nonce);
        if (parentIpcHost != null) meta.addProperty("parentIpcHost", parentIpcHost);
        if (parentIpcPort > 0) meta.addProperty("parentIpcPort", parentIpcPort);

        // Validate meta doesn't have secret
        String metaStr = gson.toJson(meta);
        if (bundle.sessionToken != null && !bundle.sessionToken.isEmpty() && metaStr.contains(bundle.sessionToken)) {
            throw new Exception("SECURITY VIOLATION: Meta file leaked sessionToken");
        }

        // Write Meta file
        Files.write(Paths.get(metaFile.getAbsolutePath()), metaStr.getBytes(StandardCharsets.UTF_8));

        return encFile.getAbsolutePath();
    }

    private static void validateSafeBeforeEncrypt(V2ExamHandoffBundle bundle, boolean isDebugMode) throws Exception {
        if (bundle == null) throw new Exception("Bundle is null");
        if (bundle.examId <= 0) throw new Exception("Invalid examId");
        if (bundle.paperId <= 0) throw new Exception("Invalid paperId");
        if (bundle.packageHash == null || bundle.packageHash.isEmpty()) throw new Exception("packageHash is empty");
        if (bundle.questionCount <= 0) throw new Exception("questionCount <= 0");
        if (bundle.questions == null || bundle.questions.isEmpty()) throw new Exception("questions array is empty");
        
        if (!isDebugMode) {
            if (bundle.attemptId == null || bundle.attemptId.isEmpty()) throw new Exception("attemptId is missing");
            if (bundle.sessionToken == null || bundle.sessionToken.isEmpty()) throw new Exception("sessionToken is missing");
        }
    }

    public static String verifyEncryptedHandoffForTest(String base64Key) throws Exception {
        File dir = new File(System.getProperty("user.dir"), RUNTIME_DIR);
        File encFile = new File(dir, ENC_FILE_NAME);
        if (!encFile.exists()) throw new Exception("ENC file not found");

        String encryptedData = new String(Files.readAllBytes(Paths.get(encFile.getAbsolutePath())), StandardCharsets.UTF_8);
        return CryptoUtils.decryptWrapper(encryptedData, base64Key);
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
