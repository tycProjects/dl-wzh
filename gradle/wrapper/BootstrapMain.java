/*
 * Everline distribution bootstrap. This is NOT Gradle's official wrapper jar.
 * It downloads the pinned Gradle distribution over HTTPS, unpacks it once,
 * then invokes its gradle executable. Replace with the official wrapper using
 * `gradle wrapper --gradle-version 8.9` after the first successful sync.
 * This source is included so the bootstrap is fully inspectable.
 */
package org.gradle.wrapper;

import java.io.*;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class BootstrapMain {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path config = root.resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(config)) { props.load(in); }
        String address = props.getProperty("distributionUrl");
        URI uri = URI.create(address);
        if (!"https".equals(uri.getScheme()) || !"services.gradle.org".equals(uri.getHost()) ||
                !address.endsWith("/gradle-8.9-bin.zip")) {
            throw new SecurityException("Unexpected Gradle distribution URL");
        }
        Path cache = Paths.get(System.getProperty("user.home"), ".gradle", "wrapper", "dists", "everline-gradle-8.9");
        Files.createDirectories(cache);
        Path executable = cache.resolve("gradle-8.9/bin/" + (isWindows() ? "gradle.bat" : "gradle"));
        try (FileChannel channel = FileChannel.open(cache.resolve(".download-lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {
            if (!Files.isRegularFile(executable)) {
                Path zip = cache.resolve("gradle-8.9-bin.zip.part");
                System.out.println("Downloading pinned Gradle 8.9 from " + uri);
                var connection = uri.toURL().openConnection();
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(60000);
                try (InputStream in = connection.getInputStream()) {
                    Files.copy(in, zip, StandardCopyOption.REPLACE_EXISTING);
                }
                try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
                    ZipEntry entry;
                    while ((entry = in.getNextEntry()) != null) {
                        Path dest = cache.resolve(entry.getName()).normalize();
                        if (!dest.startsWith(cache)) throw new SecurityException("Zip path escape");
                        if (entry.isDirectory()) Files.createDirectories(dest);
                        else {
                            Files.createDirectories(dest.getParent());
                            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                        in.closeEntry();
                    }
                }
                Files.deleteIfExists(zip);
                if (!Files.isRegularFile(executable)) throw new IOException("Gradle distribution incomplete");
                executable.toFile().setExecutable(true);
            }
        }
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(Arrays.asList(args));
        int result = new ProcessBuilder(command).directory(root.toFile()).inheritIO().start().waitFor();
        System.exit(result);
    }
    private static boolean isWindows() { return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"); }
}
