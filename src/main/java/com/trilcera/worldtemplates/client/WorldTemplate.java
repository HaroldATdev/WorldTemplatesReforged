package com.trilcera.worldtemplates.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

/**
 * Represents a world template that can be selected when creating a new world.
 *
 * @param buttonMessage The text shown on the button for this template
 * @param icon The icon texture for the template button
 * @param templateLocation The path to the template data (folder or zip)
 * @param folderName The name of the world folder to create
 * @param downloadURI Optional URL to download the template from
 */
public record WorldTemplate(
        Component buttonMessage,
        ResourceLocation icon,
        String templateLocation,
        String folderName,
        Optional<String> downloadURI
) {
    /**
     * Creates a simple template without download capability.
     */
    public WorldTemplate(Component buttonMessage, ResourceLocation icon, String templateLocation, String folderName) {
        this(buttonMessage, icon, templateLocation, folderName, Optional.empty());
    }

    /**
     * Creates a template with a download URI.
     */
    public WorldTemplate(Component buttonMessage, ResourceLocation icon, String templateLocation, String folderName, String downloadURI) {
        this(buttonMessage, icon, templateLocation, folderName, Optional.ofNullable(downloadURI));
    }
}
