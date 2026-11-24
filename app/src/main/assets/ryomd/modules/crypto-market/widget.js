(() => {
  'use strict';
  const root = document.getElementById('ryomd-crypto-market');
  if (!root || root.dataset.initialized === '1') return;
  root.dataset.initialized = '1';

  const ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 2v20M17 6.5c0-1.6-1.9-2.7-4.5-2.7S8 4.9 8 6.3s1.1 2.3 4.5 2.9 4.5 1.7 4.5 3.2-1.9 3.1-5 3.1-5-1.3-5-3"/><path d="M7 4.7 17 19.3" opacity=".25"/></svg>';
  const REFRESH_ICON = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 7v5h-5M4 17v-5h5"/><path d="M5.6 9A7 7 0 0 1 18 6l2 1M4 17l2-1a7 7 0 0 0 12.4-3"/></svg>';
  const COINS = [
    { symbol: 'BTC', name: 'Bitcoin', mark: '₿' },
    { symbol: 'ETH', name: 'Ethereum', mark: '◆' },
    { symbol: 'SOL', name: 'Solana', mark: '◎' },
    { symbol: 'TON', name: 'Toncoin', mark: 'T' }
  ];
  let currency = 'idr';
  let isLoading = false;
  let lastData = null;

  function money(value, unit) {
    if (!Number.isFinite(Number(value))) return '—';
    const n = Number(value);
    if (unit === 'idr') {
      return new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(n);
    }
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: n >= 1 ? 2 : 4, maximumFractionDigits: n >= 1 ? 2 : 6 }).format(n);
  }

  function dateLabel(iso) {
    const d = iso ? new Date(iso) : null;
    if (!d || Number.isNaN(d.getTime())) return 'waktu pembaruan tidak diketahui';
    return new Intl.DateTimeFormat('id-ID', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: 'short' }).format(d);
  }

  function changeMarkup(value) {
    if (!Number.isFinite(Number(value))) return '<span class="cm-change neutral">24j —</span>';
    const n = Number(value);
    const cls = n > 0 ? 'up' : n < 0 ? 'down' : 'neutral';
    const arrow = n > 0 ? '↗' : n < 0 ? '↘' : '→';
    return `<span class="cm-change ${cls}">${arrow} ${n > 0 ? '+' : ''}${n.toFixed(2)}%</span>`;
  }

  function renderLoading() {
    root.innerHTML = `
      <section class="cm-shell" aria-label="Crypto Market">
        <div class="cm-topline"><div class="cm-heading"><div class="cm-mark">${ICON}</div><div><h2 class="cm-title">Crypto Market</h2><p class="cm-subtitle">Pantau aset kripto pilihanmu dalam satu tempat</p></div></div><div class="cm-controls"><div class="cm-currency"><button data-currency="idr" aria-pressed="${currency === 'idr'}">IDR</button><button data-currency="usd" aria-pressed="${currency === 'usd'}">USD</button></div><button class="cm-refresh" disabled>${REFRESH_ICON} Memuat…</button></div></div>
        <div class="cm-statusline"><span class="cm-live-dot"></span><span>Menghubungkan ke sumber harga…</span></div>
        <div class="cm-grid">${COINS.map(c => `<article class="cm-card"><div class="cm-coin-head"><span class="cm-coin-icon">${c.mark}</span><div><div class="cm-name">${c.name}</div><span class="cm-symbol">${c.symbol}</span></div></div><div class="cm-price"><div class="cm-skeleton"></div></div><div class="cm-bottom"><span class="cm-change neutral">Memuat</span></div></article>`).join('')}</div>
      </section>`;
    bindCurrencyButtons();
  }

  function renderError(message) {
    root.innerHTML = `
      <section class="cm-shell" aria-label="Crypto Market">
        <div class="cm-topline"><div class="cm-heading"><div class="cm-mark">${ICON}</div><div><h2 class="cm-title">Crypto Market</h2><p class="cm-subtitle">BTC · ETH · SOL · TON</p></div></div><div class="cm-controls"><div class="cm-currency"><button data-currency="idr" aria-pressed="${currency === 'idr'}">IDR</button><button data-currency="usd" aria-pressed="${currency === 'usd'}">USD</button></div><button class="cm-refresh" id="cm-refresh">${REFRESH_ICON} Coba lagi</button></div></div>
        <div class="cm-message cm-error">${escapeHtml(message || 'Harga belum dapat dimuat. Periksa koneksi server lokal lalu coba lagi.')}</div>
        <div class="cm-footer"><span>Data disediakan oleh CoinGecko dengan sumber cadangan.</span><a href="https://www.coingecko.com/" target="_blank" rel="noopener noreferrer">CoinGecko ↗</a></div>
      </section>`;
    root.querySelector('#cm-refresh')?.addEventListener('click', load);
    bindCurrencyButtons();
  }

  function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
  }

  function render(data) {
    lastData = data;
    const prices = data.prices || {};
    const statusText = data.stale ? 'Data tersimpan · belum berhasil diperbarui' : data.cached ? 'Data cache · sumber diperbarui berkala' : 'Harga berhasil diperbarui';
    const notice = data.notice ? `<div class="cm-message">${escapeHtml(data.notice)}</div>` : '';
    root.innerHTML = `
      <section class="cm-shell" aria-label="Crypto Market">
        <div class="cm-topline">
          <div class="cm-heading"><div class="cm-mark">${ICON}</div><div><h2 class="cm-title">Crypto Market</h2><p class="cm-subtitle">Harga BTC, ETH, SOL, dan TON · pasar spot</p></div></div>
          <div class="cm-controls">
            <div class="cm-currency" role="group" aria-label="Pilih mata uang"><button data-currency="idr" aria-pressed="${currency === 'idr'}">IDR</button><button data-currency="usd" aria-pressed="${currency === 'usd'}">USD</button></div>
            <button class="cm-refresh" id="cm-refresh">${REFRESH_ICON} Perbarui harga</button>
          </div>
        </div>
        <div class="cm-statusline"><span class="cm-live-dot"></span><span>${statusText}</span><span class="cm-status-pill">${escapeHtml(data.sourceLabel || data.source || 'Market data')}</span></div>
        <div class="cm-grid">${COINS.map(coin => {
          const item = prices[coin.symbol];
          const amount = item ? (currency === 'idr' ? item.idr : item.usd) : null;
          return `<article class="cm-card"><div class="cm-coin-head"><span class="cm-coin-icon">${coin.mark}</span><div><div class="cm-name">${coin.name}</div><span class="cm-symbol">${coin.symbol}</span></div></div><div class="cm-price">${item ? money(amount, currency) : '—'}</div><div class="cm-bottom">${changeMarkup(item?.change24h)}<span class="cm-coin-note">24 jam</span></div></article>`;
        }).join('')}</div>
        ${notice}
        <div class="cm-footer"><span>Terakhir diperbarui: ${dateLabel(data.updatedAt)}${data.approxUsd ? ' · USDT ≈ USD' : ''}</span><a href="https://www.coingecko.com/" target="_blank" rel="noopener noreferrer">Sumber data ↗</a></div>
      </section>`;
    root.querySelector('#cm-refresh')?.addEventListener('click', load);
    bindCurrencyButtons();
  }

  function bindCurrencyButtons() {
    root.querySelectorAll('[data-currency]').forEach(button => {
      button.addEventListener('click', () => {
        const next = button.getAttribute('data-currency');
        if (next !== 'idr' && next !== 'usd') return;
        currency = next;
        if (lastData) render(lastData); else renderLoading();
      });
    });
  }

  async function load() {
    if (isLoading) return;
    isLoading = true;
    const button = root.querySelector('#cm-refresh');
    if (button) { button.disabled = true; button.innerHTML = `${REFRESH_ICON} Memuat…`; }
    if (!lastData) renderLoading();
    try {
      const response = await fetch('/api/crypto-market?refresh=1', {
        method: 'GET',
        headers: { accept: 'application/json' },
        cache: 'no-store'
      });
      const data = await response.json();
      if (!response.ok || data.ok !== true || !data.prices) throw new Error(data.detail || data.error || `HTTP ${response.status}`);
      render(data);
    } catch (error) {
      if (lastData) {
        render({ ...lastData, stale: true, notice: 'Pembaruan gagal. Menampilkan harga terakhir yang berhasil dimuat.' });
      } else {
        renderError(error.message || 'Tidak dapat terhubung ke endpoint /api/crypto-market. Pastikan route backend sudah dipasang.');
      }
    } finally {
      isLoading = false;
      const refresh = root.querySelector('#cm-refresh');
      if (refresh) refresh.disabled = false;
    }
  }

  window.__RYOMD_CRYPTO_MARKET_LOAD__ = load;
  if (!window.__RYOMD_CRYPTO_MARKET_TIMER__) {
    window.__RYOMD_CRYPTO_MARKET_TIMER__ = window.setInterval(() => {
      if (!document.hidden && document.getElementById('ryomd-crypto-market') &&
          typeof window.__RYOMD_CRYPTO_MARKET_LOAD__ === 'function') {
        window.__RYOMD_CRYPTO_MARKET_LOAD__();
      }
    }, 60_000);
  }
  load();
})();