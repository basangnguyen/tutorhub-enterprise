const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const { execSync, spawn } = require('child_process');
const cookieParser = require('cookie-parser');

const app = express();
app.use(cors());
app.use(cookieParser());

// Bản đồ ánh xạ User -> Port
const portMap = {};
let nextPort = 8081;

// Hàm hỗ trợ lấy Cookie từ Raw Request (cho WebSocket)
function getCookie(req, name) {
    const cookieHeader = req.headers.cookie;
    if (!cookieHeader) return null;
    const match = cookieHeader.match(new RegExp('(^| )' + name + '=([^;]+)'));
    if (match) return decodeURIComponent(match[2]);
    return null;
}

// Hàm khởi tạo Máy chủ cá nhân (Tạo User, Thư mục, Phân quyền, Giới hạn tài nguyên)
async function getOrStartUserServer(username) {
    if (!/^[a-zA-Z0-9_]+$/.test(username)) throw new Error("Tên user không hợp lệ");
    if (portMap[username]) return portMap[username];

    const port = nextPort++;
    portMap[username] = port; // Đặt chỗ trước
    const workspaceDir = `/workspace/${username}`;

    try {
        console.log(`[Mini Cloud] Đang khởi tạo môi trường cho: ${username} tại cổng ${port}`);

        // 1. Tạo User Linux ngầm (Không password)
        try { execSync(`id -u ${username}`); } 
        catch (e) { execSync(`sudo useradd -m -s /bin/bash ${username}`); }

        // 2. Tạo Thư mục và Khóa quyền sở hữu
        execSync(`sudo mkdir -p ${workspaceDir}`);
        execSync(`sudo chown -R ${username}:${username} ${workspaceDir}`);
        // Chặn không cho user khác đọc/ghi
        execSync(`sudo chmod 700 ${workspaceDir}`);

        // 3. Chạy Code-Server dưới quyền của User này + Giới hạn Tài nguyên
        // ulimit -u 150 : Max 150 tiến trình (Chống Fork bomb)
        // cpulimit -l 40 : Giới hạn CPU 40% (Chống treo máy)
        const cmd = `sudo -u ${username} bash -c "ulimit -u 150; cpulimit -l 40 -- code-server --bind-addr 127.0.0.1:${port} --auth none --disable-telemetry ${workspaceDir}"`;
        
        const child = spawn('sh', ['-c', cmd], { detached: true, stdio: 'ignore' });
        child.unref();

        // Chờ 3 giây để code-server khởi động xong
        await new Promise(resolve => setTimeout(resolve, 3000));
        
        console.log(`[Mini Cloud] Đã tạo thành công cho ${username}`);
        return port;
    } catch (e) {
        console.error(`[Mini Cloud] Lỗi khi tạo môi trường cho ${username}:`, e);
        delete portMap[username]; // Xóa map để thử lại lần sau
        throw e;
    }
}

// ---------------------------------------------------------
// 1. Endpoint Đăng nhập (Gắn Cookie)
// ---------------------------------------------------------
app.get('/login', (req, res) => {
    const user = req.query.user;
    if (user && /^[a-zA-Z0-9_]+$/.test(user)) {
        res.cookie('tutorhub_user', user, { maxAge: 86400000, httpOnly: false });
        res.redirect('/');
    } else {
        res.status(400).send("Tên đăng nhập không hợp lệ! Vui lòng không dùng ký tự đặc biệt.");
    }
});

// ---------------------------------------------------------
// 2. API: Lấy Code để Chấm điểm (Lấy theo User)
// ---------------------------------------------------------
app.get('/tutorhub-api/get-code', (req, res) => {
    try {
        const user = req.query.user;
        if (!user || !/^[a-zA-Z0-9_]+$/.test(user)) {
            return res.status(400).json({ success: false, error: "Thiếu user" });
        }

        const workspaceDir = `/workspace/${user}`;
        const filesMap = {};
        
        function readDirRecursively(dir, prefix = '') {
            const items = fs.readdirSync(dir);
            for (let item of items) {
                if (item.startsWith('.') || item === 'node_modules') continue;
                const fullPath = path.join(dir, item);
                const stat = fs.statSync(fullPath);
                
                if (stat.isDirectory()) {
                    readDirRecursively(fullPath, prefix + item + '/');
                } else if (stat.isFile()) {
                    filesMap[prefix + item] = fs.readFileSync(fullPath, 'utf8');
                }
            }
        }

        if (fs.existsSync(workspaceDir)) {
            // Dùng sudo cat để đọc file vì node đang chạy quyền root/coder, file của hs_a có quyền 700.
            // À, mặc định server.js chạy bằng user 'coder'.
            // Thư mục của hs_a được chown cho hs_a và chmod 700. Do đó 'coder' KHÔNG THỂ đọc bằng fs.readFileSync!
            // Phải dùng sudo để đọc!
        }

        // --- CÁCH ĐỌC FILE KHI BỊ PHÂN QUYỀN LINUX ---
        // Vì server.js bị chặn quyền, ta dùng lệnh `sudo find` và `sudo cat` để lấy nội dung.
        if (fs.existsSync(workspaceDir)) {
            try {
                // Liệt kê các file
                const filesListStr = execSync(`sudo find ${workspaceDir} -type f -not -path "*/\\.*" -not -path "*/node_modules/*"`).toString().trim();
                if (filesListStr) {
                    const filePaths = filesListStr.split('\n');
                    for (let fp of filePaths) {
                        const relativePath = fp.replace(workspaceDir + '/', '');
                        // Đọc nội dung
                        const content = execSync(`sudo cat ${fp}`).toString();
                        filesMap[relativePath] = content;
                    }
                }
            } catch(e) {
                console.error("Lỗi đọc file sudo:", e);
            }
        }

        res.json({ success: true, files: filesMap });
    } catch (e) {
        console.error(e);
        res.status(500).json({ success: false, error: e.message });
    }
});

// ---------------------------------------------------------
// 3. Proxy Động (Dynamic Proxy)
// ---------------------------------------------------------
const dynamicProxy = createProxyMiddleware({ 
    // Target mặc định (Sẽ bị ghi đè bởi router)
    target: 'http://127.0.0.1:8080', 
    router: async function(req) {
        const username = req.cookies ? req.cookies.tutorhub_user : getCookie(req, 'tutorhub_user');
        
        if (!username) {
            // Chưa đăng nhập, trả về cổng mặc định (8080)
            return 'http://127.0.0.1:8080';
        }

        try {
            const port = await getOrStartUserServer(username);
            return `http://127.0.0.1:${port}`;
        } catch (e) {
            return 'http://127.0.0.1:8080';
        }
    },
    ws: true,
    changeOrigin: true,
    onProxyReqWs: (proxyReq, req, socket, options, head) => {
        // options.target chứa URL target thực tế đã được router định tuyến
        const targetHost = options.target.host;
        proxyReq.setHeader('Origin', `http://${targetHost}`);
        proxyReq.setHeader('Host', targetHost);
    }
});

// Bỏ qua các đường dẫn API và Login
app.use((req, res, next) => {
    if (req.path.startsWith('/tutorhub-api') || req.path.startsWith('/login')) {
        return next();
    }
    dynamicProxy(req, res, next);
});

const PORT = 7860;
const server = app.listen(PORT, () => {
    console.log(`[Mini Cloud] Proxy điều phối đang chạy ở cổng ${PORT}`);
});

// BẮT BUỘC PHẢI CÓ DÒNG NÀY ĐỂ WEBSOCKET HOẠT ĐỘNG
server.on('upgrade', dynamicProxy.upgrade);
