'use strict';
/* World-clock form for the dashboard-first editor. No transport or live TV mutations. */
window.PhoneClock = (() => {
  const translations={
    en:{world_clock:'World clock',clockTitle:'Widget title',clockPlaces:'Places',clockPrimary:'Primary place',clockReference:'Reference clock',clockFollowTV:'Local · Follow TV',clockFormat:'Time format',clock24:'24-hour',clock12:'12-hour · AM/PM',clockDeviceFormat:'Follow device format',clockOrder:'Place order',clockManual:'Manual order',clockWest:'West → east',clockSeconds:'Seconds in full screen',clockSearch:'Search city, code or named time zone',clockAdd:'Add place',clockStudio:'Add Studio cities (5)',clockZone:'Time zone',clockReplace:'Change zone',clockNoPosition:'No map position',clockLimit:'Up to 12 places per widget.',clockRemoveReplacement:'Removing this place sets primary to {name} and its reference to Follow TV, if needed.',clockHelp:'All clocks use the TV system clock and named-zone rules. The phone location is not used.',clockCount:'{count} places',clockMoveHelp:'Moving a place selects Manual order.',clockPlacesChanged:'Places and reference clocks',clockDaytime:'Sun/moon means 06:00–18:00 local clock hours, not sunrise/sunset.',clockManualZone:'Use zone only · no map position',clockUnavailable:'Time zone unavailable',clockCurrent:'TV clock snapshot: {time}',clockNoMatches:'No matching TV time zones. Try a city, code or zone ID.'},
    th:{world_clock:'นาฬิกาโลก',clockTitle:'ชื่อวิดเจ็ต',clockPlaces:'สถานที่',clockPrimary:'สถานที่หลัก',clockReference:'นาฬิกาอ้างอิง',clockFollowTV:'เวลาท้องถิ่น · ตามทีวี',clockFormat:'รูปแบบเวลา',clock24:'24 ชั่วโมง',clock12:'12 ชั่วโมง · AM/PM',clockDeviceFormat:'ตามรูปแบบอุปกรณ์',clockOrder:'ลำดับสถานที่',clockManual:'ลำดับที่กำหนดเอง',clockWest:'ตะวันตก → ตะวันออก',clockSeconds:'วินาทีในโหมดเต็มหน้าจอ',clockSearch:'ค้นหาเมือง รหัส หรือชื่อเขตเวลา',clockAdd:'เพิ่มสถานที่',clockStudio:'เพิ่มเมืองสตูดิโอ (5 เมือง)',clockZone:'เขตเวลา',clockReplace:'เปลี่ยนเขตเวลา',clockNoPosition:'ไม่มีตำแหน่งบนแผนที่',clockLimit:'แต่ละวิดเจ็ตมีได้ไม่เกิน 12 สถานที่',clockRemoveReplacement:'เมื่อนำสถานที่นี้ออก จะใช้ {name} เป็นสถานที่หลัก และตั้งเวลาอ้างอิงเป็นตามทีวีหากจำเป็น',clockHelp:'ทุกนาฬิกาใช้นาฬิการะบบและกฎเขตเวลาของทีวี ไม่ใช้ตำแหน่งโทรศัพท์',clockCount:'{count} สถานที่',clockMoveHelp:'การย้ายสถานที่จะเลือกลำดับที่กำหนดเอง',clockPlacesChanged:'สถานที่และนาฬิกาอ้างอิง',clockDaytime:'ดวงอาทิตย์และดวงจันทร์หมายถึงเวลา 06:00–18:00 ท้องถิ่น ไม่ใช่เวลาพระอาทิตย์ขึ้นหรือตก',clockManualZone:'ใช้เฉพาะเขตเวลา · ไม่มีตำแหน่งแผนที่',clockUnavailable:'เขตเวลาไม่พร้อมใช้งาน',clockCurrent:'ภาพเวลาจากทีวี: {time}',clockNoMatches:'ไม่พบเขตเวลาบนทีวี ลองชื่อเมือง รหัส หรือชื่อเขตเวลา'}
  };
  function registerStrings(){for(const lang of ['en','th'])if(window.PhoneStrings?.[lang])Object.assign(window.PhoneStrings[lang],translations[lang]);}
  registerStrings();
  const searches=new Map(),expanded=new Set(),orderUndo=new Map();
  let preset=null;
  function configureDefaults(value){
    if(!value||!Number.isInteger(value.width)||!Number.isInteger(value.height)||value.width<1||value.width>12||value.height<1||value.height>12||!valid(value.settings))throw new Error('Invalid TV clock defaults');
    preset=JSON.parse(JSON.stringify(value));
  }
  function defaultSize(){if(!preset)throw new Error('TV clock defaults unavailable');return {width:preset.width,height:preset.height};}
  function defaults(){if(!preset)throw new Error('TV clock defaults unavailable');return JSON.parse(JSON.stringify(preset.settings));}
  function valid(s){
    if(!s||typeof s.title!=='string'||s.title.length>120||!['manual','west'].includes(s.ordering)||!['24','12','device'].includes(s.format)||typeof s.seconds!=='boolean'||!Array.isArray(s.places)||s.places.length<1||s.places.length>12)return false;
    const ids=new Set();
    for(const p of s.places){
      if(typeof p.id!=='string'||!p.id.match(/^[A-Za-z0-9_-]{1,48}$/)||ids.has(p.id))return false;ids.add(p.id);
      if(typeof p.label!=='string'||p.label.length>120||typeof p.zone!=='string'||!p.zone||p.zone.length>120||typeof p.country!=='string'||p.country.length>80||typeof p.code!=='string'||p.code.length>8)return false;
      if(Object.hasOwn(p,'latitude')||Object.hasOwn(p,'longitude'))if(!Number.isFinite(p.latitude)||Math.abs(p.latitude)>90||!Number.isFinite(p.longitude)||Math.abs(p.longitude)>180)return false;
    }
    return ids.has(s.primary)&&(s.reference===''||ids.has(s.reference));
  }
  function summary(s,t){return s.places.map(p=>p.label|| (p.zone==='device'?t('clockFollowTV'):p.zone)).join(', ');}
  function form(parent,w,ui){
    registerStrings();const {state,el,button,field,select,details,note,t,changed,render,uid,language}=ui,s=w.settings;
    const name=p=>p.label||(p.zone==='device'?t('clockFollowTV'):p.zone),catalog=state.worldClockCatalog||[];
    const names=s.places.map(p=>[p.id,name(p)]);
    field(parent,t('clockTitle'),s.title,v=>s.title=v,'text',{maxLength:120});
    note(parent,t('clockHelp'));
    select(parent,t('clockPrimary'),s.primary,names,value=>s.primary=value);
    select(parent,t('clockReference'),s.reference,[['',t('clockFollowTV')],...names],value=>s.reference=value);
    const places=el('section');places.append(el('h3',t('clockPlaces')));parent.append(places);
    const list=el('ol',undefined,'favorite-list clock-place-list');list.dataset.clockKey=w.key;places.append(list);
    const focusGrip=id=>requestAnimationFrame(()=>{[...document.querySelectorAll('.clock-place-list')].find(node=>node.dataset.clockKey===w.key)?.querySelectorAll('[data-order-key]').forEach(row=>{if(row.dataset.orderKey===id)row.querySelector('[data-reorder]').focus({preventScroll:true});});});
    const reorder=(from,to,id)=>{if(from===to)return;const before=s.places.map(p=>p.id),ordering=s.ordering;s.places.splice(to,0,s.places.splice(from,1)[0]);s.ordering='manual';orderUndo.set(w.key,{before,after:s.places.map(p=>p.id),ordering});changed();render();focusGrip(id);ui.announce?.(t('moved',{name:name(s.places[to]),position:to+1,count:s.places.length}));};
    for(const [i,p]of s.places.entries()){
      const row=el('li',undefined,'favorite-row');row.dataset.orderKey=p.id;list.append(row);
      const handle=button('⠿',()=>{},'reorder-grip');handle.dataset.reorder='true';handle.setAttribute('aria-label',t('dragApp',{name:name(p)}));row.append(handle);
      const card=details(row,name(p),expanded.has(w.key+':'+p.id));card.style.flex='1';card.style.minWidth='0';card.addEventListener('toggle',()=>{if(card.open)expanded.add(w.key+':'+p.id);else expanded.delete(w.key+':'+p.id);});
      note(card,p.zone==='device'?t('clockFollowTV'):p.zone);
      if(!Object.hasOwn(p,'latitude'))note(card,t('clockNoPosition'));
      if(state.clockNow){try{const zone=p.zone==='device'?state.clockZone:p.zone;const twelve=s.format==='12'||s.format==='device'&&!state.clock24Hour;const time=new Intl.DateTimeFormat(language,{calendar:language==='th'?'buddhist':'gregory',timeZone:zone,dateStyle:'medium',timeStyle:'short',hourCycle:twelve?'h12':'h23'}).format(new Date(state.clockNow));note(card,t('clockCurrent',{time:'\u23f2\ufe0e '+time}));}catch(_){note(card,t('clockUnavailable'));}}
      field(card,t('label'),p.label,value=>p.label=value,'text',{maxLength:120});
      card.append(button(t('clockReplace'),()=>{searches.set(w.key,{query:'',editing:p.id});render();}));
      const moves=el('div',undefined,'two-fields');
      for(const [delta,label]of [[-1,'up'],[1,'down']]){const move=button(t(label),()=>reorder(i,i+delta,p.id));move.disabled=i+delta<0||i+delta>=s.places.length;moves.append(move);}card.append(moves);
      const replacement=s.places.find(other=>other.id!==p.id);
      if((p.id===s.primary||p.id===s.reference)&&replacement)note(card,t('clockRemoveReplacement',{name:name(replacement)}));
      const remove=button(t('remove'),()=>{s.places=s.places.filter(other=>other.id!==p.id);if(s.primary===p.id)s.primary=s.places[0].id;if(s.reference===p.id)s.reference='';changed();render();},'danger quiet');remove.disabled=s.places.length===1;card.append(remove);
    }
    if(window.PhoneDrag){const dispose=PhoneDrag.orderedList(list,{drop:reorder,actions:id=>{const row=[...list.querySelectorAll('[data-order-key]')].find(row=>row.dataset.orderKey===id);if(row){const detail=row.querySelector('details');detail.open=!detail.open;detail.querySelector('summary').focus();}},announce:(kind,id)=>ui.announce?.(t(kind==='start'?'dragStart':'dragCancel',{name:name(s.places.find(p=>p.id===id)||s.places[0])}))});ui.disposers?.push(dispose);}
    const undo=orderUndo.get(w.key);if(undo&&JSON.stringify(undo.after)===JSON.stringify(s.places.map(p=>p.id)))places.append(button(t('undoReorder'),()=>{const byId=new Map(s.places.map(p=>[p.id,p]));s.places=undo.before.map(id=>byId.get(id));s.ordering=undo.ordering;orderUndo.delete(w.key);changed();render();}));
    note(places,t('clockMoveHelp'));note(places,t('clockLimit'));
    const add=button(t('clockAdd'),()=>{searches.set(w.key,{query:'',editing:null});render();});add.disabled=s.places.length>=12;places.append(add);
    const studioNames=['Bangkok','Seattle','Denver','Munich','Barcelona'];
    const studio=button(t('clockStudio'),()=>{for(const label of studioNames){const source=catalog.find(p=>p.label===label);if(source)s.places.push({...source,id:uid().slice(0,40)});}changed();render();});studio.disabled=s.places.length+5>12||studioNames.some(label=>!catalog.some(p=>p.label===label));places.append(studio);
    const searchState=searches.get(w.key);
    if(searchState){
      const box=el('section');box.append(el('h3',t(searchState.editing?'clockReplace':'clockAdd')));parent.append(box);
      const query=field(box,t('clockSearch'),searchState.query,value=>searchState.query=value,'search',{maxLength:120}),results=el('div');box.append(results);
      const pick=source=>{const chosen={...source,id:searchState.editing||uid().slice(0,40)};
        if(searchState.editing){const p=s.places.find(p=>p.id===searchState.editing);if(p){chosen.label=p.label;Object.assign(p,chosen);if(!Object.hasOwn(chosen,'latitude')){delete p.latitude;delete p.longitude;}}}
        else if(s.places.length<12)s.places.push(chosen);
        searches.delete(w.key);expanded.add(w.key+':'+chosen.id);changed();render();};
      function filter(){
        searchState.query=query.value;results.replaceChildren(button(t('clockFollowTV'),()=>pick({label:'',zone:'device',country:'',code:''})));
        const q=query.value.toLocaleLowerCase().trim().replaceAll('_',' '),matches=catalog.filter(p=>[p.label,p.zone,p.country,p.code].join(' ').toLocaleLowerCase().replaceAll('_',' ').includes(q));
        for(const p of matches.slice(0,12))results.append(button((p.label||p.zone)+' · '+p.zone,()=>pick(p),'wide-action'));
        const exact=catalog.find(p=>p.zone.toLocaleLowerCase().replaceAll('_',' ')===q);
        if(exact)results.append(button(t('clockManualZone')+' · '+exact.zone,()=>pick({label:exact.zone,zone:exact.zone,country:'',code:''}),'wide-action'));
        if(!matches.length)note(results,t('clockNoMatches'));
      }
      query.addEventListener('input',filter);filter();box.append(button(t('back'),()=>{searches.delete(w.key);render();}));
    }
    const display=details(parent,t('more'));
    select(display,t('clockFormat'),s.format,[['24',t('clock24')],['12',t('clock12')],['device',t('clockDeviceFormat')]],value=>s.format=value);
    select(display,t('clockOrder'),s.ordering,[['manual',t('clockManual')],['west',t('clockWest')]],value=>s.ordering=value);
    field(display,t('clockSeconds'),s.seconds,value=>s.seconds=value,'checkbox');note(display,t('clockDaytime'));
  }
  return {configureDefaults,defaultSize,defaults,valid,summary,form,registerStrings,translations};
})();
