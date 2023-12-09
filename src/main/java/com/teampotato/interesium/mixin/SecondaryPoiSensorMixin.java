package com.teampotato.interesium.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SecondaryPoiSensor;
import net.minecraft.world.entity.npc.Villager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//https://github.com/CaffeineMC/lithium-fabric/blob/develop/src/main/java/me/jellysquid/mods/lithium/mixin/ai/sensor/secondary_poi/SecondaryPointsOfInterestSensorMixin.java
@Mixin(SecondaryPoiSensor.class)
public abstract class SecondaryPoiSensorMixin {
    @Inject(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;)V", at = @At("HEAD"), cancellable = true)
    private void skipUselessSense(ServerLevel level, @NotNull Villager villager, CallbackInfo ci) {
        if (villager.getVillagerData().getProfession().getSecondaryPoi().isEmpty()) {
            villager.getBrain().eraseMemory(MemoryModuleType.SECONDARY_JOB_SITE);
            ci.cancel();
        }
    }
}
