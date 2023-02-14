package io.github.ultimateboomer.resolutioncontrol.mixin;

import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class)
public class WorldRendererMixin {
    @Shadow
    private RenderTarget entityOutlineFramebuffer;

    @Inject(at = @At("RETURN"), method = "makeEntityOutlineShader")
    private void onLoadEntityOutlineShader(CallbackInfo ci) {
        if(ResolutionControlMod.isInit()) {
            ResolutionControlMod.getInstance().resizeMinecraftFramebuffers();
        }
    }

    @Inject(at = @At("RETURN"), method = "resetFrameBuffers")
    private void onOnResized(CallbackInfo ci) {
        if (entityOutlineFramebuffer == null) return;
        if(ResolutionControlMod.isInit()) {
            ResolutionControlMod.getInstance().resizeMinecraftFramebuffers();
        }
    }

//    @Inject(at = @At("RETURN"), method = "loadTransparencyShader")
//    private void onLoadTransparencyShader(CallbackInfo ci) {
//        ResolutionControlMod.getInstance().resizeMinecraftFramebuffers();
//    }
}
