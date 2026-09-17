package com.trilcera.worldtemplates.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Clones the installed Trilcera template into a fresh world under saves/.
 * Logic based on LonKraft's WorldTemplateCloner.
 */
public final class WorldTemplateCloner {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Marker file written into each generated world (LonKraft parity). */
    public static final String GENERATED_MARKER = "trilcera-template.generated";

    private static final SecureRandom SEED_RANDOM = new SecureRandom();
    private static long lastGeneratedSeed;

    private WorldTemplateCloner() {
    }

    /**
     * Clones the template into saves/ and returns the created world folder name.
     * Builds into a '.name.creating' staging folder and renames at the end,
     * so a failure NEVER leaves a half-created world behind (LonKraft parity
     * plus cleanup: no more accumulating "Trilcera", "Trilcera 2", ...).
     *
     * @param gameDir  the game directory (contains saves/ and trilcera-template/)
     * @param baseName base world name, e.g. "Trilcera"
     * @return the created world folder name
     */
    public static String createFreshWorld(Path gameDir, String baseName, TemplateGameMode gameMode) throws IOException {
        Path templateRoot = TemplateInstaller.resolveTemplateRoot(gameDir);

        Path savesDir = gameDir.resolve("saves");
        Files.createDirectories(savesDir);

        String worldId = findAvailableWorldId(savesDir, baseName);
        Path destination = savesDir.resolve(worldId);
        Path staging = savesDir.resolve("." + worldId + ".creating");

        // Clean leftovers from a previous interrupted attempt
        deleteRecursively(staging);

        try {
            copyOverworldOnly(templateRoot, staging);

            long seed = generateFreshSeed();

            deleteRecursively(staging.resolve("playerdata"));
            deleteRecursively(staging.resolve("advancements"));
            deleteRecursively(staging.resolve("stats"));

            stripPlayerFromLevelDat(staging.resolve("level.dat"), seed, gameMode);
            Path oldDat = staging.resolve("level.dat_old");
            if (Files.isRegularFile(oldDat)) {
                stripPlayerFromLevelDat(oldDat, seed, gameMode);
            }

            Files.writeString(staging.resolve(GENERATED_MARKER), Long.toString(seed),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Guarantee the world's icon.png AS 64x64 (vanilla requirement: the
            // world list throws "Icon must be 64x64" for any other size). Vanilla
            // also keeps the icon permanently once the file exists
            // (GameRenderer.hasWorldScreenshot = true).
            Path templateIcon = templateRoot.resolve("icon.png");
            Path worldIcon = staging.resolve("icon.png");
            if (Files.isRegularFile(templateIcon)) {
                try {
                    writeWorldIcon(templateIcon, worldIcon);
                    LOGGER.info("[Trilcera Templates] Applied template icon.png (64x64) to world '{}'", worldId);
                } catch (Exception iconErr) {
                    // Non-fatal: log and fall back to plain copy (world list will
                    // reject an odd size and vanilla generates a screenshot icon).
                    LOGGER.warn("[Trilcera Templates] Could not resize world icon: {} - copying original", iconErr.getMessage());
                    Files.copy(templateIcon, worldIcon, StandardCopyOption.REPLACE_EXISTING);
                }
            } else if (!Files.isRegularFile(worldIcon)) {
                LOGGER.warn("[Trilcera Templates] Template has no icon.png at {} - vanilla will generate one", templateIcon);
            }

            // Final publish: staging -> saves/<worldId>
            deleteRecursively(destination);
            Files.move(staging, destination);

            LOGGER.info("[Trilcera Templates] Created world '{}' from template with seed {}",
                    worldId, seed);
            return worldId;
        } catch (IOException | RuntimeException e) {
            // Never leave a broken world behind
            try {
                deleteRecursively(staging);
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    private static synchronized long generateFreshSeed() {
        long seed = SEED_RANDOM.nextLong();
        while (seed == 0L || seed == lastGeneratedSeed) {
            seed = SEED_RANDOM.nextLong();
        }
        lastGeneratedSeed = seed;
        return seed;
    }

    private static String findAvailableWorldId(Path savesDir, String baseName) {
        if (baseName == null || baseName.isBlank()) {
            baseName = "Trilcera";
        }
        if (!Files.exists(savesDir.resolve(baseName))) {
            return baseName;
        }
        int counter = 2;
        while (Files.exists(savesDir.resolve(baseName + " " + counter))) {
            counter++;
        }
        return baseName + " " + counter;
    }

    private static void copyOverworldOnly(Path source, Path destination) throws IOException {
        Files.createDirectories(destination);
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path src : (Iterable<Path>) stream::iterator) {
                Path relative = source.relativize(src);
                if (relative.toString().isEmpty()) {
                    continue;
                }
                String first = relative.getNameCount() > 0
                        ? relative.getName(0).toString() : "";
                if (first.equals("DIM1") || first.equals("DIM-1") || first.startsWith("DIM")
                        || first.equals("playerdata") || first.equals("advancements")
                        || first.equals("stats") || first.equals(GENERATED_MARKER)
                        || first.equals("session.lock")) {
                    continue;
                }
                Path dest = destination.resolve(relative.toString());
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Path parent = dest.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private static void stripPlayerFromLevelDat(Path levelDat, long seed, TemplateGameMode gameMode) throws IOException {
        // level.dat is GZIP-compressed NBT (readCompressed/writeCompressed),
        // NOT raw NBT - LonKraft reads it the same way.
        Path backup = levelDat.resolveSibling(levelDat.getFileName() + ".bak");
        if (!Files.exists(backup)) {
            Files.copy(levelDat, backup, StandardCopyOption.COPY_ATTRIBUTES);
        }

        CompoundTag root;
        try (InputStream in = Files.newInputStream(levelDat)) {
            root = NbtIo.readCompressed(in);
        }
        CompoundTag data = root.getCompound("Data");
        data.putLong("RandomSeed", seed);
        data.putLong("WorldGenSettings_seed", seed);
        data.remove("Player");
        data.remove("CustomBosses");

        // Game mode of the new world (Survival default). Hardcore forces the
        // hardcore flag and Hard difficulty so the world list shows it in red.
        TemplateGameMode mode = gameMode != null ? gameMode : TemplateGameMode.SURVIVAL;
        if (mode.isHardcore()) {
            data.putBoolean("hardcore", true);
            data.putInt("Difficulty", 3); // HARD, forced for hardcore
            data.putInt("GameType", mode.gameType().getId());
        } else {
            data.putBoolean("hardcore", false);
            data.putInt("GameType", mode.gameType().getId());
        }
        // Creative and Spectator get cheats enabled, like the vanilla create screen.
        boolean cheats = mode == TemplateGameMode.CREATIVE || mode == TemplateGameMode.SPECTATOR;
        data.putBoolean("allowCommands", cheats);
        root.put("Data", data);

        try (OutputStream out = Files.newOutputStream(levelDat)) {
            NbtIo.writeCompressed(root, out);
        }
    }

    /**
     * Removes leftover broken worlds created by OUR mod from failed attempts:
     * folders starting with the world's base name that lack our GENERATED_MARKER.
     * Never touches normal worlds (they have no reason to start with the base name,
     * and completed worlds always carry the marker).
     *
     * @return number of folders removed
     */
    public static int cleanupBrokenWorlds(Path gameDir, String baseName) {
        if (baseName == null || baseName.isBlank()) {
            baseName = "Trilcera";
        }
        Path savesDir = gameDir.resolve("saves");
        if (!java.nio.file.Files.isDirectory(savesDir)) {
            return 0;
        }
        int removed = 0;
        String base = baseName;
        try (java.util.stream.Stream<Path> stream = java.nio.file.Files.list(savesDir)) {
            for (Path child : (Iterable<Path>) stream::iterator) {
                String name = child.getFileName().toString();
                boolean ours = name.equals(base) || name.startsWith(base + " ")
                        || name.startsWith("." + base);
                if (!ours || !java.nio.file.Files.isDirectory(child)) {
                    continue;
                }
                if (java.nio.file.Files.isRegularFile(child.resolve(GENERATED_MARKER))) {
                    continue; // completed world, keep it
                }
                try {
                    deleteRecursively(child);
                    LOGGER.info("[Trilcera Templates] Removed broken leftover world '{}'", name);
                    removed++;
                } catch (IOException e) {
                    LOGGER.warn("[Trilcera Templates] Could not remove '{}': {}", name, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("[Trilcera Templates] Cleanup scan failed: {}", e.getMessage());
        }
        return removed;
    }

    /**
     * Copies the template icon and, if needed, resizes it to the 64x64 that
     * the vanilla world selection screen requires (pattern copied from
     * vanilla FaviconTexture: NativeImage + resizeSubRectTo + writeToFile).
     */
    private static void writeWorldIcon(Path templateIcon, Path worldIcon) throws IOException {
        try (InputStream in = Files.newInputStream(templateIcon);
             NativeImage source = NativeImage.read(in)) {
            int sw = source.getWidth();
            int sh = source.getHeight();
            if (sw == 64 && sh == 64) {
                Files.copy(templateIcon, worldIcon, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                return;
            }
            try (NativeImage resized = new NativeImage(NativeImage.Format.RGBA, 64, 64, false)) {
                source.resizeSubRectTo(0, 0, sw, sh, resized);
                resized.writeToFile(worldIcon);
            }
        }
    }

    public static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            for (Path p : (Iterable<Path>) stream.sorted(Comparator.reverseOrder())::iterator) {
                Files.deleteIfExists(p);
            }
        }
    }
}
