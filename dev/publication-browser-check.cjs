const {chromium, webkit} = require('/workspace/artifacts/phone-browser/node_modules/playwright');
const {PNG} = require('/workspace/artifacts/phone-browser/node_modules/pngjs');
const jsQR = require('/workspace/artifacts/phone-browser/node_modules/jsqr');
const {spawn} = require('node:child_process');
const fs = require('node:fs');
const assert = require('node:assert/strict');
(async()=>{
  fs.mkdirSync('/workspace/artifacts/publication-browser',{recursive:true});
  const server=spawn('python3',['-m','http.server','8763','--bind','127.0.0.1','--directory','/site/dist']);
  const ready=new Promise((resolve,reject)=>{server.once('error',reject);server.stderr.once('data',resolve);setTimeout(resolve,700);});
  try {
    await ready;
    for (const [engine,type] of Object.entries({chromium,webkit})) {
      const browser=await type.launch();
      try {
        for(const [name,width] of [['phone',390],['desktop',1280]]){
          const page=await browser.newPage({viewport:{width,height:900}});
          const errors=[];page.on('pageerror',e=>errors.push(e.message));
          for(const path of ['/homeboard/','/homeboard/guide/','/homeboard/guide/print.html']){
            const response=await page.goto('http://127.0.0.1:8763'+path);
            assert.equal(response.status(),200);
            await page.evaluate(()=>Promise.all([...document.images].map(i=>i.decode())));
            assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),path+' overflow');
            const targets=await page.locator('a[href],img[src]').evaluateAll(nodes=>nodes.map(n=>n.href||n.src).filter(u=>u.startsWith(location.origin)).map(u=>new URL(u).pathname));
            for(const target of new Set(targets)){
              const resolved='/site/dist'+decodeURIComponent(target);
              assert(fs.existsSync(resolved),`Missing local target ${target}`);
            }
            if(path==='/homeboard/'){
              const download=page.getByRole('link',{name:/^Download APK/});
              assert((await download.getAttribute('href')).endsWith('.apk'));
              await page.keyboard.press('Tab');
              assert(await page.evaluate(()=>document.activeElement.tagName==='A'));
              await page.screenshot({path:`/workspace/artifacts/publication-browser/${engine}-${name}.png`,fullPage:true});
            }
            if(path.endsWith('print.html') && name==='desktop'){
              const png=PNG.sync.read(await page.locator('.download-qr').screenshot({scale:'css'}));
              const code=jsQR(new Uint8ClampedArray(png.data),png.width,png.height);
              assert.equal(code?.data,'https://biohack.net/homeboard/');
              for(let i=0;i<await page.locator('main').count();i++){
                await page.locator('main').nth(i).screenshot({path:`/workspace/artifacts/publication-browser/${engine}-guide-${i+1}.png`});
              }
            }
          }
          assert.deepEqual(errors,[]);await page.close();
          console.log(`${engine} ${name}: page, illustrated guide, print guide, images, local links and width passed`);
        }
      } finally { await browser.close(); }
    }
  } finally {server.kill('SIGTERM');}
})().catch(e=>{console.error(e);process.exit(1)});
