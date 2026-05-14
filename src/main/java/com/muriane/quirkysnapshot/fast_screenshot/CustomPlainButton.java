package com.muriane.quirkysnapshot.fast_screenshot;

import com.mojang.blaze3d.platform.Window;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CustomPlainButton extends Button {
    public CustomPlainButton(float xPercent, float yPercent, float widthPercent, float heightPercent, int guiScale, Window window, Component message, OnPress onPress) {
        this((int) (window.getWidth()*xPercent)/guiScale, (int) (window.getHeight()*yPercent)/guiScale, (int) (window.getWidth()*widthPercent)/guiScale, (int) (window.getHeight()*heightPercent)/guiScale, message, onPress);
    }
    public CustomPlainButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.extractDefaultSprite(graphics);
        this.extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
    }

    public record PlainButtonList(int x, int y, int width, int height, List<Pair<Component, OnPress>> buttonInfoList) {
        public PlainButtonList(float xPercent, float yPercent, float widthPercent, float heightPercent, int guiScale, Window window, List<Pair<Component, Button.OnPress>> buttonInfoList) {
            this((int) (window.getWidth() * xPercent) / guiScale, (int) (window.getHeight() * yPercent) / guiScale, (int) (window.getWidth() * widthPercent) / guiScale, (int) (window.getHeight() * heightPercent) / guiScale, buttonInfoList);
        }

        public void addButtons(List<Pair<Component, Button.OnPress>> buttonInfoList) {
            this.buttonInfoList.addAll(buttonInfoList);
        }

        public void addButton(Component message, Button.OnPress onPress) {
            this.buttonInfoList.add(new Pair<>(message, onPress));
        }

        public List<CustomPlainButton> getButtonsInRow() {
            List<CustomPlainButton> buttonList = new ArrayList<>();
            int count = this.buttonInfoList.size();
            for (int index = 0; index < count; index++) {
                buttonList.add(new CustomPlainButton(
                        (int) (x + (1.0 * width / count) * index), y, (int) ((1.0) * width / count), height,
                        buttonInfoList.get(index).getFirst(),
                        buttonInfoList.get(index).getSecond()
                ));
            }
            return buttonList;
        }
    }
}
