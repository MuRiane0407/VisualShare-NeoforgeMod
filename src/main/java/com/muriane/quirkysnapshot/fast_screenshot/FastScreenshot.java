package com.muriane.quirkysnapshot.fast_screenshot;

import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.muriane.quirkysnapshot.QuirkySnapshot;
import com.muriane.quirkysnapshot.item.ModItems;
import com.muriane.quirkysnapshot.key.ModKeys;
import com.muriane.quirkysnapshot.method.MScreenshot;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
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
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
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
import java.util.ArrayList;
import java.util.List;

public class FastScreenshot {
    public static Logger LOGGER = LogUtils.getLogger();
    public static FastScreenshotScreen fastScreenshotScreen;
    public static List<Pair<Identifier, Vector2i>> fastScreenshotData = new ArrayList<>();
    public static float maxThumbnailSizePercent = 0.25f;
    public static float maxFullSizePercent = 0.75f;

    @EventBusSubscriber
    public static class FastScreenshotHolder{
        public static Identifier curTextureId = null;

        /// 按键检测
        @SubscribeEvent
        public static void onKey(InputEvent.Key event){
            Minecraft mc = Minecraft.getInstance();
            if (event.getKey() == ModKeys.FAST_SCREENSHOT.getKey().getValue() && event.getAction() == InputConstants.PRESS){
                if (mc.level != null && !(mc.screen instanceof FastScreenshotScreen)){ // 用level来判断玩家是否已经在某个服务器中
                    MScreenshot.takeScreenshot(
                            mc.getMainRenderTarget(),
                            1,
                            image -> {
                                fastScreenshotScreen = new FastScreenshotScreen(mc.screen, image);
                                mc.setScreen(fastScreenshotScreen);
                            }
                    );
                }
            }else if (fastScreenshotScreen != null && mc.screen == fastScreenshotScreen) { // 只有在截图界面中才能操作
                if (event.getAction() == InputConstants.PRESS) {
                    if (event.getKey() == ModKeys.SELECTION.getKey().getValue()){
                        fastScreenshotScreen.imageModeSelection();
                    }else if (event.getKey() == ModKeys.CUT.getKey().getValue()) {
                        fastScreenshotScreen.imageCut();
                    }else if (event.getKey() == ModKeys.BRUSH.getKey().getValue()){
                        fastScreenshotScreen.imageModeBrush();
                    }else if (event.getKey() == ModKeys.COLOR_PALETTE.getKey().getValue()){
                        fastScreenshotScreen.imageColorPalette();
                    }else if (event.getKey() == ModKeys.UNDO_REDO.getKey().getValue()){
                        if (mc.hasControlDown()){
                            if (!mc.hasShiftDown()){
                                fastScreenshotScreen.imageUndo();
                            }else{
                                fastScreenshotScreen.imageRedo();
                            }
                        }
                    }else if (event.getKey() == ModKeys.SAVE.getKey().getValue()){
                        if (mc.hasControlDown()) {
                            fastScreenshotScreen.imageSave();
                        }
                    }else if (event.getKey() == ModKeys.SHARE.getKey().getValue()){
                        if (mc.hasControlDown()) {
                            fastScreenshotScreen.imageShare();
                        }
                    }
                }
            }
        }

        /// 转换带标识符的信息为图片信息
        public static String startMark = "<qs_fs>";
        public static String endMark = "</qs_fs>";
        @SubscribeEvent
        public static void onChatReceived(ClientChatReceivedEvent event){
            Component chat = event.getMessage();
            MutableComponent newChat = MutableComponent.create(Component.empty().getContents());
            for (Component component : chat.toFlatList()){
                Component cur = component;
                String curStr = cur.getString();
                boolean end = false;
                int time = curStr.length(); // 防止死循环
                while (!cur.equals(Component.empty()) && !end && time > 0){
                    int is = curStr.indexOf(startMark);
                    int ie = curStr.indexOf(endMark);
                    if (is != -1 && ie != -1) {
                        newChat.append(Component.literal(curStr.substring(0, is)).withStyle(cur.getStyle()));

                        String find = curStr.substring(is + startMark.length(), ie);
                        boolean isFind = false;
                        for (Pair<Identifier, Vector2i> data : fastScreenshotData) {
                            if (find.equals(data.getFirst().toString())) {
                                Player player = Minecraft.getInstance().player;
                                if (player != null){
                                    CompoundTag tag = new CompoundTag();
                                    tag.putString("texture_id", find);
                                    ItemStackTemplate virtualImage = new ItemStackTemplate(
                                            ModItems.VIRTUAL_IMAGE_ITEM, DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(tag)).build()
                                    );
                                    newChat.append(Component.translatable("chat.quirkysnapshot.fast_screenshot.image").withStyle(style -> style
                                            .withColor(ChatFormatting.BLUE)
                                            .withHoverEvent(new HoverEvent.ShowItem(virtualImage)
                                            )));
                                }
                                isFind = true;
                                break;
                            }
                        }
                        if (!isFind){
                            newChat.append(Component.literal(startMark + find + endMark).withStyle(cur.getStyle()));
                        }

                        String nextCurStr = curStr.substring(ie + endMark.length());
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

        /// 通过虚拟的物品Tooltip渲染图片
        @SubscribeEvent
        public static void onMakeItemTooltip(ItemTooltipEvent event){
            ItemStack stack = event.getItemStack();
            if (stack.is(ModItems.VIRTUAL_IMAGE_ITEM)){
                curTextureId = Identifier.parse(stack.getItemName().getString());
                List<Component> tooltips = event.getToolTip();
                tooltips.clear();
                // 特殊情况就会看到这个，比如textureId已经过时无法渲染对应的texture
                tooltips.add(Component.literal("You shouldn't see this, the texture has over time"));
            }
        }

        @SubscribeEvent
        public static void onRenderTooltip(RenderTooltipEvent.Pre event){
            if (curTextureId != null){
                if (!event.getItemStack().is(ModItems.VIRTUAL_IMAGE_ITEM)){
                    curTextureId = null;
                }else{
                    for (Pair<Identifier, Vector2i> data : fastScreenshotData) {
                        if (curTextureId.equals(data.getFirst())) {
                            Minecraft mc = Minecraft.getInstance();
                            Window window = Minecraft.getInstance().getWindow();
                            int guiScale = window.getGuiScale();
                            int outlineSize = 2;
                            int outlineColor = new Color(255, 255, 255).getRGB();
                            int x, y, width, height;
                            if (mc.hasShiftDown()) {
                                Vector2i fullSize = getFullSize(data.getSecond());
                                width = fullSize.x;
                                height = fullSize.y;
                            } else {
                                Vector2i thumbnailSize = getThumbnailSize(data.getSecond());
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
                            graphics.blit(RenderPipelines.GUI_TEXTURED, curTextureId, x, y, 0, 0, width, height, width, height);

                            event.setCanceled(true);
                            break;
                        }
                    }
                }
            }
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

        /// 清理缓存数据
        @SubscribeEvent
        public static void onServerClose(ServerStoppingEvent event){
            File serverDir = new File(event.getServer().getServerDirectory().toUri());
            clearFastScreenshotData(serverDir);
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event){ // 存在一个使用同一个客户端时，一个客户端退出会清空另一个客户端的缓存的问题，暂时不修复
            clearFastScreenshotData(Minecraft.getInstance().gameDirectory);
            for (Pair<Identifier, Vector2i> data : fastScreenshotData){
                Minecraft.getInstance().getTextureManager().release(data.getFirst());
            }
            fastScreenshotData.clear();
        }

        @SubscribeEvent
        public static void onGameShuttingDown(GameShuttingDownEvent event){
            clearFastScreenshotData(Minecraft.getInstance().gameDirectory);
        }

        public static void clearFastScreenshotData(File rootDir){
            File dataDir = new File(rootDir, "data");
            if (!dataDir.exists()) return;
            File modDataDir = new File(dataDir, QuirkySnapshot.MOD_ID);
            if (!modDataDir.exists()) return;
            File fastScrDataDir = new File(modDataDir, "fast-screenshot");
            if (!fastScrDataDir.exists()) return;
            File[] files = fastScrDataDir.listFiles();
            if (files != null) {
                for (File dat : files) {
                    dat.delete();
                }
            }
        }
    }

    public static class FastScreenshotScreen extends Screen {
        private final Screen lastScreen;
        private final List<NativeImage> imageHistory = new ArrayList<>();
        private int step;
        private Component tip;
        private CustomImageWidget.FastScreenshotImageWidget.InteractionMode imageInteractionMode = CustomImageWidget.FastScreenshotImageWidget.InteractionMode.SELECTION;
        private CustomImageWidget.FastScreenshotImageWidget imageWidget;
        private float[] imageHSV;
        private int imageBrushSize;

        protected FastScreenshotScreen(Screen lastScreen, NativeImage image) {
            super(Component.empty());
            this.lastScreen = lastScreen;
            this.imageHistory.add(image);
            this.step = imageHistory.size()-1;
            this.tip = Component.empty();
            this.imageHSV = new float[]{0, 0, 0};
            this.imageBrushSize = 2;
        }

        @Override
        protected void init() {
            Window window = Minecraft.getInstance().getWindow();
            int guiScale = window.getGuiScale();

            initImage(guiScale, window);
            initButtons(guiScale, window);
            initLabel(guiScale, window);
        }

        private void initLabel(int guiScale, Window window) {
            CustomLabelWidget labelWidget = new CustomLabelWidget(
                    0.125f, 0, 0.875f, 0, 0.75f, 0, 0, 32*guiScale,
                    guiScale, window,
                    this.tip
            );
            this.addRenderableWidget(labelWidget);
        }

        private void initButtons(int guiScale, Window window) {
            WidgetSprites notFindSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(QuirkySnapshot.MOD_ID, "fast_screenshot/not_find"));
            List<Pair<Pair<WidgetSprites, List<Component>>, Button.OnPress>> leftList = new ArrayList<>(List.of(
                    new Pair<>(
                            new Pair<>(notFindSprites,
                                    List.of(
                                            Component.empty()
                                    )
                            ),
                            button -> this.imageModeSelection()
                    ),
                    new Pair<>(
                            new Pair<>(notFindSprites,
                                    List.of(
                                            Component.empty()
                                    )
                            ),
                            button -> this.imageModeBrush()
                    )
            ));
            if (this.imageInteractionMode == CustomImageWidget.FastScreenshotImageWidget.InteractionMode.SELECTION){
                leftList.add(
                        new Pair<>(
                                new Pair<>(notFindSprites,
                                        List.of(
                                                Component.empty()
                                        )
                                ),
                                button -> this.imageCut()
                        )
                );
            }else if (this.imageInteractionMode == CustomImageWidget.FastScreenshotImageWidget.InteractionMode.BRUSH){
                leftList.add(
                        new Pair<>(
                                new Pair<>(notFindSprites,
                                        List.of(
                                                Component.empty()
                                        )
                                ),
                                button -> this.imageColorPalette()
                        )
                );
            }
            leftList.addAll(List.of(
                    new Pair<>(
                            new Pair<>(notFindSprites,
                                    List.of(
                                            Component.empty()
                                    )
                            ),
                            button -> this.imageUndo()
                    ),
                    new Pair<>(
                            new Pair<>(notFindSprites,
                                    List.of(
                                            Component.empty()
                                    )
                            ),
                            button -> this.imageRedo()
                    )
            ));
            CustomImageButton.ImageButtonList leftImageButtonList = new CustomImageButton.ImageButtonList(
                    0.125f, -2-32*guiScale, 0.125f, 0, 0, 32*guiScale, 0.75f, 0,
                    guiScale, window,
                    leftList
            );
            CustomImageButton.ImageButtonList rightImageButtonList = new CustomImageButton.ImageButtonList(
                    0.875f, 2, 0.125f, 0, 0, 32*guiScale, 0.75f, 0,
                    guiScale, window,
                    List.of(
                            new Pair<>(
                                    new Pair<>(notFindSprites,
                                            List.of(
                                                    Component.empty()
                                            )
                                    ),
                                    button -> this.imageSave()
                            ),
                            new Pair<>(
                                    new Pair<>(notFindSprites,
                                            List.of(
                                                    Component.empty()
                                            )
                                    ),
                                    button -> this.imageShare()
                            )
                    )
            );
            for (Button button : leftImageButtonList.getButtonsInColumn()){
                this.addRenderableWidget(button);
            }
            for (Button button : rightImageButtonList.getButtonsInColumn()){
                this.addRenderableWidget(button);
            }
        }

        private void initImage(int guiScale, Window window) {
            NativeImage origin = this.imageHistory.get(this.step); // 制造一个复制品防止原图被close掉，不然会崩溃
            NativeImage image = new NativeImage(origin.format(), origin.getWidth(), origin.getHeight(), true);
            image.copyFrom(origin);
            float scale = 0.75f;
            float widthScale = (float) window.getWidth()/image.getWidth();
            float heightScale = (float) window.getHeight()/image.getHeight();
            float useScale = Math.min(widthScale, heightScale);
            float xOffset = widthScale > heightScale ? (1-heightScale/widthScale)/2 : 0;
            float yOffset = widthScale < heightScale ? (1-widthScale/heightScale)/2 : 0;
            if (!image.isClosed()){
                this.imageWidget = new CustomImageWidget.FastScreenshotImageWidget(
                        image,
                        0.125f+xOffset*scale, 0.125f+yOffset*scale, scale*useScale, scale*useScale,
                        guiScale, window, this, this.imageInteractionMode,
                        this.imageHSV, this.imageBrushSize);
                this.addRenderableWidget(this.imageWidget);
            }else{
                LOGGER.error("The image has been closed, please tell the author to fix the problem");
            }
        }

        public void reload(){
            this.rebuildWidgets();
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(lastScreen);
        }

        public void imageCut(){
            NativeImage newImage = this.imageWidget.cutImageWithSelection();
            if (newImage != null){
                this.addNewImage(newImage);
            }else{
                this.tip = Component.translatable("tip.quirkysnapshot.fast_screenshot.no_selection_or_too_small").withStyle(ChatFormatting.RED);
            }
            this.reload();
        }

        public void imageColorPalette(){
            this.addRenderableWidget()
        }

        public void imageUndo(){
            if (this.step > 0){
                this.step -= 1;
            }else{
                this.tip = Component.translatable("tip.quirkysnapshot.fast_screenshot.cant_undo").withStyle(ChatFormatting.RED);
            }
            this.reload();
        }

        public void imageRedo(){
            if (this.step < this.imageHistory.size()-1){
                this.step += 1;
            }else{
                this.tip = Component.translatable("tip.quirkysnapshot.fast_screenshot.cant_redo").withStyle(ChatFormatting.RED);
            }
            this.reload();
        }

        public void imageSave(){
            Minecraft mc = Minecraft.getInstance();
            String imageId = MScreenshot.getImageId(this.imageHistory.get(this.step));
            File scrDir = new File(mc.gameDirectory, "screenshots");
            scrDir.mkdir();
            File modScrDir = new File(scrDir, QuirkySnapshot.MOD_ID);
            modScrDir.mkdir();
            File fastScrDir = new File(modScrDir, "fast-screenshot");
            fastScrDir.mkdir();
            File file = new File(fastScrDir, imageId + ".png");

            Util.ioPool().execute(
                    () -> {
                        try {
                            this.imageHistory.get(this.step).writeToFile(file);
                            this.tip = Component.translatable("tip.quirkysnapshot.fast_screenshot.success_save", imageId);
                        } catch (Exception e) {
                            LOGGER.error("Couldn't save image {}", e.getMessage());
                            this.tip = Component.translatable("tip.quirkysnapshot.fast_screenshot.cant_save", e).withStyle(ChatFormatting.RED);
                        }
                        this.reload();
                    });
        }

        public void imageShare(){ // 用jpg压缩 压缩比大概为5%
            NativeImage image = this.imageHistory.get(this.step);

            BufferedImage buffered = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int abgr = image.getPixel(x, y);
                    int rgb = ((abgr >> 24) & 0xFF) << 24 | ((abgr >> 16) & 0xFF) << 16 |
                            ((abgr >> 8) & 0xFF) << 8 | (abgr & 0xFF);
                    buffered.setRGB(x, y, rgb);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                ImageIO.write(buffered, "jpg", outputStream);
            }catch (IOException e){
                LOGGER.error("Error write to jpg: {}", e.getMessage());
            }

            Identifier textureId = Identifier.fromNamespaceAndPath(QuirkySnapshot.MOD_ID, MScreenshot.getImageId(image));
            ClipboardManager clipboard = new ClipboardManager();
            clipboard.setClipboard(this.minecraft.getWindow(), FastScreenshotHolder.startMark + textureId + FastScreenshotHolder.endMark);
            this.tip = Component.translatable("tip.quirkysnapshot.fast_screenshot.share", textureId.toString());

            ClientPacketDistributor.sendToServer(new ImageData(outputStream.toByteArray(), textureId.toString()));
            this.reload();
        }

        public void imageModeSelection() {
            this.imageInteractionMode = CustomImageWidget.FastScreenshotImageWidget.InteractionMode.SELECTION;
            this.reload();
        }

        public void imageModeBrush() {
            this.imageInteractionMode = CustomImageWidget.FastScreenshotImageWidget.InteractionMode.BRUSH;
            this.reload();
        }

        public void addNewImage(NativeImage newImage){
            if (this.imageHistory.size() == this.step+1) {
                this.imageHistory.add(newImage);
            }else{
                List<NativeImage> newList = new ArrayList<>();
                for (int index = 0 ; index <= this.step ; index++){
                    newList.add(this.imageHistory.get(index));
                }
                newList.add(newImage);
                this.imageHistory.clear();
                this.imageHistory.addAll(newList);
            }
            this.step = this.imageHistory.size()-1;
            this.reload();
        }
    }

    public record ImageData(byte[] data, String textureId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ImageData> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(QuirkySnapshot.MOD_ID, "image"));

        public static final StreamCodec<ByteBuf, ImageData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE_ARRAY,
                ImageData::data,
                ByteBufCodecs.STRING_UTF8,
                ImageData::textureId,
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
//                    Minecraft mc = Minecraft.getInstance();
//                    String datName = MScreenshot.getTimeId(data);
//                    File dataDir = new File(mc.gameDirectory, "data");
//                    dataDir.mkdir();
//                    File modDataDir = new File(dataDir, QuirkySnapshot.MOD_ID);
//                    modDataDir.mkdir();
//                    File fastScrDataDir = new File(modDataDir, "fast-screenshot");
//                    fastScrDataDir.mkdir();
//                    File imageDat = new File(fastScrDataDir, datName + ".dat");
//
//                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(imageDat))){
//                        writer.write(new String(data.data));
//                    }catch (IOException e){
//                        LOGGER.error("Error write to file: {}", e.getMessage());
//                    }

                    PacketDistributor.sendToAllPlayers(data);
                }
            }

            public static class ClientPayloadHandler {
                public static void handleDataOnMain(final ImageData data, final IPayloadContext context) {
                    Minecraft mc = Minecraft.getInstance();
                    String datName = MScreenshot.getTimeId(data);
                    File dataDir = new File(mc.gameDirectory, "data");
                    dataDir.mkdir();
                    File modDataDir = new File(dataDir, QuirkySnapshot.MOD_ID);
                    modDataDir.mkdir();
                    File fastScrDataDir = new File(modDataDir, "fast-screenshot");
                    fastScrDataDir.mkdir();
                    File imageDat = new File(fastScrDataDir, datName + ".dat");

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(imageDat))){
                        writer.write(new String(data.data));
                    }catch (IOException e){
                        LOGGER.error("Error write to file: {}", e.getMessage());
                    }

                    Minecraft.getInstance().execute(() -> {
                        try {
                            ByteArrayInputStream inputStream = new ByteArrayInputStream(data.data());
                            BufferedImage buffered = ImageIO.read(inputStream);
                            NativeImage image = new NativeImage(buffered.getWidth(), buffered.getHeight(), false);
                            for (int y = 0 ; y < image.getHeight() ; y++){
                                for (int x = 0 ; x < image.getWidth() ; x++){
                                    image.setPixel(x, y, buffered.getRGB(x, y));
                                }
                            }

                            Identifier textureId = Identifier.parse(data.textureId);
                            DynamicTexture texture = new DynamicTexture(textureId::toString, image);
                            Minecraft.getInstance().getTextureManager().register(textureId, texture);
                            fastScreenshotData.add(new Pair<>(textureId, new Vector2i(buffered.getWidth(), buffered.getHeight())));
                        } catch (IOException e) {
                            LOGGER.error("Error turn data into texture: {}", e.getMessage());
                        }
                    });
                }
            }
        }
    }
}
