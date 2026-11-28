(()=>{
'use strict';
const cfg={deg:true,prec:12};
/* ---------- engine (no eval) ---------- */
const E=m=>{const e=new Error(m);e.calc=1;return e};
const C={pi:Math.PI,e:Math.E,phi:(1+Math.sqrt(5))/2,inf:Infinity,ans:0};
const has=(o,k)=>Object.prototype.hasOwnProperty.call(o,k);
const dom=(c,m)=>{if(!c)throw E(m||'Invalid domain')};
const rad=x=>cfg.deg?x*Math.PI/180:x, deg=x=>cfg.deg?x*180/Math.PI:x, tr=x=>Math.abs(x)<1e-12?0:x;
const nat=x=>dom(Number.isInteger(x)&&x>=0,'Needs whole numbers ≥ 0');
const gcd=(a,b)=>{dom(Number.isInteger(a)&&Number.isInteger(b),'GCD/LCM need whole numbers');a=Math.abs(a);b=Math.abs(b);while(b)[a,b]=[b,a%b];return a};
const fact=n=>{nat(n);if(n>170)throw E('Overflow: factorial too large');let r=1;for(let i=2;i<=n;i++)r*=i;return r};
const F={
 sin:x=>tr(Math.sin(rad(x))),cos:x=>tr(Math.cos(rad(x))),
 tan:x=>{dom(Math.abs(Math.cos(rad(x)))>1e-12,'Undefined: tan at its pole');return tr(Math.tan(rad(x)))},
 asin:x=>{dom(Math.abs(x)<=1,'Invalid domain: asin needs −1 to 1');return deg(Math.asin(x))},
 acos:x=>{dom(Math.abs(x)<=1,'Invalid domain: acos needs −1 to 1');return deg(Math.acos(x))},
 atan:x=>deg(Math.atan(x)),
 sqrt:x=>{dom(x>=0,'Invalid domain: √ of a negative');return Math.sqrt(x)},cbrt:x=>Math.cbrt(x),
 ln:x=>{dom(x>0,'Invalid domain: ln needs x > 0');return Math.log(x)},
 log:x=>{dom(x>0,'Invalid domain: log needs x > 0');return Math.log10(x)},
 exp:x=>Math.exp(x),abs:x=>Math.abs(x),floor:x=>Math.floor(x),ceil:x=>Math.ceil(x),
 round:(x,n=0)=>{n=Math.trunc(n);dom(n>=0&&n<=15,'Round digits must be 0–15');const k=10**n;return Math.round(x*k)/k},
 recip:x=>{if(x===0)throw E('Divide by zero');return 1/x},
 fact,
 ncr:(n,r)=>{nat(n);nat(r);dom(r<=n,'nCr needs r ≤ n');let v=1;for(let i=1;i<=r;i++)v=v*(n-r+i)/i;return Math.round(v)},
 npr:(n,r)=>{nat(n);nat(r);dom(r<=n,'nPr needs r ≤ n');let v=1;for(let i=0;i<r;i++)v*=n-i;return v},
 gcd,lcm:(a,b)=>a===0||b===0?0:Math.abs(a/gcd(a,b)*b),
 isprime:n=>{nat(n);dom(n<=1e12,'Too large for prime check');if(n<2)return 0;for(let i=2;i*i<=n;i++)if(n%i===0)return 0;return 1},
 rand:(...a)=>a.length===0?Math.random():a.length===1?Math.random()*a[0]:a[0]+Math.random()*(a[1]-a[0])
};
const tok=s=>{
 s=s.replace(/×/g,'*').replace(/÷/g,'/').replace(/[−–]/g,'-').replace(/π/g,' pi ').replace(/φ/g,' phi ').replace(/√/g,' sqrt ').replace(/∛/g,' cbrt ').replace(/∞/g,' inf ').replace(/²/g,'^2');
 const T=[];
 for(const m of s.matchAll(/(\d+\.?\d*(?:E[+-]?\d+)?|\.\d+(?:E[+-]?\d+)?)|([A-Za-z_]+)|(\S)/g)){
  if(m[1])T.push({n:+m[1]});else if(m[2])T.push({i:m[2].toLowerCase()});else T.push({o:m[3]});
 }
 return T;
};
function evBuiltin(src){
 if(src.length>300)throw E('Expression too long');
 const T=tok(src);let p=0;
 const isOp=c=>T[p]&&T[p].o===c;
 const expr=()=>{let v=term();for(;;){if(isOp('+')){p++;v+=term()}else if(isOp('-')){p++;v-=term()}else return v}};
 const term=()=>{let v=unary();for(;;){const t=T[p];if(!t)return v;
  if(t.o==='*'){p++;v*=unary()}
  else if(t.o==='/'){p++;const d=unary();if(d===0)throw E('Divide by zero');v/=d}
  else if(t.i==='mod'){p++;const d=unary();if(d===0)throw E('Divide by zero');v%=d}
  else if(t.n!==undefined||t.o==='('||t.i){v*=unary()}
  else return v}};
 const unary=()=>{if(isOp('-')){p++;return -unary()}if(isOp('+')){p++;return unary()}return power()};
 const power=()=>{const b=post();if(isOp('^')){p++;const e=unary();if(b===0&&e<0)throw E('Divide by zero');return Math.pow(b,e)}return b};
 const post=()=>{let v=prim();for(;;){if(isOp('!')){p++;v=fact(v)}else if(isOp('%')){p++;v/=100}else return v}};
 const prim=()=>{
  const t=T[p++];if(!t)throw E('Incomplete expression');
  if(t.n!==undefined)return t.n;
  if(t.o==='('){const v=expr();if(!isOp(')'))throw E('Missing )');p++;return v}
  if(t.i){
   if(has(C,t.i))return C[t.i];
   if(!has(F,t.i))throw E('Unknown name: '+t.i);
   const f=F[t.i];let a=[];
   if(isOp('(')){p++;if(!isOp(')')){a.push(expr());while(isOp(',')){p++;a.push(expr())}}if(!isOp(')'))throw E('Missing )');p++}
   else if(f.length>0)a=[power()];
   return f(...a);
  }
  throw E('Unexpected '+t.o);
 };
 const r=expr();
 if(p<T.length)throw E('Unexpected '+(T[p].o||T[p].i||T[p].n));
 if(Number.isNaN(r))throw E('Undefined result');
 if(!Number.isFinite(r)){if(T.some(t=>t.i==='inf'))return r;throw E('Overflow: number too large')}
 if(r!==0&&Math.abs(r)<1e-300)throw E('Underflow: number too small');
 return r;
}
const fmtNum=n=>{if(n===Infinity)return '∞';if(n===-Infinity)return '-∞';if(n===0)return '0';
 const r=Number(n.toPrecision(cfg.prec));
 return Math.abs(r)>=1e15||Math.abs(r)<1e-6?r.toExponential().replace('e+','E').replace('e','E'):String(r)};


/* ---------- Math.js adapter (BigNumber, whitelisted, hardened) ---------- */
let M=null,parseFn=null,name='BUILT-IN',ansV=0;
const ID={pi:'pi',e:'e',phi:'phi',inf:'Infinity',ans:'ans',mod:'mod',sin:'sin',cos:'cos',tan:'tan',asin:'asin',acos:'acos',atan:'atan',
 sqrt:'sqrt',cbrt:'cbrt',ln:'log',log:'log10',exp:'exp',abs:'abs',floor:'floor',ceil:'ceil',round:'round',recip:'recip',fact:'factorial',
 ncr:'combinations',npr:'permutations',gcd:'gcd',lcm:'lcm',isprime:'isPrime',rand:'rand'};
const norm=s=>s.replace(/×/g,'*').replace(/÷/g,'/').replace(/[−–]/g,'-').replace(/π/g,' pi ').replace(/φ/g,' phi ').replace(/√/g,' sqrt ').replace(/∛/g,' cbrt ').replace(/∞/g,' inf ').replace(/²/g,'^2');
function useMath(lib){
 try{
  if(!lib||!lib.create||!lib.all)return false;
  const m=lib.create(lib.all,{number:'BigNumber',precision:64});
  parseFn=m.parse;
  const off=n=>()=>{throw E('Disabled: '+n)};
  const od=m.divide,of=m.factorial;
  m.import({import:off('import'),createUnit:off('createUnit'),evaluate:off('evaluate'),parse:off('parse'),simplify:off('simplify'),derivative:off('derivative'),reviver:off('reviver'),
   divide:(a,b)=>{if(m.isZero(b))throw E('Divide by zero');return od(a,b)},
   factorial:x=>{const n=Number(x);if(!Number.isInteger(n)||n<0)throw E('Factorial needs a whole number ≥ 0');if(n>170)throw E('Overflow: factorial too large');return of(x)}},{override:true});
  M=m;name='MATH.JS';return true;
 }catch(e){M=null;name='BUILT-IN';return false}
}
function evMath(src){
 if(src.length>300)throw E('Expression too long');
 let s=norm(src).replace(/%(?=\s*(?:$|[+\-*\/^)!,]|mod\b))/g,'*0.01'),inf=false;
 s=s.replace(/(\d+\.?\d*(?:E[+-]?\d+)?|\.\d+(?:E[+-]?\d+)?)|([A-Za-z_]+)|(\s+)/g,(m,n,id,ws)=>{
  if(n)return n.replace('E','e');if(ws)return m;
  const k=id.toLowerCase();if(!has(ID,k))throw E('Unknown name: '+id);if(k==='inf')inf=true;return ID[k]});
 if(!/^[\w\s.+\-*\/^%!(),]*$/.test(s))throw E('Unsupported character');
 const bn=x=>M.bignumber(x),R=x=>cfg.deg?M.multiply(x,M.divide(M.pi,180)):x,D=x=>cfg.deg?M.divide(M.multiply(x,180),M.pi):x;
 const tr=x=>M.smaller(M.abs(x),1e-12)?bn(0):x;
 const scope={ans:M.isBigNumber(ansV)?ansV:bn(ansV),
  sin:x=>tr(M.sin(R(x))),cos:x=>tr(M.cos(R(x))),
  tan:x=>{if(M.smaller(M.abs(M.cos(R(x))),1e-12))throw E('Undefined: tan at its pole');return tr(M.tan(R(x)))},
  asin:x=>D(M.asin(x)),acos:x=>D(M.acos(x)),atan:x=>D(M.atan(x)),
  recip:x=>{if(M.isZero(x))throw E('Divide by zero');return M.divide(1,x)},
  rand:(...a)=>bn(a.length===0?Math.random():a.length===1?Math.random()*+a[0]:+a[0]+Math.random()*(a[1]-a[0]))};
 let r;
 try{r=parseFn(s).compile().evaluate(scope)}
 catch(e){if(e.calc)throw e;const m=String(e&&e.message);
  throw E(/end of expression/i.test(m)?'Incomplete expression':/arenthes/i.test(m)?'Missing )':/zero/i.test(m)?'Divide by zero':'Invalid expression')}
 if(typeof r==='boolean')r=bn(r?1:0);else if(typeof r==='number')r=bn(r);
 else if(!M.isBigNumber(r))throw E('Invalid domain: result is not a real number');
 if(r.isNaN())throw E('Undefined result');
 if(!r.isFinite()){if(inf)return r;throw E('Overflow or divide by zero')}
 if(!r.isZero()){const a=r.abs();if(a.gt('1e308'))throw E('Overflow: number too large');if(a.lt('1e-300'))throw E('Underflow: number too small')}
 return r;
}
const ev=s=>M?evMath(s):evBuiltin(s);
const fmt=v=>{
 if(typeof v==='number')return fmtNum(v);
 if(v.isZero())return '0';if(!v.isFinite())return v.isNeg()?'-∞':'∞';
 const r=v.toSignificantDigits(cfg.prec),a=r.abs();
 return a.gte(1e15)||a.lt(1e-6)?r.toExponential().replace('e+','E').replace('e','E'):r.toFixed();
};
const setAns=v=>{ansV=v;C.ans=Number(v)};
useMath(globalThis.math);
const api={cfg,E,C,gcd,ev,fmt,setAns,useMath,get name(){return name}};
if(typeof module!=='undefined')module.exports=api;else globalThis.PCEngine=api;
})();
