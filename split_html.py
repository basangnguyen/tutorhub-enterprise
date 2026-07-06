import os
import re

html_path = r'd:\Ban_sao_du_an\src\main\resources\html\tldraw_board_v2.html'
css_dir = r'd:\Ban_sao_du_an\src\main\resources\html\css'
js_dir = r'd:\Ban_sao_du_an\src\main\resources\html\js'

os.makedirs(css_dir, exist_ok=True)
os.makedirs(js_dir, exist_ok=True)

with open(html_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. EXTRACT CSS
css_start_tag = '    <style>\n        /* Zoom Bottom Bar */'
css_end_tag = '    </style>\n    <!-- Th'
css_start = content.find(css_start_tag)
# Try alternative
if css_start == -1:
    css_start_tag = '<style>\r\n        /* Zoom Bottom Bar */'
    css_start = content.find(css_start_tag)

if css_start != -1:
    css_end = content.find('</style>', css_start)
    css_content = content[css_start + len(css_start_tag.split('/*')[0]) : css_end].strip()
    
    with open(os.path.join(css_dir, 'board.css'), 'w', encoding='utf-8') as f:
        f.write(css_content)
    
    content = content[:css_start] + '    <link rel="stylesheet" href="/css/board.css">\n' + content[css_end+8:]
    print("Extracted board.css")

# 2. EXTRACT JS
# Use regex to find all scripts
# We need to manually identify which script is which by content
# To do this safely, we will find exactly the scripts by a signature string inside them.

def extract_script(signature, filename):
    global content
    # Find the script tag containing the signature
    pattern = r'<script[^>]*>(.*?)</script>'
    
    matches = list(re.finditer(pattern, content, re.DOTALL))
    for match in matches:
        if signature in match.group(1):
            script_content = match.group(1).strip()
            with open(os.path.join(js_dir, filename), 'w', encoding='utf-8') as f:
                f.write(script_content)
            
            # Replace in HTML
            content = content[:match.start()] + f'<script src="/js/{filename}"></script>' + content[match.end():]
            print(f"Extracted {filename}")
            return True
    return False

extract_script('function closeCodeModal()', 'apps.js')
extract_script('function showToast(message', 'ui-manager.js')
extract_script('window.setInfiniteMode =', 'board-core.js')

with open(html_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Done refactoring tldraw_board_v2.html")
