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
        super.tick();
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
        // "Creative"-style flight: vertical speed from the flying-speed ability
        // (adjusted every tick from the module's Speed setting), horizontal
        // speed from LivingEntityMixin#getFrictionInfluencedSpeed.
        getAbilities().setFlyingSpeed(Freecam.getSpeed() / 10f);
        getAbilities().flying = true;
        this.noPhysics = Freecam.isNoClip();

        // Vertical movement (ascend/descend) isn't part of the normal
        // xxa/zza travel() impulse — vanilla creative flight applies it
        // directly from the jump/sneak keys, so we have to do the same here.
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

    // Ensures LivingEntity#aiStep() actually processes movement for this
    // entity even though it's not a "real" LocalPlayer.
    @Override
    public boolean isEffectiveAi() {
        return true;
    }

    // Translates our polled `input` into the xxa/zza/jumping impulses that
    // LivingEntity#travel() consumes each tick. LocalPlayer normally does
    // this itself; FreeCamera has to do it manually since it isn't one.
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