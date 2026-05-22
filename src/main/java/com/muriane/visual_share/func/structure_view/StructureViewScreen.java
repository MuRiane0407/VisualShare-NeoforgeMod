package com.muriane.visual_share.func.structure_view;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.model.data.ModelData;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class StructureViewScreen extends Screen {
    protected final StructureTemplate structure;
    protected final Camera viewCamera;

    protected StructureViewScreen(StructureTemplate structure) {
        super(Component.empty());
        this.structure = structure;
        this.viewCamera = new Camera();
    }

    @Override
    protected void init() {
        Entity entity = Minecraft.getInstance().getCameraEntity();

        if (entity != null) {
            ArmorStand camera = new ArmorStand(entity.level(), 0, 120, 0);
            camera.setInvisible(true);
            camera.setXRot(90);
            camera.setYRot(90);

            viewCamera.setEntity(camera);
            viewCamera.setLevel((ClientLevel) entity.level());
        }else{
            System.out.print("null\n");
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        renderSingleBlock(graphics);
    }

    private void renderSingleBlock(GuiGraphicsExtractor graphics) {

    }

    public void renderTarget(GuiGraphicsExtractor graphics){
        RenderTarget target = getMinecraft().getMainRenderTarget();
        int originWidth = target.width;
        int originHeight = target.height;
        GpuTexture sourceTexture = target.getColorTexture();
        NativeImage image = new NativeImage(originWidth, originHeight, false);

        if (sourceTexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        } else {
            GpuBuffer buffer = RenderSystem.getDevice()
                    .createBuffer(() -> "View buffer", 9, (long)originWidth * originHeight * sourceTexture.getFormat().pixelSize());
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .copyTextureToBuffer(
                            sourceTexture,
                            buffer,
                            0L,
                            () -> {
                                try (GpuBuffer.MappedView read = commandEncoder.mapBuffer(buffer, true, false)) {

                                    for (int yi = 0; yi < originHeight; yi++) {
                                        for (int xi = 0; xi < originWidth; xi++) {
                                            int argb = read.data().getInt((xi + (yi) * originWidth) * sourceTexture.getFormat().pixelSize());
                                            image.setPixelABGR(xi, height - yi - 1, argb | 0xFF000000);
                                        }
                                    }
                                }

                                buffer.close();
                            },
                            0
                    );
        }

        DynamicTexture texture = new DynamicTexture(image::toString, image);
        graphics.blit(texture.getTextureView(), texture.getSampler(), 0, 0, 512, 512, 0, 1, 0, 1);
    }

    public void tesselator(){
        Tesselator tesselator = new Tesselator();
        // 1. 获取方块模型
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(Blocks.STONE.defaultBlockState());

        // 2. 开始构建顶点
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

        // 3. 遍历模型的每个面（六个面）
        builder.addVertex(0.5f, 0.5f, 0.5f, 0xFFFFFF, 0, 0, 1, 1, 1, 1, 1);

        // 4. 提交到 GPU
        GpuTextureView colorTexture = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
        GpuTextureView depthTexture = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();

        MeshData meshData = builder.buildOrThrow();
        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Custom Gui", 32, meshData.vertexBuffer());

        RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Gui disc", colorTexture, OptionalInt.empty(), depthTexture, OptionalDouble.empty());
        renderPass.setPipeline(RenderPipelines.GUI_TEXTURED);
        renderPass.setVertexBuffer(0, buffer);
        renderPass.draw(0, 10);

        tesselator.clear();
    }
}
