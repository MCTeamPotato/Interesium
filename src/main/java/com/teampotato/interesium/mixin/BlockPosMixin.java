package com.teampotato.interesium.mixin;

import com.teampotato.interesium.api.ExtendedBlockPos;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockPos.class)
public class BlockPosMixin implements ExtendedBlockPos {
    @Unique private Boolean interesium$isInBlacklistedTargets = null;

    @Override
    public @Nullable Boolean interesium$isInBlacklistedTargets() {
        return this.interesium$isInBlacklistedTargets;
    }

    @Override
    public void interesium$setIsInBlacklistedTargets(@Nullable Boolean isInBlacklistedTargets) {
        this.interesium$isInBlacklistedTargets = isInBlacklistedTargets;
    }
}
