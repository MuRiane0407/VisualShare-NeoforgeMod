package com.muriane.visual_share.func.screenshot;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.muriane.visual_share.Config;
import com.muriane.visual_share.VisualShare;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joml.Vector2i;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.muriane.visual_share.func.screenshot.Screenshot.ScreenshotData;

public class ScreenshotPayload {
    public record ImageData(byte[] data, String imageId, String imageType) implements CustomPacketPayload {
        public static final Logger LOGGER = LogUtils.getLogger();
        public static final Type<ImageData> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "image"));

        public static final StreamCodec<ByteBuf, ImageData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE_ARRAY,
                ImageData::data,
                ByteBufCodecs.STRING_UTF8,
                ImageData::imageId,
                ByteBufCodecs.STRING_UTF8,
                ImageData::imageType,
                ImageData::new
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
                        ImageData.TYPE,
                        ImageData.STREAM_CODEC,
                        ServerPayloadHandler::handleDataOnMain
                );
            }

            @SubscribeEvent
            public static void register(RegisterClientPayloadHandlersEvent event){
                event.register(
                        ImageData.TYPE,
                        ClientPayloadHandler::handleDataOnMain
                );
            }

            public static class ServerPayloadHandler {
                public static void handleDataOnMain(final ImageData data, final IPayloadContext context) {
                    File modDataDir = new File(Minecraft.getInstance().gameDirectory, VisualShare.MOD_ID);
                    modDataDir.mkdir();
                    File fastScrDataDir = new File(modDataDir, "screenshot");
                    fastScrDataDir.mkdir();
                    File serverDir = new File(fastScrDataDir, "server");
                    serverDir.mkdir();
                    File imageData = new File(serverDir, data.imageId + "." + data.imageType);

                    try (FileOutputStream fos = new FileOutputStream(imageData)) {
                        fos.write(data.data);
                        LOGGER.info("{} upload {}", context.player().getDisplayName().getString(), data.imageId);
                    }catch (IOException e){
                        LOGGER.error("Error write to server file: {}", e.getMessage());
                    }

//                    PacketDistributor.sendToAllPlayers(data);
                }
            }

            public static class ClientPayloadHandler {
                public static void handleDataOnMain(final ImageData data, final IPayloadContext context) {
                    File modDataDir = new File(Minecraft.getInstance().gameDirectory, VisualShare.MOD_ID);
                    modDataDir.mkdir();
                    File fastScrDataDir = new File(modDataDir, "screenshot");
                    fastScrDataDir.mkdir();
                    File clientDir = new File(fastScrDataDir, "client");
                    clientDir.mkdir();
                    File imageData = new File(clientDir, data.imageId + "." + data.imageType);

                    try (FileOutputStream fos = new FileOutputStream(imageData)) {
                        fos.write(data.data);
                        LOGGER.info("Successful load image {}", data.imageId);
                    }catch (IOException e){
                        LOGGER.error("Error write to client file: {}", e.getMessage());
                    }

                    Minecraft.getInstance().execute(() -> {
                        try {
                            NativeImage image = null;
                            if (data.imageType.equals(Config.DataType.PNG.getType())) {
                                image = NativeImage.read(data.data);
                            }else if (data.imageType.equals(Config.DataType.AVIF.getType())){
                                BufferedImage buffer = ImageIO.read(new ByteArrayInputStream(data.data));
                                image = new NativeImage(buffer.getWidth(), buffer.getHeight(), true);
                                for (int y = 0 ; y < image.getHeight() ; y++){
                                    for (int x = 0 ; x < image.getWidth() ; x++){
                                        image.setPixel(x, y, buffer.getRGB(x, y));
                                    }
                                }
                            }

                            if (image != null) {
                                Identifier textureId = Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, data.imageId);
                                DynamicTexture texture = new DynamicTexture(textureId::toString, image);
                                Minecraft.getInstance().getTextureManager().register(textureId, texture);
                                ScreenshotData.put(data.imageId, new Pair<>(true, new Vector2i(image.getWidth(), image.getHeight())));
                            }
                        } catch (IOException e) {
                            LOGGER.error("Error turn data into texture: {}", e.getMessage());
                        }
                    });
                }
            }
        }
    }

    public record ImageUploadRequestData(String id, Integer cooldown) implements CustomPacketPayload {
        public static final Logger LOGGER = LogUtils.getLogger();
        public static final Type<ImageUploadRequestData> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "image_upload"));
        public static Map<String, Pair<byte[], String>> imageList = new HashMap<>();
        public static Map<UUID, Integer> imageShareCooldown = new HashMap<>();

        public static final StreamCodec<ByteBuf, ImageUploadRequestData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                ImageUploadRequestData::id,
                ByteBufCodecs.INT,
                ImageUploadRequestData::cooldown,
                ImageUploadRequestData::new
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
                        ImageUploadRequestData.TYPE,
                        ImageUploadRequestData.STREAM_CODEC,
                        ImageUploadRequestData.DataHolder.ServerPayloadHandler::handleDataOnMain
                );
            }

            @SubscribeEvent
            public static void register(RegisterClientPayloadHandlersEvent event){
                event.register(
                        ImageUploadRequestData.TYPE,
                        ImageUploadRequestData.DataHolder.ClientPayloadHandler::handleDataOnMain
                );
            }

            public static class ServerPayloadHandler {
                public static void handleDataOnMain(final ImageUploadRequestData data, final IPayloadContext context) {
                    UUID playerId = context.player().getUUID();
                    int cooldown = imageShareCooldown.getOrDefault(context.player().getUUID(), 0);
                    if (cooldown <= 0){
                        imageShareCooldown.put(playerId, Config.SERVER.SCREENSHOT_SHARE_COOLDOWN.getAsInt()*20);
                        PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new ImageUploadRequestData(data.id, cooldown));
                        LOGGER.info("{} request upload image success", context.player().getDisplayName().getString());
                    }else{
                        PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new ImageUploadRequestData(data.id, cooldown));
                        LOGGER.info("{} request upload image fail: Cooldown", context.player().getDisplayName().getString());
                    }
                }
            }

            public static class ClientPayloadHandler {
                public static void handleDataOnMain(final ImageUploadRequestData data, final IPayloadContext context) {
                    String prefix = Config.SERVER.SCREENSHOT_SHARE_IMAGE_PREFIX.get();
                    String subfix = Config.SERVER.SCREENSHOT_SHARE_IMAGE_SUBFIX.get();
                    ScreenshotScreen screen = Screenshot.imageViewScreen;

                    Pair<byte[], String> imageData = imageList.getOrDefault(data.id, null);
                    imageList.remove(data.id);

                    if (data.cooldown <= 0){
                        if (imageData != null){
                            ClientPacketDistributor.sendToServer(new ImageData(imageData.getFirst(), data.id, imageData.getSecond()));

                            ClipboardManager clipboard = new ClipboardManager();
                            clipboard.setClipboard(screen.getMinecraft().getWindow(), prefix + data.id + subfix);
                        }else{
                            screen.setTip(Component.translatable("tip.visual_share.screenshot.fail_share.no_exist"));
                        }

                        screen.setTip(Component.translatable("tip.visual_share.screenshot.share"));
                    }else{
                        screen.setTip(Component.translatable("tip.visual_share.screenshot.fail_share.cooldown", Component.literal(String.format("%.1f", data.cooldown/20f))));
                    }
                    screen.reload();
                }
            }

            @SubscribeEvent
            public static void onTick(ClientTickEvent.Pre event){
                for (UUID uuid : imageShareCooldown.keySet()){
                    int cooldown = imageShareCooldown.get(uuid)-1;
                    if (cooldown <= 0) {
                        imageShareCooldown.remove(uuid);
                    }else{
                        imageShareCooldown.put(uuid, cooldown);
                    }
                }
            }
        }
    }

    public record ImageLoadRequestData(String id) implements CustomPacketPayload{
        public static final Logger LOGGER = LogUtils.getLogger();
        public static final Type<ImageLoadRequestData> TYPE = new Type<>(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "image_load"));

        public static final StreamCodec<ByteBuf, ImageLoadRequestData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                ImageLoadRequestData::id,
                ImageLoadRequestData::new
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
                        ImageLoadRequestData.TYPE,
                        ImageLoadRequestData.STREAM_CODEC,
                        ServerPayloadHandler::handleDataOnMain
                );
            }

            @SubscribeEvent
            public static void register(RegisterClientPayloadHandlersEvent event){
                event.register(
                        ImageLoadRequestData.TYPE,
                        ClientPayloadHandler::handleDataOnMain
                );
            }

            public static class ServerPayloadHandler {
                public static void handleDataOnMain(final ImageLoadRequestData data, final IPayloadContext context) {
                    File imageData = Screenshot.ScreenshotHolder.findServerScreenshotData(data.id);
                    if (imageData != null){
                        byte[] bytes = null;
                        try (FileInputStream fis = new FileInputStream(imageData)) {
                            bytes = fis.readAllBytes();
                        }catch (IOException e){
                            LOGGER.error("Error read server file: {}", e.getMessage());
                        }

                        PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new ImageData(bytes, data.id, imageData.getName().substring(imageData.getName().lastIndexOf(".")+1)));
                        LOGGER.info("Successful find {}", data.id);
                    }else{
                        PacketDistributor.sendToPlayer((ServerPlayer) context.player(), new ImageLoadRequestData(data.id));
                        LOGGER.warn("{} don't exist", data.id);
                    }
                }
            }

            public static class ClientPayloadHandler {
                public static void handleDataOnMain(final ImageLoadRequestData data, final IPayloadContext context) {
                    ScreenshotData.put(data.id, new Pair<>(true, null));
                }
            }
        }
    }
}
