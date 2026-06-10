const https = require('https');
const fs = require('fs');

const urlEn = 'https://phet.colorado.edu/services/metadata/1.3/simulations?format=json&type=html&locale=en';
const urlVi = 'https://phet.colorado.edu/services/metadata/1.3/simulations?format=json&type=html&locale=vi';

// Helper để fetch JSON
const fetchJson = (url) => new Promise((resolve, reject) => {
    https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => resolve(JSON.parse(data)));
        res.on('error', reject);
    }).on('error', reject);
});

async function run() {
    try {
        console.log("Fetching EN...");
        const enData = await fetchJson(urlEn);
        console.log("Fetching VI...");
        const viData = await fetchJson(urlVi);

        const viTitles = {};
        if (viData.projects) {
            viData.projects.forEach(proj => {
                proj.simulations.forEach(sim => {
                    if (sim.localizedSimulations && sim.localizedSimulations.vi) {
                        viTitles[sim.name] = sim.localizedSimulations.vi.title;
                    }
                });
            });
        }

        let allSims = [];
        
        enData.projects.forEach(proj => {
            let category = (proj.subjects && proj.subjects[0]) ? proj.subjects[0] : 'Khoa học';
            // Map môn học sang Tiếng Việt
            const subMap = {
                1: 'Vật lý', 2: 'Vật lý', 3: 'Vật lý', 4: 'Vật lý', 5: 'Vật lý', 6: 'Vật lý', 7: 'Vật lý', 8: 'Vật lý',
                9: 'Sinh học', 10: 'Sinh học',
                13: 'Hóa học', 14: 'Hóa học', 15: 'Hóa học',
                19: 'Khoa học trái đất',
                21: 'Toán học', 22: 'Toán học', 23: 'Toán học'
            };
            category = subMap[category] || 'Khoa học';

            proj.simulations.forEach(sim => {
                // Lấy URL chạy trực tiếp (nếu có vi thì dùng vi, không thì en, nhưng an toàn nhất là allLocalesSimURL)
                let runUrl = sim.allLocalesSimURL 
                    ? `https://phet.colorado.edu${sim.allLocalesSimURL}` 
                    : (sim.localizedSimulations.en ? `https://phet.colorado.edu${sim.localizedSimulations.en.runUrl}` : null);
                
                if (runUrl) {
                    let title = viTitles[sim.name] || (sim.localizedSimulations.en ? sim.localizedSimulations.en.title : sim.name);
                    allSims.push({
                        title: title,
                        category: category,
                        url: runUrl
                    });
                }
            });
        });

        // Xóa trùng lặp
        const uniqueSims = [];
        const urls = new Set();
        for (let sim of allSims) {
            if (!urls.has(sim.url)) {
                urls.add(sim.url);
                uniqueSims.push(sim);
            }
        }

        const jsContent = `export const phetSims = ${JSON.stringify(uniqueSims, null, 2)};`;
        fs.writeFileSync('src/phetData.js', jsContent);
        console.log(`Success! Wrote ${uniqueSims.length} simulations to src/phetData.js`);
    } catch (e) {
        console.error("Error:", e);
    }
}

run();
