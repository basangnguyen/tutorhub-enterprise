// Dynamic imports are used instead to prevent module loading block
window.setInfiniteMode = (isInfinite) => {
            // Excalidraw mặc định là Infinite
        };

        // Hàm export dữ liệu mới kết hợp Thumbnail do JS tạo
        window.requestSaveBoardAndThumbnail = async () => {
            if (!window.tldrawAPI) return;
            const elements = window.tldrawAPI.getSceneElements();
            const appState = window.tldrawAPI.getAppState();
            
            const files = window.tldrawAPI.getFiles();
            const jsonStr = JSON.stringify({ elements: elements, appState: { theme: appState.theme, viewBackgroundColor: appState.viewBackgroundColor }, files: files });
            let thumbBase64 = "";

            try {
                // Xuất ảnh PNG độ phân giải chuẩn, không bị nhiễu UI
                const blob = await ExcalidrawLib.exportToBlob({
                    elements,
                    appState: { ...appState, exportWithDarkMode: appState.theme === 'dark' },
                    files: window.tldrawAPI.getFiles(),
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

        window.loadBoardData = async (jsonStr, boardId) => {
            window.roomState.set('boardId', boardId);
            window.currentBoardId = boardId; // Lưu lại để LiveKit dùng (Fallback)
            if (!window.tldrawAPI) return;
            try {
                if (jsonStr && jsonStr !== 'null') {
                    const data = JSON.parse(jsonStr);
                    if (data.files && Object.keys(data.files).length > 0) {
                        window.tldrawAPI.addFiles(Object.values(data.files));
                    }
                    window.tldrawAPI.updateScene({
                        elements: data.elements,
                        appState: data.appState
                    });
                }
                
                // Real-time Sync WebSocket using Yjs
                if (window.syncRoom) {
                    window.syncRoom.destroy();
                    window.syncRoom = null;
                }
                if (boardId) {
                    const hostUrl = 'wss://hocba299-3-tutorhub-sync.hf.space/yjs';
                    const yjs = await import('https://esm.sh/yjs');
                    const Y = yjs;
                    const yws = await import('https://esm.sh/y-websocket');
                    const WebsocketProvider = yws.WebsocketProvider;
                    const ykv = await import('https://esm.sh/y-utility/y-keyvalue');
                    const YKeyValue = ykv.YKeyValue;

                    const yDoc = new Y.Doc({ gc: true });
                    const yArr = yDoc.getArray(`tl_${boardId}`);
                    const yStore = new YKeyValue(yArr);
                    const room = new WebsocketProvider(hostUrl, boardId, yDoc, { connect: true });
                    window.syncRoom = room;
                    
                    room.on('status', (event) => {
                        console.log(`[Yjs] Connection status for ${boardId}:`, event.status);
                    });

                    // --- Yjs Awareness (Multiplayer Cursors) ---
                    const awareness = room.awareness;
                    const userName = window.roomState.get('userName') || window.currentUserName || 'Guest';
                    
                    let hash = 0;
                    for (let i = 0; i < userName.length; i++) {
                        hash = userName.charCodeAt(i) + ((hash << 5) - hash);
                    }
                    const hue = Math.abs(hash) % 360;
                    const cursorColor = `hsl(${hue}, 80%, 50%)`;

                    awareness.setLocalStateField('user', {
                        name: userName,
                        color: cursorColor
                    });

                    let lastMouseMoveTime = 0;
                    document.body.addEventListener('mousemove', (e) => {
                        const now = Date.now();
                        if (now - lastMouseMoveTime > 30) {
                            lastMouseMoveTime = now;
                            if (window.tldrawAPI) {
                                // Convert screen to page coordinates
                                const pagePos = window.tldrawAPI.screenToPage({x: e.clientX, y: e.clientY});
                                awareness.setLocalStateField('cursor', {
                                    x: pagePos.x,
                                    y: pagePos.y
                                });
                            }
                        }
                    });

                    awareness.on('change', () => {
                        const states = awareness.getStates();
                        const cursorsLayer = document.getElementById('cursors-layer');
                        if (!cursorsLayer) return;

                        const activeClientIds = new Set();
                        if (!window.tldrawAPI) return;

                        states.forEach((state, clientID) => {
                            if (clientID === awareness.clientID) return;
                            if (state.cursor && state.user) {
                                activeClientIds.add(clientID);
                                let cursorEl = document.getElementById(`cursor-${clientID}`);
                                if (!cursorEl) {
                                    cursorEl = document.createElement('div');
                                    cursorEl.id = `cursor-${clientID}`;
                                    cursorEl.className = 'remote-cursor';
                                    
                                    const cursorSvg = document.createElement('div');
                                    cursorSvg.className = 'remote-cursor-icon';
                                    cursorSvg.innerHTML = `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                        <path d="M5.65376 21.5721C5.41908 21.6503 5.16335 21.5422 5.04838 21.316C4.93341 21.0898 4.98971 20.8115 5.18165 20.6559L20.8931 7.91795C21.085 7.76239 21.3653 7.77312 21.5451 7.94291C21.7249 8.11269 21.7588 8.39702 21.6237 8.60533L13.109 21.7335C12.9739 21.9418 12.7161 22.0163 12.4988 21.9069L5.65376 21.5721Z" fill="${state.user.color}" stroke="white" stroke-width="2" stroke-linejoin="round"/>
                                    </svg>`;

                                    const cursorLabel = document.createElement('div');
                                    cursorLabel.className = 'remote-cursor-label';
                                    cursorLabel.style.backgroundColor = state.user.color;
                                    cursorLabel.textContent = state.user.name;

                                    cursorEl.appendChild(cursorSvg);
                                    cursorEl.appendChild(cursorLabel);
                                    cursorsLayer.appendChild(cursorEl);
                                }

                                // Initial transform
                                const screenPos = window.tldrawAPI.pageToScreen({x: state.cursor.x, y: state.cursor.y});
                                cursorEl.style.transform = `translate(${screenPos.x}px, ${screenPos.y}px)`;
                            }
                        });

                        Array.from(cursorsLayer.children).forEach((child) => {
                            const idStr = child.id.replace('cursor-', '');
                            const id = parseInt(idStr, 10);
                            if (!activeClientIds.has(id)) {
                                child.remove();
                            }
                        });
                    });
                    
                    // Reposition cursors continuously so they follow pan/zoom
                    if (window.cursorUpdateAnimationFrame) {
                        cancelAnimationFrame(window.cursorUpdateAnimationFrame);
                    }
                    const updateCursorPositions = () => {
                        if (!window.syncRoom || window.syncRoom !== room) return; // Stop if room changed
                        const states = awareness.getStates();
                        const cursorsLayer = document.getElementById('cursors-layer');
                        if (cursorsLayer && window.tldrawAPI) {
                            states.forEach((state, clientID) => {
                                if (clientID === awareness.clientID) return;
                                if (state.cursor) {
                                    let cursorEl = document.getElementById(`cursor-${clientID}`);
                                    if (cursorEl) {
                                        const screenPos = window.tldrawAPI.pageToScreen({x: state.cursor.x, y: state.cursor.y});
                                        cursorEl.style.transform = `translate(${screenPos.x}px, ${screenPos.y}px)`;
                                    }
                                }
                            });
                        }
                        window.cursorUpdateAnimationFrame = requestAnimationFrame(updateCursorPositions);
                    };
                    window.cursorUpdateAnimationFrame = requestAnimationFrame(updateCursorPositions);
                    // --- End Yjs Awareness ---

                    // Listen to tldraw store changes -> Yjs
                    window.tldrawAPI.store.listen(
                        function syncStoreChangesToYjs(update) {
                            if (update.source !== 'user') return;
                            yDoc.transact(() => {
                                Object.values(update.changes.added).forEach((record) => {
                                    yStore.set(record.id, record);
                                });
                                Object.values(update.changes.updated).forEach(([_, record]) => {
                                    yStore.set(record.id, record);
                                });
                                Object.values(update.changes.removed).forEach((record) => {
                                    yStore.delete(record.id);
                                });
                            });
                        },
                        { source: 'user', scope: 'document' }
                    );

                    // Listen to Yjs changes -> tldraw store
                    yStore.on('change', function syncYjsChangesToStore(changes) {
                        const store = window.tldrawAPI.store;
                        const toRemove = [];
                        const toPut = [];
                        changes.forEach((change, id) => {
                            switch (change.action) {
                                case 'add':
                                case 'update':
                                    const record = yStore.get(id);
                                    toPut.push(record);
                                    break;
                                case 'delete':
                                    toRemove.push(id);
                                    break;
                            }
                        });
                        if (toPut.length || toRemove.length) {
                            store.mergeRemoteChanges(() => {
                                if (toRemove.length) store.remove(toRemove);
                                if (toPut.length) store.put(toPut);
                            });
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
