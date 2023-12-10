package com.teampotato.interesium.mixin.gaiadimension;

import androsa.gaiadimension.registry.ModDimensions;
import androsa.gaiadimension.world.GaiaTeleporter;
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
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;

@Mixin(GaiaTeleporter.class)
public abstract class GaiaTeleporterMixin {
    @Shadow @Final private ServerLevel world;

    @Inject(method = "getExistingPortal", at = @At("HEAD"), cancellable = true)
    private void interesium$getExistingPortal(BlockPos pos, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        PoiManager poiManager = this.world.getPoiManager();
        int radius = 64;
        poiManager.ensureLoadedAndValid(this.world, pos, radius);
        SortedSet<PoiRecord> poiRecordSortedSet = new ObjectRBTreeSet<>(Comparator.<PoiRecord>comparingDouble((poi) -> poi.getPos().distSqr(pos)).thenComparingInt((poi) -> poi.getPos().getY()));
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInSquareIterator((poiType) -> poiType == ModDimensions.GAIA_PORTAL.get(), pos, radius, PoiManager.Occupancy.ANY, poiManager);
        while (poiRecordIterator.hasNext()) {
            PoiRecord poiRecord = poiRecordIterator.next();
            if (this.world.getBlockState(poiRecord.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                poiRecordSortedSet.add(poiRecord);
            }
        }
        cir.setReturnValue(Optional.ofNullable(poiRecordSortedSet.isEmpty() ? null : poiRecordSortedSet.first()).map(poiRecord -> {
            BlockPos blockPos = poiRecord.getPos();
            this.world.getChunkSource().distanceManager.addTicket(ChunkPos.asLong(blockPos.getX() >> 4, blockPos.getZ() >> 4), new Ticket<>(TicketType.PORTAL, 30, blockPos, false));
            BlockState blockstate = this.world.getBlockState(blockPos);
            return BlockUtil.getLargestRectangleAround(blockPos, blockstate.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, (posIn) -> this.world.getBlockState(posIn) == blockstate);
        }));
    }
}
