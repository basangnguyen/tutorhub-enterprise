// tse-safe-refresh.js
(function() {
    if (window.TSESafeRefresh) return; // already injected

    window.TSESafeRefresh = {
        refreshInProgress: false,

        triggerSnapshotAndReload: function() {
            if (this.refreshInProgress) {
                console.log("[TSE_REFRESH] Refresh ignored: already in progress.");
                return;
            }

            console.log("[TSE_REFRESH] Refresh requested.");
            this.refreshInProgress = true;

            // Hide tray flyout if it exists
            if (window.TSETrayFlyout && typeof window.TSETrayFlyout.hide === 'function') {
                window.TSETrayFlyout.hide();
            }

            console.log("[TSE_REFRESH] Collecting answer snapshot...");
            let snapshot = [];
            
            try {
                // Collect all inputs
                let elements = document.querySelectorAll('input, textarea, select');
                elements.forEach(function(el) {
                    if (!el.name && !el.id) return; // skip anonymous elements
                    
                    let item = {
                        tagName: el.tagName.toLowerCase(),
                        type: el.type ? el.type.toLowerCase() : '',
                        id: el.id,
                        name: el.name,
                        value: el.value,
                        checked: el.checked
                    };
                    snapshot.push(item);
                });

                // Collect contenteditable if any
                let editables = document.querySelectorAll('[contenteditable="true"]');
                editables.forEach(function(el) {
                    if (!el.id) return;
                    snapshot.push({
                        tagName: 'contenteditable',
                        id: el.id,
                        innerHTML: el.innerHTML
                    });
                });

                if (snapshot.length === 0) {
                    // It's okay if snapshot is empty (no inputs), but just to be safe
                    console.log("[TSE_REFRESH] Snapshot is empty (no inputs found). Proceeding anyway.");
                }

                let snapshotStr = JSON.stringify(snapshot);
                sessionStorage.setItem("TSE_REFRESH_SNAPSHOT", snapshotStr);
                sessionStorage.setItem("TSE_INPUT_MODE", window.TSEInputMode || "en");
                console.log("[TSE_REFRESH] Snapshot collected.");

            } catch (e) {
                console.error("[TSE_REFRESH] Snapshot failed.", e);
                this.showError("Không thể làm mới an toàn lúc này. Vui lòng thử lại.");
                // Log failed state to Java
                window.cefQuery && window.cefQuery({request: 'TSE_REFRESH_FAILED:Snapshot failed'});
                this.refreshInProgress = false;
                return;
            }

            this.showOverlay();

            console.log("[TSE_REFRESH] Reloading JCEF content safely.");
            // Wait slightly for overlay to render, then reload
            setTimeout(function() {
                window.location.reload();
            }, 100);
        },

        restoreIfNeeded: function() {
            let snapshotStr = sessionStorage.getItem("TSE_REFRESH_SNAPSHOT");
            if (!snapshotStr) return;

            console.log("[TSE_REFRESH] Restore scheduled.");
            
            // Wait for DOM to be fully ready
            setTimeout(function() {
                try {
                    let snapshot = JSON.parse(snapshotStr);
                    snapshot.forEach(function(item) {
                        let el = null;
                        if (item.id) {
                            el = document.getElementById(item.id);
                        }
                        if (!el && item.name) {
                            // try to find by name and value for radios
                            if (item.type === 'radio' || item.type === 'checkbox') {
                                el = document.querySelector('input[name="' + item.name + '"][value="' + item.value + '"]');
                            } else {
                                el = document.querySelector(item.tagName + '[name="' + item.name + '"]');
                            }
                        }

                        if (el) {
                            if (item.tagName === 'contenteditable') {
                                el.innerHTML = item.innerHTML;
                            } else if (item.type === 'radio' || item.type === 'checkbox') {
                                el.checked = item.checked;
                            } else {
                                el.value = item.value;
                            }
                        }
                    });

                    // Restore input mode
                    let savedMode = sessionStorage.getItem("TSE_INPUT_MODE");
                    if (savedMode) {
                        window.TSEInputMode = savedMode;
                        // Tell Java to update its footer UI
                        window.cefQuery && window.cefQuery({request: 'TSE_LANG_SELECT:' + savedMode});
                        console.log("[TSE_REFRESH] Restored input mode: " + savedMode);
                    }

                    console.log("[TSE_REFRESH] Restore completed.");
                    // Clean up
                    sessionStorage.removeItem("TSE_REFRESH_SNAPSHOT");
                    sessionStorage.removeItem("TSE_INPUT_MODE");
                } catch (e) {
                    console.error("[TSE_REFRESH] Restore failed. Snapshot preserved for debugging.", e);
                    window.cefQuery && window.cefQuery({request: 'TSE_REFRESH_FAILED:Restore failed'});
                }
            }, 300); // 300ms delay to ensure elements are parsed
        },

        showOverlay: function() {
            let overlay = document.createElement('div');
            overlay.id = 'tse-refresh-overlay';
            overlay.innerHTML = 'Đang làm mới...';
            overlay.style.cssText = 'position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.85); color:white; z-index:999999; display:flex; align-items:center; justify-content:center; font-size:24px; font-family:"Segoe UI", sans-serif;';
            document.body.appendChild(overlay);
        },

        showError: function(msg) {
            let errorDiv = document.createElement('div');
            errorDiv.innerHTML = msg;
            errorDiv.style.cssText = 'position:fixed; bottom:20px; right:20px; background:#DC2626; color:white; padding:15px 25px; border-radius:8px; z-index:999999; font-family:"Segoe UI", sans-serif; box-shadow:0 4px 12px rgba(0,0,0,0.3);';
            document.body.appendChild(errorDiv);
            setTimeout(() => errorDiv.remove(), 4000);
        }
    };

    console.log("[TSE_REFRESH] TSESafeRefresh ready.");
    
    // Auto-restore if needed when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            window.TSESafeRefresh.restoreIfNeeded();
        });
    } else {
        window.TSESafeRefresh.restoreIfNeeded();
    }
})();
