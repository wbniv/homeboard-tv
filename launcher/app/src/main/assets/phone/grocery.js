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
  const screens=new Map();
  function form(parent,w,ctx){
    const {draft,state,el,button,field,select,details,note,t,changed,render,uid,disposers,announce}=ctx;
    const id=w.settings.listId,list=draft.groceries[id],screenKey=state.tvId+':'+w.key,ui=screens.get(screenKey)||{tab:'list',edit:null,revision:state.revision};screens.set(screenKey,ui);if(ui.revision!==state.revision){ui.undoBook=null;ui.revision=state.revision;}if(ui.undoAfter&&ui.undoAfter!==JSON.stringify(draft.groceries))ui.undoBook=null;
    const fail=error=>{alert(t(error.message==='gIncompatible'?'gIncompatible':'gInvalid'));};
    function mutate(fn){try{const next=clone(draft.groceries);fn(next[id],next);if(!valid(next))throw Error('gInvalid');ui.undoBook=clone(draft.groceries);ui.undoAfter=JSON.stringify(next);draft.groceries=next;ui.edit=null;changed();render();}catch(e){fail(e);}}
    function go(edit){if(edit)edit.original=clone(list[edit.kind==='product'?'products':'items'].find(x=>x.id===edit.value.id));ui.edit=edit;render();}
    const listTitle=l=>l.title||t('grocery');
    select(parent,t('gChooseList'),id,Object.values(draft.groceries).map(l=>[l.id,listTitle(l)]),value=>{w.settings.listId=value;ui.edit=null;changed();render();});
    if(!list){note(parent,t('gUnavailable'));return;}
    const settings=details(parent,t('gListSettings'));
    field(settings,t('gListTitle'),list.title,v=>list.title=v,'text',{maxLength:120});
    let newName='';field(settings,t('gNewList'),'',v=>newName=v,'text',{maxLength:120});settings.append(button(t('gCreateList'),()=>{if(!newName.trim())return;const key=uid();draft.groceries[key]=emptyList(key,newName.trim());w.settings.listId=key;changed();render();}));
    settings.append(button(t('gDeleteList'),()=>{if(!confirm(t('gDeleteListConfirm')))return;const next=clone(draft.groceries);delete next[id];if(!Object.keys(next).length)next.groceries=emptyList('groceries');const replacement=Object.keys(next)[0];for(const view of draft.widgets)if(view.type==='grocery'&&view.settings.listId===id)view.settings.listId=replacement;draft.groceries=next;ui.undoBook=null;ui.edit=null;changed();render();},'danger'));
    const tabs=el('div',undefined,'two-fields');parent.append(tabs);for(const tab of ['list','staples','products']){const b=button(t('g'+tab[0].toUpperCase()+tab.slice(1)),()=>{ui.tab=tab;ui.edit=null;render();});b.setAttribute('aria-pressed',String(ui.tab===tab));tabs.append(b);}
    if(ui.edit){if(ui.edit.kind==='product')productForm();else itemForm();return;}
    if(ui.tab==='list'){
      note(parent,t('gDraftHelp'));
      const entry=el('div',undefined,'grocery-entry');parent.append(entry);let value='';const name=field(entry,t('gQuickAdd'),'',v=>value=v,'text',{maxLength:120});name.enterKeyHint='done';const duplicate=el('div');entry.append(duplicate);
      const add=()=>{const text=value.trim();if(!text)return;const old=list.items.find(i=>match(i.name,text));if(old){duplicate.replaceChildren(el('p',t('gDuplicate')),button(t('gEditExisting'),()=>go({kind:'item',value:clone(old)})),button(t('gAddAnother'),()=>{mutate(l=>l.items.push(item(uid(),text)));requestAnimationFrame(()=>document.querySelector('[aria-label="'+t('gQuickAdd')+'"]')?.focus());}));return;}
        mutate(l=>l.items.push(item(uid(),text)));requestAnimationFrame(()=>document.querySelector('[aria-label="'+t('gQuickAdd')+'"]')?.focus());};
      entry.append(button(t('gAdd'),add,'primary'));name.onkeydown=e=>{if(e.key==='Enter'){e.preventDefault();add();}};
      const suggestions=el('div');entry.append(suggestions);name.addEventListener('input',()=>{suggestions.replaceChildren();if(!value.trim())return;for(const p of list.products.filter(p=>[p.name,p.brand,p.variant,p.aliases].join(' ').toLocaleLowerCase().includes(value.trim().toLocaleLowerCase())).slice(0,6))suggestions.append(button([p.name,p.brand,p.variant].filter(Boolean).join(' · '),()=>go({kind:'item',value:{...item(uid(),p.name),productId:p.id,quantity:p.usualQuantity,...(p.usualAmount&&p.usualUnit?{structured:{amount:p.usualAmount,unit:p.usualUnit}}:{})}})));});
      const batch=details(parent,t('gAddSeveral'));const area=el('textarea');area.setAttribute('aria-label',t('gOnePerLine'));area.rows=4;batch.append(area);const preview=el('div');batch.append(button(t('gPreviewBatch'),()=>{const lines=area.value.split(/\r?\n/).map(s=>s.trim()).filter(Boolean);preview.replaceChildren();for(const name of lines)preview.append(el('p',name+(list.items.some(i=>match(i.name,name))?' · '+t('gDuplicate'):'')));if(lines.length)preview.append(button(t('gAddBatch'),()=>mutate(l=>{for(const n of lines)l.items.push(item(uid(),n));})));}),preview);
      const rows=el('ol',undefined,'grocery-list');parent.append(rows);
      for(const [index,i]of list.items.entries()){
        const row=el('li',undefined,'grocery-row');row.dataset.orderKey=i.id;const grip=button('⠿',()=>{});grip.dataset.reorder='';grip.setAttribute('aria-label',t('gDrag',{name:i.name}));row.append(grip,button(i.name+(i.structured?' · '+i.structured.amount+' '+i.structured.unit:i.quantity?' · '+i.quantity:''),()=>go({kind:'item',value:clone(i)})));
        const move=to=>{if(to<0||to>=list.items.length)return;mutate(l=>l.items.splice(to,0,l.items.splice(index,1)[0]));announce(t('gMoved'));};

        row.append(button('↑',()=>move(index-1)),button('↓',()=>move(index+1)));row.children[2].setAttribute('aria-label',t('gMoveUp',{name:i.name}));row.children[3].setAttribute('aria-label',t('gMoveDown',{name:i.name}));rows.append(row);
      }
      // Same shared pointer/keyboard reorder behavior as Favorites.
      disposers.push(PhoneDrag.orderedList(rows,{drop:(from,to,key)=>{mutate(l=>l.items.splice(to,0,l.items.splice(from,1)[0]));requestAnimationFrame(()=>document.querySelector('[data-order-key="'+key+'"] [data-reorder]')?.focus());},actions:key=>go({kind:'item',value:clone(list.items.find(i=>i.id===key))}),announce:()=>announce(t('gMoved'))}));
      if(!list.items.length)note(parent,t('gEmpty'));
    }else{
      if(ui.tab==='products')parent.append(button(t('gNewProduct'),()=>go({kind:'product',value:product(uid(),'')})));
      const products=list.products.filter(p=>ui.tab==='products'||p.staple);
      if(!products.length)note(parent,t('gNoStaples'));
      for(const p of products){const box=el('section');parent.append(box);box.append(el('h3',[p.name,p.brand,p.variant].filter(Boolean).join(' · ')));
        let proposed=null;try{proposed=suggestion(p);}catch(_){}const existing=list.items.find(i=>i.productId===p.id);
        if(p.tracking==='quantity'){note(box,t('gStock')+': '+(p.stock===''?t('gUnknown'):p.stock+' '+p.baseUnit));if(p.reported)note(box,t('gReported')+': '+new Date(p.reported).toLocaleString());}
        else note(box,t(p.tracking==='manual'?'gState_'+p.stockState:'gNotTracked'));
        if(p.staple&&p.tracking==='manual'){const controls=el('div',undefined,'two-fields');box.append(controls);for(const state of ['need','enough','unknown'])controls.append(button(t('gState_'+state),()=>mutate(l=>{const product=l.products.find(x=>x.id===p.id);product.stockState=state;product.reported=new Date().toISOString();})));}
        if(p.staple&&p.tracking==='none')box.append(button(t('gManual'),()=>mutate(l=>{l.products.find(x=>x.id===p.id).tracking='manual';})));
        if(proposed){if(proposed.amount)note(box,t('gSuggest',{amount:proposed.amount,unit:proposed.unit||''}));if(proposed.deficit)note(box,t('gReason',{deficit:proposed.deficit,expected:proposed.expected,unit:p.baseUnit}));}
        box.append(button(existing?t('gReviewExisting'):t('gAddToList'),()=>go({kind:'item',value:existing?clone(existing):{...item(uid(),p.name),productId:p.id,quantity:p.usualQuantity,...(proposed?.amount&&proposed?.unit?{structured:{amount:proposed.amount,unit:proposed.unit}}:p.usualAmount&&p.usualUnit?{structured:{amount:p.usualAmount,unit:p.usualUnit}}:{})}})),button(t('gEditProduct'),()=>go({kind:'product',value:clone(p)})));
      }
    }
    if(ui.undoBook)parent.append(button(t('gUndoDraft'),()=>{const next=ui.undoBook;ui.undoBook=clone(draft.groceries);ui.undoAfter=JSON.stringify(next);draft.groceries=next;changed();render();}));
    function guardedSave(collection,value,save){
      const current=list[collection].find(x=>x.id===value.id);
      if(JSON.stringify(current)===JSON.stringify(ui.edit.original)){save();return;}
      const panel=el('section');parent.append(panel,el('h3',t('gEditorConflict')));
      const describe=v=>v?[v.name,v.structured?v.structured.amount+' '+v.structured.unit:v.quantity,v.brand,v.variant,v.stock? t('gStock')+': '+v.stock+' '+v.baseUnit:'',v.note].filter(Boolean).join(' · '):t('removed');
      note(panel,t('tvValue')+': '+describe(current));note(panel,t('mineValue')+': '+describe(value));
      panel.append(button(t('useTV'),()=>go(current?{kind:collection==='products'?'product':'item',value:clone(current)}:null)),button(t('keepMine'),()=>{ui.edit.original=clone(current);save();}));
    }
    function itemForm(){const v=ui.edit.value;parent.append(el('h2',t('gItemDetails')));field(parent,t('gName'),v.name,x=>v.name=x,'text',{maxLength:120});
      select(parent,t('gProduct'),v.productId,[['',t('gFreeText')],...list.products.map(p=>[p.id,[p.name,p.brand,p.variant].filter(Boolean).join(' · ')])],id=>{v.productId=id;delete v.structured;render();});
      field(parent,t('gQuantityText'),v.quantity,x=>v.quantity=x,'text',{maxLength:40});field(parent,t('gNote'),v.note,x=>v.note=x,'text',{maxLength:500});
      let keep=false;if(!v.productId)field(parent,t('gKeepStaple'),false,x=>keep=x,'checkbox');
      if(v.productId){const p=list.products.find(p=>p.id===v.productId);let suggested=null;try{suggested=suggestion(p);}catch(_){}if(suggested?.amount&&suggested?.unit)parent.append(button(t('gUseSuggestion',{amount:suggested.amount,unit:suggested.unit}),()=>{v.structured={amount:suggested.amount,unit:suggested.unit};render();}));field(parent,t('gStructured'),!!v.structured,on=>{if(on)v.structured={amount:p.usualAmount||'1',unit:p.usualUnit||p.packageUnit||p.baseUnit||'piece'};else delete v.structured;render();},'checkbox');
        if(v.structured){field(parent,t('gAmount'),v.structured.amount,x=>v.structured.amount=x,'text',{inputMode:'decimal',maxLength:16});field(parent,t('gUnit'),v.structured.unit,x=>v.structured.unit=x,'text',{maxLength:40});try{note(parent,'= '+format(base(p,v.structured.amount,v.structured.unit),scale*scale)+' '+p.baseUnit);}catch(_){note(parent,t('gIncompatible'));}}
      }
      const preview=el('div');parent.append(preview);
      parent.append(button(t('gSaveItem'),()=>guardedSave('items',v,()=>{
        if(v.productId){const old=list.items.find(i=>i.id!==v.id&&i.productId===v.productId);if(old){preview.replaceChildren(el('p',t('gAlreadyProduct')),button(t('gEditExisting'),()=>go({kind:'item',value:clone(old)})));if(v.structured&&old.structured)try{const p=list.products.find(p=>p.id===v.productId),q=increased(p,old,v.structured);preview.append(button(t('gIncrease',{amount:q.amount,unit:q.unit}),()=>mutate(l=>{l.items.find(i=>i.id===old.id).structured=q;l.items=l.items.filter(i=>i.id!==v.id);})));}catch(_){note(preview,t('gIncompatible'));}return;}}
        mutate(l=>{const value=clone(v);if(keep&&!value.productId){const p=product(uid(),value.name);p.staple=true;p.usualQuantity=value.quantity;l.products.push(p);value.productId=p.id;}const index=l.items.findIndex(i=>i.id===value.id);if(index<0)l.items.push(value);else l.items[index]=value;});
      }),'primary'));
      if(list.items.some(i=>i.id===v.id))parent.append(button(t('gRemove'),()=>mutate(l=>l.items=l.items.filter(i=>i.id!==v.id)),'danger'));
      parent.append(button(t('gCancel'),()=>go(null)));
    }
    function productForm(){const v=ui.edit.value;parent.append(el('h2',t('gProductDetails')));
      for(const [key,label,max]of [['name','gName',120],['brand','gBrand',120],['variant','gVariant',120],['aliases','gAliases',500],['category','gCategory',120]])field(parent,t(label),v[key],x=>v[key]=x,'text',{maxLength:max});
      field(parent,t('gKeepStaple'),v.staple,x=>v.staple=x,'checkbox');
      const units=details(parent,t('gUnits'),true);select(units,t('gBaseUnit'),v.baseUnit,[['',t('gUnknown')],['g','g'],['ml','ml'],['piece',t('gPieces')]],x=>v.baseUnit=x);
      field(units,t('gPackageName'),v.packageUnit,x=>v.packageUnit=x,'text',{maxLength:40});field(units,t('gPackageSize'),v.packageSize,x=>v.packageSize=x,'text',{inputMode:'decimal',maxLength:16});note(units,t('gUnitHelp'));
      field(units,t('gQuantityText'),v.usualQuantity,x=>v.usualQuantity=x,'text',{maxLength:40});
      field(units,t('gUsualAmount'),v.usualAmount,x=>v.usualAmount=x,'text',{inputMode:'decimal',maxLength:16});field(units,t('gUsualUnit'),v.usualUnit,x=>v.usualUnit=x,'text',{maxLength:40});
      const stock=details(parent,t('gStockPolicy'),true);select(stock,t('gTracking'),v.tracking,[['none',t('gNotTracked')],['manual',t('gManual')],['quantity',t('gTrackQuantity')]],x=>{v.tracking=x;render();});
      if(v.tracking==='manual')select(stock,t('gState'),v.stockState,['unknown','enough','need'].map(s=>[s,t('gState_'+s)]),x=>{v.stockState=x;v.reported=new Date().toISOString();});
      if(v.tracking==='quantity'){
        for(const [key,label]of [['stock','gStock'],['threshold','gThreshold'],['target','gTarget'],['increment','gIncrement']])field(stock,t(label)+(v.baseUnit?' ('+v.baseUnit+')':''),v[key],x=>{v[key]=x;if(key==='stock')v.reported=new Date().toISOString();},'text',{inputMode:'decimal',maxLength:16});note(stock,t('gStockHelp'));if(v.reported)note(stock,t('gReported')+': '+new Date(v.reported).toLocaleString());
      }
      parent.append(button(t('gSaveProduct'),()=>guardedSave('products',v,()=>{const saved=list.products.find(p=>p.id===v.id);if(saved&&list.items.some(i=>i.productId===v.id&&i.structured)&&['baseUnit','packageUnit','packageSize'].some(k=>saved[k]!==v[k])&&!confirm(t('gPackageChange')))return;mutate(l=>{const at=l.products.findIndex(p=>p.id===v.id);if(at<0)l.products.push(clone(v));else l.products[at]=clone(v);});}), 'primary'));
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
  return {defaults,emptyList,product,item,valid,number,format,base,suggestion,increased,removeProduct,form};
})();
