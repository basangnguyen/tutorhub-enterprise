import os

path = r'D:\Ban_sao_du_an\src\main\java\com\mycompany\tutorhub_enterprise\server\ClientHandler.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

snippet = '''                case com.mycompany.tutorhub_enterprise.server.services.V2SubmitActions.EXAM_SUBMIT_V2_RUST_LOCKDOWN_CORE_PROBE: {
                    try {
                        com.mycompany.tutorhub_enterprise.server.services.V2RustLockdownCoreProbeService svc = new com.mycompany.tutorhub_enterprise.server.services.V2RustLockdownCoreProbeService();
                        com.mycompany.tutorhub_enterprise.server.services.V2RustLockdownCoreProbeResult r = svc.runProbe();
                        if (r.isSuccess()) {
                            Packet res = new Packet(com.mycompany.tutorhub_enterprise.server.services.V2SubmitActions.EXAM_SUBMIT_V2_RUST_LOCKDOWN_CORE_PROBE + "_OK", gson.toJson(r));
                            socket.send(gson.toJson(res));
                        } else {
                            Packet res = new Packet(com.mycompany.tutorhub_enterprise.server.services.V2SubmitActions.EXAM_SUBMIT_V2_RUST_LOCKDOWN_CORE_PROBE + "_ERROR", r.getErrorCode() + "|" + gson.toJson(r));
                            socket.send(gson.toJson(res));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Packet res = new Packet(com.mycompany.tutorhub_enterprise.server.services.V2SubmitActions.EXAM_SUBMIT_V2_RUST_LOCKDOWN_CORE_PROBE + "_ERROR", "Exception during rust lockdown core probe");
                        socket.send(gson.toJson(res));
                    }
                    break;
                }
'''

if 'EXAM_SUBMIT_V2_RUST_LOCKDOWN_CORE_PROBE' not in content:
    target = 'case com.mycompany.tutorhub_enterprise.server.services.V2SubmitActions.EXAM_SUBMIT_V2_FINAL_SIGN_OFF_GATE: {'
    if target in content:
        # find the end of this case block
        idx = content.find(target)
        idx_break = content.find('break;', idx)
        idx_end = content.find('}', idx_break) + 1
        
        content = content[:idx_end] + '\n\n' + snippet + content[idx_end:]
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print('Injected successfully.')
    else:
        print('Target not found.')
else:
    print('Already injected.')
