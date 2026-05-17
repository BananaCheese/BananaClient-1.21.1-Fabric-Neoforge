package net.bananacheese.bananaclient.modules.combat;

import net.bananacheese.bananaclient.modules.HudModule;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public class ReachDisplay extends Module implements HudModule {

    private final ModuleSetting<Boolean> onlyWhenLooking = addSetting(
            new ModuleSetting<>("Only When Looking",
                    "Only show when looking at an entity", true)
    );

    public ReachDisplay() {
        super("Reach Display", "Shows distance to looked-at entity",
                Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onRenderHud(GuiGraphics gfx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        HitResult hit = mc.hitResult;

        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            if (onlyWhenLooking.getValue()) return;
            // Show default reach when not looking at anything
            gfx.drawString(mc.font, "Reach: --",
                    mc.getWindow().getGuiScaledWidth() - 60, 4, 0xAAAAAA, true);
            return;
        }

        EntityHitResult entityHit = (EntityHitResult) hit;
        Entity target = entityHit.getEntity();
        double dist = mc.player.distanceTo(target);

        String line = String.format("Reach: %.2f", dist);
        int textW = mc.font.width(line);
        int screenW = mc.getWindow().getGuiScaledWidth();

        gfx.drawString(mc.font, line, screenW - textW - 4, 4, 0xFFFFFF, true);
    }
}