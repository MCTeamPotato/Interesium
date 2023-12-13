package com.teampotato.interesium.api.extension;

import net.minecraft.world.entity.ai.village.poi.PoiSection;
import org.jetbrains.annotations.Nullable;

public interface ExtendedPoiManager {
    @Nullable PoiSection interesium$getOrLoad(long l);
}
