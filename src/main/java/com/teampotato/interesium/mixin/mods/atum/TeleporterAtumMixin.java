package com.teampotato.interesium.mixin.mods.atum;

import com.teammetallurgy.atum.init.AtumPointsOfInterest;
import com.teammetallurgy.atum.world.teleporter.TeleporterAtum;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;

@Mixin(value = TeleporterAtum.class, remap = false)
public abstract class TeleporterAtumMixin {
    @Unique private static final int interesium$radius = 128;

    @Inject(method = "getExistingPortal", at = @At("HEAD"), cancellable = true)
    private void interesium$getExistingPortal(ServerLevel serverLevel, BlockPos pos, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        PoiManager poiManager = serverLevel.getPoiManager();
        poiManager.ensureLoadedAndValid(serverLevel, pos, interesium$radius);

        PriorityQueue<PoiRecord> poiRecordPriorityQueue = new PriorityQueue<>(Comparator.<PoiRecord>comparingDouble(poiRecord -> poiRecord.getPos().distSqr(pos)).thenComparingInt(poiRecord -> poiRecord.getPos().getY()));

        InteresiumPoiManager.getInSquareIterator(poiType -> poiType == AtumPointsOfInterest.PORTAL, pos, interesium$radius, PoiManager.Occupancy.ANY, poiManager)
                .forEachRemaining(poiRecordPriorityQueue::offer);

        cir.setReturnValue(
                Optional.ofNullable(poiRecordPriorityQueue.poll()).map(poiRecord -> {
                    BlockPos poiRecordPos = poiRecord.getPos();
                    serverLevel.getChunkSource().distanceManager.addTicket(ChunkPos.asLong(poiRecordPos.getX() >> 4, poiRecordPos.getZ() >> 4), new Ticket<>(TicketType.PORTAL, 30, poiRecordPos, false));
                    BlockState blockState = serverLevel.getBlockState(poiRecordPos);
                    return BlockUtil.getLargestRectangleAround(poiRecordPos, Direction.Axis.X, 9, Direction.Axis.Z, 9, posIn -> serverLevel.getBlockState(posIn) == blockState);
                })
        );
    }
}
