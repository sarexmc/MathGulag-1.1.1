package com.mathgulag.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
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

/** Dibujo del gulag: filtro rojo, venas que laten, pinchos, numeros de sangre y caja de humo. */
public class GulagRenderer {
    private static final int GX = 20, GY = 12;
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

    private static int mixc(int red, int green, float G) {
        return clamp255(lerp(red, green, G));
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

    private static void beginColor(Tesselator tess, VertexFormat.Mode mode) {
        tess.getBuilder().begin(mode, DefaultVertexFormat.POSITION_COLOR);
    }

    private static void prepare() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    /** Elipse con degradado radial (centro c0, borde c1). */
    private static void radial(Matrix4f m, float cx, float cy, float rx, float ry, int[] c0, int[] c1) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, cx, cy, 0f).color(c0[0], c0[1], c0[2], c0[3]).endVertex();
        int n = 24;
        for (int i = 0; i <= n; i++) {
            double ang = i * Math.PI * 2 / n;
            bb.vertex(m, cx + (float) Math.cos(ang) * rx, cy + (float) Math.sin(ang) * ry, 0f)
                    .color(c1[0], c1[1], c1[2], c1[3]).endVertex();
        }
        tess.end();
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
        float ex = (mx - 0.5f) / 0.31f, ey = (my - 0.5f) / 0.35f;
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
        // movimiento sutil
        float breathe = 1f + 0.008f * (float) Math.sin(t * 0.9f);
        float sway = 0.004f * (float) Math.sin(t * 0.3f);
        for (int i = 0; i < px.length; i++) {
            px[i] = 0.5f + (bx[i] - 0.5f) * breathe + sway + 0.0025f * (float) Math.sin(t * 0.6f + ph1[i]);
            py[i] = 0.5f + (by[i] - 0.5f) * breathe + 0.0025f * (float) Math.cos(t * 0.5f + ph2[i]);
        }
        prepare();
        for (int pass = 0; pass < 2; pass++) {
            Tesselator tess = Tesselator.getInstance();
            BufferBuilder bb = tess.getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int e = 0; e < edges.length; e++) {
                int a = edges[e][0], b = edges[e][1];
                float x0 = px[a] * w, y0 = py[a] * h, x1 = px[b] * w, y1 = py[b] * h;
                float mxn = (px[a] + px[b]) / 2f, myn = (py[a] + py[b]) / 2f;
                float wob = 1f + 0.2f * (float) Math.sin(t * 0.8f + e);
                float mx = mxn * w + jx[e] * w * wob;
                float my = myn * h + jy[e] * h * wob;
                float dx = (mxn - 0.5f) * 2f, dy = (myn - 0.5f) * 2f;
                float d = Math.min(1.4f, (float) Math.sqrt(dx * dx + dy * dy));
                float edgeF = 0.45f + d;
                float fade = Math.max(0.25f, Math.min(1f, d * 1.5f));
                float pulse = 1f + 0.25f * beat;
                if (pass == 0) {
                    float wd = Math.max(2f, (4f + 5f * edgeF) * s * pulse);
                    int al = (int) (80 * fade * I * (0.7f + 0.5f * beat));
                    int r = mixc(150, 0, G), gg = mixc(0, 120, G), bl = mixc(5, 40, G);
                    seg(bb, m, x0, y0, mx, my, wd, r, gg, bl, al);
                    seg(bb, m, mx, my, x1, y1, wd, r, gg, bl, al);
                } else {
                    float wd = Math.max(1.1f, (1.3f + 1.7f * edgeF) * s * pulse);
                    int r = clamp255(lerp(255, 70, G) + 30 * beat), gg = mixc(45, 255, G), bl = mixc(45, 110, G);
                    int al = (int) (235 * fade * I);
                    seg(bb, m, x0, y0, mx, my, wd, r, gg, bl, al);
                    seg(bb, m, mx, my, x1, y1, wd * 0.9f, r, gg, bl, al);
                }
            }
            tess.end();
        }
        RenderSystem.enableCull();
    }

    // ------------------------------------------------------------------ pinchos

    private static void spikes(Matrix4f m, float cx, float cy, float R, float t, float seed, float G, float I) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, cx, cy, 0f).color(mixc(190, 25, G), mixc(15, 160, G), mixc(25, 50, G), (int) (235 * I)).endVertex();
        int n = 15;
        float rot = seed + t * 0.04f;
        for (int i = 0; i <= n * 2; i++) {
            int k = i % (n * 2);
            double ang = k * Math.PI / n + rot + 0.14 * (hash(k * 5.3f + seed) - 0.5f);
            boolean tip = k % 2 == 0;
            float rr;
            int r, g2, b, a;
            if (tip) {
                rr = R * (0.55f + 0.45f * hash(k * 3.1f + seed * 7f)) * (1f + 0.04f * (float) Math.sin(t * 1.4f + k + seed));
                r = mixc(235, 70, G);
                g2 = mixc(40, 235, G);
                b = mixc(45, 100, G);
                a = (int) (110 * I);
            } else {
                rr = R * 0.30f;
                r = mixc(120, 0, G);
                g2 = mixc(0, 80, G);
                b = mixc(8, 20, G);
                a = (int) (230 * I);
            }
            bb.vertex(m, cx + (float) Math.cos(ang) * rr, cy + (float) Math.sin(ang) * rr, 0f).color(r, g2, b, a).endVertex();
        }
        tess.end();
    }

    private static void spikeBlob(GuiGraphics g, float cx, float cy, float R, float t, float seed, float beat,
                                  float G, float I, float hover) {
        Matrix4f m = g.pose().last().pose();
        float rr = R * (1f + 0.03f * beat) * (1f + 0.10f * hover);
        radial(m, cx, cy, rr * 1.15f, rr * 1.15f,
                new int[]{mixc(255, 60, G), mixc(40, 255, G), mixc(40, 90, G), (int) (85 * I)},
                new int[]{mixc(255, 60, G), mixc(40, 255, G), mixc(40, 90, G), 0});
        spikes(m, cx, cy, rr, t, seed, G, I);
        radial(m, cx, cy, rr * 0.36f, rr * 0.36f,
                new int[]{mixc(215, 40, G), mixc(25, 200, G), mixc(30, 60, G), (int) (240 * I)},
                new int[]{mixc(105, 0, G), mixc(0, 70, G), mixc(8, 18, G), (int) (240 * I)});
    }

    // ------------------------------------------------------------------ texto de sangre

    private static void bloodText(GuiGraphics g, String text, float cx, float cy, float scale, float G, float I,
                                  float tilt, boolean drips, float t, float seed) {
        Font font = Minecraft.getInstance().font;
        Component c = Component.literal(text).withStyle(ChatFormatting.BOLD);
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(tilt));
        ps.scale(scale, scale, 1f);
        float x = -font.width(c) / 2f;
        int a = (int) (255 * I);
        int outline = argb(a, 255, (int) lerp(225, 255, G), (int) lerp(225, 225, G));
        int shade = argb(a, (int) lerp(90, 0, G), (int) lerp(0, 90, G), 5);
        int fill = argb(a, (int) lerp(235, 60, G), (int) lerp(20, 255, G), (int) lerp(30, 90, G));
        int sheen = argb((int) (90 * I), 255, 220, 220);
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4;
            ps.pushPose();
            ps.translate((float) Math.cos(ang) * 1.25f, (float) Math.sin(ang) * 1.25f, 0f);
            g.drawString(font, c, (int) x, -4, outline, false);
            ps.popPose();
        }
        ps.pushPose();
        ps.translate(0f, 0.7f, 0f);
        g.drawString(font, c, (int) x, -4, shade, false);
        ps.popPose();
        g.drawString(font, c, (int) x, -4, fill, false);
        ps.pushPose();
        ps.translate(-0.4f, -0.5f, 0f);
        g.drawString(font, c, (int) x, -4, sheen, false);
        ps.popPose();
        ps.popPose();

        if (drips) {
            float tw = font.width(c) * scale;
            float y0 = cy + 4.2f * scale;
            for (int j = 0; j < 3; j++) {
                float dx = cx + (hash(seed * 3f + j * 1.7f) - 0.5f) * tw * 0.8f;
                float len = scale * (1.5f + 3.5f * hash(seed * 7f + j)) * (0.65f + 0.35f * (0.5f + 0.5f * (float) Math.sin(t * 0.5f + j * 2f + seed)));
                int wd = Math.max(2, (int) (scale * 0.45f));
                g.fillGradient((int) dx, (int) y0, (int) dx + wd, (int) (y0 + len),
                        argb((int) (235 * I), (int) lerp(215, 40, G), (int) lerp(15, 220, G), 25),
                        argb((int) (90 * I), (int) lerp(150, 0, G), (int) lerp(0, 150, G), 10));
            }
        }
    }

    // ------------------------------------------------------------------ caja de humo

    private static void smokyBox(GuiGraphics g, int x1, int y1, int x2, int y2, float t, float I, float s) {
        Matrix4f m = g.pose().last().pose();
        prepare();
        int[] c0 = {6, 0, 0, (int) (34 * I)};
        int[] c1 = {6, 0, 0, 0};
        for (int i = 0; i < 70; i++) {
            float hx = hash(i * 1.7f), hy = hash(i * 2.9f + 5f), hr = hash(i * 4.1f + 9f);
            float cx = x1 + (x2 - x1) * (0.05f + 0.9f * hx) + 5f * s * (float) Math.sin(t * 0.4f + i);
            float cy = y1 + (y2 - y1) * (0.05f + 0.9f * hy) + 3f * s * (float) Math.cos(t * 0.35f + i * 1.3f);
            radial(m, cx, cy, (60f + 110f * hr) * s, (40f + 70f * hr) * s, c0, c1);
        }
        for (int i = 0; i < 14; i++) {
            float cx = x1 + (x2 - x1) * (0.06f + 0.88f * hash(i * 6.7f + 2f));
            float len = (40f + 90f * hash(i * 3.3f)) * s * (1f + 0.2f * (float) Math.sin(t * 0.6f + i));
            radial(m, cx, y2 - 20f * s + len * 0.3f, 16f * s, len, new int[]{6, 0, 0, (int) (44 * I)}, c1);
        }
        RenderSystem.enableCull();
    }

    // ------------------------------------------------------------------ capas

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

        g.fill(0, 0, w, h, argb((int) (50 * I), (int) lerp(215, 0, G), (int) lerp(0, 175, G), (int) lerp(12, 40, G)));
        int strips = 26;
        float edgeW = w * 0.28f;
        for (int i = 0; i < strips; i++) {
            float f = 1f - i / (float) strips;
            int al = (int) (110 * I * f * f);
            int sw = (int) Math.ceil(edgeW / strips);
            g.fill(i * sw, 0, (i + 1) * sw, h, argb(al, 4, 0, 0));
            g.fill(w - (i + 1) * sw, 0, w - i * sw, h, argb(al, 4, 0, 0));
        }
        g.fillGradient(0, 0, w, (int) (h * 0.28f), argb((int) (120 * I), 4, 0, 0), argb(0, 4, 0, 0));
        g.fillGradient(0, (int) (h * 0.72f), w, h, argb(0, 4, 0, 0), argb((int) (120 * I), 4, 0, 0));
        g.fill(0, 0, w, h, argb((int) (22 * I * beat), (int) lerp(255, 0, G), (int) lerp(0, 255, G), 20));

        drawVeins(g, w, h, t, beat, G, I);

        Font font = Minecraft.getInstance().font;
        float s = h / 1080f;

        int bw = (int) (w * 0.30f);
        int bx0 = (w - bw) / 2, by0 = (int) (h * 0.035f), bh = Math.max(4, (int) (8 * s));
        float frac = Math.max(0f, Math.min(1f, ClientState.ticksLeft / (float) ClientState.ticksTotal));
        g.fill(bx0 - 2, by0 - 2, bx0 + bw + 2, by0 + bh + 2, argb((int) (170 * I), 0, 0, 0));
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

    public static void renderQuestionLayer(GuiGraphics g, int w, int h, int mouseX, int mouseY) {
        float I = Math.max(0.0f, ClientState.intensity);
        float G = ClientState.green;
        float t = time();
        float beat = heartbeat(beatPhase);
        float s = h / 1080f;

        smokyBox(g, (int) (w * 0.36f), (int) (h * 0.16f), (int) (w * 0.64f), (int) (h * 0.48f), t, I, s);

        prepare();
        float R = 108f * s;
        float[][] pos = new float[10][2];
        for (int d = 0; d < 10; d++) {
            pos[d][0] = ClientState.cur[d][0] * w + 3f * s * (float) Math.sin(t * 0.8f + d * 1.9f);
            pos[d][1] = ClientState.cur[d][1] * h + 3f * s * (float) Math.cos(t * 0.7f + d * 2.3f);
            double dx = mouseX - pos[d][0], dy = mouseY - pos[d][1];
            float hover = Math.sqrt(dx * dx + dy * dy) < 60 * s ? 1f : 0f;
            spikeBlob(g, pos[d][0], pos[d][1], R, t, d * 1.37f, beat, G, I, hover);
        }
        RenderSystem.enableCull();
        for (int d = 0; d < 10; d++) {
            bloodText(g, String.valueOf(d), pos[d][0], pos[d][1], 6.2f * s, G, I, (hash(d * 1.37f) - 0.5f) * 10f, false, t, d);
        }

        bloodText(g, ClientState.question, w * 0.5f, h * 0.28f, 11f * s * (1f + 0.02f * beat), G, I, 0f, true, t, 99f);
        if (!ClientState.typed.isEmpty()) {
            bloodText(g, ClientState.typed, w * 0.5f, h * 0.41f, 9f * s, G, I, 0f, true, t, 50f);
        }
    }
}
