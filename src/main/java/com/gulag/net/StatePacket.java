package com.gulag.net;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** phase: 0 nada, 1 pregunta (portador), 2 pasar bomba (portador), 3 espectador */
public record StatePacket(int phase, String question, int ticksLeft, int ticksTotal, String holder) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(phase);
        buf.writeUtf(question);
        buf.writeVarInt(ticksLeft);
        buf.writeVarInt(ticksTotal);
        buf.writeUtf(holder);
    }

    public static StatePacket decode(FriendlyByteBuf buf) {
        return new StatePacket(buf.readVarInt(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readUtf());
    }

    public static void handle(StatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.gulag.client.ClientState.apply(msg)));
        ctx.get().setPacketHandled(true);
    }
}
