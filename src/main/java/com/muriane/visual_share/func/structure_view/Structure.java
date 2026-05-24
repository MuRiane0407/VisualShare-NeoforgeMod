package com.muriane.visual_share.func.structure_view;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.muriane.visual_share.Config;
import com.muriane.visual_share.VisualShare;
import com.muriane.visual_share.func.screenshot.ScreenshotPayload;
import com.muriane.visual_share.key.ModKeys;
import com.muriane.visual_share.method.MMethod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

@EventBusSubscriber
public class Structure {
    public static Logger LOGGER = LogUtils.getLogger();
    public static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(VisualShare.MOD_ID, "structure");
    public static boolean selectionMode;
    public static Vec3i point1;
    public static Vec3i point2;
    public static Map<String, Pair<Boolean, StructureTemplate>> structureList = new HashMap<>();
    public static Pair<String, Boolean> loadStructure = new Pair<>(null, false);
    public static int RCBCooldown = 0;

    @SubscribeEvent
    public static void onKey(InputEvent.Key event){
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.screen == null) { // 用level来判断玩家是否已经在某个服务器中
            if (event.getAction() == InputConstants.PRESS){
                if (event.getKey() == ModKeys.STRUCTURE_VIEW_SELECTION.getKey().getValue()) {
                    structureSelection();
                } else if (event.getKey() == ModKeys.STRUCTURE_VIEW_SHARE.getKey().getValue()) {
                    structureView();
                }
            }
        }
    }

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

    // 进入预览屏幕（但是暂时不知道怎么在屏幕上渲染结构，直接跳过到发送）
    public static void structureView(){
        Player player = Minecraft.getInstance().player;
        Level level = Minecraft.getInstance().level;
        if (player != null) {
            if (point1 != null && point2 != null) {
                Vec3i size = new Vec3i(Math.abs(point1.getX()-point2.getX())+1, Math.abs(point1.getY()-point2.getY())+1, Math.abs(point1.getZ()-point2.getZ())+1);

                if (size.getX() * size.getY() * size.getZ() <= 32768) {
                    BlockPos pos = new BlockPos(Math.min(point1.getX(), point2.getX()), Math.min(point1.getY(), point2.getY()), Math.min(point1.getZ(), point2.getZ()));
                    boolean includeEntities = true;
                    List<Block> ignoreBlocks = new ArrayList<>();

                    if (level != null) {
                        StructureTemplate structure = new StructureTemplate();
                        structure.fillFromWorld(level, pos, size, includeEntities, ignoreBlocks);

                        structureShare(structure);
                    }
                } else {
                    player.sendOverlayMessage(Component.literal("Too big"));
                }
            } else {
                player.sendOverlayMessage(Component.literal("Points not available"));
            }
        }
    }

//    // 保存结构
//    public static void structureSave(StructureTemplate structure){
//            String structureId = MMethod.getTimeId(structure);
//            File root = Minecraft.getInstance().gameDirectory;
//            File modDir = new File(root, VisualShare.MOD_ID);
//            modDir.mkdir();
//            File strDir = new File(modDir, "structure");
//            modDir.mkdir();
//            File clientDir = new File(strDir, "client");
//            modDir.mkdir();
//            File newFile = new File(clientDir, structureId + ".nbt");
//
//            try {
//                StructureTemplateManager.save(newFile.toPath(), structure, false);
//            } catch (IOException e) {
//                LOGGER.error("Client save structure error: {}", e.getMessage());
//            }
//    }

    // 分享结构
    public static void structureShare(StructureTemplate structure) {
        String id = MMethod.getTimeId(structure);

        CompoundTag tag = new CompoundTag();
        structure.save(tag);
        StructurePayload.StructureUploadRequestData.structureList.put(id, tag);
        ClientPacketDistributor.sendToServer(new StructurePayload.StructureUploadRequestData(id, 0));
    }

    public static void structureLoad(StructureTemplate structure){
        System.out.print(structure+"\n");

        // 接下来要做的是，投影建筑，懒得做了
    }

    /// 转换带标识符的信息为结构信息
    public static MutableComponent tranInfoToStructure(Component chat){
        String prefix = Config.SERVER.STRUCTURE_SHARE_PREFIX.get();
        String subfix = Config.SERVER.STRUCTURE_SHARE_SUBFIX.get();

        MutableComponent newChat = MutableComponent.create(Component.empty().getContents());
        for (Component component : chat.toFlatList()){
            Component cur = component;
            String curStr = cur.getString();

            boolean end = false;
            int time = curStr.length(); // 防止死循环
            while (!cur.equals(Component.empty()) && !end && time > 0){
                int is = curStr.indexOf(prefix);
                int ie = curStr.indexOf(subfix);
                if (is != -1 && ie != -1) {
                    newChat.append(Component.literal(curStr.substring(0, is)).withStyle(cur.getStyle()));

                    String find = curStr.substring(is + prefix.length(), ie);
                    if (findServerStructureData(find) != null){
                        Player player = Minecraft.getInstance().player;
                        if (player != null){
                            Component text = Component.literal(find);
                            CompoundTag tag = new CompoundTag();
                            tag.putString("id", find);

                            newChat.append(Component.translatable("chat.visual_share.structure.structure").withStyle(style -> style
                                    .withColor(ChatFormatting.DARK_PURPLE)
                                    .withHoverEvent(new HoverEvent.ShowText(text))
                                    .withClickEvent(new ClickEvent.Custom(
                                            STRUCTURE,
                                            Optional.of(tag)
                                    ))
                            ));
                        }
                    }else{
                        newChat.append(Component.literal(prefix + find + subfix).withStyle(cur.getStyle()));
                    }

                    curStr = curStr.substring(ie + subfix.length());
                    cur = Component.literal(curStr).withStyle(cur.getStyle());
                } else {
                    newChat.append(cur);
                    end = true;
                }
                time--;
            }
        }

        return newChat;
    }

    public static File findServerStructureData(String name){
        // 在本地文件中查找
        File clientScreenshotData = new File(Minecraft.getInstance().gameDirectory+"/"+VisualShare.MOD_ID+"/structure/server");
        if (clientScreenshotData.exists()){
            File[] files = clientScreenshotData.listFiles();
            if (files != null) {
                for (File data : files){
                    if (data.getName().substring(0, data.getName().lastIndexOf(".")).equals(name)){
                        return data;
                    }
                }
            }
        }

        return null;
    }

    public static void onClickStructureInfo(CompoundTag compoundTag) {
        String id = compoundTag.getStringOr("id", "");
        if (!id.isEmpty()){
            loadStructure = new Pair<>(id, true);
        }
    }

    @SubscribeEvent
    public static void tryLoadStructure(ClientTickEvent.Pre event){
        if (loadStructure.getSecond()){
            String id = loadStructure.getFirst();
            Pair<Boolean, StructureTemplate> structurePair = structureList.getOrDefault(id, null);

            if (structurePair == null){
                tryGetStructure(id);
            }else{
                if (structurePair.getFirst()){
                    loadStructure = new Pair<>(null, false);
                    StructureTemplate structure = structurePair.getSecond();
                    if (structure != null){
                        structureLoad(structure);
                    }
                }
            }
        }
    }

    public static void tryGetStructure(String id){
        structureList.put(id, new Pair<>(false, null));

        File structureData = findClientStructureData(id);
        if (structureData != null){
            try {
                CompoundTag tag = NbtIo.read(structureData.toPath());
                if (tag != null) {
                    StructureTemplate template = new StructureTemplate();
                    template.load(BuiltInRegistries.BLOCK, tag);
                    structureList.put(id, new Pair<>(true, template));
                }
            }catch (IOException e){
                LOGGER.error("Error read client file: {}", e.getMessage());
            }
        }else{
            ClientPacketDistributor.sendToServer(new StructurePayload.StructureLoadRequestData(id));
        }
    }

    public static File findClientStructureData(String name){
        // 在本地文件中查找
        File clientScreenshotData = new File(Minecraft.getInstance().gameDirectory+"/"+VisualShare.MOD_ID+"/structure/client");
        if (clientScreenshotData.exists()){
            File[] files = clientScreenshotData.listFiles();
            if (files != null) {
                for (File data : files){
                    if (data.getName().substring(0, data.getName().lastIndexOf(".")).equals(name)){
                        return data;
                    }
                }
            }
        }

        return null;
    }

    @SubscribeEvent
    public static void onTickForCooldown(ClientTickEvent.Pre event){
        if (RCBCooldown > 0){
            RCBCooldown--;
        }
    }

    @SubscribeEvent
    public static void onClickBlock(PlayerInteractEvent.RightClickBlock event){
        Player player = Minecraft.getInstance().player;
        if (RCBCooldown <= 0 && player != null) {
            if (selectionMode) {
                if (point1 == null) {
                    point1 = event.getHitVec().getBlockPos();
                    player.sendOverlayMessage(Component.literal("Point1: "+point1));
                } else {
                    point2 = event.getHitVec().getBlockPos();
                    player.sendOverlayMessage(Component.literal("Point2: "+point2));
                    selectionMode = false;
                }
                RCBCooldown = 2;
                event.setCanceled(true);
            }
        }
    }
}
