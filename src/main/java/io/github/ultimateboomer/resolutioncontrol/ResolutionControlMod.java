package io.github.ultimateboomer.resolutioncontrol;

import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.MainSettingsScreen;
import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.SettingsScreen;
import io.github.ultimateboomer.resolutioncontrol.mixin.MainWindowAccessor;
import io.github.ultimateboomer.resolutioncontrol.mixin.MinecraftAccessor;
import io.github.ultimateboomer.resolutioncontrol.util.*;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.event.server.ServerStartedEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Mod("resolutioncontrol")
public class ResolutionControlMod{
	public static final String MOD_ID = "resolutioncontrol";
	public static final String MOD_NAME = "ResolutionControl+";

	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

	public static ResourceLocation identifier(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
	
	private final Minecraft client;
	
	private static ResolutionControlMod instance;
	
	public static ResolutionControlMod getInstance() {
		return instance;
	}

//	private static final String SCREENSHOT_PREFIX = "fb";

	private boolean optifineInstalled;
	
	private KeyMapping settingsKey;
	private KeyMapping screenshotKey;
	
	private boolean shouldScale = false;
	
	@Nullable
	private RenderTarget renderTarget;
	@Nullable
	private RenderTarget screenshotRenderTarget;
	@Nullable
	private RenderTarget clientRenderTarget;
	
	private Set<RenderTarget> minecraftRenderTargets;

	private Class<? extends SettingsScreen> lastSettingsScreen = MainSettingsScreen.class;

	private int currentWidth;
	private int currentHeight;

	private long estimatedMemory;

	private boolean screenshot = false;

//	private int lastWidth;
//	private int lastHeight;


	public ResolutionControlMod(){
		client = Minecraft.getInstance();

		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

		MinecraftForge.EVENT_BUS.register(this);

		onResolutionChanged();
	}

	private void setup(final FMLCommonSetupEvent event)
	{
		// some preinit code
	}

	private void doClientStuff(final FMLClientSetupEvent event) {
		onInitialize();
	}

	private void enqueueIMC(final InterModEnqueueEvent event)
	{
	}

	private void processIMC(final InterModProcessEvent event){
	}

	@SubscribeEvent
	public void serverLoaded(ServerStartedEvent event){
		if (ConfigHandler.instance.getConfig().enableDynamicResolution) {
			DynamicResolutionHandler.INSTANCE.reset();
		}
	}

	@SubscribeEvent
	public void modLoaded(FMLLoadCompleteEvent event){
		onResolutionChanged();
		Object obj = Minecraft.getInstance().getWindow();
		((MainWindowAccessor)(obj)).refreshFramebufferSize();
	}

	public void onInitialize() {
		instance = this;

		settingsKey = new KeyMapping(
				"key.resolutioncontrol.settings",
				GLFW.GLFW_KEY_O,
				"key.categories.resolutioncontrol");
		ClientRegistry.registerKeyBinding(settingsKey);

		screenshotKey = new KeyMapping(
				"key.resolutioncontrol.screenshot",
				-1,
				"key.categories.resolutioncontrol");
		ClientRegistry.registerKeyBinding(screenshotKey);

		// TODO: 2022/1/24
		optifineInstalled = false;
	}

	public static boolean isInit(){
		return instance != null;
	}

	@SubscribeEvent
	public void onClientEvent(TickEvent.ClientTickEvent event){
		if(event.phase.equals(TickEvent.Phase.END)){
			while (settingsKey.isDown()) {
				client.setScreen(SettingsScreen.getScreen(lastSettingsScreen));
			}

			while (screenshotKey.isDown()) {
				if (getOverrideScreenshotScale()) {
					this.screenshot = true;
				//	client.player.sendStatusMessage(
				//			new TranslatableComponent("resolutioncontrol.screenshot.wait"), false);
					net.minecraft.network.chat.Component message = (net.minecraft.network.chat.Component) new TextComponent("resolutioncontrol.screenshot.wait");
				 	client.player.sendMessage(message, client.player.getUUID());

				} else {
					saveScreenshot(renderTarget);
				}
			}

			if (ConfigHandler.instance.getConfig().enableDynamicResolution && client.player.isAddedToWorld()
					&& getWindow().getX() != -32000) {
				DynamicResolutionHandler.INSTANCE.tick();
			}
		}
	}

	private void saveScreenshot(RenderTarget fb) {
	/*	Screenshot.saveScreenshot(client.gameDirectory,
				RCUtil.getScreenshotFilename(client.gameDirectory).toString(),
				fb.framebufferTextureWidth, fb.framebufferTextureHeight, fb,
				text -> client.player.sendStatusMessage(text, false)); */

		Screenshot.takeScreenshot(fb);
	}
	
	public void setShouldScale(boolean shouldScale) {
		if (shouldScale == this.shouldScale) return;

		if (getScaleFactor() == 1) return;
		
		Window window = getWindow();
		if (renderTarget == null) {
			this.shouldScale = true; // so we get the right dimensions

		//	renderTarget = new RenderTarget(
		//			window.getWidth(),
		//			window.getHeight(),
		//			true,
		//			Minecraft.ON_OSX
		//	);

			renderTarget = new RenderTarget(true) {};
			renderTarget.resize(window.getWidth(), window.getHeight(), true);
		
			calculateSize();
		}

		this.shouldScale = shouldScale;

	//  client.getProfiler().endStartSection(shouldScale ? "startScaling" : "finishScaling");
		var profiller = client.getProfiler();
		profiller.push(shouldScale ? "startScaling" : "finishScaling");


		// swap out framebuffers as needed
		if (shouldScale) {
			clientRenderTarget = client.getMainRenderTarget();

			if (screenshot) {
				resizeMinecraftRenderTargets();

			//	if (!isScreenshotFramebufferAlwaysAllocated() && screenshotRenderTarget != null) {
			//		screenshotRenderTarget.deleteFramebuffer();
			//	}
			
				if (!isScreenshotFramebufferAlwaysAllocated() && screenshotRenderTarget != null) {
					screenshotRenderTarget.destroyBuffers();
					screenshotRenderTarget.clear(true);
				}

				if (screenshotRenderTarget == null) {
					initScreenshotRenderTarget();
				}

				setClientRenderTarget(screenshotRenderTarget);

				screenshotRenderTarget.bindWrite(true);
			} else {
				setClientRenderTarget(renderTarget);

				renderTarget.bindWrite(true);
			}
			// nothing on the client's renderTarget yet
		} else {
			setClientRenderTarget(clientRenderTarget);
			client.getMainRenderTarget().bindWrite(true);

			// Screenshot renderTarget
			if (screenshot) {
				saveScreenshot(screenshotRenderTarget);

				if (!isScreenshotFramebufferAlwaysAllocated()) {
				//	screenshotRenderTarget.deleteFramebuffer();
					screenshotRenderTarget.destroyBuffers();
					screenshotRenderTarget.clear(true);

					screenshotRenderTarget = null;
				}

				screenshot = false;
				resizeMinecraftRenderTargets();
			} else {
				renderTarget.blitToScreen(
						window.getWidth(),
						window.getHeight()
				);
			}
		}

	//	client.getProfiler().endStartSection("level");
		client.getProfiler().push("level");
	}

	public void initMinecraftRenderTargets() {
		if (minecraftRenderTargets != null) {
			minecraftRenderTargets.clear();
		} else {
			minecraftRenderTargets = new HashSet<>();
		}

	//	minecraftRenderTargets.add(client.worldRenderer.getEntityOutlineFramebuffer());
	//	minecraftRenderTargets.add(client.worldRenderer.getTranslucentFrameBuffer());
	//	minecraftRenderTargets.add(client.worldRenderer.getItemEntityFrameBuffer());
	//	minecraftRenderTargets.add(client.worldRenderer.getParticleFrameBuffer());
	//	minecraftRenderTargets.add(client.worldRenderer.getWeatherFrameBuffer());
	//	minecraftRenderTargets.add(client.worldRenderer.getCloudFrameBuffer());

	 	minecraftRenderTargets.add(client.levelRenderer.entityTarget());
	 	minecraftRenderTargets.add(client.levelRenderer.getTranslucentTarget());
	 	minecraftRenderTargets.add(client.levelRenderer.getItemEntityTarget());
	 	minecraftRenderTargets.add(client.levelRenderer.getParticlesTarget());
	 	minecraftRenderTargets.add(client.levelRenderer.getWeatherTarget());
	 	minecraftRenderTargets.add(client.levelRenderer.getCloudsTarget());
	}

	public RenderTarget getFramebuffer() {
		return renderTarget;
	}

	public void initScreenshotRenderTarget() {
	//	if (Objects.nonNull(screenshotRenderTarget)) screenshotRenderTarget.deleteFramebuffer();
		if (Objects.nonNull(screenshotRenderTarget)) { 
			screenshotRenderTarget.destroyBuffers();
			screenshotRenderTarget.clear(true);
		}

	//	screenshotRenderTarget = new RenderTarget(
	//			getScreenshotWidth(), getScreenshotHeight(),
	//			true, Minecraft.ON_OSX);

		renderTarget = new RenderTarget(true) {};
		renderTarget.resize(getScreenshotWidth(), getScreenshotHeight(), true);
	}
	
	public float getScaleFactor() {
		return Config.getInstance().scaleFactor;
	}
	
	public void setScaleFactor(float scaleFactor) {
		Config.getInstance().scaleFactor = scaleFactor;
		
		updateRenderTargetSize();
		
		ConfigHandler.instance.saveConfig();
	}

	public ScalingAlgorithm getUpscaleAlgorithm() {
		return Config.getInstance().upscaleAlgorithm;
	}

	public void setUpscaleAlgorithm(ScalingAlgorithm algorithm) {
		if (algorithm == Config.getInstance().upscaleAlgorithm) return;

		Config.getInstance().upscaleAlgorithm = algorithm;

		onResolutionChanged();

		ConfigHandler.instance.saveConfig();
	}

	public void nextUpscaleAlgorithm() {
		ScalingAlgorithm currentAlgorithm = getUpscaleAlgorithm();
		if (currentAlgorithm.equals(ScalingAlgorithm.NEAREST)) {
			setUpscaleAlgorithm(ScalingAlgorithm.LINEAR);
		} else {
			setUpscaleAlgorithm(ScalingAlgorithm.NEAREST);
		}
	}

	public ScalingAlgorithm getDownscaleAlgorithm() {
		return Config.getInstance().downscaleAlgorithm;
	}

	public void setDownscaleAlgorithm(ScalingAlgorithm algorithm) {
		if (algorithm == Config.getInstance().downscaleAlgorithm) return;

		Config.getInstance().downscaleAlgorithm = algorithm;

		onResolutionChanged();

		ConfigHandler.instance.saveConfig();
	}

	public void nextDownscaleAlgorithm() {
		ScalingAlgorithm currentAlgorithm = getDownscaleAlgorithm();
		if (currentAlgorithm.equals(ScalingAlgorithm.NEAREST)) {
			setDownscaleAlgorithm(ScalingAlgorithm.LINEAR);
		} else {
			setDownscaleAlgorithm(ScalingAlgorithm.NEAREST);
		}
	}
	
	public double getCurrentScaleFactor() {
		return shouldScale ?
				Config.getInstance().enableDynamicResolution ?
						DynamicResolutionHandler.INSTANCE.getCurrentScale() : Config.getInstance().scaleFactor : 1;
	}

	public boolean getOverrideScreenshotScale() {
		return Config.getInstance().overrideScreenshotScale;
	}

	public void setOverrideScreenshotScale(boolean value) {
		Config.getInstance().overrideScreenshotScale = value;
		if (value && isScreenshotFramebufferAlwaysAllocated()) {
			initScreenshotRenderTarget();
		} else {
			if (screenshotRenderTarget != null) {
			//	screenshotRenderTarget.deleteFramebuffer();
				screenshotRenderTarget.destroyBuffers();
				screenshotRenderTarget.clear(true);
				screenshotRenderTarget = null;
			}
		}
	}

	public int getScreenshotWidth() {
		return Math.max(Config.getInstance().screenshotWidth, 1);
	}

	public void setScreenshotWidth(int width) {
		Config.getInstance().screenshotWidth = width;
	}

	public int getScreenshotHeight() {
		return Math.max(Config.getInstance().screenshotHeight, 1);
	}

	public void setScreenshotHeight(int height) {
		Config.getInstance().screenshotHeight = height;
	}

	public boolean isScreenshotFramebufferAlwaysAllocated() {
		return Config.getInstance().screenshotFramebufferAlwaysAllocated;
	}

	public void setScreenshotFramebufferAlwaysAllocated(boolean value) {
		Config.getInstance().screenshotFramebufferAlwaysAllocated = value;

		if (value) {
			if (getOverrideScreenshotScale() && Objects.isNull(this.screenshotRenderTarget)) {
				initScreenshotRenderTarget();
			}
		} else {
			if (this.screenshotRenderTarget != null) {
			//	this.screenshotRenderTarget.deleteFramebuffer();
				this.screenshotRenderTarget.destroyBuffers();
				this.screenshotRenderTarget.clear(true);
				this.screenshotRenderTarget = null;
			}
		}
	}

	public void setEnableDynamicResolution(boolean enableDynamicResolution) {
		Config.getInstance().enableDynamicResolution = enableDynamicResolution;
	}

	public void onResolutionChanged() {
		if (getWindow() == null)
			return;

		LOGGER.info("Size changed to {}x{} {}x{} {}x{}",
				getWindow().getWidth(), getWindow().getHeight(),
				getWindow().getWidth(), getWindow().getHeight(),
				getWindow().getGuiScaledWidth(), getWindow().getGuiScaledHeight());

//		if (getWindow().getScaledHeight() == lastWidth
//				|| getWindow().getScaledHeight() == lastHeight)
//		{
			updateRenderTargetSize();

//			lastWidth = getWindow().getGuiScaledWidth();
//			lastHeight = getWindow().getGuiScaledHeight();
//		}


	}
	
	public void updateRenderTargetSize() {
		if (renderTarget == null)
			return;

		resize(renderTarget);
//		resize(client.worldRenderer.getEntityOutlineFramebuffer());
		resize(client.levelRenderer.entityTarget());
//		resizeMinecraftRenderTargets();

		calculateSize();
	}

	public void resizeMinecraftRenderTargets() {
		initMinecraftRenderTargets();
		minecraftRenderTargets.forEach(this::resize);
	}

	public void calculateSize() {
		currentWidth = renderTarget.width;
		currentHeight = renderTarget.height;

		// Framebuffer uses color (4 x 8 = 32 bit int) and depth (32 bit float)
		estimatedMemory = (long) currentWidth * currentHeight * 8;
	}
	
	public void resize(@Nullable RenderTarget renderTarget) {
		if (renderTarget == null) return;

		boolean prev = shouldScale;
		shouldScale = true;
		if (screenshot) {
			renderTarget.resize(
					getScreenshotWidth(),
					getScreenshotHeight(),
					Minecraft.ON_OSX
			);
		} else {
			renderTarget.resize(
					getWindow().getWidth(),
					getWindow().getHeight(),
					Minecraft.ON_OSX
			);
		}
		shouldScale = prev;
	}
	
	private Window getWindow() {
		return client.getWindow();
	}

	private void setClientRenderTarget(RenderTarget renderTarget) {
		((MinecraftAccessor)client).setRenderTarget(renderTarget);
	}

	public KeyMapping getSettingsKey() {
		return settingsKey;
	}

	public int getCurrentWidth() {
		return currentWidth;
	}

	public int getCurrentHeight() {
		return currentHeight;
	}

	public long getEstimatedMemory() {
		return estimatedMemory;
	}

	public boolean isScreenshotting() {
		return screenshot;
	}

	public boolean isOptifineInstalled() {
		return optifineInstalled;
	}

	public void saveSettings() {
		ConfigHandler.instance.saveConfig();
	}

	public void setLastSettingsScreen(Class<? extends SettingsScreen> ordinal) {
		this.lastSettingsScreen = ordinal;
	}

}
