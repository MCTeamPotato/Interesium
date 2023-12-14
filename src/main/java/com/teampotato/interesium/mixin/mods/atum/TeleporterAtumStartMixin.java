package com.teampotato.interesium.mixin.mods.atum;

import com.teammetallurgy.atum.world.teleporter.TeleporterAtum;
import com.teammetallurgy.atum.world.teleporter.TeleporterAtumStart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TeleporterAtumStart.class)
public abstract class TeleporterAtumStartMixin {
    @Redirect(method = "onAtumJoining", at = @At(value = "INVOKE", target = "Lcom/teammetallurgy/atum/world/teleporter/TeleporterAtum;placeInPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;F)Z"))
    private boolean skip(TeleporterAtum teleporterAtum, ServerLevel serverLevel, Entity entity, float yaw) {
        return false;
    }
}
