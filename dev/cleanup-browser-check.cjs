const {chromium,webkit}=require('/workspace/artifacts/phone-browser/node_modules/playwright');
const {spawn}=require('node:child_process');const fs=require('node:fs');const assert=require('node:assert/strict');
(async()=>{const child=spawn('python3',['/workspace/dev/cleanup_browser_fixture.py']);child.stderr.pipe(process.stderr);try{
const origin=await new Promise((resolve,reject)=>{child.stdout.once('data',d=>resolve(d.toString().trim()));child.once('error',reject);child.once('exit',code=>reject(Error('Fixture exited '+code)));});
fs.mkdirSync('/workspace/artifacts/cleanup-browser',{recursive:true});
for(const [name,type] of Object.entries({chromium,webkit})){
const browser=await type.launch({headless:true});try{const page=await browser.newPage({viewport:{width:1040,height:1200}});await page.goto(origin);await page.locator('#scan').waitFor();await page.waitForFunction(()=>!document.querySelector('#scan').disabled);
assert((await page.locator('#device').textContent()).includes('<b>Family TV</b>'));assert.equal(await page.locator('#device b').count(),0);
await page.locator('#scan').click();await page.waitForFunction(()=>document.querySelector('#result').textContent.includes('Read-only report'));
assert(await page.getByRole('button',{name:'Apply unavailable'}).isDisabled());assert((await page.locator('#result').textContent()).includes('No package, Home assignment or TV setting was changed.'));
await page.screenshot({path:'/workspace/artifacts/cleanup-browser/'+name+'-en.png',fullPage:true});
await page.setViewportSize({width:390,height:844});await page.selectOption('#language','th');
assert.equal(await page.locator('html').getAttribute('lang'),'th');assert((await page.locator('#result').textContent()).includes('2569'));assert(await page.getByRole('button',{name:'ยังใช้การเปลี่ยนแปลงไม่ได้'}).isDisabled());
assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await page.screenshot({path:'/workspace/artifacts/cleanup-browser/'+name+'-th.png',fullPage:true});
const denied=await page.evaluate(async()=>{const token=document.querySelector('meta[name=homeboard-token]').content;return (await fetch('/api/apply',{method:'POST',headers:{'Content-Type':'application/json','X-Homeboard-Token':token},body:'{}'})).status;});assert.equal(denied,404);
console.log(name+': actual HTTP companion, English/Thai, safe text, read-only gate and mobile layout passed');
}finally{await browser.close();}}
}finally{child.kill('SIGTERM');}})().catch(e=>{console.error(e);process.exitCode=1;});
