/* Optional LAN browser acceptance during a complete coordinator-owned session.
 * The coordinator must open Configure from phone and approve the matching code.
 * No direct TV control. This script needs an actual reachable TV HTTP address.
 */
const {chromium,devices}=require('/workspace/artifacts/phone-browser/node_modules/playwright');
const {PNG}=require('/workspace/artifacts/phone-browser/node_modules/pngjs');
const jsQR=require('/workspace/artifacts/phone-browser/node_modules/jsqr'),fs=require('fs'),assert=require('assert');
const image=process.argv[2],out=process.argv[3];
(async()=>{
 const png=PNG.sync.read(fs.readFileSync(image)),code=jsQR(new Uint8ClampedArray(png.data),png.width,png.height);
 assert(code&&code.data.startsWith('http://'),'TV QR code must decode');
 const browser=await chromium.launch({headless:true}),page=await browser.newPage({...devices['Pixel 7']});page.setDefaultTimeout(60000);
 const stateResponse=page.waitForResponse(r=>r.url().endsWith('/api/state')&&r.ok());
 await page.goto(code.data,{waitUntil:'domcontentloaded',timeout:20000});console.log('TV editor loaded; waiting for coordinator-owned TV approval.');
 const baseline=await(await stateResponse).json();await page.locator('.widget-row').first().waitFor();
 const rainIndex=baseline.draft.widgets.findIndex(w=>w.type==='rain');assert(rainIndex>=0);
 await page.locator('.widget-row').nth(rainIndex).click();await page.getByText('More options',{exact:true}).first().click();await page.getByLabel('Label',{exact:true}).fill('Phone editor verification');await page.getByRole('button',{name:'‹ Back',exact:true}).click();
 if(baseline.draft.favorites.length>1){await page.locator('.widget-row').nth(baseline.draft.widgets.findIndex(w=>w.type==='favorites')).click();await page.locator('[data-reorder]').first().focus();await page.keyboard.press('Alt+ArrowDown');await page.getByRole('button',{name:'‹ Back',exact:true}).click();}
 const resultResponse=page.waitForResponse(r=>r.url().endsWith('/api/apply')&&r.ok());await page.getByRole('button',{name:'Update '+baseline.tv,exact:true}).click();const applied=await(await resultResponse).json();assert.equal(applied.draft.widgets.find(w=>w.type==='rain').settings.label,'Phone editor verification');await page.screenshot({path:out+'/phone-applied.png',fullPage:true});
 const undoResponse=page.waitForResponse(r=>r.url().endsWith('/api/undo')&&r.ok());await page.locator('#message').getByRole('button',{name:'Undo update',exact:true}).click();const restored=await(await undoResponse).json();assert.deepEqual(restored.draft,baseline.draft,'Composite Undo must restore household settings');await page.screenshot({path:out+'/phone-undo.png',fullPage:true});
 await page.locator('#target summary').click();await page.getByRole('button',{name:'Done editing',exact:true}).click();
 fs.writeFileSync(out+'/browser-result.json',JSON.stringify({passed:true,tv:baseline.tv,revisionBefore:baseline.revision,revisionApplied:applied.revision,revisionRestored:restored.revision,checks:['TV QR','TV approval','browser Rain edit','Favorites reorder','atomic Update','composite Undo','stop']},null,2));
 await browser.close();console.log('Actual LAN browser update/Undo passed; household settings restored.');
})().catch(e=>{console.error(e.message.replace(/#[A-Za-z0-9_-]+/g,'#[redacted]'));process.exit(1);});
