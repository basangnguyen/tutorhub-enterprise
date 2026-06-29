import re

file_path = "d:/Ban_sao_du_an/src/main/resources/tse/quiz.html"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

print("Found </head>:", "</head>" in content)
print("Found <!-- NAV SHEET -->:", "<!-- NAV SHEET -->" in content)
print("Found </body>:", "</body>" in content)
print("Found fetchAndStart:", "function fetchAndStart" in content)
print("Found best-:", "best-\" + deck.id" in content or 'best-${deck.id}' in content)
