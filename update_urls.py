import os

replace_pairs = [
    ("hocba299-3-tutorhub-vscode.hf.space", "hocbatrolai293-tutorhub-vscode.hf.space"),
    ("hocba299-3-tutorhub-ai.hf.space", "hocbatrolai293-tutorhub-ai.hf.space")
]

files = [
    r'd:\Ban_sao_du_an\src\main\resources\html\tldraw_board.html',
    r'd:\Ban_sao_du_an\src\main\resources\html\tldraw_board_v2.html',
    r'd:\Ban_sao_du_an\src\main\resources\html\index.html',
    r'd:\Ban_sao_du_an\src\main\resources\frontend-board\index.html',
    r'd:\Ban_sao_du_an\src\main\java\com\mycompany\tutorhub_enterprise\client\search\LensResultPanel.java',
    r'd:\Ban_sao_du_an\src\main\java\com\mycompany\tutorhub_enterprise\client\search\GlobalSearchBar.java',
    r'd:\Ban_sao_du_an\src\main\java\com\mycompany\tutorhub_enterprise\client\LavieChatWidget.java',
    r'd:\Ban_sao_du_an\src\main\java\com\mycompany\tutorhub_enterprise\client\ai\LavieAiService.java',
    r'd:\Ban_sao_du_an\src\main\java\com\mycompany\tutorhub_enterprise\client\ChatTab.java'
]

for filepath in files:
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        changed = False
        for old_str, new_str in replace_pairs:
            if old_str in content:
                content = content.replace(old_str, new_str)
                changed = True
                
        if changed:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print("Updated", filepath)
