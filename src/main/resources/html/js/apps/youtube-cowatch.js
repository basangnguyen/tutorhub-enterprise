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