package com.muriane.visual_share.fast_screenshot;

import com.github.avifimageio.AvifWriteParam;
import com.mojang.blaze3d.platform.ClipboardManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.muriane.visual_share.Config;
import com.muriane.visual_share.VisualShare;
import com.muriane.visual_share.key.ModKeys;
import com.muriane.visual_share.method.MScreenshot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.slf4j.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FastScreenshotScreen extends Screen {
    private static final WidgetSprites notFindSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/not_find"));
    private static final WidgetSprites selectionSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/selection"), Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/selection_highlight"));
    private static final WidgetSprites brushSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/brush"), Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/brush_highlight"));
    private static final WidgetSprites cutSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/cut"), Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/cut_highlight"));
    private static final WidgetSprites colorPaletteSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/color_palette"), Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/color_palette_highlight"));
    private static final WidgetSprites brushSizeSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/brush_size"), Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/brush_size_highlight"));
    private static final WidgetSprites undoSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/undo"), Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/undo_highlight"));
    private static final WidgetSprites redoSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/redo"), Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/redo_highlight"));
    private static final WidgetSprites saveSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/save"), Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/save_highlight"));
    private static final WidgetSprites shareSprites = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/share"), Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "fast_screenshot/share_highlight"));

    private static final Logger LOGGER = LogUtils.getLogger();
    private final Screen lastScreen;
    private final List<NativeImage> imageHistory = new ArrayList<>();
    private int step;
    private Component tip;
    private FastScreenshotImageWidget.InteractionMode imageInteractionMode;
    private float[] imageHSV;
    private float imageBrushSize;
    private FastScreenshotImageWidget imageWidget;
    private final CustomNumberPanel brushSizePanel;
    private final CustomColorPalettePanel colorPalettePanel;

    protected FastScreenshotScreen(Screen lastScreen, NativeImage image) {
        super(Component.empty());
        this.lastScreen = lastScreen;
        this.imageHistory.add(image);
        this.step = imageHistory.size()-1;
        this.tip = Component.empty();
        this.imageInteractionMode = FastScreenshotImageWidget.InteractionMode.SELECTION;
        this.imageHSV = new float[]{1, 1, 1};
        this.imageBrushSize = 2;
        this.colorPalettePanel = new CustomColorPalettePanel(
                2, 0, 2, this.imageHSV,
                hsv -> {
                    this.imageHSV = hsv;
                    this.reload();
                });
        this.colorPalettePanel.setFade(false);
        this.brushSizePanel = new CustomNumberPanel(
                2, 0, 2, this.imageBrushSize, 9, 1,
                number ->{
                    this.imageBrushSize = number;
                    this.reload();
                });
        this.brushSizePanel.setFade(false);
    }

    @Override
    protected void init() {
        Window window = Minecraft.getInstance().getWindow();
        int guiScale = window.getGuiScale();

        List<AbstractWidget> widgets = new ArrayList<>();
        widgets.addAll(initImage(guiScale, window));
        widgets.addAll(initButtons(guiScale, window));
        widgets.addAll(initLabel(guiScale, window));
        widgets.addAll(initOther(guiScale, window));
        this.renderables.addAll(widgets); // 渲染从后到前
        for (AbstractWidget widget : widgets.reversed()) { // 交互从前到后
            this.addWidget(widget);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    private List<AbstractWidget> initLabel(int guiScale, Window window) {
        CustomLabelWidget labelWidget = new CustomLabelWidget(
                0.125f, 0, 0.875f, 0, 0.75f, 0, 0, 32*guiScale,
                guiScale, window,
                this.tip
        );
        return List.of(labelWidget);
    }

    private List<AbstractWidget> initButtons(int guiScale, Window window) {
        List<AbstractWidget> buttons = new ArrayList<>();
        List<Pair<Pair<WidgetSprites, List<Component>>, Button.OnPress>> leftModeList = new ArrayList<>(
                List.of(new Pair<>(
                                new Pair<>(selectionSprites,
                                        List.of(
                                                Component.translatable("button.visual_share.fast_screenshot.selection.name").withStyle(ChatFormatting.BOLD),
                                                Component.translatable("button.visual_share.fast_screenshot.selection.info").withStyle(ChatFormatting.GRAY),
                                                Component.translatable("button.visual_share.fast_screenshot.shortcuts", Component.literal(ModKeys.SELECTION.getKey().getDisplayName().getString()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY)
                                        )
                                ),
                                button -> this.setImageInterActionMode(FastScreenshotImageWidget.InteractionMode.SELECTION)
                        ),
                        new Pair<>(
                                new Pair<>(brushSprites,
                                        List.of(
                                                Component.translatable("button.visual_share.fast_screenshot.brush.name").withStyle(ChatFormatting.BOLD),
                                                Component.translatable("button.visual_share.fast_screenshot.brush.info").withStyle(ChatFormatting.GRAY),
                                                Component.translatable("button.visual_share.fast_screenshot.shortcuts", Component.literal(ModKeys.BRUSH.getKey().getDisplayName().getString()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY)
                                        )
                                ),
                                button -> this.setImageInterActionMode(FastScreenshotImageWidget.InteractionMode.BRUSH)
                        ))
        );
        int leftModeButtonListHeight = 32*leftModeList.size()*guiScale;
        CustomImageButton.ImageButtonList leftModeButtonList = new CustomImageButton.ImageButtonList(
                0.125f, -6-32*guiScale, 0.125f, 0, 0, 32*guiScale, 0, leftModeButtonListHeight,
                guiScale, window,
                leftModeList
        );

        List<Pair<Pair<WidgetSprites, List<Component>>, Button.OnPress>> leftFunctionList = new ArrayList<>();
        if (this.imageInteractionMode == FastScreenshotImageWidget.InteractionMode.SELECTION){
            leftFunctionList.add(
                    new Pair<>(
                            new Pair<>(cutSprites,
                                    List.of(
                                            Component.translatable("button.visual_share.fast_screenshot.cut.name").withStyle(ChatFormatting.BOLD),
                                            Component.translatable("button.visual_share.fast_screenshot.cut.info").withStyle(ChatFormatting.GRAY),
                                            Component.translatable("button.visual_share.fast_screenshot.shortcuts", Component.literal(ModKeys.CUT.getKey().getDisplayName().getString()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY)
                                    )
                            ),
                            button -> this.imageCut()
                    )
            );
        }else if (this.imageInteractionMode == FastScreenshotImageWidget.InteractionMode.BRUSH){
            leftFunctionList.addAll(List.of(
                    new Pair<>(
                            new Pair<>(colorPaletteSprites,
                                    List.of(
                                            Component.translatable("button.visual_share.fast_screenshot.color_palette.name").withStyle(ChatFormatting.BOLD),
                                            Component.translatable("button.visual_share.fast_screenshot.color_palette.info").withStyle(ChatFormatting.GRAY),
                                            Component.translatable("button.visual_share.fast_screenshot.shortcuts", Component.literal(ModKeys.COLOR_PALETTE.getKey().getDisplayName().getString()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY)
                                    )
                            ),
                            button -> this.imageColorPalette()
                    ),
                    new Pair<>(
                            new Pair<>(brushSizeSprites,
                                    List.of(
                                            Component.translatable("button.visual_share.fast_screenshot.brush_size.name").withStyle(ChatFormatting.BOLD),
                                            Component.translatable("button.visual_share.fast_screenshot.brush_size.info").withStyle(ChatFormatting.GRAY),
                                            Component.translatable("button.visual_share.fast_screenshot.shortcuts", Component.literal(ModKeys.BRUSH_SIZE.getKey().getDisplayName().getString()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY)
                                    )
                            ),
                            button -> this.imageBrushSize()
                    )
            ));
        }
        int leftFunctionButtonListHeightOffset = leftModeButtonListHeight;
        CustomImageButton.ImageButtonList leftFunctionButtonList = new CustomImageButton.ImageButtonList(
                0.125f, -6-32*guiScale, 0.125f, leftFunctionButtonListHeightOffset, 0, 32*guiScale, 0.75f, -leftFunctionButtonListHeightOffset,
                guiScale, window,
                leftFunctionList
        );

        CustomImageButton.ImageButtonList rightFunctionButtonList = new CustomImageButton.ImageButtonList(
                0.875f, 6, 0.125f, 0, 0, 32*guiScale, 0.75f, 0,
                guiScale, window,
                List.of(
                        new Pair<>(
                                new Pair<>(saveSprites,
                                        List.of(
                                                Component.translatable("button.visual_share.fast_screenshot.save.name").withStyle(ChatFormatting.BOLD),
                                                Component.translatable("button.visual_share.fast_screenshot.save.info").withStyle(ChatFormatting.GRAY),
                                                Component.translatable("button.visual_share.fast_screenshot.shortcuts", Component.literal("Ctrl"+"+"+ModKeys.SAVE.getKey().getDisplayName().getString()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY)
                                        )
                                ),
                                button -> this.imageSave()
                        ),
                        new Pair<>(
                                new Pair<>(shareSprites,
                                        List.of(
                                                Component.translatable("button.visual_share.fast_screenshot.share.name").withStyle(ChatFormatting.BOLD),
                                                Component.translatable("button.visual_share.fast_screenshot.share.info").withStyle(ChatFormatting.GRAY),
                                                Component.translatable("button.visual_share.fast_screenshot.shortcuts", Component.literal("Ctrl"+"+"+ModKeys.SHARE.getKey().getDisplayName().getString()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY)
                                        )
                                ),
                                button -> this.imageShare()
                        ),
                        new Pair<>(
                                new Pair<>(undoSprites,
                                        List.of(
                                                Component.translatable("button.visual_share.fast_screenshot.undo.name").withStyle(ChatFormatting.BOLD),
                                                Component.translatable("button.visual_share.fast_screenshot.undo.info").withStyle(ChatFormatting.GRAY),
                                                Component.translatable("button.visual_share.fast_screenshot.shortcuts", Component.literal("Ctrl"+"+"+ModKeys.UNDO_REDO.getKey().getDisplayName().getString()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY)
                                        )
                                ),
                                button -> this.imageUndo()
                        ),
                        new Pair<>(
                                new Pair<>(redoSprites,
                                        List.of(
                                                Component.translatable("button.visual_share.fast_screenshot.redo.name").withStyle(ChatFormatting.BOLD),
                                                Component.translatable("button.visual_share.fast_screenshot.redo.info").withStyle(ChatFormatting.GRAY),
                                                Component.translatable("button.visual_share.fast_screenshot.shortcuts", Component.literal("Ctrl"+"+"+"Shift"+"+"+ModKeys.UNDO_REDO.getKey().getDisplayName().getString()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY)
                                        )
                                ),
                                button -> this.imageRedo()
                        )
                )
        );
        buttons.addAll(leftModeButtonList.getButtonsInColumn());
        buttons.addAll(leftFunctionButtonList.getButtonsInColumn());
        buttons.addAll(rightFunctionButtonList.getButtonsInColumn());
        return buttons;
    }

    private List<AbstractWidget> initImage(int guiScale, Window window) {
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
            this.imageWidget = new FastScreenshotImageWidget(
                    image,
                    0.125f + xOffset * scale, 0.125f + yOffset * scale, scale * useScale, scale * useScale,
                    guiScale, window, this.imageInteractionMode,
                    this.imageHSV, this.imageBrushSize,
                    this::addNewImage);
            return List.of(imageWidget);
        }else{
            LOGGER.error("The image has been closed, please submit an issue");
        }
        return List.of();
    }

    private List<AbstractWidget> initOther(int guiScale, Window window){
        List<AbstractWidget> others = new ArrayList<>();
        this.colorPalettePanel.setY((window.getHeight()/guiScale-this.colorPalettePanel.getHeight())/2);
        others.add(this.colorPalettePanel);
        others.addAll(this.colorPalettePanel.getHSVSelectors());
        this.brushSizePanel.setY((window.getHeight()/guiScale-this.brushSizePanel.getHeight())/2);
        others.add(this.brushSizePanel);
        others.addAll(this.brushSizePanel.getSelectors());
        return others;
    }

    public void reload(){
//        System.out.print("reload\n");
        this.rebuildWidgets();
    }

    public void fadeOtherWidgets(){
        this.colorPalettePanel.setFade(false);
        this.brushSizePanel.setFade(false);
    }

    @Override
    public void onClose() {
        if (this.colorPalettePanel.active || this.brushSizePanel.active){
            this.fadeOtherWidgets();
            Minecraft.getInstance().setScreen(this);
        }else {
            Minecraft.getInstance().setScreen(lastScreen);
        }
    }


    public FastScreenshotImageWidget.InteractionMode getImageInterActionMode() {
        return this.imageInteractionMode;
    }

    public void setImageInterActionMode(FastScreenshotImageWidget.InteractionMode mode) {
        this.imageInteractionMode = mode;
        this.fadeOtherWidgets();
        this.reload();
    }

    public void imageCut(){
        NativeImage newImage = this.imageWidget.cutImageWithSelection();
        if (newImage != null){
            this.addNewImage(newImage);
        }else{
            this.tip = Component.translatable("tip.visual_share.fast_screenshot.no_selection_or_too_small").withStyle(ChatFormatting.RED);
        }
        this.reload();
    }

    public void imageColorPalette(){
        if (!this.colorPalettePanel.active) {
            fadeOtherWidgets();
            this.colorPalettePanel.toggleFade();
        }else{
            fadeOtherWidgets();
        }
    }

    public void imageBrushSize(){
        if (!this.brushSizePanel.active) {
            fadeOtherWidgets();
            this.brushSizePanel.toggleFade();
        }else{
            fadeOtherWidgets();
        }
    }

    public void imageUndo(){
        if (this.step > 0){
            this.step -= 1;
        }else{
            this.tip = Component.translatable("tip.visual_share.fast_screenshot.cant_undo").withStyle(ChatFormatting.RED);
        }
        this.reload();
    }

    public void imageRedo(){
        if (this.step < this.imageHistory.size()-1){
            this.step += 1;
        }else{
            this.tip = Component.translatable("tip.visual_share.fast_screenshot.cant_redo").withStyle(ChatFormatting.RED);
        }
        this.reload();
    }

    public void imageSave(){
        Minecraft mc = Minecraft.getInstance();
        String imageId = MScreenshot.getImageId(this.imageHistory.get(this.step));
        File scrDir = new File(mc.gameDirectory, "screenshots");
        scrDir.mkdir();
        File modScrDir = new File(scrDir, VisualShare.MOD_ID);
        modScrDir.mkdir();
        File fastScrDir = new File(modScrDir, "fast_screenshot");
        fastScrDir.mkdir();
        File file = new File(fastScrDir, imageId + ".png");

        Util.ioPool().execute(
                () -> {
                    try {
                        this.imageHistory.get(this.step).writeToFile(file);
                        this.tip = Component.translatable("tip.visual_share.fast_screenshot.success_save",
                                Component.literal(imageId)
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(style -> style.withClickEvent(new ClickEvent.OpenFile(file)))); // 然而界面内点了没用（或者可能是我的渲染方式点了没用）
                    } catch (Exception e) {
                        LOGGER.error("Couldn't save image {}", e.getMessage());
                        this.tip = Component.translatable("tip.visual_share.fast_screenshot.cant_save", e).withStyle(ChatFormatting.RED);
                    }
                    this.reload();
                });
    }

    public void imageShare(){
        String prefix = Config.SERVER.FAST_SCREENSHOT_SHARE_PREFIX.get();
        String subfix = Config.SERVER.FAST_SCREENSHOT_SHARE_SUBFIX.get();
        Config.OverrideMode overrideMode = Config.SERVER.FAST_SCREENSHOT_SHARE_OVERRIDE_CLIENT_PARAM.get();

        NativeImage image = this.imageHistory.get(this.step);
        String textureId = MScreenshot.getImageId(image);

        BufferedImage buffer = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getPixel(x, y);
                buffer.setRGB(x, y, argb);
            }
        }

        if (Config.SERVER.FAST_SCREENSHOT_SHARE_MAX_SIZE.get() != -1 && (image.getWidth() > Config.SERVER.FAST_SCREENSHOT_SHARE_MAX_SIZE.get() || image.getHeight() > Config.SERVER.FAST_SCREENSHOT_SHARE_MAX_SIZE.get())) {
            float widthScale = Config.SERVER.FAST_SCREENSHOT_SHARE_MAX_SIZE.get()*1f / image.getWidth();
            float heightScale = Config.SERVER.FAST_SCREENSHOT_SHARE_MAX_SIZE.get()*1f / image.getHeight();
            float scale = Math.min(widthScale, heightScale);
            int width = (int) (image.getWidth()*scale);
            int height = (int) (image.getHeight()*scale);

            java.awt.Image scaledImage = buffer.getScaledInstance(width, height, overrideMode != Config.OverrideMode.NO ? Config.SERVER.FAST_SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM.get().getAlgorithm() : Config.CLIENT.FAST_SCREENSHOT_SHARE_INTERPOLATION_ALGORITHM.get().getAlgorithm());
            buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = buffer.createGraphics();
            graphics.drawImage(scaledImage, 0, 0, null);
            graphics.dispose();
        }

        Config.DataType type = overrideMode != Config.OverrideMode.NO ? Config.SERVER.FAST_SCREENSHOT_SHARE_DATA_TYPE.get() : Config.CLIENT.FAST_SCREENSHOT_SHARE_DATA_TYPE.get();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (type.equals(Config.DataType.png)) {
            try{
                ImageIO.write(buffer, type.getType(), outputStream);
                LOGGER.info("Share image {}: png", textureId);
            } catch (IOException e) {
                LOGGER.error("Error write to {}: {}", type, e.getMessage());
            }
        }
        else if (type.equals(Config.DataType.avif)){
            ImageWriter writer = ImageIO.getImageWritersByFormatName("avif").next();
            AvifWriteParam param = (AvifWriteParam) writer.getDefaultWriteParam();

            int quality, speed;
            boolean lossless;
            if (overrideMode == Config.OverrideMode.ALL){
                quality = Config.SERVER.FAST_SCREENSHOT_SHARE_AVIF_QUALITY.get();
                speed = Config.SERVER.FAST_SCREENSHOT_SHARE_AVIF_SPEED.get();
                lossless = Config.SERVER.FAST_SCREENSHOT_SHARE_AVIF_LOSSLESS.get();
            }else if (overrideMode == Config.OverrideMode.MAX){
                quality = Math.min(Config.SERVER.FAST_SCREENSHOT_SHARE_AVIF_QUALITY.get(), Config.CLIENT.FAST_SCREENSHOT_SHARE_AVIF_QUALITY.get());
                speed = Math.min(Config.SERVER.FAST_SCREENSHOT_SHARE_AVIF_SPEED.get(), Config.CLIENT.FAST_SCREENSHOT_SHARE_AVIF_SPEED.get());
                lossless = Config.SERVER.FAST_SCREENSHOT_SHARE_AVIF_LOSSLESS.get() && Config.CLIENT.FAST_SCREENSHOT_SHARE_AVIF_LOSSLESS.get();
            }else if (overrideMode == Config.OverrideMode.MIN){
                quality = Math.max(Config.SERVER.FAST_SCREENSHOT_SHARE_AVIF_QUALITY.get(), Config.CLIENT.FAST_SCREENSHOT_SHARE_AVIF_QUALITY.get());
                speed = Math.max(Config.SERVER.FAST_SCREENSHOT_SHARE_AVIF_SPEED.get(), Config.CLIENT.FAST_SCREENSHOT_SHARE_AVIF_SPEED.get());
                lossless = Config.SERVER.FAST_SCREENSHOT_SHARE_AVIF_LOSSLESS.get() || Config.CLIENT.FAST_SCREENSHOT_SHARE_AVIF_LOSSLESS.get();
            }else{
                quality = Config.CLIENT.FAST_SCREENSHOT_SHARE_AVIF_QUALITY.get();
                speed = Config.CLIENT.FAST_SCREENSHOT_SHARE_AVIF_SPEED.get();
                lossless = Config.CLIENT.FAST_SCREENSHOT_SHARE_AVIF_LOSSLESS.get();
            }

            param.setQuality(quality); // 0-100，默认 75
            param.setSpeed(speed); // 0-10，默认 6（越高越快）
            param.setLossless(lossless); // true 为无损编码

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream)){
                writer.setOutput(ios);
                writer.write(null, new IIOImage(buffer, null, null), param);
            }catch (IOException e){
                LOGGER.error("Error write to {}: {}", type, e.getMessage());
            }
            writer.dispose();
            LOGGER.info("Share image {}: avif quality:{} speed:{} lossless:{}", textureId, quality, speed, lossless);
        }

        ClipboardManager clipboard = new ClipboardManager();
        clipboard.setClipboard(this.minecraft.getWindow(), prefix + textureId + subfix);
        this.tip = Component.translatable("tip.visual_share.fast_screenshot.share");

        ClientPacketDistributor.sendToServer(new FastScreenshot.ImageData(outputStream.toByteArray(), textureId, type.getType()));
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
