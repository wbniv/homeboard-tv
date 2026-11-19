const {chromium,webkit,devices}=require('/workspace/artifacts/phone-browser/node_modules/playwright');
const http=require('http'),fs=require('fs'),assert=require('assert'),initial=require('./phone-fixture.cjs');
const root='/workspace/launcher/app/src/main/assets/phone';let state,prior;
const widget={key:'widget:android:12345678-1234-1234-1234-123456789abc',type:'android',page:0,column:0,row:0,width:3,height:2,settings:{},presentation:{label:'Provider Agenda',minWidth:2,minHeight:2,horizontal:true,vertical:false,width:3,height:2,columns:8,rows:4,available:true}};
const server=http.createServer(async(req,res)=>{
 if(req.method==='GET'){const file=req.url==='/'?'index.html':req.url.slice(1).split('?')[0];if(!/^[a-z.-]+$/.test(file)||!fs.existsSync(root+'/'+file)){res.writeHead(404).end();return;}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.html')?'text/html':'text/css');res.end(fs.readFileSync(root+'/'+file));return;}
 let body='';for await(const part of req)body+=part;const data=JSON.parse(body||'{}');let value={ok:true};
 if(req.url==='/api/pair')value={token:'fixture',code:'123456',tv:state.tv};
 else if(req.url==='/api/status')value={status:'approved',remaining:800000};
 else if(req.url==='/api/state')value=state;
 else if(req.url==='/api/apply'){prior=structuredClone(state);state={...state,draft:data.draft,revision:state.revision+1,undo:'Phone changes'};value=state;}
 else if(req.url==='/api/undo'){state={...prior,revision:state.revision+1};value=state;}
 res.setHeader('Content-Type','application/json');res.end(JSON.stringify(value));
});
(async()=>{await new Promise(r=>server.listen(0,'0.0.0.0',r));try{
 for(const [engine,profile,name]of [[chromium,devices['Pixel 7'],'Chromium'],[webkit,devices['iPhone 13'],'WebKit']]){
  state=structuredClone(initial);state.tvId="android-widget-test-tv";state.draft.widgets=[structuredClone(widget)];state.draft.columns=8;state.draft.rows=4;state.instances={[widget.key]:1};
  const browser=await engine.launch(),page=await browser.newPage({...profile}),errors=[];page.setDefaultTimeout(7000);page.on('pageerror',e=>errors.push(e.message));
  try{await page.goto('http://127.0.0.1:'+server.address().port+'/#fixture');await page.locator('.widget-list .widget-row').filter({hasText:'Provider Agenda'}).click();
   await page.getByText(/Add and configure Android widgets on the TV\/device/).waitFor();
   await page.getByRole('button',{name:'Arrange dashboard',exact:true}).click();
   await page.locator('summary').filter({hasText:'Advanced'}).first().click();
   assert(await page.getByLabel('Height',{exact:true}).isDisabled());assert.equal(await page.getByLabel('Width',{exact:true}).getAttribute('min'),'2');
   await page.getByLabel('Width',{exact:true}).fill('2');await page.getByRole('button',{name:'Update '+state.tv,exact:true}).click();
   await page.waitForFunction(()=>document.getElementById('message').textContent.includes('Updated'));assert.equal(state.draft.widgets[0].width,2);assert.equal(state.draft.widgets[0].height,2);assert.deepEqual(errors,[]);
   await page.screenshot({path:'/workspace/artifacts/android-widgets/phone-'+name.toLowerCase()+'.png',fullPage:true});
   console.log(name+': real editor label, host setup guidance, disabled resize axis, minimum span and Update TV passed');
  }catch(e){console.error(await page.locator('body').innerText());throw e;}finally{await browser.close();}
 }
}finally{await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
