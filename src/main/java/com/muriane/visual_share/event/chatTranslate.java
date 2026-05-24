package com.muriane.visual_share.event;

import com.muriane.visual_share.Config;
import com.muriane.visual_share.func.misc.EnderChestDisplay;
import com.muriane.visual_share.func.misc.InventoryDisplay;
import com.muriane.visual_share.func.misc.ItemDisplay;
import com.muriane.visual_share.func.screenshot.Screenshot;
import com.muriane.visual_share.func.structure_view.Structure;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

@EventBusSubscriber
public class chatTranslate {
    /// 调取信息转换函数
    @SubscribeEvent
    public static void onServerReceivedChat(ServerChatEvent event){
        MutableComponent newChat = (MutableComponent) event.getMessage();

        if (Config.SERVER.ENABLE_SCREENSHOT_SHARE.get()){
            newChat = Screenshot.tranInfoToImage(newChat);
        }
        if (Config.SERVER.ENABLE_STRUCTURE_SHARE.get()){
            newChat = Structure.tranInfoToStructure(newChat);
        }
        if (Config.SERVER.ENABLE_ITEM_DISPLAY.get()) {
            newChat = ItemDisplay.tranInfoToItemInHand(newChat, event.getPlayer());
            newChat = ItemDisplay.tranInfoToItemInSlot(newChat, event.getPlayer());
        }
        if (Config.SERVER.ENABLE_INVENTORY_DISPLAY.get()){
            newChat = InventoryDisplay.tranInfoToInventory(newChat, event.getPlayer());
        }
        if (Config.SERVER.ENABLE_ENDER_CHEST_DISPLAY.get()){
            newChat = EnderChestDisplay.tranInfoToEnderChest(newChat, event.getPlayer());
        }

        event.setMessage(newChat);
    }
}
