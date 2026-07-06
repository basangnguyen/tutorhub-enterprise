import re

filepath = r'd:\Ban_sao_du_an\src\main\resources\html\tldraw_board_v2.html'

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

changes = 0

# ============================================================
# 1. ADD CSS for Apps Popup Menu (after .zoom-bottom-btn:hover::after)
# ============================================================
apps_css = """
        /* Apps Popup Menu */
        #apps-popup {
            display: none;
            position: absolute;
            bottom: 72px;
            left: 50%;
            transform: translateX(-50%);
            background: #252526;
            border: 1px solid #444;
            border-radius: 12px;
            padding: 12px;
            z-index: 10001;
            box-shadow: 0 -8px 30px rgba(0,0,0,0.5);
            min-width: 340px;
        }
        #apps-popup.show { display: block; }
        
        .apps-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 6px;
        }
        
        .apps-grid-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 5px;
            padding: 10px 6px;
            border-radius: 8px;
            cursor: pointer;
            background: transparent;
            border: none;
            color: #d1d5db;
            font-family: sans-serif;
            transition: all 0.15s ease;
        }
        .apps-grid-item:hover {
            background: rgba(255,255,255,0.1);
            color: #fff;
            transform: translateY(-1px);
        }
        .apps-grid-item i {
            font-size: 20px;
        }
        .apps-grid-item span {
            font-size: 10px;
            white-space: nowrap;
        }
        .apps-grid-item.active-app {
            color: #10b981;
            background: rgba(16, 185, 129, 0.1);
        }

        /* Apps popup arrow */
        #apps-popup::after {
            content: '';
            position: absolute;
            bottom: -8px;
            left: 50%;
            transform: translateX(-50%);
            border-left: 8px solid transparent;
            border-right: 8px solid transparent;
            border-top: 8px solid #252526;
        }
"""

css_anchor = "        .zoom-bottom-btn:hover::after {\n            opacity: 1;\n        }"
if css_anchor in content:
    content = content.replace(css_anchor, css_anchor + "\n" + apps_css)
    changes += 1
    print("[OK] Added Apps popup CSS")
else:
    print("[WARN] CSS anchor not found")

# ============================================================
# 2. REPLACE the entire toolbar section
# ============================================================
# Find the old toolbar HTML block
old_toolbar_start = '    <!-- Zoom Bottom Bar (Meeting Controls) -->\r\n    <div id="zoom-bottom-bar">'
old_toolbar_end = '    </div>\r\n\r\n    <!-- Video Sidebar'

# Also try with \n only (no \r)
old_toolbar_start_unix = old_toolbar_start.replace('\r\n', '\n')
old_toolbar_end_unix = old_toolbar_end.replace('\r\n', '\n')

# Determine line ending style
if old_toolbar_start in content:
    nl = '\r\n'
elif old_toolbar_start_unix in content:
    nl = '\n'
    old_toolbar_start = old_toolbar_start_unix
    old_toolbar_end = old_toolbar_end_unix
else:
    print("[WARN] Could not find toolbar start marker")
    nl = '\r\n'

# Build new toolbar HTML
new_toolbar = f'''    <!-- Zoom Bottom Bar (Meeting Controls) -->
    <div id="zoom-bottom-bar">
        <!-- Nhom Audio/Video (Trai) -->
        <button id="mute-all-btn" class="zoom-bottom-btn" data-tooltip="Tat Mic tat ca" onclick="muteAllStudents()">
            <i class="fa-solid fa-microphone-lines-slash"></i>
            <span>Mute All</span>
        </button>
        <button id="start-video-btn" class="zoom-bottom-btn" data-tooltip="Bat/Tat Camera" onclick="startVideoCall()">
            <i class="fa-solid fa-video"></i>
            <span>Camera</span>
        </button>

        <div class="zoom-bottom-divider"></div>

        <!-- Nhom Tuong tac (Giua) -->
        <button id="share-screen-btn" class="zoom-bottom-btn" data-tooltip="Chia se man hinh" onclick="toggleScreenShare()">
            <i class="fa-solid fa-desktop"></i>
            <span>Share</span>
        </button>
        <button id="raise-hand-btn" class="zoom-bottom-btn" data-tooltip="Gio tay phat bieu" onclick="toggleRaiseHand()">
            <i class="fa-regular fa-hand"></i>
            <span>Raise</span>
        </button>
        <button id="react-btn" class="zoom-bottom-btn" data-tooltip="Tha cam xuc" onclick="toggleReactionMenu(event)">
            <i class="fa-regular fa-face-smile"></i>
            <span>React</span>
        </button>

        <div class="zoom-bottom-divider"></div>

        <!-- Nut Apps (Popup Menu) -->
        <button id="apps-toggle-btn" class="zoom-bottom-btn" data-tooltip="Cong cu mo rong" onclick="toggleAppsMenu(event)" style="position:relative;">
            <i class="fa-solid fa-grip"></i>
            <span>Apps</span>
        </button>

        <div class="zoom-bottom-divider"></div>

        <!-- Nhom He thong (Phai) -->
        <button class="zoom-bottom-btn" data-tooltip="Danh sach Lop" onclick="togglePeopleSidebar()" style="position: relative;">
            <i class="fa-solid fa-user-group"></i>
            <span>People</span>
            <span id="roster-badge" style="position: absolute; top: 5px; right: 5px; background: #ef4444; color: white; border-radius: 50%; width: 16px; height: 16px; font-size: 10px; display: flex; align-items: center; justify-content: center; font-weight: bold; border: 2px solid #1e1e1e; display: none;">1</span>
        </button>
        <button class="zoom-bottom-btn" data-tooltip="Cai dat" onclick="toggleBoardSettingsModal()">
            <i class="fa-solid fa-gear"></i>
            <span>Settings</span>
        </button>

        <button class="zoom-bottom-btn danger" data-tooltip="Thoat lop hoc" onclick="triggerCloseBoard()">
            <i class="fa-solid fa-right-from-bracket"></i>
            <span>Leave</span>
        </button>
    </div>

    <!-- Apps Popup Menu -->
    <div id="apps-popup">
        <div class="apps-grid">
            <button class="apps-grid-item" onclick="toggleCodeMode(); closeAppsMenu();" id="app-code">
                <i class="fa-solid fa-code"></i>
                <span>Code</span>
            </button>
            <button class="apps-grid-item" onclick="openPhetModal(); closeAppsMenu();">
                <i class="fa-solid fa-flask"></i>
                <span>Thi nghiem</span>
            </button>
            <button class="apps-grid-item" onclick="toggleJudgePanel(); closeAppsMenu();">
                <i class="fa-solid fa-trophy"></i>
                <span>Arena</span>
            </button>
            <button class="apps-grid-item" onclick="toggleCoWatchModal(); closeAppsMenu();">
                <i class="fa-solid fa-tv"></i>
                <span>YouTube</span>
            </button>
            <button class="apps-grid-item" onclick="document.getElementById('upload-doc-input').click(); closeAppsMenu();">
                <i class="fa-regular fa-file-pdf"></i>
                <span>Files</span>
            </button>
            <button class="apps-grid-item" onclick="document.getElementById('math-modal').style.display='block'; closeAppsMenu();">
                <i class="fa-solid fa-square-root-variable"></i>
                <span>Math</span>
            </button>
            <button class="apps-grid-item" onclick="document.getElementById('mermaid-modal').style.display='block'; closeAppsMenu();">
                <i class="fa-solid fa-diagram-project"></i>
                <span>Diagram</span>
            </button>
            <button class="apps-grid-item" onclick="if(window.addCodeNode) window.addCodeNode(); closeAppsMenu();">
                <i class="fa-solid fa-file-code"></i>
                <span>Snippet</span>
            </button>
            <button class="apps-grid-item" onclick="if(window.addQuizNode) window.addQuizNode(); closeAppsMenu();">
                <i class="fa-solid fa-list-check"></i>
                <span>Quiz</span>
            </button>
            <button class="apps-grid-item" onclick="if(window.cefQuery) window.cefQuery({{request: 'GET_BOARDS_FOR_PICKER', persistent: false, onSuccess: function(r){{}}, onFailure: function(e,m){{}}}}); closeAppsMenu();">
                <i class="fa-solid fa-folder-open"></i>
                <span>Kho bang</span>
            </button>
            <button class="apps-grid-item" onclick="if(window.cefQuery) window.cefQuery({{request: 'TOGGLE_PERMISSION_PANEL', persistent: false, onSuccess: function(r){{}}, onFailure: function(e,m){{}}}}); closeAppsMenu();">
                <i class="fa-solid fa-users-gear"></i>
                <span>Members</span>
            </button>
            <button class="apps-grid-item" onclick="toggleRecording(); closeAppsMenu();" id="app-record">
                <i class="fa-solid fa-record-vinyl"></i>
                <span>Record</span>
            </button>
            <button class="apps-grid-item" onclick="triggerSaveBoard(); closeAppsMenu();">
                <i class="fa-solid fa-cloud-arrow-up"></i>
                <span>Save</span>
            </button>
        </div>
    </div>'''

# Replace the toolbar block
start_idx = content.find(old_toolbar_start)
if start_idx == -1:
    # Try a simpler search
    start_idx = content.find('<!-- Zoom Bottom Bar (Meeting Controls) -->')

if start_idx != -1:
    end_marker = '<!-- Video Sidebar'
    end_idx = content.find(end_marker, start_idx)
    if end_idx != -1:
        # Go back to find the closing </div> before Video Sidebar
        # We want to replace everything from toolbar start to just before Video Sidebar comment
        old_block = content[start_idx:end_idx]
        content = content[:start_idx] + new_toolbar + "\n\n    " + content[end_idx:]
        changes += 1
        print("[OK] Replaced toolbar HTML")
    else:
        print("[WARN] Could not find toolbar end marker")
else:
    print("[WARN] Could not find toolbar start marker")

# ============================================================
# 3. ADD JavaScript for Apps popup toggle (before the existing toggleCodeMode)
# ============================================================
apps_js = """
        // ==========================================
        // APPS POPUP MENU
        // ==========================================
        function toggleAppsMenu(event) {
            event.stopPropagation();
            const popup = document.getElementById('apps-popup');
            popup.classList.toggle('show');
        }
        function closeAppsMenu() {
            const popup = document.getElementById('apps-popup');
            popup.classList.remove('show');
        }
        // Click outside to close
        document.addEventListener('click', function(e) {
            const popup = document.getElementById('apps-popup');
            const btn = document.getElementById('apps-toggle-btn');
            if (popup && btn && !popup.contains(e.target) && !btn.contains(e.target)) {
                popup.classList.remove('show');
            }
        });
"""

js_anchor = "        // ==========================================\r\n        // CODE EDITOR"
js_anchor_unix = js_anchor.replace('\r\n', '\n')

if js_anchor in content:
    content = content.replace(js_anchor, apps_js + "\n" + js_anchor)
    changes += 1
    print("[OK] Added Apps popup JavaScript")
elif js_anchor_unix in content:
    content = content.replace(js_anchor_unix, apps_js + "\n" + js_anchor_unix)
    changes += 1
    print("[OK] Added Apps popup JavaScript (unix)")
else:
    # Try alternate anchor
    alt_anchor = "        let isCodeMode = false;"
    if alt_anchor in content:
        content = content.replace(alt_anchor, apps_js + "\n" + alt_anchor)
        changes += 1
        print("[OK] Added Apps popup JavaScript (alt anchor)")
    else:
        print("[WARN] JS anchor not found")

# ============================================================
# SAVE
# ============================================================
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

print(f"\n=== Done! {changes} changes applied ===")
