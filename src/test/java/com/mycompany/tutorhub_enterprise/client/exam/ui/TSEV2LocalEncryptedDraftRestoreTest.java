package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2LocalEncryptedDraftRestoreTest {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    @TempDir
    Path tempDir;

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final TSEV2AnswerDraftSnapshotService snapshotService =
            new TSEV2AnswerDraftSnapshotService(() -> Instant.parse("2026-06-19T00:00:00Z"));
    private final TSEV2LocalEncryptedDraftAutosaveService restoreService =
            new TSEV2LocalEncryptedDraftAutosaveService(snapshotService, new SecureRandom());

    @Test
    public void saveThenRestoreWithCorrectKeyPasses() {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        TSEV2AnswerDraftSnapshot snapshot = createOneAnswerSnapshot();
        restoreService.saveEncryptedDraft(snapshot, draftKey, tempDir);

        Optional<TSEV2AnswerDraftSnapshot> restored = restoreService.tryLoadEncryptedDraft(
                encPath(),
                metaPath(),
                draftKey
        );

        assertTrue(restored.isPresent());
        assertEquals(snapshot.getSnapshotHash(), restored.get().getSnapshotHash());
        assertEquals(1002, restored.get().getAnswers().get(0).getSelectedOptionId());
    }

    @Test
    public void restoreApplyIntoSelectionStateSetsAnsweredCount() {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        restoreService.saveEncryptedDraft(createOneAnswerSnapshot(), draftKey, tempDir);
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);

        TSEV2AnswerDraftSnapshot restored = restoreService.tryLoadEncryptedDraft(
                encPath(),
                metaPath(),
                draftKey
        ).orElseThrow();
        restoreService.applySnapshotToSelectionState(restored, createSafeModel(), state);

        assertEquals(1, state.getAnsweredCount());
        assertEquals(1002, state.getSelectedOption(101).orElseThrow());
    }

    @Test
    public void panelStateAfterRestoreReflectsSelectedOption() {
        TSEV2ReadOnlyExamPanel panel = new TSEV2ReadOnlyExamPanel(
                createSafeModel(),
                new TSEV2AnswerSelectionState(2),
                null,
                (model, state) -> {
                    state.selectOption(101, 1002);
                    return TSEV2ReadOnlyExamPanel.DraftRestoreResult.restored();
                }
        );

        JRadioButton restoredRadio = findRadioContaining(panel, "Safe option B");

        assertNotNull(restoredRadio);
        assertTrue(restoredRadio.isSelected());
        assertTrue(panel.renderSafePanelText().contains("Encrypted draft restored."));
        assertTrue(panel.renderSafePanelText().contains("Answered 1 / 2"));
    }

    @Test
    public void wrongKeyFailsSafely() {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        SecretKey wrongKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        restoreService.saveEncryptedDraft(createOneAnswerSnapshot(), draftKey, tempDir);

        TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException ex = assertThrows(
                TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException.class,
                () -> restoreService.tryLoadEncryptedDraft(encPath(), metaPath(), wrongKey)
        );

        assertEquals(TSEV2LocalEncryptedDraftAutosaveService.ERROR_DRAFT_DECRYPT_FAILED, ex.getErrorCode());
    }

    @Test
    public void tamperedEncryptedFileFailsSafely() throws Exception {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        restoreService.saveEncryptedDraft(createOneAnswerSnapshot(), draftKey, tempDir);
        byte[] bytes = Files.readAllBytes(encPath());
        bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] ^ 1);
        Files.write(encPath(), bytes);

        TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException ex = assertThrows(
                TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException.class,
                () -> restoreService.tryLoadEncryptedDraft(encPath(), metaPath(), draftKey)
        );

        assertEquals(TSEV2LocalEncryptedDraftAutosaveService.ERROR_DRAFT_HASH_MISMATCH, ex.getErrorCode());
    }

    @Test
    public void metaWithAnswersOrSelectedOptionIdIsRejected() throws Exception {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        restoreService.saveEncryptedDraft(createOneAnswerSnapshot(), draftKey, tempDir);
        String metaJson = Files.readString(metaPath(), StandardCharsets.UTF_8);
        String unsafeMeta = metaJson.substring(0, metaJson.length() - 1)
                + ",\"answers\":[],\"selectedOptionId\":1002}";
        Files.write(metaPath(), unsafeMeta.getBytes(StandardCharsets.UTF_8));

        TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException ex = assertThrows(
                TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException.class,
                () -> restoreService.tryLoadEncryptedDraft(encPath(), metaPath(), draftKey)
        );

        assertEquals(TSEV2LocalEncryptedDraftAutosaveService.ERROR_DRAFT_META_UNSAFE, ex.getErrorCode());
    }

    @Test
    public void payloadWithSessionTokenIsRejected() throws Exception {
        assertUnsafePayloadRejected("sessionToken");
    }

    @Test
    public void payloadWithIsCorrectIsRejected() throws Exception {
        assertUnsafePayloadRejected("isCorrect");
    }

    @Test
    public void payloadWithAnswerKeyOrCorrectOptionIsRejected() throws Exception {
        assertUnsafePayloadRejected("answerKey", "correctOption");
    }

    @Test
    public void snapshotPackageHashMismatchIsRejected() {
        TSEV2AnswerDraftSnapshot snapshot = createOneAnswerSnapshot();
        TSEV2ReadOnlyExamRenderModel mismatchedModel = createSafeModel();
        mismatchedModel.setPackageHash("different-package-hash");
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);

        TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException ex = assertThrows(
                TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException.class,
                () -> restoreService.applySnapshotToSelectionState(snapshot, mismatchedModel, state)
        );

        assertEquals(TSEV2LocalEncryptedDraftAutosaveService.ERROR_DRAFT_CONTEXT_MISMATCH, ex.getErrorCode());
        assertEquals(0, state.getAnsweredCount());
    }

    @Test
    public void selectedOptionIdNotBelongingToQuestionIsRejected() {
        TSEV2AnswerDraftSnapshot snapshot = createOneAnswerSnapshot();
        snapshot.getAnswers().get(0).setSelectedOptionId(2001);
        snapshot.setSnapshotHash(snapshotService.computeSnapshotHash(snapshot));
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);

        TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException ex = assertThrows(
                TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException.class,
                () -> restoreService.applySnapshotToSelectionState(snapshot, createSafeModel(), state)
        );

        assertEquals(TSEV2LocalEncryptedDraftAutosaveService.ERROR_DRAFT_CONTEXT_MISMATCH, ex.getErrorCode());
        assertEquals(0, state.getAnsweredCount());
    }

    @Test
    public void restoreDoesNotCreateSubmitPayloadAutosavePayloadOrNetworkMarkers() throws Exception {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        restoreService.saveEncryptedDraft(createOneAnswerSnapshot(), draftKey, tempDir);
        restoreService.tryLoadEncryptedDraft(encPath(), metaPath(), draftKey).orElseThrow();

        assertFalse(Files.exists(tempDir.resolve("submit_payload.enc")));
        assertFalse(Files.exists(tempDir.resolve("autosave_payload.enc")));
        assertFalse(Files.exists(tempDir.resolve("EXAM_SUBMIT")));

        String combined = Files.readString(metaPath(), StandardCharsets.UTF_8)
                + Files.readString(encPath(), StandardCharsets.UTF_8);
        String lower = combined.toLowerCase();
        assertFalse(lower.contains("exam_submit"));
        assertFalse(lower.contains("submit_payload"));
        assertFalse(lower.contains("httpclient"));
        assertFalse(lower.contains("socket"));
    }

    private void assertUnsafePayloadRejected(String... markers) throws Exception {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        TSEV2AnswerDraftSnapshot snapshot = createOneAnswerSnapshot();
        TSEV2DraftAutosaveMeta meta = restoreService.saveEncryptedDraft(snapshot, draftKey, tempDir);

        JsonObject unsafePayload = gson.fromJson(snapshotService.toJson(snapshot), JsonObject.class);
        for (String marker : markers) {
            unsafePayload.addProperty(marker, "blocked");
        }
        byte[] encryptedBytes = encryptPayload(gson.toJson(unsafePayload), draftKey);
        meta.setEncryptedFileSha256(sha256Hex(encryptedBytes));
        Files.write(encPath(), encryptedBytes);
        Files.write(metaPath(), gson.toJson(meta).getBytes(StandardCharsets.UTF_8));

        TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException ex = assertThrows(
                TSEV2LocalEncryptedDraftAutosaveService.DraftRestoreException.class,
                () -> restoreService.tryLoadEncryptedDraft(encPath(), metaPath(), draftKey)
        );

        assertEquals(TSEV2LocalEncryptedDraftAutosaveService.ERROR_DRAFT_PAYLOAD_UNSAFE, ex.getErrorCode());
    }

    private TSEV2AnswerDraftSnapshot createOneAnswerSnapshot() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);
        state.selectOption(101, 1002);
        return snapshotService.createSnapshot(createSafeModel(), state);
    }

    private static TSEV2ReadOnlyExamRenderModel createSafeModel() {
        TSEV2ReadOnlyExamRenderModel model = new TSEV2ReadOnlyExamRenderModel();
        model.setExamId(7);
        model.setPaperId(8);
        model.setAttemptId("attempt-debug-id");
        model.setPackageHash("package-hash-123");
        model.setQuestionCount(2);
        model.setQuestions(List.of(
                question(101, "Safe question one", List.of(
                        option(1001, "Safe option A"),
                        option(1002, "Safe option B")
                )),
                question(102, "Safe question two", List.of(
                        option(2001, "True"),
                        option(2002, "False")
                ))
        ));
        return model;
    }

    private static TSEV2ReadOnlyQuestionView question(
            int id,
            String content,
            List<TSEV2ReadOnlyOptionView> options
    ) {
        TSEV2ReadOnlyQuestionView question = new TSEV2ReadOnlyQuestionView();
        question.setId(id);
        question.setContent(content);
        question.setOptions(options);
        return question;
    }

    private static TSEV2ReadOnlyOptionView option(int id, String content) {
        TSEV2ReadOnlyOptionView option = new TSEV2ReadOnlyOptionView();
        option.setId(id);
        option.setContent(content);
        return option;
    }

    private Path encPath() {
        return tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.ENC_FILE_NAME);
    }

    private Path metaPath() {
        return tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.META_FILE_NAME);
    }

    private byte[] encryptPayload(String payloadJson, SecretKey draftKey) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, draftKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] cipherText = cipher.doFinal(payloadJson.getBytes(StandardCharsets.UTF_8));

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("alg", CIPHER_ALGORITHM);
        wrapper.addProperty("iv", Base64.getEncoder().encodeToString(iv));
        wrapper.addProperty("ciphertext", Base64.getEncoder().encodeToString(cipherText));
        return gson.toJson(wrapper).getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            String hex = Integer.toHexString(value & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    private static JRadioButton findRadioContaining(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JRadioButton
                    && ((JRadioButton) component).getText().contains(text)) {
                return (JRadioButton) component;
            }
            if (component instanceof Container) {
                JRadioButton found = findRadioContaining((Container) component, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
