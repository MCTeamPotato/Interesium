package com.teampotato.interesium.api.extension;

import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;

import java.util.Iterator;
import java.util.function.Predicate;


public interface ExtendedPoiSection {
    Iterator<PoiRecord> interesium$getRecordsIterator(Predicate<PoiType> typePredicate, PoiManager.Occupancy status);
}
