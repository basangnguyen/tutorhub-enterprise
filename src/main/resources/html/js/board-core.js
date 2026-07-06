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
            window.currentBoardId = boardId; // Lưu lại để LiveKit dùng
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

        let livekitRoom = null;
        
        async function connectToLiveKit() {
            if (window.currentRoom) return; // Đã kết nối

            const LIVEKIT_URL = window.TUTORHUB_CONFIG.LIVEKIT_URL; 
            
            try {
                const urlParams = new URLSearchParams(window.location.search);
                window.currentUserName = urlParams.get('name') || 'Guest_' + Math.floor(Math.random()*1000);
                window.currentUserRole = urlParams.get('role') === 'teacher' ? 'teacher' : 'student';

                const currentRoomId = window.currentBoardId || 'default-room';
                const safeName = encodeURIComponent(window.currentUserName);
                
                // Fetch token qua CefQuery (bảo mật, Java làm Proxy ẩn)
                const data = await new Promise((resolve, reject) => {
                    if (window.cefQuery) {
                        window.cefQuery({
                            request: `GET_LIVEKIT_TOKEN:${currentRoomId}:${safeName}`,
                            onSuccess: function(response) {
                                try {
                                    resolve(JSON.parse(response));
                                } catch (e) {
                                    reject(e);
                                }
                            },
                            onFailure: function(errorCode, errorMessage) {
                                reject(new Error(errorMessage));
                            }
                        });
                    } else {
                        // Fallback cho môi trường test trên browser thường nếu cần
                        fetch(`${window.TUTORHUB_CONFIG.SYNC_SERVER_URL}/livekit/token?room=${currentRoomId}&username=${safeName}`, {
                            headers: {
                                "Authorization": "Bearer TUTORHUB_SECRET_2026"
                            }
                        })
                            .then(res => res.json())
                            .then(resolve).catch(reject);
                    }
                });
                
                if (!data || !data.token) {
                    console.error('Lỗi lấy token từ server!', data);
                    alert('Lỗi lấy token LiveKit từ Server: ' + JSON.stringify(data));
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
                            
                            const viewCodeBtn = document.createElement('button');
                            viewCodeBtn.innerText = '👁️ Code';
                            viewCodeBtn.style.position = 'absolute';
                            viewCodeBtn.style.top = '5px';
                            viewCodeBtn.style.right = '5px';
                            viewCodeBtn.style.background = '#10b981';
                            viewCodeBtn.style.color = '#fff';
                            viewCodeBtn.style.border = 'none';
                            viewCodeBtn.style.borderRadius = '4px';
                            viewCodeBtn.style.fontSize = '10px';
                            viewCodeBtn.style.padding = '2px 5px';
                            viewCodeBtn.style.cursor = 'pointer';
                            viewCodeBtn.onclick = () => {
                                const container = document.getElementById('editor-container');
                                container.innerHTML = `<iframe src="${HUGGING_FACE_URL}/login?user=${participant.identity}" style="width:100%; height:100%; border:none;"></iframe>`;
                                if (!isCodeMode) toggleCodeMode();
                                showToast("Đang xem Code của " + participant.identity, 3000);
                            };
                            
                            wrapper.appendChild(element);
                            wrapper.appendChild(nameTag);
                            wrapper.appendChild(viewCodeBtn);
                            document.getElementById('video-sidebar').style.display = 'flex';
                            document.getElementById('video-sidebar').appendChild(wrapper);
                        } else {
                            document.getElementById('video-sidebar').appendChild(element);
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
                    if (typeof renderRoster === 'function') renderRoster();
                });

                room.on(LivekitClient.RoomEvent.ParticipantConnected, () => {
                    if (typeof renderRoster === 'function') renderRoster();
                });
                room.on(LivekitClient.RoomEvent.TrackMuted, () => { if (typeof renderRoster === 'function') renderRoster(); });
                room.on(LivekitClient.RoomEvent.TrackUnmuted, () => { if (typeof renderRoster === 'function') renderRoster(); });
                room.on(LivekitClient.RoomEvent.ParticipantMetadataChanged, () => { if (typeof renderRoster === 'function') renderRoster(); });
                room.on(LivekitClient.RoomEvent.ActiveSpeakersChanged, () => { if (typeof renderRoster === 'function') renderRoster(); });

                room.on(LivekitClient.RoomEvent.DataReceived, (payload, participant, kind, topic) => {
                    try {
                        const decoder = new TextDecoder();
                        const data = JSON.parse(decoder.decode(payload));
                        
                        if (data.type === 'quiz_vote') {
                            // Bắn event ra window để React (QuizShape) xử lý
                            window.dispatchEvent(new CustomEvent('quiz_vote_received', { detail: data }));
                        } else if (data.type === 'chat') {
                            const senderName = participant ? participant.identity : data.sender;
                            appendChatMessage(senderName, data.time, data.text, false);
                            playNotificationSound();
                            
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
                        } else if (data.type === 'roster_force_mute') {
                            if (window.currentRoom && window.currentRoom.localParticipant && data.target === window.currentRoom.localParticipant.identity) {
                                window.currentRoom.localParticipant.setMicrophoneEnabled(false).catch(e => console.error(e));
                                showToast("Giáo viên đã tắt Mic của bạn.", 5000);
                            }
                        } else if (data.type === 'roster_ask_unmute') {
                            if (window.currentRoom && window.currentRoom.localParticipant && data.target === window.currentRoom.localParticipant.identity) {
                                document.getElementById('ask-unmute-modal').style.display = 'block';
                            }
                        } else if (data.type === 'roster_kick') {
                            if (window.currentRoom && window.currentRoom.localParticipant && data.target === window.currentRoom.localParticipant.identity) {
                                triggerCloseBoard();
                            }
                        } else if (data.type === 'roster_mute_all') {
                            if (window.currentRoom && window.currentRoom.localParticipant && window.currentUserRole !== 'teacher') {
                                window.currentRoom.localParticipant.setMicrophoneEnabled(false).catch(e => console.error(e));
                                showToast("Giáo viên đã tắt Mic của bạn để giữ trật tự!", 5000);
                            }
                        } else if (data.type === 'roster_lower_all_hands') {
                            if (window.currentRoom && window.currentRoom.localParticipant) {
                                const currentMeta = parseMetadata(window.currentRoom.localParticipant.metadata);
                                if (currentMeta.isHandRaised) {
                                    currentMeta.isHandRaised = false;
                                    window.currentRoom.localParticipant.setMetadata(JSON.stringify(currentMeta));
                                }
                            }
                        } else if (data.type === 'roster_lower_hand') {
                            if (window.currentRoom && window.currentRoom.localParticipant && data.target === window.currentRoom.localParticipant.identity) {
                                const currentMeta = parseMetadata(window.currentRoom.localParticipant.metadata, window.currentRoom.localParticipant.identity);
                                currentMeta.isHandRaised = false;
                                
                                if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                                window.rosterMetadataCache[window.currentRoom.localParticipant.identity] = currentMeta;
                                
                                try { window.currentRoom.localParticipant.setMetadata(JSON.stringify(currentMeta)); } catch(e){}
                                
                                const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.currentRoom.localParticipant.identity, metadata: JSON.stringify(currentMeta) });
                                window.currentRoom.localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });
                            }
                        } else if (data.type === 'roster_admit') {
                            if (window.currentRoom && window.currentRoom.localParticipant && data.target === window.currentRoom.localParticipant.identity) {
                                const currentMeta = parseMetadata(window.currentRoom.localParticipant.metadata, window.currentRoom.localParticipant.identity);
                                currentMeta.isAdmitted = true;
                                if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                                window.rosterMetadataCache[window.currentRoom.localParticipant.identity] = currentMeta;
                                try { window.currentRoom.localParticipant.setMetadata(JSON.stringify(currentMeta)); } catch(e){}
                                const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.currentRoom.localParticipant.identity, metadata: JSON.stringify(currentMeta) });
                                window.currentRoom.localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });
                                if (typeof renderRoster === 'function') renderRoster();
                                showToast("Giáo viên đã duyệt bạn vào lớp!", 5000);
                            }
                        } else if (data.type === 'roster_send_lobby') {
                            if (window.currentRoom && window.currentRoom.localParticipant && data.target === window.currentRoom.localParticipant.identity) {
                                const currentMeta = parseMetadata(window.currentRoom.localParticipant.metadata, window.currentRoom.localParticipant.identity);
                                currentMeta.isAdmitted = false;
                                if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                                window.rosterMetadataCache[window.currentRoom.localParticipant.identity] = currentMeta;
                                try { window.currentRoom.localParticipant.setMetadata(JSON.stringify(currentMeta)); } catch(e){}
                                const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.currentRoom.localParticipant.identity, metadata: JSON.stringify(currentMeta) });
                                window.currentRoom.localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });
                                if (typeof renderRoster === 'function') renderRoster();
                            }
                        } else if (data.type === 'roster_admit_all') {
                            if (window.currentRoom && window.currentRoom.localParticipant && window.currentUserRole === 'student') {
                                const currentMeta = parseMetadata(window.currentRoom.localParticipant.metadata, window.currentRoom.localParticipant.identity);
                                if (!currentMeta.isAdmitted) {
                                    currentMeta.isAdmitted = true;
                                    if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                                    window.rosterMetadataCache[window.currentRoom.localParticipant.identity] = currentMeta;
                                    try { window.currentRoom.localParticipant.setMetadata(JSON.stringify(currentMeta)); } catch(e){}
                                    const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.currentRoom.localParticipant.identity, metadata: JSON.stringify(currentMeta) });
                                    window.currentRoom.localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });
                                    if (typeof renderRoster === 'function') renderRoster();
                                    showToast("Giáo viên đã duyệt tất cả vào lớp!", 5000);
                                }
                            }
                        } else if (data.type === 'roster_reaction') {
                            if (typeof showReactionAnimation === 'function') showReactionAnimation(data.emoji, data.sender);
                        } else if (data.type === 'roster_lobby_status') {
                            window.lobbyEnabled = data.enabled;
                            if (typeof renderRoster === 'function') renderRoster();
                        } else if (data.type === 'roster_sync_metadata') {
                            // Cập nhật metadata "chay" nếu server LiveKit không phát (Workaround)
                            if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                            window.rosterMetadataCache[data.sender] = typeof data.metadata === 'string' ? JSON.parse(data.metadata) : data.metadata;
                            if (typeof renderRoster === 'function') renderRoster();
                        } else if (data.type === 'yt_sync') {
                            if (data.action === 'open') {
                                isYtTeacher = false;
                                openYTPlayer(data.videoId, data.time, false);
                            } else if (data.action === 'close') {
                                closeCoWatchYT();
                            } else if (data.action === 'sync') {
                                if (!isYtTeacher && ytPlayer && ytPlayer.seekTo) {
                                    const timeDiff = Math.abs(ytPlayer.getCurrentTime() - data.time);
                                    if (timeDiff > 2) ytPlayer.seekTo(data.time);
                                    
                                    if (data.state === 1 && ytPlayer.getPlayerState() !== 1) ytPlayer.playVideo();
                                    else if (data.state === 2 && ytPlayer.getPlayerState() !== 2) ytPlayer.pauseVideo();
                                }
                            }
                        } else if (data.type === 'draw_sync') {
                            if (window.tldrawAPI) {
                                window.isReceivingSync = true;
                                window.tldrawAPI.updateScene({ elements: data.elements });
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
                            // Bỏ qua đồng bộ code vì StackBlitz có môi trường độc lập
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
                
                // Cập nhật Metadata cho Roster
                const initMetadata = JSON.stringify({
                    role: window.currentUserRole,
                    displayName: window.currentUserName,
                    isHandRaised: false,
                    handRaisedAt: null,
                    isAdmitted: window.currentUserRole === 'teacher' ? true : false // Teacher always admitted, student depends on lobby
                });
                
                if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                window.rosterMetadataCache[window.currentRoom.localParticipant.identity] = JSON.parse(initMetadata);

                try {
                    if (room.localParticipant.setMetadata) {
                        await room.localParticipant.setMetadata(initMetadata);
                    }
                } catch(e) {
                    console.warn("Lỗi setMetadata, có thể do thiếu quyền từ Server: ", e);
                }
                
                // Broadcast backup
                setTimeout(() => {
                    if (window.currentRoom && window.currentRoom.localParticipant) {
                        const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.currentRoom.localParticipant.identity, metadata: initMetadata });
                        const encoder = new TextEncoder();
                        window.currentRoom.localParticipant.publishData(encoder.encode(metaPayload), { reliable: true });
                    }
                }, 1000);
                
                // Gởi thêm 1 lần nữa sau 5 giây để chắc chắn người khác nhận được
                setTimeout(() => {
                    if (window.currentRoom && window.currentRoom.localParticipant) {
                        const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.currentRoom.localParticipant.identity, metadata: initMetadata });
                        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });
                    }
                }, 5000);
            } catch (e) {
                console.error("Lỗi kết nối LiveKit:", e);
                alert("Lỗi kết nối LiveKit: " + e.message);
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
                    
                    const sidebar = document.getElementById('video-sidebar');
                    sidebar.style.display = 'flex';
                    sidebar.appendChild(wrapper);
                }

            } catch (e) {
                console.error("Lỗi bật camera:", e);
                alert("Không thể bật Camera. Vui lòng cấp quyền! Lỗi chi tiết: " + e.name + " - " + e.message + "\nStack: " + e.stack);
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
                    
                    let startY = window.tldrawAPI ? window.tldrawAPI.getAppState().scrollY : 0;
                    
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
                await uploadAndInsertImage(file, file.name, window.tldrawAPI ? window.tldrawAPI.getAppState().scrollY : 0);
            }
            
            event.target.value = '';
        }
        
        async function uploadAndInsertImage(blobOrFile, fileName, yPos) {
            const formData = new FormData();
            formData.append('file', blobOrFile, fileName);
            
            try {
                const res = await fetch("https://hocbatrolai293-tutorhub-vscode.hf.space/upload-document", {
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
            if (!window.tldrawAPI) return;
            
            try {
                const proxyUrl = "https://hocbatrolai293-tutorhub-vscode.hf.space/proxy-image?url=" + encodeURIComponent(url);
                const response = await fetch(proxyUrl);
                const blob = await response.blob();
                
                const reader = new FileReader();
                reader.onloadend = () => {
                    const dataURL = reader.result;
                    const fileId = "file-" + Date.now() + Math.random().toString(36).substring(2, 9);
                    
                    const img = new Image();
                    img.onload = () => {
                        window.tldrawAPI.addFiles([{
                            id: fileId,
                            dataURL: dataURL,
                            mimeType: blob.type,
                            created: Date.now()
                        }]);
                        
                        const elements = window.tldrawAPI.getSceneElements();
                        const newElement = {
                            id: "img-" + Date.now() + Math.random().toString(36).substring(2, 9),
                            type: "image",
                            fileId: fileId,
                            x: -window.tldrawAPI.getAppState().scrollX + 50,
                            y: -window.tldrawAPI.getAppState().scrollY + yPos,
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
                        
                        window.tldrawAPI.updateScene({ elements: [...elements, newElement] });
                        
                        // Kích hoạt đồng bộ nét vẽ sau khi chèn
                        if (window.performDrawSync) {
                            setTimeout(() => window.performDrawSync(window.tldrawAPI.getSceneElements()), 100);
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
        // TÍNH NĂNG VS CODE THẬT (HUGGING FACE CODE-SERVER)
        // ==========================================

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

    // ==========================================
    // TÍNH NĂNG CO-WATCH & EXPORT PDF
    // ==========================================

    async function exportToPDF() {
        if (!window.tldrawAPI) return;
        try {
            const elements = window.tldrawAPI.getSceneElements();
            const appState = window.tldrawAPI.getAppState();
            const files = window.tldrawAPI.getFiles();
            
            showToast("Đang tạo file PDF, vui lòng chờ...", 2000);
            
            const blob = await window.ExcalidrawLib.exportToBlob({
                elements,
                appState: { ...appState, exportWithDarkMode: false, viewBackgroundColor: "#ffffff" },
                files,
                mimeType: "image/png"
            });
            
            const imgUrl = URL.createObjectURL(blob);
            const img = new Image();
            img.onload = () => {
                const { jsPDF } = window.jspdf;
                const orientation = img.width > img.height ? "l" : "p";
                const pdf = new jsPDF({ orientation: orientation, unit: "px", format: [img.width, img.height] });
                pdf.addImage(img, 'PNG', 0, 0, img.width, img.height);
                pdf.save("TutorHub_Board.pdf");
                showToast("Đã lưu PDF thành công!", 3000);
            };
            img.src = imgUrl;
        } catch (e) {
            console.error("Lỗi xuất PDF", e);
            alert("Lỗi khi tạo PDF!");
        }
    }

    function insertMathFormula() {
        const mathField = document.getElementById('math-field-input');
        if (!mathField.value.trim()) return;
        
        if (window.addMathNode) {
            window.addMathNode(mathField.value);
        }
        document.getElementById('math-modal').style.display = 'none';
        mathField.value = ''; // Reset
    }

    function insertMermaidDiagram() {
        const mermaidInput = document.getElementById('mermaid-input');
        if (!mermaidInput.value.trim()) return;
        
        if (window.addMermaidNode) {
            window.addMermaidNode(mermaidInput.value);
        }
        document.getElementById('mermaid-modal').style.display = 'none';
    }

    // ========== XỬ LÝ ĐỒNG BỘ CÙNG XEM (CO-WATCH) ==========
    
    function toggleCoWatchModal() {
        const modal = document.getElementById('cowatch-modal');
        modal.style.display = modal.style.display === 'none' ? 'block' : 'none';
    }

    function startCoWatchReels() {
        toggleCoWatchModal();
        if (window.cefQuery) {
            window.cefQuery({ 
                request: 'OPEN_REEL_COWATCH', 
                persistent: false, 
                onSuccess: function(r){}, 
                onFailure: function(e,m){} 
            });
        }
    }

    let ytPlayer = null;
    let isYtTeacher = false;
    let ytSyncInterval = null;

    function onYouTubeIframeAPIReady() {
        console.log("YouTube API Ready");
    }

    function startCoWatchYT() {
        const link = document.getElementById('cowatch-yt-input').value.trim();
        
        // Regex chuẩn để lấy ID video (hỗ trợ cả shorts, youtu.be, embed, tham số phụ)
        const match = link.match(/(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=|shorts\/))([\w-]{11})/);
        
        if (!match) {
            alert("Link YouTube không hợp lệ hoặc không tìm thấy ID video!");
            return;
        }
        
        const videoId = match[1];
        
        toggleCoWatchModal();
        isYtTeacher = true; // Chỉ người mở YT mới có quyền broadcast
        openYTPlayer(videoId, 0, true);
        
        if (window.currentRoom) {
            const payload = JSON.stringify({ type: 'yt_sync', action: 'open', videoId: videoId, time: 0, state: 1 });
            window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
        }
    }

    function openYTPlayer(videoId, startTime = 0, isTeacher = false) {
        document.getElementById('cowatch-yt-floating').style.display = 'block';
        document.getElementById('yt-overlay').style.display = isTeacher ? 'none' : 'block';
        
        if (ytPlayer) {
            ytPlayer.loadVideoById(videoId, startTime);
        } else {
            ytPlayer = new YT.Player('yt-player', {
                height: '100%',
                width: '100%',
                videoId: videoId,
                playerVars: { 
                    'autoplay': 1, 
                    'controls': 1
                },
                events: {
                    'onReady': (e) => {
                        if (startTime > 0) e.target.seekTo(startTime);
                    },
                    'onStateChange': onYTStateChange
                }
            });
        }
        
        if (isTeacher) {
            if (ytSyncInterval) clearInterval(ytSyncInterval);
            ytSyncInterval = setInterval(broadcastYTState, 1500); 
        }
    }

    function broadcastYTState() {
        if (!ytPlayer || !ytPlayer.getPlayerState) return;
        const state = ytPlayer.getPlayerState();
        const time = ytPlayer.getCurrentTime();
        
        if (window.currentRoom) {
            const payload = JSON.stringify({ type: 'yt_sync', action: 'sync', videoId: ytPlayer.getVideoData().video_id, time: time, state: state });
            window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: false });
        }
    }

    let isYTFullscreen = false;
    function toggleYTFullscreen() {
        const box = document.getElementById('cowatch-yt-floating');
        isYTFullscreen = !isYTFullscreen;
        if (isYTFullscreen) {
            box.classList.add('yt-fullscreen-mode');
        } else {
            box.classList.remove('yt-fullscreen-mode');
            // Remove inline top/left to fallback to default bottom-right
            box.style.top = '';
            box.style.left = '';
        }
    }

    function closeCoWatchYT() {
        document.getElementById('cowatch-yt-floating').style.display = 'none';
        if (ytPlayer && ytPlayer.pauseVideo) ytPlayer.pauseVideo();
        if (ytSyncInterval) clearInterval(ytSyncInterval);
        
        if (isYtTeacher && window.currentRoom) {
            const payload = JSON.stringify({ type: 'yt_sync', action: 'close' });
            window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
            isYtTeacher = false;
        }
    }

    function onYTStateChange(event) {
        if (!isYtTeacher || !window.currentRoom) return;
        broadcastYTState();
    }


    // Drag functionality for Floating Modal
    const floatingBox = document.getElementById('cowatch-yt-floating');
    const header = document.getElementById('yt-header');
    let isDragging = false, startX, startY, initialX, initialY;

    header.addEventListener('mousedown', (e) => {
        isDragging = true;
        startX = e.clientX;
        startY = e.clientY;
        initialX = floatingBox.offsetLeft;
        initialY = floatingBox.offsetTop;
    });

    document.addEventListener('mousemove', (e) => {
        if (!isDragging) return;
        const dx = e.clientX - startX;
        const dy = e.clientY - startY;
        floatingBox.style.left = (initialX + dx) + 'px';
        floatingBox.style.top = (initialY + dy) + 'px';
        floatingBox.style.right = 'auto';
        floatingBox.style.bottom = 'auto';
    });

    document.addEventListener('mouseup', () => { isDragging = false; });

    // ==========================================
    // UI HANDLERS (DOCK & SETTINGS)
    // ==========================================
    function toggleBoardSettingsModal() {
        const modal = document.getElementById('board-settings-modal');
        modal.style.display = modal.style.display === 'none' ? 'block' : 'none';
    }

    function applyBoardSettings() {
        const isDark = document.getElementById('chk-dark-mode').checked;
        const isInfinite = document.getElementById('chk-infinite').checked;
        const paperMode = document.getElementById('cb-paper-mode').value;
        
        if (window.setPaperMode) window.setPaperMode(paperMode, isDark);
        if (window.setInfiniteMode) window.setInfiniteMode(isInfinite);
    }

    function triggerSaveBoard() {
        if (window.cefQuery) {
            window.cefQuery({ request: 'REQUEST_SAVE_BOARD', persistent: false, onSuccess: function(r){}, onFailure: function(e,m){} });
        } else {
            console.log("Saving board..."); // Fallback if no JCEF
        }
    }

    function triggerCloseBoard() {
        if (window.cefQuery) {
            window.cefQuery({ request: 'CLOSE_BOARD', persistent: false, onSuccess: function(r){}, onFailure: function(e,m){} });
        } else {
            console.log("Closing board...");
        }
    }

    // ==========================================
    // CLASS ROSTER (PEOPLE PANEL) LOGIC
    // ==========================================
    
    function togglePeopleSidebar() {
        const sidebar = document.getElementById('people-sidebar');
        const isHidden = sidebar.style.display === 'none';
        
        if (isHidden) {
            sidebar.style.display = 'flex';
            sidebar.style.transform = 'translateX(0)';
            if (window.currentUserRole === 'teacher') {
                document.getElementById('roster-host-controls').style.display = 'flex';
            }
            renderRoster();
        } else {
            sidebar.style.transform = 'translateX(100%)';
            setTimeout(() => sidebar.style.display = 'none', 300);
        }
    }

    function parseMetadata(str, identity) {
        let meta = null;
        try {
            if (str) meta = JSON.parse(str);
        } catch(e) { }
        
        // Fallback to cache
        if (!meta && window.rosterMetadataCache && window.rosterMetadataCache[identity]) {
            meta = window.rosterMetadataCache[identity];
        }
        
        return meta || { role: 'student', displayName: 'Unknown', isHandRaised: false, isAdmitted: true };
    }

    function renderRoster() {
        if (!window.currentRoom) return;
        
        const listDiv = document.getElementById('roster-list');
        const searchTerm = (document.getElementById('roster-search').value || '').toLowerCase();
        
        const participants = [];
        
        // Add Local Participant
        if (window.currentRoom.localParticipant) {
            const meta = parseMetadata(window.currentRoom.localParticipant.metadata, window.currentRoom.localParticipant.identity);
            participants.push({
                participant: window.currentRoom.localParticipant,
                isLocal: true,
                meta: meta,
                id: window.currentRoom.localParticipant.identity
            });
        }
        
        // Add Remote Participants
        window.currentRoom.remoteParticipants.forEach(rp => {
            const meta = parseMetadata(rp.metadata, rp.identity);
            participants.push({
                participant: rp,
                isLocal: false,
                meta: meta,
                id: rp.identity
            });
        });
        
        // Filter by search
        let filtered = participants;
        if (searchTerm) {
            filtered = participants.filter(p => p.id.toLowerCase().includes(searchTerm) || (p.meta.displayName && p.meta.displayName.toLowerCase().includes(searchTerm)));
        }

        document.getElementById('roster-count').innerText = participants.length;
        document.getElementById('roster-badge').innerText = participants.length;
        document.getElementById('roster-badge').style.display = participants.length > 1 ? 'flex' : 'none';

        // Setup lobby local overlay
        const localP = participants.find(p => p.isLocal);
        if (typeof window.lobbyEnabled === 'undefined') window.lobbyEnabled = true;
        
        if (localP && localP.meta.role === 'student' && window.lobbyEnabled && !localP.meta.isAdmitted) {
            document.getElementById('lobby-overlay').style.display = 'flex';
        } else {
            document.getElementById('lobby-overlay').style.display = 'none';
        }

        // Sort Algorithm
        filtered.sort((a, b) => {
            if (a.isLocal) return -1;
            if (b.isLocal) return 1;
            if (!a.meta.isAdmitted && b.meta.isAdmitted) return -1; // Lobby on top
            if (a.meta.isAdmitted && !b.meta.isAdmitted) return 1;
            
            if (a.meta.role === 'teacher' && b.meta.role !== 'teacher') return -1;
            if (a.meta.role !== 'teacher' && b.meta.role === 'teacher') return 1;
            
            if (a.meta.isHandRaised && !b.meta.isHandRaised) return -1;
            if (!a.meta.isHandRaised && b.meta.isHandRaised) return 1;
            
            if (a.meta.isHandRaised && b.meta.isHandRaised) {
                return (a.meta.handRaisedAt || 0) - (b.meta.handRaisedAt || 0);
            }
            
            return a.id.localeCompare(b.id);
        });

        // Render HTML
        let html = '';
        let currentSection = '';

        filtered.forEach((p, index) => {
            let section = p.meta.role === 'teacher' ? 'Giáo viên' : 'Học sinh';
            if (!p.meta.isAdmitted) section = 'Phòng chờ';
            
            if (section !== currentSection) {
                html += `<div style="font-size: 11px; color: #888; padding: 10px 15px 5px 15px; text-transform: uppercase; font-weight: bold;">${section}</div>`;
                currentSection = section;
            }

            const isMuted = !p.participant.isMicrophoneEnabled;
            const isCamOn = p.participant.isCameraEnabled;
            const handIcon = p.meta.isHandRaised ? `<span style="background: #eab308; color: #000; border-radius: 4px; padding: 2px 4px; font-size: 10px; font-weight: bold; margin-right: 5px;">✋</span>` : '';
            const meLabel = p.isLocal ? ' <span style="color: #aaa; font-size: 11px;">(Bạn)</span>' : '';
            
            const micColor = isMuted ? '#ef4444' : '#10b981';
            const micIcon = isMuted ? 'fa-microphone-slash' : 'fa-microphone';
            const camColor = isCamOn ? '#10b981' : '#ef4444';
            const camIcon = isCamOn ? 'fa-video' : 'fa-video-slash';

            // Hover Menu for Teachers
            let hoverMenu = '';
            if (!p.isLocal && window.currentUserRole === 'teacher') {
                if (!p.meta.isAdmitted) {
                    hoverMenu = `
                    <div class="roster-actions" style="display: none; gap: 5px;">
                        <button onclick="handleAdmit('${p.id}')" title="Duyệt vào lớp" style="background: #10b981; border: none; color: white; padding: 4px; border-radius: 4px; cursor: pointer;"><i class="fa-solid fa-check"></i></button>
                        <button onclick="handleKick('${p.id}')" title="Từ chối" style="background: #ef4444; border: none; color: white; padding: 4px; border-radius: 4px; cursor: pointer;"><i class="fa-solid fa-xmark"></i></button>
                    </div>
                    `;
                } else {
                    hoverMenu = `
                    <div class="roster-actions" style="display: none; gap: 5px;">
                        <button onclick="handleSendLobby('${p.id}')" title="Đưa ra phòng chờ" style="background: #eab308; border: none; color: white; padding: 4px; border-radius: 4px; cursor: pointer;"><i class="fa-solid fa-person-walking-arrow-right"></i></button>
                        ${!isMuted ? `<button onclick="handleForceMute('${p.id}')" title="Tắt Mic" style="background: #444; border: none; color: white; padding: 4px; border-radius: 4px; cursor: pointer;"><i class="fa-solid fa-microphone-slash"></i></button>` 
                                   : `<button onclick="handleAskUnmute('${p.id}')" title="Yêu cầu bật Mic" style="background: #3b82f6; border: none; color: white; padding: 4px; border-radius: 4px; cursor: pointer;"><i class="fa-solid fa-microphone"></i></button>`}
                        ${p.meta.isHandRaised ? `<button onclick="handleLowerHand('${p.id}')" title="Hạ tay" style="background: #444; border: none; color: white; padding: 4px; border-radius: 4px; cursor: pointer;"><i class="fa-solid fa-hand-holding-hand"></i></button>` : ''}
                        <button onclick="handleKick('${p.id}')" title="Đuổi" style="background: #ef4444; border: none; color: white; padding: 4px; border-radius: 4px; cursor: pointer;"><i class="fa-solid fa-ban"></i></button>
                    </div>
                    `;
                }
            }

            html += `
            <div onmouseover="this.querySelector('.roster-actions') && (this.querySelector('.roster-actions').style.display='flex'); this.style.background='#333'" 
                 onmouseout="this.querySelector('.roster-actions') && (this.querySelector('.roster-actions').style.display='none'); this.style.background='transparent'" 
                 style="padding: 10px 15px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #2a2a2a; transition: background 0.2s;">
                
                <div style="display: flex; align-items: center; gap: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                    <div style="width: 32px; height: 32px; border-radius: 50%; background: #4b5563; display: flex; align-items: center; justify-content: center; font-weight: bold; color: white; font-size: 14px;">
                        ${(p.meta.displayName || p.id).charAt(0).toUpperCase()}
                    </div>
                    <div>
                        <div style="color: white; font-size: 14px; font-weight: 500;">${handIcon}${p.meta.displayName || p.id}${meLabel}</div>
                    </div>
                </div>

                <div style="display: flex; align-items: center; gap: 12px;">
                    ${hoverMenu}
                    <div style="display: flex; gap: 10px; color: #888;">
                        <i class="fa-solid ${micIcon}" style="color: ${micColor}; font-size: 13px;"></i>
                        <i class="fa-solid ${camIcon}" style="color: ${camColor}; font-size: 13px;"></i>
                    </div>
                </div>
            </div>
            `;
        });
        
        listDiv.innerHTML = html;
    }

    // Handlers
    function handleForceMute(targetId) {
        if (!window.currentRoom) return;
        const payload = JSON.stringify({ type: 'roster_force_mute', target: targetId });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
        showToast("Đã tắt mic học sinh.");
    }
    
    function handleAskUnmute(targetId) {
        if (!window.currentRoom) return;
        const payload = JSON.stringify({ type: 'roster_ask_unmute', target: targetId });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
        showToast("Đã gởi yêu cầu bật mic đến học sinh.");
    }
    
    function handleKick(targetId) {
        if (!window.currentRoom) return;
        const payload = JSON.stringify({ type: 'roster_kick', target: targetId });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
        showToast("Đã mời học sinh rời khỏi lớp.");
    }

    function handleMuteAll() {
        if (!window.currentRoom) return;
        const payload = JSON.stringify({ type: 'roster_mute_all' });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
        showToast("Đã tắt mic toàn bộ lớp.");
    }

    function handleLowerAllHands() {
        if (!window.currentRoom) return;
        const payload = JSON.stringify({ type: 'roster_lower_all_hands' });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
        showToast("Đã hạ tay toàn bộ lớp.");
    }

    function handleLowerHand(targetId) {
        if (!window.currentRoom) return;
        const payload = JSON.stringify({ type: 'roster_lower_hand', target: targetId });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
    }

    function handleToggleLobby() {
        const toggle = document.getElementById('lobby-toggle');
        window.lobbyEnabled = toggle.checked;
        const payload = JSON.stringify({ type: 'roster_lobby_status', enabled: window.lobbyEnabled });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
        renderRoster();
    }

    function handleAdmit(targetId) {
        if (!window.currentRoom) return;
        const payload = JSON.stringify({ type: 'roster_admit', target: targetId });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
    }

    function handleSendLobby(targetId) {
        if (!window.currentRoom) return;
        const payload = JSON.stringify({ type: 'roster_send_lobby', target: targetId });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
    }

    function handleAdmitAll() {
        if (!window.currentRoom) return;
        const payload = JSON.stringify({ type: 'roster_admit_all' });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
    }
    
    // ==========================================
    // EMOJI REACTIONS LOGIC
    // ==========================================
    
    function toggleReactionMenu(event) {
        const menu = document.getElementById('reaction-menu');
        menu.style.display = menu.style.display === 'none' ? 'flex' : 'none';
        if (event) event.stopPropagation();
    }
    
    document.addEventListener('click', (e) => {
        const menu = document.getElementById('reaction-menu');
        const btn = document.getElementById('react-btn');
        if (menu && menu.style.display === 'flex' && !menu.contains(e.target) && (!btn || !btn.contains(e.target))) {
            menu.style.display = 'none';
        }
    });

    function triggerReaction(emoji) {
        document.getElementById('reaction-menu').style.display = 'none';
        if (!window.currentRoom || !window.currentRoom.localParticipant) return;
        const payload = JSON.stringify({ type: 'roster_reaction', emoji: emoji, sender: window.currentRoom.localParticipant.identity });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
        
        // Show for self
        showReactionAnimation(emoji, window.currentRoom.localParticipant.identity);
    }
    
    function showReactionAnimation(emoji, senderId) {
        const vidBubble = document.getElementById('participant-' + senderId);
        const emojiEl = document.createElement('div');
        emojiEl.className = 'emoji-bubble';
        emojiEl.innerText = emoji;
        
        if (vidBubble) {
            const rect = vidBubble.getBoundingClientRect();
            emojiEl.style.left = (rect.left + rect.width / 2) + 'px';
            emojiEl.style.bottom = (window.innerHeight - rect.top) + 'px';
        } else {
            emojiEl.style.left = (40 + Math.random() * 20) + '%';
        }
        
        document.body.appendChild(emojiEl);
        setTimeout(() => {
            if (emojiEl.parentNode) emojiEl.remove();
        }, 3000);
    }

    // Modal popup Ask to Unmute
    window.acceptUnmuteRequest = function() {
        document.getElementById('ask-unmute-modal').style.display = 'none';
        if (window.currentRoom && window.currentRoom.localParticipant) {
            window.currentRoom.localParticipant.setMicrophoneEnabled(true).catch(e => console.error(e));
        }
    }

    // Tự động kết nối LiveKit khi test trên Browser (Không phải JCEF)
    setTimeout(() => {
        if (typeof window.cefQuery === 'undefined') {
            console.log('Chạy trên Browser (Không phải JCEF). Tự động kết nối LiveKit để test...');
            connectToLiveKit();
        }
    }, 1500);

// Explicitly expose functions to window since this is a module
window.applyBoardSettings = applyBoardSettings;
window.broadcastYTState = broadcastYTState;
window.closeCoWatchYT = closeCoWatchYT;
window.exportToPDF = exportToPDF;
window.handleAdmit = handleAdmit;
window.handleAdmitAll = handleAdmitAll;
window.handleAskUnmute = handleAskUnmute;
window.handleForceMute = handleForceMute;
window.handleKick = handleKick;
window.handleLowerAllHands = handleLowerAllHands;
window.handleLowerHand = handleLowerHand;
window.handleMuteAll = handleMuteAll;
window.handleSendLobby = handleSendLobby;
window.handleToggleLobby = handleToggleLobby;
window.insertMathFormula = insertMathFormula;
window.insertMermaidDiagram = insertMermaidDiagram;
window.onYTStateChange = onYTStateChange;
window.onYouTubeIframeAPIReady = onYouTubeIframeAPIReady;
window.openYTPlayer = openYTPlayer;
window.parseMetadata = parseMetadata;
window.renderRoster = renderRoster;
window.showReactionAnimation = showReactionAnimation;
window.startCoWatchReels = startCoWatchReels;
window.startCoWatchYT = startCoWatchYT;
window.toggleBoardSettingsModal = toggleBoardSettingsModal;
window.toggleCoWatchModal = toggleCoWatchModal;
window.togglePeopleSidebar = togglePeopleSidebar;
window.toggleReactionMenu = toggleReactionMenu;
window.toggleYTFullscreen = toggleYTFullscreen;
window.triggerCloseBoard = triggerCloseBoard;
window.triggerReaction = triggerReaction;
window.triggerSaveBoard = triggerSaveBoard;

window.closeAppsMenu = closeAppsMenu;
window.connectToLiveKit = connectToLiveKit;
window.handleDocumentUpload = handleDocumentUpload;
window.insertImageToExcalidraw = insertImageToExcalidraw;
window.publishProblem = publishProblem;
window.startVideoCall = startVideoCall;
window.submitCodeForJudging = submitCodeForJudging;
window.toggleAppsMenu = toggleAppsMenu;
window.toggleCodeMode = toggleCodeMode;
window.toggleJudgePanel = toggleJudgePanel;
window.uploadAndInsertImage = uploadAndInsertImage;

// Attach event listener directly to bypass ES module scope isolation
document.addEventListener('DOMContentLoaded', () => {
    const startVideoBtn = document.getElementById('start-video-btn');
    if (startVideoBtn) {
        startVideoBtn.addEventListener('click', startVideoCall);
    }
});
// Fallback in case DOMContentLoaded has already fired (since type="module" defers execution)
if (document.readyState === 'interactive' || document.readyState === 'complete') {
    const startVideoBtn = document.getElementById('start-video-btn');
    // Prevent attaching twice if DOMContentLoaded hasn't fired yet but we are close
    if (startVideoBtn && !startVideoBtn.dataset.listenerAttached) {
        startVideoBtn.dataset.listenerAttached = 'true';
        startVideoBtn.addEventListener('click', startVideoCall);
    }
}
