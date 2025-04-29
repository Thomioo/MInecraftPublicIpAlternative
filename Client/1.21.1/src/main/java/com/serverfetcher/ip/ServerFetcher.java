package com.serverfetcher.ip;

// --- Keep ALL existing imports ---
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.network.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import com.serverfetcher.ip.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import com.serverfetcher.ip.mixin.MultiplayerScreenInvoker;

public class ServerFetcher implements ClientModInitializer {

    // --- Keep these static ---
    public static final String MOD_ID = "serverfetcher"; // Define MOD_ID here
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID); // Use MOD_ID for logger
    public static ModConfig CONFIG;
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static String sanitizeServerAddress(String address) {
        if (address == null) {
            return "";
        }
        // Replace ':' and '/' with '_'
        // Add other replacements if needed (e.g., '<', '>', '|', '?', '*')
        return address.split(":")[0];
    }

    @Override
    public void onInitializeClient() {
        // --- Register and load the configuration ---
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new); // Choose your serializer
        CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

        // Update logging to show configured values
        LOGGER.info("Server Fetcher Mod Initialized! Using config:");
        LOGGER.info(" - Server Name: {}", CONFIG.targetServerName);
        LOGGER.info(" - Fetch URL: {}", CONFIG.ipFetchUrl);
        // Fetch IP on initial startup
        fetchIpAndUpdateServerList(); // Call the static method

        // --- Keep existing server join/disconnect logging ---
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String serverAddressString = getServerAddress(client); // This can remain non-static for now
            if (serverAddressString != null) {
                LOGGER.info("Joined server: {}", serverAddressString);
            } else {
                LOGGER.info("Joined a world (likely singleplayer or unknown address).");
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            String serverAddressString = getServerAddress(client); // This can remain non-static for now
            if (serverAddressString != null) {
                LOGGER.info("Disconnected from server: {}", serverAddressString);
            } else {
                LOGGER.info("Disconnected from world.");
            }
        });
    }

    // --- Make this method PUBLIC and STATIC ---
    public static void fetchIpAndUpdateServerList() {
        String urlToFetch = CONFIG.ipFetchUrl; // Get from config
        String serverName = CONFIG.targetServerName; // Get from config

        // Add checks
        if (urlToFetch == null || urlToFetch.trim().isEmpty()) {
            LOGGER.warn("IP Fetch URL is empty in config, skipping fetch.");
            return;
        }
        if (serverName == null || serverName.trim().isEmpty()) {
            LOGGER.warn("Target Server Name is empty in config, skipping update/add.");
            return;
        }

        // Keep the existing implementation, it already handles logging etc.
        // It accesses only static fields (LOGGER, IP_FETCH_URL, httpClient)
        // or gets instance methods like MinecraftClient.getInstance()
        LOGGER.info("Attempting to fetch server IP from: {}", urlToFetch);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlToFetch))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Server-Fetcher-MinecraftMod/1.0")
                    .GET()
                    .build();

            CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(request,
                    HttpResponse.BodyHandlers.ofString());

            future.whenCompleteAsync((response, throwable) -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null) {
                    LOGGER.error("MinecraftClient was null when processing IP fetch response.");
                    return;
                }
                Executor clientExecutor = client;

                if (throwable != null) {
                    clientExecutor.execute(() -> {
                        LOGGER.error("Failed to fetch IP from URL: {}", throwable.getMessage());
                    });
                } else {
                    if (response.statusCode() == 200) {
                        String fetchedIpRaw = response.body().trim().replace("\"", "");
                        if (fetchedIpRaw.isEmpty()) {
                            clientExecutor.execute(() -> {
                                LOGGER.warn("Fetched IP from URL was empty.");
                            });
                        } else {
                            String finalIp = fetchedIpRaw;
                            if (finalIp.startsWith("\"") && finalIp.endsWith("\"") && finalIp.length() >= 2) {
                                finalIp = finalIp.substring(1, finalIp.length() - 1);
                                LOGGER.info("Removed surrounding quotes from fetched IP.");
                            }

                            if (finalIp.isEmpty()) {
                                clientExecutor.execute(() -> {
                                    LOGGER.warn("IP address was empty after processing: {}", fetchedIpRaw);
                                });
                            } else {
                                String finalIpToUpdate = finalIp; // Use effectively final variable
                                String nameToUpdate = CONFIG.targetServerName; // Get name again just before scheduling
                                clientExecutor.execute(() -> {
                                    LOGGER.info("Successfully processed IP: {}", finalIpToUpdate);
                                    updateOrAddServer(nameToUpdate, finalIpToUpdate); // Pass BOTH name and IP
                                });
                            }
                        }
                    } else {
                        clientExecutor.execute(() -> {
                            LOGGER.warn("Failed to fetch IP: URL returned status code {}", response.statusCode());
                        });
                    }
                }
            });

        } catch (URISyntaxException e) {
            LOGGER.error("Invalid URL syntax in config: {}", urlToFetch, e); // Include URL in log
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred while setting up the IP fetch request", e);
        }
    }

    private static void renameXaeroFolders(String oldAddress, String newAddress) {
        if (oldAddress == null || newAddress == null || oldAddress.equalsIgnoreCase(newAddress)) {
            LOGGER.debug("Skipping Xaero folder rename: Addresses are null or the same.");
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            LOGGER.warn("Cannot rename Xaero folders: MinecraftClient is null.");
            return;
        }

        // Sanitize addresses for folder names
        String sanitizedOld = sanitizeServerAddress(oldAddress);
        String sanitizedNew = sanitizeServerAddress(newAddress);
        if (sanitizedOld.isEmpty() || sanitizedNew.isEmpty()) {
            LOGGER.warn("Skipping Xaero folder rename: Sanitized address is empty (Old: '{}', New: '{}')", sanitizedOld,
                    sanitizedNew);
            return;
        }

        Path runDir = client.runDirectory.toPath(); // Get Minecraft instance directory path

        // --- Define Base Paths based on user's description ---
        // IMPORTANT: Ensure these base directories actually exist where Xaero's saves
        // them in the user's setup!
        Path xaeroMinimapBase = runDir.resolve("xaero").resolve("minimap");
        Path xaeroWorldMapBase = runDir.resolve("world-map"); // Assuming 'world-map' is directly in runDir based on
                                                              // description

        // --- Construct full old and new paths ---
        String folderPrefix = "Multiplayer_";
        Path oldMinimapPath = xaeroMinimapBase.resolve(folderPrefix + sanitizedOld);
        Path newMinimapPath = xaeroMinimapBase.resolve(folderPrefix + sanitizedNew);
        Path oldWorldMapPath = xaeroWorldMapBase.resolve(folderPrefix + sanitizedOld);
        Path newWorldMapPath = xaeroWorldMapBase.resolve(folderPrefix + sanitizedNew);

        LOGGER.info("Attempting to rename Xaero folders for IP change from '{}' to '{}'", oldAddress, newAddress);

        // --- Rename Minimap Folder ---
        renameSingleXaeroFolder(oldMinimapPath, newMinimapPath, "Minimap");

        // --- Rename World Map Folder ---
        renameSingleXaeroFolder(oldWorldMapPath, newWorldMapPath, "World Map");
    }

    /**
     * Helper method to rename a single Xaero data folder.
     * Includes checks for existence and potential conflicts.
     */
    private static void renameSingleXaeroFolder(Path oldPath, Path newPath, String mapType) {
        try {
            // 1. Check if the OLD folder exists
            if (!Files.isDirectory(oldPath)) { // Use isDirectory to be sure it's not just a file
                LOGGER.debug("Xaero {} folder for old IP does not exist, skipping rename: {}", mapType, oldPath);
                return;
            }

            // 2. Check if the NEW folder *already* exists (potential conflict)
            if (Files.exists(newPath)) {
                // Decide how to handle conflict: Log warning and abort (safest)
                LOGGER.warn(
                        "Xaero {} folder for new IP already exists! Cannot rename automatically to avoid data loss: {}",
                        mapType, newPath);
                // Alternative (Risky - Deletes existing data):
                // LOGGER.warn("Xaero {} folder for new IP already exists! Deleting existing
                // folder: {}", mapType, newPath);
                // try { Files.delete(newPath); } catch (IOException eDel) {
                // LOGGER.error("Failed to delete conflicting folder {}", newPath, eDel);
                // return; }
                return; // Abort rename
            }

            // 3. Perform the rename (move)
            Files.move(oldPath, newPath); // Throws IOException on failure
            LOGGER.info("Successfully renamed Xaero {} folder: {} -> {}", mapType, oldPath.getFileName(),
                    newPath.getFileName());

        } catch (IOException e) {
            LOGGER.error("Failed to rename Xaero {} folder from {} to {}: {}", mapType, oldPath.getFileName(),
                    newPath.getFileName(), e.getMessage());
        } catch (Exception e) { // Catch any other unexpected errors
            LOGGER.error("Unexpected error while renaming Xaero {} folder: {}", mapType, oldPath.getFileName(), e);
        }
    }

    // --- Make this method STATIC as it's called by the static fetch method ---
    // It only uses static LOGGER or gets instances like
    // MinecraftClient.getInstance()
    private static void updateOrAddServer(String serverName, String fetchedIp) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            /* ... */ return;
        }

        ServerList serverList = new ServerList(client);
        if (serverList == null) {
            /* ... */ return;
        }

        try {
            serverList.loadFile();
            ServerInfo targetServer = null;
            int targetServerIndex = -1;
            boolean needsSave = false;

            for (int i = 0; i < serverList.size(); i++) {
                ServerInfo currentServer = serverList.get(i);
                if (currentServer != null && serverName.equalsIgnoreCase(currentServer.name)) {
                    targetServer = currentServer;
                    targetServerIndex = i;
                    break;
                }
            }

            if (targetServer != null) {
                // --- IP Update Logic ---
                if (!fetchedIp.equalsIgnoreCase(targetServer.address)) {
                    // Store the OLD address BEFORE updating
                    String oldAddress = targetServer.address; // <<< STORE OLD IP

                    LOGGER.info("Updating IP for server '{}' from '{}' to '{}'", serverName, oldAddress, fetchedIp);
                    targetServer.address = fetchedIp; // Update the ServerInfo object
                    serverList.set(targetServerIndex, targetServer);

                    // --- CALL THE RENAMING METHOD ---
                    renameXaeroFolders(oldAddress, fetchedIp); // <<< CALL RENAME
                    // --------------------------------

                    needsSave = true;
                } else {
                    LOGGER.info("Server '{}' already has the correct IP ({}). No update needed.", serverName,
                            fetchedIp);
                }
                // --- End IP Update Logic ---
            } else {
                // --- Add New Server Logic ---
                LOGGER.info("Adding server '{}' with IP '{}' to the list.", serverName, fetchedIp);
                ServerInfo newServerInfo = new ServerInfo(serverName, fetchedIp, ServerInfo.ServerType.OTHER);
                serverList.add(newServerInfo, false);
                needsSave = true;
                // --- End Add New Server Logic ---
            }

            if (needsSave) {
                serverList.saveFile(); // Save the server list AFTER potential rename attempt
                LOGGER.info("Server list saved.");
                // Trigger UI refresh if MultiplayerScreen is open
                refreshMultiplayerScreenIfOpen();
            }

        } catch (Exception e) {
            LOGGER.error("Failed to update or add server entry ('{}') in the server list.", serverName, e);
        }
    }

    public static void refreshMultiplayerScreenIfOpen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen instanceof MultiplayerScreen multiplayerScreen) {
            ((MultiplayerScreenInvoker) multiplayerScreen).callRefresh();
            LOGGER.info("Triggered MultiplayerScreen refresh and forced server list reload after IP update.");
        }
    }

    // --- This helper method can remain non-static if only used by instance methods
    // ---
    // If needed by static methods, it would also have to be static or rewritten.
    // In this case, it's only used by the join/disconnect listeners which are
    // instance-based.
    private String getServerAddress(MinecraftClient client) {
        // ... (Implementation remains the same) ...
        if (client == null)
            return null;

        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null && serverInfo.address != null && !serverInfo.address.isEmpty()) {
            return serverInfo.address;
        }

        if (client.getNetworkHandler() != null) {
            ClientConnection connection = client.getNetworkHandler().getConnection();
            if (connection != null) {
                SocketAddress socketAddress = connection.getAddress();
                if (socketAddress instanceof InetSocketAddress inetAddress) {
                    return inetAddress.getHostString() + ":" + inetAddress.getPort();
                } else if (socketAddress != null) {
                    return socketAddress.toString();
                }
            }
        }
        return null;
    }
}