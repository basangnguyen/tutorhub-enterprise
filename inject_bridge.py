import re

html_path = 'd:/Ban_sao_du_an/src/main/resources/tse/quiz-practice-template.html'

with open(html_path, 'r', encoding='utf-8') as f:
    content = f.read()

bridge_script = """
<script>
window.TutorHubPractice = {
    loadMenu: function(payloadJson, type) {
        try {
            const data = JSON.parse(payloadJson);
            const grid = document.querySelector('.card-grid');
            if(grid) {
                grid.innerHTML = '';
                if(data && data.length > 0) {
                    data.forEach(item => {
                        const card = document.createElement('div');
                        card.className = 'card';
                        
                        // assignments has assignmentId, paper. papers just have paperId
                        const id = type === 'ASSIGNMENT' ? item.assignmentId : item.paperId;
                        const title = type === 'ASSIGNMENT' ? item.paperName : item.title;
                        const totalQuestions = type === 'ASSIGNMENT' ? item.totalQuestions : item.questionCount;
                        const actionName = type === 'ASSIGNMENT' ? 'Làm bài được giao' : 'Luyện đề chung';
                        
                        let assignedInfo = '';
                        if (type === 'ASSIGNMENT') {
                             assignedInfo = `<p style="font-size: 13px; color: #666;">Chính sách đáp án: <b>${item.showAnswersPolicy}</b></p>`;
                        }
                        
                        card.innerHTML = `
                            <h3>${title}</h3>
                            <p>${totalQuestions} câu</p>
                            ${assignedInfo}
                            <div class="card-actions" style="margin-top: 15px; display: flex; gap: 10px;">
                                <button class="btn btn-primary" onclick="window.TutorHubPractice.triggerStart(${id}, '${type}')">${actionName}</button>
                            </div>
                        `;
                        grid.appendChild(card);
                    });
                } else {
                    grid.innerHTML = '<p>Không có dữ liệu.</p>';
                }
            }
            
            document.getElementById('menu').classList.remove('hidden');
            document.getElementById('quiz').classList.add('hidden');
            document.getElementById('flash').classList.add('hidden');
        } catch(e) {
            console.error("Parse JSON error in loadMenu", e);
        }
    },
    
    triggerStart: function(id, type) {
        if(window.JavaBridge) {
            if (type === 'ASSIGNMENT') {
                window.JavaBridge.startAssignment(id);
            } else {
                window.JavaBridge.startPractice(id);
            }
        } else {
            alert("Môi trường ngoài app: Giao tiếp Bridge không khả dụng.");
        }
    },
    
    loadQuiz: function(payloadJson) {
        try {
            const payload = JSON.parse(payloadJson);
            // payload in PRACTICE_START_SUCCESS is {attemptId, title, quizData, ...}
            window.attemptId = payload.attemptId;
            const quizData = payload.quizData; // DTO array
            
            // Map the new quizData format back to the expected array format for baseQuiz
            // [question, optionsArray, correctIndicesArray, explanation, wrongExplanationMap, qId, optionsIdMap]
            baseQuiz = quizData.map(q => {
                const options = q.options.map(o => o.text);
                const optIds = q.options.map(o => o.id);
                // Correct indices handles multiple answers
                const correctIndices = [];
                q.options.forEach((o, i) => {
                    if (o.isCorrect) correctIndices.push(i);
                });
                
                return [
                    q.questionText || '',
                    options,
                    correctIndices,
                    q.explanation || '',
                    {}, // wrongExpl
                    q.questionId,
                    optIds
                ];
            });
            
            opts.shuffleQ = document.getElementById('sw-shuffleQ').checked;
            opts.shuffleO = document.getElementById('sw-shuffleO').checked;
            opts.instant  = document.getElementById('sw-instant').checked;
            
            currentQuiz = buildRunnable();
            document.getElementById('quiz-title').textContent = payload.title + (opts.limit ? ` · ${opts.limit} câu` : '');
            document.getElementById('menu').classList.add('hidden');
            document.getElementById('flash').classList.add('hidden');
            document.getElementById('quiz').classList.remove('hidden');
            
            startTs = Date.now();
            resetQuiz();
        } catch(e) {
            console.error("Error in loadQuiz", e);
        }
    },
    
    onSaveAck: function() {
        console.log("Save answer acknowledged by Java");
    },
    
    onSubmitAck: function(payloadJson) {
        try {
            const res = JSON.parse(payloadJson);
            // Display results
            if (window.showResult) {
                 window.showResult(res.correctCount, res.totalQuestions, res.score10);
            }
        } catch(e) {
             console.error("Error in onSubmitAck", e);
        }
    },
    
    showError: function(msg) {
        alert("Lỗi: " + msg);
        document.getElementById('grade-btn').textContent = 'Nộp bài';
        document.getElementById('grade-btn').disabled = false;
    }
};

window.saveAnswer = function(questionId, selectedOptionId, isSkipped, isMarked, timeSpentSeconds) {
    if (window.JavaBridge && window.attemptId) {
        window.JavaBridge.saveAnswer(window.attemptId, questionId, selectedOptionId ? selectedOptionId.toString() : "", isSkipped, isMarked, timeSpentSeconds);
    }
};

window.submitQuiz = function(durationSeconds) {
    if (window.JavaBridge && window.attemptId) {
        window.JavaBridge.submitQuiz(window.attemptId, durationSeconds);
    }
};
</script>
</body>
"""

content = content.replace("</body>", bridge_script)

with open(html_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Injected Bridge into HTML")
