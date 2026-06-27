import re

# Update V2SubmitFeatureFlags.java
filepath = 'src/main/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitFeatureFlags.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

new_flags = """    public static final String FLAG_V2_MANUAL_TRIAL_EVIDENCE_EXECUTION_GATE = "tse.v2.manualTrialEvidenceExecutionGate.enabled";
    public static final String FLAG_V2_DEMO_RC_TRIAL_COMPLETION_GATE = "tse.v2.demoRcTrialCompletionGate.enabled";
"""
new_methods = """    public static boolean isManualTrialEvidenceExecutionGateEnabled() {
        return getFlagValue(FLAG_V2_MANUAL_TRIAL_EVIDENCE_EXECUTION_GATE, false);
    }
    public static boolean isDemoRcTrialCompletionGateEnabled() {
        return getFlagValue(FLAG_V2_DEMO_RC_TRIAL_COMPLETION_GATE, false);
    }
"""

idx = content.find('public static boolean')
content = content[:idx] + new_flags + "    " + content[idx:]
idx2 = content.rfind('}')
content = content[:idx2] + new_methods + "\n" + content[idx2:]
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Update V2SubmitActions.java
filepath = 'src/main/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitActions.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

new_actions = """    public static final String EXAM_SUBMIT_V2_MANUAL_TRIAL_EVIDENCE_EXECUTION_GATE = "EXAM_SUBMIT_V2_MANUAL_TRIAL_EVIDENCE_EXECUTION_GATE";
    public static final String EXAM_SUBMIT_V2_DEMO_RC_TRIAL_COMPLETION_GATE = "EXAM_SUBMIT_V2_DEMO_RC_TRIAL_COMPLETION_GATE";
"""
idx2 = content.rfind('}')
content = content[:idx2] + new_actions + "\n" + content[idx2:]
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Update ClientHandler.java
filepath = 'src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

new_cases = """
                case V2SubmitActions.EXAM_SUBMIT_V2_MANUAL_TRIAL_EVIDENCE_EXECUTION_GATE: {
                    com.mycompany.tutorhub_enterprise.server.services.V2ManualTrialEvidenceExecutionGateService service = new com.mycompany.tutorhub_enterprise.server.services.V2ManualTrialEvidenceExecutionGateService();
                    com.mycompany.tutorhub_enterprise.server.services.V2ManualTrialEvidenceExecutionGateResult result = service.checkGate();
                    sendPacket(new Packet(req.getType(), new com.google.gson.Gson().toJson(result)));
                    break;
                }
                case V2SubmitActions.EXAM_SUBMIT_V2_DEMO_RC_TRIAL_COMPLETION_GATE: {
                    com.mycompany.tutorhub_enterprise.server.services.V2DemoRcTrialCompletionGateService service = new com.mycompany.tutorhub_enterprise.server.services.V2DemoRcTrialCompletionGateService();
                    com.mycompany.tutorhub_enterprise.server.services.V2DemoRcTrialCompletionGateResult result = service.checkGate();
                    sendPacket(new Packet(req.getType(), new com.google.gson.Gson().toJson(result)));
                    break;
                }
"""

marker = 'case V2SubmitActions.EXAM_SUBMIT_V2_DEMO_RC_ACCEPTANCE_GATE:'
idx = content.find(marker)
if idx != -1:
    end_of_case = content.find('break;', idx) + len('break;') + 1
    next_brace = content.find('}', end_of_case)
    if next_brace != -1 and content[end_of_case:next_brace].strip() == '':
        end_of_case = next_brace + 1
    content = content[:end_of_case] + new_cases + content[end_of_case:]

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated Java sources phase 22")
