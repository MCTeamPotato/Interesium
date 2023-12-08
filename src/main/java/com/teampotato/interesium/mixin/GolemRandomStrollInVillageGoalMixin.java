package com.teampotato.interesium.mixin;

import com.google.common.collect.UnmodifiableIterator;
import com.teampotato.interesium.InteresiumPoiManager;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(GolemRandomStrollInVillageGoal.class)
public abstract class GolemRandomStrollInVillageGoalMixin extends RandomStrollGoal {
    public GolemRandomStrollInVillageGoalMixin(PathfinderMob arg, double d) {
        super(arg, d);
    }

    /**
     * @author Kasualix
     * @reason Avoid stream usage; Use overwriting to avoid unnecessary allocation in injecting; Earlierly break the iteration and return the value if possible.
     */
    @Overwrite
    @Nullable
    private BlockPos getRandomPoiWithinSection(SectionPos sectionPos) {
        UnmodifiableIterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(poiType -> true, sectionPos.origin().offset(8, 8, 8), 8, PoiManager.Occupancy.IS_OCCUPIED, ((ServerLevel) this.mob.level).getPoiManager());
        ReferenceArrayList<PoiRecord> poiRecordList = new ReferenceArrayList<>();
        while (poiRecordIterator.hasNext()) {
            PoiRecord poiRecord = poiRecordIterator.next();
            poiRecordList.add(poiRecord);
            if (ThreadLocalRandom.current().nextBoolean()) return poiRecord.getPos();
        }
        return poiRecordList.isEmpty() ? null : poiRecordList.get(ThreadLocalRandom.current().nextInt(poiRecordList.size())).getPos();
    }
}
