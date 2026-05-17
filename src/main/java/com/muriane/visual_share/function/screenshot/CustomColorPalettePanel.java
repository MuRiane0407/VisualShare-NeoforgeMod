package com.muriane.visual_share.function.screenshot;

import com.mojang.blaze3d.platform.NativeImage;
import com.muriane.visual_share.VisualShare;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomColorPalettePanel extends AbstractWidget {
    private static final WidgetSprites background = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "screenshot/color_palette_panel"));
    protected final float scale;
    protected final float[] HSV;
    protected TextureAtlas guiSprites;
    protected final ColorPaletteCallback callback;
    protected final List<CustomSelector> HSVSelectors = new ArrayList<>();

    public CustomColorPalettePanel(int x, int y, float scale, float[] HSV, ColorPaletteCallback callback) {
        super(x, y, (int) (28*scale), (int) (38*scale), Component.empty());
        this.scale = scale;
        this.HSV = HSV;
        this.callback = callback;
        AtlasManager atlasManager = Minecraft.getInstance().getAtlasManager();
        this.guiSprites = atlasManager.getAtlasOrThrow(AtlasIds.GUI);
        this.HSVSelectors.addAll(List.of(
                new CustomSelector(
                        (int) (this.getX()+7*scale), (int) (this.getY()+7*scale), (int) (2*scale), (int) (24*scale), (int) (6*scale), (int) (1*scale),
                        this.HSV[0],
                        percent -> {
                            this.HSV[0] = percent;
                            this.callback.onHSVChanged(this.HSV);
                        }
                ),
                new CustomSelector(
                        (int) (this.getX()+13*scale), (int) (this.getY()+7*scale), (int) (2*scale), (int) (24*scale), (int) (6*scale), (int) (1*scale),
                        this.HSV[1],
                        percent -> {
                            this.HSV[1] = percent;
                            this.callback.onHSVChanged(this.HSV);
                        }
                ),
                new CustomSelector(
                        (int) (this.getX()+19*scale), (int) (this.getY()+7*scale), (int) (2*scale), (int) (24*scale), (int) (6*scale), (int) (1*scale),
                        this.HSV[2],
                        percent -> {
                            this.HSV[2] = percent;
                            this.callback.onHSVChanged(this.HSV);
                        }
                )
        ));
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        for (CustomSelector selector : HSVSelectors){
            selector.setY((int) (this.getY()+7*this.scale));
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int i, int i1, float v) {
        Identifier backgroundId = background.get(this.isActive(), this.isHoveredOrFocused());
        TextureAtlasSprite backgroundSprite = this.guiSprites.getSprite(backgroundId);
        NativeImage backgroundImage = backgroundSprite.contents().byMipLevel[0];
        if (backgroundImage != null) {
            extractColor(backgroundImage);

            DynamicTexture texture = new DynamicTexture(backgroundImage::toString, backgroundImage);
            graphics.blit(texture.getTextureView(), texture.getSampler(), this.getX(), this.getY(), this.getRight(), this.getBottom(), 0, 1, 0, 1);
        }
    }

    private void extractColor(NativeImage backgroundImage) {
        int y = 7;
        int x1 = 13;
        int x2 = 19;

        for (int offsetY = 0 ; offsetY < 24 ; offsetY++){
            Color color1 = Color.getHSBColor(this.HSV[0], offsetY/24f, this.HSV[2]);
            backgroundImage.setPixel(x1, y+offsetY, color1.getRGB());
            backgroundImage.setPixel(x1+1, y+offsetY, color1.getRGB());

            Color color2 = Color.getHSBColor(this.HSV[0], this.HSV[1], offsetY/24f);
            backgroundImage.setPixel(x2, y+offsetY, color2.getRGB());
            backgroundImage.setPixel(x2+1, y+offsetY, color2.getRGB());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return false;
    }

    public void toggleFade(){
        boolean fade = !this.active;
        this.setFade(fade);
    }

    public void setFade(boolean fade){
        this.active = fade;
        this.visible = fade;
        for (CustomSelector selector : HSVSelectors){
            selector.active = fade;
            selector.visible = fade;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    public List<CustomSelector> getHSVSelectors(){
        return this.HSVSelectors;
    }

    public interface ColorPaletteCallback{
        void onHSVChanged(float[] hsv);
    }
}
