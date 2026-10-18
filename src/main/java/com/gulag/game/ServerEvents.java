package com.gulag.game;

import com.gulag.GulagMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = GulagMod.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        GulagCommand.register(e.getDispatcher());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && ServerLifecycleHooks.getCurrentServer() != null) {
            GulagManager.tick(ServerLifecycleHooks.getCurrentServer());
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (e.getEntity().level().isClientSide) return;
        if (GulagManager.onAttack(e.getEntity(), e.getTarget())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) GulagManager.onDeath(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) GulagManager.onLogout(sp);
    }
}
