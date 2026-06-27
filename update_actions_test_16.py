import re

filepath = 'src/test/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitActionsTest.java'
with open(filepath, 'r') as f:
    content = f.read()

new_tests = """    @Test
    public void testRustPhysicalMachineSafeProbeGateAction() {
        assertEquals("EXAM_SUBMIT_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE", V2SubmitActions.EXAM_SUBMIT_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE);
    }

    @Test
    public void testRustPortableIpcProbeOnlyVerifyAction() {
        assertEquals("EXAM_SUBMIT_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFY", V2SubmitActions.EXAM_SUBMIT_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFY);
    }

    @Test
    public void testDemoNotLockdownFinalDecisionGateAction() {
        assertEquals("EXAM_SUBMIT_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE", V2SubmitActions.EXAM_SUBMIT_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE);
    }
"""

last_brace_idx = content.rfind('}')
if last_brace_idx != -1:
    content = content[:last_brace_idx] + new_tests + '\n' + content[last_brace_idx:]

with open(filepath, 'w') as f:
    f.write(content)
print('Updated V2SubmitActionsTest.java')
