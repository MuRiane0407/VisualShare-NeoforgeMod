package com.muriane.visual_share.func.misc;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.muriane.visual_share.Config;
import com.muriane.visual_share.VisualShare;
import com.muriane.visual_share.key.ModKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;


@EventBusSubscriber
public class ItemDisplay {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier ITEM_DISPLAY = Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "item_display");
    public static int containerId = 32506202; // 用id确定是否不能交互，需要找一个更好的标记方法防止冲突
    public static ItemStack stack;

    @SubscribeEvent
    public static void onKey(InputEvent.Key event){
        Minecraft mc = Minecraft.getInstance();
        if (event.getAction() == InputConstants.PRESS) {
            if (event.getKey() == ModKeys.MISC_ITEM_DISPLAY.getKey().getValue()) {
                if (Config.SERVER.ENABLE_ITEM_DISPLAY.get()){
                    if (mc.level != null && mc.player != null) { // 用level来判断玩家是否已经在某个服务器中
                        if (mc.screen instanceof AbstractContainerScreen<?> inventoryScreen) {
                            Slot slot = inventoryScreen.getSlotUnderMouse();
                            if (slot != null) {
                                String prefix = Config.SERVER.ITEM_DISPLAY_PREFIX.get();
                                String subfix = Config.SERVER.ITEM_DISPLAY_SUBFIX.get();
                                String info = prefix + slot.getSlotIndex() + subfix;
                                if (Config.CLIENT.ITEM_DISPLAY_DIRECTLY_SEND.get()) {
                                    mc.player.connection.sendChat(StringUtil.trimChatMessage(StringUtils.normalizeSpace((info).trim())));
                                } else {
                                    ClipboardManager clipboard = new ClipboardManager();
                                    clipboard.setClipboard(mc.getWindow(), info);
                                    mc.player.sendOverlayMessage(Component.translatable("overlay.visual_share.misc.copy"));
                                }
                            }
                        } else if (mc.screen == null) {
                            String prefix = Config.SERVER.ITEM_DISPLAY_PREFIX.get();
                            String subfix = Config.SERVER.ITEM_DISPLAY_SUBFIX.get();
                            String info = prefix + subfix;
                            if (Config.CLIENT.ITEM_DISPLAY_DIRECTLY_SEND.get()) {
                                mc.player.connection.sendChat(StringUtil.trimChatMessage(StringUtils.normalizeSpace((info).trim())));
                            } else {
                                ClipboardManager clipboard = new ClipboardManager();
                                clipboard.setClipboard(mc.getWindow(), info);
                                mc.player.sendOverlayMessage(Component.translatable("overlay.visual_share.misc.copy"));
                            }
                        }
                    }
                }else{
                    if (mc.player != null){
                        mc.player.sendOverlayMessage(Component.translatable("overlay.visual_share.misc.item_display.not_enable"));
                    }
                }
            }
        }
    }

    // 通过刷新当前显示的物品来实现界面内展示，能用，但是怪怪的
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event){
        stack = event.getItemStack();
    }

    public static void onClickOpenContainer(CompoundTag tag){
        Player player = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;
        if (player != null && level != null) {
            String uuidStr = tag.getStringOr("uuid", "");
            if (!uuidStr.isEmpty()) {
                UUID uuid = UUID.fromString(uuidStr);
                Player player1 = level.getPlayerByUUID(uuid);

                if (player1 != null){
                    ChestMenu menu = ChestMenu.oneRow(containerId, player.getInventory());
                    menu.setItem(4, 0, stack);

                    Minecraft.getInstance().setScreen(
                            new ContainerScreen(
                                    menu,
                                    player.getInventory(),
                                    Component.translatable("container.visual_share.misc.item_display", player1.getName())
                            )
                    );
                }
            }
        }
    }

    /// 转换带标识符的信息为物品信息
    public static MutableComponent tranInfoToItemInHand(Component chat, Player player){
        String prefix = Config.SERVER.ITEM_DISPLAY_PREFIX.get();
        String suffix = Config.SERVER.ITEM_DISPLAY_SUBFIX.get();

        if (!prefix.isEmpty() && !suffix.isEmpty()){
            String marker = prefix+suffix;
            MutableComponent newChat = MutableComponent.create(Component.empty().getContents());

            for (Component component : chat.toFlatList()) {
                Component cur = component;
                String curStr = cur.getString();

                boolean end = false;
                int time = curStr.length(); // 防止死循环
                while (!cur.equals(Component.empty()) && !end && time > 0) {
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
                            CompoundTag tag = new CompoundTag();
                            tag.putString("uuid", player.getStringUUID());

                            newChat.append(Component.literal("[" + stack.getItemName().getString() + "]").withStyle(style -> style
                                    .withColor(ChatFormatting.AQUA)
                                    .withHoverEvent(new HoverEvent.ShowItem(template))
                                    .withClickEvent(new ClickEvent.Custom(ITEM_DISPLAY,
                                            Optional.of(tag)
                                    ))
                            ));
                        } else {
                            String empty = Config.SERVER.ITEM_DISPLAY_CUSTOM_EMPTY_NAME.get().isEmpty() ? Blocks.AIR.getName().getString() : Config.SERVER.ITEM_DISPLAY_CUSTOM_EMPTY_NAME.get();
                            String emptyDescription = Config.SERVER.ITEM_DISPLAY_CUSTOM_EMPTY_DESCRIPTION.get().isEmpty() ? Blocks.AIR.getName().getString() : Config.SERVER.ITEM_DISPLAY_CUSTOM_EMPTY_DESCRIPTION.get();
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
        }else{
            LOGGER.warn("Item display prefix or suffix is empty");
            return (MutableComponent) chat;
        }
    }

    public static MutableComponent tranInfoToItemInSlot(Component chat, Player player){
        String prefix = Config.SERVER.ITEM_DISPLAY_PREFIX.get();
        String suffix = Config.SERVER.ITEM_DISPLAY_SUBFIX.get();

        if (!prefix.isEmpty() && !suffix.isEmpty()){
            MutableComponent newChat = MutableComponent.create(Component.empty().getContents());

            for (Component component : chat.toFlatList()) {
                Component cur = component;
                String curStr = cur.getString();

                boolean end = false;
                int time = curStr.length(); // 防止死循环
                while (!cur.equals(Component.empty()) && !end && time > 0) {
                    int is = curStr.indexOf(prefix);
                    int ie = curStr.indexOf(suffix);
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
                                CompoundTag tag = new CompoundTag();
                                tag.putString("uuid", player.getStringUUID());

                                newChat.append(Component.literal("[" + stack.getItemName().getString() + "]").withStyle(style -> style
                                        .withColor(ChatFormatting.AQUA)
                                        .withHoverEvent(new HoverEvent.ShowItem(template))
                                        .withClickEvent(new ClickEvent.Custom(ITEM_DISPLAY,
                                                Optional.of(tag)
                                        ))
                                ));
                            } else {
                                String empty = Config.SERVER.ITEM_DISPLAY_CUSTOM_EMPTY_NAME.get().isEmpty() ? Blocks.AIR.getName().getString() : Config.SERVER.ITEM_DISPLAY_CUSTOM_EMPTY_NAME.get();
                                String emptyDescription = Config.SERVER.ITEM_DISPLAY_CUSTOM_EMPTY_DESCRIPTION.get().isEmpty() ? Blocks.AIR.getName().getString() : Config.SERVER.ITEM_DISPLAY_CUSTOM_EMPTY_DESCRIPTION.get();
                                newChat.append(Component.literal("[" + empty + "]").withStyle(style -> style
                                        .withColor(ChatFormatting.AQUA)
                                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(emptyDescription)))
                                ));
                            }
                        } else {
                            newChat.append(Component.literal(prefix + find + suffix).withStyle(cur.getStyle()));
                        }

                        curStr = curStr.substring(ie + suffix.length());
                        cur = Component.literal(curStr).withStyle(cur.getStyle());
                    } else {
                        newChat.append(cur);
                        end = true;
                    }
                    time--;
                }
            }
            return newChat;
        }else{
            LOGGER.warn("Item display prefix or suffix is empty");
            return (MutableComponent) chat;
        }
    }
}
