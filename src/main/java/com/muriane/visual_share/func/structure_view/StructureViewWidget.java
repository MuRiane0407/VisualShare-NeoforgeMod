package com.muriane.visual_share.func.structure_view;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class StructureViewWidget extends AbstractWidget {
    public StructureViewWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int i, int i1, float v) {
        graphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0xFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
