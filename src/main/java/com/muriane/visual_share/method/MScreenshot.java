package com.muriane.visual_share.method;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.util.ARGB;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

import static com.muriane.visual_share.method.MMethod.getTimeId;

public class MScreenshot {
    /**
     * 截取整个屏幕
     */
    public static void takeScreenshot(RenderTarget target, int downscaleFactor, Consumer<NativeImage> callback){
        takeScreenshot(target, downscaleFactor, 0, 0, target.width, target.height, callback);
    }

    /**
     * 截取以屏幕中心为中心的正方形屏幕，不会超过屏幕大小
     * @param boxSize 正方体半边长
     * @param reverse 若为真，则boxSize意味着从最大半边长开始向内收缩的半边长
     */
    public static void takeScreenshot(RenderTarget target, int downscaleFactor, int boxSize, boolean reverse, Consumer<NativeImage> callback){
        int midX = target.width/2;
        int midY = target.height/2;
        int limit = Math.min(midX, midY);
        int areaLength = reverse ? Math.max(0, limit-boxSize) : Math.min(limit, boxSize);
        takeScreenshot(target, downscaleFactor, midX-areaLength, midY-areaLength, 2*areaLength, 2*areaLength, callback);
    }

    /**
     * 截取从(x, y)开始的大小为(width, height)的屏幕
     * @param x 从左下角原点开始，以右为正方向的横坐标
     * @param y 从左下角原点开始，以上为正方向的纵坐标
     * @param width 向右延申的宽度
     * @param height 向上延申的高度
     */
    public static void takeScreenshot(RenderTarget target, int downscaleFactor, int x, int y, int width, int height, Consumer<NativeImage> callback){
        int originWidth = target.width;
        int originHeight = target.height;
        GpuTexture sourceTexture = target.getColorTexture();
        if (sourceTexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        } else if (width % downscaleFactor == 0 && height % downscaleFactor == 0) {
            GpuBuffer buffer = RenderSystem.getDevice()
                    .createBuffer(() -> "MScreenshot buffer", 9, (long)originWidth * originHeight * sourceTexture.getFormat().pixelSize());
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .copyTextureToBuffer(
                            sourceTexture,
                            buffer,
                            0L,
                            () -> {
                                try (GpuBuffer.MappedView read = commandEncoder.mapBuffer(buffer, true, false)) {
                                    int outputHeight = height / downscaleFactor;
                                    int outputWidth = width / downscaleFactor;
                                    NativeImage image = new NativeImage(outputWidth, outputHeight, false);

                                    for (int yi = 0; yi < outputHeight; yi++) {
                                        for (int xi = 0; xi < outputWidth; xi++) {
                                            if (downscaleFactor == 1) {
                                                int argb = read.data().getInt((xi+x + (yi+y) * originWidth) * sourceTexture.getFormat().pixelSize());
                                                image.setPixelABGR(xi, height - yi - 1, argb | 0xFF000000);
                                            } else {
                                                int red = 0;
                                                int green = 0;
                                                int blue = 0;

                                                for (int i = 0; i < downscaleFactor; i++) {
                                                    for (int j = 0; j < downscaleFactor; j++) {
                                                        int argb = read.data()
                                                                .getInt(
                                                                        (xi * downscaleFactor + i + (yi * downscaleFactor + j) * originWidth) * sourceTexture.getFormat().pixelSize()
                                                                );
                                                        red += ARGB.red(argb);
                                                        green += ARGB.green(argb);
                                                        blue += ARGB.blue(argb);
                                                    }
                                                }

                                                int sampleCount = downscaleFactor * downscaleFactor;
                                                image.setPixelABGR(xi, outputHeight - yi - 1, ARGB.color(255, red / sampleCount, green / sampleCount, blue / sampleCount));
                                            }
                                        }
                                    }

                                    callback.accept(image);
                                }

                                buffer.close();
                            },
                            0
                    );
        } else {
            throw new IllegalArgumentException("Image size is not divisible by downscale factor");
        }
    }

    public static String getImageId(NativeImage image) {
        return getTimeId(image);
    }

    public static <T> String getTimeIdWithoutHash(T object){
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date(currentTimeMillis);
        return formatter.format(date);
    }
}
