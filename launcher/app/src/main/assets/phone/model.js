'use strict';
/* Pure draft operations shared by the UI and container tests. No transport or DOM state. */
window.PhoneModel = (() => {
  const clone = value => value === undefined ? undefined : JSON.parse(JSON.stringify(value));
  const canonical = value => value && typeof value === 'object'
    ? Array.isArray(value) ? value.map(canonical) : Object.fromEntries(Object.keys(value).sort().map(k => [k, canonical(value[k])])) : value;
  const same = (a, b) => JSON.stringify(canonical(a)) === JSON.stringify(canonical(b));
  const placement = ['page', 'column', 'row', 'width', 'height'];
  const pick = (o, fields) => Object.fromEntries(fields.filter(k => Object.hasOwn(o || {}, k)).map(k => [k, clone(o[k])]));
  function settingsGroups(w) {
    if(w.type==='world_clock')return [{name:'clockPlacesChanged',fields:['places','primary','reference']},...['title','ordering','format','seconds'].map(k=>({name:k,fields:[k]}))];
    const location = w.type === 'rain' ? ['locationSource', 'name', 'latitude', 'longitude']
      : w.type === 'air' ? ['source', 'station', 'location-mode', 'location-name', 'latitude', 'longitude'] : [];
    return [...(location.length ? [{name:'location', fields:location}] : []),
      ...Object.keys(w.settings).filter(k => !location.includes(k)).map(k => ({name:k, fields:[k]}))];
  }
  function diff(base, draft) {
    const changes = [];
    if (!same(pick(base, ['columns','rows']), pick(draft, ['columns','rows']))) changes.push({kind:'grid'});
    if (base.artwork !== draft.artwork) changes.push({kind:'artwork', value:draft.artwork});
    if (!same(base.favorites, draft.favorites)) changes.push({kind:'favorites', before:base.favorites, after:draft.favorites});
    for (const old of base.widgets) if (!draft.widgets.some(w => w.key === old.key)) changes.push({kind:'remove', widget:old});
    for (const w of draft.widgets) {
      const old = base.widgets.find(x => x.key === w.key);
      if (!old) { changes.push({kind:'add', widget:w}); continue; }
      if (!same(pick(old, placement), pick(w, placement))) changes.push({kind:'placement', widget:w});
      for (const group of settingsGroups(w)) if (!same(pick(old.settings, group.fields), pick(w.settings, group.fields)))
        changes.push({kind:'setting', widget:w, group:group.name, fields:group.fields, before:pick(old.settings,group.fields), after:pick(w.settings,group.fields)});
    }
    return changes;
  }
  function validPosition(draft, w) {
    if (![...placement.map(k => w[k]), draft.columns, draft.rows].every(Number.isInteger)) return false;
    if (w.page < 0 || w.page > 15 || w.column < 0 || w.row < 0 || w.width < (w.type === 'calendar' ? 2 : 1) || w.height < 1
      || w.column+w.width > draft.columns || w.row+w.height > draft.rows) return false;
    return !draft.widgets.some(other => other.key !== w.key && other.page === w.page && w.column < other.column+other.width
      && w.column+w.width > other.column && w.row < other.row+other.height && w.row+w.height > other.row);
  }
  function validate(draft) {
    if (!Number.isInteger(draft.columns) || draft.columns < 4 || draft.columns > 12 || !Number.isInteger(draft.rows) || draft.rows < 2 || draft.rows > 12) return {kind:'grid'};
    for (const w of draft.widgets) {
      if (!validPosition(draft,w)) return {kind:'placement', key:w.key};
      const s=w.settings;
      if(w.type==='world_clock'&&!window.PhoneClock.valid(s))return {kind:'range',key:w.key};
      if (w.type === 'rain' && (![6,12,24,48].includes(s.hours) || !Number.isInteger(s.days) || s.days<0 || s.days>6)) return {kind:'range',key:w.key};
      if (w.type === 'calendar' && (!Number.isInteger(s.holidayLookaheadDays) || s.holidayLookaheadDays<1 || s.holidayLookaheadDays>2147483647)) return {kind:'range',key:w.key};
      if ((w.type === 'rain' && s.locationSource === 'fixed') || (w.type === 'air' && s.source === 'model' && s['location-mode'] === 'manual'))
        if (!Number.isFinite(s.latitude) || Math.abs(s.latitude)>90 || !Number.isFinite(s.longitude) || Math.abs(s.longitude)>180) return {kind:'coordinates',key:w.key};
    }
    return null;
  }
  function move(items, from, to) { const result=[...items]; if(from<0||from>=items.length||to<0||to>=items.length)return result; result.splice(to,0,result.splice(from,1)[0]); return result; }
  function newKey(type, widgets, instances={}) {
    const base='widget:'+type;
    if (!widgets.some(w=>w.key===base) && !Object.hasOwn(instances,base)) return base;
    for(let i=2;i<1000000;i++){const key=base+':'+i;if(!widgets.some(w=>w.key===key)&&!Object.hasOwn(instances,key))return key;}
    throw new Error('No free instance');
  }
  function defaults(type) {
    if(type==='world_clock')return window.PhoneClock.defaults();
    if(type==='rain') return {locationSource:'device',name:'',label:'',hours:24,days:6,inches:false,animation:true};
    if(type==='air') return {source:'model','location-mode':'auto','location-name':'',station:'114'};
    if(type==='calendar')return {enabled:['thai','flowers','buddhist','spain','usa','china'],holidayLookaheadDays:30,flowerWeeks:'BOTH',buddhistWeeks:'BOTH',bangkok:false,thaiNames:false,chinaWorkingDays:false,spanishNational:true,catalunya:true,tarragona:true,tivissa:true};
    return {};
  }
  function freePosition(draft,w,start=0) {
    for(let page=start;page<16;page++)for(let row=0;row<=draft.rows-w.height;row++)for(let column=0;column<=draft.columns-w.width;column++) {
      const next={...w,page,row,column};if(validPosition(draft,next))return next;
    }
    throw new Error('noRoom');
  }
  function add(draft,type,instances={}) {
    if(['calendar','favorites'].includes(type)){const existing=draft.widgets.find(w=>w.type===type);if(existing)return existing;}
    const key=['calendar','favorites'].includes(type)?'widget:'+type:newKey(type,draft.widgets,instances);
    const size=type==='world_clock'?window.PhoneClock.defaultSize():{width:type==='calendar'?2:1,height:1};
    const w=freePosition(draft,{key,type,width:Math.min(draft.columns,size.width),height:Math.min(draft.rows,size.height),settings:defaults(type)});draft.widgets.push(w);return w;
  }
  function recommended(draft,instances={},chosen={}) {
    const result=clone(draft); result.columns=8; result.rows=4;
    const selected=['rain','calendar','air','favorites'].map(type => result.widgets.find(w=>w.key===chosen[type]) || result.widgets.find(w=>w.type===type)
      || {key:['calendar','favorites'].includes(type)?'widget:'+type:newKey(type,result.widgets,instances),type,settings:defaults(type)});
    const shapes=[[0,0,8,1],[0,1,5,2],[5,1,3,2],[0,3,8,1]],extras=result.widgets.filter(w=>!selected.some(s=>s.key===w.key));
    result.widgets=selected.map((w,i)=>({...w,page:0,column:shapes[i][0],row:shapes[i][1],width:shapes[i][2],height:shapes[i][3]}));
    for(const w of extras)result.widgets.push(freePosition(result,{...w,width:Math.min(w.width,8),height:Math.min(w.height,4)},1));
    return result;
  }
  function rebase(baseState, local, latest) {
    const base=baseState.draft,remote=latest.draft,merged=clone(remote),conflicts=[];
    const resolve=(b,l,r,conflict,apply)=>{if(same(l,b)||same(l,r))return;if(same(r,b))apply(l);else conflicts.push({...conflict,mine:clone(l),tv:clone(r)});};
    resolve(pick(base,['columns','rows']),pick(local,['columns','rows']),pick(remote,['columns','rows']),{kind:'grid'},v=>Object.assign(merged,v));
    for(const field of ['artwork','favorites'])resolve(base[field],local[field],remote[field],{kind:field},v=>merged[field]=clone(v));
    const keys=new Set([...base.widgets,...local.widgets].map(w=>w.key));
    for(const key of keys){
      const b=base.widgets.find(w=>w.key===key),l=local.widgets.find(w=>w.key===key),r=remote.widgets.find(w=>w.key===key);
      if(same(l,b))continue;
      const epochChanged=b&&l&&r&&(baseState.instances?.[key]||0)!==(latest.instances?.[key]||0);
      if(!b||!l||!r||epochChanged||b.type!==r.type){
        if(epochChanged)conflicts.push({kind:'instance',key,mine:clone(l),tv:clone(r)});
        else resolve(b,l,r,{kind:'widget',key},v=>{merged.widgets=merged.widgets.filter(w=>w.key!==key);if(v)merged.widgets.push(clone(v));});
        continue;
      }
      const target=merged.widgets.find(w=>w.key===key);
      resolve(pick(b,placement),pick(l,placement),pick(r,placement),{kind:'placement',key},v=>Object.assign(target,v));
      for(const group of settingsGroups(l))resolve(pick(b.settings,group.fields),pick(l.settings,group.fields),pick(r.settings,group.fields),
        {kind:'setting',key,group:group.name,fields:group.fields},v=>{for(const k of group.fields)delete target.settings[k];Object.assign(target.settings,clone(v));});
    }
    return {draft:merged,conflicts};
  }
  function resolveConflict(draft,conflict,choice,instances={}) {
    if(choice==='tv')return;
    const value=clone(conflict.mine),target=draft.widgets.find(w=>w.key===conflict.key);
    if(conflict.kind==='grid'){Object.assign(draft,value);return;}
    if(conflict.kind==='artwork'||conflict.kind==='favorites'){draft[conflict.kind]=value;return;}
    if(conflict.kind==='instance'&&value&&['rain','air','world_clock'].includes(value.type)){
      value.key=newKey(value.type,draft.widgets,instances);draft.widgets.push(freePosition(draft,value));return;
    }
    if(conflict.kind==='widget'||conflict.kind==='instance'){
      draft.widgets=draft.widgets.filter(w=>w.key!==conflict.key);if(value)draft.widgets.push(value);return;
    }
    if(!target)return;
    if(conflict.kind==='placement')Object.assign(target,value);
    else {for(const k of conflict.fields)delete target.settings[k];Object.assign(target.settings,value);}
  }
  return {clone,same,diff,validPosition,validate,move,add,newKey,recommended,rebase,resolveConflict};
})();
