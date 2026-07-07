function closeCodeModal() {
    document.getElementById('code-modal').style.display = 'none';
}

function insertCodeShape() {
    const codeStr = document.getElementById('code-input').value;
    if (!codeStr.trim()) return;

    if (window.addCodeNode) {
        window.addCodeNode(codeStr);
    }
    closeCodeModal();
}

/* PhET Simulation Modal Logic */
// Dữ liệu phetSims (119+ thí nghiệm) đã được nạp từ file src/phetData.js

function renderPhetGrid(query = '') {
    const grid = document.getElementById('phet-grid');
    grid.innerHTML = '';

    const sims = window.phetSims || [];
    const lowerQuery = query.toLowerCase();
    const filteredSims = sims.filter(sim =>
        sim.title.toLowerCase().includes(lowerQuery) ||
        sim.category.toLowerCase().includes(lowerQuery)
    );

    if (filteredSims.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1 / -1; text-align: center; color: #64748b; padding: 20px;">Không tìm thấy thí nghiệm nào phù hợp.</div>';
        return;
    }

    filteredSims.forEach(sim => {
        const card = document.createElement('div');
        card.style.border = '1px solid #ccc';
        card.style.borderRadius = '8px';
        card.style.padding = '15px';
        card.style.cursor = 'pointer';
        card.style.backgroundColor = '#f9fafb';
        card.style.transition = 'background-color 0.2s, border-color 0.2s';
        card.style.textAlign = 'center';

        card.onmouseover = () => card.style.backgroundColor = '#e0e7ff';
        card.onmouseout = () => card.style.backgroundColor = '#f9fafb';
        card.onclick = () => {
            if (window.addPhetNode) {
                window.addPhetNode(sim.url);
            }
            closePhetModal();
        };

        card.innerHTML = `
        <div style="font-size: 12px; color: #6366f1; font-weight: bold; margin-bottom: 5px;">${sim.category}</div>
        <div style="font-weight: 600; font-size: 14px; color: #1e293b;">${sim.title}</div>
      `;
        grid.appendChild(card);
    });
}

function filterPhet() {
    const query = document.getElementById('phet-search').value;
    renderPhetGrid(query);
}

function openPhetModal() {
    document.getElementById('phet-search').value = '';
    renderPhetGrid();
    document.getElementById('phet-modal').style.display = 'flex';
}

function closePhetModal() {
    document.getElementById('phet-modal').style.display = 'none';
}

// Khởi tạo LiveKit Client (sử dụng thư viện livekit-client tải từ CDN)
function toggleChatbox() {
    const chatbox = document.getElementById('chatbox');
    if (chatbox.style.display === 'none') {
        chatbox.style.display = 'flex';
        document.getElementById('chat-input').focus();
    } else {
        chatbox.style.display = 'none';
    }
}

var isScreenSharing = false;
async function toggleScreenShare() {
    if (!window.currentRoom) return;
    const btn = document.getElementById('share-screen-btn');
    try {
        if (isScreenSharing) {
            await window.currentRoom.localParticipant.setScreenShareEnabled(false);
            isScreenSharing = false;
            btn.classList.remove('active');
        } else {
            await window.currentRoom.localParticipant.setScreenShareEnabled(true);
            isScreenSharing = true;
            btn.classList.add('active');
        }
    } catch (e) {
        console.info("SCREEN_SHARE_ERROR: " + e.message + " | Name: " + e.name);
    }
}

async function toggleRaiseHand() {
    if (!window.currentRoom) return;
    const btn = document.getElementById('raise-hand-btn');

    try {
        // Parse current metadata
        let currentMeta = { role: window.currentUserRole, displayName: window.currentUserName, isHandRaised: false, handRaisedAt: null };
        if (window.currentRoom.localParticipant.metadata) {
            currentMeta = parseMetadata(window.currentRoom.localParticipant.metadata);
        }

        // Toggle state
        currentMeta.isHandRaised = !currentMeta.isHandRaised;
        currentMeta.handRaisedAt = currentMeta.isHandRaised ? Date.now() : null;

        await window.currentRoom.localParticipant.setMetadata(JSON.stringify(currentMeta));

        if (currentMeta.isHandRaised) {
            showToast("Bạn đã giơ tay phát biểu ✋", 3000);
            btn.classList.add('active');
        } else {
            showToast("Bạn đã hạ tay xuống.", 3000);
            btn.classList.remove('active');
        }

        // Fallback broadcast if server drops metadata
        const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: window.currentUserName, metadata: JSON.stringify(currentMeta) });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });

        if (typeof renderRoster === 'function') renderRoster();

    } catch (e) {
        console.info("RAISE_HAND_ERROR: " + e.message);
    }
}

async function muteAllStudents() {
    if (!window.currentRoom) return;
    try {
        // Send a mute_all signal to all users
        const payload = JSON.stringify({ type: 'mute_all', sender: window.currentRoom.localParticipant.identity });
        const encoder = new TextEncoder();
        await window.currentRoom.localParticipant.publishData(encoder.encode(payload), { reliable: true });

        showToast("Đã gửi lệnh tắt Mic toàn lớp!", 3000);
    } catch (e) {
        console.error("MUTE_ALL_ERROR: ", e);
    }
}

let mediaRecorder;
let recordedChunks = [];
let isRecording = false;

async function toggleRecording() {
    const btn = document.getElementById('record-btn');
    if (!isRecording) {
        try {
            const stream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: true });
            mediaRecorder = new MediaRecorder(stream, { mimeType: 'video/webm' });

            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) recordedChunks.push(event.data);
            };

            mediaRecorder.onstop = async () => {
                const blob = new Blob(recordedChunks, { type: 'video/webm' });
                recordedChunks = [];

                showToast("Đang tải Video lên Server (Backblaze)...", 5000);
                const formData = new FormData();
                formData.append('video', blob, 'record.webm');

                try {
                    const res = await fetch(`${window.TUTORHUB_CONFIG.SYNC_SERVER_URL}/upload-record`, {
                        method: 'POST',
                        body: formData
                    });
                    const data = await res.json();
                    if (data.success) {
                        const finalUrl = data.url || data.localPath;
                        showToast(`Đã lưu Video! <a href="#" onclick="window.cefQuery({request: 'OPEN_URL:' + '${finalUrl}'}); return false;" style="color: #60a5fa; text-decoration: underline;">👉 Bấm vào đây để xem</a>`, 15000);
                        console.info("RECORDING_URL: " + finalUrl);
                    } else {
                        console.error("Upload error:", data.error);
                        showToast("Lỗi khi tải video lên server!", 5000);
                    }
                } catch (err) {
                    console.error("Upload failed", err);
                }

                // Reset stream tracks
                stream.getTracks().forEach(t => t.stop());
                btn.classList.remove('active');
                isRecording = false;
            };

            mediaRecorder.start();
            isRecording = true;
            btn.classList.add('active');
            showToast("Bắt đầu ghi hình lớp học!", 3000);

        } catch (err) {
            console.error("Lỗi bắt đầu ghi hình: ", err);
            showToast("Lỗi: " + err.message, 5000);
        }
    } else {
        mediaRecorder.stop();
    }
}

function sendChatMessage() {
    const input = document.getElementById('chat-input');
    const text = input.value.trim();
    if (!text || !window.currentRoom) return;

    const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const payload = JSON.stringify({ type: 'chat', sender: 'Me', time: timeStr, text: text });

    const encoder = new TextEncoder();
    try {
        window.currentRoom.localParticipant.publishData(encoder.encode(payload), { reliable: true });
    } catch (e) {
        console.info("CHAT_SEND_ERROR: " + e.message);
    }

    appendChatMessage('Me', timeStr, text, true);
    input.value = '';
}

window.sendQuizVote = function (shapeId, optionIndex) {
    if (!window.currentRoom) return;
    const payload = JSON.stringify({ type: 'quiz_vote', shapeId: shapeId, option: optionIndex });
    const encoder = new TextEncoder();
    try {
        window.currentRoom.localParticipant.publishData(encoder.encode(payload), { reliable: true });
        // Tự kích hoạt event cho chính mình để local React cũng nhận được
        window.dispatchEvent(new CustomEvent('quiz_vote_received', { detail: { shapeId, option: optionIndex } }));
    } catch (e) {
        console.info("QUIZ_VOTE_ERROR: " + e.message);
    }
};

// Bổ sung phím tắt toàn cục
document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
        const modals = ['math-modal', 'mermaid-modal', 'phet-modal', 'cowatch-modal', 'board-settings-modal'];
        modals.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = 'none';
        });
    }
    // Ngăn phím Delete/Backspace vô tình xóa shape trên bảng khi đang gõ text
    if ((e.key === 'Backspace' || e.key === 'Delete') && (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.tagName === 'MATH-FIELD')) {
        e.stopPropagation();
    }
});

// Xử lý thanh trượt độ dày nét bút
function updateCustomStrokeWidth(val) {
    const v = parseInt(val);
    if (v === 0) {
        window.__TLDRAW_CUSTOM_STROKE_WIDTH = undefined;
        document.getElementById('custom-stroke-val').innerText = 'Nét: Mặc định';
    } else {
        window.__TLDRAW_CUSTOM_STROKE_WIDTH = v;
        document.getElementById('custom-stroke-val').innerText = 'Nét: ' + v;
    }
}

function appendChatMessage(sender, time, text, isSelf) {
    const messagesDiv = document.getElementById('chat-messages');
    const msgDiv = document.createElement('div');
    msgDiv.style.display = 'flex';
    msgDiv.style.flexDirection = 'column';
    msgDiv.style.alignItems = isSelf ? 'flex-end' : 'flex-start';

    msgDiv.innerHTML = `
                <span style="font-size: 11px; color: #aaa; margin-bottom: 2px;">${sender} - ${time}</span>
                <div style="background: ${isSelf ? '#10b981' : '#333'}; color: white; padding: 8px 12px; border-radius: 6px; font-size: 13px; max-width: 85%; word-wrap: break-word;">${text}</div>
            `;
    messagesDiv.appendChild(msgDiv);
    messagesDiv.scrollTop = messagesDiv.scrollHeight;
}