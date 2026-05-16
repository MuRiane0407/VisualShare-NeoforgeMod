package com.muriane.quirkysnapshot.fast_screenshot;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CustomLabelWidget extends AbstractWidget {
    public CustomLabelWidget(float xPercent, int xOffset, float yPercent, int yOffset, float widthPercent, int widthOffset, float heightPercent, int heightOffset, int guiScale, Window window, Component message) {
        this((int) (window.getWidth() * xPercent + xOffset) / guiScale, (int) (window.getHeight() * yPercent + yOffset) / guiScale, (int) (window.getWidth() * widthPercent + widthOffset) / guiScale, (int) (window.getHeight() * heightPercent + heightOffset) / guiScale, message);
    }
    public CustomLabelWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int i, int i1, float v) {
        Component message = this.getMessage();
        if (this.getFGColor() != -1) {
            message = message.copy().withStyle((style) -> style.withColor(this.getFGColor()));
        }
        this.extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), message, 0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.isActive()) {
            return false;
        } else {
            if (this.isValidClickButton(event.buttonInfo())) {
                boolean isMouseOver = this.isMouseOver(event.x(), event.y());
                if (isMouseOver) {
                    this.onClick(event, doubleClick);
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
