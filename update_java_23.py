import re

# Update V2SubmitFeatureFlags.java
filepath = 'src/main/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitFeatureFlags.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

new_flags = """    public static final String FLAG_V2_MANUAL_TRIAL_READY_TO_RUN_GATE = "tse.v2.manualTrialReadyToRunGate.enabled";
    public static final String FLAG_V2_MANUAL_TRIAL_EVIDENCE_FINALIZER = "tse.v2.manualTrialEvidenceFinalizer.enabled";
    public static final String FLAG_V2_DEMO_RC_FINAL_ACCEPTANCE_DECISION = "tse.v2.demoRcFinalAcceptanceDecision.enabled";
"""
new_methods = """    public static boolean isManualTrialReadyToRunGateEnabled() {
        return getFlagValue(FLAG_V2_MANUAL_TRIAL_READY_TO_RUN_GATE, false);
    }
    public static boolean isManualTrialEvidenceFinalizerEnabled() {
        return getFlagValue(FLAG_V2_MANUAL_TRIAL_EVIDENCE_FINALIZER, false);
    }
    public static boolean isDemoRcFinalAcceptanceDecisionEnabled() {
        return getFlagValue(FLAG_V2_DEMO_RC_FINAL_ACCEPTANCE_DECISION, false);
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

new_actions = """    public static final String EXAM_SUBMIT_V2_MANUAL_TRIAL_READY_TO_RUN_GATE = "EXAM_SUBMIT_V2_MANUAL_TRIAL_READY_TO_RUN_GATE";
    public static final String EXAM_SUBMIT_V2_MANUAL_TRIAL_EVIDENCE_FINALIZER = "EXAM_SUBMIT_V2_MANUAL_TRIAL_EVIDENCE_FINALIZER";
    public static final String EXAM_SUBMIT_V2_DEMO_RC_FINAL_ACCEPTANCE_DECISION = "EXAM_SUBMIT_V2_DEMO_RC_FINAL_ACCEPTANCE_DECISION";
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
                case V2SubmitActions.EXAM_SUBMIT_V2_MANUAL_TRIAL_READY_TO_RUN_GATE: {
                    com.mycompany.tutorhub_enterprise.server.services.V2ManualTrialReadyToRunGateService service = new com.mycompany.tutorhub_enterprise.server.services.V2ManualTrialReadyToRunGateService();
                    com.mycompany.tutorhub_enterprise.server.services.V2ManualTrialReadyToRunGateResult result = service.checkGate();
                    sendPacket(new Packet(req.getType(), new com.google.gson.Gson().toJson(result)));
                    break;
                }
                case V2SubmitActions.EXAM_SUBMIT_V2_MANUAL_TRIAL_EVIDENCE_FINALIZER: {
                    com.mycompany.tutorhub_enterprise.server.services.V2ManualTrialEvidenceFinalizerService service = new com.mycompany.tutorhub_enterprise.server.services.V2ManualTrialEvidenceFinalizerService();
                    com.mycompany.tutorhub_enterprise.server.services.V2ManualTrialEvidenceFinalizerResult result = service.checkFinalizer();
                    sendPacket(new Packet(req.getType(), new com.google.gson.Gson().toJson(result)));
                    break;
                }
                case V2SubmitActions.EXAM_SUBMIT_V2_DEMO_RC_FINAL_ACCEPTANCE_DECISION: {
                    com.mycompany.tutorhub_enterprise.server.services.V2DemoRcFinalAcceptanceDecisionService service = new com.mycompany.tutorhub_enterprise.server.services.V2DemoRcFinalAcceptanceDecisionService();
                    com.mycompany.tutorhub_enterprise.server.services.V2DemoRcFinalAcceptanceDecisionResult result = service.checkDecision();
                    sendPacket(new Packet(req.getType(), new com.google.gson.Gson().toJson(result)));
                    break;
                }
"""

marker = 'case V2SubmitActions.EXAM_SUBMIT_V2_DEMO_RC_TRIAL_COMPLETION_GATE:'
idx = content.find(marker)
if idx != -1:
    end_of_case = content.find('break;', idx) + len('break;') + 1
    next_brace = content.find('}', end_of_case)
    if next_brace != -1 and content[end_of_case:next_brace].strip() == '':
        end_of_case = next_brace + 1
    content = content[:end_of_case] + new_cases + content[end_of_case:]

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated Java sources phase 23")
