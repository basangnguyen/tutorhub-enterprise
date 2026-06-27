package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2LocalEncryptedDraftAutosaveServiceTest {

    @TempDir
    Path tempDir;

    private final TSEV2AnswerDraftSnapshotService snapshotService =
            new TSEV2AnswerDraftSnapshotService(() -> Instant.parse("2026-06-19T00:00:00Z"));
    private final TSEV2LocalEncryptedDraftAutosaveService autosaveService =
            new TSEV2LocalEncryptedDraftAutosaveService(snapshotService, null);

    @Test
    public void saveEncryptedDraftCreatesEncryptedAndMetaFiles() throws Exception {
        TSEV2AnswerDraftSnapshot snapshot = createOneAnswerSnapshot();
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();

        TSEV2DraftAutosaveMeta meta = autosaveService.saveEncryptedDraft(snapshot, draftKey, tempDir);

        assertTrue(Files.exists(tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.ENC_FILE_NAME)));
        assertTrue(Files.exists(tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.META_FILE_NAME)));
        assertEquals(TSEV2LocalEncryptedDraftAutosaveService.SCHEMA_VERSION, meta.getSchemaVersion());
        assertEquals(TSEV2AnswerDraftSnapshotService.FLOW, meta.getFlow());
        assertEquals(snapshot.getSnapshotHash(), meta.getSnapshotHash());
        assertEquals(1, meta.getAnsweredCount());
    }

    @Test
    public void encryptedFileDoesNotContainReadableAnswerJson() throws Exception {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();

        autosaveService.saveEncryptedDraft(createOneAnswerSnapshot(), draftKey, tempDir);

        String encryptedText = Files.readString(
                tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.ENC_FILE_NAME),
                StandardCharsets.UTF_8
        ).toLowerCase();

        assertFalse(encryptedText.contains("questionid"));
        assertFalse(encryptedText.contains("selectedoptionid"));
        assertFalse(encryptedText.contains("\"answers\""));
    }

    @Test
    public void metaJsonContainsOnlySafeMetadata() throws Exception {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();

        autosaveService.saveEncryptedDraft(createOneAnswerSnapshot(), draftKey, tempDir);

        String metaJson = Files.readString(
                tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.META_FILE_NAME),
                StandardCharsets.UTF_8
        ).toLowerCase();

        assertTrue(metaJson.contains("schema"));
        assertTrue(metaJson.contains("snapshot"));
        assertFalse(metaJson.contains("\"answers\""));
        assertFalse(metaJson.contains("selectedoptionid"));
        assertFalse(metaJson.contains("sessiontoken"));
        assertFalse(metaJson.contains("keyb64"));
        assertFalse(metaJson.contains("aeskey"));
        assertFalse(metaJson.contains("secretkey"));
        assertFalse(metaJson.contains("rawkey"));
        assertFalse(metaJson.contains("plaintext"));
        assertFalse(metaJson.contains("iscorrect"));
        assertFalse(metaJson.contains("answerkey"));
        assertFalse(metaJson.contains("correctoption"));
        assertFalse(metaJson.contains("passwordhash"));
        assertFalse(metaJson.contains("password"));
        assertFalse(metaJson.contains("score"));
    }

    @Test
    public void loadEncryptedDraftWithCorrectKeyRestoresSnapshotHash() {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        TSEV2AnswerDraftSnapshot snapshot = createOneAnswerSnapshot();
        autosaveService.saveEncryptedDraft(snapshot, draftKey, tempDir);

        TSEV2AnswerDraftSnapshot restored = autosaveService.loadEncryptedDraft(
                tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.ENC_FILE_NAME),
                tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.META_FILE_NAME),
                draftKey
        );

        assertEquals(snapshot.getSnapshotHash(), restored.getSnapshotHash());
        assertEquals(snapshot.getAnsweredCount(), restored.getAnsweredCount());
        assertEquals(snapshot.getAnswers().get(0).getQuestionId(), restored.getAnswers().get(0).getQuestionId());
        assertEquals(
                snapshot.getAnswers().get(0).getSelectedOptionId(),
                restored.getAnswers().get(0).getSelectedOptionId()
        );
    }

    @Test
    public void loadEncryptedDraftWithWrongKeyFailsSafely() {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        SecretKey wrongKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        autosaveService.saveEncryptedDraft(createOneAnswerSnapshot(), draftKey, tempDir);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> autosaveService.loadEncryptedDraft(
                        tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.ENC_FILE_NAME),
                        tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.META_FILE_NAME),
                        wrongKey
                )
        );

        assertEquals("Encrypted draft cannot be loaded safely.", ex.getMessage());
    }

    @Test
    public void tamperedEncryptedFileFailsBeforeRestore() throws Exception {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        autosaveService.saveEncryptedDraft(createOneAnswerSnapshot(), draftKey, tempDir);
        Path encPath = tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.ENC_FILE_NAME);
        byte[] bytes = Files.readAllBytes(encPath);
        bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] ^ 1);
        Files.write(encPath, bytes);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> autosaveService.loadEncryptedDraft(
                        encPath,
                        tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.META_FILE_NAME),
                        draftKey
                )
        );

        assertEquals("Encrypted draft hash mismatch.", ex.getMessage());
    }

    @Test
    public void invalidSelectedOptionIsRejectedBeforeAutosave() {
        TSEV2AnswerSelectionState state = new TSEV2AnswerSelectionState(2);
        state.selectOption(101, 2001);

        assertThrows(
                IllegalArgumentException.class,
                () -> snapshotService.createSnapshot(createSafeModel(), state)
        );
        assertFalse(Files.exists(tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.ENC_FILE_NAME)));
        assertFalse(Files.exists(tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.META_FILE_NAME)));
    }

    @Test
    public void autosaveFilesDoNotContainNetworkSubmitOrBackendMarkers() throws Exception {
        SecretKey draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
        autosaveService.saveEncryptedDraft(createOneAnswerSnapshot(), draftKey, tempDir);

        String combined = (
                Files.readString(tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.ENC_FILE_NAME))
                        + Files.readString(tempDir.resolve(TSEV2LocalEncryptedDraftAutosaveService.META_FILE_NAME))
        ).toLowerCase();

        assertFalse(combined.contains("exam_submit"));
        assertFalse(combined.contains("submit_payload"));
        assertFalse(combined.contains("httpclient"));
        assertFalse(combined.contains("socket"));
        assertFalse(combined.contains("backend"));
    }

    @Test
    public void defaultAutosaveDirectoryUsesSafeDebugAttemptSegment() {
        TSEV2ReadOnlyExamRenderModel model = createSafeModel();
        model.setAttemptId("attempt debug/unsafe:segment");

        Path resolved = autosaveService.resolveDefaultAutosaveDir(model);

        String pathText = resolved.toString();
        assertTrue(pathText.contains(".tutorhub-secure-exam"));
        assertTrue(pathText.contains("v2-drafts"));
        assertTrue(pathText.endsWith("attempt_debug_unsafe_segment"));
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
}
