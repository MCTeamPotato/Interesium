package com.teampotato.interesium.mixin.mods.mca;

import com.teampotato.interesium.compat.MCARenderCache;
import forge.net.mca.Config;
import forge.net.mca.MCA;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(value = MCA.class, remap = false)
public abstract class MCAMixin {
    @Shadow public static boolean doesModExist(String modId) {
        return ThreadLocalRandom.current().nextBoolean();
    }

    @Shadow public static boolean areShadersAllowed(String key) {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * @author Kasualix
     * @reason cache isPlayerRendererAllowed.
     */
    @Overwrite
    public static boolean isPlayerRendererAllowed() {
        if (MCARenderCache.isPlayerRendererAllowed == null) MCARenderCache.isPlayerRendererAllowed = Config.getInstance().enableVillagerPlayerModel && Config.getInstance().playerRendererBlacklist.entrySet().stream().filter((entry) -> entry.getValue().equals("all") || entry.getValue().equals("block_player")).noneMatch((entry) -> doesModExist(entry.getKey()));
        return MCARenderCache.isPlayerRendererAllowed;
    }

    /**
     * @author Kasualix
     * @reason cache isVillagerRendererAllowed;
     */
    @Overwrite
    public static boolean isVillagerRendererAllowed() {
        if (MCARenderCache.isVillagerRendererAllowed == null) MCARenderCache.isVillagerRendererAllowed = !Config.getInstance().forceVillagerPlayerModel && Config.getInstance().playerRendererBlacklist.entrySet().stream().filter((entry) -> entry.getValue().equals("all") || entry.getValue().equals("block_villager")).noneMatch((entry) -> doesModExist(entry.getKey()));
        return MCARenderCache.isVillagerRendererAllowed;
    }

    /**
     * @author Kasualix
     * @reason cache areShadersAllowed;
     */
    @Overwrite
    public static boolean areShadersAllowed() {
        if (MCARenderCache.areShadersAllowed == null) MCARenderCache.areShadersAllowed = areShadersAllowed("shaders");
        return false;
    }
}
