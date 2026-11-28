#!/bin/sh
# Downloads math.js and the two pixel fonts into the project for offline use. Needs network + npm once.
set -e
cd "$(dirname "$0")/.."
tmp=$(mktemp -d)
cd "$tmp"
npm pack mathjs @fontsource/press-start-2p @fontsource/vt323 >/dev/null
for f in *.tgz; do mkdir "${f%.tgz}" && tar xzf "$f" -C "${f%.tgz}"; done
cd - >/dev/null
cp "$tmp"/mathjs-*/package/lib/browser/math.js js/vendor/math.min.js
cp "$tmp"/fontsource-press-start-2p-*/package/files/press-start-2p-latin-400-normal.woff2 assets/fonts/PressStart2P.woff2
cp "$tmp"/fontsource-vt323-*/package/files/vt323-latin-400-normal.woff2 assets/fonts/VT323.woff2
rm -rf "$tmp"
echo "Vendored math.js and fonts. Reload the app twice to refresh the offline cache."
