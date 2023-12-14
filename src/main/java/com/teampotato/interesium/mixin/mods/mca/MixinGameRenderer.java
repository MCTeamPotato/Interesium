package com.teampotato.interesium.mixin.mods.mca;

import com.mojang.datafixers.util.Pair;
import com.teampotato.interesium.compat.MCARenderCache;
import forge.net.mca.Config;
import forge.net.mca.MCA;
import forge.net.mca.client.model.CommonVillagerModel;
import forge.net.mca.entity.VillagerLike;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Collectors;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Nullable private PostChain postEffect;
    @Shadow public abstract void loadEffect(ResourceLocation resourceLocation);
    @Shadow public abstract void shutdownEffect();

    @Unique private Pair<String, ResourceLocation> mca$currentShader;
    @Unique private static final ObjectOpenHashSet<Map.Entry<String, String>> ALLOWED_SHADERS = new ObjectOpenHashSet<>();

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void initAllowedShaders(CallbackInfo ci) {
        ALLOWED_SHADERS.clear();
        ALLOWED_SHADERS.addAll(Config.getInstance().shaderLocationsMap.entrySet().stream().filter(entry -> MCA.areShadersAllowed(entry.getKey() + "_shader")).collect(Collectors.toSet()));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onCameraSet(CallbackInfo ci) {
        if (MCARenderCache.areShadersAllowed == null) return;
        if (!MCARenderCache.areShadersAllowed) return;
        if (this.minecraft.cameraEntity == null) return;
        VillagerLike<?> villagerLike = CommonVillagerModel.getVillager(this.minecraft.cameraEntity);
        if (villagerLike == null) return;
        if (this.postEffect == null) {
            if (this.mca$currentShader != null) {
                this.loadEffect(this.mca$currentShader.getSecond());
            } else {
                for (Map.Entry<String, String> entry : ALLOWED_SHADERS) {
                    if (!villagerLike.getTraits().hasTrait(entry.getKey())) continue;
                    ResourceLocation id = new ResourceLocation(entry.getValue());
                    this.mca$currentShader = new Pair<>(entry.getKey(), id);
                    this.loadEffect(id);
                    break;
                }
            }
        } else if (this.mca$currentShader != null) {
            if (villagerLike.getTraits().hasTrait(this.mca$currentShader.getFirst())) return;
            this.shutdownEffect();
            this.mca$currentShader = null;
        }
    }
}
