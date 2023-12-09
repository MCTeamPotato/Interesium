package com.teampotato.interesium.mixin.blueskies;

import com.legacy.blue_skies.blocks.construction.TroughBlock;
import com.legacy.blue_skies.events.SkiesEvents;
import com.legacy.blue_skies.registries.SkiesPointsOfInterest;
import com.teampotato.interesium.InteresiumPoiManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

@Mixin(SkiesEvents.class)
public abstract class SkiesEventsMixin {
    @Inject(method = "onEntityUpdate", at = @At(value = "INVOKE", target = "Lcom/legacy/blue_skies/util/EntityUtil;getPoisInCircle(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/ai/village/poi/PoiType;I)Ljava/util/stream/Stream;", shift = At.Shift.BEFORE), cancellable = true)
    private static void interesium$onEntityUpdate(LivingEvent.LivingUpdateEvent event, CallbackInfo ci) {
        ci.cancel();
        Animal animal = (Animal) event.getEntityLiving();
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.blueSkies$getPoisInCircle(event.getEntityLiving(), SkiesPointsOfInterest.TROUGH, 7);
        Optional.ofNullable(poiRecordIterator.hasNext() ? poiRecordIterator.next() : null).ifPresent(poiRecord -> {
            BlockPos pos = poiRecord.getPos();
            Level level = animal.level;
            BlockState trough = level.getBlockState(pos);
            Random random = level.random;
            if (TroughBlock.canConsume(trough, level)) {
                animal.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                if (random.nextFloat() < 0.3F) {
                    TroughBlock.attemptConsume(trough, level, pos);
                    animal.ageUp(30 + random.nextInt(10), true);
                } else {
                    animal.ageUp(10 + random.nextInt(10), true);
                }
            }
        });
    }

    @Inject(method = "onEntityCheckSpawn", at = @At(value = "INVOKE", target = "Lcom/legacy/blue_skies/util/EntityUtil;getPoisInCircle(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/ai/village/poi/PoiType;I)Ljava/util/stream/Stream;", shift = At.Shift.BEFORE), cancellable = true)
    private static void interesium$onEntityCheckSpawn(LivingSpawnEvent.CheckSpawn event, CallbackInfo ci) {
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.blueSkies$getPoisInCircle(event.getEntityLiving(), SkiesPointsOfInterest.WARDING_PEARL, 25);
        Optional.ofNullable(poiRecordIterator.hasNext() ? poiRecordIterator.next() : null).ifPresent(poiRecord -> event.setResult(Event.Result.DENY));
    }
}
