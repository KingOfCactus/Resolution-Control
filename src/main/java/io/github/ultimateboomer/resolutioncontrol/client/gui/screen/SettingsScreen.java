package io.github.ultimateboomer.resolutioncontrol.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.GlStateManager;
import io.github.ultimateboomer.resolutioncontrol.ResolutionControlMod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("StaticInitializerReferencesSubClass")
public class SettingsScreen extends Screen {
    protected static final ResourceLocation backgroundTexture = ResolutionControlMod.identifier("textures/gui/settings.png");

    protected static TranslatableComponent text(String path, Object... args) {
        return new TranslatableComponent(ResolutionControlMod.MOD_ID + "." + path, args);
    }

    protected static final int containerWidth = 192;
    protected static final int containerHeight = 128;

    protected static final Map<Class<? extends SettingsScreen>,
            Function<Screen, SettingsScreen>> screensSupplierList;

    static {
        screensSupplierList = new LinkedHashMap<>();
        screensSupplierList.put(MainSettingsScreen.class, MainSettingsScreen::new);
        screensSupplierList.put(ScreenshotSettingsScreen.class, ScreenshotSettingsScreen::new);
        screensSupplierList.put(InfoSettingsScreen.class, InfoSettingsScreen::new);
    }

    protected final ResolutionControlMod mod = ResolutionControlMod.getInstance();

    @Nullable
    protected final Screen parent;

    protected int centerX;
    protected int centerY;
    protected int startX;
    protected int startY;

    protected Map<Class<? extends SettingsScreen>, Button> menuButtons;

    protected Button doneButton;

    protected SettingsScreen(TranslatableComponent title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        centerX = width / 2;
        centerY = height / 2;
        startX = centerX - containerWidth / 2;
        startY = centerY - containerHeight / 2;

        // Init menu buttons
        menuButtons = new LinkedHashMap<>();
        final int menuButtonWidth = 80;
        final int menuButtonHeight = 20;
        MutableInt o = new MutableInt();

        screensSupplierList.forEach((c, constructor) -> {
            SettingsScreen r = constructor.apply(this.parent);
            Button b = new Button(
                    startX - menuButtonWidth - 20, startY + o.getValue(),
                    menuButtonWidth, menuButtonHeight,
                    r.getTitle(),
                    button -> {
                        if (minecraft != null) {
                            minecraft.setScreen(constructor.apply(this.parent));
                        }
                    }
            );

            if (this.getClass().equals(c))
                b.active = false;

            menuButtons.put(c, b);
            o.add(25);
        });

        menuButtons.values().forEach(this::addWidget);

        doneButton = new Button(
                centerX + 15, startY + containerHeight - 30,
                60, 20,
                new TranslatableComponent("gui.done"),
                button -> {
                    applySettingsAndCleanup();
                    if (minecraft != null) {
                        minecraft.setScreen(this.parent);
                    }
                }
        );
        addWidget(doneButton);
    }

    @Override
    public void render(@Nonnull PoseStack matrices, int mouseX, int mouseY, float delta) {
    //  if (minecraft != null && minecraft.world == null) {
        if (minecraft != null && minecraft.player.isAddedToWorld()) {
            renderBackground(matrices, 0);
        }

    //  GlStateManager.enableAlphaTest();
        minecraft.getTextureManager().bindForSetup(backgroundTexture);
    //  GlStateManager.color4f(1, 1, 1, 1);

        int textureWidth = 256;
        int textureHeight = 192;
        blit(
                matrices,
                centerX - textureWidth / 2, centerY - textureHeight / 2,
                0, 0,
                textureWidth, textureHeight
        );

        super.render(matrices, mouseX, mouseY, delta);

        drawLeftAlignedString(matrices, "\u00a7r" + getTitle().getString(),
                centerX + 15, startY + 10, 0x000000);

        drawRightAlignedString(matrices, text("settings.title").getString(),
                centerX + 5, startY + 10, 0x404040);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((ResolutionControlMod.getInstance().getSettingsKey().matches(keyCode, scanCode))) {
            this.applySettingsAndCleanup();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
            this.minecraft.mouseHandler.grabMouse();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void onClose() {
        this.applySettingsAndCleanup();
        super.onClose();
    }

    protected void applySettingsAndCleanup() {
        mod.saveSettings();
        mod.setLastSettingsScreen(this.getClass());
    };

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    protected void drawCenteredString(PoseStack matrices, String text, float x, float y, int color) {
        font.draw(matrices, text, x - font.width(text) / 2, y, color);
    }

    protected void drawLeftAlignedString(PoseStack matrices, String text, float x, float y, int color) {
        font.draw(matrices, text, x, y, color);
    }

    protected void drawRightAlignedString(PoseStack matrices, String text, float x, float y, int color) {
        font.draw(matrices, text, x - font.width(text), y, color);
    }

    public static SettingsScreen getScreen(Class<? extends SettingsScreen> screenClass) {
        return screensSupplierList.get(screenClass).apply(null);
    }

    protected static Component getStateText(boolean enabled) {
        return enabled ? new TranslatableComponent("addServer.resourcePack.enabled")
                : new TranslatableComponent("addServer.resourcePack.disabled");
    }
}
