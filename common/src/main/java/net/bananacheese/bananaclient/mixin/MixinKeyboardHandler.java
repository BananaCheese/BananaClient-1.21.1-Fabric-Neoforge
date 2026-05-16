package net.bananacheese.bananaclient.mixin;

import net.bananacheese.bananaclient.gui.ModuleScreen;
import net.bananacheese.bananaclient.modules.ModuleManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();

        // Right Shift opens the module GUI from in-game OR the main menu
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (mc.screen == null || mc.screen instanceof TitleScreen) {
                mc.execute(() -> mc.setScreen(new ModuleScreen()));
            }
            return;
        }

        if (mc.screen != null) return;

        for (var module : ModuleManager.getAll()) {
            if (module.getKeyCode() == key) {
                module.toggle();
            }
        }
    }
}