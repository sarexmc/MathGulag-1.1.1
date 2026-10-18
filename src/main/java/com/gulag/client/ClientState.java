package com.gulag.client;

import com.gulag.net.StatePacket;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Estado del cliente (solo se carga en el cliente). */
public class ClientState {
    public static int phase, ticksLeft, ticksTotal = 1, tickCount, nextShuffle;
    public static String question = "", holder = "";
    public static float green, intensity;

    /** Posiciones de los pinchos tal como aparecen en la captura (1920x1080), en orden de casilla. */
    public static final float[][] SLOT = {
            {520 / 1920f, 405 / 1080f}, {1405 / 1920f, 400 / 1080f}, {245 / 1920f, 600 / 1080f},
            {1570 / 1920f, 610 / 1080f}, {1030 / 1920f, 755 / 1080f}, {1345 / 1920f, 755 / 1080f},
            {725 / 1920f, 870 / 1080f}, {1690 / 1920f, 810 / 1080f}, {420 / 1920f, 925 / 1080f},
            {1500 / 1920f, 930 / 1080f}};
    /** Digito que ocupa cada casilla al inicio: 5,3,4,1,9,2,0,8,6,7 (igual que la captura). */
    private static final int[] SLOT_DIGIT = {5, 3, 4, 1, 9, 2, 0, 8, 6, 7};

    /** Posicion actual (animada) de cada digito. */
    public static final float[][] cur = new float[10][2];
    /** Casilla objetivo de cada digito. */
    public static final int[] perm = new int[10];
    private static final Random RNG = new Random();

    public static void apply(StatePacket p) {
        int old = phase;
        phase = p.phase();
        question = p.question();
        holder = p.holder();
        ticksLeft = p.ticksLeft();
        ticksTotal = Math.max(1, p.ticksTotal());
        if (old == 0 && phase != 0) {
            for (int s = 0; s < 10; s++) perm[SLOT_DIGIT[s]] = s;
            for (int d = 0; d < 10; d++) {
                cur[d][0] = SLOT[perm[d]][0];
                cur[d][1] = SLOT[perm[d]][1];
            }
            nextShuffle = tickCount + 100;
            play(SoundEvents.WARDEN_HEARTBEAT, 1.0f, 1.0f);
            play(SoundEvents.SKELETON_HURT, 0.5f, 1.0f);
            play(SoundEvents.SLIME_SQUISH, 0.4f, 1.0f);
        } else if (phase == 2 && old != 2) {
            play(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.0f);
        } else if (phase == 0 && old != 0) {
            play(SoundEvents.HONEY_BLOCK_SLIDE, 0.5f, 1.0f);
            play(SoundEvents.SLIME_SQUISH, 0.6f, 1.0f);
        }
    }

    private static void shuffle() {
        int[] slots = new int[10];
        for (int i = 0; i < 10; i++) slots[i] = i;
        for (int i = 9; i > 0; i--) {
            int j = RNG.nextInt(i + 1);
            int t = slots[i];
            slots[i] = slots[j];
            slots[j] = t;
        }
        System.arraycopy(slots, 0, perm, 0, 10);
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        tickCount++;
        if (mc.level == null || mc.player == null) {
            phase = 0;
            intensity = 0;
            green = 0;
            return;
        }
        intensity += ((phase != 0 ? 1f : 0f) - intensity) * 0.12f;
        if (intensity < 0.005f && phase == 0) intensity = 0;
        green += ((phase == 2 ? 1f : 0f) - green) * 0.18f;
        if (phase != 0 && ticksLeft > 0) ticksLeft--;
        if (phase == 1 && tickCount >= nextShuffle) {
            shuffle();
            nextShuffle = tickCount + 70;
        }
        for (int d = 0; d < 10; d++) {
            cur[d][0] += (SLOT[perm[d]][0] - cur[d][0]) * 0.14f;
            cur[d][1] += (SLOT[perm[d]][1] - cur[d][1]) * 0.14f;
        }
        if (phase == 1 && mc.screen == null) {
            mc.setScreen(new GulagScreen());
        } else if (phase != 1 && mc.screen instanceof GulagScreen) {
            mc.setScreen(null);
        }
    }

    private static void play(SoundEvent s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }
}
