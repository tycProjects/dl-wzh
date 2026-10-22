(() => {
'use strict';
const data=window.SPERSH_DATA||{movies:[],channels:[]};
const $=id=>document.getElementById(id);

function switchPage(page){
  document.querySelectorAll('.nav-item').forEach(b=>b.classList.toggle('active',b.dataset.page===page));
  $('moviesPage').classList.toggle('active',page==='movies');
  $('channelsPage').classList.toggle('active',page==='channels');
  $('sidebar').classList.remove('open');
}
document.querySelectorAll('.nav-item').forEach(b=>b.onclick=()=>switchPage(b.dataset.page));
$('menuBtn').onclick=()=>$('sidebar').classList.toggle('open');

function movieCard(movie){
  const b=document.createElement('button');b.className='movie-card';
  const poster=document.createElement('div');poster.className='movie-poster';
  if(movie.poster){const img=document.createElement('img');img.src=movie.poster;img.alt='';img.loading='lazy';poster.appendChild(img);}
  const h=document.createElement('h3');h.textContent=movie.title||'بدون عنوان';
  const p=document.createElement('p');p.textContent=[movie.year,movie.genre].filter(Boolean).join(' · ');
  b.append(poster,h,p);b.onclick=()=>{if(!(window.SPERSH_MEDIA3&&window.SPERSH_MEDIA3.play(movie)))window.SPERSH_PLAYER.openItem(movie);};return b;
}
function channelCard(ch){
  const b=document.createElement('button');b.className='channel-card';b.textContent=ch.title||'قناة';b.onclick=()=>{const item={...ch,type:'channel'};if(!(window.SPERSH_MEDIA3&&window.SPERSH_MEDIA3.play(item)))window.SPERSH_PLAYER.openItem(item);};return b;
}
function render(){
  $('moviesGrid').innerHTML='';$('channelsGrid').innerHTML='';
  data.movies.forEach(m=>$('moviesGrid').appendChild(movieCard(m)));
  data.channels.forEach(c=>$('channelsGrid').appendChild(channelCard(c)));
  $('movieCount').textContent=data.movies.length;$('channelCount').textContent=data.channels.length;
  $('moviesEmpty').hidden=data.movies.length>0;$('channelsEmpty').hidden=data.channels.length>0;
}
$('searchInput').addEventListener('input',e=>{
  const q=e.target.value.trim().toLowerCase();
  document.querySelectorAll('.movie-card,.channel-card').forEach(card=>card.hidden=q&&!card.textContent.toLowerCase().includes(q));
});
render();
window.SPERSH_APP={render,switchPage,data};
})();
