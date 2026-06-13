# TSE Vietnamese Input Engine Research

## 1. Scope
The scope of this research is to investigate algorithms for correct tone placement in Vietnamese input engines, specifically addressing tone placement issues on diphthongs and triphthongs (e.g., `oa`, `oe`, `uy`, `ươ`) in the TutorHub Secure Exam environment without relying on external OS-level keyboards.

## 2. Repositories Cloned
To understand how existing open-source Vietnamese input engines handle tone placement, the following repositories were cloned:
- [ANVIM](https://github.com/vinakey/anvim.git)
- [AVIM](https://github.com/1ec5/avim.git)
- [VNKeys-JS](https://github.com/mikeitpro/VNKeys-JS.git)

## 3. License Review
- **ANVIM**: MIT License. Safe to study and adapt algorithms.
- **AVIM**: MPL-2.0 / GPL-2.0 / LGPL-2.1 tri-license. Safe for algorithmic study, but direct code copying is avoided to maintain project licensing simplicity.
- **VNKeys-JS**: MIT License. Safe to study and adapt.

## 4. Files/Functions Studied
- `avim/content/avim.js` and `avim/components/transformer.js`: The `findC`, `main`, and `tr` functions were studied to understand how AVIM identifies the correct vowel to place a tone mark.
- `avim/content/frame.js`: Studied `handleHtmlInput` and `splice` to see how caret tracking and `selectionStart`/`selectionEnd` are handled cross-browser.

## 5. Telex Rules Learned
- Telex uses specific keys (`s`, `f`, `r`, `x`, `j`) for tones.
- The `z` key removes the previously applied tone.
- Double letters (`aa`, `ee`, `oo`) and `w` combinations (`aw`, `ow`, `uw`) form base diacritics.
- A critical rule is that combinations of characters sometimes shift the main vowel, requiring recalculation of the tone position.

## 6. Tone Placement Rules Learned
The placement of the tone mark depends on the number of vowels in a syllable and whether the syllable ends with a consonant:
1. **Single Vowel**: The tone is placed on the only vowel.
2. **Two Vowels**:
   - If the syllable **ends with a consonant** (e.g., *toán*, *hoàng*, *tiếng*, *đường*), the tone is placed on the **second vowel**.
   - If the syllable **does not end with a consonant** (i.e., the second vowel is the last character of the word), the tone is placed on the **first vowel** (e.g., *hòa*, *xòe*, *thúy*, *múa*, *chìa*, *bài*), with two major exceptions:
     - `uê` (e.g., *huệ*, *thuế*) -> tone on `ê` (second vowel).
     - `uơ` (e.g., *thuở*) -> tone on `ơ` (second vowel).
3. **Three Vowels**:
   - For `uyê` (e.g., *tuyến*, *truyện*), the tone is placed on `ê` (the third vowel).
   - For other combinations (e.g., *ngoáy*, *oải*, *lưới*, *khuỷu*), the tone is placed on the **middle (second) vowel**.
4. **`qu` and `gi` Exceptions**:
   - The `u` in `qu` and `i` in `gi` are generally treated as consonant modifiers rather than standalone vowels if followed by another vowel. Therefore, they are excluded from the main vowel counting logic.

## 7. Browser Input/Caret Handling Notes
- `selectionStart` and `selectionEnd` must be meticulously tracked.
- Using standard `replace` and adjusting `setSelectionRange` is the safest way to transform words mid-typing without causing the caret to jump unexpectedly.
- Dispatching `input` events ensures modern web frameworks detect the changes.

## 8. What Can Be Applied to TSE
- The structural algorithm of tracking vowels in a syllable and applying the tone based on vowel count and presence of a final consonant.
- The exceptions for `qu` and `gi`.
- The handling of the `z` key for tone removal.

## 9. What Should Not Be Copied
- AVIM's extensive cross-browser patching (e.g., handling specific bugs in old versions of Midas or Firefox extensions).
- Heavy DOM overlays that try to display syllables independently (not needed for simple text inputs).
- Any global `window` hijacking that interferes with the JCEF lockdown environment.

## 10. Proposed Algorithm Upgrade
The `chooseToneIndex` function in `tse-vietnamese-input-engine.js` has been completely rewritten:
1. It loops through the word to find all vowels, explicitly skipping `u` after `q` and `i` after `g` if they are followed by another vowel.
2. It counts the resulting vowels.
3. If length is 1, it returns the vowel.
4. If length is 2, it checks if the second vowel is the last character of the word. If not (meaning there's a final consonant), it places the tone on the second vowel. If yes, it places the tone on the first vowel, unless the pair is `uê` or `uơ`, in which case it chooses the second.
5. If length is 3, it checks for `uyê` to place on the third vowel, otherwise places on the middle vowel.

## 11. Test Cases
The new algorithm correctly handles the following requirements:
- `xoef` -> `xòe`
- `hoef` -> `hòe`
- `khoer` -> `khỏe`
- `hoaf` -> `hòa`
- `hoangf` -> `hoàng`
- `toans` -> `toán`
- `thuys` -> `thúy`
- `tieengs` -> `tiếng`
- `Vieetj` -> `Việt`
- `duowngf` -> `đường`
- `truowngf` -> `trường`
- `baif` -> `bài`
- `giaf` -> `già`
- `giof` -> `gió`
- `giux` -> `giữ`
- `quaf` -> `quà`
- `quef` -> `quẻ`
- `quys` -> `quý`
- `tuyeesn` -> `tuyến`
- `khuyu` -> `khuyu`
- `khuyur` -> `khuỷu`
- `giowis` -> `giới`
- `hieeur` -> `hiểu`
