
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
            injectRosterStyles();
            sidebar.style.display = 'flex';
            sidebar.style.transform = 'translateX(0)';
            if (window.roomState.get('userRole') === 'teacher') {
                document.getElementById('roster-host-controls').style.display = 'flex';
            }
            renderRoster();
        } else {
            sidebar.style.transform = 'translateX(100%)';
            setTimeout(() => sidebar.style.display = 'none', 300);
        }
    }

    // CSS injection for new roster styles
    function injectRosterStyles() {
        if (document.getElementById('roster-dynamic-styles')) return;
        const style = document.createElement('style');
        style.id = 'roster-dynamic-styles';
        style.innerHTML = `
            .roster-kebab-menu {
                position: relative;
                display: inline-block;
            }
            .roster-kebab-btn {
                background: transparent;
                border: none;
                color: #fff;
                padding: 4px 8px;
                border-radius: 4px;
                cursor: pointer;
                opacity: 0;
                transition: opacity 0.2s, background 0.2s;
            }
            .roster-row:hover .roster-kebab-btn {
                opacity: 1;
            }
            .roster-kebab-btn:hover {
                background: #444;
            }
            .roster-dropdown-content {
                display: none;
                position: absolute;
                right: 0;
                top: 100%;
                background-color: #2a2a2a;
                min-width: 180px;
                box-shadow: 0px 8px 16px 0px rgba(0,0,0,0.5);
                z-index: 9999;
                border-radius: 6px;
                overflow: hidden;
            }
            .roster-dropdown-content button {
                color: white;
                padding: 10px 12px;
                text-decoration: none;
                display: flex;
                align-items: center;
                gap: 10px;
                width: 100%;
                border: none;
                background: transparent;
                text-align: left;
                cursor: pointer;
                font-family: inherit;
                font-size: 13px;
            }
            .roster-dropdown-content button:hover {
                background-color: #3b82f6;
            }
            .roster-kebab-menu.show .roster-dropdown-content {
                display: block;
            }
            .audio-indicator {
                position: relative;
            }
            .audio-indicator::after {
                content: '';
                position: absolute;
                top: -3px; left: -3px; right: -3px; bottom: -3px;
                border: 2px solid #10b981;
                border-radius: 50%;
                animation: roster-pulse 1s infinite alternate;
                display: block;
            }
            @keyframes roster-pulse {
                0% { transform: scale(0.9); opacity: 0.7; }
                100% { transform: scale(1.2); opacity: 0; }
            }
        `;
        document.head.appendChild(style);
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
            const prejoin = document.getElementById('prejoin-lobby');
            if (prejoin) prejoin.style.display = 'flex';
        } else {
            if (typeof window.hideLobbyScreen === 'function') {
                window.hideLobbyScreen();
            } else {
                const prejoin = document.getElementById('prejoin-lobby');
                if (prejoin) prejoin.style.display = 'none';
            }
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
            
            // Add Active Speaker sort
            const speakers = window.activeSpeakers || [];
            const aIsSpeaker = speakers.includes(a.id);
            const bIsSpeaker = speakers.includes(b.id);
            if (aIsSpeaker && !bIsSpeaker) return -1;
            if (!aIsSpeaker && bIsSpeaker) return 1;
            
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

            const isSpeaking = (window.activeSpeakers || []).includes(p.id);
            const avatarClass = isSpeaking ? "audio-indicator" : "";
            
            // Hover Menu for Teachers
            let hoverMenu = '';
            if (!p.isLocal && window.roomState.get('userRole') === 'teacher') {
                if (!p.meta.isAdmitted) {
                    hoverMenu = `
                    <div class="roster-actions" style="display: flex; gap: 5px; opacity: 0; transition: opacity 0.2s;">
                        <button onclick="handleAdmit('${p.id}')" title="Duyệt vào lớp" style="background: #10b981; border: none; color: white; padding: 4px; border-radius: 4px; cursor: pointer;"><i class="fa-solid fa-check"></i></button>
                        <button onclick="handleKick('${p.id}')" title="Từ chối" style="background: #ef4444; border: none; color: white; padding: 4px; border-radius: 4px; cursor: pointer;"><i class="fa-solid fa-xmark"></i></button>
                    </div>
                    `;
                } else {
                    const muteBtn = !isMuted ? `<button onclick="handleForceMute('${p.id}')"><i class="fa-solid fa-microphone-slash"></i> Tắt Mic</button>` 
                                             : `<button onclick="handleAskUnmute('${p.id}')"><i class="fa-solid fa-microphone"></i> Yêu cầu bật Mic</button>`;
                    const lowerHandBtn = p.meta.isHandRaised ? `<button onclick="handleLowerHand('${p.id}')"><i class="fa-solid fa-hand-holding-hand"></i> Hạ tay</button>` : '';
                    
                    const directMute = (!isMuted && isSpeaking) 
                        ? `<button class="roster-kebab-btn" onclick="handleForceMute('${p.id}')" title="Tắt Mic ngay" style="color: #ef4444; font-size: 16px; margin-right: 5px;"><i class="fa-solid fa-microphone-slash"></i></button>`
                        : '';

                    hoverMenu = `
                    <div style="display: flex; align-items: center;">
                        ${directMute}
                        <div class="roster-kebab-menu" onclick="event.stopPropagation(); this.classList.toggle('show');">
                            <button class="roster-kebab-btn"><i class="fa-solid fa-ellipsis-vertical"></i></button>
                            <div class="roster-dropdown-content">
                                ${muteBtn}
                                ${lowerHandBtn}
                                <button onclick="handleSendLobby('${p.id}')"><i class="fa-solid fa-person-walking-arrow-right"></i> Đưa ra phòng chờ</button>
                                <button onclick="handleKick('${p.id}')" style="color: #ef4444;"><i class="fa-solid fa-ban"></i> Đuổi khỏi lớp</button>
                            </div>
                        </div>
                    </div>
                    `;
                }
            }

            html += `
            <div class="roster-row" style="padding: 10px 15px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #2a2a2a; transition: background 0.2s;" 
                 onmouseover="this.style.background='#333'; const acts=this.querySelector('.roster-actions'); if(acts) acts.style.opacity=1;" 
                 onmouseout="this.style.background='transparent'; const acts=this.querySelector('.roster-actions'); if(acts) acts.style.opacity=0; const kebab=this.querySelector('.roster-kebab-menu'); if(kebab) kebab.classList.remove('show');">
                
                <div style="display: flex; align-items: center; gap: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                    <div class="${avatarClass}" style="width: 32px; height: 32px; border-radius: 50%; background: #4b5563; display: flex; align-items: center; justify-content: center; font-weight: bold; color: white; font-size: 14px; position: relative;">
                        ${(p.meta.displayName || p.id).charAt(0).toUpperCase()}
                    </div>
                    <div>
                        <div style="color: white; font-size: 14px; font-weight: 500;">${handIcon}${p.meta.displayName || p.id}${meLabel}</div>
                    </div>
                </div>

                <div style="display: flex; align-items: center; gap: 12px;">
                    ${hoverMenu}
                    <div style="display: flex; gap: 10px; color: #888; width: 40px; justify-content: flex-end;">
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
