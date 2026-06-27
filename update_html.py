import re

with open('d:/Ban_sao_du_an/src/main/resources/tse/quiz-practice-template.html', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Replace buildRunnable
build_runnable_old = """function buildRunnable(){
  // áp dụng trộn câu + cắt giới hạn + trộn đáp án
  let items = baseQuiz.map((it,origIdx)=>({it, origIdx}));
  if(opts.shuffleQ) items = shuffle(items);
  if(opts.limit>0) items = items.slice(0, opts.limit);
  return items.map(({it})=>{
    let [q,options,correct,expl,wrongExpl]=it;
    wrongExpl = wrongExpl || {};
    if(opts.shuffleO){
      const idx = options.map((_,i)=>i);
      const order = shuffle(idx);
      const newOpts = order.map(i=>options[i]);
      const newCorrect = correct.map(c=>order.indexOf(c));
      // remap giải thích sai theo vị trí mới
      const newWrong = {};
      Object.keys(wrongExpl).forEach(oldIdx=>{
        const newIdx = order.indexOf(+oldIdx);
        newWrong[newIdx] = wrongExpl[oldIdx];
      });
      return [q,newOpts,newCorrect.sort((a,b)=>a-b),expl,newWrong];
    }
    return [q,options.slice(),correct.slice(),expl,Object.assign({},wrongExpl)];
  });
}"""

build_runnable_new = """function buildRunnable(){
  let items = baseQuiz.map((it,origIdx)=>({it, origIdx}));
  if(opts.shuffleQ) items = shuffle(items);
  if(opts.limit>0) items = items.slice(0, opts.limit);
  return items.map(({it})=>{
    let [q,options,correct,expl,wrongExpl,qId,optIds]=it;
    wrongExpl = wrongExpl || {};
    if(opts.shuffleO){
      const idx = options.map((_,i)=>i);
      const order = shuffle(idx);
      const newOpts = order.map(i=>options[i]);
      const newCorrect = correct.map(c=>order.indexOf(c));
      const newOptIds = order.map(i=>optIds[i]);
      const newWrong = {};
      Object.keys(wrongExpl).forEach(oldIdx=>{
        const newIdx = order.indexOf(+oldIdx);
        newWrong[newIdx] = wrongExpl[oldIdx];
      });
      return [q,newOpts,newCorrect.sort((a,b)=>a-b),expl,newWrong,qId,newOptIds];
    }
    return [q,options.slice(),correct.slice(),expl,Object.assign({},wrongExpl),qId,optIds.slice()];
  });
}"""

content = content.replace(build_runnable_old, build_runnable_new)

# 2. Inject saveAnswer into renderQuestions
render_q_target = """selections[qi].delete(oi); lbl.classList.remove('selected');
        }"""
render_q_new = """selections[qi].delete(oi); lbl.classList.remove('selected');
        }
        const qId = item[5];
        const optIds = item[6];
        const optId = optIds[oi];
        const timeSpent = typeof startTs !== 'undefined' ? Math.floor((Date.now()-startTs)/1000) : 0;
        if(window.saveAnswer && !multi && inp.checked) {
             window.saveAnswer(qId, optId, false, flagged[qi], timeSpent);
        }"""
content = content.replace(render_q_target, render_q_new)

# 3. Inject toggleFlag saveAnswer
flag_target = "document.querySelector(`#nav-sheet .nav-grid .cell:nth-child(${qi+1})`).classList.toggle('flagged');"
flag_new = """document.querySelector(`#nav-sheet .nav-grid .cell:nth-child(${qi+1})`).classList.toggle('flagged');
  const qId = currentQuiz[qi][5];
  let selOptId = null;
  if(selections[qi].size > 0) {
      const oi = Array.from(selections[qi])[0];
      selOptId = currentQuiz[qi][6][oi];
  }
  const timeSpent = typeof startTs !== 'undefined' ? Math.floor((Date.now()-startTs)/1000) : 0;
  if(window.saveAnswer) {
      window.saveAnswer(qId, selOptId, false, flagged[qi], timeSpent);
  }"""
content = content.replace(flag_target, flag_new)

# 4. Replace grade function
grade_old = """/* ====== GRADE ====== */
function grade(){
  submitted=true;
  stopTimer();
  let correctCount=0;
  currentQuiz.forEach((item,qi)=>{
    const [,,correct,expl]=item;
    const right=new Set(correct);
    const chosen=selections[qi];
    const ok=setEq(chosen,correct);
    if(ok)correctCount++;
    document.querySelectorAll(`.opt[data-qi="${qi}"]`).forEach(lbl=>{
      const oi=+lbl.dataset.oi;
      lbl.classList.remove('correct','wrong');
      if(right.has(oi))lbl.classList.add('correct');
      else if(chosen.has(oi))lbl.classList.add('wrong');
      lbl.querySelector('input').disabled=true;
    });
    const exp=document.getElementById('expl-'+qi);
    let prefix='',cls='ok';
    if(ok)prefix='✓ ';
    else if(chosen.size===0){prefix='• Chưa chọn. ';cls='miss';}
    else {prefix='✗ Sai. ';cls='err';}
    exp.className='expl show '+cls;
    exp.innerHTML=buildExplHtml(qi, prefix, correct, expl, chosen);
  });

  const n=currentQuiz.length;
  const score10=(correctCount*10/n).toFixed(2);
  document.getElementById('score').textContent=`${correctCount}/${n} · ${score10}/10`;
  document.getElementById('score').style.color='#1a1a1a';

  // chỉ lưu best khi làm full đề (không trộn cắt) để công bằng
  if(opts.limit===0) saveBest(deckNum, correctCount, n, score10);

  const result=document.getElementById('result');
  const pct=correctCount/n;
  let verdict;
  if(pct>=0.9)verdict='Quá đỉnh! 🎯';
  else if(pct>=0.7)verdict='Ngon, qua môn thoải mái.';
  else if(pct>=0.5)verdict='Tạm ổn, ôn thêm chút.';
  else verdict='Cày lại đi bro.';
  const wrongCount=n-correctCount;
  let timeNote='';
  if(opts.mode==='exam'){
    const s=Math.floor((Date.now()-startTs)/1000);
    timeNote=` · ⏱ ${Math.floor(s/60)}p${String(s%60).padStart(2,'0')}s`;
  }
  result.innerHTML=`<div class="num">${correctCount}/${n} câu đúng — ${score10}/10 điểm${timeNote}</div>
    <div class="msg">${verdict} Kéo xuống xem câu xanh / đỏ kèm giải thích.</div>
    <div class="result-actions">
      ${wrongCount>0?`<button class="btn btn-primary" onclick="retryWrong()">🔁 Làm lại ${wrongCount} câu sai</button>`:''}
      <button class="btn btn-ghost" onclick="openNav()">⊞ Bản đồ kết quả</button>
      <button class="btn btn-secondary" onclick="resetQuiz()">Làm lại từ đầu</button>
    </div>`;
  result.classList.remove('hidden');
  document.getElementById('grade-btn').textContent='Đã chấm ✓';
  window.scrollTo({top:0,behavior:'smooth'});
}"""

grade_new = """/* ====== GRADE ====== */
function grade(){
  if (opts.mode === 'exam' && !confirm('Bạn có chắc chắn muốn nộp bài?')) return;
  const durationSeconds = typeof startTs !== 'undefined' ? Math.floor((Date.now()-startTs)/1000) : 0;
  document.getElementById('grade-btn').textContent = 'Đang nộp...';
  document.getElementById('grade-btn').disabled = true;
  if(window.submitQuiz) window.submitQuiz(durationSeconds);
}

window.showResult = function(correctCount, total, score10) {
  submitted=true;
  stopTimer();
  currentQuiz.forEach((item,qi)=>{
    const [,,correct,expl]=item;
    const right=new Set(correct);
    const chosen=selections[qi];
    const ok=setEq(chosen,correct);
    
    if (correct && correct.length > 0) {
      document.querySelectorAll(`.opt[data-qi="${qi}"]`).forEach(lbl=>{
        const oi=+lbl.dataset.oi;
        lbl.classList.remove('correct','wrong');
        if(right.has(oi))lbl.classList.add('correct');
        else if(chosen.has(oi))lbl.classList.add('wrong');
        lbl.querySelector('input').disabled=true;
      });
      const exp=document.getElementById('expl-'+qi);
      let prefix='',cls='ok';
      if(ok)prefix='✓ ';
      else if(chosen.size===0){prefix='• Chưa chọn. ';cls='miss';}
      else {prefix='✗ Sai. ';cls='err';}
      exp.className='expl show '+cls;
      exp.innerHTML=buildExplHtml(qi, prefix, correct, expl, chosen);
    } else {
       document.querySelectorAll(`.opt[data-qi="${qi}"]`).forEach(lbl=>{
         lbl.querySelector('input').disabled=true;
       });
    }
  });

  const n=currentQuiz.length;
  document.getElementById('score').textContent=`${correctCount}/${total} · ${score10}/10`;
  document.getElementById('score').style.color='#1a1a1a';

  const result=document.getElementById('result');
  const pct=correctCount/total;
  let verdict;
  if(pct>=0.9)verdict='Quá đỉnh! 🎯';
  else if(pct>=0.7)verdict='Ngon, qua môn thoải mái.';
  else if(pct>=0.5)verdict='Tạm ổn, ôn thêm chút.';
  else verdict='Cày lại đi bro.';
  let timeNote='';
  if(opts.mode==='exam'){
    const s=Math.floor((Date.now()-startTs)/1000);
    timeNote=` · ⏱ ${Math.floor(s/60)}p${String(s%60).padStart(2,'0')}s`;
  }
  result.innerHTML=`<div class="num">${correctCount}/${total} câu đúng — ${score10}/10 điểm${timeNote}</div>
    <div class="msg">${verdict}</div>
    <div class="result-actions">
      <button class="btn btn-ghost" onclick="openNav()">⊞ Bản đồ kết quả</button>
      <button class="btn btn-secondary" onclick="backToMenu()">Quay lại danh sách</button>
    </div>`;
  result.classList.remove('hidden');
  window.scrollTo({top:0,behavior:'smooth'});
}"""

content = content.replace(grade_old, grade_new)

with open('d:/Ban_sao_du_an/src/main/resources/tse/quiz-practice-template.html', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done updating HTML!")
