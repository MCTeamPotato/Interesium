package com.teampotato.interesium;

import com.google.common.collect.Iterators;
import com.teampotato.interesium.api.InteresiumPoiManager;
import com.teampotato.interesium.mixin.InteresiumMixinManager;
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
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;

@Mod(Interesium.MOD_ID)
public final class Interesium {
    public static final String MOD_ID = "interesium";
    public static boolean isResourcefulBeesLoaded;

    public Interesium() {
        isResourcefulBeesLoaded = InteresiumMixinManager.isLoaded("resourcefulbees");
    }

    public static void interesium$findPortalAround(@NotNull ServerLevel level, BlockPos pos, boolean isNether, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        long begin = System.currentTimeMillis();
        SortedSet<PoiRecord> poiRecordSortedSet = new ObjectRBTreeSet<>(
                Comparator.<PoiRecord>comparingDouble(poiRecord -> poiRecord.getPos().distSqr(pos)).thenComparingInt(poiRecord -> poiRecord.getPos().getY())
        );
        Iterator<PoiRecord> poiRecords = Iterators.filter(
                InteresiumPoiManager.getInSquareIterator(poiType -> poiType == PoiType.NETHER_PORTAL, pos, isNether ? 16 : 128 , PoiManager.Occupancy.ANY, level.getPoiManager()),
                poiRecord -> level.getBlockState(poiRecord.getPos()).hasProperty(BlockStateProperties.HORIZONTAL_AXIS)
        );
        while (poiRecords.hasNext()) poiRecordSortedSet.add(poiRecords.next());
        cir.setReturnValue(
                Optional.ofNullable(poiRecordSortedSet.isEmpty() ? null : poiRecordSortedSet.first()).map(poiRecord -> {
                    BlockPos poiRecordPos = poiRecord.getPos();
                    level.getChunkSource().distanceManager.addTicket(ChunkPos.asLong(poiRecordPos.getX() >> 4, poiRecordPos.getZ() >> 4), new Ticket<>(TicketType.PORTAL, 30, poiRecordPos, false));
                    BlockState blockState = level.getBlockState(poiRecordPos);
                    return BlockUtil.getLargestRectangleAround(poiRecordPos, blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, blockPos -> level.getBlockState(blockPos) == blockState);
                })
        );
        long end = System.currentTimeMillis();
        InteresiumMixinManager.LOGGER.error("findPortalAround time cost: " + (end - begin));
        //从主世界到下界：12 9 11 11 10 13
        //从下界到主世界：62 67 115 134 140 258
    }
}
