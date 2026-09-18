package com.trilcera.worldtemplates.client.gui;

import com.trilcera.worldtemplates.Config;
import com.trilcera.worldtemplates.client.ClickGuard;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * In-game config screen for Trilcera World Templates.
 * Open from: Mod List -> select mod -> Config button.
 * Saves to config/worldtemplates-client.toml
 */
public class TrilceraConfigScreen extends Screen {

    private final Screen parent;
    private static final Component TITLE = Component.literal("World Templates Reforged");

    private EditBox buttonTextField;
    private EditBox buttonTooltipField;
    private EditBox posXField;
    private EditBox posYField;
    private EditBox widthField;
    private EditBox heightField;
    private EditBox defaultTemplateField;
    private Button defaultTemplateBtn;
    private EditBox templateNameField;
    private EditBox templateIconField;
    private EditBox worldBaseNameField;
    private Button toggleChooseBtn;
    private Button toggleDescriptionsBtn;
    private Button toggleVanillaBtn;
    private Button sortBtn;
    private Button modeBtn;
    private boolean chooseTemplate;
    @Override
    protected void init() {
        this.chooseTemplate = Config.INSTANCE.chooseTemplate.get();
        this.showDescriptions = Config.INSTANCE.showTemplateDescriptions.get();
        this.allowVanilla = Config.INSTANCE.allowVanillaWorldCreation.get();
        int cx = this.width / 2;
        this.tabButtonBtn = Button.builder(Component.literal("Boton Crear"),
                b -> { currentTab = 0; updateTabs(); })
                .bounds(cx - 210, 40, 200, 20).build();
        this.tabTemplatesBtn = Button.builder(Component.literal("Plantillas"),
                b -> { currentTab = 1; updateTabs(); })
                .bounds(cx + 10, 40, 200, 20).build();
        this.addRenderableWidget(this.tabButtonBtn);
        this.addRenderableWidget(this.tabTemplatesBtn);
        int fx = cx + 10;
        int fw = 200;
        int sy = 75;
        int rh = 24;
        this.buttonTextField = new EditBox(this.font, fx, sy, fw, 20,
                Component.literal("Texto"));
        this.buttonTextField.setMaxLength(60);
        this.buttonTextField.setValue(Config.INSTANCE.buttonText.get());
        this.buttonTooltipField = new EditBox(this.font, fx, sy + rh, fw, 20,
                Component.literal("Tooltip"));
        this.buttonTooltipField.setMaxLength(120);
        this.buttonTooltipField.setValue(Config.INSTANCE.buttonTooltip.get());
        this.posXField = new EditBox(this.font, fx, sy + rh * 2, fw, 20,
                Component.literal("Pos X"));
        this.posXField.setMaxLength(6);
        this.posXField.setValue(String.valueOf(Config.INSTANCE.buttonPositionX.get()));
        this.posXField.setFilter(s -> s.matches("-?\\d*"));
        this.posYField = new EditBox(this.font, fx, sy + rh * 3, fw, 20,
                Component.literal("Pos Y"));
        this.posYField.setMaxLength(6);
        this.posYField.setValue(String.valueOf(Config.INSTANCE.buttonPositionY.get()));
        this.posYField.setFilter(s -> s.matches("-?\\d*"));
        this.widthField = new EditBox(this.font, fx, sy + rh * 4, fw, 20,
                Component.literal("Ancho"));
        this.widthField.setMaxLength(6);
        this.widthField.setValue(String.valueOf(Config.INSTANCE.buttonWidth.get()));
        this.widthField.setFilter(s -> s.matches("\\d*"));
        this.heightField = new EditBox(this.font, fx, sy + rh * 5, fw, 20,
                Component.literal("Alto"));
        this.heightField.setMaxLength(6);
        this.heightField.setValue(String.valueOf(Config.INSTANCE.buttonHeight.get()));
        this.heightField.setFilter(s -> s.matches("\\d*"));
        this.defaultTemplateField = new EditBox(this.font, fx, sy, fw, 20,
                Component.literal("Defecto"));
        this.defaultTemplateField.setMaxLength(60);
        this.defaultTemplateField.setValue(Config.INSTANCE.defaultTemplate.get());
        this.defaultTemplateField.setVisible(false);
        this.defaultTemplateBtn = Button.builder(Component.literal("Plantilla"),
                b -> cycleDefaultTemplate()).bounds(fx, sy, fw, 20).build();
        refreshDefaultTemplateLabel();
        this.templateNameField = new EditBox(this.font, fx, sy + rh * 2, fw, 20,
                Component.literal("Nombre"));
        this.templateNameField.setMaxLength(60);
        this.templateNameField.setValue(Config.INSTANCE.templateName.get());
        this.toggleChooseBtn = Button.builder(
                Component.literal("Escoger: " + (chooseTemplate ? "SI" : "NO")),
                b -> {
                    if (!ClickGuard.allow("cfg.choose")) {
                        return;
                    }
                    chooseTemplate = !chooseTemplate;
                    b.setMessage(Component.literal("Escoger: " + (chooseTemplate ? "SI" : "NO")));
                }).bounds(fx, sy + rh, fw, 20).build();
        this.templateIconField = new EditBox(this.font, fx, sy + rh * 3, fw, 20,
                Component.literal("Icono"));
        this.templateIconField.setMaxLength(120);
        this.templateIconField.setValue(Config.INSTANCE.templateIcon.get());
        this.worldBaseNameField = new EditBox(this.font, fx, sy + rh * 4, fw, 20,
                Component.literal("Base"));
        this.worldBaseNameField.setMaxLength(40);
        this.worldBaseNameField.setValue(Config.INSTANCE.worldBaseName.get());
        this.toggleDescriptionsBtn = Button.builder(
                Component.literal("Desc: " + (showDescriptions ? "SI" : "NO")),
                b -> {
                    if (!ClickGuard.allow("cfg.desc")) {
                        return;
                    }
                    showDescriptions = !showDescriptions;
                    b.setMessage(Component.literal("Desc: " + (showDescriptions ? "SI" : "NO")));
                }).bounds(fx, sy + rh * 5, fw, 20).build();
        this.toggleVanillaBtn = Button.builder(
                Component.literal("Mostrar: " + (allowVanilla ? "SI" : "NO")),
                b -> {
                    if (!ClickGuard.allow("cfg.vanilla")) {
                        return;
                    }
                    allowVanilla = !allowVanilla;
                    b.setMessage(Component.literal("Mostrar: " + (allowVanilla ? "SI" : "NO")));
                }).bounds(fx, sy + rh * 6, fw, 20).build();
        this.sortBtn = Button.builder(sortLabel(), b -> {
                    if (ClickGuard.allow("cfg.sort")) {
                        com.trilcera.worldtemplates.client.TemplateSorting.cycleAndSave();
                        b.setMessage(sortLabel());
                    }
                }).bounds(fx, sy + rh * 7, fw, 20).build();
        this.modeBtn = Button.builder(modeLabel(), b -> {
                    if (ClickGuard.allow("cfg.mode")) {
                        cycleGameMode();
                        b.setMessage(modeLabel());
                    }
                }).bounds(fx, sy + rh * 8, fw, 20).build();
        Button saveBtn = Button.builder(Component.literal("Guardar"),
                b -> saveAndClose()).bounds(cx - 105, this.height - 28, 100, 20).build();
        Button cancelBtn = Button.builder(CommonComponents.GUI_CANCEL,
                b -> onClose()).bounds(cx + 5, this.height - 28, 100, 20).build();
        this.addRenderableWidget(saveBtn);
        this.addRenderableWidget(cancelBtn);
        updateTabs();
    }

    private boolean showDescriptions;
    private boolean allowVanilla;
    private int currentTab = 0;
    private Button tabButtonBtn;
    private Button tabTemplatesBtn;

    public TrilceraConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }
    private void updateTabs() {
        this.removeWidget(this.buttonTextField);
        this.removeWidget(this.buttonTooltipField);
        this.removeWidget(this.posXField);
        this.removeWidget(this.posYField);
        this.removeWidget(this.widthField);
        this.removeWidget(this.heightField);
        this.removeWidget(this.defaultTemplateField);
        this.removeWidget(this.defaultTemplateBtn);
        this.removeWidget(this.toggleChooseBtn);
        this.removeWidget(this.templateNameField);
        this.removeWidget(this.templateIconField);
        this.removeWidget(this.worldBaseNameField);
        this.removeWidget(this.toggleDescriptionsBtn);
        this.removeWidget(this.toggleVanillaBtn);
        this.removeWidget(this.sortBtn);
        this.removeWidget(this.modeBtn);
        this.tabButtonBtn.setMessage(Component.literal(currentTab == 0 ? "[Boton]" : "Boton"));
        this.tabTemplatesBtn.setMessage(Component.literal(currentTab == 1 ? "[Plantillas]" : "Plantillas"));
        if (currentTab == 0) {
            this.addRenderableWidget(this.buttonTextField);
            this.addRenderableWidget(this.buttonTooltipField);
            this.addRenderableWidget(this.posXField);
            this.addRenderableWidget(this.posYField);
            this.addRenderableWidget(this.widthField);
            this.addRenderableWidget(this.heightField);
        } else {
            this.addRenderableWidget(this.defaultTemplateBtn);
            this.addRenderableWidget(this.toggleChooseBtn);
            this.addRenderableWidget(this.templateNameField);
            this.addRenderableWidget(this.templateIconField);
            this.addRenderableWidget(this.worldBaseNameField);
            this.addRenderableWidget(this.toggleDescriptionsBtn);
            this.addRenderableWidget(this.toggleVanillaBtn);
            this.addRenderableWidget(this.sortBtn);
            this.addRenderableWidget(this.modeBtn);
        }
    }

    private Component sortLabel() {
        return Component.literal("Orden: " + com.trilcera.worldtemplates.client.TemplateSorting.label(
                com.trilcera.worldtemplates.client.TemplateSorting.current()));
    }

    private Component modeLabel() {
        String v = Config.INSTANCE.defaultGameMode.get();
        String name;
        if (v != null) {
            switch (v.trim().toLowerCase()) {
                case "creative" -> name = "Creativo";
                case "adventure" -> name = "Aventura";
                case "spectator" -> name = "Espectador";
                case "hardcore" -> name = "Hardcore";
                default -> name = "Supervivencia";
            }
        } else {
            name = "Supervivencia";
        }
        return Component.literal("Modo: " + name);
    }

    private void cycleGameMode() {
        String[] all = {"survival", "creative", "adventure", "spectator", "hardcore"};
        String cur = Config.INSTANCE.defaultGameMode.get();
        int idx = 0;
        if (cur != null) {
            for (int i = 0; i < all.length; i++) {
                if (all[i].equals(cur.trim().toLowerCase())) {
                    idx = i;
                    break;
                }
            }
        }
        Config.INSTANCE.defaultGameMode.set(all[(idx + 1) % all.length]);
        Config.save();
    }

    /** Select-cycles the default template through the registered templates. */
    private void cycleDefaultTemplate() {
        if (!ClickGuard.allow("cfg.deftpl")) {
            return;
        }
        java.util.List<com.trilcera.worldtemplates.client.WorldTemplate> list =
                com.trilcera.worldtemplates.client.WorldTemplateManager.getTemplates();
        String current = defaultTemplateField.getValue().trim();
        if (list.isEmpty()) {
            refreshDefaultTemplateLabel();
            return;
        }
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).folderName().equalsIgnoreCase(current)) {
                idx = i;
                break;
            }
        }
        int next = (idx + 1) % list.size();
        defaultTemplateField.setValue(list.get(next).folderName());
        refreshDefaultTemplateLabel();
    }

    private void refreshDefaultTemplateLabel() {
        if (defaultTemplateBtn != null) {
            String v = defaultTemplateField.getValue().trim();
            if (v.isEmpty()) {
                v = "(primera)";
            }
            defaultTemplateBtn.setMessage(Component.literal("Plantilla: " + v));
        }
    }

    private void saveAndClose() {
        String t = buttonTextField.getValue().trim();
        if (!t.isEmpty()) Config.INSTANCE.buttonText.set(t);
        Config.INSTANCE.buttonTooltip.set(buttonTooltipField.getValue().trim());
        Config.INSTANCE.buttonPositionX.set(parseInt(posXField.getValue(), -1));
        Config.INSTANCE.buttonPositionY.set(parseInt(posYField.getValue(), 20));
        Config.INSTANCE.buttonWidth.set(clamp(parseInt(widthField.getValue(), 200), 50, 500));
        Config.INSTANCE.buttonHeight.set(clamp(parseInt(heightField.getValue(), 20), 10, 50));
        Config.INSTANCE.defaultTemplate.set(defaultTemplateField.getValue().trim());
        Config.INSTANCE.chooseTemplate.set(chooseTemplate);
        Config.INSTANCE.templateName.set(templateNameField.getValue().trim());
        Config.INSTANCE.templateIcon.set(templateIconField.getValue().trim());
        String wb = worldBaseNameField.getValue().trim();
        if (!wb.isEmpty()) {
            Config.INSTANCE.worldBaseName.set(wb);
        }
        Config.INSTANCE.showTemplateDescriptions.set(showDescriptions);
        Config.INSTANCE.allowVanillaWorldCreation.set(allowVanilla);
        Config.save();
        onClose();
    }

    private static int parseInt(String s, int fb) {
        try {
            s = s.trim();
            if (s.isEmpty()) return fb;
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fb;
        }
    }

    private static int clamp(int v, int a, int b) {
        return Math.max(a, Math.min(b, v));
    }

    /** Draws a label and its description tooltip when the mouse hovers it. */
    private void drawLabeled(GuiGraphics g, String label, String desc, int x, int y, int mx, int my) {
        g.drawString(this.font, label, x, y, 0xFFFFFF);
        int w = this.font.width(label);
        if (mx >= x && mx <= x + w + 4 && my >= y - 2 && my <= y + 10) {
            g.renderComponentTooltip(this.font,
                    java.util.List.of(net.minecraft.network.chat.Component.literal(desc)),
                    mx + 10, my + 10);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        g.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        int lx = this.width / 2 - 210;
        int sy = 75;
        int rh = 24;
        if (currentTab == 0) {
            drawLabeled(g, "Texto:", "Texto que muestra el boton en la lista de mundos", lx, sy + 6, mx, my);
            drawLabeled(g, "Tooltip:", "Texto al pasar el mouse sobre el boton (vacio = sin tooltip)", lx, sy + rh + 6, mx, my);
            drawLabeled(g, "Pos X:", "Posicion horizontal del boton (-1 = centrado)", lx, sy + rh * 2 + 6, mx, my);
            drawLabeled(g, "Pos Y:", "Posicion vertical del boton en la pantalla", lx, sy + rh * 3 + 6, mx, my);
            drawLabeled(g, "Ancho:", "Ancho del boton en pixeles (50-500)", lx, sy + rh * 4 + 6, mx, my);
            drawLabeled(g, "Alto:", "Alto del boton en pixeles (10-50)", lx, sy + rh * 5 + 6, mx, my);
        } else {
            drawLabeled(g, "Plantilla por defecto:", "Plantilla preseleccionada. El boton la cicla entre las registradas", lx, sy + 6, mx, my);
            drawLabeled(g, "Escoger:", "SI: abre el selector de plantillas. NO: usa la primera plantilla directamente", lx, sy + rh + 6, mx, my);
            drawLabeled(g, "Nombre de la plantilla principal:", "Sobrescribe el nombre mostrado de la plantilla (vacio = su nombre propio)", lx, sy + rh * 2 + 6, mx, my);
            drawLabeled(g, "Icono:", "Ruta de textura que reemplaza el icono (vacio = icon.png de la plantilla)", lx, sy + rh * 3 + 6, mx, my);
            drawLabeled(g, "Nombre del mundo base:", "Nombre base de los mundos creados: Trilcera, Trilcera 2, ...", lx, sy + rh * 4 + 6, mx, my);
            drawLabeled(g, "Descrip:", "Muestra u oculta Folder/Source debajo del nombre en la lista", lx, sy + rh * 5 + 6, mx, my);
            drawLabeled(g, "Mostrar 'Crear un mundo nuevo':", "SI conserva el boton vanilla. NO lo oculta y el nuestro ocupa su lugar", lx, sy + rh * 6 + 6, mx, my);
            drawLabeled(g, "Orden:", "Orden de la lista de plantillas: A-Z, Z-A, Reciente, Antiguo, Original", lx, sy + rh * 7 + 6, mx, my);
            drawLabeled(g, "Modo:", "Modo de juego por defecto al crear el mundo: Supervivencia, Creativo, Aventura, Espectador, Hardcore", lx, sy + rh * 8 + 6, mx, my);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }

}
