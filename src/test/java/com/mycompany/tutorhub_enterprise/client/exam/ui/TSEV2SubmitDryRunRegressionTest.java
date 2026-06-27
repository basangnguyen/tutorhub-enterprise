package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyExamRenderModel;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyOptionView;
import com.mycompany.tutorhub_enterprise.models.exam.readonly.TSEV2ReadOnlyQuestionView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2SubmitDryRunRegressionTest {

    private Path tempDir;
    private TSEV2ReadOnlyExamRenderModel renderModel;
    private TSEV2AnswerDraftSnapshot snapshot;
    private TSEV2SubmitPayloadService payloadService;
    private TSEV2EncryptedSubmitDryRunService dryRunService;
    private SecretKey ramKey;

    @BeforeEach
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("dryrun_regression_test_");
        payloadService = new TSEV2SubmitPayloadService();
        dryRunService = new TSEV2EncryptedSubmitDryRunService();
        ramKey = TSEV2EncryptedSubmitDryRunService.generateSubmitDryRunKey();

        // 1. Setup Render Model
        renderModel = new TSEV2ReadOnlyExamRenderModel();
        renderModel.setExamId(101);
        renderModel.setPaperId(202);
        renderModel.setPackageHash("package-hash-123");
        renderModel.setQuestionCount(2);

        List<TSEV2ReadOnlyQuestionView> qList = new ArrayList<>();
        TSEV2ReadOnlyQuestionView q1 = new TSEV2ReadOnlyQuestionView();
        q1.setId(1);
        q1.setContent("Q1");
        List<TSEV2ReadOnlyOptionView> opts1 = new ArrayList<>();
        TSEV2ReadOnlyOptionView opt1A = new TSEV2ReadOnlyOptionView();
        opt1A.setId(10); opt1A.setContent("A");
        TSEV2ReadOnlyOptionView opt1B = new TSEV2ReadOnlyOptionView();
        opt1B.setId(11); opt1B.setContent("B");
        opts1.add(opt1A);
        opts1.add(opt1B);
        q1.setOptions(opts1);

        TSEV2ReadOnlyQuestionView q2 = new TSEV2ReadOnlyQuestionView();
        q2.setId(2);
        q2.setContent("Q2");
        List<TSEV2ReadOnlyOptionView> opts2 = new ArrayList<>();
        TSEV2ReadOnlyOptionView opt2A = new TSEV2ReadOnlyOptionView();
        opt2A.setId(20); opt2A.setContent("C");
        TSEV2ReadOnlyOptionView opt2B = new TSEV2ReadOnlyOptionView();
        opt2B.setId(21); opt2B.setContent("D");
        opts2.add(opt2A);
        opts2.add(opt2B);
        q2.setOptions(opts2);

        qList.add(q1);
        qList.add(q2);
        renderModel.setQuestions(qList);

        // 2. Setup Snapshot
        snapshot = new TSEV2AnswerDraftSnapshot();
        snapshot.setExamId(101);
        snapshot.setAttemptId("attempt-999");
        List<TSEV2AnswerDraftItem> draftItems = new ArrayList<>();
        draftItems.add(new TSEV2AnswerDraftItem(1, 11, Instant.now().toString())); // Answered Q1
        snapshot.setAnswers(draftItems);
        snapshot.setAnsweredCount(1);
        snapshot.setQuestionCount(2);
        snapshot.setSnapshotHash("snap-hash");
    }

    @AfterEach
    public void cleanup() throws IOException {
        Files.walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {}
            });
    }

    @Test
    public void testFullRoundTrip() throws Exception {
        // Step 1: Create Payload
        TSEV2SubmitPayload payload = payloadService.createPayload(renderModel, snapshot);
        payloadService.validatePayloadSafe(payload);
        payloadService.validatePayloadMatchesRenderModel(payload, renderModel);

        assertEquals(1, payload.getAnsweredCount());
        assertEquals(1, payload.getUnansweredCount());
        assertFalse(payload.isComplete());

        // Step 2: Encrypt and Save Dry-run
        TSEV2SubmitDryRunMeta meta = dryRunService.saveEncryptedSubmitPayloadDryRun(payload, ramKey, tempDir);
        assertNotNull(meta);

        // Step 3: Verify Files exist
        Path encFile = tempDir.resolve("v2_submit_payload_dryrun.enc");
        Path metaFile = tempDir.resolve("v2_submit_payload_dryrun.meta.json");
        assertTrue(Files.exists(encFile));
        assertTrue(Files.exists(metaFile));

        // Step 4: Verify legacy files DO NOT exist
        assertFalse(Files.exists(tempDir.resolve("submit_payload.enc")));
        assertFalse(Files.exists(tempDir.resolve("autosave_payload.enc")));

        // Step 5: Meta validation checks
        String metaJson = new String(Files.readAllBytes(metaFile));
        assertFalse(metaJson.contains("selectedOptionId"));
        assertFalse(metaJson.contains("answers"));
        assertFalse(metaJson.contains("plaintext"));
        assertFalse(metaJson.contains("sessionToken"));
        
        // Step 6: Load and verify
        TSEV2SubmitPayload loaded = dryRunService.loadEncryptedSubmitPayloadDryRun(encFile, metaFile, ramKey);
        assertEquals(payload.getPayloadHash(), loaded.getPayloadHash());
        assertEquals(payload.getExamId(), loaded.getExamId());
        assertEquals(1, loaded.getAnswers().size());
        assertEquals(11, loaded.getAnswers().get(0).getSelectedOptionId());
    }

    @Test
    public void testWrongKeyFailsSafely() throws Exception {
        TSEV2SubmitPayload payload = payloadService.createPayload(renderModel, snapshot);
        TSEV2SubmitDryRunMeta meta = dryRunService.saveEncryptedSubmitPayloadDryRun(payload, ramKey, tempDir);
        Path encFile = tempDir.resolve("v2_submit_payload_dryrun.enc");
        Path metaFile = tempDir.resolve("v2_submit_payload_dryrun.meta.json");
        
        SecretKey wrongKey = TSEV2EncryptedSubmitDryRunService.generateSubmitDryRunKey();
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            dryRunService.loadEncryptedSubmitPayloadDryRun(encFile, metaFile, wrongKey);
        });
        assertTrue(ex.getMessage().contains("ERROR_SUBMIT_DRYRUN_DECRYPT_FAILED"));
    }

    @Test
    public void testTamperFails() throws Exception {
        TSEV2SubmitPayload payload = payloadService.createPayload(renderModel, snapshot);
        TSEV2SubmitDryRunMeta meta = dryRunService.saveEncryptedSubmitPayloadDryRun(payload, ramKey, tempDir);
        
        Path encFile = tempDir.resolve("v2_submit_payload_dryrun.enc");
        Path metaFile = tempDir.resolve("v2_submit_payload_dryrun.meta.json");
        byte[] tampered = Files.readAllBytes(encFile);
        tampered[tampered.length / 2] ^= 0xFF; // flip bits
        Files.write(encFile, tampered);
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            dryRunService.loadEncryptedSubmitPayloadDryRun(encFile, metaFile, ramKey);
        });
        assertTrue(ex.getMessage().contains("ERROR_SUBMIT_DRYRUN_HASH_MISMATCH"));
    }

    @Test
    public void testUnsafePayloadRejected() throws Exception {
        TSEV2SubmitPayload payload = payloadService.createPayload(renderModel, snapshot);
        payload.setPackageHash("unsafe-token-sessionToken"); // force unsafe word
        payload.setPayloadHash(payloadService.computePayloadHash(payload));
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            dryRunService.saveEncryptedSubmitPayloadDryRun(payload, ramKey, tempDir);
        });
        assertTrue(ex.getMessage().contains("ERROR_SUBMIT_DRYRUN_PAYLOAD_UNSAFE") || ex.getMessage().contains("ERROR_SUBMIT_PAYLOAD_UNSAFE"));
    }
}
