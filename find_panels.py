import sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

with open(r'D:\Ban_sao_du_an\src\main\resources\html\tldraw_board_v2.html', 'r', encoding='utf-8') as f:
    html = f.read()

results = []

# Find context around toggleScreenShare to understand where to inject startVideoCall
idx_ts = html.find('function toggleScreenShare()')
results.append("=== BEFORE toggleScreenShare ===")
results.append(html[idx_ts-300:idx_ts+50])

# Find openPhetModal context
idx_phet = html.find('function openPhetModal()')
results.append("\n=== openPhetModal context ===")
results.append(html[idx_phet:idx_phet+400])

# Search for judge, arena, code-panel existing elements
for kw in ['judge', 'arena', 'code-panel', 'code-modal', 'judge-panel', 'judgePanel', 'isVideoCalling', 'localVideo']:
    idx = html.lower().find(kw.lower())
    if idx != -1:
        results.append(f'\n=== Found [{kw}] at {idx} ===')
        results.append(html[idx-50:idx+300])

with open(r'D:\Ban_sao_du_an\panels_report.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(results))
print("Done")
