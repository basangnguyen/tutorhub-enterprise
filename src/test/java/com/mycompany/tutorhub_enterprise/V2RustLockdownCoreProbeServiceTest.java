package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.server.services.V2RustLockdownCoreProbeService;
import com.mycompany.tutorhub_enterprise.server.services.V2RustLockdownCoreProbeResult;
import com.mycompany.tutorhub_enterprise.server.services.V2SubmitFeatureFlags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class V2RustLockdownCoreProbeServiceTest {

    @Test
    public void testProbeDisabledReturnsNotReady() {
        System.setProperty("tse.v2.rustLockdownCoreProbe.enabled", "false");
        V2RustLockdownCoreProbeService svc = new V2RustLockdownCoreProbeService();
        V2RustLockdownCoreProbeResult res = svc.runProbe();

        assertFalse(res.isSuccess());
        assertFalse(res.isReady());
        assertEquals("NOT_READY", res.getErrorCode());
        assertTrue(res.getWarnings().contains("Probe is disabled via tse.v2.rustLockdownCoreProbe.enabled=false"));
    }

    @Test
    public void testMissingExecutable() {
        System.setProperty("tse.v2.rustLockdownCoreProbe.enabled", "true");
        V2RustLockdownCoreProbeService svc = new V2RustLockdownCoreProbeService();
        // Since test env might not have rust-core.exe compiled in root, this should return missing or timeout depending on env
        V2RustLockdownCoreProbeResult res = svc.runProbe();

        // If rust-core wasn't built yet, it's missing. If it was, it runs. We'll just verify no crashes.
        assertNotNull(res);
        assertNotNull(res.getCheckedAt());
        
        if (!res.isSuccess()) {
            assertTrue(res.getErrorCode().equals("RUST_CORE_NOT_FOUND") || res.getErrorCode().equals("PROBE_EXECUTION_ERROR"));
        } else {
            assertTrue(res.isOsWindows());
        }
    }
}
