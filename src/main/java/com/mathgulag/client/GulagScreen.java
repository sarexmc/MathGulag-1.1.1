package com.mathgulag.client;

import com.mathgulag.net.AnswerPacket;
import com.mathgulag.net.Network;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Pantalla transparente con la pregunta y los numeros clicables. */
public class GulagScreen extends Screen {
    public GulagScreen() {
        super(Component.literal("Gulag"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GulagRenderer.renderQuestionLayer(g, width, height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        float s = height / 1080f;
        for (int d = 0; d < 10; d++) {
            double dx = mx - ClientState.cur[d][0] * width;
            double dy = my - ClientState.cur[d][1] * height;
            if (Math.sqrt(dx * dx + dy * dy) < 72 * s) {
                answer(d);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= 48 && key <= 57) {
            answer(key - 48);
            return true;
        }
        if (key >= 320 && key <= 329) {
            answer(key - 320);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void answer(int d) {
        if (ClientState.typed.length() >= 3) return;
        ClientState.typed += d;
        Network.CHANNEL.sendToServer(new AnswerPacket(d));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
