const fs = require('fs');
const file = 'd:/Ban_sao_du_an/src/main/resources/tse/quiz.html';
let content = fs.readFileSync(file, 'utf8');

// Replace the hardcoded cards div with a container
content = content.replace(/<div class="cards">[\s\S]*?<\/div>\s*<div class="modes">/, '<div class="cards" id="decks-container">Đang tải danh sách đề...</div><div class="modes">');

// Remove the hardcoded QUIZ_X arrays and DECKS/TITLES
content = content.replace(/const QUIZ_1 = \[[\s\S]*?const LETTERS = "ABCDEF";/, 'const LETTERS = "ABCDEF";\nlet TITLES = {};');

// Inject the new JS script at the end before </body>
const injection = 
<script>
  let decksMeta = {};
  
  window.onload = function() {
    loadMenuDecks();
  };
  
  function loadMenuDecks() {
    if(!window.cefQuery) {
        document.getElementById('decks-container').innerHTML = 'Đang chạy ngoài JCEF hoặc chưa load xong cefQuery.';
        return;
    }
    window.cefQuery({
      request: 'LIST_DECKS:',
      onSuccess: function(res) {
        try {
          const decks = JSON.parse(res);
          decksMeta = {};
          const container = document.getElementById('decks-container');
          container.innerHTML = '';
          if (decks.length === 0) {
            container.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: #666; padding: 20px;">Chưa có đề nào. Hãy click "Nhập đề từ Excel" ở thanh công cụ phía trên.</div>';
            return;
          }
          decks.forEach(deck => {
            decksMeta[deck.id] = deck;
            const color = deck.color || '#1a4f8b';
            const title = deck.title || 'Đề thi';
            const count = deck.questionCount || 0;
            const desc = deck.shortDescription || '';
            
            const cardHtml = \
              <div class="card" onclick="fetchAndStart('\', false)">
                <div class="accent" style="background:\"></div>
                <div class="body">
                  <div class="big" style="color:\">\</div>
                  <div class="mid">\ câu trắc nghiệm</div>
                  <div class="desc">\</div>
                  <div class="best" id="best-\"></div>
                  <div class="go" style="background:\">Bắt đầu →</div>
                </div>
              </div>
            \;
            container.innerHTML += cardHtml;
          });
          loadBests(); 
        } catch (e) {
          console.error("Lỗi parse list decks", e);
        }
      },
      onFailure: function(code, msg) {
        console.error("Lỗi load decks: ", msg);
        document.getElementById('decks-container').innerHTML = 'Lỗi tải danh sách đề: ' + msg;
      }
    });
  }

  function fetchAndStart(deckId, isFlashcard) {
    if(!window.cefQuery) return;
    window.cefQuery({
        request: 'GET_DECK:' + deckId,
        onSuccess: function(res) {
            try {
                const deckData = JSON.parse(res);
                deckNum = deckId;
                
                baseQuiz = deckData.questions.map(q => {
                    return [
                        q.questionText || '',
                        q.options || [],
                        q.correctOptionIndexes || [0],
                        q.explanation || '',
                        q.wrongExplanations || {}
                    ];
                });
                
                TITLES[deckId] = deckData.title;

                if (isFlashcard) {
                    startFlashcardsMapped();
                } else {
                    startQuizMapped();
                }
            } catch(e) {
                alert("Lỗi parse dữ liệu đề: " + e);
            }
        },
        onFailure: function(code, msg) {
            alert("Lỗi tải đề: " + msg);
        }
    });
  }
  
  function startQuizMapped() {
      opts.shuffleQ = document.getElementById('sw-shuffleQ').checked;
      opts.shuffleO = document.getElementById('sw-shuffleO').checked;
      opts.instant  = document.getElementById('sw-instant').checked;
      currentQuiz=buildRunnable();
      document.getElementById('quiz-title').textContent=TITLES[deckNum]+(opts.limit?\\\ · \\\ câu\\\:'');
      document.getElementById('menu').classList.add('hidden');
      document.getElementById('flash').classList.add('hidden');
      document.getElementById('quiz').classList.remove('hidden');
      resetQuiz();
  }
  
  function startFlashcardsMapped(){
      fcDeck=baseQuiz.slice();
      fcIdx=0; fcFlipped=false;
      document.getElementById('fc-title').textContent='Flashcard — '+TITLES[deckNum];
      document.getElementById('menu').classList.add('hidden');
      document.getElementById('quiz').classList.add('hidden');
      document.getElementById('flash').classList.remove('hidden');
      renderCard();
      window.scrollTo(0,0);
  }
  
  startFlashcards = function() {
      const deckIds = Object.keys(decksMeta);
      if (deckIds.length === 0) return alert("Chưa có đề nào!");
      let target = decksMeta[deckNum] ? deckNum : deckIds[0];
      fetchAndStart(target, true);
  };
  
  loadBests = function() {
      const deckIds = Object.keys(decksMeta);
      deckIds.forEach(id => {
          window.cefQuery({
              request: 'GET_BEST_SCORE:' + id,
              onSuccess: function(res) {
                  if (res) {
                      try {
                          const d = JSON.parse(res);
                          const el = document.getElementById('best-'+id);
                          if(el) el.textContent = \🏆 Cao nhất: \/\ (\/10)\;
                      } catch(e) {}
                  }
              }
          });
      });
  };
  
  saveBest = function(n, correct, total, score) {
      window.cefQuery({
          request: 'SAVE_BEST_SCORE:' + n + '|' + JSON.stringify({correct,total,score}),
          onSuccess: function(res) {
              const el = document.getElementById('best-'+n);
              if(el) el.textContent = \🏆 Cao nhất: \/\ (\/10)\;
          }
      });
  };
</script>
</body>
;

content = content.replace(/<\/body>/, injection);

fs.writeFileSync(file, content, 'utf8');
