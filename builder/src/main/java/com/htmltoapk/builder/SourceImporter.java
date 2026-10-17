package com.htmltoapk.builder;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Copies a picked HTML file, ZIP or folder into a private working folder. */
public final class SourceImporter {
    private SourceImporter() {}

    public static final class Result {
        public final File root;
        public final int files;
        public final long bytes;

        Result(File root, int files, long bytes) {
            this.root = root;
            this.files = files;
            this.bytes = bytes;
        }
    }

    private static File freshDir(Context ctx) throws IOException {
        File dir = new File(ctx.getFilesDir(), "site");
        ApkBuilder.deleteRecursively(dir);
        if (!dir.mkdirs()) throw new IOException("Cannot create site folder");
        return dir;
    }

    public static String displayName(Context ctx, Uri uri) {
        try (Cursor c = ctx.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {
        }
        return "file";
    }

    public static Result importHtmlFile(Context ctx, Uri uri) throws IOException {
        File dir = freshDir(ctx);
        File out = new File(dir, "index.html");
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open file");
            ApkBuilder.copy(in, new FileOutputStream(out));
        }
        return new Result(dir, 1, out.length());
    }

    public static Result importZip(Context ctx, Uri uri) throws IOException {
        File dir = freshDir(ctx);
        String base = dir.getCanonicalPath() + File.separator;
        int count = 0;
        long total = 0;
        try (InputStream raw = ctx.getContentResolver().openInputStream(uri)) {
            if (raw == null) throw new IOException("Cannot open ZIP");
            ZipInputStream zin = new ZipInputStream(raw);
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                String name = e.getName().replace('\\', '/');
                if (e.isDirectory() || name.startsWith("__MACOSX/") || name.endsWith(".DS_Store")) continue;
                File target = new File(dir, name);
                // zip-slip guard
                if (!target.getCanonicalPath().startsWith(base)) continue;
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("mkdir failed");
                try (FileOutputStream fos = new FileOutputStream(target)) {
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = zin.read(buf)) > 0) {
                        fos.write(buf, 0, n);
                        total += n;
                    }
                }
                count++;
            }
        }
        return new Result(findSiteRoot(dir), count, total);
    }

    public static Result importTree(Context ctx, Uri treeUri) throws IOException {
        File dir = freshDir(ctx);
        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        long[] stats = new long[2];
        copyTree(ctx.getContentResolver(), treeUri, docId, dir, stats, 0);
        return new Result(findSiteRoot(dir), (int) stats[0], stats[1]);
    }

    private static void copyTree(ContentResolver cr, Uri tree, String docId, File dest, long[] stats, int depth)
            throws IOException {
        if (depth > 20) return;
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId);
        try (Cursor c = cr.query(children, new String[]{
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
            if (c == null) return;
            while (c.moveToNext()) {
                String id = c.getString(0);
                String name = c.getString(1).replace('/', '_');
                String mime = c.getString(2);
                File target = new File(dest, name);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    if (!target.exists() && !target.mkdirs()) throw new IOException("mkdir failed");
                    copyTree(cr, tree, id, target, stats, depth + 1);
                } else {
                    Uri doc = DocumentsContract.buildDocumentUriUsingTree(tree, id);
                    try (InputStream in = cr.openInputStream(doc)) {
                        if (in == null) continue;
                        ApkBuilder.copy(in, new FileOutputStream(target));
                    }
                    stats[0]++;
                    stats[1] += target.length();
                }
            }
        }
    }

    /** If index.html is not at the top but inside a single wrapper folder, use that folder. */
    private static File findSiteRoot(File dir) {
        File cur = dir;
        for (int i = 0; i < 5; i++) {
            if (new File(cur, "index.html").isFile()) return cur;
            File[] kids = cur.listFiles();
            if (kids == null || kids.length != 1 || !kids[0].isDirectory()) break;
            cur = kids[0];
        }
        return dir;
    }

    public static boolean hasIndex(File root) {
        return root != null && new File(root, "index.html").isFile();
    }
}
