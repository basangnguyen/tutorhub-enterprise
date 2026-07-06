/**
 * BreakoutManager — Quản lý phòng nhóm nhỏ (Breakout Rooms)
 * 
 * Kiến trúc:
 * - GV tạo breakout → gửi BREAKOUT_START qua LiveKit DataChannel
 * - HS nhận lệnh → switch Yjs room + filter LiveKit tracks
 * - GV có thể "đi tuần" vào từng phòng nhóm
 * - Khi hết giờ hoặc GV kết thúc → BREAKOUT_END → reconnect main room
 */
class BreakoutManager {
    constructor() {
        this.isActive = false;
        this.isTeacher = false;
        this.mainBoardId = null;       // boardId của phòng chính (lưu lại để reconnect)
        this.currentGroupId = null;    // groupId mà user đang ở
        this.groups = new Map();       // groupId → { name, members[], boardId }
        this.timerInterval = null;
        this.endTimestamp = null;

        // Bind to room state
        if (window.roomState) {
            this.isTeacher = window.roomState.get('userRole') === 'teacher';
            window.roomState.addEventListener('change:userRole', (e) => {
                this.isTeacher = e.detail.newValue === 'teacher';
            });
        }
    }

    // ==============================
    // TEACHER API: Tạo & quản lý breakout
    // ==============================

    /**
     * Tạo breakout rooms
     * @param {Object} config - { numGroups: number, duration: number (phút), assignment: 'auto'|'manual', manualGroups?: Map }
     */
    createBreakout(config) {
        if (!this.isTeacher) return;

        const room = window.roomState.get('livekitRoom');
        if (!room) {
            if (typeof showToast === 'function') showToast('Chưa kết nối LiveKit!', 3000);
            return;
        }

        // Lưu board chính
        this.mainBoardId = window.roomState.get('boardId') || window.currentBoardId;

        // Lấy danh sách participants (trừ GV)
        const participants = [];
        room.remoteParticipants.forEach((p) => {
            participants.push(p.identity);
        });

        if (participants.length < 2) {
            if (typeof showToast === 'function') showToast('Cần ít nhất 2 học sinh để chia nhóm!', 3000);
            return;
        }

        // Tạo groups
        this.groups.clear();
        const numGroups = Math.min(config.numGroups || 2, Math.ceil(participants.length / 2));

        if (config.assignment === 'manual' && config.manualGroups) {
            // Manual assignment từ modal
            config.manualGroups.forEach((groupData, groupId) => {
                this.groups.set(groupId, {
                    name: `Nhóm ${groupId}`,
                    members: groupData.members,
                    leader: groupData.leader,
                    boardId: `breakout_${this.mainBoardId}_${groupId}`
                });
            });
        } else {
            // Auto assignment: chia đều, shuffle trước
            const shuffled = [...participants].sort(() => Math.random() - 0.5);
            for (let i = 0; i < numGroups; i++) {
                const groupId = String.fromCharCode(65 + i); // A, B, C, ...
                this.groups.set(groupId, {
                    name: `Nhóm ${groupId}`,
                    members: [],
                    leader: null,
                    boardId: `breakout_${this.mainBoardId}_${groupId}`
                });
            }
            shuffled.forEach((identity, idx) => {
                const groupId = String.fromCharCode(65 + (idx % numGroups));
                this.groups.get(groupId).members.push(identity);
            });
            // Update auto leaders
            this.groups.forEach(group => {
                if (group.members.length > 0) {
                    group.leader = group.members[0];
                }
            });
        }

        // Tính endTimestamp
        const durationMs = (config.duration || 10) * 60 * 1000;
        this.endTimestamp = Date.now() + durationMs;

        // Gửi lệnh BREAKOUT_START cho tất cả
        const payload = {
            type: 'breakout_start',
            groups: Object.fromEntries(this.groups),
            endTimestamp: this.endTimestamp,
            mainBoardId: this.mainBoardId
        };

        const encoder = new TextEncoder();
        room.localParticipant.publishData(
            encoder.encode(JSON.stringify(payload)),
            { reliable: true }
        );

        // GV tự mình cũng vào trạng thái breakout (nhưng ở overview, không ở nhóm nào)
        this.isActive = true;
        this._startTimer();
        this._renderTeacherOverview();

        if (typeof showToast === 'function') {
            showToast(`Đã tạo ${this.groups.size} phòng nhóm — ${config.duration} phút`, 3000);
        }

        // Đóng modal
        closeBreakoutModal();
    }

    /**
     * GV "đi tuần" vào một phòng nhóm
     */
    visitGroup(groupId) {
        if (!this.isTeacher || !this.isActive) return;
        const group = this.groups.get(groupId);
        if (!group) return;

        this.currentGroupId = groupId;

        // Switch Yjs room sang board của nhóm
        this._switchYjsRoom(group.boardId);

        // Filter LiveKit tracks: chỉ subscribe nhóm này
        this._filterTracksForGroup(groupId);

        // Update UI
        this._updateVisitUI(groupId);

        if (typeof showToast === 'function') {
            showToast(`Đang xem ${group.name}`, 2000);
        }
    }

    /**
     * GV quay về overview (xem tất cả nhóm)
     */
    returnToOverview() {
        if (!this.isTeacher || !this.isActive) return;

        this.currentGroupId = null;

        // Switch Yjs về board chính
        this._switchYjsRoom(this.mainBoardId);

        // Resubscribe tất cả tracks
        this._resubscribeAllTracks();

        this._renderTeacherOverview();

        if (typeof showToast === 'function') {
            showToast('Đã quay về tổng quan', 2000);
        }
    }

    /**
     * GV kết thúc tất cả breakout rooms
     */
    endAllBreakouts() {
        if (!this.isTeacher) return;

        const room = window.roomState.get('livekitRoom');
        if (!room) return;

        const payload = {
            type: 'breakout_end',
            mainBoardId: this.mainBoardId
        };

        const encoder = new TextEncoder();
        room.localParticipant.publishData(
            encoder.encode(JSON.stringify(payload)),
            { reliable: true }
        );

        // GV tự reconnect
        this._handleBreakoutEnd();
    }

    // ==============================
    // CLIENT API: Xử lý lệnh từ DataChannel
    // ==============================

    /**
     * Xử lý khi nhận lệnh breakout qua LiveKit DataChannel
     * Được gọi từ livekit-manager.js → room.on('DataReceived', ...)
     */
    handleBreakoutCommand(data) {
        if (data.type === 'breakout_start') {
            this._handleBreakoutStart(data);
        } else if (data.type === 'breakout_end') {
            this._handleBreakoutEnd();
        } else if (data.type === 'breakout_broadcast') {
            // GV gửi tin nhắn broadcast tới tất cả nhóm
            if (typeof showToast === 'function') {
                showToast(`📢 GV: ${data.message}`, 5000);
            }
        }
    }

    // ==============================
    // PRIVATE: Logic nội bộ
    // ==============================

    _handleBreakoutStart(data) {
        this.isActive = true;
        this.mainBoardId = data.mainBoardId;
        this.endTimestamp = data.endTimestamp;

        // Rebuild groups map
        this.groups.clear();
        for (const [groupId, groupData] of Object.entries(data.groups)) {
            this.groups.set(groupId, groupData);
        }

        // Tìm nhóm của mình
        const myIdentity = this._getMyIdentity();
        let myGroupId = null;

        this.groups.forEach((group, groupId) => {
            if (group.members.includes(myIdentity)) {
                myGroupId = groupId;
            }
        });

        if (myGroupId && !this.isTeacher) {
            this.currentGroupId = myGroupId;
            const group = this.groups.get(myGroupId);

            // Switch Yjs room
            this._switchYjsRoom(group.boardId);

            // Filter LiveKit tracks
            this._filterTracksForGroup(myGroupId);

            // Hiện overlay timer
            this._startTimer();
            this._showStudentBreakoutUI(myGroupId, group.name);

            if (typeof showToast === 'function') {
                showToast(`Bạn đã được chia vào ${group.name}!`, 3000);
            }
        } else if (this.isTeacher) {
            // GV: hiện overview
            this._startTimer();
            this._renderTeacherOverview();
        }
    }

    _handleBreakoutEnd() {
        if (!this.isActive) return;

        this.isActive = false;
        this.currentGroupId = null;

        // Stop timer
        if (this.timerInterval) {
            clearInterval(this.timerInterval);
            this.timerInterval = null;
        }

        // Switch Yjs về board chính
        if (this.mainBoardId) {
            this._switchYjsRoom(this.mainBoardId);
        }

        // Resubscribe tất cả tracks
        this._resubscribeAllTracks();

        // Xóa UI breakout
        this._removeBreakoutUI();

        this.groups.clear();

        if (typeof showToast === 'function') {
            showToast('Breakout kết thúc — Đã quay về phòng chính', 3000);
        }
    }

    _switchYjsRoom(boardId) {
        // Gọi whiteboard-sync.js để switch Yjs room
        if (typeof window.loadBoardData === 'function') {
            // Disconnect Yjs room cũ
            if (window.syncRoom) {
                window.syncRoom.destroy();
                window.syncRoom = null;
            }
            // Load board mới (Yjs sẽ tự connect)
            window.loadBoardData(null, boardId);
        }
    }

    _filterTracksForGroup(groupId) {
        const room = window.roomState.get('livekitRoom');
        if (!room) return;

        const group = this.groups.get(groupId);
        if (!group) return;

        const membersInGroup = new Set(group.members);
        // Nếu GV đang visit, cũng cho GV vào set
        if (this.isTeacher) {
            membersInGroup.add(this._getMyIdentity());
        }

        room.remoteParticipants.forEach((participant) => {
            const isInGroup = membersInGroup.has(participant.identity);
            participant.trackPublications.forEach((pub) => {
                if (pub.track) {
                    pub.setSubscribed(isInGroup);
                }
            });
        });
    }

    _resubscribeAllTracks() {
        const room = window.roomState.get('livekitRoom');
        if (!room) return;

        room.remoteParticipants.forEach((participant) => {
            participant.trackPublications.forEach((pub) => {
                pub.setSubscribed(true);
            });
        });
    }

    _getMyIdentity() {
        const room = window.roomState.get('livekitRoom');
        if (room && room.localParticipant) {
            return room.localParticipant.identity;
        }
        return window.roomState.get('userName') || '';
    }

    // ==============================
    // TIMER
    // ==============================

    _startTimer() {
        if (this.timerInterval) clearInterval(this.timerInterval);

        this.timerInterval = setInterval(() => {
            if (!this.endTimestamp) return;

            const remaining = this.endTimestamp - Date.now();
            if (remaining <= 0) {
                // Hết giờ
                if (this.isTeacher) {
                    this.endAllBreakouts();
                } else {
                    this._handleBreakoutEnd();
                }
                return;
            }

            // Update timer display
            const mins = Math.floor(remaining / 60000);
            const secs = Math.floor((remaining % 60000) / 1000);
            const timerEl = document.getElementById('breakout-timer-display');
            if (timerEl) {
                timerEl.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
            }

            // Cảnh báo 1 phút cuối
            if (remaining <= 60000 && remaining > 59000) {
                if (typeof showToast === 'function') {
                    showToast('⏰ Còn 1 phút! Breakout sắp kết thúc.', 3000);
                }
            }
        }, 1000);
    }

    // ==============================
    // UI RENDERING
    // ==============================

    _showStudentBreakoutUI(groupId, groupName) {
        // Xóa overlay cũ nếu có
        this._removeBreakoutUI();

        const group = this.groups.get(groupId);
        const myIdentity = this._getMyIdentity();
        const isLeader = group && group.leader === myIdentity;

        const overlay = document.createElement('div');
        overlay.id = 'breakout-overlay';
        overlay.style.cssText = `
            position: fixed; top: 10px; left: 50%; transform: translateX(-50%);
            background: rgba(59, 130, 246, 0.9); backdrop-filter: blur(10px);
            color: white; padding: 8px 20px; border-radius: 20px;
            font-family: sans-serif; font-size: 14px; font-weight: bold;
            z-index: 10002; display: flex; align-items: center; gap: 10px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.3);
        `;
        overlay.innerHTML = `
            <i class="fa-solid fa-users" style="font-size: 16px;"></i>
            <span>${groupName} ${isLeader ? '<i class="fa-solid fa-crown" style="color: gold; margin-left: 4px;" title="Nhóm trưởng"></i>' : ''}</span>
            <span style="opacity: 0.8;">—</span>
            <span id="breakout-timer-display" style="font-variant-numeric: tabular-nums;">--:--</span>
        `;
        document.body.appendChild(overlay);
    }

    _renderTeacherOverview() {
        // Xóa cũ
        this._removeBreakoutUI();

        // Sidebar cho GV
        const sidebar = document.createElement('div');
        sidebar.id = 'breakout-teacher-sidebar';
        sidebar.style.cssText = `
            position: fixed; top: 10px; right: 10px; width: 260px;
            background: rgba(15, 23, 42, 0.95); backdrop-filter: blur(10px);
            border-radius: 12px; z-index: 10002; color: white;
            font-family: sans-serif; box-shadow: 0 8px 30px rgba(0,0,0,0.5);
            max-height: 80vh; overflow-y: auto;
        `;

        let html = `
            <div style="padding: 12px 15px; border-bottom: 1px solid #334155; display: flex; justify-content: space-between; align-items: center;">
                <div style="font-weight: bold; font-size: 14px;">
                    <i class="fa-solid fa-object-group"></i> Breakout Rooms
                </div>
                <span id="breakout-timer-display" style="background: #ef4444; padding: 2px 8px; border-radius: 10px; font-size: 12px; font-variant-numeric: tabular-nums;">--:--</span>
            </div>
        `;

        this.groups.forEach((group, groupId) => {
            const isVisiting = this.currentGroupId === groupId;
            html += `
                <div style="padding: 10px 15px; border-bottom: 1px solid #1e293b; ${isVisiting ? 'background: rgba(59, 130, 246, 0.2);' : ''}">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                        <span style="font-weight: bold; font-size: 13px;">${group.name}</span>
                        <button onclick="window.breakoutManager.visitGroup('${groupId}')" 
                                style="background: ${isVisiting ? '#3b82f6' : '#475569'}; color: white; border: none; padding: 3px 10px; border-radius: 6px; cursor: pointer; font-size: 11px;">
                            ${isVisiting ? 'Đang xem' : 'Vào xem'}
                        </button>
                    </div>
                    <div style="font-size: 11px; color: #94a3b8;">
                        ${group.members.map(m => `<span style="margin-right: 6px;">${group.leader === m ? '<i class="fa-solid fa-crown" style="color: gold;" title="Nhóm trưởng"></i> ' : '👤 '}${m}</span>`).join('')}
                    </div>
                </div>
            `;
        });

        html += `
            <div style="padding: 10px 15px; display: flex; gap: 8px;">
                ${this.currentGroupId ? `
                    <button onclick="window.breakoutManager.returnToOverview()" 
                            style="flex: 1; background: #475569; color: white; border: none; padding: 8px; border-radius: 6px; cursor: pointer; font-size: 12px; font-weight: bold;">
                        ← Tổng quan
                    </button>
                ` : ''}
                <button onclick="window.breakoutManager.endAllBreakouts()" 
                        style="flex: 1; background: #ef4444; color: white; border: none; padding: 8px; border-radius: 6px; cursor: pointer; font-size: 12px; font-weight: bold;">
                    Kết thúc tất cả
                </button>
            </div>
        `;

        sidebar.innerHTML = html;
        document.body.appendChild(sidebar);
    }

    _removeBreakoutUI() {
        const overlay = document.getElementById('breakout-overlay');
        if (overlay) overlay.remove();
        const sidebar = document.getElementById('breakout-teacher-sidebar');
        if (sidebar) sidebar.remove();
        const modal = document.getElementById('breakout-modal');
        if (modal) modal.style.display = 'none';
    }
}

// ==============================
// GLOBAL INSTANCE + CLASSIN-STYLE MODAL
// ==============================

window.breakoutManager = new BreakoutManager();

// State cho modal chia nhóm
let _modalGroups = new Map();  // groupId → { name, members: [], leader: string|null }
let _unassigned = [];          // Danh sách HS chưa được chia
let _draggedStudent = null;    // Student đang được kéo

function _updateGroupLeaders() {
    _modalGroups.forEach((group) => {
        if (group.members.length === 0) {
            group.leader = null;
        } else if (!group.leader || !group.members.includes(group.leader)) {
            // Default: first member is leader
            group.leader = group.members[0];
        }
    });
}

/**
 * Mở modal tạo Breakout (GV only) — Kiểu ClassIn
 */
window.openBreakoutModal = function() {
    if (window.roomState && window.roomState.get('userRole') !== 'teacher') {
        if (typeof showToast === 'function') showToast('Chỉ giáo viên mới có thể tạo phòng nhóm!', 3000);
        return;
    }

    // Reset state
    _modalGroups.clear();
    _unassigned = [];

    // Lấy danh sách HS từ LiveKit
    const room = window.roomState.get('livekitRoom');
    if (room) {
        room.remoteParticipants.forEach((p) => {
            _unassigned.push(p.identity);
        });
    }

    if (_unassigned.length < 2) {
        if (typeof showToast === 'function') showToast('Cần ít nhất 2 học sinh để chia nhóm!', 3000);
        return;
    }

    // Tạo 2 nhóm mặc định (trống)
    _modalGroups.set('A', { name: 'Nhóm A', members: [], leader: null });
    _modalGroups.set('B', { name: 'Nhóm B', members: [], leader: null });

    _renderBreakoutModal();

    const modal = document.getElementById('breakout-modal');
    if (modal) modal.style.display = 'flex';
};

window.closeBreakoutModal = function() {
    const modal = document.getElementById('breakout-modal');
    if (modal) modal.style.display = 'none';
};

/**
 * Thêm nhóm mới
 */
window.addBreakoutGroup = function() {
    const nextId = String.fromCharCode(65 + _modalGroups.size); // C, D, E...
    if (_modalGroups.size >= 8) {
        if (typeof showToast === 'function') showToast('Tối đa 8 nhóm!', 2000);
        return;
    }
    _modalGroups.set(nextId, { name: `Nhóm ${nextId}`, members: [], leader: null });
    _renderBreakoutModal();
};

/**
 * Xóa nhóm — trả HS về danh sách chưa chia
 */
window.removeBreakoutGroup = function(groupId) {
    const group = _modalGroups.get(groupId);
    if (group) {
        _unassigned.push(...group.members);
        _modalGroups.delete(groupId);
        _renderBreakoutModal();
    }
};

/**
 * Tự động chia đều (shuffle random) — Shortcut giống ClassIn "Auto Assign"
 */
window.autoAssignBreakout = function() {
    // Thu tất cả HS về _unassigned
    _modalGroups.forEach((group) => {
        _unassigned.push(...group.members);
        group.members = [];
    });

    // Shuffle
    const shuffled = [..._unassigned].sort(() => Math.random() - 0.5);
    _unassigned = [];

    const groupIds = [..._modalGroups.keys()];
    shuffled.forEach((student, idx) => {
        const gid = groupIds[idx % groupIds.length];
        _modalGroups.get(gid).members.push(student);
    });

    _updateGroupLeaders();
    _renderBreakoutModal();
};

/**
 * GV xác nhận tạo breakout
 */
window.confirmCreateBreakout = function() {
    // Kiểm tra tất cả HS đã được chia chưa
    if (_unassigned.length > 0) {
        if (typeof showToast === 'function') showToast(`Còn ${_unassigned.length} học sinh chưa được chia nhóm!`, 3000);
        return;
    }

    // Kiểm tra mỗi nhóm có ít nhất 1 người
    let emptyGroup = false;
    _modalGroups.forEach((group, gid) => {
        if (group.members.length === 0) emptyGroup = true;
    });
    if (emptyGroup) {
        if (typeof showToast === 'function') showToast('Mỗi nhóm cần ít nhất 1 học sinh!', 3000);
        return;
    }

    const duration = parseInt(document.getElementById('breakout-duration').value) || 10;

    // Chuyển _modalGroups sang manualGroups Map
    const manualGroups = new Map();
    _modalGroups.forEach((group, gid) => {
        manualGroups.set(gid, { members: group.members, leader: group.leader });
    });

    window.breakoutManager.createBreakout({
        numGroups: _modalGroups.size,
        duration: duration,
        assignment: 'manual',
        manualGroups: manualGroups
    });
};

// ==============================
// DRAG & DROP — ClassIn style
// ==============================

window._breakoutDragStart = function(e, studentName) {
    _draggedStudent = studentName;
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', studentName);
    setTimeout(() => {
        if (e.target) e.target.style.opacity = '0.4';
    }, 0);
};

window._breakoutDragEnd = function(e) {
    e.target.style.opacity = '1';
    _draggedStudent = null;
};

window._breakoutDragOver = function(e) {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    e.currentTarget.style.background = 'rgba(59, 130, 246, 0.15)';
    e.currentTarget.style.borderColor = '#3b82f6';
};

window._breakoutDragLeave = function(e) {
    e.currentTarget.style.background = '';
    e.currentTarget.style.borderColor = '';
};

/**
 * Drop vào nhóm cụ thể
 */
window._breakoutDropToGroup = function(e, targetGroupId) {
    e.preventDefault();
    e.currentTarget.style.background = '';
    e.currentTarget.style.borderColor = '';

    if (!_draggedStudent) return;

    // Xóa student khỏi vị trí cũ
    _unassigned = _unassigned.filter(s => s !== _draggedStudent);
    _modalGroups.forEach((group) => {
        group.members = group.members.filter(s => s !== _draggedStudent);
    });

    // Thêm vào nhóm mới
    if (_modalGroups.has(targetGroupId)) {
        _modalGroups.get(targetGroupId).members.push(_draggedStudent);
    }

    _draggedStudent = null;
    _updateGroupLeaders();
    _renderBreakoutModal();
};

/**
 * Drop về "Chưa chia"
 */
window._breakoutDropToUnassigned = function(e) {
    e.preventDefault();
    e.currentTarget.style.background = '';
    e.currentTarget.style.borderColor = '';

    if (!_draggedStudent) return;

    // Xóa khỏi nhóm cũ
    _modalGroups.forEach((group) => {
        group.members = group.members.filter(s => s !== _draggedStudent);
    });

    // Thêm về unassigned (nếu chưa có)
    if (!_unassigned.includes(_draggedStudent)) {
        _unassigned.push(_draggedStudent);
    }

    _draggedStudent = null;
    _updateGroupLeaders();
    _renderBreakoutModal();
};

/**
 * GV nhấp đúp vào HS trong nhóm để đổi nhóm trưởng
 */
window.setBreakoutGroupLeader = function(groupId, studentName) {
    const group = _modalGroups.get(groupId);
    if (group && group.members.includes(studentName)) {
        group.leader = studentName;
        _renderBreakoutModal();
    }
};

// ==============================
// RENDER MODAL — ClassIn Layout
// ==============================

function _renderBreakoutModal() {
    const modal = document.getElementById('breakout-modal');
    if (!modal) return;

    const totalStudents = _unassigned.length + [..._modalGroups.values()].reduce((sum, g) => sum + g.members.length, 0);

    let html = `
    <div style="width: 680px; max-width: 95vw; max-height: 90vh; background: #1e293b; border-radius: 16px; color: white; font-family: sans-serif; box-shadow: 0 20px 60px rgba(0,0,0,0.6); display: flex; flex-direction: column; overflow: hidden;">
        <!-- Header -->
        <div style="padding: 16px 20px; border-bottom: 1px solid #334155; display: flex; justify-content: space-between; align-items: center;">
            <div>
                <h3 style="margin: 0; font-size: 18px;"><i class="fa-solid fa-object-group" style="color: #3b82f6;"></i> Chia phòng nhóm</h3>
                <div style="font-size: 12px; color: #94a3b8; margin-top: 4px;">${totalStudents} học sinh — ${_modalGroups.size} nhóm</div>
            </div>
            <button onclick="closeBreakoutModal()" style="background: none; border: none; color: #94a3b8; cursor: pointer; font-size: 20px;">✖</button>
        </div>

        <!-- Body: 2 columns -->
        <div style="display: flex; flex: 1; overflow: hidden;">

            <!-- LEFT: Chưa chia nhóm -->
            <div style="width: 200px; border-right: 1px solid #334155; display: flex; flex-direction: column;">
                <div style="padding: 10px 14px; font-size: 12px; color: #94a3b8; font-weight: bold; border-bottom: 1px solid #1e293b;">
                    CHƯA CHIA (${_unassigned.length})
                </div>
                <div id="unassigned-drop-zone" 
                     style="flex: 1; padding: 8px; overflow-y: auto; min-height: 100px; transition: background 0.2s, border-color 0.2s; border: 2px dashed transparent;"
                     ondragover="_breakoutDragOver(event)"
                     ondragleave="_breakoutDragLeave(event)"
                     ondrop="_breakoutDropToUnassigned(event)">`;

    _unassigned.forEach(student => {
        html += _renderStudentChip(student, null, false);
    });

    if (_unassigned.length === 0) {
        html += `<div style="color: #475569; font-size: 12px; text-align: center; padding: 20px 0;">Tất cả đã được chia ✓</div>`;
    }

    html += `
                </div>
            </div>

            <!-- RIGHT: Các nhóm -->
            <div style="flex: 1; overflow-y: auto; padding: 12px;">
                <div style="display: flex; flex-wrap: wrap; gap: 10px;">`;

    _modalGroups.forEach((group, groupId) => {
        html += `
                    <div style="flex: 1; min-width: 180px; background: #0f172a; border-radius: 10px; border: 2px dashed #334155; transition: background 0.2s, border-color 0.2s; display: flex; flex-direction: column;"
                         ondragover="_breakoutDragOver(event)"
                         ondragleave="_breakoutDragLeave(event)"
                         ondrop="_breakoutDropToGroup(event, '${groupId}')">
                        <div style="padding: 8px 12px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #1e293b;">
                            <span style="font-weight: bold; font-size: 13px; color: #60a5fa;">${group.name} (${group.members.length})</span>
                            ${_modalGroups.size > 2 ? `<button onclick="removeBreakoutGroup('${groupId}')" style="background: none; border: none; color: #ef4444; cursor: pointer; font-size: 12px;" title="Xóa nhóm">✕</button>` : ''}
                        </div>
                        <div style="padding: 8px; min-height: 60px; flex: 1;">`;

        group.members.forEach(student => {
            const isLeader = group.leader === student;
            html += _renderStudentChip(student, groupId, isLeader);
        });

        if (group.members.length === 0) {
            html += `<div style="color: #475569; font-size: 11px; text-align: center; padding: 15px 0;">Kéo thả HS vào đây</div>`;
        }

        html += `
                        </div>
                    </div>`;
    });

    html += `
                </div>

                <!-- Nút thêm nhóm -->
                <button onclick="addBreakoutGroup()" style="margin-top: 10px; width: 100%; padding: 8px; background: transparent; border: 2px dashed #334155; border-radius: 8px; color: #64748b; cursor: pointer; font-size: 13px; transition: all 0.2s;"
                        onmouseover="this.style.borderColor='#3b82f6'; this.style.color='#3b82f6';"
                        onmouseout="this.style.borderColor='#334155'; this.style.color='#64748b';">
                    + Thêm nhóm
                </button>
            </div>
        </div>

        <!-- Footer -->
        <div style="padding: 12px 20px; border-top: 1px solid #334155; display: flex; align-items: center; gap: 10px;">
            <div style="display: flex; align-items: center; gap: 8px; flex: 1;">
                <label style="font-size: 13px; color: #94a3b8; white-space: nowrap;">Thời gian:</label>
                <select id="breakout-duration" style="padding: 6px 10px; background: #0f172a; color: white; border: 1px solid #334155; border-radius: 6px; font-size: 13px;">
                    <option value="5">5 phút</option>
                    <option value="10" selected>10 phút</option>
                    <option value="15">15 phút</option>
                    <option value="20">20 phút</option>
                    <option value="30">30 phút</option>
                </select>
            </div>
            <button onclick="autoAssignBreakout()" style="padding: 8px 14px; background: #475569; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 13px; font-weight: bold;">
                <i class="fa-solid fa-shuffle"></i> Chia tự động
            </button>
            <button onclick="closeBreakoutModal()" style="padding: 8px 14px; background: #334155; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 13px; font-weight: bold;">Hủy</button>
            <button onclick="confirmCreateBreakout()" style="padding: 8px 18px; background: #3b82f6; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 13px; font-weight: bold;">
                <i class="fa-solid fa-play"></i> Bắt đầu
            </button>
        </div>
    </div>`;

    modal.innerHTML = html;
}

function _renderStudentChip(studentName, groupId, isLeader) {
    const dblClickAttr = groupId ? `ondblclick="setBreakoutGroupLeader('${groupId}', '${studentName}')" title="Nhấn đúp để chọn làm Nhóm trưởng"` : '';
    const icon = isLeader ? '<i class="fa-solid fa-crown" style="font-size: 10px; color: gold;"></i>' : '<i class="fa-solid fa-user" style="font-size: 10px; color: #60a5fa;"></i>';
    const border = isLeader ? 'border: 1px solid gold;' : 'border: 1px solid transparent;';
    
    return `
        <div draggable="true" 
             ondragstart="_breakoutDragStart(event, '${studentName}')"
             ondragend="_breakoutDragEnd(event)"
             ${dblClickAttr}
             style="display: inline-flex; align-items: center; gap: 6px; padding: 5px 10px; margin: 3px; background: #334155; border-radius: 6px; font-size: 12px; color: #e2e8f0; cursor: grab; user-select: none; transition: all 0.15s; ${border}"
             onmouseover="this.style.background='#475569'; this.style.transform='scale(1.03)';"
             onmouseout="this.style.background='#334155'; this.style.transform='scale(1)';">
            ${icon}
            ${studentName}
        </div>`;
}

