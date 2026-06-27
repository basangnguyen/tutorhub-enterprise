package com.mycompany.tutorhub_enterprise;

import com.google.gson.Gson;
import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;
import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeHandoffDryRunCoordinator;
import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeHandoffReader;
import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeKeyRegistry;
import com.mycompany.tutorhub_enterprise.models.exam.V2ChildLaunchDescriptor;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2RuntimeHandoffMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class V2RuntimeHandoffKeyHandoffTest {

    @BeforeEach
    public void setup() {
        V2RuntimeKeyRegistry.clearForTest();
    }

    private V2ExamHandoffBundle createValidBundle() {
        V2ExamHandoffBundle bundle = new V2ExamHandoffBundle();
        bundle.examId = 100;
        bundle.paperId = 200;
        bundle.packageHash = "dummyhash123";
        bundle.questionCount = 1;
        bundle.sessionToken = "REAL_TOKEN_XYZ";
        bundle.attemptId = "test-attempt";
        bundle.questions = Collections.singletonList(new com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffQuestion());
        return bundle;
    }

    @Test
    public void testDryRunKeyHandoff() throws Exception {
        V2ExamHandoffBundle bundle = createValidBundle();
        
        // 1. Create encrypted handoff + register key
        V2ChildLaunchDescriptor descriptor = V2RuntimeHandoffDryRunCoordinator.createEncryptedHandoffAndRegisterKey(bundle, false);
        
        // 2. verify descriptor and meta has handoffId
        assertNotNull(descriptor.handoffId);
        V2RuntimeHandoffMeta meta = V2RuntimeHandoffReader.readMeta(Paths.get(descriptor.metaPath));
        assertEquals(descriptor.handoffId, meta.handoffId);
        
        // 3. meta does NOT have AES key or session token
        String metaStr = new String(Files.readAllBytes(Paths.get(descriptor.metaPath)));
        assertFalse(metaStr.contains("aesKey"));
        assertFalse(metaStr.contains("secretKey"));
        assertFalse(metaStr.contains("REAL_TOKEN_XYZ"));

        // 4. consume key
        Optional<String> retrieved = V2RuntimeKeyRegistry.consumeKey(descriptor.handoffId);
        assertTrue(retrieved.isPresent());
        String base64Key = retrieved.get();

        // 5. verify hash
        V2RuntimeHandoffReader.verifyEncryptedFileHash(Paths.get(descriptor.encPath), meta);

        // 6. reader decrypts
        String json = V2RuntimeHandoffReader.decryptRuntimeHandoff(Paths.get(descriptor.encPath), base64Key);
        assertTrue(json.contains("REAL_TOKEN_XYZ"));

        // 7. consume second time fails
        assertFalse(V2RuntimeKeyRegistry.consumeKey(descriptor.handoffId).isPresent());
        
        // Cleanup
        new File(descriptor.metaPath).delete();
        new File(descriptor.encPath).delete();
    }
}
