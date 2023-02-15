package io.github.ultimateboomer.resolutioncontrol.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Window.class)
public abstract class MainWindowAccessor {
    @Shadow
    public abstract void refreshFramebufferSize();
}
