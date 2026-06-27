package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2StudentSubmitUiWiringReadinessServiceTest {
    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.studentSubmitUiWiringReadiness.enabled");
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }


    @Test
    public void testWiringWhenDisabled() {
        System.setProperty("tse.v2.studentSubmitUiWiringReadiness.enabled", "false");
        V2StudentSubmitUiWiringReadinessService service = new V2StudentSubmitUiWiringReadinessService();
        V2StudentSubmitUiWiringReadinessResult result = service.checkReadiness(null, null, null);
        if (result.isReady()) throw new RuntimeException("Should not be ready");
    }

    @Test
    public void testWiringWhenDepsNotReady() {
        System.setProperty("tse.v2.studentSubmitUiWiringReadiness.enabled", "true");
        V2StudentSubmitUiWiringReadinessService service = new V2StudentSubmitUiWiringReadinessService();
        V2StudentFlowCutoverMappingResult mapping = new V2StudentFlowCutoverMappingResult(); mapping.setReady(false);
        V2StudentSubmitAdapterDryRunResult adapter = new V2StudentSubmitAdapterDryRunResult(); adapter.setReady(false);
        V2StudentFlowControlledCutoverGateResult gate = new V2StudentFlowControlledCutoverGateResult.Builder().ready(false).build();
        
        V2StudentSubmitUiWiringReadinessResult result = service.checkReadiness(mapping, adapter, gate);
        if (result.isReady()) throw new RuntimeException("Should not be ready");
    }

    @Test
    public void testWiringWhenReady() {
        System.setProperty("tse.v2.studentSubmitUiWiringReadiness.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        V2StudentSubmitUiWiringReadinessService service = new V2StudentSubmitUiWiringReadinessService();
        
        V2StudentFlowCutoverMappingResult mapping = new V2StudentFlowCutoverMappingResult(); mapping.setReady(true);
        V2StudentSubmitAdapterDryRunResult adapter = new V2StudentSubmitAdapterDryRunResult(); adapter.setReady(true);
        V2StudentFlowControlledCutoverGateResult gate = new V2StudentFlowControlledCutoverGateResult.Builder().ready(true).build();
        
        V2StudentSubmitUiWiringReadinessResult result = service.checkReadiness(mapping, adapter, gate);
        if (!result.isReady()) throw new RuntimeException("Should be ready");
    }

    
}
