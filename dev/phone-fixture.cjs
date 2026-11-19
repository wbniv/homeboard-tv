const initial={tv:'Living room TV · ab12',language:'en',revision:7,busy:false,undo:'',stations:[{id:'114',name:'Lumpini Park'},{id:'119',name:'Bang Na'}],apps:[{key:'app:video/Main',name:'Video'},{key:'app:music/Main',name:'Music'},{key:'app:new/Main',name:'<b>New app</b>'}],draft:{columns:8,rows:4,artwork:'small',favorites:['app:video/Main','app:music/Main'],widgets:[
{key:'widget:rain',type:'rain',page:0,column:0,row:0,width:8,height:1,settings:{locationSource:'device',name:'',label:'',hours:24,days:6,inches:false,animation:true}},
{key:'widget:calendar',type:'calendar',page:0,column:0,row:1,width:5,height:2,settings:{enabled:['thai','flowers','buddhist','spain','usa','china'],holidayLookaheadDays:30,flowerWeeks:'BOTH',buddhistWeeks:'BOTH',bangkok:false,thaiNames:false,chinaWorkingDays:false,spanishNational:true,catalunya:true,tarragona:true,tivissa:true}},
{key:'widget:air',type:'air',page:0,column:5,row:1,width:3,height:2,settings:{source:'airbkk','location-mode':'auto','location-name':'Lumpini Park',station:'114'}},
{key:'widget:favorites',type:'favorites',page:0,column:0,row:3,width:8,height:1,settings:{}}]}};
initial.providers=[
{id:'overflight',name:'Overflight',route:'Google Play',detail:'Earlier HD/SDR verified; full testing incomplete.',url:'https://play.google.com/store/apps/details?id=tv.projectivy.plugin.wallpaperprovider.overflight',play:true,excluded:false,installed:false,version:''},
{id:'daily-photos',name:'Daily Photos',route:'Advanced · Publisher page',detail:'Baseline passed with GitHub 1.10.',url:'https://github.com/spocky/projectivy-plugin-wallpaper-bingwallpaper/releases',play:false,excluded:false,installed:true,version:'1.10'},
{id:'weather',name:'Weather Wallpaper v2.11',route:'Excluded · No install action',detail:'Google Play Protect blocked v2.11.',url:'',play:false,excluded:true,installed:false,version:''}
];
initial.worldClockDefaults=require('./world-clock-default-fixture.json');
initial.draft.headerClock=JSON.parse(JSON.stringify(initial.worldClockDefaults.settings));
initial.draft.groceries={groceries:{id:'groceries',title:'',items:[],products:[]}};
module.exports=initial;
