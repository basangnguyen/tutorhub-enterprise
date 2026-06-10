
        function toggleChatbox() {
            const chatbox = document.getElementById('chatbox');
            if (chatbox.style.display === 'none') {
                chatbox.style.display = 'flex';
                document.getElementById('chat-input').focus();
            } else {
                chatbox.style.display = 'none';
            }
        }

        let isScreenSharing = false;
        async function toggleScreenShare() {
            if (!window.currentRoom) return;
            const btn = document.getElementById('share-screen-btn');
            try {
                if (isScreenSharing) {
                    await window.currentRoom.localParticipant.setScreenShareEnabled(false);
                    isScreenSharing = false;
                    btn.innerText = "Share Screen";
                    btn.style.background = "#10b981";
                } else {
                    await window.currentRoom.localParticipant.setScreenShareEnabled(true);
                    isScreenSharing = true;
                    btn.innerText = "Stop Share";
                    btn.style.background = "#ef4444";
                }
            } catch (e) {
                console.info("SCREEN_SHARE_ERROR: " + e.message + " | Name: " + e.name);
            }
        }

        async function toggleRaiseHand() {
            if (!window.currentRoom) return;
            const btn = document.getElementById('raise-hand-btn');
            
            try {
                // Send a raise hand signal to all users
                const payload = JSON.stringify({ type: 'raise_hand', sender: window.currentRoom.localParticipant.identity });
                const encoder = new TextEncoder();
                await window.currentRoom.localParticipant.publishData(encoder.encode(payload), { reliable: true });
                
                // Show toast for self
                showToast("Bạn đã giơ tay phát biểu ✋", 3000);
                
                // Add a visual indicator to own video if exists
                const vidBubble = document.getElementById('participant-' + window.currentRoom.localParticipant.identity);
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
                
                // Change button style temporarily
                const originalBg = btn.style.background;
                btn.style.background = '#d97706'; // Darker amber
                btn.innerText = "✋ Đã giơ tay";
                setTimeout(() => {
                    btn.style.background = originalBg;
                    btn.innerText = "✋ Giơ tay";
                }, 5000);
                
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
                            const res = await fetch('http://localhost:1234/upload-record', {
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
                        btn.innerText = "⏺ Ghi hình";
                        btn.style.background = "#dc2626"; // Đỏ tĩnh
                        isRecording = false;
                    };
                    
                    mediaRecorder.start();
                    isRecording = true;
                    btn.innerText = "⏹ Dừng ghi";
                    btn.style.background = "#991b1b"; // Đỏ sậm
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
            
            const timeStr = new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
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
    