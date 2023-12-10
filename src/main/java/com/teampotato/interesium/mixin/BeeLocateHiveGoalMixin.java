package com.teampotato.interesium.mixin;

import com.teampotato.interesium.InteresiumPoiManager;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeLocateHiveGoal")
public abstract class BeeLocateHiveGoalMixin {
    @Shadow(aliases = "this$0") @Final private Bee field_20375;

    /**
     * @author Kasualix
     * @reason avoid stream usage; use faster {@link ObjectRBTreeSet} implementation
     */
    @Overwrite
    public void start() {
        this.field_20375.remainingCooldownBeforeLocatingNewHive = 200;
        BlockPos blockPosition = this.field_20375.blockPosition();
        SortedSet<BlockPos> blockPosSortedSet = new ObjectRBTreeSet<>(Comparator.comparingDouble(blockPos -> blockPos.distSqr(blockPosition)));
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(
                InteresiumPoiManager.isResourcefulBeesLoaded ?
                        poiType -> poiType == PoiType.BEEHIVE || poiType == PoiType.BEE_NEST || poiType == com.resourcefulbees.resourcefulbees.registry.ModPOIs.TIERED_BEEHIVE_POI.get() :
                        poiType -> poiType == PoiType.BEEHIVE || poiType == PoiType.BEE_NEST
                , blockPosition, 20, PoiManager.Occupancy.ANY, ((ServerLevel) this.field_20375.level).getPoiManager());
        while (poiRecordIterator.hasNext()) {
            BlockPos poiRecordPos = poiRecordIterator.next().getPos();
            if (this.field_20375.doesHiveHaveSpace(poiRecordPos)) blockPosSortedSet.add(poiRecordPos);
        }
        if (!blockPosSortedSet.isEmpty()) {
            for (BlockPos blockpos : blockPosSortedSet) {
                if (this.field_20375.goToHiveGoal.isTargetBlacklisted(blockpos)) continue;
                this.field_20375.hivePos = blockpos;
                return;
            }
            this.field_20375.goToHiveGoal.clearBlacklist();
            this.field_20375.hivePos = blockPosSortedSet.first();
        }
    }
}
