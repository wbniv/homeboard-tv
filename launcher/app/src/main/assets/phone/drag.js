'use strict';
window.PhoneDrag = (() => {
  function orderedList(list, {drop, actions, announce}) {
    const cleanups=[];const handles=[...list.querySelectorAll('[data-reorder]')];
    for(const handle of handles){
      let drag=null,frame=0,suppressClick=false;
      const clean=()=>{cancelAnimationFrame(frame);frame=0;if(!drag)return;drag.ghost?.remove();drag.marker?.remove();for(const row of drag.rows){row.style.transform='';row.classList.remove('drag-origin');}drag=null;};
      cleanups.push(clean);
      const update=()=>{
        if(!drag?.active)return;
        for(const row of drag.rows)row.style.transform='';
        const rects=drag.rows.map(row=>row.getBoundingClientRect());
        let destination=rects.filter(r=>drag.y>r.top+r.height/2).length;
        drag.to=Math.max(0,Math.min(rects.length-1,destination>drag.from?destination-1:destination));
        const height=drag.height;
        for(let i=0;i<drag.rows.length;i++){
          if(i>drag.from&&i<=drag.to)drag.rows[i].style.transform=`translateY(${-height}px)`;
          if(i>=drag.to&&i<drag.from)drag.rows[i].style.transform=`translateY(${height}px)`;
        }
        drag.ghost.style.transform=`translate(${drag.x-drag.offsetX}px,${drag.y-drag.offsetY}px)`;
        const boundary=drag.to<drag.from?rects[drag.to].top:rects[drag.to].bottom;
        drag.marker.style.top=`${boundary}px`;
        drag.marker.style.left=`${list.getBoundingClientRect().left}px`;
        drag.marker.style.width=`${list.getBoundingClientRect().width}px`;
      };
      const scroll=()=>{
        if(!drag?.active)return;
        const edge=70,bottom=window.innerHeight-95;
        let delta=drag.y<edge?-Math.min(14,(edge-drag.y)/4):drag.y>bottom?Math.min(14,(drag.y-bottom)/4):0;
        if(delta){window.scrollBy(0,delta);update();}
        frame=requestAnimationFrame(scroll);
      };
      handle.addEventListener('pointerdown',event=>{
        if(!event.isPrimary||event.button!==0)return;
        suppressClick=false;const row=handle.closest('[data-order-key]'),rows=[...list.querySelectorAll('[data-order-key]')],rect=row.getBoundingClientRect();
        drag={rows,row,from:rows.indexOf(row),to:rows.indexOf(row),key:row.dataset.orderKey,startX:event.clientX,startY:event.clientY,
          x:event.clientX,y:event.clientY,offsetX:event.clientX-rect.left,offsetY:event.clientY-rect.top,height:rect.height+8,active:false};
        handle.setPointerCapture(event.pointerId);
      });
      handle.addEventListener('pointermove',event=>{
        if(!drag)return;drag.x=event.clientX;drag.y=event.clientY;
        if(!drag.active&&Math.hypot(drag.x-drag.startX,drag.y-drag.startY)>=6){
          drag.active=true;const rect=drag.row.getBoundingClientRect();drag.ghost=drag.row.cloneNode(true);drag.ghost.className='favorite-row drag-ghost';
          drag.ghost.setAttribute('aria-hidden','true');drag.ghost.inert=true;drag.ghost.style.width=`${rect.width}px`;
          drag.marker=document.createElement('div');drag.marker.className='insertion-marker';drag.marker.setAttribute('aria-hidden','true');
          document.body.append(drag.ghost,drag.marker);drag.row.classList.add('drag-origin');announce('start',drag.key);scroll();
        }
        update();
      });
      handle.addEventListener('pointerup',event=>{
        if(!drag)return;const rect=list.getBoundingClientRect(),{active,from,to,key}=drag;
        const inside=event.clientX>=rect.left-20&&event.clientX<=rect.right+20&&event.clientY>=0&&event.clientY<=window.innerHeight;
        suppressClick=active;setTimeout(()=>suppressClick=false,0);clean();if(active&&inside&&from!==to)drop(from,to,key);else if(active)announce('cancel',key);
      });
      handle.addEventListener('pointercancel',()=>{const key=drag?.key;suppressClick=true;clean();if(key)announce('cancel',key);});
      handle.addEventListener('lostpointercapture',()=>{if(drag){const key=drag.key;clean();announce('cancel',key);}});
      handle.addEventListener('click',()=>{if(!suppressClick)actions(handle.closest('[data-order-key]').dataset.orderKey);});
      handle.addEventListener('keydown',event=>{
        const row=handle.closest('[data-order-key]');
        if(event.altKey&&(event.key==='ArrowUp'||event.key==='ArrowDown')){event.preventDefault();const rows=[...list.querySelectorAll('[data-order-key]')],from=rows.indexOf(row),to=from+(event.key==='ArrowUp'?-1:1);if(to>=0&&to<rows.length)drop(from,to,row.dataset.orderKey);}
        else if(event.key==='Enter'||event.key===' '){event.preventDefault();actions(row.dataset.orderKey);}
      });
    }
    return ()=>cleanups.forEach(clean=>clean());
  }
  function spatial(handle,grid,{initial,candidate,valid,paint,commit,cancel}){
    let drag=null;
    handle.addEventListener('pointerdown',event=>{if(!event.isPrimary||event.button!==0)return;drag={x:event.clientX,y:event.clientY,original:initial(),active:false,next:null};handle.setPointerCapture(event.pointerId);});
    handle.addEventListener('pointermove',event=>{
      if(!drag)return;if(!drag.active&&Math.hypot(event.clientX-drag.x,event.clientY-drag.y)<6)return;drag.active=true;
      drag.next=candidate(drag.original,event.clientX-drag.x,event.clientY-drag.y,grid.getBoundingClientRect());paint(drag.next,valid(drag.next));
    });
    const finish=(event,abort)=>{
      if(!drag)return;const current=drag;drag=null;
      const rect=grid.getBoundingClientRect(),inside=event.clientX>=rect.left&&event.clientX<=rect.right&&event.clientY>=rect.top&&event.clientY<=rect.bottom;
      if(current.active&&!abort&&inside&&valid(current.next))commit(current.next);else cancel(current.original,current.active);
    };
    handle.addEventListener('pointerup',event=>finish(event,false));
    handle.addEventListener('pointercancel',event=>finish(event,true));
    handle.addEventListener('lostpointercapture',event=>{if(drag)finish(event,true);});
    return ()=>{drag=null;};
  }
  return {orderedList,spatial};
})();
