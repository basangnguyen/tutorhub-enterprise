import re

with open(r'src\main\java\com\mycompany\tutorhub_enterprise\client\MainDashboard.java', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Add ExamTab variable
if 'private com.mycompany.tutorhub_enterprise.client.exam.ExamTab examTab;' not in text:
    text = text.replace('private ReelsTabPanel reelsTab;', 'private ReelsTabPanel reelsTab;\n    private com.mycompany.tutorhub_enterprise.client.exam.ExamTab examTab;')

# 2. Initialize ExamTab and add to mainCardPanel
init_target = 'mainCardPanel.add(reelsTab, "Reels");'
if init_target in text and 'examTab = new com.mycompany.tutorhub_enterprise.client.exam.ExamTab' not in text:
    init_exam = '        examTab = new com.mycompany.tutorhub_enterprise.client.exam.ExamTab(this.currentUserId, this.currentUserRole, this.networkManager);\n        mainCardPanel.add(examTab, "Exam");'
    text = text.replace(init_target, init_target + '\n\n' + init_exam)

# 3. Add to Sidebar menu
menu_target = 'menuPanel.add(createMenuItem("Bảng tin lớp", "home", "Home", 0));'
# The encoding might be wrong, so let's just look for "Home", 0
menu_target2 = 'menuPanel.add(createMenuItem("B\\xc3\\xa0ng tin l\\xe1\\xbb\\x9bp", "home", "Home", 0));'

lines = text.split('\n')
for i, line in enumerate(lines):
    if '"Home", 0' in line and 'createMenuItem' in line:
        # insert after this line
        if 'Exam' not in text:
            lines.insert(i + 1, '        menuPanel.add(createMenuItem("Kỳ thi", "text", "Exam", 0));')
        break

text = '\n'.join(lines)

with open(r'src\main\java\com\mycompany\tutorhub_enterprise\client\MainDashboard.java', 'w', encoding='utf-8') as f:
    f.write(text)

print("Injected ExamTab successfully!")
