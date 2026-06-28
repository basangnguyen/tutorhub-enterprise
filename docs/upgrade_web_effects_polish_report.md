# Upgrade Web Effects Polish Report

## 1. Files read

- `src/main/resources/upgrade-web/upgrade.html`
- `src/main/resources/upgrade-web/upgrade.css`
- `src/main/resources/upgrade-web/upgrade.js`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/UpgradeTab.java` bridge section only

## 2. Effects added in `upgrade.js`

- Page entrance orchestration for header, feature bar, billing toggle, pricing cards, and guarantee footer.
- Lightweight ambient DOM particles behind the page.
- 3D tilt/parallax hover for pricing cards.
- CTA magnetic hover, pressed state, and click ripple.
- Animated price count-up on load and billing-cycle changes.
- Billing toggle refresh animation and one-shot saving badge pulse.
- Micro interactions for feature and guarantee icons.
- Reduced-motion detection through `prefers-reduced-motion`.

## 3. CSS/HTML edits

- Edited `upgrade.css` because the new JavaScript effects need state classes, keyframes, ripple styling, ambient dots, card shine, and reduced-motion fallbacks.
- Did not edit `upgrade.html`; the existing semantic structure and inline handlers were sufficient.

## 4. `UpgradeTab.java`

- Not edited.
- Read only to verify the Java bridge contract.

## 5. Bridge kept

The existing bridge names and payload direction are preserved:

- `window.TutorHubUpgrade.selectPlan(planName, amount)`
- `window.TutorHubUpgrade.goBack()`
- `window.TutorHubUpgrade.toggleBilling(type)`

Plan payloads remain based on the existing card data:

- `Basic`
- `Premium`
- `VIP`

Amounts still come from the existing `data-monthly-amount` / `data-yearly-amount` attributes.

## 6. CDN / online assets

- No CDN added.
- No external script added.
- No online runtime asset added.

## 7. Reduced motion

- `upgrade.js` checks `prefers-reduced-motion`.
- `upgrade.css` disables particles/ripples and shortens animation/transition durations under reduced motion.

## 8. JS syntax check

Command:

```powershell
node --check .\src\main\resources\upgrade-web\upgrade.js
```

Result: passed.

## 9. Build result

Initial sandbox build failed because Maven could not access Maven Central:

```text
Permission denied: getsockopt
```

Re-run with approved external network access:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests
```

Result: BUILD SUCCESS.

Output jar:

```text
target/TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 10. `update.jar` copied

Copied:

```text
target/TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar
```

to:

```text
HF_UPLOAD/update.jar
```

Verified jar entries:

```text
upgrade-web/
upgrade-web/upgrade.css
upgrade-web/upgrade.html
upgrade-web/upgrade.js
```

## 11. Risks

- Runtime visual QA with `java -jar HF_UPLOAD/update.jar` was not executed here because the requested `taskkill java/javaw` step can close active Java tools such as NetBeans or other user processes.
- Effects are intentionally vanilla JavaScript/CSS and lightweight, but final smoothness should still be checked inside the actual JavaFX WebView because JavaFX WebView rendering differs from modern Chromium.
