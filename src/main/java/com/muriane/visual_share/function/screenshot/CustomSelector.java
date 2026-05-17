package com.muriane.visual_share.function.screenshot;

import com.muriane.visual_share.VisualShare;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class CustomSelector extends AbstractWidget {
    private static final WidgetSprites selector = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "screenshot/selector"));
    protected final int spriteWidth;
    protected final int spriteHeight;
    protected final SelectorCallback callback;
    protected float percent;

    public CustomSelector(int x, int y, int areaWidth, int areaHeight, int spriteWidth, int spriteHeight, float percent, SelectorCallback callback) {
        super(x, y, areaWidth, areaHeight, Component.empty());
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.callback = callback;
        this.percent = percent;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.percent = Math.max(0, Math.min(1, (float) ((event.y()-this.getY())/this.getHeight())));
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        this.percent = Math.max(0, Math.min(1, (float) ((event.y()-this.getY())/this.getHeight())));
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        callback.onChanged(this.percent);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int i, int i1, float v) {
        Identifier selectorId = selector.get(this.isActive(), this.isHoveredOrFocused());
        int x = (int) ((this.getX() + this.getWidth() / 2.0) - this.spriteWidth / 2.0);
        int y = (int) (this.getY() - this.spriteHeight / 2.0 + percent*this.getHeight());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, selectorId, x, y, spriteWidth, spriteHeight);
//        graphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), new Color(255,255,255).getRGB());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    public interface SelectorCallback{
        void onChanged(float percent);
    }
}
