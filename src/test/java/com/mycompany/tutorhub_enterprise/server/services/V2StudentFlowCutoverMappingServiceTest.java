package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2StudentFlowCutoverMappingServiceTest {
    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.studentFlowCutoverMapping.enabled");
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }


    @Test
    public void testMappingWhenDisabled() {
        System.setProperty("tse.v2.studentFlowCutoverMapping.enabled", "false");
        V2StudentFlowCutoverMappingService service = new V2StudentFlowCutoverMappingService();
        V2StudentFlowCutoverMappingResult result = service.inspectMapping();
        if (result.isReady()) throw new RuntimeException("Should not be ready");
        if (!"NOT_READY".equals(result.getMappingStatus())) throw new RuntimeException("Should be NOT_READY");
    }

    @Test
    public void testMappingWhenEnabled() {
        System.setProperty("tse.v2.studentFlowCutoverMapping.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        V2StudentFlowCutoverMappingService service = new V2StudentFlowCutoverMappingService();
        V2StudentFlowCutoverMappingResult result = service.inspectMapping();
        if (!result.isReady()) throw new RuntimeException("Should be ready");
        if (!"EXAM_SUBMIT".equals(result.getCurrentSubmitAction())) throw new RuntimeException("Action mismatch");
        if (!result.isLegacySubmitDetected()) throw new RuntimeException("Legacy not detected");
        if (!result.isV2ManualSubmitDetected()) throw new RuntimeException("V2 manual not detected");
    }

    @Test
    public void testMappingWhenDefaultV2AlreadyEnabled() {
        System.setProperty("tse.v2.studentFlowCutoverMapping.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        V2StudentFlowCutoverMappingService service = new V2StudentFlowCutoverMappingService();
        V2StudentFlowCutoverMappingResult result = service.inspectMapping();
        if (result.isReady()) throw new RuntimeException("Should not be ready if default V2 is enabled during mapping phase");
    }

    
}
