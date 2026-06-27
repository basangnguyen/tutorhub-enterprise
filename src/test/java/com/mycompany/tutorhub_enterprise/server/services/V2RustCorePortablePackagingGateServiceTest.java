package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2RustCorePortablePackagingGateServiceTest {

    @org.junit.jupiter.api.BeforeEach
    public void setup() {
        System.setProperty("tse.v2.rustCorePortablePackagingGate.enabled", "false");
    }

    @Test
    public void testGateNotReadyIfFlagOff() {
        V2RustCorePortablePackagingGateService service = new V2RustCorePortablePackagingGateService();
        V2RustCorePortablePackagingGateResult result = service.checkGate();
        
        assertFalse(result.isReady());
        assertEquals("NOT_READY", result.getErrorCode());
        assertFalse(result.isSuccess());
    }

    @Test
    public void testGateReadyIfFlagOn() {
        System.setProperty("tse.v2.rustCorePortablePackagingGate.enabled", "true");
        V2RustCorePortablePackagingGateService service = new V2RustCorePortablePackagingGateService();
        V2RustCorePortablePackagingGateResult result = service.checkGate();
        
        assertTrue(result.isReady());
        assertEquals("PENDING_PACKAGING_DECISION", result.getErrorCode());
        assertTrue(result.isSuccess());
    }
}
