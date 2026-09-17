package com.trilcera.worldtemplates.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads template icon.png files from the installed template folders
 * (like the server world's icon.png) and registers them as dynamic textures.
 */
public final class TemplateIconCache {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Cached texture entry with its real size (world icons are 64x64, ours 32x32). */
    public record CachedIcon(ResourceLocation location, int width, int height) {
    }

    private static final Map<String, CachedIcon> CACHE = new ConcurrentHashMap<>();

    private TemplateIconCache() {
    }

    /**
     * Returns a texture entry for the given template, preferring:
     * 1. config icon override (used directly as a resource location, 64x64 assumed)
     * 2. icon.png inside the installed template's REAL root (handles nested layout)
     * 3. the template's own icon from templates.json (32x32 default)
     */
    public static CachedIcon getIcon(WorldTemplate template) {
        // 1. Explicit override wins
        String override = com.trilcera.worldtemplates.Config.INSTANCE.templateIcon.get();
        if (override != null && !override.isBlank()) {
            return new CachedIcon(new ResourceLocation(override.trim()), 64, 64);
        }

        // 2. icon.png from the installed template's REAL root.
        // The zip nests the world one level deep (".../trilcera-template/<root>/icon.png"),
        // so resolve the effective root instead of assuming a flat layout.
        try {
            Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
            Path iconFile = null;

            String dir = template.templateLocation();
            if (dir == null || dir.isBlank()) {
                dir = template.folderName();
            }
            Path direct = gameDir.resolve(dir).resolve("icon.png");
            if (Files.isRegularFile(direct)) {
                iconFile = direct;
            } else {
                try {
                    Path root = TemplateInstaller.resolveTemplateRoot(gameDir);
                    Path nested = root.resolve("icon.png");
                    if (Files.isRegularFile(nested)) {
                        iconFile = nested;
                    }
                } catch (IOException e) {
                    LOGGER.warn("[Trilcera Templates] Template root not ready: {}", e.getMessage());
                }
            }

            if (iconFile != null) {
                String key = iconFile.toAbsolutePath().toString();
                CachedIcon cached = CACHE.get(key);
                if (cached != null) {
                    return cached;
                }
                try (InputStream in = Files.newInputStream(iconFile)) {
                    NativeImage image = NativeImage.read(in);
                    int w = image.getWidth();
                    int h = image.getHeight();
                    DynamicTexture texture = new DynamicTexture(image);
                    ResourceLocation id = Minecraft.getInstance().getTextureManager()
                            .register("worldtemplates/icon", texture);
                    CachedIcon entry = new CachedIcon(id, w, h);
                    CACHE.put(key, entry);
                    LOGGER.info("[Trilcera Templates] Loaded template icon {} ({}x{})",
                            iconFile, w, h);
                    return entry;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("[Trilcera Templates] Could not load template icon.png: {}", e.getMessage());
        }

        // 3. Fallback: template's own icon from templates.json (our 32x32 default)
        return new CachedIcon(template.icon(), 32, 32);
    }

    public static void clear() {
        CACHE.clear();
    }
}
