/**
 * Keyboard Shortcuts cho TutorHub Classroom
 * Alt+M - Toggle Mute All (GV) / Toggle Mic (HS)
 * Alt+V - Toggle Camera
 * Alt+S - Toggle Screen Share
 * Alt+H - Toggle Raise Hand
 * Alt+C - Toggle Chatbox
 * Alt+P - Toggle People Sidebar
 * Alt+A - Toggle Apps Menu
 * Escape - Đóng tất cả popup/modal
 */
document.addEventListener('keydown', function(event) {
    // Không kích hoạt phím tắt nếu đang focus vào input hoặc textarea
    if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA' || event.target.isContentEditable) {
        if (event.key === 'Escape') {
            event.target.blur(); // Blur input if Escape is pressed
        } else {
            return;
        }
    }

    if (event.altKey) {
        switch (event.code) {
            case 'KeyM':
                event.preventDefault();
                if (window.roomState.get('userRole') === 'teacher') {
                    if (typeof handleMuteAll === 'function') {
                        handleMuteAll();
                    } else if (typeof muteAllStudents === 'function') {
                        muteAllStudents();
                    }
                } else {
                    // Toggle my own mic
                    if (window.currentRoom && window.currentRoom.localParticipant) {
                        const isMuted = !window.currentRoom.localParticipant.isMicrophoneEnabled;
                        window.currentRoom.localParticipant.setMicrophoneEnabled(isMuted);
                    }
                }
                break;
            case 'KeyV':
                event.preventDefault();
                const startVideoBtn = document.getElementById('start-video-btn');
                if (startVideoBtn) startVideoBtn.click();
                break;
            case 'KeyS':
                event.preventDefault();
                if (typeof toggleScreenShare === 'function') toggleScreenShare();
                break;
            case 'KeyH':
                event.preventDefault();
                if (typeof toggleRaiseHand === 'function') toggleRaiseHand();
                break;
            case 'KeyC':
                event.preventDefault();
                if (typeof toggleChatbox === 'function') toggleChatbox();
                break;
            case 'KeyP':
                event.preventDefault();
                if (typeof togglePeopleSidebar === 'function') togglePeopleSidebar();
                break;
            case 'KeyA':
                event.preventDefault();
                if (typeof toggleAppsMenu === 'function') {
                    // Create a dummy event for event.stopPropagation()
                    toggleAppsMenu({ stopPropagation: () => {} });
                }
                break;
        }
    } else if (event.key === 'Escape') {
        // Đóng các popup, modal
        if (typeof closeAppsMenu === 'function') closeAppsMenu();
        
        const chatbox = document.getElementById('chatbox');
        if (chatbox && chatbox.style.display !== 'none') chatbox.style.display = 'none';

        const peopleSidebar = document.getElementById('people-sidebar');
        // Toggle if it's currently open
        if (peopleSidebar && peopleSidebar.style.display !== 'none' && peopleSidebar.style.transform !== 'translateX(100%)') {
            if (typeof togglePeopleSidebar === 'function') togglePeopleSidebar();
        }

        const modals = ['board-settings-modal', 'phet-modal', 'code-modal', 'judge-panel', 'youtube-cowatch-modal', 'math-modal', 'mermaid-modal'];
        modals.forEach(id => {
            const el = document.getElementById(id);
            if (el && el.style.display !== 'none') el.style.display = 'none';
        });
    }
});
