package com.muriane.visual_share.widget;

import com.muriane.visual_share.VisualShare;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CustomNumberPanel extends AbstractWidget {
    private static final WidgetSprites background = new WidgetSprites(Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "screenshot/number_panel"));
    protected final float scale;
    protected float percent;
    protected final float numberScale;
    protected final float numberOffset;
    protected final NumberPanelCallback callback;
    protected final List<CustomSelector> selectors = new ArrayList<>();

    public CustomNumberPanel(int x, int y, float scale, float number, float numberScale, float numberOffset, NumberPanelCallback callback) {
        super(x, y, (int) (22*scale), (int) (38*scale), Component.empty());
        this.scale = scale;
        this.percent = number/numberScale;
        this.numberScale = numberScale;
        this.numberOffset = numberOffset;
        this.callback = callback;
        this.selectors.add(
                new CustomSelector(
                        (int) (this.getX()+7*scale), (int) (this.getY()+7*scale), (int) (1*scale), (int) (24*scale), (int) (5*scale), (int) (1*scale),
                        this.percent,
                        p -> {
                            this.percent = p;
                            this.callback.onChanged(this.percent*this.numberScale+this.numberOffset);
                        }
                )
        );
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        for (CustomSelector selector : selectors){
            selector.setY((int) (this.getY()+7*this.scale));
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int i, int i1, float v) {
        Identifier backgroundId = background.get(this.isActive(), this.isHoveredOrFocused());
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, backgroundId, this.getX(), this.getY(), this.width, this.height);
        int left = (int) (this.getX() + 9*this.scale);
        int right = (int) (this.getX() + this.getWidth() - 3*this.scale);
        int top = this.getY();
        int bottom = this.getY() + this.getHeight();
        graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE).acceptScrollingWithDefaultCenter(Component.literal(String.format("%.1f", (this.percent*this.numberScale+this.numberOffset))), left, right, top, bottom);
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
        for (CustomSelector selector : selectors){
            selector.active = fade;
            selector.visible = fade;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    public List<CustomSelector> getSelectors(){
        return this.selectors;
    }

    public interface NumberPanelCallback{
        void onChanged(float number);
    }
}
