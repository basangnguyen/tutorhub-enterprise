const fs = require('fs');
const path = require('path');

function patchFile(filePath) {
    if (!fs.existsSync(filePath)) {
        console.warn('Cannot find file to patch:', filePath);
        return;
    }
    
    let content = fs.readFileSync(filePath, 'utf8');
    
    // Tìm chỗ lấy strokeWidth từ theme và STROKE_SIZES
    const searchString = 'strokeWidth: theme.strokeWidth * ';
    
    if (content.includes(searchString) && !content.includes('customStrokeWidth')) {
        // Sử dụng regex để thay thế linh hoạt cho cả CJS và ESM
        const regex = /strokeWidth:\s*theme\.strokeWidth\s*\*\s*([a-zA-Z0-9_$.]+)\[size\]/g;
        
        content = content.replace(regex, (match, p1) => {
            return `strokeWidth: (shape.meta && shape.meta.customStrokeWidth !== undefined) ? shape.meta.customStrokeWidth : theme.strokeWidth * ${p1}[size]`;
        });
        
        fs.writeFileSync(filePath, content);
        console.log('Successfully patched Tldraw DrawShapeUtil:', filePath);
    } else {
        console.log('File already patched or search string not found:', filePath);
    }
}

const basePath = path.join(__dirname, 'node_modules', 'tldraw');

// Patch ESM build (dùng cho Vite)
patchFile(path.join(basePath, 'dist-esm', 'lib', 'shapes', 'draw', 'DrawShapeUtil.mjs'));

// Patch CJS build
patchFile(path.join(basePath, 'dist-cjs', 'lib', 'shapes', 'draw', 'DrawShapeUtil.js'));
