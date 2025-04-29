package com.serverfetcher.ip.mixin;

import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiplayerScreen.class)
public interface MultiplayerScreenInvoker {
    @Invoker("refresh")
    void callRefresh();
}