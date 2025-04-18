package com.serverfetcher.ip.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> {
            // Get the config holder and the current config instance
            var configHolder = AutoConfig.getConfigHolder(ModConfig.class);
            var config = configHolder.getConfig();

            // Start building the screen manually
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    // Use the translation key for the title
                    .setTitle(Text.translatable("text.autoconfig.server-fetcher.title"))
                    // Define what happens when "Save" is clicked
                    .setSavingRunnable(configHolder::save);

            // Get the entry builder to create config fields
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // Get or create the "General" category
            ConfigCategory general = builder.getOrCreateCategory(
                    // Use the translation key for the category name
                    Text.translatable("text.autoconfig.server-fetcher.category.default")
            );

            // --- Add the Target Server Name entry ---
            general.addEntry(entryBuilder.startStrField(
                            // Use the translation key for the option name
                            Text.translatable("text.autoconfig.server-fetcher.option.targetServerName"),
                            config.targetServerName // Current value
                    )
                    .setDefaultValue("New Server") // Default value shown by Cloth
                    .setTooltip( // Set tooltip using translation keys
                            Text.translatable("text.autoconfig.server-fetcher.option.targetServerName.tooltip")
                    )
                    // Define what happens when the value is saved in the GUI
                    .setSaveConsumer(newValue -> config.targetServerName = newValue)
                    .build()); // Finish building this entry


            // --- Add the IP Fetch URL entry ---
            general.addEntry(entryBuilder.startStrField(
                            // Use the translation key for the option name
                            Text.translatable("text.autoconfig.server-fetcher.option.ipFetchUrl"),
                            config.ipFetchUrl // Current value
                    )
                    .setDefaultValue("https://drum-massive-directly.ngrok-free.app/ip") // Default value
                    .setTooltip( // Set multi-line tooltip using translation keys
                            Text.translatable("text.autoconfig.server-fetcher.option.ipFetchUrl.tooltip[0]"),
                            Text.translatable("text.autoconfig.server-fetcher.option.ipFetchUrl.tooltip[1]")
                    )
                    // Define what happens when the value is saved
                    .setSaveConsumer(newValue -> config.ipFetchUrl = newValue)
                    .build()); // Finish building this entry

            // Return the built screen
            return builder.build();
        };
    }
}