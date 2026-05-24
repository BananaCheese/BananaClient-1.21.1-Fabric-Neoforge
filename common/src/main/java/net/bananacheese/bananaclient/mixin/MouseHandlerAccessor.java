package net.bananacheese.bananaclient.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Accessor("accumulatedDX")
    double getAccumulatedDX();

    @Accessor("accumulatedDY")
    double getAccumulatedDY();

    @Accessor("accumulatedDX")
    void setAccumulatedDX(double value);

    @Accessor("accumulatedDY")
    void setAccumulatedDY(double value);
}

