package com.teampotato.interesium.mixin.vanilla;

import com.teampotato.interesium.api.InteresiumPoiManager;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.NearestBedSensor;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(NearestBedSensor.class)
public abstract class NearestBedSensorMixin {
    @Shadow private int triedCount;
    @Shadow private long lastUpdate;
    @Shadow @Final private Long2LongMap batchCache;

    @Unique
    private final Supplier<Predicate<BlockPos>> interesium$tickingTest = () -> (Predicate<BlockPos>) blockPos -> {
        long l = blockPos.asLong();
        if (this.batchCache.containsKey(l)) {
            return false;
        }
        if (++this.triedCount >= 5) {
            return false;
        }
        this.batchCache.put(l, this.lastUpdate + 40L);
        return true;
    };

    @Inject(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;)V", at = @At("HEAD"), cancellable = true)
    private void interesium$doTick(ServerLevel level, Mob entity, CallbackInfo ci) {
        ci.cancel();
        if (entity.isBaby()) return;
        this.triedCount = 0;
        this.lastUpdate = level.getGameTime() + (long)level.getRandom().nextInt(20);
        PoiManager poiManager = level.getPoiManager();
        Iterator<BlockPos> blockPosIterator = InteresiumPoiManager.findAllIterator(PoiType.HOME.getPredicate(), interesium$tickingTest.get(), entity.blockPosition(), 48, PoiManager.Occupancy.ANY, poiManager);
        Path path = entity.getNavigation().createPath(new ReferenceOpenHashSet<>(blockPosIterator), PoiType.HOME.getValidRange());
        if (path != null && path.canReach()) {
            BlockPos targetPos = path.getTarget();
            Optional<PoiType> poiTypeOptional = poiManager.getType(targetPos);
            if (poiTypeOptional.isPresent()) {
                entity.getBrain().setMemory(MemoryModuleType.NEAREST_BED, targetPos);
            }
        } else if (this.triedCount < 5) {
            this.batchCache.long2LongEntrySet().removeIf(entry -> entry.getLongValue() < this.lastUpdate);
        }
    }
}
