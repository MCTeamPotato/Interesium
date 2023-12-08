package com.teampotato.interesium.mixin;

import com.google.common.collect.Iterators;
import com.teampotato.interesium.InteresiumPoiManager;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;

@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {
    @Shadow @Final protected ServerLevel level;

    @Inject(method = "findPortalAround", at = @At("HEAD"), cancellable = true)
    private void interesium$findPortalAround(BlockPos pos, boolean isNether, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        SortedSet<PoiRecord> poiRecordSortedSet = new ObjectRBTreeSet<>(
                Comparator.<PoiRecord>comparingDouble(poiRecord -> poiRecord.getPos().distSqr(pos)).thenComparingInt(poiRecord -> poiRecord.getPos().getY())
        );
        Iterator<PoiRecord> poiRecords = Iterators.filter(
                InteresiumPoiManager.getInSquareIterator(poiType -> poiType == PoiType.NETHER_PORTAL, pos, isNether ? 16 : 128 , PoiManager.Occupancy.ANY, this.level.getPoiManager()),
                poiRecord -> this.level.getBlockState(poiRecord.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
        );
        while (poiRecords.hasNext()) poiRecordSortedSet.add(poiRecords.next());
        cir.setReturnValue(
                Optional.ofNullable(poiRecordSortedSet.isEmpty() ? null : poiRecordSortedSet.first()).map(poiRecord -> {
                    BlockPos poiRecordPos = poiRecord.getPos();
                    this.level.getChunkSource().distanceManager.addTicket(ChunkPos.asLong(poiRecordPos.getX() >> 4, poiRecordPos.getZ() >> 4), new Ticket<>(TicketType.PORTAL, 30, poiRecordPos, false));
                    BlockState blockState = this.level.getBlockState(poiRecordPos);
                    return BlockUtil.getLargestRectangleAround(poiRecordPos, blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, blockPos -> this.level.getBlockState(blockPos) == blockState);
                })
        );
    }
}
