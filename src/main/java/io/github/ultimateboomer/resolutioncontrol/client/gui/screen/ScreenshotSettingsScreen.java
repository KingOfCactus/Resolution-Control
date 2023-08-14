package io.github.ultimateboomer.resolutioncontrol.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ultimateboomer.resolutioncontrol.util.RCUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nullable;

public class ScreenshotSettingsScreen extends SettingsScreen {
//    private static final double[] scaleValues = {0.1, 0.25, 0.5, 1.0,
//            2.0, 3.0, 4.0, 6.0, 8.0, 16.0};

    private static final Component increaseText = new TextComponent("x2");
    private static final Component decreaseText = new TextComponent("/2");
    private static final Component resetText = new TextComponent("R");

    private EditBox widthTextField;
    private EditBox heightTextField;

    private Button increaseButton;
    private Button decreaseButton;
    private Button resetButton;

    private Button toggleOverrideSizeButton;
    private Button toggleAlwaysAllocatedButton;

    private final int buttonSize = 20;
    private final int textFieldSize = 40;

    private long estimatedSize;

    public ScreenshotSettingsScreen(@Nullable Screen parent) {
        super(text("settings.screenshot"), parent);
    }

    @Override
    protected void init() {
        super.init();

        toggleOverrideSizeButton = new Button(
                centerX + 20, centerY - 40,
                50, 20,
                getStateText(mod.getOverrideScreenshotScale()),
                button -> {
                    mod.setOverrideScreenshotScale(!mod.getOverrideScreenshotScale());
                    button.setMessage(getStateText(mod.getOverrideScreenshotScale()));
                }
        );
        addRenderableWidget(toggleOverrideSizeButton);

        toggleAlwaysAllocatedButton = new Button(
                centerX + 20, centerY - 20,
                50, 20,
                getStateText(mod.isScreenshotFramebufferAlwaysAllocated()),
                button -> {
                    mod.setScreenshotFramebufferAlwaysAllocated(!mod.isScreenshotFramebufferAlwaysAllocated());
                    button.setMessage(getStateText(mod.isScreenshotFramebufferAlwaysAllocated()));
                }
        );
        addRenderableWidget(toggleAlwaysAllocatedButton);

        widthTextField = new EditBox(font,
                centerX - 85, centerY + 7,
                textFieldSize, buttonSize,
                TextComponent.EMPTY);
        widthTextField.setValue(String.valueOf(mod.getScreenshotWidth()));
        addRenderableWidget(widthTextField);

        heightTextField = new EditBox(font,
                centerX - 35, centerY + 7,
                textFieldSize, buttonSize,
                TextComponent.EMPTY);
        heightTextField.setValue(String.valueOf(mod.getScreenshotHeight()));
        addRenderableWidget(heightTextField);

        increaseButton = new Button(
                centerX - 10 - 60, centerY + 35,
                20, 20,
                increaseText,
                button -> multiply(2.0));
        addRenderableWidget(increaseButton);

        decreaseButton = new Button(
                centerX + 10 - 60, centerY + 35,
                20, 20,
                decreaseText,
                button -> multiply(0.5));
        addRenderableWidget(decreaseButton);

        resetButton = new Button(
                centerX + 30 - 60, centerY + 35,
                20, 20,
                resetText,
                button -> resetSize());
        addRenderableWidget(resetButton);

        calculateSize();
    }

    @Override
    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        drawLeftAlignedString(matrices,
                "\u00a78" + text("settings.screenshot.overrideSize").getString(),
                centerX - 75, centerY - 35,
                0x000000);

        drawLeftAlignedString(matrices,
                "\u00a78" + text("settings.screenshot.alwaysAllocated").getString(),
                centerX - 75, centerY - 15,
                0x000000);

        drawLeftAlignedString(matrices,
                "\u00a78x",
                centerX - 42.5f, centerY + 12,
                0x000000);

        drawLeftAlignedString(matrices,
                "\u00a78" + text("settings.main.estimate").getString()
                        + " " + RCUtil.formatMetric(estimatedSize) + "B",
                centerX + 25, centerY + 12,
                0x000000);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        calculateSize();
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        widthTextField.tick();
        heightTextField.tick();
        super.tick();
    }

    @Override
    protected void applySettingsAndCleanup() {
        if (NumberUtils.isParsable(widthTextField.getValue())
                && NumberUtils.isParsable(heightTextField.getValue())) {
            int newWidth = (int) Math.abs(Double.parseDouble(widthTextField.getValue()));
            int newHeight = (int) Math.abs(Double.parseDouble(heightTextField.getValue()));

            if (newWidth != mod.getScreenshotWidth() || newHeight != mod.getScreenshotHeight()) {
                mod.setScreenshotWidth(newWidth);
                mod.setScreenshotHeight(newHeight);

                if (mod.isScreenshotFramebufferAlwaysAllocated()) {
                    mod.initScreenshotRenderTarget();
                }
            }
        }
        super.applySettingsAndCleanup();
    }

    private void multiply(double mul) {
        if (NumberUtils.isParsable(widthTextField.getValue())
                && NumberUtils.isParsable(heightTextField.getValue())) {
            widthTextField.setValue(String.valueOf(
                    (int) Math.abs(Double.parseDouble(widthTextField.getValue()) * mul)));
            heightTextField.setValue(String.valueOf(
                    (int) Math.abs(Double.parseDouble(heightTextField.getValue()) * mul)));
            calculateSize();
        }
    }

    private void resetSize() {
        mod.setScreenshotWidth(3840);
        mod.setScreenshotHeight(2160);
        widthTextField.setValue(String.valueOf(mod.getScreenshotWidth()));
        heightTextField.setValue(String.valueOf(mod.getScreenshotHeight()));
    }

    private void calculateSize() {
        if (NumberUtils.isParsable(widthTextField.getValue())
                && NumberUtils.isParsable(heightTextField.getValue())) {
            estimatedSize = (long) (Double.parseDouble(widthTextField.getValue())
                    * Double.parseDouble(heightTextField.getValue()) * 8);
        }
    }
}
