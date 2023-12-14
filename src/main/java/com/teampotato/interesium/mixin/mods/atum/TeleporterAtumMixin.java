package com.teampotato.interesium.mixin.mods.atum;

import com.teammetallurgy.atum.init.AtumPointsOfInterest;
import com.teammetallurgy.atum.world.teleporter.TeleporterAtum;
import com.teampotato.interesium.api.InteresiumPoiManager;
import com.teampotato.interesium.mixin.InteresiumMixinManager;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

@Mixin(TeleporterAtum.class)
public abstract class TeleporterAtumMixin {
    @Unique private static final int interesium$radius = 128;

    @Redirect(method = "placeEntity", at = @At(value = "INVOKE", target = "Lcom/teammetallurgy/atum/world/teleporter/TeleporterAtum;placeInPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;F)Z", ordinal = 1))
    private boolean skip1(TeleporterAtum teleporterAtum, ServerLevel serverLevel, Entity entity, float yaw) {
        return false;
    }

    @Redirect(method = "placeEntity", at = @At(value = "INVOKE", target = "Lcom/teammetallurgy/atum/world/teleporter/TeleporterAtum;placeInPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;F)Z", ordinal = 2))
    private boolean skip2(TeleporterAtum teleporterAtum, ServerLevel serverLevel, Entity entity, float yaw) {
        return false;
    }

    @Inject(method = "getExistingPortal", at = @At("HEAD"), cancellable = true)
    private void interesium$getExistingPortal(ServerLevel serverLevel, BlockPos pos, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        long begin = System.currentTimeMillis();
        PoiManager poiManager = serverLevel.getPoiManager();
        poiManager.ensureLoadedAndValid(serverLevel, pos, interesium$radius);
        PoiRecord acceptedPoiRecord = null;
        Comparator<PoiRecord> poiRecordComparator = Comparator.<PoiRecord>comparingDouble(poiRecord -> poiRecord.getPos().distSqr(pos)).thenComparingInt(poiRecord -> poiRecord.getPos().getY());

        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInSquareIterator(poiType -> poiType == AtumPointsOfInterest.PORTAL, pos, interesium$radius, PoiManager.Occupancy.ANY, poiManager);
        while (poiRecordIterator.hasNext()) {
            PoiRecord poiRecord = poiRecordIterator.next();
            if (acceptedPoiRecord == null) {
                acceptedPoiRecord = poiRecord;
            } else {
                if (poiRecordComparator.compare(poiRecord, acceptedPoiRecord) < 0) {
                    acceptedPoiRecord = poiRecord;
                }
            }
        }


        cir.setReturnValue(
                Optional.ofNullable(acceptedPoiRecord).map(poiRecord -> {
                    BlockPos poiRecordPos = poiRecord.getPos();
                    serverLevel.getChunkSource().distanceManager.addTicket(ChunkPos.asLong(poiRecordPos.getX() >> 4, poiRecordPos.getZ() >> 4), new Ticket<>(TicketType.PORTAL, 30, poiRecordPos, false));
                    BlockState blockState = serverLevel.getBlockState(poiRecordPos);
                    return BlockUtil.getLargestRectangleAround(poiRecordPos, Direction.Axis.X, 9, Direction.Axis.Z, 9, posIn -> serverLevel.getBlockState(posIn) == blockState);
                })
        );
        long end = System.currentTimeMillis();
        InteresiumMixinManager.LOGGER.error("TeleporterAtum#getExistingPortal time cost: " + (end - begin));
    }
}
