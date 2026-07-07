// room-roster-manager.js
// Handles participant roster interactions and host controls (Raise Hand, Mute All)

window.toggleRaiseHand = async function() {
    if (!window.currentRoom) return;
    const btn = document.getElementById('raise-hand-btn');

    try {
        const localIdentity = window.currentRoom.localParticipant.identity;
        const currentMetaStr = window.currentRoom.localParticipant.metadata;
        let currentMeta = null;
        try { if (currentMetaStr) currentMeta = JSON.parse(currentMetaStr); } catch (e) {}
        
        if (!currentMeta && window.rosterMetadataCache) {
            currentMeta = window.rosterMetadataCache[localIdentity];
        }

        if (!currentMeta) {
            currentMeta = { role: window.roomState ? window.roomState.get('userRole') : 'student', displayName: localIdentity, isHandRaised: false, isAdmitted: true };
        }

        // Toggle state
        currentMeta.isHandRaised = !currentMeta.isHandRaised;
        currentMeta.handRaisedAt = currentMeta.isHandRaised ? Date.now() : null;

        await window.currentRoom.localParticipant.setMetadata(JSON.stringify(currentMeta));

        if (currentMeta.isHandRaised) {
            if (typeof showToast === 'function') showToast("Bạn đã giơ tay phát biểu ✋", 3000);
            if (btn) btn.classList.add('active');
        } else {
            if (typeof showToast === 'function') showToast("Bạn đã hạ tay xuống.", 3000);
            if (btn) btn.classList.remove('active');
        }

        // Fallback broadcast if server drops metadata
        const senderName = window.currentUserName || localIdentity;
        const metaPayload = JSON.stringify({ type: 'roster_sync_metadata', sender: senderName, metadata: JSON.stringify(currentMeta) });
        window.currentRoom.localParticipant.publishData(new TextEncoder().encode(metaPayload), { reliable: true });

        if (typeof renderRoster === 'function') renderRoster();

    } catch (e) {
        console.info("RAISE_HAND_ERROR: " + e.message);
    }
};

window.muteAllStudents = async function() {
    if (!window.currentRoom) return;
    try {
        // Send a mute_all signal to all users
        const payload = JSON.stringify({ type: 'mute_all', sender: window.currentRoom.localParticipant.identity });
        const encoder = new TextEncoder();
        await window.currentRoom.localParticipant.publishData(encoder.encode(payload), { reliable: true });

        if (typeof showToast === 'function') showToast("Đã gửi lệnh tắt Mic toàn lớp!", 3000);
    } catch (e) {
        console.error("MUTE_ALL_ERROR: ", e);
    }
};
