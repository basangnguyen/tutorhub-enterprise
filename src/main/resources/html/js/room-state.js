class RoomState extends EventTarget {
    #state = {
        isConnected: false,
        roomId: null,
        boardId: null,
        userName: '',
        userRole: 'student', // 'teacher' | 'student'
        isMuted: true,
        cameraEnabled: false,
        isScreenSharing: false,
        isRecording: false,
        livekitRoom: null, // Thay thế window.currentRoom
        participants: new Map() // identity -> participant info
    };

    get(key) { return this.#state[key]; }

    set(key, value) {
        if (this.#state[key] !== value) {
            const oldValue = this.#state[key];
            this.#state[key] = value;
            this.dispatchEvent(new CustomEvent('change', { detail: { key, oldValue, newValue: value } }));
            // Lắng nghe theo key cụ thể: change:isMuted
            this.dispatchEvent(new CustomEvent(`change:${key}`, { detail: { oldValue, newValue: value } }));
        }
    }
}

window.roomState = new RoomState();
