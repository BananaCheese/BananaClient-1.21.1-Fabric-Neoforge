package net.bananacheese.bananaclient.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor {

    @Accessor("value")
    <T> T getValue();

    @Accessor("value")
    <T> void setValue(T value);
}