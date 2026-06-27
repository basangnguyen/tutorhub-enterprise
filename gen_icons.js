const fs = require('fs');
const path = require('path');
const dir = './src/main/resources/images/tab-icons';
if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });

const generateSvg = (name, d, color='#4F46E5', fill='#EEF2FF') => {
  const svg = `<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
<rect width="24" height="24" rx="6" fill="${fill}"/>
<path d="${d}" fill="${color}"/>
</svg>`;
  fs.writeFileSync(path.join(dir, name + '.svg'), svg);
};

// Simple clean icons with a colored background block and colored path
generateSvg('home', 'M10 20V14H14V20H19V12H22L12 3L2 12H5V20H10Z');
generateSvg('reels', 'M17 10.5V7C17 6.44772 16.5523 6 16 6H4C3.44772 6 3 6.44772 3 7V17C3 17.5523 3.44772 18 4 18H16C16.5523 18 17 17.5523 17 17V13.5L21 17.5V6.5L17 10.5Z', '#EC4899', '#FCE7F3');
generateSvg('message', 'M20 2H4C2.9 2 2 2.9 2 4V22L6 18H20C21.1 18 22 17.1 22 16V4C22 2.9 21.1 2 20 2Z', '#10B981', '#D1FAE5');
generateSvg('my-class', 'M12 3L1 9L12 15L21 10.09V17H23V9L12 3ZM5 13.18V17.18L12 21L19 17.18V13.18L12 17L5 13.18Z', '#F59E0B', '#FEF3C7');
generateSvg('accepted-class', 'M9 16.17L4.83 12L3.41 13.41L9 19L21 7L19.59 5.59L9 16.17Z', '#8B5CF6', '#EDE9FE');
generateSvg('calendar', 'M19 3H18V1H16V3H8V1H6V3H5C3.9 3 3 3.9 3 5V19C3 20.1 3.9 21 5 21H19C20.1 21 21 20.1 21 19V5C21 3.9 20.1 3 19 3ZM19 19H5V8H19V19Z', '#3B82F6', '#DBEAFE');
generateSvg('exam', 'M12 3L1 9L12 15L21 10.09V17H23V9L12 3ZM5 13.18V17.18L12 21L19 17.18V13.18L12 17L5 13.18Z', '#EF4444', '#FEE2E2');
generateSvg('quizhub', 'M21.58 16.09L12 21.66L2.42 16.09L12 10.51L21.58 16.09ZM21.58 10.51L12 16.09L2.42 10.51L12 4.92L21.58 10.51ZM12 21.66L12 10.51ZM12 4.92L12 16.09Z', '#14B8A6', '#CCFBF1');
generateSvg('paper', 'M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.89 22 5.99 22H18C19.1 22 20 21.1 20 20V8L14 2ZM16 18H8V16H16V18ZM16 14H8V12H16V14ZM13 9V3.5L18.5 9H13Z', '#F97316', '#FFEDD5');
generateSvg('question-bank', 'M4 6H2V20C2 21.1 2.9 22 4 22H18V20H4V6ZM20 2H8C6.9 2 6 2.9 6 4V16C6 17.1 6.9 18 8 18H20C21.1 18 22 17.1 22 16V4C22 2.9 21.1 2 20 2ZM20 16H8V4H20V16Z', '#6366F1', '#E0E7FF');
generateSvg('task', 'M19 3H14.82C14.4 1.84 13.3 1 12 1C10.7 1 9.6 1.84 9.18 3H5C3.9 3 3 3.9 3 5V19C3 20.1 3.9 21 5 21H19C20.1 21 21 20.1 21 19V5C21 3.9 20.1 3 19 3ZM12 3C12.55 3 13 3.45 13 4C13 4.55 12.55 5 12 5C11.45 5 11 4.55 11 4C11 3.45 11.45 3 12 3ZM10 17L5 12L6.41 10.59L10 14.17L17.59 6.58L19 8L10 17Z', '#8B5CF6', '#EDE9FE');
generateSvg('document', 'M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.89 22 5.99 22H18C19.1 22 20 21.1 20 20V8L14 2ZM16 18H8V16H16V18ZM16 14H8V12H16V14ZM13 9V3.5L18.5 9H13Z', '#6B7280', '#F3F4F6');
generateSvg('drawing', 'M12 3L2 12H5V20H10V14H14V20H19V12H22L12 3Z', '#F43F5E', '#FFE4E6');

console.log('Icons generated.');
