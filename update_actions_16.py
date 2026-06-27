filepath = 'src/main/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitActions.java'
with open(filepath, 'r') as f:
    content = f.read()

new_actions = """    public static final String EXAM_SUBMIT_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE = "EXAM_SUBMIT_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE";
    public static final String EXAM_SUBMIT_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFY = "EXAM_SUBMIT_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFY";
    public static final String EXAM_SUBMIT_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE = "EXAM_SUBMIT_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE";
"""

last_brace_idx = content.rfind('}')
if last_brace_idx != -1:
    content = content[:last_brace_idx] + new_actions + '\n' + content[last_brace_idx:]

with open(filepath, 'w') as f:
    f.write(content)
print('Updated V2SubmitActions.java')
