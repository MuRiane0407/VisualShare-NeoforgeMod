package com.muriane.visual_share.func.drawing_board;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.muriane.visual_share.Config;
import com.muriane.visual_share.func.screenshot.ImageViewScreen;
import com.muriane.visual_share.func.screenshot.ImageViewWidget;
import com.muriane.visual_share.key.ModKeys;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

import java.awt.*;

import static com.muriane.visual_share.func.screenshot.Screenshot.imageViewScreen;

public class DrawingBoard {
    @EventBusSubscriber
    public static class DrawingBoardHolder {
        /// 按键检测
        @SubscribeEvent
        public static void onKey(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (event.getKey() == ModKeys.DRAWING_BOARD_DRAWING_BOARD.getKey().getValue() && event.getAction() == InputConstants.PRESS) {
                if (mc.level != null && !(mc.screen instanceof ImageViewScreen)) { // 用level来判断玩家是否已经在某个服务器中
                    NativeImage image = new NativeImage(Config.CLIENT.DRAWING_BOARD_WIDTH.get(), Config.CLIENT.DRAWING_BOARD_HEIGHT.get(), true);

                    Color color = new Color(256*256*255+256*255+255);
                    Color backgroundColor = new Color(Config.CLIENT.DRAWING_BOARD_BACKGROUND_COLOR.get());
                    for (int y = 0 ; y < image.getHeight() ; y++){
                        for (int x = 0 ; x < image.getWidth() ; x++){
                            image.setPixel(x, y, backgroundColor.getRGB());
                        }
                    }

                    imageViewScreen = new ImageViewScreen(mc.screen, image);
                    imageViewScreen.setImageInterActionMode(ImageViewWidget.InteractionMode.BRUSH);
                    mc.setScreen(imageViewScreen);
                }
            }
        }
    }
}
