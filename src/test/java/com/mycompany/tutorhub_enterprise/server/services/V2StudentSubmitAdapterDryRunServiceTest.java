package com.mycompany.tutorhub_enterprise.server.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class V2StudentSubmitAdapterDryRunServiceTest {
    @BeforeEach
    public void setUp() {
        System.setProperty("tse.v2.answerPayloadContract.enabled", "true");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("tse.v2.studentSubmitAdapterDryRun.enabled");
        System.clearProperty("tse.v2.defaultStudentSubmitV2.enabled");
    }


    @Test
    public void testAdapterWhenDisabled() {
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "false");
        V2StudentSubmitAdapterDryRunService service = new V2StudentSubmitAdapterDryRunService();
        V2StudentSubmitAdapterDryRunResult result = service.dryRunRoute(1, "ATTEMPT123", "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"ATTEMPT123\",\"paperId\":1,\"answers\":[]}");
        if (result.isReady()) throw new RuntimeException("Should not be ready");
        if (!"NOT_READY".equals(result.getPlannedRoute())) throw new RuntimeException("Should be NOT_READY route");
    }

    @Test
    public void testAdapterInvalidPayload() {
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "true");
        V2StudentSubmitAdapterDryRunService service = new V2StudentSubmitAdapterDryRunService();
        V2StudentSubmitAdapterDryRunResult result = service.dryRunRoute(1, "ATTEMPT123", "invalid json");
        if (result.isReady()) throw new RuntimeException("Should not be ready on invalid payload");
    }

    @Test
    public void testAdapterDefaultV2Disabled() {
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        V2StudentSubmitAdapterDryRunService service = new V2StudentSubmitAdapterDryRunService();
        V2StudentSubmitAdapterDryRunResult result = service.dryRunRoute(1, "ATTEMPT123", "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"ATTEMPT123\",\"paperId\":1,\"answers\":[]}");
        if (!result.isReady()) throw new RuntimeException("Should be ready");
        if (!"LEGACY_V1_STUDENT_SUBMIT".equals(result.getPlannedRoute())) throw new RuntimeException("Route should be legacy");
    }

    @Test
    public void testAdapterDefaultV2Enabled() {
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        V2StudentSubmitAdapterDryRunService service = new V2StudentSubmitAdapterDryRunService();
        V2StudentSubmitAdapterDryRunResult result = service.dryRunRoute(1, "ATTEMPT123", "{\"payloadVersion\":\"TSE_V2_ANSWER_PAYLOAD_V1\",\"attemptId\":\"ATTEMPT123\",\"paperId\":1,\"answers\":[]}");
        if (!result.isReady()) throw new RuntimeException("Should be ready");
        if (!"V2_MANUAL_CANDIDATE_PIPELINE".equals(result.getPlannedRoute())) throw new RuntimeException("Route should be V2 manual pipeline");
    }

    
}
