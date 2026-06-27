import os

def write_file(path, content):
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')

mapping_test = """
package com.mycompany.tutorhub_enterprise.server.services;

public class V2StudentFlowCutoverMappingServiceTest {

    public void testMappingWhenDisabled() {
        System.setProperty("tse.v2.studentFlowCutoverMapping.enabled", "false");
        V2StudentFlowCutoverMappingService service = new V2StudentFlowCutoverMappingService();
        V2StudentFlowCutoverMappingResult result = service.inspectMapping();
        if (result.isReady()) throw new RuntimeException("Should not be ready");
        if (!"NOT_READY".equals(result.getMappingStatus())) throw new RuntimeException("Should be NOT_READY");
    }

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

    public void testMappingWhenDefaultV2AlreadyEnabled() {
        System.setProperty("tse.v2.studentFlowCutoverMapping.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        V2StudentFlowCutoverMappingService service = new V2StudentFlowCutoverMappingService();
        V2StudentFlowCutoverMappingResult result = service.inspectMapping();
        if (result.isReady()) throw new RuntimeException("Should not be ready if default V2 is enabled during mapping phase");
    }

    public static void runAll() {
        V2StudentFlowCutoverMappingServiceTest test = new V2StudentFlowCutoverMappingServiceTest();
        test.testMappingWhenDisabled();
        test.testMappingWhenEnabled();
        test.testMappingWhenDefaultV2AlreadyEnabled();
    }
}
"""

adapter_test = """
package com.mycompany.tutorhub_enterprise.server.services;

public class V2StudentSubmitAdapterDryRunServiceTest {

    public void testAdapterWhenDisabled() {
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "false");
        V2StudentSubmitAdapterDryRunService service = new V2StudentSubmitAdapterDryRunService();
        V2StudentSubmitAdapterDryRunResult result = service.dryRunRoute(1, "ATTEMPT123", "{}");
        if (result.isReady()) throw new RuntimeException("Should not be ready");
        if (!"NOT_READY".equals(result.getPlannedRoute())) throw new RuntimeException("Should be NOT_READY route");
    }

    public void testAdapterInvalidPayload() {
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "true");
        V2StudentSubmitAdapterDryRunService service = new V2StudentSubmitAdapterDryRunService();
        V2StudentSubmitAdapterDryRunResult result = service.dryRunRoute(1, "ATTEMPT123", "invalid json");
        if (result.isReady()) throw new RuntimeException("Should not be ready on invalid payload");
    }

    public void testAdapterDefaultV2Disabled() {
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        V2StudentSubmitAdapterDryRunService service = new V2StudentSubmitAdapterDryRunService();
        V2StudentSubmitAdapterDryRunResult result = service.dryRunRoute(1, "ATTEMPT123", "{}");
        if (!result.isReady()) throw new RuntimeException("Should be ready");
        if (!"LEGACY_V1_STUDENT_SUBMIT".equals(result.getPlannedRoute())) throw new RuntimeException("Route should be legacy");
    }

    public void testAdapterDefaultV2Enabled() {
        System.setProperty("tse.v2.studentSubmitAdapterDryRun.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "true");
        V2StudentSubmitAdapterDryRunService service = new V2StudentSubmitAdapterDryRunService();
        V2StudentSubmitAdapterDryRunResult result = service.dryRunRoute(1, "ATTEMPT123", "{}");
        if (!result.isReady()) throw new RuntimeException("Should be ready");
        if (!"V2_MANUAL_CANDIDATE_PIPELINE".equals(result.getPlannedRoute())) throw new RuntimeException("Route should be V2 manual pipeline");
    }

    public static void runAll() {
        V2StudentSubmitAdapterDryRunServiceTest test = new V2StudentSubmitAdapterDryRunServiceTest();
        test.testAdapterWhenDisabled();
        test.testAdapterInvalidPayload();
        test.testAdapterDefaultV2Disabled();
        test.testAdapterDefaultV2Enabled();
    }
}
"""

wiring_test = """
package com.mycompany.tutorhub_enterprise.server.services;

public class V2StudentSubmitUiWiringReadinessServiceTest {

    public void testWiringWhenDisabled() {
        System.setProperty("tse.v2.studentSubmitUiWiringReadiness.enabled", "false");
        V2StudentSubmitUiWiringReadinessService service = new V2StudentSubmitUiWiringReadinessService();
        V2StudentSubmitUiWiringReadinessResult result = service.checkReadiness(null, null, null);
        if (result.isReady()) throw new RuntimeException("Should not be ready");
    }

    public void testWiringWhenDepsNotReady() {
        System.setProperty("tse.v2.studentSubmitUiWiringReadiness.enabled", "true");
        V2StudentSubmitUiWiringReadinessService service = new V2StudentSubmitUiWiringReadinessService();
        V2StudentFlowCutoverMappingResult mapping = new V2StudentFlowCutoverMappingResult(); mapping.setReady(false);
        V2StudentSubmitAdapterDryRunResult adapter = new V2StudentSubmitAdapterDryRunResult(); adapter.setReady(false);
        V2StudentFlowControlledCutoverGateResult gate = new V2StudentFlowControlledCutoverGateResult(); gate.setReady(false);
        
        V2StudentSubmitUiWiringReadinessResult result = service.checkReadiness(mapping, adapter, gate);
        if (result.isReady()) throw new RuntimeException("Should not be ready");
    }

    public void testWiringWhenReady() {
        System.setProperty("tse.v2.studentSubmitUiWiringReadiness.enabled", "true");
        System.setProperty("tse.v2.defaultStudentSubmitV2.enabled", "false");
        V2StudentSubmitUiWiringReadinessService service = new V2StudentSubmitUiWiringReadinessService();
        
        V2StudentFlowCutoverMappingResult mapping = new V2StudentFlowCutoverMappingResult(); mapping.setReady(true);
        V2StudentSubmitAdapterDryRunResult adapter = new V2StudentSubmitAdapterDryRunResult(); adapter.setReady(true);
        V2StudentFlowControlledCutoverGateResult gate = new V2StudentFlowControlledCutoverGateResult(); gate.setReady(true);
        
        V2StudentSubmitUiWiringReadinessResult result = service.checkReadiness(mapping, adapter, gate);
        if (!result.isReady()) throw new RuntimeException("Should be ready");
    }

    public static void runAll() {
        V2StudentSubmitUiWiringReadinessServiceTest test = new V2StudentSubmitUiWiringReadinessServiceTest();
        test.testWiringWhenDisabled();
        test.testWiringWhenDepsNotReady();
        test.testWiringWhenReady();
    }
}
"""

base_dir = r"D:\Ban_sao_du_an\src\test\java\com\mycompany\tutorhub_enterprise\server\services"
write_file(os.path.join(base_dir, "V2StudentFlowCutoverMappingServiceTest.java"), mapping_test)
write_file(os.path.join(base_dir, "V2StudentSubmitAdapterDryRunServiceTest.java"), adapter_test)
write_file(os.path.join(base_dir, "V2StudentSubmitUiWiringReadinessServiceTest.java"), wiring_test)

print("Created 3 test classes")
