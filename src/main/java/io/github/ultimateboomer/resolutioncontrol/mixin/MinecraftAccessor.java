package io.github.ultimateboomer.resolutioncontrol.mixin;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Minecraft.class)
public interface MinecraftAccessor {
    @Accessor
    RenderTarget getMainRenderTarget();

//  @Mutable
//  @Accessor(value = "framebuffer")
//  void setFramebuffer(RenderTarget framebuffer);

    @Mutable
    @Accessor(value = "mainRenderTarget")
    void setRenderTarget(RenderTarget renderTarget);
}
