package io.github.ultimateboomer.resolutioncontrol;

import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.MainSettingsScreen;
import io.github.ultimateboomer.resolutioncontrol.client.gui.screen.SettingsScreen;
import io.github.ultimateboomer.resolutioncontrol.mixin.MainWindowAccessor;
import io.github.ultimateboomer.resolutioncontrol.mixin.MinecraftAccessor;
import io.github.ultimateboomer.resolutioncontrol.util.*;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.java.games.input.Component;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.*;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmlclient.registry.ClientRegistry;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;

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

	private static final String SCREENSHOT_PREFIX = "fb";

	private boolean optifineInstalled;
	
	private KeyMapping settingsKey;
	private KeyMapping screenshotKey;
	
	private boolean shouldScale = false;
	
	@Nullable
	private RenderTarget framebuffer;

	@Nullable
	private RenderTarget screenshotFrameBuffer;
	
	@Nullable
	private RenderTarget clientFramebuffer;

	private Set<RenderTarget> minecraftFramebuffers;

	private Class<? extends SettingsScreen> lastSettingsScreen = MainSettingsScreen.class;

	private int currentWidth;
	private int currentHeight;

	private long estimatedMemory;

	private boolean screenshot = false;

	private int lastWidth;
	private int lastHeight;


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
	public void serverLoaded(FMLServerStartedEvent event){
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
					saveScreenshot(framebuffer);
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
		if (framebuffer == null) {
			this.shouldScale = true; // so we get the right dimensions

		//	framebuffer = new RenderTarget(
		//			window.getWidth(),
		//			window.getHeight(),
		//			true,
		//			Minecraft.ON_OSX
		//	);

			framebuffer = new RenderTarget(true) {};
			framebuffer.resize(window.getWidth(), window.getHeight(), true);
		
			calculateSize();
		}

		this.shouldScale = shouldScale;

	//  client.getProfiler().endStartSection(shouldScale ? "startScaling" : "finishScaling");
		var profiller = client.getProfiler();
		profiller.push(shouldScale ? "startScaling" : "finishScaling");


		// swap out framebuffers as needed
		if (shouldScale) {
			clientFramebuffer = client.getMainRenderTarget();

			if (screenshot) {
				resizeMinecraftFramebuffers();

			//	if (!isScreenshotFramebufferAlwaysAllocated() && screenshotFrameBuffer != null) {
			//		screenshotFrameBuffer.deleteFramebuffer();
			//	}
			
				if (!isScreenshotFramebufferAlwaysAllocated() && screenshotFrameBuffer != null) {
					screenshotFrameBuffer.destroyBuffers();
					screenshotFrameBuffer.clear(true);
				}

				if (screenshotFrameBuffer == null) {
					initScreenshotFramebuffer();
				}

				setClientFramebuffer(screenshotFrameBuffer);

				screenshotFrameBuffer.bindWrite(true);
			} else {
				setClientFramebuffer(framebuffer);

				framebuffer.bindWrite(true);
			}
			// nothing on the client's framebuffer yet
		} else {
			setClientFramebuffer(clientFramebuffer);
			client.getMainRenderTarget().bindWrite(true);

			// Screenshot framebuffer
			if (screenshot) {
				saveScreenshot(screenshotFrameBuffer);

				if (!isScreenshotFramebufferAlwaysAllocated()) {
				//	screenshotFrameBuffer.deleteFramebuffer();
					screenshotFrameBuffer.destroyBuffers();
					screenshotFrameBuffer.clear(true);

					screenshotFrameBuffer = null;
				}

				screenshot = false;
				resizeMinecraftFramebuffers();
			} else {
				framebuffer.blitToScreen(
						window.getWidth(),
						window.getHeight()
				);
			}
		}

	//	client.getProfiler().endStartSection("level");
		client.getProfiler().push("level");
	}

	public void initMinecraftFramebuffers() {
		if (minecraftFramebuffers != null) {
			minecraftFramebuffers.clear();
		} else {
			minecraftFramebuffers = new HashSet<>();
		}

	//	minecraftFramebuffers.add(client.worldRenderer.getEntityOutlineFramebuffer());
	//	minecraftFramebuffers.add(client.worldRenderer.getTranslucentFrameBuffer());
	//	minecraftFramebuffers.add(client.worldRenderer.getItemEntityFrameBuffer());
	//	minecraftFramebuffers.add(client.worldRenderer.getParticleFrameBuffer());
	//	minecraftFramebuffers.add(client.worldRenderer.getWeatherFrameBuffer());
	//	minecraftFramebuffers.add(client.worldRenderer.getCloudFrameBuffer());

	 	minecraftFramebuffers.add(client.levelRenderer.entityTarget());
	 	minecraftFramebuffers.add(client.levelRenderer.getTranslucentTarget());
	 	minecraftFramebuffers.add(client.levelRenderer.getItemEntityTarget());
	 	minecraftFramebuffers.add(client.levelRenderer.getParticlesTarget());
	 	minecraftFramebuffers.add(client.levelRenderer.getWeatherTarget());
	 	minecraftFramebuffers.add(client.levelRenderer.getCloudsTarget());
	}

	public RenderTarget getFramebuffer() {
		return framebuffer;
	}

	public void initScreenshotFramebuffer() {
	//	if (Objects.nonNull(screenshotFrameBuffer)) screenshotFrameBuffer.deleteFramebuffer();
		if (Objects.nonNull(screenshotFrameBuffer)) { 
			screenshotFrameBuffer.destroyBuffers();
			screenshotFrameBuffer.clear(true);
		}

	//	screenshotFrameBuffer = new RenderTarget(
	//			getScreenshotWidth(), getScreenshotHeight(),
	//			true, Minecraft.ON_OSX);

		framebuffer = new RenderTarget(true) {};
		framebuffer.resize(getScreenshotWidth(), getScreenshotHeight(), true);

	}
	
	public float getScaleFactor() {
		return Config.getInstance().scaleFactor;
	}
	
	public void setScaleFactor(float scaleFactor) {
		Config.getInstance().scaleFactor = scaleFactor;
		
		updateFramebufferSize();
		
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
			initScreenshotFramebuffer();
		} else {
			if (screenshotFrameBuffer != null) {
			//	screenshotFrameBuffer.deleteFramebuffer();
				screenshotFrameBuffer.destroyBuffers();
				screenshotFrameBuffer.clear(true);
				screenshotFrameBuffer = null;
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
			if (getOverrideScreenshotScale() && Objects.isNull(this.screenshotFrameBuffer)) {
				initScreenshotFramebuffer();
			}
		} else {
			if (this.screenshotFrameBuffer != null) {
			//	this.screenshotFrameBuffer.deleteFramebuffer();
				this.screenshotFrameBuffer.destroyBuffers();
				this.screenshotFrameBuffer.clear(true);
				this.screenshotFrameBuffer = null;
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
			updateFramebufferSize();

			lastWidth = getWindow().getGuiScaledWidth();
			lastHeight = getWindow().getGuiScaledHeight();
//		}


	}
	
	public void updateFramebufferSize() {
		if (framebuffer == null)
			return;

		resize(framebuffer);
//		resize(client.worldRenderer.getEntityOutlineFramebuffer());
		resize(client.levelRenderer.entityTarget());
//		resizeMinecraftFramebuffers();

		calculateSize();
	}

	public void resizeMinecraftFramebuffers() {
		initMinecraftFramebuffers();
		minecraftFramebuffers.forEach(this::resize);
	}

	public void calculateSize() {
		currentWidth = framebuffer.width;
		currentHeight = framebuffer.height;

		// Framebuffer uses color (4 x 8 = 32 bit int) and depth (32 bit float)
		estimatedMemory = (long) currentWidth * currentHeight * 8;
	}
	
	public void resize(@Nullable RenderTarget framebuffer) {
		if (framebuffer == null) return;

		boolean prev = shouldScale;
		shouldScale = true;
		if (screenshot) {
			framebuffer.resize(
					getScreenshotWidth(),
					getScreenshotHeight(),
					Minecraft.ON_OSX
			);
		} else {
			framebuffer.resize(
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

	private void setClientFramebuffer(RenderTarget renderTarget) {
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
