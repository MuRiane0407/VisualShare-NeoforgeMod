package com.muriane.visual_share.func.misc;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.muriane.visual_share.Config;
import com.muriane.visual_share.func.screenshot.ScreenshotScreen;
import com.muriane.visual_share.key.ModKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.apache.commons.lang3.StringUtils;

@EventBusSubscriber
public class ItemShow {
    @SubscribeEvent
    public static void onKey(InputEvent.Key event){
        Minecraft mc = Minecraft.getInstance();
        if (event.getAction() == InputConstants.PRESS) {
            if (event.getKey() == ModKeys.MISC_ITEM_SHOW.getKey().getValue()) {
                if (mc.level != null && mc.player != null && mc.screen instanceof AbstractContainerScreen<?> inventoryScreen) { // 用level来判断玩家是否已经在某个服务器中
                    Slot slot = inventoryScreen.getSlotUnderMouse();
                    if (slot != null) {
                        String prefix = Config.SERVER.ITEM_SHOW_PREFIX.get();
                        String subfix = Config.SERVER.ITEM_SHOW_SUBFIX.get();
                        String info = prefix+slot.getSlotIndex()+subfix;
                        if (Config.CLIENT.ITEM_SHOW_DIRECTLY_SEND.get()) {
                            mc.player.connection.sendChat(StringUtil.trimChatMessage(StringUtils.normalizeSpace((info).trim())));
                        }else{
                            ClipboardManager clipboard = new ClipboardManager();
                            clipboard.setClipboard(mc.getWindow(), info);
                        }
                    }
                }
            }
        }
    }

    /// 转换带标识符的信息为物品信息
    public static MutableComponent tranInfoToItemInSlot(Component chat, Player player){
        String prefix = Config.SERVER.ITEM_SHOW_PREFIX.get();
        String subfix = Config.SERVER.ITEM_SHOW_SUBFIX.get();

        MutableComponent newChat = MutableComponent.create(Component.empty().getContents());
        for (Component component : chat.toFlatList()){
            Component cur = component;
            String curStr = cur.getString();

            boolean end = false;
            int time = curStr.length(); // 防止死循环
            while (!cur.equals(Component.empty()) && !end && time > 0){
                int is = curStr.indexOf(prefix);
                int ie = curStr.indexOf(subfix);
                if (is != -1 && ie != -1) {
                    newChat.append(Component.literal(curStr.substring(0, is)).withStyle(cur.getStyle()));

                    String find = curStr.substring(is + prefix.length(), ie);
                    int slot;
                    try {
                        slot = Integer.parseInt(find);
                    } catch (NumberFormatException e) {
                        slot = -1;
                    }
                    if (slot >= 0 && slot < player.getInventory().getContainerSize()) {
                        ItemStack stack = player.getInventory().getItem(slot);
                        ItemStackTemplate template;
                        if (stack != ItemStack.EMPTY) {
                            template = new ItemStackTemplate(
                                    stack.getItem(),
                                    stack.getCount(),
                                    stack.getComponentsPatch()
                            );
                            newChat.append(Component.literal("[" + stack.getItemName().getString() + "]").withStyle(style -> style
                                    .withColor(ChatFormatting.AQUA)
                                    .withHoverEvent(new HoverEvent.ShowItem(template))
                            ));
                        } else {
                            String empty = Config.SERVER.ITEM_SHOW_CUSTOM_EMPTY_NAME.get().isEmpty() ? Blocks.AIR.getName().getString() : Config.SERVER.ITEM_SHOW_CUSTOM_EMPTY_NAME.get();
                            String emptyDescription = Config.SERVER.ITEM_SHOW_CUSTOM_EMPTY_DESCRIPTION.get().isEmpty() ? Blocks.AIR.getName().getString() : Config.SERVER.ITEM_SHOW_CUSTOM_EMPTY_DESCRIPTION.get();
                            newChat.append(Component.literal("[" + empty + "]").withStyle(style -> style
                                    .withColor(ChatFormatting.AQUA)
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal(emptyDescription)))
                            ));
                        }
                    }else{
                        newChat.append(Component.literal(prefix + find + subfix).withStyle(cur.getStyle()));
                    }

                    curStr = curStr.substring(ie + subfix.length());
                    cur = Component.literal(curStr).withStyle(cur.getStyle());
                } else {
                    newChat.append(cur);
                    end = true;
                }
                time--;
            }
        }

        return newChat;
    }

    public static MutableComponent tranInfoToItemInHand(Component chat, Player player){
        String marker = Config.SERVER.ITEM_SHOW_PREFIX.get()+Config.SERVER.ITEM_SHOW_SUBFIX.get();

        MutableComponent newChat = MutableComponent.create(Component.empty().getContents());
        for (Component component : chat.toFlatList()){
            Component cur = component;
            String curStr = cur.getString();

            boolean end = false;
            int time = curStr.length(); // 防止死循环
            while (!cur.equals(Component.empty()) && !end && time > 0){
                int i = curStr.indexOf(marker);
                if (i != -1) {
                    newChat.append(Component.literal(curStr.substring(0, i)).withStyle(cur.getStyle()));

                    ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
                    ItemStackTemplate template;
                    if (stack != ItemStack.EMPTY) {
                        template = new ItemStackTemplate(
                                stack.getItem(),
                                stack.getCount(),
                                stack.getComponentsPatch()
                        );
                        newChat.append(Component.literal("[" + stack.getItemName().getString() + "]").withStyle(style -> style
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent.ShowItem(template))
                        ));
                    }else{
                        String empty = Config.SERVER.ITEM_SHOW_CUSTOM_EMPTY_NAME.get().isEmpty() ? Blocks.AIR.getName().getString() : Config.SERVER.ITEM_SHOW_CUSTOM_EMPTY_NAME.get();
                        String emptyDescription = Config.SERVER.ITEM_SHOW_CUSTOM_EMPTY_DESCRIPTION.get().isEmpty() ? Blocks.AIR.getName().getString() : Config.SERVER.ITEM_SHOW_CUSTOM_EMPTY_DESCRIPTION.get();
                        newChat.append(Component.literal("[" + empty + "]").withStyle(style -> style
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal(emptyDescription)))
                        ));
                    }

                    curStr = curStr.substring(i + marker.length());
                    cur = Component.literal(curStr).withStyle(cur.getStyle());
                } else {
                    newChat.append(cur);
                    end = true;
                }
                time--;
            }
        }

        return newChat;
    }
}
