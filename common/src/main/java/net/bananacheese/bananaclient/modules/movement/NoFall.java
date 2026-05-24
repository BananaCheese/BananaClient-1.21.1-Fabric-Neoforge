package net.bananacheese.bananaclient.modules.movement;

import net.bananacheese.bananaclient.modules.CycleSetting;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.bananacheese.bananaclient.modules.RegistryListSetting;
import net.bananacheese.bananaclient.utils.PacketUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class NoFall extends Module {

    private static NoFall INSTANCE;

    // ── Settings ──────────────────────────────────────────────────────────

    private final CycleSetting mode = addCycleSetting(
            new CycleSetting("Mode",
                    "How fall damage is cancelled",
                    0, // default = Packet
                    "Packet", "Both", "AutoPlace")
    );

    private final ModuleSetting<Boolean> hotbarOnly = addSetting(
            new ModuleSetting<>("Hotbar Only",
                    "Only use items from the hotbar", true)
    );

    private final ModuleSetting<Boolean> cancelBounce = addSetting(
            new ModuleSetting<>("Cancel Bounce",
                    "Cancel slime/cobweb bounce on landing", true)
    );

    // Items that can be placed while falling
    private final RegistryListSetting allowedItems = addRegistrySettings(
            RegistryListSetting.items("Allowed Items",
                    "Items that can be auto-placed",
                    ResourceLocation.withDefaultNamespace("water_bucket"),
                    ResourceLocation.withDefaultNamespace("powder_snow_bucket"),
                    ResourceLocation.withDefaultNamespace("hay_block"),
                    ResourceLocation.withDefaultNamespace("cobweb"),
                    ResourceLocation.withDefaultNamespace("slime_block"))
    );

    // Items whose placed block should be auto-collected after landing.
    // Defaults: water_bucket and powder_snow_bucket only.
    // Hay/cobweb/slime are intentionally excluded — they stay placed.
    private final RegistryListSetting pickupItems = addRegistrySettings(
            RegistryListSetting.items("Auto Pickup Items",
                    "Items to auto-collect after placing",
                    ResourceLocation.withDefaultNamespace("water_bucket"),
                    ResourceLocation.withDefaultNamespace("powder_snow_bucket"))
    );

    // ── Constants ─────────────────────────────────────────────────────────

    private static final float  FALL_THRESHOLD    = 3.5f;
    private static final double PLACE_WHEN_WITHIN = 3.99;
    private static final int    GROUND_SCAN_DEPTH = 64;

    // ── State ─────────────────────────────────────────────────────────────

    private boolean      hasPlaced    = false;
    private int          prevSlot     = -1;
    private int          pickupDelay  = 0;
    private BlockPos     placedPos    = null;
    private ResourceLocation placedItemId = null; // track which item we placed

    public NoFall() {
        super("NoFall", "Cancels fall damage", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    @Override
    public void onDisable() { resetState(); }

    private void resetState() {
        hasPlaced   = false;
        prevSlot    = -1;
        pickupDelay = 0;
        placedPos   = null;
        placedItemId = null;
    }

    // ── Static accessors ──────────────────────────────────────────────────

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static boolean shouldSendPacket() {
        if (!isActive()) return false;
        return INSTANCE.mode.is("Packet") || INSTANCE.mode.is("Both");
    }

    public static void onTick(LocalPlayer player) {
        if (!isActive()) return;
        if (INSTANCE.mode.is("AutoPlace"))
            INSTANCE.tickAutoPlace(player);
        if (INSTANCE.cancelBounce.getValue())
            INSTANCE.tickCancelBounce(player);
    }

    // ── Bounce cancel ─────────────────────────────────────────────────────

    /**
     * If the player just landed on a slime block or cobweb and
     * Cancel Bounce is enabled, zero out their vertical velocity
     * so they stop instead of bouncing back up.
     *
     * We only apply this when:
     * - The player is on the ground this tick
     * - Their Y velocity is upward (the bounce has already been applied
     *   by Minecraft but we cancel it before it moves the player)
     * - The block directly below them is slime or honey
     */
    private void tickCancelBounce(LocalPlayer player) {
        if (!player.onGround()) return;

        // Check if Y velocity is positive — the bounce upward has been set
        double vy = player.getDeltaMovement().y;
        if (vy <= 0) return;

        // Check block below player feet
        BlockPos below = BlockPos.containing(
                player.getX(), player.getY() - 0.01, player.getZ());
        var state = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getBlockState(below)
                : null;
        if (state == null) return;

        boolean isBouncyBlock =
                state.is(Blocks.SLIME_BLOCK) ||
                        state.is(Blocks.HONEY_BLOCK);  // honey also has mild bounce

        if (isBouncyBlock) {
            // Keep horizontal movement, zero vertical
            player.setDeltaMovement(
                    player.getDeltaMovement().x,
                    0,
                    player.getDeltaMovement().z
            );
        }
    }

    // ── Per-tick auto-place logic ─────────────────────────────────────────

    private void tickAutoPlace(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameMode == null) return;

        // ── Just landed ───────────────────────────────────────────────────
        if (player.onGround()) {
            if (hasPlaced) {
                if (prevSlot >= 0 && prevSlot < 9)
                    player.getInventory().selected = prevSlot;

                // Only schedule pickup if the placed item is in the pickup list
                boolean shouldPickup = placedItemId != null
                        && pickupItems.contains(placedItemId)
                        && placedPos != null;

                if (shouldPickup) {
                    pickupDelay = 5;
                } else {
                    placedPos    = null;
                    placedItemId = null;
                }
                hasPlaced = false;
                prevSlot  = -1;
            }

            if (pickupDelay > 0) {
                pickupDelay--;
                if (pickupDelay == 0 && placedPos != null) {
                    tryPickup(player, mc);
                    placedPos    = null;
                    placedItemId = null;
                }
            }
            return;
        }

        // ── Falling ───────────────────────────────────────────────────────
        if (player.fallDistance < FALL_THRESHOLD) return;
        if (hasPlaced) return;

        BlockPos surface = findSolidBelow(player, mc, GROUND_SCAN_DEPTH);
        if (surface == null) return;

        double heightAboveSurface = player.getY() - (surface.getY() + 1);
        if (heightAboveSurface > PLACE_WHEN_WITHIN) return;

        int slot = findAllowedSlot(player);
        if (slot < 0) return;

        ItemStack stack = player.getInventory().getItem(slot);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        int current = player.getInventory().selected;
        if (slot != current) {
            prevSlot = current;
            player.getInventory().selected = slot;
        } else {
            prevSlot = -1;
        }

        placedPos    = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        placedItemId = itemId;

        boolean placed = placeItem(player, mc, itemId, surface);

        if (placed) {
            hasPlaced = true;
        } else {
            if (prevSlot >= 0) {
                player.getInventory().selected = prevSlot;
                prevSlot = -1;
            }
            placedPos    = null;
            placedItemId = null;
        }
    }

    // ── Placement ─────────────────────────────────────────────────────────

    private boolean placeItem(LocalPlayer player, Minecraft mc,
                              ResourceLocation itemId, BlockPos surface) {
        if (isWaterBucket(itemId)) {
            return placeWithLookDown(player, mc);
        } else {
            return placeBlockItem(player, mc, surface);
        }
    }

    private boolean placeWithLookDown(LocalPlayer player, Minecraft mc) {
        float savedPitch = player.getXRot();
        float savedYaw   = player.getYRot();

        player.setXRot(90.0f);
        if (mc.getCameraEntity() == player) mc.getCameraEntity().setXRot(90.0f);

        InteractionResult result = mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        boolean success = result.consumesAction();

        player.setXRot(savedPitch);
        player.setYRot(savedYaw);
        if (mc.getCameraEntity() == player) {
            mc.getCameraEntity().setXRot(savedPitch);
            mc.getCameraEntity().setYRot(savedYaw);
        }

        return success;
    }

    private boolean placeBlockItem(LocalPlayer player, Minecraft mc, BlockPos surface) {
        try {
            Vec3 hitVec = new Vec3(
                    surface.getX() + 0.5,
                    surface.getY() + 1.0,
                    surface.getZ() + 0.5
            );
            BlockHitResult hit = new BlockHitResult(
                    hitVec, Direction.UP, surface, false);

            PacketUtils.sendPacket(new ServerboundUseItemOnPacket(
                    InteractionHand.MAIN_HAND, hit, 0));

            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Pickup ────────────────────────────────────────────────────────────

    private void tryPickup(LocalPlayer player, Minecraft mc) {
        if (mc.level == null || placedPos == null) return;

        // Scan nearby area for placed fluid
        boolean fluidFound = false;

        int horizontalRadius = 2;
        int verticalRadius = 3;

        for (int dx = -horizontalRadius; dx <= horizontalRadius && !fluidFound; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius && !fluidFound; dz++) {
                for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {

                    var checkPos = placedPos.offset(dx, dy, dz);
                    var state = mc.level.getBlockState(checkPos);

                    if (state.is(Blocks.WATER)
                            || state.is(Blocks.POWDER_SNOW)
                            || state.is(Blocks.LAVA)) {

                        fluidFound = true;

                        break;
                    }
                }
            }
        }

        if (!fluidFound) {
            return;
        }

        int bucketSlot = findEmptyBucket(player);
        if (bucketSlot < 0) {
            return;
        }

        int savedSlot = player.getInventory().selected;

        if (bucketSlot >= 9) {
            moveToHotbar(player, mc, bucketSlot, 8);
            player.getInventory().selected = 8;
        } else {
            player.getInventory().selected = bucketSlot;
        }

        placeWithLookDown(player, mc);

        player.getInventory().selected = savedSlot;
    }
    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean isWaterBucket(ResourceLocation id) {
        if (id == null) return false;
        return id.equals(ResourceLocation.withDefaultNamespace("water_bucket"))
                || id.getPath().endsWith("_water_bucket");
    }

    private BlockPos findSolidBelow(LocalPlayer player, Minecraft mc, int depth) {
        BlockPos start = BlockPos.containing(
                player.getX(), player.getY() - 1, player.getZ());
        for (int dy = 0; dy < depth; dy++) {
            BlockPos check = start.below(dy);
            if (mc.level.getBlockState(check).isSolid()) return check;
        }
        return null;
    }

    private int findAllowedSlot(LocalPlayer player) {
        Inventory inv = player.getInventory();
        int limit = hotbarOnly.getValue() ? 9 : inv.getContainerSize();
        for (int i = 0; i < limit; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && allowedItems.contains(id)) {
                if (i >= 9) {
                    moveToHotbar(player, Minecraft.getInstance(), i, 8);
                    return 8;
                }
                return i;
            }
        }
        return -1;
    }

    private int findEmptyBucket(LocalPlayer player) {
        Inventory inv = player.getInventory();
        int limit = hotbarOnly.getValue() ? 9 : inv.getContainerSize();
        for (int i = 0; i < limit; i++) {
            if (inv.getItem(i).is(Items.BUCKET)) return i;
        }
        return -1;
    }

    private void moveToHotbar(LocalPlayer player, Minecraft mc,
                              int invSlot, int hotbarSlot) {
        if (mc.gameMode == null) return;
        mc.gameMode.handleInventoryMouseClick(
                player.containerMenu.containerId,
                invSlot, hotbarSlot,
                net.minecraft.world.inventory.ClickType.SWAP,
                player
        );
        player.getInventory().selected = hotbarSlot;
    }
}