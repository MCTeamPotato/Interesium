package com.teampotato.interesium.api;

import org.jetbrains.annotations.Nullable;

public interface ExtendedBlockPos {
    @Nullable Boolean interesium$isInBlacklistedTargets();
    void interesium$setIsInBlacklistedTargets(@Nullable Boolean isInBlacklistedTargets);
}
