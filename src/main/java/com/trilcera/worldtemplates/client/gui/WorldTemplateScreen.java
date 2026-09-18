package com.trilcera.worldtemplates.client.gui;

import com.trilcera.worldtemplates.Config;
import com.trilcera.worldtemplates.client.ClickGuard;
import com.trilcera.worldtemplates.client.ClientEvents;
import com.trilcera.worldtemplates.client.TemplateSorting;
import com.trilcera.worldtemplates.client.WorldTemplate;
import com.trilcera.worldtemplates.client.WorldTemplateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * Screen for selecting a world template.
 * Shown when clicking the configurable button on the world selection screen.
 * Templates are sorted per config (A-Z default) and the list can be
 * re-sorted live with the "Orden" button.
 */
public class WorldTemplateScreen extends Screen {
    private TemplateList templateList;
    private Button createButton;
    private Button cancelButton;
    private Button sortButton;
    private WorldTemplate selectedTemplate;
    private boolean closing;
    private final Screen parent;

    private static final Component CREATE_LABEL = Component.literal("Crear Mundo");
    private static final Component CANCEL_LABEL = Component.literal("Cancelar");

    public WorldTemplateScreen(Screen parent) {
        super(Component.literal("Seleccionar Plantilla"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // init() runs again on window resize, so no interaction state may leak
        // into it: a stale 'closing' flag makes every button ignore its click.
        this.closing = false;
        this.selectedTemplate = null;
        rebuildList();

        // Create button
        int buttonY = this.height - 50;
        int buttonWidth = 150;
        int centerX = this.width / 2;

        Component createLabel = Component.literal(Config.INSTANCE.buttonText.get());

        this.createButton = Button.builder(createLabel, this::onCreate)
                .bounds(centerX - buttonWidth - 10, buttonY, buttonWidth, 20)
                .build();
        this.createButton.active = false;
        this.addRenderableWidget(this.createButton);

        this.cancelButton = Button.builder(CANCEL_LABEL, btn -> this.onClose())
                .bounds(centerX + 10, buttonY, buttonWidth, 20)
                .build();
        this.addRenderableWidget(this.cancelButton);

        // Live "Orden" button cycling the sorting modes
        this.sortButton = Button.builder(sortLabel(), btn -> {
                    if (ClickGuard.allow("wts.sort")) {
                        TemplateSorting.cycleAndSave();
                        rebuildList();
                        this.sortButton.setMessage(sortLabel());
                    }
                })
                .bounds(centerX - 60, 42, 120, 18)
                .build();
        this.addRenderableWidget(this.sortButton);
    }

    private void rebuildList() {
        // Drop the previous list first: the "Orden" button rebuilds on every
        // click and stacking lists would render and hit-test duplicates.
        if (this.templateList != null) {
            this.removeWidget(this.templateList);
        }
        List<WorldTemplate> templates = TemplateSorting.sort(WorldTemplateManager.getTemplates());
        int listTop = 66;
        int listBottom = this.height - 80;
        this.templateList = new TemplateList(this.minecraft, this.width, this.height, listTop, listBottom, 40);
        for (WorldTemplate template : templates) {
            this.templateList.addTemplateEntry(new TemplateEntry(template));
        }
        this.addWidget(this.templateList);
    }

    private Component sortLabel() {
        return Component.literal("Orden: " + TemplateSorting.label(TemplateSorting.current()));
    }

    private void onCreate(Button button) {
        if (this.closing || selectedTemplate == null) {
            return;
        }
        // Replay guard BEFORE setting 'closing': bailing out after the flag was
        // set left the selector permanently dead (no button reacted any more).
        if (!ClickGuard.allow("wts.create")) {
            return;
        }
        this.closing = true;
        WorldTemplate template = selectedTemplate;
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new TrilceraCreateWorldScreen(template, this));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        this.templateList.render(graphics, mouseX, mouseY, partialTick);

        // Draw title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // Draw selected template info if config allows
        if (selectedTemplate != null && Config.INSTANCE.showTemplateDescriptions.get()) {
            graphics.drawCenteredString(this.font, selectedTemplate.buttonMessage(), this.width / 2, 40, 0xAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Cancel: back to the world list via {@link ClientEvents#returnToWorldList}.
     * A fresh SelectWorldScreen is used (reusing removed instances crashes
     * with "Icon already closed"); the input-replay suppression starts AFTER
     * the switch, so queued replays (Ixeris) can not re-open this screen.
     */
    @Override
    public void onClose() {
        if (this.closing) {
            return;
        }
        this.closing = true;
        ClientEvents.backToParent(this.parent);
    }

    public void setSelected(WorldTemplate template) {
        this.selectedTemplate = template;
        this.createButton.active = template != null;
    }

    /**
     * List widget for displaying templates.
     */
    private class TemplateList extends ObjectSelectionList<TemplateEntry> {
        public TemplateList(net.minecraft.client.Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
            super(minecraft, width, height, top, bottom, itemHeight);
        }

        public void addTemplateEntry(TemplateEntry entry) {
            addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 + 160;
        }
    }

    /**
     * Individual template entry in the list.
     */
    private class TemplateEntry extends ObjectSelectionList.Entry<TemplateEntry> {
        private final WorldTemplate template;

        public TemplateEntry(WorldTemplate template) {
            this.template = template;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            // Draw button-like background
            int bgColor = hovered ? 0x44446644 : 0x44222222;
            graphics.fill(left, top, left + width, top + height, bgColor);

            // Draw template icon scaled to 32x32 on screen.
            // 10-arg blit: screen size (32x32) is INDEPENDENT of the sampled
            // texture region (full icon w x h), so 64x64 world icons shrink
            // correctly instead of rendering only their top-left quarter.
            com.trilcera.worldtemplates.client.TemplateIconCache.CachedIcon iconEntry =
                    com.trilcera.worldtemplates.client.TemplateIconCache.getIcon(template);
            graphics.blit(iconEntry.location(), left + 2, top + 2, 32, 32, 0.0F, 0.0F,
                    iconEntry.width(), iconEntry.height(),
                    iconEntry.width(), iconEntry.height());
            int textX = left + 40;

            // Draw template name
            graphics.drawString(font, template.buttonMessage(), textX, top + 4, 0xFFFFFF);

            // Draw folder name
            graphics.drawString(font, Component.literal("Folder: " + template.folderName()), textX, top + 16, 0x888888);

            // Draw template location
            if (template.templateLocation() != null && !template.templateLocation().isEmpty()) {
                graphics.drawString(font, Component.literal("Source: " + template.templateLocation()), textX, top + 26, 0x666666);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            setSelected(this.template);
            templateList.setSelected(this);
            return true;
        }

        @Override
        public Component getNarration() {
            return template.buttonMessage();
        }
    }
}
