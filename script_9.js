
        const root = ReactDOM.createRoot(document.getElementById('root'));
        
        let excalidrawAPI = null;

        const App = () => {
            return React.createElement(
                React.Fragment,
                null,
                React.createElement(
                    "div",
                    { style: { height: "100vh" } },
                    React.createElement(ExcalidrawLib.Excalidraw, {
                        excalidrawAPI: (api) => {
                            excalidrawAPI = api;
                            window.excalidrawAPI = api;
                            // Cấu hình nét vẽ thẳng (giống tldraw) thay vì sketchy
                            api.updateScene({
                                appState: { 
                                    currentItemRoughness: 0, // Nét thẳng, chuẩn vector
                                    currentItemRoundness: "round", // Bo cong tròn trịa
                                    currentItemFontFamily: 1, // Font hiện đại
                                }
                            });
                            if (window.cefQuery) {
                                window.cefQuery({ 
                                    request: 'EDITOR_READY', 
                                    persistent: false, 
                                    onSuccess: function(r){}, 
                                    onFailure: function(e,m){} 
                                });
                            }
                        },
                        UIOptions: {
                            canvasActions: { loadScene: false, export: false, saveAsImage: false }
                        },
                        onChange: (elements, appState, files) => {
                            if (!window.currentRoom || window.isReceivingSync) return;
                            
                            // Throttle logic
                            const now = Date.now();
                            if (!window.lastDrawSync) window.lastDrawSync = 0;
                            if (now - window.lastDrawSync < 100) { // 100ms throttle
                                if (window.syncTimeout) clearTimeout(window.syncTimeout);
                                window.syncTimeout = setTimeout(() => window.performDrawSync(elements), 100);
                                return;
                            }
                            window.lastDrawSync = now;
                            window.performDrawSync(elements);
                        }
                    })
                )
            );
        };
        
        window.performDrawSync = async (elements) => {
            if (!window.currentRoom) return;
            // Diffing
            if (!window.lastBroadcastElementsMap) window.lastBroadcastElementsMap = new Map();
            const changed = [];
            for (const el of elements) {
                const lastVersion = window.lastBroadcastElementsMap.get(el.id);
                if (!lastVersion || el.version > lastVersion) {
                    changed.push(el);
                    window.lastBroadcastElementsMap.set(el.id, el.version);
                }
            }
            if (changed.length > 0) {
                try {
                    const payload = JSON.stringify({ type: 'draw_sync', elements: changed });
                    const encoder = new TextEncoder();
                    await window.currentRoom.localParticipant.publishData(encoder.encode(payload), { reliable: true });
                } catch (e) {
                    console.error("Lỗi đồng bộ nét vẽ:", e);
                }
            }
        };

        root.render(React.createElement(App));

        window.setPaperMode = (mode, isDark) => {
            document.body.className = '';
            if (mode === 'lined') {
                document.body.classList.add(isDark ? 'paper-dark-lined' : 'paper-lined');
            } else if (mode === 'grid') {
                document.body.classList.add(isDark ? 'paper-dark-grid' : 'paper-grid');
            }
            if (window.excalidrawAPI) {
                window.excalidrawAPI.updateScene({
                    appState: { 
                        theme: isDark ? "dark" : "light",
                        viewBackgroundColor: "transparent" 
                    }
                });
            }
        };

        window.setInfiniteMode = (isInfinite) => {
            // Excalidraw mặc định là Infinite
        };

        // Hàm export dữ liệu mới kết hợp Thumbnail do JS tạo
        window.requestSaveBoardAndThumbnail = async () => {
            if (!window.excalidrawAPI) return;
            const elements = window.excalidrawAPI.getSceneElements();
            const appState = window.excalidrawAPI.getAppState();
            
            const files = window.excalidrawAPI.getFiles();
            const jsonStr = JSON.stringify({ elements: elements, appState: { theme: appState.theme, viewBackgroundColor: appState.viewBackgroundColor }, files: files });
            let thumbBase64 = "";

            try {
                // Xuất ảnh PNG độ phân giải chuẩn, không bị nhiễu UI
                const blob = await ExcalidrawLib.exportToBlob({
                    elements,
                    appState: { ...appState, exportWithDarkMode: appState.theme === 'dark' },
                    files: window.excalidrawAPI.getFiles(),
                    mimeType: "image/png"
                });
                
                // Đọc blob thành base64
                const reader = new FileReader();
                reader.readAsDataURL(blob);
                reader.onloadend = () => {
                    const base64data = reader.result.split(',')[1];
                    if (window.cefQuery) {
                        window.cefQuery({ request: 'SAVE_DATA:' + jsonStr + '|||THUMBNAIL_SEP|||' + base64data, persistent: false, onSuccess: function(r){}, onFailure: function(e,m){} });
                    }
                };
            } catch(e) {
                console.error("Export thumbnail failed", e);
                if (window.cefQuery) {
                    window.cefQuery({ request: 'SAVE_DATA:' + jsonStr + '|||THUMBNAIL_SEP|||' + thumbBase64, persistent: false, onSuccess: function(r){}, onFailure: function(e,m){} });
                }
            }
        };

        let ws = null;
        let isSyncing = false;

        window.loadBoardData = (jsonStr, boardId) => {
            window.currentBoardId = boardId; // Lưu lại để LiveKit dùng
            if (!window.excalidrawAPI) return;
            try {
                if (jsonStr && jsonStr !== 'null') {
                    const data = JSON.parse(jsonStr);
                    if (data.files && Object.keys(data.files).length > 0) {
                        window.excalidrawAPI.addFiles(Object.values(data.files));
                    }
                    window.excalidrawAPI.updateScene({
                        elements: data.elements,
                        appState: data.appState
                    });
                }
                
                // Real-time Sync WebSocket
                if (ws) ws.close();
                if (boardId) {
                    ws = new WebSocket('ws://localhost:1234/?roomId=' + boardId);
                    
                    ws.onopen = () => console.log('Connected to Sync Server for room ' + boardId);
                    
                    ws.onmessage = (event) => {
                        if (!window.excalidrawAPI) return;
                        try {
                            const data = JSON.parse(event.data);
                            if (data.type === 'sync') {
                                isSyncing = true;
                                window.excalidrawAPI.updateScene({
                                    elements: data.elements,
                                    commitToHistory: false // Không thêm vào undo history khi nhận từ remote
                                });
                                // Reset flag sau khi updateScene xong
                                setTimeout(() => isSyncing = false, 50);
                            }
                        } catch (e) {
                            console.error('Sync error', e);
                        }
                    };

                    window.excalidrawAPI.onChange((elements, appState) => {
                        if (isSyncing) return;
                        if (ws && ws.readyState === WebSocket.OPEN) {
                            // Chỉ broadcast elements để giảm tải, appState ko cần broadcast
                            ws.send(JSON.stringify({
                                type: 'sync',
                                elements: elements
                            }));
                        }
                    });
                }
            } catch (e) {
                console.error("Invalid board data", e);
            }
            
            // Auto-save
            if (!window.autoSaveInterval) {
                window.autoSaveInterval = setInterval(() => {
                    console.log('Auto saving...');
                    window.requestSaveBoardAndThumbnail();
                }, 300000);
            }
            connectToLiveKit();
        };

        let livekitRoom = null;
        
        async function connectToLiveKit() {
            if (window.currentRoom) return; // Đã kết nối

            const LIVEKIT_URL = 'wss://tutorhub-enterprise-q820cqx7.livekit.cloud'; 
            
            try {
                const currentRoomId = window.currentBoardId || 'default-room';
                const res = await fetch(`http://localhost:1234/livekit/token?room=${currentRoomId}&username=Gia+Su`);
                const data = await res.json();
                
                if (!data.token) {
                    console.error('Lỗi lấy token từ server!');
                    return;
                }

                const room = new LivekitClient.Room({
                    adaptiveStream: true,
                    dynacast: true,
                });
                window.currentRoom = room;

                room.on(LivekitClient.RoomEvent.TrackSubscribed, (track, publication, participant) => {
                    if (track.kind === LivekitClient.Track.Kind.Video || track.kind === LivekitClient.Track.Kind.Audio) {
                        const element = track.attach();
                        
                        if (track.source === LivekitClient.Track.Source.ScreenShare) {
                            element.style.width = '80vw';
                            element.style.height = '70vh';
                            element.style.objectFit = 'contain';
                            element.style.background = '#000';
                            element.style.border = '2px solid #10b981';
                            element.style.boxShadow = '0 10px 30px rgba(0,0,0,0.5)';
                            
                            const wrapper = document.createElement('div');
                            wrapper.id = 'screenshare-' + participant.identity;
                            wrapper.style.position = 'absolute';
                            wrapper.style.top = '10%';
                            wrapper.style.left = '50%';
                            wrapper.style.transform = 'translateX(-50%)';
                            wrapper.style.zIndex = '9998';
                            wrapper.style.display = 'flex';
                            wrapper.style.flexDirection = 'column';
                            
                            const header = document.createElement('div');
                            header.style.background = '#10b981';
                            header.style.color = 'white';
                            header.style.padding = '8px 15px';
                            header.style.fontWeight = 'bold';
                            header.style.display = 'flex';
                            header.style.justifyContent = 'space-between';
                            header.style.borderTopLeftRadius = '8px';
                            header.style.borderTopRightRadius = '8px';
                            header.innerHTML = `<span>🖥 Màn hình của: ${participant.identity}</span> <span style="cursor:pointer;" onclick="this.parentElement.parentElement.remove()">✖</span>`;
                            
                            wrapper.appendChild(header);
                            wrapper.appendChild(element);
                            document.body.appendChild(wrapper);
                            
                        } else if (track.kind === LivekitClient.Track.Kind.Video) {
                            element.style.width = '160px';
                            element.style.height = '90px';
                            element.style.objectFit = 'cover';
                            element.style.borderRadius = '4px';
                            element.style.boxShadow = '0 2px 8px rgba(0,0,0,0.5)';
                            element.style.border = '2px solid #ef4444'; 
                            
                            const wrapper = document.createElement('div');
                            wrapper.id = 'participant-' + participant.identity;
                            wrapper.style.position = 'relative';
                            wrapper.classList.add('video-bubble');
                            
                            const nameTag = document.createElement('div');
                            nameTag.innerText = participant.identity;
                            nameTag.style.position = 'absolute';
                            nameTag.style.bottom = '-5px';
                            nameTag.style.background = '#ef4444';
                            nameTag.style.color = '#fff';
                            nameTag.style.padding = '2px 8px';
                            nameTag.style.borderRadius = '10px';
                            nameTag.style.fontSize = '12px';
                            nameTag.style.left = '50%';
                            nameTag.style.transform = 'translateX(-50%)';
                            
                            wrapper.appendChild(element);
                            wrapper.appendChild(nameTag);
                            document.getElementById('video-container').appendChild(wrapper);
                        } else {
                            document.getElementById('video-container').appendChild(element);
                        }
                    }
                });

                room.on(LivekitClient.RoomEvent.TrackUnsubscribed, (track, publication, participant) => {
                    track.detach();
                    
                    if (track.source === LivekitClient.Track.Source.ScreenShare) {
                        const ssElement = document.getElementById('screenshare-' + participant.identity);
                        if (ssElement) ssElement.remove();
                    } else if (track.kind === LivekitClient.Track.Kind.Video) {
                        const vidElement = document.getElementById('participant-' + participant.identity);
                        if (vidElement) vidElement.remove();
                    }
                });

                room.on(LivekitClient.RoomEvent.ParticipantDisconnected, (participant) => {
                    const wrapper = document.getElementById('participant-' + participant.identity);
                    if (wrapper) wrapper.remove();
                });

                room.on(LivekitClient.RoomEvent.DataReceived, (payload, participant, kind, topic) => {
                    try {
                        const decoder = new TextDecoder();
                        const data = JSON.parse(decoder.decode(payload));
                        if (data.type === 'chat') {
                            const senderName = participant ? participant.identity : data.sender;
                            appendChatMessage(senderName, data.time, data.text, false);
                            
                            const chatbox = document.getElementById('chatbox');
                            if (chatbox.style.display === 'none') {
                                toggleChatbox();
                            }
                        } else if (data.type === 'raise_hand') {
                            const senderName = participant ? participant.identity : data.sender;
                            showToast("Học sinh " + senderName + " đang giơ tay phát biểu! ✋", 5000);
                            
                            // Add a visual indicator to their video if exists
                            const vidBubble = document.getElementById('participant-' + senderName);
                            if (vidBubble) {
                                let handIcon = vidBubble.querySelector('.hand-icon');
                                if (!handIcon) {
                                    handIcon = document.createElement('div');
                                    handIcon.className = 'hand-icon';
                                    handIcon.innerText = '✋';
                                    handIcon.style.position = 'absolute';
                                    handIcon.style.top = '5px';
                                    handIcon.style.right = '5px';
                                    handIcon.style.fontSize = '20px';
                                    handIcon.style.background = 'rgba(255,255,255,0.8)';
                                    handIcon.style.borderRadius = '50%';
                                    handIcon.style.padding = '2px';
                                    handIcon.style.boxShadow = '0 2px 5px rgba(0,0,0,0.3)';
                                    vidBubble.appendChild(handIcon);
                                    
                                    // Auto hide after 10s
                                    setTimeout(() => {
                                        if (handIcon.parentNode) handIcon.remove();
                                    }, 10000);
                                }
                            }
                        } else if (data.type === 'mute_all') {
                            if (window.currentRoom && window.currentRoom.localParticipant) {
                                window.currentRoom.localParticipant.setMicrophoneEnabled(false).then(() => {
                                    showToast("Giáo viên đã tắt Mic của bạn để giữ trật tự!", 5000);
                                }).catch(err => {
                                    console.error("Lỗi khi bị Mute All: ", err);
                                });
                            }
                        } else if (data.type === 'draw_sync') {
                            if (window.excalidrawAPI) {
                                window.isReceivingSync = true;
                                window.excalidrawAPI.updateScene({ elements: data.elements });
                                setTimeout(() => window.isReceivingSync = false, 50);
                            }
                        } else if (data.type === 'document_sync') {
                            showToast("Giáo viên vừa tải lên 1 tài liệu mới!", 3000);
                            if (typeof insertImageToExcalidraw === 'function') {
                                insertImageToExcalidraw(data.url, data.yPos);
                            }
                        } else if (data.type === 'workspace_sync') {
                            if (!isCodeMode) {
                                toggleCodeMode();
                                showToast("Giáo viên đã mở Môi trường lập trình Workspace!", 4000);
                            }
                            
                            workspaceFiles = data.files;
                            activeFile = data.active;
                            
                            if (codeEditor) {
                                isSyncingCode = true;
                                if (codeEditor.getValue() !== workspaceFiles[activeFile]) {
                                    codeEditor.setValue(workspaceFiles[activeFile]);
                                }
                                if (document.getElementById('code-language').value !== data.language) {
                                    document.getElementById('code-language').value = data.language;
                                }
                                
                                let lang = "plaintext";
                                if (activeFile.endsWith('.py')) lang = "python";
                                else if (activeFile.endsWith('.js')) lang = "javascript";
                                else if (activeFile.endsWith('.java')) lang = "java";
                                else if (activeFile.endsWith('.cpp')) lang = "cpp";
                                monaco.editor.setModelLanguage(codeEditor.getModel(), lang);
                                
                                isSyncingCode = false;
                                
                                renderFileList();
                                renderTabs();
                            }
                        } else if (data.type === 'judge_publish') {
                            if (!isCodeMode) toggleCodeMode();
                            const panel = document.getElementById('judge-panel');
                            if (panel.style.display === 'none') panel.style.display = 'flex';
                            
                            document.getElementById('judge-desc').value = data.desc;
                            document.getElementById('judge-input').value = data.input;
                            document.getElementById('judge-output').value = data.expected;
                            showToast("Giáo viên vừa giao Bài tập mới! Hãy mở Đấu trường!", 5000);
                        } else if (data.type === 'run_output') {
                            if (!isCodeMode) toggleCodeMode();
                            document.getElementById('terminal-output').innerText = data.result;
                            document.getElementById('run-status').innerText = "Đã nhận kết quả";
                        }
                    } catch (e) {
                        console.error('Lỗi nhận data:', e);
                    }
                });

                await room.connect(LIVEKIT_URL, data.token);
                console.log("LiveKit connected successfully!");
            } catch (e) {
                console.error("Lỗi kết nối LiveKit:", e);
            }
        }

        async function startVideoCall() {
            if (!window.currentRoom) {
                alert("Đang kết nối tới phòng học, vui lòng thử lại sau vài giây...");
                return;
            }
            
            document.getElementById('start-video-btn').style.display = 'none';
            
            try {
                await window.currentRoom.localParticipant.enableCameraAndMicrophone();
                
                const localTrackPub = window.currentRoom.localParticipant.getTrackPublication(LivekitClient.Track.Source.Camera);
                if (localTrackPub && localTrackPub.track) {
                    const element = localTrackPub.track.attach();
                    element.style.width = '160px';
                    element.style.height = '90px';
                    element.style.objectFit = 'cover';
                    element.style.borderRadius = '4px';
                    element.style.boxShadow = '0 2px 8px rgba(0,0,0,0.5)';
                    element.style.border = '2px solid #3b82f6';
                    element.style.transform = 'scaleX(-1)';
                    element.style.background = '#000';
                    
                    const wrapper = document.createElement('div');
                    wrapper.style.position = 'relative';
                    
                    const nameTag = document.createElement('div');
                    nameTag.innerText = 'Bạn';
                    nameTag.style.position = 'absolute';
                    nameTag.style.bottom = '-5px';
                    nameTag.style.background = '#3b82f6';
                    nameTag.style.color = '#fff';
                    nameTag.style.padding = '2px 8px';
                    nameTag.style.borderRadius = '10px';
                    nameTag.style.fontSize = '12px';
                    nameTag.style.left = '50%';
                    nameTag.style.transform = 'translateX(-50%)';
                    
                    wrapper.appendChild(element);
                    wrapper.appendChild(nameTag);
                    document.getElementById('video-container').appendChild(wrapper);
                }

            } catch (e) {
                console.error("Lỗi bật camera:", e);
                alert("Không thể bật Camera. Vui lòng cấp quyền! Lỗi chi tiết: " + e.message);
                document.getElementById('start-video-btn').style.display = 'flex';
            }
        }
        
        async function handleDocumentUpload(event) {
            const file = event.target.files[0];
            if (!file) return;
            
            showToast("Đang xử lý tài liệu...", 3000);
            
            if (file.type === "application/pdf") {
                try {
                    const arrayBuffer = await file.arrayBuffer();
                    const pdf = await pdfjsLib.getDocument(arrayBuffer).promise;
                    showToast(`Tìm thấy ${pdf.numPages} trang PDF. Đang tải lên...`, 3000);
                    
                    let startY = window.excalidrawAPI ? window.excalidrawAPI.getAppState().scrollY : 0;
                    
                    for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
                        const page = await pdf.getPage(pageNum);
                        const viewport = page.getViewport({ scale: 2.0 });
                        
                        const canvas = document.createElement("canvas");
                        const ctx = canvas.getContext("2d");
                        canvas.height = viewport.height;
                        canvas.width = viewport.width;
                        
                        await page.render({ canvasContext: ctx, viewport: viewport }).promise;
                        
                        canvas.toBlob(async (blob) => {
                            await uploadAndInsertImage(blob, "pdf-page-" + pageNum + ".png", startY + (pageNum - 1) * (viewport.height + 50));
                        }, "image/png");
                    }
                } catch (e) {
                    console.error("Lỗi xử lý PDF:", e);
                    showToast("Lỗi xử lý PDF: " + e.message);
                }
            } else if (file.type.startsWith("image/")) {
                await uploadAndInsertImage(file, file.name, window.excalidrawAPI ? window.excalidrawAPI.getAppState().scrollY : 0);
            }
            
            event.target.value = '';
        }
        
        async function uploadAndInsertImage(blobOrFile, fileName, yPos) {
            const formData = new FormData();
            formData.append('file', blobOrFile, fileName);
            
            try {
                const res = await fetch("http://localhost:1234/upload-document", {
                    method: 'POST',
                    body: formData
                });
                const data = await res.json();
                
                if (data.success) {
                    const url = data.url;
                    insertImageToExcalidraw(url, yPos);
                    
                    if (window.currentRoom) {
                        const strData = JSON.stringify({ type: 'document_sync', url: url, yPos: yPos });
                        const encoder = new TextEncoder();
                        await window.currentRoom.localParticipant.publishData(encoder.encode(strData), LivekitClient.DataPacket_Kind.RELIABLE);
                    }
                } else {
                    showToast("Lỗi tải tài liệu: " + data.error);
                }
            } catch (e) {
                console.error("Lỗi upload tài liệu:", e);
                showToast("Lỗi kết nối Server: " + e.message);
            }
        }
        
        async function insertImageToExcalidraw(url, yPos = 0) {
            if (!window.excalidrawAPI) return;
            
            try {
                const proxyUrl = "http://localhost:1234/proxy-image?url=" + encodeURIComponent(url);
                const response = await fetch(proxyUrl);
                const blob = await response.blob();
                
                const reader = new FileReader();
                reader.onloadend = () => {
                    const dataURL = reader.result;
                    const fileId = "file-" + Date.now() + Math.random().toString(36).substring(2, 9);
                    
                    const img = new Image();
                    img.onload = () => {
                        window.excalidrawAPI.addFiles([{
                            id: fileId,
                            dataURL: dataURL,
                            mimeType: blob.type,
                            created: Date.now()
                        }]);
                        
                        const elements = window.excalidrawAPI.getSceneElements();
                        const newElement = {
                            id: "img-" + Date.now() + Math.random().toString(36).substring(2, 9),
                            type: "image",
                            fileId: fileId,
                            x: -window.excalidrawAPI.getAppState().scrollX + 50,
                            y: -window.excalidrawAPI.getAppState().scrollY + yPos,
                            width: img.width / 2,
                            height: img.height / 2,
                            angle: 0,
                            strokeColor: "transparent",
                            backgroundColor: "transparent",
                            fillStyle: "hachure",
                            strokeWidth: 1,
                            strokeStyle: "solid",
                            roughness: 1,
                            opacity: 100,
                            groupIds: [],
                            roundness: null,
                            isDeleted: false,
                            boundElements: null,
                            updated: Date.now(),
                            link: null,
                            locked: false
                        };
                        
                        window.excalidrawAPI.updateScene({ elements: [...elements, newElement] });
                        
                        // Kích hoạt đồng bộ nét vẽ sau khi chèn
                        if (window.performDrawSync) {
                            setTimeout(() => window.performDrawSync(window.excalidrawAPI.getSceneElements()), 100);
                        }
                    };
                    img.src = dataURL;
                };
                reader.readAsDataURL(blob);
            } catch (e) {
                console.error("Lỗi chèn ảnh vào bảng:", e);
            }
        }

        // ==========================================
        // TÍNH NĂNG SIÊU IDE LẬP TRÌNH & ĐẤU TRƯỜNG
        // ==========================================
        // ==========================================
        // TÍNH NĂNG STACKBLITZ (VS CODE THẬT)
        // ==========================================
        let isCodeMode = false;

        function toggleCodeMode() {
            isCodeMode = !isCodeMode;
            const wrapper = document.getElementById('code-wrapper');
            const root = document.getElementById('root');
            const btn = document.getElementById('code-toggle-btn');
            
            if (isCodeMode) {
                wrapper.style.display = 'flex';
                root.style.display = 'none'; // Ẩn bảng vẽ
                btn.innerText = "🔙 Tắt Code";
                btn.style.background = "#ef4444";
                
                if (!window.sbVM) {
                    initStackBlitz();
                }
            } else {
                wrapper.style.display = 'none';
                root.style.display = 'block'; // Hiện lại bảng vẽ
                btn.innerText = "💻 Code";
                btn.style.background = "#000";
            }
        }
        
        function toggleJudgePanel() {
            const panel = document.getElementById('judge-panel');
            panel.style.display = (panel.style.display === 'none') ? 'flex' : 'none';
        }

        function initStackBlitz() {
            StackBlitzSDK.embedProject('editor-container', {
                title: 'TutorHub VS Code',
                description: 'Môi trường lập trình của bạn',
                template: 'node',
                files: {
                    'index.js': 'console.log("Xin chào từ VS Code của TutorHub!");\n// Bạn có thể gõ lệnh node index.js ở Terminal bên dưới để chạy nhé!\n',
                    'main.py': 'print("Hoặc bạn có thể viết Python và ấn Nộp Bài để máy chủ chấm điểm!")\n'
                }
            }, {
                height: '100%',
                hideExplorer: false,
                hideNavigation: true,
                forceEmbedLayout: true
            }).then(vm => {
                window.sbVM = vm;
            });
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
            if (!window.sbVM) return;
            
            const resultBox = document.getElementById('judge-result');
            const stdin = document.getElementById('judge-input').value;
            const expected = document.getElementById('judge-output').value.trim();
            
            resultBox.innerText = "Đang lấy mã nguồn từ VS Code...";
            resultBox.style.color = "#f59e0b";

            try {
                // Lấy toàn bộ File từ trong VS Code StackBlitz ra
                const files = await window.sbVM.getFsSnapshot();
                
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

                const response = await fetch('https://wandbox.org/api/compile.json', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        compiler: wandboxCompiler,
                        code: files[mainFileName],
                        codes: codesArray,
                        stdin: stdin
                    })
                });
                
                const data = await response.json();
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

    