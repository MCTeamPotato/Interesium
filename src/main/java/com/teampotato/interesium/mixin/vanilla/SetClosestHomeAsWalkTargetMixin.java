package com.teampotato.interesium.mixin.vanilla;

import com.teampotato.interesium.api.InteresiumPoiManager;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.SetClosestHomeAsWalkTarget;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
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

@Mixin(SetClosestHomeAsWalkTarget.class)
public abstract class SetClosestHomeAsWalkTargetMixin {
    @Shadow private int triedCount;
    @Shadow private long lastUpdate;
    @Shadow @Final private Long2LongMap batchCache;
    @Shadow @Final private float speedModifier;

    @Unique
    private final Supplier<Predicate<BlockPos>> interesium$startingTest = () -> new Predicate<BlockPos>() {
        @Override
        public boolean test(BlockPos pos) {
            long l = pos.asLong();
            if (batchCache.containsKey(l)) {
                return false;
            }
            if (++triedCount >= 5) {
                return false;
            }
            batchCache.put(l, lastUpdate + 40L);
            return true;
        }
    };

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void interesium$start(@NotNull ServerLevel level, @NotNull LivingEntity entity, long gameTime, @NotNull CallbackInfo ci) {
        ci.cancel();
        this.triedCount = 0;
        this.lastUpdate = level.getGameTime() + (long)level.getRandom().nextInt(20);
        PathfinderMob mob = (PathfinderMob) entity;
        PoiManager poiManager = level.getPoiManager();
        Iterator<BlockPos> blockPosIterator = InteresiumPoiManager.findAllIterator(PoiType.HOME.getPredicate(), interesium$startingTest.get(), entity.blockPosition(), 48, PoiManager.Occupancy.ANY, poiManager);
        Path path = mob.getNavigation().createPath(new ReferenceOpenHashSet<>(blockPosIterator), PoiType.HOME.getValidRange());
        if (path != null && path.canReach()) {
            BlockPos targetPos = path.getTarget();
            Optional<PoiType> poiTypeOptional = poiManager.getType(targetPos);
            if (poiTypeOptional.isPresent()) {
                mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(targetPos, this.speedModifier, 1));
            }
        } else if (this.triedCount < 5) {
            this.batchCache.long2LongEntrySet().removeIf(entry -> entry.getLongValue() < this.lastUpdate);
        }
    }
}
