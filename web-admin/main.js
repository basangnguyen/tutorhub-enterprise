import './style.css'

const app = document.querySelector('#app')
app.innerHTML = `
  <div class="sidebar">
    <div class="brand">
      <span>TutorHub</span> Admin
    </div>
    <ul class="nav-menu">
      <li class="nav-item active" id="nav-exams">Quản lý Đề Thi</li>
      <li class="nav-item" id="nav-proctor">Giám sát Học Sinh</li>
    </ul>
  </div>
  <div class="main-content" id="main-content">
    <!-- Content will be injected here -->
  </div>
`;

let socket = null;
let currentUserId = -1; 

function initConnection() {
  socket = new WebSocket('ws://localhost:8888');

  socket.onopen = () => {
    console.log('Connected to Server');
    // Auto login using the user's requested credentials
    const loginPacket = {
      action: "LOGIN",
      payload: "basangnguyen12@gmail.com|Hocbatrolai293$" 
    };
    socket.send(JSON.stringify(loginPacket));
  };

  socket.onmessage = (event) => {
    try {
        const response = JSON.parse(event.data);
        handleServerMessage(response);
    } catch(e) {
        console.error("Failed to parse response", e);
    }
  };

  socket.onclose = () => {
    console.log('Disconnected');
  };
}

function handleServerMessage(packet) {
  console.log("Received from server:", packet);
  
  if (packet.success === true && packet.payload && packet.payload.startsWith("DASHBOARD_GO")) {
     const parts = packet.payload.split('|');
     currentUserId = parseInt(parts[1]);
     console.log("Logged in as User ID", currentUserId);
     loadExamsView();
  } else if (packet.action === "GET_EXAMS_RESPONSE" || packet.action === "EXAM_LIST") {
     renderExams(packet.data || []);
  } else if (packet.action === "EXAM_QUESTIONS_LIST") {
     renderQuestions(packet.data || []);
  } else if (packet.action === "RESPONSE" || packet.action === "CREATE_EXAM_SUCCESS" || packet.success) {
     if (packet.message) alert(packet.message);
     if (packet.message && packet.message.includes("thành công") && !packet.message.includes("câu hỏi")) {
         socket.send(JSON.stringify({action: "GET_EXAMS", payload: ""}));
     }
     if (packet.message && packet.message.includes("Thêm câu hỏi thành công")) {
         if (currentManagingExamId) {
             socket.send(JSON.stringify({action: "GET_EXAM_QUESTIONS", payload: currentManagingExamId.toString()}));
         }
     }
  } else if (packet.success === false && packet.message) {
     alert("Lỗi: " + packet.message);
  }
}

function loadExamsView() {
  document.getElementById('main-content').innerHTML = `
    <div class="header">
      <h1 class="header-title">Quản lý Đề Thi</h1>
      <button class="btn" id="btn-create-exam">+ Tạo Kỳ Thi Mới</button>
    </div>
    <div class="card">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên Kỳ Thi</th>
            <th>Thời gian</th>
            <th>Trạng thái</th>
            <th>Hành động</th>
          </tr>
        </thead>
        <tbody id="exam-table-body">
          <tr><td colspan="5">Đang tải dữ liệu...</td></tr>
        </tbody>
      </table>
    </div>
  `;
  
  document.getElementById('btn-create-exam').addEventListener('click', showCreateExamModal);
  
  socket.send(JSON.stringify({
      action: "GET_EXAMS",
      payload: ""
  }));
}

function renderExams(exams) {
  const tbody = document.getElementById('exam-table-body');
  if (!exams || exams.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5">Chưa có kỳ thi nào.</td></tr>';
      return;
  }
  
  tbody.innerHTML = '';
  exams.forEach(exam => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${exam.id}</td>
        <td>${exam.title}</td>
        <td>${exam.durationMins} phút</td>
        <td>${exam.status || 'ACTIVE'}</td>
        <td>
           <button class="btn btn-secondary btn-sm" onclick="manageQuestions(${exam.id}, '${exam.title}')">Câu hỏi</button>
           <button class="btn btn-secondary btn-sm" onclick="monitorExam(${exam.id})">Giám sát</button>
        </td>
      `;
      tbody.appendChild(tr);
  });
}

let currentManagingExamId = null;

window.manageQuestions = function(examId, examTitle) {
    currentManagingExamId = examId;
    document.getElementById('main-content').innerHTML = `
      <div class="header">
        <h1 class="header-title">Câu hỏi: ${examTitle}</h1>
        <div>
           <button class="btn btn-secondary" onclick="loadExamsView()">← Quay lại</button>
           <button class="btn" id="btn-add-question">+ Thêm câu hỏi</button>
        </div>
      </div>
      <div class="card">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Loại</th>
              <th>Nội dung</th>
              <th>Điểm</th>
            </tr>
          </thead>
          <tbody id="question-table-body">
            <tr><td colspan="4">Đang tải câu hỏi...</td></tr>
          </tbody>
        </table>
      </div>
    `;
    
    document.getElementById('btn-add-question').addEventListener('click', showAddQuestionModal);
    
    socket.send(JSON.stringify({
        action: "GET_EXAM_QUESTIONS",
        payload: examId.toString()
    }));
};

function renderQuestions(questions) {
  const tbody = document.getElementById('question-table-body');
  if (!tbody) return; // Might have navigated away
  
  if (!questions || questions.length === 0) {
      tbody.innerHTML = '<tr><td colspan="4">Chưa có câu hỏi nào.</td></tr>';
      return;
  }
  
  tbody.innerHTML = '';
  questions.forEach(q => {
      let contentSnippet = q.content;
      try {
          const parsed = JSON.parse(q.content);
          if (parsed.questionText) contentSnippet = parsed.questionText;
      } catch(e) {}
      
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${q.id}</td>
        <td>${q.questionType}</td>
        <td>${contentSnippet.substring(0, 50)}${contentSnippet.length > 50 ? '...' : ''}</td>
        <td>${q.points}</td>
      `;
      tbody.appendChild(tr);
  });
}

window.showAddQuestionModal = function() {
    if (!currentManagingExamId) return;
    
    const type = prompt("Nhập loại câu hỏi (MCQ, ESSAY, FILL_BLANK):", "MCQ");
    if (!type) return;
    
    const content = prompt("Nhập nội dung câu hỏi:");
    if (!content) return;
    
    const pointsStr = prompt("Nhập số điểm:", "10");
    const points = parseFloat(pointsStr) || 10;
    
    let options = [];
    let correctAnswers = [];
    if (type === "MCQ") {
        const ops = prompt("Nhập các đáp án (phân cách bởi dấu |):", "A|B|C|D");
        if (ops) options = ops.split("|");
        const ans = prompt("Nhập đáp án đúng (ví dụ: A hoặc 0 cho index 0):");
        if (ans) correctAnswers = [ans];
    }
    
    const contentObj = {
        questionText: content,
        options: options,
        correctAnswers: correctAnswers
    };
    
    const questionObj = {
        examId: currentManagingExamId,
        questionType: type,
        category: "GENERAL",
        difficulty: "MEDIUM",
        points: points,
        content: JSON.stringify(contentObj),
        explanation: "",
        sortOrder: 1
    };
    
    socket.send(JSON.stringify({
        action: "ADD_QUESTION",
        payload: JSON.stringify(questionObj)
    }));
};

window.monitorExam = function(examId) {
    alert("Chuyển sang chế độ giám sát cho kỳ thi " + examId);
    // TODO: Implement Proctor Dashboard
};

function showCreateExamModal() {
    const title = prompt("Nhập tên kỳ thi mới:");
    if (!title) return;
    const duration = prompt("Nhập thời gian (phút):", "60");
    if (!duration) return;
    
    // Payload: JSON string of Exam object
    const examObj = {
        title: title,
        durationMins: parseInt(duration),
        status: "ACTIVE"
    };
    socket.send(JSON.stringify({
        action: "CREATE_EXAM",
        payload: JSON.stringify(examObj)
    }));
}

document.getElementById('nav-exams').addEventListener('click', () => {
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    document.getElementById('nav-exams').classList.add('active');
    loadExamsView();
});

document.getElementById('nav-proctor').addEventListener('click', () => {
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    document.getElementById('nav-proctor').classList.add('active');
    document.getElementById('main-content').innerHTML = `
      <div class="header">
        <h1 class="header-title">Giám sát Học Sinh</h1>
      </div>
      <div class="card">
        <p>Tính năng giám sát realtime đang được phát triển...</p>
      </div>
    `;
});

// Start connection
initConnection();
