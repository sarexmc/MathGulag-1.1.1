package com.gulag;

import com.gulag.net.Network;
import net.minecraftforge.fml.common.Mod;

@Mod(GulagMod.MODID)
public class GulagMod {
    public static final String MODID = "gulag";

    public GulagMod() {
        Network.register();
    }
}
