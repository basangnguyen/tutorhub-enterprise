import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

with open(r'D:\Ban_sao_du_an\src\main\resources\html\tldraw_board_v2.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Find the HTML of bottom bar
idx = html.find('id="zoom-bottom-bar"')
print('Found at:', idx)
snippet = html[idx:idx+4000]
# Write to file instead
with open(r'D:\Ban_sao_du_an\bottom_bar_snippet.txt', 'w', encoding='utf-8') as f:
    f.write(snippet)
print('Saved to bottom_bar_snippet.txt')

# Also look for onclick handlers
import re
handlers = re.findall(r'onclick="([^"]+)"', snippet)
print('Handlers:')
for h in handlers:
    print(' -', h)
