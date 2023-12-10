package com.teampotato.interesium.util;

import net.minecraft.core.Cursor3D;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public final class IterationHelper {
    public static @NotNull Iterator<ChunkPos> rangeClosedIterator(final int startChunkPosX, final int startChunkPosZ, final int endChunkPosX, final int endChunkPosZ) {
        final int k = startChunkPosX < endChunkPosX ? 1 : -1;
        final int l = startChunkPosZ < endChunkPosZ ? 1 : -1;
        return Spliterators.iterator(new Spliterators.AbstractSpliterator<ChunkPos>((long) (Math.abs(startChunkPosX - endChunkPosX) + 1) * (Math.abs(startChunkPosZ - endChunkPosZ) + 1), Spliterator.SIZED){
            @Nullable ChunkPos pos;

            @Override
            public boolean tryAdvance(Consumer<? super ChunkPos> consumer) {
                if (this.pos == null) {
                    this.pos = new ChunkPos(startChunkPosX, startChunkPosZ);
                } else {
                    int x = this.pos.x;
                    int z = this.pos.z;
                    if (x == endChunkPosX) {
                        if (z == endChunkPosZ) return false;
                        this.pos.x = startChunkPosX;
                        this.pos.z = z + l;
                    } else {
                        this.pos.x = x + k;
                    }
                }
                consumer.accept(this.pos);
                return true;
            }
        });
    }

    @Contract("_, _, _, _, _, _ -> new")
    public static @NotNull Iterator<SectionPos> betweenClosedIterator(final int i, final int j, final int k, final int l, final int m, final int n) {
        return Spliterators.iterator(new Spliterators.AbstractSpliterator<SectionPos>((long) (l - i + 1) * (m - j + 1) * (n - k + 1), Spliterator.SIZED) {
            final Cursor3D cursor = new Cursor3D(i, j, k, l, m, n);

            public boolean tryAdvance(Consumer<? super SectionPos> consumer) {
                if (this.cursor.advance()) {
                    consumer.accept(SectionPos.of(this.cursor.nextX(), this.cursor.nextY(), this.cursor.nextZ()));
                    return true;
                } else {
                    return false;
                }
            }
        });
    }
}
