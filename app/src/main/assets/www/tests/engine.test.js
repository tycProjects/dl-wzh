const E = require('../js/engine.js');
const T = require('./cases.js');
try { E.useMath(require('mathjs')); } catch (e) { /* mathjs not installed: test built-in engine */ }
E.cfg.deg = true; E.cfg.prec = 12;
let bad = 0;
for (const [e, x] of T) {
  let r; try { r = E.fmt(E.ev(e)); } catch (er) { r = 'ERR'; }
  if (r !== x) { bad++; console.log('FAIL', e, '->', r, '(want', x + ')'); }
}
console.log((T.length - bad) + '/' + T.length + ' passed [' + E.name + ']');
process.exit(bad ? 1 : 0);
