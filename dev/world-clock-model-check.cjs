/* Container-only clock draft identity/recovery checks using the real editor model. */
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const scope={window:{}};vm.createContext(scope);
for(const file of ['strings.js','clock.js','model.js'])vm.runInContext(fs.readFileSync('launcher/app/src/main/assets/phone/'+file,'utf8'),scope);
const M=scope.window.PhoneModel,C=scope.window.PhoneClock,copy=x=>JSON.parse(JSON.stringify(x));
const draft={columns:8,rows:4,artwork:'small',favorites:[],widgets:[]};
const widget=M.add(draft,'world_clock');assert(C.valid(widget.settings));assert.equal(M.validate(draft),null);
const base={draft:copy(draft),instances:{[widget.key]:1}},local=copy(draft),remote=copy(base);
local.widgets[0].settings.places[0].label='Family';remote.draft.widgets[0].settings.format='12';
let result=M.rebase(base,local,remote);assert.equal(result.conflicts.length,0);assert.equal(result.draft.widgets[0].settings.format,'12');assert.equal(result.draft.widgets[0].settings.places[0].label,'Family');assert.equal(M.validate(result.draft),null);
remote.draft.widgets[0].settings.places.push({id:'bkk',label:'Bangkok',zone:'Asia/Bangkok',country:'TH',code:'BKK'});remote.draft.widgets[0].settings.primary='bkk';remote.draft.widgets[0].settings.reference='bkk';
result=M.rebase(base,local,remote);assert.equal(result.conflicts.length,1);assert.equal(result.conflicts[0].group,'clockPlacesChanged');
M.resolveConflict(result.draft,result.conflicts[0],'mine');assert.equal(result.draft.widgets[0].settings.primary,'local');assert.equal(result.draft.widgets[0].settings.reference,'');assert.equal(M.validate(result.draft),null);
remote.instances[widget.key]=2;result=M.rebase(base,local,remote);assert.equal(result.conflicts[0].kind,'instance');
M.resolveConflict(result.draft,result.conflicts[0],'mine',remote.instances);assert.equal(result.draft.widgets.length,2);assert.notEqual(result.draft.widgets[0].key,result.draft.widgets[1].key);assert.equal(result.draft.widgets[0].settings.primary,'bkk');assert.equal(result.draft.widgets[1].settings.places[0].label,'Family');assert.equal(M.validate(result.draft),null);
const bad=C.defaults();bad.places.push(copy(bad.places[0]));assert.equal(C.valid(bad),false);assert.equal(M.validate({...draft,widgets:[{...widget,settings:bad}]}).kind,'range');
assert.deepEqual(Object.keys(C.translations.en).sort(),Object.keys(C.translations.th).sort());
console.log('World clock: draft defaults, atomic places/primary/reference recovery, independent display merge, instance replacement, validation and translation parity passed');
