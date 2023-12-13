package com.teampotato.interesium.mixin.vanilla;

import com.teampotato.interesium.api.InteresiumPoiManager;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.Map;

@Mixin(Raids.class)
public abstract class RaidsMixin extends SavedData {
    @Shadow @Final public ServerLevel level;
    @Shadow @Final public Map<Integer, Raid> raidMap;
    @Shadow public abstract Raid getOrCreateRaid(ServerLevel serverLevel, BlockPos pos);

    public RaidsMixin(String string) {
        super(string);
    }

    @Inject(method = "createOrExtendRaid", at = @At("HEAD"), cancellable = true)
    private void interesium$createOrExtendRaid(ServerPlayer player, CallbackInfoReturnable<Raid> cir) {
        if (player.isSpectator() || this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS) || !player.level.dimensionType().hasRaids()) {
            cir.setReturnValue(null);
            return;
        }
        BlockPos playerPos = player.blockPosition();
        Iterator<PoiRecord> poiRecordIterator = InteresiumPoiManager.getInRangeIterator(PoiType.ALL, playerPos, 64, PoiManager.Occupancy.IS_OCCUPIED, this.level.getPoiManager());
        int i = 0;
        Vec3 vec3 = Vec3.ZERO;
        while (poiRecordIterator.hasNext()) {
            BlockPos blockPos2 = poiRecordIterator.next().getPos();
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
        Raid raid = this.getOrCreateRaid(player.getLevel(), raidPos);
        boolean startTest = false;
        if (!raid.isStarted()) {
            if (!this.raidMap.containsKey(raid.getId())) {
                this.raidMap.put(raid.getId(), raid);
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
        this.setDirty();
        cir.setReturnValue(raid);
    }
}
