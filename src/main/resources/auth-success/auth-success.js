/**
 * TutorHub Enterprise — Auth Success page effects
 * - Provider badge + auto-close countdown (critical, runs independent of canvas)
 * - Ambient orbit particles + pastel mouse-trail (petalCanvas)
 * - Firework bursts on load + CTA hover/click (fxCanvas)
 * - Parallax for floating decorative cards
 * - Button ripple micro-interaction
 *
 * 100% original Canvas API + requestAnimationFrame implementation.
 * No external libraries / CDN. No sensitive data is ever logged.
 * Respects prefers-reduced-motion. Degrades gracefully if Canvas is unavailable.
 */
(function () {
  'use strict';

  var reduceMotion = false;
  try {
    reduceMotion = !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);
  } catch (e) { /* matchMedia unsupported — proceed with motion enabled */ }

  function rand(min, max) { return Math.random() * (max - min) + min; }
  function pick(arr) { return arr[Math.floor(Math.random() * arr.length)]; }

  // ============================================================
  // 1) Provider badge — reads the same "provider" query param as
  //    before. Only the rendered text/icon is upgraded to support
  //    a generic default state. Runs first and independently of
  //    every visual effect below.
  // ============================================================
  (function setupProviderBadge() {
    try {
      var params = new URLSearchParams(window.location.search);
      var provider = params.get('provider');
      var el = document.getElementById('providerLabel');
      var txt = document.getElementById('providerText');
      var icon = document.getElementById('providerIcon');
      if (!el || !txt) return;

      if (provider === 'google') {
        txt.textContent = 'Đăng nhập bằng Google thành công';
        if (icon) { icon.src = '/images/icon/google.svg'; icon.style.display = 'inline-block'; }
      } else if (provider === 'facebook') {
        txt.textContent = 'Đăng nhập bằng Facebook thành công';
        if (icon) { icon.src = '/images/icon/facebook.svg'; icon.style.display = 'inline-block'; }
      } else {
        // No provider, or an unrecognized value — generic success label.
        txt.textContent = 'Đăng nhập thành công';
        if (icon) icon.style.display = 'none';
      }
      el.style.display = 'inline-flex';
      // Marks the badge as already handled so the small inline fallback
      // script still in auth-success.html (kept in case this file fails
      // to load) knows to skip re-doing the same work.
      window.__AUTH_PROVIDER = provider || 'none';
    } catch (e) { /* cosmetic only — never block the page */ }
  })();

  // ============================================================
  // 2) Auto-close countdown — unchanged behavior from the
  //    original implementation, just isolated so a canvas/animation
  //    failure can never prevent it from running.
  // ============================================================
  (function setupCountdown() {
    try {
      var countdownEl = document.getElementById('countdown');
      var countdownNum = document.getElementById('countdownNum');
      if (!countdownEl || !countdownNum) return;

      var seconds = 5;
      var interval = setInterval(function () {
        seconds--;
        countdownNum.textContent = seconds;
        if (seconds <= 0) {
          clearInterval(interval);
          countdownEl.textContent = 'Bạn có thể đóng tab này.';
          try { window.close(); } catch (e) { /* browser may block this */ }
        }
      }, 1000);
    } catch (e) { /* never block the page */ }
  })();

  // ============================================================
  // 3) Decorative visual effects — fully isolated. If Canvas is
  //    unsupported or anything throws, the page is still 100%
  //    usable (this is the required fallback).
  // ============================================================
  try {
    setupVisualEffects();
  } catch (e) { /* fallback: static background already looks fine without it */ }

  function setupVisualEffects() {
    var petalCanvas = document.getElementById('petalCanvas');
    var fxCanvas = document.getElementById('fxCanvas');
    if (!petalCanvas || !fxCanvas || !petalCanvas.getContext || !fxCanvas.getContext) return;

    var pctx = petalCanvas.getContext('2d');
    var fctx = fxCanvas.getContext('2d');
    if (!pctx || !fctx) return;

    var W, H;
    function resize() {
      W = window.innerWidth;
      H = window.innerHeight;
      petalCanvas.width = W; petalCanvas.height = H;
      fxCanvas.width = W; fxCanvas.height = H;
    }
    window.addEventListener('resize', resize);
    resize();

    function heroCenter() { return { x: W * 0.5, y: H * 0.42 }; }

    // Resolves a real on-screen point for a decorative burst target, or
    // null if the element is missing/not currently rendered (e.g.
    // .mockup-wrap is display:none under the 768px breakpoint) — callers
    // skip that burst rather than firing at a stale or wrong position.
    function pointOf(el) {
      if (!el || !el.getClientRects().length) return null;
      var r = el.getBoundingClientRect();
      return { x: r.left + r.width / 2, y: r.top + r.height / 2 };
    }

    // Lightweight click-ripple on the two CTA buttons. Kept outside the
    // reduced-motion gate below — the global CSS rule already collapses
    // its duration to ~0ms when the user prefers reduced motion.
    setupButtonRipple();

    if (reduceMotion) return; // skip every heavy particle/canvas animation

    // 3D tilt + shine-replay on the mockup. Also gated behind reduced-motion
    // since a rotating card is exactly the kind of motion that spec asks us
    // to drop for users who request it.
    setupTilt();

    // ── Palettes ──
    var PASTEL = ['#C4B5FD', '#A5B4FC', '#7DD3FC', '#FBCFE8', '#FDE68A', '#BBF7D0'];
    var BOLD = ['#7C3AED', '#6366F1', '#38BDF8', '#22C55E', '#F97316', '#EC4899'];

    // ================= Mouse-trail petals/confetti =================
    var mouseX = -1000, mouseY = -1000, lastMouseX = -1000, lastMouseY = -1000;
    var isMouseActive = false, mouseTimer = null;
    var trailParticles = [];
    var MAX_TRAIL = 220;

    document.addEventListener('mousemove', function (e) {
      mouseX = e.clientX; mouseY = e.clientY;
      isMouseActive = true;
      clearTimeout(mouseTimer);
      mouseTimer = setTimeout(function () { isMouseActive = false; }, 200);
      updateParallax(e.clientX, e.clientY);
    });
    document.addEventListener('mouseleave', function () { isMouseActive = false; });

    function TrailParticle(x, y, cx, cy) {
      this.x = x; this.y = y;
      this.vx = rand(-1.4, 1.4);
      this.vy = rand(-2.2, -0.4);

      // Gentle vortex when near the hero center, per spec ("xoáy nhẹ quanh trung tâm")
      var dx = x - cx, dy = y - cy;
      var dist = Math.sqrt(dx * dx + dy * dy) || 1;
      if (dist < 260) {
        var swirl = (1 - dist / 260) * 0.6;
        this.vx += (-dy / dist) * swirl;
        this.vy += (dx / dist) * swirl;
      }

      this.gravity = rand(0.015, 0.04);
      this.life = 1;
      this.decay = rand(0.012, 0.022);
      this.size = rand(4, 8);
      this.color = pick(PASTEL);
      this.rotation = rand(0, Math.PI * 2);
      this.rotSpeed = rand(-0.08, 0.08);
      this.shape = Math.random() > 0.5 ? 'petal' : 'circle';
    }
    TrailParticle.prototype.update = function () {
      this.x += this.vx;
      this.vy += this.gravity;
      this.y += this.vy;
      this.rotation += this.rotSpeed;
      this.life -= this.decay;
      return this.life > 0;
    };
    TrailParticle.prototype.draw = function (ctx) {
      ctx.save();
      ctx.translate(this.x, this.y);
      ctx.rotate(this.rotation);
      ctx.globalAlpha = Math.max(0, this.life * 0.8);
      ctx.fillStyle = this.color;
      if (this.shape === 'petal') {
        ctx.beginPath();
        ctx.ellipse(0, 0, this.size * 0.5, this.size, 0, 0, Math.PI * 2);
        ctx.fill();
      } else {
        ctx.beginPath();
        ctx.arc(0, 0, this.size * 0.45, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.restore();
    };

    // ================= Ambient orbit particles =================
    var ambientParticles = [];
    function AmbientParticle() {
      this.angle = rand(0, Math.PI * 2);
      this.radius = rand(70, 210);
      this.speed = rand(0.0015, 0.006);
      this.size = rand(1.6, 4);
      this.color = pick(BOLD);
      this.opacity = rand(0.08, 0.28);
      this.wobble = rand(0.5, 2);
      this.wobbleSpeed = rand(0.01, 0.03);
      this.wobbleOffset = rand(0, Math.PI * 2);
      this.t = 0;
    }
    AmbientParticle.prototype.update = function () {
      this.angle += this.speed;
      this.t += this.wobbleSpeed;
    };
    AmbientParticle.prototype.draw = function (ctx, cx, cy) {
      var wobble = Math.sin(this.t + this.wobbleOffset) * this.wobble;
      var r = this.radius + wobble * 10;
      var x = cx + Math.cos(this.angle) * r;
      var y = cy + Math.sin(this.angle) * r;
      ctx.save();
      ctx.globalAlpha = this.opacity;
      ctx.fillStyle = this.color;
      ctx.beginPath();
      ctx.arc(x, y, this.size, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    };
    for (var i = 0; i < 46; i++) ambientParticles.push(new AmbientParticle());

    // ================= Firework bursts (fxCanvas) =================
    var fireworks = [];
    var MAX_FX = 480;

    function FireworkParticle(x, y) {
      var angle = rand(0, Math.PI * 2);
      var speed = rand(1.5, 6.5);
      this.x = x; this.y = y;
      this.vx = Math.cos(angle) * speed;
      this.vy = Math.sin(angle) * speed;
      this.gravity = rand(0.05, 0.09);
      this.drag = 0.985;
      this.life = 1;
      this.decay = rand(0.012, 0.022);
      this.size = rand(1.6, 3.4);
      this.color = pick(BOLD);
    }
    FireworkParticle.prototype.update = function () {
      this.vx *= this.drag;
      this.vy = this.vy * this.drag + this.gravity;
      this.x += this.vx;
      this.y += this.vy;
      this.life -= this.decay;
      return this.life > 0;
    };
    FireworkParticle.prototype.draw = function (ctx) {
      ctx.save();
      ctx.globalAlpha = Math.max(0, this.life);
      ctx.fillStyle = this.color;
      ctx.shadowColor = this.color;
      ctx.shadowBlur = 6;
      ctx.beginPath();
      ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    };

    function launchFirework(x, y, count) {
      count = count || 55;
      for (var k = 0; k < count; k++) {
        if (fireworks.length >= MAX_FX) break;
        fireworks.push(new FireworkParticle(x, y));
      }
    }

    // 2-3 bursts around the logo and the image mockup shortly after load.
    var logoEl = document.querySelector('.logo-mark');
    var mockupEl = document.querySelector('.mockup-wrap .tilt-wrap') || document.querySelector('.mockup-wrap');
    [logoEl, mockupEl, logoEl].forEach(function (el, idx) {
      setTimeout(function () {
        var p = pointOf(el); // resolved fresh here, not at page-load time
        if (!p) return; // element hidden at this breakpoint — skip that burst
        launchFirework(p.x + rand(-20, 20), p.y + rand(-15, 10), idx === 1 ? 60 : 48);
      }, 180 + idx * 380);
    });

    // Small burst on the primary CTA, throttled so hover/click can't spam particles.
    var ctaBtn = document.getElementById('ctaOpenApp');
    if (ctaBtn) {
      var lastCtaBurst = 0;
      var ctaBurst = function () {
        var now = Date.now();
        if (now - lastCtaBurst < 650) return;
        lastCtaBurst = now;
        var rect = ctaBtn.getBoundingClientRect();
        launchFirework(rect.left + rect.width / 2, rect.top + rect.height / 2, 26);
      };
      ctaBtn.addEventListener('mouseenter', ctaBurst);
      ctaBtn.addEventListener('click', ctaBurst);
    }

    // ================= Parallax for floating decorative cards =================
    // NOTE: each card already has its own CSS `cardFloat` animation for the
    // slow up/down bob. That animation is what actually renders `transform`
    // on the element, so setting `el.style.transform` directly from here
    // would get overwritten on the very next animation frame. Instead we
    // write to the --px/--py custom properties that cardFloat's keyframes
    // now read internally — the bob and the mouse-parallax then compose
    // into a single transform instead of fighting over it.
    var cards = document.querySelectorAll('.floating-card, .mfc');
    var parallaxRaf = null, pendingX = 0, pendingY = 0;
    function updateParallax(clientX, clientY) {
      pendingX = (clientX / W) * 2 - 1;
      pendingY = (clientY / H) * 2 - 1;
      if (parallaxRaf) return;
      parallaxRaf = requestAnimationFrame(applyParallax);
    }
    function applyParallax() {
      parallaxRaf = null;
      for (var c = 0; c < cards.length; c++) {
        var depth = 6 + c * 2.5;
        cards[c].style.setProperty('--px', (pendingX * depth).toFixed(1) + 'px');
        cards[c].style.setProperty('--py', (pendingY * depth).toFixed(1) + 'px');
      }
    }

    // ================= Main render loop =================
    function animate() {
      requestAnimationFrame(animate);
      var c = heroCenter();

      pctx.clearRect(0, 0, W, H);
      for (var a = 0; a < ambientParticles.length; a++) {
        ambientParticles[a].update();
        ambientParticles[a].draw(pctx, c.x, c.y);
      }

      if (isMouseActive) {
        var dx = mouseX - lastMouseX, dy = mouseY - lastMouseY;
        var dist = Math.sqrt(dx * dx + dy * dy);
        var spawnCount = Math.min(Math.floor(dist * 0.5) + 2, 8);
        for (var s = 0; s < spawnCount; s++) {
          if (trailParticles.length < MAX_TRAIL) {
            var t = s / spawnCount;
            var px = lastMouseX + dx * t + rand(-5, 5);
            var py = lastMouseY + dy * t + rand(-5, 5);
            trailParticles.push(new TrailParticle(px, py, c.x, c.y));
          }
        }
      }
      lastMouseX = mouseX; lastMouseY = mouseY;

      for (var j = trailParticles.length - 1; j >= 0; j--) {
        if (!trailParticles[j].update()) {
          trailParticles.splice(j, 1);
        } else {
          trailParticles[j].draw(pctx);
        }
      }
      if (trailParticles.length > MAX_TRAIL) {
        trailParticles.splice(0, trailParticles.length - MAX_TRAIL);
      }

      fctx.clearRect(0, 0, W, H);
      for (var f = fireworks.length - 1; f >= 0; f--) {
        if (!fireworks[f].update()) {
          fireworks.splice(f, 1);
        } else {
          fireworks[f].draw(fctx);
        }
      }
    }
    animate();
  }

  function setupButtonRipple() {
    var btns = document.querySelectorAll('.btn-primary, .btn-secondary');
    for (var i = 0; i < btns.length; i++) {
      btns[i].addEventListener('click', function (e) {
        try {
          var btn = e.currentTarget;
          var rect = btn.getBoundingClientRect();
          var size = Math.max(rect.width, rect.height) * 1.3;
          var span = document.createElement('span');
          span.className = 'ripple';
          span.style.width = span.style.height = size + 'px';
          span.style.left = (e.clientX - rect.left - size / 2) + 'px';
          span.style.top = (e.clientY - rect.top - size / 2) + 'px';
          btn.appendChild(span);
          setTimeout(function () { span.remove(); }, 650);
        } catch (err) { /* purely cosmetic */ }
      });
    }
  }

  // ================= 3D tilt + glow + shine-replay for the mockup =================
  // Hover the mockup card: it tilts toward the cursor (rotateX/rotateY,
  // capped to a small angle so it stays subtle), the shadow underneath
  // shifts and warms up to read as a glow that follows the tilt, and a
  // light sweep replays across the surface. Everything eases back to flat
  // on mouseleave. Skipped entirely under prefers-reduced-motion (the
  // caller already gates this) and inert on touch-only devices, since
  // there is no hover position to tilt toward.
  function setupTilt() {
    var tiltWrap = document.getElementById('mockupTilt');
    var mockupBrowser = tiltWrap ? tiltWrap.querySelector('.mockup-browser') : null;
    var shine = document.getElementById('mockShine');
    if (!tiltWrap) return;

    var isFinePointer = true;
    try { isFinePointer = !window.matchMedia || window.matchMedia('(pointer: fine)').matches; } catch (e) {}
    if (!isFinePointer) return; // touch device — no cursor to tilt toward

    var MAX_TILT = 9; // degrees — kept small per spec ("nghieng nhe", not a flip)
    var lastShineAt = 0;

    function replayShine() {
      if (!shine) return;
      var now = Date.now();
      if (now - lastShineAt < 900) return; // throttle re-entry spam
      lastShineAt = now;
      shine.classList.remove('mock-shine--replay');
      void shine.offsetWidth; // force reflow so the animation can restart
      shine.classList.add('mock-shine--replay');
    }

    tiltWrap.addEventListener('mouseenter', function () {
      tiltWrap.classList.remove('tilt-resetting');
      replayShine();
    });

    tiltWrap.addEventListener('mousemove', function (e) {
      var rect = tiltWrap.getBoundingClientRect();
      var px = (e.clientX - rect.left) / rect.width;  // 0..1 across the card
      var py = (e.clientY - rect.top) / rect.height;  // 0..1 down the card
      var rotateY = (px - 0.5) * 2 * MAX_TILT;        // left/right tilt
      var rotateX = (0.5 - py) * 2 * MAX_TILT;         // up/down tilt

      tiltWrap.style.transform =
        'perspective(1000px) rotateX(' + rotateX.toFixed(2) + 'deg) rotateY(' + rotateY.toFixed(2) + 'deg) scale(1.015)';

      // Glow/shadow follows the tilt direction, brighter the steeper the angle.
      if (mockupBrowser) {
        var angle = Math.sqrt(rotateX * rotateX + rotateY * rotateY) / (MAX_TILT * 1.41);
        var shadowX = (rotateY / MAX_TILT) * 18;
        var shadowY = (-rotateX / MAX_TILT) * 18 + 14;
        mockupBrowser.style.setProperty(
          '--tilt-glow',
          shadowX.toFixed(1) + 'px ' + shadowY.toFixed(1) + 'px ' + (36 + angle * 28).toFixed(0) +
          'px rgba(124, 58, 237, ' + (0.16 + angle * 0.20).toFixed(2) + ')'
        );
      }
    });

    tiltWrap.addEventListener('mouseleave', function () {
      tiltWrap.classList.add('tilt-resetting');
      tiltWrap.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale(1)';
      if (mockupBrowser) mockupBrowser.style.setProperty('--tilt-glow', '0 0px 0px rgba(124, 58, 237, 0)');
      setTimeout(function () { tiltWrap.classList.remove('tilt-resetting'); }, 520);
    });
  }

})();
