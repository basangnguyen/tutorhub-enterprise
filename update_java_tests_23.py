import re

# Update V2SubmitFeatureFlagsTest.java
filepath = 'src/test/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitFeatureFlagsTest.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
new_test = """    @Test
    public void testManualTrialReadyToRunGateFlagOffByDefault() {
        assertFalse(V2SubmitFeatureFlags.isManualTrialReadyToRunGateEnabled(), "Flag should be off by default");
    }
    @Test
    public void testManualTrialEvidenceFinalizerFlagOffByDefault() {
        assertFalse(V2SubmitFeatureFlags.isManualTrialEvidenceFinalizerEnabled(), "Flag should be off by default");
    }
    @Test
    public void testDemoRcFinalAcceptanceDecisionFlagOffByDefault() {
        assertFalse(V2SubmitFeatureFlags.isDemoRcFinalAcceptanceDecisionEnabled(), "Flag should be off by default");
    }
"""
idx2 = content.rfind('}')
content = content[:idx2] + new_test + "\n" + content[idx2:]
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Update V2SubmitActionsTest.java
filepath = 'src/test/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitActionsTest.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
new_action_test = """    @Test
    public void testManualTrialReadyToRunGateAction() {
        assertEquals("EXAM_SUBMIT_V2_MANUAL_TRIAL_READY_TO_RUN_GATE", V2SubmitActions.EXAM_SUBMIT_V2_MANUAL_TRIAL_READY_TO_RUN_GATE);
    }
    @Test
    public void testManualTrialEvidenceFinalizerAction() {
        assertEquals("EXAM_SUBMIT_V2_MANUAL_TRIAL_EVIDENCE_FINALIZER", V2SubmitActions.EXAM_SUBMIT_V2_MANUAL_TRIAL_EVIDENCE_FINALIZER);
    }
    @Test
    public void testDemoRcFinalAcceptanceDecisionAction() {
        assertEquals("EXAM_SUBMIT_V2_DEMO_RC_FINAL_ACCEPTANCE_DECISION", V2SubmitActions.EXAM_SUBMIT_V2_DEMO_RC_FINAL_ACCEPTANCE_DECISION);
    }
"""
idx2 = content.rfind('}')
content = content[:idx2] + new_action_test + "\n" + content[idx2:]
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated Java test sources 23")
