package com.trilcera.worldtemplates;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Configuration system for Trilcera World Templates.
 * Other modpack creators can customize button text, templates, and behavior.
 */
public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final Config INSTANCE;

    // Button settings
    public final ForgeConfigSpec.ConfigValue<String> buttonText;
    public final ForgeConfigSpec.ConfigValue<String> buttonTooltip;

    // Template settings
    public final ForgeConfigSpec.ConfigValue<String> defaultTemplate;
    public final ForgeConfigSpec.BooleanValue showTemplateDescriptions;
    public final ForgeConfigSpec.BooleanValue allowVanillaWorldCreation;
    public final ForgeConfigSpec.BooleanValue chooseTemplate;
    public final ForgeConfigSpec.ConfigValue<String> templateName;
    public final ForgeConfigSpec.ConfigValue<String> templateIcon;
    public final ForgeConfigSpec.ConfigValue<String> worldBaseName;
    public final ForgeConfigSpec.ConfigValue<String> defaultGameMode;
    public final ForgeConfigSpec.ConfigValue<String> templateSorting;

    // GUI settings
    public final ForgeConfigSpec.IntValue buttonPositionX;
    public final ForgeConfigSpec.IntValue buttonPositionY;
    public final ForgeConfigSpec.IntValue buttonWidth;
    public final ForgeConfigSpec.IntValue buttonHeight;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        INSTANCE = new Config(builder);
        SPEC = builder.build();
    }

    private Config(ForgeConfigSpec.Builder builder) {
        builder.comment("Trilcera World Templates Configuration")
               .push("button");

        buttonText = builder
                .comment("Text shown on the world creation button",
                         "Change this to customize the button label for your modpack")
                .define("buttonText", "Crear Trilcera");

        buttonTooltip = builder
                .comment("Tooltip shown when hovering over the button")
                .define("buttonTooltip", "Create a new world from a Trilcera template");

        buttonPositionX = builder
                .comment("X position of the button on the world selection screen",
                         "Set to -1 to center horizontally")
                .defineInRange("buttonPositionX", -1, -1, 1000);

        buttonPositionY = builder
                .comment("Y position of the button on the world selection screen")
                .defineInRange("buttonPositionY", 20, 0, 1000);

        buttonWidth = builder
                .comment("Width of the button")
                .defineInRange("buttonWidth", 200, 50, 500);

        buttonHeight = builder
                .comment("Height of the button")
                .defineInRange("buttonHeight", 20, 10, 50);

        builder.pop().push("templates");

        defaultTemplate = builder
                .comment("Default template folder to pre-select in the GUI")
                .define("defaultTemplate", "trilcera-template");

        chooseTemplate = builder
                .comment("Show the template selection screen when creating a world (YES)",
                         "Set to false to skip the screen and use the first template found")
                .define("chooseTemplate", true);

        templateName = builder
                .comment("Override for the template display name (empty = use template's own name)")
                .define("templateName", "");

        templateIcon = builder
                .comment("Override for the template icon texture",
                         "Example: worldtemplates:textures/gui/my_icon.png",
                         "Empty = use template's own icon")
                .define("templateIcon", "");

        worldBaseName = builder
                .comment("Base name for newly created worlds (Trilcera, Trilcera 2, ...)")
                .define("worldBaseName", "Trilcera");

        showTemplateDescriptions = builder
                .comment("Show template descriptions in the GUI")
                .define("showTemplateDescriptions", true);

        defaultGameMode = builder
                .comment("Game mode pre-selected when creating a world from a template",
                         "Allowed: survival, creative, adventure, spectator, hardcore",
                         "Invalid values fall back to survival")
                .define("defaultGameMode", "survival");

        templateSorting = builder
                .comment("How templates are ordered in the selector and which is 'first'",
                         "Allowed: A-Z, Z-A, NEWEST, OLDEST, ORIGINAL",
                         "A-Z = alphabetical (default), NEWEST/OLDEST = by folder date,",
                         "ORIGINAL = registration order (embedded first)")
                .define("templateSorting", "A-Z");

        allowVanillaWorldCreation = builder
                .comment("Allow creating vanilla worlds (keeps the vanilla create button)")
                .define("allowVanillaWorldCreation", false);

        builder.pop();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "worldtemplates-client.toml");
    }

    public static void save() {
        SPEC.save();
    }
}
