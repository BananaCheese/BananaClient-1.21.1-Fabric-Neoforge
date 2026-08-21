package net.bananacheese.bananaclient.modules.render.freecam;

import com.mojang.authlib.GameProfile;
import net.bananacheese.bananaclient.modules.render.Freecam;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec2;

import java.util.UUID;

import static net.minecraft.client.Minecraft.getInstance;

public class FreeCamera extends AbstractClientPlayer {

    public Input input;

    // Arm-swing bob values, read by MixinItemInHandRenderer so hand/arm
    // animation follows the freecam instead of the real (frozen) player.
    public float yBob;
    public float xBob;
    public float yBobO;
    public float xBobO;

    public FreeCamera(int id) {
        super((ClientLevel) getInstance().level, new GameProfile(UUID.randomUUID(), "FreeCamera"));

        setId(id);
        setPose(Pose.SWIMMING); // Swimming pose has no movement-speed penalties and a sensible hitbox.
        getAbilities().flying = true;
        input = new KeyboardInput(getInstance().options);
    }

    @Override
    public void tick() {
        input.tick(false, 0.3F);
        doMotion();

        // Remember where we started so No Clip can compute a pure additive
        // delta below, independent of whatever travel()/move() below does.
        double startX = getX();
        double startY = getY();
        double startZ = getZ();

        super.tick();

        if (Freecam.isNoClip()) {
            // Don't trust Entity#noPhysics to disable block collision here —
            // instead bypass collision-aware movement entirely: compute this
            // tick's movement by hand (using vanilla's own rotation-transform
            // formula, so direction/feel matches normal flight exactly) and
            // just teleport there. This is a guaranteed no-clip regardless of
            // what noPhysics does or doesn't skip on a given version.
            double[] delta = computeNoClipDelta();
            this.setPos(startX + delta[0], startY + delta[1], startZ + delta[2]);
            this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
    }

    // Mirrors vanilla LivingEntity#getInputVector: rotates the raw xxa/zza
    // input by yaw to get a world-space horizontal direction, so movement
    // feel matches normal (collision-aware) flight exactly.
    private double[] computeNoClipDelta() {
        double yawRad = Math.toRadians(this.getYRot());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double relX = this.xxa;
        double relZ = this.zza;
        double lenSq = relX * relX + relZ * relZ;
        if (lenSq > 1.0) {
            double len = Math.sqrt(lenSq);
            relX /= len;
            relZ /= len;
        }

        double horizontalSpeed = Freecam.getSpeed() / 2.0;
        if (this.isSprinting()) horizontalSpeed *= 2.0;

        double dx = (relX * cos - relZ * sin) * horizontalSpeed;
        double dz = (relZ * cos + relX * sin) * horizontalSpeed;

        double dy = 0.0;
        boolean jump = input.jumping;
        boolean sneak = input.shiftKeyDown;
        if (jump ^ sneak) {
            dy = (jump ? 1.0 : -1.0) * getAbilities().getFlyingSpeed() * 4.0;
        }

        return new double[]{dx, dy, dz};
    }

    @Override
    public void copyPosition(Entity entity) {
        moveTo(entity.getX(), entity.getEyeY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        xBob = getXRot();
        yBob = getYRot();
        xBobO = xBob; // Prevents the camera from snapping/rotating the instant freecam is enabled.
        yBobO = yBob;
    }

    private ClientLevel getClientLevel() {
        return (ClientLevel) level();
    }

    public void spawn() {
        getClientLevel().addEntity(this);
    }

    public void despawn() {
        getClientLevel().removeEntity(getId(), RemovalReason.DISCARDED);
    }

    // Prevents a fall-damage/landing sound from playing when the camera
    // touches the ground with No Clip disabled.
    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
    }

    // The real player is frozen while freecam is active, so borrow its
    // swing animation/using-item state for the hand-render mixin.
    @Override
    public float getAttackAnim(float tickDelta) {
        return getInstance().player != null ? getInstance().player.getAttackAnim(tickDelta) : 0f;
    }

    @Override
    public int getUseItemRemainingTicks() {
        return getInstance().player != null ? getInstance().player.getUseItemRemainingTicks() : 0;
    }

    @Override
    public boolean isUsingItem() {
        return getInstance().player != null && getInstance().player.isUsingItem();
    }

    // Prevents movement slow-down from ladders/vines.
    @Override
    public boolean onClimbable() {
        return false;
    }

    // Prevents movement slow-down/fog from water.
    @Override
    public boolean isInWater() {
        return false;
    }

    // Lets the real player's potion effects (e.g. Night Vision) apply visually to the freecam.
    @Override
    public MobEffectInstance getEffect(Holder<MobEffect> effect) {
        return getInstance().player != null ? getInstance().player.getEffect(effect) : null;
    }

    // No Clip: entities never push a non-colliding camera around.
    @Override
    public PushReaction getPistonPushReaction() {
        return Freecam.isNoClip() ? PushReaction.IGNORE : PushReaction.NORMAL;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    @Override
    public void setPose(Pose pose) {
        super.setPose(Pose.SWIMMING);
    }

    @Override
    protected boolean updateIsUnderwater() {
        this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
        return this.wasUnderwater;
    }

    // Prevents splash sounds from playing as the camera passes through water.
    @Override
    protected void doWaterSplashEffect() {
    }

    private void doMotion() {
        getAbilities().setFlyingSpeed(Freecam.getSpeed() / 10f);
        getAbilities().flying = true;
        this.noPhysics = Freecam.isNoClip();

        boolean jump = input.jumping;
        boolean sneak = input.shiftKeyDown;
        if (jump ^ sneak) {
            double direction = jump ? 1.0 : -1.0;
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, direction * getAbilities().getFlyingSpeed() * 3.0, 0.0));
        }

        setOnGround(false);
    }

    @Override
    public float getViewXRot(float partialTick) {
        return getXRot();
    }

    @Override
    public float getViewYRot(float partialTick) {
        return getYRot();
    }

    @Override
    public boolean isEffectiveAi() {
        return true;
    }

    @Override
    protected void serverAiStep() {
        Vec2 moveVector = new Vec2(input.leftImpulse, input.forwardImpulse);
        applyInputHelper(moveVector, input.jumping);
    }

    private void applyInputHelper(Vec2 moveVector, boolean jumping) {
        this.xxa = moveVector.x;
        this.zza = moveVector.y;
        this.jumping = jumping;
        this.setSprinting(getInstance().options.keySprint.isDown() && moveVector.y > 0.0F);
        this.yBobO = this.yBob;
        this.xBobO = this.xBob;
        this.xBob = this.xBob + (this.getXRot() - this.xBob) * 0.5F;
        this.yBob = this.yBob + (this.getYRot() - this.yBob) * 0.5F;
    }
}