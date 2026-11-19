const fs=require('fs'),vm=require('vm'),assert=require('assert');
const scope={window:{}};vm.createContext(scope);for(const f of ['model','grocery','strings','clock'])vm.runInContext(fs.readFileSync('launcher/app/src/main/assets/phone/'+f+'.js','utf8'),scope);
const G=scope.window.PhoneGrocery,M=scope.window.PhoneModel,copy=v=>JSON.parse(JSON.stringify(v));
const p={...G.product('milk','Milk'),staple:true,baseUnit:'ml',packageUnit:'carton',packageSize:'1000',tracking:'quantity',stock:'500',threshold:'1000',target:'3000',reported:'2026-11-19T02:10:00Z'};
assert.deepEqual(copy(G.suggestion(p)),{amount:'3',unit:'carton',deficit:'2500',expected:'3500'});assert.equal(G.format(G.base(p,'1.5','l'),1000000000000n),'1500');assert.throws(()=>G.base(p,'1','kg'));assert.throws(()=>G.base(p,'1','pack'));
assert.deepEqual(copy(G.increased(p,{structured:{amount:'1','unit':'l'}},{amount:'500',unit:'ml'})),{amount:'1500',unit:'ml'});
assert.equal(G.suggestion({...p,stock:'1000'}).amount,'2');assert.equal(G.suggestion({...p,stock:'1000.000001'}),null);assert.equal(G.suggestion({...p,stock:''}),null);assert.equal(G.suggestion({...p,packageSize:'',increment:'250'}).amount,'2500');
const book=G.defaults();book.groceries.products.push(p);book.groceries.items.push({...G.item('a','นม'),productId:'milk',structured:{amount:'3',unit:'carton'}});assert(G.valid(book));
const bad=copy(book);bad.groceries.products[0].stock='-1';assert(!G.valid(bad));bad.groceries.products[0].stock='500';bad.groceries.products[0].target='1000';assert(!G.valid(bad));
G.removeProduct(book.groceries,'milk');assert.equal(book.groceries.items[0].quantity,'3 carton');assert.equal(book.groceries.items[0].productId,'');assert(!book.groceries.items[0].structured);assert(G.valid(book));
const header=require('./world-clock-default-fixture.json').settings,draft={headerClock:copy(header),columns:8,rows:4,artwork:'small',favorites:[],widgets:[],groceries:G.defaults()};M.add(draft,'grocery');assert.equal(M.validate(draft),null);
const base={draft:copy(draft),instances:{}},local=copy(draft),remote=copy(base);local.groceries.groceries.items.push(G.item('a','Limes'));remote.draft.groceries.other=G.emptyList('other','Other');let merged=M.rebase(base,local,remote);assert.equal(merged.conflicts.length,0);assert.equal(merged.draft.groceries.groceries.items.length,1);assert(merged.draft.groceries.other);
remote.draft.groceries.groceries.items.push(G.item('b','Eggs'));merged=M.rebase(base,local,remote);assert.equal(merged.conflicts[0].kind,'grocery');M.resolveConflict(merged.draft,merged.conflicts[0],'mine');assert.equal(merged.draft.groceries.groceries.items[0].id,'a');
assert.equal(M.diff(base.draft,local)[0].kind,'grocery');const invalid=copy(draft);delete invalid.groceries.groceries;assert.equal(M.validate(invalid).kind,'grocery');
const unicode=G.defaults();unicode.groceries.items.push(G.item('a','🥛'.repeat(120)));assert(G.valid(unicode));unicode.groceries.items[0].name+='🥛';assert(!G.valid(unicode));
assert.deepEqual(Object.keys(scope.window.PhoneStrings.en).sort(),Object.keys(scope.window.PhoneStrings.th).sort());
console.log('Groceries: exact units, package rounding, unknown stock, manual reuse, free-text preservation, validation, per-list conflict and independent-list merge passed');

// Interrupted editors are recoverable but only a valid, owned proposal may publish.
const owner={tvId:'kitchen'},raw={groceries:G.defaults()};raw.groceries.groceries.items.push(G.item('eggs','Eggs'));
const original=copy(raw.groceries.groceries.items[0]),edited={...copy(original),note:'Breakfast'};
const buffer={tvId:'kitchen',key:'widget:grocery',listId:'groceries',edit:{kind:'item',original,value:edited,touched:true,open:true}};
G.restore('kitchen',[['kitchen-editor',buffer]]);assert.equal(G.proposal(owner,raw).book.groceries.items[0].note,'Breakfast');assert.equal(raw.groceries.groceries.items[0].note,'','Proposal is not publication');
assert.equal(G.proposal({tvId:'other'},raw).book.groceries.items[0].note,'','TV ownership is enforced');
const retained=copy(G.snapshot('kitchen'));G.clear('kitchen');G.restore('kitchen',retained);assert.equal(G.proposal(owner,raw).book.groceries.items[0].note,'Breakfast');
retained[0][1].edit.value.name='';G.restore('kitchen',retained);assert.equal(G.proposal(owner,raw).issue.code,'gNameRequired');assert.equal(G.proposal(owner,raw).book.groceries.items[0].name,'Eggs');
G.restore('kitchen',[['kitchen-editor',buffer]]);raw.groceries.groceries.items[0].note='Other cook';assert.equal(G.proposal(owner,raw).issue.code,'gEditorConflict');G.clear('kitchen');
assert.equal(G.display('500','ml'),'0.5 L');assert.equal(G.display('0.000001','ml'),'0.000000001 L','Never round away a precise report');
console.log('Groceries: interrupted editor validation, TV-scoped recovery, original-value conflict, and exact familiar display passed');

const unitBuffer={tvId:'units',key:'grocery',listId:'groceries',edit:{kind:'product',original:copy(p),value:copy(p),touched:true,measureErrors:{stock:'half'}}};
const stockDraft={groceries:G.defaults()};stockDraft.groceries.groceries.products.push(copy(p));G.restore('units',[['units-editor',unitBuffer]]);assert.equal(G.proposal({tvId:'units'},stockDraft).issue.code,'gDecimalRequired','Invalid display input blocks publication even when normalized value is unchanged');G.clear('units');
