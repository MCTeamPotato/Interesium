package com.teampotato.interesium.mixin.mca;

import com.teampotato.interesium.InteresiumPoiManager;
import forge.net.mca.entity.VillagerEntityMCA;
import forge.net.mca.entity.ai.Residency;
import forge.net.mca.server.world.data.GraveyardManager;
import forge.net.mca.server.world.data.VillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(value = Residency.class, remap = false)
public abstract class ResidencyMixin {
    @Shadow @Final private VillagerEntityMCA entity;

    @Inject(method = "reportBuildings", at = @At("HEAD"), cancellable = true)
    private void interesium$reportBuildings(CallbackInfo ci) {
        VillageManager manager = VillageManager.get((ServerLevel)this.entity.level);
        Iterator<BlockPos> blockPosIterator = InteresiumPoiManager.findAllIterator(PoiType.ALL, (p) -> !manager.cache.contains(p), this.entity.blockPosition(), 48, PoiManager.Occupancy.ANY, ((ServerLevel)this.entity.level).getPoiManager());
        while (blockPosIterator.hasNext()) manager.reportBuilding(blockPosIterator.next());
        GraveyardManager.get((ServerLevel)this.entity.level).reportToVillageManager(this.entity);
        ci.cancel();
    }
}
