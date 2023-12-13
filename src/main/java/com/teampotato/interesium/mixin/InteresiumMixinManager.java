package com.teampotato.interesium.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class InteresiumMixinManager implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, @NotNull String mixinClassName) {
        if (mixinClassName.contains(".atum.")) return isLoaded("atum");
        if (mixinClassName.contains(".betterportals.")) return isLoaded("betterportals");
        if (mixinClassName.contains(".blue_skies.")) return isLoaded("blue_skies");
        if (mixinClassName.contains(".forbidden_arcanus.")) return isLoaded("forbidden_arcanus");
        if (mixinClassName.contains(".gaiadimension.")) return isLoaded("gaiadimension");
        if (mixinClassName.contains(".mca.")) return isLoaded("mca");
        if (mixinClassName.contains(".phi.")) return isLoaded("phi");
        if (mixinClassName.contains(".vampirism.")) return isLoaded("vampirism");
        return true;
    }

    public static boolean isLoaded(String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }

    @Override public void onLoad(String s) {}
    @Override public String getRefMapperConfig() {return null;}
    @Override public void acceptTargets(Set<String> set, Set<String> set1) {}
    @Override public List<String> getMixins() {return null;}
    @Override public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}
    @Override public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {}
}
