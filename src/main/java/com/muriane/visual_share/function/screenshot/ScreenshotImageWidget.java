package com.muriane.visual_share.function.screenshot;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.slf4j.Logger;

import java.awt.*;

public class ScreenshotImageWidget extends CustomImageWidget{
    private final Logger LOGGER = LogUtils.getLogger();
    protected ImageCallback callback;
    protected InteractionMode mode;
    protected boolean release;
    protected Vector2i startPointInImage;
    protected Vector2i endPointInImage;
    protected Color brushColor;
    protected float brushSize;
    protected Vector2i brushLastPoint = null;

    public ScreenshotImageWidget(NativeImage image, float xPercent, float yPercent, float widthPercent, float heightPercent, int guiScale, @NotNull Window window, InteractionMode mode, float[] hsv, float brushSize, ImageCallback callback) {
        super((int) (window.getWidth()*xPercent)/guiScale, (int) (window.getHeight()*yPercent)/guiScale, (int) ((float) image.getWidth()*widthPercent/guiScale), (int) ((float) image.getHeight()*heightPercent/guiScale), image);
        this.callback = callback;
        this.mode = mode;
        this.brushColor = new Color(Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]));
        this.brushSize = brushSize;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        extractOutline(graphics);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        extractSelection(graphics);
    }

    public void extractOutline(GuiGraphicsExtractor graphics){
        int outlineSize = 2;
        int outlineColor = new Color(255, 255, 255).getRGB();
        // 图片后渲染就能把不该画的地方覆盖掉了（迫真边框）
        graphics.fill(this.getX()-outlineSize, this.getY()-outlineSize, this.getRight()+outlineSize, this.getBottom()+outlineSize, outlineColor);
    }

    public void extractSelection(GuiGraphicsExtractor graphics){
        if (startPointInImage != null && endPointInImage != null){
            Vector2i startPointInWidget = imageToWidget(startPointInImage);
            Vector2i endPointInWidget = imageToWidget(endPointInImage);
            int offsetX = 0;
            int offsetY = 0;
            int leftX = Math.min(startPointInWidget.x, endPointInWidget.x);
            int rightX = Math.max(startPointInWidget.x, endPointInWidget.x);
            int upY = Math.min(startPointInWidget.y, endPointInWidget.y);
            int downY = Math.max(startPointInWidget.y, endPointInWidget.y);
            if (leftX != rightX && upY != downY) {
                // 内框
                int borderColor = new Color(200, 200, 200).getRGB();
                graphics.fill(leftX+offsetX, upY+offsetY, rightX+offsetX, upY+1+offsetY, borderColor);
                graphics.fill(leftX+offsetX, upY+1+offsetY, leftX+1+offsetX, downY-1+offsetY, borderColor);
                graphics.fill(rightX-1+offsetX, upY+1+offsetY, rightX+offsetX, downY-1+offsetY, borderColor);
                graphics.fill(leftX+offsetX, downY-1+offsetY, rightX+offsetX, downY+offsetY, borderColor);
                // 外面
                if (this.release) {
                    int outsideColor = new Color(126, 126, 126, 128).getRGB();
                    graphics.fill(this.getX(), this.getY(), this.getRight(), upY+offsetY, outsideColor);
                    graphics.fill(this.getX(), upY+offsetY, leftX+offsetX, downY+offsetY, outsideColor);
                    graphics.fill(rightX+offsetX, upY+offsetY, this.getRight(), downY+offsetY, outsideColor);
                    graphics.fill(this.getX(), downY+offsetY, this.getRight(), this.getBottom(), outsideColor);
                }
            }
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.mode == InteractionMode.SELECTION) {
            this.release = false;
            this.endPointInImage = null;
            this.startPointInImage = mousePointInImage(event);
        }
//            this.screen.fadeOtherWidgets();
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        if (this.mode == InteractionMode.SELECTION) {
            this.endPointInImage = mousePointInImage(event);
        }else if (this.mode == InteractionMode.BRUSH){
            Vector2i newPoint = mousePointInImageWithoutLimit(event);
            if (this.brushLastPoint != null){
                int width = newPoint.x-this.brushLastPoint.x;
                int height = newPoint.y-this.brushLastPoint.y;
                int max = Math.max(Math.abs(width), Math.abs(height));
                for (int i = 1 ; i < max ; i++){
                    Vector2i midPoint = new Vector2i(
                            (int) Math.round(this.brushLastPoint.x+1.0/max*i*width),
                            (int) Math.round(this.brushLastPoint.y+1.0/max*i*height)
                    );
                    brushAddPoint(midPoint);
                }
            }
            this.brushLastPoint = newPoint;
            brushAddPoint(newPoint);
        }
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        if (this.mode == InteractionMode.SELECTION) {
            this.release = true;
            this.endPointInImage = mousePointInImage(event);
        }else if (this.mode == InteractionMode.BRUSH){
            this.brushLastPoint = null;
            this.callback.onSendingImage(this.image);
        }
    }

    public void brushAddPoint(Vector2i point){
        float brushOffset = this.brushSize-1;
        if (point.x >= -brushOffset && point.x <= this.image.getWidth()+brushOffset && point.y >= -brushOffset && point.y <= this.image.getHeight()+brushOffset){ // 点有没有能在图内画的部分
            for (float offsetY = -this.brushSize+1 ; offsetY <= this.brushSize-1 ; offsetY++){
                for (float offsetX = -this.brushSize+1 ; offsetX <= this.brushSize-1 ; offsetX++){
                    float x = point.x+offsetX;
                    float y = point.y+offsetY;
                    if (x >= 0 && x < this.image.getWidth() && y >= 0 && y < this.image.getHeight()) { // 画的点在不在图内
                        if (Math.pow(offsetX, 2) + Math.pow(offsetY, 2) <= Math.pow(brushOffset+0.5, 2)) { // 画的点在不在圆范围内
                            this.image.setPixel((int) x, (int) y, this.brushColor.getRGB());
                        }
                    }
                }
            }
        }
    }

    public NativeImage cutImageWithSelection(){
        if (startPointInImage != null && endPointInImage != null){
            int x = Math.min(this.startPointInImage.x, this.endPointInImage.x);
            int y = Math.min(this.startPointInImage.y, this.endPointInImage.y);
            int width = Math.abs(this.startPointInImage.x-this.endPointInImage.x);
            int height = Math.abs(this.startPointInImage.y-this.endPointInImage.y);

            if (width != 0 && height != 0) {
                NativeImage newImage = new NativeImage(width, height, false);
                this.image.copyRect(newImage, x, y, 0, 0, width, height, false, false);

                return newImage;
            }else{
                LOGGER.warn("The selection is too small");
            }
        }
        return null;
    }

    public Vector2i imageToWidget(Vector2i inImage){
        double widgetX = (inImage.x)*((double) this.getWidth()/image.getWidth()) + this.getX();
        double widgetY = (inImage.y)*((double) this.getHeight()/image.getHeight()) + this.getY();
        int x = (int) Math.max(this.getX(), Math.min(this.getRight(), widgetX));
        int y = (int) Math.max(this.getY(), Math.min(this.getBottom(), widgetY));
        return new Vector2i(x, y);
    }

    public Vector2i mousePointInWidget(MouseButtonEvent event){
        Vector2i inImage = mousePointInImage(event);
        return imageToWidget(inImage);
    }

    public Vector2i mousePointInImage(MouseButtonEvent event){
        double widgetX = event.x();
        double widgetY = event.y();
        double imageX = (widgetX-this.getX())*((double) image.getWidth()/this.getWidth());
        double imageY = (widgetY-this.getY())*((double) image.getHeight()/this.getHeight());
        int x = (int) Math.round(Math.max(0, Math.min(this.image.getWidth(), imageX)));
        int y = (int) Math.round(Math.max(0, Math.min(this.image.getHeight(), imageY)));
        return new Vector2i(x, y);
    }

    public Vector2i mousePointInImageWithoutLimit(MouseButtonEvent event){
        double widgetX = event.x();
        double widgetY = event.y();
        double imageX = (widgetX-this.getX())*((double) image.getWidth()/this.getWidth());
        double imageY = (widgetY-this.getY())*((double) image.getHeight()/this.getHeight());
        int x = (int) Math.round(imageX);
        int y = (int) Math.round(imageY);
        return new Vector2i(x, y);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isActive()) {
            if (this.isValidClickButton(event.buttonInfo())) {
                boolean isMouseOver = this.isMouseOver(event.x(), event.y());
                if (isMouseOver) {
                    this.onClick(event, doubleClick);
                    return true;
                }
            }

        }
        return false;
    }

    public InteractionMode getMode(){
        return this.mode;
    }

    public void setMode(InteractionMode mode){
        this.mode = mode;
    }

    public interface ImageCallback{
        void onSendingImage(NativeImage image);
    }

    public enum InteractionMode{
        SELECTION, BRUSH;
    }
}
