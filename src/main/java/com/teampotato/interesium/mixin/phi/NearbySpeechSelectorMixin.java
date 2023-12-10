package com.teampotato.interesium.mixin.phi;

import com.teampotato.interesium.InteresiumPoiManager;
import gdavid.phi.block.ModBlocks;
import gdavid.phi.block.tile.MPUTile;
import gdavid.phi.spell.selector.mpu.NearbySpeechSelector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.ServerChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(value = NearbySpeechSelector.class, remap = false)
public abstract class NearbySpeechSelectorMixin {
    @Inject(method = "speech", at = @At("HEAD"), cancellable = true)
    private static void speech(ServerChatEvent event, CallbackInfo ci) {
        ci.cancel();
        Player player = event.getPlayer();
        if (player.level instanceof ServerLevel) {
            Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator((type) -> type == ModBlocks.mpuPOI, player.blockPosition(), 32, PoiManager.Occupancy.ANY, ((ServerLevel) player.level).getPoiManager());
            while (poiRecordIterator.hasNext()) {
                BlockEntity tile = player.level.getBlockEntity(poiRecordIterator.next().getPos());
                if (tile instanceof MPUTile) ((MPUTile)tile).setNearbySpeech(event.getMessage());
            }
        }
    }
}
