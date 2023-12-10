package com.teampotato.interesium.mixin.forbidden_arcanus;

import com.stal111.forbidden_arcanus.common.tile.ArcaneCrystalObeliskTileEntity;
import com.stal111.forbidden_arcanus.common.tile.forge.HephaestusForgeTileEntity;
import com.stal111.forbidden_arcanus.init.other.ModPOITypes;
import com.teampotato.interesium.InteresiumPoiManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(ArcaneCrystalObeliskTileEntity.class)
public abstract class ArcaneCrystalObeliskTileEntityMixin extends BlockEntity {
    public ArcaneCrystalObeliskTileEntityMixin(BlockEntityType<?> arg) {
        super(arg);
    }

    @SuppressWarnings("DataFlowIssue")

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getPoiManager()Lnet/minecraft/world/entity/ai/village/poi/PoiManager;", shift = At.Shift.BEFORE), cancellable = true)
    private void interesium$tick(CallbackInfo ci) {
        ci.cancel();
        PoiManager poiManager = ((ServerLevel)this.level).getPoiManager();
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator((poiType) -> poiType == ModPOITypes.HEPHAESTUS_FORGE.get(), this.worldPosition, 4, PoiManager.Occupancy.ANY, poiManager);
        BlockPos blockPos = poiRecordIterator.hasNext() ? poiRecordIterator.next().getPos() : null;
        if (blockPos != null) {
            HephaestusForgeTileEntity hephaestusForge = (HephaestusForgeTileEntity) this.level.getBlockEntity(blockPos);
            if (hephaestusForge == null) return;
            hephaestusForge.getEssenceManager().increaseAureal(1);
        }
    }
}
