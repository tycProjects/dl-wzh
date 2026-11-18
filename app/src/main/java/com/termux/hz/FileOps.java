package com.termux.hz;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** File-system helpers shared by the UI and the agent tools. */
public final class FileOps {
    private FileOps() {}

    public static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList(
            ".git", ".gradle", "build", "node_modules", ".idea", "run", "out", "target", ".settings", "bin"));

    public static String readText(File f, int maxBytes) throws IOException {
        long len = f.length();
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.min(len, maxBytes));
            byte[] b = new byte[16384];
            int n, total = 0;
            while ((n = in.read(b)) > 0 && total < maxBytes) {
                out.write(b, 0, Math.min(n, maxBytes - total));
                total += n;
            }
            return out.toString("UTF-8");
        }
    }

    public static void writeText(File f, String text) throws IOException {
        File p = f.getParentFile();
        if (p != null && !p.exists()) p.mkdirs();
        try (OutputStream o = new FileOutputStream(f)) { o.write(text.getBytes("UTF-8")); }
    }

    public static boolean isBinary(File f) {
        try (InputStream in = new FileInputStream(f)) {
            byte[] b = new byte[2048];
            int n = in.read(b);
            for (int i = 0; i < n; i++) if (b[i] == 0) return true;
        } catch (IOException e) { return true; }
        return false;
    }

    public static void deleteTree(File f) {
        if (f.isDirectory() && !isSymlink(f)) {
            File[] c = f.listFiles();
            if (c != null) for (File x : c) deleteTree(x);
        }
        f.delete();
    }

    private static boolean isSymlink(File f) {
        try { return !f.getCanonicalFile().equals(f.getAbsoluteFile()); } catch (IOException e) { return false; }
    }

    public static void copyTree(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            dst.mkdirs();
            File[] c = src.listFiles();
            if (c != null) for (File x : c) copyTree(x, new File(dst, x.getName()));
        } else {
            copy(new FileInputStream(src), dst);
        }
    }

    public static void copy(InputStream in, File dst) throws IOException {
        File p = dst.getParentFile();
        if (p != null) p.mkdirs();
        try (InputStream i = in; OutputStream o = new FileOutputStream(dst)) {
            byte[] b = new byte[32768];
            int n;
            while ((n = i.read(b)) > 0) o.write(b, 0, n);
        }
    }

    /** Extracts a zip into dest. If everything sits in a single top folder, that folder is flattened. */
    public static void unzip(InputStream in, File dest) throws IOException {
        File tmp = new File(dest.getParentFile(), ".hz-unzip-" + System.nanoTime());
        tmp.mkdirs();
        String canonicalTmp = tmp.getCanonicalPath() + File.separator;
        try (ZipInputStream z = new ZipInputStream(new BufferedInputStream(in))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                File out = new File(tmp, e.getName());
                if (!out.getCanonicalPath().startsWith(canonicalTmp)) continue; // zip-slip guard
                if (e.isDirectory()) out.mkdirs();
                else {
                    out.getParentFile().mkdirs();
                    try (OutputStream o = new FileOutputStream(out)) {
                        byte[] b = new byte[32768];
                        int n;
                        while ((n = z.read(b)) > 0) o.write(b, 0, n);
                    }
                }
            }
        }
        File[] top = tmp.listFiles();
        File root = tmp;
        if (top != null && top.length == 1 && top[0].isDirectory()) root = top[0];
        if (dest.exists()) deleteTree(dest);
        if (!root.renameTo(dest)) { copyTree(root, dest); }
        deleteTree(tmp);
    }

    public static void zip(File dir, File out) throws IOException {
        try (ZipOutputStream z = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out)))) {
            addZip(z, dir, "");
        }
    }

    private static void addZip(ZipOutputStream z, File f, String prefix) throws IOException {
        File[] cs = f.listFiles();
        if (cs == null) return;
        for (File c : cs) {
            if (SKIP_DIRS.contains(c.getName()) && c.isDirectory()) continue;
            String name = prefix + c.getName();
            if (c.isDirectory()) addZip(z, c, name + "/");
            else {
                z.putNextEntry(new ZipEntry(name));
                try (InputStream i = new FileInputStream(c)) {
                    byte[] b = new byte[32768];
                    int n;
                    while ((n = i.read(b)) > 0) z.write(b, 0, n);
                }
                z.closeEntry();
            }
        }
    }

    /** Sorted listing: folders first. */
    public static List<File> list(File dir) {
        File[] fs = dir.listFiles();
        List<File> out = new ArrayList<>();
        if (fs != null) out.addAll(Arrays.asList(fs));
        Collections.sort(out, (a, b) -> {
            if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return out;
    }

    public static String rel(File root, File f) {
        String r = root.getAbsolutePath(), p = f.getAbsolutePath();
        return p.startsWith(r) ? p.substring(r.length()).replaceFirst("^/", "") : p;
    }

    public static final class Hit {
        public final File file; public final int line; public final String text;
        Hit(File f, int l, String t) { file = f; line = l; text = t; }
    }

    public static List<Hit> grep(File root, String query, boolean regex, boolean matchCase, int max) {
        List<Hit> out = new ArrayList<>();
        Pattern p;
        try {
            int flags = matchCase ? 0 : Pattern.CASE_INSENSITIVE;
            p = Pattern.compile(regex ? query : Pattern.quote(query), flags);
        } catch (Exception e) { return out; }
        grepWalk(root, p, out, max);
        return out;
    }

    private static void grepWalk(File f, Pattern p, List<Hit> out, int max) {
        if (out.size() >= max) return;
        if (f.isDirectory()) {
            if (SKIP_DIRS.contains(f.getName()) && f.getParentFile() != null && !f.getName().equals("bin")) return;
            File[] cs = f.listFiles();
            if (cs != null) for (File c : cs) grepWalk(c, p, out, max);
            return;
        }
        if (f.length() > 1_000_000 || isBinary(f)) return;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            String line;
            int n = 0;
            while ((line = r.readLine()) != null && out.size() < max) {
                n++;
                if (p.matcher(line).find()) out.add(new Hit(f, n, line.length() > 200 ? line.substring(0, 200) : line));
            }
        } catch (IOException ignored) {}
    }

    public static String tree(File root, int maxDepth, int maxEntries) {
        StringBuilder sb = new StringBuilder();
        int[] count = {0};
        treeWalk(root, "", 0, maxDepth, maxEntries, sb, count);
        if (count[0] >= maxEntries) sb.append("… (more entries not shown)\n");
        return sb.toString();
    }

    private static void treeWalk(File dir, String indent, int depth, int maxDepth, int maxEntries, StringBuilder sb, int[] count) {
        if (depth > maxDepth) return;
        for (File f : list(dir)) {
            if (count[0] >= maxEntries) return;
            if (f.isDirectory() && SKIP_DIRS.contains(f.getName())) {
                sb.append(indent).append(f.getName()).append("/ (skipped)\n");
                continue;
            }
            count[0]++;
            sb.append(indent).append(f.getName()).append(f.isDirectory() ? "/" : "").append('\n');
            if (f.isDirectory()) treeWalk(f, indent + "  ", depth + 1, maxDepth, maxEntries, sb, count);
        }
    }

    /** Resolves a user/agent supplied relative path and makes sure it stays inside root. */
    public static File safe(File root, String path) throws IOException {
        File f = new File(path).isAbsolute() ? new File(path) : new File(root, path);
        String rc = root.getCanonicalPath(), fc = f.getCanonicalPath();
        if (!fc.equals(rc) && !fc.startsWith(rc + File.separator))
            throw new IOException("Path is outside the project: " + path);
        return f;
    }
}
