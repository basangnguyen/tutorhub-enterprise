(function () {
  console.log("[TSE_QS_EXAM_JS] LOADED version=BRIGHTNESS_RESEARCH_FIX");
})();

window.TSETrayFlyout = {
    container: null,
    caret: null,
    lastBrightnessRequest: -1,
    lastVolumeRequest: -1,
    isMuted: false,
    currentVolume: 0,
    lastNonZeroVolume: 30,
    brightnessTimeout: null,
    volumeTimeout: null,

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
            
            // Add global styles for slider
            if (!document.getElementById('tse-tray-styles')) {
                const style = document.createElement('style');
                style.id = 'tse-tray-styles';
                style.innerHTML = `
                    #tse-tray-flyout input[type="range"] {
                        -webkit-appearance: none;
                        width: 100%;
                        background: transparent;
                        margin: 0;
                        padding: 0;
                    }
                    #tse-tray-flyout input[type="range"]:focus {
                        outline: none;
                    }
                    #tse-tray-flyout input[type="range"]::-webkit-slider-runnable-track {
                        width: 100%;
                        height: 4px;
                        cursor: pointer;
                        background: rgba(255, 255, 255, 0.2);
                        border-radius: 2px;
                    }
                    #tse-tray-flyout input[type="range"]::-webkit-slider-thumb {
                        height: 16px;
                        width: 16px;
                        border-radius: 50%;
                        background: #60CDFF;
                        cursor: pointer;
                        -webkit-appearance: none;
                        margin-top: -6px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.4);
                        transition: transform 0.1s;
                    }
                    #tse-tray-flyout input[type="range"]::-webkit-slider-thumb:hover {
                        transform: scale(1.1);
                    }
                    #tse-tray-flyout input[type="range"]::-webkit-slider-thumb:active {
                        transform: scale(0.9);
                    }
                `;
                document.head.appendChild(style);
            }
            
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
        
        // bindExamBrightnessSlider removed - brightness now uses inline oninput/onchange on the slider
        
        this.container.style.width = width ? width + 'px' : 'auto';
        this.container.style.display = 'block';
        
        // Calculate position
        const rect = this.container.getBoundingClientRect();
        let popupX = anchorX - rect.width / 2;
        let popupY = anchorY - rect.height - 16; // 16px padding above icon
        
        // Clamp to screen edges
        if (popupX < 12) popupX = 12;
        if (popupX + rect.width > window.innerWidth - 12) {
            popupX = window.innerWidth - rect.width - 12;
        }
        
        this.container.style.left = popupX + 'px';
        this.container.style.top = popupY + 'px';
    },

    showQuickSettings: function(payloadRaw) {
        console.log("[TSE_TRAY_DOM] Showing Quick Settings via JCEF DOM.");

        // Handle new snapshot structure from QuickSettingsController
        let payload = {};
        if (payloadRaw.snapshot) {
            payload.anchorX = payloadRaw.anchorX;
            payload.anchorY = payloadRaw.anchorY;
            payload.inputTestEnabled = payloadRaw.inputTestEnabled;
            payload.volumeMuted = payloadRaw.snapshot.volumeMuted;
            payload.volumePercent = payloadRaw.snapshot.volumePercent;
            payload.volumeSupported = payloadRaw.snapshot.volumeSupported;
            payload.brightnessPercent = payloadRaw.snapshot.brightnessPercent;
            payload.brightnessSupported = payloadRaw.snapshot.brightnessSupported;
            payload.currentSsid = payloadRaw.snapshot.wifiSsid || "Không kết nối";
            payload.wifiLoading = false;
            payload.networks = [];
            payload.hasBattery = payloadRaw.snapshot.hasBattery;
            payload.percent = payloadRaw.snapshot.batteryPercent;
            payload.statusText = payload.hasBattery ? (payloadRaw.snapshot.batteryCharging ? "Đang sạc" : "Đang dùng pin") : "Không phát hiện pin";
        } else {
            payload = payloadRaw;
        }

        this.isMuted = payload.volumeMuted || payload.volumePercent === 0;
        this.currentVolume = this.isMuted ? 0 : payload.volumePercent;
        if (payload.volumePercent > 0) {
            this.lastNonZeroVolume = payload.volumePercent;
        }

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
                    <div style="width: 24px; height: 24px; margin-right: 12px; display: flex; justify-content: center; align-items: center;">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg>
                    </div>
                    <input type="range" id="exam-slider-brightness" class="brightness-slider" data-role="brightness-slider" name="brightness" min="0" max="100" value="${payload.brightnessSupported ? payload.brightnessPercent : 0}"
                           ${!payload.brightnessSupported ? 'disabled' : ''}
                           style="flex: 1;">
                    <div id="brightness-val" style="font-size: 13px; font-weight: 500; margin-left: 12px; width: 36px; text-align: right;">${payload.brightnessSupported ? payload.brightnessPercent : 0}%</div>
                </div>
                ${!payload.brightnessSupported ? '<div style="font-size: 11px; color: #aaa; text-align: right; margin-top: -8px; margin-bottom: 8px;">Không hỗ trợ điều chỉnh độ sáng</div>' : '<div id="tse-brightness-status" style="font-size: 11px; color: #aaa; text-align: right; margin-top: -8px; margin-bottom: 8px; display: none;">Đang chỉnh...</div>'}
                
                <!-- Volume -->
                <div style="display: flex; align-items: center; margin-bottom: 4px; position: relative;">
                    <div id="volume-icon-container" onclick="window.TSETrayFlyout.toggleMute()" style="width: 28px; height: 28px; margin-right: 12px; display: flex; justify-content: center; align-items: center; cursor: pointer; border-radius: 6px; transition: background 0.1s;" onmouseover="this.style.background='rgba(255,255,255,0.1)'" onmouseout="this.style.background='transparent'">
                        ${(payload.volumeMuted || payload.volumePercent === 0) 
                            ? '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><line x1="23" y1="9" x2="17" y2="15"></line><line x1="17" y1="9" x2="23" y2="15"></line></svg>' 
                            : '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path></svg>'}
                    </div>
                    <input type="range" id="volume-val-slider" min="0" max="100" value="${payload.volumeMuted ? 0 : payload.volumePercent}"
                           ${!payload.volumeSupported ? 'disabled' : ''}
                           oninput="window.TSETrayFlyout.onVolumeInput(this.value);"
                           style="flex: 1;">
                    <div id="volume-val" style="font-size: 13px; font-weight: 500; margin-left: 12px; width: 44px; text-align: right;">${payload.volumeMuted || payload.volumePercent === 0 ? 'Muted' : payload.volumePercent + '%'}</div>
                </div>
                ${!payload.volumeSupported ? '<div style="font-size: 11px; color: #aaa; text-align: right; margin-top: -8px; margin-bottom: 8px;">Không hỗ trợ điều chỉnh âm lượng</div>' : '<div id="tse-volume-status" style="font-size: 11px; color: #aaa; text-align: right; margin-top: -8px; margin-bottom: 8px; display: none;">Đang chỉnh...</div>'}
                
                ${payload.inputTestEnabled ? `
                <!-- Test Sound Button (Debug Only) -->
                <div style="display: flex; justify-content: center; margin-top: 8px;">
                    <button onclick="window.TSETrayFlyout.onTestSoundClick()" style="background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); color: white; padding: 4px 12px; border-radius: 4px; cursor: pointer; font-size: 12px; display: flex; align-items: center;">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 6px;"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path></svg>
                        Test Sound
                    </button>
                </div>
                ` : ''}
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
        installExamBrightnessDelegation();
        debugExamBrightnessSlider();
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

    onVolumeInput: function(value) {
        let val = parseInt(value);
        this.currentVolume = val;
        this.isMuted = (val === 0);
        if (val > 0) {
            this.lastNonZeroVolume = val;
        }
        this.updateVolumeIcon(val);
        
        // Update label immediately
        const valElem = document.getElementById('volume-val');
        if (valElem) valElem.innerText = this.isMuted ? 'Muted' : val + '%';
        
        if (this.volumeTimeout) clearTimeout(this.volumeTimeout);
        this.volumeTimeout = setTimeout(() => {
            console.log("[TSE_VOLUME_UI] Debounced set requested: " + val);
            this.onVolumeChange(val);
        }, 150);
    },

    onVolumeChange: function(value) {
        if (this.lastVolumeRequest === value) return;
        this.lastVolumeRequest = value;

        const status = document.getElementById('tse-volume-status');
        if (status) {
            status.style.display = 'block';
            status.innerText = 'Đang chỉnh...';
        }

        if (window.cefQuery) {
            window.cefQuery({
                request: 'TSE_VOLUME_SET:' + value,
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

    updateVolumeIcon: function(val, forcedMute) {
        val = parseInt(val);
        let muted = forcedMute !== undefined ? forcedMute : (val === 0);
        let label = document.getElementById('volume-val');
        let iconContainer = document.getElementById('volume-icon-container');
        if (muted || val === 0) {
            if (label) label.innerText = 'Muted';
            if (iconContainer) iconContainer.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><line x1="23" y1="9" x2="17" y2="15"></line><line x1="17" y1="9" x2="23" y2="15"></line></svg>';
        } else {
            if (label) label.innerText = val + '%';
            if (iconContainer) iconContainer.innerHTML = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path></svg>';
        }
    },

    toggleMute: function() {
        if (this.isMuted) {
            let restoreVol = this.lastNonZeroVolume > 0 ? this.lastNonZeroVolume : 30;
            console.log("[TSE_VOLUME_UI] Toggle mute requested from EXAM currentMuted=true currentPercent=0");
            this.isMuted = false;
            this.currentVolume = restoreVol;
            
            let slider = document.getElementById('volume-val-slider');
            if (slider) slider.value = restoreVol;
            this.updateVolumeIcon(restoreVol, false);
            
            if (window.cefQuery) {
                window.cefQuery({ request: 'TSE_VOLUME_MUTE:false' });
            }
            console.log("[TSE_VOLUME_UI] Mute UI synced: muted=false percent=" + restoreVol);
        } else {
            console.log("[TSE_VOLUME_UI] Toggle mute requested from EXAM currentMuted=false currentPercent=" + this.currentVolume);
            if (this.currentVolume > 0) {
                this.lastNonZeroVolume = this.currentVolume;
            }
            this.isMuted = true;
            this.currentVolume = 0;
            
            let slider = document.getElementById('volume-val-slider');
            if (slider) slider.value = 0;
            this.updateVolumeIcon(0, true);
            
            if (window.cefQuery) {
                window.cefQuery({ request: 'TSE_VOLUME_MUTE:true' });
            }
            console.log("[TSE_VOLUME_UI] Mute UI synced: muted=true percent=0");
        }
    },

    onTestSoundClick: function() {
        if (window.cefQuery) {
            window.cefQuery({
                request: 'TSE_TEST_SOUND_PLAY'
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

// ========== EXAM BRIGHTNESS HANDLERS ==========

var examLastBrightnessRequest = -1;
var examBrightnessDelegationInstalled = false;
var examBrightnessDragging = false;
var examLastBrightnessValue = null;

function isExamBrightnessSlider(el) {
    return !!el && (
        el.id === "exam-slider-brightness" ||
        el.id === "brightnessSlider" ||
        el.id === "brightness-slider" ||
        el.getAttribute("data-role") === "brightness-slider" ||
        (el.classList && el.classList.contains("brightness-slider")) ||
        el.getAttribute("name") === "brightness"
    );
}

function findExamBrightnessSlider() {
    return document.getElementById("exam-slider-brightness") ||
        document.getElementById("brightnessSlider") ||
        document.getElementById("brightness-slider") ||
        document.querySelector("#tse-tray-flyout [data-role='brightness-slider']") ||
        document.querySelector("#tse-tray-flyout input[type='range'].brightness-slider") ||
        document.querySelector("#tse-tray-flyout input[type='range'][name='brightness']");
}

function closestExamBrightnessSlider(target) {
    var current = target;
    while (current && current !== document) {
        if (isExamBrightnessSlider(current)) {
            return current;
        }
        current = current.parentNode;
    }
    return null;
}

function stopExamSliderEvent(event) {
    try {
        if (event.stopPropagation) {
            event.stopPropagation();
        }
    } catch (e) {}
}

function installExamBrightnessDelegation() {
    if (examBrightnessDelegationInstalled) {
        return;
    }
    examBrightnessDelegationInstalled = true;

    document.addEventListener("pointerdown", function(event) {
        var slider = closestExamBrightnessSlider(event.target);
        if (!slider || slider.disabled) {
            return;
        }
        examBrightnessDragging = true;
        examLastBrightnessValue = parseInt(slider.value, 10);
        stopExamSliderEvent(event);
        console.log("[TSE_QS_EXAM_JS] brightness pointerdown value=" + slider.value);
    }, true);

    document.addEventListener("input", function(event) {
        var slider = closestExamBrightnessSlider(event.target);
        if (!slider || slider.disabled) {
            return;
        }
        stopExamSliderEvent(event);
        var percent = parseInt(slider.value, 10);
        examLastBrightnessValue = percent;
        console.log("[TSE_QS_EXAM_JS] brightness input percent=" + percent);
        onExamBrightnessInput(percent);
    }, true);

    document.addEventListener("change", function(event) {
        var slider = closestExamBrightnessSlider(event.target);
        if (!slider || slider.disabled) {
            return;
        }
        stopExamSliderEvent(event);
        var percent = parseInt(slider.value, 10);
        examLastBrightnessValue = percent;
        console.log("[TSE_QS_EXAM_JS] brightness change percent=" + percent);
        onExamBrightnessChange(percent);
    }, true);

    document.addEventListener("pointerup", function() {
        if (!examBrightnessDragging) {
            return;
        }
        examBrightnessDragging = false;
        if (typeof examLastBrightnessValue === "number" && !isNaN(examLastBrightnessValue)) {
            console.log("[TSE_QS_EXAM_JS] brightness pointerup commit percent=" + examLastBrightnessValue);
            onExamBrightnessChange(examLastBrightnessValue);
        }
    }, true);

    console.log("[TSE_QS_EXAM_JS] brightness delegation installed");
}

function debugExamBrightnessSlider() {
    var slider = findExamBrightnessSlider();
    if (!slider) {
        console.log("[TSE_QS_EXAM_JS] brightness slider NOT FOUND");
        return;
    }
    var rect = slider.getBoundingClientRect();
    var style = window.getComputedStyle(slider);
    var topEl = document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2);
    console.log("[TSE_QS_EXAM_JS] brightness slider FOUND"
        + " id=" + slider.id
        + " disabled=" + slider.disabled
        + " pointerEvents=" + style.pointerEvents
        + " rect=" + Math.round(rect.x) + "," + Math.round(rect.y) + "," + Math.round(rect.width) + "," + Math.round(rect.height)
        + " topElement=" + (topEl ? topEl.tagName + "#" + topEl.id + "." + topEl.className : "null")
    );
}

function onExamBrightnessInput(val) {
    var percent = parseInt(val, 10);
    console.log("[TSE_QS_EXAM_JS] brightness input percent=" + percent);
    
    var valElem = document.getElementById('brightness-val');
    if (valElem) valElem.innerText = percent + '%';
    
    // Don't send command on every input to avoid WMI lag
    // Wait for onchange (mouseup/release)
}

function onExamBrightnessChange(val) {
    var percent = parseInt(val, 10);
    if (examLastBrightnessRequest === percent) return;
    examLastBrightnessRequest = percent;
    
    console.log("[TSE_QS_EXAM_JS] brightness change percent=" + percent);
    sendExamBrightnessToJava(percent);
}

function sendExamBrightnessToJava(percent) {
    if (percent === null || percent === undefined || isNaN(percent)) {
        console.log("[TSE_QS_EXAM_JS] invalid brightness percent");
        return;
    }
    
    percent = Math.max(0, Math.min(100, percent));
    
    var command = "TSE_BRIGHTNESS_SET:" + percent;
    console.log("[TSE_QS_EXAM_JS] send command=" + command);
    
    var status = document.getElementById('tse-brightness-status');
    if (status) {
        status.style.display = 'block';
        status.innerText = 'Đang chỉnh...';
    }
    
    if (window.cefQuery) {
        window.cefQuery({
            request: command,
            onSuccess: function (response) {
                console.log("[TSE_QS_EXAM_JS] brightness command success=" + response);
                if (status) {
                    if (response === 'SUCCESS') {
                        status.innerText = 'Đã lưu';
                        setTimeout(function() { status.style.display = 'none'; }, 2000);
                    } else {
                        status.innerText = 'Lỗi / Không hỗ trợ';
                    }
                }
            },
            onFailure: function (code, message) {
                console.log("[TSE_QS_EXAM_JS] brightness command failed code=" + code + " message=" + message);
                if (status) status.innerText = 'Lỗi hệ thống';
            }
        });
    } else {
        console.log("[TSE_QS_EXAM_JS] cefQuery missing");
    }
}

console.log("[TSE_TRAY_DOM] TSETrayFlyout ready.");
