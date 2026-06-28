const puppeteer = require('puppeteer');
const path = require('path');

(async () => {
  const browser = await puppeteer.launch({
    headless: "new",
    args: ['--allow-file-access-from-files']
  });
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('LOG:', msg.text()));
  page.on('pageerror', err => console.log('ERR:', err.stack || err.toString()));
  page.on('requestfailed', request => {
    console.log('REQ_FAILED:', request.url(), request.failure().errorText);
  });
  
  const fileUrl = 'file:///' + path.join(__dirname, 'src/main/resources/cert_excel.html').replace(/\\/g, '/');
  console.log('Opening:', fileUrl);
  await page.goto(fileUrl, { waitUntil: 'networkidle2' });
  
  await browser.close();
})();
