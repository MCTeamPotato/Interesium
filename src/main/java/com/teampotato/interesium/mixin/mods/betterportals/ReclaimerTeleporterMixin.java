package com.teampotato.interesium.mixin.mods.betterportals;

import com.google.common.collect.Iterators;
import com.teampotato.interesium.api.InteresiumPoiManager;
import com.yungnickyoung.minecraft.betterportals.init.BPModFluids;
import com.yungnickyoung.minecraft.betterportals.init.BPModPOIs;
import com.yungnickyoung.minecraft.betterportals.util.BlockUtil;
import com.yungnickyoung.minecraft.betterportals.world.ReclaimerTeleporter;
import com.yungnickyoung.minecraft.betterportals.world.variant.MonolithVariantSettings;
import com.yungnickyoung.minecraft.betterportals.world.variant.MonolithVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Function;

@Mixin(ReclaimerTeleporter.class)
public abstract class ReclaimerTeleporterMixin {
    @Shadow protected abstract int xzDist(Vec3i pos1, Vec3i pos2);

    @Inject(method = "getPortalInfo", at = @At("HEAD"), cancellable = true)
    private void interesium$getPortalInfo(Entity entity, ServerLevel targetWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo, CallbackInfoReturnable<PortalInfo> cir) {
        WorldBorder worldBorder = targetWorld.getWorldBorder();
        double minX = Math.max(-2.9999872E7, worldBorder.getMinX() + 16.0);
        double minZ = Math.max(-2.9999872E7, worldBorder.getMinZ() + 16.0);
        double maxX = Math.min(2.9999872E7, worldBorder.getMaxX() - 16.0);
        double maxZ = Math.min(2.9999872E7, worldBorder.getMaxZ() - 16.0);
        double scale = DimensionType.getTeleportationScale(entity.getCommandSenderWorld().dimensionType(), targetWorld.dimensionType());
        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(Mth.clamp((double)((int)entity.getX()) * scale, minX, maxX), entity.getY(), Mth.clamp((double)((int)entity.getZ()) * scale, minZ, maxZ));
        PoiManager poiManager = targetWorld.getPoiManager();
        int blockSearchRange = 128;
        poiManager.ensureLoadedAndValid(targetWorld, targetPos, blockSearchRange);
        Comparator<BlockPos> blockPosComparator = Comparator.comparingDouble((pos) -> (double)this.xzDist(pos, targetPos));
        Iterator<BlockPos> blockPosIterator =  Iterators.filter(Iterators.transform(InteresiumPoiManager.getInSquareIterator((poiType) -> poiType == BPModPOIs.PORTAL_LAKE_POI, targetPos, blockSearchRange, PoiManager.Occupancy.ANY, poiManager), PoiRecord::getPos), (pos) -> {
            Fluid fluid = targetWorld.getBlockState(pos).getFluidState().getType();
            BlockState above = targetWorld.getBlockState(pos.above());
            return (fluid == BPModFluids.PORTAL_FLUID_FLOWING || fluid == BPModFluids.PORTAL_FLUID) && (above.getMaterial() == Material.AIR || above.getFluidState().getType() != Fluids.EMPTY);
        });
        BlockPos acceptedBlockPos = blockPosIterator.hasNext() ? blockPosIterator.next() : null;
        if (acceptedBlockPos != null) {
            while (blockPosIterator.hasNext()) {
                BlockPos blockPos = blockPosIterator.next();
                if (blockPosComparator.compare(acceptedBlockPos, blockPos) < 0) {
                    acceptedBlockPos = blockPos;
                }
            }
        }
        if (acceptedBlockPos != null) {
            targetWorld.getChunkSource().distanceManager.addTicket(ChunkPos.asLong(acceptedBlockPos.getX() >> 4, acceptedBlockPos.getZ() >> 4), new Ticket<>(TicketType.PORTAL, 30, acceptedBlockPos, false));
            cir.setReturnValue(new PortalInfo(Vec3.atLowerCornerOf(acceptedBlockPos), Vec3.ZERO, entity.yRot, entity.xRot));
        } else {
            String sourceDimension = entity.getCommandSenderWorld().dimension().location().toString();
            MonolithVariantSettings settings = MonolithVariants.get().getVariantForDimension(sourceDimension);
            int targetMinY = settings.getPlayerTeleportedMinY();
            int targetMaxY = settings.getPlayerTeleportedMaxY();
            int targetY = -1;
            targetPos.setY(targetMaxY);
            boolean foundAir = false;
            int blocksSinceAir = 0;

            for(int y = targetMaxY; y >= targetMinY; --y) {
                BlockState blockState = targetWorld.getBlockState(targetPos);
                if (blockState.getMaterial() == Material.AIR) {
                    foundAir = true;
                } else if (blockState.getMaterial().isSolid() && foundAir) {
                    if (blocksSinceAir >= 2) {
                        targetY = y + 1;
                        break;
                    }

                    foundAir = false;
                    blocksSinceAir = 0;
                }

                targetPos.move(Direction.DOWN);
                if (foundAir) {
                    ++blocksSinceAir;
                }
            }

            if (targetY == -1) {
                targetY = (targetMaxY + targetMinY) / 2;
                BlockUtil.replaceLiquid(targetWorld, targetPos.getX() - 2, targetY - 1, targetPos.getZ() - 2, targetPos.getX() + 2, targetY + 3, targetPos.getZ() + 2, SPAWN_PLATFORM_BLOCK);
                BlockUtil.replaceFallingBlock(targetWorld, targetPos.getX() - 2, targetY, targetPos.getZ() - 2, targetPos.getX() + 2, targetY + 3, targetPos.getZ() + 2, SPAWN_PLATFORM_BLOCK);
                BlockUtil.fill(targetWorld, targetPos.getX() - 1, targetY, targetPos.getZ() - 1, targetPos.getX() + 1, targetY + 2, targetPos.getZ() + 1, CAVE_AIR);
                BlockUtil.fill(targetWorld, targetPos.getX() - 1, targetY - 1, targetPos.getZ() - 1, targetPos.getX() + 1, targetY - 1, targetPos.getZ() + 1, SPAWN_PLATFORM_BLOCK);
            }

            targetPos.setY(targetY);
            targetWorld.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(targetPos), 3, targetPos);
            Vec3 playerPos = new Vec3(((double) targetPos.getX()) + 0.5, ((double)targetPos.getY()) + 0.0, ((double) targetPos.getZ()) + 0.5);
            cir.setReturnValue(new PortalInfo(playerPos, Vec3.ZERO, entity.yRot, entity.xRot));
        }
    }

    @Unique
    private static final BlockState SPAWN_PLATFORM_BLOCK = Blocks.COBBLESTONE.defaultBlockState();
    @Unique
    private static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
}
