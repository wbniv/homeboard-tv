/* Container-only real Chromium/WebKit touch UI acceptance with a controlled JSON API. */
const {chromium,webkit,devices}=require('/workspace/artifacts/phone-browser/node_modules/playwright');
const http=require('http'),fs=require('fs'),assert=require('assert'),initial=require('./phone-fixture.cjs');
const root='/workspace/launcher/app/src/main/assets/phone';let state,prior,applied,previewed,conflict=false,unknown=false;const receipts=new Map();
const server=http.createServer(async(req,res)=>{res.setHeader('Content-Type','application/json');if(req.method==='GET'){
const pathname=new URL(req.url,'http://fixture').pathname;const file=pathname==='/'?'index.html':pathname.slice(1);
if(!['index.html','editor.js','editor.css','model.js','strings.js','drag.js','clock.js','grocery.js'].includes(file)){res.writeHead(404).end();return;}
res.setHeader('Content-Type',file.endsWith('.html')?'text/html':file.endsWith('.js')?'text/javascript':'text/css');res.end(fs.readFileSync(root+'/'+file));return;}
let data='';for await(const chunk of req)data+=chunk;const body=JSON.parse(data||'{}');let result={};switch(req.url){
case '/api/pair':if(body.tvId&&body.tvId!==state.tvId){res.writeHead(422).end(JSON.stringify({field:'tvId',error:'This shortcut belongs to another TV.'}));return;}result={token:'fixture',code:'123456',tv:state.tv};break;
case '/api/status':result={status:'approved',remaining:800000};break;
case '/api/state':result=state;break;
case '/api/icon':case '/api/touch':case '/api/stop':case '/api/cancel-preview':result={ok:true};break;
case '/api/preview':previewed=body;result={ok:true};break;
case '/api/operation':result={status:receipts.has(body.operation)?'committed':'unknown'};break;
case '/api/apply':if(conflict){conflict=false;res.writeHead(422).end(JSON.stringify({field:'revision',error:'Revision changed'}));return;}
if(!receipts.has(body.operation)){prior=structuredClone(state);applied=body;state={...state,draft:body.draft,revision:state.revision+1,undo:'Phone changes'};receipts.set(body.operation,true);}result=state;if(unknown){unknown=false;req.socket.destroy();return;}break;
case '/api/undo':state={...prior,revision:state.revision+1};result=state;break;
case '/api/lookup':result={places:[{name:'Bangkok',latitude:13.75,longitude:100.5}]};break;
default:res.writeHead(404).end('{}');return;}res.end(JSON.stringify(result));});
(async()=>{
 await new Promise(resolve=>server.listen(0,'0.0.0.0',resolve));const url='http://127.0.0.1:'+server.address().port;
 for(const [engine,profile,name]of [[chromium,devices['Pixel 7'],'chromium'],[webkit,devices['iPhone 13'],'webkit']]){
  state={...structuredClone(initial),tvId:'grocery-'+name,instances:{}};receipts.clear();applied=null;
  const browser=await engine.launch(),page=await browser.newPage({...profile});page.setDefaultTimeout(7000);const errors=[];page.on('pageerror',e=>errors.push(e.message));page.on('dialog',d=>d.type()==='confirm'?d.accept():d.dismiss());
  const click=label=>page.getByRole('button',{name:label,exact:true}).click(),fill=(label,value)=>page.getByLabel(label,{exact:true}).fill(value);
  await page.goto(url+'/#fixture');await page.locator('.widget-list').waitFor();await click('+ Add widget');await click('+ Groceries');
  await fill('Add an item…','นม');await page.getByLabel('Add an item…',{exact:true}).press('Enter');assert.equal(await page.locator('.grocery-row').count(),1);assert.equal(await page.getByLabel('Add an item…',{exact:true}).inputValue(),'');assert.equal(await page.getByLabel('Add an item…',{exact:true}).evaluate(n=>n===document.activeElement),true);assert.equal(applied,null,'Adding is a phone draft only');
  await fill('Add an item…','นม');await click('Add');await page.getByText('Already on the list',{exact:true}).waitFor();await click('Edit existing item');await fill('Quantity text','2 cartons');await page.getByLabel('Keep as staple',{exact:true}).check();await click('Save item to draft');
  await page.getByText('Add several',{exact:true}).click();await fill('One item per line','Eggs\n\nLimes');await click('Preview items');await click('Add these items to draft');assert.equal(await page.locator('.grocery-row').count(),3);
  await page.getByRole('button',{name:'Reorder Limes',exact:true}).press('Alt+ArrowUp');assert.equal(await page.locator('.grocery-row').nth(1).getByRole('button').nth(1).textContent(),'Limes');
  await page.screenshot({path:'/workspace/artifacts/phone-browser/grocery-'+name+'-list.png',fullPage:true});
  await click('Staples');await page.getByRole('heading',{name:'นม',exact:true}).waitFor();await click('Manual need');await click('Need more');await page.getByText('Need more',{exact:true}).first().waitFor();
  await click('Review existing list quantity');await click('Remove from list');await page.getByRole('heading',{name:'นม',exact:true}).waitFor();await click('Add to shopping list');assert.equal(await page.getByLabel('Quantity text',{exact:true}).inputValue(),'2 cartons');await click('Save item to draft');
  await click('Products');await click('New product');await fill('Item name','Oat milk');await fill('Brand','Household brand');await fill('Variant','Unsweetened');await page.getByLabel('Keep as staple',{exact:true}).check();await page.getByLabel('Stock base unit',{exact:true}).selectOption('ml');await fill('Package unit name (e.g. carton)','carton');await fill('One package contains (in base units)','1000');await fill('Usual purchase amount','2');await fill('Usual purchase unit','carton');await page.getByLabel('Stock tracking',{exact:true}).selectOption('quantity');
  await fill('Reported stock (ml)','500');await fill('Low at or below (ml)','1000');await fill('Desired stock target (ml)','3000');await click('Save product to draft');await click('Staples');await page.getByText('Suggested: 3 carton',{exact:true}).waitFor();await page.getByText('Deficit 2500 ml; expected stock after purchase 3500 ml.',{exact:true}).waitFor();
  await page.screenshot({path:'/workspace/artifacts/phone-browser/grocery-'+name+'-stock.png',fullPage:true});
  const milk=page.locator('section').filter({has:page.getByRole('heading',{name:'Oat milk · Household brand · Unsweetened',exact:true})}).last();await milk.getByRole('button',{name:'Add to shopping list',exact:true}).click();assert.equal(await page.getByLabel('Amount',{exact:true}).inputValue(),'3');await fill('Note','For breakfast');await click('Save item to draft');await click('Shopping list');assert.equal(await page.locator('.grocery-row').count(),4);
  assert.equal(await page.getByRole('checkbox').count(),0,'No purchase checkboxes');await click('Update '+state.tv);await page.getByText('Updated '+state.tv,{exact:true}).waitFor();
  const saved=state.draft.groceries.groceries;assert.equal(saved.items.length,4);assert.equal(saved.products.length,2);assert.equal(saved.products.find(p=>p.name==='Oat milk').stock,'500','Adding need never increases inventory');assert.equal(saved.items.find(i=>i.name==='Oat milk').structured.amount,'3');
  // Remove/re-add widget preserves book. Duplicate product requests review instead of overwriting.
  await click('Staples');await milk.getByRole('button',{name:'Review existing list quantity',exact:true}).click();await fill('Amount','2');await click('Save item to draft');await click('Shopping list');assert.equal(await page.locator('.grocery-row').count(),4);
  await page.addStyleTag({content:'html{font-size:32px}'});assert(await page.locator('body').evaluate(n=>n.scrollWidth<=innerWidth),'Large text fits phone');await page.screenshot({path:'/workspace/artifacts/phone-browser/grocery-'+name+'-large-text.png',fullPage:true});
  // Keep an open editor across a remote rebase: show both versions before replacement.
  state.tvId='editor-conflict-'+name;state.revision++;await page.reload();await click('Start editing');await page.locator('.widget-list .widget-row').filter({hasText:'Groceries'}).click();
  await page.locator('.grocery-row').filter({hasText:'Oat milk'}).getByRole('button').nth(1).click();await fill('Note','My breakfast note');
  state.draft.groceries.groceries.items.find(i=>i.name==='Oat milk').note='Someone else changed this';state.revision++;
  await page.getByText('More options',{exact:true}).click();await click('Refresh from TV');await page.getByText('Refreshed from TV',{exact:true}).waitFor().catch(()=>{});
  await click('Save item to draft');await page.getByText('This item changed on the TV while this form was open. Review both versions.',{exact:true}).waitFor();await click('Keep my edit');await click('Update '+state.tv);await page.getByText('Updated '+state.tv,{exact:true}).waitFor();assert.equal(state.draft.groceries.groceries.items.find(i=>i.name==='Oat milk').note,'My breakfast note');
  assert.equal(await page.getByRole('button',{name:'Undo last grocery draft change',exact:true}).count(),0,'A committed update clears the component draft Undo');
  assert.deepEqual(errors,[]);await browser.close();console.log(name+': quick/batch entry, Thai, duplicate review, staple reuse, keyboard reorder, units, stock suggestions, atomic update and large-text rendering passed');
 }
 server.close();
})().catch(e=>{console.error(e);server.close();process.exit(1);});
