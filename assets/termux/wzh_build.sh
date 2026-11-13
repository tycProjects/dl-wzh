# WZH 2 APK: on-phone Kotlin/Java build. No Gradle, no GitHub, no server.
# Runs inside Termux. The app serves the project on 127.0.0.1 and receives the log + the APK back.
# $1 = the app's local address, $2 = the app id to give the built app (optional)
B="$1"
APPID_ARG="$2"
W="$HOME/wzh_build"
mkdir -p "$W"

post() { curl -s -m 60 -X POST --data-binary "$2" "$B/$1" >/dev/null 2>&1; }
step() { post step "$1"; }
sendlog() { [ -s "$1" ] && curl -s -m 120 -X POST --data-binary @"$1" "$B/log" >/dev/null 2>&1; }
# fail() runs inside wzh_body (a subshell): it only records why. The driver at the bottom decides whether the
# Build Doctor can repair the project and try again, or reports the failure to the app.
fail() { printf '%s' "$1" > "$W/lastfail"; exit 1; }
run() {
  local name="$1"; shift
  "$@" >"$W/cmd.log" 2>&1
  local rc=$?
  sendlog "$W/cmd.log"
  [ $rc -eq 0 ] || { printf '%s' "$name" > "$W/lastrun"; fail "$name failed"; }
}

# Build Doctor: the same repair rules the server uses (wzh_doctor.js is made from the server's server.js).
# It needs node in Termux; when node can't be had the build simply goes on without the doctor.
doctor_run() {  # $1 = optional log of the failed step. Returns 0 when it changed something.
  [ -s "$W/wzh_doctor.js" ] && command -v node >/dev/null 2>&1 || return 1
  node "$W/wzh_doctor.js" "$W/p" "$1" > "$W/doctor.out" 2>&1
  grep '^WZH-DOCTOR: ' "$W/doctor.out" > "$W/doctor.notes" 2>/dev/null
  sendlog "$W/doctor.notes"
  grep -q '^WZH-DOCTOR-COUNT: [1-9]' "$W/doctor.out"
}

wzh_body() {

step "Checking the tools"
for t in java javac jar aapt2 zip unzip curl; do
  command -v "$t" >/dev/null 2>&1 || fail "$t is not installed in Termux. Open Set up on this phone and tap Install tools."
done
D8=""; DX=""
command -v d8 >/dev/null 2>&1 && D8=1
command -v dx >/dev/null 2>&1 && DX=1
[ -z "$D8$DX" ] && fail "d8 is not installed in Termux. Open Set up on this phone and tap Install tools."

step "Unpacking the project"
if [ -n "$FIRST" ]; then
rm -rf "$W/p" "$W/o"
mkdir -p "$W/p" "$W/o/classes" "$W/o/gen" "$W/o/dex"
curl -sf -m 300 -o "$W/project.zip" "$B/project.zip" || fail "could not get the project from the app"
unzip -q -o "$W/project.zip" -d "$W/p" || fail "could not unzip the project"
if [ ! -s "$W/android.jar" ]; then
  curl -sf -m 600 -o "$W/android.jar" "$B/android.jar" || { rm -f "$W/android.jar"; fail "could not get android.jar from the app"; }
fi
# the Build Doctor's pre-build pass (rules that need no build log), before the module is located: it may fix the layout
rm -f "$W/wzh_doctor.js"
curl -sf -m 120 -o "$W/wzh_doctor.js" "$B/wzh_doctor.js" || rm -f "$W/wzh_doctor.js"
if [ -s "$W/wzh_doctor.js" ] && ! command -v node >/dev/null 2>&1; then
  step "Installing the repair tool (one time)"
  pkg install -y nodejs >"$W/cmd.log" 2>&1; sendlog "$W/cmd.log"
fi
if [ -s "$W/wzh_doctor.js" ] && command -v node >/dev/null 2>&1; then
  step "Checking the project"
  doctor_run ""
fi
else
# a repeat after the Build Doctor changed the project: keep the repaired sources, redo the build output
rm -rf "$W/o"
mkdir -p "$W/o/classes" "$W/o/gen" "$W/o/dex"
fi

MF=$(find "$W/p" -type d -name build -prune -o -name AndroidManifest.xml -print | grep '/src/main/' | head -1)
[ -z "$MF" ] && MF=$(find "$W/p" -type d -name build -prune -o -name AndroidManifest.xml -print | head -1)
[ -z "$MF" ] && fail "No AndroidManifest.xml found in the project"
MAIN=$(dirname "$MF")
if [ "$(basename "$(dirname "$MAIN")")" = "src" ]; then MOD=$(dirname "$(dirname "$MAIN")"); else MOD="$MAIN"; fi
RES="$MAIN/res"; ASSETS="$MAIN/assets"; JNI="$MAIN/jniLibs"

G=""
for f in "$MOD/build.gradle.kts" "$MOD/build.gradle"; do [ -f "$f" ] && { G="$f"; break; }; done
gnum() { [ -n "$G" ] || return 0; sed -nE "s/^[[:space:]]*$1[[:space:]]*=?[[:space:]]*\(?([0-9]+).*/\1/p" "$G" | head -1; }
gstr() { [ -n "$G" ] || return 0; sed -nE "s/^[[:space:]]*$1[[:space:]]*=?[[:space:]]*\(?[\"']([^\"']*)[\"'].*/\1/p" "$G" | head -1; }

MINSDK=$(gnum minSdk); [ -z "$MINSDK" ] && MINSDK=$(gnum minSdkVersion); [ -z "$MINSDK" ] && MINSDK=21
TARGET=$(gnum targetSdk); [ -z "$TARGET" ] && TARGET=$(gnum targetSdkVersion); [ -z "$TARGET" ] && TARGET=34
VC=$(gnum versionCode); [ -z "$VC" ] && VC=1
VN=$(gstr versionName); [ -z "$VN" ] && VN="1.0"
NS=$(gstr namespace)
AID=$(gstr applicationId)
MPKG=$(sed -nE 's/.*[[:space:]<]package="([^"]+)".*/\1/p' "$MF" | head -1)
RPKG="${NS:-${MPKG:-$AID}}"
[ -z "$RPKG" ] && fail "Could not find the app's package name (add a namespace in build.gradle or a package in the manifest)"
APPID="${APPID_ARG:-${AID:-$RPKG}}"

cp "$MF" "$W/o/AndroidManifest.xml"
if [ -z "$MPKG" ]; then
  sed -i "0,/<manifest/s//<manifest package=\"$RPKG\"/" "$W/o/AndroidManifest.xml"
elif [ -n "$NS" ] && [ "$MPKG" != "$NS" ]; then
  sed -i "s/package=\"$MPKG\"/package=\"$NS\"/" "$W/o/AndroidManifest.xml"
fi

step "Fetching libraries"
HASDEPS=""
[ -n "$G" ] && grep -E '^[[:space:]]*(implementation|api|compileOnly|runtimeOnly)\b' "$G" 2>/dev/null | grep -qv fileTree && HASDEPS=1
[ -n "$G" ] && grep -q 'viewBinding' "$G" 2>/dev/null && HASDEPS=1
if ! command -v python3 >/dev/null 2>&1; then
  pkg install -y python >"$W/cmd.log" 2>&1; sendlog "$W/cmd.log"
fi
cat > "$W/wzh_deps.py" <<'PYEOF'
# WZH 2 APK: Gradle-like dependency step for the on-phone build (stdlib only).
# Reads the module's build.gradle(.kts) + version catalog, downloads POMs/JARs/AARs from Google Maven,
# Maven Central (and any maven{} URLs the project lists), resolves versions like Gradle (highest wins, BOMs),
# unpacks AARs, compiles library resources, merges library manifests, writes BuildConfig.java.
import sys, os, re, json, subprocess, urllib.request, urllib.error, zipfile, shutil, glob
from collections import deque
import xml.etree.ElementTree as ET

ANS = 'http://schemas.android.com/apk/res/android'
TNS = 'http://schemas.android.com/tools'
for p, u in (('android', ANS), ('tools', TNS), ('dist', 'http://schemas.android.com/apk/distribution'),
             ('app', 'http://schemas.android.com/apk/res-auto')):
    ET.register_namespace(p, u)

MOD, APPID, RPKG, VC, VN, W, OUT, MFSRC, MFDST, GEN = sys.argv[1:11]
os.makedirs(OUT, exist_ok=True)
CACHE = os.path.join(W, 'm2')


class Fail(Exception):
    pass


def log(*a):
    print('WZH:', *a, flush=True)


def rd(p):
    try:
        t = open(p, encoding='utf-8', errors='replace').read()
    except OSError:
        return ''
    t = re.sub(r'/\*.*?\*/', '', t, flags=re.S)
    return re.sub(r'(?m)^\s*(//|#).*$', '', t)


def find_root(mod):
    d = os.path.abspath(mod)
    for _ in range(4):
        if any(os.path.exists(os.path.join(d, f)) for f in ('settings.gradle', 'settings.gradle.kts')):
            return d
        d = os.path.dirname(d)
    return os.path.abspath(mod)


ROOT = find_root(MOD)
GF = ('build.gradle', 'build.gradle.kts')
MODG = ''.join(rd(os.path.join(MOD, f)) for f in GF)
ROOTG = ''.join(rd(os.path.join(ROOT, f)) for f in GF + ('settings.gradle', 'settings.gradle.kts'))
PROPS = rd(os.path.join(ROOT, 'gradle.properties'))
TOML = rd(os.path.join(ROOT, 'gradle', 'libs.versions.toml'))
ALLG = MODG + '\n' + ROOTG

# ---------- variables ($version, ext.x, gradle.properties) ----------
VARS = {}
for m in re.finditer(r'(?m)^\s*(?:(?:def|val|var|ext\.|extra\[)\s*)?["\']?([A-Za-z_][\w.]*)["\']?\]?\s*=\s*["\']([^"\'\n]+)["\']', ALLG):
    VARS[m.group(1)] = m.group(2)
for m in re.finditer(r'(?m)^([\w.]+)\s*=\s*(\S.*)$', PROPS):
    VARS.setdefault(m.group(1), m.group(2).strip())


def subvars(s):
    def f(m):
        n = m.group(1)
        return VARS.get(n, VARS.get(n.split('.')[-1], m.group(0)))
    return re.sub(r'\$\{?([\w.]+)\}?', f, s)


# ---------- version catalog ----------
CAT, BUNDLES = {}, {}


def parse_catalog(t):
    secs, cur = {}, None
    for line in t.splitlines():
        line = line.split(' #')[0].strip()
        m = re.match(r'\[([\w.\-]+)\]$', line)
        if m:
            cur = m.group(1)
            secs[cur] = []
            continue
        if cur and line:
            secs[cur].append(line)
    vers = {}
    for line in secs.get('versions', []):
        m = re.match(r'([\w.\-]+)\s*=\s*(.+)$', line)
        if not m:
            continue
        s = re.search(r'["\']([^"\']+)["\']', m.group(2))
        if s:
            vers[m.group(1)] = s.group(1)
    for line in secs.get('libraries', []):
        m = re.match(r'([\w.\-]+)\s*=\s*(.+)$', line)
        if not m:
            continue
        k, v = m.group(1), m.group(2).strip()
        if v[0] in '"\'':
            coord = v.strip('"\'')
        else:
            mod = re.search(r'module\s*=\s*["\']([^"\']+)', v)
            if mod:
                ga = mod.group(1)
            else:
                g = re.search(r'group\s*=\s*["\']([^"\']+)', v)
                n = re.search(r'\bname\s*=\s*["\']([^"\']+)', v)
                if not (g and n):
                    continue
                ga = g.group(1) + ':' + n.group(1)
            ref = re.search(r'version\.ref\s*=\s*["\']([^"\']+)', v)
            ver = re.search(r'\bversion\s*=\s*["\']([^"\']+)', v)
            vv = vers.get(ref.group(1)) if ref else (ver.group(1) if ver else None)
            coord = ga + (':' + vv if vv else '')
        CAT[k.replace('-', '.').replace('_', '.')] = coord
    for line in secs.get('bundles', []):
        m = re.match(r'([\w.\-]+)\s*=\s*\[(.*)\]', line)
        if m:
            BUNDLES[m.group(1).replace('-', '.').replace('_', '.')] = [
                x.replace('-', '.').replace('_', '.') for x in re.findall(r'["\']([^"\']+)["\']', m.group(2))]


parse_catalog(TOML)

# ---------- repositories ----------
REPOS = ['https://dl.google.com/dl/android/maven2', 'https://repo1.maven.org/maven2']
for m in re.finditer(r'(?m)^.*\bmaven\b.*?["\'](https?://[^"\']+)["\']', ROOTG + '\n' + MODG):
    u = m.group(1).rstrip('/')
    if u not in REPOS:
        REPOS.append(u)
for u in ('https://jitpack.io',):
    if u not in REPOS:
        REPOS.append(u)
if os.environ.get('WZH_REPOS'):
    REPOS = os.environ['WZH_REPOS'].split()

# ---------- fetching ----------
MISS, NETERR = set(), []


def fetch(g, a, v, ext):
    rel = '%s/%s/%s/%s-%s.%s' % (g.replace('.', '/'), a, v, a, v, ext)
    dst = os.path.join(CACHE, rel)
    if os.path.exists(dst) and os.path.getsize(dst) > 0:
        return dst
    if rel in MISS:
        return None
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    for r in REPOS:
        for attempt in (1, 2):
            try:
                req = urllib.request.Request(r.rstrip('/') + '/' + rel, headers={'User-Agent': 'wzh-build'})
                with urllib.request.urlopen(req, timeout=60) as resp, open(dst + '.part', 'wb') as f:
                    shutil.copyfileobj(resp, f)
                os.replace(dst + '.part', dst)
                return dst
            except urllib.error.HTTPError:
                break  # not in this repo
            except Exception as e:  # network trouble: try once more
                if attempt == 2:
                    NETERR.append(str(e))
    MISS.add(rel)
    return None


# ---------- versions ----------
QUAL = {'dev': -6, 'alpha': -5, 'a': -5, 'beta': -4, 'b': -4, 'm': -3, 'milestone': -3, 'rc': -2, 'cr': -2,
        'snapshot': -1, 'final': 0, 'ga': 0, 'release': 0, 'sp': 1}


def vkey(v):
    out = []
    for t in re.findall(r'\d+|[A-Za-z]+', v or ''):
        out.append((1, int(t)) if t.isdigit() else (0, QUAL.get(t.lower(), 0.5)))
    return out


def vcmp(a, b):
    ka, kb = vkey(a), vkey(b)
    n = max(len(ka), len(kb))
    ka += [(0, 0)] * (n - len(ka))
    kb += [(0, 0)] * (n - len(kb))
    return (ka > kb) - (ka < kb)


def latest(g, a, prefix):
    p = fetch_meta(g, a)
    best = None
    for v in p:
        if v.startswith(prefix) and (best is None or vcmp(v, best) > 0):
            best = v
    return best


def fetch_meta(g, a):
    rel = '%s/%s/maven-metadata.xml' % (g.replace('.', '/'), a)
    for r in REPOS:
        try:
            with urllib.request.urlopen(urllib.request.Request(r.rstrip('/') + '/' + rel, headers={'User-Agent': 'wzh-build'}), timeout=60) as resp:
                return re.findall(r'<version>([^<]+)</version>', resp.read().decode('utf-8', 'replace'))
        except Exception:
            continue
    return []


# ---------- POMs ----------
POMS = {}


def pom(g, a, v):
    k = (g, a, v)
    if k in POMS:
        return POMS[k]
    POMS[k] = None
    p = fetch(g, a, v, 'pom')
    if not p:
        return None
    txt = open(p, encoding='utf-8', errors='replace').read()
    txt = re.sub(r'\sxmlns(:\w+)?="[^"]*"', '', txt)
    txt = re.sub(r'\sxsi:schemaLocation="[^"]*"', '', txt)
    try:
        root = ET.fromstring(txt)
    except ET.ParseError:
        return None

    def t(e, n):
        x = e.find(n)
        return x.text.strip() if x is not None and x.text else None

    props, mgmt = {}, {}
    par = root.find('parent')
    pg = pv = None
    if par is not None:
        pg, pa, pv = t(par, 'groupId'), t(par, 'artifactId'), t(par, 'version')
        pi = pom(pg, pa, pv) if pg and pa and pv else None
        if pi:
            props.update(pi['props'])
            mgmt.update(pi['mgmt'])
    gid = t(root, 'groupId') or pg
    ver = t(root, 'version') or pv
    props.update({'project.groupId': gid, 'project.version': ver, 'project.artifactId': a, 'version': ver,
                  'pom.version': ver, 'groupId': gid, 'project.parent.version': pv, 'parent.version': pv,
                  'project.parent.groupId': pg})
    pe = root.find('properties')
    if pe is not None:
        for c in pe:
            props[c.tag] = (c.text or '').strip()

    def sub(s):
        for _ in range(6):
            n = re.sub(r'\$\{([^}]+)\}', lambda m: str(props.get(m.group(1), m.group(0))), s or '')
            if n == s:
                break
            s = n
        return s

    local = {}
    for d in root.findall('dependencyManagement/dependencies/dependency'):
        dg, da, dv = sub(t(d, 'groupId')), sub(t(d, 'artifactId')), sub(t(d, 'version'))
        if t(d, 'scope') == 'import' and t(d, 'type') == 'pom' and dg and da and dv:
            bi = pom(dg, da, dv)
            if bi:
                mgmt.update(bi['mgmt'])
        elif dg and da and dv:
            local[(dg, da)] = dv
    mgmt.update(local)
    deps = []
    for d in root.findall('dependencies/dependency'):
        dg, da = sub(t(d, 'groupId')), sub(t(d, 'artifactId'))
        dv = sub(t(d, 'version')) or mgmt.get((dg, da))
        scope = t(d, 'scope') or 'compile'
        if scope not in ('compile', 'runtime') or (t(d, 'optional') or '') == 'true':
            continue
        ex = set()
        for e in d.findall('exclusions/exclusion'):
            ex.add((t(e, 'groupId') or '*', t(e, 'artifactId') or '*'))
        deps.append((dg, da, dv, ex))
    info = {'props': props, 'mgmt': mgmt, 'deps': deps, 'packaging': (t(root, 'packaging') or 'jar')}
    POMS[k] = info
    return info


# ---------- resolve ----------
def split_coord(s):
    s = subvars(s).split('@')[0]
    p = s.split(':')
    if len(p) < 2:
        return None
    return p[0], p[1], (p[2] if len(p) > 2 and p[2] else None)


def resolve(roots, boms):
    """roots: [(g,a,v|None)], boms: [(g,a,v)] -> ordered [(g,a,v)] with dependencies before dependents."""
    bom = {}
    for (g, a, v) in boms:
        bi = pom(g, a, v)
        if bi:
            bom.update(bi['mgmt'])
        else:
            log('WARN could not read BOM %s:%s:%s' % (g, a, v))
    chosen, done, kids, direct = {}, set(), {}, []
    q = deque()

    def want(g, a, v, ex=frozenset()):
        if (g, a) in bom and (v is None or vcmp(bom[(g, a)], v) > 0):
            v = bom[(g, a)]
        if v is None:
            log('WARN no version for %s:%s (add it to build.gradle)' % (g, a))
            return
        if '+' in v:
            lv = latest(g, a, v.split('+')[0])
            if not lv:
                log('WARN could not pick a version for %s:%s:%s' % (g, a, v))
                return
            v = lv
        if v.startswith('[') or v.startswith('('):
            v = re.split(r'[\[\](),]', v.strip())[1] or v
        if (g, a) not in chosen or vcmp(v, chosen[(g, a)]) > 0:
            chosen[(g, a)] = v
            q.append((g, a, v, ex))

    for r in roots:
        want(*r)
        direct.append((r[0], r[1]))
    redirects = {}
    while True:
        while q:
            g, a, v, ex = q.popleft()
            if chosen.get((g, a)) != v or (g, a, v) in done:
                continue
            done.add((g, a, v))
            info = pom(g, a, v)
            if info is None:
                kids[(g, a, v)] = []
                continue
            ks = []
            for (dg, da, dv, dex) in info['deps']:
                if any((eg == '*' or eg == dg) and (ea == '*' or ea == da) for (eg, ea) in ex):
                    continue
                want(dg, da, dv, frozenset(ex | dex))
                ks.append((dg, da))
            kids[(g, a, v)] = ks
        # artifacts of Kotlin-multiplatform style roots may live in another module (Gradle .module redirect)
        grew = False
        for (g, a), v in list(chosen.items()):
            if (g, a, v) in redirects:
                continue
            redirects[(g, a, v)] = None
            info = pom(g, a, v)
            if info is not None and info['packaging'] == 'pom':
                continue
            if fetch(g, a, v, 'aar') or fetch(g, a, v, 'jar'):
                continue
            mp = fetch(g, a, v, 'module')
            if not mp:
                continue
            try:
                j = json.load(open(mp, encoding='utf-8'))
            except Exception:
                continue
            best = None
            for var in j.get('variants', []):
                at, at_attr = var.get('available-at'), var.get('attributes', {})
                if not at or at_attr.get('org.gradle.usage') not in ('java-runtime', 'java-api'):
                    continue
                pt = at_attr.get('org.jetbrains.kotlin.platform.type')
                rank = {'androidJvm': 2, 'jvm': 1}.get(pt, 0)
                if rank and (best is None or rank > best[0]):
                    best = (rank, at)
            if best:
                at = best[1]
                redirects[(g, a, v)] = (at['group'], at['module'], at['version'])
                want(at['group'], at['module'], at['version'])
                kids.setdefault((g, a, v), []).append((at['group'], at['module']))
                grew = True
        if not q and not grew:
            break
    order, seen = [], set()

    def visit(ga):
        if ga in seen or ga not in chosen:
            return
        seen.add(ga)
        v = chosen[ga]
        for k in kids.get(ga + (v,), []):
            visit(k)
        order.append(ga + (v,))

    for ga in direct:
        visit(ga)
    for ga in list(chosen):
        visit(ga)
    return order


# ---------- artifacts ----------
def get_artifact(g, a, v):
    info = pom(g, a, v)
    if info is not None and info['packaging'] == 'pom':
        return None, None
    exts = ['aar', 'jar'] if (info and info['packaging'] == 'aar') else ['jar', 'aar']
    for ext in exts:
        f = fetch(g, a, v, ext)
        if f:
            return f, ext
    return None, None


def unpack_aar(path, name):
    d = os.path.join(OUT, 'aar', name)
    shutil.rmtree(d, ignore_errors=True)
    with zipfile.ZipFile(path) as z:
        z.extractall(d)
    return d


def drop_dupes(order):
    keys = {(g, a) for g, a, v in order}
    out = []
    for g, a, v in order:
        if (g, a + '-jvm') in keys or (g, a + '-android') in keys:
            continue
        if g == 'org.jetbrains.kotlin' and a in ('kotlin-stdlib-jdk7', 'kotlin-stdlib-jdk8') and \
                (g, 'kotlin-stdlib') in keys and vcmp(dict(((x, y), z) for x, y, z in order)[(g, 'kotlin-stdlib')], '1.8.0') >= 0:
            continue
        out.append((g, a, v))
    return out


# ---------- the module's declared dependencies ----------
CFG = r'(implementation|api|compileOnly|runtimeOnly)'
P1 = re.compile(r'(?m)^\s*' + CFG + r'\b\s*\(?\s*(platform\s*\(\s*)?(?:"([^"\n]+)"|\'([^\'\n]+)\'|(libs(?:\.[A-Za-z0-9_]+)+))')
P2 = re.compile(r'(?m)^\s*' + CFG + r'\b\s*\(?\s*group\s*[:=]\s*["\']([^"\']+)["\']\s*,\s*name\s*[:=]\s*["\']([^"\']+)["\'](?:\s*,\s*version\s*[:=]\s*["\']([^"\']+)["\'])?')

RT, CO, BOMS = [], [], []


def add_dep(cfg, plat, coord):
    c = split_coord(coord)
    if not c:
        return
    if plat:
        if c[2]:
            BOMS.append(c)
        return
    (CO if cfg == 'compileOnly' else RT).append(c)


for m in P1.finditer(MODG):
    cfg, plat, s1, s2, cat = m.groups()
    if cat:
        key = re.sub(r'^libs\.', '', cat)
        if key.startswith('bundles.'):
            for k in BUNDLES.get(key[len('bundles.'):], []):
                if k in CAT:
                    add_dep(cfg, plat, CAT[k])
            continue
        key = re.sub(r'\.(get|asProvider)$', '', key)
        if key in CAT:
            add_dep(cfg, plat, CAT[key])
        else:
            log('WARN %s is not in gradle/libs.versions.toml' % cat)
    else:
        add_dep(cfg, plat, s1 or s2)
for m in P2.finditer(MODG):
    cfg, g, a, v = m.groups()
    add_dep(cfg, False, '%s:%s%s' % (g, a, ':' + v if v else ''))

for pat, what in ((r'\bkapt\b|\bksp\b|annotationProcessor', 'annotation processors (kapt/ksp/annotationProcessor)'),
                  (r'dataBinding', 'data binding'),
                  (r'compose\s*=\s*true|androidx\.compose|composeOptions', 'Jetpack Compose'),
                  (r'project\s*\(\s*["\']:', 'other Gradle modules (project(":x"))')):
    if re.search(pat, MODG):
        log('WARN this project uses %s; the on-phone build cannot do that yet, use the server build if it fails.' % what)

# ---------- go ----------
def main():
    log('project root:', ROOT)
    log('repositories:', ', '.join(REPOS))
    log('dependencies: %d runtime, %d compile-only, %d BOM' % (len(RT), len(CO), len(BOMS)))
    try:
        rt = drop_dupes(resolve(RT, BOMS)) if RT else []
        rtkeys = {(g, a) for g, a, v in rt}
        co = [x for x in (drop_dupes(resolve(CO, BOMS)) if CO else []) if (x[0], x[1]) not in rtkeys]
    except Exception as e:
        raise Fail('Could not resolve the libraries: %s' % e)
    jars, cpjars, libres, pkgs, assets, manifests = [], [], [], [], [], []
    os.makedirs(os.path.join(OUT, 'res'), exist_ok=True)
    missing = []

    def take(items, runtime):
        for g, a, v in items:
            name = '%s_%s_%s' % (g, a, v)
            f, ext = get_artifact(g, a, v)
            if not f:
                info = pom(g, a, v)
                if info is None:
                    missing.append('%s:%s:%s' % (g, a, v))
                continue
            log(('+ ' if runtime else '~ ') + '%s:%s:%s' % (g, a, v))
            if ext == 'jar':
                (jars if runtime else cpjars).append(f)
                continue
            d = unpack_aar(f, name)
            cj = os.path.join(d, 'classes.jar')
            if os.path.exists(cj):
                (jars if runtime else cpjars).append(cj)
            for x in sorted(glob.glob(os.path.join(d, 'libs', '*.jar'))):
                (jars if runtime else cpjars).append(x)
            if not runtime:
                continue
            res = os.path.join(d, 'res')
            if os.path.isdir(res) and os.listdir(res):
                z = os.path.join(OUT, 'res', name + '.zip')
                r = subprocess.run(['aapt2', 'compile', '--dir', res, '-o', z], capture_output=True, text=True)
                if r.returncode != 0:
                    print(r.stdout + r.stderr)
                    raise Fail('aapt2 could not compile the resources of %s:%s:%s' % (g, a, v))
                libres.append(z)
            mf = os.path.join(d, 'AndroidManifest.xml')
            if os.path.exists(mf):
                manifests.append(mf)
                mm = re.search(r'\spackage="([^"]+)"', open(mf, encoding='utf-8', errors='replace').read())
                if mm and mm.group(1) != RPKG and mm.group(1) not in pkgs:
                    pkgs.append(mm.group(1))
            ad = os.path.join(d, 'assets')
            if os.path.isdir(ad) and os.listdir(ad):
                assets.append(ad)
            jd = os.path.join(d, 'jni')
            if os.path.isdir(jd):
                shutil.copytree(jd, os.path.join(OUT, 'jnistage', 'lib'), dirs_exist_ok=True)

    take(rt, True)
    take(co, False)
    # .aar files dropped in the module's libs/ folder
    for f in sorted(glob.glob(os.path.join(MOD, 'libs', '*.aar'))):
        log('+ local', os.path.basename(f))
        d = unpack_aar(f, 'local_' + os.path.basename(f))
        cj = os.path.join(d, 'classes.jar')
        if os.path.exists(cj):
            jars.append(cj)
        res = os.path.join(d, 'res')
        if os.path.isdir(res) and os.listdir(res):
            z = os.path.join(OUT, 'res', 'local_' + os.path.basename(f) + '.zip')
            r = subprocess.run(['aapt2', 'compile', '--dir', res, '-o', z], capture_output=True, text=True)
            if r.returncode != 0:
                print(r.stdout + r.stderr)
                raise Fail('aapt2 could not compile the resources of %s' % os.path.basename(f))
            libres.append(z)
        mf = os.path.join(d, 'AndroidManifest.xml')
        if os.path.exists(mf):
            manifests.append(mf)
            mm = re.search(r'\spackage="([^"]+)"', open(mf, encoding='utf-8', errors='replace').read())
            if mm and mm.group(1) != RPKG and mm.group(1) not in pkgs:
                pkgs.append(mm.group(1))
    if missing:
        hint = ' (no internet?)' if NETERR else ''
        raise Fail('Could not download: %s%s. Checked: %s' % (', '.join(missing[:6]), hint, ', '.join(REPOS)))

    nres = pack_java_resources(jars + sorted(glob.glob(os.path.join(MOD, 'libs', '*.jar'))))
    log('library resources (META-INF/services, data files): %d' % nres)
    merge_manifest(MFSRC, MFDST, manifests)
    write_buildconfig()
    nvb = write_viewbinding()
    if nvb:
        write_vb_interface(jars)
        log('view binding: generated %d binding classes' % nvb)

    def lst(name, items):
        open(os.path.join(OUT, name), 'w').write('\n'.join(items) + ('\n' if items else ''))
    lst('jars.txt', jars)
    lst('cpjars.txt', cpjars)
    lst('libres.txt', libres)
    lst('pkgs.txt', [':'.join(pkgs)] if pkgs else [])
    lst('assets.txt', assets)
    log('done: %d jars, %d resource sets, %d manifests merged' % (len(jars), len(libres), len(manifests)))



# ---------- Java resources inside libraries (META-INF/services/..., data files) ----------
# Gradle copies the non-class files of every dependency into the APK; d8 only makes dex. Without them e.g.
# kotlinx-coroutines cannot find its Dispatchers.Main (a META-INF/services file) and the app dies at start.
RES_SKIP_PREFIX = ('meta-inf/maven/', 'meta-inf/proguard/', 'meta-inf/com.android.tools/', 'meta-inf/versions/',
                   'meta-inf/native-image/', 'meta-inf/license', 'meta-inf/notice', 'meta-inf/dependencies')
RES_SKIP_EXACT = ('meta-inf/manifest.mf', 'meta-inf/index.list', 'androidmanifest.xml', 'resources.arsc')


def keep_java_res(name):
    low = name.lower()
    if name.endswith('/') or low.endswith('.class') or '..' in name.split('/') or name.startswith('/'):
        return False
    if low in RES_SKIP_EXACT or low.startswith(RES_SKIP_PREFIX):
        return False
    if low.startswith(('res/', 'lib/')) or re.match(r'classes\d*\.dex$', low):
        return False
    if low.startswith('meta-inf/') and re.search(r'\.(sf|rsa|dsa|ec|kotlin_module|version)$', low):
        return False
    return not low.endswith(('.java', '.kt', '.kotlin_metadata'))


def pack_java_resources(jars):
    out = os.path.join(OUT, 'jres')
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out, exist_ok=True)
    n = 0
    # the app's own src/main/resources first (it wins), then every library jar
    own = os.path.join(MOD, 'src', 'main', 'resources')
    if os.path.isdir(own):
        for dp, dn, fn in os.walk(own):
            for f in fn:
                rel = os.path.relpath(os.path.join(dp, f), own).replace(os.sep, '/')
                if keep_java_res(rel):
                    dst = os.path.join(out, rel)
                    os.makedirs(os.path.dirname(dst), exist_ok=True)
                    shutil.copyfile(os.path.join(dp, f), dst)
                    n += 1
    for jp in jars:
        try:
            z = zipfile.ZipFile(jp)
        except Exception:
            continue
        with z:
            for info in z.infolist():
                nm = info.filename
                if not keep_java_res(nm):
                    continue
                dst = os.path.join(out, nm)
                data = z.read(nm)
                if os.path.exists(dst):
                    # service files from several libraries are merged; anything else: the first one wins
                    if nm.lower().startswith('meta-inf/services/'):
                        have = open(dst, 'rb').read().decode('utf-8', 'replace').splitlines()
                        add = [l for l in data.decode('utf-8', 'replace').splitlines() if l.strip() and l not in have]
                        if add:
                            with open(dst, 'ab') as f:
                                f.write(('\n' + '\n'.join(add) + '\n').encode('utf-8'))
                    continue
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                with open(dst, 'wb') as f:
                    f.write(data)
                n += 1
    return n

# ---------- manifest merge ----------
def merge_manifest(src, dst, libmfs):
    def load(path, extra=None):
        txt = open(path, encoding='utf-8', errors='replace').read().replace('${applicationId}', APPID)
        return ET.fromstring(txt)
    placeholder = '${applicationId}' in open(src, encoding='utf-8', errors='replace').read()
    root = load(src)
    app = root.find('application')
    if app is None:
        app = ET.SubElement(root, 'application')
    N = '{%s}name' % ANS
    tools_node = '{%s}node' % TNS

    def key(e):
        return (e.tag, e.get(N) or e.get('{%s}glEsVersion' % ANS) or ET.tostring(e))
    have = {key(e) for e in list(root)} | {key(e) for e in list(app)}
    top = ('uses-permission', 'uses-permission-sdk-23', 'uses-feature', 'permission', 'queries')

    def clean(e, pkg):
        for x in e.iter():
            for k in list(x.attrib):
                if k.startswith('{%s}' % TNS):
                    del x.attrib[k]
        if e.get(N, '').startswith('.') and pkg:
            e.set(N, pkg + e.get(N))
        for x in e.iter():
            if x is not e and x.get(N, '').startswith('.') and pkg and x.tag in ('activity', 'service', 'receiver', 'provider'):
                x.set(N, pkg + x.get(N))
    added = 0
    for mf in libmfs:
        lr = load(mf)
        pkg = lr.get('package')
        for e in list(lr):
            if e.tag in top:
                if e.get(tools_node) in ('remove', 'removeAll'):
                    continue
                if e.tag == 'queries':
                    q = root.find('queries')
                    if q is None:
                        q = ET.Element('queries')
                        root.insert(0, q)
                    qh = {ET.tostring(x) for x in q}
                    for c in list(e):
                        if ET.tostring(c) not in qh:
                            q.append(c)
                    continue
                if key(e) in have:
                    continue
                clean(e, pkg)
                root.insert(0, e)
                have.add(key(e))
                added += 1
        la = lr.find('application')
        if la is not None:
            for e in list(la):
                if e.get(tools_node) in ('remove', 'removeAll'):
                    continue
                clean(e, pkg)
                if key(e) in have:
                    continue
                app.append(e)
                have.add(key(e))
                added += 1
    if not added and not placeholder:
        log('manifest unchanged')
        return
    ET.ElementTree(root).write(dst, encoding='utf-8', xml_declaration=True)
    log('manifest merge added %d entries from libraries' % added)



# ---------- ViewBinding (buildFeatures { viewBinding = true }) ----------
VB_ON = bool(re.search(r'viewBinding\s*(?:=\s*true|\s+true|\.enabled\s*=\s*true|\{\s*(?:enabled|isEnabled)\s*(?:=\s*)?true)', MODG))
RESDIR = os.environ.get('WZH_RES') or os.path.join(MOD, 'src', 'main', 'res')
VIEW_PKG = {'View': 'android.view', 'ViewGroup': 'android.view', 'ViewStub': 'android.view',
            'SurfaceView': 'android.view', 'TextureView': 'android.view', 'WebView': 'android.webkit'}


def vb_class(layout):
    return ''.join(p[:1].upper() + p[1:] for p in layout.split('_')) + 'Binding'


def vb_field(idname):
    parts = [p for p in idname.split('_') if p]
    return (parts[0][:1].lower() + parts[0][1:] if parts else idname) + ''.join(p[:1].upper() + p[1:] for p in parts[1:])


def vb_type(el):
    tag = el.tag
    if tag == 'view':
        tag = el.get('class') or 'android.view.View'
    if '.' in tag:
        return tag
    if tag in ('fragment',):
        return 'android.view.View'
    return VIEW_PKG.get(tag, 'android.widget') + '.' + tag if tag in VIEW_PKG else 'android.widget.' + tag


def vb_parse(path):
    try:
        root = ET.parse(path).getroot()
    except Exception:
        return None
    if root.get('{%s}viewBindingIgnore' % TNS) == 'true':
        return None
    ID = '{%s}id' % ANS
    fields = []

    def nm(el):
        i = el.get(ID) or ''
        return i.split('/', 1)[1] if i.startswith(('@+id/', '@id/')) else None

    def walk(el, is_root):
        name = nm(el)
        if el.tag == 'include':
            lay = (el.get('layout') or '').split('/')[-1]
            if name and lay:
                fields.append((name, ('include', lay), False))
            return
        if el.tag != 'merge' and name:
            fields.append((name, vb_type(el), is_root))
        for c in el:
            walk(c, False)
    walk(root, True)
    return {'root': None if root.tag == 'merge' else vb_type(root), 'fields': fields}


def write_viewbinding():
    if not VB_ON or not os.path.isdir(RESDIR):
        return 0
    layouts = {}
    for d in sorted(os.listdir(RESDIR)):
        if d == 'layout' or d.startswith('layout-'):
            for f in sorted(os.listdir(os.path.join(RESDIR, d))):
                if f.endswith('.xml'):
                    pr = vb_parse(os.path.join(RESDIR, d, f))
                    if pr:
                        layouts.setdefault(f[:-4], []).append(pr)
    if not layouts:
        return 0
    pkg = RPKG + '.databinding'
    outdir = os.path.join(GEN, *pkg.split('.'))
    os.makedirs(outdir, exist_ok=True)
    n = 0
    for lay, prs in sorted(layouts.items()):
        cls = vb_class(lay)
        roots = {p['root'] for p in prs}
        merge = None in roots
        rtype = 'android.view.View' if (merge or len(roots) != 1) else list(roots)[0]
        names, info = [], {}
        for p in prs:
            for (nme, typ, isroot) in p['fields']:
                if nme not in info:
                    names.append(nme)
                    info[nme] = {'types': set(), 'count': 0, 'root': False}
                info[nme]['types'].add(typ)
                info[nme]['count'] += 1
                info[nme]['root'] = info[nme]['root'] or isroot
        fl = []
        for nme in names:
            t = info[nme]['types']
            if len(t) == 1:
                typ = list(t)[0]
                typ = (pkg + '.' + vb_class(typ[1]) if layouts.get(typ[1]) else 'android.view.View') if isinstance(typ, tuple) else typ
                inc = isinstance(list(t)[0], tuple) and layouts.get(list(t)[0][1]) is not None
            else:
                typ, inc = 'android.view.View', False
            fl.append((nme, vb_field(nme), typ, inc, info[nme]['count'] < len(prs), info[nme]['root']))
        L = ['package %s;' % pkg, '',
             'import android.view.LayoutInflater;', 'import android.view.View;', 'import android.view.ViewGroup;',
             'import androidx.viewbinding.ViewBinding;', 'import %s.R;' % RPKG, '',
             'public final class %s implements ViewBinding {' % cls,
             '  private final %s rootView;' % rtype]
        for (_, f, typ, _, _, _) in fl:
            L.append('  public final %s %s;' % (typ, f))
        L.append('  private %s(%s) {' % (cls, ', '.join(['%s rootView' % rtype] + ['%s %s' % (t, f) for (_, f, t, _, _, _) in fl])))
        L.append('    this.rootView = rootView;')
        for (_, f, _, _, _, _) in fl:
            L.append('    this.%s = %s;' % (f, f))
        L += ['  }', '  public %s getRoot() { return rootView; }' % rtype]
        if merge:
            L += ['  public static %s inflate(LayoutInflater inflater, ViewGroup parent) {' % cls,
                  '    if (parent == null) { throw new NullPointerException("parent"); }',
                  '    inflater.inflate(R.layout.%s, parent);' % lay, '    return bind(parent);', '  }']
        else:
            L += ['  public static %s inflate(LayoutInflater inflater) { return inflate(inflater, null, false); }' % cls,
                  '  public static %s inflate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {' % cls,
                  '    View root = inflater.inflate(R.layout.%s, parent, false);' % lay,
                  '    if (attachToParent) { parent.addView(root); }', '    return bind(root);', '  }']
        L.append('  public static %s bind(View rootView) {' % cls)
        for (nme, f, typ, inc, nullable, isroot) in fl:
            if isroot and not nullable:
                L.append('    %s %s = (%s) rootView;' % (typ, f, typ))
                continue
            if inc:
                L.append('    View %s_v = rootView.findViewById(R.id.%s);' % (f, nme))
                L.append('    %s %s = %s_v == null ? null : %s.bind(%s_v);' % (typ, f, f, typ, f))
            else:
                L.append('    %s %s = (%s) rootView.findViewById(R.id.%s);' % (typ, f, typ, nme))
            if not nullable:
                L.append('    if (%s == null) { throw new NullPointerException("Missing required view with ID: %s"); }' % (f, nme))
        L.append('    return new %s(%s);' % (cls, ', '.join(['(%s) rootView' % rtype] + [f for (_, f, _, _, _, _) in fl])))
        L += ['  }', '}', '']
        open(os.path.join(outdir, cls + '.java'), 'w', encoding='utf-8').write('\n'.join(L))
        n += 1
    return n


def write_vb_interface(jars):
    if any('viewbinding' in os.path.basename(j).lower() for j in jars):
        return
    d = os.path.join(GEN, 'androidx', 'viewbinding')
    os.makedirs(d, exist_ok=True)
    open(os.path.join(d, 'ViewBinding.java'), 'w').write(
        'package androidx.viewbinding;\npublic interface ViewBinding {\n  android.view.View getRoot();\n}\n')

# ---------- BuildConfig ----------
def write_buildconfig():
    for dp, dn, fn in os.walk(os.path.join(MOD, 'src')):
        if 'build' in dn:
            dn.remove('build')
        if 'BuildConfig.java' in fn or 'BuildConfig.kt' in fn:
            return
    fields, seen = [], set()
    for m in re.finditer(r'(?m)^\s*buildConfigField\s*\(?\s*["\'](\w+)["\']\s*,\s*["\'](\w+)["\']\s*,\s*(.+?)\)?\s*$', MODG):
        typ, name, val = m.group(1), m.group(2), m.group(3).strip()
        if name in seen:
            continue
        seen.add(name)
        if val[0] == "'" and val[-1] == "'":
            val = val[1:-1]
        elif val[0] == '"' and val[-1] == '"':
            val = val[1:-1].replace('\\"', '"')
        fields.append('  public static final %s %s = %s;' % (typ, name, val))
    d = os.path.join(GEN, *RPKG.split('.'))
    os.makedirs(d, exist_ok=True)
    body = '\n'.join(['package %s;' % RPKG, '', 'public final class BuildConfig {',
                      '  public static final boolean DEBUG = false;',
                      '  public static final String APPLICATION_ID = "%s";' % APPID,
                      '  public static final String BUILD_TYPE = "release";',
                      '  public static final int VERSION_CODE = %s;' % (VC if VC.isdigit() else 1),
                      '  public static final String VERSION_NAME = "%s";' % VN] + fields + ['}', ''])
    open(os.path.join(d, 'BuildConfig.java'), 'w', encoding='utf-8').write(body)


if __name__ == '__main__':
    try:
        main()
    except Fail as e:
        print('WZH ERROR:', e)
        sys.exit(1)
PYEOF
mkdir -p "$W/o/deps"
DEPSOK=""
if command -v python3 >/dev/null 2>&1; then
  WZH_RES="$RES" python3 "$W/wzh_deps.py" "$MOD" "$APPID" "$RPKG" "$VC" "$VN" "$W" "$W/o/deps" "$W/o/AndroidManifest.xml" "$W/o/AndroidManifest.merged.xml" "$W/o/gen" >"$W/cmd.log" 2>&1 && DEPSOK=1
  sendlog "$W/cmd.log"
fi
if [ -z "$DEPSOK" ]; then
  [ -n "$HASDEPS" ] && fail "Fetching the project libraries failed (see the log). Needs internet and the python package in Termux."
  : >"$W/o/deps/jars.txt"; : >"$W/o/deps/cpjars.txt"; : >"$W/o/deps/libres.txt"; : >"$W/o/deps/assets.txt"; : >"$W/o/deps/pkgs.txt"; rm -f "$W/o/AndroidManifest.merged.xml"
fi
# the merger started from the already fixed-up app manifest (package= included), so just take its result
[ -s "$W/o/AndroidManifest.merged.xml" ] && cp "$W/o/AndroidManifest.merged.xml" "$W/o/AndroidManifest.xml"
DEPJARS=(); while IFS= read -r j; do [ -n "$j" ] && DEPJARS+=("$j"); done < "$W/o/deps/jars.txt"
CPJARS=(); while IFS= read -r j; do [ -n "$j" ] && CPJARS+=("$j"); done < "$W/o/deps/cpjars.txt"
LIBRES=(); while IFS= read -r j; do [ -n "$j" ] && LIBRES+=("$j"); done < "$W/o/deps/libres.txt"
LIBASSETS=(); while IFS= read -r j; do [ -n "$j" ] && LIBASSETS+=("$j"); done < "$W/o/deps/assets.txt"
EXTRAPKGS=$(head -1 "$W/o/deps/pkgs.txt" 2>/dev/null)

step "Compiling resources"
HASRES=""
if [ -d "$RES" ] && [ -n "$(ls -A "$RES" 2>/dev/null)" ]; then
  run "aapt2 compile" aapt2 compile --dir "$RES" -o "$W/o/res.zip"
  HASRES=1
fi
LINK=(aapt2 link -o "$W/o/base.apk" -I "$W/android.jar" --manifest "$W/o/AndroidManifest.xml"
      --java "$W/o/gen" --custom-package "$RPKG" --min-sdk-version "$MINSDK" --target-sdk-version "$TARGET"
      --version-code "$VC" --version-name "$VN" --auto-add-overlay)
[ "$APPID" != "$RPKG" ] && LINK+=(--rename-manifest-package "$APPID")
[ -n "$EXTRAPKGS" ] && LINK+=(--extra-packages "$EXTRAPKGS")
for a in "${LIBASSETS[@]}"; do LINK+=(-A "$a"); done
[ -d "$ASSETS" ] && LINK+=(-A "$ASSETS")
# Library resources must be overlays (-R): plain inputs may not define the same resource twice, but
# libraries do (e.g. the SearchView/Carousel styleables in both appcompat and Material). The base-most
# library is the plain input, every later one and the app's own resources are -R, so the app wins.
RESARGS=("${LIBRES[@]}")
[ -n "$HASRES" ] && RESARGS+=("$W/o/res.zip")
if [ ${#LIBRES[@]} -gt 0 ]; then
  LINK+=("${RESARGS[0]}")
  for r in "${RESARGS[@]:1}"; do LINK+=(-R "$r"); done
else
  [ -n "$HASRES" ] && LINK+=("$W/o/res.zip")
fi
run "aapt2 link" "${LINK[@]}"

find "$MAIN" -type d -name build -prune -o -type f -name '*.kt' -print > "$W/o/kt.raw"
find "$MAIN" -type d -name build -prune -o -type f -name '*.java' -print > "$W/o/java.raw"
find "$W/o/gen" -type f -name '*.java' >> "$W/o/java.raw"
[ -s "$W/o/kt.raw" ] || [ -s "$W/o/java.raw" ] || fail "No Kotlin or Java files found in the project"
sed 's/.*/"&"/' "$W/o/kt.raw" > "$W/o/kt.txt"
sed 's/.*/"&"/' "$W/o/java.raw" > "$W/o/java.txt"
cat "$W/o/kt.txt" "$W/o/java.txt" > "$W/o/all.txt"

JARS=()
while IFS= read -r j; do JARS+=("$j"); done < <(find "$MOD/libs" -name '*.jar' 2>/dev/null)
for j in "${DEPJARS[@]}"; do JARS+=("$j"); done
CP="$W/android.jar"
for j in "${JARS[@]}" "${CPJARS[@]}"; do CP="$CP:$j"; done

KSTD=""
if [ -s "$W/o/kt.raw" ]; then
  command -v kotlinc >/dev/null 2>&1 || fail "kotlinc is not installed in Termux. Open Set up on this phone and tap Install tools."
  # The Kotlin runtime must be packed into the app, or it crashes the moment it opens
  # (NoClassDefFoundError: kotlin/jvm/internal/Intrinsics). Take the jar that really holds that class
  # (never a jdk7/jdk8/sources shim, which `find | head -1` could pick), unless a library already brings it.
  KSTD=""
  for c in "$PREFIX/opt/kotlin/lib/kotlin-stdlib.jar" $(find "$PREFIX" -name 'kotlin-stdlib*.jar' ! -name '*sources*' 2>/dev/null); do
    [ -f "$c" ] && unzip -l "$c" 2>/dev/null | grep -q 'kotlin/jvm/internal/Intrinsics.class' && { KSTD="$c"; break; }
  done
  for j in "${JARS[@]}"; do
    unzip -l "$j" 2>/dev/null | grep -q 'kotlin/jvm/internal/Intrinsics.class' && { KSTD=""; break; }
  done
  if [ -z "$KSTD" ]; then
    HAVEK=""
    for j in "${JARS[@]}"; do unzip -l "$j" 2>/dev/null | grep -q 'kotlin/jvm/internal/Intrinsics.class' && HAVEK=1; done
    [ -z "$HAVEK" ] && fail "The Kotlin runtime (kotlin-stdlib.jar) was not found in Termux, so the app would crash when opened. Open Set up on this phone and run Install tools again."
  fi
  KJVM=1.8; [ ${#DEPJARS[@]} -gt 0 ] && KJVM=17
  step "Compiling Kotlin"
  run "Kotlin" kotlinc -J-Xmx1g -jvm-target "$KJVM" -cp "$CP" -d "$W/o/classes" @"$W/o/all.txt"
fi
if [ -s "$W/o/java.raw" ]; then
  step "Compiling Java"
  run "Java" javac -proc:none -source 8 -target 8 -Xlint:-options -encoding UTF-8 -cp "$W/o/classes:$CP" -d "$W/o/classes" @"$W/o/java.txt"
fi

step "Converting to dex"
run "jar" jar cf "$W/o/classes.jar" -C "$W/o/classes" .
if [ -n "$D8" ]; then
  run "d8" d8 --release --min-api "$MINSDK" --lib "$W/android.jar" --output "$W/o/dex" "$W/o/classes.jar" "${JARS[@]}" ${KSTD:+"$KSTD"}
else
  run "dx" dx --dex --min-sdk-version="$MINSDK" --output="$W/o/dex/classes.dex" "$W/o/classes.jar" "${JARS[@]}" ${KSTD:+"$KSTD"}
fi
ls "$W/o/dex"/classes*.dex >/dev/null 2>&1 || fail "The dex step made no classes.dex"

step "Packaging"
( cd "$W/o/dex" && zip -q "$W/o/base.apk" classes*.dex ) || fail "could not add the code to the app"
if [ -n "$(ls -A "$W/o/deps/jres" 2>/dev/null)" ]; then
  ( cd "$W/o/deps/jres" && zip -qr "$W/o/base.apk" . ) || fail "could not add the library files to the app"
fi
mkdir -p "$W/o/stage/lib"
[ -d "$JNI" ] && cp -r "$JNI"/. "$W/o/stage/lib/"
[ -d "$W/o/deps/jnistage/lib" ] && cp -r "$W/o/deps/jnistage/lib"/. "$W/o/stage/lib/"
if [ -n "$(ls -A "$W/o/stage/lib" 2>/dev/null)" ]; then
  ( cd "$W/o/stage" && zip -qr "$W/o/base.apk" lib ) || fail "could not add the native libraries"
fi

# Crash-on-open guard: the screen the phone starts must really be inside the finished app.
MFT="$W/o/AndroidManifest.xml"
LAUNCH=$(tr '\n' ' ' < "$MFT" | sed 's/<activity/\n<activity/g' | grep -v '^<activity-alias' | grep 'android.intent.action.MAIN' | grep -oE 'android:name="[^"]+"' | head -1 | cut -d'"' -f2)
if [ -n "$LAUNCH" ]; then
  case "$LAUNCH" in .*) LAUNCH="$RPKG$LAUNCH";; *.*) ;; *) LAUNCH="$RPKG.$LAUNCH";; esac
  LCLS="L$(echo "$LAUNCH" | tr . /);"
  if ! cat "$W/o/dex"/classes*.dex | grep -qaF "$LCLS"; then
    fail "The app would crash when opened: its start screen $LAUNCH is in the manifest but was not built into the app. Check the class name and its package (namespace / package= in the manifest)."
  fi
fi

curl -sf -m 600 -X PUT -T "$W/o/base.apk" "$B/out.apk" >/dev/null 2>&1 || fail "could not send the finished app back"
post status ok
rm -rf "$W/p" "$W/o" "$W/project.zip"
}

# Driver: run the build; when a code/resource step fails, let the Build Doctor read the failing log, repair the
# project the way the server does, and build again (at most 3 repairs). Anything else is reported as is.
TRIES=0; FIRST=1
while :; do
  rm -f "$W/lastfail" "$W/lastrun"
  ( wzh_body )
  [ $? -eq 0 ] && exit 0
  RUN=$(cat "$W/lastrun" 2>/dev/null)
  TRIES=$((TRIES+1))
  case "$RUN" in
    Kotlin|Java|"aapt2 link")
      if [ "$TRIES" -le 3 ] && doctor_run "$W/cmd.log"; then
        FIRST=""
        step "Fixing the project, then building again"
        continue
      fi ;;
  esac
  post status "fail: $(cat "$W/lastfail" 2>/dev/null || echo 'The build failed')"
  exit 1
done
