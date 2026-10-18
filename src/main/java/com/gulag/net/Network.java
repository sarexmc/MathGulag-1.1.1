package com.gulag.net;

import com.gulag.GulagMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class Network {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GulagMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, StatePacket.class, StatePacket::encode, StatePacket::decode, StatePacket::handle);
        CHANNEL.registerMessage(id++, AnswerPacket.class, AnswerPacket::encode, AnswerPacket::decode, AnswerPacket::handle);
    }
}
