import re

file_path = "d:/Ban_sao_du_an/src/main/resources/tse/quiz.html"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add CSS
css_code = """<style id="game-mode-style">
  #game-mute-btn{background:none;border:none;font-size:18px;cursor:pointer;padding:4px;margin-left:auto;}
  .game-screen{padding:16px;max-width:560px;margin:0 auto;}
  .game-intro-card,.game-result-card{background:#fff;border:1px solid var(--line);border-radius:16px;padding:28px 22px;text-align:center;box-shadow:0 3px 14px rgba(0,0,0,.07);}
  .game-intro-emoji{font-size:46px;}
  .game-intro-title{font-size:20px;font-weight:700;margin-top:6px;}
  .game-intro-sub{color:#777;font-size:13px;margin-top:4px;min-height:16px;}
  .game-time-pick{margin:22px 0 18px;}
  .game-time-label{font-size:13px;color:#555;margin-bottom:8px;}
  .game-start-btn{width:100%;padding:13px;font-size:16px;border-radius:10px;}

  #game-countdown{display:flex;align-items:center;justify-content:center;min-height:60vh;}
  .game-countdown-num{font-size:96px;font-weight:800;color:var(--blue);animation:gcPulse .45s ease-out;}
  @keyframes gcPulse{from{transform:scale(.4);opacity:0;}to{transform:scale(1);opacity:1;}}

  .game-hud{display:flex;align-items:center;justify-content:space-between;gap:10px;}
  .game-hud-item{text-align:center;min-width:60px;}
  .game-hud-label{font-size:11px;color:#999;}
  .game-hud-value{font-size:20px;font-weight:700;font-variant-numeric:tabular-nums;}
  .game-timer-ring{position:relative;width:64px;height:64px;flex-shrink:0;}
  .game-timer-ring svg{width:100%;height:100%;transform:rotate(-90deg);}
  .ring-bg{fill:none;stroke:var(--line);stroke-width:8;}
  .ring-fg{fill:none;stroke:var(--green);stroke-width:8;stroke-linecap:round;stroke-dasharray:276.46;stroke-dashoffset:0;transition:stroke .2s;}
  .game-timer-num{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;font-size:18px;font-weight:700;font-variant-numeric:tabular-nums;}

  .game-streak{display:flex;align-items:center;justify-content:center;gap:4px;margin:10px auto 0;width:fit-content;background:#fff4e0;color:#b8590a;font-weight:700;font-size:14px;padding:5px 14px;border-radius:20px;}
  .game-streak.fire{background:#ffe1c4;box-shadow:0 0 0 2px #ffb35c inset;animation:gFire .6s ease-in-out infinite alternate;}
  @keyframes gFire{from{transform:scale(1);}to{transform:scale(1.08);}}

  .game-qbox{background:#fff;border:1px solid var(--line);border-radius:14px;padding:18px 16px;margin:14px 0;min-height:70px;display:flex;flex-direction:column;justify-content:center;box-shadow:0 2px 10px rgba(0,0,0,.05);}
  .game-q{font-size:17px;font-weight:700;line-height:1.5;text-align:center;}
  .game-multi-hint{font-size:12px;color:var(--blue);text-align:center;margin-top:8px;}

  .game-tiles{display:grid;grid-template-columns:1fr 1fr;gap:10px;}
  .game-tile{border:none;border-radius:12px;padding:16px 14px;font-size:14.5px;font-weight:600;color:#fff;text-align:left;cursor:pointer;line-height:1.4;min-height:64px;display:flex;align-items:center;gap:8px;transition:transform .1s,opacity .15s;box-shadow:0 2px 6px rgba(0,0,0,.12);}
  .game-tile:active{transform:scale(.97);}
  .game-tile .gt-letter{font-weight:800;opacity:.85;flex-shrink:0;}
  .game-tile.gt-0{background:#d9534f;} .game-tile.gt-1{background:#1a4f8b;}
  .game-tile.gt-2{background:#b85b14;} .game-tile.gt-3{background:#2f8f4e;}
  .game-tile.gt-4{background:#6a4fb8;} .game-tile.gt-5{background:#1b8f9e;}
  .game-tile.gt-picked{box-shadow:0 0 0 3px #fff inset, 0 2px 6px rgba(0,0,0,.12);}
  .game-tile.gt-correct{background:#2f8f4e !important;box-shadow:0 0 0 3px #b8e6c4 inset;}
  .game-tile.gt-wrong{background:#9a9a9a !important;opacity:.55;}
  .game-tile:disabled{cursor:default;}
  .game-confirm-btn{display:block;width:100%;margin-top:12px;}

  .game-feedback{text-align:center;margin-top:14px;font-size:15px;font-weight:700;padding:10px;border-radius:10px;}
  .game-feedback.ok{background:#eaf8ee;color:#2b6b2b;}
  .game-feedback.bad{background:#fdecec;color:#a33;}
  .game-feedback .pts{display:block;font-size:13px;font-weight:600;margin-top:2px;}

  .game-result-emoji{font-size:50px;}
  .game-result-score{font-size:40px;font-weight:800;color:var(--blue);margin-top:4px;}
  .game-result-sub{color:#888;font-size:13px;margin-top:-4px;}
  .game-result-stats{display:flex;justify-content:center;gap:22px;margin:18px 0;flex-wrap:wrap;}
  .game-result-stats .v{font-size:18px;font-weight:700;}
  .game-result-stats .l{font-size:11px;color:#999;}
  .game-result-verdict{font-size:14px;color:#555;margin-bottom:6px;}

  .card-actions{display:flex;gap:8px;margin-top:12px;align-items:center;}
  .card-actions .go{margin-top:0;}
  .go-game{border:none;cursor:pointer;font-family:inherit;font-size:13px;background:#6a4fb8;color:#fff;border-radius:8px;padding:7px 12px;font-weight:600;}
</style>
</head>"""
content = content.replace("</head>", css_code)

# 2. Add HTML
html_code = """<!-- GAME MODE (kiểu Quizizz/Wayground) -->
<div id="game" class="hidden">
  <div class="quiz-header">
    <div class="top">
      <button class="back-btn" onclick="gameExit()">← Thoát</button>
      <h1 id="game-title">Chế độ Trò Chơi</h1>
      <button id="game-mute-btn" onclick="gameToggleMute()" title="Tắt/mở âm thanh">🔊</button>
    </div>
  </div>

  <div id="game-intro" class="game-screen">
    <div class="game-intro-card">
      <div class="game-intro-emoji">🎮</div>
      <div class="game-intro-title">Sẵn sàng chơi?</div>
      <div class="game-intro-sub" id="game-intro-sub"></div>
      <div class="game-time-pick">
        <div class="game-time-label">⏱ Thời gian mỗi câu</div>
        <div class="seg" id="seg-game-time">
          <button data-v="10" onclick="gameSetTime(10)">10s</button>
          <button data-v="15" onclick="gameSetTime(15)">15s</button>
          <button class="on" data-v="20" onclick="gameSetTime(20)">20s</button>
          <button data-v="30" onclick="gameSetTime(30)">30s</button>
        </div>
      </div>
      <button class="btn btn-primary game-start-btn" onclick="gameBeginRound()">🚀 Bắt đầu</button>
    </div>
  </div>

  <div id="game-countdown" class="game-screen hidden">
    <div class="game-countdown-num" id="game-countdown-num">3</div>
  </div>

  <div id="game-play" class="game-screen hidden">
    <div class="game-hud">
      <div class="game-hud-item">
        <div class="game-hud-label">Điểm</div>
        <div class="game-hud-value" id="game-score">0</div>
      </div>
      <div class="game-timer-ring">
        <svg viewBox="0 0 100 100">
          <circle class="ring-bg" cx="50" cy="50" r="44"></circle>
          <circle class="ring-fg" id="game-ring-fg" cx="50" cy="50" r="44"></circle>
        </svg>
        <div class="game-timer-num" id="game-timer-num">20</div>
      </div>
      <div class="game-hud-item">
        <div class="game-hud-label">Câu</div>
        <div class="game-hud-value" id="game-progress">1/10</div>
      </div>
    </div>
    <div class="game-streak hidden" id="game-streak">🔥 <span id="game-streak-num">0</span></div>

    <div class="game-qbox">
      <div class="game-q" id="game-q"></div>
      <div class="game-multi-hint hidden" id="game-multi-hint">Chọn tất cả đáp án đúng rồi bấm Xác nhận</div>
    </div>
    <div class="game-tiles" id="game-tiles"></div>
    <button class="btn btn-primary game-confirm-btn hidden" id="game-confirm-btn" onclick="gameConfirmMulti()">Xác nhận</button>

    <div class="game-feedback hidden" id="game-feedback"></div>
  </div>

  <div id="game-result" class="game-screen hidden">
    <div class="game-result-card">
      <div class="game-result-emoji" id="game-result-emoji">🎉</div>
      <div class="game-result-score" id="game-result-score">0</div>
      <div class="game-result-sub">điểm</div>
      <div class="game-result-stats" id="game-result-stats"></div>
      <div class="game-result-verdict" id="game-result-verdict"></div>
      <div class="result-actions">
        <button class="btn btn-primary" onclick="gameBeginRound()">🔁 Chơi lại</button>
        <button class="btn btn-secondary" onclick="gameExit()">← Về danh sách đề</button>
      </div>
    </div>
  </div>
</div>

<!-- NAV SHEET -->"""
content = content.replace("<!-- NAV SHEET -->", html_code)

# 3. Add JS
js_code = """<script>
/* ============================================================
   CHẾ ĐỘ TRÒ CHƠI (kiểu Quizizz/Wayground) — module độc lập,
   chỉ đọc baseQuiz đã có sẵn, dùng lại saveBest()/loadBests()
   đã có, KHÔNG cần thêm gì ở QuizHubBridge.java / B2.
   Công thức điểm (đúng theo tài liệu chính thức của Wayground):
   600 điểm cơ bản cho câu đúng + tối đa 400 điểm theo tốc độ.
   Phần thưởng streak là tự thiết kế (Wayground không công bố
   công thức điểm-thưởng-theo-chuỗi, chỉ công bố đây là yếu tố
   tạo động lực/hiển thị).
   ============================================================ */
const GAME_BASE_POINTS = 600;
const GAME_MAX_SPEED_BONUS = 400;
const GAME_STREAK_BONUS_STEP = 30;
const GAME_STREAK_BONUS_CAP = 150;
const GAME_FIRE_AT = 3;

let gameQuestions = [], gameIdx = 0, gameScore = 0, gameStreak = 0, gameBestStreak = 0;
let gameCorrectCount = 0, gameTimeLimitMs = 20000, gameRafId = null, gameDeadline = 0;
let gameAnswered = false, gameSelected = new Set(), gameMuted = false, gameStartedAt = 0, gameAudioCtx = null;

/* ---------- Âm thanh tổng hợp bằng Web Audio API (không cần file mp3) ---------- */
function gameAudio(){
  if(!gameAudioCtx){ try{ gameAudioCtx = new (window.AudioContext||window.webkitAudioContext)(); }catch(e){ return null; } }
  if(gameAudioCtx.state==='suspended') gameAudioCtx.resume();
  return gameAudioCtx;
}
function gameTone(freq, dur, type='sine', startGain=.18, delay=0){
  if(gameMuted) return;
  const ctx=gameAudio(); if(!ctx) return;
  const t0=ctx.currentTime+delay;
  const osc=ctx.createOscillator(), gain=ctx.createGain();
  osc.type=type; osc.frequency.setValueAtTime(freq,t0);
  gain.gain.setValueAtTime(startGain,t0);
  gain.gain.exponentialRampToValueAtTime(.001, t0+dur);
  osc.connect(gain); gain.connect(ctx.destination);
  osc.start(t0); osc.stop(t0+dur);
}
const gameSnd = {
  go(){ gameTone(523,.12); gameTone(659,.12,'sine',.18,.13); gameTone(784,.16,'sine',.18,.26); },
  correct(){ gameTone(880,.1,'sine',.2); gameTone(1175,.16,'sine',.2,.09); },
  wrong(){ gameTone(180,.22,'sawtooth',.15); },
  tick(){ gameTone(1000,.05,'square',.06); },
  streak(){ gameTone(660,.08,'sine',.15); gameTone(990,.1,'sine',.15,.08); },
  finish(){ [523,659,784,1046].forEach((f,i)=>gameTone(f,.18,'sine',.18,i*.11)); }
};
function gameToggleMute(){
  gameMuted=!gameMuted;
  document.getElementById('game-mute-btn').textContent = gameMuted?'🔇':'🔊';
}

/* ---------- Vào chế độ chơi (gọi từ fetchAndStart sau khi sửa ở mục 4) ---------- */
function startGameMapped(){
  document.getElementById('menu').classList.add('hidden');
  document.getElementById('quiz').classList.add('hidden');
  document.getElementById('flash').classList.add('hidden');
  document.getElementById('game').classList.remove('hidden');
  document.getElementById('game-title').textContent = '🎮 ' + (TITLES[deckNum]||'Trò chơi');
  document.getElementById('game-intro-sub').textContent = baseQuiz.length + ' câu hỏi · điểm theo độ chính xác + tốc độ trả lời';
  gameShowScreen('game-intro');
  window.scrollTo(0,0);
}
function gameSetTime(s){
  gameTimeLimitMs = s*1000;
  document.querySelectorAll('#seg-game-time button').forEach(b=>b.classList.toggle('on', +b.dataset.v===s));
}
function gameShowScreen(id){
  ['game-intro','game-countdown','game-play','game-result'].forEach(s=>{
    document.getElementById(s).classList.toggle('hidden', s!==id);
  });
}

function gameBeginRound(){
  gameAudio();
  gameQuestions = shuffle(baseQuiz);
  gameIdx=0; gameScore=0; gameStreak=0; gameBestStreak=0; gameCorrectCount=0;
  gameStartedAt = Date.now();
  gameShowScreen('game-countdown');
  let n=3;
  const numEl=document.getElementById('game-countdown-num');
  numEl.textContent=n;
  const iv=setInterval(()=>{
    n--;
    if(n<=0){
      clearInterval(iv);
      gameSnd.go();
      gameShowScreen('game-play');
      gameShowQuestion();
    }else{
      numEl.textContent=n;
      numEl.style.animation='none'; numEl.offsetHeight; numEl.style.animation='gcPulse .45s ease-out';
    }
  },650);
}

function gameShowQuestion(){
  gameAnswered=false; gameSelected=new Set();
  const [q,options,correct] = gameQuestions[gameIdx];
  const multi = correct.length>1;

  document.getElementById('game-score').textContent=gameScore;
  document.getElementById('game-progress').textContent=`${gameIdx+1}/${gameQuestions.length}`;
  document.getElementById('game-q').innerHTML = esc(q);
  document.getElementById('game-multi-hint').classList.toggle('hidden', !multi);
  document.getElementById('game-confirm-btn').classList.add('hidden');
  document.getElementById('game-feedback').classList.add('hidden');

  const streakEl=document.getElementById('game-streak');
  if(gameStreak>=2){
    streakEl.classList.remove('hidden');
    streakEl.classList.toggle('fire', gameStreak>=GAME_FIRE_AT);
    document.getElementById('game-streak-num').textContent=gameStreak;
  } else streakEl.classList.add('hidden');

  const tiles=document.getElementById('game-tiles');
  tiles.innerHTML='';
  options.forEach((opt,oi)=>{
    const btn=document.createElement('button');
    btn.className=`game-tile gt-${oi%6}`;
    btn.innerHTML=`<span class="gt-letter">${LETTERS[oi]}</span><span>${esc(opt)}</span>`;
    btn.onclick=()=>gameTapOption(oi, multi);
    tiles.appendChild(btn);
  });

  gameDeadline = Date.now()+gameTimeLimitMs;
  cancelAnimationFrame(gameRafId);
  let lastTickSec=null;
  const ring=document.getElementById('game-ring-fg');
  const CIRC=276.46;
  function tick(){
    const left=gameDeadline-Date.now();
    const frac=Math.max(0,left/gameTimeLimitMs);
    ring.style.strokeDashoffset = CIRC*(1-frac);
    const secLeft=Math.ceil(left/1000);
    document.getElementById('game-timer-num').textContent=Math.max(0,secLeft);
    ring.style.stroke = frac>.5 ? 'var(--green)' : frac>.2 ? 'var(--orange)' : '#d9534f';
    if(secLeft<=3 && secLeft>=1 && secLeft!==lastTickSec && !gameAnswered){ gameSnd.tick(); lastTickSec=secLeft; }
    if(left<=0){ if(!gameAnswered) gameTimeUp(); return; }
    gameRafId=requestAnimationFrame(tick);
  }
  tick();
}

function gameTapOption(oi, multi){
  if(gameAnswered) return;
  const tiles=document.querySelectorAll('.game-tile');
  if(!multi){ gameSelected=new Set([oi]); gameGradeAnswer(); return; }
  if(gameSelected.has(oi)) gameSelected.delete(oi); else gameSelected.add(oi);
  tiles[oi].classList.toggle('gt-picked', gameSelected.has(oi));
  document.getElementById('game-confirm-btn').classList.toggle('hidden', gameSelected.size===0);
}
function gameConfirmMulti(){ if(!gameAnswered) gameGradeAnswer(); }

function gameTimeUp(){
  gameAnswered=true; gameStreak=0; gameSnd.wrong();
  gameRevealAndShowFeedback(false, 0, 0);
}

function gameGradeAnswer(){
  gameAnswered=true;
  cancelAnimationFrame(gameRafId);
  const [,,correct] = gameQuestions[gameIdx];
  const ok = setEq(gameSelected, correct);
  const timeLeftMs = Math.max(0, gameDeadline-Date.now());

  let pts=0, streakBonus=0;
  if(ok){
    gameCorrectCount++; gameStreak++;
    gameBestStreak=Math.max(gameBestStreak, gameStreak);
    pts = Math.round(GAME_BASE_POINTS + GAME_MAX_SPEED_BONUS*(timeLeftMs/gameTimeLimitMs));
    if(gameStreak>=GAME_FIRE_AT) streakBonus = Math.min((gameStreak-GAME_FIRE_AT+1)*GAME_STREAK_BONUS_STEP, GAME_STREAK_BONUS_CAP);
    gameScore += pts+streakBonus;
    gameSnd.correct();
    if(gameStreak>=GAME_FIRE_AT) gameSnd.streak();
  } else { gameStreak=0; gameSnd.wrong(); }
  gameRevealAndShowFeedback(ok, pts, streakBonus);
}

function gameRevealAndShowFeedback(ok, pts, streakBonus){
  const [,,correct] = gameQuestions[gameIdx];
  const right=new Set(correct);
  document.querySelectorAll('.game-tile').forEach((btn,oi)=>{
    btn.disabled=true;
    if(right.has(oi)) btn.classList.add('gt-correct');
    else btn.classList.add('gt-wrong');
  });
  document.getElementById('game-score').textContent=gameScore;

  const fb=document.getElementById('game-feedback');
  fb.classList.remove('hidden'); fb.classList.toggle('ok',ok); fb.classList.toggle('bad',!ok);
  fb.innerHTML = ok
    ? `✓ Chính xác! <span class="pts">+${pts}${streakBonus?` +${streakBonus} 🔥`:''} điểm</span>`
    : `✗ Chưa đúng <span class="pts">Đáp án: ${correct.map(i=>LETTERS[i]).join(', ')}</span>`;

  setTimeout(gameNext, ok?1200:1700);
}

function gameNext(){
  gameIdx++;
  if(gameIdx>=gameQuestions.length){ gameShowResults(); return; }
  gameShowQuestion();
}

function gameShowResults(){
  gameSnd.finish();
  const n=gameQuestions.length;
  const pct=gameCorrectCount/n;
  document.getElementById('game-result-score').textContent=gameScore;
  document.getElementById('game-result-emoji').textContent = pct>=.9?'🏆':pct>=.7?'🎉':pct>=.5?'👍':'💪';
  document.getElementById('game-result-stats').innerHTML = `
    <div><div class="v">${gameCorrectCount}/${n}</div><div class="l">Câu đúng</div></div>
    <div><div class="v">${gameBestStreak}🔥</div><div class="l">Chuỗi cao nhất</div></div>
    <div><div class="v">${Math.round((Date.now()-gameStartedAt)/1000)}s</div><div class="l">Thời gian</div></div>
  `;
  let verdict;
  if(pct>=0.9)verdict='Quá đỉnh! 🎯';
  else if(pct>=0.7)verdict='Ngon, qua môn thoải mái.';
  else if(pct>=0.5)verdict='Tạm ổn, ôn thêm chút.';
  else verdict='Cày lại đi bro.';
  document.getElementById('game-result-verdict').textContent=verdict;

  const score10=(gameCorrectCount*10/n).toFixed(2);
  saveBest(deckNum, gameCorrectCount, n, score10); // dùng chung "🏆 Cao nhất" với Ôn tập/Thi thử

  gameShowScreen('game-result');
  window.scrollTo(0,0);
}

function gameExit(){
  cancelAnimationFrame(gameRafId);
  document.getElementById('game').classList.add('hidden');
  document.getElementById('menu').classList.remove('hidden');
  loadBests();
  window.scrollTo(0,0);
}
</script>
</body>"""
content = content.replace("</body>", js_code)

# 4. Modify fetchAndStart
content = content.replace("function fetchAndStart(deckId, isFlashcard) {", "function fetchAndStart(deckId, mode) {")
content = content.replace("""                if (isFlashcard) {
                    startFlashcardsMapped();
                } else {
                    startQuizMapped();
                }""", """                if (mode === true || mode === 'flashcard') startFlashcardsMapped();
                else if (mode === 'game') startGameMapped();
                else startQuizMapped();""")

# 5. Modify loadMenuDecks card actions
target_action = """                  <div class="best" id="best-${deck.id}"></div>
                  <div class="go" style="background:${color}">Bắt đầu →</div>"""
replacement_action = """                  <div class="best" id="best-${deck.id}"></div>
                  <div class="card-actions">
                    <div class="go" style="background:${color}">Bắt đầu →</div>
                    <button class="go-game" onclick="event.stopPropagation();fetchAndStart('${deck.id}','game')">🎮 Chơi</button>
                  </div>"""
content = content.replace(target_action, replacement_action)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Done applying Claude's changes.")
