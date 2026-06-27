const fs = require('fs');

const html = fs.readFileSync('./src/main/resources/auth-success/auth-success.html', 'utf8');
const css = fs.readFileSync('./src/main/resources/auth-success/auth-success.css', 'utf8');
const js = fs.readFileSync('./src/main/resources/auth-success/auth-success.js', 'utf8');

const logomoi = fs.readFileSync('./src/main/resources/images/logomoi.png').toString('base64');
const facebook = fs.readFileSync('./src/main/resources/images/icon/facebook.svg').toString('base64');
const zalo = fs.readFileSync('./src/main/resources/images/icon/zalo-2.png').toString('base64');
const instagram = fs.readFileSync('./src/main/resources/images/icon/instagram.svg').toString('base64');
const google = fs.readFileSync('./src/main/resources/images/icon/google.svg').toString('base64');

let fullHtml = html;
fullHtml = fullHtml.replace('<link rel="stylesheet" href="auth-success.css" />', `<style>\n${css}\n</style>`);
fullHtml = fullHtml.replace('<script src="auth-success.js"></script>', `<script>\n${js}\n</script>`);

fullHtml = fullHtml.replace('src="../images/logomoi.png"', `src="data:image/png;base64,${logomoi}"`);
fullHtml = fullHtml.replace('src="../images/logomoi.png"', `src="data:image/png;base64,${logomoi}"`);
fullHtml = fullHtml.replace('src="/images/icon/facebook.svg"', `src="data:image/svg+xml;base64,${facebook}"`);
fullHtml = fullHtml.replace('src="/images/icon/zalo-2.png"', `src="data:image/png;base64,${zalo}"`);
fullHtml = fullHtml.replace('src="/images/icon/instagram.svg"', `src="data:image/svg+xml;base64,${instagram}"`);

// Inject window.__AUTH_PROVIDER and DOM update logic before </body>
const providerScript = `
<script>
window.__AUTH_PROVIDER = 'facebook';
(function(){
  var p = window.__AUTH_PROVIDER;
  var el = document.getElementById('providerLabel');
  var nm = document.getElementById('providerName');
  var icon = document.getElementById('providerIcon');
  if(el && nm && p) {
    if(p === 'facebook') { 
      nm.textContent = 'Facebook'; 
      if (icon) icon.src = 'data:image/svg+xml;base64,${facebook}'; 
      el.style.display = 'inline-flex'; 
    }
  }
})();
</script>
</body>`;
fullHtml = fullHtml.replace('</body>', providerScript);

let workerScript = fs.readFileSync('./cloudflare/facebook-oauth-worker.js', 'utf8');

// In workerScript we generated: let htmlResponse = `<!DOCTYPE html>...
// We will find "let htmlResponse = `<!DOCTYPE html>" and end at "return new Response(htmlResponse, {"
let startIdx = workerScript.indexOf('let htmlResponse = `<!DOCTYPE html>');
let endIdx = workerScript.indexOf('return new Response(htmlResponse, {');

// Fallback if it's the original file
if (startIdx === -1) {
    startIdx = workerScript.indexOf('const html = `<html>');
    endIdx = workerScript.indexOf('return new Response(html, {');
}

if (startIdx !== -1 && endIdx !== -1) {
    const before = workerScript.substring(0, startIdx);
    const after = workerScript.substring(endIdx);
    
    // the backticks might conflict if fullHtml contains backticks, so we'll use JSON.stringify or properly escape.
    // fullHtml shouldn't contain backticks normally, but JS inside it might (template literals).
    // Using string concatenation or escaping backticks:
    const escapedHtml = fullHtml.replace(/`/g, '\\`').replace(/\$\{/g, '\\${');
    
    // We also want to keep the debug info for when success is false!
    const newHtmlLogic = `
  let htmlResponse = \`${escapedHtml}\`;

  if (!success) {
    htmlResponse = \`<html><head><meta charset='UTF-8'><title>Lỗi xử lý</title></head>
      <body style='font-family: sans-serif; text-align: center; padding: 50px;'>
      <h1 style='color: #F44336;'>Đã xảy ra lỗi.</h1>
      <div style='margin-top: 20px; text-align: left; display: inline-block; background: #f0f0f0; padding: 20px; border-radius: 8px;'>
          <p><b>Token exchange:</b> \${tokenStatus}</p>
          <p><b>Profile fetch:</b> \${profileStatus}</p>
          <p><b>HF result status:</b> \${finalHfStatus}</p>
          \${errorMessage ? \`<p style='color: red;'><b>Lỗi nội bộ:</b> \${errorMessage}</p>\` : ''}
      </div>
      <p style='margin-top: 20px;'>Vui lòng quay lại ứng dụng TutorHub.</p>
      </body></html>\`;
  }

  `;
    const newWorkerScript = before + newHtmlLogic + after.replace('Response(html, {', 'Response(htmlResponse, {');
    fs.writeFileSync('./cloudflare/facebook-oauth-worker.js', newWorkerScript);
    console.log("Updated worker script successfully!");
} else {
    console.error("Could not find html block in worker script.");
}
