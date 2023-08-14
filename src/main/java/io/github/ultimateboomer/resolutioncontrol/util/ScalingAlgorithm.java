package io.github.ultimateboomer.resolutioncontrol.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.opengl.GL11;

public enum ScalingAlgorithm {
    NEAREST(new TranslatableComponent("resolutioncontrol.settings.main.nearest"),
            GL11.GL_NEAREST, GL11.GL_NEAREST_MIPMAP_NEAREST),
    LINEAR(new TranslatableComponent("resolutioncontrol.settings.main.linear"),
            GL11.GL_LINEAR, GL11.GL_LINEAR_MIPMAP_NEAREST);

    private final Component text;
    private final int id;
    private final int idMipped;

    ScalingAlgorithm(Component text, int id, int idMipped) {
        this.text = text;
        this.id = id;
        this.idMipped = idMipped;
    }

    public Component getText() {
        return text;
    }

    public int getId(boolean mipped) {
        return mipped ? idMipped : id;
    }
}
