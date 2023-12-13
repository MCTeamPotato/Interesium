package com.teampotato.interesium.mixin.mods.gaiadimension;

import androsa.gaiadimension.registry.ModDimensions;
import androsa.gaiadimension.world.GaiaTeleporter;
import com.google.common.collect.Iterators;
import com.teampotato.interesium.api.InteresiumPoiManager;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;

@Mixin(GaiaTeleporter.class)
public abstract class GaiaTeleporterMixin {
    @Shadow @Final private ServerLevel world;

    @Inject(method = "getExistingPortal", at = @At("HEAD"), cancellable = true)
    private void interesium$getExistingPortal(BlockPos pos, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        PoiManager poiManager = this.world.getPoiManager();
        int radius = 64;
        poiManager.ensureLoadedAndValid(this.world, pos, radius);

        PriorityQueue<PoiRecord> poiRecordPriorityQueue = new PriorityQueue<>(Comparator.<PoiRecord>comparingDouble(poiRecord -> poiRecord.getPos().distSqr(pos)).thenComparingInt(poiRecord -> poiRecord.getPos().getY()));

        Iterators.filter(
                InteresiumPoiManager.getInSquareIterator(poiType -> poiType == ModDimensions.GAIA_PORTAL.get(), pos, radius, PoiManager.Occupancy.ANY, poiManager),
                poiRecord -> this.world.getBlockState(poiRecord.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
        ).forEachRemaining(poiRecordPriorityQueue::offer);

        cir.setReturnValue(
                Optional.ofNullable(poiRecordPriorityQueue.poll()).map(poiRecord -> {
                    BlockPos blockPos = poiRecord.getPos();
                    this.world.getChunkSource().distanceManager.addTicket(ChunkPos.asLong(blockPos.getX() >> 4, blockPos.getZ() >> 4), new Ticket<>(TicketType.PORTAL, 30, blockPos, false));
                    BlockState blockstate = this.world.getBlockState(blockPos);
                    return BlockUtil.getLargestRectangleAround(blockPos, blockstate.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, posIn -> this.world.getBlockState(posIn) == blockstate);
                })
        );
    }
}
