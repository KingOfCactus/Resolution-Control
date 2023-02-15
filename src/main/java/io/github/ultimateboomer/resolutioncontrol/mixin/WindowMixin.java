package io.github.ultimateboomer.resolutioncontrol.mixin;

import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Window.class)
public abstract class WindowMixin {
	@Inject(at = @At("RETURN"), method = "getWidth", cancellable = true)
	private void getWidth(CallbackInfoReturnable<Integer> ci) {
		if (ResolutionControlMod.isInit()  && ResolutionControlMod.getInstance().isScreenshotting()) {
			ci.setReturnValue(ResolutionControlMod.getInstance().getScreenshotWidth());
		} else {
			ci.setReturnValue(scale(ci.getReturnValueI()));
		}
	}
	
	@Inject(at = @At("RETURN"), method = "getHeight", cancellable = true)
	private void getHeight(CallbackInfoReturnable<Integer> ci) {
		if (ResolutionControlMod.isInit() && ResolutionControlMod.getInstance().isScreenshotting()) {
			ci.setReturnValue(ResolutionControlMod.getInstance().getScreenshotHeight());
		} else {
			ci.setReturnValue(scale(ci.getReturnValueI()));
		}
	}
	
	private int scale(int value) {
		if(!ResolutionControlMod.isInit()) return value;
		double scaleFactor = ResolutionControlMod.getInstance().getCurrentScaleFactor();
		return Math.max(Mth.ceil((double) value * scaleFactor), 1);
	}
	
	@Inject(at = @At("RETURN"), method = "getGuiScale", cancellable = true)
	private void getScaleFactor(CallbackInfoReturnable<Double> ci) {
		if(ResolutionControlMod.isInit()) {
			ci.setReturnValue(ci.getReturnValueD() * ResolutionControlMod.getInstance().getCurrentScaleFactor());
		}else{
			ci.setReturnValue(ci.getReturnValueD());
		}
	}
	
	@Inject(at = @At("RETURN"), method = "onResize")
	private void onFramebufferSizeChanged(CallbackInfo ci) {
		if(ResolutionControlMod.isInit()) {
			ResolutionControlMod.getInstance().onResolutionChanged();
		}

	}
	
	@Inject(at = @At("RETURN"), method = "refreshFramebufferSize")
	private void onUpdateFramebufferSize(CallbackInfo ci) {
		if(ResolutionControlMod.isInit()) {
			ResolutionControlMod.getInstance().onResolutionChanged();
		}
	}
}
