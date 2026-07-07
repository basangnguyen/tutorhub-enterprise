// =============================================
// TUTORHUB PRE-JOIN LOBBY MANAGER
// Inspired by Zoom / Google Meet / MS Teams
// =============================================

(function() {
    'use strict';

    // === STATE ===
    let lobbyStream = null;
    let lobbyAudioContext = null;
    let lobbyAnalyser = null;
    let lobbyMicEnabled = true;
    let lobbyCamEnabled = true;
    let selectedBackground = 'none'; // 'none', 'blur', 'bg-classroom', 'bg-nature', etc.
    let audioLevelAnimFrame = null;
    let isJoining = false;

    // Virtual background image list
    const VIRTUAL_BACKGROUNDS = [
        { id: 'none', label: 'Không', icon: '🚫', type: 'none' },
        { id: 'blur', label: 'Làm mờ', icon: '🌫️', type: 'blur' },
        { id: 'bg-classroom', label: 'Lớp học', src: '/img/backgrounds/bg-classroom.png', type: 'image' },
        { id: 'bg-nature', label: 'Thiên nhiên', src: '/img/backgrounds/bg-nature.png', type: 'image' },
        { id: 'bg-library', label: 'Thư viện', src: '/img/backgrounds/bg-library.png', type: 'image' },
        { id: 'bg-space', label: 'Vũ trụ', src: '/img/backgrounds/bg-space.png', type: 'image' },
    ];

    // Export properties to window for access
    window.VIRTUAL_BACKGROUNDS = VIRTUAL_BACKGROUNDS;

    let selfieSegmentation = null;
    let segmentationActive = false;
    let cameraFrameId = null;
    let bgImageCache = {};

    function initSegmentation() {
        if (selfieSegmentation) return;
        try {
            if (typeof window.SelfieSegmentation === 'undefined') {
                console.warn("SelfieSegmentation library not loaded yet.");
                return;
            }
            selfieSegmentation = new window.SelfieSegmentation({locateFile: (file) => {
                return `https://cdn.jsdelivr.net/npm/@mediapipe/selfie_segmentation/${file}`;
            }});
            selfieSegmentation.setOptions({
                modelSelection: 1, // 1 for landscape (faster)
            });
            selfieSegmentation.onResults(onSegmentationResults);
        } catch(e) {
            console.error("MediaPipe failed to load", e);
        }
    }

    async function processVideo() {
        if (!segmentationActive || !lobbyStream || !selfieSegmentation) return;
        const video = document.getElementById('lobby-video');
        if (video && video.readyState >= 2) {
            try {
                await selfieSegmentation.send({image: video});
            } catch (e) {
                console.error("Segmentation error", e);
            }
        }
        if (segmentationActive) {
            cameraFrameId = requestAnimationFrame(processVideo);
        }
    }

    function onSegmentationResults(results) {
        const canvas = document.getElementById('lobby-canvas');
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        
        if (canvas.width !== results.image.width) {
            canvas.width = results.image.width;
            canvas.height = results.image.height;
        }
        
        ctx.save();
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        
        // Draw mask
        ctx.globalCompositeOperation = 'copy';
        ctx.filter = 'blur(4px)'; // soften edges
        ctx.drawImage(results.segmentationMask, 0, 0, canvas.width, canvas.height);
        
        // Draw person
        ctx.globalCompositeOperation = 'source-in';
        ctx.filter = 'none';
        ctx.drawImage(results.image, 0, 0, canvas.width, canvas.height);
        
        // Draw background
        ctx.globalCompositeOperation = 'destination-over';
        
        if (selectedBackground === 'blur') {
            ctx.filter = 'blur(12px)';
            ctx.drawImage(results.image, 0, 0, canvas.width, canvas.height);
            ctx.filter = 'none';
        } else {
            const bg = VIRTUAL_BACKGROUNDS.find(b => b.id === selectedBackground);
            if (bg && bg.src) {
                let img = bgImageCache[bg.src];
                if (!img) {
                    img = new Image();
                    img.src = bg.src;
                    bgImageCache[bg.src] = img;
                }
                if (img.complete) {
                    const hRatio = canvas.width / img.width;
                    const vRatio = canvas.height / img.height;
                    const ratio = Math.max(hRatio, vRatio);
                    const centerShift_x = (canvas.width - img.width * ratio) / 2;
                    const centerShift_y = (canvas.height - img.height * ratio) / 2;  
                    ctx.drawImage(img, 0, 0, img.width, img.height,
                          centerShift_x, centerShift_y, img.width * ratio, img.height * ratio);
                }
            } else {
                ctx.fillStyle = '#000000';
                ctx.fillRect(0, 0, canvas.width, canvas.height);
            }
        }
        ctx.restore();
    }

    // === INIT ===
    window.initLobby = async function() {
        const lobby = document.getElementById('prejoin-lobby');
        if (!lobby) {
            console.warn('[Lobby] No #prejoin-lobby element found, falling back to direct connect');
            if (typeof connectToLiveKit === 'function') connectToLiveKit();
            return;
        }

        // Show lobby
        lobby.style.display = 'flex';

        // Hide the board UI (bottom bar, etc.) while lobby is visible
        const bottomBar = document.getElementById('zoom-bottom-bar');
        if (bottomBar) bottomBar.style.display = 'none';

        // Populate class info from URL params
        const urlParams = new URLSearchParams(window.location.search);
        const role = urlParams.get('role') || 'student';
        const admitted = urlParams.get('admitted') === 'true';
        const boardId = window.roomState ? window.roomState.get('boardId') : (window.currentBoardId || 'Lớp học');

        const classNameEl = document.getElementById('lobby-class-name');
        if (classNameEl) classNameEl.textContent = boardId || 'Phòng học TutorHub';

        const roleBadge = document.getElementById('lobby-role-badge');
        if (roleBadge) {
            if (role === 'teacher') {
                roleBadge.textContent = '👨‍🏫 Giáo viên';
                roleBadge.classList.add('role-teacher');
            } else {
                roleBadge.textContent = '🎓 Học sinh';
                roleBadge.classList.add('role-student');
            }
        }

        // Store join params
        window._lobbyRole = role;
        window._lobbyAdmitted = admitted;
        window._lobbyMicEnabled = false;
        window._lobbyCamEnabled = false;

        // Restore saved preferences
        const savedMic = localStorage.getItem('tutorhub_lobby_mic');
        const savedCam = localStorage.getItem('tutorhub_lobby_cam');
        const savedBg = localStorage.getItem('tutorhub_lobby_bg');

        if (savedMic !== null) lobbyMicEnabled = savedMic === 'true';
        if (savedCam !== null) lobbyCamEnabled = savedCam === 'true';
        if (savedBg) selectedBackground = savedBg;

        // Render background options
        renderBackgroundOptions();

        // Enumerate devices and start preview
        try {
            // Request permissions first to get device labels
            const tempStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true });
            tempStream.getTracks().forEach(t => t.stop());

            await enumerateDevices();
            await startPreview();
        } catch (err) {
            console.warn('[Lobby] Could not access media devices:', err.message);
            // Still show lobby but with disabled camera
            lobbyCamEnabled = false;
            lobbyMicEnabled = false;
            updateControlButtons();
            const noCam = document.getElementById('lobby-no-cam');
            if (noCam) noCam.style.display = 'flex';
        }
    };

    // === ENUMERATE DEVICES ===
    async function enumerateDevices() {
        try {
            const devices = await navigator.mediaDevices.enumerateDevices();

            const micSelect = document.getElementById('lobby-mic-select');
            const camSelect = document.getElementById('lobby-cam-select');

            if (micSelect) {
                micSelect.innerHTML = '';
                const mics = devices.filter(d => d.kind === 'audioinput');
                mics.forEach((d, i) => {
                    const opt = document.createElement('option');
                    opt.value = d.deviceId;
                    opt.textContent = d.label || ('Microphone ' + (i + 1));
                    micSelect.appendChild(opt);
                });
            }

            if (camSelect) {
                camSelect.innerHTML = '';
                const cams = devices.filter(d => d.kind === 'videoinput');
                cams.forEach((d, i) => {
                    const opt = document.createElement('option');
                    opt.value = d.deviceId;
                    opt.textContent = d.label || ('Camera ' + (i + 1));
                    camSelect.appendChild(opt);
                });
            }
        } catch (err) {
            console.error('[Lobby] enumerateDevices failed:', err);
        }
    }

    // === START CAMERA/MIC PREVIEW ===
    async function startPreview() {
        // Stop existing stream
        if (lobbyStream) {
            lobbyStream.getTracks().forEach(t => t.stop());
            lobbyStream = null;
        }
        stopAudioLevelMonitor();

        const micSelect = document.getElementById('lobby-mic-select');
        const camSelect = document.getElementById('lobby-cam-select');

        const constraints = {
            audio: lobbyMicEnabled ? {
                deviceId: micSelect && micSelect.value ? { exact: micSelect.value } : undefined
            } : false,
            video: lobbyCamEnabled ? {
                deviceId: camSelect && camSelect.value ? { exact: camSelect.value } : undefined,
                width: { ideal: 1280 },
                height: { ideal: 720 },
                frameRate: { ideal: 30 }
            } : false
        };

        // At least one must be true
        if (!constraints.audio && !constraints.video) {
            updateVideoPreview(null);
            return;
        }

        try {
            lobbyStream = await navigator.mediaDevices.getUserMedia(constraints);

            if (lobbyCamEnabled) {
                updateVideoPreview(lobbyStream);
            } else {
                updateVideoPreview(null);
            }

            if (lobbyMicEnabled && lobbyStream.getAudioTracks().length > 0) {
                startAudioLevelMonitor(lobbyStream);
            }

            updateControlButtons();
        } catch (err) {
            console.error('[Lobby] startPreview failed:', err);
            if (err.name === 'NotAllowedError') {
                showLobbyToast('⚠️ Vui lòng cấp quyền Camera/Mic trong trình duyệt');
            }
        }
    }

    // === UPDATE VIDEO PREVIEW ===
    function updateVideoPreview(stream) {
        const video = document.getElementById('lobby-video');
        const noCam = document.getElementById('lobby-no-cam');

        if (video && stream && stream.getVideoTracks().length > 0) {
            video.srcObject = stream;
            video.style.display = 'block';
            if (noCam) noCam.style.display = 'none';

            // Apply background effect to preview
            applyBackgroundToPreview();
        } else {
            if (video) {
                video.srcObject = null;
                video.style.display = 'none';
            }
            if (noCam) noCam.style.display = 'flex';
        }
    }

    // === APPLY BACKGROUND TO PREVIEW ===
    function applyBackgroundToPreview() {
        const video = document.getElementById('lobby-video');
        const canvas = document.getElementById('lobby-canvas');
        const bgOverlay = document.getElementById('lobby-bg-overlay');
        
        if (!video || !canvas) return;

        // Reset
        video.style.filter = '';
        if (bgOverlay) {
            bgOverlay.style.display = 'none';
            bgOverlay.style.backgroundImage = '';
        }

        if (selectedBackground === 'none') {
            segmentationActive = false;
            canvas.style.display = 'none';
            video.style.display = 'block';
            video.style.transform = 'scaleX(-1)';
        } else {
            // Start segmentation
            video.style.display = 'none';
            canvas.style.display = 'block';
            
            initSegmentation();
            if (!segmentationActive) {
                segmentationActive = true;
                processVideo();
            }
        }
    }

    // === TOGGLE MIC ===
    window.toggleLobbyMic = function() {
        lobbyMicEnabled = !lobbyMicEnabled;
        localStorage.setItem('tutorhub_lobby_mic', lobbyMicEnabled);

        if (lobbyStream) {
            const audioTracks = lobbyStream.getAudioTracks();
            audioTracks.forEach(t => { t.enabled = lobbyMicEnabled; });
        }

        if (!lobbyMicEnabled) {
            stopAudioLevelMonitor();
        } else if (lobbyStream && lobbyStream.getAudioTracks().length > 0) {
            startAudioLevelMonitor(lobbyStream);
        }

        updateControlButtons();
    };

    // === TOGGLE CAMERA ===
    window.toggleLobbyCamera = async function() {
        lobbyCamEnabled = !lobbyCamEnabled;
        localStorage.setItem('tutorhub_lobby_cam', lobbyCamEnabled);

        // Need to restart stream to add/remove video track
        await startPreview();
    };

    // === BACKGROUND SELECTION ===
    window.toggleBackgroundPanel = function() {
        const panel = document.getElementById('lobby-bg-panel');
        if (panel) {
            const isVisible = panel.style.display !== 'none';
            panel.style.display = isVisible ? 'none' : 'grid';
        }
    };

    window.selectLobbyBackground = function(bgId) {
        selectedBackground = bgId;
        localStorage.setItem('tutorhub_lobby_bg', bgId);

        // Update active state
        document.querySelectorAll('.lobby-bg-option').forEach(el => {
            el.classList.toggle('active', el.dataset.bgId === bgId);
        });

        applyBackgroundToPreview();
    };

    function renderBackgroundOptions() {
        const panel = document.getElementById('lobby-bg-panel');
        if (!panel) return;

        panel.innerHTML = '<div class="lobby-bg-panel-title">Nền ảo & Hiệu ứng</div><div class="lobby-bg-grid">';

        VIRTUAL_BACKGROUNDS.forEach(bg => {
            const isActive = selectedBackground === bg.id;
            let inner = '';

            if (bg.type === 'none') {
                inner = '<div class="lobby-bg-icon">' + bg.icon + '</div>';
            } else if (bg.type === 'blur') {
                inner = '<div class="lobby-bg-icon">' + bg.icon + '</div>';
            } else {
                inner = '<img src="' + bg.src + '" alt="' + bg.label + '" loading="lazy">';
            }

            panel.innerHTML += '<div class="lobby-bg-option' + (isActive ? ' active' : '') + '" data-bg-id="' + bg.id + '" onclick="selectLobbyBackground(\'' + bg.id + '\')">' +
                inner + '<span>' + bg.label + '</span></div>';
        });

        panel.innerHTML += '</div>';
    }

    // === DEVICE CHANGE ===
    window.handleLobbyMicChange = async function(deviceId) {
        await startPreview();
    };

    window.handleLobbyCamChange = async function(deviceId) {
        await startPreview();
    };

    // === AUDIO LEVEL MONITOR ===
    function startAudioLevelMonitor(stream) {
        stopAudioLevelMonitor();
        try {
            lobbyAudioContext = new (window.AudioContext || window.webkitAudioContext)();
            lobbyAnalyser = lobbyAudioContext.createAnalyser();
            lobbyAnalyser.fftSize = 256;
            lobbyAnalyser.smoothingTimeConstant = 0.5;

            const source = lobbyAudioContext.createMediaStreamSource(stream);
            source.connect(lobbyAnalyser);

            updateAudioLevel();
        } catch (err) {
            console.error('[Lobby] Audio level monitor failed:', err);
        }
    }

    function stopAudioLevelMonitor() {
        if (audioLevelAnimFrame) {
            cancelAnimationFrame(audioLevelAnimFrame);
            audioLevelAnimFrame = null;
        }
        if (lobbyAudioContext) {
            lobbyAudioContext.close().catch(() => {});
            lobbyAudioContext = null;
            lobbyAnalyser = null;
        }

        // Reset visual
        const ring = document.getElementById('lobby-audio-ring');
        if (ring) ring.style.boxShadow = '';
        const bar = document.getElementById('lobby-audio-bar');
        if (bar) bar.style.width = '0%';
    }

    function updateAudioLevel() {
        if (!lobbyAnalyser) return;

        const dataArray = new Uint8Array(lobbyAnalyser.frequencyBinCount);
        lobbyAnalyser.getByteFrequencyData(dataArray);

        // Calculate average volume
        let sum = 0;
        for (let i = 0; i < dataArray.length; i++) {
            sum += dataArray[i];
        }
        const avg = sum / dataArray.length;
        const level = Math.min(avg / 80, 1); // Normalize to 0-1

        // Update audio ring glow
        const ring = document.getElementById('lobby-audio-ring');
        if (ring && lobbyMicEnabled) {
            const glow = Math.floor(level * 20);
            ring.style.boxShadow = '0 0 ' + glow + 'px ' + Math.floor(glow / 2) + 'px rgba(16, 185, 129, ' + (level * 0.8) + ')';
        }

        // Update audio bar
        const bar = document.getElementById('lobby-audio-bar');
        if (bar) {
            bar.style.width = (level * 100) + '%';
        }

        audioLevelAnimFrame = requestAnimationFrame(updateAudioLevel);
    }

    // === UPDATE CONTROL BUTTONS ===
    function updateControlButtons() {
        const micBtn = document.getElementById('lobby-mic-btn');
        const camBtn = document.getElementById('lobby-cam-btn');

        if (micBtn) {
            const icon = micBtn.querySelector('i');
            if (lobbyMicEnabled) {
                micBtn.classList.add('active');
                micBtn.classList.remove('off');
                if (icon) icon.className = 'fa-solid fa-microphone';
            } else {
                micBtn.classList.remove('active');
                micBtn.classList.add('off');
                if (icon) icon.className = 'fa-solid fa-microphone-slash';
            }
        }

        if (camBtn) {
            const icon = camBtn.querySelector('i');
            if (lobbyCamEnabled) {
                camBtn.classList.add('active');
                camBtn.classList.remove('off');
                if (icon) icon.className = 'fa-solid fa-video';
            } else {
                camBtn.classList.remove('active');
                camBtn.classList.add('off');
                if (icon) icon.className = 'fa-solid fa-video-slash';
            }
        }
    }

    // === JOIN CLASSROOM ===
    window.joinClassroom = async function() {
        if (isJoining) return;
        isJoining = true;

        const joinBtn = document.getElementById('lobby-join-btn');
        if (joinBtn) {
            joinBtn.disabled = true;
            joinBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang kết nối...';
        }

        // Save state for LiveKit
        window._lobbyMicEnabled = lobbyMicEnabled;
        window._lobbyCamEnabled = lobbyCamEnabled;
        window._lobbyBackground = selectedBackground;

        if (segmentationActive) {
            const canvas = document.getElementById('lobby-canvas');
            if (canvas) {
                window._lobbyProcessedTrack = canvas.captureStream(30).getVideoTracks()[0];
            }
        } else {
            // Stop lobby preview stream since LiveKit will create its own raw stream
            if (lobbyStream) {
                lobbyStream.getTracks().forEach(t => t.stop());
                lobbyStream = null;
            }
        }
        stopAudioLevelMonitor();

        const role = window._lobbyRole;
        const admitted = window._lobbyAdmitted;

        if (role === 'teacher' || admitted) {
            // Teacher or pre-admitted student → join directly
            await performJoin();
        } else {
            // Student with lobby enabled → show waiting state
            showWaitingState();
            // The waiting state listens for admission via the existing lobby mechanism
            // Connect to LiveKit first (to be in the room for teacher to see)
            await performJoin();
        }
    };

    async function performJoin() {
        try {
            // Connect to LiveKit
            if (typeof connectToLiveKit === 'function') {
                await connectToLiveKit();
            }

            const role = window._lobbyRole;
            const admitted = window._lobbyAdmitted;

            if (role === 'teacher' || admitted) {
                // Hide lobby immediately
                hideLobby();
            } else {
                // Show waiting state — lobby stays visible with waiting UI
                showWaitingState();
                // The lobby will be hidden when the teacher admits the student
                // This is handled by the existing lobby-overlay mechanism
            }
        } catch (err) {
            console.error('[Lobby] Join failed:', err);
            isJoining = false;
            const joinBtn = document.getElementById('lobby-join-btn');
            if (joinBtn) {
                joinBtn.disabled = false;
                joinBtn.innerHTML = '<i class="fa-solid fa-arrow-right-to-bracket"></i> Tham gia ngay';
            }
            showLobbyToast('❌ Lỗi kết nối: ' + err.message);
        }
    }

    function showWaitingState() {
        const joinBtn = document.getElementById('lobby-join-btn');
        if (joinBtn) joinBtn.style.display = 'none';

        const waitingEl = document.getElementById('lobby-waiting-state');
        if (waitingEl) waitingEl.style.display = 'flex';

        // Hide controls that aren't needed
        const controls = document.getElementById('lobby-controls');
        const deviceRow = document.getElementById('lobby-device-row');
        if (controls) controls.style.opacity = '0.5';
        if (deviceRow) deviceRow.style.opacity = '0.5';
    }

    function hideLobby() {
        const lobby = document.getElementById('prejoin-lobby');
        if (lobby) {
            lobby.style.opacity = '0';
            lobby.style.transition = 'opacity 0.4s ease';
            setTimeout(() => {
                lobby.style.display = 'none';
                lobby.remove(); // Clean up DOM
            }, 400);
        }

        // Show the board UI
        const bottomBar = document.getElementById('zoom-bottom-bar');
        if (bottomBar) bottomBar.style.display = 'flex';

        // Update mic/camera button states on the main toolbar to match lobby selection
        updateMainToolbarFromLobby();
    }

    // Expose hideLobby for external use (e.g., when teacher admits student)
    window.hideLobbyScreen = hideLobby;

    function updateMainToolbarFromLobby() {
        // Sync mic button
        const mainMicBtn = document.getElementById('toggle-mic-btn');
        if (mainMicBtn) {
            const icon = mainMicBtn.querySelector('i');
            if (window._lobbyMicEnabled) {
                mainMicBtn.classList.add('active');
                if (icon) { icon.className = 'fa-solid fa-microphone'; icon.style.color = ''; }
            } else {
                mainMicBtn.classList.remove('active');
                if (icon) { icon.className = 'fa-solid fa-microphone-slash'; icon.style.color = '#ef4444'; }
            }
        }

        // Sync camera button
        const mainCamBtn = document.getElementById('start-video-btn');
        if (mainCamBtn) {
            const icon = mainCamBtn.querySelector('i');
            if (window._lobbyCamEnabled) {
                mainCamBtn.classList.add('active');
                if (icon) { icon.className = 'fa-solid fa-video'; icon.style.color = ''; }
            } else {
                mainCamBtn.classList.remove('active');
                if (icon) { icon.className = 'fa-solid fa-video-slash'; icon.style.color = '#ef4444'; }
            }
        }
    }

    // === TOAST ===
    function showLobbyToast(msg) {
        const container = document.getElementById('toast-container');
        if (!container) { alert(msg); return; }

        const toast = document.createElement('div');
        toast.style.cssText = 'background: #1e293b; color: white; padding: 12px 20px; border-radius: 8px; font-size: 14px; font-family: sans-serif; box-shadow: 0 4px 12px rgba(0,0,0,0.3); pointer-events: auto; animation: fadeIn 0.3s ease;';
        toast.textContent = msg;
        container.appendChild(toast);
        setTimeout(() => { toast.style.opacity = '0'; setTimeout(() => toast.remove(), 300); }, 4000);
    }

    // === SPEAKER TEST ===
    window.testLobbySpeaker = function() {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.frequency.value = 440;
            gain.gain.value = 0.3;
            osc.start();
            setTimeout(() => {
                osc.stop();
                ctx.close();
            }, 500);
            showLobbyToast('🔊 Âm thanh hoạt động tốt!');
        } catch (e) {
            showLobbyToast('❌ Không thể phát âm thanh: ' + e.message);
        }
    };

})();
