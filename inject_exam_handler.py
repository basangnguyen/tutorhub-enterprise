import re

with open(r'src\main\java\com\mycompany\tutorhub_enterprise\client\MainDashboard.java', 'r', encoding='utf-8') as f:
    text = f.read()

handler_target = 'case "GET_CLASSROOM_LESSONS_RESPONSE":'
if handler_target in text and 'case "GET_EXAMS_RESPONSE":' not in text:
    exam_handler = """                case "GET_EXAMS_RESPONSE":
                    if (examTab != null && packet.data != null) {
                        examTab.updateExamList((java.util.List<java.util.Map<String, Object>>) packet.data);
                    }
                    break;
"""
    text = text.replace(handler_target, exam_handler + '\n' + handler_target)
    
    with open(r'src\main\java\com\mycompany\tutorhub_enterprise\client\MainDashboard.java', 'w', encoding='utf-8') as f:
        f.write(text)
    print("Injected GET_EXAMS_RESPONSE successfully!")
else:
    print("Already injected or target not found.")
