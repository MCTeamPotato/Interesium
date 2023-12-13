package com.teampotato.interesium.mixin.vanilla;

import com.teampotato.interesium.api.InteresiumPoiManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;

@Mixin(AcquirePoi.class)
public abstract class AcquirePoiMixin {
    @Shadow private long nextScheduledStart;
    @Shadow @Final private Long2ObjectMap<AcquirePoi.JitteredLinearRetry> batchCache;
    @Shadow @Final private PoiType poiType;
    @Shadow @Final private MemoryModuleType<GlobalPos> memoryToAcquire;
    @Shadow @Final private Optional<Byte> onPoiAcquisitionEvent;

    @Inject(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PathfinderMob;J)V", at = @At("HEAD"), cancellable = true)
    private void interesium$start(@NotNull ServerLevel level, @NotNull PathfinderMob entity, long gameTime, @NotNull CallbackInfo ci) {
        ci.cancel();
        this.nextScheduledStart = gameTime + 20L + (long)level.getRandom().nextInt(20);
        PoiManager poiManager = level.getPoiManager();
        Predicate<BlockPos> predicate = blockPos -> {
            AcquirePoi.JitteredLinearRetry lv = this.batchCache.get(blockPos.asLong());
            if (lv == null) {
                return true;
            }
            if (!lv.shouldRetry(gameTime)) {
                return false;
            }
            lv.markAttempt(gameTime);
            return true;
        };
        Queue<BlockPos> blockPosSet = InteresiumPoiManager.limitedFindAllClosest(5, this.poiType.getPredicate(), predicate, entity.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE, poiManager);
        Path path = entity.getNavigation().createPath(new ObjectLinkedOpenHashSet<>(blockPosSet), this.poiType.getValidRange());
        if (path != null && path.canReach()) {
            BlockPos targetPos = path.getTarget();
            poiManager.getType(targetPos).ifPresent(poiType -> {
                poiManager.take(this.poiType.getPredicate(), blockPos -> blockPos.equals(targetPos), targetPos, 1);
                entity.getBrain().setMemory(this.memoryToAcquire, GlobalPos.of(level.dimension(), targetPos));
                this.onPoiAcquisitionEvent.ifPresent(state -> level.broadcastEntityEvent(entity, state));
                this.batchCache.clear();
                DebugPackets.sendPoiTicketCountPacket(level, targetPos);
            });
        } else {
            for (BlockPos blockPos : blockPosSet) {
                this.batchCache.computeIfAbsent(blockPos.asLong(), l -> new AcquirePoi.JitteredLinearRetry(level.random, gameTime));
            }
        }
    }
}
