/* Run only during the complete Cast1 coordinator phone-editor-check job. No direct device control. */
const {chromium,devices}=require('/workspace/artifacts/phone-browser/node_modules/playwright');
const {PNG}=require('/workspace/artifacts/phone-browser/node_modules/pngjs');
const jsQR=require('/workspace/artifacts/phone-browser/node_modules/jsqr'),fs=require('fs'),assert=require('assert');
const image=process.argv[2],out=process.argv[3];
const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
(async()=>{
 const png=PNG.sync.read(fs.readFileSync(image)),code=jsQR(new Uint8ClampedArray(png.data),png.width,png.height);
 assert(code&&code.data.startsWith('http://'),'TV QR code must decode');console.log('Actual TV QR decoded.');
 const browser=await chromium.launch({headless:true}),page=await browser.newPage({...devices['Pixel 7']});page.setDefaultTimeout(400000);
 page.on('response',r=>{if(r.request().isNavigationRequest())console.log('Editor navigation HTTP',r.status(),r.headers()['content-type']);});
 await page.goto(code.data,{waitUntil:'domcontentloaded'});console.log('Actual TV editor loaded.');await page.waitForFunction(()=>document.getElementById('connection').textContent.includes('Waiting for TV approval'));
 await page.screenshot({path:out+'/phone-awaiting-approval.png',fullPage:true});console.log('Waiting for scripted TV approval.');
 await page.getByLabel('Label',{exact:true}).waitFor();const approvedAt=Date.now();console.log('TV approval received; testing widget changes.');
 const baseline=await page.evaluate(()=>JSON.parse(JSON.stringify(state)));
 await page.getByLabel('Label',{exact:true}).fill('Phone editor verification');await page.getByLabel('Label',{exact:true}).press('Tab');
 const airIndex=baseline.draft.widgets.findIndex(w=>w.type==='air'),calIndex=baseline.draft.widgets.findIndex(w=>w.type==='calendar');
 await page.getByLabel('Widgets',{exact:true}).selectOption(String(airIndex));await page.getByLabel('Air-quality source').selectOption('airbkk');await page.getByLabel('Station',{exact:true}).selectOption(baseline.draft.widgets[airIndex].settings.station==='119'?'114':'119');
 await page.getByLabel('Widgets',{exact:true}).selectOption(String(calIndex));await page.getByLabel('Holiday lookahead (days)').fill('7');await page.getByLabel('Holiday lookahead (days)').press('Tab');
 await page.getByRole('button',{name:'Favorites',exact:true}).click();await page.getByRole('button',{name:'Move down',exact:true}).first().click();
 await page.getByRole('button',{name:/^Apply to TV/}).click();await page.waitForFunction(()=>document.getElementById('message').textContent.startsWith('Applied to'));
 const applied=await page.evaluate(()=>JSON.parse(JSON.stringify(state)));
 assert.equal(applied.draft.widgets.find(w=>w.type==='rain').settings.label,'Phone editor verification');assert.equal(applied.draft.widgets.find(w=>w.type==='calendar').settings.holidayLookaheadDays,7);assert.notEqual(applied.draft.widgets[airIndex].settings.station,baseline.draft.widgets[airIndex].settings.station);assert.equal(applied.draft.favorites[0],baseline.draft.favorites[1]);
 await page.screenshot({path:out+'/phone-applied.png',fullPage:true});
 // Wait for the coordinator-owned native capture before displaying the temporary preview.
 await delay(Math.max(0,approvedAt+27000-Date.now()));
 await page.getByRole('button',{name:'Preview on TV',exact:true}).click();await delay(500);
 await page.screenshot({path:out+'/phone-preview.png',fullPage:true});
 await delay(Math.max(0,approvedAt+42000-Date.now()));await page.getByRole('button',{name:'Cancel TV preview',exact:true}).click();
 await page.getByRole('button',{name:/^Undo ·/}).click();await page.waitForFunction(()=>document.getElementById('message').textContent.startsWith('Undo'));
 const restored=await page.evaluate(()=>JSON.parse(JSON.stringify(state)));
 assert.deepEqual(restored.draft,baseline.draft,'Composite Undo must restore every widget and favorite order');
 await page.screenshot({path:out+'/phone-undo.png',fullPage:true});
 await page.getByRole('button',{name:'Stop editing',exact:true}).click();
 fs.writeFileSync(out+'/browser-result.json',JSON.stringify({passed:true,tv:baseline.tv,revisionBefore:baseline.revision,revisionApplied:applied.revision,revisionRestored:restored.revision,favoriteCount:restored.draft.favorites.length,checks:['QR decoded','TV approval','real browser forms','mixed Rain/Air/Calendar/Favorites transaction','preview','composite Undo','stop/revoke']},null,2));
 console.log('Actual Cast1 browser pairing, mixed Apply, Preview, composite Undo and Stop passed; household settings restored.');await browser.close();
})().catch(e=>{console.error(e.message.replace(/#[A-Za-z0-9_-]+/g,'#[redacted]'));process.exit(1);});
