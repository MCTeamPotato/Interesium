package com.teampotato.interesium.mixin.mods.vampirism;

import com.teampotato.interesium.api.InteresiumPoiManager;
import de.teamlapen.vampirism.tileentity.TotemTileEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.stream.Stream;

@Mixin(TotemTileEntity.class)
public abstract class TotemTileEntityMixin extends BlockEntity {
    @Shadow @Nonnull public abstract AABB getVillageArea();

    public TotemTileEntityMixin(BlockEntityType<?> arg) {
        super(arg);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;count()J"))
    private long interesium$invalidate(Stream<?> instance) {
        return 0L;
    }

    @SuppressWarnings("DataFlowIssue")
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", ordinal = 0), index = 0)
    private int interesium$getBedsCount(int a) {
        int count = 0;
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator((pointOfInterestType) -> pointOfInterestType.equals(PoiType.HOME), this.worldPosition, (int)Math.sqrt(Math.pow(this.getVillageArea().getXsize(), 2.0) + Math.pow(this.getVillageArea().getZsize(), 2.0)) / 2, PoiManager.Occupancy.ANY, ((ServerLevel)this.level).getPoiManager());
        while (poiRecordIterator.hasNext()) {
            poiRecordIterator.next();
            count = count + 1;
        }
        return count;
    }
}
