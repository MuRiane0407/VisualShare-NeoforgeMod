package com.muriane.visual_share.func.misc;

import com.mojang.logging.LogUtils;
import com.muriane.visual_share.VisualShare;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.UUID;

import static com.muriane.visual_share.func.misc.ItemDisplay.containerId;

public class MiscPayload {
    public record OpenContainerData(String uuid, int containerType) implements CustomPacketPayload {
        public static final Logger LOGGER = LogUtils.getLogger();
        public static final Type<OpenContainerData> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "open_container"));

        public static final StreamCodec<ByteBuf, OpenContainerData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                OpenContainerData::uuid,
                ByteBufCodecs.INT,
                OpenContainerData::containerType,
                OpenContainerData::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @EventBusSubscriber
        public static class DataHolder{
            @SubscribeEvent
            public static void register(RegisterPayloadHandlersEvent event){
                final PayloadRegistrar registrar = event.registrar("1");
                registrar.playBidirectional(
                        OpenContainerData.TYPE,
                        OpenContainerData.STREAM_CODEC,
                        OpenContainerData.DataHolder.ServerPayloadHandler::handleDataOnMain
                );
            }

            @SubscribeEvent
            public static void register(RegisterClientPayloadHandlersEvent event){
                event.register(
                        OpenContainerData.TYPE,
                        OpenContainerData.DataHolder.ClientPayloadHandler::handleDataOnMain
                );
            }

            public static class ServerPayloadHandler {
                public static void handleDataOnMain(final OpenContainerData data, final IPayloadContext context) {
                    Level level = context.player().level();
                    UUID uuid = UUID.fromString(data.uuid);
                    Player displayPlayer = level.getPlayerByUUID(uuid);
                    Player seePlayer = context.player();

                    if (displayPlayer != null) {
                        if (data.containerType == ContainerType.INVENTORY.getType()){
                            ChestMenu menu = ChestMenu.fiveRows(containerId, seePlayer.getInventory());

                            Inventory inv = displayPlayer.getInventory();
                            for (int i = 0; i < inv.getContainerSize(); i++) {
                                if (i < 9) menu.setItem(i + 36, 0, inv.getItem(i));
                                else if (i < 36) menu.setItem(i, 0, inv.getItem(i));
                                else if (i < 40) menu.setItem(39 - i, 0, inv.getItem(i));
                                else if (i == 40) menu.setItem(8, 0, inv.getItem(i));
                            }

                            context.player().openMenu(new SimpleMenuProvider(
                                    (c, i, p) -> menu,
                                    Component.translatable("container.visual_share.misc.inventory_display", displayPlayer.getName())
                            ));
                        }else if (data.containerType == ContainerType.ENDERCHEST.getType()){
                            Container container = displayPlayer.getEnderChestInventory();
                            ChestMenu menu = ChestMenu.threeRows(containerId, seePlayer.getInventory(), container);

                            context.player().openMenu(new SimpleMenuProvider(
                                    (c, i, p) -> menu,
                                    Component.translatable("container.visual_share.misc.ender_chest_display", displayPlayer.getName())
                            ));
                        }
                    }
                }
            }

            public static class ClientPayloadHandler {
                public static void handleDataOnMain(final OpenContainerData data, final IPayloadContext context) {

                }
            }
        }

        public enum ContainerType{
            INVENTORY(0),
            ENDERCHEST(1);

            private final int type;

            ContainerType(int type) {
                this.type = type;
            }

            public int getType() {
                return type;
            }
        }
    }
}
