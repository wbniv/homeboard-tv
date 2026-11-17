const initial={tv:'Living room TV · ab12',language:'en',revision:7,busy:false,undo:'',stations:[{id:'114',name:'Lumpini Park'},{id:'119',name:'Bang Na'}],apps:[{key:'app:video/Main',name:'Video'},{key:'app:music/Main',name:'Music'},{key:'app:new/Main',name:'<b>New app</b>'}],draft:{columns:8,rows:4,artwork:'small',favorites:['app:video/Main','app:music/Main'],widgets:[
{key:'widget:rain',type:'rain',page:0,column:0,row:0,width:8,height:1,settings:{locationSource:'device',name:'',label:'',hours:24,days:6,inches:false,animation:true}},
{key:'widget:calendar',type:'calendar',page:0,column:0,row:1,width:5,height:2,settings:{enabled:['thai','flowers','buddhist','spain','usa','china'],holidayLookaheadDays:30,flowerWeeks:'BOTH',buddhistWeeks:'BOTH',bangkok:false,thaiNames:false,chinaWorkingDays:false,spanishNational:true,catalunya:true,tarragona:true,tivissa:true}},
{key:'widget:air',type:'air',page:0,column:5,row:1,width:3,height:2,settings:{source:'airbkk','location-mode':'auto','location-name':'Lumpini Park',station:'114'}},
{key:'widget:favorites',type:'favorites',page:0,column:0,row:3,width:8,height:1,settings:{}}]}};
initial.worldClockDefaults=require('./world-clock-default-fixture.json');
module.exports=initial;
