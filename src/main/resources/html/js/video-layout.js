class VideoLayoutManager {
    constructor() {
        this.container = document.getElementById('video-layout-container');
        this.videos = new Map(); // identity -> { videoEl, wrapperEl, name }
        this.currentMode = 'top-bar';
        this.activeSpeaker = null;
        this.pinnedParticipant = null;

        // Initialize from room state if available
        if (window.roomState) {
            const savedMode = window.roomState.get('videoLayoutMode');
            if (savedMode) this.setMode(savedMode);

            window.roomState.addEventListener('change', (e) => {
                if (e.detail.key === 'videoLayoutMode') {
                    this.setMode(e.detail.newValue);
                }
            });
        }
        
        // ResizeObserver for dynamic gallery layout
        this.resizeObserver = new ResizeObserver(() => {
            if (this.currentMode === 'gallery') {
                this.updateLayout();
            }
        });
        if (this.container) {
            this.resizeObserver.observe(this.container);
        }
    }

    setMode(mode) {
        if (!['top-bar', 'gallery', 'speaker'].includes(mode)) return;
        this.currentMode = mode;
        if (window.roomState && window.roomState.get('videoLayoutMode') !== mode) {
            window.roomState.set('videoLayoutMode', mode);
        }

        if (this.container) {
            // Remove old mode classes
            this.container.classList.remove('layout-top-bar', 'layout-gallery', 'layout-speaker');
            this.container.classList.add(`layout-${mode}`);
            this.updateLayout();
        }
    }

    addVideo(participantIdentity, videoElement) {
        if (this.videos.has(participantIdentity)) {
            // Update existing if needed
            const item = this.videos.get(participantIdentity);
            if (item.videoEl !== videoElement) {
                item.wrapperEl.replaceChild(videoElement, item.videoEl);
                item.videoEl = videoElement;
                this._styleVideoElement(videoElement);
            }
            return;
        }

        const wrapper = document.createElement('div');
        wrapper.id = 'participant-video-' + participantIdentity;
        wrapper.classList.add('video-wrapper');

        this._styleVideoElement(videoElement);
        
        const nameTag = document.createElement('div');
        nameTag.classList.add('video-name-tag');
        nameTag.innerText = participantIdentity;

        // Pin button
        const pinBtn = document.createElement('button');
        pinBtn.classList.add('video-pin-btn');
        pinBtn.innerHTML = '<i class="fa-solid fa-thumbtack"></i>';
        pinBtn.onclick = (e) => {
            e.stopPropagation();
            this.togglePin(participantIdentity);
        };

        wrapper.appendChild(videoElement);
        wrapper.appendChild(nameTag);
        wrapper.appendChild(pinBtn);

        wrapper.onclick = () => {
            // Mở code của người này khi click vào video (như cũ)
            if (typeof toggleCodeMode === 'function' && typeof showToast === 'function') {
                const container = document.getElementById('editor-container');
                if (container && window.TUTORHUB_CONFIG) {
                    container.innerHTML = `<iframe src="${window.TUTORHUB_CONFIG.VSCODE_SERVER}/login?user=${encodeURIComponent(participantIdentity)}" style="width:100%; height:100%; border:none;"></iframe>`;
                    if (typeof isCodeMode !== 'undefined' && !isCodeMode) toggleCodeMode();
                    showToast("Đang xem Code của " + participantIdentity, 3000);
                }
            }
        };

        this.videos.set(participantIdentity, {
            videoEl: videoElement,
            wrapperEl: wrapper,
            name: participantIdentity
        });

        if (this.container) {
            this.container.style.display = 'flex'; // Hiển thị container
            this.container.appendChild(wrapper);
            this.updateLayout();
        }
    }

    removeVideo(participantIdentity) {
        if (!this.videos.has(participantIdentity)) return;
        const item = this.videos.get(participantIdentity);
        if (item.wrapperEl && item.wrapperEl.parentNode) {
            item.wrapperEl.parentNode.removeChild(item.wrapperEl);
        }
        this.videos.delete(participantIdentity);
        
        if (this.pinnedParticipant === participantIdentity) {
            this.pinnedParticipant = null;
        }

        if (this.videos.size === 0 && this.container) {
            this.container.style.display = 'none'; // Ẩn khi không có ai
        }

        this.updateLayout();
    }

    setActiveSpeaker(identities) {
        // identities is an array of participant identities who are speaking
        this.activeSpeaker = identities.length > 0 ? identities[0] : null;

        this.videos.forEach((item, id) => {
            if (identities.includes(id)) {
                item.wrapperEl.classList.add('is-speaking');
            } else {
                item.wrapperEl.classList.remove('is-speaking');
            }
        });

        if (this.currentMode === 'speaker') {
            this.updateLayout();
        }
    }

    togglePin(participantIdentity) {
        if (this.pinnedParticipant === participantIdentity) {
            this.pinnedParticipant = null;
        } else {
            this.pinnedParticipant = participantIdentity;
            // Tự động chuyển sang Speaker Mode nếu pin
            if (this.currentMode !== 'speaker') {
                this.setMode('speaker');
            }
        }
        this.updateLayout();
    }

    updateLayout() {
        if (!this.container) return;

        this.videos.forEach((item, id) => {
            item.wrapperEl.classList.remove('main-speaker', 'small-thumbnail');
            item.wrapperEl.querySelector('.video-pin-btn').classList.remove('active');
        });

        if (this.pinnedParticipant && this.videos.has(this.pinnedParticipant)) {
            this.videos.get(this.pinnedParticipant).wrapperEl.querySelector('.video-pin-btn').classList.add('active');
        }

        if (this.currentMode === 'top-bar') {
            // Reset inline styles for flex/grid fallbacks
            this.videos.forEach((item) => {
                item.wrapperEl.style.width = '';
                item.wrapperEl.style.height = '';
            });
        } else if (this.currentMode === 'gallery') {
            const count = this.videos.size;
            this.container.setAttribute('data-count', count);
            
            // Jitsi's Dynamic Grid Algorithm
            if (count > 0 && this.container) {
                const rect = this.container.getBoundingClientRect();
                const containerWidth = rect.width;
                const containerHeight = rect.height;
                
                // Aspect ratio is 16:9
                const TARGET_RATIO = 16 / 9;
                
                // Base grid dimensions (perfect square bias)
                let columns = Math.ceil(Math.sqrt(count));
                let rows = Math.ceil(count / columns);
                
                // Calculate item dimensions based on container width
                let itemWidth = Math.floor(containerWidth / columns) - 8; // -8px for gap/margins
                let itemHeight = Math.floor(itemWidth / TARGET_RATIO);
                
                // If it overflows vertically, recalculate based on container height
                if ((itemHeight * rows) > containerHeight) {
                    itemHeight = Math.floor(containerHeight / rows) - 8;
                    itemWidth = Math.floor(itemHeight * TARGET_RATIO);
                }
                
                // Apply explicit dimensions for perfect gallery layout
                this.videos.forEach((item) => {
                    item.wrapperEl.style.width = itemWidth + 'px';
                    item.wrapperEl.style.height = itemHeight + 'px';
                });
                
                // Ensure container is flex and centered to hold the grid
                this.container.style.display = 'flex';
                this.container.style.flexWrap = 'wrap';
                this.container.style.justifyContent = 'center';
                this.container.style.alignContent = 'center';
                this.container.style.gap = '8px';
            }
        } else if (this.currentMode === 'speaker') {
            // Reset inline styles
            this.videos.forEach((item) => {
                item.wrapperEl.style.width = '';
                item.wrapperEl.style.height = '';
            });
            // Determine who is the main speaker
            let mainId = this.pinnedParticipant || this.activeSpeaker;
            
            // Nếu không có ai ghim và ko ai nói, lấy ng đầu tiên
            if (!mainId || !this.videos.has(mainId)) {
                mainId = this.videos.keys().next().value;
            }

            this.videos.forEach((item, id) => {
                if (id === mainId) {
                    item.wrapperEl.classList.add('main-speaker');
                } else {
                    item.wrapperEl.classList.add('small-thumbnail');
                }
            });
        }
    }

    _styleVideoElement(element) {
        element.style.width = '100%';
        element.style.height = '100%';
        element.style.objectFit = 'cover';
        element.style.borderRadius = '8px';
    }
}

// Khởi tạo global
window.addEventListener('DOMContentLoaded', () => {
    window.videoLayoutManager = new VideoLayoutManager();
});

window.toggleVideoLayout = function() {
    if (!window.videoLayoutManager) return;
    const current = window.videoLayoutManager.currentMode;
    let nextMode = 'gallery';
    if (current === 'top-bar') nextMode = 'gallery';
    else if (current === 'gallery') nextMode = 'speaker';
    else nextMode = 'top-bar';
    window.videoLayoutManager.setMode(nextMode);
    
    const btnIcon = document.querySelector('#layout-toggle-btn i');
    const btnSpan = document.querySelector('#layout-toggle-btn span');
    if (btnIcon && btnSpan) {
        if (nextMode === 'top-bar') {
            btnIcon.className = 'fa-solid fa-window-maximize';
            btnSpan.innerText = 'Top Bar';
        } else if (nextMode === 'gallery') {
            btnIcon.className = 'fa-solid fa-table-cells-large';
            btnSpan.innerText = 'Gallery';
        } else {
            btnIcon.className = 'fa-solid fa-user-large';
            btnSpan.innerText = 'Speaker';
        }
    }
};
