/* =========================================================================
   classes.js
   Render khu vực "Lớp học nổi bật" trong JavaFX WebView.

   API Java -> JS (Java gọi xuống):
     window.TutorHubClasses.updateData(jsonOrArray, view)
       - jsonOrArray: chuỗi JSON hoặc array các object class đã build từ
         classModels (sau khi đã applyFilterAndSort bên Java).
       - view: "grid" | "list" (tuỳ chọn, tương ứng isGridView bên Java).

   API JS -> Java (JS gọi lên qua bridge do Java set vào window):
     window.TutorHubClassesBridge.acceptClass(id)
       -> Java: tìm ClassModel theo id, gọi showClassDetailModal(...)
     window.TutorHubClassesBridge.toggleHeart(id)
       -> Java: tìm ClassModel theo id, đổi m.isSaved, gọi applyFilterAndSort()
     window.TutorHubClassesBridge.reportHeight(px)
       -> Java: chỉnh preferred size của JFXPanel để JScrollPane bên ngoài
          scroll đúng (vì WebView không tự auto-height như iframe).

   Tương thích WebView: KHÔNG dùng import/export, KHÔNG optional chaining,
   KHÔNG top-level await, không phụ thuộc CDN/framework ngoài.
   ========================================================================= */

(function () {
  "use strict";

  var state = {
    classes: [],
    view: "grid"
  };

  var gridEl = null;
  var emptyEl = null;

  /* ----------------------------- helpers ----------------------------- */

  function escapeHtml(str) {
    if (str === null || str === undefined) return "";
    return String(str).replace(/[&<>"']/g, function (c) {
      if (c === "&") return "&amp;";
      if (c === "<") return "&lt;";
      if (c === ">") return "&gt;";
      if (c === '"') return "&quot;";
      return "&#39;";
    });
  }

  function callBridge(method, args) {
    try {
      if (window.TutorHubClassesBridge && typeof window.TutorHubClassesBridge[method] === "function") {
        window.TutorHubClassesBridge[method].apply(window.TutorHubClassesBridge, args || []);
      } else {
        console.warn("[TutorHubClasses] Bridge Java chưa sẵn sàng: " + method);
      }
    } catch (err) {
      console.error("[TutorHubClasses] Lỗi khi gọi bridge " + method, err);
    }
  }

  var ICON_LOCATION =
    '<svg viewBox="0 0 24 24" fill="none"><path d="M12 22s7-7.1 7-12a7 7 0 1 0-14 0c0 4.9 7 12 7 12z" stroke-linejoin="round"/><circle cx="12" cy="10" r="2.4" stroke-linejoin="round"/></svg>';

  var ICON_CLOCK =
    '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.5 2" stroke-linecap="round" stroke-linejoin="round"/></svg>';

  var ICON_HEART =
    '<svg viewBox="0 0 24 24"><path d="M12 20.6s-7.6-4.6-10-9.2C0.4 7.8 2.7 4.6 6 4.6c2.1 0 3.6 1.2 4.5 2.6 0.4 0.6 0.6 0.9 1.5 0.9s1.1-0.3 1.5-0.9C14.4 5.8 15.9 4.6 18 4.6c3.3 0 5.6 3.2 4 6.8-2.4 4.6-10 9.2-10 9.2z" stroke-linejoin="round"/></svg>';

  /* --------------------------- xây 1 card ----------------------------- */

  function buildCard(cls, idx) {
    var taken = !!cls.isTaken;
    var saved = !!cls.isSaved;
    var id = cls.id !== undefined && cls.id !== null ? String(cls.id) : "";

    var card = document.createElement("div");
    card.className = "card";
    card.style.animationDelay = Math.min(idx * 35, 280) + "ms";

    // --- cover ---
    var cover = document.createElement("div");
    cover.className = "cover-wrap";

    var img = document.createElement("img");
    img.alt = cls.subj || "";
    img.src = cls.img || "";
    img.onerror = function () {
      cover.className = "cover-wrap fallback";
    };
    cover.appendChild(img);

    var badgeRow = document.createElement("div");
    badgeRow.className = "badge-row";

    var tagBadge = document.createElement("span");
    tagBadge.className = "badge";
    tagBadge.style.background = cls.tagColor || "#3B82F6";
    tagBadge.textContent = cls.tagText || "";
    badgeRow.appendChild(tagBadge);

    var statusBadge = document.createElement("span");
    statusBadge.className = "badge " + (taken ? "status-taken" : "status-open");
    statusBadge.textContent = taken ? "Đã chốt" : "Còn lớp";
    badgeRow.appendChild(statusBadge);

    cover.appendChild(badgeRow);

    var heartBtn = document.createElement("button");
    heartBtn.type = "button";
    heartBtn.className = "heart-btn" + (saved ? " active" : "");
    heartBtn.title = saved ? "Bỏ lưu" : "Lưu lớp học";
    heartBtn.innerHTML = ICON_HEART;
    heartBtn.addEventListener("click", function (e) {
      e.stopPropagation();
      heartBtn.classList.toggle("active");
      heartBtn.classList.remove("pulse");
      // ép reflow để animation pulse chạy lại được mỗi lần bấm
      void heartBtn.offsetWidth;
      heartBtn.classList.add("pulse");
      callBridge("toggleHeart", [id]);
    });
    cover.appendChild(heartBtn);

    card.appendChild(cover);

    // --- body ---
    var body = document.createElement("div");
    body.className = "card-body";

    var title = document.createElement("div");
    title.className = "title";
    title.title = cls.subj || "";
    title.textContent = cls.subj || "";
    body.appendChild(title);

    var rowAddr = document.createElement("div");
    rowAddr.className = "meta-row";
    rowAddr.innerHTML = ICON_LOCATION + "<span>" + escapeHtml(cls.addr) + "</span>";
    body.appendChild(rowAddr);

    var rowTime = document.createElement("div");
    rowTime.className = "meta-row";
    rowTime.innerHTML = ICON_CLOCK + "<span>" + escapeHtml(cls.time) + "</span>";
    body.appendChild(rowTime);

    var desc = document.createElement("div");
    desc.className = "desc";
    desc.title = cls.req || "";
    desc.textContent = cls.req || "";
    body.appendChild(desc);

    card.appendChild(body);

    // --- footer ---
    var footer = document.createElement("div");
    footer.className = "card-footer";

    var price = document.createElement("div");
    price.className = "price";
    price.textContent = cls.sal || "";
    footer.appendChild(price);

    var acceptBtn = document.createElement("button");
    acceptBtn.type = "button";
    acceptBtn.className = "btn-accept";
    if (taken) {
      acceptBtn.textContent = "Đã nhận";
      acceptBtn.disabled = true;
    } else {
      acceptBtn.textContent = "Nhận lớp";
      acceptBtn.addEventListener("click", function (e) {
        e.stopPropagation();
        callBridge("acceptClass", [id]);
      });
    }
    footer.appendChild(acceptBtn);

    card.appendChild(footer);

    return card;
  }

  /* ----------------------------- render -------------------------------- */

  function render() {
    if (!gridEl || !emptyEl) return;

    gridEl.innerHTML = "";

    if (!state.classes || state.classes.length === 0) {
      gridEl.hidden = true;
      emptyEl.hidden = false;
      reportHeight();
      return;
    }

    emptyEl.hidden = true;
    gridEl.hidden = false;
    gridEl.className = "cards-grid " + (state.view === "list" ? "view-list" : "view-grid");

    for (var i = 0; i < state.classes.length; i++) {
      gridEl.appendChild(buildCard(state.classes[i], i));
    }

    // Đợi layout xong rồi mới đo chiều cao thật để báo Java resize JFXPanel
    requestAnimationFrame(function () {
      requestAnimationFrame(reportHeight);
    });
  }

  function reportHeight() {
    try {
      var h = document.body ? document.body.scrollHeight : 0;
      callBridge("reportHeight", [h]);
    } catch (e) {
      console.error("[TutorHubClasses] Lỗi reportHeight", e);
    }
  }

  var resizeTimer = null;
  window.addEventListener("resize", function () {
    if (resizeTimer) clearTimeout(resizeTimer);
    resizeTimer = setTimeout(reportHeight, 120);
  });

  function normalizeList(jsonOrArray) {
    if (jsonOrArray === null || jsonOrArray === undefined) return [];
    if (typeof jsonOrArray === "string") {
      try {
        var parsed = JSON.parse(jsonOrArray);
        return Array.isArray(parsed) ? parsed : [];
      } catch (e) {
        console.error("[TutorHubClasses] JSON từ Java không hợp lệ", e);
        return [];
      }
    }
    return Array.isArray(jsonOrArray) ? jsonOrArray : [];
  }

  /* ------------------------- API cho Java gọi xuống ------------------------- */

  window.TutorHubClasses = {
    updateData: function (jsonOrArray, view) {
      state.classes = normalizeList(jsonOrArray);
      if (view === "grid" || view === "list") {
        state.view = view;
      }
      render();
    }
  };

  document.addEventListener("DOMContentLoaded", function () {
    gridEl = document.getElementById("classGrid");
    emptyEl = document.getElementById("emptyState");
    render();
  });
})();
