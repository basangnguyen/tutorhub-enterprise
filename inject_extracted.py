import re

with open('extracted_patches.txt', 'r', encoding='utf-8') as f:
    text = f.read()

parts = text.split('// HELPERS')
cases = parts[0].replace('// CASES', '').strip()
helpers = parts[1].strip() if len(parts) > 1 else ''

# Clean up variables
cases = re.sub(r'requestx+', 'req', cases)

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

# Insert helpers
helper_target = """    public void sendPacket(Packet packet) {"""
if helper_target in ch_text:
    ch_text = ch_text.replace(helper_target, helpers + "\n\n" + helper_target)
else:
    print("Could not find helper target!")

with open(r'src\main\java\com\mycompany\tutorhub_enterprise\server\ClientHandler.java', 'w', encoding='utf-8') as f:
    f.write(ch_text)

print("Injected successfully!")
