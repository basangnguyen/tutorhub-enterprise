require('dotenv').config();
const express = require('express');
const cors = require('cors');
const http = require('http');
const WebSocket = require('ws');
const { AccessToken } = require('livekit-server-sdk');
const { setupWSConnection } = require('y-websocket/bin/utils');

const port = process.env.PORT || 1234;
const app = express();

const rateLimit = require('express-rate-limit');

const allowedOrigins = [
  'http://localhost:1234',
  'http://127.0.0.1:1234',
  'http://localhost:8080',
  'http://127.0.0.1:8080'
];

const corsOptions = {
  origin: function (origin, callback) {
    if (!origin) return callback(null, true); // Allow non-browser clients or file://
    if (allowedOrigins.indexOf(origin) !== -1 || origin.endsWith('.hf.space')) {
      callback(null, true);
    } else {
      callback(new Error('CORS policy: Access from this origin is not allowed.'));
    }
  }
};

app.use(cors(corsOptions));
app.use(express.json());

const tokenLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 min
  max: 10,
  message: { error: 'Quá nhiều yêu cầu cấp token. Vui lòng thử lại sau 1 phút.' }
});

const uploadLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 min
  max: 20,
  message: { error: 'Quá nhiều yêu cầu upload. Vui lòng thử lại sau 1 phút.' }
});

const LIVEKIT_API_KEY = process.env.LIVEKIT_API_KEY || '';
const LIVEKIT_API_SECRET = process.env.LIVEKIT_API_SECRET || '';
const UPDATE_JAR_URL = process.env.UPDATE_JAR_URL || '';

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
    if (UPDATE_JAR_URL) {
      return res.redirect(302, UPDATE_JAR_URL);
    }
    return res.status(404).send('update.jar not found. Configure UPDATE_JAR_URL or upload update.jar.');
  }

  res.setHeader('Cache-Control', 'no-store');
  res.download(jarPath, 'update.jar');
});

app.get('/livekit/token', tokenLimiter, async (req, res) => {
  try {
    const expectedApiKey = process.env.TUTORHUB_API_KEY;
    if (expectedApiKey) {
      const authHeader = req.headers.authorization;
      if (!authHeader || authHeader !== `Bearer ${expectedApiKey}`) {
        return res.status(401).json({ error: 'Unauthorized: Invalid API Key' });
      }
    }

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
app.post('/upload-record', uploadLimiter, upload.single('video'), async (req, res) => {
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
app.post('/upload-document', uploadLimiter, upload.single('file'), async (req, res) => {
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

// Giữ lại WebSocket Server nhưng sử dụng y-websocket để đồng bộ CRDT
const wss = new WebSocket.Server({ noServer: true });

server.on('upgrade', (request, socket, head) => {
  // Bỏ qua các kết nối không phải WebSocket hợp lệ
  if (request.url === '/livekit/token') return;
  
  wss.handleUpgrade(request, socket, head, (ws) => {
    wss.emit('connection', ws, request);
  });
});

wss.on('connection', (ws, req) => {
  // URL format: /?roomId=...
  // y-websocket parse roomId từ req.url (mặc định lấy phần pathname hoặc cần set docName)
  const url = new URL(req.url, `http://${req.headers.host}`);
  const roomId = url.searchParams.get('roomId') || url.pathname.slice(1) || 'default';
  
  // y-websocket expects the document name to be part of the request or passed to setupWSConnection
  // We can pass the docName in setupWSConnection options (wait, setupWSConnection takes (conn, req, { docName }) in newer versions)
  // Actually, y-websocket setupWSConnection extracts docName from req.url.slice(1).split('?')[0]
  // Let's rewrite req.url so y-websocket uses roomId as docName
  req.url = `/${roomId}`;
  
  setupWSConnection(ws, req);
});

server.listen(port, () => {
  console.log(`[TutorHub Sync] Node.js Server đang chạy tại cổng ${port}`);
  console.log(`- WebSocket CRDT (Yjs): ws://localhost:${port}/?roomId=...`);
  console.log(`- LiveKit Token API: http://localhost:${port}/livekit/token`);
});
