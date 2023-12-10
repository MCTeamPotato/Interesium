package com.teampotato.interesium.mixin.blue_skies;

import com.legacy.blue_skies.blocks.dungeon.SpiderNestBlock;
import com.legacy.blue_skies.entities.hostile.boss.ArachnarchEntity;
import com.legacy.blue_skies.registries.SkiesEntityTypes;
import com.legacy.blue_skies.registries.SkiesPointsOfInterest;
import com.teampotato.interesium.InteresiumPoiManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(targets = "com.legacy.blue_skies.entities.hostile.boss.ArachnarchEntity$ScreechAttackGoal")
public abstract class ArachnarchEntityScreechAttackGoalMixin {
    @Shadow(remap = false) @Final private ArachnarchEntity boss;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/legacy/blue_skies/entities/hostile/boss/ArachnarchEntity;getHome()Lnet/minecraft/core/BlockPos;", shift = At.Shift.BEFORE), cancellable = true)
    private void interesium$tick(CallbackInfo ci) {
        ci.cancel();
        BlockPos homePos = this.boss.getHome();
        if (this.boss.level instanceof ServerLevel) {
            ServerLevel level = (ServerLevel) this.boss.level;
            PoiManager poiManager = level.getPoiManager();
            poiManager.ensureLoadedAndValid(level, homePos, 15);
            Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInSquareIterator((poiType) -> poiType == SkiesPointsOfInterest.SPIDER_NEST, homePos, 15, PoiManager.Occupancy.ANY, poiManager);
            while (poiRecordIterator.hasNext()) {
                BlockPos pos = poiRecordIterator.next().getPos();
                BlockState funny = level.getBlockState(pos);
                if (funny.getBlock() instanceof SpiderNestBlock && level.random.nextFloat() < 0.7F) {
                    SpiderNestBlock.spawnSpiders(funny, level, pos, SkiesEntityTypes.NESTED_SPIDER);
                }
            }
        }
    }
}
