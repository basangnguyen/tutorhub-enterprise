        let livekitRoom = null;
        let isAudioEnabled = false;
        let isVideoEnabled = false;
        async function connectToLiveKit() {
            if (window.roomState.get('livekitRoom')) return; // Đã kết nối

            const LIVEKIT_URL = window.TUTORHUB_CONFIG.LIVEKIT_URL; 
            
            try {
                const urlParams = new URLSearchParams(window.location.search);
                window.roomState.set('userName', urlParams.get('name') || 'Guest_' + Math.floor(Math.random()*1000));
                window.roomState.set('userRole', urlParams.get('role') === 'teacher' ? 'teacher' : 'student');
                
                // Fallback for compatibility
                window.currentUserName = window.roomState.get('userName');
                window.currentUserRole = window.roomState.get('userRole');

                const currentRoomId = window.roomState.get('boardId') || window.currentBoardId || 'default-room';
                const safeName = encodeURIComponent(window.roomState.get('userName'));
                console.log("=== STARTING LIVEKIT CONNECT ===");
                
                // Fetch token qua CefQuery (bảo mật, Java làm Proxy ẩn)
                const data = await new Promise((resolve, reject) => {
                    console.log("=== INSIDE PROMISE ===", {cefQuery: !!window.cefQuery});
                    if (window.cefQuery) {
                        window.cefQuery({
                            request: `GET_LIVEKIT_TOKEN:${currentRoomId}:${safeName}`,
                            onSuccess: function(response) {
                                console.log("=== onSuccess CALLED ===", response);
                                try {
                                    resolve(JSON.parse(response));
                                } catch (e) {
                                    reject(e);
                                }
                            },
                            onFailure: function(errorCode, errorMessage) {
                                console.log("=== onFailure CALLED ===", errorCode, errorMessage);
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
                
                console.log("=== GOT TOKEN DATA ===", data);
                
                if (!data || !data.token) {
                    console.error('Lỗi lấy token từ server!', data);
                    alert('Lỗi lấy token LiveKit từ Server: ' + JSON.stringify(data));
                    return;
                }

                const room = new LivekitClient.Room({
                    adaptiveStream: true,
                    dynacast: true,
                });
                window.roomState.set('livekitRoom', room);
                window.currentRoom = room; // Backwards compatibility

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
                            if (window.videoLayoutManager) {
                                window.videoLayoutManager.addVideo(participant.identity, element);
                            } else {
                                const container = document.getElementById('video-layout-container') || document.getElementById('video-sidebar');
                                if (container) container.appendChild(element);
                            }
                        } else {
                            // Audio track (ẩn)
                            element.style.display = 'none';
                            document.body.appendChild(element);
                        }
                    }
                });

                room.on(LivekitClient.RoomEvent.TrackUnsubscribed, (track, publication, participant) => {
                    track.detach();
                    
                    if (track.source === LivekitClient.Track.Source.ScreenShare) {
                        const ssElement = document.getElementById('screenshare-' + participant.identity);
                        if (ssElement) ssElement.remove();
                    } else if (track.kind === LivekitClient.Track.Kind.Video) {
                        if (window.videoLayoutManager) {
                            window.videoLayoutManager.removeVideo(participant.identity);
                        } else {
                            const vidElement = document.getElementById('participant-video-' + participant.identity) || document.getElementById('participant-' + participant.identity);
                            if (vidElement) vidElement.remove();
                        }
                    }
                });

                room.on(LivekitClient.RoomEvent.LocalTrackPublished, (publication, participant) => {
                    if (publication.track.kind === LivekitClient.Track.Kind.Video) {
                        const element = publication.track.attach();
                        element.style.transform = 'scaleX(-1)';
                        if (window.videoLayoutManager) {
                            window.videoLayoutManager.addVideo(participant.identity, element);
                            const myVid = window.videoLayoutManager.videos.get(participant.identity);
                            if (myVid) {
                                const nameTag = myVid.wrapperEl.querySelector('.video-name-tag');
                                if (nameTag && !nameTag.innerText.includes("(Bạn)")) nameTag.innerText += " (Bạn)";
                            }
                        } else {
                            const container = document.getElementById('video-layout-container') || document.getElementById('video-sidebar');
                            if (container) container.appendChild(element);
                        }
                    }
                });

                room.on(LivekitClient.RoomEvent.LocalTrackUnpublished, (publication, participant) => {
                    if (publication.track) {
                        publication.track.detach();
                    }
                    if (publication.track.kind === LivekitClient.Track.Kind.Video) {
                        if (window.videoLayoutManager) {
                            window.videoLayoutManager.removeVideo(participant.identity);
                        } else {
                            const wrapper = document.getElementById('local-video-wrapper') || document.getElementById('participant-video-' + participant.identity);
                            if (wrapper) wrapper.remove();
                        }
                    }
                });

                room.on(LivekitClient.RoomEvent.ParticipantDisconnected, (participant) => {
                    if (window.videoLayoutManager) {
                        window.videoLayoutManager.removeVideo(participant.identity);
                    }
                    const wrapper = document.getElementById('participant-' + participant.identity) || document.getElementById('participant-video-' + participant.identity);
                    if (wrapper) wrapper.remove();
                    if (typeof renderRoster === 'function') renderRoster();
                });

                room.on(LivekitClient.RoomEvent.ParticipantConnected, (participant) => {
                    if (typeof renderRoster === 'function') renderRoster();
                    // Teacher sends current lobby state to new participants
                    if (window.roomState.get('userRole') === 'teacher' && window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant) {
                        const isLobbyEnabled = window.lobbyEnabled !== false; // default true
                        const payload = JSON.stringify({ type: 'roster_lobby_status', enabled: isLobbyEnabled });
                        window.roomState.get('livekitRoom').localParticipant.publishData(new TextEncoder().encode(payload), { reliable: true });
                    }
                });
                room.on(LivekitClient.RoomEvent.TrackMuted, () => { if (typeof renderRoster === 'function') renderRoster(); });
                room.on(LivekitClient.RoomEvent.TrackUnmuted, () => { if (typeof renderRoster === 'function') renderRoster(); });
                room.on(LivekitClient.RoomEvent.ParticipantMetadataChanged, () => { if (typeof renderRoster === 'function') renderRoster(); });
                room.on(LivekitClient.RoomEvent.ActiveSpeakersChanged, (speakers) => { 
                    if (typeof renderRoster === 'function') renderRoster(); 
                    if (window.videoLayoutManager) {
                        window.videoLayoutManager.setActiveSpeaker(speakers.map(s => s.identity));
                    }
                });

                room.on(LivekitClient.RoomEvent.DataReceived, (payload, participant, kind, topic) => {
                    try {
                        const decoder = new TextDecoder();
                        const data = JSON.parse(decoder.decode(payload));
                        
                        if (data.type === 'breakout_start' || data.type === 'breakout_end' || data.type === 'breakout_broadcast') {
                            // Breakout Rooms — chuyển cho BreakoutManager xử lý
                            if (window.breakoutManager) {
                                window.breakoutManager.handleBreakoutCommand(data);
                            }
                        } else if (data.type === 'quiz_vote') {
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
                            if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant) {
                                window.roomState.get('livekitRoom').localParticipant.setMicrophoneEnabled(false).then(() => {
                                    showToast("Giáo viên đã tắt Mic của bạn để giữ trật tự!", 5000);
                                }).catch(err => {
                                    console.error("Lỗi khi bị Mute All: ", err);
                                });
                            }
                        } else if (data.type === 'roster_force_mute') {
                            if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant && data.target === window.roomState.get('livekitRoom').localParticipant.identity) {
                                window.roomState.get('livekitRoom').localParticipant.setMicrophoneEnabled(false).catch(e => console.error(e));
                                showToast("Giáo viên đã tắt Mic của bạn.", 5000);
                            }
                        } else if (data.type === 'roster_ask_unmute') {
                            if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant && data.target === window.roomState.get('livekitRoom').localParticipant.identity) {
                                document.getElementById('ask-unmute-modal').style.display = 'block';
                            }
                        } else if (data.type === 'roster_kick') {
                            if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant && data.target === window.roomState.get('livekitRoom').localParticipant.identity) {
                                triggerCloseBoard();
                            }
                        } else if (data.type === 'roster_mute_all') {
                            if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant && window.roomState.get('userRole') !== 'teacher') {
                                window.roomState.get('livekitRoom').localParticipant.setMicrophoneEnabled(false).catch(e => console.error(e));
                                showToast("Giáo viên đã tắt Mic của bạn để giữ trật tự!", 5000);
                            }
                        } else if (data.type === 'roster_lower_all_hands') {
                            if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant) {
                                const currentMeta = parseMetadata(window.roomState.get('livekitRoom').localParticipant.metadata);
                                if (currentMeta.isHandRaised) {
                                    currentMeta.isHandRaised = false;
                                    window.roomState.get('livekitRoom').localParticipant.setMetadata(JSON.stringify(currentMeta));
                                }
                            }
                        } else if (data.type === 'roster_lower_hand') {
                            if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant && data.target === window.roomState.get('livekitRoom').localParticipant.identity) {
                                const currentMeta = parseMetadata(window.roomState.get('livekitRoom').localParticipant.metadata, window.roomState.get('livekitRoom').localParticipant.identity);
                                currentMeta.isHandRaised = false;
                                
                                if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                                window.rosterMetadataCache[window.roomState.get('livekitRoom').localParticipant.identity] = currentMeta;
                                
                                try { window.roomState.get('livekitRoom').localParticipant.setMetadata(JSON.stringify(currentMeta)); } catch(e){}
                                
                                const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.roomState.get('livekitRoom').localParticipant.identity, metadata: JSON.stringify(currentMeta) });
                                window.roomState.get('livekitRoom').localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });
                            }
                        } else if (data.type === 'roster_admit') {
                            if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant && data.target === window.roomState.get('livekitRoom').localParticipant.identity) {
                                const currentMeta = parseMetadata(window.roomState.get('livekitRoom').localParticipant.metadata, window.roomState.get('livekitRoom').localParticipant.identity);
                                currentMeta.isAdmitted = true;
                                if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                                window.rosterMetadataCache[window.roomState.get('livekitRoom').localParticipant.identity] = currentMeta;
                                try { window.roomState.get('livekitRoom').localParticipant.setMetadata(JSON.stringify(currentMeta)); } catch(e){}
                                const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.roomState.get('livekitRoom').localParticipant.identity, metadata: JSON.stringify(currentMeta) });
                                window.roomState.get('livekitRoom').localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });
                                if (typeof renderRoster === 'function') renderRoster();
                                showToast("Giáo viên đã duyệt bạn vào lớp!", 5000);
                            }
                        } else if (data.type === 'roster_send_lobby') {
                            if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant && data.target === window.roomState.get('livekitRoom').localParticipant.identity) {
                                const currentMeta = parseMetadata(window.roomState.get('livekitRoom').localParticipant.metadata, window.roomState.get('livekitRoom').localParticipant.identity);
                                currentMeta.isAdmitted = false;
                                if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                                window.rosterMetadataCache[window.roomState.get('livekitRoom').localParticipant.identity] = currentMeta;
                                try { window.roomState.get('livekitRoom').localParticipant.setMetadata(JSON.stringify(currentMeta)); } catch(e){}
                                const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.roomState.get('livekitRoom').localParticipant.identity, metadata: JSON.stringify(currentMeta) });
                                window.roomState.get('livekitRoom').localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });
                                if (typeof renderRoster === 'function') renderRoster();
                            }
                        } else if (data.type === 'roster_admit_all') {
                            if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant && window.roomState.get('userRole') === 'student') {
                                const currentMeta = parseMetadata(window.roomState.get('livekitRoom').localParticipant.metadata, window.roomState.get('livekitRoom').localParticipant.identity);
                                if (!currentMeta.isAdmitted) {
                                    currentMeta.isAdmitted = true;
                                    if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                                    window.rosterMetadataCache[window.roomState.get('livekitRoom').localParticipant.identity] = currentMeta;
                                    try { window.roomState.get('livekitRoom').localParticipant.setMetadata(JSON.stringify(currentMeta)); } catch(e){}
                                    const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.roomState.get('livekitRoom').localParticipant.identity, metadata: JSON.stringify(currentMeta) });
                                    window.roomState.get('livekitRoom').localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });
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

                // Apply mic/cam state from lobby
                if (window._lobbyMicEnabled) {
                    try {
                        await room.localParticipant.setMicrophoneEnabled(true);
                        isAudioEnabled = true;
                        const micBtn = document.getElementById('toggle-mic-btn');
                        if (micBtn) {
                            micBtn.classList.add('active');
                            micBtn.querySelector('i').className = 'fa-solid fa-microphone';
                            micBtn.querySelector('i').style.color = '';
                        }
                    } catch (e) { console.error('Failed to enable mic:', e); }
                }

                if (window._lobbyCamEnabled) {
                    try {
                        if (window._lobbyProcessedTrack) {
                            await room.localParticipant.publishTrack(window._lobbyProcessedTrack, { source: LivekitClient.Track.Source.Camera });
                        } else {
                            await room.localParticipant.setCameraEnabled(true);
                        }
                        const camBtn = document.getElementById('start-video-btn');
                        if (camBtn) {
                            camBtn.classList.add('active');
                            camBtn.querySelector('i').className = 'fa-solid fa-video';
                            camBtn.querySelector('i').style.color = '';
                        }
                        isVideoEnabled = true;
                    } catch (e) { console.error('Failed to enable cam:', e); }
                }
                
                // Cập nhật Metadata cho Roster
                const initMetadata = JSON.stringify({
                    role: window.roomState.get('userRole'),
                    displayName: window.roomState.get('userName'),
                    isHandRaised: false,
                    handRaisedAt: null,
                    isAdmitted: window.roomState.get('userRole') === 'teacher' ? true : false // Teacher always admitted, student depends on lobby
                });
                
                if (!window.rosterMetadataCache) window.rosterMetadataCache = {};
                window.rosterMetadataCache[window.roomState.get('livekitRoom').localParticipant.identity] = JSON.parse(initMetadata);

                try {
                    if (room.localParticipant.setMetadata) {
                        await room.localParticipant.setMetadata(initMetadata);
                    }
                } catch(e) {
                    console.warn("Lỗi setMetadata, có thể do thiếu quyền từ Server: ", e);
                }
                
                // Broadcast backup
                setTimeout(() => {
                    if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant) {
                        const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.roomState.get('livekitRoom').localParticipant.identity, metadata: initMetadata });
                        const encoder = new TextEncoder();
                        window.roomState.get('livekitRoom').localParticipant.publishData(encoder.encode(metaPayload), { reliable: true });
                    }
                }, 1000);
                
                // Gởi thêm 1 lần nữa sau 5 giây để chắc chắn người khác nhận được
                setTimeout(() => {
                    if (window.roomState.get('livekitRoom') && window.roomState.get('livekitRoom').localParticipant) {
                        const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.roomState.get('livekitRoom').localParticipant.identity, metadata: initMetadata });
                        window.roomState.get('livekitRoom').localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });
                    }
                }, 5000);
            } catch (e) {
                console.error("Lỗi kết nối LiveKit:", e);
                alert("Lỗi kết nối LiveKit: " + e.message);
            }
        }

        async function toggleMic() {
            if (!window.roomState.get('livekitRoom')) {
                alert("Đang kết nối tới phòng học, vui lòng thử lại sau vài giây...");
                return;
            }
            
            const btn = document.getElementById('toggle-mic-btn') || document.getElementById('mute-all-btn');
            let icon = null;
            if (btn) icon = btn.querySelector('i');
            
            try {
                if (isAudioEnabled) {
                    // Tắt Mic
                    await window.roomState.get('livekitRoom').localParticipant.setMicrophoneEnabled(false);
                    isAudioEnabled = false;
                    
                    if (btn) btn.classList.remove('active');
                    if (icon) {
                        icon.className = 'fa-solid fa-microphone-slash';
                        icon.style.color = '#ef4444';
                    }
                } else {
                    // Bật Mic
                    await window.roomState.get('livekitRoom').localParticipant.setMicrophoneEnabled(true);
                    isAudioEnabled = true;
                    
                    if (btn) btn.classList.add('active');
                    if (icon) {
                        icon.className = 'fa-solid fa-microphone';
                        icon.style.color = '';
                    }
                }
            } catch (e) {
                console.error("Lỗi toggle mic:", e);
                alert("Không thể thao tác Mic. Lỗi chi tiết: " + e.message);
                
                // Trả về trạng thái cũ nếu lỗi
                if (isAudioEnabled) {
                    isAudioEnabled = false;
                    if (btn) btn.classList.remove('active');
                    if (icon) {
                        icon.className = 'fa-solid fa-microphone-slash';
                        icon.style.color = '#ef4444';
                    }
                }
            }
        }

        async function startVideoCall() {
            if (!window.roomState.get('livekitRoom')) {
                alert("Đang kết nối tới phòng học, vui lòng thử lại sau vài giây...");
                return;
            }
            
            const btn = document.getElementById('start-video-btn');
            const icon = btn.querySelector('i');
            
            try {
                if (isVideoEnabled) {
                    // Tắt Camera
                    if (window._lobbyProcessedTrack) {
                        await window.roomState.get('livekitRoom').localParticipant.unpublishTrack(window._lobbyProcessedTrack, true);
                    } else {
                        await window.roomState.get('livekitRoom').localParticipant.setCameraEnabled(false);
                    }
                    isVideoEnabled = false;
                    
                    // Cập nhật UI nút
                    btn.classList.remove('active');
                    if (icon) {
                        icon.className = 'fa-solid fa-video-slash';
                        icon.style.color = '#ef4444';
                    }
                    // Layout is handled by LocalTrackUnpublished
                } else {
                    // Bật Camera
                    if (window._lobbyProcessedTrack) {
                        await window.roomState.get('livekitRoom').localParticipant.publishTrack(window._lobbyProcessedTrack, { source: LivekitClient.Track.Source.Camera });
                    } else {
                        await window.roomState.get('livekitRoom').localParticipant.setCameraEnabled(true);
                    }
                    isVideoEnabled = true;
                    
                    // Cập nhật UI nút
                    btn.classList.add('active');
                    if (icon) {
                        icon.className = 'fa-solid fa-video';
                        icon.style.color = '';
                    }
                    // Layout is handled by LocalTrackPublished
                }

            } catch (e) {
                console.error("Lỗi toggle camera:", e);
                alert("Không thể thao tác Camera. Lỗi chi tiết: " + e.message);
                
                // Trả về trạng thái cũ nếu lỗi
                if (isVideoEnabled) {
                    isVideoEnabled = false;
                    btn.classList.remove('active');
                    if (icon) {
                        icon.className = 'fa-solid fa-video-slash';
                        icon.style.color = '#ef4444';
                    }
                }
            }
        }
        
