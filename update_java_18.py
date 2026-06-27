import re

# Update V2SubmitFeatureFlags.java
filepath = 'src/main/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitFeatureFlags.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

new_flags = """    public static final String FLAG_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE = "tse.v2.demoRegressionSecurityRecheckGate.enabled";
    public static final String FLAG_V2_DEMO_PACKAGE_FREEZE_GATE = "tse.v2.demoPackageFreezeGate.enabled";
"""
new_methods = """    public static boolean isDemoRegressionSecurityRecheckGateEnabled() {
        return getFlagValue(FLAG_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE, false);
    }
    public static boolean isDemoPackageFreezeGateEnabled() {
        return getFlagValue(FLAG_V2_DEMO_PACKAGE_FREEZE_GATE, false);
    }
"""

idx = content.find('public static boolean')
content = content[:idx] + new_flags + "\n    " + content[idx:]
idx2 = content.rfind('}')
content = content[:idx2] + new_methods + "\n" + content[idx2:]
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Update V2SubmitActions.java
filepath = 'src/main/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitActions.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

new_actions = """    public static final String EXAM_SUBMIT_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE = "EXAM_SUBMIT_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE";
    public static final String EXAM_SUBMIT_V2_DEMO_PACKAGE_FREEZE_GATE = "EXAM_SUBMIT_V2_DEMO_PACKAGE_FREEZE_GATE";
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
                case V2SubmitActions.EXAM_SUBMIT_V2_DEMO_REGRESSION_SECURITY_RECHECK_GATE: {
                    com.mycompany.tutorhub_enterprise.server.services.V2DemoRegressionSecurityRecheckGateService service = new com.mycompany.tutorhub_enterprise.server.services.V2DemoRegressionSecurityRecheckGateService();
                    com.mycompany.tutorhub_enterprise.server.services.V2DemoRegressionSecurityRecheckGateResult result = service.checkGate();
                    sendPacket(new Packet(req.getType(), new com.google.gson.Gson().toJson(result)));
                    break;
                }
                case V2SubmitActions.EXAM_SUBMIT_V2_DEMO_PACKAGE_FREEZE_GATE: {
                    com.mycompany.tutorhub_enterprise.server.services.V2DemoPackageFreezeGateService service = new com.mycompany.tutorhub_enterprise.server.services.V2DemoPackageFreezeGateService();
                    com.mycompany.tutorhub_enterprise.server.services.V2DemoPackageFreezeGateResult result = service.checkGate();
                    sendPacket(new Packet(req.getType(), new com.google.gson.Gson().toJson(result)));
                    break;
                }
"""

marker = 'case V2SubmitActions.EXAM_SUBMIT_V2_DEMO_HANDOFF_GATE:'
idx = content.find(marker)
if idx != -1:
    end_of_case = content.find('break;', idx) + len('break;') + 1
    next_brace = content.find('}', end_of_case)
    if next_brace != -1 and content[end_of_case:next_brace].strip() == '':
        end_of_case = next_brace + 1
    content = content[:end_of_case] + new_cases + content[end_of_case:]

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated Java sources phase 18")
