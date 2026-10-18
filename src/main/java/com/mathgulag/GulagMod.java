package com.mathgulag;

import com.mathgulag.net.Network;
import net.minecraftforge.fml.common.Mod;

@Mod(GulagMod.MODID)
public class GulagMod {
    public static final String MODID = "mathgulag";

    public GulagMod() {
        Network.register();
    }
}
