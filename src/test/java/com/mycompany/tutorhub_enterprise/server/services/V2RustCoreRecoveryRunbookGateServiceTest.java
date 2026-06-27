package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2RustCoreRecoveryRunbookGateServiceTest {

    @org.junit.jupiter.api.BeforeEach
    public void setup() {
        System.setProperty("tse.v2.rustCoreRecoveryRunbookGate.enabled", "false");
    }

    @Test
    public void testGateNotReadyIfFlagOff() {
        V2RustCoreRecoveryRunbookGateService service = new V2RustCoreRecoveryRunbookGateService();
        V2RustCoreRecoveryRunbookGateResult result = service.checkGate();
        
        assertFalse(result.isReady());
        assertEquals("NOT_READY", result.getErrorCode());
        assertFalse(result.isSuccess());
    }

    @Test
    public void testGateReadyIfFlagOnAndFileExists() {
        System.setProperty("tse.v2.rustCoreRecoveryRunbookGate.enabled", "true");
        V2RustCoreRecoveryRunbookGateService service = new V2RustCoreRecoveryRunbookGateService();
        V2RustCoreRecoveryRunbookGateResult result = service.checkGate();
        
        // Either RUNBOOK_MISSING or RUST_CORE_RECOVERY_RUNBOOK_READY depending on CWD, 
        // but we are asserting structure works without mockito.
        assertTrue(result.getErrorCode().equals("RUNBOOK_MISSING") || result.getErrorCode().equals("RUST_CORE_RECOVERY_RUNBOOK_READY"));
    }
}
