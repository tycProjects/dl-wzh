function bridge(){return window.BMAndroid||null}
function showGames(){document.getElementById('games').scrollIntoView({behavior:'smooth'});loadApps()}
function loadApps(){const box=document.getElementById('appList'),b=bridge();if(!b){box.innerHTML='<div class="empty">Abra este projeto no APK Android para listar os aplicativos instalados.</div>';return}try{const apps=JSON.parse(b.getApps());box.innerHTML='';if(!apps.length){box.innerHTML='<div class="empty">Nenhum aplicativo com atalho encontrado.</div>';return}apps.forEach(a=>{const row=document.createElement('div');row.className='app';const ico=document.createElement('div');ico.className='ico';ico.textContent=a.isGame?'▶':'APP';const meta=document.createElement('div');meta.className='meta';const name=document.createElement('strong');name.textContent=a.name;const sub=document.createElement('small');sub.textContent=(a.isGame?'JOGO • ':'APLICATIVO • ')+a.packageName;meta.append(name,sub);const btn=document.createElement('button');btn.textContent='ABRIR';btn.onclick=()=>{if(!b.launchApp(a.packageName))alert('Não foi possível abrir este aplicativo.')};row.append(ico,meta,btn);box.append(row)})}catch(e){box.innerHTML='<div class="empty">Falha ao consultar os aplicativos.</div>'}}
function enableOverlay(){const b=bridge();if(!b){alert('Recurso disponível na versão APK nativa.');return}b.requestOverlay()}
function stopOverlay(){const b=bridge();if(b)b.stopFloating();else alert('Abra no APK para controlar o botão flutuante.')}
function startBg(){const b=bridge();if(b){b.startBackground();alert('Solicitação enviada. Verifique a notificação do BLACKMAGIC.')}else alert('Abra no APK para iniciar o serviço em segundo plano.')}
function stopBg(){const b=bridge();if(b)b.stopBackground()}
function batterySettings(){const b=bridge();if(b)b.openBatterySettings();else alert('Abra no APK Android para acessar esta configuração.')}
function refreshStats(){const b=bridge();if(!b){alert('Métricas nativas aparecem no APK.');return}try{const s=JSON.parse(b.getStats());document.getElementById('ram').textContent=s.ramAvailableMb+' MB';document.getElementById('battery').textContent=(s.batteryPercent>=0?s.batteryPercent+'%':'—');document.getElementById('temp').textContent=(s.batteryTempC>=0?s.batteryTempC.toFixed(1)+' °C':'—')}catch(e){alert('Não foi possível ler as métricas.')}}
window.addEventListener('load',()=>{
  loadApps();refreshStats();
  const splash=document.getElementById('introSplash');
  if(splash){setTimeout(()=>{splash.classList.add('is-leaving');setTimeout(()=>splash.remove(),750)},2850)}
});
