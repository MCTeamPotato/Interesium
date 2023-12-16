package com.teampotato.interesium.api.util;

import com.teampotato.interesium.api.extension.ExtendedSectionPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public final class IterationHelper {
    public static @NotNull Iterator<ChunkPos> rangeClosedIterator(final int startChunkPosX, final int startChunkPosZ, final int endChunkPosX, final int endChunkPosZ) {
        final int k = startChunkPosX < endChunkPosX ? 1 : -1;
        final int l = startChunkPosZ < endChunkPosZ ? 1 : -1;

        return new Iterator<ChunkPos>() {
            int currentX = startChunkPosX;
            int currentZ = startChunkPosZ;
            @Nullable ChunkPos nextPos = null;

            @Override
            public boolean hasNext() {
                return currentX != endChunkPosX || currentZ != endChunkPosZ;
            }

            @Override
            public ChunkPos next() {
                if (nextPos == null) {
                    nextPos = new ChunkPos(currentX, currentZ);
                } else {
                    nextPos.x = currentX;
                    nextPos.z = currentZ;
                }

                if (currentX == endChunkPosX) {
                    if (currentZ != endChunkPosZ) {
                        currentX = startChunkPosX;
                        currentZ += l;
                    }
                } else {
                    currentX += k;
                }

                return nextPos;
            }
        };
    }

    @Contract("_, _, _, _, _, _ -> new")
    public static @NotNull Iterator<SectionPos> betweenClosedIterator(final int i, final int j, final int k, final int l, final int m, final int n) {
        return new Iterator<SectionPos>() {
            final Cursor3D cursor = new Cursor3D(i, j, k, l, m, n);
            @Nullable SectionPos nextPos = null;

            @Override
            public boolean hasNext() {
                return this.cursor.advance();
            }

            @Override
            public SectionPos next() {
                if (nextPos == null) {
                    nextPos = SectionPos.of(cursor.nextX(), cursor.nextY(), cursor.nextZ());
                } else {
                    ((ExtendedSectionPos)nextPos).interesium$setPos(cursor.nextX(), cursor.nextY(), cursor.nextZ());
                }
                return nextPos;
            }
        };
    }
}
