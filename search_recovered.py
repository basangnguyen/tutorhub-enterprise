import re
with open('recovered_lines.txt', 'r', encoding='utf-8') as f:
    text = f.read()
matches = re.findall(r'case "AUTH_.*', text)
for m in matches:
    print(m)
