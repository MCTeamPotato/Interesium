package com.teampotato.interesium.mixin.betterportals;

import com.teampotato.interesium.InteresiumPoiManager;
import com.yungnickyoung.minecraft.betterportals.init.BPModFluids;
import com.yungnickyoung.minecraft.betterportals.init.BPModPOIs;
import com.yungnickyoung.minecraft.betterportals.world.ReclaimerTeleporter;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(ReclaimerTeleporter.class)
public abstract class ReclaimerTeleporterMixin {
    @Shadow protected abstract int xzDist(Vec3i pos1, Vec3i pos2);

    @Redirect(method = "getPortalInfo", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;min(Ljava/util/Comparator;)Ljava/util/Optional;", remap = false))
    private Optional<BlockPos> interesium$emptyOptional(Stream<?> instance, Comparator<?> comparator) {
        return Optional.empty();
    }

    @Inject(method = "getPortalInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;toString()Ljava/lang/String;", shift = At.Shift.BEFORE), locals = LocalCapture.PRINT, cancellable = true)
    private void interesium$getPortalInfo(Entity entity, ServerLevel targetWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo, CallbackInfoReturnable<PortalInfo> cir, WorldBorder worldBorder, double minX, double minZ, double maxX, double maxZ, double scale, BlockPos.MutableBlockPos targetPos, PoiManager poiManager, int blockSearchRange, Optional<BlockPos> optional) {
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInSquareIterator((poiType) -> poiType == BPModPOIs.PORTAL_LAKE_POI, targetPos, blockSearchRange, PoiManager.Occupancy.ANY, poiManager);
        SortedSet<BlockPos> blockPosSortedSet = new ObjectRBTreeSet<>(Comparator.comparingDouble((pos) -> (double)this.xzDist(pos, targetPos)));
        while (poiRecordIterator.hasNext()) {
            BlockPos poiRecordPos = poiRecordIterator.next().getPos();
            Fluid fluid = targetWorld.getFluidState(poiRecordPos).getType();
            if (fluid == BPModFluids.PORTAL_FLUID_FLOWING || fluid == BPModFluids.PORTAL_FLUID) {
                BlockState above = targetWorld.getBlockState(poiRecordPos.above());
                if (above.getMaterial() == Material.AIR || above.getFluidState().getType() != Fluids.EMPTY) {
                    blockPosSortedSet.add(poiRecordPos);
                }
            }
        }
        BlockPos blockPos = blockPosSortedSet.isEmpty() ? null : blockPosSortedSet.first();
        if (blockPos != null) {
            targetWorld.getChunkSource().distanceManager.addTicket(ChunkPos.asLong(blockPos.getX() >> 4, blockPos.getZ() >> 4), new Ticket<>(TicketType.PORTAL, 30, blockPos, false));
            cir.setReturnValue(new PortalInfo(Vec3.atLowerCornerOf(blockPos), Vec3.ZERO, entity.yRot, entity.xRot));
        }
    }
}
