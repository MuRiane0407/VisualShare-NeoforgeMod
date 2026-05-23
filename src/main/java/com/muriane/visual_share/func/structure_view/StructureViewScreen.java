package com.muriane.visual_share.func.structure_view;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

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

        //tesselator();
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
        System.out.print(meshData+"\n");
        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Custom Gui", 32, meshData.vertexBuffer());

        RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Gui disc", colorTexture, OptionalInt.empty(), depthTexture, OptionalDouble.empty());
        renderPass.setPipeline(RenderPipelines.SOLID_BLOCK);
        renderPass.setVertexBuffer(0, buffer);
        renderPass.draw(0, 10);

        tesselator.clear();
    }

    private void renderBlocks(GuiGraphicsExtractor graphics) {
        Level level = Minecraft.getInstance().level;
        LevelRenderer levelRenderer = Minecraft.getInstance().levelRenderer;

        Matrix4f modelViewMatrix = new Matrix4f();
//        viewCamera.getViewRotationMatrix(modelViewMatrix);
        Minecraft.getInstance().gameRenderer.getMainCamera().getViewRotationMatrix(modelViewMatrix);
        ChunkSectionsToRender chunkSectionsToRender = levelRenderer.prepareChunkRenders(modelViewMatrix);

        TextureFilteringMethod textureFiltering = Minecraft.getInstance().options.textureFiltering().get();
        int maxAnisotropyValue = Minecraft.getInstance().options.maxAnisotropyValue();
        int maxAnisotropy = textureFiltering == TextureFilteringMethod.ANISOTROPIC
                ? maxAnisotropyValue
                : 1;
        GpuSampler chunkLayerSampler = RenderSystem.getDevice()
                .createSampler(
                        AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, maxAnisotropy, OptionalDouble.empty()
                );

        RenderTarget renderTarget = renderGroup(chunkSectionsToRender, ChunkSectionLayerGroup.OPAQUE, chunkLayerSampler);

        renderTexture(renderTarget,
                image -> {
                    DynamicTexture texture = new DynamicTexture(image::toString, image);
                    graphics.blit(texture.getTextureView(), texture.getSampler(), 0, 0, 256, 256, 0, 1, 0, 1);
                });
    }

    public void renderTexture(RenderTarget target, Consumer<NativeImage> callback){
        int originWidth = target.width;
        int originHeight = target.height;
        GpuTexture sourceTexture = target.getColorTexture();
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
                                NativeImage image = new NativeImage(originWidth, originHeight, false);

                                for (int yi = 0; yi < originHeight; yi++) {
                                    for (int xi = 0; xi < originWidth; xi++) {
                                        int argb = read.data().getInt((xi + (yi) * originWidth) * sourceTexture.getFormat().pixelSize());
                                        image.setPixelABGR(xi, originHeight - yi - 1, argb | 0xFF000000);
                                    }
                                }

                                callback.accept(image);
                            }

                            buffer.close();
                        },
                        0
                );
    }

    public RenderTarget renderGroup(ChunkSectionsToRender chunkSectionsToRender, ChunkSectionLayerGroup group, GpuSampler sampler) {
        RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer defaultIndexBuffer = chunkSectionsToRender.maxIndicesRequired() == 0 ? null : autoIndices.getBuffer(chunkSectionsToRender.maxIndicesRequired());
        VertexFormat.IndexType defaultIndexType = chunkSectionsToRender.maxIndicesRequired() == 0 ? null : autoIndices.type();
        ChunkSectionLayer[] layers = group.layers();
        Minecraft minecraft = Minecraft.getInstance();
        boolean wireframe = SharedConstants.DEBUG_HOTKEYS && minecraft.wireframe;

        RenderTarget renderTarget = new TextureTarget("c", 512, 512, true);
//        RenderTarget renderTarget = minecraft.getMainRenderTarget();

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Section layers for " + group.label(), renderTarget.getColorTextureView(), OptionalInt.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("Sampler0", chunkSectionsToRender.textureView(), sampler);
            renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

            for(ChunkSectionLayer layer : layers) {
                renderPass.setPipeline(wireframe ? RenderPipelines.WIREFRAME : layer.pipeline());
                Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>> drawGroup = (Int2ObjectOpenHashMap)chunkSectionsToRender.drawGroupsPerLayer().get(layer);
                ObjectIterator var16 = drawGroup.values().iterator();

                while(var16.hasNext()) {
                    List<RenderPass.Draw<GpuBufferSlice[]>> draws = (List)var16.next();
                    if (!draws.isEmpty()) {
                        if (layer == ChunkSectionLayer.TRANSLUCENT) {
                            draws = draws.reversed();
                        }

                        renderPass.drawMultipleIndexed(draws, defaultIndexBuffer, defaultIndexType, List.of("ChunkSection"), chunkSectionsToRender.chunkSectionInfos());
                    }
                }
            }
        }

        return renderTarget;
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
}
