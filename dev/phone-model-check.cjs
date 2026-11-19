/* Pure model and translation contracts; run inside the browser/build container. */
const fs=require('fs'),vm=require('vm'),assert=require('assert');
const scope={window:{}};vm.createContext(scope);for(const file of ['model.js','strings.js','clock.js','grocery.js'])vm.runInContext(fs.readFileSync('launcher/app/src/main/assets/phone/'+file,'utf8'),scope);
scope.window.PhoneClock.configureDefaults(require('./phone-fixture.cjs').worldClockDefaults);
const M=scope.window.PhoneModel,copy=v=>JSON.parse(JSON.stringify(v));
const draft={headerClock:copy(require('./world-clock-default-fixture.json').settings),columns:8,rows:4,artwork:'small',favorites:['a','b','c'],widgets:[]};
M.add(draft,'rain');M.add(draft,'calendar');M.add(draft,'air');M.add(draft,'favorites');
assert.equal(M.validate(draft),null);assert.deepEqual(copy(M.move(draft.favorites,0,2)),['b','c','a']);assert.deepEqual(draft.favorites,['a','b','c']);
const base={draft:copy(draft),instances:{'widget:rain':1}},local=copy(draft),remote=copy(base);
local.widgets[0].settings.hours=48;remote.draft.widgets[0].settings.days=3;remote.draft.artwork='banner';
let merged=M.rebase(base,local,remote);assert.equal(merged.conflicts.length,0);assert.equal(merged.draft.widgets[0].settings.hours,48);assert.equal(merged.draft.widgets[0].settings.days,3);assert.equal(merged.draft.artwork,'banner');
remote.draft.widgets[0].settings.hours=12;merged=M.rebase(base,local,remote);assert.equal(merged.conflicts.length,1);assert.equal(merged.conflicts[0].group,'hours');M.resolveConflict(merged.draft,merged.conflicts[0],'mine');assert.equal(merged.draft.widgets[0].settings.hours,48);
remote.instances['widget:rain']=3;merged=M.rebase(base,local,remote);assert.equal(merged.conflicts[0].kind,'instance');M.resolveConflict(merged.draft,merged.conflicts[0],'mine',remote.instances);assert.equal(merged.draft.widgets.filter(w=>w.type==='rain').length,2);assert.equal(merged.draft.widgets.find(w=>w.key==='widget:rain').settings.hours,12);assert.equal(M.validate(merged.draft),null);
const removed=copy(base);removed.draft.widgets.shift();merged=M.rebase(base,local,removed);assert.equal(merged.conflicts[0].kind,'widget');assert.equal(merged.draft.widgets.length,3);
const fixed=copy(draft);fixed.widgets[0].settings={...fixed.widgets[0].settings,locationSource:'fixed',latitude:100,longitude:10};assert.equal(M.validate(fixed).kind,'coordinates');
const overlap=copy(draft);overlap.widgets[1].column=0;overlap.widgets[1].row=0;assert.equal(M.validate(overlap).kind,'placement');assert.equal(M.validPosition(draft,{...draft.widgets[0],column:-1}),false);
const extra=copy(draft);M.add(extra,'rain');const recommended=M.recommended(extra);assert.equal(recommended.widgets.length,5);assert.equal(recommended.widgets[4].page,1);assert.equal(M.validate(recommended),null);assert.deepEqual(copy(recommended.favorites),draft.favorites);
assert.equal(M.diff(draft,local).length,1);assert.equal(M.diff(draft,local)[0].group,'hours');
assert.deepEqual(Object.keys(scope.window.PhoneStrings.en).sort(),Object.keys(scope.window.PhoneStrings.th).sort());
for(const key of Object.keys(scope.window.PhoneStrings.en)){const placeholders=s=>(s.match(/\{\w+\}/g)||[]).sort();assert.deepEqual(placeholders(scope.window.PhoneStrings.en[key]),placeholders(scope.window.PhoneStrings.th[key]),key);}
const clockDraft=copy(draft);M.add(clockDraft,'world_clock');assert.equal(M.validate(clockDraft),null);const clockBase={draft:copy(clockDraft),instances:{}},clockMine=copy(clockDraft),clockLatest=copy(clockBase);clockMine.widgets.at(-1).settings.primary='some-other-place';clockLatest.draft.widgets.at(-1).settings.places.pop();const clockConflict=M.rebase(clockBase,clockMine,clockLatest);assert.equal(clockConflict.conflicts[0].group,'clockPlacesChanged');
console.log('Phone model: draft isolation, collision/range validation, three-way merge/conflict, instance replacement, recommended extras, semantic review, EN/TH parity passed');
