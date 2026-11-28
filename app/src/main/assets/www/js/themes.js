(function(){
const T={
 pixel:{name:'Pixel Mono',pad:'C:k:clr:Clear all|E:k:bs:Backspace|=:k:eq:Equals|×:s:*:Multiply|7:s:7|8:s:8|9:s:9|÷:s:/:Divide|4:s:4|5:s:5|6:s:6|−:s:-:Subtract|1:s:1|2:s:2|3:s:3|+:s:+:Add:tall|0:s:0:Zero:wide|.:s:.:Decimal point'},
 coloros:{name:'Color OS',pad:'7:s:7|8:s:8|9:s:9|+:s:+:Add:op|4:s:4|5:s:5|6:s:6|−:s:-:Subtract:op|1:s:1|2:s:2|3:s:3|×:s:*:Multiply:op|C:k:clr:Clear all:red|0:s:0|=:k:eq:Equals:op|÷:s:/:Divide:op|.:s:.:Decimal point|⌫:k:bs:Backspace|(:s:(:Open bracket|):s:):Close bracket'}
};
/* To add a theme: add an entry here (name + keypad layout) and a [data-ui=<key>] block in css/main.css. */
globalThis.PC_THEMES=T;
})();
