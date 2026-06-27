import re

filepath = 'src/main/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitFeatureFlags.java'
with open(filepath, 'r') as f:
    content = f.read()

# Add new flags
new_flags = """    public static final String FLAG_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE = "tse.v2.rustPhysicalMachineSafeProbeGate.enabled";
    public static final String FLAG_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFICATION = "tse.v2.rustPortableIpcProbeOnlyVerification.enabled";
    public static final String FLAG_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE = "tse.v2.demoNotLockdownFinalDecisionGate.enabled";
"""

new_methods = """    public static boolean isRustPhysicalMachineSafeProbeGateEnabled() {
        return getFlagValue(FLAG_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE, false);
    }

    public static boolean isRustPortableIpcProbeOnlyVerificationEnabled() {
        return getFlagValue(FLAG_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFICATION, false);
    }

    public static boolean isDemoNotLockdownFinalDecisionGateEnabled() {
        return getFlagValue(FLAG_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE, false);
    }
"""

# Insert flags at the end of the existing flags (before the first method)
first_method_idx = content.find('public static boolean')
if first_method_idx != -1:
    content = content[:first_method_idx] + new_flags + "\n    " + content[first_method_idx:]

# Insert methods before the final closing brace
last_brace_idx = content.rfind('}')
if last_brace_idx != -1:
    content = content[:last_brace_idx] + new_methods + "\n" + content[last_brace_idx:]

with open(filepath, 'w') as f:
    f.write(content)

print("Updated V2SubmitFeatureFlags.java")
