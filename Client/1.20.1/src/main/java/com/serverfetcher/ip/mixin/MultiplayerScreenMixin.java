package com.serverfetcher.ip.mixin;

import com.serverfetcher.ip.ServerFetcher; // Import your main mod class
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class) // Target the MultiplayerScreen class
public abstract class MultiplayerScreenMixin {

    // Inject code at the end (TAIL) of the refresh() method
    @Inject(method = "refresh", at = @At("TAIL"))
    private void onRefreshTail(CallbackInfo ci) {
        // Call the static method from your main mod class to start the IP fetch
        ServerFetcher.LOGGER.info("Multiplayer screen refresh triggered, fetching IP...");
        ServerFetcher.fetchIpAndUpdateServerList();
    }
}