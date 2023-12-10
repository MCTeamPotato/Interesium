package com.teampotato.interesium.mixin.blue_skies;

import com.legacy.blue_skies.events.SkiesPlayerEvents;
import com.legacy.blue_skies.registries.SkiesPointsOfInterest;
import com.legacy.blue_skies.tile_entity.KeystoneTileEntity;
import com.legacy.blue_skies.util.EntityUtil;
import com.teampotato.interesium.InteresiumPoiManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.Iterator;
import java.util.Optional;

@Mixin(value = SkiesPlayerEvents.DungeonBalancingChecks.class, remap = false)

public abstract class SkiesPlayerEventsDungeonBalancingChecksMixin {
    /**
     * @author Kasualix
     * @reason avoid stream usage; Use overwriting to avoid too much unnecessary allocation in injecting; reduce allocations
     */
    @Overwrite
    public static boolean isPlacementInBossRoom(Level worldIn, BlockPos pos) {
        if (worldIn instanceof ServerLevel && EntityUtil.isInDungeon(worldIn, pos)) {
            int distance = 45;
            ServerLevel world = (ServerLevel)worldIn;
            PoiManager poiManager = world.getPoiManager();
            poiManager.ensureLoadedAndValid(world, pos, distance);
            Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInSquareIterator((poiType) -> poiType == SkiesPointsOfInterest.KEYSTONE, pos, distance, PoiManager.Occupancy.ANY, poiManager);
            Optional<PoiRecord> poiRecordOptional = Optional.ofNullable(poiRecordIterator.hasNext() ? poiRecordIterator.next() : null);
            if (poiRecordOptional.isPresent()) {
                BlockEntity blockEntity = world.getBlockEntity(poiRecordOptional.get().getPos());
                if (blockEntity instanceof KeystoneTileEntity) {
                    int x = pos.getX();
                    int y = pos.getY();
                    int z = pos.getZ();
                    KeystoneTileEntity keystoneTileEntity = (KeystoneTileEntity) blockEntity;
                    BlockPos bossRoomCenter = keystoneTileEntity.getBlockPos().offset(keystoneTileEntity.getSpawnOffset());
                    boolean isInCeilingRange = keystoneTileEntity.getBossRoomCeilingSize() == 0 || y <= bossRoomCenter.getY() + keystoneTileEntity.getBossRoomCeilingSize();
                    return isInCeilingRange && y >= bossRoomCenter.getY() - 1 && interesium$isPointInsideRec(x, y, z, bossRoomCenter.getX() - keystoneTileEntity.getBossRoomSize(), y, bossRoomCenter.getZ() - keystoneTileEntity.getBossRoomSize(), bossRoomCenter.getX() + keystoneTileEntity.getBossRoomSize(), y, bossRoomCenter.getZ() + keystoneTileEntity.getBossRoomSize());
                }
            }
        }

        return false;
    }

    @Unique
    private static boolean interesium$isPointInsideRec(final int posX, final int posY, final int posZ, final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
        return posX >= minX && posX <= maxX && posZ >= minZ && posZ <= maxZ && posY >= minY && posY <= maxY;
    }
}
