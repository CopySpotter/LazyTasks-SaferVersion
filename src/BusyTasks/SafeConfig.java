// BusyTasks Safe modification: local settings without credentials or Java deserialization.
// Based on BusyTasks, Copyright (C) 2025 Bernhard Mayr, GPL-3.0-or-later.
package BusyTasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;

public final class SafeConfig {
    private SafeConfig() { }
    public static Path path() {
        return Path.of(System.getProperty("user.home"), ".config", "busytasks-safe", "settings.properties");
    }

    public static Properties load() throws IOException {
        Path target = path();
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new java.nio.file.NoSuchFileException(target.toString());
        if (Files.isSymbolicLink(target) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Unsafe configuration path (symlink or non-regular file)");
        if (Files.size(target) > 65536) throw new IOException("Configuration file too large");
        Properties props = new Properties();
        try (InputStream stream = Files.newInputStream(target, LinkOption.NOFOLLOW_LINKS)) {
            props.load(stream);
        }
        return props;
    }

    public static int integer(Properties p, String key, int defaultValue, int min, int max) {
        try {
            int result = Integer.parseInt(p.getProperty(key, ""));
            return result >= min && result <= max ? result : defaultValue;
        } catch (NumberFormatException ex) { return defaultValue; }
    }

    public static void save(Properties p) throws IOException {
        Path target = path();
        Path directory = target.getParent();
        Path userConfigDir = directory.getParent();
        if (Files.isSymbolicLink(userConfigDir) || Files.isSymbolicLink(directory) || Files.isSymbolicLink(target))
            throw new IOException("Refusing symlink in configuration path");
        if (!Files.exists(userConfigDir)) Files.createDirectory(userConfigDir);
        if (!Files.exists(directory)) Files.createDirectory(directory);
        Set<PosixFilePermission> dirPerms = PosixFilePermissions.fromString("rwx------");
        Set<PosixFilePermission> filePerms = PosixFilePermissions.fromString("rw-------");
        // Mint/Linux has POSIX permissions. Keep the folder private even on an existing install.
        Files.setPosixFilePermissions(directory, dirPerms);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Refusing non-file configuration destination");
        Path temp = Files.createTempFile(directory, ".settings-", ".tmp",
                PosixFilePermissions.asFileAttribute(filePerms));
        try {
            try (OutputStream stream = Files.newOutputStream(temp, StandardOpenOption.TRUNCATE_EXISTING)) {
                p.store(stream, "BusyTasks Safe settings (no passwords)");
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Files.setPosixFilePermissions(target, filePerms);
        } finally { Files.deleteIfExists(temp); }
    }
}
