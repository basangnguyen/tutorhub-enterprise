require('dotenv').config();
const express = require('express');
const cors = require('cors');
const http = require('http');
const WebSocket = require('ws');
const { AccessToken } = require('livekit-server-sdk');

const port = process.env.PORT || 1234;
const app = express();

app.use(cors());
app.use(express.json());

const LIVEKIT_API_KEY = process.env.LIVEKIT_API_KEY || '';
const LIVEKIT_API_SECRET = process.env.LIVEKIT_API_SECRET || '';

app.get('/', (req, res) => {
  res.send('TutorHub Sync Server + LiveKit API is running');
});

// API tạo Token cho LiveKit
app.get('/version.json', (req, res) => {
  const versionPath = path.join(__dirname, 'version.json');
  res.setHeader('Cache-Control', 'no-store');
  res.sendFile(versionPath);
});

app.get('/update.jar', (req, res) => {
  const jarPath = path.join(__dirname, 'update.jar');
  if (!fs.existsSync(jarPath)) {
    return res.status(404).send('update.jar not found');
  }

  res.setHeader('Cache-Control', 'no-store');
  res.download(jarPath, 'update.jar');
});

app.get('/livekit/token', async (req, res) => {
  try {
    if (!LIVEKIT_API_KEY || !LIVEKIT_API_SECRET) {
      return res.status(503).json({ error: 'LiveKit credentials are not configured.' });
    }

    const roomName = req.query.room || 'default-room';
    const participantName = req.query.username || 'User_' + Math.floor(Math.random() * 10000);
    
    // UUID random cho identity
    const participantIdentity = 'id_' + Math.floor(Math.random() * 1000000);

    const at = new AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET, {
      identity: participantIdentity,
      name: participantName,
    });
    
    at.addGrant({ roomJoin: true, room: roomName });
    
    const token = await at.toJwt();
    res.json({ token });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

const multer = require('multer');
const { S3Client, PutObjectCommand, GetObjectCommand } = require('@aws-sdk/client-s3');
const { getSignedUrl } = require('@aws-sdk/s3-request-presigner');
const fs = require('fs');
const path = require('path');

const upload = multer({ dest: 'temp_uploads/' });

// Cấu hình Backblaze B2 mặc định (Lấy từ B2Helper.java)
const B2_ENDPOINT = process.env.B2_ENDPOINT || 'https://s3.us-west-004.backblazeb2.com';
const B2_KEY_ID = process.env.B2_KEY_ID || '';
const B2_APPLICATION_KEY = process.env.B2_APPLICATION_KEY || '';
const B2_BUCKET = process.env.B2_BUCKET || '';

const s3 = new S3Client({
  endpoint: B2_ENDPOINT,
  region: 'us-east-1', // B2 không quan tâm region, nhưng sdk cần có
  credentials: {
    accessKeyId: B2_KEY_ID,
    secretAccessKey: B2_APPLICATION_KEY
  }
});

// API Upload Video Ghi hình
app.post('/upload-record', upload.single('video'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ error: 'Không tìm thấy file video' });
    
    const fileStream = fs.createReadStream(req.file.path);
    const fileName = `tutorhub-record-${Date.now()}.webm`;
    
    if (B2_ENDPOINT && B2_KEY_ID && B2_APPLICATION_KEY && B2_BUCKET) {
      // Upload thẳng lên Backblaze B2
      const uploadParams = {
        Bucket: B2_BUCKET,
        Key: fileName,
        Body: fileStream,
        ContentType: 'video/webm'
      };
      await s3.send(new PutObjectCommand(uploadParams));
      fs.unlinkSync(req.file.path); // Xóa file tạm
      
      // Bucket là Private, nên phải tạo Presigned URL để xem (có hạn 7 ngày)
      const getCommand = new GetObjectCommand({
        Bucket: B2_BUCKET,
        Key: fileName,
      });
      const signedUrl = await getSignedUrl(s3, getCommand, { expiresIn: 7 * 24 * 3600 });
      
      res.json({ success: true, url: signedUrl, message: 'Đã lưu lên Backblaze B2' });
    } else {
      // Lưu ở local nếu Sếp chưa điền Key
      res.json({ success: true, localPath: req.file.path, message: 'Lưu thành công ở máy tính (Chưa cấu hình B2)' });
    }
  } catch (error) {
    console.error('Lỗi Upload Video:', error);
    res.status(500).json({ error: error.message });
  }
});

// API Upload Tài liệu (PDF/Image)
app.post('/upload-document', upload.single('file'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ error: 'Không tìm thấy file' });
    
    const fileStream = fs.createReadStream(req.file.path);
    const mimeType = req.body.mimeType || req.file.mimetype || 'image/png';
    const extension = mimeType === 'image/jpeg' ? 'jpg' : 'png';
    const fileName = `tutorhub-doc-${Date.now()}.${extension}`;
    
    if (B2_ENDPOINT && B2_KEY_ID && B2_APPLICATION_KEY && B2_BUCKET) {
      const uploadParams = {
        Bucket: B2_BUCKET,
        Key: fileName,
        Body: fileStream,
        ContentType: mimeType
      };
      await s3.send(new PutObjectCommand(uploadParams));
      fs.unlinkSync(req.file.path);
      
      const getCommand = new GetObjectCommand({
        Bucket: B2_BUCKET,
        Key: fileName,
      });
      const signedUrl = await getSignedUrl(s3, getCommand, { expiresIn: 7 * 24 * 3600 });
      
      res.json({ success: true, url: signedUrl });
    } else {
      res.json({ success: true, url: req.file.path });
    }
  } catch (error) {
    console.error('Lỗi Upload Document:', error);
    res.status(500).json({ error: error.message });
  }
});

// API Proxy Hình ảnh (Vượt qua giới hạn CORS của Backblaze B2)
app.get('/proxy-image', async (req, res) => {
  try {
    const imageUrl = req.query.url;
    if (!imageUrl) return res.status(400).send("Thiếu URL");

    const response = await fetch(imageUrl);
    if (!response.ok) throw new Error("Không thể tải ảnh từ Backblaze");

    const arrayBuffer = await response.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);

    res.setHeader('Content-Type', response.headers.get('content-type') || 'image/png');
    res.setHeader('Access-Control-Allow-Origin', '*'); // Cho phép Tldraw Board truy cập
    res.send(buffer);
  } catch (error) {
    console.error('Lỗi Proxy:', error);
    res.status(500).send(error.message);
  }
});

const server = http.createServer(app);

// Giữ lại WebSocket Broadcaster cũ cho chức năng đồng bộ Nét vẽ
const wss = new WebSocket.Server({ server });
const rooms = new Map();
const QUIZHUB_PROTOCOL = 'quizhub.live.v1';
const quizHubRooms = new Map();
const QUIZHUB_ROOM_TTL_MS = 30 * 60 * 1000;
const QUIZHUB_PARTICIPANT_TTL_MS = 30 * 1000;

function isQuizHubMessage(raw) {
  try {
    const msg = JSON.parse(raw);
    return msg && msg.protocol === QUIZHUB_PROTOCOL ? msg : null;
  } catch (error) {
    return null;
  }
}

function cleanText(value, fallback, maxLength) {
  const text = String(value || fallback || '').trim();
  return text.length > maxLength ? text.slice(0, maxLength) : text;
}

function getQuizHubRoom(roomId, msg) {
  if (!quizHubRooms.has(roomId)) {
    quizHubRooms.set(roomId, {
      roomId,
      joinCode: cleanText(msg && msg.joinCode, roomId.replace(/^quizhub-/, ''), 12),
      deckId: cleanText(msg && msg.deckId, '', 80),
      deckTitle: cleanText(msg && msg.deckTitle, 'QuizHub', 140),
      teacherId: null,
      active: false,
      closed: false,
      roster: new Map(),
      leaderboard: new Map(),
      createdAt: Date.now(),
      updatedAt: Date.now()
    });
  }
  return quizHubRooms.get(roomId);
}

function sendJson(ws, payload) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(payload));
  }
}

function broadcastJson(roomId, payload, exceptWs) {
  const roomClients = rooms.get(roomId);
  if (!roomClients) return;
  roomClients.forEach(client => {
    if (client !== exceptWs && client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify(payload));
    }
  });
}

function roomSnapshot(state) {
  trimQuizHubRoster(state);
  return {
    protocol: QUIZHUB_PROTOCOL,
    type: 'session.snapshot',
    joinCode: state.joinCode,
    deckId: state.deckId,
    deckTitle: state.deckTitle,
    teacherId: state.teacherId,
    active: state.active,
    closed: state.closed,
    roster: Array.from(state.roster.values()),
    leaderboard: Array.from(state.leaderboard.values()),
    serverTime: Date.now()
  };
}

function trimQuizHubRoster(state) {
  const now = Date.now();
  state.roster.forEach((participant, playerId) => {
    if (now - (participant.lastSeen || 0) > QUIZHUB_PARTICIPANT_TTL_MS) {
      state.roster.delete(playerId);
      if (state.teacherId === playerId) {
        state.teacherId = null;
        state.active = false;
      }
    }
  });
}

function upsertQuizHubPresence(state, msg, ws) {
  const playerId = cleanText(msg.playerId, '', 80);
  if (!playerId) return null;
  const role = msg.role === 'teacher' ? 'teacher' : 'student';
  const participant = {
    playerId,
    name: cleanText(msg.name, role === 'teacher' ? 'Giáo viên' : 'Học viên', 80),
    role,
    lastSeen: Date.now()
  };
  state.roster.set(playerId, participant);
  state.updatedAt = Date.now();
  ws.quizHubPlayerId = playerId;
  return participant;
}

function isTeacherOwner(state, msg) {
  return msg.role === 'teacher' && msg.playerId && state.teacherId === msg.playerId;
}

function rejectQuizHub(ws, code, message) {
  sendJson(ws, {
    protocol: QUIZHUB_PROTOCOL,
    type: 'server.error',
    code,
    message
  });
}

function handleQuizHubMessage(ws, roomId, msg) {
  const state = getQuizHubRoom(roomId, msg);
  state.joinCode = cleanText(msg.joinCode, state.joinCode, 12);
  if (!state.deckId || !state.teacherId || state.teacherId === msg.playerId) {
    state.deckId = cleanText(msg.deckId, state.deckId, 80);
    state.deckTitle = cleanText(msg.deckTitle, state.deckTitle, 140);
  }
  state.updatedAt = Date.now();

  if (msg.type === 'session.open' || msg.type === 'presence') {
    const participant = upsertQuizHubPresence(state, msg, ws);
    if (!participant) {
      rejectQuizHub(ws, 'missing_player', 'Thiếu playerId cho phiên QuizHub live.');
      return;
    }
    if (participant.role === 'teacher') {
      if (!state.teacherId || state.teacherId === participant.playerId) {
        state.teacherId = participant.playerId;
        state.closed = false;
        if (msg.active === true) state.active = true;
      } else {
        participant.role = 'student';
        state.roster.set(participant.playerId, participant);
        rejectQuizHub(ws, 'teacher_taken', 'Phiên này đã có giáo viên host. Bạn được đưa vào vai trò học viên.');
      }
    }
    sendJson(ws, roomSnapshot(state));
    broadcastJson(roomId, Object.assign({}, msg, participant, {
      protocol: QUIZHUB_PROTOCOL,
      type: 'presence',
      joinCode: state.joinCode,
      deckId: state.deckId,
      deckTitle: state.deckTitle
    }), ws);
    return;
  }

  if (msg.type === 'presence.leave') {
    const playerId = cleanText(msg.playerId, '', 80);
    if (playerId) {
      state.roster.delete(playerId);
      state.leaderboard.delete(playerId);
      if (state.teacherId === playerId) {
        state.teacherId = null;
        state.active = false;
      }
      state.updatedAt = Date.now();
      broadcastJson(roomId, Object.assign({}, msg, { protocol: QUIZHUB_PROTOCOL, type: 'presence.leave' }), ws);
    }
    return;
  }

  if (msg.type === 'session.start') {
    if (!isTeacherOwner(state, msg)) {
      rejectQuizHub(ws, 'teacher_required', 'Chỉ giáo viên đang host mới được bắt đầu phiên live.');
      return;
    }
    state.active = true;
    state.closed = false;
    state.deckId = cleanText(msg.deckId, state.deckId, 80);
    state.deckTitle = cleanText(msg.deckTitle, state.deckTitle, 140);
    state.updatedAt = Date.now();
    broadcastJson(roomId, Object.assign({}, msg, {
      protocol: QUIZHUB_PROTOCOL,
      type: 'session.start',
      active: true,
      joinCode: state.joinCode,
      deckId: state.deckId,
      deckTitle: state.deckTitle
    }), null);
    return;
  }

  if (msg.type === 'session.end') {
    if (!isTeacherOwner(state, msg)) {
      rejectQuizHub(ws, 'teacher_required', 'Chỉ giáo viên đang host mới được kết thúc phiên live.');
      return;
    }
    state.active = false;
    state.closed = true;
    state.updatedAt = Date.now();
    broadcastJson(roomId, Object.assign({}, msg, {
      protocol: QUIZHUB_PROTOCOL,
      type: 'session.end',
      active: false,
      closed: true,
      joinCode: state.joinCode
    }), null);
    return;
  }

  if (msg.type === 'teacher.powerup') {
    if (!isTeacherOwner(state, msg)) {
      rejectQuizHub(ws, 'teacher_required', 'Chỉ giáo viên đang host mới được gửi power-up.');
      return;
    }
    if (!state.active) {
      rejectQuizHub(ws, 'session_inactive', 'Phiên live chưa bắt đầu.');
      return;
    }
    broadcastJson(roomId, Object.assign({}, msg, {
      protocol: QUIZHUB_PROTOCOL,
      type: 'teacher.powerup',
      joinCode: state.joinCode
    }), ws);
    return;
  }

  if (msg.type === 'leaderboard.update') {
    const playerId = cleanText(msg.playerId, '', 80);
    if (!playerId) return;
    const row = {
      playerId,
      name: cleanText(msg.name, 'Player', 80),
      role: msg.role === 'teacher' ? 'teacher' : 'student',
      status: cleanText(msg.status, 'playing', 24),
      score: Number(msg.score || 0),
      correct: Number(msg.correct || 0),
      answered: Number(msg.answered || 0),
      total: Number(msg.total || 0),
      accuracy: Number(msg.accuracy || 0),
      bestStreak: Number(msg.bestStreak || 0),
      updatedAt: Date.now()
    };
    state.leaderboard.set(playerId, row);
    state.updatedAt = Date.now();
    broadcastJson(roomId, Object.assign({}, row, {
      protocol: QUIZHUB_PROTOCOL,
      type: 'leaderboard.update',
      joinCode: state.joinCode,
      deckId: state.deckId,
      deckTitle: state.deckTitle
    }), null);
  }
}

setInterval(() => {
  const now = Date.now();
  quizHubRooms.forEach((state, roomId) => {
    trimQuizHubRoster(state);
    if (now - state.updatedAt > QUIZHUB_ROOM_TTL_MS) {
      quizHubRooms.delete(roomId);
    }
  });
}, 60 * 1000);

wss.on('connection', (ws, req) => {
  const urlParams = new URLSearchParams(req.url.split('?')[1]);
  const roomId = urlParams.get('roomId') || 'default';
  
  if (!rooms.has(roomId)) rooms.set(roomId, new Set());
  rooms.get(roomId).add(ws);
  
  ws.on('message', (message) => {
    const text = message.toString();
    const quizHubMsg = isQuizHubMessage(text);
    if (quizHubMsg) {
      handleQuizHubMessage(ws, roomId, quizHubMsg);
      return;
    }
    const roomClients = rooms.get(roomId);
    if (roomClients) {
      roomClients.forEach(client => {
        if (client !== ws && client.readyState === WebSocket.OPEN) {
          client.send(text);
        }
      });
    }
  });

  ws.on('close', () => {
    const roomClients = rooms.get(roomId);
    if (roomClients) {
      roomClients.delete(ws);
      if (roomClients.size === 0) rooms.delete(roomId);
    }
  });
});

server.listen(port, () => {
  console.log(`[TutorHub Sync] Node.js Server đang chạy tại cổng ${port}`);
  console.log(`- WebSocket Broadcast: ws://localhost:${port}`);
  console.log(`- LiveKit Token API: http://localhost:${port}/livekit/token`);
});
