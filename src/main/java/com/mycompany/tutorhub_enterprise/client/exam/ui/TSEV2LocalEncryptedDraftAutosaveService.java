package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;

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
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TSEV2LocalEncryptedDraftAutosaveService {

    public static final String SCHEMA_VERSION = "tse_v2_local_draft_autosave_meta_v1";
    public static final String ENC_FILE_NAME = "v2_answer_draft_autosave.enc";
    public static final String META_FILE_NAME = "v2_answer_draft_autosave.meta.json";
    public static final String ERROR_DRAFT_CONTEXT_MISMATCH = "ERROR_DRAFT_CONTEXT_MISMATCH";
    public static final String ERROR_DRAFT_DECRYPT_FAILED = "ERROR_DRAFT_DECRYPT_FAILED";
    public static final String ERROR_DRAFT_HASH_MISMATCH = "ERROR_DRAFT_HASH_MISMATCH";
    public static final String ERROR_DRAFT_META_UNSAFE = "ERROR_DRAFT_META_UNSAFE";
    public static final String ERROR_DRAFT_PAYLOAD_UNSAFE = "ERROR_DRAFT_PAYLOAD_UNSAFE";

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
            "score"
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
            "exam_submit",
            "submit_payload"
    };

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final TSEV2AnswerDraftSnapshotService snapshotService;
    private final SecureRandom secureRandom;

    public static class DraftRestoreException extends RuntimeException {
        private final String errorCode;

        public DraftRestoreException(String errorCode, Throwable cause) {
            super(errorCode, cause);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public TSEV2LocalEncryptedDraftAutosaveService() {
        this(new TSEV2AnswerDraftSnapshotService(), new SecureRandom());
    }

    public TSEV2LocalEncryptedDraftAutosaveService(
            TSEV2AnswerDraftSnapshotService snapshotService,
            SecureRandom secureRandom
    ) {
        this.snapshotService = snapshotService == null
                ? new TSEV2AnswerDraftSnapshotService()
                : snapshotService;
        this.secureRandom = secureRandom == null ? new SecureRandom() : secureRandom;
    }

    public static SecretKey generateDraftKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (Exception ex) {
            throw new IllegalStateException("Draft key generation failed.", ex);
        }
    }

    public TSEV2DraftAutosaveMeta saveEncryptedDraft(
            TSEV2AnswerDraftSnapshot snapshot,
            SecretKey draftKey,
            Path autosaveDir
    ) {
        try {
            if (autosaveDir == null) {
                throw new IllegalArgumentException("Autosave directory is missing.");
            }
            validateDraftKey(draftKey);
            snapshotService.validateSnapshotSafe(snapshot);

            String payloadJson = snapshotService.toJson(snapshot);
            validateAutosavePayloadSafe(payloadJson);
            byte[] encryptedBytes = encryptPayload(payloadJson, draftKey);
            String encryptedSha256 = sha256Hex(encryptedBytes);

            TSEV2DraftAutosaveMeta meta = createMeta(snapshot, encryptedSha256);
            validateMetaSafe(meta);
            String metaJson = gson.toJson(meta);
            validateMetaJsonSafe(metaJson);

            Files.createDirectories(autosaveDir);
            writeAtomicEncryptedBytes(autosaveDir.resolve(ENC_FILE_NAME), encryptedBytes);
            writeAtomicSafeMeta(autosaveDir.resolve(META_FILE_NAME), metaJson.getBytes(StandardCharsets.UTF_8));
            return meta;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Encrypted draft autosave failed.", ex);
        }
    }

    public TSEV2AnswerDraftSnapshot loadEncryptedDraft(
            Path encPath,
            Path metaPath,
            SecretKey draftKey
    ) {
        try {
            validateDraftKey(draftKey);
            if (encPath == null || metaPath == null) {
                throw new IllegalArgumentException("Autosave paths are missing.");
            }

            String metaJson = Files.readString(metaPath, StandardCharsets.UTF_8);
            validateMetaJsonSafe(metaJson);
            TSEV2DraftAutosaveMeta meta = gson.fromJson(metaJson, TSEV2DraftAutosaveMeta.class);
            validateMetaSafe(meta);

            byte[] encryptedBytes = Files.readAllBytes(encPath);
            String actualHash = sha256Hex(encryptedBytes);
            if (!actualHash.equals(meta.getEncryptedFileSha256())) {
                throw new IllegalArgumentException("Encrypted draft hash mismatch.");
            }

            String payloadJson = decryptPayload(encryptedBytes, draftKey);
            validateAutosavePayloadSafe(payloadJson);
            TSEV2AnswerDraftSnapshot snapshot = gson.fromJson(payloadJson, TSEV2AnswerDraftSnapshot.class);
            snapshotService.validateSnapshotSafe(snapshot);
            validateMetaMatchesSnapshot(meta, snapshot);
            return snapshot;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Encrypted draft cannot be loaded safely.");
        }
    }

    public Optional<TSEV2AnswerDraftSnapshot> tryLoadEncryptedDraft(
            Path encPath,
            Path metaPath,
            SecretKey draftKey
    ) {
        if (encPath == null || metaPath == null || !Files.exists(encPath) || !Files.exists(metaPath)) {
            return Optional.empty();
        }

        try {
            validateDraftKey(draftKey);
        } catch (RuntimeException ex) {
            throw restoreFailure(ERROR_DRAFT_DECRYPT_FAILED, ex);
        }

        TSEV2DraftAutosaveMeta meta;
        try {
            String metaJson = Files.readString(metaPath, StandardCharsets.UTF_8);
            validateMetaJsonSafe(metaJson);
            meta = gson.fromJson(metaJson, TSEV2DraftAutosaveMeta.class);
            validateMetaSafe(meta);
        } catch (Exception ex) {
            throw restoreFailure(ERROR_DRAFT_META_UNSAFE, ex);
        }

        byte[] encryptedBytes;
        try {
            encryptedBytes = Files.readAllBytes(encPath);
            String actualHash = sha256Hex(encryptedBytes);
            if (!actualHash.equals(meta.getEncryptedFileSha256())) {
                throw new IllegalArgumentException("Encrypted draft hash mismatch.");
            }
        } catch (Exception ex) {
            throw restoreFailure(ERROR_DRAFT_HASH_MISMATCH, ex);
        }

        String payloadJson;
        try {
            payloadJson = decryptPayload(encryptedBytes, draftKey);
        } catch (Exception ex) {
            throw restoreFailure(ERROR_DRAFT_DECRYPT_FAILED, ex);
        }

        try {
            validateAutosavePayloadSafe(payloadJson);
            TSEV2AnswerDraftSnapshot snapshot = gson.fromJson(payloadJson, TSEV2AnswerDraftSnapshot.class);
            snapshotService.validateSnapshotSafe(snapshot);
            validateMetaMatchesSnapshot(meta, snapshot);
            return Optional.of(snapshot);
        } catch (Exception ex) {
            throw restoreFailure(ERROR_DRAFT_PAYLOAD_UNSAFE, ex);
        }
    }

    public void validateDraftMatchesRenderModel(
            TSEV2AnswerDraftSnapshot snapshot,
            TSEV2ReadOnlyExamRenderModel model
    ) {
        try {
            snapshotService.validateSnapshotSafe(snapshot);
            if (model == null) {
                throw new IllegalArgumentException("Render model is missing.");
            }
            List<TSEV2ReadOnlyQuestionView> questions = model.getQuestions();
            if (questions == null) {
                throw new IllegalArgumentException("Render model questions are missing.");
            }
            if (snapshot.getExamId() != model.getExamId()
                    || snapshot.getPaperId() != model.getPaperId()
                    || snapshot.getQuestionCount() != model.getQuestionCount()
                    || snapshot.getQuestionCount() != questions.size()) {
                throw new IllegalArgumentException("Draft context does not match render model.");
            }
            if (hasValue(snapshot.getAttemptId()) && hasValue(model.getAttemptId())
                    && !snapshot.getAttemptId().equals(model.getAttemptId())) {
                throw new IllegalArgumentException("Draft attemptId does not match render model.");
            }
            if (hasValue(snapshot.getPackageHash()) && hasValue(model.getPackageHash())
                    && !snapshot.getPackageHash().equals(model.getPackageHash())) {
                throw new IllegalArgumentException("Draft packageHash does not match render model.");
            }

            Map<Integer, Set<Integer>> allowedOptionIdsByQuestion = new HashMap<>();
            for (TSEV2ReadOnlyQuestionView question : questions) {
                if (question == null || question.getId() <= 0 || question.getOptions() == null) {
                    throw new IllegalArgumentException("Render model question is invalid.");
                }
                Set<Integer> optionIds = new HashSet<>();
                for (TSEV2ReadOnlyOptionView option : question.getOptions()) {
                    if (option == null || option.getId() <= 0) {
                        throw new IllegalArgumentException("Render model option is invalid.");
                    }
                    optionIds.add(option.getId());
                }
                allowedOptionIdsByQuestion.put(question.getId(), optionIds);
            }

            for (TSEV2AnswerDraftItem answer : snapshot.getAnswers()) {
                Set<Integer> optionIds = allowedOptionIdsByQuestion.get(answer.getQuestionId());
                if (optionIds == null || !optionIds.contains(answer.getSelectedOptionId())) {
                    throw new IllegalArgumentException("Draft selected option does not belong to the question.");
                }
            }
        } catch (DraftRestoreException ex) {
            throw ex;
        } catch (Exception ex) {
            throw restoreFailure(ERROR_DRAFT_CONTEXT_MISMATCH, ex);
        }
    }

    public void applySnapshotToSelectionState(
            TSEV2AnswerDraftSnapshot snapshot,
            TSEV2ReadOnlyExamRenderModel model,
            TSEV2AnswerSelectionState state
    ) {
        if (state == null) {
            throw restoreFailure(
                    ERROR_DRAFT_CONTEXT_MISMATCH,
                    new IllegalArgumentException("Selection state is missing.")
            );
        }
        validateDraftMatchesRenderModel(snapshot, model);
        state.clearAllSelections();
        for (TSEV2AnswerDraftItem answer : snapshot.getAnswers()) {
            state.selectOption(answer.getQuestionId(), answer.getSelectedOptionId());
        }
    }

    public void validateMetaSafe(TSEV2DraftAutosaveMeta meta) {
        if (meta == null) {
            throw new IllegalArgumentException("Autosave meta is missing.");
        }
        if (!SCHEMA_VERSION.equals(meta.getSchemaVersion())) {
            throw new IllegalArgumentException("Autosave meta schemaVersion is invalid.");
        }
        if (!TSEV2AnswerDraftSnapshotService.FLOW.equals(meta.getFlow())) {
            throw new IllegalArgumentException("Autosave meta flow is invalid.");
        }
        if (meta.getExamId() <= 0 || meta.getPaperId() <= 0) {
            throw new IllegalArgumentException("Autosave meta exam identifiers are invalid.");
        }
        if (meta.getQuestionCount() < 0 || meta.getAnsweredCount() < 0) {
            throw new IllegalArgumentException("Autosave meta counts are invalid.");
        }
        if (meta.getAnsweredCount() > meta.getQuestionCount()) {
            throw new IllegalArgumentException("Autosave meta answeredCount exceeds questionCount.");
        }
        if (isBlank(meta.getSnapshotHash()) || isBlank(meta.getEncryptedFileSha256())) {
            throw new IllegalArgumentException("Autosave meta hashes are missing.");
        }
        if (!ENC_FILE_NAME.equals(meta.getEncFileName())) {
            throw new IllegalArgumentException("Autosave meta encrypted file name is invalid.");
        }
        if (isBlank(meta.getCreatedAt()) || isBlank(meta.getUpdatedAt())) {
            throw new IllegalArgumentException("Autosave meta timestamps are missing.");
        }
        validateMetaJsonSafe(gson.toJson(meta));
    }

    public void validateAutosavePayloadSafe(String json) {
        if (isBlank(json)) {
            throw new IllegalArgumentException("Autosave payload is missing.");
        }
        String lower = json.toLowerCase();
        for (String token : FORBIDDEN_PAYLOAD_TOKENS) {
            if (lower.contains(token)) {
                throw new IllegalArgumentException("Autosave payload contains a blocked marker.");
            }
        }
    }

    public Path resolveDefaultAutosaveDir(TSEV2ReadOnlyExamRenderModel model) {
        String attemptSegment = "debug";
        if (model != null && !isBlank(model.getAttemptId())) {
            attemptSegment = sanitizePathSegment(model.getAttemptId());
        }
        return Path.of(
                System.getProperty("user.home"),
                ".tutorhub-secure-exam",
                "debug",
                "v2-drafts",
                attemptSegment
        );
    }

    private TSEV2DraftAutosaveMeta createMeta(TSEV2AnswerDraftSnapshot snapshot, String encryptedSha256) {
        TSEV2DraftAutosaveMeta meta = new TSEV2DraftAutosaveMeta();
        meta.setSchemaVersion(SCHEMA_VERSION);
        meta.setFlow(snapshot.getFlow());
        meta.setExamId(snapshot.getExamId());
        meta.setPaperId(snapshot.getPaperId());
        meta.setAttemptId(snapshot.getAttemptId());
        meta.setPackageHash(snapshot.getPackageHash());
        meta.setQuestionCount(snapshot.getQuestionCount());
        meta.setAnsweredCount(snapshot.getAnsweredCount());
        meta.setSnapshotHash(snapshot.getSnapshotHash());
        meta.setEncryptedFileSha256(encryptedSha256);
        meta.setEncFileName(ENC_FILE_NAME);
        meta.setCreatedAt(snapshot.getCreatedAt());
        meta.setUpdatedAt(snapshot.getUpdatedAt());
        return meta;
    }

    private byte[] encryptPayload(String payloadJson, SecretKey draftKey) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, draftKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] cipherText = cipher.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("alg", CIPHER_ALGORITHM);
        wrapper.addProperty("iv", Base64.getEncoder().encodeToString(iv));
        wrapper.addProperty("ciphertext", Base64.getEncoder().encodeToString(cipherText));
        return gson.toJson(wrapper).getBytes(StandardCharsets.UTF_8);
    }

    private String decryptPayload(byte[] encryptedBytes, SecretKey draftKey) throws Exception {
        JsonObject wrapper = gson.fromJson(new String(encryptedBytes, StandardCharsets.UTF_8), JsonObject.class);
        if (wrapper == null || !wrapper.has("alg") || !wrapper.has("iv") || !wrapper.has("ciphertext")) {
            throw new IllegalArgumentException("Encrypted draft wrapper is invalid.");
        }
        String alg = wrapper.get("alg").getAsString();
        if (!CIPHER_ALGORITHM.equals(alg)) {
            throw new IllegalArgumentException("Encrypted draft algorithm is invalid.");
        }

        byte[] iv = Base64.getDecoder().decode(wrapper.get("iv").getAsString());
        byte[] cipherText = Base64.getDecoder().decode(wrapper.get("ciphertext").getAsString());

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, draftKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] payloadBytes = cipher.doFinal(cipherText);
        return new String(payloadBytes, StandardCharsets.UTF_8);
    }

    private void validateMetaMatchesSnapshot(TSEV2DraftAutosaveMeta meta, TSEV2AnswerDraftSnapshot snapshot) {
        if (meta.getExamId() != snapshot.getExamId()
                || meta.getPaperId() != snapshot.getPaperId()
                || meta.getQuestionCount() != snapshot.getQuestionCount()
                || meta.getAnsweredCount() != snapshot.getAnsweredCount()
                || !safeEquals(meta.getAttemptId(), snapshot.getAttemptId())
                || !safeEquals(meta.getPackageHash(), snapshot.getPackageHash())
                || !safeEquals(meta.getSnapshotHash(), snapshot.getSnapshotHash())) {
            throw new IllegalArgumentException("Autosave meta does not match snapshot.");
        }
    }

    private void validateMetaJsonSafe(String metaJson) {
        if (isBlank(metaJson)) {
            throw new IllegalArgumentException("Autosave meta JSON is missing.");
        }
        String lower = metaJson.toLowerCase();
        for (String token : FORBIDDEN_META_TOKENS) {
            if (lower.contains(token)) {
                throw new IllegalArgumentException("Autosave meta contains a blocked marker.");
            }
        }
    }

    private void writeAtomicEncryptedBytes(Path target, byte[] bytes) throws IOException {
        writeAtomic(target, bytes);
    }

    private void writeAtomicSafeMeta(Path target, byte[] bytes) throws IOException {
        writeAtomic(target, bytes);
    }

    private void writeAtomic(Path target, byte[] bytes) throws IOException {
        if (target == null || target.getParent() == null) {
            throw new IllegalArgumentException("Autosave target path is invalid.");
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

    private static void validateDraftKey(SecretKey draftKey) {
        if (draftKey == null || !KEY_ALGORITHM.equalsIgnoreCase(draftKey.getAlgorithm())) {
            throw new IllegalArgumentException("Draft key is invalid.");
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

    private static DraftRestoreException restoreFailure(String errorCode, Throwable cause) {
        return new DraftRestoreException(errorCode, cause);
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
