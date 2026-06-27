package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.mycompany.tutorhub_enterprise.client.services.V2LoopbackKeyHandoffClient;
import com.mycompany.tutorhub_enterprise.client.services.V2RuntimeHandoffReader;
import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2RuntimeHandoffMeta;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class TSEV2ChildDebugLoader {

    public static TSEV2ChildDebugLoadResult load(TSEChildLaunchArgs args) {
        TSEV2ChildDebugLoadResult result = new TSEV2ChildDebugLoadResult();
        result.success = false;

        if (args.getV2HandoffMetaPath() == null || args.getV2HandoffMetaPath().isEmpty()) {
            result.errorCode = "ERROR_META_MISSING";
            return result;
        }

        if (args.getV2HandoffEncPath() == null || args.getV2HandoffEncPath().isEmpty()) {
            result.errorCode = "ERROR_ENC_MISSING";
            return result;
        }

        Path metaPath = Paths.get(args.getV2HandoffMetaPath());
        Path encPath = Paths.get(args.getV2HandoffEncPath());

        V2RuntimeHandoffMeta meta;
        try {
            meta = V2RuntimeHandoffReader.readMeta(metaPath);
        } catch (Exception e) {
            result.errorCode = "ERROR_META_INVALID";
            return result;
        }

        if (meta.parentIpcPort <= 0) {
            result.errorCode = "ERROR_IPC_PORT_INVALID";
            return result;
        }

        // Fetch key
        V2LoopbackKeyHandoffClient ipcClient = new V2LoopbackKeyHandoffClient();
        Optional<javax.crypto.SecretKey> keyOpt = ipcClient.requestKey("127.0.0.1", meta.parentIpcPort, meta.handoffId, meta.nonce);
        
        if (!keyOpt.isPresent()) {
            result.errorCode = "ERROR_KEY_FETCH_FAILED";
            return result;
        }
        
        result.keyFetched = true;
        String keyB64 = java.util.Base64.getEncoder().encodeToString(keyOpt.get().getEncoded());

        // Verify Hash
        try {
            V2RuntimeHandoffReader.verifyEncryptedFileHash(encPath, meta);
            result.hashVerified = true;
        } catch (Exception e) {
            result.errorCode = "ERROR_HASH_MISMATCH";
            return result;
        }

        // Decrypt
        String decryptedJson;
        try {
            decryptedJson = V2RuntimeHandoffReader.decryptRuntimeHandoff(encPath, keyB64);
            result.decrypted = true;
        } catch (Exception e) {
            result.errorCode = "ERROR_DECRYPT_FAILED";
            return result;
        }

        // Parse and validate
        V2ExamHandoffBundle bundle;
        try {
            bundle = V2RuntimeHandoffReader.parseBundle(decryptedJson);
            // Re-validate against security violations
            V2RuntimeHandoffReader.validateBundleForChildPrototype(bundle, true);
            result.parsed = true;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("ERROR_SECURITY_VIOLATION")) {
                result.errorCode = "ERROR_SECURITY_VIOLATION";
            } else {
                result.errorCode = "ERROR_PARSE_FAILED";
            }
            return result;
        }

        // Populate result with safe fields only
        result.success = true;
        result.examId = bundle.examId;
        result.paperId = bundle.paperId;
        result.attemptId = bundle.attemptId;
        result.packageHash = bundle.packageHash;
        result.questionCount = bundle.questions != null ? bundle.questions.size() : 0;
        result.totalScore = bundle.totalScore;
        result.deadlineAt = bundle.deadlineAt;
        
        result.renderModel = TSEV2ChildReadOnlyRenderLoader.sanitizeForReadOnlyRender(bundle);

        return result;
    }
}
