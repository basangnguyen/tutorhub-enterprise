import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

with open(r'D:\Ban_sao_du_an\src\main\resources\html\tldraw_board_v2.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Find the JS function definitions to see if they're implemented
functions = ['startVideoCall', 'toggleScreenShare', 'toggleRecording', 
             'toggleRaiseHand', 'toggleReactionMenu', 'togglePeopleSidebar',
             'muteAllStudents', 'toggleCodeMode', 'openPhetModal',
             'toggleJudgePanel', 'toggleCoWatchModal']

for fn in functions:
    idx = html.find(f'function {fn}')
    if idx == -1:
        idx = html.find(f'{fn} = function')
    if idx == -1:
        idx = html.find(f'window.{fn}')
    if idx != -1:
        with open(f'D:\\Ban_sao_du_an\\fn_{fn}.txt', 'w', encoding='utf-8') as f2:
            f2.write(html[idx:idx+500])
        print(f'FOUND: {fn} at {idx}')
    else:
        print(f'NOT FOUND: {fn}')
