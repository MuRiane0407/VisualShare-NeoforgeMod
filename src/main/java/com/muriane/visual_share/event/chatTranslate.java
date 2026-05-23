package com.muriane.visual_share.event;

import com.muriane.visual_share.Config;
import com.muriane.visual_share.func.misc.EnderChestShow;
import com.muriane.visual_share.func.misc.InventoryShow;
import com.muriane.visual_share.func.misc.ItemShow;
import com.muriane.visual_share.func.screenshot.Screenshot;
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

        if (Config.SERVER.ENABLE_ITEM_SHOW.get()) {
            newChat = ItemShow.tranInfoToItemInHand(newChat, event.getPlayer());
            newChat = ItemShow.tranInfoToItemInSlot(newChat, event.getPlayer());
        }
        if (Config.SERVER.ENABLE_INVENTORY_SHOW.get()){
            newChat = InventoryShow.tranInfoToInventory(newChat, event.getPlayer());
        }
        if (Config.SERVER.ENABLE_ENDER_CHEST_SHOW.get()){
            newChat = EnderChestShow.tranInfoToEnderChest(newChat, event.getPlayer());
        }
        newChat = Screenshot.tranInfoToImage(newChat);

        event.setMessage(newChat);
    }
}
