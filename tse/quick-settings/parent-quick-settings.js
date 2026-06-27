(function () {
    var msg = "[TSE_QS_PARENT_JS] LOADED version=BRIGHTNESS_RESEARCH_FIX";
    try {
        console.log(msg);
        if (window.javaApp && window.javaApp.log) {
            window.javaApp.log(msg);
        }
    } catch (e) {}
})();

function logToJava(msg) {
    try {
        console.log(msg);
        if (window.javaApp && window.javaApp.log) {
            window.javaApp.log(msg);
        }
    } catch (e) {}
}

function clampPercent(value) {
    var n = parseInt(value, 10);
    if (isNaN(n)) {
        return 0;
    }
    return Math.max(0, Math.min(100, n));
}

function stopSliderEvent(event) {
    if (!event) {
        return;
    }
    try {
        if (event.cancelable && event.preventDefault) {
            event.preventDefault();
        }
        if (event.stopPropagation) {
            event.stopPropagation();
        }
        if (event.stopImmediatePropagation) {
            event.stopImmediatePropagation();
        }
    } catch (e) {}
}

function notifyPopupPointerActivity() {
    try {
        if (window.javaApp && window.javaApp.notifyPopupPointerActivity) {
            window.javaApp.notifyPopupPointerActivity();
        }
    } catch (e) {}
}

var sliderState = {
    brightness: {
        mode: "IDLE",
        draftValue: null,
        version: 0,
        lastUserInputAt: 0
    },
    volume: {
        mode: "IDLE",
        draftValue: null,
        muted: false,
        version: 0,
        lastUserInputAt: 0,
        debounceTimer: null
    }
};

var brightnessDragging = false;
var lastBrightnessValue = null;
var lastBrightnessSentValue = null;
var lastBrightnessSentAt = 0;
var brightnessDelegationInstalled = false;
var activeSlider = null;
var activeSliderRect = null;
var activeSliderKind = null;
var lastPointerEventAt = 0;

function elementMatches(el, selector) {
    if (!el) {
        return false;
    }
    var fn = el.matches || el.msMatchesSelector || el.webkitMatchesSelector;
    return fn ? fn.call(el, selector) : false;
}

function closestElement(el, selector) {
    var current = el;
    while (current && current !== document) {
        if (elementMatches(current, selector)) {
            return current;
        }
        current = current.parentNode;
    }
    return null;
}

function isBrightnessSlider(el) {
    return !!el && (
        el.id === "brightnessSlider" ||
        el.id === "brightness-slider" ||
        el.id === "slider-brightness" ||
        el.getAttribute("data-role") === "brightness-slider" ||
        (el.classList && el.classList.contains("brightness-slider")) ||
        el.getAttribute("name") === "brightness"
    );
}

function isVolumeSlider(el) {
    return !!el && (
        el.id === "slider-volume" ||
        el.getAttribute("data-role") === "volume-slider" ||
        (el.classList && el.classList.contains("volume-slider")) ||
        el.getAttribute("name") === "volume"
    );
}

function findBrightnessSlider() {
    return document.getElementById("brightnessSlider") ||
        document.getElementById("brightness-slider") ||
        document.getElementById("slider-brightness") ||
        document.querySelector("[data-role='brightness-slider']") ||
        document.querySelector("input[type='range'].brightness-slider") ||
        document.querySelector("input[type='range'][name='brightness']") ||
        document.querySelector(".brightness-slider");
}

function findSliderFromEvent(event) {
    if (!event || !event.target) {
        return null;
    }

    var slider = closestElement(event.target, "[data-role='brightness-slider'],[data-role='volume-slider'],input[type='range'],.custom-slider,.brightness-slider,.volume-slider");
    if (slider && (isBrightnessSlider(slider) || isVolumeSlider(slider))) {
        return slider;
    }

    if (typeof event.clientX === "number" && typeof event.clientY === "number") {
        return findSliderAtPoint(event.clientX, event.clientY);
    }

    return null;
}

function findSliderAtPoint(x, y) {
    var sliders = [
        findBrightnessSlider(),
        document.getElementById("slider-volume"),
        document.querySelector("[data-role='volume-slider']")
    ];

    for (var i = 0; i < sliders.length; i++) {
        var slider = sliders[i];
        if (!slider || slider.style.pointerEvents === "none" || slider.disabled) {
            continue;
        }
        var rect = slider.getBoundingClientRect();
        if (x >= rect.left - 12 && x <= rect.right + 12 && y >= rect.top - 18 && y <= rect.bottom + 18) {
            return slider;
        }
    }
    return null;
}

function getSliderKind(slider) {
    if (isBrightnessSlider(slider)) {
        return "brightness";
    }
    if (isVolumeSlider(slider)) {
        return "volume";
    }
    return null;
}

function sliderValueFromClientX(slider, clientX) {
    var rect = slider.getBoundingClientRect();
    activeSliderRect = rect;
    var ratio = (clientX - rect.left) / Math.max(1, rect.width);
    return clampPercent(Math.round(ratio * 100));
}

function setSliderUIValue(sliderId, percent) {
    var slider = document.getElementById(sliderId);
    if (!slider) {
        return;
    }

    percent = clampPercent(percent);
    slider.value = percent;
    slider.setAttribute("aria-valuenow", String(percent));
    slider.setAttribute("data-value", String(percent));

    var track = document.getElementById(sliderId + "-track");
    var thumb = document.getElementById(sliderId + "-thumb");
    if (track) {
        track.style.width = percent + "%";
    }
    if (thumb) {
        thumb.style.left = percent + "%";
    }
}

function setActiveSliderValueFromX(clientX) {
    if (!activeSlider) {
        return 0;
    }
    var value = sliderValueFromClientX(activeSlider, clientX);
    setSliderUIValue(activeSlider.id, value);
    return value;
}

function installBrightnessDelegation() {
    if (brightnessDelegationInstalled) {
        logToJava("[TSE_QS_PARENT_JS] brightness delegation already installed");
        return;
    }
    brightnessDelegationInstalled = true;

    document.addEventListener("pointerdown", function (event) {
        lastPointerEventAt = Date.now();
        handleSliderStart(event, event.clientX, event.clientY, "pointerdown");
    }, true);

    document.addEventListener("pointermove", function (event) {
        lastPointerEventAt = Date.now();
        handleSliderMove(event, event.clientX, "pointermove");
    }, true);

    document.addEventListener("pointerup", function (event) {
        lastPointerEventAt = Date.now();
        handleSliderEnd(event, "pointerup");
    }, true);

    document.addEventListener("mousedown", function (event) {
        if (Date.now() - lastPointerEventAt < 250) {
            return;
        }
        handleSliderStart(event, event.clientX, event.clientY, "mousedown");
    }, true);

    document.addEventListener("mousemove", function (event) {
        if (Date.now() - lastPointerEventAt < 250) {
            return;
        }
        handleSliderMove(event, event.clientX, "mousemove");
    }, true);

    document.addEventListener("mouseup", function (event) {
        if (Date.now() - lastPointerEventAt < 250) {
            return;
        }
        handleSliderEnd(event, "mouseup");
    }, true);

    document.addEventListener("touchstart", function (event) {
        if (event.touches && event.touches.length > 0) {
            handleSliderStart(event, event.touches[0].clientX, event.touches[0].clientY, "touchstart");
        }
    }, true);

    document.addEventListener("touchmove", function (event) {
        if (event.touches && event.touches.length > 0) {
            handleSliderMove(event, event.touches[0].clientX, "touchmove");
        }
    }, true);

    document.addEventListener("touchend", function (event) {
        handleSliderEnd(event, "touchend");
    }, true);

    document.addEventListener("input", function (event) {
        var slider = findSliderFromEvent(event);
        if (!slider || !isBrightnessSlider(slider)) {
            return;
        }
        stopSliderEvent(event);
        var percent = clampPercent(slider.value);
        lastBrightnessValue = percent;
        logToJava("[TSE_QS_PARENT_JS] brightness input percent=" + percent);
        onBrightnessInput(percent);
    }, true);

    document.addEventListener("change", function (event) {
        var slider = findSliderFromEvent(event);
        if (!slider || !isBrightnessSlider(slider)) {
            return;
        }
        stopSliderEvent(event);
        var percent = clampPercent(slider.value);
        lastBrightnessValue = percent;
        logToJava("[TSE_QS_PARENT_JS] brightness change percent=" + percent);
        onBrightnessChange(percent);
    }, true);

    logToJava("[TSE_QS_PARENT_JS] brightness delegation installed");
}

function handleSliderStart(event, clientX, clientY, source) {
    var slider = findSliderFromEvent(event) || findSliderAtPoint(clientX, clientY);
    if (!slider || slider.style.pointerEvents === "none" || slider.disabled) {
        return;
    }

    var kind = getSliderKind(slider);
    if (!kind) {
        return;
    }

    stopSliderEvent(event);
    notifyPopupPointerActivity();

    activeSlider = slider;
    activeSliderRect = slider.getBoundingClientRect();
    activeSliderKind = kind;

    var value = setActiveSliderValueFromX(clientX);

    if (kind === "brightness") {
        brightnessDragging = true;
        lastBrightnessValue = value;
        sliderState.brightness.mode = "DRAGGING";
        logToJava("[TSE_QS_PARENT_JS] brightness pointerdown value=" + value + " source=" + source);
        onBrightnessInput(value);
    } else if (kind === "volume") {
        sliderState.volume.mode = "DRAGGING";
        logToJava("[TSE_QS_PARENT_JS] volume pointerdown value=" + value + " source=" + source);
        onVolumeInput(value);
    }
}

function handleSliderMove(event, clientX, source) {
    if (!activeSlider) {
        return;
    }

    stopSliderEvent(event);
    notifyPopupPointerActivity();

    var value = setActiveSliderValueFromX(clientX);
    if (activeSliderKind === "brightness") {
        lastBrightnessValue = value;
        onBrightnessInput(value);
    } else if (activeSliderKind === "volume") {
        onVolumeInput(value);
    }
}

function handleSliderEnd(event, source) {
    if (!activeSlider) {
        return;
    }

    stopSliderEvent(event);
    notifyPopupPointerActivity();

    var value = clampPercent(activeSlider.value);
    if (activeSliderKind === "brightness") {
        if (typeof lastBrightnessValue === "number" && !isNaN(lastBrightnessValue)) {
            value = clampPercent(lastBrightnessValue);
        }
        logToJava("[TSE_QS_PARENT_JS] brightness pointerup commit percent=" + value + " source=" + source);
        onBrightnessChange(value);
        brightnessDragging = false;
    } else if (activeSliderKind === "volume") {
        logToJava("[TSE_QS_PARENT_JS] volume pointerup commit percent=" + value + " source=" + source);
        onVolumeChange(value);
    }

    activeSlider = null;
    activeSliderRect = null;
    activeSliderKind = null;
}

function onBrightnessInput(val) {
    var percent = clampPercent(val);
    sliderState.brightness.mode = "DRAGGING";
    sliderState.brightness.lastUserInputAt = Date.now();
    lastBrightnessValue = percent;
    notifyPopupPointerActivity();

    var vBright = document.getElementById("brightness-val");
    if (vBright) {
        vBright.innerText = percent + "%";
    }
    setSliderUIValue("slider-brightness", percent);
    logToJava("[TSE_QS_PARENT_JS] brightness input percent=" + percent);
}

function onBrightnessChange(val) {
    var percent = clampPercent(val);
    sliderState.brightness.mode = "COMMIT_PENDING";
    sliderState.brightness.lastUserInputAt = Date.now();
    sliderState.brightness.version++;
    lastBrightnessValue = percent;

    logToJava("[TSE_QS_PARENT_JS] brightness change percent=" + percent + " version=" + sliderState.brightness.version);
    sendBrightnessToJava(percent);
    sliderState.brightness.mode = "COMMITTED";
}

function sendBrightnessToJava(percent) {
    percent = clampPercent(percent);
    var now = Date.now();
    if (lastBrightnessSentValue === percent && now - lastBrightnessSentAt < 250) {
        logToJava("[TSE_QS_PARENT_JS] skip duplicate brightness command percent=" + percent);
        return;
    }
    lastBrightnessSentValue = percent;
    lastBrightnessSentAt = now;

    if (!window.javaApp || !window.javaApp.setBrightnessCommand) {
        logToJava("[TSE_QS_PARENT_JS] javaApp.setBrightnessCommand missing");
        return;
    }

    var payload = JSON.stringify({
        percent: percent,
        requestId: "parent-brightness-" + Date.now()
    });

    logToJava("[TSE_QS_PARENT_JS] send setBrightnessCommand payload=" + payload);
    window.javaApp.setBrightnessCommand(payload);
}

function debugBrightnessSlider() {
    var slider = findBrightnessSlider();

    if (!slider) {
        logToJava("[TSE_QS_PARENT_JS] brightness slider NOT FOUND");
        return;
    }

    var rect = slider.getBoundingClientRect();
    var style = window.getComputedStyle(slider);
    var cx = rect.left + rect.width / 2;
    var cy = rect.top + rect.height / 2;
    var topEl = document.elementFromPoint(cx, cy);
    var topDesc = "null";
    if (topEl) {
        topDesc = topEl.tagName + "#" + topEl.id + "." + topEl.className;
        if (topEl === slider) {
            topDesc += "(slider)";
        } else if (slider.contains && slider.contains(topEl)) {
            topDesc += "(inside-slider)";
        }
    }

    logToJava("[TSE_QS_PARENT_JS] slider debug"
        + " id=" + slider.id
        + " class=" + slider.className
        + " disabled=" + (!!slider.disabled)
        + " pointerEvents=" + style.pointerEvents
        + " rect=" + Math.round(rect.x) + "," + Math.round(rect.y) + "," + Math.round(rect.width) + "," + Math.round(rect.height)
        + " topElement=" + topDesc
    );
}

window.updateState = function(jsonStr) {
    installBrightnessDelegation();

    if (brightnessDragging) {
        logToJava("[TSE_QS_PARENT_JS] skip full render while brightness dragging");
        return;
    }

    try {
        var data = JSON.parse(jsonStr);
        logToJava("[TSE_QS_PARENT_JS] updateState brightnessSupported=" + data.brightnessSupported
            + " brightnessPercent=" + data.brightnessPercent
            + " brightnessWritable=" + data.brightnessWritable);

        var wifi = document.getElementById("wifi-val");
        if (wifi) {
            wifi.innerText = data.wifiSsid || data.wifiText || "No connection";
        }

        var vBatt = document.getElementById("battery-val");
        if (vBatt) {
            if (data.hasBattery) {
                vBatt.innerText = data.batteryPercent + "% - " + (data.batteryCharging ? "Charging" : "On battery");
            } else {
                vBatt.innerText = "AC power";
            }
        }

        var sBright = findBrightnessSlider();
        var vBright = document.getElementById("brightness-val");
        if (sBright) {
            if (data.brightnessSupported) {
                sBright.style.pointerEvents = "auto";
                sBright.style.opacity = "1";
                sBright.disabled = false;

                var backendVersion = data.brightnessVersion || 0;
                var canUpdate = sliderState.brightness.mode === "IDLE" ||
                    sliderState.brightness.mode === "SYSTEM_SYNC" ||
                    (sliderState.brightness.mode === "COMMITTED" && backendVersion >= sliderState.brightness.version);

                if (canUpdate) {
                    sliderState.brightness.mode = "IDLE";
                    setSliderUIValue("slider-brightness", data.brightnessPercent);
                    if (vBright) {
                        vBright.innerText = clampPercent(data.brightnessPercent) + "%";
                    }
                }
            } else {
                sBright.style.pointerEvents = "none";
                sBright.style.opacity = "0.5";
                sBright.disabled = true;
                setSliderUIValue("slider-brightness", 0);
                if (vBright) {
                    vBright.innerText = "N/A";
                }
            }
        }

        var sVol = document.getElementById("slider-volume");
        var vVol = document.getElementById("volume-val");
        if (sVol) {
            if (data.volumeSupported) {
                sVol.style.pointerEvents = "auto";
                sVol.style.opacity = "1";
                sVol.disabled = false;

                var backendVolVersion = data.volumeVersion || 0;
                var volCanUpdate = sliderState.volume.mode === "IDLE" ||
                    sliderState.volume.mode === "SYSTEM_SYNC" ||
                    (sliderState.volume.mode === "COMMITTED" && backendVolVersion >= sliderState.volume.version);

                if (volCanUpdate) {
                    sliderState.volume.mode = "IDLE";
                    sliderState.volume.muted = data.volumeMuted || data.volumePercent === 0;
                    sliderState.volume.draftValue = sliderState.volume.muted ? 0 : clampPercent(data.volumePercent);
                    setSliderUIValue("slider-volume", sliderState.volume.draftValue);
                    syncVolumeUI();
                }
            } else {
                sVol.style.pointerEvents = "none";
                sVol.style.opacity = "0.5";
                sVol.disabled = true;
                setSliderUIValue("slider-volume", 0);
                if (vVol) {
                    vVol.innerText = "N/A";
                }
            }
        }
    } catch (e) {
        logToJava("[TSE_QS_PARENT_JS] updateState error: " + e.message);
    }

    installBrightnessDelegation();
    debugBrightnessSlider();
};

function onVolumeInput(val) {
    var currentVol = clampPercent(val);
    sliderState.volume.mode = "DRAGGING";
    sliderState.volume.lastUserInputAt = Date.now();
    sliderState.volume.draftValue = currentVol;
    sliderState.volume.muted = currentVol === 0;

    setSliderUIValue("slider-volume", currentVol);
    syncVolumeUI();

    if (sliderState.volume.debounceTimer) {
        clearTimeout(sliderState.volume.debounceTimer);
    }
    sliderState.volume.debounceTimer = setTimeout(function() {
        if (window.javaApp && window.javaApp.setVolumeCommand) {
            sliderState.volume.version++;
            window.javaApp.setVolumeCommand(JSON.stringify({
                percent: currentVol,
                requestId: "vol-drag-" + Date.now()
            }));
        }
    }, 150);
}

function onVolumeChange(val) {
    var currentVol = clampPercent(val);
    sliderState.volume.mode = "COMMIT_PENDING";
    sliderState.volume.lastUserInputAt = Date.now();
    sliderState.volume.draftValue = currentVol;
    sliderState.volume.muted = currentVol === 0;

    if (sliderState.volume.debounceTimer) {
        clearTimeout(sliderState.volume.debounceTimer);
    }

    sliderState.volume.version++;
    if (window.javaApp && window.javaApp.setVolumeCommand) {
        window.javaApp.setVolumeCommand(JSON.stringify({
            percent: currentVol,
            requestId: "vol-commit-" + Date.now()
        }));
    }
    syncVolumeUI();
    sliderState.volume.mode = "COMMITTED";
}

function toggleMute() {
    sliderState.volume.mode = "COMMIT_PENDING";
    sliderState.volume.lastUserInputAt = Date.now();

    if (sliderState.volume.muted) {
        sliderState.volume.muted = false;
        if (sliderState.volume.draftValue === 0 || sliderState.volume.draftValue === null) {
            sliderState.volume.draftValue = 30;
        }
    } else {
        sliderState.volume.muted = true;
        sliderState.volume.draftValue = 0;
    }

    setSliderUIValue("slider-volume", sliderState.volume.draftValue);
    sliderState.volume.version++;
    if (window.javaApp && window.javaApp.setMutedCommand) {
        window.javaApp.setMutedCommand(JSON.stringify({
            muted: sliderState.volume.muted,
            requestId: "mute-" + Date.now()
        }));
    }
    syncVolumeUI();
    sliderState.volume.mode = "COMMITTED";
}

function syncVolumeUI() {
    var vVol = document.getElementById("volume-val");
    var iconOn = document.getElementById("icon-vol-on");
    var iconOff = document.getElementById("icon-vol-off");
    var currentVol = clampPercent(sliderState.volume.draftValue || 0);

    setSliderUIValue("slider-volume", currentVol);

    if (sliderState.volume.muted || currentVol === 0) {
        if (vVol) {
            vVol.innerText = "Muted";
        }
        if (iconOn) {
            iconOn.style.display = "none";
        }
        if (iconOff) {
            iconOff.style.display = "block";
        }
    } else {
        if (vVol) {
            vVol.innerText = currentVol + "%";
        }
        if (iconOn) {
            iconOn.style.display = "block";
        }
        if (iconOff) {
            iconOff.style.display = "none";
        }
    }
}

document.addEventListener("contextmenu", function(event) {
    event.preventDefault();
});

function pollState() {
    installBrightnessDelegation();

    if (window.javaApp && window.javaApp.getSnapshotJson) {
        try {
            var jsonStr = window.javaApp.getSnapshotJson();
            logToJava("[TSE_QS_PARENT_JS] pollState got jsonStr length=" + (jsonStr ? jsonStr.length : 0));
            if (jsonStr && jsonStr !== "{}") {
                window.updateState(jsonStr);
            }
        } catch (e) {
            logToJava("[TSE_QS_PARENT_JS] pollState error: " + e.message);
        }
    } else {
        logToJava("[TSE_QS_PARENT_JS] pollState: javaApp=" + !!window.javaApp
            + " getSnapshotJson=" + (window.javaApp ? typeof window.javaApp.getSnapshotJson : "N/A"));
    }

    debugBrightnessSlider();
}

function onAppReady() {
    logToJava("[TSE_QS_PARENT_JS] onAppReady called");
    installBrightnessDelegation();
    pollState();
    debugBrightnessSlider();

    document.addEventListener("mousedown", function(e) {
        logToJava("[TSE_QS_PARENT_JS] GLOBAL mousedown target="
            + (e.target ? e.target.tagName : "null")
            + " id=" + (e.target ? e.target.id : "null")
            + " x=" + e.clientX
            + " y=" + e.clientY);
    }, true);
}
