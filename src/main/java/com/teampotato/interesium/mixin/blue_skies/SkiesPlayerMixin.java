package com.teampotato.interesium.mixin.blue_skies;

import com.legacy.blue_skies.blocks.dungeon.KeystoneBlock;
import com.legacy.blue_skies.capability.SkiesPlayer;
import com.legacy.blue_skies.network.PacketHandler;
import com.legacy.blue_skies.network.s_to_c.DisplayToastPacket;
import com.legacy.blue_skies.registries.SkiesPointsOfInterest;
import com.teampotato.interesium.InteresiumPoiManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Optional;

@Mixin(value = SkiesPlayer.class, remap = false)
public abstract class SkiesPlayerMixin {
    @Shadow private Player player;

    @Inject(method = "teleportToNearestKeystone", at = @At("HEAD"), cancellable = true)
    private void interesium$teleportToNearestKeystone(CallbackInfo ci) {
        ci.cancel();
        if (this.player.level instanceof ServerLevel) {
            BlockPos playerPos = this.player.blockPosition();
            byte distance = 30;
            ServerLevel level = (ServerLevel) this.player.level;
            PoiManager poiManager = level.getPoiManager();
            poiManager.ensureLoadedAndValid(this.player.level, playerPos, distance);
            Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInSquareIterator((poiType) -> poiType == SkiesPointsOfInterest.KEYSTONE, playerPos, distance, PoiManager.Occupancy.ANY, poiManager);
            Optional.ofNullable(poiRecordIterator.hasNext() ? poiRecordIterator.next() : null).ifPresent(poiRecord -> {
                BlockPos poiRecordPos = poiRecord.getPos();
                BlockState keystoneState = level.getBlockState(poiRecordPos);
                BlockPos spawnPos = keystoneState.hasProperty(KeystoneBlock.DirectionalKeystoneBlock.FACING) ?
                        poiRecordPos.relative(keystoneState.getValue(KeystoneBlock.DirectionalKeystoneBlock.FACING), 2) :
                        poiRecordPos.relative(Direction.NORTH, 3);
                this.player.setPos(((double) spawnPos.getX() + 0.5D), spawnPos.getY(), ((double)spawnPos.getZ() + 0.5D));
                if (this.player instanceof ServerPlayer) PacketHandler.sendToClient(new DisplayToastPacket((byte)1), (ServerPlayer) this.player);
            });
        }
    }
}
