import re

filepath = 'src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

new_cases = """
                case V2SubmitActions.EXAM_SUBMIT_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE: {
                    com.mycompany.tutorhub_enterprise.server.services.V2RustPhysicalMachineSafeProbeGateService service = new com.mycompany.tutorhub_enterprise.server.services.V2RustPhysicalMachineSafeProbeGateService();
                    com.mycompany.tutorhub_enterprise.server.services.V2RustPhysicalMachineSafeProbeGateResult result = service.checkGate();
                    sendPacket(new Packet(req.getType(), new com.google.gson.Gson().toJson(result)));
                    break;
                }
                case V2SubmitActions.EXAM_SUBMIT_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFY: {
                    com.mycompany.tutorhub_enterprise.server.services.V2RustPortableIpcProbeOnlyVerificationService service = new com.mycompany.tutorhub_enterprise.server.services.V2RustPortableIpcProbeOnlyVerificationService();
                    com.mycompany.tutorhub_enterprise.server.services.V2RustPortableIpcProbeOnlyVerificationResult result = service.verify();
                    sendPacket(new Packet(req.getType(), new com.google.gson.Gson().toJson(result)));
                    break;
                }
                case V2SubmitActions.EXAM_SUBMIT_V2_DEMO_NOT_LOCKDOWN_FINAL_DECISION_GATE: {
                    com.mycompany.tutorhub_enterprise.server.services.V2DemoNotLockdownFinalDecisionGateService service = new com.mycompany.tutorhub_enterprise.server.services.V2DemoNotLockdownFinalDecisionGateService();
                    com.mycompany.tutorhub_enterprise.server.services.V2DemoNotLockdownFinalDecisionGateResult result = service.checkGate();
                    sendPacket(new Packet(req.getType(), new com.google.gson.Gson().toJson(result)));
                    break;
                }
"""

marker = 'case V2SubmitActions.EXAM_SUBMIT_V2_RUST_CORE_RECOVERY_RUNBOOK_GATE:'
idx = content.find(marker)
if idx != -1:
    end_of_case = content.find('break;', idx) + len('break;') + 1
    # Check if closing brace exists right after break
    next_brace = content.find('}', end_of_case)
    if next_brace != -1 and content[end_of_case:next_brace].strip() == '':
        end_of_case = next_brace + 1
    
    content = content[:end_of_case] + new_cases + content[end_of_case:]

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print('Updated ClientHandler.java')
