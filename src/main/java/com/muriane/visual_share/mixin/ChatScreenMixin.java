package com.muriane.visual_share.mixin;

import com.mojang.logging.LogUtils;
import com.muriane.visual_share.func.misc.EnderChestDisplay;
import com.muriane.visual_share.func.misc.InventoryDisplay;
import com.muriane.visual_share.func.misc.ItemDisplay;
import com.muriane.visual_share.func.structure_view.Structure;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Unique
    private static final Logger visual_share$LOGGER = LogUtils.getLogger();

    @Inject(method = "handleComponentClicked",
            at = @At(value = "HEAD"),
            cancellable = true)
    public void visual_share$handleComponentClicked(Style clicked, boolean allowInsertions, CallbackInfoReturnable<Boolean> cir){
        ClickEvent event = clicked.getClickEvent();
        if (event instanceof ClickEvent.Custom(Identifier id, Optional<Tag> payload)) {
            if (id.equals(ItemDisplay.ITEM_DISPLAY)){
                if (payload.isPresent()){
                    ItemDisplay.onClickOpenContainer((CompoundTag) payload.get());
                }else{
                    visual_share$LOGGER.warn("Empty tag in item display");
                }
                cir.setReturnValue(true);
            }else if (id.equals(InventoryDisplay.INVENTORY_DISPLAY)){
                if (payload.isPresent()){
                    InventoryDisplay.onClickOpenContainer((CompoundTag) payload.get());
                }else{
                    visual_share$LOGGER.warn("Empty tag in inventory display");
                }
                cir.setReturnValue(true);
            }else if (id.equals(EnderChestDisplay.ENDER_CHEST_DISPLAY)){
                if (payload.isPresent()){
                    EnderChestDisplay.onClickOpenContainer((CompoundTag) payload.get());
                }else{
                    visual_share$LOGGER.warn("Empty tag in ender chest display");
                }
                cir.setReturnValue(true);
            }else if (id.equals(Structure.STRUCTURE)){
                if (payload.isPresent()){
                    Structure.onClickStructureInfo((CompoundTag) payload.get());
                }else{
                    visual_share$LOGGER.warn("Empty tag in structure share");
                }
                cir.setReturnValue(true);
            }
        }
    }
}
