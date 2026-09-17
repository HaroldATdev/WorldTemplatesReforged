package com.trilcera.worldtemplates.client;

import net.minecraft.world.level.GameType;

import java.util.Locale;

/**
 * Game modes offered when creating a world from a template.
 *
 * <p>Vanilla's {@link GameType} has no HARDCORE constant in 1.20.1 - hardcore is a
 * separate boolean flag in {@code level.dat} - so we model the full set ourselves
 * and translate it into the right NBT fields in {@link WorldTemplateCloner}.</p>
 */
public enum TemplateGameMode {
    SURVIVAL("survival", GameType.SURVIVAL, false),
    CREATIVE("creative", GameType.CREATIVE, false),
    ADVENTURE("adventure", GameType.ADVENTURE, false),
    SPECTATOR("spectator", GameType.SPECTATOR, false),
    HARDCORE("hardcore", GameType.SURVIVAL, true);

    private final String id;
    private final GameType gameType;
    private final boolean hardcore;

    TemplateGameMode(String id, GameType gameType, boolean hardcore) {
        this.id = id;
        this.gameType = gameType;
        this.hardcore = hardcore;
    }

    /** Config identifier, e.g. "survival" or "hardcore". */
    public String id() {
        return id;
    }

    /** Underlying vanilla game type (HARDCORE maps to SURVIVAL + flag). */
    public GameType gameType() {
        return gameType;
    }

    /** True only for {@link #HARDCORE}. */
    public boolean isHardcore() {
        return hardcore;
    }

    /** Human-readable label shown on the create screen button. */
    public String label() {
        return switch (this) {
            case CREATIVE -> "Creativo";
            case ADVENTURE -> "Aventura";
            case SPECTATOR -> "Espectador";
            case HARDCORE -> "Hardcore";
            case SURVIVAL -> "Supervivencia";
        };
    }

    /** Next mode in cycle order (used by the button). */
    public TemplateGameMode next() {
        TemplateGameMode[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /** Parses a config id case-insensitively, or null when unknown. */
    public static TemplateGameMode byId(String id) {
        if (id != null) {
            String v = id.trim().toLowerCase(Locale.ROOT);
            for (TemplateGameMode m : values()) {
                if (m.id.equals(v)) {
                    return m;
                }
            }
        }
        return null;
    }

    /** Parses a config id, falling back to {@link #SURVIVAL} on invalid values. */
    public static TemplateGameMode byIdOrSurvival(String id) {
        TemplateGameMode m = byId(id);
        return m != null ? m : SURVIVAL;
    }
}
