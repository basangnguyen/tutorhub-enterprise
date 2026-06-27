package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public class TSEV2EncryptedSubmitDryRunService {

    public static final String SCHEMA_VERSION = "1.0";
    public static final String FLOW = "PAPER_START_V2";
    public static final String ENC_FILE_NAME = "v2_submit_payload_dryrun.enc";
    public static final String META_FILE_NAME = "v2_submit_payload_dryrun.meta.json";

    public static final String ERROR_SUBMIT_DRYRUN_HASH_MISMATCH = "ERROR_SUBMIT_DRYRUN_HASH_MISMATCH";
    public static final String ERROR_SUBMIT_DRYRUN_DECRYPT_FAILED = "ERROR_SUBMIT_DRYRUN_DECRYPT_FAILED";
    public static final String ERROR_SUBMIT_DRYRUN_META_UNSAFE = "ERROR_SUBMIT_DRYRUN_META_UNSAFE";
    public static final String ERROR_SUBMIT_DRYRUN_PAYLOAD_UNSAFE = "ERROR_SUBMIT_DRYRUN_PAYLOAD_UNSAFE";
    public static final String ERROR_SUBMIT_DRYRUN_CONTEXT_MISMATCH = "ERROR_SUBMIT_DRYRUN_CONTEXT_MISMATCH";

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private static final String[] FORBIDDEN_META_TOKENS = {
            "answers",
            "selectedoptionid",
            "sessiontoken",
            "keyb64",
            "aeskey",
            "secretkey",
            "rawkey",
            "plaintextjson",
            "plaintext",
            "iscorrect",
            "answerkey",
            "correctoption",
            "passwordhash",
            "password",
            "score",
            "gradingresult"
    };

    private static final String[] FORBIDDEN_PAYLOAD_TOKENS = {
            "sessiontoken",
            "keyb64",
            "aeskey",
            "secretkey",
            "rawkey",
            "plaintextjson",
            "plaintext",
            "iscorrect",
            "answerkey",
            "correctoption",
            "passwordhash",
            "password",
            "score",
            "gradingresult",
            "exam_submit",
            "submit_payload.enc",
            "autosave_payload.enc"
    };

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final TSEV2SubmitPayloadService payloadService;
    private final SecureRandom secureRandom;

    public static class EncryptedSubmitDryRunException extends RuntimeException {
        private final String errorCode;

        public EncryptedSubmitDryRunException(String errorCode, Throwable cause) {
            super(errorCode, cause);
            this.errorCode = errorCode;
        }

        public EncryptedSubmitDryRunException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public TSEV2EncryptedSubmitDryRunService() {
        this(new TSEV2SubmitPayloadService(), new SecureRandom());
    }

    public TSEV2EncryptedSubmitDryRunService(
            TSEV2SubmitPayloadService payloadService,
            SecureRandom secureRandom
    ) {
        this.payloadService = payloadService == null
                ? new TSEV2SubmitPayloadService()
                : payloadService;
        this.secureRandom = secureRandom == null ? new SecureRandom() : secureRandom;
    }

    public static SecretKey generateSubmitDryRunKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (Exception ex) {
            throw new IllegalStateException("Submit dry-run key generation failed.", ex);
        }
    }

    public TSEV2SubmitDryRunMeta saveEncryptedSubmitPayloadDryRun(
            TSEV2SubmitPayload payload,
            SecretKey submitDryRunKey,
            Path dryRunDir
    ) {
        try {
            if (dryRunDir == null) {
                throw new IllegalArgumentException("Dry-run directory is missing.");
            }
            validateSubmitDryRunKey(submitDryRunKey);
            payloadService.validatePayloadSafe(payload);

            String payloadJson = payloadService.toJson(payload);
            validateSubmitPayloadJsonSafe(payloadJson);

            byte[] encryptedBytes = encryptPayload(payloadJson, submitDryRunKey);
            String encryptedSha256 = sha256Hex(encryptedBytes);

            TSEV2SubmitDryRunMeta meta = createMeta(payload, encryptedSha256);
            validateMetaSafe(meta);

            String metaJson = gson.toJson(meta);
            validateMetaJsonSafe(metaJson);

            Files.createDirectories(dryRunDir);
            writeAtomicEncryptedBytes(dryRunDir.resolve(ENC_FILE_NAME), encryptedBytes);
            writeAtomicSafeMeta(dryRunDir.resolve(META_FILE_NAME), metaJson.getBytes(StandardCharsets.UTF_8));
            
            return meta;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Encrypted submit dry-run failed.", ex);
        }
    }

    public TSEV2SubmitPayload loadEncryptedSubmitPayloadDryRun(
            Path encPath,
            Path metaPath,
            SecretKey submitDryRunKey
    ) {
        try {
            validateSubmitDryRunKey(submitDryRunKey);
            if (encPath == null || metaPath == null) {
                throw new IllegalArgumentException("Dry-run paths are missing.");
            }

            String metaJson = Files.readString(metaPath, StandardCharsets.UTF_8);
            validateMetaJsonSafe(metaJson);
            TSEV2SubmitDryRunMeta meta = gson.fromJson(metaJson, TSEV2SubmitDryRunMeta.class);
            validateMetaSafe(meta);

            byte[] encryptedBytes = Files.readAllBytes(encPath);
            String actualHash = sha256Hex(encryptedBytes);
            if (!actualHash.equals(meta.getEncryptedFileSha256())) {
                throw restoreFailure(ERROR_SUBMIT_DRYRUN_HASH_MISMATCH, new IllegalArgumentException("Encrypted file hash mismatch."));
            }

            String payloadJson;
            try {
                payloadJson = decryptPayload(encryptedBytes, submitDryRunKey);
            } catch (Exception ex) {
                throw restoreFailure(ERROR_SUBMIT_DRYRUN_DECRYPT_FAILED, ex);
            }

            validateSubmitPayloadJsonSafe(payloadJson);
            TSEV2SubmitPayload payload = gson.fromJson(payloadJson, TSEV2SubmitPayload.class);
            payloadService.validatePayloadSafe(payload);
            validateMetaMatchesPayload(meta, payload);

            return payload;
        } catch (EncryptedSubmitDryRunException ex) {
            throw ex;
        } catch (Exception ex) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_PAYLOAD_UNSAFE, ex);
        }
    }

    public void validateMetaSafe(TSEV2SubmitDryRunMeta meta) {
        if (meta == null) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_META_UNSAFE, "Meta is missing.");
        }
        if (!SCHEMA_VERSION.equals(meta.getSchemaVersion())) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_META_UNSAFE, "Schema version is invalid.");
        }
        if (!FLOW.equals(meta.getFlow())) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_META_UNSAFE, "Flow is invalid.");
        }
        if (meta.getExamId() <= 0 || meta.getPaperId() <= 0) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_META_UNSAFE, "Exam identifiers are invalid.");
        }
        if (meta.getQuestionCount() < 0 || meta.getAnsweredCount() < 0) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_META_UNSAFE, "Counts are invalid.");
        }
        if (meta.getAnsweredCount() > meta.getQuestionCount()) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_META_UNSAFE, "Answered count exceeds question count.");
        }
        if (isBlank(meta.getPayloadHash()) || isBlank(meta.getEncryptedFileSha256())) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_META_UNSAFE, "Hashes are missing.");
        }
        if (!ENC_FILE_NAME.equals(meta.getEncFileName())) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_META_UNSAFE, "Encrypted file name is invalid.");
        }
        validateMetaJsonSafe(gson.toJson(meta));
    }

    public void validateSubmitPayloadJsonSafe(String json) {
        if (isBlank(json)) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_PAYLOAD_UNSAFE, "Submit payload JSON is missing.");
        }
        String lower = json.toLowerCase();
        for (String token : FORBIDDEN_PAYLOAD_TOKENS) {
            if (lower.contains(token)) {
                throw restoreFailure(ERROR_SUBMIT_DRYRUN_PAYLOAD_UNSAFE, "Submit payload JSON contains forbidden token: " + token);
            }
        }
    }

    public Path resolveDefaultDryRunDir(TSEV2SubmitPayload payload) {
        String attemptSegment = "debug";
        if (payload != null && !isBlank(payload.getAttemptId())) {
            attemptSegment = sanitizePathSegment(payload.getAttemptId());
        }
        return Path.of(
                System.getProperty("user.home"),
                ".tutorhub-secure-exam",
                "debug",
                "v2-submit-dryrun",
                attemptSegment
        );
    }

    private TSEV2SubmitDryRunMeta createMeta(TSEV2SubmitPayload payload, String encryptedSha256) {
        TSEV2SubmitDryRunMeta meta = new TSEV2SubmitDryRunMeta();
        meta.setSchemaVersion(SCHEMA_VERSION);
        meta.setFlow(FLOW);
        meta.setMode("DEBUG_DRY_RUN");
        meta.setExamId(payload.getExamId());
        meta.setPaperId(payload.getPaperId());
        meta.setAttemptId(payload.getAttemptId());
        meta.setPackageHash(payload.getPackageHash());
        meta.setQuestionCount(payload.getQuestionCount());
        meta.setAnsweredCount(payload.getAnsweredCount());
        meta.setUnansweredCount(payload.getUnansweredCount());
        meta.setComplete(payload.isComplete());
        meta.setPayloadHash(payload.getPayloadHash());
        meta.setEncryptedFileSha256(encryptedSha256);
        meta.setEncFileName(ENC_FILE_NAME);
        String now = Instant.now().toString();
        meta.setCreatedAt(now);
        meta.setUpdatedAt(now);
        return meta;
    }

    private void validateMetaMatchesPayload(TSEV2SubmitDryRunMeta meta, TSEV2SubmitPayload payload) {
        if (meta.getExamId() != payload.getExamId()
                || meta.getPaperId() != payload.getPaperId()
                || meta.getQuestionCount() != payload.getQuestionCount()
                || meta.getAnsweredCount() != payload.getAnsweredCount()
                || meta.getUnansweredCount() != payload.getUnansweredCount()
                || meta.isComplete() != payload.isComplete()
                || !safeEquals(meta.getAttemptId(), payload.getAttemptId())
                || !safeEquals(meta.getPackageHash(), payload.getPackageHash())
                || !safeEquals(meta.getPayloadHash(), payload.getPayloadHash())) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_CONTEXT_MISMATCH, new IllegalArgumentException("Meta does not match payload."));
        }
    }

    private void validateMetaJsonSafe(String metaJson) {
        if (isBlank(metaJson)) {
            throw restoreFailure(ERROR_SUBMIT_DRYRUN_META_UNSAFE, "Meta JSON is missing.");
        }
        String lower = metaJson.toLowerCase();
        for (String token : FORBIDDEN_META_TOKENS) {
            if (lower.contains(token)) {
                throw restoreFailure(ERROR_SUBMIT_DRYRUN_META_UNSAFE, "Meta JSON contains forbidden token.");
            }
        }
    }

    private byte[] encryptPayload(String payloadJson, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] cipherText = cipher.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("alg", CIPHER_ALGORITHM);
        wrapper.addProperty("iv", Base64.getEncoder().encodeToString(iv));
        wrapper.addProperty("ciphertext", Base64.getEncoder().encodeToString(cipherText));
        return gson.toJson(wrapper).getBytes(StandardCharsets.UTF_8);
    }

    private String decryptPayload(byte[] encryptedBytes, SecretKey key) throws Exception {
        JsonObject wrapper = gson.fromJson(new String(encryptedBytes, StandardCharsets.UTF_8), JsonObject.class);
        if (wrapper == null || !wrapper.has("alg") || !wrapper.has("iv") || !wrapper.has("ciphertext")) {
            throw new IllegalArgumentException("Encrypted wrapper is invalid.");
        }
        String alg = wrapper.get("alg").getAsString();
        if (!CIPHER_ALGORITHM.equals(alg)) {
            throw new IllegalArgumentException("Encrypted algorithm is invalid.");
        }

        byte[] iv = Base64.getDecoder().decode(wrapper.get("iv").getAsString());
        byte[] cipherText = Base64.getDecoder().decode(wrapper.get("ciphertext").getAsString());

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] payloadBytes = cipher.doFinal(cipherText);
        return new String(payloadBytes, StandardCharsets.UTF_8);
    }

    private void writeAtomicEncryptedBytes(Path target, byte[] bytes) throws IOException {
        writeAtomic(target, bytes);
    }

    private void writeAtomicSafeMeta(Path target, byte[] bytes) throws IOException {
        writeAtomic(target, bytes);
    }

    private void writeAtomic(Path target, byte[] bytes) throws IOException {
        if (target == null || target.getParent() == null) {
            throw new IllegalArgumentException("Target path is invalid.");
        }
        Files.createDirectories(target.getParent());
        Path temp = target.getParent().resolve(target.getFileName().toString() + ".tmp");
        Files.write(temp, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void validateSubmitDryRunKey(SecretKey key) {
        if (key == null || !KEY_ALGORITHM.equalsIgnoreCase(key.getAlgorithm())) {
            throw new IllegalArgumentException("Submit dry-run key is invalid.");
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toLowerHex(digest.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    private static String toLowerHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            String hex = Integer.toHexString(value & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private static String sanitizePathSegment(String value) {
        String sanitized = value.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.length() > 80) {
            return sanitized.substring(0, 80);
        }
        return sanitized.isBlank() ? "debug" : sanitized;
    }

    private static boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private static EncryptedSubmitDryRunException restoreFailure(String errorCode, Throwable cause) {
        return new EncryptedSubmitDryRunException(errorCode, cause);
    }

    private static EncryptedSubmitDryRunException restoreFailure(String errorCode, String message) {
        return new EncryptedSubmitDryRunException(errorCode, message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
