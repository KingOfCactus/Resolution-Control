package io.github.ultimateboomer.resolutioncontrol.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import io.github.ultimateboomer.resolutioncontrol.util.RCUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
//import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TextComponent;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings("FieldCanBeLocal")
public final class MainSettingsScreen extends SettingsScreen {
//	private static final ResourceLocation backgroundTexture = ResolutionControlMod.identifier("textures/gui/settings.png");

	private static final float[] scaleValues = {0.0f, 0.01f, 0.025f, 0.05f, 0.1f, 0.25f, 0.5f, 0.75f, 1.0f,
			1.25f, 1.5f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f};

	private static final double redValue = 2.0;

	private static final TextComponent increaseText = new TextComponent("+");
	private static final TextComponent decreaseText = new TextComponent("-");
	private static final TextComponent setText = new TextComponent("S");
	private static final TextComponent resetText = new TextComponent("R");
	private static final TextComponent cancelText = new TextComponent("C");

	private Button increaseButton;
	private Button decreaseButton;
	private Button setButton;
	private Button cancelOrResetButton;

	private EditBox entryTextField;

	private Button upscaleAlgoButton;
	private Button downscaleAlgoButton;

	private boolean manualEntry = false;

	public MainSettingsScreen(@Nullable Screen parent) {
		super(text("settings.main"), parent);
	}

	@Override
	protected void init() {
		super.init();

		int buttonSize = 20;
		int buttonOffset = buttonSize / 2;
		int buttonY = centerY + 15 - buttonSize / 2;
		int textFieldSize = 40;

		decreaseButton = new Button(
			centerX - 55 - buttonOffset - buttonSize / 2, buttonY,
			buttonSize, buttonSize,
			decreaseText,
			button -> changeScaleFactor(false));
		addRenderableWidget(decreaseButton);

		increaseButton = new Button(
			centerX - 55 + buttonOffset - buttonSize / 2, buttonY,
			buttonSize, buttonSize,
				increaseText,
			button -> changeScaleFactor(true)
		);
		addRenderableWidget(increaseButton);

		setButton = new Button(
				centerX - 55 - buttonOffset - buttonSize / 2, buttonY + buttonSize,
				buttonSize, buttonSize,
				setText,
				button -> {
					setManualEntry(!manualEntry, false);
				}
		);
		addRenderableWidget(setButton);

		cancelOrResetButton = new Button(
				centerX - 55 - buttonOffset + buttonSize / 2, buttonY + buttonSize,
				buttonSize, buttonSize,
				resetText,
				button -> {
					if (manualEntry) {
						setManualEntry(false, true);
					} else {
						mod.setScaleFactor(1.0f);
						updateButtons();
					}
				}
		);
		addRenderableWidget(cancelOrResetButton);

		entryTextField = new EditBox(font,
				centerX - 55 - textFieldSize / 2, centerY - 36,
				textFieldSize, buttonSize, TextComponent.EMPTY);
		entryTextField.setVisible(false);
		addRenderableWidget(entryTextField);

		upscaleAlgoButton = new Button(
			centerX + 15, centerY - 28,
			60, buttonSize,
			mod.getUpscaleAlgorithm().getText(),
			button -> {
				mod.nextUpscaleAlgorithm();
				button.setMessage(mod.getUpscaleAlgorithm().getText());
			}
		);
		addRenderableWidget(upscaleAlgoButton);

		downscaleAlgoButton = new Button(
				centerX + 15, centerY + 8,
				60, buttonSize,
				mod.getDownscaleAlgorithm().getText(),
				button -> {
					mod.nextDownscaleAlgorithm();
					button.setMessage(mod.getDownscaleAlgorithm().getText());
				}
		);
		addRenderableWidget(downscaleAlgoButton);

		updateButtons();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.manualEntry) {
			if (keyCode == GLFW.GLFW_KEY_ENTER) {
				this.setManualEntry(false, false);
				return true;
			} else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				this.setManualEntry(false, true);
				return true;
			} else {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		} else {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
	}

	@Override
	public void render(PoseStack matrices, int mouseX, int mouseY, float time) {
		super.render(matrices, mouseX, mouseY, time);

		if (!this.manualEntry) {
			drawCenteredString(matrices, String.format("\u00a7%s%s\u00a7rx",
					mod.getScaleFactor() > redValue ? "4" : "0", mod.getScaleFactor()),
					centerX - 55, centerY - 36, 0x000000);

			drawCenteredString(matrices, String.format("\u00a78%sx%s\u00a7r",
					ResolutionControlMod.getInstance().getCurrentWidth(),
					ResolutionControlMod.getInstance().getCurrentHeight()),
					centerX - 55, centerY - 24, 0x000000);

			drawCenteredString(matrices, "\u00a78" + text("settings.main.estimate",
					RCUtil.formatMetric(ResolutionControlMod.getInstance().getEstimatedMemory()) + "B")
							.getString() + "\u00a7r",
					centerX - 55, centerY - 12, 0x000000);
		}

		drawLeftAlignedString(matrices,
				"\u00a78" + text("settings.main.upscale").getString(),
				centerX + 15, centerY - 40, 0x000000);
		drawLeftAlignedString(matrices,
				"\u00a78" + text("settings.main.downscale").getString(),
				centerX + 15, centerY - 5, 0x000000);


	}

	@Override
	public void tick() {
		if (manualEntry) {
			if (!this.entryTextField.isFocused()) {
				entryTextField.setFocus(true);
			}

			if (!entryTextField.active) {
				entryTextField.active = true;
			}
		}

		entryTextField.tick();
		super.tick();
	}

	private void changeScaleFactor(boolean add) {
		float currentScale = mod.getScaleFactor();
		int nextIndex = ArrayUtils.indexOf(scaleValues, currentScale);
		if (nextIndex == -1) {
			for (int i = -1; i < scaleValues.length; ++i) {
				double scale1 = i == -1 ? 0.0 : scaleValues[i];
				double scale2 = i == scaleValues.length - 1 ? Double.POSITIVE_INFINITY : scaleValues[i + 1];

				if (currentScale > scale1 && currentScale < scale2) {
					nextIndex = i + (add ? 1 : 0);
					break;
				}
			}
		} else {
			nextIndex += add ? 1 : -1;
		}

		mod.setScaleFactor(scaleValues[nextIndex]);

		updateButtons();
	}

	private void updateButtons() {
		increaseButton.active = mod.getScaleFactor() < scaleValues[scaleValues.length - 1];
		decreaseButton.active = mod.getScaleFactor() > scaleValues[0];
	}

	public void setManualEntry(boolean manualEntry, boolean cancel) {
		this.manualEntry = manualEntry;
		if (manualEntry) {
			entryTextField.setValue(String.valueOf(mod.getScaleFactor()));
			entryTextField.setVisible(true);
			entryTextField.setCursorPosition(0);
			entryTextField.setCursorPosition(entryTextField.getValue().length());
			entryTextField.active = true;
			cancelOrResetButton.setMessage(cancelText);
			increaseButton.active = false;
			decreaseButton.active = false;
			this.entryTextField.setFocus(true);
		} else {
			if (!cancel) {
				String text = entryTextField.getValue();
				if (NumberUtils.isParsable(text)) {
					float value = Math.abs(Float.parseFloat(text));
					mod.setScaleFactor(value);
				}
			}

			entryTextField.setVisible(false);
			setButton.setMessage(setText);
			cancelOrResetButton.setMessage(resetText);
			increaseButton.active = true;
			decreaseButton.active = true;

			updateButtons();
		}
	}
}
