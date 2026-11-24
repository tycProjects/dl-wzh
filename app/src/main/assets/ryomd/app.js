(() => {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const safeStorage = {
    get(key, fallback) { try { const v = localStorage.getItem(key); return v == null ? fallback : JSON.parse(v); } catch { return fallback; } },
    set(key, value) { try { localStorage.setItem(key, JSON.stringify(value)); } catch {} }
  };
  const state = {
    category: 'all', search: '', visibleLimit: 12, services: [], history: safeStorage.get('nx_dl_history', []),
    historyFilter: 'all', assetName: '211648.mp4', zipServiceId: 'whatsapp-bot', zipMode: 'source',
    modalRequest: 0, soundEnabled: safeStorage.get('nx_sound_enabled', true) !== false
  };
  const savedAssistantMessages = safeStorage.get('ryomd_assistant_messages_v1', []);
  const assistantState = {
    messages: Array.isArray(savedAssistantMessages) ? savedAssistantMessages.filter(item => item && ['user','assistant','notice'].includes(item.role) && typeof item.content === 'string').slice(-80) : [],
    conversationId: safeStorage.get('ryomd_assistant_conversation_v1', '') || `ryomd-${Date.now()}-${Math.random().toString(36).slice(2,10)}`,
    config: null, sending: false
  };
  safeStorage.set('ryomd_assistant_conversation_v1', assistantState.conversationId);

  const TOOLS = [
    { id: 'wa-bot', title: 'WhatsApp Bot RYOMD', cat: 'bots', tag: 'BOT', icon: 'whatsapp', desc: 'Jalankan, hentikan, mulai ulang, dan lihat log bot WhatsApp.' },
    { id: 'all-services', title: 'Multi-Bot Runtime', cat: 'bots', tag: 'CORE', icon: 'server', desc: 'Kontrol WhatsApp, Telegram, Hermes, dan RVC dari satu panel.' },
    { id: 'zip-deploy', title: 'Bot & Plugin Updater', cat: 'bots', tag: 'CORE', icon: 'package', desc: 'Unggah paket source bot atau plugin WhatsApp dengan backup.' },
    { id: 'app-update', title: 'App Update Center', cat: 'bots', tag: 'UPDATE', icon: 'refresh', desc: 'Pasang paket pembaruan antarmuka Ryomd dari file ZIP.' },
    { id: 'env-config', title: 'Entry Config Manager', cat: 'bots', tag: 'CORE', icon: 'sliders', desc: 'Atur entry point dan file .env secara write-only tanpa menampilkan kredensial rahasia.' },
    { id: 'terminal-shell', title: 'Diagnostic Terminal', cat: 'bots', tag: 'SAFE', icon: 'terminal', desc: 'Jalankan perintah diagnostik yang diizinkan oleh backend.' },
    { id: 'file-explorer', title: 'Workspace Explorer', cat: 'bots', tag: 'CORE', icon: 'folder', desc: 'Lihat file workspace dan pratinjau file teks yang aman.' },
    { id: 'pkg-deps', title: 'Dependency Manager', cat: 'bots', tag: 'SYSTEM', icon: 'blocks', desc: 'Periksa dan pasang paket runtime yang dibutuhkan tool.' },
    { id: 'system-logs', title: 'System Log Center', cat: 'bots', tag: 'LIVE', icon: 'logs', desc: 'Baca log dashboard dan bot dengan penyamaran token.' },
    { id: 'sys-telemetry', title: 'System Monitor', cat: 'bots', tag: 'LIVE', icon: 'activity', desc: 'Pantau CPU, RAM, storage, dan status layanan.' },
    { id: 'banner-studio', title: 'Media Studio', cat: 'bots', tag: 'MEDIA', icon: 'image', desc: 'Ganti video banner, avatar, dan poster dari perangkat.' },
    { id: 'social-dl', title: 'Social Downloader', cat: 'downloader', tag: 'DOWNLOAD', icon: 'download', desc: 'Unduh video atau album media melalui yt-dlp lokal.' },
    { id: 'yt-audio', title: 'Audio Extractor', cat: 'downloader', tag: 'AUDIO', icon: 'audio', desc: 'Ekstrak audio MP3 dari URL yang didukung yt-dlp.' },
    { id: 'yt-player', title: 'Ryomd Tube', cat: 'tools', tag: 'MEDIA', icon: 'spark', desc: 'Beranda, langganan, riwayat, dan koleksi ala YouTube dengan pemutar native Android. Login Google opsional.' },
    { id: 'music-studio', title: 'Music Studio V3', cat: 'tools', tag: 'MUSIC', icon: 'music', desc: 'Cari dan putar track penuh Audius, preview Apple, playlist, antrean, favorit, riwayat, lirik, dan library lokal.' },
    { id: 'comic-reader', title: 'Online Comic Reader', cat: 'tools', tag: 'ONLINE', icon: 'book', desc: 'Cari manga, buka daftar chapter, dan baca halaman.' },
    { id: 'anime-hub', title: 'Anime Hub', cat: 'tools', tag: 'CATALOG', icon: 'book', desc: 'Katalog anime dan manhwa, pencarian, jadwal, episode, subtitle, dan pemutar streaming Lumio di dalam panel.' },
    { id: 'qr-gen', title: 'QR Code Studio', cat: 'maker', tag: 'MAKER', icon: 'qr', desc: 'Buat QR Code PNG dari teks, nomor, atau tautan.' },
    { id: 'quote-maker', title: 'Quote Card Designer', cat: 'maker', tag: 'MAKER', icon: 'quote', desc: 'Rancang kartu kutipan dan ekspor sebagai PNG.' },
    { id: 'pass-gen', title: 'Secure Password', cat: 'maker', tag: 'SECURITY', icon: 'key', desc: 'Buat kata sandi acak memakai crypto browser.' },
    { id: 'avatar-card', title: 'Profile Card Studio', cat: 'maker', tag: 'MAKER', icon: 'user', desc: 'Desain kartu profil dari foto lokal dan teks.' },
    { id: 'html-preview', title: 'HTML Playground', cat: 'maker', tag: 'CODE', icon: 'code', desc: 'Uji HTML/CSS/JS dalam iframe terisolasi.' },
    { id: 'module-reg', title: 'Module Registry', cat: 'maker', tag: 'CORE', icon: 'puzzle', desc: 'Daftarkan manifest modul tambahan ke workspace.' },
    { id: 'ryo-ai', title: 'Ryomd AI Assistant', cat: 'tools', tag: 'AI', icon: 'spark', desc: 'Asisten AI pribadi untuk percakapan, bantuan teknis, dan pengelolaan workspace melalui koneksi Hermes.' },
    { id: 'bmkg-cuaca', title: 'BMKG Weather', cat: 'tools', tag: 'LIVE', icon: 'cloud', desc: 'Tampilkan prakiraan cuaca per desa dari data BMKG.' },
    { id: 'crypto-scan', title: 'Crypto Market', cat: 'tools', tag: 'LIVE', icon: 'chart', desc: 'Lihat harga BTC, ETH, SOL, dan TON dalam IDR/USD.' },
    { id: 'ip-intel', title: 'IP & Network Info', cat: 'tools', tag: 'NETWORK', icon: 'globe', desc: 'Periksa IP publik, ISP, ASN, dan lokasi jaringan.' },
    { id: 'web-encrypt', title: 'Text Encoder', cat: 'tools', tag: 'UTILITY', icon: 'shield', desc: 'Encode dan decode Base64, URL, dan Hex.' },
    { id: 'morse-code', title: 'Morse Studio', cat: 'tools', tag: 'UTILITY', icon: 'radio', desc: 'Konversi teks ke Morse dan dengarkan kode bunyi.' },
    { id: 'foto-link', title: 'Image Data URL', cat: 'tools', tag: 'UTILITY', icon: 'file-image', desc: 'Ubah gambar lokal menjadi Data URL/Base64.' }
  ];

  // Original inline SVG symbol set. Each icon shares the same optical weight and viewBox.
  const ICONS = {
    whatsapp: '<path d="M20.1 11.6a8.1 8.1 0 0 1-11.9 7.1L4 20l1.3-4A8.1 8.1 0 1 1 20.1 11.6Z"/><path d="M8.7 8.5c.2-.5.5-.5.8-.5h.5l.8 1.9-.8 1c.7 1.4 1.7 2.3 3.2 2.9l.9-.9 1.9.8v.5c0 .4-.2.7-.6.9-.5.3-1.2.4-2 .2-2.9-.8-5.1-2.9-5.8-5.3-.2-.7-.1-1.2.1-1.5Z"/>',
    server: '<rect x="3" y="4" width="18" height="6" rx="2"/><rect x="3" y="14" width="18" height="6" rx="2"/><path d="M7 7h.01M7 17h.01M11 7h6M11 17h6"/>',
    package: '<path d="m12 3 8 4.5v9L12 21l-8-4.5v-9L12 3Z"/><path d="m4.3 7.6 7.7 4.3 7.7-4.3M12 12v9M8 5.3l8 4.5"/>',
    refresh: '<path d="M20 7v5h-5M4 17v-5h5"/><path d="M6.1 9A7 7 0 0 1 18 6l2 6M4 12l2 6a7 7 0 0 0 11.9-3"/>',
    sliders: '<path d="M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1"/><circle cx="15" cy="6" r="2"/><circle cx="9" cy="12" r="2"/><circle cx="17" cy="18" r="2"/>',
    terminal: '<path d="m5 7 5 5-5 5M12 17h7"/>',
    folder: '<path d="M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z"/><path d="M3 9h18"/>',
    blocks: '<rect x="3" y="3" width="8" height="8" rx="2"/><rect x="13" y="3" width="8" height="5" rx="1.5"/><rect x="13" y="10" width="8" height="11" rx="2"/><rect x="3" y="13" width="8" height="8" rx="2"/>',
    logs: '<path d="M5 4h14M5 9h14M5 14h8M5 19h10"/><circle cx="18" cy="16" r="3"/><path d="m20.2 18.2 1.3 1.3"/>',
    activity: '<path d="M3 12h4l2.2-6 4.1 12 2.1-6H21"/>',
    image: '<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="1.6"/><path d="m4 17 5-5 4 4 3-3 4 4"/>',
    download: '<path d="M12 3v12m-5-5 5 5 5-5M4 18v3h16v-3"/>',
    audio: '<path d="M4 10v4h4l5 4V6l-5 4H4Z"/><path d="M16 9a5 5 0 0 1 0 6M18.5 6.5a8.5 8.5 0 0 1 0 11"/>',
    music: '<path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/>',
    book: '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21V5.5Z"/><path d="M4 17a2.5 2.5 0 0 1 2.5-2.5H20M8 7h8M8 10h6"/>',
    qr: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><path d="M14 14h3v3h-3zM20 14v2M17 20h4M14 20v1"/>',
    quote: '<path d="M8.5 11H4V8a3 3 0 0 1 3-3h2v6ZM20 11h-4.5V8a3 3 0 0 1 3-3h2v6ZM4 11v2a5 5 0 0 0 5 5M15.5 11v2a5 5 0 0 0 5 5"/>',
    key: '<circle cx="8" cy="15" r="4"/><path d="m11 12 8-8 2 2-2 2 2 2-3 3-2-2-2 2M8 15h.01"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    code: '<path d="m8 8-4 4 4 4M16 8l4 4-4 4M14 5l-4 14"/>',
    puzzle: '<path d="M8 4a2 2 0 1 1 4 0v2h3a2 2 0 0 1 2 2v3h2a2 2 0 1 1 0 4h-2v3a2 2 0 0 1-2 2h-3v-2a2 2 0 1 0-4 0v2H6a2 2 0 0 1-2-2v-3h2a2 2 0 1 0 0-4H4V8a2 2 0 0 1 2-2h2V4Z"/>',
    spark: '<path d="m12 3 1.7 5.3L19 10l-5.3 1.7L12 17l-1.7-5.3L5 10l5.3-1.7L12 3Z"/><path d="m19 16 .8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8L19 16Z"/>',
    cloud: '<path d="M7 18h10a4 4 0 0 0 .3-8A6 6 0 0 0 6 9a4.5 4.5 0 0 0 1 9Z"/><path d="m9 21 1-2m4 2 1-2"/>',
    chart: '<path d="M4 20V5M4 20h17"/><path d="m7 15 4-4 3 2 6-7"/><path d="M16 6h4v4"/>',
    globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18"/>',
    shield: '<path d="M12 3 20 6v5c0 5-3.4 8.5-8 10-4.6-1.5-8-5-8-10V6l8-3Z"/><path d="m8.5 12 2.2 2.2 4.8-5"/>',
    radio: '<path d="M12 18v3M8 21h8M7 14a7 7 0 0 1 10 0M4 10a11 11 0 0 1 16 0M12 4h.01"/><circle cx="12" cy="15" r="1.5"/>',
    'file-image': '<path d="M6 3h8l4 4v14H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z"/><path d="M14 3v5h5M7 17l3-3 2 2 2-2 3 3"/><circle cx="9" cy="10" r="1"/>'
  };
  const iconSvg = (name) => `<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">${ICONS[name] || ICONS.spark}</svg>`;
  const esc = (value) => String(value ?? '').replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));

  function toast(message, type = '') {
    const region = $('#toastWrap');
    if (!region) return;
    const node = document.createElement('div');
    node.className = `toast ${type}`.trim();
    node.textContent = String(message || 'Selesai');
    region.appendChild(node);
    window.setTimeout(() => node.remove(), 3500);
  }

  async function api(url, options = {}) {
    const headers = { Accept: 'application/json', ...(options.headers || {}) };
    const response = await fetch(url, { ...options, headers });
    const raw = await response.text();
    let data;
    try { data = raw ? JSON.parse(raw) : {}; }
    catch { throw new Error(`Server mengirim respons tidak valid (HTTP ${response.status}).`); }
    if (!response.ok || data.ok === false) throw new Error(data.message || data.output || `Permintaan gagal (HTTP ${response.status}).`);
    return data;
  }

  async function postJson(url, value) {
    return api(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(value) });
  }

  // Small synthesized click sound. The browser creates audio only after a user gesture.
  let audioContext = null;
  function playClick(force = false) {
    if (!force && !state.soundEnabled) return;
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      audioContext ||= new AudioCtx();
      if (audioContext.state === 'suspended') audioContext.resume();
      const osc = audioContext.createOscillator();
      const gain = audioContext.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(720, audioContext.currentTime);
      osc.frequency.exponentialRampToValueAtTime(480, audioContext.currentTime + .045);
      gain.gain.setValueAtTime(.0001, audioContext.currentTime);
      gain.gain.exponentialRampToValueAtTime(.035, audioContext.currentTime + .006);
      gain.gain.exponentialRampToValueAtTime(.0001, audioContext.currentTime + .055);
      osc.connect(gain); gain.connect(audioContext.destination);
      osc.start(); osc.stop(audioContext.currentTime + .06);
    } catch { /* audio is optional on browsers that restrict Web Audio */ }
  }
  document.addEventListener('pointerdown', (event) => {
    const target = event.target.closest('button, .tool-card, a');
    if (target && !target.disabled && target.id !== 'soundToggle') playClick();
  }, { passive: true });
  const soundToggle = $('#soundToggle');
  function syncSoundButton() {
    soundToggle?.setAttribute('aria-pressed', String(state.soundEnabled));
    soundToggle?.setAttribute('aria-label', state.soundEnabled ? 'Efek suara aktif' : 'Efek suara nonaktif');
    if (soundToggle) soundToggle.style.color = state.soundEnabled ? 'var(--accent)' : 'var(--subtle)';
  }
  soundToggle?.addEventListener('click', () => {
    state.soundEnabled = !state.soundEnabled;
    safeStorage.set('nx_sound_enabled', state.soundEnabled);
    syncSoundButton();
    if (state.soundEnabled) playClick(true);
    toast(state.soundEnabled ? 'Efek suara diaktifkan.' : 'Efek suara dimatikan.');
  });
  syncSoundButton();

  // Clock, device and network indicators.
  function updateClock() {
    const now = new Date();
    const clock = $('#liveClock');
    const date = $('#liveDate');
    if (clock) clock.textContent = now.toLocaleTimeString('id-ID', { hour12: false });
    if (date) date.textContent = now.toLocaleDateString('id-ID', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' });
  }
  updateClock(); window.setInterval(updateClock, 1000);
  function syncNetwork() {
    const online = navigator.onLine;
    $('#pillNetwork').textContent = online ? 'Perangkat online' : 'Perangkat offline';
    $('#netDot')?.classList.toggle('is-online', online);
    $('#netDot')?.classList.toggle('is-offline', !online);
  }
  syncNetwork(); window.addEventListener('online', syncNetwork); window.addEventListener('offline', syncNetwork);

  const heroVideo = $('#heroVideo');
  const heroFallback = $('#heroFallbackImg');
  if (heroVideo) {
    heroVideo.addEventListener('error', () => { heroVideo.style.display = 'none'; if (heroFallback) heroFallback.style.display = 'block'; }, true);
    heroVideo.addEventListener('loadeddata', () => { heroVideo.style.display = 'block'; if (heroFallback) heroFallback.style.display = 'none'; });
  }

  // Catalog rendering and filters.
  const categoryNames = { all: 'Semua tools', bots: 'Bot & Server', downloader: 'Downloader', maker: 'Creator Studio', tools: 'Utilities' };
  const grid = $('#RyomdToolsGrid');
  function filteredTools() {
    const query = state.search.toLocaleLowerCase('id-ID');
    return TOOLS.filter(tool => (state.category === 'all' || tool.cat === state.category) && (!query || `${tool.title} ${tool.desc} ${tool.cat} ${tool.tag}`.toLocaleLowerCase('id-ID').includes(query)));
  }
  function renderTools() {
    const filtered = filteredTools();
    const filteredMode = state.category !== 'all' || state.search.length > 0;
    const shown = filteredMode ? filtered : filtered.slice(0, state.visibleLimit);
    $('#catalogTitle').textContent = categoryNames[state.category] || 'Semua tools';
    $('#catalogVisibleCount').textContent = String(shown.length);
    $('#heroToolCount').textContent = String(TOOLS.length);
    $('#downloaderHistoryBox').hidden = state.category !== 'downloader';
    const more = $('#expandToolsBtn');
    more.hidden = filteredMode || shown.length >= filtered.length;
    $('#expandCounter').textContent = `${shown.length} / ${filtered.length}`;
    if (!shown.length) {
      grid.innerHTML = `<div class="empty-state">Tidak ada tools yang cocok dengan pencarian <b>${esc(state.search)}</b>.</div>`;
      return;
    }
    grid.innerHTML = shown.map(tool => `
      <button class="tool-card" type="button" data-open-tool="${esc(tool.id)}" data-cat="${esc(tool.cat)}" aria-label="Buka ${esc(tool.title)}">
        <div class="tool-card-top"><span class="tool-icon">${iconSvg(tool.icon)}</span><span class="tool-tag ${tool.tag === 'LIVE' || tool.tag === 'BOT' ? 'tag-live' : tool.tag === 'UPDATE' || tool.tag === 'MEDIA' ? 'tag-new' : ''}">${esc(tool.tag)}</span></div>
        <div class="tool-card-copy"><h3>${esc(tool.title)}</h3><p>${esc(tool.desc)}</p></div><span class="tool-card-arrow" aria-hidden="true">↗</span>
      </button>`).join('');
  }
  $$('#categoryTabs [data-cat]').forEach(button => button.addEventListener('click', () => {
    state.category = button.dataset.cat;
    $$('#categoryTabs [data-cat]').forEach(tab => { tab.classList.toggle('active', tab === button); tab.setAttribute('aria-selected', String(tab === button)); });
    state.visibleLimit = 12;
    renderTools();
  }));
  const searchInput = $('#toolSearchInput');
  searchInput.addEventListener('input', () => {
    state.search = searchInput.value.trim();
    $('#clearSearchBtn').hidden = !state.search;
    renderTools();
  });
  $('#clearSearchBtn').addEventListener('click', () => { searchInput.value = ''; state.search = ''; $('#clearSearchBtn').hidden = true; renderTools(); searchInput.focus(); });
  $('#expandToolsBtn').addEventListener('click', () => { state.visibleLimit += 12; renderTools(); });

  // Downloader history.
  function renderHistory() {
    const items = state.history.filter(item => state.historyFilter === 'all' || item.type === state.historyFilter);
    const list = $('#dlHistoryList');
    if (!items.length) { list.textContent = 'Belum ada riwayat unduhan.'; return; }
    list.innerHTML = items.slice(0, 30).map(item => `<div class="history-row"><span>${esc(item.title)}<small> · ${new Date(item.at || Date.now()).toLocaleString('id-ID')}</small></span><span>${esc(item.type)}</span></div>`).join('');
  }
  $$('.history-filter').forEach(button => button.addEventListener('click', () => {
    state.historyFilter = button.dataset.hfilt;
    $$('.history-filter').forEach(item => item.classList.toggle('active', item === button));
    renderHistory();
  }));
  $('#clearDlHistoryBtn').addEventListener('click', () => { state.history = []; safeStorage.set('nx_dl_history', []); renderHistory(); toast('Riwayat unduhan dibersihkan.', 'success'); });

  // Backend health & system snapshot.
  async function syncServerState() {
    try {
      const [system, services] = await Promise.all([api('/api/system'), api('/api/services')]);
      state.services = Array.isArray(services.services) ? services.services : [];
      const active = state.services.filter(service => service.status === 'Running').length;
      const total = state.services.length || 4;
      $('#pillSvcText').textContent = `${active}/${total} layanan aktif`;
      $('#metricServices').textContent = `${active} / ${total}`;
      $('#dropBotStatus').textContent = state.services.find(s => s.id === 'whatsapp-bot')?.status === 'Running' ? 'WhatsApp sedang berjalan' : 'WhatsApp berhenti';
      $('#dropSysStatus').textContent = `CPU ${system.cpu.percent}% · RAM ${system.memory.percent}%`;
      $('#metricCpu').textContent = String(system.cpu.percent ?? 0);
      $('#metricCpuBar').style.width = `${Math.max(0, Math.min(100, Number(system.cpu.percent) || 0))}%`;
      $('#metricRam').textContent = Number(system.memory.usedGb || 0).toFixed(1);
      $('#metricRamSub').textContent = `dari ${Number(system.memory.totalGb || 0).toFixed(1)} GB (${system.memory.percent || 0}%)`;
      const indicator = $('#globalServiceIndicator');
      indicator.classList.add('is-online'); indicator.classList.remove('is-offline');
      $('#backendHint').textContent = 'Server lokal merespons';
      $('#pillSvcText').title = `${active} dari ${total} layanan berjalan`;
    } catch (error) {
      const indicator = $('#globalServiceIndicator');
      indicator.classList.remove('is-online'); indicator.classList.add('is-offline');
      $('#pillSvcText').textContent = 'Backend terputus';
      $('#backendHint').textContent = 'Periksa server.js dan alamat dashboard';
      $('#metricServices').textContent = '— / 4';
    }
  }

  // Header dropdown.
  const menuButton = $('#menuInfoBtn');
  const dropdown = $('#RyomdDropdown');
  function closeDropdown() { dropdown.classList.remove('open'); menuButton.setAttribute('aria-expanded', 'false'); }
  menuButton.addEventListener('click', event => { event.stopPropagation(); dropdown.classList.toggle('open'); menuButton.setAttribute('aria-expanded', String(dropdown.classList.contains('open'))); });
  document.addEventListener('click', event => { if (!dropdown.contains(event.target) && !menuButton.contains(event.target)) closeDropdown(); });

  // Modal lifecycle and tool launch.
  const backdrop = $('#toolModalBackdrop');
  const modalBody = $('#modalToolBody');
  const modalTitle = $('#modalToolTitle');
  const modalSub = $('#modalToolSub');
  const modalIcon = $('#modalToolIcon');
  let telemetryRefreshTimer = null;

  function stopTelemetryRefresh() {
    if (telemetryRefreshTimer) {
      window.clearInterval(telemetryRefreshTimer);
      telemetryRefreshTimer = null;
    }
  }

  function closeModal() {
    stopTelemetryRefresh();
    state.modalRequest++;
    backdrop.classList.remove('open');
    backdrop.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
    const animeFrame = document.getElementById('ryomdAnimeFrame');
    if (animeFrame) animeFrame.src = 'about:blank';
    const animeModal = backdrop.querySelector('.tool-modal');
    if (animeModal) animeModal.classList.remove('ryomd-anime-expanded');
  }
  $('#closeToolModalBtn').addEventListener('click', closeModal);
  backdrop.addEventListener('click', event => { if (event.target === backdrop) closeModal(); });
  document.addEventListener('keydown', event => { if (event.key === 'Escape') { closeModal(); closeDropdown(); } });
  $('#floatingAiBtn').addEventListener('click', () => openTool('ryo-ai'));
  $('#heroUploadBtn').addEventListener('click', () => openTool('banner-studio'));
  $('#dropAboutBtn').addEventListener('click', () => openTool('about'));

  document.addEventListener('click', event => {
    const trigger = event.target.closest('[data-open-tool]');
    if (!trigger) return;
    event.preventDefault();
    closeDropdown();
    openTool(trigger.dataset.openTool);
  });

  function modalIntro(text) { return `<p class="tool-intro">${esc(text)}</p>`; }
  function button(label, action, cls = '') { return `<button type="button" class="tool-btn ${cls}" data-action="${esc(action)}">${label}</button>`; }

  // RYOMD_WHATSAPP_CONTROL_PREMIUM_V1
  function serviceCard(service) {
    const running = service.status === 'Running';
    return `<article class="tool-card-panel wa-card ${running?'is-running':'is-stopped'}" data-service-card="${esc(service.id)}">
      <div class="wa-card-top"><span>RYOMD SERVICE / WHATSAPP</span><b class="wa-state ${running?'online':'offline'}"><i></i>${esc(service.status||'Unknown')}</b></div>
      <div class="wa-identity"><div class="wa-avatar">◉</div><div><h3>${esc(service.name)}</h3><p>WhatsApp automation · Node.js runtime</p></div></div>
      <div class="wa-metrics"><div><small>ENTRY POINT</small><strong>${esc(service.entry||'—')}</strong></div><div><small>PROCESS ID</small><strong class="wa-pid">${esc(service.pid||'—')}</strong></div></div>
      <div class="wa-folder"><small>WORKSPACE DIRECTORY</small><code>${esc(service.folder||'Folder belum ditentukan')}</code></div>
      <div class="tool-actions wa-actions">${button('▶ Start','service:start:'+service.id,'primary')}${button('↻ Restart','service:restart:'+service.id)}${button('■ Stop','service:stop:'+service.id,'danger')}${button('⌁ Live log','service:log:'+service.id,'teal')}</div>
    </article>`;
  }
  async function renderServices(toolId) {
    const data = await api('/api/services');
    state.services = data.services || [];
    const list = toolId === 'wa-bot' ? state.services.filter(service => service.id === 'whatsapp-bot') : state.services;
    const active = list.filter(service => service.status === 'Running').length;
    const title = toolId === 'wa-bot' ? 'WhatsApp Control Desk' : 'Service Control Desk';
    modalBody.innerHTML = `<section class="wa-view">
      <header class="wa-hero"><div><span>RYOMD / OPERATIONS</span><h2>${title}</h2><p>Pantau proses bot, kendalikan runtime, dan periksa output layanan dari satu tempat.</p></div><aside><small>ACTIVE SERVICES</small><b>${active}<i> / ${list.length}</i></b><em>● ${active?'Runtime active':'No active runtime'}</em></aside></header>
      <div class="wa-section"><div><small>RUNTIME MANAGEMENT</small><h3>Service instance</h3></div><span>${list.length} SERVICE${list.length===1?'':'S'}</span></div>
      <div class="tool-stack wa-list">${list.map(serviceCard).join('')||'<div class="tool-card-panel">Data layanan belum tersedia.</div>'}</div>
      <section class="tool-card-panel wa-log"><div class="wa-log-head"><div><small>DIAGNOSTICS / STREAM</small><h3>Output layanan</h3><p>Output proses dari live log.</p></div>${button('⟳ Refresh status','service:refresh')}</div><div class="wa-terminal-head"><span>● ● ●</span><b>RYOMD TERMINAL</b><small>LOCAL OUTPUT</small></div><pre class="tool-pre wa-log-output" id="svcLiveLogBox">Pilih tombol Live log untuk membaca output.</pre></section>
    </section>`;
  }

  // RYOMD_DEPLOYMENT_CENTER_UI_V1
  function renderZipDeploy() {
    state.zipServiceId = 'whatsapp-bot';
    state.zipMode = 'source';

    modalBody.innerHTML = modalIntro('Kelola pembaruan source bot dan plugin WhatsApp melalui satu panel deployment.') +
      '<section class="deploy-ui">' +
        '<header class="deploy-hero">' +
          '<div class="deploy-hero-copy">' +
            '<span class="deploy-eyebrow">RYOMD / RELEASE MANAGEMENT</span>' +
            '<h2>Bot &amp; Plugin Updater</h2>' +
            '<p>Deploy source bot atau plugin dengan perlindungan file penting dan backup otomatis.</p>' +
            '<span class="deploy-safe-label"><i></i> PROTECTED DEPLOYMENT</span>' +
          '</div>' +
          '<div class="deploy-hero-symbol" aria-hidden="true"><span>⬆</span><small>DEPLOY</small></div>' +
        '</header>' +

        '<div class="deploy-section-heading"><div><small>STEP 01</small><h3>Pilih jenis paket</h3><p>Tentukan materi pembaruan yang akan dipasang.</p></div><span class="deploy-step-count">01 / 03</span></div>' +

        '<div class="deploy-mode-card">' +
          '<label for="zipModeSelect">JENIS DEPLOYMENT</label>' +
          '<select class="tool-select deploy-select" id="zipModeSelect">' +
            '<option value="source">Pembaruan source bot</option>' +
            '<option value="plugin">Plugin WhatsApp (CommonJS)</option>' +
          '</select>' +
          '<div class="deploy-mode-hint"><span class="deploy-hint-icon">↗</span><span id="deployModeHint">Perbarui file source layanan tanpa mengganti konfigurasi dan sesi.</span></div>' +
        '</div>' +

        '<div id="botSourceOptions" class="deploy-option-panel">' +
          '<div class="deploy-section-heading"><div><small>STEP 02</small><h3>Layanan tujuan</h3><p>Pilih lokasi pemasangan paket source.</p></div><span class="deploy-step-count">02 / 03</span></div>' +
          '<div class="deploy-mode-card">' +
            '<label for="zipServiceSelect">TARGET SERVICE</label>' +
            '<select class="tool-select deploy-select" id="zipServiceSelect">' +
              '<option value="whatsapp-bot">WhatsApp Bot RYOMD</option>' +
              '<option value="telegram-bot">Telegram Bot</option>' +
              '<option value="hermes">Hermes module</option>' +
              '<option value="rvc">RVC Voice Studio</option>' +
            '</select>' +
          '</div>' +
          '<div class="deploy-section-heading deploy-structure-heading"><div><small>PACKAGE GUIDE</small><h3>Struktur paket source</h3><p>Pastikan entry point tersedia di root ZIP.</p></div></div>' +
          '<div class="deploy-target-grid">' +
            '<article class="deploy-target-card"><span class="deploy-target-icon wa">WA</span><div><b>WhatsApp</b><small><code>bot.js</code> + source pendukung</small></div></article>' +
            '<article class="deploy-target-card"><span class="deploy-target-icon tg">TG</span><div><b>Telegram</b><small><code>bot.py</code> atau <code>main.py</code></small></div></article>' +
            '<article class="deploy-target-card"><span class="deploy-target-icon hm">HM</span><div><b>Hermes</b><small><code>main.py</code> / source modul</small></div></article>' +
            '<article class="deploy-target-card"><span class="deploy-target-icon rv">RVC</span><div><b>RVC Studio</b><small><code>app.py</code> + source aplikasi</small></div></article>' +
          '</div>' +
        '</div>' +

        '<div id="pluginOptions" class="deploy-option-panel" hidden>' +
          '<div class="deploy-section-heading"><div><small>PLUGIN PACKAGE</small><h3>WhatsApp CommonJS</h3><p>Aturan struktur dan batas ukuran plugin.</p></div><span class="deploy-step-count">PLUGIN</span></div>' +
          '<div class="deploy-plugin-card">' +
            '<div class="deploy-plugin-top"><span class="deploy-plugin-icon">{ }</span><div><b>Plugin package</b><small>JavaScript · CommonJS</small></div><span class="deploy-format-badge">.JS</span></div>' +
            '<p>ZIP harus berisi file <code>kategori/nama.js</code> atau <code>plugins/kategori/nama.js</code>.</p>' +
            '<div class="deploy-limit-grid"><div><b>30</b><small>FILE MAKS.</small></div><div><b>200 KB</b><small>PER PLUGIN</small></div><div><b>3 MB</b><small>TOTAL</small></div></div>' +
            '<div class="deploy-plugin-note">Setiap plugin harus mengekspor <code>command</code> dan <code>run</code>. Plugin serta perintah inti dilindungi.</div>' +
          '</div>' +
        '</div>' +

        '<div class="deploy-section-heading deploy-upload-heading"><div><small>STEP 03</small><h3>Upload paket</h3><p>Pilih file ZIP dari penyimpanan perangkat.</p></div><span class="deploy-step-count">03 / 03</span></div>' +
        '<div class="deploy-upload-card">' +
          '<div class="deploy-upload-icon">↑</div>' +
          '<b>Siapkan paket deployment</b>' +
          '<p>Gunakan file ZIP sesuai jenis paket dan layanan tujuan.</p>' +
          '<div class="tool-actions deploy-upload-actions">' +
            button('Pilih file ZIP', 'choose-bot-zip', 'primary') +
          '</div>' +
          '<span class="deploy-upload-caption">SOURCE / PLUGIN · ZIP ARCHIVE</span>' +
          '<div class="tool-output deploy-upload-status" id="botZipStatus" role="status" aria-live="polite"></div>' +
        '</div>' +

        '<div class="deploy-protection">' +
          '<span class="deploy-lock">◇</span>' +
          '<div><b>Protected files &amp; automatic backup</b>' +
            '<p>Deployer mencadangkan file yang diganti dan melewati <code>.env</code>, kredensial, <code>package.json</code>, lockfile, database, sesi, token, model, log, serta dependensi.</p>' +
          '</div>' +
        '</div>' +
        '<p class="deploy-footer-note">Setelah plugin dipasang, gunakan perintah <code>.reloadfitur</code> di WhatsApp untuk memuat ulang plugin tanpa restart bot.</p>' +
      '</section>';

    $('#zipModeSelect', modalBody).addEventListener('change', event => {
      state.zipMode = event.target.value;
      $('#botSourceOptions', modalBody).hidden = state.zipMode !== 'source';
      $('#pluginOptions', modalBody).hidden = state.zipMode !== 'plugin';
      const hint = $('#deployModeHint', modalBody);
      if (hint) {
        hint.textContent = state.zipMode === 'plugin'
          ? 'Pasang plugin WhatsApp dengan format CommonJS yang didukung.'
          : 'Perbarui file source layanan tanpa mengganti konfigurasi dan sesi.';
      }
    });

    $('#zipServiceSelect', modalBody).addEventListener('change', event => {
      state.zipServiceId = event.target.value;
    });
  }


  function renderAppUpdate() {
    modalBody.innerHTML = modalIntro('Kelola pembaruan antarmuka dan pemasangan modul Ryomd.') +
      `<div class="au-center">
        <header class="au-center-hero">
          <div class="au-center-hero-icon">↻</div>
          <div class="au-center-hero-copy">
            <span class="au-center-kicker">RYOMD SYSTEM · UPDATE CONTROL</span>
            <h3>App Update Center</h3>
            <p>Kelola antarmuka dan modul dengan jalur pemasangan yang terpisah dan tervalidasi.</p>
          </div>
          <span class="au-center-protected">● PROTECTED</span>
        </header>

        <section class="au-center-card au-center-card-ui">
          <div class="au-center-card-head">
            <span class="au-center-number">01</span>
            <div class="au-center-card-title">
              <span class="au-center-kicker">INTERFACE PACKAGE</span>
              <h4>Pembaruan antarmuka</h4>
            </div>
            <span class="au-center-tag au-center-tag-ui">UI ONLY</span>
          </div>
          <p class="au-center-description">
            Pasang paket ZIP untuk memperbarui tampilan aplikasi. Hanya file antarmuka yang ada dalam daftar izin yang dapat diganti.
          </p>

          <div class="au-center-file-list">
            <div class="au-center-file"><span>HTML</span><code>public/index.html</code></div>
            <div class="au-center-file"><span>CSS</span><code>public/styles.css</code></div>
            <div class="au-center-file"><span>JS</span><code>public/app.js</code></div>
          </div>

          <div class="au-center-optional">
            <span class="au-center-optional-label">FILE OPSIONAL</span>
            <span>Tema</span><span>Pembaca komik</span><span>Ikon Ryomd</span>
          </div>

          <div class="au-center-actions">
            ${button('Pilih paket update aplikasi (.zip)', 'choose-app-zip', 'primary')}
          </div>

          <div class="au-center-security">
            <span class="au-center-security-icon">✓</span>
            <div><strong>Area inti terlindungi</strong><small>Tidak mengubah server.js, kredensial, database, konfigurasi, atau sesi bot.</small></div>
          </div>
        </section>

        <section class="au-center-card au-center-card-module">
          <div class="au-center-card-head">
            <span class="au-center-number">02</span>
            <div class="au-center-card-title">
              <span class="au-center-kicker">MODULE INSTALLATION</span>
              <h4>Pemasangan modul</h4>
            </div>
            <span class="au-center-tag au-center-tag-module">MODULE</span>
          </div>
          <p class="au-center-description">
            Pasang Crypto Market melalui jalur modul khusus. Manifest dan setiap file akan divalidasi secara terpisah.
          </p>

          <div class="au-center-package">
            <span class="au-center-package-icon">◇</span>
            <div><strong>Crypto Market Patch v1</strong><small>RYOMD_CRYPTO_MARKET_PATCH_v1.zip</small></div>
            <span class="au-center-size">MAX 8 MB</span>
          </div>

          <div class="au-center-module-upload">
            <label for="moduleZipInput" class="au-center-module-pick">
              <span class="au-center-upload-icon">＋</span>
              <span><strong>Pilih ZIP Modul Crypto Market</strong><small>Ketuk untuk memilih file .zip</small></span>
            </label>
            <input id="moduleZipInput" type="file" accept=".zip,application/zip" hidden>
          </div>

          <div class="tool-output au-center-status" id="moduleInstallStatus">Menunggu paket modul dipilih.</div>

          <div class="au-center-security au-center-security-module">
            <span class="au-center-security-icon">✓</span>
            <div><strong>Instalasi terisolasi</strong><small>Hanya file modul Crypto Market yang diizinkan. File inti, konfigurasi, dan sesi bot tidak ikut diganti.</small></div>
          </div>
        </section>
      </div>`;

    const moduleInput = $('#moduleZipInput', modalBody);
    const moduleStatus = $('#moduleInstallStatus', modalBody);

    moduleInput.addEventListener('change', async event => {
      const file = event.target.files?.[0];
      if (!file) return;

      if (!/\.zip$/i.test(file.name)) {
        toast('Pilih file ZIP modul.', 'error');
        event.target.value = '';
        return;
      }

      if (file.size > 8 * 1024 * 1024) {
        toast('Paket modul maksimal 8 MB.', 'error');
        event.target.value = '';
        return;
      }

      moduleStatus.classList.remove('is-error', 'is-success');
      moduleStatus.classList.add('is-pending');
      moduleStatus.textContent = 'Mengunggah dan memvalidasi paket modul...';

      try {
        const response = await fetch('/api/module-install', {
          method: 'POST',
          headers: { 'Content-Type': 'application/zip' },
          body: file
        });

        const raw = await response.text();
        let data;
        try { data = JSON.parse(raw); }
        catch { throw new Error('Respons server pemasangan modul tidak valid.'); }

        if (!response.ok || !data.ok) {
          throw new Error(data.message || 'Pemasangan modul gagal.');
        }

        moduleStatus.classList.remove('is-pending', 'is-error');
        moduleStatus.classList.add('is-success');
        moduleStatus.textContent = data.message || 'Modul berhasil dipasang.';
        toast('Modul Crypto Market berhasil dipasang.', 'success');
      } catch (error) {
        moduleStatus.classList.remove('is-pending', 'is-success');
        moduleStatus.classList.add('is-error');
        moduleStatus.textContent = error.message;
        toast(error.message, 'error');
      } finally {
        event.target.value = '';
      }
    });
  }

  function renderEnvConfig() {
    const options = [
      ['whatsapp-bot', 'WhatsApp Bot', 'bot.js'],
      ['telegram-bot', 'Telegram Bot', 'bot.py'],
      ['hermes-agent', 'Hermes Agent', 'main.py'],
      ['rvc-studio', 'RVC Studio', 'app.py']
    ];

    modalBody.innerHTML = `${modalIntro('Atur entry point dan file environment layanan dengan perlindungan kredensial.') }
      <div class="ec-manager">

        <header class="ec-hero">
          <div class="ec-hero-icon">⚙</div>
          <div class="ec-hero-copy">
            <span class="ec-kicker">RYOMD SYSTEM · SERVICE CONFIG</span>
            <h3>Entry Config Manager</h3>
            <p>Konfigurasi layanan dan environment dalam satu panel terkontrol.</p>
          </div>
          <span class="ec-writeonly">🔒 WRITE ONLY</span>
        </header>

        <section class="ec-section">
          <div class="ec-section-heading">
            <span class="ec-step">01</span>
            <div>
              <span class="ec-kicker">SERVICE SELECTION</span>
              <h4>Pilih layanan</h4>
            </div>
          </div>

          <div class="ec-field-box">
            <label for="configService">Layanan yang dikonfigurasi</label>
            <select class="tool-select ec-select" id="configService">
              ${options.map(([id, name]) => `<option value="${id}">${name}</option>`).join('')}
            </select>
            <small>Pilih layanan untuk mengisi nama file entry default secara otomatis.</small>
          </div>

          <div class="ec-field-box">
            <label for="configEntry">Nama file entry point</label>
            <div class="ec-entry-input">
              <span class="ec-file-prefix">JS / PY</span>
              <input class="tool-input" id="configEntry" value="bot.js" maxlength="80" placeholder="bot.js" autocomplete="off" spellcheck="false">
            </div>
            <small>Gunakan nama file yang benar-benar tersedia di folder layanan.</small>
          </div>
        </section>

        <section class="ec-section ec-env-section">
          <div class="ec-section-heading">
            <span class="ec-step ec-step-gold">02</span>
            <div>
              <span class="ec-kicker">ENVIRONMENT VARIABLES</span>
              <h4>Environment (.env)</h4>
            </div>
            <span class="ec-optional">OPSIONAL</span>
          </div>

          <div class="ec-secret-banner">
            <span class="ec-lock-icon">🔐</span>
            <div>
              <strong>Protected environment input</strong>
              <small>Isi file lama tidak pernah dibaca atau ditampilkan oleh panel.</small>
            </div>
          </div>

          <div class="ec-editor">
            <div class="ec-editor-bar">
              <div class="ec-editor-dots"><i></i><i></i><i></i></div>
              <span>.env · secure editor</span>
              <b>WRITE ONLY</b>
            </div>
            <textarea class="tool-textarea ec-env-editor" id="configEnvInput" rows="7" spellcheck="false" autocomplete="off" autocapitalize="off" placeholder="BOT_TOKEN=isi_token_di_sini&#10;API_KEY=isi_api_key_di_sini&#10;# Baris komentar diperbolehkan"></textarea>
            <div class="ec-editor-foot">
              <span>KEY=VALUE</span>
              <span>BATAS 16 KB</span>
            </div>
          </div>

          <div class="ec-env-hint">
            <span>i</span>
            <p>Tempel variabel dalam format <code>KEY=VALUE</code>. Baris komentar diperbolehkan. Kolom kosong tidak mengubah file <code>.env</code> yang sudah ada.</p>
          </div>
        </section>

        <section class="ec-default-panel">
          <div class="ec-default-title">ENTRY DEFAULT LAYANAN</div>
          <div class="ec-default-grid">
            <div><span>WhatsApp</span><code>bot.js</code></div>
            <div><span>Telegram</span><code>bot.py</code></div>
            <div><span>Hermes</span><code>main.py</code></div>
            <div><span>RVC</span><code>app.py</code></div>
          </div>
          <p>Pastikan file entry tersedia di folder layanan sebelum menjalankan bot.</p>
        </section>

        <div class="ec-save-area">
          ${button('Simpan konfigurasi', 'save-entry-config', 'primary')}
          <div class="ec-save-caption"><span>✓</span> Backup otomatis · Izin file privat</div>
        </div>

        <div class="tool-output ec-config-status" id="configStatus">Menunggu perubahan konfigurasi.</div>

      </div>`;

    $('#configService', modalBody).addEventListener('change', event => {
      $('#configEntry', modalBody).value =
        options.find(option => option[0] === event.target.value)?.[2] || 'index.js';
    });
  }

  function renderTerminal() {
    const commands = [
      {
        command: 'pwd',
        title: 'Lokasi kerja',
        description: 'Periksa direktori kerja panel saat ini.',
        number: '01',
        category: 'SYSTEM'
      },
      {
        command: 'pm2 status',
        title: 'Status proses',
        description: 'Lihat proses layanan yang dikelola PM2.',
        number: '02',
        category: 'PROCESS'
      },
      {
        command: 'ls -la',
        title: 'Daftar direktori',
        description: 'Lihat file dan folder pada direktori kerja.',
        number: '03',
        category: 'FILES'
      },
      {
        command: 'ls -la modules/whatsapp/plugins',
        title: 'WhatsApp Plugins',
        description: 'Periksa daftar plugin WhatsApp yang tersedia.',
        number: '04',
        category: 'WHATSAPP'
      },
      {
        command: 'ls -lh public/assets',
        title: 'Aset antarmuka',
        description: 'Periksa aset UI dan ukuran setiap file.',
        number: '05',
        category: 'ASSETS'
      }
    ];

    modalBody.innerHTML = modalIntro('Terminal diagnostik dengan perintah terbatas dan aman untuk pemeriksaan layanan.') +
      `<div class="dt-console">

        <header class="dt-hero">
          <div class="dt-hero-icon">⌘</div>
          <div class="dt-hero-copy">
            <span class="dt-kicker">RYOMD SYSTEM · DIAGNOSTICS</span>
            <h3>Diagnostic Terminal</h3>
            <p>Pusat pemeriksaan sistem, proses, plugin, dan aset aplikasi.</p>
          </div>
          <span class="dt-live"><i></i> READY</span>
        </header>

        <div class="dt-safety">
          <span class="dt-safety-icon">✓</span>
          <div>
            <strong>Restricted command environment</strong>
            <small>Shell bebas dan perintah penghapusan tidak tersedia. Hanya perintah yang diizinkan backend yang dapat dijalankan.</small>
          </div>
        </div>

        <section class="dt-command-section">
          <div class="dt-section-heading">
            <div>
              <span class="dt-kicker">AVAILABLE OPERATIONS</span>
              <h4>Perintah diagnostik</h4>
            </div>
            <span class="dt-count">05 COMMANDS</span>
          </div>

          <div class="dt-command-list">
            ${commands.map(item => `
              <article class="dt-command-card">
                <div class="dt-command-top">
                  <span class="dt-command-number">${item.number}</span>
                  <span class="dt-command-category">${item.category}</span>
                </div>
                <div class="dt-command-info">
                  <strong>${esc(item.title)}</strong>
                  <small>${esc(item.description)}</small>
                </div>
                <code class="dt-command-code">${esc(item.command)}</code>
                <div class="dt-command-action">
                  ${button('Jalankan diagnostik', `terminal:${item.command}`, 'primary')}
                </div>
              </article>
            `).join('')}
          </div>
        </section>

        <section class="dt-output-panel">
          <div class="dt-output-heading">
            <div>
              <span class="dt-kicker">EXECUTION CONSOLE</span>
              <h4>Output terminal</h4>
            </div>
            <span class="dt-output-ready"><i></i> OUTPUT</span>
          </div>

          <div class="dt-terminal-bar">
            <div class="dt-terminal-dots"><i></i><i></i><i></i></div>
            <span>ryomd-diagnostics</span>
            <b>READ / EXECUTE</b>
          </div>

          <pre class="tool-pre dt-terminal-output" id="nxTermOut">Pilih salah satu perintah diagnostik untuk melihat hasilnya.</pre>
          <div class="dt-output-foot">
            <span>RYOMD LOCAL TERMINAL</span>
            <span>CONTROLLED ACCESS</span>
          </div>
        </section>

        <div class="dt-footer-note">
          <span>i</span>
          <p>Output ditampilkan di area terminal ini. Jika perintah gagal, periksa status layanan dan izin akses folder.</p>
        </div>

      </div>`;
  }

  async function loadFiles(filePath = '.') {
    const output = $('#nxFilePreview', modalBody);
    const list = $('#nxFilesList', modalBody);
    const current = $('#nxFilePath', modalBody);

    if (list) list.textContent = 'Memuat isi workspace...';

    try {
      const data = await api(`/api/files?path=${encodeURIComponent(filePath)}`);

      if (data.type === 'file') {
        if (output) output.textContent = data.content || '(File kosong)';
        return;
      }

      if (current) current.textContent = data.path || filePath;
      if (output) output.textContent = 'Pilih file teks untuk membaca pratinjau.';

      if (list) {
        list.innerHTML =
          `<button type="button" class="file-entry wx-entry wx-entry-parent"
            data-file-path="${esc(filePath === '.' ? '.' : filePath.split('/').slice(0,-1).join('/') || '.')}">
            <span class="wx-entry-icon">↰</span>
            <span class="wx-entry-name">Folder sebelumnya</span>
            <span class="wx-entry-kind">UP</span>
          </button>` +
          (data.items || []).map(item => {
            const next = filePath === '.' ? item.name : `${filePath}/${item.name}`;
            const isDir = item.type === 'dir';

            return `<button type="button"
              class="file-entry wx-entry ${isDir ? 'wx-entry-folder' : 'wx-entry-file'}"
              data-file-path="${esc(next)}">
              <span class="wx-entry-icon">${isDir ? '▰' : '▤'}</span>
              <span class="wx-entry-name">${esc(item.name)}</span>
              <span class="wx-entry-kind">${isDir ? 'FOLDER' : 'FILE'}</span>
            </button>`;
          }).join('');
      }
    } catch (error) {
      if (list) list.textContent = error.message;
    }
  }

  function renderFileExplorer() {
    modalBody.innerHTML = `${modalIntro('Jelajahi file workspace dan baca pratinjau teks yang aman. Folder sensitif tetap dibatasi oleh backend.')}

      <div class="wx-explorer">

        <header class="wx-hero">
          <div class="wx-hero-icon">▰</div>
          <div class="wx-hero-copy">
            <span class="wx-kicker">RYOMD SYSTEM · FILE MANAGEMENT</span>
            <h3>Workspace Explorer</h3>
            <p>Jelajahi direktori proyek dan periksa file tanpa meninggalkan panel.</p>
          </div>
          <span class="wx-protected">● PROTECTED</span>
        </header>

        <div class="wx-scope-notice">
          <span class="wx-scope-icon">✓</span>
          <div>
            <strong>Workspace scope protection</strong>
            <small>Folder .env, node_modules, session, dan database tidak dapat dibuka. Akses tetap divalidasi oleh backend.</small>
          </div>
        </div>

        <section class="wx-browser">
          <div class="wx-location">
            <span class="wx-location-icon">⌂</span>
            <div class="wx-location-info">
              <span class="wx-kicker">CURRENT WORKSPACE PATH</span>
              <code id="nxFilePath">.</code>
            </div>
            ${button('Root', 'files:root')}
          </div>

          <div class="wx-section-heading">
            <div>
              <span class="wx-kicker">PROJECT CONTENTS</span>
              <h4>File &amp; folder</h4>
            </div>
            <span class="wx-scope-tag">PROJECT ONLY</span>
          </div>

          <div class="file-list wx-file-list" id="nxFilesList">
            <div class="wx-loading">Memuat isi workspace...</div>
          </div>
        </section>

        <section class="wx-preview-panel">
          <div class="wx-preview-heading">
            <div class="wx-preview-icon">⌘</div>
            <div class="wx-preview-title">
              <span class="wx-kicker">SAFE TEXT VIEWER</span>
              <h4>Pratinjau file</h4>
            </div>
            <span class="wx-readonly">READ ONLY</span>
          </div>

          <div class="wx-editor-bar">
            <div class="wx-editor-dots"><i></i><i></i><i></i></div>
            <span>workspace-preview</span>
            <b>TEXT</b>
          </div>

          <pre class="tool-pre wx-file-preview" id="nxFilePreview">Pilih file teks untuk membaca pratinjau.</pre>

          <div class="wx-preview-footer">
            <span>LOCAL WORKSPACE</span>
            <span>SAFE PREVIEW</span>
          </div>
        </section>

        <div class="wx-footer-note">
          <span>i</span>
          <p>Pratinjau menggunakan teks yang dikirim backend. Explorer tidak menyediakan editor atau aksi penghapusan file.</p>
        </div>

      </div>`;

    loadFiles('.');
  }

  async function renderDependencies() {
    const data = await api('/api/dependencies');
    const deps = data.dependencies || [];
    const installedCount = deps.filter(dep => dep.installed).length;
    const missingCount = deps.length - installedCount;

    const iconMap = {
      'node.js': ['⬡', 'JAVASCRIPT RUNTIME'],
      'ffmpeg': ['♫', 'MEDIA ENGINE'],
      'python': ['Py', 'PYTHON RUNTIME'],
      'git': ['⑂', 'VERSION CONTROL'],
      'sqlite': ['▦', 'LOCAL DATABASE'],
      'yt-dlp': ['↓', 'MEDIA DOWNLOADER'],
      'unzip': ['▤', 'ARCHIVE UTILITY']
    };

    const getMeta = name =>
      iconMap[String(name || '').toLowerCase()] || ['◇', 'RUNTIME DEPENDENCY'];

    modalBody.innerHTML = `${modalIntro('Periksa dependensi runtime yang digunakan oleh tool Ryomd dan kelola pemasangannya melalui package manager Termux.')}

      <div class="dm-manager">

        <header class="dm-hero">
          <div class="dm-hero-icon">⬡</div>
          <div class="dm-hero-copy">
            <span class="dm-kicker">RYOMD SYSTEM · RUNTIME CONTROL</span>
            <h3>Dependency Manager</h3>
            <p>Pusat pemeriksaan runtime, versi paket, dan kebutuhan sistem aplikasi.</p>
          </div>
          <span class="dm-hero-tag">TERMUX</span>
        </header>

        <section class="dm-overview">
          <div class="dm-overview-heading">
            <div>
              <span class="dm-kicker">ENVIRONMENT HEALTH</span>
              <h4>Ringkasan dependensi</h4>
            </div>
            <span class="dm-health"><i></i> CHECKED</span>
          </div>

          <div class="dm-stats">
            <div class="dm-stat dm-stat-installed">
              <span class="dm-stat-icon">✓</span>
              <div><strong>${installedCount}</strong><small>Terpasang</small></div>
            </div>
            <div class="dm-stat dm-stat-missing">
              <span class="dm-stat-icon">＋</span>
              <div><strong>${missingCount}</strong><small>Belum terpasang</small></div>
            </div>
            <div class="dm-stat dm-stat-total">
              <span class="dm-stat-icon">▦</span>
              <div><strong>${deps.length}</strong><small>Total paket</small></div>
            </div>
          </div>

          <div class="dm-progress-track">
            <div class="dm-progress-fill" style="width:${deps.length ? Math.round(installedCount / deps.length * 100) : 0}%"></div>
          </div>
          <div class="dm-progress-caption">
            <span>Runtime readiness</span>
            <strong>${deps.length ? Math.round(installedCount / deps.length * 100) : 0}%</strong>
          </div>
        </section>

        <section class="dm-dependencies">
          <div class="dm-section-heading">
            <div>
              <span class="dm-kicker">PACKAGE INVENTORY</span>
              <h4>Daftar dependensi</h4>
            </div>
            <span class="dm-count">${deps.length} PACKAGES</span>
          </div>

          ${deps.length ? `<div class="dm-grid">
            ${deps.map(dep => {
              const [symbol, category] = getMeta(dep.name);
              const version = dep.version || (dep.installed ? 'Versi tidak tersedia' : 'Belum terdeteksi');

              return `<article class="dm-card ${dep.installed ? 'dm-card-installed' : 'dm-card-missing'}">
                <div class="dm-card-top">
                  <span class="dm-package-icon">${symbol}</span>
                  <span class="dm-category">${category}</span>
                </div>

                <div class="dm-card-title">
                  <h4>${esc(dep.name)}</h4>
                  <span class="dm-status ${dep.installed ? 'dm-status-ok' : 'dm-status-missing'}">
                    <i></i>${dep.installed ? 'Terpasang' : 'Belum terpasang'}
                  </span>
                </div>

                <div class="dm-version-box">
                  <span>VERSI TERDETEKSI</span>
                  <code title="${esc(version)}">${esc(version)}</code>
                </div>

                <div class="dm-card-action">
                  ${dep.installed
                    ? '<span class="dm-installed-label">✓ Runtime tersedia</span>'
                    : button(`Install ${esc(dep.name)}`, `install:${dep.pkg}`, 'primary')}
                </div>
              </article>`;
            }).join('')}
          </div>` : `<div class="dm-empty">
            <span>◇</span>
            <strong>Belum ada data dependensi</strong>
            <small>Backend tidak mengirim daftar paket runtime.</small>
          </div>`}
        </section>

        <div class="dm-security-note">
          <span>i</span>
          <p>Status diperoleh dari environment Termux. Instalasi hanya tersedia melalui aksi package manager yang sudah diizinkan backend.</p>
        </div>

      </div>`;
  }

  async function renderLogs() {
    const data = await api('/api/logs');
    const logs = Array.isArray(data.logs) ? data.logs : [];
    const activeLogs = logs.filter(log => String(log.content || '').trim()).length;
    const emptyLogs = logs.length - activeLogs;

    modalBody.innerHTML = `${modalIntro('Pantau output dashboard dan layanan dari satu pusat log. Data sensitif tetap disamarkan oleh backend sebelum ditampilkan.')}

      <div class="slc-center">

        <header class="slc-hero">
          <div class="slc-hero-icon">≋</div>
          <div class="slc-hero-copy">
            <span class="slc-kicker">RYOMD SYSTEM · LOG MONITORING</span>
            <h3>System Log Center</h3>
            <p>Pusat pemantauan aktivitas dashboard, WhatsApp, Telegram, dan layanan lainnya.</p>
          </div>
          <span class="slc-live"><i></i> LOCAL LOGS</span>
        </header>

        <section class="slc-overview">
          <div class="slc-overview-heading">
            <div>
              <span class="slc-kicker">LOG INVENTORY</span>
              <h4>Ringkasan log</h4>
            </div>
            <span class="slc-total">${logs.length} SOURCES</span>
          </div>

          <div class="slc-stats">
            <div class="slc-stat slc-stat-active">
              <span class="slc-stat-icon">▤</span>
              <div><strong>${activeLogs}</strong><small>Ada data</small></div>
            </div>
            <div class="slc-stat slc-stat-empty">
              <span class="slc-stat-icon">○</span>
              <div><strong>${emptyLogs}</strong><small>Kosong</small></div>
            </div>
            <div class="slc-stat slc-stat-all">
              <span class="slc-stat-icon">≋</span>
              <div><strong>${logs.length}</strong><small>Total log</small></div>
            </div>
          </div>
        </section>

        <section class="slc-log-section">
          <div class="slc-section-heading">
            <div>
              <span class="slc-kicker">SERVICE OUTPUT</span>
              <h4>Daftar log layanan</h4>
            </div>
            <span class="slc-section-tag">LIVE DATA</span>
          </div>

          <div class="slc-log-list">
            ${logs.length ? logs.map(log => {
              const content = String(log.content || '');
              const hasContent = content.trim().length > 0;
              const lineCount = hasContent ? content.split(/\r?\n/).length : 0;
              const source = String(log.source || 'unknown');
              const lowerSource = source.toLowerCase();
              const type = lowerSource.includes('whatsapp') ? 'whatsapp' :
                lowerSource.includes('telegram') ? 'telegram' :
                lowerSource.includes('hermes') ? 'hermes' :
                lowerSource.includes('rvc') ? 'rvc' : 'dashboard';
              const icon = type === 'whatsapp' ? '◉' :
                type === 'telegram' ? '➤' :
                type === 'hermes' ? '⌘' :
                type === 'rvc' ? '♫' : '▤';

              return `<article class="slc-log-card ${hasContent ? 'slc-has-data' : 'slc-empty-log'}">
                <div class="slc-log-head">
                  <span class="slc-log-icon slc-icon-${type}">${icon}</span>
                  <div class="slc-log-identity">
                    <h4>${esc(source)}.log</h4>
                    <small>${type.toUpperCase()} · ${hasContent ? `${lineCount} BARIS` : 'NO OUTPUT'}</small>
                  </div>
                  <span class="slc-status ${hasContent ? 'slc-status-active' : 'slc-status-empty'}">
                    <i></i>${hasContent ? 'Ada data' : 'Kosong'}
                  </span>
                </div>

                <div class="slc-terminal">
                  <div class="slc-terminal-bar">
                    <div class="slc-terminal-dots"><i></i><i></i><i></i></div>
                    <span>${esc(source)}.log</span>
                    <b>${hasContent ? 'OUTPUT' : 'EMPTY'}</b>
                  </div>
                  <pre class="slc-log-content">${esc(hasContent ? content : '(Kosong)')}</pre>
                </div>
              </article>`;
            }).join('') : `
              <div class="slc-no-logs">
                <span>≋</span>
                <strong>Belum ada sumber log</strong>
                <small>Backend belum mengirim data log layanan.</small>
              </div>
            `}
          </div>
        </section>

        <section class="slc-maintenance">
          <div class="slc-maintenance-heading">
            <span class="slc-maintenance-icon">⌫</span>
            <div>
              <strong>Dashboard log maintenance</strong>
              <small>Pembersihan hanya berlaku untuk log dashboard.</small>
            </div>
          </div>
          <div class="slc-maintenance-action">
            ${button('Bersihkan log dashboard', 'clear-logs', 'danger')}
          </div>
          <div class="slc-preserve-note">
            <span>✓</span>
            <p>Log WhatsApp tidak ikut dihapus. Log layanan lain mengikuti aturan backend yang sudah berlaku.</p>
          </div>
        </section>

        <div class="slc-security-note">
          <span>🔒</span>
          <p>Token, password, API key, dan authorization disamarkan oleh backend sebelum isi log dikirim ke antarmuka.</p>
        </div>

      </div>`;
  }

  async function renderTelemetry() {
    const requestId = state.modalRequest;
    stopTelemetryRefresh();

    let busy = false;

    const isActive = () =>
      requestId === state.modalRequest &&
      backdrop.classList.contains('open');

    const percentValue = value => {
      const n = Number(value);
      return Number.isFinite(n) ? Math.max(0, Math.min(100, n)) : 0;
    };

    const serviceMarkup = services => {
      return (services || []).map(service => {
        const running = service.status === 'Running';
        return `<div class="sm-service-row">
          <span class="sm-service-name">${esc(service.name)}</span>
          <span class="sm-service-status ${running ? 'is-running' : 'is-stopped'}">
            <i></i>${esc(service.status || 'Unknown')}
          </span>
        </div>`;
      }).join('');
    };

    const update = async initial => {
      if (busy || !isActive()) return;
      busy = true;

      try {
        const [system, serviceData] = await Promise.all([
          api('/api/system'),
          api('/api/services')
        ]);

        if (!isActive()) return;

        const services = serviceData.services || [];
        state.services = services;

        const cpu = system.cpu || {};
        const memory = system.memory || {};
        const storage = system.storage || {};
        const serviceSummary = system.services || {};

        const cpuPercent = percentValue(cpu.percent);
        const ramPercent = percentValue(memory.percent);
        const diskPercent = percentValue(storage.percent);
        const cpuReady = cpu.available === true;
        const cores = Math.max(1, Number(cpu.cores) || 1);
        const load = Number(cpu.load1m) || 0;

        const cpuText = cpuReady ? `${cpuPercent}%` : 'Sampling…';
        const cpuMeta = `${cores} core · Load 1 menit ${load.toFixed(2)}`;

        const sampledTime = cpu.sampledAt
          ? new Date(cpu.sampledAt).toLocaleTimeString('id-ID')
          : 'menunggu sampel';

        const updatedTime = new Date().toLocaleTimeString('id-ID');
        const running = Number(serviceSummary.running) ||
          services.filter(item => item.status === 'Running').length;
        const totalServices = Number(serviceSummary.total) || services.length;

        const rows = serviceMarkup(services);

        if (initial) {
          modalBody.innerHTML = `${modalIntro('Telemetri host lokal Ryomd. CPU diukur menggunakan sampel proses top; RAM, storage, dan layanan diperbarui otomatis.')}

            <div class="sm-monitor">

              <header class="sm-hero">
                <div class="sm-hero-icon">⌁</div>
                <div class="sm-hero-copy">
                  <span class="sm-kicker">RYOMD SYSTEM · LIVE TELEMETRY</span>
                  <h3>System Monitor</h3>
                  <p>Pantau kesehatan perangkat dan runtime layanan dalam satu dashboard.</p>
                </div>
                <span class="sm-live"><i></i> MONITORING</span>
              </header>

              <section class="sm-cpu-panel">
                <div class="sm-card-heading">
                  <div>
                    <span class="sm-kicker">PROCESSOR LOAD</span>
                    <h4>CPU utilization</h4>
                  </div>
                  <span class="sm-source-tag">TOP SAMPLE</span>
                </div>

                <div class="sm-cpu-main">
                  <strong id="smCpuValue">${cpuText}</strong>
                  <span id="smCpuMeta">${esc(cpuMeta)}</span>
                </div>

                <div class="sm-gauge sm-gauge-cpu">
                  <i id="smCpuBar" style="width:${cpuReady ? cpuPercent : 0}%"></i>
                </div>

                <div class="sm-cpu-footer">
                  <span id="smCpuSample">${cpuReady ? `Sampel: ${esc(sampledTime)}` : 'Mengumpulkan dua sampel CPU…'}</span>
                  <span>Refresh 5 detik</span>
                </div>
                <p class="sm-load-note">Load average menunjukkan antrean beban sistem, bukan persentase penggunaan CPU.</p>
              </section>

              <section class="sm-metric-grid">
                <article class="sm-metric-card sm-ram-card">
                  <div class="sm-metric-head">
                    <span class="sm-metric-icon">▥</span>
                    <span class="sm-metric-label">MEMORY / RAM</span>
                  </div>
                  <strong id="smRamValue">${ramPercent}%</strong>
                  <div class="sm-gauge"><i id="smRamBar" style="width:${ramPercent}%"></i></div>
                  <small id="smRamMeta">${Number(memory.usedGb || 0).toFixed(2)} / ${Number(memory.totalGb || 0).toFixed(2)} GB</small>
                </article>

                <article class="sm-metric-card sm-storage-card">
                  <div class="sm-metric-head">
                    <span class="sm-metric-icon">▣</span>
                    <span class="sm-metric-label">STORAGE</span>
                  </div>
                  <strong id="smStorageValue">${diskPercent}%</strong>
                  <div class="sm-gauge"><i id="smStorageBar" style="width:${diskPercent}%"></i></div>
                  <small id="smStorageMeta">${Number(storage.usedGb || 0).toFixed(2)} / ${Number(storage.totalGb || 0).toFixed(2)} GB</small>
                </article>

                <article class="sm-metric-card sm-services-card">
                  <div class="sm-metric-head">
                    <span class="sm-metric-icon">⌘</span>
                    <span class="sm-metric-label">SERVICES</span>
                  </div>
                  <strong id="smServicesValue">${running}/${totalServices}</strong>
                  <div class="sm-gauge"><i id="smServicesBar" style="width:${totalServices ? Math.round(running / totalServices * 100) : 0}%"></i></div>
                  <small>Layanan berjalan</small>
                </article>
              </section>

              <section class="sm-service-panel">
                <div class="sm-section-heading">
                  <div>
                    <span class="sm-kicker">RUNTIME OVERVIEW</span>
                    <h4>Status layanan</h4>
                  </div>
                  <span class="sm-auto-tag"><i></i> AUTO REFRESH</span>
                </div>
                <div class="sm-service-list" id="smServiceRows">
                  ${rows || '<p class="sm-empty">Belum ada data layanan.</p>'}
                </div>
              </section>

              <div class="sm-monitor-footer">
                <span>HOST TELEMETRY · LOCAL</span>
                <span id="smRefreshStatus">Diperbarui ${esc(updatedTime)}</span>
              </div>

            </div>`;
        } else {
          const setText = (selector, value) => {
            const element = $(selector, modalBody);
            if (element) element.textContent = value;
          };

          const setBar = (selector, value) => {
            const element = $(selector, modalBody);
            if (element) element.style.width = `${value}%`;
          };

          setText('#smCpuValue', cpuText);
          setText('#smCpuMeta', cpuMeta);
          setText('#smCpuSample', cpuReady
            ? `Sampel: ${sampledTime}`
            : 'Mengumpulkan dua sampel CPU…');
          setBar('#smCpuBar', cpuReady ? cpuPercent : 0);

          setText('#smRamValue', `${ramPercent}%`);
          setText('#smRamMeta',
            `${Number(memory.usedGb || 0).toFixed(2)} / ${Number(memory.totalGb || 0).toFixed(2)} GB`);
          setBar('#smRamBar', ramPercent);

          setText('#smStorageValue', `${diskPercent}%`);
          setText('#smStorageMeta',
            `${Number(storage.usedGb || 0).toFixed(2)} / ${Number(storage.totalGb || 0).toFixed(2)} GB`);
          setBar('#smStorageBar', diskPercent);

          setText('#smServicesValue', `${running}/${totalServices}`);
          setBar('#smServicesBar',
            totalServices ? Math.round(running / totalServices * 100) : 0);

          const serviceList = $('#smServiceRows', modalBody);
          if (serviceList) {
            serviceList.innerHTML = rows || '<p class="sm-empty">Belum ada data layanan.</p>';
          }

          setText('#smRefreshStatus', `Diperbarui ${updatedTime}`);
        }
      } catch (error) {
        if (!isActive()) return;
        const status = $('#smRefreshStatus', modalBody);

        if (initial) {
          modalBody.innerHTML = modalIntro('Telemetri sistem tidak dapat dimuat.') +
            `<div class="tool-card-panel"><h4>Gagal membaca telemetri</h4><p>${esc(error.message || 'Terjadi kesalahan.')}</p></div>`;
        } else if (status) {
          status.textContent = `Gagal memperbarui: ${error.message}`;
        }
      } finally {
        busy = false;
      }
    };

    await update(true);

    if (isActive()) {
      telemetryRefreshTimer = window.setInterval(() => update(false), 5000);
    }
  }


  function renderBannerStudio() {
    const items = [
      {
        filename: '211648.mp4',
        title: 'Video hero banner',
        format: 'VIDEO MP4',
        note: 'Video MP4 · Maks. 120 MB',
        label: 'Pilih video',
        icon: '▶',
        type: 'video',
        description: 'Media bergerak untuk area hero utama dashboard.'
      },
      {
        filename: '211650.jpg',
        title: 'Avatar / logo',
        format: 'IMAGE ASSET',
        note: 'JPG / PNG / WebP · Maks. 120 MB',
        label: 'Pilih avatar',
        icon: '◎',
        type: 'avatar',
        description: 'Identitas visual dan gambar profil Ryomd.'
      },
      {
        filename: '211649.jpg',
        title: 'Poster banner',
        format: 'IMAGE ASSET',
        note: 'JPG / PNG / WebP · Maks. 120 MB',
        label: 'Pilih poster',
        icon: '▧',
        type: 'poster',
        description: 'Gambar poster untuk pelengkap area banner.'
      }
    ];

    modalBody.innerHTML = `${modalIntro('Kelola media visual dashboard Ryomd dari penyimpanan perangkat. Pilih aset dan kirim ke lokasi yang digunakan panel.')}

      <div class="ms-studio">

        <header class="ms-hero">
          <div class="ms-hero-icon">▧</div>
          <div class="ms-hero-copy">
            <span class="ms-kicker">RYOMD SYSTEM · VISUAL ASSETS</span>
            <h3>Media Studio</h3>
            <p>Kelola video banner, avatar, dan poster dalam satu workspace media.</p>
          </div>
          <span class="ms-local-tag">LOCAL</span>
        </header>

        <div class="ms-intro-strip">
          <span class="ms-intro-icon">✓</span>
          <div>
            <strong>Asset destination controlled</strong>
            <small>Setiap media dikirim ke nama file tetap di dalam public/assets.</small>
          </div>
        </div>

        <section class="ms-assets-section">
          <div class="ms-section-heading">
            <div>
              <span class="ms-kicker">DASHBOARD MEDIA LIBRARY</span>
              <h4>Kelola aset visual</h4>
            </div>
            <span class="ms-assets-count">03 ASSETS</span>
          </div>

          <div class="ms-assets-grid">
            ${items.map(item => `
              <article class="ms-asset-card ms-asset-${item.type}">
                <div class="ms-asset-visual">
                  <span class="ms-asset-visual-icon">${item.icon}</span>
                  <span class="ms-asset-visual-label">${item.format}</span>
                  <span class="ms-asset-visual-number">${item.type === 'video' ? '01' : item.type === 'avatar' ? '02' : '03'}</span>
                </div>

                <div class="ms-asset-content">
                  <div class="ms-asset-title">
                    <h4>${esc(item.title)}</h4>
                    <span class="ms-asset-note">${esc(item.note)}</span>
                  </div>

                  <p class="ms-asset-description">${esc(item.description)}</p>

                  <div class="ms-target-box">
                    <span>TARGET FILE</span>
                    <code>public/assets/${esc(item.filename)}</code>
                  </div>

                  <div class="ms-asset-action">
                    ${button(item.label, `asset:${item.filename}`, 'primary')}
                  </div>
                </div>
              </article>
            `).join('')}
          </div>
        </section>

        <section class="ms-upload-info">
          <div class="ms-upload-info-icon">⇧</div>
          <div>
            <strong>Media upload guide</strong>
            <p>Pilih file sesuai format yang diminta. Setiap aset memiliki batas ukuran maksimum 120 MB dan nama target yang sudah ditentukan panel.</p>
          </div>
        </section>

        <div class="ms-footer">
          <span>RYOMD MEDIA WORKSPACE</span>
          <span>PUBLIC / ASSETS</span>
        </div>

      </div>`;
  }

  function makeText(tag, text, className) { const node = document.createElement(tag); node.textContent = String(text ?? ''); if (className) node.className = className; return node; }
  function renderDownloadFiles(files) {
    const list = $('#dlFilesList', modalBody);
    const count = $('#dlResultCount', modalBody);
    if (!list) return;

    const items = Array.isArray(files) ? files : [];
    list.replaceChildren();

    if (count) {
      count.textContent = `${items.length} FILE${items.length === 1 ? '' : 'S'}`;
    }

    if (!items.length) {
      const empty = document.createElement('div');
      empty.className = 'dlx-empty';
      empty.append(
        makeText('span', '⇩', 'dlx-empty-icon'),
        makeText('strong', 'Belum ada hasil unduhan', 'dlx-empty-title'),
        makeText('small', 'File yang berhasil diunduh akan muncul di sini.', 'dlx-empty-note')
      );
      list.appendChild(empty);
      return;
    }

    items.forEach(file => {
      const type = String(file.type || 'file').toLowerCase();
      const name = String(file.name || 'Unduhan');
      const size = `${(Number(file.size || 0) / 1048576).toFixed(2)} MB`;

      const card = document.createElement('article');
      card.className = `dlx-file-card dlx-file-${type}`;

      const head = document.createElement('div');
      head.className = 'dlx-file-head';

      const icon = makeText(
        'span',
        type === 'video' ? '▶' : type === 'audio' ? '♫' : type === 'image' ? '▧' : '▤',
        'dlx-file-icon'
      );

      const identity = document.createElement('div');
      identity.className = 'dlx-file-identity';
      identity.append(
        makeText('strong', name, 'dlx-file-name'),
        makeText('small', `${type.toUpperCase()} · ${size}`, 'dlx-file-meta')
      );

      const status = makeText('span', 'READY', 'dlx-file-ready');
      head.append(icon, identity, status);
      card.appendChild(head);

      const mediaFrame = document.createElement('div');
      mediaFrame.className = 'dlx-media-frame';

      if (file.type === 'video' && file.url) {
        const media = document.createElement('video');
        media.controls = true;
        media.playsInline = true;
        media.preload = 'metadata';
        media.src = file.url;
        mediaFrame.appendChild(media);
      } else if (file.type === 'audio' && file.url) {
        const audioBox = document.createElement('div');
        audioBox.className = 'dlx-audio-preview';
        audioBox.append(
          makeText('span', '♫', 'dlx-audio-symbol'),
          makeText('strong', 'Audio preview', 'dlx-audio-title')
        );
        const media = document.createElement('audio');
        media.controls = true;
        media.preload = 'metadata';
        media.src = file.url;
        audioBox.appendChild(media);
        mediaFrame.appendChild(audioBox);
      } else if (file.type === 'image' && file.url) {
        const media = document.createElement('img');
        media.src = file.url;
        media.alt = name;
        media.loading = 'lazy';
        mediaFrame.appendChild(media);
      } else {
        mediaFrame.append(
          makeText('span', '▤', 'dlx-placeholder-icon'),
          makeText('small', 'Pratinjau tidak tersedia', 'dlx-placeholder-text')
        );
      }

      card.appendChild(mediaFrame);

      const actions = document.createElement('div');
      actions.className = 'dlx-file-actions';

      const link = document.createElement('a');
      link.className = 'tool-btn primary dlx-save-link';
      link.href = file.downloadUrl || file.url || '#';
      link.download = name;
      link.textContent = '↓  Simpan ke perangkat';
      actions.appendChild(link);

      card.appendChild(actions);
      list.appendChild(card);
    });
  }


  // RYOMD_YOUTUBE_IFRAME_PLAYER_V1
  function renderYouTubePlayer() {
    modalBody.innerHTML = `${modalIntro('Putar video atau playlist YouTube langsung di dashboard menggunakan pemutar resmi YouTube.')}
      <section class="ryo-yt-player">
        <div class="ryo-yt-player-head">
          <span class="ryo-yt-kicker">RYOMD MEDIA / YOUTUBE</span>
          <h3>YouTube Player</h3>
          <p>Masukkan tautan video, ID video 11 karakter, atau tautan playlist YouTube.</p>
        </div>
        <div class="ryo-yt-form">
          <label for="ryoYoutubeInput">URL / ID video / playlist</label>
          <input class="tool-input" id="ryoYoutubeInput" type="text" inputmode="url" autocomplete="off" placeholder="https://www.youtube.com/watch?v=… atau ID video" spellcheck="false">
          <div class="ryo-yt-actions">
            ${button('Putar sekarang', 'youtube-play', 'primary')}
            <a class="tool-btn ryo-yt-search-link" href="https://www.youtube.com/" target="_blank" rel="noopener noreferrer">Cari di YouTube ↗</a>
          </div>
        </div>
        <div class="ryo-yt-status" id="ryoYoutubeStatus" role="status" aria-live="polite">Player siap. Tempel tautan video atau playlist.</div>
        <div class="ryo-yt-stage" id="ryoYoutubeStage">
          <div class="ryo-yt-placeholder"><span>▶</span><b>Belum ada video</b><small>Pemutar resmi YouTube akan tampil di sini.</small></div>
        </div>
        <a class="ryo-yt-external" id="ryoYoutubeExternal" href="https://www.youtube.com/" target="_blank" rel="noopener noreferrer" hidden>Buka di YouTube ↗</a>
        <p class="ryo-yt-note">Pemutaran menggunakan YouTube Embed. Sebagian video dapat membatasi pemutaran tertanam berdasarkan kebijakan pemilik konten.</p>
      </section>`;
    const input = $('#ryoYoutubeInput', modalBody);
    if (input) input.addEventListener('keydown', event => {
      if (event.key === 'Enter') { event.preventDefault(); loadYouTubePlayer(); }
    });
  }

  function parseYouTubeTarget(rawValue) {
    const raw = String(rawValue || '').trim();
    if (!raw) throw new Error('Masukkan URL atau ID video YouTube.');
    if (/^[A-Za-z0-9_-]{11}$/.test(raw)) return { type: 'video', id: raw };
    if (/^(?:PL|UU|LL|FL|RD|OLAK5uy_)[A-Za-z0-9_-]{8,}$/i.test(raw)) return { type: 'playlist', id: raw };

    let url;
    try { url = new URL(raw); }
    catch { throw new Error('Format tautan YouTube tidak dikenali.'); }
    const host = url.hostname.toLowerCase().replace(/\.$/, '');
    const allowed = ['youtube.com', 'www.youtube.com', 'm.youtube.com', 'music.youtube.com', 'youtu.be', 'www.youtu.be', 'youtube-nocookie.com', 'www.youtube-nocookie.com'];
    if (url.protocol !== 'https:' || !allowed.includes(host) || url.username || url.password || url.port) {
      throw new Error('Gunakan tautan HTTPS dari youtube.com, youtu.be, atau youtube-nocookie.com.');
    }
    const listId = url.searchParams.get('list');
    if (listId && (/^(?:PL|UU|LL|FL|RD|OLAK5uy_)[A-Za-z0-9_-]{8,}$/i.test(listId))) {
      return { type: 'playlist', id: listId };
    }
    let videoId = url.searchParams.get('v') || '';
    if (!videoId) {
      const parts = url.pathname.split('/').filter(Boolean);
      if (host === 'youtu.be') videoId = parts[0] || '';
      else if (['embed', 'shorts', 'live', 'v'].includes(parts[0])) videoId = parts[1] || '';
    }
    if (!/^[A-Za-z0-9_-]{11}$/.test(videoId)) {
      if (listId && /^[A-Za-z0-9_-]{10,120}$/.test(listId)) return { type: 'playlist', id: listId };
      throw new Error('ID video tidak valid. Gunakan URL video, URL playlist, atau ID video 11 karakter.');
    }
    return { type: 'video', id: videoId };
  }

  function loadYouTubePlayer() {
    const input = $('#ryoYoutubeInput', modalBody);
    const stage = $('#ryoYoutubeStage', modalBody);
    const status = $('#ryoYoutubeStatus', modalBody);
    const external = $('#ryoYoutubeExternal', modalBody);
    if (!input || !stage || !status) return;
    try {
      const target = parseYouTubeTarget(input.value);
      const embed = target.type === 'playlist'
        ? `https://www.youtube-nocookie.com/embed/videoseries?list=${encodeURIComponent(target.id)}&rel=0&playsinline=1`
        : `https://www.youtube-nocookie.com/embed/${encodeURIComponent(target.id)}?rel=0&playsinline=1`;
      const iframe = document.createElement('iframe');
      iframe.className = 'ryo-yt-iframe';
      iframe.src = embed;
      iframe.title = target.type === 'playlist' ? 'YouTube playlist player' : 'YouTube video player';
      iframe.loading = 'lazy';
      iframe.referrerPolicy = 'strict-origin-when-cross-origin';
      iframe.allow = 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share';
      iframe.allowFullscreen = true;
      stage.replaceChildren(iframe);
      const watchUrl = target.type === 'playlist'
        ? `https://www.youtube.com/playlist?list=${encodeURIComponent(target.id)}`
        : `https://www.youtube.com/watch?v=${encodeURIComponent(target.id)}`;
      if (external) { external.href = watchUrl; external.hidden = false; }
      status.textContent = target.type === 'playlist' ? 'Playlist dimuat melalui YouTube Embed.' : 'Video dimuat melalui YouTube Embed.';
    } catch (error) {
      status.textContent = error.message || 'Tautan tidak dapat diproses.';
      status.classList.add('is-error');
    }
  }
  // END RYOMD_YOUTUBE_IFRAME_PLAYER_V1


  // RYOMD_BMKG_OPEN_METEO_FALLBACK_V1
  let bmkgLastLocation = null;

  function openMeteoWeatherDescription(code) {
    const n = Number(code);
    if (n === 0) return 'Cerah';
    if (n === 1) return 'Cerah berawan';
    if (n === 2) return 'Cerah berawan';
    if (n === 3) return 'Berawan tebal';
    if (n === 45 || n === 48) return 'Berkabut';
    if ([51, 53, 55, 56, 57].includes(n)) return 'Gerimis';
    if ([61, 63, 65, 66, 67].includes(n)) return 'Hujan';
    if ([71, 73, 75, 77, 85, 86].includes(n)) return 'Salju';
    if ([80, 81, 82].includes(n)) return 'Hujan lokal';
    if ([95, 96, 99].includes(n)) return 'Badai petir';
    return 'Kondisi cuaca bervariasi';
  }

  async function loadWeatherFallback() {
    const output = $('#bmkgOut', modalBody);
    const buttonEl = $('[data-action="load-weather-fallback"]', modalBody);
    if (!output || !bmkgLastLocation) {
      if (output) output.textContent = 'Koordinat lokasi belum tersedia. Ambil prakiraan BMKG terlebih dahulu.';
      return;
    }
    if (buttonEl) { buttonEl.disabled = true; buttonEl.textContent = 'Memuat prakiraan…'; }
    output.innerHTML = '<div class="bmkg-loading-state"><b>Mengambil prakiraan alternatif…</b><small>Menghubungi Open-Meteo berdasarkan koordinat lokasi terpilih.</small></div>';
    try {
      const params = new URLSearchParams({
        latitude: String(bmkgLastLocation.lat),
        longitude: String(bmkgLastLocation.lon),
        current: 'temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m',
        hourly: 'temperature_2m,relative_humidity_2m,precipitation_probability,weather_code,wind_speed_10m',
        forecast_days: '2', timezone: 'auto'
      });
      const response = await fetch('https://api.open-meteo.com/v1/forecast?' + params.toString(), { method: 'GET', mode: 'cors', cache: 'no-store' });
      if (!response.ok) throw new Error('Open-Meteo merespons HTTP ' + response.status + '.');
      const data = await response.json();
      const current = data.current || {};
      if (!Number.isFinite(Number(current.temperature_2m))) throw new Error('Data suhu dari Open-Meteo belum tersedia.');
      const fmt = value => Number.isFinite(Number(value)) ? String(Math.round(Number(value))) : '—';
      const hourly = data.hourly || {};
      const times = Array.isArray(hourly.time) ? hourly.time : [];
      const nextRows = [];
      const now = Date.now() - 60 * 60 * 1000;
      for (let i = 0; i < times.length && nextRows.length < 8; i++) {
        const stamp = new Date(times[i]).getTime();
        if (!Number.isFinite(stamp) || stamp < now) continue;
        nextRows.push({
          time: new Date(times[i]).toLocaleTimeString('id-ID', { hour: '2-digit', minute: '2-digit' }),
          temp: fmt(hourly.temperature_2m?.[i]),
          rain: fmt(hourly.precipitation_probability?.[i]),
          weather: openMeteoWeatherDescription(hourly.weather_code?.[i])
        });
      }
      const location = esc(bmkgLastLocation.label || 'Lokasi terpilih');
      const cards = nextRows.map(row => `<article class="bmkg-alt-hour"><b>${esc(row.time)}</b><span>${esc(row.weather)}</span><strong>${esc(row.temp)}°C</strong><small>Peluang hujan ${esc(row.rain)}%</small></article>`).join('');
      output.innerHTML = `<section class="bmkg-alt-result"><div class="bmkg-alt-source">PRAKIRAAN ALTERNATIF · OPEN-METEO</div><h4>${location}</h4><div class="bmkg-alt-current"><strong>${fmt(current.temperature_2m)}°C</strong><span>${esc(openMeteoWeatherDescription(current.weather_code))}</span><small>Terasa ${fmt(current.apparent_temperature)}°C · Kelembapan ${fmt(current.relative_humidity_2m)}% · Angin ${fmt(current.wind_speed_10m)} km/jam</small></div><div class="bmkg-alt-hours">${cards || '<p>Data per jam belum tersedia.</p>'}</div><p class="bmkg-alt-attribution">Sumber prakiraan: <a href="https://open-meteo.com/" target="_blank" rel="noopener noreferrer">Open-Meteo</a> · CC BY 4.0. Ini bukan data resmi BMKG. BMKG tetap menjadi sumber utama saat data prakiraannya tersedia.</p></section>`;
    } catch (error) {
      output.innerHTML = `<div class="bmkg-load-message is-error"><b>Prakiraan alternatif gagal dimuat</b><span>${esc(error.message || 'Periksa koneksi internet lalu coba lagi.')}</span><button class="tool-btn" type="button" data-action="load-weather-fallback">Coba lagi</button></div>`;
    } finally {
      const retry = $('[data-action="load-weather-fallback"]', modalBody);
      if (retry) { retry.disabled = false; retry.textContent = 'Coba Open-Meteo'; }
    }
  }
  // END RYOMD_BMKG_OPEN_METEO_FALLBACK_V1

  async function refreshDownloads() { const data = await api('/api/downloads/list'); renderDownloadFiles(data.files || []); }
  function renderDownloader(toolId) {
    const audio = toolId === 'yt-audio';

    modalBody.innerHTML = `${modalIntro('Tempel URL HTTPS publik dari konten yang memang boleh kamu unduh. Proses berjalan di server lokal menggunakan yt-dlp dan FFmpeg.')}

      <div class="dlx-downloader">

        <header class="dlx-hero">
          <div class="dlx-hero-icon">⇩</div>
          <div class="dlx-hero-copy">
            <span class="dlx-kicker">RYOMD SYSTEM · MEDIA DOWNLOAD</span>
            <h3>${audio ? 'Audio Extractor' : 'Social Downloader'}</h3>
            <p>${audio ? 'Ekstrak audio dari URL yang didukung.' : 'Unduh video atau album media ke workspace lokal.'}</p>
          </div>
          <span class="dlx-local-tag">LOCAL ENGINE</span>
        </header>

        <section class="dlx-form-panel">
          <div class="dlx-section-heading">
            <div>
              <span class="dlx-kicker">SOURCE CONFIGURATION</span>
              <h4>Sumber konten</h4>
            </div>
            <span class="dlx-step">01</span>
          </div>

          <div class="dlx-field">
            <label for="dlUrlInp">URL konten</label>
            <div class="dlx-url-wrap">
              <span class="dlx-url-icon">↗</span>
              <input class="tool-input" type="url" id="dlUrlInp"
                inputmode="url" autocomplete="url"
                placeholder="https://tempel-tautan-konten..."
                spellcheck="false">
            </div>
            <small>Gunakan URL HTTPS publik yang dapat diakses oleh server.</small>
          </div>

          <div class="dlx-field">
            <label for="dlFormatInp">Format output</label>
            <select class="tool-select dlx-format-select" id="dlFormatInp">
              <option value="mp4">Video MP4</option>
              <option value="mp3">Audio MP3</option>
              <option value="slides">Album / slide gambar</option>
            </select>
            <div class="dlx-format-hint">
              <span>i</span>
              <p>Album tersedia hanya jika situs dan konten menyediakan gambar carousel.</p>
            </div>
          </div>

          <div class="dlx-action-row">
            ${button('Mulai download', 'run-download', 'primary')}
            ${button('Refresh hasil', 'refresh-downloads')}
          </div>

          <div class="tool-output dlx-download-status" id="dlResultOut" role="status" aria-live="polite">
            Siap menerima URL konten.
          </div>
        </section>

        <section class="dlx-results-section">
          <div class="dlx-section-heading">
            <div>
              <span class="dlx-kicker">LOCAL DOWNLOAD LIBRARY</span>
              <h4>Hasil unduhan</h4>
              <small>File tersimpan di workspace server.</small>
            </div>
            <span class="dlx-result-count" id="dlResultCount">MEMUAT…</span>
          </div>

          <div class="download-list dlx-results-grid" id="dlFilesList">
            <div class="dlx-empty">
              <span class="dlx-empty-icon">⇩</span>
              <strong class="dlx-empty-title">Memuat hasil unduhan</strong>
              <small class="dlx-empty-note">Mengambil daftar file dari server lokal.</small>
            </div>
          </div>
        </section>

        <div class="dlx-footer-note">
          <span>✓</span>
          <p>File disimpan di folder <code>downloads/</code> pada workspace server. Unduh hanya konten yang boleh kamu akses dan simpan; perhatikan hak cipta serta ketentuan platform sumber.</p>
        </div>

      </div>`;

    if (audio) $('#dlFormatInp', modalBody).value = 'mp3';

    refreshDownloads().catch(error => {
      const output = $('#dlResultOut', modalBody);
      if (output) output.textContent = error.message;
    });
  }

  function drawTextWrapped(ctx, text, x, y, maxWidth, lineHeight, maxLines) {
    const words = String(text).split(/\s+/); let line = ''; let lines = 0;
    for (const word of words) {
      const test = line ? `${line} ${word}` : word;
      if (ctx.measureText(test).width > maxWidth && line) { ctx.fillText(line, x, y); y += lineHeight; lines++; line = word; if (lines >= maxLines) return; }
      else line = test;
    }
    if (line && lines < maxLines) ctx.fillText(line, x, y);
  }
  function decorateQuoteMaker() {
    if (!modalBody || modalBody.querySelector('.qdx-shell')) return;

    // Wrap the existing DOM without recreating controls. Existing IDs,
    // canvas references, and direct/delegated event listeners remain intact.
    const shell = document.createElement('section');
    shell.className = 'qdx-shell';

    const hero = document.createElement('header');
    hero.className = 'qdx-hero';
    hero.innerHTML = `
      <div class="qdx-hero-mark" aria-hidden="true">“</div>
      <div class="qdx-hero-copy">
        <span class="qdx-eyebrow">RYOMD · CREATIVE STUDIO</span>
        <h3>Quote Card Designer</h3>
        <p>Ubah kalimat sederhana menjadi kartu kutipan yang siap dibagikan.</p>
      </div>
      <span class="qdx-export-tag">PNG EXPORT</span>`;

    const content = document.createElement('div');
    content.className = 'qdx-content';
    while (modalBody.firstChild) content.appendChild(modalBody.firstChild);
    shell.append(hero, content);
    modalBody.replaceChildren(shell);

    content.querySelectorAll('.tool-field').forEach(el => el.classList.add('qdx-field'));
    content.querySelectorAll('label').forEach(el => el.classList.add('qdx-label'));
    content.querySelectorAll('input, textarea, select').forEach(el => el.classList.add('qdx-control'));
    content.querySelectorAll('button, .tool-btn').forEach(el => el.classList.add('qdx-button'));
    content.querySelectorAll('.tool-actions').forEach(el => el.classList.add('qdx-actions'));

    // Style the existing live preview/canvas only. Do not redraw or replace it.
    const visualNodes = [...content.querySelectorAll('canvas, img, video, svg')];
    visualNodes.forEach(el => {
      el.classList.add('qdx-visual');
      const stage = el.parentElement;
      if (stage && stage !== content && stage !== shell) stage.classList.add('qdx-preview-stage');
    });

    // Give existing preview/output containers a polished frame without
    // depending on a particular canvas ID or changing any export handler.
    content.querySelectorAll('*').forEach(el => {
      const id = String(el.id || '').toLowerCase();
      const cls = typeof el.className === 'string' ? el.className.toLowerCase() : '';
      if (/(preview|canvas|quote.?output|card.?result)/.test(id + ' ' + cls)) {
        el.classList.add('qdx-output-region');
      }
    });
  }

  function renderQuoteMaker() {
    modalBody.innerHTML = `<div class="tool-field"><label for="qTextInp">Kutipan</label><textarea class="tool-textarea" id="qTextInp" maxlength="220">Pelan-pelan bukan berarti berhenti. Yang penting tetap bergerak.</textarea></div><div class="tool-field"><label for="qAuthorInp">Nama / atribusi</label><input class="tool-input" id="qAuthorInp" maxlength="64" value="Ryomd Notes"></div><canvas class="canvas-preview" id="quoteCanvas" width="1080" height="620"></canvas><div class="tool-actions">${button('Download PNG', 'download-quote', 'primary')}</div>`;
    const canvas = $('#quoteCanvas', modalBody); const ctx = canvas.getContext('2d');
    const draw = () => {
      const grad = ctx.createLinearGradient(0, 0, 1080, 620); grad.addColorStop(0, '#182a43'); grad.addColorStop(1, '#101b2b');
      ctx.fillStyle = grad; ctx.fillRect(0,0,1080,620); ctx.strokeStyle = 'rgba(169,190,255,.52)'; ctx.lineWidth = 3; ctx.strokeRect(36,36,1008,548);
      ctx.fillStyle = '#9fb5ef'; ctx.font = '700 22px system-ui'; ctx.fillText('RYOMD  /  NOTES', 84, 116);
      ctx.fillStyle = '#f1f5fa'; ctx.font = '600 42px system-ui'; drawTextWrapped(ctx, $('#qTextInp', modalBody).value, 84, 270, 900, 64, 4);
      ctx.fillStyle = '#9eafc5'; ctx.font = '26px system-ui'; ctx.fillText(`— ${$('#qAuthorInp', modalBody).value}`, 84, 505);
      ctx.fillStyle = '#7286a5'; ctx.font = '18px system-ui'; ctx.fillText('KEEP MOVING FORWARD', 84, 548);
    };
    $('#qTextInp', modalBody).addEventListener('input', draw); $('#qAuthorInp', modalBody).addEventListener('input', draw); draw();
  }

  function renderAvatarMaker() {
    modalBody.innerHTML = `
      <section class="pcx-shell">
        <div class="pcx-heading">
          <div class="pcx-heading-icon" aria-hidden="true">✦</div>
          <div>
            <div class="pcx-eyebrow">RYOMD CREATIVE STUDIO</div>
            <h3>Profile Card Designer</h3>
            <p>Pilih kategori, sesuaikan identitas, lalu ekspor kartu profil dalam PNG.</p>
          </div>
        </div>

        <div class="pcx-workspace">
          <div class="pcx-controls">
            <div class="pcx-section-title"><span>01</span><div><b>Identitas profil</b><small>Informasi yang tampil pada kartu</small></div></div>
            <label class="pcx-upload" for="avatarPhotoInp">
              <span class="pcx-upload-mark">＋</span>
              <span><b>Foto profil</b><small id="avatarPhotoName">Pilih gambar dari perangkat · maks. 8 MB</small></span>
              <span class="pcx-upload-action">Pilih foto</span>
            </label>
            <input class="pcx-file-input" type="file" id="avatarPhotoInp" accept="image/*">

            <div class="pcx-field">
              <label for="avatarNameInp">Nama</label>
              <input class="tool-input" id="avatarNameInp" maxlength="36" value="Ryomd User" placeholder="Nama profil">
            </div>
            <div class="pcx-field">
              <label for="avatarSubInp">Subjudul</label>
              <input class="tool-input" id="avatarSubInp" maxlength="55" value="Personal workspace" placeholder="Contoh: Designer · Developer">
            </div>

            <div class="pcx-section-title pcx-category-title"><span>02</span><div><b>Kategori desain</b><small>Pilih karakter visual kartu</small></div></div>
            <div class="pcx-category-grid">
              <label class="pcx-category-card is-active" data-category-card="professional">
                <input type="radio" name="avatarCategory" id="avatarCategoryProfessional" value="professional" checked>
                <span class="pcx-category-swatch pcx-swatch-professional">P</span>
                <span><b>Professional</b><small>Formal · clean</small></span>
              </label>
              <label class="pcx-category-card" data-category-card="minimalist">
                <input type="radio" name="avatarCategory" value="minimalist">
                <span class="pcx-category-swatch pcx-swatch-minimalist">M</span>
                <span><b>Minimalist</b><small>Simple · calm</small></span>
              </label>
              <label class="pcx-category-card" data-category-card="gaming">
                <input type="radio" name="avatarCategory" value="gaming">
                <span class="pcx-category-swatch pcx-swatch-gaming">G</span>
                <span><b>Gaming</b><small>Neon · bold</small></span>
              </label>
              <label class="pcx-category-card" data-category-card="anime">
                <input type="radio" name="avatarCategory" value="anime">
                <span class="pcx-category-swatch pcx-swatch-anime">A</span>
                <span><b>Anime</b><small>Soft · playful</small></span>
              </label>
              <label class="pcx-category-card" data-category-card="luxury">
                <input type="radio" name="avatarCategory" value="luxury">
                <span class="pcx-category-swatch pcx-swatch-luxury">L</span>
                <span><b>Luxury</b><small>Black · gold</small></span>
              </label>
              <label class="pcx-category-card" data-category-card="creator">
                <input type="radio" name="avatarCategory" value="creator">
                <span class="pcx-category-swatch pcx-swatch-creator">C</span>
                <span><b>Creator</b><small>Vibrant · social</small></span>
              </label>
            </div>
            <div class="pcx-private-note"><span>◎</span><span><b>Foto diproses secara lokal</b><small>Gambar tidak diunggah ke server. Desain dibuat langsung di browser.</small></span></div>
          </div>

          <div class="pcx-preview-panel">
            <div class="pcx-preview-top"><div><span class="pcx-live-dot"></span><b>Live preview</b></div><span>1080 × 660 px</span></div>
            <div class="pcx-canvas-wrap"><canvas class="canvas-preview pcx-canvas" id="avatarCanvas" width="1080" height="660" aria-label="Pratinjau kartu profil"></canvas></div>
            <div class="pcx-preview-caption"><span>PREVIEW KARTU PROFIL</span><span id="pcxCurrentStyle">PROFESSIONAL</span></div>
            <div class="pcx-export-row">${button('Download profile card', 'download-avatar', 'primary')}</div>
            <p class="pcx-export-note">PNG resolusi tinggi · siap dibagikan</p>
          </div>
        </div>
      </section>`;

    const canvas = $('#avatarCanvas', modalBody);
    const ctx = canvas.getContext('2d');
    const photoInput = $('#avatarPhotoInp', modalBody);
    const nameInput = $('#avatarNameInp', modalBody);
    const subInput = $('#avatarSubInp', modalBody);
    const photoName = $('#avatarPhotoName', modalBody);
    let photo = null;

    const themes = {
      professional: { bg1:'#102b3c', bg2:'#14253a', accent:'#58d6c5', text:'#f5fbff', muted:'#afc5d1', tag:'PROFESSIONAL PROFILE' },
      minimalist:   { bg1:'#f2eee5', bg2:'#e5dfd2', accent:'#283b36', text:'#1d2925', muted:'#68716b', tag:'PERSONAL PROFILE' },
      gaming:       { bg1:'#100d24', bg2:'#211044', accent:'#9d73ff', text:'#f7f3ff', muted:'#c0b4e8', tag:'PLAYER PROFILE' },
      anime:        { bg1:'#ffe8f1', bg2:'#e8e4ff', accent:'#bd76bd', text:'#503d62', muted:'#806d8c', tag:'MY PROFILE' },
      luxury:       { bg1:'#111111', bg2:'#29251d', accent:'#d8b66d', text:'#fff8e8', muted:'#c5b994', tag:'SIGNATURE PROFILE' },
      creator:      { bg1:'#351a4d', bg2:'#152b54', accent:'#ffad72', text:'#fff8ff', muted:'#d9c6e5', tag:'CREATOR PROFILE' }
    };

    const roundedRect = (c, x, y, w, h, r) => {
      const radius = Math.min(r, w / 2, h / 2);
      c.beginPath();
      c.moveTo(x + radius, y);
      c.arcTo(x + w, y, x + w, y + h, radius);
      c.arcTo(x + w, y + h, x, y + h, radius);
      c.arcTo(x, y + h, x, y, radius);
      c.arcTo(x, y, x + w, y, radius);
      c.closePath();
    };

    const fitText = (text, maxWidth, startSize, minSize, weight = 700) => {
      let size = startSize;
      while (size > minSize) {
        ctx.font = `${weight} ${size}px system-ui, sans-serif`;
        if (ctx.measureText(text).width <= maxWidth) break;
        size -= 2;
      }
      return size;
    };

    const drawPhoto = (x, y, size, style, radiusMode = 'circle') => {
      ctx.save();
      if (radiusMode === 'circle') {
        ctx.beginPath(); ctx.arc(x + size / 2, y + size / 2, size / 2, 0, Math.PI * 2); ctx.clip();
      } else {
        roundedRect(ctx, x, y, size, size, style === 'minimalist' ? 18 : 38); ctx.clip();
      }
      if (photo) {
        const scale = Math.max(size / photo.width, size / photo.height);
        const w = photo.width * scale, h = photo.height * scale;
        ctx.drawImage(photo, x + (size - w) / 2, y + (size - h) / 2, w, h);
      } else {
        ctx.fillStyle = style === 'minimalist' ? '#c8c1b3' : '#34465f';
        ctx.fillRect(x, y, size, size);
        ctx.fillStyle = style === 'minimalist' ? '#ffffff' : '#c5d5ff';
        ctx.font = `700 ${Math.round(size * .48)}px system-ui, sans-serif`;
        ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
        ctx.fillText((nameInput.value.trim() || 'R').charAt(0).toUpperCase(), x + size / 2, y + size / 2 + 4);
        ctx.textAlign = 'left'; ctx.textBaseline = 'alphabetic';
      }
      ctx.restore();
    };

    const draw = () => {
      const selected = modalBody.querySelector('input[name="avatarCategory"]:checked');
      const category = selected ? selected.value : 'professional';
      const t = themes[category] || themes.professional;
      const name = (nameInput.value.trim() || 'Ryomd User').slice(0, 26);
      const subtitle = (subInput.value.trim() || 'Personal workspace').slice(0, 42);
      const bg = ctx.createLinearGradient(0, 0, 1080, 660);
      bg.addColorStop(0, t.bg1); bg.addColorStop(1, t.bg2);
      ctx.clearRect(0, 0, 1080, 660); ctx.fillStyle = bg; ctx.fillRect(0, 0, 1080, 660);

      if (category === 'professional') {
        ctx.fillStyle = t.accent; ctx.fillRect(0, 0, 18, 660);
        ctx.globalAlpha = .10; ctx.fillStyle = '#ffffff'; ctx.beginPath(); ctx.arc(1000, 50, 260, 0, Math.PI*2); ctx.fill(); ctx.globalAlpha = 1;
        ctx.strokeStyle = 'rgba(255,255,255,.18)'; ctx.lineWidth = 2; roundedRect(ctx, 34, 34, 1012, 592, 28); ctx.stroke();
        drawPhoto(112, 178, 300, category, 'circle');
        ctx.strokeStyle = t.accent; ctx.lineWidth = 10; ctx.beginPath(); ctx.arc(262, 328, 157, 0, Math.PI*2); ctx.stroke();
        ctx.fillStyle = t.accent; ctx.fillRect(520, 220, 7, 220);
        ctx.textAlign = 'left'; ctx.fillStyle = t.accent; ctx.font = '700 22px system-ui'; ctx.fillText(t.tag, 570, 154);
        ctx.fillStyle = t.text; ctx.font = `700 ${fitText(name, 420, 58, 34)}px system-ui`; ctx.fillText(name, 570, 310);
        ctx.fillStyle = t.muted; ctx.font = '30px system-ui'; ctx.fillText(subtitle, 570, 365);
        ctx.fillStyle = t.muted; ctx.font = '18px system-ui'; ctx.fillText('RYOMD  /  PERSONAL IDENTITY', 570, 510);
      } else if (category === 'minimalist') {
        ctx.strokeStyle = 'rgba(40,59,54,.32)'; ctx.lineWidth = 2; roundedRect(ctx, 36, 36, 1008, 588, 20); ctx.stroke();
        ctx.fillStyle = t.accent; ctx.fillRect(92, 110, 90, 5);
        ctx.fillStyle = t.muted; ctx.font = '700 20px system-ui'; ctx.fillText(t.tag, 92, 165);
        drawPhoto(92, 220, 250, category, 'rounded');
        ctx.fillStyle = t.text; ctx.font = `700 ${fitText(name, 560, 68, 38)}px Georgia, serif`; ctx.fillText(name, 405, 335);
        ctx.fillStyle = t.muted; ctx.font = '30px system-ui'; ctx.fillText(subtitle, 408, 392);
        ctx.fillStyle = t.accent; ctx.fillRect(408, 445, 470, 3);
        ctx.fillStyle = t.muted; ctx.font = '18px system-ui'; ctx.fillText('A LITTLE INTRODUCTION', 408, 492);
      } else if (category === 'gaming') {
        ctx.globalAlpha = .18; ctx.strokeStyle = t.accent; ctx.lineWidth = 3;
        for (let x = 650; x < 1120; x += 48) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x - 260, 660); ctx.stroke(); }
        ctx.globalAlpha = 1;
        ctx.strokeStyle = t.accent; ctx.lineWidth = 4; ctx.strokeRect(38, 38, 1004, 584);
        ctx.fillStyle = t.accent; ctx.fillRect(72, 74, 14, 80);
        drawPhoto(112, 188, 286, category, 'rounded');
        ctx.strokeStyle = t.accent; ctx.lineWidth = 8; ctx.strokeRect(104, 180, 302, 302);
        ctx.fillStyle = t.accent; ctx.font = '700 20px system-ui'; ctx.fillText(t.tag, 490, 186);
        ctx.fillStyle = t.text; ctx.font = `900 ${fitText(name, 480, 64, 36)}px system-ui`; ctx.fillText(name, 490, 302);
        ctx.fillStyle = t.muted; ctx.font = '28px system-ui'; ctx.fillText(subtitle, 492, 362);
        ctx.fillStyle = t.accent; ctx.font = '700 18px system-ui'; ctx.fillText('READY PLAYER  /  RYOMD', 492, 510);
      } else if (category === 'anime') {
        ctx.globalAlpha = .30; ctx.fillStyle = '#ffffff';
        [[920,110,70],[1000,520,110],[470,90,36],[80,570,48]].forEach(([x,y,r])=>{ctx.beginPath();ctx.arc(x,y,r,0,Math.PI*2);ctx.fill();});
        ctx.globalAlpha = 1;
        ctx.strokeStyle = 'rgba(255,255,255,.75)'; ctx.lineWidth = 3; roundedRect(ctx, 34, 34, 1012, 592, 42); ctx.stroke();
        drawPhoto(112, 168, 310, category, 'circle');
        ctx.strokeStyle = '#ffffff'; ctx.lineWidth = 12; ctx.beginPath(); ctx.arc(267, 323, 161, 0, Math.PI*2); ctx.stroke();
        ctx.fillStyle = t.accent; ctx.font = '700 22px system-ui'; ctx.fillText('✦  '+t.tag+'  ✦', 520, 190);
        ctx.fillStyle = t.text; ctx.font = `800 ${fitText(name, 450, 60, 34)}px system-ui`; ctx.fillText(name, 520, 310);
        ctx.fillStyle = t.muted; ctx.font = '28px system-ui'; ctx.fillText(subtitle, 522, 370);
        ctx.fillStyle = t.accent; ctx.font = '700 18px system-ui'; ctx.fillText('MADE WITH A LITTLE MAGIC', 522, 505);
      } else if (category === 'luxury') {
        ctx.strokeStyle = t.accent; ctx.lineWidth = 3; roundedRect(ctx, 34, 34, 1012, 592, 12); ctx.stroke();
        ctx.strokeStyle = 'rgba(216,182,109,.42)'; ctx.lineWidth = 1; roundedRect(ctx, 48, 48, 984, 564, 8); ctx.stroke();
        drawPhoto(118, 188, 278, category, 'circle');
        ctx.strokeStyle = t.accent; ctx.lineWidth = 5; ctx.beginPath(); ctx.arc(257, 327, 147, 0, Math.PI*2); ctx.stroke();
        ctx.textAlign = 'left'; ctx.fillStyle = t.accent; ctx.font = '600 22px Georgia, serif'; ctx.fillText('—  '+t.tag+'  —', 500, 190);
        ctx.fillStyle = t.text; ctx.font = `600 ${fitText(name, 480, 62, 36)}px Georgia, serif`; ctx.fillText(name, 500, 312);
        ctx.fillStyle = t.muted; ctx.font = 'italic 28px Georgia, serif'; ctx.fillText(subtitle, 502, 372);
        ctx.fillStyle = t.accent; ctx.fillRect(502, 425, 360, 2);
        ctx.fillStyle = t.muted; ctx.font = '18px Georgia, serif'; ctx.fillText('RYOMD  ·  SIGNATURE EDITION', 502, 485);
      } else {
        ctx.globalAlpha = .25; ctx.fillStyle = t.accent; ctx.beginPath(); ctx.arc(1020, 20, 230, 0, Math.PI*2); ctx.fill(); ctx.beginPath(); ctx.arc(35, 650, 180, 0, Math.PI*2); ctx.fill(); ctx.globalAlpha = 1;
        ctx.fillStyle = 'rgba(255,255,255,.12)'; roundedRect(ctx, 34, 34, 1012, 592, 34); ctx.fill();
        drawPhoto(112, 178, 292, category, 'rounded');
        ctx.fillStyle = t.accent; ctx.font = '700 22px system-ui'; ctx.fillText(t.tag, 520, 190);
        ctx.fillStyle = t.text; ctx.font = `800 ${fitText(name, 440, 62, 34)}px system-ui`; ctx.fillText(name, 520, 310);
        ctx.fillStyle = t.muted; ctx.font = '28px system-ui'; ctx.fillText(subtitle, 522, 372);
        ctx.fillStyle = t.accent; roundedRect(ctx, 520, 432, 250, 48, 24); ctx.fill();
        ctx.fillStyle = '#241632'; ctx.font = '700 17px system-ui'; ctx.fillText('CREATE  ·  INSPIRE', 548, 463);
      }
      $('#pcxCurrentStyle', modalBody).textContent = category.toUpperCase();
      modalBody.querySelectorAll('[data-category-card]').forEach(card => {
        card.classList.toggle('is-active', card.dataset.categoryCard === category);
      });
    };

    photoInput.addEventListener('change', event => {
      const file = event.target.files?.[0];
      if (!file) return;
      if (!file.type.startsWith('image/')) { toast('File harus berupa gambar.', 'error'); photoInput.value = ''; return; }
      if (file.size > 8 * 1024 * 1024) { toast('Ukuran foto maksimal 8 MB.', 'error'); photoInput.value = ''; return; }
      const reader = new FileReader();
      reader.onload = () => {
        const img = new Image();
        img.onload = () => { photo = img; photoName.textContent = file.name; draw(); };
        img.onerror = () => toast('Foto tidak dapat dibaca. Coba file gambar lain.', 'error');
        img.src = reader.result;
      };
      reader.onerror = () => toast('Gagal membaca foto.', 'error');
      reader.readAsDataURL(file);
    });
    nameInput.addEventListener('input', draw);
    subInput.addEventListener('input', draw);
    modalBody.querySelectorAll('input[name="avatarCategory"]').forEach(input => input.addEventListener('change', draw));
    draw();
  }
  function renderHtmlPreview() {
    const starter = '<main style="font-family:system-ui;padding:24px;color:#172338"><h1>Halo, Ryomd.</h1><p>Pratinjau HTML berjalan di area terisolasi.</p></main>';
    modalBody.innerHTML = `${modalIntro('Tulis HTML, CSS, dan JavaScript lalu jalankan di pratinjau terisolasi. Kode tetap berada di browser dan iframe menggunakan sandbox.')}
      <section class="hp-studio">
        <div class="hp-hero">
          <div class="hp-hero-mark">&lt;/&gt;</div>
          <div class="hp-hero-copy">
            <div class="hp-eyebrow">RYOMD CREATIVE LAB</div>
            <h3>HTML Playground</h3>
            <p>Bangun, uji, dan lihat hasil kode dalam satu workspace.</p>
          </div>
          <span class="hp-safe-pill"><span></span> Sandbox aktif</span>
        </div>
        <div class="hp-workspace">
          <section class="hp-panel hp-editor-panel">
            <div class="hp-panel-head">
              <div><span class="hp-panel-kicker">01 / EDITOR</span><h4>Source code</h4></div>
              <span class="hp-file-chip">HTML · CSS · JS</span>
            </div>
            <div class="hp-editor-caption"><span class="hp-live-dot"></span> Editor lokal <span class="hp-caption-sep">•</span> Tidak diunggah ke server</div>
            <textarea class="tool-textarea hp-code-editor" id="htmlCodeInp" rows="12" spellcheck="false" autocapitalize="off" autocomplete="off" aria-label="Kode HTML CSS dan JavaScript">${starter}</textarea>
            <div class="hp-editor-footer">
              <span class="hp-editor-hint">Tulis markup dan style pada area editor.</span>
              <div class="hp-run-action">${button('Render preview', 'run-html', 'primary')}</div>
            </div>
          </section>
          <section class="hp-panel hp-preview-panel">
            <div class="hp-panel-head">
              <div><span class="hp-panel-kicker">02 / OUTPUT</span><h4>Live preview</h4></div>
              <span class="hp-preview-chip">Preview</span>
            </div>
            <div class="hp-browser-bar"><span class="hp-browser-dots"><i></i><i></i><i></i></span><span class="hp-browser-label">ryomd.local / preview</span><span class="hp-browser-lock">⌑</span></div>
            <iframe class="hp-preview-frame" id="htmlFrame" sandbox="allow-scripts" title="HTML sandbox preview"></iframe>
            <div class="hp-preview-foot"><span class="hp-live-dot"></span> Output terisolasi <span class="hp-caption-sep">•</span> Form, popup, dan navigasi top-level dibatasi</div>
          </section>
        </div>
        <div class="hp-tip"><span class="hp-tip-icon">i</span><p><strong>Catatan keamanan</strong> Kode berjalan di iframe sandbox tanpa akses form, popup, atau navigasi top-level. Jangan masukkan rahasia atau token ke dalam source.</p></div>
      </section>`;
    const render = () => { $('#htmlFrame',modalBody).srcdoc = $('#htmlCodeInp',modalBody).value; };
    render();
  }

  async function renderModuleRegistry() {
    const data = await api('/api/modules');
    const modules = Array.isArray(data.modules) ? data.modules : [];
    const moduleCards = modules.map((module, index) => {
      const status = String(module.status || 'registered');
      const statusClass = status === 'registered' ? 'running' : 'stopped';
      return `<article class="mrx-module-card">
        <div class="mrx-module-symbol" aria-hidden="true">▦</div>
        <div class="mrx-module-main">
          <div class="mrx-module-title-row">
            <h5>${esc(module.name || 'Unnamed module')}</h5>
            <span class="tool-status ${statusClass}">${esc(status)}</span>
          </div>
          <span class="mrx-module-id-label">MODULE ID</span>
          <code class="mrx-module-id">${esc(module.id || '')}</code>
        </div>
        <span class="mrx-module-index">${String(index + 1).padStart(2, '0')}</span>
      </article>`;
    }).join('');

    modalBody.innerHTML = `<section class="mrx-registry" aria-label="Module Registry">
      <header class="mrx-hero">
        <div class="mrx-hero-copy">
          <span class="mrx-eyebrow">RYOMD WORKSPACE / EXTENSIONS</span>
          <h3>Module Registry</h3>
          <p>Kelola identitas modul tambahan dalam satu workspace.</p>
        </div>
        <div class="mrx-stat" aria-label="${modules.length} modul tercatat">
          <strong>${modules.length}</strong>
          <span>MODUL TERCATAT</span>
        </div>
      </header>

      <div class="mrx-safety-note" role="note">
        <span class="mrx-safety-icon" aria-hidden="true">✓</span>
        <div><strong>Registry-only · Aman secara default</strong>
          <p>Pendaftaran hanya menyimpan metadata manifest. Kode modul tidak dijalankan otomatis.</p>
        </div>
      </div>

      <div class="mrx-layout">
        <section class="mrx-form-panel">
          <div class="mrx-panel-heading">
            <span class="mrx-panel-kicker">NEW ENTRY</span>
            <h4>Daftarkan modul</h4>
            <p>Tambahkan identitas modul ke registry workspace.</p>
          </div>
          <div class="mrx-field">
            <label for="nModId">ID modul</label>
            <input class="tool-input" id="nModId" maxlength="48" placeholder="contoh: sticker-maker" autocomplete="off" spellcheck="false">
            <small>Gunakan ID singkat dengan huruf kecil dan tanda hubung.</small>
          </div>
          <div class="mrx-field">
            <label for="nModName">Nama modul</label>
            <input class="tool-input" id="nModName" maxlength="80" placeholder="Sticker Maker" autocomplete="off">
            <small>Nama ini akan ditampilkan pada daftar registry.</small>
          </div>
          <div class="mrx-submit">${button('Daftarkan manifest', 'register-module', 'primary')}</div>
          <p class="mrx-form-footnote"><span aria-hidden="true">ⓘ</span> Modul tidak diaktifkan atau dieksekusi oleh proses pendaftaran ini.</p>
        </section>

        <section class="mrx-list-panel">
          <div class="mrx-list-heading">
            <div><span class="mrx-panel-kicker">WORKSPACE INVENTORY</span><h4>Modul terdaftar</h4></div>
            <span class="mrx-count">${modules.length} ITEM${modules.length === 1 ? '' : 'S'}</span>
          </div>
          <div class="mrx-module-list">
            ${moduleCards || `<div class="mrx-empty-state">
              <span class="mrx-empty-icon" aria-hidden="true">＋</span>
              <strong>Belum ada modul terdaftar</strong>
              <p>Modul yang didaftarkan akan muncul di sini beserta ID dan status manifest-nya.</p>
            </div>`}
          </div>
        </section>
      </div>
    </section>`;
  }

  async function renderAssistant() {
    try { assistantState.config = await api('/api/assistant/status'); }
    catch (error) { assistantState.config = { provider: 'hermes', endpoint: 'http://127.0.0.1:8642', model: 'hermes-agent', configured: false, hasKey: false, keyHint: 'Status server tidak tersedia' }; }
    const cfg = assistantState.config;
    const providerName = cfg.provider === 'gemini' ? 'Gemini API' : 'Hermes Agent';
    modalBody.innerHTML = `<section class="assistant-app assistant-app-v2" aria-label="Ryomd AI Assistant">
      <header class="ra-ai-header">
        <div class="ra-ai-brand">
          <div class="ra-ai-mark" aria-hidden="true"><span>R</span><i></i></div>
          <div class="ra-ai-brand-copy">
            <span class="assistant-eyebrow">PERSONAL AI / RYOMD</span>
            <h3>Halo, aku Ryomd.</h3>
            <p>Asisten pribadimu untuk diskusi, bantuan teknis, dan pekerjaan di workspace ini.</p>
          </div>
        </div>
        <div class="ra-ai-status-area">
          <span class="ra-ai-status-label">KONEKSI AI</span>
          <span class="assistant-connection ${cfg.hasKey ? 'is-ready' : ''}" id="assistantConnectionStatus"><i></i><b>${cfg.configured ? 'Kredensial tersimpan' : 'Belum terhubung'}</b></span>
          <div class="ra-ai-provider-card"><span>PROVIDER</span><strong id="assistantProviderLabel">${esc(providerName)} · ${esc(cfg.model || '')}</strong></div>
        </div>
      </header>
      <nav class="assistant-actions ra-ai-actions" aria-label="Aksi assistant">
        <button class="assistant-chip" type="button" data-action="assistant:quick-status"><span aria-hidden="true">⌁</span> Ringkas status layanan</button>
        <button class="assistant-chip" type="button" data-action="assistant:settings"><span aria-hidden="true">⚙</span> Pengaturan koneksi</button>
        <button class="assistant-chip" type="button" data-action="assistant:new"><span aria-hidden="true">＋</span> Percakapan baru</button>
      </nav>
      <section class="assistant-settings ra-ai-settings" id="assistantSettingsPanel" hidden></section>
      <section class="ra-ai-conversation" aria-label="Percakapan">
        <div class="ra-ai-conversation-head"><div><span class="ra-ai-section-kicker">WORKSPACE CHAT</span><strong>Percakapan</strong></div><span class="ra-ai-local-tag"><i></i> Riwayat lokal</span></div>
        <div class="assistant-chat ra-ai-chat" id="assistantChat" role="log" aria-live="polite" aria-relevant="additions text"></div>
      </section>
      <div class="assistant-composer ra-ai-composer">
        <div class="ra-ai-composer-label"><span>MESSAGE COMPOSER</span><span>MAKS. 6.000 KARAKTER</span></div>
        <div class="ra-ai-composer-row"><textarea id="assistantMessageInput" rows="2" maxlength="6000" placeholder="Tulis pesan untuk Ryomd…" aria-label="Pesan untuk Ryomd"></textarea><button type="button" class="assistant-send" data-action="assistant:send" aria-label="Kirim pesan"><span>➤</span></button></div>
        <div class="assistant-footnote ra-ai-footnote"><span>Enter untuk mengirim · Shift + Enter untuk baris baru</span><span>Pesan diproses melalui provider yang dipilih</span></div>
      </div>
      <p class="assistant-security-note ra-ai-security"><span class="ra-ai-security-icon" aria-hidden="true">◇</span><span><strong>Privasi koneksi</strong>Kunci API disimpan di backend lokal, tidak ditampilkan kembali di browser. Gemini langsung hanya menyediakan percakapan; Hermes dapat memakai tool yang aktif pada konfigurasi Hermes milikmu.</span></p>
    </section>`;
    renderAssistantMessages();
  }
  function saveAssistantMessages() {
    assistantState.messages = assistantState.messages.slice(-80);
    safeStorage.set('ryomd_assistant_messages_v1', assistantState.messages);
    safeStorage.set('ryomd_assistant_conversation_v1', assistantState.conversationId);
  }
  function renderAssistantMessages(loading = false) {
    const chat = $('#assistantChat', modalBody); if (!chat) return;
    if (!assistantState.messages.length) {
      chat.innerHTML = `<div class="assistant-welcome"><span class="assistant-welcome-mark">R</span><b>Ryomd siap membantu.</b><p>Tanyakan apa saja, minta penjelasan tool, atau minta bantuan mendiagnosis layanan yang terhubung.</p><small>Gunakan Pengaturan Koneksi untuk menghubungkan Hermes atau Gemini.</small></div>`;
    } else {
      chat.innerHTML = assistantState.messages.map(item => `<article class="assistant-message ${item.role === 'user' ? 'from-user' : item.role === 'notice' ? 'from-notice' : 'from-assistant'}"><div class="assistant-message-meta"><b>${item.role === 'user' ? 'Kamu' : item.role === 'notice' ? 'Info' : 'Ryomd'}</b><span>${item.at ? new Date(item.at).toLocaleTimeString('id-ID', {hour:'2-digit',minute:'2-digit'}) : ''}</span></div><div class="assistant-message-content">${esc(item.content).replace(/\n/g, '<br>')}</div></article>`).join('');
    }
    if (loading) chat.insertAdjacentHTML('beforeend', '<div class="assistant-typing" id="assistantTyping"><i></i><i></i><i></i><span>Ryomd sedang menyusun jawaban…</span></div>');
    chat.scrollTop = chat.scrollHeight;
  }
  function pushAssistantMessage(role, content) {
    assistantState.messages.push({ role, content: String(content || ''), at: Date.now() });
    saveAssistantMessages();
    renderAssistantMessages(assistantState.sending);
  }
  async function sendAssistantMessage(text) {
    const message = String(text || '').trim();
    if (!message || assistantState.sending) return;
    const history = assistantState.messages.filter(item => ['user','assistant'].includes(item.role)).slice(-18).map(item => ({ role: item.role, content: item.content }));
    pushAssistantMessage('user', message);
    const input = $('#assistantMessageInput', modalBody); if (input) input.value = '';
    assistantState.sending = true; renderAssistantMessages(true);
    const send = $('[data-action="assistant:send"]', modalBody); if (send) send.disabled = true;
    try {
      const data = await postJson('/api/assistant/chat', { conversationId: assistantState.conversationId, message, history });
      pushAssistantMessage('assistant', data.reply || 'Provider tidak mengirim teks jawaban.');
      const label = $('#assistantProviderLabel', modalBody); if (label) label.textContent = `${data.provider === 'gemini' ? 'Gemini API' : 'Hermes Agent'} · ${data.model || ''}`;
    } catch (error) {
      pushAssistantMessage('notice', `Pesan belum terkirim: ${error.message}`);
      toast(error.message, 'error');
    } finally {
      assistantState.sending = false; renderAssistantMessages(false);
      const currentSend = $('[data-action="assistant:send"]', modalBody); if (currentSend) currentSend.disabled = false;
      $('#assistantMessageInput', modalBody)?.focus();
    }
  }
  async function openAssistantSettings() {
    const panel = $('#assistantSettingsPanel', modalBody); if (!panel) return;
    const cfg = assistantState.config || await api('/api/assistant/status');
    panel.hidden = false;
    panel.innerHTML = `<div class="assistant-settings-head"><div><b>Koneksi model AI</b><small>Rahasia koneksi hanya disimpan oleh server lokal.</small></div><button type="button" class="assistant-chip" data-action="assistant:settings-close">Tutup</button></div>
      <div class="tool-field"><label for="assistantProvider">Provider AI</label><select class="tool-select" id="assistantProvider"><option value="hermes" ${cfg.provider === 'hermes' ? 'selected' : ''}>Hermes Agent (disarankan untuk tool)</option><option value="gemini" ${cfg.provider === 'gemini' ? 'selected' : ''}>Google Gemini API (chat)</option></select></div>
      <div class="tool-field" id="assistantEndpointField"><label for="assistantEndpoint">Alamat Hermes lokal</label><input class="tool-input" id="assistantEndpoint" value="${esc(cfg.endpoint || 'http://127.0.0.1:8642')}" placeholder="http://127.0.0.1:8642" autocomplete="off"><small class="assistant-help">Default Hermes API: http://127.0.0.1:8642. Port dapat diganti bila gateway milikmu memakai port berbeda.</small></div>
      <div class="tool-field"><label for="assistantModel">Model</label><input class="tool-input" id="assistantModel" value="${esc(cfg.model || (cfg.provider === 'gemini' ? 'gemini-3.8-flash' : 'hermes-agent'))}" maxlength="100" autocomplete="off"></div>
      <div class="tool-field"><label for="assistantApiKey">API key / API_SERVER_KEY</label><input class="tool-input" id="assistantApiKey" type="password" value="" autocomplete="new-password" spellcheck="false" placeholder="${cfg.hasKey ? 'Sudah tersimpan — kosongkan untuk mempertahankan' : 'Tempel key di sini'}"><small class="assistant-help">Status: ${esc(cfg.keyHint || (cfg.hasKey ? 'Tersimpan' : 'Belum diatur'))}. Kolom tidak akan menampilkan key yang tersimpan.</small></div>
      <label class="assistant-clear-key"><input type="checkbox" id="assistantClearKey"> Hapus key yang tersimpan</label>
      <div class="assistant-settings-buttons"><button type="button" class="tool-btn primary" data-action="assistant:save-settings">Simpan koneksi</button><button type="button" class="tool-btn" data-action="assistant:test">Tes koneksi</button></div>
      <p class="assistant-settings-status" id="assistantSettingsStatus" aria-live="polite">${cfg.hasKey ? 'Kredensial sudah tersimpan. Tekan Tes koneksi untuk memverifikasi gateway/provider.' : 'Belum ada kredensial. Simpan koneksi setelah mengisi API key.'}</p>
      <div class="assistant-setup-note"><b>Petunjuk Hermes</b><p>Aktifkan <code>API_SERVER_ENABLED=true</code> dan tetapkan <code>API_SERVER_KEY</code> pada file <code>~/.hermes/.env</code>, lalu jalankan <code>hermes gateway</code>. Setelah itu masukkan alamat gateway dan key tersebut di sini.</p><b>Petunjuk Gemini</b><p>Pilih Gemini, masukkan API key dari Google AI Studio dan model yang tersedia pada akunmu. Key tidak perlu dimasukkan ke source HTML/JavaScript.</p></div>`;
    const updateProvider = () => { const gemini = $('#assistantProvider', panel).value === 'gemini'; $('#assistantEndpointField', panel).hidden = gemini; if (gemini && $('#assistantModel', panel).value === 'hermes-agent') $('#assistantModel', panel).value = 'gemini-3.8-flash'; if (!gemini && $('#assistantModel', panel).value.startsWith('gemini-')) $('#assistantModel', panel).value = 'hermes-agent'; };
    $('#assistantProvider', panel).addEventListener('change', updateProvider); updateProvider();
  }
  async function saveAssistantSettings() {
    const status = $('#assistantSettingsStatus', modalBody); if (status) status.textContent = 'Menyimpan pengaturan…';
    try {
      const data = await postJson('/api/assistant/settings', { provider: $('#assistantProvider', modalBody).value, endpoint: $('#assistantEndpoint', modalBody)?.value || 'http://127.0.0.1:8642', model: $('#assistantModel', modalBody).value, apiKey: $('#assistantApiKey', modalBody).value, clearKey: $('#assistantClearKey', modalBody).checked });
      assistantState.config = data;
      if ($('#assistantApiKey', modalBody)) $('#assistantApiKey', modalBody).value = '';
      if ($('#assistantClearKey', modalBody)) $('#assistantClearKey', modalBody).checked = false;
      if (status) status.textContent = data.message + ' Tekan Tes koneksi untuk memverifikasi.';
      const pill = $('#assistantConnectionStatus', modalBody); if (pill) { pill.innerHTML = `<i></i><b>${data.hasKey ? 'Kredensial tersimpan' : 'Belum terhubung'}</b>`; pill.classList.toggle('is-ready', !!data.hasKey); }
      const label = $('#assistantProviderLabel', modalBody); if (label) label.textContent = `${data.provider === 'gemini' ? 'Gemini API' : 'Hermes Agent'} · ${data.model}`;
      toast('Pengaturan AI disimpan.', 'success');
    } catch (error) { if (status) status.textContent = error.message; toast(error.message, 'error'); }
  }
  async function testAssistantConnection() {
    const status = $('#assistantSettingsStatus', modalBody); if (status) status.textContent = 'Menghubungi provider AI…';
    try { const data = await postJson('/api/assistant/test', {}); if (status) status.textContent = `${data.message} ${data.model?.length ? 'Model tersedia: ' + data.model.join(', ') : ''}`; const pill = $('#assistantConnectionStatus', modalBody); if (pill) { pill.classList.add('is-ready'); pill.innerHTML = '<i></i><b>Terhubung</b>'; } toast(data.message, 'success'); }
    catch (error) { if (status) status.textContent = error.message; const pill = $('#assistantConnectionStatus', modalBody); if (pill) { pill.classList.remove('is-ready'); pill.innerHTML = '<i></i><b>Tidak terhubung</b>'; } toast(error.message, 'error'); }
  }
  async function assistantQuickStatus() {
    try {
      const data = await api('/api/services');
      const details = (data.services || []).map(item => `${item.name}: ${item.status}; entry=${item.entry || '—'}; entry tersedia=${item.hasEntry ? 'ya' : 'tidak'}`).join('\n');
      await sendAssistantMessage(`Tolong analisis kondisi layanan Ryomd ini. Tandai mana yang berjalan, mana yang belum siap, dan berikan langkah perbaikan yang sesuai tanpa mengklaim telah mengubah apa pun.

${details || 'Belum ada data layanan.'}`);
    } catch (error) { toast(`Tidak bisa membaca status layanan: ${error.message}`, 'error'); }
  }

  
  // RYOMD_BMKG_REGION_PICKER_V1 — ADM4 names sourced from the 2022 Kemendagri code dataset.
  const BMKG_REGION_CSV_URL = 'https://raw.githubusercontent.com/mrayhanfadil/kodepos-id/main/kodepos_parsed.csv';
  const BMKG_REGION_CACHE_DB = 'ryomd-bmkg-region-cache-v1';
  const BMKG_REGION_CACHE_KEY = 'kodepos-parsed-kemendagri-2022-20260821';

  function bmkgOpenRegionCache() {
    return new Promise((resolve) => {
      if (!window.indexedDB) return resolve(null);
      let request;
      try { request = window.indexedDB.open(BMKG_REGION_CACHE_DB, 1); }
      catch { return resolve(null); }
      request.onupgradeneeded = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains('datasets')) db.createObjectStore('datasets');
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => resolve(null);
      request.onblocked = () => resolve(null);
    });
  }

  async function bmkgReadRegionCache() {
    const db = await bmkgOpenRegionCache();
    if (!db) return '';
    return new Promise((resolve) => {
      try {
        const request = db.transaction('datasets', 'readonly').objectStore('datasets').get(BMKG_REGION_CACHE_KEY);
        request.onsuccess = () => { const value = typeof request.result === 'string' ? request.result : ''; db.close(); resolve(value); };
        request.onerror = () => { db.close(); resolve(''); };
      } catch { try { db.close(); } catch {} resolve(''); }
    });
  }

  async function bmkgWriteRegionCache(csv) {
    const db = await bmkgOpenRegionCache();
    if (!db) return false;
    return new Promise((resolve) => {
      try {
        const tx = db.transaction('datasets', 'readwrite');
        tx.objectStore('datasets').put(csv, BMKG_REGION_CACHE_KEY);
        tx.oncomplete = () => { db.close(); resolve(true); };
        tx.onerror = tx.onabort = () => { db.close(); resolve(false); };
      } catch { try { db.close(); } catch {} resolve(false); }
    });
  }

  async function bmkgGetRegionCsv(statusNode) {
    const cached = await bmkgReadRegionCache();
    if (cached.length > 1000) {
      if (statusNode) statusNode.textContent = 'Daftar wilayah dimuat dari penyimpanan perangkat.';
      return cached;
    }
    if (statusNode) statusNode.textContent = 'Mengunduh direktori wilayah ADM4 (sekitar 9 MB, hanya pertama kali)…';
    const response = await fetch(BMKG_REGION_CSV_URL, { method: 'GET', mode: 'cors', cache: 'force-cache' });
    if (!response.ok) throw new Error('Direktori wilayah tidak dapat diunduh (HTTP ' + response.status + '). Periksa koneksi internet lalu coba lagi.');
    const csv = await response.text();
    if (csv.length < 1000 || !/kode_wilayah/i.test(csv.slice(0, 2000))) throw new Error('Format direktori wilayah tidak dikenali. Coba muat ulang beberapa saat lagi.');
    await bmkgWriteRegionCache(csv);
    return csv;
  }

  function bmkgParseRegionCsv(csv) {
    const records = [];
    let row = [], field = '', quoted = false;
    for (let i = 0; i < csv.length; i++) {
      const ch = csv[i];
      if (ch === '"') {
        if (quoted && csv[i + 1] === '"') { field += '"'; i++; }
        else quoted = !quoted;
      } else if (ch === ',' && !quoted) {
        row.push(field); field = '';
      } else if ((ch === '\n' || ch === '\r') && !quoted) {
        if (ch === '\r' && csv[i + 1] === '\n') i++;
        row.push(field); field = '';
        if (row.some(value => value !== '')) records.push(row);
        row = [];
      } else field += ch;
    }
    if (field.length || row.length) { row.push(field); if (row.some(value => value !== '')) records.push(row); }
    if (records.length < 2) throw new Error('Daftar wilayah kosong atau tidak lengkap.');
    const header = records.shift().map((value, index) => value.trim().replace(/^\uFEFF/, '').toLowerCase());
    const col = (...names) => header.findIndex(value => names.includes(value));
    const idx = {
      code: col('kode_wilayah'), village: col('desa'), district: col('kecamatan'),
      regency: col('kabupaten'), province: col('provinsi'), type: col('dt2')
    };
    if (Object.values(idx).slice(0, 5).some(value => value < 0)) throw new Error('Kolom kode dan nama wilayah tidak lengkap pada dataset.');
    const output = [];
    for (const values of records) {
      const code = String(values[idx.code] || '').trim();
      const parts = code.split('.');
      if (!/^\d{2}\.\d{2}\.\d{2}\.\d{4}$/.test(code) || parts.length !== 4) continue;
      const village = String(values[idx.village] || '').trim();
      const district = String(values[idx.district] || '').trim();
      const regency = String(values[idx.regency] || '').trim();
      const province = String(values[idx.province] || '').trim();
      if (!village || !district || !regency || !province) continue;
      output.push({
        code, provinceCode: parts[0], regencyCode: parts.slice(0, 2).join('.'),
        districtCode: parts.slice(0, 3).join('.'), province, regency, district, village,
        regencyType: idx.type >= 0 ? String(values[idx.type] || '').trim() : ''
      });
    }
    if (!output.length) throw new Error('Tidak ada kode ADM4 valid di direktori wilayah.');
    return output;
  }

  function bmkgUniqueRegionOptions(rows, codeKey, nameKey, prefix) {
    const map = new Map();
    for (const row of rows) {
      if (prefix && !row[codeKey].startsWith(prefix)) continue;
      const code = row[codeKey];
      if (!map.has(code)) {
        let name = row[nameKey];
        if (codeKey === 'regencyCode' && row.regencyType) name = row.regencyType + ' ' + name;
        map.set(code, name);
      }
    }
    return [...map].map(([code, name]) => ({ code, name })).sort((a, b) => a.name.localeCompare(b.name, 'id'));
  }

  function bmkgSetSelectOptions(select, items, placeholder) {
    if (!select) return;
    select.replaceChildren();
    const first = document.createElement('option');
    first.value = ''; first.textContent = placeholder; select.appendChild(first);
    for (const item of items) {
      const option = document.createElement('option');
      option.value = item.code; option.textContent = item.name;
      select.appendChild(option);
    }
    select.disabled = items.length === 0;
  }

  async function initBmkgRegionPicker() {
    const provinceSelect = $('#bmkgProvince', modalBody);
    const regencySelect = $('#bmkgRegency', modalBody);
    const districtSelect = $('#bmkgDistrict', modalBody);
    const villageSelect = $('#bmkgVillage', modalBody);
    const codeInput = $('#bmkgCode', modalBody);
    const summary = $('#bmkgRegionSummary', modalBody);
    const status = $('#bmkgRegionStatus', modalBody);
    const loadButton = $('[data-action="load-weather"]', modalBody);
    if (!provinceSelect || !regencySelect || !districtSelect || !villageSelect || !codeInput) return;
    const defaultCode = '33.10.25.1003';
    const setStatus = (message, isError = false) => {
      if (status) { status.textContent = message; status.classList.toggle('is-error', isError); }
    };
    const resetBelowProvince = () => {
      bmkgSetSelectOptions(regencySelect, [], 'Pilih kabupaten/kota');
      bmkgSetSelectOptions(districtSelect, [], 'Pilih kecamatan');
      bmkgSetSelectOptions(villageSelect, [], 'Pilih desa/kelurahan');
      codeInput.value = '';
    };
    const resetBelowRegency = () => {
      bmkgSetSelectOptions(districtSelect, [], 'Pilih kecamatan');
      bmkgSetSelectOptions(villageSelect, [], 'Pilih desa/kelurahan');
      codeInput.value = '';
    };
    const resetBelowDistrict = () => {
      bmkgSetSelectOptions(villageSelect, [], 'Pilih desa/kelurahan');
      codeInput.value = '';
    };
    const updateSummary = () => {
      const row = rows.find(item => item.code === villageSelect.value);
      codeInput.value = row ? row.code : '';
      if (loadButton) loadButton.disabled = !row;
      if (summary) {
        summary.innerHTML = row
          ? '<b>Lokasi dipilih</b><span>' + [row.village, row.district, row.regencyType ? row.regencyType + ' ' + row.regency : row.regency, row.province].map(esc).join(' · ') + '</span><small>Kode ADM4 terisi otomatis untuk permintaan BMKG.</small>'
          : '<b>Belum ada lokasi dipilih</b><span>Pilih provinsi, kabupaten/kota, kecamatan, lalu desa/kelurahan.</span>';
      }
      if (row) setStatus('Lokasi siap. Tekan Ambil prakiraan untuk melihat data BMKG.');
    };
    let rows;
    try {
      const csv = await bmkgGetRegionCsv(status);
      rows = bmkgParseRegionCsv(csv);
      bmkgSetSelectOptions(provinceSelect, bmkgUniqueRegionOptions(rows, 'provinceCode', 'province'), 'Pilih provinsi');
      resetBelowProvince();
      const fillRegencies = () => {
        const selected = provinceSelect.value;
        bmkgSetSelectOptions(regencySelect, bmkgUniqueRegionOptions(rows.filter(row => row.provinceCode === selected), 'regencyCode', 'regency'), 'Pilih kabupaten/kota');
        resetBelowRegency();
      };
      const fillDistricts = () => {
        const selected = regencySelect.value;
        bmkgSetSelectOptions(districtSelect, bmkgUniqueRegionOptions(rows.filter(row => row.regencyCode === selected), 'districtCode', 'district'), 'Pilih kecamatan');
        resetBelowDistrict();
      };
      const fillVillages = () => {
        const selected = districtSelect.value;
        const items = rows.filter(row => row.districtCode === selected)
          .map(row => ({ code: row.code, name: row.village + (row.code.slice(-4).startsWith('1') ? ' · Kelurahan' : ' · Desa') }))
          .sort((a, b) => a.name.localeCompare(b.name, 'id'));
        bmkgSetSelectOptions(villageSelect, items, 'Pilih desa/kelurahan');
        codeInput.value = '';
      };
      provinceSelect.addEventListener('change', () => { fillRegencies(); updateSummary(); });
      regencySelect.addEventListener('change', () => { fillDistricts(); updateSummary(); });
      districtSelect.addEventListener('change', () => { fillVillages(); updateSummary(); });
      villageSelect.addEventListener('change', updateSummary);
      const initial = rows.find(row => row.code === defaultCode);
      if (initial) {
        provinceSelect.value = initial.provinceCode; fillRegencies();
        regencySelect.value = initial.regencyCode; fillDistricts();
        districtSelect.value = initial.districtCode; fillVillages();
        villageSelect.value = initial.code;
      }
      updateSummary();
      setStatus('Direktori siap: ' + rows.length.toLocaleString('id-ID') + ' desa/kelurahan.');
    } catch (error) {
      resetBelowProvince();
      setStatus(error.message || 'Gagal memuat daftar wilayah.', true);
      const retry = $('#bmkgRetryRegions', modalBody);
      if (retry) retry.hidden = false;
    }
  }
  // END RYOMD_BMKG_REGION_PICKER_V1

  
  // RYOMD_BMKG_WEATHER_VISUAL_RENDER_V2 — present BMKG forecast as cards and a temperature chart.
  function bmkgWeatherIcon(description) {
    const value = String(description || '').toLowerCase();
    if (/petir|thunder|badai/.test(value)) return '⛈️';
    if (/hujan/.test(value)) return /lebat|sedang/.test(value) ? '🌧️' : '🌦️';
    if (/kabut|asap|mist|fog/.test(value)) return '🌫️';
    if (/cerah/.test(value) && /awan/.test(value)) return '🌤️';
    if (/cerah|sunny|clear/.test(value)) return '☀️';
    if (/berawan|awan|cloud/.test(value)) return '☁️';
    return '🌥️';
  }

  function bmkgWeatherTime(value) {
    const text = String(value || '');
    const match = text.match(/(\d{2}:\d{2})/);
    return match ? match[1] : '—';
  }

  function bmkgWeatherDate(value) {
    const text = String(value || '');
    const date = text.slice(0, 10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return 'Periode prakiraan';
    const parsed = new Date(date + 'T00:00:00');
    if (Number.isNaN(parsed.getTime())) return date;
    return parsed.toLocaleDateString('id-ID', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  function bmkgWeatherValue(value, suffix = '') {
    if (value === null || value === undefined || value === '') return '—';
    const number = Number(value);
    return Number.isFinite(number) ? String(number) + suffix : '—';
  }

  function bmkgRenderTemperatureChart(rows) {
    const points = rows.map((item, index) => ({
      index,
      time: bmkgWeatherTime(item.local_datetime),
      value: Number(item.t)
    })).filter(point => Number.isFinite(point.value));
    if (!points.length) return '<div class="bmkg-chart-empty">Data suhu tidak tersedia untuk divisualisasikan.</div>';

    const width = 760, height = 218, left = 54, right = 22, top = 24, bottom = 52;
    const plotWidth = width - left - right, plotHeight = height - top - bottom;
    let min = Math.min(...points.map(point => point.value));
    let max = Math.max(...points.map(point => point.value));
    if (min === max) { min -= 2; max += 2; }
    else { min -= 1; max += 1; }
    const x = index => points.length === 1 ? left + plotWidth / 2 : left + (index / (points.length - 1)) * plotWidth;
    const y = value => top + ((max - value) / (max - min)) * plotHeight;
    const polyline = points.map((point, index) => `${x(index).toFixed(1)},${y(point.value).toFixed(1)}`).join(' ');
    const ticks = 4;
    const grid = Array.from({ length: ticks }, (_, index) => {
      const value = max - ((max - min) * index / (ticks - 1));
      const yy = y(value);
      return `<line x1="${left}" y1="${yy.toFixed(1)}" x2="${width-right}" y2="${yy.toFixed(1)}" class="bmkg-chart-grid"/><text x="${left-12}" y="${(yy+4).toFixed(1)}" text-anchor="end" class="bmkg-chart-axis">${Math.round(value)}°</text>`;
    }).join('');
    const labels = points.map((point, index) => {
      const xx = x(index);
      return `<text x="${xx.toFixed(1)}" y="${height-17}" text-anchor="middle" class="bmkg-chart-axis">${esc(point.time)}</text>`;
    }).join('');
    const dots = points.map((point, index) => {
      const xx = x(index), yy = y(point.value);
      return `<circle cx="${xx.toFixed(1)}" cy="${yy.toFixed(1)}" r="5" class="bmkg-chart-dot"/><text x="${xx.toFixed(1)}" y="${(yy-12).toFixed(1)}" text-anchor="middle" class="bmkg-chart-value">${point.value}°</text>`;
    }).join('');
    return `<svg class="bmkg-temp-chart" viewBox="0 0 ${width} ${height}" role="img" aria-label="Grafik perubahan suhu prakiraan BMKG">${grid}<defs><linearGradient id="bmkgTempLine" x1="0" x2="1"><stop offset="0%" stop-color="#8e7cff"/><stop offset="100%" stop-color="#55c8bd"/></linearGradient></defs><polyline points="${polyline}" class="bmkg-chart-line"/>${dots}${labels}</svg>`;
  }

  function bmkgRenderForecastVisual(rows) {
    const first = rows[0] || {};
    const temperatures = rows.map(item => Number(item.t)).filter(Number.isFinite);
    const humidities = rows.map(item => Number(item.hu)).filter(Number.isFinite);
    const minTemp = temperatures.length ? Math.min(...temperatures) : null;
    const maxTemp = temperatures.length ? Math.max(...temperatures) : null;
    const avgHumidity = humidities.length ? Math.round(humidities.reduce((sum, value) => sum + value, 0) / humidities.length) : null;
    const firstDescription = String(first.weather_desc || first.weather || 'Kondisi tidak tersedia');
    const selectedLocation = $('#bmkgRegionSummary span', modalBody)?.textContent || 'Wilayah terpilih';
    const dateLabel = bmkgWeatherDate(first.local_datetime);
    const cards = rows.map((item, index) => {
      const description = String(item.weather_desc || item.weather || 'Kondisi tidak tersedia');
      const rain = /hujan|petir|badai/i.test(description);
      const time = bmkgWeatherTime(item.local_datetime);
      const temp = bmkgWeatherValue(item.t, '°');
      const humidity = bmkgWeatherValue(item.hu, '%');
      const date = index === 0 || String(item.local_datetime || '').slice(0, 10) !== String(rows[index-1]?.local_datetime || '').slice(0, 10)
        ? bmkgWeatherDate(item.local_datetime) : '';
      return `<article class="bmkg-hour-card${rain ? ' is-rain' : ''}"><div class="bmkg-hour-card-top"><b>${esc(time)}</b><span>${esc(date || 'Prakiraan')}</span></div><div class="bmkg-hour-icon" aria-hidden="true">${bmkgWeatherIcon(description)}</div><strong class="bmkg-hour-temp">${esc(temp)}</strong><span class="bmkg-hour-condition">${esc(description)}</span><span class="bmkg-hour-humidity">💧 Kelembapan ${esc(humidity)}</span></article>`;
    }).join('');

    return `<div class="bmkg-visual-dashboard">
      <section class="bmkg-current-overview"><div class="bmkg-current-copy"><span class="bmkg-overview-kicker">RINGKASAN PRAKIRAAN · ${esc(dateLabel)}</span><h5>${esc(selectedLocation)}</h5><p>Data awal periode: ${esc(bmkgWeatherTime(first.local_datetime))} WIB</p><div class="bmkg-current-condition"><span class="bmkg-current-icon" aria-hidden="true">${bmkgWeatherIcon(firstDescription)}</span><div><b>${esc(firstDescription)}</b><small>Prakiraan BMKG, bukan pengamatan langsung</small></div></div></div><div class="bmkg-current-temp">${Number.isFinite(Number(first.t)) ? `${Math.round(Number(first.t))}<sup>°C</sup>` : '—'}<small>Suhu awal prakiraan</small></div></section>
      <div class="bmkg-metric-grid"><div class="bmkg-metric-card"><span>SUHU TERENDAH</span><b>${minTemp === null ? '—' : `${minTemp}°C`}</b><small>Dalam data yang ditampilkan</small></div><div class="bmkg-metric-card"><span>SUHU TERTINGGI</span><b>${maxTemp === null ? '—' : `${maxTemp}°C`}</b><small>Dalam data yang ditampilkan</small></div><div class="bmkg-metric-card"><span>RATA-RATA KELEMBAPAN</span><b>${avgHumidity === null ? '—' : `${avgHumidity}%`}</b><small>Dihitung dari titik prakiraan</small></div><div class="bmkg-metric-card"><span>DATA PRAKIRAAN</span><b>${rows.length}</b><small>Titik waktu ditampilkan</small></div></div>
      <section class="bmkg-chart-card"><div class="bmkg-visual-section-head"><div><span class="bmkg-section-kicker">TEMPERATURE TREND</span><h5>Perubahan suhu</h5></div><span class="bmkg-chart-unit">°C · sesuai data BMKG</span></div>${bmkgRenderTemperatureChart(rows)}</section>
      <section class="bmkg-hourly-section"><div class="bmkg-visual-section-head"><div><span class="bmkg-section-kicker">HOURLY FORECAST</span><h5>Prakiraan per waktu</h5></div><span class="bmkg-hour-count">${rows.length} data</span></div><div class="bmkg-hour-grid">${cards}</div></section>
      <div class="bmkg-visual-footnote"><span>☁</span><p>Informasi ini merupakan prakiraan cuaca dari BMKG. Kondisi aktual dapat berbeda. Data suhu, kelembapan, waktu, dan deskripsi cuaca ditampilkan sesuai respons BMKG.</p></div>
    </div>`;
  }
  // END RYOMD_BMKG_WEATHER_VISUAL_RENDER_V2

// RYOMD_MEDIA_CENTER_GOOGLE_ACCOUNT_V1  (V2 — Ryomd Tube: tampilan ala YouTube, langganan lokal, impor Takeout)
  const RYOMD_YT_HISTORY_KEY = 'ryomd_youtube_watch_history_v1';
  const RYOMD_YT_ACCOUNT_KEY = 'ryomd_youtube_google_profile_v1';
  const RYOMD_YT_SUBS_KEY = 'ryomd_youtube_local_subs_v1';
  const RYOMD_YT_HISTORY_MAX = 300;
  const RYOMD_YT_SUBS_MAX = 500;
  const RYOMD_YT_FEED_TTL = 10 * 60 * 1000;
  const ryomdYouTubeReadJson = (key, fallback) => {
    try { const value = JSON.parse(localStorage.getItem(key) || 'null'); return value == null ? fallback : value; }
    catch (_) { return fallback; }
  };
  const ryomdYouTubeWriteJson = (key, value) => { try { localStorage.setItem(key, JSON.stringify(value)); return true; } catch (_) { return false; } };
  const ryomdYouTubeState = {
    query: '', currentVideoId: '', lastResults: [], currentChannelUrl: '', currentChannel: null,
    view: 'home', nav: [], current: null, autoTried: false,
    account: ryomdYouTubeReadJson(RYOMD_YT_ACCOUNT_KEY, null),
    sessionReady: false, subscriptions: [], playlists: [],
    history: ryomdYouTubeReadJson(RYOMD_YT_HISTORY_KEY, []),
    localSubs: ryomdYouTubeReadJson(RYOMD_YT_SUBS_KEY, []),
    searchCache: new Map(), feedCache: null, homeHasMore: false, homeLoadingMore: false
  };
  if (!Array.isArray(ryomdYouTubeState.history)) ryomdYouTubeState.history = [];
  if (!Array.isArray(ryomdYouTubeState.localSubs)) ryomdYouTubeState.localSubs = [];

  const RYOMD_YT_NATIVE_BUTTONS = '[data-action="yt-native-play"], [data-action="yt-native-audio"], [data-action="yt-link-play"], [data-action="yt-link-audio"]';
  const RYT_ICONS = {
    play: '<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false"><path d="M8 5v14l11-7z" fill="currentColor"/></svg>',
    music: '<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false"><path d="M9 18V5l11-2v13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><circle cx="6" cy="18" r="3" fill="currentColor"/><circle cx="17" cy="16" r="3" fill="currentColor"/></svg>',
    home: '<svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" focusable="false"><path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
    subs: '<svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" focusable="false"><rect x="3" y="7" width="18" height="13" rx="2.5" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M7 4h10M10 11l5 2.5-5 2.5z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    history: '<svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" focusable="false"><path d="M3.5 12a8.5 8.5 0 1 0 2.6-6.1L3.5 8.5M3.5 4v4.5H8M12 7.5V12l3 2" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    library: '<svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" focusable="false"><path d="M4 6h12M4 10.5h12M4 15h7M17 13v6l5-3z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    search: '<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false"><circle cx="11" cy="11" r="6.5" fill="none" stroke="currentColor" stroke-width="2"/><path d="m16 16 4.5 4.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
    link: '<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false"><path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
    back: '<svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true" focusable="false"><path d="M15 5 8 12l7 7" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    check: '<svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" focusable="false"><path d="m5 12.5 4.5 4.5L19 7.5" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    external: '<svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" focusable="false"><path d="M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>'
  };
  const RYOMD_YT_ICON_PLAY = RYT_ICONS.play;
  const RYOMD_YT_ICON_MUSIC = RYT_ICONS.music;

  // ------------------------------------------------------------------ util
  const rytVideoId = (value) => {
    const text = String(value || '').trim();
    if (/^[A-Za-z0-9_-]{11}$/.test(text)) return text;
    try {
      const url = new URL(text);
      const v = url.searchParams.get('v');
      if (v && /^[A-Za-z0-9_-]{11}$/.test(v)) return v;
      const parts = url.pathname.split('/').filter(Boolean);
      if (/youtu\.be$/i.test(url.hostname) && /^[A-Za-z0-9_-]{11}$/.test(parts[0] || '')) return parts[0];
      if (['shorts', 'embed', 'live'].includes(parts[0]) && /^[A-Za-z0-9_-]{11}$/.test(parts[1] || '')) return parts[1];
    } catch (_) { }
    return '';
  };
  const rytCompact = new Intl.NumberFormat('id-ID', { notation: 'compact', maximumFractionDigits: 1 });
  const rytViews = (n) => { n = Number(n); return Number.isFinite(n) && n > 0 ? `${rytCompact.format(n)} tayangan` : ''; };
  const rytDuration = (secs) => {
    secs = Math.floor(Number(secs));
    if (!Number.isFinite(secs) || secs <= 0) return '';
    const h = Math.floor(secs / 3600), m = Math.floor((secs % 3600) / 60), s = secs % 60;
    return h ? `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}` : `${m}:${String(s).padStart(2, '0')}`;
  };
  const rytAgo = (ts) => {
    const diff = Date.now() - Number(ts || 0);
    if (!Number.isFinite(diff) || diff < 0) return '';
    const min = Math.floor(diff / 60000);
    if (min < 1) return 'Baru saja';
    if (min < 60) return `${min} menit lalu`;
    const hr = Math.floor(min / 60);
    if (hr < 24) return `${hr} jam lalu`;
    const day = Math.floor(hr / 24);
    return day < 30 ? `${day} hari lalu` : new Date(ts).toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' });
  };
  const rytColor = (text) => {
    let hash = 0; for (const ch of String(text || 'R')) hash = (hash * 31 + ch.charCodeAt(0)) | 0;
    return `hsl(${Math.abs(hash) % 360} 45% 38%)`;
  };
  const rytInitial = (text) => esc((String(text || 'R').trim()[0] || 'R').toUpperCase());
  const rytAvatar = (name, image, cls = '') => image
    ? `<img class="ryt-avatar ${cls}" src="${esc(image)}" data-i="${rytInitial(name)}" alt="" loading="lazy" referrerpolicy="no-referrer" onerror="this.replaceWith(Object.assign(document.createElement('span'),{className:this.className,textContent:this.dataset.i}))">`
    : `<span class="ryt-avatar ${cls}" style="background:${rytColor(name)}" aria-hidden="true">${rytInitial(name)}</span>`;
  const rytChannelKey = (url) => String(url || '').toLowerCase().replace(/^https?:\/\/(www\.|m\.)?youtube\.com/, '').replace(/[?#].*$/, '').replace(/\/+$/, '');
  const rytScrollTop = () => { try { modalBody.scrollTo({ top: 0 }); } catch (_) { modalBody.scrollTop = 0; } };

  // ------------------------------------------------- data layer (native -> Termux)
  const rytNativeCalls = new Map();
  window.ryomdYouTubeCatalog = {
    done(id, text) {
      const entry = rytNativeCalls.get(id);
      if (!entry) return;
      rytNativeCalls.delete(id); window.clearTimeout(entry.timer);
      let data = null;
      try { data = JSON.parse(text); } catch (_) { entry.reject(new Error('Respons katalog native tidak valid.')); return; }
      if (data && data.ok !== false) entry.resolve(data); else entry.reject(new Error(String(data && data.error || 'Katalog native gagal.')));
    }
  };
  function rytNative(method, ...args) {
    return new Promise((resolve, reject) => {
      const bridge = window.RyomdYouTubeCatalog;
      if (!bridge || typeof bridge[method] !== 'function') { reject(new Error('Katalog native hanya tersedia di APK.')); return; }
      const id = 'c' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
      const timer = window.setTimeout(() => { rytNativeCalls.delete(id); reject(new Error('Katalog native tidak merespons (timeout).')); }, 45000);
      rytNativeCalls.set(id, { resolve, reject, timer });
      try { bridge[method](id, ...args); } catch (error) { window.clearTimeout(timer); rytNativeCalls.delete(id); reject(error); }
    });
  }
  /** Ambil data YouTube: native NewPipe lebih dulu (tanpa Termux), cadangan ke backend Termux. */
  async function rytData(kind, value, limit, offset = 0) {
    const hasNative = Boolean(window.RyomdYouTubeCatalog);
    const backend = () => {
      if (kind === 'search') return api(`/api/youtube/search?q=${encodeURIComponent(value)}&limit=${limit || 40}&offset=${Math.max(0, Number(offset) || 0)}`);
      if (kind === 'channel') return api(`/api/youtube/channel?url=${encodeURIComponent(value)}`);
      return api(`/api/youtube/video?id=${encodeURIComponent(value)}`);
    };
    const native = () => kind === 'search' ? rytNative('searchPage', value, limit || 40, Math.max(0, Number(offset) || 0))
      : kind === 'channel' ? rytNative('channel', value, limit || 24) : rytNative('video', value);
    const order = hasNative ? [native, backend] : [backend];
    let firstError = null;
    for (const attempt of order) {
      try { return await attempt(); } catch (error) { firstError = firstError || error; }
    }
    throw firstError || new Error('Sumber data YouTube tidak tersedia.');
  }

  // --------------------------------------------------------- local library
  function rytSaveSubs() { ryomdYouTubeState.localSubs = ryomdYouTubeState.localSubs.slice(0, RYOMD_YT_SUBS_MAX); ryomdYouTubeWriteJson(RYOMD_YT_SUBS_KEY, ryomdYouTubeState.localSubs); ryomdYouTubeState.feedCache = null; }
  const rytIsSubscribed = (url) => { const key = rytChannelKey(url); return Boolean(key) && ryomdYouTubeState.localSubs.some(row => rytChannelKey(row.url) === key); };
  function rytToggleSubscription(channel) {
    if (!channel || !channel.url) return false;
    const key = rytChannelKey(channel.url);
    const exists = ryomdYouTubeState.localSubs.some(row => rytChannelKey(row.url) === key);
    if (exists) ryomdYouTubeState.localSubs = ryomdYouTubeState.localSubs.filter(row => rytChannelKey(row.url) !== key);
    else ryomdYouTubeState.localSubs.unshift({ url: String(channel.url), title: String(channel.title || 'Channel YouTube'), thumbnail: String(channel.thumbnail || ''), addedAt: Date.now() });
    rytSaveSubs();
    return !exists;
  }
  function rytMergeSubs(rows) {
    let added = 0;
    for (const row of rows) {
      if (!row || !row.url) continue;
      const key = rytChannelKey(row.url);
      if (!key || ryomdYouTubeState.localSubs.some(item => rytChannelKey(item.url) === key)) continue;
      ryomdYouTubeState.localSubs.push({ url: String(row.url), title: String(row.title || 'Channel YouTube'), thumbnail: String(row.thumbnail || ''), addedAt: Date.now() }); added++;
    }
    if (added) rytSaveSubs();
    return added;
  }
  function ryomdYouTubeSaveHistory(video) {
    const id = rytVideoId(video && video.id);
    if (!id) return;
    const previous = ryomdYouTubeState.history.find(row => row && row.id === id) || {};
    const item = {
      id, title: String(video.title || previous.title || 'Video YouTube'),
      channel: String(video.channel || previous.channel || 'Channel YouTube'),
      channelUrl: String(video.channelUrl || previous.channelUrl || ''),
      duration: Number(video.duration || previous.duration || 0) || 0,
      thumbnail: String(video.thumbnail || video.thumbnailUrl || previous.thumbnail || `https://i.ytimg.com/vi/${id}/hqdefault.jpg`),
      watchedAt: Number(video.watchedAt) || Date.now()
    };
    ryomdYouTubeState.history = [item, ...ryomdYouTubeState.history.filter(row => row && row.id !== id)].slice(0, RYOMD_YT_HISTORY_MAX);
    ryomdYouTubeWriteJson(RYOMD_YT_HISTORY_KEY, ryomdYouTubeState.history);
  }
  const rytKnownVideo = (id) => ryomdYouTubeState.lastResults.find(v => v && v.id === id)
    || ryomdYouTubeState.history.find(v => v && v.id === id) || null;

  // ------------------------------------------------------------ native play
  function ryomdYouTubeNativePlay(idOrUrl, audioOnly) {
    const status = $('#ryomdYtNativeStatus', modalBody);
    const setStatus = (text, state) => {
      if (!status) { if (state === 'error') toast(text, 'error'); return; }
      status.textContent = text; status.dataset.state = state;
      status.classList.toggle('is-error', state === 'error');
      status.classList.toggle('is-loading', state === 'loading');
      status.setAttribute('aria-busy', String(state === 'loading'));
    };
    const buttons = modalBody.querySelectorAll(RYOMD_YT_NATIVE_BUTTONS);
    if (!window.RyomdYouTube || typeof window.RyomdYouTube.playYouTube !== 'function') {
      setStatus('Pemutar native hanya tersedia di APK Ryomd Android. Di browser, gunakan tombol "Buka di YouTube".', 'error');
      return;
    }
    const id = rytVideoId(idOrUrl);
    if (!id) { setStatus('Masukkan tautan atau ID video YouTube yang valid.', 'error'); return; }
    const known = rytKnownVideo(id);
    ryomdYouTubeSaveHistory(known ? known : { id });
    setStatus(audioOnly ? 'Menyiapkan audio…' : 'Menyiapkan pemutar…', 'loading');
    if (!status) toast(audioOnly ? 'Menyiapkan audio…' : 'Menyiapkan pemutar…');
    buttons.forEach(button => { button.disabled = true; });
    window.clearTimeout(ryomdYouTubeNativePlay.timer);
    ryomdYouTubeNativePlay.timer = window.setTimeout(() => {
      buttons.forEach(button => { button.disabled = false; });
      if (status && status.dataset.state === 'loading') setStatus('Tidak ada respons dari pemutar. Coba lagi.', 'error');
    }, 60000);
    try { window.RyomdYouTube.playYouTube(id, Boolean(audioOnly)); }
    catch (error) {
      setStatus('Gagal memanggil pemutar native: ' + String(error), 'error');
      buttons.forEach(button => { button.disabled = false; });
    }
  }

  window.ryomdNativeYouTubeStatus = function (payload) {
    if (!payload) return;
    const state = String(payload.state || '');
    const target = $('#ryomdYtNativeStatus', modalBody);
    modalBody.querySelectorAll(RYOMD_YT_NATIVE_BUTTONS).forEach(button => { button.disabled = state === 'loading'; });
    if (!target) { if (state === 'error') toast(String(payload.message || 'Pemutar gagal.'), 'error'); return; }
    target.textContent = String(payload.message || '');
    target.dataset.state = state;
    target.classList.toggle('is-error', state === 'error');
    target.classList.toggle('is-loading', state === 'loading');
    target.setAttribute('aria-busy', String(state === 'loading'));
  };

  // --------------------------------------------------------------- account
  function ryomdYouTubeRenderAccount() {
    const target = $('#ryomdYtAccountState', modalBody);
    if (!target) return;
    const profile = ryomdYouTubeState.account;
    const connectedAvatar = profile && ryomdYouTubeState.sessionReady;
    if (connectedAvatar) {
      target.innerHTML = `<span class="ryt-account-copy">${rytAvatar(profile.name, profile.picture, 'is-sm')}<span><b>${esc(profile.name || 'Akun Google terhubung')}</b><small>${esc(profile.email || 'Izin YouTube aktif di sesi ini')}</small></span></span><button type="button" class="ryt-chip-btn" data-action="yt-disconnect">Putuskan</button>`;
      return;
    }
    if (profile) {
      target.innerHTML = `<span class="ryt-account-copy">${rytAvatar(profile.name, profile.picture, 'is-sm')}<span><b>${esc(profile.name || 'Akun Google tersimpan')}</b><small>${esc(profile.email || 'Sambungkan ulang untuk memuat data akun')}</small></span></span><button type="button" class="ryt-chip-btn" data-action="yt-connect">Hubungkan ulang</button>`;
      return;
    }
    target.innerHTML = `<span class="ryt-account-copy"><span class="ryt-avatar is-sm" aria-hidden="true">?</span><span><b>Belum masuk akun</b><small>Opsional · langganan &amp; playlist akun</small></span></span><button type="button" class="ryt-chip-btn is-primary" data-action="yt-connect">Masuk Google</button>`;
  }

  window.ryomdYouTubeAccount = {
    onConnected(profile) {
      ryomdYouTubeState.account = {
        name: profile.name || 'Akun Google terhubung', email: profile.email || '',
        picture: profile.picture || '', connectedAt: Date.now()
      };
      ryomdYouTubeState.sessionReady = true;
      ryomdYouTubeWriteJson(RYOMD_YT_ACCOUNT_KEY, ryomdYouTubeState.account);
      ryomdYouTubeRenderAccount();
      toast('Akun Google berhasil disambungkan.', 'success');
      if (window.RyomdYouTubeAccount && ['subscriptions', 'playlists'].includes(ryomdYouTubeState.view)) ryomdYouTubeSetTab(ryomdYouTubeState.view);
    },
    onSubscriptions(payload) {
      ryomdYouTubeState.subscriptions = Array.isArray(payload.items) ? payload.items : [];
      if (ryomdYouTubeState.view === 'subscriptions') ryomdYouTubeRenderSubscriptions();
    },
    onPlaylists(payload) {
      ryomdYouTubeState.playlists = Array.isArray(payload.items) ? payload.items : [];
      if (ryomdYouTubeState.view === 'playlists') ryomdYouTubeRenderPlaylists();
    },
    onError(payload) {
      const message = String(payload && payload.message || 'Permintaan Google gagal.');
      toast(message, 'error');
      const content = $('#ryomdYtContent', modalBody);
      if (content && ['subscriptions', 'playlists'].includes(ryomdYouTubeState.view) && !ryomdYouTubeState.subscriptions.length && !ryomdYouTubeState.playlists.length) {
        content.insertAdjacentHTML('afterbegin', `<div class="ryt-notice is-error" role="alert"><b>Akun Google belum dapat dipakai</b><p>${esc(message)}</p></div>`);
      }
    },
    onDisconnected(result) {
      ryomdYouTubeState.account = null; ryomdYouTubeState.sessionReady = false; ryomdYouTubeState.subsRequested = false;
      ryomdYouTubeState.subscriptions = []; ryomdYouTubeState.playlists = [];
      try { localStorage.removeItem(RYOMD_YT_ACCOUNT_KEY); } catch (_) { }
      ryomdYouTubeRenderAccount();
      const failed = result && result.revoked === false;
      toast(failed ? String(result.message || 'Sesi lokal diputus; izin Google mungkin masih aktif.') : 'Akun Google diputus dan izin aplikasi dicabut.', failed ? 'error' : 'success');
      if (['subscriptions', 'playlists'].includes(ryomdYouTubeState.view)) ryomdYouTubeSetTab(ryomdYouTubeState.view);
    }
  };

  // ---------------------------------------------------------------- shell
  function ryomdYouTubeShell(content = '') {
    const nav = [['home', 'Beranda', RYT_ICONS.home], ['subscriptions', 'Langganan', RYT_ICONS.subs], ['history', 'Riwayat', RYT_ICONS.history], ['playlists', 'Koleksi', RYT_ICONS.library]];
    return `<section class="ryomd-media-shell ryt-shell" aria-label="Ryomd Tube">
      <header class="ryt-header">
        <div class="ryt-brand"><span class="ryt-logo" aria-hidden="true"><svg viewBox="0 0 24 24" width="18" height="18"><path d="M8.5 6.5v11l9-5.5z" fill="currentColor"/></svg></span><b>Ryomd<span>Tube</span></b></div>
        <a class="ryt-ext" href="https://www.youtube.com/" target="_blank" rel="noopener noreferrer" aria-label="Buka YouTube di peramban">${RYT_ICONS.external}</a>
      </header>
      <div class="ryt-account" id="ryomdYtAccountState"></div>
      <div class="ryt-searchbar" role="search">
        <input class="tool-input" id="ryomdYtSearch" type="search" enterkeyhint="search" value="${esc(ryomdYouTubeState.query)}" placeholder="Cari video atau channel" aria-label="Cari video atau channel" autocomplete="off">
        <button type="button" class="ryt-icon-btn is-search" data-action="yt-search" aria-label="Cari">${RYT_ICONS.search}</button>
        <button type="button" class="ryt-icon-btn" data-action="yt-tab" data-view="link" aria-label="Putar dari tautan">${RYT_ICONS.link}</button>
      </div>
      <div class="ryt-chips" id="ryomdYtChips" role="toolbar" aria-label="Topik"></div>
      <div class="ryomd-media-content ryt-content" id="ryomdYtContent" aria-live="polite">${content || '<div class="ryomd-media-loading"><i></i><i></i><i></i><span>Menyiapkan beranda…</span></div>'}</div>
      <nav class="ryt-nav" role="tablist" aria-label="Navigasi Ryomd Tube">
        ${nav.map(([view, label, icon]) => `<button type="button" role="tab" data-action="yt-tab" data-view="${view}" aria-selected="${ryomdYouTubeState.view === view}" class="${ryomdYouTubeState.view === view ? 'is-active' : ''}">${icon}<small>${label}</small></button>`).join('')}
      </nav>
      <input type="file" id="ryomdYtImportInput" accept=".csv,.json,text/csv,application/json,text/plain" hidden>
    </section>`;
  }

  const RYT_TOPICS = [['trending indonesia', 'Semua'], ['musik indonesia terbaru', 'Musik'], ['gaming indonesia', 'Gaming'], ['anime sub indo', 'Anime'], ['berita hari ini', 'Berita'], ['teknologi gadget', 'Teknologi'], ['sepak bola highlights', 'Olahraga'], ['resep masakan', 'Kuliner']];
  function rytRenderChips(active) {
    const box = $('#ryomdYtChips', modalBody);
    if (!box) return;
    box.innerHTML = RYT_TOPICS.map(([q, label]) => `<button type="button" data-action="yt-home-topic" data-query="${esc(q)}" class="${q === active ? 'is-active' : ''}">${label}</button>`).join('');
  }

  function ryomdYouTubeVideoCard(video, options = {}) {
    const id = rytVideoId(video && video.id);
    if (!id) return '';
    const title = esc(video.title || 'Video tanpa judul');
    const channel = esc(video.channel || 'Channel YouTube');
    const thumbnail = esc(video.thumbnail || video.thumbnailUrl || `https://i.ytimg.com/vi/${id}/hqdefault.jpg`);
    const duration = rytDuration(video.duration);
    const meta = [video.channel ? esc(video.channel) : '', rytViews(video.viewCount), options.watched ? rytAgo(video.watchedAt) : esc(video.published || '')].filter(Boolean).join(' · ');
    return `<article class="ryt-card${options.compact ? ' is-compact' : ''}">
      <div class="ryt-thumb">
        <button type="button" class="ryt-thumb-open" data-action="yt-open-video" data-id="${esc(id)}" aria-label="Buka ${title}">
          <img src="${thumbnail}" alt="" loading="lazy" referrerpolicy="no-referrer" onerror="this.style.display='none'">${duration ? `<i>${esc(duration)}</i>` : ''}
        </button>
        <button type="button" class="ryt-quick-play" data-action="yt-quick-play" data-id="${esc(id)}" aria-label="Putar langsung: ${title}">${RYT_ICONS.play}</button>
      </div>
      <div class="ryt-card-row">
        ${video.channelUrl ? `<button type="button" class="ryt-avatar-btn" data-action="yt-open-channel" data-url="${esc(video.channelUrl)}" aria-label="Buka channel ${channel}">${rytAvatar(video.channel, '')}</button>` : `<span class="ryt-avatar-btn">${rytAvatar(video.channel, '')}</span>`}
        <button type="button" class="ryt-card-meta" data-action="yt-open-video" data-id="${esc(id)}"><b>${title}</b><small>${meta}</small></button>
        ${options.removable ? `<button type="button" class="ryt-icon-btn is-ghost" data-action="yt-history-remove" data-id="${esc(id)}" aria-label="Hapus dari riwayat">×</button>` : `<button type="button" class="ryt-icon-btn is-ghost" data-action="yt-quick-audio" data-id="${esc(id)}" aria-label="Putar sebagai musik">${RYT_ICONS.music}</button>`}
      </div>
    </article>`;
  }

  const rytEmpty = (title, text, buttonHtml = '') => `<div class="ryt-empty"><b>${esc(title)}</b><p>${text}</p>${buttonHtml}</div>`;
  const rytSkeleton = (label) => `<div class="ryomd-media-loading" role="status"><i></i><i></i><i></i><span>${esc(label)}</span></div>`;

  // ------------------------------------------------------------- navigation
  function rytRenderCurrent() {
    const cur = ryomdYouTubeState.current || { kind: 'tab', view: 'home' };
    if (cur.kind === 'channel') return ryomdYouTubeOpenChannel(cur.url, { push: false });
    if (cur.kind === 'video') return ryomdYouTubeOpenVideo(cur.id, { push: false });
    return ryomdYouTubeSetTab(cur.view || 'home', { keepStack: true });
  }
  function rytGo(next) {
    if (ryomdYouTubeState.current) ryomdYouTubeState.nav.push(ryomdYouTubeState.current);
    if (ryomdYouTubeState.nav.length > 30) ryomdYouTubeState.nav.shift();
    ryomdYouTubeState.current = next;
  }
  function rytBack() {
    const prev = ryomdYouTubeState.nav.pop();
    ryomdYouTubeState.current = prev || { kind: 'tab', view: 'home' };
    return rytRenderCurrent();
  }
  const rytBackBar = (label) => `<div class="ryt-backbar"><button type="button" class="ryt-chip-btn" data-action="yt-back">${RYT_ICONS.back}<span>${esc(label || 'Kembali')}</span></button></div>`;

  async function renderYouTube() {
    Object.assign(ryomdYouTubeState, { view: 'home', nav: [], current: { kind: 'tab', view: 'home' }, autoTried: false });
    modalBody.innerHTML = ryomdYouTubeShell();
    ryomdYouTubeRenderAccount();
    const input = $('#ryomdYtSearch', modalBody);
    if (input) input.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); ryomdYouTubeSearch(); } });
    const picker = $('#ryomdYtImportInput', modalBody);
    if (picker) picker.addEventListener('change', () => rytHandleImport(picker));
    await ryomdYouTubeRenderHome();
  }

  async function ryomdYouTubeRenderHome(query) {
    const term = String(query || ryomdYouTubeState.query || RYT_TOPICS[0][0]).trim();
    rytRenderChips(RYT_TOPICS.some(([q]) => q === term) ? term : '');
    const searchField = $('#ryomdYtSearch', modalBody);
    if (searchField && !searchField.value.trim() && !RYT_TOPICS.some(([q]) => q === term)) searchField.value = term;
    await ryomdYouTubeSearch(term, true);
  }

  async function ryomdYouTubeSearch(forcedQuery, inHome, options = {}) {
    ryomdYouTubeState.view = 'home';
    ryomdYouTubeState.current = { kind: 'tab', view: 'home' };
    const chipBox = $('#ryomdYtChips', modalBody); if (chipBox) chipBox.hidden = false;
    modalBody.querySelectorAll('.ryt-nav [data-view]').forEach(tab => { const active = tab.dataset.view === 'home'; tab.classList.toggle('is-active', active); tab.setAttribute('aria-selected', String(active)); });
    const input = $('#ryomdYtSearch', modalBody);
    const query = String(forcedQuery !== undefined && typeof forcedQuery === 'string' ? forcedQuery : (input ? input.value : ryomdYouTubeState.query)).trim();
    if (query.length < 2) { toast('Masukkan kata pencarian minimal 2 karakter.', 'error'); return; }
    // tempel tautan YouTube di kolom cari -> buka langsung
    const pastedId = /^https?:\/\//i.test(query) ? rytVideoId(query) : '';
    if (pastedId) { await ryomdYouTubeOpenVideo(pastedId); return; }
    ryomdYouTubeState.query = query;
    if (input && !inHome) input.value = query;
    const isTopic = RYT_TOPICS.some(([q]) => q === query);
    rytRenderChips(isTopic ? query : '');
    const content = $('#ryomdYtContent', modalBody);
    if (!content) return;
    const offset = Math.max(0, Number(options.offset) || 0);
    if (options.forceRefresh && offset === 0) ryomdYouTubeState.searchCache.delete(query);
    const cached = offset === 0 && !options.forceRefresh ? ryomdYouTubeState.searchCache.get(query) : null;
    let data;
    if (cached && Date.now() - cached.at < RYOMD_YT_FEED_TTL) data = cached.data;
    else {
      content.setAttribute('aria-busy', 'true');
      if (offset === 0) content.innerHTML = rytSkeleton(isTopic ? 'Memuat beranda…' : 'Mencari video dan channel…');
      try {
        data = await rytData('search', query, 40, offset);
        if (offset === 0) ryomdYouTubeState.searchCache.set(query, { at: Date.now(), data });
      } catch (error) {
        content.setAttribute('aria-busy', 'false');
        const message = esc(error.message || 'Sumber data YouTube tidak merespons.');
        if (offset > 0) { ryomdYouTubeState.homeLoadingMore = false; const more = content.querySelector('[data-action="yt-load-more"]'); if (more) { more.disabled = false; more.textContent = 'Coba muat lagi'; } content.insertAdjacentHTML('beforeend', `<div class="ryt-notice is-error">${message}</div>`); }
        else content.innerHTML = rytEmpty('Pencarian belum tersedia', `${message}<br>Periksa internet. Pemutaran dari tab Tautan tetap bisa dipakai.`, `<button type="button" class="ryt-chip-btn is-primary" data-action="yt-refresh">Coba lagi</button>`);
        return;
      }
    }
    if (ryomdYouTubeState.view !== 'home' || !$('#ryomdYtContent', modalBody)) return;
    const newVideos = Array.isArray(data.videos) ? data.videos : [];
    if (offset > 0 && options.append) {
      const existing = new Set(ryomdYouTubeState.lastResults.map(v => rytVideoId(v.id)));
      ryomdYouTubeState.lastResults = ryomdYouTubeState.lastResults.concat(newVideos.filter(v => !existing.has(rytVideoId(v.id))));
    } else { ryomdYouTubeState.lastResults = newVideos; }
    ryomdYouTubeState.homeHasMore = data.hasMore === true;
    ryomdYouTubeState.homeLoadingMore = false;
    const channels = Array.isArray(data.channels) ? data.channels : [];
    const channelHtml = channels.length && !isTopic ? `<section class="ryt-shelf"><h4>Channel</h4><div class="ryt-strip">${channels.map(ch => `<button type="button" class="ryt-strip-item" data-action="yt-open-channel" data-url="${esc(ch.url)}">${rytAvatar(ch.name, ch.thumbnail, 'is-lg')}<b>${esc(ch.name)}</b></button>`).join('')}</div></section>` : '';
    const header = `<section class="ryt-section-head ryt-home-head"><div><h3>${isTopic ? 'Rekomendasi video' : `Hasil untuk “${esc(query)}”`}</h3><p>${ryomdYouTubeState.lastResults.length} video ditampilkan</p></div><button type="button" class="ryt-chip-btn" data-action="yt-refresh" aria-label="Segarkan beranda">↻ Segarkan</button></section>`;
    const videoHtml = ryomdYouTubeState.lastResults.length
      ? `<div class="ryt-feed">${ryomdYouTubeState.lastResults.map(v => ryomdYouTubeVideoCard(v)).join('')}</div>`
      : rytEmpty('Belum ada hasil video', 'Coba kata kunci lain.');
    const moreHtml = ryomdYouTubeState.homeHasMore ? `<div class="ryt-load-more-wrap"><button type="button" class="ryt-chip-btn is-primary" data-action="yt-load-more">Muat video lainnya</button></div>` : (ryomdYouTubeState.lastResults.length ? `<p class="ryt-end-note">Semua hasil yang tersedia sudah ditampilkan.</p>` : '');
    const pageWarning = data.paginationWarning ? `<div class="ryt-notice">Sebagian halaman belum berhasil dimuat: ${esc(data.paginationWarning)}. Tekan Segarkan untuk mencoba lagi.</div>` : '';
    content.setAttribute('aria-busy', 'false');
    content.innerHTML = `${header}${pageWarning}${channelHtml}${videoHtml}${moreHtml}`;
  }

  async function ryomdYouTubeSetTab(view, options = {}) {
    const allowed = ['home', 'subscriptions', 'history', 'playlists', 'link'];
    ryomdYouTubeState.view = allowed.includes(view) ? view : 'home';
    if (!options.keepStack) ryomdYouTubeState.nav = [];
    ryomdYouTubeState.current = { kind: 'tab', view: ryomdYouTubeState.view };
    modalBody.querySelectorAll('.ryt-nav [data-view]').forEach(tab => {
      const active = tab.dataset.view === ryomdYouTubeState.view;
      tab.classList.toggle('is-active', active); tab.setAttribute('aria-selected', String(active));
    });
    const chips = $('#ryomdYtChips', modalBody);
    if (chips) chips.hidden = ryomdYouTubeState.view !== 'home';
    const content = $('#ryomdYtContent', modalBody);
    if (!content) return;
    rytScrollTop();
    if (ryomdYouTubeState.view === 'home') { await ryomdYouTubeRenderHome(); return; }
    if (ryomdYouTubeState.view === 'subscriptions') { await ryomdYouTubeRenderSubscriptions(); return; }
    if (ryomdYouTubeState.view === 'history') { ryomdYouTubeRenderHistory(); return; }
    if (ryomdYouTubeState.view === 'playlists') { ryomdYouTubeRenderPlaylists(); return; }
    content.innerHTML = `<section class="ryt-panel"><h3>Putar dari tautan</h3><p>Tempel URL atau ID video YouTube (youtube.com, youtu.be, shorts, music.youtube.com). Tidak memerlukan pencarian atau login.</p>
      <input class="tool-input" id="ryomdYtLinkInput" type="url" inputmode="url" autocomplete="off" autocapitalize="off" spellcheck="false" placeholder="https://youtu.be/… atau ID 11 karakter" aria-label="Tautan video YouTube">
      <div class="ryomd-yt-native-actions"><button type="button" data-action="yt-link-play">${RYT_ICONS.play}<span>Putar video</span></button><button type="button" data-action="yt-link-audio">${RYT_ICONS.music}<span>Putar musik</span></button></div>
      <div class="ryomd-yt-native-status" id="ryomdYtNativeStatus" role="status" aria-live="polite" aria-busy="false">Siap memutar.</div></section>`;
    const field = $('#ryomdYtLinkInput', modalBody);
    if (field) field.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); ryomdYouTubeNativePlay(field.value, false); } });
  }

  // ---------------------------------------------------------- subscriptions
  function rytAllChannels() {
    const rows = ryomdYouTubeState.localSubs.map(row => ({ url: row.url, title: row.title, thumbnail: row.thumbnail, local: true }));
    for (const item of ryomdYouTubeState.subscriptions || []) {
      const sn = item.snippet || {};
      const channelId = sn.resourceId && sn.resourceId.channelId;
      if (!channelId) continue;
      const url = `https://www.youtube.com/channel/${channelId}`;
      if (rows.some(row => rytChannelKey(row.url) === rytChannelKey(url))) continue;
      const image = sn.thumbnails && (sn.thumbnails.medium || sn.thumbnails.default || sn.thumbnails.high);
      rows.push({ url, title: sn.title || 'Channel YouTube', thumbnail: image && image.url || '', local: false });
    }
    return rows;
  }

  async function rytLoadFeed(channels) {
    const cache = ryomdYouTubeState.feedCache;
    const signature = channels.map(c => rytChannelKey(c.url)).join('|');
    if (cache && cache.signature === signature && Date.now() - cache.at < RYOMD_YT_FEED_TTL) return cache.videos;
    const picked = channels.slice(0, 12);
    const results = [];
    let cursor = 0;
    const worker = async () => {
      while (cursor < picked.length) {
        const channel = picked[cursor++];
        try { const data = await rytData('channel', channel.url, 8); results.push((data.videos || []).slice(0, 6).map(v => ({ ...v, channel: v.channel || channel.title, channelUrl: v.channelUrl || channel.url }))); }
        catch (_) { results.push([]); }
      }
    };
    await Promise.all([worker(), worker(), worker()]);
    const merged = []; const seen = new Set();
    for (let i = 0; i < 6; i++) for (const list of results) { const v = list[i]; if (v && v.id && !seen.has(v.id)) { seen.add(v.id); merged.push(v); } }
    ryomdYouTubeState.feedCache = { signature, at: Date.now(), videos: merged };
    return merged;
  }

  async function ryomdYouTubeRenderSubscriptions() {
    const content = $('#ryomdYtContent', modalBody);
    if (!content) return;
    if (!ryomdYouTubeState.sessionReady && ryomdYouTubeState.account && !ryomdYouTubeState.autoTried && window.RyomdYouTubeAccount) {
      ryomdYouTubeState.autoTried = true; // pulihkan sesi Google yang pernah disambungkan
      try { window.RyomdYouTubeAccount.connect(); } catch (_) { }
    }
    if (ryomdYouTubeState.sessionReady && window.RyomdYouTubeAccount && !ryomdYouTubeState.subscriptions.length && !ryomdYouTubeState.subsRequested) {
      ryomdYouTubeState.subsRequested = true; window.RyomdYouTubeAccount.loadSubscriptions();
    }
    const channels = rytAllChannels();
    const actions = `<div class="ryt-actions">
      <button type="button" class="ryt-chip-btn" data-action="yt-import" data-kind="subs">Impor langganan (.csv)</button>
      ${ryomdYouTubeState.sessionReady ? '<button type="button" class="ryt-chip-btn" data-action="yt-subscriptions-refresh">Muat ulang akun</button>' : '<button type="button" class="ryt-chip-btn" data-action="yt-connect">Masuk Google</button>'}
    </div>${rytTakeoutHelp()}`;
    if (!channels.length) {
      content.innerHTML = rytEmpty('Belum ada langganan', 'Buka sebuah channel lalu ketuk <b>Berlangganan</b>, impor <code>subscriptions.csv</code> dari Google Takeout, atau masuk dengan akun Google untuk mengambil daftar langganan Anda.') + actions;
      return;
    }
    const strip = `<section class="ryt-shelf"><div class="ryt-strip">${channels.slice(0, 60).map(ch => `<button type="button" class="ryt-strip-item" data-action="yt-open-channel" data-url="${esc(ch.url)}">${rytAvatar(ch.title, ch.thumbnail, 'is-lg')}<b>${esc(ch.title)}</b></button>`).join('')}</div></section>`;
    content.innerHTML = `${strip}${actions}<div id="ryomdYtSubFeed" class="ryt-feed">${rytSkeleton('Memuat video terbaru dari channel langganan…')}</div>`;
    const videos = await rytLoadFeed(channels);
    const feed = $('#ryomdYtSubFeed', modalBody);
    if (!feed || ryomdYouTubeState.view !== 'subscriptions') return;
    feed.innerHTML = videos.length ? videos.map(v => ryomdYouTubeVideoCard(v)).join('') : rytEmpty('Video terbaru belum dapat dimuat', 'Periksa internet lalu buka tab ini lagi.');
  }

  const rytTakeoutHelp = () => `<details class="ryt-help"><summary>Cara mengambil data dari akun Google (Takeout)</summary><ol>
    <li>Buka <a href="https://takeout.google.com/" target="_blank" rel="noopener noreferrer">takeout.google.com</a>, pilih hanya <b>YouTube dan YouTube Music</b>.</li>
    <li>Di “Semua data YouTube disertakan” pilih <b>langganan</b> dan <b>riwayat</b>; untuk riwayat ubah format ke <b>JSON</b>.</li>
    <li>Unduh dan ekstrak ZIP, lalu impor <code>subscriptions.csv</code> (Langganan) atau <code>watch-history.json</code> (Riwayat) di sini.</li></ol></details>`;

  // ---------------------------------------------------------------- history
  function ryomdYouTubeRenderHistory() {
    const content = $('#ryomdYtContent', modalBody);
    if (!content) return;
    const rows = ryomdYouTubeState.history;
    const groups = [['Hari ini', 0], ['Kemarin', 1], ['Lebih lama', Infinity]];
    const startOfDay = new Date(); startOfDay.setHours(0, 0, 0, 0);
    const dayIndex = (ts) => Math.max(0, Math.floor((startOfDay.getTime() - new Date(ts).setHours(0, 0, 0, 0)) / 86400000));
    const buckets = groups.map(() => []);
    rows.forEach(row => { const d = dayIndex(row.watchedAt || 0); buckets[d <= 0 ? 0 : d === 1 ? 1 : 2].push(row); });
    const head = `<section class="ryt-section-head"><div><h3>Riwayat tontonan</h3><p>${rows.length} video di perangkat ini.</p></div><div class="ryt-actions"><button type="button" class="ryt-chip-btn" data-action="yt-import" data-kind="history">Impor riwayat (.json)</button>${rows.length ? '<button type="button" class="ryt-chip-btn is-danger" data-action="yt-history-clear">Hapus semua</button>' : ''}</div></section>`;
    if (!rows.length) { content.innerHTML = head + rytEmpty('Riwayat masih kosong', 'Video yang Anda putar atau buka di sini akan muncul. Riwayat akun Google dapat diimpor dari file Takeout.') + rytTakeoutHelp(); return; }
    content.innerHTML = head + groups.map(([label], i) => buckets[i].length ? `<section class="ryt-shelf"><h4>${label}</h4><div class="ryt-feed">${buckets[i].slice(0, 80).map(item => ryomdYouTubeVideoCard(item, { watched: true, removable: true })).join('')}</div></section>` : '').join('');
  }

  // ------------------------------------------------------------- collection
  function ryomdYouTubeRenderPlaylists() {
    const content = $('#ryomdYtContent', modalBody);
    if (!content) return;
    if (!ryomdYouTubeState.sessionReady) {
      if (ryomdYouTubeState.account && !ryomdYouTubeState.autoTried && window.RyomdYouTubeAccount) { ryomdYouTubeState.autoTried = true; try { window.RyomdYouTubeAccount.connect(); } catch (_) { } }
      content.innerHTML = rytEmpty('Playlist akun Google', 'Masuk dengan akun Google untuk melihat playlist YouTube Anda. Login memakai layar resmi Google — password tidak pernah masuk ke aplikasi ini.', '<button type="button" class="ryt-chip-btn is-primary" data-action="yt-connect">Masuk dengan Google</button>');
      return;
    }
    if (!ryomdYouTubeState.playlists.length) {
      content.innerHTML = rytSkeleton('Memuat playlist akun…');
      if (window.RyomdYouTubeAccount) window.RyomdYouTubeAccount.loadPlaylists();
      return;
    }
    const rows = ryomdYouTubeState.playlists;
    content.innerHTML = `<section class="ryt-section-head"><div><h3>Playlist akun</h3><p>${rows.length} playlist.</p></div><button type="button" class="ryt-chip-btn" data-action="yt-playlists-refresh">Muat ulang</button></section><div class="ryt-list">${rows.map(item => {
      const sn = item.snippet || {}; const id = item.id || '';
      const image = sn.thumbnails && (sn.thumbnails.medium || sn.thumbnails.default || sn.thumbnails.high);
      const count = item.contentDetails && item.contentDetails.itemCount;
      return `<a class="ryt-list-item" href="${esc(`https://www.youtube.com/playlist?list=${encodeURIComponent(id)}`)}" target="_blank" rel="noopener noreferrer">${image && image.url ? `<img src="${esc(image.url)}" alt="" loading="lazy" referrerpolicy="no-referrer">` : '<span class="ryt-avatar is-lg">▶</span>'}<span><b>${esc(sn.title || 'Playlist')}</b><small>${count != null ? esc(count) + ' video · ' : ''}Buka di YouTube</small></span></a>`;
    }).join('')}</div>`;
  }

  // --------------------------------------------------------------- channel
  async function ryomdYouTubeOpenChannel(channelUrl, options = {}) {
    if (!channelUrl) return;
    if (options.push !== false) rytGo({ kind: 'channel', url: channelUrl });
    const content = $('#ryomdYtContent', modalBody);
    if (!content) return;
    const chips = $('#ryomdYtChips', modalBody); if (chips) chips.hidden = true;
    rytScrollTop();
    content.innerHTML = rytBackBar() + rytSkeleton('Memuat channel…');
    try {
      const data = await rytData('channel', channelUrl, 30);
      if (!ryomdYouTubeState.current || ryomdYouTubeState.current.kind !== 'channel') return;
      const channel = data.channel || {};
      const url = channel.url || channelUrl;
      ryomdYouTubeState.currentChannelUrl = url;
      ryomdYouTubeState.currentChannel = { url, title: channel.title || 'Channel YouTube', thumbnail: channel.thumbnail || '' };
      const videos = (Array.isArray(data.videos) ? data.videos : []).map(v => ({ ...v, channel: v.channel || channel.title, channelUrl: v.channelUrl || url }));
      ryomdYouTubeState.lastResults = videos.length ? videos : ryomdYouTubeState.lastResults;
      const subscribed = rytIsSubscribed(url) || rytIsSubscribed(channelUrl);
      const subs = Number(channel.subscriberCount) > 0 ? `${rytCompact.format(Number(channel.subscriberCount))} subscriber · ` : '';
      content.innerHTML = `${rytBackBar()}<section class="ryt-channel-head">${rytAvatar(channel.title, channel.thumbnail, 'is-xl')}<div><h3>${esc(channel.title || 'Channel YouTube')}</h3><small>${subs}${videos.length} video terbaru</small></div>
        <button type="button" class="ryt-sub-btn${subscribed ? ' is-on' : ''}" data-action="yt-subscribe" aria-pressed="${subscribed}">${subscribed ? RYT_ICONS.check + '<span>Berlangganan</span>' : '<span>Berlangganan</span>'}</button></section>
        ${channel.description ? `<details class="ryt-help"><summary>Tentang channel</summary><pre class="ryomd-yt-description">${esc(channel.description)}</pre></details>` : ''}
        <div class="ryt-feed">${videos.length ? videos.map(v => ryomdYouTubeVideoCard(v)).join('') : rytEmpty('Video belum tersedia', 'Channel ini belum menampilkan video.')}</div>`;
    } catch (error) {
      content.innerHTML = rytBackBar() + rytEmpty('Channel tidak dapat dimuat', esc(error.message || 'Gagal membaca channel.'));
    }
  }

  // ----------------------------------------------------------------- video
  async function ryomdYouTubeOpenVideo(id, options = {}) {
    const videoId = rytVideoId(id);
    if (!videoId) return;
    if (options.push !== false) rytGo({ kind: 'video', id: videoId });
    ryomdYouTubeState.currentVideoId = videoId;
    const content = $('#ryomdYtContent', modalBody);
    if (!content) return;
    const chips = $('#ryomdYtChips', modalBody); if (chips) chips.hidden = true;
    rytScrollTop();
    content.innerHTML = rytBackBar() + rytSkeleton('Memuat video…');
    let video = null; let degraded = false;
    try { const data = await rytData('video', videoId); video = data.video || null; } catch (_) { degraded = true; }
    if (ryomdYouTubeState.currentVideoId !== videoId || !ryomdYouTubeState.current || ryomdYouTubeState.current.kind !== 'video') return;
    const known = rytKnownVideo(videoId) || {};
    video = { ...known, ...(video || {}) };
    video.id = videoId;
    ryomdYouTubeSaveHistory(video);
    const title = video.title || 'Video YouTube';
    const thumbnail = video.thumbnail || video.thumbnailUrl || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
    const channelUrl = video.channelUrl || '';
    const subscribed = channelUrl && rytIsSubscribed(channelUrl);
    const related = ryomdYouTubeState.lastResults.filter(v => v && v.id !== videoId).slice(0, 12);
    const meta = [rytViews(video.viewCount), video.published ? esc(video.published) : ''].filter(Boolean).join(' · ');
    content.innerHTML = `${rytBackBar()}<article class="ryt-watch">
      <div class="ryt-player-preview"><img src="${esc(thumbnail)}" alt="" referrerpolicy="no-referrer" onerror="this.style.display='none'">
        <button type="button" class="ryt-big-play" data-action="yt-native-play" data-id="${esc(videoId)}" aria-label="Putar video layar penuh">${RYT_ICONS.play}</button></div>
      <h2>${esc(title)}</h2>${meta ? `<p class="ryt-meta">${meta}</p>` : ''}
      ${degraded ? '<div class="ryt-notice">Info lengkap belum dapat dimuat, tetapi video tetap bisa diputar.</div>' : ''}
      <div class="ryomd-yt-native-actions"><button type="button" data-action="yt-native-play" data-id="${esc(videoId)}">${RYT_ICONS.play}<span>Putar video</span></button><button type="button" data-action="yt-native-audio" data-id="${esc(videoId)}">${RYT_ICONS.music}<span>Putar musik</span></button></div>
      <div class="ryomd-yt-native-status" id="ryomdYtNativeStatus" role="status" aria-live="polite" aria-busy="false">Video dibuka lewat pemutar native Ryomd (Media3). Pilihan resolusi tersedia dari tombol Kualitas di bagian atas pemutar layar penuh.</div>
      ${video.channel || channelUrl ? `<div class="ryt-channel-row">${channelUrl ? `<button type="button" class="ryt-avatar-btn" data-action="yt-open-channel" data-url="${esc(channelUrl)}" aria-label="Buka channel">${rytAvatar(video.channel, '')}</button>` : rytAvatar(video.channel, '')}<span class="ryt-channel-name"><b>${esc(video.channel || 'Channel YouTube')}</b></span>${channelUrl ? `<button type="button" class="ryt-sub-btn${subscribed ? ' is-on' : ''}" data-action="yt-subscribe-video" data-url="${esc(channelUrl)}" data-title="${esc(video.channel || '')}" aria-pressed="${Boolean(subscribed)}">${subscribed ? RYT_ICONS.check + '<span>Berlangganan</span>' : '<span>Berlangganan</span>'}</button>` : ''}</div>` : ''}
      ${video.description ? `<details class="ryt-help"><summary>Deskripsi</summary><pre class="ryomd-yt-description">${esc(video.description)}</pre></details>` : ''}
      <a class="ryt-open-external" href="https://www.youtube.com/watch?v=${encodeURIComponent(videoId)}" target="_blank" rel="noopener noreferrer">${RYT_ICONS.external}<span>Buka di YouTube</span></a>
      <section class="ryomd-yt-comments" id="ryomdYtComments"><h4>Komentar</h4><div class="ryt-notice">Memuat komentar…</div></section>
      ${related.length ? `<section class="ryt-shelf"><h4>Berikutnya</h4><div class="ryt-feed">${related.map(v => ryomdYouTubeVideoCard(v)).join('')}</div></section>` : ''}
    </article>`;
    ryomdYouTubeLoadComments(videoId);
  }

  async function ryomdYouTubeLoadComments(videoId) {
    const target = $('#ryomdYtComments', modalBody);
    if (!target) return;
    try {
      const data = await api(`/api/youtube/comments?id=${encodeURIComponent(videoId)}`);
      if (ryomdYouTubeState.currentVideoId !== videoId) return;
      const comments = Array.isArray(data.comments) ? data.comments : [];
      if (!comments.length) { target.innerHTML = `<h4>Komentar</h4><div class="ryt-notice">${esc(data.message || 'Komentar publik tidak tersedia untuk video ini.')}</div>`; return; }
      target.innerHTML = `<h4>Komentar <small>${comments.length}</small></h4><div class="ryomd-yt-comment-list">${comments.map(comment => `<article><b>${esc(comment.author || 'Pengguna YouTube')}</b><small>${esc(comment.time || '')}${comment.likes ? ' · ' + Number(comment.likes).toLocaleString('id-ID') + ' suka' : ''}</small><p>${esc(comment.text || '')}</p></article>`).join('')}</div>`;
    } catch (error) {
      if (ryomdYouTubeState.currentVideoId !== videoId) return;
      target.innerHTML = '<h4>Komentar</h4><div class="ryt-notice">Komentar membutuhkan backend Ryomd di Termux yang sedang berjalan.</div>';
    }
  }

  // ---------------------------------------------------------------- import
  function rytParseCsv(text) {
    const rows = []; let row = [], field = '', quoted = false;
    for (let i = 0; i < text.length; i++) {
      const ch = text[i];
      if (quoted) { if (ch === '"') { if (text[i + 1] === '"') { field += '"'; i++; } else quoted = false; } else field += ch; }
      else if (ch === '"') quoted = true;
      else if (ch === ',') { row.push(field); field = ''; }
      else if (ch === '\n') { row.push(field); rows.push(row); row = []; field = ''; }
      else if (ch !== '\r') field += ch;
    }
    if (field || row.length) { row.push(field); rows.push(row); }
    return rows;
  }
  function rytImportSubsCsv(text) {
    const rows = rytParseCsv(text.replace(/^\uFEFF/, ''));
    if (rows.length < 2) return 0;
    const head = rows[0].map(c => c.trim().toLowerCase());
    let urlIdx = head.findIndex(c => c.includes('url'));
    let idIdx = head.findIndex(c => c.includes('id'));
    const titleIdx = head.findIndex(c => c.includes('title') || c.includes('judul') || c.includes('name'));
    const parsed = rows.slice(1).map(cols => {
      let url = urlIdx >= 0 ? (cols[urlIdx] || '').trim() : '';
      const cid = idIdx >= 0 ? (cols[idIdx] || '').trim() : '';
      if (!url && /^UC[\w-]{20,}$/.test(cid)) url = `https://www.youtube.com/channel/${cid}`;
      url = url.replace(/^http:\/\//i, 'https://');
      return { url, title: titleIdx >= 0 ? (cols[titleIdx] || '').trim() : '' };
    }).filter(row => /youtube\.com\//i.test(row.url));
    return rytMergeSubs(parsed);
  }
  function rytImportHistoryJson(text) {
    let data; try { data = JSON.parse(text); } catch (_) { throw new Error('File JSON tidak valid.'); }
    if (!Array.isArray(data)) throw new Error('Format riwayat tidak dikenali. Pilih watch-history.json dari Takeout.');
    let added = 0; const have = new Set(ryomdYouTubeState.history.map(row => row.id));
    const items = [];
    for (const entry of data) {
      const id = rytVideoId(entry && entry.titleUrl);
      if (!id) continue;
      const sub = Array.isArray(entry.subtitles) && entry.subtitles[0] || {};
      items.push({ id, title: String(entry.title || '').replace(/^(Watched|Telah menonton|Anda menonton)\s+/i, '') || 'Video YouTube', channel: sub.name || '', channelUrl: sub.url || '', watchedAt: Date.parse(entry.time) || Date.now() });
    }
    const merged = [...ryomdYouTubeState.history];
    for (const item of items) { if (have.has(item.id)) continue; have.add(item.id); merged.push({ ...item, thumbnail: `https://i.ytimg.com/vi/${item.id}/hqdefault.jpg`, duration: 0 }); added++; }
    merged.sort((a, b) => (b.watchedAt || 0) - (a.watchedAt || 0));
    ryomdYouTubeState.history = merged.slice(0, RYOMD_YT_HISTORY_MAX);
    ryomdYouTubeWriteJson(RYOMD_YT_HISTORY_KEY, ryomdYouTubeState.history);
    return added;
  }
  async function rytHandleImport(input) {
    const file = input.files && input.files[0];
    const kind = input.dataset.kind || '';
    input.value = '';
    if (!file) return;
    try {
      if (file.size > 25 * 1024 * 1024) throw new Error('File terlalu besar (maks 25 MB).');
      const text = await file.text();
      const looksJson = /^\s*[\[{]/.test(text);
      if (looksJson || /\.json$/i.test(file.name)) {
        const added = rytImportHistoryJson(text);
        toast(`${added} video ditambahkan ke riwayat.`, 'success');
        if (ryomdYouTubeState.view === 'history') ryomdYouTubeRenderHistory();
      } else if (/<html|<!doctype/i.test(text.slice(0, 400))) {
        throw new Error('File HTML belum didukung. Ekspor riwayat dari Takeout dalam format JSON.');
      } else {
        const added = rytImportSubsCsv(text);
        toast(`${added} channel ditambahkan ke langganan.`, added ? 'success' : '');
        if (ryomdYouTubeState.view === 'subscriptions') ryomdYouTubeRenderSubscriptions();
      }
    } catch (error) { toast(String(error.message || error), 'error'); }
  }
// END RYOMD_MEDIA_CENTER_GOOGLE_ACCOUNT_V1

  async function renderWeather() {
    modalBody.innerHTML = `${modalIntro('Pilih lokasi berdasarkan nama wilayah. Kode ADM4 akan diisi otomatis untuk mengambil prakiraan dari BMKG.')}
      <section class="bmkg-studio" aria-label="Pemilih prakiraan cuaca BMKG">
        <div class="bmkg-weather-hero">
          <div class="bmkg-weather-hero-copy"><span class="bmkg-weather-eyebrow">WEATHER DESK · INDONESIA</span><h3>Cuaca di wilayahmu.</h3><p>Pilih sampai tingkat desa atau kelurahan. Tidak perlu mencari kode ADM4 secara manual.</p></div>
          <div class="bmkg-weather-symbol" aria-hidden="true">☁</div>
        </div>
        <div class="bmkg-region-card">
          <div class="bmkg-region-card-head"><div><span class="bmkg-section-kicker">PILIH LOKASI</span><h4>Di mana kamu ingin melihat prakiraan?</h4></div><span class="bmkg-region-step">01 — 04</span></div>
          <div class="bmkg-region-grid">
            <label class="bmkg-region-field"><span><i>01</i> Provinsi</span><select class="tool-select bmkg-region-select" id="bmkgProvince"><option value="">Memuat daftar provinsi…</option></select></label>
            <label class="bmkg-region-field"><span><i>02</i> Kabupaten / Kota</span><select class="tool-select bmkg-region-select" id="bmkgRegency" disabled><option value="">Pilih kabupaten/kota</option></select></label>
            <label class="bmkg-region-field"><span><i>03</i> Kecamatan</span><select class="tool-select bmkg-region-select" id="bmkgDistrict" disabled><option value="">Pilih kecamatan</option></select></label>
            <label class="bmkg-region-field"><span><i>04</i> Desa / Kelurahan</span><select class="tool-select bmkg-region-select" id="bmkgVillage" disabled><option value="">Pilih desa/kelurahan</option></select></label>
          </div>
          <div class="bmkg-region-summary" id="bmkgRegionSummary"><b>Belum ada lokasi dipilih</b><span>Pilih wilayah secara berurutan dari provinsi sampai desa/kelurahan.</span></div>
          <input type="hidden" id="bmkgCode" value="">
          <div class="bmkg-region-status" id="bmkgRegionStatus" role="status" aria-live="polite">Menyiapkan daftar wilayah…</div>
          <button type="button" class="tool-btn bmkg-retry" id="bmkgRetryRegions" hidden>Muat ulang daftar wilayah</button>
          <div class="bmkg-weather-actions">${button('Ambil prakiraan', 'load-weather', 'primary')}</div>
        </div>
        <div class="bmkg-forecast-card">
          <div class="bmkg-forecast-card-head"><div><span class="bmkg-section-kicker">FORECAST</span><h4>Prakiraan cuaca</h4></div><span class="bmkg-forecast-live"><i></i> DATA BMKG</span></div>
          <div class="bmkg-result" id="bmkgOut" aria-live="polite"><div class="bmkg-empty-state"><span class="bmkg-empty-icon">☁</span><b>Prakiraan cuaca siap ditampilkan</b><p>Pilih desa atau kelurahan, lalu tekan Ambil prakiraan.</p></div></div>
        </div>
        <div class="bmkg-attribution"><b>Sumber prakiraan: BMKG</b><span>Prakiraan ini bukan pengamatan langsung. Atribusi BMKG ditampilkan sesuai ketentuan sumber data. Daftar nama dan kode wilayah menggunakan kompilasi kode Kemendagri 2022.</span></div>
      </section>`;
    const retry = $('#bmkgRetryRegions', modalBody);
    if (retry) retry.addEventListener('click', () => { retry.hidden = true; initBmkgRegionPicker(); });
    const loadButton = $('[data-action="load-weather"]', modalBody);
    if (loadButton) loadButton.disabled = true;
    await initBmkgRegionPicker();
  }

  async function renderCrypto() {
    modalBody.innerHTML = modalIntro('Pantau harga BTC, ETH, SOL, dan TON dalam IDR atau USD.') +
      '<div id="ryomd-crypto-market" aria-live="polite"></div>';
    if (!document.getElementById('ryomdCryptoMarketStyles')) {
      const style = document.createElement('link');
      style.id = 'ryomdCryptoMarketStyles';
      style.rel = 'stylesheet';
      style.href = '/modules/crypto-market/widget.css?v=1';
      document.head.appendChild(style);
    }
    const script = document.createElement('script');
    script.src = '/modules/crypto-market/widget.js?v=1';
    script.async = true;
    script.onerror = () => {
      const root = document.getElementById('ryomd-crypto-market');
      if (root) root.textContent = 'Widget Crypto Market gagal dimuat. Periksa server lokal.';
    };
    modalBody.appendChild(script);
  }
  async function renderIpIntel() {
    modalBody.innerHTML = `${modalIntro('Informasi IP publik diperoleh dari penyedia data jaringan eksternal. IP internal perangkat tidak ditampilkan.')}
      <div class="tool-actions">${button('Periksa jaringan', 'load-ip', 'primary')}</div><pre class="tool-pre" id="ipOut" style="margin-top:11px">Mengambil informasi jaringan…</pre>`;
    await loadIpInfo();
  }
  async function loadIpInfo() {
    $('#ipOut',modalBody).textContent = 'Mengambil informasi…';
    try { const data = await api('/api/public/ip'); $('#ipOut',modalBody).textContent = `IP Publik : ${data.ip || '—'}\nISP / ASN : ${data.org || '—'} (${data.asn || '—'})\nKota      : ${[data.city,data.region,data.country_name].filter(Boolean).join(', ')}\nZona Waktu: ${data.timezone || '—'}`; }
    catch(error) { $('#ipOut',modalBody).textContent = error.message; }
  }

  const MORSE = { A:'.-',B:'-...',C:'-.-.',D:'-..',E:'.',F:'..-.',G:'--.',H:'....',I:'..',J:'.---',K:'-.-',L:'.-..',M:'--',N:'-.',O:'---',P:'.--.',Q:'--.-',R:'.-.',S:'...',T:'-',U:'..-',V:'...-',W:'.--',X:'-..-',Y:'-.--',Z:'--..',0:'-----',1:'.----',2:'..---',3:'...--',4:'....-',5:'.....',6:'-....',7:'--...',8:'---..',9:'----.',' ':'/' };
  function renderMorse() {
    modalBody.innerHTML = `<div class="tool-field"><label for="morseInp">Teks yang akan dikonversi</label><input class="tool-input" id="morseInp" value="RYOMD OVERDRIVE"></div><pre class="tool-pre" id="morseOut"></pre><div class="tool-actions" style="margin-top:10px">${button('Putar bunyi Morse', 'play-morse', 'primary')}${button('Salin kode', 'copy-morse')}</div>`;
    $('#morseInp',modalBody).addEventListener('input', updateMorse); updateMorse();
  }
  function updateMorse() { const text = $('#morseInp',modalBody)?.value.toUpperCase() || ''; $('#morseOut',modalBody).textContent = [...text].map(char => MORSE[char] || '').filter(Boolean).join('   '); }

  function renderTextEncoder() {
    modalBody.innerHTML = `<div class="tool-field"><label for="encInp">Input teks</label><textarea class="tool-textarea" id="encInp" rows="5">Ryomd Overdrive — Local Workspace</textarea></div><div class="tool-actions">${button('Encode Base64', 'encode:b64e', 'primary')}${button('Decode Base64', 'encode:b64d')}${button('Text → Hex', 'encode:hex')}${button('URL Encode', 'encode:url')}${button('URL Decode', 'encode:urldecode')}</div><pre class="tool-pre" id="encOut" style="margin-top:10px"></pre>`;
    encodeText('b64e');
  }
  function encodeText(mode) {
    const value = $('#encInp',modalBody).value; let result;
    try {
      if (mode === 'b64e') result = btoa(unescape(encodeURIComponent(value)));
      else if (mode === 'b64d') result = decodeURIComponent(escape(atob(value.trim())));
      else if (mode === 'hex') result = [...new TextEncoder().encode(value)].map(byte => byte.toString(16).padStart(2,'0')).join(' ');
      else if (mode === 'url') result = encodeURIComponent(value);
      else if (mode === 'urldecode') result = decodeURIComponent(value.replace(/\+/g,' '));
      else result = 'Mode tidak dikenal.';
    } catch(error) { result = `Input tidak valid: ${error.message}`; }
    $('#encOut',modalBody).textContent = result;
  }

      // RYOMD_ANIME_HUB_UI_V1_START
  function renderAnimeHub() {
    const modal = backdrop.querySelector('.tool-modal');
    if (modal) modal.classList.add('ryomd-anime-expanded');
    modalBody.innerHTML = `
      <section class="ryomd-anime-embed" aria-label="RYOMD Anime streaming">
        <div class="ryomd-anime-toolbar">
          <div class="ryomd-anime-brand"><span class="ryomd-anime-mark">R</span><span><b>RYOMD ANIME</b><small>LUMIO ENGINE · LOCAL SERVICE</small></span></div>
          <div class="ryomd-anime-actions">
            <span class="ryomd-anime-state" id="ryomdAnimeState"><i></i><b>Menyiapkan service…</b></span>
            <button type="button" class="ryomd-anime-action" id="ryomdAnimeReload">Mulai ulang</button>
            <button type="button" class="ryomd-anime-action danger" id="ryomdAnimeStop">Hentikan</button>
          </div>
        </div>
        <div class="ryomd-anime-stage" id="ryomdAnimeStage">
          <div class="ryomd-anime-loading" id="ryomdAnimeLoading"><span class="ryomd-anime-spinner"></span><b>Menyalakan mesin Anime</b><small>Katalog, database, dan player sedang dipersiapkan.</small></div>
          <iframe id="ryomdAnimeFrame" title="RYOMD Anime" allow="autoplay; fullscreen; picture-in-picture; encrypted-media" allowfullscreen referrerpolicy="no-referrer" loading="eager" src="about:blank"></iframe>
        </div>
        <div class="ryomd-anime-footer"><span id="ryomdAnimeServiceAddress">Service lokal · menunggu alamat service</span><span>Menutup jendela tidak menghapus riwayat atau database Anime.</span></div>
      </section>`;

    const stateEl = $('#ryomdAnimeState', modalBody);
    const stage = $('#ryomdAnimeStage', modalBody);
    const loading = $('#ryomdAnimeLoading', modalBody);
    const frame = $('#ryomdAnimeFrame', modalBody);
    const reload = $('#ryomdAnimeReload', modalBody);
    const stop = $('#ryomdAnimeStop', modalBody);
    let launchSequence = 0;
    frame.addEventListener('load', () => {
      if (frame.src && frame.src !== 'about:blank') loading.hidden = true;
    });

    function setState(label, status) {
      const state = status || 'loading';
      stateEl.className = 'ryomd-anime-state ' + state;
      stateEl.innerHTML = '<i></i><b>' + esc(label) + '</b>';
    }
    function showError(data) {
      const message = String(data && data.message || 'Anime Service gagal dijalankan.');
      setState('Service belum siap', 'error');
      loading.hidden = true;
      let extra = '';
      if (data && data.setupCommand) {
        extra += '<details class="ryomd-anime-error-details"><summary>Perintah instal dependensi Termux</summary><code id="ryomdAnimeSetupCommand">' + esc(data.setupCommand) + '</code><button type="button" id="ryomdAnimeCopySetup">Salin perintah</button></details>';
      }
      if (data && data.logs) {
        extra += '<details class="ryomd-anime-error-details"><summary>Lihat log service</summary><pre>' + esc(String(data.logs).slice(-6000)) + '</pre></details>';
      }
      const errorBox = document.createElement('div');
      errorBox.className = 'ryomd-anime-error';
      errorBox.innerHTML = '<span>RYOMD ANIME</span><h3>Tidak dapat membuka katalog</h3><p>' + esc(message) + '</p>' + extra;
      stage.replaceChildren(errorBox);
      const copyBtn = $('#ryomdAnimeCopySetup', errorBox);
      if (copyBtn) copyBtn.addEventListener('click', async () => {
        const command = $('#ryomdAnimeSetupCommand', errorBox)?.textContent || '';
        try {
          await navigator.clipboard.writeText(command);
          copyBtn.textContent = 'Tersalin';
        } catch (_) {
          copyBtn.textContent = 'Salin tidak didukung';
        }
      });
    }
    function showFrame(url, portValue) {
      let target;
      const port = Number(portValue);
      try { target = new URL(String(url || '')); }
      catch (_) { throw new Error('Launcher tidak mengembalikan alamat service Anime yang valid.'); }
      const allowedPort = Number.isInteger(port) && port >= 8767 && port <= 8799;
      const samePort = target.port === String(port);
      const localRoot = target.pathname === '/' && !target.search && !target.hash && !target.username && !target.password;
      if (target.protocol !== 'http:' || target.hostname !== '127.0.0.1' || !allowedPort || !samePort || !localRoot) {
        throw new Error('Alamat service Anime ditolak. Endpoint harus berupa 127.0.0.1 dengan port lokal 8767–8799 yang sama dengan port dari launcher.');
      }
      loading.hidden = false;
      loading.innerHTML = '<span class="ryomd-anime-spinner"></span><b>Membuka RYOMD Anime</b><small>Menyiapkan katalog dan pemutar video.</small>';
      stage.replaceChildren(loading, frame);
      frame.src = target.href;
      const address = $('#ryomdAnimeServiceAddress', modalBody);
      if (address) address.textContent = 'Service lokal · ' + target.host;
      setState('Service aktif', 'ready');
    }
    async function launch(restart) {
      const sequence = ++launchSequence;
      reload.disabled = true;
      stop.disabled = true;
      setState(restart ? 'Memulai ulang…' : 'Menyalakan service…', 'loading');
      loading.hidden = false;
      loading.innerHTML = '<span class="ryomd-anime-spinner"></span><b>' + (restart ? 'Memulai ulang mesin Anime' : 'Menyalakan mesin Anime') + '</b><small>Menyiapkan katalog, database, dan player.</small>';
      if (restart) frame.src = 'about:blank';
      try {
        const response = await fetch('/api/anime/launch' + (restart ? '?restart=1' : ''), { method: 'POST', headers: { Accept: 'application/json' } });
        const data = await response.json();
        if (sequence !== launchSequence) return;
        if (!response.ok || !data.ok) { showError(data); return; }
        showFrame(data.url, data.port);
      } catch (error) {
        if (sequence !== launchSequence) return;
        showError({ message: error.message || 'Tidak dapat menghubungi pengelola service di RyoPanel.' });
      } finally {
        if (sequence === launchSequence) {
          reload.disabled = false;
          stop.disabled = false;
        }
      }
    }
    reload.addEventListener('click', () => launch(true));
    stop.addEventListener('click', async () => {
      stop.disabled = true;
      setState('Menghentikan…', 'loading');
      try {
        const response = await fetch('/api/anime/stop', { method: 'POST', headers: { Accept: 'application/json' } });
        const data = await response.json();
        frame.src = 'about:blank';
        stage.replaceChildren();
        const stopped = document.createElement('div');
        stopped.className = 'ryomd-anime-loading';
        stopped.innerHTML = '<b>RYOMD Anime dihentikan</b><small>' + esc(data.message || 'Service dihentikan. Database dan riwayat tetap tersimpan.') + '</small>';
        stage.appendChild(stopped);
        setState('Service berhenti', 'stopped');
      } catch (error) {
        setState('Gagal menghentikan service', 'error');
      } finally {
        stop.disabled = false;
      }
    });
    launch(false);
  }
  // RYOMD_ANIME_HUB_UI_V1_END

  // RYOMD MUSIC STUDIO V3 — independent client-side providers and local library.
  const musicState = {
    tab: 'discover', provider: 'youtube', query: '', results: [], loading: false, error: '', notice: '',
    favorites: safeStorage.get('ryomd_music_v3_favorites', []),
    playlists: safeStorage.get('ryomd_music_v3_playlists', []),
    history: safeStorage.get('ryomd_music_v3_history', []),
    localTracks: [],
    queue: safeStorage.get('ryomd_music_v3_queue', []),
    currentIndex: -1, current: null, activePlaylist: null,
    repeat: 'off', shuffle: false, volume: Number(safeStorage.get('ryomd_music_v3_volume', 0.82))
  };
  const MUSIC_API = 'https://api.audius.co/v1';
  let musicDock = null;
  let musicAudio = null;
  let musicYoutubeDock = null;
  let musicYoutubeDismissed = false;
  let musicYoutubeApiPromise = null;
  let musicYoutubePlayer = null;
  let musicYoutubePlayerSeq = 0;
  let musicObjectUrl = null;
  let musicDbPromise = null;

  function musicSafeUrl(value) {
    try { const url = new URL(String(value || '')); return ['https:', 'http:'].includes(url.protocol) ? url.href : ''; }
    catch { return ''; }
  }
  function musicDuration(seconds) {
    const n = Math.max(0, Number(seconds) || 0);
    return `${Math.floor(n / 60)}:${String(Math.floor(n % 60)).padStart(2, '0')}`;
  }
  function musicTrackKey(track) { return `${track.source || 'local'}:${String(track.id || track.key || '')}`; }
  function musicNormalizeAudius(track) {
    if (!track || !track.id) return null;
    const user = track.user || {};
    const artwork = track.artwork && (track.artwork['480x480'] || track.artwork['150x150'] || track.artwork['1000x1000']);
    const permalink = musicSafeUrl(track.permalink) || (user.handle ? `https://audius.co/${encodeURIComponent(user.handle)}/${encodeURIComponent(String(track.title || 'track').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, ''))}-${encodeURIComponent(track.id)}` : 'https://audius.co');
    return {
      id: String(track.id), key: `audius:${track.id}`, source: 'audius', title: String(track.title || 'Untitled'),
      artist: String(user.name || user.handle || 'Unknown artist'), artwork: musicSafeUrl(artwork),
      duration: Number(track.duration) || 0, genre: String(track.genre || ''),
      streamUrl: `${MUSIC_API}/tracks/${encodeURIComponent(track.id)}/stream`,
      playable: track.is_streamable === true && track.is_stream_gated !== true,
      externalUrl: permalink, previewOnly: false
    };
  }
  function musicNormalizeApple(track) {
    if (!track || !track.trackId || !track.previewUrl) return null;
    const art = String(track.artworkUrl100 || '').replace(/\d+x\d+bb\./, '512x512bb.');
    return {
      id: String(track.trackId), key: `apple:${track.trackId}`, source: 'apple', title: String(track.trackName || 'Untitled'),
      artist: String(track.artistName || 'Unknown artist'), artwork: musicSafeUrl(art),
      duration: 30, genre: String(track.primaryGenreName || ''),
      streamUrl: musicSafeUrl(track.previewUrl), playable: true,
      externalUrl: musicSafeUrl(track.trackViewUrl), previewOnly: true
    };
  }
  // Match provider metadata to the user's query before presenting a track as a candidate.
  // This prevents loosely related Audius search hits from masquerading as the requested song.
  function musicSearchNormalize(value) {
    // Unicode-aware normalization keeps Japanese, Korean, Chinese and other scripts searchable.
    // Strip Latin combining accents only; retain non-Latin marks such as Japanese dakuten.
    return String(value || '').normalize('NFKD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase()
      .replace(/&/g, ' and ').replace(/[^\p{L}\p{N}\u3099\u309A]+/gu, ' ').trim().replace(/\s+/g, ' ');
  }
  const MUSIC_QUERY_STOPWORDS = new Set(['official','audio','video','lyrics','lyric','hd','4k','visualizer']);
  function musicScoreQuery(query, track) {
    const normalizedQuery = musicSearchNormalize(query);
    const queryTokens = [...new Set(normalizedQuery.split(' ').filter(word => word && !MUSIC_QUERY_STOPWORDS.has(word)))];
    if (!queryTokens.length) return { matched: true, score: 1 };
    const title = musicSearchNormalize(track.title);
    const artist = musicSearchNormalize(track.artist);
    const titleTokens = new Set(title.split(' ').filter(Boolean));
    const artistTokens = new Set(artist.split(' ').filter(Boolean));
    const matchedTokens = queryTokens.filter(word => titleTokens.has(word) || artistTokens.has(word));
    const titleHits = queryTokens.filter(word => titleTokens.has(word)).length;
    const artistHits = queryTokens.filter(word => artistTokens.has(word)).length;
    const coverage = matchedTokens.length / queryTokens.length;
    const artistCoverage = artistHits / queryTokens.length;
    const phraseMatch = Boolean(normalizedQuery && (title.includes(normalizedQuery) || artist.includes(normalizedQuery)));
    const compactQuery = queryTokens.join('');
    const compactMatch = compactQuery.length >= 4 && (title.replace(/\s/g, '').includes(compactQuery) || artist.replace(/\s/g, '').includes(compactQuery));
    const isSingleTerm = queryTokens.length === 1;
    const minCoverage = queryTokens.length <= 4 ? 0.8 : 0.75;
    const matched = phraseMatch || compactMatch || (isSingleTerm && matchedTokens.length === 1) ||
      (coverage >= minCoverage && (titleHits > 0 || artistCoverage >= 0.75));
    const score = Math.min(1, coverage * 0.78 + (titleHits / queryTokens.length) * 0.17 + (phraseMatch || compactMatch ? 0.05 : 0));
    return { matched, score, coverage, phraseMatch, compactMatch };
  }
  function musicFilterAndRank(items, query, sourceBoost = 0) {
    return items.map(track => {
      const match = musicScoreQuery(query, track);
      return { ...track, matchScore: match.score + sourceBoost, searchMatched: match.matched };
    }).filter(track => track.searchMatched)
      .sort((a, b) => b.matchScore - a.matchScore || (a.title || '').localeCompare(b.title || ''));
  }
  async function musicFetchJson(url, timeoutMs = 14000) {
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(url, { signal: controller.signal, headers: { Accept: 'application/json' } });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return await response.json();
    } finally { window.clearTimeout(timer); }
  }
  async function musicSearchAudius(query, trending = false) {
    const path = trending ? '/tracks/trending' : '/tracks/search';
    const params = new URLSearchParams({ limit: '40' });
    if (!trending) params.set('query', query);
    const payload = await musicFetchJson(`${MUSIC_API}${path}?${params.toString()}`);
    const rows = Array.isArray(payload.data) ? payload.data : [];
    const tracks = rows.map(musicNormalizeAudius).filter(Boolean);
    // Trending is intentionally broad; a typed search must meet a title/artist match threshold.
    return trending ? tracks : musicFilterAndRank(tracks, query, 0.015);
  }
  async function musicSearchApple(query) {
    const params = new URLSearchParams({ term: query, entity: 'song', limit: '30', media: 'music' });
    const payload = await musicFetchJson(`https://itunes.apple.com/search?${params.toString()}`);
    const tracks = (Array.isArray(payload.results) ? payload.results : []).map(musicNormalizeApple).filter(Boolean);
    return musicFilterAndRank(tracks, query, 0);
  }
  async function musicSearchYouTube(query) {
    if (!query || query.trim().length < 2) return [];
    const endpoint = `http://127.0.0.1:3003/api/search?q=${encodeURIComponent(query.trim())}&limit=18`;
    let response;
    try {
      response = await fetch(endpoint, { method: 'GET', mode: 'cors', credentials: 'omit', cache: 'no-store', headers: { Accept: 'application/json' } });
    } catch (_) {
      throw new Error('Layanan pencarian YouTube Music belum berjalan. Pasang Bridge Termux, lalu jalankan: bash ~/ryomd-ytmusic-bridge/control.sh start.');
    }
    let payload;
    try { payload = await response.json(); } catch (_) { throw new Error('Bridge YouTube Music mengirim respons yang tidak valid.'); }
    if (!response.ok || payload.error) throw new Error(payload.message || `Pencarian YouTube Music gagal (HTTP ${response.status}).`);
    const rows = Array.isArray(payload.songs) ? payload.songs : [];
    return rows.map((song, index) => {
      const id = String(song.videoId || song.id || '');
      if (!/^[A-Za-z0-9_-]{11}$/.test(id)) return null;
      const artist = String(song.artist || 'Artis tidak diketahui');
      const url = `https://music.youtube.com/watch?v=${encodeURIComponent(id)}`;
      return {
        id, key: `youtube:${id}`, source: 'youtube', title: String(song.title || 'Tanpa judul'),
        artist, artwork: musicSafeUrl(song.thumbnail), album: String(song.album || ''),
        duration: Number(song.durationSeconds) || 0, genre: 'YouTube Music · Songs', videoId: id,
        playable: song.isAvailable !== false, isAvailable: song.isAvailable !== false,
        previewOnly: false, externalUrl: url,
        matchScore: 1 - index * 0.001, explicit: Boolean(song.isExplicit)
      };
    }).filter(Boolean);
  }
  function musicPersist() {
    safeStorage.set('ryomd_music_v3_favorites', musicState.favorites);
    safeStorage.set('ryomd_music_v3_playlists', musicState.playlists);
    safeStorage.set('ryomd_music_v3_history', musicState.history.slice(0, 80));
    safeStorage.set('ryomd_music_v3_queue', musicState.queue);
    safeStorage.set('ryomd_music_v3_volume', musicState.volume);
  }
  function musicIsFavorite(track) { return musicState.favorites.some(item => musicTrackKey(item) === musicTrackKey(track)); }
  function musicToggleFavorite(track) {
    const key = musicTrackKey(track);
    const existed = musicIsFavorite(track);
    musicState.favorites = musicState.favorites.filter(item => musicTrackKey(item) !== key);
    if (!existed) musicState.favorites.unshift(track);
    musicPersist();
    toast(existed ? 'Dihapus dari favorit.' : 'Ditambahkan ke favorit.', 'success');
  }
  function musicEnsureDock() {
    if (musicDock) return;
    musicDock = document.createElement('aside');
    musicDock.className = 'ryo-music-dock';
    musicDock.id = 'ryoMusicDock';
    musicDock.hidden = true;
    musicDock.innerHTML = `
      <audio id="ryoMusicAudio" preload="none"></audio>
      <button type="button" class="ryo-dock-open" data-music-dock="now-playing" aria-label="Buka pemutar penuh">
        <img class="ryo-dock-art" id="ryoMusicDockArt" alt="Sampul lagu" src="">
        <span class="ryo-dock-copy"><b id="ryoMusicDockTitle">Belum ada lagu</b><small id="ryoMusicDockArtist">Pilih lagu untuk mulai</small></span>
      </button>
      <div class="ryo-dock-controls">
        <button type="button" data-music-dock="previous" aria-label="Lagu sebelumnya" title="Sebelumnya"><svg viewBox="0 0 24 24"><path d="M6 5v14M19 6 8 12l11 6V6Z"/></svg></button>
        <button type="button" data-music-dock="toggle" class="ryo-dock-play" aria-label="Putar" title="Putar"><svg viewBox="0 0 24 24"><path d="m8 5 11 7-11 7V5Z"/></svg></button>
        <button type="button" data-music-dock="next" aria-label="Lagu berikutnya" title="Berikutnya"><svg viewBox="0 0 24 24"><path d="M18 5v14M5 6l11 6-11 6V6Z"/></svg></button>
      </div>
      <div class="ryo-dock-progress"><span id="ryoMusicCurrentTime">0:00</span><input id="ryoMusicSeek" type="range" min="0" max="1000" value="0" aria-label="Posisi pemutaran"><span id="ryoMusicDuration">0:00</span></div>
      <label class="ryo-dock-volume" title="Volume"><svg viewBox="0 0 24 24"><path d="M4 10v4h4l5 4V6l-5 4H4Z"/><path d="M16 9a5 5 0 0 1 0 6"/></svg><input id="ryoMusicVolume" type="range" min="0" max="1" step="0.02" value="${musicState.volume}" aria-label="Volume"></label>
      <button type="button" class="ryo-dock-queue" data-music-dock="queue" aria-label="Buka antrean" title="Antrean"><svg viewBox="0 0 24 24"><path d="M4 6h13M4 11h13M4 16h8M17 15l3 3-3 3"/></svg></button>`;
    document.body.appendChild(musicDock);
    musicAudio = $('#ryoMusicAudio', musicDock);
    musicAudio.volume = Math.min(1, Math.max(0, musicState.volume));
    musicAudio.addEventListener('play', musicSyncDock);
    musicAudio.addEventListener('pause', musicSyncDock);
    musicAudio.addEventListener('timeupdate', musicSyncDockProgress);
    musicAudio.addEventListener('timeupdate', musicUpdateLyricsDisplay);
    musicAudio.addEventListener('loadedmetadata', musicSyncDockProgress);
    musicAudio.addEventListener('ended', () => musicAdvance(1, true));
    musicAudio.addEventListener('error', () => {
      if (musicState.current) toast(musicState.current.previewOnly ? 'Preview Apple tidak dapat diputar.' : musicState.current.source === 'local' ? 'File audio lokal tidak didukung atau tidak dapat dibaca browser.' : 'Stream Audius gagal dimuat. Coba lagu lain atau buka halaman sumber.', 'error');
      musicSyncDock();
    });
    musicDock.addEventListener('click', event => {
      const control = event.target.closest('[data-music-dock]');
      if (!control) return;
      const action = control.dataset.musicDock;
      if (action === 'now-playing') {
        musicState.tab = 'nowplaying';
        if (modalTitle.textContent === 'Music Studio V3') renderMusicStudio();
        else openTool('music-studio');
      } else if (action === 'toggle') {
        if (!musicAudio.src) return;
        if (musicAudio.paused) musicAudio.play().catch(() => toast('Browser tidak dapat memulai pemutaran.', 'error'));
        else musicAudio.pause();
      } else if (action === 'next') musicAdvance(1);
      else if (action === 'previous') musicAdvance(-1);
      else if (action === 'queue') { musicState.tab = 'queue'; openTool('music-studio'); }
    });
    $('#ryoMusicSeek', musicDock).addEventListener('input', event => {
      if (Number.isFinite(musicAudio.duration) && musicAudio.duration > 0) musicAudio.currentTime = Number(event.target.value) / 1000 * musicAudio.duration;
    });
    $('#ryoMusicVolume', musicDock).addEventListener('input', event => { musicState.volume = Math.min(1, Math.max(0, Number(event.target.value))); musicAudio.volume = musicState.volume; musicPersist(); });
  }
  function musicInstallYoutubeDockDrag() {
    if (!musicYoutubeDock) return;
    const header = $('.ryomd-music-youtube-head', musicYoutubeDock);
    if (!header || header.dataset.ryoDragReady === '1') return;
    header.dataset.ryoDragReady = '1';
    const positionKey = 'ryomd_music_youtube_dock_position_v1';
    let active = null;

    const clampPosition = (left, top) => {
      const rect = musicYoutubeDock.getBoundingClientRect();
      const maxLeft = Math.max(8, window.innerWidth - rect.width - 8);
      const maxTop = Math.max(8, window.innerHeight - rect.height - 8);
      return {
        left: Math.min(maxLeft, Math.max(8, left)),
        top: Math.min(maxTop, Math.max(8, top))
      };
    };
    const applyPosition = (left, top) => {
      const point = clampPosition(left, top);
      musicYoutubeDock.style.left = `${point.left}px`;
      musicYoutubeDock.style.top = `${point.top}px`;
      musicYoutubeDock.style.right = 'auto';
      musicYoutubeDock.style.bottom = 'auto';
      return point;
    };
    const savePosition = () => {
      const rect = musicYoutubeDock.getBoundingClientRect();
      try {
        localStorage.setItem(positionKey, JSON.stringify({ left: rect.left, top: rect.top }));
      } catch (_) {}
    };

    header.addEventListener('pointerdown', event => {
      if (event.target.closest('button') || (event.pointerType === 'mouse' && event.button !== 0)) return;
      const rect = musicYoutubeDock.getBoundingClientRect();
      active = { pointerId: event.pointerId, startX: event.clientX, startY: event.clientY, left: rect.left, top: rect.top, moved: false };
      musicYoutubeDock.style.left = `${rect.left}px`;
      musicYoutubeDock.style.top = `${rect.top}px`;
      musicYoutubeDock.style.right = 'auto';
      musicYoutubeDock.style.bottom = 'auto';
      header.classList.add('is-dragging');
      try { header.setPointerCapture(event.pointerId); } catch (_) {}
      event.preventDefault();
    });
    header.addEventListener('pointermove', event => {
      if (!active || event.pointerId !== active.pointerId) return;
      const dx = event.clientX - active.startX;
      const dy = event.clientY - active.startY;
      if (Math.abs(dx) + Math.abs(dy) > 3) active.moved = true;
      if (active.moved) {
        applyPosition(active.left + dx, active.top + dy);
        event.preventDefault();
      }
    });
    const finishDrag = event => {
      if (!active || (event && event.pointerId !== active.pointerId)) return;
      const moved = active.moved;
      active = null;
      header.classList.remove('is-dragging');
      if (moved) savePosition();
    };
    header.addEventListener('pointerup', finishDrag);
    header.addEventListener('pointercancel', finishDrag);
    header.addEventListener('lostpointercapture', finishDrag);
    window.addEventListener('resize', () => {
      if (!musicYoutubeDock || musicYoutubeDock.hidden || !musicYoutubeDock.style.left) return;
      const rect = musicYoutubeDock.getBoundingClientRect();
      applyPosition(rect.left, rect.top);
    }, { passive: true });

    musicYoutubeDock.dataset.ryoDragPositionKey = positionKey;
  }

  function musicRestoreYoutubeDockPosition() {
    if (!musicYoutubeDock) return;
    const key = musicYoutubeDock.dataset.ryoDragPositionKey || 'ryomd_music_youtube_dock_position_v1';
    try {
      const saved = JSON.parse(localStorage.getItem(key) || 'null');
      if (!saved || !Number.isFinite(Number(saved.left)) || !Number.isFinite(Number(saved.top))) return;
      requestAnimationFrame(() => {
        if (!musicYoutubeDock || musicYoutubeDock.hidden) return;
        const rect = musicYoutubeDock.getBoundingClientRect();
        const maxLeft = Math.max(8, window.innerWidth - rect.width - 8);
        const maxTop = Math.max(8, window.innerHeight - rect.height - 8);
        musicYoutubeDock.style.left = `${Math.min(maxLeft, Math.max(8, Number(saved.left)))}px`;
        musicYoutubeDock.style.top = `${Math.min(maxTop, Math.max(8, Number(saved.top)))}px`;
        musicYoutubeDock.style.right = 'auto';
        musicYoutubeDock.style.bottom = 'auto';
      });
    } catch (_) {}
  }

  function musicEnsureYoutubeDock() {
    if (musicYoutubeDock) return;
    musicYoutubeDock = document.createElement('aside');
    musicYoutubeDock.className = 'ryomd-music-youtube-dock';
    musicYoutubeDock.id = 'ryomdMusicYoutubeDock';
    musicYoutubeDock.hidden = true;
    musicYoutubeDock.setAttribute('aria-label', 'Pemutar YouTube Music');
    musicYoutubeDock.innerHTML = `
      <header class="ryomd-music-youtube-head">
        <span><b>RYOMD MUSIC <em class="ryomd-music-drag-cue">GESER PANEL</em></b><small id="ryomdMusicYoutubeTitle">Pemutar YouTube tertanam</small></span>
        <div><button type="button" data-music-yt-dock="previous" aria-label="Lagu sebelumnya" title="Sebelumnya">⏮</button><button type="button" data-music-yt-dock="next" aria-label="Lagu berikutnya" title="Berikutnya">⏭</button><button type="button" data-music-yt-dock="close" aria-label="Tutup pemutar" title="Tutup">×</button></div>
      </header>
      <div class="ryomd-music-youtube-frame" id="ryomdMusicYoutubeFrame"><div class="ryomd-music-youtube-placeholder">Player YouTube akan tampil di sini.</div></div>
      <footer class="ryomd-music-youtube-foot" id="ryomdMusicYoutubeFoot" role="status" aria-live="polite">Pemutaran berlangsung di dalam Ryomd. Iklan dan kebijakan konten tetap mengikuti YouTube.</footer>`;
    document.body.appendChild(musicYoutubeDock);
    musicInstallYoutubeDockDrag();
    musicYoutubeDock.addEventListener('click', event => {
      const control = event.target.closest('[data-music-yt-dock]');
      if (!control) return;
      const action = control.dataset.musicYtDock;
      if (action === 'previous') musicAdvance(-1);
      else if (action === 'next') musicAdvance(1);
      else if (action === 'close') {
        musicYoutubeDismissed = true;
        musicYoutubePlayerSeq++;
        if (musicYoutubePlayer && typeof musicYoutubePlayer.destroy === 'function') {
          try { musicYoutubePlayer.destroy(); } catch (_) {}
        }
        musicYoutubePlayer = null;
        const frame = $('#ryomdMusicYoutubeFrame', musicYoutubeDock);
        if (frame) frame.innerHTML = '<div class="ryomd-music-youtube-placeholder">Player ditutup. Tekan Putar lagi dari Music Studio untuk membukanya.</div>';
        const foot = $('#ryomdMusicYoutubeFoot', musicYoutubeDock);
        if (foot) foot.textContent = 'Player ditutup.';
        musicYoutubeDock.hidden = true;
        modalBody.classList.remove('ryomd-music-youtube-open');
        document.body.classList.remove('ryomd-music-youtube-active');
        if (!musicDock || musicDock.hidden) document.body.classList.remove('ryo-music-dock-active');
      }
    });
  }
  function musicEnsureYouTubePlayerApi() {
    if (window.YT && typeof window.YT.Player === 'function') return Promise.resolve(window.YT);
    if (musicYoutubeApiPromise) return musicYoutubeApiPromise;
    musicYoutubeApiPromise = new Promise((resolve, reject) => {
      let settled = false;
      const finish = (error) => {
        if (settled) return;
        settled = true;
        window.clearTimeout(timer);
        if (error) reject(error);
        else if (window.YT && typeof window.YT.Player === 'function') resolve(window.YT);
        else reject(new Error('YouTube Player API belum siap.'));
      };
      const previousReady = window.onYouTubeIframeAPIReady;
      const timer = window.setTimeout(() => finish(new Error('Koneksi ke YouTube Player API melewati batas waktu.')), 20000);
      window.onYouTubeIframeAPIReady = function () {
        if (typeof previousReady === 'function') {
          try { previousReady(); } catch (_) {}
        }
        finish();
      };
      let script = document.querySelector('script[src*="youtube.com/iframe_api"]');
      if (!script) {
        script = document.createElement('script');
        script.src = 'https://www.youtube.com/iframe_api';
        script.async = true;
        script.onerror = () => finish(new Error('YouTube Player API tidak dapat dimuat.'));
        document.head.appendChild(script);
      } else {
        script.addEventListener('error', () => finish(new Error('YouTube Player API tidak dapat dimuat.')), { once: true });
        if (window.YT && typeof window.YT.Player === 'function') finish();
      }
    }).catch(error => {
      musicYoutubeApiPromise = null;
      throw error;
    });
    return musicYoutubeApiPromise;
  }
  function musicYoutubeErrorDetails(code) {
    const messages = {
      2: ['ID video tidak valid', 'Hasil pencarian memberikan ID video yang tidak diterima YouTube. Cari ulang lagu tersebut.'],
      5: ['Pemutar HTML5 gagal', 'Browser/WebView tidak dapat memutar format video ini. Coba hasil video lain.'],
      100: ['Video tidak tersedia', 'Video mungkin dihapus atau dijadikan privat oleh pemiliknya.'],
      101: ['Video tidak boleh di-embed', 'Pemilik video menonaktifkan pemutaran di situs/aplikasi lain. Ryomd tidak dapat menghapus aturan pemilik video.'],
      150: ['Video tidak boleh di-embed', 'Pemilik video menonaktifkan pemutaran di situs/aplikasi lain. Ryomd tidak dapat menghapus aturan pemilik video.'],
      153: ['Identitas pemutar tidak diterima', 'YouTube tidak menerima HTTP Referer atau identitas aplikasi. Buka dashboard Ryomd langsung dari alamat lokalnya, bukan dari file HTML/preview.']
    };
    return messages[Number(code)] || ['Video gagal diputar', `YouTube mengembalikan kode error ${Number(code) || 'tidak diketahui'}. Coba hasil video lain.`];
  }
  function musicMarkYoutubeUnavailable(videoId, code) {
    const matches = [
      musicState.results, musicState.favorites, musicState.history, musicState.queue,
      musicState.localTracks,
      ...musicState.playlists.map(item => item.tracks || [])
    ];
    for (const list of matches) {
      for (const item of list || []) {
        if (item && item.source === 'youtube' && String(item.videoId || item.id) === videoId) {
          item.playable = false;
          item.embedError = Number(code);
        }
      }
    }
    if (musicState.current && musicState.current.source === 'youtube' && String(musicState.current.videoId || musicState.current.id) === videoId) {
      musicState.current.playable = false;
      musicState.current.embedError = Number(code);
    }
    musicPersist();
    if (modalTitle.textContent === 'Music Studio V3') renderMusicStudio();
  }
  function musicShowYoutubeError(code, videoId, sequence) {
    if (sequence !== musicYoutubePlayerSeq || musicYoutubeDismissed) return;
    const frame = $('#ryomdMusicYoutubeFrame', musicYoutubeDock);
    const foot = $('#ryomdMusicYoutubeFoot', musicYoutubeDock);
    const [title, detail] = musicYoutubeErrorDetails(code);
    if ([100, 101, 150].includes(Number(code))) musicMarkYoutubeUnavailable(videoId, code);
    if (frame) {
      const panel = document.createElement('div');
      panel.className = 'ryomd-music-youtube-error';
      const heading = document.createElement('strong');
      heading.textContent = title;
      const description = document.createElement('p');
      description.textContent = `${detail} Ryomd tidak akan mengganti lagu secara otomatis atau membuka aplikasi YouTube.`;
      panel.append(heading, description);
      frame.replaceChildren(panel);
    }
    if (foot) foot.textContent = 'Pemutaran tertanam ditolak atau gagal. Pilih sendiri hasil lain dari daftar pencarian bila diperlukan.';
  }
  function musicTryNextYouTubeCandidate() {
    const candidates = musicState.results.filter(item => item && item.source === 'youtube');
    if (!candidates.length) {
      toast('Tidak ada hasil video lain. Ubah kata kunci pencarian.', 'info');
      return;
    }
    const currentId = String(musicState.current?.videoId || musicState.current?.id || '');
    const currentIndex = candidates.findIndex(item => String(item.videoId || item.id) === currentId);
    for (let offset = 1; offset <= candidates.length; offset++) {
      const candidate = candidates[(Math.max(currentIndex, -1) + offset) % candidates.length];
      if (candidate && candidate.playable !== false && String(candidate.videoId || candidate.id) !== currentId) {
        musicPlayTrack(candidate);
        return;
      }
    }
    toast('Semua kandidat lain sudah dicoba atau tidak tersedia untuk pemutaran tertanam.', 'info');
  }
  function musicShowYoutubeDock(track) {
    musicEnsureYoutubeDock();
    const videoId = String(track?.videoId || track?.id || '').trim();
    if (!/^[A-Za-z0-9_-]{11}$/.test(videoId)) {
      toast('ID lagu dari katalog YouTube Music tidak valid.', 'error');
      return false;
    }
    musicYoutubeDismissed = false;
    musicYoutubePlayerSeq++;
    if (musicYoutubePlayer && typeof musicYoutubePlayer.destroy === 'function') {
      try { musicYoutubePlayer.destroy(); } catch (_) {}
    }
    musicYoutubePlayer = null;
    const frame = $('#ryomdMusicYoutubeFrame', musicYoutubeDock);
    const foot = $('#ryomdMusicYoutubeFoot', musicYoutubeDock);
    $('#ryomdMusicYoutubeTitle', musicYoutubeDock).textContent = `${track.title} · ${track.artist}`;
    if (frame) {
      const iframe = document.createElement('iframe');
      iframe.id = 'ryomdMusicYoutubePlayerFrame';
      iframe.title = `${track.title} — pemutar tertanam Ryomd`;
      iframe.loading = 'eager';
      iframe.referrerPolicy = 'strict-origin-when-cross-origin';
      iframe.allow = 'autoplay; encrypted-media; picture-in-picture; fullscreen; web-share';
      iframe.allowFullscreen = true;
      iframe.src = `https://www.youtube-nocookie.com/embed/${encodeURIComponent(videoId)}?autoplay=1&playsinline=1&controls=1&rel=0&hl=id&enablejsapi=1&origin=${encodeURIComponent(location.origin)}`;
      frame.replaceChildren(iframe);
    }
    if (foot) foot.textContent = 'Diputar di panel Ryomd dari hasil katalog YouTube Music. Geser panel dari header. Jika YouTube menolak embed untuk lagu ini, pilih hasil lain secara manual; Ryomd tidak akan membuka aplikasi atau mengganti lagu otomatis.';
    musicYoutubeDock.hidden = false;
    modalBody.classList.add('ryomd-music-youtube-open');
    document.body.classList.add('ryomd-music-youtube-active', 'ryo-music-dock-active');
    musicRestoreYoutubeDockPosition();
    return true;
  }
  function musicHideYoutubeDock() {
    if (!musicYoutubeDock) {
      modalBody.classList.remove('ryomd-music-youtube-open');
      return;
    }
    musicYoutubePlayerSeq++;
    if (musicYoutubePlayer && typeof musicYoutubePlayer.destroy === 'function') {
      try { musicYoutubePlayer.destroy(); } catch (_) {}
    }
    musicYoutubePlayer = null;
    const frame = $('#ryomdMusicYoutubeFrame', musicYoutubeDock);
    if (frame) frame.innerHTML = '<div class="ryomd-music-youtube-placeholder">Player YouTube tidak aktif.</div>';
    musicYoutubeDock.hidden = true;
    modalBody.classList.remove('ryomd-music-youtube-open');
    document.body.classList.remove('ryomd-music-youtube-active');
  }
  function musicSyncDock() {
    if (!musicDock || !musicAudio) return;
    if (musicState.current?.source === 'youtube') {
      musicDock.hidden = true;
      const playerVisible = Boolean(musicYoutubeDock && !musicYoutubeDock.hidden && !musicYoutubeDismissed);
      document.body.classList.toggle('ryomd-music-youtube-active', playerVisible);
      document.body.classList.toggle('ryo-music-dock-active', playerVisible);
      return;
    }
    musicHideYoutubeDock();
    const playButton = $('[data-music-dock="toggle"]', musicDock);
    if (playButton) {
      playButton.innerHTML = musicAudio.paused
        ? '<svg viewBox="0 0 24 24"><path d="m8 5 11 7-11 7V5Z"/></svg>'
        : '<svg viewBox="0 0 24 24"><path d="M8 5h3v14H8zM15 5h3v14h-3z"/></svg>';
      playButton.setAttribute('aria-label', musicAudio.paused ? 'Putar' : 'Jeda');
    }
    const isVisible = Boolean(musicState.current) && musicState.current.source !== 'youtube';
    musicDock.hidden = !isVisible;
    document.body.classList.toggle('ryo-music-dock-active', Boolean(musicState.current) && (isVisible || !musicYoutubeDismissed));
    if (isVisible) {
      $('#ryoMusicDockTitle', musicDock).textContent = musicState.current.title;
      $('#ryoMusicDockArtist', musicDock).textContent = `${musicState.current.artist} · ${musicState.current.previewOnly ? 'Preview ±30 detik' : musicState.current.source === 'local' ? 'File lokal penuh' : 'Audius full stream'}`;
      const art = $('#ryoMusicDockArt', musicDock);
      art.src = musicState.current.artwork || '';
      art.hidden = !musicState.current.artwork;
      const nowPlay = $('[data-music-action="now-playing-toggle"]', modalBody);
      if (nowPlay) {
        const isPlaying = !musicAudio.paused;
        nowPlay.setAttribute('aria-label', isPlaying ? 'Jeda' : 'Putar');
        nowPlay.innerHTML = `<svg viewBox="0 0 24 24">${isPlaying ? '<path d="M7 5h4v14H7zM15 5h4v14h-4z"/>' : '<path d="m8 5 11 7-11 7V5Z"/>'}</svg>`;
        const status = $('.ryo-now-footer > span', modalBody);
        if (status) status.innerHTML = `<i></i>${isPlaying ? 'SEDANG DIPUTAR' : 'DIJEDA'}`;
      }
      musicSyncDockProgress();
    }
  }
  function musicSyncDockProgress() {
    if (!musicDock || !musicAudio) return;
    const duration = Number.isFinite(musicAudio.duration) ? musicAudio.duration : (musicState.current?.duration || 0);
    $('#ryoMusicCurrentTime', musicDock).textContent = musicDuration(musicAudio.currentTime || 0);
    $('#ryoMusicDuration', musicDock).textContent = musicDuration(duration);
    const seek = $('#ryoMusicSeek', musicDock);
    if (seek && !seek.matches(':active')) seek.value = duration > 0 ? String(Math.round((musicAudio.currentTime / duration) * 1000)) : '0';
    const nowSeek = $('#ryoMusicNowSeek', modalBody);
    if (nowSeek && !nowSeek.matches(':active')) nowSeek.value = duration > 0 ? String(Math.round((musicAudio.currentTime / duration) * 1000)) : '0';
    const nowCurrent = $('#ryoMusicNowCurrent', modalBody);
    const nowDuration = $('#ryoMusicNowDuration', modalBody);
    if (nowCurrent) nowCurrent.textContent = musicDuration(musicAudio.currentTime || 0);
    if (nowDuration) nowDuration.textContent = musicDuration(duration);
  }

  async function musicPlayTrack(track, options = {}) {
    const isYouTube = track?.source === 'youtube';
    if (!track || !track.playable || (!isYouTube && track.source !== 'local' && !track.streamUrl)) {
      toast('Sumber ini tidak menyediakan pemutaran langsung.', 'error'); return;
    }
    musicEnsureDock();
    if (options.index != null) {
      musicState.currentIndex = options.index;
    } else {
      const key = musicTrackKey(track);
      musicState.queue = [track, ...musicState.queue.filter(item => musicTrackKey(item) !== key)];
      musicState.currentIndex = 0;
    }
    musicState.current = track;
    musicState.history = [{ ...track, playedAt: Date.now() }, ...musicState.history.filter(item => musicTrackKey(item) !== musicTrackKey(track))].slice(0, 80);
    musicPersist();
    if (isYouTube) {
      if (musicObjectUrl) { URL.revokeObjectURL(musicObjectUrl); musicObjectUrl = null; }
      musicAudio.pause();
      musicAudio.removeAttribute('src');
      musicAudio.load();
      // Play the YouTube Music song's videoId in Ryomd's embedded player.
      // Do not hand off to the Android YouTube Music or YouTube application.
      musicShowYoutubeDock(track);
      if (modalTitle.textContent === 'Music Studio V3') renderMusicStudio();
      return;
    }
    let nextUrl = musicSafeUrl(track.streamUrl);
    if (track.source === 'local') {
      try {
        const record = await musicLocalTransaction('readonly', 'get', track.id);
        if (!record?.blob) throw new Error('File tidak ditemukan di penyimpanan lokal.');
        if (musicObjectUrl) URL.revokeObjectURL(musicObjectUrl);
        musicObjectUrl = URL.createObjectURL(record.blob);
        nextUrl = musicObjectUrl;
      } catch (error) { toast(error.message || 'File lokal tidak dapat dibuka.', 'error'); return; }
    } else if (musicObjectUrl) { URL.revokeObjectURL(musicObjectUrl); musicObjectUrl = null; }
    if (!nextUrl) { toast('Alamat stream tidak valid.', 'error'); return; }
    musicSyncDock();
    musicAudio.src = nextUrl;
    musicAudio.load();
    try { await musicAudio.play(); }
    catch { toast('Pemutaran diblokir atau stream tidak tersedia. Tekan Putar sekali lagi atau buka sumber resmi.', 'error'); }
    musicSyncDock();
    if (modalTitle.textContent === 'Music Studio V3') renderMusicStudio();
  }
  function musicAdvance(direction, ended = false) {
    if (!musicState.queue.length) return;
    if (musicState.repeat === 'one' && ended && musicAudio) { musicAudio.currentTime = 0; musicAudio.play().catch(() => {}); return; }
    let next = musicState.currentIndex + direction;
    if (musicState.shuffle && direction > 0 && musicState.queue.length > 1) {
      const options = musicState.queue.map((_, index) => index).filter(index => index !== musicState.currentIndex);
      next = options[Math.floor(Math.random() * options.length)];
    }
    if (next < 0) next = musicState.repeat === 'all' ? musicState.queue.length - 1 : 0;
    if (next >= musicState.queue.length) {
      if (musicState.repeat !== 'all') { if (musicAudio) musicAudio.pause(); return; }
      next = 0;
    }
    musicPlayTrack(musicState.queue[next], { index: next });
  }
  function musicEnqueue(track) {
    if (!track) return;
    const key = musicTrackKey(track);
    if (musicState.queue.some(item => musicTrackKey(item) === key)) { toast('Lagu sudah ada di antrean.'); return; }
    musicState.queue.push(track); musicPersist();
    toast('Ditambahkan ke antrean.', 'success');
    if (modalTitle.textContent === 'Music Studio V3' && musicState.tab === 'queue') renderMusicStudio();
  }
  function musicTrackCard(track, index, listKind = 'result') {
    const favorite = musicIsFavorite(track);
    const sourceLabel = track.source === 'youtube' ? (track.playable === false ? 'TIDAK TERSEDIA DI YT MUSIC' : 'YOUTUBE MUSIC · SONGS') : track.source === 'audius' ? (track.playable ? 'FULL TRACK' : 'BUKA SUMBER') : track.source === 'local' ? 'FILE LOKAL' : 'PREVIEW ±30 DETIK';
    const sourceClass = track.source === 'youtube' ? 'youtube' : track.source === 'audius' ? 'audius' : track.source === 'local' ? 'local' : 'apple';
    const artwork = musicSafeUrl(track.artwork);
    const playlists = musicState.playlists.length ? `<select class="ryo-music-add-select" data-music-playlist-select="${index}" aria-label="Tambah ke playlist"><option value="">+ Playlist</option>${musicState.playlists.map(list => `<option value="${esc(list.id)}">${esc(list.name)}</option>`).join('')}</select>` : '';
    return `<article class="ryo-music-track" data-track-key="${esc(musicTrackKey(track))}">
      <div class="ryo-track-art">${artwork ? `<img src="${esc(artwork)}" alt="Sampul ${esc(track.title)}" loading="lazy" onerror="this.style.display='none'">` : '<span aria-hidden="true">♪</span>'}</div>
      <div class="ryo-track-info"><b>${esc(track.title)}</b><span>${esc(track.artist)}</span><small>${esc(track.genre || 'Music')} · ${track.source === 'apple' ? 'Preview ±30 detik' : musicDuration(track.duration)}</small></div>
      <span class="ryo-track-source ${sourceClass}">${sourceLabel}</span>
      <div class="ryo-track-actions"><button type="button" class="ryo-music-action primary" data-music-action="play-${listKind}" data-index="${index}" ${track.playable ? '' : 'disabled'} aria-label="Putar ${esc(track.title)}">▶</button>${listKind === 'queue' ? '' : `<button type="button" class="ryo-music-action" data-music-action="queue-${listKind}" data-index="${index}" aria-label="Tambah antrean">＋</button>`}<button type="button" class="ryo-music-action ${favorite ? 'is-favorite' : ''}" data-music-action="favorite-${listKind}" data-index="${index}" aria-label="${favorite ? 'Hapus favorit' : 'Tambah favorit'}">${favorite ? '♥' : '♡'}</button>${listKind === 'playlist' ? `<button type="button" class="ryo-music-action danger" data-music-action="remove-playlist-track" data-index="${index}" aria-label="Hapus lagu dari playlist">−</button>` : ''}${playlists}${track.externalUrl && track.source !== 'youtube' ? `<a class="ryo-music-action" href="${esc(musicSafeUrl(track.externalUrl))}" target="_blank" rel="noopener noreferrer" aria-label="Buka sumber lagu">↗</a>` : ''}</div>
    </article>`;
  }
  function musicEmpty(title, detail) { return `<div class="ryo-music-empty"><span aria-hidden="true">♪</span><b>${esc(title)}</b><p>${esc(detail)}</p></div>`; }
  function musicRenderResultList(items, kind = 'result') {
    if (!items.length) return musicEmpty('Belum ada lagu', 'Cari judul atau nama artis untuk menemukan musik.');
    return `<div class="ryo-music-results">${items.map((track, index) => musicTrackCard(track, index, kind)).join('')}</div>`;
  }
  function musicRenderLyrics() {
    const track = musicState.current;
    const key = track ? musicTrackKey(track) : 'none';
    const saved = track ? safeStorage.get(`ryomd_music_v3_lyrics_${key}`, '') : '';
    return `<section class="ryo-music-panel"><div class="ryo-music-panel-head"><div><span class="ryo-music-overline">LYRICS WORKSPACE</span><h3>Lirik &amp; sinkronisasi</h3></div><span class="ryo-music-pill">LRC / TEKS</span></div>${track ? `<p class="ryo-music-muted">Lagu aktif: <b>${esc(track.title)}</b> — ${esc(track.artist)}</p>` : '<p class="ryo-music-muted">Putar lagu terlebih dahulu atau tempel lirik untuk disimpan pada lagu aktif.</p>'}<label class="ryo-music-label" for="ryoMusicLyricsInput">Tempel lirik biasa atau format LRC [mm:ss.xx]</label><textarea id="ryoMusicLyricsInput" class="ryo-music-lyrics-input" placeholder="[00:12.00]Baris lirik pertama\n[00:17.50]Baris lirik berikutnya">${esc(saved)}</textarea><div class="ryo-music-row"><button class="ryo-music-btn violet" type="button" data-music-action="save-lyrics" ${track ? '' : 'disabled'}>Simpan lirik lokal</button><span class="ryo-music-muted">Sinkronisasi otomatis untuk timestamp LRC.</span></div><div id="ryoMusicLyricsLive" class="ryo-music-lyrics-live">${track ? musicLyricsLiveHtml(saved) : 'Belum ada lagu aktif.'}</div></section>`;
  }
  function musicLyricsLiveHtml(text) {
    if (!text) return '<p class="ryo-music-muted">Belum ada lirik tersimpan untuk lagu ini.</p>';
    const lines = String(text).split(/\r?\n/).map(line => line.trim()).filter(Boolean);
    const parsed = lines.map(line => {
      const match = line.match(/^\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)$/);
      if (!match) return { time: null, text: line.replace(/^\[[^\]]+\]/, '') };
      return { time: Number(match[1]) * 60 + Number(match[2]) + Number(`0.${match[3] || '0'}`), text: match[4] };
    }).filter(item => item.text);
    return `<div class="ryo-lyrics-lines">${parsed.map((line, index) => `<p data-lyric-index="${index}" ${line.time == null ? '' : `data-lyric-time="${line.time}"`}>${esc(line.text)}</p>`).join('')}</div>`;
  }
  function musicRenderPlaylists() {
    if (musicState.activePlaylist) {
      const playlist = musicState.playlists.find(item => item.id === musicState.activePlaylist);
      if (!playlist) { musicState.activePlaylist = null; }
      else return `<section class="ryo-music-panel"><div class="ryo-music-panel-head"><div><span class="ryo-music-overline">LOCAL PLAYLIST</span><h3>${esc(playlist.name)}</h3><p>${playlist.tracks.length} lagu tersimpan di perangkat ini.</p></div><button class="ryo-music-btn" type="button" data-music-action="playlist-back">Kembali</button></div><div class="ryo-music-row"><button class="ryo-music-btn violet" type="button" data-music-action="play-playlist">▶ Putar playlist</button></div>${musicRenderResultList(playlist.tracks, 'playlist')}</section>`;
    }
    return `<section class="ryo-music-panel"><div class="ryo-music-panel-head"><div><span class="ryo-music-overline">ON-DEVICE LIBRARY</span><h3>Playlist saya</h3><p>Playlist disimpan secara lokal di browser perangkat.</p></div></div><form id="ryoMusicPlaylistForm" class="ryo-music-create-form"><input id="ryoMusicPlaylistName" maxlength="48" class="ryo-music-input" placeholder="Nama playlist baru" required><button class="ryo-music-btn violet" type="submit">＋ Buat playlist</button></form>${musicState.playlists.length ? `<div class="ryo-music-playlists">${musicState.playlists.map(list => `<article class="ryo-music-playlist"><div class="ryo-playlist-cover">♫</div><div class="ryo-playlist-copy"><b>${esc(list.name)}</b><small>${list.tracks.length} lagu</small></div><button class="ryo-music-btn" type="button" data-music-action="open-playlist" data-playlist-id="${esc(list.id)}">Buka</button><button class="ryo-music-action danger" type="button" data-music-action="delete-playlist" data-playlist-id="${esc(list.id)}" aria-label="Hapus playlist">×</button></article>`).join('')}</div>` : musicEmpty('Playlist belum dibuat', 'Buat playlist pertama untuk mengelompokkan lagu favoritmu.')}</section>`;
  }
  function musicRenderQueue() {
    return `<section class="ryo-music-panel"><div class="ryo-music-panel-head"><div><span class="ryo-music-overline">UP NEXT</span><h3>Antrean pemutaran</h3><p>${musicState.queue.length} lagu · ${musicState.current ? `Sedang diputar: ${esc(musicState.current.title)}` : 'Belum ada lagu aktif'}</p></div><div class="ryo-music-row"><button class="ryo-music-btn" type="button" data-music-action="toggle-shuffle">${musicState.shuffle ? 'Acak: ON' : 'Acak: OFF'}</button><button class="ryo-music-btn" type="button" data-music-action="toggle-repeat">Ulang: ${musicState.repeat.toUpperCase()}</button></div></div>${musicState.queue.length ? `<div class="ryo-music-results">${musicState.queue.map((track, index) => `${musicTrackCard(track, index, 'queue')}<div class="ryo-queue-remove"><span>${index === musicState.currentIndex ? 'SEDANG DIPUTAR' : `URUTAN ${index + 1}`}</span><button type="button" data-music-action="remove-queue" data-index="${index}">Hapus dari antrean</button></div>`).join('')}</div><div class="ryo-music-row"><button class="ryo-music-btn" type="button" data-music-action="clear-queue">Kosongkan antrean</button></div>` : musicEmpty('Antrean kosong', 'Tambahkan lagu dari hasil pencarian untuk mulai mendengarkan.')}</section>`;
  }
  function musicOpenLocalDb() {
    if (musicDbPromise) return musicDbPromise;
    musicDbPromise = new Promise((resolve, reject) => {
      if (!('indexedDB' in window)) { reject(new Error('Penyimpanan lokal perangkat tidak didukung browser ini.')); return; }
      const request = indexedDB.open('ryomd_music_studio_v3', 1);
      request.onupgradeneeded = () => { const db = request.result; if (!db.objectStoreNames.contains('audio')) db.createObjectStore('audio', { keyPath: 'id' }); };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error || new Error('Database lokal gagal dibuka.'));
    });
    return musicDbPromise;
  }
  async function musicLocalTransaction(mode, action, recordId, record) {
    const db = await musicOpenLocalDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction('audio', mode);
      const store = tx.objectStore('audio');
      const req = action === 'all' ? store.getAll() : action === 'get' ? store.get(recordId) : action === 'delete' ? store.delete(recordId) : store.put(record);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error || new Error('Operasi file lokal gagal.'));
      tx.onerror = () => reject(tx.error || new Error('Transaksi penyimpanan gagal.'));
    });
  }
  async function musicLoadLocalTracks() {
    try {
      const records = await musicLocalTransaction('readonly', 'all');
      musicState.localTracks = records.map(item => ({ id: item.id, key: `local:${item.id}`, source: 'local', title: item.title, artist: item.artist || 'File di perangkat', artwork: '', duration: Number(item.duration) || 0, genre: item.mime || 'Audio lokal', streamUrl: '', playable: true, externalUrl: '', previewOnly: false, size: item.size || 0 }));
    } catch (error) { musicState.error = error.message || 'Library lokal tidak dapat dibuka.'; }
  }
  async function musicImportLocalFiles(fileList) {
    const files = [...(fileList || [])];
    if (!files.length) return;
    const valid = files.filter(file => file.type.startsWith('audio/') || /\.(mp3|m4a|aac|wav|ogg|opus|flac)$/i.test(file.name));
    if (!valid.length) { toast('Pilih file audio seperti MP3, M4A, WAV, OGG, atau FLAC.', 'error'); return; }
    const maxFile = 150 * 1024 * 1024;
    try {
      for (const file of valid) {
        if (file.size > maxFile) { toast(`${file.name}: ukuran melebihi 150 MB, dilewati.`, 'error'); continue; }
        const id = `local-${Date.now()}-${Math.random().toString(36).slice(2,8)}`;
        const title = file.name.replace(/\.[^.]+$/, '');
        await musicLocalTransaction('readwrite', 'put', null, { id, title, artist: 'File di perangkat', mime: file.type || 'audio/*', size: file.size, addedAt: Date.now(), blob: file });
      }
      await musicLoadLocalTracks();
      if (modalTitle.textContent === 'Music Studio V3' && musicState.tab === 'local') renderMusicStudio();
      toast(`${valid.length} file audio diproses ke library lokal.`, 'success');
    } catch (error) { toast(`Gagal menyimpan audio lokal: ${error.message || 'Penyimpanan penuh atau tidak diizinkan.'}`, 'error'); }
  }
  function musicRenderLocalLibrary() {
    return `<section class="ryo-music-panel"><div class="ryo-music-panel-head"><div><span class="ryo-music-overline">DEVICE LIBRARY</span><h3>Musik di perangkat</h3><p>Audio disimpan di IndexedDB browser; tidak diunggah ke server.</p></div><span class="ryo-music-pill">${musicState.localTracks.length} FILE</span></div><label class="ryo-music-import" for="ryoMusicLocalFiles"><span class="ryo-import-symbol">＋</span><span><b>Tambahkan file audio</b><small>MP3 · M4A · AAC · WAV · OGG · OPUS · FLAC (maks. 150 MB/file)</small></span></label><input id="ryoMusicLocalFiles" type="file" accept="audio/*,.mp3,.m4a,.aac,.wav,.ogg,.opus,.flac" multiple hidden>${musicState.localTracks.length ? `<div class="ryo-music-results">${musicState.localTracks.map((track,index) => `${musicTrackCard(track,index,'local')}<div class="ryo-queue-remove"><span>${(track.size / 1048576).toFixed(1)} MB · TERSIMPAN DI PERANGKAT</span><button type="button" data-music-action="delete-local" data-index="${index}">Hapus file</button></div>`).join('')}</div>` : musicEmpty('Library lokal masih kosong', 'Pilih file audio dari penyimpanan Android. File tetap berada di penyimpanan browser pada perangkat ini.')}</section>`;
  }
  function musicRenderNowPlaying() {
    const track = musicState.current;
    if (!track) return `<section class=\"ryo-now-empty\"><span class=\"ryo-now-empty-icon\">♫</span><h3>Belum ada lagu diputar</h3><p>Pilih track dari Jelajahi, Favorit, Playlist, atau library perangkat untuk membuka pemutar penuh.</p><button type=\"button\" class=\"ryo-music-btn violet\" data-music-action=\"tab\" data-tab=\"discover\">Jelajahi musik</button></section>`;
    if (track.source === 'youtube') {
      const artwork = musicSafeUrl(track.artwork);
      const favorite = musicIsFavorite(track);
      const dockVisible = Boolean(musicYoutubeDock && !musicYoutubeDock.hidden && !musicYoutubeDismissed);
      return `<section class="ryo-now-playing ryo-now-youtube ryo-now-ytmusic">
        <header class="ryo-now-topbar"><button type="button" class="ryo-now-back" data-music-action="tab" data-tab="discover" aria-label="Kembali ke Jelajahi">←</button><div><span class="ryo-music-overline">YOUTUBE MUSIC CATALOG</span><b>PEMUTAR DI PANEL RYOMD</b></div><button type="button" class="ryo-now-queue-open" data-music-action="now-playing-queue" aria-label="Buka antrean">☷</button></header>
        <div class="ryo-now-youtube-card">${artwork ? `<img src="${esc(artwork)}" alt="">` : '<span>♫</span>'}<div><small>YOUTUBE MUSIC · SONGS</small><h2>${esc(track.title)}</h2><p>${esc(track.artist)}</p>${track.album ? `<small>${esc(track.album)}</small>` : ''}</div><button type="button" class="ryo-now-favorite ${favorite ? 'active' : ''}" data-music-action="now-playing-favorite" aria-label="${favorite ? 'Hapus dari favorit' : 'Tambahkan ke favorit'}">${favorite ? '♥' : '♡'}</button></div>
        <div class="ryo-music-ytm-handoff"><strong>${dockVisible ? 'Lagu sedang dibuka di pemutar mengambang Ryomd' : 'Pemutar berada di panel mengambang Ryomd'}</strong><p>Kontrol play/pause dan pencarian waktu tersedia pada pemutar YouTube tertanam. Panel dapat digeser dari bagian judul.</p><div class="ryo-music-row"><button class="ryo-music-btn violet" type="button" data-music-action="reopen-youtube">${dockVisible ? 'Tampilkan pemutar' : 'Buka pemutar di panel'}</button></div></div>
        <div class="ryo-music-row"><button class="ryo-music-btn" type="button" data-music-action="now-playing-previous">Sebelumnya</button><button class="ryo-music-btn" type="button" data-music-action="now-playing-next">Berikutnya</button><button class="ryo-music-btn" type="button" data-music-action="now-playing-queue">Antrean (${musicState.queue.length})</button></div>
        <p class="ryo-music-legal">Pencarian memakai katalog YouTube Music, sedangkan pemutaran di panel memakai YouTube Embedded Player resmi. Video yang dihapus, privat, dibatasi usia, atau tidak diizinkan untuk embed tetap tidak dapat diputar di panel.</p>
      </section>`;
    }
    const artwork = musicSafeUrl(track.artwork);
    const source = track.previewOnly ? 'APPLE PREVIEW · ±30 DETIK' : track.source === 'local' ? 'LOCAL LIBRARY · FILE PENUH' : 'AUDIUS · FULL TRACK STREAM';
    const duration = Number.isFinite(musicAudio?.duration) && musicAudio.duration > 0 ? musicAudio.duration : (track.duration || 0);
    const currentTime = musicAudio?.currentTime || 0;
    const progress = duration > 0 ? Math.min(1000, Math.round(currentTime / duration * 1000)) : 0;
    const playing = Boolean(musicAudio && !musicAudio.paused);
    const favorite = musicIsFavorite(track);
    return `<section class=\"ryo-now-playing\">
      <header class=\"ryo-now-topbar\"><button type=\"button\" class=\"ryo-now-back\" data-music-action=\"tab\" data-tab=\"discover\" aria-label=\"Kembali ke Jelajahi\"><svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"m15 18-6-6 6-6\"/></svg></button><div><span class=\"ryo-music-overline\">NOW PLAYING</span><b>RYOMD MUSIC STUDIO</b></div><button type=\"button\" class=\"ryo-now-queue-open\" data-music-action=\"now-playing-queue\" aria-label=\"Buka antrean\"><svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M4 6h16M4 11h16M4 16h10M17 15l3 3-3 3\"/></svg></button></header>
      <div class=\"ryo-now-stage\">
        <div class=\"ryo-now-art-wrap\"><div class=\"ryo-now-art-glow\"></div>${artwork ? `<img class=\"ryo-now-art\" src=\"${esc(artwork)}\" alt=\"Sampul ${esc(track.title)}\" onerror=\"this.style.visibility='hidden'\">` : `<div class=\"ryo-now-art ryo-now-art-fallback\"><span>♫</span><small>RYOMD MUSIC</small></div>`}<div class=\"ryo-now-equalizer\" aria-hidden=\"true\"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div></div>
        <div class=\"ryo-now-meta\"><div class=\"ryo-now-track-copy\"><span class=\"ryo-now-source\">${source}</span><h2>${esc(track.title)}</h2><p>${esc(track.artist)}</p>${track.genre ? `<small>${esc(track.genre)}</small>` : ''}</div><button type=\"button\" class=\"ryo-now-favorite ${favorite ? 'active' : ''}\" data-music-action=\"now-playing-favorite\" aria-label=\"${favorite ? 'Hapus dari favorit' : 'Tambahkan ke favorit'}\">${favorite ? '♥' : '♡'}</button></div>
        <div class=\"ryo-now-progress\"><input id=\"ryoMusicNowSeek\" type=\"range\" min=\"0\" max=\"1000\" step=\"1\" value=\"${progress}\" aria-label=\"Posisi pemutaran lagu\"><div><time id=\"ryoMusicNowCurrent\">${musicDuration(currentTime)}</time><time id=\"ryoMusicNowDuration\">${musicDuration(duration)}</time></div></div>
        <div class=\"ryo-now-controls\"><button type=\"button\" class=\"ryo-now-mode ${musicState.shuffle ? 'active' : ''}\" data-music-action=\"toggle-shuffle\" aria-label=\"Acak ${musicState.shuffle ? 'aktif' : 'nonaktif'}\"><svg viewBox=\"0 0 24 24\"><path d=\"M16 3h5v5M4 20 21 3M21 16v5h-5M15 15l6 6M4 4l5 5\"/></svg></button><button type=\"button\" class=\"ryo-now-skip\" data-music-action=\"now-playing-previous\" aria-label=\"Lagu sebelumnya\"><svg viewBox=\"0 0 24 24\"><path d=\"M6 5v14M19 6 8 12l11 6V6Z\"/></svg></button><button type=\"button\" class=\"ryo-now-play\" data-music-action=\"now-playing-toggle\" aria-label=\"${playing ? 'Jeda' : 'Putar'}\"><svg viewBox=\"0 0 24 24\">${playing ? '<path d=\"M7 5h4v14H7zM15 5h4v14h-4z\"/>' : '<path d=\"m8 5 11 7-11 7V5Z\"/>'}</svg></button><button type=\"button\" class=\"ryo-now-skip\" data-music-action=\"now-playing-next\" aria-label=\"Lagu berikutnya\"><svg viewBox=\"0 0 24 24\"><path d=\"M18 5v14M5 6l11 6-11 6V6Z\"/></svg></button><button type=\"button\" class=\"ryo-now-mode ${musicState.repeat !== 'off' ? 'active' : ''}\" data-music-action=\"toggle-repeat\" aria-label=\"Ulangi: ${musicState.repeat}\"><svg viewBox=\"0 0 24 24\"><path d=\"m17 2 4 4-4 4M3 11V9a3 3 0 0 1 3-3h15M7 22l-4-4 4-4M21 13v2a3 3 0 0 1-3 3H3\"/>${musicState.repeat === 'one' ? '<path d=\"M12 9v6m-2-4 2-2 2 2\"/>' : ''}</svg></button></div>
        <footer class=\"ryo-now-footer\"><span><i></i>${playing ? 'SEDANG DIPUTAR' : 'SIAP DIPUTAR'}</span><button type=\"button\" data-music-action=\"now-playing-lyrics\">Lirik</button><button type=\"button\" data-music-action=\"now-playing-queue\">Antrean (${musicState.queue.length})</button></footer>
      </div>
      <div class=\"ryo-now-note\">${track.previewOnly ? 'Sumber ini hanya menyediakan cuplikan sekitar 30 detik. Untuk pemutaran penuh, cari versi yang dapat di-stream di Audius atau gunakan file audio milikmu.' : track.source === 'local' ? 'File audio diputar langsung dari library lokal perangkat.' : 'Durasi penuh mengikuti stream yang diizinkan oleh pemilik track di Audius.'}</div>
    </section>`;
  }

  function renderMusicStudio() {
    musicEnsureDock();
    const tabs = [['discover','Jelajahi'],['favorites','Favorit'],['playlists','Playlist'],['queue','Antrean'],['history','Riwayat'],['local','Perangkat'],['lyrics','Lirik'],...(musicState.current ? [['nowplaying','Diputar']] : [])];
    let content = '';
    if (musicState.tab === 'favorites') content = `<section class="ryo-music-panel"><div class="ryo-music-panel-head"><div><span class="ryo-music-overline">SAVED TRACKS</span><h3>Favorit</h3><p>${musicState.favorites.length} lagu tersimpan di browser.</p></div></div>${musicRenderResultList(musicState.favorites, 'favorite')}</section>`;
    else if (musicState.tab === 'playlists') content = musicRenderPlaylists();
    else if (musicState.tab === 'queue') content = musicRenderQueue();
    else if (musicState.tab === 'history') content = `<section class="ryo-music-panel"><div class="ryo-music-panel-head"><div><span class="ryo-music-overline">RECENTLY PLAYED</span><h3>Riwayat pemutaran</h3><p>Disimpan lokal hingga 80 lagu.</p></div><button class="ryo-music-btn" type="button" data-music-action="clear-history">Hapus riwayat</button></div>${musicRenderResultList(musicState.history, 'history')}</section>`;
    else if (musicState.tab === 'local') content = musicRenderLocalLibrary();
    else if (musicState.tab === 'lyrics') content = musicRenderLyrics();
    else if (musicState.tab === 'nowplaying') content = musicRenderNowPlaying();
    else {
      const sourceButtons = [['youtube','YouTube Music · Default'],['all','Semua sumber'],['audius','Full track · Audius'],['apple','Preview · Apple']];
      const genres = ['Pop','Rock','J-Pop','K-Pop','Electronic','Hip-Hop','Jazz','Classical','Soundtrack','Indie'];
      content = `<section class="ryo-music-hero ryo-music-hero-ytm"><div><span class="ryo-music-overline">RYOMD MUSIC STUDIO / V3.6</span><h3>Cari lagu di YouTube Music.</h3><p>Pencarian memakai katalog Songs YouTube Music. Tombol Putar menjalankan hasil di pemutar tertanam dalam panel Ryomd.</p><span class="ryo-music-native-badge"><i></i> YOUTUBE MUSIC · IN-PANEL PLAYER</span></div><span class="ryo-music-orbit" aria-hidden="true">♫</span></section>
        <form class="ryo-music-search" id="ryoMusicSearchForm"><label class="sr-only" for="ryoMusicSearchInput">Cari judul lagu atau artis</label><input id="ryoMusicSearchInput" class="ryo-music-input" value="${esc(musicState.query)}" maxlength="120" placeholder="Cari judul lagu, artis, atau genre…" autocomplete="off"><button class="ryo-music-btn violet" type="submit" ${musicState.loading ? 'disabled' : ''}>${musicState.loading ? 'Mencari…' : 'Cari musik'}</button></form>
        <div class="ryo-music-source-tabs" role="group" aria-label="Sumber katalog">${sourceButtons.map(([value,label]) => `<button type="button" class="${musicState.provider === value ? 'active' : ''}" data-music-action="provider" data-provider="${value}" aria-pressed="${musicState.provider === value}">${label}</button>`).join('')}</div>
        <div class="ryo-music-genres" aria-label="Genre populer">${genres.map(genre => `<button type="button" data-music-action="genre" data-genre="${genre}">${genre}</button>`).join('')}</div>
        <section class="ryo-music-panel"><div class="ryo-music-panel-head"><div><span class="ryo-music-overline">${musicState.query ? 'SEARCH RESULTS' : 'DISCOVER'}</span><h3>${musicState.query ? `Hasil untuk “${esc(musicState.query)}”` : 'Trending & pilihan'}</h3><p>${musicState.loading ? 'Menghubungi katalog musik…' : 'Hasil disaring berdasarkan kecocokan judul dan artis.'}</p></div><button class="ryo-music-btn" type="button" data-music-action="refresh-discover">Muat ulang</button></div>${musicState.error ? `<div class="ryo-music-error">${esc(musicState.error)}${musicState.query ? ` <button type="button" class="ryo-music-btn" data-music-action="youtube-empty-search">Cari “${esc(musicState.query)}” di YouTube Music</button>` : ''}</div>` : ''}${musicState.notice ? `<div class="ryo-music-match-notice">${esc(musicState.notice)} <button type="button" data-music-action="youtube-empty-search">Cari di YouTube Music</button></div>` : ''}${musicRenderResultList(musicState.results, 'result')}</section>
        <p class="ryo-music-legal">YouTube Music: katalog Songs untuk pencarian, YouTube Embedded Player resmi untuk pemutaran di panel. Video yang tidak diizinkan untuk embed tetap tidak dapat diputar.</p>`;
    }
    modalBody.innerHTML = `<div class="ryo-music-app"><nav class="ryo-music-main-tabs" aria-label="Navigasi Music Studio">${tabs.map(([id,label]) => `<button type="button" data-music-action="tab" data-tab="${id}" class="${musicState.tab === id ? 'active' : ''}" aria-pressed="${musicState.tab === id}">${label}</button>`).join('')}</nav>${content}</div>`;
    musicSyncDock();
    if (musicState.tab === 'lyrics') { musicLyricActiveIndex = -1; musicUpdateLyricsDisplay(); }
  }
  async function musicLoadCatalog(query, trending = false) {
    musicState.loading = true; musicState.error = ''; musicState.notice = ''; musicState.query = query || '';
    renderMusicStudio();
    if (musicState.provider === 'youtube' && !query) {
      musicState.loading = false;
      musicState.error = 'Masukkan judul lagu atau nama artis untuk mencari di YouTube Music.';
      renderMusicStudio();
      return;
    }
    const tasks = [];
    if (musicState.provider === 'all' || musicState.provider === 'audius') tasks.push(musicSearchAudius(query, trending).then(items => ({ provider: 'audius', items })).catch(error => ({ provider: 'audius', error })));
    if (!trending && query && (musicState.provider === 'all' || musicState.provider === 'apple')) tasks.push(musicSearchApple(query).then(items => ({ provider: 'apple', items })).catch(error => ({ provider: 'apple', error })));
    if (!trending && query && (musicState.provider === 'all' || musicState.provider === 'youtube')) tasks.push(musicSearchYouTube(query).then(items => ({ provider: 'youtube', items })).catch(error => ({ provider: 'youtube', error })));
    const responses = await Promise.all(tasks);
    const tracks = responses.flatMap(result => result.items || []);
    tracks.sort((a, b) => { const scoreDiff = (b.matchScore || 0) - (a.matchScore || 0); if (scoreDiff) return scoreDiff; const rank = item => item.source === 'audius' ? 0 : item.source === 'apple' ? 1 : 2; return rank(a) - rank(b); });
    musicState.results = tracks;
    const failures = responses.filter(result => result.error);
    const fullTracks = tracks.filter(track => track.source === 'audius' && track.playable);
    const previews = tracks.filter(track => track.source === 'apple');
    musicState.loading = false;
    if (!tracks.length) musicState.error = failures.length ? failures.map(item => item.error?.message || `Sumber ${item.provider} tidak merespons.`).join(' ') : 'Tidak ditemukan lagu pada katalog ini. Coba kata kunci judul dan artis yang lebih lengkap.';
    else if (failures.length) musicState.error = `Sebagian sumber tidak merespons: ${failures.map(item => item.provider).join(', ')}. Hasil dari sumber lain tetap ditampilkan.`;
    const youtubeTracks = tracks.filter(track => track.source === 'youtube');
    if (query && youtubeTracks.length) musicState.notice = 'Hasil berasal dari katalog Songs YouTube Music. Tekan Putar untuk memuat lagu di panel pemutar Ryomd.';
    else if (query && !fullTracks.length && previews.length) musicState.notice = 'Hasil yang cocok hanya menyediakan preview Apple sekitar 30 detik. Coba pilih sumber YouTube Music untuk mencari video lagu melalui player resmi.';
    else if (query && !fullTracks.length && musicState.provider !== 'apple' && musicState.provider !== 'youtube') musicState.notice = 'Belum ditemukan versi Audius full-stream yang cocok. Coba ulang pencarian pada katalog Songs YouTube Music.';
    if (modalTitle.textContent === 'Music Studio V3' && musicState.tab === 'discover') renderMusicStudio();
  }
  let musicLyricActiveIndex = -1;
  function musicUpdateLyricsDisplay() {
    const live = $('#ryoMusicLyricsLive', modalBody);
    if (!live || !musicState.current) return;
    const lines = $$('.ryo-lyrics-lines p[data-lyric-time]', live);
    if (!lines.length) return;
    const time = musicAudio?.currentTime || 0;
    let activeIndex = -1;
    for (let index = 0; index < lines.length; index++) {
      if (Number(lines[index].dataset.lyricTime) <= time) activeIndex = index;
      else break;
    }
    if (activeIndex === musicLyricActiveIndex) return;
    musicLyricActiveIndex = activeIndex;
    lines.forEach((line, index) => line.classList.toggle('active', index === activeIndex));
    if (activeIndex >= 0) lines[activeIndex].scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }
  modalBody.addEventListener('click', event => {
    const control = event.target.closest('[data-music-action]');
    if (!control) return;
    const action = control.dataset.musicAction;
    const index = Number(control.dataset.index);
    if (action === 'now-playing-toggle') {
      if (musicState.current?.source === 'youtube') { musicShowYoutubeDock(musicState.current); }
      else if (musicAudio?.src) { if (musicAudio.paused) musicAudio.play().catch(() => toast('Browser tidak dapat memulai pemutaran.', 'error')); else musicAudio.pause(); }
      musicSyncDock(); renderMusicStudio();
    } else if (action === 'now-playing-previous') { musicAdvance(-1); renderMusicStudio(); }
    else if (action === 'now-playing-next') { musicAdvance(1); renderMusicStudio(); }
    else if (action === 'now-playing-favorite') { if (musicState.current) musicToggleFavorite(musicState.current); renderMusicStudio(); }
    else if (action === 'now-playing-queue') { musicState.tab = 'queue'; renderMusicStudio(); }
    else if (action === 'now-playing-lyrics') { musicState.tab = 'lyrics'; renderMusicStudio(); }
    else if (action === 'tab') { musicState.tab = control.dataset.tab; musicState.activePlaylist = null; renderMusicStudio(); if (musicState.tab === 'local') musicLoadLocalTracks().then(() => { if (modalTitle.textContent === 'Music Studio V3' && musicState.tab === 'local') renderMusicStudio(); }); }
    else if (action === 'provider') { musicState.provider = control.dataset.provider; musicLoadCatalog(musicState.query, !musicState.query); }
    else if (action === 'genre') { musicState.tab = 'discover'; musicState.query = control.dataset.genre; musicLoadCatalog(musicState.query, false); }
    else if (action === 'refresh-discover') musicLoadCatalog(musicState.query, !musicState.query);
    else if (action.startsWith('play-')) {
      const kind = action.slice(5);
      const list = kind === 'result' ? musicState.results : kind === 'favorite' ? musicState.favorites : kind === 'history' ? musicState.history : kind === 'playlist' ? (musicState.playlists.find(item => item.id === musicState.activePlaylist)?.tracks || []) : kind === 'local' ? musicState.localTracks : musicState.queue;
      if (kind === 'queue') musicPlayTrack(list[index], { index });
      else if (kind === 'playlist') { musicState.queue = list.slice(); musicState.currentIndex = index; musicPersist(); musicPlayTrack(list[index], { index }); }
      else musicPlayTrack(list[index]);
    } else if (action.startsWith('queue-')) {
      const kind = action.slice(6);
      const list = kind === 'result' ? musicState.results : kind === 'favorite' ? musicState.favorites : kind === 'history' ? musicState.history : kind === 'playlist' ? (musicState.playlists.find(item => item.id === musicState.activePlaylist)?.tracks || []) : kind === 'local' ? musicState.localTracks : [];
      musicEnqueue(list[index]);
    } else if (action.startsWith('favorite-')) {
      const kind = action.slice(9);
      const list = kind === 'result' ? musicState.results : kind === 'favorite' ? musicState.favorites : kind === 'history' ? musicState.history : kind === 'playlist' ? (musicState.playlists.find(item => item.id === musicState.activePlaylist)?.tracks || []) : kind === 'local' ? musicState.localTracks : musicState.queue;
      musicToggleFavorite(list[index]); renderMusicStudio();
    } else if (action === 'open-playlist') { musicState.activePlaylist = control.dataset.playlistId; renderMusicStudio(); }
    else if (action === 'playlist-back') { musicState.activePlaylist = null; renderMusicStudio(); }
    else if (action === 'remove-playlist-track') { const list = musicState.playlists.find(item => item.id === musicState.activePlaylist); if (list) { list.tracks.splice(index, 1); musicPersist(); renderMusicStudio(); } }
    else if (action === 'youtube-empty-search') { if (musicState.query) { musicState.provider = 'youtube'; musicState.tab = 'discover'; musicLoadCatalog(musicState.query, false); } }
    else if (action === 'reopen-youtube') { if (musicState.current?.source === 'youtube') musicShowYoutubeDock(musicState.current); }
    else if (action === 'delete-local') {
      const track = musicState.localTracks[index];
      if (track) musicLocalTransaction('readwrite', 'delete', track.id).then(async () => { const key = musicTrackKey(track); musicState.favorites = musicState.favorites.filter(item => musicTrackKey(item) !== key); musicState.history = musicState.history.filter(item => musicTrackKey(item) !== key); musicState.queue = musicState.queue.filter(item => musicTrackKey(item) !== key); musicState.playlists.forEach(list => { list.tracks = list.tracks.filter(item => musicTrackKey(item) !== key); }); if (musicState.current && musicTrackKey(musicState.current) === key) { musicAudio?.pause(); musicState.current = null; if (musicObjectUrl) URL.revokeObjectURL(musicObjectUrl); musicObjectUrl = null; musicSyncDock(); } musicPersist(); await musicLoadLocalTracks(); renderMusicStudio(); toast('File dihapus dari library lokal.', 'success'); }).catch(error => toast(error.message || 'Tidak dapat menghapus file.', 'error'));
    } else if (action === 'delete-playlist') {
      const list = musicState.playlists.find(item => item.id === control.dataset.playlistId);
      musicState.playlists = musicState.playlists.filter(item => item.id !== control.dataset.playlistId);
      if (musicState.activePlaylist === control.dataset.playlistId) musicState.activePlaylist = null;
      musicPersist(); toast(`Playlist ${list?.name || ''} dihapus.`, 'success'); renderMusicStudio();
    } else if (action === 'play-playlist') {
      const list = musicState.playlists.find(item => item.id === musicState.activePlaylist);
      if (list?.tracks.length) { musicState.queue = list.tracks.slice(); musicState.currentIndex = 0; musicPersist(); musicPlayTrack(list.tracks[0], { index: 0 }); }
    } else if (action === 'remove-queue') {
      musicState.queue.splice(index, 1);
      if (index < musicState.currentIndex) musicState.currentIndex--;
      else if (index === musicState.currentIndex) { musicState.currentIndex = Math.min(index, musicState.queue.length - 1); if (musicState.currentIndex >= 0) musicPlayTrack(musicState.queue[musicState.currentIndex], { index: musicState.currentIndex }); else { musicState.current = null; musicAudio?.pause(); musicSyncDock(); } }
      musicPersist(); renderMusicStudio();
    } else if (action === 'clear-queue') { musicState.queue = []; musicState.currentIndex = -1; musicState.current = null; musicAudio?.pause(); if (musicAudio) musicAudio.removeAttribute('src'); musicSyncDock(); musicPersist(); renderMusicStudio(); }
    else if (action === 'toggle-shuffle') { musicState.shuffle = !musicState.shuffle; renderMusicStudio(); }
    else if (action === 'toggle-repeat') { musicState.repeat = musicState.repeat === 'off' ? 'all' : musicState.repeat === 'all' ? 'one' : 'off'; renderMusicStudio(); }
    else if (action === 'clear-history') { musicState.history = []; musicPersist(); renderMusicStudio(); }
    else if (action === 'save-lyrics') {
      if (!musicState.current) return;
      const input = $('#ryoMusicLyricsInput', modalBody);
      safeStorage.set(`ryomd_music_v3_lyrics_${musicTrackKey(musicState.current)}`, input?.value || '');
      musicUpdateLyricsDisplay(); toast('Lirik disimpan di perangkat.', 'success');
    }
  });
  modalBody.addEventListener('submit', event => {
    if (event.target.id === 'ryoMusicSearchForm') {
      event.preventDefault();
      const input = $('#ryoMusicSearchInput', modalBody);
      const query = String(input?.value || '').trim();
      if (!query) { toast('Masukkan judul lagu, artis, atau genre.'); return; }
      musicLoadCatalog(query, false);
    } else if (event.target.id === 'ryoMusicPlaylistForm') {
      event.preventDefault();
      const name = String($('#ryoMusicPlaylistName', modalBody)?.value || '').trim();
      if (!name) return;
      if (musicState.playlists.some(item => item.name.toLowerCase() === name.toLowerCase())) { toast('Nama playlist sudah digunakan.', 'error'); return; }
      const playlist = { id: `playlist-${Date.now()}-${Math.random().toString(36).slice(2,7)}`, name, tracks: [], createdAt: Date.now() };
      musicState.playlists.unshift(playlist); musicPersist(); renderMusicStudio(); toast('Playlist berhasil dibuat.', 'success');
    }
  });
  modalBody.addEventListener('input', event => {
    if (event.target.id === 'ryoMusicNowSeek' && Number.isFinite(musicAudio?.duration) && musicAudio.duration > 0) {
      musicAudio.currentTime = (Number(event.target.value) / 1000) * musicAudio.duration;
      musicSyncDockProgress();
    }
  });
  modalBody.addEventListener('change', event => {
    if (event.target.id === 'ryoMusicLocalFiles') { musicImportLocalFiles(event.target.files); event.target.value = ''; return; }
    const select = event.target.closest('[data-music-playlist-select]');
    if (!select || !select.value) return;
    const index = Number(select.dataset.musicPlaylistSelect);
    const track = musicState.tab === 'discover' ? musicState.results[index] : musicState.tab === 'favorites' ? musicState.favorites[index] : musicState.tab === 'history' ? musicState.history[index] : musicState.tab === 'playlists' ? (musicState.playlists.find(item => item.id === musicState.activePlaylist)?.tracks || [])[index] : musicState.tab === 'local' ? musicState.localTracks[index] : musicState.queue[index];
    const playlist = musicState.playlists.find(item => item.id === select.value);
    if (playlist && track) {
      if (!playlist.tracks.some(item => musicTrackKey(item) === musicTrackKey(track))) { playlist.tracks.push(track); musicPersist(); toast(`Ditambahkan ke ${playlist.name}.`, 'success'); }
      else toast('Lagu sudah ada di playlist.');
    }
    select.value = '';
  });

  async function openTool(toolId) {
    const tool = TOOLS.find(item => item.id === toolId);
    if (!tool && toolId !== 'about') return;
    const requestId = ++state.modalRequest;
    stopTelemetryRefresh();
    if (toolId === 'about') {
      modalIcon.innerHTML = iconSvg('spark'); modalTitle.textContent = 'Tentang Ryomd Overdrive'; modalSub.textContent = 'Control panel untuk workspace lokal';
      modalBody.innerHTML = `<div class="tool-card-panel"><h4>Ryomd Overdrive · v2.2</h4><p>Dashboard lokal untuk mengelola layanan, utilitas kreatif, downloader, pembaca komik, dan paket pembaruan. Backend dijalankan oleh Node.js melalui <code>server.js</code>.</p></div><p class="tool-note">Tool yang bergantung pada source bot atau paket sistem hanya dapat berjalan bila file dan dependensi yang diperlukan sudah tersedia pada perangkat host.</p>`;
    } else {
      modalIcon.innerHTML = iconSvg(tool.icon); modalTitle.textContent = tool.title; modalSub.textContent = tool.desc;
      modalBody.innerHTML = '<p class="tool-intro">Memuat modul…</p>';
    }
    backdrop.classList.add('open'); backdrop.setAttribute('aria-hidden', 'false'); document.body.style.overflow = 'hidden';
    if (!tool) return;
    try {
      switch (toolId) {
        case 'wa-bot': case 'all-services': await renderServices(toolId); break;
        case 'zip-deploy': renderZipDeploy(); break;
        case 'app-update': renderAppUpdate(); break;
        case 'env-config': renderEnvConfig(); break;
        case 'terminal-shell': renderTerminal(); break;
        case 'file-explorer': renderFileExplorer(); break;
        case 'pkg-deps': await renderDependencies(); break;
        case 'system-logs': await renderLogs(); break;
        case 'sys-telemetry': await renderTelemetry(); break;
        case 'banner-studio': renderBannerStudio(); break;
        case 'social-dl': case 'yt-audio': renderDownloader(toolId); break;
        case 'yt-player': await renderYouTube(); break;
        case 'music-studio': musicState.tab = musicState.tab || 'discover'; renderMusicStudio(); if (!musicState.results.length) musicLoadCatalog('', true); break;
        case 'comic-reader': modalBody.innerHTML = '<iframe title="Ryomd Online Comic Reader" src="/comic-reader.html?embedded=1" loading="lazy" style="display:block;width:100%;height:min(78vh,900px);min-height:560px;border:0;border-radius:10px;background:#0a101a"></iframe>'; break;
        case 'anime-hub': renderAnimeHub(); break;
        case 'qr-gen': renderQr(); break;
        case 'quote-maker': renderQuoteMaker(); decorateQuoteMaker(); break;
        case 'pass-gen': renderPassword(); break;
        case 'avatar-card': renderAvatarMaker(); break;
        case 'html-preview': renderHtmlPreview(); break;
        case 'module-reg': await renderModuleRegistry(); break;
        case 'ryo-ai': await renderAssistant(); break;
        case 'bmkg-cuaca': renderWeather(); break;
        case 'crypto-scan': await renderCrypto(); break;
        case 'ip-intel': await renderIpIntel(); break;
        case 'web-encrypt': renderTextEncoder(); break;
        case 'morse-code': renderMorse(); break;
        case 'foto-link': renderImageDataUrl(); break;
        default: modalBody.innerHTML = '<p class="tool-intro">Tool belum tersedia.</p>';
      }
    } catch (error) {
      if (requestId === state.modalRequest) modalBody.innerHTML = `<div class="tool-card-panel"><h4>Modul tidak dapat dimuat</h4><p>${esc(error.message || 'Terjadi kesalahan.')}</p></div>`;
    }
  }
  function renderQr() {
    let previewUrl = null;
    modalBody.innerHTML = `
      <div class="qrx-shell">
        <section class="qrx-hero">
          <div class="qrx-hero-icon" aria-hidden="true">▦</div>
          <div class="qrx-hero-copy">
            <span class="qrx-eyebrow">RYOMD · CREATOR TOOL</span>
            <h3>QR Code Studio</h3>
            <p>Buat QR untuk teks, nomor, tautan, atau tautan berbagi foto dan video.</p>
          </div>
          <span class="qrx-live"><i></i> READY</span>
        </section>

        <div class="qrx-layout">
          <section class="qrx-panel qrx-input-panel">
            <div class="qrx-panel-heading">
              <span class="qrx-step">01</span>
              <div><h4>Konten QR</h4><p>Tentukan data yang akan dibaca saat QR dipindai.</p></div>
            </div>

            <div class="qrx-field">
              <label for="qrTextInp">Teks, nomor, atau URL</label>
              <textarea class="tool-textarea qrx-textarea" id="qrTextInp" rows="3" maxlength="2500" placeholder="Tulis teks, nomor WhatsApp, atau tautan…">https://wa.me/</textarea>
              <small>Contoh: https://wa.me/628123456789 atau teks biasa.</small>
            </div>

            <div class="qrx-separator"><span>ATAU GUNAKAN MEDIA</span></div>

            <div class="qrx-field">
              <label for="qrMediaInp">Pilih foto atau video</label>
              <label class="qrx-file-picker" for="qrMediaInp">
                <span class="qrx-file-symbol">＋</span>
                <span><strong>Pilih file dari perangkat</strong><small>Foto JPG, PNG, WebP atau video MP4, WebM</small></span>
                <span class="qrx-file-action">PILIH</span>
              </label>
              <input class="qrx-native-file" type="file" id="qrMediaInp" accept="image/*,video/mp4,video/webm,video/quicktime">
              <div class="qrx-media-preview" id="qrMediaPreview"><span class="qrx-empty-media">Pratinjau media akan tampil di sini.</span></div>
              <small>File dipratinjau secara lokal. File tidak dikirim ke server oleh pembaruan UI ini.</small>
            </div>

            <div class="qrx-field">
              <label for="qrMediaUrlInp">URL berbagi publik untuk media</label>
              <input class="tool-input qrx-url-input" id="qrMediaUrlInp" type="url" inputmode="url" placeholder="https://tautan-publik-foto-atau-video…" maxlength="2000">
              <small>Untuk QR media yang bisa dibuka perangkat lain, tempel tautan publik setelah media diunggah ke layanan penyimpanan.</small>
            </div>

            <div class="qrx-hint"><span>ⓘ</span><p>QR hanya menyimpan teks atau URL, bukan file foto/video itu sendiri. Jangan gunakan tautan privat yang meminta izin akses.</p></div>
            <div class="qrx-actions">${button('Generate QR', 'generate-qr', 'primary')}<span class="qrx-action-caption">OUTPUT PNG · 400 × 400</span></div>
          </section>

          <section class="qrx-panel qrx-output-panel">
            <div class="qrx-panel-heading">
              <span class="qrx-step">02</span>
              <div><h4>Hasil QR Code</h4><p>Pindai untuk menguji tujuan sebelum dibagikan.</p></div>
            </div>
            <div class="qrx-result-wrap">
              <div class="qr-preview qrx-result" id="qrPreviewBox"><span class="qrx-empty-qr">▦<small>QR Code akan tampil di sini</small></span></div>
            </div>
            <div class="qrx-output-foot"><span class="qrx-output-dot"></span><span>Pastikan tautan dapat dibuka tanpa login.</span></div>
            <div class="qrx-guide"><strong>Tips penggunaan</strong><span>1. Isi teks atau URL.</span><span>2. Tekan Generate QR.</span><span>3. Pindai dengan kamera untuk menguji.</span><span>4. Buka / simpan PNG.</span></div>
          </section>
        </div>
      </div>`;

    const mediaInput = $('#qrMediaInp', modalBody);
    const mediaPreview = $('#qrMediaPreview', modalBody);
    const mediaUrlInput = $('#qrMediaUrlInp', modalBody);
    mediaInput.addEventListener('change', () => {
      mediaPreview.replaceChildren();
      if (previewUrl) {
        try { URL.revokeObjectURL(previewUrl); } catch {}
        previewUrl = null;
      }
      const file = mediaInput.files && mediaInput.files[0];
      if (!file) {
        const empty = document.createElement('span');
        empty.className = 'qrx-empty-media';
        empty.textContent = 'Pratinjau media akan tampil di sini.';
        mediaPreview.appendChild(empty);
        return;
      }
      const isImage = file.type.startsWith('image/');
      const isVideo = file.type.startsWith('video/');
      if (!isImage && !isVideo) {
        mediaInput.value = '';
        toast('Pilih file foto atau video yang didukung.', 'error');
        return;
      }
      if (file.size > 50 * 1024 * 1024) {
        mediaInput.value = '';
        toast('Ukuran pratinjau maksimal 50 MB.', 'error');
        return;
      }
      previewUrl = URL.createObjectURL(file);
      const media = document.createElement(isVideo ? 'video' : 'img');
      media.className = 'qrx-preview-media';
      media.controls = isVideo;
      media.playsInline = true;
      media.preload = 'metadata';
      if (isImage) media.alt = 'Pratinjau foto yang dipilih';
      media.src = previewUrl;
      const info = document.createElement('div');
      info.className = 'qrx-file-meta';
      const name = document.createElement('strong');
      name.textContent = file.name;
      const detail = document.createElement('span');
      detail.textContent = `${isVideo ? 'VIDEO' : 'FOTO'} · ${(file.size / (1024 * 1024)).toFixed(2)} MB · pratinjau lokal`;
      info.append(name, detail);
      mediaPreview.append(media, info);
    });

    mediaUrlInput.addEventListener('input', () => {
      const textInput = $('#qrTextInp', modalBody);
      if (mediaUrlInput.value.trim()) textInput.setAttribute('data-qrx-suspended', 'true');
      else textInput.removeAttribute('data-qrx-suspended');
    });
    generateQr();
  }

  function generateQr() {
    const textInput = $('#qrTextInp', modalBody);
    const mediaInput = $('#qrMediaInp', modalBody);
    const mediaUrlInput = $('#qrMediaUrlInp', modalBody);
    const box = $('#qrPreviewBox', modalBody);
    if (!textInput || !box) return;
    box.replaceChildren();
    const selectedFile = mediaInput && mediaInput.files && mediaInput.files[0];
    const mediaUrl = mediaUrlInput ? mediaUrlInput.value.trim() : '';
    let value = textInput.value.trim();

    if (selectedFile && !mediaUrl) {
      const notice = document.createElement('div');
      notice.className = 'qrx-result-message';
      const heading = document.createElement('strong');
      heading.textContent = 'Media sudah dipilih';
      const body = document.createElement('span');
      body.textContent = 'Agar QR dapat dibuka di perangkat lain, unggah file ke penyimpanan publik lalu tempel URL berbagi di kolom URL media. File lokal tidak dapat dijadikan tautan QR lintas perangkat.';
      notice.append(heading, body);
      box.appendChild(notice);
      return;
    }

    if (mediaUrl) {
      try {
        const parsed = new URL(mediaUrl);
        if (!['http:', 'https:'].includes(parsed.protocol)) throw new Error('protocol');
        value = mediaUrl;
      } catch {
        const error = document.createElement('span');
        error.className = 'qrx-result-message qrx-result-error';
        error.textContent = 'URL media tidak valid. Gunakan tautan publik yang diawali http:// atau https://.';
        box.appendChild(error);
        return;
      }
    }

    if (!value) {
      const message = document.createElement('span');
      message.className = 'qrx-result-message';
      message.textContent = 'Masukkan teks, nomor, atau URL terlebih dahulu.';
      box.appendChild(message);
      return;
    }
    if (value.length > 2500) {
      const message = document.createElement('span');
      message.className = 'qrx-result-message qrx-result-error';
      message.textContent = 'Konten terlalu panjang. Maksimal 2.500 karakter.';
      box.appendChild(message);
      return;
    }

    const image = document.createElement('img');
    image.alt = 'QR Code hasil';
    image.loading = 'eager';
    image.className = 'qrx-generated-image';
    image.src = `https://api.qrserver.com/v1/create-qr-code/?size=400x400&margin=12&data=${encodeURIComponent(value)}`;
    image.addEventListener('error', () => {
      box.replaceChildren();
      const message = document.createElement('span');
      message.className = 'qrx-result-message qrx-result-error';
      message.textContent = 'QR tidak dapat dibuat. Periksa koneksi internet lalu coba lagi.';
      box.appendChild(message);
    }, { once: true });
    box.appendChild(image);

    const link = document.createElement('a');
    link.className = 'tool-btn qrx-download-link';
    link.href = image.src;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.textContent = 'Buka / simpan PNG';
    box.appendChild(link);

    const payload = document.createElement('span');
    payload.className = 'qrx-payload-caption';
    payload.textContent = `${value.length} karakter · QR PNG 400 × 400`;
    box.appendChild(payload);
  }


  function renderPassword() {
    modalBody.innerHTML = `
      <section class="pw-studio">
        <div class="pw-studio-hero">
          <div class="pw-studio-topline">
            <span class="pw-studio-eyebrow"><span class="pw-studio-live-dot"></span> PASSWORD GENERATOR</span>
            <span class="pw-studio-secure-tag">PRIVATE BY DESIGN</span>
          </div>
          <div class="pw-studio-hero-copy">
            <div class="pw-studio-emblem" aria-hidden="true">✳</div>
            <div>
              <h3>Secure Password</h3>
              <p>Buat kata sandi acak yang kuat langsung di browser.</p>
            </div>
          </div>
          <div class="pw-studio-hero-foot"><span>LOCAL GENERATION</span><span>NO SERVER STORAGE</span></div>
        </div>

        <div class="pw-studio-layout">
          <div class="pw-studio-panel pw-studio-controls">
            <div class="pw-studio-panel-heading">
              <div><span class="pw-studio-step">01 / CONFIGURATION</span><h4>Atur kata sandi</h4></div>
              <span class="pw-studio-heading-icon" aria-hidden="true">⌘</span>
            </div>

            <div class="pw-studio-length-row">
              <label for="pwLenRange">Panjang karakter</label>
              <output id="pwLenVal" for="pwLenRange">24 karakter</output>
            </div>
            <input class="tool-range pw-studio-range" type="range" id="pwLenRange" min="8" max="64" value="24" aria-label="Panjang kata sandi">
            <div class="pw-studio-range-scale"><span>8 · Singkat</span><span>64 · Maksimal</span></div>

            <div class="pw-studio-output-heading">
              <label for="pwOut">Password hasil</label>
              <span class="pw-studio-output-state"><span></span> READY</span>
            </div>
            <textarea class="tool-textarea pw-studio-output" id="pwOut" rows="3" readonly spellcheck="false" autocapitalize="off" autocomplete="off" aria-label="Password yang dihasilkan"></textarea>

            <div class="pw-studio-actions">
              ${button('Generate ulang', 'generate-password', 'primary')}
              ${button('Salin password', 'copy-password')}
            </div>
            <p class="pw-studio-hint">Gunakan tombol salin untuk mengambil hasil. Hindari membagikan kata sandi kepada orang lain.</p>
          </div>

          <aside class="pw-studio-panel pw-studio-security">
            <div class="pw-studio-security-icon" aria-hidden="true">◈</div>
            <span class="pw-studio-step">02 / SECURITY</span>
            <h4>Dibuat secara lokal</h4>
            <p>Kata sandi dihasilkan di browser perangkat ini. Nilainya tidak dikirim atau disimpan oleh server Ryomd.</p>
            <div class="pw-studio-security-divider"></div>
            <div class="pw-studio-security-item"><span class="pw-studio-check">✓</span><span><b>Secure random</b><small>crypto.getRandomValues()</small></span></div>
            <div class="pw-studio-security-item"><span class="pw-studio-check">✓</span><span><b>Privasi perangkat</b><small>Pemrosesan di sisi browser</small></span></div>
          </aside>
        </div>
        <div class="pw-studio-footer"><span class="pw-studio-footer-mark">RYOMD / SECURITY TOOLS</span><span>Atur panjang · Generate · Salin</span></div>
      </section>`;
    generatePassword();
    $('#pwLenRange',modalBody).addEventListener('input', () => {
      $('#pwLenVal',modalBody).textContent = `${$('#pwLenRange',modalBody).value} karakter`;
    });
  }
  function generatePassword() {
    const length = Number($('#pwLenRange',modalBody).value); const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*_-+=?';
    const bytes = new Uint32Array(length); crypto.getRandomValues(bytes); $('#pwOut',modalBody).value = [...bytes].map(value => chars[value % chars.length]).join('');
  }

  function renderImageDataUrl() {
    modalBody.innerHTML = `<div class="tool-field"><label for="imgB64Inp">Pilih gambar</label><input class="tool-input" type="file" id="imgB64Inp" accept="image/*"></div><pre class="tool-pre" id="imgB64Out">File diproses lokal di browser. Tidak ada upload ke server.</pre><div class="tool-actions" style="margin-top:10px">${button('Salin Data URL', 'copy-image-url')}</div>`;
    $('#imgB64Inp',modalBody).addEventListener('change', event => {
      const file = event.target.files?.[0]; if (!file) return;
      if (!file.type.startsWith('image/')) return toast('Pilih file gambar.', 'error');
      if (file.size > 5*1024*1024) return toast('Ukuran gambar maksimal 5 MB.', 'error');
      const reader = new FileReader(); reader.onload = () => { $('#imgB64Out',modalBody).textContent = reader.result; toast('Data URL berhasil dibuat.', 'success'); }; reader.readAsDataURL(file);
    });
  }

  function copyText(value) {
    if (navigator.clipboard?.writeText) return navigator.clipboard.writeText(value).then(() => toast('Berhasil disalin.', 'success')).catch(() => fallbackCopy(value));
    fallbackCopy(value);
  }
  function fallbackCopy(value) {
    const area = document.createElement('textarea'); area.value = value; area.style.position = 'fixed'; area.style.opacity = '0'; document.body.appendChild(area); area.select();
    try { document.execCommand('copy'); toast('Berhasil disalin.', 'success'); } catch { toast('Tidak dapat menyalin teks di browser ini.', 'error'); }
    area.remove();
  }
  function downloadCanvas(canvas, fileName) { const link = document.createElement('a'); link.download = fileName; link.href = canvas.toDataURL('image/png'); link.click(); }

  function runMorseAudio() {
    const text = $('#morseInp',modalBody).value.toUpperCase(); const code = [...text].map(char => MORSE[char] || '').filter(Boolean).join(' ');
    const AudioCtx = window.AudioContext || window.webkitAudioContext; if (!AudioCtx) return toast('Audio tidak didukung browser ini.', 'error');
    audioContext ||= new AudioCtx(); if (audioContext.state === 'suspended') audioContext.resume();
    const sequence = code.replace(/[^.-/]/g,''); let start = audioContext.currentTime + .05;
    for (const symbol of sequence) {
      if (symbol === '/') { start += .28; continue; }
      const duration = symbol === '.' ? .09 : .25; const osc = audioContext.createOscillator(); const gain = audioContext.createGain();
      osc.type = 'sine'; osc.frequency.value = 650; gain.gain.setValueAtTime(.0001,start); gain.gain.exponentialRampToValueAtTime(.12,start+.008); gain.gain.setValueAtTime(.12,start+duration-.01); gain.gain.exponentialRampToValueAtTime(.0001,start+duration);
      osc.connect(gain); gain.connect(audioContext.destination); osc.start(start); osc.stop(start+duration); start += duration+.08;
    }
  }

  // Single delegated action handler for every tool button.
  modalBody.addEventListener('click', async event => {
    const buttonElement = event.target.closest('[data-action]'); if (!buttonElement) return;
    const action = buttonElement.dataset.action;
    if (!action) return;
    if (action === 'assistant:send') { await sendAssistantMessage($('#assistantMessageInput', modalBody)?.value || ''); return; }
    if (action === 'assistant:settings') { await openAssistantSettings(); return; }
    if (action === 'assistant:settings-close') { const panel = $('#assistantSettingsPanel', modalBody); if (panel) panel.hidden = true; return; }
    if (action === 'assistant:save-settings') { await saveAssistantSettings(); return; }
    if (action === 'assistant:test') { await testAssistantConnection(); return; }
    if (action === 'assistant:quick-status') { await assistantQuickStatus(); return; }
    if (action === 'assistant:new') { assistantState.messages = []; assistantState.conversationId = `ryomd-${Date.now()}-${Math.random().toString(36).slice(2,10)}`; saveAssistantMessages(); renderAssistantMessages(); toast('Percakapan baru dimulai.'); return; }
    if (action.startsWith('service:')) {
      const [, command, serviceId] = action.split(':');
      if (command === 'refresh') { await renderServices($('#modalToolTitle').textContent.includes('WhatsApp') ? 'wa-bot' : 'all-services'); return; }
      if (command === 'log') {
        const box = $('#svcLiveLogBox',modalBody); if (!box) return;
        box.textContent = 'Memuat log…';
        try { const data = await api(`/api/services/log?serviceId=${encodeURIComponent(serviceId)}`); box.textContent = data.content || '(Kosong)'; }
        catch(error) { box.textContent = error.message; }
        return;
      }
      const old = buttonElement.textContent; buttonElement.disabled = true; buttonElement.textContent = 'Tunggu…';
      try { const data = await postJson('/api/services', { serviceId, action: command }); toast(data.message || 'Perintah diterima.', 'success'); await renderServices($('#modalToolTitle').textContent.includes('WhatsApp') ? 'wa-bot' : 'all-services'); syncServerState(); }
      catch(error) { toast(error.message, 'error'); buttonElement.disabled = false; buttonElement.textContent = old; }
      return;
    }
    if (action === 'choose-bot-zip') { const mode = state.zipMode; (mode === 'plugin' ? $('#pluginZipUploadInput') : $('#botZipUploadInput')).click(); return; }
    if (action === 'choose-app-zip') { $('#appZipUploadInput').click(); return; }
    if (action.startsWith('asset:')) { state.assetName = action.slice(6); $('#assetUploadInput').click(); return; }
    if (action.startsWith('terminal:')) { runTerminal(action.slice(9)); return; }
    if (action.startsWith('install:')) { installDependency(action.slice(8), buttonElement); return; }
    if (action === 'files:root') { loadFiles('.'); return; }
    if (action === 'save-entry-config') { saveEntryConfig(); return; }
    if (action === 'clear-logs') { if (!confirm('Bersihkan log dashboard? Log WhatsApp tetap dipertahankan.')) return; try { const data = await postJson('/api/logs', { action: 'clear' }); toast(data.message, 'success'); await renderLogs(); } catch(error) { toast(error.message,'error'); } return; }
    if (action === 'refresh-downloads') { try { await refreshDownloads(); } catch(error) { $('#dlResultOut',modalBody).textContent = error.message; } return; }
    if (action === 'yt-search') { await ryomdYouTubeSearch(); return; }
    if (action === 'yt-home-topic') {
      ryomdYouTubeState.nav = [];
      const field = $('#ryomdYtSearch', modalBody); if (field) field.value = '';
      const chipBox = $('#ryomdYtChips', modalBody); if (chipBox) chipBox.hidden = false;
      await ryomdYouTubeRenderHome(buttonElement.dataset.query || 'trending indonesia');
      return;
    }
    if (action === 'yt-refresh') {
      ryomdYouTubeState.homeLoadingMore = false;
      await ryomdYouTubeSearch(ryomdYouTubeState.query || RYT_TOPICS[0][0], true, { forceRefresh: true });
      return;
    }
    if (action === 'yt-load-more') {
      if (ryomdYouTubeState.homeLoadingMore || !ryomdYouTubeState.homeHasMore) return;
      ryomdYouTubeState.homeLoadingMore = true;
      const nextOffset = ryomdYouTubeState.lastResults.length;
      const moreButton = modalBody.querySelector('[data-action="yt-load-more"]');
      if (moreButton) { moreButton.disabled = true; moreButton.textContent = 'Memuat…'; }
      await ryomdYouTubeSearch(ryomdYouTubeState.query || RYT_TOPICS[0][0], true, { offset: nextOffset, append: true });
      return;
    }
    if (action === 'yt-connect') {
      if (window.RyomdYouTubeAccount && typeof window.RyomdYouTubeAccount.connect === 'function') window.RyomdYouTubeAccount.connect();
      else toast('Login Google hanya tersedia di APK Ryomd Android.', 'error');
      return;
    }
    if (action === 'yt-disconnect') { if (window.RyomdYouTubeAccount && typeof window.RyomdYouTubeAccount.disconnect === 'function') window.RyomdYouTubeAccount.disconnect(); return; }
    if (action === 'yt-subscriptions-refresh') {
      if (window.RyomdYouTubeAccount && ryomdYouTubeState.sessionReady) { ryomdYouTubeState.subsRequested = true; window.RyomdYouTubeAccount.loadSubscriptions(); }
      else toast('Masuk dengan akun Google terlebih dahulu.', 'error');
      return;
    }
    if (action === 'yt-playlists-refresh') {
      if (window.RyomdYouTubeAccount && ryomdYouTubeState.sessionReady) window.RyomdYouTubeAccount.loadPlaylists();
      else toast('Masuk dengan akun Google terlebih dahulu.', 'error');
      return;
    }
    if (action === 'yt-history-clear') { if (!confirm('Hapus seluruh riwayat tontonan Ryomd di perangkat ini?')) return; ryomdYouTubeState.history = []; try { localStorage.removeItem(RYOMD_YT_HISTORY_KEY); } catch (_) { } ryomdYouTubeRenderHistory(); toast('Riwayat dibersihkan.'); return; }
    if (action === 'yt-history-remove') {
      const removeId = buttonElement.dataset.id || '';
      ryomdYouTubeState.history = ryomdYouTubeState.history.filter(row => row.id !== removeId);
      ryomdYouTubeWriteJson(RYOMD_YT_HISTORY_KEY, ryomdYouTubeState.history);
      ryomdYouTubeRenderHistory();
      return;
    }
    if (action === 'yt-open-channel') { await ryomdYouTubeOpenChannel(buttonElement.dataset.url || ''); return; }
    if (action === 'yt-open-video') { await ryomdYouTubeOpenVideo(buttonElement.dataset.id || ''); return; }
    if (action === 'yt-quick-play') { ryomdYouTubeNativePlay(buttonElement.dataset.id || '', false); return; }
    if (action === 'yt-quick-audio') { ryomdYouTubeNativePlay(buttonElement.dataset.id || '', true); return; }
    if (action === 'yt-native-play' || action === 'yt-native-audio') {
      ryomdYouTubeNativePlay(buttonElement.dataset.id || '', action === 'yt-native-audio');
      return;
    }
    if (action === 'yt-link-play' || action === 'yt-link-audio') {
      const field = $('#ryomdYtLinkInput', modalBody);
      ryomdYouTubeNativePlay(field ? field.value : '', action === 'yt-link-audio');
      return;
    }
    if (action === 'yt-tab') { await ryomdYouTubeSetTab(buttonElement.dataset.view || 'home'); return; }
    if (action === 'yt-back' || action === 'yt-back-search') { await rytBack(); return; }
    if (action === 'yt-subscribe' || action === 'yt-subscribe-video') {
      const channel = action === 'yt-subscribe'
        ? ryomdYouTubeState.currentChannel
        : { url: buttonElement.dataset.url || '', title: buttonElement.dataset.title || '', thumbnail: '' };
      const on = rytToggleSubscription(channel);
      buttonElement.classList.toggle('is-on', on);
      buttonElement.setAttribute('aria-pressed', String(on));
      buttonElement.innerHTML = on ? `${RYT_ICONS.check}<span>Berlangganan</span>` : '<span>Berlangganan</span>';
      toast(on ? 'Ditambahkan ke Langganan.' : 'Berhenti berlangganan.', on ? 'success' : '');
      return;
    }
    if (action === 'yt-import') {
      const picker = $('#ryomdYtImportInput', modalBody);
      if (picker) { picker.dataset.kind = buttonElement.dataset.kind || ''; picker.click(); }
      return;
    }
    if (action === 'run-download') { runDownload(buttonElement); return; }
    if (action === 'youtube-play') { await renderYouTube(); return; }
    if (action === 'load-weather-fallback') { await loadWeatherFallback(); return; }
    if (action === 'generate-qr') { generateQr(); return; }
    if (action === 'download-quote') { downloadCanvas($('#quoteCanvas',modalBody), 'Ryomd-Quote.png'); return; }
    if (action === 'download-avatar') { downloadCanvas($('#avatarCanvas',modalBody), 'Ryomd-Profile.png'); return; }
    if (action === 'generate-password') { generatePassword(); return; }
    if (action === 'copy-password') { copyText($('#pwOut',modalBody).value); return; }
    if (action === 'run-html') { $('#htmlFrame',modalBody).srcdoc = $('#htmlCodeInp',modalBody).value; return; }
    if (action === 'register-module') { registerModule(); return; }
    if (action === 'load-weather') { loadWeather(); return; }
    if (action === 'load-crypto') { renderCrypto(); return; }
    if (action === 'load-ip') { loadIpInfo(); return; }
    if (action.startsWith('encode:')) { encodeText(action.slice(7)); return; }
    if (action === 'play-morse') { runMorseAudio(); return; }
    if (action === 'copy-morse') { copyText($('#morseOut',modalBody).textContent); return; }
    if (action === 'copy-image-url') { copyText($('#imgB64Out',modalBody).textContent); return; }
    if (action === 'copy-download-link') return;
  });
  modalBody.addEventListener('click', event => {
    const fileButton = event.target.closest('[data-file-path]');
    if (fileButton) loadFiles(fileButton.dataset.filePath);
  });
  modalBody.addEventListener('keydown', event => {
    if (event.target && event.target.id === 'assistantMessageInput' && event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault(); sendAssistantMessage(event.target.value);
    }
  });

  async function runTerminal(command) {
    const out = $('#nxTermOut',modalBody); if (!out) return;
    out.textContent += `\n$ ${command}\nMenjalankan…`;
    try { const data = await postJson('/api/terminal', { command }); out.textContent += `\n${data.output || data.message || 'Selesai.'}`; }
    catch(error) { out.textContent += `\n[ERROR] ${error.message}`; }
    out.scrollTop = out.scrollHeight;
  }
  async function installDependency(pkg, buttonElement) {
    const old = buttonElement.textContent; buttonElement.disabled = true; buttonElement.textContent = 'Memasang…';
    try { const data = await postJson('/api/dependencies/install', { pkg }); toast(data.message || 'Pemasangan selesai.', data.ok ? 'success' : ''); await renderDependencies(); }
    catch(error) { toast(error.message, 'error'); buttonElement.disabled = false; buttonElement.textContent = old; }
  }
  async function saveEntryConfig() {
    const serviceId = $('#configService',modalBody).value;
    const entry = $('#configEntry',modalBody).value.trim();
    const envContent = $('#configEnvInput',modalBody).value;
    const output = $('#configStatus',modalBody);
    if (!/^[a-zA-Z0-9._-]{1,80}\.(?:js|py|mjs|cjs)$/.test(entry) || entry.includes('..')) { output.textContent = 'Nama entry tidak valid. Gunakan nama file sederhana seperti bot.js atau main.py.'; return; }
    if (new Blob([envContent]).size > 16 * 1024) { output.textContent = 'Isi .env melebihi batas 16 KB.'; return; }
    output.textContent = 'Menyimpan konfigurasi…';
    try {
      const data = await postJson('/api/services', { serviceId, action: 'set-config', entry, envContent });
      output.textContent = data.message;
      $('#configEnvInput',modalBody).value = '';
      toast('Konfigurasi disimpan.', 'success');
    } catch(error) { output.textContent = error.message; }
  }
  async function registerModule() {
    const id = $('#nModId',modalBody).value.trim(); const name = $('#nModName',modalBody).value.trim();
    if (!id || !name) return toast('Lengkapi ID dan nama modul.', 'error');
    try { const data = await postJson('/api/modules', { action: 'register-manifest', manifest: { id, name } }); toast(data.message, 'success'); await renderModuleRegistry(); }
    catch(error) { toast(error.message, 'error'); }
  }
  async function runDiagnostic(mode) {
    const output = $('#aiDiagOut',modalBody); output.textContent = 'Memeriksa…';
    try {
      if (mode === 'status') { const data = await api('/api/services'); output.textContent = data.services.map(service => `${service.name}: ${service.status} · PID ${service.pid || '—'}\nFolder: ${service.folder}`).join('\n\n'); }
      else { const command = mode === 'plugins' ? 'ls -la modules/whatsapp/plugins' : 'ls -lh public/assets'; const data = await postJson('/api/terminal', { command }); output.textContent = data.output; }
    } catch(error) { output.textContent = error.message; }
  }
  async function loadWeather() {
    const code = $('#bmkgCode',modalBody).value.trim();
    const output = $('#bmkgOut',modalBody);
    const buttonEl = $('[data-action="load-weather"]', modalBody);
    if (!/^\d{2}\.\d{2}\.\d{2}\.\d{4}$/.test(code)) {
      output.innerHTML = '<div class="bmkg-load-message is-error"><b>Lokasi belum lengkap</b><span>Pilih provinsi, kabupaten/kota, kecamatan, dan desa/kelurahan terlebih dahulu.</span></div>';
      return;
    }
    if (buttonEl) { buttonEl.disabled = true; buttonEl.dataset.busy = '1'; }
    output.innerHTML = '<div class="bmkg-loading-state"><span class="bmkg-loading-orb">☁</span><b>Mengambil prakiraan BMKG…</b><small>Data cuaca sedang dimuat untuk wilayah pilihanmu.</small><span class="bmkg-loading-bar"><i></i></span></div>';
    try {
      const data = await api(`/api/public/bmkg?adm4=${encodeURIComponent(code)}`);
      const rows = (data.data?.[0]?.cuaca || []).flat().slice(0,8);
      if (!rows.length) {
        const loc = data.lokasi || data.data?.[0]?.lokasi || {};
        const lat = Number(loc.lat), lon = Number(loc.lon);
        const label = [loc.desa, loc.kecamatan, loc.kotkab].filter(Boolean).join(', ');
        bmkgLastLocation = Number.isFinite(lat) && Number.isFinite(lon) && Math.abs(lat) <= 90 && Math.abs(lon) <= 180
          ? { lat, lon, label } : null;
        output.innerHTML = `<div class="bmkg-load-message is-error"><b>Data prakiraan BMKG belum tersedia</b><span>BMKG mengenali lokasi, tetapi respons saat ini tidak memuat titik prakiraan (cuaca kosong). Ini bukan kesalahan pemilih wilayah. Coba kembali setelah BMKG memperbarui datanya.</span>${label ? `<small>Lokasi: ${esc(label)}</small>` : ''}${bmkgLastLocation ? '<button class="tool-btn" type="button" data-action="load-weather-fallback">Coba prakiraan alternatif · Open-Meteo</button>' : '<small>Koordinat lokasi tidak tersedia untuk sumber alternatif.</small>'}</div>`;
      } else {
        output.innerHTML = bmkgRenderForecastVisual(rows);
      }
    } catch(error) {
      output.innerHTML = `<div class="bmkg-load-message is-error"><b>Gagal mengambil prakiraan</b><span>${esc(error.message || 'Periksa koneksi lalu coba lagi.')}</span></div>`;
    } finally {
      if (buttonEl) { buttonEl.disabled = !$('#bmkgCode',modalBody)?.value; delete buttonEl.dataset.busy; }
    }
  }
  async function runDownload(buttonElement) {
    const url = $('#dlUrlInp',modalBody).value.trim(); const format = $('#dlFormatInp',modalBody).value; const output = $('#dlResultOut',modalBody);
    if (!/^https:\/\//i.test(url)) { toast('Masukkan URL HTTPS yang valid.', 'error'); $('#dlUrlInp',modalBody).focus(); return; }
    buttonElement.disabled = true; buttonElement.textContent = 'Memproses…'; output.textContent = 'Server sedang memproses file. Tunggu sampai ada hasil atau pesan error.';
    try {
      const data = await postJson('/api/downloads', { url, format });
      if (data.code === 'DEPENDENCY_MISSING') { output.textContent = `${data.message}\nPaket yang dibutuhkan: ${(data.missing || []).join(', ')}`; return; }
      output.textContent = data.message || 'Download selesai.'; renderDownloadFiles(data.files || []);
      state.history.unshift({ title: url, type: format.toUpperCase(), at: Date.now() }); state.history = state.history.slice(0,30); safeStorage.set('nx_dl_history',state.history); renderHistory(); toast(data.message || 'Download selesai.', 'success');
    } catch(error) { output.textContent = error.message; }
    finally { buttonElement.disabled = false; buttonElement.textContent = 'Mulai download'; }
  }

  // File upload actions.
  function imageToJpeg(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onerror = () => reject(new Error('Gambar tidak dapat dibaca.'));
      reader.onload = () => {
        const image = new Image();
        image.onerror = () => reject(new Error('Format gambar tidak dapat dibuka oleh browser.'));
        image.onload = () => {
          const maxSide = 4096; const scale = Math.min(1, maxSide / Math.max(image.naturalWidth, image.naturalHeight));
          const canvas = document.createElement('canvas'); canvas.width = Math.max(1, Math.round(image.naturalWidth * scale)); canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
          const context = canvas.getContext('2d'); context.fillStyle = '#ffffff'; context.fillRect(0, 0, canvas.width, canvas.height); context.drawImage(image, 0, 0, canvas.width, canvas.height);
          canvas.toBlob(blob => blob ? resolve(blob) : reject(new Error('Konversi ke JPEG gagal.')), 'image/jpeg', .92);
        };
        image.src = reader.result;
      };
      reader.readAsDataURL(file);
    });
  }
  function uploadAssetInput() {
    const input = $('#assetUploadInput');
    input.addEventListener('change', async event => {
      const file = event.target.files?.[0]; if (!file) return;
      if (state.assetName === '211648.mp4' && file.type !== 'video/mp4') { toast('Pilih file video MP4.', 'error'); input.value=''; return; }
      if (state.assetName !== '211648.mp4' && !/^image\//.test(file.type)) { toast('Pilih file gambar.', 'error'); input.value=''; return; }
      if (file.size > 120*1024*1024) { toast('Maksimal ukuran aset 120 MB.', 'error'); input.value=''; return; }
      toast(`Mengunggah ${file.name}…`);
      try {
        const uploadBlob = state.assetName === '211648.mp4' || file.type === 'image/jpeg' ? file : await imageToJpeg(file);
        const uploadType = state.assetName === '211648.mp4' ? 'video/mp4' : 'image/jpeg';
        const response = await fetch(`/api/upload-asset?name=${encodeURIComponent(state.assetName)}`, { method:'POST', headers:{'Content-Type':uploadType}, body:uploadBlob });
        const data = await response.json(); if (!response.ok || !data.ok) throw new Error(data.message || 'Upload aset gagal.');
        toast(data.message || 'Aset berhasil disimpan.', 'success');
        if (state.assetName === '211648.mp4') { heroVideo.style.display='block'; heroFallback.style.display='none'; heroVideo.src=`/assets/211648.mp4?t=${Date.now()}`; heroVideo.load(); }
        else if (state.assetName === '211650.jpg') { $$('img[src*="211650.jpg"]').forEach(image => image.src=`/assets/211650.jpg?t=${Date.now()}`); }
        else { heroFallback.src=`/assets/211649.jpg?t=${Date.now()}`; heroFallback.style.display='block'; heroVideo.style.display='none'; }
      } catch(error) { toast(error.message, 'error'); }
      finally { input.value=''; }
    });
  }
  uploadAssetInput();

  async function uploadZip(file, mode) {
    if (!file || !/\.zip$/i.test(file.name)) throw new Error('Pilih file ZIP.');
    if (file.size === 0) throw new Error('File ZIP kosong.');
    if (mode === 'plugin' && file.size > 16*1024*1024) throw new Error('Ukuran plugin ZIP maksimal 16 MB.');
    if (mode === 'source' && file.size > 64*1024*1024) throw new Error('Paket source bot maksimal 64 MB.');
    toast(`Mengunggah ${file.name}…`);
    const endpoint = mode === 'plugin' ? '/api/plugins/upload-zip' : `/api/services/upload-zip?serviceId=${encodeURIComponent(state.zipServiceId)}`;
    const response = await fetch(endpoint, { method:'POST', headers:{'Content-Type':'application/zip'}, body:file });
    const raw = await response.text(); let data; try { data = JSON.parse(raw); } catch { throw new Error(`Respons server tidak valid (HTTP ${response.status}).`); }
    if (!response.ok || !data.ok) throw new Error(data.message || 'Paket bot gagal dipasang.');
    toast(data.message || 'Paket bot berhasil dipasang.', 'success');
    const status = $('#botZipStatus',modalBody); if (status) status.textContent = data.message || 'Deploy selesai.';
    syncServerState();
  }
  $('#botZipUploadInput').addEventListener('change', async event => { const file = event.target.files?.[0]; try { await uploadZip(file,'source'); } catch(error) { toast(error.message,'error'); } finally { event.target.value=''; } });
  $('#pluginZipUploadInput').addEventListener('change', async event => { const file = event.target.files?.[0]; try { await uploadZip(file,'plugin'); } catch(error) { toast(error.message,'error'); } finally { event.target.value=''; } });
  $('#appZipUploadInput').addEventListener('change', async event => {
    const file = event.target.files?.[0]; if (!file) return;
    if (!/\.zip$/i.test(file.name)) { toast('Pilih file ZIP pembaruan aplikasi.', 'error'); event.target.value=''; return; }
    if (file.size > 32*1024*1024) { toast('Ukuran paket aplikasi maksimal 32 MB.', 'error'); event.target.value=''; return; }
    const status = $('#appUpdateStatus',modalBody); if (status) status.textContent = 'Mengunggah dan memeriksa paket…';
    try {
      const response = await fetch('/api/app-update', { method:'POST', headers:{'Content-Type':'application/zip'}, body:file });
      const raw = await response.text(); let data; try { data=JSON.parse(raw); } catch { throw new Error('Respons server pembaruan tidak valid.'); }
      if (!response.ok || !data.ok) throw new Error(data.message || 'Pembaruan aplikasi gagal.');
      if (status) status.textContent = data.message;
      toast('Pembaruan berhasil dipasang. Memuat ulang dashboard…','success');
      setTimeout(() => location.reload(), 900);
    } catch(error) { if (status) status.textContent = error.message; toast(error.message,'error'); }
    finally { event.target.value=''; }
  });

  // Service logs and status refresh are explicit actions, not mutation-observer patches.
  async function init() {
    renderTools(); renderHistory(); await syncServerState();
    window.setInterval(syncServerState, 15000);
  }
  init();
})();
