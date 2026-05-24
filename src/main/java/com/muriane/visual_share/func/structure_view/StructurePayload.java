package com.muriane.visual_share.func.structure_view;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.muriane.visual_share.Config;
import com.muriane.visual_share.VisualShare;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.muriane.visual_share.func.structure_view.Structure.structureList;

public class StructurePayload {
    public record StructureUploadRequestData(String id, int cooldown) implements CustomPacketPayload {
        public static final Logger LOGGER = LogUtils.getLogger();
        public static final Type<StructureUploadRequestData> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "structure_upload_request"));
        public static Map<String, CompoundTag> structureList = new HashMap<>();
        public static final Map<UUID, Integer> uploadCooldown = new HashMap<>();

        public static final StreamCodec<ByteBuf, StructureUploadRequestData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                StructureUploadRequestData::id,
                ByteBufCodecs.INT,
                StructureUploadRequestData::cooldown,
                StructureUploadRequestData::new
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
                        StructureUploadRequestData.TYPE,
                        StructureUploadRequestData.STREAM_CODEC,
                        StructureUploadRequestData.DataHolder.ServerPayloadHandler::handleDataOnMain
                );
            }

            @SubscribeEvent
            public static void register(RegisterClientPayloadHandlersEvent event){
                event.register(
                        StructureUploadRequestData.TYPE,
                        StructureUploadRequestData.DataHolder.ClientPayloadHandler::handleDataOnMain
                );
            }

            public static class ServerPayloadHandler {
                public static void handleDataOnMain(final StructureUploadRequestData data, final IPayloadContext context) {
                    int cooldown = uploadCooldown.getOrDefault(context.player().getUUID(), 0);
                    LOGGER.info("Player {} request upload {}: Cooldown {}s", context.player().getDisplayName().getString(), data.id, cooldown);

                    if (cooldown == 0) uploadCooldown.put(context.player().getUUID(), Config.SERVER.SCREENSHOT_SHARE_COOLDOWN.getAsInt() * 20);
                    PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new StructureUploadRequestData(data.id, cooldown));
                }
            }

            public static class ClientPayloadHandler {
                public static void handleDataOnMain(final StructureUploadRequestData data, final IPayloadContext context) {
                    String prefix = Config.SERVER.STRUCTURE_SHARE_PREFIX.get();
                    String subfix = Config.SERVER.STRUCTURE_SHARE_SUBFIX.get();
                    Window window = Minecraft.getInstance().getWindow();

                    CompoundTag structureData = structureList.getOrDefault(data.id, null);
                    structureList.remove(data.id);

                    Player player = context.player();
                    if (data.cooldown <= 0) {
                        if (structureData != null){
                            ClientPacketDistributor.sendToServer(new StructureData(data.id, structureData));

                            ClipboardManager clipboard = new ClipboardManager();
                            clipboard.setClipboard(window, prefix + data.id + subfix);

                            player.sendOverlayMessage(Component.literal("Write into paste board"));
                            LOGGER.info("{} upload are allowed", data.id);
                        }else{
                            LOGGER.warn("The structureData don't exist: {}", data.id);
                        }
                    }else{
                        player.sendOverlayMessage(Component.literal("Cooldown"));
                        LOGGER.info("{} upload failed: Cooldown {}s", data.id, data.cooldown/20f);
                    }
                }
            }

            @SubscribeEvent
            public static void cooldown(ClientTickEvent.Pre event){
                for (UUID uuid : uploadCooldown.keySet()){
                    int cooldown = uploadCooldown.get(uuid)-1;
                    if (cooldown <= 0){
                        uploadCooldown.remove(uuid);
                    }else{
                        uploadCooldown.put(uuid, cooldown);
                    }
                }
            }
        }
    }

    public record StructureData(String id, CompoundTag tag) implements CustomPacketPayload {
        public static final Logger LOGGER = LogUtils.getLogger();
        public static final Type<StructureData> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "structure"));

        public static final StreamCodec<ByteBuf, StructureData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                StructureData::id,
                ByteBufCodecs.COMPOUND_TAG,
                StructureData::tag,
                StructureData::new
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
                        StructureData.TYPE,
                        StructureData.STREAM_CODEC,
                        StructureData.DataHolder.ServerPayloadHandler::handleDataOnMain
                );
            }

            @SubscribeEvent
            public static void register(RegisterClientPayloadHandlersEvent event){
                event.register(
                        StructureData.TYPE,
                        StructureData.DataHolder.ClientPayloadHandler::handleDataOnMain
                );
            }

            public static class ServerPayloadHandler {
                public static void handleDataOnMain(final StructureData data, final IPayloadContext context) {
                    File root = Minecraft.getInstance().gameDirectory;
                    File modDir = new File(root, VisualShare.MOD_ID);
                    modDir.mkdir();
                    File strDir = new File(modDir, "structure");
                    modDir.mkdir();
                    File serverDir = new File(strDir, "server");
                    modDir.mkdir();
                    File newFile = new File(serverDir, data.id + ".nbt");

                    StructureTemplate structure = new StructureTemplate();
                    structure.load(BuiltInRegistries.BLOCK, data.tag);
                    try {
                        StructureTemplateManager.save(newFile.toPath(), structure, false);
                        LOGGER.info("Server save structure success");
                    } catch (IOException e) {
                        LOGGER.error("Server save structure error: {}", e.getMessage());
                    }
                }
            }

            public static class ClientPayloadHandler {
                public static void handleDataOnMain(final StructureData data, final IPayloadContext context) {
                    File root = Minecraft.getInstance().gameDirectory;
                    File modDir = new File(root, VisualShare.MOD_ID);
                    modDir.mkdir();
                    File strDir = new File(modDir, "structure");
                    modDir.mkdir();
                    File clientDir = new File(strDir, "client");
                    modDir.mkdir();
                    File newFile = new File(clientDir, data.id + ".nbt");

                    StructureTemplate structure = new StructureTemplate();
                    structure.load(BuiltInRegistries.BLOCK, data.tag);
                    try {
                        StructureTemplateManager.save(newFile.toPath(), structure, false);
                        LOGGER.info("Client save structure success");
                    } catch (IOException e) {
                        LOGGER.error("Client save structure error: {}", e.getMessage());
                    }

                    structureList.put(data.id, new Pair<>(true, structure));
                }
            }
        }
    }

    public record StructureLoadRequestData(String id) implements CustomPacketPayload{
        public static final Logger LOGGER = LogUtils.getLogger();
        public static final Type<StructureLoadRequestData> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "structure_load_request"));

        public static final StreamCodec<ByteBuf, StructureLoadRequestData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                StructureLoadRequestData::id,
                StructureLoadRequestData::new
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
                        StructureLoadRequestData.TYPE,
                        StructureLoadRequestData.STREAM_CODEC,
                        StructureLoadRequestData.DataHolder.ServerPayloadHandler::handleDataOnMain
                );
            }

            @SubscribeEvent
            public static void register(RegisterClientPayloadHandlersEvent event){
                event.register(
                        StructureLoadRequestData.TYPE,
                        StructureLoadRequestData.DataHolder.ClientPayloadHandler::handleDataOnMain
                );
            }

            public static class ServerPayloadHandler {
                public static void handleDataOnMain(final StructureLoadRequestData data, final IPayloadContext context) {
                    File structureData = Structure.findServerStructureData(data.id);
                    if (structureData != null){
                        try {
                            CompoundTag tag = NbtIo.readCompressed(structureData.toPath(), NbtAccounter.defaultQuota());

                            PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new StructureData(data.id, tag));
                            LOGGER.info("Successful find {}", data.id);
                        }catch (IOException e){
                            PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new StructureLoadRequestData(data.id));
                            LOGGER.error("Error read server file: {}", e.getMessage());
                        }
                    }else{
                        PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new StructureLoadRequestData(data.id));
                        LOGGER.warn("{} don't exist", data.id);
                    }
                }
            }

            public static class ClientPayloadHandler {
                public static void handleDataOnMain(final StructureLoadRequestData data, final IPayloadContext context) {
                    structureList.put(data.id, new Pair<>(true, null));
                    context.player().sendOverlayMessage(Component.literal("No that structure"));
                }
            }
        }
    }
}
