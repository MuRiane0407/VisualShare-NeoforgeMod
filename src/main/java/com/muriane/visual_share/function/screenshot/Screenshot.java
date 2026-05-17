package com.muriane.visual_share.function.screenshot;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.muriane.visual_share.Config;
import com.muriane.visual_share.VisualShare;
import com.muriane.visual_share.item.ModItems;
import com.muriane.visual_share.key.ModKeys;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joml.Vector2i;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Screenshot {
    public static Logger LOGGER = LogUtils.getLogger();
    public static ScreenshotScreen screenshotScreen;
    public static Map<String, Pair<Boolean, Vector2i>> ScreenshotData = new HashMap<>(); // 图片ID, 是否存在, 尺寸
    public static double maxThumbnailSizePercent = Config.CLIENT.SCREENSHOT_SHARE_THUMBNAIL_IMAGE_SIZE.getAsDouble();
    public static double maxFullSizePercent = Config.CLIENT.SCREENSHOT_SHARE_FULL_IMAGE_SIZE.getAsDouble();

    @EventBusSubscriber
    public static class ScreenshotHolder{
        public static String curTextureId = null;

        /// 按键检测
        @SubscribeEvent
        public static void onKey(InputEvent.Key event){
            Minecraft mc = Minecraft.getInstance();
            if (event.getKey() == ModKeys.screenshot.getKey().getValue() && event.getAction() == InputConstants.PRESS){
                if (mc.level != null && !(mc.screen instanceof ScreenshotScreen)){ // 用level来判断玩家是否已经在某个服务器中
                    com.muriane.visual_share.method.MScreenshot.takeScreenshot(
                            mc.getMainRenderTarget(),
                            1,
                            image -> {
                                screenshotScreen = new ScreenshotScreen(mc.screen, image);
                                mc.setScreen(screenshotScreen);
                            }
                    );
                }
            }else if (screenshotScreen != null && mc.screen == screenshotScreen) { // 只有在截图界面中才能操作
                if (event.getAction() == InputConstants.PRESS) {
                    if (event.getKey() == ModKeys.SELECTION.getKey().getValue()){
                        screenshotScreen.setImageInterActionMode(ScreenshotImageWidget.InteractionMode.SELECTION);
                    }else if (event.getKey() == ModKeys.BRUSH.getKey().getValue()){
                        screenshotScreen.setImageInterActionMode(ScreenshotImageWidget.InteractionMode.BRUSH);
                    }else if (event.getKey() == ModKeys.UNDO_REDO.getKey().getValue()){
                        if (mc.hasControlDown()){
                            if (!mc.hasShiftDown()){
                                screenshotScreen.imageUndo();
                            }else{
                                screenshotScreen.imageRedo();
                            }
                        }
                    }else if (event.getKey() == ModKeys.SAVE.getKey().getValue()){
                        if (mc.hasControlDown()) {
                            screenshotScreen.imageSave();
                        }
                    }else if (event.getKey() == ModKeys.SHARE.getKey().getValue()){
                        if (mc.hasControlDown()) {
                            screenshotScreen.imageShare();
                        }
                    }

                    if (screenshotScreen.getImageInterActionMode() == ScreenshotImageWidget.InteractionMode.SELECTION) {
                        if (event.getKey() == ModKeys.CUT.getKey().getValue()) {
                            screenshotScreen.imageCut();
                        }
                    }

                    if (screenshotScreen.getImageInterActionMode() == ScreenshotImageWidget.InteractionMode.BRUSH) {
                        if (event.getKey() == ModKeys.COLOR_PALETTE.getKey().getValue()) {
                            screenshotScreen.imageColorPalette();
                        } else if (event.getKey() == ModKeys.BRUSH_SIZE.getKey().getValue()) {
                            screenshotScreen.imageBrushSize();
                        }
                    }
                }
            }
        }

        /// 转换带标识符的信息为图片信息
        @SubscribeEvent
        public static void onServerReceivedChat(ServerChatEvent event){
            String prefix = Config.SERVER.SCREENSHOT_SHARE_IMAGE_PREFIX.get();
            String subfix = Config.SERVER.SCREENSHOT_SHARE_IMAGE_SUBFIX.get();

            Component chat = event.getMessage();
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
                        if (findServerScreenshotData(find) != null){
                            Player player = Minecraft.getInstance().player;
                            if (player != null){
                                CompoundTag tag = new CompoundTag();
                                tag.putString("texture_id", find);
                                ItemStackTemplate virtualImage = new ItemStackTemplate(
                                        ModItems.VIRTUAL_IMAGE_ITEM, DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build()
                                );
                                newChat.append(Component.translatable("chat.visual_share.screenshot.image").withStyle(style -> style
                                        .withColor(ChatFormatting.BLUE)
                                        .withHoverEvent(new HoverEvent.ShowItem(virtualImage)
                                        )));
                            }
                        }else{
                            newChat.append(Component.literal(prefix + find + subfix).withStyle(cur.getStyle()));
                        }

                        String nextCurStr = curStr.substring(ie + subfix.length());
                        cur = Component.literal(nextCurStr).withStyle(cur.getStyle());
                    } else {
                        newChat.append(cur);
                        end = true;
                    }
                    time--;
                }
            }
            event.setMessage(newChat);
        }

        public static File findServerScreenshotData(String name){
            // 在本地文件中查找
            File clientScreenshotData = new File(Minecraft.getInstance().gameDirectory+"/"+VisualShare.MOD_ID+"/screenshot/server");
            if (clientScreenshotData.exists()){
                File[] files = clientScreenshotData.listFiles();
                if (files != null) {
                    for (File data : files){
                        if (data.getName().substring(0, data.getName().lastIndexOf(".")).equals(name)){
                            return data;
                        }
                    }
                }
            }

            return null;
        }

        /// 通过虚拟的物品Tooltip渲染图片
        @SubscribeEvent
        public static void onMakeItemTooltip(ItemTooltipEvent event){
            ItemStack stack = event.getItemStack();
            if (stack.is(ModItems.VIRTUAL_IMAGE_ITEM)){
                curTextureId = stack.getItemName().getString();
                List<Component> tooltips = event.getToolTip();
                tooltips.clear();

                // 特殊情况就会看到这个，比如textureId已经过时无法渲染对应的texture
                Pair<Boolean, Vector2i> info = ScreenshotData.getOrDefault(curTextureId, null);
                if (info != null && info.getFirst()) {
                    if (info.getSecond() != null){
                        tooltips.add(Component.translatable("tip.visual_share.screenshot.image.image"));
                    }else{
                        tooltips.add(Component.translatable("tip.visual_share.screenshot.image.no_image"));
                    }
                }else{
                    tooltips.add(Component.translatable("tip.visual_share.screenshot.image.image_loading"));
                }
            }else{
                curTextureId = null;
            }
        }

        @SubscribeEvent
        public static void onRenderTooltip(RenderTooltipEvent.Pre event){
            if (curTextureId != null){
                if (!event.getItemStack().is(ModItems.VIRTUAL_IMAGE_ITEM)){
                    curTextureId = null;
                }else{
                    Pair<Boolean, Vector2i> info = ScreenshotData.getOrDefault(curTextureId, null);
                    if (info == null) {
                        tryGetImage();
                    }else{
                        if (info.getFirst() == true && info.getSecond() != null){
                            renderImage(event, info.getSecond());
                        }
                    }
                }
            }
        }

        public static void tryGetImage(){
            ScreenshotData.put(curTextureId, new Pair<>(false, null));

            File imageData = findClientScreenshotData(curTextureId);
            if (imageData != null){
                String imageType = imageData.getName().substring(imageData.getName().lastIndexOf(".")+1);
                try (FileInputStream fis = new FileInputStream(imageData)){
                    NativeImage image = null;
                    if (imageType.equals(Config.DataType.PNG.getType())) {
                        image = NativeImage.read(fis);
                    }else if (imageType.equals(Config.DataType.AVIF.getType())){
                        BufferedImage buffer = ImageIO.read(new ByteArrayInputStream(fis.readAllBytes()));
                        image = new NativeImage(buffer.getWidth(), buffer.getHeight(), true);
                        for (int y = 0 ; y < image.getHeight() ; y++){
                            for (int x = 0 ; x < image.getWidth() ; x++){
                                image.setPixel(x, y, buffer.getRGB(x, y));
                            }
                        }
                    }

                    if (image != null){
                        Identifier textureId = Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, curTextureId);
                        DynamicTexture texture = new DynamicTexture(textureId::toString, image);
                        Minecraft.getInstance().getTextureManager().register(textureId, texture);
                        Screenshot.ScreenshotData.put(curTextureId, new Pair<>(true, new Vector2i(image.getWidth(), image.getHeight())));
                    }
                }catch (IOException e){
                    LOGGER.error("Error read client file: {}", e.getMessage());
                }
            }else{
                ClientPacketDistributor.sendToServer(new ImageLoadRequestData(curTextureId));
            }
        }

        public static File findClientScreenshotData(String name){
            // 在本地文件中查找
            File clientScreenshotData = new File(Minecraft.getInstance().gameDirectory+"/"+VisualShare.MOD_ID+"/screenshot/client");
            if (clientScreenshotData.exists()){
                File[] files = clientScreenshotData.listFiles();
                if (files != null) {
                    for (File data : files){
                        if (data.getName().substring(0, data.getName().lastIndexOf(".")).equals(name)){
                            return data;
                        }
                    }
                }
            }

            return null;
        }

        public static void renderImage(RenderTooltipEvent.Pre event, Vector2i size){
            Minecraft mc = Minecraft.getInstance();
            Window window = Minecraft.getInstance().getWindow();
            int guiScale = window.getGuiScale();
            int outlineSize = 2;
            int outlineColor = new Color(255, 255, 255).getRGB();
            int x, y, width, height;
            if (mc.hasShiftDown()) {
                Vector2i fullSize = getFullSize(size);
                width = fullSize.x;
                height = fullSize.y;
            } else {
                Vector2i thumbnailSize = getThumbnailSize(size);
                width = thumbnailSize.x;
                height = thumbnailSize.y;
            }
            x = Math.max(outlineSize, Math.min(window.getWidth() / guiScale - width - outlineSize, event.getX() + 10));
            y = Math.max(outlineSize, Math.min(window.getHeight() / guiScale - height - outlineSize, event.getY() - height / 2));

            GuiGraphicsExtractor graphics = event.getGraphics();
            graphics.fill(x-outlineSize, y-outlineSize, x+width+outlineSize, y, outlineColor);
            graphics.fill(x-outlineSize, y, x, y+height, outlineColor);
            graphics.fill(x+width, y, x+width+outlineSize, y+height, outlineColor);
            graphics.fill(x-outlineSize, y+height, x+width+outlineSize, y+height+outlineSize, outlineColor);
            graphics.blit(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, curTextureId), x, y, 0, 0, width, height, width, height);

            event.setCanceled(true);
        }

        public static Vector2i getFullSize(Vector2i origin){
            Window window = Minecraft.getInstance().getWindow();
            int width = origin.x;
            int height = origin.y;
            int toWidth = (int) (maxFullSizePercent * window.getWidth()/window.getGuiScale());
            int toHeight = (int) (maxFullSizePercent * window.getHeight()/window.getGuiScale());
            float widthScale = (float) toWidth /width;
            float heightScale = (float) toHeight /height;
            float useScale = Math.min(widthScale, heightScale);
            width = (int) (width*useScale);
            height = (int) (height*useScale);
            return new Vector2i(width, height);
        }

        public static Vector2i getThumbnailSize(Vector2i origin){
            Window window = Minecraft.getInstance().getWindow();
            int width = origin.x;
            int height = origin.y;
            int toWidth = (int) (maxThumbnailSizePercent * window.getWidth()/window.getGuiScale());
            int toHeight = (int) (maxThumbnailSizePercent * window.getHeight()/window.getGuiScale());
            float widthScale = (float) toWidth/width;
            float heightScale = (float) toHeight/height;
            float useScale = Math.min(widthScale, heightScale);
            width = (int) (width*useScale);
            height = (int) (height*useScale);
            return new Vector2i(width, height);
        }

//        /// 清理缓存数据
//        @SubscribeEvent
//        public static void onServerClose(ServerStoppingEvent event){
//            File serverDir = new File(event.getServer().getServerDirectory().toUri());
//            clearScreenshotData(serverDir);
//        }
//
//        @SubscribeEvent
//        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event){ // 存在一个使用同一个客户端时，一个客户端退出会清空另一个客户端的缓存的问题，暂时不修复
//            clearScreenshotData(Minecraft.getInstance().gameDirectory);
//            for (String data : ScreenshotData.keySet()){
//                Minecraft.getInstance().getTextureManager().release(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, data));
//            }
//            ScreenshotData.clear();
//        }
//
//        @SubscribeEvent
//        public static void onGameShuttingDown(GameShuttingDownEvent event){
//            clearScreenshotData(Minecraft.getInstance().gameDirectory);
//        }
//
//        public static void clearScreenshotData(File rootDir){
//            File modDataDir = new File(rootDir, VisualShare.MOD_ID);
//            if (!modDataDir.exists()) return;
//            File fastScrDataDir = new File(modDataDir, "screenshot");
//            if (!fastScrDataDir.exists()) return;
//            File[] files = fastScrDataDir.listFiles();
//            if (files != null) {
//                for (File dat : files) {
//                    dat.delete();
//                }
//            }
//        }
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
                    File imageData = ScreenshotHolder.findServerScreenshotData(data.id);
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

    public record ImageData(byte[] data, String imageId, String imageType) implements CustomPacketPayload {
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
                                Screenshot.ScreenshotData.put(data.imageId, new Pair<>(true, new Vector2i(image.getWidth(), image.getHeight())));
                            }
                        } catch (IOException e) {
                            LOGGER.error("Error turn data into texture: {}", e.getMessage());
                        }
                    });
                }
            }
        }
    }
}
