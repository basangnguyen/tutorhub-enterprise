
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

        let isCodeMode = false;

        // Lưu đường link Hugging Face Space của Sếp ở đây!
        let HUGGING_FACE_URL = window.TUTORHUB_CONFIG.VSCODE_SERVER_URL;

        function toggleCodeMode() {
            isCodeMode = !isCodeMode;
            const wrapper = document.getElementById('code-wrapper');
            const root = document.getElementById('root');
            const btn = document.getElementById('code-toggle-btn');
            
            if (isCodeMode) {
                if (wrapper) wrapper.style.display = 'flex';
                if (root) root.style.display = 'none'; // Ẩn bảng vẽ
                if (btn) btn.classList.add('active');
                
                const container = document.getElementById('editor-container');
                // Nếu chưa có iframe thì tạo, gọi qua endpoint /login để set Cookie
                if (container && container.innerHTML.trim() === '') {
                    const userName = window.currentRoom && window.currentRoom.localParticipant ? window.currentRoom.localParticipant.identity : "guest";
                    container.innerHTML = `<iframe src="${HUGGING_FACE_URL}/login?user=${userName}" style="width:100%; height:100%; border:none;"></iframe>`;
                }
            } else {
                if (wrapper) wrapper.style.display = 'none';
                if (root) root.style.display = 'block'; // Hiện lại bảng vẽ
                if (btn) btn.classList.remove('active');
            }
        }
        
        function toggleJudgePanel() {
            const panel = document.getElementById('judge-panel');
            panel.style.display = (panel.style.display === 'none') ? 'flex' : 'none';
        }

        // Giáo viên giao bài tập
        function publishProblem() {
            const desc = document.getElementById('judge-desc').value;
            const input = document.getElementById('judge-input').value;
            const expected = document.getElementById('judge-output').value;
            
            if (window.currentRoom) {
                const strData = JSON.stringify({ type: 'judge_publish', desc: desc, input: input, expected: expected });
                const encoder = new TextEncoder();
                window.currentRoom.localParticipant.publishData(encoder.encode(strData), LivekitClient.DataPacket_Kind.RELIABLE);
                alert("Đã giao bài tập cho cả lớp!");
            }
        }

        // Học sinh nộp bài
        async function submitCodeForJudging() {
            if (HUGGING_FACE_URL.includes("huggingface.co/spaces") && !HUGGING_FACE_URL.includes("tutorhub")) {
                alert("Bạn chưa cấu hình Link Máy chủ VS Code thật! Vui lòng làm theo hướng dẫn.");
                return;
            }
            
            const resultBox = document.getElementById('judge-result');
            const stdin = document.getElementById('judge-input').value;
            const expected = document.getElementById('judge-output').value.trim();
            
            resultBox.innerText = "Đang lấy mã nguồn từ VS Code Server...";
            resultBox.style.color = "#f59e0b";

            try {
                // Tải toàn bộ File từ Máy chủ Hugging Face về thông qua Proxy API
                const userName = window.currentRoom ? window.currentRoom.localParticipant.identity : "guest";
                const response = await fetch(HUGGING_FACE_URL + `/tutorhub-api/get-code?user=${userName}`);
                const resData = await response.json();
                
                if (!resData.success) {
                    throw new Error("Lỗi Server: " + resData.error);
                }
                
                const files = resData.files;
                
                // Ưu tiên nộp file main.py, nếu không có thì nộp index.js
                let mainFileName = "";
                if (files['main.py']) mainFileName = 'main.py';
                else if (files['index.js']) mainFileName = 'index.js';
                else {
                    const fileNames = Object.keys(files).filter(f => !f.includes('node_modules') && !f.includes('package.json'));
                    if (fileNames.length > 0) mainFileName = fileNames[0];
                }

                if (!mainFileName) {
                    resultBox.innerText = "❌ Lỗi: Không tìm thấy file code nào!";
                    resultBox.style.color = "#ef4444";
                    return;
                }

                let lang = "python";
                let wandboxCompiler = 'cpython-3.10.15';
                if (mainFileName.endsWith('.js')) wandboxCompiler = 'nodejs-18.20.4';
                if (mainFileName.endsWith('.java')) wandboxCompiler = 'openjdk-jdk-21+35';
                if (mainFileName.endsWith('.cpp')) wandboxCompiler = 'gcc-13.2.0';

                let codesArray = [];
                for (let f in files) {
                    if (f !== mainFileName && typeof files[f] === 'string' && !f.includes('node_modules')) {
                        codesArray.push({ file: f, code: files[f] });
                    }
                }

                resultBox.innerText = `Đang chấm điểm file ${mainFileName}...`;

                const wandboxResponse = await fetch('https://wandbox.org/api/compile.json', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        compiler: wandboxCompiler,
                        code: files[mainFileName],
                        codes: codesArray,
                        stdin: stdin
                    })
                });
                
                const data = await wandboxResponse.json();
                let output = (data.program_message || "").trim();
                
                if (output === expected) {
                    resultBox.innerText = `✅ PASSED (100 Điểm) - Đã chấm ${mainFileName}`;
                    resultBox.style.color = "#10b981";
                    showToast("Chúc mừng! Bạn đã giải thành công!", 4000);
                } else {
                    resultBox.innerText = `❌ FAILED - Output thực tế: ${output || 'Lỗi/Rỗng'}`;
                    resultBox.style.color = "#ef4444";
                }
            } catch (e) {
                resultBox.innerText = "Lỗi Server Chấm: " + e.message;
            }
        }
