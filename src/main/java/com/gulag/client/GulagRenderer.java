package com.gulag.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/** Todo el dibujo del gulag: filtro rojo, venas que laten, pinchos, numeros y caja negra. */
public class GulagRenderer {
    private static final int GX = 15, GY = 9;
    private static float[] bx, by, ph1, ph2, px, py, jx, jy;
    private static int[][] edges;
    private static float beatPhase, lastT;

    // ------------------------------------------------------------------ utilidades

    private static float time() {
        return (System.currentTimeMillis() % 3600000L) / 1000f;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int clamp255(float v) {
        return Math.max(0, Math.min(255, (int) v));
    }

    private static int argb(int a, int r, int g, int b) {
        return (Math.max(0, Math.min(255, a)) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    private static float hash(float x) {
        float s = (float) Math.sin(x * 12.9898f) * 43758.5453f;
        return s - (float) Math.floor(s);
    }

    private static float heartbeat(float phase) {
        float c = phase % 1f;
        float a = (float) Math.exp(-Math.pow((c - 0.08f) / 0.05f, 2));
        float b = 0.7f * (float) Math.exp(-Math.pow((c - 0.30f) / 0.06f, 2));
        return Math.max(a, b);
    }

    // ------------------------------------------------------------------ red de venas

    private static void init() {
        if (bx != null) return;
        Random r = new Random(1337);
        int n = GX * GY;
        bx = new float[n];
        by = new float[n];
        ph1 = new float[n];
        ph2 = new float[n];
        px = new float[n];
        py = new float[n];
        for (int i = 0; i < GX; i++) {
            for (int j = 0; j < GY; j++) {
                int k = i * GY + j;
                bx[k] = (i + 0.5f + (r.nextFloat() - 0.5f) * 0.95f) / GX;
                by[k] = (j + 0.5f + (r.nextFloat() - 0.5f) * 0.95f) / GY;
                ph1[k] = r.nextFloat() * 6.28f;
                ph2[k] = r.nextFloat() * 6.28f;
            }
        }
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i < GX; i++) {
            for (int j = 0; j < GY; j++) {
                int a = i * GY + j;
                if (i + 1 < GX) tryEdge(list, r, a, (i + 1) * GY + j, 0.93f);
                if (j + 1 < GY) tryEdge(list, r, a, i * GY + j + 1, 0.93f);
                if (i + 1 < GX && j + 1 < GY && r.nextFloat() < 0.3f) tryEdge(list, r, a, (i + 1) * GY + j + 1, 1f);
                if (i + 1 < GX && j > 0 && r.nextFloat() < 0.3f) tryEdge(list, r, a, (i + 1) * GY + j - 1, 1f);
            }
        }
        edges = list.toArray(new int[0][]);
        jx = new float[edges.length];
        jy = new float[edges.length];
        for (int e = 0; e < edges.length; e++) {
            jx[e] = (r.nextFloat() - 0.5f) * 0.03f;
            jy[e] = (r.nextFloat() - 0.5f) * 0.045f;
        }
    }

    private static void tryEdge(List<int[]> list, Random r, int a, int b, float prob) {
        if (r.nextFloat() > prob) return;
        float mx = (bx[a] + bx[b]) / 2f, my = (by[a] + by[b]) / 2f;
        float ex = (mx - 0.5f) / 0.30f, ey = (my - 0.5f) / 0.33f;
        if (ex * ex + ey * ey < 0.85f + r.nextFloat() * 0.3f) return; // centro libre
        list.add(new int[]{a, b});
    }

    private static void seg(BufferBuilder bb, Matrix4f m, float x0, float y0, float x1, float y1, float wd,
                            int r, int g, int b, int a) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;
        float nx = -dy / len * wd * 0.5f, ny = dx / len * wd * 0.5f;
        bb.vertex(m, x0 + nx, y0 + ny, 0f).color(r, g, b, a).endVertex();
        bb.vertex(m, x1 + nx, y1 + ny, 0f).color(r, g, b, a).endVertex();
        bb.vertex(m, x1 - nx, y1 - ny, 0f).color(r, g, b, a).endVertex();
        bb.vertex(m, x0 - nx, y0 - ny, 0f).color(r, g, b, a).endVertex();
    }

    private static void drawVeins(GuiGraphics g, int w, int h, float t, float beat, float G, float I) {
        init();
        Matrix4f m = g.pose().last().pose();
        float s = h / 1080f;
        float breathe = 1f + 0.022f * (float) Math.sin(t * 1.3f);
        float sway = 0.014f * (float) Math.sin(t * 0.45f); // izquierda <-> derecha
        for (int i = 0; i < px.length; i++) {
            px[i] = 0.5f + (bx[i] - 0.5f) * breathe + sway + 0.007f * (float) Math.sin(t * 0.9f + ph1[i]);
            py[i] = 0.5f + (by[i] - 0.5f) * breathe + 0.007f * (float) Math.cos(t * 0.7f + ph2[i]);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for (int pass = 0; pass < 2; pass++) {
            Tesselator tess = Tesselator.getInstance();
            BufferBuilder bb = tess.getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int e = 0; e < edges.length; e++) {
                int a = edges[e][0], b = edges[e][1];
                float x0 = px[a] * w, y0 = py[a] * h, x1 = px[b] * w, y1 = py[b] * h;
                float mxn = (px[a] + px[b]) / 2f, myn = (py[a] + py[b]) / 2f;
                float wob = 1f + 0.5f * (float) Math.sin(t * 1.1f + e);
                float mx = mxn * w + jx[e] * w * wob;
                float my = myn * h + jy[e] * h * wob;
                float dx = (mxn - 0.5f) * 2f, dy = (myn - 0.5f) * 2f;
                float d = Math.min(1.4f, (float) Math.sqrt(dx * dx + dy * dy));
                float edgeF = 0.45f + d;
                float fade = Math.max(0.25f, Math.min(1f, d * 1.5f));
                float pulse = 1f + 0.45f * beat;
                if (pass == 0) {
                    float wd = (7f + 9f * edgeF) * s * pulse;
                    int r = clamp255(lerp(150, 0, G)), gg = clamp255(lerp(0, 120, G)), bl = clamp255(lerp(5, 40, G));
                    int al = (int) (110 * fade * I * (0.7f + 0.5f * beat));
                    seg(bb, m, x0, y0, mx, my, wd, r, gg, bl, al);
                    seg(bb, m, mx, my, x1, y1, wd, r, gg, bl, al);
                } else {
                    float wd = (2.4f + 3.2f * edgeF) * s * pulse;
                    int r = clamp255(lerp(255, 70, G) + 40 * beat), gg = clamp255(lerp(45, 255, G)),
                            bl = clamp255(lerp(45, 110, G));
                    int al = (int) (245 * fade * I);
                    seg(bb, m, x0, y0, mx, my, wd, r, gg, bl, al);
                    seg(bb, m, mx, my, x1, y1, wd * 0.9f, r, gg, bl, al);
                }
            }
            tess.end();
        }
        RenderSystem.enableCull();
    }

    // ------------------------------------------------------------------ pinchos

    private static void fan(Matrix4f m, float cx, float cy, float R, float t, float seed, float beat,
                            int[] cCenter, int[] cTip, int[] cValley, float valleyR, float tipJitter) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, cx, cy, 0f).color(cCenter[0], cCenter[1], cCenter[2], cCenter[3]).endVertex();
        int n = 18;
        float rot = t * 0.25f * (hash(seed) > 0.5f ? 1 : -1) + seed;
        for (int i = 0; i <= n * 2; i++) {
            int k = i % (n * 2);
            double ang = k * Math.PI / n + rot;
            boolean tip = k % 2 == 0;
            float rr;
            int[] c;
            if (tip) {
                float jit = 0.8f + tipJitter * hash(k * 3.1f + seed * 7f);
                rr = R * jit * (1f + 0.10f * (float) Math.sin(t * 3f + k * 1.7f + seed) + 0.08f * beat);
                c = cTip;
            } else {
                rr = R * valleyR;
                c = cValley;
            }
            bb.vertex(m, cx + (float) Math.cos(ang) * rr, cy + (float) Math.sin(ang) * rr, 0f)
                    .color(c[0], c[1], c[2], c[3]).endVertex();
        }
        tess.end();
    }

    private static void spikeBlob(GuiGraphics g, float cx, float cy, float R, float t, float seed, float beat,
                                  float G, float I, float hover) {
        Matrix4f m = g.pose().last().pose();
        float rr = R * (1f + 0.04f * beat) * (1f + 0.12f * hover);
        int a = (int) (255 * I);
        // halo
        fan(m, cx, cy, rr * 1.35f, t, seed, beat,
                new int[]{clamp255(lerp(255, 60, G)), clamp255(lerp(40, 255, G)), clamp255(lerp(40, 90, G)), (int) (120 * I)},
                new int[]{clamp255(lerp(200, 0, G)), clamp255(lerp(0, 200, G)), clamp255(lerp(0, 50, G)), 0},
                new int[]{clamp255(lerp(200, 0, G)), clamp255(lerp(0, 200, G)), clamp255(lerp(0, 50, G)), (int) (70 * I)},
                0.55f, 0.35f);
        // estrella solida con pinchos
        fan(m, cx, cy, rr, t, seed, beat,
                new int[]{clamp255(lerp(255, 120, G)), clamp255(lerp(90, 255, G)), clamp255(lerp(80, 140, G)), a},
                new int[]{clamp255(lerp(165, 0, G)), clamp255(lerp(0, 150, G)), clamp255(lerp(8, 30, G)), a},
                new int[]{clamp255(lerp(235, 40, G)), clamp255(lerp(35, 235, G)), clamp255(lerp(35, 80, G)), a},
                0.52f, 0.35f);
        // disco interior brillante
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, cx - rr * 0.1f, cy - rr * 0.12f, 0f)
                .color(clamp255(lerp(255, 150, G)), clamp255(lerp(130, 255, G)), clamp255(lerp(120, 170, G)), a).endVertex();
        for (int i = 0; i <= 28; i++) {
            double ang = i * Math.PI * 2 / 28;
            bb.vertex(m, cx + (float) Math.cos(ang) * rr * 0.5f, cy + (float) Math.sin(ang) * rr * 0.5f, 0f)
                    .color(clamp255(lerp(190, 10, G)), clamp255(lerp(10, 170, G)), clamp255(lerp(15, 40, G)), a).endVertex();
        }
        tess.end();
    }

    // ------------------------------------------------------------------ texto grande

    private static void bigText(GuiGraphics g, String text, float cx, float cy, float scale, float G, float I) {
        Font font = Minecraft.getInstance().font;
        Component c = Component.literal(text).withStyle(ChatFormatting.BOLD);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 0);
        ps.scale(scale, scale, 1f);
        float x = -font.width(c) / 2f;
        int a = (int) (255 * I);
        int dark = argb(a, (int) lerp(45, 0, G), (int) lerp(0, 45, G), 0);
        int fill = argb(a, (int) lerp(255, 60, G), (int) lerp(35, 255, G), (int) lerp(40, 90, G));
        int sheen = argb((int) (110 * I), 255, 235, 235);
        // contorno oscuro
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4;
            ps.pushPose();
            ps.translate((float) Math.cos(ang) * 1.3f, (float) Math.sin(ang) * 1.3f + 0.8f, 0f);
            g.drawString(font, c, (int) x, -4, dark, false);
            ps.popPose();
        }
        g.drawString(font, c, (int) x, -4, fill, false);
        ps.pushPose();
        ps.translate(-0.45f, -0.45f, 0f);
        g.drawString(font, c, (int) x, -4, sheen, false);
        ps.popPose();
        ps.popPose();
    }

    // ------------------------------------------------------------------ caja negra humeante

    private static void smokyBox(GuiGraphics g, int x1, int y1, int x2, int y2, float t, float I) {
        for (int k = 7; k >= 1; k--) {
            int e = k * 8;
            g.fill(x1 - e, y1 - e, x2 + e, y2 + e, argb((int) (15 * I), 0, 0, 0));
        }
        g.fill(x1, y1, x2, y2, argb((int) (205 * I), 8, 0, 0));
        int cols = 36;
        float cw = (x2 - x1) / (float) cols;
        for (int i = 0; i < cols; i++) {
            float len = (14 + hash(i * 7.3f) * 80) * (1f + 0.25f * (float) Math.sin(t * 1.4f + i));
            int cx1 = (int) (x1 + i * cw);
            g.fillGradient(cx1, y2, (int) (cx1 + cw) + 1, (int) (y2 + len), argb((int) (195 * I), 8, 0, 0), argb(0, 8, 0, 0));
        }
    }

    // ------------------------------------------------------------------ capas

    /** HUD permanente: filtro, venas, mensaje sobre la hotbar y barra de tiempo. */
    public static void renderOverlay(GuiGraphics g, float partialTick, int w, int h) {
        float I = ClientState.intensity;
        if (I < 0.01f) return;
        float G = ClientState.green;
        float t = time();
        float dt = Math.min(0.1f, Math.max(0f, t - lastT));
        lastT = t;
        float urgency = 1f - Math.max(0f, Math.min(1f, ClientState.ticksLeft / (float) ClientState.ticksTotal));
        beatPhase += dt * (1.1f + 1.6f * urgency * (ClientState.phase == 3 ? 0.5f : 1f));
        float beat = heartbeat(beatPhase);

        // filtro de color (el centro queda semitransparente)
        g.fill(0, 0, w, h, argb((int) (62 * I), (int) lerp(215, 0, G), (int) lerp(0, 175, G), (int) lerp(12, 40, G)));
        // oscuridad en los bordes (fondo de las venas negro)
        int strips = 26;
        float edgeW = w * 0.30f;
        for (int i = 0; i < strips; i++) {
            float f = 1f - i / (float) strips;
            int al = (int) (150 * I * f * f);
            int sw = (int) Math.ceil(edgeW / strips);
            g.fill(i * sw, 0, (i + 1) * sw, h, argb(al, 4, 0, 0));
            g.fill(w - (i + 1) * sw, 0, w - i * sw, h, argb(al, 4, 0, 0));
        }
        g.fillGradient(0, 0, w, (int) (h * 0.30f), argb((int) (170 * I), 4, 0, 0), argb(0, 4, 0, 0));
        g.fillGradient(0, (int) (h * 0.70f), w, h, argb(0, 4, 0, 0), argb((int) (170 * I), 4, 0, 0));
        // pulso en toda la pantalla
        g.fill(0, 0, w, h, argb((int) (26 * I * beat), (int) lerp(255, 0, G), (int) lerp(0, 255, G), 20));

        drawVeins(g, w, h, t, beat, G, I);

        Font font = Minecraft.getInstance().font;
        float s = h / 1080f;

        // barra de tiempo
        int bw = (int) (w * 0.30f);
        int bx0 = (w - bw) / 2, by0 = (int) (h * 0.035f), bh = Math.max(4, (int) (8 * s));
        float frac = Math.max(0f, Math.min(1f, ClientState.ticksLeft / (float) ClientState.ticksTotal));
        g.fill(bx0 - 2, by0 - 2, bx0 + bw + 2, by0 + bh + 2, argb((int) (200 * I), 0, 0, 0));
        g.fill(bx0, by0, bx0 + (int) (bw * frac), by0 + bh,
                argb((int) (255 * I), (int) lerp(255, 60, G), (int) lerp(30, 255, G), (int) lerp(30, 90, G)));
        String label = ClientState.phase == 3
                ? "BOMBA: " + ClientState.holder
                : String.format("TNT  %.1fs", ClientState.ticksLeft / 20f);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(w / 2f, by0 + bh + 8 * s, 0);
        ps.scale(1.5f * Math.max(0.7f, s), 1.5f * Math.max(0.7f, s), 1f);
        g.drawCenteredString(font, Component.literal(label).withStyle(ChatFormatting.BOLD), 0, 0,
                argb((int) (255 * I), 255, 235, 235));
        ps.popPose();

        // mensaje encima de la hotbar
        if (ClientState.phase == 2) {
            float pop = 1f + 0.06f * beat;
            ps.pushPose();
            ps.translate(w / 2f, h - 86 * Math.max(0.8f, s), 0);
            ps.scale(1.9f * pop, 1.9f * pop, 1f);
            g.drawCenteredString(font,
                    Component.literal("\u00a1Golpea a otro jugador para pasar la bomba!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    0, 0, 0xFF55FF55);
            ps.popPose();
        }
    }

    /** Capa de la pregunta (solo la ve quien tiene la bomba, dentro de GulagScreen). */
    public static void renderQuestionLayer(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
        float I = Math.max(0.0f, ClientState.intensity);
        float G = ClientState.green;
        float t = time();
        float beat = heartbeat(beatPhase);
        float s = h / 1080f;

        // caja negra detras de la pregunta
        smokyBox(g, (int) (w * 0.347f), (int) (h * 0.09f), (int) (w * 0.656f), (int) (h * 0.50f), t, I);

        // pinchos con numeros
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        float R = 80f * s;
        float[][] pos = new float[10][2];
        for (int d = 0; d < 10; d++) {
            pos[d][0] = ClientState.cur[d][0] * w + 7f * s * (float) Math.sin(t * 1.3f + d * 1.9f);
            pos[d][1] = ClientState.cur[d][1] * h + 7f * s * (float) Math.cos(t * 1.1f + d * 2.3f);
            double dx = mouseX - pos[d][0], dy = mouseY - pos[d][1];
            float hover = Math.sqrt(dx * dx + dy * dy) < 72 * s ? 1f : 0f;
            spikeBlob(g, pos[d][0], pos[d][1], R, t, d * 1.37f, beat, G, I, hover);
        }
        RenderSystem.enableCull();
        for (int d = 0; d < 10; d++) {
            bigText(g, String.valueOf(d), pos[d][0], pos[d][1], 4.6f * s, G, I);
        }

        // pregunta
        bigText(g, ClientState.question, w * 0.5f, h * 0.285f + 5f * s * (float) Math.sin(t * 1.2f),
                13.5f * s * (1f + 0.025f * beat), G, I);
    }
}
