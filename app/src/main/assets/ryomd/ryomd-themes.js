(() => {
  'use strict';
  const KEY = 'ryomd-dashboard-theme-v2';
  const THEMES = [
    { id: 'dark', name: 'Gelap', icon: '◐', note: 'Slate · blue' },
    { id: 'light', name: 'Terang', icon: '◑', note: 'Soft white · cobalt' }
  ];
  const read = () => {
    try { const value = localStorage.getItem(KEY); return THEMES.some(theme => theme.id === value) ? value : 'dark'; }
    catch { return 'dark'; }
  };
  const apply = (id, persist = true) => {
    const theme = THEMES.find(item => item.id === id) || THEMES[0];
    document.documentElement.setAttribute('data-ryomd-theme', theme.id);
    const toggle = document.getElementById('ryomdThemeToggle');
    if (toggle) {
      const icon = toggle.querySelector('.ryomd-theme-emoji');
      const label = toggle.querySelector('.ryomd-theme-label');
      if (icon) icon.textContent = theme.icon;
      if (label) label.textContent = theme.name;
    }
    const menu = document.getElementById('ryomdThemeMenu');
    if (menu) menu.querySelectorAll('[data-theme-choice]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.themeChoice === theme.id)));
    if (persist) { try { localStorage.setItem(KEY, theme.id); } catch {} }
  };
  const build = () => {
    if (!document.body || document.getElementById('ryomdThemeDock')) return;
    const dock = document.createElement('div'); dock.id = 'ryomdThemeDock';
    dock.innerHTML = `<div id="ryomdThemeMenu" role="group" aria-label="Pilih tema" hidden><div class="ryomd-theme-heading">Tampilan</div>${THEMES.map(theme => `<button type="button" data-theme-choice="${theme.id}" aria-pressed="false"><span class="ryomd-theme-emoji" aria-hidden="true">${theme.icon}</span><span class="ryomd-theme-name">${theme.name}</span><span aria-hidden="true">✓</span></button>`).join('')}</div><button id="ryomdThemeToggle" type="button" aria-expanded="false" aria-controls="ryomdThemeMenu"><span class="ryomd-theme-emoji" aria-hidden="true">◐</span><span class="ryomd-theme-label">Gelap</span><span aria-hidden="true">⌃</span></button>`;
    document.body.appendChild(dock);
    const toggle = dock.querySelector('#ryomdThemeToggle'); const menu = dock.querySelector('#ryomdThemeMenu');
    const close = () => { menu.hidden = true; toggle.setAttribute('aria-expanded','false'); };
    toggle.addEventListener('click', () => { menu.hidden = !menu.hidden; toggle.setAttribute('aria-expanded',String(!menu.hidden)); });
    menu.querySelectorAll('[data-theme-choice]').forEach(button => button.addEventListener('click', () => { apply(button.dataset.themeChoice); close(); }));
    document.addEventListener('pointerdown', event => { if (!dock.contains(event.target)) close(); });
    document.addEventListener('keydown', event => { if (event.key === 'Escape') close(); });
  };
  apply(read(), false);
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', () => { build(); apply(read(), false); }, { once: true });
  else { build(); apply(read(), false); }
})();
