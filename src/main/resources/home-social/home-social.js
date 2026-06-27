(function () {
  var fallbackBanners = [
    {
      id: "banner-1",
      imageUrl: "../images/slide1.png",
      kicker: "TutorHub Enterprise",
      title: "Explore Dream Platform",
      subtitle: "Lop live, lich hoc va khoanh khac hoc tap duoc gom gon trong mot bang tin.",
      cta: "Kham pha ngay"
    },
    {
      id: "banner-2",
      imageUrl: "../images/slide2.png",
      kicker: "Live Classroom",
      title: "Lop hoc thong minh hon",
      subtitle: "Theo doi lop dang dien ra, lich sap toi va nhung noi dung noi bat cua lop.",
      cta: "Mo lop hoc"
    },
    {
      id: "banner-3",
      imageUrl: "../images/slide3.png",
      kicker: "Study Moments",
      title: "Giu lai buoi hoc dang nho",
      subtitle: "Locket luu lai khoanh khac hoc tap bang anh, cam xuc va tin nhan.",
      cta: "Xem Locket"
    }
  ];

  var fallbackLocketPosts = [
    {
      id: "locket-1",
      author: "Nguyen Ngoc Le Vy",
      initials: "LV",
      time: "2 gio truoc",
      caption: "Buoi hoc hom nay that hieu qua",
      imageUrl: "../images/general/general1.png",
      likes: 128,
      comments: 24,
      liked: true
    },
    {
      id: "locket-2",
      author: "Minh Anh",
      initials: "MA",
      time: "1 ngay truoc",
      caption: "Hoc tieng Anh moi ngay",
      imageUrl: "../images/english/english1.jpg",
      likes: 96,
      comments: 18,
      liked: false
    },
    {
      id: "locket-3",
      author: "Gia Han",
      initials: "GH",
      time: "2 ngay truoc",
      caption: "Hoa hoc that thu vi",
      imageUrl: "../images/chemistry/chemistry1.jpg",
      likes: 104,
      comments: 20,
      liked: true
    },
    {
      id: "locket-4",
      author: "Quang Huy",
      initials: "QH",
      time: "3 ngay truoc",
      caption: "Giai bai tap toan nang cao",
      imageUrl: "../images/math/math1.jpg",
      likes: 88,
      comments: 16,
      liked: false
    },
    {
      id: "locket-5",
      author: "Bao Tran",
      initials: "BT",
      time: "4 ngay truoc",
      caption: "Co gang tung ngay de dat muc tieu",
      imageUrl: "../images/IELTS/IELTS1.jpg",
      likes: 112,
      comments: 22,
      liked: false
    }
  ];

  var banners = fallbackBanners.slice();
  var locketPosts = fallbackLocketPosts.slice();
  var locketCanPost = true;
  var currentBanner = 0;
  var autoSlideTimer = null;
  var locketState = "ready";
  var initialized = false;

  function payloadString(payload) {
    try {
      return JSON.stringify(payload || {});
    } catch (err) {
      return "{}";
    }
  }

  function bridgeLog(message) {
    try {
      if (window.javaApp && window.javaApp.log) {
        window.javaApp.log(String(message));
      }
    } catch (err) {
      if (window.console && console.log) {
        console.log(message);
      }
    }
  }

  function sendEvent(type, payload) {
    var json = payloadString(payload);
    try {
      if (window.javaApp && window.javaApp.onEvent) {
        window.javaApp.onEvent(type, json);
      }
    } catch (err) {
      bridgeLog("Bridge event failed: " + type + " " + err.message);
    }
  }

  function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function safeCssUrl(value) {
    return String(value == null ? "" : value)
      .replace(/\\/g, "\\\\")
      .replace(/'/g, "\\'")
      .replace(/"/g, "\\\"")
      .replace(/\)/g, "\\)");
  }

  function asArray(value) {
    if (Object.prototype.toString.call(value) === "[object Array]") {
      return value;
    }
    return [];
  }

  function stringValue(value, fallback) {
    if (value == null) {
      return fallback || "";
    }
    var text = String(value);
    return text.length ? text : (fallback || "");
  }

  function numberValue(value, fallback) {
    var number = parseInt(value, 10);
    if (isNaN(number)) {
      return fallback || 0;
    }
    return number;
  }

  function booleanValue(value, fallback) {
    if (value === true || value === false) {
      return value;
    }
    return fallback === true;
  }

  function initialsFromName(name) {
    var parts = stringValue(name, "TH").split(/\s+/);
    var out = "";
    for (var i = 0; i < parts.length && out.length < 2; i++) {
      if (parts[i]) {
        out += parts[i].charAt(0).toUpperCase();
      }
    }
    return out || "TH";
  }

  function normalizeBanners(items) {
    var source = asArray(items);
    var normalized = [];
    for (var i = 0; i < source.length; i++) {
      var item = source[i] || {};
      normalized.push({
        id: stringValue(item.id, "banner-" + (i + 1)),
        imageUrl: stringValue(item.imageUrl, ""),
        kicker: stringValue(item.kicker, "TutorHub Enterprise"),
        title: stringValue(item.title, "TutorHub Enterprise"),
        subtitle: stringValue(item.subtitle, "Bang tin hoc tap thong minh cho giao vien va hoc vien."),
        cta: stringValue(item.ctaText, stringValue(item.cta, "Kham pha ngay"))
      });
    }
    return normalized.length ? normalized : fallbackBanners.slice();
  }

  function normalizeLocketItems(items) {
    var source = asArray(items);
    var normalized = [];
    for (var i = 0; i < source.length; i++) {
      var item = source[i] || {};
      var author = stringValue(item.authorName, stringValue(item.author, "TutorHub"));
      var imageUrl = stringValue(item.thumbnailUrl, stringValue(item.imageUrl, ""));
      normalized.push({
        id: stringValue(item.id, "locket-" + (i + 1)),
        author: author,
        authorAvatar: stringValue(item.authorAvatar, ""),
        initials: stringValue(item.authorInitials, stringValue(item.initials, initialsFromName(author))),
        time: stringValue(item.timeText, stringValue(item.time, "Vua xong")),
        caption: stringValue(item.caption, ""),
        imageUrl: imageUrl,
        likes: numberValue(item.likeCount, numberValue(item.likes, 0)),
        comments: numberValue(item.commentCount, numberValue(item.comments, 0)),
        liked: booleanValue(item.likedByMe, booleanValue(item.liked, false)),
        canDelete: booleanValue(item.canDelete, false)
      });
    }
    return normalized;
  }

  function applyHomeState(state) {
    try {
      var safeState = state || {};
      banners = normalizeBanners(safeState.banners);
      locketPosts = normalizeLocketItems(safeState.locketItems);
      locketCanPost = safeState.locketCanPost !== false;
      locketState = "ready";
      if (currentBanner >= banners.length) {
        currentBanner = 0;
      }
      if (!initialized) {
        return;
      }
      renderBanners();
      renderLocket();
      startAutoSlide();
      sendEvent("HOME_SOCIAL_STATE_APPLIED", {
        bannerCount: banners.length,
        locketCount: locketPosts.length,
        locketCanPost: locketCanPost
      });
    } catch (err) {
      bridgeLog("Home social setState failed: " + err.message);
      banners = fallbackBanners.slice();
      locketPosts = [];
      locketCanPost = true;
      if (initialized) {
        renderBanners();
        renderLocket();
      }
    }
  }

  function layeredImage(url, gradientIndex) {
    var safe = safeCssUrl(url);
    var gradient;
    if (gradientIndex === 2) {
      gradient = "linear-gradient(135deg, rgba(15,118,110,.74), rgba(37,99,235,.38))";
    } else if (gradientIndex === 3) {
      gradient = "linear-gradient(135deg, rgba(37,99,235,.62), rgba(139,92,246,.34))";
    } else {
      gradient = "linear-gradient(135deg, rgba(18,130,189,.64), rgba(37,99,235,.28))";
    }
    if (!safe) {
      return gradient;
    }
    return "url('" + safe + "'), " + gradient;
  }

  function renderAvatar(post) {
    if (post.authorAvatar) {
      return "<div class=\"avatar avatar-image\" style=\"background-image:url('" + safeCssUrl(post.authorAvatar) + "')\"></div>";
    }
    return "<div class=\"avatar\">" + escapeHtml(post.initials) + "</div>";
  }

  function renderBanners() {
    var viewport = document.getElementById("bannerViewport");
    var dots = document.getElementById("bannerDots");
    var html = "";
    var dotHtml = "";

    for (var i = 0; i < banners.length; i++) {
      var item = banners[i];
      var activeClass = i === currentBanner ? " active" : "";
      html += ""
        + "<article class=\"banner-slide fallback-gradient-" + ((i % 3) + 1) + activeClass + "\" "
        + "data-index=\"" + i + "\" "
        + "style=\"background-image: " + layeredImage(item.imageUrl, (i % 3) + 1) + ";\">"
        + "  <div class=\"banner-copy\">"
        + "    <div class=\"banner-kicker\">" + escapeHtml(item.kicker) + "</div>"
        + "    <h1 class=\"banner-title\">" + escapeHtml(item.title) + "</h1>"
        + "  </div>"
        + "</article>";

      dotHtml += "<button class=\"carousel-dot" + activeClass + "\" data-index=\"" + i + "\" type=\"button\" aria-label=\"Go to banner " + (i + 1) + "\"></button>";
    }

    viewport.innerHTML = html;
    dots.innerHTML = dotHtml;

    var slides = viewport.getElementsByClassName("banner-slide");
    for (var s = 0; s < slides.length; s++) {
      slides[s].onclick = function () {
        var idx = parseInt(this.getAttribute("data-index"), 10);
        sendEvent("HOME_BANNER_CLICK", banners[idx]);
      };
    }

    var dotButtons = dots.getElementsByClassName("carousel-dot");
    for (var d = 0; d < dotButtons.length; d++) {
      dotButtons[d].onclick = function (event) {
        event.stopPropagation();
        setBanner(parseInt(this.getAttribute("data-index"), 10));
      };
    }
  }

  function setBanner(index) {
    if (index < 0) {
      index = banners.length - 1;
    }
    if (index >= banners.length) {
      index = 0;
    }
    currentBanner = index;
    renderBanners();
  }

  function startAutoSlide() {
    stopAutoSlide();
    autoSlideTimer = window.setInterval(function () {
      setBanner(currentBanner + 1);
    }, 5200);
  }

  function stopAutoSlide() {
    if (autoSlideTimer) {
      window.clearInterval(autoSlideTimer);
      autoSlideTimer = null;
    }
  }

  function renderLoadingState(rail) {
    var html = "";
    for (var i = 0; i < 3; i++) {
      html += ""
        + "<div class=\"locket-skeleton\">"
        + "  <div class=\"skeleton-image\"></div>"
        + "  <div class=\"skeleton-line\"></div>"
        + "  <div class=\"skeleton-line short\"></div>"
        + "</div>";
    }
    rail.innerHTML = html;
  }

  function renderStateCard(rail, title, copy) {
    rail.innerHTML = ""
      + "<div class=\"locket-state-card\">"
      + "  <div class=\"locket-state-title\">" + escapeHtml(title) + "</div>"
      + "  <div class=\"locket-state-copy\">" + escapeHtml(copy) + "</div>"
      + "</div>";
  }

  function renderLocket() {
    var rail = document.getElementById("locketRail");
    var html = "";

    if (locketState === "loading") {
      renderLoadingState(rail);
      updateRailControls();
      return;
    }

    if (locketState === "error") {
      renderStateCard(rail, "Khong tai duoc Locket", "Vui long thu lai sau it phut.");
      updateRailControls();
      return;
    }

    if (!locketPosts || locketPosts.length === 0) {
      renderStateCard(rail, "Chua co khoanh khac nao", "Anh Locket moi se hien tai day khi ban chia se.");
      updateRailControls();
      return;
    }

    for (var i = 0; i < locketPosts.length; i++) {
      var post = locketPosts[i];
      var likedClass = post.liked ? " liked" : "";
        var heartIconSrc = post.liked ? "../images/icon_svg/heart-fill.svg" : "../images/icon_svg/heart.svg";
        var heartIconStr = "<img src='" + heartIconSrc + "' width='18' height='18' style='vertical-align: middle; margin-right: 4px; opacity: 0.8;'>";
        var commentIconStr = "<img src='../images/icon_svg/message-circle.svg' width='18' height='18' style='vertical-align: middle; margin-right: 4px; opacity: 0.8;'>";

        html += ""
        + "<article class=\"locket-card\" data-id=\"" + escapeHtml(post.id) + "\">"
        + "  <div class=\"locket-image fallback-gradient-" + ((i % 3) + 1) + "\" style=\"background-image: " + layeredImage(post.imageUrl, (i % 3) + 1) + ";\">"
        + "    <div class=\"locket-author\">"
        + "      " + renderAvatar(post)
        + "      <div class=\"author-copy\">"
        + "        <div class=\"author-name\">" + escapeHtml(post.author) + "</div>"
        + "        <div class=\"post-time\">" + escapeHtml(post.time) + "</div>"
        + "      </div>"
        + "    </div>"
        + "    <div class=\"locket-caption\">" + escapeHtml(post.caption) + "</div>"
        + "  </div>"
        + "  <footer class=\"locket-footer\">"
        + "    <div class=\"metric-group\">"
        + "      <button class=\"metric-button heart-button" + likedClass + "\" type=\"button\" data-id=\"" + escapeHtml(post.id) + "\">" + heartIconStr + "<span>" + post.likes + "</span></button>"
        + "      <button class=\"metric-button comment-button\" type=\"button\" data-id=\"" + escapeHtml(post.id) + "\">" + commentIconStr + "<span>" + post.comments + "</span></button>"
        + "    </div>"
        + "  </footer>"
        + "</article>";
    }

    if (locketCanPost) {
      html += ""
        + "<button class=\"add-locket-card\" id=\"addLocket\" type=\"button\">"
        + "  <span class=\"add-icon\">+</span>"
        + "  <span class=\"add-title\">Dang anh Locket</span>"
        + "  <span class=\"add-subtitle\">Chon anh se duoc mo o phase sau</span>"
        + "</button>";
    }

    rail.innerHTML = html;

    for (var i = 0; i < locketPosts.length; i++) {
      var p = locketPosts[i];
      if (p.imageUrl) {
        (function(post) {
          var img = new Image();
          img.onerror = function() {
            bridgeLog("[LOCKET_JS][IMAGE_ERROR] Failed to load feed image: " + post.imageUrl);
            var card = document.querySelector('.locket-card[data-id="' + escapeHtml(post.id) + '"]');
            if (card) {
              var imgDiv = card.querySelector('.locket-image');
              if (imgDiv && !imgDiv.querySelector('.image-error-text')) {
                var errText = document.createElement('div');
                errText.className = 'image-error-text';
                errText.textContent = 'Không tải được ảnh từ storage';
                errText.style.position = 'absolute';
                errText.style.top = '50%';
                errText.style.left = '50%';
                errText.style.transform = 'translate(-50%, -50%)';
                errText.style.color = '#dc2626';
                errText.style.fontSize = '12px';
                errText.style.fontWeight = '700';
                errText.style.padding = '4px 8px';
                errText.style.background = 'rgba(255,255,255,0.9)';
                errText.style.borderRadius = '6px';
                errText.style.textAlign = 'center';
                errText.style.pointerEvents = 'none';
                imgDiv.appendChild(errText);
              }
            }
          };
          img.src = post.imageUrl;
        })(p);
      }
    }

    var cards = rail.getElementsByClassName("locket-card");
    for (var c = 0; c < cards.length; c++) {
      cards[c].onclick = function () {
        var id = this.getAttribute("data-id");
        sendEvent("LOCKET_VIEW_OPEN", findPost(id));
      };
    }

    var hearts = rail.getElementsByClassName("heart-button");
    for (var h = 0; h < hearts.length; h++) {
      hearts[h].onclick = function (event) {
        event.stopPropagation();
        var id = this.getAttribute("data-id");
        toggleLike(id);
      };
    }

    var comments = rail.getElementsByClassName("comment-button");
    for (var cm = 0; cm < comments.length; cm++) {
      comments[cm].onclick = function (event) {
        event.stopPropagation();
        var id = this.getAttribute("data-id");
        currentCommentPostId = id;
        sendEvent("LOCKET_COMMENT_OPEN", findPost(id));
      };
    }

    var addLocket = document.getElementById("addLocket");
    if (addLocket) {
      addLocket.onclick = function () {
        sendEvent("LOCKET_CREATE_OPEN", { source: "add-card" });
      };
    }

    window.setTimeout(updateRailControls, 0);
  }

  function findPost(id) {
    for (var i = 0; i < locketPosts.length; i++) {
      if (locketPosts[i].id === id) {
        return locketPosts[i];
      }
    }
    return { id: id };
  }

  function toggleLike(id) {
    var post = findPost(id);
    if (!post || !post.id) {
      return;
    }
    post.liked = !post.liked;
    post.likes += post.liked ? 1 : -1;
    if (post.likes < 0) {
      post.likes = 0;
    }
    renderLocket();
    sendEvent("LOCKET_POST_REACT", {
      id: post.id,
      liked: post.liked,
      likes: post.likes
    });
  }

  function getRailStep() {
    var rail = document.getElementById("locketRail");
    var firstCard = rail.getElementsByClassName("locket-card")[0] || rail.getElementsByClassName("add-locket-card")[0];
    if (!firstCard) {
      return 282;
    }
    return firstCard.offsetWidth + 14;
  }

  function scrollLocket(direction) {
    var rail = document.getElementById("locketRail");
    var step = getRailStep();
    var current = rail.scrollLeft;
    var target = Math.round((current + direction * step) / step) * step;
    if (target < 0) {
      target = 0;
    }
    rail.scrollLeft = target;
    window.setTimeout(updateRailControls, 180);
  }

  function updateRailControls() {
    var rail = document.getElementById("locketRail");
    var prev = document.getElementById("locketPrev");
    var next = document.getElementById("locketNext");
    if (!rail || !prev || !next) {
      return;
    }
    var canScroll = rail.scrollWidth > rail.clientWidth + 4;
    prev.className = prev.className.replace(" is-hidden", "").replace(" is-disabled", "");
    next.className = next.className.replace(" is-hidden", "").replace(" is-disabled", "");
    if (!canScroll) {
      prev.className += " is-hidden";
      next.className += " is-hidden";
      return;
    }
    if (rail.scrollLeft <= 2) {
      prev.className += " is-disabled";
    }
    if (rail.scrollLeft + rail.clientWidth >= rail.scrollWidth - 4) {
      next.className += " is-disabled";
    }
  }

  function wireControls() {
    document.getElementById("bannerPrev").onclick = function () {
      setBanner(currentBanner - 1);
      startAutoSlide();
    };
    document.getElementById("bannerNext").onclick = function () {
      setBanner(currentBanner + 1);
      startAutoSlide();
    };

    var hero = document.getElementsByClassName("hero-carousel")[0];
    hero.onmouseenter = stopAutoSlide;
    hero.onmouseleave = startAutoSlide;

    document.getElementById("locketPrev").onclick = function () {
      scrollLocket(-1);
    };
    document.getElementById("locketNext").onclick = function () {
      scrollLocket(1);
    };
    document.getElementById("locketSeeAll").onclick = function () {
      sendEvent("LOCKET_VIEW_OPEN", { source: "see-all" });
    };
    document.getElementById("locketRail").onscroll = updateRailControls;
    window.onresize = updateRailControls;
  }

  window.TutorHubHomeSocial = {
    setState: function (newState) {
      if (!newState) return;
      var safeState = typeof newState === "string" ? JSON.parse(newState) : newState;
      applyHomeState(safeState);
    },
    setStateBase64: function (base64Str) {
      try {
          var decodedStr = decodeURIComponent(escape(window.atob(base64Str)));
          var safeState = JSON.parse(decodedStr);
          applyHomeState(safeState);
      } catch (err) {
          bridgeLog("Home social setStateBase64 failed: " + err.message);
      }
    }
  };

  window.TutorHubHomeSocialBridgeReady = function () {
    sendEvent("HOME_SOCIAL_READY", {
      bannerCount: banners.length,
      locketCount: locketPosts.length
    });
  };

  window.TutorHubHomeSocialMockState = function (state) {
    locketState = state || "ready";
    renderLocket();
  };

  var currentCommentPostId = null;

  window.updateLocketReactionBase64 = function(b64) {
      try {
          var payload = JSON.parse(decodeURIComponent(escape(window.atob(b64))));
          var post = findPost(payload.postId);
          if (post && post.id) {
              post.liked = payload.reacted;
              renderLocket();
          }
      } catch (err) {}
  };

  window.updateLocketCommentsBase64 = function(b64) {
      try {
          var comments = JSON.parse(decodeURIComponent(escape(window.atob(b64))));
          renderCommentModal(comments);
      } catch (err) {}
  };

  window.addLocketCommentBase64 = function(b64) {
      try {
          var comment = JSON.parse(decodeURIComponent(escape(window.atob(b64))));
          appendCommentToModal(comment);
          
          var post = findPost(comment.postId || currentCommentPostId);
          if (post && post.id) {
              post.comments = (post.comments || 0) + 1;
              renderLocket();
          }
          
          var input = document.getElementById("locketCommentInput");
          if (input) {
              input.disabled = false;
              input.value = "";
              input.focus();
          }
      } catch (err) {}
  };

  window.deleteLocketComment = function(commentId) {
      var item = document.getElementById("comment-" + commentId);
      if (item) item.remove();
  };

  function renderCommentModal(comments) {
      var modal = document.getElementById("locketCommentModal");
      if (!modal) {
          modal = document.createElement("div");
          modal.id = "locketCommentModal";
          modal.className = "locket-modal-overlay";
          document.body.appendChild(modal);
      }
      
      var html = '<div class="locket-modal-content">';
      html += '<div class="locket-modal-header">';
      html += '  <h3 style="margin:0;font-size:18px;font-weight:700;">Bình luận</h3>';
      html += '  <button type="button" class="close-modal-btn" onclick="document.getElementById(\'locketCommentModal\').style.display=\'none\'">✕</button>';
      html += '</div>';
      
      html += '<div class="locket-modal-body" id="locketCommentList">';
      if (!comments || comments.length === 0) {
          html += '<div class="empty-comments">Chưa có bình luận nào. Hãy là người đầu tiên bình luận!</div>';
      } else {
          for (var i = 0; i < comments.length; i++) {
              html += generateCommentHtml(comments[i]);
          }
      }
      html += '</div>';
      
      html += '<div class="locket-modal-footer">';
      html += '  <input type="text" id="locketCommentInput" placeholder="Thêm bình luận..." onkeypress="if(event.key === \'Enter\') window.submitLocketComment()" />';
      html += '  <button type="button" class="submit-comment-btn" onclick="window.submitLocketComment()">Đăng</button>';
      html += '</div>';
      
      html += '</div>';
      
      modal.innerHTML = html;
      modal.style.display = "flex";
      
      var list = document.getElementById("locketCommentList");
      list.scrollTop = list.scrollHeight;
  }

  function generateCommentHtml(comment) {
      var initial = initialsFromName(comment.authorName);
      var avatarHtml = comment.authorAvatar ? '<img src="' + escapeHtml(comment.authorAvatar) + '" class="comment-avatar" />' : '<div class="comment-avatar-placeholder">' + escapeHtml(initial) + '</div>';
      
      return '<div class="comment-item" id="comment-' + comment.id + '">' +
             '  ' + avatarHtml +
             '  <div class="comment-content-wrap">' +
             '    <div class="comment-author">' + escapeHtml(comment.authorName) + '</div>' +
             '    <div class="comment-text">' + escapeHtml(comment.content) + '</div>' +
             '    <div class="comment-time">' + escapeHtml(comment.timeAgo || "Vừa xong") + '</div>' +
             '  </div>' +
             '</div>';
  }

  function appendCommentToModal(comment) {
      var list = document.getElementById("locketCommentList");
      if (list) {
          var empty = list.querySelector(".empty-comments");
          if (empty) empty.remove();
          list.insertAdjacentHTML("beforeend", generateCommentHtml(comment));
          list.scrollTop = list.scrollHeight;
      }
  }

  window.submitLocketComment = function() {
      var input = document.getElementById("locketCommentInput");
      if (!input || !input.value.trim() || !currentCommentPostId) return;
      
      var text = input.value.trim();
      sendEvent("LOCKET_COMMENT_CREATE", {
          postId: currentCommentPostId,
          content: text
      });
      input.value = "Đang gửi...";
      input.disabled = true;
  };

  function init() {
    try {
      initialized = true;
      renderBanners();
      renderLocket();
      wireControls();
      startAutoSlide();
      bridgeLog("home-social.js initialized");
    } catch (err) {
      bridgeLog("Home social render failed: " + err.message);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
