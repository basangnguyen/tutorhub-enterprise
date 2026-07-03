# QuizHub Quizizz-Inspired Game Development Plan

Date: 2026-07-02

## 1. Design read

Reading this as: product development plan for a desktop learning app tab, with a professional-but-playful quiz game experience inspired by Quizizz/Wayground, adapted to TutorHub's Java desktop + JCEF + cloud-first architecture.

This plan does not copy Quizizz assets, branding, sounds, icons, names, or visual identity. It extracts product patterns and converts them into TutorHub-owned mechanics.

## 2. Research summary from Quizizz / Wayground

Current Quizizz documentation is now under Wayground. Important product patterns:

1. Live session structure
   - Teacher-Led: teacher controls pacing and moves everyone question by question.
   - Student-Paced: learners move at their own pace.
   - Student-Paced includes Classic, Mastery Peak, Test, and Team modes.
   - Classic supports live leaderboard and extra player-vs-player features.
   - Test mode is no-frills and minimizes distraction.
   - Team mode combines individual answers into team performance.

2. Scoring model
   - Wayground separates academic accuracy points from gamified session scores.
   - Accuracy points represent learning correctness and can sync to reports/LMS.
   - Session scores drive leaderboard position and can be influenced by speed and power-ups.
   - Timer scoring: correct answer gives 600 score. With timer enabled and answers allowed after time ends, users can earn 0-400 extra score based on speed. Wrong answers and timed-out answers get 0.

3. Power-ups
   - Power-ups affect session scores, not accuracy points.
   - Available examples: Supersonic, Streak Booster, Gift, Double Jeopardy, 2X, 50-50, Eraser, Immunity, Time Freeze, Power Play, Streak Saver, Glitch.
   - Students can receive up to 3 power-ups, each one usable once.
   - Teachers can deploy some power-ups in Classic mode; teacher power-ups affect all active students and have cooldown.

4. Player-vs-player mechanics
   - Strike and Shield mode includes score attacks/protection: subtract points, halve score, swap score, block/redirect/absorb attacks.
   - This only affects session scores and is limited to active sessions.
   - A rebound protection exists to reduce repeated targeting of the same player.

5. Mastery Peak
   - Students climb toward a mastery goal.
   - Incorrect questions reappear after some time.
   - Students encounter break rooms with power-ups, safe choices, or mini-games.
   - Teacher dashboard shows progress, reattempts, mastery count, and tiered leaderboard.
   - Awards at the end can include top scorer, fastest climber, obstacle-related awards, and reattempt-related awards.

6. Mini-games and early finisher flow
   - Brain Gym gives early finishers a memory mini-game while waiting.
   - Mastery Peak mini-games include circle drawing, memory matching, and swarm deflection.
   - Playground / WayArena is real-time multiplayer: students answer questions to earn spell ammo, then move in an arena and cast spells. It is live-only and keyboard-oriented.

7. Themes, avatars, media
   - Themes can include unique music, memes, and graphics.
   - Qbits are customizable student avatars that react to answers and appear in lobbies, leaderboards, and results.
   - Audio mute and reduced disruption controls are important for classroom use.

8. Anti-cheating
   - Anti-Cheating Monitor can warn/report tab switch, fullscreen exit, copy/paste, right click, window resize, web extension interaction, and similar suspicious actions.
   - For TutorHub desktop, this overlaps with the Secure Exam Mode roadmap and should not be mixed into the casual QuizHub game MVP.

Key sources:

- Wayground help home: https://help.wayground.com/support/home
- Live Session Modes: https://help.wayground.com/support/solutions/articles/158000404918-live-session-modes-on-wayground
- Grade Questions Using Timer: https://help.wayground.com/support/solutions/articles/158000404053-grade-questions-using-timer
- Accuracy Measurement: https://help.wayground.com/support/solutions/articles/158000404051-understand-how-accuracy-is-measured-on-wayground
- Power-Ups: https://help.wayground.com/support/solutions/articles/158000404941-power-ups-their-types
- Strike and Shield: https://help.wayground.com/support/solutions/articles/158000404931-boost-engagement-with-player-vs-player-mode-strike-and-shield
- Mastery Peak: https://help.wayground.com/support/solutions/articles/158000404926-host-an-assessment-quiz-in-mastery-peak-mode
- Mastery Peak Mini Games: https://help.wayground.com/support/solutions/articles/158000404927-mini-games-on-the-mastery-peak-game-mode
- Brain Gym: https://help.wayground.com/support/solutions/articles/158000404928-brain-gym-mini-games-for-early-finishers-
- Qbits Avatars: https://help.wayground.com/support/solutions/articles/158000404950-introducing-qbits-customizable-avatars-for-students-on-wayground-
- Playground: https://help.wayground.com/support/solutions/articles/158000451315-about-playground
- WayArena: https://help.wayground.com/support/solutions/articles/158000451316-wayarena

## 3. Current QuizHub state

Current code already has a useful foundation:

- Main UI is `src/main/resources/tse/quiz.html`, loaded by JCEF inside `QuizHubTab`.
- Java bridge uses `window.cefQuery` through `QuizHubCefRouterHandler`.
- Deck list and deck details come from `LIST_DECKS` and `GET_DECK:<deckId>`.
- Deck storage is cloud-first via Backblaze B2, with local cache fallback.
- Imported decks use `QuizHubDeck`, `QuizHubQuestion`, `QuizHubDeckSummary`.
- Attempts and best scores exist in `QuizHubAttemptService`, stored as JSON locally.
- Current quiz modes:
  - Study
  - Exam
  - Flashcards
  - Game
- Current game mode already includes:
  - Question timer
  - Score HUD
  - Answer tiles
  - WebAudio sound effects
  - Anime.js effects
  - canvas-confetti effects
  - Streak and combo multiplier
  - Result screen with score, accuracy, badges, retry wrong questions
- Current game score constants in `quiz.html`:
  - Base correct score: 600
  - Max speed bonus: 400
  - Streak bonus step: 30
  - Streak bonus cap: 150
  - Combo starts after streak 3
  - Combo grows by 0.05 and caps at 1.25

Current gaps:

- Most game scoring is client-only JS and not saved as detailed score events.
- `QuizHubAttempt` stores only broad score, duration, answer correctness, selected options, and timeMs.
- No power-up model yet.
- No theme pack model yet.
- No avatar/profile model yet.
- No per-question speed/score breakdown in report.
- No distinction between academic accuracy points and gamified session score.
- No deterministic Java-side score engine test suite.
- No classroom/live session server, so real multiplayer should be deferred.

## 4. Product principles for TutorHub QuizHub

1. Learning result first
   - Accuracy points are the source of truth for academic progress.
   - Game score is motivational only.

2. Single-player first, multiplayer later
   - Build polished local game mechanics before adding server complexity.
   - Classroom live mode requires session server, presence, sync, and teacher dashboard.

3. Deterministic scoring
   - The score formula must be deterministic and testable.
   - JS can render instant feedback, but Java should own canonical persistence and validation.

4. Offline-compatible assets
   - Game should run inside JCEF without CDN.
   - Sound, theme, image, animation assets should be bundled locally.

5. No brand copying
   - Use TutorHub names, TutorHub visual system, TutorHub mascot/avatar direction.
   - Do not copy Qbits, Quizizz memes, names, sounds, UI exact layouts, or assets.

6. Accessibility controls
   - Mute audio.
   - Reduce motion.
   - High contrast answer colors.
   - Keyboard navigation for answer selection.

## 5. Target feature set

### 5.1 Modes

P0, keep and polish:

- Practice: current Study mode. Instant feedback optional.
- Test: current Exam mode. No power-ups by default, no distracting effects, timer optional.
- Classic Game: one-player Quizizz-like mode with score, speed bonus, streak, power-ups, badges.
- Flashcard: current flip cards.

P1:

- Mastery Climb: retry wrong questions through levels until reaching a mastery goal.
- Smart Flashcard: Leitner/spaced-repetition review.
- Brain Gym Match: early-finisher or warm-up memory/matching mini-game.

P2:

- Team Challenge local/async: team score from stored attempts or same-device rotation.
- Teacher-led presenter mode: projected question stage, students answer on same device later only if live server exists.

P3:

- Live multiplayer with lobby, join code, leaderboard, teacher dashboard.
- Arena-style mini-game only after session infrastructure exists.

### 5.2 Score system

Create two separate score concepts:

1. Accuracy points
   - Per question default: 1 point.
   - Future: allow 1-20 points per question.
   - Correct single-answer: full points.
   - Correct multi-answer: full points only if exact set match in MVP.
   - Wrong or timed out: 0.
   - Not affected by speed, streak, combo, power-ups, or visual effects.

2. Session score
   - Used for motivation, badges, leaderboard, and personal best.
   - Correct answer baseline: 600.
   - Speed bonus: `round(400 * clamp(timeLeftMs / timeLimitMs, 0, 1))`.
   - Wrong answer: 0.
   - Timed-out answer: 0.
   - Optional streak bonus:
     - Trigger after 3 consecutive correct answers.
     - Add fixed bonus and/or multiplier.
     - Keep current cap to avoid runaway scores.
   - Optional power-up multiplier:
     - Applied only to session score.
     - Record the final formula into `GameScoreEvent` for audit.

Recommended v1 formula:

```text
if wrong or timedOut:
  accuracyPoints = 0
  sessionScore = 0
else:
  accuracyPoints = questionPoints
  base = 600
  speedBonus = round(400 * timeLeftMs / timeLimitMs)
  streakMultiplier = min(1.25, 1 + max(0, streakAfterAnswer - 2) * 0.05)
  powerMultiplier = product(activeScoreMultipliers)
  flatBonus = activeFlatBonuses + streakFlatBonus
  sessionScore = round((base + speedBonus) * streakMultiplier * powerMultiplier + flatBonus)
```

### 5.3 Power-ups

P0 student power-ups:

- 2X: doubles session score for one correct answer.
- 50-50: hides half of incorrect options.
- Eraser: hides one incorrect option.
- Time Freeze: pauses timer for the current question.
- Immunity: if first answer is wrong, allow one retry for the same question.
- Streak Saver: protects streak once after a wrong answer.
- Supersonic: 1.5x score for 20 seconds if user keeps answering quickly.

P1 student power-ups:

- Double Jeopardy: 2x if correct, 0 and streak loss if wrong.
- Gift: not useful in solo mode; defer until team/live mode.
- Glitch: visual-only classroom fun; use sparingly and make it disable-able.
- Power Play: global boost, defer until live/teacher mode.

P2 teacher/live power-ups:

- Teacher 2X for all active students.
- Teacher Supersonic window.
- Teacher Glitch effect.
- Cooldown: at least 90 seconds.
- Never affect academic accuracy.

Power-up data model:

```text
QuizHubPowerUp
  id
  type
  ownerType: student | teacher | system
  targetScope: self | player | all | question
  durationMs
  usesRemaining
  consumedAt
  effectConfig
```

### 5.4 Mastery Climb

TutorHub version of Mastery Peak should be "Mastery Climb":

- Goal: reach target accuracy, e.g. 80%, 90%, or 100%.
- Wrong questions re-enter the queue after a delay or after 2-3 other questions.
- Each level contains a small batch of questions.
- At level end, show a break room:
  - Pick a booster.
  - Pick a safe route.
  - Play a 10-20 second mini-game.
- Badges:
  - Fast Climber: best speed among personal attempts.
  - Steady Climber: no streak broken for N questions.
  - Comeback: high improvement after retries.
  - Mastery: reached target accuracy.

No live mountain leaderboard in P0 because it needs multiplayer.

### 5.5 Mini-games

Use small standalone games that reuse existing question data:

1. Match Sprint
   - Match question/term to correct answer.
   - Good for definitions and short MCQ stems.
   - Time-based score.

2. Memory Grid
   - 4x4 card grid.
   - Pairs: term and correct answer.
   - Good as Brain Gym or warm-up.

3. Circle Accuracy
   - Canvas challenge, not directly academic.
   - Use as break-room game only.

4. Defense Clicker
   - Tap/click incoming objects, similar to swarm deflection.
   - Keep it short to avoid overshadowing learning.

Avoid building arena multiplayer until a live session backend exists.

### 5.6 Themes, graphics, sound

Theme pack structure:

```text
QuizHubThemePack
  id
  name
  palette
  background
  answerTileStyle
  avatarStyle
  musicLoop
  sfxMap
  reducedMotionVariant
```

P0 themes:

- Focus White: current clean white UI.
- Arcade Light: brighter answer tiles, more motion.
- Calm Study: minimal motion and muted sound.

P1 themes:

- Cyber Lab: for computer/security subjects.
- Summit: for Mastery Climb.
- Classic Classroom: low-distraction for teacher projection.

Audio plan:

- Keep WebAudio oscillator tones in P0 because it is zero-asset and already works.
- Add a `QuizHubAudioBus` JS module:
  - event names: answer.correct, answer.wrong, streak.fire, powerup.use, timer.tick, result.win, result.badge.
  - global mute.
  - per-theme sound mapping.
  - reduced-motion/reduced-sound profile.
- Later add Howler.js only when using real local sound sprites.

Graphics plan:

- Keep CSS/Anime.js/canvas-confetti for P0.
- Build reusable animation tokens:
  - answer-enter
  - answer-correct
  - answer-wrong
  - score-float
  - streak-pulse
  - powerup-activate
  - result-countup
- Do not animate layout dimensions; use transform/opacity.

### 5.7 Avatars and rewards

TutorHub avatar system should not copy Qbits.

P0:

- Use simple generated profile icon from user initials or existing TutorHub mascot style.
- Result badges only.

P1:

- Add `QuizHubPlayerProfile`:
  - displayName
  - avatarId
  - color
  - unlockedBadges
  - coins
  - settings: sound/motion/theme

P2:

- Add avatar shop if the product needs retention mechanics.
- Coins must be earned from learning activity and should not become a distraction.

### 5.8 Reports and analytics

MVP report fields:

- deckId
- mode
- startedAt
- finishedAt
- durationSeconds
- totalCount
- correctCount
- accuracyPercent
- accuracyPoints
- maxAccuracyPoints
- sessionScore
- bestStreak
- averageResponseMs
- per-question:
  - questionId
  - selected options
  - correct
  - timeMs
  - accuracyPoints
  - sessionScore
  - speedBonus
  - streakBefore
  - streakAfter
  - powerUpsUsed

Reports UI:

- Overview: accuracy, score, time, streak, best attempt.
- Questions: per-question correctness and time.
- Topics: accuracy by topic/difficulty.
- Growth: last N attempts over time.
- Game: power-up usage, streaks, speed distribution.

## 6. Architecture plan

### 6.1 Keep JCEF, split JS into modules

Current `quiz.html` is too large and mixes UI, data mapping, scoring, sound, game logic, and report rendering.

Recommended split, still bundled locally:

```text
src/main/resources/tse/
  quiz.html
  quizhub/
    core/
      bridge.js
      state.js
      scoring.js
      shuffle.js
      telemetry.js
    ui/
      deckMenu.js
      quizMode.js
      gameMode.js
      resultView.js
      reportsView.js
    game/
      powerups.js
      audioBus.js
      animations.js
      themes.js
      masteryClimb.js
      miniGames.js
```

Do this incrementally. Do not rewrite everything in one patch.

### 6.2 Java services

Add:

```text
QuizHubScoringService
QuizHubGameSessionService
QuizHubPowerUpService
QuizHubProgressService
QuizHubReportService
QuizHubThemeService
```

Keep:

```text
QuizHubDeckService
QuizHubAttemptService
QuizHubExcelImportService
QuizHubBridge
QuizHubCefRouterHandler
```

Bridge additions:

```text
START_GAME_SESSION:<deckId>|<optionsJson>
SAVE_GAME_ATTEMPT:<sessionJson>
GET_GAME_REPORT:<deckId>
GET_PLAYER_PROFILE:
SAVE_PLAYER_PROFILE:<json>
GET_THEME_PACKS
SAVE_QUESTION_PROGRESS:<json>
```

### 6.3 Data model additions

```text
QuizHubGameSession
  id
  deckId
  mode
  options
  startedAt
  finishedAt
  playerId
  summary
  questionResults
  scoreEvents
  powerUpEvents

QuizHubQuestionResult
  questionId
  selected
  correct
  timedOut
  responseMs
  accuracyPoints
  maxAccuracyPoints
  sessionScore
  speedBonus
  streakBefore
  streakAfter
  powerUpsUsed

QuizHubScoreEvent
  questionId
  baseScore
  speedBonus
  streakMultiplier
  powerMultiplier
  flatBonus
  finalScore
  reason

QuizHubQuestionProgress
  questionId
  deckId
  leitnerBox
  nextReviewAt
  correctStreak
  wrongCount
  lastSeenAt

QuizHubAchievement
  id
  type
  earnedAt
  payload
```

### 6.4 Storage

Short term:

- JSON files are acceptable for attempts and profile data.
- Keep cloud deck storage as currently implemented.

Medium term:

- Move reports/progress to SQLite if query volume grows.
- Keep cloud sync as an explicit service, not hidden in UI.

Recommended storage paths:

```text
%APPDATA%/TutorHub/quizhub/
  decks/
  attempts/
  game-sessions/
  progress/
  profiles/
  reports/
  themes/
```

## 7. Development roadmap

### Phase 0: Stabilize current game foundation

Goal:

- Keep current Game mode working while preparing for deeper features.

Work:

- Document current score formula in code comments and docs.
- Extract score constants into one object.
- Add JS unit-like test page or Java scoring tests for formula parity.
- Ensure mute, reduced motion, and no-CDN behavior.
- Save current game attempt with detailed per-question time and score.

Files/modules:

- `quiz.html`
- `QuizHubAttempt`
- `QuizHubAnswerRecord`
- `QuizHubAttemptService`
- new `QuizHubScoringService`

Acceptance:

- Correct answer at full time gives 1000 before streak/power-ups.
- Correct answer at zero time gives 600.
- Wrong/timed-out gives 0.
- Attempt JSON contains responseMs and score details.

### Phase 1: Formal scoring and reporting model

Goal:

- Separate academic accuracy from session score.

Work:

- Add accuracy points and session score fields.
- Add `QuizHubQuestionResult`.
- Persist `GameSession` separately from simple attempts.
- Result screen shows both:
  - "Do chinh xac" / accuracy
  - "Diem game" / session score
- Add local report summary.

Acceptance:

- Power-ups cannot alter accuracy.
- Reports can sort by accuracy, score, speed, and topic.

### Phase 2: Classic Game v1

Goal:

- Build a polished single-player game mode comparable in feel to a modern quiz game.

Work:

- Add pre-game setup:
  - time per question
  - number of questions
  - difficulty/topic filter
  - power-ups on/off
  - sound/motion profile
- Add HUD:
  - score
  - timer ring
  - question progress
  - streak
  - active power-ups
- Add answer state:
  - fast tap
  - keyboard selection
  - multi-answer confirm
  - correct/wrong reveal
  - explanation panel
- Add result:
  - count-up score
  - accuracy
  - best streak
  - average response time
  - badges
  - retry wrong questions

Acceptance:

- User can complete a full game with no network after deck is cached.
- No layout overlap at desktop and small window sizes.
- Audio can be muted.
- Game remains usable with reduced motion.

### Phase 3: Student power-ups v1

Goal:

- Add meaningful solo-mode power-ups without live server dependency.

Work:

- Add power-up inventory with max 3 random power-ups per run.
- Implement:
  - 2X
  - 50-50
  - Eraser
  - Time Freeze
  - Immunity
  - Streak Saver
  - Supersonic
- Add power-up events to saved session.
- Show small tutorial on first use.

Acceptance:

- Each power-up works once and records event data.
- Power-ups affect session score only.
- Multi-answer questions handle 50-50/Eraser safely.

### Phase 4: Themes, sound, avatars, achievements

Goal:

- Make QuizHub feel like a finished game surface while staying professional.

Work:

- Add local theme packs.
- Add `QuizHubAudioBus`.
- Add badge/achievement registry.
- Add profile avatar placeholder.
- Add reward summary after game.

Acceptance:

- Theme can be changed without reloading app.
- No remote asset load.
- Badges are deterministic and saved.

### Phase 5: Mastery Climb

Goal:

- Convert "retry wrong questions" into a mastery learning mode.

Work:

- Add mastery goal setup.
- Add reattempt queue.
- Add levels and break-room decisions.
- Add mini power-up rewards between levels.
- Add mastery-specific report:
  - repeated questions
  - improvement after retry
  - average attempts per question

Acceptance:

- Wrong questions reappear.
- User can reach mastery target.
- Report shows improvement, not only final score.

### Phase 6: Brain Gym and mini-games

Goal:

- Add lightweight mini-games that reuse quiz data.

Work:

- Implement Match Sprint.
- Implement Memory Grid.
- Optionally implement Circle Accuracy as non-academic break game.
- Use same audio/theme system.

Acceptance:

- Mini-games are optional and never block quiz completion.
- Report separates mini-game score from academic accuracy.

### Phase 7: Live classroom mode

Goal:

- Build the multiplayer layer only after single-player is stable.

Prerequisites:

- Session server or local LAN host.
- Join code.
- Presence tracking.
- Teacher dashboard.
- Reconnect handling.
- Anti-abuse rules for player-vs-player mechanics.

Features:

- Lobby with avatars.
- Student-paced live Classic.
- Live leaderboard.
- Teacher-deployed power-ups.
- Team mode.
- Strike/Shield-like mechanics under TutorHub-owned names.
- Spectator/projector view.

Acceptance:

- Two or more clients can join the same session.
- Teacher can start/end session.
- Leaderboard updates in real time.
- Disconnect and reconnect do not corrupt reports.

## 8. Technical risks

1. Large `quiz.html`
   - Risk: hard to maintain and test.
   - Mitigation: split modules incrementally.

2. Scoring only in JS
   - Risk: hard to trust reports and hard to test.
   - Mitigation: Java scoring service and persisted score events.

3. Power-up edge cases
   - Risk: 50-50 or Eraser can break multi-answer questions.
   - Mitigation: define safe option removal rules and test each question type.

4. Audio annoyance
   - Risk: classroom/game sound can distract.
   - Mitigation: global mute, low-sound theme, reduced-sound setting.

5. Motion/performance in JCEF
   - Risk: heavy animation causes stutter.
   - Mitigation: use transform/opacity, reduce particles, respect reduced motion.

6. Multiplayer scope
   - Risk: live mode is a separate product.
   - Mitigation: defer until session backend exists.

7. Secure exam overlap
   - Risk: anti-cheating belongs to Secure Exam Mode, not casual game mode.
   - Mitigation: keep QuizHub Test mode lightweight unless explicitly entering exam mode.

## 9. Immediate next tasks

Recommended order:

1. Add a formal score model and save detailed game attempt data.
2. Extract current JS game scoring into a small module.
3. Add Java tests for score formula.
4. Add result report with accuracy vs game score split.
5. Add power-up inventory UI with only 2X and Time Freeze first.
6. Add 50-50/Eraser after option-hiding logic is tested.
7. Add Mastery Climb after retry-wrong data is persisted.

## 10. Definition of done for QuizHub Game v1

- User can select any imported deck and play Classic Game.
- Game score uses 600 + 0-400 speed bonus.
- Academic accuracy is displayed separately from game score.
- At least 4 power-ups work and are persisted.
- Sound, motion, and theme can be controlled.
- Result screen includes accuracy, score, streak, average time, badges, and retry wrong.
- Attempts are saved with per-question telemetry.
- Reports can show weak questions/topics.
- No CDN or remote assets required for gameplay.
- Existing Study, Exam, Flashcard flows remain stable.
