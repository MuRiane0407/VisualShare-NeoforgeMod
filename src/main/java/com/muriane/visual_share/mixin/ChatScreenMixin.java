package com.muriane.visual_share.mixin;

import com.mojang.logging.LogUtils;
import com.muriane.visual_share.func.misc.EnderChestShow;
import com.muriane.visual_share.func.misc.InventoryShow;
import com.muriane.visual_share.func.misc.ItemShow;
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
            if (id.equals(ItemShow.ITEM_SHOW)){
                if (payload.isPresent()){
                    ItemShow.onClickOpenContainer((CompoundTag) payload.get());
                }else{
                    visual_share$LOGGER.warn("Empty tag in item show");
                }
                cir.setReturnValue(true);
            }else if (id.equals(InventoryShow.INVENTORY_SHOW)){
                if (payload.isPresent()){
                    InventoryShow.onClickOpenContainer((CompoundTag) payload.get());
                }else{
                    visual_share$LOGGER.warn("Empty tag in inventory show");
                }
                cir.setReturnValue(true);
            }else if (id.equals(EnderChestShow.ENDER_CHEST_SHOW)){
                if (payload.isPresent()){
                    EnderChestShow.onClickOpenContainer((CompoundTag) payload.get());
                }else{
                    visual_share$LOGGER.warn("Empty tag in ender chest show");
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
