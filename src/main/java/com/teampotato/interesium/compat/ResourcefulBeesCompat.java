package com.teampotato.interesium.compat;

import com.resourcefulbees.resourcefulbees.registry.ModPOIs;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.jetbrains.annotations.NotNull;

public class ResourcefulBeesCompat {
    private static PoiType poiType;
    public static @NotNull PoiType getTieredBeehivePoi() {
        if (poiType == null) poiType = ModPOIs.TIERED_BEEHIVE_POI.get();
        return poiType;
    }
}
