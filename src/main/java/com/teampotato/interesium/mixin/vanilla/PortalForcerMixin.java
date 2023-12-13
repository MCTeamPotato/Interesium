package com.teampotato.interesium.mixin.vanilla;

import com.teampotato.interesium.Interesium;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {
    @Shadow @Final protected ServerLevel level;

    @Inject(method = "findPortalAround", at = @At("HEAD"), cancellable = true)
    private void interesium$findPortalAround(BlockPos pos, boolean isNether, CallbackInfoReturnable<Optional<BlockUtil.FoundRectangle>> cir) {
        Interesium.interesium$findPortalAround(this.level, pos, isNether, cir);
    }
}
