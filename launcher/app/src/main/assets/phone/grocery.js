'use strict';
/* Exact decimal quantities, household-entered products, and temporary shopping needs. */
window.PhoneGrocery=(()=>{
  const clone=v=>v===undefined?undefined:JSON.parse(JSON.stringify(v)),scale=1000000n;
  function number(v,positive=false){if(typeof v!=='string'||!/^\d{1,9}(\.\d{1,6})?$/.test(v))throw Error('gInvalid');const [a,b='']=v.split('.');const n=BigInt(a)*scale+BigInt(b.padEnd(6,'0'));if(positive&&n===0n)throw Error('gInvalid');return n;}
  function format(n,s=scale){const a=n/s,b=(n%s).toString().padStart(s.toString().length-1,'0').replace(/0+$/,'');return a.toString()+(b?'.'+b:'');}
  const dimension=u=>({g:'g',kg:'g',ml:'ml',l:'ml',piece:'piece',dozen:'piece'}[u]||'');
  const factor=u=>u==='kg'||u==='l'?1000n:u==='dozen'?12n:1n;
  function base(p,amount,unit){const n=number(amount,true);if(dimension(unit)&&dimension(unit)===p.baseUnit)return n*factor(unit)*scale;if(unit===p.packageUnit&&p.packageSize)return n*number(p.packageSize,true);throw Error('gIncompatible');}
  function suggestion(p){if(!p.staple)return null;if(p.tracking==='manual')return p.stockState==='need'?{amount:p.usualAmount,unit:p.usualUnit}:null;if(p.tracking!=='quantity'||p.stock==='')return null;
    const stock=number(p.stock),low=number(p.threshold),target=number(p.target,true);if(stock>low)return null;const step=number(p.packageSize||p.increment,true),count=(target-stock+step-1n)/step;
    return {amount:p.packageSize?count.toString():format(count*step),unit:p.packageSize?p.packageUnit:p.baseUnit,deficit:format(target-stock),expected:format(stock+count*step)};
  }
  const emptyList=(id,title='')=>({id,title,items:[],products:[]});
  const defaults=()=>({groceries:emptyList('groceries')});
  const product=(id,name)=>({id,name,brand:'',variant:'',aliases:'',category:'',staple:false,usualQuantity:'',usualAmount:'',usualUnit:'',baseUnit:'',packageUnit:'',packageSize:'',tracking:'none',stockState:'unknown',stock:'',reported:'',threshold:'',target:'',increment:'1'});
  const item=(id,name)=>({id,name,quantity:'',note:'',productId:''});
  const match=(a,b)=>a.trim().toLocaleLowerCase()===b.trim().toLocaleLowerCase();
  function valid(book){try{if([...JSON.stringify(book)].reduce((n,c)=>n+(c.codePointAt(0)>65535?4:c.codePointAt(0)>2047?3:c.codePointAt(0)>127?2:1),0)>262144)return false;if(Object.keys(book).length>32)return false;for(const [id,l]of Object.entries(book)){
    const text=(v,max,required=false)=>{if(typeof v!=='string'||Array.from(v).length>max||required&&!v.trim())throw Error('gInvalid');};text(id,100,true);if(l.id!==id)return false;text(l.title,120);if(l.items.length>200||l.products.length>200)return false;
    const ids=new Set();for(const p of l.products){text(p.id,100,true);if(ids.has(p.id))return false;ids.add(p.id);text(p.name,120,true);for(const k of ['brand','variant','category'])text(p[k],120);text(p.aliases,500);for(const k of ['usualQuantity','usualAmount','usualUnit','baseUnit','packageUnit','packageSize','tracking','stockState','stock','reported','threshold','target','increment'])text(p[k],40);
      if(typeof p.staple!=='boolean'||!['none','manual','quantity'].includes(p.tracking)||!['unknown','enough','need'].includes(p.stockState)||!['','g','ml','piece'].includes(p.baseUnit))return false;
      if(p.packageSize){number(p.packageSize,true);if(!p.packageUnit.trim()||!p.baseUnit||dimension(p.packageUnit))return false;}if(p.usualAmount)number(p.usualAmount,true);
      if(p.reported&&!Number.isFinite(Date.parse(p.reported)))return false;
      if(p.tracking==='quantity'){if(!p.baseUnit||number(p.target,true)<=number(p.threshold))return false;number(p.increment,true);if(p.stock!==''){number(p.stock);if(!p.reported)return false;}}
    }
    ids.clear();const linked=new Set();for(const i of l.items){text(i.id,100,true);if(ids.has(i.id))return false;ids.add(i.id);text(i.name,120,true);text(i.quantity,40);text(i.note,500);text(i.productId,100);const p=l.products.find(p=>p.id===i.productId);if(i.productId&&(!p||linked.has(i.productId)))return false;if(i.productId)linked.add(i.productId);if(i.structured)base(p,i.structured.amount,i.structured.unit);}
  }return true;}catch(_){return false;}}
  function increased(p,old,q){if(!old.structured)throw Error('gIncompatible');const total=base(p,old.structured.amount,old.structured.unit)+base(p,q.amount,q.unit),divisor=base(p,'1',q.unit),scaled=total*scale/divisor;if(scaled*divisor!==total*scale)throw Error('gIncompatible');return {amount:format(scaled),unit:q.unit};}
  function removeProduct(list,id){list.products=list.products.filter(p=>p.id!==id);for(const i of list.items)if(i.productId===id){if(i.structured){i.quantity=i.structured.amount+' '+i.structured.unit;delete i.structured;}i.productId='';}}
  // Recoverable editors belong to a TV, widget and named list. They never cross contexts.
  const screens=new Map();
  const same=(a,b)=>window.PhoneModel.same(a,b);
  const dirty=e=>e&&e.touched&&(!same(e.value,e.original)||e.keep||e.measureErrors&&Object.keys(e.measureErrors).length);
  function snapshot(tvId){return [...screens.entries()].filter(([,s])=>s.tvId===tvId).map(([key,s])=>[key,clone({...s,undoBook:null,undoAfter:null})]);}
  function restore(tvId,records=[]){for(const [key,s]of records)if(s.tvId===tvId)screens.set(key,clone(s));}
  function clear(tvId){for(const [key,s]of screens)if(s.tvId===tvId)screens.delete(key);}
  function acknowledge(tvId,records=[]){for(const [key,old]of records){const s=screens.get(key);if(s?.tvId===tvId&&same(s.edit,old.edit)){s.edit=null;s.undoBook=null;s.feedback='';}}}
  function display(value,baseUnit){if(value==='')return '';try{const n=number(value);if((baseUnit==='ml'||baseUnit==='g'))return format(n,1000n*scale)+' '+(baseUnit==='ml'?'L':'kg');return format(n)+' '+baseUnit;}catch(_){return value+' '+baseUnit;}}
  function editIssue(e,list){
    const v=e.value;if(e.measureErrors&&Object.keys(e.measureErrors).length)return {field:Object.keys(e.measureErrors)[0],code:'gDecimalRequired'};if(!v.name.trim())return {field:'name',code:'gNameRequired'};
    if(e.kind==='item'){
      if(v.structured){try{base(list.products.find(p=>p.id===v.productId),v.structured.amount,v.structured.unit);}catch(_){return {field:'amount',code:'gIncompatible'};}}
      if(v.productId&&list.items.some(i=>i.id!==v.id&&i.productId===v.productId))return {field:'productId',code:'gAlreadyProduct'};
    }else{
      for(const key of ['packageSize','usualAmount',...(v.tracking==='quantity'?['stock','threshold','target','increment']:[])]){
        if(key==='stock'&&v[key]===''||!['stock','threshold','target','increment'].includes(key)&&!v[key])continue;
        try{number(v[key],!['stock','threshold'].includes(key));}catch(_){return {field:key,code:'gDecimalRequired'};}
      }
      if(v.packageSize&&(!v.packageUnit.trim()||!v.baseUnit))return {field:'packageSize',code:'gPackageRequired'};
      if(v.tracking==='quantity'){
        if(!v.baseUnit)return {field:'baseUnit',code:'gUnitRequired'};
        if(number(v.target,true)<=number(v.threshold))return {field:'target',code:'gTargetRequired'};
      }
    }
    return null;
  }
  function applyEdit(book,s){
    const e=s.edit,l=book[s.listId];if(!l)return {code:'gUnavailable'};
    const records=l[e.kind==='product'?'products':'items'],current=records.find(x=>x.id===e.value.id);
    if(!same(current,e.original)&&!same(current,e.value))return {code:'gEditorConflict'};
    const issue=editIssue(e,l);if(issue)return issue;
    if(e.kind==='product'&&current&&l.items.some(i=>i.productId===current.id&&i.structured)&&['baseUnit','packageUnit','packageSize'].some(k=>current[k]!==e.value[k])&&!e.packageApproved)return {code:'gPackageReview'};
    const value=clone(e.value);
    if(e.kind==='item'&&e.keep&&!value.productId){const p=product(e.stapleId,value.name);p.staple=true;p.tracking='manual';p.usualQuantity=value.quantity;l.products.push(p);value.productId=p.id;}
    const at=records.findIndex(x=>x.id===value.id);if(at<0)records.push(value);else records[at]=value;
    return valid(book)?null:{code:'gInvalid'};
  }
  function proposal(state,draft){const book=clone(draft.groceries);let issue=null;
    for(const [key,s]of screens){if(s.tvId!==state.tvId||!dirty(s.edit))continue;const before=clone(book),problem=applyEdit(book,s);if(problem){Object.keys(book).forEach(k=>delete book[k]);Object.assign(book,before);issue??={...problem,key:s.key,screen:key,kind:'groceryEditor'};}}
    return {book,issue};
  }
  function leave(state,draft){const proposed=proposal(state,draft);if(proposed.issue)return;draft.groceries=proposed.book;for(const [,s]of screens)if(s.tvId===state.tvId&&dirty(s.edit)){s.undoBook=null;s.edit=null;}}
  function recovery(parent,ctx){
    const {state,draft,el,button,note,t,changed,render}=ctx;
    for(const [,s]of screens){
      if(s.tvId!==state.tvId||!dirty(s.edit))continue;
      const widget=draft.widgets.find(w=>w.key===s.key);
      if(widget?.settings.listId===s.listId&&draft.groceries[s.listId])continue;
      const panel=el('section');panel.dataset.groceryRecovery=s.key;parent.append(panel);panel.append(el('h2',t('gRecoveredEdit',{name:s.edit.value.name||s.listTitle||t('grocery')})));
      note(panel,t('gRecoveredHelp'));
      if(!draft.groceries[s.listId]){
        note(panel,[s.edit.value.name,s.edit.value.quantity,s.edit.value.note].filter(Boolean).join(' · '));
        panel.append(button(t('gRestoreDraftList'),()=>{draft.groceries[s.listId]=emptyList(s.listId,s.listTitle||'');s.edit.original=undefined;const v=s.edit.value;if(s.edit.kind==='item'&&v.productId){if(v.structured){v.quantity=v.structured.amount+' '+v.structured.unit;delete v.structured;}v.productId='';}changed();render();}));
        panel.append(button(t('gCancel'),()=>{s.edit=null;changed();render();}));
      }else{s.edit.open=true;form(panel,{key:s.key,type:'grocery',settings:{listId:s.listId}},ctx);}
    }
  }
  function form(parent,w,ctx){
    const {draft,state,el,button,field:rawField,select:rawSelect,details:rawDetails,note,t,changed,render,uid,disposers,announce}=ctx;
    const id=w.settings.listId,screenKey=state.tvId+':'+w.key+':'+id;
    const ui=screens.get(screenKey)||{tvId:state.tvId,key:w.key,listId:id,tab:'list',filter:'all',edit:null,quick:'',batch:'',revision:state.revision};screens.set(screenKey,ui);
    const list=draft.groceries[id];if(list)ui.listTitle=list.title;
    function details(parent,label,open=false){ui.disclosures??={};const node=rawDetails(parent,label,ui.disclosures[label]??open);node.addEventListener('toggle',()=>{if(node.isConnected){ui.disclosures[label]=node.open;changed();}});return node;}
    if(ui.revision!==state.revision){ui.undoBook=null;ui.revision=state.revision;}if(ui.undoAfter&&ui.undoAfter!==JSON.stringify(draft.groceries))ui.undoBook=null;
    let refreshPreview=()=>{};const errors=el('p',undefined,'grocery-error');errors.setAttribute('role','status');parent.append(errors);
    const fail=error=>{errors.textContent=t(error.message==='gIncompatible'?'gIncompatible':error.message||'gInvalid');errors.scrollIntoView({block:'nearest'});};
    const fieldErrors=[];
    function paintIssue(issue){errors.textContent=issue&&!issue.field?t(issue.code):'';for(const f of fieldErrors){const invalid=issue?.field===f.key;f.input.setAttribute('aria-invalid',String(invalid));f.error.textContent=invalid?t(issue.code):'';}}
    function updateEditor(){if(!ui.edit)return;ui.edit.touched=true;ui.edit.packageApproved=false;paintIssue(editIssue(ui.edit,list));refreshPreview();changed();}
    function field(parent,label,value,onEdit,type='text',limits={}){
      const input=rawField(parent,label,value,v=>{onEdit(v);updateEditor();},type,limits);
      if(ui.edit){const names={name:'gName',amount:'gAmount',packageSize:'gPackageSize',usualAmount:'gUsualAmount',stock:'gStock',threshold:'gThreshold',target:'gTarget',increment:'gIncrement'},key=Object.keys(names).find(k=>label===t(names[k])||label.startsWith(t(names[k])+' ('));
        if(key){const error=el('span',undefined,'grocery-field-error');error.id='grocery-field-'+uid();error.setAttribute('aria-live','polite');input.setAttribute('aria-describedby',error.id);input.parentElement.append(error);fieldErrors.push({input,error,key});if(ui.edit.touched)paintIssue(editIssue(ui.edit,list));}}
      return input;
    }
    function select(parent,label,value,choices,onEdit){const input=rawSelect(parent,label,value,choices,v=>{onEdit(v);updateEditor();});if(ui.edit&&label===t('gMeasureUnit')){const error=el('span',undefined,'grocery-field-error');input.parentElement.append(error);fieldErrors.push({input,error,key:'baseUnit'});if(ui.edit.touched)paintIssue(editIssue(ui.edit,list));}return input;}
    function mutate(fn,feedback=''){try{const next=clone(draft.groceries);fn(next[id],next);if(!valid(next))throw Error('gInvalid');ui.undoBook=clone(draft.groceries);ui.undoAfter=JSON.stringify(next);draft.groceries=next;ui.feedback=feedback;if(ui.edit?.open)ui.edit=null;changed();render();}catch(e){fail(e);}}
    function go(edit,replace=false){if(edit&&!replace&&dirty(ui.edit)&&ui.edit.kind===edit.kind&&ui.edit.value.id===edit.value.id){ui.edit.open=true;changed();render();return;}if(edit&&!replace&&dirty(ui.edit)&&ui.edit.value.id!==edit.value.id){const pending=proposal(state,draft);if(pending.issue){fail(Error(pending.issue.code));return;}leave(state,draft);}if(edit){edit.original=clone(list[edit.kind==='product'?'products':'items'].find(x=>x.id===edit.value.id));edit.stapleId=uid();edit.open=true;edit.touched=false;}ui.edit=edit;changed();render();}
    function retain(){if(ui.edit){ui.edit.open=false;leave(state,draft);changed();render();}}

    const listTitle=l=>l.title||t('grocery');
    if(!list){note(parent,t('gUnavailable'));return;}
    const menu=details(parent,listTitle(list));menu.className='grocery-title-menu';
    if(Object.keys(draft.groceries).length>1)select(menu,t('gChooseList'),id,Object.values(draft.groceries).map(l=>[l.id,listTitle(l)]),value=>{const pending=proposal(state,draft);if(pending.issue){fail(Error(pending.issue.code));render();return;}leave(state,draft);w.settings.listId=value;changed();render();});
    const settings=details(menu,t('gListSettings'));
    field(settings,t('gListTitle'),list.title,v=>list.title=v,'text',{maxLength:120});
    let newName=ui.newName||'';rawField(settings,t('gNewList'),newName,v=>{newName=v;ui.newName=v;},'text',{maxLength:120});settings.append(button(t('gCreateList'),()=>{if(!newName.trim())return;if(proposal(state,draft).issue){fail(Error('gFinishEdit'));return;}leave(state,draft);ui.newName='';const key=uid();draft.groceries[key]=emptyList(key,newName.trim());w.settings.listId=key;changed();render();}));
    settings.append(button(t('gDeleteList'),()=>{if(!confirm(t('gDeleteListConfirm')))return;const next=clone(draft.groceries);delete next[id];if(!Object.keys(next).length)next.groceries=emptyList('groceries');const replacement=Object.keys(next)[0];for(const view of draft.widgets)if(view.type==='grocery'&&view.settings.listId===id)view.settings.listId=replacement;draft.groceries=next;for(const [key,s]of screens)if(s.tvId===state.tvId&&s.listId===id)screens.delete(key);ui.undoBook=null;ui.edit=null;changed();render();},'danger'));
    menu.append(button(t('gProducts'),()=>{ui.disclosures[listTitle(list)]=false;ui.tab='products';render();}));
    if(ui.tab!=='list'&&!ui.edit?.open)parent.append(button('‹ '+t('gList'),()=>{ui.tab='list';if(ui.edit)ui.edit.open=false;changed();render();},'back'));
    if(ui.edit&&!ui.edit.open&&dirty(ui.edit))parent.append(button(t('gResumeEdit',{name:ui.edit.value.name||t('gItemDetails')}),()=>{ui.edit.open=true;render();}));
    if(ui.edit?.open){parent.append(button('‹ '+t('gList'),retain,'back'));if(ui.edit.kind==='product')productForm();else itemForm();return;}
    if(ui.tab==='list'){
      const entry=el('div',undefined,'grocery-entry');parent.append(entry);let value=ui.quick;const name=rawField(entry,t('gQuickAdd'),value,v=>{value=v;ui.quick=v;},'text',{maxLength:120});name.enterKeyHint='done';const duplicate=el('div');entry.append(duplicate);
      const add=()=>{const text=value.trim();if(!text)return;const old=list.items.find(i=>match(i.name,text));if(old){duplicate.replaceChildren(el('p',t('gDuplicate')),button(t('gEditExisting'),()=>go({kind:'item',value:clone(old)})),button(t('gAddAnother'),()=>{ui.quick='';mutate(l=>l.items.push(item(uid(),text)),t('gAdded',{name:text}));requestAnimationFrame(()=>document.querySelector('[aria-label="'+t('gQuickAdd')+'"]')?.focus());}));return;}
        ui.quick='';mutate(l=>l.items.push(item(uid(),text)),t('gAdded',{name:text}));requestAnimationFrame(()=>document.querySelector('[aria-label="'+t('gQuickAdd')+'"]')?.focus());};
      entry.append(button(t('gAdd'),add,'primary'));name.onkeydown=e=>{if(e.key==='Enter'){e.preventDefault();add();}};
      if(ui.feedback)note(parent,ui.feedback);note(parent,t('gDraftHelp'));
      const suggestions=el('div');entry.append(suggestions);name.addEventListener('input',()=>{suggestions.replaceChildren();if(!value.trim())return;for(const p of list.products.filter(p=>[p.name,p.brand,p.variant,p.aliases].join(' ').toLocaleLowerCase().includes(value.trim().toLocaleLowerCase())).slice(0,6))suggestions.append(button([p.name,p.brand,p.variant,p.usualAmount?p.usualAmount+' '+p.usualUnit:p.usualQuantity].filter(Boolean).join(' · '),()=>reuse(p)));});
      const rows=el('ol',undefined,'grocery-list');parent.append(rows);
      for(const [index,i]of list.items.entries()){
        const row=el('li',undefined,'grocery-row');row.dataset.orderKey=i.id;const grip=button('⠿',()=>{});grip.dataset.reorder='';grip.setAttribute('aria-label',t('gDrag',{name:i.name}));row.append(grip,button(i.name+(i.structured?' · '+i.structured.amount+' '+i.structured.unit:i.quantity?' · '+i.quantity:''),()=>go({kind:'item',value:clone(i)})));
        const move=to=>{if(to<0||to>=list.items.length)return;mutate(l=>l.items.splice(to,0,l.items.splice(index,1)[0]));announce(t('gMoved'));};

        const options=details(row,t('gRowOptions',{name:i.name}));options.append(button(t('gMoveUp',{name:i.name}),()=>move(index-1)),button(t('gMoveDown',{name:i.name}),()=>move(index+1)));rows.append(row);
      }
      // Same shared pointer/keyboard reorder behavior as Favorites.
      disposers.push(PhoneDrag.orderedList(rows,{drop:(from,to,key)=>{mutate(l=>l.items.splice(to,0,l.items.splice(from,1)[0]));requestAnimationFrame(()=>document.querySelector('[data-order-key="'+key+'"] [data-reorder]')?.focus());},actions:key=>go({kind:'item',value:clone(list.items.find(i=>i.id===key))}),announce:()=>announce(t('gMoved'))}));
      if(!list.items.length)note(parent,t('gEmpty'));
      parent.append(button(t('gCheckStaples'),()=>{ui.disclosures[listTitle(list)]=false;ui.tab='staples';changed();render();},'wide-action'));
      const batch=details(parent,t('gAddSeveral'));const area=el('textarea');area.setAttribute('aria-label',t('gOnePerLine'));area.rows=4;area.value=ui.batch;area.oninput=()=>{ui.batch=area.value;changed();};batch.append(area);const preview=el('div');batch.append(button(t('gPreviewBatch'),()=>{const lines=area.value.split(/\r?\n/).map(s=>s.trim()).filter(Boolean);preview.replaceChildren();for(const name of lines)preview.append(el('p',name+(list.items.some(i=>match(i.name,name))?' · '+t('gDuplicate'):'')));if(lines.length)preview.append(button(t('gAddBatch'),()=>{ui.batch='';mutate(l=>{for(const n of lines)l.items.push(item(uid(),n));});}));}),preview);

    }else{
      if(ui.tab==='products')parent.append(button(t('gNewProduct'),()=>go({kind:'product',value:product(uid(),'')})));
      const staples=list.products.filter(p=>p.staple),needs=p=>p.tracking==='manual'?p.stockState==='need':!!suggestion(p),unknown=p=>p.tracking==='none'||p.tracking==='manual'&&p.stockState==='unknown'||p.tracking==='quantity'&&p.stock==='';
      if(ui.tab==='staples'){parent.append(el('h2',t('gCheckStaples')));select(parent,t('gFilter'),ui.filter,[['all',t('gAllStaples')+' ('+staples.length+')'],['need',t('gState_need')+' ('+staples.filter(needs).length+')'],['unknown',t('gState_unknown')+' ('+staples.filter(unknown).length+')']],v=>{ui.filter=v;render();});}
      const products=list.products.filter(p=>ui.tab==='products'||p.staple&&(ui.filter==='all'||ui.filter==='need'&&needs(p)||ui.filter==='unknown'&&unknown(p)));
      if(!products.length)note(parent,t(staples.length?'gNoMatches':'gNoStaples'));
      for(const p of products){const box=el('section');parent.append(box);box.append(el('h3',[p.name,p.brand,p.variant].filter(Boolean).join(' · ')));
        let proposed=null;try{proposed=suggestion(p);}catch(_){}const existing=list.items.find(i=>i.productId===p.id);
        if(p.tracking==='quantity'){note(box,t('gStock')+': '+(p.stock===''?t('gUnknown'):display(p.stock,p.baseUnit)));if(p.reported)note(box,t('gReported')+': '+new Date(p.reported).toLocaleString(undefined,{hour12:false}));}
        else note(box,t(p.tracking==='manual'?'gState_'+p.stockState:'gNotTracked'));
        if(p.staple&&p.tracking==='manual'){const controls=el('div',undefined,'two-fields');box.append(controls);for(const state of ['need','enough','unknown'])controls.append(button(t('gState_'+state),()=>mutate(l=>{const product=l.products.find(x=>x.id===p.id);product.stockState=state;product.reported=new Date().toISOString();})));}
        if(p.staple&&p.tracking==='none')box.append(button(t('gManual'),()=>mutate(l=>{l.products.find(x=>x.id===p.id).tracking='manual';})));
        if(proposed){if(proposed.amount)note(box,t('gSuggest',{amount:proposed.amount,unit:proposed.unit||''}));if(proposed.deficit){note(box,t('gStockReason',{low:display(p.threshold,p.baseUnit),target:display(p.target,p.baseUnit),expected:display(proposed.expected,p.baseUnit)}));if(p.packageSize)note(box,t('gPackageMeaning',{amount:display(p.packageSize,p.baseUnit),unit:p.packageUnit}));}}
        if(existing){const amount=existing.structured?existing.structured.amount+' '+existing.structured.unit:existing.quantity;note(box,t('gOnList',{amount:amount||existing.name}));if(proposed?.amount&&proposed?.unit&&existing.structured&&!same(existing.structured,{amount:proposed.amount,unit:proposed.unit}))box.append(button(t('gChangeAmount',{before:amount,after:proposed.amount+' '+proposed.unit}),()=>mutate(l=>{l.items.find(i=>i.id===existing.id).structured={amount:proposed.amount,unit:proposed.unit};})));}
        box.append(button(existing?t('gReviewExisting'):proposed?.amount&&proposed?.unit?t('gAddAmount',{amount:proposed.amount,unit:proposed.unit}):t('gAddToList'),()=>existing?go({kind:'item',value:clone(existing)}):reuse(p,proposed)),button(t('gEditProduct'),()=>go({kind:'product',value:clone(p)})));
      }
    }
    if(ui.undoBook)parent.append(button(t('gUndoDraft'),()=>{const next=ui.undoBook;ui.undoBook=clone(draft.groceries);ui.undoAfter=JSON.stringify(next);draft.groceries=next;changed();render();}));
    function reuse(p,proposed=null){const old=list.items.find(i=>i.productId===p.id);if(old){fail(Error('gListed'));return;}let q=proposed?.amount&&proposed?.unit?proposed:p.usualAmount&&p.usualUnit?{amount:p.usualAmount,unit:p.usualUnit}:null;let quantity=p.usualQuantity;if(q)try{base(p,q.amount,q.unit);}catch(_){quantity=quantity||q.amount+' '+q.unit;q=null;}ui.quick='';mutate(l=>l.items.push({...item(uid(),p.name),productId:p.id,quantity,...(q?{structured:{amount:q.amount,unit:q.unit}}:{})}),t('gAdded',{name:p.name}));announce(t('gAdded',{name:p.name}));}
    function guardedSave(collection,value,save){
      const issue=editIssue(ui.edit,list);if(issue&&issue.code!=='gAlreadyProduct'){fail(Error(issue.code));return;}
      const current=list[collection].find(x=>x.id===value.id);
      if(same(current,ui.edit.original)){save();return;}
      const panel=el('section');parent.append(panel,el('h3',t('gEditorConflict')));
      const describe=v=>v?[v.name,v.structured?v.structured.amount+' '+v.structured.unit:v.quantity,v.brand,v.variant,v.stock? t('gStock')+': '+v.stock+' '+v.baseUnit:'',v.note].filter(Boolean).join(' · '):t('removed');
      note(panel,t('tvValue')+': '+describe(current));note(panel,t('mineValue')+': '+describe(value));
      panel.append(button(t('useTV'),()=>go(current?{kind:collection==='products'?'product':'item',value:clone(current)}:null,true)),button(t('keepMine'),()=>{ui.edit.original=clone(current);save();}));
    }
    function itemForm(){const v=ui.edit.value;parent.append(el('h2',t('gItemDetails')));field(parent,t('gName'),v.name,x=>v.name=x,'text',{maxLength:120});
      const link=details(parent,t('gProduct'));select(link,t('gProduct'),v.productId,[['',t('gFreeText')],...list.products.map(p=>[p.id,[p.name,p.brand,p.variant].filter(Boolean).join(' · ')])],id=>{v.productId=id;delete v.structured;render();});
      const p=v.productId?list.products.find(p=>p.id===v.productId):null;
      if(v.structured){
        const amount=el('div',undefined,'two-fields');parent.append(amount);field(amount,t('gAmount'),v.structured.amount,x=>v.structured.amount=x,'text',{inputMode:'decimal',maxLength:16});
        const unit=field(amount,t('gUnit'),v.structured.unit,x=>v.structured.unit=x,'text',{maxLength:40}),suggestions=el('datalist');suggestions.id='units-'+uid();unit.setAttribute('list',suggestions.id);for(const value of [...new Set([p?.packageUnit,'l','ml','kg','g','piece','dozen'].filter(Boolean))]){const option=el('option');option.value=value;suggestions.append(option);}amount.append(suggestions);
        parent.append(button(t('gCustomText'),()=>{v.quantity=v.structured.amount+' '+v.structured.unit;delete v.structured;updateEditor();render();}));
      }else{
        field(parent,t('gHowMuch'),v.quantity,x=>v.quantity=x,'text',{maxLength:40});
        if(p?.baseUnit)parent.append(button(t('gChooseAmount'),()=>{v.structured={amount:p.usualAmount||'1',unit:p.usualUnit||p.packageUnit||p.baseUnit};updateEditor();render();}));
      }
      field(parent,t('gNote'),v.note,x=>v.note=x,'text',{maxLength:500});
      if(!v.productId)field(parent,t('gKeepStaple'),!!ui.edit.keep,x=>{ui.edit.keep=x;ui.edit.touched=true;},'checkbox');
      if(p){let suggested=null;try{suggested=suggestion(p);}catch(_){}if(suggested?.amount&&suggested?.unit)parent.append(button(t('gUseSuggestion',{amount:suggested.amount,unit:suggested.unit}),()=>{v.structured={amount:suggested.amount,unit:suggested.unit};updateEditor();render();}));}
      const preview=el('div');parent.append(preview);
      parent.append(button(t('gDone'),()=>guardedSave('items',v,()=>{
        if(v.productId){const old=list.items.find(i=>i.id!==v.id&&i.productId===v.productId);if(old){preview.replaceChildren(el('p',t('gAlreadyProduct')),button(t('gEditExisting'),()=>go({kind:'item',value:clone(old)})));if(v.structured&&old.structured)try{const p=list.products.find(p=>p.id===v.productId),q=increased(p,old,v.structured);preview.append(button(t('gIncrease',{amount:q.amount,unit:q.unit}),()=>mutate(l=>{l.items.find(i=>i.id===old.id).structured=q;l.items=l.items.filter(i=>i.id!==v.id);})));}catch(_){note(preview,t('gIncompatible'));}return;}}
        mutate(l=>{const value=clone(v);if(ui.edit.keep&&!value.productId){const p=product(ui.edit.stapleId,value.name);p.staple=true;p.tracking='manual';p.usualQuantity=value.quantity;l.products.push(p);value.productId=p.id;}const index=l.items.findIndex(i=>i.id===value.id);if(index<0)l.items.push(value);else l.items[index]=value;});
      }),'primary'));
      if(list.items.some(i=>i.id===v.id))parent.append(button(t('gRemove'),()=>mutate(l=>l.items=l.items.filter(i=>i.id!==v.id)),'danger'));
      parent.append(button(t('gCancel'),()=>go(null)));
    }
    function productForm(){const v=ui.edit.value;parent.append(el('h2',t('gProductDetails')));
      field(parent,t('gName'),v.name,x=>v.name=x,'text',{maxLength:120});field(parent,t('gUsualPurchase'),v.usualQuantity,x=>v.usualQuantity=x,'text',{maxLength:40});const identity=details(parent,t('gProductDetails'));for(const [key,label]of [['brand','gBrand'],['variant','gVariant']])field(identity,t(label),v[key],x=>v[key]=x,'text',{maxLength:120});const more=details(identity,t('more'));for(const [key,label,max]of [['aliases','gAliases',500],['category','gCategory',120]])field(more,t(label),v[key],x=>v[key]=x,'text',{maxLength:max});
      field(parent,t('gKeepStaple'),v.staple,x=>v.staple=x,'checkbox');
      const units=details(parent,t('gUnits'));
      const e=ui.edit;e.measureUnit??=(v.baseUnit==='ml'?'l':v.baseUnit==='g'?'kg':v.baseUnit||'');
      function measureChoice(where){select(where,t('gMeasureUnit'),e.measureUnit,[['',t('gUnknown')],['l','L'],['ml','ml'],['kg','kg'],['g','g'],['piece',t('gPieces')]],x=>{if(e.measureErrors&&Object.keys(e.measureErrors).length)return;e.measureUnit=x;v.baseUnit=dimension(x);render();});}
      function measured(where,key,label){const unit=e.measureUnit,f=factor(unit),raw=e.measureErrors?.[key];let shown=v[key];try{if(shown!=='')shown=format(number(shown),f*scale);}catch(_){}field(where,t(label)+(unit?' ('+(unit==='l'?'L':unit)+')':''),raw??shown,x=>{e.measureErrors??={};try{if(x===''){v[key]='';delete e.measureErrors[key];}else{v[key]=format(number(x)*f);delete e.measureErrors[key];}}catch(_){e.measureErrors[key]=x;}if(key==='stock')v.reported=new Date().toISOString();},'text',{inputMode:'decimal',maxLength:16});}
      measureChoice(units);
      field(units,t('gPackageName'),v.packageUnit,x=>v.packageUnit=x,'text',{maxLength:40});measured(units,'packageSize','gPackageSize');note(units,t('gUnitHelp'));
      field(units,t('gUsualAmount'),v.usualAmount,x=>v.usualAmount=x,'text',{inputMode:'decimal',maxLength:16});field(units,t('gUsualUnit'),v.usualUnit,x=>v.usualUnit=x,'text',{maxLength:40});
      const stock=details(parent,t('gStockPolicy'),v.tracking!=='none');select(stock,t('gTracking'),v.tracking,[['none',t('gNotTracked')],['manual',t('gManual')],['quantity',t('gTrackQuantity')]],x=>{v.tracking=x;render();});
      if(v.tracking==='manual')select(stock,t('gState'),v.stockState,['unknown','enough','need'].map(s=>[s,t('gState_'+s)]),x=>{v.stockState=x;v.reported=new Date().toISOString();});
      if(v.tracking==='quantity'){
        measureChoice(stock);for(const [key,label]of [['stock','gStock'],['threshold','gThreshold'],['target','gTarget'],['increment','gIncrement']])measured(stock,key,label);note(stock,t('gStockHelp'));if(v.reported)note(stock,t('gReported')+': '+new Date(v.reported).toLocaleString(undefined,{hour12:false}));
      }
      const packageReview=el('section');parent.append(packageReview);
      refreshPreview=()=>{packageReview.replaceChildren();const old=list.products.find(p=>p.id===v.id),affected=list.items.filter(i=>i.productId===v.id&&i.structured);if(!old||!affected.length||!['baseUnit','packageUnit','packageSize'].some(k=>old[k]!==v[k]))return;
        note(packageReview,t('gPackageChange'));note(packageReview,'1 '+old.packageUnit+' = '+display(old.packageSize,old.baseUnit)+' → 1 '+v.packageUnit+' = '+display(v.packageSize,v.baseUnit));
        for(const i of affected){let meaning=t('gIncompatible');try{meaning=display(format(base(v,i.structured.amount,i.structured.unit),scale*scale),v.baseUnit);}catch(_){}note(packageReview,i.name+' · '+i.structured.amount+' '+i.structured.unit+' → '+meaning);}};refreshPreview();
      parent.append(button(t('gDone'),()=>guardedSave('products',v,()=>{const saved=list.products.find(p=>p.id===v.id);if(saved&&list.items.some(i=>i.productId===v.id&&i.structured)&&['baseUnit','packageUnit','packageSize'].some(k=>saved[k]!==v[k])&&!confirm(t('gPackageChange')))return;ui.edit.packageApproved=true;mutate(l=>{const at=l.products.findIndex(p=>p.id===v.id);if(at<0)l.products.push(clone(v));else l.products[at]=clone(v);});}), 'primary'));
      if(list.products.some(p=>p.id===v.id)){
        parent.append(button(t('gDeleteProduct'),()=>{if(confirm(t('gDeleteProductConfirm')))mutate(l=>removeProduct(l,v.id));},'danger'));
        const merge=details(parent,t('gMerge'));let target='';select(merge,t('gMergeInto'),'', [['',t('gChooseProduct')],...list.products.filter(p=>p.id!==v.id).map(p=>[p.id,p.name+' '+p.brand+' '+p.variant])],x=>target=x);
        merge.append(button(t('gPreviewMerge'),()=>{const p=list.products.find(p=>p.id===target);if(!p)return;const sourceRow=list.items.find(i=>i.productId===v.id),targetRow=list.items.find(i=>i.productId===target);let q=null;
          try{if(sourceRow?.structured)base(p,sourceRow.structured.amount,sourceRow.structured.unit);if(sourceRow&&targetRow){if(!sourceRow.structured||!targetRow.structured)throw Error('gIncompatible');q=increased(p,targetRow,sourceRow.structured);}}catch(e){fail(e);return;}
          const panel=el('div');merge.append(panel);note(panel,t('gMergeWarning',{source:v.name,target:p.name})+(q?' '+q.amount+' '+q.unit:''));panel.append(button(t('gConfirmMerge'),()=>mutate(l=>{const destination=l.products.find(x=>x.id===target);destination.staple=destination.staple||v.staple;destination.aliases=[destination.aliases,v.name,v.aliases].filter(Boolean).join(', ');if(sourceRow){if(targetRow){l.items.find(i=>i.id===targetRow.id).structured=q;l.items=l.items.filter(i=>i.id!==sourceRow.id);}else l.items.find(i=>i.id===sourceRow.id).productId=target;}l.products=l.products.filter(p=>p.id!==v.id);})));}));
      }
      parent.append(button(t('gCancel'),()=>go(null)));
    }
  }
  return {defaults,emptyList,product,item,valid,number,format,base,suggestion,increased,removeProduct,form,recovery,leave,snapshot,restore,clear,acknowledge,proposal,display,back:(state,draft,w)=>{const ui=screens.get(state.tvId+':'+w.key+':'+w.settings.listId);if(ui?.edit?.open){ui.edit.open=false;leave(state,draft);return true;}if(ui&&ui.tab!=='list'){ui.tab='list';return true;}return false;}};
})();
