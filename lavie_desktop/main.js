const { app, BrowserWindow, screen, ipcMain } = require('electron')

function createWindow () {
  const { width, height } = screen.getPrimaryDisplay().workAreaSize;

  const win = new BrowserWindow({
    width: width,
    height: height,
    x: 0,
    y: 0,
    transparent: true,
    frame: false, 
    hasShadow: false, 
    alwaysOnTop: true,
    skipTaskbar: true, // Chạy ngầm, không hiện dưới taskbar
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  })

  // Ép quyền hiển thị tối đa để không bao giờ bị ứng dụng khác đè lên
  win.setAlwaysOnTop(true, 'screen-saver');

  // Đặt mặc định là bỏ qua click (xuyên thấu xuống desktop/app bên dưới)
  win.setIgnoreMouseEvents(true, { forward: true })

  // Lắng nghe sự kiện từ renderer.js để bật/tắt khả năng click
  ipcMain.on('set-ignore-mouse-events', (event, ignore, options) => {
    const webContents = event.sender
    const win = BrowserWindow.fromWebContents(webContents)
    win.setIgnoreMouseEvents(ignore, options)
  })

  // Lắng nghe sự kiện click từ Lavie và gửi UDP về Java
  ipcMain.on('lavie-clicked', () => {
    const dgramClient = require('dgram').createSocket('udp4');
    dgramClient.send('LAVIE_CLICKED', 15001, '127.0.0.1', (err) => {
      dgramClient.close();
    });
  })

  // Lắng nghe sự kiện di chuyển từ Lavie và gửi UDP về Java
  ipcMain.on('lavie-moved', (event, pos) => {
    const dgramClient = require('dgram').createSocket('udp4');
    const msg = `POS:${pos.x},${pos.y}`;
    dgramClient.send(msg, 15001, '127.0.0.1', (err) => {
      dgramClient.close();
    });
  })

  win.loadFile('index.html')

  // Khởi tạo UDP Server để nhận Volume từ Java
  const dgram = require('dgram');
  const server = dgram.createSocket('udp4');
  
  server.on('message', (msg) => {
    const text = msg.toString();
    if (text.startsWith("STATE:")) {
      if (win && !win.isDestroyed()) {
        win.webContents.send('lavie-state', text.substring(6).trim());
      }
    } else {
      try {
        const vol = parseFloat(text);
        if (win && !win.isDestroyed() && !isNaN(vol)) {
          win.webContents.send('lipsync', vol);
        }
      } catch (e) {}
    }
  });
  
  server.on('error', () => {});
  
  try {
    server.bind(15000, '127.0.0.1');
  } catch (e) {}
}

app.whenReady().then(() => {
  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
