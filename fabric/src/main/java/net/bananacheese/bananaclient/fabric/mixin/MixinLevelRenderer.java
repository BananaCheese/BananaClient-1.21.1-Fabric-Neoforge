package net.bananacheese.bananaclient.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.spongepowered.asm.mixin.injection.callback.LocalCapture.CAPTURE_FAILHARD;

/**
 * Fabric-specific fix for Freecam's "Show Player" setting.
 *
 * Ported from an earlier revision of Xolt's Freecam mod (before it moved to
 * the newer extractVisibleEntities/LevelRenderState rendering pipeline),
 * which targets the same LevelRenderer#renderLevel shape 1.21.1 still uses.
 *
 * Unlike NeoForge, Fabric's unmodified-vanilla render pipeline excludes the
 * local player from the level's entity render pass entirely (not just via
 * EntityRenderDispatcher#shouldRender) to avoid the classic "see your own
 * head in first person" bug. Forcing shouldRender=true isn't enough here —
 * the player never reaches that check in the first place — so this manually
 * renders it back in.
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    protected abstract void renderEntity(Entity entity, double camX, double camY, double camZ, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource);

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 0), locals = CAPTURE_FAILHARD)
    private void bananaclient$onRenderLevel(DeltaTracker deltaTracker,
                                            boolean renderBlockOutline,
                                            Camera camera,
                                            GameRenderer gameRenderer,
                                            LightTexture lightTexture,
                                            Matrix4f matrix4f,
                                            Matrix4f matrix4f2,
                                            CallbackInfo ci,
                                            // Locals below are captured only so we can reach poseStack/bufferSource.
                                            TickRateManager tickRateManager,
                                            float partialTick,
                                            ProfilerFiller profilerFiller,
                                            Vec3 cameraPosition,
                                            double x,
                                            double y,
                                            double z,
                                            boolean frustumNotNull,
                                            Frustum frustum,
                                            float renderDistance,
                                            boolean fog,
                                            Matrix4fStack modelViewStack,
                                            boolean bl4,
                                            PoseStack poseStack,
                                            MultiBufferSource.BufferSource bufferSource) {
        var player = Minecraft.getInstance().player;
        if (player != null && Freecam.isActive() && Freecam.shouldRenderPlayer()) {
            renderEntity(player, x, y, z, partialTick, poseStack, renderBuffers.bufferSource());
        }
    }
}