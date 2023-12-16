package com.teampotato.interesium.mixin.vanilla;

import com.teampotato.interesium.Interesium;
import com.teampotato.interesium.api.InteresiumPoiManager;
import com.teampotato.interesium.mods.ResourcefulBeesCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeLocateHiveGoal")
public abstract class BeeLocateHiveGoalMixin {
    @Shadow(aliases = "this$0") @Final private Bee field_20375;

    /**
     * @author Kasualix
     * @reason avoid stream usage; use faster {@link PriorityQueue} implementation
     */
    @Overwrite
    public void start() {
        this.field_20375.remainingCooldownBeforeLocatingNewHive = 200;
        BlockPos blockPosition = this.field_20375.blockPosition();
        PriorityQueue<BlockPos> blockPosPriorityQueue = new PriorityQueue<>(Comparator.comparingDouble(blockPos -> blockPos.distSqr(blockPosition)));
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(
                Interesium.IS_RESOURCEFUL_BEES_LOADED ?
                        poiType -> poiType == PoiType.BEEHIVE || poiType == PoiType.BEE_NEST || poiType == ResourcefulBeesCompat.TIERED_BEEHIVE_POI :
                        poiType -> poiType == PoiType.BEEHIVE || poiType == PoiType.BEE_NEST
                , blockPosition, 20, PoiManager.Occupancy.ANY, ((ServerLevel) this.field_20375.level).getPoiManager());
        while (poiRecordIterator.hasNext()) {
            BlockPos poiRecordPos = poiRecordIterator.next().getPos();
            if (this.field_20375.doesHiveHaveSpace(poiRecordPos)) blockPosPriorityQueue.offer(poiRecordPos);
        }
        if (!blockPosPriorityQueue.isEmpty()) {
            for (BlockPos blockpos : blockPosPriorityQueue) {
                if (this.field_20375.goToHiveGoal.isTargetBlacklisted(blockpos)) continue;
                this.field_20375.hivePos = blockpos;
                return;
            }
            this.field_20375.goToHiveGoal.clearBlacklist();
            this.field_20375.hivePos = blockPosPriorityQueue.poll();
        }
    }
}
