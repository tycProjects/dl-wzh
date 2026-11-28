# Pixel Calculator

Retro 8-bit calculator with a scientific workspace, history, memory and unit/finance/date tools. No build step, no backend.

## Run
Open `index.html` directly, or serve the folder (`npx serve .`). Installing as an app and offline caching need `http(s)` (service workers do not run on `file://`).

## Offline-first
- Calculations, history, memory and settings run locally and persist in `localStorage` (corrupted data is discarded safely).
- `sw.js` precaches the app shell. After the first online visit it works with no connection.
- No CDN or remote fonts are used. Fonts fall back to monospace until you run the vendor step.

## Engine
- `js/engine.js` has two interchangeable engines behind one API (`ev`, `fmt`):
  - **Math.js** (when `js/vendor/math.min.js` exists): BigNumber arithmetic at 64 digits, so `0.1+0.2` is exactly `0.3`. Input is whitelisted: only known function/constant names and a fixed character set reach the parser; `import`, `evaluate`, `parse`, `createUnit`, `simplify`, `derivative` are disabled. Results must be real numbers inside ±1e308.
  - **Built-in** recursive-descent parser (no `eval`) used when Math.js is absent.
- The badge at the top-left of the display shows the active engine.

## Get Math.js and fonts (one-time, needs network)
```
sh scripts/vendor.sh
```
This copies `math.min.js` and the two fonts into `js/vendor/` and `assets/fonts/`. Reload twice to refresh the cache.

## Scientific workspace
SCI opens four groups of 10 keys each: TRIG, POWER, DISC (discrete/rounding), CONST (constants and symbols). DEG/RAD, ANS and precision (4-12 digits) sit below. The last group is remembered.

## Tests
`npm test` runs `tests/cases.js` through the engine (also uses Math.js if `npm i mathjs` is installed). The in-app TEST button runs the same cases in the browser.

## Keyboard
Digits, `+ - * / ^ % ! ( ) , .`, letters for function names, Enter or `=` to evaluate, Backspace, Escape to clear.

## Known limits
- Prime check is capped at 1e12. `isprime` returns 1 or 0.
- Results beyond ±1e308 report overflow even in BigNumber mode.
- The Math.js path was written without Math.js available in the build sandbox. Run `npm i mathjs && npm test` to confirm it on your machine.

## Themes
Open THEME and pick one; the choice is saved. Built in: **Pixel Mono** (default) and **Color OS** (soft light-gray, black operator keys, red C, 7-8-9 on top). A theme is a keypad layout in `js/themes.js` plus a `[data-ui=<key>]` block in `css/main.css`. Color OS adds a fifth row (`.`, backspace, `(`, `)`) so decimals and editing stay available.
