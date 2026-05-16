package com.muriane.quirkysnapshot.fast_screenshot;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;

public class CustomImageWidget extends AbstractWidget {
    protected NativeImage image;
    protected int interactionOffset = 2;

    public CustomImageWidget(float xPercent, float yPercent, float widthPercent, float heightPercent, int guiScale, Window window, NativeImage image) {
        this((int) (window.getWidth()*xPercent)/guiScale, (int) (window.getHeight()*yPercent)/guiScale, (int) ((float) image.getWidth()*widthPercent/guiScale), (int) ((float) image.getHeight()*heightPercent/guiScale), image);
    }
    public CustomImageWidget(int x, int y, int width, int height, NativeImage image) {
        super(x, y, width, height, Component.empty());
        this.image = image;
    }

    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        DynamicTexture texture = new DynamicTexture(image::toString, image);
        graphics.blit(texture.getTextureView(), texture.getSampler(), this.getX(), this.getY(), this.getRight(), this.getBottom(), 0, 1, 0, 1);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.isActive() && mouseX >= (double)this.getX()-interactionOffset && mouseY >= (double)this.getY()-interactionOffset && mouseX <= (double)this.getRight()+interactionOffset && mouseY <= (double)this.getBottom()+interactionOffset;
    }
}
