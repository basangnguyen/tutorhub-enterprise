// media-device-manager.js
// Handles camera, microphone, and screen share toggling for LiveKit Room

window.isAudioEnabled = false;
window.isVideoEnabled = false;
window.isScreenSharing = false;

window.toggleMic = async function() {
    if (!window.roomState || !window.roomState.get('livekitRoom')) {
        alert("Đang kết nối tới phòng học, vui lòng thử lại sau vài giây...");
        return;
    }
    const room = window.roomState.get('livekitRoom');
    const btn = document.getElementById('toggle-mic-btn') || document.getElementById('mute-all-btn');
    let icon = null;
    if (btn) icon = btn.querySelector('i');
    
    try {
        if (window.isAudioEnabled) {
            // Tắt Mic
            await room.localParticipant.setMicrophoneEnabled(false);
            window.isAudioEnabled = false;
            
            if (btn) btn.classList.remove('active');
            if (icon) {
                icon.className = 'fa-solid fa-microphone-slash';
                icon.style.color = '#ef4444';
            }
        } else {
            // Bật Mic
            await room.localParticipant.setMicrophoneEnabled(true);
            window.isAudioEnabled = true;
            
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
        if (window.isAudioEnabled) {
            window.isAudioEnabled = false;
            if (btn) btn.classList.remove('active');
            if (icon) {
                icon.className = 'fa-solid fa-microphone-slash';
                icon.style.color = '#ef4444';
            }
        }
    }
};

window.startVideoCall = async function() {
    if (!window.roomState || !window.roomState.get('livekitRoom')) {
        alert("Đang kết nối tới phòng học, vui lòng thử lại sau vài giây...");
        return;
    }
    const room = window.roomState.get('livekitRoom');
    const btn = document.getElementById('start-video-btn');
    const icon = btn ? btn.querySelector('i') : null;
    
    try {
        if (window.isVideoEnabled) {
            // Tắt Camera
            if (window._lobbyProcessedTrack) {
                await room.localParticipant.unpublishTrack(window._lobbyProcessedTrack, true);
            } else {
                await room.localParticipant.setCameraEnabled(false);
            }
            window.isVideoEnabled = false;
            
            // Cập nhật UI nút
            if (btn) btn.classList.remove('active');
            if (icon) {
                icon.className = 'fa-solid fa-video-slash';
                icon.style.color = '#ef4444';
            }
        } else {
            // Bật Camera
            if (window._lobbyProcessedTrack) {
                await room.localParticipant.publishTrack(window._lobbyProcessedTrack, { 
                    source: LivekitClient.Track.Source.Camera,
                    simulcast: true 
                });
            } else {
                await room.localParticipant.setCameraEnabled(true);
            }
            window.isVideoEnabled = true;
            
            // Cập nhật UI nút
            if (btn) btn.classList.add('active');
            if (icon) {
                icon.className = 'fa-solid fa-video';
                icon.style.color = '';
            }
        }
    } catch (e) {
        console.error("Lỗi toggle camera:", e);
        alert("Không thể thao tác Camera. Lỗi chi tiết: " + e.message);
        
        // Trả về trạng thái cũ nếu lỗi
        if (window.isVideoEnabled) {
            window.isVideoEnabled = false;
            if (btn) btn.classList.remove('active');
            if (icon) {
                icon.className = 'fa-solid fa-video-slash';
                icon.style.color = '#ef4444';
            }
        }
    }
};

window.toggleScreenShare = async function() {
    if (!window.roomState || !window.roomState.get('livekitRoom')) return;
    const room = window.roomState.get('livekitRoom');
    const btn = document.getElementById('share-screen-btn');
    try {
        if (window.isScreenSharing) {
            await room.localParticipant.setScreenShareEnabled(false);
            window.isScreenSharing = false;
            if (btn) btn.classList.remove('active');
        } else {
            await room.localParticipant.setScreenShareEnabled(true);
            window.isScreenSharing = true;
            if (btn) btn.classList.add('active');
        }
    } catch (e) {
        console.info("SCREEN_SHARE_ERROR: " + e.message + " | Name: " + e.name);
    }
};
