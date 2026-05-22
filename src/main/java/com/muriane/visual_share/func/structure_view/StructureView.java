package com.muriane.visual_share.func.structure_view;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.muriane.visual_share.VisualShare;
import com.muriane.visual_share.key.ModKeys;
import com.muriane.visual_share.method.MScreenshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StructureView {
    public static Logger LOGGER = LogUtils.getLogger();
    public static boolean selectionMode;
    public static Vec3i point1;
    public static Vec3i point2;
    public static Map<String, StructureTemplate> structureList = new HashMap<>();

    // 启用或关闭选区模式
    public static void structureSelection(){
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            point1 = point2 = null;
            if (!selectionMode) {
                player.sendOverlayMessage(Component.literal("Start structure selection"));
            }else{
                player.sendOverlayMessage(Component.literal("Cancel structure selection"));
            }
            selectionMode = !selectionMode;
        }
    }

    // 进入预览屏幕
    public static void structureView(){
        Player player = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;
        if (player != null) {
            if (point1 != null && point2 != null) {
                Vec3i size = new Vec3i(Math.abs(point1.getX()-point2.getX())+1, Math.abs(point1.getY()-point2.getY())+1, Math.abs(point1.getZ()-point2.getZ())+1);

                if (size.getX() * size.getY() * size.getZ() <= 10000) {
                    BlockPos pos = new BlockPos(Math.min(point1.getX(), point2.getX()), Math.min(point1.getY(), point2.getY()), Math.min(point1.getZ(), point2.getZ()));
                    boolean includeEntities = true;
                    List<Block> ignoreBlocks = new ArrayList<>();

                    if (level != null) {
                        StructureTemplate structure = new StructureTemplate();
                        structure.fillFromWorld(level, pos, size, includeEntities, ignoreBlocks);

                        Minecraft.getInstance().setScreen(new StructureViewScreen(structure));
                    }
                } else {
                    player.sendOverlayMessage(Component.literal("Too big"));
                }
            } else {
                player.sendOverlayMessage(Component.literal("Points not available"));
            }
        }
    }

    // 保存结构
    public static void structureSave(StructureTemplate structure){
            String structureId = MScreenshot.getTimeId(structure);
            File root = Minecraft.getInstance().gameDirectory;
            File modDir = new File(root, VisualShare.MOD_ID);
            modDir.mkdir();
            File strDir = new File(modDir, "structure");
            modDir.mkdir();
            File clientDir = new File(strDir, "client");
            modDir.mkdir();
            File newFile = new File(clientDir, structureId + ".nbt");

            try {
                StructureTemplateManager.save(newFile.toPath(), structure, false);
            } catch (IOException e) {
                LOGGER.error("Client save structure error: {}", e.getMessage());
            }
    }

    // 分享结构
    public static void structureShare(String id){
        StructureTemplate structure = structureList.getOrDefault(id, null);
        if (structure != null){
            ClientPacketDistributor.sendToServer(new StructureViewPayload.StructureUploadRequestData(id, 0));
        }else{
            Player player = Minecraft.getInstance().player;
            if (player != null){
                player.sendOverlayMessage(Component.literal("The structure don't exist: " + id));
            }
            LOGGER.warn("The structure don't exist: {}", id);
        }
    }

    @EventBusSubscriber
    public static class structureViewHolder{
        public static boolean RCBCooldown;

        @SubscribeEvent
        public static void onKey(InputEvent.Key event){
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null && minecraft.screen == null) { // 用level来判断玩家是否已经在某个服务器中
                if (event.getAction() == InputConstants.PRESS){
                    if (event.getKey() == ModKeys.STRUCTURE_VIEW_SELECTION.getKey().getValue()) {
                        structureSelection();
                    } else if (event.getKey() == ModKeys.STRUCTURE_VIEW_PREVIEW.getKey().getValue()) {
                        structureView();
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onTickForCooldown(ClientTickEvent.Pre event){
            RCBCooldown = false;
        }

        @SubscribeEvent
        public static void onClickBlock(PlayerInteractEvent.RightClickBlock event){
            Player player = Minecraft.getInstance().player;
            if (!RCBCooldown && player != null) {
                if (selectionMode) {
                    if (point1 == null) {
                        point1 = event.getHitVec().getBlockPos();
                        player.sendOverlayMessage(Component.literal("Point1: "+point1));
                    } else {
                        point2 = event.getHitVec().getBlockPos();
                        player.sendOverlayMessage(Component.literal("Point2: "+point2));
                        selectionMode = false;
                    }
                    RCBCooldown = true;
                    event.setCanceled(true);
                }
            }
        }
    }
}
