/* Real production phone UI in Chromium/WebKit; controlled API, no device transport. */
const {chromium,webkit,devices}=require('/workspace/artifacts/phone-browser/node_modules/playwright');
const assert=require('node:assert/strict'),http=require('node:http'),fs=require('node:fs');
const fixture=require('./phone-fixture.cjs');
let state,prior,preview;
const root='/workspace/launcher/app/src/main/assets/phone/';
const server=http.createServer(async(req,res)=>{
 res.setHeader('Content-Type','application/json');
 if(req.method==='GET'){
  const file=new URL(req.url,'http://fixture').pathname.slice(1)||'index.html';
  if(!/^(index.html|editor.js|editor.css|model.js|strings.js|drag.js|clock.js|grocery.js|timers.js)$/.test(file)){res.writeHead(404).end();return;}
  res.setHeader('Content-Type',file.endsWith('.html')?'text/html':file.endsWith('.js')?'text/javascript':'text/css');res.end(fs.readFileSync(root+file));return;
 }
 let body='';for await(const part of req)body+=part;const input=JSON.parse(body||'{}');let result;
 switch(req.url){
  case '/api/pair':result={token:'fixture',code:'123456',tv:state.tv};break;
  case '/api/status':result={status:'approved',remaining:800000};break;
  case '/api/state':result=state;break;
  case '/api/preview':preview=structuredClone(input);result={ok:true};break;
  case '/api/cancel-preview':case '/api/touch':case '/api/icon':result={ok:true};break;
  case '/api/apply':assert.equal(input.revision,state.revision);prior=structuredClone(state);state={...state,draft:input.draft,revision:state.revision+1,undo:'Phone changes'};result=state;break;
  case '/api/undo':assert.equal(input.revision,state.revision);state={...prior,revision:state.revision+1};result=state;break;
  default:res.writeHead(404).end('{}');return;
 }res.end(JSON.stringify(result));
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'0.0.0.0',resolve));const url='http://127.0.0.1:'+server.address().port;
 try{
  for(const [engine,profile,name]of [[chromium,devices['Pixel 7'],'chromium'],[webkit,devices['iPhone 13'],'webkit']]){
   state={...structuredClone(fixture),tvId:'scale-browser-proof',instances:{},dashboardScale:50};state.draft.columns=16;state.draft.rows=8;
   const baseline=structuredClone(state.draft);prior=preview=null;
   const browser=await engine.launch();
   try{
    const page=await browser.newPage({...profile});page.setDefaultTimeout(10000);const errors=[];page.on('pageerror',e=>errors.push(e.message));
    const click=name=>page.getByRole('button',{name,exact:true}).click();
    await page.goto(url+'/#fixture');await page.locator('.widget-list .widget-row').first().waitFor();
    await click('Arrange dashboard');await page.locator('.card-label').first().click();await page.getByText('Advanced placement',{exact:true}).first().click();
    for(const [label,value]of [['Width','6'],['Column','10'],['Row','5'],['Height','2']]){
     await page.getByLabel(label,{exact:true}).fill(value);await page.getByLabel(label,{exact:true}).press('Tab');
    }
    await click('Preview on TV');await page.getByRole('button',{name:'Cancel TV preview',exact:true}).waitFor();
    const moved=preview.draft.widgets.find(w=>w.type==='rain');assert.deepEqual([moved.column,moved.row,moved.width,moved.height],[9,4,6,2]);
    assert.deepEqual(state.draft,baseline,'Preview must not save');await click('Cancel TV preview');
    await click('Update '+state.tv);await page.getByText('Updated '+state.tv,{exact:true}).waitFor();
    assert.deepEqual(state.draft.widgets.find(w=>w.type==='rain'),moved);assert.equal(state.draft.columns,16);assert.equal(state.draft.rows,8);assert.equal(state.dashboardScale,50);
    fs.mkdirSync('/workspace/docs/testing/2026-11-26-dashboard-scale',{recursive:true});
    await page.screenshot({path:'/workspace/docs/testing/2026-11-26-dashboard-scale/'+name+'-phone-moved.png',fullPage:true});
    await page.locator('#message').getByRole('button',{name:'Undo update',exact:true}).click();
    await page.waitForFunction(()=>document.querySelector('#message')?.textContent.includes('Undid'));
    assert.deepEqual(state.draft,baseline);assert.equal(state.dashboardScale,50);assert.deepEqual(errors,[]);
    assert(await page.locator('body').evaluate(b=>b.scrollWidth<=innerWidth),'Phone viewport overflow');
    await page.screenshot({path:'/workspace/docs/testing/2026-11-26-dashboard-scale/'+name+'-phone-undone.png',fullPage:true});
    console.log(name+': 16×8 UI move/resize beyond old grid, unsaved preview/cancel, Update and Undo passed');
   }finally{await browser.close();}
  }
 }finally{server.close();}
})().catch(e=>{console.error(e);process.exit(1)});
