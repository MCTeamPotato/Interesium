package com.teampotato.interesium.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import com.teampotato.interesium.InteresiumPoiManager;
import com.teampotato.interesium.IterationHelper;
import com.teampotato.interesium.api.ExtendedPoiManager;
import com.teampotato.interesium.api.ExtendedPoiSection;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;
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
     * @reason Avoid stream; Using overwriting to avoid unnecessary operation in injecting
     */
    @Overwrite
    public Optional<BlockPos> find(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int distance, PoiManager.Occupancy status) {
        Iterator<BlockPos> blockPosIterator = InteresiumPoiManager.findAllIterator(typePredicate, posPredicate, pos, distance, status, (PoiManager) (Object) this);
        return Optional.ofNullable(blockPosIterator.hasNext() ? blockPosIterator.next() : null);
    }

    @Inject(method = "findClosest", at = @At("HEAD"), cancellable = true)
    private void interesium$findClosest(Predicate<PoiType> typePredicate, BlockPos pos, int distance, PoiManager.Occupancy status, CallbackInfoReturnable<Optional<BlockPos>> cir) {
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(typePredicate, pos, distance, status, (PoiManager) (Object) this);

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
     * @reason Avoid stream; Using overwriting to avoid unnecessary operation in injecting
     */
    @Overwrite
    public Optional<BlockPos> take(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int distance) {
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(typePredicate, pos, distance, PoiManager.Occupancy.HAS_SPACE, (PoiManager) (Object) this);
        while (poiRecordIterator.hasNext()) {
            PoiRecord poiRecord = poiRecordIterator.next();
            BlockPos recoredPos = poiRecord.getPos();
            if (posPredicate.test(recoredPos)) {
                poiRecord.acquireTicket();
                return Optional.ofNullable(recoredPos);
            }
        }
        return Optional.empty();
    }

    /**
     * @author Kasualix
     * @reason Avoid stream; Using overwriting to avoid unnecessary operation in injecting;
     */
    @Overwrite
    public Optional<BlockPos> getRandom(Predicate<PoiType> typePredicate, Predicate<BlockPos> posPredicate, PoiManager.Occupancy status, BlockPos pos, int distance, Random rand) {
        for (PoiRecord poiRecord : new ReferenceOpenHashSet<> /* random element insertion, replacing Collections#shuffle */(InteresiumPoiManager.getInRangeIterator(typePredicate, pos, distance, status, (PoiManager) (Object) this))) {
            BlockPos blockPos = poiRecord.getPos();
            if (posPredicate.test(blockPos)) {
                return Optional.ofNullable(blockPos);
            }
        }
        return Optional.empty();
    }

    /**
     * @author Kasualix
     * @reason Avoid stream; Using overwriting to avoid unnecessary operation in injecting
     */
    @SuppressWarnings("OptionalAssignedToNull")
    @Overwrite
    private boolean isVillageCenter(long l) {
        Optional<PoiSection> poiSectionOptional = this.get(l);
        if (poiSectionOptional == null) return false;
        return poiSectionOptional.map(poiSection -> ((ExtendedPoiSection)poiSection).interesium$getRecordsIterator(PoiType.ALL, PoiManager.Occupancy.IS_OCCUPIED).hasNext()).orElse(false);
    }

    @Inject(method = "updateFromSection", at = @At("HEAD"), cancellable = true)
    private void interesium$updateFromSection(LevelChunkSection section, @NotNull SectionPos sectionPos, BiConsumer<BlockPos, PoiType> posToTypeConsumer, CallbackInfo ci) {
        ci.cancel();
        int x = sectionPos.x() << 4;
        int y = sectionPos.y() << 4;
        int z = sectionPos.z() << 4;
        for (BlockPos blockPos : BlockPos.betweenClosed(x, y, z, x + 15, y + 15, z + 15)) {
            PoiType.forState(section.getBlockState(SectionPos.sectionRelative(blockPos.getX()), SectionPos.sectionRelative(blockPos.getY()), SectionPos.sectionRelative(blockPos.getZ()))).ifPresent(poiType -> posToTypeConsumer.accept(blockPos, poiType));
        }
    }

    @Inject(method = "ensureLoadedAndValid", at = @At("HEAD"), cancellable = true)
    private void interesium$ensureLoadedAndValid(LevelReader levelReader, @NotNull BlockPos pos, int coordinateOffset, CallbackInfo ci) {
        ci.cancel();
        int x = pos.getX() >> 4;
        int z = pos.getZ() >> 4;
        int radius = Math.floorDiv(coordinateOffset, 16);
        Iterator<SectionPos> sectionPosIterator = IterationHelper.betweenClosedIterator(x - radius, 0, z - radius, x + radius, 15, z + radius);
        while (sectionPosIterator.hasNext()) {
            SectionPos sectionPos = sectionPosIterator.next();
            if (!this.getOrLoad(sectionPos.asLong()).map(PoiSection::isValid).orElse(false)) {
                int sectionPosX = sectionPos.x();
                int sectionPosZ = sectionPos.z();
                if (this.loadedChunks.add(ChunkPos.asLong(sectionPosX, sectionPosZ))) levelReader.getChunk(sectionPosX, sectionPosZ, ChunkStatus.EMPTY);
            }
        }
    }
}
