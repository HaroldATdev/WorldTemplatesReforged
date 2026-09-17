package com.trilcera.worldtemplates.client.gui;

import com.mojang.logging.LogUtils;
import com.trilcera.worldtemplates.Config;
import com.trilcera.worldtemplates.client.TemplateGameMode;
import com.trilcera.worldtemplates.client.WorldTemplate;
import com.trilcera.worldtemplates.client.WorldTemplateCloner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Creation screen shown after picking a template.
 * Shows the game-mode selector (Survival default) and launches the world copy
 * on a background thread, then opens the world. Any failure is routed to the
 * TrilceraErrorScreen with a reason and retry action.
 */
public class TrilceraCreateWorldScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final WorldTemplate template;
    private Button launchButton;
    private Button modeButton;
    private Component status;
    private boolean working;
    private TemplateGameMode gameMode;

    public TrilceraCreateWorldScreen(WorldTemplate template) {
        super(Component.literal("Crear Trilcera"));
        this.template = template;
        this.gameMode = parseDefaultGameMode(Config.INSTANCE.defaultGameMode.get());
        this.status = Component.literal("Listo para crear tu mundo Trilcera.");
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.modeButton = Button.builder(modeLabel(), b -> cycleMode())
                .bounds(cx - 100, cy - 20, 200, 20).build();
        this.launchButton = Button.builder(
                        Component.literal("Crear '" + template.buttonMessage().getString() + "'"),
                        b -> launch())
                .bounds(cx - 100, cy + 20, 200, 20).build();
        this.addRenderableWidget(this.modeButton);
        this.addRenderableWidget(this.launchButton);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds(cx - 100, cy + 45, 200, 20).build());
    }

    private void cycleMode() {
        this.gameMode = this.gameMode.next();
        this.modeButton.setMessage(modeLabel());
    }

    private Component modeLabel() {
        Component c = Component.literal("Modo de juego: " + this.gameMode.label());
        if (this.gameMode.isHardcore()) {
            c = c.copy().withStyle(style -> style.withColor(0xFF5555));
        }
        return c;
    }
private void launch() {
        if (working) {
            return;
        }
        working = true;
        launchButton.active = false;
        status = Component.literal("Creando mundo desde la plantilla...");
        Minecraft mc = this.minecraft;
        TemplateGameMode mode = this.gameMode;

        new Thread(() -> {
            try {
                String worldName = WorldTemplateCloner.createFreshWorld(
                        mc.gameDirectory.toPath(),
                        Config.INSTANCE.worldBaseName.get(),
                        mode);
                mc.execute(() -> {
                    status = Component.literal("Abriendo '" + worldName + "'...");
                    openWorldByName(worldName);
                });
            } catch (Exception e) {
                LOGGER.error("[Trilcera Templates] Failed to create world", e);
                String reason = describe(e);
                mc.execute(() -> showError(reason));
            }
        }, "Trilcera-World-Create").start();
    }

    /** Human-readable reason for the common failure classes. */
    private static String describe(Exception e) {
        String msg = e.getMessage();
        String cause = e.getCause() != null ? e.getCause().getMessage() : null;
        String detail = (cause != null && !cause.isBlank()) ? cause : (msg != null ? msg : e.toString());
        String cls = e.getClass().getSimpleName();

        if (cls.contains("IllegalState") || (msg != null && msg.contains("level.dat"))) {
            return "La plantilla instalada no es valida (level.dat). Elimina la carpeta trilcera-template y reinicia el juego para reinstalarla.";
        }
        if (msg != null && msg.toLowerCase().contains("space")) {
            return "No hay espacio en disco para crear el mundo: " + detail;
        }
        if (msg != null && msg.toLowerCase().contains("denied")) {
            return "Permiso denegado al escribir el mundo: " + detail;
        }
        return "No se pudo crear el mundo: " + detail;
    }

    private static TemplateGameMode parseDefaultGameMode(String v) {
        // byIdOrSurvival logs nothing itself; invalid values fall back safely.
        return TemplateGameMode.byIdOrSurvival(v);
    }

    private void openWorldByName(String worldName) {
        Minecraft mc = Minecraft.getInstance();
        try {
            // WorldOpenFlows.loadLevel(parentScreen, levelId) opens the world.
            mc.createWorldOpenFlows().loadLevel(new SelectWorldScreen(new TitleScreen()), worldName);
        } catch (Exception e) {
            LOGGER.error("[Trilcera Templates] Failed to open created world", e);
            mc.execute(() -> showError(
                    "El mundo '" + worldName + "' se creo, pero no se pudo abrir: "
                            + (e.getMessage() != null ? e.getMessage() : e.toString())));
        }
    }

    private void showError(String reason) {
        TrilceraErrorScreen err = new TrilceraErrorScreen(new SelectWorldScreen(new TitleScreen()),
                reason, this::launch);
        err.init(this.minecraft, this.width, this.height);
        this.minecraft.setScreen(err);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        int cy = this.height / 2;
        g.drawCenteredString(this.font, this.title, this.width / 2, cy - 55, 0xFFFFFF);
        g.drawCenteredString(this.font, template.buttonMessage(),
                this.width / 2, cy - 40, 0x55FF55);
        g.drawCenteredString(this.font, status,
                this.width / 2, cy - 2, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new SelectWorldScreen(new TitleScreen()));
        }
    }
}