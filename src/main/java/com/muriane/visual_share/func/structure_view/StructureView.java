package com.muriane.visual_share.func.structure_view;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.muriane.visual_share.VisualShare;
import com.muriane.visual_share.func.screenshot.ImageViewScreen;
import com.muriane.visual_share.func.screenshot.ImageViewWidget;
import com.muriane.visual_share.key.ModKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.slf4j.Logger;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.List;

public class StructureView {
    public static Logger LOGGER = LogUtils.getLogger();
    public static Vec3i point1;
    public static Vec3i point2;

    public static void structureSelection(Vec3i point){
        if (point1 == null || point2 != null){
            point1 = point;
            point2 = null;
        }else{
            point2 = point;
        }
        System.out.print(point1+" | "+point2+"\n");
    }

    public static void structureSave(){
        if (point1 != null && point2 != null){
            Level level = Minecraft.getInstance().level;
            BlockPos pos = new BlockPos(Math.min(point1.getX(), point2.getX()), Math.min(point1.getY(), point2.getY()), Math.min(point1.getZ(), point2.getZ()));
            BlockPos size = new BlockPos(Math.abs(point1.getX() - point2.getX()), Math.abs(point1.getY() - point2.getY()), Math.abs(point1.getZ() - point2.getZ()));
            boolean include = true;
            List<Block> ignoreBlocks = new ArrayList<>();

            File root = Minecraft.getInstance().gameDirectory;
            File modDir = new File(root, VisualShare.MOD_ID);
            modDir.mkdir();
            File strDir = new File(modDir, "structure");
            modDir.mkdir();
            boolean asText = false;

            if (level != null) {
                StructureTemplate structure = new StructureTemplate();
                structure.fillFromWorld(level, pos, size, include, ignoreBlocks);

                try {
                    StructureTemplateManager.save(strDir.toPath(), structure, asText);
                } catch (IOException e) {
                    LOGGER.error("Structure save error: {}", e.getMessage());
                }
            }
        }
    }

    @EventBusSubscriber
    public static class structureViewHolder{
        @SubscribeEvent
        public static void onKey(InputEvent.Key event){
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;

            if (mc.level != null && mc.screen == null) { // 用level来判断玩家是否已经在某个服务器中
                if (event.getAction() == InputConstants.PRESS){
                    if (event.getKey() == ModKeys.STRUCTURE_VIEW_SELECTION.getKey().getValue()) {
                        double range = 10;
                        float partialTicks = 1;
                        boolean withLiquids = false;

                        if (player != null) {
                            BlockHitResult result = (BlockHitResult) player.pick(range, partialTicks, withLiquids);
                            BlockPos pos = result.getBlockPos();
                            structureSelection(pos);
                        }
                    } else if (event.getKey() == ModKeys.STRUCTURE_VIEW_PREVIEW.getKey().getValue()) {
                        structureSave();
                    }
                }
            }
        }
    }
}
