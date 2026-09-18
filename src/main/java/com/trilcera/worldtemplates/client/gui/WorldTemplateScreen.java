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

    /*
     * Vertical rhythm of the header, top -> bottom:
     *   title (y = 15)  ->  selected template name  ->  "Orden" button  ->  list
     * Every step leaves room for a 9 px font line / an 18 px button plus a gap,
     * so the header never overlaps itself at any GUI scale.
     */
    /** Y of the screen title. */
    private static final int HEADER_TOP = 15;
    /** Distance from the title baseline row to the selected-template name row. */
    private static final int SELECTED_NAME_OFFSET = 17;
    /** Distance from the selected-template name row to the top of the button. */
    private static final int SORT_BUTTON_OFFSET = 18;
    private static final int SORT_BUTTON_WIDTH = 120;
    private static final int SORT_BUTTON_HEIGHT = 18;
    /** Gap between the "Orden" button and the top of the template list. */
    private static final int LIST_GAP = 6;
    /** Space kept below the list for the create/cancel row. */
    private static final int LIST_BOTTOM_MARGIN = 80;
    private static final int LIST_ROW_HEIGHT = 40;

    // Layout resolved by computeLayout(); the render pass reuses it so the
    // drawn text and the widgets always agree.
    private int titleY = HEADER_TOP;
    private int selectedNameY = HEADER_TOP + SELECTED_NAME_OFFSET;
    private int sortButtonY = HEADER_TOP + SELECTED_NAME_OFFSET + SORT_BUTTON_OFFSET;
    private int listTop = HEADER_TOP + SELECTED_NAME_OFFSET + SORT_BUTTON_OFFSET
            + SORT_BUTTON_HEIGHT + LIST_GAP;

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
        computeLayout();
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
                .bounds(centerX - (SORT_BUTTON_WIDTH / 2), this.sortButtonY,
                        SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(this.sortButton);
    }

    /**
     * Resolves the header/list geometry for the current window size.
     * The header is anchored to the top and the list gets whatever space is left
     * above the create/cancel row. Minecraft clamps the scaled window to
     * 320x240, so the 74 px header always leaves room for a couple of rows.
     */
    private void computeLayout() {
        this.titleY = HEADER_TOP;
        this.selectedNameY = this.titleY + SELECTED_NAME_OFFSET;
        this.sortButtonY = this.selectedNameY + SORT_BUTTON_OFFSET;
        this.listTop = this.sortButtonY + SORT_BUTTON_HEIGHT + LIST_GAP;
    }

    private void rebuildList() {
        // Drop the previous list first: the "Orden" button rebuilds on every
        // click and stacking lists would render and hit-test duplicates.
        if (this.templateList != null) {
            this.removeWidget(this.templateList);
        }
        List<WorldTemplate> templates = TemplateSorting.sort(WorldTemplateManager.getTemplates());
        int listTop = this.listTop;
        int listBottom = this.height - LIST_BOTTOM_MARGIN;
        this.templateList = new TemplateList(this.minecraft, this.width, this.height, listTop, listBottom, LIST_ROW_HEIGHT);
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

        // Draw title, then the selected template name below it. The "Orden"
        // button is drawn afterwards by super.render() and sits a further 18 px
        // down, so it never covers either text.
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.titleY, 0xFFFFFF);

        // Draw selected template info if config allows
        if (selectedTemplate != null && Config.INSTANCE.showTemplateDescriptions.get()) {
            graphics.drawCenteredString(this.font, selectedTemplate.buttonMessage(), this.width / 2, this.selectedNameY, 0xAAAAAA);
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
