package com.teampotato.interesium.mixin.mods.mca;

import com.teampotato.interesium.api.InteresiumPoiManager;
import forge.net.mca.server.world.data.Building;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

@Mixin(value = Building.class, remap = false)
public abstract class BuildingMixin {
    @Shadow public abstract BlockPos getCenter();
    @Shadow public abstract BlockPos getPos0();
    @Shadow public abstract BlockPos getPos1();
    @Shadow public abstract boolean containsPos(Vec3i pos);

    @Inject(method = "findClosestEmptyBed", at = @At("HEAD"), cancellable = true)
    private void interesium$findClosestEmptyBed(@NotNull ServerLevel world, BlockPos pos, CallbackInfoReturnable<Optional<BlockPos>> cir) {
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInSquareIterator(PoiType.HOME.getPredicate(), this.getCenter(), this.getPos0().distManhattan(this.getPos1()), PoiManager.Occupancy.ANY, world.getPoiManager());
        Comparator<BlockPos> blockPosComparator = Comparator.comparingInt(a -> a.distManhattan(pos));
        BlockPos blockPos = null;

        while (poiRecordIterator.hasNext()) {
            BlockPos poiRecordPos = poiRecordIterator.next().getPos();
            BlockState blockState = world.getBlockState(poiRecordPos);

            if (!blockState.getValue(BedBlock.OCCUPIED) && blockState.is(BlockTags.BEDS) && this.containsPos(poiRecordPos)) {
                if (blockPos == null) {
                    blockPos = poiRecordPos;
                } else {
                    if (blockPosComparator.compare(blockPos, poiRecordPos) < 0) {
                        blockPos = poiRecordPos;
                    }
                }
            }
        }

        cir.setReturnValue(Optional.ofNullable(blockPos));
    }
}
