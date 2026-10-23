import 'dart:convert';
import 'package:archive/archive.dart';

/// Packs one file into a zip, the same way the HTML app does before uploading.
List<int> zipSingle(String name, List<int> bytes) {
  final a = Archive()..addFile(ArchiveFile(name, bytes.length, bytes));
  return ZipEncoder().encode(a)!;
}

/// Tiny index.html that shows three dots, then opens the live site (website-link builds).
List<int> websiteZip(String url) {
  final esc = url.replaceAll('&', '&amp;').replaceAll('"', '&quot;');
  final html = '''<!DOCTYPE html>
<html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
html,body{margin:0;height:100%;background:#111111;}
@media (prefers-color-scheme:light){html,body{background:#f6f6f4;}.d{background:#1a1a1a!important;}}
#ld{position:fixed;inset:0;display:flex;align-items:center;justify-content:center;gap:9px;}
.d{width:11px;height:11px;border-radius:50%;background:#f2f2ee;animation:b 1s ease-in-out infinite;}
.d:nth-child(2){animation-delay:.15s}.d:nth-child(3){animation-delay:.3s}
@keyframes b{0%,80%,100%{transform:translateY(0);opacity:.45}40%{transform:translateY(-11px);opacity:1}}
</style>
</head><body>
<div id="ld" role="status" aria-label="Loading"><span class="d"></span><span class="d"></span><span class="d"></span></div>
<script>setTimeout(function(){location.replace("$esc");},60);</script>
</body></html>''';
  return zipSingle('index.html', utf8.encode(html));
}
