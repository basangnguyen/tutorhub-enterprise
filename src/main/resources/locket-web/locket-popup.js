(function () {
  'use strict';

  var posts = [];
  var currentIndex = 0;
  var mode = "viewer"; // "viewer" | "cameraPreview" | "draft"
  var slideshowTimer = null;
  var uploadBusy = false;

  /* ===== UTILS ===== */
  function $(id) { return document.getElementById(id); }

  function payloadString(payload) {
    try { return JSON.stringify(payload || {}); } catch (e) { return "{}"; }
  }

  function emit(type, payload) {
    try {
      if (window.bridge && window.bridge.onEvent) {
        window.bridge.onEvent(type, payloadString(payload));
      }
    } catch (e) {
      if (window.console && console.log) { console.log("[LOCKET_POPUP] bridge error", type, e); }
    }
  }

  function escapeHtml(v) {
    return String(v == null ? "" : v)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function cssUrl(v) {
    return String(v == null ? "" : v)
      .replace(/\\/g, "\\\\")
      .replace(/'/g, "\\'")
      .replace(/"/g, "\\\"")
      .replace(/\)/g, "\\)");
  }

  function asArray(v) {
    return Object.prototype.toString.call(v) === "[object Array]" ? v : [];
  }

  function str(v, fallback) {
    var s = String(v == null ? "" : v);
    return s.length > 0 ? s : (fallback || "");
  }

  function num(v, fallback) {
    var n = parseInt(v, 10);
    return isNaN(n) ? (fallback || 0) : n;
  }

  function bool(v, fallback) {
    return v === true || v === false ? v : (fallback === true);
  }

  function initials(name) {
    var parts = str(name, "TH").trim().split(/\s+/);
    var out = "";
    for (var i = 0; i < parts.length && out.length < 2; i++) {
      if (parts[i]) out += parts[i].charAt(0).toUpperCase();
    }
    return out || "TH";
  }

  /* ===== NORMALIZE DATA ===== */
  function normalizeItems(items) {
    var src = asArray(items);
    var result = [];
    for (var i = 0; i < src.length; i++) {
      var item = src[i] || {};
      var author = str(item.authorName, str(item.author, "TutorHub"));
      var imageUrl = str(item.imageUrl, str(item.thumbnailUrl, ""));
      result.push({
        id: str(item.id, "locket-" + (i + 1)),
        imageUrl: imageUrl,
        thumbnailUrl: str(item.thumbnailUrl, imageUrl),
        caption: str(item.caption, "Khoảnh khắc học tập"),
        authorName: author,
        authorAvatar: str(item.authorAvatar, ""),
        authorInitials: str(item.authorInitials, str(item.initials, initials(author))),
        timeText: str(item.timeText, str(item.time, "Vừa xong")),
        likeCount: num(item.likeCount, num(item.likes, 0)),
        commentCount: num(item.commentCount, num(item.comments, 0)),
        likedByMe: bool(item.likedByMe, bool(item.liked, false))
      });
    }
    return result;
  }

  /* ===== STATUS ===== */
  function setStatus(msg, kind) {
    var node = $("statusLine");
    node.textContent = msg || "";
    node.className = "status-line" + (kind ? " " + kind : "");
  }

  /* ===== UPLOAD BUSY ===== */
  function setUploadBusy(busy, message) {
    uploadBusy = busy === true;
    var overlay = $("uploadOverlay");
    overlay.className = uploadBusy ? "upload-overlay active" : "upload-overlay";
    $("uploadText").textContent = message || "Đang đăng ảnh...";
    $("submitButton").disabled = uploadBusy || mode === "cameraPreview";
    $("okButton").disabled = uploadBusy;
    $("imageButton").disabled = uploadBusy;
  }

  /* ===== RENDER AVATAR (viewer header) ===== */
  function renderHeaderAvatar(post) {
    var avatar = $("authorAvatar");
    avatar.className = "avatar";
    avatar.style.backgroundImage = "";
    avatar.textContent = post.authorInitials || initials(post.authorName);
    if (post.authorAvatar) {
      avatar.className = "avatar has-image";
      avatar.style.backgroundImage = "url('" + cssUrl(post.authorAvatar) + "')";
      avatar.textContent = "";
    }
  }

  /* ===== RENDER RECENT LIST (cột trái) ===== */
  function renderRecent() {
    var list = $("recentList");
    if (!posts.length) {
      list.innerHTML = '<div class="recent-empty">Chưa có ảnh nào.</div>';
      return;
    }

    var html = "";
    for (var i = 0; i < posts.length; i++) {
      var post = posts[i];
      var active = (i === currentIndex) ? " active" : "";
      var avatarStyle = "";
      if (post.authorAvatar) {
        avatarStyle = " style=\"background-image:url('" + cssUrl(post.authorAvatar) + "');background-size:cover;\"";
      }
      var avatarContent = post.authorAvatar
        ? ""
        : "<span class=\"recent-avatar-initials\">" + escapeHtml(post.authorInitials || initials(post.authorName)) + "</span>";

      html += ""
        + "<button class=\"recent-item" + active + "\" type=\"button\" data-index=\"" + i + "\">"
        + "  <div class=\"recent-avatar\"" + avatarStyle + ">"
        +      avatarContent
        + "    <span class=\"recent-dot\"></span>"
        + "  </div>"
        + "  <div class=\"recent-info\">"
        + "    <div class=\"recent-name\">" + escapeHtml(post.authorName || "TutorHub") + "</div>"
        + "    <div class=\"recent-time\">" + escapeHtml(post.timeText || "Vừa xong") + "</div>"
        + "  </div>"
        + "</button>";
    }

    list.innerHTML = html;

    var buttons = list.getElementsByClassName("recent-item");
    for (var b = 0; b < buttons.length; b++) {
      buttons[b].onclick = function () {
        currentIndex = parseInt(this.getAttribute("data-index"), 10);
        mode = "viewer";
        render();
      };
    }
  }

  /* ===== RENDER VIEWER (khu giữa, chế độ xem ảnh) ===== */
  function renderViewer() {
    var post = getCurrentPost();
    var viewerImage = $("viewerImage");
    var emptyState = $("emptyState");
    var cameraFrame = $("cameraFrame");
    var panel = $("viewerPanel");

    cameraFrame.className = "camera-frame";
    panel.className = "viewer-panel";
    
    $("submitButton").disabled = uploadBusy;
    $("okLabel").textContent = "OK";
    $("okButton").className = "round-action ok-action";

    if (!post) {
      viewerImage.style.display = "none";
      emptyState.className = "empty-state active";
      $("authorName").textContent = "TutorHub";
      $("timeText").textContent = "";
      $("captionInput").value = "";
      $("prevButton").disabled = true;
      $("nextButton").disabled = true;
      return;
    }

    emptyState.className = "empty-state";
    viewerImage.style.display = "block";
    viewerImage.onerror = function() {
        bridgeLog("[LOCKET_JS][IMAGE_ERROR] Failed to load image: " + viewerImage.src);
        viewerImage.style.display = "none";
        emptyState.className = "empty-state active";
        var esText = emptyState.querySelector("strong");
        var esSub = emptyState.querySelector("span");
        if (esText) esText.textContent = "Không tải được ảnh từ storage";
        if (esSub) esSub.textContent = "Vui lòng thử lại sau";
    };
    viewerImage.src = post.imageUrl || post.thumbnailUrl || "";
    $("authorName").textContent = post.authorName || "TutorHub";
    $("timeText").textContent = post.timeText || "Vừa xong";
    var cCap = post.caption || "";
    if (cCap === "Khoanh khac hoc tap moi") cCap = "";
    $("captionInput").value = cCap;
    $("prevButton").disabled = posts.length <= 1;
    $("nextButton").disabled = posts.length <= 1;
    renderHeaderAvatar(post);
  }

  /* ===== RENDER CAMERA MODE ===== */
  function renderCameraPreview() {
    var panel = $("viewerPanel");
    panel.className = "viewer-panel camera-preview";
    $("viewerImage").style.display = "none";
    $("emptyState").className = "empty-state";
    $("cameraFrame").className = "camera-frame active";
    $("submitButton").disabled = true;
    $("okLabel").textContent = "Chụp";
    $("okButton").className = "round-action ok-action active";
    setStatus("Camera đang bật. Bấm OK lần nữa để chụp.", "");
    
    var cName = window.tutorhubState && window.tutorhubState.currentUserName ? window.tutorhubState.currentUserName : "Tài khoản của bạn";
    var cAvatar = window.tutorhubState && window.tutorhubState.currentUserAvatarBase64 ? window.tutorhubState.currentUserAvatarBase64 : "";
    $("authorName").textContent = cName;
    $("timeText").textContent = "Bây giờ";
    var avatar = $("authorAvatar");
    avatar.className = "avatar";
    avatar.style.backgroundImage = "";
    avatar.textContent = initials(cName);
    if (cAvatar) {
      avatar.className = "avatar has-image";
      avatar.style.backgroundImage = "url('data:image/png;base64," + cssUrl(cAvatar) + "')";
      avatar.textContent = "";
    }
  }

  /* ===== RENDER DRAFT MODE ===== */
  function renderDraft() {
    var post = getCurrentPost();
    var viewerImage = $("viewerImage");
    var emptyState = $("emptyState");
    var cameraFrame = $("cameraFrame");
    var panel = $("viewerPanel");

    cameraFrame.className = "camera-frame";
    panel.className = "viewer-panel draft-mode";
    
    $("submitButton").disabled = uploadBusy;
    $("okLabel").textContent = "OK";
    $("okButton").className = "round-action ok-action";

    if (!post) {
      return;
    }

    emptyState.className = "empty-state";
    viewerImage.style.display = "block";
    viewerImage.src = post.imageUrl || post.thumbnailUrl || "";
    $("authorName").textContent = post.authorName || "TutorHub";
    $("timeText").textContent = post.timeText || "Vừa xong";
    renderHeaderAvatar(post);
    
    // Disable prev/next for draft mode
    $("prevButton").disabled = true;
    $("nextButton").disabled = true;
  }

  /* ===== RENDER (main) ===== */
  function render() {
    if (mode === "cameraPreview") {
      renderCameraPreview();
    } else if (mode === "draft") {
      renderDraft();
    } else {
      renderViewer();
    }
    renderRecent();
  }

  /* ===== GET CURRENT POST ===== */
  function getCurrentPost() {
    if (!posts.length) return null;
    if (currentIndex < 0) currentIndex = posts.length - 1;
    if (currentIndex >= posts.length) currentIndex = 0;
    return posts[currentIndex];
  }

  /* ===== PHOTO NAV ===== */
  function nextPhoto() {
    if (!posts.length) return;
    currentIndex = (currentIndex + 1) % posts.length;
    mode = "viewer";
    render();
    emit("LOCKET_NEXT", { index: currentIndex });
  }

  function prevPhoto() {
    if (!posts.length) return;
    currentIndex = (currentIndex - 1 + posts.length) % posts.length;
    mode = "viewer";
    render();
    emit("LOCKET_PREV", { index: currentIndex });
  }

  /* ===== SLIDESHOW ===== */
  function startSlideshow() {
    stopSlideshow();
    $("playButton").className = "round-action active";
    slideshowTimer = window.setInterval(nextPhoto, 5000);
    setStatus("Đang phát slideshow mỗi 5 giây.", "");
  }

  function stopSlideshow() {
    if (slideshowTimer) {
      window.clearInterval(slideshowTimer);
      slideshowTimer = null;
    }
    $("playButton").className = "round-action";
  }

  function toggleSlideshow() {
    if (slideshowTimer) {
      stopSlideshow();
      setStatus("Đã dừng slideshow.", "");
    } else {
      if (!posts.length) {
        setStatus("Chưa có ảnh để phát slideshow.", "");
        return;
      }
      startSlideshow();
    }
    emit("LOCKET_SLIDESHOW_TOGGLE", { playing: slideshowTimer != null });
  }

  /* ===== CAMERA ===== */
  function startCamera() {
    if (uploadBusy) return;
    stopSlideshow();
    mode = "cameraPreview";
    render();
    emit("LOCKET_CAMERA_START", {});
  }

  function onOk() {
    if (mode === "cameraPreview") {
      emit("LOCKET_CAMERA_CAPTURE", {});
    } else {
      startCamera();
    }
  }

  /* ===== PICKED IMAGE ===== */
  function showPickedImage(dataUrl, fileName) {
    stopSlideshow();
    var preview = {
      id: "preview",
      imageUrl: dataUrl,
      thumbnailUrl: dataUrl,
      caption: "",
      authorName: "Bạn",
      authorInitials: "ME",
      authorAvatar: "",
      timeText: fileName || "Ảnh mới",
      likeCount: 0,
      commentCount: 0,
      likedByMe: false
    };
    posts.unshift(preview);
    currentIndex = 0;
    $("captionInput").value = "";
    setStatus("Ảnh đã sẵn sàng. Nhập caption và bấm Đăng.", "success");
    mode = "draft";
    render();
  }

  /* ===== SUBMIT POST ===== */
  function submitPost() {
    if (uploadBusy) return;
    var caption = $("captionInput").value || "";
    setUploadBusy(true, "Đang upload ảnh...");
    setStatus("Đang đăng Locket...", "");
    emit("LOCKET_POST_SUBMIT", { caption: caption });
  }

  /* ===== REACTION ===== */
  function react(reactionType) {
    var post = getCurrentPost();
    if (!post) return;
    emit("LOCKET_REACTION", { postId: post.id, reactionType: reactionType });
    setStatus("Đã gửi cảm xúc.", "success");
  }

  /* ===== WIRE EVENTS ===== */
  function wire() {
    $("createPostButton").onclick = function () {
      emit("LOCKET_PICK_IMAGE", { source: "create-card" });
    };
    $("imageButton").onclick = function () {
      emit("LOCKET_PICK_IMAGE", { source: "action" });
    };
    $("okButton").onclick = onOk;
    $("submitButton").onclick = submitPost;
    $("prevButton").onclick = prevPhoto;
    $("nextButton").onclick = nextPhoto;
    $("playButton").onclick = toggleSlideshow;
    $("exitButton").onclick = function () {
      stopSlideshow();
      emit("LOCKET_CLOSE", {});
    };
    $("optionsButton").onclick = function () {
      setStatus("Tùy chọn nâng cao sẽ được bổ sung sau.", "");
    };
    $("messageButton").onclick = function () {
      emit("LOCKET_MESSAGE_OPEN", {});
    };

    var reactionBtns = document.getElementsByClassName("reaction");
    for (var i = 0; i < reactionBtns.length; i++) {
      reactionBtns[i].onclick = function () {
        react(this.getAttribute("data-reaction") || "HEART");
      };
    }
  }

  /* ===== PUBLIC API (called from Java) ===== */
  window.TutorHubLocketPopup = {
    setAssets: function(assets) {
      if (!assets) return;
      if (assets.icons) {
        var setSrc = function(sel, val) {
          var el = document.querySelector(sel);
          if (el && val) el.src = val;
        };
        setSrc("#playButton img", assets.icons.play);
        setSrc("#imageButton img", assets.icons.camera);
        setSrc("#optionsButton img", assets.icons.tuychon);
        setSrc("#messageButton img", assets.icons.message);
        setSrc("#exitButton img", assets.icons.close);
        setSrc(".section-label-icon img", assets.icons.clock);
        setSrc(".empty-icon img", assets.icons.camera_empty);
        setSrc("#plusIcon", assets.icons.plus);
      }
      if (assets.reactions) {
        var btns = document.querySelectorAll(".reaction");
        for (var i = 0; i < btns.length; i++) {
          var key = btns[i].getAttribute("data-reaction");
          if (key && assets.reactions[key]) {
            var img = btns[i].querySelector("img");
            if (img) img.src = assets.reactions[key];
          }
        }
      }
    },
    setState: function (state) {
      var safeState = state || {};
      posts = normalizeItems(safeState.items);
      currentIndex = num(safeState.startIndex, 0);
      window.tutorhubState = safeState;
      if (currentIndex < 0 || currentIndex >= posts.length) currentIndex = 0;
      mode = "viewer";
      render();
    },
    onPickedImage: function (dataUrl, fileName) {
      setUploadBusy(false);
      showPickedImage(dataUrl, fileName);
    },
    onCameraFrame: function (dataUrl) {
      if (mode !== "cameraPreview") return;
      var cameraImage = $("cameraImage");
      if (cameraImage) cameraImage.src = dataUrl;
      $("cameraHint").textContent = "Camera đang bật";
    },
    onCameraCaptured: function (dataUrl, fileName) {
      setUploadBusy(false);
      showPickedImage(dataUrl, fileName || "camera.jpg");
    },
    setUploading: function (busy, message) {
      setUploadBusy(busy === true, message);
    },
    setStatus: function (message, kind) {
      setStatus(message, kind);
    },
    onUploadError: function (message) {
      setUploadBusy(false);
      setStatus(message || "Không đăng được ảnh. Vui lòng thử lại.", "error");
    },
    onPostAccepted: function () {
      setUploadBusy(false);
      setStatus("Đã gửi bài đăng. Feed sẽ cập nhật sau khi server xác nhận.", "success");
    }
  };

  window.TutorHubLocketBridgeReady = function () {
    emit("LOCKET_POPUP_READY", {});
  };

  /* ===== INIT ===== */
  function init() {
    wire();
    render();
    if (window.bridge) {
      emit("LOCKET_POPUP_READY", {});
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }

})();
