const fs = require('fs');
const file = 'd:/Ban_sao_du_an/src/main/resources/html/tldraw_board_v2.html';
const content = fs.readFileSync(file, 'utf8');
const replaced = content.replace('<script src="/js/board-core.js"></script>', '<script type="module" src="/js/board-core.js"></script>');
fs.writeFileSync(file, replaced);
console.log('done');
