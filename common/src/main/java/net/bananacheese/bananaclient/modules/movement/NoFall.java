package net.bananacheese.bananaclient.modules.movement;

import net.bananacheese.bananaclient.modules.CycleSetting;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.bananacheese.bananaclient.modules.RegistryListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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

    private final ModuleSetting<Boolean> autoPickup = addSetting(
            new ModuleSetting<>("Auto Pickup",
                    "Pick up placed water after landing", true)
    );

    private final RegistryListSetting allowedItems = addRegistrySettings(
            RegistryListSetting.items("Allowed Items",
                    "Items that can be auto-placed",
                    ResourceLocation.withDefaultNamespace("water_bucket"),
                    ResourceLocation.withDefaultNamespace("powder_snow_bucket"))
    );

    // ── Constants ─────────────────────────────────────────────────────────

    // Fall distance before we even check anything
    private static final float FALL_THRESHOLD = 3.5f;

    // Place when this many blocks or fewer above the surface
    // Low enough to be safe but high enough that the block exists before landing
    private static final double PLACE_WHEN_WITHIN = 4.0;

    // How far down to scan for solid ground
    private static final int GROUND_SCAN_DEPTH = 64;

    // ── State ─────────────────────────────────────────────────────────────

    private boolean  hasPlaced           = false;
    private int      prevSlot            = -1;
    private int      pickupDelay         = 0;
    private BlockPos placedPos           = null;
    private boolean  placedWasFluidBucket = false;

    public NoFall() {
        super("NoFall", "Cancels fall damage", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    @Override
    public void onDisable() { resetState(); }

    private void resetState() {
        hasPlaced            = false;
        prevSlot             = -1;
        pickupDelay          = 0;
        placedPos            = null;
        placedWasFluidBucket = false;
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
        if (!isActive() || !INSTANCE.mode.is("AutoPlace")) return;
        INSTANCE.tickAutoPlace(player);
    }

    // ── Per-tick logic ────────────────────────────────────────────────────

    private void tickAutoPlace(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameMode == null) return;

        // ── Just landed ───────────────────────────────────────────────────
        if (player.onGround()) {
            if (hasPlaced) {
                if (prevSlot >= 0 && prevSlot < 9)
                    player.getInventory().selected = prevSlot;

                if (autoPickup.getValue() && placedPos != null && placedWasFluidBucket) {
                    pickupDelay = 8;
                } else {
                    placedPos = null;
                }
                hasPlaced            = false;
                prevSlot             = -1;
                placedWasFluidBucket = false;
            }

            if (pickupDelay > 0) {
                pickupDelay--;
                if (pickupDelay == 0 && placedPos != null) {
                    tryPickupWater(player, mc);
                    placedPos = null;
                }
            }
            return;
        }

        // ── Falling ───────────────────────────────────────────────────────
        if (player.fallDistance < FALL_THRESHOLD) return;
        if (hasPlaced) return;

        // Find solid ground below
        BlockPos surface = findSolidBelow(player, mc, GROUND_SCAN_DEPTH);
        double heightAboveSurface = surface != null
                ? player.getY() - (surface.getY() + 1)
                : Double.MAX_VALUE;

        // Only place when close enough to the ground
        if (heightAboveSurface > PLACE_WHEN_WITHIN) return;

        int slot = findAllowedSlot(player);
        if (slot < 0) return;

        ItemStack stack = player.getInventory().getItem(slot);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        System.out.println("[BananaClient] NoFall: placing " + itemId
                + " (height=" + String.format("%.2f", heightAboveSurface) + ")");

        // Switch slot if needed
        int current = player.getInventory().selected;
        if (slot != current) {
            prevSlot = current;
            player.getInventory().selected = slot;
        } else {
            prevSlot = -1;
        }

        placedPos            = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        placedWasFluidBucket = isWaterBucket(itemId);

        // Look straight down, use the item, restore look direction
        boolean placed = placeWithLookDown(player, mc);
        System.out.println("[BananaClient] NoFall: placed=" + placed);

        if (placed) {
            hasPlaced = true;
        } else {
            if (prevSlot >= 0) {
                player.getInventory().selected = prevSlot;
                prevSlot = -1;
            }
            placedPos            = null;
            placedWasFluidBucket = false;
        }
    }

    /**
     * Temporarily rotates the player to look straight down,
     * calls useItem (works for all placeable items when looking down),
     * then restores the original rotation.
     *
     * Looking straight down means the item places at the player's feet
     * on the block directly below — correct for water, snow, and blocks.
     */
    private boolean placeWithLookDown(LocalPlayer player, Minecraft mc) {
        // Save current rotation
        float savedPitch = player.getXRot();
        float savedYaw   = player.getYRot();

        // Rotate to look straight down
        player.setXRot(90.0f);
        player.setYRot(savedYaw); // yaw doesn't matter when looking straight down

        // Also update the camera entity so raycast targets correctly
        if (mc.getCameraEntity() == player) {
            mc.getCameraEntity().setXRot(90.0f);
        }

        // Use the item
        InteractionResult result = mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        boolean success = result.consumesAction();

        // Restore rotation
        player.setXRot(savedPitch);
        player.setYRot(savedYaw);
        if (mc.getCameraEntity() == player) {
            mc.getCameraEntity().setXRot(savedPitch);
        }

        return success;
    }

    // ── Pickup ────────────────────────────────────────────────────────────

    private void tryPickupWater(LocalPlayer player, Minecraft mc) {
        if (mc.level == null || placedPos == null) return;

        // Check ±3 blocks vertically from where we placed
        boolean waterFound = false;
        for (int dy = -3; dy <= 3; dy++) {
            var state = mc.level.getBlockState(placedPos.above(dy));
            if (state.is(Blocks.WATER) || state.is(Blocks.POWDER_SNOW)) {
                waterFound = true;
                break;
            }
        }

        if (!waterFound) {
            System.out.println("[BananaClient] NoFall: pickup — no water/snow found near " + placedPos);
            return;
        }

        int bucketSlot = findEmptyBucket(player);
        if (bucketSlot < 0) {
            System.out.println("[BananaClient] NoFall: pickup — no empty bucket");
            return;
        }

        int savedSlot = player.getInventory().selected;

        if (bucketSlot >= 9) {
            moveToHotbar(player, mc, bucketSlot, 8);
            player.getInventory().selected = 8;
        } else {
            player.getInventory().selected = bucketSlot;
        }

        // Look down and use the empty bucket — collects fluid at feet
        boolean result = placeWithLookDown(player, mc);
        System.out.println("[BananaClient] NoFall: pickup result=" + result);

        player.getInventory().selected = savedSlot;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Only vanilla water_bucket and modded *_water_bucket variants
     * are treated as water buckets. Everything else (powder snow,
     * hay bale, etc.) uses the same useItem-with-look-down path
     * but doesn't trigger auto-pickup.
     */
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
