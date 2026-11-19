/* Container-only clock draft identity/recovery checks using the real editor model. */
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const scope={window:{}};vm.createContext(scope);
for(const file of ['strings.js','clock.js','grocery.js','model.js'])vm.runInContext(fs.readFileSync('launcher/app/src/main/assets/phone/'+file,'utf8'),scope);
const M=scope.window.PhoneModel,C=scope.window.PhoneClock,copy=x=>JSON.parse(JSON.stringify(x));
C.configureDefaults(require('./phone-fixture.cjs').worldClockDefaults);
const draft={headerClock:copy(C.defaults()),columns:8,rows:4,artwork:'small',favorites:[],widgets:[]};
const widget=M.add(draft,'world_clock');assert.equal(widget.width,8);assert.equal(widget.height,1);assert(C.valid(widget.settings));assert.equal(M.validate(draft),null);
const base={draft:copy(draft),instances:{[widget.key]:1}},local=copy(draft),remote=copy(base);
local.widgets[0].settings.places[0].label='Family';remote.draft.widgets[0].settings.format='12';
let result=M.rebase(base,local,remote);assert.equal(result.conflicts.length,0);assert.equal(result.draft.widgets[0].settings.format,'12');assert.equal(result.draft.widgets[0].settings.places[0].label,'Family');assert.equal(M.validate(result.draft),null);
remote.draft.widgets[0].settings.places.push({id:'extra-bkk',label:'Bangkok',zone:'Asia/Bangkok',country:'TH',code:'BKK'});remote.draft.widgets[0].settings.primary='extra-bkk';remote.draft.widgets[0].settings.reference='extra-bkk';
result=M.rebase(base,local,remote);assert.equal(result.conflicts.length,1);assert.equal(result.conflicts[0].group,'clockPlacesChanged');
M.resolveConflict(result.draft,result.conflicts[0],'mine');assert.equal(result.draft.widgets[0].settings.primary,'bkk');assert.equal(result.draft.widgets[0].settings.reference,'');assert.equal(M.validate(result.draft),null);
remote.instances[widget.key]=2;result=M.rebase(base,local,remote);assert.equal(result.conflicts[0].kind,'instance');
M.resolveConflict(result.draft,result.conflicts[0],'mine',remote.instances);assert.equal(result.draft.widgets.length,2);assert.notEqual(result.draft.widgets[0].key,result.draft.widgets[1].key);assert.equal(result.draft.widgets[0].settings.primary,'extra-bkk');assert.equal(result.draft.widgets[1].settings.places[0].label,'Family');assert.equal(M.validate(result.draft),null);
const bad=C.defaults();bad.places.push(copy(bad.places[0]));assert.equal(C.valid(bad),false);assert.equal(M.validate({...draft,widgets:[{...widget,settings:bad}]}).kind,'range');
assert.deepEqual(Object.keys(C.translations.en).sort(),Object.keys(C.translations.th).sort());
const beforeDefaults=copy(widget);const nextPreset=copy(require('./phone-fixture.cjs').worldClockDefaults);nextPreset.settings.format='12';nextPreset.width=6;C.configureDefaults(nextPreset);nextPreset.settings.places[0].label='Do not share mutable data';const later=M.add(draft,'world_clock');assert.equal(later.width,6);assert.equal(later.settings.format,'12');assert.equal(later.settings.places[0].label,'Bangkok');assert.deepEqual(copy(draft.widgets[0]),beforeDefaults);const small={columns:4,rows:2,artwork:'small',favorites:[],widgets:[]};assert.equal(M.add(small,'world_clock').width,4);
console.log('World clock: draft defaults, atomic places/primary/reference recovery, independent display merge, instance replacement, validation and translation parity passed');

const fixedBase=copy(require('./phone-fixture.cjs')),fixedLocal=copy(fixedBase.draft),fixedRemote=copy(fixedBase);
fixedLocal.headerClock.places[0].label='Home';fixedRemote.draft.headerClock.format='12';
let headerMerge=M.rebase(fixedBase,fixedLocal,fixedRemote);assert.equal(headerMerge.conflicts.length,0);assert.equal(headerMerge.draft.headerClock.places[0].label,'Home');assert.equal(headerMerge.draft.headerClock.format,'12');assert.equal(headerMerge.draft.widgets.length,4);assert.equal(M.diff(fixedBase.draft,fixedLocal)[0].widget.key,M.HEADER);
fixedRemote.draft.headerClock.primary='sea';headerMerge=M.rebase(fixedBase,fixedLocal,fixedRemote);assert.equal(headerMerge.conflicts.length,1);M.resolveConflict(headerMerge.draft,headerMerge.conflicts[0],'mine');assert.equal(headerMerge.draft.headerClock.primary,'bkk');assert.equal(M.validate(headerMerge.draft),null);
