package com.teampotato.interesium.mixin;

import com.teampotato.interesium.InteresiumPoiManager;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;

@Mixin(targets = "net.minecraft.world.effect.MobEffects$1")
public abstract class MobEffectsMixin {
    @Redirect(method = "applyEffectTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raids;createOrExtendRaid(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/world/entity/raid/Raid;"))
    private @Nullable Raid interesium$createOrExtendRaid(Raids raids, @NotNull ServerPlayer player) {
        if (player.isSpectator() || raids.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS) || !player.level.dimensionType().hasRaids()) return null;
        BlockPos playerPos = player.blockPosition();
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(PoiType.ALL, playerPos, 64, PoiManager.Occupancy.IS_OCCUPIED, raids.level.getPoiManager());
        int i = 0;
        Vec3 vec3 = Vec3.ZERO;
        while (poiRecordIterator.hasNext()) {
            PoiRecord poiRecord = poiRecordIterator.next();
            BlockPos blockPos2 = poiRecord.getPos();
            vec3 = vec3.add(blockPos2.getX(), blockPos2.getY(), blockPos2.getZ());
            ++i;
        }
        BlockPos raidPos;
        if (i > 0) {
            vec3 = vec3.scale(1.0 / (double)i);
            raidPos = new BlockPos(vec3);
        } else {
            raidPos = playerPos;
        }
        Raid raid = raids.getOrCreateRaid(player.getLevel(), raidPos);
        boolean startTest = false;
        if (!raid.isStarted()) {
            if (!raids.raidMap.containsKey(raid.getId())) {
                raids.raidMap.put(raid.getId(), raid);
            }
            startTest = true;
        } else if (raid.getBadOmenLevel() < raid.getMaxBadOmenLevel()) {
            startTest = true;
        } else {
            player.removeEffect(MobEffects.BAD_OMEN);
            player.connection.send(new ClientboundEntityEventPacket(player, (byte) 43));
        }
        if (startTest) {
            raid.absorbBadOmen(player);
            player.connection.send(new ClientboundEntityEventPacket(player, (byte) 43));
            if (!raid.hasFirstWaveSpawned()) {
                player.awardStat(Stats.RAID_TRIGGER);
                CriteriaTriggers.BAD_OMEN.trigger(player);
            }
        }
        raids.setDirty();
        return raid;
    }
}
