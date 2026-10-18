package com.gulag.client;

import com.gulag.GulagMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GulagMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("gulag_overlay", (gui, g, partialTick, w, h) -> GulagRenderer.renderOverlay(g, partialTick, w, h));
    }
}
