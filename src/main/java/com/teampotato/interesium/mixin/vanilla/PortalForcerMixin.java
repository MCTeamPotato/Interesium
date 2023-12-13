package com.teampotato.interesium.mixin.vanilla;

import com.google.common.collect.Iterators;
import com.teampotato.interesium.api.InteresiumPoiManager;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
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
import java.util.Optional;

@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {
    //findPortalAround 执行时长（数据单位：ms）

    //原版
    //从主世界到下界：56 10 10 10
    //从下界到主世界：410 477 772 926

    //Interesium
    //从主世界到下界：1
    //从下界到主世界：56

    @Shadow @Final protected ServerLevel level;

    @Inject(method = "findPortalAround", at = @At("HEAD"), cancellable = true)
    private void interesium$findPortalAround(BlockPos pos, boolean isNether, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        ObjectHeapPriorityQueue<PoiRecord> poiRecordPriorityQueue = new ObjectHeapPriorityQueue<>(2, Comparator.<PoiRecord>comparingDouble(poiRecord -> poiRecord.getPos().distSqr(pos)).thenComparingInt(poiRecord -> poiRecord.getPos().getY()));
        Iterators
                .filter(InteresiumPoiManager.getInSquareIterator(poiType -> poiType == PoiType.NETHER_PORTAL, pos, isNether ? 16 : 128 , PoiManager.Occupancy.ANY, level.getPoiManager()), poiRecord -> level.getBlockState(poiRecord.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
                .forEachRemaining(poiRecord -> {
                    poiRecordPriorityQueue.enqueue(poiRecord);
                    if (poiRecordPriorityQueue.size() > 1) poiRecordPriorityQueue.dequeue();
                });

        cir.setReturnValue(
                Optional.ofNullable(poiRecordPriorityQueue.isEmpty() ? null : poiRecordPriorityQueue.dequeue()).map(poiRecord -> {
                    BlockPos poiRecordPos = poiRecord.getPos();
                    level.getChunkSource().distanceManager.addTicket(ChunkPos.asLong(poiRecordPos.getX() >> 4, poiRecordPos.getZ() >> 4), new Ticket<>(TicketType.PORTAL, 30, poiRecordPos, false));
                    BlockState blockState = level.getBlockState(poiRecordPos);
                    return BlockUtil.getLargestRectangleAround(poiRecordPos, blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, blockPos -> level.getBlockState(blockPos) == blockState);
                })
        );
    }
}
