package io.github.ultimateboomer.resolutioncontrol.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Window.class)
public interface MainWindowAccessor {
    @Invoker
    void refreshFramebufferSize();
}
