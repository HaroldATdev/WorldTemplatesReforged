package com.trilcera.worldtemplates.client;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Installs the embedded Trilcera template world into the game directory.
 * Logic based on LonKraft's TemplateInstaller:
 * - Unzips an embedded template zip into trilcera-template/
 * - Works through a .installing temp directory with zip-slip protection
 * - Validates that level.dat exists after extraction
 */
public final class TemplateInstaller {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Final template directory inside the game folder. */
    public static final String TEMPLATE_DIRECTORY = "trilcera-template";
    /** Temp directory used while unzipping. */
    public static final String INSTALLING_DIRECTORY = "trilcera-template-installing";
    /** Embedded template zip inside our mod jar. */
    public static final String RESOURCE = "/trilcera-template.zip";

    private TemplateInstaller() {
    }

    /**
     * Ensures trilcera-template/ exists in the game directory.
     * If it already exists, this is a no-op (like LonKraft).
     */
    public static void ensureInstalled(Path gameDir) throws IOException {
        Path target = gameDir.resolve(TEMPLATE_DIRECTORY);
        if (Files.isDirectory(target)) {
            return;
        }
        if (Files.exists(target)) {
            throw new IOException(TEMPLATE_DIRECTORY + " already exists but is not a folder");
        }

        Path staging = gameDir.resolve(INSTALLING_DIRECTORY);
        deleteRecursively(staging);
        Files.createDirectories(staging);

        try (InputStream in = TemplateInstaller.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IOException("Embedded template resource " + RESOURCE + " not found");
            }
            try (ZipInputStream zip = new ZipInputStream(in)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name == null || name.isEmpty() || name.equals("/")) {
                        zip.closeEntry();
                        continue;
                    }
                    Path resolved = staging.resolve(name).normalize();
                    // Zip-slip protection (same as LonKraft)
                    if (!resolved.startsWith(staging)) {
                        throw new IOException("Blocked unsafe zip entry: " + name);
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(resolved);
                    } else {
                        Path parent = resolved.getParent();
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }
                        Files.copy(zip, resolved, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zip.closeEntry();
                }
            }
        }

        // LonKraft validation: the extracted template must contain level.dat
        // (at root or one folder deep, since some zips wrap everything in a folder)
        Path effectiveRoot = findTemplateRoot(staging);
        if (effectiveRoot == null) {
            deleteRecursively(staging);
            throw new IOException(
                    "The embedded Trilcera template does not contain level.dat");
        }

        // Move staging to the final directory
        Files.move(staging, target);
        LOGGER.info("[Trilcera Templates] Template installed from '{}' into '{}'",
                effectiveRoot.getFileName() == null ? "zip root" : effectiveRoot.getFileName(),
                target);
    }

    /**
     * Finds the directory that actually contains level.dat:
     * the staging root itself, or (LonKraft-style single-root zips) the only
     * top-level folder inside it.
     *
     * @return the effective template root, or null if level.dat is missing
     */
    private static Path findTemplateRoot(Path staging) throws IOException {
        if (Files.isRegularFile(staging.resolve("level.dat"))) {
            return staging;
        }
        try (var stream = Files.list(staging)) {
            for (Path child : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(child) && Files.isRegularFile(child.resolve("level.dat"))) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Resolves the effective template directory for cloning:
     * trilcera-template/ itself, or its single inner folder if the zip
     * wrapped the world in one top-level folder.
     */
    public static Path resolveTemplateRoot(Path gameDir) throws IOException {
        Path target = gameDir.resolve(TEMPLATE_DIRECTORY);
        if (!Files.isDirectory(target) || !Files.isRegularFile(target.resolve("level.dat"))) {
            Path nested = findTemplateRoot(target);
            if (nested != null) {
                return nested;
            }
            throw new IOException("The " + TEMPLATE_DIRECTORY
                    + " folder is missing or does not contain level.dat");
        }
        return target;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            for (Path p : (Iterable<Path>) stream.sorted(java.util.Comparator.reverseOrder())::iterator) {
                Files.deleteIfExists(p);
            }
        }
    }
}
