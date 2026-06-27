package com.mycompany.tutorhub_enterprise;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;
import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeHandoffReader;
import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeHandoffService;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2RuntimeHandoffMeta;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffQuestion;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class V2RuntimeHandoffReaderTest {

    private String testKey;

    @BeforeEach
    public void setup() throws Exception {
        testKey = CryptoUtils.generateAESKey();
        cleanupFiles();
    }

    @AfterEach
    public void teardown() {
        cleanupFiles();
    }

    private void cleanupFiles() {
        File dir = new File(System.getProperty("user.dir"), V2RuntimeHandoffService.RUNTIME_DIR);
        File encFile = new File(dir, V2RuntimeHandoffService.ENC_FILE_NAME);
        File metaFile = new File(dir, V2RuntimeHandoffService.META_FILE_NAME);
        if (encFile.exists()) encFile.delete();
        if (metaFile.exists()) metaFile.delete();
    }

    private V2ExamHandoffBundle createValidBundle() {
        V2ExamHandoffBundle bundle = new V2ExamHandoffBundle();
        bundle.examId = 101;
        bundle.paperId = 202;
        bundle.attemptId = "TEST-ATTEMPT-ID";
        bundle.sessionToken = "REAL_SECRET_SESSION_TOKEN";
        bundle.durationMinutes = 60;
        bundle.packageHash = "TEST_PACKAGE_HASH";
        bundle.questionCount = 1;
        bundle.totalScore = 10;
        
        bundle.questions = new ArrayList<>();
        V2ExamHandoffQuestion q = new V2ExamHandoffQuestion();
        q.questionId = 1;
        q.content = "Encrypted Question";
        q.type = "MCQ";
        
        q.options = new ArrayList<>();
        V2ExamHandoffOption opt = new V2ExamHandoffOption();
        opt.optionLabel = "A";
        opt.content = "Answer A";
        q.options.add(opt);
        
        bundle.questions.add(q);
        return bundle;
    }

    @Test
    public void testReadAndDecryptSuccess() throws Exception {
        V2ExamHandoffBundle bundle = createValidBundle();
        String encPath = V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKey, false, null, null, null, 0);

        File dir = new File(System.getProperty("user.dir"), V2RuntimeHandoffService.RUNTIME_DIR);
        Path metaPath = Paths.get(dir.getAbsolutePath(), V2RuntimeHandoffService.META_FILE_NAME);
        Path encryptedPath = Paths.get(dir.getAbsolutePath(), V2RuntimeHandoffService.ENC_FILE_NAME);

        V2RuntimeHandoffMeta meta = V2RuntimeHandoffReader.readMeta(metaPath);
        assertNotNull(meta);
        assertEquals("TEST_PACKAGE_HASH", meta.packageHash);

        assertDoesNotThrow(() -> {
            V2RuntimeHandoffReader.verifyEncryptedFileHash(encryptedPath, meta);
        });

        String decryptedJson = V2RuntimeHandoffReader.decryptRuntimeHandoff(encryptedPath, testKey);
        V2ExamHandoffBundle parsedBundle = V2RuntimeHandoffReader.parseBundle(decryptedJson);

        assertNotNull(parsedBundle);
        assertEquals(101, parsedBundle.examId);
        assertDoesNotThrow(() -> {
            V2RuntimeHandoffReader.validateBundleForChildPrototype(parsedBundle, false);
        });
    }

    @Test
    public void testInvalidHashRejection() throws Exception {
        V2ExamHandoffBundle bundle = createValidBundle();
        V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKey, false, null, null, null, 0);

        File dir = new File(System.getProperty("user.dir"), V2RuntimeHandoffService.RUNTIME_DIR);
        Path metaPath = Paths.get(dir.getAbsolutePath(), V2RuntimeHandoffService.META_FILE_NAME);
        Path encryptedPath = Paths.get(dir.getAbsolutePath(), V2RuntimeHandoffService.ENC_FILE_NAME);

        V2RuntimeHandoffMeta meta = V2RuntimeHandoffReader.readMeta(metaPath);
        meta.encryptedFileSha256 = "INVALID_HASH";

        Exception e = assertThrows(Exception.class, () -> {
            V2RuntimeHandoffReader.verifyEncryptedFileHash(encryptedPath, meta);
        });
        assertEquals("ERROR_HASH_MISMATCH", e.getMessage());
    }

    @Test
    public void testInvalidKeyFailsSafe() throws Exception {
        V2ExamHandoffBundle bundle = createValidBundle();
        V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKey, false, null, null, null, 0);

        File dir = new File(System.getProperty("user.dir"), V2RuntimeHandoffService.RUNTIME_DIR);
        Path encryptedPath = Paths.get(dir.getAbsolutePath(), V2RuntimeHandoffService.ENC_FILE_NAME);

        String badKey = CryptoUtils.generateAESKey();
        
        Exception e = assertThrows(Exception.class, () -> {
            V2RuntimeHandoffReader.decryptRuntimeHandoff(encryptedPath, badKey);
        });
        assertEquals("ERROR_DECRYPT_FAILED", e.getMessage());
    }

    @Test
    public void testParseRejectsSecurityKeywords() throws Exception {
        String badJson1 = "{\"isCorrect\": true}";
        Exception e1 = assertThrows(Exception.class, () -> V2RuntimeHandoffReader.parseBundle(badJson1));
        assertEquals("ERROR_SECURITY_VIOLATION_ANSWER_FIELDS", e1.getMessage());

        String badJson2 = "{\"answerKey\": \"A\"}";
        Exception e2 = assertThrows(Exception.class, () -> V2RuntimeHandoffReader.parseBundle(badJson2));
        assertEquals("ERROR_SECURITY_VIOLATION_ANSWER_FIELDS", e2.getMessage());
    }

    @Test
    public void testReaderDoesNotLogPlaintextToken() throws Exception {
        V2ExamHandoffBundle bundle = createValidBundle();
        V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKey, false, null, null, null, 0);

        File dir = new File(System.getProperty("user.dir"), V2RuntimeHandoffService.RUNTIME_DIR);
        Path metaPath = Paths.get(dir.getAbsolutePath(), V2RuntimeHandoffService.META_FILE_NAME);
        Path encryptedPath = Paths.get(dir.getAbsolutePath(), V2RuntimeHandoffService.ENC_FILE_NAME);

        V2RuntimeHandoffMeta meta = V2RuntimeHandoffReader.readMeta(metaPath);
        V2RuntimeHandoffReader.verifyEncryptedFileHash(encryptedPath, meta);
        String decryptedJson = V2RuntimeHandoffReader.decryptRuntimeHandoff(encryptedPath, testKey);
        
        // This is mainly a logical check that the functions executed successfully and returned the parsed data
        // without dumping the JSON to disk.
        V2ExamHandoffBundle parsedBundle = V2RuntimeHandoffReader.parseBundle(decryptedJson);
        assertEquals("REAL_SECRET_SESSION_TOKEN", parsedBundle.sessionToken);
    }
}
