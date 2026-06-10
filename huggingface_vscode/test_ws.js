const WebSocket = require('ws');

const ws = new WebSocket('ws://localhost:7860/');

ws.on('open', function open() {
  console.log('connected');
  ws.send('something');
});

ws.on('message', function incoming(data) {
  console.log('received: %s', data);
});

ws.on('error', function error(err) {
  console.error('error:', err);
});
