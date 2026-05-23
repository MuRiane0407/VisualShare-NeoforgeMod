package com.muriane.visual_share.func.misc;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.muriane.visual_share.Config;
import com.muriane.visual_share.VisualShare;
import com.muriane.visual_share.key.ModKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Optional;

@EventBusSubscriber
public class InventoryShow {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier INVENTORY_SHOW = Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "inventory_show");

    @SubscribeEvent
    public static void onKey(InputEvent.Key event){
        Minecraft mc = Minecraft.getInstance();
        if (event.getAction() == InputConstants.PRESS) {
            if (event.getKey() == ModKeys.MISC_INVENTORY_SHOW.getKey().getValue()) {
                if (Config.SERVER.ENABLE_INVENTORY_SHOW.get()){
                    if (mc.level != null && mc.player != null) { // 用level来判断玩家是否已经在某个服务器中
                        String marker = Config.SERVER.INVENTORY_SHOW_MARKER.get();
                        if (Config.CLIENT.ITEM_SHOW_DIRECTLY_SEND.get()) {
                            mc.player.connection.sendChat(StringUtil.trimChatMessage(StringUtils.normalizeSpace((marker).trim())));
                        } else {
                            ClipboardManager clipboard = new ClipboardManager();
                            clipboard.setClipboard(mc.getWindow(), marker);
                            mc.player.sendOverlayMessage(Component.translatable("overlay.visual_share.misc.copy"));
                        }
                    }
                }else{
                    if (mc.player != null){
                        mc.player.sendOverlayMessage(Component.translatable("overlay.visual_share.misc.inventory_show.not_enable"));
                    }
                }
            }
        }
    }

    public static void onClickOpenContainer(CompoundTag tag) {
        String uuidStr = tag.getStringOr("uuid", "");
        if (!uuidStr.isEmpty()) {
            ClientPacketDistributor.sendToServer(new MiscPayload.OpenContainerData(uuidStr, MiscPayload.OpenContainerData.ContainerType.INVENTORY.getType()));
        }
    }

    /// 转换带标识符的信息为物品栏信息
    public static MutableComponent tranInfoToInventory(Component chat, Player player){
        String marker = Config.SERVER.INVENTORY_SHOW_MARKER.get();

        if (!marker.isEmpty()){
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

                        CompoundTag tag = new CompoundTag();
                        tag.putString("uuid", player.getStringUUID());
                        newChat.append(Component.translatable("chat.visual_share.inventory_show", player.getName()).withStyle(style -> style
                                .withColor(ChatFormatting.GOLD)
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open")))
                                .withClickEvent(new ClickEvent.Custom(INVENTORY_SHOW,
                                        Optional.of(tag)
                                ))
                        ));

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
            LOGGER.warn("Inventory show marker is empty");
            return (MutableComponent) chat;
        }
    }
}
