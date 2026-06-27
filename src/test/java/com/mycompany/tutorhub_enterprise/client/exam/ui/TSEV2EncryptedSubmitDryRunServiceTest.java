package com.mycompany.tutorhub_enterprise.client.exam.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TSEV2EncryptedSubmitDryRunServiceTest {

    private TSEV2EncryptedSubmitDryRunService service;
    private SecretKey validKey;
    private Path tempDir;
    private TSEV2SubmitPayload testPayload;

    @BeforeEach
    public void setup() throws IOException {
        service = new TSEV2EncryptedSubmitDryRunService();
        validKey = TSEV2EncryptedSubmitDryRunService.generateSubmitDryRunKey();
        tempDir = Files.createTempDirectory("dryrun_test_" + UUID.randomUUID());

        testPayload = new TSEV2SubmitPayload();
        testPayload.setPayloadVersion("1.0");
        testPayload.setFlow("PAPER_START_V2");
        testPayload.setExamId(100);
        testPayload.setPaperId(200);
        testPayload.setAttemptId("attempt-123");
        testPayload.setPackageHash("abcdef");
        testPayload.setQuestionCount(3);
        testPayload.setAnsweredCount(2);
        testPayload.setUnansweredCount(1);
        testPayload.setComplete(false);
        testPayload.setDraftSnapshotHash("hash1");
        testPayload.setPayloadHash("dummyhash");
        testPayload.setPreparedAt("2026-06-19T00:00:00Z");

        List<TSEV2SubmitAnswerItem> answers = new ArrayList<>();
        TSEV2SubmitAnswerItem item1 = new TSEV2SubmitAnswerItem();
        item1.setQuestionId(1);
        item1.setSelectedOptionId(11);
        item1.setAnsweredAt("2026-06-19T00:01:00Z");
        answers.add(item1);

        TSEV2SubmitAnswerItem item2 = new TSEV2SubmitAnswerItem();
        item2.setQuestionId(2);
        item2.setSelectedOptionId(22);
        item2.setAnsweredAt("2026-06-19T00:02:00Z");
        answers.add(item2);

        testPayload.setAnswers(answers);
        
        // Re-hash to pass validation
        TSEV2SubmitPayloadService payloadService = new TSEV2SubmitPayloadService();
        testPayload.setPayloadHash(payloadService.computePayloadHash(testPayload));
    }

    @AfterEach
    public void cleanup() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(p -> {
                     try {
                         Files.delete(p);
                     } catch (IOException ignored) {}
                 });
        }
    }

    @Test
    public void testSaveCreatesEncryptedAndMetaFiles() {
        TSEV2SubmitDryRunMeta meta = service.saveEncryptedSubmitPayloadDryRun(testPayload, validKey, tempDir);

        Path encFile = tempDir.resolve("v2_submit_payload_dryrun.enc");
        Path metaFile = tempDir.resolve("v2_submit_payload_dryrun.meta.json");

        assertTrue(Files.exists(encFile));
        assertTrue(Files.exists(metaFile));
        assertFalse(Files.exists(tempDir.resolve("submit_payload.enc")));
        assertFalse(Files.exists(tempDir.resolve("autosave_payload.enc")));

        assertNotNull(meta);
        assertEquals("v2_submit_payload_dryrun.enc", meta.getEncFileName());
        assertEquals(100, meta.getExamId());
        assertEquals(3, meta.getQuestionCount());
    }

    @Test
    public void testMetaFileDoesNotLeakData() throws IOException {
        service.saveEncryptedSubmitPayloadDryRun(testPayload, validKey, tempDir);
        Path metaFile = tempDir.resolve("v2_submit_payload_dryrun.meta.json");
        String metaJson = Files.readString(metaFile, StandardCharsets.UTF_8);
        String lower = metaJson.toLowerCase();

        assertFalse(lower.contains("answers"), "Meta must not leak answers");
        assertFalse(lower.contains("selectedoptionid"), "Meta must not leak selections");
        assertFalse(lower.contains("sessiontoken"), "Meta must not leak token");
        assertFalse(lower.contains("keyb64"), "Meta must not leak key");
        assertFalse(lower.contains("plaintext"), "Meta must not leak plaintext");
        assertFalse(lower.contains("iscorrect"), "Meta must not leak truth");
        assertFalse(lower.contains("correctoption"), "Meta must not leak truth");
        assertFalse(lower.contains("password"), "Meta must not leak password");
    }

    @Test
    public void testEncryptedFileNotReadablePlaintext() throws IOException {
        service.saveEncryptedSubmitPayloadDryRun(testPayload, validKey, tempDir);
        Path encFile = tempDir.resolve("v2_submit_payload_dryrun.enc");
        String content = Files.readString(encFile, StandardCharsets.UTF_8);

        assertFalse(content.contains("PAPER_START_V2"), "Encrypted file should not contain plaintext");
        assertFalse(content.contains("attempt-123"), "Encrypted file should not contain plaintext");
    }

    @Test
    public void testLoadEncryptedDryRunSuccess() {
        service.saveEncryptedSubmitPayloadDryRun(testPayload, validKey, tempDir);
        
        Path encFile = tempDir.resolve("v2_submit_payload_dryrun.enc");
        Path metaFile = tempDir.resolve("v2_submit_payload_dryrun.meta.json");

        TSEV2SubmitPayload loaded = service.loadEncryptedSubmitPayloadDryRun(encFile, metaFile, validKey);

        assertNotNull(loaded);
        assertEquals(100, loaded.getExamId());
        assertEquals(3, loaded.getQuestionCount());
        assertEquals(2, loaded.getAnswers().size());
        assertEquals(11, loaded.getAnswers().get(0).getSelectedOptionId());
    }

    @Test
    public void testWrongKeyFailsSafely() {
        service.saveEncryptedSubmitPayloadDryRun(testPayload, validKey, tempDir);
        
        Path encFile = tempDir.resolve("v2_submit_payload_dryrun.enc");
        Path metaFile = tempDir.resolve("v2_submit_payload_dryrun.meta.json");

        SecretKey wrongKey = TSEV2EncryptedSubmitDryRunService.generateSubmitDryRunKey();

        TSEV2EncryptedSubmitDryRunService.EncryptedSubmitDryRunException ex = assertThrows(
                TSEV2EncryptedSubmitDryRunService.EncryptedSubmitDryRunException.class,
                () -> service.loadEncryptedSubmitPayloadDryRun(encFile, metaFile, wrongKey)
        );

        assertEquals(TSEV2EncryptedSubmitDryRunService.ERROR_SUBMIT_DRYRUN_DECRYPT_FAILED, ex.getErrorCode());
    }

    @Test
    public void testTamperedEncryptedFileFails() throws IOException {
        service.saveEncryptedSubmitPayloadDryRun(testPayload, validKey, tempDir);
        
        Path encFile = tempDir.resolve("v2_submit_payload_dryrun.enc");
        Path metaFile = tempDir.resolve("v2_submit_payload_dryrun.meta.json");

        byte[] tampered = Files.readAllBytes(encFile);
        tampered[10] = (byte) (tampered[10] ^ 0xFF);
        Files.write(encFile, tampered);

        TSEV2EncryptedSubmitDryRunService.EncryptedSubmitDryRunException ex = assertThrows(
                TSEV2EncryptedSubmitDryRunService.EncryptedSubmitDryRunException.class,
                () -> service.loadEncryptedSubmitPayloadDryRun(encFile, metaFile, validKey)
        );

        assertEquals(TSEV2EncryptedSubmitDryRunService.ERROR_SUBMIT_DRYRUN_HASH_MISMATCH, ex.getErrorCode());
    }
}
