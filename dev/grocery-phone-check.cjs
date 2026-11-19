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
  const browser=await engine.launch(),page=await browser.newPage({...profile,viewport:{width:390,height:844}});page.setDefaultTimeout(7000);const errors=[];page.on('pageerror',e=>errors.push(e.message));page.on('dialog',d=>d.type()==='confirm'?d.accept():d.dismiss());
  const click=label=>page.getByRole('button',{name:label,exact:true}).click(),fill=(label,value)=>page.getByLabel(label,{exact:true}).fill(value);
  await page.goto(url+'/#fixture');await page.locator('.widget-list').waitFor();await click('+ Add widget');await click('+ Groceries');
  const quick=page.getByLabel('Add an item…',{exact:true});
  assert((await quick.boundingBox()).y<600,'Capture visible without scrolling');
  assert.equal(await page.getByLabel('Choose list',{exact:true}).count(),0,'One list does not need a selector');
  for(const name of ['นม','Eggs','Limes']){await quick.fill(name);await quick.press('Enter');assert.equal(await quick.evaluate(n=>n===document.activeElement),true);}
  assert.equal(await page.locator('.grocery-row').count(),3);assert.equal(applied,null,'Adding is a phone draft only');
  await quick.fill('นม');await click('Add');await page.getByText('Already on the list',{exact:true}).waitFor();await click('Edit existing item');
  await fill('How much?','2 cartons');await page.getByLabel('Keep for next time',{exact:true}).check();await click('Done');
  await page.getByRole('button',{name:'Reorder Limes',exact:true}).press('Alt+ArrowUp');assert.equal(await page.locator('.grocery-row').nth(1).getByRole('button').nth(1).textContent(),'Limes');
  const order=await page.locator('.grocery-row').evaluateAll(rows=>rows.map(r=>r.dataset.orderKey));const grip=page.locator('[data-reorder]').first();await grip.scrollIntoViewIfNeeded();const rect=await grip.boundingBox();await page.mouse.move(rect.x+10,rect.y+10);await page.mouse.down();await page.mouse.move(rect.x+12,rect.y+45);await grip.dispatchEvent('pointercancel',{pointerId:1});await page.mouse.up();assert.deepEqual(await page.locator('.grocery-row').evaluateAll(rows=>rows.map(r=>r.dataset.orderKey)),order,'Cancelled reorder is lossless');
  await page.getByText('Add several',{exact:true}).click();await fill('One item per line','Coriander');await click('Preview items');await click('Add these items to draft');assert.equal(await page.locator('.grocery-row').count(),4);await page.locator('.grocery-row').filter({hasText:'Coriander'}).getByRole('button').nth(1).click();await click('Remove from list');
  await page.screenshot({path:'/workspace/artifacts/phone-browser/grocery-'+name+'-list.png',fullPage:true});
  await click('Check staples');await page.getByRole('heading',{name:'นม',exact:true}).waitFor();await click('Need more');
  await page.getByLabel('Show staples',{exact:true}).selectOption('need');assert.equal(await page.getByRole('heading',{name:'นม',exact:true}).count(),1);
  await page.getByLabel('Show staples',{exact:true}).selectOption('unknown');await page.getByText('No staples in this view. Choose All staples to see the rest.',{exact:true}).waitFor();
  await page.getByLabel('Show staples',{exact:true}).selectOption('all');await click('Already on list · Change quantity');await click('Remove from list');await click('Add to shopping list');
  assert.equal(await page.getByRole('heading',{name:'Item details',exact:true}).count(),0,'Reuse adds directly');
  await click('Enough');await click('‹ Shopping list');assert.equal(await page.locator('.grocery-row').count(),3,'Enough retains the existing need');
  await quick.fill('นม');await page.locator('.grocery-entry').getByRole('button',{name:'นม · 2 cartons',exact:true}).click();await page.getByText('Already on list · Tap the item to change its quantity.',{exact:true}).waitFor();assert.equal(await page.locator('.grocery-row').count(),3,'Repeated reuse never increases amount');
  // An ordinary edit survives Back without a separate save action.
  await page.locator('.grocery-row').filter({hasText:'Eggs'}).getByRole('button').nth(1).click();await fill('Note','For breakfast');await click('‹ Shopping list');
  await page.locator('.grocery-row').filter({hasText:'Eggs'}).getByRole('button').nth(1).click();assert.equal(await page.getByLabel('Note',{exact:true}).inputValue(),'For breakfast');
  await fill('Note','Cancel only this edit');await click('Cancel');
  await page.locator('.grocery-row').filter({hasText:'Eggs'}).getByRole('button').nth(1).click();assert.equal(await page.getByLabel('Note',{exact:true}).inputValue(),'For breakfast');
  // Incomplete ordinary fields survive refresh and explicit recovery, and block publication.
  await fill('Item name','');await fill('Note','Unfinished recovered note');assert.equal(await page.getByRole('button',{name:'Update '+state.tv,exact:true}).isDisabled(),true);
  await page.reload();await click('Start editing');await page.locator('.widget-list').waitFor();await page.getByText('More options',{exact:true}).click();await click('Restore kept draft for review');
  assert.equal(await page.getByLabel('Note',{exact:true}).inputValue(),'Unfinished recovered note');assert.equal(await page.getByLabel('Item name',{exact:true}).inputValue(),'');
  await fill('Item name','Eggs');await click('‹ Shopping list');
  // Management is reachable from the named list title, after capture.
  await page.locator('.grocery-title-menu > summary').click();await click('Products');await click('New product');await fill('Item name','Oat milk');
  await page.getByLabel('Keep for next time',{exact:true}).check();
  await page.getByText('Units and packaging',{exact:true}).click();await page.getByLabel('Measure in',{exact:true}).first().selectOption('l');
  await fill('Package unit name (e.g. carton)','carton');await fill('One package contains (L)','1');await fill('Usual purchase amount','2');await fill('Usual purchase unit','carton');
  await page.getByText('Track stock',{exact:true}).click();await page.getByLabel('Stock tracking',{exact:true}).selectOption('quantity');
  await fill('Reported stock (L)','0.5');await fill('Low at or below (L)','1');await fill('Desired stock target (L)','3');await click('Done');
  await click('‹ Shopping list');await click('Check staples');await page.getByText('Suggested: 3 carton',{exact:true}).waitFor();await page.getByText('Low at 1 L · Target 3 L · Would leave 3.5 L after purchase.',{exact:true}).waitFor();
  await page.getByText('1 carton contains 1 L. Round up to whole packages.',{exact:true}).waitFor();
  await page.screenshot({path:'/workspace/artifacts/phone-browser/grocery-'+name+'-stock.png',fullPage:true});
  const milk=()=>page.locator('section').filter({has:page.getByRole('heading',{name:'Oat milk',exact:true})}).last();await milk().getByRole('button',{name:'Add 3 carton',exact:true}).click();
  await milk().getByRole('button',{name:'Already on list · Change quantity',exact:true}).click();assert.equal(await page.getByLabel('Amount',{exact:true}).inputValue(),'3');
  await fill('Unit','kg');await page.getByText('Units are unknown or incompatible. Confirm the product’s base unit and package size, or keep free text.',{exact:true}).first().waitFor();assert.equal(await page.getByRole('button',{name:'Update '+state.tv,exact:true}).isDisabled(),true);assert.equal(await page.getByLabel('Amount',{exact:true}).inputValue(),'3');
  await fill('Unit','carton');await click('‹ Shopping list');await click('‹ Shopping list');
  assert.equal(await page.locator('.grocery-row').count(),4);assert.equal(await page.getByRole('checkbox').count(),0,'No purchase checkboxes');
  // Mix another widget change with groceries; review names both outcomes.
  await click('‹ Back');await page.locator('.widget-list .widget-row').filter({hasText:'Rain'}).first().click();await page.getByRole('button',{name:'6',exact:true}).click();
  await page.getByText('Review changes',{exact:true}).click();await page.getByText(/Add Oat milk · 3 carton/).waitFor();await page.getByText(/Next 6 hours/).first().waitFor();
  unknown=true;await click('Update '+state.tv);await page.getByText('Updated '+state.tv,{exact:true}).waitFor();
  const saved=state.draft.groceries.groceries;assert.equal(saved.items.length,4);assert.equal(saved.products.length,2);assert.equal(saved.products.find(p=>p.name==='Oat milk').stock,'500','Adding need never increases inventory');assert.equal(saved.items.find(i=>i.name==='Oat milk').structured.amount,'3');
  await click('Undo update');await page.getByText('Undid the last update on '+state.tv,{exact:true}).waitFor();assert.equal(state.draft.widgets.filter(w=>w.type==='grocery').length,0,'One Undo covers groceries and rain');
  // Reapply fixture for open-editor concurrent changes.
  state.draft=structuredClone(applied.draft);state.revision++;state.tvId='editor-conflict-'+name;
  await page.reload();await click('Start editing');await page.locator('.widget-list .widget-row').filter({hasText:'Groceries'}).click();
  await page.locator('.grocery-row').filter({hasText:'Oat milk'}).getByRole('button').nth(1).click();await fill('Note','My breakfast note');
  state.draft.groceries.groceries.items.find(i=>i.name==='Oat milk').note='Someone else changed this';state.revision++;
  await page.getByText('More options',{exact:true}).click();await click('Refresh from TV');await page.getByText('Latest TV settings loaded.',{exact:true}).waitFor();
  await click('Done');await page.getByRole('heading',{name:'This item changed while you were editing. Review both versions before continuing.',exact:true}).waitFor();await page.getByText(/Someone else changed this/).waitFor();await page.getByText(/My breakfast note/).waitFor();await click('Keep my edit');
  await click('Update '+state.tv);await page.getByText('Updated '+state.tv,{exact:true}).waitFor();assert.equal(state.draft.groceries.groceries.items.find(i=>i.name==='Oat milk').note,'My breakfast note');
  assert.equal(await page.getByRole('button',{name:'Undo last grocery draft change',exact:true}).count(),0,'Acknowledgement clears draft Undo');
  await page.addStyleTag({content:'html{font-size:32px}'});assert(await page.locator('body').evaluate(n=>n.scrollWidth<=innerWidth),'Large text fits phone');await page.screenshot({path:'/workspace/artifacts/phone-browser/grocery-'+name+'-large-text.png',fullPage:true});
  // Thai copy, narrow viewport and retained focus use the same workflow.
  state.language='th';state.tvId='thai-'+name;await page.reload();await click('Start editing');await page.locator('.widget-list .widget-row').filter({hasText:'ของชำ'}).click();
  await fill('เพิ่มสินค้า…','ข้าว');await page.getByLabel('เพิ่มสินค้า…',{exact:true}).press('Enter');await page.screenshot({path:'/workspace/artifacts/phone-browser/grocery-'+name+'-thai.png',fullPage:true});
  state.language='en';state.tvId='unknown-units-'+name;const tea=await page.evaluate(()=>window.PhoneGrocery.product('tea','Tea'));state.draft.groceries.groceries.products.push({...tea,usualAmount:'1',usualUnit:'packet'});
  await page.reload();await click('Start editing');await page.locator('.widget-list .widget-row').filter({hasText:'Groceries'}).click();await fill('Add an item…','Tea');await page.locator('.grocery-entry').getByRole('button',{name:'Tea · 1 packet',exact:true}).click();
  await click('Update '+state.tv);await page.getByText('Updated '+state.tv,{exact:true}).waitFor();const unknownTea=state.draft.groceries.groceries.items.find(i=>i.name==='Tea');assert.equal(unknownTea.quantity,'1 packet');assert.equal(unknownTea.structured,undefined,'Unconfirmed units remain text');
  // Correct a package in familiar units; preview the meaning of existing needs.
  await click('Check staples');const stockCard=()=>page.locator('section').filter({has:page.getByRole('heading',{name:'Oat milk',exact:true})}).last();await stockCard().getByRole('button',{name:'Edit product / stock',exact:true}).click();
  if(!await page.getByLabel('One package contains (L)',{exact:true}).isVisible())await page.getByText('Units and packaging',{exact:true}).click();
  await fill('One package contains (L)','0.75');await page.getByText('1 carton = 1 L → 1 carton = 0.75 L',{exact:true}).waitFor();await page.getByText('Oat milk · 3 carton → 2.25 L',{exact:true}).waitFor();await click('Done');
  await stockCard().getByRole('button',{name:'Change 3 carton → 4 carton',exact:true}).click();await page.getByText('On list: 4 carton',{exact:true}).waitFor();await click('Update '+state.tv);await page.getByText('Updated '+state.tv,{exact:true}).waitFor();assert.equal(state.draft.groceries.groceries.products.find(p=>p.name==='Oat milk').packageSize,'750');assert.equal(state.draft.groceries.groceries.items.find(i=>i.name==='Oat milk').structured.amount,'4');assert.equal(state.draft.groceries.groceries.products.find(p=>p.name==='Oat milk').stock,'500');await click('‹ Shopping list');
  // An external widget removal cannot strand an incomplete form or silently resurrect the widget.
  await page.locator('.grocery-row').filter({hasText:'Tea'}).getByRole('button').nth(1).click();await fill('Item name','');await fill('Note','Keep this after widget removal');
  state.draft.widgets=state.draft.widgets.filter(w=>w.type!=='grocery');state.revision++;await page.getByText('More options',{exact:true}).click();await click('Refresh from TV');
  await page.getByRole('heading',{name:/Unfinished grocery edit:/}).waitFor();assert.equal(await page.getByLabel('Note',{exact:true}).inputValue(),'Keep this after widget removal');await fill('Item name','Tea');await click('Done');
  await click('Update '+state.tv);await page.getByText('Updated '+state.tv,{exact:true}).waitFor();assert(!state.draft.widgets.some(w=>w.type==='grocery'));assert.equal(state.draft.groceries.groceries.items.find(i=>i.name==='Tea').note,'Keep this after widget removal');
  // A removed list needs an explicit recovery decision and must not rebind the TV widget.
  state.tvId='removed-list-'+name;state.revision++;state.draft.widgets.push({key:'widget:grocery:recovery',type:'grocery',settings:{listId:'groceries'},page:3,column:0,row:0,width:4,height:2});
  await page.reload();await click('Start editing');await page.locator('.widget-list .widget-row').filter({hasText:'Groceries'}).click();await page.locator('.grocery-row').filter({hasText:'Tea'}).getByRole('button').nth(1).click();await fill('Item name','');await fill('Note','Keep after list deletion');
  delete state.draft.groceries.groceries;state.draft.groceries.other={id:'other',title:'Other',items:[],products:[]};state.draft.widgets.find(w=>w.type==='grocery').settings.listId='other';state.revision++;
  await page.getByText('More options',{exact:true}).click();await click('Refresh from TV');await click('Restore list in my draft to keep this edit');await fill('Item name','Tea');assert.equal(await page.getByLabel('How much?',{exact:true}).inputValue(),'1 packet');await click('Done');
  await click('Update '+state.tv);await page.getByText('Updated '+state.tv,{exact:true}).waitFor();assert.equal(state.draft.widgets.find(w=>w.type==='grocery').settings.listId,'other');assert.equal(state.draft.groceries.groceries.items[0].note,'Keep after list deletion');assert.equal(state.draft.groceries.groceries.items[0].productId,'','Missing product link becomes readable text only after explicit recovery');
  assert.deepEqual(errors,[]);await browser.close();console.log(name+': capture/focus, direct reuse, filters, recoverable incomplete edits, inline units, familiar stock, conflicts, mixed publication/retry/Undo, Thai and large text passed');
 }
 server.close();
})().catch(e=>{console.error(e);server.close();process.exit(1);});
