package com.gulag.net;

import java.util.function.Supplier;
import com.gulag.game.GulagManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record AnswerPacket(int digit) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(digit);
    }

    public static AnswerPacket decode(FriendlyByteBuf buf) {
        return new AnswerPacket(buf.readVarInt());
    }

    public static void handle(AnswerPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) GulagManager.onAnswer(sender, msg.digit());
        });
        ctx.get().setPacketHandled(true);
    }
}
