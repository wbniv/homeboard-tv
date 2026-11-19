'use strict';
(() => {
  const M=window.PhoneModel,$=id=>document.getElementById(id);
  let language=navigator.language.startsWith('th')?'th':'en',state=null,draft=null,token='',invitation=location.hash.slice(1),connected=false;
  let route={name:'overview'},working=false,pending=null,conflicts=[],pollTimer=0,lastTouch=0,lookups=0,sessionEnded=false,previewing=false;
  let reorderUndo=null,restoreRecord=null,disposers=[],expiry=0,iconCache=new Map();
  const scrolls=new Map(),queries=new Map();
  let shortcutTV=new URLSearchParams(location.search).get('tv')||'',shortcutLabel=new URLSearchParams(location.search).get('name')||'';
  const entryURL=()=>location.pathname+location.search;
  history.replaceState({route},'',entryURL());
  const t=(key,values={})=>(PhoneStrings[language][key]??PhoneStrings.en[key]??key).replace(/\{(\w+)\}/g,(_,name)=>String(values[name]??''));
  function el(tag,text,cls){const node=document.createElement(tag);if(text!==undefined)node.textContent=text;if(cls)node.className=cls;return node;}
  function button(label,action,cls){const node=el('button',label,cls);node.type='button';node.onclick=()=>Promise.resolve().then(action).catch(showError);return node;}
  function note(parent,text){parent.append(el('p',text,'muted'));}
  function section(parent,title){const node=el('section');if(title)node.append(el('h2',title));parent.append(node);return node;}
  function details(parent,label,open=false){const node=el('details');node.open=open;node.append(el('summary',label));parent.append(node);return node;}
  function announce(text){$('announcement').textContent='';requestAnimationFrame(()=>$('announcement').textContent=text);}
  function message(text,kind='info',action){const region=$('message');region.replaceChildren();region.className=kind;if(text)region.append(el('p',text));if(action)region.append(button(action.label,action.run));}
  function uid(){const data=new Uint8Array(24);crypto.getRandomValues(data);return [...data].map(x=>x.toString(16).padStart(2,'0')).join('');}
  async function api(path,body={}){
    const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),15000);
    try{const r=await fetch('/api/'+path,{method:'POST',headers:{'Content-Type':'application/json','X-Homeboard-Session':token},body:JSON.stringify(body),signal:controller.signal});
      const result=await r.json();if(!r.ok){const e=new Error(result.error||'Request failed');e.field=result.field;e.status=r.status;throw e;}return result;
    }finally{clearTimeout(timeout);}
  }
  function showError(error){
    if(error.status===403){disconnect('expired');return;}
    if(error.field==='tvId'){message(t('shortcutWrongTV'),'warning');return;}
    if(error.field==='busy'){message(t('busy',{tv:tvName()}),'warning');return;}
    if(error.message==='noRoom'){message(t('noRoom'),'warning');return;}
    message(error.status===422?t('invalid'):t('failed',{tv:tvName()}),'warning');
    const more=details($('message'),t('errorHelp'));more.append(el('p',error.message));
  }
  const tvName=()=>state?.tv||t('yourTV');
  const recordKey=()=>state?'homeboard-controller:'+state.tvId:null;
  function readRecord(){try{return JSON.parse(sessionStorage.getItem(recordKey())||'null');}catch(_){return null;}}
  function persist(){if(!state)return;try{
    const record={tvId:state.tvId,base:state,draft,route,pending,groceryEditors:PhoneGrocery.snapshot(state.tvId)};
    if(M.diff(state.draft,payload()).length||pending||record.groceryEditors.some(([,s])=>s.edit?.touched||s.quick||s.batch||s.newName))sessionStorage.setItem(recordKey(),JSON.stringify(record));else sessionStorage.removeItem(recordKey());
  }catch(_){/* Session storage is optional; edits remain in this tab's memory. */}}
  function touch(){if(connected&&Date.now()-lastTouch>60000){lastTouch=Date.now();api('touch').catch(()=>{});}}
  function changed(){persist();touch();updateActions();updateReview();}
  function routeKey(value=route){return value.name+':'+(value.key||'')+':'+(value.page||0);}
  function navigate(next,replace=false){if(state&&route.name==='widget'&&next.key!==route.key)PhoneGrocery.leave(state,draft);scrolls.set(routeKey(),window.scrollY);route=next;history[replace?'replaceState':'pushState']({route},'',entryURL());render();window.scrollTo(0,scrolls.get(routeKey())||0);persist();}
  window.addEventListener('popstate',event=>{if(state)PhoneGrocery.leave(state,draft);scrolls.set(routeKey(),window.scrollY);route=event.state?.route||{name:'overview'};render();window.scrollTo(0,scrolls.get(routeKey())||0);});
  function back(){const w=currentWidget();if(route.name==='widget'&&w?.type==='grocery'&&PhoneGrocery.back(state,draft,w)){changed();render();return;}if(route.name==='widget'||route.name==='arrange'||route.name==='add'||route.name==='recommended')navigate({name:'overview'});else if(route.name==='apps'||route.name==='favoriteActions')navigate({name:'favorites'});else navigate({name:'overview'});}
  function currentWidget(){return route.key===M.HEADER&&draft?M.headerWidget(draft):draft?.widgets.find(w=>w.key===route.key);}
  const widgetType=w=>w.type==='android'?(w.presentation?.label||t('androidWidget')):w.type==='unsupported'?t('unsupportedWidget'):['rain','calendar','air','favorites','world_clock','grocery','timer'].includes(w.type)?t(w.type):w.type==='app'?t('appShortcut'):w.type;
  const appName=key=>state.apps.find(app=>app.key===key)?.name||t('unavailable');
  function locationName(w){const s=w.settings;if(w.type==='grocery')return draft.groceries?.[s.listId]?.title||'';if(w.type==='rain')return s.label|| (s.locationSource==='device'?t('automatic'):s.name||t('unnamed'));
    if(w.type==='grocery')return draft.groceries?.[s.listId]?.title||'';
    if(w.type==='world_clock')return s.title;
    if(w.type==='air')return s.source==='airbkk'?(state.stations.find(x=>x.id===s.station)?.name||t('station')):s['location-mode']==='auto'?t('automatic'):s['location-name']||t('unnamed');return '';}
  function title(w){let name=widgetType(w),where=locationName(w);if(where)name+=' · '+where;
    const duplicates=draft.widgets.filter(x=>x.type===w.type&&locationName(x)===where);if(!w.fixed&&duplicates.length>1)name+=' '+(duplicates.findIndex(x=>x.key===w.key)+1);return name;}
  function summary(w){const s=w.settings;if(w.type==='grocery')return t('gCount',{count:draft.groceries?.[s.listId]?.items.length||0});if(w.type==='rain')return t('nextHours',{hours:s.hours})+' · '+t('followingDays',{days:s.days});
    if(w.type==='world_clock')return t('clockCount',{count:s.places.length})+' · '+PhoneClock.summary(s,t);
    if(w.type==='calendar')return t('holidays',{days:s.holidayLookaheadDays})+' · '+t(s.flowerWeeks);
    if(w.type==='air')return t(s.source==='airbkk'?'airbkk':'model')+' · '+t('trends');
    if(w.type==='favorites')return t('appsCount',{count:draft.favorites.length})+' · '+(w.height===1?t('bottomRow'):t('favorites'));return widgetType(w);}
  const icon=w=>({android:'▣',unsupported:'▣',rain:'☂',calendar:'▦',air:'∿',favorites:'★',grocery:'▤',timer:'⏱',app:'▣'}[w.type]||'◷');
  function field(parent,label,value,onEdit,type='text',limits={}){
    const wrap=el('label'),caption=el('span',label),input=el('input');input.type=type;input.setAttribute('aria-label',label);
    if(type==='checkbox'){input.checked=!!value;wrap.className='check';}else input.value=value??'';
    for(const [key,v]of Object.entries(limits))input[key]=v;
    input.addEventListener(type==='checkbox'?'change':'input',()=>{onEdit(type==='checkbox'?input.checked:type==='number'?input.valueAsNumber:input.value);changed();});
    wrap.append(caption,input);parent.append(wrap);return input;
  }
  function select(parent,label,value,choices,onEdit){const wrap=el('label'),input=el('select');input.setAttribute('aria-label',label);wrap.append(el('span',label));
    for(const [id,name]of choices){const option=el('option',name);option.value=id;option.selected=String(value)===String(id);input.append(option);}
    input.onchange=()=>{onEdit(input.value);changed();};wrap.append(input);parent.append(wrap);return input;
  }
  function choices(parent,label,current,values,onEdit){const group=el('fieldset',undefined,'segments');group.append(el('legend',label));
    for(const value of values){const control=button(String(value),()=>{onEdit(value);for(const b of group.querySelectorAll('button'))b.setAttribute('aria-pressed',String(b===control));changed();});control.setAttribute('aria-pressed',String(value===current));group.append(control);}parent.append(group);}
  function header(){const root=$('heading');root.replaceChildren();if(state&&route.name!=='overview')root.append(button('‹ '+t('back'),back,'back'));
    const name=route.name==='overview'?t('overview'):route.name==='widget'&&currentWidget()?title(currentWidget()):t(route.name==='favoriteActions'?'favorites':route.name==='apps'?'addApps':route.name==='recommended'?'recommended':route.name==='add'?'add':route.name==='arrange'?'arrange':route.name==='favorites'?'favorites':route.name==='shortcut'?'addTVIcon':route.name==='providers'?'providers':'overview');root.append(el('h1',name));
    const target=$('target');target.replaceChildren();if(!state)return;
    const menu=details(target,state.tv);menu.className='target-menu';
    menu.append(button(t('providers'),()=>navigate({name:'providers'})),button(t("addTVIcon"),()=>navigate({name:"shortcut"})),button(t('changeTV'),()=>message(t('changeTVHelp'))),button(t('help'),()=>message(t('helpText'))),button(t('done'),finish));
  }
  function diagram(parent,{arrange=false,page=0}={}){
    const grid=el('div',undefined,'dashboard-diagram'+(arrange?' arranging':''));grid.style.setProperty('--columns',draft.columns);grid.style.setProperty('--rows',draft.rows);grid.setAttribute('aria-label',t('page',{page:page+1}));parent.append(grid);
    for(const w of draft.widgets.filter(x=>x.page===page)){
      const tile=el('div',undefined,'dashboard-card '+w.type+(route.key===w.key?' selected':''));tile.dataset.widgetKey=w.key;
      const paint=(position,valid=true)=>{tile.style.gridColumn=(position.column+1)+' / span '+position.width;tile.style.gridRow=(position.row+1)+' / span '+position.height;tile.classList.toggle('blocked',!valid);};paint(w);
      const edit=button(icon(w)+' '+widgetType(w),()=>navigate(arrange?{name:'arrange',key:w.key,page}:{name:w.type==='favorites'?'favorites':'widget',key:w.key}),'card-label');tile.append(edit);
      if(arrange&&route.key===w.key){
        const wire=(mode)=>{const handle=button(mode==='move'?'⠿':'↘',()=>{},'spatial-handle '+mode);handle.setAttribute('aria-label',t(mode==='move'?'dragCard':'resizeCard',{name:title(w)}));tile.append(handle);
          disposers.push(PhoneDrag.spatial(handle,grid,{initial:()=>M.clone(w),candidate:(original,dx,dy,rect)=>mode==='move'?{...original,column:original.column+Math.round(dx/(rect.width/draft.columns)),row:original.row+Math.round(dy/(rect.height/draft.rows))}:{...original,width:original.width+Math.round(dx/(rect.width/draft.columns)),height:original.height+Math.round(dy/(rect.height/draft.rows))},
            valid:candidate=>M.validPosition(draft,candidate),paint,commit:candidate=>{Object.assign(w,candidate);changed();render();announce(t(mode==='move'?'movedCard':'resizedCard'));},cancel:(original,active)=>{paint(original);if(active)announce(t('blocked'));}}));};wire('move');wire('resize');
      }
      grid.append(tile);
    }
    return grid;
  }
  function topBar(parent,locked=false){
    const box=section(parent,t('topBar')),w=M.headerWidget(draft);
    box.append(button('◷ '+title(w)+' · '+t('fixedSlot'),()=>navigate({name:'widget',key:M.HEADER}),'widget-row'));
    note(box,locked?t('fixedHelp'):summary(w));
  }
  function overview(parent){
    topBar(parent);
    const intro=section(parent,t('dashboard'));diagram(intro);note(intro,t('tap'));
    const list=el('div',undefined,'widget-list');intro.append(list);
    for(const w of draft.widgets){const row=button('',()=>navigate({name:w.type==='favorites'?'favorites':'widget',key:w.key}),'widget-row');row.append(el('span',icon(w),'widget-icon'),el('span',undefined,'widget-copy'));
      row.lastChild.append(el('strong',title(w)),el('span',summary(w),'muted'));row.append(el('span','›','chevron'));list.append(row);}
    if(!draft.widgets.length)note(intro,t('empty'));
    parent.append(button(t('arrange'),()=>navigate({name:'arrange',page:0}),'wide-action'),button('+ '+t('add'),()=>navigate({name:'add'}),'wide-action secondary'),button(t('providers'),()=>navigate({name:'providers'}),'wide-action secondary'));
  }
  function providerDirectory(parent){
    note(parent,t('providerFor',{tv:tvName()}));
    note(parent,t('providerHelp'));
    for(const entry of state.providers||[]){
      const card=section(parent,entry.name);card.className='provider-card';card.dataset.provider=entry.id;
      note(card,entry.route);card.append(el('p',entry.detail));
      note(card,entry.installed?t('providerInstalled',{version:entry.version}):t('providerMissing'));
      // Read-only directory links never enter the shared dashboard draft.
      if(!entry.excluded&&!entry.installed&&typeof entry.url==='string'){
        let url;try{url=new URL(entry.url);}catch(_){continue;}
        if(url.protocol!=='https:'||!(entry.play?url.hostname==='play.google.com'&&url.pathname==='/store/apps/details':url.hostname==='github.com'))continue;
        const link=el('a',t(entry.play?'providerPlay':'providerPublisher'),'wide-action');
        link.href=url.href;link.target='_blank';link.rel='noopener noreferrer';card.append(link);
      }
      if(entry.installed&&!entry.excluded)note(card,t('providerOnTV'));
    }
  }
  function locationForm(parent,w){
    const s=w.settings,air=w.type==='air',key=air?'location-mode':'locationSource',automatic=air?'auto':'device',fixed=air?'manual':'fixed';
    select(parent,t('location'),s[key],[[automatic,t('automatic')],[fixed,t('fixed')]],value=>{s[key]=value;if(value===automatic){delete s.latitude;delete s.longitude;}render();});
    if(s[key]===automatic)return;
    if(s.latitude!==undefined&&s.longitude!==undefined)note(parent,(air?s['location-name']:s.name||t('unnamed'))+' · '+s.latitude+', '+s.longitude);
    const searchBox=section(parent,t('choosePlace')),query=field(searchBox,t('query'),queries.get(w.key)||'',value=>{queries.set(w.key,value);},'search',{maxLength:240});
    // A query belongs to UI state, never to a widget's serialized configuration.
    const result=el('div',undefined,'places');const run=async()=>{
      const generation=++lookups,text=query.value.trim();if(!text)return;result.replaceChildren(el('p',t('searching')));
      try{let response=await api('lookup',{query:text,address:false});if(!response.places.length)response=await api('lookup',{query:text,address:true});if(generation!==lookups)return;
        result.replaceChildren();if(!response.places.length)note(result,t('noPlaces'));
        for(const place of response.places)result.append(button(place.name+' · '+place.latitude+', '+place.longitude,()=>{s.latitude=place.latitude;s.longitude=place.longitude;s[air?'location-name':'name']=place.name;changed();render();},'place-result'));
      }catch(_){if(generation===lookups)result.replaceChildren(el('p',t('lookupFailed')));}
    };
    query.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();run();}});searchBox.append(button(t('search'),run),result);
    const exact=details(parent,t('coordinates'));field(exact,t('placeName'),s[air?'location-name':'name'],value=>s[air?'location-name':'name']=value,'text',{maxLength:240});
    const row=el('div',undefined,'two-fields');field(row,t('latitude'),s.latitude,value=>s.latitude=value,'number',{min:-90,max:90,step:'any',required:true});field(row,t('longitude'),s.longitude,value=>s.longitude=value,'number',{min:-180,max:180,step:'any',required:true});exact.append(row);
  }
  function widgetForm(parent,w){
    if(!w){navigate({name:'overview'},true);return;}
    const box=section(parent),s=w.settings;note(box,summary(w));
    if(w.type==='android'||w.type==='unsupported'){
      note(box,t(w.type==='android'?'androidSetup':'unsupportedWidget'));
      if(w.type==='android'&&w.presentation?.available===false)note(box,t('unavailable'));
    } else if(w.type==='timer'){
      PhoneTimers.render(box,{state,el,button,note,field,t,api,uid,disposers});
    } else if(w.type==='grocery'){
      PhoneGrocery.form(box,w,{draft,state,el,button,field,select,details,note,t,changed,render,uid,disposers,announce});
    } else if(w.type==='world_clock'){
      PhoneClock.form(box,w,{state,el,button,field,select,details,note,t,changed,render,uid,language,disposers,announce});
    } else if(w.type==='rain'){
      locationForm(box,w);choices(box,t('hours'),s.hours,[6,12,24,48],value=>s.hours=value);
      select(box,t('days'),s.days,Array.from({length:7},(_,i)=>[i,t('followingDays',{days:i})]),value=>s.days=Number(value));
      const more=details(box,t('more'));select(more,t('units'),String(s.inches),[['false',t('millimetres')],['true',t('inches')]],value=>s.inches=value==='true');field(more,t('animation'),s.animation,value=>s.animation=value,'checkbox');field(more,t('label'),s.label,value=>s.label=value,'text',{maxLength:120});note(more,t('renameHint'));
    } else if(w.type==='calendar'){
      for(const source of ['thai','flowers','buddhist','spain','usa','china']){
        field(box,t('source_'+source),s.enabled.includes(source),enabled=>{s.enabled=s.enabled.filter(x=>x!==source);if(enabled)s.enabled.push(source);render();},'checkbox');
        if(!s.enabled.includes(source))continue;
        if(source==='flowers'||source==='buddhist'){const key=source==='flowers'?'flowerWeeks':'buddhistWeeks';select(box,t(key),s[key],['THIS','NEXT','BOTH'].map(v=>[v,t(v)]),value=>s[key]=value);}
        if(source==='spain'){const more=details(box,t('more'));for(const key of ['spanishNational','catalunya','tarragona','tivissa'])field(more,t(key),s[key],value=>s[key]=value,'checkbox');}
        if(source==='china'){const more=details(box,t('more'));field(more,t('chinaWorkingDays'),s.chinaWorkingDays,value=>s.chinaWorkingDays=value,'checkbox');}
      }
      field(box,t('lookahead'),s.holidayLookaheadDays,value=>s.holidayLookaheadDays=value,'number',{min:1,max:2147483647,step:1,required:true});
      const more=details(box,t('more'));for(const key of ['bangkok','thaiNames'])field(more,t(key),s[key],value=>s[key]=value,'checkbox');
    } else if(w.type==='air'){
      select(box,t('source'),s.source,[['model',t('model')],['airbkk',t('airbkk')]],value=>{s.source=value;render();});
      if(s.source==='model')locationForm(box,w);else select(box,t('station'),s.station,state.stations.map(station=>[station.id,station.name]),value=>s.station=value);
      note(box,t('sourceHelp'));
    }
    if(w.fixed){note(box,t("positionFixed"));note(box,t("fixedHelp"));return;}
    box.append(button(t('arrange'),()=>navigate({name:'arrange',key:w.key,page:w.page}),'secondary'));
    parent.append(button(t('removeWidget'),()=>{if(PhoneGrocery.proposal(state,draft).issue?.key===w.key){showValidation();return;}draft.widgets=draft.widgets.filter(x=>x.key!==w.key);changed();navigate({name:'overview'});},'danger quiet'));
  }
  function appArt(key){
    const art=el('span',Array.from(appName(key))[0]||'▣','app-art'),image=el('img');image.alt='';image.hidden=true;art.append(image);
    const load=()=>{if(!connected)return;if(!iconCache.has(key))iconCache.set(key,api('icon',{key}).catch(()=>({})));
      iconCache.get(key).then(result=>{if(!art.isConnected||!/^data:image\/png;base64,[A-Za-z0-9+/=]+$/.test(result.icon||''))return;art.firstChild.textContent='';image.src=result.icon;image.hidden=false;});};
    if('IntersectionObserver'in window){const observer=new IntersectionObserver(entries=>{if(entries.some(e=>e.isIntersecting)){observer.disconnect();load();}});observer.observe(art);disposers.push(()=>observer.disconnect());}else load();return art;
  }
  function focusGrip(key){requestAnimationFrame(()=>{const row=[...document.querySelectorAll('[data-order-key]')].find(row=>row.dataset.orderKey===key);row?.querySelector('[data-reorder]')?.focus({preventScroll:true});});}
  function reorder(from,to,key){
    const before=[...draft.favorites],after=M.move(before,from,to);if(M.same(before,after))return;
    draft.favorites=after;reorderUndo={before,after};changed();render();focusGrip(key);announce(t('moved',{name:appName(key),position:to+1,count:after.length}));
  }
  function favoriteList(parent){
    const box=section(parent);note(box,t('appsCount',{count:draft.favorites.length})+' · '+t('drag'));
    const list=el('ol',undefined,'favorite-list');list.setAttribute('aria-label',t('selectedApps'));box.append(list);
    for(const key of draft.favorites){
      const row=el('li',undefined,'favorite-row');row.dataset.orderKey=key;
      const handle=button('⠿',()=>{},'reorder-grip');handle.dataset.reorder='true';handle.setAttribute('aria-label',t('dragApp',{name:appName(key)}));
      row.append(handle,appArt(key),el('span',appName(key),'app-name'));
      const actions=button(t('more'),()=>navigate({name:'favoriteActions',key}),'row-actions');actions.setAttribute('aria-label',t('rowActions',{name:appName(key)}));row.append(actions);list.append(row);
    }
    disposers.push(PhoneDrag.orderedList(list,{drop:reorder,actions:key=>navigate({name:'favoriteActions',key}),announce:(kind,key)=>announce(t(kind==='start'?'dragStart':'dragCancel',{name:appName(key)}))}));
    if(reorderUndo&&M.same(reorderUndo.after,draft.favorites))box.append(button(t('undoReorder'),()=>{draft.favorites=[...reorderUndo.before];reorderUndo=null;changed();render();},'secondary'));
    box.append(button('+ '+t('addApps'),()=>navigate({name:'apps'}),'wide-action'));note(box,t('keyboardReorder'));
    select(box,t('artwork'),draft.artwork,[['small',t('small')],['banner',t('banner')]],value=>draft.artwork=value);
  }
  function favoriteActions(parent){
    const key=route.key,index=draft.favorites.indexOf(key);if(index<0){navigate({name:'favorites'},true);return;}
    const box=section(parent,appName(key));
    for(const [label,to]of [['up',index-1],['down',index+1],['first',0],['last',draft.favorites.length-1]]){
      const control=button(t(label),()=>{reorder(index,to,key);navigate({name:'favorites'},true);focusGrip(key);},'wide-action');control.disabled=to<0||to>=draft.favorites.length||to===index;box.append(control);
    }
    box.append(button(t('removeFavorite'),()=>{draft.favorites=draft.favorites.filter(x=>x!==key);reorderUndo=null;changed();navigate({name:'favorites'},true);},'danger wide-action'));
  }
  function appPicker(parent){
    const picked=new Set(),box=section(parent),list=el('div',undefined,'app-picker'),count=el('p',t('picked',{count:0}),'muted');
    const query=field(box,t('findApps'),'',()=>filter(),'search');box.append(list,count);
    const add=button(t('addSelected'),()=>{draft.favorites.push(...picked);reorderUndo=null;changed();navigate({name:'favorites'},true);},'primary wide-action');add.disabled=true;
    const filter=()=>{list.replaceChildren();for(const app of state.apps.filter(a=>a.name.toLocaleLowerCase().includes(query.value.toLocaleLowerCase()))){
      const row=el('label',undefined,'picker-row'),input=el('input');input.type='checkbox';input.setAttribute('aria-label',app.name);input.checked=draft.favorites.includes(app.key)||picked.has(app.key);input.disabled=draft.favorites.includes(app.key);
      input.onchange=()=>{if(input.checked)picked.add(app.key);else picked.delete(app.key);count.textContent=t('picked',{count:picked.size});add.disabled=!picked.size;};
      row.append(input,appArt(app.key),el('span',app.name));list.append(row);
    }};
    box.append(add);filter();
  }
  function placementFields(parent,w){
    const fields=el('div',undefined,'two-fields');
    for(const [key,min,max]of [['page',1,16],['column',1,draft.columns],['row',1,draft.rows],['width',w.type==='calendar'?2:1,draft.columns],['height',1,draft.rows]]){
      const oneBased=['page','column','row'].includes(key);
      const bounds={min,max,step:1,required:true},p=w.presentation;
      if(w.type==='android'&&p&&(key==='width'||key==='height')){
        bounds.disabled=key==='width'?!p.horizontal:!p.vertical;
        const minimum=key==='width'?p.minWidth:p.minHeight;
        if(draft.columns===p.columns&&draft.rows===p.rows&&w[key]>=minimum)bounds.min=minimum;
      }
      field(fields,t(key==='page'?'pageLabel':key),w[key]+(oneBased?1:0),value=>{w[key]=value-(oneBased?1:0);},'number',bounds);
    }
    parent.append(fields);
  }
  function placementAction(w,change){const next={...w,...change};if(!M.validPosition(draft,next)){message(t('blocked'),'warning');return;}Object.assign(w,change);changed();render();}
  function arrange(parent){
    topBar(parent,true);
    const page=route.page||0,box=section(parent);select(box,t('pageLabel'),page,Array.from({length:Math.max(1,...draft.widgets.map(w=>w.page+1))},(_,i)=>[i,t('page',{page:i+1})]),value=>navigate({name:'arrange',page:Number(value)},true));
    diagram(box,{arrange:true,page});note(box,t('schematic'));
    const selected=currentWidget();
    if(selected&&!selected.fixed){
      const controls=section(parent,title(selected));
      const presets=el('div',undefined,'size-choices'),columns=draft.columns;
      const compact=selected.type==='air'?[Math.min(3,columns),2]:selected.type==='calendar'?[2,1]:selected.type==='favorites'?[Math.min(4,columns),1]:[Math.min(4,columns),1];
      const shapes=[['compact',...compact],['wide',Math.min(columns,selected.type==='calendar'?5:4),2],['tall',Math.min(3,columns),Math.min(3,draft.rows)],['full',columns,selected.type==='favorites'||selected.type==='rain'?1:2]];
      for(const [label,width,height]of shapes){const control=button(t(label),()=>placementAction(selected,{width,height}));control.disabled=!M.validPosition(draft,{...selected,width,height});control.setAttribute('aria-pressed',String(selected.width===width&&selected.height===height));presets.append(control);}controls.append(el('h3',t('size')),presets);
      const move=details(controls,t('move')+' / '+t('resize')),buttons=el('div',undefined,'placement-actions');
      for(const [label,change]of [['left',{column:selected.column-1}],['right',{column:selected.column+1}],['moveUp',{row:selected.row-1}],['moveDown',{row:selected.row+1}],['smallerWidth',{width:selected.width-1}],['largerWidth',{width:selected.width+1}],['smallerHeight',{height:selected.height-1}],['largerHeight',{height:selected.height+1}]]){
        const control=button(t(label),()=>placementAction(selected,change));control.disabled=!M.validPosition(draft,{...selected,...change});buttons.append(control);
      }move.append(buttons);
      const advanced=details(controls,t('advanced'));placementFields(advanced,selected);
    }else note(parent,t('selectCard'));
    const cardList=section(parent,t('selectCard'));
    for(const w of draft.widgets)cardList.append(button(title(w)+' · '+t('page',{page:w.page+1}),()=>navigate({name:'arrange',key:w.key,page:w.page},true),'wide-action'));
    const preview=button(t('preview'),async()=>{if(invalid()){showValidation();return;}await api('preview',{revision:state.revision,draft:payload()});previewing=true;message(t('previewHelp'));render();},'secondary wide-action');preview.disabled=!connected||!!invalid()||working||!!pending;parent.append(preview);
    if(previewing)parent.append(button(t('cancelPreview'),async()=>{await api('cancel-preview');previewing=false;message('');render();},'wide-action'));
    parent.append(button(t('recommended'),()=>navigate({name:'recommended'}),'wide-action secondary'));
    const advanced=details(parent,t('advanced')),row=el('div',undefined,'two-fields');field(row,t('columns'),draft.columns,value=>draft.columns=value,'number',{min:4,max:12,step:1,required:true});field(row,t('rows'),draft.rows,value=>draft.rows=value,'number',{min:2,max:12,step:1,required:true});advanced.append(row);
  }
  function addWidgets(parent){const box=section(parent);note(box,t('androidSetup'));for(const type of ['rain','calendar','air','favorites','grocery','timer'])box.append(button('+ '+t(type),()=>{const w=M.add(draft,type,state.instances);changed();navigate({name:type==='favorites'?'favorites':'widget',key:w.key});},'wide-action'));}
  function recommended(parent){const box=section(parent);note(box,t('recommendHelp'));note(box,t('extraCards'));const selected={};
    for(const type of ['rain','air']){const cards=draft.widgets.filter(w=>w.type===type);if(cards.length>1){selected[type]=cards[0].key;select(box,t(type),selected[type],cards.map(w=>[w.key,title(w)]),value=>selected[type]=value);}}
    box.append(button(t('useLayout'),()=>{draft=M.recommended(payload(),state.instances,selected);changed();navigate({name:'arrange',page:0});},'primary wide-action'));
  }
  function payload(){return {...M.clone(draft),groceries:PhoneGrocery.proposal(state,draft).book};}
  function invalid(){if(!draft)return null;return PhoneGrocery.proposal(state,draft).issue||M.validate(payload())||([...$('editor').querySelectorAll('input,select')].some(input=>!input.checkValidity())?{kind:'input'}:null);}
  function validationText(issue){if(issue?.kind==='groceryEditor')return t(issue.code);return t({grid:'gridError',placement:'placementError',range:'rangeError',coordinates:'coordinateError',grocery:'gInvalid',input:'invalid'}[issue?.kind]||'invalid');}
  function showValidation(){const issue=invalid();if(issue?.kind==='groceryEditor'){const records=PhoneGrocery.snapshot(state.tvId);for(const [key,s]of records)if(key===issue.screen&&s.edit)s.edit.open=true;PhoneGrocery.restore(state.tvId,records);}
    if(issue?.key&&route.name!=='arrange'){if(draft.widgets.some(w=>w.key===issue.key))navigate({name:'widget',key:issue.key});else render();}message(validationText(issue),'warning');
    const bad=[...$('editor').querySelectorAll('input,select')].find(input=>!input.checkValidity());if(bad){for(let p=bad.parentElement;p;p=p.parentElement)if(p.tagName==='DETAILS')p.open=true;bad.focus();bad.reportValidity();}}
  const settingLabel=key=>t({holidayLookaheadDays:'lookahead',enabled:'calendar',inches:'units',name:'placeName',locationSource:'location','location-mode':'location','location-name':'placeName'}[key]||key);
  function settingsText(w,group,values){
    if(w.type==='world_clock'){
      if(group==='clockPlacesChanged')return PhoneClock.summary({...w.settings,...values},t);
      const key=Object.keys(values||{})[0],value=values?.[key];
      if(typeof value==='boolean')return t(value?'on':'off');
      return t({manual:'clockManual',west:'clockWest','24':'clock24','12':'clock12',device:'clockDeviceFormat'}[value]||value||'');
    }
    if(group==='location')return locationName({...w,settings:{...w.settings,...values}});
    const key=Object.keys(values||{})[0],value=values?.[key];
    if(key==='inches')return t(value?'inches':'millimetres');
    if(key==='hours')return t('nextHours',{hours:value});if(key==='days')return t('followingDays',{days:value});
    if(key==='enabled')return (value||[]).map(source=>t('source_'+source)).join(', ');
    if(typeof value==='boolean')return t(value?'on':'off');if(['flowerWeeks','buddhistWeeks'].includes(key))return t(value);
    return String(value??'');
  }
  function changeText(change){
    if(change.kind==='grocery'){
      const outcomes=[];
      for(const [collection,added,edited,removed]of [['items','gOutcomeAdded','gOutcomeEdited','gOutcomeRemoved'],['products','gOutcomeKept','gOutcomeProduct','gOutcomeForgot']]){
        const before=change.before?.[collection]||[],after=change.after?.[collection]||[];
        for(const value of after){const old=before.find(x=>x.id===value.id);if(!M.same(old,value)){let name=value.name;if(collection==='items')name+=[value.structured?value.structured.amount+' '+value.structured.unit:value.quantity,value.note].filter(Boolean).map(s=>' · '+s).join('');else if(value.tracking==='manual')name+=' · '+t('gState_'+value.stockState);else if(value.tracking==='quantity')name+=' · '+t('gStock')+' '+(value.stock===''?t('gUnknown'):PhoneGrocery.display(value.stock,value.baseUnit));outcomes.push(t(old?edited:added,{name}));}}
        for(const value of before)if(!after.some(x=>x.id===value.id))outcomes.push(t(removed,{name:value.name}));
      }
      return t('gChanged',{name:change.after?.title||change.before?.title||t('grocery')})+' · '+(outcomes.join('; ')||t('gListDetailsChanged'));
    }
    if(change.kind==='grid')return t('change_grid');if(change.kind==='artwork')return t('change_artwork')+' · '+t(change.value);
    if(change.kind==='favorites')return t('change_favorites')+' · '+(change.after.map(appName).join(' → ')||t('empty'));
    const name=title(change.widget);if(change.kind==='add')return t('change_add',{name,page:t('page',{page:change.widget.page+1})});
    if(change.kind==='remove')return t('change_remove',{name});if(change.kind==='placement')return t('change_placement',{name})+' · '+t('page',{page:change.widget.page+1})+' · '+t('column')+' '+(change.widget.column+1)+' · '+t('row')+' '+(change.widget.row+1)+' · '+change.widget.width+'×'+change.widget.height;
    return t('change_setting',{name,setting:change.group==='location'?t('location'):settingLabel(change.group)})+' · '+settingsText(change.widget,change.group,change.after);
  }
  function updateReview(){const region=$('change-summary');if(!region||!state)return;const list=region.querySelector('ul');list.replaceChildren();
    for(const change of M.diff(state.draft,payload())){const text=changeText(change),row=el('li');if(change.kind==='grocery'&&text.length>360){let count=0;for(const collection of ['items','products']){const before=change.before?.[collection]||[],after=change.after?.[collection]||[];count+=new Set([...before,...after].filter(v=>!M.same(before.find(x=>x.id===v.id),after.find(x=>x.id===v.id))).map(v=>v.id)).size;}const more=details(row,t('gChanged',{name:change.after?.title||change.before?.title||t('grocery')})+' · '+t('gOutcomeCount',{count}));more.append(el('p',text));}else row.textContent=text;list.append(row);}region.hidden=!list.childNodes.length;}
  function addReview(parent){const review=details(parent,t('review'));review.id='change-summary';review.append(el('ul'));updateReview();}
  function updateActions(){
    if(route.name==='shortcut'||route.name==='providers'){$('actions').replaceChildren();return;}
    const root=$('actions');root.replaceChildren();if(!state)return;
    const changes=M.diff(state.draft,payload()),issue=invalid();
    if(changes.length){const review=button(changes.length===1?t('oneChange'):t('changes',{count:changes.length}),()=>{const summary=$('change-summary');summary.open=true;summary.scrollIntoView({block:'center',behavior:'auto'});},'review-link');root.append(review);}
    else root.append(el('span',t(issue?'gUnfinished':'noChanges'),'muted'));
    if(pending){root.append(button(t('checkResult'),()=>connected?reconcile():connect(),'primary'));}
    else {const action=button(working?t('updating',{tv:tvName()}):t('update',{tv:tvName()}),update,'primary');action.disabled=working||!connected||!changes.length||!!issue||conflicts.some(c=>!c.resolved);root.append(action);}
    if(issue)root.append(button(validationText(issue),showValidation,'validation-link'));
    $('editor').inert=working;$('heading').inert=working;
    $('connection').textContent=!connected?t('disconnected'):expiry>0&&expiry<120000?t('remaining',{minutes:Math.ceil(expiry/60000)}):t('connected');
  }
  function conflictText(conflict,value){
    if(value===undefined)return t('removed');
    if(conflict.kind==='grocery')return (value.title||t('grocery'))+' · '+t('gCount',{count:value.items.length});
    const w=draft.widgets.find(w=>w.key===conflict.key)||conflict.mine||conflict.tv;
    if(conflict.kind==='widget'||conflict.kind==='instance')return widgetType(value)+' · '+(locationName(value)||t('page',{page:value.page+1}));
    if(conflict.kind==='favorites')return value.map(appName).join(', ');
    if(conflict.kind==='artwork')return t(value);if(conflict.kind==='grid')return value.columns+' × '+value.rows;
    if(conflict.kind==='placement')return t('page',{page:value.page+1});return settingsText(w,conflict.group,value);
  }
  function renderConflicts(parent){
    const region=section(parent,t('conflictTitle',{tv:tvName()}));
    for(const conflict of conflicts.filter(c=>!c.resolved)){
      const card=section(region,conflict.kind==='grocery'?t('grocery'):conflict.kind==='setting'?settingLabel(conflict.group):conflict.kind==='favorites'?t('favorites'):conflict.key?widgetType(conflict.mine||conflict.tv||{type:'app'}):t('arrange'));
      if(conflict.kind==='instance')note(card,t('replaced'));
      card.append(el('p',t('tvValue')+': '+conflictText(conflict,conflict.tv)),el('p',t('mineValue')+': '+conflictText(conflict,conflict.mine)));
      for(const choice of ['tv','mine']){
        const label=choice==='tv'?t('useTV'):conflict.kind==='instance'&&['rain','air'].includes(conflict.mine?.type)?t('addMine'):t('keepMine');
        card.append(button(label,()=>{M.resolveConflict(draft,conflict,choice,state.instances);conflict.resolved=true;changed();render();},choice==='mine'?'secondary':''));
      }
    }
  }
  function render(){
    if(route.name==='widget'&&draft&&!currentWidget())route={name:'overview'};
    if(route.name==='favoriteActions'&&draft&&!draft.favorites.includes(route.key))route={name:'favorites'};
    const focused=document.activeElement?.getAttribute('aria-label'),sameRoute=render.lastRoute===routeKey();
    for(const dispose of disposers)dispose?.();disposers=[];++lookups;
    header();const root=$('editor');root.replaceChildren();if(!state){intro();return;}
    if(conflicts.some(c=>!c.resolved))renderConflicts(root);
    PhoneGrocery.recovery(root,{draft,state,el,button,field,select,details,note,t,changed,render,uid,disposers,announce});
    if(route.name==='providers'){providerDirectory(root);$('actions').replaceChildren();render.lastRoute=routeKey();return;}
    if(route.name==='shortcut'){shortcutForm(root);$('actions').replaceChildren();render.lastRoute=routeKey();return;}
    if(route.name==='widget')widgetForm(root,currentWidget());else if(route.name==='favorites')favoriteList(root);else if(route.name==='favoriteActions')favoriteActions(root);
    else if(route.name==='apps')appPicker(root);else if(route.name==='arrange')arrange(root);else if(route.name==='add')addWidgets(root);else if(route.name==='recommended')recommended(root);else overview(root);
    addReview(root);
    const secondary=details(root,t('more'));
    const refreshButton=button(t('refresh'),async()=>{await refresh();message(t('refreshed'));});refreshButton.disabled=!!pending;secondary.append(refreshButton);
    secondary.append(button(t('discard'),()=>{draft=M.clone(state.draft);PhoneGrocery.clear(state.tvId);conflicts=[];reorderUndo=null;restoreRecord=null;changed();message('');render();},'danger quiet'));
    if(state.undo){const undo=button(t('undo'),undoUpdate);undo.disabled=!connected||!!pending||working;secondary.append(undo);}
    if(restoreRecord)secondary.append(button(t('restore'),()=>{
      if(restoreRecord.tvId!==state.tvId){message(t('linkedDraft'));return;}
      PhoneGrocery.restore(state.tvId,restoreRecord.groceryEditors);const merged=M.rebase(restoreRecord.base,restoreRecord.draft,state);draft=merged.draft;conflicts=merged.conflicts;route=restoreRecord.route||route;restoreRecord=null;changed();message(t('draftRestored'));render();
    }));
    updateActions();render.lastRoute=routeKey();
    if(sameRoute&&focused){const control=[...document.querySelectorAll('[aria-label]')].find(x=>x.getAttribute('aria-label')===focused);control?.focus({preventScroll:true});}
  }
  function prepareShortcut(label){
    shortcutTV=state.tvId;shortcutLabel=label.trim()||state.tv;
    const url=new URL(location.href);url.hash='';url.search='';url.searchParams.set('tv',shortcutTV);url.searchParams.set('name',shortcutLabel);
    history.replaceState({route},'',url.pathname+url.search);document.title=shortcutLabel;
    document.querySelector('meta[name="apple-mobile-web-app-title"]').content=shortcutLabel;
    document.querySelector('link[rel="manifest"]').href='/shortcut.webmanifest?name='+encodeURIComponent(shortcutLabel);
  }
  function shortcutForm(parent){
    const box=section(parent);note(box,t('shortcutFor',{tv:state.tv}));
    prepareShortcut(shortcutLabel||state.tv);
    field(box,t('iconName'),shortcutLabel,value=>prepareShortcut(value),'text',{maxLength:60});
    note(box,t('shortcutAndroid'));note(box,t('shortcutIPhone'));note(box,t('shortcutReconnect'));
  }
  function intro(){
    const root=$('editor');root.replaceChildren();root.append(el('h1',t('app')),el('p',t('network')),button(t('connect'),connect,'primary'));$('actions').replaceChildren();
  }
  function disconnect(reason='expired'){
    connected=false;working=false;token='';clearTimeout(pollTimer);persist();
    message(t(reason==='ended'?'ended':'expired',{tv:tvName()}),'warning',sessionEnded?undefined:{label:t('reconnect',{tv:tvName()}),run:connect});
    if(state){header();updateActions();}else intro();
  }
  async function connect(){
    persist();sessionEnded=false;message('');const response=await api('pair',{invitation,tvId:shortcutTV});invitation='';token=response.token;
    const root=$('editor');for(const dispose of disposers)dispose?.();disposers=[];
    root.replaceChildren(el('h1',t('waiting',{tv:response.tv||tvName()})),el('p',response.code,'pair-code'),el('p',t('match',{code:response.code})),el('p',t('network'),'muted'));
    $('actions').replaceChildren();poll();
  }
  async function approved(next){
    if(shortcutTV&&shortcutTV!==next.tvId){token='';throw new Error(t('shortcutWrongTV'));}
    PhoneClock.configureDefaults(next.worldClockDefaults);
    if(state&&state.tvId===next.tvId){
      const merged=M.rebase(state,draft,next);state=next;draft=merged.draft;conflicts=merged.conflicts;message(t('rebased'));
    }else{
      persist();state=next;draft=M.clone(next.draft);pending=null;conflicts=[];route={name:'overview'};iconCache.clear();restoreRecord=readRecord();
      if(restoreRecord?.tvId===state.tvId&&restoreRecord.pending){pending=restoreRecord.pending;draft=M.clone(restoreRecord.draft);PhoneGrocery.restore(state.tvId,restoreRecord.groceryEditors);route=restoreRecord.route||route;}
      else if(restoreRecord)message(t('retained'));
    }
    language=next.language==='th'?'th':'en';document.documentElement.lang=language;connected=true;working=false;render();if(pending)await reconcile();
  }
  async function poll(){
    clearTimeout(pollTimer);if(sessionEnded||!token)return;
    try{const result=await api('status');expiry=result.remaining;
      if(result.status==='approved'){if(!connected)await approved(await api('state'));else updateActions();pollTimer=setTimeout(poll,10000);}
      else if(result.status==='pending')pollTimer=setTimeout(poll,1200);else disconnect();
    }catch(_){disconnect();}
  }
  async function refresh(){
    const next=await api('state');if(next.tvId!==state.tvId){persist();message(t('linkedDraft'),'warning');return;}
    const merged=M.rebase(state,draft,next);state=next;draft=merged.draft;conflicts=merged.conflicts;changed();render();
  }
  function accepted(next,operation){
    const hadNewEdits=operation.kind==='apply'&&!M.same(payload(),operation.body.draft);
    const merged=operation.kind==='undo'?M.rebase(operation.base||state,draft,next):hadNewEdits?M.rebase({...state,draft:operation.body.draft},payload(),next):{draft:M.clone(next.draft),conflicts:[]};
    PhoneGrocery.acknowledge(state.tvId,operation.groceryEditors);state=next;draft=merged.draft;conflicts=merged.conflicts;pending=null;working=false;restoreRecord=null;reorderUndo=null;connected=true;
    persist();render();message(t(operation.kind==='undo'?'undoSuccess':'updated',{tv:tvName()}),'success',operation.kind==='apply'?{label:t('undo'),run:undoUpdate}:undefined);
  }
  async function submit(operation){
    operation.base??=M.clone(state);pending=operation;working=true;persist();updateActions();message(t(operation.kind==='apply'?'updating':'checking',{tv:tvName()}));
    try{const next=await api(operation.kind==='apply'?'apply':'undo',operation.body);accepted(next,operation);}
    catch(error){
      working=false;
      if(error.status&&error.status<500){pending=null;persist();
        if(error.field==='revision'){await refresh();message(conflicts.length?t('conflictTitle',{tv:tvName()}):t('rebased'),'warning');}
        else showError(error);
      }else {message(t('checking',{tv:tvName()}),'warning');await reconcile();}
    }finally{working=false;updateActions();}
  }
  async function update(){
    if(!connected||working||pending)return;if(invalid()){showValidation();return;}
    if(conflicts.some(c=>!c.resolved))return;
    await submit({kind:'apply',tvId:state.tvId,groceryEditors:PhoneGrocery.snapshot(state.tvId),body:{revision:state.revision,operation:uid(),draft:payload()}});
  }
  async function undoUpdate(){if(!connected||working||pending)return;await submit({kind:'undo',tvId:state.tvId,body:{revision:state.revision,operation:uid()}});}
  async function reconcile(){
    if(!pending||!connected)return;const operation=pending;
    if(operation.tvId!==state.tvId){message(t('linkedDraft'),'warning');return;}
    try{const result=await api('operation',{operation:operation.body.operation});
      if(result.status==='committed'){accepted(await api('state'),operation);return;}
      message(t('knownNotApplied'),'warning',{label:t('retryUpdate'),run:()=>submit(operation)});
    }catch(_){message(t('uncertain'),'warning',{label:t('checkResult'),run:()=>connected?reconcile():connect()});}
    updateActions();
  }
  async function finish(){persist();sessionEnded=true;try{if(connected)await api('stop');}finally{disconnect('ended');}}
  window.addEventListener('beforeunload',persist);
  intro();if(invitation)connect().catch(showError);
})();
