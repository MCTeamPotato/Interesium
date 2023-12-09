package com.teampotato.interesium.mixin.blueskies;

import com.legacy.blue_skies.blocks.dungeon.SpiderNestBlock;
import com.legacy.blue_skies.entities.hostile.boss.ArachnarchEntity;
import com.legacy.blue_skies.registries.SkiesEntityTypes;
import com.legacy.blue_skies.registries.SkiesPointsOfInterest;
import com.legacy.blue_skies.registries.SkiesSounds;
import com.teampotato.interesium.InteresiumPoiManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(targets = "com.legacy.blue_skies.entities.hostile.boss.ArachnarchEntity$ScreechAttackGoal")
public abstract class ArachnarchEntityScreechAttackGoalMixin {
    @Shadow(remap = false) @Final private ArachnarchEntity boss;

    @Shadow(remap = false) private int screechTime;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void interesium$tick(CallbackInfo ci) {
        ci.cancel();
        this.boss.getLookControl().setLookAt(this.boss.getTarget(), 30.0F, 30.0F);
        if (this.screechTime > 0) --this.screechTime;
        if (this.screechTime == 75) {
            this.boss.level.broadcastEntityEvent(this.boss, (byte)6);
            this.boss.playSound(SkiesSounds.ENTITY_ARACHNARCH_SCREECH, 2.0F, (ThreadLocalRandom.current().nextFloat() - ThreadLocalRandom.current().nextFloat()) * 0.2F + 1.0F);
        }

        if (this.screechTime == 79 && !this.boss.level.isClientSide) {
            for (Player players : this.boss.getPlayersInDungeonArea()) {
                players.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false));
            }
        }

        if (this.screechTime == 20) {
            BlockPos homePos = this.boss.getHome();
            byte radius = 15;
            if (this.boss.level instanceof ServerLevel) {
                ServerLevel level = (ServerLevel) this.boss.level;
                PoiManager poiManager = level.getPoiManager();
                poiManager.ensureLoadedAndValid(level, homePos, radius);
                Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInSquareIterator((poiType) -> poiType == SkiesPointsOfInterest.SPIDER_NEST, homePos, radius, PoiManager.Occupancy.ANY, poiManager);
                while (poiRecordIterator.hasNext()) {
                    BlockPos pos = poiRecordIterator.next().getPos();
                    BlockState funny = level.getBlockState(pos);
                    if (funny.getBlock() instanceof SpiderNestBlock && level.random.nextFloat() < 0.7F) {
                        SpiderNestBlock.spawnSpiders(funny, level, pos, SkiesEntityTypes.NESTED_SPIDER);
                    }
                }
            }
        }

        if (this.screechTime <= 1) this.boss.setTicksUntilNextScreech(1200);
    }
}
