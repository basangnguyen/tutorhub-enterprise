import sys

files = [
    r'D:\Ban_sao_du_an\src\main\resources\html\js\roster.js',
    r'D:\Ban_sao_du_an\src\main\resources\html\js\apps.js',
    r'D:\Ban_sao_du_an\src\main\resources\html\js\livekit-manager.js',
    r'D:\Ban_sao_du_an\src\main\resources\html\js\keyboard-shortcuts.js'
]
for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    content = content.replace("window.currentUserRole === 'teacher'", "window.roomState.get('userRole') === 'teacher'")
    content = content.replace("window.currentUserRole !== 'teacher'", "window.roomState.get('userRole') !== 'teacher'")
    content = content.replace("window.currentUserRole === 'student'", "window.roomState.get('userRole') === 'student'")
    content = content.replace("role: window.currentUserRole", "role: window.roomState.get('userRole')")
    content = content.replace("displayName: window.currentUserName", "displayName: window.roomState.get('userName')")

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print('Updated global references in JS files')
