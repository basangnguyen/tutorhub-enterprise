(function () {
  "use strict";

  if (window.TSEVietnameseInput && window.TSEVietnameseInput.__installed) {
    return;
  }

  var mode = window.__TSE_INPUT_MODE === "vi" ? "vi" : "en";
  var composing = false;
  var internalChange = false;
  var skipNextInput = false;

  var COMBINATIONS = [
    [/^duow/g, "đươ"], [/^Duow/g, "Đươ"], [/^DUOW/g, "ĐƯƠ"],
    [/dD/g, "đ"], [/DD/g, "Đ"], [/dd/g, "đ"],
    [/uow/g, "ươ"], [/Uow/g, "Ươ"], [/UOW/g, "ƯƠ"],
    [/aa/g, "â"], [/AA/g, "Â"], [/Aa/g, "Â"],
    [/aw/g, "ă"], [/AW/g, "Ă"], [/Aw/g, "Ă"],
    [/ee/g, "ê"], [/EE/g, "Ê"], [/Ee/g, "Ê"],
    [/oo/g, "ô"], [/OO/g, "Ô"], [/Oo/g, "Ô"],
    [/ow/g, "ơ"], [/OW/g, "Ơ"], [/Ow/g, "Ơ"],
    [/uw/g, "ư"], [/UW/g, "Ư"], [/Uw/g, "Ư"]
  ];

  var TONE_MAP = {
    "a": ["á", "à", "ả", "ã", "ạ"], "A": ["Á", "À", "Ả", "Ã", "Ạ"],
    "ă": ["ắ", "ằ", "ẳ", "ẵ", "ặ"], "Ă": ["Ắ", "Ằ", "Ẳ", "Ẵ", "Ặ"],
    "â": ["ấ", "ầ", "ẩ", "ẫ", "ậ"], "Â": ["Ấ", "Ầ", "Ẩ", "Ẫ", "Ậ"],
    "e": ["é", "è", "ẻ", "ẽ", "ẹ"], "E": ["É", "È", "Ẻ", "Ẽ", "Ẹ"],
    "ê": ["ế", "ề", "ể", "ễ", "ệ"], "Ê": ["Ế", "Ề", "Ể", "Ễ", "Ệ"],
    "i": ["í", "ì", "ỉ", "ĩ", "ị"], "I": ["Í", "Ì", "Ỉ", "Ĩ", "Ị"],
    "o": ["ó", "ò", "ỏ", "õ", "ọ"], "O": ["Ó", "Ò", "Ỏ", "Õ", "Ọ"],
    "ô": ["ố", "ồ", "ổ", "ỗ", "ộ"], "Ô": ["Ố", "Ồ", "Ổ", "Ỗ", "Ộ"],
    "ơ": ["ớ", "ờ", "ở", "ỡ", "ợ"], "Ơ": ["Ớ", "Ờ", "Ở", "Ỡ", "Ợ"],
    "u": ["ú", "ù", "ủ", "ũ", "ụ"], "U": ["Ú", "Ù", "Ủ", "Ũ", "Ụ"],
    "ư": ["ứ", "ừ", "ử", "ữ", "ự"], "Ư": ["Ứ", "Ừ", "Ử", "Ữ", "Ự"],
    "y": ["ý", "ỳ", "ỷ", "ỹ", "ỵ"], "Y": ["Ý", "Ỳ", "Ỷ", "Ỹ", "Ỵ"]
  };

  var TONE_INDEX = { s: 0, f: 1, r: 2, x: 3, j: 4 };

  var REMOVE_TONE = {
    "á": "a", "à": "a", "ả": "a", "ã": "a", "ạ": "a",
    "Á": "A", "À": "A", "Ả": "A", "Ã": "A", "Ạ": "A",
    "ắ": "ă", "ằ": "ă", "ẳ": "ă", "ẵ": "ă", "ặ": "ă",
    "Ắ": "Ă", "Ằ": "Ă", "Ẳ": "Ă", "Ẵ": "Ă", "Ặ": "Ă",
    "ấ": "â", "ầ": "â", "ẩ": "â", "ẫ": "â", "ậ": "â",
    "Ấ": "Â", "Ầ": "Â", "Ẩ": "Â", "Ẫ": "Â", "Ậ": "Â",
    "é": "e", "è": "e", "ẻ": "e", "ẽ": "e", "ẹ": "e",
    "É": "E", "È": "E", "Ẻ": "E", "Ẽ": "E", "Ẹ": "E",
    "ế": "ê", "ề": "ê", "ể": "ê", "ễ": "ê", "ệ": "ê",
    "Ế": "Ê", "Ề": "Ê", "Ể": "Ê", "Ễ": "Ê", "Ệ": "Ê",
    "í": "i", "ì": "i", "ỉ": "i", "ĩ": "i", "ị": "i",
    "Í": "I", "Ì": "I", "Ỉ": "I", "Ĩ": "I", "Ị": "I",
    "ó": "o", "ò": "o", "ỏ": "o", "õ": "o", "ọ": "o",
    "Ó": "O", "Ò": "O", "Ỏ": "O", "Õ": "O", "Ọ": "O",
    "ố": "ô", "ồ": "ô", "ổ": "ô", "ỗ": "ô", "ộ": "ô",
    "Ố": "Ô", "Ồ": "Ô", "Ổ": "Ô", "Ỗ": "Ô", "Ộ": "Ô",
    "ớ": "ơ", "ờ": "ơ", "ở": "ơ", "ỡ": "ơ", "ợ": "ơ",
    "Ớ": "Ơ", "Ờ": "Ơ", "Ở": "Ơ", "Ỡ": "Ơ", "Ợ": "Ơ",
    "ú": "u", "ù": "u", "ủ": "u", "ũ": "u", "ụ": "u",
    "Ú": "U", "Ù": "U", "Ủ": "U", "Ũ": "U", "Ụ": "U",
    "ứ": "ư", "ừ": "ư", "ử": "ư", "ữ": "ư", "ự": "ư",
    "Ứ": "Ư", "Ừ": "Ư", "Ử": "Ư", "Ữ": "Ư", "Ự": "Ư",
    "ý": "y", "ỳ": "y", "ỷ": "y", "ỹ": "y", "ỵ": "y",
    "Ý": "Y", "Ỳ": "Y", "Ỷ": "Y", "Ỹ": "Y", "Ỵ": "Y"
  };

  function isEditableTarget(target) {
    if (!target || target.disabled || target.readOnly) {
      return false;
    }
    if (target.isContentEditable) {
      return true;
    }
    var tag = (target.tagName || "").toLowerCase();
    if (tag === "textarea") {
      return true;
    }
    if (tag !== "input") {
      return false;
    }
    var type = (target.getAttribute("type") || "text").toLowerCase();
    return ["text", "search", "email", "url", "tel"].indexOf(type) !== -1;
  }

  function isWordChar(ch) {
    return /[0-9A-Za-zÀ-ỹĐđ]/.test(ch);
  }

  function applyCombinations(word) {
    var out = word;
    for (var i = 0; i < COMBINATIONS.length; i++) {
      out = out.replace(COMBINATIONS[i][0], COMBINATIONS[i][1]);
    }
    return out;
  }

  function stripToneMarks(word) {
    var out = "";
    for (var i = 0; i < word.length; i++) {
      out += REMOVE_TONE[word.charAt(i)] || word.charAt(i);
    }
    return out;
  }

  function baseVowel(ch) {
    return REMOVE_TONE[ch] || ch;
  }

  function isVowel(ch) {
    return Object.prototype.hasOwnProperty.call(TONE_MAP, baseVowel(ch));
  }

  function chooseToneIndex(word) {
    var vowels = [];
    var stripped = stripToneMarks(word).toLowerCase();
    for (var i = 0; i < word.length; i++) {
      var ch = stripped.charAt(i);
      if (isVowel(ch)) {
        if (ch === "u" && i > 0 && stripped.charAt(i - 1) === "q" && i < word.length - 1 && isVowel(stripped.charAt(i + 1))) {
          // treat 'u' as part of 'q'
        } else if (ch === "i" && i > 0 && stripped.charAt(i - 1) === "g" && i < word.length - 1 && isVowel(stripped.charAt(i + 1))) {
          // treat 'i' as part of 'g'
        } else {
          vowels.push(i);
        }
      }
    }

    if (vowels.length === 0) {
      return -1;
    }
    if (vowels.length === 1) {
      return vowels[0];
    }

    if (vowels.length === 2) {
      var v1 = stripped.charAt(vowels[0]);
      var v2 = stripped.charAt(vowels[1]);
      var hasEndConsonant = vowels[1] < stripped.length - 1;

      if (hasEndConsonant) {
        return vowels[1];
      } else {
        if ((v1 === "u" && v2 === "e") || (v1 === "u" && v2 === "o")) {
          return vowels[1];
        }
        return vowels[0];
      }
    }

    if (vowels.length === 3) {
      if (stripped.charAt(vowels[0]) === "u" && stripped.charAt(vowels[1]) === "y" && (stripped.charAt(vowels[2]) === "e" || stripped.charAt(vowels[2]) === "ê")) {
        return vowels[2];
      }
      return vowels[1];
    }

    return vowels[Math.floor(vowels.length / 2)];
  }

  function applyTone(word, marker) {
    if (marker === "z") {
      return stripToneMarks(word);
    }
    var tone = TONE_INDEX[marker];
    if (tone === undefined) {
      return word;
    }
    var target = chooseToneIndex(word);
    if (target < 0) {
      return word + marker;
    }
    var chars = word.split("");
    var base = baseVowel(chars[target]);
    if (!TONE_MAP[base]) {
      return word + marker;
    }
    chars[target] = TONE_MAP[base][tone];
    return chars.join("");
  }

  function transformWord(rawWord) {
    if (!rawWord) {
      return rawWord;
    }
    var marker = rawWord.charAt(rawWord.length - 1).toLowerCase();
    var hasToneMarker = Object.prototype.hasOwnProperty.call(TONE_INDEX, marker) || marker === "z";
    var source = hasToneMarker ? rawWord.substring(0, rawWord.length - 1) : rawWord;
    var transformed = applyCombinations(source);
    if (hasToneMarker) {
      transformed = applyTone(transformed, marker);
    }
    return transformed;
  }

  function currentWordBounds(text, caret) {
    var start = caret;
    while (start > 0 && isWordChar(text.charAt(start - 1))) {
      start--;
    }
    return { start: start, end: caret };
  }

  function processInputElement(target) {
    if (typeof target.selectionStart !== "number" || typeof target.selectionEnd !== "number") {
      return;
    }
    if (target.selectionStart !== target.selectionEnd) {
      return;
    }
    var value = target.value;
    var caret = target.selectionStart;
    var bounds = currentWordBounds(value, caret);
    var rawWord = value.substring(bounds.start, bounds.end);
    var converted = transformWord(rawWord);
    if (converted === rawWord) {
      return;
    }
    internalChange = true;
    target.value = value.substring(0, bounds.start) + converted + value.substring(bounds.end);
    var newCaret = bounds.start + converted.length;
    target.setSelectionRange(newCaret, newCaret);
    target.dispatchEvent(new Event("input", { bubbles: true }));
    internalChange = false;
  }

  function processContentEditable(target) {
    var selection = window.getSelection();
    if (!selection || selection.rangeCount === 0 || !selection.isCollapsed) {
      return;
    }
    var range = selection.getRangeAt(0);
    if (!target.contains(range.startContainer) || range.startContainer.nodeType !== Node.TEXT_NODE) {
      return;
    }
    var node = range.startContainer;
    var text = node.nodeValue || "";
    var caret = range.startOffset;
    var bounds = currentWordBounds(text, caret);
    var rawWord = text.substring(bounds.start, bounds.end);
    var converted = transformWord(rawWord);
    if (converted === rawWord) {
      return;
    }
    internalChange = true;
    node.nodeValue = text.substring(0, bounds.start) + converted + text.substring(bounds.end);
    var newRange = document.createRange();
    newRange.setStart(node, bounds.start + converted.length);
    newRange.collapse(true);
    selection.removeAllRanges();
    selection.addRange(newRange);
    target.dispatchEvent(new Event("input", { bubbles: true }));
    internalChange = false;
  }

  document.addEventListener("keydown", function (event) {
    skipNextInput = !!(event.ctrlKey || event.altKey || event.metaKey);
  }, true);

  document.addEventListener("compositionstart", function () {
    composing = true;
  }, true);

  document.addEventListener("compositionend", function () {
    composing = false;
  }, true);

  document.addEventListener("input", function (event) {
    if (internalChange || composing || mode !== "vi") {
      return;
    }
    var target = event.target;
    if (target && target.id === 'tse-input-test-textarea') {
      console.log('[TSE_INPUT_TEST] key/input captured in mode=vi');
    }
    if (skipNextInput) {
      skipNextInput = false;
      return;
    }
    if (event.inputType && event.inputType.indexOf("insertText") !== 0 && event.inputType !== "deleteContentBackward") {
      return;
    }
    var target = event.target;
    if (!isEditableTarget(target)) {
      return;
    }
    if (target.isContentEditable) {
      processContentEditable(target);
    } else {
      processInputElement(target);
    }
  }, true);

  window.TSEVietnameseInput = {
    __installed: true,
    setMode: function (nextMode) {
      mode = nextMode === "vi" ? "vi" : "en";
      window.__TSE_INPUT_MODE = mode;
      return mode;
    },
    getMode: function () {
      return mode;
    },
    isEnabled: function () {
      return mode === "vi";
    },
    transformWord: transformWord
  };
})();
