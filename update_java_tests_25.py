import re

# Update V2SubmitFeatureFlagsTest.java
filepath = 'src/test/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitFeatureFlagsTest.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
new_test = """    @Test
    public void testFinalDemoTrialAcceptanceFlagOffByDefault() {
        assertFalse(V2SubmitFeatureFlags.isFinalDemoTrialAcceptanceEnabled(), "Flag should be off by default");
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
    public void testFinalDemoTrialAcceptanceAction() {
        assertEquals("EXAM_SUBMIT_V2_FINAL_DEMO_TRIAL_ACCEPTANCE", V2SubmitActions.EXAM_SUBMIT_V2_FINAL_DEMO_TRIAL_ACCEPTANCE);
    }
"""
idx2 = content.rfind('}')
content = content[:idx2] + new_action_test + "\n" + content[idx2:]
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated Java test sources 25")
