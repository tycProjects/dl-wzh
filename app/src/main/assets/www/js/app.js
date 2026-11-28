(()=>{
'use strict';
const{E,gcd,ev,fmt,setAns}=PCEngine;
const $=s=>document.querySelector(s);
const mk=(t,c,x)=>{const e=document.createElement(t);if(c)e.className=c;if(x!=null)e.textContent=x;return e};
const ST={get(k,d,ok){try{const v=JSON.parse(localStorage.getItem(k));return v!=null&&ok(v)?v:d}catch(e){return d}},set(k,v){try{localStorage.setItem(k,JSON.stringify(v))}catch(e){}}};
const s0=ST.get('pc.set',{deg:true,prec:12},v=>typeof v==='object'&&typeof v.deg==='boolean'&&v.prec>=4&&v.prec<=12);
const S=Object.assign(PCEngine.cfg,{deg:s0.deg,prec:s0.prec,ans:0,
 mem:ST.get('pc.mem',0,v=>typeof v==='number'&&isFinite(v)),
 hist:ST.get('pc.hist',[],v=>Array.isArray(v)&&v.every(h=>h&&typeof h.e==='string'&&typeof h.r==='string')).slice(0,100)});

/* ---------- UI state ---------- */
let expr='',done=false;
const ex=$('#ex'),pv=$('#pv'),mi=$('#mi');
const disp=s=>s.replace(/\*/g,'×').replace(/\//g,'÷').replace(/-/g,'−');
const flash=m=>{pv.className='';pv.textContent=m};
function render(msg,err){
 ex.textContent=disp(expr)||'0';ex.scrollLeft=ex.scrollWidth;
 mi.textContent='';mi.append(mk('span',0,S.deg?'DEG':'RAD'),mk('span',0,S.mem?'M':''),mk('span',0,PCEngine.name));
 $('#degb').textContent=S.deg?'DEG':'RAD';
 pv.className=err?'err':'';
 if(msg!==undefined){pv.textContent=msg;return}
 pv.textContent='';
 if(done||!expr||/rand/i.test(expr)||/^-?[\d.]+$/.test(expr))return;
 try{pv.textContent='= '+fmt(ev(expr))}catch(e){pv.textContent=e.calc?e.message:'Invalid expression'}
}
function ins(s){
 if(done){expr=/^[+\-*\/^%!]|^ mod/.test(s)?expr:'';done=false}
 if(s==='.'&&/\.\d*$/.test(expr))return;
 expr+=s;render();
}
function eq(){
 if(!expr||done)return;
 try{const r=ev(expr),s=fmt(r),old=expr;
  S.hist.unshift({e:old,r:s});S.hist=S.hist.slice(0,100);ST.set('pc.hist',S.hist);
  S.ans=Number(r);setAns(r);expr=s;done=true;render(disp(old)+' =');drawHist();
 }catch(e){render(e.calc?e.message:'Invalid expression',1)}
}
const cur=()=>{try{return Number(ev(expr))}catch(e){return S.ans}};
const saveMem=()=>{ST.set('pc.mem',S.mem);render()};
async function copy(t){
 try{await navigator.clipboard.writeText(t);flash('Copied')}
 catch(e){const a=mk('textarea');a.value=t;document.body.append(a);a.select();try{document.execCommand('copy');flash('Copied')}catch(_){flash('Copy failed')}a.remove()}
}
function act(k){
 if(k==='clr'){expr='';done=false;render()}
 else if(k==='bs'){if(done){expr='';done=false}else expr=expr.replace(/(?:[a-zA-Z]+\(| mod |.)$/,'');render()}
 else if(k==='eq')eq();
 else if(k==='neg'){done=false;
  if(/^-?[\d.]+(E-?\d+)?$/.test(expr))expr=expr[0]==='-'?expr.slice(1):'-'+expr;
  else if(expr)expr=expr.startsWith('-(')&&expr.endsWith(')')?expr.slice(2,-1):'-('+expr+')';
  render()}
 else if(k==='mc'){S.mem=0;saveMem()}
 else if(k==='mr')ins(fmt(S.mem));
 else if(k==='ms'){S.mem=cur();saveMem()}
 else if(k==='mp'){S.mem+=cur();saveMem()}
 else if(k==='mm'){S.mem-=cur();saveMem()}
 else if(k==='deg'){S.deg=!S.deg;ST.set('pc.set',{deg:S.deg,prec:S.prec});render()}
 else if(k==='copy')copy(expr||'0');
 else if(k==='test')runTests();
}

/* ---------- build keypad ---------- */
const TH=globalThis.PC_THEMES;
let ui=ST.get('pc.ui','pixel',v=>typeof v==='string'&&Object.prototype.hasOwnProperty.call(TH,v));
function buildPad(){
 const pad=$('#pad');pad.textContent='';
 TH[ui].pad.split('|').forEach(d=>{const[l,t,v,x,c]=d.split(':');const b=mk('button','b '+(c||''),l);b.dataset[t]=v;b.setAttribute('aria-label',x||l);pad.append(b)});
}
function setTheme(k){
 ui=k;ST.set('pc.ui',k);document.documentElement.dataset.ui=k;buildPad();
 document.querySelectorAll('#thl button').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.t===k)));
}
Object.keys(TH).forEach(k=>{const b=mk('button','b',TH[k].name);b.dataset.t=k;b.onclick=()=>setTheme(k);$('#thl').append(b)});
setTheme(ui);
const MEMN={MC:['mc','Memory clear'],MR:['mr','Memory recall'],'M+':['mp','Memory add'],'M−':['mm','Memory subtract'],MS:['ms','Memory store']};
Object.keys(MEMN).forEach(l=>{const b=mk('button','b',l);b.dataset.k=MEMN[l][0];b.setAttribute('aria-label',MEMN[l][1]);$('#mem').append(b)});
const SG={
 TRIG:'sin~sin(|cos~cos(|tan~tan(|asin~asin(|acos~acos(|atan~atan(|π~pi|(~(|)~)|±~neg',
 POWER:'√~sqrt(|∛~cbrt(|x²~^2|xʸ~^|eˣ~exp(|ln~ln(|log~log(|1/x~recip(|x!~!|abs~abs(',
 DISC:'nCr~ncr(|nPr~npr(|gcd~gcd(|lcm~lcm(|prime~isprime(|⌊x⌋~floor(|⌈x⌉~ceil(|rnd~round(|mod~ mod |rand~rand()',
 CONST:'π~pi|e~e|φ~phi|∞~inf|EE~E|%~%|,~,|(~(|)~)|ANS~ans'};
const sgEl=$('#sg');
function showGroup(g){
 sgEl.querySelectorAll('button').forEach(x=>x.setAttribute('aria-pressed',String(x.dataset.g===g)));
 const box=$('#sci');box.textContent='';
 SG[g].split('|').forEach(d=>{const i=d.indexOf('~'),l=d.slice(0,i),v=d.slice(i+1),x=mk('button','b',l);
  if(v==='neg')x.dataset.k='neg';else x.dataset.s=v;x.setAttribute('aria-label',l);box.append(x)});
 ST.set('pc.sg',g);
}
Object.keys(SG).forEach(g=>{const x=mk('button','b',g);x.dataset.g=g;x.onclick=()=>showGroup(g);sgEl.append(x)});
showGroup(ST.get('pc.sg','TRIG',v=>typeof v==='string'&&Object.prototype.hasOwnProperty.call(SG,v)));
for(let i=4;i<=12;i++)$('#prec').append(new Option(i===12?'12 (max)':i,i));
$('#prec').value=S.prec;
$('#prec').onchange=e=>{S.prec=+e.target.value;ST.set('pc.set',{deg:S.deg,prec:S.prec});render()};

$('.win').addEventListener('click',e=>{
 const b=e.target.closest('button');if(!b)return;
 const{s,k}=b.dataset;if(s!==undefined)ins(s);else if(k)act(k);
});
document.querySelectorAll('.tabs .b').forEach(b=>b.onclick=()=>{
 const open=b.getAttribute('aria-expanded')==='true';
 document.querySelectorAll('.tabs .b').forEach(x=>{x.setAttribute('aria-expanded','false');$('#p-'+x.dataset.p).hidden=true});
 if(!open){b.setAttribute('aria-expanded','true');const p=$('#p-'+b.dataset.p);p.hidden=false;p.scrollIntoView({block:'nearest'})}
});

/* ---------- history ---------- */
function drawHist(){
 const ul=$('#hl');ul.textContent='';
 if(!S.hist.length){ul.append(mk('li',0,'No history yet. Press = to save a calculation.'));return}
 S.hist.forEach((h,i)=>{
  const li=mk('li');li.append(mk('div','hx',disp(h.e)+' = '+h.r));
  const r=mk('div','hb');
  [['USE','use'],['COPY','cp'],['DEL','del']].forEach(([t,a])=>{const b=mk('button','b',t);b.dataset.a=a;b.dataset.i=i;b.setAttribute('aria-label',t+' record '+(i+1));r.append(b)});
  li.append(r);ul.append(li);
 });
}
$('#hl').addEventListener('click',e=>{
 const b=e.target.closest('button');if(!b)return;
 const i=+b.dataset.i,h=S.hist[i];if(!h)return;
 if(b.dataset.a==='use'){expr=h.r;done=false;render()}
 else if(b.dataset.a==='cp')copy(h.e+' = '+h.r);
 else{S.hist.splice(i,1);ST.set('pc.hist',S.hist);drawHist()}
});
$('#hc').onclick=()=>{S.hist=[];ST.set('pc.hist',S.hist);drawHist()};

/* ---------- tools ---------- */
const U={Length:{m:1,km:1000,cm:.01,mm:.001,mi:1609.344,yd:.9144,ft:.3048,in:.0254},
 Weight:{kg:1,g:.001,mg:1e-6,lb:.45359237,oz:.028349523125,t:1000},
 Temp:{C:0,F:0,K:0},
 Speed:{'m/s':1,'km/h':1/3.6,mph:.44704,kn:.514444444},
 Time:{s:1,min:60,h:3600,day:86400,week:604800,year:31557600},
 Area:{'m²':1,'km²':1e6,'cm²':1e-4,ha:1e4,acre:4046.8564224,'ft²':.09290304,'in²':6.4516e-4},
 Volume:{L:1,mL:.001,'m³':1000,gal:3.785411784,qt:.946352946,cup:.2365882365,'fl oz':.0295735295625},
 Data:{bit:.125,B:1,KB:1024,MB:1048576,GB:1073741824,TB:1099511627776}};
const PCT=['A% of B','B increased by A%','B decreased by A%','Difference between A and B (%)','A is B% of what?'];
const N=(v,...k)=>k.map(x=>{if(v[x]===''||!Number.isFinite(+v[x]))throw E('Enter valid numbers');return +v[x]});
const pad2=n=>String(n).padStart(2,'0');
const today=()=>{const d=new Date();return d.getFullYear()+'-'+pad2(d.getMonth()+1)+'-'+pad2(d.getDate())};
const dt=s=>{const m=/^(\d{4})-(\d\d)-(\d\d)$/.exec(s);if(!m)throw E('Pick valid dates');return new Date(+m[1],m[2]-1,+m[3])};
const ymd=(a,b)=>{if(a>b)[a,b]=[b,a];let y=b.getFullYear()-a.getFullYear(),m=b.getMonth()-a.getMonth(),d=b.getDate()-a.getDate();
 if(d<0){m--;d+=new Date(b.getFullYear(),b.getMonth(),0).getDate()}if(m<0){y--;m+=12}return[y,m,d]};
const sel=(id,l,d,o)=>[id,l,'sel',d,o], inp=(id,l,d,t)=>[id,l,t||'number',d];
const TL={
 unit:{t:'Unit converter',f:v=>{const c=v.cat||'Length',us=Object.keys(U[c]);return[sel('cat','Type','Length',Object.keys(U)),inp('val','Value',1),sel('from','From',us[0],us),sel('to','To',us[1],us)]},
  run:v=>{const[x]=N(v,'val');let r;
   if(v.cat==='Temp'){const c={C:x=>x,F:x=>(x-32)*5/9,K:x=>x-273.15}[v.from](x);r={C:c,F:c*9/5+32,K:c+273.15}[v.to]}
   else r=x*U[v.cat][v.from]/U[v.cat][v.to];
   return[fmt(r)+' '+v.to,r]}},
 pct:{t:'Percentage',f:()=>[sel('mode','Mode',PCT[0],PCT),inp('a','A',20),inp('b','B',150)],
  run:v=>{const[a,b]=N(v,'a','b'),i=PCT.indexOf(v.mode);let r;
   if(i===0)r=a/100*b;else if(i===1)r=b*(1+a/100);else if(i===2)r=b*(1-a/100);
   else if(i===3){if(a+b===0)throw E('A + B cannot be 0');r=Math.abs(a-b)/Math.abs((a+b)/2)*100}
   else{if(b===0)throw E('B cannot be 0');r=a/(b/100)}
   return[fmt(r)+(i===3?' %':''),r]}},
 date:{t:'Date difference',f:()=>[inp('d1','From',today(),'date'),inp('d2','To',today(),'date')],
  run:v=>{const a=dt(v.d1),b=dt(v.d2),n=Math.round(Math.abs(b-a)/864e5),[y,m,d]=ymd(a,b);return[n+' days ('+y+'y '+m+'m '+d+'d)',n]}},
 age:{t:'Age',f:()=>[inp('dob','Birth date','2000-01-01','date')],
  run:v=>{const a=dt(v.dob),b=dt(today());if(a>b)throw E('Birth date is in the future');const[y,m,d]=ymd(a,b);return[y+' years, '+m+' months, '+d+' days',y]}},
 ratio:{t:'Ratio  A : B = C : ?',f:()=>[inp('a','A',3),inp('b','B',4),inp('c','C',9)],
  run:v=>{const[a,b,c]=N(v,'a','b','c');if(a===0)throw E('A cannot be 0');const d=b*c/a;
   let s=Number.isInteger(a)&&Number.isInteger(b)&&(a||b)?(a/gcd(a,b))+':'+(b/gcd(a,b)):fmt(a/b)+':1';
   return['D = '+fmt(d)+'  (A:B = '+s+')',d]}},
 simple:{t:'Simple interest',f:()=>[inp('p','Principal',1000),inp('r','Rate % per year',5),inp('t','Years',3)],
  run:v=>{const[p,r,t]=N(v,'p','r','t'),i=p*r*t/100;return['Interest '+fmt(i)+' | Total '+fmt(p+i),i]}},
 comp:{t:'Compound interest',f:()=>[inp('p','Principal',1000),inp('r','Rate % per year',5),inp('t','Years',3),inp('n','Compounds per year',12)],
  run:v=>{const[p,r,t,n]=N(v,'p','r','t','n');if(n<=0)throw E('Compounds per year must be > 0');const a=p*Math.pow(1+r/100/n,n*t);return['Total '+fmt(a)+' | Interest '+fmt(a-p),a]}},
 bmi:{t:'BMI',f:()=>[inp('kg','Weight (kg)',70),inp('cm','Height (cm)',175)],
  run:v=>{const[kg,cm]=N(v,'kg','cm');if(kg<=0||cm<=0)throw E('Values must be > 0');const b=kg/Math.pow(cm/100,2);
   return['BMI '+fmt(b)+' ('+(b<18.5?'Underweight':b<25?'Normal':b<30?'Overweight':'Obese')+')',b]}}
};
let tkey='unit',tnum=null;
const vals=()=>{const o={};$('#tf').querySelectorAll('[data-id]').forEach(i=>o[i.dataset.id]=i.value);return o};
function calc(){
 const o=$('#to');
 try{const[t,n]=TL[tkey].run(vals());o.textContent=t;tnum=n}
 catch(e){o.textContent=e.calc?e.message:'Check your inputs';tnum=null}
}
function tool(key,v={}){
 tkey=key;const box=$('#tf');box.textContent='';
 TL[key].f(v).forEach(([id,lab,ty,def,opts])=>{
  const l=mk('label','fld',lab);let i;
  if(ty==='sel'){i=mk('select');opts.forEach(o=>i.append(new Option(o,o)))}else{i=mk('input');i.type=ty;i.step='any'}
  i.dataset.id=id;i.value=v[id]??def;l.append(i);box.append(l);
 });
 calc();
}
Object.keys(TL).forEach(k=>$('#ts').append(new Option(TL[k].t,k)));
$('#ts').onchange=e=>tool(e.target.value);
$('#tf').addEventListener('input',e=>{if(e.target.dataset.id==='cat'){const v=vals();delete v.from;delete v.to;tool(tkey,v)}else calc()});
$('#tu').onclick=()=>{if(tnum!==null)ins(fmt(tnum))};
$('#tc').onclick=()=>copy($('#to').textContent);

/* ---------- keyboard ---------- */
addEventListener('keydown',e=>{
 if(/^(INPUT|SELECT|TEXTAREA)$/.test(e.target.tagName)||e.ctrlKey||e.metaKey||e.altKey)return;
 const k=e.key;let h;
 if(/^[0-9.+\-*\/()^%!,]$/.test(k)||/^[a-zA-Z]$/.test(k)){ins(k);h=k}
 else if(k==='Enter'||k==='='){eq();h='eq'}
 else if(k==='Backspace'){act('bs');h='bs'}
 else if(k==='Escape'){act('clr');h='clr'}
 else return;
 e.preventDefault();
 const b=document.querySelector('.pad [data-s="'+h+'"],.pad [data-k="'+h+'"]');
 if(b){b.classList.add('on');setTimeout(()=>b.classList.remove('on'),120)}
});

/* ---------- self tests (cases shared with tests/engine.test.js) ---------- */
function runTests(){
 const d=S.deg,p=S.prec;S.deg=true;S.prec=12;setAns(0);
 const T=globalThis.PC_TESTS||[],bad=[];
 T.forEach(([e,x])=>{let r;try{r=fmt(ev(e))}catch(er){r='ERR'}if(r!==x)bad.push(e+' -> '+r+' (want '+x+')')});
 S.deg=d;S.prec=p;setAns(S.ans);
 bad.forEach(m=>console.warn('FAIL',m));
 render('TEST '+(T.length-bad.length)+'/'+T.length+(bad.length?' FAIL':' OK'),bad.length>0);
}

drawHist();tool('unit');render();
if('serviceWorker' in navigator&&/^https?:$/.test(location.protocol))navigator.serviceWorker.register('sw.js').catch(()=>{});
})();
