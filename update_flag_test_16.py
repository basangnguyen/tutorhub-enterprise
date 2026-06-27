import re

filepath = 'src/test/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitFeatureFlagsTest.java'
with open(filepath, 'r') as f:
    content = f.read()

new_tests = """    @Test
    public void testRustPhysicalMachineSafeProbeGateFlagOffByDefault() {
        assertFalse(V2SubmitFeatureFlags.isRustPhysicalMachineSafeProbeGateEnabled(), "Flag should be off by default");
    }

    @Test
    public void testRustPortableIpcProbeOnlyVerificationFlagOffByDefault() {
        assertFalse(V2SubmitFeatureFlags.isRustPortableIpcProbeOnlyVerificationEnabled(), "Flag should be off by default");
    }

    @Test
    public void testDemoNotLockdownFinalDecisionGateFlagOffByDefault() {
        assertFalse(V2SubmitFeatureFlags.isDemoNotLockdownFinalDecisionGateEnabled(), "Flag should be off by default");
    }
"""

last_brace_idx = content.rfind('}')
if last_brace_idx != -1:
    content = content[:last_brace_idx] + new_tests + '\n' + content[last_brace_idx:]

with open(filepath, 'w') as f:
    f.write(content)
print('Updated V2SubmitFeatureFlagsTest.java')
