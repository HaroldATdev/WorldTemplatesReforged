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
        mc.setScreen(new SelectWorldScreen(new TitleScreen()));
        suppressCreateClicks(1200);
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

        // LonKraft parity: redirect CreateWorldScreen when vanilla is disabled.
        // ONLY for user-facing opens: vanilla ALSO opens CreateWorldScreen
        // internally (padre=GenericDirtMessageScreen) while the world list
        // processes existing worlds - intercepting that hijacked the flow and
        // kept re-opening the template selector even with chooseTemplate=NO.
        if (event.getScreen() instanceof CreateWorldScreen
                && !Config.INSTANCE.allowVanillaWorldCreation.get()
                && !(event.getCurrentScreen() instanceof TrilceraCreateWorldScreen)
                && !(event.getCurrentScreen() instanceof WorldTemplateScreen)) {
            Screen current = event.getCurrentScreen();
            boolean userFacing = current instanceof SelectWorldScreen
                    || current instanceof WorldTemplateScreen
                    || current instanceof TrilceraCreateWorldScreen
                    || current instanceof TrilceraErrorScreen;
            if (!userFacing) {
                LOGGER.info("[WTR] CreateWorldScreen interno ignorado (padre={})",
                        current != null ? current.getClass().getSimpleName() : "null");
                return;
            }
            LOGGER.info("[WTR] CreateWorldScreen interceptado -> flujo propio (padre={})",
                    current.getClass().getSimpleName());
            event.setNewScreen(new WorldTemplateScreen(current));
        }
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

        if (choose) {
            LOGGER.info("[WTR] Boton: escogerPlantilla=SI, orden={}, abriendo selector", sort);
            mc.setScreen(new WorldTemplateScreen(parent));
            return; // X1
        }

        List<WorldTemplate> sorted = TemplateSorting.sort(WorldTemplateManager.getTemplates());
        WorldTemplate first = sorted.isEmpty() ? null : sorted.get(0);
        if (first == null) {
            // Build the embedded template on the fly: it is always the fallback.
            String dir = DefaultTemplateProvider.EMBEDDED_TEMPLATE_DIR;
            first = new WorldTemplate(
                    Component.literal(DefaultTemplateProvider.displayName()),
                    new ResourceLocation(DefaultTemplateProvider.iconPath()),
                    dir, dir);
            sorted = List.of(first);
        }

        LOGGER.info("[WTR] Boton: escogerPlantilla=NO, orden={}, plantillas={}, usando='{}'",
                sort, sorted.size(), first.folderName());

        // Pre-flight: validate the template BEFORE showing a broken create screen.
        String preflightError = preflightTemplate();
        if (preflightError != null) {
            mc.setScreen(new TrilceraErrorScreen(parent, preflightError));
            return;
        }
        mc.setScreen(new TrilceraCreateWorldScreen(first, parent));
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
