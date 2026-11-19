'use strict';
window.PhoneTimers={render(parent,c){
  const {state,el,button,note,field,t,api,uid,disposers}=c;
  const storeKey='homeboard-timers:'+state.tvId;
  let current=state.timers,closed=false,busy=false;if(current)current.received=performance.now();
  let pending=null;try{pending=JSON.parse(sessionStorage.getItem(storeKey)||'null');}catch(_){}
  note(parent,t('timerLive'));note(parent,t('timerSound'));
  const zone=el('p'),status=el('p'),retry=el('div'),rows=el('div');parent.append(zone,status,retry,rows);
  const controls=new Map();let version=-1;
  const clock=ms=>{const sec=Math.ceil(Math.max(0,ms)/1000);return (sec>=3600?Math.floor(sec/3600)+':':'')+String(Math.floor(sec/60)%60).padStart(2,'0')+':'+String(sec%60).padStart(2,'0');};
  function save(){try{if(pending)sessionStorage.setItem(storeKey,JSON.stringify(pending));else sessionStorage.removeItem(storeKey);}catch(_){} }
  function warning(){retry.replaceChildren();if(pending){note(retry,t('timerPending'));retry.append(button(t('timerRetry'),()=>send(null)));}}
  async function send(command){if(busy)return;if(pending&&command)throw new Error(t('timerPending'));if(command)pending={...command,operation:uid(),revision:current.revision};if(!pending)return;save();warning();busy=true;
    try{current=await api('timers',pending);current.received=performance.now();pending=null;save();update();}
    catch(e){if(e.status===422){pending=null;save();current=await api('timer-state');update();}throw e;}
    finally{busy=false;warning();}}
  function update(){if(closed||!current)return;zone.textContent=t('timerZone',{zone:current.zone});status.textContent=!current.alarmAccess?t('timerPermission'):current.alertError?t('timerAudioError'):'';
    if(version!==current.revision){const focus=document.activeElement?.dataset.timerAction;version=current.revision;rows.replaceChildren();controls.clear();
      if(!current.items.length)note(rows,t('timerEmpty'));
      for(const item of current.items){const card=el('section',undefined,'timer-card');card.append(el('h3',item.name||t(item.kind==='event'?'timerEvent':'timerCooking')));const label=el('p',undefined,'timer-remaining');card.append(label);controls.set(item.id,label);
        if(item.kind==='timer')note(card,t('timerOriginal',{time:clock(item.duration)}));
        const actions=el('div',undefined,'timer-actions');function action(key,labelKey){const b=button(t(labelKey),()=>send({action:key,id:item.id}));b.dataset.timerAction=item.id+':'+key;actions.append(b);}
        if(item.kind==='timer'){if(item.state==='running')action('pause','timerPause');if(item.state==='paused')action('resume','timerResume');if(['running','paused'].includes(item.state))action('add','timerAdd');if(item.state!=='running')action('restart','timerRestart');}
        action(item.state==='stopped'?'remove':'stop',item.state==='stopped'?'removeWidget':'timerStop');card.append(actions);rows.append(card);
      }if(focus)[...rows.querySelectorAll('button')].find(b=>b.dataset.timerAction===focus)?.focus({preventScroll:true});}
    const delta=performance.now()-(current.received||performance.now());
    for(const item of current.items){const label=controls.get(item.id);if(!label)continue;const stateText=t('timerState_'+item.state);
      if(item.kind==='event'){const locale=state.language==='th'?'th-TH':'en-GB';label.textContent=new Intl.DateTimeFormat(locale,{dateStyle:'medium',timeStyle:'short',timeZone:current.zone}).format(item.at)+' · '+stateText;}
      else label.textContent=(['running','paused'].includes(item.state)?'⏱ '+clock(item.state==='running'?item.deadline-current.elapsed-delta:item.remaining)+' · ':'')+stateText;}
    warning();}
  const silence=button(t('timerSilence'),()=>send({action:'silence'}));parent.append(silence);
  const form=el('section');form.append(el('h2',t('timerNew')));parent.append(form);let kind='timer',name='',minutes=5,seconds=0,local='';
  const choices=el('div',undefined,'timer-actions');for(const n of [1,3,5,10,15,20,30])choices.append(button(t('timerPreset',{minutes:n}),()=>send({action:'create',kind:'timer',name,duration:n*60000})));form.append(choices);
  field(form,t('timerName'),name,v=>name=v,'text',{maxLength:80});
  const select=el('select');select.setAttribute('aria-label',t('timerKind'));for(const [value,key]of [['timer','timerCooking'],['event','timerEvent']]){const o=el('option',t(key));o.value=value;select.append(o);}form.append(select);
  const duration=el('div'),event=el('div');form.append(duration,event);event.hidden=true;
  field(duration,t('timerMinutes'),minutes,v=>minutes=v,'number',{min:0,max:1440,step:1,required:true});field(duration,t('timerSeconds'),seconds,v=>seconds=v,'number',{min:0,max:59,step:1,required:true});
  const now=new Date(current.now+15*60000),parts=new Intl.DateTimeFormat('en-CA',{timeZone:current.zone,year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).formatToParts(now),part=k=>parts.find(p=>p.type===k).value;
  local=part('year')+'-'+part('month')+'-'+part('day')+'T'+part('hour')+':'+part('minute');
  field(event,t('timerLocal'),local,v=>local=v,'datetime-local',{required:true});note(event,t('timerDateHelp'));
  const start=button(t('timerStart'),()=>{if(kind==='timer'&&(!Number.isInteger(minutes)||!Number.isInteger(seconds)||minutes<0||minutes>1440||seconds<0||seconds>59))throw new Error(t('timerInvalid'));return send(kind==='timer'?{action:'create',kind,name,duration:minutes*60000+seconds*1000}:{action:'create',kind,name,local,zone:current.zone});},'primary');form.append(start);
  select.onchange=()=>{kind=select.value;duration.hidden=kind!=='timer';event.hidden=kind!=='event';start.textContent=t(kind==='event'?'timerSet':'timerStart');};
  async function poll(){if(closed)return;try{current=await api('timer-state');current.received=performance.now();update();}catch(e){status.textContent=t('timerDisconnected');}if(!closed)timer=setTimeout(poll,2000);}
  let timer=setTimeout(poll,2000);const ticker=setInterval(update,500);disposers.push(()=>{closed=true;clearTimeout(timer);clearInterval(ticker);});update();
}};
