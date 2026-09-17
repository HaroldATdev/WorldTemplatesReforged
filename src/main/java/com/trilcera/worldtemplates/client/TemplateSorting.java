package com.trilcera.worldtemplates.client;

import com.mojang.logging.LogUtils;
import com.trilcera.worldtemplates.Config;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Sorts the registered templates according to the configurable "templateSorting"
 * option. The "first" template (used when chooseTemplate = No) is the first
 * element of the sorted list, so the default (A-Z) picks the alphabetically
 * first template.
 */
public final class TemplateSorting {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Collator COLLATOR = Collator.getInstance(new Locale("es", "ES"));

    /** All supported sorting keys, in cycle order for the GUI button. */
    public static final String[] OPTIONS = {"A-Z", "Z-A", "NEWEST", "OLDEST", "ORIGINAL"};

    private TemplateSorting() {
    }

    /** Returns the configured sorting key, falling back to A-Z on invalid values. */
    public static String current() {
        String s = Config.INSTANCE.templateSorting.get();
        if (s == null) {
            return "A-Z";
        }
        String up = s.trim().toUpperCase(Locale.ROOT);
        for (String opt : OPTIONS) {
            if (opt.equals(up)) {
                return opt;
            }
        }
        LOGGER.warn("[Trilcera Templates] Invalid templateSorting '{}', falling back to A-Z", s);
        return "A-Z";
    }

    /** Returns a sorted copy of the given templates per the configured mode. */
    public static List<WorldTemplate> sort(List<WorldTemplate> templates) {
        String mode = current();
        if (mode.equals("ORIGINAL")) {
            return new ArrayList<>(templates);
        }
        List<WorldTemplate> copy = new ArrayList<>(templates);
        Comparator<WorldTemplate> cmp = switch (mode) {
            case "Z-A" -> Comparator.comparing((WorldTemplate t) -> name(t), COLLATOR).reversed();
            case "NEWEST" -> Comparator.comparingLong(TemplateSorting::folderTime).reversed();
            case "OLDEST" -> Comparator.comparingLong(TemplateSorting::folderTime);
            default -> Comparator.comparing(TemplateSorting::name, COLLATOR); // A-Z
        };
        copy.sort(cmp);
        return copy;
    }

    /** Cycles to the next sorting option (used by the selector's sort button). */
    public static String cycleAndSave() {
        String cur = current();
        int idx = 0;
        for (int i = 0; i < OPTIONS.length; i++) {
            if (OPTIONS[i].equals(cur)) {
                idx = i;
                break;
            }
        }
        String next = OPTIONS[(idx + 1) % OPTIONS.length];
        Config.INSTANCE.templateSorting.set(next);
        Config.save();
        return next;
    }

    /** Human-friendly label for the sort button. */
    public static String label(String mode) {
        return switch (mode) {
            case "Z-A" -> "Z-A";
            case "NEWEST" -> "Reciente";
            case "OLDEST" -> "Antiguo";
            case "ORIGINAL" -> "Original";
            default -> "A-Z";
        };
    }

    private static String name(WorldTemplate t) {
        if (t == null || t.buttonMessage() == null) {
            return "";
        }
        return t.buttonMessage().getString();
    }

    /** Last-modified time of the template's folder (0 if unknown -> sorts last/first). */
    private static long folderTime(WorldTemplate t) {
        try {
            String dir = t.templateLocation();
            if (dir == null || dir.isBlank()) {
                dir = t.folderName();
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.gameDirectory == null) {
                return 0L;
            }
            Path folder = mc.gameDirectory.toPath().resolve(dir);
            if (!Files.isDirectory(folder)) {
                return 0L;
            }
            return Files.getLastModifiedTime(folder).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}