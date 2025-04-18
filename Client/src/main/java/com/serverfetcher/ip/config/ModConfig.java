package com.serverfetcher.ip.config;

import com.serverfetcher.ip.ServerFetcher; // Import main class to reference MOD_ID
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = ServerFetcher.MOD_ID) // Config filename will be "server-fetcher.json"
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip(count = 2) // Adds a 2-line description in the config GUI
    @ConfigEntry.Gui.RequiresRestart(false) // Indicate if restart is needed (false=no)
    public String ipFetchUrl = ""; // Default URL

    @ConfigEntry.Gui.Tooltip // Adds a 1-line description
    @ConfigEntry.Gui.RequiresRestart(false)
    public String targetServerName = "New Server"; // Default server name

    // You don't need a constructor. AutoConfig handles defaults.
    // Add more config options here if needed later.
}