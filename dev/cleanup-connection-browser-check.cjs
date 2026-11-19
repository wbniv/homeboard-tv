const {chromium,webkit}=require('/workspace/artifacts/phone-browser/node_modules/playwright');
const {spawn}=require('node:child_process');const fs=require('node:fs');const assert=require('node:assert/strict');
(async()=>{const child=spawn('python3',['/workspace/dev/cleanup_browser_fixture.py'],{env:{...process.env,STANDALONE_FIXTURE:'1'}});try{
const origin=await new Promise((resolve,reject)=>{child.stdout.once('data',d=>resolve(d.toString().trim()));child.once('error',reject)});
fs.mkdirSync('/workspace/docs/testing/2026-11-19-cleanup-connections',{recursive:true});
for(const [name,type] of Object.entries({chromium,webkit})){
const browser=await type.launch();try{const page=await browser.newPage({viewport:{width:1000,height:1100}});await page.goto(origin);await page.locator('#connection').waitFor({state:'visible'});
await page.locator('#pair-address').fill('192.168.1.9:22222');await page.locator('#pair-code').fill('123456');await page.locator('#pair-button').click();await page.waitForFunction(()=>document.querySelector('#connection-message').textContent.startsWith('Paired'));
assert.equal(await page.locator('#pair-code').inputValue(),'');await page.locator('#connect-address').fill('192.168.1.9:33333');await page.locator('#connect-button').click();await page.waitForFunction(()=>document.querySelector('#identity').textContent.includes('FIXTURE123'));
assert(await page.locator('#scan').isDisabled());await page.locator('#confirm').check();assert(await page.locator('#scan').isEnabled());
await page.screenshot({path:'/workspace/docs/testing/2026-11-19-cleanup-connections/'+name+'-en.png',fullPage:true});
await page.locator('#scan').click();await page.waitForFunction(()=>document.querySelector('#result').textContent.includes('Read-only report'));assert(await page.getByRole('button',{name:'Apply unavailable'}).isDisabled());
await page.setViewportSize({width:390,height:844});await page.selectOption('#language','th');assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
await page.screenshot({path:'/workspace/docs/testing/2026-11-19-cleanup-connections/'+name+'-th.png',fullPage:true});await page.locator('#disconnect').click();await page.waitForFunction(()=>document.querySelector('#identity').textContent==='');assert(await page.locator('#scan').isDisabled());
console.log(name+': pairing form cleared, identity confirmation, inspection, Thai mobile layout and disconnect passed');
}finally{await browser.close()}}
}finally{child.kill('SIGTERM')}})().catch(e=>{console.error(e);process.exitCode=1});
