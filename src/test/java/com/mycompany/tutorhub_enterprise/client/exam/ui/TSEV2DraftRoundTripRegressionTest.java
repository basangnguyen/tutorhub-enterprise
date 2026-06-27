package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2DraftRoundTripRegressionTest {

    private TSEV2ReadOnlyExamRenderModel renderModel;
    private TSEV2AnswerSelectionState selectionState;
    private TSEV2AnswerDraftSnapshotService snapshotService;
    private TSEV2LocalEncryptedDraftAutosaveService autosaveService;
    private SecretKey draftKey;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() {
        // 1. Create a dummy read-only render model
        renderModel = new TSEV2ReadOnlyExamRenderModel();
        renderModel.setExamId(101);
        renderModel.setPaperId(202);
        renderModel.setAttemptId("ATTEMPT-123");
        renderModel.setPackageHash("HASH-ABC");
        renderModel.setQuestionCount(2);

        TSEV2ReadOnlyQuestionView q1 = new TSEV2ReadOnlyQuestionView();
        q1.setId(1);
        q1.setContent("Q1");
        TSEV2ReadOnlyOptionView opt1 = new TSEV2ReadOnlyOptionView();
        opt1.setId(10);
        TSEV2ReadOnlyOptionView opt2 = new TSEV2ReadOnlyOptionView();
        opt2.setId(11);
        q1.getOptions().add(opt1);
        q1.getOptions().add(opt2);

        TSEV2ReadOnlyQuestionView q2 = new TSEV2ReadOnlyQuestionView();
        q2.setId(2);
        q2.setContent("Q2");
        TSEV2ReadOnlyOptionView opt3 = new TSEV2ReadOnlyOptionView();
        opt3.setId(20);
        q2.getOptions().add(opt3);

        renderModel.getQuestions().add(q1);
        renderModel.getQuestions().add(q2);

        // 2. Initialize Selection State
        selectionState = new TSEV2AnswerSelectionState(renderModel.getQuestionCount());
        snapshotService = new TSEV2AnswerDraftSnapshotService();
        autosaveService = new TSEV2LocalEncryptedDraftAutosaveService();
        draftKey = TSEV2LocalEncryptedDraftAutosaveService.generateDraftKey();
    }

    @Test
    public void testRoundTrip_SelectionToAutosaveToRestoreToSelection() throws Exception {
        // Step 1: User selects answers
        selectionState.selectOption(1, 11);
        selectionState.selectOption(2, 20);
        
        assertEquals(2, selectionState.getAnsweredCount());

        // Step 2: Snapshot the state
        TSEV2AnswerDraftSnapshot snapshot = snapshotService.createSnapshot(renderModel, selectionState);
        assertNotNull(snapshot);
        assertEquals(2, snapshot.getAnswers().size());

        // Ensure no sensitive data in snapshot JSON
        Gson gson = new Gson();
        String snapshotJson = gson.toJson(snapshot);
        assertFalse(snapshotJson.contains("sessionToken"));
        assertFalse(snapshotJson.contains("answerKey"));
        assertFalse(snapshotJson.contains("isCorrect"));
        assertFalse(snapshotJson.contains("password"));

        // Step 3: Autosave
        autosaveService.saveEncryptedDraft(snapshot, draftKey, tempDir);

        // Verify files exist
        File encFile = new File(tempDir.toFile(), TSEV2LocalEncryptedDraftAutosaveService.ENC_FILE_NAME);
        File metaFile = new File(tempDir.toFile(), TSEV2LocalEncryptedDraftAutosaveService.META_FILE_NAME);
        assertTrue(encFile.exists());
        assertTrue(metaFile.exists());

        // Verify meta contains safe data only
        String metaContent = Files.readString(metaFile.toPath());
        assertFalse(metaContent.contains("answers"));
        assertFalse(metaContent.contains("sessionToken"));
        assertTrue(metaContent.contains("HASH-ABC"));

        // Verify enc is a wrapper and not plain payload JSON
        String encContent = Files.readString(encFile.toPath());
        assertTrue(encContent.startsWith("{"));
        assertTrue(encContent.contains("\"ciphertext\""));
        assertFalse(encContent.contains("answers"));

        // Step 4: Restore
        Optional<TSEV2AnswerDraftSnapshot> restoredOpt = autosaveService.tryLoadEncryptedDraft(encFile.toPath(), metaFile.toPath(), draftKey);
        assertTrue(restoredOpt.isPresent(), "Restore should succeed with valid context and key");
        TSEV2AnswerDraftSnapshot restoredSnapshot = restoredOpt.get();

        // Step 5: Apply to new RAM state
        TSEV2AnswerSelectionState newSelectionState = new TSEV2AnswerSelectionState(renderModel.getQuestionCount());
        autosaveService.applySnapshotToSelectionState(restoredSnapshot, renderModel, newSelectionState);

        // Step 6: Verify restored state matches
        assertEquals(2, newSelectionState.getAnsweredCount());
        assertEquals(11, newSelectionState.getSelectedOption(1).get());
        assertEquals(20, newSelectionState.getSelectedOption(2).get());
    }
}
