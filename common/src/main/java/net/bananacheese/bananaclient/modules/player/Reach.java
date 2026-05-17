package net.bananacheese.bananaclient.modules.player;

import net.bananacheese.bananaclient.modules.HudModule;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public class Reach extends Module implements HudModule {

    private static Reach INSTANCE;

    private final ModuleSetting<Float> distance = addSetting(
            new ModuleSetting<>("Distance", "Reach distance in blocks", 6.0f)
    );

    private final ModuleSetting<Boolean> showDisplay = addSetting(
            new ModuleSetting<>("Show Display", "Show reach distance on HUD", true)
    );

    private final ModuleSetting<Boolean> onlyEntity = addSetting(
            new ModuleSetting<>("Only Entity", "Only show when looking at entity", true)
    );

    public Reach() {
        super("Reach", "Modifies your reach distance", Category.PLAYER, GLFW.GLFW_KEY_UNKNOWN);
        INSTANCE = this;
    }

    public static boolean isActive() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static double getDistance() {
        return INSTANCE != null ? INSTANCE.distance.getValue().doubleValue() : 4.5;
    }

    @Override
    public void onDisable() {
        // Restore default reach on disable
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var blockAttr = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockAttr != null) blockAttr.setBaseValue(4.5);
        var entityAttr = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityAttr != null) entityAttr.setBaseValue(3.0);
    }

    @Override
    public void onRenderHud(GuiGraphics gfx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        if (!showDisplay.getValue()) return;

        HitResult hit = mc.hitResult;

        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            if (onlyEntity.getValue()) return;
            int sw = mc.getWindow().getGuiScaledWidth();
            gfx.drawString(mc.font, "Reach: --", sw - 60, 4, 0xAAAAAA, true);
            return;
        }

        EntityHitResult entityHit = (EntityHitResult) hit;
        Entity target = entityHit.getEntity();
        double dist = mc.player.distanceTo(target);

        String line = String.format("Reach: %.2f / %.1f", dist, distance.getValue());
        int textW = mc.font.width(line);
        int screenW = mc.getWindow().getGuiScaledWidth();
        gfx.drawString(mc.font, line, screenW - textW - 4, 4, 0xFFFFFF, true);
    }
}