package com.muriane.visual_share.mixin;

import com.muriane.visual_share.func.misc.ItemDisplay;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(
            method = "handleContainerInput",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void visual_share$handleContainerInput(int containerId, int slotNum, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci){
        if (containerId == ItemDisplay.containerId){
            ci.cancel();
        }
    }
}
