import sys

files = [
    r'D:\Ban_sao_du_an\src\main\resources\html\index.html',
    r'D:\Ban_sao_du_an\src\main\resources\html\tldraw_board.html',
    r'D:\Ban_sao_du_an\src\main\resources\html\tldraw_board_v2.html'
]
for file_path in files:
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        content = content.replace("window.currentUserRole === 'teacher'", "window.roomState.get('userRole') === 'teacher'")
        content = content.replace("window.currentUserRole !== 'teacher'", "window.roomState.get('userRole') !== 'teacher'")
        content = content.replace("window.currentUserRole === 'student'", "window.roomState.get('userRole') === 'student'")
        content = content.replace("role: window.currentUserRole", "role: window.roomState.get('userRole')")
        content = content.replace("displayName: window.currentUserName", "displayName: window.roomState.get('userName')")

        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
    except FileNotFoundError:
        pass

print('Updated HTML files references')
