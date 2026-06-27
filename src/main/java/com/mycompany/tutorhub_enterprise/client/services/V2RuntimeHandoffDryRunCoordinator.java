package com.mycompany.tutorhub_enterprise.client.services;

import com.mycompany.tutorhub_enterprise.models.exam.V2ExamHandoffBundle;
import com.mycompany.tutorhub_enterprise.models.exam.V2ChildLaunchDescriptor;
import com.mycompany.tutorhub_enterprise.client.exam.utils.CryptoUtils;

import javax.crypto.SecretKey;
import java.time.Duration;

public class V2RuntimeHandoffDryRunCoordinator {

    public static V2ChildLaunchDescriptor createEncryptedHandoffAndRegisterKey(V2ExamHandoffBundle bundle, boolean isDebugMode) throws Exception {
        // 1. Generate new AES key
        String base64Key = CryptoUtils.generateAESKey();

        // 2. Register key to get handoffId
        String purpose = "V2_HANDOFF_" + (bundle.attemptId != null ? bundle.attemptId : "DEBUG");
        String handoffId = V2RuntimeKeyRegistry.registerKey(base64Key, Duration.ofMinutes(5), purpose);

        // 3. Create nonce for IPC
        byte[] nonceBytes = new byte[24];
        new java.security.SecureRandom().nextBytes(nonceBytes);
        String nonce = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);

        // 4. Create encrypted handoff (pass handoffId so service writes it to meta)
        // Note: For dry run, parentIpcHost/Port is not actually bound, but we can write 127.0.0.1 and a dummy port.
        String encPath = V2RuntimeHandoffService.createEncryptedRuntimeHandoff(bundle, base64Key, isDebugMode, handoffId, nonce, "127.0.0.1", 12345);

        // 4. Create descriptor
        V2ChildLaunchDescriptor descriptor = new V2ChildLaunchDescriptor();
        descriptor.handoffId = handoffId;
        descriptor.encPath = encPath;
        // The meta path is assumed to be next to encPath
        descriptor.metaPath = encPath.replace(".enc", ".meta.json");
        descriptor.examId = bundle.examId;
        descriptor.paperId = bundle.paperId;
        descriptor.attemptId = bundle.attemptId;
        descriptor.deadlineAt = bundle.deadlineAt;
        descriptor.packageHash = bundle.packageHash;
        descriptor.createdAt = bundle.createdAt;
        descriptor.flow = bundle.flow;

        return descriptor;
    }
}
