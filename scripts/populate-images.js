/**
 * populate-images.js
 * ==================
 * Script Node.js tự động lấy URL ảnh thật từ Unsplash API
 * và cập nhật vào vietnam_landmarks.json.
 *
 * Chạy 1 lần sau khi có Unsplash API key:
 *   node scripts/populate-images.js ACCESS_KEY_CUA_BAN
 *
 * Sau khi chạy: commit lại file vietnam_landmarks.json đã được cập nhật.
 */

const fs = require('fs');
const path = require('path');
const https = require('https');

const ACCESS_KEY = process.argv[2];
if (!ACCESS_KEY) {
  console.error('❌ Thiếu Unsplash Access Key!');
  console.error('   Cách dùng: node scripts/populate-images.js YOUR_KEY');
  console.error('   Đăng ký miễn phí: https://unsplash.com/developers');
  process.exit(1);
}

const JSON_PATH = path.join(__dirname, '..', 'public', 'data', 'vietnam_landmarks.json');

/** Mapping từ landmark ID sang query Unsplash tốt nhất */
const UNSPLASH_QUERIES = {
  'ha-long-bay':              'ha long bay vietnam',
  'mu-cang-chai':             'mu cang chai rice terrace vietnam',
  'sapa-rice-terrace':        'sapa vietnam rice terraces misty',
  'hoan-kiem-lake':           'hoan kiem lake hanoi vietnam',
  'son-doong-cave':           'son doong cave vietnam',
  'phong-nha-ke-bang':        'phong nha cave vietnam',
  'ban-gioc-waterfall':       'ban gioc waterfall vietnam',
  'ninh-binh':                'ninh binh tam coc vietnam',
  'bai-dinh-temple':          'pagoda vietnam temple',
  'dong-van-plateau':         'ha giang vietnam mountain pass',
  'hoi-an-ancient-town':      'hoi an vietnam lanterns',
  'golden-bridge-ba-na':      'golden bridge ba na hills vietnam',
  'hai-van-pass':             'hai van pass vietnam coast',
  'da-nang-beach':            'da nang beach vietnam',
  'my-son-sanctuary':         'my son champa temple vietnam',
  'lang-co-lagoon':           'lang co vietnam lagoon',
  'hue-imperial-city':        'hue citadel imperial vietnam',
  'mui-ne-sand-dune':         'mui ne sand dunes vietnam',
  'ly-son-island':            'vietnam island tropical aerial',
  'da-lat-lake':              'da lat vietnam lake pine forest',
  'mekong-delta':             'mekong delta vietnam aerial river',
  'phu-quoc-island':          'phu quoc island vietnam beach',
  'con-dao-island':           'con dao island vietnam pristine',
  'cat-tien-national-park':   'vietnam tropical rainforest',
  'cai-rang-floating-market': 'mekong floating market vietnam',
  'notre-dame-saigon':        'notre dame cathedral saigon',
  'ben-nha-rong':             'ho chi minh city river saigon',
  'buon-ma-thuot-highland':   'vietnam coffee highland plantation',
  'nha-trang-beach':          'nha trang bay vietnam turquoise',
  'binh-ba-island':           'vietnam tropical island clear water',
};

function httpsGet(url) {
  return new Promise((resolve, reject) => {
    const options = {
      headers: {
        'Authorization': `Client-ID ${ACCESS_KEY}`,
        'Accept-Version': 'v1',
      },
    };
    https.get(url, options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        if (res.statusCode === 200) resolve(JSON.parse(data));
        else reject(new Error(`HTTP ${res.statusCode}: ${data}`));
      });
    }).on('error', reject);
  });
}

async function fetchUnsplashPhoto(query) {
  const url = `https://api.unsplash.com/search/photos?query=${encodeURIComponent(query)}&orientation=landscape&per_page=3&content_filter=high&order_by=relevant`;
  const data = await httpsGet(url);
  const photo = data.results?.[0];
  if (!photo) throw new Error(`No results for: ${query}`);
  return {
    image_url:      `${photo.urls.raw}&w=1920&q=85&fit=crop&auto=format`,
    image_thumb:    `${photo.urls.raw}&w=800&q=70&fit=crop&auto=format`,
    image_credit:   photo.user.name,
    image_credit_url: `${photo.user.links.html}?utm_source=tutorhub&utm_medium=referral`,
    image_source:   'Unsplash',
    unsplash_id:    photo.id,
    download_url:   photo.links.download_location,
  };
}

async function main() {
  console.log('🚀 Bắt đầu populate ảnh thật từ Unsplash...\n');
  const json = JSON.parse(fs.readFileSync(JSON_PATH, 'utf8'));
  let updated = 0, skipped = 0, failed = 0;

  for (const landmark of json.landmarks) {
    // Bỏ qua nếu đã có ảnh thật (không phải Picsum)
    if (landmark.image_source !== 'Picsum' && landmark.unsplash_id) {
      console.log(`  ⏭️  ${landmark.id} — đã có ảnh thật, bỏ qua`);
      skipped++;
      continue;
    }

    const query = UNSPLASH_QUERIES[landmark.id];
    if (!query) {
      console.warn(`  ⚠️  ${landmark.id} — không có query mapping, bỏ qua`);
      skipped++;
      continue;
    }

    try {
      console.log(`  🔍 ${landmark.id} — query: "${query}"`);
      const photo = await fetchUnsplashPhoto(query);
      Object.assign(landmark, photo);
      console.log(`  ✅ ${landmark.id} — OK (by ${photo.image_credit})`);
      updated++;
      // Chờ 250ms giữa các request để không vượt rate limit
      await new Promise(r => setTimeout(r, 250));
    } catch (err) {
      console.error(`  ❌ ${landmark.id} — FAIL: ${err.message}`);
      failed++;
    }
  }

  json.lastUpdated = new Date().toISOString().split('T')[0];
  json.note = 'Ảnh được lấy từ Unsplash API. Đã có ảnh thật.';
  fs.writeFileSync(JSON_PATH, JSON.stringify(json, null, 2), 'utf8');

  console.log(`\n📊 Kết quả:`);
  console.log(`   ✅ Cập nhật: ${updated}`);
  console.log(`   ⏭️  Bỏ qua:  ${skipped}`);
  console.log(`   ❌ Thất bại: ${failed}`);
  console.log('\n✨ Xong! Commit lại vietnam_landmarks.json vào git.');
}

main().catch(err => {
  console.error('💥 Script crash:', err);
  process.exit(1);
});
