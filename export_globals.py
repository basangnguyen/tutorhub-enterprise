import re
import os

filepath = r"d:\Ban_sao_du_an\src\main\resources\html\js\board-core.js"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Match any whitespace at start of line
matches = re.findall(r'^[ \t]*(?:async )?function ([a-zA-Z0-9_]+)\(', content, re.MULTILINE)

exports = set()
for func in matches:
    if func not in ['syncStoreChangesToYjs', 'syncYjsChangesToStore', 'updateYjsAwareness', 'updateStorePresence']:
        exports.add(func)

append_content = "\n"
for func in sorted(list(exports)):
    if f"window.{func} = {func};" not in content:
        append_content += f"window.{func} = {func};\n"

if append_content != "\n":
    with open(filepath, "a", encoding="utf-8") as f:
        f.write(append_content)

print("Exported functions:", sorted(list(exports)))
