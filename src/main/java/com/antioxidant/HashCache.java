package com.antioxidant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Properties;

public final class HashCache {
    private final Path cacheFile;
    private final Properties hashes = new Properties();

    public HashCache(Path cacheFile) {
        this.cacheFile = cacheFile;
        if (Files.isRegularFile(cacheFile)) {
            try (var input = Files.newInputStream(cacheFile)) {
                hashes.load(input);
            } catch (IOException ignored) {
            }
        }
    }

    public boolean writeIfChanged(String key, Path target, byte[] content) throws IOException {
        String hash = sha256(content);
        if (hash.equals(hashes.getProperty(key)) && Files.isRegularFile(target)) return false;
        Files.createDirectories(target.getParent());
        Files.write(target, content);
        hashes.setProperty(key, hash);
        return true;
    }

    public void save() throws IOException {
        Files.createDirectories(cacheFile.getParent());
        try (var output = Files.newOutputStream(cacheFile)) {
            hashes.store(output, "Antioxidant SHA-256 output cache");
        }
    }

    public static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}