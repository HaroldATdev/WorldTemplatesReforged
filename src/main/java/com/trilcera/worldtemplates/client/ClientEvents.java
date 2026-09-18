package com.trilcera.worldtemplates.client;

import com.mojang.logging.LogUtils;
import com.trilcera.worldtemplates.Config;
import com.trilcera.worldtemplates.client.gui.TrilceraCreateWorldScreen;
import com.trilcera.worldtemplates.client.gui.TrilceraErrorScreen;
import com.trilcera.worldtemplates.client.gui.WorldTemplateScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Client-side Forge events that inject the "Crear Trilcera" button.
 * Technique based on LonKraft's ClientEvents (event based, no mixins).
 */
@Mod.EventBusSubscriber(modid = "worldtemplates", value = Dist.CLIENT)
public class ClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile boolean templateChecked;
    private static final Component VANILLA_CREATE = Component.translatable("selectWorld.create");
    private static volatile long suppressCreateClicksUntil;

    /** Swallows clicks on the world-list button right after one of our screens
     * closes: input-replay mods (Ixeris) re-dispatch queued clicks once the
     * (possibly long) screen switch finishes, and they land on whatever button
     * now occupies the same coordinates. */
    public static void suppressCreateClicks(long ms) {
        suppressCreateClicksUntil = System.currentTimeMillis() + ms;
    }

    /**
     * Opens a fresh world list. Reusing a removed SelectWorldScreen instance
     * is NOT safe: setScreen() already closed its world favicons, and removing
     * it again crashes with "Icon already closed" (WorldSelectionList entries).
     * Note: SelectWorldScreen.init() runs a full datapack reload via
     * managedBlock (seconds on big modpacks) while queued input replays
     * (Ixeris) pile up - they fire right after init() returns, so the
     * suppression window must start AFTER setScreen(), never before.
     */
    public static void returnToWorldList() {
        Minecraft mc = Minecraft.getInstance();
        // Empty world list: vanilla replaces the list with its own
        // CreateWorldScreen (WorldSelectionList.loadLevels() ->
        // CreateWorldScreen.openFresh(minecraft, null)), which we intercept and
        // turn into our own flow - so returning to the list would bounce right
        // back. Go to the main menu instead. With worlds present we DO return
        // to the list, as requested.
        if (isWorldListEmpty() && !Config.INSTANCE.allowVanillaWorldCreation.get()) {
            mc.setScreen(new TitleScreen());
            return;
        }
        mc.setScreen(new SelectWorldScreen(new TitleScreen()));
        suppressCreateClicks(1200);
    }

    /**
     * True when saves/ holds no usable world at all (no folder with a level.dat).
     * That is exactly the condition under which vanilla's world list opens the
     * vanilla CreateWorldScreen by itself, so our Opening handler has to treat
     * it as a user-facing open instead of ignoring it.
     */
    public static boolean isWorldListEmpty() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.gameDirectory == null) {
                return false;
            }
            java.nio.file.Path saves = mc.gameDirectory.toPath().resolve("saves");
            if (!java.nio.file.Files.isDirectory(saves)) {
                return true;
            }
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(saves)) {
                return stream.noneMatch(p -> java.nio.file.Files.isRegularFile(p.resolve("level.dat")));
            }
        } catch (Exception e) {
            // Be conservative: assume the player DOES have worlds.
            return false;
        }
    }

    /** Back to the parent: instant for our own screens, fresh world list otherwise. */
    public static void backToParent(Screen parent) {
        if (parent instanceof WorldTemplateScreen || parent instanceof TrilceraCreateWorldScreen) {
            suppressCreateClicks(1200);
            Minecraft.getInstance().setScreen(parent);
            return;
        }
        returnToWorldList();
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        ensureTemplateInstalled();

        if (!(event.getScreen() instanceof SelectWorldScreen selectScreen)) {
            return;
        }

        boolean allowVanilla = Config.INSTANCE.allowVanillaWorldCreation.get();

        if (allowVanilla) {
            int posX = Config.INSTANCE.buttonPositionX.get();
            int posY = Config.INSTANCE.buttonPositionY.get();
            int w = Config.INSTANCE.buttonWidth.get();
            int h = Config.INSTANCE.buttonHeight.get();
            if (posX == -1) {
                posX = selectScreen.width / 2 - w / 2;
            }
            Button ours = Button.builder(buttonLabel(), btn -> onCreatePressed(selectScreen))
                    .bounds(posX, posY, w, h)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.literal(Config.INSTANCE.buttonTooltip.get())))
                    .build();
            event.addListener(ours);
        } else {
            // LonKraft behavior: hide vanilla create button, ours takes its place
            for (var child : java.util.List.copyOf(event.getScreen().children())) {
                if (!(child instanceof Button button)) {
                    continue;
                }
                if (!button.getMessage().getString().equals(VANILLA_CREATE.getString())) {
                    continue;
                }
                button.active = false;
                button.visible = false;

                Button ours = Button.builder(buttonLabel(), btn -> onCreatePressed(selectScreen))
                        .bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight())
                        .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                                Component.literal(Config.INSTANCE.buttonTooltip.get())))
                        .build();
                event.addListener(ours);
                return;
            }
            LOGGER.warn("[Trilcera Templates] Vanilla button not found, using fallback pos");
            int posX = selectScreen.width / 2 - Config.INSTANCE.buttonWidth.get() / 2;
            Button ours = Button.builder(buttonLabel(), btn -> onCreatePressed(selectScreen))
                    .bounds(posX, Config.INSTANCE.buttonPositionY.get(),
                            Config.INSTANCE.buttonWidth.get(), Config.INSTANCE.buttonHeight.get())
                    .build();
            event.addListener(ours);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        ensureTemplateInstalled();

        if (!(event.getScreen() instanceof CreateWorldScreen)) {
            return;
        }
        if (Config.INSTANCE.allowVanillaWorldCreation.get()) {
            return; // the pack wants the vanilla screen: hands off
        }
        Screen current = event.getCurrentScreen();
        if (current instanceof WorldTemplateScreen
                || current instanceof TrilceraCreateWorldScreen
                || current instanceof TrilceraErrorScreen) {
            return; // already inside our own flow
        }

        // Two vanilla paths lead here:
        //  1) the player pressed a still-visible vanilla "Create New World"
        //     button -> current screen is the SelectWorldScreen;
        //  2) saves/ has NO worlds, so WorldSelectionList.loadLevels() calls
        //     CreateWorldScreen.openFresh(minecraft, null) on its own (the
        //     current screen is the GenericDirtMessageScreen it shows first).
        //     This is the case that used to drop the player on the vanilla
        //     screen and made chooseTemplate=NO look broken.
        // Everything else (Edit / Re-Create / other mods) is left untouched.
        boolean fromWorldList = current instanceof SelectWorldScreen;
        boolean emptyListAutoOpen = isWorldListEmpty();
        if (!fromWorldList && !emptyListAutoOpen) {
            LOGGER.info("[WTR] CreateWorldScreen ignorado (flujo ajeno: padre={})", typeName(current));
            return;
        }

        LOGGER.info("[WTR] CreateWorldScreen interceptado (padre={}, listaVacia={}) -> flujo propio",
                typeName(current), emptyListAutoOpen);
        // A null parent means "we came from an empty world list": our screens
        // then fall back to the main menu instead of bouncing on the list.
        event.setNewScreen(ownFlowScreen(emptyListAutoOpen ? null : current));
    }

    private static String typeName(Screen screen) {
        return screen == null ? "null" : screen.getClass().getSimpleName();
    }

    /**
     * Single entry point of our own flow, shared by the world-list button and by
     * intercepted CreateWorldScreen opens. It honours chooseTemplate, so "NO"
     * never opens the selector: it goes straight to the create screen with the
     * first template after the configured sorting.
     */
    private static Screen ownFlowScreen(Screen parent) {
        if (Config.INSTANCE.chooseTemplate.get()) {
            LOGGER.info("[WTR] flujo propio: abriendo selector de plantillas");
            return new WorldTemplateScreen(parent);
        }
        WorldTemplate first = firstTemplateOrDefault();
        String preflightError = preflightTemplate();
        if (preflightError != null) {
            LOGGER.warn("[WTR] flujo propio: plantilla no valida - {}", preflightError);
            return new TrilceraErrorScreen(parent, preflightError);
        }
        LOGGER.info("[WTR] flujo propio: creando directamente con '{}'", first.folderName());
        return new TrilceraCreateWorldScreen(first, parent);
    }

    /**
     * First template AFTER the configured sorting; when none is registered the
     * embedded trilcera-template is built on the fly (always the fallback).
     */
    private static WorldTemplate firstTemplateOrDefault() {
        List<WorldTemplate> sorted = TemplateSorting.sort(WorldTemplateManager.getTemplates());
        if (!sorted.isEmpty()) {
            return sorted.get(0);
        }
        String dir = DefaultTemplateProvider.EMBEDDED_TEMPLATE_DIR;
        return new WorldTemplate(
                Component.literal(DefaultTemplateProvider.displayName()),
                new ResourceLocation(DefaultTemplateProvider.iconPath()),
                dir, dir);
    }

    /**
     * Deterministic button flow:
     * - chooseTemplate = YES -> template selection screen
     * - chooseTemplate = NO  -> first template AFTER sorting; if the registered
     *   list is empty, the embedded trilcera-template is built on the fly.
     *   Never falls back to the selector screen.
     */
    private static void onCreatePressed(SelectWorldScreen parent) {
        long now = System.currentTimeMillis();
        if (now < suppressCreateClicksUntil) {
            LOGGER.info("[WTR] Click ignorado: replay de entrada tras cerrar pantalla ({} ms restantes)",
                    suppressCreateClicksUntil - now);
            return;
        }
        if (!ClickGuard.allow("create-press")) {
            LOGGER.info("[WTR] Click ignorado: rebote (replay de Ixeris)");
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean choose = Config.INSTANCE.chooseTemplate.get();
        String sort = TemplateSorting.current();

        LOGGER.info("[WTR] Boton: escogerPlantilla={}, orden={}, listaVacia={}",
                choose ? "SI" : "NO", sort, isWorldListEmpty());
        // One single entry point (ownFlowScreen), shared with the intercepted
        // CreateWorldScreen opens: it honours chooseTemplate and reports template
        // problems through the error screen instead of opening a broken screen.
        mc.setScreen(ownFlowScreen(parent));
    }

    /** Returns an error message if the template is not usable, else null. */
    private static String preflightTemplate() {
        try {
            Minecraft mc = Minecraft.getInstance();
            TemplateInstaller.ensureInstalled(mc.gameDirectory.toPath());
            TemplateInstaller.resolveTemplateRoot(mc.gameDirectory.toPath());
            return null;
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("level.dat")) {
                return "La plantilla instalada no es valida (level.dat). Elimina la carpeta trilcera-template y reinicia el juego para reinstalarla.";
            }
            if (msg != null && msg.contains("not found")) {
                return "La plantilla embebida no se encontro. Reinstala el mod.";
            }
            return "Error de plantilla: " + msg;
        }
    }

    private static Component buttonLabel() {
        return Component.literal(Config.INSTANCE.buttonText.get());
    }

    public static void ensureTemplateInstalled() {
        if (templateChecked) {
            return;
        }
        synchronized (ClientEvents.class) {
            if (templateChecked) {
                return;
            }
            try {
                Minecraft mc = Minecraft.getInstance();
                File gameDir = mc.gameDirectory;
                if (gameDir != null) {
                    DefaultTemplateProvider.ensureInstalled(gameDir.toPath());
                    WorldTemplateManager.ensureDefaultTemplate();
                    // Automatic cleanup of broken leftovers (failed attempts):
                    // worlds named like ours but WITHOUT our generated marker.
                    int cleaned = WorldTemplateCloner.cleanupBrokenWorlds(
                            gameDir.toPath(), Config.INSTANCE.worldBaseName.get());
                    if (cleaned > 0) {
                        LOGGER.info("[Trilcera Templates] Cleaned {} broken leftover world(s)", cleaned);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[Trilcera Templates] Failed to install embedded template", e);
            }
            templateChecked = true;
        }
    }

    private ClientEvents() {
    }
}
