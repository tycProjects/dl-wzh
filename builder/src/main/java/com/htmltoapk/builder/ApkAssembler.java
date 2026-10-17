package com.htmltoapk.builder;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Rebuilds the template APK with a few entries replaced and the website added.
 * resources.arsc is written uncompressed and 4-byte aligned (required for targetSdk 30+),
 * which is what zipalign would normally do. The result still has to be signed.
 */
public final class ApkAssembler {
    private static final Pattern ICON = Pattern.compile("res/drawable-nodpi[^/]*/ic_launcher\\.png");

    private ApkAssembler() {}

    /** Name of the icon PNG inside the compiled template, or null. */
    public static String findIconEntry(File templateApk) throws IOException {
        try (ZipFile zf = new ZipFile(templateApk)) {
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                String n = en.nextElement().getName();
                if (ICON.matcher(n).matches()) return n;
            }
        }
        return null;
    }

    public static byte[] readEntry(File templateApk, String name) throws IOException {
        try (ZipFile zf = new ZipFile(templateApk)) {
            ZipEntry e = zf.getEntry(name);
            if (e == null) throw new IOException("Template is missing " + name);
            try (InputStream in = zf.getInputStream(e)) {
                return readAll(in);
            }
        }
    }

    /**
     * @param replace entries of the template to swap (by exact name)
     * @param add     extra files to add (e.g. assets/config.json)
     * @param wwwDir  website folder, added as assets/www/...
     */
    public static void assemble(File templateApk, File outApk, Map<String, byte[]> replace,
                                Map<String, byte[]> add, File wwwDir) throws IOException {
        try (ZipFile zf = new ZipFile(templateApk);
             CountingStream counter = new CountingStream(Files.newOutputStream(outApk.toPath()));
             ZipOutputStream zos = new ZipOutputStream(counter)) {

            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String name = e.getName();
                if (e.isDirectory() || name.toUpperCase().startsWith("META-INF/")) continue;
                byte[] data = replace.get(name);
                if (data == null) {
                    try (InputStream in = zf.getInputStream(e)) {
                        data = readAll(in);
                    }
                }
                writeBytes(zos, counter, name, data, name.equals("resources.arsc"));
            }
            for (Map.Entry<String, byte[]> a : add.entrySet()) {
                writeBytes(zos, counter, a.getKey(), a.getValue(), false);
            }
            if (wwwDir != null) {
                List<File> files = new ArrayList<>();
                collect(wwwDir, files);
                Collections.sort(files);
                String base = wwwDir.getAbsolutePath();
                for (File f : files) {
                    String rel = f.getAbsolutePath().substring(base.length()).replace(File.separatorChar, '/');
                    if (rel.startsWith("/")) rel = rel.substring(1);
                    zos.closeEntry();
                    zos.putNextEntry(new ZipEntry("assets/www/" + rel));
                    Files.copy(f.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }
    }

    private static void collect(File dir, List<File> out) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        for (File k : kids) {
            if (k.isDirectory()) collect(k, out);
            else out.add(k);
        }
    }

    private static void writeBytes(ZipOutputStream zos, CountingStream counter, String name,
                                   byte[] data, boolean stored) throws IOException {
        zos.closeEntry(); // so counter.count is the offset of the next local header
        ZipEntry e = new ZipEntry(name);
        if (stored) {
            e.setMethod(ZipEntry.STORED);
            e.setSize(data.length);
            e.setCompressedSize(data.length);
            CRC32 crc = new CRC32();
            crc.update(data);
            e.setCrc(crc.getValue());
            int nameLen = name.getBytes(StandardCharsets.UTF_8).length;
            int pad = (int) ((4 - ((counter.count + 30 + nameLen) % 4)) % 4);
            if (pad != 0) {
                byte[] extra = new byte[4 + pad]; // unknown extra field: id 0xD935, size = pad
                extra[0] = 0x35;
                extra[1] = (byte) 0xD9;
                extra[2] = (byte) pad;
                e.setExtra(extra);
            }
        } else {
            e.setMethod(ZipEntry.DEFLATED);
        }
        zos.putNextEntry(e);
        zos.write(data);
        zos.closeEntry();
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    private static final class CountingStream extends FilterOutputStream {
        long count;

        CountingStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            count += len;
        }
    }
}
