package com.teampotato.interesium.mixin.vanilla;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.teampotato.interesium.api.InteresiumPoiManager;
import com.teampotato.interesium.api.extension.ExtendedPoiManager;
import com.teampotato.interesium.api.extension.ExtendedPoiSection;
import com.teampotato.interesium.util.IterationHelper;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(PoiManager.class)
public abstract class PoiManagerMixin extends SectionStorage<PoiSection> implements ExtendedPoiManager {
    @Shadow @Final private LongSet loadedChunks;

    public PoiManagerMixin(File file, Function<Runnable, Codec<PoiSection>> function, Function<Runnable, PoiSection> function2, DataFixer dataFixer, DataFixTypes arg, boolean bl) {
        super(file, function, function2, dataFixer, arg, bl);
    }

    @Override
    public @Nullable PoiSection interesium$getOrLoad(long l) {
        return this.getOrLoad(l).orElse(null);
    }

    @Inject(method = "getCountInRange", at = @At("HEAD"), cancellable = true)
    private void interesium$getCountInRange(Predicate<PoiType> predicate, BlockPos pos, int distance, PoiManager.Occupancy status, CallbackInfoReturnable<Long> cir) {
        long count = 0L;
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(predicate, pos, distance, status, (PoiManager) (Object) this);
        while (poiRecordIterator.hasNext()) {
            count = count + 1;
            poiRecordIterator.next();
        }
        cir.setReturnValue(count);
    }

    /**
     * @author Kasualix
     * @reason Avoid stream; Using overwriting to avoid too much unnecessary operation in injecting
     */
    @Overwrite
    public Optional<BlockPos> find(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int distance, PoiManager.Occupancy status) {
        Iterator<BlockPos> blockPosIterator = InteresiumPoiManager.findAllIterator(typePredicate, posPredicate, pos, distance, status, (PoiManager) (Object) this);
        return Optional.ofNullable(blockPosIterator.hasNext() ? blockPosIterator.next() : null);
    }

    @Inject(method = "findClosest", at = @At("HEAD"), cancellable = true)
    private void interesium$findClosest(Predicate<PoiType> typePredicate, BlockPos pos, int distance, PoiManager.Occupancy status, CallbackInfoReturnable<Optional<BlockPos>> cir) {
        final Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(typePredicate, pos, distance, status, (PoiManager) (Object) this);

        double minDistanceSquared = Double.MAX_VALUE;
        BlockPos closestPos = null;

        while (poiRecordIterator.hasNext()) {
            BlockPos currentPos = poiRecordIterator.next().getPos();
            double currentDistanceSquared = currentPos.distSqr(pos);

            if (currentDistanceSquared < minDistanceSquared) {
                minDistanceSquared = currentDistanceSquared;
                closestPos = currentPos;
            }
        }

        cir.setReturnValue(Optional.ofNullable(closestPos));
    }

    /**
     * @author Kasualix
     * @reason Avoid stream; Using overwriting to avoid too much unnecessary operation in injecting
     */
    @Overwrite
    public Optional<BlockPos> take(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int distance) {
        final Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(typePredicate, pos, distance, PoiManager.Occupancy.HAS_SPACE, (PoiManager) (Object) this);
        while (poiRecordIterator.hasNext()) {
            final PoiRecord poiRecord = poiRecordIterator.next();
            final BlockPos recoredPos = poiRecord.getPos();
            if (posPredicate.test(recoredPos)) {
                poiRecord.acquireTicket();
                return Optional.ofNullable(recoredPos);
            }
        }
        return Optional.empty();
    }

    /**
     * @author Kasualix
     * @reason Avoid stream; Using overwriting to avoid too much unnecessary operation in injecting;
     */
    @Overwrite
    public Optional<BlockPos> getRandom(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, PoiManager.Occupancy status, BlockPos pos, int distance, Random random) {
        final ReferenceArrayList<PoiRecord> poiRecords = new ReferenceArrayList<>(InteresiumPoiManager.getInRangeIterator(typePredicate, pos, distance, status, (PoiManager) (Object) this));
        final ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        for (PoiRecord poiRecord : poiRecords) {
            final BlockPos blockPos = poiRecord.getPos();
            if (posPredicate.test(blockPos)) {
                if (threadLocalRandom.nextBoolean()) return Optional.ofNullable(blockPos);
            } else {
                poiRecords.remove(poiRecord);
            }
        }
        return poiRecords.isEmpty() ? Optional.empty() : Optional.ofNullable(poiRecords.get(threadLocalRandom.nextInt(poiRecords.size())).getPos());
    }

    /**
     * @author Kasualix
     * @reason Avoid stream; Using overwriting to avoid too much unnecessary operation in injecting
     */
    @SuppressWarnings("OptionalAssignedToNull")
    @Overwrite
    private boolean isVillageCenter(long l) {
        final Optional<PoiSection> poiSectionOptional = this.get(l);
        if (poiSectionOptional == null) return false;
        return poiSectionOptional.map(poiSection -> ((ExtendedPoiSection)poiSection).interesium$getRecordsIterator(PoiType.ALL, PoiManager.Occupancy.IS_OCCUPIED).hasNext()).orElse(false);
    }

    @Inject(method = "updateFromSection", at = @At("HEAD"), cancellable = true)
    private void interesium$updateFromSection(LevelChunkSection section, @NotNull SectionPos sectionPos, BiConsumer<BlockPos, PoiType> posToTypeConsumer, CallbackInfo ci) {
        ci.cancel();
        final int x = sectionPos.x() << 4;
        final int y = sectionPos.y() << 4;
        final int z = sectionPos.z() << 4;
        for (BlockPos blockPos : BlockPos.betweenClosed(x, y, z, x + 15, y + 15, z + 15)) {
            PoiType.forState(section.getBlockState(SectionPos.sectionRelative(blockPos.getX()), SectionPos.sectionRelative(blockPos.getY()), SectionPos.sectionRelative(blockPos.getZ()))).ifPresent(poiType -> posToTypeConsumer.accept(blockPos, poiType));
        }
    }

    @Inject(method = "ensureLoadedAndValid", at = @At("HEAD"), cancellable = true)
    private void interesium$ensureLoadedAndValid(LevelReader levelReader, @NotNull BlockPos pos, int coordinateOffset, CallbackInfo ci) {
        ci.cancel();
        final int x = pos.getX() >> 4;
        final int z = pos.getZ() >> 4;
        final int radius = Math.floorDiv(coordinateOffset, 16);
        final Iterator<SectionPos> sectionPosIterator = IterationHelper.betweenClosedIterator(x - radius, 0, z - radius, x + radius, 15, z + radius);
        while (sectionPosIterator.hasNext()) {
            final SectionPos sectionPos = sectionPosIterator.next();
            if (!this.getOrLoad(sectionPos.asLong()).map(PoiSection::isValid).orElse(false)) {
                final int sectionPosX = sectionPos.x();
                final int sectionPosZ = sectionPos.z();
                if (this.loadedChunks.add(ChunkPos.asLong(sectionPosX, sectionPosZ))) levelReader.getChunk(sectionPosX, sectionPosZ, ChunkStatus.EMPTY);
            }
        }
    }
}
