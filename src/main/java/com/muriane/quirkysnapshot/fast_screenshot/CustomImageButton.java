package com.muriane.quirkysnapshot.fast_screenshot;

import com.mojang.blaze3d.platform.Window;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CustomImageButton extends ImageButton {
    private final List<Component> message;

    public CustomImageButton(int x, int y, int width, int height, WidgetSprites sprites, List<Component> message, OnPress onPress) {
        super(x, y, width, height, sprites, onPress);
        this.message = message;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null && isActive() && isHovered()){
            graphics.setComponentTooltipForNextFrame(screen.getFont(), this.message, mouseX, mouseY);
        }
    }

    public record ImageButtonList(int x, int y, int width, int height, List<Pair<Pair<WidgetSprites, List<Component>>, Button.OnPress>> buttonInfoList){
        public ImageButtonList(float xPercent, float yPercent, float widthPercent, float heightPercent, int guiScale, Window window, List<Pair<Pair<WidgetSprites, List<Component>>, Button.OnPress>> buttonInfoList) {
            this((int) (window.getWidth() * xPercent) / guiScale, (int) (window.getHeight() * yPercent) / guiScale, (int) (window.getWidth() * widthPercent) / guiScale, (int) (window.getHeight() * heightPercent) / guiScale, buttonInfoList);
        }

        public ImageButtonList(float xPercent, int xOffset, float yPercent, int yOffset, float widthPercent, int widthOffset, float heightPercent, int heightOffset, int guiScale, Window window, List<Pair<Pair<WidgetSprites, List<Component>>, Button.OnPress>> buttonInfoList) {
            this((int) (window.getWidth() * xPercent + xOffset) / guiScale, (int) (window.getHeight() * yPercent + yOffset) / guiScale, (int) (window.getWidth() * widthPercent + widthOffset) / guiScale, (int) (window.getHeight() * heightPercent + heightOffset) / guiScale, buttonInfoList);
        }

        public void addButtons(List<Pair<Pair<WidgetSprites, List<Component>>, Button.OnPress>> buttonInfoList) {
            this.buttonInfoList.addAll(buttonInfoList);
        }

        public void addButton(WidgetSprites sprites, List<Component> hoverOnMessage, Button.OnPress onPress) {
            this.buttonInfoList.add(new Pair<>(new Pair<>(sprites, hoverOnMessage), onPress));
        }

        public List<ImageButton> getButtonsInColumn() {
            List<ImageButton> buttonList = new ArrayList<>();
            int count = this.buttonInfoList.size();
            int size = width;
            int yOffset = Math.min(size+1, height/count);
            for (int index = 0; index < count; index++) {
                buttonList.add(new CustomImageButton(
                        x, y + index*yOffset, size, size,
                        buttonInfoList.get(index).getFirst().getFirst(),
                        buttonInfoList.get(index).getFirst().getSecond(),
                        buttonInfoList.get(index).getSecond()
                ));
            }
            return buttonList;
        }
    }
}
