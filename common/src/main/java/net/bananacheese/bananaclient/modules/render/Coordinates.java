package net.bananacheese.bananaclient.modules.render;

import net.bananacheese.bananaclient.modules.HudModule;
import net.bananacheese.bananaclient.modules.Module;
import net.bananacheese.bananaclient.modules.ModuleSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class Coordinates extends Module implements HudModule {

    private final ModuleSetting<Boolean> showBiome = addSetting(
            new ModuleSetting<>("Show Biome", "Display current biome name", false)
    );

    private final ModuleSetting<Boolean> showFacing = addSetting(
            new ModuleSetting<>("Show Facing", "Display cardinal direction", true)
    );

    public Coordinates() {
        super("Coordinates", "Displays your XYZ position", Category.RENDER, GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onRenderHud(GuiGraphics gfx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        // Don't render while the module GUI is open
        if (mc.screen != null) return;

        Player p = mc.player;
        int x = (int) Math.floor(p.getX());
        int y = (int) Math.floor(p.getY());
        int z = (int) Math.floor(p.getZ());

        String coordLine = String.format("XYZ: %d / %d / %d", x, y, z);

        int drawX = 4;
        int drawY = 4;

        gfx.drawString(mc.font, coordLine, drawX, drawY, 0xFFFFFF, true);

        if (showFacing.getValue()) {
            String facing = getFacing(p);
            gfx.drawString(mc.font, "Facing: " + facing, drawX, drawY + 10, 0xAAAAAA, true);
        }

        if (showBiome.getValue()) {
            var biomeHolder = mc.level.getBiome(BlockPos.containing(p.getX(), p.getY(), p.getZ()));
            String biomeName = biomeHolder.unwrapKey()
                    .map(k -> k.location().getPath().replace("_", " "))
                    .orElse("Unknown");
            // Capitalise first letter of each word
            String[] words = biomeName.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (!w.isEmpty())
                    sb.append(Character.toUpperCase(w.charAt(0)))
                            .append(w.substring(1)).append(" ");
            }
            gfx.drawString(mc.font, "Biome: " + sb.toString().trim(),
                    drawX, drawY + 20, 0xAAAAAA, true);
        }
    }

    private String getFacing(Player p) {
        float yaw = p.getYRot() % 360;
        if (yaw < 0) yaw += 360;
        if (yaw >= 315 || yaw < 45)  return "South (+Z)";
        if (yaw < 135)                return "West (-X)";
        if (yaw < 225)                return "North (-Z)";
        return "East (+X)";
    }
}
