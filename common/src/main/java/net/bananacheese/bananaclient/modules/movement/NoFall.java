package net.bananacheese.bananaclient.modules.movement;

import net.bananacheese.bananaclient.modules.CycleSetting;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.bananacheese.bananaclient.modules.RegistryListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
                    2, // default = Both
                    "Packet", "Reset", "Both", "AutoPlace")
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

    // Must have fallen this far before we try to place anything
    private static final float FALL_THRESHOLD = 3.5f;

    // Only place a block item when we are at least this many blocks above ground.
    // Gives the block time to exist before we land on it.
    private static final int BLOCK_PLACE_MIN_HEIGHT = 5;

    // How far down to scan for a solid surface
    private static final int GROUND_SCAN_DEPTH = 12;

    // ── State ─────────────────────────────────────────────────────────────

    private boolean hasPlaced             = false;
    private int     prevSlot              = -1;
    private int     pickupDelay           = 0;
    private BlockPos placedPos            = null;
    private boolean placedWasFluidBucket  = false; // true only for water bucket

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

    public static boolean shouldReset() {
        if (!isActive()) return false;
        return INSTANCE.mode.is("Reset") || INSTANCE.mode.is("Both");
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

                // Only schedule pickup for water bucket placements
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

        // Find solid ground below and how far away it is
        BlockPos surface = findSolidBelow(player, mc, GROUND_SCAN_DEPTH);

        int slot = findAllowedSlot(player);
        if (slot < 0) return;

        ItemStack stack = player.getInventory().getItem(slot);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        boolean isWaterBucket = isWaterBucket(itemId);
        boolean isBlockItem   = !isWaterBucket; // everything else needs a surface

        // For block items, only place if we have enough height above the surface
        // so the block has time to exist before we land
        if (isBlockItem) {
            if (surface == null) {
                System.out.println("[BananaClient] NoFall: no ground found for block item");
                return;
            }
            double heightAboveSurface = player.getY() - (surface.getY() + 1);
            System.out.println("[BananaClient] NoFall: height above surface = " + heightAboveSurface);
            if (heightAboveSurface < BLOCK_PLACE_MIN_HEIGHT) {
                // Not high enough yet — wait for next tick
                return;
            }
        }

        System.out.println("[BananaClient] NoFall: attempting to place " + itemId);

        // Switch slot if needed
        int current = player.getInventory().selected;
        if (slot != current) {
            prevSlot = current;
            player.getInventory().selected = slot;
        } else {
            prevSlot = -1;
        }

        placedPos            = BlockPos.containing(player.getX(), player.getY(), player.getZ());
        placedWasFluidBucket = isWaterBucket;

        boolean placed = isWaterBucket
                ? tryPlaceWaterBucket(player, mc)
                : tryPlaceBlockItem(player, mc, surface);

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

    // ── Placement strategies ──────────────────────────────────────────────

    /**
     * Water bucket — useItem in air places water at player feet.
     * This is the only bucket type that works this way in 1.21.1.
     */
    private boolean tryPlaceWaterBucket(LocalPlayer player, Minecraft mc) {
        InteractionResult result = mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        return result.consumesAction();
    }

    /**
     * Block items and powder snow bucket — require a solid block face.
     * Places on top of the found surface.
     */
    private boolean tryPlaceBlockItem(LocalPlayer player, Minecraft mc, BlockPos surface) {
        if (surface == null) return false;
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(surface.above()),
                Direction.UP,
                surface,
                false
        );
        InteractionResult result = mc.gameMode.useItemOn(
                player, InteractionHand.MAIN_HAND, hit);
        return result.consumesAction();
    }

    // ── Pickup logic ──────────────────────────────────────────────────────

    /**
     * Picks up placed water by using an empty bucket while the player
     * is standing in or near the water source.
     * useItem with an empty bucket collects any fluid the player is
     * touching — much more reliable than useItemOn with a fake hit result.
     */
    private void tryPickupWater(LocalPlayer player, Minecraft mc) {
        if (mc.level == null || placedPos == null) return;

        // Verify water is actually there in a ±3 block vertical range
        boolean waterFound = false;
        for (int dy = -3; dy <= 3; dy++) {
            var state = mc.level.getBlockState(placedPos.above(dy));
            if (state.is(Blocks.WATER) || state.is(Blocks.POWDER_SNOW)) {
                waterFound = true;
                break;
            }
        }

        if (!waterFound) {
            System.out.println("[BananaClient] NoFall: pickup — no water/snow found");
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

        // useItem with empty bucket collects fluid the player is touching
        InteractionResult result = mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        System.out.println("[BananaClient] NoFall: pickup result=" + result);

        player.getInventory().selected = savedSlot;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Water bucket is the only fluid bucket that places via useItem in air.
     * Lava bucket and powder snow bucket require a block face.
     */
    private boolean isWaterBucket(ResourceLocation id) {
        if (id == null) return false;
        return id.equals(ResourceLocation.withDefaultNamespace("water_bucket"))
                || (id.getPath().endsWith("_water_bucket")); // modded water buckets e.g. create:water_bucket
    }

    /**
     * Scans downward from just below the player's feet.
     * Returns the first solid block found, or null if none within depth.
     */
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