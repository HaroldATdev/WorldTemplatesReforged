package com.trilcera.worldtemplates.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable error screen showing a specific reason plus navigation actions:
 * Retry (optional), World list (fresh SelectWorldScreen) and Main menu.
 */
public class TrilceraErrorScreen extends Screen {
    private final Screen parent;
    private final String reason;
    private final Runnable retryAction;
    private final boolean showRetry;

    public TrilceraErrorScreen(Screen parent, String reason) {
        this(parent, reason, null);
    }

    public TrilceraErrorScreen(Screen parent, String reason, Runnable retryAction) {
        super(Component.literal("Error"));
        this.parent = parent;
        this.reason = reason == null ? "Error desconocido" : reason;
        this.retryAction = retryAction;
        this.showRetry = retryAction != null;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 + 40;
        int bw = 130;
        int gap = 10;
        int n = showRetry ? 3 : 2;
        int total = n * bw + (n - 1) * gap;
        int startX = cx - total / 2;

        int i = 0;
        if (showRetry) {
            this.addRenderableWidget(Button.builder(Component.literal("Reintentar"),
                            b -> { if (retryAction != null) retryAction.run(); })
                    .bounds(startX + i * (bw + gap), y, bw, 20).build());
            i++;
        }
        this.addRenderableWidget(Button.builder(Component.literal("Lista de mundos"),
                        b -> Minecraft.getInstance().setScreen(new SelectWorldScreen(new TitleScreen())))
                .bounds(startX + i * (bw + gap), y, bw, 20).build());
        i++;
        this.addRenderableWidget(Button.builder(Component.literal("Menu principal"),
                        b -> Minecraft.getInstance().setScreen(new TitleScreen()))
                .bounds(startX + i * (bw + gap), y, bw, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        g.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFF5555);

        // Wrap the reason text
        int maxW = this.width - 80;
        List<String> lines = wrap(reason, maxW);
        int y = this.height / 2 - 40;
        for (String line : lines) {
            g.drawCenteredString(this.font, line, this.width / 2, y, 0xFFFFFF);
            y += 12;
        }
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            String test = sb.length() == 0 ? word : sb + " " + word;
            if (this.font.width(test) > maxWidth && sb.length() > 0) {
                lines.add(sb.toString());
                sb = new StringBuilder(word);
            } else {
                sb = new StringBuilder(test);
            }
        }
        if (sb.length() > 0) {
            lines.add(sb.toString());
        }
        return lines;
    }

    @Override
    public void onClose() {
        Minecraft mc = this.minecraft != null ? this.minecraft : Minecraft.getInstance();
        if (this.parent != null) {
            mc.setScreen(this.parent);
        } else {
            mc.setScreen(new SelectWorldScreen(new TitleScreen()));
        }
    }
}