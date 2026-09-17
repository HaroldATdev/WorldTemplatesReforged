package com.trilcera.worldtemplates.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages world templates loaded from resource packs.
 * Templates are defined in assets/worldtemplates/templates.json
 */
public class WorldTemplateManager extends SimplePreparableReloadListener<List<WorldTemplate>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final ResourceLocation TEMPLATES_JSON = new ResourceLocation("worldtemplates", "templates.json");

    private static List<WorldTemplate> templates = new ArrayList<>();

    public static List<WorldTemplate> getTemplates() {
        return templates;
    }

    public static Optional<WorldTemplate> getTemplateByFolder(String folderName) {
        return templates.stream()
                .filter(t -> t.folderName().equalsIgnoreCase(folderName))
                .findFirst();
    }

    @Override
    protected List<WorldTemplate> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<WorldTemplate> loaded = new ArrayList<>();

        for (Resource resource : resourceManager.getResourceStack(TEMPLATES_JSON)) {
            try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null && root.has("templates")) {
                    JsonArray array = root.getAsJsonArray("templates");
                    for (JsonElement element : array) {
                        try {
                            WorldTemplate template = parseTemplate(element.getAsJsonObject());
                            if (template != null) {
                                loaded.add(template);
                                LOGGER.info("Loaded world template: {}", template.folderName());
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to parse template entry", e);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load templates.json", e);
            }
        }

        return loaded;
    }

    @Override
    protected void apply(List<WorldTemplate> loadedTemplates, ResourceManager resourceManager, ProfilerFiller profiler) {
        templates = loadedTemplates;
        LOGGER.info("Loaded {} world template(s)", templates.size());
    }

    private WorldTemplate parseTemplate(JsonObject json) {
        String buttonText = json.has("buttonText") ? json.get("buttonText").getAsString() : "Create World";
        String iconPath = json.has("icon") ? json.get("icon").getAsString() : "worldtemplates:textures/gui/default_template.png";
        String templateLocation = json.has("templateLocation") ? json.get("templateLocation").getAsString() : "";
        String folderName = json.has("folderName") ? json.get("folderName").getAsString() : "world";

        // Honor config overrides for name and icon
        String nameOverride = com.trilcera.worldtemplates.Config.INSTANCE.templateName.get();
        if (nameOverride != null && !nameOverride.isBlank()) {
            buttonText = nameOverride.trim();
        }
        String iconOverride = com.trilcera.worldtemplates.Config.INSTANCE.templateIcon.get();
        if (iconOverride != null && !iconOverride.isBlank()) {
            iconPath = iconOverride.trim();
        }

        Component buttonMessage = Component.literal(buttonText);
        ResourceLocation icon = new ResourceLocation(iconPath);

        Optional<String> downloadURI = Optional.empty();
        if (json.has("downloadURI") && !json.get("downloadURI").isJsonNull()) {
            downloadURI = Optional.of(json.get("downloadURI").getAsString());
        }

        return new WorldTemplate(buttonMessage, icon, templateLocation, folderName, downloadURI);
    }

    /**
     * Registers the embedded trilcera-template as the FIRST template
     * (our mod generates it, like LonKraft generates its single template).
     * Called from ClientEvents after the install check.
     */
    public static void ensureDefaultTemplate() {
        String dir = com.trilcera.worldtemplates.client.DefaultTemplateProvider.EMBEDDED_TEMPLATE_DIR;
        boolean present = templates.stream().anyMatch(t ->
                t.folderName().equalsIgnoreCase(dir)
                        || t.templateLocation().equalsIgnoreCase(dir));
        if (present) {
            return;
        }
        WorldTemplate embedded = new WorldTemplate(
                Component.literal(com.trilcera.worldtemplates.client.DefaultTemplateProvider.displayName()),
                new ResourceLocation(com.trilcera.worldtemplates.client.DefaultTemplateProvider.iconPath()),
                dir,
                dir);
        List<WorldTemplate> withDefault = new ArrayList<>();
        withDefault.add(embedded);
        withDefault.addAll(templates);
        templates = withDefault;
        LOGGER.info("Registered embedded Trilcera template as first template");
    }
}
