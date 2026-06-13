window.TSETrayFlyout = {
    container: null,
    caret: null,
    lastBrightnessRequest: -1,

    init: function() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.id = 'tse-tray-flyout';
            this.container.style.cssText = `
                position: fixed;
                background: rgba(32, 32, 32, 0.85);
                backdrop-filter: blur(20px);
                -webkit-backdrop-filter: blur(20px);
                color: #ffffff;
                border-radius: 12px;
                box-shadow: 0 8px 32px rgba(0,0,0,0.4);
                padding: 16px;
                z-index: 2147483647;
                display: none;
                font-family: 'Segoe UI Variable', 'Segoe UI', Arial, sans-serif;
                border: 1px solid rgba(255, 255, 255, 0.1);
            `;
            
            // Add caret (optional, Win11 doesn't always have a caret, but we keep a subtle one or omit it. Let's omit for cleaner Win11 look)
            
            document.body.appendChild(this.container);

            // Global click to close
            document.addEventListener('mousedown', (e) => {
                if (this.container.style.display !== 'none' && !this.container.contains(e.target)) {
                    this.hide();
                }
            });
        }
    },

    show: function(anchorX, anchorY, htmlContent, width) {
        this.init();
        
        this.container.innerHTML = htmlContent;
        this.container.style.width = width ? width + 'px' : 'auto';
        this.container.style.display = 'block';
        
        // Calculate position
        const rect = this.container.getBoundingClientRect();
        let popupX = anchorX - rect.width / 2;
        let popupY = anchorY - rect.height - 16; // 16px padding above icon
        
        // Clamp to screen
        if (popupX < 12) popupX = 12;
        if (popupX + rect.width > window.innerWidth - 12) popupX = window.innerWidth - rect.width - 12;
        
        this.container.style.left = popupX + 'px';
        this.container.style.top = popupY + 'px';
    },

    showQuickSettings: function(payload) {
        console.log("[TSE_TRAY_DOM] Showing Quick Settings via JCEF DOM.");
        
        let wifiContent = '';
        if (payload.wifiLoading) {
            wifiContent = `
                <div style="font-size: 13px; color: #aaa;">Đang quét WiFi...</div>
            `;
        } else {
            let networksHtml = (payload.networks || []).map(n => `
                <div style="padding: 6px 8px; border-radius: 4px; cursor: pointer; display: flex; align-items: center;" 
                     onmouseover="this.style.background='rgba(255,255,255,0.06)'" 
                     onmouseout="this.style.background='transparent'"
                     onclick="window.TSETrayFlyout.onWifiClick()">
                    <div style="font-size: 13px;">${n}</div>
                </div>
            `).join('');
            
            wifiContent = `
                <div style="padding: 6px 8px; border-radius: 4px; margin-bottom: 4px; display: flex; align-items: center; background: rgba(255,255,255,0.06);">
                    <div style="font-weight: 600; font-size: 13px; color: #60CDFF;">${payload.currentSsid} (Đang kết nối)</div>
                </div>
                <div style="max-height: 120px; overflow-y: auto; margin-bottom: 8px;">
                    ${networksHtml}
                </div>
                <div id="tse-wifi-msg" style="font-size: 12px; color: #ff99a4; display: none; margin-bottom: 8px;">Chế độ chỉ xem, không thể kết nối</div>
            `;
        }

        const html = `
            <!-- Top section: Tiles -->
            <div style="display: flex; gap: 8px; margin-bottom: 16px;">
                <!-- WiFi Tile -->
                <div style="flex: 1; background: rgba(255,255,255,0.05); border-radius: 8px; padding: 12px; border: 1px solid rgba(255,255,255,0.08);">
                    <div style="display: flex; align-items: center; margin-bottom: 8px;">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#60CDFF" stroke-width="2" style="margin-right: 8px;">
                            <path d="M5 12.55a11 11 0 0 1 14.08 0"></path>
                            <path d="M1.42 9a16 16 0 0 1 21.16 0"></path>
                            <path d="M8.53 16.11a6 6 0 0 1 6.95 0"></path>
                            <line x1="12" y1="20" x2="12.01" y2="20"></line>
                        </svg>
                        <span style="font-weight: 600; font-size: 14px;">Wi-Fi</span>
                    </div>
                    ${wifiContent}
                </div>
            </div>
            
            <!-- Middle section: Sliders -->
            <div style="margin-bottom: 16px; padding: 0 4px;">
                <!-- Brightness Slider -->
                <div style="display: flex; align-items: center; margin-bottom: 12px; position: relative;">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" style="margin-right: 12px;">
                        <circle cx="12" cy="12" r="5"></circle>
                        <line x1="12" y1="1" x2="12" y2="3"></line>
                        <line x1="12" y1="21" x2="12" y2="23"></line>
                        <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                        <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                        <line x1="1" y1="12" x2="3" y2="12"></line>
                        <line x1="21" y1="12" x2="23" y2="12"></line>
                        <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                        <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
                    </svg>
                    <div style="flex: 1; position: relative; height: 20px; display: flex; align-items: center;">
                        <input type="range" id="tse-brightness-slider" min="0" max="100" value="${payload.brightnessSupported ? payload.brightnessPercent : 0}" ${!payload.brightnessSupported ? 'disabled' : ''} style="width: 100%; -webkit-appearance: none; appearance: none; background: transparent; cursor: pointer;" oninput="window.TSETrayFlyout.onBrightnessInput(this.value)" onchange="window.TSETrayFlyout.onBrightnessChange(this.value)">
                        <div style="position: absolute; pointer-events: none; left: 0; width: 100%; height: 4px; background: rgba(255,255,255,0.2); border-radius: 2px; z-index: -1;">
                            <div id="tse-brightness-track" style="height: 100%; width: ${payload.brightnessSupported ? payload.brightnessPercent : 0}%; background: ${payload.brightnessSupported ? '#60CDFF' : '#555'}; border-radius: 2px;"></div>
                        </div>
                    </div>
                </div>
                ${!payload.brightnessSupported ? '<div style="font-size: 11px; color: #aaa; text-align: right; margin-top: -8px; margin-bottom: 8px;">Không hỗ trợ điều chỉnh độ sáng</div>' : '<div id="tse-brightness-status" style="font-size: 11px; color: #aaa; text-align: right; margin-top: -8px; margin-bottom: 8px; display: none;">Đang chỉnh...</div>'}
                
                <!-- Volume Slider -->
                <div style="display: flex; align-items: center;">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2" style="margin-right: 12px;">
                        <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                        <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
                    </svg>
                    <div style="flex: 1; height: 4px; background: rgba(255,255,255,0.2); border-radius: 2px; position: relative;">
                        <div style="position: absolute; left: 0; top: 0; height: 100%; width: 50%; background: #60CDFF; border-radius: 2px;"></div>
                        <div style="position: absolute; left: 50%; top: -4px; width: 12px; height: 12px; background: #60CDFF; border-radius: 50%; transform: translateX(-50%);"></div>
                    </div>
                </div>
            </div>
            
            <!-- Bottom section: Status -->
            <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 12px;">
                <div style="display: flex; align-items: center; font-size: 12px; color: #ccc;">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 6px;">
                        <rect x="1" y="6" width="18" height="12" rx="2" ry="2"></rect>
                        <line x1="23" y1="13" x2="23" y2="11"></line>
                    </svg>
                    ${payload.hasBattery ? payload.percent + '% - ' + payload.statusText : 'Nguồn điện: AC'}
                </div>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#aaa" stroke-width="2">
                    <circle cx="12" cy="12" r="3"></circle>
                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                </svg>
            </div>
        `;
        
        // Use a fixed width for Quick Settings like Win11
        this.show(payload.anchorX, payload.anchorY, html, 320);
    },

    showLanguageSwitcher: function(payload) {
        console.log("[TSE_TRAY_DOM] Showing Language Switcher via JCEF DOM.");
        
        const modes = [
            { id: 'ENG', name: 'English', desc: 'US Keyboard' },
            { id: 'VIE', name: 'Vietnamese Telex', desc: 'TSE Internal Engine' }
        ];
        
        let html = '';
        modes.forEach(m => {
            const isActive = m.id === payload.activeMode;
            const accentStyle = isActive ? `border-left: 3px solid #60CDFF; background: rgba(255,255,255,0.06);` : `border-left: 3px solid transparent;`;
            html += `
                <div style="padding: 10px 16px; margin-bottom: 4px; border-radius: 4px; cursor: pointer; display: flex; flex-direction: column; ${accentStyle}"
                     onmouseover="if('${m.id}' !== '${payload.activeMode}') this.style.background='rgba(255,255,255,0.04)'" 
                     onmouseout="if('${m.id}' !== '${payload.activeMode}') this.style.background='transparent'"
                     onclick="window.TSETrayFlyout.onLanguageSelect('${m.id}')">
                    <div style="font-weight: 600; font-size: 14px; margin-bottom: 2px;">${m.name}</div>
                    <div style="font-size: 12px; color: #aaa;">${m.desc}</div>
                </div>
            `;
        });
        
        this.show(payload.anchorX, payload.anchorY, html, 280);
    },

    onWifiClick: function() {
        const msg = document.getElementById('tse-wifi-msg');
        if (msg) msg.style.display = 'block';
    },

    onBrightnessInput: function(value) {
        // Update track visually immediately
        const track = document.getElementById('tse-brightness-track');
        if (track) {
            track.style.width = value + '%';
        }
    },

    onBrightnessChange: function(value) {
        if (this.lastBrightnessRequest === value) return;
        this.lastBrightnessRequest = value;

        const status = document.getElementById('tse-brightness-status');
        if (status) {
            status.style.display = 'block';
            status.innerText = 'Đang chỉnh...';
        }

        if (window.cefQuery) {
            window.cefQuery({
                request: 'TSE_BRIGHTNESS_SET:' + value,
                onSuccess: function(response) {
                    if (status) {
                        if (response === 'SUCCESS') {
                            status.innerText = 'Đã lưu';
                            setTimeout(() => { status.style.display = 'none'; }, 2000);
                        } else {
                            status.innerText = 'Lỗi / Không hỗ trợ';
                        }
                    }
                },
                onFailure: function(errorCode, errorMessage) {
                    if (status) status.innerText = 'Lỗi hệ thống';
                }
            });
        }
    },

    onLanguageSelect: function(modeId) {
        if (window.cefQuery) {
            window.cefQuery({request: 'SUBMIT_PAYLOAD:TSE_LANG_SELECT:' + modeId});
        }
        this.hide();
    },

    hide: function() {
        if (this.container) {
            this.container.style.display = 'none';
        }
    }
};

console.log("[TSE_TRAY_DOM] TSETrayFlyout ready.");
