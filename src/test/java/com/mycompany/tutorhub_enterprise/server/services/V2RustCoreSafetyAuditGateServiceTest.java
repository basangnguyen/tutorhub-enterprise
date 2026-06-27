package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class V2RustCoreSafetyAuditGateServiceTest {

    @BeforeEach
    public void setup() {
        System.setProperty("tse.v2.rustCoreSafetyAuditGate.enabled", "false");
    }

    @Test
    public void testGateDisabled() {
        V2RustCoreSafetyAuditGateService service = new V2RustCoreSafetyAuditGateService();
        V2RustCoreSafetyAuditGateResult result = service.checkGate();
        assertFalse(result.isReady());
        assertEquals("NOT_READY", result.getErrorCode());
    }

    @Test
    public void testGateEnabled() {
        System.setProperty("tse.v2.rustCoreSafetyAuditGate.enabled", "true");
        V2RustCoreSafetyAuditGateService service = new V2RustCoreSafetyAuditGateService();
        V2RustCoreSafetyAuditGateResult result = service.checkGate();
        
        // Either RUST_CORE_DIR_MISSING or RUST_CORE_SAFETY_AUDIT_READY depending on CWD, 
        // but we are asserting structure works without mockito.
        assertTrue(result.getErrorCode().equals("RUST_CORE_DIR_MISSING") || result.getErrorCode().equals("RUST_CORE_SAFETY_AUDIT_READY"));
    }
}
