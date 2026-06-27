package com.mycompany.tutorhub_enterprise.server.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;

public class V2RustLockdownCoreProbeService {

    public V2RustLockdownCoreProbeResult runProbe() {
        V2RustLockdownCoreProbeResult result = new V2RustLockdownCoreProbeResult();
        result.setCheckedAt(Instant.now().toString());
        result.setWarnings(new ArrayList<>());

        if (!V2SubmitFeatureFlags.isRustLockdownCoreProbeEnabled()) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("NOT_READY");
            result.getWarnings().add("Probe is disabled via tse.v2.rustLockdownCoreProbe.enabled=false");
            return result;
        }

        File executable = new File("rust-core/target/debug/rust-core.exe");
        if (!executable.exists()) {
            executable = new File("rust-core.exe"); // Fallback for testing
            if (!executable.exists()) {
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("RUST_CORE_NOT_FOUND");
                result.getWarnings().add("rust-core.exe executable not found");
                return result;
            }
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(executable.getAbsolutePath(), "--probe");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.setSuccess(false);
                result.setReady(false);
                result.setErrorCode("RUST_CORE_TIMEOUT");
                return result;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            Gson gson = new Gson();
            V2RustLockdownCoreProbeResult parsedResult = gson.fromJson(output.toString(), V2RustLockdownCoreProbeResult.class);
            return parsedResult;

        } catch (Exception e) {
            result.setSuccess(false);
            result.setReady(false);
            result.setErrorCode("PROBE_EXECUTION_ERROR");
            result.getWarnings().add(e.getMessage());
            return result;
        }
    }
}
