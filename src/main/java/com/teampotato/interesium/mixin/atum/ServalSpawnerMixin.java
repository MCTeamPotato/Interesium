package com.teampotato.interesium.mixin.atum;

import com.teammetallurgy.atum.world.spawner.ServalSpawner;
import com.teampotato.interesium.InteresiumPoiManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(ServalSpawner.class)
public abstract class ServalSpawnerMixin {
    @Redirect(method = "attemptSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;getCountInRange(Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)J"))
    private long interesium$attemptSpawn(PoiManager poiManager, Predicate<PoiType> predicate, BlockPos pos, int distance, PoiManager.Occupancy status) {
        return InteresiumPoiManager.canEntitySpawn(poiManager, predicate, pos, distance, status);
    }
}
