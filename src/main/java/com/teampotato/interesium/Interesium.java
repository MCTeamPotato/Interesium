package com.teampotato.interesium;

import com.teampotato.interesium.mixin.InteresiumMixinManager;
import net.minecraftforge.fml.common.Mod;

@Mod(Interesium.MOD_ID)
public final class Interesium {
    public static final String MOD_ID = "interesium";
    public static boolean isResourcefulBeesLoaded;

    public Interesium() {
        isResourcefulBeesLoaded = InteresiumMixinManager.isLoaded("resourcefulbees");
    }
}
