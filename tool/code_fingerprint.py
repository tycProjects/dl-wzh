#!/usr/bin/env python3
"""Prints the code fingerprint of an APK, the same value the app sends to the build server.

Usage:  python3 tool/code_fingerprint.py path/to/app-release.apk

Put the value into TYCEPT_CODE_SHA256 on Render (several values can be separated with commas,
for example the current release and the one before it).

How it is made (lib/app_proof.dart does exactly the same on the phone): for every file inside the
APK called classes*.dex, AndroidManifest.xml, resources.arsc, lib/<abi>/libapp.so or
lib/<abi>/libflutter.so, take the SHA-256 of its real (unzipped) bytes. Sort the files by name,
write one line "name:sha256\n" per file, and take the SHA-256 of all those lines together.
"""
import hashlib, re, struct, sys, zlib

WANTED = re.compile(r'^(classes[0-9]*\.dex|AndroidManifest\.xml|resources\.arsc|lib/[^/]+/lib(app|flutter)\.so)$')


def entry_digest(f, method, comp_size, off):
    f.seek(off)
    hdr = f.read(30)
    if len(hdr) < 30 or struct.unpack('<I', hdr[:4])[0] != 0x04034B50:
        raise ValueError('bad local header')
    n, e = struct.unpack('<HH', hdr[26:30])
    f.seek(off + 30 + n + e)
    h = hashlib.sha256()
    left = comp_size
    d = zlib.decompressobj(-15) if method == 8 else None
    if method not in (0, 8):
        raise ValueError('unsupported compression method %d' % method)
    while left > 0:
        chunk = f.read(min(65536, left))
        if not chunk:
            raise ValueError('truncated entry')
        left -= len(chunk)
        h.update(d.decompress(chunk) if d else chunk)
    if d:
        h.update(d.flush())
    return h.hexdigest()


def fingerprint(path):
    with open(path, 'rb') as f:
        f.seek(0, 2)
        size = f.tell()
        tail_len = min(size, 66000)
        f.seek(size - tail_len)
        tail = f.read(tail_len)
        i = tail.rfind(b'PK\x05\x06')
        if i < 0:
            raise ValueError('not a zip/apk')
        cd_size, cd_off = struct.unpack('<II', tail[i + 12:i + 20])
        f.seek(cd_off)
        cd = f.read(cd_size)
        pos, found = 0, {}
        while pos + 46 <= len(cd) and struct.unpack('<I', cd[pos:pos + 4])[0] == 0x02014B50:
            method = struct.unpack('<H', cd[pos + 10:pos + 12])[0]
            comp_size = struct.unpack('<I', cd[pos + 20:pos + 24])[0]
            nlen, elen, clen = struct.unpack('<HHH', cd[pos + 28:pos + 34])
            off = struct.unpack('<I', cd[pos + 42:pos + 46])[0]
            name = cd[pos + 46:pos + 46 + nlen].decode('utf-8', 'replace')
            if WANTED.match(name):
                found[name] = (method, comp_size, off)
            pos += 46 + nlen + elen + clen
        if 'classes.dex' not in found or 'AndroidManifest.xml' not in found:
            raise ValueError('classes.dex or AndroidManifest.xml missing')
        lines = ''.join('%s:%s\n' % (n, entry_digest(f, *found[n])) for n in sorted(found))
        return hashlib.sha256(lines.encode('utf-8')).hexdigest(), sorted(found)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    fp, names = fingerprint(sys.argv[1])
    print(fp)
    print('(%d files: %s)' % (len(names), ', '.join(names)), file=sys.stderr)
