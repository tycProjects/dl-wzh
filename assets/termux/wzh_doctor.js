#!/usr/bin/env node
// WZH 2 APK: Build Doctor for the on-phone build. Generated from the server's server.js (the same repair
// rules, extracted with their helpers); run in Termux as:  node wzh_doctor.js <projectRoot> [buildLogFile]
// Prints one "WZH-DOCTOR: <what was fixed>" line per repair and "WZH-DOCTOR-COUNT: <n>" at the end.

async function fetchWithTimeout(url, opts = {}, timeoutMs = 30000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...opts, signal: controller.signal });
  } catch (err) {
    if (err.name === 'AbortError') throw new Error(`Request timed out after ${timeoutMs}ms: ${url}`);
    throw err;
  } finally {
    clearTimeout(timer);
  }
}

const fs = require('fs');

const path = require('path');

const IGNORED_DIR_NAMES = new Set(['.git', '__MACOSX', 'node_modules', '.gradle', '.idea', 'build']);

function listProjectFiles(projectRoot, predicate) {
  const out = [];
  const queue = [projectRoot];
  while (queue.length) {
    const dir = queue.shift();
    let entries;
    try { entries = fs.readdirSync(dir, { withFileTypes: true }); } catch { continue; }
    for (const e of entries) {
      if (e.isDirectory()) {
        if (!IGNORED_DIR_NAMES.has(e.name)) queue.push(path.join(dir, e.name));
      } else if (e.isFile()) {
        const full = path.join(dir, e.name);
        if (!predicate || predicate(full, e.name)) out.push(full);
      }
    }
  }
  return out;
}

function dedupeRasterResources(projectRoot) {
  const fixes = [];
  const priority = ['.png', '.webp', '.jpg', '.jpeg', '.gif'];
  const resDirRe = /[\\/]res[\\/](?:mipmap|drawable)[^\\/]*$/;
  const groups = new Map(); // "<dir>|<resource name>" -> [files]
  for (const file of listProjectFiles(projectRoot, (full, name) =>
    resDirRe.test(path.dirname(full)) && priority.includes(path.extname(name).toLowerCase()))) {
    // foo.9.png (nine-patch) is still resource "foo"
    const resName = path.basename(file, path.extname(file)).replace(/\.9$/, '');
    const key = `${path.dirname(file)}|${resName}`;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(file);
  }
  for (const files of groups.values()) {
    if (files.length < 2) continue;
    files.sort((a, b) => priority.indexOf(path.extname(a).toLowerCase()) - priority.indexOf(path.extname(b).toLowerCase()));
    const keep = files[0];
    for (const dup of files.slice(1)) {
      try {
        fs.rmSync(dup, { force: true });
        fixes.push(`Removed duplicate resource ${path.relative(projectRoot, dup)} (same resource name as ${path.relative(projectRoot, keep)}; Android fails with "Duplicate resources" when both exist)`);
      } catch (_) {}
    }
  }
  return fixes;
}

function fixFileVsValueDuplicateResources(projectRoot) {
  const fixes = [];
  const FILE_TYPES = new Set(['drawable', 'color', 'layout', 'anim', 'animator', 'menu', 'xml', 'raw', 'font', 'mipmap', 'interpolator', 'transition', 'navigation']);
  const normQual = (q) => (q || '').replace(/-v\d+$/i, '').toLowerCase();
  const resFiles = listProjectFiles(projectRoot, (full) => /[\\/]res[\\/][a-z]+(?:-[^\\/]+)?[\\/][^\\/]+$/.test(full));
  const fileRes = new Set(); // "<resDir>|<type>|<qual>|<name>"
  const valueFiles = [];
  for (const f of resFiles) {
    const dir = path.dirname(f);
    const m = path.basename(dir).match(/^([a-z]+)(?:-(.+))?$/);
    if (!m) continue;
    const resDir = path.dirname(dir);
    if (path.basename(resDir) !== 'res') continue;
    if (m[1] === 'values') { if (f.endsWith('.xml')) valueFiles.push({ file: f, resDir, qual: normQual(m[2]) }); continue; }
    if (!FILE_TYPES.has(m[1])) continue;
    const name = path.basename(f, path.extname(f)).replace(/\.9$/, '');
    fileRes.add(`${resDir}|${m[1]}|${normQual(m[2])}|${name}`);
  }
  if (!fileRes.size) return fixes;
  const entryRe = /[ \t]*<(drawable|color|item)\b([^>]*?)(?:\/>|>[\s\S]*?<\/\1>)[ \t]*\r?\n?/g;
  for (const v of valueFiles) {
    let c;
    try { c = fs.readFileSync(v.file, 'utf8'); } catch (_) { continue; }
    const removed = [];
    const out = c.replace(entryRe, (whole, tag, attrs) => {
      const nm = attrs.match(/\bname\s*=\s*"([^"]+)"/);
      if (!nm) return whole;
      let type = tag;
      if (tag === 'item') {
        const tm = attrs.match(/\btype\s*=\s*"([^"]+)"/);
        if (!tm) return whole; // an <item> inside a <style>/<array>, not a resource
        type = tm[1];
      }
      if (!FILE_TYPES.has(type)) return whole;
      if (!fileRes.has(`${v.resDir}|${type}|${v.qual}|${nm[1]}`)) return whole;
      removed.push(`${type}/${nm[1]}`);
      return '';
    });
    if (removed.length) {
      fs.writeFileSync(v.file, out, 'utf8');
      const shown = removed.slice(0, 6).join(', ') + (removed.length > 6 ? `, +${removed.length - 6} more` : '');
      fixes.push(`Removed ${removed.length} duplicate resource entr${removed.length === 1 ? 'y' : 'ies'} from ${path.relative(projectRoot, v.file)} (${shown}) -- the same resource already exists as a file under res/, and Android fails with "Duplicate resources" when both exist; kept the file`);
    }
  }
  return fixes;
}

function fixFlutterV1Embedding(projectRoot) {
  const fixes = [];
  const pubspecs = listProjectFiles(projectRoot, (full, name) => name === 'pubspec.yaml');
  for (const pub of pubspecs) {
    let ptxt = '';
    try { ptxt = fs.readFileSync(pub, 'utf8'); } catch (_) { continue; }
    if (!/sdk:\s*flutter/.test(ptxt)) continue;
    const mainDir = path.join(path.dirname(pub), 'android', 'app', 'src', 'main');
    const manifestPath = path.join(mainDir, 'AndroidManifest.xml');
    const rel = (p) => path.relative(projectRoot, p) || p;
    if (!fs.existsSync(manifestPath)) {
      // Flutter treats a missing app manifest as the v1 embedding too, so say so instead of staying silent.
      if (fs.existsSync(path.join(path.dirname(pub), 'android'))) {
        fixes.push(`Build Doctor warnings: ${rel(manifestPath)} does not exist, and Flutter reports that as "deleted Android v1 embedding" -- the android/app/src/main/AndroidManifest.xml file must be in the zip (or delete the android/ folder so one is generated fresh).`);
      }
      continue;
    }

    let m = fs.readFileSync(manifestPath, 'utf8');
    const before = m;
    // Flutter parses this file as XML and calls the app v1 if the parse fails, so clean the
    // things an editor on a phone commonly leaves behind: a BOM, or blank space before <?xml.
    m = m.replace(/^\uFEFF/, '').replace(/^\s+(?=<\?xml)/, '');
    // Mirror Flutter's own check: the FIRST live <meta-data android:name="flutterEmbedding"> decides.
    // A commented-out entry, a value of 1, or a "1" listed before a "2" all still mean v1.
    const mLive = m.replace(/<!--[\s\S]*?-->/g, '');
    const decls = [...mLive.matchAll(/<meta-data\b[^>]*>/g)].map(x => x[0]).filter(t => /android:name\s*=\s*["']flutterEmbedding["']/.test(t));
    const firstVal = decls.length ? ((/android:value\s*=\s*["']([^"']*)["']/.exec(decls[0]) || [])[1] || null) : null;
    const hasV2 = firstVal === '2' && decls.every(t => /android:value\s*=\s*["']2["']/.test(t));
    if (!hasV2) {
      // drop every stale/wrong declaration, then add one clean one inside <application>
      m = m.replace(/[ \t]*<meta-data\b(?=[^>]*android:name\s*=\s*["']flutterEmbedding["'])[^>]*?(?:\/>|><\/meta-data>)[ \t]*\r?\n?/g, '');
      // a self-closing <application ... /> has nowhere to put it -- open it up
      m = m.replace(/<application\b([^>]*?)\s*\/>/, (w, attrs) => `<application${attrs}>\n    </application>`);
      if (/<\/application>/.test(m)) {
        m = m.replace(/(\r?\n)?([ \t]*)<\/application>/, (w, nl, ind) =>
          `\n${ind}    <meta-data\n${ind}        android:name="flutterEmbedding"\n${ind}        android:value="2" />\n${ind}</application>`);
      }
    }
    // the android: prefix must be declared or the XML parse fails (and Flutter falls back to v1)
    if (!/xmlns:android\s*=/.test(m)) {
      m = m.replace(/<manifest\b/, '<manifest xmlns:android="http://schemas.android.com/apk/res/android"');
    }
    m = m.replace(/android:name\s*=\s*"io\.flutter\.app\.FlutterApplication"/g, 'android:name="${applicationName}"');
    if (m !== before) {
      fs.writeFileSync(manifestPath, m, 'utf8');
      fixes.push(`Flutter: declared the v2 embedding (flutterEmbedding=2) in ${rel(manifestPath)} -- Flutter refuses to build an Android app that still uses the deleted v1 embedding`);
    }

    const srcFiles = listProjectFiles(mainDir, (full, name) => /\.(kt|java)$/.test(name));
    for (const f of srcFiles) {
      let c = fs.readFileSync(f, 'utf8');
      if (!/io\.flutter\.app\.|registerWith\s*\(\s*this\s*\)/.test(c)) continue;
      const orig = c;
      c = c.replace(/io\.flutter\.app\.FlutterFragmentActivity/g, 'io.flutter.embedding.android.FlutterFragmentActivity');
      c = c.replace(/io\.flutter\.app\.FlutterActivity/g, 'io.flutter.embedding.android.FlutterActivity');
      if (/io\.flutter\.app\.FlutterApplication/.test(c)) {
        if (/PluginRegistrantCallback|PluginRegistry/.test(c)) {
          fixes.push(`Build Doctor warnings: ${rel(f)} extends the v1 FlutterApplication with a plugin-registrant callback -- remove that callback by hand (the v2 embedding registers plugins automatically).`);
        } else {
          c = c.replace(/io\.flutter\.app\.FlutterApplication/g, 'android.app.Application').replace(/\bFlutterApplication\b/g, 'Application');
        }
      }
      c = c.replace(/^[ \t]*(?:GeneratedPluginRegistrant\.)?registerWith\s*\(\s*this\s*\)\s*;?[ \t]*\r?\n/gm, '');
      if (c !== orig) {
        fs.writeFileSync(f, c, 'utf8');
        fixes.push(`Flutter: moved ${rel(f)} from v1 embedding classes (io.flutter.app.*, registerWith(this)) to the v2 embedding`);
      }
    }
  }
  return fixes;
}

function findAppManifestForDoctor(projectRoot) {
  const manifests = listProjectFiles(projectRoot, (full, name) =>
    name === 'AndroidManifest.xml' && path.basename(path.dirname(full)) === 'main'
  );
  if (!manifests.length) return null;

  const appLike = manifests.filter(m => {
    const moduleDir = path.dirname(path.dirname(path.dirname(m)));
    return ['build.gradle', 'build.gradle.kts'].some(f => {
      const p = path.join(moduleDir, f);
      if (!fs.existsSync(p)) return false;
      const c = fs.readFileSync(p, 'utf8');
      return /com\.android\.application|com\.android\.application\.library/.test(c);
    });
  });
  return (appLike.length === 1 ? appLike[0] : manifests.length === 1 ? manifests[0] : null);
}

function addManifestPermission(content, permission) {
  if (new RegExp(`android:name=["']${permission.replace(/\./g, '\\.') }["']`).test(content)) return content;
  const insert = `    <uses-permission android:name="${permission}" />\n`;
  return content.replace(/(<manifest[^>]*>\s*)/i, `$1${insert}`);
}

function ensureManifestService(content, serviceName, attrs) {
  const serviceRe = new RegExp(`<service\\b[^>]*android:name=["']${serviceName.replace(/\./g, '\\.') }["'][^>]*>`, 'i');
  const selfClosingRe = new RegExp(`<service\\b[^>]*android:name=["']${serviceName.replace(/\./g, '\\.') }["'][^>]*/>,`, 'i');
  const match = content.match(serviceRe);
  if (match) {
    let tag = match[0];
    let changed = false;
    for (const [k, v] of Object.entries(attrs)) {
      const attrRe = new RegExp(`android:${k}=["'][^"']*["']`, 'i');
      if (attrRe.test(tag)) {
        const next = tag.replace(attrRe, `android:${k}="${v}"`);
        changed ||= next !== tag;
        tag = next;
      } else {
        tag = tag.replace(/\s*\/?>(?:\s*)$/i, ` android:${k}="${v}" />`);
        changed = true;
      }
    }
    return { content: changed ? content.replace(match[0], tag) : content, changed };
  }
  const line = `    <service android:name="${serviceName}"${Object.entries(attrs).map(([k,v]) => ` android:${k}="${v}"`).join('')} />\n`;
  return { content: content.replace(/<application\b[^>]*>/i, m => `${m}\n${line}`), changed: true };
}

function fixMissingThemeLibraryDependency(projectRoot) {
  const fixes = [];
  const appModule = findAppModule(projectRoot);
  if (!appModule) return fixes;
  const moduleDir = path.dirname(path.dirname(path.dirname(appModule.resDir)));
  const manifestPath = path.join(moduleDir, 'src', 'main', 'AndroidManifest.xml');
  if (!fs.existsSync(manifestPath)) return fixes;
  const manifest = fs.readFileSync(manifestPath, 'utf8');

  // Every android:theme="@style/X" reference in the manifest (application-
  // level and per-activity/per-service) is a candidate -- any one of them
  // failing to resolve is a real AAPT failure, not just the app-wide theme.
  const themeNames = new Set();
  const THEME_ATTR_RE = /android:theme="@style\/([\w.]+)"/g;
  let tm;
  while ((tm = THEME_ATTR_RE.exec(manifest))) themeNames.add(tm[1]);
  if (!themeNames.size) return fixes;

  // Map every locally declared <style> to its parent, across every
  // values*/ resource file in this module, so a custom theme name (the
  // usual case -- "AppTheme", not the library theme directly) can be
  // walked up to whatever built-in library theme it ultimately extends.
  const styleFiles = listProjectFiles(appModule.resDir, (full) =>
    /\.xml$/.test(full) && /^values/.test(path.basename(path.dirname(full)))
  );
  const parentOf = {};
  const STYLE_TAG_RE = /<style\b([^>]*)>/g;
  for (const f of styleFiles) {
    let c;
    try { c = fs.readFileSync(f, 'utf8'); } catch { continue; }
    let sm;
    STYLE_TAG_RE.lastIndex = 0;
    while ((sm = STYLE_TAG_RE.exec(c))) {
      const attrs = sm[1];
      const nameMatch = attrs.match(/\bname="([^"]+)"/);
      if (!nameMatch) continue;
      const name = nameMatch[1];
      const parentMatch = attrs.match(/\bparent="([^"]*)"/);
      if (parentMatch) {
        // parent="" is Android's own way of saying "no parent" (used for
        // an intentionally bare base style) -- keep it as the chain's end
        // rather than treating an empty string as "no match found".
        parentOf[name] = parentMatch[1];
      } else if (!(name in parentOf)) {
        // No explicit parent: Android's dotted-name convention implies
        // inheriting from everything before the last dot (e.g.
        // "AppTheme.NoActionBar" implicitly extends "AppTheme").
        const dot = name.lastIndexOf('.');
        if (dot > 0) parentOf[name] = name.slice(0, dot);
      }
    }
  }

  // artifact -> the theme name(s) that led to needing it, so the fix
  // message below can say *why* rather than just *what*.
  const neededArtifacts = new Map();
  for (const themeName of themeNames) {
    let resolved = themeName;
    const seen = new Set();
    while (parentOf[resolved] && !seen.has(resolved)) {
      seen.add(resolved);
      resolved = parentOf[resolved];
    }
    let artifact = null;
    if (/MaterialComponents/i.test(resolved)) {
      artifact = 'com.google.android.material:material:1.11.0';
    } else if (/AppCompat/i.test(resolved)) {
      artifact = 'androidx.appcompat:appcompat:1.6.1';
    }
    if (!artifact) continue;
    const chainDesc = themeName === resolved ? `"${themeName}"` : `"${themeName}" (resolves via styles.xml to "${resolved}")`;
    if (!neededArtifacts.has(artifact)) neededArtifacts.set(artifact, []);
    neededArtifacts.get(artifact).push(chainDesc);
  }
  if (!neededArtifacts.size) return fixes;

  // Same module build file prepareAutoRepair edits for the log-driven
  // version of this fix -- kept in sync deliberately so both paths land
  // the dependency in the same place.
  for (const gradleFile of ['build.gradle.kts', 'build.gradle']) {
    const gradlePath = path.join(moduleDir, gradleFile);
    if (!fs.existsSync(gradlePath)) continue;
    let c = fs.readFileSync(gradlePath, 'utf8');
    const isKts = gradleFile.endsWith('.kts');
    let changed = false;
    for (const [artifact, chainDescs] of neededArtifacts) {
      const coords = artifact.split(':').slice(0, 2).join(':');
      if (c.includes(coords)) continue; // already declared (any version)
      if (!/dependencies\s*\{/.test(c)) continue;
      const depLine = isKts ? `    implementation("${artifact}")` : `    implementation '${artifact}'`;
      c = c.replace(/dependencies\s*\{/, `dependencies {\n${depLine}`);
      changed = true;
      fixes.push(`Build Doctor: added missing ${artifact} dependency to ${path.relative(projectRoot, gradlePath)} (manifest theme ${chainDescs.join(', ')} only exists in that library)`);
    }
    if (changed) fs.writeFileSync(gradlePath, c, 'utf8');
    break; // only one module build file is the real one
  }

  return fixes;
}

function doctorFixLegacyOutputFileNameSet(content) {
  if (!/\boutputFileName\s*\.\s*set\b/.test(content)) return content;
  if (!/\b(?:applicationVariants|libraryVariants|testVariants|unitTestVariants|outputs\s*\.\s*(?:all|each|configureEach)|BaseVariantOutput|VariantOutputImpl)\b/.test(content)) return content;
  const re = /(\boutputFileName)\s*\.\s*set\s*\(/g;
  let out = '', last = 0, m;
  while ((m = re.exec(content))) {
    const open = m.index + m[0].length - 1;
    let depth = 0, i = open, quote = null;
    for (; i < content.length; i++) {
      const ch = content[i];
      if (quote) {
        if (ch === '\\') { i++; continue; }
        if (ch === quote) quote = null;
        continue;
      }
      if (ch === '"' || ch === "'") { quote = ch; continue; }
      if (ch === '(') depth++;
      else if (ch === ')') { depth--; if (depth === 0) break; }
    }
    if (depth !== 0) break; // unbalanced: leave the rest untouched
    out += content.slice(last, m.index) + m[1] + ' = ' + content.slice(open + 1, i).trim();
    last = i + 1;
    re.lastIndex = last;
  }
  return out + content.slice(last);
}

async function runBuildDoctor(projectRoot, log = '') {
  const fixes = [];

  // Runs first, before allSourceFiles is gathered below, so if this
  // restructures the project (flat app files -> a real app/ subfolder),
  // every other fix in this function sees the corrected layout instead of
  // the original flat one. Placed inside runBuildDoctor itself (rather than
  // its caller normalizeProjectForCI) specifically so it also runs on every
  // AI-repair retry -- prepareAutoRepair calls runBuildDoctor directly and
  // does NOT go through normalizeProjectForCI, so anything added only there
  // would never get a chance to run once a build has already failed.
  const flatModuleFix = fixFlatAndroidAppModule(projectRoot);
  if (flatModuleFix) fixes.push(flatModuleFix);

  const allSourceFiles = listProjectFiles(projectRoot, (full, name) =>
    /\.(kt|kts|java|xml|gradle|properties|json|js|ts)$/.test(name)
  );
  const source = allSourceFiles.map(f => {
    try { return fs.readFileSync(f, 'utf8'); } catch { return ''; }
  }).join('\n');

  const manifestPath = findAppManifestForDoctor(projectRoot);
  if (manifestPath) {
    let manifest = fs.readFileSync(manifestPath, 'utf8');
    const original = manifest;

    // WorkManager foreground service + Android 14 dataSync contract.
    const usesWorkManager = /androidx\.work|work-runtime|SystemForegroundService|CoroutineWorker|Worker\b/.test(source);
    const usesDataSync = /FOREGROUND_SERVICE_TYPE_DATA_SYNC|foregroundServiceType\s*[=:]\s*["']?dataSync|dataSync/i.test(source);
    const usesAnyForeground = /startForeground\s*\(|ForegroundInfo\s*\(|FOREGROUND_SERVICE_TYPE_/i.test(source);
    if (usesWorkManager && usesAnyForeground) {
      const before = manifest;
      manifest = addManifestPermission(manifest, 'android.permission.FOREGROUND_SERVICE');
      if (manifest !== before) fixes.push('Added missing FOREGROUND_SERVICE permission');
    }
    if (usesDataSync) {
      const before = manifest;
      manifest = addManifestPermission(manifest, 'android.permission.FOREGROUND_SERVICE_DATA_SYNC');
      if (manifest !== before) fixes.push('Added missing FOREGROUND_SERVICE_DATA_SYNC permission');
      const result = ensureManifestService(manifest, 'androidx.work.impl.foreground.SystemForegroundService', {
        foregroundServiceType: 'dataSync',
        exported: 'false'
      });
      manifest = result.content;
      if (result.changed) fixes.push('Declared WorkManager SystemForegroundService as dataSync');
    }

    // Android 13+ notification permission: adding the declaration is safe;
    // runtime permission remains the app developer\'s responsibility.
    if (/NotificationManager|NotificationCompat|\.notify\s*\(/.test(source) && /targetSdk(?:Version)?\s*[=:]\s*3[3-9]|targetSdk\s*3[3-9]/.test(source)) {
      const before = manifest;
      manifest = addManifestPermission(manifest, 'android.permission.POST_NOTIFICATIONS');
      if (manifest !== before) fixes.push('Added POST_NOTIFICATIONS permission for notification-using app');
    }

    // Android 12+: components with intent-filters need android:exported.
    // Only auto-fix the unambiguous case: launcher activity => true.
    manifest = manifest.replace(/<(activity|receiver|service)(\b[^>]*?)>([\s\S]*?)<\/\1>/gi, (whole, type, attrs, body) => {
      if (/android:exported\s*=/.test(attrs) || !/<intent-filter\b/i.test(body)) return whole;
      if (type.toLowerCase() === 'activity' && /android\.intent\.action\.MAIN|android\.intent\.category\.LAUNCHER/i.test(body)) {
        fixes.push('Added android:exported="true" to launcher activity');
        return `<${type}${attrs} android:exported="true">${body}</${type}>`;
      }
      return whole;
    });

    if (manifest !== original) fs.writeFileSync(manifestPath, manifest, 'utf8');
  }

  // Normalize obvious JVM 1.8 settings because this CI uses JDK 17. This is
  // deliberately limited to compiler target declarations, not arbitrary code.
  const gradleFiles = listProjectFiles(projectRoot, (full, name) => /build\.gradle(?:\.kts)?$/.test(name));
  for (const file of gradleFiles) {
    let c = fs.readFileSync(file, 'utf8');
    const before = c;
    c = c.replace(/JavaVersion\.VERSION_1_8/g, 'JavaVersion.VERSION_17');
    c = c.replace(/jvmTarget\s*=\s*["']1\.8["']/g, 'jvmTarget = "17"');
    c = c.replace(/jvmTarget\.set\(["']1\.8["']\)/g, 'jvmTarget.set("17")');
    if (c !== before) {
      fs.writeFileSync(file, c, 'utf8');
      fixes.push(`Normalized Java/Kotlin JVM target to 17 in ${path.relative(projectRoot, file)}`);
    }
  }

  // `outputFileName.set("x")` inside applicationVariants.all { variant.outputs.all { ... } }
  // is a hard configuration failure ("No signature of method:
  // java.lang.String.set()"): in that legacy variant API outputFileName is a
  // plain String, not a Property, so it must be assigned. (`.set` only exists
  // on the new androidComponents/Property-based APIs, which have no
  // outputFileName.) Generic rewrite, independent of any project's names.
  for (const file of gradleFiles) {
    const c = fs.readFileSync(file, 'utf8');
    const fixed = doctorFixLegacyOutputFileNameSet(c);
    if (fixed !== c) {
      fs.writeFileSync(file, fixed, 'utf8');
      fixes.push(`Fixed legacy variant API misuse in ${path.relative(projectRoot, file)}: outputFileName.set(...) -> outputFileName = ... (outputFileName is a String there, not a Property)`);
    }
  }

  // Missing MaterialComponents/AppCompat theme dependency, caught *before*
  // CI runs instead of waiting for AAPT to fail with "resource style/Theme.X
  // not found". See fixMissingThemeLibraryDependency's own header for how.
  fixes.push(...fixMissingThemeLibraryDependency(projectRoot));

  // WorkManager.enqueue(null) is a compile error, not just a bug: enqueue()
  // is overloaded as both enqueue(WorkRequest) and
  // enqueue(List<? extends WorkRequest>), and a bare `null` argument
  // matches both equally well, so javac/kotlinc refuses to pick one
  // ("reference to enqueue is ambiguous"). A blind cast to WorkRequest gets
  // the project compiling but leaves a guaranteed runtime NullPointerException
  // in place -- enqueue() still receives an actual null. So: build a real
  // WorkRequest instead, but ONLY when it can be done without guessing --
  // i.e. exactly one Worker/ListenableWorker/CoroutineWorker subclass is
  // defined in that SAME file (so it's referenceable by simple name with no
  // import to manage, and there's no ambiguity about which Worker was
  // meant). Any other shape -- zero Worker subclasses in the file, or more
  // than one -- falls back to the old cast-only fix: still unblocks CI, but
  // doesn't invent which class to enqueue.
  const WORKER_CLASS_RE = {
    java: /\bclass\s+(\w+)\s+extends\s+(?:Worker|ListenableWorker|CoroutineWorker)\b/g,
    kt: /\bclass\s+(\w+)\s*(?:\([^)]*\))?\s*:\s*(?:Worker|ListenableWorker|CoroutineWorker)\b/g
  };
  for (const file of listProjectFiles(projectRoot, (full, name) => /\.(java|kt|kts)$/.test(name))) {
    let c = fs.readFileSync(file, 'utf8');
    if (!/\.enqueue\(\s*null\s*\)/.test(c)) continue;
    const before = c;
    const isJava = file.endsWith('.java');
    const classRe = isJava ? WORKER_CLASS_RE.java : WORKER_CLASS_RE.kt;
    const workerNames = [...c.matchAll(classRe)].map((m) => m[1]);
    const uniqueNames = [...new Set(workerNames)];

    if (uniqueNames.length === 1) {
      const workerClass = uniqueNames[0];
      // Fully-qualified so this never depends on (or has to manage) an
      // existing import in the file.
      const replacement = isJava
        ? `.enqueue(new androidx.work.OneTimeWorkRequest.Builder(${workerClass}.class).build())`
        : `.enqueue(androidx.work.OneTimeWorkRequest.Builder(${workerClass}::class.java).build())`;
      c = c.replace(/\.enqueue\(\s*null\s*\)/g, replacement);
      if (c !== before) {
        fs.writeFileSync(file, c, 'utf8');
        fixes.push(`Replaced WorkManager.enqueue(null) with a real OneTimeWorkRequest for ${workerClass} in ${path.relative(projectRoot, file)} (the only Worker subclass in that file -- enqueue(null) compiled after a bare overload-disambiguating cast but threw a NullPointerException at runtime the moment it ran)`);
      }
    } else {
      // Zero or multiple Worker subclasses in this file -- no safe single
      // candidate to construct a request for, so only unblock compilation,
      // same as before.
      c = c.replace(/\.enqueue\(\s*null\s*\)/g, '.enqueue((androidx.work.WorkRequest) null)');
      if (c !== before) {
        fs.writeFileSync(file, c, 'utf8');
        fixes.push(`Resolved ambiguous WorkManager.enqueue(null) overload in ${path.relative(projectRoot, file)} (cast to WorkRequest -- compiles, but enqueuing a real null will still throw at runtime; pass an actual WorkRequest instead -- couldn't auto-generate one because this file has ${uniqueNames.length === 0 ? 'no' : 'more than one'} Worker subclass, so which one was intended is ambiguous)`);
      }
    }
  }

  // WorkManager's default WorkerFactory can only construct a Worker/
  // ListenableWorker/CoroutineWorker subclass it can actually see, so
  // androidx.work requires that subclass be `public` -- a package-private
  // (Java) or non-public (Kotlin `internal`/`private`) Worker compiles fine
  // but fails lint's fatal WorkerHasAPublicModifier check (and would crash
  // at runtime if it somehow slipped through, since WorkerFactory can't
  // reach a class it can't see). Purely a visibility-modifier fix -- never
  // touches the class body/logic, matching the same "unblock CI, don't
  // rewrite app logic" contract as the enqueue(null) fix above.
  for (const file of listProjectFiles(projectRoot, (full, name) => /\.java$/.test(name))) {
    let c = fs.readFileSync(file, 'utf8');
    const before = c;
    c = c.replace(
      /(^|[\r\n])([ \t]*)((?:(?:private|protected|static|final|abstract)\s+)*)(class\s+\w+\s+extends\s+(?:Worker|ListenableWorker|CoroutineWorker)\b)/g,
      (whole, pre, indent, modifiers, decl) => (/\bpublic\b/.test(modifiers) ? whole : `${pre}${indent}public ${modifiers}${decl}`)
    );
    if (c !== before) {
      fs.writeFileSync(file, c, 'utf8');
      fixes.push(`Added missing public modifier to a Worker subclass in ${path.relative(projectRoot, file)} (WorkManager's default WorkerFactory can only construct public Worker/ListenableWorker/CoroutineWorker subclasses)`);
    }
  }
  // Same rule for Kotlin -- classes are public by default there, so the fix
  // is dropping an explicit `internal`/`private` rather than adding a
  // keyword. Constructor args (`class Foo(ctx: Context) : Worker(...)`) are
  // matched but left untouched; only the visibility modifier is removed.
  for (const file of listProjectFiles(projectRoot, (full, name) => /\.kts?$/.test(name))) {
    let c = fs.readFileSync(file, 'utf8');
    const before = c;
    c = c.replace(
      /(^|[\r\n])([ \t]*)(?:internal|private)\s+(class\s+\w+\s*(?:\([^)]*\))?\s*:\s*(?:Worker|ListenableWorker|CoroutineWorker)\b)/g,
      (whole, pre, indent, rest) => `${pre}${indent}${rest}`
    );
    if (c !== before) {
      fs.writeFileSync(file, c, 'utf8');
      fixes.push(`Removed internal/private modifier from a Worker subclass in ${path.relative(projectRoot, file)} (WorkManager's default WorkerFactory can only construct public Worker/ListenableWorker/CoroutineWorker subclasses; Kotlin classes are public by default once that modifier is gone)`);
    }
  }

  // "Illegal escape: '\/'" -- forward slash is never a recognized escape
  // character in Java or Kotlin string/char literals (unlike JSON, where
  // \/ is a legal, purely optional way to escape a slash). This almost
  // always comes from pasting a JSON-escaped URL -- e.g.
  // "https:\/\/example.com" -- straight into source, since that's valid
  // JSON but not valid Kotlin/Java. Replacing \/ with a literal / never
  // changes the string's value (both decode to the same character), so
  // this is a pure, always-safe simplification -- not a guess about intent
  // the way the enqueue(null) fix above has to be.
  //
  // Only touches text actually inside a string or char literal (matched
  // with escape-aware regexes, not just "contains \/ anywhere"), and
  // Kotlin's triple-quoted raw strings are protected first and restored
  // untouched afterward, since raw strings don't interpret escapes at all
  // -- a literal \/ in one is real content, not an invalid escape.
  for (const file of listProjectFiles(projectRoot, (full, name) => /\.(java|kt|kts)$/.test(name))) {
    let c = fs.readFileSync(file, 'utf8');
    if (!c.includes('\\/')) continue;
    const before = c;

    const rawStrings = [];
    c = c.replace(/"""[\s\S]*?"""/g, (m) => {
      rawStrings.push(m);
      return `\u0000RAWSTR${rawStrings.length - 1}\u0000`;
    });
    c = c.replace(/"(?:\\.|[^"\\\n])*"/g, (str) => str.replace(/\\\//g, '/'));
    c = c.replace(/'\\\/'/g, "'/'");
    c = c.replace(/\u0000RAWSTR(\d+)\u0000/g, (_, i) => rawStrings[Number(i)]);

    if (c !== before) {
      fs.writeFileSync(file, c, 'utf8');
      fixes.push(`Removed invalid '\\/' escape sequence(s) in ${path.relative(projectRoot, file)} (forward slash never needs escaping in Java/Kotlin -- likely a JSON-escaped URL pasted directly into a string literal; simplified to a literal / with no change in meaning)`);
    }
  }

  // "Incorrect template entry" -- and the long cascade of unrelated-looking
  // "Expecting '}'" / "Unresolved reference" / "Only expressions are
  // allowed in this context" errors that follow it in the same file.
  // Kotlin requires a '$' inside a double-quoted string literal to be
  // followed by either a valid identifier-start character ($name) or '{'
  // (${expr}). A '$' meant as a literal character -- most often a currency
  // symbol, or a filename/display-name string built from user-supplied
  // text -- has to be escaped as \$; left bare, kotlinc rejects it and then
  // loses its place in the file, so a single stray character produces a
  // dozen unrelated-looking errors after it. Escaping a bare '$' can never
  // change what the string was trying to say (one that wasn't already a
  // valid template couldn't have compiled at all), so this is as safe as
  // the \/ fix just above -- same raw-string protection, for the same
  // reason (Kotlin raw strings escape a literal '$' as ${'$'} instead,
  // which this deliberately leaves alone).
  for (const file of listProjectFiles(projectRoot, (full, name) => /\.(kt|kts)$/.test(name))) {
    // A lone '$' is valid Kotlin, so only touch it when the compiler really reported a template error.
    if (!/Incorrect template entry/i.test(log || '')) break;
    let c = fs.readFileSync(file, 'utf8');
    if (!c.includes('$')) continue;
    const before = c;

    const rawStrings = [];
    c = c.replace(/"""[\s\S]*?"""/g, (m) => {
      rawStrings.push(m);
      return `\u0000RAWSTR${rawStrings.length - 1}\u0000`;
    });
    c = c.replace(/"(?:\\.|[^"\\\n])*"/g, (str) => {
      const inner = str.slice(1, -1);
      const fixedInner = inner.replace(/(?<!\\)\$(?![A-Za-z_{])/g, '\\$');
      return `"${fixedInner}"`;
    });
    c = c.replace(/\u0000RAWSTR(\d+)\u0000/g, (_, i) => rawStrings[Number(i)]);

    if (c !== before) {
      fs.writeFileSync(file, c, 'utf8');
      fixes.push(`Escaped a stray unescaped '$' in a Kotlin string literal in ${path.relative(projectRoot, file)} (a '$' not followed by an identifier or '{' is a guaranteed "Incorrect template entry" compile error, and produces a long cascade of unrelated-looking errors afterward in the same file)`);
    }
  }

  // "'setProgress' hides member of supertype 'Activity' and needs an 'override' modifier": the app declares its
  // own function with the name (and parameters) of a final/inherited Activity method (setProgress, setTitle...).
  // It can't be overridden, so rename the app's own function and its unqualified calls inside that file.
  if (log) {
    const ktAll = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
    const done = new Set();
    for (const e of doctorKtLogErrors(log)) {
      const hm = /'([A-Za-z_]\w*)' hides member of supertype '[^']+' and needs an 'override' modifier/i.exec(e.msg);
      if (!hm) continue;
      const abs = doctorResolveLogFile(e.file, ktAll);
      if (!abs) continue;
      const name = hm[1];
      const key = abs + ':' + name;
      if (done.has(key)) continue;
      done.add(key);
      let c = fs.readFileSync(abs, 'utf8');
      const nn = name + 'Ui';
      if (new RegExp('\\b' + nn + '\\b').test(c)) continue; // name already taken, leave it to the person
      const re = new RegExp('(?<![\\w.$])' + name + '(?=\\s*\\()|(?<=\\bthis\\.)' + name + '(?=\\s*\\()|(?<=::)' + name + '\\b', 'g');
      const out = c.replace(re, nn);
      if (out !== c) {
        fs.writeFileSync(abs, out, 'utf8');
        fixes.push(`Renamed the app's own function ${name}() to ${nn}() in ${path.relative(projectRoot, abs)} (Activity already has a final ${name}, so it can't be redeclared)`);
      }
    }
  }

  // Remove invalid XML comments everywhere Android's resource merger can see.
  for (const file of listProjectFiles(projectRoot, (full, name) => name.endsWith('.xml') && (full.includes(`${path.sep}res${path.sep}`) || name === 'AndroidManifest.xml'))) {
    let c = fs.readFileSync(file, 'utf8');
    const cleaned = c.replace(/<!--[\s\S]*?-->/g, m => m.includes('--') ? '' : m);
    if (cleaned !== c) {
      fs.writeFileSync(file, cleaned, 'utf8');
      fixes.push(`Removed invalid XML comments from ${path.relative(projectRoot, file)}`);
    }
  }

  // VectorDrawable only supports <path>, <group>, and <clip-path> inside a
  // <vector> root -- there is no <circle> element, even though
  // android:cx/android:cy/android:r read like plausible attributes (they're
  // valid in plain SVG, which is presumably where the confusion comes from).
  // AAPT fails resource linking with "attribute android:cx not found" (and
  // the same for cy/r) rather than silently ignoring it. The fix is
  // mechanical and lossless -- a circle of radius r centered at (cx,cy) is
  // exactly two semicircular arcs, so this rewrites each <circle .../> into
  // an equivalent <path pathData="..."/> and carries over any other
  // attributes (fillColor, strokeColor, etc.) unchanged. Only applies when
  // cx/cy/r are all present and numeric; anything else is left alone rather
  // than guessed at.
  for (const file of listProjectFiles(projectRoot, (full, name) => name.endsWith('.xml') && full.includes(`${path.sep}drawable`))) {
    let c = fs.readFileSync(file, 'utf8');
    if (!/<circle\b/.test(c)) continue;
    const before = c;
    c = c.replace(/<circle\b([^>]*)\/>/g, (whole, attrs) => {
      const getAttr = (name) => {
        const m = attrs.match(new RegExp(`android:${name}\\s*=\\s*"([^"]+)"`));
        return m ? parseFloat(m[1]) : null;
      };
      const cx = getAttr('cx');
      const cy = getAttr('cy');
      const r = getAttr('r');
      if (cx === null || cy === null || r === null || Number.isNaN(cx) || Number.isNaN(cy) || Number.isNaN(r)) {
        return whole; // missing/non-numeric geometry -- not safe to guess, leave for a human
      }
      const otherAttrs = attrs.replace(/android:(cx|cy|r)\s*=\s*"[^"]*"/g, '').replace(/\s+/g, ' ').trim();
      const pathData = `M${cx - r},${cy} A${r},${r} 0 1 0 ${cx + r},${cy} A${r},${r} 0 1 0 ${cx - r},${cy} Z`;
      return `<path android:pathData="${pathData}"${otherAttrs ? ' ' + otherAttrs : ''}/>`;
    });
    if (c !== before) {
      fs.writeFileSync(file, c, 'utf8');
      fixes.push(`Converted <circle> element(s) to VectorDrawable-compatible <path> in ${path.relative(projectRoot, file)} (AAPT does not support <circle> inside a vector drawable)`);
    }
  }

  // android:fillColor="none" (or "transparent") is valid in plain SVG but
  // not in Android resources: color attributes only accept a #hex color, a
  // @color reference or a ?attr theme reference. AAPT fails resource linking
  // with "'none' is incompatible with attribute fillColor (attr) color" --
  // reported once per affected file, so a single bad path in an adaptive
  // icon fails ic_launcher.xml, ic_launcher_foreground.xml and the mipmap
  // copy together. "#00000000" is fully transparent ARGB, i.e. exactly what
  // "none" means, so the swap is lossless. Only the literal keywords are
  // touched (case-insensitive), and only fillColor/strokeColor, so every real
  // color -- including the auto-detected launcher accent color -- passes
  // through unchanged. Covers drawable*/ and mipmap*/ in any module (app,
  // Flutter's android/app, library modules).
  for (const file of listProjectFiles(projectRoot, (full, name) =>
    name.endsWith('.xml') && /[\\/]res[\\/](?:drawable|mipmap)[^\\/]*[\\/][^\\/]+$/.test(full))) {
    let c = fs.readFileSync(file, 'utf8');
    if (!/android:(?:fillColor|strokeColor)\s*=\s*"\s*(?:none|transparent)\s*"/i.test(c)) continue;
    const cleaned = c.replace(
      /(android:(?:fillColor|strokeColor)\s*=\s*)"\s*(?:none|transparent)\s*"/gi,
      '$1"#00000000"'
    );
    if (cleaned !== c) {
      fs.writeFileSync(file, cleaned, 'utf8');
      fixes.push(`Replaced fillColor/strokeColor="none" with "#00000000" (fully transparent) in ${path.relative(projectRoot, file)} (AAPT rejects the SVG-style keyword "none" for Android color attributes)`);
    }
  }

  // R.layout.spinner_item does not exist -- there is no default spinner_item
  // layout generated into a project's own R class. The layout developers
  // actually mean is one of Android's built-in framework layouts, reached via
  // android.R.layout.* instead of the project's own R.layout.*:
  // android.R.layout.simple_spinner_item (row layout) and
  // android.R.layout.simple_spinner_dropdown_item (dropdown row layout).
  // This is a pure "wrong R class" typo -- javac fails with "cannot find
  // symbol" because R.layout genuinely has no spinner_item member -- so
  // qualifying it with android. is always the fix, never a guess: nothing
  // else could have been meant by a bare `spinner_item` reference, since the
  // project never declares that layout itself. Dropdown usage (passed to a
  // second call like setDropDownViewResource, or appearing a second time in
  // the same statement/line) maps to the dropdown variant; a bare first use
  // maps to the plain row layout.
  for (const file of listProjectFiles(projectRoot, (full, name) => /\.(java|kt|kts)$/.test(name))) {
    let c = fs.readFileSync(file, 'utf8');
    if (!/\bR\.layout\.spinner_item\b/.test(c)) continue;
    const before = c;
    c = c.replace(/\bR\.layout\.spinner_item\b/g, (whole, offset) => {
      const lineStart = c.lastIndexOf('\n', offset) + 1; // 0 if no preceding newline
      const lineEndIdx = c.indexOf('\n', offset);
      const fullLine = c.slice(lineStart, lineEndIdx === -1 ? undefined : lineEndIdx);
      const isDropdown = /setDropDownViewResource|dropdown/i.test(fullLine);
      return isDropdown ? 'android.R.layout.simple_spinner_dropdown_item' : 'android.R.layout.simple_spinner_item';
    });
    if (c !== before) {
      fs.writeFileSync(file, c, 'utf8');
      fixes.push(`Qualified R.layout.spinner_item as android.R.layout.simple_spinner_item(/_dropdown_item) in ${path.relative(projectRoot, file)} (spinner_item is a built-in Android framework layout, not a project resource -- the project's own R.layout has no such member)`);
    }
  }

  // `new int[][]{ new int[]{ X } }` where a plain int[] is expected (e.g.
  // passed to a method like setColor(int[]) or assigned to an int[]-typed
  // variable/field) is a doubled-up array literal: wrapping a single-element
  // int[] in an outer array produces int[][], not int[]. This is almost
  // always caused by copy-pasting a color-state-list-style nested array
  // (int[][] states) into a spot that only ever wanted the flat color list.
  // The fix is lossless and unambiguous only in the specific shape "an outer
  // array literal containing exactly one inner int[] literal" -- collapsing
  // it to that single inner array preserves every element with no
  // reordering or loss, and multi-element outer arrays (a real int[][], e.g.
  // an actual set of state-color pairs) are deliberately left untouched
  // since collapsing those would discard data, not just unwrap it.
  for (const file of listProjectFiles(projectRoot, (full, name) => /\.(java|kt|kts)$/.test(name))) {
    let c = fs.readFileSync(file, 'utf8');
    const before = c;
    c = c.replace(/new\s+int\[\]\[\]\s*\{\s*(new\s+int\[\]\s*\{[^{}]*\})\s*\}/g, (whole, inner) => inner);
    if (c !== before) {
      fs.writeFileSync(file, c, 'utf8');
      fixes.push(`Collapsed a doubled-up "new int[][]{ new int[]{...} }" literal to a plain int[] in ${path.relative(projectRoot, file)} (wrapping a single int[] in an outer array produced int[][] where an int[] was expected -- unwrapped since the outer array held exactly one element, so no data was discarded)`);
    }
  }

  // Native (NDK) builds that vendor a dependency as a git submodule (e.g.
  // ShadowHook under jni/) fail with a C/C++ "file not found" on the
  // vendored header whenever the submodule wasn't checked out -- a plain
  // `actions/checkout` (or a zip/tarball upload) leaves the submodule
  // directory present but empty. This is safe to auto-repair: it only
  // populates directories the project's own .gitmodules already declares,
  // it never touches application source, and if the repo isn't a git
  // checkout (so submodules can't be initialized here) it degrades to a
  // diagnostic instead of silently doing nothing.
  if (fs.existsSync(path.join(projectRoot, '.gitmodules'))) {
    const gitmodules = fs.readFileSync(path.join(projectRoot, '.gitmodules'), 'utf8');
    const submodulePaths = [...gitmodules.matchAll(/^\s*path\s*=\s*(.+)\s*$/gm)].map(m => m[1].trim());
    const uninitialized = submodulePaths.filter(p => {
      const full = path.join(projectRoot, p);
      try { return fs.readdirSync(full).length === 0; } catch { return true; }
    });
    if (uninitialized.length && fs.existsSync(path.join(projectRoot, '.git'))) {
      const { spawnSync } = require('child_process');
      const result = spawnSync('git', ['submodule', 'update', '--init', '--recursive'], {
        cwd: projectRoot,
        encoding: 'utf8',
      });
      if (result.status === 0) {
        fixes.push(`Initialized empty git submodule(s) required by the native build: ${uninitialized.join(', ')} (declared in .gitmodules but not checked out)`);
      } else {
        fixes.push(`Build Doctor warnings: found uninitialized git submodule(s) (${uninitialized.join(', ')}) but 'git submodule update --init --recursive' failed: ${(result.stderr || result.error || '').toString().split('\n')[0]}`);
      }
    } else if (uninitialized.length) {
      fixes.push(`Build Doctor warnings: git submodule(s) appear uninitialized (${uninitialized.join(', ')}) but no .git directory was found to repair them from -- ensure checkout fetches submodules (e.g. actions/checkout with submodules: recursive)`);
    }
  }

  // Capacitor/Cordova native module projects (capacitor-android,
  // capacitor-cordova-android-plugins, or any other capacitor-*/cordova-*
  // plugin project) get `include`d in settings.gradle, but they physically
  // live under node_modules and don't exist in a plain zip/git upload until
  // `npx cap sync android` has actually run. When it hasn't, Gradle doesn't
  // fail with an obvious missing-file error -- it fails much later, deep in
  // dependency resolution, with "No matching variant ... No variants exist",
  // which reads like a version/attribute mismatch (see kb-0801 above) and
  // sends a log-based AI repair chasing the wrong fix entirely. Catching it
  // here, before the project is even pushed to CI, means the repair chain
  // never has to guess at it from a confusing log in the first place.
  const settingsPathForNativePlugins = ['settings.gradle', 'settings.gradle.kts']
    .map(f => path.join(projectRoot, f))
    .find(f => fs.existsSync(f));
  if (settingsPathForNativePlugins) {
    const settingsText = fs.readFileSync(settingsPathForNativePlugins, 'utf8');
    const includedNames = [...settingsText.matchAll(/include\s*\(?\s*['"]:([\w-]+)['"]/g)].map(m => m[1]);
    const nativePluginNames = includedNames.filter(n => /capacitor|cordova/i.test(n));
    const missingPluginDirs = nativePluginNames.filter(name => {
      // Capacitor's generated settings snippet usually overrides projectDir
      // explicitly (it doesn't live at the conventional ./<name> path); fall
      // back to that convention only when no override is declared.
      const dirOverride = settingsText.match(
        new RegExp(`project\\(['"]:${name}['"]\\)\\.projectDir\\s*=\\s*new File\\(['"]([^'"]+)['"]`)
      );
      const resolvedDir = dirOverride ? path.resolve(projectRoot, dirOverride[1]) : path.join(projectRoot, name);
      return !fs.existsSync(path.join(resolvedDir, 'build.gradle')) &&
        !fs.existsSync(path.join(resolvedDir, 'build.gradle.kts'));
    });
    if (missingPluginDirs.length) {
      // Only attempt the actual sync when node_modules/@capacitor/android is
      // already present -- `cap sync` reads from node_modules, it doesn't
      // install it. If it's missing (the common case for a bare zip upload
      // with no node_modules/), running `npm install` here first would be a
      // slow, network-dependent step on every doctor pass, so this degrades
      // to a diagnostic instead rather than risking a hung/slow job.
      if (fs.existsSync(path.join(projectRoot, 'node_modules', '@capacitor', 'android'))) {
        const { spawnSync } = require('child_process');
        const result = spawnSync('npx', ['cap', 'sync', 'android'], {
          cwd: projectRoot,
          encoding: 'utf8',
          timeout: 5 * 60 * 1000,
        });
        if (result.status === 0) {
          fixes.push(`Ran 'npx cap sync android' to populate missing native plugin module(s): ${missingPluginDirs.join(', ')} (settings.gradle included them but their directories had no build.gradle)`);
        } else {
          fixes.push(`Build Doctor warnings: settings.gradle includes Capacitor/Cordova module(s) with no build.gradle (${missingPluginDirs.join(', ')}), and 'npx cap sync android' failed to populate them: ${(result.stderr || result.error || '').toString().split('\n')[0]}`);
        }
      } else {
        fixes.push(`Build Doctor warnings: settings.gradle includes Capacitor/Cordova module(s) with no build.gradle (${missingPluginDirs.join(', ')}) -- node_modules/@capacitor/android isn't present in this upload, so Build Doctor can't safely run 'npx cap sync android' here. Run 'npm install && npx cap sync android' locally before uploading, or add an npm install + cap sync step to the CI workflow before Gradle runs.`);
      }
    }
  }

  // Flag-only diagnostics are recorded in the job message, but are not
  // destructive auto-fixes. The build itself remains the final authority.
  const diagnostics = [];
  // Matches both Kotlin-DSL style ("compileSdk = 35") and Groovy-DSL style
  // ("compileSdk 35", no '=') -- the original \s*= here only caught the
  // former, so any plain Groovy build.gradle (the more common case) tripped
  // a false "could not find compileSdk" warning even when it was right
  // there in the file.
  if (/uses-sdk|compileSdkVersion|compileSdk\s*[=\s]/.test(source) === false && /com\.android\.application/.test(source)) {
    diagnostics.push('Could not confidently find compileSdk in the uploaded Gradle project');
  }
  if (/google-services\.json/.test(source) && !fs.existsSync(path.join(projectRoot, 'app', 'google-services.json'))) {
    diagnostics.push('Firebase references detected but app/google-services.json may be missing');
  }
  if (diagnostics.length) fixes.push(`Build Doctor warnings: ${diagnostics.join(' | ')}`);

  // Both rules below react to the ACTUAL failing build log (only available
  // on a repair retry, not the first pre-build pass -- see the call site
  // comment above). Each is a no-op when log is empty.
  fixes.push(...fixMissingResourceStub(log, projectRoot));
  fixes.push(...fixNeverThrownCatch(log, projectRoot));
  // Unlike the two rules above (which react to a specific known failure
  // shape), this one generalizes: any "Could not find <group>:<artifact>:
  // <version>" dependency-resolution failure gets a shot at an automatic,
  // evidence-based correction before the job burns an AI-repair attempt on
  // it. See fixUnresolvableDependencyCoordinates for exactly how "evidence-
  // based" is enforced.
  fixes.push(...(await fixUnresolvableDependencyCoordinates(log, projectRoot)));
  fixes.push(...fixAndroidModuleConfig(log, projectRoot));
  fixes.push(...fixDisabledBuildFeatures(log, projectRoot));
  fixes.push(...fixHumanAndAiMistakes(log, projectRoot));
  fixes.push(...fixGradleWrapperFromLog(log, projectRoot));
  // Log-independent: runs on the first pre-build pass too, so a project that
  // ships ic_launcher.png + ic_launcher.webp never reaches Gradle broken.
  fixes.push(...dedupeRasterResources(projectRoot));
  // Also log-independent: a res/ file and a values/*.xml entry with the same
  // type+name ("Duplicate resources"), and Flutter's deleted v1 embedding.
  for (const step of [() => fixFileVsValueDuplicateResources(projectRoot), () => fixFlutterV1Embedding(projectRoot)]) {
    try { fixes.push(...step()); } catch (e) { fixes.push(`Build Doctor warnings: a repair rule threw and was skipped (${String(e && e.message || e).split('\n')[0]})`); }
  }

  return fixes;
}

function fixDisabledBuildFeatures(log, projectRoot) {
  const fixes = [];
  const logText = String(log || '');
  const manifests = listProjectFiles(projectRoot, (full, name) =>
    name === 'AndroidManifest.xml' && path.basename(path.dirname(full)) === 'main' && path.basename(path.dirname(path.dirname(full))) === 'src'
  );
  for (const manifest of manifests) {
    const moduleDir = path.dirname(path.dirname(path.dirname(manifest)));
    const gradleFile = ['build.gradle', 'build.gradle.kts'].map(f => path.join(moduleDir, f)).find(f => fs.existsSync(f));
    if (!gradleFile) continue;
    let gradle = fs.readFileSync(gradleFile, 'utf8');
    if (!/com\.android\.application|com\.android\.library|id\s*\(?\s*['"]android/.test(gradle)) continue;
    const isKts = gradleFile.endsWith('.kts');

    const aidlFiles = listProjectFiles(moduleDir, (full, name) => name.endsWith('.aidl'));
    const srcFiles = listProjectFiles(moduleDir, (full, name) => /\.(java|kt)$/.test(name));
    const srcText = srcFiles.map(f => { try { return fs.readFileSync(f, 'utf8'); } catch { return ''; } }).join('\n');
    const layoutFiles = listProjectFiles(moduleDir, (full, name) => name.endsWith('.xml') && /[\\/]res[\\/]layout[^\\/]*[\\/]/.test(full));
    const hasDataBindingLayout = layoutFiles.some(f => { try { return /^\s*(<\?xml[^>]*\?>\s*)?<layout[\s>]/.test(fs.readFileSync(f, 'utf8')); } catch { return false; } });
    const usesGeneratedBinding = /import\s+[\w.]+\.databinding\.\w+Binding\b/.test(srcText) || /\bDataBindingUtil\b/.test(srcText) ||
      /symbol:\s+class\s+\w+Binding\b|Unresolved reference:\s*\w*Binding\b|package [\w.]*databinding does not exist/.test(logText);
    const usesViewBindingInflate = /\b\w+Binding\.(inflate|bind)\s*\(/.test(srcText);

    const wants = [];
    if (aidlFiles.length > 0) wants.push({ key: 'aidl', why: `${aidlFiles.length} .aidl file(s) found` });
    if (/\bBuildConfig\b/.test(srcText) || /variable BuildConfig|Unresolved reference:\s*BuildConfig/.test(logText)) wants.push({ key: 'buildConfig', why: 'source references BuildConfig' });
    if (usesGeneratedBinding || usesViewBindingInflate) {
      if (hasDataBindingLayout || /\bDataBindingUtil\b/.test(srcText)) wants.push({ key: 'dataBinding', why: 'layout(s) use a <layout> data-binding root' });
      else wants.push({ key: 'viewBinding', why: 'source uses generated *Binding classes' });
    }
    if (/\bresValue\s*\(?\s*['"]/.test(gradle)) wants.push({ key: 'resValues', why: 'build.gradle calls resValue(...)' });
    if (/android\.renderscript\.|\bRenderScript\b/.test(srcText)) wants.push({ key: 'renderScript', why: 'source uses RenderScript' });

    const missing = wants.filter(w => !new RegExp(`\\b${w.key}\\s*(=\\s*)?true\\b`).test(gradle));

    // AIDL sources present but Java still can't see the generated class: an
    // .aidl with no `package` line lives in the unnamed package, which
    // named-package Java classes can never import. Flag it, don't guess.
    for (const f of aidlFiles) {
      const txt = fs.readFileSync(f, 'utf8');
      if (!/^\s*package\s+[\w.]+\s*;/m.test(txt) && /cannot find symbol[\s\S]{0,200}class I\w+|package I\w+ does not exist/.test(logText)) {
        fixes.push(`Build Doctor warnings: ${path.relative(projectRoot, f)} has no 'package' declaration, so its generated class is in the unnamed package and can't be imported from com.* classes -- add a matching package line (and move it to the matching aidl/<package path>/ folder).`);
      }
    }
    if (!missing.length) continue;

    const lines = missing.map(w => (isKts ? `${w.key} = true` : `${w.key} true`));
    const featuresRe = /(buildFeatures\s*\{)/;
    if (featuresRe.test(gradle)) {
      // Drop any explicit "= false" for the features we are enabling, then add ours.
      for (const w of missing) gradle = gradle.replace(new RegExp(`^[ \\t]*${w.key}\\s*(=\\s*)?false\\s*$`, 'gm'), '');
      gradle = gradle.replace(featuresRe, `$1\n        ${lines.join('\n        ')}`);
    } else if (/(^|\n)\s*android\s*\{/.test(gradle)) {
      gradle = gradle.replace(/((?:^|\n)\s*android\s*\{)/, `$1\n    buildFeatures {\n        ${lines.join('\n        ')}\n    }\n`);
    } else {
      continue;
    }
    fs.writeFileSync(gradleFile, gradle, 'utf8');
    fixes.push(`Enabled ${missing.map(w => 'buildFeatures.' + w.key).join(' + ')} in ${path.relative(projectRoot, gradleFile)} (AGP 8+ disables these by default; ${missing.map(w => w.why).join(', ')})`);
  }
  return fixes;
}

function fixAndroidModuleConfig(log, projectRoot) {
  const fixes = [];
  const logText = String(log || '');
  const manifests = listProjectFiles(projectRoot, (full, name) =>
    name === 'AndroidManifest.xml' && path.basename(path.dirname(full)) === 'main' && path.basename(path.dirname(path.dirname(full))) === 'src'
  );
  const needCompile = Math.max(0, ...[...logText.matchAll(/compile against version (\d+) or later/g)].map(m => +m[1]));
  const needMin = Math.max(0, ...[...logText.matchAll(/minSdkVersion\s+\d+\s+cannot be smaller than version (\d+) declared in library/g)].map(m => +m[1]));

  for (const manifestPath of manifests) {
    const moduleDir = path.dirname(path.dirname(path.dirname(manifestPath)));
    const gradleFile = ['build.gradle', 'build.gradle.kts'].map(f => path.join(moduleDir, f)).find(f => fs.existsSync(f));
    if (!gradleFile) continue;
    let gradle = fs.readFileSync(gradleFile, 'utf8');
    if (!/com\.android\.application|com\.android\.library|id\s*\(?\s*['"]android/.test(gradle)) continue;
    const isKts = gradleFile.endsWith('.kts');
    const rel = path.relative(projectRoot, gradleFile);
    const beforeGradle = gradle;

    let manifest = fs.readFileSync(manifestPath, 'utf8');
    const pkgMatch = manifest.match(/<manifest\b[^>]*?\spackage\s*=\s*["']([\w.]+)["']/);
    const nsMatch = gradle.match(/\bnamespace\s*(?:=\s*)?["']([\w.]+)["']/);

    if (!nsMatch) {
      const appIdMatch = gradle.match(/\bapplicationId\s*(?:=\s*)?["']([\w.]+)["']/);
      const ns = (pkgMatch && pkgMatch[1]) || (appIdMatch && appIdMatch[1]);
      if (ns && /(^|\n)\s*android\s*\{/.test(gradle)) {
        gradle = gradle.replace(/((?:^|\n)\s*android\s*\{)/, `$1\n    namespace ${isKts ? '= ' : ''}"${ns}"`);
        fixes.push(`Added missing namespace "${ns}" to ${rel} (AGP 8 requires it; taken from the project's own ${pkgMatch ? 'manifest package' : 'applicationId'})`);
        if (pkgMatch && pkgMatch[1] === ns) {
          manifest = manifest.replace(/(<manifest\b[^>]*?)\spackage\s*=\s*["'][\w.]+["']/, '$1');
          fs.writeFileSync(manifestPath, manifest, 'utf8');
          fixes.push(`Removed the now-redundant package="${ns}" attribute from ${path.relative(projectRoot, manifestPath)} (AGP 8 rejects it once namespace is set)`);
        }
      }
    } else if (pkgMatch && pkgMatch[1] === nsMatch[1]) {
      manifest = manifest.replace(/(<manifest\b[^>]*?)\spackage\s*=\s*["'][\w.]+["']/, '$1');
      fs.writeFileSync(manifestPath, manifest, 'utf8');
      fixes.push(`Removed the redundant package="${pkgMatch[1]}" attribute from ${path.relative(projectRoot, manifestPath)} (namespace already set in ${rel}; AGP 8 rejects both)`);
    }

    if (needCompile) {
      gradle = gradle.replace(/\b(compileSdk(?:Version)?)(\s*(?:=\s*)?)(\d+)\b/, (whole, key, sep, val) => {
        if (+val >= needCompile) return whole;
        fixes.push(`Raised ${key} ${val} -> ${needCompile} in ${rel} (a dependency states it must compile against ${needCompile}+)`);
        return `${key}${sep}${needCompile}`;
      });
    }
    if (needMin) {
      gradle = gradle.replace(/\b(minSdk(?:Version)?)(\s*(?:=\s*)?)(\d+)\b/, (whole, key, sep, val) => {
        if (+val >= needMin) return whole;
        fixes.push(`Raised ${key} ${val} -> ${needMin} in ${rel} (a dependency declares minSdk ${needMin}; this drops devices below API ${needMin})`);
        return `${key}${sep}${needMin}`;
      });
    }
    if (gradle !== beforeGradle) fs.writeFileSync(gradleFile, gradle, 'utf8');
  }
  return fixes;
}

const DOCTOR_ESC_RE = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

function fixStringResourceMistakes(projectRoot) {
  const fixes = [];
  const files = listProjectFiles(projectRoot, (full, name) => name.endsWith('.xml') && /[\\/]res[\\/]values[^\\/]*[\\/][^\\/]+$/.test(full)).sort();
  const seenAcross = new Map(); // "<dir>|<tag>|<name>" -> { file, norm }
  const DUP_TAGS = 'string|color|dimen|integer|bool|style|plurals|string-array|array|integer-array|declare-styleable';
  for (const file of files) {
    let c = fs.readFileSync(file, 'utf8');
    const orig = c;
    const rel = path.relative(projectRoot, file);
    const notes = [];

    // apostrophes, ampersands, multiple non-positional format args
    c = c.replace(/(<string\b([^>]*)>)([\s\S]*?)(<\/string>)/g, (whole, open, attrs, text, close) => {
      if (/<!\[CDATA\[/.test(text) || /\/\s*$/.test(attrs)) return whole;
      let t = text;
      if (!(/^\s*"/.test(t) && /"\s*$/.test(t))) {
        const t2 = t.replace(/(?<!\\)'/g, "\\'");
        if (t2 !== t) { notes.push('escaped an unescaped apostrophe (\\\')'); t = t2; }
      }
      const t3 = t.replace(/&(?!(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);)/g, '&amp;');
      if (t3 !== t) { notes.push('escaped a bare & as &amp;'); t = t3; }
      if (!/formatted\s*=\s*"false"/.test(attrs) && !/%\d+\$/.test(t)) {
        let n = 0;
        const spec = /%%|%([-#+0,(]*\d*(?:\.\d+)?[sdfxXeEcbBoOhHgGaAt])/g;
        const count = [...t.matchAll(spec)].filter(m => m[0] !== '%%').length;
        if (count >= 2) {
          t = t.replace(spec, (m, rest) => (m === '%%' ? m : `%${++n}$${rest}`));
          notes.push('made multiple %s/%d placeholders positional (%1$s, %2$s...)');
        }
      }
      return t === text ? whole : open + t + close;
    });

    // duplicate resources: same tag+name repeated in one file (keep first)
    const seenHere = new Set();
    const dupRe = new RegExp(`[ \\t]*<(${DUP_TAGS})\\b([^>]*?)\\bname\\s*=\\s*"([^"]+)"([^>]*?)(?:\\/>|>[\\s\\S]*?<\\/\\1>)[ \\t]*\\r?\\n?`, 'g');
    const dirKey = path.dirname(file);
    c = c.replace(dupRe, (whole, tag, a1, name, a2) => {
      if (/\bproduct\s*=/.test(a1 + a2)) return whole;
      const key = `${tag}|${name}`;
      if (seenHere.has(key)) { notes.push(`removed duplicate <${tag} name="${name}">`); return ''; }
      seenHere.add(key);
      const norm = whole.replace(/\s+/g, ' ').trim();
      const ak = `${dirKey}|${key}`;
      const prev = seenAcross.get(ak);
      if (prev && prev.file !== file) {
        if (prev.norm === norm) { notes.push(`removed <${tag} name="${name}"> (identical copy already in ${path.relative(projectRoot, prev.file)})`); seenHere.delete(key); return ''; }
        fixes.push(`Build Doctor warnings: <${tag} name="${name}"> is defined in both ${path.relative(projectRoot, prev.file)} and ${rel} with different content -- Android rejects duplicate resources; delete or rename one.`);
      } else if (!prev) seenAcross.set(ak, { file, norm });
      return whole;
    });

    if (c !== orig) {
      fs.writeFileSync(file, c, 'utf8');
      const uniq = [...new Set(notes)];
      fixes.push(`Fixed string-resource mistakes in ${rel}: ${uniq.join('; ')}`);
    }
  }
  return fixes;
}

function fixInvalidResourceFileNames(projectRoot) {
  const fixes = [];
  const resFiles = listProjectFiles(projectRoot, (full, name) =>
    /[\\/]src[\\/][^\\/]+[\\/]res[\\/](?!values)[^\\/]+[\\/][^\\/]+$/.test(full) && !name.startsWith('.'));
  const renames = []; // { type, oldBase, newBase }
  for (const file of resFiles) {
    const name = path.basename(file);
    const dot = name.indexOf('.');
    const base = dot === -1 ? name : name.slice(0, dot);
    const ext = dot === -1 ? '' : name.slice(dot);
    if (/^[a-z][a-z0-9_]*$/.test(base)) continue;
    let nb = base.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase().replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '');
    if (!nb) nb = 'res_file';
    if (!/^[a-z]/.test(nb)) nb = 'r_' + nb;
    const target = path.join(path.dirname(file), nb + ext);
    if (fs.existsSync(target)) {
      fixes.push(`Build Doctor warnings: ${path.relative(projectRoot, file)} has an invalid resource name (must be lowercase a-z, 0-9, _) but ${nb + ext} already exists in that folder -- rename it by hand.`);
      continue;
    }
    fs.renameSync(file, target);
    const type = path.basename(path.dirname(file)).split('-')[0];
    if (!renames.some(r => r.type === type && r.oldBase === base)) renames.push({ type, oldBase: base, newBase: nb });
    fixes.push(`Renamed invalid resource file ${path.relative(projectRoot, file)} -> ${nb + ext} (resource names may only use lowercase a-z, 0-9 and _)`);
  }
  if (renames.length) {
    const refFiles = listProjectFiles(projectRoot, (full, name) => /\.(java|kt|xml)$/.test(name));
    for (const f of refFiles) {
      let c = fs.readFileSync(f, 'utf8');
      const before = c;
      for (const r of renames) {
        c = c.replace(new RegExp(`@${r.type}/${DOCTOR_ESC_RE(r.oldBase)}(?![\\w.])`, 'g'), `@${r.type}/${r.newBase}`);
        c = c.replace(new RegExp(`\\bR\\.${r.type}\\.${DOCTOR_ESC_RE(r.oldBase)}(?![\\w])`, 'g'), `R.${r.type}.${r.newBase}`);
      }
      if (c !== before) fs.writeFileSync(f, c, 'utf8');
    }
  }
  return fixes;
}

function fixMissingXmlNamespaces(projectRoot) {
  const fixes = [];
  const NS = { android: 'http://schemas.android.com/apk/res/android', app: 'http://schemas.android.com/apk/res-auto', tools: 'http://schemas.android.com/tools' };
  const files = listProjectFiles(projectRoot, (full, name) =>
    name === 'AndroidManifest.xml' || (name.endsWith('.xml') && /[\\/]res[\\/][^\\/]+[\\/][^\\/]+$/.test(full)));
  for (const file of files) {
    const c = fs.readFileSync(file, 'utf8');
    const masked = c.replace(/<!--[\s\S]*?-->|<\?[\s\S]*?\?>|<!\[CDATA\[[\s\S]*?\]\]>/g, m => ' '.repeat(m.length));
    const root = masked.match(/<([A-Za-z_][\w.:-]*)/);
    if (!root) continue;
    const missing = Object.keys(NS).filter(p =>
      new RegExp(`\\s${p}:[\\w.-]+\\s*=`).test(masked) && !new RegExp(`xmlns:${p}\\s*=`).test(masked));
    if (!missing.length) continue;
    const at = root.index + root[0].length;
    const insert = missing.map(p => ` xmlns:${p}="${NS[p]}"`).join('');
    fs.writeFileSync(file, c.slice(0, at) + insert + c.slice(at), 'utf8');
    fixes.push(`Declared missing XML namespace(s) ${missing.map(p => 'xmlns:' + p).join(', ')} on the root element of ${path.relative(projectRoot, file)}`);
  }
  return fixes;
}

const SUPPORT_TO_ANDROIDX_CLASSES = {
  'android.support.v7.app.AppCompatActivity': 'androidx.appcompat.app.AppCompatActivity',
  'android.support.v7.app.AppCompatDelegate': 'androidx.appcompat.app.AppCompatDelegate',
  'android.support.v7.app.ActionBar': 'androidx.appcompat.app.ActionBar',
  'android.support.v7.app.AlertDialog': 'androidx.appcompat.app.AlertDialog',
  'android.support.v7.widget.Toolbar': 'androidx.appcompat.widget.Toolbar',
  'android.support.v7.widget.AppCompatButton': 'androidx.appcompat.widget.AppCompatButton',
  'android.support.v7.widget.AppCompatTextView': 'androidx.appcompat.widget.AppCompatTextView',
  'android.support.v7.widget.AppCompatImageView': 'androidx.appcompat.widget.AppCompatImageView',
  'android.support.v7.widget.AppCompatEditText': 'androidx.appcompat.widget.AppCompatEditText',
  'android.support.v7.widget.RecyclerView': 'androidx.recyclerview.widget.RecyclerView',
  'android.support.v7.widget.LinearLayoutManager': 'androidx.recyclerview.widget.LinearLayoutManager',
  'android.support.v7.widget.GridLayoutManager': 'androidx.recyclerview.widget.GridLayoutManager',
  'android.support.v7.widget.CardView': 'androidx.cardview.widget.CardView',
  'android.support.v4.app.Fragment': 'androidx.fragment.app.Fragment',
  'android.support.v4.app.FragmentActivity': 'androidx.fragment.app.FragmentActivity',
  'android.support.v4.app.FragmentManager': 'androidx.fragment.app.FragmentManager',
  'android.support.v4.app.FragmentTransaction': 'androidx.fragment.app.FragmentTransaction',
  'android.support.v4.app.DialogFragment': 'androidx.fragment.app.DialogFragment',
  'android.support.v4.app.ActivityCompat': 'androidx.core.app.ActivityCompat',
  'android.support.v4.app.NotificationCompat': 'androidx.core.app.NotificationCompat',
  'android.support.v4.app.NotificationManagerCompat': 'androidx.core.app.NotificationManagerCompat',
  'android.support.v4.content.ContextCompat': 'androidx.core.content.ContextCompat',
  'android.support.v4.content.FileProvider': 'androidx.core.content.FileProvider',
  'android.support.v4.view.ViewPager': 'androidx.viewpager.widget.ViewPager',
  'android.support.v4.widget.SwipeRefreshLayout': 'androidx.swiperefreshlayout.widget.SwipeRefreshLayout',
  'android.support.v4.widget.DrawerLayout': 'androidx.drawerlayout.widget.DrawerLayout',
  'android.support.annotation.NonNull': 'androidx.annotation.NonNull',
  'android.support.annotation.Nullable': 'androidx.annotation.Nullable',
  'android.support.annotation.StringRes': 'androidx.annotation.StringRes',
  'android.support.annotation.DrawableRes': 'androidx.annotation.DrawableRes',
  'android.support.annotation.ColorRes': 'androidx.annotation.ColorRes',
  'android.support.annotation.IdRes': 'androidx.annotation.IdRes',
  'android.support.annotation.LayoutRes': 'androidx.annotation.LayoutRes',
  'android.support.annotation.RequiresApi': 'androidx.annotation.RequiresApi',
  'android.support.design.widget.FloatingActionButton': 'com.google.android.material.floatingactionbutton.FloatingActionButton',
  'android.support.design.widget.Snackbar': 'com.google.android.material.snackbar.Snackbar',
  'android.support.design.widget.TabLayout': 'com.google.android.material.tabs.TabLayout',
  'android.support.design.widget.NavigationView': 'com.google.android.material.navigation.NavigationView',
  'android.support.design.widget.BottomNavigationView': 'com.google.android.material.bottomnavigation.BottomNavigationView',
  'android.support.design.widget.AppBarLayout': 'com.google.android.material.appbar.AppBarLayout',
  'android.support.design.widget.TextInputLayout': 'com.google.android.material.textfield.TextInputLayout',
  'android.support.design.widget.TextInputEditText': 'com.google.android.material.textfield.TextInputEditText',
  'android.support.design.widget.CoordinatorLayout': 'androidx.coordinatorlayout.widget.CoordinatorLayout',
  'android.support.constraint.ConstraintLayout': 'androidx.constraintlayout.widget.ConstraintLayout',
  'android.support.constraint.Guideline': 'androidx.constraintlayout.widget.Guideline'
};

const SUPPORT_TO_ANDROIDX_DEPS = {
  'com.android.support:appcompat-v7': 'androidx.appcompat:appcompat:1.6.1',
  'com.android.support:support-v4': 'androidx.legacy:legacy-support-v4:1.0.0',
  'com.android.support:recyclerview-v7': 'androidx.recyclerview:recyclerview:1.3.2',
  'com.android.support:cardview-v7': 'androidx.cardview:cardview:1.0.0',
  'com.android.support:design': 'com.google.android.material:material:1.11.0',
  'com.android.support:support-annotations': 'androidx.annotation:annotation:1.7.1',
  'com.android.support:multidex': 'androidx.multidex:multidex:2.0.1',
  'com.android.support.constraint:constraint-layout': 'androidx.constraintlayout:constraintlayout:2.1.4'
};

function fixLegacySupportLibrary(projectRoot) {
  const fixes = [];
  const srcFiles = listProjectFiles(projectRoot, (full, name) => /\.(java|kt|xml)$/.test(name) && !/[\\/]build[\\/]/.test(full));
  const gradleFiles = listProjectFiles(projectRoot, (full, name) => /^build\.gradle(\.kts)?$/.test(name));
  const usesOldSrc = srcFiles.some(f => { try { return /android\.support\./.test(fs.readFileSync(f, 'utf8')); } catch { return false; } });
  const usesOldDeps = gradleFiles.some(f => /com\.android\.support/.test(fs.readFileSync(f, 'utf8')));
  if (!usesOldSrc && !usesOldDeps) return fixes;

  const entries = Object.entries(SUPPORT_TO_ANDROIDX_CLASSES).sort((a, b) => b[0].length - a[0].length);
  let changedFiles = 0, leftovers = 0;
  for (const f of srcFiles) {
    let c = fs.readFileSync(f, 'utf8');
    if (!/android\.support\./.test(c)) continue;
    const before = c;
    for (const [oldName, newName] of entries) c = c.replace(new RegExp(DOCTOR_ESC_RE(oldName) + '(?![\\w])', 'g'), newName);
    if (c !== before) { fs.writeFileSync(f, c, 'utf8'); changedFiles++; }
    if (/android\.support\./.test(c)) leftovers++;
  }
  let depChanges = 0;
  for (const f of gradleFiles) {
    let c = fs.readFileSync(f, 'utf8');
    const before = c;
    c = c.replace(/(['"])(com\.android\.support(?:\.constraint)?:[\w-]+):[^'"]+\1/g, (whole, q, ga) => {
      const rep = SUPPORT_TO_ANDROIDX_DEPS[ga];
      return rep ? `${q}${rep}${q}` : whole;
    });
    if (c !== before) { fs.writeFileSync(f, c, 'utf8'); depChanges++; }
    if (/com\.android\.support/.test(c)) leftovers++;
  }
  const propsPath = path.join(projectRoot, 'gradle.properties');
  let props = fs.existsSync(propsPath) ? fs.readFileSync(propsPath, 'utf8') : '';
  if (!/^\s*android\.useAndroidX\s*=\s*true/m.test(props)) {
    props = props.replace(/\s*$/, '\n') + 'android.useAndroidX=true\nandroid.enableJetifier=true\n';
    fs.writeFileSync(propsPath, props, 'utf8');
  }
  if (changedFiles || depChanges) fixes.push(`Migrated legacy android.support.* to AndroidX (${changedFiles} source/layout file(s), ${depChanges} build file(s)) -- the old support library no longer exists on modern AGP`);
  if (leftovers) fixes.push('Build Doctor warnings: some android.support.* / com.android.support references have no automatic AndroidX mapping and were left as-is -- migrate them by hand (Android Studio: Refactor > Migrate to AndroidX).');
  return fixes;
}

function fixDeprecatedGradleSyntax(log, projectRoot) {
  const fixes = [];
  const logText = String(log || '');
  const CONF = { compile: 'implementation', testCompile: 'testImplementation', androidTestCompile: 'androidTestImplementation', debugCompile: 'debugImplementation', releaseCompile: 'releaseImplementation', provided: 'compileOnly', runtime: 'runtimeOnly' };
  const gradleFiles = listProjectFiles(projectRoot, (full, name) => /\.gradle(\.kts)?$/.test(name));
  for (const f of gradleFiles) {
    let c = fs.readFileSync(f, 'utf8');
    const before = c;
    const notes = [];
    c = c.replace(/\bjcenter\s*\(\s*\)/g, () => { notes.push('jcenter() -> mavenCentral() (JCenter is shut down)'); return 'mavenCentral()'; });
    c = c.replace(/^(\s*)(compile|testCompile|androidTestCompile|debugCompile|releaseCompile|provided|runtime)(\s*\(?\s*(?:['"]|project\b|files\b|fileTree\b))/gm,
      (whole, ws, conf, rest) => { notes.push(`${conf} -> ${CONF[conf]}`); return ws + CONF[conf] + rest; });
    // AGP 8: packagingOptions { exclude 'x' } -> packagingOptions { resources { excludes += ['x'] } }
    if (/Could not find method (?:exclude|pickFirst|merge)\(\)/.test(logText) || /packagingOptions[\s\S]{0,40}Could not find method/.test(logText)) {
      const isKts = f.endsWith('.kts');
      c = c.replace(/packagingOptions\s*\{([^{}]*)\}/g, (whole, body) => {
        const groups = { excludes: [], pickFirsts: [], merges: [] };
        const lineRe = /^\s*(exclude|pickFirst|merge)\s*\(?\s*['"]([^'"]+)['"]\s*\)?\s*$/gm;
        let m, other = body;
        while ((m = lineRe.exec(body))) groups[{ exclude: 'excludes', pickFirst: 'pickFirsts', merge: 'merges' }[m[1]]].push(m[2]);
        other = body.replace(lineRe, '').trim();
        if (other) return whole; // has something we don't understand -- leave it
        const lines = Object.entries(groups).filter(([, v]) => v.length)
          .map(([k, v]) => `            ${k} += ${isKts ? `setOf(${v.map(x => `"${x}"`).join(', ')})` : `[${v.map(x => `'${x}'`).join(', ')}]`}`);
        if (!lines.length) return whole;
        notes.push('packagingOptions exclude/pickFirst/merge -> resources { excludes/pickFirsts/merges += ... }');
        return `packagingOptions {\n        resources {\n${lines.join('\n')}\n        }\n    }`;
      });
    }
    if (c !== before) {
      fs.writeFileSync(f, c, 'utf8');
      fixes.push(`Updated removed Gradle syntax in ${path.relative(projectRoot, f)}: ${[...new Set(notes)].join('; ')}`);
    }
  }
  // Java `switch (R.id.x)` breaks on AGP 8 (R ids are no longer compile-time constants).
  if (/constant expression required|attribute value must be constant/i.test(logText)) {
    const propsPath = path.join(projectRoot, 'gradle.properties');
    let props = fs.existsSync(propsPath) ? fs.readFileSync(propsPath, 'utf8') : '';
    if (!/^\s*android\.nonFinalResIds\s*=/m.test(props)) {
      props = props.replace(/\s*$/, '\n') + 'android.nonFinalResIds=false\n';
      fs.writeFileSync(propsPath, props, 'utf8');
      fixes.push('Set android.nonFinalResIds=false in gradle.properties (AGP 8 made R.id.* non-constant, which breaks switch/case and annotation use of R ids; this restores the old behaviour without touching code)');
    }
  }
  return fixes;
}

const DOCTOR_KNOWN_IMPORTS = {
  Context: 'android.content.Context', Intent: 'android.content.Intent', IntentFilter: 'android.content.IntentFilter', BroadcastReceiver: 'android.content.BroadcastReceiver',
  SharedPreferences: 'android.content.SharedPreferences', PackageManager: 'android.content.pm.PackageManager', Manifest: 'android.Manifest',
  Bundle: 'android.os.Bundle', Handler: 'android.os.Handler', Looper: 'android.os.Looper', Build: 'android.os.Build', Environment: 'android.os.Environment',
  RemoteException: 'android.os.RemoteException', IBinder: 'android.os.IBinder',
  Toast: 'android.widget.Toast', TextView: 'android.widget.TextView', Button: 'android.widget.Button', EditText: 'android.widget.EditText', ImageView: 'android.widget.ImageView',
  ProgressBar: 'android.widget.ProgressBar', LinearLayout: 'android.widget.LinearLayout', ListView: 'android.widget.ListView', Spinner: 'android.widget.Spinner',
  ArrayAdapter: 'android.widget.ArrayAdapter', CheckBox: 'android.widget.CheckBox',
  View: 'android.view.View', ViewGroup: 'android.view.ViewGroup', LayoutInflater: 'android.view.LayoutInflater', MenuItem: 'android.view.MenuItem', Menu: 'android.view.Menu',
  MotionEvent: 'android.view.MotionEvent', KeyEvent: 'android.view.KeyEvent', Gravity: 'android.view.Gravity',
  WebView: 'android.webkit.WebView', WebViewClient: 'android.webkit.WebViewClient', WebSettings: 'android.webkit.WebSettings',
  Uri: 'android.net.Uri', Log: 'android.util.Log', Bitmap: 'android.graphics.Bitmap', BitmapFactory: 'android.graphics.BitmapFactory', Drawable: 'android.graphics.drawable.Drawable',
  Settings: 'android.provider.Settings', PendingIntent: 'android.app.PendingIntent', NotificationManager: 'android.app.NotificationManager',
  NotificationChannel: 'android.app.NotificationChannel', Service: 'android.app.Service', Activity: 'android.app.Activity',
  File: 'java.io.File', IOException: 'java.io.IOException', InputStream: 'java.io.InputStream', OutputStream: 'java.io.OutputStream',
  FileInputStream: 'java.io.FileInputStream', FileOutputStream: 'java.io.FileOutputStream', BufferedReader: 'java.io.BufferedReader', InputStreamReader: 'java.io.InputStreamReader',
  ArrayList: 'java.util.ArrayList', HashMap: 'java.util.HashMap', HashSet: 'java.util.HashSet', Arrays: 'java.util.Arrays', Collections: 'java.util.Collections',
  Locale: 'java.util.Locale', Date: 'java.util.Date', UUID: 'java.util.UUID', Objects: 'java.util.Objects',
  List: 'java.util.List', Map: 'java.util.Map', Set: 'java.util.Set',
  Pattern: 'java.util.regex.Pattern', Matcher: 'java.util.regex.Matcher', SimpleDateFormat: 'java.text.SimpleDateFormat',
  URL: 'java.net.URL', HttpURLConnection: 'java.net.HttpURLConnection',
  Executors: 'java.util.concurrent.Executors', ExecutorService: 'java.util.concurrent.ExecutorService', TimeUnit: 'java.util.concurrent.TimeUnit',
  JSONObject: 'org.json.JSONObject', JSONArray: 'org.json.JSONArray', JSONException: 'org.json.JSONException',
  AppCompatActivity: 'androidx.appcompat.app.AppCompatActivity', NonNull: 'androidx.annotation.NonNull', Nullable: 'androidx.annotation.Nullable',
  ContextCompat: 'androidx.core.content.ContextCompat', ActivityCompat: 'androidx.core.app.ActivityCompat', NotificationCompat: 'androidx.core.app.NotificationCompat',
  NotificationManagerCompat: 'androidx.core.app.NotificationManagerCompat', Fragment: 'androidx.fragment.app.Fragment', RecyclerView: 'androidx.recyclerview.widget.RecyclerView',
  // graphics / drawables
  GradientDrawable: 'android.graphics.drawable.GradientDrawable', ColorDrawable: 'android.graphics.drawable.ColorDrawable', ShapeDrawable: 'android.graphics.drawable.ShapeDrawable',
  LayerDrawable: 'android.graphics.drawable.LayerDrawable', RippleDrawable: 'android.graphics.drawable.RippleDrawable', BitmapDrawable: 'android.graphics.drawable.BitmapDrawable',
  StateListDrawable: 'android.graphics.drawable.StateListDrawable', Color: 'android.graphics.Color', Canvas: 'android.graphics.Canvas', Paint: 'android.graphics.Paint',
  Path: 'android.graphics.Path', Rect: 'android.graphics.Rect', RectF: 'android.graphics.RectF', Matrix: 'android.graphics.Matrix', Point: 'android.graphics.Point',
  Typeface: 'android.graphics.Typeface', PorterDuff: 'android.graphics.PorterDuff', PixelFormat: 'android.graphics.PixelFormat',
  // text
  TextWatcher: 'android.text.TextWatcher', Editable: 'android.text.Editable', Spannable: 'android.text.Spannable', SpannableString: 'android.text.SpannableString',
  SpannableStringBuilder: 'android.text.SpannableStringBuilder', Selection: 'android.text.Selection', Html: 'android.text.Html', TextUtils: 'android.text.TextUtils',
  InputType: 'android.text.InputType', ForegroundColorSpan: 'android.text.style.ForegroundColorSpan', StyleSpan: 'android.text.style.StyleSpan',
  UnderlineSpan: 'android.text.style.UnderlineSpan', RelativeSizeSpan: 'android.text.style.RelativeSizeSpan',
  LinkMovementMethod: 'android.text.method.LinkMovementMethod', ScrollingMovementMethod: 'android.text.method.ScrollingMovementMethod',
  // widgets / views
  FrameLayout: 'android.widget.FrameLayout', RelativeLayout: 'android.widget.RelativeLayout', ScrollView: 'android.widget.ScrollView',
  HorizontalScrollView: 'android.widget.HorizontalScrollView', Switch: 'android.widget.Switch', RadioButton: 'android.widget.RadioButton', RadioGroup: 'android.widget.RadioGroup',
  SeekBar: 'android.widget.SeekBar', ImageButton: 'android.widget.ImageButton', GridView: 'android.widget.GridView', TableLayout: 'android.widget.TableLayout',
  TableRow: 'android.widget.TableRow', Space: 'android.widget.Space', AdapterView: 'android.widget.AdapterView', BaseAdapter: 'android.widget.BaseAdapter', CompoundButton: 'android.widget.CompoundButton',
  Window: 'android.view.Window', WindowManager: 'android.view.WindowManager', Display: 'android.view.Display', SurfaceView: 'android.view.SurfaceView',
  ViewTreeObserver: 'android.view.ViewTreeObserver', GestureDetector: 'android.view.GestureDetector', ScaleGestureDetector: 'android.view.ScaleGestureDetector',
  HapticFeedbackConstants: 'android.view.HapticFeedbackConstants', WindowInsets: 'android.view.WindowInsets', TextureView: 'android.view.TextureView',
  // animation
  Animation: 'android.view.animation.Animation', AnimationUtils: 'android.view.animation.AnimationUtils', AlphaAnimation: 'android.view.animation.AlphaAnimation',
  TranslateAnimation: 'android.view.animation.TranslateAnimation', ScaleAnimation: 'android.view.animation.ScaleAnimation', RotateAnimation: 'android.view.animation.RotateAnimation',
  AccelerateInterpolator: 'android.view.animation.AccelerateInterpolator', DecelerateInterpolator: 'android.view.animation.DecelerateInterpolator',
  AccelerateDecelerateInterpolator: 'android.view.animation.AccelerateDecelerateInterpolator', OvershootInterpolator: 'android.view.animation.OvershootInterpolator',
  LinearInterpolator: 'android.view.animation.LinearInterpolator', BounceInterpolator: 'android.view.animation.BounceInterpolator',
  ValueAnimator: 'android.animation.ValueAnimator', ObjectAnimator: 'android.animation.ObjectAnimator', AnimatorSet: 'android.animation.AnimatorSet',
  Animator: 'android.animation.Animator', AnimatorListenerAdapter: 'android.animation.AnimatorListenerAdapter',
  // os / app / content / net / media
  CountDownTimer: 'android.os.CountDownTimer', SystemClock: 'android.os.SystemClock', Vibrator: 'android.os.Vibrator', VibrationEffect: 'android.os.VibrationEffect',
  VibratorManager: 'android.os.VibratorManager', PowerManager: 'android.os.PowerManager', StrictMode: 'android.os.StrictMode', AsyncTask: 'android.os.AsyncTask',
  Parcelable: 'android.os.Parcelable', Message: 'android.os.Message', HandlerThread: 'android.os.HandlerThread',
  Dialog: 'android.app.Dialog', DownloadManager: 'android.app.DownloadManager', Application: 'android.app.Application', Notification: 'android.app.Notification',
  AlarmManager: 'android.app.AlarmManager', ProgressDialog: 'android.app.ProgressDialog', ActivityManager: 'android.app.ActivityManager',
  ClipData: 'android.content.ClipData', ClipboardManager: 'android.content.ClipboardManager', ContentResolver: 'android.content.ContentResolver',
  ContentValues: 'android.content.ContentValues', DialogInterface: 'android.content.DialogInterface', ActivityNotFoundException: 'android.content.ActivityNotFoundException',
  ComponentName: 'android.content.ComponentName', ServiceConnection: 'android.content.ServiceConnection', Configuration: 'android.content.res.Configuration',
  Resources: 'android.content.res.Resources', AssetManager: 'android.content.res.AssetManager', ColorStateList: 'android.content.res.ColorStateList',
  ConnectivityManager: 'android.net.ConnectivityManager', NetworkInfo: 'android.net.NetworkInfo', Network: 'android.net.Network', NetworkCapabilities: 'android.net.NetworkCapabilities',
  MediaPlayer: 'android.media.MediaPlayer', AudioManager: 'android.media.AudioManager', MediaStore: 'android.provider.MediaStore', OpenableColumns: 'android.provider.OpenableColumns',
  DisplayMetrics: 'android.util.DisplayMetrics', TypedValue: 'android.util.TypedValue', AttributeSet: 'android.util.AttributeSet', SparseArray: 'android.util.SparseArray',
  Patterns: 'android.util.Patterns',
  // webkit
  WebChromeClient: 'android.webkit.WebChromeClient', CookieManager: 'android.webkit.CookieManager', JavascriptInterface: 'android.webkit.JavascriptInterface',
  ValueCallback: 'android.webkit.ValueCallback', WebResourceRequest: 'android.webkit.WebResourceRequest', WebResourceResponse: 'android.webkit.WebResourceResponse',
  WebResourceError: 'android.webkit.WebResourceError', ConsoleMessage: 'android.webkit.ConsoleMessage', GeolocationPermissions: 'android.webkit.GeolocationPermissions',
  PermissionRequest: 'android.webkit.PermissionRequest', URLUtil: 'android.webkit.URLUtil', MimeTypeMap: 'android.webkit.MimeTypeMap', DownloadListener: 'android.webkit.DownloadListener',
  SslErrorHandler: 'android.webkit.SslErrorHandler', WebStorage: 'android.webkit.WebStorage',
  // java
  ByteArrayOutputStream: 'java.io.ByteArrayOutputStream', ByteArrayInputStream: 'java.io.ByteArrayInputStream', BufferedInputStream: 'java.io.BufferedInputStream',
  BufferedOutputStream: 'java.io.BufferedOutputStream', BufferedWriter: 'java.io.BufferedWriter', OutputStreamWriter: 'java.io.OutputStreamWriter', FileReader: 'java.io.FileReader',
  FileWriter: 'java.io.FileWriter', PrintWriter: 'java.io.PrintWriter', StandardCharsets: 'java.nio.charset.StandardCharsets', Iterator: 'java.util.Iterator',
  LinkedList: 'java.util.LinkedList', LinkedHashMap: 'java.util.LinkedHashMap', TreeMap: 'java.util.TreeMap', Queue: 'java.util.Queue', Deque: 'java.util.Deque',
  ArrayDeque: 'java.util.ArrayDeque', Random: 'java.util.Random', Timer: 'java.util.Timer', TimerTask: 'java.util.TimerTask', Calendar: 'java.util.Calendar',
  TimeZone: 'java.util.TimeZone', Optional: 'java.util.Optional', Comparator: 'java.util.Comparator', StringTokenizer: 'java.util.StringTokenizer',
  ConcurrentHashMap: 'java.util.concurrent.ConcurrentHashMap', AtomicInteger: 'java.util.concurrent.atomic.AtomicInteger', AtomicBoolean: 'java.util.concurrent.atomic.AtomicBoolean',
  AtomicLong: 'java.util.concurrent.atomic.AtomicLong', CountDownLatch: 'java.util.concurrent.CountDownLatch', Future: 'java.util.concurrent.Future',
  Callable: 'java.util.concurrent.Callable', CopyOnWriteArrayList: 'java.util.concurrent.CopyOnWriteArrayList', MessageDigest: 'java.security.MessageDigest',
  NoSuchAlgorithmException: 'java.security.NoSuchAlgorithmException', SecureRandom: 'java.security.SecureRandom', URI: 'java.net.URI', URLEncoder: 'java.net.URLEncoder',
  URLDecoder: 'java.net.URLDecoder', MalformedURLException: 'java.net.MalformedURLException', SocketTimeoutException: 'java.net.SocketTimeoutException',
  UnknownHostException: 'java.net.UnknownHostException', DecimalFormat: 'java.text.DecimalFormat', NumberFormat: 'java.text.NumberFormat',
  ZipInputStream: 'java.util.zip.ZipInputStream', ZipOutputStream: 'java.util.zip.ZipOutputStream', ZipEntry: 'java.util.zip.ZipEntry', GZIPInputStream: 'java.util.zip.GZIPInputStream'
};

function doctorResolveLogFile(logPath, candidates) {
  const base = path.basename(logPath);
  const same = candidates.filter(c => path.basename(c) === base);
  if (same.length === 1) return same[0];
  if (same.length > 1) {
    const segs = logPath.split(/[\\/]/).reverse();
    let best = null, bestScore = 0, tie = false;
    for (const c of same) {
      const cs = c.split(/[\\/]/).reverse();
      let score = 0;
      while (score < segs.length && score < cs.length && segs[score] === cs[score]) score++;
      if (score > bestScore) { best = c; bestScore = score; tie = false; } else if (score === bestScore) tie = true;
    }
    return tie ? null : best;
  }
  return null;
}

function doctorAddImport(content, fq, isKotlin) {
  // Already imported (with or without ';', an alias or a trailing comment)? Adding it again
  // is what produces "Conflicting import, imported name 'X' is ambiguous".
  if (new RegExp('^[ \\t]*import\\s+' + fq.replace(/\./g, '\\.') + '(?:\\s+as\\s+\\w+)?[ \\t]*;?[ \\t]*(?:\\/\\/.*)?\\r?$', 'm').test(content)) return content;
  const line = isKotlin ? `import ${fq}` : `import ${fq};`;
  const imports = [...content.matchAll(/^import\s.+$/gm)];
  if (imports.length) {
    const last = imports[imports.length - 1];
    const at = last.index + last[0].length;
    return content.slice(0, at) + '\n' + line + content.slice(at);
  }
  const pkg = content.match(/^package\s.+$/m);
  if (pkg) {
    const at = pkg.index + pkg[0].length;
    return content.slice(0, at) + '\n\n' + line + content.slice(at);
  }
  return line + '\n' + content;
}

function fixMissingImportsFromLog(log, projectRoot) {
  const fixes = [];
  const logText = String(log || '');
  if (!logText) return fixes;
  const found = []; // { file(log path), name, kotlin }
  for (const m of logText.matchAll(/(\S+\.java):\d+: error: cannot find symbol[\s\S]{0,250}?symbol:\s+(?:class|variable)\s+(\w+)/g)) found.push({ file: m[1], name: m[2], kotlin: false });
  for (const m of logText.matchAll(/e:\s*(?:file:\/\/)?(\S+\.kt)[:\s]\(?\d+[:,]\s*\d+\)?:?\s*Unresolved reference:?\s*'?(\w+)'?/g)) found.push({ file: m[1], name: m[2], kotlin: true });
  if (!found.length) return fixes;
  const candidates = listProjectFiles(projectRoot, (full, name) => /\.(java|kt)$/.test(name));
  const done = new Set();
  for (const f of found) {
    const abs = doctorResolveLogFile(f.file, candidates);
    if (!abs || done.has(abs + '|' + f.name)) continue;
    done.add(abs + '|' + f.name);
    let c = fs.readFileSync(abs, 'utf8');
    if (new RegExp(`^import\\s+[\\w.]+\\.${f.name}\\b`, 'm').test(c)) continue;
    if (new RegExp(`\\b(class|interface|object|enum)\\s+${f.name}\\b`).test(c)) continue;
    let fq = null;
    const composeFq = f.kotlin ? doctorComposeImportFor(f.name, c, projectRoot) : null;
    if (composeFq) {
      fq = composeFq; // Compose file: pick the Compose/Material class, not the Android view one
    } else if (f.name === 'R' || f.name === 'BuildConfig') {
      // walk up to the module's build file and use its namespace
      let dir = path.dirname(abs), gradle = null;
      while (dir.startsWith(projectRoot) && dir !== path.dirname(dir)) {
        gradle = ['build.gradle', 'build.gradle.kts'].map(x => path.join(dir, x)).find(x => fs.existsSync(x));
        if (gradle) break;
        dir = path.dirname(dir);
      }
      const ns = gradle && (fs.readFileSync(gradle, 'utf8').match(/\bnamespace\s*(?:=\s*)?["']([\w.]+)["']/) || [])[1];
      const pkg = (c.match(/^package\s+([\w.]+)/m) || [])[1];
      if (ns && pkg !== ns) fq = `${ns}.${f.name}`;
    } else if (DOCTOR_KNOWN_IMPORTS[f.name]) {
      fq = DOCTOR_KNOWN_IMPORTS[f.name];
      if (f.kotlin && /^(List|Map|Set|Date)$/.test(f.name) && !/^java\.util\./.test(fq)) fq = null;
    }
    if (!fq) continue;
    c = doctorAddImport(c, fq, f.kotlin);
    fs.writeFileSync(abs, c, 'utf8');
    fixes.push(`Added missing import ${fq} to ${path.relative(projectRoot, abs)} (compiler reported it couldn't find ${f.name})`);
  }
  return fixes;
}

function fixMissingCommonPermissions(projectRoot) {
  const fixes = [];
  const manifestPath = findAppManifestForDoctor(projectRoot);
  if (!manifestPath) return fixes;
  const src = listProjectFiles(projectRoot, (full, name) => /\.(java|kt)$/.test(name) && !/[\\/]build[\\/]/.test(full))
    .map(f => { try { return fs.readFileSync(f, 'utf8'); } catch { return ''; } }).join('\n');
  const RULES = [
    ['android.permission.INTERNET', /\b(HttpURLConnection|HttpsURLConnection|OkHttpClient|Retrofit|Volley|openConnection\s*\(|InetAddress|Socket\s*\()|URL\s*\(\s*["']https?:|loadUrl\s*\(\s*["']https?:/],
    ['android.permission.ACCESS_NETWORK_STATE', /\b(ConnectivityManager|NetworkCapabilities|getActiveNetworkInfo|registerNetworkCallback)\b/],
    ['android.permission.WAKE_LOCK', /\b(newWakeLock|PowerManager\.WakeLock)\b/]
    // NOTE: intentionally no VIBRATE rule here. MainActivity.java's built-in
    // vibrate() bridge (haptic tap on buttons/links) unconditionally
    // references Vibrator/VibratorManager/VibrationEffect in every generated
    // project, so a usage-based rule for VIBRATE would always match and
    // force the permission into the manifest no matter what the user chose
    // in the permissions UI. That bridge already checks hasVibrator() and
    // no-ops safely without the permission (see its own comment), so
    // VIBRATE must stay purely opt-in via buildOptionalPermissionsXml.
  ];
  let manifest = fs.readFileSync(manifestPath, 'utf8');
  const added = [];
  for (const [perm, re] of RULES) {
    if (!re.test(src)) continue;
    const next = addManifestPermission(manifest, perm);
    if (next !== manifest) { manifest = next; added.push(perm.replace('android.permission.', '')); }
  }
  if (added.length) {
    fs.writeFileSync(manifestPath, manifest, 'utf8');
    fixes.push(`Added missing manifest permission(s) the code clearly needs: ${added.join(', ')} (code uses the matching APIs; without them the app crashes/fails at runtime)`);
  }
  return fixes;
}

function fixTextViewEditTextOnlyMethods(log, projectRoot) {
  const fixes = [];
  const logText = String(log || '');
  if (!logText) return fixes;
  const found = [];
  const re = /(\S+\.java):(\d+): error: cannot find symbol[\s\S]{0,400}?symbol:\s+method\s+(setSelection|selectAll|extendSelection)\([^)]*\)[\s\S]{0,160}?location:\s+variable\s+(\w+)\s+of type\s+(?:android\.widget\.)?TextView\b/g;
  for (const m of logText.matchAll(re)) found.push({ file: m[1], line: Number(m[2]), method: m[3], variable: m[4] });
  if (!found.length) return fixes;
  const candidates = listProjectFiles(projectRoot, (full, name) => /\.java$/.test(name));
  const done = new Set();
  for (const f of found) {
    const key = `${f.file}:${f.line}:${f.variable}.${f.method}`;
    if (done.has(key)) continue;
    done.add(key);
    const abs = doctorResolveLogFile(f.file, candidates);
    if (!abs) continue;
    const content = fs.readFileSync(abs, 'utf8');
    const lines = content.split('\n');
    const head = `${f.variable}.${f.method}(`;
    // Another repair (an added import line, say) may already have shifted the
    // line numbers in the compiler log, so look a few lines either side too.
    let idx = -1;
    for (const off of [0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5]) {
      const cand = f.line - 1 + off;
      if (cand >= 0 && cand < lines.length && lines[cand].includes(head)) { idx = cand; break; }
    }
    if (idx === -1) continue;
    const ln = lines[idx];
    let out = '', i = 0, changed = false;
    for (;;) {
      const at = ln.indexOf(head, i);
      if (at === -1) { out += ln.slice(i); break; }
      const prev = at > 0 ? ln[at - 1] : '';
      let j = at + head.length, depth = 1;
      while (j < ln.length && depth > 0) { if (ln[j] === '(') depth++; else if (ln[j] === ')') depth--; j++; }
      let k = j;
      while (k < ln.length && /\s/.test(ln[k])) k++;
      if (/[\w.]/.test(prev) || depth !== 0 || ln[k] !== ';') {
        out += ln.slice(i, at + head.length);
        i = at + head.length;
        continue;
      }
      const args = ln.slice(at + head.length, j - 1).trim();
      const text = `${f.variable}.getText()`;
      const call = `android.text.Selection.${f.method}((android.text.Spannable) ${text}${args ? ', ' + args : ''})`;
      out += ln.slice(i, at) + `{ if (${text} instanceof android.text.Spannable) ${call}; }`;
      i = k + 1;
      changed = true;
    }
    if (!changed) continue;
    lines[idx] = out;
    fs.writeFileSync(abs, lines.join('\n'), 'utf8');
    fixes.push(`Rewrote ${f.variable}.${f.method}(...) in ${path.relative(projectRoot, abs)}:${idx + 1} -- ${f.method} only exists on EditText, but ${f.variable} is a TextView (now uses android.text.Selection, guarded so a non-editable view is left alone)`);
  }
  return fixes;
}

function fixUnknownGradlePropertyFromLog(log, projectRoot) {
  const fixes = [];
  const names = new Set();
  for (const m of String(log || '').matchAll(/Could not get unknown property '([A-Za-z_][\w.]*)' for (?:root )?project/g)) names.add(m[1]);
  if (!names.size) return fixes;
  const settings = listProjectFiles(projectRoot, (full, name) => name === 'settings.gradle' || name === 'settings.gradle.kts')
    .sort((a, b) => a.split(path.sep).length - b.split(path.sep).length);
  const rootDir = settings.length ? path.dirname(settings[0]) : projectRoot;
  const file = path.join(rootDir, 'gradle.properties');
  let c = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '';
  const added = [];
  for (const n of names) {
    if (new RegExp('^\\s*' + n.replace(/\./g, '\\.') + '\\s*[=:]', 'm').test(c)) continue;
    c += `${c && !c.endsWith('\n') ? '\n' : ''}${n}=\n`;
    added.push(n);
  }
  if (!added.length) return fixes;
  fs.writeFileSync(file, c, 'utf8');
  fixes.push(`Declared missing Gradle propert${added.length > 1 ? 'ies' : 'y'} ${added.join(', ')} in ${path.relative(projectRoot, file) || 'gradle.properties'} (the build script reads ${added.length > 1 ? 'them' : 'it'} with no value defined; signing values are filled in by the CI workflow)`);
  return fixes;
}

function fixGradleWrapperFromLog(log, projectRoot) {
  const fixes = [];
  const m = String(log || '').match(/Minimum supported Gradle version is (\d+(?:\.\d+)*)/);
  if (!m) return fixes;
  const required = m[1];
  const cmp = (a, b) => {
    const pa = a.split('.').map(Number), pb = b.split('.').map(Number);
    for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
      const d = (pa[i] || 0) - (pb[i] || 0);
      if (d) return d;
    }
    return 0;
  };
  const settings = listProjectFiles(projectRoot, (full, name) => name === 'settings.gradle' || name === 'settings.gradle.kts')
    .sort((a, b) => a.split(path.sep).length - b.split(path.sep).length)[0];
  if (!settings) return fixes;
  const rootDir = path.dirname(settings);
  const propsPath = path.join(rootDir, 'gradle', 'wrapper', 'gradle-wrapper.properties');
  if (fs.existsSync(propsPath)) {
    let c = fs.readFileSync(propsPath, 'utf8');
    const mm = c.match(/(distributionUrl\s*=.*?gradle-)(\d+(?:\.\d+)*)(-(?:bin|all)\.zip)/);
    if (mm && cmp(mm[2], required) < 0) {
      c = c.replace(mm[0], `${mm[1]}${required}${mm[3]}`);
      fs.writeFileSync(propsPath, c, 'utf8');
      fixes.push(`Raised Gradle wrapper ${mm[2]} -> ${required} in ${path.relative(projectRoot, propsPath)} (the log says the project's Android Gradle Plugin needs Gradle ${required}+)`);
    }
    return fixes;
  }
  fs.mkdirSync(path.dirname(propsPath), { recursive: true });
  fs.writeFileSync(propsPath, [
    'distributionBase=GRADLE_USER_HOME',
    'distributionPath=wrapper/dists',`distributionUrl=https\\://services.gradle.org/distributions/gradle-${required}-bin.zip`,
    'zipStoreBase=GRADLE_USER_HOME',
    'zipStorePath=wrapper/dists',
    ''
  ].join('\n'), 'utf8');
  fixes.push(`Added ${path.relative(projectRoot, propsPath)} declaring Gradle ${required} (the project had no wrapper and its Android Gradle Plugin needs Gradle ${required}+; CI installs the version this file declares)`);
  return fixes;
}

function fixMovedOkHttpFactoriesFromLog(log, projectRoot) {
  const fixes = [];
  const logText = String(log || '');
  if (!logText) return fixes;
  const wanted = new Set(); // "HttpUrl.get" / "HttpUrl.parse" / "MediaType.get" / "MediaType.parse"
  for (const m of logText.matchAll(/Using '(get|parse)\(String\):\s*(HttpUrl|MediaType)\??'\s+is an error/g)) wanted.add(`${m[2]}.${m[1]}`);
  if (!wanted.size) return fixes;
  const EXT = {
    'HttpUrl.get': ['toHttpUrl', 'okhttp3.HttpUrl.Companion.toHttpUrl'],
    'HttpUrl.parse': ['toHttpUrlOrNull', 'okhttp3.HttpUrl.Companion.toHttpUrlOrNull'],
    'MediaType.get': ['toMediaType', 'okhttp3.MediaType.Companion.toMediaType'],
    'MediaType.parse': ['toMediaTypeOrNull', 'okhttp3.MediaType.Companion.toMediaTypeOrNull']
  };
  const files = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  for (const abs of files) {
    let c = fs.readFileSync(abs, 'utf8');
    const orig = c;
    const imports = [];
    for (const key of wanted) {
      const [ext, fq] = EXT[key];
      const [type, method] = key.split('.');
      const re = new RegExp(`(?<![\\w.])(?:okhttp3\\.)?${type}\\.${method}\\(`, 'g');
      let out = '', last = 0, m, changed = false;
      while ((m = re.exec(c))) {
        // find the matching close paren, skipping string literals
        let i = re.lastIndex, depth = 1, top = -1;
        while (i < c.length && depth > 0) {
          const ch = c[i];
          if (ch === '"') { i++; while (i < c.length && c[i] !== '"') { if (c[i] === '\\') i++; i++; } }
          else if (ch === '(' || ch === '[' || ch === '{') depth++;
          else if (ch === ')' || ch === ']' || ch === '}') depth--;
          else if (ch === ',' && depth === 1 && top < 0) top = i;
          i++;
        }
        if (depth !== 0 || top >= 0) continue; // unbalanced or multi-arg: leave alone
        const arg = c.slice(re.lastIndex, i - 1).trim();
        if (!arg) continue;
        const simple = /^[\w.]+$/.test(arg) || /^"(?:[^"\\]|\\.)*"$/.test(arg);
        out += c.slice(last, m.index) + (simple ? arg : `(${arg})`) + '.' + ext + '()';
        last = i;
        re.lastIndex = i;
        changed = true;
      }
      if (changed) { c = out + c.slice(last); imports.push(fq); }
    }
    if (c === orig) continue;
    for (const fq of imports) {
      if (!new RegExp(`^import\\s+${fq.replace(/\./g, '\\.')}\\s*$`, 'm').test(c)) c = doctorAddImport(c, fq, true);
    }
    fs.writeFileSync(abs, c, 'utf8');
    fixes.push(`Rewrote moved OkHttp factory call(s) to extension functions in ${path.relative(projectRoot, abs)} (${[...wanted].join(', ')}; the log says they are now compile errors)`);
  }
  return fixes;
}

function doctorFindKotlinExprEnd(c, start) {
  let i = start, depth = 0;
  const contAfter = /(?:[+\-*\/%&|=,.<>:?!]|\bin|\bis|\bas)\s*$/;
  const contBefore = /^\s*(?:\.|\?\.|\?:|&&|\|\||[+\-*\/%]\s|as\b|is\b|in\b)/;
  while (i < c.length) {
    const ch = c[i];
    if (ch === '"') {
      if (c.startsWith('"""', i)) { const e = c.indexOf('"""', i + 3); if (e < 0) return -1; i = e + 3; continue; }
      i++;
      while (i < c.length && c[i] !== '"') { if (c[i] === '\\') i++; if (c[i] === '\n') return -1; i++; }
      i++; continue;
    }
    if (ch === "'") { i++; while (i < c.length && c[i] !== "'") { if (c[i] === '\\') i++; i++; } i++; continue; }
    if (ch === '/' && c[i + 1] === '/') { while (i < c.length && c[i] !== '\n') i++; continue; }
    if (ch === '/' && c[i + 1] === '*') { const e = c.indexOf('*/', i + 2); if (e < 0) return -1; i = e + 2; continue; }
    if (ch === '(' || ch === '[' || ch === '{') { depth++; i++; continue; }
    if (ch === ')' || ch === ']' || ch === '}') { if (depth === 0) return i; depth--; i++; continue; }
    if (depth === 0 && ch === ';') return i;
    if (depth === 0 && ch === '\n') {
      const before = c.slice(start, i).replace(/\/\/.*$/, '');
      const rest = c.slice(i + 1);
      const nextLine = rest.slice(0, rest.indexOf('\n') < 0 ? rest.length : rest.indexOf('\n'));
      if (!contAfter.test(before) && !contBefore.test(nextLine)) return i;
    }
    i++;
  }
  return depth === 0 ? c.length : -1;
}

function fixKotlinReturnAnyToStringFromLog(log, projectRoot) {
  const fixes = [];
  const logText = String(log || '');
  if (!logText) return fixes;
  const found = [];
  for (const m of logText.matchAll(/e:\s*(?:file:\/\/)?(\S+\.kt)[:\s]\(?(\d+)[:,]\s*(\d+)\)?:?\s*Return type mismatch:\s*expected\s*'kotlin\.String\??',\s*actual\s*'kotlin\.Any(\??)'/g)) {
    found.push({ file: m[1], line: Number(m[2]), col: Number(m[3]), nullable: m[4] === '?' });
  }
  if (!found.length) return fixes;
  const candidates = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const byFile = new Map();
  for (const f of found) {
    const abs = doctorResolveLogFile(f.file, candidates);
    if (!abs) continue;
    if (!byFile.has(abs)) byFile.set(abs, []);
    byFile.get(abs).push(f);
  }
  for (const [abs, list] of byFile) {
    let c = fs.readFileSync(abs, 'utf8');
    const edits = [];
    const seen = new Set();
    for (const f of list) {
      const lines = c.split('\n');
      let start = -1, lineNo = -1;
      // Earlier repairs (an added import) may have shifted line numbers a little.
      for (const off of [0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5]) {
        const idx = f.line - 1 + off;
        if (idx < 0 || idx >= lines.length) continue;
        const ln = lines[idx];
        const before = ln.slice(0, Math.max(0, f.col - 1));
        let col = -1;
        if (/\breturn\s+$/.test(before) && ln.slice(f.col - 1).trim()) col = f.col - 1;
        else { const rm = ln.match(/^(\s*)return\s+(?=\S)/); if (rm && !/^return@/.test(ln.trim())) col = rm[0].length; }
        if (col < 0) continue;
        if (/^\s*\/\//.test(ln)) continue;
        start = lines.slice(0, idx).reduce((n, l) => n + l.length + 1, 0) + col;
        lineNo = idx + 1;
        break;
      }
      if (start < 0 || seen.has(start)) continue;
      seen.add(start);
      const end = doctorFindKotlinExprEnd(c, start);
      if (end <= start) continue;
      const rawExpr = c.slice(start, end);
      const expr = rawExpr.replace(/\s+$/, '');
      if (!expr.trim() || /\.toString\(\)$/.test(expr.trim())) continue;
      const trail = rawExpr.slice(expr.length);
      const wrapped = f.nullable ? `(${expr})?.toString() ?: ""` : `(${expr}).toString()`;
      edits.push({ start, end: start + expr.length, wrapped, lineNo });
      void trail;
    }
    if (!edits.length) continue;
    edits.sort((a, b) => b.start - a.start);
    for (const e of edits) c = c.slice(0, e.start) + e.wrapped + c.slice(e.end);
    fs.writeFileSync(abs, c, 'utf8');
    fixes.push(`Wrapped returned value in .toString() at ${path.relative(projectRoot, abs)}:${edits.map(e => e.lineNo).reverse().join(', ')} -- the Kotlin compiler says the function must return String but the expression is typed Any ('Return type mismatch: expected kotlin.String, actual kotlin.Any')`);
  }
  return fixes;
}

function doctorFoldAscii(str) {
  return str.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D');
}

function doctorFixSplitIdentifiersInCode(code, found) {
  const re = /[\p{L}\p{N}_$]+/gu;
  const hasNonAscii = (w) => /[^\x00-\x7F]/.test(w);
  const words = [...code.matchAll(re)].map(m => ({ w: m[0], i: m.index, e: m.index + m[0].length }));
  if (words.length < 2) return code;
  let out = '', pos = 0, k = 0;
  while (k < words.length) {
    let j = k;
    if (hasNonAscii(words[k].w)) {
      while (j + 1 < words.length && hasNonAscii(words[j + 1].w) && /^[ \t]+$/.test(code.slice(words[j].e, words[j + 1].i))) j++;
    }
    if (j > k) {
      const joined = words.slice(k, j + 1).map(x => x.w).join('');
      const folded = doctorFoldAscii(joined);
      found.set(code.slice(words[k].i, words[j].e), folded);
      out += code.slice(pos, words[k].i) + folded;
      pos = words[j].e;
    }
    k = j + 1;
  }
  return out + code.slice(pos);
}

function doctorSplitJavaKotlinSource(c, found) {
  // split into code / non-code (strings, chars, comments) so only code is rewritten
  let out = '', codeStart = 0, i = 0;
  const flush = (to) => { out += doctorFixSplitIdentifiersInCode(c.slice(codeStart, to), found); };
  while (i < c.length) {
    const ch = c[i];
    let end = -1;
    if (ch === '/' && c[i + 1] === '/') { end = c.indexOf('\n', i); if (end < 0) end = c.length; }
    else if (ch === '/' && c[i + 1] === '*') { end = c.indexOf('*/', i + 2); end = end < 0 ? c.length : end + 2; }
    else if (c.startsWith('"""', i)) { end = c.indexOf('"""', i + 3); end = end < 0 ? c.length : end + 3; }
    else if (ch === '"') { let j = i + 1; while (j < c.length && c[j] !== '"' && c[j] !== '\n') { if (c[j] === '\\') j++; j++; } end = Math.min(j + 1, c.length); }
    else if (ch === "'") { let j = i + 1; while (j < c.length && c[j] !== "'" && c[j] !== '\n') { if (c[j] === '\\') j++; j++; } end = Math.min(j + 1, c.length); }
    if (end < 0) { i++; continue; }
    flush(i);
    out += c.slice(i, end);
    i = end; codeStart = end;
  }
  flush(c.length);
  return out;
}

function fixSplitNonAsciiIdentifiers(projectRoot) {
  const fixes = [];
  const files = listProjectFiles(projectRoot, (full, name) => /\.(java|kt)$/.test(name));
  for (const abs of files) {
    let c;
    try { c = fs.readFileSync(abs, 'utf8'); } catch (e) { continue; }
    if (!/[^\x00-\x7F]/.test(c)) continue;
    const found = new Map();
    const next = doctorSplitJavaKotlinSource(c, found);
    if (next === c || !found.size) continue;
    fs.writeFileSync(abs, next, 'utf8');
    const sample = [...found].slice(0, 4).map(([a, b]) => `"${a}" -> ${b}`).join(', ');
    { const sc = doctorReadTranslatedSidecar(projectRoot), k = path.relative(projectRoot, abs); sc[k] = [...new Set((sc[k] || []).concat([...found.values()]))]; doctorWriteTranslatedSidecar(projectRoot, sc); }
    fixes.push(`Repaired ${found.size} identifier(s) containing spaces in ${path.relative(projectRoot, abs)} (translated source split names like ${sample}; joined and converted to plain ASCII so javac/kotlinc can parse them)`);
  }
  return fixes;
}

const DOCTOR_TRANSLATED_SIDECAR = '.wzh-translated-idents.json';

function doctorReadTranslatedSidecar(projectRoot) {
  try { return JSON.parse(fs.readFileSync(path.join(projectRoot, DOCTOR_TRANSLATED_SIDECAR), 'utf8')) || {}; } catch (e) { return {}; }
}

function doctorWriteTranslatedSidecar(projectRoot, data) {
  try { fs.writeFileSync(path.join(projectRoot, DOCTOR_TRANSLATED_SIDECAR), JSON.stringify(data, null, 1), 'utf8'); } catch (e) { /* best effort */ }
}

function doctorMaskNonCode(c) {
  // same text, same length, with string/char literals and comments blanked (newlines kept)
  const a = c.split('');
  const blank = (s, e) => { for (let k = s; k < e && k < a.length; k++) if (a[k] !== '\n') a[k] = ' '; };
  let i = 0;
  while (i < c.length) {
    const ch = c[i];
    let end = -1;
    if (ch === '/' && c[i + 1] === '/') { end = c.indexOf('\n', i); if (end < 0) end = c.length; }
    else if (ch === '/' && c[i + 1] === '*') { end = c.indexOf('*/', i + 2); end = end < 0 ? c.length : end + 2; }
    else if (c.startsWith('"""', i)) { end = c.indexOf('"""', i + 3); end = end < 0 ? c.length : end + 3; }
    else if (ch === '"') { let j = i + 1; while (j < c.length && c[j] !== '"' && c[j] !== '\n') { if (c[j] === '\\') j++; j++; } end = Math.min(j + 1, c.length); }
    else if (ch === "'") { let j = i + 1; while (j < c.length && c[j] !== "'" && c[j] !== '\n') { if (c[j] === '\\') j++; j++; } end = Math.min(j + 1, c.length); }
    if (end < 0) { i++; continue; }
    blank(i, end);
    i = end;
  }
  return a.join('');
}

function doctorWordRe(name) {
  return new RegExp('(?<![\\p{L}\\p{N}_$])' + name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '(?![\\p{L}\\p{N}_$])', 'gu');
}

function doctorEnclosingBlock(m, pos) {
  let depth = 0;
  for (let i = Math.min(pos, m.length) - 1; i >= 0; i--) {
    const ch = m[i];
    if (ch === '}') depth++;
    else if (ch === '{') {
      if (depth > 0) { depth--; continue; }
      let close = -1, d = 1;
      for (let j = i + 1; j < m.length; j++) {
        if (m[j] === '{') d++;
        else if (m[j] === '}') { d--; if (d === 0) { close = j; break; } }
      }
      let hs = i;
      while (hs > 0 && !';{}'.includes(m[hs - 1])) hs--;
      return { open: i, close: close < 0 ? m.length : close, header: m.slice(hs, i) };
    }
  }
  return null;
}

function doctorClassNameOfHeader(h) {
  const mm = /\b(?:class|interface|enum)\s+([\p{L}\p{N}_$]+)/u.exec(h || '');
  return mm ? mm[1] : null;
}

function doctorInsideParens(m, pos) {
  let depth = 0;
  for (let i = pos - 1; i >= 0; i--) {
    const ch = m[i];
    if (ch === ')') depth++;
    else if (ch === '(') { if (depth === 0) return i; depth--; }
    else if (depth === 0 && (ch === ';' || ch === '{' || ch === '}')) return -1;
  }
  return -1;
}

const DOCTOR_NOT_A_TYPE_WORD = new Set(['return', 'new', 'throw', 'else', 'case', 'instanceof', 'assert', 'yield', 'import', 'package', 'goto', 'break', 'continue']);

function fixTranslatedDeclarationMismatchFromLog(log, projectRoot) {
  const fixes = [];
  const logText = String(log || '');
  if (!logText) return fixes;
  const ID = '[\\p{L}\\p{N}_$]+';
  const re = new RegExp('(\\S+\\.java):(\\d+): error: cannot find symbol[\\s\\S]{0,500}?symbol:\\s+(variable|method)\\s+(' + ID + ')[^\\n]*\\n\\s*location:\\s+(?:variable\\s+' + ID + '\\s+of type\\s+([\\p{L}\\p{N}_$.<>]+)|(?:class|interface|enum)\\s+([\\p{L}\\p{N}_$.<>]+))', 'gu');
  const errs = [];
  for (const m of logText.matchAll(re)) {
    const lastSeg = (t) => t ? t.replace(/<.*$/, '').split('.').pop() : null;
    errs.push({ file: m[1], line: Number(m[2]), kind: m[3], name: m[4], memberOf: lastSeg(m[5]), scopeClass: lastSeg(m[6]) });
  }
  if (!errs.length) return fixes;
  errs.sort((a, b) => a.file.localeCompare(b.file) || a.line - b.line);

  const javaFiles = listProjectFiles(projectRoot, (full, name) => /\.java$/.test(name));
  const state = new Map(); // abs -> { c, m, changed }
  const load = (abs) => {
    if (!state.has(abs)) { const c = fs.readFileSync(abs, 'utf8'); state.set(abs, { c, m: doctorMaskNonCode(c), changed: false }); }
    return state.get(abs);
  };
  const sidecar = doctorReadTranslatedSidecar(projectRoot);
  const rel = (abs) => path.relative(projectRoot, abs);
  const translatedNames = (abs) => {
    const st = load(abs);
    const set = new Set(sidecar[rel(abs)] || []);
    for (const w of st.m.matchAll(/[\p{L}\p{N}_$]+/gu)) if (/[^\x00-\x7F]/.test(w[0])) set.add(w[0]);
    return set;
  };
  const totalOccurrences = (name) => {
    const found = [];
    for (const abs of javaFiles) {
      const st = load(abs);
      for (const mm of st.m.matchAll(doctorWordRe(name))) found.push({ abs, idx: mm.index });
      if (found.length > 1) break;
    }
    return found;
  };
  const prevNonSpace = (m, i) => { let k = i - 1; while (k >= 0 && /\s/.test(m[k])) k--; return k; };
  const nextNonSpace = (m, i) => { let k = i; while (k < m.length && /\s/.test(m[k])) k++; return k; };
  const asDeclaration = (m, d, len, kind) => {
    const pk = prevNonSpace(m, d);
    if (pk < 0 || !/[\p{L}\p{N}_$>\]]/u.test(m[pk])) return false;
    let ws = pk; while (ws > 0 && /[\p{L}\p{N}_$]/u.test(m[ws - 1])) ws--;
    if (/[\p{L}\p{N}_$]/u.test(m[pk]) && DOCTOR_NOT_A_TYPE_WORD.has(m.slice(ws, pk + 1))) return false;
    const nk = nextNonSpace(m, d + len);
    const nc = m[nk];
    return kind === 'method' ? nc === '(' : '=;,):'.includes(nc || '');
  };
  const done = new Set();
  for (const e of errs) {
    const errAbs = doctorResolveLogFile(e.file, javaFiles);
    if (!errAbs) continue;
    const key = errAbs + '|' + e.name + '|' + (e.memberOf || e.scopeClass || '') + '|' + e.kind;
    // locate the use site (the log's line number can be a little off if another repair added lines)
    let p = -1, qualified = false, st = load(errAbs);
    const lines = st.m.split('\n');
    for (const off of [0, 1, -1, 2, -2, 3, -3]) {
      const li = e.line - 1 + off;
      if (li < 0 || li >= lines.length) continue;
      const mm = doctorWordRe(e.name).exec(lines[li]);
      if (!mm) continue;
      const lineStart = lines.slice(0, li).reduce((n, l) => n + l.length + 1, 0);
      p = lineStart + mm.index;
      const pk = prevNonSpace(st.m, p);
      qualified = pk >= 0 && st.m[pk] === '.';
      break;
    }
    if (p < 0) continue;

    const className = e.memberOf || (qualified ? e.scopeClass : null);
    const cands = [];
    const consider = (abs, name, d, ok) => {
      if (!asDeclaration(load(abs).m, d, name.length, e.kind)) return;
      if (!ok(load(abs).m, d)) return;
      const occ = totalOccurrences(name);
      if (occ.length !== 1) return; // referenced elsewhere: it is consistent, not the mismatch
      cands.push({ abs, name, d });
    };
    if (className) {
      // member of a named class: find where that class is declared
      for (const abs of [errAbs, ...javaFiles.filter(f => f !== errAbs)]) {
        const cm = new RegExp('\\b(?:class|interface|enum)\\s+' + className.replace(/[$]/g, '\\$') + '\\b').exec(load(abs).m);
        if (!cm) continue;
        const open = load(abs).m.indexOf('{', cm.index);
        if (open < 0) continue;
        for (const name of translatedNames(abs)) {
          for (const mm of load(abs).m.matchAll(doctorWordRe(name))) {
            consider(abs, name, mm.index, (m, d) => {
              const blk = doctorEnclosingBlock(m, d);
              return blk && blk.open === open;
            });
          }
        }
        break;
      }
    } else {
      const chain = [];
      let blk = doctorEnclosingBlock(st.m, p);
      while (blk) { const cn = doctorClassNameOfHeader(blk.header); if (cn) chain.push(cn); blk = doctorEnclosingBlock(st.m, blk.open); }
      for (const name of translatedNames(errAbs)) {
        for (const mm of st.m.matchAll(doctorWordRe(name))) {
          consider(errAbs, name, mm.index, (m, d) => {
            const blkD = doctorEnclosingBlock(m, d);
            if (!blkD) return false;
            const cn = doctorClassNameOfHeader(blkD.header);
            const paren = doctorInsideParens(m, d);
            if (paren >= 0) { // a method parameter: in scope inside that method's body
              let depth = 0, i = paren;
              for (; i < m.length; i++) { if (m[i] === '(') depth++; else if (m[i] === ')') { depth--; if (depth === 0) break; } }
              const bo = m.indexOf('{', i);
              if (bo < 0 || m.slice(i, bo).indexOf(';') >= 0) return false;
              const bb = doctorEnclosingBlock(m, bo + 1);
              return !!bb && bb.open === bo && p > bo && p < bb.close;
            }
            if (cn) return chain.includes(cn);                 // field of this (or an enclosing) class
            return d < p && p > blkD.open && p < blkD.close;   // local declared before the use, block still open
          });
        }
      }
    }
    if (cands.length !== 1) {
      if (cands.length > 1) fixes.push(`Build Doctor warnings: javac cannot find ${e.kind} "${e.name}" in ${rel(errAbs)}:${e.line}; ${cands.length} translated declarations could match (${cands.map(x => x.name).join(', ')}) so none was renamed -- rename the declaration to "${e.name}" by hand.`);
      continue;
    }
    const cand = cands[0];
    const cs = load(cand.abs);
    cs.c = cs.c.slice(0, cand.d) + e.name + cs.c.slice(cand.d + cand.name.length);
    cs.m = doctorMaskNonCode(cs.c);
    cs.changed = true;
    done.add(key);
    const r = rel(cand.abs);
    if (sidecar[r]) sidecar[r] = sidecar[r].filter(x => x !== cand.name);
    fixes.push(`Renamed translated declaration "${cand.name}" -> "${e.name}" in ${r} (javac could not find ${e.kind} "${e.name}" used at ${rel(errAbs)}:${e.line}; that declaration was the only unreferenced translated one in scope, so only its name changed)`);
  }
  for (const [abs, st] of state) if (st.changed) fs.writeFileSync(abs, st.c, 'utf8');
  if (fixes.length) doctorWriteTranslatedSidecar(projectRoot, sidecar);
  return fixes;
}

function doctorKtLogErrors(log) {
  const out = [];
  const text = String(log || '');
  for (const m of text.matchAll(/e:\s*(?:file:\/\/)?(\S+\.kt)[:\s]\(?(\d+)[:,]\s*(\d+)\)?:?\s*([^\n]*)/g)) {
    out.push({ file: m[1], line: Number(m[2]), col: Number(m[3]), msg: m[4], end: m.index + m[0].length });
  }
  return out;
}

function doctorLineStarts(c) {
  const starts = [0];
  for (let i = 0; i < c.length; i++) if (c[i] === '\n') starts.push(i + 1);
  return starts;
}

function doctorCallArgSpans(code, open) {
  let depth = 0, start = open + 1;
  const spans = [];
  for (let i = open; i < code.length; i++) {
    const ch = code[i];
    if (ch === '(' || ch === '[' || ch === '{') depth++;
    else if (ch === ')' || ch === ']' || ch === '}') {
      depth--;
      if (depth === 0) {
        if (code.slice(start, i).trim()) spans.push([start, i]);
        return { close: i, spans };
      }
    } else if (ch === ',' && depth === 1) { spans.push([start, i]); start = i + 1; }
  }
  return null;
}

function doctorIsPureSimpleExpr(t) {
  const s = String(t || '').replace(/\.(?:toFloat|toPx|toInt|toDouble)\(\)/g, '');
  if (!s.trim()) return false;
  if (!/^[\w.\s*+\-\/()]+$/.test(s)) return false;
  if (/\+\+|--/.test(s)) return false;
  return !/[\w)]\s*\(/.test(s);
}

function fixComposeColorImportFromLog(log, projectRoot) {
  const fixes = [];
  const errs = doctorKtLogErrors(log).filter(e =>
    /inferred type is android\.graphics\.Color but androidx\.compose\.ui\.graphics\.Color was expected/.test(e.msg) ||
    /Too many arguments for public constructor Color\(\) defined in android\.graphics\.Color/.test(e.msg));
  if (!errs.length) return fixes;
  const candidates = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const files = new Set();
  for (const e of errs) { const abs = doctorResolveLogFile(e.file, candidates); if (abs) files.add(abs); }
  for (const abs of files) {
    let c = fs.readFileSync(abs, 'utf8');
    const rel = path.relative(projectRoot, abs);
    if (!/^import\s+android\.graphics\.Color\s*$/m.test(c)) continue;
    if (/^import\s+androidx\.compose\.ui\.graphics\.Color\s*$/m.test(c)) continue;
    const code = doctorMaskNonCode(c);
    if (/\bColor\.(parseColor|rgb|argb|red|green|blue|alpha|colorToHSV|HSVToColor|TRANSPARENT|BLACK|WHITE|RED|GREEN|BLUE|GRAY|GREY|DKGRAY|LTGRAY|YELLOW|CYAN|MAGENTA)\b/.test(code)) {
      fixes.push(`Build Doctor warnings: ${rel} imports android.graphics.Color but the compiler expects the Compose Color, and the file also uses Android-only Color helpers (Color.parseColor / Color.RED ...), so the import was NOT swapped automatically -- use androidx.compose.ui.graphics.Color for Compose colors and write android.graphics.Color.xxx in full for the Android ones.`);
      continue;
    }
    c = c.replace(/^import\s+android\.graphics\.Color\s*$/m, 'import androidx.compose.ui.graphics.Color');
    fs.writeFileSync(abs, c, 'utf8');
    fixes.push(`Switched 'import android.graphics.Color' to 'import androidx.compose.ui.graphics.Color' in ${rel} (compiler: type mismatch android.graphics.Color vs Compose Color / too many arguments for Color(); that file builds Compose colors)`);
  }
  return fixes;
}

function fixCanvasDrawRoundRectFromLog(log, projectRoot) {
  const fixes = [];
  const text = String(log || '');
  const errs = doctorKtLogErrors(text).filter(e => {
    if (!/None of the following functions can be called with the arguments supplied/.test(e.msg)) return false;
    const tail = text.slice(e.end, e.end + 700);
    return /drawRoundRect/.test(tail) && /android\.graphics\.Canvas/.test(tail);
  });
  if (!errs.length) return fixes;
  const candidates = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const byFile = new Map();
  for (const e of errs) {
    const abs = doctorResolveLogFile(e.file, candidates);
    if (!abs) continue;
    if (!byFile.has(abs)) byFile.set(abs, []);
    byFile.get(abs).push(e);
  }
  for (const [abs, list] of byFile) {
    let c = fs.readFileSync(abs, 'utf8');
    const rel = path.relative(projectRoot, abs);
    const code = doctorMaskNonCode(c);
    const starts = doctorLineStarts(c);
    const edits = [];
    const seen = new Set();
    for (const e of list) {
      let open = -1, lineNo = -1;
      for (const off of [0, 1, -1, 2, -2, 3, -3]) {
        const idx = e.line - 1 + off;
        if (idx < 0 || idx >= starts.length) continue;
        const ls = starts[idx];
        const le = idx + 1 < starts.length ? starts[idx + 1] - 1 : c.length;
        const mm = code.slice(ls, le).match(/\bdrawRoundRect\s*\(/);
        if (mm) { open = ls + mm.index + mm[0].length - 1; lineNo = idx + 1; break; }
      }
      if (open < 0 || seen.has(open)) continue;
      seen.add(open);
      const sp = doctorCallArgSpans(code, open);
      if (!sp) continue;
      const args = sp.spans.map(([s, t]) => { const raw = c.slice(s, t); const txt = raw.trim(); return { txt, end: s + raw.replace(/\s+$/, '').length }; });
      if (args.some(a => /^\w+\s*=(?!=)/.test(a.txt))) {
        fixes.push(`Build Doctor warnings: drawRoundRect at ${rel}:${lineNo} uses named arguments, so it was not edited automatically -- android.graphics.Canvas needs (RectF, rx, ry, Paint) or (left, top, right, bottom, rx, ry, Paint).`);
        continue;
      }
      if (args.length === 4 || args.length === 7) continue; // already a valid Canvas overload (e.g. fixed by an earlier pass)
      const lastIsPaint = args.length > 0 && /paint/i.test(args[args.length - 1].txt);
      let dup = -1;
      if (args.length === 3 && lastIsPaint) dup = 1;
      else if (args.length === 6 && lastIsPaint) dup = 4;
      if (dup < 0 || !doctorIsPureSimpleExpr(args[dup].txt)) {
        fixes.push(`Build Doctor warnings: drawRoundRect at ${rel}:${lineNo} has ${args.length} argument(s) and doesn't match android.graphics.Canvas -- it needs (RectF, rx, ry, Paint) or (left, top, right, bottom, rx, ry, Paint). Not edited automatically because the fix isn't obvious.`);
        continue;
      }
      edits.push({ at: args[dup].end, ins: ', ' + args[dup].txt, lineNo, n: args.length });
    }
    if (!edits.length) continue;
    edits.sort((a, b) => b.at - a.at);
    for (const ed of edits) c = c.slice(0, ed.at) + ed.ins + c.slice(ed.at);
    fs.writeFileSync(abs, c, 'utf8');
    fixes.push(`Added the missing second corner radius to drawRoundRect in ${rel}:${edits.map(x => x.lineNo).reverse().join(', ')} (android.graphics.Canvas needs rx AND ry; the call passed only one radius, so it was repeated)`);
  }
  return fixes;
}

const DOCTOR_COMPOSE_OPTIN_BY_WORD = {
  'foundation': 'androidx.compose.foundation.ExperimentalFoundationApi',
  'animation': 'androidx.compose.animation.ExperimentalAnimationApi',
  'foundation layout': 'androidx.compose.foundation.layout.ExperimentalLayoutApi'
};

function doctorAddFileOptIn(c, simpleName) {
  const ann = c.match(/^@file:OptIn\(([^)]*)\)([ \t]*(?:\/\/[^\n]*)?)$/m);
  if (ann) {
    if (new RegExp('\\b' + simpleName + '::class').test(ann[1])) return c;
    const next = '@file:OptIn(' + ann[1].replace(/[\s,]+$/, '') + ', ' + simpleName + '::class)' + ann[2];
    return c.slice(0, ann.index) + next + c.slice(ann.index + ann[0].length);
  }
  const line = '@file:OptIn(' + simpleName + '::class)\n';
  const pkg = c.match(/^package\s/m);
  return pkg ? c.slice(0, pkg.index) + line + c.slice(pkg.index) : line + c;
}

function fixExperimentalComposeApiFromLog(log, projectRoot) {
  const fixes = [];
  const items = [];
  for (const e of doctorKtLogErrors(log)) {
    let fq = null, word = null;
    const mk = e.msg.match(/@OptIn\(\s*([\w.]+)::class\s*\)/) || e.msg.match(/marked with '@([\w.]+)'/);
    if (mk && /^androidx\.[\w.]*\.Experimental\w+$/.test(mk[1])) fq = mk[1];
    else {
      const w = e.msg.match(/^This ([\w ]+?) API is experimental/i);
      if (w) word = w[1].toLowerCase();
    }
    if (fq || word) items.push({ file: e.file, fq, word });
  }
  if (!items.length) return fixes;
  const candidates = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const byFile = new Map();
  for (const it of items) {
    const abs = doctorResolveLogFile(it.file, candidates);
    if (!abs) continue;
    if (!byFile.has(abs)) byFile.set(abs, []);
    byFile.get(abs).push(it);
  }
  for (const [abs, list] of byFile) {
    let c = fs.readFileSync(abs, 'utf8');
    const rel = path.relative(projectRoot, abs);
    const want = new Set();
    for (const it of list) {
      let fq = it.fq;
      if (!fq && it.word === 'material') {
        if (/^import\s+androidx\.compose\.material3\./m.test(c)) fq = 'androidx.compose.material3.ExperimentalMaterial3Api';
        else if (/^import\s+androidx\.compose\.material\./m.test(c)) fq = 'androidx.compose.material.ExperimentalMaterialApi';
      } else if (!fq) fq = DOCTOR_COMPOSE_OPTIN_BY_WORD[it.word] || null;
      if (fq) want.add(fq);
      else fixes.push(`Build Doctor warnings: ${rel} uses an experimental "${it.word}" API but the opt-in marker couldn't be worked out safely -- add @OptIn(<the Experimental...Api class>::class) by hand.`);
    }
    const added = [];
    for (const fq of want) {
      const simple = fq.slice(fq.lastIndexOf('.') + 1);
      const pkgOf = fq.slice(0, fq.lastIndexOf('.'));
      const before = c;
      const hasImport = new RegExp('^import\\s+' + fq.replace(/\./g, '\\.') + '\\s*$', 'm').test(c) ||
        new RegExp('^import\\s+' + pkgOf.replace(/\./g, '\\.') + '\\.\\*\\s*$', 'm').test(c);
      if (!hasImport) c = doctorAddImport(c, fq, true);
      c = doctorAddFileOptIn(c, simple);
      if (c !== before) added.push(simple);
    }
    if (!added.length) continue;
    fs.writeFileSync(abs, c, 'utf8');
    fixes.push(`Added @file:OptIn(${added.map(a => a + '::class').join(', ')}) to ${rel} (compiler: "This ... API is experimental and is likely to change or to be removed in the future")`);
  }
  return fixes;
}

function warnReturnInExpressionBodyFromLog(log, projectRoot) {
  const fixes = [];
  const seen = new Set();
  for (const e of doctorKtLogErrors(log)) {
    if (!/Returns are not allowed for functions with expression body/.test(e.msg)) continue;
    const key = path.basename(e.file) + ':' + e.line;
    if (seen.has(key)) continue;
    seen.add(key);
    fixes.push(`Build Doctor warnings: ${path.basename(e.file)}:${e.line}:${e.col} uses 'return' inside a function written as 'fun x() = ...' with no return type. Change it to a block body with an explicit type: 'fun x(): <Type> { return ... }'. Not edited automatically because the return type can't be known from the log.`);
  }
  return fixes;
}

function doctorIsComposeFile(c) {
  return /@Composable\b/.test(c) || /^import\s+androidx\.compose\./m.test(c);
}

function doctorMaterialBase(c, projectRoot) {
  if (/^import\s+androidx\.compose\.material3\./m.test(c)) return 'androidx.compose.material3';
  if (/^import\s+androidx\.compose\.material\.(?!icons)/m.test(c)) return 'androidx.compose.material';
  try {
    let m3 = 0, m2 = 0;
    for (const f of listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name)).slice(0, 300)) {
      const t = fs.readFileSync(f, 'utf8');
      if (/^import\s+androidx\.compose\.material3\./m.test(t)) m3++;
      else if (/^import\s+androidx\.compose\.material\.(?!icons)/m.test(t)) m2++;
    }
    if (m2 > m3) return 'androidx.compose.material';
  } catch (e) { /* default below */ }
  return 'androidx.compose.material3';
}

const DOCTOR_COMPOSE_MATERIAL_NAMES = new Set([
  'Text', 'Icon', 'IconButton', 'Button', 'OutlinedButton', 'TextButton', 'Card', 'Surface', 'Scaffold', 'TopAppBar',
  'BottomAppBar', 'FloatingActionButton', 'ExtendedFloatingActionButton', 'TextField', 'OutlinedTextField', 'Switch',
  'Checkbox', 'RadioButton', 'Slider', 'CircularProgressIndicator', 'LinearProgressIndicator', 'AlertDialog',
  'DropdownMenu', 'DropdownMenuItem', 'Snackbar', 'SnackbarHost', 'SnackbarHostState', 'MaterialTheme', 'Divider',
  'ListItem', 'Tab', 'TabRow', 'Typography', 'Shapes', 'ModalDrawerSheet', 'ModalNavigationDrawer', 'DrawerValue',
  'rememberDrawerState', 'LocalContentColor', 'ProvideTextStyle', 'darkColorScheme', 'lightColorScheme'
]);

const DOCTOR_COMPOSE_M3_ONLY = new Set([
  'HorizontalDivider', 'VerticalDivider', 'NavigationBar', 'NavigationBarItem', 'CenterAlignedTopAppBar',
  'LargeTopAppBar', 'MediumTopAppBar', 'ElevatedCard', 'OutlinedCard', 'FilledTonalButton', 'ElevatedButton',
  'ModalBottomSheet', 'SearchBar', 'FilterChip', 'AssistChip', 'SegmentedButton', 'PrimaryTabRow',
  'darkColorScheme', 'lightColorScheme', 'ColorScheme', 'dynamicDarkColorScheme', 'dynamicLightColorScheme'
]);

const DOCTOR_COMPOSE_FIXED = {
  // layout
  Column: 'androidx.compose.foundation.layout.Column', Row: 'androidx.compose.foundation.layout.Row', Box: 'androidx.compose.foundation.layout.Box',
  Spacer: 'androidx.compose.foundation.layout.Spacer', Arrangement: 'androidx.compose.foundation.layout.Arrangement',
  PaddingValues: 'androidx.compose.foundation.layout.PaddingValues', BoxWithConstraints: 'androidx.compose.foundation.layout.BoxWithConstraints',
  WindowInsets: 'androidx.compose.foundation.layout.WindowInsets',
  fillMaxSize: 'androidx.compose.foundation.layout.fillMaxSize', fillMaxWidth: 'androidx.compose.foundation.layout.fillMaxWidth',
  fillMaxHeight: 'androidx.compose.foundation.layout.fillMaxHeight', padding: 'androidx.compose.foundation.layout.padding',
  size: 'androidx.compose.foundation.layout.size', height: 'androidx.compose.foundation.layout.height', width: 'androidx.compose.foundation.layout.width',
  wrapContentSize: 'androidx.compose.foundation.layout.wrapContentSize', wrapContentWidth: 'androidx.compose.foundation.layout.wrapContentWidth',
  wrapContentHeight: 'androidx.compose.foundation.layout.wrapContentHeight', widthIn: 'androidx.compose.foundation.layout.widthIn',
  heightIn: 'androidx.compose.foundation.layout.heightIn', sizeIn: 'androidx.compose.foundation.layout.sizeIn',
  defaultMinSize: 'androidx.compose.foundation.layout.defaultMinSize', offset: 'androidx.compose.foundation.layout.offset',
  aspectRatio: 'androidx.compose.foundation.layout.aspectRatio', imePadding: 'androidx.compose.foundation.layout.imePadding',
  systemBarsPadding: 'androidx.compose.foundation.layout.systemBarsPadding', statusBarsPadding: 'androidx.compose.foundation.layout.statusBarsPadding',
  navigationBarsPadding: 'androidx.compose.foundation.layout.navigationBarsPadding',
  // foundation
  background: 'androidx.compose.foundation.background', border: 'androidx.compose.foundation.border', clickable: 'androidx.compose.foundation.clickable',
  verticalScroll: 'androidx.compose.foundation.verticalScroll', horizontalScroll: 'androidx.compose.foundation.horizontalScroll',
  rememberScrollState: 'androidx.compose.foundation.rememberScrollState', Image: 'androidx.compose.foundation.Image',
  BorderStroke: 'androidx.compose.foundation.BorderStroke', isSystemInDarkTheme: 'androidx.compose.foundation.isSystemInDarkTheme',
  LazyColumn: 'androidx.compose.foundation.lazy.LazyColumn', LazyRow: 'androidx.compose.foundation.lazy.LazyRow',
  rememberLazyListState: 'androidx.compose.foundation.lazy.rememberLazyListState', itemsIndexed: 'androidx.compose.foundation.lazy.itemsIndexed',
  LazyVerticalGrid: 'androidx.compose.foundation.lazy.grid.LazyVerticalGrid', GridCells: 'androidx.compose.foundation.lazy.grid.GridCells',
  RoundedCornerShape: 'androidx.compose.foundation.shape.RoundedCornerShape', CircleShape: 'androidx.compose.foundation.shape.CircleShape',
  KeyboardOptions: 'androidx.compose.foundation.text.KeyboardOptions',
  // ui
  Alignment: 'androidx.compose.ui.Alignment', clip: 'androidx.compose.ui.draw.clip', alpha: 'androidx.compose.ui.draw.alpha',
  shadow: 'androidx.compose.ui.draw.shadow', rotate: 'androidx.compose.ui.draw.rotate', scale: 'androidx.compose.ui.draw.scale',
  drawBehind: 'androidx.compose.ui.draw.drawBehind', graphicsLayer: 'androidx.compose.ui.graphics.graphicsLayer',
  Brush: 'androidx.compose.ui.graphics.Brush', Shape: 'androidx.compose.ui.graphics.Shape', asImageBitmap: 'androidx.compose.ui.graphics.asImageBitmap',
  ImageBitmap: 'androidx.compose.ui.graphics.ImageBitmap', toArgb: 'androidx.compose.ui.graphics.toArgb', Offset: 'androidx.compose.ui.geometry.Offset',
  ContentScale: 'androidx.compose.ui.layout.ContentScale', LocalContext: 'androidx.compose.ui.platform.LocalContext',
  LocalDensity: 'androidx.compose.ui.platform.LocalDensity', LocalConfiguration: 'androidx.compose.ui.platform.LocalConfiguration',
  painterResource: 'androidx.compose.ui.res.painterResource', stringResource: 'androidx.compose.ui.res.stringResource',
  FontWeight: 'androidx.compose.ui.text.font.FontWeight', FontFamily: 'androidx.compose.ui.text.font.FontFamily',
  TextAlign: 'androidx.compose.ui.text.style.TextAlign', TextOverflow: 'androidx.compose.ui.text.style.TextOverflow',
  TextDecoration: 'androidx.compose.ui.text.style.TextDecoration', TextStyle: 'androidx.compose.ui.text.TextStyle',
  KeyboardType: 'androidx.compose.ui.text.input.KeyboardType', ImeAction: 'androidx.compose.ui.text.input.ImeAction',
  PasswordVisualTransformation: 'androidx.compose.ui.text.input.PasswordVisualTransformation',
  Dp: 'androidx.compose.ui.unit.Dp', IntOffset: 'androidx.compose.ui.unit.IntOffset', Icons: 'androidx.compose.material.icons.Icons',
  // runtime
  collectAsState: 'androidx.compose.runtime.collectAsState', derivedStateOf: 'androidx.compose.runtime.derivedStateOf',
  DisposableEffect: 'androidx.compose.runtime.DisposableEffect', SideEffect: 'androidx.compose.runtime.SideEffect',
  rememberCoroutineScope: 'androidx.compose.runtime.rememberCoroutineScope', produceState: 'androidx.compose.runtime.produceState',
  mutableStateListOf: 'androidx.compose.runtime.mutableStateListOf', mutableFloatStateOf: 'androidx.compose.runtime.mutableFloatStateOf',
  mutableLongStateOf: 'androidx.compose.runtime.mutableLongStateOf', mutableStateMapOf: 'androidx.compose.runtime.mutableStateMapOf',
  State: 'androidx.compose.runtime.State', MutableState: 'androidx.compose.runtime.MutableState',
  CompositionLocalProvider: 'androidx.compose.runtime.CompositionLocalProvider', compositionLocalOf: 'androidx.compose.runtime.compositionLocalOf',
  staticCompositionLocalOf: 'androidx.compose.runtime.staticCompositionLocalOf', rememberUpdatedState: 'androidx.compose.runtime.rememberUpdatedState',
  snapshotFlow: 'androidx.compose.runtime.snapshotFlow', Stable: 'androidx.compose.runtime.Stable', Immutable: 'androidx.compose.runtime.Immutable',
  // coroutines
  launch: 'kotlinx.coroutines.launch', delay: 'kotlinx.coroutines.delay', withContext: 'kotlinx.coroutines.withContext',
  Dispatchers: 'kotlinx.coroutines.Dispatchers', CoroutineScope: 'kotlinx.coroutines.CoroutineScope', async: 'kotlinx.coroutines.async'
};

function doctorComposeImportFor(name, c, projectRoot) {
  if (!doctorIsComposeFile(c)) return null;
  if (name === 'Color') {
    const code = doctorMaskNonCode(c);
    if (/\bColor\.(parseColor|rgb|argb|red|green|blue|alpha|colorToHSV|HSVToColor|TRANSPARENT|BLACK|WHITE|RED|GREEN|BLUE|GRAY|GREY|DKGRAY|LTGRAY|YELLOW|CYAN|MAGENTA)\b/.test(code)) return null;
    return 'androidx.compose.ui.graphics.Color';
  }
  if (name === 'items') return /^import\s+androidx\.compose\.foundation\.lazy\.grid\./m.test(c) ? 'androidx.compose.foundation.lazy.grid.items' : 'androidx.compose.foundation.lazy.items';
  if (DOCTOR_COMPOSE_M3_ONLY.has(name)) return 'androidx.compose.material3.' + name;
  if (DOCTOR_COMPOSE_MATERIAL_NAMES.has(name)) return doctorMaterialBase(c, projectRoot) + '.' + name;
  return DOCTOR_COMPOSE_FIXED[name] || null;
}

function fixComposeDelegateImportsFromLog(log, projectRoot) {
  const fixes = [];
  const errs = doctorKtLogErrors(log).filter(e => /delegate/i.test(e.msg) && /\b(getValue|setValue)\(/.test(e.msg));
  if (!errs.length) return fixes;
  const candidates = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const byFile = new Map();
  for (const e of errs) {
    const abs = doctorResolveLogFile(e.file, candidates);
    if (!abs) continue;
    if (!byFile.has(abs)) byFile.set(abs, new Set());
    for (const m of e.msg.matchAll(/\b(getValue|setValue)\(/g)) byFile.get(abs).add(m[1]);
  }
  for (const [abs, need] of byFile) {
    let c = fs.readFileSync(abs, 'utf8');
    const rel = path.relative(projectRoot, abs);
    const usesState = /\bby\s+(?:remember|rememberSaveable|mutableStateOf|derivedStateOf|produceState|[\w.]+\.collectAsState|animate\w+AsState)/.test(c);
    if (!usesState && !doctorIsComposeFile(c)) continue;
    const added = [];
    for (const fn of need) {
      const fq = 'androidx.compose.runtime.' + fn;
      if (new RegExp('^import\\s+' + fq.replace(/\./g, '\\.') + '\\s*$', 'm').test(c)) continue;
      if (/^import\s+androidx\.compose\.runtime\.\*\s*$/m.test(c)) continue;
      c = doctorAddImport(c, fq, true);
      added.push(fn);
    }
    if (!added.length) continue;
    fs.writeFileSync(abs, c, 'utf8');
    fixes.push(`Added import androidx.compose.runtime.${added.join(' / ')} to ${rel} (compiler: 'by remember { mutableStateOf(...) }' needs it to work as a property delegate)`);
  }
  return fixes;
}

function doctorFindExprBodyFun(c, code, pos, starts) {
  const re = /\bfun\b/g;
  const cands = [];
  let m;
  while ((m = re.exec(code)) && m.index < pos) cands.push(m.index);
  for (let k = cands.length - 1; k >= 0 && k >= cands.length - 8; k--) {
    const at = cands[k];
    const hm = /^fun\s*(?:<[^>]*>\s*)?(?:[\w.<>?,\s]*?\.)?(\w+)\s*\(/.exec(code.slice(at, at + 300));
    if (!hm) continue;
    const open = at + hm[0].length - 1;
    const sp = doctorCallArgSpans(code, open);
    if (!sp) continue;
    let i = sp.close + 1;
    while (i < code.length && /\s/.test(code[i])) i++;
    if (code[i] === ':') return { explicitType: true };
    if (code[i] !== '=') continue;
    let exprStart = i + 1;
    while (exprStart < code.length && /\s/.test(code[exprStart])) exprStart++;
    const exprEnd = doctorFindKotlinExprEnd(c, exprStart);
    if (exprEnd > exprStart && pos >= exprStart && pos < exprEnd) {
      return { at, name: hm[1], headerEnd: sp.close + 1, exprStart, exprEnd };
    }
  }
  return null;
}

function fixReturnInExpressionBodyFromLog(log, projectRoot) {
  const fixes = [];
  const errs = doctorKtLogErrors(log).filter(e => /Returns are not allowed for functions with expression body/.test(e.msg));
  if (!errs.length) return fixes;
  const allKt = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const byFile = new Map();
  for (const e of errs) {
    const abs = doctorResolveLogFile(e.file, allKt);
    if (!abs) { fixes.push(...warnReturnInExpressionBodyFromLog(log, projectRoot).slice(0, 1)); continue; }
    if (!byFile.has(abs)) byFile.set(abs, []);
    byFile.get(abs).push(e);
  }
  for (const [abs, list] of byFile) {
    let c = fs.readFileSync(abs, 'utf8');
    const rel = path.relative(projectRoot, abs);
    const done = [];
    // bottom-to-top, so an edit never shifts the line numbers of the errors still to be handled
    list.sort((x, y) => y.line - x.line || y.col - x.col);
    for (const e of list) {
      const code = doctorMaskNonCode(c);
      const starts = doctorLineStarts(c);
      const li = e.line - 1;
      if (li < 0 || li >= starts.length) continue;
      const lineEnd = li + 1 < starts.length ? starts[li + 1] - 1 : c.length;
      let pos = starts[li] + Math.max(0, e.col - 1);
      if (!/^return\b(?!@)/.test(code.slice(pos, pos + 8))) {
        const mm = /\breturn\b(?!@)/.exec(code.slice(starts[li], lineEnd));
        if (!mm) continue;
        pos = starts[li] + mm.index;
      }
      const decl = doctorFindExprBodyFun(c, code, pos, starts);
      const spot = `${rel}:${e.line}:${e.col}`;
      if (!decl || decl.explicitType) {
        fixes.push(`Build Doctor warnings: ${spot} uses 'return' inside a function with an expression body, but the function could not be located safely -- change it to a block body with an explicit return type by hand.`);
        continue;
      }
      const exprText = c.slice(decl.exprStart, decl.exprEnd);
      const found = new Set();
      // (a) call sites with a declared variable type
      const callRe = new RegExp('(?:val|var)\\s+\\w+\\s*:\\s*([^=\\n]+?)\\s*=\\s*(?:[\\w.]+\\.)?' + decl.name + '\\s*\\(', 'g');
      for (const f of allKt) {
        let t; try { t = fs.readFileSync(f, 'utf8'); } catch { continue; }
        for (const m of t.matchAll(callRe)) {
          const ty = m[1].trim();
          if (ty && !/^(Nothing|Unit)\??$/.test(ty)) found.add(ty);
        }
      }
      // (b) cache lookup: map[key] ?: return ...
      const lk = exprText.match(/\b(\w+)\s*(?:\[[^\]]*\]|\.get\([^)]*\))\s*\?:\s*return\b/);
      if (lk) {
        const dr = new RegExp('(?:val|var)\\s+' + lk[1] + '\\s*(?::\\s*[\\w.]+\\s*<\\s*[^<>,]+,\\s*([^<>]+(?:<[^<>]*>)?)\\s*>|=\\s*[\\w.]*(?:Map|Cache)\\w*\\s*<\\s*[^<>,]+,\\s*([^<>]+(?:<[^<>]*>)?)\\s*>)');
        const dm = c.match(dr);
        const v = dm && (dm[1] || dm[2]);
        if (v) found.add(v.trim());
      }
      const bases = new Set([...found].map(t => t.replace(/\?$/, '')));
      if (bases.size !== 1) {
        fixes.push(`Build Doctor warnings: ${spot} uses 'return' inside a function written as 'fun ${decl.name}(...) = ...' with no return type, and the type could not be worked out safely (${bases.size === 0 ? 'no call site or cache declaration shows it' : 'call sites disagree: ' + [...bases].join(' / ')}). Change it to 'fun ${decl.name}(...): <Type> { return ... }'.`);
        continue;
      }
      let type = [...bases][0];
      if ([...found].some(t => t.endsWith('?')) || /\breturn\s+null\b/.test(exprText)) type += '?';
      const lineStart = c.lastIndexOf('\n', decl.at) + 1;
      const indent = (c.slice(lineStart, decl.at).match(/^\s*/) || [''])[0];
      const body = exprText.replace(/\s+$/, '');
      c = c.slice(0, decl.headerEnd) + `: ${type} {\n${indent}    return ${body}\n${indent}}` + c.slice(decl.exprEnd);
      done.push(`${decl.name}(): ${type}`);
    }
    if (done.length) {
      fs.writeFileSync(abs, c, 'utf8');
      fixes.push(`Converted ${done.join(', ')} in ${rel} to a block body with an explicit return type (Kotlin does not allow 'return' inside 'fun x() = ...' without a declared type; the type came from how the function is used / the cache it reads)`);
    }
  }
  return fixes;
}

function fixFinalMemberOverrideFromLog(log, projectRoot) {
  const fixes = [];
  const errs = doctorKtLogErrors(log).map(e => {
    const m = /'([\p{L}\p{N}_$]+)'\s+in\s+'([\p{L}\p{N}_$.]+)'\s+is final and cannot be overridden/u.exec(e.msg);
    return m ? { ...e, name: m[1], owner: m[2] } : null;
  }).filter(Boolean);
  if (!errs.length) return fixes;
  const allKt = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const seen = new Set();
  for (const e of errs) {
    const key = `${e.file}:${e.line}:${e.name}`;
    if (seen.has(key)) continue;
    seen.add(key);
    const abs = doctorResolveLogFile(e.file, allKt);
    const spot = `${path.basename(e.file)}:${e.line}`;
    if (!abs) { fixes.push(`Build Doctor warnings: ${spot} overrides final '${e.name}' of ${e.owner}, but the file could not be located in the project.`); continue; }
    let c = fs.readFileSync(abs, 'utf8');
    const rel = path.relative(projectRoot, abs);
    const starts = doctorLineStarts(c);
    const li = e.line - 1;
    if (li < 0 || li >= starts.length) continue;
    const lineEnd = li + 1 < starts.length ? starts[li + 1] - 1 : c.length;
    const line = c.slice(starts[li], lineEnd);
    const declRe = new RegExp('\\boverride\\s+((?:(?:public|protected|internal|open|final|inline)\\s+)*)fun\\s+(?:<[^>]*>\\s*)?' + e.name + '\\s*\\(');
    const dm = declRe.exec(line);
    if (!dm) {
      fixes.push(`Build Doctor warnings: ${rel}:${e.line} overrides final '${e.name}' of ${e.owner}, but it is not a plain 'override fun ${e.name}(...)' -- rename that member by hand and remove 'override'.`);
      continue;
    }
    // pick a new name that is not already used anywhere in this file
    let newName = e.name + 'Value';
    for (let n = 2; new RegExp('(?<![\\p{L}\\p{N}_$])' + newName + '(?![\\p{L}\\p{N}_$])', 'u').test(c); n++) newName = e.name + 'Value' + n;
    const code = doctorMaskNonCode(c);
    const edits = []; // [start, end, text]
    // 1) the declaration: drop `override `, rename
    const declAbs = starts[li] + dm.index;
    const nameAbs = c.indexOf(e.name, declAbs + dm[0].indexOf('fun'));
    edits.push([declAbs, declAbs + dm[0].indexOf('fun'), dm[1] || '']);
    edits.push([nameAbs, nameAbs + e.name.length, newName]);
    // 2) calls in this file: bare `name(` or `this.name(` (never `x.name(` / `super.name(`)
    const callRe = new RegExp('(?<![\\p{L}\\p{N}_$.])(?:this\\s*\\.\\s*)?(' + e.name + ')\\s*\\(', 'gu');
    let calls = 0;
    for (const m of code.matchAll(callRe)) {
      const at = m.index + m[0].indexOf(e.name);
      if (at === nameAbs) continue;
      if (/\bfun\s+$/.test(code.slice(Math.max(0, at - 12), at))) continue; // another declaration
      edits.push([at, at + e.name.length, newName]);
      calls++;
    }
    edits.sort((a, b) => b[0] - a[0]);
    for (const [s, en, t] of edits) c = c.slice(0, s) + t + c.slice(en);
    fs.writeFileSync(abs, c, 'utf8');
    fixes.push(`Renamed '${e.name}' to '${newName}' in ${rel} and removed 'override' (${e.owner}.${e.name} is final in the Android framework so Kotlin can't override it; updated ${calls} call(s) in the same file)`);
  }
  return fixes;
}

function fixKotlinSyntaxErrorFromLog(log, projectRoot) {
  const fixes = [];
  const errs = doctorKtLogErrors(log).filter(e => /^Syntax error:/i.test(e.msg));
  if (!errs.length) return fixes;
  const allKt = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const lines = new Map(); // abs -> sorted unique line numbers
  const colAt = new Map(); // "abs:line" -> column the compiler pointed at
  for (const e of errs) {
    const abs = doctorResolveLogFile(e.file, allKt);
    if (!abs) continue;
    if (!colAt.has(abs + ':' + e.line)) colAt.set(abs + ':' + e.line, e.col);
    if (!lines.has(abs)) lines.set(abs, new Map());
    const per = lines.get(abs);
    if (!per.has(e.line)) per.set(e.line, []);
    per.get(e.line).push(e.msg);
  }
  for (const [abs, per] of lines) {
    let c = fs.readFileSync(abs, 'utf8');
    const rel = path.relative(projectRoot, abs);
    const arr = c.split('\n');
    let changed = false;
    // How many more '}' than '{' the file has (string/comment text ignored). A
    // "stray" closing brace can only be removed while this is still positive.
    let strayBudget = 0;
    {
      const masked = doctorMaskNonCode(c);
      strayBudget = (masked.match(/\}/g) || []).length - (masked.match(/\{/g) || []).length;
    }
    for (const [lineNo, msgs] of [...per.entries()].sort((a, b) => a[0] - b[0])) {
      const idx = lineNo - 1;
      if (idx < 0 || idx >= arr.length) continue;
      let line = arr[idx];
      const original = line;
      const notes = [];
      // (a) typographic quotes
      if (/[\u201C\u201D\u201E\u201F]/.test(line) || /[\u2018\u2019]/.test(line)) {
        const fixed = line.replace(/[\u201C\u201D\u201E\u201F]/g, '"').replace(/[\u2018\u2019]/g, "'");
        // only keep it if it no longer contains them outside of "..." text (inside a string they are harmless)
        const strippedStrings = line.replace(/"(?:\\.|[^"\\])*"/g, '""');
        if (/[\u201C\u201D\u201E\u201F\u2018\u2019]/.test(strippedStrings)) { line = fixed; notes.push('typographic quotes replaced with straight quotes'); }
      }
      // (b) unescaped inner double quotes, single-line, not a raw string, not a template with nested quotes
      if (!notes.length && !line.includes('"""') && !/\$\{[^}]*"/.test(line)) {
        const unesc = [];
        for (let i = 0; i < line.length; i++) { if (line[i] === '\\') { i++; continue; } if (line[i] === '"') unesc.push(i); }
        if (unesc.length >= 4 && unesc.length % 2 === 0) {
          // look for the pattern  ( ... "text "word" more text" ... )  : quote that touches a letter on both sides
          const candidates = [];
          for (let k = 1; k < unesc.length - 1; k++) {
            const p = unesc[k], before = line[p - 1] || ' ', after = line[p + 1] || ' ';
            // opening inner quote:  <space>"word   or closing inner quote: word"<space/punct>
            if (/\s/.test(before) && /[\p{L}\p{N}]/u.test(after)) candidates.push(p);
            else if (/[\p{L}\p{N}.!?]/u.test(before) && /[\s.,!?]/.test(after) && !/[,)]/.test(after)) candidates.push(p);
          }
          if (candidates.length && candidates.length % 2 === 0 && unesc.length - candidates.length === 2) {
            let out = line;
            for (const p of candidates.slice().sort((x, y) => y - x)) out = out.slice(0, p) + '\\' + out.slice(p);
            line = out; notes.push('unescaped quote(s) inside a string literal escaped with a backslash');
          }
        }
      }
      // (c) missing trailing ')' on a complete single-line call statement
      if (!notes.length && msgs.some(m => /Expecting '\)'/.test(m))) {
        const masked = doctorMaskNonCode(line);
        const opens = (masked.match(/\(/g) || []).length, closes = (masked.match(/\)/g) || []).length;
        const trimmed = line.replace(/\s+$/, '');
        let nextCode = '';
        for (let j = idx + 1; j < arr.length; j++) { const t = arr[j].trim(); if (t && !t.startsWith('//')) { nextCode = t; break; } }
        const endsOpen = /[,(+\-*\/&|=.?:<>]$/.test(trimmed) || /[{]$/.test(trimmed);
        const nextContinues = /^[).,?:+\-*\/&|]/.test(nextCode) || /^\{|^else\b/.test(nextCode);
        if (opens - closes === 1 && !endsOpen && !nextContinues && trimmed.length) {
          const tail = line.slice(trimmed.length); // keep trailing whitespace/comment position
          line = line.slice(0, trimmed.length) + ')' + tail;
          notes.push("added the missing closing ')' at the end of the statement");
        }
      }
      // (d) "Expecting a top level declaration" exactly on a '}' while the file
      //     has more '}' than '{': that brace closes something that was never
      //     opened. Remove just that one character (the line is blanked, never
      //     deleted, so the other line numbers in the log stay valid).
      if (!notes.length && strayBudget > 0 && msgs.some(m => /Expecting a top level declaration/i.test(m))) {
        const col = colAt.get(abs + ':' + lineNo);
        if (col && original[col - 1] === '}') {
          const without = original.slice(0, col - 1) + original.slice(col);
          line = without.trim() === '' ? '' : without;
          strayBudget--;
          notes.push("removed one stray closing '}' -- the file has more '}' than '{' and the compiler stopped exactly on it; if a class or function looks cut short, check the closing brace just above it");
        }
      }
      if (notes.length && line !== original) {
        arr[idx] = line; changed = true;
        fixes.push(`Fixed Kotlin syntax error at ${rel}:${lineNo} (${notes.join('; ')})`);
      } else {
        const shown = original.trim().slice(0, 220);
        fixes.push(`Build Doctor warnings: ${rel}:${lineNo} has a Kotlin syntax error (${msgs[0].replace(/^Syntax error:\s*/i, '').slice(0, 80)}) that no safe automatic rule matched. Source line: ${shown}`);
      }
    }
    if (changed) fs.writeFileSync(abs, arr.join('\n'), 'utf8');
  }
  return fixes;
}

function fixMisplacedPackagingBlockFromLog(log, projectRoot) {
  const fixes = [];
  const text = String(log || '');
  const hits = [];
  const seen = new Set();
  for (const m of text.matchAll(/Build file '([^']+)' line:\s*(\d+)[\s\S]{0,400}?Could not find method (resources|jniLibs)\(\)/g)) {
    const key = `${m[1]}:${m[2]}:${m[3]}`;
    if (!seen.has(key)) { seen.add(key); hits.push({ file: m[1], line: parseInt(m[2], 10), name: m[3] }); }
  }
  if (!hits.length) return fixes;
  const gradleFiles = listProjectFiles(projectRoot, (full, name) => /^build\.gradle$/.test(name));
  let agp = null;
  try { agp = xrDetectAgp(projectRoot); } catch (e) { agp = null; }
  const blockName = agp && agp.major >= 8 ? 'packaging' : 'packagingOptions';
  const byFile = new Map();
  for (const h of hits) {
    const abs = doctorResolveLogFile(h.file, gradleFiles);
    if (!abs) { fixes.push(`Build Doctor warnings: ${path.basename(h.file)}:${h.line} has ${h.name} { } outside a packaging block, but the file could not be located.`); continue; }
    if (!byFile.has(abs)) byFile.set(abs, []);
    byFile.get(abs).push(h);
  }
  for (const [abs, list] of byFile) {
    let c = fs.readFileSync(abs, 'utf8');
    const rel = path.relative(projectRoot, abs);
    let changed = false;
    list.sort((a, b) => b.line - a.line);
    for (const h of list) {
      const code = doctorMaskNonCode(c);
      const starts = doctorLineStarts(c);
      const li = h.line - 1;
      if (li < 0 || li >= starts.length) continue;
      const lineEnd = li + 1 < starts.length ? starts[li + 1] - 1 : c.length;
      const m = new RegExp('^([ \\t]*)' + h.name + '\\s*\\{').exec(code.slice(starts[li], lineEnd));
      if (!m) { fixes.push(`Build Doctor warnings: ${rel}:${h.line} is not a plain '${h.name} {' line -- wrap it in ${blockName} { } by hand.`); continue; }
      const open = starts[li] + m[0].length - 1;
      let d = 0, close = -1;
      for (let i = open; i < code.length; i++) {
        if (code[i] === '{') d++;
        else if (code[i] === '}') { d--; if (d === 0) { close = i; break; } }
      }
      if (close < 0) continue;
      const indent = m[1];
      const bodyStart = starts[li] + indent.length;
      const inner = c.slice(bodyStart, close + 1).split('\n').map((l, k) => (k === 0 ? l : '    ' + l)).join('\n');
      c = c.slice(0, bodyStart) + `${blockName} {\n${indent}    ${inner}\n${indent}}` + c.slice(close + 1);
      changed = true;
      fixes.push(`Wrapped ${h.name} { } in ${blockName} { } at ${rel}:${h.line} (it must live inside the packaging block, not directly in android { })`);
    }
    if (changed) fs.writeFileSync(abs, c, 'utf8');
  }
  return fixes;
}

const DOCTOR_MOVED_FRAMEWORK_CLASSES = {
  'android.net.HttpResponseCache': 'android.net.http.HttpResponseCache'
};

function fixMovedFrameworkClasses(projectRoot) {
  const fixes = [];
  const entries = Object.entries(DOCTOR_MOVED_FRAMEWORK_CLASSES);
  const files = listProjectFiles(projectRoot, (full, name) => /\.(java|kt)$/.test(name));
  for (const abs of files) {
    let c;
    try { c = fs.readFileSync(abs, 'utf8'); } catch { continue; }
    const isKotlin = /\.kt$/.test(abs);
    let out = c;
    const done = [];
    for (const [wrong, right] of entries) {
      const simple = wrong.slice(wrong.lastIndexOf('.') + 1);
      const wrongPkg = wrong.slice(0, wrong.lastIndexOf('.'));
      const rightPkg = right.slice(0, right.lastIndexOf('.'));
      const hadWildcard = new RegExp('^import\\s+' + wrongPkg.replace(/\./g, '\\.') + '\\.\\*', 'm').test(out);
      if (out.includes(wrong)) {
        const re = new RegExp('(?<![\\w.])' + wrong.replace(/\./g, '\\.') + '(?!\\w)', 'g');
        const next = out.replace(re, right);
        if (next !== out) { out = next; done.push(`${wrong} -> ${right}`); }
      }
      // `import android.net.*;` + a bare HttpResponseCache.install(...) call
      if (hadWildcard) {
        const masked = doctorMaskNonCode(out);
        const usesBare = new RegExp('(?<![\\w.])' + simple + '(?!\\w)').test(masked);
        const hasRight = new RegExp('^import\\s+' + right.replace(/\./g, '\\.') + '\\b', 'm').test(out) ||
          new RegExp('^import\\s+' + rightPkg.replace(/\./g, '\\.') + '\\.\\*', 'm').test(out);
        if (usesBare && !hasRight) { out = doctorAddImport(out, right, isKotlin); done.push(`added import ${right}`); }
      }
    }
    if (out === c) continue;
    fs.writeFileSync(abs, out, 'utf8');
    fixes.push(`Fixed wrong package for a framework class in ${path.relative(projectRoot, abs)} (${done.join('; ')}) -- the class does not exist in the old package, so javac/kotlinc reported "cannot find symbol"`);
  }
  return fixes;
}

function doctorParseLeadingAnnotations(line) {
  const indent = (line.match(/^\s*/) || [''])[0];
  let i = indent.length;
  const anns = [];
  while (line[i] === '@') {
    const m = /^@(?:(file|field|get|set|param|property|receiver|setparam|delegate):)?([A-Za-z_][\w.]*)/.exec(line.slice(i));
    if (!m) break;
    let j = i + m[0].length;
    let args = null;
    if (line[j] === '(') {
      let depth = 0, k = j;
      for (; k < line.length; k++) {
        const ch = line[k];
        if (ch === '"') { k++; while (k < line.length && line[k] !== '"') { if (line[k] === '\\') k++; k++; } continue; }
        if (ch === '(') depth++;
        else if (ch === ')') { depth--; if (depth === 0) break; }
      }
      if (depth !== 0) return null; // arguments run onto the next line: not handled
      args = line.slice(j + 1, k);
      j = k + 1;
    }
    anns.push({ text: line.slice(i, j), target: m[1] || '', name: m[2].slice(m[2].lastIndexOf('.') + 1), args });
    i = j;
    while (line[i] === ' ' || line[i] === '\t') i++;
  }
  return { indent, anns, rest: line.slice(i) };
}

function doctorSplitTopLevelArgs(s) {
  const out = [];
  let depth = 0, cur = '';
  for (let i = 0; i < s.length; i++) {
    const ch = s[i];
    if (ch === '"') { cur += ch; i++; while (i < s.length && s[i] !== '"') { if (s[i] === '\\') { cur += s[i]; i++; } cur += s[i]; i++; } cur += '"'; continue; }
    if (ch === '(' || ch === '[' || ch === '{') depth++;
    if (ch === ')' || ch === ']' || ch === '}') depth--;
    if (ch === ',' && depth === 0) { out.push(cur.trim()); cur = ''; continue; }
    cur += ch;
  }
  if (cur.trim()) out.push(cur.trim());
  return out.filter(Boolean);
}

function fixNonRepeatableAnnotationFromLog(log, projectRoot) {
  const fixes = [];
  const errs = doctorKtLogErrors(log).filter(e => /This annotation is not repeatable/i.test(e.msg));
  if (!errs.length) return fixes;
  const allKt = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const byFile = new Map();
  for (const e of errs) {
    const abs = doctorResolveLogFile(e.file, allKt);
    if (!abs) { fixes.push(`Build Doctor warnings: ${path.basename(e.file)}:${e.line} has a non-repeatable annotation but the file could not be located in the project.`); continue; }
    if (!byFile.has(abs)) byFile.set(abs, new Set());
    byFile.get(abs).add(e.line);
  }
  for (const [abs, lineSet] of byFile) {
    const rel = path.relative(projectRoot, abs);
    const raw = fs.readFileSync(abs, 'utf8');
    const arr = raw.split('\n');
    const cr = arr.map(l => l.endsWith('\r') ? '\r' : '');
    const lines = arr.map(l => l.replace(/\r$/, ''));
    let changed = false;
    const doneLines = new Set();
    for (const lineNo of [...lineSet].sort((a, b) => a - b)) {
      const idx = lineNo - 1;
      if (doneLines.has(idx)) continue;
      const p0 = lines[idx] === undefined ? null : doctorParseLeadingAnnotations(lines[idx]);
      if (!p0 || !p0.anns.length) { fixes.push(`Build Doctor warnings: ${rel}:${lineNo} is reported as a non-repeatable annotation but that line does not start with an annotation -- remove the duplicate by hand.`); continue; }
      // Which lines belong to the same declaration?
      let group = [];
      if (p0.anns.some(a => a.target === 'file')) {
        for (let k = 0; k < lines.length; k++) {
          const p = doctorParseLeadingAnnotations(lines[k]);
          if (p && p.anns.length && p.anns.every(a => a.target === 'file') && !p.rest.trim()) group.push(k);
        }
      } else {
        let s = idx, e = idx;
        while (s > 0) { const p = doctorParseLeadingAnnotations(lines[s - 1]); if (p && p.anns.length && !p.rest.trim()) s--; else break; }
        while (e + 1 < lines.length && doctorParseLeadingAnnotations(lines[e]).rest.trim() === '') {
          const p = doctorParseLeadingAnnotations(lines[e + 1]);
          if (p && p.anns.length) e++; else break;
        }
        for (let k = s; k <= e; k++) group.push(k);
      }
      const items = [];
      for (const k of group) {
        const p = doctorParseLeadingAnnotations(lines[k]);
        if (!p) continue;
        p.anns.forEach((a, n) => items.push({ line: k, n, ann: a, drop: false, text: a.text }));
        doneLines.add(k);
      }
      const byKey = new Map();
      for (const it of items) {
        const key = it.ann.target + ':' + it.ann.name;
        if (!byKey.has(key)) byKey.set(key, []);
        byKey.get(key).push(it);
      }
      const notes = [];
      for (const [key, list] of byKey) {
        if (list.length < 2) continue;
        const name = list[0].ann.name;
        const tgt = list[0].ann.target ? list[0].ann.target + ':' : '';
        if ((name === 'OptIn' || name === 'Suppress') && list.every(it => it.ann.args !== null)) {
          const merged = [];
          for (const it of list) for (const a of doctorSplitTopLevelArgs(it.ann.args)) if (!merged.includes(a)) merged.push(a);
          list[0].text = `@${tgt}${name}(${merged.join(', ')})`;
          for (const it of list.slice(1)) it.drop = true;
          notes.push(`merged ${list.length} @${tgt}${name} annotations into one`);
        } else if (list.every(it => it.text.replace(/\s+/g, '') === list[0].text.replace(/\s+/g, ''))) {
          for (const it of list.slice(1)) it.drop = true;
          notes.push(`removed ${list.length - 1} duplicate ${list[0].text.slice(0, 60)}`);
        } else {
          fixes.push(`Build Doctor warnings: ${rel} has @${tgt}${name} more than once with different arguments (${list.map(it => `line ${it.line + 1}: ${it.text.slice(0, 60)}`).join(' | ')}) -- keep one by hand; not edited automatically because merging them could change meaning.`);
        }
      }
      if (!notes.length) continue;
      for (const k of group) {
        const p = doctorParseLeadingAnnotations(lines[k]);
        if (!p) continue;
        const mine = items.filter(it => it.line === k);
        if (!mine.some(it => it.drop || it.text !== it.ann.text)) continue;
        const kept = mine.filter(it => !it.drop).map(it => it.text);
        const rest = p.rest.trim() ? p.rest : '';
        lines[k] = kept.length ? p.indent + kept.join(' ') + (rest ? ' ' + rest : '') : (rest ? p.indent + rest : '');
        changed = true;
      }
      fixes.push(`Fixed non-repeatable annotation in ${rel}:${lineNo} (${notes.join('; ')}) -- compiler: "This annotation is not repeatable"`);
    }
    if (changed) fs.writeFileSync(abs, lines.map((l, k) => l + cr[k]).join('\n'), 'utf8');
  }
  return fixes;
}

function fixConflictingImportFromLog(log, projectRoot) {
  const fixes = [];
  const errs = doctorKtLogErrors(log).filter(e => /Conflicting import, imported name '/.test(e.msg));
  if (!errs.length) return fixes;
  const allKt = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const byFile = new Map();
  for (const e of errs) {
    const abs = doctorResolveLogFile(e.file, allKt);
    const nm = (e.msg.match(/imported name '([^']+)'/) || [])[1];
    if (!abs || !nm) continue;
    if (!byFile.has(abs)) byFile.set(abs, new Set());
    byFile.get(abs).add(nm);
  }
  for (const [abs, names] of byFile) {
    const rel = path.relative(projectRoot, abs);
    const raw = fs.readFileSync(abs, 'utf8');
    const lines = raw.split('\n');
    const IMPORT_RE = /^\s*import\s+([\w.`]+?)(?:\s+as\s+(\w+))?\s*;?\s*(?:\/\/.*)?\r?$/;
    let changed = false;
    for (const nm of names) {
      const entries = [];
      lines.forEach((l, i) => {
        const m = IMPORT_RE.exec(l);
        if (!m || m[1].endsWith('.*')) return;
        const key = m[2] || m[1].slice(m[1].lastIndexOf('.') + 1);
        if (key === nm) entries.push({ i, fq: m[1].replace(/`/g, '') });
      });
      if (entries.length < 2) continue;
      const distinct = [...new Set(entries.map(x => x.fq))];
      if (distinct.length === 1) {
        for (const x of entries.slice(1)) { lines[x.i] = lines[x.i].endsWith('\r') ? '\r' : ''; }
        changed = true;
        fixes.push(`Removed ${entries.length - 1} duplicate "import ${distinct[0]}" from ${rel} (lines ${entries.slice(1).map(x => x.i + 1).join(', ')}) -- compiler: "Conflicting import, imported name '${nm}' is ambiguous"`);
      } else {
        fixes.push(`Build Doctor warnings: ${rel} imports '${nm}' from different packages (${entries.map(x => `line ${x.i + 1}: ${x.fq}`).join(' | ')}) -- keep one by hand (or give one an alias: import <fq> as Other).`);
      }
    }
    if (changed) fs.writeFileSync(abs, lines.join('\n'), 'utf8');
  }
  return fixes;
}

function doctorFindReceiverStart(line, endExcl) {
  let i = endExcl - 1;
  let start = -1;
  while (i >= 0) {
    while (i >= 1 && line[i] === '!' && line[i - 1] === '!') i -= 2;
    if (i >= 0 && (line[i] === ')' || line[i] === ']')) {
      const close = line[i], open = close === ')' ? '(' : '[';
      let depth = 0, k = i;
      for (; k >= 0; k--) { if (line[k] === close) depth++; else if (line[k] === open) { depth--; if (depth === 0) break; } }
      if (k < 0) return -1;
      start = k; i = k - 1;
      const identBefore = i >= 0 && /[\w$]/.test(line[i]);
      if (!identBefore) return open === '(' ? start : -1; // "(x as T)" group itself, or an index with no base
    }
    if (i >= 0 && /[\w$]/.test(line[i])) {
      while (i >= 0 && /[\w$]/.test(line[i])) i--;
      start = i + 1;
    } else return start;
    if (i >= 0 && line[i] === '.') { i--; if (i >= 0 && line[i] === '?') i--; continue; }
    return start;
  }
  return start;
}

function fixProcessMembersOnWrongReceiverFromLog(log, projectRoot) {
  const fixes = [];
  const MEMBERS = new Set(['inputStream', 'outputStream', 'errorStream', 'waitFor', 'exitValue', 'destroy', 'destroyForcibly', 'isAlive', 'pid']);
  const STRONG = new Set(['waitFor', 'exitValue', 'destroyForcibly']);
  const hits = new Map(); // "file|line|col" -> hit
  for (const e of doctorKtLogErrors(log)) {
    let name = null, m;
    if ((m = e.msg.match(/^Function invocation '(\w+)\(\.\.\.\)' expected/))) name = m[1];
    else if ((m = e.msg.match(/^Unresolved reference:\s*'?(\w+)'?/))) name = m[1];
    if (name && MEMBERS.has(name)) hits.set(`${e.file}|${e.line}|${e.col}`, { file: e.file, line: e.line, col: e.col, name });
  }
  if (!hits.size) return fixes;
  const byLine = new Map(); // "file|line" -> hits[]
  for (const h of hits.values()) {
    const k = `${h.file}|${h.line}`;
    if (!byLine.has(k)) byLine.set(k, []);
    byLine.get(k).push(h);
  }
  const allKt = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
  const edits = new Map(); // abs -> Map(lineIdx -> hits[])
  for (const list of byLine.values()) {
    const names = new Set(list.map(h => h.name));
    if (!(names.size >= 2 || [...names].some(n => STRONG.has(n)))) continue;
    const abs = doctorResolveLogFile(list[0].file, allKt);
    if (!abs) continue;
    if (!edits.has(abs)) edits.set(abs, new Map());
    edits.get(abs).set(list[0].line - 1, list);
  }
  for (const [abs, perLine] of edits) {
    const rel = path.relative(projectRoot, abs);
    const raw = fs.readFileSync(abs, 'utf8');
    const lines = raw.split('\n');
    let changed = false;
    for (const [idx, list] of perLine) {
      let line = lines[idx];
      if (line === undefined) continue;
      const before = line;
      const casted = [];
      for (const h of [...list].sort((a, b) => b.col - a.col)) { // right to left: earlier columns stay valid
        const ns = h.col - 1;
        if (line.slice(ns, ns + h.name.length) !== h.name) { fixes.push(`Build Doctor warnings: ${rel}:${h.line}:${h.col} '${h.name}' was reported on a receiver of the wrong type, but the source line doesn't line up with the log -- cast the receiver to java.lang.Process by hand.`); continue; }
        const safe = line[ns - 2] === '?' && line[ns - 1] === '.';
        const dot = safe ? ns - 2 : ns - 1;
        if (line[dot] !== '.' && !(safe && line[dot] === '?')) { fixes.push(`Build Doctor warnings: ${rel}:${h.line}:${h.col} '${h.name}' starts a continued line, so its receiver is on an earlier line -- cast it to java.lang.Process by hand.`); continue; }
        const recvEnd = safe ? ns - 2 : ns - 1;
        const recvStart = doctorFindReceiverStart(line, recvEnd);
        if (recvStart < 0 || recvStart >= recvEnd) { fixes.push(`Build Doctor warnings: ${rel}:${h.line}:${h.col} could not work out the receiver of '${h.name}' safely -- cast it to java.lang.Process by hand.`); continue; }
        const recv = line.slice(recvStart, recvEnd);
        if (/\bas\??\s+(?:java\.lang\.)?Process\s*\)?$/.test(recv)) continue; // already cast
        line = line.slice(0, recvStart) + `(${recv} as${safe ? '?' : ''} java.lang.Process)` + line.slice(recvEnd);
        casted.push(h.name);
      }
      if (line !== before) {
        lines[idx] = line; changed = true;
        fixes.push(`Cast the receiver to java.lang.Process on ${rel}:${idx + 1} (compiler: ${[...new Set(casted)].join(', ')} don't exist on the receiver's type -- it is typed Any/unknown, e.g. a reflective newProcess result). If that value can be something other than a Process, declare its real type instead.`);
      }
    }
    if (changed) fs.writeFileSync(abs, lines.join('\n'), 'utf8');
  }
  return fixes;
}

const DOCTOR_EXPERIMENTAL_KOTLIN_FEATURES = {
  'unit conversions on arbitrary expressions': 'UnitConversionsOnArbitraryExpressions'
};

function fixExperimentalKotlinFeatureFromLog(log, projectRoot) {
  const fixes = [];
  const wanted = new Set();
  const unknown = new Set();
  for (const m of String(log || '').matchAll(/The feature "([^"]+)" is experimental and should be enabled explicitly/g)) {
    const flag = DOCTOR_EXPERIMENTAL_KOTLIN_FEATURES[m[1].toLowerCase()];
    if (flag) wanted.add(flag); else unknown.add(m[1]);
  }
  if (unknown.size) fixes.push(`Build Doctor warnings: Kotlin says the feature "${[...unknown].join('", "')}" must be enabled explicitly, but the Doctor doesn't know its compiler flag -- add -XXLanguage:+<FeatureName> to freeCompilerArgs by hand.`);
  if (!wanted.size) return fixes;
  const files = listProjectFiles(projectRoot, (full, name) => /^build\.gradle(\.kts)?$/.test(name));
  const hadBefore = new Set(); // flags a build file already carried before this run
  const addedNow = new Set();
  for (const abs of files) {
    let c = fs.readFileSync(abs, 'utf8');
    if (!/\bandroid\s*\{/.test(c) || !/kotlin/i.test(c)) continue; // only Kotlin Android modules
    const isKts = /\.kts$/.test(abs);
    const added = [];
    for (const flag of wanted) {
      if (c.includes(flag)) { hadBefore.add(flag); continue; }
      const arg = `-XXLanguage:+${flag}`;
      const line = isKts ? `freeCompilerArgs += listOf("${arg}")` : `freeCompilerArgs += ['${arg}']`;
      if (/kotlinOptions\s*\{/.test(c)) c = c.replace(/(kotlinOptions\s*\{)/, (m0) => `${m0}\n        ${line}`);
      else c = c.replace(/(\bandroid\s*\{)/, (m0) => `${m0}\n    kotlinOptions {\n        ${line}\n    }`);
      added.push(flag);
      addedNow.add(flag);
    }
    if (!added.length) continue;
    fs.writeFileSync(abs, c, 'utf8');
    fixes.push(`Enabled Kotlin language feature ${added.join(', ')} in ${path.relative(projectRoot, abs)} (compiler: the feature is experimental and should be enabled explicitly)`);
  }
  // The switch was already on in an earlier attempt and the compiler STILL rejects the code:
  // this Kotlin version ignores it, so the expression itself has to change. Don't guess
  // blindly -- put the real source line in the warning.
  const stuck = [...hadBefore].filter(f => !addedNow.has(f));
  if (stuck.length) {
    const allKt = listProjectFiles(projectRoot, (full, name) => /\.kt$/.test(name));
    const seen = new Set();
    for (const e of doctorKtLogErrors(log)) {
      const fm = e.msg.match(/^The feature "([^"]+)" is experimental/);
      if (!fm || !stuck.includes(DOCTOR_EXPERIMENTAL_KOTLIN_FEATURES[fm[1].toLowerCase()])) continue;
      const abs = doctorResolveLogFile(e.file, allKt);
      if (!abs || seen.has(abs + ':' + e.line)) continue;
      seen.add(abs + ':' + e.line);
      const srcLine = (fs.readFileSync(abs, 'utf8').split('\n')[e.line - 1] || '').trim().slice(0, 240);
      fixes.push(`Build Doctor warnings: ${path.relative(projectRoot, abs)}:${e.line} -- Kotlin still rejects "${fm[1]}" even though the -XXLanguage switch is already set, so this compiler ignores it. The function-typed value(s) at column ${[...new Set(doctorKtLogErrors(log).filter(x => x.line === e.line && /^The feature/.test(x.msg)).map(x => x.col))].join(' and ')} must be wrapped in a lambda, e.g. { value() }. Source line: ${srcLine}`);
    }
  }
  return fixes;
}

const DOCTOR_SHIZUKU_COMPAT_KT = `package tycdoctor

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Added by the build doctor. Shizuku API 13.1+ made Shizuku.newProcess private, so a
 * direct call no longer compiles ("it is private in 'rikka/shizuku/Shizuku'"). The
 * method is still in the library; this makes the same call through reflection.
 */
object ShizukuCompat {
    private val method: Method by lazy {
        val m = Class.forName("rikka.shizuku.Shizuku").getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        m.isAccessible = true
        m
    }

    @JvmStatic
    fun newProcess(cmd: Array<String>, env: Array<String>?, dir: String?): java.lang.Process {
        try {
            return method.invoke(null, cmd, env, dir) as java.lang.Process
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }
    }
}
`;

const DOCTOR_SHIZUKU_COMPAT_JAVA = `package tycdoctor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Added by the build doctor. Shizuku API 13.1+ made Shizuku.newProcess private, so a
 * direct call no longer compiles ("has private access in Shizuku"). The method is still
 * in the library; this makes the same call through reflection.
 */
public final class ShizukuCompat {
    private static Method method;

    private ShizukuCompat() {}

    private static synchronized Method method() throws Exception {
        if (method == null) {
            Method m = Class.forName("rikka.shizuku.Shizuku").getDeclaredMethod(
                    "newProcess", String[].class, String[].class, String.class);
            m.setAccessible(true);
            method = m;
        }
        return method;
    }

    public static Process newProcess(String[] cmd, String[] env, String dir) {
        try {
            return (Process) method().invoke(null, new Object[] { cmd, env, dir });
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause() != null ? e.getCause() : e;
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            throw new RuntimeException(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
`;

function fixPrivateShizukuNewProcessFromLog(log, projectRoot) {
  const fixes = [];
  const text = String(log || '');
  const named = new Set();
  for (const m of text.matchAll(/(\S+\.kt)[:\s]\(?\d+[:,]\s*\d+\)?:?\s*Cannot access '[^\n]*?\bnewProcess\([^\n]*?it is private in '(?:rikka[/.]shizuku[/.])?Shizuku'/g)) named.add(m[1]);
  for (const m of text.matchAll(/(\S+\.java):\d+: error: newProcess\([^\n]*?\) has private access in (?:rikka\.shizuku\.)?Shizuku/g)) named.add(m[1]);
  if (!named.size) return fixes;
  const src = listProjectFiles(projectRoot, (full, name) => /\.(kt|java)$/.test(name));
  const CALL = /(?<![\w.])(?:rikka\.shizuku\.)?Shizuku\s*\.\s*newProcess\s*\(/g;
  let wroteHelperFor = null; // source root the helper was written under
  let anyKotlin = false;
  const touched = [];
  for (const logPath of named) {
    const abs = doctorResolveLogFile(logPath, src);
    if (!abs) { fixes.push(`Build Doctor warnings: the log names ${path.basename(logPath)} for the private Shizuku.newProcess error but the file could not be located in the project.`); continue; }
    const c = fs.readFileSync(abs, 'utf8');
    const out = c.replace(CALL, 'tycdoctor.ShizukuCompat.newProcess(');
    if (out === c) continue;
    fs.writeFileSync(abs, out, 'utf8');
    touched.push({ abs, n: (c.match(CALL) || []).length });
    if (/\.kt$/.test(abs)) anyKotlin = true;
  }
  if (!touched.length) return fixes;
  // The helper goes next to the code, under the same src/<set>/java|kotlin root.
  const first = touched[0].abs;
  const rootM = first.match(/^(.*?[\\/]src[\\/][^\\/]+[\\/](?:java|kotlin))[\\/]/);
  const srcRoot = rootM ? rootM[1] : path.dirname(first);
  const helperDir = path.join(srcRoot, 'tycdoctor');
  const helperFile = path.join(helperDir, anyKotlin ? 'ShizukuCompat.kt' : 'ShizukuCompat.java');
  if (!fs.existsSync(path.join(helperDir, 'ShizukuCompat.kt')) && !fs.existsSync(path.join(helperDir, 'ShizukuCompat.java'))) {
    fs.mkdirSync(helperDir, { recursive: true });
    fs.writeFileSync(helperFile, anyKotlin ? DOCTOR_SHIZUKU_COMPAT_KT : DOCTOR_SHIZUKU_COMPAT_JAVA, 'utf8');
    wroteHelperFor = helperFile;
  }
  // A shrunk (R8/ProGuard) release build would strip a private method nobody calls directly.
  const moduleDir = rootM ? path.resolve(srcRoot, '..', '..', '..') : null;
  if (moduleDir) {
    const rules = path.join(moduleDir, 'proguard-rules.pro');
    const keep = '-keep class rikka.shizuku.Shizuku { *; }';
    try {
      const cur = fs.existsSync(rules) ? fs.readFileSync(rules, 'utf8') : '';
      if (!cur.includes(keep)) fs.writeFileSync(rules, (cur && !cur.endsWith('\n') ? cur + '\n' : cur) + '# Added by the build doctor: Shizuku.newProcess is called through reflection\n' + keep + '\n', 'utf8');
    } catch { /* not fatal: only matters for minified release builds */ }
  }
  fixes.push(`Replaced ${touched.reduce((a, t) => a + t.n, 0)} direct Shizuku.newProcess call(s) in ${touched.map(t => path.relative(projectRoot, t.abs)).join(', ')} with tycdoctor.ShizukuCompat.newProcess` +
    (wroteHelperFor ? ` and added ${path.relative(projectRoot, wroteHelperFor)}` : '') +
    ` -- Shizuku 13.1+ made newProcess private ("it is private in 'rikka/shizuku/Shizuku'"), the helper calls the same method through reflection`);
  return fixes;
}

function fixHumanAndAiMistakes(log, projectRoot) {
  const fixes = [];
  const steps = [
    () => fixSplitNonAsciiIdentifiers(projectRoot),
    () => fixMovedFrameworkClasses(projectRoot),
    () => fixNonRepeatableAnnotationFromLog(log, projectRoot),
    () => fixConflictingImportFromLog(log, projectRoot),
    () => fixProcessMembersOnWrongReceiverFromLog(log, projectRoot),
    () => fixExperimentalKotlinFeatureFromLog(log, projectRoot),
    () => fixPrivateShizukuNewProcessFromLog(log, projectRoot),
    () => fixTranslatedDeclarationMismatchFromLog(log, projectRoot),
    () => fixStringResourceMistakes(projectRoot),
    () => fixInvalidResourceFileNames(projectRoot),
    () => fixMissingXmlNamespaces(projectRoot),
    () => fixLegacySupportLibrary(projectRoot),
    () => fixDeprecatedGradleSyntax(log, projectRoot),
    () => fixTextViewEditTextOnlyMethods(log, projectRoot),
    () => fixKotlinReturnAnyToStringFromLog(log, projectRoot),
    () => fixComposeColorImportFromLog(log, projectRoot),
    () => fixCanvasDrawRoundRectFromLog(log, projectRoot),
    () => fixExperimentalComposeApiFromLog(log, projectRoot),
    () => fixComposeDelegateImportsFromLog(log, projectRoot),
    () => fixReturnInExpressionBodyFromLog(log, projectRoot),
    () => fixFinalMemberOverrideFromLog(log, projectRoot),
    () => fixKotlinSyntaxErrorFromLog(log, projectRoot),
    () => fixMisplacedPackagingBlockFromLog(log, projectRoot),
    () => fixMissingImportsFromLog(log, projectRoot),
    () => fixMovedOkHttpFactoriesFromLog(log, projectRoot),
    () => fixUnknownGradlePropertyFromLog(log, projectRoot),
    () => fixMissingCommonPermissions(projectRoot)
  ];
  // One broken rule must never take down the whole doctor pass.
  for (const step of steps) {
    try { fixes.push(...step()); } catch (e) { fixes.push(`Build Doctor warnings: a repair rule threw and was skipped (${String(e && e.message || e).split('\n')[0]})`); }
  }
  return fixes;
}

const KNOWN_BAD_DEPENDENCY_COORDINATES = {
  // Real coordinates are dev.rikka.shizuku:api and dev.rikka.shizuku:provider.
  // com.ryan.shang:shizuku-api / com.ryan.shang:shizuku-provider don't exist
  // under any groupId on Maven Central, yet paired real, currently-published
  // version numbers -- exactly the shape this whole fixer exists to catch.
  'com.ryan.shang:shizuku-api': 'dev.rikka.shizuku:api',
  'com.ryan.shang:shizuku-provider': 'dev.rikka.shizuku:provider'
};

function buildDependencyCoordinateRegex(groupId, artifactId) {
  const g = groupId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const a = artifactId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`(["'])${g}:${a}:([^"'\\s]+)\\1`, 'g');
}

async function lookupMavenCentralGroupIds(artifactId, timeoutMs = 8000) {
  const url = `https://search.maven.org/solrsearch/select?q=a:%22${encodeURIComponent(artifactId)}%22&rows=20&wt=json`;
  const res = await fetchWithTimeout(url, {}, timeoutMs);
  if (!res.ok) throw new Error(`search.maven.org returned ${res.status}`);
  const data = await res.json();
  const docs = data?.response?.docs || [];
  // Only count groupIds that actually published this exact artifactId
  // (Solr's "a:" search can loosely match) -- and only unique ones.
  return [...new Set(docs.filter(d => d.a === artifactId).map(d => d.g))];
}

async function fixUnresolvableDependencyCoordinates(log, projectRoot) {
  const fixes = [];
  if (!log || !/Could not find /.test(log)) return fixes;

  const gradleFiles = listProjectFiles(projectRoot, (full, name) =>
    name === 'build.gradle' || name === 'build.gradle.kts'
  );
  if (!gradleFiles.length) return fixes;

  // "Could not find com.ryan.shang:shizuku-api:13.1.5." -- Gradle prints
  // this once per unresolved module, each on its own line. The version
  // group is matched as dot-joined segments (not a bare [\w.-]+) so it
  // doesn't swallow that trailing sentence period into the version string.
  const missing = [...new Set(
    [...log.matchAll(/Could not find ([\w.\-]+):([\w.\-]+):([\w-]+(?:\.[\w-]+)*)/g)]
      .map(([, g, a, v]) => `${g}:${a}:${v}`)
  )].slice(0, 8); // bound the work (and the API calls) per repair pass
  if (!missing.length) return fixes;

  const unresolvedNotes = [];

  for (const coordinate of missing) {
    const [groupId, artifactId, version] = coordinate.split(':');
    const knownGood = KNOWN_BAD_DEPENDENCY_COORDINATES[`${groupId}:${artifactId}`];
    let correctGroupId, correctArtifactId, source;

    if (knownGood) {
      // A confirmed prior case -- may correct the artifactId as well as the
      // groupId (the shizuku case needed both), so trust it outright rather
      // than running it back through the single-groupId evidence bar below.
      [correctGroupId, correctArtifactId] = knownGood.split(':');
      source = 'known-bad-coordinate list';
    } else {
      // The network fallback only ever hypothesizes a wrong groupId with
      // the artifactId unchanged -- it has no way to know the artifactId
      // itself was also mangled, so it can only search Maven Central for
      // exactly the artifactId the log already named.
      let candidates;
      try {
        candidates = await lookupMavenCentralGroupIds(artifactId);
      } catch (err) {
        unresolvedNotes.push(`${coordinate} (Maven Central lookup failed: ${err.message})`);
        continue;
      }
      // Evidence bar: exactly one other groupId publishes this artifactId,
      // and it isn't the one already failing. Two or more candidates means
      // the artifactId is too generic to guess from safely (e.g. a name
      // like "core" is published by dozens of unrelated groups) -- Build
      // Doctor surfaces those as a warning instead of picking one.
      const realCandidates = candidates.filter(g => g !== groupId);
      if (realCandidates.length !== 1) {
        unresolvedNotes.push(
          realCandidates.length === 0
            ? `${coordinate} (no other groupId on Maven Central publishes "${artifactId}" -- likely a genuinely missing/renamed/removed dependency, not just a wrong groupId)`
            : `${coordinate} (ambiguous -- "${artifactId}" is also published by ${realCandidates.join(', ')}; picking one automatically would be a guess)`
        );
        continue;
      }
      correctGroupId = realCandidates[0];
      correctArtifactId = artifactId;
      source = 'Maven Central';
    }

    let rewrote = false;
    for (const file of gradleFiles) {
      let content;
      try { content = fs.readFileSync(file, 'utf8'); } catch { continue; }
      const regex = buildDependencyCoordinateRegex(groupId, artifactId);
      if (!regex.test(content)) continue;
      const updated = content.replace(regex, (whole, quote, ver) => `${quote}${correctGroupId}:${correctArtifactId}:${ver}${quote}`);
      if (updated !== content) {
        fs.writeFileSync(file, updated, 'utf8');
        rewrote = true;
      }
    }
    if (rewrote) {
      fixes.push(
        `Corrected dependency coordinate: "${groupId}:${artifactId}:${version}" doesn't exist, but ${source} confirms ` +
        `"${correctGroupId}:${correctArtifactId}:${version}" does -- rewrote it in the project's build.gradle(.kts).`
      );
    } else {
      // The log named this coordinate but it isn't in the string form the
      // regex above rewrites (map-style dependency, version catalog, or a
      // generated/included build file this scan didn't reach) -- don't
      // claim a fix that wasn't actually applied.
      unresolvedNotes.push(`${coordinate} (found "${correctGroupId}:${correctArtifactId}" as the likely correct coordinate on ${source}, but couldn't locate the dependency in a rewritable string-coordinate form -- check for a version-catalog or map-style declaration)`);
    }
  }

  if (unresolvedNotes.length) {
    fixes.push(`Build Doctor warnings: unresolved dependency coordinate(s) needing a human look: ${unresolvedNotes.join(' | ')}`);
  }

  return fixes;
}

function fixMissingResourceStub(log, projectRoot) {
  const fixes = [];
  if (!log) return fixes;
  const appModule = findAppModule(projectRoot);
  if (!appModule) return fixes;
  const resDir = appModule.resDir;

  const missing = [...log.matchAll(
    /resource (drawable|mipmap|style)\/([\w.]+)\s*\(aka [\w.]+:(?:drawable|mipmap|style)\/[\w.]+\)\s*not found/gi
  )];
  if (!missing.length) return fixes;

  for (const [, type, name] of missing) {
    if (type === 'style') {
      const stylesPath = path.join(resDir, 'values', 'styles.xml');
      fs.mkdirSync(path.dirname(stylesPath), { recursive: true });
      let content = fs.existsSync(stylesPath)
        ? fs.readFileSync(stylesPath, 'utf8')
        : '<resources>\n</resources>\n';
      if (content.includes(`name="${name}"`)) continue;
      content = content.replace(
        '</resources>',
        `    <!-- AUTO-REPAIR: placeholder -- "${name}" was referenced but never defined. Replace with your real theme. -->\n` +
        `    <style name="${name}" parent="Theme.MaterialComponents.Light.NoActionBar"/>\n` +
        `</resources>`
      );
      fs.writeFileSync(stylesPath, content, 'utf8');
      fixes.push(`Created placeholder style "${name}" in ${path.relative(projectRoot, stylesPath)} (was referenced in the manifest/build but never defined -- replace with your real theme)`);
    } else {
      const dir = path.join(resDir, type);
      fs.mkdirSync(dir, { recursive: true });
      const file = path.join(dir, `${name}.xml`);
      if (fs.existsSync(file)) continue;
      const isLauncher = /ic_launcher/i.test(name);
      const xml = isLauncher
        ? `<?xml version="1.0" encoding="utf-8"?>\n` +
          `<!-- AUTO-REPAIR: placeholder launcher icon -- replace with a real icon. -->\n` +
          `<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n` +
          `    <background android:drawable="@android:color/white"/>\n` +
          `    <foreground android:drawable="@android:color/darker_gray"/>\n` +
          `</adaptive-icon>\n`
        : `<?xml version="1.0" encoding="utf-8"?>\n` +
          `<!-- AUTO-REPAIR: placeholder -- "${name}" was referenced but never provided. -->\n` +
          `<shape xmlns:android="http://schemas.android.com/apk/res/android">\n` +
          `    <solid android:color="#CCCCCC"/>\n` +
          `</shape>\n`;
      fs.writeFileSync(file, xml, 'utf8');
      fixes.push(`Created placeholder ${type} "${name}" in ${path.relative(projectRoot, file)} (referenced but missing from the upload -- replace with a real asset)`);
    }
  }
  return fixes;
}

function fixNeverThrownCatch(log, projectRoot) {
  const fixes = [];
  if (!log || !/exception [\w.]+ is never thrown in body of corresponding try statement/i.test(log)) return fixes;

  // de-duplicate (file, line, exception)
  const seen = new Set();
  const found = [];
  for (const m of String(log).matchAll(/(\S+\.java):(\d+):\s*error:\s*exception ([\w.]+) is never thrown/g)) {
    const key = `${m[1]}:${m[2]}:${m[3]}`;
    if (!seen.has(key)) { seen.add(key); found.push({ file: m[1], line: parseInt(m[2], 10), exc: m[3] }); }
  }
  if (!found.length) return fixes;
  const allJava = listProjectFiles(projectRoot, (full, name) => /\.java$/.test(name));
  const byFile = new Map();
  for (const f of found) {
    const abs = doctorResolveLogFile(f.file, allJava);
    if (!abs) continue;
    if (!byFile.has(abs)) byFile.set(abs, []);
    byFile.get(abs).push(f);
  }
  const matchClose = (code, open) => {
    let d = 0;
    for (let i = open; i < code.length; i++) {
      if (code[i] === '{') d++;
      else if (code[i] === '}') { d--; if (d === 0) return i; }
    }
    return -1;
  };
  const matchOpen = (code, close) => {
    let d = 0;
    for (let i = close; i >= 0; i--) {
      if (code[i] === '}') d++;
      else if (code[i] === '{') { d--; if (d === 0) return i; }
    }
    return -1;
  };
  for (const [abs, list] of byFile) {
    const rel = path.relative(projectRoot, abs);
    let c = fs.readFileSync(abs, 'utf8');
    let changed = false;
    list.sort((a, b) => b.line - a.line); // bottom-to-top keeps earlier line numbers valid
    for (const f of list) {
      const code = doctorMaskNonCode(c);
      const starts = doctorLineStarts(c);
      const li = f.line - 1;
      if (li < 0 || li >= starts.length) continue;
      const lineEnd = li + 1 < starts.length ? starts[li + 1] - 1 : c.length;
      const simple = f.exc.split('.').pop();
      const spot = `${rel}:${f.line}`;
      // the catch keyword on (or, for a header split over lines, just after) the reported line
      const cm = /\bcatch\s*\(\s*(?:final\s+)?([^)]*?)\s+([\p{L}\p{N}_$]+)\s*\)\s*\{/u.exec(code.slice(starts[li], Math.min(c.length, lineEnd + 200)));
      if (!cm || !(c.slice(starts[li] + cm.index, starts[li] + cm.index + cm[0].length)).includes(simple)) {
        fixes.push(`Build Doctor warnings: ${spot} has an unreachable catch(${simple}) but its header isn't in a shape the doctor can edit safely -- delete that catch block by hand.`);
        continue;
      }
      const catchAt = starts[li] + cm.index;
      const headerEnd = catchAt + cm[0].length;
      const openBrace = headerEnd - 1;
      const closeBrace = matchClose(code, openBrace);
      if (closeBrace < 0) { fixes.push(`Build Doctor warnings: ${spot} unreachable catch(${simple}) -- could not find its closing brace, left alone.`); continue; }

      // multi-catch: catch (A | X e)  -> just drop X from the alternatives
      const typesText = c.slice(catchAt, headerEnd).match(/\(\s*(?:final\s+)?([^)]*?)\s+[\p{L}\p{N}_$]+\s*\)/u)[1];
      const alts = typesText.split('|').map(t => t.trim());
      if (alts.length > 1) {
        const keep = alts.filter(t => t.split('.').pop() !== simple);
        if (keep.length && keep.length < alts.length) {
          const hdr = c.slice(catchAt, headerEnd).replace(typesText, keep.join(' | '));
          c = c.slice(0, catchAt) + hdr + c.slice(headerEnd);
          changed = true;
          fixes.push(`Removed unreachable ${simple} from the multi-catch at ${spot} (javac proved it is never thrown there)`);
          continue;
        }
      }

      // single-type catch: remove the whole block, keeping the '}' of the block before it
      let p = catchAt - 1;
      while (p >= 0 && /\s/.test(code[p])) p--;
      if (p < 0 || code[p] !== '}') { fixes.push(`Build Doctor warnings: ${spot} unreachable catch(${simple}) isn't directly after a '}' -- left alone.`); continue; }
      const prevClose = p;
      const afterText = code.slice(closeBrace + 1, closeBrace + 60);
      const otherHandlerAfter = /^\s*(?:catch|finally)\b/.test(afterText);
      const prevOpen = matchOpen(code, prevClose);
      if (prevOpen < 0) { fixes.push(`Build Doctor warnings: ${spot} unreachable catch(${simple}) -- couldn't match the try block, left alone.`); continue; }
      const beforePrev = code.slice(Math.max(0, prevOpen - 400), prevOpen).replace(/\s+$/, '');
      const prevIsHandler = /\bfinally$/.test(beforePrev) || /\bcatch\s*\([^)]*\)$/.test(beforePrev);
      const prevIsPlainTry = /\btry$/.test(beforePrev);
      if (otherHandlerAfter || prevIsHandler) {
        c = c.slice(0, prevClose + 1) + c.slice(closeBrace + 1);
        changed = true;
        fixes.push(`Removed unreachable catch(${simple}) at ${spot} (javac proved it is never thrown; the try still has its other catch/finally)`);
      } else if (prevIsPlainTry) {
        // it was the only handler: turn `try { ... }` into a plain block so the code inside still runs exactly the same
        const realTry = code.lastIndexOf('try', prevOpen);
        c = c.slice(0, realTry) + c.slice(realTry + 3, prevClose + 1) + c.slice(closeBrace + 1);
        changed = true;
        fixes.push(`Removed unreachable catch(${simple}) at ${spot} and unwrapped its 'try' (javac proved the exception is never thrown, and it was the only handler, so the try body now runs as a plain block)`);
      } else {
        fixes.push(`Build Doctor warnings: ${spot} unreachable catch(${simple}) belongs to a try-with-resources -- remove that catch by hand.`);
      }
    }
    if (changed) fs.writeFileSync(abs, c, 'utf8');
  }
  return fixes;
}

function findBalancedBlocks(content, keywordRegex) {
  const flags = keywordRegex.flags.includes('g') ? keywordRegex.flags : keywordRegex.flags + 'g';
  const re = new RegExp(keywordRegex.source, flags);
  const blocks = [];
  let m;
  while ((m = re.exec(content))) {
    const braceStart = content.indexOf('{', m.index);
    if (braceStart === -1) continue;
    let depth = 0;
    let end = -1;
    for (let i = braceStart; i < content.length; i++) {
      if (content[i] === '{') depth++;
      else if (content[i] === '}') {
        depth--;
        if (depth === 0) {
          end = i;
          break;
        }
      }
    }
    if (end === -1) continue;
    blocks.push({ start: m.index, end: end + 1 });
    re.lastIndex = end + 1; // don't re-scan inside a block we already captured
  }
  return blocks;
}

function fixFlatAndroidAppModule(projectRoot) {
  const settingsPath = ['settings.gradle', 'settings.gradle.kts']
    .map((f) => path.join(projectRoot, f))
    .find(fs.existsSync);
  if (!settingsPath) return null;

  const settingsContent = fs.readFileSync(settingsPath, 'utf8');
  if (!/include\s*\(?\s*['"]:app['"]/.test(settingsContent)) return null;

  const appDir = path.join(projectRoot, 'app');
  if (fs.existsSync(appDir)) return null; // already a real module -- not this case

  const manifestPath = path.join(projectRoot, 'src', 'main', 'AndroidManifest.xml');
  if (!fs.existsSync(manifestPath)) return null;

  const buildGradlePath = ['build.gradle', 'build.gradle.kts']
    .map((f) => path.join(projectRoot, f))
    .find(fs.existsSync);
  if (!buildGradlePath) return null;

  let buildContent = fs.readFileSync(buildGradlePath, 'utf8');
  if (!/com\.android\.application/.test(buildContent)) return null;

  // Pull out any buildscript{...} block first -- it configures the ROOT
  // project's own classpath (the AGP version itself, resolved via legacy
  // apply-plugin inheritance) and belongs at the true top level, not inside
  // the app module that classpath is used to build.
  const buildscriptBlocks = findBalancedBlocks(buildContent, /\bbuildscript\s*(?=\{)/);
  let rootBuildContent = '';
  if (buildscriptBlocks.length) {
    const { start, end } = buildscriptBlocks[0];
    rootBuildContent = buildContent.slice(start, end) + '\n';
    buildContent = (buildContent.slice(0, start) + buildContent.slice(end)).trim() + '\n';
  }

  fs.mkdirSync(appDir, { recursive: true });

  const buildGradleName = path.basename(buildGradlePath);
  fs.writeFileSync(path.join(appDir, buildGradleName), buildContent, 'utf8');
  fs.rmSync(buildGradlePath, { force: true });
  fs.writeFileSync(path.join(projectRoot, buildGradleName), rootBuildContent, 'utf8');

  fs.renameSync(path.join(projectRoot, 'src'), path.join(appDir, 'src'));

  for (const extra of ['proguard-rules.pro', 'google-services.json']) {
    const from = path.join(projectRoot, extra);
    if (fs.existsSync(from)) fs.renameSync(from, path.join(appDir, extra));
  }

  return 'Moved flat Android module files (build.gradle, src/) into app/ -- settings.gradle already ' +
    "declares ':app' as a separate module, but its files were sitting at the project root with no " +
    '"app" subfolder, which makes \':app\' and \':\' resolve to the same directory and fails with ' +
    '"Multiple projects in this build have project directory ...: [:, :app]"';
}

function findAppModule(projectRoot) {
  const manifests = [];
  const queue = [projectRoot];
  while (queue.length) {
    const d = queue.shift();
    let entries;
    try {
      entries = fs.readdirSync(d, { withFileTypes: true });
    } catch {
      continue;
    }
    for (const e of entries) {
      if (e.isDirectory()) {
        if (IGNORED_DIR_NAMES.has(e.name)) continue;
        queue.push(path.join(d, e.name));
      } else if (e.isFile() && e.name === 'AndroidManifest.xml' && path.basename(d) === 'main' && path.basename(path.dirname(d)) === 'src') {
        manifests.push(path.join(d, e.name));
      }
    }
  }
  if (!manifests.length) return null;

  const withPlugin = manifests.filter((manifestPath) => {
    const moduleDir = path.dirname(path.dirname(path.dirname(manifestPath)));
    for (const gradleFile of ['build.gradle', 'build.gradle.kts']) {
      const p = path.join(moduleDir, gradleFile);
      if (fs.existsSync(p)) {
        const content = fs.readFileSync(p, 'utf8');
        if (/com\.android\.application/.test(content)) return true;
      }
    }
    return false;
  });

  let chosen;
  if (withPlugin.length === 1) {
    chosen = withPlugin[0];
  } else if (withPlugin.length === 0) {
    const withIcon = manifests.filter((m) => /android:icon\s*=/.test(fs.readFileSync(m, 'utf8')));
    if (withIcon.length !== 1) return null;
    chosen = withIcon[0];
  } else {
    return null; // more than one application-plugin module -- genuinely ambiguous
  }

  const moduleDir = path.dirname(path.dirname(path.dirname(chosen)));
  const manifestXml = fs.readFileSync(chosen, 'utf8');
  const iconMatch = manifestXml.match(/android:icon\s*=\s*"@mipmap\/([a-zA-Z0-9_]+)"/);
  return {
    resDir: path.join(moduleDir, 'src', 'main', 'res'),
    iconName: iconMatch ? iconMatch[1] : 'ic_launcher'
  };
}

function xrRead(file) {
  try { return fs.readFileSync(file, 'utf8'); } catch { return null; }
}

function xrParseVer(v) {
  const m = /(\d+)(?:\.(\d+))?(?:\.(\d+))?/.exec(String(v || ''));
  if (!m) return null;
  return [Number(m[1]), Number(m[2] || 0), Number(m[3] || 0)];
}

function xrBuildFiles(root, { settings = false } = {}) {
  const re = settings ? /^(?:build|settings)\.gradle(?:\.kts)?$/ : /^build\.gradle(?:\.kts)?$/;
  return listProjectFiles(root, (full, name) => re.test(name));
}

function xrDetectAgp(root) {
  const files = [
    ...xrBuildFiles(root, { settings: true }),
    ...listProjectFiles(root, (full, name) => /\.versions\.toml$/.test(name))
  ];
  const res = [
    /com\.android\.tools\.build:gradle:(\d+\.\d+(?:\.\d+)?)/,
    /['"]com\.android\.(?:application|library)['"]\s*\)?\s*version\s*\(?\s*['"](\d+\.\d+(?:\.\d+)?)/,
    /^\s*(?:agp|androidGradlePlugin|android-gradle-plugin)\s*=\s*"(\d+\.\d+(?:\.\d+)?)/m
  ];
  for (const file of files) {
    const c = xrRead(file);
    if (c == null) continue;
    for (const re of res) {
      const m = re.exec(c);
      if (m) {
        const v = xrParseVer(m[1]);
        return { str: m[1], major: v[0], minor: v[1] };
      }
    }
  }
  return null;
}

// ---- command line -------------------------------------------------------------------------------
// The phone build logs come from kotlinc/javac/aapt2, the doctor's rules read Gradle-style logs. kotlinc prints
//   /path/File.kt:12:5: error: unresolved reference 'x'.      Gradle prints   e: file:///path/File.kt:12:5 Unresolved reference 'x'.
function wzhNormalizeLog(text) {
  return String(text || '').split('\n').map((line) => {
    const m = /^(\/.+?\.kt):(\d+):(\d+): (error|warning): (.*)$/.exec(line);
    if (!m) return line;
    const msg = m[5].charAt(0).toUpperCase() + m[5].slice(1);
    return `${m[4] === 'error' ? 'e' : 'w'}: file://${m[1]}:${m[2]}:${m[3]} ${msg}`;
  }).join('\n');
}

(async () => {
  const root = process.argv[2];
  const logFile = process.argv[3];
  let log = '';
  try { if (logFile) log = wzhNormalizeLog(fs.readFileSync(logFile, 'utf8')); } catch (_) { /* no log */ }
  try {
    const fixes = await runBuildDoctor(root, log);
    for (const f of fixes) console.log('WZH-DOCTOR: ' + f);
    console.log('WZH-DOCTOR-COUNT: ' + fixes.filter((f) => !/^Build Doctor warnings/.test(f)).length);
  } catch (e) {
    console.log('WZH-DOCTOR: skipped (' + String((e && e.message) || e).split('\n')[0] + ')');
    console.log('WZH-DOCTOR-COUNT: 0');
  }
})();
