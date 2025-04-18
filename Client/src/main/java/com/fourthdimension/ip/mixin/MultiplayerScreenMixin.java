package com.fourthdimension.ip.mixin;

import com.fourthdimension.ip.FourthDimensionIP; // Import your main mod class
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class) // Target the MultiplayerScreen class
public abstract class MultiplayerScreenMixin {

    // Inject code at the beginning (HEAD) of the refresh() method
    @Inject(method = "refresh", at = @At("HEAD"))
    private void onRefresh(CallbackInfo ci) {
        // Call the static method from your main mod class to start the IP fetch
        FourthDimensionIP.LOGGER.info("Multiplayer screen refresh triggered, fetching IP...");
        FourthDimensionIP.fetchIpAndUpdateServerList();
    }
}