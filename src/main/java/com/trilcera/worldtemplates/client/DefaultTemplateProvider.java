package com.trilcera.worldtemplates.client;

import com.mojang.logging.LogUtils;
import com.trilcera.worldtemplates.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Default (embedded) template shipped with this mod.
 * Handles the 'trilcera-template' world and acts as the fallback when
 * the template selection screen is disabled (chooseTemplate = No),
 * i.e. the FIRST template our mod generates.
 */
public final class DefaultTemplateProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String EMBEDDED_TEMPLATE_DIR = "trilcera-template";
    public static final String DEFAULT_DISPLAY_NAME = "Trilcera";
    public static final String DEFAULT_ICON = "worldtemplates:textures/gui/default_template.png";

    private DefaultTemplateProvider() {
    }

    /** Display name honoring the templateName config override. */
    public static String displayName() {
        String override = Config.INSTANCE.templateName.get();
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        return DEFAULT_DISPLAY_NAME;
    }

    /** Icon path honoring the templateIcon config override. */
    public static String iconPath() {
        String override = Config.INSTANCE.templateIcon.get();
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        return DEFAULT_ICON;
    }

    /** Ensures the embedded template is installed and returns its effective root. */
    public static Path ensureInstalled(Path gameDir) throws IOException {
        try {
            TemplateInstaller.ensureInstalled(gameDir);
        } catch (IOException e) {
            LOGGER.error("[Trilcera Templates] Could not install embedded template", e);
            throw e;
        }
        return TemplateInstaller.resolveTemplateRoot(gameDir);
    }
}
