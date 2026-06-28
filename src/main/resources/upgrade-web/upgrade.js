/**
 * Upgrade tab interactions.
 *
 * This file intentionally keeps the Java bridge contract stable:
 * - window.TutorHubUpgrade.selectPlan(planName, amount)
 * - window.TutorHubUpgrade.goBack()
 * - window.TutorHubUpgrade.toggleBilling(type)
 */
(function() {
  'use strict';

  var currentBilling = 'monthly';
  var reduceMotion = false;

  if (window.matchMedia) {
    try {
      reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    } catch (ignore) {
      reduceMotion = false;
    }
  }

  function safeCallBridge(methodName, args) {
    if (window.TutorHubUpgrade && typeof window.TutorHubUpgrade[methodName] === 'function') {
      try {
        window.TutorHubUpgrade[methodName].apply(window.TutorHubUpgrade, args);
      } catch (e) {
        console.error('Java bridge call failed: ' + methodName, e);
      }
    } else {
      console.warn('Java bridge unavailable: ' + methodName);
    }
  }

  function queryAll(selector, root) {
    return Array.prototype.slice.call((root || document).querySelectorAll(selector));
  }

  function parsePriceText(text) {
    var digits = String(text || '').replace(/[^\d]/g, '');
    return digits ? parseInt(digits, 10) : 0;
  }

  function formatVnd(value) {
    if (!value) return '0\u0111';
    return String(Math.round(value)).replace(/\B(?=(\d{3})+(?!\d))/g, '.') + '\u0111';
  }

  function animatePrice(priceEl, targetText) {
    if (!priceEl) return;

    var targetNumber = parsePriceText(targetText);
    var fromNumber = parsePriceText(priceEl.textContent);

    if (reduceMotion || !window.requestAnimationFrame || fromNumber === targetNumber) {
      priceEl.textContent = targetText;
      return;
    }

    var started = null;
    var duration = 460;

    function step(timestamp) {
      if (!started) started = timestamp;
      var progress = Math.min((timestamp - started) / duration, 1);
      var eased = 1 - Math.pow(1 - progress, 3);
      var value = fromNumber + (targetNumber - fromNumber) * eased;

      priceEl.textContent = formatVnd(value);

      if (progress < 1) {
        window.requestAnimationFrame(step);
      } else {
        priceEl.textContent = targetText;
      }
    }

    window.requestAnimationFrame(step);
  }

  function updateBillingVisual(type) {
    var btnMonthly = document.getElementById('btn-monthly');
    var btnYearly = document.getElementById('btn-yearly');
    var toggle = document.querySelector('.billing-toggle');
    var pill = document.getElementById('billing-pill');

    if (toggle) {
      toggle.setAttribute('data-active', type);
    }

    if (btnMonthly && btnYearly) {
      if (type === 'monthly') {
        btnMonthly.classList.add('billing-btn--active');
        btnYearly.classList.remove('billing-btn--active');
      } else {
        btnMonthly.classList.remove('billing-btn--active');
        btnYearly.classList.add('billing-btn--active');
      }
      btnMonthly.setAttribute('aria-pressed', type === 'monthly' ? 'true' : 'false');
      btnYearly.setAttribute('aria-pressed', type === 'yearly' ? 'true' : 'false');
    }

    if (pill) {
      pill.style.transform = 'translateX(0)';
    }
  }

  function updatePrices(type) {
    var cards = queryAll('.card');
    var pricingGrid = document.querySelector('.pricing-grid');

    if (pricingGrid && !reduceMotion) {
      pricingGrid.classList.remove('is-billing-refresh');
      void pricingGrid.offsetWidth;
      pricingGrid.classList.add('is-billing-refresh');
    }

    for (var i = 0; i < cards.length; i++) {
      var priceEl = cards[i].querySelector('.card-price');
      var periodEl = cards[i].querySelector('.card-period');
      if (!priceEl || !periodEl) continue;

      var priceText = priceEl.getAttribute(type === 'monthly' ? 'data-monthly' : 'data-yearly');
      var periodText = periodEl.getAttribute(type === 'monthly' ? 'data-monthly' : 'data-yearly');

      animatePrice(priceEl, priceText);
      periodEl.innerHTML = '&thinsp;' + periodText;
    }
  }

  function pulseSaveBadge() {
    var badge = document.getElementById('save-badge');
    if (!badge || reduceMotion) return;
    badge.classList.remove('save-badge--pulse');
    void badge.offsetWidth;
    badge.classList.add('save-badge--pulse');
  }

  window.handleGoBack = function() {
    safeCallBridge('goBack', []);
  };

  window.setBilling = function(type) {
    if (type !== 'monthly' && type !== 'yearly') return;
    if (currentBilling === type) return;

    currentBilling = type;
    updateBillingVisual(type);
    updatePrices(type);
    pulseSaveBadge();

    safeCallBridge('toggleBilling', [type]);
  };

  window.handleSelectPlan = function(planName) {
    var card = document.querySelector('.card[data-plan="' + planName + '"]');
    if (!card) return;

    var amountStr = card.getAttribute(currentBilling === 'monthly' ? 'data-monthly-amount' : 'data-yearly-amount');
    var amount = parseFloat(amountStr) || 0;

    var formattedPlanName = planName.charAt(0).toUpperCase() + planName.slice(1);
    if (planName.toLowerCase() === 'vip') {
      formattedPlanName = 'VIP';
    }

    safeCallBridge('selectPlan', [formattedPlanName, amount]);
  };

  function createAmbientParticles() {
    if (reduceMotion || document.querySelector('.ambient-field')) return;

    var field = document.createElement('div');
    field.className = 'ambient-field';
    field.setAttribute('aria-hidden', 'true');

    var colors = ['ambient-dot--indigo', 'ambient-dot--blue', 'ambient-dot--amber', 'ambient-dot--green'];
    for (var i = 0; i < 16; i++) {
      var dot = document.createElement('span');
      dot.className = 'ambient-dot ' + colors[i % colors.length];
      dot.style.left = (8 + Math.random() * 84) + '%';
      dot.style.top = (8 + Math.random() * 78) + '%';
      dot.style.animationDelay = (Math.random() * 4).toFixed(2) + 's';
      dot.style.animationDuration = (7 + Math.random() * 5).toFixed(2) + 's';
      field.appendChild(dot);
    }

    document.body.appendChild(field);
  }

  function setupEntrance() {
    var groups = [
      '.page-header',
      '.features-bar',
      '.billing-section',
      '.pricing-section .card',
      '.guarantees-section'
    ];

    var order = 0;
    for (var i = 0; i < groups.length; i++) {
      var nodes = queryAll(groups[i]);
      for (var j = 0; j < nodes.length; j++) {
        nodes[j].classList.add('js-enter');
        nodes[j].style.transitionDelay = reduceMotion ? '0ms' : (60 + order * 55) + 'ms';
        order++;
      }
    }

    window.setTimeout(function() {
      document.body.classList.add('is-ready');
    }, 30);
  }

  function setupCardTilt() {
    if (reduceMotion) return;

    var cards = queryAll('.card');
    for (var i = 0; i < cards.length; i++) {
      (function(card) {
        card.addEventListener('mousemove', function(event) {
          var rect = card.getBoundingClientRect();
          var x = event.clientX - rect.left;
          var y = event.clientY - rect.top;
          var rotateY = ((x / rect.width) - 0.5) * 7;
          var rotateX = (0.5 - (y / rect.height)) * 7;

          card.classList.add('is-tilting');
          card.style.transform = 'perspective(900px) rotateX(' + rotateX.toFixed(2) + 'deg) rotateY(' + rotateY.toFixed(2) + 'deg) translateY(-7px)';
        });

        card.addEventListener('mouseleave', function() {
          card.classList.remove('is-tilting');
          card.style.transform = '';
        });
      })(cards[i]);
    }
  }

  function setupButtons() {
    var buttons = queryAll('.btn-cta:not(:disabled)');
    for (var i = 0; i < buttons.length; i++) {
      (function(button) {
        button.addEventListener('mousemove', function(event) {
          if (reduceMotion) return;
          var rect = button.getBoundingClientRect();
          var dx = ((event.clientX - rect.left) / rect.width - 0.5) * 8;
          var dy = ((event.clientY - rect.top) / rect.height - 0.5) * 5;
          button.style.transform = 'translate(' + dx.toFixed(1) + 'px, ' + dy.toFixed(1) + 'px)';
        });

        button.addEventListener('mouseleave', function() {
          button.style.transform = '';
        });

        button.addEventListener('mousedown', function() {
          button.classList.add('is-pressed');
        });

        button.addEventListener('mouseup', function() {
          button.classList.remove('is-pressed');
        });

        button.addEventListener('click', function(event) {
          addRipple(button, event);
        });
      })(buttons[i]);
    }
  }

  function addRipple(button, event) {
    if (reduceMotion) return;

    var rect = button.getBoundingClientRect();
    var ripple = document.createElement('span');
    var size = Math.max(rect.width, rect.height);

    ripple.className = 'btn-ripple';
    ripple.style.width = size + 'px';
    ripple.style.height = size + 'px';
    ripple.style.left = (event.clientX - rect.left - size / 2) + 'px';
    ripple.style.top = (event.clientY - rect.top - size / 2) + 'px';

    button.appendChild(ripple);
    window.setTimeout(function() {
      if (ripple.parentNode) ripple.parentNode.removeChild(ripple);
    }, 520);
  }

  function setupMicroInteractions() {
    var featureItems = queryAll('.feature-item, .guarantee-item');
    for (var i = 0; i < featureItems.length; i++) {
      featureItems[i].classList.add('micro-lift');
    }
  }

  function setupInitialPrices() {
    updateBillingVisual(currentBilling);
    var cards = queryAll('.card');
    for (var i = 0; i < cards.length; i++) {
      var priceEl = cards[i].querySelector('.card-price');
      if (priceEl) {
        animatePrice(priceEl, priceEl.getAttribute('data-monthly'));
      }
    }
  }

  function init() {
    setupEntrance();
    createAmbientParticles();
    setupInitialPrices();
    setupCardTilt();
    setupButtons();
    setupMicroInteractions();
  }

  window.addEventListener('DOMContentLoaded', init);

})();
