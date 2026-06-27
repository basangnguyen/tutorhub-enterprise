package com.mycompany.tutorhub_enterprise.server.services;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class V2RustPortableIpcProbeOnlyVerificationService {

    public V2RustPortableIpcProbeOnlyVerificationResult verify() {
        V2RustPortableIpcProbeOnlyVerificationResult result = new V2RustPortableIpcProbeOnlyVerificationResult();

        if (!V2SubmitFeatureFlags.isRustPortableIpcProbeOnlyVerificationEnabled()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("GATE_DISABLED");
            return result;
        }

        File exeFile = new File("dist/TutorHubSecureExam/rust-core.exe");
        if (!exeFile.exists()) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("PENDING_PACKAGING_DECISION"); // As requested, if not found, it could be pending
            result.setPortableIpcStatus("RUST_CORE_NOT_FOUND");
            return result;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(exeFile.getAbsolutePath(), "--probe");
            pb.directory(new File("dist/TutorHubSecureExam"));
            Process process = pb.start();
            
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.setReady(false);
                result.setSuccess(false);
                result.setErrorCode("RUST_CORE_TIMEOUT");
                return result;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                result.setReady(true);
                result.setSuccess(true);
                result.setPortableIpcStatus("PROBE_ONLY_VERIFIED");
            } else {
                result.setReady(false);
                result.setSuccess(false);
                result.setErrorCode("PROBE_FAILED_WITH_EXIT_CODE");
            }

        } catch (IOException | InterruptedException e) {
            result.setReady(false);
            result.setSuccess(false);
            result.setErrorCode("PROBE_EXECUTION_ERROR");
        }

        return result;
    }
}
