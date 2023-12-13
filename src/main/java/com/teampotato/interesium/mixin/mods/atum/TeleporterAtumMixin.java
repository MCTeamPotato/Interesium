package com.teampotato.interesium.mixin.mods.atum;

import com.teammetallurgy.atum.init.AtumPointsOfInterest;
import com.teammetallurgy.atum.world.teleporter.TeleporterAtum;
import com.teampotato.interesium.api.InteresiumPoiManager;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;

@Mixin(value = TeleporterAtum.class, remap = false)
public abstract class TeleporterAtumMixin {
    @Inject(method = "getExistingPortal", at = @At("HEAD"), cancellable = true)
    private void interesium$getExistingPortal(ServerLevel serverLevel, BlockPos pos, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        PoiManager poiManager = serverLevel.getPoiManager();
        int radius = 128;
        poiManager.ensureLoadedAndValid(serverLevel, pos, radius);
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInSquareIterator(poiType -> poiType == AtumPointsOfInterest.PORTAL, pos, radius, PoiManager.Occupancy.ANY, poiManager);
        SortedSet<PoiRecord> poiRecordSortedSet = new ObjectRBTreeSet<>(Comparator.<PoiRecord>comparingDouble((poi) -> poi.getPos().distSqr(pos)).thenComparingInt((poi) -> poi.getPos().getY()));
        while (poiRecordIterator.hasNext()) poiRecordSortedSet.add(poiRecordIterator.next());
        cir.setReturnValue(
                Optional.ofNullable(poiRecordSortedSet.isEmpty() ? null : poiRecordSortedSet.first()).map((poiRecord) -> {
                    BlockPos poiRecordPos = poiRecord.getPos();
                    serverLevel.getChunkSource().distanceManager.addTicket(ChunkPos.asLong(poiRecordPos.getX() >> 4, poiRecordPos.getZ() >> 4), new Ticket<>(TicketType.PORTAL, 30, poiRecordPos, false));
                    BlockState blockState = serverLevel.getBlockState(poiRecordPos);
                    return BlockUtil.getLargestRectangleAround(poiRecordPos, Direction.Axis.X, 9, Direction.Axis.Z, 9, (posIn) -> serverLevel.getBlockState(posIn) == blockState);
                })
        );
    }
}
