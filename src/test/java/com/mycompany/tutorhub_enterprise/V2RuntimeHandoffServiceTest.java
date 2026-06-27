package com.mycompany.tutorhub_enterprise;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;
import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeHandoffService;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffOption;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffQuestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class V2RuntimeHandoffServiceTest {

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
    public void testEncryptDecryptRoundTrip() throws Exception {
        V2ExamHandoffBundle bundle = createValidBundle();
        String encPath = V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKey, false, null, null, null, 0);

        assertNotNull(encPath);
        File encFile = new File(encPath);
        assertTrue(encFile.exists());

        // Decrypt and verify
        String decryptedJson = V2RuntimeHandoffService.verifyEncryptedHandoffForTest(testKey);
        JsonObject obj = new Gson().fromJson(decryptedJson, JsonObject.class);
        
        assertEquals(101, obj.get("examId").getAsInt());
        assertEquals("REAL_SECRET_SESSION_TOKEN", obj.get("sessionToken").getAsString());
        assertEquals("TEST-ATTEMPT-ID", obj.get("attemptId").getAsString());
    }

    @Test
    public void testEncryptedFileHasNoPlaintextToken() throws Exception {
        V2ExamHandoffBundle bundle = createValidBundle();
        V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKey, false, null, null, null, 0);

        File dir = new File(System.getProperty("user.dir"), V2RuntimeHandoffService.RUNTIME_DIR);
        File encFile = new File(dir, V2RuntimeHandoffService.ENC_FILE_NAME);
        
        String rawContent = new String(Files.readAllBytes(Paths.get(encFile.getAbsolutePath())), StandardCharsets.UTF_8);
        assertFalse(rawContent.contains("REAL_SECRET_SESSION_TOKEN"));
    }

    @Test
    public void testMetaFileHasNoPlaintextTokenAndHasSha256() throws Exception {
        V2ExamHandoffBundle bundle = createValidBundle();
        V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKey, false, null, null, null, 0);

        File dir = new File(System.getProperty("user.dir"), V2RuntimeHandoffService.RUNTIME_DIR);
        File metaFile = new File(dir, V2RuntimeHandoffService.META_FILE_NAME);
        assertTrue(metaFile.exists());

        String metaContent = new String(Files.readAllBytes(Paths.get(metaFile.getAbsolutePath())), StandardCharsets.UTF_8);
        assertFalse(metaContent.contains("REAL_SECRET_SESSION_TOKEN"));
        
        JsonObject metaObj = new Gson().fromJson(metaContent, JsonObject.class);
        assertTrue(metaObj.has("encryptedFileSha256"));
        assertNotNull(metaObj.get("encryptedFileSha256").getAsString());
    }

    @Test
    public void testMissingSessionTokenFailsInProduction() {
        V2ExamHandoffBundle bundle = createValidBundle();
        bundle.sessionToken = null;
        
        Exception e = assertThrows(Exception.class, () -> {
            V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKey, false, null, null, null, 0);
        });
        assertTrue(e.getMessage().contains("sessionToken is missing"));
    }
    
    @Test
    public void testIsCorrectIsBlockedBeforeEncrypt() {
        V2ExamHandoffBundle bundle = createValidBundle();
        bundle.questions.get(0).content = "This has isCorrect inside";
        
        Exception e = assertThrows(Exception.class, () -> {
            V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, testKey, false, null, null, null, 0);
        });
        assertTrue(e.getMessage().contains("SECURITY VIOLATION"));
    }
}
