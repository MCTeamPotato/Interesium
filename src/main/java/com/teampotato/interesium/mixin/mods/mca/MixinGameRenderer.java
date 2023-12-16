package com.teampotato.interesium.mixin.mods.mca;

import com.mojang.datafixers.util.Pair;
import com.teampotato.interesium.mods.MCARenderCache;
import forge.net.mca.Config;
import forge.net.mca.MCA;
import forge.net.mca.client.model.CommonVillagerModel;
import forge.net.mca.entity.VillagerLike;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
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

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Nullable private PostChain postEffect;
    @Shadow public abstract void loadEffect(ResourceLocation resourceLocation);
    @Shadow public abstract void shutdownEffect();

    @Unique private Pair<String, ResourceLocation> mca$currentShader;
    @Unique private static ObjectIterator<Map.Entry<String, String>> ALLOWED_SHADERS_ITERATOR;

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void initAllowedShaders(CallbackInfo ci) {
        ALLOWED_SHADERS_ITERATOR = new ObjectOpenHashSet<>(Config.getInstance().shaderLocationsMap.entrySet().stream().filter(entry -> MCA.areShadersAllowed(entry.getKey() + "_shader")).iterator()).iterator();
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
                while (ALLOWED_SHADERS_ITERATOR.hasNext()) {
                    Map.Entry<String, String> entry = ALLOWED_SHADERS_ITERATOR.next();
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
