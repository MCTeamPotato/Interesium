package com.teampotato.interesium.mixin;

import com.google.common.collect.Iterators;
import com.teampotato.interesium.InteresiumPoiManager;
import com.teampotato.interesium.api.ExtendedPoiSection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Mixin(PoiSection.class)
public abstract class PoiSectionMixin implements ExtendedPoiSection {
    @Mutable @Shadow @Final private Map<PoiType, Set<PoiRecord>> byType;

    @Inject(method = "<init>(Ljava/lang/Runnable;ZLjava/util/List;)V", at = @At("RETURN"))
    private void reInitialize(CallbackInfo ci) {
        this.byType = new Reference2ReferenceOpenHashMap<>(this.byType);
    }

    @Override
    public Iterator<PoiRecord> interesium$getRecordsIterator(Predicate<PoiType> typePredicate, PoiManager.@NotNull Occupancy status) {
        final Iterator<Map.Entry<PoiType, Set<PoiRecord>>> entryIterator = this.byType.entrySet().iterator();
        final Iterator<PoiRecord> iterator = new Iterator<PoiRecord>() {
            Iterator<PoiRecord> recordIterator = Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!recordIterator.hasNext() && entryIterator.hasNext()) {
                    Map.Entry<PoiType, Set<PoiRecord>> nextEntry = entryIterator.next();
                    if (typePredicate.test(nextEntry.getKey())) recordIterator = nextEntry.getValue().iterator();
                }
                return recordIterator.hasNext();
            }

            @Override
            public PoiRecord next() {
                return recordIterator.next();
            }
        };

        final Predicate<? super PoiRecord> statusTest = status.getTest();
        if (statusTest.equals(InteresiumPoiManager.ALWAYS_TRUE)) {
            return iterator;
        } else {
            return Iterators.filter(iterator, statusTest::test);
        }
    }

    @Dynamic
    @Inject(method = {"method_19143", "lambda$add$8", "func_234155_a_"}, at = @At("HEAD"), cancellable = true)
    private static void onComputeByType(PoiType arg, @NotNull CallbackInfoReturnable<Set<PoiRecord>> cir) {
        cir.setReturnValue(new ObjectOpenHashSet<>());
    }
}
