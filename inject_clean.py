import re
import os

with open('extracted_patches.txt', 'r', encoding='utf-8') as f:
    text = f.read()

parts = text.split('// HELPERS')
cases = parts[0].replace('// CASES', '').strip()
helpers = parts[1].strip() if len(parts) > 1 else ''

# Replace variable scopes `AuthRequest requestxxx = ` with just `req = ` to avoid collisions.
cases = re.sub(r'AuthRequest\s+requestx+\s*=\s*', 'req = ', cases)
# Replace `AuthRequest request =` with `req = ` too just in case
cases = re.sub(r'AuthRequest\s+request\s*=\s*', 'req = ', cases)

# Change variable references
cases = re.sub(r'requestx+', 'req', cases)
cases = cases.replace(' request.', ' req.')

# Add EXAM cases
exam_cases = """
                // --- EXAM MODULE ---
                case "CREATE_EXAM": {
                    sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamService.handleCreateExam(this.userId, packet.payload));
                    break;
                }
                case "GET_EXAMS": {
                    sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamService.handleGetExams(this.userId));
                    break;
                }
                case "ADD_EXAM_QUESTION": {
                    sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamService.handleAddQuestion(packet.payload));
                    break;
                }
                case "START_EXAM_SESSION": {
                    sendPacket(com.mycompany.tutorhub_enterprise.server.services.ExamService.handleStartSession(this.userId, packet.payload));
                    break;
                }
"""

with open(r'src\main\java\com\mycompany\tutorhub_enterprise\server\ClientHandler.java', 'r', encoding='utf-8') as f:
    ch_text = f.read()

# Add missing imports
imports = """
import com.mycompany.tutorhub_enterprise.models.auth.AuthRequest;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;
"""
if "import com.mycompany.tutorhub_enterprise.models.auth.AuthRequest;" not in ch_text:
    ch_text = ch_text.replace("import com.mycompany.tutorhub_enterprise.models.Packet;", "import com.mycompany.tutorhub_enterprise.models.Packet;\n" + imports)

# Wrap `req` declaration inside the switch block or right before it
switch_start = "switch (packet.action) {"
if "AuthRequest req;" not in ch_text:
    ch_text = ch_text.replace(switch_start, "AuthRequest req;\n            " + switch_start)


# Insert cases into switch block
switch_end_target = """                    }
                    break;
                }
            }
        } catch (Exception e) {"""

if switch_end_target in ch_text:
    replacement = "                    }\n                    break;\n                }\n" + cases + "\n" + exam_cases + "            }\n        } catch (Exception e) {"
    ch_text = ch_text.replace(switch_end_target, replacement)
else:
    print("Could not find switch end target!")

# Fix AuthResult and LoginSession in helpers
helpers = helpers.replace('AuthResult', 'AuthService.AuthResult')
helpers = helpers.replace('LoginSession', 'AuthService.LoginSession')

# Insert helpers
helper_target = """    public void sendPacket(Packet packet) {"""
if helper_target in ch_text:
    ch_text = ch_text.replace(helper_target, helpers + "\n\n" + helper_target)
else:
    print("Could not find helper target!")

with open(r'src\main\java\com\mycompany\tutorhub_enterprise\server\ClientHandler.java', 'w', encoding='utf-8') as f:
    f.write(ch_text)

print("Injected cleanly successfully!")
