const puppeteer = require('puppeteer');

(async () => {
    const browser = await puppeteer.launch({ headless: true });
    const page = await browser.newPage();
    page.on('console', msg => console.log('PAGE LOG:', msg.text()));
    page.on('requestfailed', request => console.log('PAGE LOG FAILED REQUEST:', request.url(), request.failure().errorText));
    page.on('response', response => {
        if (!response.ok()) {
            console.log('PAGE LOG 404/500:', response.url(), response.status());
        }
    });
    page.on('pageerror', err => console.log('PAGE ERROR:', err.toString()));
    
    await page.setViewport({ width: 1280, height: 800 });
    console.log("Navigating...");
    await page.goto('http://localhost:5055/tldraw_board_v2.html', { waitUntil: 'networkidle2' });
    console.log("Waiting for 2 seconds...");
    await new Promise(r => setTimeout(r, 2000));
    console.log("Taking screenshot...");
    await page.screenshot({ path: 'test_screenshot.png' });
    const numChildren = await page.evaluate(() => document.getElementById('root').children.length);
    console.log("Root children count:", numChildren);
    const rootHtml = await page.evaluate(() => document.getElementById('root').innerHTML.substring(0, 500));
    console.log("Root HTML:", rootHtml);
    console.log("Done.");
    await browser.close();
})();
