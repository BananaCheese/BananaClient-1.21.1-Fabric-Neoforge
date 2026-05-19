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
                    "Pick up placed water/snow after landing", true)
    );

    private final RegistryListSetting allowedItems = addRegistrySettings(
            RegistryListSetting.items("Allowed Items",
                    "Items that can be auto-placed",
                    ResourceLocation.withDefaultNamespace("water_bucket"),
                    ResourceLocation.withDefaultNamespace("powder_snow_bucket"))
    );

    // ── Auto-place state ──────────────────────────────────────────────────

    // How far the player must have fallen before we trigger placement.
    // 3.5 blocks is just above the safe fall distance of 3 blocks.
    private static final float FALL_THRESHOLD = 3.5f;

    private boolean  hasPlaced   = false; // true after we placed this fall
    private int      prevSlot    = -1;    // hotbar slot to restore after placing
    private int      pickupDelay = 0;     // ticks remaining before pickup attempt
    private BlockPos placedPos   = null;  // where we placed the fluid

    public NoFall() {
        super("NoFall", "Cancels fall damage", Category.MOVEMENT, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private void resetState() {
        hasPlaced   = false;
        prevSlot    = -1;
        pickupDelay = 0;
        placedPos   = null;
    }

    // ── Static accessors used by mixins ───────────────────────────────────

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

    // Called from MixinLocalPlayer.aiStep every tick
    public static void onTick(LocalPlayer player) {
        if (!isActive() || !INSTANCE.mode.is("AutoPlace")) return;
        INSTANCE.tickAutoPlace(player);
    }

    // ── Per-tick auto-place logic ─────────────────────────────────────────

    private void tickAutoPlace(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameMode == null) return;

        // ── Just landed ───────────────────────────────────────────────────
        if (player.onGround()) {
            if (hasPlaced) {
                // Restore the hotbar slot we were on before placement
                if (prevSlot >= 0 && prevSlot < 9) {
                    player.getInventory().selected = prevSlot;
                }
                // Start the pickup countdown
                if (autoPickup.getValue() && placedPos != null) {
                    pickupDelay = 6;
                } else {
                    placedPos = null;
                }
                hasPlaced = false;
                prevSlot  = -1;
            }

            // Pickup countdown
            if (pickupDelay > 0) {
                pickupDelay--;
                if (pickupDelay == 0 && placedPos != null) {
                    tryPickup(player, mc);
                    placedPos = null;
                }
            }
            return;
        }

        // ── Falling — check if we should place ────────────────────────────
        if (player.fallDistance < FALL_THRESHOLD) return;
        if (hasPlaced) return; // already placed this fall

        // Find an allowed item in the inventory
        int slot = findAllowedSlot(player);
        if (slot < 0) return; // nothing available

        // Switch to that slot
        int current = player.getInventory().selected;
        if (slot != current) {
            prevSlot = current;
            player.getInventory().selected = slot;
        }

        // Record where the player's feet are so we can pick up later
        placedPos = BlockPos.containing(
                player.getX(),
                player.getY(),
                player.getZ()
        );

        // Use the item — water/snow buckets use `useItem` (right-click in air)
        // This works regardless of whether there is a block below the player,
        // unlike useItemOn which requires a solid face to place against.
        InteractionResult result = mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);

        if (result.consumesAction()) {
            hasPlaced = true;
        } else {
            // useItem failed — reset state so we try again next tick
            if (prevSlot >= 0) {
                player.getInventory().selected = prevSlot;
                prevSlot = -1;
            }
            placedPos = null;
        }
    }

    // ── Pickup logic ──────────────────────────────────────────────────────

    private void tryPickup(LocalPlayer player, Minecraft mc) {
        if (mc.level == null || placedPos == null) return;

        // Check the placed position and one block above for water/snow
        BlockPos target = null;
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos check = placedPos.above(dy);
            var state = mc.level.getBlockState(check);
            if (state.is(Blocks.WATER) || state.is(Blocks.POWDER_SNOW)) {
                target = check;
                break;
            }
        }
        if (target == null) return; // block already gone or wrong type

        // Find an empty bucket to collect with
        int bucketSlot = findEmptyBucket(player);
        if (bucketSlot < 0) return;

        // Switch to the bucket slot
        int savedSlot = player.getInventory().selected;
        if (bucketSlot < 9) {
            player.getInventory().selected = bucketSlot;
        } else {
            // Move from main inventory to a hotbar slot first
            moveToHotbar(player, mc, bucketSlot, 8);
            player.getInventory().selected = 8;
        }

        // Right-click the block face to collect the fluid
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(target),
                Direction.UP,
                target,
                false
        );
        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);

        // Restore the slot
        player.getInventory().selected = savedSlot;
    }

    // ── Inventory helpers ─────────────────────────────────────────────────

    /**
     * Finds the first hotbar (or inventory if hotbarOnly=false) slot
     * containing an item in the allowed list.
     * Returns the slot index, or -1 if not found.
     */
    private int findAllowedSlot(LocalPlayer player) {
        Inventory inv = player.getInventory();
        int limit = hotbarOnly.getValue() ? 9 : inv.getContainerSize();

        for (int i = 0; i < limit; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && allowedItems.contains(id)) {
                // If in main inventory (slot >= 9), swap to hotbar first
                if (i >= 9) {
                    moveToHotbar(player, Minecraft.getInstance(), i, 8);
                    return 8;
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds an empty bucket (Items.BUCKET) in the inventory.
     * Returns slot index or -1.
     */
    private int findEmptyBucket(LocalPlayer player) {
        Inventory inv = player.getInventory();
        int limit = hotbarOnly.getValue() ? 9 : inv.getContainerSize();
        for (int i = 0; i < limit; i++) {
            if (inv.getItem(i).is(Items.BUCKET)) return i;
        }
        return -1;
    }

    /**
     * Swaps an inventory item into the specified hotbar slot using
     * the container click API so the server is informed.
     */
    private void moveToHotbar(LocalPlayer player, Minecraft mc,
                              int invSlot, int hotbarSlot) {
        if (mc.gameMode == null) return;
        mc.gameMode.handleInventoryMouseClick(
                player.containerMenu.containerId,
                invSlot,
                hotbarSlot,
                net.minecraft.world.inventory.ClickType.SWAP,
                player
        );
        player.getInventory().selected = hotbarSlot;
    }
}