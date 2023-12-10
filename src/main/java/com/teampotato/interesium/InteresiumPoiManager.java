package com.teampotato.interesium;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.teampotato.interesium.api.ExtendedPoiManager;
import com.teampotato.interesium.api.ExtendedPoiSection;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;

@Mod(InteresiumPoiManager.MOD_ID)
public final class InteresiumPoiManager {
    public static final String MOD_ID = "interesium";

    public static final Predicate<? super PoiRecord> ALWAYS_TRUE = poiRecord -> true;

    public static boolean isResourcefulBeesLoaded;

    public InteresiumPoiManager() {
        isResourcefulBeesLoaded = ModList.get().isLoaded("resourcefulbees");
    }

    @Contract("_, _, _, _, _ -> new")
    public static @NotNull UnmodifiableIterator<PoiRecord> getInRangeIterator(Predicate<PoiType> predicate, BlockPos pos, int distance, PoiManager.Occupancy status, PoiManager poiManager) {
        return Iterators.filter(getInSquareIterator(predicate, pos, distance, status, poiManager), poiRecord -> poiRecord.getPos().distSqr(pos) <= ((double) distance * distance));
    }

    public static @NotNull Iterator<BlockPos> findAllIterator(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int distance, PoiManager.Occupancy status, PoiManager poiManager) {
        final Iterator<PoiRecord> poiRecordIterator = getInRangeIterator(typePredicate, pos, distance, status, poiManager);

        return new Iterator<BlockPos>() {
            @Override
            public boolean hasNext() {
                return poiRecordIterator.hasNext();
            }

            @Override
            public BlockPos next() {
                BlockPos nextPos = poiRecordIterator.next().getPos();
                while (!posPredicate.test(nextPos) && poiRecordIterator.hasNext()) nextPos = poiRecordIterator.next().getPos();
                return nextPos;
            }
        };
    }

    public static @NotNull Set<BlockPos> findAllClosestFive(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int distance, PoiManager.Occupancy status, PoiManager poiManager) {
        final ObjectRBTreeSet<BlockPos> blockPosSortedSet = new ObjectRBTreeSet<>(Comparator.comparingDouble(blockPos -> blockPos.distSqr(pos)));
        final Iterator<BlockPos> all = findAllIterator(typePredicate, posPredicate, pos, distance, status, poiManager);
        while (all.hasNext()) blockPosSortedSet.add(all.next());
        if (blockPosSortedSet.size() <= 5) return blockPosSortedSet;
        final Set<BlockPos> limitedBlockPosSet = new LinkedHashSet<>(5);
        for (BlockPos blockPos : blockPosSortedSet) {
            limitedBlockPosSet.add(blockPos);
            if (limitedBlockPosSet.size() == 5) break;
        }
        return limitedBlockPosSet;
    }

    public static @NotNull Iterator<PoiRecord> getInSquareIterator(Predicate<PoiType> predicate, @NotNull BlockPos pos, int distance, PoiManager.Occupancy status, PoiManager poiManager) {
        final int radius = Math.floorDiv(distance, 16) + 1;
        final int x = pos.getX() >> 4;
        final int z = pos.getZ() >> 4;
        final Iterator<ChunkPos> chunkPosIterator = IterationHelper.rangeClosedIterator(x - radius, z - radius, x + radius, z + radius);

        return new Iterator<PoiRecord>() {
            Iterator<PoiRecord> currentIterator = Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!currentIterator.hasNext() && chunkPosIterator.hasNext()) {
                    currentIterator = InteresiumPoiManager.getInChunkIterator(predicate, chunkPosIterator.next(), status, poiManager);
                    currentIterator = Iterators.filter(currentIterator, poiRecord -> {
                        final BlockPos poiRecordPos = poiRecord.getPos();
                        return Math.abs(poiRecordPos.getX() - pos.getX()) <= distance && Math.abs(poiRecordPos.getZ() - pos.getZ()) <= distance;
                    });
                }
                return currentIterator.hasNext();
            }

            @Override
            public PoiRecord next() {
                return currentIterator.next();
            }
        };
    }

    @Contract(value = "_, _, _, _ -> new", pure = true)
    public static @NotNull Iterator<PoiRecord> getInChunkIterator(Predicate<PoiType> predicate, ChunkPos posChunk, PoiManager.Occupancy status, PoiManager poiManager) {
        return new Iterator<PoiRecord>() {
            int y = 0;
            Iterator<PoiRecord> recordIterator = Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!recordIterator.hasNext() && y < 16) {
                    final PoiSection poiSection = ((ExtendedPoiManager) poiManager).interesium$getOrLoad(SectionPos.asLong(posChunk.x, y, posChunk.z));
                    if (poiSection != null) recordIterator = ((ExtendedPoiSection) poiSection).interesium$getRecordsIterator(predicate, status);
                    y++;
                }
                return recordIterator.hasNext();
            }

            @Override
            public PoiRecord next() {
                return recordIterator.next();
            }
        };
    }

    public static long canEntitySpawn(PoiManager poiManager, Predicate<PoiType> predicate, BlockPos pos, int distance, PoiManager.Occupancy status) {
        long count = 0L;
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(predicate, pos, distance, status, poiManager);
        while (poiRecordIterator.hasNext()) {
            poiRecordIterator.next();
            count = count + 1;
            if (count == 5L) return count;
        }
        return 0L;
    }

    public static Iterator<PoiRecord> blueSkies$getPoisInCircle(@NotNull Entity entity, PoiType type, int radius) {
        if (entity.level instanceof ServerLevel) {
            ServerLevel level = (ServerLevel) entity.level;
            BlockPos startPos = entity.blockPosition();
            PoiManager poiManager = level.getPoiManager();
            poiManager.ensureLoadedAndValid(level, startPos, radius);
            return getInRangeIterator((poiType) -> poiType == type, startPos, radius, PoiManager.Occupancy.ANY, poiManager);
        }
        return Collections.emptyIterator();
    }
}
