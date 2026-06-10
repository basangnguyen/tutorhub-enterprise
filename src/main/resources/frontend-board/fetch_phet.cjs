const https = require('https');
const fs = require('fs');

const url = 'https://phet.colorado.edu/services/metadata/1.2/simulations?format=json&type=html';

https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, (res) => {
    let data = '';
    res.on('data', (chunk) => {
        data += chunk;
    });
    res.on('end', () => {
        try {
            fs.writeFileSync('phet_raw.json', data);
            console.log('Successfully written raw data.');
            
            const json = JSON.parse(data);
            console.log(`Total projects: ${json.projects.length}`);
            if (json.projects.length > 0) {
                console.log('Sample project:', JSON.stringify(json.projects[0], null, 2));
            }
        } catch (e) {
            console.error('Error parsing JSON: ', e.message);
        }
    });
}).on('error', (e) => {
    console.error('Error fetching: ', e.message);
});
