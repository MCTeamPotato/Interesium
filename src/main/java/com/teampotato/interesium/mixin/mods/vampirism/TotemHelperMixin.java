package com.teampotato.interesium.mixin.mods.vampirism;

import com.google.common.collect.Iterators;
import com.teampotato.interesium.api.InteresiumPoiManager;
import com.teampotato.interesium.util.IteratorContainerList;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.config.VampirismConfig;
import de.teamlapen.vampirism.tileentity.TotemHelper;
import de.teamlapen.vampirism.tileentity.TotemTileEntity;
import de.teamlapen.vampirism.world.FactionPointOfInterestType;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(value = TotemHelper.class, remap = false)
public abstract class TotemHelperMixin {


    @Shadow @Final private static Map<ResourceKey<Level>, Map<BlockPos, BlockPos>> totemPositions;

    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "getTotemPosNearPos", at = @At("HEAD"), cancellable = true)
    private static void interesium$getTotemPosNearPos(ServerLevel world, BlockPos pos, CallbackInfoReturnable<Optional<BlockPos>> cir) {
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator((p) -> true, pos, 25, PoiManager.Occupancy.ANY, world.getPoiManager());
        cir.setReturnValue(poiRecordIterator.hasNext() ? interesium$getTotemPosition(world.dimension(), poiRecordIterator) : Optional.empty());
    }

    @Unique
    private static Optional<BlockPos> interesium$getTotemPosition(ResourceKey<Level> dimension, @NotNull Iterator<PoiRecord> pois) {
        Map<BlockPos, BlockPos> totemPositionMap = totemPositions.computeIfAbsent(dimension, (key) -> new HashMap<>());

        PoiRecord pointOfInterest;
        do {
            if (!pois.hasNext()) return Optional.empty();
            pointOfInterest = pois.next();
        } while(!totemPositionMap.containsKey(pointOfInterest.getPos()));

        return Optional.of(totemPositionMap.get(pointOfInterest.getPos()));
    }

    @Inject(method = "forceFactionCommand", at = @At("HEAD"), cancellable = true)
    private static void interesium$forceFactionCommand(IFaction<?> faction, ServerPlayer player, CallbackInfoReturnable<Component> cir) {
        ServerLevel level = player.getLevel();
        BlockPos playerPos = player.blockPosition();
        Map<BlockPos, BlockPos> totemPositionMap = totemPositions.computeIfAbsent(level.dimension(), (key) -> new HashMap<>());
        PoiRecord acceptedPoiRecord = null;
        Comparator<PoiRecord> poiRecordComparator = Comparator.comparingInt((point) -> (int)point.getPos().distSqr(playerPos));
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator((point) -> true, playerPos, 25, PoiManager.Occupancy.ANY, level.getPoiManager());
        boolean hasEntryInPosMap = false;
        while (poiRecordIterator.hasNext()) {
            PoiRecord poiRecord = poiRecordIterator.next();
            if (!hasEntryInPosMap && totemPositionMap.containsKey(poiRecord.getPos())) hasEntryInPosMap = true;
            if (acceptedPoiRecord == null) {
                acceptedPoiRecord = poiRecord;
            } else {
                if (poiRecordComparator.compare(acceptedPoiRecord, poiRecord) < 0) {
                    acceptedPoiRecord = poiRecord;
                }
            }
        }
        if (!hasEntryInPosMap) {
            cir.setReturnValue(new TranslatableComponent("command.vampirism.test.village.no_village"));
        } else {
            BlockEntity te = level.getBlockEntity(totemPositionMap.get(acceptedPoiRecord.getPos()));
            if (!(te instanceof TotemTileEntity)) {
                LOGGER.warn("TileEntity at {} is no TotemTileEntity", totemPositionMap.get(acceptedPoiRecord.getPos()));
                cir.setReturnValue(new TextComponent(""));
            } else {
                ((TotemTileEntity) te).setForcedFaction(faction);
                cir.setReturnValue(new TranslatableComponent("command.vampirism.test.village.success", faction == null ? "none" : faction.getName()));
            }
        }
    }

    @Inject(method = "getVillagePointsOfInterest", at = @At("HEAD"), cancellable = true)
    private static void interesium$getVillagePointsOfInterest(ServerLevel world, BlockPos pos, CallbackInfoReturnable<Set<PoiRecord>> cir) {
        PoiManager poiManager = world.getPoiManager();
        Set<PoiRecord> finished = new ObjectOpenHashSet<>();
        IteratorContainerList<PoiRecord> points = new IteratorContainerList<>(InteresiumPoiManager.getInRangeIterator((type) -> !(type instanceof FactionPointOfInterestType), pos, 50, PoiManager.Occupancy.ANY, poiManager));

        while(!points.isEmpty()) {
            Iterator<Iterator<PoiRecord>> transformedIterator = Iterators.transform(points.iterator(), poiRecord -> InteresiumPoiManager.getInRangeIterator(type -> !(type instanceof FactionPointOfInterestType), poiRecord.getPos(), 40, PoiManager.Occupancy.ANY, poiManager));
            points.clear();
            while (transformedIterator.hasNext()) {
                Iterator<PoiRecord> poiRecordIterator = transformedIterator.next();
                while (poiRecordIterator.hasNext()) {
                    PoiRecord poiRecord = poiRecordIterator.next();
                    if (!finished.contains(poiRecord) && poiRecord.getPos().closerThan(pos, (double) VampirismConfig.BALANCE.viMaxTotemRadius.get())) {
                        points.add(poiRecord);
                    }
                    finished.add(poiRecord);
                }
            }
        }

        cir.setReturnValue(finished);
    }
}
